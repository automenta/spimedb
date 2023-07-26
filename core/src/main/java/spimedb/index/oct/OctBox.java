package spimedb.index.oct;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.NotNull;
import spimedb.util.geom.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class OctBox<K> extends AABB implements Shape3D {

    /**
     * alternative tree recursion limit, number of world units when cells are
     * not subdivided any further
     * TODO resolution as array
     */
    protected Vec3D resolution;

    public final OctBox parent;

    protected OctBox[] children;

    protected Collection<IdBB> items;



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
        if (resolution.volume() <= 0)
            throw new RuntimeException("resolution must have non-zero volume"); //otherwise the root will hold everything due to root being always below threshold
    }

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
    public final boolean holds(IdBB item) {
        return items.contains(item);
    }

    public final boolean holdsRecursively(IdBB item) {
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
     * Adds all toAdd of the collection to the octree.
     *
     * @param toAdd
     *            point collection
     * @return how many toAdd were added
     */
    public final void putAll(final Iterable<? extends IdBB> toAdd) {
        toAdd.forEach(this::put);
    }

    public final void forEachLocal(Consumer<IdBB> visitor) {
        Collection<IdBB> ii = this.items;
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
    public OctBox<K> put(final IdBB x) {

        BB p = x.getBB();

        // check if point is inside cube
        if (containsPoint(p)) {
            //find the largest leaf that can contain x
            if (belowResolution(x.getBB())) {
                if (items == null) {
                    items = newItemCollection();
                    items.add(x);
                    onModified();
                } else if (items.add(x)) {
                    onModified();
                }
                return this;
            } else {
                int octant = getOctantID(p);

                boolean modified = false;

                if (children == null) {
                    children = new OctBox[8];
                    modified = true;
                }

                final Vec3D extent = this.extent;
                OctBox target = children[octant];
                if (target == null) {
                    Vec3D off = new Vec3D(
                            minX() + ((octant & 1) != 0 ? extent.x() : 0),
                            minY() + ((octant & 2) != 0 ? extent.y() : 0),
                            minZ() + ((octant & 4) != 0 ? extent.z() : 0));
                    target = children[octant] = newBox(this, off, extent.scale(0.5f));
                    modified = true;
                }

                OctBox result = target.put(x);
                if (result == null)
                    System.err.println(x + " was not inserted in a child of " + this);
                //throw new NullPointerException

                if (modified)
                    onModified();

                return result;
            }
        }
        return null;
    }

    @NotNull
    protected OctBox newBox(OctBox parent, Vec3D off, Vec3D extent) {
        return new OctBox(parent, off, extent);
    }

    //TODO pass the target box as a parameter so it can base its decision on that
    protected Collection<IdBB> newItemCollection() {
        //return new FastList();
        return new HashSet<>();
    }


    public void forEachRecursive(Consumer<IdBB> visitor) {
        forEachLocal(visitor);
        OctBox[] cc = this.children;
        if (cc !=null) {
            for (OctBox<K> c : cc) {
                if (c!=null)
                    c.forEachRecursive(visitor);
            }
        }
    }

    public void forEachRecursiveWithBox(BiConsumer<OctBox<K>, IdBB> visitor) {
        forEachLocal(i -> visitor.accept(OctBox.this, i));

        OctBox[] cc = this.children;
        if (cc !=null) {
            for (OctBox<K> c : cc) {
                if (c!=null)
                    c.forEachRecursiveWithBox(visitor);
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
            return children.clone();
//            OctBox[] clones = new OctBox[8];
//            System.arraycopy(children, 0, clones, 0, 8);
//            return clones;
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

    public final Collection<IdBB> getItems() {
        final Collection<IdBB> i = this.items;
        if (i == null) return Collections.EMPTY_LIST;
        return i;
    }

    public final int itemCountRecursively() {
        final int[] x = {0};
        forEachBox(n -> x[0] += n.itemCount());
        return x[0];
    }

    public final int itemCount() {
        final Collection<IdBB> i = this.items;
        if (i == null) return 0;
        return i.size();
    }

    public List<IdBB> getItemsRecursively() {
        return getItemsRecursively(new FastList());
    }

    /**
     * @return the points
     */
    public List<IdBB> getItemsRecursively(List<IdBB> results) {
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
    @Deprecated public List<IdBB> getItemsWithin(BB b) {
        List<IdBB> results = null;
        if (this.intersectsBox(b)) {
            if (items != null) {
                for (IdBB q : items) {
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
                        List<IdBB> points = children[i].getItemsWithin(b);
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

    public void forEachBox(BB b, Consumer<IdBB> c) {
        if (this.intersectsBox(b)) {
            final OctBox[] childs = this.children;
            if (items != null) {
                for (IdBB q : items) {
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

    public void forEachNeighbor(IdBB item, XYZ boxRadius, Consumer<OctBox> visitor) {
        //SOON
        throw new UnsupportedOperationException();
    }

    public void forEachInSphere(Sphere s, Consumer<IdBB> c) {

        if (this.intersectsSphere(s)) {
            if (items != null) {
                for (IdBB q : items) {
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
    @Deprecated public List<IdBB> getItemsWithin(Sphere s) {
        List<IdBB> results = null;
        if (this.intersectsSphere(s)) {
            if (items != null) {
                for (IdBB q : items) {
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
                        List<IdBB> points = children[i].getItemsWithin(s);
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
    public void forEachInSphere(Vec3D sphereOrigin, float clipRadius, Consumer<IdBB> c) {
        forEachInSphere(new Sphere(sphereOrigin, clipRadius), c);
    }

    private boolean reduceBranch() {
        boolean modified = false;
        if (items != null && items.isEmpty()) {
            items = null;
            modified = true;
        }
        if (children!=null) {
            int nullCount = 0;
            for (int i = 0; i < 8; i++) {
                OctBox ci = children[i];
                if (ci != null) {
                    if ((ci.items == null)) {
                        children[i] = null;
                        modified = true;
                        nullCount++;
                    }
                } else {
                    nullCount++;
                }
            }
            if (nullCount == 8) {
                children = null;
                modified = true;
            }
        }
        if (parent != null) {
            if (parent.reduceBranch())
                parent.onModified();
        }
        return modified;
    }

    protected void onModified() {

    }

    /**
     * Removes a point from the tree and (optionally) tries to release memory by
     * reducing now empty sub-branches.
     *
     * @return true, if the point was found & removed
     */
    public boolean remove(Object _p) {
        boolean found = false;
        IdBB p = (IdBB)_p;
        OctBox leaf = getLeafForPoint(p.getBB());
        if (leaf != null) {
            if (leaf.items.remove(p)) {
                found = true;
                if (leaf.items.size() == 0) {
                    leaf.reduceBranch();
                }
            }
        }

        if (found)
            onModified();

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
        Collection<IdBB> ii = this.items;
        String x = "<OctBox:" + super.toString() + ":" +
                ((ii !=null) ? ii.size() : 0);
        return x;
    }

    /** identified bounding box */
    public interface IdBB {
        String id();
        BB getBB();
    }
}
