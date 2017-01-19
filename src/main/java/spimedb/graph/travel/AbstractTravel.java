package spimedb.graph.travel;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An empty implementation of a graph iterator to minimize the effort required to implement graph
 * iterators.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * @author Barak Naveh
 * @since Jul 19, 2003
 */
public abstract class AbstractTravel<V, E> implements Travel<V, E> {

    protected final List<Traveller<V, E>> travellers = new CopyOnWriteArrayList<>();

    protected boolean crossComponentTraversal = true;

    // We keep this cached redundantly with Travellers.size()
    // so that subclasses can use it as a fast check to see if
    // event firing calls can be skipped.


    // TODO: support ConcurrentModificationException if graph modified
    // during iteration.

    //protected Specifics<V, E> specifics;
//

    /**
     * Sets the cross component traversal flag - indicates whether to traverse the graph across
     * connected components.
     *
     * @param crossComponentTraversal if <code>true</code> traverses across connected components.
     */
    public void setCrossComponentTraversal(boolean crossComponentTraversal) {
        this.crossComponentTraversal = crossComponentTraversal;
    }

    /**
     * Test whether this iterator is set to traverse the graph across connected components.
     *
     * @return <code>true</code> if traverses across connected components, otherwise
     * <code>false</code>.
     */
    @Override
    public boolean isCrossComponentTraversal() {
        return crossComponentTraversal;
    }




    /**
     * Adds the specified traversal listener to this iterator.
     *
     * @param passenger the traversal listener to be added.
     */
    @Override
    public void addTraveller(Traveller<V, E> passenger) {

        //if (!passengers.contains(passenger)) {
        travellers.add(passenger);
        //}
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the specified traversal listener from this iterator.
     *
     * @param l the traversal listener to be removed.
     */
    @Override
    public void removeTraveller(Traveller<V, E> l) {
        travellers.remove(l);
    }

    /**
     * Informs all listeners that the traversal of the current connected component finished.
     *
     * @param e the connected component finished event.
     */
    protected void fireConnectedComponentFinished(ObjectIntPair e) {
        for (int i = 0, passengersSize = travellers.size(); i < passengersSize; i++) {
            travellers.get(i).componentExit(e);
        }
    }

    /**
     * Informs all listeners that the traversal of the current connected component finished.
     *
     * @param e the connected component finished event.
     */
    protected void fireConnectedComponentStarted(ObjectIntPair e) {
        for (int i = 0, passengersSize = travellers.size(); i < passengersSize; i++) {
            travellers.get(i).componentEnter(e);
        }
    }


    boolean hasTravellers() {
        return !travellers.isEmpty();
    }


    /**
     * Informs all listeners that a the specified edge was visited.
     *
     * @param e the edge traversal event.
     */
    protected void fireEdgeTraversed(V s, E e, V t) {
        for (int i = 0, passengersSize = travellers.size(); i < passengersSize; i++) {
            travellers.get(i).edge(s, e, t);
        }
    }

    /**
     * Informs all listeners that a the specified vertex was visited.
     *
     * @param e the vertex traversal event.
     */
    protected void fireVertexEnter(@Nullable Pair<V, E> incoming, V v) {
        for (int i = 0, passengersSize = travellers.size(); i < passengersSize; i++) {
            travellers.get(i).vertexEnter(incoming, v);
        }

    }

    /**
     * Informs all listeners that a the specified vertex was finished.
     *
     * @param e the vertex traversal event.
     */
    protected void fireVertexFinished(V v) {
        for (int i = 0, passengersSize = travellers.size(); i < passengersSize; i++) {
            travellers.get(i).vertexExit(v);
        }
    }

    // -------------------------------------------------------------------------

//    /**
//     * Creates directed/undirected graph specifics according to the provided graph -
//     * directed/undirected, respectively.
//     *
//     * @param g the graph to create specifics for
//     * @return the created specifics
//     */
//    static <V, E> Specifics<V, E> createGraphSpecifics(Graph<V, E> g) {
//        if (g instanceof DirectedGraph<?, ?>) {
//            return new DirectedSpecifics<>((DirectedGraph<V, E>) g);
//        } else {
//            return new UndirectedSpecifics<>(g);
//        }
//    }

//    /**
//     * Provides unified interface for operations that are different in directed graphs and in
//     * undirected graphs.
//     */
//    abstract static class Specifics<VV, EE> {
//        /**
//         * Returns the edges outgoing from the specified vertex in case of directed graph, and the
//         * edge touching the specified vertex in case of undirected graph.
//         *
//         * @param vertex the vertex whose outgoing edges are to be returned.
//         * @return the edges outgoing from the specified vertex in case of directed graph, and the
//         * edge touching the specified vertex in case of undirected graph.
//         */
//        public abstract Set<? extends EE> edgesOf(VV vertex);
//    }

//    /**
//     * An implementation of {@link Specifics} for a directed graph.
//     */
//    static class DirectedSpecifics<VV, EE>
//            extends Specifics<VV, EE> {
//        private DirectedGraph<VV, EE> graph;
//
//        /**
//         * Creates a new DirectedSpecifics object.
//         *
//         * @param g the graph for which this specifics object to be created.
//         */
//        public DirectedSpecifics(DirectedGraph<VV, EE> g) {
//            graph = g;
//        }
//
//        /**
//         * @see CrossComponentIterator.Specifics#edgesOf(Object)
//         */
//        @Override
//        public Set<? extends EE> edgesOf(VV vertex) {
//            return graph.outgoingEdgesOf(vertex);
//        }
//    }

//    /**
//     * An implementation of {@link Specifics} in which edge direction (if any) is ignored.
//     */
//    static class UndirectedSpecifics<VV, EE>
//            extends Specifics<VV, EE> {
//        private Graph<VV, EE> graph;
//
//        /**
//         * Creates a new UndirectedSpecifics object.
//         *
//         * @param g the graph for which this specifics object to be created.
//         */
//        public UndirectedSpecifics(Graph<VV, EE> g) {
//            graph = g;
//        }
//
//        /**
//         * @see CrossComponentIterator.Specifics#edgesOf(Object)
//         */
//        @Override
//        public Set<EE> edgesOf(VV vertex) {
//            return graph.edgesOf(vertex);
//        }
//    }
}