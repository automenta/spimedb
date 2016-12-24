package spimedb.index.graph;

import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jgrapht.*;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.util.ArrayUnenforcedSet;

import java.util.*;

/**
 * Created by me on 7/15/15.
 */
public abstract class MapGraph<V, C, E> extends AbstractGraph<V, E> {
    private static final long serialVersionUID = -1263088497616142427L;

    private static final String LOOPS_NOT_ALLOWED = "loops not allowed";
    private static final String NOT_IN_DIRECTED_GRAPH = "no such operation in a directed graph";
    final boolean allowingLoops = false;
    final boolean allowingMultipleEdges = true;

    protected final Map<V, VertexContainer<C, E>> vertices;
    protected final Map<E, Twin<V>> edges;


    /**
     * Construct a new graph. The graph can either be directed or undirected,
     * depending on the specified edge factory.
     *
     * @throws NullPointerException if the specified edge factory is <code>
     *                              null</code>.
     */
    protected MapGraph() {
        vertices = newVertexMap(); //new LinkedHashMap<V, DirectedEdgeContainer<V, E>>();
        edges = newEdgeMap(); //new LinkedHashMap<E, IntrusiveEdge>();
    }

    protected abstract Map<V, VertexContainer<C, E>> newVertexMap();

    protected abstract Map<E, Twin<V>> newEdgeMap();

    /**
     * Returns <code>true</code> if and only if self-loops are allowed in this
     * graph. A self loop is an edge that its source and target vertices are the
     * same.
     *
     * @return <code>true</code> if and only if graph loops are allowed.
     */
    public boolean isAllowingLoops() {
        return allowingLoops;
    }

    /**
     * Returns <code>true</code> if and only if multiple edges are allowed in
     * this graph. The meaning of multiple edges is that there can be many edges
     * going from vertex v1 to vertex v2.
     *
     * @return <code>true</code> if and only if multiple edges are allowed.
     */
    public boolean isAllowingMultipleEdges() {
        return allowingMultipleEdges;
    }

    /**
     * @see Graph#addEdge(Object, Object)
     */
    @Override
    public E addEdge(V sourceVertex, V targetVertex) {
        throw new UnsupportedOperationException();
//        assertVertexExist(sourceVertex);
//        assertVertexExist(targetVertex);
//
//        if (!allowingMultipleEdges
//                && containsEdge(sourceVertex, targetVertex))
//        {
//            return null;
//        }
//
//        if (!allowingLoops && sourceVertex.equals(targetVertex)) {
//            throw new IllegalArgumentException(LOOPS_NOT_ALLOWED);
//        }
//
//        E e = createEdge(sourceVertex, targetVertex);
//
//        if (containsEdge(e)) { // this restriction should stay!
//
//            return null;
//        } else {
//            IntrusiveEdge intrusiveEdge =
//                    createIntrusiveEdge(e, sourceVertex, targetVertex);
//
//            edgeMap.put(e, new Pair( sourceVertex, targetVertex ));
//            specifics.addEdgeToTouchingVertices(e);
//
//            return e;
//        }
    }

    /**
     * adds vertices if they dont already exist
     *
     * @see Graph#addEdge(Object, Object, Object)
     */
    @Override
    public boolean addEdge(V sourceVertex, V targetVertex, E e) {
        if (e == null) {
            throw new NullPointerException();
        } else if (containsEdge(e)) {
            return false;
        }

        addVertex(sourceVertex);
        addVertex(targetVertex);

        if (!allowingLoops && sourceVertex.equals(targetVertex)) {
            throw new IllegalArgumentException(LOOPS_NOT_ALLOWED);
        }

        if (!allowingMultipleEdges && containsEdge(sourceVertex, targetVertex)) {
            return false;
        }

        synchronized (edges) {
            edges.put(e, Tuples.twin(sourceVertex, targetVertex));
            getEdgeContainer(sourceVertex).addOutgoingEdge(e);
            getEdgeContainer(targetVertex).addIncomingEdge(e);
        }

        return true;
    }

    /**
     * @see Graph#getEdgeSource(Object)
     */
    @Override
    public V getEdgeSource(E e) {
        Twin<V> ee = edges.get(e);
        return ee!=null ? ee.getOne() : null;
    }

    /**
     * @see Graph#getEdgeTarget(Object)
     */
    @Override
    public V getEdgeTarget(E e) {
        Twin<V> ee = edges.get(e);
        return ee!=null ? ee.getTwo() : null;
    }

    /**
     * Returns a shallow copy of this graph instance. Neither edges nor vertices
     * are cloned.
     *
     * @return a shallow copy of this set.
     * @throws RuntimeException
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() {
        throw new UnsupportedOperationException();

//        try {
//            TypeUtil<AbstractBaseGraph<V, E>> typeDecl = null;
//
//            AbstractBaseGraph<V, E> newGraph =
//                    TypeUtil.uncheckedCast(super.clone(), typeDecl);
//
//            newGraph.edgeMap = new LinkedHashMap<E, IntrusiveEdge>();
//
//            newGraph.edgeFactory = this.edgeFactory;
//            newGraph.unmodifiableEdgeSet = null;
//            newGraph.unmodifiableVertexSet = null;
//
//            // NOTE:  it's important for this to happen in an object
//            // method so that the new inner class instance gets associated with
//            // the right outer class instance
//            newGraph.specifics = newGraph.createSpecifics();
//
//            Graphs.addGraph(newGraph, this);
//
//            return newGraph;
//        } catch (CloneNotSupportedException e) {
//            e.printStackTrace();
//            throw new RuntimeException();
//        }
    }

    /**
     * @see Graph#containsEdge(Object)
     */
    @Override
    public boolean containsEdge(E e) {
        return edges.containsKey(e);
    }

    /**
     * @see Graph#edgeSet()
     */
    @Override
    public Set<E> edgeSet() {
        return edges.keySet();
    }

    /**
     * @see Graph#removeEdge(Object, Object)
     */
    @Override
    public E removeEdge(V sourceVertex, V targetVertex) {

        synchronized (edges) {
            E e = getEdge(sourceVertex, targetVertex, true);
            if (e != null) {
                if (edges.remove(e) != null) {
                    removeEdgeFromTarget(e);
                }
            }
            return e;
        }

    }

    /**
     * @see Graph#removeEdge(Object)
     */
    @Override
    public boolean removeEdge(E e) {
        synchronized (edges) {
            if (edges.remove(e)!=null) {
                removeEdgeFromTouchingVertices(e);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * @see Graph#removeVertex(Object)
     */
    @Override
    public boolean removeVertex(V v) {

        if (containsVertex(v)) {
            Set<E> touchingEdgesList = edgesOf(v);

            // cannot iterate over list - will cause
            // ConcurrentModificationException
            removeAllEdges(new ArrayList<E>(touchingEdgesList));

            getVertexSet().remove(v); // remove the vertex itself

            return true;
        } else {
            return false;
        }
    }

    /**
     * @see Graph#getEdgeWeight(Object)
     */
    @Override
    public double getEdgeWeight(E e) {
        return Double.NaN;
//        if (e instanceof DefaultWeightedEdge) {
//            return ((DefaultWeightedEdge) e).getWeight();
//        } else if (e == null) {
//            throw new NullPointerException();
//        } else {
//            return WeightedGraph.DEFAULT_EDGE_WEIGHT;
//        }
    }

    @Override
    public EdgeFactory<V, E> getEdgeFactory() {
        return null;
    }

    @Override
    public Set<V> vertexSet() {
        return vertices.keySet();
    }


    /**
     * @see WeightedGraph#setEdgeWeight(Object, double)
     */
    public void setEdgeWeight(E e, double weight) {
        throw new RuntimeException("weights not supported");
//        assert (e instanceof DefaultWeightedEdge) : e.getClass();
//        ((DefaultWeightedEdge) e).weight = weight;
    }

    @Override
    public boolean containsVertex(V v) {
        return vertices.containsKey(v);
    }

    abstract protected Set<E> newEdgeSet();

    @Override
    public boolean addVertex(V v) {
        // add with a lazy edge container entry
        return vertices.putIfAbsent(v, null) == null;
    }

    public void addVertex(V id, C value) {
        vertices.computeIfAbsent(id, k ->
            new VertexContainer<>(value, newEdgeSet(), newEdgeSet() )
        );
    }



    protected Set<V> getVertexSet() {
        return vertices.keySet();
    }

    /**
     * @see Graph#getAllEdges(Object, Object)
     */
    @Override
    public Set<E> getAllEdges(V sourceVertex, V targetVertex) {
        Set<E> edges = null;

        if (containsVertex(sourceVertex) && containsVertex(targetVertex)) {
            edges = new ArrayUnenforcedSet<E>();

            VertexContainer ec = getEdgeContainer(sourceVertex);

            Iterator<E> iter = ec.outgoing.iterator();

            while (iter.hasNext()) {
                E e = iter.next();

                if (getEdgeTarget(e).equals(targetVertex)) {
                    edges.add(e);
                }
            }
        }

        return edges;
    }

    /**
     * @see Graph#getEdge(Object, Object)
     */
    @Override
    public E getEdge(V sourceVertex, V targetVertex) {
        return getEdge(sourceVertex, targetVertex, false);
    }

    protected E getEdge(V sourceVertex, V targetVertex, boolean removeFromSrcIfFound) {
        if (containsVertex(sourceVertex) && containsVertex(targetVertex)) {
            VertexContainer srcVertex = getEdgeContainer(sourceVertex);

            Iterator<E> iter = srcVertex.outgoing.iterator();

            while (iter.hasNext()) {
                E e = iter.next();

                if (getEdgeTarget(e).equals(targetVertex)) {
                    if (removeFromSrcIfFound)
                        iter.remove();
                    return e;
                }
            }
        }

        return null;

    }

    /**
     * @see UndirectedGraph#degreeOf(Object)
     */
    public int degreeOf(V vertex) {
        throw new UnsupportedOperationException(NOT_IN_DIRECTED_GRAPH);
    }

    /**
     * @see Graph#edgesOf(Object)
     */
    @Override
    public Set<E> edgesOf(V vertex) {
        ArrayUnenforcedSet<E> inAndOut =
                new ArrayUnenforcedSet<E>(getEdgeContainer(vertex).incoming);
        inAndOut.addAll(getEdgeContainer(vertex).outgoing);

        // we have two copies for each self-loop - remove one of them.
        if (allowingLoops) {
            Set<E> loops = getAllEdges(vertex, vertex);

            for (int i = 0; i < inAndOut.size(); ) {
                Object e = inAndOut.get(i);

                if (loops.contains(e)) {
                    inAndOut.remove(i);
                    loops.remove(e); // so we remove it only once
                } else {
                    i++;
                }
            }
        }

        return Collections.unmodifiableSet(inAndOut);
    }

    /**
     * @see DirectedGraph#inDegreeOf(Object)
     */
    public int inDegreeOf(V vertex) {
        return getEdgeContainer(vertex).incoming.size();
    }

    /**
     * @see DirectedGraph#incomingEdgesOf(Object)
     */
    public Set<E> incomingEdgesOf(V vertex) {
        return getEdgeContainer(vertex).incoming;
    }

    /**
     * @see DirectedGraph#outDegreeOf(Object)
     */
    public int outDegreeOf(V vertex) {
        return getEdgeContainer(vertex).outgoing.size();
    }

    /**
     * @see DirectedGraph#outgoingEdgesOf(Object)
     */
    public Set<E> outgoingEdgesOf(V vertex) {
        return getEdgeContainer(vertex).outgoing;
    }

    public void removeEdgeFromTouchingVertices(E e) {
        removeEdgeFromSource(e);
        removeEdgeFromTarget(e);
    }

    protected void removeEdgeFromTarget(E e) {
        V target = getEdgeTarget(e);
        getEdgeContainer(target).removeIncomingEdge(e);
    }

    protected void removeEdgeFromSource(E e) {
        V source = getEdgeSource(e);
        getEdgeContainer(source).removeOutgoingEdge(e);
    }

    /**
     * A lazy build of edge container for specified vertex.
     *
     * @param vertex a vertex in this graph.
     * @return EdgeContainer
     */
    private VertexContainer<C, E> getEdgeContainer(V vertex) {

        VertexContainer<C, E> ec = vertices.computeIfAbsent(vertex, v -> {
            return new VertexContainer(null, newEdgeSet(), newEdgeSet());
        });

        return ec;
    }


    public Collection<VertexContainer<C,E>> containerSet() {
        return vertices.values();
    }

    public C getVertexValue(V i) {
        VertexContainer<C, E> c = vertices.get(i);
        return c!=null ? c.getValue() : null;
    }

}
