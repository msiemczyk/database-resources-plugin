/*
 * Copyright (c) 2021 Maciek Siemczyk
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.plugins.reservableresources;

import static org.jenkins.plugins.reservableresources.ReservableResourcesManager.LOG_PREFIX;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jenkins.plugins.reservableresources.actions.BuildEnvironmentContributingAction;
import org.jenkins.plugins.reservableresources.actions.ReservableResourcesBuildAction;
import org.jenkins.plugins.reservableresources.actions.ReservableResourcesBuildAction.AcquiredResource;
import org.jenkins.plugins.reservableresources.model.RequiredReservableResource;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.util.FormValidation;

/**
 * Reservable resources build wrapper is responsible for acquiring and releasing
 * of the resources for the individual builds ({@link Run}).
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
        
        List<AcquiredResource> acquiredResources = new ArrayList<>(requiredResources.size());

        try {
            for (RequiredReservableResource requiredResource : requiredResources) {
                final String label = requiredResource.getResourceLabel();
                
                logger.println(LOG_PREFIX + "Acquiring a resource from '" + label + "'...");

                setBuildDescription(
                    build,
                    "Waiting for next available resource from '" + label + "'...");
                
                Node node = ReservableResourcesManager.getInstance()
                    .acquireResource(timeoutInMinutes, requiredResource, build);
                
                acquiredResources.add(new AcquiredResource(label, node.getNodeName()));
                
                build.addAction(new BuildEnvironmentContributingAction(requiredResource.getEnvVariablePrefix(), node));
                
                logger.println(
                    LOG_PREFIX + "Successfully acquired '" + node.getNodeName() + "' from '" + label + "'.");
            }
            
            setBuildDescription(build, "");
            
            build.addAction(new ReservableResourcesBuildAction(acquiredResources));
        }
        catch (TimeoutException exception) {
            final String message = "Aborted waiting for resource due "
                + "to reaching time-out of " + timeoutInMinutes + " minutes.";
            
            logger.println(LOG_PREFIX + message);
            
            releaseAcquiredResources(logger, acquiredResources);
            
            setBuildDescription(build, message);
            abortBuild(build, message);
            return null;
        }
        catch (Exception exception) {
            releaseAcquiredResources(logger, acquiredResources);
            
            // Re-throw the exception or the build will proceed.
            throw exception;
        }
        
        return new Environment() {
            
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {

                releaseAcquiredResources(logger, acquiredResources);
                
                return super.tearDown(build, listener);
            }
        };
    }
    
    private void releaseAcquiredResources(final PrintStream logger, List<AcquiredResource> acquiredResources) {

        for (AcquiredResource acquiredResource : acquiredResources) {
            ReservableResourcesManager.getInstance().releaseResource(acquiredResource.nodeName);
            
            logger.println(LOG_PREFIX + "Released the '" + acquiredResource.nodeName + "' resource.");
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
    
    private void abortBuild(
            final AbstractBuild<?, ?> build,
            final String message) throws InterruptedException {

        Executor executor = build.getExecutor();

        if (executor == null) {
            throw new InterruptedException(message);
        }

        // TODO: in the future maybe add CauseOfInterruption argument
        executor.interrupt(Result.FAILURE);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BuildWrapper> {

        @Override
        public String getDisplayName() {

            return "This build requires reservable resource(s)";
        }
        
        public int defaultTimeout() {
            
            return DEFAULT_TIMEOUT_IN_MINUTES;
        }
        
        /**
         * Checks that the time-out is given and is a positive number.
         *
         * @param value The string value to validate.
         * 
         * @return the validation results.
         */
        public FormValidation doCheckTimeoutInMinutes(
                @QueryParameter
                final String value) {
           
            final FormValidation validateRequired = FormValidation.validateRequired(value);
            
            if (validateRequired != FormValidation.ok()) {
                return validateRequired;
            }
            
            return FormValidation.validatePositiveInteger(value);
        }
    }
}
