package com.kixeye.kixmpp.p2p;

/*
 * #%L
 * Zaqar
 * %%
 * Copyright (C) 2014 Charles Barry
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.kixeye.kixmpp.p2p.discovery.NodeDiscovery;
import com.kixeye.kixmpp.p2p.listener.ClusterListener;
import com.kixeye.kixmpp.p2p.message.JoinRequest;
import com.kixeye.kixmpp.p2p.message.JoinResponse;
import com.kixeye.kixmpp.p2p.message.MessageRegistry;
import com.kixeye.kixmpp.p2p.message.MessageWrapper;
import com.kixeye.kixmpp.p2p.node.*;
import com.kixeye.kixmpp.p2p.util.Net;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ClusterClient maintains a list of Zaqar peer-to-peer connections.
 */
public class ClusterClient {
    private final static Logger logger = LoggerFactory.getLogger(ClusterClient.class);

    private final Node localNode;
    private final NodeServer server;
    private final MessageRegistry messageRegistry = new MessageRegistry();
    private final ExecutorService executorService;
    private final ScheduledFuture<?> pollingTask;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private final Object joinLock = new Object();

    // node maps
    private final Map<NodeId,Node> idToNode = new ConcurrentHashMap<>();
    private final Map<NodeAddress,Node> addrToNode = new ConcurrentHashMap<>();
    private final AtomicInteger nodeCount = new AtomicInteger(0);

    private NodeDiscovery discovery;
    private ClusterListener listener;

    public ClusterClient(ClusterListener listener, String hostAddress, int hostPort, NodeDiscovery discovery, long msPollingTime, ExecutorService executorService) {
        this.listener = listener;
        this.discovery = discovery;
        this.executorService = executorService;
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();

        // create local node
        NodeAddress localAddr;
        if (hostAddress != null && !hostAddress.isEmpty()) {
            localAddr = new NodeAddress(hostAddress,hostPort);
        } else {
            localAddr = new NodeAddress(Net.getLocalAddress(), hostPort );
        }
        localNode = new LocalNode(this, new NodeId(),localAddr);
        addNode(localNode);

        // Create listener for peer connections
        server = new NodeServer();
        server.initialize(hostAddress, hostPort, bossGroup, workerGroup, messageRegistry, new ServerChannelHandler());

        // schedule polling task
        pollingTask = workerGroup.scheduleWithFixedDelay( new Runnable() {
            @Override
            public void run() {
                pollForNodes();
            }
        }, 15, msPollingTime, TimeUnit.MILLISECONDS);
    }


    /**
     * Get the message registry for this client
     * @return message registry
     */
    public MessageRegistry getMessageRegistry() {
        return messageRegistry;
    }


    /**
     * Send message to the destination node.
     * @param destinationNodeId
     * @param msg
     */
    public void sendMessage(NodeId destinationNodeId, Object msg) {
        Node node = idToNode.get(destinationNodeId);
        if (node != null) {
            MessageWrapper wrapper = MessageWrapper.wrap(msg);
            node.sendMessage(wrapper);
            wrapper.release();
        }
    }

    /**
     * Send message to the destination nodes
     * @param destinationNodeIds
     * @param msg
     */
    public void sendMessage(List<NodeId> destinationNodeIds, Object msg) {
        MessageWrapper wrapper = MessageWrapper.wrap(msg);
        for (NodeId nid : destinationNodeIds) {
            Node node = idToNode.get(nid);
            if (node != null) {
                node.sendMessage(wrapper);
            }
        }
        wrapper.release();
    }


    /**
     * Send message to all nodes.
     * @param msg
     */
    public void sendMessageToAll(Object msg, boolean includeSelf) {
        MessageWrapper wrapper = MessageWrapper.wrap(msg);
        for (Node node : idToNode.values()) {
            if (!includeSelf && node == localNode) {
                continue;
            }
            node.sendMessage(wrapper);
        }
        wrapper.release();
    }


    /**
     * [DEBUG] - Get cluster nodes.
     * @return
     */
    public List<Node> getNodes() {
        return new ArrayList<>(idToNode.values());
    }


    /**
     * [DEBUG] - Get local node
     * @return
     */
    public Node getLocalNode() {
        return localNode;
    }


    /**
     * Get the local node's id.
     * @return local node id
     */
    public NodeId getLocalNodeId() {
        return localNode.getId();
    }


    /**
     * Get number of nodes in the peer-to-peer network.
     * @return
     */
    public int getNodeCount() {
        return nodeCount.get();
    }


    /**
     * Get the cluster listener
     */
    public ClusterListener getClusterListener() {
        return listener;
    }


    /**
     * Shutdown this node of the peer-to-peer network.
     */
    public void shutdown() {
        pollingTask.cancel(false);
        server.shutdown();
        for (Node node : idToNode.values()) {
            node.close();
        }
        idToNode.clear();
        addrToNode.clear();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        // break dependencies
        listener = null;
        discovery = null;
    }


    /**
     * Use the NodeProvider to poll for Zaqar peers.
     */
    private void pollForNodes() {
        try {
            for (NodeAddress address : discovery.getNodeAddresses()) {
                synchronized (joinLock) {
                    if (addrToNode.containsKey(address)) {
                        continue;
                    }
                    Node node = new RemoteNode(this,address,workerGroup,messageRegistry,localNode);
                    addNode(node);
                }
            }
        } catch (Exception ex) {
            logger.error("Unexpected exception polling for nodes", ex);
        }
    }


    /**
     * Add node to repository.
     * @param node
     */
    public void addNode(final Node node) {
        Node existingNode = null;
        synchronized (joinLock) {
            if (node.getId() != null) {
                existingNode = idToNode.put(node.getId(), node);
            }
            if (node.getAddress() != null) {
                addrToNode.put(node.getAddress(),node);
            }
        }
        if (existingNode == null && node.getId() != null) {
            nodeCount.incrementAndGet();
            executorService.execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            listener.onNodeJoin(ClusterClient.this, node.getId());
                        }
                    });
        }
    }


    /**
     * Remove node from repository.
     * @param node
     */
    public void removeNode(final Node node) {
        Node existingNode = null;
        synchronized (joinLock) {
            if (node.getId() != null) {
                existingNode = idToNode.remove(node.getId());
            }
            if (node.getAddress() != null) {
                addrToNode.remove(node.getAddress());
            }
        }
        if (existingNode != null) {
            nodeCount.decrementAndGet();
            executorService.execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            listener.onNodeLeft(ClusterClient.this, node.getId());
                        }
                    });
        }
    }


    /**
     * Handle initial join message from incoming peers and hand off to node
     */
    @ChannelHandler.Sharable
    private class ServerChannelHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof JoinRequest) {
                JoinRequest joinRequest = (JoinRequest) msg;

                // Check for duplicate IDs in the peer network.  Rare but could happen.
                if (joinRequest.getJoinerId() == localNode.getId()) {
                    logger.error("Duplicate ID found in the peer network");
                    ctx.writeAndFlush(new JoinResponse(JoinResponse.ResponseCode.REJECTED_ID_ALREADY_IN_USE,localNode.getId(), localNode.getAddress()));
                    ctx.close();
                    return;
                }

                // Handle join request
                synchronized (joinLock) {
                    if (joinRequest.getJoinerAddress().equals(localNode.getAddress())) {
                        logger.error("Received join for myself? Local address: [{}] - Joiner address: [{}]", localNode.getAddress(), joinRequest.getJoinerAddress());
                        return;
                    }

                    Node existingNode = addrToNode.get(joinRequest.getJoinerAddress());
                    if (existingNode != null) {
                        if (existingNode.getId() != null) {
                            // we already have an active node to that client, reject this join
                            ctx.writeAndFlush(new JoinResponse(JoinResponse.ResponseCode.REJECTED_EXISTING_CONNECTION, localNode.getId(), localNode.getAddress()));
                            return;
                        } else if (localNode.getId().compareTo(joinRequest.getJoinerId()) < 0) {
                            // our node is still initializing and our Id is lower, reject this join
                            ctx.writeAndFlush(new JoinResponse(JoinResponse.ResponseCode.REJECTED_EXISTING_CONNECTION, localNode.getId(), localNode.getAddress()));
                            return;
                        } else {
                            // our node is still initializing but our Id is higher so kill ours and allow this through
                            existingNode.setOrphaned(true);
                            idToNode.remove(joinRequest.getJoinerId());
                            addrToNode.remove(joinRequest.getJoinerAddress());
                        }
                    } else {
                        // unknown node, so continue join process
                    }

                    // add new node for this connection
                    Node newNode = new RemoteNode(ClusterClient.this, joinRequest.getJoinerId(), joinRequest.getJoinerAddress(), ctx.channel(), this);
                    addNode(newNode);
                }

                // notify incoming connection they are good
                ctx.writeAndFlush(new JoinResponse(JoinResponse.ResponseCode.OK, localNode.getId(), localNode.getAddress()));
            } else {
                logger.error("Unexpected message in ServerChannelHandler: ", msg.getClass().toString());
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("Exception processing message", cause);
        }
    }
}
