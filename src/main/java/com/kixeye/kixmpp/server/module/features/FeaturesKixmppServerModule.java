package com.kixeye.kixmpp.server.module.features;

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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.List;

import org.jdom2.Element;

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.KixmppStreamEnd;
import com.kixeye.kixmpp.KixmppStreamStart;
import com.kixeye.kixmpp.handler.KixmppStreamHandler;
import com.kixeye.kixmpp.server.KixmppServer;
import com.kixeye.kixmpp.server.module.KixmppServerModule;
import com.kixeye.kixmpp.server.module.auth.SaslKixmppServerModule;

/**
 * Displays features to the client.
 * 
 * @author ebahtijaragic
 */
public class FeaturesKixmppServerModule implements KixmppServerModule {
	private KixmppServer server;
	
	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#install(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void install(KixmppServer server) {
		this.server = server;
		
		this.server.getEventEngine().registerStreamHandler(SERVER_FEATURE_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#uninstall(com.kixeye.kixmpp.server.KixmppServer)
	 */
	public void uninstall(KixmppServer server) {
		this.server.getEventEngine().unregisterStreamHandler(SERVER_FEATURE_HANDLER);
	}

	/**
	 * @see com.kixeye.kixmpp.server.module.KixmppModule#getFeatures(io.netty.channel.Channel)
	 */
	public List<Element> getFeatures(Channel channel) {
		return null;
	}
	
	private KixmppStreamHandler SERVER_FEATURE_HANDLER = new KixmppStreamHandler() {
		/**
		 * @see com.kixeye.kixmpp.server.KixmppStreamHandler#handleStreamStart(io.netty.channel.Channel, com.kixeye.kixmpp.KixmppStreamStart)
		 */
		public void handleStreamStart(Channel channel, KixmppStreamStart streamStart) {
			Boolean isAuthed = channel.attr(SaslKixmppServerModule.IS_AUTHENTICATED).get();
			
			channel.writeAndFlush(new KixmppStreamStart(new KixmppJid(server.getDomain()), null, isAuthed == null, "" + channel.hashCode()));
			
			Element features = new Element("features", "stream", "http://etherx.jabber.org/streams");
			
			for (KixmppServerModule module : server.modules()) {
				List<Element> featuresList = module.getFeatures(channel);
				
				if (featuresList != null) {
					for (Element featureElement : featuresList) {
						features.addContent(featureElement);
					}
				}
			}
			
			channel.writeAndFlush(features);
		}

		/**
		 * @see com.kixeye.kixmpp.server.KixmppStreamHandler#handleStreamEnd(io.netty.channel.Channel, com.kixeye.kixmpp.KixmppStreamEnd)
		 */
		public void handleStreamEnd(final Channel channel, KixmppStreamEnd streamEnd) {
			channel.writeAndFlush(new KixmppStreamEnd()).addListener(new GenericFutureListener<Future<? super Void>>() {
				public void operationComplete(Future<? super Void> future) throws Exception {
					channel.close();
				}
			});
		}
	};
}
