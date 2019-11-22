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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.collect.Sets;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

/**
 * TODO: comments
 */
public class DatabaseResource extends AbstractDescribableImpl<DatabaseResource> {

    private final String name;
    private String description;
    
    private final Set<String> labels;

    @DataBoundConstructor
    public DatabaseResource(
            final String name,
            final String labels) {

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Given name is blank.");
        }
        
        if (StringUtils.isBlank(labels)) {
            throw new IllegalArgumentException("Given labels string is blank.");
        }
        
        this.name = name;
        this.labels = Sets.newHashSet(StringUtils.split(labels));
        
        if (this.labels.contains(DescriptorImpl.MASTER)) {
            throw new IllegalArgumentException("Given labels contain 'master'.");
        }
    }
    
    public String getName() {
    
        return name;
    }

    public String getLabels() {
    
        return String.join(" ", labels);
    }
    
    public Set<String> getLabelsSet() {
        
        return labels;
    }
    
    public String getDescription() {

        return description;
    }

    @DataBoundSetter
    public void setDescription(String description) {

        this.description = description;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DatabaseResource> {
        
        private static final String MASTER = "master";

        /**
         * Checks that the name is given and is unique.
         *
         * @param value The string value to validate.
         * 
         * @return the validation results.
         */
        public FormValidation doCheckName(
                @QueryParameter
                final String value) {
           
            final FormValidation validateRequired = FormValidation.validateRequired(value);
            
            if (validateRequired != FormValidation.ok()) {
                return validateRequired;
            }
            
            // FIXME: not working correctlty
//            List<DatabaseResource> configuredDatabaseResources = 
//                Optional.ofNullable(GlobalConfiguration.all().get(GlobalConfigurationExtension.class))
//                    .map(GlobalConfigurationExtension::getConfiguredResources)
//                    .orElse(new ArrayList<DatabaseResource>());
//            
//            if (configuredDatabaseResources.stream()
//                    .anyMatch(resource -> resource.getName().equalsIgnoreCase(value))) {
//                
//                return FormValidation.error("The name '" + value + "' is already defined.");
//            }

            return FormValidation.ok();
        }
        
        /**
         * Checks that at least one label is given and it is not master.
         *
         * @param value The string value to validate.
         * 
         * @return the validation results.
         */
        public FormValidation doCheckLabels(
                @QueryParameter
                final String value) {
           
            final FormValidation validateRequired = FormValidation.validateRequired(value);
            
            if (validateRequired != FormValidation.ok()) {
                return validateRequired;
            }
            
            if (Stream.of(StringUtils.split(value)).anyMatch(label -> label.equalsIgnoreCase(MASTER))) {
                return FormValidation.error("Master can not be used as a database resource.");
            }
            
            return FormValidation.ok();
        }
        
        /**
         * This method provides auto-completion items for the 'labels' field.
         * Stapler finds this method via the naming convention.
         *
         * @param value The text that the user entered.
         */
        public AutoCompletionCandidates doAutoCompleteLabels(@QueryParameter String value) {
            
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            
            Jenkins.getInstance().getLabels().stream()
                .map(Label::getExpression)
                .filter(expression -> !expression.equalsIgnoreCase(MASTER))
                .filter(filterdExpression -> StringUtils.containsIgnoreCase(filterdExpression, value))
                .forEach(matchedExpression -> candidates.add(matchedExpression));
            
            return candidates;
        }
    }
}
