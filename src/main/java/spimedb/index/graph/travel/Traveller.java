package spimedb.index.graph.travel;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * rides along on a travel, observing events
 */
public interface Traveller<V,E> {

    /**
     * Called to inform the listener that the specified edge have been visited during the graph
     * traversal. Depending on the traversal algorithm, edge might be visited more than once.
     */
    void edge(@NotNull V s, @NotNull E e, @NotNull V t);

    /**
     * Called to inform the listener that the specified vertex have been visited during the graph
     * traversal. Depending on the traversal algorithm, vertex might be visited more than once.
     */
    boolean vertexEnter(@Nullable Pair<V,E> incoming, @NotNull V v);

    /**
     * Called to inform the listener that the specified vertex have been finished during the graph
     * traversal. Exact meaning of "finish" is algorithm-dependent; e.g. for DFS, it means that all
     * vertices reachable via the vertex have been visited as well.
     */
    boolean vertexExit(@NotNull V v);

    void componentEnter(ObjectIntPair e);
    void componentExit(ObjectIntPair e);
}
