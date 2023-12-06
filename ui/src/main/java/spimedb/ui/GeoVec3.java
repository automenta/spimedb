package spimedb.ui;

import jcog.math.v3;
import org.w3c.dom.Element;

import static java.lang.Double.parseDouble;

/**
 * TODO
 * from: https://wiki.openstreetmap.org/wiki/Node
 * Do not use IEEE 32-bit floating point data type since it is limited to about 5 decimal places for the highest longitude.
 * A 32-bit method used by the Rails port is to use an integer (by multiplying each coordinate in degrees by 1E7 and rounding it: this allows to cover all absolute signed coordinates in ±214.7483647 degrees, or a maximum difference of 429.4967295 degrees, a bit more than what is needed).
 * For computing projections, IEEE 64 bit floating points are needed for intermediate results.
 * The 7 rounded decimal places for coordinates in degrees define the worst error of longitude to a maximum of ±5.56595 millimeters on the Earth equator, i.e. it allows building maps with centimetric precision. With only 5 decimal places, the precision of map data would be only metric, causing severe changes of shapes for important objects like buildings, or many zigzags or angular artefacts on roads.
 */
public class GeoVec3 extends v3 {
//    private final double latitude;
//    private final double longitude;
//    private final double altitude;

    public GeoVec3() {
        this(0, 0, 0);
    }

    public GeoVec3(double longitude, double latitude, double altitude) {
        super((float)longitude, (float)latitude, (float)altitude);
//        this.latitude = latitude;
//        this.longitude = longitude;
//        this.altitude = altitude;
    }

//    public GeoVec3(double latitude, double longitude) {
//        this(longitude, latitude, 0);
//    }

    public GeoVec3(Element e) {
        this(parseDouble(e.getAttribute("lon")), parseDouble(e.getAttribute("lat")),
                parseDoubleOr(e.getAttribute("alt"), 0));
    }

    private static double parseDoubleOr(String alt, double otherwise) {
        return alt == null || alt.isEmpty() ? otherwise : parseDouble(alt);
    }

    public float getLatitude() {
        return y;
    }

    public float getLongitude() {
        return x;
    }

    public float getAltitude() {
        return z;
    }
}
