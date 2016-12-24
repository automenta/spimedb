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
package spimedb.cluster.ml.centroid;

import spimedb.cluster.ml.feature.Feature;

import java.io.Serializable;
import java.util.Collection;

/***
 * Interface that all Cluster Centroid objects must implement
 * 
 * @author slangevin
 *
 * @param <T>
 */
public interface Centroid<T extends Feature> extends Serializable {

	/***
	 * Add an Instance feature to this centroid. 
	 * 
	 * The centroid will aggregate these features and compute a centroid value that summarizes them 
	 * 
	 * @param feature feature to add to this centroid
	 */
    void add(T feature);

    /***
     * Remove an Instance feature from this centroid
     * 
     * @param feature
     */
    void remove(T feature);

	void setName(String name);

	String getName();

	Class<T> getType();

	void reset();
	
    /**
     * Get the centroid value represented
     */
    T getCentroid();

    /**
     * Get the centroid value represented, modified to be aggregatable with
     * other centroids or features. This will often simply be the centroid, but
     * may contain more information (see, for example, semantic and word
     * frequency centroids)
     */
    Collection<T> getAggregatableCentroid();
}
