/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the GNU Public
 * License 2.0 which is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package jnetention.p2p;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A message to be send to the peers.
 * 
 * @author jfk
 * @date Mar 2, 2011 1:53:44 PM
 * @since 1.0
 * 
 */
public class Message {
    public final String id;
    public final String topic;
    public final String message;
    public final List<InetSocketAddress> sentTo = new ArrayList<InetSocketAddress>();
    private long sent = -1;

    public Message(final String id, final String topic, final String message) {
        this.id = id;
        this.message = message;
        this.topic = topic;
    }

    public void addSentTo(final InetSocketAddress target) {
        sentTo.add(target);
    }

    /**
     * @return the id
     */
    public final String getId() {
        return id;
    }

    /**
     * @return the message
     */
    public final String getMessage() {
        return message;
    }

    public long getSent() {
        return sent;
    }

    /**
     * @return the sentTo
     */
    public final List<InetSocketAddress> getSentTo() {
        return sentTo;
    }

    public String getTopic() {
        return topic;
    }

    public void resend() {
        sent = -1;
    }

    public long sent() {
        return sent = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append("Message [id=");
        builder.append(id);
        builder.append(", topic=");
        builder.append(topic);
        builder.append(", message=");
        builder.append(message);
        builder.append(", sentTo=");
        builder.append(sentTo);

        if (sent != -1) {
            builder.append(", sent=");
            builder.append(new Date(sent));
            builder.append("]");
        }

        return builder.toString();
    }
}
