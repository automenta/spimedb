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

import com.kixeye.kixmpp.p2p.ClusterClient;
import com.kixeye.kixmpp.p2p.message.MessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Node {

    protected final static Logger logger = LoggerFactory.getLogger(Node.class);

    private final NodeAddress address;

    public enum State {
        CONNECTING,
        CONNECTED,
        CLOSED
    }

    protected NodeId id;
    protected ClusterClient cluster;
    protected State state;
    protected boolean orphaned;

    /**
     * Constructor for Node created from an incoming connection.
     * @param id
     * @param address
     */
    public Node(ClusterClient cluster, NodeId id, NodeAddress address) {
        this.cluster = cluster;
        this.id = id;
        this.address = address;
    }


    /**
     * Constructor for Node that needs to connect to the incoming address.
     * @param address
     */
    public Node(ClusterClient cluster, NodeAddress address) {
        this.cluster = cluster;
        this.id = null;
        this.address = address;
    }


    /**
     * Set the subscriber id, only allowed once.
     * @param value
     */
    public void setId(NodeId value) {
        if (id == null) {
            id = value;
        } else {
            throw new UnsupportedOperationException("id can only be set once");
        }
    }


    /**
     * Get the subscriber id.  It may be null if it has not been set yet.
     * @return
     */
    public NodeId getId() {
        return id;
    }


    /**
     * Get the address.
     * @return
     */
    public NodeAddress getAddress() {
        return address;
    }


    /**
     * orphaned
     */
    public void setOrphaned(boolean value) {
        this.orphaned = value;
    }

    /**
     * Send a message to the owner of this node.
     * @param msg
     */
    public abstract void sendMessage(MessageWrapper msg);


    /**
     * Shutdown the node.
     */
    public void close() {
        state = State.CLOSED;
        if (cluster != null) {
            if (!orphaned) {
                cluster.removeNode(this);
            }
            cluster = null;
        }
    }
}