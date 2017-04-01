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
package spimedb.cluster.feature.semantic.distance;

import spimedb.cluster.distance.DistanceFunction;
import spimedb.cluster.feature.semantic.SemanticFeature;

/***
 * A distance function that computes the distance between two SemanticFeatures
 * 
 * In order to use this distance function you must provide a taxonomy that describes the Semantic Class hierarchy as a tree
 * 
 * The distance calculation finds the lowest common ancestor between the two SemanticFeatures and computes
 * the distance using the algorithm described in:
 * 
 * Wu, Z. and Palmer, M., Verbs semantics and lexical selection. In Proceedings of the 32nd annual meeting on Association for Computational Linguistics (Jun. 1994).
 * 
 * @author slangevin
 *
 */
public class WuPalmerDistance extends DistanceFunction<SemanticFeature> {
	private static final long serialVersionUID = 2357149443801960761L;
	private final Concept taxonomy;
	
	public WuPalmerDistance(Concept taxonomy) {
		this(taxonomy, 1);
	}
	
	public WuPalmerDistance(Concept taxonomy, double weight) {
		super(weight);
		this.taxonomy = taxonomy;
	}
	
	@Override
	public double distance(SemanticFeature x, SemanticFeature y) {
		double dist = 1;
//		double penalty = 0.2; //0.3; 	// penalty for not being the same uri
		
//		// For now there is no distance if the uri is the same entity or same concept
//		if (x.getUri().equalsIgnoreCase(y.getUri())) {
//			return 0;
//		}
		
		Concept cx = taxonomy.findConcept(x.getConcept());
		Concept cy = taxonomy.findConcept(y.getConcept());	
		Concept lca = cx.findCommonAncestor(cy);
		
		// No common ancestor exists - return max distance
		if (lca != null) {
			int n3 = lca.getDepth();
			int n1 = cx.getDepth() - n3;
			int n2 = cy.getDepth() - n3;			
			dist = 1.0 -  2.0 * n3 / (n1 + n2 + 2.0 * n3);
		}
		return dist;
//		return (dist + penalty) / (1 + penalty);
	}

}
