package automenta.netention.run;


import automenta.netention.data.ClimateViewerSources;
import automenta.netention.net.proxy.CachingProxyServer;
import automenta.netention.net.proxy.URLSensor;
import automenta.netention.web.Web;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.MimeMappings;

import java.io.File;

import static io.undertow.Handlers.resource;


public class ClimateViewerProxy {


    final static String cachePath = "cache";

    public static void main(String[] args) throws Exception {

        /*final static String eq4_5week = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.atom";

        public USGSEarthquakes() {
            super("USGSEarthquakes", eq4_5week, 128);*/


        //new IRCBot(s.db, "RAWinput", "irc.freenode.net", "#netention");


        Web j = new Web()
            //.add("/", ClientResources.handleClientResources())
            .add("/proxy", new CachingProxyServer(cachePath))
            .add("/cache", resource(
                    new FileResourceManager(new File(cachePath), 64))
                    .setDirectoryListingEnabled(true).setMimeMappings(MimeMappings.DEFAULT))
            .start("localhost", 8080);


        //InfiniPeer.local("i", cachePath, 32000);
        new ClimateViewerSources() {

            @Override
            public void onLayer(String id, String name, String kml, String icon, String currentSection) {
                URLSensor r = new URLSensor(currentSection + "/" + id, name, kml, icon);
                //p.add(r);
            }

            @Override
            public void onSection(String name, String id, String icon) {

            }
        };


//        int webPort = 9090;
//
//        SpacetimeWebServer s = SpacetimeWebServer(
//                //ElasticSpacetime.temporary("cv", -1),
//                ElasticSpacetime.local("cv", "cache", true),
//                "localhost",
//                webPort);

//        /*
//        //EXAMPLES
//        {
////            s.add("irc",
////                    new IRCBot(s.db, "RAWinput", "irc.freenode.net", "#netention", "#nars"
////                            /*"#archlinux", "#jquery"*/).serverChannel
////            );
//
//            s.add("eq", new USGSEarthquakes());
//
//
//            //new IRCBot(s.db, "RAWinput", "irc.freenode.net", "#netention");
//            //new FileTailWindow(s.db, "netlog", "/home/me/.xchat2/scrollback/FreeNode/#netention.txt").start();
//
//            s.add("demo", new SimulationDemo.SimpleSimulation("DroneSquad3"));
//            /*s.addPrefixPath("/sim", new WebSocketCore(
//                    new SimpleSimulation("x")
//            ).handler());*/
//        }
//        */
    }


//
//    @Path("/api")
//    public static class API {
//        @GET @Produces("text/json")
//        public Object get() {
//            try {
//                List<RESTEndpoints.Endpoint> x = RESTEndpoints.restEndpoints(ClimateViewerProxy.class);
//                return x;
//            } catch (Exception e) {
//                return e.toString();
//            }
//
//        }
//    }
//
//    @Path("/version")
//    public static class Version {
//        @GET
//        @Produces("text/plain")
//        public String get() {
//            return "1.0";
//        }
//    }
//
//    @Path("/index")
//    public static class Index {
//        @GET @Produces("text/json")
//        public NObject[] get() {
//            return new NObject[] {
//                    new NObject(null, "abc"), new NObject(null, "def")
//            };
//        }
//    }



}
