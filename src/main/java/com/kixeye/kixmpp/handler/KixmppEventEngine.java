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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.jdom2.Element;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.KixmppStreamEnd;
import com.kixeye.kixmpp.KixmppStreamStart;
import com.kixeye.kixmpp.tuple.Tuple;

/**
 * An event engine that uses a {@link DispatchQueue}.
 * 
 * @author ebahtijaragic
 */
public class KixmppEventEngine {
	private static final String HANDLER_WILDCARD = "*";
	
	private final ConcurrentHashMap<Tuple, Set<KixmppStanzaHandler>> stanzaHandlers = new ConcurrentHashMap<>();
	private final Set<KixmppConnectionHandler> connectionHandlers = Collections.newSetFromMap(new ConcurrentHashMap<KixmppConnectionHandler, Boolean>());
	private final Set<KixmppStreamHandler> streamHandlers = Collections.newSetFromMap(new ConcurrentHashMap<KixmppStreamHandler, Boolean>());
	private final Set<KixmppSessionHandler> sessionHandlers = Collections.newSetFromMap(new ConcurrentHashMap<KixmppSessionHandler, Boolean>());

	private final LoadingCache<String, DispatchQueue> queues = CacheBuilder.newBuilder()
			.expireAfterAccess(30, TimeUnit.SECONDS)
			.build(new CacheLoader<String, DispatchQueue>() {
				public DispatchQueue load(String key) throws Exception {
					return Dispatch.createQueue(key);
				}
			});
	
	/**
	 * Publishes a stanza.
	 * 
	 * @param channel
	 * @param stanza
	 */
	public void publishStanza(Channel channel, Element stanza) {
		String to = stanza.getAttributeValue("to");
		
		DispatchQueue queue;
		
		try {
			if (to != null) {
				queue = queues.get("address:" + to);
			} else {
				queue = queues.get("channel:" + channel.hashCode());
			}
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		if (to != null) {
			Set<KixmppStanzaHandler> recipientHandlers = stanzaHandlers.get(Tuple.from(stanza.getQualifiedName(), KixmppJid.fromRawJid(to)));
			
			if (recipientHandlers != null) {
				for (KixmppStanzaHandler handler : recipientHandlers) {
					queue.execute(new ExecuteStanzaHandler(handler, channel, stanza));
				}
			}
			
			recipientHandlers = stanzaHandlers.get(Tuple.from(HANDLER_WILDCARD, KixmppJid.fromRawJid(to)));
			
			if (recipientHandlers != null) {
				for (KixmppStanzaHandler handler : recipientHandlers) {
					queue.execute(new ExecuteStanzaHandler(handler, channel, stanza));
				}
			}
		}
		
		Set<KixmppStanzaHandler> globalHandlers = stanzaHandlers.get(Tuple.from(stanza.getQualifiedName()));
		
		if (globalHandlers != null) {
			for (KixmppStanzaHandler handler : globalHandlers) {
				queue.execute(new ExecuteStanzaHandler(handler, channel, stanza));
			}
		}
		
		globalHandlers = stanzaHandlers.get(Tuple.from(HANDLER_WILDCARD));
		
		if (globalHandlers != null) {
			for (KixmppStanzaHandler handler : globalHandlers) {
				queue.execute(new ExecuteStanzaHandler(handler, channel, stanza));
			}
		}
	}
	
	/**
	 * Published an arbitrary task for serial execution.
	 * 
	 * @param jid
	 * @param task
	 */
	public void publishTask(KixmppJid jid, Task task) {
		DispatchQueue queue;
		
		try {
            if (jid != null) {
                queue = queues.get("address:" + jid.getFullJid());
            } else {
                queue = queues.get("default");
            }
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		queue.execute(task);
	}
	
	/**
	 * Published an arbitrary task for serial execution.
	 * 
	 * @param channel
	 * @param task
	 */
	public void publishTask(Channel channel, Task task) {
		DispatchQueue queue;
		
		try {
			queue = queues.get("channel:" + channel.hashCode());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}

		queue.execute(task);
	}
	
	/**
	 * Publishes that a channel has connected.
	 * 
	 * @param channel
	 */
	public void publishConnected(Channel channel) {
		DispatchQueue queue;
		
		try {
			queue = queues.get("channel:" + channel.hashCode());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		for (KixmppConnectionHandler handler : connectionHandlers) {
			queue.execute(new ExecuteConnectionConnectedHandler(handler, channel));
		}
	}
	
	/**
	 * Publishes that a channel has disconnected.
	 * 
	 * @param channel
	 */
	public void publishDisconnected(Channel channel) {
		DispatchQueue queue;
		
		try {
			queue = queues.get("channel:" + channel.hashCode());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		for (KixmppConnectionHandler handler : connectionHandlers) {
			queue.execute(new ExecuteConnectionDisconnectedHandler(handler, channel));
		}
	}
	
	/**
	 * Publishes a stream start event.
	 * 
	 * @param channel
	 * @param streamStart
	 */
	public void publishStreamStart(Channel channel, KixmppStreamStart streamStart) {
		DispatchQueue queue;
		
		try {
			queue = queues.get("channel:" + channel.hashCode());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		for (KixmppStreamHandler handler : streamHandlers) {
			queue.execute(new ExecuteStreamStartHandler(handler, channel, streamStart));
		}
	}
	
	/**
	 * Publishes a stream end event.
	 * 
	 * @param channel
	 * @param streamEnd
	 */
	public void publishStreamEnd(Channel channel, KixmppStreamEnd streamEnd) {
		DispatchQueue queue;
		
		try {
			queue = queues.get("channel:" + channel.hashCode());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		for (KixmppStreamHandler handler : streamHandlers) {
			queue.execute(new ExecuteStreamEndHandler(handler, channel, streamEnd));
		}
	}

	/**
	 * Publishes a session start event.
	 *
	 * @param channel
	 */
	public void publishSessionStart(Channel channel) {
		DispatchQueue queue;

		try {
			queue = queues.get("channel:" + channel.hashCode());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}

		for (KixmppSessionHandler handler : sessionHandlers) {
			queue.execute(new ExecuteSessionStartHandler(handler, channel));
		}
	}

	/**
	 * Registers a handler to listen to connection events.
	 * 
	 * @param handler
	 */
	public void registerConnectionHandler(KixmppConnectionHandler handler) {
		connectionHandlers.add(handler);
	}

	/**
	 * Unregisters a connection handler.
	 * 
	 * @param handler
	 */
	public void unregisterConnectionHandler(KixmppConnectionHandler handler) {
		connectionHandlers.remove(handler);
	}

	/**
	 * Registers a stream handler.
	 *
	 * @param handler
	 */
	public void registerStreamHandler(KixmppStreamHandler handler) {
		streamHandlers.add(handler);
	}

	/**
	 * Unregisters a stream handler.
	 *
	 * @param handler
	 */
	public void unregisterStreamHandler(KixmppStreamHandler handler) {
		streamHandlers.remove(handler);
	}

	/**
	 * Registers a session handler.
	 *
	 * @param handler
	 */
	public void registerSessionHandler(KixmppSessionHandler handler) {
		sessionHandlers.add(handler);
	}

	/**
	 * Unregisters a session handler.
	 *
	 * @param handler
	 */
	public void unregisterSessionHandler(KixmppSessionHandler handler) {
		sessionHandlers.remove(handler);
	}

	/**
	 * Registers a stanza handler.
	 * 
	 * @param qualifiedName
	 * @param jid
	 * @param handler
	 */
	public void registerStanzaHandler(KixmppJid jid, String qualifiedName, KixmppStanzaHandler handler) {
		Tuple key = Tuple.from(qualifiedName, jid);
		
		Set<KixmppStanzaHandler> handlers = stanzaHandlers.get(key);
		
		if (handlers == null) {
			Set<KixmppStanzaHandler> newHandlers = Collections.newSetFromMap(new ConcurrentHashMap<KixmppStanzaHandler, Boolean>());
			
			handlers = stanzaHandlers.putIfAbsent(key, newHandlers);
			
			if (handlers == null) {
				handlers = newHandlers;
			}
        }
		
		handlers.add(handler);
	}
	
	/**
	 * Registers a stanza handler.
	 * 
	 * @param jid
	 * @param handler
	 */
	public void registerStanzaHandler(KixmppJid jid, KixmppStanzaHandler handler) {
		registerStanzaHandler(jid, HANDLER_WILDCARD, handler);
	}
	
	/**
	 * Registers a stanza handler.
	 * 
	 * @param handler
	 */
	public void registerGlobalStanzaHandler(String qualifiedName, KixmppStanzaHandler handler) {
		Tuple key = Tuple.from(qualifiedName);
		
		Set<KixmppStanzaHandler> handlers = stanzaHandlers.get(key);
		
		if (handlers == null) {
			Set<KixmppStanzaHandler> newHandlers = Collections.newSetFromMap(new ConcurrentHashMap<KixmppStanzaHandler, Boolean>());
			
			handlers = stanzaHandlers.putIfAbsent(key, newHandlers);
			
			if (handlers == null) {
				handlers = newHandlers;
			}
        }
		
		handlers.add(handler);
	}
	
	/**
	 * Registers a stanza handler.
	 * 
	 * @param handler
	 */
	public void registerGlobalStanzaHandler(KixmppStanzaHandler handler) {
		registerGlobalStanzaHandler(HANDLER_WILDCARD, handler);
	}

	/**
	 * Unregisters a stanza handler.
	 * 
	 * @param jid
	 * @param qualifiedName
	 * @param handler
	 */
	public void unregisterStanzaHandler(KixmppJid jid, String qualifiedName, KixmppStanzaHandler handler) {
		Set<KixmppStanzaHandler> handlers = stanzaHandlers.get(Tuple.from(qualifiedName, jid));
		
		if (handlers != null) {
			handlers.remove(handler);
		}
	}
	
	/**
	 * Unregisters a stanza handler.
	 * 
	 * @param jid
	 * @param handler
	 */
	public void unregisterStanzaHandler(KixmppJid jid, KixmppStanzaHandler handler) {
		unregisterStanzaHandler(jid, HANDLER_WILDCARD, handler);
	}
	
	/**
	 * Unregisters a stanza handler.
	 * 
	 * @param qualifiedName
	 * @param handler
	 */
	public void unregisterGlobalStanzaHandler(String qualifiedName, KixmppStanzaHandler handler) {
		Set<KixmppStanzaHandler> handlers = stanzaHandlers.get(Tuple.from(qualifiedName));
		
		if (handlers != null) {
			handlers.remove(handler);
		}
	}
	
	/**
	 * Unregisters a stanza handler.
	 * 
	 * @param handler
	 */
	public void unregisterGlobalStanzaHandler(KixmppStanzaHandler handler) {
		unregisterGlobalStanzaHandler(HANDLER_WILDCARD, handler);
	}
	
	/**
	 * Unregisters all the handlers.
	 */
	public void unregisterAll() {
		stanzaHandlers.clear();
		connectionHandlers.clear();
		streamHandlers.clear();
		sessionHandlers.clear();
	}
	
	private static class ExecuteStanzaHandler extends Task {
		private final KixmppStanzaHandler handler;
		private final Channel channel;
		private final Element stanza;
		
		public ExecuteStanzaHandler(KixmppStanzaHandler handler, Channel channel, Element stanza) {
			this.handler = handler;
			this.channel = channel;
			this.stanza = stanza;
		}

		public void run() {
			handler.handle(channel, stanza);
		}
	}
	
	private static class ExecuteConnectionConnectedHandler extends Task {
		private final KixmppConnectionHandler handler;
		private final Channel channel;
		
		public ExecuteConnectionConnectedHandler(KixmppConnectionHandler handler, Channel channel) {
			this.handler = handler;
			this.channel = channel;
		}

		public void run() {
			handler.handleConnected(channel);
		}
	}
	
	private static class ExecuteConnectionDisconnectedHandler extends Task {
		private final KixmppConnectionHandler handler;
		private final Channel channel;
		
		public ExecuteConnectionDisconnectedHandler(KixmppConnectionHandler handler, Channel channel) {
			this.handler = handler;
			this.channel = channel;
		}

		public void run() {
			handler.handleDisconnected(channel);
		}
	}
	
	private static class ExecuteStreamStartHandler extends Task {
		private final KixmppStreamHandler handler;
		private final Channel channel;
		private final KixmppStreamStart start;
		
		public ExecuteStreamStartHandler(KixmppStreamHandler handler, Channel channel, KixmppStreamStart start) {
			this.handler = handler;
			this.channel = channel;
			this.start = start;
		}

		public void run() {
			handler.handleStreamStart(channel, start);
		}
	}
	
	private static class ExecuteStreamEndHandler extends Task {
		private final KixmppStreamHandler handler;
		private final Channel channel;
		private final KixmppStreamEnd end;
		
		public ExecuteStreamEndHandler(KixmppStreamHandler handler, Channel channel, KixmppStreamEnd end) {
			this.handler = handler;
			this.channel = channel;
			this.end = end;
		}

		public void run() {
			handler.handleStreamEnd(channel, end);
		}
	}

	private static class ExecuteSessionStartHandler extends Task {
		private final KixmppSessionHandler handler;
		private final Channel channel;

		public ExecuteSessionStartHandler(KixmppSessionHandler handler, Channel channel) {
			this.handler = handler;
			this.channel = channel;
		}

		public void run() {
			handler.handleSessionStart(channel);
		}
	}
}
