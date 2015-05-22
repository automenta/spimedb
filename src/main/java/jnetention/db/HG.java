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

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        add(Core.json.<JsonNode>valueToTree(j));
    }

    public void add(JsonNode j) {



        //TODO avoid string conversion
        Json json = Json.read(j.toString());
        jsonNode.add(json);

        HGHandle x = jsonNode.unique(json);
        Object X = jsonNode.get(x);
        System.out.println(graph.getType(x) + " " + X + " " + X.getClass());



    }

    public void print(PrintStream out) {
        List<HGHandle> x = jsonNode.findAll(new AnyAtomCondition());
        x.forEach(o -> out.println( graph.<Object>get(o) + "\t" +  jsonNode.getAll(HGQuery.hg.incident(o))));

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




    public static void main(String[] args) {
        new HG();
    }

    public Iterator<NObject> allValues() {
        //TODO see if there is a more specific query
        HGSearchResult<HGHandle> r = jsonNode.find(HGQuery.hg.all());
        //HGSearchResult<HGHandle> r = jsonNode.find(HGQuery.hg.all());

        return Iterators.transform(r, new Function<HGHandle, NObject>() {
            @Override
            public NObject apply(HGHandle hgHandle) {
                Object n = jsonNode.get(hgHandle);
                if (n instanceof Json) {
                    Json j = (Json) n;
                    if (j.isObject()) {
                        Map<String, Json> m = j.asJsonMap();
                        String uuid = hgHandle.getPersistent().toString();
                        return NObject.from(uuid, j.asJsonMap());
                    }
                }
                return null;
            }
        });
    }
}
