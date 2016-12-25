package spimedb.index.rtree;

import org.junit.Assert;
import org.junit.Test;


import static spimedb.index.rtree.RTree2DTest.createRectNDTree;
import static spimedb.index.rtree.RTree2DTest.generateRandomRects;

/**
 * Created by me on 12/21/16.
 */
public class RTreeNDTest {

    /**
     * Use an small bounding box to ensure that only expected rectangles are returned.
     * Verifies the count returned from search AND the number of rectangles results.
     * 2D but using N-d impl
     */
    @Test
    public void rectNDSearchTest2() {

        final int entryCount = 20;

        System.out.println("rectNDSearchTest2");

        for (RTree.Split type : RTree.Split.values()) {
            RTree<RectND> rTree = createRectNDTree(2, 8, type);
            for (int i = 0; i < entryCount; i++) {
                rTree.add(new RectND(new PointND(i, i), new PointND(i+3, i+3)));
            }

            final RectND searchRect = new RectND(new PointND(5, 5), new PointND(10, 10));
            RectND[] results = new RectND[entryCount];

            final int foundCount = rTree.containing(searchRect, results);
            int resultCount = 0;
            for(int i = 0; i < results.length; i++) {
                if(results[i] != null) {
                    resultCount++;
                }
            }

            final int expectedCount = 9;
            Assert.assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount, expectedCount, foundCount);
            Assert.assertEquals("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);

            // If the order of nodes in the tree changes, this test may fail while returning the correct results.
            for (int i = 0; i < resultCount; i++) {
                Assert.assertTrue("Unexpected result found", results[i].min.coord[0] == i + 2 && results[i].min.coord[1] == i + 2 && results[i].max.coord[0] == i + 5 && results[i].max.coord[1] == i + 5);
            }

            System.out.println("\t" + rTree.stats());
        }
    }


    /**
     * Use an enormous bounding box to ensure that every rectangle is returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    public void RectNDSearchAllTest() {

        final int entryCount = 1000;
        for (int dim = 2; dim <= 7; dim++) {

            final RectND[] rects = generateRandomRects(dim, entryCount);

            System.out.println("RectNDSearchAllTest[dim=" + dim + ']');

            for (RTree.Split type : RTree.Split.values()) {
                RTree<RectND> rTree = createRectNDTree(2, 8, type);
                for (int i = 0; i < rects.length; i++) {
                    rTree.add(rects[i]);
                }

                final RectND searchRect = new RectND(
                        PointND.fill(dim, Float.NEGATIVE_INFINITY),
                        PointND.fill(dim, Float.POSITIVE_INFINITY)
                );

                RectND[] results = new RectND[entryCount];

                final int foundCount = rTree.containing(searchRect, results);
                int resultCount = 0;
                for (int i = 0; i < results.length; i++) {
                    if (results[i] != null) {
                        resultCount++;
                    }
                }

                final int expectedCount = entryCount;
                Assert.assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount, expectedCount, foundCount);
                Assert.assertEquals("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);

                System.out.println("\t" + rTree.stats());

            }
        }
    }

}
