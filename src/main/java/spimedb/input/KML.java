/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.input;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.giscore.events.*;
import org.opensextant.giscore.events.SimpleField.Type;
import org.opensextant.giscore.geometry.Geometry;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.utils.Color;
import spimedb.MutableNObject;
import spimedb.NObject;
import spimedb.SpimeDB;
import spimedb.input.kml.KmlReader;
import spimedb.input.kml.UrlRef;
import spimedb.util.HTMLFilter;
import spimedb.util.JSON;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static spimedb.input.kml.KmlReader.logger;

/**
 * TODO - remove null descriptions - store HTML content separately so it does
 * not get indexed - max token size
 * <p>
 * see https://github.com/OpenSextant/giscore/wiki
 *
 * @author me
 */
public class KML {

    final int maxPathDepth = 3;
    private final Proxy proxy;
    private final SpimeDB db;
    final AtomicLong serial = new AtomicLong();

    private String layer;
    final Deque<String> path = new ArrayDeque();

    boolean enableDescriptions = true;
    private String pathString;
    private String parentPathString;


    public static String getSerial(long serial) {

        return Long.toString(serial, Character.MAX_RADIX);
        //return Base64.getEncoder().withoutPadding().encodeToString(Longs.toByteArray(serial));
        //return Base64.getEncoder().withoutPadding().encodeToString(Longs.toByteArray(serial));
        //return Base64.getEncoder().withoutPadding().encodeToString(BigInteger.valueOf(serial).toByteArray());
        //.add(BigInteger.valueOf(layer.hashCode()).shiftRight(32)
        //.toByteArray();
    }

    public static String[] pathArray(Deque<String> p) {
        return p.toArray(new String[p.size()]);
    }

    public interface GISVisitor {

        boolean on(IGISObject go, String[] path) throws IOException;

        void start(String layer);

        void end();
    }


    public void transformKML(Supplier<KmlReader> source, String layer, SpimeDB st, final GISVisitor visitor) {

        this.layer = layer;

        KmlReader reader = source.get();
        reader.setRewriteStyleUrls(true);

        SimpleField layerfield = new SimpleField("layer", Type.STRING);
        layerfield.setLength(32);

        final AtomicInteger exceptions = new AtomicInteger();
        final Set<Class> exceptionClass = new HashSet();

        serial.set(1);
        path.clear();

        visitor.start(layer);

        do {

            //for (IGISObject go; (go = reader.read()) != null;) {
            // do something with the gis object; e.g. check for placemark, NetworkLink, etc.
            try {

                IGISObject go = reader.read();
                if (go == null) {
                    break;
                }

                if (go instanceof DocumentStart) {
                    //DocumentStart ds = (DocumentStart) go;

                    continue;

                }

                if (go instanceof ContainerEnd) {
                    path.removeLast();
                    updatePath();
                    continue;
                }

                //if ((go instanceof ContainerStart) || (go instanceof Feature)) {
                //if (go instanceof ContainerStart) {
                //}

                //add to the path after container start is processed
                if (go instanceof ContainerStart) {
                    //ContainerStart cs = (ContainerStart) go;
                    //TODO startTime?

                    String i;
                    if (path.isEmpty())
                        i = layer;
                    else {
                        i = getSerial(serial.getAndIncrement());
                    }

                    //System.out.println(cs + " " + cs.getId());
                    updatePath(i);

                }

                if (!visitor.on(go, pathArray(path))) {
                    break;
                }


            } catch (Throwable t) {
                //System.err.println(t);
                exceptions.incrementAndGet();
                exceptionClass.add(t.getClass());
                logger.error("error: {}", t);
                break;
            }
        } while (true);

// get list of network links that were retrieved from step above
        List<URI> _links = reader.getNetworkLinks();

        Set<URI> networkLinks = !_links.isEmpty() ? new LinkedHashSet(_links) : Collections.emptySet();

        reader.close();

        if (!networkLinks.isEmpty()) {

            // Now import features from all referenced network links.
            // if Networklinks have nested network links then they will be added to end
            // of the list and processed one after another. The handleEvent() callback method
            // below will be called with each feature (i.e. Placemark, GroundOverlay, etc.)
            // as it is processed in the target KML resources.
            reader.importFromNetworkLinks(
                    new KmlReader.ImportEventHandler() {
                        public boolean kmlEvent(UrlRef ref, IGISObject gisObj) {

                            // if gisObj instanceOf Feature, GroundOverlay, etc.
                            // do something with the gisObj
                            // return false to abort the recursive network link parsing
                            /*if (visited.contains(ref))
                             return false;*/
                            //System.out.println("Loading NetworkLink: " + ref + " " + gisObj);
                            String r = ref.toString();
                            boolean pathChanged = false;
                            if (!((path.isEmpty()) && (path.getLast().equals(r)))) {
                                path.add(r);
                                pathChanged = true;
                            }

                            serial.getAndIncrement();

                            try {
                                visitor.on(gisObj, pathArray(path));
                            } catch (Throwable t) {
                                logger.error("visit {}: {}", gisObj, t);
                            }

                            if (pathChanged) {
                                path.removeLast();
                            }

                            return true;
                        }

                        @Override
                        public void kmlError(URI uri, Exception excptn) {
                            exceptions.incrementAndGet();
                            exceptionClass.add(excptn.getClass());
                        }

                    });

        }

        if (exceptions.get() > 0) {
            System.err.println("  Exceptions: " + exceptions + " of " + exceptionClass);
        }

        visitor.end();

    }

    void updatePath(String next) {
        path.add(next);
        if (path.size() < maxPathDepth) {
            updatePath();
        }
    }

    void updatePath() {
        int ps = path.size();
        if (ps > 0) {
            parentPathString = String.join("/", new ArrayList(path).subList(0, path.size() - 1));
        } else {
            parentPathString = null;
        }

        pathString = String.join("/", path);
    }

//    public static void exec(String cmd) {
//        try {
//            String[] cmdParm = {"/bin/sh", "-c", cmd};
//
//            Process proc = Runtime.getRuntime().exec(cmdParm);
//            IOUtils.copy(proc.getInputStream(), System.out);
//            IOUtils.copy(proc.getErrorStream(), System.err);
//            proc.waitFor();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    public static void toPostgres(String layerName, String inputFile, String host, String user, String db) {
//        exec("ogr2ogr -update -append -skipFailures -f \"PostgreSQL\" PG:\"host=" + host + " user=" + user + " dbname=" + db + "\" " + inputFile + " -nln " + layerName);
//    }
//
//    public static void toGeoJSON(String inputFile, String outputFile) {
////        ogr2ogr -f "GeoJSON" output.json input.kml
//        exec("rm -f " + outputFile);
//        exec("ogr2ogr -f GeoJSON -skipFailures " + outputFile + " " + inputFile);
//    }

    final public static ObjectMapper json = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true)
            .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public static ObjectNode fromJSON(String x) {
        try {
            return json.readValue(x, ObjectNode.class);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public KML(SpimeDB db) {
        this(db, null);
    }

    public KML(SpimeDB db, Proxy proxy) {
        this.db = db;
        this.proxy = proxy;
    }


    public Runnable url(String url) {
        try {
            return url(new URL(url).getFile(), url);
        } catch (IOException e) {
            return () -> {
                logger.error("invalid url \"{}\": {}", e);
            };
        }
    }

    public Runnable url(String id, String url) throws IOException {
        return url(id, url, null);
    }

    public Runnable file(String id, String path) throws IOException {
        return url(id, "file:///" + path, null);
    }

    public Runnable url(String id, String url, Proxy proxy) {
        return task(id, () -> {
            try {
                return new KmlReader(new URL(url), proxy);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public Runnable file(String id, File f) {
        return task(id, () -> {
            try {
                return new KmlReader(f);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public Runnable task(String id, Supplier<KmlReader> reader) {
        return () -> {

            try {

                long start = System.currentTimeMillis();

                final Map<String, Style> styles = new LinkedHashMap();


                //1. pre-process: collect style information
                transformKML(reader, id, db, new GISVisitor() {

                    final Map<String, String> styleMap = new LinkedHashMap();

                    @Override
                    public void start(String layer) {
                    }

                    protected void onStyle(Style s) {
                        String id = s.getId();
                        styles.put(id, s);
                    }

                    protected void onStyleMap(StyleMap ss) {

                        String ssid = ss.getId();
                        if (ssid == null) {
                            System.err.println("null id: " + ss);
                            return;
                        }
                        Pair p = ss.getPair(StyleMap.NORMAL);
                        StyleSelector ps = p.getStyleSelector();
                        if (ps instanceof Style) {
                            styles.put(ssid, ((Style) ps));
                        } else if (ps instanceof StyleMap) {
                            //System.out.println("Unmanaged StyleMap: " + p);
                            styleMap.put(ssid, p.getStyleUrl());
                        }

                        //TODO highlight?
                    }

                    @Override
                    public boolean on(IGISObject go, String[] path) {

                        if (go instanceof Style) {
                            onStyle((Style) go);
                        } else if (go instanceof StyleMap) {
                            onStyleMap((StyleMap) go);
                        }
                        if (go instanceof ContainerStart) {
                            ContainerStart cs = (ContainerStart) go;

                            for (StyleSelector ss : cs.getStyles()) {
                                if (ss instanceof Style) {
                                    onStyle((Style) ss);
                                } else if (ss instanceof StyleMap) {
                                    onStyleMap((StyleMap) ss);
                                }
                            }
                        }

                        return true;
                    }

                    @Override
                    public void end() {
                        for (Map.Entry<String, String> e : styleMap.entrySet()) {
                            String from = e.getKey();
                            String to = e.getValue();
                            Style toStyle = styles.get(to);
                            if (toStyle == null) {
                                System.err.println("Missing style: " + to);
                                continue;
                            }
                            styles.put(from, toStyle);
                        }
                    }

                });

                //System.out.println(layer + " STYLES:  \n" + styles.keySet());


                //2. process features
                transformKML(reader, id, db, new MyGISVisitor(id, styles));

                long end = System.currentTimeMillis();
                logger.warn("{} loaded: {}(ms)", id, end - start);

            } catch (Throwable e) {
                logger.error("error {}", e);
            }

        };
    }

    public static boolean anchorHash(String su) {
        return su.charAt(0) == '#';
    }

    //    NObject stylemapJson(NObject fb, StyleMap s) throws IOException {
//        Pair normal = s.getPair(StyleMap.NORMAL);
//        //System.out.println(normal.getStyleUrl() + " " + normal.getStyleSelector());
//        return fb;
//    }


    static NObject styleJson(MutableNObject fb, Style s) {

        //System.out.println("Applying style: " + s);
        String iconUrl = s.getIconUrl();
        if (iconUrl != null) {
            fb.put("iconUrl", iconUrl);
        }

        String baloonText = s.getBalloonText();
        if (baloonText != null) {
            fb.put("baloonText", baloonText);
        }

        /*Color iconColor = s.getIconColor();
         if (iconColor!=null)
         fb.put("iconColor", iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), iconColor.getAlpha() );
        
        
         Color lineColor = s.getLineColor();
         if (lineColor!=null)
         fb.put("lineColor", new Integer[] { lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha() } );
        
         Color polyColor = s.getPolyColor();
         if (polyColor!=null)
         fb.put("polyColor", "rgba(" + polyColor.getRed() + "," + polyColor.getGreen()+ "," + polyColor.getBlue()+ "," + polyColor.getAlpha() + ")"  );
         */
        Color polyColor = s.getPolyColor();
        if (polyColor != null) {
            fb.put("polyColor", polyColor.toString().substring("org.opensextant.giscore.utils.".length()));
        }

        Double lineWidth = s.getLineWidth();
        if (lineWidth != null) {
            fb.put("lineWidth", lineWidth);
        }

        return fb;

    }

    //    /*
//    private synchronized boolean processStyle(Style s) throws IOException {
//        String id = s.getId();
//
//        String fullStyleId = layer + "_" + id;
//
//        System.err.println("processStyle: " + s.getId() + " -> " + fullStyleId);
//
//        NObject fb = jsonBuilder();
//        styleJson(fb, s);
//        fb;
//
//        //use '_' instead of '#' which is reserved for URL encoding
//        bulk = st.add(bulk, "style", fullStyleId, fb);
//        updateBulk();
//
//        /*else {
//         System.out.println(s);
//         }*/
//        return true;
//
//    }
    /*
     private String processStyleMap(StyleMap styleMap) throws IOException {
     //System.out.println(styleMap.getId() + " " + styleMap.getPair(StyleMap.NORMAL));
     if (styleMap.getPair(StyleMap.NORMAL).getStyleSelector() != null) {
     processStyle((Style) styleMap.getPair(StyleMap.NORMAL).getStyleSelector());
     }
     String normalURL = styleMap.getPair(StyleMap.NORMAL).getStyleUrl();
     if (normalURL.startsWith("#")) {
     normalURL = normalURL.substring(1);
     }
     normalURL = layer + "_" + normalURL;
     return normalURL;
     }
     */
    public static double[][] toArray(List<Point> lp) {
        double[][] points = new double[lp.size()][2];

        for (int i = 0; i < points.length; i++) {
            Geodetic2DPoint c = lp.get(i).getCenter();
            double[] pi = points[i];
            pi[0] = c.getLatitudeAsDegrees();
            pi[1] = c.getLongitudeAsDegrees();
        }
        return points;
    }

    private class MyGISVisitor implements GISVisitor {

        private final String id;
        private final Map<String, Style> styles;
        public boolean rootFound;

        public MyGISVisitor(String id, Map<String, Style> styles) {
            this.id = id;
            this.styles = styles;
        }

        @Override
        public void start(String layer) {

        }

        @Override
        public boolean on(IGISObject go, String[] path) throws IOException {
            if (go == null) {
                throw new RuntimeException("null GISObject: " + Arrays.toString(path));
            }

            MutableNObject d;

            if (go instanceof ContainerStart) {
                ContainerStart cs = (ContainerStart) go;
                //TODO startTime?
                //System.out.println(cs + " " + cs.getId());

                if (path.length == 1) {
                    if (rootFound) {
                        throw new RuntimeException("Multiple roots");
                    }
                    rootFound = true;
                    //name the top level folder
                    d = new MutableNObject(id);
                } else {
                    d = new MutableNObject(pathString);
                    d.setTag(parentPathString);
                }

                d.name(cs.getName());

                /*String styleUrl = cs.getStyleUrl();
                 if (styleUrl != null) {
                 if (styleUrl.startsWith("#")) {
                 styleUrl = styleUrl.substring(1);
                 }
                 styleUrl = layer + "_" + styleUrl;
                 System.err.println("Container styleUrl: " + styleUrl);
                 d.put("styleUrl", styleUrl);
                 }
                 */
                if (enableDescriptions) {
                    String desc = cs.getDescription();
                    if ((desc != null) && (desc.length() > 0)) {
                        //filter
                        desc = HTMLFilter.filterHTML(desc);
                        if (desc.length() > 0) {
                            d.description(desc);
                        }
                    }
                }
            } else {
                d = new MutableNObject(JSON.uuid64());
                d.setTag(pathString);
            }

            if (go instanceof Common) {
                Common cm = (Common) go;
                if (cm.getStartTime() != null) {
                    if (cm.getEndTime() != null) {
                        d.when(cm.getStartTime().getTime(), cm.getEndTime().getTime());
                    } else {
                        d.when(cm.getStartTime().getTime());
                    }
                }

            }

            if (go instanceof Feature) {
                Feature f = (Feature) go;

                d.name(f.getName());

                if (enableDescriptions) {
                    String desc = f.getDescription();
                    if ((desc != null) && (desc.length() > 0)) {
                        //filter
                        desc = HTMLFilter.filterHTML(desc);
                        if (desc.length() > 0) {
                            d.description(desc);
                        }
                    }
                }

                if (f.getSnippet() != null) {
                    if (!f.getSnippet().isEmpty()) {
                        d.put("snippet", f.getSnippet());
                    }
                }


                Geometry g = f.getGeometry();


                if (g != null) {
                    /*if (g instanceof Circle) {

                    }
                    else */
                    if (g instanceof Point) {
                        Point pp = (Point) g;
                        d.where(pp.getCenter());

                    } else if (g instanceof org.opensextant.giscore.geometry.LinearRing) {
                        logger.warn("unhandled geometry type: {}: {}", g.getClass(), g);

                    } else if (g instanceof org.opensextant.giscore.geometry.Line) {
                        org.opensextant.giscore.geometry.Line l = (org.opensextant.giscore.geometry.Line) g;
                        d.where(l);
                    } else if (g instanceof org.opensextant.giscore.geometry.MultiLinearRings) {
                        logger.warn("unhandled geometry type: {}: {}", g.getClass(), g);
                    } else if (g instanceof org.opensextant.giscore.geometry.Polygon) {
                        org.opensextant.giscore.geometry.Polygon p = (org.opensextant.giscore.geometry.Polygon) g;
                        d.where(p);
                    } else {
                        logger.warn("unhandled geometry type: {}: {}", g.getClass(), g);
                    }

                    //TODO other types
                }

                Style styleInline = null;
                StyleSelector fstyle = f.getStyle();
                if (fstyle != null) {

                    if (fstyle instanceof Style) {

                        Style ss = (Style) fstyle;
                        styleInline = ss;

                    } else if (fstyle instanceof StyleMap) {
                        StyleMap ss = (StyleMap) fstyle;
                        styleInline = styles.get(ss.getId());
                        if (styleInline == null) {
                            logger.warn("Missing style: {}", ss.getId());
                        }
                    }

                }

                if ((f.getStyleUrl() != null) || (styleInline != null)) {

                    //fb = jsonBuilder("style");

                    if (f.getStyleUrl() != null) {
                        String su = f.getStyleUrl();
                        if (anchorHash(su)) {
                            su = su.substring(1);
                        }

                        Style s = styles.get(su);
                        if (s == null) {
                            //System.err.println("Missing: " + f.getStyleUrl());
                        } else {
                            styleJson(d, s);
                        }
                    }

                    if (styleInline != null) {
                        styleJson(d, styleInline);
                    }


                }

            }

            if (go instanceof Schema) {
                //..
            }

            /*if (d!=null)*/

                /*if (d.getName() == null)  {
                    System.err.println("Un-NObjectized: " + go);
                    return false;
                }*/
            db.put(d);


            return true;
        }

        @Override
        public void end() {
        }


    }
}
