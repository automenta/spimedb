package spimedb.server;

import spimedb.Main;
import spimedb.Plugin;
import spimedb.SpimeDB;
import spimedb.SpimeDBPeer;

/**
 * Created by me on 5/1/17.
 */
public class UDP implements Plugin {

    private final SpimeDB db;
    int port;

    private SpimeDBPeer peer = null;

    public UDP(SpimeDB db) {
        this.db = db;
    }

//    public UDP(SpimeDB db, int port) {
//        this(db);
//        setPort(port);
//    }

    public SpimeDBPeer peer() { return peer; }

    public UDP setPort(int port) {

        synchronized (this) {

            int p = this.port;
            if (p == port)
                return this;

            if (this.peer != null) {
                this.peer.stop();
            }

            this.port = port;

            if (port == -1)
                return this;

            try {
                this.peer = new SpimeDBPeer(port, db);
            } catch (Exception e) {
                Main.logger.error("{}", e);
            }
        }

        return this;

    }

    public int getPort() {
        return port;
    }
}
