package spimedb;

import org.junit.Test;
import spimedb.db.SpimeDB;
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

        System.out.println( db.children("Place") );
        System.out.println( db.children("Action") );

        ArrayList found = new ArrayList();
        db.intersecting(new float[] { 0, 1}, new float[] { 0, 1}, found::add, new String[] { "Place" });

        assertTrue(found.contains(place));
        assertFalse(found.contains(action));

    }


}
