package spimedb.server;

import com.google.common.util.concurrent.RateLimiter;
import de.jjedele.sbf.StableBloomFilter;
import de.jjedele.sbf.hashing.StringHashProvider;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.infinispan.commons.util.concurrent.ConcurrentHashSet;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.query.Query;

import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * interactive session (ie, Client as seen from Server)
 * TODO: https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
 */
public class Session extends AbstractServerWebSocket {
    /**
     * bytes per second
     */
    public static final int defaultOutRateBytesPerSecond = 8 * 1024;

//    /**
//     * max # of items that can be remembered to have already been sent.
//     * this should not exceed the client's object bag capacity, which it should configure on
//     * connecting or changing its capacity
//     */
//    final int ALREADY_SENT_MEMORY_CAPACITY = 16;

    /**
     * response bandwidth throttle
     */
    final RateLimiter defaultOutRate = RateLimiter.create(defaultOutRateBytesPerSecond);

    final Set<Task> active = new ConcurrentHashSet<>();

    final StableBloomFilter<String> remoteMemory = new StableBloomFilter<>( /* size */ 4096, 3, 0.0005f, new StringHashProvider());

    ///final ObjectFloatHashMap<String> attention = new ObjectFloatHashMap<>();

    //final UnBloomFilter<String> sent = new UnBloomFilter<>(ALREADY_SENT_MEMORY_CAPACITY, String::getBytes);

    final SpimeDB db;

    final SimpleBindings scope;
    private WebSocketChannel chan;

    public Session(SpimeDB db) {
        this.db = db;
        scope = new SimpleBindings();
        scope.put("me", new API());
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {
        super.onConnect(exchange, socket);
        this.chan = socket;
    }

    /**
     * API accessible by clients
     */
    public class API {

        private Task currentFocus;


        public String status() {
            return db.toString();
        }

        /**
         * provides root-level startup tags
         */
        public Task tagRoots() {
            return this.currentFocus = new Task(Session.this) {

                @Override
                public void run() {
                    Iterator<String> r = db.tags.roots();
                    try {
                        while (r.hasNext()) {
                            NObject t = db.get(r.next());
                            sendJSON(chan, t);
                            remoteMemory.add(t.id());
                        }
                    } catch (IOException e) {
                        return;
                    }
                }
            };
        }

        /**
         * allows client to inform server of invalidations
         */
        public boolean forgot(String... ids) {
            for (String id : ids) {
                remoteMemory.remove(id);
            }
            return true;
        }

        public Task focusLonLat(float[][] bounds) {

            if (currentFocus != null) {
                currentFocus.stop();
                currentFocus = null;
            }

            logger.info("start {} focusLonLat {}", this, bounds);

            float[] lon = new float[]{bounds[0][0], bounds[1][0]};
            float[] lat = new float[]{bounds[0][1], bounds[1][1]};

            String[] tags = new String[]{};

            return this.currentFocus = new Task(Session.this) {

                @Override
                public void run() {

                    Set<NObject> lowPriority = new HashSet(1024);

                    db.get(new Query((n) -> {

                        if (!running.get()) //early exit test
                            return false;

                        try {

                            int[] idHash = remoteMemory.hash(n.id());
                            if (!remoteMemory.contains(idHash)) {
                                sendJSON(chan, n);
                                remoteMemory.add(idHash);
                            } else {
                                lowPriority.add(n); //buffer it for sending later (low-priority)
                            }
                        } catch (IOException e) {
                            stop();
                            return false; //likely a disconnect
                        }

                        return running.get(); //continue

                    }).where(lon, lat).in(tags));

                    if (running.get()) {
                        for (NObject n : lowPriority) {
                            try {
                                sendJSON(chan, n);
                                remoteMemory.add(n.id());
                            } catch (IOException e) {
                                break;
                            }
                        }
                    }
                }
            };

        }

    }


//    public boolean remoteProbablyHas(NObject n) {
//        return remoteMemory.addIfMissing(n.id());
//    }


    /**
     * interpret text as js code to execute
     */
    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) {

        String code = message.getData().trim();
        if (code.isEmpty())
            return; //ignore

        SpimeDB.runLater(() -> {
            JSExec.eval(code, scope, db.js, result -> {
                Object resultObj = result.o;

                if (resultObj instanceof Task) {
                    //if the result of the evaluation is a Task, queue it
                    start((Task) resultObj);
                } else {
                    //else send the immediate result
                    try {
                        sendJSONBinary(socket, result, defaultOutRate, null);
                    } catch (IOException e) {
                        logger.info("{} {}", socket, e.getMessage());
                    }
                }
            });
        });

    }


    protected void start(Task t) {
        SpimeDB.runLater(() -> {
            t.run();
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
