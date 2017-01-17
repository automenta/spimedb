package spimedb.server;

import com.google.common.util.concurrent.RateLimiter;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.infinispan.commons.util.concurrent.ConcurrentHashSet;
import spimedb.util.bloom.UnBloomFilter;

import java.util.Set;

/**
 * interactive session (ie, Client as seen from Server)
 * TODO: https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
 */
public class Session extends ServerWebSocket {
    /** bytes per second */
    public static final int OUTPUT_throttle = 8 * 1024;

    /** max # of items that can be remembered to have already been sent.
     * this should not exceed the client's object bag capacity, which it should configure on
     * connecting or changing its capacity */
    final int ALREADY_SENT_MEMORY_CAPACITY = 16;

    /** response bandwidth throttle */
    final RateLimiter outRate = RateLimiter.create(OUTPUT_throttle);

    final Set<Task> active = new ConcurrentHashSet<>();

    final ObjectFloatHashMap<String> attention = new ObjectFloatHashMap<>();

    final UnBloomFilter<String> sent = new UnBloomFilter<>(ALREADY_SENT_MEMORY_CAPACITY, String::getBytes);


}
