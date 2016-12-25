package spimedb.impl;

import com.google.common.collect.Iterators;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.index.graph.MapGraph;
import spimedb.index.graph.VertexContainer;
import spimedb.index.oct.OctBox;
import spimedb.util.geom.BB;
import spimedb.util.geom.Vec3D;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


public class OctSpimeDB implements SpimeDB {

    public enum OpEdge {
        extinh, intinh
    }

    public final MapGraph<String, NObject, Pair<OpEdge, Twin<String>>> graph;
    public final OctBox oct;

    /** in-memory, map-based */
    public OctSpimeDB() {
        this(new SpimeMapGraph());
    }

    public OctSpimeDB(MapGraph<String, NObject, Pair<OpEdge, Twin<String>>> g) {
        this.graph = g;

        this.oct = new MyOctBox(
                new Vec3D(-180f, -90f, -1),
                new Vec3D(360f, 180f, 2),
                new Vec3D(0.05f, 0.05f, 0.05f));

        /** add any pre-existing values */
        graph.vertices.forEach((k,v)->{
            NObject vv = v.value();
            BB spatial = vv.getBB();
            float x = spatial.x(); //HACK use x=NaN to signal non-spatial
            if (x==x)
                oct.put(vv);
        });
    }

    @Override
    public String toString() {
        return "OctSpimeDB{" +
                graph +
                "\n, oct=" + oct +
                '}';
    }

    @Override
    public void close() {

    }

    @Override
    public NObject put(NObject d) {
        final String id = d.getId();

        if (d.isSpatial()) {
            oct.put(d);
        }

        graph.put(id, d);

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
        return Iterators.transform(graph.containerSet().iterator(), VertexContainer::value);
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

        oct.forEachInSphere(new Vec3D((float)lat, (float)lon, 0), radDegrees, n -> {
            l.add((NObject)n); //TODO HACK avoid casting, maybe change generics
            //TODO exit from this loop early if capacity reached
        });

        //System.out.println(lat + " " + lon + " " + radDegrees + " -> " + l);

        return l.iterator();
    }

    private float metersToDegrees(float radMeters) {
        return radMeters / 110648f;
    }

    public static class SpimeMapGraph extends MapGraph<String, NObject, Pair<OpEdge, Twin<String>>> {

        public SpimeMapGraph() {
            super(new java.util.concurrent.ConcurrentHashMap(), new java.util.concurrent.ConcurrentHashMap());
        }

        @Override
        protected Set<Pair<OpEdge, Twin<String>>> newEdgeSet() {
            return new UnifiedSet<>();
        }

        @Override
        protected NObject newBlankVertex(String s) {
            //return new NObject(s);
            return null;
        }
    }

    static class MyOctBox extends OctBox {

        public MyOctBox(Vec3D origin, Vec3D extents, Vec3D resolution) {
            super(origin, extents, resolution);
        }

        @NotNull
        @Override
        protected OctBox newBox(OctBox parent, Vec3D off, Vec3D extent) {
            return new MyOctBox(parent, off, extent);
        }

        @Override protected void onModified() {
            System.out.println(this + " modified");
        }

    }
}
