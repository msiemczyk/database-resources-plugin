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
package org.jenkins.plugins.reservableresources;

import static org.jenkins.plugins.reservableresources.ReservableResourcesManager.LOG_PREFIX;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jenkins.plugins.reservableresources.actions.BuildEnvironmentContributingAction;
import org.jenkins.plugins.reservableresources.actions.ReservableResourcesBuildAction;
import org.jenkins.plugins.reservableresources.actions.ReservableResourcesBuildAction.AcquiredResource;
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
public class ReservableResourcesBuildWrapper extends BuildWrapper {

    private static final int DEFAULT_TIMEOUT_IN_MINUTES = 180;
    
    private final int timeoutInMinutes;
    private final List<RequiredReservableResource> requiredResources;

    @DataBoundConstructor
    public ReservableResourcesBuildWrapper(
            int timeoutInMinutes,
            List<RequiredReservableResource> resources) {
        
        if (timeoutInMinutes < 1) {
            throw new IllegalArgumentException("Given timeout in minutes (" + timeoutInMinutes + ") is not positive.");
        }
        
        this.timeoutInMinutes = timeoutInMinutes;
        this.requiredResources = resources;
    }

    public long getTimeoutInMinutes() {

        return timeoutInMinutes;
    }
    
    public List<RequiredReservableResource> getResources() {
        
        return requiredResources;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Environment setUp(
            final AbstractBuild build, 
            final Launcher launcher,
            final BuildListener listener) throws IOException, InterruptedException {

        final PrintStream logger = listener.getLogger();
        
        Map<String, Node> reservedNodesByLabel = new HashMap<>();
        List<AcquiredResource> acquiredResources = new ArrayList<>(requiredResources.size());
        
        for (RequiredReservableResource requiredResource : requiredResources) {
            final String label = requiredResource.getResourceLabel();
            
            logger.println(LOG_PREFIX + "Acquiring a resource from '" + label + "'...");

            Node node = ReservableResourcesManager.getInstance().acquireResource(requiredResource, build); // timeoutInMinutes
            reservedNodesByLabel.put(label, node);
            
            acquiredResources.add(
                new AcquiredResource(
                    label, 
                    ReservableResourcesManager.getInstance().getResource(label).getDescription(),
                    reservedNodesByLabel.get(label).getNodeName()));
            
            NodePropertyExtension nodeProperties = node.getNodeProperties().get(NodePropertyExtension.class);
//                .map(NodePropertyExtension::getVmName)
//                .orElse(node.getNodeName());
            
            nodeProperties.getSettings()
            
            if (nodeProperties.getCopyEnvVariables()) {
                List<EnvironmentVariablesNodeProperty.Entry> nodeEnvVariables = Optional.ofNullable(
                        node.getNodeProperties().get(EnvironmentVariablesNodeProperty.class))
                    .map(EnvironmentVariablesNodeProperty::getEnv)
                    .orElse(new ArrayList<>());
            }

            build.addAction(new BuildEnvironmentContributingAction(
                requiredResource.getEnvVariablePrefix(), node.getNodeName(), nodeEnvVariables));
            
            logger.println(
                LOG_PREFIX + "Successfully acquired '" + node.getNodeName() + "' from '" + label + "'.");
        }
        
        build.addAction(new ReservableResourcesBuildAction(acquiredResources));
        
        return new Environment() {
            
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {

                for (Node lockedNode : reservedNodesByLabel.values()) {
                    ReservableResourcesManager.getInstance().releaseResource(lockedNode);
                    
                    logger.println(LOG_PREFIX + "Released the '" + lockedNode.getNodeName() + "' resource.");
                }
                
                return super.tearDown(build, listener);
            }
        };
    }

//  public void setTimeoutInMinutes(long timeoutInMinutes) throws FormException {
//
//      if (timeoutInMinutes < 1) {
//          throw new FormException("Given timeout in minutes must be a positive number.", "timeoutInMinutes");
//      }
//      
//      this.timeoutInMinutes = timeoutInMinutes;
//  }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<BuildWrapper> {

        @Override
        public String getDisplayName() {

            return "This build requires reservable resource(s)";
        }
        
        public int defaultTimeout() {
            
            return DEFAULT_TIMEOUT_IN_MINUTES;
        }
    }
}
