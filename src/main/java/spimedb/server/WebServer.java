/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.server;


import com.fasterxml.jackson.core.JsonGenerator;
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
import org.eclipse.collections.api.map.primitive.ObjectFloatMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.infinispan.commons.util.concurrent.ConcurrentWeakKeyHashMap;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.client.Client;
import spimedb.query.Query;
import spimedb.util.HTTP;
import spimedb.util.JSON;
import spimedb.util.js.JavaToJavascript;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.Map;
import java.util.StringTokenizer;

import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static io.undertow.UndertowOptions.ENABLE_SPDY;

/**
 * @author me
 */
public class WebServer extends PathHandler {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebServer.class);
    public static final String resourcePath = Paths.get("src/main/resources/public/").toAbsolutePath().toString();
    private final SpimeDB db;

    final JavaToJavascript j2js;

    final ConcurrentWeakKeyHashMap<ServerConnection, Session> session = new ConcurrentWeakKeyHashMap<>();

    //    private List<String> paths = new ArrayList();
//    private final String host;
//    private final int port;

//    public Web add(String path, HttpHandler ph) {
//        super.addPrefixPath(path, ph);
//        paths.add(path);
//        return this;
//    }

    public WebServer(SpimeDB db, int port) {
        this(db, "0.0.0.0", port);
    }

    public WebServer(final SpimeDB db, String host, int port) {
        super();
        this.db = db;


        //Cache<MethodReference, Program> programCache = (Cache<MethodReference, Program>) Infinispan.cache(HTTP.TMP_SPIMEDB_CACHE_PATH + "/j2js" , "programCache");
        j2js = JavaToJavascript.build();

        addPrefixPath("/", resource(new FileResourceManager(
                Paths.get(resourcePath).toFile(), 0, true, "/")));


        addPrefixPath("/spimedb.js", ex -> HTTP.stream(ex, (o) -> {
            try {
                o.write(j2js.compileMain(Client.class).toString().getBytes());
            } catch (IOException e) {
                logger.warn("spimedb.js {}", e);
                try {
                    o.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }));

        addPrefixPath("/tag", ex -> HTTP.stream(ex, (o) -> {
            try {
                o.write(JSON.toJSON(db.schema.tags().stream().map(db::get).toArray(NObject[]::new)).getBytes());
            } catch (IOException e) {
                logger.warn("tag {}", e);
            }
        }));

        /* client attention management */
        addPrefixPath("/attn", websocket(new SessionSocket() {

            @Override
            protected void onMessage(WebSocketChannel socket, BufferedTextMessage message, Session session) {

                //if this is a new session, set default attention to the pagerank of tags
                if (session.attention.isEmpty()) {
                    ObjectFloatMap<String> rank = new ObjectFloatHashMap<String>();
                    session.attention.putAll(rank);
                }

                String messageData = message.getData();
                if (messageData.isEmpty()) {
                    send(socket, session.attention);
                } else {

                    StringTokenizer t = new StringTokenizer(messageData, "\t");
                    String tag = t.nextToken();
                    String value = t.nextToken();

                    logger.info("attn {} {}:{}", session, tag, value);

                    float fv = Float.valueOf(value);
                    if (fv != fv) /* NaN */
                        session.attention.remove(tag);
                    else
                        session.attention.put(tag, fv);
                }

            }
        }));

        //SECURITY RISK: DANGER

        addPrefixPath("/attn", new JavascriptShell((session, ws)->new Task(db, session, ws)).get());

        addPrefixPath("/shell", new JavascriptShell().with((e) -> {
            e.put("db", db);
        }));


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
                if (x1 != null && x2 != null && y1 != null && y2 != null) {
                    float[] lon = new float[]{Float.parseFloat(x1.getFirst()), Float.parseFloat(x2.getFirst())};
                    float[] lat = new float[]{Float.parseFloat(y1.getFirst()), Float.parseFloat(y2.getFirst())};

                    ex.setPersistent(true);

                    Session session = Session.session(ex);

                    HTTP.stream(ex, (o) -> {

                        try {
                            JsonGenerator gen =
                                    JSON.msgPackMapper.getFactory().createGenerator(o);

                            gen.writeStartArray();

                            final int[] count = {0};


                            String[] tags = new String[]{};

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
        logger.info("Start @ {}:{}\n\tresources={}", host, port, resourcePath);


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
