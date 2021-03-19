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
package org.jenkins.plugins.reservableresources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkins.plugins.reservableresources.model.NodePropertyExtension;
import org.jenkins.plugins.reservableresources.model.RequiredReservableResource;

import hudson.model.AbstractBuild;
import hudson.model.Node;
import jenkins.model.Jenkins;

/**
 * This singleton manages the reservation of resources and keeps track of everything.
 */
public class ReservableResourcesManager {

    // TODO: add some more logging as well
    private static final Logger log = Logger.getLogger(ReservableResourcesManager.class.getName());
    
    public static final String LOG_PREFIX = "[reservable-resources] ";
   
    private final Map<String, BuildQueue> buildQueuesByLabel = new ConcurrentHashMap<>();
    private final Map<String, ReservedResource> reservedByNodeName = new ConcurrentHashMap<>();
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ReservableResourcesManager() {
    
    }
    
    /**
     * Acquire a reservable resource.
     * 
     * @param timeoutInMinutes Integer representing maximum wait time to acquire the resource.
     * @param requiredResource Metadata information about required resource.
     * @param build Reference to {@link AbstractBuild} object that is reserving this resource.
     * 
     * @return Reference to acquired {@link Node}; never null.
     * 
     * @throws InterruptedException if build is aborted.
     * @throws TimeoutException if build is aborted due to a time-out.
     */
    public Node acquireResource(
            final int timeoutInMinutes,
            final RequiredReservableResource requiredResource,
            final AbstractBuild<?, ?> build) throws InterruptedException, TimeoutException {

        log.fine("About to acquire " + requiredResource + ".");
        
        final String label = requiredResource.getResourceLabel();
        
        List<Node> reservableNodes = getReservableNodes(label);
        
        if (reservableNodes.isEmpty()) {
            throw new IllegalArgumentException("The are no reservable nodes with label '" + label + "'.");
        }
        
        return buildQueuesByLabel.computeIfAbsent(label, k -> new BuildQueue(label))
            .acquireAvailableNode(timeoutInMinutes, build);
    }

    /**
     * Manually reserve a node resource.
     * 
     * @param nodeName String representing node name of the resource.
     * 
     * @throws IllegalArgumentException if resource with given node name does not exit.
     * @throws IllegalStateException if the resource is already reserved.
     */
    public void reserveResource(final String nodeName) {
        
        Node node = Jenkins.get().getNodes().stream()
            .filter(unfilteredNode -> unfilteredNode.getNodeProperty(NodePropertyExtension.class) != null)
            .filter(resourceNode -> resourceNode.getNodeName().equals(nodeName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("There is no node resource with given name."));
                
        synchronized (reservedByNodeName) {
            if (reservedByNodeName.containsKey(nodeName)) {
                throw new IllegalStateException("Resource with node name '" + nodeName + "' is already reserved.");
            }
            
            reservedByNodeName.put(nodeName, new ReservedResource(node, Jenkins.getAuthentication().getName()));    
        }
    }
    
    /**
     * Releases reserved resource.
     * 
     * @param nodeName String representing node name of the resource.
     * 
     * @throws IllegalArgumentException if given node name is blank.
     */
    public synchronized void releaseResource(final String nodeName) {

        if (StringUtils.isBlank(nodeName)) {
            throw new IllegalArgumentException("Given node name is blank.");
        }
        
        reservedByNodeName.remove(nodeName);
    }
    
    public List<Node> getReservableNodes() {

        return Jenkins.get().getNodes().stream()
            .filter(node -> node.getNodeProperty(NodePropertyExtension.class) != null)
            .collect(Collectors.toList());
    }
    
    public Optional<ReservedResource> getReservedInfo(final Node node) {

        return Optional.ofNullable(reservedByNodeName.get(node.getNodeName())); 
    }
    
    @SuppressWarnings("java:S1452")
    public List<AbstractBuild<?, ?>> getBuildQueueBuilds(final String nodeLabelString) {
        
        List<AbstractBuild<?, ?>> queueBuilds = buildQueuesByLabel.values().stream()
            .filter(queue -> nodeLabelString.contains(queue.label))
            .flatMap(filteredQueue -> filteredQueue.getQueueBuilds().stream())
            .collect(Collectors.toList());
        
        log.log(
            Level.FINEST,
            "Got following builds {0} for node label string {1}.",
            new Object[] { queueBuilds, nodeLabelString });
        
        return queueBuilds;
    }
    
    private List<Node> getReservableNodes(String resourceLabel) {

        List<Node> reservableNodes = Jenkins.get().getNodes().stream()
            .filter(node -> node.getNodeProperty(NodePropertyExtension.class) != null)
            .filter(reservableNode -> reservableNode.getLabelString().contains(resourceLabel))
            .collect(Collectors.toList());
        
        log.log(Level.FINEST, "Got reservable resources {0}.", reservableNodes);
        
        return reservableNodes;
    }
    
    /**
     * Singleton instance.
     */
    private static ReservableResourcesManager instance;
    
    /**
     * Gets the {@link ReservableResourcesManager} singleton. 
     * 
     * @return The instance; never null.
     */
    @Nonnull
    public static synchronized ReservableResourcesManager getInstance() {

        if (instance == null) {
            instance = new ReservableResourcesManager();
        }

        return instance;
    }
        
    private final class BuildQueue {
    
        private final String label;
        private final BlockingQueue<AcquireTask> queue = new LinkedBlockingQueue<>();
        
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        
        @SuppressWarnings("java:S3077")
        private volatile AcquireTask currentAcquireTask;
        
        public BuildQueue(String label) {

            this.label = label;
            
            executor.execute(() -> {
                                
                while (true) {
                    try {
                        currentAcquireTask = queue.take();
                        
                        handOutNextAvailableNode(label, currentAcquireTask);
                    }
                    catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    catch (Exception ignoreException) {
                        // Don't stop polling if there are other other exception.
                    }
                    finally {
                        currentAcquireTask = null;
                    }
                }
            });
        }
        
        @SuppressWarnings("java:S1452")
        public List<AbstractBuild<?, ?>> getQueueBuilds() {
            
            List<AbstractBuild<?, ?>> builds = new ArrayList<>();
            
            if (currentAcquireTask != null) {
                builds.add(currentAcquireTask.build);
            }
            
            builds.addAll(queue.stream().map(AcquireTask::getBuild).collect(Collectors.toList()));
            
            return builds;
        }
        
        public Node acquireAvailableNode(
                int timeoutInMinutes,
                AbstractBuild<?, ?> build) throws InterruptedException, TimeoutException {

            AcquireTask acquireTask = new AcquireTask(build);
            
            queue.add(acquireTask);
            
            try {
                return acquireTask.get(timeoutInMinutes, TimeUnit.MINUTES);
            }
            catch (ExecutionException exception) {
                // This shouldn't really happen with current code.
                throw new InterruptedException(exception.getMessage());
            }
            finally {
                // Remove the task from the queue (if it still there) or reset current task.
                if (!queue.remove(acquireTask)) {
                    currentAcquireTask = null;    
                }
            }
        }
        
        private void handOutNextAvailableNode(
                String label,
                AcquireTask acquireTask) throws InterruptedException {

            while (acquireTask.build.isBuilding()) {
                List<Node> availableNodes = getReservableNodes(label).stream()
                    .filter(node -> node.toComputer().isOnline())
                    .filter(filteredNode -> !reservedByNodeName.containsKey(filteredNode.getNodeName()))
                    .collect(Collectors.toList());
                
                if (availableNodes.isEmpty()) {
                    TimeUnit.SECONDS.sleep(1);
                    continue;
                }

                Node availableNode = availableNodes.get(new Random().nextInt(availableNodes.size()));
                
                synchronized (reservedByNodeName) {
                    // The build was aborted if current task is null.
                    if (currentAcquireTask == null) {
                        return;
                    }
                    
                    if (reservedByNodeName.containsKey(availableNode.getNodeName())) {
                        continue;
                    }
                    
                    reservedByNodeName.put(
                        availableNode.getNodeName(),
                        new ReservedResource(availableNode, acquireTask.build));
                }

                acquireTask.setNode(availableNode);
                return;
            }
            
        }
    }
    
    private static final class AcquireTask extends FutureTask<Node> {

        private final AbstractBuild<?, ?> build;
        
        public AcquireTask(AbstractBuild<?,?> build) {

            super(() -> null);
            
            this.build = build;
        }

        @SuppressWarnings("java:S1452")
        public AbstractBuild<?, ?> getBuild() {

            return build;
        }

        public void setNode(Node node) {

            this.set(node);
        }
    }
}
