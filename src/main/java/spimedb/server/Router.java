package spimedb.server;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sf.ehcache.util.WeakIdentityConcurrentMap;
import org.mockito.internal.util.concurrent.WeakConcurrentSet;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by me on 3/30/17.
 */
public class Router<K,V> {


    final ConcurrentHashMap<K,
            Set<V>> on = new ConcurrentHashMap();

    public void on(K k, V v) {
        Set<V> www = on.compute(k, (kk, vv) -> {
            if (vv == null) {
//                vv = new WeakConcurrentSet(
//                        WeakConcurrentSet.Cleaner.INLINE);
                vv = Sets.newConcurrentHashSet();
            }
            return vv;
        });
        www.add(v);
    }

    public void each(String[] ss, Consumer<V> vv) {
        for (String s : ss) {
            on.get(s).forEach(vv::accept);
        }
    }

    public void off(Iterable<? extends K> k, V v) {
        k.forEach(kk -> off(kk, v));
    }

    public void off(K k, V v) {
        on.computeIfPresent(k, (kk,vv)-> {
            vv.remove(v); //done here so it's atomic
            return vv;
        });
    }
}
