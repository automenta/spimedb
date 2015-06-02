package com.kixeye.kixmpp.p2p.serialization;

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

import com.kixeye.kixmpp.p2p.message.MessageRegistry;
import com.kixeye.kixmpp.p2p.node.NodeId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

@RunWith(JUnit4.class)
public class SerializationTest {

    @Test
    public void envelopSerializeTest() throws IOException {
        MessageRegistry messageRegistry = new MessageRegistry();
        messageRegistry.addCustomMessage(1, Envelop.class);
        messageRegistry.addCustomMessage(2, DataObject.class);

        NodeId nid = new NodeId();
        DataObject message = new DataObject(42,"Testing..1..2..3");
        Envelop envelop = new Envelop(nid,message);

        ByteBuf buf = ProtostuffEncoder.serializeToByteBuf(messageRegistry, Unpooled.buffer(), envelop);
        Envelop result = (Envelop) ProtostuffDecoder.deserializeFromByteBuf(messageRegistry, buf);

        Assert.assertEquals(nid, result.getSenderId());
        Assert.assertEquals(message, result.getMessage());
    }


    static public class Envelop {
        private NodeId senderId;
        private Object message;

        public Envelop() {
        }

        public Envelop(NodeId senderId, Object message) {
            this.senderId = senderId;
            this.message = message;
        }

        public NodeId getSenderId() {
            return senderId;
        }

        public Object getMessage() {
            return message;
        }
    }

    public static class DataObject {
        private int integerValue;
        private String stringValue;

        public DataObject() {
        }

        public DataObject(int integerValue, String stringValue) {
            this.integerValue = integerValue;
            this.stringValue = stringValue;
        }

        public int getIntegerValue() {
            return integerValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DataObject that = (DataObject) o;

            if (integerValue != that.integerValue) return false;
            if (!stringValue.equals(that.stringValue)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = integerValue;
            result = 31 * result + stringValue.hashCode();
            return result;
        }
    }
}
