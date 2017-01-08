/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.web;


import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import org.infinispan.commons.util.concurrent.ConcurrentWeakKeyHashMap;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;
import spimedb.query.Query;
import spimedb.util.HTTP;
import spimedb.util.JSON;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.Map;

import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static io.undertow.UndertowOptions.ENABLE_SPDY;

/**
 *
 * @author me
 */
public class WebServer extends PathHandler {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebServer.class);
    public static final String resourcePath = Paths.get("src/main/resources/public/").toAbsolutePath().toString();
    private final SpimeDB db;

    final ConcurrentWeakKeyHashMap<ServerConnection, Session> session = new ConcurrentWeakKeyHashMap<>();


//    private List<String> paths = new ArrayList();
//    private final String host;
//    private final int port;

//    public Web add(String path, HttpHandler ph) {
//        super.addPrefixPath(path, ph);
//        paths.add(path);
//        return this;
//    }

    public WebServer(SpimeDB db, int port)   {
        this(db, "0.0.0.0", port);
    }

    public WebServer(final SpimeDB db, String host, int port) {
        super();
        this.db = db;
//        this.host = host;
//        this.port = port;


        addPrefixPath("/",resource(new FileResourceManager(
                Paths.get(resourcePath).toFile(), 0, true, "/")));

        addPrefixPath("/tag", ex -> HTTP.stream(ex, (o) ->
                JSON.toJSON( Lists.newArrayList(Iterables.transform(db.schema.inh.nodes(), db::get)), o)));

        addPrefixPath("/session", websocket(new SessionSocket() {

            @Override
            protected void onMessage(WebSocketChannel socket, BufferedTextMessage message, Session session) {
                //System.out.println(socket + " " + message + " " + session);
            }
        }));

        //SECURITY RISK: DANGER
        /*
        addPrefixPath("/shell", new JavascriptShell().with((e)->{
            e.put("db", db);
        }));
        */

        addPrefixPath("/earth/region2d/summary", new HttpHandler() {

            final int MAX_RESULTS = 1024;
            final int MAX_RESPONSE_BYTES = 1024 * 1024;

            @Override
            public void handleRequest(final HttpServerExchange ex) throws Exception {

                Map<String, Deque<String>> reqParams = ex.getQueryParameters();

                /*Deque<String> rads = reqParams.get("r");
                Deque<String> lons = reqParams.get("x");
                Deque<String> lats = reqParams.get("y");*/

                Deque<String> x1 = reqParams.get("x1");
                Deque<String> x2 = reqParams.get("x2");
                Deque<String> y1 = reqParams.get("y1");
                Deque<String> y2 = reqParams.get("y2");

                //if (lats != null && lons != null && rads != null) {
                if (x1!=null && x2!=null && y1!=null && y2!=null) {
                    float[] lon = new float[] { Float.parseFloat(x1.getFirst()), Float.parseFloat(x2.getFirst()) };
                    float[] lat = new float[] { Float.parseFloat(y1.getFirst()), Float.parseFloat(y2.getFirst()) };

                    ex.setPersistent(true);

                    Session session = Session.session(ex);

                    HTTP.stream(ex, (o) -> {

                        try {
                            JsonGenerator gen =
                                    JSON.msgPackMapper.getFactory().createGenerator(o);

                            gen.writeStartArray();

                            final int[] count = {0};


                            String[] tags = new String[] {};

                            db.get(new Query((n) -> {

                                String i = n.id();
                                if (!session.sent.containsAndAdd(i)) {
                                    try {

                                        //gen.writeStartObject();
                                        gen.writeObject(n);
                                        if (count[0]++ >= MAX_RESULTS || ex.getResponseBytesSent() >= MAX_RESPONSE_BYTES)
                                            return false;

                                        //gen.writeEndObject();

                                        //gen.writeRaw(',');

                                    } catch (IOException e) {
                                        logger.error("{} {}", gen, ex.getRequestPath(), e);
                                        return false;
                                    }
                                }

                                return true; //continue

                            }).where(lon, lat).in(tags));

                            gen.writeEndArray();
                            gen.close();

                        } catch (IOException e2) {
                            logger.error("{} {}", ex.getRequestPath(), e2);
                        }

                    });

                    //System.out.println("bloom hit rate= " + bloom.hitrate(false));

                } else {
                    ex.getResponseSender().send("invalid parameters");
                }



            }


        });



        Undertow.Builder b = Undertow.builder()
                .addHttpListener(port, host)
                .setServerOption(ENABLE_HTTP2, true)
                .setServerOption(ENABLE_SPDY, true)
                .setIoThreads(8)
                .setHandler(new EncodingHandler(this, new ContentEncodingRepository()
                        .addEncodingHandler("gzip", new GzipEncodingProvider(), 100)
                        .addEncodingHandler("deflate", new DeflateEncodingProvider(), 50))
                );



                //.setDirectoryListingEnabled(true)
                //.setHandler(path().addPrefixPath("/", ClientResources.handleClientResources())

//        addPrefixPath("/tag/meta", new HttpHandler() {
//
//            @Override
//            public void handleRequest(HttpServerExchange ex) throws Exception {
//
//                sendTags(
//                        db.searchID(
//                                getStringArrayParameter(ex, "id"), 0, 60, "tag"
//                        ),
//                        ex);
//
//            }
//
//        });
//        addPrefixPath("/style/meta", new HttpHandler() {
//
//            @Override
//            public void handleRequest(HttpServerExchange ex) throws Exception {
//
//                send(json(
//                                db.searchID(
//                                        getStringArrayParameter(ex, "id"), 0, 60, "style"
//                                )),
//                        ex);
//
//            }
//
//        });
//
        logger.info("Start @ {}:{}\n\tdb={}\n\tresources={}", host, port, db, resourcePath);


        b.build().start();

        //CORS fucking sucks
        /*  .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
            .header("Access-Control-Allow-CredentialMax-Age", "1209600")
         */

        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java

//        addPrefixPath("/socket", new WebSocketCore(
//                index
//        ).handler());
//
//        addPrefixPath("/tag/index", new ChannelSnapshot(index));
//
//
//
//        addPrefixPath("/tag", (new WebSocketCore() {
//
//            final String cachePath = "cache";
//            final int cacheProxyPort = 16000;
//
//            @Override
//            public synchronized Channel getChannel(WebSocketCore.WebSocketConnection socket, String id) {
//                Channel c = super.getChannel(socket, id);
//
//                if (c == null) {
//                    //Tag t = new Tag(id, id);
//                    c = new ElasticChannel(db, id, "tag");
//                    super.addChannel(c);
//                }
//
//                return c;
//            }
//
//            @Override
//            protected void onOperation(String operation, Channel c, JsonNode param, WebSocketChannel socket) {
//
//                //TODO prevent interrupting update operation if already in-progress
//                switch (operation) {
//                    case "update":
//                        try {
//                            ObjectNode meta = (ObjectNode) c.getSnapshot().get("meta");
//                            if (meta!=null && meta.has("kmlLayer")) {
//                                String kml = meta.get("kmlLayer").textValue();
//
//                                {
//                                    ObjectNode nc = c.getSnapshot();
//                                    meta = (ObjectNode) nc.get("meta");
//
//                                    meta.put("status", "Updating");
//                                    c.commit(nc);
//                                }
//
//                                System.out.println("Updating " + c);
//
//                                //TODO replace proxy with HttpRequestCached:
////                                try {
////                                    new ImportKML(db, cache.proxy, c.id, kml).run();
////                                } catch (Exception e) {
////                                    ObjectNode nc = c.getSnapshot();
////                                    meta = (ObjectNode) nc.get("meta");
////                                    meta.put("status", e.toString());
////                                    c.commit(nc);
////                                    throw e;
////                                }
//
//                                {
//                                    ObjectNode nc = c.getSnapshot();
//                                    meta = (ObjectNode) nc.get("meta");
//
//                                    meta.put("status", "Ready");
//                                    meta.put("modifiedAt", new Date().getTime());
//                                    c.commit(nc);
//
//                                }
//
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//
//                        break;
//                }
//
//            }
//
//        }).handler());
//
//

//
        //addPrefixPath("/wikipedia", new Wikipedia());


    }



//    public void start() {
//
//        logger.info("Starting web server @ " + host + ":" + port + "\n  " + paths);
//
//        server.start();
//
//    }




}
