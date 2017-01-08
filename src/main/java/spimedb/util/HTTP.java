package spimedb.util;

import com.rometools.rome.io.impl.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * dead simple HTTP response cache
 */
public class HTTP {


    private static final Logger logger = LoggerFactory.getLogger(HTTP.class);

    public static final String TMP_SPIMEDB_CACHE = "/tmp/spimedb.cache"; //TODO use correct /tmp location per platform (ex: Windows will need somewhere else)

    private final Path cachePath;

    public HTTP() throws IOException {
        this(TMP_SPIMEDB_CACHE);
    }

    public HTTP(String cachePath) throws IOException {
        this.cachePath = Files.createDirectories(Paths.get(cachePath));
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

        Path targetPath = cachePath.resolve(Base64.encode(u.toString()));
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
