package spimedb.client;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.json.JSON;

/**
 * SpimeDB Client UI - converted to JS with TeaVM, running in browser
 */
public class Client {


    public Client() {
        HTMLDocument document = HTMLDocument.current();
        HTMLElement div = document.createElement("div");
        div.appendChild(document.createTextNode("TeaVM generated element"));

        WebSocket ws = WebSocket.newSocket("attn");
        ws.setOnData((msg)->{
            System.out.println(JSON.stringify(msg));
            document.getBody().appendChild(document.createTextNode(JSON.stringify(msg)));
        });
        ws.setOnOpen(()->{
            ws.send("");

            document.getBody().appendChild(div).appendChild(
                    document.createTextNode(JSON.stringify(ws)));

        });
    }

    public static void main(String[] args) {

        new Client();





    }

}
