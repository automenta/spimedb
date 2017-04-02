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

    private final SpimeDB db;

    public Peer(int port, SpimeDB db) throws SocketException, UnknownHostException {
        super(port);
        this.db = db;

        db.on(
            Tagged(
                (e)-> {
                    byte[] message = e.get("udp");
                    if (message!=null) {
                        say(new Msg(message), 1f, false);
                    } else {
                        say(e.toJSONString(false), 3);
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
                    if (pp!=-1) {
                        ping(new InetSocketAddress(hh, pp));
                    }
                }
            }, "peer")
        );

    }

    @Override
    protected void receive(Msg m) {
        if (m.id()==id) {
            return;
        }

        String json = m.dataString();
        JsonNode parsed = JSON.fromJSON(json);
        JsonNode pi = parsed.get("I");
        String id;
        if (pi!=null) {
            id = pi.textValue();
        } else {
            id = UUID.randomUUID().toString(); //TODO better
        }

        db.add(new MutableNObject(id).putAll(parsed).put("udp", m.data()));
    }

    @Override
    public void ping(@Nullable InetSocketAddress to) {
        logger.debug("ping: " + to);
        super.ping(to);
    }
}
