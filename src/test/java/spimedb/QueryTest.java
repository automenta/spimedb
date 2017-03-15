package spimedb;

import com.google.common.collect.Iterables;
import jcog.Util;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.junit.Test;
import spimedb.index.DObject;
import spimedb.index.SearchResult;
import spimedb.io.ImportSchemaOrg;
import spimedb.query.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by me on 6/13/15.
 */
public class QueryTest {

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



        db.sync();

        ArrayList<NObject> found = new ArrayList();
        SearchResult result = db.get(new Query(found::add)
            //.in("Place")
            .where(new double[]{0, 1}, new double[]{0, 1})
        );

        assertFalse(found.isEmpty());

        System.out.println(found);
        assertEquals(dplace.toString(), found.get(0).toString());

        assertTrue(found + "", found.contains(place));


    }

    @Test
    public void testDAGActivation() throws IOException {
        SpimeDB db = new SpimeDB();

        ImportSchemaOrg.load(db);

        //System.out.println(db.tag);

        MutableNObject place = new MutableNObject("civicstructure");
        place.where(0.5f, 0.5f);
        place.withTags("Place");
        db.add(place);

        MutableNObject action = new MutableNObject("action");
        action.where(0.5f, 0.5f);
        action.withTags("InteractAction");
        db.add(action);

        Set<String> placeSubtags = set( db.tagsAndSubtags("Place") );
        System.out.println(placeSubtags);
        Set<String> actionSubtags = set( db.tagsAndSubtags("Action") );
        System.out.println(actionSubtags);
        assertNotEquals(0, placeSubtags.size());
        assertNotEquals(0, actionSubtags.size());
        assertNotEquals(placeSubtags, actionSubtags);

        ArrayList<NObject> found = new ArrayList();
        db.get(new Query(found::add).in("InteractAction").where(new double[] { 0, 1}, new double[] { 0, 1}));

        assertFalse(found.isEmpty());
        assertTrue(found + "", found.contains(action));
        assertFalse(found.contains(place));

    }

    private Set<String> set(Iterable<String> t) {
        Set<String> s = new HashSet();
        Iterables.addAll(s, t);
        return s;
    }


}
