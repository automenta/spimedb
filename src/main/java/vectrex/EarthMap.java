package vectrex;

import toxi.geom.Vec3D;

/**
 * PlanetMap preconfigured for Earth's known dimensions and irregular sphericity */
public class EarthMap<K, V extends IdBB<K>> extends PlanetMap<K,V> {
    public EarthMap(Vec3D center, Vec3D radius, Vec3D resolution) {
        super(center, radius, resolution);
    }
}
