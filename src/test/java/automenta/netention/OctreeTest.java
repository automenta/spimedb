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
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 6/13/15.
 */
public class OctreeTest {

    static class DummyPoint extends AABB implements IdBB<String> {

        final String id = UUID.randomUUID().toString();

        public DummyPoint(float x, float y, float z) {
            super(x, y, z, 1);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public BB getBB() {
            return this;
        }
    }

    @Test
    public void test1() {
        OctBox<String> o = new OctBox<>(
                new DummyPoint(-2f, -2f, -2f),
                new DummyPoint(4f, 4f, 4f),
                new DummyPoint(0.05f, 0.05f, 0.05f));

        assertEquals(0, o.countPointsRecursively());

        OctBox<String> block = o.ADD(new DummyPoint(3f, 3f, 3f));
        assertTrue(block!=null);
        assertEquals(1, o.countPointsRecursively());

        o.ADD(new DummyPoint(0, 1, 0));
        o.ADD(new DummyPoint(0, 1, 0));
        o.ADD(new DummyPoint(0, 0, 1));
        o.ADD(new DummyPoint(0, 0, 1.25f));
        o.ADD(new DummyPoint(0, 0, 1.5f));
        o.ADD(new DummyPoint(0, 0, -1));
        o.ADD(new DummyPoint(0, 0, -1.25f));
        o.ADD(new DummyPoint(0, 0, -1.50f));
        o.ADD(new DummyPoint(0, 0, -1.55f));
        o.ADD(new DummyPoint(0, 0, -1.575f));

        System.out.println(o);
        o.forEachInBox(System.out::println);


        o.forEachInBox(x -> {
            Collection p = (x.getPoints());
            //if (!p.isEmpty())
            System.out.println(x + " " + p);
        });

        //System.out.println("size: " + o.getNumChildren());

        assertEquals(o.countPointsRecursively(), 11);

        int[] sphereCount = new int[1];
        o.forEachInSphere(new Vec3D(0, 0, -0.75f), 0.5f, x -> {
            sphereCount[0]++;
        });
        assertEquals(2, sphereCount[0]);

        int[] boxCount = new int[1];

        BB BB = new AABB(new Vec3D(0f, -0.5f, -2.0f), new Vec3D(0.5f, 0.5f, 0.5f));
        o.forEachInBox(BB, x -> {
            boxCount[0]++;
        });
        assertEquals(3, boxCount[0]);



    }
}
