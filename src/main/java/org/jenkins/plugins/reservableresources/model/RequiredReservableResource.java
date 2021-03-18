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
