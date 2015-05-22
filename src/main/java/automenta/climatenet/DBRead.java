///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package automenta.climatenet;
//
//import java.util.HashMap;
//import java.util.Map;
//import org.geotools.data.DataStore;
//import org.geotools.data.DataStoreFinder;
//import org.geotools.data.FeatureSource;
//import org.geotools.data.Query;
//import org.geotools.feature.FeatureCollection;
//import org.geotools.feature.FeatureIterator;
//
///**
// *
// * @see http://docs.geotools.org/stable/userguide/library/jdbc/postgis.html
// */
//public class DBRead {
// 
//    public static void main(String[] arg) throws Exception {
//            Map params = new HashMap();
//            params.put("dbtype", "postgis");        //must be postgis
//            params.put("host", "localhost");        //the name or ip address of the machine running PostGIS
//            params.put("port", 5432);  //the port that PostGIS is running on (generally 5432)
//            
//            params.put("database", "cv");      //the name of the database to connect to.
//            params.put("user", "me");         //the user to connect with
//            params.put("passwd", "");               //the password of the user.
//
//            DataStore pgDatastore = DataStoreFinder.getDataStore(params);
//            
//                                
//            if (pgDatastore == null) throw new RuntimeException("Can not connect to database: " + params);
//            
//            String layer = "cvr01";
//            
//            
//            System.out.println(pgDatastore.getSchema(layer));
//            
//            
//            FeatureSource f = pgDatastore.getFeatureSource(layer);
//
//            //System.out.println("count: " + fsBC.getCount(Query.ALL));        
//            
//            FeatureCollection c = f.getFeatures();
//            
//            System.out.println(c + " " + c.size());
//            
//            FeatureIterator ci = c.features();
//            while (ci.hasNext()) {
//                try {
//                    System.out.println(ci.next());
//                }
//                catch (Exception e) {
//                    System.err.println(e);
//                }
//            }
//
//    }
//}
