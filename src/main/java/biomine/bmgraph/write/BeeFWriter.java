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
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

/**
 * BMGraph writer for outputting BeeF. The output supports most features
 * of BMGraph. Groups nodes can be written as hyperedges or as special
 * nodes.
 * @author Kimmo Kulovesi
 */
public class BeeFWriter extends GraphMLWriter {

    static {
        attributeKeywords.put(AttributeType.BOOLEAN, "boolean");
        attributeKeywords.put(AttributeType.REAL, "float");
        attributeKeywords.put(AttributeType.INTEGER, "integer");
        attributeKeywords.put(AttributeType.STRING, "string");
        IGNORED_NODE_ATTRIBUTES.add("pos");
        IGNORED_NODE_ATTRIBUTES.add("pos_x");
        IGNORED_NODE_ATTRIBUTES.add("pos_y");
        IGNORED_EDGE_ATTRIBUTES.add("pos");
        IGNORED_EDGE_ATTRIBUTES.add("pos_x");
        IGNORED_EDGE_ATTRIBUTES.add("pos_y");
    }

    protected String graphId;
    protected String graphURI = "http://biomine.cs.helsinki.fi/";

    /**
     * Should groups be written as hyperedges (default = true)?
     */
    public boolean writeGroupsAsHyperedges = true;

    /**
     * Escape text for characters that might break the output.
     * @param text Text to escape.
     * @return The escaped text.
     */
    @Override
    protected String escapeText (String text) {
        text = text.replace("\\", "\\\\'");
        text = text.replace("'", "\\'");
        text = text.replace("\t", "\\t");
        text = text.replace("\n", "\\n");
        text = text.replace("\"", "\\\"");
        text = text.replace("`", "\\`");
        return text;
    }

    /**
     * Print the given attribute to the output and end the
     * current line (feature predicate).
     * @param attribute Attribute id (not the actual name!).
     * @param value Attribute value.
     */
    @Override
    protected void printAttribute(String attribute, String value) {
        if (attribute == null || value == null)
            return;
        stream.print("'");
        stream.print(escapeText(attribute));
        stream.print("', ");
        AttributeType type = getAttributeType(attribute);
        switch (type) {
            case BOOLEAN:
            case INTEGER:
            case REAL:
                stream.print(value);
                break;
            default:
                stream.print("'");
                stream.print(escapeText(value));
                stream.print("'");
                break;
        }
        stream.println(" ).");
    }

    /**
     * Print the given node attribute to the output.
     * @param node Node id.
     * @param attribute Attribute name.
     * @param value Attribute value.
     */
    protected void printNodeAttribute (String node,
                                       String attribute, String value) {
        stream.print("node_feature( ");
        stream.print(graphId);
        stream.print(", ");
        stream.print(node);
        stream.print(", ");
        printAttribute(attribute, value);
    }

    /**
     * Print the given edge attribute to the output.
     * @param edge Edge id.
     * @param attribute Attribute name.
     * @param value Attribute value.
     */
    protected void printEdgeAttribute (String edge,
                                       String attribute, String value) {
        stream.print("edge_feature( ");
        stream.print(graphId);
        stream.print(", ");
        stream.print(edge);
        stream.print(", ");
        printAttribute(attribute, value);
    }

    /**
     * Print all attributes of the given entity, except ignored ones.
     * @param entity The entity whose attributes to print.
     * @param entId The entity id in the graph.
     * @param ignored The set of ignored attributes (not printed).
     * @param isNode Is the entity a node?
     */
    protected void printAttributes (BMEntity entity, String entId,
                                    Set<String> ignored,
                                    boolean isNode) {
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
                if (isNode) {
                    printNodeAttribute(entId, key, value);
                } else {
                    printEdgeAttribute(entId, key, value);
                }
            }
        }
    }

    /**
     * Print all attributes of the given entity, except ignored ones.
     * @param entity The entity whose attributes to print.
     * @param entId The entity id in the graph.
     * @param ignored The set of ignored attributes (not printed).
     */
    protected void printAttributes (BMEntity entity, String entId,
                                    Set<String> ignored) {
        printAttributes(entity, entId, ignored,
                        (entity instanceof BMNode));
    }

    /**
     * Print the given BMNode to the output, along with its attributes.
     * @param node The node to print.
     * @param nodeId The id of the node in the output.
     */
    @Override
    protected void printNode (BMNode node, String nodeId) {
        stream.println("node( "+graphId+", "+nodeId+" ).");
        printNodeAttribute(nodeId, "dbid", node.getId());
        {
            String[] splitId = node.splitId();
            if (splitId != null) {
                printNodeAttribute(nodeId, "db", splitId[0]);
                printNodeAttribute(nodeId, "id", splitId[1]);
            }
        }
        printNodeAttribute(nodeId, "type", node.getType());
        printAttributes(node, nodeId, ignoredNodeAttributes);
        String[] pos = splitPos(node.get("pos"));
        if (pos != null) {
            printNodeAttribute(nodeId, "pos_x", pos[0]);
            printNodeAttribute(nodeId, "pos_y", pos[1]);
        }
    }

    /**
     * Print the given BMNode to the output, along with its attributes.
     * @param node The node to print.
     */
    @Override
    protected void printNode (BMNode node) {
        String nodeId = nodeIds.get(node);
        assert nodeId != null : "Tried to print unknown node!";
        printNode(node, nodeId);
    }

    /**
     * Print the given BMEdge to the output, along with its attributes.
     * @param edge The edge to print.
     * @param edgeId Edge id.
     */
    protected void printEdge (BMEdge edge, String edgeId) {
        boolean isReversed = edge.isReversed();
        String from = nodeIds.get(edge.getFrom());
        String to = nodeIds.get(edge.getTo());
        String linktype = edge.getLinktype();
        String reversed = graph.getReverseType(linktype);

        stream.print("edge( ");
        stream.print(graphId);
        stream.print(", ");
        stream.print(edgeId);
        stream.print(", ");
        stream.print(from);
        stream.print(", ");
        stream.print(to);
        stream.println(" ).");
        if (linktype.length() > 0) {
            printEdgeAttribute(edgeId, "type", linktype);
        }
        if (!edge.isSymmetric()) {
            if (!reversed.equals("-"+linktype)) {
                printEdgeAttribute(edgeId, "reverse_type", reversed);
            }
            if (isReversed) {
                printEdgeAttribute(edgeId, "_reversed", "true");
            }
            stream.print("end_feature( ");
            stream.print(graphId);
            stream.print(", ");
            stream.print(edgeId);
            stream.print(", ");
            stream.print(to);
            stream.println(", 'is_target', true ).");
        }
        printAttributes(edge, edgeId, ignoredEdgeAttributes);
        String[] pos = splitPos(edge.get("pos"));
        if (pos != null) {
            printEdgeAttribute(edgeId, "pos_x", pos[0]);
            printEdgeAttribute(edgeId, "pos_y", pos[1]);
        }
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

        int i;
        TreeSet<String> attributesInUse;

        assert !writeDirected : "directed writing not implemented";

        ignoreNodeAttributes(IGNORED_NODE_ATTRIBUTES);
        ignoreEdgeAttributes(IGNORED_EDGE_ATTRIBUTES);

        if (graphId == null) {
            // DEBUG: Where to get the graph ids (and do we need any)?
            graphId = "g";
        }

        // Header
        stream.println("graph( "+graphId+", '"+graphURI+"' ).");
        {
            String[] db = graph.getDatabaseArray();
            if (db[0] != null) {
                String database = db[0];
                if (db[1] != null) {
                    database += " "+db[1];
                    if (db[2] != null) {
                        database += " "+db[2];
                        if (db[3] != null) {
                            database += " "+db[3];
                        }
                    }
                }
                stream.println("attribute( 'database_version', string ).");
                stream.println("graph_feature( 'database_version', '"+
                               database+"' ).");
            }
        }

        // Linktypes
        for (String linktype : linktypes) {
            String reverse = graph.getReverseType(linktype);
            if (reverse.equals(linktype) || !reverse.equals("-"+linktype)) {
                stream.print("attribute( ");
                stream.println("'reverse_type["+linktype+"]', string ).");
                stream.print("graph_feature( "+graphId+", ");
                stream.print("'reverse_type["+linktype+"]', '");
                stream.print(reverse);
                stream.println("' ).");
            }
        }

        // Comments
        attributesInUse = new TreeSet<String>();
        if (comments != null) {
            String canvasLine = null;
            for (String comment : comments) {
                if (comment.startsWith("_canvas ")) {
                    stream.println("attribute( 'bmvis_canvas', string ).");
                    canvasLine = "graph_feature( 'bmvis_canvas', '" +
                                 comment.replaceFirst("_canvas ", "") +
                                 "' ).";
                } else {
                    stream.println("# "+comment);
                }
            }
            if (canvasLine != null) {
                stream.println(canvasLine);
            }
        }

        stream.println("attribute( 'is_target', boolean ).");

        {
            LinkedList<BMEntity> entities = new LinkedList<BMEntity>();
            HashMap<String, AttributeType> inferredTypes;
            
            entities.addAll(nodes);
            entities.addAll(edges);
            inferredTypes = AttributeType.inferFromValues(entities);
            inferredTypes.putAll(attributeTypes);
            attributeTypes.clear();
            attributeTypes.putAll(inferredTypes);
        }

        // Output node attribute specifications
        attributesInUse = nodeAttributesInUse(nodes);
        if (writeGroupsAsHyperedges || writeGroupsAsSubgraphs) {
            attributesInUse.remove("_nodesize");
            attributesInUse.remove("_groupnode");
        }
        for (String attribute : attributesInUse) {
            stream.print("attribute( '");
            stream.print(attribute);
            stream.print("', ");
            stream.print(getAttributeKeyword(attribute));
            stream.println(" ).");
        }

        // Output edge attribute specifications
        attributesInUse = edgeAttributesInUse(edges);
        for (String attribute : attributesInUse) {
            // DEBUG: Collision with node attributes?
            stream.print("attribute( '");
            stream.print(attribute);
            stream.print("', ");
            stream.print(getAttributeKeyword(attribute));
            stream.println(" ).");
        }

        nodeIds = new HashMap<BMNode, String>(nodes.size());

        // Nodes other than group nodes
        i = 0;
        for (BMNode node : nodes) {
            String nodeId;
            ++i;
            nodeId = "n"+i;
            nodeIds.put(node, nodeId);
            if (!groupNodes.contains(node)) {
                printNode(node, nodeId);
                if (specialNodes.contains(node))
                    printNodeAttribute(nodeId, "_special", "true");
            }
        }

        assert !writeGroupsAsSubgraphs :
            "writing subgraph groups not supported (yet)";

        // Group nodes
        for (BMNode node : groupNodes) {
            List<BMEdge> groupEdges;
            Collection<BMNode> members = graph.getMembersFor(node);
            String nodeId = nodeIds.get(node);
            groupEdges = graph.getNodeEdges(node);

            members = new TreeSet<BMNode>(members);
            if (members != null && members.size() > 0) {
                ++i;

                if (writeGroupsAsHyperedges) {
                    String memberId;
                    int j;

                    for (BMNode member : members) {
                        ++i;
                        memberId = "n"+i;
                        nodeIds.put(member, memberId);

                        printNode(member, memberId);
                        if (specialNodes.contains(node)) {
                            printNodeAttribute(memberId, "_special", "true");
                        }
                        j = 0;
                        for (BMEdge edge : groupEdges) {
                            ++j;
                            String linktype = edge.getLinktype();
                            BMNode from = edge.getFrom();
                            BMNode to = edge.getTo();
                            BMEdge cedge = null;
                            for (BMEdge medge : groupMemberEdges) {
                                // DEBUG: Terribly inefficient inner loop
                                if (linktype.equals(medge.getLinktype()) &&
                                    (medge.getFrom().equals(member) &&
                                     medge.getTo().equals(to)) ||
                                    (medge.getTo().equals(member) &&
                                     medge.getFrom().equals(from))) {
                                    cedge = medge.clone();
                                    break;
                                }
                            }
                            if (cedge == null) {
                                cedge = edge.cloneReplace(node, member);
                            } else {
                                cedge.putAll(edge.getAttributes());
                            }
                            printEdge(cedge, "e"+j+memberId);
                        }
                    }

                    for (BMNode member : members) {
                        memberId = nodeIds.get(member);
                        stream.print("hyperedge( ");
                        stream.print(graphId);
                        stream.print(", ");
                        stream.print(nodeId);
                        stream.print(", ");
                        stream.print(memberId);
                        stream.println(" ).");
                    }
                    printEdgeAttribute(nodeId, "_groupedge", "true");
                    printAttributes(node, nodeId, ignoredNodeAttributes,
                                    false);
                } else {
                    Iterator<BMNode> iterator = members.iterator();
                    BMNode member = iterator.next();

                    printNodeAttribute(nodeId, "_nodesize",
                            ""+members.size());
                    String member_ids = member.toString();
                    while (iterator.hasNext()) {
                        member = iterator.next();
                        member_ids = member_ids + " " + member.toString();
                    }
                    printNodeAttribute(nodeId, "_members", member_ids);

                    printNode(node);
                    if (specialNodes.contains(node))
                        printNodeAttribute(nodeId, "_special", "true");
                    printNodeAttribute(nodeId, "_groupnode", "true");
                }
            }
        }

        // Edges
        i = 0;
        for (BMEdge edge : edges) {
            printEdge(edge, "e"+i);
            ++i;
        }
    }

    /**
     * Main function for testing with files given on command line. All
     * files specified on the command line (or stdin if none) are
     * considered to be part of a single graph. This graph will be read
     * and then output as GraphML (with edge directions canonized).
     * @param filenames Files to parse, none for System.in.
     */
    public static void main (String[] filenames) throws Exception {
        BeeFWriter writer = new BeeFWriter();
        BMGraph graph = readGraph(filenames);
        writer.setGraph(graph);
        writer.setStream(System.out);
        writer.writeSorted(true, true);
    }

}
