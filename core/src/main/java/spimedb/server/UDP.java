package spimedb.server;

import spimedb.*;

/**
 * Created by me on 5/1/17.
 */
public class UDP implements Plugin {

    private final SpimeDB db;
    int port;

    private Peer peer = null;

    public UDP(SpimeDB db) {
        this.db = db;
    }
    public UDP(SpimeDB db, int port) {
        this(db);
        setPort(port);
    }

    public Peer peer() { return peer; }

    public void setPort(int port) {

        synchronized (this) {

            int p = this.port;
            if (p == port)
                return;

            if (this.peer != null) {
                this.peer.stop();
            }

            this.port = port;

            if (port == -1)
                return;

            try {
                this.peer = new SpimeDBPeer(port, db);
            } catch (Exception e) {
                Main.logger.error("{}", e);
            }
        }


    }

    public int getPort() {
        return port;
    }
}
