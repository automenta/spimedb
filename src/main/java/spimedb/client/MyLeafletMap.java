package spimedb.client;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.dom.html.HTMLElement;
import spimedb.client.leaflet.*;

import java.util.Arrays;

import static spimedb.client.JS.get;
import static spimedb.client.JS.getFloat;


public class MyLeafletMap {

    private final Client client;
    private final ChangeBatcher<NObj, Layer> differ;
    private final LeafletMap map;
    private final JsConsumer mapChanged;

    public MyLeafletMap(Client client, HTMLElement mapContainer) {
        this.client = client;
    /*
     * TODO
        continuousWorld: true,
        worldCopyJump: true
     */
        map = LeafletMap.create(mapContainer, LeafletMapOptions.create());

        map.setView(LatLng.create(40, -80), 8);
        mapChanged = (e) -> {
            JSObject bounds = map.getBounds();

            //parse this kind of noise:
            // {"_southWest":{"lat":39.932380403490875,"lng":-80.50094604492188},"_northEast":{"lat":40.082274490356966,"lng":-79.62203979492189}}
            JSObject sw = get(bounds, "_southWest");
            JSObject ne = get(bounds, "_northEast");

            float[][] b = new float[][]{
                    new float[]{getFloat(sw, "lng"), getFloat(sw, "lat")},
                    new float[]{getFloat(ne, "lng"), getFloat(ne, "lat")}
            };

            client.io.send("me.focusLonLat(" + "[" + Arrays.toString(b[0]) + "," + Arrays.toString(b[1]) + "])");
        };

        TileLayer base = TileLayer.create("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", TileLayerOptions.create())
                .addTo(map);
        //map.onClick((LeafletMouseEvent event) -> click(event.getLatlng()));


        Icon defaultIcon = DivIcon.create("textIcon", "<a>+</a>");
        //doc.createElement("button").withText("X")

        ClusterLayer c = ClusterLayer.create().addTo(map);

        differ = new ChangeBatcher<NObj, Layer>(50, Layer[]::new) {

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
                    //System.out.println(lon + " " + lat);
                    //System.out.println(shown.size() + " " + obj.size());

                    //CircleMarker c = CircleMarker.create(lon, lat, 1, name, "#0f3");
                    Marker m = Marker.create(lon, lat, name, "#0f3");
                    m.setIcon(defaultIcon);

                    return m;


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
