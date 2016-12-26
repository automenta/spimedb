package spimedb.index.rtree;

/**
 * Created by me on 12/2/16.
 */
public abstract class Rect1D implements HyperRect<Point1D> {


    abstract public double from();

    abstract public double to();

    @Override
    public HyperRect getMbr(HyperRect r) {

        Rect1D s = (Rect1D) r;
        double from = from();
        double to = to();
        double f = Math.min(from, s.from());
        double t = Math.max(to, s.to());
        return new DefaultRect1D(f, t);
    }

    @Override
    public int dim() {
        return 1;
    }

    @Override
    public HyperPoint getMin() {
        return new Point1D(from());
    }

    @Override
    public HyperPoint getMax() {
        return new Point1D(to());
    }

    @Override
    public HyperPoint center() {
        return new Point1D(center(0));
    }

    @Override
    public double center(int d) {
        assert (d == 0);
        return (from() + to()) / 2.0;
    }

    @Override
    public double getRange(int d) {
        return Math.abs(from() - to());
    }

    @Override
    public boolean contains(HyperRect r) {
        Rect1D inner = (Rect1D) r;
        return inner.from() >= from() && inner.to() <= to();
    }

    @Override
    public boolean intersects(HyperRect r) {
        Rect1D rr = (Rect1D) r;
        return (Math.max(from(), rr.from()) <= Math.min(to(), rr.to()));
    }

    @Override
    public double cost() {
        return getRange(0);
    }


    public static class DefaultRect1D extends Rect1D {


        private final double from, to;

        /**
         * point
         */
        public DefaultRect1D(double x) {
            this(x, x);
        }

        /**
         * line segment
         */
        public DefaultRect1D(double from, double to) {
            if (from > to)
                throw new UnsupportedOperationException("order fault");
            this.from = from;
            this.to = to;
        }


        @Override
        public String toString() {
            return "[" + from +  "," + to + "]";
        }

        @Override
        public double from() {
            return from;
        }

        @Override
        public double to() {
            return to;
        }
    }


}
