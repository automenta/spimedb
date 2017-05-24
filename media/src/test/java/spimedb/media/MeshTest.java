package spimedb.media;

import com.google.common.base.Joiner;
import jcog.Util;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Test;
import spimedb.*;
import spimedb.index.Search;
import spimedb.query.Query;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * end-to-end multinode integration test
 */
public class MeshTest {

    @Test
    public void testMesh1() throws Exception {

        System.setProperty("debug", "true");

        final SpimePeer worker = new SpimePeer(10000, new SpimeDB());//TODO: .with(UDP.class, new UDP()).restart();
        final SpimePeer client = new SpimePeer(10001, new SpimeDB());//TODO: .with(UDP.class, new UDP()).restart();

        ThreadGroup workerGroup = new ThreadGroup("Worker");
        Thread workerThread = new Thread(workerGroup, () -> {

            worker.db.add(new MutableNObject("exists already").withTags("xyz"));

            Util.sleep(1500);

            worker.db.add(new MutableNObject("newly created").withTags("xyz"));

            worker.stop();

        }, "Worker");


        List<NObject> receivedAsync = new ArrayList();

        ThreadGroup clientGroup = new ThreadGroup("Client");
        Thread clientThread = new Thread(clientGroup, () -> {

            client.ping(10000);

            Util.sleep(1000);

            client.db.find(new Query().in("xyz")).forEach((d) -> {
                receivedAsync.add(d);
                return true;
            }, 3000, client::stop);

        }, "Client");


        workerThread.start();
        clientThread.start();


        clientThread.join();
        workerThread.join(); //wait for clientThread to finish first


        System.out.println(receivedAsync);

        assertEquals(2,
                receivedAsync.size());
                //IteratorUtils.size(client.db.find(new Query().in("xyz")).docs()));

    }

    @Test
    public void testMesh2() throws Exception {
        //System.setProperty("debug", "true");

        int nPeers = 3;
        List<SpimePeer> peers = new ArrayList(nPeers);
        for (int i = 0; i < nPeers; i++) {
            int port = 10000 + i;
            SpimePeer p = new SpimePeer(port, new SpimeDB());//TODO: .with(UDP.class, new UDP()).restart();
            p.db.add(new MutableNObject().name("Bot" + port).withTags("peer"));

            peers.add(p);

            if (i > 0) {
                p.ping(peers.get(i-1).port());
            }

            Util.sleep(100);
        }

        Util.sleep(1000);

        //peers.forEach(p -> System.out.println(p.summary()));

        Set<NObject> recv = new LinkedHashSet();
        peers.get(0).
                db.find(new Query().in("peer"))
                //db.find("peer", 16)
        .forEach(recv::add, 2000, () -> {});


//        System.out.println();
//        System.out.println(Joiner.on("\n").join(peers.get(0).seen));
//        System.out.println();

        System.err.println(Joiner.on("\n").join(recv));

        assertEquals(3, recv.size());

        peers.forEach(SpimePeer::stop);

    }

}
