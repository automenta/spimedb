package spimedb.client.leaflet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public abstract class LatLng implements JSObject {
    public static final double DEG_TO_RAD = Math.PI / 180;

    public static final double RAD_TO_DEG = 180 / Math.PI;

    public static final double MAX_MARGIN = 1.0E-9;

    @JSProperty("lat")
    public abstract double getLat();

    @JSProperty("lng")
    public abstract double getLng();

    public abstract double distanceTo(LatLng other);

    public double distanceTo(double lat, double lng) {
        return distanceTo(LatLng.create(lat, lng));
    }

    public abstract LatLng wrap(double left, double right);

    @JSBody(params = { "lat", "lng" }, script = "return L.latLng(lat, lng);")
    public static native LatLng create(double lat, double lng);
}
