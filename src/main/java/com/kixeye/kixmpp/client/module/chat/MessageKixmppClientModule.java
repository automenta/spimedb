package com.kixeye.kixmpp.client.module.chat;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.client.KixmppClient;
import com.kixeye.kixmpp.client.module.KixmppClientModule;
import com.kixeye.kixmpp.handler.KixmppStanzaHandler;

/**
 * A module that handles private messages.
 * 
 * @author ebahtijaragic
 */
public class MessageKixmppClientModule implements KixmppClientModule {
	private static final Logger logger = LoggerFactory.getLogger(MessageKixmppClientModule.class);
	
	private Set<MessageListener> messageListeners = Collections.newSetFromMap(new ConcurrentHashMap<MessageListener, Boolean>());

	private KixmppClient client = null;
	
	/**
	 * @param listener the listener to add
	 */
	public void addMessageListener(MessageListener listener) {
		messageListeners.add(listener);
	}

	/**
	 * @param listener the listener to remove
	 */
	public void removeMessageListener(MessageListener listener) {
		messageListeners.remove(listener);
	}

	/**
	 * @see com.kixeye.kixmpp.client.module.KixmppClientModule#install(com.kixeye.kixmpp.client.KixmppClient)
	 */
	public void install(KixmppClient client) {
		this.client = client;

		client.getEventEngine().registerGlobalStanzaHandler("message", messageHandler);
	}

	/**
	 * @see com.kixeye.kixmpp.client.module.KixmppClientModule#uninstall(com.kixeye.kixmpp.client.KixmppClient)
	 */
	public void uninstall(KixmppClient client) {
		client.getEventEngine().unregisterGlobalStanzaHandler("message", messageHandler);
	}

	/**
	 * Sends a private message.
	 * 
	 * @param toJid
	 * @param body
	 */
    public void sendMessage(KixmppJid toJid, String body) {
    	Element messageElement = new Element("message");
    	messageElement.setAttribute("type", "chat");
    	messageElement.setAttribute("from", client.getJid().getFullJid());
    	messageElement.setAttribute("to", toJid.getBaseJid());
    	
    	Element bodyElement = new Element("body");
    	bodyElement.setText(body);
    	
    	messageElement.addContent(bodyElement);
    	
		client.sendStanza(messageElement);
    }

	private KixmppStanzaHandler messageHandler = new KixmppStanzaHandler() {
		public void handle(Channel channel, Element stanza) {
			if ("chat".equals(stanza.getAttributeValue("type"))) {
				Message message = new Message(KixmppJid.fromRawJid(stanza.getAttributeValue("from")), 
						KixmppJid.fromRawJid(stanza.getAttributeValue("to")), 
						stanza.getChildText("body", stanza.getNamespace()));
				
				for (MessageListener listener : messageListeners) {
					try {
						listener.handle(message);
					} catch (Exception e) {
						logger.error("Exception thrown while executing message listener", e);
					}
				}
			}
		}
	};
}
