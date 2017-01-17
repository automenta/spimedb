package spimedb.client.leaflet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.json.JSON;

/**
 *
 * @author Alexey Andreev
 */
public abstract class Marker implements Layer {

    /**
        var options = {
            data: f,
            title: label,
            color: "#ff7800",
            weight: 1
        };
    */
    @JSBody(params = {"lon", "lat", "options"}, script = "return L.marker([lat, lon], options);")
    public static native Marker create(float lon, float lat, JSObject options);

    public static Marker create(float lon, float lat, String title, String color) {
        return Marker.create(
            lon, lat,
            JSON.parse("{\"title\":\"" + title + "\", \"fillColor\":\"" + color + "\", \"weight\":1}") //HACK
        );
    }

    public abstract Marker addTo(Layer layer);

    public abstract Marker setIcon(Icon emptyIcon);

    public abstract LatLng getLatLng();

}
