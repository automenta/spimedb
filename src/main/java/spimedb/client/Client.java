package spimedb.client;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSFunction;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.css.ElementCSSInlineStyle;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import spimedb.bag.BudgetMerge;
import spimedb.bag.ObservablePriBag;
import spimedb.client.leaflet.*;
import spimedb.client.lodash.Lodash;
import spimedb.client.websocket.WebSocket;

import java.util.*;
import java.util.function.IntFunction;

import static spimedb.client.JS.get;
import static spimedb.client.JS.getFloat;

/**
 * SpimeDB Client UI - converted to JS with TeaVM, running in browser
 */
public class Client {

    private final HTMLDocument doc;

    static void setVisible(ElementCSSInlineStyle element, boolean visible) {
        CSSStyleDeclaration es = element.getStyle();
        if (!visible)
            es.setProperty("display", "none");
        else
            es.removeProperty("display");
    }


    public final ObservablePriBag<NObj> tag = new ObservablePriBag<>(16, BudgetMerge.max, new HashMap<>());
    public final ObservablePriBag<NObj> obj = new ObservablePriBag<>(64, BudgetMerge.max, new HashMap<>());


    public final WebSocket io = WebSocket.newSocket("attn");


    protected void init() {

        io.send("db"); //get the database summary

            /*document.getBody().appendChild(div).appendChild(
                    document.createTextNode(JSON.stringify(ws)));*/
    }


    public Client() {

        this.doc = HTMLDocument.current();

        io.onJSONBinary((x) -> {
            if (JSString.isInstance(x)) {
                Console.log(x);
            } else {
                NObj nx = NObj.fromJSON(x);
                if (nx != null) {
                    obj.put(nx, 0.5f);
                    //Console.log(x);
                    //                if (obj.put(nx, 0.5f) != null) {
                    //                    //System.out.println("#=" + obj.size() + ": " + nx);
                    //                }
                }
            }
        });

        io.setOnOpen(this::init);

        HTMLElement mapContainer = doc.createElement("div");
        mapContainer.setAttribute("id", "view");
        doc.getBody().appendChild(mapContainer);

        new MyLeafletMap(mapContainer);
    }

    /** batches and notifies listeners about bag changes */
    abstract public static class ChangeBatcher<X> {


        private final JSFunction onChange;
        final Collection<X> toAdd = new ArrayList();
        final Collection<X> toRemove = new ArrayList();

        static final Object[] empty = new Object[0];

        public ChangeBatcher(int updateMS, IntFunction<X[]> arrayBuilder) {

            onChange = Lodash.throttle(()-> {

                X[] removed;
                if (toRemove.isEmpty()) {
                    removed = (X[]) empty;
                } else {
                    removed = toRemove.toArray(arrayBuilder.apply(toRemove.size()));
                    toRemove.clear();
                }

                X[] added;
                if (toAdd.isEmpty()) {
                    added = (X[]) empty;
                } else {
                    added = toAdd.toArray(arrayBuilder.apply(toAdd.size()));
                    toAdd.clear();
                }

                if (added!=empty || removed!=empty)
                    update(added, removed);

            }, updateMS);
        }

        abstract public void update(X[] added, X[] removed);

        public void add(X m) {
            toAdd.add(m);
            //if (exact)
                //toRemove.remove(m);
            onChange.call(null);
        }

        public void remove(X m) {
            toRemove.add(m);
            //if (exact)
                //toAdd.remove(m);
            onChange.call(null);
        }
    }


    public class MyLeafletMap {

        final Map<String, Layer> shown = new HashMap();

        private final ChangeBatcher<Layer> differ;
        private final LeafletMap map;
        private final JsConsumer mapChanged;

        public MyLeafletMap(HTMLElement mapContainer) {
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

                float[][] b = new float[][] {
                        new float[] { getFloat(sw, "lng"), getFloat(sw, "lat") },
                        new float[] { getFloat(ne, "lng"), getFloat(ne, "lat") }
                };

                io.send("me.focusLonLat(" + "[" + Arrays.toString(b[0]) + "," + Arrays.toString(b[1]) + "])");
            };

            TileLayer base = TileLayer.create("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", TileLayerOptions.create())
                    .addTo(map);
            //map.onClick((LeafletMouseEvent event) -> click(event.getLatlng()));


            Icon defaultIcon = DivIcon.create("textIcon", "<a>+</a>");
            //doc.createElement("button").withText("X")

            ClusterLayer c = ClusterLayer.create().addTo(map);

            differ = new ChangeBatcher<Layer>(50, Layer[]::new) {
                @Override
                public void update(Layer[] added, Layer[] removed) {
                    //System.out.println(Arrays.toString(added) + " - " + Arrays.toString(removed));
                    c.removeLayers(removed);
                    c.addLayers(added);
                    obj.mul(0.95f); //forgetting
                }
            };


            //attach these events when the map shows and hides
            obj.ADD.on(N -> {
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
                    if (!shown.containsKey(nid)) {

                        float lon = JS.toFloat(x);
                        float lat = JS.toFloat(y);
                        //System.out.println(lon + " " + lat);
                        //System.out.println(shown.size() + " " + obj.size());

                        //CircleMarker c = CircleMarker.create(lon, lat, 1, name, "#0f3");
                        Marker m = Marker.create(lon, lat, name, "#0f3");
                        m.setIcon(defaultIcon);

                        Layer alreadyShown = shown.put(nid, m);
                        if (alreadyShown!=null) {
                            Console.log("already visible: ", N.data);
                        } else {
                            differ.add(m);
                        }
                    }
                }

            });

            obj.REMOVE.on(x -> {
                Layer m = shown.remove(x.id);
                if (m!=null) {
                    //Console.log(JSNumber.valueOf(shown.size()), JSNumber.valueOf(obj.size()));
                    differ.remove(m);
                }
                //for now, it is ok if remove was called on something not shown
            });

            map.onZoomEnd(mapChanged);
            map.onMoveEnd(mapChanged);

            //mapChange.accept(null); //<- call this after websockets init

            //map.onShow(..)
            //map.onHide(..)


        }
    }





    public static void main(String[] args) {

        new Client();


    }

}
