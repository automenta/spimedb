/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.netention.data;

import automenta.netention.Core;
import automenta.netention.Tag;
import com.fasterxml.jackson.databind.JsonNode;
import com.syncleus.spangraph.InfiniPeer;
import com.syncleus.spangraph.SpanGraph;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author me
 */
public class ClimateViewerSources {

    final String channel = "spacetime";
    static final String basePath = "cache";
    static final String layersFile = "data/climateviewer.json";
    private final SpanGraph st;

    String currentSection = "Unknown";

    public ClimateViewerSources(final InfiniPeer peer) throws Exception {

        this.st = new SpanGraph(channel, peer);

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

        JsonNode lll = Core.fromJSON(layers).get("cv");

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

                Tag t = Tag.the(st, id);
                if (t != null)
                    t.name(name);

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


                    Tag t = Tag.the(st, id);
                    t.name(name);

                    if (kml != null)
                        t.meta("kmlLayer", kml);

                    if (icon != null)
                        t.icon(icon);

                    if (currentSection != null) {
                        t.inheritance(currentSection, 1.0);
                    }


                    //System.out.println(currentSection + " " + name + " " + x);
                    //executor.submit(new ImportKML(st, proxy, id, name, url));
                }


            }
        });

        System.out.println(st.vertexSet());
        System.out.println(st.edgeSet());

    }



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
