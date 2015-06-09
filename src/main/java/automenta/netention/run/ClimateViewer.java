package automenta.netention.run;


import automenta.netention.data.ClimateViewerSources;
import automenta.netention.web.shell.NARServer;
import com.syncleus.spangraph.InfiniPeer;


public class ClimateViewer {



    public static void main(String[] args) throws Exception {
        InfiniPeer peer = InfiniPeer.local("nars");
        NARServer s = new NARServer(peer, 8080);


//        int webPort = 9090;
//
//        SpacetimeWebServer s = SpacetimeWebServer(
//                //ElasticSpacetime.temporary("cv", -1),
//                ElasticSpacetime.local("cv", "cache", true),
//                "localhost",
//                webPort);

        new ClimateViewerSources(peer);

        s.start();

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
