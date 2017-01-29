/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.server;


import com.google.common.collect.Lists;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.resource.FileResourceManager;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.suggest.Lookup;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.client.Client;
import spimedb.index.Search;
import spimedb.index.lucene.DocumentNObject;
import spimedb.util.HTTP;
import spimedb.util.JSON;
import spimedb.util.js.JavaToJavascript;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static spimedb.util.HTTP.getStringParameter;

/**
 * @author me
 */
public class WebServer extends PathHandler {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebServer.class);
    public static final String resourcePath = Paths.get("src/main/resources/public/").toAbsolutePath().toString();
    private final SpimeDB db;

    final JavaToJavascript j2js;

    private final double websocketOutputRateLimitBytesPerSecond = 1024 * 1024;


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
                o.write(JSON.toJSONBytes(db.tags().stream().map(db::get).toArray(NObject[]::new)));
            } catch (IOException e) {
                logger.warn("tag {}", e);
            }
        }));

        addPrefixPath("/suggest", ex -> HTTP.stream(ex, (o) -> {
            String qText = getStringParameter(ex, "q");
            if (qText==null || (qText=qText.trim()).isEmpty())
                return;

            try {
                List<Lookup.LookupResult> x = db.suggest(qText, 16);
                JSON.toJSON( Lists.transform(x, y -> y.key), o );
            } catch (Exception e) {
                logger.warn("suggest: {}", e);
                try { o.write(JSON.toJSONBytes(e)); } catch (IOException e1) { }
            }
        }));

        addPrefixPath("/search", ex -> HTTP.stream(ex, (o) -> {
            String qText = getStringParameter(ex, "q");
            if (qText==null || (qText=qText.trim()).isEmpty())
                return;

            try {

                Search.SearchResult x = db.find(qText, 10);

                o.write('[');
                Iterator<Document> ii = x.docs();
                while (ii.hasNext()) {
                    JSON.toJSON( DocumentNObject.get(ii.next()), o, ',' );
                }
                o.write("{}]".getBytes()); //<-- TODO search result metadata, query time etc

            } catch (Exception e) {
                logger.warn("{} -> {}", qText, e);
                try { o.write(JSON.toJSONBytes(e)); } catch (IOException e1) { }
            }

        }));


        /* client attention management */
        addPrefixPath("/attn", websocket(new Session(db, websocketOutputRateLimitBytesPerSecond)));


        Undertow.Builder b = Undertow.builder()
                .addHttpListener(port, host)
                .setServerOption(ENABLE_HTTP2, true)
                //.setServerOption(ENABLE_SPDY, true)
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
