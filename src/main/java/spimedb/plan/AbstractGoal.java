package spimedb.plan;

/**
 * Created by me on 1/20/17.
 */
abstract public class AbstractGoal<A extends Agent> extends Goal<A> {

    private final String id;

    public AbstractGoal(String id) {
        this.id = getClass().getSimpleName() + "." + id;
    }

    @Override
    public String id() {
        return id;
    }

}
