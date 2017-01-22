package spimedb.io;

import org.junit.Test;
import spimedb.SpimeDB;
import spimedb.index.rtree.RectND;
import spimedb.query.QueryCollection;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.function.Supplier;

import static org.junit.Assert.assertTrue;

/**
 * Created by me on 12/29/16.
 */
public class GeoJSONTest {

    final SpimeDB db = new SpimeDB();

    public final static Supplier<InputStream> eqGeoJson = ()->new BufferedInputStream(GeoJSONTest.class.getClassLoader().getResourceAsStream("eq.geojson"), 1024);

    @Test
    public void test1() throws IOException {

        db.put(GeoJSON.get(eqGeoJson.get(), GeoJSON.baseGeoJSONBuilder));

        int all = db.size();
        assertTrue(all > 50);

        //time query
        QueryCollection a = new QueryCollection(new ArrayList<>()).when(1.48252053E12f, 1.48250336E12f);
        db.get(a);
        int aNum = a.result.size();
        assertTrue(aNum > 0);
        assertTrue(aNum < all/4);

//        System.out.println(a);
//        System.out.println(aNum + " / " + all + " found:");
//        System.out.println("\t" + a.result);
//
//        System.out.println();

        //time & space query (more restrictive): positive lon, positive lat quadrant
        QueryCollection b = new QueryCollection(new ArrayList<>()).bounds(
            new RectND(
                    new float[] {  1.48252053E12f, 0, 0, Float.NEGATIVE_INFINITY },
                    new float[] {  1.48250336E12f, 180, 180, Float.POSITIVE_INFINITY } )
        );
        db.get(b);
        int bNum = b.result.size();
        assertTrue(bNum > 0);
        assertTrue(bNum < aNum);
        System.out.println("\t" + b.result);

    }

}