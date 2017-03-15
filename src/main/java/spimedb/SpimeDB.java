package spimedb;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Stopwatch;
import jcog.list.FasterList;
import jcog.tree.rtree.rect.RectDoubleND;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
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
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.surround.query.OrQuery;
import org.apache.lucene.queryparser.xml.builders.DisjunctionMaxQueryBuilder;
import org.apache.lucene.search.*;
import org.apache.lucene.search.suggest.DocumentDictionary;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.FreeTextSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;
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
import spimedb.query.Query;
import spimedb.util.Locker;
import spimedb.util.PrioritizedExecutor;
import spimedb.util.datatypes.DoubleRange;

import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
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


public class SpimeDB {


    public static final String VERSION = "SpimeDB v-0.00";

    @JsonIgnore
    public final static Logger logger = LoggerFactory.getLogger(SpimeDB.class);

    public final PrioritizedExecutor exe = new PrioritizedExecutor(
            Math.max(2, 1 + Runtime.getRuntime().availableProcessors())
    );

    /**
     * default location of file resources if unspecified
     */
    public static final String TMP_SPIMEDB_CACHE_PATH = "/tmp/spimedb.cache"; //TODO use correct /tmp location per platform (ex: Windows will need somewhere else)


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

    final static DObject REMOVE = new DObject() {

    };

    private final Map<String, DObject> out = new ConcurrentHashMap<>(1024);
    private final Cache<String, DObject> cache =
            Caffeine.newBuilder().maximumSize(NObjectCacheSize).build();

    private final AtomicBoolean writing = new AtomicBoolean(false);
    private final StandardAnalyzer analyzer;
    public final File file;

    protected long lastWrite = 0;

    private Lookup suggester;

    private DocumentDictionary nameDict;
    private DirectoryReader nameDictReader;

    final static String[] ROOT = new String[]{""};


    private /*final */ Directory taxoDir;
    private final FacetsConfig facetsConfig = new FacetsConfig();


    public String indexPath;

    final QueryParser defaultFindQueryParser;


    /**
     * in-memory
     */
    public SpimeDB() throws IOException {
        this(null, new RAMDirectory());
        this.indexPath = null;
        this.taxoDir = new RAMDirectory();

    }

    /**
     * disk
     */
    public SpimeDB(String path) throws IOException {
        this(new File(path), FSDirectory.open(new File(path).toPath()));
        this.indexPath = file.getAbsolutePath();
        this.taxoDir = FSDirectory.open(file.toPath().resolve("taxo"));
        logger.info("index ready: file://{}", indexPath);
    }

    private SpimeDB(File file, Directory dir)  {

        this.file = file;
        this.dir = dir;
        this.analyzer = new StandardAnalyzer();

        this.facetsConfig.setHierarchical(NObject.ID, true);
        this.facetsConfig.setMultiValued(NObject.TAG, true);

        final String[] defaultFindFields = new String[]{NObject.NAME, NObject.DESC};
        final Map<String, Float> defaultFindFieldStrengths = Maps.mutable.with(
                NObject.NAME, 1f,
                NObject.ID, 0.75f,
                NObject.DESC, 0.5f,
                NObject.TAG, 0.25f
        );
        this.defaultFindQueryParser = new MultiFieldQueryParser(defaultFindFields, analyzer, defaultFindFieldStrengths);

        logger.info("{} objects loaded", size());
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
        if (searcher == null)
            return null;


        FacetsCollector fc = new FacetsCollector();

        // MatchAllDocsQuery is for "browsing" (counts facets
        // for all non-deleted docs in the index); normally
        // you'd use a "normal" query:
        searcher.search(new MatchAllDocsQuery(), fc);


        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);

        Facets facets = new FastTaxonomyFacetCounts(taxoReader, facetsConfig, fc);

        FacetResult result = facets.getTopChildren(count, dimension);

        taxoReader.close();
        searcher.getIndexReader().close();

        return result;
    }

    @Nullable
    private Lookup suggester() {
        synchronized (dir) {
            if (lastWrite - lastSuggesterCreated > minSuggesterUpdatePeriod) {
                suggester = null; //re-create since it is invalidated
            }

            if (suggester == null) {
                if (nameDict == null) {
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

    @Nullable
    protected IndexSearcher searcher() {
        DirectoryReader r = reader();
        return r != null ? new IndexSearcher(r) : null;
    }

    @Nullable
    private DirectoryReader reader() {
        try {
            return DirectoryReader.indexExists(dir) ? DirectoryReader.open(dir) : null;
        } catch (IOException e) {
            logger.error("index reader: {}", e);
            throw new RuntimeException(e);
            //return null;
        }
    }

    @Nullable
    public SearchResult find(String query, int hitsPerPage) throws IOException, ParseException {
        return find(defaultFindQueryParser.parse(query), hitsPerPage);
    }

    @Nullable
    private SearchResult find(org.apache.lucene.search.Query q, int hitsPerPage) throws IOException {

        IndexSearcher searcher = searcher();
        if (searcher != null) {

            FacetsCollector fc = new FacetsCollector();

            FacetsCollector.search(searcher, q, hitsPerPage, fc);
            //searcher.search(q, fc);

            TopDocs docs = searcher.search(q, hitsPerPage);
            if (docs.totalHits > 0) {

                int facetCount = hitsPerPage; //DEFAULT

                TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);

                Facets facets = new FastTaxonomyFacetCounts(taxoReader, facetsConfig, fc);

                FacetResult facetResults = facets.getTopChildren(facetCount, NObject.TAG);

                taxoReader.close();

                return new SearchResult(q, searcher, docs, facetResults);
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



    public Set<String> tags() {
        return Collections.emptySet();
    }

    public Iterator<String> roots() {
        return Collections.emptyIterator();

    }

    public int size() {
        DirectoryReader r = reader();
        return r == null ? 0 : r.maxDoc();
//        int[] size = new int[1];
//        forEach(x -> size[0]++); //HACK
//        return size[0];
    }

    public long now() {
        return System.currentTimeMillis();
    }

    private void commit() {
        if (writing.compareAndSet(false, true)) {
            exe.run(1f, () -> {

                if (out.isEmpty())
                    return;

                try {

                    IndexWriterConfig writerConf = new IndexWriterConfig(analyzer);
                    writerConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

                    IndexWriter writer = new IndexWriter(dir, writerConf);

                    DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir);

                    int written = 0, removed = 0;

                    while (!out.isEmpty()) {

                        //long seq = writer.addDocuments(Iterables.transform(drain(out.entrySet()), documenter));

                        Iterator<Map.Entry<String, DObject>> ii = out.entrySet().iterator();
                        while (ii.hasNext()) {
                            Map.Entry<String, DObject> nn = ii.next();
                            ii.remove();
                            DObject val = nn.getValue();

                            String id = nn.getKey();
                            Term key = new Term(NObject.ID, id);
                            if (val!=REMOVE) {
                                writer.updateDocument(key,
                                    facetsConfig.build(taxoWriter, val.document)
                                );
                                written++;
                            } else {
                                writer.deleteDocuments(key);
                                cache.invalidate(id);
                                removed++;
                            }
                        }

                        writer.commit();
                        taxoWriter.commit();
                    }


                    writer.close();
                    taxoWriter.close();


                    logger.debug("{} indexed, {} removed", written, removed);

                } catch (IOException e) {

                    logger.error("indexing error: {}", e);
                }

                lastWrite = now();

                writing.set(false);

            });
        }

    }

    @Nullable
    DObject commit(NObject previous, NObject _next) {

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
        return d;
    }


    /**
     * deletes the index and optionally triggers a rebuild
     */
    public synchronized void clear(boolean rebuild) {
        exe.pq.clear();
        try {
            exe.exe.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        out.clear();
        cache.invalidateAll();

        if (indexPath != null) {
            recursiveDelete(new File(indexPath));
        }

    }

    static void recursiveDelete(File dir) {
        //to end the recursive loop
        if (!dir.exists())
            return;

        //if directory, go inside and call recursively
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                //call recursively
                recursiveDelete(f);
            }
        }
        //call delete to delete files and empty directory
        dir.delete();
    }

    public void remove(String id) {
        out.put(id, REMOVE);
        commit();
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





    @JsonProperty("status") /*@JsonSerialize(as = RawSerializer.class)*/
    @Override
    public String toString() {
        return "{\"" + getClass().getSimpleName() + "\"}";
    }


    final List<BiFunction<NObject, NObject, NObject>> onChange = new CopyOnWriteArrayList<>();

    public void on(BiFunction<NObject, NObject, NObject> changed) {
        onChange.add(changed);
    }


    final Locker<String> locker = new Locker();


    public void run(String id, Runnable r) {
        run(id, () -> {
            r.run();
            return null;
        });
    }

    public <X> X run(String id, Supplier<X> r) {
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

        if (thrown != null) {
            throw new RuntimeException(thrown);
        }

        return result;
    }

    public void addAsync(float pri, @Nullable NObject next) {
        if (next != null) {
            /*return */
            exe.run(pri, () -> {
                /*return */
                add(next);
            });
        } /*else
            return null;*/
    }

    /**
     * returns the resulting (possibly merged/transformed) nobject, which differs from typical put() semantics
     */
    public DObject add(@Nullable NObject n) {
        if (n == null)
            return null;

        return run(n.id(), addProcedure(n));
    }

    public NObject merge(@NotNull MutableNObject n) {
        return run(n.id(), mergeProcedure(n));
    }

    public Supplier<DObject> mergeProcedure(MutableNObject next) {
        return () -> {

            String id = next.id();

            DObject previous = get(id);
            if (previous == null) {
                logger.error("{} does not pre-exist for merge with {}", id, next);
            }
            MutableNObject merged = new MutableNObject(previous);

            final boolean[] changed = {false};
            next.forEach((k, v) -> {
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
        if (bf.size() != size)
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
                int ai = as.indexOf('<');
                as = ai != -1 ? as.substring(ai) : "";
                int bi = bs.indexOf('<');
                bs = bi != -1 ? bs.substring(bi) : "";
                if (!as.equals(bs))
                    return false;
            }

        }
        return true;
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
    public void forEach(Consumer<NObject> each) {

        /* When documents are deleted, gaps are created in the numbering. These are eventually removed as the index evolves through merging. Deleted documents are dropped when segments are merged. A freshly-merged segment thus has no gaps in its numbering. */
        DirectoryReader r = reader();
        if (r == null)
            return;

        int max = r.maxDoc();

        IntStream.range(0, max).parallel().forEach(i -> {
            Document d = null;
            try {
                d = r.document(i);
                if (d != null)
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
            if (d != null)
                return DObject.get(d);
            return null;
        });
    }


    @Nullable public SearchResult get(@NotNull Query q) {
        q.onStart();

        Predicate<NObject> each = q.each;

        IndexSearcher s = searcher();
        if (s == null)
            return null;

        try {
            if (q.bounds != null && q.bounds.length > 0) {
                BooleanQuery.Builder bqb = new BooleanQuery.Builder();


                List<QueryNode> nodes = new FasterList();
                for (RectDoubleND x : q.bounds) {
                    org.apache.lucene.search.Query subQuery;
                    switch (q.boundsCondition) {
                        case Intersect:
                            subQuery = DoubleRangeField.newIntersectsQuery(NObject.BOUND, x.min.coord, x.max.coord);
                            break;
                        default:
                            q.onEnd();
                            throw new UnsupportedOperationException("TODO");
                    }

                    bqb.add(subQuery, BooleanClause.Occur.SHOULD);
                }

                try {
                    SearchResult result = find(bqb.build(), q.limit);
                    result.forEach((d, score) -> {
                        return q.each.test(DObject.get(d));
                    });
                    q.onEnd();
                    return result;
                } catch (IOException e) {
                    q.onEnd();
                    logger.error("{}", e);
                }
            }

            throw new UnsupportedOperationException("empty query");
        } finally {
            try {
                searcher().getIndexReader().close();
            } catch (IOException e) { }
        }



//        Iterable<String> include = Collections.emptyList(); //tagsAndSubtags(q.include);
//        for (String t : include) {
//            SpatialSearch<NObject> s = spaceIfExists(t);
//            if (s != null && !s.isEmpty()) {
//                if (q.bounds != null && q.bounds.length > 0) {
//                    for (RectND x : q.bounds) {
//                        switch (q.boundsCondition) {
//                            case Contain:
//                                if (!s.containing(x, each))
//                                    break;
//                                break;
//                            case Intersect:
//                                if (!s.intersecting(x, each))
//                                    break;
//                                break;
//                        }
//                    }
//                } else {
//                    if (!s.intersecting(RectND.ALL_4, each)) //iterate all items
//                        break;
//                }
//            }
        //}

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
            return Collections.emptyList(); //new SubTags(graph, parentTags);
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
        while (!exe.pq.isEmpty() || exe.running.get() > 0) {
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


    @Deprecated public NObject graphed(String id) {
        NObject n = get(id);
        return n;
//        if (n != null)
//            return graphed(n);
//        return null;
    }

//    public GraphedNObject graphed(NObject n) {
//        if ((n instanceof GraphedNObject) && (((GraphedNObject) n).graph == graph))
//            return (GraphedNObject) n; //already wrapped
//
//        return new GraphedNObject(this.graph, n);
//    }


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