package spimedb.io;

import spimedb.MutableNObject;
import spimedb.SpimeDB;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;


public class FileDirectory {

    public static String filenameable(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    public static void createFileNodes(String pathStr, SpimeDB db) {
        File path = Paths.get(pathStr).toFile();
        if (path == null) {
            throw new RuntimeException("not found: " + path);
        }

        if (path.isDirectory()) {
//            next.accept(
//                () -> Iterators.transform( Iterators.forArray( path.listFiles() ), eachFile::apply)
//            );

            for (File x : path.listFiles()) {
                try {

                    URL u = x.toURL();
                    String us = u.toString();
                    db.add(new MutableNObject(filenameable(u.getFile()), x.getName())
                        .put("url_in", us)
                        .put("url", u.getFile())
                    );

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

            }

            return;
        }
    }
}
