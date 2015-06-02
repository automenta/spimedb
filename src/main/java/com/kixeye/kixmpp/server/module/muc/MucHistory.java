package com.kixeye.kixmpp.server.module.muc;

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

/**
 * Holds Muc history.
 * 
 * @author ebahtijaragic
 */
public class MucHistory implements Comparable<MucHistory> {
	private final KixmppJid from;
	private final KixmppJid to;
	private final String nickname;
	private final String body;
	private final long timestamp;

	/**
	 * @param from
	 * @param to
	 * @param nickname
	 * @param body
	 * @param timestamp
	 */
	public MucHistory(KixmppJid from, KixmppJid to, String nickname,
			String body, long timestamp) {
		this.from = from;
		this.to = to;
		this.nickname = nickname;
		this.body = body;
		this.timestamp = timestamp;
	}

	/**
	 * @return the from
	 */
	public KixmppJid getFrom() {
		return from;
	}

	/**
	 * @return the to
	 */
	public KixmppJid getTo() {
		return to;
	}

	/**
	 * @return the nickname
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * @return the body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public int compareTo(MucHistory otherMucHistory) {
		Long timeStamp1 = this.getTimestamp();
		Long timeStamp2 = otherMucHistory.getTimestamp();
		return timeStamp1.compareTo(timeStamp2);
	}
}
