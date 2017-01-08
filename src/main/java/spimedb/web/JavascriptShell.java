package spimedb.web;

import io.undertow.server.HttpHandler;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.function.Consumer;

/**
 * Created by me on 12/26/16.
 */
class JavascriptShell extends WebSocket {
    private final Logger logger = LoggerFactory.getLogger("/shell");

    final ScriptEngineManager engineManager = new ScriptEngineManager();
    final ScriptEngine engine = engineManager.getEngineByName("nashorn");


    public JavascriptShell() {

    }

    public HttpHandler with(Consumer<ScriptEngine> e) {
        e.accept(engine);
        return get();
    }


    /**
     * memoized javascript evaluation
     */
    public static final class JSExec {
        //InetSocketAddr...

        /**
         * input
         */
        public final String i;

        /**
         * output
         */
        public final Object o;

        public final long[] when;

        JSExec(String i, Object o, long timeStart, long timeEnd) {
            this.i = i;
            this.o = o;
            this.when = new long[]{timeStart, timeEnd};
        }

        @Override
        public String toString() {
            //HACK manual JSON generation, used for when jackson cant serialize 'o'
            return "{\"i\":" + i +
                     ",\"o\":" + o +
                     ",\"@\":[[" + when[0] + "," + when[1] + "]]}";
        }
    }

    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) {
        String i = message.getData().trim();
        if (i.isEmpty())
            return; //ignore


        Object o;
        long start = System.currentTimeMillis();
        try {
            o = engine.eval(i);
        } catch (Throwable e) {
            o = e;
        }
        long end = System.currentTimeMillis();

        send(socket, new JSExec(i, o, start, end));

    }

}
