package spimedb;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;
import spimedb.query.Query;
import spimedb.sense.ImportSchemaOrg;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

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

        Set<Vertex> placeSubtags = db.tagsAndSubtags("Place").collect(Collectors.toSet());
        System.out.println(placeSubtags);
        Set<Vertex> actionSubtags = db.tagsAndSubtags("Action").collect(Collectors.toSet());
        System.out.println(actionSubtags);
        assertNotEquals(0, placeSubtags.size());
        assertNotEquals(0, actionSubtags.size());
        assertNotEquals(placeSubtags, actionSubtags);

        ArrayList<NObject> found = new ArrayList();
        db.get(new Query(found::add).in("Place").where(new float[] { 0, 1}, new float[] { 0, 1}));

        assertTrue(found + "", found.contains(place));
        assertFalse(found.contains(action));

    }


}
