package spimedb;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opensextant.geodesy.*;
import org.opensextant.giscore.geometry.Line;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.geometry.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.index.rtree.PointND;
import spimedb.index.rtree.RectND;
import spimedb.sense.KML;
import spimedb.util.JSON;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



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

@JsonSerialize(using=NObject.NObjectSerializer.class)
//@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.NON_PRIVATE)
//@JsonInclude(value= JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
//@Indexed
public class NObject extends RectND implements Serializable {


    static final class NObjectSerializer extends JsonSerializer<NObject> {

        @Override
        public void serialize(NObject o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            {
                jsonGenerator.writeStringField("I", o.id);
                if (o.name!=null)
                    jsonGenerator.writeStringField("N", o.name);

                if (o.tag!=null && o.tag.length > 0) {
                    jsonGenerator.writeObjectField(">", o.tag);
                }

                if (o.data!=null) {
                    //inline the map data
                    o.data.forEach((k,v)->{
                        try {
                            jsonGenerator.writeObjectField(k, v);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }

                //zip the min/max bounds
                if (o.bounded()) {
                    jsonGenerator.writeFieldName("@");
                    jsonGenerator.writeStartArray();
                    if (!o.max.equals(o.min)) {
                        int dim = o.min.dim();
                        for (int i = 0; i < dim; i++) {
                            float a = o.min.coord[i];
                            float b = o.max.coord[i];
                            if (a == b) {
                                jsonGenerator.writeNumber(a);
                            } else {
                                if (a == Float.NEGATIVE_INFINITY && b == Float.POSITIVE_INFINITY) {
                                    jsonGenerator.writeNumber(Float.NaN);
                                } else {
                                    jsonGenerator.writeStartArray();
                                    jsonGenerator.writeNumber(a);
                                    jsonGenerator.writeNumber(b);
                                    jsonGenerator.writeEndArray();
                                }
                            }
                        }
                    } else {
                        writeArrayValues(o.min.coord, jsonGenerator);
                    }
                    jsonGenerator.writeEndArray();
                }

            }
            jsonGenerator.writeEndObject();
        }

        private static void writeArrayValues(float[] xx, JsonGenerator jsonGenerator) throws IOException {
            for (float x : xx) {
                float y;
                if (x == Float.POSITIVE_INFINITY || x == Float.NEGATIVE_INFINITY)
                    y = Float.NaN; //as string, "NaN" is shorter than "Infinity"
                else
                    y = x;

                jsonGenerator.writeNumber(y);
            }
        }

    }

    /*@Field(store = Store.YES)*/ @JsonProperty("I") final String id;

    /*@Field(store = Store.YES)*/ @JsonProperty("N") String name;

    //@JsonProperty("T") long[] time = null;



    //TODO use a ObjectDouble primitive map structure
    @JsonProperty("^") Map<String, Object> data = null;


    /** extensional inheritance: what this nobject is "inside" of (its container) */
    /*@Field(name = "inside")*/ @JsonProperty(">")
    public String[] tag = null;

//    /** extensional inheritance: what this nobject is "outside" of (its contents) */
//    @Field(name = "outside") @JsonProperty("<") private Set<String> outside = new HashSet();

    public NObject() {
        this(JSON.uuid());
    }

    public NObject(String id) {
        this(id, null);
    }

    public NObject(NObject copy) {
        this(copy.id, copy.name);
        setTag(copy.tag);
        data.putAll(copy.data);
    }

    public NObject(String id, String name) {
        super(PointND.fill(4, Float.NEGATIVE_INFINITY), PointND.fill(4, Float.POSITIVE_INFINITY));

        this.id = id!=null ? id : JSON.uuid();
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        NObject no =(NObject)o;
        return id.equals(no.id);// && super.equals(o);
    }

    @Override
    public int hashCode() {
        return id.hashCode(); //super.hashCode();
    }



    public <X> X get(String tag) {
        return (X) data.get(tag);
    }

    public NObject put(String tag) {
        return put(tag, 1.0f);
    }

    public NObject put(String tag, Object value) {
        switch (tag) {
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
        data.put(tag, value);
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

    public String description() {
        Object d = get("_");
        return d == null ? "" : d.toString();
    }


    @Override
    public String toString() {
        return new String(JSON.toJSON(this));
    }




    public NObject name(String name) {
        this.name = name;
        return this;
    }

    //public final static String POINT = ".";
    public final static String LINESTRING = "-";
    public final static String POLYGON = "*";


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
        put("g" + NObject.LINESTRING, points);
        return this;
    }

    public NObject where(Polygon p) {
        double[][] outerRing = KML.toArray(p.getOuterRing().getPoints());

        //TODO handle inner rings

        where(p.getBoundingBox());
        put("g" + NObject.POLYGON, outerRing);
        return this;
    }

    /** sets the inside property */
    public NObject setTag(String... tags) {

        for (String t : tags) {
            if (t.equals(getId()))
                throw new RuntimeException("object can not be inside itself");
        }

        if (tags.length > 1) {
            //TODO remove any duplicates
        }

        this.tag = tags;


        return this;
    }



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
