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

import io.netty.channel.Channel;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.client.KixmppClient;
import com.kixeye.kixmpp.client.module.KixmppClientModule;
import com.kixeye.kixmpp.handler.KixmppStanzaHandler;

/**
 * A module for handling errors.
 * 
 * @author ebahtijaragic
 */
public class ErrorKixmppClientModule implements KixmppClientModule {
	private static final Logger logger = LoggerFactory.getLogger(ErrorKixmppClientModule.class);
	
	private Set<ErrorListener> errorListeners = Collections.newSetFromMap(new ConcurrentHashMap<ErrorListener, Boolean>());

	/**
	 * @param listener the listener to add
	 */
	public void addErrorListener(ErrorListener listener) {
		errorListeners.add(listener);
	}

	/**
	 * @param listener the listener to remove
	 */
	public void removeErrorListener(ErrorListener listener) {
		errorListeners.remove(listener);
	}
	
	@Override
	public void install(KixmppClient client) {
		client.getEventEngine().registerGlobalStanzaHandler(errorHandler);
	}

	@Override
	public void uninstall(KixmppClient client) {
		client.getEventEngine().registerGlobalStanzaHandler(errorHandler);
	}

	private KixmppStanzaHandler errorHandler = new KixmppStanzaHandler() {
		public void handle(Channel channel, Element stanza) {
			if ("error".equals(stanza.getAttributeValue("type"))) {
				KixmppJid by = null;
				String type = null;
				Integer code = null;
				Element conditionElement = null;
				
				Element errorElement = stanza.getChild("error", stanza.getNamespace());
				
				if (errorElement != null) {
					String byString = errorElement.getAttributeValue("by");
					if (byString != null) {
						by = KixmppJid.fromRawJid(byString);
					}
					
					type = errorElement.getAttributeValue("type");
					
					String codeString = errorElement.getAttributeValue("code");
					if (codeString != null) {
						code = Integer.parseInt(codeString);
					}
					
					if (!errorElement.getChildren().isEmpty()) {
						conditionElement = errorElement.getChildren().get(0);
					}
				}
				
				Error error = new Error(stanza, by, type, code, conditionElement);
				
				for (ErrorListener listener : errorListeners) {
					try {
						listener.handle(error);
					} catch (Exception e) {
						logger.error("Exception thrown while executing error listener", e);
					}
				}
			}
		}
	};
}
