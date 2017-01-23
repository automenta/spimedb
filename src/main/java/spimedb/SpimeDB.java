package spimedb;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Iterators;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.graph.MapGraph;
import spimedb.graph.VertexContainer;
import spimedb.graph.VertexIncidence;
import spimedb.graph.travel.BreadthFirstTravel;
import spimedb.graph.travel.CrossComponentTravel;
import spimedb.graph.travel.UnionTravel;
import spimedb.index.rtree.LockingRTree;
import spimedb.index.rtree.RTree;
import spimedb.index.rtree.RectND;
import spimedb.index.rtree.SpatialSearch;
import spimedb.query.Query;
import spimedb.util.FileUtils;

import javax.script.ScriptEngineManager;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static spimedb.index.rtree.SpatialSearch.DEFAULT_SPLIT_TYPE;


public class SpimeDB implements Iterable<NObject> {


    public static final String VERSION = "SpimeDB v-0.00";

    @JsonIgnore
    public final static Logger logger = LoggerFactory.getLogger(SpimeDB.class);
    public static final ForkJoinPool exe = ForkJoinPool.commonPool();

    /**
     * default location of file resources if unspecified
     */
    public static final String TMP_SPIMEDB_CACHE_PATH = "/tmp/spimedb.cache"; //TODO use correct /tmp location per platform (ex: Windows will need somewhere else)

    @JsonIgnore
    public final Map<String, SpatialSearch<NObject>> spacetime = new ConcurrentHashMap<>();

    @JsonIgnore
    @Deprecated
    public final Map<String, NObject> objMap;


    /**
     * server-side javascript engine
     */
    transient final ScriptEngineManager engineManager = new ScriptEngineManager();
    transient public final NashornScriptEngine js = (NashornScriptEngine) engineManager.getEngineByName("nashorn");

    private File resources;

    public final MapGraph<String, String> graph = new MapGraph<String, String>(
            new ConcurrentHashMap<>(),
            () -> Collections.synchronizedSet(new UnifiedSet()));

    final static String[] ROOT = new String[]{""};
    private final VertexContainer<String, String> rootNode;

    /**
     * in-memory, map-based
     */
    public SpimeDB() {
        this(new ConcurrentHashMap());
    }

    public SpimeDB(Map<String, NObject> g) {
        super();
        rootNode = graph.addVertex(ROOT[0]);
        this.objMap = g;
        resources(TMP_SPIMEDB_CACHE_PATH);
    }

    public static void LOG(String l, Level ll) {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(l)).setLevel(ll);
    }


    public void tag(@NotNull String x, @NotNull String[] nextTags, @Nullable String[] prevTags) {

        if (nextTags.length == 0)
            nextTags = ROOT;

        if (prevTags != null) {
            ImmutableSet<String> ns = Sets.immutable.of(nextTags);
            ImmutableSet<String> ps = Sets.immutable.of(prevTags);
            if (ns.equals(ps))
                return; //no change
        }

        synchronized (graph) {

            VertexContainer<String, String> src = graph.addVertex(x);

            if (prevTags != null) {
                //TODO use Set intersection to determine the difference in tags that actually need to be removed because some may just get added again below
                for (String y : prevTags) {
                    graph.removeEdge(src, x, y, NObject.INH);
                }
            }

            for (String y : nextTags) {
                graph.addEdge(src, x, y, NObject.INH);
            }
        }

    }


    public Set<String> tags() {
        return graph.vertexSet();
    }

    public Iterator<String> roots() {
        return rootNode.inV();
    }

    private static class SubTags<V, E> extends UnionTravel<V, E, Object> {
        public SubTags(MapGraph<V, E> graph, V... parentTags) {
            super(graph, parentTags);
        }

        @Override
        protected CrossComponentTravel<V, E, Object> get(V start, MapGraph<V, E> graph, Map<V, Object> seen) {
            return new BreadthFirstTravel<>(graph, start, seen);
        }
    }

    public SpimeDB resources(String path) {
        this.resources = FileUtils.pathOrCreate(path).toFile();
        return this;
    }

    public SpatialSearch<NObject> spaceIfExists(String tag) {
        return spacetime.get(tag);
    }

    public SpatialSearch<NObject> space(String tag) {
        return spacetime.computeIfAbsent(tag, (t) -> {
            return new LockingRTree<NObject>(new RTree<NObject>(new RectND.Builder(),
                    2, 8, DEFAULT_SPLIT_TYPE),
                    new ReentrantReadWriteLock());
        });
    }


//    @Override
//    public NObject a(String id, String... tags) {
//        return null;
//    }

    @JsonProperty("status") /*@JsonSerialize(as = RawSerializer.class)*/
    @Override
    public String toString() {
        return "{\"" + getClass().getSimpleName() + "\":{\"size\":" + size() +
                ",\"spacetime\":\"" + spacetime + "\"}}";
    }


    public void close() {

    }


    final List<BiConsumer<NObject, SpimeDB>> onChange = new CopyOnWriteArrayList<>();

    public void on(BiConsumer<NObject, SpimeDB> changed) {
        onChange.add(changed);
    }


    //TODO
    /*public void on(BiConsumer<NObject,NObject> changedFromTo) {

    }*/


    /**
     * returns the resulting (possibly merged/transformed) nobject, which differs from typical put() semantics
     */
    public NObject add(@Nullable NObject next) {
        if (next == null)
            return null;

        return objMap.compute(next.id(), (i, previous) -> {

            boolean changed, neww;

            if (previous != null) {
                if (NObject.equalsDeep(graphed(previous), graphed(next)))
                    return previous;

                changed = true;
                neww = false;
            } else {
                changed = false;
                neww = true;
            }

            NObject current = reindex(previous, next);

            if (!onChange.isEmpty()) {
                exe.submit(() -> {
                    for (BiConsumer<NObject, SpimeDB> c : onChange) {
                        c.accept(current, this);
                    }
                });
            }

            return current;
        });
    }

    private NObject reindex(NObject previous, NObject current) {

        String[] tags = current.tags();

        this.tag(current.id(), tags, previous != null ? previous.tags() : null);


        if (current instanceof GraphedNObject)
            current = new MutableNObject(current); //store as un-graphed immutable

        //HACK remove tag field now that it is indexed in the graph
        if (current instanceof MutableNObject)
            ((MutableNObject) current).remove(">");

        if (current.bounded()) {
            reindexSpatial(previous, current, tags);
        }

        return current;
    }

    private void reindexSpatial(NObject previous, NObject current, String[] tags) {
        for (String t : tags) {
            if (!t.isEmpty()) { //dont store in root
                SpatialSearch<NObject> s = space(t);
                if (previous != null)
                    s.remove(previous);
                s.add(current);
            }
        }
    }

    protected NObject internal(NObject next) {
        return next;
    }


    public Iterator<NObject> iterator() {
        GraphedNObject reusedWrapper = new GraphedNObject(graph);
        return Iterators.transform(
            objMap.values().iterator(), x -> {
                reusedWrapper.set(x);
                return new MutableNObject( reusedWrapper );
            }
        );
    }

    @JsonIgnore
    public boolean isEmpty() {
        return size() == 0;
    }

    @JsonIgnore
    public int size() {
        return objMap.size();
    }


    public NObject get(String nobjectID) {


        return objMap.get(nobjectID);
    }


    public Query get(Query q) {
        q.onStart();

        Predicate<NObject> each = q.each;


        Iterable<String> include = tagsAndSubtags(q.include);
        for (String t : include) {
            SpatialSearch<NObject> s = spaceIfExists(t);
            if (s != null && !s.isEmpty()) {
                if (q.bounds != null && q.bounds.length > 0) {
                    for (RectND x : q.bounds) {
                        switch (q.boundsCondition) {
                            case Contain:
                                if (!s.containing(x, each))
                                    break;
                                break;
                            case Intersect:
                                if (!s.intersecting(x, each))
                                    break;
                                break;
                        }
                    }
                } else {
                    if (!s.intersecting(RectND.ALL_4, each)) //iterate all items
                        break;
                }
            }

        }

        q.onEnd();
        return q;
    }


    /**
     * computes the set of subtree (children) tags held by the extension of the input (parent) tags
     *
     * @param parentTags if empty, searches all tags; otherwise searches the specified tags and all their subtags
     */
    public Iterable<String> tagsAndSubtags(@Nullable String... parentTags) {
        if (parentTags == null || parentTags.length == 0)
            return this.tags(); //ALL
        else {
            return new SubTags(graph, parentTags);
        }
    }

    public static void runLater(Runnable r) {
        exe.execute(r);
    }

    public void add(Stream<? extends NObject> s) {
        s.forEach(this::add);
    }

    @Deprecated
    public static synchronized void sync() {
        sync(60);
    }

    public static synchronized void sync(float seconds) {
        exe.awaitQuiescence(Math.round(seconds * 1000f), TimeUnit.MILLISECONDS);
    }


    public GraphedNObject graphed(String id) {
        NObject n = get(id);
        if (n != null)
            return graphed(n);
        return null;
    }

    public GraphedNObject graphed(NObject n) {
        if ((n instanceof GraphedNObject) && (((GraphedNObject) n).graph == graph))
            return (GraphedNObject) n; //already wrapped

        return new GraphedNObject(this.graph, n);
    }

    @JsonSerialize(using = NObject.NObjectSerializer.class)
    public static class GraphedNObject extends ProxyNObject {

        private final MapGraph<String, String> graph;


        GraphedNObject(MapGraph<String, String> graph) {
            this.graph = graph;
        }

        GraphedNObject(MapGraph<String, String> graph, NObject n) {
            this(graph);
            set(n);
        }

        @Override
        public void forEach(BiConsumer<String, Object> each) {
            n.forEach((k, v) -> {
                if (!k.equals(TAG)) //HACK filter out tag field because the information will be present in the graph
                    each.accept(k, v);
            });

            VertexContainer<String, String> v = graph.vertex(id(), false);
            if (v != null) {
                Map<String, VertexIncidence<String>> boundary = v.incidence();
                boundary.forEach(each);
            }
        }

    }

    //    static class MyOctBox extends OctBox {
//
//        public MyOctBox(Vec3D origin, Vec3D extents, Vec3D resolution) {
//            super(origin, extents, resolution);
//        }
//
//        @NotNull
//        @Override
//        protected OctBox newBox(OctBox parent, Vec3D off, Vec3D extent) {
//            return new MyOctBox(parent, off, extent);
//        }
//
//        @Override protected void onModified() {
//            System.out.println(this + " modified");
//        }
//
//    }

//    public static <E> Pair<E, Twin<String>> edge(E e, String from, String to) {
//        return Tuples.pair(e, Tuples.twin(from, to));
//    }


}
