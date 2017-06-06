package spimedb;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import jcog.Util;
import jcog.event.ArrayTopic;
import jcog.event.Topic;
import jcog.list.FasterList;
import jcog.random.XorShift128PlusRandom;
import jcog.tree.rtree.rect.RectDoubleND;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleRange;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.graph.MapGraph;
import spimedb.graph.travel.BreadthFirstTravel;
import spimedb.graph.travel.CrossComponentTravel;
import spimedb.graph.travel.UnionTravel;
import spimedb.index.DObject;
import spimedb.index.Search;
import spimedb.query.Query;
import spimedb.server.Router;
import spimedb.server.WebIO;
import spimedb.util.Locker;
import spimedb.util.PrioritizedExecutor;

import javax.script.ScriptEngineManager;
import javax.ws.rs.ext.Provider;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;


@Provider
public class SpimeDB {

    final static boolean DEBUG = System.getProperty("debug", "false").equals("true");

    static {
        if (!DEBUG)
            SpimeDB.LOG(Logger.ROOT_LOGGER_NAME, Level.INFO);

        SpimeDB.LOG(Reflections.log, Level.WARN);
        SpimeDB.LOG("logging", Level.WARN);
    }

    public static final String VERSION = "SpimeDB v-0.00";

    @JsonIgnore
    public final static Logger logger = LoggerFactory.getLogger(SpimeDB.class);

    //Tag
    public static final String[] GENERAL = new String[]{""};

    final static Random rng = new XorShift128PlusRandom(System.nanoTime() ^ (-31 * System.currentTimeMillis()));

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
    public SearcherManager searcherMgr;
    private ReaderManager readerMgr;

    protected long lastWrite = 0;

    private Lookup suggester;


    private /*final */ Directory taxoDir;
    private final FacetsConfig facetsConfig = new FacetsConfig();


    public String indexPath;

    final ThreadLocal<QueryParser> defaultFindQueryParser;

    IndexWriter writer;
    final IndexWriterConfig writerConf;

    DirectoryTaxonomyWriter taxoWriter;

    long lastSuggesterCreated = 0;
    long minSuggesterUpdatePeriod = 1000 * 2;


    /**
     * in-memory
     */
    public SpimeDB() throws IOException {
        this(null, new RAMDirectory());
        this.indexPath = null;
        this.taxoDir = new RAMDirectory();
        this.taxoWriter = new DirectoryTaxonomyWriter(taxoDir);
    }

    /**
     * disk
     * TODO must be a directory path, add test
     */
    public SpimeDB(File directoryPath) throws IOException {
        this(directoryPath, FSDirectory.open(directoryPath.toPath()));
        this.indexPath = file.getAbsolutePath();
        this.taxoDir = FSDirectory.open(file.toPath().resolve("taxo"));

        this.taxoWriter = new DirectoryTaxonomyWriter(taxoDir);


        logger.info("index file://{} loaded ({} objects)", indexPath, size());
    }

    private SpimeDB(File file, Directory dir) {

        this.file = file;
        this.dir = dir;
        this.analyzer = new StandardAnalyzer();

        this.facetsConfig.setHierarchical(NObject.ID, true);
        this.facetsConfig.setMultiValued(NObject.ID, false);

        this.facetsConfig.setHierarchical(NObject.TAG, false);
        this.facetsConfig.setMultiValued(NObject.TAG, true);

        final String[] defaultFindFields = new String[]{
                NObject.NAME,
                NObject.DESC,
                NObject.TAG,
                NObject.ID};

        this.defaultFindQueryParser = ThreadLocal.withInitial(()->new MultiFieldQueryParser(defaultFindFields, analyzer, Maps.mutable.with(
                NObject.NAME, 1f,
                NObject.ID, 1f,
                NObject.DESC, 0.25f,
                NObject.TAG, 0.5f
        )));

        writerConf = new IndexWriterConfig(analyzer);
        writerConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        try {
            writer = new IndexWriter(dir, writerConf);
            readerMgr = new ReaderManager(writer);
            searcherMgr = new SearcherManager(writer, new SearcherFactory());
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    @NotNull
    public static String uuidString() {
        return Util.uuid64();
        //return Base64.getEncoder().encodeToString(uuidBytes()).replaceAll("\\/", "`");
        //return BinTxt.encode(uuidBytes());
    }

    public static byte[] uuidBytes() {
        return ArrayUtils.addAll(
                Longs.toByteArray(rng.nextLong()),
                Longs.toByteArray(rng.nextLong())
        );
    }


    @Nullable
    public FacetResult facets(String dimension, int count) {

        return withSearcher((searcher) -> {
            FacetsCollector fc = new FacetsCollector();

            // MatchAllDocsQuery is for "browsing" (counts facets
            // for all non-deleted docs in the index); normally
            // you'd use a "normal" query:
            try {
                searcher.search(new MatchAllDocsQuery(), fc);
                TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);

                Facets facets = new FastTaxonomyFacetCounts(taxoReader, facetsConfig, fc);

                FacetResult result = facets.getTopChildren(count, dimension);

                taxoReader.close();

                return result;
            } catch (IOException e) {
                logger.error("{}", e);
            }

            return null;
        });
    }

    @NotNull
    private Lookup suggester() {

        Lookup suggester = this.suggester;

        if (suggester == null || lastWrite - lastSuggesterCreated > minSuggesterUpdatePeriod) {
            suggester = null; //re-create since it is invalidated

            Lock l = locker.get("_suggester");

            l.lock();
            try {
                if (this.suggester != null)
                    return this.suggester; //created while waiting for the lock

                FreeTextSuggester nextSuggester = new FreeTextSuggester(new SimpleAnalyzer());

                withReader((nameDictReader) -> {

                    DocumentDictionary nameDict = new DocumentDictionary(nameDictReader, NObject.NAME, NObject.NAME);

                    Stopwatch time = Stopwatch.createStarted();

                    try {
                        nextSuggester.build(nameDict);
                    } catch (IOException f) {
                        logger.error("suggester build {}", f);
                    }

                    this.suggester = nextSuggester;

                    lastSuggesterCreated = now();
                    logger.info("suggester built size={} {}ms", nextSuggester.getCount(), time.elapsed(MILLISECONDS));

                    //time.reset();

                    //                    FacetsCollector fc = new FacetsCollector();
                    //                    FacetsCollector.search(new IndexSearcher(nameDictReader), new MatchAllDocsQuery(), Integer.MAX_VALUE,
                    //                            fc
                    //                    );
                    //                    logger.info("facets updated, count={} {}ms", fc., time.elapsed(MILLISECONDS));


                });


                return this.suggester;
            } finally {
                l.unlock();
            }

        } else {
            return suggester;
        }

    }


    private Document the(String id) {
        return withSearcher((searcher) -> {
            TermQuery x = new TermQuery(new Term(NObject.ID, id));
            try {
                TopDocs y = searcher.search(x, firstResultOnly);
                int hits = y.totalHits;
                if (hits > 0) {
                    if (hits > 1) {
                        logger.warn("multiple documents with id={} exist: {}", id, y);
                    }
                    return searcher.doc(y.scoreDocs[0].doc);
                }
            } catch (IOException e) {
                logger.error("{}", e);
            }
            return null;
        });
    }


    public <X> X withSearcher(Function<IndexSearcher, X> c) {
        IndexSearcher s = searcher();
        try {
            X result = c.apply(s);
            return result;
        } finally {
            try {
                searcherMgr.release(s);
            } catch (IOException e) {
                logger.error("{}", e);
            }
        }
    }

    @NotNull
    protected IndexSearcher searcher() {
        try {
            searcherMgr.maybeRefresh();
            return searcherMgr.acquire();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @NotNull
    private DirectoryReader reader() {
        try {
            readerMgr.maybeRefresh();
            return readerMgr.acquire();
            //return DirectoryReader.open(writer); //NRT mode
            //return DirectoryReader.indexExists(dir) ? DirectoryReader.open(dir) : null;
        } catch (IOException e) {
            logger.error("reader open {}", e);
            throw new RuntimeException(e);
            //return null;
        }
    }

    public void withReader(Consumer<DirectoryReader> c) {
        DirectoryReader r = reader();
        try {
            c.accept(r);
        } finally {
            try {
                readerMgr.release(r);
            } catch (IOException e) {
                logger.error("reader close {}", e);
            }
        }
    }

    @NotNull
    public Search find(String query, int hitsPerPage) throws IOException, ParseException {
        return find(parseQuery(query), hitsPerPage);
    }

    private org.apache.lucene.search.Query parseQuery(String query) throws ParseException {
        QueryParser qp = defaultFindQueryParser.get();
        try {
            return qp.parse(query);
        } catch (ParseException e) {
            //HACK remove special characters which lucene may try parse
            query = query.replace('/', ' ');
            return qp.parse(query);
        } catch (Throwable t) {
            throw new ParseException(query + ": " + t.getMessage());
        }
    }

    @NotNull
    private Search find(org.apache.lucene.search.Query q, int hitsPerPage) throws IOException {
        return find(q, hitsPerPage, hitsPerPage);
    }

    @NotNull
    private Search find(org.apache.lucene.search.Query q, int hitsPerPage, int facetCount) throws IOException {

        FacetsCollector fc = new FacetsCollector();

        IndexSearcher searcher = searcher();

        q.rewrite(searcher.getIndexReader());

        TopDocs docs = FacetsCollector.search(searcher, q, hitsPerPage, fc);
        FacetResult facetResults;

        if (docs.totalHits > 0) {

            DirectoryTaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoWriter);

            Facets facets = new FastTaxonomyFacetCounts(taxoReader, facetsConfig, fc);

            facetResults = facets.getTopChildren(facetCount, NObject.TAG);

            taxoReader.close();


        } else {

            docs = null;
            facetResults = null;

        }

        Search ss = new Search(q, searcher, this, docs, facetResults);

        onSearch.emit(ss);

        return ss;

        //return new SearchResult(q, null, null); //TODO: return EmptySearchResult;
    }

    public List<Lookup.LookupResult> suggest(String qText, int count) throws IOException {
        return suggester().lookup(qText, false, count);
    }

    public static void LOG(String l, Level ll) {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(l)).setLevel(ll);
    }

    public static void LOG(Logger log, Level ll) {
        LOG(log.getName(), ll);
    }


    public int size() {
        final int[] size = new int[1];
        withReader((r) -> {
            size[0] = r.maxDoc();
        });
        return size[0];
    }

    public long now() {
        return System.currentTimeMillis();
    }

    private void commit() {
        exe.run(1f, () -> {

            if (out.isEmpty() || !writing.compareAndSet(false, true)) {
                return; //already writing in another thread
            }

            int written = 0, removed = 0;

            try {

                while (!out.isEmpty()) {

                    //long seq = writer.addDocuments(Iterables.transform(drain(out.entrySet()), documenter));

                    Iterator<Map.Entry<String, DObject>> ii = out.entrySet().iterator();
                    while (ii.hasNext()) {
                        Map.Entry<String, DObject> nn = ii.next();
                        ii.remove();

                        String id = nn.getKey();
                        Term key = new Term(NObject.ID, id);

                        DObject val = nn.getValue();
                        if (val != REMOVE) {
                            if (-1 != writer.updateDocument(key,
                                    facetsConfig.build(taxoWriter, val.document))) { //TODO FacetsConfig can be made faster in a batch mode which re-uses data structures
                                written++;
                            }
                        } else {
                            cache.invalidate(id);
                            if (writer.deleteDocuments(key) != -1) {
                                removed++;
                            }
                        }
                    }

                    writer.commit();
                    taxoWriter.commit();
                    lastWrite = now();
                }

            } catch (IOException e) {
                logger.error("indexing error: {}", e);
            } finally {
                writing.set(false);
                //logger.debug("{} indexed, {} removed", written, removed);
            }


        });

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

        pub(next);

        return d;
    }

    private void pub(NObject next) {

        String[] tags = next.tags();
        if (tags == null || tags.length == 0) {
            tags = GENERAL;
        }

        ImmutableSet<String> filters = WebIO.searchResultFull;
        NObject finalNext = new FilteredNObject(next, filters);

        Consumer<Consumer<NObject>> take = (c) -> c.accept(finalNext);
        onTag.emit(tags, take);
        on.forEach(take);
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

    public void add(JsonNode x) {

//        if (x.isArray()) {
//            x.forEach(this::add);
//            return;
//        } else {

        JsonNode inode = x.get("I");
        String I = (inode != null) ? inode.toString() : SpimeDB.uuidString();

        MutableNObject d = new MutableNObject(I)
                .withTags(NObject.TAG_PUBLIC)
                .put("_", x)
                .when(System.currentTimeMillis());

        add(d);
//        }
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

    final Set<NObjectConsumer> on = Sets.newConcurrentHashSet();
    public final Router<String, Consumer<NObject>> onTag = new Router(); //TODO make private

    /**
     * active searches
     */
    public final Topic<Search> onSearch = new ArrayTopic();


    public void on(BiFunction<NObject, NObject, NObject> changed) {
        onChange.add(changed);
    }

    public void on(NObjectConsumer c) {
        update(c, true);
    }

    public void off(NObjectConsumer c) {
        update(c, false);
    }

    private void update(NObjectConsumer c, boolean enable) {
        if (c instanceof NObjectConsumer.OnTag) {
            for (String x : ((NObjectConsumer.OnTag) c).any) {
                if (enable)
                    onTag.on(x, c);
                else
                    onTag.off(x, c);
            }
        } else {
            if (enable)
                on.add(c);
            else
                on.remove(c);
        }
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
            logger.error("{} {} {}", id, r, thrown);
            throw new RuntimeException(thrown);
        }

        return result;
    }

    public void addAsync(float pri, @Nullable NObject next) {
        if (next != null) {
            /*return */
            exe.run(pri, () -> {
                /*return */
                DObject d = add(next);
                //System.out.println(d.document);
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

            //logger.debug("add {}", id);

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

    public void forEach(Consumer<NObject> each) {


        withReader((r) -> {
            int max = r.maxDoc(); // When documents are deleted, gaps are created in the numbering. These are eventually removed as the index evolves through merging. Deleted documents are dropped when segments are merged. A freshly-merged segment thus has no gaps in its numbering.

            IntStream.range(0, max).forEach(i -> {
                Document d = null;
                try {
                    d = r.document(i);
                    if (d != null)
                        each.accept(DObject.get(d));
                } catch (IOException e) {
                    logger.error("forEach {}", e);
                }
            });

        });
    }


    public DObject get(String id) {
        return cache.get(id, (i) -> {
            Document d = the(i);
            return d != null ? DObject.get(d) : null;
        });
    }


    @Nullable
    public Search find(@NotNull Query q) {

        BooleanQuery bq = buildQuery(q);

        q.onStart(this);
        try {
            return find(bq, q.limit);
        } catch (IOException e) {

            logger.error("{}", e);
            return null;
        }
    }

    private BooleanQuery buildQuery(@NotNull Query q) {
        BooleanQuery.Builder bqb = new BooleanQuery.Builder();

        if (q.tagInclude != null) {
            int tags = q.tagInclude.length;
            //if (tags > 1) {
                List<org.apache.lucene.search.Query> tagQueries = new FasterList();
                for (String s : q.tagInclude)
                    tagQueries.add(tagTermQuery(s));

                bqb.add(
                        new DisjunctionMaxQuery(tagQueries, 1f / tags),
                        BooleanClause.Occur.MUST);

            /* } else if (tags == 1) {
                bqb.add(tagTermQuery(q.tagInclude[0]))
            }*/
        }

        if (q.bounds != null && q.bounds.length > 0) {

            List<org.apache.lucene.search.Query> boundQueries = new FasterList();

            for (RectDoubleND x : q.bounds) {
                switch (q.boundsCondition) {
                    case Intersect:
                        boundQueries.add(DoubleRange.newIntersectsQuery(NObject.BOUND, x.min.coord, x.max.coord));
                        break;
                    default:
                        throw new UnsupportedOperationException("TODO");
                }


            }

            bqb.add(new DisjunctionMaxQuery(boundQueries, 0.1f),
                    BooleanClause.Occur.MUST);

        }

        return bqb.build();
    }

    @NotNull
    static private TermQuery tagTermQuery(String tag) {
        return new TermQuery(new Term(NObject.TAG, tag));
    }


    public void runLater(Runnable r) {
        runLater(0.5f, r);
    }

    public void runLater(float pri, Runnable r) {
        exe.run(pri, r);
    }

    public void add(Collection<? extends NObject> s) {
        add(s.stream());
    }

    public void add(Stream<? extends NObject> s) {
        s.forEach(this::add);
    }

    /** returns whether the db has completely synch'd */
    public boolean sync(int waitDelayMS) {
        if (busy()) {
            Util.sleep(waitDelayMS);
            return busy();
        } else {
            return false;
        }
    }

    private boolean busy() {
        return exe.running.get() > 0 || !exe.pq.isEmpty();
    }

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