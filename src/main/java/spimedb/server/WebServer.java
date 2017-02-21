/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.server;


import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.resource.FileResourceManager;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.suggest.Lookup;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import spimedb.FilteredNObject;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.client.Client;
import spimedb.index.DObject;
import spimedb.index.SearchResult;
import spimedb.server.webdav.WebdavServlet;
import spimedb.util.HTTP;
import spimedb.util.JSON;
import spimedb.util.js.JavaToJavascript;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static spimedb.util.HTTP.getStringParameter;

/**
 * @author me
 */
public class WebServer extends PathHandler {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebServer.class);

    public static final String staticPath = Paths.get("src/main/resources/public/").toAbsolutePath().toString();

    public static final String localPathDefault = Paths.get("src/main/resources/local/").toAbsolutePath().toString();

    private final SpimeDB db;

    final JavaToJavascript j2js;

    @Deprecated private final double websocketOutputRateLimitBytesPerSecond = 64 * 1024;

    private int port = 0;
    private String host = null;

    static final ContentEncodingRepository compression = new ContentEncodingRepository()
            .addEncodingHandler("gzip", new GzipEncodingProvider(), 100)
            .addEncodingHandler("deflate", new DeflateEncodingProvider(), 50);

    private Undertow server;


    public WebServer(final SpimeDB db) {
        super();
        this.db = db;

        //Cache<MethodReference, Program> programCache = (Cache<MethodReference, Program>) Infinispan.cache(HTTP.TMP_SPIMEDB_CACHE_PATH + "/j2js" , "programCache");
        j2js = JavaToJavascript.build();

        boolean localPath = false;
        if (db.indexPath!=null) {
            File publicFolder = db.file.getParentFile().toPath().resolve("public").toFile();
            if (publicFolder.exists()) {

                addPrefixPath("/local", resource(new FileResourceManager(
                        publicFolder, 0, true, "/")));
                localPath = true;
            }
        }

        if (!localPath) {
            //HACK add the defaut local to prevent 404's
            addPrefixPath("/local", resource(new FileResourceManager(
                    Paths.get(localPathDefault).toFile(), 0, true, "/")));
        }

        File staticPathFile = Paths.get(staticPath).toFile();

        addPrefixPath("/", resource(new FileResourceManager(
                staticPathFile, 0, true, "/")));


//        try {
//            addPrefixPath("/", WebdavServlet.get("/"));
//        } catch (ServletException e) {
//            logger.error("{}", e);
//        }

        addPrefixPath("/spimedb.js", ex -> HTTP.stream(ex, (o) -> {
            try {
                o.write(j2js.compileMain(Client.class).toString().getBytes());
            } catch (IOException e) {
                logger.warn("spimedb.js {}", e);

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
            if (qText == null || (qText = qText.trim()).isEmpty())
                return;

            try {
                List<Lookup.LookupResult> x = db.suggest(qText, 16);
                if (x != null)
                    JSON.toJSON(Lists.transform(x, y -> y.key), o);

            } catch (Exception e) {
                logger.warn("suggest: {}", e.getMessage());
                /*(try {
                    o.write(JSON.toJSONBytes(e));
                } catch (IOException e1) {
                })*/
            }
        }));

        addPrefixPath("/facet", ex -> HTTP.stream(ex, (o) -> {
            String dimension = getStringParameter(ex, "q");
            if (dimension == null || (dimension = dimension.trim()).isEmpty())
                return;

            try {

                FacetResult x = db.facets(dimension, 32);

                if (x != null)
                    JSON.toJSON(
                            Stream.of(x.labelValues).map(y -> new Object[]{y.label, y.value}).toArray(Object[]::new)
                            /*Stream.of(x.labelValues).collect(
                            Collectors.toMap(y->y.label, y->y.value ))*/, o);

            } catch (Exception e) {
                logger.warn("suggest: {}", e.getMessage());
            }
        }));

        addPrefixPath("/thumbnail", ex -> {
            send(getStringParameter(ex, "I"), "thumbnail", "image/jpg", ex);
        });
        addPrefixPath("/data", ex -> {
            send(getStringParameter(ex, "I"), "data", "application/pdf", ex);
        });

        addPrefixPath("/search", ex -> HTTP.stream(ex, (o) -> {
            String qText = getStringParameter(ex, "q");
            if (qText == null || (qText = qText.trim()).isEmpty())
                return;

            try {

                SearchResult xx = db.find(qText, 10);
                if (xx!=null) {

                    o.write('[');
                    xx.forEach((r, x) -> {
                        JSON.toJSON(searchResult(
                                DObject.get(r), x
                        ), o, ',');
                    });
                    o.write("{}]".getBytes()); //<-- TODO search result metadata, query time etc

                    xx.close();
                }

            } catch (Exception e) {
                logger.warn("{} -> {}", qText, e.getMessage());
                /*try {
                    o.write(JSON.toJSONBytes(e));
                } catch (IOException e1) {
                }*/
            }

        }));


        /* client attention management */
        addPrefixPath("/attn", websocket(new Session(db, websocketOutputRateLimitBytesPerSecond)));

        restart();

    }

//    void setStatic(String path) {
//        if (usePath!=null) {
//            addPrefixPath("/", resource(new FileResourceManager(
//                    Paths.get(usePath).toFile(), 0, true, "/")));
//        }
//    }

    public void setHost(String host) {

        if (!Objects.equal(this.host, host)) {
            this.host = host;
            restart();
        }
    }

    private synchronized void restart() {
        String host = this.host;

        if (host == null)
            host = "0.0.0.0"; //any IPv4

        if (port == 0)
            return;

        Undertow.Builder b = Undertow.builder()
                .addHttpListener(port, host)
                .setServerOption(ENABLE_HTTP2, true);

        if (compression != null)
            b.setHandler(new EncodingHandler(this, compression));

        Undertow nextServer = b.build();
        if (server != null) {
            try {
                logger.error("stop: {}", server);
                server.stop();
            } catch (Exception e) {
                logger.error("http stop: {}", e);
                this.server = null;
            }
        }

        try {
            logger.info("start: {}:{} staticPath={}", host, port, staticPath);
            (this.server = nextServer).start();
        } catch (Exception e) {
            logger.error("http start: {}", e);
            this.server = null;
        }

    }

    public void setPort(int port) {
        if (this.port != port) {
            this.port = port;
            restart();
        }
    }

    private void send(@Nullable String id, String field, @Nullable String contentType, HttpServerExchange ex) {
        if (id != null) {
            HTTP.stream(ex, (o) -> {

                DObject d = db.get(id);
                if (d != null) {
                    byte[] b = d.get(field);
                    if (b != null) {
                        try {
                            o.write(b);
                        } catch (IOException e) {

                        }
                    }
                } else {
                    ex.setStatusCode(404);
                }

            }, contentType != null ? contentType : "text/plain");
        } else {
            ex.setStatusCode(404);
        }
    }

    static final ImmutableSet<String> searchResultKeys =
            Sets.immutable.of(
                    NObject.ID, NObject.NAME, NObject.DESC, NObject.INH, NObject.TAG, NObject.BOUND,
                    "thumbnail", "data", "score",
                    NObject.TYPE
            );

    private FilteredNObject searchResult(NObject d, ScoreDoc x) {
        return new FilteredNObject(db.graphed(d), searchResultKeys) {
            @Override
            protected Object value(String key, Object v) {
                switch (key) {
                    case "thumbnail":
                        //rewrite the thumbnail blob byte[] as a String URL
                        return "/thumbnail?I=" + d.id();
                    case "data":
                        //rewrite the thumbnail blob byte[] as a String URL (if not already a string representing a URL)
                        return !(v instanceof String) ? "/data?I=" + d.id() : v;
                }
                return v;
            }

            @Override
            public void forEach(BiConsumer<String, Object> each) {
                super.forEach(each);
                each.accept("score", x.score);
            }
        };
    }


}


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

