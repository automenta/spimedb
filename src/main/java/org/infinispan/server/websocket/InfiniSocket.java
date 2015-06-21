package org.infinispan.server.websocket;

import automenta.netention.web.ClientResources;
import com.syncleus.spangraph.InfiniPeer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.CharsetUtil;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.server.websocket.handlers.GetHandler;
import org.infinispan.server.websocket.handlers.NotifyHandler;
import org.infinispan.server.websocket.handlers.PutHandler;
import org.infinispan.server.websocket.handlers.RemoveHandler;
import org.infinispan.server.websocket.json.JsonConversionException;
import org.infinispan.server.websocket.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static io.undertow.Handlers.websocket;


/**
 * An HTTP server which serves Web Socket requests on an Infinispan cacheManager.
 * <p>
 *    Websocket specific code lifted from Netty WebSocket Server example.
 * </p>
 */

public class InfiniSocket implements WebSocketConnectionCallback {


    private final CacheContainer caches;
    private Map<String, OpHandler> operationHandlers;

    public static void main(String[] args) {

        InfiniPeer caches = InfiniPeer.local("x");
        Cache<Object, Object> cacheC = caches.the("c");

        Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .setServerOption(UndertowOptions.ENABLE_SPDY, true)
                //.addHttpsListener(8443, bindAddress, sslContext)

                .addHttpListener(8080, "localhost")
                .setIoThreads(4)
                .setHandler(new InfiniSocket(caches).handler()).build().start();

    }

    public InfiniSocket(CacheContainer caches) {
        this.caches = caches;

        operationHandlers = new HashMap<String, OpHandler>();
        operationHandlers.put("put", new PutHandler());
        operationHandlers.put("get", new GetHandler());
        operationHandlers.put("remove", new RemoveHandler());
        NotifyHandler notifyHandler = new NotifyHandler();
        operationHandlers.put("notify", notifyHandler);
        operationHandlers.put("unnotify", notifyHandler);
    }

    public static final String INFINISPAN_WS_JS_FILENAME = "infinispan-ws.js";

    private static String javascript;


    public class WebSocketConnection extends AbstractReceiveListener implements WebSocketCallback<Void> {

        private final WebSocketChannel socket;

        private static final String INFINISPAN_WS_JS_FILENAME = "infinispan-ws.js";


        private boolean connectionUpgraded;
        //private final Map<String, Cache<Object, Object>> startedCaches;
        private WebSocketServerHandshaker handshaker;

        public WebSocketConnection(WebSocketChannel socket) {
            this.socket = socket;
        }

        @Override
        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
            handle(message.getData());
        }

        protected void handle(String message) {
            JsonObject payload = null;
            try {
                payload = JsonObject.fromString(message);
                String opCode = (String) payload.get(OpHandler.OP_CODE);
                String cacheName = (String) payload.get(OpHandler.CACHE_NAME);
                Cache<Object, Object> cache = caches.getCache(cacheName);

                OpHandler handler = operationHandlers.get(opCode);
                if (handler != null) {
                    handler.handleOp(payload, cache, socket);
                }
            } catch (JsonConversionException e) {
                e.printStackTrace();
            }

        }

//        public static void send(WebSocketChannel socket, String dataobject) {
//            try {
//                ByteBuffer data = ByteBuffer.wrap(Core.json.writeValueAsBytes(object));
//                //System.out.println("Sending: " + data);
//                WebSockets.sendText(data, socket, this);
//            } catch (JsonProcessingException ex) {
//                ex.printStackTrace();
//            }
//        }


        @Override
        public void complete(WebSocketChannel webSocketChannel, Void aVoid) {

        }

        @Override
        public void onError(WebSocketChannel webSocketChannel, Void aVoid, Throwable throwable) {

        }
    }

    private void loadScriptToResponse(FullHttpRequest req, DefaultFullHttpResponse res) {
        String wsAddress = getWebSocketLocation(req);

        StringWriter writer = new StringWriter();
        writer.write("var defaultWSAddress = '" + wsAddress + "';");
        writer.write(InfiniSocket.getJavascript());

        ByteBuf content = res.content().writeBytes(writer.toString().getBytes(CharsetUtil.UTF_8));

        res.headers().set("Content-type", "text/javascript; charset=UTF-8");

    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //log.debugf(cause, "Error processing request on channel %s" , ctx.name());
        cause.printStackTrace();
        ctx.close();
    }

    private String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.headers().get(HttpHeaders.Names.HOST) + "/";
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {
        socket.getReceiveSetter().set(new WebSocketConnection(socket));
        socket.resumeReceives();
    }

    public HttpHandler handler() {

        return websocket(this, httpHandler()).addExtension(new PerMessageDeflateHandshake());
    }

    private HttpHandler httpHandler() {
        return ClientResources.handleClientResources();
    }


//    private static class WebSocketServerPipelineFactory extends ChannelInitializer<Channel> {
//
//        private CacheContainer cacheContainer;
//        private Map<String, OpHandler> operationHandlers;
//        private Map<String, Cache<Object, Object>> startedCaches = CollectionFactory.makeConcurrentMap();
//
//        public WebSocketServerPipelineFactory(CacheContainer cacheContainer) {
//            this.cacheContainer = cacheContainer;
//
//            operationHandlers = new HashMap<String, OpHandler>();
//            operationHandlers.put("put", new PutHandler());
//            operationHandlers.put("get", new GetHandler());
//            operationHandlers.put("remove", new RemoveHandler());
//            NotifyHandler notifyHandler = new NotifyHandler();
//            operationHandlers.put("notify", notifyHandler);
//            operationHandlers.put("unnotify", notifyHandler);
//        }
//
//        @Override
//        public void initChannel(Channel channel) throws Exception {
//            // Create a default pipeline implementation.
//            ChannelPipeline pipeline = channel.pipeline();
//
//            pipeline.addLast("decoder", new HttpRequestDecoder());
//            pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
//            pipeline.addLast("encoder", new HttpResponseEncoder());
//            pipeline.addLast("handler", new WebSocketServerHandler(cacheContainer, operationHandlers, startedCaches));
//        }
//    }

    public static String getJavascript() {
        if (javascript != null) {
            return javascript;
        }

        BufferedReader scriptReader = new BufferedReader(new InputStreamReader(InfiniSocket.class.getResourceAsStream(INFINISPAN_WS_JS_FILENAME)));

        try {
            StringWriter writer = new StringWriter();

            String line = scriptReader.readLine();
            while (line != null) {
                writer.write(line);
                writer.write('\n');
                line = scriptReader.readLine();
            }

            javascript = writer.toString();

        } catch (IOException e) {
            e.printStackTrace();
            //throw logger.unableToSendWebSocketsScriptToTheClient(e);
        } finally {
            try {
                scriptReader.close();
            } catch (IOException e) {
                e.printStackTrace();
                //throw logger.unableToCloseWebSocketsStream(e);
            }
        }

        return javascript;

    }
}
