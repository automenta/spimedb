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
            if (logger.isDebugEnabled())
                logger.debug(url + " layer unknown");
            exchange.endExchange();
            return;
        }

        url = HttpCache.decodeURIComponent(url);

        String suffix = "";
        if (url.contains("/")) {
            int i = url.indexOf("/");
            suffix = url.substring(i);
            url = url.substring(0, i);
        }

        NObject n = db.get(url);
        Boolean canProxy = n.get("P");
        if (canProxy != true) {
            if (logger.isDebugEnabled())
                logger.debug(n + " can not be proxied");

            exchange.endExchange();
            return;
        }

        url = n.get("G");
        if (url == null) {
            logger.warn(n + " missing URL");
            exchange.endExchange();
            return;
        }

        Number maxAge = n.get("PmaxAge");
        if (maxAge == null) {
            logger.warn(n + " missing PmaxAge");
            exchange.endExchange();
            return;
        }


        cache.get(url + suffix, new Consumer<HttpCache.CachedURL>() {

            @Override
            public void accept(HttpCache.CachedURL response) {
                response.send(exchange);
            }
        }, maxAge.longValue());

    }

}
