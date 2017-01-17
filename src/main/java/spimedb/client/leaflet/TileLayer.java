package spimedb.client.leaflet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
public abstract class TileLayer implements JSObject {

    @JSBody(params = { "urlTemplate", "options" }, script = "return L.tileLayer(urlTemplate, options);")
    public static native TileLayer create(String urlTemplate, TileLayerOptions options);

    public static TileLayer create(String urlTemplate) {
        return create(urlTemplate, TileLayerOptions.create());
    }

    public abstract TileLayer addTo(LeafletMap map);
}
