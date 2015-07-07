package vectrex;

import toxi.geom.Vec3D;
import toxi.geom.XYZ;

import java.util.*;


public class OctSet<V extends XYZ> extends OctBox<V> implements Set<V> {


    final Map<V, OctBox<V>> data = new HashMap();

    public OctSet(Vec3D o, Vec3D extents, Vec3D resolution) {
        super(o, extents, resolution);
    }

    @Override
    public OctBox<V> ADD(V p) {
        OctBox<V> target = super.ADD(p);
        if (target!=null) {
            data.put(p, target);
            return target;
        }
        return null;
    }

    @Override
    public boolean remove(Object p) {
        //TODO use the value in data for fast access
        if (data.remove(p)!=null) {
            super.remove(p);
            return true;
        }
        return false;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return data.containsKey(o);
    }

    @Override
    public Iterator<V> iterator() {
        return data.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return data.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(V v) {
        if (ADD(v)!=null) {
            return true;
        }
        return false;
    }



    @Override
    public boolean containsAll(Collection<?> c) {
        return data.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        return putAll(c) == c.size();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        //SOON
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> points) {
        //SOON
        throw new UnsupportedOperationException();
    }
}
