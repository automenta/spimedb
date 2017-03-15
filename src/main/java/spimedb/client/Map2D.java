package spimedb.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSFunction;
import org.teavm.jso.dom.html.HTMLElement;
import spimedb.NObject;
import spimedb.client.leaflet.*;
import spimedb.client.lodash.Lodash;
import spimedb.client.util.JS;
import spimedb.client.util.JsConsumer;
import spimedb.util.bag.ChangeBatcher;

import java.util.Arrays;

/**
 * Leaflet map widget
 */
public class Map2D {

    //TODO add element resize handler

    private final Client client;
    private final ChangeBatcher<NObj, Layer> differ;
    private final LeafletMap map;
    private final JsConsumer mapChanged;
    private final TileLayer base;

    /**
     * display update perid (milliseconds). changes that arrive in between updates are batched with ChangeBatcher
     */
    public static final int REDRAW_MS = 25;

    /**
     * refresh update period
     */
    public static final int UPDATE_MS = 50;

    private final JSFunction refresh;

    protected Icon defaultIcon = DivIcon.create("textIcon", "<a>+</a>");

    public Map2D(Client client, HTMLElement mapContainer) {
        this.client = client;

        map = LeafletMap.create(mapContainer, LeafletMapOptions.create().continuousWorld(true).worldCopyJump(true));
        map.setView(LatLng.create(40, -80), 8);

        refresh = Lodash.debounce(this::refresh, UPDATE_MS);

        mapChanged = (e) -> {
            refresh.call(null);
        };

        base = TileLayer.create("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", TileLayerOptions.create())
                .addTo(map);


        ClusterLayer c = ClusterLayer.create().addTo(map);

        differ = new ChangeBatcher<NObj, Layer>(REDRAW_MS, Layer[]::new) {

            @Override
            public void update(Layer[] added, Layer[] removed) {
                //System.out.println(Arrays.toString(added) + " - " + Arrays.toString(removed));
                c.removeLayers(removed);

                forget();

                c.addLayers(added);
                //System.out.println(differ.built.size() + " " + obj.size());
            }

            @Override
            public Layer build(NObj N) {
                JSObject n = N.data;

                JSArray bounds = JS.getArray(n, "@");
                if (bounds == null)
                    return null;

                return Map2D.this.build(N, bounds);
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

    @Nullable
    protected Layer build(NObj N, JSArray bounds) {
        JSObject time = bounds.get(0);
        JSObject x = bounds.get(1); //LON
        JSObject y = bounds.get(2); //LAT
        JSObject z = bounds.get(3);

        String nid = N.id;
        String name = JS.getString(N.data, "N", nid);


        JSObject lineString = JS.get(N.data, NObject.LINESTRING);
        if (lineString != null) {
            return Polyline.create(points(lineString));
        }


        JSObject gonString = JS.get(N.data, NObject.POLYGON);
        if (gonString != null) {
            return Polygon.create(points(gonString));
        }


        if (JS.isNumber(x) && JS.isNumber(y)) {
            //POINT

            //shown.computeIfAbsent(N.id, id -> {


            float lon = JS.toFloat(x);
            float lat = JS.toFloat(y);

            if ((lon == lon && lat == lat)) {
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

    @NotNull
    private static LatLng[] points(JSObject lineString) {
        JSArray points = lineString.cast();
        int l = points.getLength();
        LatLng[] pp = new LatLng[l];
        for (int i = 0; i < l; i++) {
            JSArray np = points.get(i).cast();
            pp[i] = LatLng.create(JS.getFloat(np, 0), JS.getFloat(np, 1));
        }
        return pp;
    }

    final float invisibleForget = 0.9f;

    protected void refresh() {
        LatLngBounds bounds = map.getBounds();

        forget();


        //parse this kind of noise:
        // {"_southWest":{"lat":39.932380403490875,"lng":-80.50094604492188},"_northEast":{"lat":40.082274490356966,"lng":-79.62203979492189}}
        LatLng sw = bounds.getSouthWest();
        LatLng ne = bounds.getNorthEast();

        float[][] b = new float[][]{
                new float[]{(float) sw.getLng(), (float) sw.getLat()},
                new float[]{(float) ne.getLng(), (float) ne.getLat()}
        };

        client.sync();
        client.io.send("me.focusLonLat(" + '[' + Arrays.toString(b[0]) + ',' + Arrays.toString(b[1]) + "])");
    }

    private void forget() {
        LatLngBounds bounds = map.getBounds();
        this.differ.forEach((k,v)->{
            boolean forget = false;
            if (v instanceof Marker) {
                if (!bounds.contains(((Marker)v).getLatLng()))
                    forget = true;
            } else if (v instanceof Polyline) {
                if (!bounds.intersects(((Polyline)v).getBounds()))
                    forget = true;
            } else if (v instanceof Polygon) {
                if (!bounds.intersects(((Polygon)v).getBounds()))
                    forget = true;
            }
            if (forget)
                client.obj.mul(k, invisibleForget);
        });
        client.obj.sort();
    }

}
