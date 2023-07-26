package spimedb.server;

import com.google.common.collect.Lists;
import jcog.io.Twokenize;
import jcog.net.http.HttpConnection;
import jcog.net.http.HttpModel;
import jcog.net.http.HttpServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import spimedb.MutableNObject;
import spimedb.SpimeDB;

import java.util.ArrayList;
import java.util.List;

public class Server implements HttpModel {
    public final SpimeDB db;
    private final HttpServer server;

    public Server(SpimeDB db) {
        this.db = db;
        server = new HttpServer("localhost", 8080, this);
    }

    @Override
    public void wssOpen(WebSocket ws, ClientHandshake handshake) {
        //ws.send("hi");
    }

    @Override
    public void wssMessage(WebSocket ws, String message) {
        /*
            { _: 'get',
              earth: [ latMin, lonMin, latMax, lonMax ] //optional
              time: [tMin, tMax]                        //optional
              detailMin: [ latEps, lonEps, tEps ]       //optional
              tagInclude: [ ... ]                       //optional
              tagExclude: [ ... ]                       //optional
              //TODO output parameters
            }
            Get items
        */
        /*
            { _: 'index' }
            Get tag index, ontology, schema, etc..
        */
    }
//    @Override
//    public void wssMessage(WebSocket ws, ByteBuffer message) {
//
//    }

    public void tell(String id, String[] tags, String message) {
        MutableNObject n = new MutableNObject(id, message);

        ArrayList<String> tt = Lists.newArrayList(tags);

        List<Twokenize.Span> sp = Twokenize.twokenize(message);
        for (Twokenize.Span s : sp) {
            String c = s.content.trim();
            switch (s.pattern) {
                case "hashtag":
                    c = c.substring(1); //remove hashtag
                    break;
                case "emoticon":
                case "url":
                case "mention":
                case "email":
                    break;
                default:
                    c = null;
            }
            if (c!=null)
                tt.add(c);
        }

        n.when(System.currentTimeMillis());
        n.withTags(
                //Iterables.concat(
                tt
                //Collections.singletonList(chan.getDestinationAddress().toString())
                //)
        );

        //TODO decorate with: geo-ip, etc

        db.add(n);
    }

    @Override
    public void response(HttpConnection h) {
        //h.respond("");
        //        URI url = h.url();
//
//        String path = url.getPath();
//        switch (path) {
////            case "/teavm/runtime.js":
////                h.respond(new File("/tmp/tea/runtime.js")); //TODO managed temporary directory
////                break;
////            case "/teavm/classes.js":
////                h.respond(new File("/tmp/tea/classes.js")); //TODO managed temporary directory
////                break;
////            case "/websocket.js":
////                h.respond(spacegraph.web.util.WebSocket.websocket_js);
////                break;
////            case "/msgpack.js":
////                h.respond(MsgPack.msgpack_js);
////                break;

    }

    public static void main(String[] args) {
        new Server(new SpimeDB()).server.fps(10);
    }

}
