package spimedb.server;

import io.undertow.websockets.core.WebSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;
import spimedb.query.Query;
import spimedb.util.JSON;

import java.util.concurrent.atomic.AtomicBoolean;

import static spimedb.server.ServerWebSocket.send;

/**
 * a session-contextualized task
 */
public class Task {

    public static Logger logger = LoggerFactory.getLogger(Task.class);

    private final Session session;
    private final WebSocketChannel chan;

    final int MAX_RESULTS = 1024;
    final int MAX_RESPONSE_BYTES = 1024 * 1024;
    public final SpimeDB db;
    final AtomicBoolean running = new AtomicBoolean();
    private final long whenStart;
    private long whenStop;


    public Task(SpimeDB db, Session s, WebSocketChannel chan) {
        this.session = s;
        this.db = db;
        this.chan = chan;

        this.whenStart = System.currentTimeMillis();

        this.running.set(true);
        s.active.add(this);
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            this.whenStop = System.currentTimeMillis();
            if (session.active.remove(this))
                logger.info("stop {} {}ms", this, (whenStop - whenStart));
            else
                logger.error("already removed {}", this);
        }
    }

    public void focusClear() {
        //logger.info("start {} focusClear", this);
        session.active.forEach(Task::stop);
    }

    public void focusLonLat(float[][] bounds) {

        focusClear();

        logger.info("start {} focusLonLat {}", this, bounds);

        float[] lon = new float[]{bounds[0][0], bounds[1][0]};
        float[] lat = new float[]{bounds[0][1], bounds[1][1]};


        //JsonGenerator gen =
        //      JSON.msgPackMapper.

        //gen.writeStartArray();

        final int[] count = {0};


        String[] tags = new String[]{};


        db.get(new Query((n) -> {

            String i = n.id();
            if (!session.sent.containsAndAdd(i)) {

                //gen.writeStartObject();

                String json = JSON.toJSON(n);
                int size = json.length();

                send(chan, json);
                if (count[0]++ >= MAX_RESULTS)// || ex.getResponseBytesSent() >= MAX_RESPONSE_BYTES)
                    return false;

                session.outRate.acquire(size);

                //gen.writeEndObject();

                //gen.writeRaw(',');


            }

            return running.get(); //continue

        }).where(lon, lat).in(tags));


    }

}
