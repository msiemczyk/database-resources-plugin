/*
 * Copyright (C) 2021 Maciek Siemczyk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.jenkins.plugins.reservableresources;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Result;
import jenkins.model.Jenkins;

/**
 * This singleton manages the reservation of resources and keeps track of everything.
 */
public class ReservableResourcesManager {

    // TODO: add some file logging as well
    private static final Logger log = Logger.getLogger(ReservableResourcesManager.class.getName());
    
    public static final String LOG_PREFIX = "[reservable-resources] ";
    
    private final Map<String, Node> reservedNodesByName = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private ReservableResourcesManager() {
    
    }
    
    public List<Node> getResources() {

        return Jenkins.get().getNodes().stream()
            .filter(node -> node.getNodeProperty(NodePropertyExtension.class) != null)
            .collect(Collectors.toList());
    }
    
    public boolean isNodeReserved(final Node node) {

        return reservedNodesByName.containsKey(node.getNodeName());
    }
    
    public synchronized Node acquireResource(
            final RequiredReservableResource requiredResource,
            final AbstractBuild<?, ?> build) throws InterruptedException {

        log.info("About to acquire " + requiredResource + ".");
        
        final String label = requiredResource.getResourceLabel();
        
        List<Node> reservableNodes = getReservableNodes(label);
        
        if (reservableNodes.isEmpty()) {
            throw new IllegalArgumentException("The are no reservable nodes with label '" + label + "'.");
        }
        
        Node availableNode = acquireAvailableNode(label, reservableNodes, build);
        
        reservedNodesByName.put(availableNode.getNodeName(), availableNode);
        
        return availableNode;
    }

    public synchronized void releaseResource(final Node lockedNode) {

        reservedNodesByName.remove(lockedNode.getNodeName());
    }
    
    private List<Node> getReservableNodes(String resourceLabel) {

        return Jenkins.get().getNodes().stream()
            .filter(node -> node.getNodeProperty(NodePropertyExtension.class) != null)
            .filter(reservableNode -> reservableNode.getLabelString().contains(resourceLabel))
            .collect(Collectors.toList());
    }
    
    private Node acquireAvailableNode(
            String resourceLabel,
            List<Node> reservableNodes,
            AbstractBuild<?,?> build) throws InterruptedException {

        long timeoutInMs = TimeUnit.MINUTES.toMillis(getTimeoutInMinutes());
        long startTime = System.currentTimeMillis();
        
        while (true) {
            List<Node> availableNodes = reservableNodes.stream()
                .filter(node -> node.toComputer().isOnline())
                .filter(filteredNode -> !reservedNodesByName.containsKey(filteredNode.getNodeName()))
                .collect(Collectors.toList());
        
            if (availableNodes.isEmpty()) {
                setBuildDescription(
                    build,
                    "Waiting for next available resource from '" + resourceLabel + "'...");
                
                if ((System.currentTimeMillis() - startTime) > timeoutInMs) {
                    abortBuild(build);
                }
                
                TimeUnit.SECONDS.sleep(5);
                continue;
            }
            
            setBuildDescription(build, "");
            
            return availableNodes.get(0);
        }
    }

    private long getTimeoutInMinutes() {

        return 90;
//        return Optional.ofNullable(GlobalConfiguration.all().get(GlobalConfigurationExtension.class))
//            .map(GlobalConfigurationExtension::getTimeoutInMinutes)
//            .orElse(GlobalConfigurationExtension.DEFAULT_TIMEOUT_IN_MINUTES);
    }

    private void setBuildDescription(
            final AbstractBuild<?, ?> build,
            final String description) {

        try {
            build.setDescription(description);
        } catch (IOException ignoreException) {
        }
    }

    private void abortBuild(final AbstractBuild<?, ?> build) throws InterruptedException {

        Executor executor = build.getExecutor();
        
        if (executor == null) {
            throw new InterruptedException("Reservable resource maximum wait time reached.");
        }
        
        executor.interrupt(Result.FAILURE);
    }
    
    /**
     * Singleton instance.
     */
    private static ReservableResourcesManager instance;
    
    /**
     * Gets the {@link ReservableResourcesManager} singleton. 
     * 
     * @return The instance; never null.
     */
    @Nonnull
    public static synchronized ReservableResourcesManager getInstance() {

        if (instance == null) {
            instance = new ReservableResourcesManager();
        }

        return instance;
    }
}
