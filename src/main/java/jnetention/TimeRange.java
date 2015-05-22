/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jnetention;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author me
 */
public class TimeRange {

    public long from, to;

    public TimeRange(long from, long to) {
        this.from = from;
        this.to = to;
    }
    
    public static TimeRange get(NObject n) {
        return n.firstValue(TimeRange.class);
    }

    public long duration() { return to-from; }
    
    public List<TimePoint> discretize(long timePeriod) {
        List<TimePoint> l = new ArrayList();
        long d = duration();
        if (d < timePeriod) {
            //mid point
            l.add( new TimePoint((from + to)/2) );
        }
        else {
            //distribute the points evenly 
            long remainder = d % timePeriod;
            long t = from + remainder/2;
            while (t < to) {
                l.add(new TimePoint(t) );
                t+=timePeriod;
            }
        }
        return l;
    }
    
}
