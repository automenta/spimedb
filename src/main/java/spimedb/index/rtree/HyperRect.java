package spimedb.index.rtree;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * An N dimensional rectangle or "hypercube" that is a representation of a data entry.
 * <p>
 * Created by jcairns on 4/30/15.
 */
public interface HyperRect<X> {

    /**
     * Calculate the resulting mbr when combining param HyperRect with this HyperRect
     *
     * @param r - mbr to add
     * @return new HyperRect representing mbr of both HyperRects combined
     */
    HyperRect<X> getMbr(HyperRect<X> r);

    /**
     * Get number of dimensions used in creating the HyperRect
     *
     * @return number of dimensions
     */
    int dim();

    /**
     * Get the minimum HyperPoint of this HyperRect
     *
     * @return min HyperPoint
     */
    HyperPoint getMin();

    /**
     * Get the minimum HyperPoint of this HyperRect
     *
     * @return min HyperPoint
     */
    HyperPoint getMax();

    /**
     * Get the HyperPoint representing the center point in all dimensions of this HyperRect
     *
     * @return middle HyperPoint
     */
    HyperPoint center();

    double center(int d);

    /**
     * Calculate the distance between the min and max HyperPoints in given dimension
     *
     * @param d - dimension to calculate
     * @return double - the numeric range of the dimention (min - max)
     */
    double getRange(final int d);

    default double getRangeFinite(int d, double elseValue) {
        double r = getRange(d);
        if (!Double.isFinite(r))
            return elseValue;
        else
            return r;
    }


    /**
     * Determines if this HyperRect fully contains parameter HyperRect
     *
     * @param r - HyperRect to test
     * @return true if contains, false otherwise
     */
    boolean contains(HyperRect r);

    /**
     * Determines if this HyperRect intersects parameter HyperRect
     *
     * @param r - HyperRect to test
     * @return true if intersects, false otherwise
     */
    boolean intersects(HyperRect r);

    /**
     * Calculate the "cost" of this HyperRect - usually the area across all dimensions
     *
     * @return - cost
     */
    double cost();

    /**
     * Calculate the perimeter of this HyperRect - across all dimesnions
     *
     * @return - perimeter
     */
    default double perimeter() {
        double p = 0.0;
        final int nD = this.dim();
        for (int d = 0; d < nD; d++) {
            p += 2.0 * this.getRange(d);
        }
        return p;
    }

    default double getRangeMin() {
        int dim = dim();
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < dim; i++) {
            double r = getRange(i);
            if (r < min) {
                min = r;
            }
        }
        return min;
    }

    /** whether any of the dimensions are finite */
    default boolean bounded() {
        int dim = dim();
        for (int i = 0; i < dim; i++) {
            double r = getRange(i);
            if (Double.isFinite(r))
                return true;
        }
        return false;
    }

}
