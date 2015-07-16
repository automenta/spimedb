package automenta.netention.run;


import automenta.netention.Core;
import automenta.netention.NObject;
import automenta.netention.geo.SpimeBase;
import automenta.netention.web.ClientResources;
import automenta.netention.web.Web;
import automenta.netention.web.WebSocketCore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.HashMultimap;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SpimeServer extends Web {

    final int MAX_QUERY_RESULTS = 256;

    public class SpimeSocket extends WebSocketCore {

        public SpimeSocket() {
            super(true);
        }

        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {
            super.onConnect(exchange, socket);


//            base.forEach( x -> send(socket, x) );
//
//
//            Query c = base.find().spatial().onField("where").
//                    within(500, Unit.KM).ofLatitude(40).andLongitude(-80).createQuery();
//
//
//            CacheQuery cq = base.find(c);
//
//            System.out.println(cq.list());
//            System.out.println(cq.getResultSize());


        }

        @Override
        protected void onJSONMessage(WebSocketChannel socket, JsonNode j) {
            super.onJSONMessage(socket, j);
        }
    }


    public static final Logger log = LoggerFactory.getLogger(SpimeServer.class);

    private final SpimeBase base;

    public SpimeServer(SpimeBase s) {
        super();


        //static content
        add("/", ClientResources.handleClientResources());

        //websocket
        add("/ws", new SpimeSocket().get());


        add("/index", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {

                send(Core.json.writeValueAsString(base.getInsides(null)), exchange);
            }
        });

        //TODO add parametres for root & how many levels
        add("/obj/inside/", new HttpHandler() {

            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {

                String rp = ex.getRelativePath();
                if (rp.length() > 0) {
                    String nobjectID = rp.substring(1); //removes leading '/'

                    send(Core.json.writeValueAsString(base.getInsides(nobjectID)), ex);
                }
                /*send(o -> {
                    PrintStream p = new PrintStream(o);
                    Iterator<Map.Entry<String, Collection<String>>> ie = base.ext.entrySet().iterator();
                    while (ie.hasNext()) {
                        Map.Entry<String, Collection<String>> e = ie.next();
                        p.print(e.getKey() + ": " + e.getValue() + "\n");
                    }
                    p.flush();
                }, exchange);*/
            }
        });

        add("/obj/", new HttpHandler() {

            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {

                String nobjectID = ex.getRelativePath().substring(1); //removes leading '/'
                NObject n = base.get(nobjectID);
                if (n == null)
                    ex.setResponseCode(404);
                else {
                    send(n.toString(), ex);
                }
            }
        });

        add("/planet/earth/region2d/circle/summary", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {
                try {
                    Iterator cq = queryCircle(ex.getQueryParameters());
                    if (cq != null) {
                        sendSummaryResults(cq, ex, MAX_QUERY_RESULTS);
                    } else {
                        //invalid query or other server error
                        ex.setResponseCode(500);
                        ex.endExchange();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        add("/planet/earth/region2d/circle/detail", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {

                try {
                    Iterator cq = queryCircle(ex.getQueryParameters());
                    if (cq != null) {
                        sendFullResults(cq, ex, MAX_QUERY_RESULTS);
                    } else {
                        //invalid query or other server error
                        ex.setResponseCode(500);
                        ex.endExchange();
                    }

                    //                String[] sections = ex.getRelativePath().split("/");
                    //                String mode = sections[1];
                    //                String query = sections[2];
                    //                handleRequest(mode, query, ex);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        this.base = s;

    }

    private Iterator<NObject> queryCircle(Map<String, Deque<String>> q) {
        /* Circle("x", "y", "m")
                            x = lon
                            y = lat
                            m = radius (meters)
                    */
        double lon = Double.valueOf(q.get("x").getFirst().toString());
        double lat = Double.valueOf(q.get("y").getFirst().toString());
        double radMeters = Double.valueOf(q.get("r").getFirst().toString());

        final int maxResults = MAX_QUERY_RESULTS;
        Iterator<NObject> cq = base.get( lat, lon, radMeters, maxResults );
        return cq;
    }

    public void sendSummaryResults(Iterator i, HttpServerExchange ex, int maxResults) {
        HashMultimap<String,String> m = HashMultimap.create();

        StringBuilder sb = new StringBuilder(128);
        while (i.hasNext()) {
            NObject n = (NObject)i.next();

            String ss = n.summary(sb);

            /*for (String p : n.inside()) {
                m.put(p, ss);
            }*/
            m.put(n.inside(), ss);
        }



        send(o -> {

            PrintStream p = new PrintStream(o);

            try {
                p.print('{');

                boolean first = true;
                for (String path : m.keys()) {
                    Set<String> s = m.get(path);
                    String items = String.join(",", s); //TODO output directly

                    if (!first) {
                        p.append(',');
                    }
                    else
                        first = false;

                    p.append('\"').append(path).append("\":[");

                    p.append(items).append("]");

                }

                p.print('}');
                p.flush();
            }
            catch (Exception e) {
                e.printStackTrace();
            }


//            try {
//
//                Core.json.writeValue(o, m);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }, ex);

    }

    public void sendFullResults(Iterator<NObject> i, HttpServerExchange ex, int maxResults) {
        //cq.maxResults(maxResults).sort(Sort.RELEVANCE);


            send(o -> {
                try {
                    Core.json.writeArrayValues(o, i, ',');
                    o.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, ex);

        /*} else {
            //no results
            ex.endExchange();
        }*/
    }

}
