package spimedb.index;

import spimedb.IdBB;
import spimedb.index.oct.OctMap;
import spimedb.util.geom.Vec3D;

/**
 * OctMap with special handling for planetary coordinates and spheroid geometry.
 * 1st dimensions is Latitude, 2nd dimension is Longitude, 3rd dimension is time */
public class PlanetMap<K, V extends IdBB> extends OctMap<K,V> {

    public PlanetMap(Vec3D center, Vec3D radius, Vec3D resolution) {
        super(center, radius, resolution);
    }
}
