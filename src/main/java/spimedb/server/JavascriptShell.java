package spimedb.server;

import io.undertow.server.HttpHandler;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Created by me on 12/26/16.
 */
class JavascriptShell extends ServerWebSocket {

    private static final Logger logger = LoggerFactory.getLogger(JavascriptShell.class );


    final ScriptEngineManager engineManager = new ScriptEngineManager();
    final NashornScriptEngine engine = (NashornScriptEngine) engineManager.getEngineByName("nashorn");

    @Nullable
    private final BiFunction<Session, WebSocketChannel, Object> context;


    public JavascriptShell() {
        this(null);
    }

    public JavascriptShell(BiFunction<Session, WebSocketChannel, Object> s) {
        this.context = s;

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
        String code = message.getData().trim();
        if (code.isEmpty())
            return; //ignore


        Object o;
        long start = System.currentTimeMillis();
        try {
            if (context == null) {
                o = engine.eval(code);
            } else {


                Bindings b = engine.createBindings();

                //TODO try b.put("this", ...
                b.put("_s", context.apply(Session.session(socket), socket));
                o = engine.eval("_s." + code, b);
            }
        } catch (Throwable e) {
            o = e;
        }
        long end = System.currentTimeMillis();

        send(socket, new JSExec(code, o, start, end));

    }

}
