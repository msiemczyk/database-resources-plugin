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

import static org.jenkins.plugins.databaseresources.DatabaseResourcesManager.LOG_PREFIX;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jenkins.plugins.databaseresources.actions.BuildEnvironmentContributingAction;
import org.jenkins.plugins.databaseresources.actions.DatabaseResourcesBuildAction;
import org.jenkins.plugins.databaseresources.actions.DatabaseResourcesBuildAction.AcquiredResource;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.BuildWrapper;

/**
 * TODO: might have to change this to QueueTaskDispatcher for better visibility.
 * 
 * @see BuildWrapper
 */
public class DatabaseResourcesBuildWrapper extends BuildWrapper {

    private final List<RequiredDatabaseResource> requiredResources;

    @DataBoundConstructor
    public DatabaseResourcesBuildWrapper(List<RequiredDatabaseResource> resources) {
        
        this.requiredResources = resources;
    }

    public List<RequiredDatabaseResource> getResources() {
        
        return requiredResources;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Environment setUp(
            final AbstractBuild build, 
            final Launcher launcher,
            final BuildListener listener) throws IOException, InterruptedException {

        final PrintStream logger = listener.getLogger();
        
        Map<String, Node> lockedNodesByResourceName = new HashMap<>();
        List<AcquiredResource> acquiredResources = new ArrayList<>(requiredResources.size());
        
        for (RequiredDatabaseResource requiredResource : requiredResources) {
            final String resourceName = requiredResource.getResourceName();
            
            logger.println(LOG_PREFIX + "Acquiring a resource from '" + resourceName + "'...");

            Node node = DatabaseResourcesManager.getInstance().acquireResource(requiredResource, build);
            lockedNodesByResourceName.put(resourceName, node);
            
            acquiredResources.add(
                new AcquiredResource(
                    resourceName, 
                    DatabaseResourcesManager.getInstance().getResource(resourceName).getDescription(),
                    lockedNodesByResourceName.get(resourceName).getNodeName()));
            
            List<EnvironmentVariablesNodeProperty.Entry> nodeEnvVariables = Optional.ofNullable(
                    node.getNodeProperties().get(EnvironmentVariablesNodeProperty.class))
                .map(EnvironmentVariablesNodeProperty::getEnv)
                .orElse(new ArrayList<>());

            build.addAction(new BuildEnvironmentContributingAction(
                requiredResource.getVariablePrefix(), node.getNodeName(), nodeEnvVariables));
            
            logger.println(
                LOG_PREFIX + "Successfully acquired '" + node.getNodeName() + "' from '" + resourceName + "'.");
        }
        
        build.addAction(new DatabaseResourcesBuildAction(acquiredResources));
        
        return new Environment() {
            
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {

                for (Node lockedNode : lockedNodesByResourceName.values()) {
                    DatabaseResourcesManager.getInstance().releaseResource(lockedNode);
                    
                    logger.println(LOG_PREFIX + "Released the '" + lockedNode.getNodeName() + "' resource.");
                }
                
                return super.tearDown(build, listener);
            }
        };
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BuildWrapper> {

        @Override
        public String getDisplayName() {

            return "This build requires database resource(s)";
        }
    }
}
