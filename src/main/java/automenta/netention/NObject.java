package automenta.netention;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hibernate.search.annotations.*;
import org.hibernate.search.spatial.Coordinates;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.geodesy.Geodetic3DPoint;
import org.opensextant.giscore.geometry.Line;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.geometry.Polygon;

import java.io.Serializable;
import java.util.*;

import static automenta.netention.geo.ImportKML.toArray;

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
@Indexed
@Spatial(spatialMode = SpatialMode.RANGE, store=Store.YES ) //http://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/
public class NObject implements Serializable, Coordinates {

    final static long ETERNAL = Long.MIN_VALUE;

    @Field(store = Store.YES) @JsonProperty("I") final String id;

    @Field(store = Store.YES) @JsonProperty("N") String name;

    @JsonProperty("T") long[] time = null;

    /** lat, lon, alt (m), [planet ID?] */
    @JsonProperty("S") float[] space = null;


    //TODO use a ObjectDouble primitive map structure
    @JsonProperty("^") Map<String, Object> fields = null;


    /** extensional inheritance: what this nobject is "inside" of (its container) */
    @Field(name = "inside") @JsonProperty(">") private String inside = null;

//    /** extensional inheritance: what this nobject is "outside" of (its contents) */
//    @Field(name = "outside") @JsonProperty("<") private Set<String> outside = new HashSet();

    public NObject() {
        this(Core.uuid());
    }

    public NObject(String id) {
        this(id, null);

    }

    public NObject(String id, String name) {
        if (id == null)
            id = Core.uuid();
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    /**
     * timepoint, or -1 if none
     */
    public long[] getTime() {
        return time;
    }


    public Collection<String> tagSet() {
        return fields.keySet();
    }

    public NObject when(final long at) {
        return when(at,at);
    }

    public NObject when(final long start, final long end) {
        if (this.time == null) this.time = new long[2];
        this.time[0] = start;
        this.time[1] = end;
        return this;
    }

    public NObject where(float[] coord) {
        return where(coord[0], coord[1], coord[2]);
    }

    public NObject where(float lat, float lng, float alt, Object... shaped) {

        if (space == null) space = new float[3];
        space[0] = lat;
        space[1] = lng;
        space[2] = alt;

        if (shaped.length > 0) {
            put("s", shaped);
        }


        return this;
    }

    public NObject where(NObject otherLocation) {
        return where(otherLocation.space);
    }

    public NObject where(double lat, double lon) {
        return where((float)lat, (float)lon, Float.NaN);
    }
    public NObject where(double lat, double lon, double alt, Object... shape) {
        return where((float)lat, (float)lon, (float)alt, shape);
    }

    public NObject where(float lat, float lng) {
        return where(lat, lng, Float.NaN);
    }

    //TODO provide non-boxed versoins of these
    @JsonIgnore
    //@Latitude
    public Double getLatitude() {
        if (space!=null)
            return Double.valueOf(space[0]);
        return Double.NaN;
    }

    @JsonIgnore
    //@Longitude
    public Double getLongitude() {
        if (space!=null)
            return Double.valueOf(space[1]);
        return Double.NaN;
    }

    @JsonIgnore
    public float getAltitude() {
        if (space!=null)
            return space[2];
        return Float.NaN;
    }

    public <X> X get(String tag) {
        return (X) fields.get(tag);
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

        if (fields == null) fields = new HashMap();
        fields.put(tag, value);
        return this;
    }




    public void description(String d) {
        put("_", d);
    }

    public String description() {
        return get("_").toString();
    }

    @JsonIgnore
    public long getTimeStart() {
        if (time == null) return ETERNAL;
        return time[0];
    }

    @JsonIgnore
    public long getTimeEnd() {
        if (time == null) return ETERNAL;
        return time[1];
    }

    @Override
    public String toString() {
        return Core.toJSON(this);
    }


    @JsonIgnore
    public boolean isSpatial() {
        return space!=null;
    }
    @JsonIgnore
    public boolean isTemporal() {
        return time!=null;
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
        double[][] points = toArray(lp);

        return where(l.getCenter(), NObject.LINESTRING, points);

    }

    public NObject where(Polygon p) {
        double[][] outerRing = toArray(p.getOuterRing().getPoints());

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

        sb.append("{\"I\":\"").append(getId()).append("\"");

        String name = getName();
        if (name!=null)
            sb.append(",\"N\":\"").append(name).append("\"");
        if (isSpatial())
            sb.append(",\"S\":").append(Arrays.toString(space)); //TODO append
        if (isTemporal())
            sb.append(",\"T\":").append(Arrays.toString(time)); //TODO append

        sb.append('}');

        return sb.toString();
    }
}
