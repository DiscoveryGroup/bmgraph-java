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

package biomine.bmgraph.write;

import biomine.bmgraph.BMEdge;
import biomine.bmgraph.BMGraph;
import biomine.bmgraph.BMNode;

import java.util.Collection;
import java.util.TreeSet;
import java.util.LinkedList;

/**
 * BMGraph writer for outputting "hacked" GraphML format to a file or stream.
 * This is like GraphMLWriter, but it converts each original edge to
 * a node representing the edge, and connects the original endpoints of
 * the edge through that node. This is to circumvent some limitations in
 * GraphML viewers that do not support all of our edge attributes. The
 * nodes representing edges will have the "_edgenode" attribute set. If
 * edge direction is used, the edge with the arrowhead will have the
 * attribute "_arrowedge" set.
 * @author Kimmo Kulovesi
 */
public class GraphMLEdgeHackWriter extends GraphMLWriter {

    static {
        IGNORED_EDGE_ATTRIBUTES.add("pos");
        IGNORED_EDGE_ATTRIBUTES.add("pos_x");
        IGNORED_EDGE_ATTRIBUTES.add("pos_y");
        IGNORED_NODE_ATTRIBUTES.add("_arrowedge");

        LinkedList<String> ll = new LinkedList<String>();
        ll.add("_edgenode");
        IGNORED_EDGE_ATTRIBUTES.addAll(ll);
        IGNORED_NODE_ATTRIBUTES.addAll(ll);
    }
    
    /**
     * The last used id for virtual edgenodes.
     */
    protected int virtualNodeId;

    /**
     * Print an edge by converting it to two edges and a virtual node.
     * @param edge Edge to print.
     */
    @Override
    protected void printEdge (BMEdge edge) {
        BMNode from, to;
        String linktype, reversed;
        String edgeNodeId, edgeNodeType;
        BMNode edgeNode;
        BMEdge virtualEdge;
        String from_pos, to_pos;

        if (edge.isReversed()) {
            from = edge.getTo();
            to = edge.getFrom();
            reversed = edge.getLinktype();
            linktype = graph.getReverseType(reversed);
        } else {
            from = edge.getFrom();
            to = edge.getTo();
            linktype = edge.getLinktype();
            reversed = graph.getReverseType(linktype);
        }

        ++virtualNodeId;
        edgeNodeId = "en"+virtualNodeId;
        edgeNodeType = edge.getLinktype();
        if (edgeNodeType.charAt(0) == '-')
            edgeNodeType = edgeNodeType.substring(1);
        edgeNode = new BMNode(edgeNodeType, "edge"+":"+virtualNodeId);
        nodeIds.put(edgeNode, edgeNodeId);
        printNode(edgeNode);
        printNodeAttribute("_edgenode", "true");
        printNodeAttribute("reverse_type", reversed);
        printAttributes(edge, ignoredEdgeAttributes, nodeAttributes);

        from_pos = edge.get("pos");
        to_pos = null;
        if (from_pos != null) {
            int comma = from_pos.indexOf(',');
            if (comma > 0) {
                try {
                    // Normalize pos format and ensure correctness by parsing
                    to_pos = from_pos.substring(comma + 1);
                    from_pos = from_pos.substring(0, comma);
                    double x = Double.parseDouble(from_pos);
                    double y = Double.parseDouble(to_pos);
                    from_pos = Double.toString(x);
                    to_pos = Double.toString(y);
                } catch (Exception e) {
                    // Ignore errors from parsing the position
                    from_pos = null;
                    to_pos = null;
                }
            } else {
                from_pos = null;
            }
        }
        if (from_pos == null) {
            from_pos = from.get("pos");
            to_pos = to.get("pos");
            if (from_pos != null && to_pos != null) {
                int from_comma = from_pos.indexOf(',');
                int to_comma = to_pos.indexOf(',');
                if (from_comma > 0 && to_comma > 0) {
                    try {
                        double x = Double.parseDouble(to_pos.substring(0,
                                                                to_comma));
                        double y = Double.parseDouble(to_pos.substring(to_comma
                                                                + 1));
                        x += Double.parseDouble(from_pos.substring(0,
                                                                from_comma));
                        y += Double.parseDouble(from_pos.substring(from_comma
                                                                + 1));
                        from_pos = Double.toString(x / 2.0);
                        to_pos = Double.toString(y / 2.0);
                        printNodeAttribute("pos", from_pos+","+to_pos);
                    } catch (Exception e) {
                        // Ignore exceptions from parsing the numbers
                        from_pos = null;
                        to_pos = null;
                    }
                } else {
                    from_pos = null;
                    to_pos = null;
                }
            }
        }
        if (from_pos != null && to_pos != null) {
            printNodeAttribute("pos_x", from_pos);
            printNodeAttribute("pos_y", to_pos);
        }

        stream.println("</node>");

        virtualEdge = edge.cloneReplace(from, edgeNode);
        if (writeDirected)
            virtualEdge.put("_arrowedge", "true");
        super.printEdge(virtualEdge);
        virtualEdge = edge.cloneReplace(to, edgeNode);
        super.printEdge(virtualEdge);
    }

    /**
     * Get the set of node attributes actually in use, including edge
     * attributes for virtual nodes.
     * @param nodes The collection of nodes for which to get the attributes.
     * @return The set of attributes in use (modifiable).
     */
    @Override
    protected TreeSet<String> nodeAttributesInUse (Collection<BMNode> nodes) {
        TreeSet<String> attributesInUse;

        attributesInUse = super.nodeAttributesInUse(nodes);
        // Edge attributes become attributes of the virtual nodes
        attributesInUse.addAll(super.edgeAttributesInUse(graph.getEdges()));

        return attributesInUse;
    }

    @Override
    protected TreeSet<String> edgeAttributesInUse (Collection<BMEdge> edges) {
        TreeSet<String> attributesInUse;
        attributesInUse = super.edgeAttributesInUse(edges);
        attributesInUse.add("_arrowedge");

        return attributesInUse;
    }
    
    @Override
    protected void writeGraph (Collection<BMNode> nodes,
                               Collection<BMEdge> edges,
                               Collection<String> comments,
                               Collection<BMNode> specialNodes,
                               Collection<BMNode> groupNodes,
                               Collection<BMEdge> groupMemberEdges,
                               Collection<String> linktypes) {
        virtualNodeId = 0;
        super.writeGraph(nodes, edges, comments, specialNodes,
                         groupNodes, groupMemberEdges, linktypes);
    }

    /**
     * Main function for testing with files given on command line. All
     * files specified on the command line (or stdin if none) are
     * considered to be part of a single graph. This graph will be read
     * and then output as GraphML (with edge directions canonized).
     * BMGraph edges will be represented by virtual nodes for use with
     * viewers not supporting edge labels or attributes.
     * @param filenames Files to parse, none for System.in.
     */
    public static void main (String[] filenames) throws Exception {
        GraphMLWriter writer = new GraphMLEdgeHackWriter();
        writer.writeDirected = true;
        BMGraph graph = readGraph(filenames);
        writer.setGraph(graph);
        writer.setStream(System.out);
        writer.writeSorted(true, true);
    }

}
