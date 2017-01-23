package spimedb.io;

import spimedb.MutableNObject;
import spimedb.SpimeDB;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Created by me on 1/19/17.
 */
public class FileDirectory {


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
                    db.add(new MutableNObject(us).put("url", us));

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

            }

            return;
        }
    }
}
