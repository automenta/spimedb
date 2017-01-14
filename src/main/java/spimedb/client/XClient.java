package spimedb.client;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.json.JSON;

/**
 * Created by me on 1/13/17.
 */
public class XClient {



    public static void main(String[] args) {

        HTMLDocument document = HTMLDocument.current();
        HTMLElement div = document.createElement("div");
        div.appendChild(document.createTextNode("TeaVM generated element"));

        WebSocket ws = WebSocket.newSocket("localhost", 8080, "attn");
        ws.onData((msg)->{
            System.out.println(JSON.stringify(msg));

            //TLogger.getAnonymousLogger().info(TString.msg);
            document.getBody().appendChild(document.createTextNode(JSON.stringify(msg)));
        });
        ws.setOnOpen(()->{
            ws.send("");

            document.getBody().appendChild(div).appendChild(
                    document.createTextNode(JSON.stringify(ws)));

        });



    }

}
