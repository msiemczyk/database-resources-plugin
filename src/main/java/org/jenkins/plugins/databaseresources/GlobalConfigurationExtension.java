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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

/**
 * TODO: comments
 */
@Extension
public class GlobalConfigurationExtension extends GlobalConfiguration {
    
    public static final long DEFAULT_TIMEOUT_IN_MINUTES = TimeUnit.HOURS.toMinutes(12);

    private long timeoutInMinutes = DEFAULT_TIMEOUT_IN_MINUTES;
    private List<DatabaseResource> configuredResources;
    
    public GlobalConfigurationExtension() {
    
        load();
    }
    
    public long getTimeoutInMinutes() {

        return timeoutInMinutes;
    }

    public void setTimeoutInMinutes(long timeoutInMinutes) throws FormException {

        if (timeoutInMinutes < 1) {
            throw new FormException("Given timeout in minutes must be a positive number.", "timeoutInMinutes");
        }
        
        this.timeoutInMinutes = timeoutInMinutes;
    }
    
    public List<DatabaseResource> getConfiguredResources() {

        return configuredResources;
    }

    @Override
    public boolean configure(
            final StaplerRequest request,
            final JSONObject json) throws FormException {
        
        setTimeoutInMinutes(json.getLong("timeoutInMinutes"));
        
        // Stapler oddity, empty lists coming from the HTTP request are not set on bean by "req.bindJSON(this, json)".
        configuredResources = request.bindJSONToList(DatabaseResource.class, json.get("configuredResources"));
        
        save();
        return true;
    }
}
