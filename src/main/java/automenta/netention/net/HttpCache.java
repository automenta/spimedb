package automenta.netention.net;


import com.syncleus.spangraph.InfiniPeer;
import org.apache.commons.io.IOUtils;

import java.io.InputStreamReader;
import java.net.URL;


public class HttpCache {




    final SpanCache fileCache;

    public HttpCache(String path, InfiniPeer peer)  {
        fileCache = new SpanCache(path, peer);
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
