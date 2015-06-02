package com.kixeye.kixmpp.p2p.message;

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
import com.kixeye.kixmpp.p2p.node.NodeId;

public class JoinResponse {

    private NodeId responderId;
    private NodeAddress responderAddress;
    private ResponseCode result = ResponseCode.OK;

    public JoinResponse() {
    }

    public JoinResponse(JoinResponse.ResponseCode result, NodeId responderId, NodeAddress responderAddress) {
        this.result = result;
        this.responderId = responderId;
        this.responderAddress = responderAddress;
    }

    public NodeId getResponderId() {
        return responderId;
    }

    public NodeAddress getResponderAddress() {
        return responderAddress;
    }

    public ResponseCode getResult() {
        return result;
    }

    public enum ResponseCode {
        OK,
        REJECTED_EXISTING_CONNECTION,
        REJECTED_ID_ALREADY_IN_USE
    }
}
