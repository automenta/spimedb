package spimedb.server;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.undertow.server.HttpHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.eclipse.collections.api.map.primitive.ObjectFloatMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.util.JSON;

import java.io.IOException;
import java.nio.ByteBuffer;

import static io.undertow.Handlers.websocket;

/**
 * Manages websocket i/o to a channel
 */
abstract public class ServerWebSocket extends AbstractReceiveListener implements WebSocketCallback<Void>, WebSocketConnectionCallback {

    static final Logger logger = LoggerFactory.getLogger(ServerWebSocket.class);


    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {

        logger.info("{} connect {}", socket.getPeerAddress(), socket.getUrl() );

        socket.getReceiveSetter().set(this);
        socket.resumeReceives();

    }

    @Override
    protected void onClose(WebSocketChannel socket, StreamSourceFrameChannel channel) throws IOException {

        logger.info("{} disconnect", socket.getPeerAddress());
    }


    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) throws IOException {


    }


    @Override
    protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {

        //System.out.println(channel + " recv bin: " + message.getData());
    }


    public static void send(WebSocketChannel socket, String s) {


        try {
            WebSockets.sendTextBlocking(s, socket);
        } catch (IOException e) {
            logger.error("err: {} {}", socket, e.toString());
        }

    }

    public static void send(WebSocketChannel socket, ObjectFloatMap<String> m) {
        //TODO use MsgPack'd JSON object

        StringBuilder sb = new StringBuilder(m.size() * 16).append('{');
        m.forEachKeyValue((k,v) -> {
            sb.append('\"').append(k).append("\":").append(v).append(',');
        });
        int cur = sb.length();
        if (cur > 1) //remove trailing ',' if at least one element was printed
            sb.setLength(cur-1);
        sb.append('}');
        send(socket, sb.toString());
    }

    public static void send(WebSocketChannel socket, Object object) {


        byte[] s;
        try {
            s = JSON.jsonLoose.writeValueAsBytes(object);
        } catch (JsonProcessingException t) {
            s = object.toString().getBytes(); //could not make json so just use toString()
        }

        try {
            WebSockets.sendTextBlocking(ByteBuffer.wrap(s), socket);
        } catch (IOException e) {
            logger.error("err: {} {}", socket, e.toString());
        }


    }


    @Override
    public void onError(WebSocketChannel wsc, Void t, Throwable thrwbl) {
        logger.error("err: {} {}", wsc, thrwbl.toString());
    }

    @Override
    public void complete(WebSocketChannel channel, Void context) {
    }



    public HttpHandler get() {
        return websocket(this).addExtension(new PerMessageDeflateHandshake());
    }

}

//    protected void onJSONMessage(WebSocketChannel socket, JsonNode j) {
//
//    }

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

