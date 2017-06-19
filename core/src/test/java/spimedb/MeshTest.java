package spimedb;

import com.google.common.base.Joiner;
import org.junit.Test;
import spimedb.query.Query;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static jcog.Util.sleep;
import static org.junit.Assert.assertEquals;

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


        List<NObject> receivedAsync = new ArrayList();


        client.runFPS(5f);
        Thread workerThread = new Thread(workerGroup, () -> {

            worker.runFPS(5f);

            sleep(500);

            worker.db.add(new MutableNObject("exists already").withTags("xyz"));

            sleep(500);

            worker.db.add(new MutableNObject("newly created").withTags("xyz"));

            sleep(500);

            worker.stop();

        }, "Worker");
        workerThread.start();

        sleep(500);

        assertEquals(0, client.them.size());

        client.ping(10000);

        sleep(500);

        assertEquals(1, client.them.size());
        assertEquals(1, worker.them.size());

        new Query().in("xyz").start(client.db).forEach((d, s) -> {
            receivedAsync.add(d);
            return true;
        }, 2500, () -> {

            System.out.println(receivedAsync);

            assertEquals(2,
                    receivedAsync.size());
            //IteratorUtils.size(client.db.find(new Query().in("xyz")).docs()));

        });


        workerThread.join(); //wait for clientThread to finish first

        assertEquals(2, receivedAsync.size());
    }

    @Test
    public void testMesh2() throws Exception {
        System.setProperty("debug", "true");

        int nPeers = 3;
        List<SpimePeer> peers = new ArrayList(nPeers);
        for (int i = 0; i < nPeers; i++) {
            int port = 11000 + i;
            SpimePeer p = new SpimePeer(port, new SpimeDB());//TODO: .with(UDP.class, new UDP()).restart();
            peers.add(p);

            p.db.add(new MutableNObject().name("Bot" + port).withTags("peer"));
            p.db.sync(200);

            p.runFPS(10f);

            if (i > 0) {
                p.ping(peers.get(i - 1).port());
            }

        }

        sleep(5000);

        //peers.forEach(p -> System.out.println(p.summary()));

        Set<NObject> recv = new LinkedHashSet();
        new Query().in("peer").start(peers.get(0).
                db)
                //db.find("peer", 16)
                .forEach((r, s) -> recv.add(r), 2000, () -> {
                    //        System.out.println();
                    System.out.println(Joiner.on("\n").join(peers));
//        System.out.println();

                    System.err.println(Joiner.on("\n").join(recv));

                    assertEquals(3, recv.size());

                    peers.forEach(SpimePeer::stop);
                });


    }

}
