package com.kixeye.kixmpp.server.module.session;

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

import com.kixeye.kixmpp.KixmppStreamStart;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.LinkedList;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;

import com.kixeye.kixmpp.handler.KixmppStanzaHandler;
import com.kixeye.kixmpp.server.KixmppServer;
import com.kixeye.kixmpp.server.module.KixmppServerModule;

/**
 * Handles sessions.
 * 
 * @author ebahtijaragic
 */
public class SessionKixmppServerModule implements KixmppServerModule {
	public static AttributeKey<Boolean> IS_SESSION_ESTABLISHED = AttributeKey.valueOf("IS_SESSION_ESTABLISHED");
	
	private KixmppServer server;
	
	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#install(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void install(KixmppServer server) {
		this.server = server;
		
		this.server.getEventEngine().registerGlobalStanzaHandler("iq", SESSION_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#uninstall(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void uninstall(KixmppServer server) {
		this.server.getEventEngine().unregisterGlobalStanzaHandler("iq", SESSION_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#getFeatures(io.netty.channel.Channel)
	 */
	public List<Element> getFeatures(Channel channel) {
		List<Element> features = new LinkedList<>();

		Boolean isSessionEnabled = channel.attr(IS_SESSION_ESTABLISHED).get();
		
		if (isSessionEnabled == null || isSessionEnabled == false) {
			Element bind = new Element("session", null, "urn:ietf:params:xml:ns:xmpp-session");
			
			features.add(bind);
		}
		
		return features;
	}
	
	private KixmppStanzaHandler SESSION_HANDLER = new KixmppStanzaHandler() {
		/**
		 * @see com.kixeye.kixmpp.server.KixmppStanzaHandler#handle(io.netty.channel.Channel, org.jdom2.Element)
		 */
		public void handle(Channel channel, Element stanza) {
			Element session = stanza.getChild("session", Namespace.getNamespace("urn:ietf:params:xml:ns:xmpp-session"));
			
			if (session != null) {
				channel.attr(IS_SESSION_ESTABLISHED).set(true);
				
				Element iq = new Element("iq");
				iq.setAttribute("type", "result");
				
				String id = stanza.getAttributeValue("id");
				
				if (id != null) {
					iq.setAttribute("id", id);
				}
				
				channel.writeAndFlush(iq);
				server.getEventEngine().publishSessionStart(channel);
			}
		}
	};
}
