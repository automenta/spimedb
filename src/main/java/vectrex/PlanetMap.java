package vectrex;

import spangraph.InfiniPeer;
import toxi.geom.Vec3D;
import toxi.geom.XYZ;

/**
 * OctMap with special handling for planetary coordinates and spheroid geometry.
 * 1st dimensions is Latitude, 2nd dimension is Longitude, 3rd dimension is time */
public class PlanetMap<K extends XYZ, V> extends OctMap<K,V> {

    public PlanetMap(InfiniPeer p, String id, Vec3D center, Vec3D radius, Vec3D resolution) {
        super(p, id, center, radius, resolution);
    }
}
