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
package spimedb.cluster.validation.unsupervised.external;

import spimedb.cluster.Instance;
import spimedb.cluster.unsupervised.cluster.Cluster;

import java.util.*;

/***
 * An external clustering validation implementation of Normalized Mutual Information
 * 
 * @author slangevin
 *
 */
public class NormMutualInformation {

	private static Map<String, Collection<Instance>> getEvents(Collection<Cluster> clusters) {
		Map<String, Collection<Instance>> events = new HashMap<>();
		
		for (Cluster c : clusters) {
			for (Instance inst : c.getMembers()) {
				if (!events.containsKey(inst.getClassLabel())) {
					events.put(inst.getClassLabel(), new ArrayList<>());
				}
				events.get(inst.getClassLabel()).add(inst);
			}
		}
		return events;
	}
	
	private static double getNormFactor(int numInstances, Collection<Instance> instances) {
		double val = ((double)instances.size() / numInstances);
		double norm = val * Math.log(val);
		return norm;
	}
	
	private static double getMI(int numInstances, Collection<Instance> event, Collection<Cluster> clusters) {
		double mi = 0;
		
		for (Cluster c : clusters) {
			// calc the intersection of the event with the cluster
			Set<Instance> intersect = new HashSet<>(c.getMembers());
			intersect.retainAll(event);
			
			if (intersect.isEmpty()) continue;
			
			// calc mutual information of event with cluster
			mi += ((double)intersect.size() / numInstances) * Math.log(numInstances * (double)intersect.size() / (event.size() * c.size()));
		}
		
		return mi;
	}
	
	public static double validate(Collection<Cluster> clusters) {
		int numInstances = 0;
		double norm = 0, factor1 = 0, factor2 = 0, mi = 0;
		
		// calculate the total number of instances
		Map<String, Collection<Instance>> events = getEvents(clusters);
		for (Collection<Instance> e : events.values()) {
			numInstances += e.size();
		}
		
		// calculate normalization factor
		for (Cluster c : clusters) {
			factor1 += getNormFactor(numInstances, c.getMembers());
		}
		for (Collection<Instance> e : events.values()) {
			factor2 += getNormFactor(numInstances, e);
		}
		norm = 0.5 * (-1 * factor1 - factor2);
		
		// calculate the mutual information for all events and clusters
		for (Map.Entry<String, Collection<Instance>> stringCollectionEntry : events.entrySet()) {
			mi += getMI(numInstances, stringCollectionEntry.getValue(), clusters);
		}
		// return normalized nmi
		return (mi / norm);
	}
}
