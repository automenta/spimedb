/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.netention.geo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.FeatureCollection;

import java.net.URL;


/**
 *
 * @see https://github.com/opendatalab-de/geojson-jackson
 */
public class ImportGeoJSON {
   //http://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson.php
    
    public final static ObjectMapper geojsonMapper = new ObjectMapper().
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    
    public static void main(String[] arg) throws Exception {
        URL pointFile = new URL("http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.geojson"); 
        
        
       FeatureCollection featureCollection = 
            geojsonMapper.readValue(pointFile.openStream(), FeatureCollection.class);
       
       System.out.println(featureCollection.getFeatures());
       
    }
}
