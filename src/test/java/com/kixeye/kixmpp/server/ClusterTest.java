package com.kixeye.kixmpp.server;

/*
 * #%L
 * KIXMPP
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
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

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.client.KixmppClient;
import com.kixeye.kixmpp.client.module.muc.MucJoin;
import com.kixeye.kixmpp.client.module.muc.MucKixmppClientModule;
import com.kixeye.kixmpp.client.module.muc.MucListener;
import com.kixeye.kixmpp.client.module.muc.MucMessage;
import com.kixeye.kixmpp.client.module.presence.Presence;
import com.kixeye.kixmpp.client.module.presence.PresenceKixmppClientModule;
import com.kixeye.kixmpp.client.module.presence.PresenceListener;
import com.kixeye.kixmpp.p2p.ClusterClient;
import com.kixeye.kixmpp.p2p.discovery.ConstNodeDiscovery;
import com.kixeye.kixmpp.p2p.node.NodeAddress;
import com.kixeye.kixmpp.server.module.auth.InMemoryAuthenticationService;
import com.kixeye.kixmpp.server.module.auth.SaslKixmppServerModule;
import com.kixeye.kixmpp.server.module.muc.MucKixmppServerModule;
import com.kixeye.kixmpp.server.utils.SocketUtils;

import io.netty.handler.ssl.SslContext;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class ClusterTest {
    public static final InetSocketAddress SERVER_A_SOCKET = new InetSocketAddress(SocketUtils.findAvailableTcpPort());
    public static final InetSocketAddress SERVER_B_SOCKET = new InetSocketAddress(SocketUtils.findAvailableTcpPort());
    public static final InetSocketAddress SERVER_A_CLUSTER = new InetSocketAddress("127.0.0.1",SocketUtils.findAvailableTcpPort());
    public static final InetSocketAddress SERVER_B_CLUSTER = new InetSocketAddress("127.0.0.1",SocketUtils.findAvailableTcpPort());
    public static final ConstNodeDiscovery discovery = new ConstNodeDiscovery(
            new NodeAddress(SERVER_A_CLUSTER.getHostName(), SERVER_A_CLUSTER.getPort()),
            new NodeAddress(SERVER_B_CLUSTER.getHostName(), SERVER_B_CLUSTER.getPort())
    );

    @Test
    public void twoNodeCluster() throws Exception {

        // turn on Netty's leak detector
        System.setProperty("io.netty.leakDetectionLevel","PARANOID");

        // start servers
        KixmppServer serverA = new KixmppServer(SERVER_A_SOCKET, "testChat", SERVER_A_CLUSTER, discovery);
        KixmppServer serverB = new KixmppServer(SERVER_B_SOCKET, "testChat", SERVER_B_CLUSTER, discovery);
        serverA.start().get();
        serverB.start().get();

        // start clients
        TestClient clientA = new TestClient(serverA,"userA");
        TestClient clientB = new TestClient(serverB,"userB");
        clientA.connect();
        clientB.connect();

        // spin waiting for cluster
        waitForCluster(serverA.getCluster());
        waitForCluster(serverB.getCluster());

        // send message to room
        clientA.sendRoomMessage();

        // verity both clients got the message
        clientA.verifyRoomMessage();
        clientB.verifyRoomMessage();

        // shut everything down
        clientA.disconnect();
        clientB.disconnect();
        serverA.stop();
        serverB.stop();
    }

    private void waitForCluster(ClusterClient cluster) throws InterruptedException {
        for (int i = 0; ; i++) {
            if (cluster.getNodeCount() == 2) {
                break;
            }
            if (i > 10) {
                Assert.fail("cluster failed to form!");
                break;
            }
            Thread.sleep(500);
        }
    }

    public class TestClient {
           private final Logger logger = LoggerFactory.getLogger(TestClient.class);
           private final LinkedBlockingQueue<Presence> presences = new LinkedBlockingQueue<>();
           private final LinkedBlockingQueue<MucJoin> mucJoins = new LinkedBlockingQueue<>();
           private final LinkedBlockingQueue<MucMessage> mucMessages = new LinkedBlockingQueue<>();

           private KixmppServer server;
           private String username;
           private KixmppClient client;
           private MucJoin mucJoin;


           public TestClient(KixmppServer server, String username) {
               this.server = server;
               this.username = username;
               try {
                   client = new KixmppClient(SslContext.newClientContext(), KixmppClient.Type.TCP);
               } catch (SSLException e) {
                   logger.error("SSL Exception", e);
               }
           }

           public void connect() throws InterruptedException, ExecutionException, TimeoutException {
               // create MUC service and room
               ((InMemoryAuthenticationService) server.module(SaslKixmppServerModule.class).getAuthenticationService()).addUser(username, "testPassword");
               server.module(MucKixmppServerModule.class).addService("conference").addRoom("someRoom");

               // connect to server
               client.connect(server.getBindAddress().getHostName(), server.getBindAddress().getPort(), server.getDomain()).get();

               // establish message handlers
               client.module(PresenceKixmppClientModule.class).addPresenceListener(new PresenceListener() {
                   public void handle(Presence presence) {
                       presences.offer(presence);
                   }
               });

               client.module(MucKixmppClientModule.class).addJoinListener(new MucListener<MucJoin>() {
                   public void handle(MucJoin event) {
                       mucJoins.offer(event);
                   }
               });

               client.module(MucKixmppClientModule.class).addMessageListener(new MucListener<MucMessage>() {
                   public void handle(MucMessage event) {
                       mucMessages.offer(event);
                   }
               });

               // login
               client.login(username, "testPassword", "test").get(2,TimeUnit.SECONDS);

               // presence to available
               client.module(PresenceKixmppClientModule.class).updatePresence(new Presence());
               Presence presence = presences.poll(2,TimeUnit.SECONDS);
               Assert.assertNotNull(presence);

               // join room
               client.module(MucKixmppClientModule.class).joinRoom(KixmppJid.fromRawJid("someRoom@conference.testChat"), username);
               mucJoin = mucJoins.poll(5,TimeUnit.SECONDS);
               Assert.assertNotNull(mucJoin);
           }

           public void sendRoomMessage() {
               client.module(MucKixmppClientModule.class).sendRoomMessage(mucJoin.getRoomJid(), "someMessage", username);
           }

           public void verifyRoomMessage() throws InterruptedException {
               MucMessage message = mucMessages.poll(5, TimeUnit.SECONDS);
               Assert.assertNotNull(message);
           }

           public void disconnect() throws ExecutionException, InterruptedException {
               client.disconnect().get();
           }

         }
}
