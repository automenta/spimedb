package com.kixeye.kixmpp.p2p.util;

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


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Net {
    private final static Logger logger = LoggerFactory.getLogger(Net.class);
    private final static String localhost = "127.0.0.1";

    /**
     * Try to find a good IP for the local computer.  It looks for a
     * standard IP4 address, then standard IP6, then IP4 P2P, then
     * IP6 P2P, and if all else fails returns 127.0.0.1.
     *
     * @return IP address for the local computer.
     */
    static public String getLocalAddress() {
        try {
            // Enumerate NICs looking for a reasonable IP
            String bestAddress = localhost;
            int bestScore = 0;
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements())
            {
                NetworkInterface n = (NetworkInterface) e.nextElement();

                // ignore loop backs
                if (n.isLoopback()) {
                    continue;
                }

                // allow P2P but rank it lower than others
                int baseScore = 10;
                if (n.isPointToPoint()) {
                    baseScore = 5;
                }

                // get first IP associated with NIC
                Enumeration<InetAddress> ee = n.getInetAddresses();
                while (ee.hasMoreElements())
                {
                    int score = baseScore;
                    InetAddress i = (InetAddress) ee.nextElement();
                    if (i.isLoopbackAddress()) {
                        continue;
                    }

                    // prefer ip4 addresses for now
                    if (i instanceof Inet4Address) {
                        score += 1;
                    }

                    // replace best of this one is better
                    if (score > bestScore) {
                        bestAddress = i.getHostAddress();
                        bestScore = score;
                        break;
                    }
                }
            }

            return bestAddress;

        } catch (Exception e) {
            logger.error("Exception trying to find a local IP address, defaulting to 127.0.0.1", e);
            return localhost;
        }
    }
}
