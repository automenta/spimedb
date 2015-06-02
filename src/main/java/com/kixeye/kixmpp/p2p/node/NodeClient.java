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
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeClient {
    private static final Logger logger = LoggerFactory.getLogger(NodeClient.class);

    private Channel channel;

    public void initialize(
            final String host, int port,
            final EventLoopGroup workerGroup,
            final MessageRegistry messageRegistry,
            final ChannelInboundHandlerAdapter channelListener) throws InterruptedException {
        // prepare connection
        Bootstrap boot = new Bootstrap();
        boot.group(workerGroup);
        boot.channel(NioSocketChannel.class);
        boot.option(ChannelOption.SO_KEEPALIVE,true);
        boot.option(ChannelOption.TCP_NODELAY,true);
        boot.handler( new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
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

        // connect
        channel = boot.connect(host,port).sync().channel();
    }

    public void send(Object msg) {
        channel.writeAndFlush(msg);
    }

    public void shutdown() {
        try {
            channel.close().sync();
        } catch (InterruptedException e) {
            logger.error("Exception shutting down", e);
        }
    }
}
