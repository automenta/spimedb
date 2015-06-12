/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.netention.net;


import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Proxy server which caches requests and their data to files on disk
 * TODO broken since removing async-http-client which depended on netty 3 (we are using netty4 in other dependencies)
 */
public class CachingProxyServer implements HttpHandler {

    public static final Logger logger = LoggerFactory.getLogger(CachingProxyServer.class);


    public final Proxy proxy;
    final String cachePath;

    public static void main(String[] args) throws Exception {
        new CachingProxyServer(8080, "cache");

    }

    final static int threads = 8;
    final ExecutorService executor = Executors.newFixedThreadPool(threads);

    public CachingProxyServer(int port, String cachePath) {


        this.cachePath = cachePath + "/";
        Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setIoThreads(4)
                .setHandler(this)
                .build();
        server.start();

        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(port));

        logger.info("Cache proxy started: " + proxy + ", saving to: " + cachePath);
    }

    public synchronized void caching(String uri, CachedURL c) throws IOException {
        File f = getCacheFile(uri);
        logger.info("Caching " + uri + " to" + f.getAbsolutePath());

        ObjectOutputStream paraficheiro = new ObjectOutputStream(new FileOutputStream(f));
        paraficheiro.writeObject(c);

        paraficheiro.close();
        //cache.put(pedido.URI, f.getAbsolutePath());

    }

    public File getCacheFile(String uri) throws UnsupportedEncodingException {
        String filename = URLEncoder.encode(uri, "UTF-8");
        return new File(cachePath + filename);
    }

    public CachedURL uncaching(String uripedido) throws Exception {

        File ficheirocached = getCacheFile(uripedido);

        ObjectInputStream deficheiro = new ObjectInputStream(new FileInputStream(ficheirocached));

        CachedURL x = (CachedURL) deficheiro.readObject();
            /*bytescached = new byte[(int) ficheirocached.length()];
             deficheiro.read(bytescached);*/
        return x;


    }

    public static class CachedURL implements Serializable {

        public byte[] content;
        public int responseCode;
        public List<String[]> responseHeader; //TODO UTF8

        public CachedURL() {
        }

        public CachedURL(HeaderMap requestHeaders, int responseCode, byte[] content) {
            responseHeader = new ArrayList();
            for (HeaderValues x : requestHeaders) {
                for (String y : x) {
                    responseHeader.add(new String[]{x.toString(), y});
                }
            }
            this.responseCode = responseCode;
            this.content = content;
        }
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        final String url = exchange.getQueryString();

        logger.info("request: " + url);

        CachedURL response = null;

        try {
            response = uncaching(url);
        }
        catch (Exception e) { }

        if (response == null) {

            logger.info("cache miss: " + url);

            response = executor.submit(
                    new Request(
                            new URL(url),
                            exchange.getRequestHeaders(),
                            exchange.getResponseCode()
                    )
            ).get();

            logger.info("cache set: " + response.content.length + " bytes");

        }
        else {
            logger.info("cache hit: " + url);
        }

        if (response == null) {
            logger.error("unable to download " + url);
            exchange.setResponseCode(404);
        }
        else {

            exchange.setResponseCode(response.responseCode);
            for (String[] x : response.responseHeader) {
                exchange.getResponseHeaders().add(new HttpString(x[0]), x[1]);
            }
            exchange.getResponseSender().send(ByteBuffer.wrap(response.content));
            logger.info("sending client cached " + response.content + " bytes");
        }

    }
//
//    public static void setHeader(HttpServerExchange exchange, FluentCaseInsensitiveStringsMap response) {
//        for (Map.Entry<String, List<String>> entry : response.entrySet()) {
//            if (!entry.getValue().isEmpty()) {
//                exchange.getResponseHeaders().put(new HttpString(entry.getKey()),
//                        entry.getValue().iterator().next());
//            }
//        }
//    }

    public class Request implements Callable<CachedURL> {
        private final HeaderMap request;
        int response;
        private URL url;
        private CachedURL curl;


        public Request(URL url, HeaderMap request, int responseCode) {
            this.url = url;
            this.response = responseCode;
            this.request = request;
        }

        @Override
        public CachedURL call() throws Exception {

            try {
                byte[] data = IOUtils.toByteArray(url);


                caching(url.toString(), this.curl =
                        new CachedURL(request, response, data));
            }
            catch (Exception e) {
                logger.error(e.toString());
                return null;
            }

            return curl;
        }
    }

}
