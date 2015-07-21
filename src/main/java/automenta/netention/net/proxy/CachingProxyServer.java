/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.netention.net.proxy;


import automenta.netention.NObject;
import automenta.netention.geo.SpimeBase;
import automenta.netention.net.HttpCache;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static automenta.netention.web.Web.send;

/**
 * Proxy server which caches requests and their data to files on disk
 */
public class CachingProxyServer extends PathHandler {

    public static final Logger logger = LoggerFactory.getLogger(CachingProxyServer.class);

    private final HttpCache cache;
    private final SpimeBase db;



    public CachingProxyServer(SpimeBase db, String cachePath) {
        this(db, new HttpCache(cachePath));
    }

    public CachingProxyServer(SpimeBase db, HttpCache h) {
        super(16);
        this.db = db;
        this.cache = h;
    }


    //final static int threads = 8;
    //final ExecutorService executor = Executors.newFixedThreadPool(threads);




    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }


        String url = exchange.getQueryString();
        if (url == null || url.isEmpty()) {
            send("empty", exchange);
            exchange.endExchange();
            return;
        }

        url = HttpCache.decodeURIComponent(url);



        NObject n = null;
        try {
            n = db.get(url);
        }
        catch (Exception e) {
            //TODO remove need for this catch
        }


        String target;

        long ttl;

        if (n == null) {
            //raw URL
            target = url;
            ttl = 2L * 60 * 60 * 1000; //default
        }
        else {

            String suffix = "";
            if (url.contains("/")) {
                int i = url.indexOf("/");
                if (i < url.length()-1) {
                    suffix = url.substring(i + 1 /* +1 to exclude the '/' */);
                    //url = url.substring(0, i);
                }
            }

            Boolean canProxy = n.get("P");
            if (canProxy != true) {
                send(n.getId() + " proxying disabled", exchange);
                exchange.endExchange();
                return;
            }


            url = n.get("G");
            if (url == null) {
                send(n.getId() + " missing PmaxAge", exchange);
                exchange.endExchange();
                return;
            }

            Number maxAge = n.get("PmaxAge");
            if (maxAge == null) {
                send(n.getId() + " missing PmaxAge", exchange);
                exchange.endExchange();
                return;
            }

            target = url + suffix;
            ttl = maxAge.longValue();
        }


        cache.get(target, new Consumer<HttpCache.CachedURL>() {

            @Override
            public void accept(HttpCache.CachedURL response) {
                if (response != null) {
                    response.send(exchange);
                }
                else {
                    exchange.setResponseCode(404);
                    exchange.endExchange();
                }

            }
        }, ttl);

    }

}
