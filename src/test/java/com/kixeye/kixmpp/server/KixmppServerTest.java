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

import io.netty.handler.ssl.SslContext;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.netty.util.concurrent.Promise;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.junit.Assert;
import org.junit.Test;

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.client.KixmppClient;
import com.kixeye.kixmpp.client.module.chat.MessageKixmppClientModule;
import com.kixeye.kixmpp.client.module.chat.MessageListener;
import com.kixeye.kixmpp.client.module.muc.MucJoin;
import com.kixeye.kixmpp.client.module.muc.MucKixmppClientModule;
import com.kixeye.kixmpp.client.module.muc.MucListener;
import com.kixeye.kixmpp.client.module.muc.MucMessage;
import com.kixeye.kixmpp.client.module.presence.Presence;
import com.kixeye.kixmpp.client.module.presence.PresenceKixmppClientModule;
import com.kixeye.kixmpp.client.module.presence.PresenceListener;
import com.kixeye.kixmpp.p2p.discovery.ConstNodeDiscovery;
import com.kixeye.kixmpp.server.module.auth.InMemoryAuthenticationService;
import com.kixeye.kixmpp.server.module.auth.SaslKixmppServerModule;
import com.kixeye.kixmpp.server.module.muc.MucHistory;
import com.kixeye.kixmpp.server.module.muc.MucHistoryProvider;
import com.kixeye.kixmpp.server.module.muc.MucKixmppServerModule;
import com.kixeye.kixmpp.server.utils.SocketUtils;

/**
 * Tests the {@link KixmppServer}
 * 
 * @author ebahtijaragic
 */
public class KixmppServerTest {
	@Test
	public void testUserMapping() throws Exception {
		try (KixmppServer server = new KixmppServer(new InetSocketAddress(SocketUtils.findAvailableTcpPort()), "testChat",
				new InetSocketAddress(SocketUtils.findAvailableTcpPort()), new ConstNodeDiscovery())) {
			Assert.assertNotNull(server.start().get(2, TimeUnit.SECONDS));

			((InMemoryAuthenticationService) server.module(SaslKixmppServerModule.class).getAuthenticationService()).addUser("testUser", "testPassword");
			server.module(MucKixmppServerModule.class).addService("conference").addRoom("someRoom");

			try (KixmppClient client = new KixmppClient(SslContext.newClientContext(), KixmppClient.Type.TCP)) {
				final LinkedBlockingQueue<Presence> presences = new LinkedBlockingQueue<>();
				final LinkedBlockingQueue<MucJoin> mucJoins = new LinkedBlockingQueue<>();
				final LinkedBlockingQueue<MucMessage> mucMessages = new LinkedBlockingQueue<>();

				Assert.assertNotNull(client.connect("localhost",server.getBindAddress().getPort(), server.getDomain())
						.get(2, TimeUnit.SECONDS));

				client.module(PresenceKixmppClientModule.class)
						.addPresenceListener(new PresenceListener() {
							public void handle(Presence presence) {
								presences.offer(presence);
							}
						});

				client.module(MucKixmppClientModule.class).addJoinListener(
						new MucListener<MucJoin>() {
							public void handle(MucJoin event) {
								mucJoins.offer(event);
							}
						});

				client.module(MucKixmppClientModule.class).addMessageListener(
						new MucListener<MucMessage>() {
							public void handle(MucMessage event) {
								mucMessages.offer(event);
							}
						});

				Assert.assertNotNull(client.login("testUser", "testPassword", "testResource").get(2, TimeUnit.SECONDS));
				client.module(PresenceKixmppClientModule.class).updatePresence(new Presence());

				Assert.assertNotNull(server.getChannel(KixmppJid.fromRawJid("testUser@testchat/testResource")));
			}

			int count = 0;
			
			while (count < 50 && server.getChannel(KixmppJid.fromRawJid("testUser@testchat/testResource")) != null) {
				count++;
				System.gc();
				Thread.sleep(100);
			}

			Assert.assertNull(server.getChannel(KixmppJid.fromRawJid("testUser@testchat/testResource")));
		}
	}
	
	@Test
	public void testPrivateMessageUsingKixmpp() throws Exception {
		try (KixmppServer server = new KixmppServer(new InetSocketAddress(SocketUtils.findAvailableTcpPort()), "testChat",
				new InetSocketAddress(SocketUtils.findAvailableTcpPort()), new ConstNodeDiscovery())) {
			Assert.assertNotNull(server.start().get(2, TimeUnit.SECONDS));

			((InMemoryAuthenticationService) server.module(SaslKixmppServerModule.class).getAuthenticationService()).addUser("testUser1", "testPassword");
			((InMemoryAuthenticationService) server.module(SaslKixmppServerModule.class).getAuthenticationService()).addUser("testUser2", "testPassword");
			
			try (KixmppClient client1 = new KixmppClient()) {
				final LinkedBlockingQueue<Presence> client1Presences = new LinkedBlockingQueue<>();
				final LinkedBlockingQueue<com.kixeye.kixmpp.client.module.chat.Message> client1Messages = new LinkedBlockingQueue<>();

				Assert.assertNotNull(client1.connect("localhost",
						server.getBindAddress().getPort(), server.getDomain())
						.get(2, TimeUnit.SECONDS));

				client1.module(PresenceKixmppClientModule.class)
						.addPresenceListener(new PresenceListener() {
							public void handle(Presence presence) {
								client1Presences.offer(presence);
							}
						});
				client1.module(MessageKixmppClientModule.class)
						.addMessageListener(new MessageListener() {
							public void handle(com.kixeye.kixmpp.client.module.chat.Message message) {
								client1Messages.offer(message);
							}
						});

				Assert.assertNotNull(client1.login("testUser1", "testPassword", "testResource").get(2, TimeUnit.SECONDS));
				client1.module(PresenceKixmppClientModule.class).updatePresence(new Presence());

				Assert.assertNotNull(client1Presences.poll(2, TimeUnit.SECONDS));
				
				try (KixmppClient client2 = new KixmppClient()) {
					final LinkedBlockingQueue<Presence> client2Presences = new LinkedBlockingQueue<>();
					final LinkedBlockingQueue<com.kixeye.kixmpp.client.module.chat.Message> client2Messages = new LinkedBlockingQueue<>();

					Assert.assertNotNull(client2.connect("localhost",
							server.getBindAddress().getPort(), server.getDomain())
							.get(2, TimeUnit.SECONDS));

					client2.module(PresenceKixmppClientModule.class)
							.addPresenceListener(new PresenceListener() {
								public void handle(Presence presence) {
									client2Presences.offer(presence);
								}
							});
					client2.module(MessageKixmppClientModule.class)
							.addMessageListener(new MessageListener() {
								public void handle(com.kixeye.kixmpp.client.module.chat.Message message) {
									client2Messages.offer(message);
								}
							});

					Assert.assertNotNull(client2.login("testUser2", "testPassword", "testResource").get(2, TimeUnit.SECONDS));
					client2.module(PresenceKixmppClientModule.class).updatePresence(new Presence());

					Assert.assertNotNull(client2Presences.poll(2, TimeUnit.SECONDS));

					final String body = UUID.randomUUID().toString().replace("-", "");
					
					client2.module(MessageKixmppClientModule.class).sendMessage(client1.getJid(), body);
					
					com.kixeye.kixmpp.client.module.chat.Message client1Message = client1Messages.poll(2, TimeUnit.SECONDS);
					
					Assert.assertNotNull(client1Message);
					Assert.assertEquals(client2.getJid(), client1Message.getFrom());
					Assert.assertEquals(client1.getJid().getBaseJid(), client1Message.getTo().getFullJid());
					Assert.assertEquals(body, client1Message.getBody());
				}
			}
		}
	}

	@Test
	public void testSimpleUsingKixmpp() throws Exception {
		try (KixmppServer server = new KixmppServer(new InetSocketAddress(SocketUtils.findAvailableTcpPort()), "testChat",
				new InetSocketAddress(SocketUtils.findAvailableTcpPort()), new ConstNodeDiscovery())) {
			Assert.assertNotNull(server.start().get(2, TimeUnit.SECONDS));

			((InMemoryAuthenticationService) server.module(SaslKixmppServerModule.class).getAuthenticationService()).addUser("testUser", "testPassword");
			server.module(MucKixmppServerModule.class).addService("conference").addRoom("someRoom");

			try (KixmppClient client = new KixmppClient(SslContext.newClientContext(), KixmppClient.Type.TCP)) {
				final LinkedBlockingQueue<Presence> presences = new LinkedBlockingQueue<>();
				final LinkedBlockingQueue<MucJoin> mucJoins = new LinkedBlockingQueue<>();
				final LinkedBlockingQueue<MucMessage> mucMessages = new LinkedBlockingQueue<>();

				Assert.assertNotNull(client.connect("localhost",
						server.getBindAddress().getPort(), server.getDomain())
						.get(2, TimeUnit.SECONDS));

				client.module(PresenceKixmppClientModule.class)
						.addPresenceListener(new PresenceListener() {
							public void handle(Presence presence) {
								presences.offer(presence);
							}
						});

				client.module(MucKixmppClientModule.class).addJoinListener(
						new MucListener<MucJoin>() {
							public void handle(MucJoin event) {
								mucJoins.offer(event);
							}
						});

				client.module(MucKixmppClientModule.class).addMessageListener(
						new MucListener<MucMessage>() {
							public void handle(MucMessage event) {
								mucMessages.offer(event);
							}
						});

				Assert.assertNotNull(client.login("testUser", "testPassword", "testResource").get(2, TimeUnit.SECONDS));
				client.module(PresenceKixmppClientModule.class).updatePresence(new Presence());

				Assert.assertNotNull(presences.poll(2, TimeUnit.SECONDS));

				client.module(MucKixmppClientModule.class).joinRoom(KixmppJid.fromRawJid("someRoom@conference.testChat"), "testNick");

				MucJoin mucJoin = mucJoins.poll(2, TimeUnit.SECONDS);

				Assert.assertNotNull(mucJoin);

				client.module(MucKixmppClientModule.class).sendRoomMessage(mucJoin.getRoomJid(), "someMessage", "testNick");

				MucMessage mucMessage = mucMessages.poll(2, TimeUnit.SECONDS);

				Assert.assertNotNull(mucMessage);
				Assert.assertEquals("someMessage", mucMessage.getBody());
			}
		}
	}
	
	@Test
	public void testSimpleUsingKixmppWithWebSocket() throws Exception {
		try (KixmppServer server = new KixmppServer(new InetSocketAddress(SocketUtils.findAvailableTcpPort()), "testChat",
				new InetSocketAddress(SocketUtils.findAvailableTcpPort()), new ConstNodeDiscovery())) {
			server.enableWebSocket();
			
			Assert.assertNotNull(server.start().get(2, TimeUnit.SECONDS));

			((InMemoryAuthenticationService) server.module(SaslKixmppServerModule.class).getAuthenticationService()).addUser("testUser", "testPassword");
			server.module(MucKixmppServerModule.class).addService("conference").addRoom("someRoom");

			try (KixmppClient client = new KixmppClient(SslContext.newClientContext(), KixmppClient.Type.WEBSOCKET)) {
				final LinkedBlockingQueue<Presence> presences = new LinkedBlockingQueue<>();
				final LinkedBlockingQueue<MucJoin> mucJoins = new LinkedBlockingQueue<>();
				final LinkedBlockingQueue<MucMessage> mucMessages = new LinkedBlockingQueue<>();

				Assert.assertNotNull(client.connect("localhost",
						server.getWebSocketAddress().getPort(), server.getDomain())
						.get(2, TimeUnit.SECONDS));

				client.module(PresenceKixmppClientModule.class)
						.addPresenceListener(new PresenceListener() {
							public void handle(Presence presence) {
								presences.offer(presence);
							}
						});

				client.module(MucKixmppClientModule.class).addJoinListener(
						new MucListener<MucJoin>() {
							public void handle(MucJoin event) {
								mucJoins.offer(event);
							}
						});

				client.module(MucKixmppClientModule.class).addMessageListener(
						new MucListener<MucMessage>() {
							public void handle(MucMessage event) {
								mucMessages.offer(event);
							}
						});

				Assert.assertNotNull(client.login("testUser", "testPassword", "testResource").get(2, TimeUnit.SECONDS));
				client.module(PresenceKixmppClientModule.class).updatePresence(new Presence());

				Assert.assertNotNull(presences.poll(2, TimeUnit.SECONDS));

				client.module(MucKixmppClientModule.class).joinRoom(KixmppJid.fromRawJid("someRoom@conference.testChat"), "testNick");

				MucJoin mucJoin = mucJoins.poll(2, TimeUnit.SECONDS);

				Assert.assertNotNull(mucJoin);

				client.module(MucKixmppClientModule.class).sendRoomMessage(mucJoin.getRoomJid(), "someMessage", "testNick");

				MucMessage mucMessage = mucMessages.poll(2, TimeUnit.SECONDS);

				Assert.assertNotNull(mucMessage);
				Assert.assertEquals("someMessage", mucMessage.getBody());
			}
		}
	}
	
	@Test
	public void testSimpleUsingKixmppWithHistory() throws Exception {
		try (KixmppServer server = new KixmppServer(new InetSocketAddress(SocketUtils.findAvailableTcpPort()), "testChat",
				new InetSocketAddress(SocketUtils.findAvailableTcpPort()), new ConstNodeDiscovery())) {
			Assert.assertNotNull(server.start().get(2, TimeUnit.SECONDS));

			((InMemoryAuthenticationService) server.module(SaslKixmppServerModule.class).getAuthenticationService()).addUser("testUser", "testPassword");
			server.module(MucKixmppServerModule.class).addService("conference").addRoom("someRoom");
			
			server.module(MucKixmppServerModule.class).setHistoryProvider(new MucHistoryProvider() {
				public Promise<List<MucHistory>> getHistory(KixmppJid roomJid, KixmppJid userJid, Integer maxChars, Integer maxStanzas, Integer seconds, String since) {
					Promise<List<MucHistory>> promise = server.createPromise();
					List<MucHistory> history = new ArrayList<>(maxStanzas);
					
					for (int i = 0; i < maxStanzas; i++) {
						history.add(new MucHistory(KixmppJid.fromRawJid("user" + i + "@" + server.getDomain() + "/computer"), roomJid, 
								"nick" + i, "message" + i, System.currentTimeMillis()));
					}

					promise.setSuccess(history);
					return promise;
				}
			});

			try (KixmppClient client = new KixmppClient(SslContext.newClientContext(), KixmppClient.Type.TCP)) {
				final LinkedBlockingQueue<Presence> presences = new LinkedBlockingQueue<>();
				final LinkedBlockingQueue<MucJoin> mucJoins = new LinkedBlockingQueue<>();
				final LinkedBlockingQueue<MucMessage> mucMessages = new LinkedBlockingQueue<>();

				Assert.assertNotNull(client.connect("localhost",
						server.getBindAddress().getPort(), server.getDomain())
						.get(2, TimeUnit.SECONDS));

				client.module(PresenceKixmppClientModule.class)
						.addPresenceListener(new PresenceListener() {
							public void handle(Presence presence) {
								presences.offer(presence);
							}
						});

				client.module(MucKixmppClientModule.class).addJoinListener(
						new MucListener<MucJoin>() {
							public void handle(MucJoin event) {
								mucJoins.offer(event);
							}
						});

				client.module(MucKixmppClientModule.class).addMessageListener(
						new MucListener<MucMessage>() {
							public void handle(MucMessage event) {
								mucMessages.offer(event);
							}
						});

				Assert.assertNotNull(client.login("testUser", "testPassword", "testResource").get(2, TimeUnit.SECONDS));
				client.module(PresenceKixmppClientModule.class).updatePresence(new Presence());

				Assert.assertNotNull(presences.poll(2, TimeUnit.SECONDS));

				client.module(MucKixmppClientModule.class).joinRoom(KixmppJid.fromRawJid("someRoom@conference.testChat"), "testNick", 5, null, null, null);

				MucJoin mucJoin = mucJoins.poll(2, TimeUnit.SECONDS);

				Assert.assertNotNull(mucJoin);

				int count = 0;
				
				while (mucMessages.poll(2, TimeUnit.SECONDS) != null) {
					count++;
				}
				
				Assert.assertEquals(5, count);
			}
		}
	}

	@Test
	public void testSimpleUsingSmack() throws Exception {
		try (KixmppServer server = new KixmppServer(new InetSocketAddress(SocketUtils.findAvailableTcpPort()), "testChat",
				new InetSocketAddress(SocketUtils.findAvailableTcpPort()), new ConstNodeDiscovery())) {
			Assert.assertNotNull(server.start().get(2, TimeUnit.SECONDS));

			((InMemoryAuthenticationService) server.module(
					SaslKixmppServerModule.class).getAuthenticationService())
					.addUser("testUser", "testPassword");
			server.module(MucKixmppServerModule.class).addService("conference")
					.addRoom("someRoom");

			XMPPConnection connection = new XMPPTCPConnection(
					new ConnectionConfiguration("localhost", server
							.getBindAddress().getPort(), server.getDomain()));

			try {
				connection.connect();

				connection.login("testUser", "testPassword");

				final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();

				PacketListener messageListener = new PacketListener() {
					public void processPacket(Packet packet)
							throws NotConnectedException {
						messages.offer((Message) packet);
					}
				};

				MultiUserChat chat = new MultiUserChat(connection,
						"someRoom@conference.testChat");
				chat.addMessageListener(messageListener);
				chat.join("testNick");

				chat.sendMessage("hello!");

				Message message = messages.poll(2, TimeUnit.SECONDS);

				Assert.assertNotNull(message);

				if (null == message.getBody()
						|| "".equals(message.getBody().trim())) {
					message = messages.poll(2, TimeUnit.SECONDS);

					Assert.assertNotNull(message);

					Assert.assertEquals("hello!", message.getBody());
				}
			} finally {
				connection.disconnect();
			}
		}
	}
}
