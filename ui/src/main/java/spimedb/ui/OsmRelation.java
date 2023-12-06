package spimedb.ui;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by unkei on 2017/04/26.
 */
public class OsmRelation extends OsmGroup {

    public OsmRelation(long id, List<OsmElement> children, Map<String, String> tags) {
        super(id, tags);
        this.children = children;
    }

    public void addChildren(List<OsmElement> c) {
        if (this.children == null)
            this.children = c;
        else
            this.children.addAll(c);
    }

    @Override
    public void forEach(Consumer<OsmElement> eachChild) {
        if (children!=null) {
            for (OsmElement child : children) {
                eachChild.accept(child);
            }
        }
    }


}
