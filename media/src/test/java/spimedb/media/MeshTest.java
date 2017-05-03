package spimedb.media;

import jcog.Util;
import org.junit.Test;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.Spime;
import spimedb.SpimeDBPeer;
import spimedb.server.UDP;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * end-to-end multinode integration test
 */
public class MeshTest {

    @Test
    public void testMesh1() throws Exception {
        Spime worker = new Spime();//TODO: .with(UDP.class, new UDP()).restart();
        Spime client = new Spime();

        final SpimeDBPeer[] workerPeer = new SpimeDBPeer[1];

        ThreadGroup workerGroup = new ThreadGroup("Worker");
        Thread workerThread = new Thread(workerGroup, () -> {

            worker.put(UDP.class, new UDP(worker.db, 10000)); //HACK;
            worker.restart();

            workerPeer[0] = worker.get(UDP.class).peer();

            worker.db.add(new MutableNObject("abc").withTags("xyz"));

            Util.sleep(3000);

        }, "Worker");

        AtomicBoolean failure = new AtomicBoolean(false);

        ThreadGroup clientGroup = new ThreadGroup("Client");
        Thread clientThread = new Thread(clientGroup, () -> {

            client.put(UDP.class,new UDP(client.db, 10001)); //HACK;
            client.restart();

            SpimeDBPeer udp = client.get(UDP.class).peer();
            udp.ping(10000);

            Util.sleep(1000);

            List<NObject> found = new ArrayList();
            try {
                //SearchResult r = client.db.find("xyz", 10 /* HACK */);
                client.db.onTag.on("xyz", (x) -> {
                    found.add(x);
                });

                System.out.println(udp.me + " declaring need: xyz");
                udp.need("xyz", 0.5f);

            } catch (Exception e) {
                assertTrue(false);
            }

            Util.sleep(1000);

            if (found.size()!=1)
                failure.set(true);

        }, "Client");


        workerThread.start();
        clientThread.start();

        //

        clientThread.join();
        workerThread.join(); //wait for clientThread to finish first


        workerPeer[0].them.forEach(u -> {
            System.out.println(workerPeer[0].me + " sees " + u.id + " as " + u);
        });


        assertFalse(failure.get());
    }
}
