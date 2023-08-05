/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.media;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jcog.TODO;
import org.geojson.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.util.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.stream.Stream;


/**
 *
 * @see https://github.com/opendatalab-de/geojson-jackson
 * TODO write unit test, with the file stored locally as a test-resource
 */
public class GeoJSON   {
   //http://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson.php
    
    public final static ObjectMapper geojsonMapper = new ObjectMapper().
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    static final Logger logger = LoggerFactory.getLogger(GeoJSON.class);


    public static void load(String url, Function<Feature, NObject> builder, SpimeDB db) {
        try {
            db.addAsync(get(HTTP.inputStream(url), builder));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Stream<NObject> get(InputStream i, Function<Feature, NObject> builder) throws IOException {
        FeatureCollection featureCollection = geojsonMapper.readValue(i, FeatureCollection.class);
        logger.info("{} contained {} objects", i, featureCollection.getFeatures().size());
        return featureCollection.getFeatures().stream().map(builder);
    }



    public static class GeoJSONBuilder implements Function<Feature, NObject> {
        public final String tag;

        public GeoJSONBuilder(String tag) {
            this.tag = tag;
        }

        @Override
        public NObject apply(Feature f) {
            GeoNObject d = new GeoNObject(f.getId());

            String name = nameProperty(f); if (name!=null) d.name(name);
            String desc = descProperty(f); if (desc!=null) d.description(desc);
//            var m = f.getProperties();

            GeoJsonObject g = f.getGeometry();
            if (g != null) {
                if (g instanceof org.geojson.Point point) {
                    LngLatAlt coord = point.getCoordinates();
                    d.where(coord.getLongitude(), coord.getLatitude(), coord.getAltitude());
                } else if (g instanceof Polygon pg){
                    d.where(pg);
                } else if (g instanceof LineString ls) {
                    d.where(ls);
                } else {
                    throw new TODO();
                }

            }

            Object time = f.getProperty("time"); if (time instanceof Long l) d.when(l,l+1 /* HACK */);

            d.withTags(tag);

//            //EQ specific mappnig
//            //Feature{properties={mag=6.2, place=33km S of Tolotangga, Indonesia, time=1483050618360, updated=1483052912295, tz=480, url=http://earthquake.usgs.gov/earthquakes/eventpage/us10007nl0, detail=http://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/us10007nl0.geojson, felt=73, cdi=7, mmi=4.81, alert=green, status=reviewed, tsunami=1, sig=642, net=us, code=10007nl0, ids=,us10007nl0,, sources=,us,, types=,dyfi,geoserve,losspager,origin,phase-data,shakemap,, nst=null, dmin=3.611, rms=1.52, gap=26, magType=mwp, type=earthquake, title=M 6.2 - 33km S of Tolotangga, Indonesia}, geometry=Point{coordinates=LngLatAlt{longitude=118.6088, latitude=-9.0665, altitude=72.27}} GeoJsonObject{}, id='us10007nl0'}
//            d.name(f.getProperty("place"));
//            Object mag = f.getProperty("mag");
//            if (mag != null)
//                d.put("eqMag", mag.toString());


//                    /*if (g instanceof Circle) {
//
//                    }
//                    else */if (g instanceof Point) {
//                Point pp = (Point) g;
//                d.where(pp.getCenter());
//
//            } else if (g instanceof org.opensextant.giscore.geometry.LinearRing) {
//                logger.warn("unhandled geometry type: {}: {}", g.getClass(), g );
//
//            } else if (g instanceof org.opensextant.giscore.geometry.Line) {
//                org.opensextant.giscore.geometry.Line l = (org.opensextant.giscore.geometry.Line) g;
//                d.where( l );
//            } else if (g instanceof org.opensextant.giscore.geometry.MultiLinearRings) {
//                logger.warn("unhandled geometry type: {}: {}", g.getClass(), g );
//            } else if (g instanceof org.opensextant.giscore.geometry.Polygon) {
//                org.opensextant.giscore.geometry.Polygon p = (org.opensextant.giscore.geometry.Polygon) g;
//                d.where(p);
//            } else {
//                logger.warn("unhandled geometry type: {}: {}", g.getClass(), g );
//            }
//
//            //TODO other types
//        }

            return d;
        }

        @Nullable private static String descProperty(Feature f) {
            String s = f.getProperty("description");
            if (s != null) return s;
            return f.getProperty("Description");
        }

        @Nullable private static String nameProperty(Feature f) {
            String s = f.getProperty("name");
            if (s != null) return s;
            return f.getProperty("Name");
        }


    }

//    public static void main(String[] arg) throws Exception {
//        URL pointFile = new URL("http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.geojson");
//
//
//
//
//
//    }
}
