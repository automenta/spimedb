package spimedb;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import spimedb.io.FileDirectory;
import spimedb.io.Multimedia;
import spimedb.server.WebServer;

import java.io.IOException;

/**
 * Created by me on 6/14/15.
 */

public class Main {

    static {
        SpimeDB.LOG(Logger.ROOT_LOGGER_NAME, Level.INFO);
    }

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.out.println("usage: spime '{...JSON configuration...}'");
            System.out.println("\texample configuration:");
            System.out.println("\t'{port:8080,path:\"/doc\"}'");
            System.out.println();
            return;
        }

        String cfgString = args[0];
        JsonNode config = spimedb.util.JSON.fromJSON(cfgString);
        JsonNode portNode = config.get("port");
        if (portNode==null) {
            System.err.println("configuration missing 'port' field");
            return;
        }
        int port = portNode.intValue();

        JsonNode pathNode = config.get("path");
        if (pathNode==null) {
            System.err.println("configuration missing 'path' field");
            return;
        }

        String path = pathNode.asText();

        SpimeDB db =  /*Infinispan.db(
            //"/tmp/climate"
            null
        );*/ new SpimeDB(path + "/index");


        new Multimedia(db);

        try {
            new WebServer(db, port);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }

//        Phex p = Phex.the();
//        p.start();
//        p.startSearch("kml");


        FileDirectory.load(path, db);

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

        //db.find("s*", 32).docs().forEachRemaining(d -> {
//        db.forEach((n) -> {
//            System.out.println(n);
//
//            Document dd = db.the(n.id());
//            System.out.println("\t" + dd);
//        });
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
