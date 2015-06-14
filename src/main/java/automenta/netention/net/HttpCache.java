package automenta.netention.net;


import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** TODO provide a Cached response result as a File stream
 * rather than loading it into memory and returning the byte[]
 */
public class HttpCache {

    public static final Logger logger = LoggerFactory.getLogger(HttpCache.class);

    final String cachePath;

    final static int threads = 4;
    final ExecutorService executor = Executors.newFixedThreadPool(threads);

    public HttpCache(String cachePath) {
        this.cachePath = cachePath + "/";
    }


    public void get(String url, Consumer<CachedURL> consumer) {
        logger.info("http request: " + url);

        CachedURL response = null;

        //attempt to read, or if expired, just get the newest
        try {
            response = get(url);
        } catch (Exception e) {
        }

        boolean write = true;

        if (response == null) {

            logger.debug("cache miss: " + url);

            try {
                response = executor.submit(
                        new Request(
                                new URL(url)
                        )
                ).get();
            } catch (Exception e) {
                consumer.accept(null);
            }

            logger.info("cache set: " + response.size + " bytes");

            write = true;

        } else {
            logger.info("cache hit: " + url);
            consumer.accept(response);
        }

        if (response == null) {
            logger.error("undownloaded: " + url);
            consumer.accept(null);
        } else {
            consumer.accept(response);
        }

        if (write && response!=null) {
            try {
                put(decodeURIComponent(url.toString()), response);
            }
            catch (Exception e) {
                logger.error(e.toString());
            }
        }

    }

    public static String decodeURIComponent(String s) {
        if (s == null) {
            return null;
        }

        String result = null;

        try {
            result = URLDecoder.decode(s, "UTF-8");
        }

        // This exception should never occur.
        catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }



//    public void run() {
//        logger.info("Web server start:, saving to: " + cachePath);
//        web.build().start();
//    }

    public synchronized void put(String uri, CachedURL c) throws IOException {


        File f = getCacheFile(uri, true);
        logger.info("Caching " + uri + " to" + f.getAbsolutePath());

        ObjectOutputStream ff = new ObjectOutputStream(new FileOutputStream(f));
        ff.writeObject(c);
        ff.close();

        File g = getCacheFile(uri, false);
        FileOutputStream gg = new FileOutputStream(g);
        IOUtils.write(c.content, gg);
        gg.close();

        //cache.put(pedido.URI, f.getAbsolutePath());

    }

    public File getCacheFile(String filename, boolean header) throws UnsupportedEncodingException {
        if (filename.startsWith("https://")) filename = filename.substring(8);
        if (filename.startsWith("http://")) filename = filename.substring(7);
        if (filename.startsWith("http:/")) filename = filename.substring(6);

        filename = URLEncoder.encode(filename, "UTF-8");

        filename = filename.replace("%2F", "/"); //restore '/' for directory seprataion.
        filename = filename.replace("%26", "/&"); //separate at '&' also
        filename = filename.replace("%3F", "/?"); //separate at '&' also
        filename = filename.replace("%3D", "="); //restore '=' this is valid in unix filename
        filename = filename.replace("%2C", ","); //restore ',' this is valid in unix filename
        filename = filename.replace("%3A", ":"); //restore ',' this is valid in unix filename


        String target = cachePath + filename + (header ? ".h" : "");

        File f = new File(target);

        File parent = f.getParentFile();
        parent.mkdirs();

        return f;
    }

    public synchronized CachedURL get(String u) throws Exception {


        File header = getCacheFile(u, true);
        ObjectInputStream deficheiro = new ObjectInputStream(new FileInputStream(header));

        CachedURL x = (CachedURL) deficheiro.readObject();
        //x.content = new byte[x.size];

        File content = getCacheFile(u, false);
        final FileInputStream cc = new FileInputStream(content);
        x.contentStream = cc;

        return x;
    }


    public static class CachedURL implements Serializable {

        transient public byte[] content; //stored separately

        public int responseCode;
        public List<String[]> responseHeader;
        public int size;
        transient public FileInputStream contentStream;


        public CachedURL() {

        }

        public CachedURL(Header[] responseHeaders, int responseCode, byte[] content)  {

//            try {
//                this.content = IOUtils.toByteArray(contentStream);
//                this.responseCode = responseCode;
//                this.size = content.length;
//            } catch (IOException e) {
//                this.responseCode = 404;
//                this.size = 0;
//            }
            this.content = content;
            this.size = content.length;

            responseHeader = new ArrayList(responseHeaders.length);

            for (Header h : responseHeaders) {
                responseHeader.add(new String[]{h.getName(), h.getValue()});
            }

        }

        public byte[] bytes() {
            if (content!=null) return content;
            else
                try {
                    return IOUtils.toByteArray(contentStream);
                } catch (IOException e) {
                    return null;
                }
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
        public int responseCode;
        public URL url;
        public CachedURL curl;
        public HttpResponse response;

        public Request(URL url) throws URISyntaxException {
            this.url = url;
        }

        @Override
        public CachedURL call() throws Exception {

            //

            CloseableHttpClient httpclient = HttpClients.createDefault();
            byte[] data;
            try {
                HttpGet httpget = new HttpGet(url.toURI());

                logger.info("HTTP GET " + httpget.getRequestLine());

                // Create a custom response handler
                ResponseHandler<byte[]> responseHandler = new ResponseHandler<byte[]>() {
                    ;

                    @Override
                    public byte[] handleResponse(final HttpResponse hresponse) throws ClientProtocolException, IOException {
                        response = hresponse;
                        responseCode = hresponse.getStatusLine().getStatusCode();
                        if (responseCode >= 200 && responseCode < 300) {
                            HttpEntity entity = hresponse.getEntity();
                            return EntityUtils.toByteArray(entity);
                            //return entity.getContent();
                        } else {
                            return null;
                        }
                    }

                };

                data = httpclient.execute(httpget, responseHandler);

                if (data != null) {
                    this.curl = new CachedURL(response.getAllHeaders(), responseCode, data);
                }

            } finally {
                httpclient.close();
            }

            return curl;
        }
    }


}
