package spimedb.ui;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by unkei on 2017/04/25.
 */
public class OsmWay extends OsmGroup {

    public OsmWay(long id, List<OsmElement> children, Map<String, String> tags) {
        super(id, tags);
        this.children = children;
    }

    @Override
    public void forEach(Consumer<OsmElement> eachChild) {
        if (children!=null) children.forEach(eachChild);
    }

    public List<OsmElement> getOsmNodes() {
        return this.children;
    }

    public void addOsmWay(OsmWay way) {

        if (way == null || way.children == null) return;

        List<OsmElement> newChildren = way.children;
        if (this.children == null) {
            this.children = newChildren;
        } else {
//            List<OsmElement> combinedNodes = new FasterList<>(this.children);
//            combinedNodes.addAll(newChildren);
//            this.children = combinedNodes;
            this.children.addAll(newChildren);
        }
    }

    public boolean isFollowedBy(OsmWay way) {
        int wc = way.children != null ? way.children.size() : 0;
        if (wc > 0) {
            if (this.children != null) {
                int tc = this.children.size(); assert (tc > 0); /*if (!this.children.isEmpty())*/
                return way.children.get(0).id == this.children.get(tc - 1).id;
            }
        }
        return false;
    }

    public boolean isLoop() {
        int s;
        List<? extends OsmElement> c = this.children;
        if (c != null && (s = c.size()) > 3) {
            OsmElement first = c.get(0);
            if (first != null) {
                OsmElement last = c.get(s - 1);
                if (last != null)
                    return first.id == last.id;
            }
        }
        return false;
    }


}
