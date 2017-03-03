package spimedb.io;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.MutableNObject;
import spimedb.SpimeDB;
import spimedb.index.DObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.function.Predicate;


public class Crawl {

    public static final Logger logger = LoggerFactory.getLogger(Crawl.class);

    public static String filenameable(String inputName) {
        return inputName.replaceAll("[^\\/a-zA-Z0-9-_\\.]", "_");
    }

    public static void fileDirectory(String pathStr, SpimeDB db) {
        File path = Paths.get(pathStr).toFile();
        if (path == null) {
            throw new RuntimeException("not found: " + path);
        }

        if (path.isDirectory()) {
//            next.accept(
//                () -> Iterators.transform( Iterators.forArray( path.listFiles() ), eachFile::apply)
//            );

            String root = db.file.getParent();
            String indexFolder = db.file.getAbsolutePath();
            String publicFolder = db.file.getAbsolutePath() + "/public";

            for (File x : path.listFiles()) {
                String xPath = x.getAbsolutePath();
                if (xPath.equals(indexFolder) || xPath.equals(publicFolder)) //exclude the index folder
                    continue;

                if (x.isDirectory())
                    continue; //dont recurse for now

                try {
                    URL u = x.toURL();
                    String p = u.getPath();
                    if (p.startsWith(root)) {
                        p = p.substring(root.length()+1);
                    }

                    url(filenameable(p), u, 0.8f, db);


                } catch (Exception e) {
                    logger.error("{}", e.getMessage());
                }

            }

            return;
        }
    }

    public static void url(String id, URL u, float pri, SpimeDB db) {
        String us = u.toString();

        DObject p = db.get(id);
        String whenCached = p != null ? p.get("url_cached") : null;
        try {
            if (whenCached == null || Long.valueOf(whenCached) < u.openConnection().getLastModified()) {
                db.addAsync(pri, new MutableNObject(id)
                        .put("url_in", us)
                        .put("url", u.toString())
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void pageLinks(String url, Predicate<String> acceptHref, SpimeDB db) throws IOException {

        URL uu = new URL(url);
        Document page = Jsoup.parse(uu, 10*1000);
        page.getElementsByTag("a").forEach(a -> {
            String href = (a.attr("href"));
            if (href!=null) {

                try {
                    URL vv = new URL(uu, uu.getPath() + '/' + href);
                    if (acceptHref.test(vv.toString())) {
                        try {
                            url(vv.getHost() + vv.getPath(), vv, 0.5f, db);
                        } catch (RuntimeException e) {
                            logger.warn("{}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("{}", e.getMessage());
                }
            }
        });

    }

    static String fileName(String url) {
        if (url.endsWith("/"))
            throw new RuntimeException("not a file?");

        int slash = url.lastIndexOf('/');
        if (slash == -1)
            return url;
        else
            return url.substring(slash + 1);
    }
}
