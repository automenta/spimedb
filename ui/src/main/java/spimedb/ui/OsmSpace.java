package spimedb.ui;


import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import com.jogamp.opengl.math.FloatUtil;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.math.v2;
import jcog.math.v3;
import jcog.signal.FloatRange;
import jcog.tree.rtree.rect.RectF;
import jcog.util.ArrayUtil;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.video.Draw;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL.GL_LINE_STRIP;
import static com.jogamp.opengl.GL.GL_POINTS;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

/**
 * OSM Renderer context
 * Created by unkei on 2017/04/25.
 */
public enum OsmSpace { ;

//    public static final Logger logger = LoggerFactory.getLogger(OsmSpace.class);

    public abstract static class LonLatProjection {

        public boolean changed = false;

        public final double[] project(float lon, float lat, float alt) {
            double[] d = new double[3];
            project(lon, lat, alt, d);
            return d;
        }

        public final void project(float lon, float lat, float alt, double[] target) {
            project(lon, lat, alt, target, 0);
        }

        public abstract void project(float lon, float lat, float alt, double[] target, int offset);

        public abstract void unproject(float x, float y, float z, double[] target);

        public final double[] project(GeoVec3 global, double[] target) {
            return project(global, target, 0);
        }

        public final double[] project(GeoVec3 global, double[] target, int offset) {
            project(global.x /* lon */, global.y /* lat */, global.z /* alt */, target, offset);
            return target;
        }

        /** notifies the renderer that points will need reprojected */
        public final boolean changed() {
            return changed;
        }

        public final void changeNoticed() {
            changed = false;
        }

        public abstract void transform(GL2 gl, RectF bounds);

        public abstract void pan(float tx, float ty, RectF bounds);

        public abstract void zoom(float wheel);

        public abstract void center(float lon, float lat);

        public void untransform(GL2 gl, RectF bounds) {

        }

        public void rotate(float tx, float ty) {
            /* impl, if applicable */
        }
    }

    /** for debugging; this will be distorted, increasingly towards the poles (extreme latitudes) */
    public static final class RawProjection extends LonLatProjection {
        private final v2 center = new v2();

        /** TODO move scale, center, translate to a generic 2D projection impl */
        final FloatRange scale = new FloatRange(16.0f, 0.001f, 1000.0f);
        private float viewScale;

        @Override
        public void project(float lon, float lat, float alt, double[] target, int offset) {
            target[offset++] = lon;
            target[offset++] = lat;
            target[offset] = alt;
        }

        @Override
        public void unproject(float x, float y, float z, double[] target) {
            float scale = viewScale;
            target[0] = (x/scale+ center.x) ;
            target[1] = (y/scale+ center.y) ;
            target[2] = 0;
        }

//        final v2 camVel = new v2();
//        private final float camMomentum = 0.5f;

        @Override
        public void transform(GL2 gl, RectF bounds) {

//            center.added(camVel);
//            camVel.multiplyEach(camMomentum);

            viewScale = scale(bounds);

            gl.glScalef(viewScale, viewScale, 1);
            gl.glTranslatef(-center.x, -center.y, 0);

        }

        float scale(RectF bounds) {
            return this.scale.floatValue() * Math.max(bounds.w, bounds.h);
        }

        @Override
        public void pan(float tx, float ty, RectF bounds) {
            float speed = 0.5f;
            float s =
                    //1f / viewScale;
                    speed / (viewScale);

//            camVel.set(tx * s, ty * s);
//            center.added(camVel);
            center.added(tx*s, ty*s);
//            camVel.multiplyEach(camMomentum);

        }

        @Override
        public void zoom(float wheel) {
            scale.mul(1.0f - wheel * 0.1f);
        }

        @Override
        public void center(float lon, float lat) {
            center.set(lon, lat);
        }
    }

    public static final class ECEFProjection extends LonLatProjection {

        public final v3 camFwd = new v3(0, 1, 0);
        public final v3 camUp = new v3(0, 0, -1);
        final v3 camPos = new v3(0,0,-100);
        final v3 rot = new v3();

        float alt = 0.0f;
        private final float[] mat4f = new float[16];

        final FloatRange scale = new FloatRange(1/ 10000.0f, 1/ 250_000.0f, 1.0f);
        private float lon, lat;


//        @Override
//        public void rotate(float tx, float ty) {
//            rot.add(0*tx/400000f,0*ty/400000f, ty/400000f);
//        }

        @Override
        public void unproject(float x, float y, float z, double[] target) {
            ECEF.ecef2latlon(x , y , z, target);
        }

        @Override
        public void project(float lon, float lat, float alt, double[] target, int offset) {
            ECEF.latlon2ecef(lat , lon , alt, target, offset);
        }

        @Override
        public void transform(GL2 gl, RectF bounds) {


//

            gl.glMatrixMode(GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            float zNear = 0.001f, zFar = 120;
//            float tanFovV = (float) Math.tan(45 * FloatUtil.PI / 180.0f / 2f);
            float aspect =
                    //1;
                    //bounds.h / bounds.w;
                    bounds.w / bounds.h;
//            float top = tanFovV * zNear;
//            float right = aspect * top;
//            float bottom = -top;
//            float left = -right;

            gl.glMultMatrixf(FloatUtil.makePerspective(mat4f, 0, true, 45 * FloatUtil.PI / 180.0f, aspect, zNear, zFar), 0);
//
            Draw.glu.gluLookAt(0 + camFwd.x, 0 + camFwd.y, 0 + camFwd.z,
                    0, 0, 0,
//                    camPos.x, camPos.y, camPos.z,
                    camUp.x, camUp.y, camUp.z);

            gl.glMatrixMode(GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();

            //System.out.println(camPos + " " + rot);

            gl.glTranslatef(camPos.x, camPos.y, camPos.z);

            gl.glRotatef(rot.x, 0, 0, 1);
            gl.glRotatef(rot.y, 1, 0, 0);
            gl.glRotatef(rot.z, 0, 1, 0);

//            System.out.println(scale);
            float scale = this.scale.floatValue();
            gl.glScalef(scale,scale,scale);


//            initDepth(gl);
//            initBlend(gl);

//            gl.glColorMaterial(GL_FRONT_AND_BACK,
//                    GL_AMBIENT_AND_DIFFUSE
//            );
//            gl.glEnable(GL_COLOR_MATERIAL);
            //gl.glEnable(GL_NORMALIZE);

            //debug:
//            gl.glColor4f(1,1,1, 0.85f);

            //gl.glLineWidth(2);
            //gl.glBegin(GL_LINE_STRIP );

//            gl.glPointSize(4);
//            gl.glBegin(
//                GL_POINTS
////                GL2.GL_QUADS
////                GL_TRIANGLE_STRIP
//            );
//
//            int dLat = 10;
//            int dLon = 10;
//            for (int lat = -90; lat < +90; lat+= dLat) {
//                for (int lon = -180; lon < +180; lon+= dLon) {
//                    gl.glVertex3dv(project(lon, lat, 0), 0);
//
////                    gl.glVertex3fv(project(lon - dLon/2, lat - dLat/2, 0, new float[3], 0), 0);
////                    gl.glVertex3fv(project(lon + dLon/2, lat - dLat/2, 0, new float[3], 0), 0);
////                    gl.glVertex3fv(project(lon + dLon/2, lat + dLat/2, 0, new float[3], 0), 0);
////                    gl.glVertex3fv(project(lon - dLon/2, lat + dLat/2, 0, new float[3], 0), 0);
////
////                    float[] c = project(lon, lat, 0, new float[3], 0);
////                    float[] d = project(lon, lat, dLon, new float[3], 0);
////                    for (int i = 0; i < d.length; i++) {
////                        c[i] -= d[i];
////                    }
////                    gl.glNormal3fv(c, 0);
//                }
//            }
//            gl.glEnd();

//            gl.glMatrixMode(GL_PROJECTION);
//            gl.glLoadIdentity();
//            gl.glOrtho(0, w, 0, h, -1.5, 1.5);
//            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
//            gl.glLoadIdentity();
        }

        @Override
        public void untransform(GL2 gl, RectF bounds) {
            gl.glPopMatrix();
            gl.glMatrixMode(GL_PROJECTION);
            gl.glPopMatrix();
            gl.glMatrixMode(GL_MODELVIEW);
        }

        @Override
        public void pan(float tx, float ty, RectF bounds) {
            float s = scale.floatValue();
            center(lon + tx * s, lat + ty * s);
//            camPos.x += -tx * s;// * alt;
//            camPos.z +=  ty * s;// * alt;
        }

        @Override
        public void zoom(float wheel) {
            //scale.multiply(1f + (wheel) * 0.1f);
            float d = wheel * 250.0f;//scale.floatValue();
//            camPos.x += camFwd.x * d;
//            camPos.y += camFwd.y * d;
//            camPos.z += camFwd.z * d;

            alt = Util.clamp( alt + d, -10000, 10000);
            center(lon, lat);
//            System.out.println(camPos + " " + camFwd);
        }

        @Override
        public void center(float lon, float lat) {


//            System.out.println(camPos);

            this.lon = lon; this.lat = lat;
            setPos(camPos, project(lon, lat, alt));
            //setPos(camFwd, project(lon, lat, -10));
            //camFwd.subbed(camPos);
            camFwd.set(camPos);
            camFwd.scaled(-1);
            camFwd.normalized();

            //camUp.cross(camPos,camFwd);
            //camUp.normalize();
        }

        public void setPos(v3 target, double[] groundTarget) {
            double scale = -this.scale.floatValue();
            target.set((float)(groundTarget[0]*scale), (float)(groundTarget[1]*scale), (float)(groundTarget[2]*scale));
        }
    }

    public static class OsmRenderer implements GLUtessellatorCallback, Consumer<GL2> {
        public final GLUtessellator tobj = GLU.gluNewTess();
        private final LonLatProjection project;

        @Deprecated
        public transient GL2 gl;
        public List<Consumer<GL2>> draw = new Lst();

        boolean wireframe = false;

        public List<Consumer<GL2>> dbuf = new Lst();
        private final FloatArrayList vbuf = new FloatArrayList(8*1024);
        private int nextType;

        public OsmRenderer(GL2 gl, LonLatProjection project) {
            this.gl = gl;
            this.project = project;

            GLU.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_END, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, this);
            GLU.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, this);

        }

        public void clear() {
            draw.clear();
        }

        public void addNode(OsmNode node) {
            Map<String, String> tags = node.tags;

            float pointSize = 1;
            float r = 0.5f, g = 0.5f, b = 0.5f, a = 1.0f;
            if (tags!=null && !tags.isEmpty()) {
                String highway = tags.get("highway");
                String natural = tags.get("natural");

                if ("bus_stop".equals(highway)) {

                    pointSize = 3;
                    r = g = b = 1.0f;
                    a = 0.7f;
                } else if ("traffic_signals".equals(highway)) {
                    pointSize = 3;
                    r = g = 1.0f;
                    b = 0.0f;
                    a = 0.7f;
                } else if ("tree".equals(natural) || "scrub".equals(natural)) {
                    pointSize = 3;
                    g = 1.0f;
                    r = b = 0.0f;
                    a = 0.7f;
                } else {
                    pointSize = 3;
                    r = 1.0f;
                    g = b = 0.0f;
                    a = 0.7f;
                }
            }

            double[] c3 = new double[3];

            project.project(node.pos, c3);

            draw.add(new OsmDrawPoint(pointSize, r, g, b, a, c3));
        }

        public void addWay(OsmWay w) {

            Map<String, String> tags = w.tags;


            boolean isPolygon = false;
            float lw = 1.0f;
            short ls = (short) 0xffff;
            float r = 0.5f;
            float g = 0.5f;
            float b = 0.5f;
            float a = 1.0f;

            if (tags!=null && !tags.isEmpty()) {


                for (Map.Entry<String, String> entry : tags.entrySet()) {
                    String k = entry.getKey(), v = entry.getValue();
                    switch (k) {
                        case "building":
                            switch (v) {
                                case "commercial" -> {
                                    r = 0.25f;
                                    g = 0.5f;
                                    b = 0.5f;
                                    a = 1.0f;
                                    lw = 2.0f;
                                    isPolygon = true;
                                }
                                default -> {
                                    r = 0.5f;
                                    g = 0.25f;
                                    b = 0.5f;
                                    a = 1.0f;
                                    lw = 1.0f;
                                    isPolygon = true;
                                }
                            }
                            break;
                        case "barrier":
                            r = 0.5f;
                            g = 0.5f;
                            b = 0.2f;
                            a = 1.0f;
                            lw = 1.0f;
                            break;
                        case "waterway":
                            switch (v) {
                                case "stream", "river" -> {
                                    r = 0.0f;
                                    g = 0.0f;
                                    b = 1.0f;
                                    a = 1.0f;
                                    lw = 1.0f;
                                }
                            }
                            break;
                        case "natural":
                            switch (v) {
                                case "wetland", "water" -> {
                                    r = 0.0f;
                                    g = 0.0f;
                                    b = 1.0f;
                                    a = 1.0f;
                                    lw = 1.0f;
                                    isPolygon = true;
                                }
                                case "scrub" -> {
                                    r = 0.0f;
                                    g = 0.5f;
                                    b = 0.0f;
                                    a = 1.0f;
                                    lw = 1.0f;
                                    isPolygon = true;
                                }
                                case "valley" -> {
                                    r = 0.5f;
                                    g = 0.5f;
                                    b = 0.0f;
                                    a = 1.0f;
                                    lw = 1.0f;
                                    isPolygon = true;
                                }
                                case "wood" -> {
                                    r = 0.0f;
                                    g = 1.0f;
                                    b = 0.0f;
                                    a = 1.0f;
                                    lw = 1.0f;
                                    isPolygon = true;
                                }
                                default -> {
                                    //System.out.println("unstyled: " + k + " = " + v);
                                }
                            }
                            break;
                        case "leisure":
                            switch (v) {
                                case "park" -> {
                                    r = 0.1f;
                                    g = 0.9f;
                                    b = 0.0f;
                                    a = 0.25f;
                                    lw = 1.0f;
                                    isPolygon = true;
                                }
                            }
                            break;
                        case "landuse":
                            //                                case "industrial":
                            //                                    break;
                            //                                default:
                            //                                    System.out.println("unstyled: " + k + " = " + v);
                            //                                    break;
                            switch (v) {
                                case "forest", "grass", "recreation_ground" -> {
                                    r = 0.1f;
                                    g = 0.9f;
                                    b = 0.0f;
                                    a = 1.0f;
                                    lw = 1.0f;
                                    isPolygon = true;
                                }
                            }
                            break;
                        case "route":
                            //                                case "sidewalk":
                            //                                    r = 0f;
                            //                                    g = 0.5f;
                            //                                    b = 0f;
                            //                                    a = 1f;
                            //                                    lw = v.equals("both") ? 1.5f : 0.75f;
                            //                                    break;
                            switch (v) {
                                case "road" -> {
                                    r = 1.0f;
                                    g = 1.0f;
                                    b = 1.0f;
                                    a = 1.0f;
                                    lw = 1.0f;
                                }
                                case "train" -> {
                                    r = 1.0f;
                                    g = 1.0f;
                                    b = 1.0f;
                                    a = 1.0f;
                                    lw = 5.0f;
                                    ls = (short) 0xF0F0;
                                }
                                default -> {
                                    //System.out.println("unstyled: " + k + " = " + v);
                                }
                            }
                            break;

                        case "highway":
                            switch (v) {
                                case "service" -> {
                                    r = 1.0f;
                                    g = 0.0f;
                                    b = 1.0f;
                                    a = 1.0f;
                                    lw = 2.0f;
                                }
                                case "path", "pedestrian" -> {
                                    r = 0.0f;
                                    g = 0.5f;
                                    b = 0.0f;
                                    a = 1.0f;
                                    lw = 2.0f;
                                }
                                case "motorway_link", "motorway" -> {
                                    r = 1.0f;
                                    g = 0.5f;
                                    b = 0.0f;
                                    a = 1.0f;
                                    lw = 5.0f;
                                }
                                case "tertiary" -> {
                                    r = 0.7f;
                                    g = 0.5f;
                                    b = 0.0f;
                                    a = 1.0f;
                                    lw = 4.0f;
                                }
                                default -> {
                                    r = 1.0f;
                                    g = 1.0f;
                                    b = 1.0f;
                                    a = 0.5f;
                                    lw = 3.0f;
                                }
                            }
                            break;
                        default:
//                            System.out.println("unstyled: " + k + " = " + v + "(" + tags + ")");
                            break;
                    }
                }

            }

            isPolygon = isPolygon && w.isLoop();

            if (isPolygon && !wireframe) {
                List<OsmElement> nn = w.getOsmNodes();
                int s = nn.size();
                if (s > 0) {
                    double[][] coord = new double[s][7];
                    for (int i = 0; i < s; i++) {
                        double[] ci = project.project(((OsmNode)nn.get(i)).pos, coord[i]);
                        ci[3] = r;
                        ci[4] = g;
                        ci[5] = b;
                        ci[6] = a;
                    }

                    draw.add(new OsmPolygonDraw(r, g, b, a, lw, ls, this, coord));
                }


            } else {

                List ways = w.getOsmNodes();
                int ws = ways.size();
                if (ws > 0) {
                    double[] c3 = new double[3 * ws];
                    for (int i = 0, waysSize = ws; i < waysSize; i++)
                        project.project(((OsmNode)ways.get(i)).pos, c3, i * 3);

                    draw.add(new OsmLineDraw(r, g, b, a, lw, ls, c3));
                }

            }
        }
        @Override
        public void begin(int type) {
            //gl.glBegin(type);
            nextType = type;
        }

        @Override
        public void end() {
            float[] coord = vbuf.toArray();
            vbuf.clear();
            int myNextType = nextType;
            dbuf.add(gl->{
                gl.glBegin(myNextType);
                int nn = coord.length / 7;
                for (int ii = 0, i = 0; i < nn; i++) {
                    gl.glColor4fv(coord, ii); ii+=4;
                    gl.glVertex3fv(coord, ii); ii+=3;
                }
                gl.glEnd();
            });
        }



        @Override
        public void vertex(Object vertexData) {
            if (vertexData instanceof double[] pointer) {

                if (pointer.length >= 7) {
                    //gl.glColor3dv(pointer, 3);
                    vbuf.addAll((float)pointer[3], (float)pointer[4], (float)pointer[5], (float)pointer[6]);
                } else {
                    vbuf.addAll(1,1,1,1);
                }
                //gl.glVertex3dv(pointer, 0);

                vbuf.addAll((float)pointer[0], (float)pointer[1], (float)pointer[2]);



            } else if (vertexData instanceof float[] pointer) {

                if (pointer.length >= 7) {
//                    gl.glColor3fv(pointer, 3);
                    vbuf.addAll(pointer[3], pointer[4], pointer[5], pointer[6]);
                } else {
                    vbuf.addAll(1,1,1,1);
                }

                //gl.glVertex3fv(pointer, 0);
                vbuf.addAll(pointer[0], pointer[1], pointer[2]);

            } else
                throw new UnsupportedOperationException();
        }

        @Override
        public void vertexData(Object vertexData, Object polygonData) {
        }

        /*
         * combineCallback is used to create a new vertex when edges intersect.
         * coordinate location is trivial to calculate, but weight[4] may be used to
         * average color, normal, or texture coordinate data. In this program, color
         * is weighted.
         */
        @Override
        public void combine(double[] coords, Object[] data,
                            float[] weight, Object[] outData) {
            float[] vertex = new float[7];

            vertex[0] = (float) coords[0];
            vertex[1] = (float) coords[1];
            vertex[2] = (float) coords[2];
            int n = data.length;
            for (int cc = 3; cc < 7; cc++) {
                double v = 0;
                for (int dd = 0; dd < n; dd++) {
                    Object ddd = data[dd];
                    if (ddd instanceof float[] d) {
                        v += weight[dd] * d[cc];
                    } else {
                        double[] d = (double[]) ddd;
                        if (d != null)
                            v += weight[dd] * d[cc];
                    }
                }
                vertex[cc] = (float) v;
            }
            outData[0] = vertex;
        }

        @Override
        public void combineData(double[] coords, Object[] data,
                                float[] weight, Object[] outData, Object polygonData) {
        }

        @Override
        public void error(int errnum) {
            String estring = Draw.glu.gluErrorString(errnum);
            System.err.println("Tessellation Error: " + estring);
            System.exit(0);
        }

        @Override
        public void beginData(int type, Object polygonData) {
        }

        @Override
        public void endData(Object polygonData) {
        }

        @Override
        public void edgeFlag(boolean boundaryEdge) {
        }

        @Override
        public void edgeFlagData(boolean boundaryEdge, Object polygonData) {
        }

        @Override
        public void errorData(int errnum, Object polygonData) {
        }

        @Override
        public void accept(GL2 g) {
            for (Consumer<GL2> d : draw)
                d.accept(g);
        }
    }

//    public OsmSpace(IRL irl) {
//
//        this.irl = irl;



//        double minLat = osm.geoBounds.minLat;
//        double minLon = osm.geoBounds.minLon;
//        double maxLat = osm.geoBounds.maxLat;
//        double maxLon = osm.geoBounds.maxLon;
//        scaleLat = (float) ((maxLat - minLat));
//        scaleLon = (float) ((maxLon - minLon));
//        scaleLat = scaleLon = Math.max(scaleLat, scaleLon);
//        center = new GeoVec3((maxLat + minLat) / 2, (maxLon + minLon) / 2);

//    }
//
//    public OsmVolume volume() {
//        return new OsmVolume();
//    }

//    public static class OsmVolume extends AbstractSpatial<Osm> {
//
//        OsmVolume() {
//            super(null);
//        }
//
//        @Override
//        public void forEachBody(Consumer<Collidable> c) {
//
//        }
//
//        @Override
//        public float radius() {
//            return 0;
//        }
//
//        @Override
//        public void renderAbsolute(GL2 gl, int dtMS) {
////            if (mapRender == null) {
////                mapRender = compileMap(gl, osm);
////            }
////            mapRender.render(gl);
//        }
//
//    }

    private static class OsmPolygonDraw implements Consumer<GL2> {
        private final float r;
        private final float g;
        private final float b;
        private final float a;
        private final float lw;
        private final short ls;
        //private final float[] coord;
        private final Consumer<GL2> draw;

        OsmPolygonDraw(float r, float g, float b, float a, float lw, short ls, OsmRenderer s, double[][] coord) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.lw = lw;
            this.ls = ls;

            GLUtessellator tobj = s.tobj;

            GLU.gluTessBeginPolygon(tobj, null);
            GLU.gluTessBeginContour(tobj);
            for (double[] ci : coord) {
                GLU.gluTessVertex(tobj, ci, 0, ci);
            }
            GLU.gluTessEndContour(tobj);
            GLU.gluTessEndPolygon(tobj);

            Consumer<GL2>[] draws = s.dbuf.toArray(ArrayUtil.EMPTY_CONSUMER_ARRAY);
            this.draw = draws.length == 1 ? draws[0] : (G -> {
                for (Consumer<GL2> d : draws)
                    d.accept(G);
            });

            s.dbuf.clear();
        }

        @Override
        public void accept(GL2 gl) {
            gl.glColor4f(r, g , b, a);
            gl.glLineWidth(lw);
            gl.glLineStipple(1, ls);

            draw.accept(gl);
        }
    }

    private static class OsmLineDraw implements Consumer<GL2> {
        private final float r;
        private final float g;
        private final float b;
        private final float a;
        private final float lw;
        private final short ls;
        private final double[] c3;

        public OsmLineDraw(float r, float g, float b, float a, float lw, short ls, double[] c3) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.lw = lw;
            this.ls = ls;
            this.c3 = c3;
        }

        @Override
        public void accept(GL2 gl) {
            gl.glColor4f(r, g, b, a);
            gl.glLineWidth(lw);
            gl.glLineStipple(1, ls);
            gl.glBegin(GL_LINE_STRIP);
            int n = c3.length / 3;
            for (int i = 0; i < n; i++)
                gl.glVertex3dv(c3, i * 3);
            gl.glEnd();
        }
    }

    private static class OsmDrawPoint implements Consumer<GL2> {
        private final float pointSize;
        private final float r;
        private final float g;
        private final float b;
        private final float a;
        private final double[] xyz;

        public OsmDrawPoint(float pointSize, float r, float g, float b, float a, double[] xyz) {
            this.pointSize = pointSize;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.xyz = xyz;
        }

        @Override
        public void accept(GL2 gl) {
            gl.glPointSize(pointSize);
            gl.glBegin(GL_POINTS);
            gl.glColor4f(r, g, b, a);
            gl.glVertex3dv(xyz, 0);
            gl.glEnd();
        }
    }


}