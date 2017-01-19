package spimedb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opensextant.geodesy.*;
import org.opensextant.giscore.geometry.Line;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.geometry.Polygon;
import spimedb.index.rtree.PointND;
import spimedb.index.rtree.RectND;
import spimedb.sense.KML;
import spimedb.util.JSON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;


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

@JsonSerialize(using= AbstractNObject.NObjectSerializer.class)
public class MutableNObject extends RectND implements AbstractNObject {


    @Override
    public void forEach(BiConsumer<String, Object> each) {
        if (data!=null)
            data.forEach(each);
    }


    @Override
    public String[] tags() {
        return tag;
    }

    @JsonProperty("I") final String id;

    @JsonProperty("N") String name;





    //TODO use a ObjectDouble primitive map structure
    @JsonProperty("^") Map<String, Object> data = null;


    /** extensional inheritance: what this nobject is "inside" of (its container)
     *  by default, use the root node. but try not to pollute it
     */
    @JsonProperty(">") public String[] tag = Tags.ROOT;

//    /** extensional inheritance: what this nobject is "outside" of (its contents) */
//    @Field(name = "outside") @JsonProperty("<") private Set<String> outside = new HashSet();


    public MutableNObject(String id) {
        this(id, null);
    }

    public MutableNObject(MutableNObject copy) {
        this(copy.id, copy.name);
        setTag(copy.tag);
        data.putAll(copy.data);
    }

    public MutableNObject(String id, String name) {
        super(PointND.fill(4, Float.NEGATIVE_INFINITY), PointND.fill(4, Float.POSITIVE_INFINITY));

        this.id = id!=null ? id : JSON.uuid();
        this.name = name;
    }



    @Override
    public String name() {
        return name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        MutableNObject no =(MutableNObject)o;
        return id.equals(no.id);// && super.equals(o);
    }

    @Override
    public int hashCode() {
        return id.hashCode(); //super.hashCode();
    }



    @Override
    public <X> X get(String tag) {
        return (X) data.get(tag);
    }


    public AbstractNObject put(String key, Object value) {
        switch (key) {
            case ">":
                if (value instanceof String[])
                    setTag((String[])value);
                else if (value instanceof String)
                    setTag((String)value);
                else
                    throw new RuntimeException("invalid tag property");
                return this;

//            case "<":
//                //HACK
//                setOutside(Sets.newHashSet((String[])value));
//                return this;

            case "I":
                if ((value instanceof String) && (value.equals(id))) {
                    //already being set to same ID
                    return this;
                }
                throw new RuntimeException(this + " can not change ID");
            case "N":
                name(value.toString());
                return this;
        }

        if (data == null) data = new HashMap<>();
        data.put(key, value);
        return this;
    }

    public void description(String d) {
        if (d.isEmpty()) {
            if (data!=null)
                data.remove("_");
            return;
        }

        put("_", d);
    }




    @Override
    public String toString() {
        return new String(JSON.toJSON(this));
    }




    public AbstractNObject name(String name) {
        this.name = name;
        return this;
    }

    //public final static String POINT = ".";
    public final static String LINESTRING = "-";
    public final static String POLYGON = "*";


    public AbstractNObject where(Geodetic2DPoint c) {

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

    public AbstractNObject where(Longitude AX, Longitude BX, Latitude AY, Latitude BY) {


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


    public AbstractNObject where(Line l) {

        List<Point> lp = l.getPoints();
        double[][] points = KML.toArray(lp);

        where(l.getBoundingBox());
        put('g' + MutableNObject.LINESTRING, points);
        return this;
    }

    public AbstractNObject where(Polygon p) {
        double[][] outerRing = KML.toArray(p.getOuterRing().getPoints());

        //TODO handle inner rings

        where(p.getBoundingBox());
        put('g' + MutableNObject.POLYGON, outerRing);
        return this;
    }

    /** sets the inside property */
    public AbstractNObject setTag(String... tags) {

        if (tags.length > 1) {
            //TODO remove any duplicates
        }

        for (String t : tags) {
            if (t.equals(id()))
                throw new RuntimeException("object can not be inside itself");
        }

        if (tags.length == 0)
            tags = Tags.ROOT;

        this.tag = tags;

        return this;
    }



    @Override
    public final String id() {
        return id;
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


    @Override
    public boolean bounded() {
        return super.bounded();
    }
}
