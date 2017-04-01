/**
 * Copyright (c) 2013 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package spimedb.util.math.algebra;

import junit.framework.Assert;
import org.junit.Test;

public class TestAngleUtilities {
    private static final double EPSILON = 1E-12;

    @Test
    public void testAngleRanges () {
        Assert.assertEquals(   1.0, AngleUtilities.intoRangeDegrees(0.0,  361.0), EPSILON);
        Assert.assertEquals(-180.0, AngleUtilities.intoRangeDegrees(0.0,  180.0), EPSILON);
        Assert.assertEquals(-180.0, AngleUtilities.intoRangeDegrees(0.0, -180.0), EPSILON);
        Assert.assertEquals(  -1.0, AngleUtilities.intoRangeDegrees(0.0,  359.0), EPSILON);
    }
}
