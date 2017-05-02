package spimedb.media.geojson;

import org.junit.Test;
import spimedb.SpimeDB;
import spimedb.media.GeoJSON;
import spimedb.query.Query;
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



    public final static Supplier<InputStream> eqGeoJson = ()->new BufferedInputStream(GeoJSONTest.class.getClassLoader().getResourceAsStream("geojson/eq.geojson"), 1024);

    @Test
    public void test1() throws IOException {

        final SpimeDB db = new SpimeDB();

        db.add(GeoJSON.get(eqGeoJson.get(), GeoJSON.baseGeoJSONBuilder));

        db.sync();


        int all = db.size();
        assertTrue(all > 50);

        //time query
        QueryCollection a = new QueryCollection(
                new Query()
                        .when(1.48252053E12f, 1.48250336E12f),
                new ArrayList<>()
        ).get(db);

        int aNum = a.result.size();
        assertTrue(aNum > 0);
        assertTrue(aNum < all/4);

        db.sync();

//        System.out.println(a);
//        System.out.println(aNum + " / " + all + " found:");
//        System.out.println("\t" + a.result);
//
//        System.out.println();

        //time & space query (more restrictive): positive lon, positive lat quadrant
        QueryCollection b = new QueryCollection(new Query().where(
                new double[]{1.48252053E12f, -90, 90, Double.NEGATIVE_INFINITY},
                new double[]{1.48250336E12f, -90, 90, Double.POSITIVE_INFINITY}
        ), new ArrayList<>()).get(db);

        int bNum = b.result.size();
        assertTrue(bNum > 0);
        System.out.println("\t" + b.result + "\n" + "bNum=" + bNum + ", aNum=" + aNum);
        assertTrue(bNum != aNum);

    }

}