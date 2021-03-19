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
package org.jenkins.plugins.reservableresources.model;

import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.reservableresources.ReservableResourcesManager;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

/**
 * This class describes metadata information about one resource, possibly of many,
 * configured for one particular Jenkins job.
 * 
 * @see Descriptor
 */
public class RequiredReservableResource extends AbstractDescribableImpl<RequiredReservableResource> {

    private final String resourceLabel;
    private final String envVariablesPrefix;

    @DataBoundConstructor
    public RequiredReservableResource(
            final String resourceLabel,
            final String envVariablesPrefix) {

        if (StringUtils.isBlank(resourceLabel)) {
            throw new IllegalArgumentException("Given resource label is blank.");
        }

        if (StringUtils.isBlank(envVariablesPrefix)) {
            throw new IllegalArgumentException("Given environment variable prefix is blank.");
        }
        
        this.resourceLabel = resourceLabel;
        this.envVariablesPrefix = envVariablesPrefix;
    }
    
    public String getResourceLabel() {
        
        return resourceLabel;
    }
    
    public String getEnvVariablePrefix() {
        
        return envVariablesPrefix;
    }

    @Override
    public String toString() {

        return "RequiredReservableResource [resourceLabel=" + resourceLabel
                + ", envVariablesPrefix=" + envVariablesPrefix + "]";
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<RequiredReservableResource> {

        /**
         * Checks that the resource label is given.
         *
         * @param value The string value to validate.
         * 
         * @return the validation results.
         */
        public FormValidation doCheckResourceLabel(
                @QueryParameter
                final String value) {
           
            final FormValidation validateRequired = FormValidation.validateRequired(value);
            
            if (validateRequired != FormValidation.ok()) {
                return validateRequired;
            }
            
            if (resourceDoesNotExist(value)) {
                return FormValidation.error("Therea are no reservable resources with label '" + value + "'.");
            }
            
            return FormValidation.ok();
        }
        
        /**
         * This method provides auto-completion items for the 'resourceLabel' field.
         * Stapler finds this method via the naming convention.
         *
         * @param value The text that the user entered.
         */
        public AutoCompletionCandidates doAutoCompleteResourceLabel(@QueryParameter String value) {
            
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            
            ReservableResourcesManager.getInstance().getReservableNodes().stream()
                .flatMap(node -> Stream.of(StringUtils.split(node.getLabelString())))
                .distinct()
                .filter(label -> StringUtils.containsIgnoreCase(label, value))
                .forEach(candidates::add);
            
            return candidates;
        }
        
        private boolean resourceDoesNotExist(final String resourceLabel) {

            return ReservableResourcesManager.getInstance().getReservableNodes().stream()
                .flatMap(node -> Stream.of(StringUtils.split(node.getLabelString())))
                .noneMatch(resourceLabel::equals);
        }
    }
}
