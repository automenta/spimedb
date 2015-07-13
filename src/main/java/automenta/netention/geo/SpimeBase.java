package automenta.netention.geo;

import automenta.netention.net.NObject;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.stat.Statistics;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.context.Flag;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * http://infinispan.org/docs/8.0.x/user_guide/user_guide.html#_the_infinispan_query_module
 * https://github.com/infinispan/infinispan/blob/master/query/src/test/java/org/infinispan/query/queries/spatial/QuerySpatialTest.java
 */
public class SpimeBase implements Iterable<NObject> {

    public static final Logger log = LoggerFactory.getLogger(SpimeBase.class);

    private final Cache<String, NObject> cache;

    public final SearchManager search;
    private final EntityContext nobjContext;

    public SpimeBase(CacheContainer cm, String id) {
        this(cm.getCache(id));
    }

    public SpimeBase(CacheContainer cm) {
        this(cm.getCache());
    }

    public SpimeBase(Cache<String, NObject> cache) {

        this.cache = cache;



        this.search = Search.getSearchManager(cache);
        this.nobjContext = search.buildQueryBuilderForClass(NObject.class);

    }

    public static NObject newNObject() {
        return new NObject();
    }
    public static NObject newNObject(String id) {
        return new NObject(id);
    }

    /*public static ConfigurationBuilder withIndexing(ConfigurationBuilder cb) {
        return withIndexing(cb, "ram");
    }*/
    public static ConfigurationBuilder withIndexing(ConfigurationBuilder cb) {
//        Cache cache = cb.
//Directory indexDir = DirectoryBuilder.newDirectoryInstance(cache, cache, cache, indexName)
//                                     .create();
        return withIndexing(cb, "infinispan");
    }



    public static ConfigurationBuilder withIndexing(ConfigurationBuilder cb, String directoryProvider) {
        cb.indexing()
                .index(Index.ALL)
                .addProperty("default.directory_provider", directoryProvider)
                .addProperty("lucene_version", "LUCENE_CURRENT");


        /*
        hibernate.search.Animals.2.indexwriter.max_merge_docs = 10
        hibernate.search.Animals.2.indexwriter.merge_factor = 20
        hibernate.search.Animals.2.indexwriter.term_index_interval = default
        hibernate.search.default.indexwriter.max_merge_docs = 100
        hibernate.search.default.indexwriter.ram_buffer_size = 64
         */
        return cb;
    }

    public static SpimeBase disk(String diskPath, int maxEntries) {
        ConfigurationBuilder c = new ConfigurationBuilder();

        c.invocationBatching().enable();

        c.persistence()
                .addSingleFileStore()
                .location(diskPath)
                .maxEntries(maxEntries)
                .fetchPersistentState(true)
                .ignoreModifications(false)
                .preload(true)

                .async().enable()
                .versioning().disable()


                .unsafe();

                //.purgeOnStartup(true)
                /*.clustering()
                        //.cacheMode(CacheMode.DIST_SYNC)
                .cacheMode(CacheMode.DIST_SYNC)
                .sync()
                .l1().lifespan(25000L)
                .hash().numOwners(3);*/


        return newSpimeBase(c);
    }

    /** in-RAM index */
    public static SpimeBase memory() {

        ConfigurationBuilder c = new ConfigurationBuilder();


        return newSpimeBase(c);
    }

    public static SpimeBase newSpimeBase(ConfigurationBuilder c) {

        withIndexing(c);

        final DefaultCacheManager cacheManager = new DefaultCacheManager(c.build());


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            cacheManager.stop();
        }));

        SpimeBase db = new SpimeBase(cacheManager.getCache());

        log.info("Created: " + db + " with " + c);


        return db;
    }


//    public static void main(String[] args) {
//
//
//        SpimeBase db = SpimeBase.memory();
//
//        //sm.getMassIndexer().start();
//
//        db.put(new NObject(null, "Spatial NObject").where(0.75f, 0.66f));
//        db.put(new NObject(null, "Temporal NObject").now());
//
//
//        db.forEach( x -> System.out.println(x) );
//
//
//        Query c = db.find().keyword().onField("name").matching("Spatial NObject").createQuery();
//        CacheQuery cq = db.find(c);
//
//        System.out.println(cq.list());
//        System.out.println(cq.getResultSize());
//
//
//    }

    public QueryBuilder find() {
        return nobjContext.get();
    }

    public CacheQuery find(Query q) {
        return search.getQuery(q, NObject.class);
    }

    public Statistics getStatistics() {
        return search.getStatistics();
    }

    public NObject put(final NObject d) {
        NObject removed = cache.put(d.getId(), d);

        //if (removed == null || !removed.inside().equals(d.inside())) extensionality(d);

        //TODO remove extensionality of what is removed if different

        return removed;
    }

    public void putFast(final NObject d) {
        cache.getAdvancedCache()
                .withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.SKIP_CACHE_LOAD)
                .putAsync(d.getId(), d);

        extensionality(d);
    }

    private void extensionality(NObject d) {
        String p = d.inside();
        if (p==null) return;

        NObject n = get(p);
        if (n == null)
            return; //err

        n.setOutside("TRUE");
        cache.replaceAsync(p, n);

    }


    @Override
    public Iterator<NObject> iterator() {
        return cache.values().iterator();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    public int size() {
        return cache.size();
    }


    public NObject get(String nobjectID) {
        return cache.get(nobjectID);
    }
}
