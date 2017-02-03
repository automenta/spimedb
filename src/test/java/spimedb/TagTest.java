package spimedb;

import com.google.common.collect.Iterators;
import org.junit.Test;

import java.io.IOException;

import static com.google.common.collect.Iterables.size;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 1/14/17.
 */
public class TagTest {



    @Test
    public void testTagActivationTrigger() throws IOException {

        final SpimeDB db = new SpimeDB();

        Tag v = new Tag("Test");
        db.add(v);

        System.out.println(db.graph);

        Tag u = new Tag("Y", "Test");
        db.add(u);

        System.out.println(db.graph);

        Tag t = new Tag("X", "Test");
        db.add(t);

        System.out.println(db.graph);

        assertEquals(0, t.pri(), 0.01f);


        //+1 for the root tag, ""
        assertEquals(3+1, size(db.tagsAndSubtags()));
        assertEquals(1, Iterators.size(db.roots()));
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

//    @Test
//    public void testExpandingTag() throws IOException {
//
//        final SpimeDB db = new SpimeDB();
//
//        Consumer<Tag> onGeoJSONActivate = (t) -> {
//            try {
//                db.add(GeoJSON.get(eqGeoJson.get(), GeoJSON.baseGeoJSONBuilder));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        };
//        Consumer<Tag> onDeact = (t) -> {
//
//        };
//
//        Tag u = new Tag.ExpandingTag("Earthquake", onGeoJSONActivate, onDeact, "Disaster");
//        db.add(u);
//
//        Tag t = new Tag("Hurricane", "Disaster");
//        db.add(t);
//
//
//        u.pri(null, 0.75f); //activate
//
//        db.sync();
//
//        System.out.println(db.toString());
//    }

    @Test public void testGraphDecoration() throws IOException {

        final SpimeDB db = new SpimeDB();

        db.add(new Tag("Disaster"));
        db.add(new Tag("Hurricane", "Disaster"));

        db.sync();

        assertEquals(null, db.graphed(""));
        assertEquals("{\"I\":\"Disaster\",\"inh\":{\">\":[\"Hurricane\"],\"<\":[\"\"]}}", db.graphed("Disaster").toString() );
        assertEquals("{\"I\":\"Hurricane\",\"inh\":{\"<\":[\"Disaster\"]}}", db.graphed("Hurricane").toString() );
    }

}
