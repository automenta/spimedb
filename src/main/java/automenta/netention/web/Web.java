package automenta.netention.web;

import automenta.netention.Core;
import automenta.netention.run.SpimeServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.SetHeaderHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.util.Headers;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Utility functions for web server processes
 */
public class Web extends PathHandler {

    public static class ServletWeb extends Web {
        public final ServletContainer container = ServletContainer.Factory.newInstance();
        private final Set<Class<?>> services = new HashSet();

        public Web deploy(DeploymentInfo builder) {
            DeploymentManager manager = this.container.addDeployment(builder);
            manager.deploy();
            try {
                add(builder.getContextPath(), manager.start());
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Web add(Class<?> service) {
            services.add(service);
            return this;
        }


    }


    boolean compress = false;

    public Undertow server;

//    static void send(String s, HttpServerExchange ex) {
//        ex.startBlocking();
//
//        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
//
//        try {
//            ex.getOutputStream().write(Utf8.toUtf8(s));
//        } catch (IOException e) {
//            SpacetimeWebServer.logger.severe(e.toString());
//        }
//
//        ex.getResponseSender().close();
//    }

    public static void send(String s, HttpServerExchange ex) {
        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        ex.getResponseSender().send(s);
        ex.endExchange();
    }

    public static void send(Consumer<OutputStream> s, HttpServerExchange ex) {

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        ex.dispatch(() -> {
            ex.startBlocking();

            try {
                OutputStream os = ex.getOutputStream();
                s.accept(os);
                os.flush();
            } catch (Exception ex1) {
                SpimeServer.log.warn(ex1.toString());
            }

            ex.getResponseSender().close();
        });
    }


    static void send(byte[] s, HttpServerExchange ex, String type) {

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, type);

        ex.getResponseSender().send(ByteBuffer.wrap(s));

        //ex.getResponseSender().close();
        ex.endExchange();
    }

    static void send(JsonNode d, HttpServerExchange ex) {
        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        ex.startBlocking();

        try {
            Core.json.writeValue(ex.getOutputStream(), d);
        } catch (IOException ex1) {
            SpimeServer.log.warn(ex1.toString());
        }

        ex.getResponseSender().close();
    }

    static String[] getStringArrayParameter(HttpServerExchange ex, String param) throws IOException {
        Map<String, Deque<String>> reqParams = ex.getQueryParameters();

        Deque<String> idArray = reqParams.get(param);

        ArrayNode a = Core.json.readValue(idArray.getFirst(), ArrayNode.class);

        String[] ids = new String[a.size()];
        int j = 0;
        for (JsonNode x : a) {
            ids[j++] = x.textValue();
        }

        return ids;
    }

    public Web add(String path, HttpHandler ph) {
        super.addPrefixPath(path, ph);
        return this;
    }

    public Web start(Undertow.Builder builder) {
//        deploy(new Application() {
//            @Override public Set<Class<?>> getClasses() {
//                return services;
//            }
//        }, "/api");


        Undertow.Builder s;
        if (compress) {

            final EncodingHandler handler =
                    new EncodingHandler(new ContentEncodingRepository()
                            .addEncodingHandler("gzip",
                                    new GzipEncodingProvider(), 5,
                                   Predicates.parse("max-content-size[50000]"))
                            )
                            .setNext(this);

// ...
            s = builder.setHandler(handler);
        } else {

            s = builder.setHandler(new SetHeaderHandler(this, "Access-Control-Allow-Origin","*"));
        }
        this.server = s.build();
        this.server.start();
        return this;
    }

    public void stop() {
        this.server.stop();
    }

    public Web start(String host, int port) {

        return start(Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .setServerOption(UndertowOptions.ENABLE_SPDY, true)
                //.setWorkerThreads(6)
                //.setIoThreads(6)

                        //.addHttpsListener(8443, bindAddress, sslContext)

                .addHttpListener(port, host)

        );

    }


}
