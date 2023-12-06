package spimedb.ui;

import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.rect.HyperRectFloat;

import java.util.List;
import java.util.Map;

public abstract class OsmGroup extends OsmElement {
    //    public boolean isMultipolygon;

    protected List<OsmElement> children;

    private transient HyperRegion bounds;

    public OsmGroup(long id, Map<String, String> tags) {
        super(id, tags);
    }

    public void invalidate() {
        bounds = null;
    }

    protected void validate() {
        int n;
		n = children == null ? 0 : children.size();

        HyperRegion bounds;
        switch (n) {
            case 0 -> bounds = HyperRectFloat.unbounded3;
            case 1 -> bounds = children.get(0);
            default -> {
                bounds = children.get(0);
                for (int i = 1; i < n; i++) {
                    bounds = bounds.mbr(children.get(i));
                }
            }
        }
        this.bounds = bounds;
    }

    @Override
    public HyperRegion mbr(HyperRegion r) {
        return bounds().mbr(r);
    }

    @Override
    public double coord(int dimension, boolean maxOrMin) {
        return bounds().coord(dimension, maxOrMin);
    }

    public HyperRegion bounds() {
        HyperRegion b = bounds;
        if (b == null) {
            validate();
            b = bounds;
        }
        return b;
    }

}