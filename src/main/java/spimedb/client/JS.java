package spimedb.client;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;

/**
 * Created by me on 1/16/17.
 */
public class JS {

    @JSBody(params = { "instance", "index" }, script = "return instance[index];")
    public static native float getFloat(JSObject instance, String index);



    @JSBody(params = { "instance", "index" }, script = "return instance[index];")
    public static native JSObject get(JSObject instance, String index);

    @JSBody(params = { "instance", "index" }, script = "return instance[index];")
    public static native JSObject get(JSObject instance, JSObject index);

    @JSBody(params = { "instance", "index", "obj" }, script = "instance[index] = obj;")
    public static native void set(JSObject instance, JSObject index, JSObject obj);

    @JSBody(params = { "instance" }, script = "parseFloat(instance);")
    public static native float toFloat(JSObject instance);
}
