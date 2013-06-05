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
import biomine.bmgraph.BMEntity;
import biomine.bmgraph.BMGraph;
import biomine.bmgraph.BMNode;
import biomine.bmgraph.attributes.AttributeType;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;

/**
 * BMGraph writer for outputting GraphML format to a file or stream.
 * The GraphML output supports most, if not all, features of BMGraph.
 * Even node groups can be represented as subgraphs, though this is
 * still quite experimental (and probably will remain so, as GraphML
 * readers seem to ignore them).
 * @author Kimmo Kulovesi
 */
public class GraphMLWriter extends BMGraphWriter {

    /**
     * Node attributes which are always ignored when writing. These
     * would override some of the attribute names generated internally.
     */
    public static final HashSet<String> IGNORED_NODE_ATTRIBUTES =
        new HashSet<String>();

    /**
     * Edge attributes which are always ignored when writing. These
     * would override some of the attribute names generated internally.
     */
    public static final HashSet<String> IGNORED_EDGE_ATTRIBUTES =
        new HashSet<String>();

    static {
        attributeKeywords.put(AttributeType.BOOLEAN, "boolean");
        attributeKeywords.put(AttributeType.REAL, "double");
        attributeKeywords.put(AttributeType.INTEGER, "int");
        attributeKeywords.put(AttributeType.STRING, "string");
        IGNORED_NODE_ATTRIBUTES.add("_groupnode");
        IGNORED_NODE_ATTRIBUTES.add("_membernode");
        IGNORED_NODE_ATTRIBUTES.add("special");
        IGNORED_NODE_ATTRIBUTES.add("dbid");
        IGNORED_NODE_ATTRIBUTES.add("db");
        IGNORED_NODE_ATTRIBUTES.add("id");
        IGNORED_NODE_ATTRIBUTES.add("type");
        IGNORED_NODE_ATTRIBUTES.add("_nodesize");
        IGNORED_NODE_ATTRIBUTES.add("pos_x");
        IGNORED_NODE_ATTRIBUTES.add("pos_y");
        IGNORED_NODE_ATTRIBUTES.add("_members");
        IGNORED_EDGE_ATTRIBUTES.add("type");
        IGNORED_EDGE_ATTRIBUTES.add("reverse_type");
        IGNORED_EDGE_ATTRIBUTES.add("_reversed");
    }

    @Override
    public boolean writesAttributeTypes () {
        return true;
    }

    /**
     * Should groups be written as subgraphs (default = false)?
     */
    public boolean writeGroupsAsSubgraphs = false;

    /**
     * Should the graph be written as a directed graph (default = false)?
     */
    public boolean writeDirected = false;

    /**
     * The mapping from BMNodes to node ids in the output.
     */
    protected HashMap<BMNode, String> nodeIds;

    /**
     * The mapping of node attribute names to ids in the output.
     */
    protected HashMap<String, String> nodeAttributes;

    /**
     * The mapping of edge attribute names to ids in the output.
     */
    protected HashMap<String, String> edgeAttributes;

    /**
     * Escape text for characters that might break the output.
     * @param text Text to escape.
     * @return The escaped text.
     */
    protected String escapeText (String text) {
        return text.replaceAll("&", "&amp;").replaceAll("\"", "&quot;");
    }

    /**
     * Print the given attribute to the output.
     * Must be inside the proper element (attributes list).
     * @param attribute Attribute id (not the actual name!).
     * @param value Attribute value.
     */
    protected void printAttribute(String attribute, String value) {
        if (attribute == null || value == null)
            return;
        stream.print("\t<data key=\"");
        stream.print(attribute);
        stream.print("\">");
        stream.print(escapeText(value));
        stream.println("</data>");
    }

    /**
     * Print the given node attribute to the output.
     * Must be inside the proper element (node attributes list).
     * @param attribute Attribute name.
     * @param value Attribute value.
     */
    protected void printNodeAttribute (String attribute, String value) {
        printAttribute(nodeAttributes.get(attribute), value);
    }

    /**
     * Print the given edge attribute to the output.
     * Must be inside the proper element (edge attributes list).
     * @param attribute Attribute name.
     * @param value Attribute value.
     */
    protected void printEdgeAttribute (String attribute, String value) {
        printAttribute(edgeAttributes.get(attribute), value);
    }

    /**
     * Print all attributes of the given entity, except ignored ones.
     * @param entity The entity whose attributes to print.
     * @param ignored The set of ignored attributes (not printed).
     * @param attributeIds The mapping of attribute names to ids.
     */
    protected void printAttributes (BMEntity entity, Set<String> ignored,
                                  HashMap<String, String> attributeIds) {
        HashMap<String, String> attributes = entity.getAttributes();
        if (attributes == null)
            return;
        TreeSet<String> sorted = new TreeSet<String>(attributes.keySet());
        sorted.removeAll(ignored);
        for (String key : sorted) {
            AttributeType type = getAttributeType(key);
            String value = attributes.get(key);
            if (type.matches(value)) {
                if (type == AttributeType.BOOLEAN)
                    value = value.replace("0", "false").replace("1", "true");
                printAttribute(attributeIds.get(key), value);
            }
        }
    }

    /**
     * Split the comma-separated position attribute into two doubles
     * and return an array of their string representations.
     * @param pos The position attribute (may be null).
     * @return Array of two elements (0 = x-pos, 1 = y-pos) or null.
     */
    protected static String[] splitPos(String pos) {
        if (pos == null)
            return null;

        int comma = pos.indexOf(',');
        if (comma > 0 && comma < pos.length()) {
            String xpos = null, ypos = null;
            try {
                double x = Double.parseDouble(pos.substring(0, comma));
                double y = Double.parseDouble(pos.substring(comma + 1));
                xpos = Double.toString(x);
                ypos = Double.toString(y);
            } catch (Exception e) {
                // Ignore errors in parsing position
            }
            if (xpos != null && ypos != null) {
                String[] posa = { xpos, ypos };
                return posa;
            }
        }
        return null;
    }

    /**
     * Print the given BMNode to the output, along with its attributes.
     * @param node The node to print.
     * @param nodeid The id of the node in the output.
     */
    protected void printNode (BMNode node, String nodeid) {
        stream.print("<node id=\"");
        stream.print(nodeid);
        stream.println("\">");
        printNodeAttribute("dbid", node.getId());
        {
            String[] splitId = node.splitId();
            if (splitId != null) {
                printNodeAttribute("db", splitId[0]);
                printNodeAttribute("id", splitId[1]);
            }
        }
        printNodeAttribute("type", node.getType());
        printAttributes(node, ignoredNodeAttributes, nodeAttributes);
        String[] pos = splitPos(node.get("pos"));
        if (pos != null) {
            printNodeAttribute("pos_x", pos[0]);
            printNodeAttribute("pos_y", pos[1]);
        }
    }

    /**
     * Print the given BMNode to the output, along with its attributes.
     * @param node The node to print.
     */
    protected void printNode (BMNode node) {
        String nodeid = nodeIds.get(node);
        assert nodeid != null : "Tried to print unknown node!";
        printNode(node, nodeid);
    }

    /**
     * Print the given BMEdge to the output, along with its attributes.
     * @param edge The edge to print.
     */
    protected void printEdge (BMEdge edge) {
        String from, to;
        String linktype, reversed;

        if (edge.isReversed()) {
            from = nodeIds.get(edge.getTo());
            to = nodeIds.get(edge.getFrom());
            reversed = edge.getLinktype();
            linktype = graph.getReverseType(reversed);
        } else {
            from = nodeIds.get(edge.getFrom());
            to = nodeIds.get(edge.getTo());
            linktype = edge.getLinktype();
            reversed = graph.getReverseType(linktype);
        }

        stream.print("<!-- ");
        stream.print(edge.getFrom());
        stream.print(edge.isReversed() ? " <- " : " -> ");
        stream.print(edge.getTo());
        stream.print(" -->\n<edge source=\"");
        stream.print(from);
        stream.print("\" target=\"");
        stream.print(to);
        stream.println("\">");
        printEdgeAttribute("type", linktype);
        printEdgeAttribute("reverse_type", reversed);
        printAttributes(edge, ignoredEdgeAttributes, edgeAttributes);
        stream.println("</edge>");
    }

    /**
     * Get the set of node attributes actually in use.
     * @param nodes The collection of nodes for which to get the attributes.
     * @return The set of attributes in use (modifiable).
     */
    protected TreeSet<String> nodeAttributesInUse (Collection<BMNode> nodes) {
        TreeSet<String> attributesInUse = new TreeSet<String>();

        for (BMNode node : nodes) {
            HashMap<String, String> attributes = node.getAttributes();
            if (attributes != null) {
                for (String key : attributes.keySet())
                    attributesInUse.add(key);
            }
        }
        attributesInUse.removeAll(ignoredNodeAttributes);
        attributesInUse.addAll(IGNORED_NODE_ATTRIBUTES);

        return attributesInUse;
    }

    /**
     * Get the set of edge attributes actually in use.
     * @param edges The collection of edges for which to get the attributes.
     * @return The set of attributes in use (modifiable).
     */
    protected TreeSet<String> edgeAttributesInUse (Collection<BMEdge> edges) {
        TreeSet<String> attributesInUse = new TreeSet<String>();

        for (BMEdge edge : edges) {
            HashMap<String, String> attributes = edge.getAttributes();
            if (attributes != null) {
                for (String key : attributes.keySet())
                    attributesInUse.add(key);
            }
        }
        attributesInUse.removeAll(ignoredEdgeAttributes);
        attributesInUse.addAll(IGNORED_EDGE_ATTRIBUTES);

        return attributesInUse;
    }
    
    protected void writeGraph (Collection<BMNode> nodes,
                               Collection<BMEdge> edges,
                               Collection<String> comments,
                               Collection<BMNode> specialNodes,
                               Collection<BMNode> groupNodes,
                               Collection<BMEdge> groupMemberEdges,
                               Collection<String> linktypes) {
        assert graph != null : "Null graph when writing";
        assert stream != null : "Null stream when writing";

        int i;
        TreeSet<String> attributesInUse;

        ignoreNodeAttributes(IGNORED_NODE_ATTRIBUTES);
        ignoreEdgeAttributes(IGNORED_EDGE_ATTRIBUTES);

        // Header
        stream.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        stream.println("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\""); 
        stream.println("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        stream.println("xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns ");
        stream.println(" http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">");

        // Linktypes
        for (String linktype : linktypes) {
            String reverse = graph.getReverseType(linktype);
            if (reverse.equals(linktype)) {
                stream.print("<!-- _symmetric ");
                stream.print(linktype);
                stream.println(" -->");
            } else {
                if (!reverse.equals("-"+linktype)) {
                    stream.print("<!-- _reverse ");
                    stream.print(linktype);
                    stream.print(" ");
                    stream.print(reverse);
                    stream.println(" -->");
                }
            }
        }

        // Comments
        if (comments != null) {
            for (String comment : comments) {
                stream.print("<!-- ");
                stream.print(comment.replaceAll("-->", "--&gt;"));
                stream.println(" -->");
            }
        }

        // Output node attribute specifications
        attributesInUse = nodeAttributesInUse(nodes);
        nodeAttributes = new HashMap<String, String>(attributesInUse.size());
        i = 0;
        for (String attribute : attributesInUse) {
            ++i;
            nodeAttributes.put(attribute, "n_"+i);
            stream.print("<key id=\"n_");
            stream.print(i);
            stream.print("\" for=\"node\" attr.name=\"");
            stream.print(escapeText(attribute));
            stream.print("\" attr.type=\"");
            stream.print(getAttributeKeyword(attribute));
            stream.println("\" />");
        }

        // Output edge attribute specifications
        attributesInUse = edgeAttributesInUse(edges);
        edgeAttributes = new HashMap<String, String>(attributesInUse.size());
        i = 0;
        for (String attribute : attributesInUse) {
            ++i;
            edgeAttributes.put(attribute, "e_"+i);
            stream.print("<key id=\"e_");
            stream.print(i);
            stream.print("\" for=\"edge\" attr.name=\"");
            stream.print(escapeText(attribute));
            stream.print("\" attr.type=\"");
            stream.print(getAttributeKeyword(attribute));
            stream.println("\" />");
        }

        // Graph header
        stream.print("<graph edgedefault=\"");
        if (!writeDirected)
            stream.print("un");
        stream.println("directed\">");

        // Nodes other than group nodes
        nodeIds = new HashMap<BMNode, String>(nodes.size());
        i = 0;
        for (BMNode node : nodes) {
            ++i;
            nodeIds.put(node, ""+i);
            if (!groupNodes.contains(node)) {
                printNode(node, ""+i);
                if (specialNodes.contains(node))
                    printNodeAttribute("special", "true");
                printNodeAttribute("_nodesize", "1");
                stream.println("</node>");
            }
        }

        // Group nodes
        for (BMNode node : groupNodes) {
            List<BMEdge> groupEdges;
            Collection<BMNode> members = graph.getMembersFor(node);
            groupEdges = graph.getNodeEdges(node);
            printNode(node);
            if (specialNodes.contains(node))
                printNodeAttribute("special", "true");
            printNodeAttribute("_groupnode", "true");
            if (members != null && members.size() > 0) {
                members = new TreeSet<BMNode>(members);
                Iterator<BMNode> iterator = members.iterator();
                BMNode member = iterator.next();

                printNodeAttribute("_nodesize", ""+members.size());

                if (writeGroupsAsSubgraphs) {
                    stream.print("\n<!-- Group: ");
                    stream.print(node.toString());
                    stream.print("-->\n<graph id=\"G_");
                    stream.print(nodeIds.get(node));
                    stream.print("\" edgedefault=\"");
                    if (!writeDirected)
                        stream.print("un");
                    stream.println("directed\">");
                    do {
                        ++i;
                        nodeIds.put(member, ""+i);
                        printNode(member);
                        printNodeAttribute("membernode", "true");
                        stream.println("</node>");
                        // DEBUG: Should member edges be printed?
                        for (BMEdge edge : groupEdges)
                            printEdge(edge.cloneReplace(node, member));
                        if (!iterator.hasNext())
                            break;
                        member = iterator.next();
                    } while (true);
                    stream.println("</graph>\n");
                } else {
                    String member_ids = member.toString();
                    while (iterator.hasNext()) {
                        member = iterator.next();
                        member_ids = member_ids + " " + member.toString();
                    }
                    printNodeAttribute("_members", member_ids);
                }
            }
            stream.println("</node>");
        }

        // Edges
        for (BMEdge edge : edges) {
            printEdge(edge);
        }

        nodeIds = null;
        nodeAttributes = null;
        edgeAttributes = null;
        stream.println("</graph>\n</graphml>");
    }

    /**
     * Main function for testing with files given on command line. All
     * files specified on the command line (or stdin if none) are
     * considered to be part of a single graph. This graph will be read
     * and then output as GraphML (with edge directions canonized).
     * @param filenames Files to parse, none for System.in.
     */
    public static void main (String[] filenames) throws Exception {
        GraphMLWriter writer = new GraphMLWriter();
        BMGraph graph = readGraph(filenames);
        writer.setGraph(graph);
        writer.setStream(System.out);
        writer.writeSorted(true, true);
    }

}
