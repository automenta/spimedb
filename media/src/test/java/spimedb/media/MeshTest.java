package spimedb.media;

import jcog.Util;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Test;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.Peer;
import spimedb.Spime;
import spimedb.index.SearchResult;
import spimedb.server.UDP;

import java.io.IOException;
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

        ThreadGroup workerGroup = new ThreadGroup("Worker");
        Thread workerThread = new Thread(workerGroup, () -> {

            worker.put(UDP.class, new UDP(worker.db, 10000)); //HACK;
            worker.restart();

            worker.db.add(new MutableNObject("abc").withTags("xyz"));

        }, "Worker");

        ThreadGroup clientGroup = new ThreadGroup("Client");
        Thread clientThread = new Thread(clientGroup, () -> {
            client.put(UDP.class,new UDP(client.db, 10001)); //HACK;

            client.restart();

            Peer udp = client.get(UDP.class).peer();
            udp.ping(10000);

            Util.sleep(1000);

            List<NObject> found = new ArrayList();
            try {
                //SearchResult r = client.db.find("xyz", 10 /* HACK */);
                client.db.onTag.on("xyz", (x) -> {
                    found.add(x);
                });

                udp.ask("xyz");

            } catch (Exception e) {
                assertTrue(false);
            }

            Util.sleep(1000);

            assertEquals(1, found.size());

        }, "Client");


        workerThread.start();
        clientThread.start();

        //

        clientThread.join();
        workerThread.join(); //wait for clientThread to finish first

    }
}
