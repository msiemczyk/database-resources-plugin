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

import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.slaves.DumbSlave;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;

/**
 * TODO: comments
 */
public class NodePropertyExtension extends NodeProperty<DumbSlave> {

    private final boolean copyEnvVariables;    
    private final List<Setting> settings;
   
    @DataBoundConstructor
    public NodePropertyExtension(
            boolean copyEnvVariables,
            List<Setting> settings) {

        super();
        
        this.copyEnvVariables = copyEnvVariables;
        this.settings = settings;
    }

    public boolean getCopyEnvVariables() {

        return copyEnvVariables;
    }

    public List<Setting> getSettings() {

        return settings;
    }

    public static class Setting {

        public final String key;
        public final String value;

//        private Setting(Map.Entry<String, String> e) {
//
//            this(e.getKey(), e.getValue());
//        }

        @DataBoundConstructor
        public Setting(
                String key,
                String value) {

            this.key = key;
            this.value = value;
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
