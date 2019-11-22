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

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.slaves.DumbSlave;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;

/**
 * TODO: comments
 */
public class NodePropertyExtension extends NodeProperty<DumbSlave> {

    private final String vmName;
   
    @DataBoundConstructor
    public NodePropertyExtension(final String vmName) {

        super();
        
        this.vmName = vmName;
    }

    public String getVmName() {

        return vmName;
    }

    @Extension
    public static class DescriptorImpl extends NodePropertyDescriptor {
     
        @Override
        public String getDisplayName() {
            
            return "Database resource";
        }
    }
}
