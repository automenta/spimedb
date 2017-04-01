package spimedb.client.leaflet;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
@JSFunctor
public interface LeafletEventListener<T extends LeafletEvent> extends JSObject {
    void occur(T event);
}
