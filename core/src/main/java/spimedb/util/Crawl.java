package spimedb.util;


import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.MutableNObject;
import spimedb.SpimeDB;
import spimedb.index.DObject;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.function.Predicate;

import static spimedb.index.DObject.parseKeywords;


public class Crawl {

    public static final Logger logger = LoggerFactory.getLogger(Crawl.class);



//    public static void fileDirectory(String pathStr, SpimeDB db) {
//        File path = Paths.get(pathStr).toFile();
//        if (path == null) {
//            throw new RuntimeException("not found: " + path);
//        }
//
//        if (path.isDirectory()) {
////            next.accept(
////                () -> Iterators.transform( Iterators.forArray( path.listFiles() ), eachFile::apply)
////            );
//
//            String root = db.file.getParent();
//            String indexFolder = db.file.getAbsolutePath();
//            String publicFolder = db.file.getAbsolutePath() + "/public";
//
//            logger.info("crawl directory {}", path);
//            for (File x : path.listFiles()) {
//
//                String xPath = x.getAbsolutePath();
//                if (xPath.equals(indexFolder) || xPath.equals(publicFolder)) //exclude the index folder
//                    continue;
//
//                if (x.isDirectory())
//                    continue; //dont recurse for now
//
//                try {
//                    URL u = x.toURL();
//                    String p = u.getPath();
//                    if (p.startsWith(root)) {
//                        p = p.substring(root.length()+1);
//                    }
//
//                    float pri = 0.5f * (1 / (1f + x.length()/(1024f*1024))); //deprioritize relative to megabyte increase
//
//                    url(filenameable(p), u, "file:" + xPath, db, pri);
//
//                } catch (Exception e) {
//                    logger.error("{}", e.getMessage());
//                }
//
//            }
//
//        }
//    }

    public static void url(String id, URL u, String url_in, SpimeDB db, float pri) {
        DObject p = db.get(id);
        Long whenCached = p != null ? p.get("url_cached") : null;
        try {
            if (whenCached == null || whenCached < u.openConnection().getLastModified()) {
                String urlString = u.toString();
                Tokenizer tokenizer =
                    new KeywordTokenizer();
                    //new LowerCaseTokenizer();
                Set<String> keywords = parseKeywords(tokenizer, urlString);

                MutableNObject n = new MutableNObject(id)
                        .withTags(keywords.toArray(new String[0]))
                        .put("url_in", url_in)
                        .put("url", urlString);

                //logger.info("crawl {}", n);

                db.addAsync(pri, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void pageLinks(String url, Predicate<String> acceptHref, SpimeDB db) throws IOException {

        URL uu = new URL(url);
        Document page = Jsoup.parse(uu, 10*1000);
        page.getElementsByTag("a").forEach(a -> {
            String href = a.attr("href");
            if (href!=null) {

                try {
                    URL vv = new URL(uu, uu.getPath() + '/' + href);
                    if (acceptHref.test(vv.toString())) {
                        try {
                            url(vv.getHost() + vv.getPath(), vv, vv.toString(), db, 0.5f);
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

    public static String fileName(String url) {
        if (url.endsWith("/"))
            throw new RuntimeException("not a file?");

        int slash = url.lastIndexOf('/');
        return slash == -1 ? url : url.substring(slash + 1);
    }
}
