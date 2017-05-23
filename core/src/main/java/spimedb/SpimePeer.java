package spimedb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import jcog.Texts;
import jcog.Util;
import jcog.net.UDPeer;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spimedb.index.Search;
import spimedb.server.WebIO;
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

    private static final float QUERY_NEED = 0.5f;
    private static final int MAX_HITS_FOR_PEER = 16;

    public final SpimeDB db;

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
                        tell(nobject, avgNeed);
                    }
                }
            }
        });

        db.onSearch.on(this::onSearch);

        db.on(
                Tagged(
                        (e) -> {
                            byte[] message = e.get("udp");
                            if (message != null) {
                                say(new Msg(message), 1f, false);
                            } else {
                                tell(JSON.toJSONBytes(e), 3);
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


    private void onSearch(Search q) {
        if (!isOnline())
            return;

        int s = q.tagsInc.length;
        if (s > 0) {
            float each = QUERY_NEED / s;
            for (int i = 0; i < s; i++) {
                need(q.tagsInc[i], each);
            }

            tell(Util.toJSON(Maps.mutable.of(

                NObject.ID, q.id,

                NObject.QUERY,
                    //HACK create a dummy query with just the tags
                    Joiner.on(" ").join(q.tagsInc)

            )), 1f);
        }


    }

    public boolean isOnline() {
        return (!them.isEmpty());
    }

    protected void tell(Object n, float pri) {
        try {
            tell(Util.toBytes(n), 1 /* TODO pri */);
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
        return n.get("from") == null;
    }

    @Override
    protected void onUpdateNeed() {

    }

    @Override
    protected void onTell(@Nullable UDProfile connected, @NotNull Msg m) {

        JsonNode parsed = null;
        try {

            parsed = Util.fromBytes(m.data(), JsonNode.class);

            JsonNode pi = parsed.get(NObject.ID);
            if (pi!=null) {

                JsonNode qi = parsed.get(NObject.QUERY);
                if (qi != null) {
                    db.runLater(() -> {
                        String qt = qi.textValue();
                        logger.debug("answering: {}", qt);
                        onQuery(connected, pi.textValue(), qt);
                    });
                    return;
                }
            }

            String id;
            if (pi != null) {
                id = pi.textValue();
            } else {
                id = SpimeDB.uuidString(); //TODO better
            }

            MutableNObject y = new MutableNObject(id).putAll(parsed).put("from", m.array());

            logger.debug("told: {}", y);

            db.addAsync(0.5f, y);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static class ResponseNObject extends FilteredNObject {

        private final String queryID;

        public ResponseNObject(String queryID) {
            super(WebIO.searchResultFull);
            this.queryID = queryID;
        }

        @Override
        protected Object value(String k, Object v) {
            if (k.equals(NObject.TAG)) {
                if (v instanceof String)
                    return v + " " +  queryID;
//                else if (v instanceof String[])
//                    return ArrayUtils.add((String[]) v, queryID);
                else
                    throw new UnsupportedOperationException();
            }
            return v;
        }
    }

    protected void onQuery(UDProfile connected, String queryID, String query) {
        //TODO move this to the peer query response handling


        try {
            Search r = db.find(query, MAX_HITS_FOR_PEER);
            ResponseNObject rn = new ResponseNObject(queryID);
            r.forEach((_n, s) -> {
                try {
                    ProxyNObject n = rn.set(_n);

                    byte[] nb = Util.toBytes(n);

                    send(new Msg(Command.TELL.id,
                            (byte) 1, this.me, null,
                            nb), connected.addr);

                    return true;
                } catch (JsonProcessingException e) {
                    logger.error("{}", e);
                    return false;
                }

            });
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }


}
