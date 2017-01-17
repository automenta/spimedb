package spimedb.server;

import io.undertow.websockets.core.WebSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * a session-contextualized task
 */
abstract public class Task implements BiConsumer<SpimeDB, WebSocketChannel> {

    public static Logger logger = LoggerFactory.getLogger(Task.class);

    private final Session session;

    final int MAX_RESULTS = 1024;
    //final int MAX_RESPONSE_BYTES = 1024 * 1024;

    protected final AtomicBoolean running = new AtomicBoolean();

    private final long whenCreated;
    private long whenStarted /* TODO */, whenStopped;

    /** bytes transferred out */
    final AtomicLong outBytes = new AtomicLong(0);

    //final int[] count = {0};


    public Task(Session s) {
        this.session = s;

        this.whenCreated = System.currentTimeMillis();

        this.running.set(true);
        s.active.add(this);
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            this.whenStopped = System.currentTimeMillis();
            if (session.active.remove(this))
                logger.info("stop {} {}ms", this, (whenStopped - whenCreated));
            else
                logger.error("already removed {}", this);
        }
    }

}
