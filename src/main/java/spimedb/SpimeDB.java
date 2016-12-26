package spimedb;

import java.util.function.Consumer;


public interface SpimeDB extends Iterable<NObject> {

    String VERSION = "Spacetime ClimateNet -0.00";

    void close();

    NObject put(NObject d);

    NObject get(String nobjectID);

    boolean isEmpty();

    int size();

    void children(String parent, Consumer<String> each);

    Iterable<NObject> intersecting(double lat, double lon, double radMeters, int maxResults);

    enum Scope {
        Private, Trusted, Public, Anonymous, Advertise
    }

}
