package automenta.netention.net;

import automenta.netention.Core;
import org.codehaus.jackson.annotate.JsonProperty;

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
public class NObject implements Serializable {

    @JsonProperty("I") String id;

    @JsonProperty("N") String name;

    @JsonProperty("T") TimePoint time = null;

    @JsonProperty("S") SpacePoint space = null;

    Map<String, Double> tags = new HashMap(); //TODO use a ObjectDouble primitive map structure

    public NObject() {
        this(Core.uuid());
    }

    public NObject(String id) {
        this(id, null);

    }

    public NObject(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Double> getTags() {
        return tags;
    }

    /**
     * timepoint, or -1 if none
     */
    public TimePoint getTime() {
        return time;
    }

    public SpacePoint getSpace() {
        return space;
    }

    public Collection<String> tagSet() {
        return tags.keySet();
    }

    public NObject when(long when) {
        this.time = new TimePoint(when);
        return this;
    }

    public NObject where(double lat, double lng) {
        return where(new SpacePoint(lat, lng));
    }

    public NObject where(SpacePoint s) {
        this.space = s;
        return this;
    }

    public NObject tag(String tag) {
        return tag(tag, 1.0);
    }

    public NObject tag(String tag, double strength) {
        tags.put(tag, strength);
        return this;
    }

}
