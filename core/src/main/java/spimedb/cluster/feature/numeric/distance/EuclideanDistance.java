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
package spimedb.cluster.feature.numeric.distance;

import spimedb.cluster.distance.DistanceFunction;
import spimedb.cluster.feature.numeric.NumericVectorFeature;

/***
 * A distance function that computes the Euclidean distance between two VectorFeatures
 * 
 * @author slangevin
 *
 */
public class EuclideanDistance extends DistanceFunction<NumericVectorFeature> {
	private static final long serialVersionUID = -1493313434323633636L;

	public EuclideanDistance(double weight) {
		super(weight);
	}
	
	@Override
	public double distance(NumericVectorFeature x, NumericVectorFeature y) {
		double[] vector1 = x.getValue();
		double[] vector2 = y.getValue();
		
		double d = 0;
		for (int i = 0; i < vector1.length; i++) {
			d += Math.pow(vector1[i] - vector2[i], 2);
		}
		
		// return euclidean distance
		return Math.sqrt( d / (double)vector1.length );
	}
}
