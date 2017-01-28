package spimedb;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jcog.Util;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.suggest.DocumentDictionary;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.FreeTextSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
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
import spimedb.index.Search;
import spimedb.index.lucene.DocumentNObject;
import spimedb.index.rtree.*;
import spimedb.query.Query;
import spimedb.util.FileUtils;

import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static spimedb.index.rtree.SpatialSearch.DEFAULT_SPLIT_TYPE;


public class SpimeDB extends Search  {


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

//    @JsonIgnore
//    @Deprecated
//    public final Map<String, NObject> objMap;


    /**
     * server-side javascript engine
     */
    transient final ScriptEngineManager engineManager = new ScriptEngineManager();
    transient public final NashornScriptEngine js = (NashornScriptEngine) engineManager.getEngineByName("nashorn");

    private final StandardAnalyzer analyzer;

    private Lookup suggester;
    private DocumentDictionary nameDict;


    public final MapGraph<String, String> graph = new MapGraph<String, String>(
            new ConcurrentHashMap<>(),
            () -> Collections.synchronizedSet(new UnifiedSet()));

    final static String[] ROOT = new String[]{""};
    private final VertexContainer<String, String> rootNode;

    private final static RectBuilder<NObject> rectBuilder = (n) -> {
        PointND min = n.min();
        PointND max = n.max();
        if (min.equals(max))
            return new RectND(min); //share min/max point
        else
            return new RectND(min.coord, max.coord);
    };
    public String indexPath;


    /**
     * in-memory, map-based
     */
    public SpimeDB() throws IOException {
        this(new RAMDirectory());
        this.indexPath = null;
    }

    public SpimeDB(String path) throws IOException {
        this(FSDirectory.open(new File(path).toPath()));
        this.indexPath = new File(path).getAbsolutePath();
        logger.info("index: {}", indexPath);
    }

    SpimeDB(Directory dir) throws IOException {
        super(dir);

        this.analyzer = new StandardAnalyzer();

        rootNode = graph.addVertex(ROOT[0]);

        int preloaded[] = new int[1];
        forEach(x -> {
            reindex(x);
            preloaded[0]++;
        });
        logger.info("{} objects loaded", preloaded[0]);

    }

    long lastSuggesterCreated = 0;
    long minSuggesterUpdatePeriod = 1000 * 2;

    @Nullable private Lookup suggester() {
        synchronized (dir) {
            if (lastCommit - lastSuggesterCreated > minSuggesterUpdatePeriod ) {
                suggester = null; //re-create since it is invalidated
            }

            if (suggester == null) {
                nameDict = new DocumentDictionary(reader(), NObject.NAME, NObject.NAME);
                FreeTextSuggester nextSuggester = new FreeTextSuggester(new SimpleAnalyzer());
                try {
                    nextSuggester.build(nameDict);
                    suggester = nextSuggester;
                    lastSuggesterCreated = now();
                    logger.info("suggester updated, count={}", suggester.getCount());
                } catch (IOException e) {
                    logger.error("suggester update: {}", e);
                    return null;
                }
            }
        }
        return suggester;
    }



        /*public Iterable<Document> all() {

    }*/

    public Document the(String id) {


        try {
            IndexSearcher searcher = searcher();
            if (searcher == null)
                return null;

            TermQuery x = new TermQuery(new Term(NObject.ID, id));
            TopDocs y = searcher.search(x, firstResultOnly);
            int hits = y.totalHits;
            if (hits > 0) {
                if (hits > 1) {
                    logger.warn("multiple documents with id={} exist: {}", id, y);
                }
                return searcher.doc(y.scoreDocs[0].doc);
            }

        } catch (IOException e) {
            logger.warn("query: {}", e);
        }
        return null;
    }

    @Nullable
    private IndexSearcher searcher()  {
        DirectoryReader r = reader();
        if (r != null) {
            return new IndexSearcher(r);
        } else {
            return null;
        }
    }

    @Nullable private DirectoryReader reader()  {
        try {
            if (DirectoryReader.indexExists(dir))
                return DirectoryReader.open(dir);
            else
                return null;
        } catch (IOException e) {
            logger.error("index reader: {}", e);
            return null;
        }
    }

    public SearchResult find(String query, int hitsPerPage) throws IOException, ParseException {
        org.apache.lucene.search.Query q = new QueryParser(NObject.NAME, analyzer).parse(query);
        return find(q, hitsPerPage);
    }

    @NotNull
    private SearchResult find(org.apache.lucene.search.Query q, int hitsPerPage) throws IOException {

        IndexSearcher searcher = searcher();
        if (searcher != null) {
            TopDocs docs = searcher.search(q, hitsPerPage);
            if (docs.totalHits > 0) {
                return new SearchResult(q, searcher, docs);
            }
        }

        return new SearchResult(q, null, null); //TODO: return EmptySearchResult;
    }


    public List<Lookup.LookupResult> suggest(String qText, int count) throws IOException {
        return suggester().lookup(qText, false, count);
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

    public int size() {
        int[] size = new int[1];
        forEach(x -> size[0]++); //HACK
        return size[0];
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



    public SpatialSearch<NObject> spaceIfExists(String tag) {
        return spacetime.get(tag);
    }

    public SpatialSearch<NObject> space(String tag) {
        return spacetime.computeIfAbsent(tag, (t) -> {
            return new LockingRTree<NObject>(new RTree<NObject>(rectBuilder,
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
        return "{\"" + getClass().getSimpleName() + "\":{" +
                ",\"spacetime\":\"" + spacetime + "\"}}";
    }


    public void close() {

    }


    final List<BiConsumer<NObject, SpimeDB>> onChange = new CopyOnWriteArrayList<>();

    public void on(BiConsumer<NObject, SpimeDB> changed) {
        onChange.add(changed);
    }


    public final ConcurrentHashMap<String,ReentrantLock> lock = new ConcurrentHashMap<>();


    public void run(String id, Runnable r)  {
        run(id, ()->{
            r.run();
            return null;
        });
    }

    public <X> X run(String id, Supplier<X> r)  {
        ReentrantLock l = lock.computeIfAbsent(id, MyReentrantLock::new);

        l.lock();

        Throwable thrown = null;
        X result = null;
        try {
            result = r.get();
        } catch (Throwable t) {
            thrown = t;
        }

        l.unlock();

        if (thrown!=null) {
            throw new RuntimeException(thrown);
        }

        return result;
    }

    public Future<NObject> addAsync(@Nullable NObject next) {
        return exe.submit(()->{
           return add(next);
        });
    }

    /**
     * returns the resulting (possibly merged/transformed) nobject, which differs from typical put() semantics
     */
    public NObject add(@Nullable NObject _next) {
        if (_next == null)
            return null;

        String id = _next.id();
        DocumentNObject next = DocumentNObject.get(_next);

        return run(id, () -> {

            logger.debug("add {}", id);

            DocumentNObject previous = get(id);
            if (previous != null) {


                if (deepEquals(previous.document, next.document))
                    return previous;
                //if (graphed(previous).equalsDeep(graphed(next))) {
                    //return previous;
                //}
            }

            commit(next);

            reindex(previous, next);

            if (!onChange.isEmpty()) {
                for (BiConsumer<NObject, SpimeDB> c : onChange) {
                    c.accept(next, this);
                }
            }

            return next;
        });
    }

    private boolean deepEquals(Document a, Document b) {
        List<IndexableField> af = (a.getFields());
        List<IndexableField> bf = (b.getFields());
        int size = af.size();
        if (bf.size()!=size)
            return false;

        for (int i = 0; i < size; i++) {

            if (!af.get(i).name().equals(bf.get(i).name()))
                return false;

            IndexableField afi = af.get(i);
            IndexableField bfi = bf.get(i);

            String as = afi.toString();
            String bs = bfi.toString();


            //HACK
            as = as.substring(as.indexOf('<'));
            bs = bs.substring(bs.indexOf('<'));
            if (!as.equals(bs))
                return false;


        }
        return true;
    }

    private void reindex(NObject current) {
        reindex(null, current);
    }

    private void reindex(NObject previous, NObject current) {


        String[] tags = current.tags();

        this.tag(current.id(), tags, previous != null ? previous.tags() : null);


        if (current instanceof GraphedNObject)
            current = new MutableNObject(current); //store as un-graphed immutable

        //HACK remove tag field now that it is indexed in the graph
        if (current instanceof MutableNObject)
            ((MutableNObject) current).remove(">");

        if ((previous!=null && previous.bounded()) || (current!=null && current.bounded())) {
            reindexSpatial(previous, current, tags);
        }

    }

    private void reindexSpatial(NObject previous, NObject current, String[] tags) {
        for (String t : tags) {
            if (!t.isEmpty()) { //dont store in root
                SpatialSearch<NObject> s = space(t);
                if (previous != null && previous.bounded())
                    s.remove(previous);
                if (current != null && current.bounded())
                    s.add(current);
            }
        }
    }

    protected NObject internal(NObject next) {
        return next;
    }


//    public Iterator<NObject> iterator() {
//        GraphedNObject reusedWrapper = new GraphedNObject(graph);
//        return Iterators.transform(
//            objMap.values().iterator(), x -> {
//                reusedWrapper.set(x);
//                return new MutableNObject( reusedWrapper );
//            }
//        );
//    }
    public void forEach(Consumer<NObject> each)  {

        //long startTime = System.currentTimeMillis();
        //create the term query object
        MatchAllDocsQuery query = new MatchAllDocsQuery();
        //do the search
        TopDocs hits = null;
        try {
            IndexSearcher searcher = searcher();
            if (searcher == null)
                return;

            hits = searcher.search(query, Integer.MAX_VALUE);
            //long endTime = System.currentTimeMillis();

//            System.out.println(hits.totalHits +
//                    " documents found. Time :" + (endTime - startTime) + "ms");
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                if (doc!=null) {
                    each.accept(DocumentNObject.get(doc));
                }
            }
        } catch (IOException e) {
            logger.error("{}",e);
        }
    }


    public DocumentNObject get(String id) {
        Document d = the(id);
        if (d!=null)
            return DocumentNObject.get(d);
        return null;
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

    private final class MyReentrantLock extends ReentrantLock {
        private final String id;

        public MyReentrantLock(String id) {
            super(true);
            this.id = id;
        }

        @Override
        public synchronized void unlock() {
            if (!hasQueuedThreads()) {
                lock.remove(id);
            }
            super.unlock();
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
