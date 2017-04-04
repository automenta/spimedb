package spimedb;

import com.fasterxml.jackson.databind.JsonNode;
import jcog.Texts;
import jcog.net.UDPeer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.util.JSON;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

import static spimedb.NObjectConsumer.HashPredicate;
import static spimedb.NObjectConsumer.Tagged;

/**
 * Created by me on 4/1/17.
 */
public class Peer extends UDPeer {

    public final static Logger logger = LoggerFactory.getLogger(Peer.class);

    public Peer(int port) throws SocketException, UnknownHostException {
        super(port);
    }

    @Override
    public void ping(@Nullable InetSocketAddress to) {
        logger.debug("ping: " + to);
        super.ping(to);
    }
}
