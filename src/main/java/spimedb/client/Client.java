package spimedb.client;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSFunction;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.css.ElementCSSInlineStyle;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Node;
import spimedb.bag.BudgetMerge;
import spimedb.bag.ObservablePriBag;
import spimedb.client.leaflet.Layer;
import spimedb.client.lodash.Lodash;
import spimedb.client.util.Console;
import spimedb.client.websocket.WebSocket;

import java.util.HashMap;
import java.util.function.Consumer;

/**
 * SpimeDB Client UI - converted to JS with TeaVM, running in browser
 */
public class Client {

    private final HTMLDocument doc;

    private final InvalidationNotifier invalidation;

    final static int minForgetPeriodMS = 100;
    private final JSFunction forgetting;

    static void setVisible(ElementCSSInlineStyle element, boolean visible) {
        CSSStyleDeclaration es = element.getStyle();
        if (!visible)
            es.setProperty("display", "none");
        else
            es.removeProperty("display");
    }


    public final ObservablePriBag<NObj> obj = new ObservablePriBag<>(512, BudgetMerge.or, new HashMap<>());


    public final WebSocket io = WebSocket.newSocket("attn");

    protected void init() {

        io.send("me.status()"); //get the database summary
        io.send("me.tagRoots()");

            /*document.getBody().appendChild(div).appendChild(
                    document.createTextNode(JSON.stringify(ws)));*/
    }


    public Client() {

        forgetting = Lodash.throttle(()->{
            obj.mul(0.99f); //forgetting
        }, minForgetPeriodMS);

        this.doc = HTMLDocument.current();

        io.onJSONBinary((x) -> {

            NObj nx = NObj.fromJSON(x);
            if (nx != null) {
                obj.put(nx, nx.isLeaf() ? 0.5f : 1f);

                forgetting.call(null);

                //Console.log(x);
                //                if (obj.put(nx, 0.5f) != null) {
                //                    //System.out.println("#=" + obj.size() + ": " + nx);
                //                }
            } else {
                Console.log(x);
            }

        });

        HTMLElement mapContainer = doc.createElement("div");
        mapContainer.setAttribute("id", "view");
        doc.getBody().appendChild(mapContainer);

        new Map2D(this, mapContainer) {
            @Nullable
            @Override
            protected Layer build(NObj N, JSArray bounds) {
                Layer l = super.build(N, bounds);
                if (l!=null) {
                    obj.put(N, 0.1f); //boost for visibility
                }
                return l;
            }
        };

        new ObjTable(this, doc.getBody()) {

            {
                Window.setInterval(()->{

                    //System.out.println(obj.map.keySet());

                    forEach((n, m) -> {
                        float p = obj.pri(n, 0);
                        ((HTMLElement)m).setAttribute("style", "opacity: " +
                                Math.round(100.0 * (0.5f * 0.5f * p)) + "%");
                    });


                }, 500);
            }

            @Override
            protected Node build(NObj n) {
                Node m = super.build(n);
                return m;
            }
        };

        io.onOpen(this::init);

        invalidation = new InvalidationNotifier();
    }


    /** call this to prepare communications which may change our memory */
    public void sync() {
        if (invalidation.flush()) {
        }
    }

    class InvalidationNotifier implements Consumer<NObj> {

        public static final int DEFAULT_BUFFER_SIZE = 256;

        private final JSFunction sendInvalidations;

        //TODO buffer bytes directly, dont involve String
        StringBuilder invalidated = null;

        /** min period between sending invalidation batches */
        private final int invalidationPeriodMS = 100;

        public InvalidationNotifier() {
            sendInvalidations = Lodash.throttle(this::_flush, invalidationPeriodMS);
            obj.REMOVE.on(this);
        }

        @Override
        public void accept(NObj n) {
            if (invalidated == null)
                invalidated = new StringBuilder(DEFAULT_BUFFER_SIZE).append("me.forgot([");
            invalidated.append('\"').append(n.id).append("\",");
            sendInvalidations.call(null);
        }

        //HACK for TeaVM it has trouble fitting the boolean return value to the void lambda return
        void _flush() {
            flush();
        }

        public boolean flush() {
            StringBuilder b = invalidated;
            if (b == null)
                return false;

            invalidated = null;

            b.setLength(b.length() - 1); //remove trailing comma
            b.append("])");
            String bs = b.toString();
            b.setLength(0); //clear
            io.send(bs);
            return true;
        }
    }

    public static void main(String[] args) {

        new Client();

    }

}
