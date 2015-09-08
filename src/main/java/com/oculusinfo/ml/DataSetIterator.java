package com.oculusinfo.ml;

import java.util.Iterator;
import java.util.Map;


final public class DataSetIterator implements Iterator<Instance> {
    final Iterator<Map.Entry<String, Instance>> iterator;

	public DataSetIterator(Map<String, Instance> map) {
        this.iterator = map.entrySet().iterator();
    }

	@Override
    public final boolean hasNext() {
        return iterator.hasNext();
    }

	@Override
    public final Instance next() {
        return iterator.next().getValue();
    }

	@Override
    public final void remove() {
        iterator.remove();
    }
}
