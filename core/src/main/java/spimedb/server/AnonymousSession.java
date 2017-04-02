package spimedb.server;

import com.google.common.collect.Lists;
import io.undertow.websockets.core.WebSocketChannel;
import jcog.io.Twokenize;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.NObjectConsumer;
import spimedb.SpimeDB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by me on 3/30/17.
 * TODO make the NObjectConsumer impl an inner class and extend OnTag
 */
public class AnonymousSession extends Session implements NObjectConsumer {

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

        statusChange(socket, "connect");
    }

    private void statusChange(WebSocketChannel socket, String status) {
        db.add(
            new MutableNObject(sessionID + "." + serial.incrementAndGet(),
            status + " "+ socket.getDestinationAddress() ));
    }

    @Override
    protected void onDisconnected(WebSocketChannel socket) {
        if (chan.isEmpty()) { //last one?
            synchronized (chan) {
                if (chan.isEmpty()) { //check again. the first one only elides synchronization in non-empty case
                    db.onTag.off(filter.keySet(), this);
                    filter.clear();
                }
            }
        }

        statusChange(socket, "disconnect");
    }

    final String sessionID;
    final AtomicInteger serial = new AtomicInteger();

    public class API {

        public void tell(String[] tags, String message) {
            MutableNObject n = new MutableNObject(sessionID + "." + serial.incrementAndGet(), message);

            ArrayList<String> tt = Lists.newArrayList(tags);

            List<Twokenize.Span> sp = Twokenize.twokenize(message);
            for (Twokenize.Span s : sp) {
                String c = s.content.trim();
                switch (s.pattern) {
                    case "hashtag":
                        c = c.substring(1); //remove hashtag
                        break;
                    case "emoticon":
                    case "url":
                    case "mention":
                    case "email":
                        break;
                    default:
                        c = null;
                }
                if (c!=null)
                    tt.add(c);
            }

            n.when(System.currentTimeMillis());
            n.withTags(
                //Iterables.concat(
                    tt
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
                    db.onTag.off(ss, this);
                return null;
            }else {
                if (e == null) {
                    db.onTag.on(ss, this);
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
