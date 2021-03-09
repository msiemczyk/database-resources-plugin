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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.slaves.DumbSlave;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;

/**
 * This extension adds reservable resource node property for {@link DumbSlave}s.
 * 
 * @see NodeProperty
 */
public class NodePropertyExtension extends NodeProperty<DumbSlave> {

    private final List<Setting> settings;
   
    @DataBoundConstructor
    public NodePropertyExtension(List<Setting> settings) {

        super();
        
        this.settings = settings;
    }

    public List<Setting> getSettings() {

        return settings;
    }
    
    public static class Setting extends AbstractDescribableImpl<Setting> {

        public final String key;
        public final String value;

        @DataBoundConstructor
        public Setting(
                String key,
                String value) {

            if (StringUtils.isBlank(key)) {
                throw new IllegalArgumentException("Given setting key is blank.");
            }

            if (StringUtils.isBlank(value)) {
                throw new IllegalArgumentException("Given setting value is blank.");
            }
            
            this.key = key;
            this.value = value;
        }
        
        @Extension
        public static class DescriptorImpl extends Descriptor<Setting> {

            @Override
            public String getDisplayName() {

                return "";
            }
        }
    }
    
    @Extension
    public static class DescriptorImpl extends NodePropertyDescriptor {
     
        @Override
        public String getDisplayName() {
            
            return "Reservable resource";
        }
    }
}
