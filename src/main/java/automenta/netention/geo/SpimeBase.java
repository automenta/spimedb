package automenta.netention.geo;

import automenta.netention.NObject;

import java.util.Collection;
import java.util.Iterator;


public interface SpimeBase extends Iterable<NObject> {
    NObject put(NObject d);

    @Override
    Iterator<NObject> iterator();

    boolean isEmpty();

    int size();

    NObject get(String nobjectID);

    Collection<String> getInsides(String nobjectID);


    Iterator<NObject> get(double lat, double lon, double radMeters, int maxResults);

    static NObject newNObject() {
        return new NObject();
    }

    static NObject newNObject(String id) {
        return new NObject(id);
    }

}
