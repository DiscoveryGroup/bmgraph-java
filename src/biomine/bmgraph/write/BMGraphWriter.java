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
import biomine.bmgraph.attributes.AttributeType;
import biomine.bmgraph.read.BMGraphReader;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.TreeSet;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

/**
 * BMGraph writer for outputting BMGraph format to a file or stream.
 *
 * <p>This class is the preferred way of writing a BMGraph to a file.
 *
 * <p>Apart from "regular" (fast, simple) output, this also supports sorted
 * output which will canonize the output order for everything, allowing
 * simple text-based tools to be used for comparing graphs.
 *
 * @author Kimmo Kulovesi
 */

public class BMGraphWriter {

    /**
     * Types assigned to attribute names.
     */
    public static final HashMap<String, AttributeType> attributeTypes =
                                    new HashMap<String, AttributeType>();

    static {
        attributeTypes.putAll(AttributeType.STANDARD_TYPES);
    }

    /**
     * Graph to be written.
     */
    protected BMGraph graph;

    /**
     * Stream to write to.
     */
    protected PrintStream stream;

    /**
     * Node attributes that aren't printed from nodes.
     */
    protected Set<String> ignoredNodeAttributes;

    /**
     * Edge attributes that aren't printed from edges.
     */
    protected Set<String> ignoredEdgeAttributes;

    /**
     * Keywords (in the output format specification) assigned to attribute types.
     */
    protected static final HashMap<AttributeType, String> attributeKeywords
                                    = new HashMap<AttributeType, String>();

    /**
     * Create a new writer for which the graph and stream MUST be set later.
     * This will create a new writer without any graph or stream set.
     * This can be useful if the ignored attributes are to be set
     * before the input graph and/or output stream are known. However,
     * writing will crash if it is attempted before both graph and
     * stream are set for the writer.
     */
    public BMGraphWriter () {
        this(null, null);
    }

    /**
     * Create a new writer with given graph and OutputStream.
     * @param graph The graph to write.
     * @param stream The stream to write to.
     */
    public BMGraphWriter (BMGraph graph, OutputStream stream) {
        this(graph, new PrintStream(stream));
    }

    /**
     * Create a new writer with given graph and PrintStream.
     * @param graph The graph to write.
     * @param stream The stream to write to.
     */
    public BMGraphWriter (BMGraph graph, PrintStream stream) {
        this(graph, stream, null, null);
    }

    /**
     * Create a new writer with given graph, PrintStream and ignored attributes.
     * The ignored attributes will not be printed from the graph to the
     * output. This can be used to remove attributes that were only used
     * internally, etc.
     * @param graph The graph to write.
     * @param stream The stream to write to.
     * @param ignoreNode Ignored node attributes (or null if none).
     * @param ignoreEdge Ignored edge attributes (or null if none).
     */
    public BMGraphWriter (BMGraph graph, PrintStream stream,
                          Set<String> ignoreNode, Set<String> ignoreEdge) {
        this.graph = graph;
        this.stream = stream;
        ignoredEdgeAttributes = (ignoreNode == null ?
                                    new HashSet<String>(8, 0.9f) : ignoreNode);
        ignoredNodeAttributes = (ignoreEdge == null ?
                                    new HashSet<String>(8, 0.9f) : ignoreEdge);
    }

    /**
     * Ignore the given attribute when writing.
     * @param attribute The attribute to ignore.
     */
    public void ignoreNodeAttribute (String attribute) {
        assert attribute != null : "Null attribute";
        ignoredNodeAttributes.add(attribute);
    }

    /**
     * Ignore the given attribute when writing.
     * @param attribute The attribute to ignore.
     */
    public void ignoreEdgeAttribute (String attribute) {
        assert attribute != null : "Null attribute";
        ignoredEdgeAttributes.add(attribute);
    }

    /**
     * Ignore the given attributes when writing.
     * @param attributes The attributes to ignore.
     */
    public void ignoreNodeAttributes (Collection<String> attributes) {
        assert attributes != null : "Null attributes";
        ignoredNodeAttributes.addAll(attributes);
    }

    /**
     * Ignore the given attributes when writing.
     * @param attributes The attributes to ignore.
     */
    public void ignoreEdgeAttributes (Collection<String> attributes) {
        assert attributes != null : "Null attributes";
        ignoredEdgeAttributes.addAll(attributes);
    }

    /**
     * Un-ignore the given attribute when writing.
     * @param attribute The attribute to ignore.
     */
    public void unignoreNodeAttribute (String attribute) {
        assert attribute != null : "Null attribute";
        ignoredNodeAttributes.remove(attribute);
    }

    /**
     * Un-ignore the given attribute when writing.
     * @param attribute The attribute to ignore.
     */
    public void unignoreEdgeAttribute (String attribute) {
        assert attribute != null : "Null attribute";
        ignoredEdgeAttributes.remove(attribute);
    }

    /**
     * Un-ignore the given attributes when writing.
     * @param attributes The attributes to ignore.
     */
    public void unignoreNodeAttributes (Collection<String> attributes) {
        assert attributes != null : "Null attributes";
        ignoredNodeAttributes.removeAll(attributes);
    }

    /**
     * Un-ignore the given attributes when writing.
     * @param attributes The attributes to ignore.
     */
    public void unignoreEdgeAttributes (Collection<String> attributes) {
        assert attributes != null : "Null attributes";
        ignoredEdgeAttributes.removeAll(attributes);
    }

    /**
     * Get the type assigned to a given attribute name.
     * @param attribute Attribute name (e.g. "reliability").
     * @return Type of attribute (defaults to STRING).
     */
    public AttributeType getAttributeType (String attribute) {
        assert attribute != null : "Null attribute";

        AttributeType type = attributeTypes.get(attribute);
        return (type == null ? AttributeType.STRING : type);
    }

    /**
     * Assign a type to an attribute.
     * @param attribute Attribute name (e.g. "reliability").
     * @param type Type for attribute.
     */
    public void setAttributeType (String attribute, AttributeType type) {
        assert attribute != null : "Null attribute";
        assert type != null : "Null type";

        if (type == AttributeType.STRING)
            attributeTypes.remove(attribute);
        else
            attributeTypes.put(attribute, type);
    }

    /**
     * Does this writer write attribute types (not all formats support them)?
     * @return True iff this writer outputs attribute type information.
     */
    public boolean writesAttributeTypes () {
        return false;
    }

    /**
     * Get the keyword used by the output format to specify a given attribute type.
     * @param type Attribute type.
     * @return The keyword for the given attribute type, null if unspecified.
     */
    public String getAttributeKeyword (AttributeType type) {
        return attributeKeywords.get(type);
    }

    /**
     * Get the keyword used by the output format to specify a given attribute.
     * @param attribute Attribute name.
     * @return The keyword for the given attribute type, null if unspecified.
     */
    public String getAttributeKeyword (String attribute) {
        return attributeKeywords.get(getAttributeType(attribute));
    }

    /**
     * Set the graph to write.
     * @param graph The graph to write next.
     */
    public void setGraph (BMGraph graph) {
        assert graph != null : "Null graph";
        this.graph = graph;
    }

    /**
     * Get the graph which will be written by this writer.
     * @return The graph that will be written next (or null if none).
     */
    public BMGraph getGraph () {
        return this.graph;
    }

    /**
     * Set the stream to write to.
     * @param stream The stream to write to.
     */
    public void setStream (PrintStream stream) {
        assert stream != null : "Null stream";
        this.stream = stream;
    }

    /**
     * Set the stream to write to.
     * @param stream The stream to write to.
     */
    public void setStream (OutputStream stream) {
        assert stream != null : "Null stream";
        setStream(new PrintStream(stream));
    }

    /**
     * Write the graph to the stream in "random" (unsorted) order.
     * @param comments True iff comments are to be written.
     */
    public void write (boolean comments) {
        TreeSet<String> cc = new
            TreeSet<String>(graph.specialCommentsToStrings());
        if (comments)
            cc.addAll(graph.getComments());

        TreeSet<String> linktypes = new TreeSet<String>();
        for (BMEdge edge : graph.getEdges())
            linktypes.add(edge.getLinktype());
        linktypes.retainAll(graph.getLinktypeDefinitions());

        if (comments)

        writeGraph( graph.getNodes(),
                    graph.getEdges(),
                    cc,
                    graph.getSpecialNodes(),
                    graph.getGroupNodes(),
                    graph.getGroupMemberEdges(),
                    linktypes);
    }

    /**
     * Write the graph to the stream in unsorted order with comments.
     */
    public void write () {
        write(true);
    }

    /**
     * Write the graph to the stream in canonical (sorted) order.
     * Edge directions are not canonized.
     * @param comments True iff comments are to be written.
     */
    public void writeSorted (boolean comments) {
        writeSorted(comments, false);
    }

    /**
     * Write the graph to the stream in canonical (sorted) order.
     * @param comments True iff comments are to be written.
     * @param canonizeEdges True iff edge directions are to be canonized.
     */
    public void writeSorted (boolean comments, boolean canonizeEdges) {
        TreeSet<String> cc = new
            TreeSet<String>(graph.specialCommentsToStrings());
        if (comments)
            cc.addAll(graph.getComments());

        TreeSet<BMEdge> edgeSet = new TreeSet<BMEdge>();
        TreeSet<String> linktypes = new TreeSet<String>();
        for (BMEdge edge : graph.getEdges()) {
            linktypes.add(edge.getLinktype());
            edgeSet.add(canonizeEdges ? edge.canonicalDirection() : edge);
        }
        linktypes.retainAll(graph.getLinktypeDefinitions());

        writeGraph( new TreeSet<BMNode>(graph.getNodes()),
                    edgeSet, cc,
                    new TreeSet<BMNode>(graph.getSpecialNodes()),
                    new TreeSet<BMNode>(graph.getGroupNodes()),
                    graph.getGroupMemberEdges(),
                    linktypes );
    }

    /**
     * Write the graph to the stream in sorted order without comments.
     */
    public void writeSorted () {
        writeSorted(false);
    }

    /**
     * Write the graph to the stream in given order.
     * When this method is called, both the graph and stream associated
     * with this writer must be valid and in a state such that the writing
     * can safely proceed.
     * @param nodes Nodes in the order that they will be printed in.
     * @param edges Edges in the order that they will be printed in.
     * @param comments Comments in the order that thay will be printed in
     * (or null).
     * @param specialNodes Special nodes in the order that they will be
     * printed in.
     * @param groupNodes Group nodes in the order that they will be printed in.
     * @param groupMemberEdges Group member edges in the order that they
     * will be printed in.
     * @param linktypes Linktype definitions in the order that they will
     * be printed in.
     */
    protected void writeGraph (Collection<BMNode> nodes,
                               Collection<BMEdge> edges,
                               Collection<String> comments,
                               Collection<BMNode> specialNodes,
                               Collection<BMNode> groupNodes,
                               Collection<BMEdge> groupMemberEdges,
                               Collection<String> linktypes) {
        assert graph != null : "Null graph when writing";
        assert stream != null : "Null stream when writing";
        Collection<BMNode> allGroupMembers = new TreeSet<BMNode>();

        // Comments
        if (comments != null) {
            for (String comment : comments) {
                stream.print("# ");
                stream.println(comment);
            }
        }

        // Special comments
        {
            String[] db = graph.getDatabaseArray();
            if (db[0] != null) {
                String comment = "# _database "+db[0];
                if (db[1] != null) {
                    comment += " "+db[1];
                    if (db[2] != null) {
                        comment += " "+db[2];
                        if (db[3] != null)
                            comment += " "+db[3];
                    }
                }
                stream.println(comment);
            }
                        
        }

        // Special nodes
        for (BMNode special : specialNodes)
            stream.println(special);

        // Group nodes
        {
            BMNode member;
            Iterator<BMNode> iterator;
            Collection<BMNode> members;

            for (BMNode group : groupNodes) {
                members = graph.getMembersFor(group);
                if (members != null && members.size() > 0) {
                    members = new TreeSet<BMNode>(members);
                    allGroupMembers.addAll(members);
                    stream.print("# _group ");
                    stream.print(group);
                    stream.print(" ");
                    stream.print(members.size());
                    stream.print(" ");
                    iterator = members.iterator();
                    member = iterator.next();
                    stream.print(member.getType());
                    stream.print(" ");
                    stream.print(member.getId());
                    while(iterator.hasNext()) {
                        member = iterator.next();
                        stream.print(",");
                        stream.print(member.getId());
                    }
                    stream.print("\n");
                }
            }
        }

        // Linktypes
        for (String linktype : linktypes) {
            String reverse = graph.getReverseType(linktype);
            if (reverse.equals(linktype)) {
                stream.print("# _symmetric ");
                stream.println(linktype);
            } else {
                if (!reverse.equals("-"+linktype)) {
                    stream.print("# _reverse ");
                    stream.print(linktype);
                    stream.print(" ");
                    stream.println(reverse);
                }
            }
        }
        
        // Edges
        for (BMEdge edge : edges) {
            stream.println(edge.toString() +
                           edge.attributesToStringExcept(ignoredEdgeAttributes));
        }

        // Edge attributes
        for (BMEdge edge : groupMemberEdges) {
            stream.println("# _edge " + edge.toString() +
                            edge.attributesToStringExcept(ignoredEdgeAttributes));
        }

        // Node attributes
        {
            String attributes;
            for (BMNode node : nodes) {
                attributes = node.attributesToStringExcept(ignoredNodeAttributes);
                if (attributes.length() > 0 || graph.getDegree(node) == 0) {
                    stream.print("# _attributes ");
                    stream.print(node);
                    stream.println(attributes);
                }
            }

            // Group member attributes
            for (BMNode node : allGroupMembers) {
                attributes = node.attributesToStringExcept(ignoredNodeAttributes);
                if (attributes.length() > 0) {
                    stream.print("# _attributes ");
                    stream.print(node);
                    stream.println(attributes);
                }
            }
        }
    }

    /**
     * Read a BMGraph from a given set of files or from stdin.
     * @param filenames Files to read from (all assumed to merge into one graph).
     * @return The graph that was read.
     */
    protected static BMGraph readGraph (String[] filenames) {
        BMGraphReader reader = new BMGraphReader();
        boolean readAnyFiles = false;
        if (filenames != null && filenames.length > 0) {
            for (String filename : filenames) {
                if (filename != null) {
                    readAnyFiles = true;
                    reader.parseFile(filename);
                }
            }
        }
        if (!readAnyFiles)
            reader.parseSystemIn();
        return reader.getGraph();
    }

    /**
     * Main function for testing with files given on command line. All
     * files specified on the command line (or stdin if none) are
     * considered to be part of a single graph. This graph will be read
     * and then output as canonized BMGraph.
     * @param filenames Files to parse, none for System.in.
     */
    public static void main (String[] filenames) throws Exception {
        boolean ungroup = false;
        BMGraphWriter writer = new BMGraphWriter();
        if (filenames.length > 0 && filenames[0].equals("--ungroup")) {
            filenames[0] = null;
            ungroup = true;
        }
        writer.setGraph(readGraph(filenames));
        if (ungroup)
            writer.getGraph().ungroupAll();
        writer.setStream(System.out);
        writer.writeSorted(true);
    }

}
