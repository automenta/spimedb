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

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.client.module.presence.Presence;

/**
 * Represents a MUC presence for a user.
 * 
 * @author ebahtijaragic
 */
public class MucPresence extends Presence {
	private final KixmppJid userJid;
	private final String affiliation;
	private final String role;

	/**
	 * @param from
	 * @param to
	 * @param type
	 * @param status
	 * @param show
	 * @param userJid
	 * @param affiliation
	 * @param role
	 */
	public MucPresence(KixmppJid from, KixmppJid to, String type, String status,
			String show, KixmppJid userJid, String affiliation, String role) {
		super(from, to, type, status, show);
		this.userJid = userJid;
		this.affiliation = affiliation;
		this.role = role;
	}

	/**
	 * @param from
	 * @param to
	 * @param type
	 * @param userJid
	 * @param affiliation
	 * @param role
	 */
	public MucPresence(KixmppJid from, KixmppJid to, String type, KixmppJid userJid,
			String affiliation, String role) {
		super(from, to, type, null, null);
		this.userJid = userJid;
		this.affiliation = affiliation;
		this.role = role;
	}

	/**
	 * @return the userJid
	 */
	public KixmppJid getUserJid() {
		return userJid;
	}

	/**
	 * @return the affiliation
	 */
	public String getAffiliation() {
		return affiliation;
	}

	/**
	 * @return the role
	 */
	public String getRole() {
		return role;
	}
}
