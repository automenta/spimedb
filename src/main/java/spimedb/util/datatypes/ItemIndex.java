package spimedb.util.datatypes;

import java.util.List;

public interface ItemIndex<T> {

    void clear();

    T forID(int id);

    int getID(T item);

    List<T> getItems();

    int index(T item);

    boolean isIndexed(T item);

    int reindex(T item, T newItem);

    int size();

    int unindex(T item);

}