package automenta.netention.net;

import automenta.netention.Core;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hibernate.search.annotations.*;
import org.hibernate.search.spatial.Coordinates;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
@Spatial(name = "nobject", spatialMode = SpatialMode.RANGE)
public class NObject implements Serializable, Coordinates {

    final static long ETERNAL = Long.MIN_VALUE;

    @Field(store = Store.YES) @JsonProperty("I") final String id;

    @Field(store = Store.YES) @JsonProperty("N") final String name;

    @JsonProperty("T") long[] time = null;

    /** lat, lon, alt (m), [planet ID?] */
    @JsonProperty("S") float[] space = null;


    //TODO use a ObjectDouble primitive map structure
    @JsonProperty("^") Map<String, Object> fields = null;

    @JsonProperty("_") String description = null;


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

    public NObject when(final long when) {
        if (this.time == null) this.time = new long[2];
        this.time[0] = this.time[1] = when;
        return this;
    }

    public NObject where(float[] coord) {
        return where(coord[0], coord[1], coord[2]);
    }

    public NObject where(float lat, float lng, float alt) {

        if (space == null) space = new float[3];
        space[0] = lat;
        space[1] = lng;

        return this;
    }

    public NObject where(NObject otherLocation) {
        return where(otherLocation.space);
    }

    public NObject where(double lat, double lon) {
        return where((float)lat, (float)lon, Float.NaN);
    }

    public NObject where(float lat, float lng) {
        return where(lat, lng, Float.NaN);
    }

    //TODO provide non-boxed versoins of these
    @JsonIgnore
    public Double getLatitude() {
        if (space!=null)
            return Double.valueOf(space[0]);
        return Double.NaN;
    }

    @JsonIgnore
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

    public NObject put(String tag) {
        return put(tag, 1.0f);
    }

    public NObject put(String tag, Object value) {
        if (fields == null) fields = new HashMap();
        fields.put(tag, value);
        return this;
    }




    public void description(String d) {
        this.description = d;
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


    public NObject now() {
        when( System.currentTimeMillis() );
        return this;
    }
}
