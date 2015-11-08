package vectrex;

import com.gs.collections.impl.list.mutable.FastList;
import toxi.geom.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class OctBox<K> extends AABB implements Shape3D {

    /**
     * alternative tree recursion limit, number of world units when cells are
     * not subdivided any further
     */
    protected Vec3D resolution;

    public final OctBox parent;

    protected OctBox[] children;

    protected Collection<IdBB<K>> items;


    /**
     * Constructs a new AbstractOctree node within the AABB cube volume: {o.x, o.y,
     * o.z} ... {o.x+size, o.y+size, o.z+size}
     *
     * @param p
     *            parent node
     * @param o
     *            tree origin
     * @param halfSize
     *            half length of the tree volume along a single axis
     */
    private OctBox(OctBox p, Vec3D o, Vec3D halfSize) {
        super(o.plus(halfSize), new Vec3D(halfSize));
        this.parent = p;
        if (parent != null) {
            resolution = parent.resolution;
        }


    }

    /** tests if the item is in this box (NOT recursively) */
    public final boolean holds(IdBB<K> item) {
        return items.contains(item);
    }

    public final boolean holdsRecursively(IdBB<K> item) {
        if (!holds(item)) {
            OctBox[] cc = this.children;
            if (cc!=null) {
                for (OctBox<K> c : cc) {
                    if (c != null && c.holdsRecursively(item))
                        return true;
                }
            }
        }
        return false;
    }

    public final int depth() {
        final OctBox p = parent;
        if (p == null) return 0;
        return p.depth() + 1;
    }

    /**
     * Constructs a new AbstractOctree node within the AABB cube volume: {o.x, o.y,
     * o.z} ... {o.x+size, o.y+size, o.z+size}
     *
     * @param o
     *            tree origin
     *            size of the tree volume along a single axis
     */
    public OctBox(Vec3D o, Vec3D extents, Vec3D resolution) {
        this(null, o, extents);
        this.resolution = resolution;
    }


    /**
     * Adds all toAdd of the collection to the octree.
     *
     * @param toAdd
     *            point collection
     * @return how many toAdd were added
     */
    public final void putAll(final Iterable<? extends IdBB<K>> toAdd) {
        toAdd.forEach(this::put);
    }

    public final void forEach(Consumer<IdBB<K>> visitor) {
        Collection<IdBB<K>> ii = this.items;
        if (ii !=null)
            ii.forEach(visitor);
    }

    //TODO memoize this result in a special leaf subclass
    public final boolean belowResolution(BB content) {
        Vec3D e = extent;
        Vec3D r = resolution;
        XYZ ce = content.getExtents();

        return e.x <= Math.max(r.x, ce.x()) ||
                e.y <= Math.max(r.y, ce.y()) ||
                e.z <= Math.max(r.z, ce.z());
    }

    /**
     * Adds a new point/particle to the tree structure. All points are stored
     * within leaf nodes only. The tree implementation is using lazy
     * instantiation for all intermediate tree levels.
     *
     * @param p
     * @return the box it was inserted to, or null if wasn't
     */
    public OctBox<K> put(final IdBB<K> x) {

        BB p = x.getBB();

        // check if point is inside cube
        if (containsPoint(p)) {
            //find the largest leaf that can contain x
            if (belowResolution(x.getBB())) {
                if (items == null) {
                    items = newItemCollection();
                }
                items.add(x);
                return this;
            } else {
                if (children == null) {
                    children = new OctBox[8];
                }
                int octant = getOctantID(p);

                final Vec3D extent = this.extent;
                if (children[octant] == null) {
                    Vec3D off = new Vec3D(
                            minX() + ((octant & 1) != 0 ? extent.x() : 0),
                            minY() + ((octant & 2) != 0 ? extent.y() : 0),
                            minZ() + ((octant & 4) != 0 ? extent.z() : 0));
                    children[octant] = new OctBox(this, off,
                            extent.scale(0.5f));
                }
                return children[octant].put(x);
            }
        }
        return null;
    }

    protected Collection<IdBB<K>> newItemCollection() {
        return new FastList();
    }


    public void forEach(BiConsumer<OctBox<K>, IdBB<K>> visitor) {
        forEach(i -> {
           visitor.accept(OctBox.this, i);
        });

        OctBox[] cc = this.children;
        if (cc !=null) {
            for (OctBox<K> c : cc) {
                if (c!=null)
                    c.forEach(visitor);
            }
        }

    }

    public void forEachBox(Consumer<OctBox<K>> visitor) {
        visitor.accept(this);
        OctBox[] cc = this.children;
        if (cc !=null) {
            for (OctBox c : cc) {
                if (c != null) {
                    c.forEachBox(visitor);
                }
            }
        }
    }



    public final boolean containsPoint(XYZ p) {
        return p.isInAABB(this);
    }

    public void clear() {
        zero();
        children = null;
        items = null;
    }

    /**
     * @return a copy of the child nodes array
     */
    public OctBox[] getChildrenCopy() {
        if (children != null) {
            OctBox[] clones = new OctBox[8];
            System.arraycopy(children, 0, clones, 0, 8);
            return clones;
        }
        return null;
    }



    /**
     * Finds the leaf node which spatially relates to the given point
     *
     * @return leaf node or null if point is outside the tree dimensions
     */
    public OctBox getLeafForPoint(final XYZ p) {
        // if not a leaf node...
        if (p.isInAABB(this)) {
            final OctBox[] children = this.children;
            if (children!=null) {
                int octant = getOctantID(p);
                if (children[octant] != null) {
                    return children[octant].getLeafForPoint(p);
                }
            } else if (items != null) {
                return this;
            }
        }
        return null;
    }



    /**
     * Returns the minimum size of nodes (in world units). This value acts as
     * tree recursion limit since nodes smaller than this size are not
     * subdivided further. Leaf node are always smaller or equal to this size.
     *
     * @return the minimum size of tree nodes
     */
    public final Vec3D getResolution() {
        return resolution;
    }


//    /**
//     * Computes the local child octant/cube index for the given point
//     *
//     * @param plocal
//     *            point in the node-local coordinate system
//     * @return octant index
//     */
//    protected final int getOctantID(final Vec3D plocal) {
//        final XYZ h = this.extent;
//
//        return (plocal.x >= h.x() ? 1 : 0) + (plocal.y >= h.y() ? 2 : 0)
//                + (plocal.z >= h.z() ? 4 : 0);
//    }

    /** computes getOctantID for the point subtracted by another point,
     *  without needing to allocate a temporary object
     */
    private int getOctantID(final XYZ p) {
        //final XYZ h = this.extent;
        return ((p.x() - x) >= 0 ? 1 : 0) + ((p.y() - y) >= 0 ? 2 : 0)
                + ((p.z() - z) >= 0 ? 4 : 0);
    }



    /**
     * @return the parent
     */
    public final OctBox getParent() {
        return parent;
    }

    public final Collection<IdBB<K>> getItems() {
        final Collection<IdBB<K>> i = this.items;
        if (i == null) return Collections.EMPTY_LIST;
        return i;
    }

    public final int itemCountRecursively() {
        final int[] x = {0};
        forEachBox(n -> x[0] += n.itemCount());
        return x[0];
    }

    public final int itemCount() {
        final Collection<IdBB<K>> i = this.items;
        if (i == null) return 0;
        return i.size();
    }

    public List<IdBB<K>> getItemsRecursively() {
        return getItemsRecursively(new FastList());
    }

    /**
     * @return the points
     */
    public List<IdBB<K>> getItemsRecursively(List<IdBB<K>> results) {
        final OctBox[] children = this.children;
        if (items != null) {
            results.addAll(items);
        } else if (children!=null) {
            for (int i = 0; i < 8; i++) {
                if (children[i] != null) {
                    children[i].getItemsRecursively(results);
                }
            }
        }
        return results;
    }

    /**
     * Selects all stored points within the given axis-aligned bounding box.
     *
     * @param b
     *            AABB
     * @return all points with the box volume
     */
    @Deprecated public List<IdBB<K>> getItemsWithin(BB b) {
        List<IdBB<K>> results = null;
        if (this.intersectsBox(b)) {
            if (items != null) {
                for (IdBB<K> q : items) {
                    if (q.getBB().isInAABB(b)) {
                        if (results == null) {
                            results = new FastList();
                        }
                        results.add(q);
                    }
                }
            } else if (children!=null) {
                for (int i = 0; i < 8; i++) {
                    if (children[i] != null) {
                        List<IdBB<K>> points = children[i].getItemsWithin(b);
                        if (points != null) {
                            if (results == null) {
                                results = new FastList();
                            }
                            results.addAll(points);
                        }
                    }
                }
            }
        }
        return results;
    }

    public void forEachBox(BB b, Consumer<IdBB<K>> c) {
        if (this.intersectsBox(b)) {
            final OctBox[] childs = this.children;
            if (items != null) {
                for (IdBB<K> q : items) {
                    if (q.getBB().isInAABB(b)) {
                        c.accept(q);
                    }
                }
            } else if (childs!=null) {
                for (int i = 0; i < 8; i++) {

                    OctBox ci = childs[i];
                    if (ci != null) {
                        ci.forEachBox(b, c);
                    }
                }
            }
        }
    }

    public void forEachNeighbor(IdBB<K> item, XYZ boxRadius, Consumer<OctBox> visitor) {
        //SOON
        throw new UnsupportedOperationException();
    }

    public void forEachInSphere(Sphere s, Consumer<IdBB<K>> c) {

        if (this.intersectsSphere(s)) {
            if (items != null) {
                for (IdBB<K> q : items) {
                    if (s.containsPoint(q.getBB())) {
                        c.accept(q);
                    }
                }
            } else if (children!=null) {
                for (int i = 0; i < 8; i++) {
                    OctBox cc = children[i];
                    if (cc != null) {
                        cc.forEachInSphere(s, c);
                    }
                }
            }
        }
    }


    /**
     * Selects all stored points within the given sphere volume
     *
     * @param s
     *            sphere
     * @return selected points
     */
    @Deprecated public List<IdBB<K>> getItemsWithin(Sphere s) {
        List<IdBB<K>> results = null;
        if (this.intersectsSphere(s)) {
            if (items != null) {
                for (IdBB<K> q : items) {
                    if (s.containsPoint(q.getBB())) {
                        if (results == null) {
                            results = new FastList();
                        }
                        results.add(q);
                    }
                }
            } else if (children!=null) {
                for (int i = 0; i < 8; i++) {
                    if (children[i] != null) {
                        List<IdBB<K>> points = children[i].getItemsWithin(s);
                        if (points != null) {
                            if (results == null) {
                                results = new FastList();
                            }
                            results.addAll(points);
                        }
                    }
                }
            }
        }
        return results;
    }



    /**
     * Selects all stored points within the given sphere volume
     *
     * @param sphereOrigin
     * @param clipRadius
     * @return selected points
     */
    public void forEachInSphere(Vec3D sphereOrigin, float clipRadius, Consumer<IdBB<K>> c) {
        forEachInSphere(new Sphere(sphereOrigin, clipRadius), c);
    }



    private void reduceBranch() {
        if (items != null && items.size() == 0) {
            items = null;
        }
        if (children!=null) {
            for (int i = 0; i < 8; i++) {
                if (children[i] != null && children[i].items == null) {
                    children[i] = null;
                }
            }
        }
        if (parent != null) {
            parent.reduceBranch();
        }
    }

    /**
     * Removes a point from the tree and (optionally) tries to release memory by
     * reducing now empty sub-branches.
     *
     * @return true, if the point was found & removed
     */
    public boolean remove(Object _p) {
        boolean found = false;
        IdBB<K> p = (IdBB<K>)_p;
        OctBox leaf = getLeafForPoint(p.getBB());
        if (leaf != null) {
            if (leaf.items.remove(p)) {
                found = true;
                if (leaf.items.size() == 0) {
                    leaf.reduceBranch();
                }
            }
        }
        return found;
    }

    public boolean removeAll(Collection<?> points) {
        boolean allRemoved = true;
        for (Object p : points) {
            allRemoved &= remove(p);
        }
        return allRemoved;
    }

    /*
     * (non-Javadoc)
     *
     * @see toxi.geom.AABB#toString()
     */
    public String toString() {
        Collection<IdBB<K>> ii = this.items;
        String x = "<OctBox:" + super.toString() + ":" +
                ((ii !=null) ? ii.size() : 0);
        return x;
    }
}
