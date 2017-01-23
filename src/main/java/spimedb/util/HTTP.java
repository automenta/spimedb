package spimedb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;
import spimedb.io.FileDirectory;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.undertow.Handlers.resource;

/**
 * dead simple HTTP response cache
 */
public class HTTP {


    static final String defaultClientPath = "./src/main/resources/public";
    private static final Logger logger = LoggerFactory.getLogger(HTTP.class);


    private final Path cachePath;

    public HTTP() {
        this(SpimeDB.TMP_SPIMEDB_CACHE_PATH);
    }

    public HTTP(String cachePath) {
        this.cachePath = FileUtils.pathOrCreate(cachePath);
    }

    @Nullable public static Path tmpCacheDir() {
        return FileUtils.pathOrCreate(SpimeDB.TMP_SPIMEDB_CACHE_PATH);
    }
    @Nullable public static File tmpCacheFile(String path) {
        File f = tmpCacheDir().resolve(path).toFile();
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return f;
    }

    public static void send(String s, HttpServerExchange ex) {
        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        ex.getResponseSender().send(s);
        ex.endExchange();
    }

    public static void stream(HttpServerExchange ex, Consumer<OutputStream> s) {

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        ex.dispatch(() -> {
            ex.startBlocking();

            OutputStream os = ex.getOutputStream();
            s.accept(os);

            //ex.getResponseSender().close();
            ex.endExchange();

        });
    }

    static void send(byte[] s, HttpServerExchange ex, String type) {

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, type);

        ex.getResponseSender().send(ByteBuffer.wrap(s));

        //ex.getResponseSender().close();
        ex.endExchange();
    }

    static void send(JsonNode d, HttpServerExchange ex) {
        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");




        ex.startBlocking();

        try {
            JSON.json.writeValue(ex.getOutputStream(), d);
        } catch (IOException ex1) {
            logger.warn("send: {}", ex1);
        }

        ex.getResponseSender().close();
    }

    static String[] getStringArrayParameter(HttpServerExchange ex, String param) throws IOException {
        Map<String, Deque<String>> reqParams = ex.getQueryParameters();

        Deque<String> idArray = reqParams.get(param);

        ArrayNode a = JSON.json.readValue(idArray.getFirst(), ArrayNode.class);

        String[] ids = new String[a.size()];
        int j = 0;
        for (JsonNode x : a) {
            ids[j++] = x.textValue();
        }

        return ids;
    }

    public static HttpHandler handleClientResources() {
        return handleClientResources(defaultClientPath);
    }

    public static HttpHandler handleClientResources(String clientPath) {
        File base = new File(clientPath);

        return resource(

                new FileResourceManager(base, 0))

//                        new CachingResourceManager(
//                                16384,
//                                16*1024*1024,
//                                new DirectBufferCache(100, 10, 1000),
//                                new PathResourceManager(getResourcePath(), 0, true, true),
//                                0 //7 * 24 * 60 * 60 * 1000
//                        ))
                .setCachable((x) -> true)
                //.setDirectoryListingEnabled(true)
                .addWelcomeFiles("index.html")
        ;


//        return header(resource( new FileResourceManager(base, 100, true, "/") )
//                    .setWelcomeFiles("index.html")
//                    .setDirectoryListingEnabled(true), "Access-Control-Allow-Origin", "*");
    }

    public void asStream(String url, /* long maxAge */ Consumer<InputStream> result) throws IOException {

        //TODO use Tee pipe to read and write the cache miss simultaneously and still provide streaming result to the callee
        //and just provide a FileInputStream to a cache hit

        IOException e = asFile(url, file -> {
            try {
                result.accept(new FileInputStream(file));
                return null; //OK
            } catch (FileNotFoundException ee) {
                //e.printStackTrace();
                return ee;
            }
        });

        if (e!=null)
            throw e;
    }

    public String asString(String url) throws IOException {
        return asFile(url, f->{
            try {
                return IOUtils.toString(new FileInputStream(f));
            } catch (IOException e) {
                //e.printStackTrace();
                return e.toString();
            }
        });
    }

    public void asFile(String url,  Consumer<File> result) throws IOException {
        asFile(url, f -> { result.accept(f); return null; } );
    }

    public <X> X asFile(String url, /* long maxAge, etc.. */ Function<File,X> result) throws IOException {

        URL u = new URL(url);
        //u.openConnection().... <- properly check cache conditions via the response headers or something

        Path targetPath = cachePath.resolve(FileDirectory.filenameable(u.toString()));
        File target = targetPath.toFile();

        if (!target.exists()) {
            logger.info("cache miss: {} @ {}", url, target);
            Files.createFile(targetPath);

            //TODO file locking
            FileOutputStream fos = new FileOutputStream(target);
            IOUtils.copy(u.openStream(), fos);
            fos.close();
        } else {
            logger.info("cache hit: {} @ {}", url, target);
        }

        return result.apply(target);
    }

    public static void main(String[] args) throws IOException {
        HTTP http = new HTTP();
        http.asFile("http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.geojson", (f) -> {
            System.out.println(f);
        });
    }

}
