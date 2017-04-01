package spimedb.client.leaflet;

/**
 * Created by me on 1/17/17.
 */
public interface Layered {

    Layer addTo(LeafletMap map);

    Layer remove();

}
