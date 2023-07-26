//package spimedb.server;
//
//import com.google.common.collect.Lists;
//import io.undertow.websockets.core.WebSocketChannel;
//import io.undertow.websockets.spi.WebSocketHttpExchange;
//import jcog.io.Twokenize;
//import spimedb.MutableNObject;
//import spimedb.NObject;
//import spimedb.NObjectConsumer;
//import spimedb.SpimeDB;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * Created by me on 3/30/17.
// * TODO make the NObjectConsumer impl an inner class and extend OnTag
// */
//public class AnonymousSession extends Session {
//
//    //final ConcurrentHashMap<String, Integer> filter = new ConcurrentHashMap();
//
//    public AnonymousSession(SpimeDB db) {
//        super(db);
//
//        sessionID = SpimeDB.uuidString();
//
//        set("me", new API());
//    }
//
//
//    @Override
//    protected void onConnected(WebSocketChannel socket) {
//        if (chan.size() == 1) { //first one?
//            synchronized (chan) {
//                if (chan.size() == 1) { //check again. the first one only elides synchronization in non-empty case
//                    tag("", +1);
//                }
//            }
//        }
//
//        statusChange(socket, "connect");
//    }
//
//    private void statusChange(WebSocketChannel socket, String status) {
//        db.add(
//            new MutableNObject(sessionID + '/' + serial.incrementAndGet(),
//            status + " "+ socket.getDestinationAddress() ));
//    }
//
//    @Override
//    protected void onDisconnected(WebSocketChannel socket) {
//        if (chan.isEmpty()) { //last one?
////            synchronized (chan) {
////                if (chan.isEmpty()) { //check again. the first one only elides synchronization in non-empty case
////                    filter.clear();
////                }
////            }
//        }
//
//        statusChange(socket, "disconnect");
//    }
//
//    final String sessionID;
//    final AtomicInteger serial = new AtomicInteger();
//
//    public class API {
//
//
//
//    }
//
//
//    public void tag(String s, int v) {
//
////        filter.compute(s, (ss, e) -> {
////            if (v == 0) {
////                if (e!=null)
////                    db.onTag.off(ss, this);
////                return null;
////            }else {
////                if (e == null) {
////                    db.onTag.on(ss, this);
////                }
////                return v;
////            }
////        });
//
//
//    }
//
//    //static final Cache<String,TagSession> tagListeners = Caffeine.newBuilder().weakValues().build();
//
//    static TagSession tag(SpimeDB db, String tag) {
//        //return tagListeners.get(tag, t -> {
//            return new TagSession(db) {
//                @Override protected void onConnected(WebSocketChannel socket) {
//                    super.onConnected(socket);
//                    db.onTag.on(tag, this);
//                }
//
//                @Override
//                public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {
//                    super.onConnect(exchange, socket);
//                }
//
//                @Override
//                protected void onDisconnected(WebSocketChannel socket) {
//                    db.onTag.off(tag, this);
//                    super.onDisconnected(socket);
//                }
//            };
//        //});
//    }
//
//
//    public static class TagSession extends Session implements NObjectConsumer {
//
//        public TagSession(SpimeDB db) {
//            super(db);
//        }
//
//        @Override
//        public void accept(NObject nObject) {
//            chan.forEach(c -> sendJSONBinary(c, nObject));
//        }
//
//    }
//
//}
