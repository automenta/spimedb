package spimedb.util;

import spimedb.util.geom.Rect;

/** maintains a short-term history of a series of rectangular focus
 * requests, to compute if a new request's area can be culled
 * TODO
  */
public class RectangleFocusHistory {

    static final class Req extends Rect {
        public final long when;

        Req(float x1, float y1, float x2, float y2, long when) {
            super((x1 + x2)/2f, (y1+y2)/2f, (x2-x1), (y2-y1));
            this.when = when;
        }
    }

    //CircularArrayList<Req> history;
}
