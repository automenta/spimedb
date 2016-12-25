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
 * RTree node that contains leaf nodes
 * <p>
 * Created by jcairns on 4/30/15.
 */
final class Branch<T> implements Node<T> {

    private final Node[] child;
    private final RectBuilder<T> builder;
    private final int mMax;
    private final int mMin;
    private final RTree.Split splitType;

    private HyperRect mbr;
    private int size;

    Branch(final RectBuilder<T> builder, final int mMin, final int mMax, final RTree.Split splitType) {
        this.mMin = mMin;
        this.mMax = mMax;
        this.builder = builder;
        this.mbr = null;
        this.size = 0;
        this.child = new Node[mMax];
        this.splitType = splitType;
    }

    /**
     * Add a new node to this branch's list of children
     *
     * @param n node to be added (can be leaf or branch)
     * @return position of the added node
     */
    int addChild(final Node<T> n) {
        if (size < mMax) {
            child[size++] = n;

            HyperRect nr = n.bounds();
            mbr = mbr != null ? mbr.getMbr(nr) : nr;
            return size - 1;
        } else {
            throw new RuntimeException("Too many children");
        }
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public HyperRect bounds() {
        return mbr;
    }

    /**
     * Adds a data entry to one of the child nodes of this branch
     *
     * @param t data entry to add
     * @return Node that the entry was added to
     */
    @Override
    public Node<T> add(final T t) {
        final HyperRect tRect = builder.apply(t);
        if (size < mMin) {
            for (int i = 0; i < size; i++) {
                if (child[i].bounds().contains(tRect)) {
                    child[i] = child[i].add(t);
                    mbr = mbr.getMbr(child[i].bounds());
                    return child[i];
                }
            }
            // no overlapping node - grow
            final Node<T> nextLeaf = Leaf.create(builder, mMin, mMax, splitType);
            nextLeaf.add(t);
            final int nextChild = addChild(nextLeaf);
            mbr = mbr.getMbr(child[nextChild].bounds());

            return this;

        } else {
            final int bestLeaf = chooseLeaf(t, tRect);

            child[bestLeaf] = child[bestLeaf].add(t);

            mbr = mbr.getMbr(child[bestLeaf].bounds());

            // optimize on split to remove the extra created branch when there
            // is space for the children here
            if (child[bestLeaf].size() == 2 &&
                    size < mMax &&
                    child[bestLeaf] instanceof Branch) {
                final Branch<T> branch = (Branch<T>) child[bestLeaf];
                child[bestLeaf] = branch.child[0];
                child[size++] = branch.child[1];
            }

            return this;
        }
    }

    @Override
    public Node<T> remove(final T t) {
        final HyperRect tRect = builder.apply(t);
        Node<T> returned = null;
        for (int i = 0; i < size; i++) {
            if (child[i].bounds().contains(tRect)) {
                returned = child[i].remove(t);

                // Replace a Branch Node with 1 child with it's child
                // Will not work for a RTree with mMin > 2
                if (returned != null) {
                    if (returned.size() == 0) {
                        child[i] = null;
                        if (i < (size - 1)) {
                            child[i] = child[size - 1];
                            child[size - 1] = null;
                        }
                        --size;
                    }
                    if (size == 1) {
                        return child[0];
                    }
                    if (child[i] != null) {
                        if (child[i].size() == 1 && returned.isLeaf()) {
                            child[i] = returned;
                        }
                    }
                }
            }
        }
        return returned;
    }

    @Override
    public Node<T> update(final T told, final T tnew) {
        final HyperRect tRect = builder.apply(told);
        for (int i = 0; i < size; i++) {
            Node c = child[i];
            if (c.bounds().contains(tRect)) {
                child[i] = c = c.update(told, tnew);
                mbr = mbr.getMbr(c.bounds());
                return c;
            }
        }
        return this;
    }

    @Override
    public int containing(final HyperRect rect, final T[] t, int n) {
        final int tLen = t.length;
        final int n0 = n;
        for (int i = 0; i < size && n < tLen; i++) {
            Node c = child[i];
            if (rect.intersects(c.bounds())) {
                n += c.containing(rect, t, n);
            }
        }
        return n - n0;
    }

    @Override
    public boolean containing(final HyperRect rect, final Predicate<T> t) {

        for (int i = 0; i < size; i++) {
            Node c = child[i];
            if (rect.intersects(c.bounds())) {
                if (!c.containing(rect, t))
                    return false;
            }
        }
        return true;
    }

    /**
     * @return number of child nodes
     */
    @Override
    public int size() {
        return size;
    }

    private int chooseLeaf(final T t, final HyperRect tRect) {
        if (size > 0) {
            int bestNode = 0;
            HyperRect childMbr = child[0].bounds().getMbr(tRect);
            double leastEnlargement = childMbr.cost() - (child[0].bounds().cost() + tRect.cost());
            double leastPerimeter = childMbr.perimeter();

            for (int i = 1; i < size; i++) {
                childMbr = child[i].bounds().getMbr(tRect);
                final double nodeEnlargement = childMbr.cost() - (child[i].bounds().cost() + tRect.cost());
                if (nodeEnlargement < leastEnlargement) {
                    leastEnlargement = nodeEnlargement;
                    leastPerimeter = childMbr.perimeter();
                    bestNode = i;
                } else if (RTree.isEqual(nodeEnlargement, leastEnlargement)) {
                    final double childPerimeter = childMbr.perimeter();
                    if (childPerimeter < leastPerimeter) {
                        leastEnlargement = nodeEnlargement;
                        leastPerimeter = childPerimeter;
                        bestNode = i;
                    }
                } // else its not the least

            }
            return bestNode;
        } else {
            final Node<T> n = Leaf.create(builder, mMin, mMax, splitType);
            n.add(t);
            child[size++] = n;

            mbr = mbr == null ? n.bounds() : mbr.getMbr(n.bounds());

            return size - 1;
        }
    }

    /**
     * Return child nodes of this branch.
     *
     * @return array of child nodes (leaves or branches)
     */
    public Node[] getChildren() {
        return child;
    }

    @Override
    public void forEach(Consumer<T> consumer) {
        for (int i = 0; i < size; i++) {
            child[i].forEach(consumer);
        }
    }

    @Override
    public void intersecting(Consumer<T> consumer, HyperRect rect) {
        for (int i = 0; i < size; i++) {
            if (rect.intersects(child[i].bounds())) {
                child[i].intersecting(consumer, rect);
            }
        }
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        for (int i = 0; i < size; i++) {
            child[i].collectStats(stats, depth + 1);
        }
        stats.countBranchAtDepth(depth);
    }

    @Override
    public Node<T> instrument() {
        for (int i = 0; i < size; i++) {
            child[i] = child[i].instrument();
        }
        return new CounterNode<>(this);
    }
}
