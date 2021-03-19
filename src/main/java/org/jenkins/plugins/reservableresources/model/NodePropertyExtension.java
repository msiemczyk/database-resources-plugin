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
