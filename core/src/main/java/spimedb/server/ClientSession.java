//package spimedb.server;
//
//import jcog.bloom.StableBloomFilter;
//import jcog.bloom.hash.StringHasher;
//import jcog.random.XoRoShiRo128PlusRandom;
//import org.eclipse.collections.api.set.ImmutableSet;
//import org.eclipse.collections.impl.factory.Sets;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import spimedb.FilteredNObject;
//import spimedb.NObject;
//import spimedb.SpimeDB;
//import spimedb.index.DObject;
//import spimedb.query.Query;
//
//import java.util.HashSet;
//import java.util.Set;
//
///**
// * interactive session (ie, Client as seen from Server)
// * TODO: https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
// */
//public class ClientSession extends Session {
//
//    static final ImmutableSet<String> mapIncludesFields = Sets.immutable.with(
//            NObject.POLYGON, NObject.LINESTRING, NObject.NAME, NObject.TAG, NObject.INH
//    );
//
//    ///final ObjectFloatHashMap<String> attention = new ObjectFloatHashMap<>();
//
//    //final UnBloomFilter<String> sent = new UnBloomFilter<>(ALREADY_SENT_MEMORY_CAPACITY, String::getBytes);
//
//    public ClientSession(SpimeDB db, double outputRateLimitBytesPerSecond) {
//        super(db, outputRateLimitBytesPerSecond);
//        set("me", new API());
//    }
//
//    static final ImmutableSet<String> tagFields = Sets.immutable.of(
//            NObject.ID, NObject.NAME, NObject.DESC
//    );
//
//    /**
//     * API accessible by clients
//     */
//    public class API {
//
//        final StableBloomFilter<String> remoteMemory = new StableBloomFilter(
//            /* size */ 32 * 1024, 3, 0, new XoRoShiRo128PlusRandom(1), new StringHasher());
//
//        private Task currentFocus;
//
//        /**
//         * send predicted-to-be-known items after sending predicted-to-be-unknown-by-client
//         */
//        private final boolean ensureSent = false;
//
//
//        public String status() {
//            return db.toString();
//        }
//
//        public Task get(String[] id) {
//            return new Task(ClientSession.this) {
//                @Override
//                public void run() {
//                    for (String x : id) {
//
//                        trySend(this, x, true, tagFields);
//
//                    }
//                }
//            };
//        }
//
//
////
////        /**
////         * provides root-level startup tags
////         */
////        public Task tagRoots() {
////            return this.currentFocus = new Task(ClientSession.this) {
////
////                @Override
////                public void run() {
////                    Iterator<String> r = db.roots();
////                    try {
////                        while (r.hasNext()) {
////
////                            trySend(this, r.next(), true, tagFields);
////                        }
////                    } catch (IOException e) {
////                        return;
////                    }
////                }
////            };
////        }
//
//        /**
//         * allows client to inform server of its forgotten items
//         */
//        public void forgot(String... ids) {
//            for (String id : ids) {
//                remoteMemory.remove(id);
//            }
//        }
//
//
//        public Task focusLonLat(double[][] bounds) {
//
//            //logger.info("start {} focusLonLat {}", this, bounds);
//
//            double[] lon = new double[]{bounds[0][0], bounds[1][0]};
//            double[] lat = new double[]{bounds[0][1], bounds[1][1]};
//
//            String[] tags = new String[]{};
//
//            return new Task(ClientSession.this) {
//
//
//                @Override
//                public void run() {
//
//                    if (currentFocus != null) {
//                        currentFocus.stop();
//                        currentFocus = null;
//                    }
//
//                    currentFocus = this;
//
//                    Set<NObject> lowPriority = new HashSet<>(1024);
//
//                    new Query().where(lon, lat).in(tags).start(db).forEachLocal((dd, s) -> {
//
//                        NObject n = transmittable(dd);
//
//                        if (!running.get()) //early exit test
//                            return false;
//
//
//                        if (!trySend(this, n, false) && ensureSent)
//                            lowPriority.add(n); //buffer it for sending later (low-priority)
//
//
//                        return running.get(); //continue
//
//                    });
//
//                    if (running.get()) {
//                        for (NObject n : lowPriority) {
//
//                            trySend(this, n, true);
//
//                        }
//                    }
//                }
//
//                @NotNull
//                private FilteredNObject transmittable(NObject n) {
//                    return new FilteredNObject(n, mapIncludesFields);
//                }
//            };
//
//        }
//
//
//        private boolean trySend(Task t, String id, boolean force, @Nullable ImmutableSet<String> includeKeys) {
//            int[] idHash = remoteMemory.hash(id);
//            if (force || !remoteMemory.contains(idHash)) {
//                DObject d = db.get(id);
//                if (d != null) {
//                    NObject n = /*db.graphed( */includeKeys != null ? new FilteredNObject(d, includeKeys) : d;
//                    if (n != null) {
//                        chan.forEach(c -> t.sendJSON(c, n));
//                        remoteMemory.add(idHash);
//                        return true;
//                    } else {
//                        return false;
//                    }
//                }
//            }
//            return false;
//        }
//
//        private boolean trySend(Task t, NObject n, boolean force) {
//            int[] idHash = remoteMemory.hash(n.id());
//            if (force || !remoteMemory.contains(idHash)) {
//                chan.forEach(c -> t.sendJSON(c, n));
//                remoteMemory.add(idHash);
//                return true;
//            }
//            return false;
//        }
//
//    }
//
//
////    public boolean remoteProbablyHas(NObject n) {
////        return remoteMemory.addIfMissing(n.id());
////    }
//
//
//    //    public void stopAll() {
////        active.forEach(Task::stop);
////    }
////
////    /**
////     * returns the input task
////     */
////    public Task stopAllExcept(Task t) {
////        active.forEach(x -> {
////            if (t != x)
////                x.stop();
////        });
////        return t;
////    }
//
//
//}
