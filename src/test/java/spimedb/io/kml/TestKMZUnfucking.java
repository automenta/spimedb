package spimedb.io.kml;


import org.junit.Test;
import spimedb.SpimeDB;
import spimedb.io.KML;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;


public class TestKMZUnfucking {
    @Test public void testKMLGeometry1() throws IOException {
        SpimeDB es = new SpimeDB();
        URL v = TestKMZUnfucking.class.getClassLoader().getResource("WeeklyVolcanoGE-Reports.kmz");
        //System.out.println(v);

        new KML(es).file("main",
            new File(
                v.getPath()
            )
        ).run();


        es.forEach( x -> System.out.println(x) );

        assertEquals(24, es.size());

        System.out.println(es.tags.graph);
    }
}
