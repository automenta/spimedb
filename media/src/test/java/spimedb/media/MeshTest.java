package spimedb.media;

import jcog.Util;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Test;
import spimedb.*;
import spimedb.query.Query;
import spimedb.server.SpimeUDP;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * end-to-end multinode integration test
 */
public class MeshTest {

    @Test
    public void testMesh1() throws Exception {
        Spime worker = new Spime();//TODO: .with(UDP.class, new UDP()).restart();
        Spime client = new Spime();

        final SpimePeer[] workerPeer = new SpimePeer[1];

        ThreadGroup workerGroup = new ThreadGroup("Worker");
        Thread workerThread = new Thread(workerGroup, () -> {

            worker.put(SpimeUDP.class, new SpimeUDP(worker.db).setPort(10000)); //HACK;
            worker.restart();

            workerPeer[0] = worker.get(SpimeUDP.class).peer();

            worker.db.add(new MutableNObject("exists already").withTags("xyz"));

            Util.sleep(3000);

            worker.db.add(new MutableNObject("newly created").withTags("xyz"));

        }, "Worker");


        ThreadGroup clientGroup = new ThreadGroup("Client");
        Thread clientThread = new Thread(clientGroup, () -> {

            client.put(SpimeUDP.class,new SpimeUDP(client.db).setPort(10001)); //HACK;
            client.restart();

            SpimePeer clientPeer = client.get(SpimeUDP.class).peer();
            clientPeer.ping(10000);

            Util.sleep(1000);

            clientPeer.need("xyz", 0.5f);

            Util.sleep(3000);

        }, "Client");


        workerThread.start();
        clientThread.start();

        //

        clientThread.join();
        workerThread.join(); //wait for clientThread to finish first


        assertEquals(2, IteratorUtils.size(client.db.get(new Query().in("xyz")).docs()));

    }

    @Test
    public void testMesh2() throws Exception {
        int nPeers = 10;
        List<SpimePeer> peers = new ArrayList(nPeers);
        for (int i = 0; i < nPeers; i++) {
            SpimePeer p = new SpimePeer(10000 + i, new SpimeDB());//TODO: .with(UDP.class, new UDP()).restart();
            peers.add(p);


            if (i > 0) {
                p.ping(peers.get(i-1).port());
            }
        }
        Util.sleep(1000);

        peers.forEach(p -> System.out.println(p.summary()));

    }
}
