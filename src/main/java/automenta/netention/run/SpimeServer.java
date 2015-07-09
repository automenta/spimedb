package automenta.netention.run;


import automenta.netention.geo.ImportKML;
import automenta.netention.geo.SpimeBase;
import automenta.netention.web.ClientResources;
import automenta.netention.web.Web;
import automenta.netention.web.WebSocketCore;
import com.fasterxml.jackson.databind.JsonNode;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.Unit;
import org.infinispan.query.CacheQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SpimeServer extends Web {

    public class SpimeSocket extends WebSocketCore {

        public SpimeSocket() {
            super(true);
        }

        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {
            super.onConnect(exchange, socket);


            base.forEach( x -> send(socket, x) );


            Query c = base.find().spatial().onField("where").
                    within(500, Unit.KM).ofLatitude(40).andLongitude(-80).createQuery();


            CacheQuery cq = base.find(c);

            System.out.println(cq.list());
            System.out.println(cq.getResultSize());



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

        this.base = s;

    }


    public static void main(String[] args) throws IOException {
        SpimeBase es = SpimeBase.disk("/tmp/sb", 128*1024);


        if (es.isEmpty()) {
            System.out.println("Initializing database...");

            new ImportKML(es).url("main",
                    "file:///tmp/kml/EOL-Field-Projects-CV3D.kmz"
                    //"file:///tmp/kml/GVPWorldVolcanoes-List.kmz"
            ).run();

        }

        System.out.println(es.size() + " objects loaded");


        new SpimeServer(es).start("localhost", 8080);
    }
}
