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
        Thread workerThread = new Thread(workerGroup, () -> {

            worker.setFPS(5f);

            worker.db.add(new MutableNObject("exists already").withTags("xyz"));

            sleep(1500);

            worker.db.add(new MutableNObject("newly created").withTags("xyz"));

            sleep(2500);

            worker.stop();

        }, "Worker");


        List<NObject> receivedAsync = new ArrayList();

        ThreadGroup clientGroup = new ThreadGroup("Client");
        Thread clientThread = new Thread(clientGroup, () -> {

            client.setFPS(5f);

            client.ping(10000);

            //sleep(1000);

            client.db.find(new Query().in("xyz")).forEach((d, s) -> {
                receivedAsync.add(d);
                return true;
            }, 4000, () -> {

                System.out.println(receivedAsync);

                assertEquals(2,
                        receivedAsync.size());
                //IteratorUtils.size(client.db.find(new Query().in("xyz")).docs()));

            });

        }, "Client");


        workerThread.start();
        clientThread.start();


        clientThread.join();
        workerThread.join(); //wait for clientThread to finish first


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
            p.db.sync(100);

            p.setFPS(6f);

            if (i > 0) {
                p.ping(peers.get(i - 1).port());
            }

        }

        sleep(3000);

        //peers.forEach(p -> System.out.println(p.summary()));

        Set<NObject> recv = new LinkedHashSet();
        peers.get(0).
                db.find(new Query().in("peer"))
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
