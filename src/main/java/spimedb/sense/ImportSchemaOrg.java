package spimedb.sense;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.db.SpimeDB;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Schema.org and ActivityStreams Ontology import
 *
 * @author me
 */
abstract public class ImportSchemaOrg {

    final static Logger logger = LoggerFactory.getLogger(ImportSchemaOrg.class);

    public static void load(SpimeDB db) {
        //MutableGraph<String> types = GraphBuilder.directed().allowsSelfLoops(false).nodeOrder(ElementOrder.unordered()).expectedNodeCount(1024).build();


        try {
            new ImportSchemaOrg() {


                @Override
                public void onClass(String id, String label, List<String> supertypes, String comment) {




                    //id = escape(id);

                    NObject t = new NObject(id, label);
                    t.description(comment);
                    t.put(">", supertypes.toArray(new String[supertypes.size()]));

                    db.put(t);


//                    types.addNode(id);
//                    for (String s : supertypes) {
//                        if (!s.isEmpty())
//                            types.putEdge(escape(s), id);
//                    }


                }

                private String escape(String id) {
                    return id.replace('-', '_');
                }

                @Override
                public void onProperty(String id, String label, List<String> domains, List<String> ranges, String comment) {
//                    NObject t = new NObject(id, label);
//                    t.description(comment);
//
//                    db.put(t);

                    /*for (String s : domains) {
                        db.edgeAdd(id, SpimeDB.OpEdge.extinh, s);
                    }
                    for (String s : ranges) {
                        db.edgeAdd(s, SpimeDB.OpEdge.extinh, id);
                    }*/

                }


            };
        } catch (IOException e) {
            e.printStackTrace();
        }



//        MutableGraph<String> copy = Graphs.copyOf(types);
//
//
//        //topological sort
//        while (!copy.nodes().isEmpty()) {
//            //find roots
//            for (String r : Lists.newArrayList(copy.nodes())) {
//                if (copy.inDegree(r) == 0) {
//                    copy.removeNode(r);
//
//                    Set<String> superClasses = types.predecessors(r);
//                    Class<? extends NObject> rc = db.the(r, superClasses.toArray(new String[superClasses.size()]));
//                    //System.out.println(r + "\t" + rc + ":\t" + getSuperInterfacesOf(rc) );
//
//
//                }
//
//            }
//
//            //NObject t = db.newTag(id, supertypes.toArray(new String[supertypes.size()]));
//
//        }
//
//        logger.info("{} classes created ({} inheritances)", types.nodes().size(), types.edges().size());
    }


    public ImportSchemaOrg() throws IOException {

            String[] line;
            CSVReader reader = new CSVReader(new FileReader("data/schema.org/all-classes.csv"), ',', '\"');
            int c = 0;
            while ((line = reader.readNext()) != null) {
                if (c++ == 0) { /* skip first line */
                    continue;
                }

                //System.out.println("  " + Arrays.asList(line));
                String id = line[0];
                String label = line[1];
                String comment = line[2];
                //List<String> ancestors = Arrays.asList(line[3].split(" "));
                List<String> supertypes = Arrays.asList(line[4].split(" "));
                //List<String> subtypes = Arrays.asList(line[5].split(" "));
                //List<String> properties;
            /*if ((line.length >= 7) && (line[6].length() > 0))
             properties = Arrays.asList(line[6].split(" "));
             else
             properties = Collections.EMPTY_LIST;*/
                //System.out.println(id + " " + label);
                //System.out.println("  " + supertypes);
                //System.out.println("  " + properties);
                if (id.equals("Action")) {
                    supertypes = Collections.emptyList();
                }

                onClass(id, label, supertypes, comment);
            }
            reader.close();

            reader = new CSVReader(new FileReader("data/schema.org/all-properties.csv"), ',', '\"');
            c = 0;
            while ((line = reader.readNext()) != null) {
                if (c++ == 0) { /* skip first line */
                    continue;
                }

                //System.out.println("  " + Arrays.asList(line));
                //[id, label, comment, domains, ranges]
                String id = line[0].trim();
                String label = "";
                String comment = "";
                if (line.length > 1) {
                    label = line[1];
                }
                if (line.length > 2) {
                    comment = line[2];
                }
                List<String> domains;
                List<String> ranges;
                if ((line.length >= 4) && (line[3].length() > 0)) {
                    domains = Arrays.asList(line[3].split(" "));
                } else {
                    domains = Collections.emptyList();
                }
                if ((line.length >= 5) && (line[4].length() > 0)) {
                    ranges = Arrays.asList(line[4].split(" "));
                /*ranges = ranges.stream().map(s -> {
                 if (Core.isPrimitive(s.toLowerCase()))
                 return s.toLowerCase();
                 return s;
                 }).collect(toList());*/

                } else {
                    ranges = Collections.emptyList();
                }
                onProperty(id, label, domains, ranges, comment);
            }
            reader.close();

            reader = new CSVReader(new FileReader("data/activitystreams/verbs.csv"), ',', '\"');
            while ((line = reader.readNext()) != null) {
                //System.out.println("  " + Arrays.asList(line));
                //[id, label, comment, domains, ranges]
                String id = line[0].trim();
                if (id.length() == 0) {
                    continue;
                }
                String iduppercase = id.substring(0, 1).toUpperCase() + id.substring(1, id.length());
                String description = line[1];

                onClass(id, iduppercase, Lists.newArrayList("Action"), description);
            }
            reader.close();

        }

    abstract public void onClass(String id, String label, List<String> supertypes, String comment);

    abstract public void onProperty(String id, String label, List<String> domains, List<String> ranges, String comment);

}
