package spimedb;

import java.util.function.Predicate;


public interface SpimeDB extends Iterable<NObject> {



    String VERSION = "SpimeDB v-0.00";

    void close();

    NObject put(NObject d);

    NObject get(String nobjectID);


    boolean isEmpty();

    int size();


    Iterable<NObject> intersecting(double lon, double lat, double radMeters, int maxResults);

    void intersecting(float lon, float lat, float radMeters, Predicate<NObject> each);

    void intersecting(float[] lon, float[] lat, Predicate<NObject> each);

    /** (re-)creates new tag (class) */
    Class the(String id, String... supertags);

    /** (re-)creates new instance */
    NObject a(String id, String... tags);

    enum Scope {
        Private, Trusted, Public, Anonymous, Advertise
    }

    enum OpEdge {
        extinh, intinh
    }
}
