package spimedb.cluster;

import java.util.Iterator;
import java.util.Map;


final public class DataSetIterator implements Iterator<Instance> {
    final Iterator<Map.Entry<String, Instance>> iterator;

	public DataSetIterator(Map<String, Instance> map) {
        this.iterator = map.entrySet().iterator();
    }

	@Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

	@Override
    public Instance next() {
        return iterator.next().getValue();
    }

	@Override
    public void remove() {
        iterator.remove();
    }
}
