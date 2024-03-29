package spimedb.util;

import okhttp3.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import io.undertow.server.HttpHandler;
//import io.undertow.server.HttpServerExchange;
//import io.undertow.server.handlers.resource.FileResourceManager;
//import io.undertow.util.Headers;
//import org.apache.commons.io.IOUtils;
//import org.jetbrains.annotations.Nullable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import spimedb.SpimeDB;
//
//import java.io.*;
//import java.net.URL;
//import java.nio.ByteBuffer;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Deque;
//import java.util.Map;
//import java.util.function.Consumer;
//import java.util.function.Function;
//
//import static io.undertow.Handlers.resource;
//
public class HTTP {

    /* https://square.github.io/okhttp/features/caching/ */
    private static final Cache cache = new Cache(new File("/var/tmp", "spimedb"),
            1024 * 1024L * 1024L //1gb
    );
    private static final OkHttpClient client = new OkHttpClient.Builder().cache(cache).build();


    public static InputStream inputStream(String url) throws IOException {
        return inputStream(url, null);
    }

    public static InputStream inputStream(String url, @Nullable String post) throws IOException {
        Request.Builder rb = new Request.Builder().url(url);

        if (post!=null) {
            //POST
            RequestBody b = RequestBody.create(post, MediaType.get("text/json"));
            rb.post(b);
        } else {
            //GET
        }

        var request = rb.build();

        Response response = client.newCall(request).execute();
        //try () {
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);

//            Headers responseHeaders = response.headers();
//            for (int i = 0; i < responseHeaders.size(); i++)
//                System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));

//              System.out.println("HTTP cache");
//              System.out.println("hit: " + cache.hitCount());
//              System.out.println("request: " + cache.requestCount());
//              System.out.println("network: " + cache.networkCount());
//              System.out.println("writeSuccess: " + cache.writeSuccessCount());

            return response.body().byteStream();

//        byte[] bb = b.bytes();
//        var i = new ByteArrayInputStream(bb);
//              b.close();
//              response.close();
//              return i;
        //}
    }
//        public static void asStream(String url, /* long maxAge */ Consumer<InputStream> result) throws IOException {
//
//        //TODO use Tee pipe to read and write the cache miss simultaneously and still provide streaming result to the callee
//        //and just provide a FileInputStream to a cache hit
//
//        IOException e = asFile(url, file -> {
//            try {
//                result.accept(new FileInputStream(file));
//                return null; //OK
//            } catch (FileNotFoundException ee) {
//                //e.printStackTrace();
//                return ee;
//            }
//        });
//
//        if (e!=null)
//            throw e;
//    }
//
//    public static String asString(String url) throws IOException {
//        return asFile(url, f->{
//            try {
//                return IOUtils.toString(new FileInputStream(f));
//            } catch (IOException e) {
//                //e.printStackTrace();
//                return e.toString();
//            }
//        });
//    }
//
//    public static void asFile(String url,  Consumer<File> result) throws IOException {
//        asFile(url, f -> { result.accept(f); return null; } );
//    }
//
//
////    static final String defaultClientPath = "./src/main/resources/public";
////    private static final Logger logger = LoggerFactory.getLogger(HTTP.class);
////
////
////    private final Path cachePath;
////
////    public HTTP() {
////        this(SpimeDB.TMP_SPIMEDB_CACHE_PATH);
////    }
////
////    public HTTP(String cachePath) {
////        this.cachePath = FileUtils.pathOrCreate(cachePath);
////    }
////
////    @Nullable public static Path tmpCacheDir() {
////        return FileUtils.pathOrCreate(SpimeDB.TMP_SPIMEDB_CACHE_PATH);
////    }
////    @Nullable public static File tmpCacheFile(String path) {
////        File f = tmpCacheDir().resolve(path).toFile();
////        if (!f.exists()) {
////            try {
////                f.createNewFile();
////            } catch (IOException e) {
////                e.printStackTrace();
////                return null;
////            }
////        }
////        return f;
////    }
////
////    public static void send(String s, HttpServerExchange ex) {
////        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
////        ex.getResponseSender().send(s);
////        ex.endExchange();
////    }
////
////    public static void stream(HttpServerExchange ex, Consumer<OutputStream> s) {
////        stream(ex, s, "text/plain");
////    }
////
////    public static void stream(HttpServerExchange ex, Consumer<OutputStream> s, String contentType) {
////
////        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
////
////        ex.dispatch(() -> {
////            ex.startBlocking();
////
////            OutputStream os = ex.getOutputStream();
////            s.accept(os);
////
////            //ex.getResponseSender().close();
////            ex.endExchange();
////
////        });
////    }
////
////    static void send(byte[] s, HttpServerExchange ex, String type) {
////
////        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, type);
////
////        ex.getResponseSender().send(ByteBuffer.wrap(s));
////
////        //ex.getResponseSender().close();
////        ex.endExchange();
////    }
////
////    static void send(JsonNode d, HttpServerExchange ex) {
////        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
////
////
////
////
////        ex.startBlocking();
////
////        try {
////            JSON.json.writeValue(ex.getOutputStream(), d);
////        } catch (IOException ex1) {
////            logger.warn("send", ex1);
////        }
////
////        ex.getResponseSender().close();
////    }
////
////    public static String[] getStringArrayParameter(HttpServerExchange ex, String param) throws IOException {
////        String s = getStringParameter(ex, param);
////
////        ArrayNode a = JSON.json.readValue(s, ArrayNode.class);
////
////        String[] ids = JSON.toStrings(a);
////
////        return ids;
////    }
////
////    @Nullable public static String getStringParameter(HttpServerExchange ex, String param) {
////        Map<String, Deque<String>> reqParams = ex.getQueryParameters();
////
////        Deque<String> idArray = reqParams.get(param);
////        if (idArray==null)
////            return null;
////
////        return idArray.getFirst();
////    }
////
////    public static HttpHandler handleClientResources() {
////        return handleClientResources(defaultClientPath);
////    }
////
////    public static HttpHandler handleClientResources(String clientPath) {
////        File base = new File(clientPath);
////
////        return resource(
////
////                new FileResourceManager(base, 0))
////
//////                        new CachingResourceManager(
//////                                16384,
//////                                16*1024*1024,
//////                                new DirectBufferCache(100, 10, 1000),
//////                                new PathResourceManager(getResourcePath(), 0, true, true),
//////                                0 //7 * 24 * 60 * 60 * 1000
//////                        ))
////                .setCachable((x) -> true)
////                //.setDirectoryListingEnabled(true)
////                .addWelcomeFiles("index.html")
////        ;
////
////
//////        return header(resource( new FileResourceManager(base, 100, true, "/") )
//////                    .setWelcomeFiles("index.html")
//////                    .setDirectoryListingEnabled(true), "Access-Control-Allow-Origin", "*");
////    }
//
//    public static void asStream(String url, /* long maxAge */ Consumer<InputStream> result) throws IOException {
//
//        //TODO use Tee pipe to read and write the cache miss simultaneously and still provide streaming result to the callee
//        //and just provide a FileInputStream to a cache hit
//
//        IOException e = asFile(url, file -> {
//            try {
//                result.accept(new FileInputStream(file));
//                return null; //OK
//            } catch (FileNotFoundException ee) {
//                //e.printStackTrace();
//                return ee;
//            }
//        });
//
//        if (e!=null)
//            throw e;
//    }
//
////    public static String asString(String url) throws IOException {
////        return asFile(url, f->{
////            try {
////                return IOUtils.toString(new FileInputStream(f));
////            } catch (IOException e) {
////                //e.printStackTrace();
////                return e.toString();
////            }
////        });
////    }
////    public static void asFile(String url,  Consumer<File> result) throws IOException {
////        asFile(url, f -> { result.accept(f); return null; } );
////    }
//
//    public static String filenameable(String inputName) {
//        return inputName.replaceAll("[^\\/a-zA-Z0-9-_\\.]", "_");
//    }
//
////    public static  <X> X asFile(String url, /* long maxAge, etc.. */ Function<File,X> result) throws IOException {
////
////        URL u = new URL(url);
////        //u.openConnection().... <- properly check cache conditions via the response headers or something
////
////        Path targetPath = cachePath.resolve(filenameable(u.toString()));
////        File target = targetPath.toFile();
////
////        if (!target.exists()) {
////            logger.info("cache miss: {} @ {}", url, target);
////            Files.createFile(targetPath);
////
////            //TODO file locking
////            FileOutputStream fos = new FileOutputStream(target);
////            IOUtils.copy(u.openStream(), fos);
////            fos.close();
////        } else {
////            logger.info("cache hit: {} @ {}", url, target);
////        }
////
////        return result.apply(target);
////    }
//
////    public static void main(String[] args) throws IOException {
////        HTTP http = new HTTP();
////        http.asFile("http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.geojson", (Consumer<File>) System.out::println);
////    }
//
}
