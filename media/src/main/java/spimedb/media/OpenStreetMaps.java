package spimedb.media;

import de.westnordost.osmapi.ApiRequestWriter;
import de.westnordost.osmapi.ApiResponseReader;
import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.map.data.*;
import de.westnordost.osmapi.overpass.MapDataWithGeometryHandler;
import de.westnordost.osmapi.overpass.OverpassMapDataApi;
import jcog.data.list.Lst;
import org.geojson.GeoJsonObject;
import org.geojson.LineString;
import org.jetbrains.annotations.NotNull;
import org.opensextant.geodesy.Geodetic2DPoint;
import spimedb.SpimeDB;
import spimedb.cluster.DataSet;
import spimedb.util.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** https://unpkg.com/osmtogeojson@3.0.0-beta.5/index.js */
public class OpenStreetMaps implements MapDataWithGeometryHandler {

    private final String apiURL = "https://overpass-api.de/api/";
    private final SpimeDB db;
    private static final Set<String> tagExcludes = Set.of("source", "wikidata" /* TODO */);

    public static void main(String[] args) {
        SpimeDB db = new SpimeDB();
        new OpenStreetMaps(db).load(
            -23, -43.0995, -22.9995, -43.1
        );
        //db.forEach(System.out::println);
    }

    public OpenStreetMaps(SpimeDB db) {
        this.db = db;
    }

    public void load(double latMin, double lonMin, double latMax, double lonMax) {
        OsmConnection connection = new OsmConnection(apiURL, "") {
            @Override
            public <T> T makeRequest(String call, String method, boolean authenticate, ApiRequestWriter writer, ApiResponseReader<T> reader) {
                var bos = new ByteArrayOutputStream();
                try {
                    writer.write(bos);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    //var is = HTTP.inputStream(apiURL + "/" + call, new String(bos.toByteArray()));
                    String url = apiURL + call + "?" + new String(bos.toByteArray());
                    var is = HTTP.inputStream(url);
                    T y = reader.parse(is);
                    is.close();
                    return y;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        OverpassMapDataApi overpass = new OverpassMapDataApi(connection);
        overpass.queryElementsWithGeometry(
            "[bbox:" + lonMin + "," + latMin + "," + lonMax + "," + latMax + "]; (node; way;); out geom;"
            , this);
    }

    @Override
    public void handle(BoundingBox boundingBox) {

    }

    @Override
    public void handle(Node node) {
        GeoNObject g = new GeoNObject("osm_" + node.getId());
        LatLon p = node.getPosition();
        g.where(p.getLongitude(), p.getLatitude(), 0 /* TODO ele */);
        if (!tags(node.getTags(), g)) return;
        db.add(g);
    }

    @Override
    public void handle(Way way, BoundingBox boundingBox, List<LatLon> points) {
        GeoNObject g = new GeoNObject("osm_" + way.getId());
        if (!tags(way.getTags(), g))
            return;

        if (points.get(0).equals(points.get(points.size()-1))) {
            g.where(points, true);
        } else {
            g.where(points, false);
        }
        db.add(g);
    }

    /** extract OSM tags */
    private static boolean tags(Map<String, String> tags, GeoNObject g) {
        if (!tags.containsKey("name"))
            return false; //ignore if has no name
        tags.forEach((k, v) -> {
            if (tagExcludes.contains(k))
                return;
            switch (k) {
                case "name" -> g.name(v);
                case "amenity" -> g.withTags(v);
                default -> g.put("osm:" + k, v);
            }
        });
        return true;
    }

    @Override
    public void handle(Relation relation, BoundingBox boundingBox, Map<Long, LatLon> nodeGeom, Map<Long, List<LatLon>> wayGeom) {
        //System.out.println("relation: " + relation.getTags());
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
