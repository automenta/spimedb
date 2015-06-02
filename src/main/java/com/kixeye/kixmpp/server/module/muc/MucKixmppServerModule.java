package com.kixeye.kixmpp.server.module.muc;

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

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.util.concurrent.Promise;
import org.fusesource.hawtdispatch.Task;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.handler.KixmppStanzaHandler;
import com.kixeye.kixmpp.server.KixmppServer;
import com.kixeye.kixmpp.server.cluster.message.RoomBroadcastTask;
import com.kixeye.kixmpp.server.cluster.message.RoomTask;
import com.kixeye.kixmpp.server.module.KixmppServerModule;
import com.kixeye.kixmpp.server.module.bind.BindKixmppServerModule;

/**
 * Handles presence.
 * 
 * @author ebahtijaragic
 */
public class MucKixmppServerModule implements KixmppServerModule {
	private static final Logger logger = LoggerFactory.getLogger(MucKixmppServerModule.class);
	
	private Set<MucRoomMessageListener> messageListeners = Collections.newSetFromMap(new ConcurrentHashMap<MucRoomMessageListener, Boolean>());

	private KixmppServer server;
	
	private ConcurrentHashMap<String, MucService> services = new ConcurrentHashMap<>();
	
	private MucHistoryProvider historyProvider = new MucHistoryProvider() {
		private final List<MucHistory> emptyList = Collections.unmodifiableList(new ArrayList<MucHistory>(0));

		@Override
		public Promise<List<MucHistory>> getHistory(KixmppJid roomJid, KixmppJid userJid, Integer maxChars, Integer maxStanzas, Integer seconds, String since) {
			Promise<List<MucHistory>> promise = server.createPromise();
			promise.setSuccess(emptyList);
			return promise;
		}
	};
	
	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#install(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void install(KixmppServer server) {
		this.server = server;
		
		this.server.getEventEngine().registerGlobalStanzaHandler("presence", JOIN_ROOM_HANDLER);
		this.server.getEventEngine().registerGlobalStanzaHandler("presence", LEAVE_ROOM_HANDLER);
		this.server.getEventEngine().registerGlobalStanzaHandler("message", ROOM_MESSAGE_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#uninstall(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void uninstall(KixmppServer server) {
		this.server.getEventEngine().unregisterGlobalStanzaHandler("presence", JOIN_ROOM_HANDLER);
		this.server.getEventEngine().unregisterGlobalStanzaHandler("presence", LEAVE_ROOM_HANDLER);
		this.server.getEventEngine().unregisterGlobalStanzaHandler("message", ROOM_MESSAGE_HANDLER);
	}
	
	/**
	 * @param listener the listener to add
	 */
	public void addRoomMessageListener(MucRoomMessageListener listener) {
		messageListeners.add(listener);
	}

	/**
	 * @param listener the listener to remove
	 */
	public void removeRoomMessageListener(MucRoomMessageListener listener) {
		messageListeners.remove(listener);
	}
	
	/**
	 * Publish a message for the listeners to pick up.
	 * 
	 * @param roomJid
	 * @param sender
	 * @param messages
	 */
	protected void publishMessage(KixmppJid roomJid, KixmppJid sender, String senderNickname, String... messages) {
		server.getEventEngine().publishTask(
        		roomJid,
        		new InvokeListenersTask(this, 
        				roomJid, 
        				sender, 
        				senderNickname, 
        				messages)
        		);
	}
	
	/**
	 * Adds a {@link InMemoryMucService}
	 * 
	 * @param name
	 * @return
	 */
	public MucService addService(String name) {
		return addService(name.toLowerCase(), new InMemoryMucService(server, name));
	}

	/**
	 * Adds a {@link MucService}.
	 * 
	 * @param name
	 * @param service
	 * @return
	 */
	public MucService addService(String name, MucService service) {
		MucService prevService = services.putIfAbsent(name.toLowerCase(), service);
		
		return prevService == null ? service : prevService;
	}
	
	/**
	 * Gets a {@link MucService}.
	 * 
	 * @param name
	 * @return
	 */
	public MucService getService(String name) {
		return services.get(name);
	}
	
	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#getFeatures(io.netty.channel.Channel)
	 */
	public List<Element> getFeatures(Channel channel) {
		return null;
	}
	
	/**
	 * @return the historyProvider
	 */
	public MucHistoryProvider getHistoryProvider() {
		return historyProvider;
	}

	/**
	 * @param historyProvider the historyProvider to set
	 */
	public void setHistoryProvider(MucHistoryProvider historyProvider) {
		this.historyProvider = historyProvider;
	}

	/**
	 * Figures out what to do with a {@link RoomTask}.
	 * 
	 * @param roomTask
	 */
	public void handleClusterTask(RoomTask roomTask) {
		if (roomTask instanceof RoomBroadcastTask) {
			RoomBroadcastTask broadcastTask = (RoomBroadcastTask)roomTask;

			KixmppJid roomJid = new KixmppJid(broadcastTask.getRoomId(), broadcastTask.getServiceSubDomain() + "." + server.getDomain());
			
			publishMessage(roomJid, broadcastTask.getFromRoomJid(), broadcastTask.getNickname(), broadcastTask.getMessages());
		}
		
        MucService service = getService(roomTask.getServiceSubDomain());
        if (service == null) {
            return;
        }
        
        MucRoom room = service.getRoom(roomTask.getRoomId());
        if (room == null) {
            return;
        }
        
        roomTask.setRoom(room);
        
        server.getEventEngine().publishTask(room.getRoomJid(),roomTask);
	}
	
	private KixmppStanzaHandler JOIN_ROOM_HANDLER = new KixmppStanzaHandler() {
		/**
		 * @see com.kixeye.kixmpp.server.KixmppStanzaHandler#handle(io.netty.channel.Channel, org.jdom2.Element)
		 */
		public void handle(Channel channel, Element stanza) {
			Element x = stanza.getChild("x", Namespace.getNamespace("http://jabber.org/protocol/muc"));
			
			if (x != null) {
				KixmppJid fullRoomJid = KixmppJid.fromRawJid(stanza.getAttributeValue("to"));
				
				MucService service = services.get(fullRoomJid.getDomain().toLowerCase().replace("." + server.getDomain(), ""));

				if (service != null) {
					MucRoom room = service.getRoom(fullRoomJid.getNode());
					
					if (room != null) {
                        server.getEventEngine().publishTask(room.getRoomJid(), 
                        		new JoinRoomTask(channel, room, fullRoomJid.getResource(), x));
					} // TODO handle else
				} // TODO handle else
			}
		}
	};

	private KixmppStanzaHandler LEAVE_ROOM_HANDLER = new KixmppStanzaHandler() {
		/**
		 * @see com.kixeye.kixmpp.server.KixmppStanzaHandler#handle(io.netty.channel.Channel, org.jdom2.Element)
		 */
		public void handle(Channel channel, Element stanza) {
			if (stanza.getAttribute("type") != null && stanza.getAttribute("to") != null) {
				if (stanza.getAttributeValue("type").equals("unavailable")) {
					KixmppJid fullRoomJid = KixmppJid.fromRawJid(stanza.getAttributeValue("to"));
					MucService service = services.get(fullRoomJid.getDomain().toLowerCase().replace("." + server.getDomain(), ""));
					if (service != null) {
						MucRoom room = service.getRoom(fullRoomJid.getNode());
						if (room != null) {
							server.getEventEngine().publishTask(room.getRoomJid(),
									new LeaveRoomTask(channel, room, fullRoomJid.getResource()));
						}
					}
				}
			}
		}
	};

	private KixmppStanzaHandler ROOM_MESSAGE_HANDLER = new KixmppStanzaHandler() {
		/**
		 * @see com.kixeye.kixmpp.server.KixmppStanzaHandler#handle(io.netty.channel.Channel, org.jdom2.Element)
		 */
		public void handle(Channel channel, Element stanza) {
			if ("groupchat".equals(stanza.getAttributeValue("type"))) {
				KixmppJid fullRoomJid = KixmppJid.fromRawJid(stanza.getAttributeValue("to"));

				MucService service = services.get(fullRoomJid.getDomain().toLowerCase().replace("." + server.getDomain(), ""));
				
				if (service != null) {
					MucRoom room = service.getRoom(fullRoomJid.getNode());

					if (room != null) {
                        Element body = stanza.getChild("body", stanza.getNamespace());
                        
                        server.getEventEngine().publishTask(room.getRoomJid(), 
                        		new ReceiveMessageTask(channel.attr(BindKixmppServerModule.JID).get(), room, body.getText()));
					} // TODO handle else
				} // TODO handle else
			}
		}
	};
	
	private static class JoinRoomTask extends Task {
		private final Channel channel;
		private final MucRoom room;
		private final String nickname;
		private final Element x;

		public JoinRoomTask(Channel channel, MucRoom room, String nickname, Element x) {
			this.channel = channel;
			this.room = room;
			this.nickname = nickname;
			this.x = x;
		}

		public void run() {
			room.join(channel, nickname, x);
		}
	}

	private static class LeaveRoomTask extends Task {
		private final Channel channel;
		private final MucRoom room;
		private final String nickname;

		public LeaveRoomTask(Channel channel, MucRoom room, String nickname) {
			this.channel = channel;
			this.room = room;
			this.nickname = nickname;
		}

		public void run() {
			room.userLeft(channel, nickname);
		}
	}

	private static class ReceiveMessageTask extends Task {
		private final KixmppJid sender;
		private final MucRoom room;
		private final String body;
		
		public ReceiveMessageTask(KixmppJid sender, MucRoom room, String body) {
			this.sender = sender;
			this.room = room;
			this.body = body;
		}

		public void run() {
            room.receiveMessages(sender, true, body);
		}
	}
	
	private static class InvokeListenersTask extends Task {
		private final MucKixmppServerModule module;
		private final KixmppJid roomJid;
		private final KixmppJid sender;
		private final String senderNickname;
		private final String[] messages;
		
		/**
		 * @param module
		 * @param roomJid
		 * @param sender
		 * @param senderNickname
		 * @param messages
		 */
		public InvokeListenersTask(MucKixmppServerModule module,
				KixmppJid roomJid, KixmppJid sender, String senderNickname,
				String[] messages) {
			this.module = module;
			this.roomJid = roomJid;
			this.sender = sender;
			this.senderNickname = senderNickname;
			this.messages = messages;
		}

		public void run() {
			for (MucRoomMessageListener listener : module.messageListeners) {
				try {
					listener.handle(roomJid, sender, senderNickname, messages);
				} catch (Exception e) {
					logger.error("Error while invoking listener: [{}].", listener, e);
				}
			}
		}
	}
}
