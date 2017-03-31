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
    private final Router<String, Consumer<NObject>> tag;

    public AnonymousSession(SpimeDB db, Router<String,Consumer<NObject>> tag) {
        super(db);

        this.tag = tag;

        UUID u = UUID.randomUUID();
        sessionID = u.toString();// LongString.toString(u.getLeastSignificantBits()) + "" + LongString.toString(u.getMostSignificantBits());

        set("me", new API());
    }


    @Override
    protected void onConnected(WebSocketChannel socket) {
        synchronized (chan) {
            if (chan.size() == 1) { //first
                tag("", +1);
            }
        }
    }



    @Override
    protected void onDisconnected(WebSocketChannel socket) {
        synchronized (chan) {
            if (chan.size() == 0) { //last
                tag.off(filter.keySet(), this);
                filter.clear();
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
            tag.each(channels, (c)->c.accept(n));
        }

    }


    public void tag(String s, int v) {

        filter.compute(s, (ss, e) -> {
            if (v == 0) {
                if (e!=null)
                    tag.off(ss, this);
                return null;
            }else {
                if (e == null) {
                    tag.on(ss, this);
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
