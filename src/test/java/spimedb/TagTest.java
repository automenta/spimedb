package spimedb;

import org.junit.Test;
import spimedb.sense.GeoJSON;

import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static spimedb.sense.GeoJSONTest.eqGeoJson;

/**
 * Created by me on 1/14/17.
 */
public class TagTest {

    final SpimeDB db = new SpimeDB();

    @Test
    public void testTagActivationTrigger() {

        Tag u = new Tag("Y", "Test");
        Tag t = new Tag("X", "Test");

        db.put(u);
        db.put(t);

        assertEquals(0, t.pri(), 0.01f);

        assertEquals(3, db.tagsAndSubtags().count());
        assertEquals(1, db.tagsAndSubtags("X").count());
        assertEquals(1, db.tagsAndSubtags("Y").count());
        assertEquals(3, db.tagsAndSubtags("Test").count());

        t.pri(null, 0.75f); //activate
        assertEquals(0.75f, t.pri(), 0.01f);

//        assertEquals(1, activations.intValue());
//        assertEquals(0, deactivations.intValue());

        t.pri(null, -0.5f); //deactivate
        assertEquals(0.25f, t.pri(), 0.01f);

//        assertEquals(1, activations.intValue());
//        assertEquals(1, deactivations.intValue());
    }

    @Test
    public void testExpandingTag() {

        Consumer<Tag> onGeoJSONActivate = (t) -> {
            try {
                db.put(GeoJSON.get(eqGeoJson, GeoJSON.baseGeoJSONBuilder));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        Consumer<Tag> onDeact = (t) -> {

        };

        Tag u = new Tag.ExpandingTag("Earthquake", onGeoJSONActivate, onDeact, "Disaster");
        db.put(u);

        Tag t = new Tag("Hurricane", "Disaster");
        db.put(t);


        u.pri(null, 0.75f); //activate

        SpimeDB.sync();

        System.out.println(db.toString());
        System.out.println(db.schema.toString());
    }
}
