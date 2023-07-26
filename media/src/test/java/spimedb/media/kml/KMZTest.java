package spimedb.media.kml;


import org.junit.Test;
import spimedb.SpimeDB;
import spimedb.media.GeoNObject;
import spimedb.media.KML;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;


public class KMZTest {
    @Test public void testKMLGeometry1() throws IOException {
        SpimeDB db = new SpimeDB();
        URL v = KMZTest.class.getClassLoader().getResource("kml/WeeklyVolcanoGE-Reports.kmz");
        //System.out.println(v);

        new KML(db, new GeoNObject("main")).file("main",
            new File(
                v.getPath()
            )
        ).run();

        db.sync(50);

        db.forEach(System.out::println);

        assertEquals(23, db.size());

    }
}
