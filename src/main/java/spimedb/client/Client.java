package spimedb.client;

import org.teavm.jso.JSObject;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.css.ElementCSSInlineStyle;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import spimedb.bag.BudgetMerge;
import spimedb.bag.ObservablePriBag;
import spimedb.client.leaflet.*;

import java.util.Arrays;
import java.util.HashMap;

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
    public final ObservablePriBag<NObj> obj = new ObservablePriBag<>(128, BudgetMerge.add, new HashMap<>());


    public final ClientWebSocket attn = ClientWebSocket.newSocket("attn");
    public final ClientWebSocket shell = ClientWebSocket.newSocket("shell");


    protected void init() {

        shell.send("db");

            /*document.getBody().appendChild(div).appendChild(
                    document.createTextNode(JSON.stringify(ws)));*/
    }


    public Client() {

        this.doc = HTMLDocument.current();

        attn.onTextJSON((x) -> {
            NObj nx = NObj.fromJSON( x );
            if (nx!=null) {
                obj.put(nx, 0.5f);
                //Console.log(x);
//                if (obj.put(nx, 0.5f) != null) {
//                    //System.out.println("#=" + obj.size() + ": " + nx);
//                }
            }
        });


        shell.setOnOpen(this::init);
        shell.onText(Console::log);

        HTMLElement mapContainer = doc.createElement("div");
        mapContainer.setAttribute("id", "view");
        doc.getBody().appendChild(mapContainer);
        newLeafletMap(mapContainer);
    }

    private LeafletMap newLeafletMap(HTMLElement mapContainer) {

        LeafletMap map = LeafletMap.create(mapContainer, LeafletMapOptions.create());

        map.setView(LatLng.create(40, -80), 8);
        JsConsumer mapChange = (e) -> {
            JSObject bounds = map.getBounds();

            //parse this kind of noise:
            // {"_southWest":{"lat":39.932380403490875,"lng":-80.50094604492188},"_northEast":{"lat":40.082274490356966,"lng":-79.62203979492189}}
            JSObject sw = get(bounds, "_southWest");
            JSObject ne = get(bounds, "_northEast");

            float[][] b = new float[][] {
                new float[] { getFloat(sw, "lng"), getFloat(sw, "lat") },
                new float[] { getFloat(ne, "lng"), getFloat(ne, "lat") }
            };

            attn.send("focusLonLat(" + "[" + Arrays.toString(b[0]) + "," + Arrays.toString(b[1]) + "])");

        };
        map.onLoad(mapChange);
        map.onZoomEnd(mapChange);
        map.onMoveEnd(mapChange);
        //map.onShow(..)
        //map.onHide(..)

        //attach these events when the map shows and hides
        obj.ADD.on(x -> {
            Console.log("+",x.data);
        });
        obj.REMOVE.on(x -> {
            Console.log("-",x.data);
        });

        TileLayer.create("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", TileLayerOptions.create())
                .addTo(map);
        //map.onClick((LeafletMouseEvent event) -> click(event.getLatlng()));

        return map;
    }

    public static void main(String[] args) {

        new Client();


    }

}
