package spimedb.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jcog.data.set.ArrayUnenforcedSet;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * High-performance customizable JGraphT DirectedGraph impl
 */
public class MapGraph<V, E> implements Serializable {

    @JsonIgnore public final Map<V, VertexContainer<V,E>> vertices;

    @JsonIgnore private transient final Supplier<Set> edgeSetBuilder;

    /**
     * Construct a new graph. The graph can either be directed or undirected,
     * depending on the specified edge factory.
     *
     * @throws NullPointerException if the specified edge factory is <code>
     *                              null</code>.
     */
    public MapGraph(Map<V, VertexContainer<V, E>> vertexMap, Supplier<Set> edgeSetBuilder) {
        vertices = vertexMap;
        this.edgeSetBuilder = edgeSetBuilder;
    }

    @Override
    public String toString() {
        return vertices.toString();
    }

    /**
     * adds vertices if they dont already exist
     *
     * @see Graph#addEdge(Object, Object, Object)
     */
    
    public boolean addEdge(@NotNull V sourceVertex, @NotNull V targetVertex, @NotNull E e) {


        if (sourceVertex.equals(targetVertex)) {
            throw new IllegalArgumentException("loops not allowed");
        }

        VertexContainer<V, E> src = vertex(sourceVertex, true);

        return addEdge(src, sourceVertex, targetVertex, e);

    }

    public boolean addEdge(VertexContainer<V, E> src, @NotNull V sourceVertex, @NotNull V targetVertex, @NotNull E e) {
        if (src.addOutgoingEdge(e, targetVertex)) {
            if (!vertex(targetVertex, true).addIncomingEdge(sourceVertex, e)) {
                throw new RuntimeException("incidence fault");
            }
            return true;
        }
        return false; //already added
    }

    public boolean removeEdge(VertexContainer<V, E> srcC, @NotNull V src, @NotNull V tgt, @NotNull E e) {
        if (srcC.removeOutgoingEdge(e, tgt)) {
            if (!vertex(tgt, false).removeIncomingEdge(src, e)) {
                throw new RuntimeException("incidence fault");
            }
            return true;
        }

        return false; //already added
    }

    public boolean removeEdge(V src, V tgt, E e) {
        VertexContainer<V, E> s = vertex(src, false);
        if (s!=null) {
            VertexContainer<V, E> t = vertex(tgt, false);
            if (t!=null && s.removeOutgoingEdge(e, tgt)) {
                if (!t.removeIncomingEdge(src, e))
                    throw new RuntimeException("incidence fault");
                return true;
            }
        }
        return false;
    }

    /**
     * @see Graph#removeVertex(Object)
     */
    public boolean removeVertex(V v) {

        VertexContainer<V,E> container = vertices.remove(v);
        if (container!=null) {
            container.incoming().forEach(e -> vertex(e.getOne(), false).removeOutgoingEdge(e.getTwo(), v));
            container.outgoing().forEach(e -> vertex(e.getTwo(), false).removeIncomingEdge(v, e.getOne()));
            return true;
        }

        return false;
    }



    public Set<V> vertexSet() {
        return vertices.keySet();
    }


    
    public boolean containsVertex(V v) {
        return vertices.containsKey(v);
    }


    /** if vertex exists, still return true */
    public VertexContainer<V, E> addVertex(V id) {
        // add with a lazy edge container entry
        //return vertices.putIfAbsent(v, null) == null;
        return vertex(id, true);
    }




    protected Set<V> getVertexSet() {
        return vertices.keySet();
    }

    /**
     * @see Graph#getAllEdges(Object, Object)
     */
    
    public Set<E> getAllEdges(V sourceVertex, V targetVertex) {
        Set<E> edges = null;

        VertexContainer<V,E> S = vertex(sourceVertex, false);
        if (S!=null && containsVertex(targetVertex)) {
            edges = new ArrayUnenforcedSet<>();

            for (Pair<E,V> e : S.outgoing()) {
                if (e.getTwo().equals(targetVertex)) {
                    edges.add(e.getOne());
                }
            }
        }

        return edges;
    }




    /**
     * @see DirectedGraph#inDegreeOf(Object)
     */
    public int inDegreeOf(V vertex) {
        return incomingEdgesOf(vertex).size();
    }

    /**
     * @see DirectedGraph#incomingEdgesOf(Object)
     */
    public Set<Pair<V,E>> incomingEdgesOf(V vertex) {
        VertexContainer<V, E> vv = vertex(vertex, false);
        return vv!=null ? vv.incoming() : Collections.emptySet();
    }

    /**
     * @see DirectedGraph#outDegreeOf(Object)
     */
    public int outDegreeOf(V vertex) {
        return outgoingEdgesOf(vertex).size();
    }

    /**
     * @see DirectedGraph#outgoingEdgesOf(Object)
     */
    public Set<Pair<E,V>> outgoingEdgesOf(V vertex) {
        VertexContainer<V, E> vv = vertex(vertex, false);
        return vv!=null ? vv.outgoing() : Collections.emptySet();
    }


    /**
     * A lazy build of edge container for specified vertex.
     *
     * @param vertex a vertex in this graph.
     * @return EdgeContainer
     */
    public VertexContainer<V,E> vertex(V vertex, boolean createIfMissing) {
        if (createIfMissing) {
            return vertices.computeIfAbsent(vertex, v ->
                    new VertexContainer<>(edgeSetBuilder.get(), edgeSetBuilder.get()));
        } else {
            return vertices.get(vertex);
        }
    }


    public boolean containsEdge(V s, V t, E e) {
        VertexContainer<V, E> vs = vertex(s, false);
        return vs != null && vs.containsOutgoingEdge(e, t);
    }

    public boolean isLeaf(V id) {
        return inDegreeOf(id)==0;
    }
}
