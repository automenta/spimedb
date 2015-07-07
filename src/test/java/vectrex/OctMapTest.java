package vectrex;

import org.junit.Test;
import spangraph.InfiniPeer;
import toxi.geom.Vec3D;
import toxi.geom.XYZ;

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

    @Test
    public void test1() {
        InfiniPeer p = InfiniPeer.local("i", "/tmp/t", 1024);
        OctMap<Event,String> o = new OctMap<>(p, "octmap",
                new Vec3D(-180f,-90f,0f ), /* AD */
                new Vec3D(180f,90f,2100f ), /* 0AD .. 2100AD */
                new Vec3D(1f,0.75f,2f)
        );

        o.clear();

        Event[] ee = new Event[] {
                new Event("Beginning of Common Era", 75f, -20f, 0),
                new Event("Invasion of Western Hemisphere", 65f, 34f, 1492f),
                new Event("Monolith Discovered", -50f, 40f, 2001f),
                new Event("Earth Destroyed", 0, 10f, 2015f)};

        for (Event e  : ee) {
            o.put(e, "");
        }

        System.out.println(o);
        o.box().forEachInBox(x -> System.out.println(x));


        assertTrue(o.validate());


        o.flush();
        p.stop();
    }
}
