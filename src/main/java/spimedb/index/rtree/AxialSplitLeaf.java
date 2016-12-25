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

import java.util.Arrays;
import java.util.Comparator;

/**
 * Fast RTree split suggested by Yufei Tao taoyf@cse.cuhk.edu.hk
 * <p>
 * Perform an axial split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public final class AxialSplitLeaf<T> extends Leaf<T> {

    AxialSplitLeaf(final RectBuilder<T> builder, final int mMin, final int mMax) {
        super(builder, mMin, mMax, RTree.Split.AXIAL);
    }

    @Override
    protected Node<T> split(final T t) {
        final Branch<T> pNode = new Branch<>(builder, mMin, mMax, splitType);
        final Node<T> l1Node = create(builder, mMin, mMax, splitType);
        final Node<T> l2Node = create(builder, mMin, mMax, splitType);
        final int nD = r[0].dim();

        final HyperRect[] sortedMbr = r.clone();

        // choose axis to split
        int axis = 0;
        double rangeD = mbr.getRangeFinite(0, 0);
        for (int d = 1; d < nD; d++) {
            // split along the greatest finite range extent
            final double dr = mbr.getRangeFinite(d, 0);
            if (dr > rangeD) {
                axis = d;
                rangeD = dr;
            }
        }

        if (rangeD == 0 || !Double.isFinite(rangeD))
            throw new UnsupportedOperationException("non-finite range can not be split in " + this);

        final int splitDimension = axis;

        Arrays.sort(sortedMbr, Comparator.comparingDouble(o -> o.center(splitDimension)));

        for (int i = 0; i < size / 2; i++) {
            outerLoop:
            for (int j = 0; j < size; j++) {
                if (r[j] == sortedMbr[i]) {
                    l1Node.add(entry[j]);
                    break outerLoop;
                }
            }
        }

        for (int i = size / 2; i < size; i++) {
            outerLoop:
            for (int j = 0; j < size; j++) {
                if (r[j] == sortedMbr[i]) {
                    l2Node.add(entry[j]);
                    break outerLoop;
                }
            }
        }

        classify(l1Node, l2Node, t);

        pNode.addChild(l1Node);
        pNode.addChild(l2Node);

        return pNode;
    }

}
