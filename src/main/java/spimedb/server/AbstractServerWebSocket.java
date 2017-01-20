package spimedb.server;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import io.undertow.server.HttpHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.util.JSON;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import static io.undertow.Handlers.websocket;

/**
 * Manages websocket i/o to a channel
 */
abstract public class AbstractServerWebSocket extends AbstractReceiveListener implements WebSocketCallback<Void>, WebSocketConnectionCallback {

    static final Logger logger = LoggerFactory.getLogger(AbstractServerWebSocket.class);


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

    public static void sendJSONText(WebSocketChannel socket, Object object) throws IOException {
        sendJSON(socket, object, JSON.jsonText, null, null);
    }

    public static void sendJSONBinary(WebSocketChannel socket, Object object) throws IOException {
        sendJSON(socket, object, JSON.msgPackMapper,null, null);
    }

    public static void sendJSONBinary(WebSocketChannel socket, Object object, RateLimiter r, AtomicLong outBytes) throws IOException {
        sendJSON(socket, object, JSON.msgPackMapper, r, outBytes);
    }

    public static void sendJSON(WebSocketChannel socket, Object object, ObjectMapper encoder, @Nullable RateLimiter r, @Nullable AtomicLong outBytes) throws IOException {

        byte[] s;
        if (object instanceof byte[]) {
            s = (byte[])object;
        } else {
            try {
                s = encoder.writeValueAsBytes(object);
            } catch (JsonProcessingException t) {
                s = object.toString().getBytes(); //could not make json so just use toString()
            }
        }

        int size = s.length;

        if (r!=null) {
            r.acquire(size);
        }

        WebSockets.sendBinary(ByteBuffer.wrap(s), socket, null);
        //WebSockets.sendBinaryBlocking(ByteBuffer.wrap(s), socket);

        if (outBytes!=null)
            outBytes.addAndGet(size);
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

