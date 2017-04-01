package spimedb.query;

import spimedb.NObject;
import spimedb.SpimeDB;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * TODO implement Collection<>
 */
public class QueryCollection implements Predicate<NObject> {
    public final Collection<NObject> result;
    private final Query query;

    public QueryCollection(Query q, Collection<NObject> result) {
        super();
        this.query = q;
        this.result = result;
    }

    @Override
    public boolean test(NObject next) {
        result.add(next);
        return true;
    }

    public QueryCollection get(SpimeDB db) {
        db.get(query).forEach((d,s)->result.add(d));
        return this;
    }
}
