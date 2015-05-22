/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import automenta.climatenet.p2p.TomPeer;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.RTT;

import java.io.IOException;
import java.util.Random;

/**
 *
 * https://github.com/tomp2p/TomP2P/blob/master/examples/src/main/java/net/tomp2p/examples/ExampleHashMap.java
 */
public class TestP2PDHT {

    static PeerDHT master = null;
    static Random RND = new Random();

    public static PeerDHT[] createAndAttachPeersDHT(int nr, int port) throws IOException {
        PeerDHT[] peers = new PeerDHT[nr];
        master = peers[0];

        for (int i = 0; i < nr; i++) {
            if (i == 0) {
                peers[0] = new PeerBuilderDHT(new PeerBuilder(new Number160(RND)).ports(port).start()).start();
            } else {
                peers[i] = new PeerBuilderDHT(new PeerBuilder(new Number160(RND)).masterPeer(peers[0].peer()).start()).start();

            }
        }
        return peers;
    }

    public static PeerDHT[] createMasters(int nr, int port) throws IOException {
        PeerDHT[] peers = new PeerDHT[nr];

        for (int i = 0; i < nr; i++) {
            peers[i] = new PeerBuilderDHT(new PeerBuilder(new Number160(RND)).ports(port + i).start()).start();
        }
        return peers;
    }

    public static void bootstrap(PeerDHT[] peers) {
        //make perfect bootstrap, the regular can take a while
        for (int i = 0; i < peers.length; i++) {
            for (int j = 0; j < peers.length; j++) {
                if (i != j) {
                    peers[i].peerBean().peerMap().peerFound(
                            peers[j].peerAddress(), null, null, new RTT());
                }
            }
        }
    }

    public static void _main(String[] args) throws Exception {
        try {
            PeerDHT[] peers = createMasters(2, 4001);

            bootstrap(peers);

            TomPeer myPeer1 = new TomPeer(peers[0]);
            TomPeer myPeer2 = new TomPeer(peers[1]);

            Thread.sleep(500);

            myPeer1.send("a", "b");
            //myPeer2.sendOne("c", "d");
            myPeer2.send("e", "f");
            //myPeer1.sendOne("g", "h");

            Thread.sleep(2000);

//            myPeer1.put("This is my location key", "This is my domain", "This is my content key",
//                    "And here comes the data").awaitUninterruptibly();
//            
//            System.out.println("1 put");
//            
//            FutureGet futureGet = myPeer2.get("This is my location key", "This is my domain", "This is my content key");
//            
//            futureGet.awaitUninterruptibly();
//            
//            System.out.println("2 get");
//            
//            if (futureGet.isSuccess()) {
//                Map<Number640, Data> map = futureGet.dataMap();
//                for (Data data : map.values()) {
//                    MyData<String> myData = (MyData<String>) data.object();
//                    System.out.println("key: " + myData.key() + ", domain: " + myData.domain() + ", content: "
//                            + myData.content() + ", data: " + myData.data());
//                }
//            }
        } finally {
            if (master != null) {
                master.shutdown();
            }
        }
    }


}
