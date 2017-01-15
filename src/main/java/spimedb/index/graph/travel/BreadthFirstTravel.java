package spimedb.index.graph.travel;

import spimedb.index.graph.MapGraph;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A breadth-first iterator for a directed and an undirected graph. For this iterator to work
 * correctly the graph must not be modified during iteration. Currently there are no means to ensure
 * that, nor to fail-fast. The results of such modifications are undefined.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * @author Barak Naveh
 * @since Jul 19, 2003
 */
public class BreadthFirstTravel<V, E> extends CrossComponentTravel<V, E, Object> {

    private final Deque<V> queue = new ArrayDeque<>();

    /**
     * Creates a new breadth-first iterator for the specified graph.
     *
     * @param g the graph to be iterated.
     */
    public BreadthFirstTravel(MapGraph<V, E> g) {

        this(g, null);
    }

    public BreadthFirstTravel(MapGraph<V,E> g, V start) {
        this(g, start, new ConcurrentHashMap<>());
    }

    /**
     * Creates a new breadth-first iterator for the specified graph. Iteration will start at the
     * specified start vertex and will be limited to the connected component that includes that
     * vertex. If the specified start vertex is <code>null</code>, iteration will start at an
     * arbitrary vertex and will not be limited, that is, will be able to traverse all the graph.
     *  @param g           the graph to be iterated.
     * @param startVertex the vertex iteration to be started.
     * @param s
     */
    public BreadthFirstTravel(MapGraph<V, E> g, V startVertex, Map<V, Object> s) {
        super(g, startVertex, s);
    }

    /**
     * @see CrossComponentTravel#isConnectedComponentExhausted()
     */
    @Override
    protected boolean isConnectedComponentExhausted() {
        return queue.isEmpty();
    }

    /**
     * @see CrossComponentTravel#encounterVertex(Object, Object)
     */
    @Override
    protected void encounterVertex(V vertex, E edge) {
        putSeenData(vertex, this);
        queue.add(vertex);
    }

    /**
     * @see CrossComponentTravel#encounterVertexAgain(Object, Object)
     */
    @Override
    protected void encounterVertexAgain(V vertex, E edge) {
    }

    /**
     * @see CrossComponentTravel#provideNextVertex()
     */
    @Override
    protected V provideNextVertex() {
        return queue.removeFirst();
    }

    @Override
    public boolean wantsEdges() {
        return false;
    }
}