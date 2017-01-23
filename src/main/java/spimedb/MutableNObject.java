package spimedb;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jetbrains.annotations.Nullable;
import org.opensextant.geodesy.*;
import org.opensextant.giscore.geometry.Line;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.geometry.Polygon;
import spimedb.index.rtree.PointND;
import spimedb.io.KML;

import java.util.List;


/**
 *
 * dimensions: time (t), lon (x), lat (y), alt (z), ...
 *
 * https://github.com/FasterXML/jackson-annotations/
 *
 * https://github.com/infinispan/infinispan/blob/master/query/src/test/java/org/infinispan/query/test/Person.java
 * https://github.com/infinispan/infinispan/blob/master/query/src/test/java/org/infinispan/query/queries/spatial/QuerySpatialTest.java#L78
 *
 */

@JsonSerialize(using= NObject.NObjectSerializer.class)
public class MutableNObject extends ImmutableNObject {


    public MutableNObject(String id) {
        this(id, null);
    }

    public MutableNObject(NObject copy) {
        this(copy.id(), copy.name());
        withTags(copy.tags());
        copy.forEach(data::put);
    }

    public MutableNObject(String id, String name) {
        super(PointND.fill(4, Float.NEGATIVE_INFINITY), PointND.fill(4, Float.POSITIVE_INFINITY), id, name);

    }


    public MutableNObject put(String key, Object value) {
        switch (key) {
            case TAG:
                if (value instanceof String[])
                    withTags((String[])value);
                else if (value instanceof String)
                    withTags((String)value);
                else
                    throw new RuntimeException("invalid tag property");
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
            data.put(key, value);
        }
        return this;
    }

    public void description(String d) {
        if (d.isEmpty()) {
            if (data!=null)
                data.remove(DESC);
            return;
        }

        put("_", d);
    }


    public NObject name(String name) {
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
            if (a > b) { float t = a; a = b; b = t; } //swap
            min.coord[1] = a;
            max.coord[1] = b;
            assert (a <= b);
        }

        {
            float a = (float) AY.inDegrees();
            float b = (float) BY.inDegrees();
            if (a > b) { float t = a; a = b; b = t; } //swap
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


    public NObject where(Line l) {

        List<Point> lp = l.getPoints();
        double[][] points = KML.toArray(lp);

        where(l.getBoundingBox());
        put('g' + MutableNObject.LINESTRING, points);
        return this;
    }

    public NObject where(Polygon p) {
        double[][] outerRing = KML.toArray(p.getOuterRing().getPoints());

        //TODO handle inner rings

        where(p.getBoundingBox());
        put('g' + MutableNObject.POLYGON, outerRing);
        return this;
    }

    /** sets the inside property */
    public MutableNObject withTags(String... tags) {

        if (tags.length > 1) {
            //TODO remove any duplicates
        }

        for (String t : tags) {
            if (t.equals(id()))
                throw new RuntimeException("object can not be inside itself");
        }

        if (tags.length == 0)
            tags = SpimeDB.ROOT;

        this.tag = tags;

        return this;
    }


    public void when(long start, long end) {
        min.coord[0] = start;
        max.coord[0] = end;
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
        min.coord[3] = z;
        max.coord[3] = z;
    }

    @Deprecated public long[] whenLong() {
        float a = min.coord[0];
        float b = max.coord[0];
        return new long[] { (long)a, (long)b };
    }

    @Nullable
    public <X> X remove(String key) {
        if (data!=null)
            return (X)data.remove(key);
        return null;
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
