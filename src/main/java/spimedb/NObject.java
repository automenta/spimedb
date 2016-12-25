package spimedb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Geodetic3DPoint;
import org.opensextant.giscore.geometry.Line;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.geometry.Polygon;
import spimedb.index.rtree.PointND;
import spimedb.index.rtree.RectND;
import spimedb.sense.ImportKML;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.hibernate.search.annotations.Field;
//import org.hibernate.search.annotations.Indexed;
//import org.hibernate.search.annotations.Store;



/**
 *
 * dimensions: time, lat, lon, alt, ...
 *
 * https://github.com/FasterXML/jackson-annotations/
 *
 * https://github.com/infinispan/infinispan/blob/master/query/src/test/java/org/infinispan/query/test/Person.java
 * https://github.com/infinispan/infinispan/blob/master/query/src/test/java/org/infinispan/query/queries/spatial/QuerySpatialTest.java#L78
 *
 */

@JsonSerialize
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.NON_PRIVATE)
@JsonInclude(value= JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
//@Indexed
public class NObject extends RectND implements Serializable {


    /*@Field(store = Store.YES)*/ @JsonProperty("I") final String id;

    /*@Field(store = Store.YES)*/ @JsonProperty("N") String name;

    //@JsonProperty("T") long[] time = null;



    //TODO use a ObjectDouble primitive map structure
    @JsonProperty("^") Map<String, Object> data = null;


    /** extensional inheritance: what this nobject is "inside" of (its container) */
    /*@Field(name = "inside")*/ @JsonProperty(">") private String inside = null;

//    /** extensional inheritance: what this nobject is "outside" of (its contents) */
//    @Field(name = "outside") @JsonProperty("<") private Set<String> outside = new HashSet();

    public NObject() {
        this(Core.uuid());
    }

    public NObject(String id) {
        this(id, null);
    }

    public NObject(String id, String name) {
        super(PointND.fill(4, Float.NEGATIVE_INFINITY), PointND.fill(4, Float.POSITIVE_INFINITY));

        this.id = id!=null ? id : Core.uuid();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getData() {
        return data;
    }



    public Collection<String> tagSet() {
        return data.keySet();
    }

//    /**
//     * timepoint, or -1 if none
//     */
//    final public float timeStart() {
//        return bounds.minZ();
//    }
//    final public float timeStop() {
//        return bounds.maxZ();
//    }
//
//    public NObject when(final float at) {
//        return when(at,at);
//    }
//
//    public NObject when(final float  start, final float end) {
//        bounds.setRangeZ(start, end);
//        return this;
//    }
//
//    public NObject where(float[] coord) {
//        return where(coord[0], coord[1], coord[2]);
//    }
//
//    public NObject where(float lat, float lng, float alt, Object... geometry) {
//        bounds.setX(lat);
//        bounds.setY(lng);
//        bounds.setZ(alt);
//
////        space[0] = lat;
////        space[1] = lng;
////        space[2] = alt;
//
//        if (geometry.length > 0) {
//            put("g", geometry);
//        }
//
//
//        return this;
//    }
//
////    public NObject where(NObject otherLocation) {
////        return where(otherLocation.spacetime);
////    }
//
//    public NObject where(double lat, double lon) {
//        return where((float)lat, (float)lon);
//    }
//
//    public NObject where(double lat, double lon, double alt, Object... shape) {
//        return where((float)lat, (float)lon, (float)alt, shape);
//    }
//
//    public NObject where(float lat, float lng) {
//
//        return where(lat, lng, 0 /* Float.NaN*/);
//    }

    public <X> X get(String tag) {
        return (X) data.get(tag);
    }

    public NObject put(String tag) {
        return put(tag, 1.0f);
    }

    public NObject put(String tag, Object value) {
        switch (tag) {
            case ">": setInside((String)value); return this;
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
            case "N": name(value.toString()); return this;
        }

        if (data == null) data = new HashMap();
        data.put(tag, value);
        return this;
    }




    public void description(String d) {
        put("_", d);
    }

    public String description() {
        return get("_").toString();
    }


    @Override
    public String toString() {
        return Core.toJSON(this);
    }




    public NObject name(String name) {
        this.name = name;
        return this;
    }

    public final static String POINT = ".";
    public final static String LINESTRING = "-";
    public final static String POLYGON = "*";


    public NObject where(Geodetic2DPoint c, Object... shape) {

        float lat = (float) c.getLatitudeAsDegrees();

        min.coord[1] = max.coord[1] = lat;

        float lon = (float) c.getLongitudeAsDegrees();
        min.coord[2] = max.coord[2] = lon;


        if (c instanceof Geodetic3DPoint) {
            float ele = (float) ((Geodetic3DPoint) c).getElevation();
            min.coord[3] = max.coord[3] = ele;
        } else {
            min.coord[3] = Float.NEGATIVE_INFINITY;
            max.coord[3] = Float.POSITIVE_INFINITY;
        }

        return this;
    }

    public NObject where(Line l) {

        List<Point> lp = l.getPoints();
        double[][] points = ImportKML.toArray(lp);

        return where(l.getCenter(), NObject.LINESTRING, points);

    }

    public NObject where(Polygon p) {
        double[][] outerRing = ImportKML.toArray(p.getOuterRing().getPoints());

        //TODO handle inner rings

        return where(p.getCenter(), NObject.POLYGON, new double[][][]{outerRing /* inner rings */});
    }

    /** sets the inside property */
    public NObject setInside(String parents) {
        this.inside = parents;
        if (this.inside.equals(getId()))
            throw new RuntimeException("object can not be inside itself");
        return this;
    }

//    /** sets the children property */
//    public NObject setOutside(Set<String> content) {
//        this.outside = content;
//        return this;
//    }

    public String inside() { return inside; }
    //public Set<String> outside() { return outside; }


    /** produces a "1-line" summar JSON object as a string */
    @JsonIgnore public String summary(StringBuilder sb) {
        sb.setLength(0);

        sb.append("{\"I\":\"").append(getId()).append('"');

        String name = getName();
        if (name!=null) {
            //TODO use nars Utf8
            sb.append(",\"N\":\"").append(name).append('"');
        }

//        if (isSpatial())
//            sb.append(",\"S\":").append(Arrays.toString(spacetime)); //TODO append
//        if (isTemporal())
//            sb.append(",\"T\":").append(Arrays.toString(time)); //TODO append

        sb.append('}');

        return sb.toString();
    }


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

    public void eternal() {
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

}
