package spimedb.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Locker<X> {

    public final ConcurrentHashMap<X,DBLock> lock = new ConcurrentHashMap<>();

    public Lock get(X id) {
        return lock.computeIfAbsent(id, DBLock::new);
    }

    public final class DBLock extends ReentrantLock {
        private final X id;

        public DBLock(X id) {
            super(true);
            this.id = id;
        }

        //TODO did this need to be synchronized?
        @Override public void unlock() {
            super.unlock();
            lock.computeIfPresent(id,
                (k,v) -> (!hasQueuedThreads()) ? null : v);
        }
    }

}
