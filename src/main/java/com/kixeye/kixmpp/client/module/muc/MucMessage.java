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
 * A MUC message.
 * 
 * @author ebahtijaragic
 */
public class MucMessage {
	private final KixmppJid from;
	private final KixmppJid to;
	private final String language;
	private final String body;

	/**
	 * @param from
	 * @param to
	 * @param language
	 * @param body
	 */
	public MucMessage(KixmppJid from, KixmppJid to, String language, String body) {
		this.from = from;
		this.to = to;
		this.language = language;
		this.body = body;
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
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @return the body
	 */
	public String getBody() {
		return body;
	}
}
