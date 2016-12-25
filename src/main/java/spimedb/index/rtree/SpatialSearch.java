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

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Created by jcovert on 12/30/15.
 */
public interface SpatialSearch<T> {

    int DEFAULT_MIN_M = 2;
    int DEFAULT_MAX_M = 8;
    RTree.Split DEFAULT_SPLIT_TYPE = RTree.Split.AXIAL;

    /**
     * Create an R-Tree with default values for m, M, and split type
     *
     * @param builder - Builder implementation used to create HyperRects out of T's
     * @param <T>     - The store type of the bound
     * @return SpatialSearch - The spatial search and index structure
     */
    public static <T> SpatialSearch<T> rTree(final RectBuilder<T> builder) {
        return new RTree<>(builder, DEFAULT_MIN_M, DEFAULT_MAX_M, DEFAULT_SPLIT_TYPE);
    }

    /**
     * Create an R-Tree with specified values for m, M, and split type
     *
     * @param builder   - Builder implementation used to create HyperRects out of T's
     * @param minM      - minimum number of entries per node of this tree
     * @param maxM      - maximum number of entries per node of this tree (exceeding this causes node split)
     * @param splitType - type of split to use when M+1 entries are added to a node
     * @param <T>       - The store type of the bound
     * @return SpatialSearch - The spatial search and index structure
     */
    static <T> SpatialSearch<T> rTree(final RectBuilder<T> builder, final int minM, final int maxM, final RTree.Split splitType) {
        return new RTree<>(builder, minM, maxM, splitType);
    }

    /**
     * Create a protected R-Tree with default values for m, M, and split type
     *
     * @param builder - Builder implementation used to create HyperRects out of T's
     * @param <T>     - The store type of the bound
     * @return SpatialSearch - The spatial search and index structure
     */
    static <T> SpatialSearch<T> lockingRTree(final RectBuilder<T> builder) {
        return new LockingRTree<>(rTree(builder), new ReentrantReadWriteLock(true));
    }

    /**
     * Create a protected R-Tree with specified values for m, M, and split type
     *
     * @param builder   - Builder implementation used to create HyperRects out of T's
     * @param minM      - minimum number of entries per node of this tree
     * @param maxM      - maximum number of entries per node of this tree (exceeding this causes node split)
     * @param splitType - type of split to use when M+1 entries are added to a node
     * @param <T>       - The store type of the bound
     * @return SpatialSearch - The spatial search and index structure
     */
    static <T> SpatialSearch<T> lockingRTree(final RectBuilder<T> builder, final int minM, final int maxM, final RTree.Split splitType) {
        return new LockingRTree<>(rTree(builder, minM, maxM, splitType), new ReentrantReadWriteLock(true));
    }

    /**
     * Search for entries intersecting given bounding rect
     *
     * @param rect - Bounding rectangle to use for querying
     * @param t    - Array to store found entries
     * @return Number of results found
     */
    int containing(final HyperRect rect, final T[] t);


    /**
     * Add the data entry to the SpatialSearch structure
     *
     * @param t Data entry to be added
     */
    void add(final T t);

    /**
     * Remove the data entry from the SpatialSearch structure
     *
     * @param t Data entry to be removed
     */
    void remove(final T t);

    /**
     * Update entry in tree
     *
     * @param told - Entry to update
     * @param tnew - Entry to update it to
     */
    void update(final T told, final T tnew);

    /**
     * Get the number of entries in the tree
     *
     * @return entry count
     */
    int size();

    void forEach(Consumer<T> consumer);

    void intersecting(HyperRect rect, Consumer<T> consumer);

    Stats stats();

}
