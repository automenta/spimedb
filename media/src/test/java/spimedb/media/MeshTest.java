package spimedb.media;

import jcog.Util;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Test;
import spimedb.*;
import spimedb.index.Search;
import spimedb.query.Query;

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

        final SpimePeer worker = new SpimePeer(10000, new SpimeDB());//TODO: .with(UDP.class, new UDP()).restart();
        final SpimePeer client = new SpimePeer(10001, new SpimeDB());//TODO: .with(UDP.class, new UDP()).restart();

        ThreadGroup workerGroup = new ThreadGroup("Worker");
        Thread workerThread = new Thread(workerGroup, () -> {

            worker.db.add(new MutableNObject("exists already").withTags("xyz"));

            Util.sleep(3000);

            worker.db.add(new MutableNObject("newly created").withTags("xyz"));

            worker.stop();

        }, "Worker");


        List<NObject> receivedAsync = new ArrayList();

        ThreadGroup clientGroup = new ThreadGroup("Client");
        Thread clientThread = new Thread(clientGroup, () -> {

            client.ping(10000);

            Util.sleep(1000);

            Search result = client.db.find(new Query().in("xyz"));
            result.forEach((d,s) -> {
                receivedAsync.add(d);
                return true;
            });

            Util.sleep(3000);

            client.stop();

        }, "Client");


        workerThread.start();
        clientThread.start();

        //

        clientThread.join();
        workerThread.join(); //wait for clientThread to finish first


        System.out.println(receivedAsync);

        assertEquals(2, IteratorUtils.size(client.db.find(new Query().in("xyz")).docs()));



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
