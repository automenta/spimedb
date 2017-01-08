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

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by jcovert on 6/16/15.
 */
public class Rect2DTest {

    @Test
    public void centroidTest() {

        Rect2D rect = new Rect2D(0, 0, 4, 3);

        HyperPoint centroid = rect.center();
        double x = centroid.coord(0);
        double y = centroid.coord(1);
        Assert.assertTrue("Bad X-coord of centroid - expected " + 2.0 + " but was " + x, x == 2.0d);
        Assert.assertTrue("Bad Y-coord of centroid - expected " + 1.5 + " but was " + y, y == 1.5d);
    }

    @Test
    public void mbrTest() {

        Rect2D rect = new Rect2D(0, 0, 4, 3);

        // shouldn't affect MBR
        Rect2D rectInside = new Rect2D(0, 0, 1, 1);
        HyperRect mbr = rect.mbr(rectInside);
        double expectedMinX = rect.min().coord(0);
        double expectedMinY = rect.min().coord(1);
        double expectedMaxX = rect.max().coord(0);
        double expectedMaxY = rect.max().coord(1);
        double actualMinX = mbr.min().coord(0);
        double actualMinY = mbr.min().coord(1);
        double actualMaxX = mbr.max().coord(0);
        double actualMaxY = mbr.max().coord(1);
        Assert.assertTrue("Bad minX - Expected: " + expectedMinX + " Actual: " + actualMinX, actualMinX == expectedMinX);
        Assert.assertTrue("Bad minY - Expected: " + expectedMinY + " Actual: " + actualMinY, actualMinY == expectedMinY);
        Assert.assertTrue("Bad maxX - Expected: " + expectedMaxX + " Actual: " + actualMaxX, actualMaxX == expectedMaxX);
        Assert.assertTrue("Bad maxY - Expected: " + expectedMaxY + " Actual: " + actualMaxY, actualMaxY == expectedMaxY);

        // should affect MBR
        Rect2D rectOverlap = new Rect2D(3, 1, 5, 4);
        mbr = rect.mbr(rectOverlap);
        expectedMinX = 0.0d;
        expectedMinY = 0.0d;
        expectedMaxX = 5.0d;
        expectedMaxY = 4.0d;
        actualMinX = mbr.min().coord(0);
        actualMinY = mbr.min().coord(1);
        actualMaxX = mbr.max().coord(0);
        actualMaxY = mbr.max().coord(1);
        Assert.assertTrue("Bad minX - Expected: " + expectedMinX + " Actual: " + actualMinX, actualMinX == expectedMinX);
        Assert.assertTrue("Bad minY - Expected: " + expectedMinY + " Actual: " + actualMinY, actualMinY == expectedMinY);
        Assert.assertTrue("Bad maxX - Expected: " + expectedMaxX + " Actual: " + actualMaxX, actualMaxX == expectedMaxX);
        Assert.assertTrue("Bad maxY - Expected: " + expectedMaxY + " Actual: " + actualMaxY, actualMaxY == expectedMaxY);
    }

    @Test
    public void rangeTest() {

        Rect2D rect = new Rect2D(0, 0, 4, 3);

        double xRange = rect.getRange(0);
        double yRange = rect.getRange(1);
        Assert.assertTrue("Bad range in dimension X - expected " + 4.0 + " but was " + xRange, xRange == 4.0d);
        Assert.assertTrue("Bad range in dimension Y - expected " + 3.0 + " but was " + yRange, yRange == 3.0d);
    }


    @Test
    public void containsTest() {

        Rect2D rect = new Rect2D(0, 0, 4, 3);

        // shares an edge on the outside, not contained
        Rect2D rectOutsideNotContained = new Rect2D(4, 2, 5, 3);
        Assert.assertTrue("Shares an edge but should not be 'contained'", !rect.contains(rectOutsideNotContained));

        // shares an edge on the inside, not contained
        Rect2D rectInsideNotContained = new Rect2D(0, 1, 4, 5);
        Assert.assertTrue("Shares an edge but should not be 'contained'", !rect.contains(rectInsideNotContained));

        // shares an edge on the inside, contained
        Rect2D rectInsideContained = new Rect2D(0, 1, 1, 2);
        Assert.assertTrue("Shares an edge and should be 'contained'", rect.contains(rectInsideContained));

        // intersects
        Rect2D rectIntersects = new Rect2D(3, 2, 5, 4);
        Assert.assertTrue("Intersects but should not be 'contained'", !rect.contains(rectIntersects));

        // contains
        Rect2D rectContained = new Rect2D(1, 1, 2, 2);
        Assert.assertTrue("Contains and should be 'contained'", rect.contains(rectContained));

        // does not contain or intersect
        Rect2D rectNotContained = new Rect2D(5, 0, 6, 1);
        Assert.assertTrue("Does not contain and should not be 'contained'", !rect.contains(rectNotContained));
    }

    @Test
    public void intersectsTest() {

        Rect2D rect = new Rect2D(0, 0, 4, 3);

        // shares an edge on the outside, intersects
        Rect2D rectOutsideIntersects = new Rect2D(4, 2, 5, 3);
        Assert.assertTrue("Shares an edge and should 'intersect'", rect.intersects(rectOutsideIntersects));

        // shares an edge on the inside, intersects
        Rect2D rectInsideIntersects = new Rect2D(0, 1, 4, 5);
        Assert.assertTrue("Shares an edge and should 'intersect'", rect.intersects(rectInsideIntersects));

        // shares an edge on the inside, intersects
        Rect2D rectInsideIntersectsContained = new Rect2D(0, 1, 1, 2);
        Assert.assertTrue("Shares an edge and should 'intersect'", rect.intersects(rectInsideIntersectsContained));

        // intersects
        Rect2D rectIntersects = new Rect2D(3, 2, 5, 4);
        Assert.assertTrue("Intersects and should 'intersect'", rect.intersects(rectIntersects));

        // contains
        Rect2D rectContained = new Rect2D(1, 1, 2, 2);
        Assert.assertTrue("Contains and should 'intersect'", rect.intersects(rectContained));

        // does not contain or intersect
        Rect2D rectNotIntersects = new Rect2D(5, 0, 6, 1);
        Assert.assertTrue("Does not intersect and should not 'intersect'", !rect.intersects(rectNotIntersects));
    }

    @Test
    public void costTest() {

        Rect2D rect = new Rect2D(0, 0, 4, 3);
        double cost = rect.cost();
        Assert.assertTrue("Bad cost - expected " + 12.0 + " but was " + cost, cost == 12.0d);
    }
}
