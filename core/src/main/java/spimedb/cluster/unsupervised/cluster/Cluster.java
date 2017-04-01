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
package spimedb.cluster.unsupervised.cluster;

import com.fasterxml.jackson.annotation.JsonIgnore;
import spimedb.cluster.Instance;
import spimedb.cluster.centroid.Centroid;
import spimedb.cluster.feature.Feature;

import java.util.*;

/***
 * A class that represents a cluster.  
 * 
 * Each cluster has a set of Instances that belong to the cluster and centroids that 
 * summarize the Features of the Instance members.  The centroids are used when computing 
 * distances of Instances with clusters during clustering. 
 *   
 * @author slangevin
 *
 */
public class Cluster extends Instance {
	private static final long serialVersionUID = -1800048730067118377L;
	
	protected boolean onlineUpdate = false;  	// default to not updating centroid when new members are added
	@SuppressWarnings("rawtypes")
	protected final Map<String, Centroid> centroids = new HashMap<>();
	protected final Set<Instance> members = new LinkedHashSet<>();

	public Cluster() {
		super();
	}
	
	@SuppressWarnings("rawtypes")
	public Cluster(String id, Collection<FeatureTypeDefinition> types, boolean onlineUpdate) {
		super(id);

		//TODO determine size of the collections from 'types'.size()
		for (FeatureTypeDefinition def : types) {
			try {
				Centroid centroid = def.centroidClass.newInstance();
				centroid.setName(def.featureName);
				centroids.put(def.featureName, centroid);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}

		this.onlineUpdate = onlineUpdate;
	}
	
	@SuppressWarnings("rawtypes")
	public void reset() {
		this.members.clear();
		for (Map.Entry<String, Centroid> stringCentroidEntry : centroids.entrySet()) {
			try {
				Centroid centroid = stringCentroidEntry.getValue();
				centroid.reset();
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	public void updateCentroid() {
		for (Map.Entry<String, Centroid> stringCentroidEntry : centroids.entrySet()) {
			addFeature(stringCentroidEntry.getValue().getCentroid());
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void updateCentroids(Instance inst, boolean removefrom) {
		for (Map.Entry<String, Centroid> stringCentroidEntry : centroids.entrySet()) {
			Centroid m = stringCentroidEntry.getValue();
			Feature feature = inst.getFeature(stringCentroidEntry.getKey());
			if (feature != null) {
				if (removefrom) {
					m.remove(feature);
				}
				else {
					m.add(feature);
				}
				
				if (onlineUpdate) {
					addFeature(m.getCentroid());
				}
			}
		}
	}
	
	public boolean add(Instance inst) {	
		boolean isNew = members.add(inst);
		updateCentroids(inst, false);
		return isNew;
	}
	
	public boolean remove(Instance inst) {
		boolean isAltered = members.remove(inst);
		updateCentroids(inst, false);
		return isAltered;
	}
	
	public boolean replace(Instance oldValue, Instance newValue) {
		boolean altered = remove(oldValue);
		
		if (altered) {
			add(newValue);
		}
		return altered;
	}

	public boolean contains(Instance inst) {
		return members.contains(inst);
	}

	@Override
	@JsonIgnore
	public boolean isEmpty() {
		return members.isEmpty();
	}
	
	@SuppressWarnings("rawtypes")
	@JsonIgnore
	public Map<String, Centroid> getCentroids() {
		return centroids;
	}
	
	@SuppressWarnings("rawtypes")
	@JsonIgnore
	public void setCentroids(Map<String, Centroid> centroids) {
		this.centroids.putAll(centroids);
	}
	
	public Set<Instance> getMembers() {
		return members;
	}
	
	public void setMembers(Set<Instance> members) {
		this.members.addAll(members);
	}
	
	@SuppressWarnings("rawtypes")
	@JsonIgnore
	public Collection<FeatureTypeDefinition> getTypeDefs() {
		Collection<FeatureTypeDefinition> typedefs = new LinkedList<>();
		for (Centroid centroid : centroids.values()) {
			typedefs.add(new FeatureTypeDefinition(centroid.getName(), centroid.getClass(), null));
		}
		return typedefs;
	}

	public int size() {
		return members.size();
	}
	
	@Override
	public String toString() {
		return toString(false);
	}
	
	public String toString(boolean printMembers) {
		StringBuilder str = new StringBuilder();
		str.append(super.toString());
		
		if (printMembers) {
			str.append("\nMembers:\n");
			for (Instance inst : this.getMembers()) {
				str.append(inst.toString()).append('\n');
			}
		}
		return str.toString();
	}

	/**
	 * A method that can be used to spit out information on this cluster every iteration.
	 */
    public String getIterationDebugInfo () {
        return members.size()+" members";
    }
}
