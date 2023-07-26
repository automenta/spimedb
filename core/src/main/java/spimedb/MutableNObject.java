package spimedb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jcog.tree.rtree.point.DoubleND;
import org.eclipse.collections.impl.factory.Iterables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spimedb.util.JSON;

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


    public MutableNObject() {
        this(SpimeDB.uuidString());
    }

    public MutableNObject(String id) {
        this(id, null);
    }

    public MutableNObject(NObject copy) {
        super(copy.min(), copy.max(), copy.id(), copy.name());
        copy.forEach(this::put);
    }

    public MutableNObject(String id, String name) {
        super(DoubleND.fill(4, Float.NEGATIVE_INFINITY), DoubleND.fill(4, Float.POSITIVE_INFINITY), id, name);
    }

    @Override
    public DoubleND max() {
        return max;
    }

    @Override
    public DoubleND min() {
        return min;
    }

    public MutableNObject putLater(String key, float pri, Supplier lazy) {
        return putLater(key, pri, null, lazy);
    }

    public MutableNObject putLater(String key, float pri, Object intermediate, Supplier lazy) {
        return put(key, new LazyValue(key, intermediate, pri, lazy));
    }

    public MutableNObject put(String key, Object value) {

        switch (key) {
            case TAG -> {
                String[] tt;
                if (value instanceof String[])
                    tt = ((String[]) value).clone();
                else if (value instanceof String)
                    tt = ((String) value).trim().split(" "); //new String[] { (String) value };
                else if (value instanceof ArrayNode) {
                    tt = JSON.toStrings((ArrayNode) value);
                } else
                    throw new RuntimeException("invalid tag property");
                withTags(tt);
                return this;
            }

//            case "<":
//                //HACK
//                setOutside(Sets.newHashSet((String[])value));
//                return this;

            case ID -> {
                if ((value instanceof String) && (value.equals(id))) {
                    //already being set to same ID
                    return this;
                }
                throw new RuntimeException(this + " can not change ID");
            }
            case NAME -> {
                name(value.toString().trim());
                return this;
            }
        }

        //synchronized (data) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        //}
        return this;
    }

    public MutableNObject description(String d) {
        if (d!=null)
            d = d.trim();

        if (d == null || d.isEmpty()) {
            if (data != null)
                data.remove(DESC);

        } else {
            put(DESC, d);
        }
        return this;
    }


    public MutableNObject name(String name) {
        this.name = name;
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
            this.tag = null;
        } else {
            this.tag = s.toArray(new String[0]);
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
            else if (v.isTextual())
                s = v.textValue();
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
