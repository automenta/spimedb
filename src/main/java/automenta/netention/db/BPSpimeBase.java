package automenta.netention.db;

import automenta.netention.NObject;
import automenta.netention.geo.SpimeBase;
import com.google.common.collect.Iterators;
import com.syncleus.spangraph.HashMapGraph;
import com.syncleus.spangraph.MapGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import org.infinispan.util.concurrent.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blueprints Spimebase impl
 */
public class BPSpimeBase implements SpimeBase {

//    public static class NObjectRef {
//        public final String id;
//        public final NObject obj;
//
//        public NObjectRef(NObject n) {
//            this.obj = n;
//            this.id = n.getId();
//        }
//
//        @Override
//        public String toString() {
//            return id;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (obj == this) return true;
//            if (!(obj instanceof NObjectRef)) return false;
//            return id.equals((NObjectRef).id)
//            return super.equals(obj);
//        }
//    }

    final Map<String,NObject> index;
    final MapGraph<String> graph;

    public BPSpimeBase() {
        index = new IdentityHashMap();
        graph = new HashMapGraph() {

            @Override
            protected Map newEdgeMap() {
                return new ConcurrentHashMap();
            }

            @Override
            protected Map newVertexMap() {
                return new ConcurrentHashMap();
            }

            @Override
            protected Map<String, Set<Edge>> newVertexEdgeMap() {
                return new ConcurrentHashMap();
            }

            @Override
            protected Set<Edge> newEdgeSet(int i) {
                return new ConcurrentHashSet(i);
            }
        };
    }

    @Override
    public NObject put(NObject d) {
        final String id = d.getId();
        index.put(id, d);

        if (graph.getVertex(id)==null)
            graph.addVertex(id);

        String parent = d.inside();
        if (parent!=null) {
            if (graph.getVertex(parent)==null)
                graph.addVertex(parent);
            graph.addEdge(null, graph.getVertex(parent), graph.getVertex(id), ">");

        }

        return null;
    }

    @Override
    public Iterator<NObject> iterator() {
        return Iterators.transform(graph.vertexSet().iterator(), v ->  index.get(v.getId()));
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return graph.vertexCount();
    }

    @Override
    public NObject get(String nobjectID) {
        return index.get(nobjectID);
    }

    @Override
    public Collection<String> getInsides(String nobjectID) {
        Set<String> i = new TreeSet();

        if (nobjectID == null) {
            graph.getEdges().forEach(e -> {
                i.add(e.getVertex(Direction.OUT).getId().toString());
                //System.err.println(e.getLabel())
            });
        }
        else {

            graph.getEdges(">", get(nobjectID)).forEach(e -> {
                i.add(e.getLabel());
            });
        }

        return i;
    }

    @Override
    public Iterator<NObject> get(double lat, double lon, double radMeters, int maxResults) {
        return null;
    }
}
