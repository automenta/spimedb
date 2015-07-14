package automenta.netention.run;


import automenta.netention.Core;
import automenta.netention.geo.ImportKML;
import automenta.netention.geo.SpimeBase;
import automenta.netention.net.NObject;
import automenta.netention.web.ClientResources;
import automenta.netention.web.Web;
import automenta.netention.web.WebSocketCore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.HashMultimap;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.SpatialContext;
import org.hibernate.search.query.dsl.SpatialTermination;
import org.hibernate.search.query.dsl.Unit;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.FetchOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

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

    public CacheQuery queryCircle(double lat, double lon, double radMeters, int maxResults) {

        //Bounds
        //SpatialMatchingContext whereQuery = base.find().spatial().onField("nobject");
        SpatialContext whereQuery = base.objSearch.buildQueryBuilderForClass(NObject.class).get().spatial();


        SpatialTermination qb = whereQuery.
                within(radMeters / 1000.0, Unit.KM)
                .ofLatitude(lat)
                .andLongitude(lon);


        if (qb != null) {
            Query c = qb.createQuery();

            CacheQuery cq = base.find(c).maxResults(maxResults);
            return cq;
        }
        return null;

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

                send(Core.json.writeValueAsString(base.ext.keySet()), exchange);
            }
        });

        //TODO add parametres for root & how many levels
        add("/obj/inside/", new HttpHandler() {

            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {

                String rp = ex.getRelativePath();
                if (rp.length() > 0) {
                    String nobjectID = rp.substring(1); //removes leading '/'

                    send(Core.json.writeValueAsString(base.ext.get(nobjectID)), ex);
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
                    CacheQuery cq = queryCircle(ex.getQueryParameters());
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
                    CacheQuery cq = queryCircle(ex.getQueryParameters());
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

    private CacheQuery queryCircle(Map<String, Deque<String>> q) {
        /* Circle("x", "y", "m")
                            x = lon
                            y = lat
                            m = radius (meters)
                    */
        double lon = Double.valueOf(q.get("x").getFirst().toString());
        double lat = Double.valueOf(q.get("y").getFirst().toString());
        double radMeters = Double.valueOf(q.get("r").getFirst().toString());

        final int maxResults = MAX_QUERY_RESULTS;

        return queryCircle(lat, lon, radMeters, maxResults);
    }

    public void sendSummaryResults(CacheQuery cq, HttpServerExchange ex, int maxResults) {
        HashMultimap<String,String> m = HashMultimap.create();

        Iterator i = cq.iterator(new FetchOptions().fetchMode(FetchOptions.FetchMode.LAZY).fetchSize(maxResults));
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

    public void sendFullResults(CacheQuery cq, HttpServerExchange ex, int maxResults) {
        //cq.maxResults(maxResults).sort(Sort.RELEVANCE);

        if (cq.getResultSize() > 0) {
            Iterator i = cq.iterator(new FetchOptions().fetchMode(FetchOptions.FetchMode.LAZY).fetchSize(maxResults));

            send(o -> {
                try {
                    Core.json.writeArrayValues(o, i, ',');
                    o.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, ex);
        } else {
            //no results
            ex.endExchange();
        }
    }


    public static void main(String[] args) throws IOException {
        SpimeBase es = SpimeBase.disk("/tmp/sf", 128 * 1024);


        if (es.isEmpty()) {
            System.out.println("Initializing database...");

            String[] urls = new String[]{
                    "file:///home/me/kml/EOL-Field-Projects-CV3D.kmz",
                    "file:///home/me/kml/GVPWorldVolcanoes-List.kmz",
                    "file:///home/me/kml/submarine-cables-CV3D.kmz",
                    "file:///home/me/kml/fusion-landing-points-CV3D.kmz",
                    "file:///home/me/kml/CV-Reports-October-2014-Climate-Viewer-3D.kmz"
            };

            for (String u : urls) {
                new ImportKML(es).url(u).run();
            }

        }

        System.out.println("Indices: " + es.getStatistics().indexedEntitiesCount());


        //System.out.println(es.size() + " objects loaded");


        new SpimeServer(es).start("localhost", 8080);
    }
}
