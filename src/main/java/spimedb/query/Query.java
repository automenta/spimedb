package spimedb.query;

import jcog.tree.rtree.rect.RectDoubleND;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spimedb.NObject;

import java.util.Arrays;
import java.util.function.Predicate;

import static spimedb.query.Query.BoundsCondition.Intersect;

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
    public RectDoubleND[] bounds = null;

    public BoundsCondition boundsCondition = Intersect;

    /**
     * tags within which to search; if null, searches all
     */
    public String[] include = null;

    public int limit = 128;


    protected Query() {
        this(null);
    }

    /**
     *
     * @param each if null, attempts to use this instance as the predicate (as it can be implemented in subclasses)
     */
    public Query(@Nullable Predicate<NObject> each) {
        this.whenCreated = System.currentTimeMillis();
        this.each = each != null ? each : (Predicate)this;
    }

    public Query limit(int limit) {
        this.limit = limit;
        return this;
    }

    public enum BoundsCondition {
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
    public void onStart() {
        this.whenStarted = System.currentTimeMillis();
    }

    public void onEnd() {
        this.whenEnded = System.currentTimeMillis();
    }

    public Query in(String... tags) {
        ensureNotStarted();

        include = tags;

        return this;
    }


    /** time-axis only */
    public <Q extends Query> Q when(float start, float end) {
        return (Q) bounds(new RectDoubleND(
                new double[]{start, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY},
                new double[]{end, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY}
        ));
    }

    /**
     * specific lat x lon region, at any time
     */
    public <Q extends Query> Q where(double[] lon, double[] lat) {
        return (Q) bounds(new RectDoubleND(
                new double[]{Double.NEGATIVE_INFINITY, lon[0], lat[0], Double.NEGATIVE_INFINITY},
                new double[]{Double.POSITIVE_INFINITY, lon[1], lat[1], Double.POSITIVE_INFINITY}
        ));
    }

    @NotNull
    public <Q extends Query> Q bounds(RectDoubleND... newBounds) {
        ensureNotStarted();

        if (bounds!=null)
            throw new RuntimeException("bounds already specified");

        bounds = newBounds;

        return (Q) this;
    }

    private void ensureNotStarted() {
        if (whenStarted != 0)
            throw new RuntimeException("Query already executing");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "each=" + (each != this ? each : "this" ) +
                ", whenCreated=" + whenCreated +
                ", whenStarted=" + whenStarted +
                ", whenEnded=" + whenEnded + "=(" + ((double)(whenEnded - whenStarted)) + "ms)" +
                ", bounds=" + Arrays.toString(bounds) +
                ", boundsCondition=" + boundsCondition +
                ", include=" + ((include == null || include.length == 0) ? "ALL" : Arrays.toString(include)) +
                '}';
    }
}
