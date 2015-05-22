package jnetention.db;

import com.sleepycat.je.EnvironmentConfig;
import mjson.hgdb.HyperNodeJson;
import mjson.hgdb.JsonTypeSchema;
import org.hypergraphdb.HGConfiguration;
import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.storage.bje.BJEStorageImplementation;

import java.util.List;

import static mjson.Json.object;

/**
 * Hypergraph DB
 * https://code.google.com/p/hypergraphdb/wiki/Json
 */
public class HG {

    public HG() {


        HGConfiguration config = new HGConfiguration();
        config.setTransactional(false);
        config.setSkipOpenedEvent(true);
        config.getTypeConfiguration().addSchema(new JsonTypeSchema());

        {
            BJEStorageImplementation bj = new BJEStorageImplementation();
            // sets the DB to work "In Memory"
            bj.getConfiguration().getEnvironmentConfig().setConfigParam(EnvironmentConfig.LOG_MEM_ONLY, "true");

            config.setStoreImplementation(bj);
        }

        HyperGraph graph = HGEnvironment.get("/tmp/x", config);



        HyperNodeJson jsonNode =
                new HyperNodeJson(graph);

// Add a JSON object with two properties to the database
        jsonNode.add(object("name", "Pedro", "age", 28));

// ... later, do a lookup for all objects
// with name="Pedro" the results are returned
// as a Json array:
        List<HGHandle> A = jsonNode.findAll(object("name", "Pedro"));
        System.out.println(A);

// delete the object with name="Pedro" and age=28:
        jsonNode.remove(jsonNode.exactly(
                object("name", "Pedro", "age", 28)));
    }


    public static void main(String[] args) {
        new HG();
    }
}
