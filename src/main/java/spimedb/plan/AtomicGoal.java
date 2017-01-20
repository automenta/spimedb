package spimedb.plan;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created by me on 1/20/17.
 */
abstract public class AtomicGoal<A extends Agent> extends AbstractGoal<A> {

    public AtomicGoal(String id) {
        super(id);
    }

    @NotNull
    @Override
    public void DO(@NotNull A context, Consumer<Iterable<Goal<? super A>>> next) throws RuntimeException {
        run(context);
    }

    protected abstract void run(A context) throws RuntimeException;

}
