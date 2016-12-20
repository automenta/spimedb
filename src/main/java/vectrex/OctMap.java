package vectrex;


import toxi.geom.Vec3D;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** TODO extract infinispan to subclass allowing any Map impl for index */
public class OctMap<K,V extends IdBB<K>> implements Map<K,V> {

    private static final Logger logger = Logger.getLogger(OctMap.class.getSimpleName()); // + ":" + id);

    /** holder for _oct for infinispan persistence */
    protected final Map<Long, OctBox<K>> _oct;

    protected final Map<K, V> map;
    protected final OctBox<K> box;

    boolean startupCheck = true;

    public OctMap(Vec3D center, Vec3D radius, Vec3D resolution) {

        this.map = new HashMap(); //p.the(id);
        this._oct = new HashMap(); //p.the(id + ".oct");

        if (_oct.isEmpty()) {
            OctBox newBox = this.box = new OctBox(center, radius, resolution);
            _oct.put(0L, newBox);
            logger.info("new octree created: " + box);
        }
        else {
            this.box = _oct.get(0L);
            if (this.box == null) {
                throw new RuntimeException("Unable to load persisted OctBox:" + this._oct);
            }
            logger.info("existing octbox loaded: " + box);


            if (startupCheck) {
                if (!validate()) {
                    reindex();
                    flush();
                }
            }
        }
    }


    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    final public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    final public V get(Object key) {
        return map.get(key);
    }


    final public V put(V value) {
        return put(value.id(), value);
    }

    @Override
    public V put(K key, V value) {
        V removed = map.put(key, value);
        if (removed!=null) {
            box.remove(removed);
        }
        if (box.put(value)==null) {
            throw new RuntimeException("Octree rejected value=" + value + ", key=" + key );
        }
        return removed;
    }


    @Override
    public V remove(Object key) {
        V removed = map.remove(key);
        if (removed!=null) {
            if (!box.remove(removed)) {
                throw new RuntimeException("Octree missing value for key=" + key + "=" + removed);
            }
        }
        return removed;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
        box.putAll(m.values());
    }

    @Override
    public void clear() {
        map.clear();
        box.zero();
        box.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    public void reindex() {
        logger.info("re-indexing " + map.size() + " items");
        box.zero();
        box.putAll(map.values());

        validate();
    }

    /** manually flush the octree to persistence */
    public boolean flush() {
        _oct.put(0L, box);
        return validate();
    }

    public boolean validate() {
        int e = box.itemCountRecursively();
        int msize = map.size();
        boolean consistent = (e == msize);
        logger.info("octbox contains " + e + " entries. consistent with map=" + msize + " is " + consistent);
        return consistent;
    }

    public OctBox box() {
        return box;
    }
}
