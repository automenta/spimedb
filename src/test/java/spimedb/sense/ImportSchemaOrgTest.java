package spimedb.sense;

import org.junit.Test;
import spimedb.SpimeDB;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * tests schema.org import and the type hierarchy creation and inference
 * ie. equivalent of a mini-RDFS reasoner
 */
public class ImportSchemaOrgTest {

    @Test
    public void test1() {
        SpimeDB db = new SpimeDB();
        ImportSchemaOrg.load(db);
//        r.graph.vertexSet().forEach(v -> {
//            System.out.println(v);
//            System.out.println("\t" + r.graph.edgesOf(v));
//        });

        assertTrue(db.obj.size() > 500);

        assertEquals(1, db.tags.graph.outDegreeOf("Action"));
        assertTrue(db.tags.graph.inDegreeOf("Action") > 1);

        //System.out.println(db.tags.graph.vertex("Action", false).outVset());
        //System.out.println(db.get("replace"));

        assertEquals(1, db.tags.graph.outDegreeOf("Place"));
        assertTrue(db.tags.graph.inDegreeOf("Place") > 1);

        //TODO: assertTrue(r.tags.isConnected)

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