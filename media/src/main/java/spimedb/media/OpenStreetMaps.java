package spimedb.media;

import de.westnordost.osmapi.ApiRequestWriter;
import de.westnordost.osmapi.ApiResponseReader;
import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.map.data.*;
import de.westnordost.osmapi.overpass.MapDataWithGeometryHandler;
import de.westnordost.osmapi.overpass.OverpassMapDataApi;
import jcog.Log;
import jcog.Util;
import org.slf4j.Logger;
import spimedb.SpimeDB;
import spimedb.util.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** https://unpkg.com/osmtogeojson@3.0.0-beta.5/index.js */
public class OpenStreetMaps implements MapDataWithGeometryHandler {

    private final String apiURL = "https://overpass-api.de/api/";
    private final SpimeDB db;
    private static final Set<String> tagExcludes = Set.of("source", "wikidata" /* TODO */);

    public OpenStreetMaps(SpimeDB db) {
        this.db = db;
    }

    public void load(double lat, double lon, double size) {
        load(lat - size/2, lon - size/2, lat + size/2, lon + size/2);
    }

    public void load(double latMin, double lonMin, double latMax, double lonMax) {
        new OverpassMapDataApi(new MyOsmConnection()).queryElementsWithGeometry(
            "[bbox:" + latMin + "," + lonMin + "," + latMax + "," + lonMax +
                    "]; (node; way;); out geom;"
                    //"]; (node; way;); out body geom;"
                    //"]; (node; way; relation;); out body geom;"

            , this);

        logger.info("({}..{},{}..{}) loaded", latMin, latMax, lonMin, lonMax);
    }

    private static final Logger logger = Log.log(OpenStreetMaps.class);

    @Override
    public void handle(BoundingBox boundingBox) {

    }

    @Override
    public void handle(Node node) {
        Map<String, String> tags = node.getTags();
        if (!tags.containsKey("name"))
            return; //ignore if has no name

        GeoNObject g = new GeoNObject("osm_node_" + node.getId());
        LatLon p = node.getPosition();
        g.where(p.getLongitude(), p.getLatitude(), 0 /* TODO ele */);
        tags(g, tags);
        db.add(g);
    }

    @Override
    public void handle(Way way, BoundingBox boundingBox, List<LatLon> points) {
        GeoNObject g = new GeoNObject("osm_way_" + way.getId());
        tags(g, way.getTags());
        g.where(points, points.get(0).equals(points.get(points.size() - 1)));
        db.add(g);
    }

    /** extract OSM tags */
    private static void tags(GeoNObject g, Map<String, String> tags) {
        tags.forEach((k, v) -> {
            if (tagExcludes.contains(k))
                return;
            switch (k) {
                case "name" -> g.name(v);
                case "amenity" -> g.withTags(v);
                default -> g.put("osm:" + k, v);
            }
        });
    }

    @Override
    public void handle(Relation relation, BoundingBox boundingBox, Map<Long, LatLon> nodeGeom, Map<Long, List<LatLon>> wayGeom) {
        //System.out.println("relation: " + relation.getTags());
//        //HACK
//        GeoNObject g = new GeoNObject("osm_rel_" + relation.getId());
//        tags(g, relation.getTags());
//        if (!wayGeom.isEmpty()) {
//            var points = wayGeom.values().iterator().next();
//            g.where(points, points.get(0).equals(points.get(points.size() - 1)));
//        } else {
//            if (nodeGeom.isEmpty()) return;
//            var node = nodeGeom.values().iterator().next();
//            g.where(node.getLongitude(), node.getLatitude());
//        }
//        db.add(g);

    }

    private class MyOsmConnection extends OsmConnection {

        MyOsmConnection() {
            super(OpenStreetMaps.this.apiURL, "");
        }

        @Override
        public <T> T makeRequest(String call, String method, boolean authenticate, ApiRequestWriter writer, ApiResponseReader<T> reader) {
            var bos = new ByteArrayOutputStream();
            try {
                writer.write(bos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String url = apiURL + call + "?" + new String(bos.toByteArray());

            try {
                var is = HTTP.inputStream(url);
                T y = reader.parse(is);
                is.close();
                return y;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

//    public static class WayWithGeometry {
//        public WayWithGeometry(Way way, BoundingBox bounds, List<LatLon> geometry) {
//            this.way = way;
//            this.bounds = bounds;
//            this.geometry = geometry;
//        }
//
//        public final Way way;
//        public final BoundingBox bounds;
//        public final List<LatLon> geometry;
//    }
//
//    public static class RelationWithGeometry {
//        RelationWithGeometry(Relation relation, BoundingBox bounds, Map<Long, LatLon> nodeGeometries, Map<Long, List<LatLon>> wayGeometries) {
//            this.relation = relation;
//            this.bounds = bounds;
//            this.nodeGeometries = nodeGeometries;
//            this.wayGeometries = wayGeometries;
//        }
//
//        public final Relation relation;
//        public final BoundingBox bounds;
//        public final Map<Long, LatLon> nodeGeometries;
//        public final Map<Long, List<LatLon>> wayGeometries;
//    }
//
//    public static final class MapDataWithGeometryCollection implements MapDataWithGeometryHandler {
//        public BoundingBox bounds = null;
//        public final List<Node> nodes = new Lst<>();
//        public final List<WayWithGeometry> waysWithGeometry = new Lst<>();
//        public final List<RelationWithGeometry> relationsWithGeometry = new Lst<>();
//
//        @Override
//        public void handle(BoundingBox bounds) {
//            this.bounds = bounds;
//        }
//
//        @Override
//        public void handle(Node node) {
//            nodes.add(node);
//        }
//
//        @Override
//        public void handle(Way way, BoundingBox bounds, List<LatLon> geometry) {
//            waysWithGeometry.add(new WayWithGeometry(way, bounds, geometry));
//        }
//
//        @Override
//        public void handle(
//                Relation relation,
//                BoundingBox bounds,
//                Map<Long, LatLon> nodeGeometries,
//                Map<Long, List<LatLon>> wayGeometries) {
//            relationsWithGeometry.add(new RelationWithGeometry(relation, bounds, nodeGeometries, wayGeometries));
//        }
//    }
}
