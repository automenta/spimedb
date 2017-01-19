package spimedb.client;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.dom.html.HTMLElement;
import spimedb.bag.ChangeBatcher;
import spimedb.client.leaflet.*;
import spimedb.client.util.JS;
import spimedb.client.util.JsConsumer;

import java.util.Arrays;

/** Leaflet map widget */
public class Map2D {

    //TODO add element resize handler

    private final Client client;
    private final ChangeBatcher<NObj, Layer> differ;
    private final LeafletMap map;
    private final JsConsumer mapChanged;
    private final TileLayer base;

    /** display update perid (milliseconds). changes that arrive in between updates are batched with ChangeBatcher */
    public static final int UPDATE_MS = 40;

    public Map2D(Client client, HTMLElement mapContainer) {
        this.client = client;

        map = LeafletMap.create(mapContainer, LeafletMapOptions.create().continuousWorld(true).worldCopyJump(true));
        map.setView(LatLng.create(40, -80), 8);

        mapChanged = (e) -> {
            LatLngBounds bounds = map.getBounds();

            //parse this kind of noise:
            // {"_southWest":{"lat":39.932380403490875,"lng":-80.50094604492188},"_northEast":{"lat":40.082274490356966,"lng":-79.62203979492189}}
            LatLng sw = bounds.getSouthWest();
            LatLng ne = bounds.getNorthEast();

            float[][] b = new float[][]{
                    new float[]{ (float)sw.getLng(), (float)sw.getLat() },
                    new float[]{ (float)ne.getLng(), (float)ne.getLat() }
            };

            client.io.send("me.focusLonLat(" + '[' + Arrays.toString(b[0]) + ',' + Arrays.toString(b[1]) + "])");
        };

        base = TileLayer.create("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", TileLayerOptions.create())
                .addTo(map);

        Icon defaultIcon = DivIcon.create("textIcon", "<a>+</a>");

        ClusterLayer c = ClusterLayer.create().addTo(map);

        differ = new ChangeBatcher<NObj, Layer>(UPDATE_MS, Layer[]::new) {

            @Override
            public void update(Layer[] added, Layer[] removed) {
                //System.out.println(Arrays.toString(added) + " - " + Arrays.toString(removed));
                c.removeLayers(removed);
                c.addLayers(added);
                //System.out.println(differ.built.size() + " " + obj.size());
                client.obj.mul(0.95f); //forgetting
            }

            @Override
            public Layer build(NObj N) {
                JSObject n = N.data;

                JSArray bounds = JS.getArray(n, "@");
                if (bounds == null)
                    return null;

                JSObject time = bounds.get(0);
                JSObject x = bounds.get(1); //LON
                JSObject y = bounds.get(2); //LAT
                JSObject z = bounds.get(3);

                String nid = N.id;
                String name = JS.getString(n, "N", nid);

                if (JS.isNumber(x) && JS.isNumber(y)) {
                    //POINT

                    //shown.computeIfAbsent(N.id, id -> {


                    float lon = JS.toFloat(x);
                    float lat = JS.toFloat(y);

                    if ((lon==lon && lat==lat)) {
                        //System.out.println(lon + " " + lat);
                        //System.out.println(shown.size() + " " + obj.size());

                        //CircleMarker c = CircleMarker.create(lon, lat, 1, name, "#0f3");
                        Marker m = Marker.create(lon, lat, name, "#0f3");
                        m.setIcon(defaultIcon);

                        return m;
                    } else {
                        return null;
                    }
                }

                return null;
            }
        };


        //attach these events when the map shows and hides
        client.obj.ADD.on(differ::add);
        client.obj.REMOVE.on(differ::remove);

        map.onZoomEnd(mapChanged);
        map.onMoveEnd(mapChanged);

        //mapChange.accept(null); //<- call this after websockets init

        //map.onShow(..)
        //map.onHide(..)

    }
}
