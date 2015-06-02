package com.kixeye.kixmpp.p2p.node;

/*
 * #%L
 * Zaqar
 * %%
 * Copyright (C) 2014 Charles Barry
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


import com.kixeye.kixmpp.p2p.message.MessageRegistry;
import com.kixeye.kixmpp.p2p.serialization.ProtostuffDecoder;
import com.kixeye.kixmpp.p2p.serialization.ProtostuffEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NodeServer {
    private static final Logger logger = LoggerFactory.getLogger(NodeServer.class);

    private Channel acceptChannel;

    public void initialize(
            final String host, final int port,
            final EventLoopGroup bossGroup,
            final EventLoopGroup workerGroup,
            final MessageRegistry messageRegistry,
            final ChannelInboundHandlerAdapter channelListener) {
        ServerBootstrap boot = new ServerBootstrap();
        boot.group(bossGroup,workerGroup);
        boot.channel(NioServerSocketChannel.class);
        boot.option(ChannelOption.SO_BACKLOG, 32);
        boot.childOption(ChannelOption.SO_KEEPALIVE, true);
        boot.childOption(ChannelOption.TCP_NODELAY,true);
        boot.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();

                //p.addLast(new LoggingHandler());

                // encoders
                p.addLast(new LengthFieldPrepender(4));
                p.addLast(new ProtostuffEncoder(messageRegistry));

                // decoders
                p.addLast(new LengthFieldBasedFrameDecoder(0x100000,0,4,0,4));
                p.addLast(new ProtostuffDecoder(messageRegistry));
                p.addLast(channelListener);
            }
        });

        // start accepting connection
        try {
    		logger.info("Starting NodeServer on [{}]...", port);
    		
            if (host == null) {
                acceptChannel = boot.bind(port).sync().channel();
            } else {
                acceptChannel = boot.bind(host, port).sync().channel();
            }

    		logger.info("NodeServer listening on [{}]...", port);
        } catch (InterruptedException e) {
            logger.error("Binding to port {} failed", port, e);
        }

    }

    public void shutdown() {
        try {
            if (acceptChannel != null) {
                acceptChannel.close().sync();
            }
        } catch (Exception ex) {
            logger.error("Exception shutting down NodeServer", ex);
        }
    }
}
