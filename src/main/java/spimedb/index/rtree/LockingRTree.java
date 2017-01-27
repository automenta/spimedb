package spimedb.index.rtree;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by jcovert on 12/30/15.
 */
public class LockingRTree<T> implements SpatialSearch<T> {

    private final SpatialSearch<T> rTree;
    private final Lock readLock;
    private final Lock writeLock;

    public LockingRTree(SpatialSearch<T> rTree, ReadWriteLock lock) {
        this.rTree = rTree;
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    /**
     * Blocking locked search
     *
     * @param rect - HyperRect to search
     * @param t    - array to hold results
     * @return number of entries found
     */
    @Override
    public int containing(HyperRect rect, T[] t) {
        readLock.lock();
        try {
            return rTree.containing(rect, t);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Blocking locked add
     *
     * @param t - entry to add
     */
    @Override
    public void add(T t) {
        writeLock.lock();
        try {
            rTree.add(t);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Blocking locked remove
     *
     * @param t - entry to remove
     */
    @Override
    public void remove(T t) {
        writeLock.lock();
        try {
            rTree.remove(t);
        } finally {
            writeLock.unlock();
        }
    }

//    @Override
//    public void change(T x, T y) {
//        writeLock.lock();
//        try {
//            rTree.remove(x);
//            rTree.add(y);
//        } finally {
//            writeLock.unlock();
//        }
//    }

    /**
     * Blocking locked update
     *
     * @param told - entry to update
     * @param tnew - entry with new value
     */
    @Override
    public void update(T told, T tnew) {
        writeLock.lock();
        try {
            rTree.update(told, tnew);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Non-blocking locked search
     *
     * @param rect - HyperRect to search
     * @param t    - array to hold results
     * @return number of entries found or -1 if lock was not acquired
     */
    public int trySearch(HyperRect rect, T[] t) {
        if (readLock.tryLock()) {
            try {
                return rTree.containing(rect, t);
            } finally {
                readLock.unlock();
            }
        }
        return -1;
    }

    /**
     * Non-blocking locked add
     *
     * @param t - entry to add
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryAdd(T t) {
        if (writeLock.tryLock()) {
            try {
                rTree.add(t);
            } finally {
                writeLock.unlock();
            }
            return true;
        }
        return false;
    }

    /**
     * Non-blocking locked remove
     *
     * @param t - entry to remove
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryRemove(T t) {
        if (writeLock.tryLock()) {
            try {
                rTree.remove(t);
            } finally {
                writeLock.unlock();
            }
            return true;
        }
        return false;
    }

    /**
     * Non-blocking locked update
     *
     * @param told - entry to update
     * @param tnew - entry with new values
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryUpdate(T told, T tnew) {
        if (writeLock.tryLock()) {
            try {
                rTree.update(told, tnew);
            } finally {
                writeLock.unlock();
            }
            return true;
        }
        return false;
    }

    @Override
    public int size() {
        return rTree.size();
    }

    @Override
    public void forEach(Consumer<T> consumer) {
        readLock.lock();
        try {
            rTree.forEach(consumer);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containing(HyperRect rect, Predicate<T> consumer) {
        boolean result;
        readLock.lock();
        try {
            result = rTree.containing(rect, consumer);
        } finally {
            readLock.unlock();
        }
        return result;
    }

    @Override
    public boolean intersecting(HyperRect rect, Predicate<T> consumer) {
        boolean result;
        readLock.lock();
        try {
            result = rTree.intersecting(rect, consumer);
        } finally {
            readLock.unlock();
        }
        return result;
    }

    @Override
    public Stats stats() {
        readLock.lock();
        try {
            return rTree.stats();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String toString() {
        return rTree.toString();
    }
}
