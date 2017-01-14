package spimedb;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 1/14/17.
 */
public class TagTest {

    @Test
    public void testTagActivationTrigger() {
        SpimeDB s = new SpimeDB();

        AtomicInteger activations = new AtomicInteger(0);
        AtomicInteger deactivations = new AtomicInteger(0);

        Tag t = new Tag("X", "Test") {

            protected void activate() {
                activations.incrementAndGet();
            }

            protected void deactivate() {
                deactivations.incrementAndGet();
            }

            @Override
            protected void reprioritize(float before, float after) {
                float thresh = 0.5f;
                if (before < thresh && after >= thresh) {
                    activate();
                } else if (before >= thresh && after < thresh) {
                    deactivate();
                }
            }
        };

        s.put(t);

        assertEquals(0, t.pri(), 0.01f);

        Set<Vertex> all = s.tagsAndSubtags().collect(Collectors.toSet());
        assertEquals(2, all.size());
        Set<Vertex> xOnly = s.tagsAndSubtags("X").collect(Collectors.toSet());
        assertEquals(1, xOnly.size());

        t.pri(null, 0.75f); //activate
        assertEquals(0.75f, t.pri(), 0.01f);

        assertEquals(1, activations.intValue());
        assertEquals(0, deactivations.intValue());

        t.pri(null, -0.5f); //deactivate
        assertEquals(0.25f, t.pri(), 0.01f);

        assertEquals(1, activations.intValue());
        assertEquals(1, deactivations.intValue());
    }
}
