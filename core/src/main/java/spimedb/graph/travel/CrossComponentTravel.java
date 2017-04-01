package spimedb.graph.travel;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spimedb.graph.MapGraph;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * Provides a cross-connected-component traversal functionality for iterator subclasses.
 *
 * @param <V> vertex type
 * @param <E> edge type
 * @param <D> type of data associated to seen vertices
 * @author Barak Naveh
 * @since Jan 31, 2004
 */
public abstract class CrossComponentTravel<V, E, D> extends AbstractTravel<V, E> {
    private static final int CCS_BEFORE_COMPONENT = 1;
    private static final int CCS_WITHIN_COMPONENT = 2;
    private static final int CCS_AFTER_COMPONENT = 3;

    /**
     * true = in, false = out
     */
    private final boolean inOrOut = true;

    public CrossComponentTravel(MapGraph<V, E> g, Map<V, D> seen) {
        this(g, null, seen);
    }

    /**
     * Standard vertex visit state enumeration.
     */
    protected enum VisitColor {
        /**
         * Vertex has not been returned via iterator yet.
         */
        WHITE,

        /**
         * Vertex has been returned via iterator, but we're not done with all of its out-edges yet.
         */
        GRAY,

        /**
         * Vertex has been returned via iterator, and we're done with all of its out-edges.
         */
        BLACK
    }


    /**
     * Connected component traversal started event.
     */
    public static final int CONNECTED_COMPONENT_ENTER = 31;

    private final ObjectIntPair ccEnterEvent = pair(this, CONNECTED_COMPONENT_ENTER);

    /**
     * Connected component traversal finished event.
     */
    public static final int CONNECTED_COMPONENT_EXIT = 32;

    private final ObjectIntPair ccExitEvent = pair(this, CONNECTED_COMPONENT_EXIT);

    private final Iterator<V> vertexIterator;

    /**
     * Stores the vertices that have been seen during iteration and (optionally) some additional
     * traversal info regarding each vertex.
     */
    public final Map<V, D> seen;

    private V startVertex;

    public final MapGraph<V, E> graph;

    /**
     * The connected component state
     */
    private int state = CCS_BEFORE_COMPONENT;

    /**
     * Creates a new iterator for the specified graph. Iteration will start at the specified start
     * vertex. If the specified start vertex is <code>
     * null</code>, Iteration will start at an arbitrary graph vertex.
     *
     * @param g           the graph to be iterated.
     * @param startVertex the vertex iteration to be started.
     * @param seen
     * @throws IllegalArgumentException if <code>g==null</code> or does not contain
     *                                  <code>startVertex</code>
     */
    public CrossComponentTravel(MapGraph<V, E> g, @Nullable V startVertex, @NotNull Map<V, D> seen) {

        super();
        this.seen = seen;
        this.graph = g;

//        specifics = createGraphSpecifics(g);
        vertexIterator = g.vertexSet().iterator();
        setCrossComponentTraversal(startVertex == null);

//        reusableEdgeEvent = new FlyweightEdgeEvent<>(this, null);
//        reusableVertexEvent = new FlyweightVertexEvent<>(this, null);

        if (startVertex == null) {
            // pick a start vertex if graph not empty
            if (vertexIterator.hasNext()) {
                this.startVertex = vertexIterator.next();
            } else {
                this.startVertex = null;
            }
        } else if (g.containsVertex(startVertex)) {
            this.startVertex = startVertex;
        } else {
            throw new IllegalArgumentException("graph must contain the start vertex");
        }
    }


    /**
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        if (startVertex != null) {
            encounterStartVertex();
        }

        if (isConnectedComponentExhausted()) {
            if (state == CCS_WITHIN_COMPONENT) {
                state = CCS_AFTER_COMPONENT;
                fireConnectedComponentFinished(ccExitEvent);
            }

            if (isCrossComponentTraversal()) {
                while (vertexIterator.hasNext()) {
                    V v = vertexIterator.next();

                    if (!isSeenVertex(v)) {
                        encounterVertex(v, null);
                        state = CCS_BEFORE_COMPONENT;

                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * @see java.util.Iterator#next()
     */
    @Override
    public V next() {
        if (startVertex != null) {
            encounterStartVertex();
        }

        if (hasNext()) {
            if (state == CCS_BEFORE_COMPONENT) {
                state = CCS_WITHIN_COMPONENT;
                fireConnectedComponentStarted(ccEnterEvent);
            }


            V nextVertex = provideNextVertex();

            //Pair<V, E> incoming = Tuples.pair(currentVertex, );
            fireVertexEnter(null, nextVertex);

            addIncidentEdges(nextVertex);

            return nextVertex;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns <tt>true</tt> if there are no more uniterated vertices in the currently iterated
     * connected component; <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt> if there are no more uniterated vertices in the currently iterated
     * connected component; <tt>false</tt> otherwise.
     */
    protected abstract boolean isConnectedComponentExhausted();

    /**
     * Update data structures the first time we see a vertex.
     *
     * @param vertex the vertex encountered
     * @param edge   the edge via which the vertex was encountered, or null if the vertex is a
     *               starting point
     */
    protected abstract void encounterVertex(V vertex, E edge);

    /**
     * Returns the vertex to be returned in the following call to the iterator <code>next</code>
     * method.
     *
     * @return the next vertex to be returned by this iterator.
     */
    protected abstract V provideNextVertex();

    /**
     * Access the data stored for a seen vertex.
     *
     * @param vertex a vertex which has already been seen.
     * @return data associated with the seen vertex or <code>null</code> if no data was associated
     * with the vertex. A <code>null</code> return can also indicate that the vertex was
     * explicitly associated with <code>
     * null</code>.
     */
    protected D saw(V vertex) {
        return seen.get(vertex);
    }

    /**
     * Determines whether a vertex has been seen yet by this traversal.
     *
     * @param vertex vertex in question
     * @return <tt>true</tt> if vertex has already been seen
     */
    protected boolean isSeenVertex(V vertex) {
        return seen.containsKey(vertex);
    }

    /**
     * Called whenever we re-encounter a vertex. The default implementation does nothing.
     *
     * @param vertex the vertex re-encountered
     * @param edge   the edge via which the vertex was re-encountered
     */
    protected abstract void encounterVertexAgain(V vertex, E edge);

    /**
     * Stores iterator-dependent data for a vertex that has been seen.
     *
     * @param vertex a vertex which has been seen.
     * @param data   data to be associated with the seen vertex.
     * @return previous value associated with specified vertex or <code>
     * null</code> if no data was associated with the vertex. A <code>
     * null</code> return can also indicate that the vertex was explicitly associated with
     * <code>null</code>.
     */
    protected D putSeenData(V vertex, D data) {
        return seen.put(vertex, data);
    }

    /**
     * Called when a vertex has been finished (meaning is dependent on traversal represented by
     * subclass).
     *
     * @param vertex vertex which has been finished
     */
    protected void finishVertex(V vertex) {
        fireVertexFinished(vertex);
    }

    private void addIncidentEdges(V s) {

        boolean firesEdges = wantsEdges();

        if (inOrOut) {
            for (Pair<V, E> p : graph.incomingEdgesOf(s)) {
                go(s, firesEdges, p.getOne(), p.getTwo());
            }
        } else {
            for (Pair<E, V> p : graph.outgoingEdgesOf(s)) {
                go(s, firesEdges, p.getTwo(), p.getOne());
            }
        }

    }

    private void go(V s, boolean firesEdges, V t, E edge) {
        if (firesEdges)
            fireEdgeTraversed(s, edge, t);

        if (isSeenVertex(t)) {
            encounterVertexAgain(t, edge);
        } else {
            encounterVertex(t, edge);
        }
    }


    @Override
    public Iterable<Pair<E, V>> edgesOut(V vertex) {
        return graph.outgoingEdgesOf(vertex);
    }


//    private EdgeTraversalEvent<E> createEdgeTraversalEvent(E edge) {
//        if (isReuseEvents()) {
//            reusableEdgeEvent.setEdge(edge);
//
//            return reusableEdgeEvent;
//        } else {
//            return new EdgeTraversalEvent<>(this, edge);
//        }
//    }
//
//    private ObjectIntPair createVertexTraversalEvent(V vertex) {
//        if (isReuseEvents()) {
//            reusableVertexEvent.setVertex(vertex);
//
//            return reusableVertexEvent;
//        } else {
//            return new VertexTraversalEvent<>(this, vertex);
//        }
//    }

    private void encounterStartVertex() {
        encounterVertex(startVertex, null);
        startVertex = null;
    }

//    static interface SimpleContainer<T> {
//        /**
//         * Tests if this container is empty.
//         *
//         * @return <code>true</code> if empty, otherwise <code>false</code>.
//         */
//        public boolean isEmpty();
//
//        /**
//         * Adds the specified object to this container.
//         *
//         * @param o the object to be added.
//         */
//        public void add(T o);
//
//        /**
//         * Remove an object from this container and return it.
//         *
//         * @return the object removed from this container.
//         */
//        public T remove();
//    }

}
