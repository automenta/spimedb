package automenta.netention.net;

import automenta.netention.Core;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.codehaus.jackson.annotate.JsonAutoDetect;
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
@JsonSerialize
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.NON_PRIVATE)
@JsonInclude(value= JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
public class NObject implements Serializable {

    @JsonProperty("I") String id;

    @JsonProperty("N") String name;

    @JsonProperty("T")
    TimePoint time = null;

    @JsonProperty("S") SpacePoint space = null;

    //TODO use a ObjectDouble primitive map structure
    @JsonProperty("^") Map<String, Double> inh = null;

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

    public Map<String, Double> getInh() {
        return inh;
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
        return inh.keySet();
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
        if (inh == null) inh = new HashMap();
        inh.put(tag, strength);
        return this;
    }


    public void description(String d) {
        this.description = d;
    }
}
