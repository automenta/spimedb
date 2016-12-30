package spimedb.query;

import spimedb.NObject;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * TODO implement Collection<>
 */
public class QueryCollection extends Query implements Predicate<NObject> {
    public final Collection<NObject> result;

    public QueryCollection(Collection<NObject> result) {
        super();
        this.result = result;
    }

    @Override
    public boolean test(NObject next) {
        result.add(next);
        return true;
    }
}
