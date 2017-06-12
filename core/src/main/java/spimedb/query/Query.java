package spimedb.query;

import jcog.tree.rtree.rect.RectDoubleND;
import org.jetbrains.annotations.NotNull;
import spimedb.SpimeDB;

import java.util.Arrays;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static spimedb.query.Query.BoundsCondition.Intersect;

/**
 * General spatiotemporal x tag Query
 */
public class Query  {


    /**
     * time the query was created
     */
    public final long whenCreated;

    long whenAccepted;

//    public static final double[] ANY_SCALAR = {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
//    public static final RectDoubleND[] ANYWHERE_4 =
//            new RectDoubleND[] ( ANY_SCALAR, ANY_SCALAR );

    /**
     * OR-d together, potentially executed in parallel
     */
    public RectDoubleND[] bounds = null;

    public BoundsCondition boundsCondition = Intersect;

    /**
     * tags within which to search; if null, searches all
     */
    public String[] tagInclude = null;

    public int limit = 128;


    /**
     *
     * @param each if null, attempts to use this instance as the predicate (as it can be implemented in subclasses)
     */
    public Query() {
        this.whenCreated = System.currentTimeMillis();
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
     * @param spimeDB
     */
    public void onStart(SpimeDB spimeDB) {
        this.whenAccepted = System.currentTimeMillis();
    }



    public Query in(String... tags) {
        ensureNotStarted();

        tagInclude = tags;

        return this;
    }


    /** time-axis only */
    public <Q extends Query> Q when(float start, float end) {
        return (Q) bounds(new RectDoubleND(
                new double[]{start, NEGATIVE_INFINITY, NEGATIVE_INFINITY, NEGATIVE_INFINITY},
                new double[]{end, POSITIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY}
        ));
    }

    /**
     * specific lat x lon region, at any time
     */
    public <Q extends Query> Q where(double[] lon, double[] lat) {
        return where(lon[0], lon[1], lat[0], lat[1]);
    }

    public <Q extends Query> Q where(double lonMin, double lonMax, double latMin, double latMax) {

        double centerX = (lonMin + lonMax)/2;
        double centerY = (latMin + latMax)/2;

        //TODO is there a way to do this without looping
        while (centerX < -180)
            centerX += 360;
        while (centerX > +180)
            centerX -= 360;

        double wHalf = Math.min(Math.abs(lonMax - lonMin), 360)/2.0;
        double hHalf = Math.min(Math.abs(latMax - latMin), 180)/2.0;

        //TODO clip latitude in -90,+90

        double latMinFinal = centerY - hHalf;
        double latMaxFinal = centerY + hHalf;
        if (centerX - wHalf < -180 || centerX + wHalf > +180) {
            //crosses international dateline, subdivide into 2 queries

            double x1max, x2min;
            if (centerX - wHalf < -180) {
                x1max = centerX + wHalf;
                x2min = 360 + (centerX -wHalf);
            } else if (centerX + wHalf > +180) {
                x1max = -180 + ((centerX + wHalf) - 180);
                x2min= centerX - wHalf;
            } else {
                ///??
                throw new UnsupportedOperationException();
            }

            return bounds(
                new RectDoubleND(
                    new double[]{NEGATIVE_INFINITY, -180, latMinFinal, NEGATIVE_INFINITY},
                    new double[]{POSITIVE_INFINITY, x1max, latMaxFinal, POSITIVE_INFINITY}
                ),new RectDoubleND(
                    new double[]{NEGATIVE_INFINITY, x2min, latMinFinal, NEGATIVE_INFINITY},
                    new double[]{POSITIVE_INFINITY, +180, latMaxFinal, POSITIVE_INFINITY}
            ));
        } else {
            return (Q) bounds(new RectDoubleND(
                    new double[]{NEGATIVE_INFINITY, centerX-wHalf, latMinFinal, NEGATIVE_INFINITY},
                    new double[]{POSITIVE_INFINITY, centerX+wHalf, latMaxFinal, POSITIVE_INFINITY}
            ));
        }
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
        if (whenAccepted != 0)
            throw new RuntimeException("Query already executing");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' +
                ", whenCreated=" + whenCreated +
                ", whenAccepted=" + whenAccepted +
                ", bounds=" + Arrays.toString(bounds) +
                ", boundsCondition=" + boundsCondition +
                ", include=" + ((tagInclude == null || tagInclude.length == 0) ? "ALL" : Arrays.toString(tagInclude)) +
                '}';
    }


}
