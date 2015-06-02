package com.kixeye.kixmpp.p2p.node;

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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NodeIdTest {

    @Test
    public void toStringTest() {
        NodeId a = new NodeId(1);
        Assert.assertEquals("NodeId{ id=0x1 }", a.toString());

        NodeId b = new NodeId(10);
        Assert.assertEquals("NodeId{ id=0xA }", b.toString());
    }

    @Test
    public void compareToTest() {
        NodeId a = new NodeId(1);
        NodeId b = new NodeId(2);

        Assert.assertTrue( a.compareTo(b) < 0);
        Assert.assertTrue( b.compareTo(a) > 0);
        Assert.assertTrue( a.compareTo(a) == 0);
    }

    @Test
    public void equalsTest() {
        NodeId a = new NodeId(1);
        NodeId b = new NodeId(2);
        NodeId c = new NodeId(1);
        NodeId d = new NodeId(a);

        Assert.assertTrue( a.equals(a) );
        Assert.assertTrue( a.equals(c) );
        Assert.assertTrue( a.equals(d) );
        Assert.assertTrue( c.equals(d) );
        Assert.assertFalse( a.equals(b) );
        Assert.assertFalse( a.equals(null) );
        Assert.assertFalse( a.equals( new Long(1)) );
    }

    @Test
    public void randomTest() {
        NodeId a = new NodeId();
        NodeId b = new NodeId();
        Assert.assertFalse( a.equals(b) );
    }

    @Test
    public void hashCodeTest() {
        NodeId a = new NodeId(0);
        NodeId b = new NodeId(0);
        NodeId c = new NodeId(1);
        Assert.assertEquals( a.hashCode(), b.hashCode() );
        Assert.assertNotEquals( a.hashCode(), c.hashCode() );
    }
}
