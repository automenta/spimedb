package spimedb.index.graph.travel;

import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;
import spimedb.index.graph.MapGraph;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * runs a set of travels sharing a common 'seen' experience.
 * this effectively performs a set of union of the vertex
 * events since 'seen' is keyed by vertices.
 */
abstract public class UnionTravel<V, E, D> implements Iterable<V>, Consumer<V> {

    private final Iterable<CrossComponentTravel<V, E, D>> subTravels;

    private final Map<V, D> seen = new ConcurrentHashMap<V, D>();


    public UnionTravel(MapGraph<V, E> g, V... keys) {
        this(g, Arrays.asList(keys));
    }

    public UnionTravel(MapGraph<V, E> g, Iterable<V> keys) {

        subTravels = Iterables.transform(keys, x -> {
            CrossComponentTravel<V, E, D> t = get(x, g, seen);
            if (t.seen != seen)
                throw new RuntimeException("subTravels should have been constructed to share seen");
            return t;
        });
    }

    protected abstract CrossComponentTravel<V,E,D> get(V start, MapGraph<V, E> graph, Map<V, D> seen);

    @NotNull
    @Override
    public Iterator<V> iterator() {

        //TODO implement a custom iterator that provides intermediate results
        subTravels.forEach(s -> {
            //attach travellers
//            if (hasTravellers()) {
//                for (Traveller<V, E> t : travellers) {
//                    s.addTraveller(t);
//                }
//            }

            s.forEachRemaining(this);
        });

        return seen.keySet().iterator();

    }

    @Override
    public void accept(V v) {

    }
}
