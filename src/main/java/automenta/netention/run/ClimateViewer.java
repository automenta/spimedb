package automenta.netention.run;


import automenta.netention.data.ClimateViewerSources;
import automenta.netention.net.proxy.InfiniProxy;
import automenta.netention.net.proxy.URLSensor;
import automenta.netention.web.ClientResources;
import com.syncleus.spangraph.InfiniPeer;


public class ClimateViewer {



    public static void main(String[] args) throws Exception {
        InfiniProxy p = new InfiniProxy("CVgeo", InfiniPeer.local(), 8080, "cache");

        p.addPrefixPath("/", ClientResources.handleClientResources());


        /*final static String eq4_5week = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.atom";

        public USGSEarthquakes() {
            super("USGSEarthquakes", eq4_5week, 128);*/


        //new IRCBot(s.db, "RAWinput", "irc.freenode.net", "#netention");

        new ClimateViewerSources() {

            @Override
            public void onLayer(String id, String name, String kml, String icon, String currentSection) {
                URLSensor r = new URLSensor(currentSection + "/" + id, name, kml, icon);
                p.add(r);
            }

            @Override
            public void onSection(String name, String id, String icon) {

            }
        };

        p.run();


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


}
