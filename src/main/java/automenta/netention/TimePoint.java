package automenta.netention;

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
        return (TimePoint) n.get("T");
    }

    public String toString() {
        return new Date(at).toString();
    }
}
