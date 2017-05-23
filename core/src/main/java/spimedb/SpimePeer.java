package spimedb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jcog.Texts;
import jcog.Util;
import jcog.net.UDPeer;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.index.SearchResult;
import spimedb.query.Query;
import spimedb.util.JSON;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

import static spimedb.NObjectConsumer.HashPredicate;
import static spimedb.NObjectConsumer.Tagged;

/**
 * Created by me on 4/4/17.
 */
public class SpimePeer extends UDPeer {

    static final Logger logger = LoggerFactory.getLogger(SpimePeer.class);

    private final SpimeDB db;

    public SpimePeer(int port, SpimeDB db) throws SocketException {
        super(port);

        this.db = db;

        db.on((nobject) -> {
            if (isOnline() && isOriginal(nobject) && isPublic(nobject)) {
                String[] tags = nobject.tags();
                int numTags = tags.length;
                if (numTags > 0) {
                    float totalNeed = 0;

                    for (String t : tags) {
                        totalNeed += need.pri(t);
                    }
                    float avgNeed = totalNeed / numTags;

                    if (totalNeed > 0) {
                        //TODO send to peers which need it (the most)
                        share(nobject, avgNeed);
                    }
                }
            }
        });

        db.on(
                Tagged(
                        (e) -> {
                            byte[] message = e.get("udp");
                            if (message != null) {
                                say(new Msg(message), 1f, false);
                            } else {
                                believe(JSON.toJSONBytes(e), 3);
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

    public boolean isOnline() {
        return (!them.isEmpty());
    }

    protected void share(NObject n, float pri) {
        try {
            believe(Util.toBytes(n), 1 /* TODO pri */);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * to filter the sharing of nobjects
     */
    public boolean isPublic(NObject n) {
        return true;
    }
    public boolean isOriginal(NObject n) {
        return n.get("from")==null;
    }

    @Override
    protected void onUpdateNeed() {

        SearchResult r = db.get(new Query().limit((int)Math.ceil(16)).in(need.data.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY)));
        if (r!=null) {
            r.forEach/*Document*/((d, s) -> {
                share(d, 1f /* TODO: unitize(need.dotProduct(d.tags()) )) */);
                return true;
            });
            r.close();
        }
    }

    @Override
    protected void onBelief(@Nullable UDProfile connected, @NotNull Msg m) {

        JsonNode parsed = null;
        try {

            parsed = Util.fromBytes(m.data(), JsonNode.class);

            JsonNode pi = parsed.get("I");
            String id;
            if (pi != null) {
                id = pi.textValue();
            } else {
                id = SpimeDB.uuidString(); //TODO better
            }

            MutableNObject y = new MutableNObject(id).putAll(parsed).put("from", m.array());
            logger.debug("{} recv {}", me, y);
            db.add(y);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void ask(String xyz) {

    }
}
