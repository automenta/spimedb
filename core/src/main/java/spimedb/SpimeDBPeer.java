package spimedb;

import com.fasterxml.jackson.databind.JsonNode;
import jcog.Texts;
import spimedb.util.JSON;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import static spimedb.NObjectConsumer.HashPredicate;
import static spimedb.NObjectConsumer.Tagged;

/**
 * Created by me on 4/4/17.
 */
public class SpimeDBPeer extends Peer {

    private final SpimeDB db;

    public SpimeDBPeer(int port, SpimeDB db) throws SocketException, UnknownHostException {
        super(port);

        this.db = db;

        db.on(
                Tagged(
                        (e) -> {
                            byte[] message = e.get("udp");
                            if (message != null) {
                                say(new Msg(message), 1f, false);
                            } else {
                                say(JSON.toJSONBytes(e), 3);
                            }
                        },
                        "")
        );
        db.on(
                //#peer(<host>)
                HashPredicate((PEER, addr) -> {
                    String[] hp = addr.split(":");
                    if (hp.length == 2) {
                        int pp = Texts.i(hp[1], -1);
                        String hh = hp[0];
                        if (pp != -1) {
                            ping(new InetSocketAddress(hh, pp));
                        }
                    }
                }, "peer")
        );
    }

    @Override
    protected void receive(Msg m) {
        if (m.id() == id) {
            return;
        }

        JsonNode parsed = JSON.fromJSON(m.data(), JsonNode.class);
        JsonNode pi = parsed.get("I");
        String id;
        if (pi != null) {
            id = pi.textValue();
        } else {
            id = SpimeDB.uuidString(); //TODO better
        }

        db.add(new MutableNObject(id).putAll(parsed).put("udp", m.data()));
    }
}
