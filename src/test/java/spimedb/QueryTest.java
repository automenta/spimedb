package spimedb;

import com.google.common.collect.Iterables;
import org.junit.Test;
import spimedb.query.Query;
import spimedb.sense.ImportSchemaOrg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by me on 6/13/15.
 */
public class QueryTest {

    @Test
    public void testDAGActivation() {
        SpimeDB db = new SpimeDB();

        ImportSchemaOrg.load(db);

        //System.out.println(db.tag);

        NObject place = new NObject("civicstructure");
        place.where(0.5f, 0.5f);
        place.setTag("CivicStructure");
        db.put(place);

        NObject action = new NObject("action");
        action.where(0.5f, 0.5f);
        action.setTag("InteractAction");
        db.put(action);

        Set<String> placeSubtags = set( db.tagsAndSubtags("Place") );
        System.out.println(placeSubtags);
        Set<String> actionSubtags = set( db.tagsAndSubtags("Action") );
        System.out.println(actionSubtags);
        assertNotEquals(0, placeSubtags.size());
        assertNotEquals(0, actionSubtags.size());
        assertNotEquals(placeSubtags, actionSubtags);

        ArrayList<NObject> found = new ArrayList();
        db.get(new Query(found::add).in("Place").where(new float[] { 0, 1}, new float[] { 0, 1}));

        assertTrue(found + "", found.contains(place));
        assertFalse(found.contains(action));

    }

    private Set<String> set(Iterable<String> t) {
        Set<String> s = new HashSet();
        Iterables.addAll(s, t);
        return s;
    }


}
