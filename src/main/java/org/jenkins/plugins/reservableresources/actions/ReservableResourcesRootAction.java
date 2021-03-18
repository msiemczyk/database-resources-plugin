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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jenkins.plugins.reservableresources.ReservableResourcesManager;
import org.jenkins.plugins.reservableresources.ReservedResource;
import org.jenkins.plugins.reservableresources.ReservedResource.ReservedBy;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.RootAction;

@Extension
public class ReservableResourcesRootAction implements RootAction {

    @Override
    public String getDisplayName() {

        return "Reservable Resources";
    }

    @Override
    public String getIconFileName() {

        return "/plugin/reservable-resources/images/main-icon.png";
    }

    @Override
    public String getUrlName() {

        return "reservable-resources";
    }
    
    @POST
    public HttpResponse doReserve(
            @QueryParameter
            final String nodeName) {
        
        ReservableResourcesManager.getInstance().reserveResource(nodeName);
        
        return HttpResponses.forwardToPreviousPage(); 
    }
    
    @POST
    public HttpResponse doRelease(
            @QueryParameter
            final String nodeName) {
        
        ReservableResourcesManager.getInstance().releaseResource(nodeName);
        
        return HttpResponses.forwardToPreviousPage(); 
    }

    public static Map<String, LabelInfo> getInfosByLabel() {

        Map<String, List<ResourceInfo>> resourcesInfosByLabel =
            ReservableResourcesManager.getInstance().getReservableNodes().stream()
                .collect(Collectors.groupingBy(
                    Node::getLabelString,
                    Collectors.mapping(node -> new ResourceInfo(
                            node,
                            ReservableResourcesManager.getInstance().getReservedInfo(node)),
                        Collectors.toList())));
        
        Map<String, LabelInfo> labelInfos = new HashMap<>(resourcesInfosByLabel.size());
        
        for (Entry<String, List<ResourceInfo>> entry : resourcesInfosByLabel.entrySet()) {
            List<AbstractBuild<?, ?>> queueBuilds =
                ReservableResourcesManager.getInstance().getBuildQueueBuilds(entry.getKey());
            
            labelInfos.put(entry.getKey(), new LabelInfo(queueBuilds, entry.getValue()));
        }
        
        return labelInfos;
    }
    
    public static final class LabelInfo {
     
        public final List<AbstractBuild<?, ?>> buildQueue;
        public final List<ResourceInfo> resourceInfos;
        
        public LabelInfo(
                List<AbstractBuild<?, ?>> buildQueue,
                List<ResourceInfo> resourceInfos) {

            this.buildQueue = buildQueue;
            this.resourceInfos = resourceInfos;
        }        
    }
    
    public static final class ResourceInfo {
        
        public final Node node;
        public final Computer computer;
        
        public final ReservedBy reservedBy;
        
        public ResourceInfo(
                Node node,
                Optional<ReservedResource> reservedResource) {

            this.node = node;
            this.computer = node.toComputer();
            this.reservedBy = reservedResource.map(ReservedResource::getReservedBy).orElse(null);
        }
    }
}
