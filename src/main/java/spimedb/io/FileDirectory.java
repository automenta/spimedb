package spimedb.io;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import spimedb.SpimeDB;
import spimedb.plan.AbstractGoal;
import spimedb.plan.Goal;

import java.io.File;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 1/19/17.
 */
public class FileDirectory extends AbstractGoal<SpimeDB> {


    private final String pathStr;
    private final Function<File, Goal<? super SpimeDB>> eachFile;

    public FileDirectory(String path, Function<File, Goal<? super SpimeDB>> eachFile) {
        super(path);
        this.pathStr = path;
        this.eachFile = eachFile;
    }

    @NotNull
    @Override
    public void DO(@NotNull SpimeDB context, Consumer<Iterable<Goal<? super SpimeDB>>> next) throws RuntimeException {
        File path = Paths.get(pathStr).toFile();
        if (path == null) {
            throw new RuntimeException("not found: " + path);
        }

        if (path.isDirectory()) {
            next.accept(
                () -> Iterators.transform( Iterators.forArray( path.listFiles() ), eachFile::apply)
            );
            return;
        }
        /* else {
            //is zip file?
        } */


        throw new RuntimeException("not a directory or file-containing archive: " + path);
    }
}
