//package spimedb.server;
//
//import com.google.common.util.concurrent.RateLimiter;
//import io.undertow.websockets.core.WebSocketChannel;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicLong;
//
///**
// * a session-contextualized task
// */
//abstract public class Task implements Runnable {
//
//    public static Logger logger = LoggerFactory.getLogger(Task.class);
//
//    //final int MAX_RESULTS = 1024;
//    //final int MAX_RESPONSE_BYTES = 1024 * 1024;
//
//    protected final AtomicBoolean running = new AtomicBoolean();
//
//    private final long whenCreated;
//
//    //private long whenStarted; // TODO
//    private long whenStopped;
//
//    /** bytes transferred out */
//    protected final AtomicLong outBytes = new AtomicLong(0);
//    protected final RateLimiter outRate;
//
//    //final int[] count = {0};
//
//
//    public Task(Session s) {
//        this(s.defaultOutRate);
//    }
//
//    public Task(final RateLimiter outRate) {
//        this.outRate = outRate;
//
//        this.whenCreated = System.currentTimeMillis();
//
//        this.running.set(true);
//        //s.active.add(this);
//    }
//
//    public void stop() {
//        if (running.compareAndSet(true, false)) {
//            this.whenStopped = System.currentTimeMillis();
//
//            long dms = whenStopped - whenCreated;
//            float kb = outBytes.get()/1024f;
//            //logger.info("stop {} {}ms, sent {}Kb ({} Kb/sec)", this, dms, kb, (kb/(dms/1000f)) );
//        }
//    }
//
//    /** o may be any JSON serializable object, or byte[] */
//    protected void sendJSON(WebSocketChannel c, Object o) {
//        AbstractServerWebSocket.sendJSONBinary(c, o, outRate, outBytes);
//    }
//
//}
