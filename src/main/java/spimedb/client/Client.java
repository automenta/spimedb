package spimedb.client;

import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.css.ElementCSSInlineStyle;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import spimedb.bag.BudgetMerge;
import spimedb.bag.ObservablePriBag;
import spimedb.client.util.Console;
import spimedb.client.websocket.WebSocket;

import java.util.HashMap;

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

        HTMLElement mapContainer = doc.createElement("div");
        mapContainer.setAttribute("id", "view");
        doc.getBody().appendChild(mapContainer);

        new Map2D(this, mapContainer);

        io.setOnOpen(this::init);
    }


    public static void main(String[] args) {

        new Client();


    }

}
