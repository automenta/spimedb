package spimedb.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by me on 2/9/17.
 */
public class Locker<X> {

    public final ConcurrentHashMap<X,DBLock> lock = new ConcurrentHashMap<>();

    public Lock get(X id) {
        return lock.computeIfAbsent(id, DBLock::new);
    }

    public final class DBLock extends ReentrantLock {
        private final Object id;

        public DBLock(Object id) {
            super(true);
            this.id = id;
        }

        @Override
        public synchronized void unlock() {
            if (!hasQueuedThreads()) {
                lock.remove(id);
            }
            super.unlock();
        }
    }

}
