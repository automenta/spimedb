package spimedb.sense;

import org.junit.Test;
import spimedb.db.SpimeDB;

import static org.junit.Assert.assertTrue;

/**
 * tests schema.org import and the type hierarchy creation and inference
 * ie. equivalent of a mini-RDFS reasoner
 */
public class ImportSchemaOrgTest {

    @Test
    public void test1() {
        SpimeDB r = new SpimeDB();
        ImportSchemaOrg.load(r);
//        r.graph.vertexSet().forEach(v -> {
//            System.out.println(v);
//            System.out.println("\t" + r.graph.edgesOf(v));
//        });

        assertTrue(r.obj.size() > 500);


//        System.out.println(r.graph.vertexSet().size() + " " + r.graph.edgeSet().size());
//        assertTrue(r.graph.edgeSet().size() > 1000);


        //AllDirectedPaths a = new AllDirectedPaths(r.graph);
//        AStarShortestPath a = new AStarShortestPath(r.graph);
//        GraphPath p1 = a.getShortestPath("Casino", "Place", (x, y) -> 1);
//        System.out.println(p1);

//        Object copy = r.graph.clone()
//        trans = TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(copy);
//        System.out.println(trans.getClass());
//        System.out.println(trans.getClass());
    }
}