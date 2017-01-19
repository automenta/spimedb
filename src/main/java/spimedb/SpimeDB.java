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


public class SpimeDB implements Iterable<AbstractNObject>  {



    public static final String VERSION = "SpimeDB v-0.00";

    @JsonIgnore
    public final static Logger logger = LoggerFactory.getLogger(SpimeDB.class);
    public static final ForkJoinPool exe = ForkJoinPool.commonPool();

    @JsonIgnore public final Map<String, SpatialSearch<AbstractNObject>> spacetime = new ConcurrentHashMap<>();

    @JsonIgnore public final Map<String, AbstractNObject> obj;

    public final Tags tags = new Tags();

    /** server-side javascript engine */
    final ScriptEngineManager engineManager = new ScriptEngineManager();
    public final NashornScriptEngine js = (NashornScriptEngine) engineManager.getEngineByName("nashorn");

    /** whether the root node has a spatial index, which would store everything */
    private boolean rootSpace = false;

    /** in-memory, map-based */
    public SpimeDB() {
        this(new ConcurrentHashMap());
    }

    public SpimeDB(Map<String, AbstractNObject> g) {

        this.obj = g;

    }

    public SpatialSearch<AbstractNObject> spaceIfExists(String tag) {
        return spacetime.get(tag);
    }

    public SpatialSearch<AbstractNObject> space(String tag) {
        return spacetime.computeIfAbsent(tag, (t) -> {
            return new LockingRTree<AbstractNObject>(new RTree<AbstractNObject>(new RectND.Builder(),
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

    @JsonProperty("status") /*@JsonSerialize(as = RawSerializer.class)*/ @Override
    public String toString() {
        return "{\"" + getClass().getSimpleName() + "\":{\"size\":" + size() +
                ",\"spacetime\":\"" + spacetime + "\"}}";
    }


    public void close() {

    }


    /** returns the resulting (possibly merged/transformed) nobject, which differs from typical put() semantics */
    public AbstractNObject put(@Nullable AbstractNObject d) {
        if (d == null)
            return null;

        //TODO use 'obj.merge' for correct un-indexing of prevoius value
        String id = d.id();

        AbstractNObject previous = obj.put(id, d);

        boolean changed = false;

        if (previous!=null) {
             if (previous.equals(d))
                 return previous;
             else {
                 changed = true;
             }
        }


        String[] tags = d.tags();
        if (tags!=null && tags.length > 0) {
            for (String t : tags) {
//                Tag tagJect = schema.tag(t, tt -> {
//                    return new Tag(tt);
//                });
                //if (this.schema.inh.addNode(t)) {
                    //index the tag if it doesnt exist in the graph
//                    NObject tagJect = get(t);
//                    if (tagJect!=null) {
//                        String[] parents = tagJect.tag;
//                        if (parents != null)
//                            this.tags.tag(t, parents);
//                    }
                //}
            }
            this.tags.tag(id, tags);

            if (d.bounded()) {
                for (String t : tags)
                    if (rootSpace || !t.isEmpty()) //dont store in root
                        space(t).add(d);
            }
        }

        if (changed) {
            //TODO emit notification
        }

        return d;
    }

    private void tag(String id, String[] parents) {
        tags.tag(id, parents);
    }



    public Iterator<AbstractNObject> iterator() {
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


    public AbstractNObject get(String nobjectID) {
        return obj.get(nobjectID);
    }


    public Query get(Query q) {
        q.onStart();

        Predicate<AbstractNObject> each = q.each;


        Iterable<String> include = tagsAndSubtags(q.include);
        for (String t : include) {
            SpatialSearch<AbstractNObject> s = spaceIfExists(t);
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


    /** computes the set of subtree (children) tags held by the extension of the input (parent) tags
     * @param parentTags if empty, searches all tags; otherwise searches the specified tags and all their subtags
     */
    public Iterable<String> tagsAndSubtags(@Nullable String... parentTags) {
        if (parentTags == null || parentTags.length==0)
            return tags.tags(); //ALL
        else
            return tags.tagsAndSubtags(parentTags);
    }

    public static void runLater(Runnable r) {
        exe.execute(r);
    }

    public void put(Stream<? extends AbstractNObject> s) {
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
