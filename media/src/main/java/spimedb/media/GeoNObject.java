package spimedb.media;

import jcog.Util;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.opensextant.geodesy.*;
import org.opensextant.giscore.geometry.Line;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.geometry.Polygon;
import spimedb.MutableNObject;
import spimedb.NObject;

import java.util.List;

/**
 * Created by me on 4/2/17.
 */
public class GeoNObject extends MutableNObject {

    public GeoNObject(String id) {
        super(id);
    }

    public GeoNObject(NObject copy) {
        super(copy);
    }

    public NObject where(Geodetic2DPoint c) {

        min.coord[1] = max.coord[1] = c.getLongitudeAsDegrees();
        min.coord[2] = max.coord[2] = c.getLatitudeAsDegrees();

        if (c instanceof Geodetic3DPoint g3) {
            min.coord[3] = max.coord[3] = g3.getElevation();
        } else {
            min.coord[3] = Float.NEGATIVE_INFINITY;
            max.coord[3] = Float.POSITIVE_INFINITY;
        }

        //put("g" + NObject.POINT, )

        return this;
    }

    public NObject where(Longitude AX, Longitude BX, Latitude AY, Latitude BY) {


        {
            float a = (float) AX.inDegrees();
            float b = (float) BX.inDegrees();
            if (a > b) {
                float t = a;
                a = b;
                b = t;
            } //swap
            min.coord[1] = a;
            max.coord[1] = b;
            assert (a <= b);
        }

        {
            float a = (float) AY.inDegrees();
            float b = (float) BY.inDegrees();
            if (a > b) {
                float t = a;
                a = b;
                b = t;
            } //swap
            min.coord[2] = a;
            max.coord[2] = b;
            assert (a <= b);
        }

//        if ((a instanceof Geodetic3DPoint) && (b instanceof Geodetic3DPoint)) {
//            float az = (float) ((Geodetic3DPoint) a).getElevation();
//            float bz = (float) ((Geodetic3DPoint) b).getElevation();
//            if (az > bz) { float t = az; az = bz; bz = t; } //swap
//            min.coord[3] = az;
//            max.coord[3] = bz;
//            assert(min.coord[3] < max.coord[3]);
//        } else {
//            min.coord[3] = Float.NEGATIVE_INFINITY;
//            max.coord[3] = Float.POSITIVE_INFINITY;
//        }

        return this;
    }

    public void where(Geodetic2DBounds bb) {
        where(bb.getEastLon(), bb.getWestLon(), bb.getSouthLat(), bb.getNorthLat());
    }

    private static double[][] toArrayGeoJSON(List<LngLatAlt> lp) {
        double[][] points = new double[lp.size()][2];

        for (int i = 0; i < points.length; i++) {
            var c = lp.get(i);
            double[] pi = points[i];
            pi[0] = c.getLatitude();
            pi[1] = c.getLongitude();
        }
        return points;
    }

    public static double[][] toArray(List<Point> lp) {
        double[][] points = new double[lp.size()][2];

        for (int i = 0; i < points.length; i++) {
            Geodetic2DPoint c = lp.get(i).getCenter();
            double[] pi = points[i];
            pi[0] = c.getLatitudeAsDegrees();
            pi[1] = c.getLongitudeAsDegrees();
        }
        return points;
    }
    public NObject where(Line l) {

        List<Point> lp = l.getPoints();
        double[][] points = toArray(lp);

        where(l.getBoundingBox());
        put(NObject.LINESTRING, points);
        return this;
    }
    public NObject where(LineString l) {
        var points = toArrayGeoJSON(l.getCoordinates());
        double[] bb = bounds(points);
        where(bb[0], bb[1], bb[2], bb[3]);
        put(NObject.LINESTRING, points);
        return this;
    }

    /** TODO handle inner rings? */
    public NObject where(Polygon p) {
        double[][] outerRing = toArray(p.getOuterRing().getPoints());
        where(p.getBoundingBox());
        put(NObject.POLYGON, outerRing);
        return this;
    }
    /** TODO handle inner rings? */
    public NObject where(org.geojson.Polygon p) {
        var outerRing = toArrayGeoJSON(p.getExteriorRing());
        double[] bb = bounds(outerRing); where(bb[0], bb[1], bb[2], bb[3]);
        put(NObject.POLYGON, outerRing);
        return this;
    }
    public static double[] bounds(double[][] c) {
        double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
        for (var a : c) {
            double y = a[0];
            double x = a[1];
            //double z = a.getAltitude() //TODO
            xMin = Util.min(xMin, x);
            xMax = Util.max(xMax, x);
            yMin = Util.min(yMin, y);
            yMax = Util.max(yMax, y);
        }
        return new double[] { xMin, yMin, xMax, yMax };
    }
//    public static double[] bounds(List<LngLatAlt> c) {
//        double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY;
//        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
//        for (var a : c) {
//            double x = a.getLongitude();
//            double y = a.getLatitude();
//            //double z = a.getAltitude() //TODO
//            xMin = Util.min(xMin, x);
//            xMax = Util.max(xMax, x);
//            yMin = Util.min(yMin, y);
//            yMax = Util.max(yMax, y);
//        }
//        return new double[] { xMin, yMin, xMax, yMax };
//    }
}
