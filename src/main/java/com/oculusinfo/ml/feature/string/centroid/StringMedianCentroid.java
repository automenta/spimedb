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
package com.oculusinfo.ml.feature.string.centroid;

import com.oculusinfo.ml.centroid.Centroid;
import com.oculusinfo.ml.feature.bagofwords.distance.EditDistance;
import com.oculusinfo.ml.feature.string.StringFeature;

import java.util.*;

/***
 * A Centroid for StringFeatures that represents the centroid using an approximation of the string median algorithm
 * 
 * The algorithm has two main steps: 
 * 1) a subset of nr points (reference points) from the set P is randomly selected. 
 * The sum of distances from each point in P to the reference points are calculated and stored.
 * 2) The nt points of P whose sum of distances is lowest are selected (test points). 
 * For each test point, the sum of distances to every point belonging to P is calculated and stored. 
 * The test point that minimizes this sum is selected as the median of the set.
 * 
 * @author slangevin
 *
 */
public class StringMedianCentroid implements Centroid<StringFeature> {
	private static final long serialVersionUID = -251730602782817386L;
	private static final int NUM_REFERENCE = 10;
	private static final int NUM_TEST = 10;
	
	private String name;
	private String[] referencePoints = new String[NUM_REFERENCE];
	private String[] testPoints = new String[NUM_REFERENCE];
	
	private final Map<String, StringFeature> points = new HashMap<String, StringFeature>();
	
	
	private static class DistanceScore {
		public final String feature;
		public final double distance;
		
		public DistanceScore(String feature, double distance) {
			this.feature = feature;
			this.distance = distance;
		}
	}
	
	@Override
	public void add(StringFeature feature) {
		points.put(feature.getId(), feature);
	}
	
	@Override
	public void remove(StringFeature feature) {
		points.remove(feature.getId());
	}

	@Override
	public Collection<StringFeature> getAggregatableCentroid () {
        return points.values();
    }
	
	private void computeTestPoints() {
		int nr = Math.min(NUM_REFERENCE, points.size());
		
		// compute the distances of all points to the reference points
		List<DistanceScore> distances = new LinkedList<DistanceScore>();
		for (Map.Entry<String, StringFeature> stringStringFeatureEntry : points.entrySet()) {
			double sum = 0;
			StringFeature f = stringStringFeatureEntry.getValue();
			for (int i=0; i < nr; i++) {
				sum += EditDistance.getNormLevenshteinDistance(referencePoints[i], f.getValue());
			}
			distances.add(new DistanceScore(f.getValue(), sum));
		}
		
		// sort by distance
		Collections.sort(distances, dsc);
		
		// set the test points
		int nt = Math.min(NUM_TEST, points.size());
		for (int i=0; i < nt; i++) {
			testPoints[i] = distances.get(i).feature;
		}
	}
	
	//TODO this should take into account the count of the strings
	private String computeMedian() {
		int nt = Math.min(NUM_TEST, points.size());
		
		if (nt == 0) return "";
		
		// compute the distances of test points all points 
		List<DistanceScore> distances = new LinkedList<DistanceScore>();
		for (int i=0; i < nt; i++) {
			double sum = 0;
			for (Map.Entry<String, StringFeature> stringStringFeatureEntry : points.entrySet()) {
				StringFeature f = stringStringFeatureEntry.getValue();
				sum += EditDistance.getNormLevenshteinDistance(testPoints[i], f.getName());
			}
			distances.add(new DistanceScore(testPoints[i], sum));
		}
		
		// sort by distance
		Collections.sort(distances, dsc);
		
		// return the smallest distance point as median	
		return distances.get(0).feature;
	}
	
	private void setReferencePoints() {
		int n = Math.min(NUM_REFERENCE, points.size());
		
		ArrayList<String> keys = new ArrayList<String>(points.keySet());
		Collections.shuffle(keys);
		
		for (int i=0; i < n; i++) {
			referencePoints[i] = points.get(keys.get(i)).getValue();
		}
	}

	@Override
	public StringFeature getCentroid() {
		StringFeature median = new StringFeature(name);
		
		setReferencePoints();
		computeTestPoints();
	
		median.setValue( computeMedian() );
		
		return median;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Class<StringFeature> getType() {
		return StringFeature.class;
	}

	@Override
	public void reset() {
		referencePoints = new String[NUM_REFERENCE];
		testPoints = new String[NUM_REFERENCE];
		points.clear();
	}

	public static final class DistanceScoreComparator implements Comparator<DistanceScore> {
		@Override
		public int compare(DistanceScore o1, DistanceScore o2) {
            return Double.compare(o1.distance, o2.distance);
        }
	}
	final static DistanceScoreComparator dsc = new DistanceScoreComparator();

}
