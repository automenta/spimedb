package spimedb.index.rtree;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static spimedb.index.rtree.RTree2DTest.*;

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
            List<RectND> results = new ArrayList();

            rTree.intersecting(searchRect, results::add);
            int resultCount = 0;
            for(int i = 0; i < results.size(); i++) {
                if(results.get(i) != null) {
                    resultCount++;
                }
            }

            final int expectedCount = 9;
            //Assert.assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount, expectedCount, foundCount);

            Assert.assertEquals("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);

            // If the order of nodes in the tree changes, this test may fail while returning the correct results.
            for (int i = 0; i < resultCount; i++) {
                assertTrue("Unexpected result found", results.get(i).min.coord(0) == i + 2 && results.get(i).min.coord(1) == i + 2 && results.get(i).max.coord(0) == i + 5 && results.get(i).max.coord(1) == i + 5);
            }

            System.out.println("\t" + rTree.stats());
        }
    }

    @Test
    public void testSearchAllWithOneDimensionRandomlyInfinite() {
        System.out.println("\n\nINfinites");
        final int entryCount = 400;
        searchAll(2, 4, (dim)-> generateRandomRectsWithOneDimensionRandomlyInfinite(dim, entryCount));
    }

    /**
     * Use an enormous bounding box to ensure that every rectangle is returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    public void RectNDSearchAllTest() {
        System.out.println("\n\nfinites");
        final int entryCount = 400;
        searchAll(1, 6, (dim)->generateRandomRects(dim, entryCount));
    }

    static void searchAll(int minDim, int maxDim, IntFunction<RectND[]> generator) {
        for (int dim = minDim; dim <= maxDim; dim++) {

            final RectND[] rects = generator.apply(dim);
            Set<RectND> input = new HashSet();
            for (RectND r : rects)
                input.add(r);

            System.out.println("\tRectNDSearchAllTest[dim=" + dim + ']');

            for (RTree.Split type : RTree.Split.values()) {
                RTree<RectND> rTree = createRectNDTree(2, 8, type);
                for (int i = 0; i < rects.length; i++) {
                    rTree.add(rects[i]);
                }

                final RectND searchRect = new RectND(
                        PointND.fill(dim, Float.NEGATIVE_INFINITY),
                        PointND.fill(dim, Float.POSITIVE_INFINITY)
                );

                RectND[] results = new RectND[rects.length];

                final int foundCount = rTree.containing(searchRect, results);
                int resultCount = 0;
                for (int i = 0; i < results.length; i++) {
                    if (results[i] != null) {
                        resultCount++;
                    }
                }

                final int expectedCount = rects.length;
                Assert.assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount, expectedCount, foundCount);
                Assert.assertEquals("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);

                Set<RectND> output = new HashSet();
                for (RectND r : results)
                    output.add(r);


                assertEquals( " same content", input, output);


                Stats s = rTree.stats();
                System.out.println("\t" + s);
                //System.out.println("\t" + rTree.getRoot());
                assertTrue(s.getMaxDepth() < 8 /* reasonable */);
            }
        }
    }

}
