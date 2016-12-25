package spimedb.impl;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.stats.Stats;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.index.graph.MapGraph;
import spimedb.index.graph.VertexContainer;

import java.util.Set;

/**
 * Infinispan Impl
 * http://infinispan.org/docs/9.0.x/user_guide/user_guide.html
 */
public class InfiniSpimeDB {

    public static SpimeDB get(String path) {

        GlobalConfiguration global = new GlobalConfigurationBuilder()
                .serialization()
//                .addAdvancedExternalizer(new PermanentConceptExternalizer())
//                .addAdvancedExternalizer(new ConceptExternalizer())
                .build();

        Configuration infinispanConfiguration = new ConfigurationBuilder()
                .unsafe()
                //.versioning().disable()
                .storeAsBinary().storeKeysAsBinary(true).storeValuesAsBinary(true)
                .persistence()
                //.passivation(true)
                .addSingleFileStore().location(path)
                //cb.locking().concurrencyLevel(1);
                //cb.customInterceptors().addInterceptor();
                .jmxStatistics().disable()
                .build();

        DefaultCacheManager cm = new DefaultCacheManager(global, infinispanConfiguration);

//        this.conceptsLocal = new DecoratedCache<>(
//                concepts.getAdvancedCache(),
//                Flag.CACHE_MODE_LOCAL, /*Flag.SKIP_LOCKING,*/ Flag.SKIP_OWNERSHIP_CHECK,
//                Flag.SKIP_REMOTE_LOOKUP);
//        this.conceptsLocalNoResult = conceptsLocal.withFlags(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP);


        Cache<String, VertexContainer<NObject, Pair<OctSpimeDB.OpEdge, Twin<String>>>> vertex =
                cm.getCache("vertex");

        Cache<Pair<OctSpimeDB.OpEdge, Twin<String>>, Twin<String>> edge =
                cm.getCache("edge");

        enableStats(vertex);
        enableStats(edge);

        OctSpimeDB db = new OctSpimeDB(new MapGraph<String, NObject, Pair<OctSpimeDB.OpEdge, Twin<String>>>(vertex, edge) {


            @Override
            protected Set<Pair<OctSpimeDB.OpEdge, Twin<String>>> newEdgeSet() {
                return new UnifiedSet<>(0);
            }

            @Override
            protected NObject newBlankVertex(String s) {
                return new NObject(s);
            }

            @Override
            public String toString() {
                return cm.toString() + ": " +
                        //cm.getCacheManagerStatus() +
                        " vertices=" + vertex.size() + "," + statString(vertex) +
                        " edges=" + edges.size() + "," + statString(edge)
                        ;
            }
        });

        return db;
    }

    private static void enableStats(Cache vertex) {
        vertex.getAdvancedCache().getStats().setStatisticsEnabled(true);
    }

    private static String statString(Cache c) {
        Stats s = c.getAdvancedCache().getStats();

        return  "hits=" + Math.round((100f * s.getHits() / s.getRetrievals())) + "%" +
               " read=" + s.getAverageReadTime() + "ms" +
               " write=" + s.getAverageReadTime() + "ms"
                ;
    }
}
