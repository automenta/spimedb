package spimedb;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import spimedb.index.rtree.PointND;

import java.util.function.BiConsumer;

@JsonSerialize(using = NObject.NObjectSerializer.class)
public class ProxyNObject implements NObject {
    /**
     * current object
     */
    protected NObject n;

    public void set(NObject n) {
        this.n = n;
    }

    @Override
    public String id() {
        return n.id();
    }

    @Override
    public String name() {
        return n.name();
    }

    @Override
    public String[] tags() {
        return n.tags();
    }

    @Override
    public void forEach(BiConsumer<String, Object> each) {
        n.forEach(each);
    }

    @Override
    public <X> X get(String tag) {
        return n.get(tag);
    }

    @Override
    public PointND min() {
        return n.min();
    }

    @Override
    public PointND max() {
        return n.max();
    }

    @Override
    public String toString() {
        return stringify();
    }
}
