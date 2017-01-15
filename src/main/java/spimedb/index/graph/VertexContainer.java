package spimedb.index.graph;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Set;

/**
 * A container for vertex edges.
 *
 * <p>In this edge container we use array lists to minimize memory toll.
 * However, for high-degree vertices we replace the entire edge container
 * with a direct access subclass (to be implemented).</p>
 *
 * @author Barak Naveh
 */
public class VertexContainer<V,E> implements Serializable
{
    final Set<Pair<V,E>> incoming;

    final Set<Pair<E,V>> outgoing;

    public VertexContainer(Set<Pair<V,E>> incoming, Set<Pair<E,V>> outgoing) {
        this.incoming = incoming;
        this.outgoing = outgoing;
    }

    @Override
    public String toString() {
        return "{" +
                "i=" + incoming +
                ",o=" + outgoing +
                '}';
    }

    /**
     * .
     *
     * @param e
     */
    public boolean addIncomingEdge(V s, E e) {
        return incoming.add(pair(s, e));
    }

    @NotNull private Pair pair(Object x, Object y) {
        return Tuples.pair(x, y);
    }

    /**
     * .
     *
     * @param e
     */
    public boolean addOutgoingEdge(E e, V t) {
        return outgoing.add(Tuples.pair(e, t));
    }

    /**
     * .
     *
     * @param e
     */
    public boolean removeIncomingEdge(V s, E e) {
        return incoming.remove(pair(s, e));
    }

    /**
     * .
     *
     * @param e
     */
    public boolean removeOutgoingEdge(E e, V t)    {
        return outgoing.remove(Tuples.pair(e, t));
    }


    public boolean containsOutgoingEdge(E e, V t) {
        return outgoing.contains(Tuples.pair(e, t));
    }
}
