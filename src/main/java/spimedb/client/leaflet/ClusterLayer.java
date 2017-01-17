package spimedb.client.leaflet;

import org.teavm.jso.JSBody;

/**
 * TODO integrate R-tree
 * https://github.com/Leaflet/Leaflet.markercluster
 */
public abstract class ClusterLayer implements Layer {

    @JSBody(params = { }, script = "return new L.MarkerClusterGroup();")
    public static native ClusterLayer create();

    public abstract ClusterLayer addTo(LeafletMap map);

}
