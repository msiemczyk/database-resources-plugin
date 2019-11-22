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
package org.jenkins.plugins.databaseresources.actions;

import org.apache.commons.lang.StringUtils;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

public class BuildEnvironmentContributingAction extends InvisibleAction implements EnvironmentContributingAction {
	
	private final String variablePrefix;
	private final String hostName;
	private final String sid;
    private final String vmName;
	
	public BuildEnvironmentContributingAction(
	        final String variablePrefix,
	        final String hostName,
	        final String sid,
	        final String vmName) {

        super();
        
        this.variablePrefix = variablePrefix;
        this.hostName = hostName;
        this.sid = sid;
        this.vmName = vmName;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars environmentVariables) {

        String prefix = "";
        
        if (StringUtils.isNotBlank(variablePrefix)) {
            prefix = variablePrefix.endsWith("_") ? variablePrefix : variablePrefix + "_";
        }
            
        environmentVariables.put(prefix + "DB_HOST_NAME", hostName);
        environmentVariables.put(prefix + "DB_HOST_SID", sid);
        environmentVariables.put(prefix + "DB_VM_NAME", vmName);
    }
}
