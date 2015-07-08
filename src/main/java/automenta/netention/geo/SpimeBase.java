package automenta.netention.geo;

import automenta.netention.net.NObject;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.EntityContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;

import java.util.Iterator;

/**
 * http://infinispan.org/docs/8.0.x/user_guide/user_guide.html#_the_infinispan_query_module
 * https://github.com/infinispan/infinispan/blob/master/query/src/test/java/org/infinispan/query/queries/spatial/QuerySpatialTest.java
 */
public class SpimeBase implements Iterable<NObject> {


    private final Cache<String, NObject> cache;
    private final SearchManager search;
    private final EntityContext nobjContext;

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

    /** in-RAM index */
    public static SpimeBase memory() {
        Configuration infinispanConfiguration = new ConfigurationBuilder()
                .indexing()
                .index(Index.ALL)
                .addProperty("default.directory_provider", "ram")
                .addProperty("lucene_version", "LUCENE_CURRENT")
                .build();

        DefaultCacheManager cacheManager = new DefaultCacheManager(infinispanConfiguration);

        SpimeBase db = new SpimeBase(cacheManager.getCache());
        return db;
    }

    public static void main(String[] args) {


        SpimeBase db = SpimeBase.memory();

        //sm.getMassIndexer().start();

        db.put(new NObject(null, "Spatial NObject").where(0.75f, 0.66f));
        db.put(new NObject(null, "Temporal NObject").now());


        db.forEach( x -> System.out.println(x) );


        Query c = db.find().keyword().onField("name").matching("Spatial NObject").createQuery();
        CacheQuery cq = db.find(c);

        System.out.println(cq.list());
        System.out.println(cq.getResultSize());


    }

    public QueryBuilder find() {
        return nobjContext.get();
    }

    public CacheQuery find(Query q) {
        return search.getQuery(q, NObject.class);
    }

    public NObject put(final NObject d) {
        return cache.put(d.getId(), d);
    }

    @Override
    public Iterator<NObject> iterator() {
        return cache.values().iterator();
    }
}
