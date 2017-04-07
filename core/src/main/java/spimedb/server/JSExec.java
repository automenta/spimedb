package spimedb.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import spimedb.NObject;
import spimedb.util.JSON;

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
            o = t.getMessage() + "\n" + Joiner.on(' ').join(t.getStackTrace());

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
        return "{\"" + NObject.NAME + "\":" + i + //input command as 'NAME'
                ",\"" + NObject.DESC + "\":" + o + //output result as 'DESC'
                ",\"@\":[[" + when[0] + "," + when[1] + "]]}";
    }

    public JsonNode toJSON() {
        ObjectNode x = JSON.json.createObjectNode();
        x.put(NObject.NAME, i);
        x.put(NObject.DESC, JSON.json.valueToTree(o));
        x.put("@", JSON.json.createArrayNode().add(
            JSON.json.createArrayNode().add(when[0]).add(when[1])
        ) );
        return x;
    }
}
