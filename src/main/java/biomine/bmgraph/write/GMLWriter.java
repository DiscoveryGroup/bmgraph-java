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
 * BMGraph writer for outputting GML format to a file or stream.
 * @author Kimmo Kulovesi
 */

public class GMLWriter extends BMGraphWriter {

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
        IGNORED_NODE_ATTRIBUTES.add("label");
        IGNORED_NODE_ATTRIBUTES.add("special");
        IGNORED_NODE_ATTRIBUTES.add("_groupnode");
        IGNORED_NODE_ATTRIBUTES.add("_nodesize");
        IGNORED_NODE_ATTRIBUTES.add("member_type");
        IGNORED_NODE_ATTRIBUTES.add("member_ids");
        IGNORED_EDGE_ATTRIBUTES.add("id");
        IGNORED_EDGE_ATTRIBUTES.add("label");
        IGNORED_EDGE_ATTRIBUTES.add("source");
        IGNORED_EDGE_ATTRIBUTES.add("target");
    }

    /**
     * The value for the "Graphic" attribute to write (default = false).
     */
    protected boolean writeGraphic = false;

    /**
     * The value for the directed attribute to write (default = false).
     */
    protected boolean writeDirected = false;

    private void printAttributes (BMEntity entity, Set<String> ignored) {
        String attributes = entity.attributesToStringExcept(ignored,
                                                            "\t", " &; ", "&; \n");
        if (attributes.length() > 0) {
            attributes = attributes.replaceAll("\"", "'");
            attributes = attributes.replaceAll("&; ", "\"");
            stream.print(attributes);
        }
    }

    private void printNode (int nodeid, BMNode node) {
        stream.print("node [\n\tid ");
        stream.println(nodeid);
        stream.print("\tlabel \"");
        stream.print(node.toString());
        stream.println("\"");
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
        stream.println("graph [");
        stream.print("\tdirected ");
        stream.println(writeDirected ? "1" : "0");
        stream.print("\tGraphic ");
        stream.println(writeGraphic ? "1" : "0");

        // Linktypes
        for (String linktype : linktypes) {
            String reverse = graph.getReverseType(linktype);
            if (reverse.equals(linktype)) {
                stream.print("\tcomment \"_symmetric ");
                stream.print(linktype);
                stream.println("\"");
            } else {
                if (!reverse.equals("-"+linktype)) {
                    stream.print("\tcomment \"_reverse ");
                    stream.print(linktype);
                    stream.print(" ");
                    stream.print(reverse);
                    stream.println("\"");
                }
            }
        }

        // Comments
        if (comments != null) {
            for (String comment : comments) {
                stream.print("\tcomment \"");
                stream.print(comment.replaceAll("\"", "'"));
                stream.println("\"");
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
                    stream.println("\tspecial 1");
                    stream.println("]");
                }
            }
            for (BMNode node : groupNodes) {
                Collection<BMNode> members = graph.getMembersFor(node);
                ++i;
                nodeIds.put(node, i);
                printNode(i, node);
                if (specialNodes.contains(node))
                    stream.println("\tspecial 1");
                stream.println("\t_groupnode 1");
                if (members != null && members.size() > 0) {
                    members = new TreeSet<BMNode>(members);
                    Iterator<BMNode> iterator = members.iterator();
                    BMNode member = iterator.next();

                    stream.print("\t_nodesize ");
                    stream.println(members.size());

                    stream.print("\tmember_type \"");
                    stream.print(member.getType());
                    stream.println("\"");

                    stream.print("\tmember_ids \"");
                    stream.print(member.getId());
                    while (iterator.hasNext()) {
                        member = iterator.next();
                        stream.print(",");
                        stream.print(member.getId());
                    }
                    stream.println("\"");
                }
                stream.println("]");
            }
            for (BMNode node : nodes) {
                if (!nodeIds.containsKey(node)) {
                    ++i;
                    nodeIds.put(node, i);
                    printNode(i, node);
                    stream.println("]");
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
            stream.print("edge [\n\tsource ");
            stream.println(from);
            stream.print("\ttarget ");
            stream.println(to);
            stream.print("\tlabel \"");
            stream.print(linktype);
            stream.println("\"");
            printAttributes(edge, ignoredEdgeAttributes);
            stream.println("]");
        }

        stream.println("]");
    }

    /**
     * Main function for testing with files given on command line. All
     * files specified on the command line (or stdin if none) are
     * considered to be part of a single graph. This graph will be read
     * and then output as GML (with edge directions canonized).
     * @param filenames Files to parse, none for System.in.
     */
    public static void main (String[] filenames) throws Exception {
        GMLWriter writer = new GMLWriter();
        writer.setGraph(readGraph(filenames));
        writer.setStream(System.out);
        writer.writeSorted(true, true);
    }

}
