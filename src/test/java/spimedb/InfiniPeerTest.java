//package automenta.netention;
//
//import org.junit.Test;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
///**
// * Created by me on 6/7/15.
// */
//public class InfiniPeerTest {
//
//
//    @Test
//    public void testInfiniPeerCache() {
//
//        /*GlobalConfiguration globalConfig = new GlobalConfigurationBuilder()
//                .serialization()
//                .addAdvancedExternalizer(998, new PersonExternalizer())
//                .addAdvancedExternalizer(999, new AddressExternalizer())
//                .build();*/
//
//        /*Configuration config = new ConfigurationBuilder()
//                .persistence().passivation(false)
//                .addSingleFileStore().location("/tmp").async().enable()
//                .preload(false).shared(false).threadPoolSize(20).build();*/
//
//
//
//        InfiniPeer cacheMan = InfiniPeer.tmp();
//
//        final Cache<Object, Object> cache = cacheMan.the("abc", true);
//
//
//        // Add a entry
//        cache.put("key", "value");
//
//        //cache.putForExternalRead(UUID.randomUUID().toString(), cacheMan.getAddress());
//        //cache.put(UUID.randomUUID().toString(), cacheMan.getAddress());
//
//// Validate the entry is now in the cache
//        assertEquals(1, cache.size());
//        assertTrue(cache.containsKey("key"));
//// Remove the entry from the cache
//        Object v = cache.remove("key");
//// Validate the entry is no longer in the cache
//        assertEquals("value", v);
//
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        /*
//
//        //synchronize for display purposes
//        synchronized(cache) {
//            cacheMan.print(cache, System.out);
//        }
//        */
//
//    }
//
//
//}
