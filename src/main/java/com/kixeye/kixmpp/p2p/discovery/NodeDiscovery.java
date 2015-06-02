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

import java.util.List;

public interface NodeDiscovery {
    /**
     * Return a list of peer-to-peer nodes.  This will be called on a thread
     * owned by the Zaqar client but there will only be one call at a time.
     *
     * @return List of peer-to-peer node addresses.
     */
    List<NodeAddress> getNodeAddresses();
}
