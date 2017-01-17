package spimedb.client.leaflet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.json.JSON;

/**
 *
 * @author Alexey Andreev
 */
public abstract class CircleMarker implements Layer {

    @JSBody(params = {"lon", "lat", "options"}, script = "return L.circleMarker([lat, lon], options);")
    public static native CircleMarker create(float lon, float lat, JSObject options);

    public static CircleMarker create(float lon, float lat, float radius, String title, String color) {
        return CircleMarker.create(
            lon, lat,
            JSON.parse("{\"radius\":\"" + radius+ "\",\"title\":\"" + title + "\",\"fillColor\":\"" + color + "\",\"weight\":1}") //HACK
        );
    }


    public abstract CircleMarker addTo(Layer layer);

    public abstract LatLng getLatLng();
}
