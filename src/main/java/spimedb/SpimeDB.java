package spimedb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.graph.MapGraph;
import spimedb.graph.VertexContainer;
import spimedb.graph.VertexIncidence;
import spimedb.index.rtree.LockingRTree;
import spimedb.index.rtree.RTree;
import spimedb.index.rtree.RectND;
import spimedb.index.rtree.SpatialSearch;
import spimedb.plan.Agent;
import spimedb.query.Query;
import spimedb.util.FileUtils;

import javax.script.ScriptEngineManager;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static spimedb.index.rtree.SpatialSearch.DEFAULT_SPLIT_TYPE;


public class SpimeDB extends Agent implements Iterable<NObject> {


    public static final String VERSION = "SpimeDB v-0.00";

    @JsonIgnore
    public final static Logger logger = LoggerFactory.getLogger(SpimeDB.class);
    public static final ForkJoinPool exe = ForkJoinPool.commonPool();

    /** default location of file resources if unspecified */
    public static final String TMP_SPIMEDB_CACHE_PATH = "/tmp/spimedb.cache"; //TODO use correct /tmp location per platform (ex: Windows will need somewhere else)

    @JsonIgnore
    public final Map<String, SpatialSearch<NObject>> spacetime = new ConcurrentHashMap<>();

    @JsonIgnore
    public final Map<String, NObject> obj;

    transient public final Tags tags = new Tags();

    /**
     * server-side javascript engine
     */
    transient final ScriptEngineManager engineManager = new ScriptEngineManager();
    transient public final NashornScriptEngine js = (NashornScriptEngine) engineManager.getEngineByName("nashorn");

    private File resources;


    /**
     * in-memory, map-based
     */
    public SpimeDB() {
        this(new ConcurrentHashMap());
    }

    public SpimeDB(Map<String, NObject> g) {
        super(ForkJoinPool.commonPool());

        this.obj = g;
        resources(TMP_SPIMEDB_CACHE_PATH);
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

    Class[] resolve(String... tags) {
        return this.tags.resolve(tags);
    }

    public Class the(String tagID, String... supertags) {

        //.subclass(NObject.class).implement(s)
                    /*.method(any())
                    .intercept(MethodDelegation.to(MyInterceptor.class)
                            .andThen(SuperMethodCall.INSTANCE)
                            .defineField("myCustomField", Object.class, Visibility.PUBLIC)*/
                    /*.make()
                    .load(cl)
                    .getLoaded();*/
        return tags.the(tagID, supertags);
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


    final List<BiConsumer<NObject,SpimeDB>> onChange = new CopyOnWriteArrayList<>();

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

        return obj.compute(next.id(), (i, previous) -> {

            boolean changed, neww;

            if (previous != null) {
                if (NObject.equalsDeep(previous, next))
                    return previous;

                changed = true;
                neww = false;
            } else {
                changed = false;
                neww = true;
            }

            NObject current = internal(next);

            for (BiConsumer<NObject, SpimeDB> c : onChange) {
                exe.execute(()->c.accept(current, this));
            }

            reindex(previous, current);

            return current;
        });
    }

    private void reindex(NObject previous, NObject current) {

        String[] tags = current.tags();

        this.tags.tag(current.id(), tags, previous!=null ? previous.tags() : null);

        if (current.bounded()) {
            for (String t : tags) {
                if (!t.isEmpty()) { //dont store in root
                    SpatialSearch<NObject> s = space(t);
                    if (previous != null)
                        s.remove(previous);
                    s.add(current);
                }
            }
        }
    }

    protected NObject internal(NObject next) {
        return next;
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
            return tags.tags(); //ALL
        else
            return tags.tagsAndSubtags(parentTags);
    }

    public static void runLater(Runnable r) {
        exe.execute(r);
    }

    public void add(Stream<? extends NObject> s) {
        s.forEach(this::add);
    }

    public static synchronized void sync() {
        exe.awaitQuiescence(60, TimeUnit.SECONDS);
    }


    public GraphedNObject graphed(String id) {
        NObject n = get(id);
        if (n!=null)
            return graphed(n);
        return null;
    }

    public GraphedNObject graphed(NObject n) {
        return new GraphedNObject(tags.graph, n);
    }

    @JsonSerialize(using = NObject.NObjectSerializer.class)
    public static class GraphedNObject extends ProxyNObject {

        private final MapGraph<String, String> graph;

        GraphedNObject(MapGraph<String,String> graph) {
            this.graph = graph;
        }

        GraphedNObject(MapGraph<String,String> graph, NObject n) {
            this(graph);
            set(n);
        }

        @Override
        public void forEach(BiConsumer<String, Object> each) {
            n.forEach((k,v) -> {
                if (!k.equals(TAG)) //HACK filter out tag field because the information will be present in the graph
                    each.accept(k, v);
            });

            VertexContainer<String, String> v = graph.vertex(id(), false);
            if (v != null) {
                Map<String,VertexIncidence<String>> boundary = v.incidence();
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
