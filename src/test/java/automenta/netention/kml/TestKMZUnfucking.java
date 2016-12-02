package automenta.netention.kml;


import automenta.netention.db.SpimeGraph;
import automenta.netention.geo.ImportKML;
import automenta.netention.geo.SpimeBase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;


public class TestKMZUnfucking {

    @Test
    public void testKMLGeometry1() throws IOException {

        SpimeBase es = new SpimeGraph();

        URL v = TestKMZUnfucking.class.getClassLoader().getResource("WeeklyVolcanoGE-Reports.kmz");
        System.out.println(v);

        new ImportKML(es).file("main",
            new File(
                v.getPath()
            )
        ).run();


        es.forEach( x -> System.out.println(x) );

        assertEquals(12, es.size());
    }
}
