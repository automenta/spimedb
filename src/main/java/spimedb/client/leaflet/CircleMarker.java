package spimedb.client.leaflet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.json.JSON;

/**
 *
 * @author Alexey Andreev
 */
public abstract class CircleMarker implements Layer {

    /**
        var options = {
            data: f,
            title: label,
            color: "#ff7800",
            weight: 1
        };
    */
    @JSBody(params = {"latlng","options"}, script = "return L.circleMarker(latlng, options);")
    public static native CircleMarker create(LatLng latlng, JSObject options);

    public static CircleMarker create(float lon, float lat, String title, String color) {
        return CircleMarker.create(
            LatLng.at(lon, lat),
            JSON.parse("{\"title\":\"" + title + "\", \"color\":\"" + color + "\"}") //HACK
        );
    }


    public abstract CircleMarker addTo(Layer layer);

    public abstract LatLng getLatLng();
}
