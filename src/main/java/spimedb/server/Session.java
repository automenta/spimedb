package spimedb.server;

import com.google.common.util.concurrent.RateLimiter;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import spimedb.SpimeDB;

import javax.script.SimpleBindings;

/**
 * Created by me on 3/4/17.
 */
public class Session extends AbstractServerWebSocket {
    /**
     * response bandwidth throttle
     */
    final RateLimiter defaultOutRate;
    final SpimeDB db;
    final SimpleBindings scope;
    protected WebSocketChannel chan;

    public Session(SpimeDB db) {
        this(db, Double.POSITIVE_INFINITY);
    }

    public Session(SpimeDB db, double outputRateLimitBytesPerSecond) {
        this.defaultOutRate = RateLimiter.create(outputRateLimitBytesPerSecond);
        this.db = db;
        scope = new SimpleBindings();
    }

    public void set(String key, Object value) {
        scope.put(key, value);
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {
        super.onConnect(exchange, socket);
        this.chan = socket;
    }

    /**
     * interpret text as js code to execute
     */
    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) {

        String code = message.getData().trim();
        if (code.isEmpty())
            return; //ignore

        db.runLater(1f, () -> {
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
        db.runLater(1f, () -> {
            t.run();
            t.stop();
        });
    }
}
