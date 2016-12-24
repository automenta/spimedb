/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.web;


import io.undertow.Undertow;
import io.undertow.server.handlers.resource.FileResourceManager;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;

import java.nio.file.Paths;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static io.undertow.UndertowOptions.ENABLE_SPDY;

/**
 *
 * @author me
 */
public class SpacetimeWebServer  {

    public static final org.slf4j.Logger logger = LoggerFactory.getLogger(SpacetimeWebServer.class.toString());
    public static final String resourcePath = Paths.get("src/main/resources/public/").toAbsolutePath().toString();
    private final SpimeDB db;


//    private List<String> paths = new ArrayList();
//    private final String host;
//    private final int port;

//    public Web add(String path, HttpHandler ph) {
//        super.addPrefixPath(path, ph);
//        paths.add(path);
//        return this;
//    }

    public SpacetimeWebServer(SpimeDB db, int port)   {
        this(db, "0.0.0.0", port);
    }

    public SpacetimeWebServer(final SpimeDB db, String host, int port) {
        super();
        this.db = db;
//        this.host = host;
//        this.port = port;

        Undertow.Builder b = Undertow.builder()
                .addHttpListener(port, host)
                .setServerOption(ENABLE_HTTP2, true)
                .setServerOption(ENABLE_SPDY, true)
                .setIoThreads(8)
                .setHandler(path().addPrefixPath("/",resource(new FileResourceManager(
                        Paths.get(resourcePath).toFile(), 0, true, "/"))
                        //.setDirectoryListingEnabled(true)
                //.setHandler(path().addPrefixPath("/", ClientResources.handleClientResources())
                ));

        logger.info("Starting web server @ {}:{}\n : resources={}", host, port, resourcePath);


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
//        addPrefixPath("/geoCircle", new HttpHandler() {
//
//            @Override
//            public void handleRequest(final HttpServerExchange ex) throws Exception {
//                Map<String, Deque<String>> reqParams = ex.getQueryParameters();
//
//                //   Deque<String> deque = reqParams.get("attrName");
//                //Deque<String> dequeVal = reqParams.get("value");
//                Deque<String> lats = reqParams.get("lat");
//                Deque<String> lons = reqParams.get("lon");
//                Deque<String> rads = reqParams.get("radiusM");
//
//                if (lats != null && lons != null && rads != null) {
//                    //System.out.println(lats.getFirst() + "  "+ lons.getFirst() + " "+ rads.getFirst());
//                    double lat = Double.parseDouble(lats.getFirst());
//                    double lon = Double.parseDouble(lons.getFirst());
//                    double rad = Double.parseDouble(rads.getFirst());
//
//                    SearchHits result = db.search(lat, lon, rad, 60);
//
//                    XContentBuilder d = responseTagOrFeature(result);
//
//                    send(d, ex);
//
//                }
//                ex.getResponseSender().send("");
//
//            }
//
//        });
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
