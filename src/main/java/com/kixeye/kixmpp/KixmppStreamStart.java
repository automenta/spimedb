package com.kixeye.kixmpp;

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


import org.jdom2.Element;

/**
 * An event indicating a stream started.
 * 
 * @author ebahtijaragic
 */
public class KixmppStreamStart {
	private final boolean includeXmlHeader;
	private final KixmppJid from;
	private final KixmppJid to;
	private final String id;
	
	/**
	 * @param includeXmlHeader
	 * @param from
	 * @param to
	 */
	public KixmppStreamStart(KixmppJid from, KixmppJid to, boolean includeXmlHeader, String id) {
		this.from = from;
		this.to = to;
		this.includeXmlHeader = includeXmlHeader;
		this.id = id;
	}
	
	/**
	 * @param includeXmlHeader
	 * @param from
	 * @param to
	 */
	public KixmppStreamStart(KixmppJid from, KixmppJid to, boolean includeXmlHeader) {
		this(from, to, includeXmlHeader, null);
	}

	/**
	 * Creates a stream start.
	 * 
	 * @param element
	 */
	public KixmppStreamStart(Element element, boolean includeXmlHeader) {
		this.includeXmlHeader = includeXmlHeader;
		
		if (element != null) {
			String from = element.getAttributeValue("from");
			String to = element.getAttributeValue("to");
			
			if (from != null) {
				this.from = KixmppJid.fromRawJid(from);
			} else {
				this.from = null;
			}
			
			if (to != null) {
				this.to = KixmppJid.fromRawJid(to);
			} else {
				this.to = null;
			}
			
			this.id = element.getAttributeValue("id");
		} else {
			this.from = null;
			this.to = null;
			this.id = null;
		}
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
	 * @return the includeXmlHeader
	 */
	public boolean doesIncludeXmlHeader() {
		return includeXmlHeader;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
}
