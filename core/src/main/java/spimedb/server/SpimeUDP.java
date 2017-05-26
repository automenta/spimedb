package spimedb.server;

import spimedb.Main;
import spimedb.Plugin;
import spimedb.SpimeDB;
import spimedb.SpimePeer;

/**
 * Created by me on 5/1/17.
 */
public class SpimeUDP implements Plugin {

    private final SpimeDB db;
    int port;

    private SpimePeer peer = null;

    public SpimeUDP(SpimeDB db) {
        this.db = db;
    }

//    public UDP(SpimeDB db, int port) {
//        this(db);
//        setPort(port);
//    }

    public SpimePeer peer() { return peer; }

    public SpimeUDP setPort(int port) {

        synchronized (db) {

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
                this.peer = new SpimePeer(port, db);
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
