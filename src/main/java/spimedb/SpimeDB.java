package spimedb;

import java.util.Iterator;
import java.util.function.Consumer;


public interface SpimeDB extends Iterable<NObject> {

    String VERSION = "Spacetime ClimateNet -0.00";

    void close();

    NObject put(NObject d);

    NObject get(String nobjectID);

    boolean isEmpty();

    int size();

    void children(String parent, Consumer<String> each);

    Iterator<NObject> get(double lat, double lon, double radMeters, int maxResults);

    public enum Scope {
        Private, Trusted, Public, Anonymous, Advertise
    }

}
