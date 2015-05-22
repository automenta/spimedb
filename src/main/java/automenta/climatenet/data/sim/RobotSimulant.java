package automenta.climatenet.data.sim;

import automenta.climatenet.p2p.NObject;
import com.google.common.collect.Lists;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by me on 4/14/15.
 */
public class RobotSimulant extends NObject {

    //boolean air;
    //boolean ground;

    /** 2d shadow (top-down) geometric representation, local coordinates */
    Polygon shadowLocal;

    /** 2d shadow (top-down) geometric representation, world coordinates */
    transient Polygon shadowWorld;


    private List<NObject> memory = new ArrayList();

    public interface SimulantController {
        void update(RobotSimulant r, double dt);
    }

    public final List<SimulantController> controllers;

    public RobotSimulant(String id, double lat, double lon, double size, SimulantController... ctl) {
        super(id);

        where(lat, lon);

        shadowLocal = new Polygon(l(-size/2,-size/2), l(-size/2,-size/2), l(size/2,size/2), l(size/2,size/2));
        shadowWorld = new Polygon(
                new LngLatAlt(),
                new LngLatAlt(),
                new LngLatAlt(),
                new LngLatAlt()
        );
        controllers = Lists.newArrayList(ctl);
    }

    public RobotSimulant know(NObject... n) {
        Collections.addAll(memory, n);
        return this;
    }
    public RobotSimulant knowHere(NObject... n) {
        for (NObject x : n)
            x.where(getWhere());
        Collections.addAll(memory, n);
        return this;
    }

    public static class GeoSynchOrbitController implements SimulantController {

        double lonPerSecond = 0.1;
        double altitutde = 0;

        @Override
        public void update(RobotSimulant r, double dt) {
            r.where(0, lonPerSecond * dt + r.getWhere().getLongitude());
        }
    }

    public static class CircleController implements SimulantController {

        double radPerSec = 0.1;
        double radius = 0.1;
        double altitutde = 0;
        double t =0;
        public LngLatAlt center;

        public CircleController(LngLatAlt center) {
            this.center = center;
        }

        @Override
        public void update(RobotSimulant r, double dt) {
            t += dt;
            double y = Math.sin(radPerSec * t) * radius;
            double x = Math.cos(radPerSec * t) * radius;
            r.where(y + center.getLatitude(), x + center.getLongitude());
        }
    }

    public static class VelocityController implements SimulantController {
        public final LngLatAlt velocity = new LngLatAlt(0,0,0);

        @Override
        public void update(RobotSimulant r, double dt) {
            r.where(velocity.getLatitude() * dt, velocity.getLongitude() * dt);
            //r.position.setAltitude(velocity.getAltitude() * dt);
        }
    }

    public List<NObject> getMemory() {
        return memory;
    }

    public Polygon update(double dt) {

        for (SimulantController c : controllers) {
            c.update(this, dt);
        }

        int npoints = shadowWorld.getCoordinates().get(0).size();
        for (int i = 0; i < npoints; i++) {
            LngLatAlt w = shadowWorld.getCoordinates().get(0).get(i);
            LngLatAlt l = shadowLocal.getCoordinates().get(0).get(i);
            w.setLongitude(getWhere().getLongitude() + l.getLongitude());
            w.setLatitude(getWhere().getLatitude() + l.getLatitude());
        }

        return shadowWorld;
    }

    public static LngLatAlt l(double lat, double lon) {
        return new LngLatAlt(lon, lat);
    }

}
