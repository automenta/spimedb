package spimedb.graph.travel;

import org.eclipse.collections.api.tuple.Pair;

import java.util.Iterator;

/**
 * this isnt a graph traversal, it's a travel. no need to make up words for things which
 * already have a more commonly used equivalent!!!
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 */
public interface Travel<V, E> extends Iterator<V> {
    /**
     * Test whether this iterator is set to traverse the grpah across connected components.
     *
     * @return <code>true</code> if traverses across connected components, otherwise
     *         <code>false</code>.
     */
    boolean isCrossComponentTraversal();




    /** outgoing edges if directed, all edges if undirected */
    Iterable<Pair<E, V>> edgesOut(V vertex);

    /**
     * Adds the specified traversal listener to this iterator.
     *
     * @param l the traversal listener to be added.
     */
    void addTraveller(Traveller<V, E> l);

    /**
     * Unsupported.
     */
    @Override
    void remove();

    /**
     * Removes the specified traversal listener from this iterator.
     *
     * @param l the traversal listener to be removed.
     */
    void removeTraveller(Traveller<V, E> l);

    /** whether this travel will provide travellers with edge events */
    boolean wantsEdges();


}

// End GraphIterator.java