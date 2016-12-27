package spimedb;

import spimedb.db.RTreeSpimeDB;

import java.util.function.Consumer;
import java.util.function.Predicate;


public interface SpimeDB extends Iterable<NObject> {

    String VERSION = "Spacetime ClimateNet -0.00";

    void close();

    NObject put(NObject d);

    NObject get(String nobjectID);

    void edgeAdd(String subject, RTreeSpimeDB.OpEdge e, String object);
    void edgeRemove(String subject, RTreeSpimeDB.OpEdge e, String object);

    boolean isEmpty();

    int size();

    void children(String parent, Consumer<String> each);

    Iterable<NObject> intersecting(double lon, double lat, double radMeters, int maxResults);

    void intersecting(float lon, float lat, float radMeters, Predicate<NObject> each);

    void intersecting(float[] lon, float[] lat, Predicate<NObject> each);

    enum Scope {
        Private, Trusted, Public, Anonymous, Advertise
    }

    enum OpEdge {
        extinh, intinh
    }
}
