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

import org.apache.commons.lang.StringUtils;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;

public class BuildEnvironmentContributingAction extends InvisibleAction implements EnvironmentContributingAction {
	
	private final String variablePrefix;
	
	private final String nodeName;
	private final List<Entry> nodeEnvVariables;
	
	public BuildEnvironmentContributingAction(
	        final String variablePrefix,
	        final String nodeName,
	        final List<Entry> nodeEnvVariables) {

        super();
        
        this.variablePrefix = variablePrefix;
        this.nodeName = nodeName;
        this.nodeEnvVariables = nodeEnvVariables;
    }
	
    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars environmentVariables) {

        String prefix = "";
        
        if (StringUtils.isNotBlank(variablePrefix)) {
            prefix = variablePrefix.endsWith("_") ? variablePrefix : variablePrefix + "_";
        }
            
        environmentVariables.put(prefix + "NODE_NAME", nodeName);
        
        for (Entry envVariable : nodeEnvVariables) {
            environmentVariables.put(prefix + envVariable.key, envVariable.value);
        }
    }
}
