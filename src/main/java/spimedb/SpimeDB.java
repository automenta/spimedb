package spimedb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.index.rtree.LockingRTree;
import spimedb.index.rtree.RTree;
import spimedb.index.rtree.RectND;
import spimedb.index.rtree.SpatialSearch;
import spimedb.query.Query;

import javax.script.ScriptEngineManager;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static spimedb.index.rtree.SpatialSearch.DEFAULT_SPLIT_TYPE;


public class SpimeDB implements Iterable<NObject> {


    public static final String VERSION = "SpimeDB v-0.00";

    @JsonIgnore
    public final static Logger logger = LoggerFactory.getLogger(SpimeDB.class);
    public static final ForkJoinPool exe = ForkJoinPool.commonPool();

    @JsonIgnore
    public final Map<String, SpatialSearch<NObject>> spacetime = new ConcurrentHashMap<>();

    @JsonIgnore
    public final Map<String, NObject> obj;

    public final Tags tags = new Tags();

    /**
     * server-side javascript engine
     */
    final ScriptEngineManager engineManager = new ScriptEngineManager();
    public final NashornScriptEngine js = (NashornScriptEngine) engineManager.getEngineByName("nashorn");



    /**
     * in-memory, map-based
     */
    public SpimeDB() {
        this(new ConcurrentHashMap());
    }

    public SpimeDB(Map<String, NObject> g) {

        this.obj = g;

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


    /**
     * returns the resulting (possibly merged/transformed) nobject, which differs from typical put() semantics
     */
    public NObject put(@Nullable NObject next) {
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

            NObject nextI = internal(next);

            String[] tags = nextI.tags();

            this.tags.tag(nextI.id(), tags, previous!=null ? previous.tags() : null);

            if (nextI.bounded()) {
                for (String t : tags) {
                    if (!t.isEmpty()) { //dont store in root
                        SpatialSearch<NObject> s = space(t);
                        if (previous != null)
                            s.remove(previous);
                        s.add(nextI);
                    }
                }
            }

            return nextI;
        });
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

    public void put(Stream<? extends NObject> s) {
        s.forEach(this::put);
    }

    public static synchronized void sync() {
        exe.awaitQuiescence(60, TimeUnit.SECONDS);
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
