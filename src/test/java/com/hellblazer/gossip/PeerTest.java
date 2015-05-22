package com.hellblazer.gossip;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import jnetention.p2p.Peer;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 5/22/15.
 */
public class PeerTest {

    @Test
    public void testPeers() throws IOException, InterruptedException {
        AtomicBoolean received = new AtomicBoolean(false);

        final Peer a = new Peer(10001) {


        };

        final Peer b = new Peer(10002, Lists.newArrayList(new InetSocketAddress("localhost", 10001))) {

            @Override
            public void onUpdate(UUID id, JsonNode j) {

                assertEquals("{\"value\":\"test\"}", j.toString());
                received.set(true);
                stop();
                a.stop();
            }
        };

        //a.put((Serializable)new byte[] { 0, 1, 2, 3, 4, 5 });
        //a.put((Serializable)new byte[] { 0, 1, 2, 3, 4, 5 });
        //a.put("test");
        a.put(new TestBean("test"));

        a.gossip.waitFor(10);

        System.out.println("finished");

        assertTrue(received.get());
    }

    public static class TestBean implements Serializable {
        private final String value;

        public TestBean(String v) {
            this.value = v;
        }

        public String getValue() {
            return value;
        }
    }
}
