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
import biomine.bmgraph.BMNode;
import biomine.bmgraph.attributes.AttributeType;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Iterator;

/**
 * BMGraph writer for outputting TLP (Tulip) format to a file or stream.
 * @author Kimmo Kulovesi
 */
public class TLPWriter extends BMGraphWriter {

    /**
     * Attributes which are always ignored when writing. These
     * would override some of the attribute names generated internally.
     */
    public static final HashSet<String> IGNORED_ATTRIBUTES = new HashSet<String>();

    static {
        attributeKeywords.put(AttributeType.BOOLEAN, "bool");
        attributeKeywords.put(AttributeType.REAL, "double");
        attributeKeywords.put(AttributeType.STRING, "string");
        attributeKeywords.put(AttributeType.INTEGER, "int");
        IGNORED_ATTRIBUTES.add("viewLabel");
        IGNORED_ATTRIBUTES.add("viewColor");
        IGNORED_ATTRIBUTES.add("viewLayout");
        IGNORED_ATTRIBUTES.add("viewSelection");
        IGNORED_ATTRIBUTES.add("viewSize");
        IGNORED_ATTRIBUTES.add("viewShape");
        IGNORED_ATTRIBUTES.add("special");
        IGNORED_ATTRIBUTES.add("_groupnode");
        IGNORED_ATTRIBUTES.add("_nodesize");
        IGNORED_ATTRIBUTES.add("member_type");
        IGNORED_ATTRIBUTES.add("member_ids");
    }

    @Override
    public boolean writesAttributeTypes () {
        return true;
    }

    private String checkedValue (String key, String value) {
        AttributeType type = getAttributeType(key);
        if (!type.matches(value))
            return null;
        if (type == AttributeType.BOOLEAN)
            return value.replace("0", "false").replace("1", "true");
        return value.replaceAll("\"", "'");
    }

    private void addAttribute (Map<String, String> attributes,
                                 String key, String value) {
        String existing = attributes.get(key);
        if (existing == null)
            existing = "";
        attributes.put(key, existing + "  " + value + "\n");
    }

    private void addNodeAttribute (Map<String, String> attributes,
                                   int node, String key, String value) {
        value = checkedValue(key, value);
        if (value != null)
            addAttribute(attributes, key, "(node "+node+" \""+value+"\")");
    }

    private void addEdgeAttribute (Map<String, String> attributes,
                                   int edge, String key, String value) {
        value = checkedValue(key, value);
        if (value != null)
            addAttribute(attributes, key, "(edge "+edge+" \""+value+"\")");
    }

    private void addAttributes (Map<String, String> attributes,
                                HashMap<String, String> add,
                                String id, Set<String> ignored) {
        if (add == null)
            return;
        for (String key : new TreeSet<String>(add.keySet())) {
            if (!ignored.contains(key)) {
                String value = checkedValue(key, add.get(key));
                if (value != null)
                    addAttribute(attributes, key, "("+id+" \""+value+"\")");
            }
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
        HashMap<BMNode, Integer> nodeIds;
        HashMap<String, String> attributes = new HashMap<String, String>(128);
        nodeIds = new HashMap<BMNode, Integer>(nodes.size());

        ignoreNodeAttributes(IGNORED_ATTRIBUTES);
        ignoreEdgeAttributes(IGNORED_ATTRIBUTES);

        // Header
        stream.println("(tlp \"2.0\"");

        // Linktypes
        for (String linktype : linktypes) {
            String reverse = graph.getReverseType(linktype);
            if (reverse.equals(linktype)) {
                stream.print("(comments \"_symmetric ");
                stream.print(linktype);
                stream.println("\")");
            } else {
                if (!reverse.equals("-"+linktype)) {
                    stream.print("(comments \"_reverse ");
                    stream.print(linktype);
                    stream.print(" ");
                    stream.print(reverse);
                    stream.println("\")");
                }
            }
        }

        // Comments
        if (comments != null) {
            for (String comment : comments) {
                stream.print("(comments \"");
                stream.print(comment.replaceAll("\"", "'"));
                stream.println("\")");
            }
        }

        // Nodes
        { 
            i = 0;

            for (BMNode node : specialNodes) {
                if (!groupNodes.contains(node)) {
                    ++i;
                    nodeIds.put(node, i);
                    addNodeAttribute(attributes, i, "special", "true");
                }
            }
            for (BMNode node : groupNodes) {
                Collection<BMNode> members = graph.getMembersFor(node);
                ++i;
                nodeIds.put(node, i);
                if (specialNodes.contains(node))
                    addNodeAttribute(attributes, i, "special", "true");
                addNodeAttribute(attributes, i, "_groupnode", "true");
                if (members != null && members.size() > 0) {
                    members = new TreeSet<BMNode>(members);
                    Iterator<BMNode> iterator = members.iterator();
                    BMNode member = iterator.next();

                    addNodeAttribute(attributes, i, "_nodesize",
                                     ""+members.size());
                    addNodeAttribute(attributes, i, "member_type",
                                     member.getType());

                    String ids = member.getId();
                    while (iterator.hasNext()) {
                        member = iterator.next();
                        ids = ids+","+member.getId();
                    }
                    addNodeAttribute(attributes, i, "member_ids", ids);
                }
            }
            for (BMNode node : nodes) {
                int nodeid;
                if (nodeIds.containsKey(node)) {
                    nodeid = nodeIds.get(node);
                } else {
                    ++i;
                    nodeIds.put(node, i);
                    nodeid = i;
                }
                addNodeAttribute(attributes, nodeid, "viewLabel", node.toString());
                addAttributes(attributes, node.getAttributes(),
                              "node "+i, ignoredNodeAttributes);
            }

            stream.print("(nodes");
            for (int nodeid = 1; nodeid <= i; ++nodeid) {
                stream.print(" ");
                stream.print(nodeid);
            }
            stream.println(")");
        }

        // Edges
        i = 0;
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
            ++i;
            stream.print("(edge ");
            stream.print(i);
            stream.print(" ");
            stream.print(from);
            stream.print(" ");
            stream.print(to);
            stream.println(")");
            addEdgeAttribute(attributes, i, "viewLabel", linktype);
            addAttributes(attributes, edge.getAttributes(),
                          "edge "+i, ignoredEdgeAttributes);
        }

        // Attributes
        for (String key : new TreeSet<String>(attributes.keySet())) {
            AttributeType type = getAttributeType(key);
            stream.print("(property 0 ");
            stream.print(getAttributeKeyword(type));
            stream.print(" \"");
            stream.print(key);
            stream.print("\"\n  (default ");
            switch (type) {
                case BOOLEAN:
                    stream.println("\"false\" \"false\")");
                    break;
                case REAL:
                    stream.println("\"1.0\" \"1.0\")");
                    break;
                case INTEGER:
                    stream.println("\"0\" \"0\")");
                    break;
                default:
                    stream.println("\"\" \"\")");
                    break;
            }
            stream.print(attributes.get(key));
            stream.println(")");
        }

        stream.println(")");
    }

    /**
     * Main function for testing with files given on command line. All
     * files specified on the command line (or stdin if none) are
     * considered to be part of a single graph. This graph will be read
     * and then output as TLP (with edge directions canonized).
     * @param filenames Files to parse, none for System.in.
     */

    public static void main (String[] filenames) throws Exception {
        TLPWriter writer = new TLPWriter();
        writer.setGraph(readGraph(filenames));
        writer.setStream(System.out);
        writer.writeSorted(true, true);
    }

}
