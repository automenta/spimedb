package spimedb;

import org.junit.Test;
import spimedb.index.DObject;
import spimedb.index.Search;
import spimedb.query.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by me on 6/13/15.
 */
public class QueryTest {

    /**
     * tag not specified, gets everything
     */
    @Test
    public void testSpacetimeIndexing() throws IOException {
        SpimeDB db = new SpimeDB();

        MutableNObject place = new MutableNObject("Somewhere");
        place.where(0.5f, 0.5f);
        place.withTags("Place");
        DObject dplace = db.add(place);

//        System.out.println(dplace);
//        for (IndexableField f : dplace.document.getFields()) {
//            System.out.println(f.name() + " = " + f.binaryValue());
//        }

        assertFalse( db.sync(500) );

        List<NObject> found = new ArrayList();
        Search r = db.find(new Query()
                .where(new double[]{0, 1}, new double[]{0, 1})
        );
        r.forEach((d, s) -> found.add(d), 0, () -> {
            assertFalse(found.isEmpty());

            assertEquals(dplace.toString(), found.get(0).toString());

            assertTrue(found + "", found.contains(place));
        });
        assertEquals(1, r.localDocs.totalHits);


    }

    @Test
    public void testSpacetimeTagIndexing() throws IOException {

        SpimeDB db = new SpimeDB();

        MutableNObject place = new MutableNObject("Somewhere");
        place.where(0.5f, 0.5f);
        place.withTags("Place");
        DObject dr = db.add(place);

        MutableNObject person = new MutableNObject("Someone");
        person.where(0.4f, 0.6f);
        person.withTags("Person");
        DObject dp = db.add(person);


        db.sync(50);


        ArrayList<NObject> found = new ArrayList();
        db.find( new Query().in("Person") ).forEachLocal((d, s) -> found.add(d));

        found.forEach(f -> System.out.println(f));
        assertEquals(1, found.size());
        System.out.println(found);

        assertEquals(dp.toString(), found.get(0).toString());
        assertNotEquals(dr.toString(), found.get(0).toString());

    }


    @Test public void testInternationalDatelineSplit() {
        Query crossLeft = new Query().where(-185, -170, 10, 20);
        assertEquals(2, crossLeft.bounds.length);
        System.out.println( Arrays.toString(crossLeft.bounds) );
        assertEquals( -180, crossLeft.bounds[0].min.coord(1), 0.01f );
        assertEquals( 175, crossLeft.bounds[1].min.coord(1), 0.01f );

        Query crossRight = new Query().where(170, 185, 10, 20);
        assertEquals(2, crossRight.bounds.length);
        System.out.println( Arrays.toString(crossRight.bounds) );

    }
//    @Test
//    public void testDAGActivation() throws IOException {
//        SpimeDB db = new SpimeDB();
//
//        ImportSchemaOrg.load(db);
//
//        //System.out.println(db.tag);
//
//        MutableNObject place = new MutableNObject("civicstructure");
//        place.where(0.5f, 0.5f);
//        place.withTags("Place");
//        db.add(place);
//
//        MutableNObject action = new MutableNObject("action");
//        action.where(0.5f, 0.5f);
//        action.withTags("InteractAction");
//        db.add(action);
//
//        Set<String> placeSubtags = set( db.tagsAndSubtags("Place") );
//        System.out.println(placeSubtags);
//        Set<String> actionSubtags = set( db.tagsAndSubtags("Action") );
//        System.out.println(actionSubtags);
//        assertNotEquals(0, placeSubtags.size());
//        assertNotEquals(0, actionSubtags.size());
//        assertNotEquals(placeSubtags, actionSubtags);
//
//        ArrayList<NObject> found = new ArrayList();
//        db.get(new Query(found::add).in("InteractAction").where(new double[] { 0, 1}, new double[] { 0, 1}));
//
//        assertFalse(found.isEmpty());
//        assertTrue(found + "", found.contains(action));
//        assertFalse(found.contains(place));
//
//    }
//
//    private Set<String> set(Iterable<String> t) {
//        Set<String> s = new HashSet();
//        Iterables.addAll(s, t);
//        return s;
//    }


}
