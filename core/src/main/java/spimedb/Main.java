package spimedb;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import com.google.common.io.Files;
import jcog.Texts;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.internal.util.reflection.BeanPropertySetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spimedb.server.WebServer;
import spimedb.util.Crawl;
import spimedb.util.Locker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 * Created by me on 6/14/15.
 */

public class Main extends FileAlterationListenerAdaptor {

    public final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static final ch.qos.logback.classic.Logger LOG;


    static {

        Thread.currentThread().setName("$");

        //http://logback.qos.ch/manual/layouts.html

        LOG = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        LoggerContext loggerContext = LOG.getLoggerContext();
        loggerContext.reset();


        SpimeDB.LOG(Logger.ROOT_LOGGER_NAME, Level.DEBUG);

    }

    private final SpimeDB db;
    final String dbPathIgnored;

    /**
     * live objects
     */
    final Map<Pair<Class, String>, Object> obj = new ConcurrentHashMap();

    final Map<String, Class> klassPath = new ConcurrentHashMap<>();

    @Nullable
    private final FileAlterationObserver fsObserver;

    final Pair<Class, String> key(File f) {

        String fileName = f.getName();
        if (fileName.startsWith("."))
            return null; //ignore hidden files

        String absolutePath = f.getAbsolutePath();

        if (absolutePath.startsWith(dbPathIgnored))
            return null; //ignore index directory, subdirectory of the root


        String[] parts = fileName.split(".");

        String id;
        Class klass;
        if (parts.length < 1) {
            /** if only one filename component:
             *       if this string resolves in the klasspath, then that's what it is and also its name
             *       else default to 'String'
             */
            Class specificclass = klassPath.get(fileName);
            if (specificclass == null) {
                id = fileName;
                klass = String.class;
            } else {
                klass = specificclass;
                id = ""; //designated for the singleton
            }
        } else if (parts.length >= 2) {
            String classSting = parts[0];
            klass = klass(classSting);
            id = parts[1];
        } else {
            return null;
        }

        return key(klass, id);

    }

    public static Pair<Class, String> key(Class klass) {
        return key(klass, "");
    }

    @NotNull
    public static Pair<Class, String> key(Class klass, String id) {
        return Tuples.pair(klass, id);
    }

    public Class klass(String klass) {
        return klassPath.get(klass);
    }

    @Override
    public void onFileCreate(File file) {
        update(file);
    }

    @Override
    public void onFileChange(File file) {
        update(file);
    }

    private void update(File file) {

        Pair<Class, String> k = key(file);
        if (k == null)
            return;

        logger.info("reload file://{}", file);
        merge(k, build(k, file));
    }

    @Override
    public void onFileDelete(File file) {
        Pair<Class, String> k = key(file);
        if (k == null)
            return;

        remove(k);
    }


    final Locker<Pair<Class, String>> locker = new Locker();

    private Object merge(Pair<Class, String> k, Function build) {
        Lock l = locker.get(k);
        l.lock();
        try {
            if (build == null) {
                Object v = obj.remove(k);
                logger.info("remove {}: {}", k, v);
                return v;
            } else {
                return obj.compute(k, (kk, existing) -> build.apply(existing));
            }
        } finally {
            l.unlock();
        }
    }

    /**
     * the function returned will accept the previous value (null if there was none) and return a result
     *
     * @param file
     * @param klass
     */
    @NotNull
    private Function build(Pair<Class, String> id, File file) {
        String[] parts = file.getName().split(".");
        switch (parts.length) {
            case 0:
            case 1:
                //string
                if (id.getOne() == String.class) {
                    return new FileToString(file);
                } else {
                    return buildDefault(id, file);
                }
            case 2:
                //properties file
                return buildDefault(id, file);


            case 3:
                switch (parts[3]) {
                    case "js": //javascript
                        break;
                    case "json":
                        break;
                    case "xml":
                        break;
                    case "java": //dynamically recompiled java
                        break;
                }
                break;
        }

        return (x) -> {
            logger.info("unbuildable: {}", file);
            return x;
        };
    }

    @NotNull
    private Function buildDefault(Pair<Class, String> id, File file) {
        if (id.getOne() == LogConfigurator.class) {
            return (Object x) -> {
                LogConfigurator newConfig = new LogConfigurator(file);


                if (x instanceof LogConfigurator) {
                    LogConfigurator old = (LogConfigurator) x;
                    LoggingLogger.info("stop {}", old.message);
                    old.stop();
                }

                LoggingLogger.info("start {}", newConfig.message);
                return newConfig;
            };
        } else {
            return new PropertyBuilder(id, file);
        }
    }


    static final Class[] spimeDBConstructor = new Class[]{SpimeDB.class};

    public class PropertyBuilder<X> implements Function<X, X> {
        public final File file;
        private final Pair<Class, String> id;


        public PropertyBuilder(Pair<Class, String> id, File file) {
            this.id = id;
            this.file = file;
        }

        @Override
        public X apply(X x) {
            Class cl = id.getOne();

            if (x == null) {

                //HACK TODO use some adaptive constructor argument injector
                for (Constructor c : cl.getConstructors()) {
                    Class[] types = c.getParameterTypes();
                    Object[] param;
                    if (Arrays.equals(types, spimeDBConstructor)) { //HACK look for spimeDB as the only arg
                        param = new Object[]{db};
                    } else {
                        param = new Object[] {};
                    }

                    try {
                        x = (X) c.newInstance(param);
                    } catch (Exception e) {
                        logger.error("error instantiating {} {} {} {}", file, cl, c, e.getMessage());
                        return null;
                    }
                }

            }

            Properties p = new Properties();
            try {
                p.load(new FileInputStream(file));
            } catch (IOException e) {
                logger.error("properties configure {} {} {}", file, cl, e.getMessage());
            }

            for (Map.Entry e : p.entrySet()) {
                Object k = e.getKey().toString();
                Object v = e.getValue();

                String field = k.toString();
                Field f = field(cl, field);
                if (f != null) {
                    //TODO better type decoding
                    BeanPropertySetter s = new BeanPropertySetter(x, f);
                    try {
                        switch (f.getType().toString()) {
                            case "float":
                                v = Texts.f(v.toString());
                                break;
                            case "int":
                                v = Texts.i(v.toString());
                                break;
                        }
                        logger.info("{}.{}={}", x, field, v);
                        s.set(v);
                    } catch (IllegalArgumentException aa) {
                        logger.info("invalid type {} for {}", v.getClass(), f);
                    }
                } else {
                    logger.info("unknown field {} {}", file, field);
                }
            }

            return x;
        }
    }

    private static Field field(Class cl, String field) {
        //TODO add a reflection cache
        try {
            return cl.getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public <X> Object put(Class k, X v) {
        return put(key(k), v);
    }

    public <X> Object put(Pair<Class, String> k, X v) {
        if (v == null) {
            return remove(k);
        }

        Object existing = obj.put(k, v);
        if (existing != v) {
            logger.info("{} = {}", k, v);
            if (existing != null) {
                //logger.info("{} = {}", k, v);
            } else {
                //logger.info("{} = {} <== {}", k, v, existing);
                onRemove(k, existing);
            }
            onAdd(k, existing);
        }
        return existing;
    }

    protected void onAdd(Pair<Class, String> k, Object v) {

    }

    protected void onRemove(Pair<Class, String> k, Object v) {

    }

    public <X> X get(Pair<Class<? extends X>, String> k) {
        if (k == null)
            return null;
        return (X) obj.get(k);
    }

    public Object remove(Pair<Class, String> k) {
        return merge(k, null);
    }


    public final static Logger LoggingLogger = LoggerFactory.getLogger(LOG.getClass());

    class LogConfigurator {


        Appender appender;
        String message;

        public LogConfigurator(File f) {

            Appender appender = null;

            if (f != null) {
                try {
                    String line = Files.readFirstLine(f, Charset.defaultCharset()).trim();
                    switch (line) {
                        case "rolling":
                            String logFile = db.file.toPath().resolve("log").toAbsolutePath().toString();
                            message = ("rolling to file://{}" + logFile);

                            RollingFileAppender r = new RollingFileAppender();
                            r.setFile(logFile);
                            r.setAppend(true);

                            SizeBasedTriggeringPolicy triggeringPolicy = new ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy();
                            triggeringPolicy.setMaxFileSize(FileSize.valueOf("5MB"));
                            triggeringPolicy.start();

                            FixedWindowRollingPolicy policy = new FixedWindowRollingPolicy();
                            policy.setFileNamePattern("log.%i.gz");
                            policy.setParent(r);
                            policy.setContext(LOG.getLoggerContext());
                            policy.start();

                            r.setEncoder(logEncoder());
                            r.setRollingPolicy(policy);
                            r.setTriggeringPolicy(triggeringPolicy);


                            appender = r;
                            break;
                        //TODO TCP/UDP etc
                    }
                } catch (IOException e) {
                    logger.error("{}", e);
                    appender = null;
                }
            }

            if (appender == null) {
                //default
                ConsoleAppender a = new ConsoleAppender();
                a.setEncoder(logEncoder());
                appender = a;
                message = "ConsoleAppender";
            }


            LOG.detachAndStopAllAppenders();

            appender.setContext(LOG.getLoggerContext());
            appender.start();

            LOG.addAppender(appender);


            this.appender = appender;
        }

        private Encoder logEncoder() {
            PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
            logEncoder.setContext(LOG.getLoggerContext());
            logEncoder.setPattern("%highlight(%logger{0}) %green(%thread) %message%n");
            logEncoder.setImmediateFlush(true);
            logEncoder.start();

            return logEncoder;
        }

        public void stop() {
            if (appender != null) {

                appender.stop();
                appender = null;
            }
        }
    }


    public Main(String path) throws Exception {



        //setup default klasspath
        klassPath.put("http", WebServer.class);
        klassPath.put("log", LogConfigurator.class);
        klassPath.put("udp", UDP.class);
        //klassPath.put("crawl", Crawl.class);

        put(LogConfigurator.class, new LogConfigurator(null));


        if (path!=null) {
            db = new SpimeDB(path + "/_");
        } else {
            db = new SpimeDB();
        }

        dbPathIgnored = db.file!=null ? db.file.getAbsolutePath() : null;

        //new Multimedia(db);

        if (path!=null) {
            fsObserver = new FileAlterationObserver(path);
            logger.info("watching: file://{}", path);
        /* http://www.baeldung.com/java-watchservice-vs-apache-commons-io-monitor-library */
            int updatePeriodMS = 200;
            FileAlterationMonitor monitor = new FileAlterationMonitor(updatePeriodMS);

            //monitor.setThreadFactory(Executors.defaultThreadFactory());

            fsObserver.addListener(this);
            monitor.addObserver(fsObserver);
            monitor.start();

            //load existing files
            rebuild();
        } else {
            fsObserver = null;
        }

//            @Override
//            public synchronized void clear(boolean rebuild) {
//                super.clear(rebuild);
//                System.exit(2);
//            }
//        };


    }

    static class UDP {

        private final SpimeDB db;
        int port;

        private Peer peer = null;

        public UDP(SpimeDB db) {
            this.db = db;
        }


        public void setPort(int port) {
            int p = this.port;
            if (p == port)
                return;
            synchronized (this) {
                this.port = port;
                if (port==-1)
                    return;
                if (this.peer != null) {
                    this.peer.stop();
                }
                try {
                    this.peer = new Peer(port, db);
                } catch (Exception e) {
                    logger.error("{}", e);
                }
            }


        }

        public int getPort() {
            return port;
        }
    }

    protected void rebuild() {
        if (fsObserver!=null)
            reload(fsObserver);
        clean();
    }

    /** remove entries for which their source file has been removed */
    private void clean() {
        db.forEach(x -> {
           if (x.get("url_cached")!=null) {
               String f = x.get("url_in");
               boolean valid = false;
               if (f.startsWith("file:")) {
                   //try {

                   valid = new File(f.substring(5)).exists();
               } else {
                   //TODO handle remote URI's in a different way than local files
                   valid = true;
               }
               //} catch (URISyntaxException e) {

               //}
               if (!valid) {
                   db.remove(x.id());
               }
           }
        });
    }
    private void reload(FileAlterationObserver observer) {
        for (File f : observer.getDirectory().listFiles()) {
            if (f.getName().startsWith("."))
                continue; //ignore hidden files

            db.exe.run(0.9f, () -> {
                if (f.isFile()) {
                    update(f);
                } else if (f.isDirectory() && !f.getAbsolutePath().equals(db.indexPath)) {
                    //default: index a directory
                    db.exe.run(0.8f, () -> {
                        Crawl.fileDirectory(f.getAbsolutePath(), db);
                    });
                }
            });
        }
    }

    public void put(Class c, String id, Object value) {
        put(key(c, id), value);
    }


    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("usage: spime [path]\t\tNo path specified; using default (in-memory) configuration");
            Main m = new Main(null);
            mainDefault(m);
        } else {
            String dataPath = args[0];
            new Main(dataPath);
        }


//        Phex p = Phex.the();
//        p.start();
//        p.startSearch("kml");

        //Crawl.pageLinks("...", (x) -> x.endsWith(".pdf"), db);

        //Crawl.fileDirectory(path, db);


        /*
        try {
            new SpimeJS(db).with("db", db).run(new File("data/climateviewer.js"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        */


            /*
            new Netention() {

                @Override
                protected void onTag(String id, String name, List<String> extend) {
                    NObject n = new NObject(id, name);
                    if (extend!=null && !extend.isEmpty())
                        n.setTag(extend.toArray(new String[extend.size()]));
                    db.put(n);
                }
            };
            */


        //ImportGeoJSON

        //ImportSchemaOrg.load(db);


        //db.add(GeoJSON.get(eqGeoJson.get(), GeoJSON.baseGeoJSONBuilder));


//            db.forEach(x -> {
//                System.out.println(x);
//            });


    }

    private static void mainDefault(Main m) {
        SpimeDB db = m.db;

        int port = 8080;

        WebServer w = new WebServer(db);
        w.setPort(port);
        m.put(WebServer.class, w);

        try {
            new Peer(port, m.db);
        } catch (Exception e) {
            logger.error("starting Peer: {}", e);
            e.printStackTrace();
        }


    }

    private class FileToString implements Function {

        private final File file;

        public FileToString(File file) {
            this.file = file;
        }

        @Override
        public Object apply(Object o) {
            try {
                return Files.toString(file, Charset.defaultCharset());
            } catch (IOException e) {
                logger.error("reading string: {}", file);
                return null;
            }
        }
    }


//    final static String cachePath = "cache";
//
//    public static void main(String[] args) throws Exception {
//
//        /*final static String eq4_5week = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.atom";
//
//        public USGSEarthquakes() {
//            super("USGSEarthquakes", eq4_5week, 128);*/
//
//
//        //new IRCBot(s.db, "RAWinput", "irc.freenode.net", "#netention");
//
//
//        HttpCache httpCache = new HttpCache(cachePath);
//
//
//
//        //Self s = new Self();
//
//        Web j = new Web()
//                .add("/wikipedia", new Wikipedia(httpCache))
////                .add("/api/tag", new PathHandler() {
////
////                    @Override
////                    public void handleRequest(HttpServerExchange exchange) throws Exception {
////
////                        ArrayList<Object> av = Lists.newArrayList(s.allValues());
////                        byte[] b = Core.jsonAnnotated.writeValueAsBytes(av);
////                        Web.send(b, exchange, "application/json" );
////                    }
////                })
//                .add("/", ClientResources.handleClientResources())
//                .start("localhost", 8080);
//
//
//
//        SchemaOrg.load(null);
////        logger.info("Loading ClimateViewer (ontology)");
////        new ClimateViewer(s.db);
////        logger.info("Loading Netention (ontology)");
////        NOntology.load(s.db);
//
//        //InfiniPeer.local("i", cachePath, 32000);
//        new ClimateViewerSources() {
//
//            @Override
//            public void onLayer(String id, String name, String kml, String icon, String currentSection) {
//                URLSensor r = new URLSensor(currentSection + "/" + id, name, kml, icon);
//                //p.add(r);
//            }
//
//            @Override
//            public void onSection(String name, String id, String icon) {
//
//            }
//        };
//
//
////        int webPort = 9090;
////
////        SpacetimeWebServer s = SpacetimeWebServer(
////                //ElasticSpacetime.temporary("cv", -1),
////                ElasticSpacetime.local("cv", "cache", true),
////                "localhost",
////                webPort);
//
////        /*
////        //EXAMPLES
////        {
//////            s.add("irc",
//////                    new IRCBot(s.db, "RAWinput", "irc.freenode.net", "#netention", "#nars"
//////                            /*"#archlinux", "#jquery"*/).serverChannel
//////            );
////
////            s.add("eq", new USGSEarthquakes());
////
////
////            //new IRCBot(s.db, "RAWinput", "irc.freenode.net", "#netention");
////            //new FileTailWindow(s.db, "netlog", "/home/me/.xchat2/scrollback/FreeNode/#netention.txt").start();
////
////            s.add("demo", new SimulationDemo.SimpleSimulation("DroneSquad3"));
////            /*s.addPrefixPath("/sim", new WebSocketCore(
////                    new SimpleSimulation("x")
////            ).handler());*/
////        }
////        */
//    }
//
//

}
