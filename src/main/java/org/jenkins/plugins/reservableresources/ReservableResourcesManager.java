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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.jenkins.plugins.reservableresources.model.NodePropertyExtension;
import org.jenkins.plugins.reservableresources.model.RequiredReservableResource;

import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Result;
import jenkins.model.Jenkins;

/**
 * This singleton manages the reservation of resources and keeps track of everything.
 */
public class ReservableResourcesManager {

    // TODO: add some more logging as well
    private static final Logger log = Logger.getLogger(ReservableResourcesManager.class.getName());
    
    public static final String LOG_PREFIX = "[reservable-resources] ";
    
    private final Map<String, ReservedResource> reservedByNodeName = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private ReservableResourcesManager() {
    
    }
    
    public synchronized Node acquireResource(
            final int timeoutInMinutes,
            final RequiredReservableResource requiredResource,
            final AbstractBuild<?, ?> build) throws InterruptedException {

        log.fine("About to acquire " + requiredResource + ".");
        
        final String label = requiredResource.getResourceLabel();
        
        List<Node> reservableNodes = getReservableNodes(label);
        
        if (reservableNodes.isEmpty()) {
            throw new IllegalArgumentException("The are no reservable nodes with label '" + label + "'.");
        }
        
        Node availableNode = acquireAvailableNode(timeoutInMinutes, label, reservableNodes, build);
        
        reservedByNodeName.put(
            availableNode.getNodeName(),
            new ReservedResource(availableNode, build));
        
        return availableNode;
    }

    /**
     * Releases reserved resource.
     * 
     * @param nodeName String representing node name of the resource.
     */
    public synchronized void releaseResource(final String nodeName) {

        reservedByNodeName.remove(nodeName);
    }
    
    public List<Node> getReservableNodes() {

        return Jenkins.get().getNodes().stream()
            .filter(node -> node.getNodeProperty(NodePropertyExtension.class) != null)
            .collect(Collectors.toList());
    }
    
    public Optional<ReservedResource> getReservedInfo(final Node node) {

        return Optional.ofNullable(reservedByNodeName.get(node.getNodeName())); 
    }
    
    private List<Node> getReservableNodes(String resourceLabel) {

        return Jenkins.get().getNodes().stream()
            .filter(node -> node.getNodeProperty(NodePropertyExtension.class) != null)
            .filter(reservableNode -> reservableNode.getLabelString().contains(resourceLabel))
            .collect(Collectors.toList());
    }
    
    private Node acquireAvailableNode(
            int timeoutInMinutes,
            String resourceLabel,
            List<Node> reservableNodes,
            AbstractBuild<?,?> build) throws InterruptedException {

        long timeoutInMs = TimeUnit.MINUTES.toMillis(timeoutInMinutes);
        long startTime = System.currentTimeMillis();
        
        while (true) {
            List<Node> availableNodes = reservableNodes.stream()
                .filter(node -> node.toComputer().isOnline())
                .filter(filteredNode -> !reservedByNodeName.containsKey(filteredNode.getNodeName()))
                .collect(Collectors.toList());
        
            if (availableNodes.isEmpty()) {
                setBuildDescription(
                    build,
                    "Waiting for next available resource from '" + resourceLabel + "'...");
                
                if ((System.currentTimeMillis() - startTime) > timeoutInMs) {
                    // TODO: figure out how to print to console when aborting due to time-out
                    abortBuild(build);
                }
                
                TimeUnit.SECONDS.sleep(5);
                continue;
            }
            
            setBuildDescription(build, "");
            
            return availableNodes.get(0);
        }
    }

    private void setBuildDescription(
            final AbstractBuild<?, ?> build,
            final String description) {

        try {
            build.setDescription(description);
        } catch (IOException ignoreException) {
            // Not much we can do with this exception so ignore it.
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
    
    public static final class ReservedResource {
        
        public final Node node;
        public final ReservedBy reservedBy;
        
        public ReservedResource(
                Node node,
                AbstractBuild<?, ?> build) {

            this.node = node;
            this.reservedBy = new ReservedBy(build.toString(), build);
        }
        
        public static final class ReservedBy {
            
            private final String displayName;
            private final AbstractBuild<?, ?> build;
            
            public ReservedBy(
                    String displayName,
                    AbstractBuild<?,?> build) {

                this.displayName = displayName;
                this.build = build;
            }

            public String getDisplayName() {

                return displayName;
            }

            @SuppressWarnings("java:S1452")
            public AbstractBuild<?, ?> getBuild() {

                return build;
            }
        }
    }
}
