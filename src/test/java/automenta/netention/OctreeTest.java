package automenta.netention;

import org.junit.Test;
import toxi.geom.AABB;
import toxi.geom.BB;
import toxi.geom.Vec3D;
import vectrex.IdBB;
import vectrex.OctBox;

import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static toxi.geom.Vec3D.v;

/**
 * Created by me on 6/13/15.
 */
public class OctreeTest {


    static class DummyPoint extends AABB implements IdBB<String> {

        final String id = UUID.randomUUID().toString();

        DummyPoint(float x, float y, float z, float r) {
            super(x, y, z, r);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object v) {
            return v == this;
        }

        @Override
        public BB getBB() {
            return this;
        }
    }

    static DummyPoint point(float x, float y, float z) {
        return cube(x, y, z, 0.01f);
    }
    static DummyPoint cube(float x, float y, float z, float l) {
        return new DummyPoint(x, y, z, l);
    }
    
    /** points or volumes below the minimum resolution of the tree */
    @Test public void testPoints() {
        OctBox<String> o = new OctBox<>(
                point(-2f, -2f, -2f),
                point(4f, 4f, 4f),
                point(0.05f, 0.05f, 0.05f));

        assertEquals(0, o.itemCountRecursively());

        OctBox<String> block = o.put(point(3f, 3f, 3f));
        assertTrue(block!=null);
        assertEquals(1, o.itemCountRecursively());

        o.put(point(0, 1, 0));
        o.put(point(0, 1, 0));
        o.put(point(0, 0, 1));
        o.put(point(0, 0, 1.25f));
        o.put(point(0, 0, 1.5f));
        o.put(point(0, 0, -1));
        o.put(point(0, 0, -1.25f));
        o.put(point(0, 0, -1.50f));
        o.put(point(0, 0, -1.55f));
        o.put(point(0, 0, -1.575f));

        System.out.println(o);
        o.forEachBox(System.out::println);


        o.forEachBox(x -> {
            Collection p = (x.getItems());
            //if (!p.isEmpty())
            System.out.println(x + " " + p);
        });

        //System.out.println("size: " + o.getNumChildren());

        assertEquals(o.itemCountRecursively(), 11);

        int[] sphereCount = new int[1];
        o.forEachInSphere(new Vec3D(0, 0, -0.75f), 0.5f, x -> {
            sphereCount[0]++;
        });
        assertEquals(2, sphereCount[0]);

        int[] boxCount = new int[1];

        BB BB = new AABB(new Vec3D(0f, -0.5f, -2.0f), new Vec3D(0.5f, 0.5f, 0.5f));
        o.forEachBox(BB, x -> {
            boxCount[0]++;
        });
        assertEquals(3, boxCount[0]);

    }

    /** both points AND cubes above the minimum resolution of the
     *  tree;
     *  the cubes will be stored in non-leaf nodes */
    @Test public void testNonPoints() {
        OctBox<String> octBox = new OctBox<>(
                v(-2f, -2f, -2f),
                v(4f, 4f, 4f),
                v(0.05f, 0.05f, 0.05f));

        DummyPoint p;
        octBox.put(p = point(0, 0, 0));
        DummyPoint c;
        octBox.put(c = cube(0, 0, 0, 0.5f));

        assertEquals(2, octBox.itemCountRecursively());

        //System.out.println(octBox);
        octBox.forEach((subBox, item) ->
                System.out.println(subBox + " " + item));

        OctBox pBox = octBox.getLeafForPoint(p);
        //System.out.println("box for p: " + pBox);
        assertTrue(pBox.holds(p)); //the point is at the leaf
        assertEquals(1, pBox.itemCount());

        OctBox pBoxParent = pBox.getParent().getParent().getParent().getParent();
        //System.out.println("box for p parent: " + pBoxParent);
        assertFalse(pBox.holds(c)); //the cube is NOT at the leaf
        assertTrue(pBoxParent.holds(c)); //the cube is at a parent of the leaf because of its volume

        assertEquals(0, pBoxParent.getParent().itemCount()); //zero items in the parent box of the box holding c
    }
}
