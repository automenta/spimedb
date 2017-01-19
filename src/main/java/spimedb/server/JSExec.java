package spimedb.server;

import jdk.nashorn.api.scripting.NashornScriptEngine;

import javax.script.SimpleBindings;
import java.util.function.Consumer;

/**
 * memoized javascript evaluation
 */
public final class JSExec {
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

    static public void eval(String code, SimpleBindings bindings, NashornScriptEngine engine, Consumer<JSExec> onResult) {
        Object o;
        long start = System.currentTimeMillis();
        try {
            if (bindings == null)
                o = engine.eval(code);
            else
                o = engine.eval(code, bindings);

        } catch (Throwable t) {
            o = t;
        }

        if (o == null) {
            //return null to avoid sending the execution summary
            onResult.accept(null);
        } else {
            long end = System.currentTimeMillis();

            onResult.accept(new JSExec(code, o, start, end));
        }
    }

    @Override
    public String toString() {
        //HACK manual JSON generation, used for when jackson cant serialize 'o'
        return "{\"i\":" + i +
                ",\"o\":" + o +
                ",\"@\":[[" + when[0] + "," + when[1] + "]]}";
    }
}
