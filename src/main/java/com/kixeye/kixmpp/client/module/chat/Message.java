package com.kixeye.kixmpp.client.module.chat;

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
 * Presence information.
 * 
 * @author ebahtijaragic
 */
public class Message {
	private final KixmppJid from;
	private final KixmppJid to;
	private final String body;

	/**
	 * @param from
	 * @param to
	 * @param body
	 */
	public Message(KixmppJid from, KixmppJid to, String body) {
		this.from = from;
		this.to = to;
		this.body = body;
	}
	
	/**
	 * Default constructor.
	 */
	public Message() {
		this.from = null;
		this.to = null;
		this.body = null;
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
	 * @return the body
	 */
	public String getBody() {
		return body;
	}
}
