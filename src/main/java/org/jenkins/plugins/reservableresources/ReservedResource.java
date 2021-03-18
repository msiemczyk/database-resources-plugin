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
package org.jenkins.plugins.reservableresources;

import hudson.model.AbstractBuild;
import hudson.model.Node;

/**
 * Simple POJO storing information about a reserved resource.
 */
public final class ReservedResource {
    
    private final Node node;
    private final ReservedBy reservedBy;
    
    /**
     * Constructor for reservation made by a build.
     * 
     * @param node Reserved node.
     * @param build Build making reservation.
     */
    public ReservedResource(
            Node node,
            AbstractBuild<?, ?> build) {

        this.node = node;
        this.reservedBy = new ReservedBy(build.toString(), build);
    }
    
    /**
     * Constructor for reservation made by a user.
     * 
     * @param node Reserved node.
     * @param build Name of the user making reservation.
     */
    public ReservedResource(
            Node node,
            String reservedBy) {

        this.node = node;
        this.reservedBy = new ReservedBy(reservedBy, null);
    }

    public Node getNode() {
    
        return node;
    }

    public ReservedBy getReservedBy() {
    
        return reservedBy;
    }

    public static final class ReservedBy {
        
        private final String displayName;
        private final AbstractBuild<?, ?> build;
        
        public ReservedBy(
                String displayName,
                AbstractBuild<?,?> build) {

            this.displayName = displayName;
            this.build = build;
        }

        public String getDisplayName() {

            return displayName;
        }

        @SuppressWarnings("java:S1452")
        public AbstractBuild<?, ?> getBuild() {

            return build;
        }
    }
}