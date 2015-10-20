package automenta.netention;

import org.junit.Test;
import spangraph.InfiniPeer;
import toxi.geom.Vec3D;
import toxi.geom.XYZ;
import vectrex.OctMap;

import java.io.Serializable;

import static org.junit.Assert.assertTrue;

/**
 * Created by me on 6/14/15.
 */
public class OctMapTest {

    public static class Event implements XYZ, Serializable {

        public final String name;
        private final float lon, lat, year;

        public Event(String name, float lon, float lat, float year)  {
            this.name = name;
            this.lon = lon;
            this.lat = lat;
            this.year = year;
        }

        @Override
        public boolean equals(Object that) {
            if (that == this) return true;
            return name.equals(((Event)that).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "Event[\'" + name + "\']";
        }

        @Override
        public float x() {
            return lon;
        }

        @Override
        public float y() {
            return lat;
        }

        @Override
        public float z() {
            return year;
        }
    }

    InfiniPeer p = InfiniPeer.tmp();
    OctMap<Event, String> o = new OctMap<>(p, "octmap",
            new Vec3D(-180f, -90f, 0f), /* AD */
            new Vec3D(180f, 90f, 2100f), /* 0AD .. 2100AD */
            new Vec3D(1f, 0.75f, 2f)
    );

    @Test
    public synchronized void test0() {
        o.clear();
        assertTrue(o.size() == 0);
        assertTrue(o.isEmpty());

        o.put(new Event("Beginning of Common Era", 75f, -20f, 0), "");
        o.box().forEachInBox(x -> System.out.println(x));
        assertTrue(o.validate());
    }

    @Test
    public synchronized void test1() {
        o.clear();
        Event[] ee = new Event[] {
                new Event("Beginning of Common Era", 75f, -20f, 0),
                new Event("Invasion of Western Hemisphere", 65f, 34f, 1492f),
                new Event("Monolith Discovered", -50f, 40f, 2001f),
                new Event("Earth Destroyed", 0, 10f, 2015f)};

        for (Event e  : ee) {
            o.put(e, "");
        }

        //System.out.println(o);
        o.box().forEachInBox(x -> System.out.println(x));

        assertTrue(o.validate());

    }

//    @After
//    public void stop() {
//        o.flush();
//        p.stop();
//    }
}