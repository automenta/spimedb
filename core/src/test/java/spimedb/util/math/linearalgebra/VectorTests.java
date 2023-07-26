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
package spimedb.util.math.linearalgebra;

import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class VectorTests {
    @Test
    public void testVectorEquality () {
        assertNotEquals(new Vector(Double.NaN), new Vector(0));
        assertNotEquals(new Vector(0), new Vector(Double.NaN));
        assertEquals(new Vector(0), new Vector(0));
        assertEquals(new Vector(Double.NaN), new Vector(Double.NaN));
    }

    @Test
    public void testCrossProduct () {
        Vector X = new Vector(1, 0, 0);
        Vector Y = new Vector(0, 1, 0);
        Vector Z = new Vector(0, 0, 1);
        Assert.assertEquals(Z, X.cross(Y));
        Assert.assertEquals(Y, Z.cross(X));
        Assert.assertEquals(X,  Y.cross(Z));
        Assert.assertEquals(Z.scale(-1.0), Y.cross(X));
        Assert.assertEquals(Y.scale(-1.0), X.cross(Z));
        Assert.assertEquals(X.scale(-1.0),  Z.cross(Y));
    }
}
