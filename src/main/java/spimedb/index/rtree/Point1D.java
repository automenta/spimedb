package spimedb.index.rtree;

/**
 * Created by me on 12/2/16.
 */
public class Point1D implements HyperPoint {
    private final double x;

    public Point1D(double X) {
        this.x = X;
    }

    @Override
    public int dim() {
        return 1;
    }

    @Override
    public Double coord(int d) {
        return x;
    }

    @Override
    public double distance(HyperPoint p) {
        return Math.abs(x - ((Point1D) p).x);
    }

    @Override
    public double distance(HyperPoint p, int d) {
        return distance(p);
    }
}
