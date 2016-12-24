/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.sense;

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
abstract public class ImportNetention {

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

    public ImportNetention() throws Exception {
        this(new File("./data/ontology.json"));
    }

    public ImportNetention(File u) throws IOException {

        Map<String, Object> wrapper
                = jsonMapper.readValue(new FileInputStream(u), Map.class);

        for (LinkedHashMap o : (Iterable<LinkedHashMap>) wrapper.get("class")) {
            onTag(o);
        }

    }

    abstract public void onTag(LinkedHashMap id);
    //abstract public void onProperty(String id);

    public static void main(String[] args) throws Exception {
        new ImportNetention() {

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
                System.out.println(id + ' ' + name + ' ' + extend);
            }

        };
    }

    /**
     * Generalization of a URL/URI, label, semantic predicate, type / class, or any kind of literalizable concept.
     */
    public static class Tag {


        public final String id;

        public String name;
        public String description;

        //public Map<String,Object> meta = new HashMap();


        private String icon;

        //TEMPORARY
        public final static String Human = "Human";
        public final static String User = "User";

        public final static String Learn = "Learn";
        public final static String Do = "Do";
        public final static String Teach = "Teach";

        public final static String Can = "Can";
        public final static String Need = "Need";
        public final static String Not = "Not";

        public final static String Web = "Web";
        public final static String tag = "tag";
        public final static String property = "property";



        protected Tag(String id) {

            this.id = id;

            //TODO replace with getOrAdd method


        }

    //    public Tag meta(String key, Object value) {
    //        vertex.setProperty(key, value);
    //        return this;
    //    }


        public void setDescription(String d) {
            this.description = d;
        }

        public void icon(String icon) {
            this.icon = icon;
        }


        public void name(String name) {
            this.name = name;
        }

    //    public void addEdge(String label, String target) {
    //        edges.put(label)
    //    }

    //    public Edge inheritance(String object, double v) {
    //        if (v > 0) {
    //            MapGraph.MVertex vv = vertex.graph().getVertex(object);
    //            if (vv == null) {
    //                vv = vertex.graph().addVertex(object);
    //            }
    //            Edge e = vertex.addEdge(NALOperator.INHERITANCE.str, vv);
    //            e.setProperty("%", v);
    //            return e;
    //        }
    //        else {
    //            //TODO
    //        }
    //        return null;
    //    }
    }
}
