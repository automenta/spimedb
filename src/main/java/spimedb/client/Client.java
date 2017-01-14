package spimedb.client;

import spimedb.client.leaflet.*;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.css.ElementCSSInlineStyle;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.json.JSON;

/**
 * SpimeDB Client UI - converted to JS with TeaVM, running in browser
 */
public class Client {

    static void setVisible(ElementCSSInlineStyle element, boolean visible) {
        CSSStyleDeclaration es = element.getStyle();
        if (!visible)
            es.setProperty("display", "none");
        else
            es.removeProperty("display");
    }



    public Client() {
        HTMLDocument document = HTMLDocument.current();
//        HTMLElement div = document.createElement("div");
//        div.appendChild(document.createTextNode("TeaVM generated element"));

        WebSocket ws = WebSocket.newSocket("attn");
        ws.setOnData((msg)->{
            System.out.println(JSON.stringify(msg));
            //document.getBody().appendChild(document.createTextNode(JSON.stringify(msg)));
        });
        ws.setOnOpen(()->{
            ws.send("");

            /*document.getBody().appendChild(div).appendChild(
                    document.createTextNode(JSON.stringify(ws)));*/

        });

        HTMLElement mapContainer = document.createElement("div");
        mapContainer.setAttribute("id", "view");
        document.getBody().appendChild(mapContainer);

        LeafletMap map = LeafletMap.create(mapContainer, LeafletMapOptions.create());
        map.setView(LatLng.create(40, -80), 13);



        TileLayer.create("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", TileLayerOptions.create()
                /*.attribution("&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a> " +
                        "contributors")*/)
                .addTo(map);
        //map.onClick((LeafletMouseEvent event) -> click(event.getLatlng()));
    }

    public static void main(String[] args) {

        new Client();





    }

}
