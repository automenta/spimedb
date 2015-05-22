package automenta.climatenet.p2p;


import org.apache.commons.io.IOUtils;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class HttpCache {


    FileCache fileCache;
    public static final HttpCache the = new HttpCache("cache/file");


    public HttpCache(String path)  {
        fileCache = new FileCache(path, 7, TimeUnit.DAYS);
    }


    public byte[] get(String url) throws Exception {

        byte[] b = fileCache.get(url);
        if (b!=null) {
            System.out.println("Download (cached): " + url);
            return b;
        }

        System.out.println("Download: " + url);
        URL u = new URL(url);
        b = IOUtils.toByteArray(new InputStreamReader(u.openStream()));


        fileCache.put(url, b);
        return b;
    }

}
