//package spimedb;
//
//import org.junit.Test;
//import org.neo4j.graphdb.GraphDatabaseService;
//import org.neo4j.graphdb.Label;
//import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Transaction;
//import org.neo4j.graphdb.factory.GraphDatabaseFactory;
//import org.neo4j.graphdb.factory.GraphDatabaseSettings;
//import org.neo4j.test.ImpermanentGraphDatabase;
//
//import java.io.File;
//
///**
// * Created by me on 1/14/17.
// */
//public class Neo4JTest {
//
//    public static class Neo {
//
//        public static GraphDatabaseService get() {
//            //return new TestGraphDatabaseFactory().newImpermanentDatabase();
//            return new ImpermanentGraphDatabase(new File("/var/tmp/neo"));
//        }
//
//        public static GraphDatabaseService get(String path) {
//            GraphDatabaseService graphDb;
//            graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(path))
//                    .setConfig(GraphDatabaseSettings.auth_enabled, "false")
//                    .setConfig(GraphDatabaseSettings.store_internal_log_level, "WARN")
////                    .setConfig(GraphDatabaseSettings.pagecache_memory, "512M")
////                    .setConfig(GraphDatabaseSettings.string_block_size, "60")
////                    .setConfig(GraphDatabaseSettings.array_block_size, "300")
//                    .newGraphDatabase();
//            Runtime.getRuntime().addShutdownHook(new Thread(graphDb::shutdown));
//            return graphDb;
//        }
//    }
//
//    @Test
//    public void testNeo1() {
//        GraphDatabaseService m =
//                Neo.get();
//                //MyNeo.get("/tmp/neo1");
//
//
//
//        Node r = null;
//        try ( Transaction tx = m.beginTx() ) {
//            r = m.createNode(Label.label("tag1"));
//            //n = graphDb.createNode();
//            r.setProperty( "name", "NAMED" );
//            tx.success();
//        }
//
//        System.out.println(r);
//        System.out.println(m);
//        try ( Transaction tx = m.beginTx() ) {
//            m.getAllNodes().forEach(System.out::println);
//            tx.success();
//        }
//    }
//}
