/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.sense;

import com.fasterxml.jackson.databind.JsonNode;
import spimedb.util.JSON;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author me
 */
public abstract class ClimateViewer {

    static final String basePath = "cache";
    static final String layersFile = "data/climateviewer.json";

    String currentSection = "Unknown";

    public ClimateViewer() throws Exception {


        /*ExecutorService executor =

         threads == 1 ?
         Executors.newSingleThreadExecutor() :
         Executors.newFixedThreadPool(threads);*/
        if (!Files.exists(Paths.get(basePath))) {
            Files.createDirectory(Paths.get(basePath));
        }

        URI uri = new File(layersFile).toURI();

        byte[] encoded = Files.readAllBytes(Paths.get(uri));

        String layers = new String(encoded, "UTF8");

        JsonNode lll = JSON.fromJSON(layers).get("cv");

        JsonNode n = lll;





        n.forEach(x -> {

            //x.isObject() &&
            if (x.has("section")) {

                String name = x.get("section").textValue();
                String id = getSectionID(x);
                String icon = x.get("icon").textValue();

                if (id == null) {
                    throw new RuntimeException("Section " + x + " missing ID");
                }

                onSection(name, id, icon);
                //Tag t = Tag.the(st, id);
                //if (t != null)
                  //  t.name(name);

            }
            else  {
                String icon = null;

                if (x.has("icon"))
                    icon = x.get("icon").textValue();

                if (x.isTextual()) {
                    currentSection = x.textValue();
                } else if (x.isObject() && x.has("section")) {
                    currentSection = getSectionID(x);
                } else if (!x.isObject() && !x.has("layer")) {
                    System.err.println("Unrecognized item: " + x);
                } else {
                    final String id = x.get("layer").textValue();
                    final String kml = x.has("kml") ? x.get("kml").textValue() : null;
                    final String name = x.get("name").textValue();



                    onLayer(id, name, kml, icon, currentSection);

                    //System.out.println(currentSection + " " + name + " " + x);
                    //executor.submit(new ImportKML(st, proxy, id, name, url));
                }


            }
        });


    }

    abstract public void onLayer(String id, String name, String kml, String icon, String currentSection);

    abstract public void onSection(String name, String id, String icon);


//
//    public static void _main(String[] args) throws Exception {
//
//        CachingProxyServer cache = new CachingProxyServer(16000, basePath);
//
//        ElasticSpacetime es = ElasticSpacetime.server("cv", false);
//        //new ClimateViewer(cache.proxy, layersFile, es, 1, 3);
//    }

    public static String getSectionID(JsonNode x) {
        if (x.has("id")) {
            return x.get("id").textValue();
        } else if (x.has("section")) {
            String s = x.get("section").textValue();
            if (s.contains(" ")) {
                return null;
            }
            return s;
        }
        return null;
    }

}
