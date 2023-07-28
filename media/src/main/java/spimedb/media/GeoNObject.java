package spimedb.media;

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

        double lon = c.getLongitudeAsDegrees();
        min.coord[1] = max.coord[1] = lon;

        double lat = c.getLatitudeAsDegrees();
        min.coord[2] = max.coord[2] = lat;


        if (c instanceof Geodetic3DPoint g3) {
            double ele = g3.getElevation();
            min.coord[3] = max.coord[3] = ele;
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

    public NObject where(Polygon p) {
        double[][] outerRing = toArray(p.getOuterRing().getPoints());

        //TODO handle inner rings

        where(p.getBoundingBox());
        put(NObject.POLYGON, outerRing);
        return this;
    }


}
