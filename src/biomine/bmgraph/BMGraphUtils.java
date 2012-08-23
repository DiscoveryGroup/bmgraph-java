/*
 * Copyright 2012 University of Helsinki.
 * 
 * This file is part of bmgraph-java.
 *
 * bmgraph-java is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * bmgraph-java is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with bmgraph-java.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package biomine.bmgraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;

import biomine.bmgraph.read.BMGraphReader;
import biomine.bmgraph.write.BMGraphWriter;


/**
 * BMGraphUtils. Helpers for working with BMGraph graphs.
 */

public class BMGraphUtils {
    
    /**
     * Read a file specified by name into a new BMGraph.
     * <p>Note: No exception handling is done in this helper.
     * @param filename The name/path of the BMGraph file.
     * @return A new BMGraph, or null in case of failure.
     */
    public static BMGraph readBMGraph (String filename)
        throws FileNotFoundException {
        assert filename != null : "Null filename";

        BMGraphReader reader = new BMGraphReader();
        return reader.parseFile(filename) ? reader.getGraph() : null;
    }    
    
    /**
     * Read a file specified by a File object into a new BMGraph.
     * @param File The File object of the BMGraph file.
     * @return A new BMGraph, or null in case of failure.
     */
    public static BMGraph readBMGraph (File file) {
        assert file != null : "Null file";

        BMGraphReader reader = new BMGraphReader();
        return reader.parseFile(file) ? reader.getGraph() : null;
    }
    
    /**
     * Read a new BMGraph from an InputStream.
     * @param stream The InputStream from which the graph is read.
     * @return A new BMGraph, or null in case of failure.
     */
    public static BMGraph readBMGraph (InputStream stream) {
        assert stream != null : "Null stream";
                        
        BMGraphReader reader = new BMGraphReader();
        return reader.parseStream(stream, null) ? reader.getGraph() : null;
    }
    
    /**
     * Find an edge between two nodes in a BMGraph.
     * 
     * <p>The search is undirected in the sense that if an edge exists
     * between the parameter nodes it is found regardless of its
     * direction or the order of the parameters.
     *
     * <p>In case there are multiple edges between the given nodes, any
     * one of these edges may be returned (it is NOT guaranteed to be any 
     * particular edge; this may or may not be completely undeterministic,
     * and may change without any notification).
     *
     * @param graph The BMGraph in which to find the edge.
     * @param node1 The first BMNode.
     * @param node2 The second BMNode.
     * @return A BMEdge representing an edge found between the two nodes
     * in the given graph, or null if none found. UNDETERMINTISTIC!
     */
    public static BMEdge findEdge (BMGraph graph, BMNode node1, BMNode node2) {
        assert graph != null : "Null graph";
        assert node1 != null : "Null node 1";
        assert node2 != null : "Null node 2";

        // Find the node with fewer edges and search those for a match

        List<BMEdge> edges = graph.getNodeEdges(node1);
        List<BMEdge> edges2 = graph.getNodeEdges(node2);

        if (edges.size() > edges2.size()) {
            // Use the list with fewer edges
            BMNode tmp = node1;
            node1 = node2;
            node2 = tmp;
            edges = edges2;
        }
        for (BMEdge edge : edges) {
            if (edge.otherNode(node1) == node2)
                return edge;
        }
        return null;
    }
    
    /**
     * Find all edges between two nodes in a BMGraph.
     * 
     * <p>The search is undirected in the sense that if an edge exists
     * between the parameter nodes it is found regardless of its
     * direction or the order of the parameters.
     *
     * <p>In case there are multiple edges between the given nodes, all
     * of these edges are returned.
     *
     * @param graph The BMGraph in which to find the edge.
     * @param node1 The first BMNode.
     * @param node2 The second BMNode.
     * @return A list of BMEdges containing all edges found between
     * the two nodes in the given graph, or null if no edge were found.
     */
    public static List<BMEdge> findEdges (BMGraph graph, BMNode node1, 
	    BMNode node2) {
        assert graph != null : "Null graph";
        assert node1 != null : "Null node 1";
        assert node2 != null : "Null node 2";

        // Find the node with fewer edges and search those for a match

        List<BMEdge> edges = graph.getNodeEdges(node1);
        List<BMEdge> edges2 = graph.getNodeEdges(node2);
       
        if (edges.size() > edges2.size()) {
            // Use the list with fewer edges
            BMNode tmp = node1;
            node1 = node2;
            node2 = tmp;
            edges = edges2;
        }
        
        List<BMEdge> edgelist = new ArrayList<BMEdge>();
        for (BMEdge edge : edges) {
            if (edge.otherNode(node1) == node2) {
                edgelist.add(edge);
            }
        }
        return edgelist.isEmpty() ? null : edgelist;
    }
    
    /**
     * Writes the given BMGraph to a file specified by name.
     * @param graph The BMGraph to write.
     * @param file The filename/path of the file to write the BMGraph into.
     */
    public static void writeBMGraph (BMGraph graph, String file) throws IOException {
        assert graph != null : "Null graph";
        assert file != null : "Null file";
        assert file.length() != 0 : "Empty filename";

        FileOutputStream fos = new FileOutputStream(file);
        BMGraphWriter bmGraphWriter = new BMGraphWriter(graph, fos);
        bmGraphWriter.writeSorted(true);
        fos.close();
    }
    
    /**
     * Writes the given BMGraph to a file specified by File object.
     * @param graph The BMGraph to write.
     * @param file The File object representing the file in which to write the
     * BMGraph.
     */
    public static void writeBMGraph (BMGraph graph, File file) throws IOException {
        assert graph != null : "Null graph";
        assert file != null : "Null file";

        FileOutputStream fos = new FileOutputStream(file);
        BMGraphWriter bmGraphWriter = new BMGraphWriter(graph, fos);
        bmGraphWriter.writeSorted(true);
        fos.close();
    }
    
    /**
     * Writes the given BMGraph to an OutputStream, without closing it.
     * @param graph The BMGraph to write.
     * @param stream The OutputStream in which to write the graph (the
     * stream is NOT closed after writing).
     */
    public static void writeBMGraph (BMGraph graph, OutputStream stream) throws IOException {        
        assert graph != null : "Null graph";
        assert stream != null : "Null stream";

        BMGraphWriter bmGraphWriter = new BMGraphWriter(graph, stream);
        bmGraphWriter.writeSorted(true);        
    }

    /**
     * Create a new, empty BMGraph using an existing BMGraph as
     * a template for settings and metadata.
     * @param graph The existing BMGraph to copy graph metadata from.
     * @return A new BMGraph with no nodes or edges.
     */
    public static BMGraph graphFromTemplate (BMGraph graph) {
        assert graph != null : "Null graph";

        BMGraph g = new BMGraph();

        // DEBUG: Should regular (non-special) comments be copied?

        g.createAttributeMaps = graph.createAttributeMaps;
        g.database = graph.database.clone();

        // Copy linktype definitions from template

        g.canonicalDirections = new HashSet<String>(graph.canonicalDirections.size());
        g.canonicalDirections.addAll(graph.canonicalDirections);
        g.reverseLinktype = new HashMap<String, String>(graph.reverseLinktype.size(), 0.9f);
        g.reverseLinktype.putAll(graph.reverseLinktype);

        // Copy special comments (complicated, since they can be either
        // Sets or Maps, and we don't want to recycle the same instances
        // in case the original graph is modified).

        g.copySpecialCommentsFrom(graph);
        return g;
    }

    /**
     * Finds "inbound" and "outbound" goodness for each vertex in graph.  
     * For a vertex v in graph, the outbound goodness of v is the sum of 
     * goodness values of all edges adjacent to v in supergraph but 
     * not in graph.  The inbound goodness of v is the sum of goodness values 
     * of all edges adjacent to v in graph.  Any numeric attribute can be used
     * (see parameter goodnessAttribute).
     * <p>
     * Calculated outbound goodness values are added to "output_goodness" 
     * vertex attributes.  Inbound goodness values are added to 
     * "inbound_goodness" vertex attributes.  Here _goodness will be replaced
     * by goodnessAttribute; for example, if goodnessAttribute = "reliability",
     * added attributes will be "inbound_reliability" and 
     * "outbound_reliability".
     * <p>
     * Any supergraph edges that have both endpoints in graph are silently
     * ignored. 
     *
     * @param graph graph for which adjancent goodness is calculated
     * @param supergraph supergraph of graph
     * @param goodnessAttribute use the value of this edge attribute as goodness 
     */
    public static void calculateAdjacentGoodness(BMGraph graph,
	    BMGraph supergraph, String goodnessAttribute) {
	for (BMNode n : graph.getNodes()) {
	    double inpr = 0.0;
	    double outpr = 0.0;
	    for (BMEdge e : supergraph.getNodeEdges(n)) {
		double goodness = Double.parseDouble(e.get(goodnessAttribute));	
		if (graph.hasEdge(e)) {
		    inpr += goodness;
		    continue;
		}
		if (graph.hasNode(e.getSource()) 
			&& graph.hasNode(e.getTarget())) {
		    continue;
		}
		outpr += goodness;
	    }
	    n.put("outbound_ " + goodnessAttribute, String.valueOf(outpr));
	    n.put("inbound_" + goodnessAttribute, String.valueOf(inpr));
	}
    }
    
    /**
     * Convert BMGraph to a random walk matrix.
     *  
     * Random walk can be restarting or non-restarting.  If restartProbability 
     * is greater than zero, the walk is restarted to startNode with that 
     * probability at each node.  If restartProbabilityAttribute is non-null, 
     * each node u that has such attribute with value pr is assumed to have
     * edge (u, startNode) with probability pr.
     * <p> 
     * Node-to-index and index-to-node mappings are recorded to the given
     * map references.  Note that both maps will be cleared.
     * 
     * @param graph BMGraph where the random walk is done.
     * @param startNode Starting node for the walk.
     * @param restartProbability Global restart probability (for random walk 
     * with restart).
     * @param transitionProbabilityAttribute Node attribute name for transition
     * probability.
     * @param restartProbabilityAttribute Node attribute name for node-specific 
     * restart probability.
     * @param nodeToIndexMap Mapping from node set to state set.
     * @param indexToNodeMap Mapping from state set to node set.
     * @return Stochastic adjacency matrix, where M[i][j] is the transition 
     * probability from state i to j.
     */
    public static double[][] convertToRWMatrix(
	    BMGraph graph, 
	    BMNode startNode, 
	    double restartProbability,
	    String transitionProbabilityAttribute,
	    String restartProbabilityAttribute, 
	    Map<BMNode, Integer> nodeToIndexMap,
	    Map<Integer, BMNode> indexToNodeMap) {
	int N = graph.numNodes();
	double[][] M = new double[N][N];
	
	// create indexes
	nodeToIndexMap.clear();
	indexToNodeMap.clear();
	int i = 0;
	for (BMNode n : graph.getNodes()) {
	    nodeToIndexMap.put(n, i);
	    indexToNodeMap.put(i, n);
	    i++;
	}
	
	// create adjacency matrix, one row at a time
	for (i = 0; i < N; i++) {
	    BMNode u = indexToNodeMap.get(i);
	    
	    // calculate total (normalization) probability
	    // first go through neighbors
	    // add also neighbors' indices to a priority queue for later use
	    double totalPr = 0.0;
	    List<BMNode> neighbors = graph.getNeighbors(u);
	    PriorityQueue<Integer> neighborIds = new PriorityQueue<Integer>();
	    for (BMNode v : neighbors) {
		int k = nodeToIndexMap.get(v);
		neighborIds.add(k);
		List<BMEdge> edges = BMGraphUtils.findEdges(graph, u, v);
		for (BMEdge e : edges) {
		    totalPr += Double.parseDouble
		    	(e.get(transitionProbabilityAttribute));
		}
	    }
	    // check for restart probability attribute
	    double nodeRestartPr = 0.0;
	    if (restartProbabilityAttribute != null) {
		String nodeRestartValue = u.get(restartProbabilityAttribute);	    
	    	if (nodeRestartValue != null) {
	    	    nodeRestartPr = Double.parseDouble(nodeRestartValue);
	    	}
	    }
	    totalPr += nodeRestartPr;
 
	    // process neighbor nodes v ∈ N(u)
	    double edgePr;
	    boolean restartDone = false;
	    for (BMNode v : neighbors) {
		int k = nodeToIndexMap.get(v);
		
		// fill M[i][k]		
		edgePr = 0.0;
                List<BMEdge> edges = BMGraphUtils.findEdges
                    (graph, indexToNodeMap.get(i), 
                     indexToNodeMap.get(k));
                for (BMEdge e : edges) {
		    edgePr += Double.parseDouble
		    	(e.get(transitionProbabilityAttribute));
		}
		
                double transitionPr;
		if (v.equals(startNode)) {		    
		    edgePr += nodeRestartPr;
		    transitionPr = restartProbability 
		    	+ (1 - restartProbability) * edgePr / totalPr;
		    restartDone = true;	    
                } else {
                    transitionPr = (1 - restartProbability) * edgePr / totalPr;
                }
                
		M[i][k] = transitionPr;
	    }
            
	    int j = 0;
	    while (j < N) {
		// fill M[i][j..k-1] with zeros, where k is the index
		// of next neighboring node
		int k = neighborIds.isEmpty() ? N : neighborIds.poll();
		while (j < k) {
		    M[i][j] = 0.0;
		    j++;
		}                
		j++;
	    }

            // fill restart transition probability, if needed
	    if (!restartDone) {		
		int k = nodeToIndexMap.get(startNode);
		M[i][k] = restartProbability 
			+ (1 - restartProbability) * nodeRestartPr / totalPr;
	    }	    
	}
	
	return M;
    }
}
