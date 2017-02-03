package spimedb.server;

import com.google.common.util.concurrent.RateLimiter;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import jcog.bloom.StableBloomFilter;
import jcog.bloom.hashing.StringHashProvider;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spimedb.FilteredNObject;
import spimedb.GraphedNObject;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.index.DObject;
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

    static final ImmutableSet<String> mapIncludesFields = Sets.immutable.with(
            NObject.POLYGON, NObject.LINESTRING, NObject.NAME, NObject.TAG, NObject.INH
    );

    /**
     * response bandwidth throttle
     */
    final RateLimiter defaultOutRate;



    final StableBloomFilter<String> remoteMemory = new StableBloomFilter<>(
            /* size */ 32 * 1024, 3, 0.01f, new StringHashProvider());

    ///final ObjectFloatHashMap<String> attention = new ObjectFloatHashMap<>();

    //final UnBloomFilter<String> sent = new UnBloomFilter<>(ALREADY_SENT_MEMORY_CAPACITY, String::getBytes);

    final SpimeDB db;

    final SimpleBindings scope;
    private WebSocketChannel chan;

    public Session(SpimeDB db, double outputRateLimitBytesPerSecond) {
        this.db = db;
        this.defaultOutRate = RateLimiter.create(outputRateLimitBytesPerSecond);
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

        /** send predicted-to-be-known items after sending predicted-to-be-unknown-by-client */
        private final boolean ensureSent = false;


        public String status() {
            return db.toString();
        }

        public Task get(String[] id) {
            return new Task(Session.this) {
                @Override public void run() {
                    for (String x : id) {
                        try {
                            trySend(this, x, true);
                        } catch (IOException e) {
                            break;
                        }
                    }
                }
            };
        }

        /**
         * provides root-level startup tags
         */
        public Task tagRoots() {
            return this.currentFocus = new Task(Session.this) {

                @Override
                public void run() {
                    Iterator<String> r = db.roots();
                    try {
                        while (r.hasNext()) {
                            trySend(this, r.next(), true);
                        }
                    } catch (IOException e) {
                        return;
                    }
                }
            };
        }

        /**
         * allows client to inform server of its forgotten items
         */
        public void forgot(String... ids) {
            for (String id : ids) {
                remoteMemory.remove(id);
            }
        }



        public Task focusLonLat(float[][] bounds) {

            logger.info("start {} focusLonLat {}", this, bounds);

            float[] lon = new float[]{bounds[0][0], bounds[1][0]};
            float[] lat = new float[]{bounds[0][1], bounds[1][1]};

            String[] tags = new String[]{};

            return new Task(Session.this) {



                @Override
                public void run() {

                    if (currentFocus != null) {
                        currentFocus.stop();
                        currentFocus = null;
                    }

                    currentFocus = this;

                    Set<NObject> lowPriority = new HashSet<>(1024);

                    db.get(new Query((n) -> {

                        n = transmittable(n);

                        if (!running.get()) //early exit test
                            return false;

                        try {
                            if (!trySend(this, n, false) && ensureSent)
                                lowPriority.add(n); //buffer it for sending later (low-priority)
                        } catch (IOException e) {
                            stop();
                            return false; //likely a disconnect
                        }

                        return running.get(); //continue

                    }).where(lon, lat).in(tags));

                    if (running.get()) {
                        for (NObject n : lowPriority) {
                            try {
                                trySend(this, n, true);
                            } catch (IOException e) {
                                break;
                            }
                        }
                    }
                }

                @NotNull
                private FilteredNObject transmittable(NObject n) {
                    return new FilteredNObject( db.graphed(n), mapIncludesFields);
                }
            };

        }

        private boolean trySend(Task t, String id, boolean force) throws IOException {
            return trySend(t, id, force, null);
        }

        private boolean trySend(Task t, String id, boolean force, @Nullable ImmutableSet<String> includeKeys) throws IOException {
            int[] idHash = remoteMemory.hash(id);
            if (force || !remoteMemory.contains(idHash)) {
                DObject d = db.get(id);
                if (d!=null) {
                    GraphedNObject n = db.graphed(includeKeys!=null ? new FilteredNObject(d, includeKeys) : d);
                    if (n != null) {
                        t.sendJSON(chan, n);
                        remoteMemory.add(idHash);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }

        private boolean trySend(Task t, NObject n, boolean force) throws IOException {
            int[] idHash = remoteMemory.hash(n.id());
            if (force || !remoteMemory.contains(idHash)) {
                t.sendJSON(chan, n);
                remoteMemory.add(idHash);
                return true;
            }
            return false;
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
                if (result == null)
                    return;

                Object resultObj = result.o;

                if (resultObj instanceof Task) {
                    //if the result of the evaluation is a Task, queue it
                    start((Task) resultObj);
                } else {
                    //else send the immediate result
                    //try {
                        sendJSONBinary(socket, result, defaultOutRate, null);
//                    } catch (IOException e) {
//                        logger.info("{} {}", socket, e.getMessage());
//                    }
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

//    public void stopAll() {
//        active.forEach(Task::stop);
//    }
//
//    /**
//     * returns the input task
//     */
//    public Task stopAllExcept(Task t) {
//        active.forEach(x -> {
//            if (t != x)
//                x.stop();
//        });
//        return t;
//    }


}
