package spimedb.ui;

import com.google.common.base.Joiner;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import jcog.data.list.Lst;
import jcog.math.v2;
import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.rect.HyperRectFloat;
import jcog.tree.rtree.rect.RectF;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.impl.block.factory.Comparators;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.state.FingerMove;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.unit.Animating;
import spacegraph.space2d.container.unit.Clipped;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.geo.osm.Osm;
import spacegraph.util.geo.osm.OsmElement;
import spacegraph.util.geo.osm.OsmNode;
import spacegraph.util.geo.osm.OsmWay;
import spacegraph.video.Draw;
import spacegraph.video.OsmSpace;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static spacegraph.space2d.container.grid.Containers.grid;

public class OsmSurface extends PaintSurface {


//    private static final Consumer<GL2> loading = (gl) -> {
//        gl.glColor3f(1, 0, 0);
//        Draw.rectFrame(0, 0, 1, 1, 0.1f, gl);
//    };
    public final AtomicBoolean debugIndexBounds = new AtomicBoolean(false);



    final v2 translate = new v2();
    private final RTree<OsmElement> index;
    public final OsmSpace.LonLatProjection projection =
            //new OsmSpace.RawProjection();
            new OsmSpace.ECEFProjection();

    final FingerMove rotate = new FingerMove(2) {

        @Override
        protected void move(float tx, float ty) {
            projection.rotate(tx, ty);
        }
    };
    final FingerMove pan = new FingerMove(0) {

        @Override
        protected boolean incremental() {
            return true;
        }

        @Override
        public void move(float tx, float ty) {
            projection.pan(tx, ty, bounds);
        }
    };

    private final List<OsmElement> hilight = new Lst(128);
    @Deprecated
    public final Osm o = new Osm();
    double[] touch = new double[3];

    public OsmSurface(RTree<OsmElement> i) {
        this.index = i;
    }

    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        gl.glPushMatrix();

        gl.glTranslatef(
                translate.x + bounds.x + bounds.w / 2,
                translate.y + bounds.y + bounds.h / 2, 0); //center in view


        projection.transform(gl, bounds);

        {
            renderMap(gl);

            //                renderIndexBounds(gl);
            if (debugIndexBounds.get()) renderTouchedIndexBounds(gl);

            for (OsmElement each : hilight) renderBounds(gl, each);

        }

        projection.untransform(gl, bounds);

        gl.glPopMatrix();

    }

    private void renderTouchedIndexBounds(GL2 gl) {
        index.root().intersectingNodes(HyperRectFloat.cube(touch, 0), n -> {
            renderBounds(gl, n.bounds());
            return true;
        }, index.model);
    }

    private void renderIndexBounds(GL2 gl) {

        gl.glLineWidth(2);

        index.root().streamNodesRecursively().forEach(n -> renderBounds(gl, n.bounds()));
    }

    private void renderBounds(GL2 gl, HyperRegion b) {
        if (b instanceof OsmWay)
            b = ((OsmWay) b).bounds();
        if (b instanceof HyperRectFloat) {
            rect(gl, (HyperRectFloat) b);
        }
    }

    private void rect(GL2 gl, HyperRectFloat r) {
        float x1 = r.min.coord(0), y1 = r.min.coord(1);
        float x2 = r.max.coord(0), y2 = r.max.coord(1);

        double[] ff = new double[3];
        projection.project(x1, y1, 0, ff, 0);
        x1 = (float) ff[0];
        y1 = (float) ff[1];
        projection.project(x2, y2, 0, ff, 0);
        x2 = (float) ff[0];
        y2 = (float) ff[1];

        gl.glLineWidth(4);
        Draw.colorHash(gl, r.hashCode(), 0.75f);
        //Draw.rect(
        Draw.rectStroke(
                x1, y1, x2 - x1, y2 - y1,
                gl
        );
    }

    private void renderMap(GL2 gl) {

        RectF b = o.geoBounds;
        if (b == null)
            return;

        synchronized (index) {

            Consumer<GL2> renderProc;

//                if (!o.ready)
//                    renderProc = loading;
//                else {
            GLContext ctx = gl.getContext();
            Object c = ctx.getAttachedObject(o.id);
            if (c != null && projection.changed()) {
                //detach and create new
                ctx.detachObject(o.id);
                c = null;
            }

            if (c == null) {

                c = new OsmSpace.OsmRenderer(gl, projection);

                OsmSpace.OsmRenderer r = ((OsmSpace.OsmRenderer) c);
//                        HyperRectFloat viewBounds = new HyperRectFloat(
//                                new float[] { },
//                                new float[] { }
//                        );
                o.ways.forEach(r::addWay);

                for (OsmNode osmNode : o.nodes.values()) r.addNode(osmNode);
//                        //index.forEach(e -> {//whileEachIntersecting(viewBounds,e->{
//                            if (e instanceof OsmWay)
//                                r.addWay((OsmWay)e);
//                            else if (e instanceof OsmNode)
//                                r.addNode((OsmNode)e);
//                            //return true;
//                        });
                ctx.attachObject(o.id, c);
                projection.changeNoticed();
            }
            renderProc = (Consumer<GL2>) c;
//                }

            renderProc.accept(gl);

//                /* debug */ {
//                    gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
//                    Draw.rectFrame(gl, b.cx(), b.cy(),
//                            b.w, b.h, 0.0001f);
//
//                }
        }
    }

    public void changed() {
        projection.changed = true;
    }

    public OsmSurface load(Osm x) {

        synchronized (index) {
            Consumer<OsmElement> index = xx -> {
                if (xx.tags != null && !xx.tags.isEmpty())
                    this.index.add(xx);
            };

            for (OsmNode osmNode : x.nodes.values())
                index.accept(osmNode);

            for (OsmWay osmWay : x.ways.values())
                index.accept(osmWay);
//            o.relations.values().forEach(i.index::addAt);
//            System.out.println(o.nodes.size() + " nodes");

//        System.out.println(o);
//        for (OsmNode n : o.nodes.values()) {
//            if (n.tags != null)
//                System.out.println(n);
//        }

            this.o.addAll(x);
        }

        changed();

        return this;
    }

    public OsmSurface go(Osm x) {
        load(x);
        projection.center(x.geoBounds.cx(), x.geoBounds.cy());
        return this;
    }


    @Override
    public Surface finger(Finger finger) {



        float wheel;
        if ((wheel = finger.rotationY(true)) != 0) projection.zoom(wheel);

        hilight.clear();

        if (finger.test(pan) || finger.test(rotate)) return this;
        else {
            //v2 pos = finger.posGlobal(); //posPixel;

            v2 pos = finger.posGlobal();
            float wx = (pos.x - cx());
            float wy = (pos.y - cy());
            float wz = 0;

            //TODO unproject screen to world

            projection.unproject(wx, wy, wz, touch);
            //System.out.println(n4(wx,wy,wz) + " -> " + n4(touch));

//            float[] untouch = new float[3];
//            projection.project(touch[0], touch[1], touch[2], untouch, 0);
//            System.out.println("  " + n4(untouch[0] - wx) + " " + n4(untouch[1] - wy));

            float rad = 0.0000f;
            HyperRectFloat cursor = HyperRectFloat.cube(touch, rad);
            index.intersectsWhile(cursor, (each) -> {
                //System.out.println(each.tags);
                if (each.tags != null) hilight.add(each);
                return true;
            });

        }

        return null;
    }

    public Surface widget() {
        return
                new Stacking(
                    new Clipped(this)
                    ,grid(new EmptySurface(), new EmptySurface(), new EmptySurface(),
                        new Animating<>(new BitmapLabel(), new Consumer<>() {

                            OsmElement last;

                            @Override
                            public void accept(BitmapLabel b) {

                                List<OsmElement> h = OsmSurface.this.hilight;
                                if (!h.isEmpty()) try {
                                    OsmElement hh = ((Lst<OsmElement>) h).min(
                                        Comparators.byDoubleFunction((DoubleFunction<OsmElement>)HyperRegion::perimeter)
                                    );
                                    //
                                    //                            }
                                    //                            int hn = h.size();
                                    //                            if (hn > 1) {
                                    //                                b.text(hn + " objects");
                                    //                            } else if (hn == 1) {
                                    if (hh != null && hh != last) {
                                        b.text(Joiner.on("\n").join(hh.tags.entrySet()));
                                        last = hh;
                                    }
                                } catch (RuntimeException t) {
                                    b.text("");
                                    //ignored HACK
                                }
                                else
                                    b.text("");
                            }
                        }, 0.05f)
//                        Gridding.col(
//                            new AnimLabel(()->"translation: " + translate.toString()),
//                            new AnimLabel(()->"scale: " + scale.toString())
//                        )
                )
            )
    ;
    }

    private static class AnimLabel extends VectorLabel {
        final Supplier<String> text;

        public AnimLabel(Supplier<String> text) {
            this.text = text;
        }

        @Override
        protected void renderContent(ReSurface r) {
            text(text.get());
            super.renderContent(r);
        }

    }


}