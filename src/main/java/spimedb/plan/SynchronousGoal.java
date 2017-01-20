package spimedb.plan;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created by me on 1/20/17.
 */
abstract public class SynchronousGoal<A extends Agent> extends AbstractGoal<A> {

    public SynchronousGoal(String id) {
        super(id);
    }

    @NotNull
    @Override
    public void DO(@NotNull A context, Consumer<Iterable<Goal<? super A>>> next) throws RuntimeException {
        next.accept( run(context) );
    }

    protected abstract Iterable<Goal<? super A>> run(A context) throws RuntimeException;

}
