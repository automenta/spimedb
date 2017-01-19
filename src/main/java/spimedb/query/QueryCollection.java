package spimedb.query;

import spimedb.AbstractNObject;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * TODO implement Collection<>
 */
public class QueryCollection extends Query implements Predicate<AbstractNObject> {
    public final Collection<AbstractNObject> result;

    public QueryCollection(Collection<AbstractNObject> result) {
        super();
        this.result = result;
    }

    @Override
    public boolean test(AbstractNObject next) {
        result.add(next);
        return true;
    }
}
