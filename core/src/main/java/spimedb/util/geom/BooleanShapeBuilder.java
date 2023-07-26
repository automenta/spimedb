package spimedb.util.geom;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class BooleanShapeBuilder {

    public enum Type {
        UNION,
        INTERSECTION,
        DIFFERENCE,
        XOR
    }

    private final int bezierRes;

    private final Area area;
    private final Type type;

    public BooleanShapeBuilder(Type type) {
        this(type, 8);
    }

    public BooleanShapeBuilder(Type type, int bezierRes) {
        this.type = type;
        this.bezierRes = bezierRes;
        area = new Area();
    }

    public BooleanShapeBuilder addShape(Shape2D s) {
        return combineWithArea(new Area(convertToAWTShape(s)));
    }

    public BooleanShapeBuilder combineWithArea(Area a) {
        switch (type) {
            case UNION -> area.add(a);
            case INTERSECTION -> area.intersect(a);
            case DIFFERENCE -> area.subtract(a);
            case XOR -> area.exclusiveOr(a);
        }
        return this;
    }

    public List<Polygon2D> computeShapes() {
        List<Polygon2D> shapes = new ArrayList<>();
        PathIterator i = area.getPathIterator(null);
        float[] buf = new float[6];
        Vec2D prev = new Vec2D();
        Polygon2D s = null;
        while (!i.isDone()) {
            int id = i.currentSegment(buf);
            switch (id) {
                case PathIterator.SEG_MOVETO:
                    s = new Polygon2D();
                    shapes.add(s);
                    prev.set(buf[0], buf[1]);
                    s.add(prev.copy());
                    break;
                case PathIterator.SEG_LINETO:
                    prev.set(buf[0], buf[1]);
                    s.add(prev.copy());
                    break;
                case PathIterator.SEG_CUBICTO:
                    Vec2D pa = new Vec2D(buf[0], buf[1]);
                    Vec2D pb = new Vec2D(buf[2], buf[3]);
                    Vec2D pc = new Vec2D(buf[4], buf[5]);
                    for (int t = 0; t <= bezierRes; t++) {
                        s.add(BezierCurve2D.computePointInSegment(prev, pa, pb,
                                pc, (float) t / bezierRes));
                    }
                    prev.set(pc);
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Unsupported path segment type: " + id);
            }
            i.next();
        }
        return shapes;
    }

    private static Shape convertToAWTShape(Shape2D s) {
        if (s instanceof Rect) {
            Rect r = (Rect) s;
            return new Rectangle2D.Float(r.x, r.y, r.width, r.height);
        }
        if (s instanceof Triangle2D t) {
            Path2D path = new Path2D.Float();
            path.moveTo(t.a.x, t.a.y);
            path.lineTo(t.b.x, t.b.y);
            path.lineTo(t.c.x, t.c.y);
            path.closePath();
            return path;
        }
        if (s instanceof Ellipse e) {
            Vec2D r = e.getRadii();
            return new Ellipse2D.Float(e.x - r.x, e.y - r.y, r.x * 2, r.y * 2);
        }
        if (!(s instanceof Polygon2D)) {
            s = s.toPolygon2D();
        }
        Polygon2D poly = (Polygon2D) s;
        Path2D path = new Path2D.Float();
        Vec2D p = poly.get(0);
        path.moveTo(p.x, p.y);
        for (int i = 1, num = poly.getNumVertices(); i < num; i++) {
            p = poly.get(i);
            path.lineTo(p.x, p.y);
        }
        path.closePath();
        return path;
    }

    public Area getArea() {
        return area;
    }
}
