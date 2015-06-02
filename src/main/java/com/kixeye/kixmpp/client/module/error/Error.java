package com.kixeye.kixmpp.client.module.error;

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

import com.kixeye.kixmpp.KixmppJid;

/**
 * Defines a reusable error.
 * 
 * @author ebahtijaragic
 */
public class Error {
	private final Element rootElement;
	
	private final KixmppJid by;
	
	private final String type;
	private final Integer code;
	
	private final Element conditionElement;

	/**
	 * @param rootElement
	 * @param by
	 * @param type
	 * @param code
	 * @param conditionElement
	 */
	public Error(Element rootElement, KixmppJid by, String type, Integer code, Element conditionElement) {
		this.rootElement = rootElement;
		this.by = by;
		this.type = type;
		this.code = code;
		this.conditionElement = conditionElement;
	}

	/**
	 * @return the rootElement
	 */
	public Element getRootElement() {
		return rootElement;
	}

	/**
	 * @return the by
	 */
	public KixmppJid getBy() {
		return by;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the code
	 */
	public Integer getCode() {
		return code;
	}

	/**
	 * @return the conditionElement
	 */
	public Element getConditionElement() {
		return conditionElement;
	}
}
