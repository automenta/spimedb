package spimedb.server;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by me on 3/30/17.
 */
public class Router<K,V> {


    final ConcurrentHashMap<K,
            Set<V>> on = new ConcurrentHashMap();

    public boolean on(K k, V v) {
        Set<V> www = on.computeIfAbsent(k, (kk) -> Sets.newConcurrentHashSet());
        return www.add(v);
    }

    public void emit(String[] ss, Consumer<V> vv) {
        for (String s : ss) {
            Set<V> vs = on.get(s);
            if (vs!=null)
                vs.forEach(vv::accept);
        }
    }

    public void off(Iterable<? extends K> k, V v) {
        k.forEach(kk -> off(kk, v));
    }

    public void off(K k, V v) {

        on.computeIfPresent(k, (kk,vv)-> {
            //done here so it's atomic
            if (vv!=null) {
                if (vv.remove(v) && vv.isEmpty()) {
                    return null;
                }
            }
            return vv;
        });
    }
}
