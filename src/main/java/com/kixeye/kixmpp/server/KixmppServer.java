package com.kixeye.kixmpp.server;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Striped;
import com.kixeye.kixmpp.*;
import com.kixeye.kixmpp.handler.KixmppEventEngine;
import com.kixeye.kixmpp.interceptor.KixmppStanzaInterceptor;
import com.kixeye.kixmpp.p2p.ClusterClient;
import com.kixeye.kixmpp.p2p.discovery.ConstNodeDiscovery;
import com.kixeye.kixmpp.p2p.discovery.NodeDiscovery;
import com.kixeye.kixmpp.p2p.listener.ClusterListener;
import com.kixeye.kixmpp.p2p.node.NodeId;
import com.kixeye.kixmpp.server.cluster.mapreduce.MapReduceTracker;
import com.kixeye.kixmpp.server.cluster.message.*;
import com.kixeye.kixmpp.server.module.KixmppServerModule;
import com.kixeye.kixmpp.server.module.auth.SaslKixmppServerModule;
import com.kixeye.kixmpp.server.module.bind.BindKixmppServerModule;
import com.kixeye.kixmpp.server.module.chat.ChatKixmppServerModule;
import com.kixeye.kixmpp.server.module.disco.DiscoKixmppServerModule;
import com.kixeye.kixmpp.server.module.features.FeaturesKixmppServerModule;
import com.kixeye.kixmpp.server.module.muc.*;
import com.kixeye.kixmpp.server.module.presence.PresenceKixmppServerModule;
import com.kixeye.kixmpp.server.module.roster.RosterKixmppServerModule;
import com.kixeye.kixmpp.server.module.session.SessionKixmppServerModule;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.fusesource.hawtdispatch.Task;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
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
 * A XMPP server.
 * 
 * @author ebahtijaragic
 */
public class KixmppServer implements AutoCloseable, ClusterListener {
	private static final Logger logger = LoggerFactory.getLogger(KixmppServer.class);
	
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	public static final InetSocketAddress DEFAULT_SOCKET_ADDRESS = new InetSocketAddress(5222);
	public static final InetSocketAddress DEFAULT_WEBSOCKET_ADDRESS = new InetSocketAddress(5290);
    public static final InetSocketAddress DEFAULT_CLUSTER_ADDRESS = new InetSocketAddress(8100);
    
    public static final int CUSTOM_MESSAGE_START = 16;

	private final InetSocketAddress bindAddress;
	private final String domain;

	private final ServerBootstrap bootstrap;

	private InetSocketAddress webSocketAddress;
	private ServerBootstrap webSocketBootstrap;
	
	private final KixmppEventEngine eventEngine;
	
	private final Set<String> modulesToRegister = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	private final ConcurrentHashMap<String, KixmppServerModule> modules = new ConcurrentHashMap<>();

	private final Set<KixmppStanzaInterceptor> interceptors = Collections.newSetFromMap(new ConcurrentHashMap<KixmppStanzaInterceptor, Boolean>());

	private final AtomicReference<ChannelFuture> channelFuture = new AtomicReference<>();
	private final AtomicReference<Channel> channel = new AtomicReference<>();

	private final AtomicReference<ChannelFuture> webSocketChannelFuture = new AtomicReference<>();
	private final AtomicReference<Channel> webSocketChannel = new AtomicReference<>();
	
	private AtomicReference<State> state = new AtomicReference<>(State.STOPPED);
	
	private final DefaultChannelGroup channels;
	private final ConcurrentHashMap<KixmppJid, Channel> jidChannel = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Set<Channel>> usernameChannel = new ConcurrentHashMap<>();
	private final Striped<Lock> usernameChannelStripes = Striped.lock(Runtime.getRuntime().availableProcessors() * 4);
	private MucRoomEventHandler mucRoomEventHandler = new DefaultMucRoomEventHandler();

    private static enum State {
		STARTING,
		STARTED,

		STOPPING,
		STOPPED
	}

    private final ClusterClient cluster;
    private final MapReduceTracker mapReduce;

    /**
	 * Creates a new {@link KixmppServer} with the given ssl engine.
	 *
	 * @param domain
	 */
	public KixmppServer(String domain) {
		this(DEFAULT_SOCKET_ADDRESS, domain, DEFAULT_CLUSTER_ADDRESS, new ConstNodeDiscovery() );
	}

	public MucRoomEventHandler getMucRoomEventHandler() {
		return mucRoomEventHandler;
	}

	public void setMucRoomEventHandler(MucRoomEventHandler mucRoomEventHandler) {
		this.mucRoomEventHandler = mucRoomEventHandler;
	}
	/**
	 * Creates a new {@link KixmppServer} with the given ssl engine.
	 * 
	 * @param bindAddress
	 * @param domain
	 */
	public KixmppServer(InetSocketAddress bindAddress, String domain) {
		this(bindAddress, domain, DEFAULT_CLUSTER_ADDRESS, new ConstNodeDiscovery());
	}

	public KixmppServer(int port, String domain) {
		this(new InetSocketAddress(port), domain);
	}

		/**
         * Creates a new {@link KixmppServer} with the given ssl engine.
         *
         * @param bindAddress
         * @param domain
         */
	public KixmppServer(InetSocketAddress bindAddress, String domain, InetSocketAddress clusterAddress, NodeDiscovery clusterDiscovery) {
		this(bindAddress, domain, clusterAddress, clusterDiscovery, true);
	}
	
	/**
	 * Creates a new {@link KixmppServer} with the given ssl engine.
	 * 
	 * @param bindAddress
	 * @param domain
	 */
	public KixmppServer(InetSocketAddress bindAddress, String domain, InetSocketAddress clusterAddress, NodeDiscovery clusterDiscovery, boolean useEpollIfAvailable) {
		if (useEpollIfAvailable && OS.indexOf("nux") >= 0) {
			this.bootstrap = new ServerBootstrap()
				.group(new EpollEventLoopGroup(), new EpollEventLoopGroup())
				.channel(EpollServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new KixmppCodec());
						ch.pipeline().addLast(new KixmppServerMessageHandler());
					}
				});
		} else {
			this.bootstrap = new ServerBootstrap()
				.group(new NioEventLoopGroup(), new NioEventLoopGroup())
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new KixmppCodec());
						ch.pipeline().addLast(new KixmppServerMessageHandler());
					}
				});
		}

        this.cluster = new ClusterClient( this, clusterAddress.getHostName(), clusterAddress.getPort(), clusterDiscovery, 300000, bootstrap.group() );
        this.cluster.getMessageRegistry().addCustomMessage(1, RoomBroadcastTask.class);
		this.cluster.getMessageRegistry().addCustomMessage(2, RoomPresenceBroadcastTask.class);
		this.cluster.getMessageRegistry().addCustomMessage(3, PrivateChatTask.class);
		this.cluster.getMessageRegistry().addCustomMessage(4, GetMucRoomNicknamesRequest.class);
		this.cluster.getMessageRegistry().addCustomMessage(5, GetMucRoomNicknamesResponse.class);
		this.mapReduce = new MapReduceTracker(this, bootstrap.group());
        this.channels = new DefaultChannelGroup("All Channels", GlobalEventExecutor.INSTANCE);

		this.bindAddress = bindAddress;
		this.domain = domain.toLowerCase();
		this.eventEngine = new KixmppEventEngine();

		this.modulesToRegister.add(FeaturesKixmppServerModule.class.getName());
		this.modulesToRegister.add(SaslKixmppServerModule.class.getName());
		this.modulesToRegister.add(BindKixmppServerModule.class.getName());
		this.modulesToRegister.add(SessionKixmppServerModule.class.getName());
		this.modulesToRegister.add(PresenceKixmppServerModule.class.getName());
		this.modulesToRegister.add(MucKixmppServerModule.class.getName());
		this.modulesToRegister.add(RosterKixmppServerModule.class.getName());
		this.modulesToRegister.add(DiscoKixmppServerModule.class.getName());
		this.modulesToRegister.add(ChatKixmppServerModule.class.getName());
	}
	
	/**
	 * Enables the WebSocket port.
	 */
	public KixmppServer enableWebSocket() {
		return enableWebSocket(DEFAULT_WEBSOCKET_ADDRESS);
	}
	
	/**
	 * Enables the WebSocket port.
	 * 
	 * @param webSocketAddress
	 */
	public KixmppServer enableWebSocket(InetSocketAddress webSocketAddress) {
		if (state.get() != State.STOPPED) {
			throw new IllegalStateException(String.format("The current state is [%s] but must be [STOPPED]", state.get()));
		}
		
		this.webSocketAddress = webSocketAddress;
		
		if (this.bootstrap.group() instanceof EpollEventLoopGroup && this.bootstrap.childGroup() instanceof EpollEventLoopGroup) {
			this.webSocketBootstrap = new ServerBootstrap()
					.group(this.bootstrap.group(), this.bootstrap.childGroup())
					.channel(EpollServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						protected void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast(new HttpServerCodec());
							ch.pipeline().addLast(new HttpObjectAggregator(65536));
							ch.pipeline().addLast(new WebSocketServerHandler());
							ch.pipeline().addLast(new KixmppWebSocketCodec());
							ch.pipeline().addLast(new KixmppServerMessageHandler());
						}
					});
		} else {
			this.webSocketBootstrap = new ServerBootstrap()
				.group(this.bootstrap.group(), this.bootstrap.childGroup())
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new HttpServerCodec());
						ch.pipeline().addLast(new HttpObjectAggregator(65536));
						ch.pipeline().addLast(new WebSocketServerHandler());
						ch.pipeline().addLast(new KixmppWebSocketCodec());
						ch.pipeline().addLast(new KixmppServerMessageHandler());
					}
				});
		}
		
		return this;
	}
	
	/**
	 * Starts the server.
	 * 
	 * @throws Exception
	 */
	public ListenableFuture<KixmppServer> start() throws Exception {
		checkAndSetState(State.STARTING, State.STOPPED);
		
		logger.info("Starting Kixmpp Server on [{}]...", bindAddress);

		// register all modules
		for (String moduleClassName : modulesToRegister) {
			installModule(moduleClassName);
		}
		
		final SettableFuture<KixmppServer> responseFuture = SettableFuture.create();
		
		final GenericFutureListener<Future<? super Void>> channelFutureListener = new GenericFutureListener<Future<? super Void>>() {
			@Override
			public synchronized void operationComplete(Future<? super Void> future) throws Exception {
				if (webSocketChannelFuture.get() != null && webSocketChannelFuture.get().isDone()) {
					if (webSocketChannelFuture.get().isSuccess()) {
						logger.info("Kixmpp WebSocket Server listening on [{}]", webSocketAddress);
						
						webSocketChannel.set(webSocketChannelFuture.get().channel());
						if (channelFuture.get() == null && !responseFuture.isDone()) {
							logger.info("Started Kixmpp Server");
							state.set(State.STARTED);
							responseFuture.set(KixmppServer.this);
						}
						webSocketChannelFuture.set(null);
					} else {
						logger.error("Unable to start Kixmpp WebSocket Server on [{}]", webSocketAddress, future.cause());

						if (channelFuture.get() == null && !responseFuture.isDone()) {
							state.set(State.STOPPED);
							responseFuture.setException(future.cause());
						}
						webSocketChannelFuture.set(null);
					}
				} else if (channelFuture.get() != null && channelFuture.get().isDone()) {
					if (channelFuture.get().isSuccess()) {
						logger.info("Kixmpp Server listening on [{}]", bindAddress);
						
						channel.set(channelFuture.get().channel());
						if (webSocketChannelFuture.get() == null && !responseFuture.isDone()) {
							logger.info("Started Kixmpp Server");
							state.set(State.STARTED);
							responseFuture.set(KixmppServer.this);
						}
						channelFuture.set(null);
					} else {
						logger.error("Unable to start Kixmpp Server on [{}]", bindAddress, future.cause());

						if (webSocketChannelFuture.get() == null && !responseFuture.isDone()) {
							state.set(State.STOPPED);
							responseFuture.setException(future.cause());
						}
						channelFuture.set(null);
					}
				}
			}
		};

		channelFuture.set(bootstrap.bind(bindAddress));

		channelFuture.get().addListener(channelFutureListener);
		
		if (webSocketAddress != null && webSocketBootstrap != null) {
			webSocketChannelFuture.set(webSocketBootstrap.bind(webSocketAddress));

			webSocketChannelFuture.get().addListener(channelFutureListener);
		}
		
		return responseFuture;
	}
	
	/**
	 * Stops the server.
	 * 
	 * @return
	 */
	public ListenableFuture<KixmppServer> stop() {
		checkAndSetState(State.STOPPING, State.STARTED, State.STARTING);

		logger.info("Stopping Kixmpp Server...");

        // shutdown clustering
        cluster.shutdown();

		for (Entry<String, KixmppServerModule> entry : modules.entrySet()) {
			entry.getValue().uninstall(this);
		}

		final SettableFuture<KixmppServer> responseFuture = SettableFuture.create();

		ChannelFuture serverChannelFuture = channelFuture.get();
		
		if (serverChannelFuture != null) {
			serverChannelFuture.cancel(true);
		}
		
		ChannelFuture webSocketServerChannelFuture = webSocketChannelFuture.get();
		
		if (webSocketServerChannelFuture != null) {
			webSocketServerChannelFuture.cancel(true);
		}
		
		final Channel serverChannel = channel.get();
		final Channel webSocketServerChannel = webSocketChannel.get();
		
		if (serverChannel == null && webSocketServerChannel == null) {
			logger.info("Stopped Kixmpp Server");
			
			state.set(State.STOPPED);

			responseFuture.set(KixmppServer.this);
		} else {
			final GenericFutureListener<Future<? super Void>> channelFutureListener = new GenericFutureListener<Future<? super Void>>() {
				public synchronized void operationComplete(Future<? super Void> future) throws Exception {
					if ((serverChannel != null && !serverChannel.isActive()) && 
						(webSocketServerChannel != null && !webSocketServerChannel.isActive())) {
						logger.info("Stopped Kixmpp Server");
						
						state.set(State.STOPPED);
						
						eventEngine.unregisterAll();
						
						responseFuture.set(KixmppServer.this);
					}
				}
			};
			
			if (serverChannel != null) {
				serverChannel.disconnect().addListener(channelFutureListener);
			}
			
			if (webSocketServerChannel != null) {
				webSocketServerChannel.disconnect().addListener(channelFutureListener);
			}
		}
		
		return responseFuture;
	}

	/**
	 * @see java.lang.AutoCloseable#close()
	 */
	public void close() throws Exception {
		stop();
	}
	
	/**
	 * Sets Netty {@link ChannelOption}s.
	 * 
	 * @param option
	 * @param value
	 * @return
	 */
    public <T> KixmppServer channelOption(ChannelOption<T> option, T value) {
    	bootstrap.option(option, value);
    	return this;
    }
    
    /**
	 * Sets Netty child {@link ChannelOption}s.
	 * 
	 * @param option
	 * @param value
	 * @return
	 */
    public <T> KixmppServer childChannelOption(ChannelOption<T> option, T value) {
    	bootstrap.childOption(option, value);
    	return this;
    }
    
    /**
     * @param moduleClass
     * @return true if module is installed
     */
    public boolean hasActiveModule(Class<?> moduleClass) {
    	return modules.containsKey(moduleClass.getName());
    }
    
    /**
     * Gets or installs a module.
     * 
     * @param moduleClass
     * @return
     */
    @SuppressWarnings("unchecked")
	public <T extends KixmppServerModule> T module(Class<T> moduleClass) {
    	T module = (T)modules.get(moduleClass.getName());
    	
    	if (module == null) {
    		module = (T)installModule(moduleClass.getName());
    	}

    	return module;
    }
    
    /**
     * Returns a collections of active modules.
     * 
     * @return
     */
    public Collection<KixmppServerModule> modules() {
    	return modules.values();
    }

    /**
     * Gets the event engine.
     * 
     * @return
     */
    public KixmppEventEngine getEventEngine() {
    	return eventEngine;
    }
    
    /**
	 * @return the bindAddress
	 */
	public InetSocketAddress getBindAddress() {
		return bindAddress;
	}
	
	/**
	 * @return the bindAddress
	 */
	public InetSocketAddress getWebSocketAddress() {
		return webSocketAddress;
	}

	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}

    /**
     * Adds a stanza interceptor.
     * 
     * @param interceptor
     */
    public boolean addInterceptor(KixmppStanzaInterceptor interceptor) {
    	return interceptors.add(interceptor);
    }
    
    /**
     * Removes a stanza interceptor.
     * 
     * @param interceptor
     */
    public boolean removeInterceptor(KixmppStanzaInterceptor interceptor) {
    	return interceptors.remove(interceptor);
    }
    
    /**
     * Gets the number of channels.
     * 
     * @return
     */
    public int getChannelCount() {
    	return channels.size();
    }
    
    /**
     * Gets a channel that is assigned to this JID.
     * 
     * @param jid
     * @return
     */
    public Channel getChannel(KixmppJid jid) {
    	return jidChannel.get(jid);
    }
    
    /**
     * Gets channel by username.
     * 
     * @param username
     * @return
     */
    @SuppressWarnings("unchecked")
	public Set<Channel> getChannels(String username) {
    	Set<Channel> channels = usernameChannel.get(username);
    	if (channels != null) {
    		return Collections.unmodifiableSet(channels);
    	} else {
    		return Collections.EMPTY_SET;
    	}
    }
    
    /**
     * Adds a channel mapping.
     * 
     * @param jid
     * @param channel
     */
    public void addChannelMapping(KixmppJid jid, Channel channel) {
    	jidChannel.put(jid, channel);
    	
    	Lock lock = usernameChannelStripes.get(jid.getNode());
    	
    	try {
    		lock.lock();
    		
    		Set<Channel> channels = usernameChannel.get(jid.getNode());
    		
    		if (channels == null) {
    			channels = new HashSet<>();
    		}
        	channels.add(channel);

        	usernameChannel.put(jid.getNode(), channels);
    	} finally {
    		lock.unlock();
    	}
    }
    
	/**
     * Tries to install module.
     * 
     * @param moduleClassName
     */
	private KixmppServerModule installModule(String moduleClassName) {
		KixmppServerModule module = null;
		
		try {
			module = (KixmppServerModule)Class.forName(moduleClassName).newInstance();
			module.install(this);
			
			modules.put(moduleClassName, module);
		} catch (Exception e) {
			logger.error("Error while installing module", e);
		}
		
		return module;
    }
    
    /**
     * Checks the state and sets it.
     * 
     * @param update
     * @param expectedStates
     * @throws IllegalStateException
     */
    private void checkAndSetState(State update, State... expectedStates) throws IllegalStateException {
    	if (expectedStates != null) {
    		boolean wasSet = false;
    		
    		for (State expectedState : expectedStates) {
    			if (state.compareAndSet(expectedState, update)) {
    				wasSet = true;
    				break;
    			}
    		}
    		
    		if (!wasSet) {
    			throw new IllegalStateException(String.format("The current state is [%s] but must be [%s]", state.get(), expectedStates));
    		}
    	} else {
    		if (!state.compareAndSet(null, update)) {
    			throw new IllegalStateException(String.format("The current state is [%s] but must be [null]", state.get()));
			}
    	}
    }

	/**
	 * Message handler for the {@link KixmppServer}
	 * 
	 * @author ebahtijaragic
	 */
	private final class KixmppServerMessageHandler extends ChannelDuplexHandler {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof Element) {
				Element stanza = (Element)msg;
				
				boolean rejected = false;
				
				for (KixmppStanzaInterceptor interceptor : interceptors) {
					try {
						interceptor.interceptIncoming(ctx.channel(),(Element)msg);
					} catch (KixmppStanzaRejectedException e) {
						rejected = true;
						
						logger.debug("Incoming stanza interceptor [{}] threw an rejected exception.", interceptor, e);
					} catch (Exception e) {
						logger.error("Incoming stanza interceptor [{}] threw an exception.", interceptor, e);
					}
				}
				
				if (!rejected) {
					eventEngine.publishStanza(ctx.channel(), stanza);
				}
			} else if (msg instanceof KixmppStreamStart) {
				KixmppStreamStart streamStart = (KixmppStreamStart)msg;

				eventEngine.publishStreamStart(ctx.channel(), streamStart);
			} else if (msg instanceof KixmppStreamEnd) {
				KixmppStreamEnd streamEnd = (KixmppStreamEnd)msg;

				eventEngine.publishStreamEnd(ctx.channel(), streamEnd);
			} else {
				logger.error("Unknown message type [{}] from Channel [{}]", msg, ctx.channel());
			}
		}
		
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			boolean rejected = false;
			
			if (msg instanceof Element) {
				for (KixmppStanzaInterceptor interceptor : interceptors) {
					try {
						interceptor.interceptOutgoing(ctx.channel(), (Element)msg);
					} catch (KixmppStanzaRejectedException e) {
						rejected = true;
						
						logger.debug("Outgoing stanza interceptor [{}] threw an rejected exception.", interceptor, e);
					} catch (Exception e) {
						logger.error("Outgoing stanza interceptor [{}] threw an exception.", interceptor, e);
					}
				}
			}
			
			if (!rejected) {
				super.write(ctx, msg, promise);
			}
		}
		
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			logger.debug("Channel [{}] connected.", ctx.channel());
			
			channels.add(ctx.channel());
			
			eventEngine.publishConnected(ctx.channel());
		}
		
		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			logger.debug("Channel [{}] disconnected.", ctx.channel());
			
			channels.remove(ctx.channel());

			KixmppJid jid = ctx.channel().attr(BindKixmppServerModule.JID).get();
			
			if (jid != null) {
				jidChannel.remove(jid);
				
				Lock lock = usernameChannelStripes.get(jid.getNode());
				
				try {
					lock.lock();
					usernameChannel.remove(jid.getNode());
				} finally {
					lock.unlock();
				}
			}
			
			eventEngine.publishDisconnected(ctx.channel());
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			logger.error("Unexpected exception.", cause);
			
			ctx.close();
		}
	}
	
	public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
	    private WebSocketServerHandshaker handshaker;

	    @Override
	    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
	        if (msg instanceof FullHttpRequest) {
	            handleHttpRequest(ctx, (FullHttpRequest) msg);
	        } else if (msg instanceof WebSocketFrame) {
	            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
	        }
	    }

	    @Override
	    public void channelReadComplete(ChannelHandlerContext ctx) {
	        ctx.flush();
	    }

	    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
	        // Handle a bad request.
	        if (!req.getDecoderResult().isSuccess()) {
	            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
	            return;
	        }

	        // Allow only GET methods.
	        if (req.getMethod() != HttpMethod.GET) {
	            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
	            return;
	        }

	        // Handshake
	        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), "xmpp", false);
	        handshaker = wsFactory.newHandshaker(req);
	        
	        if (handshaker == null) {
	            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
	        } else {
	            handshaker.handshake(ctx.channel(), req);
	        }
	    }

	    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
	        if (frame instanceof CloseWebSocketFrame) {
	            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
	        } else if (frame instanceof PingWebSocketFrame) {
	            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
	        } else {
                ctx.fireChannelRead(frame);
	        }
	    }

	    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
	        // Generate an error page if response getStatus code is not OK (200).
	        if (res.getStatus().code() != 200) {
	            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
	            res.content().writeBytes(buf);
	            buf.release();
	            HttpHeaders.setContentLength(res, res.content().readableBytes());
	        }

	        // Send the response and close the connection if necessary.
	        ChannelFuture f = ctx.channel().writeAndFlush(res);
	        if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
	            f.addListener(ChannelFutureListener.CLOSE);
	        }
	    }

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	        cause.printStackTrace();
	        ctx.close();
	    }

	    private String getWebSocketLocation(FullHttpRequest req) {
	        String location =  req.headers().get(HttpHeaders.Names.HOST);
	        
            return "ws://" + location;
	    }
	}
	
	public <T> Promise<T> createPromise() {
		return bootstrap.childGroup().next().newPromise();
	}
	
    public ClusterClient getCluster() {
        return cluster;
    }

    public void sendMapReduceRequest(MapReduceRequest request) {
        mapReduce.sendRequest(request);
    }

    public void registerCustomMessage(int id, Class<?> clazz) {
        cluster.getMessageRegistry().addCustomMessage(CUSTOM_MESSAGE_START + id, clazz);
    }

    @Override
    public void onNodeJoin(ClusterClient cluster, NodeId nodeId) {
        logger.info("Node {} joined cluster", nodeId.toString());
    }

    @Override
    public void onNodeLeft(ClusterClient cluster, NodeId nodeId) {
        logger.info("Node {} left cluster", nodeId.toString());
    }

    @Override
    public void onMessage(ClusterClient cluster, NodeId senderId, Object message) {

        // inject server reference
        if (message instanceof ClusterTask) {
            ((ClusterTask) message).setKixmppServer(this);
        }

        if (message instanceof MapReduceRequest) {
            MapReduceRequest request = (MapReduceRequest) message;
            request.setSenderId(senderId);
            getEventEngine().publishTask(request.getTargetJID(),request);
        } else if (message instanceof MapReduceResponse) {
            MapReduceResponse response = (MapReduceResponse) message;
            mapReduce.processResponse(response);
        } else  if (message instanceof RoomTask) {
            RoomTask roomTask = (RoomTask) message;
            module(MucKixmppServerModule.class).handleClusterTask(roomTask);
        } else if (message instanceof Task) {
            Task task = (Task) message;
            task.run();
        }
    }
}
