/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.server;


import com.google.common.base.Objects;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import org.eclipse.collections.impl.factory.Sets;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.io.File;
import java.nio.file.Paths;
import java.util.Set;

import static io.undertow.Handlers.ipAccessControl;
import static io.undertow.Handlers.websocket;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;

/**
 * @author me
 *         see:
 *         https://docs.jboss.org/resteasy/docs/3.1.2.Final/userguide/html/
 *         https://docs.jboss.org/resteasy/docs/3.1.2.Final/userguide/html/RESTEasy_Embedded_Container.html#d4e1380
 */
public class WebServer extends PathHandler {


    private final boolean CORS = true;

    private boolean development = false;

    @ApplicationPath("/")
    public final class WebApp extends Application {

        @Override
        public Set getSingletons() {
            return Sets.mutable.of(
                    new WebAPI(WebServer.this)
            );
        }

        @Override
        public Set<Class<?>> getClasses() {
            return Sets.mutable.of(
                    io.swagger.jaxrs.listing.ApiListingResource.class,
                    io.swagger.jaxrs.listing.SwaggerSerializers.class
            );
        }

    }

    public Undertow server;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebServer.class);

    static final String staticPath = Paths.get("src/main/resources/public/").toAbsolutePath().toString();


    public final SpimeDB db;


    private int port = Integer.MIN_VALUE;
    private String host = null;

    static final ContentEncodingRepository compression = new ContentEncodingRepository()
            .addEncodingHandler("gzip", new GzipEncodingProvider(), 100)
            .addEncodingHandler("deflate", new DeflateEncodingProvider(), 50);


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

        Application application = new WebApp();

        String contextPath = "/";
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplication(application);
        DeploymentInfo di = this.undertowDeployment(deployment);
        di.setClassLoader(application.getClass().getClassLoader());
        di.setContextPath(contextPath);
        di.setDeploymentName("SpimeDB");
        di.setAsyncExecutor(db.exe);

        //deployment.getProviderFactory().
        DeploymentManager manager = container.addDeployment(di);
        manager.deploy();

        try {
            HttpHandler api = Handlers.disableCache(manager.start());

            if (CORS)
                api = Handlers.header(api, "Access-Control-Allow-Origin", "*");

            ResourceHandler statics = new ResourceHandler(staticResources(db), api);
            statics.setCacheTime(24 * 60 * 60 * 1000);

            if (development) { //add cache filter for client-side code development
                statics.setCachable(p -> {

                    String rp = p.getRequestPath();
                    if (!rp.startsWith("/lib/")) {
                        if (rp.endsWith(".js") || rp.endsWith(".html") || rp.endsWith(".css"))
                            return false;
                    }


                    return true;
                });
            }
            addPrefixPath("/", statics);

        } catch (ServletException var4) {
            throw new RuntimeException(var4);
        }


//        try {
//            addPrefixPath("/", WebdavServlet.get("/"));
//        } catch (ServletException e) {
//            logger.error("{}", e);
//        }

//        addPrefixPath("/tag", ex -> HTTP.stream(ex, (o) -> {
//            try {
//                o.write(JSON.toJSONBytes(db.tags().stream().map(db::get).toArray(NObject[]::new)));
//            } catch (IOException e) {
//                logger.error("tag {}", e);
//            }
//        }));







        /* client attention management */
        //addPrefixPath("/client", websocket(new ClientSession(db, websocketOutputRateLimitBytesPerSecond)));

//        addPrefixPath("/anon",
//            websocket( new AnonymousSession(db) )
//        );
//
//        addPrefixPath("/on/tag/",
//                //getRequestPath().substring(8)
//                websocket( AnonymousSession.tag(db, "public") ) );
//
//        addPrefixPath("/console",
//                //getRequestPath().substring(8)
//                websocket( new ConsoleSession(db) ) );

        addPrefixPath("/admin",
                ipAccessControl(websocket(new Admin(db)), false)
                    .addAllow("0.0.0.0" /* localhost only (ipv4) */)
                    .addAllow("0:0:0:0:0:0:0:1" /* loalhost only (ipv6) */)
        );

        restart();

    }


    private ResourceManager staticResources(SpimeDB db) {

        int transferMinSize = 1024 * 1024;

        ChainedResourceManager res = new ChainedResourceManager();

        if (db.indexPath != null) {
            File myStaticPath = db.file != null ? db.file.getParentFile().toPath().resolve("public").toFile() : null;
            if (myStaticPath != null && myStaticPath.exists()) {
                logger.info("static resource: overlay {}", myStaticPath);
                res.add(
                        new FileResourceManager(myStaticPath, transferMinSize, true, "/")
                );
            }
        }

        File staticPath = Paths.get(WebServer.staticPath).toFile();
        if (staticPath != null && staticPath.exists()) {
            logger.info("static resource: source {} (development mode)", staticPath);
            development = true;
            res.add(
                    new FileResourceManager(staticPath, transferMinSize, true, "/")
            );
            return res;
        } else {
            logger.info("static resource: classloader");
            development = false;
            res.add(
                    new ClassPathResourceManager(getClass().getClassLoader(), "public")
            );
        }

        return res;

//        final int METADATA_MAX_AGE = 3 * 1000; //ms
//        DirectBufferCache dataCache = new DirectBufferCache(1000, 10,
//                16 * 1024 * 1024, BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR,
//                METADATA_MAX_AGE);

//        CachingResourceManager cres = new CachingResourceManager(
//                100,
//                transferMinSize /* max size */,
//                dataCache, res, METADATA_MAX_AGE);
//
//        return cres;
    }

    public void setPort(int port) {
        if (this.port != port) {
            this.port = port;
            restart();
        }
    }

    public void setHost(String host) {

        if (!Objects.equal(this.host, host)) {
            this.host = host;
            restart();
        }
    }

    private synchronized void restart() {
        if (port == Integer.MIN_VALUE)
            return;

        String host = this.host;

        if (host == null)
            host = "0.0.0.0"; //any IPv4


        Undertow.Builder b = Undertow.builder();
        b.setServerOption(ENABLE_HTTP2, true);

//        try {
//            SSLContext ssl = SSLContext.getDefault();
//            b.addHttpsListener(port, host, ssl);
//        } catch (NoSuchAlgorithmException e) {
//            logger.error("ssl not available {}", e);
            b.addHttpListener(port, host);
        //}



        if (compression != null)
            b.setHandler(new EncodingHandler(this, compression));

        if (server != null) {
            try {
                logger.error("stop {}", server);
                server.stop();
            } catch (Exception e) {
                logger.error("http stop: {}", e);
                this.server = null;
            }
        }

        try {
            (server = b.build()).start();

            logger.info("start {}", this);


        } catch (Exception e) {
            logger.error("http start: {}", e);
            this.server = null;
        }


    }

    @Override
    public String toString() {
        return server != null ? server.getListenerInfo().toString() : "stopped";
    }

    final ServletContainer container = ServletContainer.Factory.newInstance();


    static DeploymentInfo undertowDeployment(ResteasyDeployment deployment, String mapping) {
        if (mapping == null) {
            mapping = "/";
        }

        if (!mapping.startsWith("/")) {
            mapping = '/' + mapping;
        }

        if (!mapping.endsWith("/")) {
            mapping = mapping + '/';
        }

        mapping = mapping + '*';
        String prefix = null;
        if (!mapping.equals("/*")) {
            prefix = mapping.substring(0, mapping.length() - 2);
        }

        ServletInfo resteasyServlet = Servlets.servlet("ResteasyServlet", HttpServlet30Dispatcher.class).setAsyncSupported(true).setLoadOnStartup(1).addMapping(mapping);
        if (prefix != null) {
            resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);
        }

        return (new DeploymentInfo()).addServletContextAttribute(ResteasyDeployment.class.getName(), deployment).addServlet(resteasyServlet);
    }

    DeploymentInfo undertowDeployment(ResteasyDeployment deployment) {
        return WebServer.undertowDeployment(deployment, "/");
    }

    //        public UndertowJaxrsServer start(Undertow.Builder builder) {
//            this.server = builder.setHandler(this.root).build();
//            this.server.start();
//            return this;
//        }
//
//        public UndertowJaxrsServer start() {
//            this.server = Undertow.builder().addHttpListener(PortProvider.getPort(), "localhost").setHandler(this.root).build();
//            this.server.start();
//            return this;
//        }


//       @Test
//   public void testApplicationPath() throws Exception
//   {
//      server.deploy(MyApp.class);
//      Client client = ClientBuilder.newClient();
//      String val = client.target(TestPortProvider.generateURL("/base/test"))
//                         .request().get(String.class);
//      Assert.assertEquals("hello world", val);
//      client.close();
//   }
//
//   @Test
//   public void testApplicationContext() throws Exception
//   {
//      server.deploy(MyApp.class, "/root");
//      Client client = ClientBuilder.newClient();
//      String val = client.target(TestPortProvider.generateURL("/root/test"))
//                         .request().get(String.class);
//      Assert.assertEquals("hello world", val);
//      client.close();
//   }
//
//   @Test
//   public void testDeploymentInfo() throws Exception
//   {
//      DeploymentInfo di = server.undertowDeployment(MyApp.class);
//      di.setContextPath("/di");
//      di.setDeploymentName("DI");
//      server.deploy(di);
//      Client client = ClientBuilder.newClient();
//      String val = client.target(TestPortProvider.generateURL("/di/base/test"))
//                         .request().get(String.class);
//      Assert.assertEquals("hello world", val);
//      client.close();
//   }
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

