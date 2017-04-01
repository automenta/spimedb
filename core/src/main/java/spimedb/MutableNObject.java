package spimedb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jcog.tree.rtree.point.DoubleND;
import org.eclipse.collections.impl.factory.Iterables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opensextant.geodesy.*;
import org.opensextant.giscore.geometry.Line;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.geometry.Polygon;
import spimedb.util.JSON;

import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;


/**
 * dimensions: time (t), lon (x), lat (y), alt (z), ...
 * <p>
 * https://github.com/FasterXML/jackson-annotations/
 * <p>
 * https://github.com/infinispan/infinispan/blob/master/query/src/test/java/org/infinispan/query/test/Person.java
 * https://github.com/infinispan/infinispan/blob/master/query/src/test/java/org/infinispan/query/queries/spatial/QuerySpatialTest.java#L78
 */

@JsonSerialize(using = NObject.NObjectSerializer.class)
public class MutableNObject extends ImmutableNObject {


    public MutableNObject(String id) {
        this(id, null);
    }

    public MutableNObject(NObject copy) {
        super(copy.bounded() ? copy.min() : unbounded, copy.bounded() ? copy.max() : unbounded, copy.id(), copy.name());
        copy.forEach(this::put);
    }

    public MutableNObject(String id, String name) {
        super(DoubleND.fill(4, Float.NEGATIVE_INFINITY), DoubleND.fill(4, Float.POSITIVE_INFINITY), id, name);
    }

    public MutableNObject putLater(String key, float pri, Supplier lazy) {
        return putLater(key, pri, null, lazy);
    }

    public MutableNObject putLater(String key, float pri, Object intermediate, Supplier lazy) {
        return put(key, new LazyValue(key, intermediate, pri, lazy));
    }

    public MutableNObject put(String key, Object value) {

        switch (key) {
            case TAG:
                String[] tt;
                if (value instanceof String[])
                    tt = ((String[]) value).clone();
                else if (value instanceof String)
                    tt = ((String)value).split(" "); //new String[] { (String) value };
                else if (value instanceof ArrayNode) {
                    tt = JSON.toStrings((ArrayNode)value);
                } else
                    throw new RuntimeException("invalid tag property");

                withTags(tt);
                return this;

//            case "<":
//                //HACK
//                setOutside(Sets.newHashSet((String[])value));
//                return this;

            case ID:
                if ((value instanceof String) && (value.equals(id))) {
                    //already being set to same ID
                    return this;
                }
                throw new RuntimeException(this + " can not change ID");
            case NAME:
                name(value.toString());
                return this;
        }

        synchronized (data) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }
        return this;
    }

    public void description(String d) {
        if (d.isEmpty()) {
            if (data != null)
                data.remove(DESC);
            return;
        }

        put(DESC, d);
    }


    public MutableNObject name(String name) {
        this.name = name;
        return this;
    }


    public NObject where(Geodetic2DPoint c) {

        float lon = (float) c.getLongitudeAsDegrees();
        min.coord[1] = max.coord[1] = lon;

        float lat = (float) c.getLatitudeAsDegrees();
        min.coord[2] = max.coord[2] = lat;


        if (c instanceof Geodetic3DPoint) {
            float ele = (float) ((Geodetic3DPoint) c).getElevation();
            min.coord[3] = max.coord[3] = ele;
        } else {
            min.coord[3] = Float.NEGATIVE_INFINITY;
            max.coord[3] = Float.POSITIVE_INFINITY;
        }

        //put("g" + NObject.POINT, )

        return this;
    }

    public NObject where(Longitude AX, Longitude BX, Latitude AY, Latitude BY) {


        {
            float a = (float) AX.inDegrees();
            float b = (float) BX.inDegrees();
            if (a > b) {
                float t = a;
                a = b;
                b = t;
            } //swap
            min.coord[1] = a;
            max.coord[1] = b;
            assert (a <= b);
        }

        {
            float a = (float) AY.inDegrees();
            float b = (float) BY.inDegrees();
            if (a > b) {
                float t = a;
                a = b;
                b = t;
            } //swap
            min.coord[2] = a;
            max.coord[2] = b;
            assert (a <= b);
        }

//        if ((a instanceof Geodetic3DPoint) && (b instanceof Geodetic3DPoint)) {
//            float az = (float) ((Geodetic3DPoint) a).getElevation();
//            float bz = (float) ((Geodetic3DPoint) b).getElevation();
//            if (az > bz) { float t = az; az = bz; bz = t; } //swap
//            min.coord[3] = az;
//            max.coord[3] = bz;
//            assert(min.coord[3] < max.coord[3]);
//        } else {
//            min.coord[3] = Float.NEGATIVE_INFINITY;
//            max.coord[3] = Float.POSITIVE_INFINITY;
//        }

        return this;
    }

    public void where(Geodetic2DBounds bb) {
        where(bb.getEastLon(), bb.getWestLon(), bb.getSouthLat(), bb.getNorthLat());
    }


    public static double[][] toArray(List<Point> lp) {
        double[][] points = new double[lp.size()][2];

        for (int i = 0; i < points.length; i++) {
            Geodetic2DPoint c = lp.get(i).getCenter();
            double[] pi = points[i];
            pi[0] = c.getLatitudeAsDegrees();
            pi[1] = c.getLongitudeAsDegrees();
        }
        return points;
    }
    public NObject where(Line l) {

        List<Point> lp = l.getPoints();
        double[][] points = toArray(lp);

        where(l.getBoundingBox());
        put(NObject.LINESTRING, points);
        return this;
    }

    public NObject where(Polygon p) {
        double[][] outerRing = toArray(p.getOuterRing().getPoints());

        //TODO handle inner rings

        where(p.getBoundingBox());
        put(NObject.POLYGON, outerRing);
        return this;
    }

    public MutableNObject withTags(String... tags) {
        return withTags(Iterables.iList(tags));
    }

    /**
     * sets the inside property
     */
    public MutableNObject withTags(Iterable<? extends String> tags) {

        TreeSet<String> s = new TreeSet();

        for (String t : tags) {
            if (t.equals(id()))
                throw new RuntimeException("object can not be inside itself");
            s.add(t);
        }

        if (s.isEmpty()) {
            this.tag = SpimeDB.ROOT;
        } else {
            this.tag = s.toArray(new String[s.size()]);
        }


        return this;
    }


    public MutableNObject when(long when) {
        min.coord[0] = max.coord[0] = when;
        return this;
    }

    public MutableNObject when(long start, long end) {
        min.coord[0] = start;
        max.coord[0] = end;
        return this;
    }

    public void when(float when) {
        min.coord[0] = when;
        max.coord[0] = when;
    }

    public void setEternal() {
        min.coord[0] = Float.NEGATIVE_INFINITY;
        max.coord[0] = Float.POSITIVE_INFINITY;
    }

    public void where(float x, float y) {
        min.coord[1] = x;
        max.coord[1] = x;
        min.coord[2] = y;
        max.coord[2] = y;
    }

    public void where(float x, float y, float z) {
        where(x, y);
        if (z!=z) { //avoid NaN
            min.coord[3] = Float.NEGATIVE_INFINITY;
            max.coord[3] = Float.POSITIVE_INFINITY;
        } else {
            min.coord[3] = z;
            max.coord[3] = z;
        }
    }

    @Deprecated
    public long[] whenLong() {
        double a = min.coord[0];
        double b = max.coord[0];
        return new long[]{(long) a, (long) b};
    }

    @Nullable
    public <X> X remove(String key) {
        if (data != null)
            return (X) data.remove(key);
        return null;
    }


    public Object /* previous */ putIfAbsent(String key, @NotNull Object value) {
        return data.putIfAbsent(key, value);
    }

    public MutableNObject without(String k) {
        remove(k);
        return this;
    }

    public MutableNObject putAll(JsonNode x) {
        x.fields().forEachRemaining(e -> {
            JsonNode v = e.getValue();
            Object s;
            if (v.isBoolean())
                s = v.booleanValue();
            else if (v.isNumber()/* || v.canConvertToInt()*/)
                s = v.numberValue();
            else
                s = v.toString();
            put(e.getKey(), s);

        });
        return this;
    }


//    /** produces a "1-line" summar JSON object as a string */
//    @JsonIgnore public String summary(StringBuilder sb) {
//        sb.setLength(0);
//
//        sb.append("{\"I\":\"").append(getId()).append('"');
//
//        String name = getName();
//        if (name!=null) {
//            //TODO use nars Utf8
//            sb.append(",\"N\":\"").append(name).append('"');
//        }
//
////        if (isSpatial())
////            sb.append(",\"S\":").append(Arrays.toString(spacetime)); //TODO append
////        if (isTemporal())
////            sb.append(",\"T\":").append(Arrays.toString(time)); //TODO append
//
//        sb.append('}');
//
//        return sb.toString();
//    }


}