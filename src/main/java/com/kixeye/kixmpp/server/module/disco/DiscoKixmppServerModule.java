package com.kixeye.kixmpp.server.module.disco;

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

import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;

import com.kixeye.kixmpp.handler.KixmppStanzaHandler;
import com.kixeye.kixmpp.server.KixmppServer;
import com.kixeye.kixmpp.server.module.KixmppServerModule;

/**
 * Handles disco features.
 * 
 * @author ebahtijaragic
 */
public class DiscoKixmppServerModule implements KixmppServerModule {
	private KixmppServer server;
	
	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#install(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void install(KixmppServer server) {
		this.server = server;
		
		this.server.getEventEngine().registerGlobalStanzaHandler("iq", ROSTER_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#uninstall(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void uninstall(KixmppServer server) {
		this.server.getEventEngine().unregisterGlobalStanzaHandler("iq", ROSTER_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#getFeatures(io.netty.channel.Channel)
	 */
	public List<Element> getFeatures(Channel channel) {
		return null;
	}
	
	private KixmppStanzaHandler ROSTER_HANDLER = new KixmppStanzaHandler() {
		/**
		 * @see com.kixeye.kixmpp.server.KixmppStanzaHandler#handle(io.netty.channel.Channel, org.jdom2.Element)
		 */
		public void handle(Channel channel, Element stanza) {
			Element infoQuery = stanza.getChild("query", Namespace.getNamespace("http://jabber.org/protocol/disco#info"));
			Element itemsQuery = stanza.getChild("query", Namespace.getNamespace("http://jabber.org/protocol/disco#items"));
			
			if (infoQuery != null) {
				Element iq = new Element("iq");
				iq.setAttribute("type", "result");
				
				String to = stanza.getAttributeValue("to");
				
				if (to != null) {
					iq.setAttribute("from", to);
				}
				
				String id = stanza.getAttributeValue("id");
				
				if (id != null) {
					iq.setAttribute("id", id);
				}
				
				Element queryResult = new Element("query", Namespace.getNamespace("http://jabber.org/protocol/disco#info"));
				
				queryResult.addContent(new Element("feature", Namespace.getNamespace("http://jabber.org/protocol/disco#info")).setAttribute("var", "http://jabber.org/protocol/disco#info"));
				queryResult.addContent(new Element("feature", Namespace.getNamespace("http://jabber.org/protocol/disco#info")).setAttribute("var", "http://jabber.org/protocol/disco#items"));
				queryResult.addContent(new Element("feature", Namespace.getNamespace("http://jabber.org/protocol/disco#info")).setAttribute("var", "http://jabber.org/protocol/muc"));
				queryResult.addContent(new Element("feature", Namespace.getNamespace("http://jabber.org/protocol/disco#info")).setAttribute("var", "jabber:iq:time"));
				queryResult.addContent(new Element("feature", Namespace.getNamespace("http://jabber.org/protocol/disco#info")).setAttribute("var", "jabber:iq:version"));
				
				iq.addContent(queryResult);
				
				channel.writeAndFlush(iq);
			} else if (itemsQuery != null) {
				Element iq = new Element("iq");
				iq.setAttribute("type", "result");
				
				String to = stanza.getAttributeValue("to");
				
				if (to != null) {
					iq.setAttribute("from", to);
				}
				
				String id = stanza.getAttributeValue("id");
				
				if (id != null) {
					iq.setAttribute("id", id);
				}
				
				Element queryResult = new Element("query", Namespace.getNamespace("http://jabber.org/protocol/disco#items"));
				iq.addContent(queryResult);
				
				channel.writeAndFlush(iq);
			}
		}
	};
}
