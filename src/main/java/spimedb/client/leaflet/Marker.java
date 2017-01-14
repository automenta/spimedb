package spimedb.client.leaflet;

import org.teavm.jso.JSBody;

/**
 *
 * @author Alexey Andreev
 */
public abstract class Marker implements Layer {
    @JSBody(params = "latlng", script = "return L.marker(latlng);")
    public static native Marker create(LatLng latlng);

    public abstract Marker addTo(LeafletMap map);

    public abstract LatLng getLatLng();
}
