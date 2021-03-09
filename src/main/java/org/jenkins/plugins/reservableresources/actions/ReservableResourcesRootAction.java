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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.reservableresources.ReservableResourcesManager;
import org.jenkins.plugins.reservableresources.ReservableResourcesManager.ReservedResource;
import org.jenkins.plugins.reservableresources.ReservableResourcesManager.ReservedResource.ReservedBy;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import hudson.Extension;
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
    public HttpResponse doUnreserve(
            @QueryParameter
            final String nodeName) {
        
        if (StringUtils.isBlank(nodeName)) {
            throw new IllegalArgumentException("Node name parameter is blank.");
        }

        ReservableResourcesManager.getInstance().releaseResource(nodeName);
        
        return HttpResponses.forwardToPreviousPage(); 
    }

    public static Map<String, List<ResourceInfo>> getResourceInfos() {

        return ReservableResourcesManager.getInstance().getReservableNodes().stream()
            .collect(Collectors.groupingBy(
                Node::getLabelString,
                Collectors.mapping(node -> new ResourceInfo(
                        node,
                        ReservableResourcesManager.getInstance().getReservedInfo(node)),
                    Collectors.toList())));
    }
    
    public static final class ResourceInfo {
        
        private final Node node;
        private final Computer computer;
        
        private final ReservedBy reservedBy;
        
        public ResourceInfo(
                Node node,
                Optional<ReservedResource> reservedResource) {

            this.node = node;
            this.computer = node.toComputer();
            this.reservedBy = reservedResource.map(resource -> resource.reservedBy).orElse(null);
        }

        public Node getNode() {
        
            return node;
        }
        
        public Computer getComputer() {

            return computer;
        }

        public ReservedBy getReservedBy() {
        
            return reservedBy;
        }
    }
}
