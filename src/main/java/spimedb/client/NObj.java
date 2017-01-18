package spimedb.client;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.json.JSON;

import static spimedb.client.util.JS.get;

/** client-side: wraps a JSON-encoded Nobject */
public class NObj  {

    public final String id;
    public final JSObject data;

    public static NObj fromJSON(JSObject data) {
        JSObject ID = get(data, "I");
        if (ID!=null && JSString.isInstance(ID)) {
            String id = ((JSString)ID).stringValue();
            return new NObj(id, data);
        }
        return null;
    }

    NObj(String id, JSObject data) {
        this.id = id;
        this.data = data;
    }

    @Override
    public String toString() {
        return JSON.stringify(data);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this==obj || (obj instanceof NObj && id.equals(((NObj)obj).id));
    }
}
