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
