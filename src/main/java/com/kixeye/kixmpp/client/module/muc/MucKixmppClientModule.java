package com.kixeye.kixmpp.client.module.muc;

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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.client.KixmppClient;
import com.kixeye.kixmpp.client.module.KixmppClientModule;
import com.kixeye.kixmpp.date.XmppDateUtils;
import com.kixeye.kixmpp.handler.KixmppStanzaHandler;

/**
 * A {@link KixmppClientModule} that deals with MUCs.
 * 
 * @author ebahtijaragic
 */
public class MucKixmppClientModule implements KixmppClientModule {
	private static final Logger logger = LoggerFactory.getLogger(MucKixmppClientModule.class);
	
	private Set<MucListener<MucMessage>> messageListeners = Collections.newSetFromMap(new ConcurrentHashMap<MucListener<MucMessage>, Boolean>());
	private Set<MucListener<MucInvite>> invitationListeners = Collections.newSetFromMap(new ConcurrentHashMap<MucListener<MucInvite>, Boolean>());
	private Set<MucListener<MucJoin>> joinListeners = Collections.newSetFromMap(new ConcurrentHashMap<MucListener<MucJoin>, Boolean>());
	
	private KixmppClient client = null;
	
	/**
	 * @param listener the listener to add
	 */
	public void addMessageListener(MucListener<MucMessage> listener) {
		messageListeners.add(listener);
	}

	/**
	 * @param listener the listener to add
	 */
	public void removeMessageListener(MucListener<MucMessage> listener) {
		messageListeners.remove(listener);
	}

	/**
	 * @param listener the listener to add
	 */
	public void addInviteListener(MucListener<MucInvite> listener) {
		invitationListeners.add(listener);
	}

	/**
	 * @param listener the listener to add
	 */
	public void removeInviteListener(MucListener<MucInvite> listener) {
		invitationListeners.remove(listener);
	}
	
	/**
	 * @param listener the listener to add
	 */
	public void addJoinListener(MucListener<MucJoin> listener) {
		joinListeners.add(listener);
	}
	
	/**
	 * @param listener the listener to add
	 */
	public void removeJoinListener(MucListener<MucJoin> listener) {
		joinListeners.remove(listener);
	}
	
	/**
	 * Joins a room.
	 * 
	 * @param roomJid
	 * @param nickname
	 */
	public void joinRoom(KixmppJid roomJid, String nickname, Integer maxStanzas, Integer maxChars, Integer seconds, DateTime since) {
		Element presence = new Element("presence");
		presence.setAttribute("from", client.getJid().getFullJid());
		presence.setAttribute("to", roomJid + "/" + nickname);

		Element x = new Element("x", "http://jabber.org/protocol/muc");
		presence.addContent(x);

		Element history = new Element("history", "http://jabber.org/protocol/muc");
		if (maxStanzas != null) {
			history.setAttribute("maxstanzas", maxStanzas.toString());
		}
		if (maxChars != null) {
			history.setAttribute("maxchars", maxChars.toString());
		}
		if (seconds != null) {
			history.setAttribute("seconds", seconds.toString());
		}
		if (since != null) {
			history.setAttribute("since", XmppDateUtils.format(since));
		}
		x.addContent(history);
		
		client.sendStanza(presence);
	}
	
	/**
	 * Joins a room.
	 * 
	 * @param roomJid
	 * @param nickname
	 */
	public void joinRoom(KixmppJid roomJid, String nickname) {
		joinRoom(roomJid, nickname, null, null, null, null);
	}
	
	/**
	 * Joins a room with an invitation.
	 * 
	 * @param invitation
	 * @param invitation
	 */
	public void joinRoom(MucInvite invitation, String nickname) {
		joinRoom(invitation.getRoomJid(), nickname);
	}
	
	/**
	 * Sends a room message to a room.
	 * 
	 * @param roomJid
	 * @param roomMessage
	 */
	public void sendRoomMessage(KixmppJid roomJid, String roomMessage, String nickname) {
		Element message = new Element("message");
		message.setAttribute("from", client.getJid().toString());
		message.setAttribute("to", roomJid.toString());
		message.setAttribute("type", "groupchat");
		
		Element bodyElement = new Element("body", message.getNamespace());
		bodyElement.setText(roomMessage);
		message.addContent(bodyElement);

		client.sendStanza(message);
	}
	
	/**
	 * @see com.kixeye.kixmpp.client.module.KixmppClientModule#install(com.kixeye.kixmpp.client.KixmppClient)
	 */
	public void install(KixmppClient client) {
		this.client = client;
		
		client.getEventEngine().registerGlobalStanzaHandler("message", mucMessageHandler);
		client.getEventEngine().registerGlobalStanzaHandler("presence", mucPresenceHandler);
	}

	/**
	 * @see com.kixeye.kixmpp.client.module.KixmppClientModule#uninstall(com.kixeye.kixmpp.client.KixmppClient)
	 */
	public void uninstall(KixmppClient client) {
		client.getEventEngine().unregisterGlobalStanzaHandler("message", mucMessageHandler);
		client.getEventEngine().unregisterGlobalStanzaHandler("presence", mucPresenceHandler);
	}
	
	private KixmppStanzaHandler mucPresenceHandler = new KixmppStanzaHandler() {
		@Override
		public void handle(Channel channel, Element stanza) {
			Element inX  = stanza.getChild("x", Namespace.getNamespace("http://jabber.org/protocol/muc#user"));
			
			if (inX != null) {
				Element inItem = inX.getChild("item", Namespace.getNamespace("http://jabber.org/protocol/muc#user"));
				
				if (inItem != null) {
					KixmppJid from = KixmppJid.fromRawJid(stanza.getAttributeValue("from"));
					
					MucJoin message = new MucJoin(from.withoutResource(), 
							from, 
							inItem.getAttributeValue("affiliation"), 
							inItem.getAttributeValue("role"));
					
					for (MucListener<MucJoin> listener : joinListeners) {
						try {
							listener.handle(message);
						} catch (Exception e) {
							logger.error("Exception thrown while executing MucJoin listener", e);
						}
					}
				}
			}
		}
	};
	
	private KixmppStanzaHandler mucMessageHandler = new KixmppStanzaHandler() {
		public void handle(Channel channel, Element stanza) {
			String type = stanza.getAttributeValue("type");
			
			if (type == null) {
				type = "";
			}
			
			switch (type) {
				case "chat":
					// ignore
					break;
				case "groupchat":
					String language = null;
					String bodyMessage = null;
					
					Element body = stanza.getChild("body", stanza.getNamespace());
					
					if (body != null) {
						bodyMessage = body.getText();
						language = body.getAttributeValue("xml:lang");

						MucMessage message = new MucMessage(KixmppJid.fromRawJid(stanza.getAttributeValue("from")), KixmppJid.fromRawJid(stanza.getAttributeValue("to")), language, bodyMessage);
						
						for (MucListener<MucMessage> listener : messageListeners) {
							try {
								listener.handle(message);
							} catch (Exception e) {
								logger.error("Exception thrown while executing MucInvite listener", e);
							}
						}
					}
					
					break;
				case "":
					// check if invite
					for (Element invitation : stanza.getChildren("x", Namespace.getNamespace("jabber:x:conference"))) {
						KixmppJid roomJid = KixmppJid.fromRawJid(invitation.getAttributeValue("jid"));
                        KixmppJid toJid = KixmppJid.fromRawJid(stanza.getAttributeValue("to"));
                        KixmppJid fromJid = KixmppJid.fromRawJid(stanza.getAttributeValue("from"));
						MucInvite invite = new MucInvite(fromJid, toJid, roomJid);
						for (MucListener<MucInvite> listener : invitationListeners) {
							try {
								listener.handle(invite);
							} catch (Exception e) {
								logger.error("Exception thrown while executing MucInvite listener", e);
							}
						}
					}
                    for (Element x : stanza.getChildren("x",  Namespace.getNamespace("http://jabber.org/protocol/muc#user"))) {
                        for (Element i : x.getChildren("invite")) {
                            KixmppJid roomJid = KixmppJid.fromRawJid( stanza.getAttributeValue("from"));
                            KixmppJid toJid =  KixmppJid.fromRawJid( stanza.getAttributeValue("to"));
                            KixmppJid fromJid = KixmppJid.fromRawJid( i.getAttributeValue("from"));
                            MucInvite invite = new MucInvite(fromJid,toJid,roomJid);
                            for (MucListener<MucInvite> listener : invitationListeners) {
                                try {
                                    listener.handle(invite);
                                } catch (Exception e) {
                                    logger.error("Exception thrown while executing MucInvite listener", e);
                                }
                            }
                        }
                    }
					break;
			}
		}
	};
}
