package com.kixeye.kixmpp.client.module.presence;

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
public class Presence {
	private final KixmppJid from;
	private final KixmppJid to;
	private final String type;
	private final String status;
	private final String show;

	/**
	 * @param from
	 * @param to
	 * @param type
	 * @param status
	 * @param show
	 */
	public Presence(KixmppJid from, KixmppJid to, String type, String status,
			String show) {
		this.from = from;
		this.to = to;
		this.type = type;
		this.status = status;
		this.show = show;
	}
	
	/**
	 * @param type
	 * @param status
	 * @param show
	 */
	public Presence(String type, String status, String show) {
		this.from = null;
		this.to = null;
		this.type = type;
		this.status = status;
		this.show = show;
	}
	
	/**
	 * Default constructor.
	 */
	public Presence() {
		this.from = null;
		this.to = null;
		this.type = null;
		this.status = null;
		this.show = null;
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
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @return the show
	 */
	public String getShow() {
		return show;
	}
}
