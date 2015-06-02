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

/**
 * An exception that gets thrown when a stanza is rejected.
 * 
 * @author ebahtijaragic
 */
public class KixmppStanzaRejectedException extends KixmppException {
	private static final long serialVersionUID = -2495670401789500578L;

	/**
	 * A rejection exception with a reason.
	 * 
	 * @param reason
	 */
	public KixmppStanzaRejectedException(String reason) {
		super(reason);
	}
	
	/**
	 * A rejection exception with a reason and a cause.
	 * 
	 * @param reason
	 * @param cause
	 */
	public KixmppStanzaRejectedException(String reason, Throwable cause) {
		super(reason, cause);
	}
}
