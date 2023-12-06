package spimedb.ui;

import jcog.data.map.UnifriedMap;
import jcog.tree.rtree.HyperRegion;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by unkei on 2017/04/26.
 */
public abstract class OsmElement implements HyperRegion {

    public final long id;

    //CompactStringObjectMap ?
    public Map<String, String> tags;

    OsmElement(long id, Map<String, String> tags) {
        this.id = id;

        if (tags == null || tags.isEmpty())
            tags = null; //Collections.emptyMap();
        this.tags = tags;
    }

    @Override
    public boolean equals(Object obj) {
        return this==obj || (obj instanceof OsmElement && ((OsmElement)obj).id == id);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    public void forEach(Consumer<OsmElement> eachChild) {
        //leaf
    }

    public void tag(String k, String v) {
        if (tags == null || tags.isEmpty()) {
            tags = new UnifriedMap(1);
        }
        tags.put(k, v);
    }


    @Override
    public final int dim() {
        return 3;
    }

}
