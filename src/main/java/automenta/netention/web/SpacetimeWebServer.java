/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.netention.web;

import automenta.netention.Core;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.syncleus.spangraph.InfiniPeer;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static io.undertow.Handlers.header;
import static io.undertow.Handlers.resource;

/**
 *
 * @author me
 */
public class SpacetimeWebServer extends PathHandler {

    public static final Logger logger = Logger.getLogger(SpacetimeWebServer.class.toString());


    final String clientPath = "./src/web";
    private final InfiniPeer db;


    private List<String> paths = new ArrayList();
    private final Undertow server;
    private final String host;
    private final int port;




    public SpacetimeWebServer(InfiniPeer db, int port) throws Exception {
        this(db, "localhost", port);
    }

    public SpacetimeWebServer(final InfiniPeer db, String host, int port) throws Exception {
        this.db = db;
        this.host = host;
        this.port = port;




        server = Undertow.builder()
                .addHttpListener(port, host)
                .setIoThreads(8)
                .setHandler(this)
                .build();


        //CORS fucking sucks
        /*  .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
            .header("Access-Control-Allow-Credentials", "true")
            .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
            .header("Access-Control-Max-Age", "1209600")
         */

        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
        addPrefixPath("/", header(resource(
                new FileResourceManager(new File(clientPath), 100, true, "/")).
                setDirectoryListingEnabled(false), "Access-Control-Allow-Origin", "*"));

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
//        addPrefixPath("/wikipedia", new Wikipedia());


    }



    public void start() {

        logger.info("Starting web server @ " + host + ":" + port + "\n  " + paths);

        server.start();

    }

    @Override
    public synchronized PathHandler addPrefixPath(String path, HttpHandler handler) {
        paths.add(path);
        return super.addPrefixPath(path, handler);
    }


    public static void send(String s, HttpServerExchange ex) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        try {
            ex.getOutputStream().write(Charset.forName("UTF-8").encode(s).array());
        } catch (IOException e) {
            logger.severe(e.toString());
        }

        ex.getResponseSender().close();
    }

    public static void send(JsonNode d, HttpServerExchange ex) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        try {
            Core.json.writeValue(ex.getOutputStream(), d);
        } catch (IOException ex1) {
            logger.severe(ex1.toString());
        }

        ex.getResponseSender().close();
    }


    public static String[] getStringArrayParameter(HttpServerExchange ex, String param) throws IOException {
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

}
