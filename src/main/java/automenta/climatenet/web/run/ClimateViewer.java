//package automenta.climatenet.web.run.run;
//
//import automenta.climatenet.data.elastic.ElasticSpacetime;
//import automenta.climatenet.data.geo.USGSEarthquakes;
//
//
//public class ClimateViewer {
//
//
//
//    public static void main(String[] args) throws Exception {
//        int webPort = 9090;
//
//        NetentionServer s = new NetentionServer(
//                //ElasticSpacetime.temporary("cv", -1),
//                ElasticSpacetime.local("cv", "cache", true),
//                "localhost",
//                webPort);
//
//        new automenta.climatenet.data.ClimateViewer(s.db);
//
//
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
//
//
//        s.start();
//    }
//
//
//}
