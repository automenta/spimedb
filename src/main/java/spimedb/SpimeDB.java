package spimedb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import net.bytebuddy.ByteBuddy;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.index.oct.OctBox;
import spimedb.index.rtree.LockingRTree;
import spimedb.index.rtree.RTree;
import spimedb.index.rtree.RectND;
import spimedb.index.rtree.SpatialSearch;
import spimedb.query.Query;
import spimedb.util.geom.Vec3D;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static spimedb.index.rtree.SpatialSearch.DEFAULT_SPLIT_TYPE;


public class SpimeDB implements Iterable<NObject>  {

    public static final String VERSION = "SpimeDB v-0.00";

    final static Logger logger = LoggerFactory.getLogger(SpimeDB.class);

    public final MutableGraph<String> tag = GraphBuilder.directed().allowsSelfLoops(false).expectedNodeCount(512).nodeOrder(ElementOrder.unordered()).build();

    @JsonIgnore public final Map<String,SpatialSearch<NObject>> spacetime = new ConcurrentHashMap<>();

    public final Map<String, NObject> obj;

    /** in-memory, map-based */
    public SpimeDB() {
        this(new ConcurrentHashMap());
    }

    public SpimeDB(Map<String, NObject> g) {

        this.obj = g;

        /*this.oct = new MyOctBox(
                new Vec3D(-180f, -90f, -1),
                new Vec3D(360f, 180f, 2),
                new Vec3D(0.05f, 0.05f, 0.05f));*/

    }

    public SpatialSearch<NObject> space(String tag) {
        return spacetime.computeIfAbsent(tag, (t) -> {
            return new LockingRTree<NObject>(new RTree<NObject>(new RectND.Builder(),
                    2, 8, DEFAULT_SPLIT_TYPE),
                    new ReentrantReadWriteLock());
        });
    }

    protected final Map<String,Class> tagClasses = new ConcurrentHashMap<>();
    protected final ClassLoader cl = ClassLoader.getSystemClassLoader();
    final ByteBuddy tagProxyBuilder = new ByteBuddy();

    Class[] resolve(String... tags) {
        Class[] c = new Class[tags.length];
        int i = 0;
        for (String s : tags) {
            Class x = tagClasses.get(s);
            if (x == null)
                throw new NullPointerException("missing class: " + s);
            c[i++] = x;
        }
        return c;
    }

    public Class the(String tagID, String... supertags) {

        synchronized (tagClasses) {
            if (tagClasses.containsKey(tagID))
                throw new RuntimeException(tagID + " class already defined");

            Class[] s = resolve(supertags);
            Class proxy = tagProxyBuilder.makeInterface(s).name("_" + tagID).make().load(cl).getLoaded();
            tagClasses.put(tagID, proxy);
            return proxy;
        }

        //.subclass(NObject.class).implement(s)
                    /*.method(any())
                    .intercept(MethodDelegation.to(MyInterceptor.class)
                            .andThen(SuperMethodCall.INSTANCE)
                            .defineField("myCustomField", Object.class, Visibility.PUBLIC)*/
                    /*.make()
                    .load(cl)
                    .getLoaded();*/
    }

//    @Override
//    public NObject a(String id, String... tags) {
//        return null;
//    }

    @JsonProperty("status") /*@JsonSerialize(as = RawSerializer.class)*/ @Override
    public String toString() {
        return "{\"" + getClass().getSimpleName() + "\":{\"size\":" + size() +
                ",\"spacetime\":\"" + spacetime + "\"}}";
    }


    public void close() {

    }


    public NObject put(NObject d) {
        //final String id = d.getId();

        //TODO use 'obj.merge' for correct un-indexing of prevoius value
        String id = d.getId();

        NObject previous = obj.put(id, d);

        /*if (tag.nodes().contains(id)) {
            //TODO re-tag
        }*/

        String[] tags = d.tag;
        if (tags!=null) {
            for (String t : tags) {
                if (this.tag.addNode(t)) {
                    //index the tag if it doesnt exist in the graph
                    NObject tagJect = get(t);
                    if (tagJect!=null) {
                        String[] parents = tagJect.tag;
                        if (parents != null)
                            tag(t, parents);
                    }
                }
            }
            tag(id, tags);

            if (d.bounded()) {
                for (String t : tags)
                    space(t).add(d);
            }
        }

        //Object tags = obj.get(">");

        return null;
    }

    private void tag(String id, String[] parents) {
        for (String parentTag : parents) {
            tag.putEdge(parentTag, id);
        }
    }


    public static <E> Pair<E, Twin<String>> edge(E e, String from, String to) {
        return Tuples.pair(e, Tuples.twin(from, to));
    }

    public Iterator<NObject> iterator() {
        return obj.values().iterator();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return size() == 0;
    }

    @JsonIgnore
    public int size() {
        return obj.size();
    }


    public NObject get(String nobjectID) {
        return obj.get(nobjectID);
    }


    public Query get(Query q) {
        q.onStart();

        main:
        for (String t : tagsAndSubtags(q.include)) {

            SpatialSearch<NObject> s = space(t);
            if (s.isEmpty())
                continue;

            for (RectND x : q.bounds) {
                switch (q.boundsCondition) {
                    case Contain:
                        if (!s.containing(x, q.each))
                            break main;
                        break;
                    case Intersect:
                        if (!s.intersecting(x, q.each))
                            break main;
                        break;
                }
            }
        }

        q.onEnd();
        return q;
    }


    /** computes the set of subtree (children) tags held by the extension of the input (parent) tags
     * @param parentTags if empty, searches all tags; otherwise searches the specified tags and all their subtags
     */
    public Set<String> tagsAndSubtags(@Nullable String... parentTags) {

        if (parentTags == null || parentTags.length == 0) {
            return tag.nodes();
        }

        Set<String> s = new HashSet();

        for (String x : parentTags) {
            if (s.add(x)) {
                s.addAll( tag.successors(x) );
            }
        }
        return s;
    }


    private static float metersToDegrees(float radMeters) {
        return radMeters / 110648f;
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
