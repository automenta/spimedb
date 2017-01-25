package spimedb.client.leaflet;

import org.teavm.jso.JSBody;

/**
 *
 * http://leafletjs.com/reference-1.0.3.html#polygon
 * var latlngs = [[37, -109.05],[41, -109.03],[41, -102.05],[37, -102.04]];
 * var polygon = L.polygon(latlngs, {color: 'red'}).addTo(map);
 */
public abstract class Polygon implements LeafletPath {

    @JSBody(params = { "latlngs" }, script = "return L.polygon(latlngs);")
    public static native Polygon create(LatLng[] latlngs);

    @JSBody(params = { "latlngs", "options" }, script = "return L.polygon(latlngs, options);")
    public static native Polygon create(LatLng[] latlngs, PolylineOptions options);

//    public static Polyline create(Collection<LatLng> latlngs, PolylineOptions options) {
//        return create(latlngs.toArray(new LatLng[latlngs.size()]), options);
//    }
//
//    public static Polyline create(Collection<LatLng> latlngs) {
//        return create(latlngs, PolylineOptions.create());
//    }

    @Override
    public abstract Polygon addTo(LeafletMap map);
}
