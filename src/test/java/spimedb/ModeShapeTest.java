//package spimedb;
//
//import ch.qos.logback.classic.Level;
//import org.modeshape.common.collection.Problems;
//import org.modeshape.jcr.ModeShapeEngine;
//import org.modeshape.jcr.RepositoryConfiguration;
//
//import javax.jcr.Node;
//import javax.jcr.Repository;
//import javax.jcr.RepositoryException;
//import javax.jcr.Session;
//
///**
// * https://github.com/ModeShape/modeshape-examples/blob/master/modeshape-embedded-example/src/main/java/org/modeshape/example/embedded/ModeShapeExample.java
// */
//public class ModeShapeTest {
//
//    static {
//        ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("org.modeshape")).setLevel(Level.WARN);
//    }
//
//    public static void main(String[] args) {
//
//        // Create and start the engine ...
//        ModeShapeEngine engine = new ModeShapeEngine();
//
//        engine.start();
//
//        // Load the configuration for a repository via the classloader (can also use path to a file)...
//        Repository repository = null;
//        String repositoryName = null;
//        try
//
//        {
//
////            "path" : "${application.home.location}/data"
///*
//    "storage" : {
//        "persistence" : {
//            "type" : "db",
//            "connectionUrl": "jdbc:h2:file:./target/clustered/db;AUTO_SERVER=TRUE",
//            "driver": "org.h2.Driver"
//        },
//        "binaryStorage":{
//          "type":"file",
//          "directory":"storage/binaries",
//          "minimumBinarySizeInBytes":4096
//        }
// */
///*
//  "storage" : {
//        "persistence" : {
//            "type" : "mem"
//        }
//    },
//
//"            \"path\" : \"{java.io.tmpdir}/spimedb\"\n" +
// */
//
//            RepositoryConfiguration config = RepositoryConfiguration.read(
//                "{\n" +
//                        "    \"name\" : \"DataRepository\",\n" +
////                        "    \"transactionMode\" : \"auto\",\n" +
////                        "    \"monitoring\" : {\n" +
////                        "        \"enabled\" : true,\n" +
////                        "    },\n" +
//                        "    \"workspaces\" : {\n" +
//                        "        \"predefined\" : [\"otherWorkspace\"],\n" +
//                        "        \"default\" : \"default\",\n" +
//                        "        \"allowCreation\" : true,\n" +
//                        "    },\n" +
//                        "    \"storage\" : {\n" +
//                        "        \"persistence\": {\n" +
//                        "            \"type\": \"file\",\n" +
//                        "            \"path\" : \"/tmp/spimedb\"\n" +
//                        "        },\n" +
//                        "        \"binaryStorage\" : {\n" +
//                        "            \"type\" : \"file\",\n" +
//                        "            \"directory\" : \"DataRepository/binaries\",\n" +
//                        "            \"minimumBinarySizeInBytes\" : 4096\n" +
//                        "        }\n" +
//                        "    },\n" +
//                        "    \"security\" : {\n" +
//                        "        \"anonymous\" : {\n" +
//                        "            \"username\" : \"<anonymous>\",\n" +
//                        "            \"roles\" : [\"readonly\",\"readwrite\",\"admin\"],\n" +
//                        "            \"useOnFailedLogin\" : false\n" +
//                        "        },\n" +
////                        "        \"providers\" : [\n" +
////                        "          {\n" +
////                        "           \"name\" : \"My Custom Security Provider\",\n" +
////                        "           \"classname\" : \"com.example.MyAuthenticationProvider\"\n" +
////                        "          },\n" +
////                        "          {\n" +
////                        "            \"classname\" : \"JAAS\",\n" +
////                        "            \"policyName\" : \"modeshape-jcr\"\n" +
////                        "          }\n" +
////                        "         ]\n" +
//                        "    },\n" +
//                        "    \"indexProviders\" : {\n" +
//                        "        \"local\" : {\n" +
//                        "            \"classname\" : \"org.modeshape.jcr.index.local.LocalIndexProvider\",\n" +
//                        "            \"directory\" : \"target/LocalIndexProviderQueryTest\"\n" +
//                        "        },\n" +
//                        "    },\n" +
//                        "   \"indexes\" : {\n" +
//                        "        \"nodesByName\" : {\n" +
//                        "            \"kind\" : \"value\",\n" +
//                        "            \"provider\" : \"local\",\n" +
//                        "            \"nodeType\" : \"nt:base\",\n" +
//                        "            \"columns\" : \"jcr:name(NAME)\"\n" +
//                        "        },\n" +
//                        "        \"nodesByLocalName\" : {\n" +
//                        "            \"kind\" : \"value\",\n" +
//                        "            \"provider\" : \"local\",\n" +
//                        "            \"nodeType\" : \"nt:base\",\n" +
//                        "            \"columns\" : \"mode:localName(STRING)\"\n" +
//                        "        },\n" +
//                        "        \"nodesByDepth\" : {\n" +
//                        "            \"kind\" : \"value\",\n" +
//                        "            \"provider\" : \"local\",\n" +
//                        "            \"nodeType\" : \"nt:base\",\n" +
//                        "            \"columns\" : \"mode:depth(LONG)\"\n" +
//                        "        },\n" +
//                        "        \"nodesByPath\" : {\n" +
//                        "            \"kind\" : \"value\",\n" +
//                        "            \"provider\" : \"local\",\n" +
//                        "            \"nodeType\" : \"nt:base\",\n" +
//                        "            \"columns\" : \"jcr:path(PATH)\"\n" +
//                        "        }\n" +
//                        "     },\n" +
////                        "    \"sequencing\" : {\n" +
////                        "        \"removeDerivedContentWithOriginal\" : true,\n" +
////                        "        \"threadPool\" : \"modeshape-workers\",\n" +
////                        "        \"sequencers\" : {\n" +
////                        "            \"ZIP Sequencer\" : {\n" +
////                        "                \"description\" : \"ZIP Files loaded under '/files' and extracted into '/sequenced/zip/$1'\",\n" +
////                        "                \"classname\" : \"ZipSequencer\",\n" +
////                        "                \"pathExpressions\" : [\"default:/files(//)(*.zip[*])/jcr:content[@jcr:data] => default:/sequenced/zip/$1\"],\n" +
////                        "            },\n" +
////                        "            \"Delimited Text File Sequencer\" : {\n" +
////                        "                \"classname\" : \"org.modeshape.sequencer.text.DelimitedTextSequencer\",\n" +
////                        "                \"pathExpressions\" : [\"[MODE:Clustering])/jcr:content[@jcr:data] => default:/sequenced/text/delimited/$1\"],\n" +
////                        "                \"splitPattern\" : \",\"\n" +
////                        "            }\n" +
////                        "        }\n" +
////                        "    }\n" +
//                        "}"
//            );
//
////            RepositoryConfiguration config = new RepositoryConfiguration(configX)
////                    .withName("Test Repo")
////                    .with(new LocalEnvironment());
//
//
//            //org.modeshape.schematic.document.Json.write(config.getDocument(), System.out);
//
//
////            Editor editor = config.edit();
////            editor.setString(RepositoryConfiguration.FieldName.JNDI_NAME, "new-jndi-name");
//
////            // Create a new configuration with the edited document ...
////            RepositoryConfiguration newConfig = new RepositoryConfiguration(editor,config.getName());
////
////            // Deploy the new configuration ...
////            javax.jcr.Repository repo = engine.deploy(config);
//
//
//            // We could change the name of the repository programmatically ...
//            // config = config.withName("Some Other Repository");
//
//            // Verify the configuration for the repository ...
//            Problems problems = config.validate();
//            if (problems.hasErrors()) {
//                System.err.println("Problems starting the engine.");
//                System.err.println(problems);
//                System.exit(-1);
//            }
//
//            // Deploy the repository ...
//            repository = engine.deploy(config);
//            repositoryName = config.getName();
//        } catch ( Throwable e)  {
//            e.printStackTrace();
//            System.exit(-1);
//            return;
//        }
//
//        Session session = null;
//        try
//
//        {
//            // Get the repository
//            repository = engine.getRepository(repositoryName);
//
//            // Create a session ...
//            session = repository.login("default");
//
//            // Get the root node ...
//            Node root = session.getRootNode();
//            assert root != null;
//
//            System.out.println(root);
//            root.getNodes().forEachRemaining( x -> System.out.println(x));
//
//            root.getNode("x").setProperty("y", "z");
//            session.save();
//
//            System.out.println("Found the root node in the \"" + session.getWorkspace().getName() + "\" workspace");
//        } catch ( RepositoryException e)  {
//            e.printStackTrace();
//        } finally {
//            if (session != null) session.logout();
//            System.out.println("Shutting down engine ...");
//            try {
//                engine.shutdown().get();
//                System.out.println("Success!");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}
