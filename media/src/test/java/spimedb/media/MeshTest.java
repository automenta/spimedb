package spimedb.media;

import jcog.Util;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Test;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.Spime;
import spimedb.SpimeDBPeer;
import spimedb.query.Query;
import spimedb.server.UDP;

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

        final SpimeDBPeer[] workerPeer = new SpimeDBPeer[1];

        ThreadGroup workerGroup = new ThreadGroup("Worker");
        Thread workerThread = new Thread(workerGroup, () -> {

            worker.put(UDP.class, new UDP(worker.db).setPort(10000)); //HACK;
            worker.restart();

            workerPeer[0] = worker.get(UDP.class).peer();

            worker.db.add(new MutableNObject("exists already").withTags("xyz"));

            Util.sleep(3000);

            worker.db.add(new MutableNObject("newly created").withTags("xyz"));

        }, "Worker");


        ThreadGroup clientGroup = new ThreadGroup("Client");
        Thread clientThread = new Thread(clientGroup, () -> {

            client.put(UDP.class,new UDP(client.db).setPort(10001)); //HACK;
            client.restart();

            SpimeDBPeer clientPeer = client.get(UDP.class).peer();
            clientPeer.ping(10000);

            Util.sleep(1000);

            List<NObject> found = new ArrayList();
            try {
                //SearchResult r = client.db.find("xyz", 10 /* HACK */);
//                client.db.onTag.on("xyz", (x) -> {
//                    found.add(x);
//                });

                clientPeer.need("xyz", 0.5f);

            } catch (Exception e) {
                assertTrue(false);
            }

            Util.sleep(3000);


        }, "Client");


        workerThread.start();
        clientThread.start();

        //

        clientThread.join();
        workerThread.join(); //wait for clientThread to finish first


        assertEquals(2, IteratorUtils.size(client.db.get(new Query().in("xyz")).docs()));

    }
}
