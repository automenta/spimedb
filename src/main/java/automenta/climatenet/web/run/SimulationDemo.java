//package automenta.climatenet.web.run.run;
//
//import automenta.climatenet.data.elastic.ElasticSpacetime;
//import automenta.climatenet.data.geo.USGSEarthquakes;
//import automenta.climatenet.data.sim.RobotSimulant;
//import automenta.climatenet.knowtention.Channel;
//import automenta.climatenet.p2p.NObject;
//import automenta.climatenet.p2p.SpacetimeTagPlan;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.geojson.LngLatAlt;
//
//import java.util.ArrayList;
//import java.util.List;
//
//
//public class SimulationDemo {
//
//    public static class SimpleSimulation extends Channel implements Runnable {
//
//        List<RobotSimulant> agents = new ArrayList();
//        long lastUpdate = System.currentTimeMillis();
//        long updatePeriodMS = 1500;
//        List<SpacetimeTagPlan.Possibility> possibilities = new ArrayList();
//
//
//        public SimpleSimulation(String id) {
//            super(id);
//
//
//            long now = System.currentTimeMillis();
//
//            //y12 gigadeth inc.
//            double cy = 35.98909;
//            double cx = -84.2566178;
//            double b = 0.01;
//
//            agents.add(new RobotSimulant("Megatron Spike", cx, cy, 0,
//                    new RobotSimulant.CircleController(new LngLatAlt(cy, cx))));
//
//            agents.add(new RobotSimulant("Trypticon Perceptor", cx + b, cy + 2 * b, 0)
//                    .knowHere(
//                            new NObject().when(now + 10000).tag("Physics", 0.5).tag("Biology"),
//                            new NObject().when(now + 20000).tag("Sleep", 0.8)
//                    )
//            );
//
//            agents.add(new RobotSimulant("Skywarp Mindwipe", cx - b, cy, 0)
//                    .knowHere(new NObject().when(now + 20000).tag("Physics", 0.75).tag("Chemistry")));
//
//
//            //Dope Freak Ultra
//            //Killa Method Money
//            //Dr-T Bonecrusher
//
//
//
//            new Thread(this).start();
//        }
//
//        int planningPeriods = 4;
//        int c = 0;
//
//        @Override
//        public void run() {
//
//
//            while (true) {
//
//                update();
//
//                c++;
//
//                try {
//                    Thread.sleep(updatePeriodMS);
//                } catch (InterruptedException e) {}
//            }
//        }
//
//        protected synchronized void update() {
//            long now = System.currentTimeMillis();
//            double dt = (now - lastUpdate) / 1000.0;
//            lastUpdate = now;
//
//
//            ObjectNode next = om.getNodeFactory().objectNode();
//            for (RobotSimulant r : agents) {
//                //Polygon p = r.update(dt);
//                r.update(dt);
//                //LngLatAlt p = r.position;
//
////                try {
////                    ////TODO hack fuck jackson this is ridiculous
////                    //TODO avoid using intermediate string repr here
////                    String s = om.writeValueAsString(p);
////                    ObjectNode vv = om.readValue(s, ObjectNode.class);
////
////                    next.put(r.id, vv);
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
//
//
//                next.set(r.getId(), om.valueToTree(r));
//            }
//
//
//            if (c % planningPeriods == 0) {
//
//                List<NObject> n= new ArrayList();
//                for (RobotSimulant r : agents) {
//                    n.addAll(r.getMemory());
//                }
//
//
//                SpacetimeTagPlan p = new SpacetimeTagPlan(n, true, 1000, true, false );
//                possibilities.clear();
//                possibilities.addAll(p.compute());
//
//            }
//
//            int p = 0;
//            for (SpacetimeTagPlan.Possibility poss : possibilities) {
//                next.set(poss.getId(), om.valueToTree(poss));
//                p++;
//            }
//
//            //System.out.println(next);
//
//            commit(next);
//        }
//
//    }
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
//            s.add("demo", new SimpleSimulation("BattleTelemetry"));
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
