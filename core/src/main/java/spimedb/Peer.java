package spimedb;

import jcog.net.UDPeer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketException;


/**
 * Created by me on 4/1/17.
 */
public class Peer extends UDPeer {

    public final static Logger logger = LoggerFactory.getLogger(Peer.class);

    public Peer(int port) throws SocketException {
        super(port);
    }

    @Override
    public void ping(@Nullable InetSocketAddress to) {
        logger.debug("ping: {}", to);
        super.ping(to);
    }

    public void ask(String xyz) {
        say("{\"?\": \"" + xyz + "\"}", 3);
    }

//    @Override
//    protected void receive(Msg m) {
//        logger.info("recv: {}", m);
//    }
}
