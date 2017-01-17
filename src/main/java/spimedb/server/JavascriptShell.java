package spimedb.server;

import io.undertow.server.HttpHandler;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.io.IOException;
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
    private final BiFunction<Session, WebSocketChannel, Object> contextBuilder;


    public JavascriptShell() {
        this(null);
    }

    public JavascriptShell(BiFunction<Session, WebSocketChannel, Object> s) {
        this.contextBuilder = s;

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

        SpimeDB.runLater( ()->
            eval(code,
                 contextBuilder!=null ? contextBuilder.apply(Session.session(socket), socket)
                         : null,
                    (result) -> {
                        try {
                            send(socket, result);
                        } catch (IOException e) {
                            logger.info("{} {}", socket, e.getMessage());
                        }
                    },
                engine
            )
        );

    }

    static public void eval(String code, Object context, Consumer<JSExec> onResult, NashornScriptEngine engine) {
        Object o;
        long start = System.currentTimeMillis();
        try {
            if (context == null) {

                o = engine.eval(code);

            } else {

                try {
                    //TODO try b.put("this", ...
                    o = engine.eval("_s." + code,
                            new SimpleBindings(Maps.mutable.of("_s", context))
                    );

                } catch (Throwable t) {
                    o = t;
                } finally {
                    if (context instanceof Task) {
                        ((Task) context).stop();
                    }
                }
            }
        } catch (Throwable e) {
            o = e;
        }

        long end = System.currentTimeMillis();

        onResult.accept( new JSExec(code, o, start, end) );
    }

}
