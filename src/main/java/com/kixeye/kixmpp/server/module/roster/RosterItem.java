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

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.server.module.presence.PresenceSubscription;

/**
 * Defines a roster item.
 * 
 * @author ebahtijaragic
 */
public class RosterItem {
	private final KixmppJid jid;
	private final String name;
	private final PresenceSubscription subscription;
	private final String group;
	
	/**
	 * @param jid
	 * @param name
	 * @param subscription
	 * @param group
	 */
	public RosterItem(KixmppJid jid, String name, PresenceSubscription subscription, String group) {
		this.jid = jid;
		this.name = name;
		this.subscription = subscription;
		this.group = group;
	}

	/**
	 * @return the jid
	 */
	public KixmppJid getJid() {
		return jid;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the subscription
	 */
	public PresenceSubscription getSubscription() {
		return subscription;
	}

	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}
}
