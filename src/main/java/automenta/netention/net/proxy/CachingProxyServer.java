/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.netention.net.proxy;


import automenta.netention.net.HttpCache;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.HttpString;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static automenta.netention.web.Web.send;

/**
 * Proxy server which caches requests and their data to files on disk
 */
public class CachingProxyServer extends PathHandler {

    private final HttpCache cache;


    public CachingProxyServer(String cachePath) {
        this(new HttpCache(cachePath));
    }

    public CachingProxyServer(HttpCache h) {

        this.cache = h;

    }


    final static int threads = 8;
    final ExecutorService executor = Executors.newFixedThreadPool(threads);



    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        String url = exchange.getQueryString();

        if (url == null) return;

        url = HttpCache.decodeURIComponent(url);

        cache.get(url, new Consumer<HttpCache.CachedURL>() {

            @Override
            public void accept(HttpCache.CachedURL response) {

                response.send(exchange);



            }
        });

    }

}
