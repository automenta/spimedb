package vectrex;


import org.infinispan.Cache;
import spangraph.InfiniPeer;
import toxi.geom.Vec3D;
import toxi.geom.XYZ;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** TODO extract infinispan to subclass allowing any Map impl for index */
public class OctMap<K extends XYZ, V> implements Map<K,V> {

    private final Logger logger;

    /** holder for _oct for infinispan persistence */
    protected final Cache<Long, OctBox<K>> _oct;

    protected final Cache<K, V> map;
    protected final OctBox<K> box;

    boolean startupCheck = true;

    public OctMap(InfiniPeer p, String id, Vec3D center, Vec3D radius, Vec3D resolution) {

        this.logger = Logger.getLogger(OctMap.class.getSimpleName() + ":" + id);

        this.map = p.the(id);
        this._oct = p.the(id + ".oct");

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
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        V removed = map.put(key, value);
        if (removed!=null) {
            octRemove(key, removed);
        }
        if (box.ADD(key)==null) {
            throw new RuntimeException("Octree rejected value=" + value + ", key=" + key );
        }
        return removed;
    }

    private void octRemove(K key, V v) {
        if (!box.remove(key)) {
            throw new RuntimeException("Octree inconsistency detected on removal key=" + key + ", value=" + v);
        }
    }

    @Override
    public V remove(Object key) {
        V v = map.remove(key);
        if (v!=null) {
            octRemove((K)key, v);
        }
        return v;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
        box.putAll(m.keySet());
    }

    @Override
    public void clear() {
        map.clear();
        box.zero();
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
        box.putAll(map.keySet());

        validate();
    }

    /** manually flush the octree to persistence */
    public boolean flush() {
        _oct.put(0L, box);
        return validate();
    }

    public boolean validate() {
        int e = box.countPointsRecursively();
        int msize = map.size();
        boolean consistent = (e == msize);
        logger.info("octbox contains " + e + " entries. consistent with map=" + msize + " is " + consistent);
        return consistent;
    }

    public OctBox box() {
        return box;
    }
}
