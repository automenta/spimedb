package spimedb.server;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.SpimeDB;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by me on 3/30/17.
 */
public class AnonymousSession extends Session implements Consumer<NObject> {

    final ConcurrentHashMap<String, Integer> filter = new ConcurrentHashMap();

    public AnonymousSession(SpimeDB db) {
        super(db);

        UUID u = UUID.randomUUID();
        sessionID = u.toString();// LongString.toString(u.getLeastSignificantBits()) + "" + LongString.toString(u.getMostSignificantBits());

        set("me", new API());
    }


    @Override
    protected void onConnected(WebSocketChannel socket) {
        if (chan.size() == 1) { //first one?
            synchronized (chan) {
                if (chan.size() == 1) { //check again. the first one only elides synchronization in non-empty case
                    tag("", +1);
                }
            }
        }
    }

    @Override
    protected void onDisconnected(WebSocketChannel socket) {
        if (chan.isEmpty()) { //last one?
            synchronized (chan) {
                if (chan.isEmpty()) { //check again. the first one only elides synchronization in non-empty case
                    db.tag.off(filter.keySet(), this);
                    filter.clear();
                }
            }
        }
    }

    final String sessionID;
    final AtomicInteger serial = new AtomicInteger();

    public class API {

        public void tell(String[] channels, String message) {
            MutableNObject n = new MutableNObject(sessionID + "." + serial.incrementAndGet(), message);

            n.when(System.currentTimeMillis());
            n.withTags(
                //Iterables.concat(
                    Lists.newArrayList(channels)
                    //Collections.singletonList(chan.getDestinationAddress().toString())
                //)
            );

            //TODO decorate with: geo-ip, etc

            db.add(n);
        }

    }


    public void tag(String s, int v) {

        filter.compute(s, (ss, e) -> {
            if (v == 0) {
                if (e!=null)
                    db.tag.off(ss, this);
                return null;
            }else {
                if (e == null) {
                    db.tag.on(ss, this);
                }
                return v;
            }
        });


    }



    @Override
    public void accept(NObject nObject) {
        chan.forEach(c -> sendJSONBinary(c, nObject));
    }

}
