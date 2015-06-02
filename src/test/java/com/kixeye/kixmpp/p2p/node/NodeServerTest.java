package com.kixeye.kixmpp.p2p.node;

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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kixeye.kixmpp.p2p.ClusterClient;
import com.kixeye.kixmpp.p2p.discovery.ConstNodeDiscovery;
import com.kixeye.kixmpp.p2p.listener.ClusterListener;
import com.kixeye.kixmpp.p2p.message.JoinRequest;
import com.kixeye.kixmpp.p2p.message.JoinResponse;
import com.kixeye.kixmpp.p2p.message.MessageRegistry;
import com.kixeye.kixmpp.p2p.message.PingRequest;

@RunWith(JUnit4.class)
public class NodeServerTest {
    @Before
    public void setup() {
        System.setProperty("io.netty.leakDetectionLevel","PARANOID");
    }

    @Test(expected = java.net.BindException.class)
    public void portAlreadyInUseTest() {
        final MessageRegistry messageRegistry = new MessageRegistry();
        final NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        final NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        NodeServer serverA = new NodeServer();
        NodeServer serverB = new NodeServer();
        try {
            serverA.initialize("127.0.0.1", 8042, bossGroup, workerGroup, messageRegistry, new ChannelInboundHandlerAdapter() );
            serverB.initialize("127.0.0.1", 8042, bossGroup, workerGroup, messageRegistry, new ChannelInboundHandlerAdapter() );
        } finally {
            serverA.shutdown();
            serverB.shutdown();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


    @Test
    public void clientSendAndServerReceiveTest() throws InterruptedException {
        final MessageRegistry messageRegistry = new MessageRegistry();
        final NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        final NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        final BlockingQueue<Object> serverMessages = new LinkedBlockingQueue<>();
        final NodeAddress address = new NodeAddress("a",8001);


        NodeServer server = new NodeServer();
        server.initialize("127.0.0.1",8042, bossGroup, workerGroup, messageRegistry, new ChannelInboundHandlerAdapter(){
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                serverMessages.offer(msg);
            }
        });

        NodeClient client = new NodeClient();
        client.initialize("127.0.0.1", 8042, workerGroup, messageRegistry, new ChannelInboundHandlerAdapter());
        client.send( new JoinRequest(new NodeId(42), address) );
        client.send( new JoinResponse(JoinResponse.ResponseCode.OK,new NodeId(42), address) );

        Object msg = serverMessages.poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(msg);
        Assert.assertEquals(msg.getClass(), JoinRequest.class);
        Assert.assertEquals(((JoinRequest)msg).getJoinerId(), new NodeId(42));
        Assert.assertEquals(((JoinRequest)msg).getJoinerAddress(), address);

        msg = serverMessages.poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(msg);
        Assert.assertEquals(msg.getClass(), JoinResponse.class);
        Assert.assertEquals(((JoinResponse)msg).getResult(), JoinResponse.ResponseCode.OK);

        client.shutdown();
        server.shutdown();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Test
    public void peerConnectTest() throws InterruptedException {
        final String localhost = "127.0.0.1";
        final ConstNodeDiscovery discovery = new ConstNodeDiscovery(
                new NodeAddress(localhost,8000),
                new NodeAddress(localhost,8001),
                new NodeAddress(localhost,8002));

        final JoinCountClusterListener listenerA = new JoinCountClusterListener();
        final JoinCountClusterListener listenerB = new JoinCountClusterListener();
        final JoinCountClusterListener listenerC = new JoinCountClusterListener();

        final ClusterClient repA = new ClusterClient(listenerA,localhost, 8000, discovery, 2000, Executors.newSingleThreadScheduledExecutor());
        final ClusterClient repB = new ClusterClient(listenerB,localhost, 8001, discovery, 2000, Executors.newSingleThreadScheduledExecutor());
        final ClusterClient repC = new ClusterClient(listenerC,localhost, 8002, discovery, 2000, Executors.newSingleThreadScheduledExecutor());
        try {
            // spin until 3 nodes are found
            for (int i = 0; i < 10; i++) {
                Thread.sleep(500);
                if (repA.getNodeCount() >= 3
                        && repB.getNodeCount() >= 3
                        && repC.getNodeCount() >= 3) {
                    break;
                }
            }

            Assert.assertEquals(3, repA.getNodeCount());
            Assert.assertEquals(3, repB.getNodeCount());
            Assert.assertEquals(3, repC.getNodeCount());

            // spin until 3 nodes are process in the callback
            for (int i = 0; i < 10; i++) {
                Thread.sleep(500);
                if (listenerA.getJoinCount() == 3
                        && listenerB.getJoinCount() == 3
                        && listenerC.getJoinCount() == 3) {
                    break;
                }
            }

            Assert.assertEquals(3, listenerA.getJoinCount());
            Assert.assertEquals(3, listenerB.getJoinCount());
            Assert.assertEquals(3, listenerC.getJoinCount());

            // send message to all nodes
            for (NodeId nid : listenerA.getNodes()) {
                repA.sendMessage(nid, new PingRequest());
            }

        } finally {
            repA.shutdown();
            repB.shutdown();
            repC.shutdown();
        }
    }

    private class JoinCountClusterListener implements ClusterListener {

        private final Logger logger = LoggerFactory.getLogger(JoinCountClusterListener.class);
        private final ConcurrentSkipListSet<NodeId> nodes = new ConcurrentSkipListSet<>();

        @Override
        public void onNodeJoin(ClusterClient cluster, NodeId nodeId) {
            nodes.add(nodeId);
        }

        @Override
        public void onNodeLeft(ClusterClient cluster, NodeId nodeId) {
            nodes.remove(nodeId);
        }

        @Override
        public void onMessage(ClusterClient cluster, NodeId senderId, Object message) {
            logger.debug("got a message");
        }

        public int getJoinCount() {
            return nodes.size();
        }

        public Set<NodeId> getNodes() {
            return nodes;
        }
    }
}
