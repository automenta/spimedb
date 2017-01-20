package spimedb.input;

import com.google.common.base.Joiner;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * PBF datasets: https://mapzen.com/data/metro-extracts/
 */
public class OSM implements Sink, EntityProcessor {


    public OSM(String pbfFile) {
        PbfReader r = new PbfReader(new File(pbfFile), 1);
        r.setSink(this);
        r.run();
    }

    @Override
    public void release() {

    }

    @Override
    public void complete() {

    }

    @Override
    public void initialize(Map<String, Object> map) {
        //System.out.println(map);
    }

    @Override
    public void process(EntityContainer entityContainer) {
        //System.out.println(entityContainer);
        entityContainer.process(this);
    }

    public void process(BoundContainer bound) {

    }

    public void process(NodeContainer node) {
        //this.processorUser.incrementNodeCount();
        //EntityReporter.this.totalUser.incrementNodeCount();

        Node n = node.getEntity();
        Collection<Tag> nt = n.getTags();
        if (!nt.isEmpty()) {
            System.out.println(n.getLongitude() + " " + n.getLatitude() + " " + Joiner.on(" ").join(nt));
        }
    }

    public void process(WayContainer way) {
        //System.out.println(way.getEntity());
    }

    public void process(RelationContainer relation) {
        //System.out.println(relation.getEntity());

        /*
        Relation r = relation.getEntity();
        Collection<Tag> rt = r.getTags();
        //if (!rt.isEmpty())
            System.out.println( r.getMembers() + " " +  Joiner.on(" ").join(rt) );
        */
    }

    public static void main(String[] args) {

        new OSM("/home/me/d/p1.osm.pbf");

    }

}
