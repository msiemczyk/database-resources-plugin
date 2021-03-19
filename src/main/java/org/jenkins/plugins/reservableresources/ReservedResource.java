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