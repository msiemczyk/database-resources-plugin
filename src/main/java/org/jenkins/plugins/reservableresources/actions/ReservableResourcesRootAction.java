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
