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
import biomine.bmgraph.BMNode;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;

/**
 * BMGraph writer for outputting XGMML format to a file or stream. Note
 * that this implementation is not entirely safe with maliciously chosen
 * entity names, linktypes or attributes, as most strings are not
 * escaped for character sequences that might break the output result.
 * @author Kimmo Kulovesi
 */
public class XGMMLWriter extends BMGraphWriter {

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
        IGNORED_NODE_ATTRIBUTES.add("id");
        IGNORED_NODE_ATTRIBUTES.add("special");
        IGNORED_NODE_ATTRIBUTES.add("_groupnode");
        IGNORED_NODE_ATTRIBUTES.add("_nodesize");
        IGNORED_NODE_ATTRIBUTES.add("member_type");
        IGNORED_EDGE_ATTRIBUTES.add("id");
        IGNORED_EDGE_ATTRIBUTES.add("source");
        IGNORED_EDGE_ATTRIBUTES.add("target");
    }

    /**
     * Value for the "Graphic" attribute to write (default = false).
     */
    protected boolean writeGraphic = false;

    /**
     * Value for the "directed" attribute to write (default = false).
     */
    protected boolean writeDirected = false;

    private void printAttributes (BMEntity entity, Set<String> ignored) {
        String attributes = entity.attributesToStringExcept(ignored,
                                                            "\t<att name= ;",
                                                             " ; value= ;",
                                                             " ; />\n");
        if (attributes.length() > 0) {
            attributes = attributes.replaceAll("&", "&amp;");
            attributes = attributes.replaceAll("\"", "&quot;");
            attributes = attributes.replaceAll(" ;", "\"");
            stream.print(attributes);
        }
    }

    private void printNode (int nodeid, BMNode node) {
        stream.print("<node id=\""+nodeid+"\" label=\"");
        stream.print(node.toString());
        stream.println("\">");
        printAttributes(node, ignoredNodeAttributes);
    }

    @Override
    protected void writeGraph (Collection<BMNode> nodes,
                               Collection<BMEdge> edges,
                               Collection<String> comments,
                               Collection<BMNode> specialNodes,
                               Collection<BMNode> groupNodes,
                               Collection<BMEdge> groupMemberEdges,
                               Collection<String> linktypes) {
        assert graph != null : "Null graph when writing";
        assert stream != null : "Null stream when writing";

        HashMap<BMNode, Integer> nodeIds;
        nodeIds = new HashMap<BMNode, Integer>(nodes.size());

        ignoreNodeAttributes(IGNORED_NODE_ATTRIBUTES);
        ignoreEdgeAttributes(IGNORED_EDGE_ATTRIBUTES);

        // Header
        stream.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        stream.println("<!DOCTYPE graph PUBLIC " +
                       "\"-//John Punin//DTD graph description//EN\" " +
                       "\"http://www.cs.rpi.edu/~puninj/XGMML/xgmml.dtd\">");
        stream.print("<graph directed=\"");
        stream.print(writeDirected ? "1" : "0");
        stream.print("\" Graphic=\"");
        stream.print(writeGraphic ? "1" : "0");
        stream.println("\">");

        // Comments
        if (comments != null) {
            for (String comment : comments) {
                comment = comment.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                stream.println("<!-- "+comment+" -->");
            }
        }

        // Nodes
        { 
            int i = 0;

            for (BMNode node : specialNodes) {
                if (!groupNodes.contains(node)) {
                    ++i;
                    nodeIds.put(node, i);
                    printNode(i, node);
                    stream.println("\t<att name=\"special\" value=\"1\" />");
                    stream.println("</node>");
                }
            }
            for (BMNode node : groupNodes) {
                Collection<BMNode> members = graph.getMembersFor(node);
                ++i;
                nodeIds.put(node, i);
                printNode(i, node);
                if (specialNodes.contains(node))
                    stream.println("\t<att name=\"special\" value=\"1\" />");
                stream.println("\t<att name=\"_groupnode\" value=\"1\" />");
                if (members != null && members.size() > 0) {
                    members = new TreeSet<BMNode>(members);
                    Iterator<BMNode> iterator = members.iterator();

                    stream.print("\t<att type=\"integer\" name=\"_nodesize\"");
                    stream.print(" value=\"");
                    stream.print(members.size());
                    stream.println("\" />");

                    stream.print("\t<att name=\"member_type\" value=\"");
                    stream.print(iterator.next().getType());
                    stream.println("\" />");

                    stream.println("<att><graph>");
                    for (BMNode member : members) {
                        ++i;
                        nodeIds.put(member, i);
                        printNode(i, member);
                        stream.println("\t<att name=\"membernode\" value=\"1\" />");
                        stream.println("</node>");
                    }
                    stream.println("</graph></att>");
                }
                stream.println("</node>");
            }
            for (BMNode node : nodes) {
                if (!nodeIds.containsKey(node)) {
                    ++i;
                    nodeIds.put(node, i);
                    printNode(i, node);
                    stream.println("</node>");
                }
            }
        }

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
        
        // Edges
        for (BMEdge edge : edges) {
            Integer from, to;
            String linktype;
            if (edge.isReversed()) {
                from = nodeIds.get(edge.getTo());
                to = nodeIds.get(edge.getFrom());
                linktype = graph.getReverseType(edge.getLinktype());
            } else {
                from = nodeIds.get(edge.getFrom());
                to = nodeIds.get(edge.getTo());
                linktype = edge.getLinktype();
            }
            stream.print("<edge source=\"");
            stream.print(from);
            stream.print("\" target=\"");
            stream.print(to);
            stream.print("\" label=\"");
            stream.print(linktype);
            stream.println("\" >");
            printAttributes(edge, ignoredEdgeAttributes);
            stream.println("</edge>");
        }

        stream.println("</graph>");
    }

    /**
     * Main function for testing with files given on command line. All
     * files specified on the command line (or stdin if none) are
     * considered to be part of a single graph. This graph will be read
     * and then output as XGMML (with edge directions canonized).
     * @param filenames Files to parse, none for System.in.
     */

    public static void main (String[] filenames) throws Exception {
        XGMMLWriter writer = new XGMMLWriter();
        writer.setGraph(readGraph(filenames));
        writer.setStream(System.out);
        writer.writeSorted(true, true);
    }

}
