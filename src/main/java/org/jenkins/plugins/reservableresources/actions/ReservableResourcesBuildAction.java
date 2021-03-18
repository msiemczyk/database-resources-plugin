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
package org.jenkins.plugins.reservableresources.actions;

import java.util.List;

import hudson.model.Action;

public class ReservableResourcesBuildAction implements Action {
	    
    private final List<AcquiredResource> acquiredResources;

    public ReservableResourcesBuildAction(List<AcquiredResource> acquiredResources) {

        this.acquiredResources = acquiredResources;
    }
    
    public List<AcquiredResource> getAcquiredResources() {

        return acquiredResources;
    }

    @Override
    public String getIconFileName() {

        return "/plugin/reservable-resources/images/main-icon.png";
    }

    @Override
    public String getDisplayName() {

        return "Reservable Resources";
    }

    @Override
    public String getUrlName() {

        return "reservable-resources";
    }
    
    public static final class AcquiredResource {
        
        public final String label;
        public final String nodeName;
        
        public AcquiredResource(
                final String label,
                final String nodeName) {

            this.label = label;
            this.nodeName = nodeName;
        }
    }
}
