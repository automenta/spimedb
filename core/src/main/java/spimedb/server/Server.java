package spimedb.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import jcog.data.list.Lst;
import jcog.io.Twokenize;
import jcog.net.http.HttpConnection;
import jcog.net.http.HttpModel;
import jcog.net.http.HttpServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.index.Search;
import spimedb.query.Query;
import spimedb.util.JSON;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Server implements HttpModel {
    public final SpimeDB db;
    public final HttpServer server;

    public Server(SpimeDB db) {
        this.db = db;
        server = new HttpServer("localhost", 8080, this);
    }

    @Override
    public void wssOpen(WebSocket ws, ClientHandshake handshake) {
        //ws.send("hi");
        System.out.println("OPEN " + ws);
    }

    @Override
    public void wssMessage(WebSocket ws, String message) {


        JsonNode m;
        try {
            m = JSON.fromJSON(message);
        } catch (JsonProcessingException e) {
            return;
        }
        switch (m.get("_").asText()) {
            case "index" ->
                /*
                    { _: 'index' }
                    Get tag index, ontology, schema, etc..
                */
                ws.send(JSON.toJSONString(db.facets(NObject.TAG, 32).labelValues));
            case "earth" -> {
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
                var B = m.get(NObject.BOUND);
                Query q = new Query().where(
                        B.get(0).asDouble(), B.get(1).asDouble(),
                        B.get(2).asDouble(), B.get(3).asDouble());

                Search r = q.start(db);
                List<NObject> found = new Lst<>();
                r.forEach((d, s) -> found.add(new MutableNObject(d)), 0, () -> {
                    if (!found.isEmpty())
                        ws.send(JSON.toJSONString(found));
//                    assertEquals(1, db.size());
//                    assertNotNull(r.localDocs);
//                    assertEquals(1, r.localDocs.totalHits.value);
//                    assertFalse(found.isEmpty());
//                    assertEquals(dplace.toString(), found.get(0).toString());
//                    assertTrue(String.valueOf(found), found.contains(place));
                });
            }
            default -> {
            }
        }
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
        URI url = h.url();
//
        String path = url.getPath();
//        try {
            if (path.equals("/"))
                path = "/index.html";

            h.respond(
                //new File(Server.class.getResource(path).toURI())
                new File("./src/main/resources" + path)
            );
//        } catch (URISyntaxException e) {
//            //throw new RuntimeException(e);
//            h.respond("404");
//        }
        ;
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
        new Server(new SpimeDB()).server.fps(30);
    }

}
