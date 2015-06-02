package com.kixeye.kixmpp.p2p.discovery;

/*
 * #%L
 * Zaqar
 * %%
 * Copyright (C) 2014 Charles Barry
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.kixeye.kixmpp.p2p.node.NodeAddress;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class ConstNodeDiscoveryTest {
    @Test
    public void listConstructor() {
        List<NodeAddress> nodes = new ArrayList<>();
        nodes.add( new NodeAddress("127.0.0.1",8000) );
        nodes.add( new NodeAddress("127.0.0.1",8001) );
        ConstNodeDiscovery discovery = new ConstNodeDiscovery(nodes);
        Assert.assertArrayEquals( nodes.toArray(), discovery.getNodeAddresses().toArray() );
    }

    @Test
    public void arrayConstructor() {
        NodeAddress[] nodes = new NodeAddress[]{
            new NodeAddress("127.0.0.1",8000),
            new NodeAddress("127.0.0.1",8001)
        };
        ConstNodeDiscovery discovery = new ConstNodeDiscovery(nodes);
        Assert.assertArrayEquals( nodes, discovery.getNodeAddresses().toArray() );
    }

    @Test
    public void varArgConstructor() {
        NodeAddress a = new NodeAddress("127.0.0.1",8000);
        NodeAddress b = new NodeAddress("127.0.0.1",8000);
        ConstNodeDiscovery discovery = new ConstNodeDiscovery(a, b);
        List<NodeAddress> nodes = discovery.getNodeAddresses();
        Assert.assertEquals( nodes.get(0), a );
        Assert.assertEquals( nodes.get(1), b );
    }
}
