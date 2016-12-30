package spimedb.plan;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.ml.clustering.*;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.geojson.LngLatAlt;
import spimedb.NObject;

import java.util.*;

/**
 * @author me
 *         <p>
 *         http://commons.apache.org/proper/commons-math/javadocs/api-3.3/org/apache/commons/math3/ml/clustering/FuzzyKMeansClusterer.html
 */
public class SpacetimeTagPlan {

    public final Dimensions dimensions;
    public final List<Goal> goals = new ArrayList();

    //Parameters, can be changed between computations
    protected double timeWeight = 1.0;  //normalized to seconds
    protected double spaceWeight = 1.0; //meters
    protected double altWeight = 1.0; //meters
    protected double tagWeight = 1.0;  //weight per each individual tag
    protected double minPossibilityTagStrength = 0.02; //minimum strength that a resulting tag must be to be added to a generated Possibility

    //internal
    private final boolean time;
    private final boolean space;
    private final boolean spaceAltitude;
    private final boolean tags;
    public final List<NObject> objects;
    private double timeWeightNext = timeWeight;
    private double spaceWeightNext = spaceWeight;
    private double tagWeightNext = tagWeight;
    private double altWeightNext = altWeight;

    static class Dimensions extends ArrayList<String> {
        private static final long ETERNAL = Long.MIN_VALUE;
        public final long timePeriod;
        private static final long NullTimePoint = -1;
        double[] min, max;
        private int tagIndex = -1; //index where tags begin in the mapping; they continue until the end

        /**
         * @param timePeriod in ms (unixtime)
         */
        public Dimensions(long timePeriod) {
            this.timePeriod = timePeriod;
        }

        /**
         * reset before generating a new sequence of goals
         */
        public void reset() {
            min = max = null;
        }

        public List<Goal> newGoals(NObject o) {
            boolean firstGoal = false;
            if (min == null) {
                firstGoal = true;
                min = new double[size()];
                max = new double[size()];
            }

            List<Goal> goals = new LinkedList();
            Map<String, Object> ts = o.getData();


            List<Long> times = new FastList(2);

            if (get(0).equals("time")) {
                //convert time ranges to a set of time points
                long[] startEnd = o.whenLong();

                long start = startEnd[0];
                long end = startEnd[1];

                if (end != ETERNAL) {
                    times.addAll(new TimeRange(start, end).discretize(timePeriod));
                } else if (start != ETERNAL) {
                    times.add(start);
                }
                //} else {
//                    long tp = o.when();
//                    if (tp!=-1) {
//                        times.add(tp);
//                    }
//                    else {
                //no time involvement, ignore this NObject
                return goals;
                //}

            } else

            {
                //add a null timepoint so the following iteration occurs
                times.add(NullTimePoint);
            }

            tagIndex = -1;

            for (
                    long currentTime
                    : times)

            {

                double[] d = new double[this.size()];
                int i = 0;

                for (String s : this) {
                    switch (s) {
                        case "lat":
                            d[i] = latitude(o);
                            break;
                        case "lon":
                            d[i] = longitude(o);
                            break;
                        case "time":
                            d[i] = currentTime;
                            break;
                        case "alt":
                            d[i] = altitude(o);
                            break;
                        default:
                            if (tagIndex == -1) {
                                tagIndex = i;
                            }
                            Object v = ts.get(s);
                            if (v instanceof Number) {
                                double strength = ((Number) v).doubleValue();
                                d[i] = strength;

                            }
                            break;
                    }
                    i++;
                }
                if (firstGoal) {
                    System.arraycopy(d, 0, min, 0, d.length);
                    System.arraycopy(d, 0, max, 0, d.length);
                } else {
                    for (int j = 0; j < d.length; j++) {
                        if (d[j] < min[j]) min[j] = d[j];
                        if (d[j] > max[j]) max[j] = d[j];
                    }
                }

                goals.add(new Goal(o, this, d));
            }

            return goals;
        }

        double latitude(NObject o) {
            return o.center(1);
        }

        double longitude(NObject o) {
            return o.center(2);
        }

        double altitude(NObject o) {
            return o.center(3);
        }

        /**
         * normalize (to 0..1.0) a collection of Goals with respect to the min/max calculated during the prior goal generation
         */
        public void normalize(Collection<Goal> goals) {

            for (Goal g : goals) {
                double d[] = g.getPoint();
                for (int i = 0; i < d.length; i++) {
                    double MIN = min[i];
                    double MAX = max[i];
                    if (MIN != MAX) {
                        d[i] = (d[i] - MIN) / (MAX - MIN);
                    } else {
                        d[i] = 0.5;
                    }
                }
            }
        }

        public void denormalize(Goal g) {
            denormalize(g.getPoint());
        }

        public void denormalize(double[] d) {
            for (int i = 0; i < d.length; i++) {
                double MIN = min[i];
                double MAX = max[i];
                if (MIN != MAX) {
                    d[i] = d[i] * (MAX - MIN) + MIN;
                } else {
                    d[i] = MIN;
                }
            }

            //normalize tags against each other
            if (tagIndex >= d.length) return;

            double min, max;
            min = max = d[tagIndex];
            for (int i = tagIndex + 1; i < d.length; i++) {
                if (d[i] > max) max = d[i];
                if (d[i] < min) min = d[i];
            }
            if (min != max) {
                for (int i = tagIndex; i < d.length; i++) {
                    d[i] = (d[i] - min) / (max - min);
                }
            }

        }


    }


    /**
     * a point in goal-space; the t parameter is included for referencing what the dimensions mean
     */
    public static class Goal extends DoublePoint {
        private final Dimensions mapping;

        /**
         * the involved object
         */
        private final NObject object;

        public Goal(NObject o, Dimensions t, double[] v) {
            super(v);
            this.object = o;
            this.mapping = t;
        }


    }

    //TODO add a maxDimensions parameter that will exclude dimensions with low aggregate strength

    //TODO support negative strengths to indicate avoidance

    /**
     * @param n             list of objects
     * @param tags          whether to involve tags
     * @param timePeriod    time period of discrete minimum interval; set to zero to not involve time as a dimension
     * @param space         whether to involve space latitude & longitude
     * @param spaceAltitude whether to involve space altitude
     */
    public SpacetimeTagPlan(List<NObject> n, boolean tags, long timePeriod, boolean space, boolean spaceAltitude) {

        this.objects = n;

        //1. compute mapping
        this.dimensions = new Dimensions(timePeriod);

        this.time = timePeriod > 0;
        this.space = space;
        this.spaceAltitude = spaceAltitude;
        this.tags = tags;


        if (this.time)
            dimensions.add("time");
        if (space) {
            dimensions.add("lat");
            dimensions.add("lon");
        }
        if (spaceAltitude) {
            dimensions.add("alt");
        }

        //TODO filter list of objects according to needed features for the clustering parameters

        if (tags) {
            Set<String> uniqueTags = new HashSet();
            for (NObject o : n) {
                Collections.addAll(uniqueTags, o.tag);
            }
            dimensions.addAll(uniqueTags);
        }


    }

    public interface PlanResult {
        void onFinished(SpacetimeTagPlan plan, List<Possibility> possibilities);

        void onError(SpacetimeTagPlan plan, Exception e);
    }


    public void computeAsync(PlanResult r) {
        try {
            List<Possibility> result = compute();
            r.onFinished(this, result);
        } catch (Exception e) {
            r.onError(this, e);
        }
    }

    public synchronized List<Possibility> compute() {
        goals.clear();
        dimensions.reset();

        this.spaceWeight = this.spaceWeightNext;
        this.altWeight = this.altWeightNext;
        this.timeWeight = this.timeWeightNext;
        this.tagWeight = this.tagWeightNext;

        //2. compute goal vectors
        for (NObject o : objects) {
            goals.addAll(dimensions.newGoals(o));
        }

        //3. normalize
        dimensions.normalize(goals);


        //4. distance function
        DistanceMeasure distanceMetric = (DistanceMeasure) (a, b) -> {
            double dist = 0;
            int i = 0;

            if (time) {
                dist += Math.abs(a[i] - b[i]) * timeWeight;
                i++;
            }
            if (space) {
                //TODO use earth surface distance measurement on non-normalized space lat,lon coordinates

                if (spaceWeight != 0) {
                    double dx = Math.abs(a[i] - b[i]);
                    i++;
                    double dy = Math.abs(a[i] - b[i]);
                    i++;

                    double ed = Math.sqrt(dx * dx + dy * dy);
                    dist += ed * spaceWeight;
                } else {
                    i += 2;
                }
            }
            if (spaceAltitude) {
                dist += Math.abs(a[i] - b[i]) * altWeight;
                i++;
            }
            if (tags) {
                if ((a.length > 0) && (tagWeight != 0)) {
                    double tagWeightFraction = tagWeight / (a.length);
                    for (; i < a.length; i++) {
                        dist += Math.abs(a[i] - b[i]) * tagWeightFraction;
                    }
                }
            }

            return dist;
        };

        //5. cluster

        List<? extends Cluster<Goal>> centroids = cluster(distanceMetric);

        //6. denormalize and return annotated objects
        for (Goal g : goals) {
            dimensions.denormalize(g);
        }

        return getPossibilities(centroids);
    }

    private List<? extends Cluster<Goal>> cluster(DistanceMeasure distanceMetric) {
        //return clusterDBScan(distanceMetric);
        return clusterFuzzyKMeans(distanceMetric);
    }

    private List<Cluster<Goal>> clusterDBScan(DistanceMeasure distanceMetric) {
        double radius = 1.0; //if all points are normalized
        DBSCANClusterer<Goal> clusterer = new DBSCANClusterer<>(radius, 1, distanceMetric);
        return clusterer.cluster(goals);
    }

    private List<CentroidCluster<Goal>> clusterFuzzyKMeans(DistanceMeasure distanceMetric) {

        //TODO use a clustering class to hold these for the fuzzyKmeans impl
        int numCentroids = (int) Math.ceil(Math.sqrt(goals.size()));
        int maxIterations = 5;
        double fuzziness = 1.05; // > 1

        FuzzyKMeansClusterer<Goal> clusterer = new FuzzyKMeansClusterer<>(numCentroids, fuzziness, maxIterations, distanceMetric);
        List<CentroidCluster<Goal>> centroids = clusterer.cluster(goals);
        return centroids;
    }


    public Dimensions getDimensions() {
        return dimensions;
    }

    public static class Possibility extends NObject {
        //private final double[] center;

        public Possibility(String id) {
            super(id);
            //this.center = center;
        }

        //public double[] getCenter() {
        //return center;
        //}


    }

    protected List<Possibility> getPossibilities(List<? extends Cluster<Goal>> centroids) {
        List<Possibility> l = new ArrayList(centroids.size());

        int pID = 0;
        for (Cluster<Goal> c : centroids) {
            double[] point;
            if (c instanceof CentroidCluster) {
                point = ((CentroidCluster) c).getCenter().getPoint();
            } else {
                //find the centroid of the points in the cluster
                Goal g0 = c.getPoints().get(0);

                ArrayRealVector v = new ArrayRealVector(g0.getPoint().length);
                for (Goal g : c.getPoints()) {
                    //TODO avoid allocating new ArrayRealVector here
                    v.combineToSelf(1, 1, new ArrayRealVector(g.getPoint()));
                }
                point = v.getDataRef();
            }

            dimensions.denormalize(point);

            Possibility p = new Possibility("p" + (pID++));
            int i = 0;
            if (time) {
                long when = (long) point[i++];

                //TODO use timerange based on discretizing period duration?
                //p.add("when", new TimePoint((long)when));
                p.when(when);
            }
            LngLatAlt s = null;
            if (space) {
                double lat = point[i++];
                double lon = point[i++];
                p.where((float) lat, (float) lon);
            }


            if (spaceAltitude) {
                double alt = point[i++];
                if (s == null) {
                    //this shouldnt be used
                    p.where(0, 0, (float) alt);
                } else
                    s.setAltitude(alt);
            }


            if (tags) {
                for (; i < point.length; i++) {
                    double strength = point[i];
                    if (strength > minPossibilityTagStrength) {
                        String tag = dimensions.get(i);
                        p.put(tag, strength);
                    }
                }
            }

            l.add(p);
        }

        return l;
    }

    public void setTimeWeight(double timeWeight) {
        this.timeWeightNext = timeWeight;
    }

    public void setSpaceWeight(double spaceWeight) {
        this.spaceWeightNext = spaceWeight;
    }

    public void setTagWeight(double tagWeight) {
        this.tagWeightNext = tagWeight;
    }

    public void setAltWeight(double altWeight) {
        this.altWeightNext = altWeight;
    }

    public double getAltWeight() {
        return altWeight;
    }

    public double getSpaceWeight() {
        return spaceWeight;
    }

    public double getTagWeight() {
        return tagWeight;
    }

    public double getTimeWeight() {
        return timeWeight;
    }


}
