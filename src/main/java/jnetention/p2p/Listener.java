/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the GNU Public
 * License 2.0 which is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package jnetention.p2p;

/**
 * Listen for messages
 * 
 * @author jfk
 * @date Mar 2, 2011 9:48:36 PM
 * @since 1.0
 * 
 */
public interface Listener {
    /**
     * Handle a message that was received.
     * 
     * @param topic
     *            the topic
     * @param message
     *            the message
     */
    public void handleMessage(String topic, String message);
}
