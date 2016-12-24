package spimedb.sense;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Lists;
import spimedb.NObject;

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

    public static void load(final Object self) throws IOException {
        try {
            new ImportSchemaOrg() {

                @Override
                public void onClass(String id, String label, List<String> supertypes, String comment) {


                    NObject t = new NObject(id, label);


                    t.description(comment);

                    for (String s : supertypes) {
                        t.put(s);
                        //t.inh.put(s, 1.0);
                    }

                    //self.add(t);


                }

                @Override
                public void onProperty(String id, String label, List<String> domains, List<String> ranges, String comment) {
                }

            };
        } catch (IOException e) {
            e.printStackTrace();
        }


    }




    public ImportSchemaOrg() throws IOException {

        String[] line;
        CSVReader reader = new CSVReader(new FileReader("data/schema.org/all-classes.csv"), ',', '\"');
        int c = 0;
        while ((line = reader.readNext()) != null) {
            if (c++ == 0) { /* skip first line */ continue;
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
                supertypes = Collections.EMPTY_LIST;
            }

            onClass(id, label, supertypes, comment);
        }
        reader.close();

        reader = new CSVReader(new FileReader("data/schema.org/all-properties.csv"), ',', '\"');
        c = 0;
        while ((line = reader.readNext()) != null) {
            if (c++ == 0) { /* skip first line */ continue;
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
                domains = Collections.EMPTY_LIST;
            }
            if ((line.length >= 5) && (line[4].length() > 0)) {
                ranges = Arrays.asList(line[4].split(" "));
                /*ranges = ranges.stream().map(s -> {
                 if (Core.isPrimitive(s.toLowerCase()))
                 return s.toLowerCase();
                 return s;
                 }).collect(toList());*/

            } else {
                ranges = Collections.EMPTY_LIST;
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
