package spimedb;

import jcog.net.UDPeer;

import java.io.IOException;


/**
 * Created by me on 4/1/17.
 */
public class Peer extends UDPeer {

    public Peer(int port) throws IOException {
        super(port);
    }

}
