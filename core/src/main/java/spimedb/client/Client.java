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
import spimedb.client.leaflet.Layer;
import spimedb.client.lodash.Lodash;
import spimedb.client.util.Console;
import spimedb.client.websocket.WebSocket;
import spimedb.util.bag.BudgetMerge;
import spimedb.util.bag.ObservablePriBag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * SpimeDB Client UI - converted to JS with TeaVM, running in browser
 */
public class Client {

    private final HTMLDocument doc;

    private final Refresher refresh;

    final static float forgetRate = 0.98f;
    final static int minForgetPeriodMS = 5;

    private final JSFunction forgetting;

    static void setVisible(ElementCSSInlineStyle element, boolean visible) {
        CSSStyleDeclaration es = element.getStyle();
        if (!visible)
            es.setProperty("display", "none");
        else
            es.removeProperty("display");
    }


    public final ObservablePriBag<NObj> obj = new ObservablePriBag<>(1024, BudgetMerge.or, new HashMap<>());


    public final WebSocket io = WebSocket.newSocket("client");

    protected void init() {

        io.send("me.status()"); //get the database summary
        io.send("me.tagRoots()");

            /*document.getBody().appendChild(div).appendChild(
                    document.createTextNode(JSON.stringify(ws)));*/
    }


    public Client() {

        forgetting = Lodash.debounce(()->{
            obj.mul(forgetRate); //forgetting
        }, minForgetPeriodMS);

        this.doc = HTMLDocument.current();

        io.onJSONBinary((x) -> {

            NObj nx = NObj.fromJSON(x);
            if (nx != null) {
                if (obj.put(nx, nx.isLeaf() ? 0.5f : 1f)!=null) {
                    //acquire supertags
                    for (String s : nx.tags()) {
                        if (!obj.containsKey(s)) {
                            request(s);
                        }
                    }
                }

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

        refresh = new Refresher();
    }

    public void request(String s) {
        if (!s.isEmpty()) //ignore the root
            refresh.request(s);
    }


    /** call this to prepare communications which may change our memory */
    public void sync() {
        refresh.flush();
    }

    class Refresher {

        public static final int DEFAULT_BUFFER_SIZE = 256;

        private final JSFunction ready;

        final Set<String> requested = new HashSet();

        //TODO buffer bytes directly, dont involve String
        StringBuilder forgotten = null;

        /** min period between sending invalidation batches */
        private final int invalidationPeriodMS = 100;

        public Refresher() {
            ready = Lodash.debounce(this::flush, invalidationPeriodMS);
            obj.REMOVE.on(x -> {
                this.forgotten = append(x.id, "me.forgot(", this.forgotten);
                ready();
            });
        }

        private void ready() {
            ready.call(null);
        }

        public void request(String id) {
            if (requested.add('\"' + id + '\"'))
                ready();
        }

        private StringBuilder append(String id, String prefix, StringBuilder f) {
            if (f == null)
                f = new StringBuilder(DEFAULT_BUFFER_SIZE).append(prefix).append("[");
            f.append('\"').append(id).append("\",");
            return f;
        }


        //HACK for TeaVM it has trouble fitting the boolean return value to the void lambda return

        public void flush() {
            StringBuilder b = forgotten;
            if (b != null) {
                forgotten = null;

                b.setLength(b.length() - 1); //remove trailing comma
                b.append("])");
                String bs = b.toString();
                b.setLength(0); //clear
                io.send(bs);
            }


            if (!requested.isEmpty()) {
                String[] r = requested.toArray(new String[requested.size()]);
                requested.clear();
                io.send("me.get(" + Arrays.toString(r) + ")");
            }
        }


    }

    public static void main(String[] args) {

        new Client();

    }

}
