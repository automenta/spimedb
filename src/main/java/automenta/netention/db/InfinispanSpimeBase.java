package automenta.netention.db;

import automenta.netention.NObject;
import automenta.netention.geo.SpimeBase;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.*;
import org.hibernate.search.stat.Statistics;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.FetchOptions;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * http://infinispan.org/docs/8.0.x/user_guide/user_guide.html#_the_infinispan_query_module
 * https://github.com/infinispan/infinispan/blob/master/query/src/test/java/org/infinispan/query/queries/spatial/QuerySpatialTest.java
 */
public class InfinispanSpimeBase implements SpimeBase {

    public static final Logger log = LoggerFactory.getLogger(InfinispanSpimeBase.class);


    private final Cache<String, NObject> obj;

    /**
     * extensional inheritance -- parent/child containment relationships
     */
    public final Cache<String /* parent */, Collection<String> /* children */> ext;

    public final SearchManager objSearch;
    private final EntityContext nobjContext;

    public InfinispanSpimeBase(CacheContainer cm, String id) {


        this.obj = cm.getCache(id + "_obj");
        this.objSearch = Search.getSearchManager(obj);

        this.ext = cm.getCache(id + "_ext");

        this.nobjContext = objSearch.buildQueryBuilderForClass(NObject.class);

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
        //https://github.com/tsykora/infinispan-odata-server/blob/master/src/main/resources/indexing-perf.xml#L82
        cb.indexing()
                .index(Index.ALL)
                .addProperty("default.directory_provider", directoryProvider)
                .addProperty("lucene_version", "LUCENE_CURRENT")
                .addProperty("lucene_version", "LUCENE_CURRENT")
                //<!-- Supporting exclusive index usage will require lock cleanup on crashed nodes to be implemented -->
                .addProperty("hibernate.search.default.exclusive_index_use", "false")
                .addProperty("default.chunk_size", "128000")
                .addProperty("default.indexwriter.merge_factor", "30")
                .addProperty("default.indexwriter.merge_max_size", "1024")
                .addProperty("default.indexwriter.ram_buffer_size", "256");
                //        <!-- Write indexes in Infinispan -->
                //        <property name="default.chunk_size" value="128000" />
                //
                //        <!-- The default is 10, but we don't want to waste many cycles in merging
                //        (tune for writes at cost of reader fragmentation) -->
                //        <property name="default.indexwriter.merge_factor" value="30" />
                //
                //        <!-- Never create segments larger than 1GB -->
                //        <property name="default.indexwriter.merge_max_size" value="1024" />
                //
                //        <!-- IndexWriter flush buffer size in MB -->
                //        <property name="default.indexwriter.ram_buffer_size" value="256" />
                //
                //        <!-- Enable sharding on writers
                //                <property name="default.sharding_strategy.nbr_of_shards" value="6" /> -->
                //        ;


        /*
        hibernate.search.Animals.2.indexwriter.max_merge_docs = 10
        hibernate.search.Animals.2.indexwriter.merge_factor = 20
        hibernate.search.Animals.2.indexwriter.term_index_interval = default
        hibernate.search.default.indexwriter.max_merge_docs = 100
        hibernate.search.default.indexwriter.ram_buffer_size = 64
         */
        return cb;
    }

    public static InfinispanSpimeBase disk(String diskPath, int maxEntries) {
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

    /**
     * in-RAM index
     */
    public static SpimeBase memory() {

        ConfigurationBuilder c = new ConfigurationBuilder();


        return newSpimeBase(c);
    }

    public static InfinispanSpimeBase newSpimeBase(ConfigurationBuilder c) {

        withIndexing(c);

        final DefaultCacheManager cacheManager = new DefaultCacheManager(c.build());


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            cacheManager.stop();
        }));

        InfinispanSpimeBase db = new InfinispanSpimeBase(cacheManager, "");

        log.info("Created: " + db + " with " + c);


        return db;
    }


    public QueryBuilder find() {
        return nobjContext.get();
    }

    public CacheQuery find(Query q) {
        return objSearch.getQuery(q, NObject.class);
    }

    public Statistics getStatistics() {
        return objSearch.getStatistics();
    }

    @Override
    public NObject put(final NObject d) {
        NObject removed = obj.put(d.getId(), d);

        //if (removed == null || !removed.inside().equals(d.inside())) extensionality(d);

        //TODO remove extensionality of what is removed if different

        String currInside = d.inside();
        String prevInside;
        if (removed != null) {
            prevInside = removed.inside();
        } else {
            prevInside = null;
        }

        if (currInside != null) {
            if (currInside.equals(prevInside)) {
                //extensionality unchanged
            } else {
                if (prevInside != null)
                    unchild(d.getId(), prevInside);
                child(d.getId(), currInside);
            }
        } else {
            if (prevInside != null) {
                unchild(d.getId(), prevInside);
            } else {
                //still no parent
            }
            root(d.getId());
        }


        return removed;
    }

    protected void root(String rootID) {
        ext.put(rootID, new ArrayList(0));
    }

    protected boolean child(String childID, String parentID) {
        Collection<String> e = ext.get(parentID);
        if (e == null) {
            e = new HashSet();
        }
        else if (e.size() > 0 && (!(e instanceof Set))) {
            //allow list if e.size() == 1, but if grows > 1, convert to set
            e = new HashSet(e);
        }
        if (e.add(childID)) {
            ext.put(parentID, e);
            return true;
        }
        return false;
    }

    protected boolean unchild(String childID, String parentID) {
        Collection<String> e = ext.get(parentID);
        if (e != null) {
            if (e.remove(childID)) {
                ext.put(parentID, e);
                return true;
            }
        }
        return false;
    }

    /*
    public void putFast(final NObject d) {

        cache.getAdvancedCache()
                .withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.SKIP_CACHE_LOAD)
                .putAsync(d.getId(), d);

        extensionality(d);
    }*/



    @Override
    public Iterator<NObject> iterator() {
        return obj.values().iterator();
    }

    @Override
    public boolean isEmpty() {
        return obj.isEmpty();
    }

    @Override
    public int size() {
        return obj.size();
    }


    @Override
    public NObject get(String nobjectID) {
        return obj.get(nobjectID);
    }

    @Override
    public Collection<String> getInsides(String nobjectID) {
        return ext.get(nobjectID);
    }



    @Override
    public Iterator get(double lat, double lon, double radMeters, int maxResults) {

        //Bounds
        //SpatialMatchingContext whereQuery = base.find().spatial().onField("nobject");
        SpatialContext whereQuery = objSearch.buildQueryBuilderForClass(NObject.class).get().spatial();


        SpatialTermination qb = whereQuery.
                within(radMeters / 1000.0, Unit.KM)
                .ofLatitude(lat)
                .andLongitude(lon);


        if (qb != null) {
            Query c = qb.createQuery();
            return this.find(c).iterator(new FetchOptions().fetchMode(FetchOptions.FetchMode.LAZY).fetchSize(maxResults));
        }

        return null;
    }
}


