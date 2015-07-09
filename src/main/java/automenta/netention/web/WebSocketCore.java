package automenta.netention.web;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import automenta.netention.Core;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.undertow.server.HttpHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

import static io.undertow.Handlers.websocket;

/**
 * Manages websocket i/o to a channel
 */
abstract public class WebSocketCore extends AbstractReceiveListener implements WebSocketCallback<Void>, WebSocketConnectionCallback {

    public static final Logger log = LoggerFactory.getLogger(WebSocketCore.class);
    private final boolean attemptJSONParseOfText;


    public WebSocketCore(boolean attemptJSONParseOfText) {
        super();
        this.attemptJSONParseOfText = attemptJSONParseOfText;
    }


    public HttpHandler get() {
        return websocket(this).addExtension(new PerMessageDeflateHandshake());
    }


    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {

        if (log.isInfoEnabled())
            log.info(socket.getPeerAddress() + " connected websocket");

        socket.getReceiveSetter().set(this);
        socket.resumeReceives();
    }

    @Override
    protected void onClose(WebSocketChannel socket, StreamSourceFrameChannel channel) throws IOException {

        if (log.isInfoEnabled())
            log.info(socket.getPeerAddress() + " disconnected websocket");
    }


    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) throws IOException {

        if (attemptJSONParseOfText) {
            try {
                //System.out.println(socket + " recv txt: " + message.getData());
                JsonNode j = Core.json.readValue(message.getData(), JsonNode.class);
                onJSONMessage(socket, j);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void onJSONMessage(WebSocketChannel socket, JsonNode j) {

    }

//        public final EventObserver channelObserver = new EventObserver() {
//
//            @Override
//            public void event(Class event, Object[] args) {
//                if (event == ChannelChange.class) {
//                    Channel c = (Channel)args[0];
//                    JsonNode patch = null;
//                    if (args.length > 1)
//                        patch = (JsonNode)args[1];
//
//                    if (patch == null) {
//                        //send entire object
//                        sendChannel(c);
//                    }
//                    else {
//                        sendPatch(c.id, patch);
//                    }
//                }
//            }
//
//        };

//
//        protected void sendChannel(Channel c) {
//            ArrayNode a = Core.newJson.arrayNode();
//            a.add("=");
//            a.add(c.get());
//            send(socket, a);
//        }
//        protected void sendPatch(String channelID, JsonNode patch) {
//            ArrayNode a = Core.newJson.arrayNode();
//            a.add("+");
//            a.add(channelID);
//            a.add(patch);
//            send(socket, a);
//        }
//


    @Override
    protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {

        //System.out.println(channel + " recv bin: " + message.getData());
    }



    public void send(WebSocketChannel socket, Object object) {
        try {
            ByteBuffer data = ByteBuffer.wrap(Core.json.writeValueAsBytes(object));
            //System.out.println("Sending: " + data);
            WebSockets.sendText(data, socket, this);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }
    }

//    @Override
//    public void complete(WebSocketChannel wsc, Void t) {
//        //System.out.println("Sent: " + wsc);
//    }

    @Override
    public void onError(WebSocketChannel wsc, Void t, Throwable thrwbl) {
        //System.out.println("Error: " + thrwbl);
        log.error(thrwbl.toString());
    }

    @Override
    public void complete(WebSocketChannel channel, Void context) {
        log.info("Complete: " + channel);
    }
}
