package spimedb;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import jcog.Util;
import jcog.list.FasterList;
import jcog.net.UDPeer;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spimedb.index.DObject;
import spimedb.index.Search;
import spimedb.query.Query;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

/**
 * Created by me on 4/4/17.
 */
public class SpimePeer extends UDPeer {

    private static final float QUERY_NEED = 0.5f;
    private static final int MAX_HITS_FOR_PEER = 16;

    public final SpimeDB db;

    public SpimePeer(SpimeDB db) throws IOException {
        this((InetAddress) null, 0, db);
    }


    public SpimePeer(int port, SpimeDB db) throws IOException {
        this((InetAddress) null, port, db);
    }

    public SpimePeer(String host, int port, SpimeDB db) throws IOException {
        this(InetAddress.getByName(host), port, db);
    }

    public SpimePeer(InetAddress addr, int port, SpimeDB db) throws IOException {
        super(addr, port);

        this.db = db;

        db.on(this::tryShare);
        db.onSearch.on(this::onSearch);

    }

    @Override
    public String toString() {
        return super.toString() + ":" + addr;
    }


    private void tryShare(NObject n) {

        if (isOnline() && isOriginal(n) && isPublic(n)) {
            String[] tags = n.tags();
            int numTags = tags.length;
            if (numTags > 0) {
                float totalNeed = 0;

                for (String t : tags) {
                    totalNeed += need.pri(t);
                }
                float avgNeed = totalNeed / numTags;

                if (totalNeed > 0) {
                    //TODO send to peers which need it (the most)
                    tellSome(n, avgNeed);
                }
            }
        }

    }

    @Override
    public boolean next() {
        if (!super.next())
            return false;

        if (isOnline() && !need.data.isEmpty()) {

            String[] needs = need.data.keySet().toArray(new String[need.data.size()]);

            JsonNode query = Util.jsonNode(Maps.mutable.of(
                    NObject.QUERY, Joiner.on(" ").join(needs)
            ));
            boolean asked;
            if (asked = tellSome(query, 1f) > 0) {
                logger.info("asked: {} x {}", needs, asked);
            }

            //decay: TODO move to .mul method
            List<String> toRemove = new FasterList();
            need.data.replaceAll((k, x) -> {
                float y = x * 0.75f; //decay rate
                if (y < 0.1f) {
                    toRemove.add(k);
                }
                return y;
            });
            toRemove.forEach(need.data::remove);
        }

        return true;
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

        }


    }

    public boolean isOnline() {
        return (!them.isEmpty());
    }

    protected int tellSome(Object n, float pri) {
        int ttl = priToTTL(pri);
        if (ttl > 0) {
            try {
                return tellSome(Util.toBytes(n), ttl /* TODO pri */, true);
            } catch (Throwable e) {
                logger.error("{}", e);
            }
        }
        return 0;
    }

    private int priToTTL(float pri) {
        if (pri > 0.5f)
            return 2;
        else if (pri > 0)
            return 1;
        else
            return 0;
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


            JsonNode qi = parsed.get(NObject.QUERY);
            if (qi != null) {
                db.runLater(() -> {
                    String qt = qi.textValue();
                    logger.debug("answering: {}", qt);
                    onQuery(connected, qt);
                });
                return;
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


    protected void onQuery(UDProfile connected, String query) {
        //TODO move this to the peer query response handling


        try {
            Search r = new Query(query).limit(MAX_HITS_FOR_PEER).start(db);
            //ResponseNObject rn = new ResponseNObject(queryID);
            r.forEachLocalDoc((_n, s) -> {

                tryShare(DObject.get(_n));
                return true;
            });

//                try {
//                    ProxyNObject n = rn.set(DObject.get(_n));
//
//                    byte[] nb = Util.toBytes(n);
//
//                    send(new Msg(Command.TELL.id,
//                            (byte) 1, this.me, null,
//                            nb), connected.addr);
//
//                    return true;
//                } catch (JsonProcessingException e) {
//                    logger.error("{}", e);
//                    return false;
//                }
//
//            });
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

//    static class ResponseNObject extends FilteredNObject {
//
//        private final String queryID;
//
//        public ResponseNObject(String queryID) {
//            super(WebIO.searchResultFull);
//            this.queryID = queryID;
//        }
//
//        @Override
//        protected Object value(String k, Object v) {
//            if (k.equals(NObject.TAG)) {
//                if (v instanceof String)
//                    return v + " " + queryID;
////                else if (v instanceof String[])
////                    return ArrayUtils.add((String[]) v, queryID);
//                else
//                    throw new UnsupportedOperationException();
//            }
//            return v;
//        }
//    }
}
