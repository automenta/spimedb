/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.server;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.resource.*;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.suggest.Lookup;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.xnio.BufferAllocator;
import spimedb.FilteredNObject;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.client.Client;
import spimedb.index.DObject;
import spimedb.index.SearchResult;
import spimedb.query.Query;
import spimedb.util.HTTP;
import spimedb.util.JSON;
import spimedb.util.js.JavaToJavascript;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static java.lang.Double.parseDouble;
import static spimedb.util.HTTP.getStringParameter;

/**
 * @author me
 */
public class WebServer extends PathHandler {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebServer.class);

    public static final String staticPath = Paths.get("src/main/resources/public/").toAbsolutePath().toString();

    private static final int BUFFER_SIZE = 32 * 1024;

    private final SpimeDB db;

    final JavaToJavascript j2js;

    @Deprecated private final double websocketOutputRateLimitBytesPerSecond = 64 * 1024;

    private int port = 0;
    private String host = null;

    static final ContentEncodingRepository compression = new ContentEncodingRepository()
            .addEncodingHandler("gzip", new GzipEncodingProvider(), 100)
            .addEncodingHandler("deflate", new DeflateEncodingProvider(), 50);

    private Undertow server;

    public static class OverridingFileResourceManager extends FileResourceManager {

        private final FileResourceManager override;

        public OverridingFileResourceManager(File base, int transferMinSize, File override) {
            super(base, transferMinSize, true, "/");

            this.override = new FileResourceManager(override, transferMinSize, true, "/");
        }

        @Override
        public Resource getResource(String p) {
            Resource x = override.getResource(p);
            if (x != null)
                return x;
            else
                return super.getResource(p);
        }

    }

    //final Default nar = NARBuilder.newMultiThreadNAR(1, new RealTime.DS());

//    @Override
//    public void handleRequest(HttpServerExchange exchange) throws Exception {
//        String s = exchange.getQueryString();
//        nar.believe(
//                s.isEmpty() ?
//
//            $.func(
//                $.the(exchange.getDestinationAddress().toString()),
//                $.quote(exchange.getRequestURL())
//             )
//                        :
//            $.func(
//                $.the(exchange.getDestinationAddress().toString()),
//                $.quote(exchange.getRequestURL()),
//                $.the(s)  ),
//
//            Tense.Present
//        );
//
//        super.handleRequest(exchange);
//    }

    public WebServer(final SpimeDB db) {
        super();
        this.db = db;


//        nar.log();
//        nar.loop(10f);


        initStaticResource(db);


//        try {
//            addPrefixPath("/", WebdavServlet.get("/"));
//        } catch (ServletException e) {
//            logger.error("{}", e);
//        }

        j2js = JavaToJavascript.build();
        initJ2JS();

        addPrefixPath("/tag", ex -> HTTP.stream(ex, (o) -> {
            try {
                o.write(JSON.toJSONBytes(db.tags().stream().map(db::get).toArray(NObject[]::new)));
            } catch (IOException e) {
                logger.error("tag {}", e);
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
                logger.error("suggest: {}", e.getMessage());
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

                FacetResult x = db.facets(dimension, 48);

                if (x != null)
                    stream(o, x);

            } catch (Exception e) {
                logger.warn("facet: {}", e.getMessage());
            }
        }));

        addPrefixPath("/thumbnail", ex -> {
            send(getStringParameter(ex, "I"), "thumbnail", "image/jpg", ex);
        });
        addPrefixPath("/data", ex -> {
            send(getStringParameter(ex, "I"), "data", "application/pdf", ex);
        });

        addPrefixPath("/earth", ex -> HTTP.stream(ex, (o) -> {
            String b = getStringParameter(ex, "r");
            String[] bb = b.split("_");
            if (bb.length!=4) {
                ex.setStatusCode(StatusCodes.BAD_REQUEST);
                return;
            }

            double[] lons = new double[2], lats = new double[2];

            lons[0] = parseDouble(bb[0]);
            lats[0] = parseDouble(bb[1]);
            lons[1] = parseDouble(bb[2]);
            lats[1] = parseDouble(bb[3]);

            SearchResult r = db.get(new Query().limit(32).where(lons, lats));
            send(r, o, ex, searchResultSummary);

        }));



        addExactPath("/tell/json", (e) -> {
            //POST only
            if (e.getRequestMethod().equals(HttpString.tryFromString("POST"))) {
                //System.out.println(e);
                //System.out.println(e.getRequestHeaders());

                e.getRequestReceiver().receiveFullString((ex, s) -> {

                    JsonNode x = JSON.fromJSON(s);

                    JsonNode inode = x.get("I");
                    String I = (inode == null) ? UUID.randomUUID().toString() : inode.toString();

                    MutableNObject d = new MutableNObject(I)
                            .putAll(x)
                            .when(System.currentTimeMillis());

                    db.add( d );
                });

                e.endExchange();
            }
        });

        addPrefixPath("/search", ex -> HTTP.stream(ex, (o) -> {
            String qText = getStringParameter(ex, "q");
            if (qText == null || (qText = qText.trim()).isEmpty())
                return;

            try {
                send(db.find(qText, 50), o, ex, searchResultFull);
            } catch (Exception e) {
                logger.warn("{} -> {}", qText, e.getMessage());
                ex.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            }

        }));


        /* client attention management */
        //addPrefixPath("/client", websocket(new ClientSession(db, websocketOutputRateLimitBytesPerSecond)));
        addPrefixPath("/anon",
            websocket( new AnonymousSession(db) )
        );


        //addPrefixPath("/admin", websocket(new Admin(db)));

        restart();

    }

    private void initJ2JS() {
        //Cache<MethodReference, Program> programCache = (Cache<MethodReference, Program>) Infinispan.cache(HTTP.TMP_SPIMEDB_CACHE_PATH + "/j2js" , "programCache");
        addPrefixPath("/spimedb.js", ex -> HTTP.stream(ex, (o) -> {
            try {
                o.write(j2js.compileMain(Client.class).toString().getBytes());
            } catch (IOException e) {
                logger.error("spimedb.js {}", e);

            }
        }));
    }

    private void initStaticResource(SpimeDB db) {
        File staticPath = Paths.get(WebServer.staticPath).toFile();
        File myStaticPath = db.file!=null ? db.file.getParentFile().toPath().resolve("public").toFile() : null;

        int transferMinSize = 1024 * 1024;
        final int METADATA_MAX_AGE = 3 * 1000; //ms

        ResourceManager res;
        if (db.indexPath!=null && myStaticPath!=null && myStaticPath.exists()) {
            res = new OverridingFileResourceManager(staticPath, transferMinSize, myStaticPath);
        } else {
            //HACK add the defaut local to prevent 404's
            //res = new ClassPathResourceManager(getClass().getClassLoader(), staticPath);
            res = new FileResourceManager(
                    staticPath, 0, true, "/");
        }

        DirectBufferCache dataCache = new DirectBufferCache(1000, 10,
                16 * 1024 * 1024, BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR,
                METADATA_MAX_AGE);

        CachingResourceManager cres = new CachingResourceManager(
                100,
                transferMinSize /* max size */,
                dataCache, res, METADATA_MAX_AGE);


        ResourceHandler rr = resource(cres);
        rr.setCacheTime(24 * 60 * 60 * 1000);
        addPrefixPath("/", rr);
    }

    private void send(SearchResult r, OutputStream o, HttpServerExchange ex, ImmutableSet<String> keys) {
        if (r!=null) {

            try {
                o.write("[[".getBytes());
                r.forEachDocument((y, x) -> {
                    JSON.toJSON(searchResult(
                            DObject.get(y), x, keys
                    ), o, ',');
                    return true;
                });
                o.write("{}],".getBytes()); //<-- TODO search result metadata, query time etc

                if (r.facets != null) {
                    stream(o, r.facets);
                    o.write(']');
                } else
                    o.write("[]]".getBytes());

                r.close();
                return;

            } catch (IOException e) {

            }
        }

        ex.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);

    }

    private void stream(OutputStream o, FacetResult x) {
        JSON.toJSON(
                Stream.of(x.labelValues).map(y -> new Object[]{y.label, y.value}).toArray(Object[]::new)
                /*Stream.of(x.labelValues).collect(
                Collectors.toMap(y->y.label, y->y.value ))*/, o);
    }


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

    private void send(@Nullable String id, String field, @Deprecated @Nullable String contentType, HttpServerExchange ex) {
        if (id != null) {

            DObject d = db.get(id);
            if (d != null) {
                Object f = d.get(field);

                if (f instanceof String) {
                    //interpret the string stored at this as a URL or a redirect to another field
                    String s = (String) f;
                    switch (s) {
                        case "data":
                            if (!field.equals("data"))
                                send(id, "data", contentType, ex);
                            else {
                                //infinite loop
                                throw new UnsupportedOperationException("document field redirect cycle");
                            }
                            break;
                        default:
                            if (s.startsWith("file:")) {
                                File ff = new File(s.substring(5));
                                if (ff.exists()) {
                                    HTTP.stream(ex, (o) -> {
                                        try {
                                            IOUtils.copyLarge(new FileInputStream(ff), o, new byte[BUFFER_SIZE]);
                                        } catch (IOException e) {
                                            ex.setStatusCode(404);
                                        }
                                    }, contentType != null ? contentType : "text/plain");

                                } else {
                                    ex.setStatusCode(404);
                                }
                            }
                            break;
                    }
                } else if (f instanceof byte[]) {

                    byte[] b = (byte[]) f;

                    HTTP.stream(ex, (o) -> {
                        try { o.write(b); } catch (IOException e) {   }
                    }, contentType != null ? contentType : "text/plain");

                } else {
                    ex.setStatusCode(404);
                }

            } else {
                ex.setStatusCode(404);
            }

        } else {
            ex.setStatusCode(404);
        }
    }

    static final ImmutableSet<String> searchResultSummary =
            Sets.immutable.of(
                    NObject.ID, NObject.NAME, NObject.INH, NObject.TAG, NObject.BOUND,
                    "thumbnail", "score", NObject.LINESTRING, NObject.POLYGON,
                    NObject.TYPE
            );
    static final ImmutableSet<String> searchResultFull =
            Sets.immutable.withAll(Iterables.concat(Sets.mutable.ofAll(searchResultSummary), Sets.immutable.of(
                    NObject.DESC, "data"
            )));

    private static FilteredNObject searchResult(NObject d, ScoreDoc x, ImmutableSet<String> keys) {
        return new FilteredNObject(d, keys) {
            @Override
            protected Object value(String key, Object v) {
                switch (key) {
                    case "thumbnail":
                        //rewrite the thumbnail blob byte[] as a String URL
                        return d.id();
                    case "data":
                        //rewrite the thumbnail blob byte[] as a String URL (if not already a string representing a URL)
                        if (v instanceof byte[]) {
                            return d.id();
                        } else if (v instanceof String) {
                            String s = (String)v;
                            if (s.startsWith("file:")) {
                                return d.id();  //same as if it's a byte
                            } else {
                                return s;
                            }
                        } else {
                            //??
                        }
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

