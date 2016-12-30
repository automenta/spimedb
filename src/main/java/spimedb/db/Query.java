package spimedb.db;

import spimedb.NObject;
import spimedb.index.rtree.RectND;

import java.util.function.Predicate;

import static spimedb.db.Query.BoundsCondition.Intersect;

/**
 * General spatiotemporal x tag Query
 */
public class Query {

    /**
     * for each result; if returns false, the query terminates
     */
    public final Predicate<NObject> each;

    /**
     * time the query was created
     */
    public final long whenCreated;

    long whenStarted, whenEnded;

    /**
     * OR-d together, potentially executed in parallel
     */
    public RectND[] bounds = null;

    public BoundsCondition boundsCondition = Intersect;

    /**
     * tags within which to search; if null, searches all
     */
    public String[] include = null;

    public Query(Predicate<NObject> each) {
        this.whenCreated = System.currentTimeMillis();
        this.each = each;
    }

    enum BoundsCondition {
        /**
         * the query bound may intersect or contain a matched bound
         */
        Intersect,

        /**
         * the query bound needs to fully contain a matched bound
         */
        Contain
    }


    /**
     * called by the db when the query begins executing
     */
    void onStart() {
        this.whenStarted = System.currentTimeMillis();
    }

    void onEnd() {
        this.whenEnded = System.currentTimeMillis();
    }

    public Query in(String... tags) {
        ensureNotStarted();

        include = tags;

        return this;
    }

    /**
     * specific lat x lon region, at any time
     */
    public Query where(float[] lon, float[] lat) {
        ensureNotStarted();

        bounds = new RectND[]{new RectND(
                new float[]{Float.NEGATIVE_INFINITY, lon[0], lat[0], Float.NEGATIVE_INFINITY},
                new float[]{Float.POSITIVE_INFINITY, lon[1], lat[1], Float.POSITIVE_INFINITY}
        )};

        return this;
    }

    private void ensureNotStarted() {
        if (whenStarted != 0)
            throw new RuntimeException("Query already executing");
    }

}
