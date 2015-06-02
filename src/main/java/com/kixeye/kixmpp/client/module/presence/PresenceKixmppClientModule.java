package com.kixeye.kixmpp.client.module.presence;

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
 * A module that handles presence info.
 * 
 * @author ebahtijaragic
 */
public class PresenceKixmppClientModule implements KixmppClientModule {
	private static final Logger logger = LoggerFactory.getLogger(PresenceKixmppClientModule.class);
	
	private Set<PresenceListener> presenceListeners = Collections.newSetFromMap(new ConcurrentHashMap<PresenceListener, Boolean>());
	
	private KixmppClient client = null;
	
	/**
	 * @param listener the listener to add
	 */
	public void addPresenceListener(PresenceListener listener) {
		presenceListeners.add(listener);
	}

	/**
	 * @param listener the listener to remove
	 */
	public void removePresenceListener(PresenceListener listener) {
		presenceListeners.remove(listener);
	}

	/**
	 * @see com.kixeye.kixmpp.client.module.KixmppClientModule#install(com.kixeye.kixmpp.client.KixmppClient)
	 */
	public void install(KixmppClient client) {
		this.client = client;

		client.getEventEngine().registerGlobalStanzaHandler("presence", presenceHandler);
	}

	/**
	 * @see com.kixeye.kixmpp.client.module.KixmppClientModule#uninstall(com.kixeye.kixmpp.client.KixmppClient)
	 */
	public void uninstall(KixmppClient client) {
		client.getEventEngine().unregisterGlobalStanzaHandler("presence", presenceHandler);
	}

	/**
	 * Updates the current user's presence.
	 *     
	 * @param presence
	 */
    public void updatePresence(Presence presence) {
    	Element presenceElement = new Element("presence");
    	
    	if (presence.getType() != null) {
    		presenceElement.setAttribute("type", presence.getType());
    	}
    	
    	if (presence.getStatus() != null) {
    		Element statusElement = new Element("status");
    		statusElement.setText(presence.getStatus());
    		
    		presenceElement.addContent(statusElement);
    	}
    	
    	if (presence.getShow() != null) {
    		Element showElement = new Element("show");
    		showElement.setText(presence.getShow());
    		
    		presenceElement.addContent(showElement);
    	}
		
		client.sendStanza(presenceElement);
    }

	private KixmppStanzaHandler presenceHandler = new KixmppStanzaHandler() {
		public void handle(Channel channel, Element stanza) {
			Presence presence = new Presence(KixmppJid.fromRawJid(stanza.getAttributeValue("from")), 
					KixmppJid.fromRawJid(stanza.getAttributeValue("to")), 
					stanza.getAttributeValue("type"), 
					stanza.getChildText("status", stanza.getNamespace()), 
					stanza.getChildText("show", stanza.getNamespace()));
			
			for (PresenceListener listener : presenceListeners) {
				try {
					listener.handle(presence);
				} catch (Exception e) {
					logger.error("Exception thrown while executing Presence listener", e);
				}
			}
		}
	};
}
