package spimedb.plan;

import spimedb.util.JSON;

/**
 * Created by me on 1/20/17.
 */
abstract public class AbstractGoal<A extends Agent> extends Goal<A> {

    private final String id;

    public AbstractGoal(Object... id) {
        String argString = JSON.toJSONString(id);
        this.id = getClass().getSimpleName() + "(" + argString.substring(1, argString.length()-1) + ")";
    }

    @Override
    public String id() {
        return id;
    }

}
