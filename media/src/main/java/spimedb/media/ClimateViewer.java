/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.media;

import com.fasterxml.jackson.databind.JsonNode;
import spimedb.SpimeDB;
import spimedb.server.Server;

/**
 * @author me
 */
public class ClimateViewer {

//    static final String basePath = "cache";
//    static final String layersFile = "data/climateviewer.json";

//    String currentSection = "Unknown";

    public static void main(String[] args) {
        new ClimateViewer();
    }

    public ClimateViewer() {

        Server server = new Server(new SpimeDB());

        geojson("nuclear", "https://climateviewer.org/layers/geojson/2018/Nuclear-Reactors-Pressurized-Water-ClimateViewer-3D.geojson", server);

        geojson("emf", "https://climateviewer.org/layers/geojson/2018/Extremely-Low-Frequency-ULF-ELF-VLF-Transmission-Sites-ClimateViewer-3D.geojson", server);

//        geojson("earthquake", "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson", server);
        //geojson("air", "https://gis-calema.opendata.arcgis.com/datasets/acee3cdfe7ff45ad9751e8f9d95a50b3_0.geojson?outSR=%7B%22latestWkid%22%3A3857%2C%22wkid%22%3A102100%7D", server);

        kml("https://climateviewer.org/layers/kml/2018/submarine-telecommunication-cables-ClimateViewer-3D.kmz",
                (GeoNObject) new GeoNObject("submarine.kmz").withTags("submarine"), server);

        //server.db.sync(50);
        //server.db.forEach(System.out::println);
        //System.out.println("db size=" + server.db.size());

        server.server.fps(30);

//        /*ExecutorService executor =
//
//         threads == 1 ?
//         Executors.newSingleThreadExecutor() :
//         Executors.newFixedThreadPool(threads);*/
//        if (!Files.exists(Paths.get(basePath))) {
//            Files.createDirectory(Paths.get(basePath));
//        }
//
//        URI uri = new File(layersFile).toURI();
//
//        String layers = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);
//
//        JsonNode lll = JSON.fromJSON(layers).get("cv");
//
//        JsonNode n = lll;
//
//
//        n.forEach(x -> {
//
//            //x.isObject() &&
//            if (x.has("section")) {
//
//                String name = x.get("section").textValue();
//                String id = getSectionID(x);
//                String icon = x.get("icon").textValue();
//
//                if (id == null) {
//                    throw new RuntimeException("Section " + x + " missing ID");
//                }
//
//                onSection(name, id, icon);
//                //Tag t = Tag.the(st, id);
//                //if (t != null)
//                //  t.name(name);
//
//            } else {
//                String icon = null;
//
//                if (x.has("icon"))
//                    icon = x.get("icon").textValue();
//
//                if (x.isTextual()) {
//                    currentSection = x.textValue();
//                } else if (x.isObject() && x.has("section")) {
//                    currentSection = getSectionID(x);
//                } else if (!x.isObject() && !x.has("layer")) {
//                    System.err.println("Unrecognized item: " + x);
//                } else {
//                    final String id = x.get("layer").textValue();
//                    final String kml = x.has("kml") ? x.get("kml").textValue() : null;
//                    final String name = x.get("name").textValue();
//                    onLayer(id, name, kml, icon, currentSection);
//
//                    //System.out.println(currentSection + " " + name + " " + x);
//                    //executor.submit(new ImportKML(st, proxy, id, name, url));
//                }
//
//
//            }
//        });


    }

    private static void kml(String url, GeoNObject prototype, Server server) {
        new KML(server.db, prototype).url(null, url).run();
    }

    private static void geojson(String tag, String url, Server server) {
        GeoJSON.load(url, new GeoJSON.GeoJSONBuilder(tag), server.db);
    }

//    abstract public void onLayer(String id, String name, String kml, String icon, String currentSection);
//
//    abstract public void onSection(String name, String id, String icon);


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
