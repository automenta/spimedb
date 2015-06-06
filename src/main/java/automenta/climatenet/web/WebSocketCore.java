///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package automenta.climatenet.web;
//
//import automenta.climatenet.knowtention.Channel.ChannelChange;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.github.fge.jsonpatch.JsonPatch;
//import io.undertow.server.HttpHandler;
//import io.undertow.websockets.WebSocketConnectionCallback;
//import io.undertow.websockets.core.*;
//import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
//import io.undertow.websockets.spi.WebSocketHttpExchange;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//
//import static io.undertow.Handlers.websocket;
//
///**
// * Manages websocket i/o to a channel
// */
//public class WebSocketCore extends Core implements WebSocketConnectionCallback {
//
//
//    public WebSocketCore() {
//        super();
//    }
//
//    public WebSocketCore(Channel... initialChannels) {
//        super();
//
//        for (Channel c : initialChannels)
//            addChannel(c);
//    }
//
//    public HttpHandler handler() {
//        return websocket(this).addExtension(new PerMessageDeflateHandshake());
//    }
//
//    public class WebSocketConnection extends AbstractReceiveListener implements WebSocketCallback<Void> {
//
//        private final WebSocketChannel socket;
//
//        public WebSocketConnection(WebSocketChannel socket) {
//            super();
//
//            System.out.println(socket.getPeerAddress() + " connected websocket");
//
//            this.socket = socket;
//            /*Channel c = core.newChannel();
//            addChannel(c);
//
//            send(socket, c);
//            */
//        }
//
//
//
//        @Override
//        protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) throws IOException {
//
//            try {
//                //System.out.println(socket + " recv txt: " + message.getData());
//                JsonNode j = Core.json.readValue(message.getData(), JsonNode.class);
//                if (j.isArray()) {
//                    if (j.size() > 1) {
//                        String operation = j.get(0).textValue();
//                        if (operation!=null) {
//                            switch (operation) {
//                                case "p":
//                                    onPatch(j, socket);
//                                    break;
//                                case "on":
//                                    onOn(j, socket);
//                                    break;
//                                case "!":
//                                    onReload(j, socket);
//                                    break;
//                                case "off":
//                                    onOff(j, socket);
//                                    break;
//                                default:
//                                    onOperation(operation, channel(j), j, socket);
//                                    break;
//                            }
//                        }
//                    }
//
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
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
//        public Channel channel(JsonNode j) {
//            String channel = j.get(1).textValue();
//
//            Channel c = getChannel(this, channel);
//            return c;
//        }
//
//        protected Channel setChannelSubscription(JsonNode j, WebSocketChannel socket, boolean subscribed) {
//            String channel = j.get(1).textValue();
//
//            Channel c = getChannel(this, channel);
//            if (c==null) {
//                send(socket, channel + " does not exist");
//                return null;
//            }
//
//            c.set(channelObserver, subscribed, ChannelChange.class);
//
//            return c;
//        }
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
//        /** triggers channel .commit() and this may result in a patch (but not necessarily replace) */
//        protected void onReload(JsonNode j, WebSocketChannel socket) {
//            Channel c = channel(j);
//            c.commit();
//        }
//
//        /** 'on', subscribe */
//        protected void onOn(JsonNode j, WebSocketChannel socket) {
//            Channel c = setChannelSubscription(j, socket, true);
//            if (c==null) return;
//
//            c.commit();
//
//            sendChannel(c);
//        }
//
//        /** 'off', unsubscribe */
//        protected void onOff(JsonNode j, WebSocketChannel socket) {
//            setChannelSubscription(j, socket, false);
//        }
//
//        protected void onPatch(JsonNode j, WebSocketChannel socket) {
//            //jsonpatch
//            String channel = j.get(1).textValue();
//
//            Channel c = getChannel(channel);
//
//            ArrayNode patchJson = (ArrayNode) j.get(2);
//            try {
//
//
//                //JsonPatch patch = Core.getPatch(patchJson);
//                c.applyPatch(JsonPatch.fromJson(patchJson));
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                clientError(socket, e.toString());
//            }
//        }
//
//        public void clientError(WebSocketChannel socket, String message) {
//            WebSockets.sendText(message, socket, null);
//        }
//
//        @Override
//        protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
//
//            //System.out.println(channel + " recv bin: " + message.getData());
//        }
//
//        @Override
//        protected void onClose(WebSocketChannel socket, StreamSourceFrameChannel channel) throws IOException {
//
//            System.out.println(socket.getPeerAddress() + " disconnected websocket");
//        }
//
//        /**
//         * send the complete copy of the channel
//         */
//        public void send(WebSocketChannel socket, Channel c) {
//            send(socket, c.root);
//        }
//
//        public void send(WebSocketChannel socket, Object object) {
//            try {
//                ByteBuffer data = ByteBuffer.wrap(Core.json.writeValueAsBytes(object));
//                //System.out.println("Sending: " + data);
//                WebSockets.sendText(data, socket, this);
//            } catch (JsonProcessingException ex) {
//                ex.printStackTrace();
//            }
//        }
//
//        @Override
//        public void complete(WebSocketChannel wsc, Void t) {
//            //System.out.println("Sent: " + wsc);
//        }
//
//        @Override
//        public void onError(WebSocketChannel wsc, Void t, Throwable thrwbl) {
//            //System.out.println("Error: " + thrwbl);
//        }
//
//
//
//    }
//
//    @Override
//    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {
//        socket.getReceiveSetter().set(new WebSocketConnection(socket));
//        socket.resumeReceives();
//
//    }
//
//    protected void onOperation(String operation, Channel c, JsonNode param, WebSocketChannel socket) {
//
//    }
//
//}
