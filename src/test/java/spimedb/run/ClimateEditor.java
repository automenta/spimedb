package spimedb.run;

import io.baratine.service.Service;
import io.baratine.web.Get;
import io.baratine.web.RequestWeb;
import io.baratine.web.Web;
import spimedb.SpimeScript;
import spimedb.index.graph.SpimeGraph;
import spimedb.sense.ImportKML;
import spimedb.web.AbstractWeb;
import spimedb.web.MousePointer;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;

/**
 * Created by me on 6/14/15.
 */
@Service
public class ClimateEditor extends AbstractWeb {
    //InfinispanSpimeBase db = InfinispanSpimeBase.disk("/tmp/sf", 128 * 1024);
    final SpimeGraph db = new SpimeGraph();

    public ClimateEditor() {

        if (db.isEmpty()) {
            System.out.println("Initializing database...");

            try {
                new SpimeScript(db).run(new File("data/climateviewer2.js"));
            } catch (Exception e) {
                e.printStackTrace();
            }


            String[] urls = new String[]{
                    //"file:///home/me/kml/EOL-Field-Projects-CV3D.kmz",
                    //"file:///home/me/kml/GVPWorldVolcanoes-List.kmz",
                    "file:///home/me/kml/submarine-cables-CV3D.kmz",
                    // http://climateviewer.org/layers/kml/3rdparty/places/submarine-cables-CV3D.kmz
                    //"file:///home/me/kml/fusion-landing-points-CV3D.kmz",
                    //"file:///home/me/kml/CV-Reports-October-2014-Climate-Viewer-3D.kmz"
            };

            for (String u : urls) {
                try {
                    new ImportKML(db).url(u).run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            db.forEach(x -> {
                System.out.println(x);
            });
        }





    }

    @Get("/hello")
    public void doHello(RequestWeb request) {
        request.ok(new MousePointer(2,2));
    }

    public static void main(String[] args) throws IOException, ScriptException {

        Web.include(ClimateEditor.class);
        Web.go(args);
//        new SpimeServer(db)
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
