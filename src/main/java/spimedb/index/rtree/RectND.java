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
 * Created by jcovert on 6/15/15.
 */
public class RectND implements HyperRect<PointND> {

    final PointND min, max;

    RectND(final PointND p) {
        min = p;
        max = p;
    }

    RectND(float[] a, float[] b) {
        this(new PointND(a), new PointND(b));
    }

    RectND(final PointND a, final PointND b) {
        int dim = a.dim();

        float[] min = new float[dim];
        float[] max = new float[dim];

        float[] ad = a.coord;
        float[] bd = b.coord;
        for (int i = 0; i < dim; i++) {
            float ai = ad[i];
            float bi = bd[i];
            min[i] = Math.min(ai, bi);
            max[i] = Math.max(ai, bi);
        }
        this.min = new PointND(min);
        this.max = new PointND(max);
    }


    @Override
    public boolean contains(final HyperRect r) {
        final RectND x = (RectND) r;

        int dim = dim();
        for (int i = 0; i < dim; i++) {
            if (!(min.coord[i] <= x.min.coord[i] && max.coord[i] >= x.max.coord[i]))
                return false;
        }
        return true;
    }

    @Override
    public boolean intersects(final HyperRect r) {
        final RectND x = (RectND) r;

        int dim = dim();
        for (int i = 0; i < dim; i++) {
            if (min.coord[i] > x.max.coord[i] || x.min.coord[i] > max.coord[i])
                return false;
        }
        return true;
    }

    @Override
    public double cost() {
        float sigma = 1f;
        int dim = dim();
        for (int i = 0; i < dim; i++) {
            sigma *= max.coord[i] - min.coord[i];
        }
        return sigma;
    }

    @Override
    public HyperRect getMbr(final HyperRect r) {
        final RectND x = (RectND) r;

        int dim = dim();
        float[] newMin = new float[dim];
        float[] newMax = new float[dim];
        for (int i = 0; i < dim; i++) {
            newMin[i] = Math.min(min.coord[i], x.min.coord[i]);
            newMax[i] = Math.max(max.coord[i], x.max.coord[i]);
        }
        return new RectND(newMin, newMax);
    }


    @Override
    public HyperPoint center() {
        int dim = dim();
        float[] c = new float[dim];
        float[] minf = this.min.coord;
        float[] maxf = this.max.coord;
        for (int i = 0; i < dim; i++) {
            float min = minf[i];
            float max = maxf[i];
            c[i] = min + (max - min) / 2f;
        }
        return new PointND(c);
    }


    @Override
    public int dim() {
        return min.dim();
    }

    @Override
    public HyperPoint getMin() {
        return min;
    }

    @Override
    public HyperPoint getMax() {
        return max;
    }

    @Override
    public double getRange(final int i) {
        return max.coord[i] - min.coord[i];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null /*|| getClass() != o.getClass()*/) return false;

        RectND r = (RectND) o;
        return min.equals(r.min) && max.equals(r.max);
    }

    @Override
    public int hashCode() {
        int result = min.hashCode();
        result = 31 * result + max.hashCode();
        return result;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(min);
        sb.append(',');
        sb.append(max);
        sb.append(')');
        return sb.toString();
    }

    public final static class Builder implements RectBuilder<RectND> {

        @Override
        public HyperRect apply(final RectND rect2D) {
            return rect2D;
        }

    }

}