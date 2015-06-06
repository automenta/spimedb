//package automenta.climatenet.web.run.run;
//
//import automenta.climatenet.ReadOnlyChannel;
//import automenta.climatenet.SpacetimeWebServer;
//import automenta.climatenet.data.NOntology;
//import automenta.climatenet.data.SchemaOrg;
//import automenta.climatenet.data.elastic.ElasticSpacetime;
//import automenta.climatenet.p2p.TomPeer;
//import net.tomp2p.connection.PeerBean;
//import net.tomp2p.dht.PeerBuilderDHT;
//import net.tomp2p.p2p.PeerBuilder;
//import net.tomp2p.peers.Number160;
//import org.pircbotx.snapshot.ChannelSnapshot;
//
//import java.util.UUID;
//
///**
// * Created by me on 4/14/15.
// */
//public class NetentionServer extends SpacetimeWebServer {
//
//    public NetentionServer(final ElasticSpacetime db, String host, int port) throws Exception {
//        super(db, host, port);
//    }
//
//    public static void main(String[] args) throws Exception {
//        int webPort = 9090;
//        int p2pPort = 9091;
//        String peerID = UUID.randomUUID().toString();
//        final boolean peerEnable = false;
//
//        NetentionServer s = new NetentionServer(
//                ElasticSpacetime.serverOrLocal("localhost", "cv", false),
//                "localhost",
//                webPort);
//
//
//        logger.info("Loading Schema.org (ontology)");
//        SchemaOrg.load(s.db);
//        logger.info("Loading ClimateViewer (ontology)");
//        new ClimateViewer(s.db);
//        logger.info("Loading Netention (ontology)");
//        NOntology.load(s.db);
//
//        //EXAMPLES
//        {
//            //new IRCBot(s.db, "RAWinput", "irc.freenode.net", "#netention");
//            //new FileTailWindow(s.db, "netlog", "/home/me/.xchat2/scrollback/FreeNode/#netention.txt").start();
//
//        }
//
//        if (peerEnable) {
//            final TomPeer peer = new TomPeer(
//                    new PeerBuilderDHT(new PeerBuilder(Number160.createHash(peerID)).ports(p2pPort).start()).start());
//            peer.add(s.db);
//
//            s.addPrefixPath("/peer/index", new ChannelSnapshot(new ReadOnlyChannel<PeerBean>("/peer/index") {
//                @Override
//                public PeerBean nextValue() {
//                    return peer.peer.peerBean();
//                }
//            }));
//            s.addPrefixPath("/peer/connection", new ChannelSnapshot(new ReadOnlyChannel("/peer/connection") {
//                @Override
//                public Object nextValue() {
//                    return peer.peer.peer().connectionBean();
//                }
//            }));
//            s.addPrefixPath("/peer/route", new ChannelSnapshot(new ReadOnlyChannel("/peer/route") {
//                @Override
//                public Object nextValue() {
//                    return peer.peer.peer().distributedRouting().peerMap();
//                }
//            }));
//        }
//
//        s.start();
//    }
//
//}
