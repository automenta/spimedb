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

        MutableNObject place = new MutableNObject("civicstructure");
        place.where(0.5f, 0.5f);
        place.setTag("Place");
        db.put(place);

        MutableNObject action = new MutableNObject("action");
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
        db.get(new Query(found::add).in("InteractAction").where(new float[] { 0, 1}, new float[] { 0, 1}));

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
