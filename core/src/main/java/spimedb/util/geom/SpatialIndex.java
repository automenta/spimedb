package spimedb.util.geom;

import java.util.List;

public interface SpatialIndex<T> {

    void clear();

    boolean index(T p);

    boolean isIndexed(T item);

    List<T> itemsWithinRadius(T p, float radius, List<T> results);

    boolean reindex(T p, T q);

    int size();

    boolean unindex(T p);

}