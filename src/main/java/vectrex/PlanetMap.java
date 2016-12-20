package vectrex;

import toxi.geom.Vec3D;

/**
 * OctMap with special handling for planetary coordinates and spheroid geometry.
 * 1st dimensions is Latitude, 2nd dimension is Longitude, 3rd dimension is time */
public class PlanetMap<K, V extends IdBB<K>> extends OctMap<K,V> {

    public PlanetMap(Vec3D center, Vec3D radius, Vec3D resolution) {
        super(center, radius, resolution);
    }
}
