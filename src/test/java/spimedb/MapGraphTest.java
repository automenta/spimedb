package spimedb;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import spimedb.index.graph.MapGraph;
import spimedb.index.graph.travel.BreadthFirstTravel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import static org.junit.Assert.*;

/**
 * adapted from: https://github.com/jgrapht/jgrapht/blob/master/jgrapht-core/src/test/java/org/jgrapht/graph/DefaultDirectedGraphTest.java
 */
public class MapGraphTest {

    // ~ Instance fields --------------------------------------------------------

    private String v1 = "v1";
    private String v2 = "v2";
    private String v3 = "v3";

    // ~ Methods ----------------------------------------------------------------

    /**
     * .
     */
    @Test
    public void testEdgeSetFactory()
    {
        MapGraph<String, String> g = newTestGraph();

        g.addVertex(v1);
        assertTrue(g.containsVertex(v1));
        assertFalse(g.containsVertex(v2));
        g.addVertex(v2);
        g.addVertex(v3);

        assertTrue(g.addEdge(v1, v2, "x"));
        {
            assertFalse(g.addEdge(v1, v2, "x")); //dup
            assertTrue(g.containsEdge(v1, v2, "x"));
            assertFalse(g.containsEdge(v1, v2, "xy"));
            assertFalse(g.containsEdge(v2, v1, "x"));
        }
        assertTrue(g.addEdge(v2, v1, "x"));
        assertTrue(g.addEdge(v2, v3, "x"));
        assertTrue(g.addEdge(v3, v1, "x"));

        assertEquals(2, g.inDegreeOf(v1));
        assertEquals(1, g.outDegreeOf(v1));

        assertEquals(ImmutableSet.of("x"), g.getAllEdges(v1, v2));

        assertEquals(
                "{v1={[v2:x, v3:x] | [x:v2]}, v2={[v1:x] | [x:v1, x:v3]}, v3={[v2:x] | [x:v1]}}",
                g.toString());

        //test multigraph ability by creating a different kind of edge between v1 and v2
        assertTrue(g.addEdge(v1, v2, "y"));

        assertEquals(2, g.outDegreeOf(v1)); //increased
        assertEquals(ImmutableSet.of("x", "y"), g.getAllEdges(v1, v2));


    }

    @NotNull
    private MapGraph<String, String> newTestGraph() {
        return new MapGraph<>(new LinkedHashMap<>(), ()->new LinkedHashSet<>());
    }

    @Test public void testBreadthFirst() {
        MapGraph<String, String> g = newTestGraph();
        g.addVertex(v1);
        g.addVertex(v2);
        g.addVertex(v3);
        assertTrue(g.addEdge(v1, v2, "x"));
        assertTrue(g.addEdge(v1, v2, "y"));
        assertTrue(g.addEdge(v2, v1, "x"));
        assertTrue(g.addEdge(v2, v3, "x"));
        assertTrue(g.addEdge(v3, v1, "x"));

        ArrayList<String> all = Lists.newArrayList(new BreadthFirstTravel<>(g));
        assertEquals(3, all.size());


        ArrayList<String> from1 = Lists.newArrayList(new BreadthFirstTravel<>(g, v1));
        assertEquals("[v1, v2, v3]", from1.toString());
        ArrayList<String> from2 = Lists.newArrayList(new BreadthFirstTravel<>(g, v2));
        assertEquals("[v2, v1, v3]", from2.toString());
        ArrayList<String> from3 = Lists.newArrayList(new BreadthFirstTravel<>(g, v3));
        assertEquals("[v3, v1, v2]", from3.toString());


    }

//    /**
//     * .
//     */
//    public void testEdgeOrderDeterminism()
//    {
//        DirectedGraph<String, DefaultEdge> g = new DirectedMultigraph<>(DefaultEdge.class);
//        g.addVertex(v1);
//        g.addVertex(v2);
//        g.addVertex(v3);
//
//        DefaultEdge e1 = g.addEdge(v1, v2);
//        DefaultEdge e2 = g.addEdge(v2, v3);
//        DefaultEdge e3 = g.addEdge(v3, v1);
//
//        Iterator<DefaultEdge> iter = g.edgeSet().iterator();
//        assertEquals(e1, iter.next());
//        assertEquals(e2, iter.next());
//        assertEquals(e3, iter.next());
//
//        // some bonus tests
//        assertTrue(Graphs.testIncidence(g, e1, v1));
//        assertTrue(Graphs.testIncidence(g, e1, v2));
//        assertFalse(Graphs.testIncidence(g, e1, v3));
//        assertEquals(v2, Graphs.getOppositeVertex(g, e1, v1));
//        assertEquals(v1, Graphs.getOppositeVertex(g, e1, v2));
//
//        assertEquals("([v1, v2, v3], [(v1,v2), (v2,v3), (v3,v1)])", g.toString());
//    }
//
//    /**
//     * .
//     */
//    public void testEdgesOf()
//    {
//        DirectedGraph<String, DefaultEdge> g = createMultiTriangle();
//
//        assertEquals(3, g.edgesOf(v1).size());
//        assertEquals(3, g.edgesOf(v2).size());
//        assertEquals(2, g.edgesOf(v3).size());
//    }
//
//    /**
//     * .
//     */
//    public void testInDegreeOf()
//    {
//        DirectedGraph<String, DefaultEdge> g = createMultiTriangle();
//
//        assertEquals(2, g.inDegreeOf(v1));
//        assertEquals(1, g.inDegreeOf(v2));
//        assertEquals(1, g.inDegreeOf(v3));
//    }
//
//    /**
//     * .
//     */
//    public void testOutDegreeOf()
//    {
//        DirectedGraph<String, DefaultEdge> g = createMultiTriangle();
//
//        assertEquals(1, g.outDegreeOf(v1));
//        assertEquals(2, g.outDegreeOf(v2));
//        assertEquals(1, g.outDegreeOf(v3));
//    }
//
//    /**
//     * .
//     */
//    public void testVertexOrderDeterminism()
//    {
//        DirectedGraph<String, DefaultEdge> g = createMultiTriangle();
//        Iterator<String> iter = g.vertexSet().iterator();
//        assertEquals(v1, iter.next());
//        assertEquals(v2, iter.next());
//        assertEquals(v3, iter.next());
//    }
//
//    private DirectedGraph<String, DefaultEdge> createMultiTriangle()
//    {
//        DirectedGraph<String, DefaultEdge> g = new DirectedMultigraph<>(DefaultEdge.class);
//        initMultiTriangle(g);
//
//        return g;
//    }
//
//    private void initMultiTriangle(DirectedGraph<String, DefaultEdge> g)
//    {
//        g.addVertex(v1);
//        g.addVertex(v2);
//        g.addVertex(v3);
//
//        g.addEdge(v1, v2);
//        g.addEdge(v2, v1);
//        g.addEdge(v2, v3);
//        g.addEdge(v3, v1);
//    }
}
