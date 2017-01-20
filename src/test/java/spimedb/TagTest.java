package spimedb;

import com.google.common.collect.Iterators;
import org.junit.Test;
import spimedb.input.GeoJSON;

import java.io.IOException;
import java.util.function.Consumer;

import static com.google.common.collect.Iterables.size;
import static org.junit.Assert.assertEquals;
import static spimedb.input.GeoJSONTest.eqGeoJson;

/**
 * Created by me on 1/14/17.
 */
public class TagTest {

    final SpimeDB db = new SpimeDB();

    @Test
    public void testTagActivationTrigger() {

        Tag v = new Tag("Test");
        db.put(v);

        System.out.println(db.tags.graph);

        Tag u = new Tag("Y", "Test");
        db.put(u);

        System.out.println(db.tags.graph);

        Tag t = new Tag("X", "Test");
        db.put(t);

        System.out.println(db.tags.graph);

        assertEquals(0, t.pri(), 0.01f);


        //+1 for the root tag, ""
        assertEquals(3+1, size(db.tagsAndSubtags()));
        assertEquals(1, Iterators.size(db.tags.roots()));
        assertEquals(1, size(db.tagsAndSubtags("X")));
        assertEquals(1, size(db.tagsAndSubtags("Y")));
        assertEquals(3, size(db.tagsAndSubtags("Test")));

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
                db.put(GeoJSON.get(eqGeoJson.get(), GeoJSON.baseGeoJSONBuilder));
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
        System.out.println(db.tags.toString());
    }

    @Test public void testGraphDecoration() {

        db.put(new Tag("Disaster"));
        db.put(new Tag("Hurricane", "Disaster"));

        assertEquals(null, db.graphed(""));
        assertEquals("{\"I\":\"Disaster\",\"inh\":{\">\":[\"Hurricane\"],\"<\":[\"\"]}}", db.graphed("Disaster").toString() );
        assertEquals("{\"I\":\"Hurricane\",\"inh\":{\"<\":[\"Disaster\"]}}", db.graphed("Hurricane").toString() );
    }

}
