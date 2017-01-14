package spimedb.client.leaflet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import java.util.Collection;

/**
 *
 * @author Alexey Andreev
 */
public abstract class LatLngBounds implements JSObject {
    @JSBody(params = { "southWest", "northEast" }, script = "return L.latLngBounds(southWest, northEast);")
    public static native LatLngBounds create(LatLng southWest, LatLng northEast);

    @JSBody(params = "latlngs", script = "return L.latLngBounds(latlngs);")
    public static native LatLngBounds fromPoints(LatLng... latlngs);

    public abstract LatLngBounds extend(LatLng latlng);

    public abstract LatLngBounds extend(LatLngBounds otherBounds);

    public abstract LatLng getSouthWest();

    public abstract LatLng getNorthEast();

    public abstract LatLng getNorthWest();

    public abstract LatLng getSouthEast();

    public abstract double getWest();

    public abstract double getEast();

    public abstract double getNorth();

    public abstract double getSouth();

    public abstract LatLng getCenter();

    public abstract boolean contains(LatLngBounds otherBounds);

    public abstract boolean contains(LatLng latlng);

    public abstract boolean intersects(LatLngBounds otherBounds);

    public abstract LatLngBounds pad(double bufferRatio);

    public abstract boolean isValid();

    public static LatLngBounds fromPoints(Collection<LatLng> latlngs) {
        return fromPoints(latlngs.toArray(new LatLng[latlngs.size()]));
    }
}
