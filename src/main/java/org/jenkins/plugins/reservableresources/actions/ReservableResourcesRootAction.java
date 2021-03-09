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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.reservableresources.ReservableResourcesManager;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import hudson.Extension;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.RootAction;
import jenkins.model.Jenkins;

@Extension
public class ReservableResourcesRootAction implements RootAction {

//	public static final PermissionGroup PERMISSIONS_GROUP = new PermissionGroup(
//			LockableResourcesManager.class, Messages._LockableResourcesRootAction_PermissionGroup());
//	public static final Permission UNLOCK = new Permission(PERMISSIONS_GROUP,
//			Messages.LockableResourcesRootAction_UnlockPermission(),
//			Messages._LockableResourcesRootAction_UnlockPermission_Description(), Jenkins.ADMINISTER,
//			PermissionScope.JENKINS);
//	public static final Permission RESERVE = new Permission(PERMISSIONS_GROUP,
//			Messages.LockableResourcesRootAction_ReservePermission(),
//			Messages._LockableResourcesRootAction_ReservePermission_Description(), Jenkins.ADMINISTER,
//			PermissionScope.JENKINS);
//
//	public static final Permission VIEW = new Permission(PERMISSIONS_GROUP,
//			Messages.LockableResourcesRootAction_ViewPermission(),
//			Messages._LockableResourcesRootAction_ViewPermission_Description(), Jenkins.ADMINISTER,
//			PermissionScope.JENKINS);
	
    @Override
    public String getDisplayName() {

        return "Reservable Resources";
    }

//    @Override
//    public String getDescription() {
//        
//        return "Manage and monitor database resources that are defined in global settings.";
//    }
    
    // ublic static final String ICON = "/plugin/lockable-resources/img/device-24x24.png";
    @Override
    public String getIconFileName() {

        //return (Jenkins.getInstance().hasPermission(VIEW)) ? ICON : null;
        return "/plugin/reservable-resources/images/48x48/database-icon.png";
    }

    @Override
    public String getUrlName() {

        //return (Jenkins.getInstance().hasPermission(VIEW)) ? "lockable-resources" : "";
        return "reservable-resources";
    }
    
    @POST
    public HttpResponse doUnlock(
            @QueryParameter
            final String nodeName) {
        
        if (StringUtils.isBlank(nodeName)) {
            throw new IllegalArgumentException("Node name parameter is blank.");
        }
        
        Node node = Jenkins.get().getNode(nodeName);
        
        if (node == null) {
            throw new IllegalArgumentException("Node with name '" + nodeName + "' does not exist.");
        }
        
        ReservableResourcesManager.getInstance().releaseResource(node);
        
        return HttpResponses.forwardToPreviousPage(); 
    }

    public static List<DatabaseResourceInfo> getResourceInfos() {

        final List<DatabaseResourceInfo> resourceInfos = new ArrayList<>();
        
        Set<Label> labels = Jenkins.get().getLabels();
        
//        for (ReservableResource resource : ReservableResourcesManager.getInstance().getResources()) {
//            DatabaseResourceInfo info = new DatabaseResourceInfo(resource.getName(), resource.getDescription());
//                
//            Map<String, List<NodeInfo>> nodeInfosByLabel = new HashMap<>();
//            info.setNodeInfosByLabel(nodeInfosByLabel);
//            
//            for (String resourceLabel : resource.getLabelsSet()) {
//                List<NodeInfo> nodeInfos = new ArrayList<>();
//                
//                labels.stream()
//                    .filter(label -> label.getExpression().equalsIgnoreCase(resourceLabel))
//                    .flatMap(filteredLabel -> filteredLabel.getNodes().stream())
//                    .forEach(node -> nodeInfos.add(new NodeInfo(
//                        node.getNodeName(),
//                        ReservableResourcesManager.getInstance().isNodeReserved(node))));
//                
//                nodeInfosByLabel.put(resourceLabel, nodeInfos);
//            }
//
//            resourceInfos.add(info);
//        }
        
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
    
//    @POST
//    public void doConfigure(StaplerRequest req, StaplerResponse rsp) throws Descriptor.FormException, IOException, ServletException {
//        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
//        JSONObject json = req.getSubmittedForm();
//        Jenkins.get().clouds.rebuildHetero(req,json, Cloud.all(), "cloud");
//        FormApply.success(req.getContextPath() + "/manage").generateResponse(req, rsp, null);
//    }
    
//	public String getUserName() {
//		User current = User.current();
//		if (current != null)
//			return current.getFullName();
//		else
//			return null;
//	}

//	public List<LockableResource> getResources() {
//		return LockableResourcesManager.get().getResources();
//	}
//
//	public int getFreeResourceAmount(String label) {
//		return LockableResourcesManager.get().getFreeResourceAmount(label);
//	}
//
//	public Set<String> getAllLabels() {
//		return LockableResourcesManager.get().getAllLabels();
//	}
//
//	public int getNumberOfAllLabels() {
//		return LockableResourcesManager.get().getAllLabels().size();
//	}

}
