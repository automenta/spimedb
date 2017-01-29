package spimedb.io.kml;


import org.junit.Test;
import spimedb.MutableNObject;
import spimedb.SpimeDB;
import spimedb.io.KML;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;


public class TestKMZUnfucking {
    @Test public void testKMLGeometry1() throws IOException {
        SpimeDB db = new SpimeDB();
        URL v = TestKMZUnfucking.class.getClassLoader().getResource("WeeklyVolcanoGE-Reports.kmz");
        //System.out.println(v);

        new KML(db, new MutableNObject("main")).file("main",
            new File(
                v.getPath()
            )
        ).run();

        db.sync();

        db.forEach( x -> System.out.println(x) );

        assertEquals(23, db.size());

        System.out.println(db.graph);
    }
}
