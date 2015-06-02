package com.kixeye.kixmpp.server.module.auth;

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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.AttributeKey;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.jdom2.Element;

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.handler.KixmppStanzaHandler;
import com.kixeye.kixmpp.server.KixmppServer;
import com.kixeye.kixmpp.server.module.KixmppServerModule;
import com.kixeye.kixmpp.server.module.bind.BindKixmppServerModule;

/**
 * Handles SASL auth.
 * 
 * @author ebahtijaragic
 */
public class SaslKixmppServerModule implements KixmppServerModule {
	public static AttributeKey<Boolean> IS_AUTHENTICATED = AttributeKey.valueOf("IS_AUTHENTICATED");
	
	private AuthenticationService authenticationService;
	
	private KixmppServer server;
	
	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#install(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void install(KixmppServer server) {
		this.server = server;
		this.authenticationService = new InMemoryAuthenticationService(server);
		
		this.server.getEventEngine().registerGlobalStanzaHandler("auth", AUTH_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#uninstall(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void uninstall(KixmppServer server) {
		this.server.getEventEngine().unregisterGlobalStanzaHandler("auth", AUTH_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppServerModule#getFeatures(io.netty.channel.Channel)
	 */
	public List<Element> getFeatures(Channel channel) {
		List<Element> features = new LinkedList<>();
		
		Boolean isAuthed = channel.attr(IS_AUTHENTICATED).get();
		
		if (isAuthed == null || isAuthed == false) {
			Element mechanisms = new Element("mechanisms", null, "urn:ietf:params:xml:ns:xmpp-sasl");
			
			Element plainMechanism = new Element("mechanism", "urn:ietf:params:xml:ns:xmpp-sasl");
			plainMechanism.setText("PLAIN");
			
			mechanisms.addContent(plainMechanism);
			
			features.add(mechanisms);
		}
		
		return features;
	}
	
	/**
	 * @return the authenticationService
	 */
	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	/**
	 * @param authenticationService the authenticationService to set
	 */
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	private KixmppStanzaHandler AUTH_HANDLER = new KixmppStanzaHandler() {
		/**
		 * @see com.kixeye.kixmpp.server.KixmppStanzaHandler#handle(io.netty.channel.Channel, org.jdom2.Element)
		 */
		public void handle(final Channel channel, Element stanza) {
			if ("PLAIN".equals(stanza.getAttributeValue("mechanism"))) {
				String base64Encoded = stanza.getText();
				
				ByteBuf encodecCredentials = channel.alloc().buffer().writeBytes(base64Encoded.getBytes(StandardCharsets.UTF_8));
				ByteBuf rawCredentials = Base64.decode(encodecCredentials);
				String raw = rawCredentials.toString(StandardCharsets.UTF_8);
				encodecCredentials.release();
				rawCredentials.release();
				
				String[] credentialsSplit = raw.split("\0");
				if (credentialsSplit.length > 1) {
					final String username = credentialsSplit[1];

					authenticationService.authenticate(username, credentialsSplit[2]).addListener(
						new GenericFutureListener<Future<Boolean>>() {
							@Override
							public void operationComplete(final Future<Boolean> future) throws Exception {
								if (future.isSuccess()) {
									Boolean authResult = future.getNow();
									if (authResult) {
										channel.attr(IS_AUTHENTICATED).set(true);
										channel.attr(BindKixmppServerModule.JID).set(new KixmppJid(username, server.getDomain(), UUID.randomUUID().toString().replace("-", "")));
										Element success = new Element("success", null, "urn:ietf:params:xml:ns:xmpp-sasl");
										channel.writeAndFlush(success);
									} else {
										Element failure = new Element("failure", null, "urn:ietf:params:xml:ns:xmpp-sasl");
										channel.writeAndFlush(failure);
									}
								} else {
									Element failure = new Element("failure", null, "urn:ietf:params:xml:ns:xmpp-sasl");
									channel.writeAndFlush(failure);
								}
							}
						}
					);
				} else {
					Element failure = new Element("failure", null, "urn:ietf:params:xml:ns:xmpp-sasl");
					
					channel.writeAndFlush(failure);
				}
			} else {
				Element failure = new Element("failure", null, "urn:ietf:params:xml:ns:xmpp-sasl");
				
				channel.writeAndFlush(failure);
			}
		}
	};
}
