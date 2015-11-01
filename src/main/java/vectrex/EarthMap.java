package vectrex;

import spangraph.InfiniPeer;
import toxi.geom.Vec3D;

/**
 * PlanetMap preconfigured for Earth's known dimensions and irregular sphericity */
public class EarthMap<K, V extends IdBB<K>> extends PlanetMap<K,V> {
    public EarthMap(InfiniPeer p, String id, Vec3D center, Vec3D radius, Vec3D resolution) {
        super(p, id, center, radius, resolution);
    }
}
