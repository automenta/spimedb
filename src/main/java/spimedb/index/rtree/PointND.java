package spimedb.index.rtree;


import java.util.Arrays;

import static spimedb.index.rtree.RTree.FPSILON;


/**
 * Created by me on 12/21/16.
 */
public class PointND implements HyperPoint {

    public final float[] coord;

    public PointND(PointND copy) {
        this(copy.coord.clone());
    }

    public PointND(float... coord) {
        this.coord = coord;
    }

    public static PointND fill(int dims, float value) {
        float[] a = new float[dims];
        Arrays.fill(a, value);
        return new PointND(a);
    }

    @Override
    public int dim() {
        return coord.length;
    }

    @Override
    public Float coord(int d) {
        return coord[d];
    }

    @Override
    public double distance(HyperPoint h) {
        PointND p = (PointND) h;
        float sumSq = 0;
        for (int i = 0; i < coord.length; i++) {
            float x = coord[i];
            float y = p.coord[i];
            float xMinY = x - y;
            sumSq += xMinY * xMinY;
        }
        return Math.sqrt(sumSq);
    }

    @Override
    public double distance(HyperPoint p, int i) {
        return Math.abs(coord[i] - ((PointND) p).coord[i]);
    }

    @Override
    public boolean equals(Object obj) {
        //TODO use float epsilon tolerance
        if (this == obj) return true;
        PointND p = (PointND) obj;
        return RTree.equals(coord, p.coord, FPSILON);
    }

    @Override
    public int hashCode() {
        //TODO compute each component rounded to nearest epsilon?
        return Arrays.hashCode(coord);
    }

    @Override
    public String toString() {
        return '(' + Arrays.toString(coord) + ')';
    }


}
