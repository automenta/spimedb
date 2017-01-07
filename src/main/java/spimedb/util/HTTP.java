package spimedb.util;

import com.rometools.rome.io.impl.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Created by me on 1/7/17.
 */
public class HTTP {


    private static final Logger logger = LoggerFactory.getLogger(HTTP.class);
    private final Path cachePath;

    public HTTP() throws IOException {
        this("/tmp/spimedb.cache");
    }

    public HTTP(String cachePath) throws IOException {
        this.cachePath = Files.createDirectories(Paths.get(cachePath));
    }

    public void get(String url, /* long maxAge */ Consumer<File> result) throws IOException {

        URL u = new URL(url);
        //u.openConnection()....

        Path targetPath = cachePath.resolve(Base64.encode(u.toString()));
        File target = targetPath.toFile();

        if (!target.exists()) {
            logger.info("cache miss: {} @ {}", url, target);
            Files.createFile(targetPath);
            FileOutputStream fos = new FileOutputStream(target);
            IOUtils.copy(u.openStream(), fos);
            fos.close();
        } else {
            logger.info("cache hit: {} @ {}", url, target);
        }

        result.accept(target);



//        HTTPResponse res = http.executeRefresh(new HTTPRequest(url));
//
//        if (res.hasPayload()) {
//
//            FilePayload payload = (FilePayload) res.getPayload().get();
//            final File parent = payload.getFile().getParentFile();
//            System.out.println(parent);
//            result.accept(parent);
//            //final File file = ((FilePersistentCacheStorage)cache).getFileManager().resolve(key);
//        } else {
//            throw new IOException("null: " + res);
//        }
////        Payload pl = r.getPayload().get();
////        System.out.println(pl + " " + pl.getInputStream().getClass() + " " + pl.length());

    }

    public static void main(String[] args) throws IOException {
        HTTP http = new HTTP();
        http.get("http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.geojson", (f) -> {
            System.out.println(f);
        });
    }

}
