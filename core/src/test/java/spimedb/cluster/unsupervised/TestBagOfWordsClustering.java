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
import spimedb.cluster.feature.bagofwords.BagOfWordsFeature;
import spimedb.cluster.feature.bagofwords.centroid.BagOfWordsCentroid;
import spimedb.cluster.feature.bagofwords.distance.EditDistance;
import spimedb.cluster.feature.spatial.GeoSpatialFeature;
import spimedb.cluster.feature.spatial.centroid.FastGeoSpatialCentroid;
import spimedb.cluster.feature.spatial.distance.HaversineDistance;
import spimedb.cluster.unsupervised.cluster.Cluster;
import spimedb.cluster.unsupervised.cluster.ClusterResult;
import spimedb.cluster.unsupervised.cluster.threshold.ThresholdClusterer;

public class TestBagOfWordsClustering {

	private static final String FEATURE_NAME1 = "tokens";
	private static final String SOFT_FEATURE_NAME = "opt1";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DataSet ds = new DataSet();
		
		Instance inst = new Instance("1");
		BagOfWordsFeature feature = new BagOfWordsFeature(FEATURE_NAME1);
		feature.setCount("jack", 1);
		feature.setCount("black", 1);
		inst.addFeature(feature);
		
		GeoSpatialFeature soft = new GeoSpatialFeature(SOFT_FEATURE_NAME);
		soft.setValue(39.76, -98.5);
		inst.addFeature(soft);
		ds.add(inst);
		
		inst = new Instance("2");
		feature = new BagOfWordsFeature(FEATURE_NAME1);
		feature.setCount("jack", 1);
		feature.setCount("black", 1);
		inst.addFeature(feature);
		ds.add(inst);
		
		inst = new Instance("3");
		feature = new BagOfWordsFeature(FEATURE_NAME1);
		feature.setCount("jack", 1);
		inst.addFeature(feature);
		
		soft = new GeoSpatialFeature(SOFT_FEATURE_NAME);
		soft.setValue(39.76, -98.5);
		inst.addFeature(soft);
		ds.add(inst);
		
		inst = new Instance("4");
		feature = new BagOfWordsFeature(FEATURE_NAME1);
		feature.setCount("jack", 1);
		feature.setCount("l.", 1);
		feature.setCount("black", 1);
		inst.addFeature(feature);
		
		soft = new GeoSpatialFeature(SOFT_FEATURE_NAME);
		soft.setValue(39.76, -98.5);
		inst.addFeature(soft);
		ds.add(inst);
		
		inst = new Instance("5");
		feature = new BagOfWordsFeature(FEATURE_NAME1);
		feature.setCount("j.", 1);
		feature.setCount("black", 1);
		inst.addFeature(feature);
		
		soft = new GeoSpatialFeature(SOFT_FEATURE_NAME);
		soft.setValue(39.76, -98.5);
		inst.addFeature(soft);
		ds.add(inst);
		
		inst = new Instance("6");
		feature = new BagOfWordsFeature(FEATURE_NAME1);
		feature.setCount("j", 1);
		feature.setCount("black", 1);
		inst.addFeature(feature);
		
		soft = new GeoSpatialFeature(SOFT_FEATURE_NAME);
		soft.setValue(39.76, -98.5);
		inst.addFeature(soft);
		ds.add(inst);
		
		inst = new Instance("7");
		feature = new BagOfWordsFeature(FEATURE_NAME1);
		feature.setCount("black", 1);
		inst.addFeature(feature);
		
		soft = new GeoSpatialFeature(SOFT_FEATURE_NAME);
		soft.setValue(39.76, -98.5);
		inst.addFeature(soft);
		ds.add(inst);
		
		inst = new Instance("8");
		feature = new BagOfWordsFeature(FEATURE_NAME1);
		feature.setCount("jackie", 1);
		feature.setCount("black", 1);
		inst.addFeature(feature);
		
		soft = new GeoSpatialFeature(SOFT_FEATURE_NAME);
		soft.setValue(39.76, -98.5);
		inst.addFeature(soft);
		ds.add(inst);
		
		inst = new Instance("9");
		feature = new BagOfWordsFeature(FEATURE_NAME1);
		feature.setCount("jack", 1);
		feature.setCount("brown", 1);
		inst.addFeature(feature);
		
		soft = new GeoSpatialFeature(SOFT_FEATURE_NAME);
		soft.setValue(10.0, 8.0);
		inst.addFeature(soft);
		ds.add(inst);
		
		inst = new Instance("10");
		feature = new BagOfWordsFeature(FEATURE_NAME1);
		feature.setCount("jackie", 1);
		feature.setCount("green", 1);
		inst.addFeature(feature);
		
		soft = new GeoSpatialFeature(SOFT_FEATURE_NAME);
		soft.setValue(39.76, -98.5);
		inst.addFeature(soft);
		ds.add(inst);
				
		ThresholdClusterer clusterer = new ThresholdClusterer();
		clusterer.registerFeatureType(
				FEATURE_NAME1,
				BagOfWordsCentroid.class, 
				new EditDistance(1.0));
		
		clusterer.registerFeatureType(
				SOFT_FEATURE_NAME,
				FastGeoSpatialCentroid.class,
				new HaversineDistance(1.0));
		
		clusterer.setThreshold(0.5);
		
		ClusterResult clusters = clusterer.doCluster(ds);
		for (Cluster c : clusters) {
			System.out.println(c);
		}
	}
}
