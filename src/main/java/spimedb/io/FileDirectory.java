package spimedb.io;

import spimedb.MutableNObject;
import spimedb.SpimeDB;
import spimedb.index.lucene.DocumentNObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;


public class FileDirectory {

    public static String filenameable(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    public static void load(String pathStr, SpimeDB db) {
        File path = Paths.get(pathStr).toFile();
        if (path == null) {
            throw new RuntimeException("not found: " + path);
        }

        if (path.isDirectory()) {
//            next.accept(
//                () -> Iterators.transform( Iterators.forArray( path.listFiles() ), eachFile::apply)
//            );

            for (File x : path.listFiles()) {
                if (db.indexPath != null && x.getAbsolutePath().equals(db.indexPath)) //exclude the index folder
                    continue;

                if (x.isDirectory())
                    continue; //dont recurse for now

                try {


                    URL u = x.toURL();
                    String us = u.toString();
                    String uf = fileName(u.getFile());


                    String id = filenameable(uf);

                    DocumentNObject p = db.get(id);
                    String whenCached = p != null ? p.get("url_cached") : null;
                    if (whenCached == null || Long.valueOf(whenCached) < x.lastModified()) {
                        db.addAsync(new MutableNObject(id)
                                .put("url_in", us)
                                .put("url", uf)
                        );
                    }


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

            }

            return;
        }
    }

    public static String fileName(String url) {
        if (url.endsWith("/"))
            throw new RuntimeException("not a file?");

        int slash = url.lastIndexOf('/');
        if (slash == -1)
            return url;
        else
            return url.substring(slash + 1);
    }
}
