package spimedb;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Stopwatch;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.suggest.DocumentDictionary;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.FreeTextSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.graph.MapGraph;
import spimedb.graph.VertexContainer;
import spimedb.graph.travel.BreadthFirstTravel;
import spimedb.graph.travel.CrossComponentTravel;
import spimedb.graph.travel.UnionTravel;
import spimedb.index.DObject;
import spimedb.index.SearchResult;
import spimedb.index.rtree.*;
import spimedb.query.Query;
import spimedb.util.Locker;
import spimedb.util.PrioritizedExecutor;

import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static spimedb.index.rtree.SpatialSearch.DEFAULT_SPLIT_TYPE;


public class SpimeDB  {


    public static final String VERSION = "SpimeDB v-0.00";

    @JsonIgnore
    public final static Logger logger = LoggerFactory.getLogger(SpimeDB.class);

    public final PrioritizedExecutor exe = new PrioritizedExecutor(
        Math.max(2, 2 + Runtime.getRuntime().availableProcessors())
    );

    /**
     * default location of file resources if unspecified
     */
    public static final String TMP_SPIMEDB_CACHE_PATH = "/tmp/spimedb.cache"; //TODO use correct /tmp location per platform (ex: Windows will need somewhere else)

    @JsonIgnore
    public final Map<String, SpatialSearch<NObject>> spacetime = new ConcurrentHashMap<>();
    protected final Directory dir;

    protected static final CollectorManager<TopScoreDocCollector, TopDocs> firstResultOnly = new CollectorManager<TopScoreDocCollector, TopDocs>() {

        @Override
        public TopScoreDocCollector newCollector() {
            return TopScoreDocCollector.create(1);
        }


        @Override
        public TopDocs reduce(Collection<TopScoreDocCollector> collectors) {
            Iterator<TopScoreDocCollector> ii = collectors.iterator();
            if (ii.hasNext()) {
                TopScoreDocCollector l = ii.next();
                return l.topDocs();
            }
            return null;
        }
    };

//    @JsonIgnore
//    @Deprecated
//    public final Map<String, NObject> objMap;


    /**
     * server-side javascript engine
     */
    transient final ScriptEngineManager engineManager = new ScriptEngineManager();
    transient public final NashornScriptEngine js = (NashornScriptEngine) engineManager.getEngineByName("nashorn");

    static final int NObjectCacheSize = 64 * 1024;

    private final Map<String,DObject> out = new ConcurrentHashMap<>(1024);
    private final Cache<String, DObject> cache =
            Caffeine.newBuilder().maximumSize(NObjectCacheSize).build();

    private final AtomicBoolean writing = new AtomicBoolean(false);
    private final StandardAnalyzer analyzer;
    public final File file;

    protected long lastWrite = 0;

    private Lookup suggester;

    private DocumentDictionary nameDict;
    private DirectoryReader nameDictReader;


    public final MapGraph<String, String> graph = new MapGraph<String, String>(
            new ConcurrentHashMap<>(),
            () -> Collections.synchronizedSet(new UnifiedSet()));

    final static String[] ROOT = new String[]{""};
    private final VertexContainer<String, String> rootNode;


    private /* final */ Directory taxoDir;
    private final FacetsConfig facetsConfig = new FacetsConfig();

    private final static RectBuilder<NObject> rectBuilder = (n) -> {
        PointND min = n.min();
        PointND max = n.max();
        if (min.equals(max))
            return new RectND(min); //share min/max point
        else
            return new RectND(min.coord, max.coord);
    };
    public String indexPath;

    final QueryParser defaultFindQueryParser;


    /**
     * in-memory, map-based
     */
    public SpimeDB() throws IOException {
        this(null, new RAMDirectory());
        this.indexPath = null;
        this.taxoDir = new RAMDirectory();
    }

    public SpimeDB(String path) throws IOException {
        this(new File(path), FSDirectory.open(new File(path).toPath()));
        this.indexPath = file.getAbsolutePath();
        this.taxoDir = FSDirectory.open(file.toPath().resolve("taxo"));
        logger.info("index ready: file://{}", indexPath);
    }

    private SpimeDB(File file, Directory dir) throws IOException {

        this.file = file;
        this.dir = dir;
        this.analyzer = new StandardAnalyzer();

        this.facetsConfig.setHierarchical(NObject.ID, true);
        this.facetsConfig.setMultiValued(NObject.TAG, true);

//        DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(dir);
//        taxoWriter.addCategory(new FacetLabel(NObject.TYPE));
//        taxoWriter.commit();
//        taxoWriter.close();


        final String[] defaultFindFields = new String[] { NObject.NAME, NObject.DESC };
        final Map<String,Float> defaultFindFieldStrengths = Maps.mutable.with(
            NObject.NAME,1f,
            NObject.ID,0.75f,
            NObject.DESC,0.5f,
            NObject.TAG,0.25f
        );
        this.defaultFindQueryParser = new MultiFieldQueryParser(defaultFindFields, analyzer, defaultFindFieldStrengths);

        rootNode = graph.addVertex(ROOT[0]);

        int preloaded[] = new int[1];
        forEach(x -> {
            //System.out.println(x.toJSONString(true));
            reindex(x);
            preloaded[0]++;
        });
        logger.info("{} objects loaded", preloaded[0]);

    }

    long lastSuggesterCreated = 0;
    long minSuggesterUpdatePeriod = 1000 * 2;

    public static StringField string(String key, String value) {
        return new StringField(key, value, Field.Store.YES);
    }

//    public static final FieldType tokenizedString = new FieldType();
//    static {
//        tokenizedString.setOmitNorms(true);
//        tokenizedString.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
//        tokenizedString.setStored(true);
//        tokenizedString.setTokenized(true);
//        tokenizedString.freeze();
//    }
//
//    public static Field stringTokenized(String key, String value) {
//        return new Field(key, value, tokenizedString);
//    }

    public static TextField text(String key, String value) {
        return new TextField(key, value, Field.Store.YES);
    }


    public FacetResult facets(String dimension, int count) throws IOException {

        IndexSearcher searcher = searcher();
        if (searcher==null)
            return null;

        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);


        FacetsCollector fc = new FacetsCollector();

        // MatchAllDocsQuery is for "browsing" (counts facets
        // for all non-deleted docs in the index); normally
        // you'd use a "normal" query:
        searcher.search(new MatchAllDocsQuery(), fc);


        Facets facets = new FastTaxonomyFacetCounts(taxoReader, facetsConfig, fc);

        FacetResult result = facets.getTopChildren(count, dimension);

        searcher.getIndexReader().close();
        taxoReader.close();

        return result;
    }

    @Nullable private Lookup suggester() {
        synchronized (dir) {
            if (lastWrite - lastSuggesterCreated > minSuggesterUpdatePeriod ) {
                suggester = null; //re-create since it is invalidated
            }

            if (suggester == null) {
                if (nameDict==null) {
                    nameDictReader = reader();
                    if (nameDictReader == null)
                        return null;
                }

                nameDict = new DocumentDictionary(nameDictReader, NObject.NAME, NObject.NAME);
                FreeTextSuggester nextSuggester = new FreeTextSuggester(new SimpleAnalyzer());

                try {

                    Stopwatch time = Stopwatch.createStarted();

                    nextSuggester.build(nameDict);
                    suggester = nextSuggester;
                    lastSuggesterCreated = now();
                    logger.info("suggester updated, count={} {}ms", suggester.getCount(), time.elapsed(MILLISECONDS));

                    time.reset();

//                    FacetsCollector fc = new FacetsCollector();
//                    FacetsCollector.search(new IndexSearcher(nameDictReader), new MatchAllDocsQuery(), Integer.MAX_VALUE,
//                            fc
//                    );
//                    logger.info("facets updated, count={} {}ms", fc., time.elapsed(MILLISECONDS));

                } catch (IllegalArgumentException f) {
                    return null;
                } catch (IOException e) {
                    logger.error("suggester update: {}", e);
                    return null;
                }
            }
        }
        return suggester;
    }



    private Document the(String id) {

        try {
            IndexSearcher searcher = searcher();
            if (searcher == null)
                return null;

            TermQuery x = new TermQuery(new Term(NObject.ID, id));
            TopDocs y = searcher.search(x, firstResultOnly);
            int hits = y.totalHits;
            Document result = null;
            if (hits > 0) {
                if (hits > 1) {
                    logger.warn("multiple documents with id={} exist: {}", id, y);
                }
                result = searcher.doc(y.scoreDocs[0].doc);
            }

            searcher.getIndexReader().close();
            return result;
        } catch (IOException e) {
            logger.warn("query: {}", e);
            return null;
        }

    }

    @Nullable protected IndexSearcher searcher()  {
        DirectoryReader r = reader();
        return r != null ? new IndexSearcher(r) : null;
    }

    @Nullable private DirectoryReader reader()  {
        try {
            return DirectoryReader.indexExists(dir) ? DirectoryReader.open(dir) : null;
        } catch (IOException e) {
            logger.error("index reader: {}", e);
            throw new RuntimeException(e);
            //return null;
        }
    }

    @Nullable public SearchResult find(String query, int hitsPerPage) throws IOException, ParseException {
        return find(defaultFindQueryParser.parse(query), hitsPerPage);
    }

    @Nullable private SearchResult find(org.apache.lucene.search.Query q, int hitsPerPage) throws IOException {

        IndexSearcher searcher = searcher();
        if (searcher != null) {
            TopDocs docs = searcher.search(q, hitsPerPage);
            if (docs.totalHits > 0) {
                return new SearchResult(q, searcher, docs);
            }
        }

        //return new SearchResult(q, null, null); //TODO: return EmptySearchResult;
        return null;
    }


    public List<Lookup.LookupResult> suggest(String qText, int count) throws IOException {
        Lookup suggester = suggester();
        return suggester != null ? suggester.lookup(qText, false, count) : null;
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
        DirectoryReader r = reader();
        return r == null ? 0 : r.maxDoc();
//        int[] size = new int[1];
//        forEach(x -> size[0]++); //HACK
//        return size[0];
    }

    public long now() { return System.currentTimeMillis(); }

    private void commit() {
        if (writing.compareAndSet(false, true)) {
            exe.run(1f, () -> {
                try {

                    if (out.isEmpty())
                        return;

                    IndexWriterConfig writerConf = new IndexWriterConfig(analyzer);
                    writerConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                    writerConf.setRAMBufferSizeMB(1);

                    IndexWriter writer = new IndexWriter(dir, writerConf);

                    DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir);

                    int written = 0;

                    while (!out.isEmpty()) {

                        //long seq = writer.addDocuments(Iterables.transform(drain(out.entrySet()), documenter));

                        Iterator<Map.Entry<String, DObject>> ii = out.entrySet().iterator();
                        while (ii.hasNext()) {
                            Map.Entry<String, DObject> nn = ii.next();
                            ii.remove();
                            writer.updateDocument(new Term(NObject.ID, nn.getKey()),
                                    facetsConfig.build(taxoWriter, nn.getValue().document)
                                    //nn.getValue().document
                            );
                            written++;
                        }

                        writer.commit();
                        taxoWriter.commit();
                    }

                    writer.close();
                    taxoWriter.close();
                    writing.set(false);

                    lastWrite = now();

                    logger.debug("{} indexed", written);

                } catch (IOException e) {
                    writing.set(false);

                    logger.error("indexing error: {}", e);
                }
            });
        }

    }

    @Nullable DObject commit(NObject previous, NObject _next) {

        NObject next = _next;
        if (!onChange.isEmpty()) {
            for (BiFunction<NObject, NObject, NObject> c : onChange) {
                next = c.apply(previous, next);
            }
        }

        if (next == null)
            return null;

        DObject d = DObject.get(next, this);
        String id = d.id();
        out.put(id, d);
        cache.put(id, d);
        commit();
        reindex(previous, d);
        return d;
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



    SpatialSearch<NObject> spaceIfExists(String tag) {
        return spacetime.get(tag);
    }

    SpatialSearch<NObject> space(String tag) {
        return spacetime.computeIfAbsent(tag, (t) -> {
            return new LockingRTree<NObject>(new RTree<NObject>(rectBuilder,
                    2, 8, DEFAULT_SPLIT_TYPE),
                    new ReentrantReadWriteLock());
        });
    }


    @JsonProperty("status") /*@JsonSerialize(as = RawSerializer.class)*/
    @Override
    public String toString() {
        return "{\"" + getClass().getSimpleName() + "\":{" +
                ",\"spacetime\":\"" + spacetime + "\"}}";
    }


    final List<BiFunction<NObject, NObject, NObject>> onChange = new CopyOnWriteArrayList<>();

    public void on(BiFunction<NObject, NObject, NObject> changed) {
        onChange.add(changed);
    }


    final Locker<String> locker = new Locker();


    public void run(String id, Runnable r)  {
        run(id, ()->{
            r.run();
            return null;
        });
    }

    public <X> X run(String id, Supplier<X> r)  {
        Lock l = locker.get(id);

        Throwable thrown = null;
        X result = null;

        l.lock();
        try {
            try {
                result = r.get();
            } catch (Throwable t) {
                thrown = t;
            }
        } finally {
            l.unlock();
        }

        if (thrown!=null) {
            throw new RuntimeException(thrown);
        }

        return result;
    }

    public void addAsync(float pri, @Nullable NObject next) {
        if (next!=null) {
            /*return */exe.run(pri, () -> {
                /*return */add(next);
            });
        } /*else
            return null;*/
    }

    /**
     * returns the resulting (possibly merged/transformed) nobject, which differs from typical put() semantics
     */
    public NObject add(@Nullable NObject n) {
        if (n == null)
            return null;

        return run(n.id(), addProcedure(n));
    }

    public NObject merge(@NotNull MutableNObject n) {
        return run(n.id(), mergeProcedure(n));
    }

    public Supplier<DObject> mergeProcedure(MutableNObject _next) {
        return () -> {

            String id = _next.id();

            DObject previous = get(id);
            if (previous == null) {
                logger.error("{} does not pre-exist for merge with {}", id, _next);
            }
            MutableNObject merged  = new MutableNObject(previous);

            final boolean[] changed = {false};
            _next.forEach((k,v)->{
                Object v0 = merged.get(k);
                if (v0 == null || !v0.equals(v)) {
                    merged.put(k, v);
                    changed[0] = true;
                }
            });

            if (!changed[0])
                return previous; //no-change

            logger.debug("merge {}", id);

            return commit(previous, merged);
        };
    }

    @NotNull
    private Supplier<DObject> addProcedure(@Nullable NObject _next) {
        return () -> {

            String id = _next.id();
            DObject next = DObject.get(_next, this);

            DObject previous = get(id);
            if (previous != null) {
                if (deepEquals(previous.document, next.document))
                    return previous;
            }

            logger.debug("add {}", id);

            return commit(previous, next);
        };
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


            {
                String asv = afi.stringValue();
                String bsv = afi.stringValue();
                if (asv != null && bsv != null && asv.equals(bsv))
                    continue;
                else if (asv != null ^ bsv != null)
                    return false; //one is null the other isnt
            }

            {
                BytesRef ab = afi.binaryValue();
                BytesRef bb = bfi.binaryValue();
                if (ab != null && bb != null && ab.bytesEquals(bb))
                    continue;
                else if (ab != null ^ bb != null)
                    return false; //one is null the other isnt
            }

            {
                String as = afi.toString();
                String bs = bfi.toString();
                if (as.equals(bs))
                    continue;


                //HACK
                as = as.substring(as.indexOf('<'));
                bs = bs.substring(bs.indexOf('<'));
                if (!as.equals(bs))
                    return false;
            }

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

        /* When documents are deleted, gaps are created in the numbering. These are eventually removed as the index evolves through merging. Deleted documents are dropped when segments are merged. A freshly-merged segment thus has no gaps in its numbering. */
        DirectoryReader r = reader();
        if (r == null)
            return;

        int max = r.maxDoc();

        IntStream.range(0, max).parallel().forEach(i -> {
            Document d = null;
            try {
                d = r.document(i);
                if (d!=null)
                    each.accept(DObject.get(d));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

//        for (int i = 0; i < max; i++) {
//            try {
//                Document d = r.document(i);
//                if (d!=null)
//                    each.accept(DObject.get(d));
//            } catch (IOException e) {
//                logger.error("doc: {}", e.getMessage());
//            }
//        }

        try {
            r.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        //long startTime = System.currentTimeMillis();
//        //create the term query object
//        MatchAllDocsQuery query = new MatchAllDocsQuery();
//        //do the search
//        TopDocs hits = null;
//        try {
//            IndexSearcher searcher = searcher();
//            if (searcher == null)
//                return;
//
//            hits = searcher.search(query, Integer.MAX_VALUE);
//            //long endTime = System.currentTimeMillis();
//
////            System.out.println(hits.totalHits +
////                    " documents found. Time :" + (endTime - startTime) + "ms");
//            for (ScoreDoc scoreDoc : hits.scoreDocs) {
//                Document doc = searcher.doc(scoreDoc.doc);
//                if (doc!=null) {
//                    each.accept(DObject.get(doc));
//                }
//            }
//
//            searcher.getIndexReader().close();
//        } catch (IOException e) {
//            logger.error("{}",e);
//        }
    }


    public DObject get(String id) {
        return cache.get(id, (i) -> {
            Document d = the(i);
            if (d!=null)
                return DObject.get(d);
            return null;
        });
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

    public void runLater(Runnable r) {
        runLater(0.5f, r);
    }

    public void runLater(float pri, Runnable r) {
        exe.run(pri, r);
    }

    public void add(Stream<? extends NObject> s) {
        s.forEach(this::add);
    }

    @Deprecated
    public synchronized void sync() {
        int waitDelayMS = 50;
        while (!exe.pq.isEmpty()) {
            try {
                Thread.sleep(waitDelayMS);
            } catch (InterruptedException e) {
                logger.warn("{}", e);
            }
        }
    }

//    public synchronized void sync(float seconds) {
//        //exe.awaitQuiescence(Math.round(seconds * 1000f), MILLISECONDS);
//
//    }


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

///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.apache.lucene.demo.facet;
//
//
//        import java.io.IOException;
//        import java.util.ArrayList;
//        import java.util.List;
//
//        import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
//        import org.apache.lucene.document.Document;
//        import org.apache.lucene.facet.DrillDownQuery;
//        import org.apache.lucene.facet.DrillSideways.DrillSidewaysResult;
//        import org.apache.lucene.facet.DrillSideways;
//        import org.apache.lucene.facet.FacetField;
//        import org.apache.lucene.facet.FacetResult;
//        import org.apache.lucene.facet.Facets;
//        import org.apache.lucene.facet.FacetsCollector;
//        import org.apache.lucene.facet.FacetsConfig;
//        import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
//        import org.apache.lucene.facet.taxonomy.TaxonomyReader;
//        import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
//        import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
//        import org.apache.lucene.index.DirectoryReader;
//        import org.apache.lucene.index.IndexWriter;
//        import org.apache.lucene.index.IndexWriterConfig;
//        import org.apache.lucene.index.IndexWriterConfig.OpenMode;
//        import org.apache.lucene.search.IndexSearcher;
//        import org.apache.lucene.search.MatchAllDocsQuery;
//        import org.apache.lucene.store.Directory;
//        import org.apache.lucene.store.RAMDirectory;
//
///** Shows simple usage of faceted indexing and search. */
//public class SimpleFacetsExample {
//
//    private final Directory indexDir = new RAMDirectory();
//    private final Directory taxoDir = new RAMDirectory();
//    private final FacetsConfig config = new FacetsConfig();
//
//    /** Empty constructor */
//    public SimpleFacetsExample() {
//        config.setHierarchical("Publish Date", true);
//    }
//
//    /** Build the example index. */
//    private void index() throws IOException {
//        IndexWriter indexWriter = new IndexWriter(indexDir, new IndexWriterConfig(
//                new WhitespaceAnalyzer()).setOpenMode(OpenMode.CREATE));
//
//        // Writes facet ords to a separate directory from the main index
//        DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir);
//
//        Document doc = new Document();
//        doc.add(new FacetField("Author", "Bob"));
//        doc.add(new FacetField("Publish Date", "2010", "10", "15"));
//        indexWriter.addDocument(config.build(taxoWriter, doc));
//
//        doc = new Document();
//        doc.add(new FacetField("Author", "Lisa"));
//        doc.add(new FacetField("Publish Date", "2010", "10", "20"));
//        indexWriter.addDocument(config.build(taxoWriter, doc));
//
//        doc = new Document();
//        doc.add(new FacetField("Author", "Lisa"));
//        doc.add(new FacetField("Publish Date", "2012", "1", "1"));
//        indexWriter.addDocument(config.build(taxoWriter, doc));
//
//        doc = new Document();
//        doc.add(new FacetField("Author", "Susan"));
//        doc.add(new FacetField("Publish Date", "2012", "1", "7"));
//        indexWriter.addDocument(config.build(taxoWriter, doc));
//
//        doc = new Document();
//        doc.add(new FacetField("Author", "Frank"));
//        doc.add(new FacetField("Publish Date", "1999", "5", "5"));
//        indexWriter.addDocument(config.build(taxoWriter, doc));
//
//        indexWriter.close();
//        taxoWriter.close();
//    }
//
//    /** User runs a query and counts facets. */
//    private List<FacetResult> facetsWithSearch() throws IOException {
//        DirectoryReader indexReader = DirectoryReader.open(indexDir);
//        IndexSearcher searcher = new IndexSearcher(indexReader);
//        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
//
//        FacetsCollector fc = new FacetsCollector();
//
//        // MatchAllDocsQuery is for "browsing" (counts facets
//        // for all non-deleted docs in the index); normally
//        // you'd use a "normal" query:
//        FacetsCollector.search(searcher, new MatchAllDocsQuery(), 10, fc);
//
//        // Retrieve results
//        List<FacetResult> results = new ArrayList<>();
//
//        // Count both "Publish Date" and "Author" dimensions
//        Facets facets = new FastTaxonomyFacetCounts(taxoReader, config, fc);
//        results.add(facets.getTopChildren(10, "Author"));
//        results.add(facets.getTopChildren(10, "Publish Date"));
//
//        indexReader.close();
//        taxoReader.close();
//
//        return results;
//    }
//
//    /** User runs a query and counts facets only without collecting the matching documents.*/
//    private List<FacetResult> facetsOnly() throws IOException {
//        DirectoryReader indexReader = DirectoryReader.open(indexDir);
//        IndexSearcher searcher = new IndexSearcher(indexReader);
//        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
//
//        FacetsCollector fc = new FacetsCollector();
//
//        // MatchAllDocsQuery is for "browsing" (counts facets
//        // for all non-deleted docs in the index); normally
//        // you'd use a "normal" query:
//        searcher.search(new MatchAllDocsQuery(), fc);
//
//        // Retrieve results
//        List<FacetResult> results = new ArrayList<>();
//
//        // Count both "Publish Date" and "Author" dimensions
//        Facets facets = new FastTaxonomyFacetCounts(taxoReader, config, fc);
//
//        results.add(facets.getTopChildren(10, "Author"));
//        results.add(facets.getTopChildren(10, "Publish Date"));
//
//        indexReader.close();
//        taxoReader.close();
//
//        return results;
//    }
//
//    /** User drills down on 'Publish Date/2010', and we
//     *  return facets for 'Author' */
//    private FacetResult drillDown() throws IOException {
//        DirectoryReader indexReader = DirectoryReader.open(indexDir);
//        IndexSearcher searcher = new IndexSearcher(indexReader);
//        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
//
//        // Passing no baseQuery means we drill down on all
//        // documents ("browse only"):
//        DrillDownQuery q = new DrillDownQuery(config);
//
//        // Now user drills down on Publish Date/2010:
//        q.add("Publish Date", "2010");
//        FacetsCollector fc = new FacetsCollector();
//        FacetsCollector.search(searcher, q, 10, fc);
//
//        // Retrieve results
//        Facets facets = new FastTaxonomyFacetCounts(taxoReader, config, fc);
//        FacetResult result = facets.getTopChildren(10, "Author");
//
//        indexReader.close();
//        taxoReader.close();
//
//        return result;
//    }
//
//    /** User drills down on 'Publish Date/2010', and we
//     *  return facets for both 'Publish Date' and 'Author',
//     *  using DrillSideways. */
//    private List<FacetResult> drillSideways() throws IOException {
//        DirectoryReader indexReader = DirectoryReader.open(indexDir);
//        IndexSearcher searcher = new IndexSearcher(indexReader);
//        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
//
//        // Passing no baseQuery means we drill down on all
//        // documents ("browse only"):
//        DrillDownQuery q = new DrillDownQuery(config);
//
//        // Now user drills down on Publish Date/2010:
//        q.add("Publish Date", "2010");
//
//        DrillSideways ds = new DrillSideways(searcher, config, taxoReader);
//        DrillSidewaysResult result = ds.search(q, 10);
//
//        // Retrieve results
//        List<FacetResult> facets = result.facets.getAllDims(10);
//
//        indexReader.close();
//        taxoReader.close();
//
//        return facets;
//    }
//
//    /** Runs the search example. */
//    public List<FacetResult> runFacetOnly() throws IOException {
//        index();
//        return facetsOnly();
//    }
//
//    /** Runs the search example. */
//    public List<FacetResult> runSearch() throws IOException {
//        index();
//        return facetsWithSearch();
//    }
//
//    /** Runs the drill-down example. */
//    public FacetResult runDrillDown() throws IOException {
//        index();
//        return drillDown();
//    }
//
//    /** Runs the drill-sideways example. */
//    public List<FacetResult> runDrillSideways() throws IOException {
//        index();
//        return drillSideways();
//    }
//
//    /** Runs the search and drill-down examples and prints the results. */
//    public static void main(String[] args) throws Exception {
//        System.out.println("Facet counting example:");
//        System.out.println("-----------------------");
//        SimpleFacetsExample example = new SimpleFacetsExample();
//        List<FacetResult> results1 = example.runFacetOnly();
//        System.out.println("Author: " + results1.get(0));
//        System.out.println("Publish Date: " + results1.get(1));
//
//        System.out.println("Facet counting example (combined facets and search):");
//        System.out.println("-----------------------");
//        List<FacetResult> results = example.runSearch();
//        System.out.println("Author: " + results.get(0));
//        System.out.println("Publish Date: " + results.get(1));
//
//        System.out.println("Facet drill-down example (Publish Date/2010):");
//        System.out.println("---------------------------------------------");
//        System.out.println("Author: " + example.runDrillDown());
//
//        System.out.println("Facet drill-sideways example (Publish Date/2010):");
//        System.out.println("---------------------------------------------");
//        for(FacetResult result : example.runDrillSideways()) {
//            System.out.println(result);
//        }
//    }
//
//}