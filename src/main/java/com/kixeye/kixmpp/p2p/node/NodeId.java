package com.kixeye.kixmpp.p2p.node;

/*
 * #%L
 * Hermes
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Read-only identifier for a peer-to-peer node.
 */
public class NodeId implements Comparable<NodeId> {
    private final static Logger logger = LoggerFactory.getLogger(NodeId.class);
    private final static long machineId;

    public final long id;

    static {
        // Hash network interfaces in an attempt to create a "mostly unique" id for this computer.
        int machineHash;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> iter = NetworkInterface.getNetworkInterfaces();
            while ( iter.hasMoreElements() ){
                NetworkInterface ni = iter.nextElement();
                sb.append( ni.toString() );
            }
            machineHash = sb.toString().hashCode();
        } catch (Exception e) {
            logger.error("Unexpected exception during NodeId static initialization", e);
            machineHash = ThreadLocalRandom.current().nextInt();
        }
        machineId = (machineHash & 0x00000000ffffffffL) << 32;
    }

    public NodeId() {
        long rand = ThreadLocalRandom.current().nextInt() & 0x00000000ffffffffL;
        this.id = machineId | rand;
    }

    public NodeId(long id) {
        this.id = id;
    }

    public NodeId(NodeId nid) {
        this.id = nid.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeId nodeId = (NodeId) o;

        if (id != nodeId.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public int compareTo(NodeId o) {
        return Long.compare(this.id,o.id);
    }

    @Override
    public String toString() {
        return "NodeId{ id=0x" + Long.toHexString(id).toUpperCase() + " }";
    }
}
