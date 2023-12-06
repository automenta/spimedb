package spimedb.ui;

import jcog.User;
import jcog.memoize.CaffeineMemoize;
import jcog.memoize.Memoize;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.rect.RectF;
import jcog.tree.rtree.split.QuadraticSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.util.geo.osm.Osm;
import spacegraph.util.geo.osm.OsmElement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;

/**
 * "in real life" geospatial data cache
 */
public class IRL {

    private final User user;
    private static final Logger logger = LoggerFactory.getLogger(IRL.class);


    public final RTree<OsmElement> index =
        new RTree<>(Function.identity(), 4,
                //LinearSplit.the
                QuadraticSplit.the
        );

    public IRL(User u) {
        this.user = u;
    }

    final Memoize<RectF, Osm> reqCache = CaffeineMemoize.build(
            bounds -> _load(bounds.left(), bounds.bottom(), bounds.right(), bounds.top()), 1024, false);


    /**
     * gets the Osm grid cell containing the specified coordinate, of our conventional size
     */
    public Osm load(float lon0, float lat0, float lon1, float lat1) {
//        float x0 = Util.round(lon - lonRange / (2 - Float.MIN_NORMAL), LON_RESOLUTION);
//        float y0 = Util.round(lat - latRange / (2 - Float.MIN_NORMAL), LAT_RESOLUTION);
//        return reqCache.apply(RectFloat.XYXY(
//                x0, y0,
//                x0 + LON_RESOLUTION, y0 + LAT_RESOLUTION
//        ));
        assert(lon1 > lon0 && lat1 > lat0);
        return reqCache.apply(
            RectF.XYXY(
            lon0, lat0, lon1, lat1
            )
        );
    }

    private Osm _load(double lonMin, double latMin, double lonMax, double latMax) {
        Osm osm = new Osm();

        URL u = Osm.url("https://api.openstreetmap.org", lonMin, latMin, lonMax, latMax);
        osm.id = u.toExternalForm();

        //Exe.runLater(() ->
        user.get(u.toString(), () -> {
            try {
                logger.info("Download {}", u);
                try (InputStream uu = u.openStream()) {
                    return uu.readAllBytes();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }, data -> {
            try {
                logger.info("Loading {} ({} bytes)", u, data.length);


                osm.load(new ByteArrayInputStream(data));
//                synchronized(index) {
//                    osm.addAll(o);
////                    osm.ways.forEachValue(index::add);
////                    //osm.nodes.forEachValue(index::add);
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return osm;
    }



}
