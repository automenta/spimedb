package spimedb.ui;

import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.rect.HyperRectFloat;

import java.util.Map;

/**
 * Created by unkei on 2017/04/25.
 */
public class OsmNode extends OsmElement {
    public final GeoVec3 pos;

    public OsmNode(long id, GeoVec3 pos, Map<String, String> tags) {
        super(id, tags);
        this.pos = pos;
    }

    @Override
    public String toString() {
        return id + "@" + pos + (tags!=null ? tags.toString() : "");
    }

    @Override
    public HyperRegion mbr(HyperRegion r) {
        return r.mbr(new HyperRectFloat(pos));
    }

    @Override
    public double coord(int dimension, boolean maxOrMin) {
        return switch (dimension) {
            case 0 -> pos.x;
            case 1 -> pos.y;
            case 2 -> pos.z;
            default -> Double.NaN;
        };
    }
}
