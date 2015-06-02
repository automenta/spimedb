package jnetention.db;

import automenta.climatenet.Core;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.io.Files;
import com.sleepycat.je.EnvironmentConfig;
import jnetention.NObject;
import mjson.Json;
import mjson.hgdb.HyperNodeJson;
import mjson.hgdb.JsonTypeSchema;
import org.hypergraphdb.*;
import org.hypergraphdb.query.AnyAtomCondition;
import org.hypergraphdb.storage.bje.BJEStorageImplementation;
import org.hypergraphdb.type.javaprimitive.ByteType;

import java.io.PrintStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/**
 * Hypergraph DB
 * https://code.google.com/p/hypergraphdb/wiki/Json
 */
public class HG {

    public final HyperGraph graph;

    final HGConfiguration config = new HGConfiguration();

    private final HyperNodeJson jsonNode;

    /** temporary */
    public HG() {

        config.setTransactional(false);
        config.setSkipOpenedEvent(true);
        config.getTypeConfiguration().addSchema(new JsonTypeSchema());


        {
            BJEStorageImplementation bj = new BJEStorageImplementation();
            // sets the DB to work "In Memory"
            bj.getConfiguration().getEnvironmentConfig().setConfigParam(EnvironmentConfig.LOG_MEM_ONLY, "true");
            bj.getConfiguration().getDatabaseConfig().setTemporary(true);
            config.setStoreImplementation(bj);

        }

        this.graph = HGEnvironment.get(Files.createTempDir().getAbsolutePath(), config);
        this.jsonNode = new HyperNodeJson(graph);
    }


    /** persistent */
    public HG(String path) {
        config.setTransactional(false);
        config.setSkipOpenedEvent(true);
        config.getTypeConfiguration().addSchema(new JsonTypeSchema());

        this.graph = HGEnvironment.get(path, config);
        this.jsonNode = new HyperNodeJson(graph);
    }



    public void add(Serializable j) {

        add((JsonNode)Core.json.valueToTree(j));
    }

    public void add(JsonNode j) {

        System.out.println(j);

        //TODO avoid string conversion
        Json json = Json.read(j.toString());
        jsonNode.add(json);

        HGHandle x = jsonNode.unique(json);
        Object X = jsonNode.get(x);
        System.out.println(graph.getType(x) + " " + X + ' ' + X.getClass());



    }

    public void print(PrintStream out) {
        List<HGHandle> x = jsonNode.findAll(new AnyAtomCondition());
        x.forEach(o -> out.println( graph.get(o) + "\t" +  jsonNode.getAll(HGQuery.hg.incident(o))));

//        HGDepthFirstTraversal traversal =
//                new HGDepthFirstTraversal(graph, new SimpleALGenerator(graph));
//
//        while (traversal.hasNext())
//        {
//            Pair<HGHandle, HGHandle> current = traversal.next();
//            HGLink l = graph.get(current.getFirst());
//            Object atom = graph.get(current.getSecond());
//            out.println(l + " -> " + atom);
//        }
    }

//
//        HyperNodeJson jsonNode =
//                new HyperNodeJson(graph);
//
//// Add a JSON object with two properties to the database
//        jsonNode.add(object("name", "Pedro", "age", 28));
//
//// ... later, do a lookup for all objects
//// with name="Pedro" the results are returned
//// as a Json array:
//        List<HGHandle> A = jsonNode.findAll(object("name", "Pedro"));
//        System.out.println(A);
//
//// delete the object with name="Pedro" and age=28:
//        jsonNode.remove(jsonNode.exactly(
//                object("name", "Pedro", "age", 28)));

    public static class StringRef implements HGPersistentHandle {

        public final String s;
        private final byte[] bytes;

        public StringRef(String s) {
            this.s = s;
            this.bytes = Charset.forName("UTF-8").encode(s).array();
        }

        @Override
        public byte[] toByteArray() {
            return bytes;
        }

        @Override
        public int compareTo(HGPersistentHandle o) {
            if (o instanceof StringRef)
                return new ByteType.ByteComparator().compare(bytes, ((StringRef)o).bytes);
            return -1;
        }

        @Override
        public HGPersistentHandle getPersistent() {
            return this;
        }
    }

    public Object get(String id) {
        Object x = jsonNode.get(new StringRef(id));
        if (x instanceof Json) {
            Json j = (Json)x;
            return NObject.from(id, j.asJsonMap());
        }
        return x;
    }

    public Iterator<NObject> allValues() {
        return Iterators.transform(
                //TODO see if there is a more specific query
                jsonNode.find(HGQuery.hg.all()), hgNobject);
    }



    class HGJsontoNObject implements Function<HGHandle, NObject> {
        @Override
        public NObject apply(HGHandle hgHandle) {
            Object n = jsonNode.get(hgHandle);
            if (n instanceof Json) {
                Json j = (Json) n;
                if (j.isObject()) {
                    return NObject.from(hgHandle, j.asJsonMap());
                }
            }
            return null;
        }
    }
    final HGJsontoNObject hgNobject = new HGJsontoNObject();
}
