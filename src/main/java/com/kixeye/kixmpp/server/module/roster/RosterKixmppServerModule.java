package com.kixeye.kixmpp.server.module.roster;

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

import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.Future;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.handler.KixmppStanzaHandler;
import com.kixeye.kixmpp.server.KixmppServer;
import com.kixeye.kixmpp.server.module.KixmppServerModule;
import com.kixeye.kixmpp.server.module.bind.BindKixmppServerModule;

/**
 * Handles roster features.
 * 
 * @author ebahtijaragic
 */
public class RosterKixmppServerModule implements KixmppServerModule {
	public static final RosterProvider NOOP_ROSTER_PROVIDER = new RosterProvider() {

		private final List<RosterItem> emptyList = Collections.unmodifiableList(new ArrayList<RosterItem>(0));
		
		public Promise<List<RosterItem>> getRoster(KixmppJid userJid) {
			return ImmediateEventExecutor.INSTANCE.<List<RosterItem>>newPromise().setSuccess(emptyList);
		}
	};
	
	private KixmppServer server;
	
	private RosterProvider rosterProvider = NOOP_ROSTER_PROVIDER;
	
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
	
	/**
	 * @return the rosterProvider
	 */
	public RosterProvider getRosterProvider() {
		return rosterProvider;
	}

	/**
	 * @param rosterProvider the rosterProvider to set
	 */
	public void setRosterProvider(RosterProvider rosterProvider) {
		this.rosterProvider = rosterProvider;
	}

	private KixmppStanzaHandler ROSTER_HANDLER = new KixmppStanzaHandler() {
		/**
		 * @see com.kixeye.kixmpp.server.KixmppStanzaHandler#handle(io.netty.channel.Channel, org.jdom2.Element)
		 */
		public void handle(final Channel channel, final Element stanza) {

			Element query = stanza.getChild("query", Namespace.getNamespace("jabber:iq:roster"));
			
			if (query != null) {

				Promise<List<RosterItem>> promise = rosterProvider.getRoster(channel.attr(BindKixmppServerModule.JID).get());

				promise.addListener(new GenericFutureListener<Future<List<RosterItem>>>() {

					@Override
					public void operationComplete(final Future<List<RosterItem>> future) throws Exception {

						Element iq = new Element("iq");
						iq.setAttribute("type", "result");

						String id = stanza.getAttributeValue("id");
						if (id != null) {
							iq.setAttribute("id", id);
						}

						Element queryResult = new Element("query", Namespace.getNamespace("jabber:iq:roster"));

						if (future.isSuccess()) {
							final List<RosterItem> roster = future.getNow();
							if (roster != null && !roster.isEmpty()) {
								for (RosterItem rosterItem : roster) {
									Element item = new Element("item", queryResult.getNamespace());

									if (rosterItem.getJid() != null) {
										item.setAttribute("jid", rosterItem.getJid().getFullJid());
									}
									if (rosterItem .getName() != null) {
										item.setAttribute("name", rosterItem.getName());
									}
									if (rosterItem.getSubscription() != null) {
										item.setAttribute("subscription", rosterItem.getSubscription().name());
									}
									if (rosterItem.getGroup() != null) {
										Element group = new Element("group", item.getNamespace());
										group.setText(rosterItem.getGroup());

										item.addContent(group);
									}

									queryResult.addContent(item);
								}
							}
						}
						iq.addContent(queryResult);

						channel.writeAndFlush(iq);
					}
				});
			}
		}
	};
}
