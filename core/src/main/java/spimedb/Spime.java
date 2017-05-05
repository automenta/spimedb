package spimedb;

import ch.qos.logback.classic.Level;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xnio.Xnio;
import spimedb.server.UDP;
import spimedb.server.WebServer;
import spimedb.util.Crawl;

import java.io.File;
import java.util.Arrays;

/**
 * Spime launcher
 */
public class Spime extends Main {

    static {
        SpimeDB.LOG(Xnio.getInstance().getName(), Level.WARN);
        SpimeDB.LOG( "i18n", Level.WARN);

    }
    public final SpimeDB db;
    private final String dbPathIgnored;


    /** in-memory */
    public Spime() throws Exception {
        this(null);
    }

    /** on-disk */
    public Spime(String path) throws Exception {
        super(path, Maps.mutable.of(
                "http", WebServer.class
        ));

        if (path != null) {
            db = new SpimeDB(path + "/_");
        } else {
            db = new SpimeDB();
        }

        dbPathIgnored = db.file != null ? db.file.getAbsolutePath() : null;

    }

    @NotNull
    @Override
    protected Object[] defaultConstructorArgs(Class[] types) {
        Object[] param;
        if (Arrays.equals(types, spimeDBConstructor)) { //HACK look for spimeDB as the only arg
            param = new Object[]{db};
        } else {
            param = new Object[]{};
        }
        return param;
    }

    @Nullable
    @Override
    protected Pair<Class, String> key(String fileName, String absolutePath) {
        if (absolutePath.startsWith(dbPathIgnored))
            return null; //ignore index directory, subdirectory of the root

        return super.key(fileName, absolutePath);
    }

    @Override
    String workingDirectory() {
        return db.file.toPath().resolve("log").toAbsolutePath().toString();
    }

    @Override
    protected void updateDirectory(File d) {
        super.updateDirectory(d);
        //default: index a directory
        //db.exe.run(0.8f, () -> {
        if (!d.getAbsolutePath().equals(db.indexPath))
            Crawl.fileDirectory(d.getAbsolutePath(), db);
        //});

    }

    @Override
    public Spime restart() {
        super.restart();

        //remove entries for which their source file has been removed

        db.forEach(x -> {
            if (x.get("url_cached") != null) {
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

        return this;
    }

    private static void mainDefault(Spime m) {
        SpimeDB db = m.db;

        int port = 8080;

        WebServer w = new WebServer(db);
        w.setPort(port);
        m.put(WebServer.class, w);


        try {
            m.put(UDP.class, new SpimeDBPeer(port, m.db));
        } catch (Exception e) {
            logger.error("udp {}", e);
            e.printStackTrace();
        }


    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("usage: spime [path]\t\tNo path specified; using default (in-memory) configuration");
            Spime m = new Spime(null);
            mainDefault(m);
            m.restart();
        } else {
            String dataPath = args[0];
            new Spime(dataPath).restart();
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


}
