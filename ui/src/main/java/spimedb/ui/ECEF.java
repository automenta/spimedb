package spimedb.ui;

import static java.lang.Math.*;
import static jcog.Util.cube;
import static jcog.Util.sqr;

/*
 *  ECEF - Earth Centered Earth Fixed coordinate system
 *  https://en.wikipedia.org/wiki/ECEF
 *
 *  LLA - Lat Lon Alt
 */
public enum ECEF {
	;


	private static final double a = 6378137;
    private static final double asq = sqr(a);

    private static final double e = 8.1819190842622e-2;
    private static final double esq = sqr(e);
    private static final double esqInv = 1 - esq;

    static final double DEG2RAD = Math.PI/180;



    public static double[] ecef2latlon(double[] ecef) {
        return ecef2latlon(ecef[0], ecef[1], ecef[2], new double[3]);
    }

    public static double[] ecef2latlon(double x, double y, double z, double[] target) {

        double b = sqrt(asq * esqInv);
        double bsq = sqr(b);
        double ep = sqrt((asq - bsq) / bsq);
        double p = sqrt(sqr(x) + sqr(y));
        double th = atan2(a * z, b * p);

        double lat = atan2((z + sqr(ep) * b * cube(sin(th))), (p - esq * a * cube(cos(th))));

        double N = a / sqrt(1 - esq * sqr(sin(lat)));
        double alt = p / cos(lat) - N;

        double lon = atan2(y, x) % (2 * Math.PI);

        target[0] = lat; target[1] = lon; target[2] = alt;
        return target;
    }


    public static double[] latlon2ecef(double... lla) {
        return latlon2ecef(lla[0], lla[1], lla[2]);
    }

    public static double[] latlon2ecef(double lat, double lon, double alt) {
        return latlon2ecef(lat, lon, alt, new double[3], 0);
    }


    public static double[] latlon2ecef(double lat, double lon, double alt, double[] target, int offset) {


        lat *= DEG2RAD;
        lon *= DEG2RAD;

        double sinLat = sin(lat);
        double N = a / sqrt(1 - esq * sqr(sinLat));

        double cosLat = cos(lat);
        double xy = (N + alt) * cosLat;
        double x = xy * cos(lon);
        double y = xy * sin(lon);

        target[offset++] = x;
        target[offset++] = y;
        target[offset] = (jcog.Util.fma(esqInv, N, alt)) * sinLat;
        return target;
    }
}