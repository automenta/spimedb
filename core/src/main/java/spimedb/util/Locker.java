package spimedb.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;


public class Locker<X> {

    final static Logger logger = LoggerFactory.getLogger(Locker.class);

    final Cache<X, DBLock> lock = Caffeine.newBuilder().weakValues().build();

    Lock get(X id) {
        return lock.get(id, DBLock::new);
    }

    public Lock lock(X id) {
        Lock l = get(id);
        l.lock();
        //logger.debug("lock {}", id);
        return l;
    }

    public void locked(X id, Runnable r) {
        Lock l = lock(id);
        try {
            r.run();
        } finally {
            l.unlock();
        }
    }

    public void locked(X x, Consumer<X> r) {
        locked(x, (xx) -> {
            r.accept(xx);
            return null;
        });
    }

    public <Y> Y locked(X x, Function<X, Y> r) {
        Lock l = lock(x);
        Y y = null;
        try {
            y = r.apply(x);
        } finally {
            l.unlock();
        }
        return y;
    }

    public final class DBLock extends ReentrantLock {
        private final X id;

        public DBLock(X id) {
            super(true);
            this.id = id;
        }

//        //TODO did this need to be synchronized?
//        @Override
//        public void unlock() {
//            /*if (logger.isDebugEnabled())
//                logger.debug("unlock {}, queue={}", id, getQueuedThreads());*/
//            super.unlock();
//        }
    }

}
