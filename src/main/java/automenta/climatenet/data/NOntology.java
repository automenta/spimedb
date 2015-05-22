/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.data;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author me
 */
abstract public class NOntology {

    public final static ObjectMapper jsonMapper = new ObjectMapper().
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
            configure(Feature.ALLOW_SINGLE_QUOTES, true);

//    public static void load(final ElasticSpacetime db) throws Exception {
//        final BulkRequestBuilder bulk = db.newBulk();
//
//        new NOntology() {
//
//            @Override
//            public void onTag(LinkedHashMap o) {
//
//                String id = o.get("id").toString();
//
//                String name = o.get("name").toString();
//                Object description = o.get("description");
//                List extend = (List) o.get("extend");
//
//                Tag t = new Tag(id, name);
//                if (description != null) {
//                    t.setDescription(description.toString());
//                }
//
//                if (extend!=null)
//                    for (Object s : extend) {
//                        t.inh.put(s.toString(), 1.0);
//                    }
//
//                if (o.containsKey("wmsLayer")) {
//                    t.meta.put("wmsLayer", o.get("wmsLayer"));
//                }
//                if (o.containsKey("tileLayer")) {
//                    t.meta.put("tileLayer", o.get("tileLayer"));
//                }
//
//                db.addTag(bulk, t);
//            }
//
//        };
//
//        db.commit(bulk);
//
//    }

    public NOntology() throws Exception {
        this(new File("./data/ontology.json"));
    }

    public NOntology(File u) throws IOException {

        Map<String, Object> wrapper
                = jsonMapper.readValue(new FileInputStream(u), Map.class);

        for (LinkedHashMap o : (Iterable<LinkedHashMap>) wrapper.get("class")) {
            onTag(o);
        }

    }

    abstract public void onTag(LinkedHashMap id);
    //abstract public void onProperty(String id);

    public static void main(String[] args) throws Exception {
        new NOntology() {

            @Override
            public void onTag(LinkedHashMap o) {
                //System.out.println(o.getClass() + ": " + o);
                String id = o.get("id").toString();
                String name = o.get("name").toString();
                List extend = (List) o.get("extend");

                if (o.containsKey("tileLayer")) {

                }
                if (o.containsKey("wmsLayer")) {

                }
                System.out.println(id + " " + name + " " + extend);
            }

        };
    }

}
