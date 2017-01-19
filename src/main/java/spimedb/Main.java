package spimedb;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.db.Infinispan;
import spimedb.sense.KML;
import spimedb.server.WebServer;
import spimedb.util.js.SpimeJS;

import java.io.File;

/**
 * Created by me on 6/14/15.
 */

public class Main {

    static {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    public static void main(String[] args)  {

        SpimeDB db =  Infinispan.get(
            //"/tmp/climate"
            null
        );

        try {
            new WebServer(db, 8080);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }

        if (db.isEmpty())
            addInitialData(db);

    }

    static void addInitialData(SpimeDB db) {

        SpimeDB.logger.info("Initializing database...");

        try {
            new SpimeJS(db).with("db", db).run(new File("data/climateviewer.js"));
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        //System.out.println(db.tag.nodes().size() + " nodes, " + db.tag.edges().size() + " edges");


        String[] urls = new String[]{
                "file:///home/me/kml/Indian-Lands.kmz",
                "file:///home/me/kml/Ten-Most-Radioactive-Locations-On-Earth-CV3D.kmz",
                "file:///home/me/kml/Restored-Renewable-Recreational-and-Residential-Toxic-Trash-Dumps.kml",
                "file:///home/me/kml/submarine-cables-CV3D.kmz",
                "file:///home/me/kml/DHS-Fusion-Centers-CV3D.kmz"
//
//                    //"file:///home/me/kml/EOL-Field-Projects-CV3D.kmz",
//                    //"file:///home/me/kml/GVPWorldVolcanoes-List.kmz",
//                    //"file:///home/me/kml/fusion-landing-points-CV3D.kmz",
//                    //"file:///home/me/kml/CV-Reports-October-2014-Climate-Viewer-3D.kmz"
        };
        for (String u : urls) {
            SpimeDB.runLater(new KML(db).url(u));
        }


//            db.forEach(x -> {
//                System.out.println(x);
//            });
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
