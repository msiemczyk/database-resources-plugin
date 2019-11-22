/*
 * Copyright (C) 2019 Maciek Siemczyk
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
package org.jenkins.plugins.databaseresources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Result;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

/**
 * TODO: comments
 */
public class DatabaseResourcesManager {

    // TODO: add some file logging as well
    private static final Logger log = Logger.getLogger(DatabaseResourcesManager.class.getName());
    
    public static final String LOG_PREFIX = "[database-resources] ";
    
    private final Map<String, Node> lockedNodesByName = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private DatabaseResourcesManager() {
    
    }
    
    public List<DatabaseResource> getResources() {

        return Optional.ofNullable(GlobalConfiguration.all().get(GlobalConfigurationExtension.class))
            .map(GlobalConfigurationExtension::getConfiguredResources)
            .orElse(new ArrayList<DatabaseResource>());
    }

    public DatabaseResource getResource(final String resourceName) {

        for (DatabaseResource resource : getResources()) {
            if (resource.getName().equalsIgnoreCase(resourceName)) {
                return resource;
            }
        }
        
        return null;
    }
    
    public boolean isNodeLocked(final Node node) {

        return lockedNodesByName.containsKey(node.getNodeName());
    }
    
    public synchronized Node acquireResource(
            final RequiredDatabaseResource requiredResource,
            final AbstractBuild<?, ?> build) throws InterruptedException {

        final DatabaseResource databaseResource = getDatabaseResource(requiredResource);
        
        if (databaseResource == null) {
            throw new IllegalArgumentException("The required database resource with name '"
                + requiredResource.getResourceName() + "' does not exist.");
        }
        
        Node availableNode = acquireAvailableNode(databaseResource, build);
        
        lockedNodesByName.put(availableNode.getNodeName(), availableNode);
        
        return availableNode;
    }

    public synchronized void releaseResource(final Node lockedNode) {

        lockedNodesByName.remove(lockedNode.getNodeName());
    }
    
    private DatabaseResource getDatabaseResource(RequiredDatabaseResource requiredResource) {

        for (DatabaseResource databaseResource : getResources()) {
            if (databaseResource.getName().equalsIgnoreCase(requiredResource.getResourceName())) {
                return databaseResource;
            }
        }
        
        return null;
    }
    
    private Node acquireAvailableNode(
            final DatabaseResource databaseResource,
            final AbstractBuild<?,?> build) throws InterruptedException {

        long timeoutInMs = TimeUnit.MINUTES.toMillis(getTimeoutInMinutes());
        long startTime = System.currentTimeMillis();
        
        while (true) {
            List<Node> availableNodes = Jenkins.getInstance().getLabels().stream()
                .filter(label -> databaseResource.getLabelsSet().contains(label.getExpression()))
                .flatMap(filteredLabel -> filteredLabel.getNodes().stream())
                .filter(node -> node.toComputer().isOnline())
                .filter(filteredNode -> !lockedNodesByName.containsKey(filteredNode.getNodeName()))
                .collect(Collectors.toList());
        
            if (availableNodes.isEmpty()) {
                setBuildDescription(
                    build,
                    "Waiting for next available resource from " + databaseResource.getName() + "...");
                
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

        return Optional.ofNullable(GlobalConfiguration.all().get(GlobalConfigurationExtension.class))
            .map(GlobalConfigurationExtension::getTimeoutInMinutes)
            .orElse(GlobalConfigurationExtension.DEFAULT_TIMEOUT_IN_MINUTES);
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
            throw new InterruptedException("Maximum wait time reached.");
        }
        
        executor.interrupt(Result.FAILURE);
    }
    
    /**
     * Singleton instance.
     */
    private static DatabaseResourcesManager instance;
    
    /**
     * Gets the {@link DatabaseResourcesManager} singleton. 
     * 
     * @return The instance; never null.
     */
    @Nonnull
    public static synchronized DatabaseResourcesManager getInstance() {

        if (instance == null) {
            instance = new DatabaseResourcesManager();
        }

        return instance;
    }
}
