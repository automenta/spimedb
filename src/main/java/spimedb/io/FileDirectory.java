package spimedb.io;

import org.jetbrains.annotations.NotNull;
import spimedb.MutableNObject;
import spimedb.SpimeDB;
import spimedb.plan.AbstractGoal;
import spimedb.plan.Goal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Created by me on 1/19/17.
 */
public class FileDirectory extends AbstractGoal<SpimeDB> {


    private final String pathStr;

    public FileDirectory(String path) {
        super(path);
        this.pathStr = path;
    }

    @NotNull
    @Override
    public void DO(@NotNull SpimeDB context, Consumer<Iterable<Goal<? super SpimeDB>>> next) throws RuntimeException {
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
                    context.add(new MutableNObject(us).put("url", us));

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

            }

            return;
        }
        /* else {
            //is zip file?
        } */


        throw new RuntimeException("not a directory or file-containing archive: " + path);
    }
}
