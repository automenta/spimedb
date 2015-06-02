package com.kixeye.kixmpp.server.cluster.message;

/*
 * #%L
 * KIXMPP
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
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

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.p2p.node.NodeId;

import java.util.UUID;

public abstract class MapReduceRequest extends ClusterTask {

    // serialized fields
    private KixmppJid targetJID;
    private UUID transactionId;

    // local-only fields
    private transient NodeId senderId;

    public MapReduceRequest() {
    }

    public MapReduceRequest(KixmppJid targetJID) {
        this.targetJID = targetJID;
    }

    public void setSenderId(NodeId senderId) {
        this.senderId = senderId;
    }


    public NodeId getSenderId() {
        return senderId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public KixmppJid getTargetJID() {
        return targetJID;
    }

    public void reply(MapReduceResponse response) {
        response.setTransactionId( getTransactionId() );
        getKixmppServer().getCluster().sendMessage( getSenderId(), response );
    }

    public abstract void mergeResponse(MapReduceResponse response);

    public abstract void onComplete(boolean timedOut);


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapReduceRequest)) return false;

        MapReduceRequest that = (MapReduceRequest) o;

        if (!transactionId.equals(that.transactionId)) return false;

        return true;
    }


    @Override
    public int hashCode() {
        return transactionId.hashCode();
    }
}
