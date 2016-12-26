package spimedb.index.rtree;

import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 12/2/16.
 */
public class Rect1DTest {
    @Test
    public void centroidTest() {

        Rect1D rect = new Rect1D.DefaultRect1D(0, 4);

        HyperPoint centroid = rect.center();
        double x = centroid.coord(0);
        Assert.assertTrue("Bad X-coord of centroid - expected " + 2.0 + " but was " + x, x == 2.0d);

    }

    /**
     * Use an small bounding box to ensure that only expected rectangles are returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    public void rect2DSearchTest() {

        final int entryCount = 20;

        //for (RTree.Split type : RTree.Split.values()) {
            RTree<Double> t = new RTree<>((x) -> new Rect1D.DefaultRect1D(x, x), 2, 3, RTree.Split.LINEAR);
            for (int i = 0; i < entryCount; i++) {
                t.add((double)(i*i));
            }

            //t.forEach(x -> System.out.println(x));

            Stats s = t.stats();
            System.out.println(s);

            DoubleArrayList d = new DoubleArrayList();
            t.intersecting(new Rect1D.DefaultRect1D(1, 101), d::add);

            assertEquals(10, d.size());

            System.out.println(d);

//            final Rect2D searchRect = new Rect2D(5, 5, 10, 10);
//            Rect2D[] results = new Rect2D[entryCount];
//
//            final int foundCount = rTree.search(searchRect, results);
//            int resultCount = 0;
//            for(int i = 0; i < results.length; i++) {
//                if(results[i] != null) {
//                    resultCount++;
//                }
//            }
//
//            final int expectedCount = 9;
//            Assert.assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount, expectedCount, foundCount);
//            Assert.assertEquals("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);
//
//            // If the order of nodes in the tree changes, this test may fail while returning the correct results.
//            for (int i = 0; i < resultCount; i++) {
//                Assert.assertTrue("Unexpected result found", results[i].min.x == i + 2 && results[i].min.y == i + 2 && results[i].max.x == i + 5 && results[i].max.y == i + 5);
//            }

    }
}
