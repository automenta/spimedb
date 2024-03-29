package spimedb;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import com.google.common.io.Files;
import jcog.Str;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.util.Locker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * "Main.java" - a file-system driven, dynamically reconfiguring, dependency-injection container
 * designed to make DI as fun as mainlining an endless supply of ___
 */
public abstract class Main extends FileAlterationListenerAdaptor {

    public final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static final ch.qos.logback.classic.Logger LOG;

    final int fileObservePeriod = 500;

    static {

        Thread.currentThread().setName("$");

        //http://logback.qos.ch/manual/layouts.html

        LOG = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
//        LoggerContext loggerContext = LOG.getLoggerContext();
//        loggerContext.reset();


    }


    /**
     * live objects
     */
    final Map<Pair<Class, String>, Object> obj = new ConcurrentHashMap();

    final Map<String, Class> klassPath = new ConcurrentHashMap<>();

    @Nullable
    private FileAlterationObserver fsObserver;
    public final File path;
    private FileAlterationMonitor monitor;

    @Nullable
    protected Pair<Class, String> key(File f) {

        String fileName = f.getName();
        if (fileName.startsWith("."))
            return null; //ignore hidden files

        String absolutePath = f.getAbsolutePath();
        return key(fileName, absolutePath);

    }

    @Nullable
    protected Pair<Class, String> key(String fileName, String absolutePath) {


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
        updateFile(file);
    }

    @Override
    public void onFileChange(File file) {
        updateFile(file);
    }

    protected void updateFile(File file) {

        Pair<Class, String> k = key(file);
        if (k == null)
            return;

        //logger.info("reload file://{}", file);

        merge(k, build(k, file));
    }

    protected void updateDirectory(File d) {
        //impl in subclasses
    }

    @Override
    public void onFileDelete(File file) {
        Pair<Class, String> k = key(file);
        if (k == null)
            return;

        remove(k);
    }


    static final Locker<Pair<Class, String>> locker = new Locker();

    private Object merge(Pair<Class, String> kk, Function build) {
        return locker.locked(kk, (k) -> {
            if (build == null) {
                Object v = obj.remove(k);
                logger.info("remove {}: {}", k, v);
                return v;
            } else {
                return obj.compute(k, (kkk, existing) -> build.apply(existing));
            }
        });
    }

    /**
     * the function returned will accept the previous value (null if there was none) and return a result
     *
     * @param file
     */
    @NotNull
    private Function build(Pair<Class, String> id, File file) {
        String[] parts = file.getName().split(".");
        switch (parts.length) {
            case 0, 1 -> {
                //string
                if (id.getOne() == String.class) {
                    return new FileToString(file);
                } else {
                    return buildDefault(id, file);
                }
            }
            case 2 -> {
                //properties file
                return buildDefault(id, file);
            }
            case 3 -> {
                switch (parts[3]) {
                    case "js": //javascript
                        break;
                    case "json", "xml":
                        break;
                    case "java": //dynamically recompiled java
                        break;
                }
            }
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


                if (x instanceof LogConfigurator old) {
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

    public <X extends Plugin> Main with(Class<X> c) {
        //TODO
        return this;
    }

    /**
     * from: mockito
     */
    public static class AccessibilityChanger {

        private Boolean wasAccessible = null;

        /**
         * safely disables access
         */
        public void safelyDisableAccess(AccessibleObject accessibleObject) {
            assert wasAccessible != null : "accessibility info shall not be null";
            try {
                accessibleObject.setAccessible(wasAccessible);
            } catch (Throwable t) {
                //ignore
            }
        }

        /**
         * changes the accessibleObject accessibility and returns true if accessibility was changed
         */
        public void enableAccess(AccessibleObject accessibleObject) {
            wasAccessible = accessibleObject.isAccessible();
            accessibleObject.setAccessible(true);
        }
    }

    /**
     * This utility class will call the setter of the property to inject a new value.
     * from: mockito
     */
    public static class BeanPropertySetter {

        private static final String SET_PREFIX = "set";

        private final Object target;
        private final boolean reportNoSetterFound;
        private final Field field;

        /**
         * New BeanPropertySetter
         *
         * @param target              The target on which the setter must be invoked
         * @param propertyField       The field that should be accessed with the setter
         * @param reportNoSetterFound Allow the set method to raise an Exception if the setter cannot be found
         */
        public BeanPropertySetter(final Object target, final Field propertyField, boolean reportNoSetterFound) {
            this.field = propertyField;
            this.target = target;
            this.reportNoSetterFound = reportNoSetterFound;
        }

        /**
         * New BeanPropertySetter that don't report failure
         *
         * @param target        The target on which the setter must be invoked
         * @param propertyField The propertyField that must be accessed through a setter
         */
        public BeanPropertySetter(final Object target, final Field propertyField) {
            this(target, propertyField, false);
        }

        /**
         * Set the value to the property represented by this {@link org.mockito.internal.util.reflection.BeanPropertySetter}
         *
         * @param value the new value to pass to the property setter
         * @return <code>true</code> if the value has been injected, <code>false</code> otherwise
         * @throws RuntimeException Can be thrown if the setter threw an exception, if the setter is not accessible
         *                          or, if <code>reportNoSetterFound</code> and setter could not be found.
         */
        public boolean set(final Object value) {

            AccessibilityChanger changer = new AccessibilityChanger();
            Method writeMethod = null;
            try {
                writeMethod = target.getClass().getMethod(setterName(field.getName()), field.getType());

                changer.enableAccess(writeMethod);
                writeMethod.invoke(target, value);
                return true;
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Setter '" + writeMethod + "' of '" + target + "' with value '" + value + "' threw exception : '" + e.getTargetException() + "'", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access not authorized on field '" + field + "' of object '" + target + "' with value: '" + value + "'", e);
            } catch (NoSuchMethodException e) {
                reportNoSetterFound();
            } finally {
                if (writeMethod != null) {
                    changer.safelyDisableAccess(writeMethod);
                }
            }

            reportNoSetterFound();
            return false;
        }

        /**
         * Retrieve the setter name from the field name.
         * <p>
         * <p>Implementation is based on the code of {@link java.beans.Introspector}.</p>
         *
         * @param fieldName the Field name
         * @return Setter name.
         */
        private String setterName(String fieldName) {
            return SET_PREFIX +
                    fieldName.substring(0, 1).toUpperCase(Locale.ENGLISH) +
                    fieldName.substring(1);
        }

        private void reportNoSetterFound() {
            if (reportNoSetterFound) {
                throw new RuntimeException("Problems setting value on object: [" + target + "] for property : [" + field.getName() + "], setter not found");
            }
        }

    }

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
                    Object[] param = defaultConstructorArgs(types);

                    try {
                        logger.info("start {}", x);
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
                            case "float" -> v = Str.f(v.toString());
                            case "int" -> v = Str.i(v.toString());
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

    /**
     * HACK TODO make not required
     */
    @NotNull
    abstract protected Object[] defaultConstructorArgs(Class[] types);

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

    public <X> X get(Class<? extends X> k) {
        return (X) obj.get(key(k));
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

    /**
     * HACK TODO make not a requirement
     */
    abstract String workingDirectory();

    class LogConfigurator {


        Appender appender;
        String message;

        public LogConfigurator(File f) {

            Appender appender = null;

            if (f != null) {
                try {
                    String line = Files.readFirstLine(f, Charset.defaultCharset()).trim();
                    if (line.equals("rolling")) {
                        String logFile = workingDirectory();
                        message = ("rolling to file://{}" + logFile);

                        RollingFileAppender r = new RollingFileAppender();
                        r.setFile(logFile);
                        r.setAppend(true);

                        SizeBasedTriggeringPolicy triggeringPolicy = new SizeBasedTriggeringPolicy();
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


    @SafeVarargs
    public Main(@Nullable String path, Map<String, Class> initialKlassPath, Class<? extends Plugin>... plugin) {
        this(path != null ? new File(path) : null, initialKlassPath, plugin);
    }

    @SafeVarargs
    public Main(@Nullable File path, Map<String, Class> initialKlassPath, Class<? extends Plugin>... plugin) {

        if (path != null)
            path = path.getAbsoluteFile();

        this.path = path;

        klassPath.putAll(initialKlassPath);

        //setup default klasspath
        klassPath.putIfAbsent("log", LogConfigurator.class);

        put(LogConfigurator.class, new LogConfigurator(null));

//        Set<Class<? extends Plugin>> plugins = new Reflections(Main.class.getClassLoader())
//                .getSubTypesOf(Plugin.class);

        for (Class c : plugin) {
            String id = c.getSimpleName().toLowerCase();
            logger.warn("Plugin available: {}", id);
            klassPath.put(id, c);
        }


        if (path != null) {
            fsObserver = new FileAlterationObserver(path);
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

    /**
     * reload files
     */
    protected synchronized Main restart() {
        /* http://www.baeldung.com/java-watchservice-vs-apache-commons-io-monitor-library */
        if (fsObserver != null && this.monitor == null) {

            fsObserver.addListener(this);

            this.monitor = new FileAlterationMonitor(fileObservePeriod);

            monitor.addObserver(fsObserver);

            try {
                monitor.start();
                logger.info("watching file://{}", path);

                reload(fsObserver);

            } catch (Exception e) {
                logger.error("file observe {}", e);
                this.fsObserver = null;
            }

        }
        return this;
    }


    private void reload(FileAlterationObserver observer) {
        for (File f : observer.getDirectory().listFiles()) {

            if (f.getName().startsWith("."))
                continue; //ignore hidden files


            //exe.submit(0.9f, () -> {

            logger.debug("reload {}", f);


            if (f.isFile()) {
                updateFile(f);
            } else if (f.isDirectory()) {
                updateDirectory(f);
            }

            //});
        }
    }


//    public void put(Class c, String id, Object value) {
//        put(key(c, id), value);
//    }


    private record FileToString(File file) implements Function {

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
