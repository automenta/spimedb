package spimedb.server;

import com.google.common.util.concurrent.RateLimiter;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.infinispan.commons.util.concurrent.ConcurrentHashSet;
import spimedb.SpimeDB;
import spimedb.query.Query;
import spimedb.util.bloom.UnBloomFilter;

import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.Set;

/**
 * interactive session (ie, Client as seen from Server)
 * TODO: https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
 */
public class Session extends AbstractServerWebSocket {
    /**
     * bytes per second
     */
    public static final int OUTPUT_throttle = 8 * 1024;

    /**
     * max # of items that can be remembered to have already been sent.
     * this should not exceed the client's object bag capacity, which it should configure on
     * connecting or changing its capacity
     */
    final int ALREADY_SENT_MEMORY_CAPACITY = 16;

    /**
     * response bandwidth throttle
     */
    final RateLimiter outRate = RateLimiter.create(OUTPUT_throttle);


    final Set<Task> active = new ConcurrentHashSet<>();

    final ObjectFloatHashMap<String> attention = new ObjectFloatHashMap<>();

    final UnBloomFilter<String> sent = new UnBloomFilter<>(ALREADY_SENT_MEMORY_CAPACITY, String::getBytes);

    final SpimeDB db;
    final SimpleBindings scope;

    public Session(SpimeDB db) {
        this.db = db;
        scope = new SimpleBindings();
        scope.put("db", db);
        scope.put("me", new API());
    }

    /** API accessible by clients */
    public class API {

        public Task focusLonLat(float[][] bounds) {

            stopAll();

            logger.info("start {} focusLonLat {}", this, bounds);

            float[] lon = new float[]{bounds[0][0], bounds[1][0]};
            float[] lat = new float[]{bounds[0][1], bounds[1][1]};

            String[] tags = new String[]{};

            return new Task(Session.this) {

                @Override
                public void accept(SpimeDB db, WebSocketChannel chan) {
                    db.get(new Query((n) -> {

                        if (!running.get()) //early exit test
                            return false;

                        String i = n.id();
                        if (!sent.containsOrAdd(i)) {

                            try {
                                AbstractServerWebSocket.sendJSONBinary(chan, n);
                            } catch (IOException e) {
                                return false; //likely a disconnect
                            }
                        }

                        return running.get(); //continue

                    }).where(lon, lat).in(tags));

                }
            };

        }

    }


    /** interpret text as js code to execute */
    @Override protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) {

        String code = message.getData().trim();
        if (code.isEmpty())
            return; //ignore

        SpimeDB.runLater(() -> {
            JSExec.eval(code, scope, db.js, result -> {
                Object resultObj = result.o;

                if (resultObj instanceof Task) {
                    //if the result of the evaluation is a Task, queue it
                    start(socket, (Task) resultObj, db);
                } else {
                    //else send the immediate result
                    try {
                        sendJSONBinary(socket, result);
                    } catch (IOException e) {
                        logger.info("{} {}", socket, e.getMessage());
                    }
                }
            });
        });

    }


    protected void start(WebSocketChannel socket, Task t, SpimeDB db2) {
        SpimeDB.runLater(() -> {
            t.accept(db, socket);
            t.stop();
        });
    }

    public void stopAll() {
        active.forEach(Task::stop);
    }

    /**
     * returns the input task
     */
    public Task stopAllExcept(Task t) {
        active.forEach(x -> {
            if (t != x)
                x.stop();
        });
        return t;
    }


}
