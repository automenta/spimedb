/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.sense;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.MutableNObject;
import spimedb.NObject;

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

    public static Stream<NObject> get(InputStream i, Function<Feature, NObject> builder) throws IOException {

        FeatureCollection featureCollection = geojsonMapper.readValue(i, FeatureCollection.class);
        logger.info("{} contained {} objects", i, featureCollection.getFeatures().size());
        return featureCollection.getFeatures().stream().map(builder);
    }

    /** TODO remove Earthquake specific tags into an extended builder */
    public static final Function<Feature, NObject> baseGeoJSONBuilder = (f) -> {
        MutableNObject d = new MutableNObject(f.getId());


        GeoJsonObject g = f.getGeometry();
        if (g != null) {
            if (g instanceof org.geojson.Point) {
                org.geojson.Point point = (org.geojson.Point) g;
                LngLatAlt coord = point.getCoordinates();
                d.where((float)coord.getLongitude(), (float)coord.getLatitude(), (float)coord.getAltitude());
            }
        }

        Object time = f.getProperty("time");
        if (time!=null) {
            if (time instanceof Long) {
                d.when(((Long)time));
            }
        }

        //EQ specific mappnig
        //Feature{properties={mag=6.2, place=33km S of Tolotangga, Indonesia, time=1483050618360, updated=1483052912295, tz=480, url=http://earthquake.usgs.gov/earthquakes/eventpage/us10007nl0, detail=http://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/us10007nl0.geojson, felt=73, cdi=7, mmi=4.81, alert=green, status=reviewed, tsunami=1, sig=642, net=us, code=10007nl0, ids=,us10007nl0,, sources=,us,, types=,dyfi,geoserve,losspager,origin,phase-data,shakemap,, nst=null, dmin=3.611, rms=1.52, gap=26, magType=mwp, type=earthquake, title=M 6.2 - 33km S of Tolotangga, Indonesia}, geometry=Point{coordinates=LngLatAlt{longitude=118.6088, latitude=-9.0665, altitude=72.27}} GeoJsonObject{}, id='us10007nl0'}
        d.name( f.getProperty("place") );
        d.setTag("Earthquake");
        d.put( "eqMag", f.getProperty("mag") );


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
    };

//    public static void main(String[] arg) throws Exception {
//        URL pointFile = new URL("http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.geojson");
//
//
//
//
//
//    }
}
