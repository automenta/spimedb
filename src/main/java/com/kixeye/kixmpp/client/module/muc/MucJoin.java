package com.kixeye.kixmpp.client.module.muc;

import com.kixeye.kixmpp.KixmppJid;

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

/**
 * A message about the user joining the room.
 * 
 * @author ebahtijaragic
 */
public class MucJoin {
	private final KixmppJid roomJid;
	private final KixmppJid userJid;
	private final String affiliation;
	private final String role;

	/**
	 * @param roomJid
	 * @param userJid
	 * @param affiliation
	 * @param role
	 */
	public MucJoin(KixmppJid roomJid, KixmppJid userJid, String affiliation, String role) {
		this.roomJid = roomJid;
		this.userJid = userJid;
		this.affiliation = affiliation;
		this.role = role;
	}

	/**
	 * @return the roomJid
	 */
	public KixmppJid getRoomJid() {
		return roomJid;
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
