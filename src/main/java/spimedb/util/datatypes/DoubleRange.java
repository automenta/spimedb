/*
 *   __               .__       .__  ._____.           
 * _/  |_  _______  __|__| ____ |  | |__\_ |__   ______
 * \   __\/  _ \  \/  /  |/ ___\|  | |  || __ \ /  ___/
 *  |  | (  <_> >    <|  \  \___|  |_|  || \_\ \\___ \ 
 *  |__|  \____/__/\_ \__|\___  >____/__||___  /____  >
 *                   \/       \/             \/     \/ 
 *
 * Copyright (c) 2006-2011 Karsten Schmidt
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * http://creativecommons.org/licenses/LGPL/2.1/
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 */

package spimedb.util.datatypes;

import spimedb.util.math.MathUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class DoubleRange {

    public static DoubleRange fromSamples(double... samples) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double s : samples) {
            min = MathUtils.min(min, s);
            max = MathUtils.max(max, s);
        }
        return new DoubleRange(min, max);
    }

    public static DoubleRange fromSamples(List<Double> samples) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double s : samples) {
            min = MathUtils.min(min, s);
            max = MathUtils.max(max, s);
        }
        return new DoubleRange(min, max);
    }

    //@XmlAttribute
    public double min, max;

    //@XmlAttribute(name = "default")
    public double currValue;

    protected Random random = new Random();

    public DoubleRange() {
        this(0d, 1d);
    }

    public DoubleRange(double min, double max) {
        // swap if necessary...
        if (min > max) {
            double t = max;
            max = min;
            min = t;
        }
        this.min = min;
        this.max = max;
        this.currValue = min;
    }

    public double adjustCurrentBy(double val) {
        return setCurrent(currValue + val);
    }

    public DoubleRange copy() {
        DoubleRange range = new DoubleRange(min, max);
        range.currValue = currValue;
        range.random = random;
        return range;
    }

    /**
     * Returns the value at the normalized position <code>(0.0 = min ... 1.0 =
     * max-EPS)</code> within the range. Since the max value is exclusive, the
     * value returned for position 1.0 is the range max value minus
     * {@link MathUtils#EPS}. Also note the given position is not being clipped
     * to the 0.0-1.0 interval, so when passing in values outside that interval
     * will produce out-of-range values too.
     * 
     * @param perc
     * @return value within the range
     */
    public final double getAt(double perc) {
        return min + (max - min - MathUtils.EPS) * perc;
    }

    public double getCurrent() {
        return currValue;
    }

    public double getMedian() {
        return (min + max) * 0.5f;
    }

    public double getRange() {
        return max - min;
    }

    public boolean isValueInRange(float val) {
        return val >= min && val <= max;
    }

    public double pickRandom() {
        currValue = MathUtils.random(random, (float) min, (float) max);
        return currValue;
    }

    public DoubleRange seed(long seed) {
        random.setSeed(seed);
        return this;
    }

    public double setCurrent(double val) {
        currValue = MathUtils.clip(val, min, max);
        return currValue;
    }

    public DoubleRange setRandom(Random rnd) {
        random = rnd;
        return this;
    }

    public Double[] toArray(double step) {
        List<Double> range = new LinkedList<>();
        double v = min;
        while (v < max) {
            range.add(v);
            v += step;
        }
        return range.toArray(new Double[range.size()]);
    }

    @Override
    public String toString() {
        return "DoubleRange: " + min + " -> " + max;
    }
}