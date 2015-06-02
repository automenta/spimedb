package com.kixeye.kixmpp.server.module.chat;

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

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.handler.KixmppStanzaHandler;
import com.kixeye.kixmpp.server.KixmppServer;
import com.kixeye.kixmpp.server.cluster.message.PrivateChatTask;
import com.kixeye.kixmpp.server.module.KixmppServerModule;
import com.kixeye.kixmpp.server.module.bind.BindKixmppServerModule;

/**
 * Handles private chat features.
 * 
 * @author ebahtijaragic
 */
public class ChatKixmppServerModule implements KixmppServerModule {
	private KixmppServer server;

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#install(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void install(KixmppServer server) {
		this.server = server;
		
		this.server.getEventEngine().registerGlobalStanzaHandler("message", MESSAGE_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#uninstall(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void uninstall(KixmppServer server) {
		this.server.getEventEngine().unregisterGlobalStanzaHandler("message", MESSAGE_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#getFeatures(io.netty.channel.Channel)
	 */
	public List<Element> getFeatures(Channel channel) {
		return null;
	}
	
	private KixmppStanzaHandler MESSAGE_HANDLER = new KixmppStanzaHandler() {
		/**
		 * @see com.kixeye.kixmpp.server.KixmppStanzaHandler#handle(io.netty.channel.Channel, org.jdom2.Element)
		 */
		public void handle(Channel channel, Element stanza) {
			if ("chat".equals(stanza.getAttributeValue("type"))) {
				KixmppJid fromJid = channel.attr(BindKixmppServerModule.JID).get();
				KixmppJid toJid = KixmppJid.fromRawJid(stanza.getAttributeValue("to"));
				String body = stanza.getChildText("body", stanza.getNamespace());

				server.getCluster().sendMessageToAll(new PrivateChatTask(fromJid, toJid, body), true);
			}
		}
	};

}
