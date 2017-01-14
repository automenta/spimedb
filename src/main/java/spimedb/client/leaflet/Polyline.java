package spimedb.client.leaflet;

import org.teavm.jso.JSBody;

import java.util.Collection;

/**
 *
 * @author Alexey Andreev
 */
public abstract class Polyline implements LeafletPath {
    public static Polyline create(LatLng[] latlngs) {
        return create(latlngs, PolylineOptions.create());
    }

    @JSBody(params = { "latlngs", "options" }, script = "return L.polyline(latlngs, options);")
    public static native Polyline create(LatLng[] latlngs, PolylineOptions options);

    public static Polyline create(Collection<LatLng> latlngs, PolylineOptions options) {
        return create(latlngs.toArray(new LatLng[latlngs.size()]), options);
    }

    public static Polyline create(Collection<LatLng> latlngs) {
        return create(latlngs, PolylineOptions.create());
    }

    @Override
    public abstract Polyline addTo(LeafletMap map);
}
