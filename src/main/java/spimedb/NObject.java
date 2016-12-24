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
import spimedb.sense.ImportKML;
import spimedb.util.geom.AABB;
import spimedb.util.geom.BB;

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
public class NObject implements Serializable, IdBB {

    final static long ETERNAL = Long.MIN_VALUE;

    /*@Field(store = Store.YES)*/ @JsonProperty("I") final String id;

    /*@Field(store = Store.YES)*/ @JsonProperty("N") String name;

    //@JsonProperty("T") long[] time = null;

    /** lat, lon, alt (m), [planet ID?] */
    //@JsonProperty("S")
    final AABB bounds = new AABB();


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

    /**
     * timepoint, or -1 if none
     */
    final public float timeStart() {
        return bounds.minZ();
    }
    final public float timeStop() {
        return bounds.maxZ();
    }


    public Collection<String> tagSet() {
        return data.keySet();
    }

    public NObject when(final float at) {
        return when(at,at);
    }

    public NObject when(final float  start, final float end) {
        bounds.setRangeZ(start, end);
        return this;
    }

    public NObject where(float[] coord) {
        return where(coord[0], coord[1], coord[2]);
    }

    public NObject where(float lat, float lng, float alt, Object... geometry) {
        bounds.setX(lat);
        bounds.setY(lng);
        //TODO alt

//        space[0] = lat;
//        space[1] = lng;
//        space[2] = alt;

        if (geometry.length > 0) {
            put("g", geometry);
        }


        return this;
    }

//    public NObject where(NObject otherLocation) {
//        return where(otherLocation.spacetime);
//    }

    public NObject where(double lat, double lon) {
        return where((float)lat, (float)lon, Float.NaN);
    }
    public NObject where(double lat, double lon, double alt, Object... shape) {
        return where((float)lat, (float)lon, (float)alt, shape);
    }

    public NObject where(float lat, float lng) {
        return where(lat, lng, Float.NaN);
    }

//    //TODO provide non-boxed versoins of these
//    @JsonIgnore
//    //@Latitude
//    public Double getLatitude() {
//        if (space!=null)
//            return Double.valueOf(space[0]);
//        return Double.NaN;
//    }
//
//    @JsonIgnore
//    //@Longitude
//    public Double getLongitude() {
//        if (space!=null)
//            return Double.valueOf(space[1]);
//        return Double.NaN;
//    }

//    @JsonIgnore
//    public float getAltitude() {
//        if (space!=null)
//            return space[2];
//        return Float.NaN;
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


    @JsonIgnore
    public boolean isSpatial() {
        return bounds !=null;
    }
    @JsonIgnore
    public boolean isTemporal() {
        return bounds.hasZ();
    }

    public NObject now() {
        when( System.currentTimeMillis() );
        return this;
    }

    public NObject name(String name) {
        this.name = name;
        return this;
    }

    public final static String POINT = ".";
    public final static String LINESTRING = "-";
    public final static String POLYGON = "*";


    public NObject where(Geodetic2DPoint c, Object... shape) {

        double lat = c.getLatitudeAsDegrees();
        double lon = c.getLongitudeAsDegrees();
        double ele = Double.NaN;
        if (c instanceof Geodetic3DPoint) {
            ele = ((Geodetic3DPoint)c).getElevation();
        }

        //http://geojson.org/
        return where(lat, lon, ele, shape);
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


    @Override
    public final String id() {
        return id;
    }

    @Override
    public final BB getBB() {
        return bounds;
    }

    public final float getLatitude() {
        return getBB().x();
    }
    public final float getLongitude() {
        return getBB().y();
    }

    public static float getAltitude() {
        return Float.NaN; //TODO
    }
}
