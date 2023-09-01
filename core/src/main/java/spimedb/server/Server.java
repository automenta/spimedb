package spimedb.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import jcog.data.list.Lst;
import jcog.io.Twokenize;
import jcog.net.http.HttpConnection;
import jcog.net.http.HttpModel;
import jcog.net.http.HttpServer;
import org.java_websocket.WebSocket;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.index.DObject;
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
        server = new HttpServer("0.0.0.0", 8080, this);
    }

//    @Override
//    public void wssOpen(WebSocket ws, ClientHandshake handshake) {
//        //ws.send("hi");
//        //System.out.println("OPEN " + ws);
//    }

    @Override
    public void wssMessage(WebSocket ws, String message) {
        JsonNode m;
        try {
            m = JSON.fromJSON(message);
        } catch (JsonProcessingException e) {
            return;
        }

        switch (m.get("_").asText()) {
            case "tag" ->
                /*
                    { _: 'index' }
                    Get tag index, ontology, schema, etc..
                */
                send("tag", db.facets(NObject.TAG, 1024).labelValues, ws);
            case "getAll" -> {
                ArrayNode B = (ArrayNode) m.get("id");
                String[] ids = new String[B.size()]; for (int i = 0; i < ids.length; i++)  ids[i] = B.get(i).asText();
                var found = db.get(ids);
//                B.forEach((ID) -> {
//                    String id = ID.asText();
//                    var d = db.get(id);
//                    if (d!=null)
//                        found.add(d);
//                });
                if (found!=null)
                    send("full", found, ws);
            }

            case "earth" -> {
                /*
                    REQUEST:
                    { _: 'get',
                      earth: [ latMin, lonMin, latMax, lonMax ] //optional
                      time: [tMin, tMax]                        //optional
                      detailMin: [ latEps, lonEps, tEps ]       //optional
                      tagInclude: [ ... ]                       //optional
                      tagExclude: [ ... ]                       //optional
                      output: "id" | "full"
                      //TODO output parameters
                    }
                */
                var B = m.get(NObject.BOUND);
                Query q = new Query().where(
                        B.get(0).asDouble(), B.get(1).asDouble(),
                        B.get(2).asDouble(), B.get(3).asDouble());

                if (m.has("in")) {
                    var ins = (ArrayNode)m.get("in");
                    if (ins.size() > 0) {
                        var inss = Lists.newArrayList(Iterables.transform(ins, JsonNode::asText)).toArray(new String[0]);
                        q.in(inss);
                    }
                }

                Search r = q.start(db);

                var _output = m.get("output");
                String output = _output == null ? "full" : _output.textValue();
                switch (output) {
                    case "full" -> {
                        List<NObject> found = new Lst<>();
                        r.forEach(DObject::get, (d, s) -> found.add(new MutableNObject(d)), () -> {
                            //if (!found.isEmpty())
                                send("full", found, ws);
                        });
                    }
                    case "id" -> {
                        List<String> found = new Lst<>();
                        r.forEach(d -> d.get(NObject.ID), (id, s) -> found.add(id), () -> {
                            //if (!found.isEmpty())
                                send("id", found, ws);
                        });
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void send(String type, Object x, WebSocket s) {
        s.send("{\"" + type + "\":" + JSON.toJSONString(x) + "}"); //HACK
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
        n.withTags(tt);

        //TODO decorate with: geo-ip, etc

        db.add(n);
    }

    @Override
    public void response(HttpConnection h) {
        URI url = h.url();
        String path = url.getPath();
        if (path.equals("/"))
            path = "/index.html";

        h.respond(
            new File("./src/main/resources" + path)
            //new File(Server.class.getResource(path).toURI())
        );
    }

    public static void main(String[] args) {
        new Server(new SpimeDB()).server.fps(30);
    }

}
