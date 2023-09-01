package spimedb.query;


import jcog.Util;
import jcog.data.list.Lst;
import jcog.tree.rtree.rect.HyperRectDouble;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.document.DoubleRange;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.index.Search;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static spimedb.query.Query.BoundsCondition.Intersect;

/**
 * General spatiotemporal x tag Query
 */
public class Query  {

    public final static Logger logger = LoggerFactory.getLogger(Query.class);

    /**
     * time the query was created
     */
    public final long whenCreated;

    long whenAccepted;

    /**
     * OR-d together, potentially executed in parallel
     */
    public HyperRectDouble[] bounds = null;

    //TODO: public HyperRectDouble minBounds = null;

    public BoundsCondition boundsCondition = Intersect;

    /** query string to parse */
    String queryString = null;

    /**
     * tags within which to search; if null, searches all
     */
    public String[] tagInclude = null;

    public int limit = 16 * 1024;
    private ScoreDoc after = null;

    /**
     *
     * @param each if null, attempts to use this instance as the predicate (as it can be implemented in subclasses)
     */
    public Query() {
        this.whenCreated = System.currentTimeMillis();
    }

    public Query(String q)  {
        this();
        this.queryString = q;
    }

    public Query limit(int limit) {
        this.limit = limit;
        return this;
    }

    public Query after(ScoreDoc after) {
        this.after = after;
        return this;
    }

    public Search start(SpimeDB db, Collector... collectors) {

        BooleanQuery bq = buildQuery(db);

        this.whenAccepted = System.currentTimeMillis();

        try {
            return find(bq, limit, db, after, collectors);
        } catch (IOException e) {
            logger.error("Query.start", e);
            return null;
        }

    }

    private Search find(org.apache.lucene.search.Query q, int limit, SpimeDB db, ScoreDoc after, Collector... collectors) throws IOException {

        TopScoreDocCollector hitsCollector = TopScoreDocCollector.create(limit, after, limit/2/*Integer.MAX_VALUE*/);

        Collector collector = collectors.length > 0 ?
            MultiCollector.wrap(ArrayUtils.add(collectors, hitsCollector)) :
            hitsCollector;

        IndexSearcher searcher = db.searcher();
        searcher.search(q, collector);
        TopDocs docs = hitsCollector.topDocs();

        Search s = new Search(q, searcher, db, docs);
        if (docs.totalHits.value > 0) {
            for (Collector c : collectors) {
                if (c instanceof CollectFacets cf)
                    cf.commit(s, db);
            }
        }
        return s;
    }

//    public static TermQuery tagTermQuery(String tag) {
//        return new TermQuery(new Term(NObject.TAG, tag));
//    }

    private BooleanQuery buildQuery(SpimeDB db) {
        BooleanQuery.Builder bqb = new BooleanQuery.Builder();

        if (tagInclude != null) {
//            int tags = tagInclude.length;
            //if (tags > 1) {
//            List<org.apache.lucene.search.Query> tagQueries = new Lst<>(tags);
//            for (String s : tagInclude)
//                tagQueries.add(tagTermQuery(s));
//            bqb.add(new DisjunctionMaxQuery(tagQueries, 1f / tags),
//                    BooleanClause.Occur.MUST);

            bqb.add(termSetQuery(NObject.TAG, tagInclude), BooleanClause.Occur.MUST);

            /* } else if (tags == 1) {
                bqb.add(tagTermQuery(tagInclude[0]))
            }*/
        }

        if (bounds != null && bounds.length > 0) {

            List<org.apache.lucene.search.Query> boundQueries = new Lst<>();

            for (HyperRectDouble x : bounds) {
                if (Objects.requireNonNull(boundsCondition) == Intersect) {
                    boundQueries.add(DoubleRange.newIntersectsQuery(NObject.BOUND, x.min.coord, x.max.coord));
                } else {
                    throw new UnsupportedOperationException("TODO");
                }


            }

            bqb.add(new DisjunctionMaxQuery(boundQueries, 0.1f), BooleanClause.Occur.MUST);
        }

        if (queryString!=null) {
            try {
                bqb.add(db.parseQuery(queryString), BooleanClause.Occur.MUST);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        return bqb.build();
    }


    public static TermInSetQuery termSetQuery(String field, String[] values) {
        Lst<BytesRef> _tags = new Lst<>();
        for (String s : values)
            _tags.add(new BytesRef(s));

        TermInSetQuery termSetQuery = new TermInSetQuery(field, _tags);
        return termSetQuery;
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


    public Query in(String... tags) {
        ensureNotStarted();

        tagInclude = tags;

        return this;
    }


    /** time-axis only */
    public <Q extends Query> Q when(double start, double end) {
        return bounds(new HyperRectDouble(
                new double[]{start, NEGATIVE_INFINITY, NEGATIVE_INFINITY, NEGATIVE_INFINITY},
                new double[]{end, POSITIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY}
        ));
    }

//    private static double a2t(double angle) {
//        return angle / (180/Math.PI);
//    }
//    private static double t2a(double theta) {
//        return theta * (180/Math.PI);
//    }

    public <Q extends Query> Q where(double lonMin, double lonMax, double latMin, double latMax) {
//        //TODO is there a way to normalize these polar coordinates without looping
//        while (lonMin < -180) lonMin += 360;
//        while (lonMin > +180) lonMin -= 360;
//        while (lonMax < -180) lonMax += 360;
//        while (lonMax > +180) lonMax -= 360;

//        lonMin = t2a(MathUtils.normalizeAngle(a2t(lonMin), 0));
//        lonMax = t2a(MathUtils.normalizeAngle(a2t(lonMax), 0));
//        latMin = MathUtils.normalizeAngle(latMin, 0);
//        latMax = MathUtils.normalizeAngle(latMax, 0);
        latMin = Util.clamp(latMin, -90, +90);
        latMax = Util.clamp(latMax, -90, +90);

        if (lonMin > lonMax) { /* swap */ var c = lonMin; lonMin = lonMax; lonMax = c; }
        if (latMin > latMax) { /* swap */ var c = latMin; latMin = latMax; latMax = c; }

        double centerX = (lonMin + lonMax)/2;
        double centerY = (latMin + latMax)/2;

//        //TODO is there a way to do this without looping
        while (centerX < -180) centerX += 360;
        while (centerX > +180) centerX -= 360;

        double wHalf = Math.min(Math.abs(lonMax - lonMin), 360)/2;
        double hHalf = Math.min(Math.abs(latMax - latMin), 180)/2;

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
                new HyperRectDouble(
                    new double[]{NEGATIVE_INFINITY, -180, latMinFinal, NEGATIVE_INFINITY},
                    new double[]{POSITIVE_INFINITY, x1max, latMaxFinal, POSITIVE_INFINITY}
                ),new HyperRectDouble(
                    new double[]{NEGATIVE_INFINITY, x2min, latMinFinal, NEGATIVE_INFINITY},
                    new double[]{POSITIVE_INFINITY, +180, latMaxFinal, POSITIVE_INFINITY}
            ));
        } else {
            return bounds(new HyperRectDouble(
                    new double[]{NEGATIVE_INFINITY, centerX-wHalf, latMinFinal, NEGATIVE_INFINITY},
                    new double[]{POSITIVE_INFINITY, centerX+wHalf, latMaxFinal, POSITIVE_INFINITY}
            ));
        }
    }

    public <Q extends Query> Q bounds(HyperRectDouble... newBounds) {
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
