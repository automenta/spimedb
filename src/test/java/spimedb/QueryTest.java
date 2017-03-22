package spimedb;

import org.junit.Test;
import spimedb.index.DObject;
import spimedb.index.SearchResult;
import spimedb.query.Query;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by me on 6/13/15.
 */
public class QueryTest {

    /** tag not specified, gets everything */
    @Test public void testSpacetimeIndexing() throws IOException {
        SpimeDB db = new SpimeDB();

        MutableNObject place = new MutableNObject("Somewhere");
        place.where(0.5f, 0.5f);
        place.withTags("Place");
        DObject dplace = db.add(place);

//        System.out.println(dplace);
//        for (IndexableField f : dplace.document.getFields()) {
//            System.out.println(f.name() + " = " + f.binaryValue());
//        }

        db.sync();

        ArrayList<DObject> found = new ArrayList();

        new Query()
            .where(new double[]{0, 1}, new double[]{0, 1})
            .forEach(db, (n, s) -> found.add(n));

        assertFalse(found.isEmpty());

        assertEquals(dplace.toString(), found.get(0).toString());

        assertTrue(found + "", found.contains(place));

    }

    @Test public void testSpacetimeTagIndexing() throws IOException {

        SpimeDB db = new SpimeDB();

        MutableNObject place = new MutableNObject("Somewhere");
        place.where(0.5f, 0.5f);
        place.withTags("Place");
        DObject dr = db.add(place);

        MutableNObject person = new MutableNObject("Someone");
        person.where(0.4f, 0.6f);
        person.withTags("Person");
        DObject dp = db.add(person);


        db.sync();

        ArrayList<NObject> found = new ArrayList();
        new Query()
            .in("Person")
            .where(new double[]{0, 1}, new double[]{0, 1})
            .forEach(db, (d, s) -> found.add(d));

        assertEquals(1, found.size());
        System.out.println(found);

        assertEquals(dp.toString(), found.get(0).toString());
        assertNotEquals(dr.toString(), found.get(0).toString());

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
