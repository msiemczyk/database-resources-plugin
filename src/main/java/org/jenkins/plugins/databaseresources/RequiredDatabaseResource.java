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

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

/**
 * TODO: comments
 */
public class RequiredDatabaseResource extends AbstractDescribableImpl<RequiredDatabaseResource> {

    private final String resourceName;
    private final String variablePrefix;

    @DataBoundConstructor
    public RequiredDatabaseResource(
            final String resourceName,
            final String variablePrefix) {

        if (StringUtils.isBlank(resourceName)) {
            throw new IllegalArgumentException("Given resource name is blank.");
        }
        
        if (resourceDoesNotExist(resourceName)) {
            throw new IllegalArgumentException("Resource with name '" + resourceName + "' does not exist.");
        }
        
        this.resourceName = resourceName;
        this.variablePrefix = variablePrefix;
    }
    
    public String getResourceName() {
        
        return resourceName;
    }
    
    public String getVariablePrefix() {
        
        return variablePrefix;
    }
    
    private static boolean resourceDoesNotExist(final String resourceName) {

        return DatabaseResourcesManager.getInstance().getResource(resourceName) == null;
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<RequiredDatabaseResource> {

        /**
         * Checks that the resource name is given.
         *
         * @param value The string value to validate.
         * 
         * @return the validation results.
         */
        public FormValidation doCheckResourceName(
                @QueryParameter
                final String value) {
           
            final FormValidation validateRequired = FormValidation.validateRequired(value);
            
            if (validateRequired != FormValidation.ok()) {
                return validateRequired;
            }
            
            if (resourceDoesNotExist(value)) {
                return FormValidation.error("Resource with name '" + value + "' does not exist.");
            }
            
            return FormValidation.ok();
        }
        
        /**
         * This method provides auto-completion items for the 'resourceName' field.
         * Stapler finds this method via the naming convention.
         *
         * @param value The text that the user entered.
         */
        public AutoCompletionCandidates doAutoCompleteResourceName(@QueryParameter String value) {
            
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            
            DatabaseResourcesManager.getInstance().getResources().stream()
                .map(DatabaseResource::getName)
                .filter(filterdExpression -> StringUtils.containsIgnoreCase(filterdExpression, value))
                .forEach(matchedExpression -> candidates.add(matchedExpression));
            
            return candidates;
        }
    }
}
