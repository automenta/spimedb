package spimedb.server;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.SpimeDB;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
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

        tag("", +1); //auto-subscribve to the root

        set("me", new API());
    }

    final UUID sessionID = UUID.randomUUID();
    final AtomicInteger serial = new AtomicInteger();

    public class API {

        public void tell(String[] channels, String message) {
            MutableNObject n = new MutableNObject(sessionID + "" + serial.incrementAndGet(), message);
            //TODO geo-ip etc
            tag.each(channels, (c)->c.accept(n));
        }

    }

    public void tag(String s, int v) {

        filter.compute(s, (ss, e) -> {
            if (v == 0) {
                if (e!=null)
                    tag.off(ss, this);
                return null;
            }

            if (e==null) {
                tag.on(ss, this);
            }
            return v;
        });


    }

    @Override
    protected void onClose(WebSocketChannel socket, StreamSourceFrameChannel channel) throws IOException {
        super.onClose(socket, channel);

        tag.off(filter.keySet(), this);
        filter.clear();
    }

    @Override
    public void accept(NObject nObject) {
        sendJSONBinary(chan, nObject);
    }

}
