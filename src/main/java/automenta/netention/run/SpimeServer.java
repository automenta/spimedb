package automenta.netention.run;


import automenta.netention.Core;
import automenta.netention.geo.ImportKML;
import automenta.netention.geo.SpimeBase;
import automenta.netention.net.NObject;
import automenta.netention.web.ClientResources;
import automenta.netention.web.Web;
import automenta.netention.web.WebSocketCore;
import com.fasterxml.jackson.databind.JsonNode;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.hibernate.search.query.dsl.SpatialContext;
import org.hibernate.search.query.dsl.SpatialTermination;
import org.hibernate.search.query.dsl.Unit;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.FetchOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

public class SpimeServer extends Web {

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

    public SpimeServer(SpimeBase s)  {
        super();

        //static content
        add("/", ClientResources.handleClientResources());

        //websocket
        add("/ws", new SpimeSocket().get());

        add("/space", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {

                try {

                    Map<String, Deque<String>> q = ex.getQueryParameters();

                    //TODO handle planet ("p")



                    SpatialTermination qb = null;

                    String regionShape = q.get("R").getFirst().toString();



                    if (regionShape != null) {

                        //Bounds
                        //SpatialMatchingContext whereQuery = base.find().spatial().onField("nobject");
                        SpatialContext whereQuery = base.search.buildQueryBuilderForClass(NObject.class).get().spatial();



                        switch (regionShape) {
                            case "c":
                                /* Circle("x", "y", "m")
                                        x = lon
                                        y = lat
                                        m = radius (meters)
                                */
                                double lon = Double.valueOf(q.get("x").getFirst().toString());
                                double lat = Double.valueOf(q.get("y").getFirst().toString());
                                double radMeters = Double.valueOf(q.get("r").getFirst().toString());

                                qb = whereQuery.
                                        within(radMeters / 1000.0, Unit.KM)
                                        .ofLatitude(lat)
                                        .andLongitude(lon);
                                break;

                        }

                    }



                    if (qb != null) {
                        Query c = qb.createQuery();

                        final int MAX_QUERY_RESULTS = 128;

                        CacheQuery cq = base.find(c);
                        cq.maxResults(MAX_QUERY_RESULTS).sort(Sort.RELEVANCE);


                        if (cq.getResultSize() > 0) {
                            //StringBuilder sb = new StringBuilder();

                            Iterator i = cq.iterator(new FetchOptions().fetchMode(FetchOptions.FetchMode.LAZY).fetchSize(MAX_QUERY_RESULTS));

                            send(o -> {
                                try {
                                    Core.json.writeArrayValues(o, i, ',');
                                    o.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
//                                while (i.hasNext()) {
//                                    NObject r = (NObject)i.next();
//                                    o.write(r.toBytes(true));
//                                    //sb.append(r.toString());
//                                }

                            }, ex);

                            //send(sb.toString(), ex);
                        }
                        else {
                            //no results
                            ex.endExchange();
                        }

                    } else {
                        //invalid query or other server error
                        ex.setResponseCode(500);
                        ex.endExchange();
                    }


                    //                String[] sections = ex.getRelativePath().split("/");
                    //                String mode = sections[1];
                    //                String query = sections[2];
                    //                handleRequest(mode, query, ex);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        this.base = s;

    }


    public static void main(String[] args) throws IOException {
        SpimeBase es = SpimeBase.disk("/tmp/sf", 128*1024);


        if (es.isEmpty()) {
            System.out.println("Initializing database...");

            String[] urls = new String[] {
                "file:///tmp/kml/EOL-Field-Projects-CV3D.kmz",
                "file:///tmp/kml/GVPWorldVolcanoes-List.kmz",
                "file:///tmp/kml/submarine-cables-CV3D.kmz",
                "file:///tmp/kml/fusion-landing-points-CV3D.kmz",
                "file:///tmp/kml/CV-Reports-October-2014-Climate-Viewer-3D.kmz"
            };

            for (String u : urls) {
                new ImportKML(es).url(u).run();
            }

        }

        System.out.println(es.getStatistics().getIndexedClassNames());
        System.out.println(es.getStatistics().indexedEntitiesCount());

        System.out.println(es.size() + " objects loaded");


        new SpimeServer(es).start("localhost", 8080);
    }
}
