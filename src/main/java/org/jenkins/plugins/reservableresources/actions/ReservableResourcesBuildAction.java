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
