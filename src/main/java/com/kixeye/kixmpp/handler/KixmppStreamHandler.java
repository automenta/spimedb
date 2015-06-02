package com.kixeye.kixmpp.handler;

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

import com.kixeye.kixmpp.KixmppStreamEnd;
import com.kixeye.kixmpp.KixmppStreamStart;

/**
 * Handles streams.
 * 
 * @author ebahtijaragic
 */
public interface KixmppStreamHandler {
	/**
	 * Handles a stream start.
	 * 
	 * @param channel
	 * @param streamStart
	 */
	public void handleStreamStart(Channel channel, KixmppStreamStart streamStart);

	/**
	 * Handles a stream end.
	 * 
	 * @param channel
	 * @param streamEnd
	 */
	public void handleStreamEnd(Channel channel, KixmppStreamEnd streamEnd);
}
