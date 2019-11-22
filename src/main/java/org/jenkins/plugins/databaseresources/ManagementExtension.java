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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.verb.POST;

import hudson.Extension;
import hudson.model.Label;
import hudson.model.ManagementLink;
import hudson.model.Node;
import jenkins.model.Jenkins;

/**
 * TODO: comments
 */
@Extension
@ExportedBean
public class ManagementExtension extends ManagementLink {

    @Override
    public String getDisplayName() {

        return "Database Resources";
    }

    @Override
    public String getDescription() {
        
        return "Manage and monitor database resources that are defined in global settings.";
    }
    
    @Override
    public String getIconFileName() {

        return "/plugin/database-resources/images/48x48/database-icon.png";
    }

    @Override
    public String getUrlName() {

        return "database-resources";
    }
    
    @POST
    public HttpResponse doUnlock(
            @QueryParameter
            final String nodeName) {
        
        if (StringUtils.isBlank(nodeName)) {
            throw new IllegalArgumentException("Node name parameter is blank.");
        }
        
        Node node = Jenkins.getInstance().getNode(nodeName);
        
        if (node == null) {
            throw new IllegalArgumentException("Node with name '" + nodeName + "' does not exist.");
        }
        
        DatabaseResourcesManager.getInstance().releaseResource(node);
        
        return HttpResponses.forwardToPreviousPage(); 
    }

    public static List<DatabaseResourceInfo> getResourceInfos() {

        final List<DatabaseResourceInfo> resourceInfos = new ArrayList<>();
        
        Set<Label> labels = Jenkins.getInstance().getLabels();
        
        for (DatabaseResource resource : DatabaseResourcesManager.getInstance().getResources()) {
            DatabaseResourceInfo info = new DatabaseResourceInfo(resource.getName(), resource.getDescription());
                
            Map<String, List<NodeInfo>> nodeInfosByLabel = new HashMap<>();
            info.setNodeInfosByLabel(nodeInfosByLabel);
            
            for (String resourceLabel : resource.getLabelsSet()) {
                List<NodeInfo> nodeInfos = new ArrayList<>();
                
                labels.stream()
                    .filter(label -> label.getExpression().equalsIgnoreCase(resourceLabel))
                    .flatMap(filteredLabel -> filteredLabel.getNodes().stream())
                    .forEach(node -> nodeInfos.add(new NodeInfo(
                        node.getNodeName(),
                        DatabaseResourcesManager.getInstance().isNodeLocked(node))));
                
                nodeInfosByLabel.put(resourceLabel, nodeInfos);
            }

            resourceInfos.add(info);
        }
        
        return resourceInfos;
    }

    public static final class DatabaseResourceInfo {

        private final String name;
        private final String description;
        
        private Map<String, List<NodeInfo>> nodeInfosByLabel;

        public DatabaseResourceInfo(
                final String name,
                final String description) {

            this.name = name;
            this.description = description;
        }

        public String getName() {
        
            return name;
        }
        
        public String getDescription() {
        
            return description;
        }

        public Map<String, List<NodeInfo>> getNodeInfosByLabel() {

            return nodeInfosByLabel;
        }

        public void setNodeInfosByLabel(Map<String, List<NodeInfo>> nodeInfosByLabel) {

            this.nodeInfosByLabel = nodeInfosByLabel;
        }
    }
    
    public static final class NodeInfo {
        
        private final String name;
        private final boolean isLocked;
        
        public NodeInfo(String name, boolean isLocked) {

            this.name = name;
            this.isLocked = isLocked;
        }

        public String getName() {
        
            return name;
        }
        
        public boolean getIsLocked() {
        
            return isLocked;
        }
    }
}
