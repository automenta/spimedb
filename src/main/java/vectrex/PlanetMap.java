package vectrex;

import spangraph.InfiniPeer;
import toxi.geom.Vec3D;

/**
 * OctMap with special handling for planetary coordinates and spheroid geometry.
 * 1st dimensions is Latitude, 2nd dimension is Longitude, 3rd dimension is time */
public class PlanetMap<K, V extends IdBB<K>> extends OctMap<K,V> {

    public PlanetMap(InfiniPeer p, String id, Vec3D center, Vec3D radius, Vec3D resolution) {
        super(p, id, center, radius, resolution);
    }
}
