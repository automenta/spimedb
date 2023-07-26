package spimedb.graph;

import com.google.common.collect.Iterators;
import jcog.data.set.ArrayUnenforcedSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * TODO
 *  change this to one Map<Pair<E,V>,Byte> where the integer's value bits mean:
 *      0 = incoming (off/on)
 *      1 = outgoing (off/on)
 */
public record VertexContainer<V, E>(Set<Pair<V, E>> incoming, Set<Pair<E, V>> outgoing) implements Serializable {

    @Override
    public String toString() {
        return "{" + incoming + " | " + outgoing + '}';
    }

    /**
     * .
     *
     * @param e
     */
    public boolean addIncomingEdge(V s, E e) {
        return incoming.add(pair(s, e));
    }

    @NotNull
    private static Pair pair(Object x, Object y) {
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
    public boolean removeOutgoingEdge(E e, V t) {
        return outgoing.remove(Tuples.pair(e, t));
    }


    public boolean containsOutgoingEdge(E e, V t) {
        return outgoing.contains(Tuples.pair(e, t));
    }

    public Iterator<V> inV() {
        return Iterators.transform(incoming.iterator(), Pair::getOne);
    }

    public Iterator<E> inE() {
        return Iterators.transform(incoming.iterator(), Pair::getTwo);
    }

    public Iterator<V> outV() {
        return Iterators.transform(outgoing.iterator(), Pair::getTwo);
    }

    public Iterator<E> outE() {
        return Iterators.transform(outgoing.iterator(), Pair::getOne);
    }

    public Set<V> inVset() {
        ArrayUnenforcedSet<V> a = new ArrayUnenforcedSet<>();
        Iterators.addAll(a, inV());
        return a;
    }

    public Set<V> outVset() {
        ArrayUnenforcedSet<V> a = new ArrayUnenforcedSet<>();
        Iterators.addAll(a, outV());
        return a;
    }

    /**
     * collates the edges by edge type, then direction, then vertex
     */
    public Map<E, VertexIncidence<V>> incidence() {
        Map<E, VertexIncidence<V>> m = new HashMap();
        incoming.forEach(ve -> m.computeIfAbsent(ve.getTwo(), ee -> new VertexIncidence<>()).in.add(ve.getOne()));
        outgoing.forEach(ev -> m.computeIfAbsent(ev.getOne(), ee -> new VertexIncidence<>()).out.add(ev.getTwo()));
        return m;
    }

}
