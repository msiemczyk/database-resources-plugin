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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.reservableresources.model.NodePropertyExtension;
import org.jenkins.plugins.reservableresources.model.NodePropertyExtension.Setting;

import hudson.EnvVars;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;
import hudson.model.Node;
import hudson.model.Run;

public class BuildEnvironmentContributingAction extends InvisibleAction implements EnvironmentContributingAction {
	
	private static final String NODE_NAME = "NODE_NAME";

    private final String variablePrefix;
	
	private final String nodeName;
	private final List<Setting> nodeEnvVariables;
	
	public BuildEnvironmentContributingAction(
	        final String variablePrefix,
	        final Node node) {

        super();
        
        this.variablePrefix = variablePrefix;
        this.nodeName = node.getNodeName();
        
        this.nodeEnvVariables = Optional.ofNullable(
                node.getNodeProperties().get(NodePropertyExtension.class))
            .map(NodePropertyExtension::getSettings)
            .orElse(new ArrayList<>());
    }
	
    @Override
    public void buildEnvironment(
            final Run<?, ?> run,
            final EnvVars environmentVariables) {
        
        String prefix = "";
        
        if (StringUtils.isNotBlank(variablePrefix)) {
            prefix = variablePrefix.endsWith("_") ? variablePrefix : variablePrefix + "_";
        }
            
        environmentVariables.put(prefix + NODE_NAME, nodeName);
        
        for (Setting envVariable : nodeEnvVariables) {
            environmentVariables.put(prefix + envVariable.key, envVariable.value);
        }
    }
}
