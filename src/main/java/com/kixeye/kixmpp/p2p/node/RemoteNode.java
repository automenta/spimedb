package com.kixeye.kixmpp.p2p.node;

/*
 * #%L
 * Hermes
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

import com.kixeye.kixmpp.p2p.ClusterClient;
import com.kixeye.kixmpp.p2p.message.JoinRequest;
import com.kixeye.kixmpp.p2p.message.JoinResponse;
import com.kixeye.kixmpp.p2p.message.MessageRegistry;
import com.kixeye.kixmpp.p2p.message.MessageWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;

public class RemoteNode extends Node {

    private Channel channel;
    private NodeClient client;
    private Node localNode;

    /**
     * Constructor used to create a RemoteNode that connects to the node's server.
     * @param address
     * @param workerGroup
     * @throws InterruptedException
     */
    public RemoteNode(ClusterClient repository, NodeAddress address, EventLoopGroup workerGroup, MessageRegistry messageRegistry, Node localNode) throws InterruptedException {
        super(repository,address);
        this.state = State.CONNECTING;
        this.localNode = localNode;
        this.channel = null;
        this.client = new NodeClient();
        client.initialize(address.getHost(), address.getPort(), workerGroup, messageRegistry, new NodeChannelHandler());
    }


    /**
     * Constructor used to create a RemoteNode from an incoming connection.
     * @param id
     * @param address
     * @param channel
     * @param oldHandler
     */
    public RemoteNode(ClusterClient cluster, NodeId id, NodeAddress address, Channel channel, ChannelInboundHandlerAdapter oldHandler) {
        super(cluster, id, address);
        this.state = State.CONNECTED;
        this.channel = channel;
        this.client = null;

        // remove initial server handler and replace with node handler
        channel.pipeline().remove(oldHandler);
        channel.pipeline().addLast( new NodeChannelHandler() );
    }

    @Override
    public void sendMessage(MessageWrapper wrapper) {
        if (channel != null) {
            ByteBuf buf = wrapper.getSerialized(cluster.getMessageRegistry());
            channel.writeAndFlush(buf.retain());
        }
    }

    /**
     * Close network connections associated with the node
     */
    @Override
    public void close() {
        super.close();

        // close the socket connection
        if (channel != null) {
            channel.close();
            channel = null;
        }

        // shutdown client
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }


    /**
     * Channel handler for the Node.
     */
    public class NodeChannelHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // on connection establishment, request to join the cluster
            ctx.writeAndFlush( new JoinRequest(localNode.getId(),localNode.getAddress()) );
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            close();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof JoinResponse) {
                JoinResponse joinResponse = (JoinResponse) msg;
                switch (joinResponse.getResult()) {
                    case OK:
                        setId( joinResponse.getResponderId() );
                        state = State.CONNECTED;
                        channel = ctx.channel();
                        cluster.addNode(RemoteNode.this);
                        break;
                    case REJECTED_EXISTING_CONNECTION:
                        ctx.close();
                        break;
                    case REJECTED_ID_ALREADY_IN_USE:
                        logger.error("JoinResponse: Duplicate ID in peer network!");
                        ctx.close();
                        break;
                    default:
                        logger.error("JoinResponse: Invalid result {}", joinResponse.getResult().toString());
                        ctx.close();
                        break;
                }
            } else {
                cluster.getClusterListener().onMessage(cluster, id, msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("Exception processing a message from node {}", getId().toString(), cause);
        }
    }
}
