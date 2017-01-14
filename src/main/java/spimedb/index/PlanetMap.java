package spimedb.index;

import spimedb.index.oct.OctBox;
import spimedb.index.oct.OctMap;
import spimedb.util.geom.Vec3D;

import java.util.Map;

/**
 * OctMap with special handling for planetary coordinates and spheroid geometry.
 * 1st dimensions is Latitude, 2nd dimension is Longitude, 3rd dimension is time */
public class PlanetMap<K, V extends OctBox.IdBB> extends OctMap<K,V> {


    public PlanetMap(Map<K, V> items, Map<Long, OctBox<K>> boxes, Vec3D center, Vec3D radius, Vec3D resolution) {
        super(items, boxes, center, radius, resolution);
    }

    public static float metersToDegrees(float radMeters) {
        return radMeters / 110648f;
    }
}
