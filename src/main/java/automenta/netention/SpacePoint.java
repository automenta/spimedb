///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package automenta.netention;
//
//import java.io.Serializable;
//
///**
// *
// * @author me
// */
//public class SpacePoint implements Serializable {
//    //public String planet = "Earth";
//    public double lat;
//    public double lon;
//    public double alt; //in meters
//
//    public SpacePoint(double lat, double lon) {
//        this(lat, lon, 0 /*Double.NaN*/);
//    }
//
//    public SpacePoint(double lat, double lon, double alt) {
//        this.lat = lat;
//        this.lon = lon;
//        this.alt = alt;
//    }
//
////    public GeoHash getGeoHash(int bits) {
////        /**
////	 * This method uses the given number of characters as the desired precision
////	 * value. The hash can only be 64bits long, thus a maximum precision of 12
////	 * characters can be achieved.
////	 */
////        return GeoHash.withBitPrecision(lat,lon, bits);
////    }
//
//    public static SpacePoint get(NObject n) {
//        return (SpacePoint) n.get("G");
//    }
//
//    public String toString() {
//        String s = String.format("%.2f", lat) + ',' + String.format("%.2f", lon);
//
//        if (!Double.isNaN(alt)) {
//            s += "," + alt;
//        }
//        return s;
//    }
//}
