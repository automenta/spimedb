package spimedb;

import org.apache.commons.math3.util.MathUtils;
import spimedb.web.Session;

/**
 * Created by me on 1/12/17.
 */
public class Tag extends NObject {

    float priority = 0;

    public Tag(NObject raw) {
        super(raw);
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
            reprioritize(prevValue, p);
        }
    }

    protected void reprioritize(float before, float after) {

    }

}
