package spimedb.index.graph;

import com.google.common.collect.Iterators;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.index.oct.OctBox;
import spimedb.util.geom.Vec3D;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


public class SpimeGraph implements SpimeDB {


    public enum OpEdge {
        extinh, intinh
    }


    final MapGraph<String, NObject, Pair<OpEdge, Twin<String>>> graph = new SpimeMapGraph();
    public final OctBox<byte[]> spacetime = new OctBox(
            new Vec3D(-180f, -90f, 0),
            new Vec3D(360f, 180f, 0),
            new Vec3D(0.05f, 0.05f, 0f)
    );

    public SpimeGraph() {


    }

    @Override
    public void close() {

    }

    @Override
    public NObject put(NObject d) {
        final String id = d.getId();

        if (d.isSpatial()) {
            spacetime.put(d);
        }

        graph.addVertex(id, d);

        String parent = d.inside();
        if (parent != null) {
            graph.addVertex(parent);
            graph.addEdge(parent, id, edge(OpEdge.extinh, parent, id));

        }

        return null;
    }


    public static <E> Pair<E, Twin<String>> edge(E e, String from, String to) {
        return Tuples.pair(e, Tuples.twin(from, to));
    }


    @Override
    public Iterator<NObject> iterator() {
        return Iterators.transform(graph.containerSet().iterator(), VertexContainer::getValue );
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return graph.vertexSet().size();
    }

    @Override
    public NObject get(String nobjectID) {
        return graph.getVertexValue(nobjectID);
    }

    @Override
    public void children(String parent, Consumer<String> each) {



        (parent == null ? (graph.edgeSet()) : (graph.outgoingEdgesOf(parent))).forEach(e -> {
            if (e.getOne() == OpEdge.extinh)
                each.accept(e.getTwo().getOne());
        });

    }

    @Override
    public Iterator<NObject> get(double lat, double lon, double radMeters, int maxResults) {
        float radDegrees = metersToDegrees((float)radMeters);

        List<NObject> l = new FastList() {

            int count = 0;

            @Override
            public boolean add(Object newItem) {
                if (count == maxResults) return false;

                if (super.add(newItem)) {
                    count++;
                    return true;
                }
                return false;
            }
        };

        spacetime.forEachInSphere(new Vec3D((float)lat, (float)lon, 0), radDegrees, n -> {
            l.add((NObject)n); //TODO HACK avoid casting, maybe change generics
            //TODO exit from this loop early if capacity reached
        });

        //System.out.println(lat + " " + lon + " " + radDegrees + " -> " + l);

        return l.iterator();
    }

    private float metersToDegrees(float radMeters) {
        return radMeters / 110648f;
    }

    private static class SpimeMapGraph extends MapGraph<String, NObject, Pair<OpEdge, Twin<String>>> {

        @Override
        protected Map<String, VertexContainer<NObject, Pair<OpEdge, Twin<String>>>> newVertexMap() {
            return new ConcurrentHashMap();
        }

        @Override
        protected Map<Pair<OpEdge, Twin<String>>, Twin<String>> newEdgeMap() {
            return new ConcurrentHashMap();
        }

        @Override
        protected Set<Pair<OpEdge, Twin<String>>> newEdgeSet() {
            return new UnifiedSet<>();
        }
    }
}
