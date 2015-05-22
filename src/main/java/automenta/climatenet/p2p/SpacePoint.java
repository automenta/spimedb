package automenta.climatenet.p2p;

import org.geojson.LngLatAlt;

import java.io.Serializable;

/**
 * Created by me on 4/23/15.
 */
public class SpacePoint extends LngLatAlt implements Serializable {
    //public String planet = "Earth";


    public SpacePoint(double lat, double lon) {
        super(lon, lat);
    }

    public SpacePoint(double lat, double lon, double alt) {
        super(lon, lat, alt);
    }

//        public GeoHash getGeoHash(int bits) {
//            /**
//             * This method uses the given number of characters as the desired precision
//             * value. The hash can only be 64bits long, thus a maximum precision of 12
//             * characters can be achieved.
//             */
//            return GeoHash.withBitPrecision(lat,lon, bits);
//        }

//        public String toString() {
//            String s = String.format("%.2f", lat) + "," + String.format("%.2f", lon);
//
//            if (!Double.isNaN(alt)) {
//                s += "," + alt;
//            }
//            return s;
//        }
}
