package spimedb;

import org.apache.commons.math3.util.MathUtils;
import spimedb.server.Session;

import java.util.function.Consumer;

/**
 * Created by me on 1/12/17.
 */
public class Tag extends MutableNObject {

    float priority = 0;

    public Tag(String id, String... supers) {
        super(id);
        setTag(supers);
    }

    public Tag(MutableNObject rawSourceToCopyFrom) {
        super(rawSourceToCopyFrom);
    }

    public float pri() {
        return priority;
    }

    public void pri(Session who, float value) {
        setPriority( pri() + value);
    }

    public void priMult(float rate) {
        setPriority( pri() * rate );
    }

    private void setPriority(float p) {
        float prevValue = this.priority;

        p = Math.max(0f, Math.min(1f, p));

        if (!MathUtils.equals(prevValue,p)) {
            this.priority = p;
            reprioritized(prevValue, p);
        }
    }

    /** called after reprioritization */
    protected void reprioritized(float before, float after) {

    }


    /** assigned an activation and a deactivation procedure which are invoked
     *  if the priority crosses the threshold (default=0.5)
     *  this is analogous to a [+] branch node in a Tree view that when clicked, shows its contained children
     *
     *  the deactivation is analogous to an undo operation for the activation
     *
     *  TODO add a debouncing filter to prevent invocations resulting from high frequency priority oscillation
     */
    public static class ExpandingTag extends Tag {
        final float thresh = 0.5f;

        int activations = 0, deactivations = 0;
        public final Consumer<Tag> onActivate;
        public final Consumer<Tag> onDectivate;

        public ExpandingTag(String id, Consumer<Tag> onActivate, Consumer<Tag> onDectivate, String... supers) {
            super(id, supers);

            this.onActivate = onActivate;
            this.onDectivate = onDectivate;
        }

        protected void activate() {
            onActivate.accept(this);
            activations++;
        }

        protected void deactivate() {
            onDectivate.accept(this);
            deactivations++;
        }

        @Override
        protected void reprioritized(float before, float after) {
            if (before < thresh && after >= thresh) {
                SpimeDB.runLater(this::activate);
            } else if (before >= thresh && after < thresh) {
                SpimeDB.runLater(this::deactivate);
            }
        }
    }

}
