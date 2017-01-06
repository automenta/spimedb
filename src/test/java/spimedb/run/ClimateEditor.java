package spimedb.run;

import spimedb.SpimeDB;
import spimedb.db.InfiniSpimeDB;
import spimedb.util.js.SpimeScript;
import spimedb.web.WebServer;

import java.io.File;

/**
 * Created by me on 6/14/15.
 */

public class ClimateEditor  {


    static {
        //HACK force programmatic logger settings
        try {
            Class.forName("spimedb.NObject");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ClimateEditor(SpimeDB db) {

        if (db.isEmpty()) {
            System.out.println("Initializing database...");

            try {
                new SpimeScript(db).run(new File("data/climateviewer.js"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            //ImportGeoJSON
            //ImportSchemaOrg.load(db);

            //System.out.println(db.tag.nodes().size() + " nodes, " + db.tag.edges().size() + " edges");



//            String[] urls = new String[]{
//                    "file:///home/me/kml/Indian-Lands.kmz",
//                    "file:///home/me/kml/Ten-Most-Radioactive-Locations-On-Earth-CV3D.kmz",
//                    "file:///home/me/kml/Restored-Renewable-Recreational-and-Residential-Toxic-Trash-Dumps.kml",
//                    "file:///home/me/kml/submarine-cables-CV3D.kmz",
//                    "file:///home/me/kml/DHS-Fusion-Centers-CV3D.kmz"
//
//                    //"file:///home/me/kml/EOL-Field-Projects-CV3D.kmz",
//                    //"file:///home/me/kml/GVPWorldVolcanoes-List.kmz",
//                    // http://climateviewer.org/layers/kml/3rdparty/places/submarine-cables-CV3D.kmz
//                    //"file:///home/me/kml/fusion-landing-points-CV3D.kmz",
//                    //"file:///home/me/kml/CV-Reports-October-2014-Climate-Viewer-3D.kmz"
//            };
//            Stream.of(urls).parallel().forEach(u -> new ImportKML(db).url(u).run());


//            db.forEach(x -> {
//                System.out.println(x);
//            });

//            Iterable<NObject> r = db.intersecting(40.44, -79.99, 420000, -1);
//            System.out.println(Joiner.on("\n").join(r));

        }


    }


    public static void main(String[] args) {

        SpimeDB db =  InfiniSpimeDB.get(
                //"/tmp/climate"
                null
        );

        new ClimateEditor(db);



        new WebServer(db, 8080);
//
//                //.add("/proxy", new CachingProxyServer(es, cachePath))
//                .add("/cache", resource(
//                        new FileResourceManager(new File(cachePath), 64))
//                        .setDirectoryListingEnabled(true).setMimeMappings(MimeMappings.DEFAULT))
//                .start("localhost", 8080);
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
