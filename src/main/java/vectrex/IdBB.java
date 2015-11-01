package vectrex;

import toxi.geom.BB;



/** identified bounding box */
public interface IdBB<K> {
    public K id();
    public BB getBB();
}
