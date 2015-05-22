package automenta.climatenet.p2p;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO use a better discretization method (Ex: 1D SOM)
 * @author me
 */
public class TimeRange extends TimePoint {

    public long dt;

    public TimeRange(long from, long to) {
        super(from);
        this.dt = to - from;
    }

    @JsonIgnore
    @Override
    public boolean isInstant() { return dt == 0; }

    public long getDt() { return dt; }

    @JsonIgnore public long getMid() { return at + dt/2; }
    @JsonIgnore public long getEnd() { return at + dt; }

    public List<Long> discretize(long timePeriod) {
        List<Long> l = new ArrayList();
        long d = getDt();
        if (d < timePeriod) {
            //mid point
            l.add( getMid() );
        }
        else {
            //distribute the points evenly
            long remainder = d % timePeriod;
            long t = getStart() + remainder/2;
            while (t < getEnd()) {
                l.add( (t) );
                t+=timePeriod;
            }
        }
        return l;
    }

}
