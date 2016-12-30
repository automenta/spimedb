package spimedb;

import org.junit.Test;
import spimedb.query.Query;
import spimedb.sense.ImportSchemaOrg;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

        System.out.println( db.tagsAndSubtags("Place") );
        System.out.println( db.tagsAndSubtags("Action") );

        ArrayList found = new ArrayList();
        db.get(new Query(found::add).in("Place").where(new float[] { 0, 1}, new float[] { 0, 1}));

        assertTrue(found.contains(place));
        assertFalse(found.contains(action));

    }


}
