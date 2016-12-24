package spimedb;

import spimedb.util.geom.BB;



/** identified bounding box */
public interface IdBB {
    String id();
    BB getBB();
}
