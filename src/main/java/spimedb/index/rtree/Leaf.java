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

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Node that will contain the data entries. Implemented by different type of SplitType leaf classes.
 * <p>
 * Created by jcairns on 4/30/15.
 */
abstract class Leaf<T> implements Node<T> {
    final int mMax;       // max entries per node
    final int mMin;       // least number of entries per node

    final HyperRect[] r;
    final T[] entry;

    final RectBuilder<T> builder;
    final RTree.Split splitType;
    HyperRect mbr;
    int size;

    Leaf(final RectBuilder<T> builder, final int mMin, final int mMax, final RTree.Split splitType) {
        this.mMin = mMin;
        this.mMax = mMax;
        this.mbr = null;
        this.builder = builder;
        this.r = new HyperRect[mMax];
        this.entry = (T[]) new Object[mMax];
        this.size = 0;
        this.splitType = splitType;
    }

    @Override
    public Node<T> add(final T t) {
        if (size < mMax) {
            final HyperRect tRect = builder.apply(t);
            mbr = mbr != null ? mbr.mbr(tRect) : tRect;

            r[size] = tRect;
            entry[size++] = t;
        } else {
            for (int i = 0; i < size; i++) {
                if (entry[i] == null) {
                    entry[i] = t;
                    r[i] = builder.apply(t);
                    mbr = mbr.mbr(r[i]);
                    return this;
                }
            }
            return split(t);
        }

        return this;
    }

    @Override
    public Node<T> remove(final T t) {
        for (int i = 0; i < size; i++) {
            if (entry[i].equals(t)) {
                entry[i] = null;
                r[i] = null;
                if (i < (size - 1)) {
                    entry[i] = entry[size - 1];
                    r[i] = r[size - 1];
                    entry[size - 1] = null;
                    r[size - 1] = null;
                }
                size--;
                if (size > 0) {
                    mbr = r[0];
                    for (i = 1; i < size; i++) {
                        mbr = mbr.mbr(r[i]);
                    }
                }
                return this;
            }
        }
        return null;
    }

    @Override
    public Node<T> update(final T told, final T tnew) {

        remove(told);
        add(tnew);

        return this;
    }

    @Override
    public boolean containing(HyperRect rect, Predicate<T> t) {
        for (int i = 0; i < size; i++) {
            if (rect.contains(r[i])) {
                if (!t.test(entry[i]))
                    return false;
            }
        }
        return true;
    }

    @Override
    public int containing(final HyperRect rect, final T[] t, int n) {
        final int tLen = t.length;
        final int n0 = n;

        for (int i = 0; i < size && n < tLen; i++) {
            if (rect.contains(r[i])) {
                t[n++] = entry[i];
            }
        }
        return n - n0;
    }

    @Override
    public int size() {
        return size;
    }

    public T getEntry(final int dx) {
        return entry[dx];
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public HyperRect bounds() {
        return mbr;
    }

    /**
     * Splits a lead node that has the maximum number of entries into 2 leaf nodes of the same type with half
     * of the entries in each one.
     *
     * @param t entry to be added to the full leaf node
     * @return newly created node storing half the entries of this node
     */
    protected abstract Node<T> split(final T t);

    @Override
    public void forEach(Consumer<T> consumer) {
        for (int i = 0; i < size; i++) {
            consumer.accept(entry[i]);
        }
    }

    @Override
    public boolean intersecting(HyperRect rect, Predicate<T> t) {
        for (int i = 0; i < size; i++) {
            if (rect.intersects(r[i])) {
                if (!t.test(entry[i]))
                    return false;
            }
        }
        return true;
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        if (depth > stats.getMaxDepth()) {
            stats.setMaxDepth(depth);
        }
        stats.countLeafAtDepth(depth);
        stats.countEntriesAtDepth(size, depth);
    }

    /**
     * Figures out which newly made leaf node (see split method) to add a data entry to.
     *
     * @param l1Node left node
     * @param l2Node right node
     * @param t      data entry to be added
     */
    final void classify(final Node<T> l1Node, final Node<T> l2Node, final T t) {

        final HyperRect tRect = builder.apply(t);
        final HyperRect l1Mbr = l1Node.bounds().mbr(tRect);

        double tCost = tRect.cost();

        double l1c = l1Mbr.cost();
        final double l1CostInc = Math.max(l1c - (l1Node.bounds().cost() + tCost), 0.0);
        final HyperRect l2Mbr = l2Node.bounds().mbr(tRect);
        double l2c = l2Mbr.cost();
        final double l2CostInc = Math.max(l2c - (l2Node.bounds().cost() + tCost), 0.0);
        if (l2CostInc > l1CostInc) {
            l1Node.add(t);
        } else if (RTree.equals(l1CostInc, l2CostInc)) {
            final double l1MbrCost = l1c;
            final double l2MbrCost = l2c;
            if (l1MbrCost < l2MbrCost) {
                l1Node.add(t);
            } else if (RTree.equals(l1MbrCost, l2MbrCost)) {
                final double l1MbrMargin = l1Mbr.perimeter();
                final double l2MbrMargin = l2Mbr.perimeter();
                if (l1MbrMargin < l2MbrMargin) {
                    l1Node.add(t);
                } else if (RTree.equals(l1MbrMargin, l2MbrMargin)) {
                    // break ties with least number
                    ((l1Node.size() < l2Node.size()) ? l1Node : l2Node).add(t);

                } else {
                    l2Node.add(t);
                }
            } else {
                l2Node.add(t);
            }
        } else {
            l2Node.add(t);
        }

    }

    @Override
    public Node<T> instrument() {
        return new CounterNode<>(this);
    }

    @Override
    public String toString() {
        return "Leaf" + splitType + "{" + mbr + "x" + size +'}';
    }
}
