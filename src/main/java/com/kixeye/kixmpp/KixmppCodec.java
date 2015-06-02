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


import com.fasterxml.aalto.AsyncInputFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.kixeye.kixmpp.jdom.StAXElementBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamConstants;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * An XMPP codec for the client.
 * 
 * @author ebahtijaragic
 */
public class KixmppCodec extends ByteToMessageCodec<Object> {
	private static final int STANZA_ELEMENT_DEPTH = 2;

	private static final Logger logger  = LoggerFactory.getLogger(KixmppCodec.class);

	private StAXElementBuilder elementBuilder = null;
	
	private InputFactoryImpl inputFactory = new InputFactoryImpl();
	private AsyncXMLStreamReader streamReader = inputFactory.createAsyncXMLStreamReader();
	private AsyncInputFeeder asyncInputFeeder = streamReader.getInputFeeder();
	
	public enum XMLStreamReaderConfiguration {
		SPEED,
		LOW_MEMORY_USAGE,
		ROUND_TRIPPING,
		CONVENIENCE,
		XML_CONFORMANCE
	}
	
	/**
	 * Creates a new codec and optimizes the parser for speed.
	 */
	public KixmppCodec() {
		this(XMLStreamReaderConfiguration.SPEED);
	}
	
	/**
	 * Creates a new codec and optimizes the parser based on the configuration flag.
	 * 
	 * @param configuration tells the codec how to optimize the XMLStreamReader
	 */
	public KixmppCodec(XMLStreamReaderConfiguration configuration) {
		switch (configuration) {
			case CONVENIENCE:
				inputFactory.configureForConvenience();
				break;
			case LOW_MEMORY_USAGE:
				inputFactory.configureForLowMemUsage();
				break;
			case ROUND_TRIPPING:
				inputFactory.configureForRoundTripping();
				break;
			case SPEED:
				inputFactory.configureForSpeed();
				break;
			case XML_CONFORMANCE:
				inputFactory.configureForXmlConformance();
				break;
		}
	}
	
	/**
	 * @see io.netty.handler.codec.ByteToMessageCodec#decode(io.netty.channel.ChannelHandlerContext, io.netty.buffer.ByteBuf, java.util.List)
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Received: [{}]", in.toString(StandardCharsets.UTF_8));
		}
		
		int retryCount = 0;
		Exception thrownException = null;
		
		// feed the data into the async xml input feeded
		byte[] data = new byte[in.readableBytes()];
		in.readBytes(data);
		
		if (streamReader != null) {
			while (retryCount < 2) {
				try {
					asyncInputFeeder.feedInput(data, 0, data.length);
					
					int event = -1;
					
					while (isValidEvent(event = streamReader.next())) {
						// handle stream start/end
						if (streamReader.getDepth() == STANZA_ELEMENT_DEPTH - 1) {
							if (event == XMLStreamConstants.END_ELEMENT) {
								out.add(new KixmppStreamEnd());

								asyncInputFeeder.endOfInput();
								streamReader.close();
								
								streamReader = null;
								asyncInputFeeder = null;
								
								break;
							} else if (event == XMLStreamConstants.START_ELEMENT) {
								StAXElementBuilder streamElementBuilder = new StAXElementBuilder(true);
								
								streamElementBuilder.process(streamReader);
			
								out.add(new KixmppStreamStart(null, true));
							}
						// only handle events that have element depth of 2 and above (everything under <stream:stream>..)
						} else if (streamReader.getDepth() >= STANZA_ELEMENT_DEPTH) {
							// if this is the beginning of the element and this is at stanza depth
							if (event == XMLStreamConstants.START_ELEMENT && streamReader.getDepth() == STANZA_ELEMENT_DEPTH) {
								elementBuilder = new StAXElementBuilder(true);
								elementBuilder.process(streamReader);
	
								// get the constructed element
								Element element = elementBuilder.getElement();
								
								if ("stream:stream".equals(element.getQualifiedName())) {
									throw new RuntimeException("Starting a new stream.");
								}
							// if this is the ending of the element and this is at stanza depth
						    } else if (event == XMLStreamConstants.END_ELEMENT && streamReader.getDepth() == STANZA_ELEMENT_DEPTH) {
								elementBuilder.process(streamReader);
	
								// get the constructed element
								Element element = elementBuilder.getElement();
								
					    		out.add(element);
					    
					    	// just process the event
						    } else {
								elementBuilder.process(streamReader);
						    }
						}
					}
					
					break;
				} catch (Exception e) {
					retryCount++;
					
					logger.info("Attempting to recover from impropper XML: " + e.getMessage());
					
					thrownException = e;
					
					try {
						streamReader.close();
					} finally {
						streamReader = inputFactory.createAsyncXMLStreamReader();
						asyncInputFeeder = streamReader.getInputFeeder();
					}
				}
			}
			
			if (retryCount > 1) {
				throw thrownException;
			}
		}
	}
	
	/**
	 * @param event the event id
	 * @return <b>true</b> if this event is not the end of a document event and it is not an event incomplete event
	 */
	private boolean isValidEvent(int event) {
		return (event != XMLStreamConstants.END_DOCUMENT && event != AsyncXMLStreamReader.EVENT_INCOMPLETE);
	}
	
	/**
	 * @see io.netty.handler.codec.ByteToMessageCodec#encode(io.netty.channel.ChannelHandlerContext, java.lang.Object, io.netty.buffer.ByteBuf)
	 */
	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		if (msg instanceof Element) {
			new XMLOutputter().output((Element)msg, new ByteBufOutputStream(out));
		} else if (msg instanceof KixmppStreamStart) {
			KixmppStreamStart streamStart = (KixmppStreamStart)msg;
			if (streamStart.doesIncludeXmlHeader()) {
				out.writeBytes("<?xml version='1.0' encoding='UTF-8'?>".getBytes(StandardCharsets.UTF_8));
			}
			out.writeBytes("<stream:stream ".getBytes(StandardCharsets.UTF_8));
			if (streamStart.getId() != null) {
				out.writeBytes(String.format("id=\"%s\" ", streamStart.getId()).getBytes(StandardCharsets.UTF_8));
			}
			if (streamStart.getFrom() != null) {
				out.writeBytes(String.format("from=\"%s\" ", streamStart.getFrom().getFullJid()).getBytes(StandardCharsets.UTF_8));
			}
			if (streamStart.getTo() != null) {
				out.writeBytes(String.format("to=\"%s\" ", streamStart.getTo().getFullJid()).getBytes(StandardCharsets.UTF_8));
			}
			out.writeBytes("version=\"1.0\" xmlns=\"jabber:client\" xmlns:stream=\"http://etherx.jabber.org/streams\">".getBytes(StandardCharsets.UTF_8));
		} else if (msg instanceof KixmppStreamEnd) {
			out.writeBytes("</stream:stream>".getBytes(StandardCharsets.UTF_8));
		} else if (msg instanceof String) {
			out.writeBytes(((String)msg).getBytes(StandardCharsets.UTF_8));
		} else if (msg instanceof ByteBuf) {
			ByteBuf buf = (ByteBuf)msg;
			
			out.writeBytes(buf, 0, buf.readableBytes());
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("Sending: [{}]", out.toString(StandardCharsets.UTF_8));
		}
	}
}