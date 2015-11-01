package automenta.netention.db;

import automenta.netention.NObject;
import automenta.netention.geo.SpimeBase;
import com.google.common.collect.Iterators;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.list.mutable.FastList;
import com.gs.collections.impl.map.mutable.ConcurrentHashMap;
import com.gs.collections.impl.set.mutable.UnifiedSet;
import com.gs.collections.impl.tuple.Tuples;
import toxi.geom.Vec3D;
import vectrex.OctBox;

import java.util.*;


public class SpimeGraph implements SpimeBase {

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
    public NObject put(NObject d) {
        final String id = d.getId();

        if (d.isSpatial()) {
            spacetime.ADD(d);
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
        return Iterators.transform(graph.containerSet().iterator(), c -> c.getValue() );
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
    public Collection<String> getInsides(String nobjectID) {
        Set<String> i = new TreeSet();

        if (nobjectID == null) {
            graph.edgeSet().forEach(e -> {
                if (e.getOne() == OpEdge.extinh)
                    i.add(e.getTwo().getOne());
            });
        } else {

            graph.outgoingEdgesOf(nobjectID).forEach(e -> {
                if (e.getOne() == OpEdge.extinh)
                    i.add(e.getTwo().getTwo());
            });
        }

        return i;
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
