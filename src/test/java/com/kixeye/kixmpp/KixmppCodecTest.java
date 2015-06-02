package com.kixeye.kixmpp;

/*
 * #%L
 * KIXMPP Parent
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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link KixmppCodec}
 * 
 * @author ebahtijaragic
 */
public class KixmppCodecTest {
	@Test
	public void testSampleXmppClientSessionPerLine() throws Exception {
		final AtomicReference<KixmppStreamStart> start = new AtomicReference<>();
		final AtomicReference<KixmppStreamEnd> end = new AtomicReference<>();
		final ArrayList<Element> elements = new ArrayList<>();
		
		ChannelInboundHandlerAdapter handler = new ChannelInboundHandlerAdapter() {
			@Override
			public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
				if (msg instanceof KixmppStreamStart) {
					start.set((KixmppStreamStart)msg);
				} else if (msg instanceof KixmppStreamEnd) {
					end.set((KixmppStreamEnd)msg);
				} else if (msg instanceof Element) {
					elements.add((Element)msg);
				}
			}
		};
		
		EmbeddedChannel channel = new EmbeddedChannel(
				new KixmppCodec(),
				handler);
		
		// write a packer per line
		try (InputStream inputStream = this.getClass().getResourceAsStream("/sampleXmppClientSession.xml")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			
			String line = null;
			while ((line = reader.readLine()) != null) {
				channel.writeInbound(channel.alloc().buffer().writeBytes(line.getBytes(StandardCharsets.UTF_8)));
			}
		}
		
		Assert.assertNotNull(start.get());
		Assert.assertNotNull(end.get());
		
		Assert.assertEquals(5, elements.size());

		Assert.assertEquals("starttls", elements.get(0).getName());
		Assert.assertEquals("auth", elements.get(1).getName());
		Assert.assertEquals("iq", elements.get(2).getName());
		Assert.assertEquals("iq", elements.get(3).getName());
		Assert.assertEquals("iq", elements.get(4).getName());
	}
	
	@Test
	public void testSampleXmppClientSessionPer100Chars() throws Exception {
		final AtomicReference<KixmppStreamStart> start = new AtomicReference<>();
		final AtomicReference<KixmppStreamEnd> end = new AtomicReference<>();
		final ArrayList<Element> elements = new ArrayList<>();
		
		ChannelInboundHandlerAdapter handler = new ChannelInboundHandlerAdapter() {
			@Override
			public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
				if (msg instanceof KixmppStreamStart) {
					start.set((KixmppStreamStart)msg);
				} else if (msg instanceof KixmppStreamEnd) {
					end.set((KixmppStreamEnd)msg);
				} else if (msg instanceof Element) {
					elements.add((Element)msg);
				}
			}
		};
		
		EmbeddedChannel channel = new EmbeddedChannel(
				new KixmppCodec(),
				handler);
		
		// write a packer per line
		try (InputStream inputStream = this.getClass().getResourceAsStream("/sampleXmppClientSession.xml")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			
			int charsRead = 0;
			char[] chars = new char[100];
			while ((charsRead = reader.read(chars)) != -1) {
				StringWriter writer = new StringWriter();
				
				for (int i = 0; i < charsRead; i++) {
					writer.append(chars[i]);
				}
				
				channel.writeInbound(channel.alloc().buffer().writeBytes(writer.toString().getBytes(StandardCharsets.UTF_8)));
			}
		}
		
		Assert.assertNotNull(start.get());
		Assert.assertNotNull(end.get());
		
		Assert.assertEquals(5, elements.size());

		Assert.assertEquals("starttls", elements.get(0).getName());
		Assert.assertEquals("auth", elements.get(1).getName());
		Assert.assertEquals("iq", elements.get(2).getName());
		Assert.assertEquals("iq", elements.get(3).getName());
		Assert.assertEquals("iq", elements.get(4).getName());
	}
	
	@Test
	public void testSampleXmppServerSession() throws Exception {
		final AtomicReference<KixmppStreamStart> start = new AtomicReference<>();
		final AtomicReference<KixmppStreamEnd> end = new AtomicReference<>();
		final ArrayList<Element> elements = new ArrayList<>();
		
		ChannelInboundHandlerAdapter handler = new ChannelInboundHandlerAdapter() {
			@Override
			public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
				if (msg instanceof KixmppStreamStart) {
					start.set((KixmppStreamStart)msg);
				} else if (msg instanceof KixmppStreamEnd) {
					end.set((KixmppStreamEnd)msg);
				} else if (msg instanceof Element) {
					elements.add((Element)msg);
				}
			}
		};
		
		EmbeddedChannel channel = new EmbeddedChannel(
				new KixmppCodec(),
				handler);

		// write a packet per line
		try (InputStream inputStream = this.getClass().getResourceAsStream("/sampleXmppServerSession.xml")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			
			String line = null;
			while ((line = reader.readLine()) != null) {
				channel.writeInbound(channel.alloc().buffer().writeBytes(line.getBytes(StandardCharsets.UTF_8)));
			}
		}

		Assert.assertNotNull(start.get());
		Assert.assertNotNull(end.get());
		
		Assert.assertEquals(6, elements.size());

		Assert.assertEquals("features", elements.get(0).getName());
		Assert.assertEquals("proceed", elements.get(1).getName());
		Assert.assertEquals("success", elements.get(2).getName());
		Assert.assertEquals("iq", elements.get(3).getName());
		Assert.assertEquals("iq", elements.get(4).getName());
		Assert.assertEquals("iq", elements.get(5).getName());
	}
}
