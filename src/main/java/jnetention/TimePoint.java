package jnetention;

import java.util.Date;

/**
 *
 * @author me
 */
public class TimePoint {

    public long at;    

    public TimePoint(long t) {
        this.at = t;
    }

    public static TimePoint get(NObject n) {   
        return n.firstValue(TimePoint.class);
    }

    public String toString() {
        return new Date(at).toString();
    }
}
