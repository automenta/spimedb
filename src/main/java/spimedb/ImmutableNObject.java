package spimedb;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ArrayUtils;
import spimedb.index.rtree.PointND;
import spimedb.index.rtree.RectND;
import spimedb.util.JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@JsonSerialize(using= NObject.NObjectSerializer.class)
public abstract class ImmutableNObject extends RectND implements NObject {

    //public final static String POINT = ".";
    public final static String LINESTRING = "-";
    public final static String POLYGON = "*";

    protected final String id;

    /** extensional inheritance: what this nobject is "inside" of (its container)
     *  by default, use the root node. but try not to pollute it
     */
    protected String[] tag = ArrayUtils.EMPTY_STRING_ARRAY;

    protected String name;
    protected final Map<String, Object> data = new HashMap();


    public ImmutableNObject(PointND a, PointND b, String id, String name) {
        super(a, b);
        this.id = id!=null ? id : JSON.uuid64();
        this.name = name;
    }

    @Override
    public void forEach(BiConsumer<String, Object> each) {

        String name = name();
        if (name != null)
            each.accept(NAME, name);

        String[] tag = tags();
        if (tag != null && tag.length > 0) {
            each.accept(TAG, tag);
        }

        if (data!=null)
            data.forEach(each);
    }

    @Override
    public String[] tags() {
        return tag;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        MutableNObject no =(MutableNObject)o;
        return id.equals(no.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public <X> X get(String tag) {
        return (X) data.get(tag);
    }

    @Override
    public String toString() {
        return toJSONString();
    }

    @Override
    public final String id() {
        return id;
    }


}
