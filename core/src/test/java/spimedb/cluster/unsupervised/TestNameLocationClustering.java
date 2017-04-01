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
package spimedb.cluster.unsupervised;

import spimedb.cluster.DataSet;
import spimedb.cluster.Instance;
import spimedb.cluster.feature.spatial.GeoSpatialFeature;
import spimedb.cluster.feature.spatial.centroid.GeoSpatialCentroid;
import spimedb.cluster.feature.spatial.distance.HaversineDistance;
import spimedb.cluster.feature.string.StringFeature;
import spimedb.cluster.feature.string.centroid.StringMedianCentroid;
import spimedb.cluster.feature.string.distance.EditDistance;
import spimedb.cluster.unsupervised.cluster.Cluster;
import spimedb.cluster.unsupervised.cluster.ClusterResult;
import spimedb.cluster.unsupervised.cluster.kmeans.KMeans;

import java.util.Random;

public class TestNameLocationClustering {
	
	public static void main(String[] args) {
		DataSet ds = new DataSet();
		
		String[] tokens = {"alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "romeo", "sierra", "tango", "whiskey"};
		
		Random rnd = new Random();
		for (int i=0; i < 100000; i++) {
			// create a new data instance
			Instance inst = new Instance();
			
			// add name feature to the instance
			StringFeature name = new StringFeature("name");
			name.setValue( tokens[rnd.nextInt(tokens.length)] + " " + tokens[rnd.nextInt(tokens.length)]);
			inst.addFeature(name);
			
			// add geo spatial feature to the instance
			GeoSpatialFeature geo = new GeoSpatialFeature("location");
			geo.setLatitude(rnd.nextDouble() * 180 - 90);
			geo.setLongitude(rnd.nextDouble() * 360 - 180);
			inst.addFeature(geo);
		
			// add the instance to the dataset
			ds.add(inst);
		}
		
		// create a k-means clusterer with k=4, 5 max iterations
		KMeans clusterer = new KMeans(4, 5, false);
		
		// register the name features distance function and centroid method using a weight of 1.0
		clusterer.registerFeatureType(
				"name",
				StringMedianCentroid.class, 
				new EditDistance(1.0));
		
		// register the location features distance function and centroid method using a weight of 1.0
		clusterer.registerFeatureType(
				"location", 
				GeoSpatialCentroid.class,
				new HaversineDistance(1.0));
		
		ClusterResult clusters = clusterer.doCluster(ds);
		for (Cluster c : clusters) {
			System.out.println(c.toString(false));
		}
		clusterer.terminate();
	}
}
