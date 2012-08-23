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

import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Collection;


/**
 * BMGraph. A BMGraph consists of nodes connected by edges, linktypes
 * (and their definitions) for the edges, a set of special nodes and
 * a list of comments.
 *
 * <p>BMGraphs are non-directed, although the information of the edge
 * direction is retained for labeled, non-symmetric edges. This is
 * obviously less general than directed graphs, but makes the
 * implementation much more straightforward and efficient.
 *
 * <p>Nodes may be grouped into node groups, where each group member node
 * shares the same set of attributes and edges. The implementation here
 * will automatically ungroup member nodes of such groups if they are
 * referred to directly. This method of grouping is quite obsolete,
 * and interactive, non-permanent grouping methods in an interactive
 * visualization program are now preferred. The old-style groups are
 * handy for some non-interactive tools, however, and will continue
 * to be supported (for now).
 *
 * <p>To use a BMGraph properly, only BMNodes and BMEdges returned by the
 * graph's methods should be manipulated. The nodes and edges in general
 * represent the concepts, and they will compare equal based on the
 * "concept identity" (type, id, etc) alone. However, in the context of
 * a BMGraph, the set of node and/or edge attributes must be applied to
 * the specific instances of the nodes and edges stored in the graph,
 * and therefore care must be taken to always obtain the reference from
 * the BMGraph itself. This implementation takes some care to avoid
 * pitfalls by providing the user with the proper references to the
 * instances in the graph, but this is not always guaranteed where not
 * explicitly stated, as it is possible to modify the links in edges and
 * such externally after they have been set by BMGraph.
 *
 * <p>For graphs with non-labeled edges, create the edges with either
 * empty linktypes or a lone minus ("-") for the reverse direction. If
 * you wish to make non-labeled edges completely directionless, define
 * the empty string ("") as a symmetric linktype as well.
 *
 * @author Kimmo Kulovesi
 * @author Atte Hinkka, ahinkka@cs.helsinki.fi
 */

public class BMGraph {
    /**
     * Built-in reverse-linknames (only in the canonical direction).
     * Note that these are intentionally not used internally by default
     * (they must be defined with "defineReverseLinktype" for that),
     * but they are built-in to support reading these types as written
     * by some legacy tools.
     *
     * Also, these names are used to get readable linknames for
     * visualization purposes where the machine-readable "-linktype"
     * would be obscure. Note that this HashSet is editable, and
     * further symmetric linktypes may be defined for each graph.
     */
    public static final HashMap<String, String> REVERSE_LINKTYPES = 
                new HashMap<String, String>(8, 0.9f);

    /**
     * Default symmetric linknames. More can be defined for each graph,
     * but these are the defaults that a graph is born with. Note that
     * this HashSet is editable, and furthermore symmetric linktypes may
     * be defined for each graph.
     */
    public static final HashSet<String> SYMMETRIC_LINKTYPES =
                new HashSet<String>(8, 0.9f);

    // DEBUG: Eventually these built-in names should be removed from
    // here and moved exclusively to BMVis.
    static {
        REVERSE_LINKTYPES.put("refers_to", "referred_by");
        REVERSE_LINKTYPES.put("codes_for", "coded_by");
        REVERSE_LINKTYPES.put("has_child", "has_parent");
        REVERSE_LINKTYPES.put("contains", "contained_by");
        REVERSE_LINKTYPES.put("affects", "affected_by");
        REVERSE_LINKTYPES.put("belongs_to", "has_member");
        REVERSE_LINKTYPES.put("has_child", "has_parent");
        REVERSE_LINKTYPES.put("has_variant", "is_variant_of");
        REVERSE_LINKTYPES.put("has_function", "is_function_of");
        REVERSE_LINKTYPES.put("subsumes", "subsumed_by");
        REVERSE_LINKTYPES.put("targets", "targeted_by");
        REVERSE_LINKTYPES.put("is_located_in", "is_location_of");
        REVERSE_LINKTYPES.put("is_part_of", "has_part");
        REVERSE_LINKTYPES.put("participates_in", "has_participant");
        REVERSE_LINKTYPES.put("resolves_to", "resolved_from");
        REVERSE_LINKTYPES.put("has_name", "names");
        REVERSE_LINKTYPES.put("", "-");
        SYMMETRIC_LINKTYPES.add("is_related_to");
        SYMMETRIC_LINKTYPES.add("interacts_with");
        SYMMETRIC_LINKTYPES.add("is_homologous_to");
        SYMMETRIC_LINKTYPES.add("functionally_associated_to");
        SYMMETRIC_LINKTYPES.add("has_synonym");
        SYMMETRIC_LINKTYPES.add("overlaps");
    }

    /**
     * The set of nodes present in the graph (other than group members).
     * The map is used only because HashSet doesn't support retrieving
     * a reference to the actual instance stored in the set, so each
     * node always maps to an instance of the same node.
     */
    protected HashMap<BMNode, BMNode> nodes;

    /**
     * The set of edges in the graph.
     * The map is used only because HashSet doesn't support retrieving
     * a reference to the actual instance stored in the set, so each
     * node always maps to an instance of the same node.
     */
    protected HashMap<BMEdge, BMEdge> edges;

    /**
     * A mapping from each node to a list of edges connecting it.
     */
    protected Map<BMNode, ArrayList<BMEdge>> nodeEdges;

    /**
     * A mapping from each group node to the list of member nodes.
     */
    protected Map<BMNode, LinkedList<BMNode>> groupMembers;

    /**
     * A mapping from each group member node to the group node.
     */
    protected Map<BMNode, BMNode> groupNodes;

    /**
     * A mapping from group member nodes to their pre-group edges.
     */
    protected Map<BMNode, ArrayList<BMEdge>> groupMemberEdges;

    /**
     * The set of special nodes ("protected nodes", if you will).
     */
    protected Set<BMNode> specialNodes;

    /**
     * Mappings for reversing linktypes (in both directions). This may
     * may also include obsolete linktypes, e.g. when a reverse name is
     * replaced but the forward name is kept, then the old reverse name will
     * still map to the forward name but not the other way around.
     */
    protected Map<String, String> reverseLinktype;

    /**
     * The set of canonical directions for linktypes. Symmetric
     * linktypes are also included in this set, even though they do not
     * have a canonical direction. The only way to check for a symmetric
     * linktype is to see if it maps to the same name in reverseLinktype.
     */
    protected Set<String> canonicalDirections;

    /**
     * A linked list of comments related to the graph. No particular
     * file format is assumed for these, so don't expect these to have
     * any comment marker like "#" as a prefix (for the BMGraph format
     * it is stripped by the parser).
     */
    protected LinkedList<String> comments;

    /**
     * A mapping from each special comment to a Set<String> or Map<String,String>.
     * By default, each special comment is represented as a set. 
     * Once {@link #getSpecialCommentAsMap(String)} or 
     * {@link #putSpecialComment(String, String, String)}
     * is called, the representation is changed to a Map.
     * 
     * "Special comments" are meant for various programs to store
     * additional settings and metadata in graph files. When a special
     * comment is parsed, the associated value is added to the set
     * for that comment "type".
     * 
     */
    protected Map<String, Object> specialComments;

    /**
     * True iff empty maps of attributes should be created.
     * By default, all nodes and edges in a BMGraph will have attribute
     * maps created for them upon addition to the graph, unless they
     * already have a map of their own. This behavior can be disabled by
     * setting this to false.
     *
     * <p>Note: This setting has no effect on group member nodes, since
     * they are usually not accessed directly without ungrouping (at
     * which point they are no longer group members and therefore become
     * subject to this setting).
     */
    protected boolean createAttributeMaps;

    /**
     * A four-element array describing the source database.
     * <p>The elements are:
     * <ol><li>database name (e.g. "biomine")<li>
     *     <li>database version (e.g. "v3_2_beta")</li>
     *     <li>database server hostname</li>
     *     <li>node viewer URL for this database</li>
     *  </ol>
     *
     *  <p>Any of the elements may be null.
     */
    protected String[] database;

    /**
     * URL for node expansion.
     */
    protected URL nodeExpandURL;

    /**
     * Name of the callable program for node expansion.
     */
    protected String nodeExpandProgram;

    /**
     * Create a new, empty graph.
     */
    public BMGraph () {
        nodes = new HashMap<BMNode, BMNode>(512);
        edges = new HashMap<BMEdge, BMEdge>(1024);
        nodeEdges = new HashMap<BMNode, ArrayList<BMEdge>>(512);
        groupMemberEdges = new HashMap<BMNode, ArrayList<BMEdge>>(128);
        specialNodes = new HashSet<BMNode>(8);
        groupNodes = new HashMap<BMNode, BMNode>(8);
        groupMembers = new HashMap<BMNode, LinkedList<BMNode>>();
        reverseLinktype = new HashMap<String, String>(16, 0.9f);
        canonicalDirections = new HashSet<String>(16);
        comments = new LinkedList<String>();
        createAttributeMaps = true;
        for (Map.Entry<String, String> entry : REVERSE_LINKTYPES.entrySet()) {
            String key = entry.getKey();
            // Define the reverse types to support _reading_ them
            defineReverseLinktype(key, entry.getValue());
            // Redefine the reverse as "-forward" to avoid writing these
            defineReverseLinktype(key, "-"+key);
        }
        for (String s : SYMMETRIC_LINKTYPES) {
            defineSymmetricLinktype(s);
        }
        database = new String[4];
        database[0] = database[1] = database[2] = database[3] = null;
        nodeExpandURL = null;
        nodeExpandProgram = null;
        specialComments = new HashMap<String, Object>(8);
    }

    /**
     * Create a shallow clone of an existing BMGraph.
     * @param src An existing BMGraph to clone.
     */
    public BMGraph (BMGraph src) {
        nodes = new HashMap<BMNode, BMNode>(src.nodes);
        edges = new HashMap<BMEdge, BMEdge>(src.edges);
        nodeEdges = new HashMap<BMNode, ArrayList<BMEdge>>(src.nodeEdges.size());
        for (Map.Entry<BMNode, ArrayList<BMEdge>> e : src.nodeEdges.entrySet()) {
            nodeEdges.put(e.getKey(), new ArrayList<BMEdge>(e.getValue()));
        }
        groupMemberEdges = new HashMap<BMNode, ArrayList<BMEdge>>(src.groupMemberEdges.size());
        for (Map.Entry<BMNode, ArrayList<BMEdge>> e : src.groupMemberEdges.entrySet()) {
            groupMemberEdges.put(e.getKey(), new ArrayList<BMEdge>(e.getValue()));
        }
        specialNodes = new HashSet<BMNode>(src.specialNodes);
        groupNodes = new HashMap<BMNode, BMNode>(src.groupNodes);
        reverseLinktype = new HashMap<String, String>(src.reverseLinktype);
        canonicalDirections = new HashSet<String>(src.canonicalDirections);
        comments = new LinkedList<String>(src.comments);
        createAttributeMaps = src.createAttributeMaps;
        database = src.database.clone();
        nodeExpandURL = src.nodeExpandURL;
        nodeExpandProgram = src.nodeExpandProgram;
        specialComments = new HashMap<String, Object>(src.specialComments.size());
        copySpecialCommentsFrom(src);
    }

    /**
     * Create a shallow clone of this BMGraph.
     * Note that the actual node and edge objects are not cloned, i.e.
     * changing the attributes of a node or edge in this graph changes
     * the attributes of that node object in cloned graphs as well.
     * @return A shallow clone of this BMGraph.
     */
    @Override
    public Object clone () {
        return new BMGraph(this);
    }

    /**
     * Copy special comments from another graph to this one.
     * The existing special comments in this graph will be kept, but
     * possibly overwritten by those from src.
     * @param src The BMGraph to copy special comments from.
     */
    protected void copySpecialCommentsFrom (BMGraph src) {
        for (Map.Entry<String, Object> e : src.specialComments.entrySet()) {
            String key = e.getKey();
            Object o = e.getValue();

            if (o instanceof Map) {
                Map<String, String> map = new HashMap<String, String>(
                        ((Map<String, String>)o).size() );
                map.putAll((Map<String, String>) o);
                specialComments.put(key, map);
            } else if (o instanceof Set) {
                Set<String> set = new HashSet<String>(((Set<String>)o).size());
                set.addAll((Set<String>) o);
                specialComments.put(key, set);
            } else {
                // DEBUG: This could be problematic since we recycle the
                // same object here, but according to the current
                // implementation of BMGraph special comments this
                // should never happen.
                specialComments.put(key, o);
            }
        }
    }

    /**
     * Ensure this graph has the given node (adding it only if necessary).
     * This is essentially "putNode", but importantly returns the
     * existing instance of the node in the graph instead of replacing
     * it when the node already exists. If the node doesn't exist in
     * the graph, it will be added and the parameter returned. In either
     * case, the returned reference must be used to refer to the node
     * in this graph. Note that an existing node's attributes will not
     * be modified by this, even if the node given as argument has
     * a different set of attributes than the one already in the graph.
     *
     * <p>NOTE: Group member nodes can not be included in the graph
     * separately from the group. They are therefore ungrouped from any
     * group if given as argument to this method. If a group becomes
     * empty as a result, it will be silently deleted. Therefore a
     * previously added group node may no longer be part of the graph
     * after calling this method for another node.
     * @param node The node which will be in this graph.
     * @return The instance of the given node in this graph.
     */
    public BMNode ensureHasNode (BMNode node) {
        assert node != null : "Null node";

        BMNode result = nodes.get(node);
        if (result == null) {
            if (hasGroupMember(node)) {
                result = ungroupMemberNode(node);
            } else {
                nodeEdges.put(node, new ArrayList<BMEdge>());
                nodes.put(node, node);
                result = node;
            }
        }
        if (createAttributeMaps && result.getAttributes() == null)
            result.setAttributes(new HashMap<String, String>(8, 0.9f));
        return result;
    }

    /**
     * Ensure this graph has the given edge (adding it only if necessary).
     * This is essentially "putEdge", but importantly returns the
     * existing instance of the edge in the graph instead of replacing
     * it when the edge already exists. If the edge doesn't exist in
     * the graph, it will be added and the parameter returned. In either
     * case, the returned reference must be used to refer to the edge
     * in this graph. Note that an existing edge's attributes will not
     * be modified by this, even if the edge given as argument has
     * a different set of attributes than the one already in the graph.
     *
     * <p>The nodes of the edge will also be ensured to be in the graph.
     * It is not necessary therefore to add them beforehand by calling
     * ensureHasNode. Note, however, that group members may not be part
     * of the graph separately of their groups, so any such edge
     * endpoints will be silently ungrouped.
     *
     * <p>It is possible for this method to fail in some rare cases, like
     * when the from-node of the edge is a group node and the to-node
     * is the only member node of that group. Such cases would not be
     * valid anyhow. It is also possible that a group node becomes
     * deleted due to ungrouping and later gets re-inserted as
     * a nonsensical node representing no group, but such cases are
     * equally invalid.
     *
     * <p>Note that the it is assumed in many places that the edges have
     * canonical linktypes, and only in the canonical direction (i.e.
     * to store an edge in the other direction, the reversed linktype
     * should be given to the BMEdge constructor). This restriction is,
     * however, not enforced at this point.
     * @param edge The edge which will be in this graph.
     * @return The instance of the given edge stored in this graph.
     */
    public BMEdge ensureHasEdge (BMEdge edge) {
        assert edge != null : "Null edge";

        BMEdge result = edges.get(edge);
        if (result != null)
            return result; // Edge already exists

        BMNode from, to;

        from = ensureHasNode(edge.getFrom());
        to = ensureHasNode(edge.getTo());
        if (!to.equals(edge.getTo())) {
            // To-node had to be ungrouped, therefore the from-node
            // might no longer be valid in this graph...
            if (nodes.get(from) != null)
                return null;

            if (!to.equals(edge.getTo()))
                edge = edge.cloneWith(from, to);
            else
                edge = edge.cloneReplace(edge.getTo(), to);
        } else if (!from.equals(edge.getFrom())) {
            edge = edge.cloneReplace(edge.getFrom(), from);
        } else {
            edge.setNodeReferences(from, to);
        if (edge == null)
            return null;

        // DEBUG: Should the prior edges.get be removed?
        result = edges.get(edge);
        if (result != null)
            return result;
        }

        nodeEdges.get(from).add(edge);
        nodeEdges.get(to).add(edge);
        edges.put(edge, edge);

        if (createAttributeMaps && edge.getAttributes() == null)
            edge.setAttributes(new HashMap<String, String>(16, 0.9f));

        return edge;
    }

    /**
     * Add attributes to an edge in the graph, including member edges.
     * The given attributes map is added to any existing attributes the
     * given edge has in the graph. In case the edge has no attributes
     * map, a clone of the given map will be assigned to it.
     *
     * <p>This method can also be used to add attributes to edges
     * between group members, so that these attributes take effect
     * at the time of ungrouping (though always overridden by group
     * attributes). Any current attributes present in the group edge
     * that the member edge is part of will also not be added to
     * the member edge attributes, in order to avoid unnecessary
     * duplication.
     *
     * @return The edge stored in this graph, to which attributes were
     * added, or null if no matching edge was found in the graph. If the
     * group edge overrides all of the given attributes in case of
     * a member edge being set, the group edge is returned (as no member
     * edge will then be created).
     */
    public BMEdge addEdgeAttributes (BMEdge edge,
                                     HashMap<String, String> attributes) {
        assert attributes != null : "Null attributes";
        attributes = (HashMap<String, String>)attributes.clone();

        BMEdge e = edges.get(edge);
        if (e != null) {
            addOrSetEdgeAttributes(e, attributes);
            return e;
        }

        BMNode n, o, gn;
        HashMap<String, String> ea;

        e = edge;
        o = edge.getFrom();
        if (nodes.containsKey(o)) {
            n = edge.getTo();
            if (nodes.containsKey(n))
                return null; // Both nodes found, but no such edge
        } else {
            n = o;
            o = edge.getTo();
            if (!nodes.containsKey(o)) {
                gn = groupNodes.get(o);
                if (gn == null) {
                    return null; // One node not found at all -> no such edge
                }
                e = e.cloneReplace(o, gn);
                assert e != null : "Edge clone/replace failed";
            }
        }
        gn = groupNodes.get(n);
        if (gn == null)
            return null; // One node not found at all -> no such edge
        e = edges.get(e.cloneReplace(n, gn));
        if (e == null) 
            return null; // Both nodes found, but no such edge

        // Now e is the corresponding edge in the graph, and at least
        // n is a group member node (o may also be one)

        // Don't override group edge attributes
        ea = e.getAttributes();
        if (ea != null) {
            for (String key : ea.keySet())
                attributes.remove(key);
            if (attributes.isEmpty())
                return e; // Group edge overrides all attributes
        }

        // Add the edge to the member edges list
        edge = edge.cloneWithoutAttributes();
        edge.setAttributes(attributes);
        
        edge = addOrSetMemberEdgeAttributes(n, edge);
        if (groupNodes.containsKey(o))
            edge = addOrSetMemberEdgeAttributes(o, edge);
        
        return edge;
    }

    private BMEdge addOrSetMemberEdgeAttributes (BMNode n, BMEdge edge) {
        ArrayList<BMEdge> el = groupMemberEdges.get(n);
        if (el == null) {
            el = new ArrayList<BMEdge>();
            groupMemberEdges.put(n, el);
        } else {
            int i = el.indexOf(edge);
            if (i >= 0) {
                HashMap<String, String> attributes = edge.getAttributes();
                edge = el.remove(i);
                addOrSetEdgeAttributes(edge, attributes);
            }
        }
        el.add(edge);
        return edge;
    }
        
    /**
     * Get the instance of the given node in this graph.
     * @param node Node to get.
     * @return A reference to the node's instance in this graph, or null if none.
     */
    public BMNode getNode (BMNode node) {
        return nodes.get(node);
    }
    
    /**
     * Get the instance of the given node in this graph.
     * @param nodeId A String representation of a node (type_dbid).
     * @return A reference to the node's instance in this graph, or null if none.
     */
    public BMNode getNode(String nodeId) throws ParseException {
        return nodes.get(new BMNode(nodeId));
    }

    /**
     * Get the instance of the given edge in this graph.
     * @param edge Edge to get.
     * @return A reference to the edge's instance in this graph, or null if none.
     */
    public BMEdge getEdge (BMEdge edge) {
        return edges.get(edge);
    }

    /**
     * Get a given edge from this graph with its direction set so
     * that the given node is its source. Does not work on symmetric
     * edges (they have no distinct source node), and does not
     * alter the edge in the graph.
     * @param edge Edge to get.
     * @param source Desired source node for the edge.
     * @return A reference either to the edge's instance in this graph,
     * or a clone of the edge with its direction inverted so that
     * the given node source is its source.
     */
    public BMEdge getEdgeWithNodeAsSource (BMEdge edge, BMNode source) {
        final BMEdge e = getEdge(edge);
        if (e != null) {
            return (e.getSource().equals(source)) ?
                        e : e.inverted(getReverseType(e.getLinktype()));
        } else {
            return null;
        }
    }
    
    /**
     * Remove the given node (and all its edges) from the graph. This
     * also works on group member nodes, in which case they will simply
     * be removed from the group. If a group becomes completely empty,
     * the group node will also be removed. Meanwhile, removing
     * a group node will remove all of the member nodes as well.
     * @param node Node to remove.
     * @return The instance of the node that was stored in the graph.
     */
    public BMNode removeNode (BMNode node) {
        BMNode othernode;
        LinkedList<BMNode> members;
        ArrayList<BMEdge> ne, te;

        // Remove any group member edges involving this node
        ne = groupMemberEdges.remove(node);
        if (ne != null) {
            for (BMEdge edge : ne) {
                othernode = edge.otherNode(node);
                te = groupMemberEdges.get(othernode);
                if (te != null) {
                    te.remove(edge);
                    if (te.isEmpty())
                        groupMemberEdges.remove(othernode);
                }
            }
        }

        BMNode n = nodes.get(node);
        if (n == null) {
            // Check if the node is a group member
            n = groupNodes.get(node);
            if (n != null) {
                members = groupMembers.get(n);
                assert members != null : "Group node with no members list";
                members.remove(node);
                groupNodes.remove(node);
                if (members.size() == 0)
                    removeNode(n);
            }
            return n;
        }

        // Remove edges in both directions
        ne = nodeEdges.remove(n);
        for (BMEdge edge : ne) {
            othernode = edge.otherNode(n);
            te = nodeEdges.get(othernode);
            if (te != null)
                te.remove(edge);
            edges.remove(edge);
        }

        specialNodes.remove(n);

        // Remove any members if this is a group node
        members = groupMembers.remove(n);
        if (members != null) {
            for (BMNode member : members) {
                groupNodes.remove(member); // unlink from group
                removeNode(member); // remove member edges
            }
        }
        return nodes.remove(n);
    }

    /**
     * Remove the given edge from the graph. Edges connecting to group members
     * may not be removed individually, instead the edge must be removed
     * from the entire group (or the group must be ungrouped first).
     * @param edge Edge to remove.
     * @return The instance of the edge that was stored in the graph.
     */
    public BMEdge removeEdge (BMEdge edge) {
        BMNode t, f;
        ArrayList<BMEdge> l;
        LinkedList<BMNode> tm, fm;

        f = edge.getFrom();
        l = nodeEdges.get(f);
        if (l == null)
            return null;
        l.remove(edge);

        t = edge.getTo();
        l = nodeEdges.get(t);
        assert l != null : "Edge missing from other node's list";
        l.remove(edge);

        // Remove any group member edges if either or both nodes are groups
        tm = groupMembers.get(t);
        fm = groupMembers.get(f);
        if (tm != null || fm != null) {
            String linktype = edge.getLinktype();
            HashSet<BMNode> nodes = new HashSet<BMNode>();
            nodes.add(t);
            if (tm != null)
                nodes.addAll(tm);
            removeMemberEdges(f, fm, nodes, linktype);
            nodes.clear();
            nodes.add(f);
            if (fm != null)
                nodes.addAll(fm);
            removeMemberEdges(t, tm, nodes, linktype);
        }

        return edges.remove(edge);
    }

    private void removeMemberEdges (BMNode group, LinkedList<BMNode> members,
                                    HashSet<BMNode> nodes, String linktype) {
        ArrayList<BMEdge> l;
        BMEdge e;
        BMNode n = group;
        Iterator<BMEdge> eiter;
        Iterator<BMNode> iter = (members == null ? null : members.iterator());
        do {
            l = groupMemberEdges.get(n);
            if (l != null) {
                eiter = l.iterator();
                while (eiter.hasNext()) {
                    e = eiter.next();
                    if (linktype.equals(e.getLinktype())
                            && nodes.contains(e.otherNode(n))) {
                        eiter.remove();
                    }
                }
                if (l.isEmpty())
                    groupMemberEdges.remove(n);
            }
            if (iter == null || !iter.hasNext())
                break;
            n = iter.next();
        } while(true);
    }

    /**
     * Define a reverse name for a linktype. Linktypes should not be
     * redefined once defined, though it will work in most realistic
     * cases. Linktypes should be defined before any edges with those
     * linknames are added to the graph, otherwise invalid reverse names
     * will be generated for them.
     * @param forward The "forward" linktype in the canonical direction.
     * @param reverse The reverse name of the linktype (null to undefine).
     */
    public void defineReverseLinktype (String forward, String reverse) {
        assert forward != null : "Null forward linktype";

        if (reverse == null)
            reverse = ("-"+forward).intern();
        
        if (forward.equals(reverse)) {
            reverse = forward;
        } else {
            canonicalDirections.remove(reverse);
            reverseLinktype.put(reverse, forward);
        }
        canonicalDirections.add(forward);
        reverseLinktype.put(forward, reverse);
    }

    /**
     * Undefine all reverse linktypes (but not symmetric ones).
     * This will revert all non-symmetric reverse names to the default
     * "-linktype" notation.
     */
    public void undefineReverseTypes () {
        String reverse;
        for (String type : canonicalDirections) {
            if (!isSymmetricType(type)) {
                reverse = ("-"+type).intern();
                reverseLinktype.put(type, reverse);
                reverseLinktype.put(reverse, type);
            }
        }
    }

    /**
     * Define a symmetric linktype. A symmetric linktype has no
     * canonical direction and its reverse name is the same as the
     * forward name. Symmetric linktypes must be defined before any
     * edges with that linktype are added to the graph, otherwise
     * invalid reverse names will be constructed for them.
     * @param linktype The symmetric linktype to define.
     */
    public void defineSymmetricLinktype (String linktype) {
        assert linktype != null : "Null linktype";
        canonicalDirections.add(linktype);
        reverseLinktype.put(linktype, linktype);
    }

    /**
     * Get the set of linktypes defined for this graph.
     * The set will contain the linktype names for the canonical
     * direction (or the only direction for symmetric linktypes).
     * The reverse name for each linktype can be obtained by
     * calling getReverseType.
     * @return The set of linktypes (unmodifiable).
     */
    public Set<String> getLinktypeDefinitions () {
        return Collections.<String>unmodifiableSet(canonicalDirections);
    }

    /**
     * Get the canonical name for a given linktype. This will canonize
     * the minus-prefixed reversal notation to the true reverse names.
     * Note that a canonical linktype _name_ is not necessarily one in
     * the canonical _direction_.
     * @param linktype The linktype to canonize.
     * @return The canonized linktype.
     */
    public String getCanonicalType (String linktype) {
        assert linktype != null : "Null linktype";
        String result;

        // Deal with self-negating negations
        linktype = linktype.replaceFirst("^(--)*", "");
        if (linktype.charAt(0) == '-') {
            // Check if there's a reverse name specified
            result = reverseLinktype.get(linktype.substring(1));
            return (result == null ? linktype : result);
        }

        // Do a double reversal to catch cases like when "referred_by"
        // is substituted with "-refers_to" in the lookup map
        result = reverseLinktype.get(linktype);
        if (result == null)
            return linktype;
        result = reverseLinktype.get(result);
        
        return (result == null ? linktype : result);
    }
    
    /**
     * Get the canonical reverse name for a given linktype. The linktype
     * should be canonized first by using getCanonicalType, so to
     * reverse an arbitrary linktype x use something like:
     * reverse = getReverseType(getCanonicalType(x));
     * @param linktype The linktype to reverse.
     * @return The reverse name of linktype.
     */
    public String getReverseType (String linktype) {
        assert linktype != null : "Null linktype";

        String result;
        result = reverseLinktype.get(linktype);
        if (result == null) {
            if (linktype.length() != 0 && linktype.charAt(0) == '-')
                result = linktype.substring(1);
            else
                result = "-"+linktype;
            result = result.intern();
            // Store the result so we don't have to construct the
            // reverse every time it comes up again:
            reverseLinktype.put(result, linktype);
            reverseLinktype.put(linktype, result);
        }
        return result;
    }

    /**
     * Get a human-readable name for the type of a given edge.
     * Most forward edge types are assumed to be human-readable
     * anyhow, but in some cases they may be elaborated with the
     * knowledge of the connected nodes.
     * @param edge The edge.
     * @return Human-readable forward linktype of the edge.
     */
    public String getReadableType (BMEdge edge) {
        if (edge == null)
            return null;
        String s = edge.getLinktype();
        if (s.equals("="))
            return "";
        if (s.equals("has")) {
            String to_type = edge.getTo().getType();
            if (to_type.endsWith("Variant"))
                return "has_variant";
            else if (to_type.endsWith("Function"))
                return "has_function";
            // DEBUG: Should we just construct these from any type name?
        }
        return s;
    }

    /**
     * Get a human-readable reverse name for the type of a given edge.
     * This type is intended for the visualization of reverse linknames
     * only, and may depend on node types.
     * @param edge The edge.
     * @return Human-readable reverse linktype of the edge.
     */
    public String getReadableReverseType (BMEdge edge) {
        if (edge == null)
            return null;
        String reverse = getReverseType(edge.getLinktype());
        if (reverse.charAt(0) != '-')
            return reverse;
        if (reverse.length() == 1)
            return "";

        String s = reverse.substring(1);
        if (REVERSE_LINKTYPES.containsKey(s))
            reverse = REVERSE_LINKTYPES.get(s);

        if (s.equals("has")) {
            String to_type = edge.getTo().getType();
            if (to_type.endsWith("Variant"))
                return "is_variant_of";
            else if (to_type.endsWith("Function"))
                return "is_function_of";
        } else if (s.equals("is_part_of")) {
            String from_type = edge.getFrom().getType();
            if (from_type.endsWith("Site")
                    || from_type.endsWith("Element")
                    || from_type.endsWith("Modification")
                    || from_type.equals("Repeat"))
                return "contains";
        } else if (s.equals("is_found_in")) {
            String to_type = edge.getTo().getType();
            if (to_type.endsWith("Component"))
                return "is_location_of";
        }
        return reverse;
    }

    /**
     * Check if a linktype is symmetric (without a reverse). The
     * linktype should be canonized first by using getCanonicalType.
     * @param linktype Linktype to check.
     * @return True iff the linktype is symmetric (same in both directions).
     */
    public boolean isSymmetricType (String linktype) {
        assert linktype != null : "Null linktype";
        return linktype.equals(reverseLinktype.get(linktype));
    }

    /**
     * Check if a linktype is in the canonical direction. The linktype
     * should be canonized first, as only canonical linktypes may be in
     * the canonical direction. Note that a canonical linktype may also be
     * in the non-canonic direction, e.g. "referred_by". Symmetric
     * linktypes are never in the canonical direction (as there isn't one)!
     * @param linktype Linktype to check.
     * @return True iff the linktype is in a canonical direction.
     */
    public boolean isCanonicalDirection (String linktype) {
        assert linktype != null : "Null linktype";
        if (canonicalDirections.contains(linktype))
            return !(linktype.equals(reverseLinktype.get(linktype)));
        if (linktype.length() > 0)
            return (linktype.charAt(0) != '-');
        return true;
    }

    /**
     * Check if the graph contains a given node. Note that group member
     * nodes are not considered part of the graph as such, they need to
     * be checked separately with "hasGroupMember".
     * @param node The node to query.
     * @return True iff the node is contained in this graph.
     */
    public boolean hasNode (BMNode node) {
        return nodes.containsKey(node);
    }

    /**
     * Check if the graph contains a given edge. Note that edges
     * involving group member nodes cannot be queried directly, even
     * though conceptually the edge may be in the graph.
     * @param edge The edge to query.
     * @return True iff the edge is contained in this graph in the exact form.
     */
    public boolean hasEdge (BMEdge edge) {
        return edges.containsKey(edge);
    }

    /**
     * Check if a node is a "special node" in this graph.
     * Note that the specialness of the node is not a property of the
     * node, but of the instance of the graph.
     * @param node Node to query.
     * @return True iff the node is a special node in this graph.
     */
    public boolean hasSpecialNode (BMNode node) {
        return specialNodes.contains(node);
    }

    /**
     * Check if a node is a group node in this graph.
     * @param node The node to check.
     * @return True iff the node is a group node in this graph.
     */
    public boolean hasGroupNode (BMNode node) {
        return groupMembers.containsKey(node);
    }

    /**
     * Check if the graph contains a given node as group member.
     * @param node The node to query.
     * @return True iff the node is a group member node in this graph.
     */
    public boolean hasGroupMember (BMNode node) {
        return groupNodes.containsKey(node);
    }

    /**
     * Get the group node for a given group member node.
     * @param member A group member node.
     * @return The group node containing the member node, null if none found.
     */
    public BMNode getGroupNodeFor (BMNode member) {
        return groupNodes.get(member);
    }

    /**
     * Get the instance of a given group member node stored in the graph.
     * @param member A group member node.
     * @return The instance of the group member node as stored in the graph.
     */
    public BMNode getGroupMember (BMNode member) {
        BMNode group = groupNodes.get(member);
        if (group == null)
            return null;
        for (BMNode node : groupMembers.get(group)) {
            if (node.equals(member))
                return node;
        }
        assert false : "groupNodes has member but groupMembers doesn't!";
        return null;
    }

    /**
     * Get the list of member nodes for a given group node.
     * @param groupnode A group node.
     * @return The list of member nodes for the given group node (unmodifiable).
     */
    public List<BMNode> getMembersFor (BMNode groupnode) {
        List<BMNode> members = groupMembers.get(groupnode);
        return (members != null) ? Collections.unmodifiableList(members) : null;
    }

    /**
     * Set the "special node" status of a node in this graph.
     * @param node The node to set the special status of.
     * @param special The new special status (true is special).
     */
    public void setSpecial (BMNode node, boolean special) {
        if (special)
            markSpecial(node);
        else
            unmarkSpecial(node);
    }

    /**
     * Mark a node as a "special node" in this graph.
     * @param node The node to make special.
     * @return The instance of the node in this graph, or null if not present.
     */
    public BMNode markSpecial (BMNode node) {
        node = nodes.get(node);
        if (node != null && !specialNodes.contains(node))
            specialNodes.add(node);
        return node;
    }

    /**
     * Unmark a node as a "special node" in this graph. This doesn't
     * ensure that the node appears in the graph in any way, just that
     * it isn't marked as a special node.
     * @param node The node to mark as non-special.
     */
    public void unmarkSpecial (BMNode node) {
        specialNodes.remove(node);
    }

    /** Unmark all "special nodes" in this graph. */ 
    public void clearSpecialNodes() {
        specialNodes.clear();
    }
    
    /**
     * Add a comment to this graph. The comment will have no
     * significance to the implementation in this class, it will
     * simply be stored at the end of the list of comments. Note
     * that there are separate "special comments" intended to
     * be used for storing settings for other packages.
     * @param comment The comment to add.
     */
    public void addComment (String comment) {
        assert comment != null : "Null comment";
        comments.addLast(comment);
    }

    /**
     * Return the size of a node group. For a single node, this will
     * return 1, for a group node this will return the number of nodes
     * in that group (may also be 1 due to ungroupings). If the node is
     * not found in the graph at all, -1 is returned. If the node is in
     * the graph as a group _member_, 0 is returned.
     * @param node The node to check.
     * @return The "size" (in nodes) of a node.
     */
    public int getNodeSize (BMNode node) {
        if (nodes.get(node) == null)
            return (groupNodes.containsKey(node) ? 0 : -1);

        LinkedList<BMNode> members = groupMembers.get(node);
        if (members == null)
            return 1;
        return members.size();
    }

    /**
     * Get a node's degree in the graph.
     * @param node The node whose degree to get.
     * @return The node's degree (defined here as the number of edges).
     */
    public int getDegree (BMNode node) {
        List<BMEdge> ne = nodeEdges.get(node);
        return (ne == null ? 0 : ne.size());
    }

    /**
     * Get a node's degree in the graph, for a given linktype.
     * The degree is defined as the number of edges with the given
     * linktype. As an exception, a linktype of null will match all
     * edges, giving the same result as getDegree(node).
     * @param node The node whose linktype degree to get.
     * @param linktype The linktype of edges that make up the degree.
     * @return The node's degree (the number of edges of linktype).
     */
    public int getDegree (BMNode node, String linktype) {
        return getDegree(node, linktype, null);
    }

    /**
     * Get a node's degree in the graph, for given link and destination types.
     * The degree is defined as the number of edges from the node such
     * that the linktype and type of destination node (i.e. the neigbor
     * connected through that edge) match the given arguments.
     * @param node The node whose linktype degree to get.
     * @param linktype The linktype of edges that make up the degree.
     * @param desttype The type of the neighbor nodes that make up the degree.
     * @return The node's degree (the number of edges of linktype to
     * nodes of desttype).
     */
    public int getDegree (BMNode node, String linktype, String desttype) {
        if (linktype == null && desttype == null)
            return getDegree(node);
        List<BMEdge> ne = nodeEdges.get(node);
        if (ne == null)
            return 0;

        String etype;
        int degree = 0;
        for (BMEdge edge : ne) {
            etype = edge.getLinktype();
            if (linktype == etype || linktype.equals(etype)) {
                if (desttype == null) {
                    ++degree;
                } else {
                    etype = edge.otherNode(node).getType();
                    if (desttype == etype || desttype.equals(etype))
                        ++degree;
                }
            }
        }
        return degree;
    }

    /**
     * Get a node's degree in the graph, of the same type as a given edge.
     * The degree is defined as the number of edges from the node such
     * that the linktype and type of destination node match those of the
     * given edge, which must be an edge of the given node.
     * @param node The node whose linktype degree to get.
     * @param edge An edge of node the degree of which to get.
     * @return The node's degree (the number of edges of linktype to
     * nodes of desttype).
     */
    public int getDegree (BMNode node, BMEdge edge) {
        assert edge != null : "Null edge";
        return getDegree(node, edge.getLinktype(),
                         edge.otherNode(node).getType());
    }

    /**
     * Ungroup the given group member node. The group node will be
     * silently deleted if the group is left empty. The instance of the
     * node stored in the graph will be the one given as argument.
     * Null will be returned if the argument node is not a group member,
     * and no further effect will take place.
     * @param node The group member node to ungroup.
     * @return The instance of the ungrouped member node in this graph.
     */
    public BMNode ungroupMemberNode (BMNode node) {
        ArrayList<BMEdge> el, te;
        HashMap<String, String> attributes;

        // Remove the membernode in both directions
        BMNode groupnode = groupNodes.remove(node);
        if (groupnode == null)
            return null;
        groupMembers.get(groupnode).remove(node);

        // Add the ex-member as a regular node
        node = ensureHasNode(node);

        // Copy (additional) attributes from the groupnode
        attributes = groupnode.getAttributes();
        if (attributes != null) {
            if (node.getAttributes() == null)
                node.setAttributes((HashMap<String, String>)attributes.clone());
            else
                node.putAll(attributes);
        }

        // Transfer the "special" status from the group node
        if (hasSpecialNode(groupnode))
            markSpecial(node);

        // Copy edges from the groupnode
        el = (ArrayList<BMEdge>)nodeEdges.get(groupnode).clone();
        for (BMEdge groupedge : el) {
            BMEdge edge = groupedge.cloneReplace(groupnode, node);
            edge = ensureHasEdge(edge);
            assert edge != null : "Failed to clone group edge for member";
        }

        // Copy attributes from old member edges (and remove them)
        el = groupMemberEdges.remove(node);
        if (el != null) {
            BMNode othernode;
            BMEdge e;
            for (BMEdge edge : el) {
                e = getEdge(edge);
                // Remove the edge record from the other node, unless the other
                // node is a group member node (in which case leave the
                // edge in the other node's record until THAT node is ungrouped)
                if (e != null || !hasGroupMember(edge.otherNode(node))) {
                    othernode = edge.otherNode(node);
                    te = groupMemberEdges.get(othernode);
                    if (te != null) {
                        te.remove(edge);
                        if (te.isEmpty())
                            groupMemberEdges.remove(othernode);
                    }
                }
                // Copy attributes but give priority to group node attributes
                if (e != null) {
                    attributes = edge.getAttributes();
                    if (attributes != null) {
                        HashMap<String, String> ga = e.getAttributes();
                        e.setAttributes(attributes);
                        if (ga != null)
                            e.putAll(ga);
                    }
                }
            }
        }

        // Remove the group if it is now empty
        if (groupMembers.get(groupnode).size() == 0)
            removeNode(groupnode);

        return node;
    }

    /**
     * Ungroup the given group node into individual nodes. Note that
     * the group node will be removed in the process of ungrouping (only
     * the member nodes will remain of it).
     * @param groupnode The group node to ungroup.
     * @return The number of member nodes that were ungrouped.
     */
    public int ungroup (BMNode groupnode) {
        int count = 0;
        LinkedList<BMNode> members = groupMembers.get(groupnode);
        if (members == null)
            return 0;

        while (!members.isEmpty()) {
            ++count;
            ungroupMemberNode(members.getFirst());
        }

        assert count > 0 : "Group with no members?";
        assert groupMembers.get(groupnode) == null : "Ungrouping failed?";

        return count;
    }

    /**
     * Ungroup all group nodes in this graph. Note that the group nodes
     * will be removed in the process of ungrouping (only the member
     * nodes will remain of them).
     * @return The change in the total number of nodes in the graph.
     */
    public int ungroupAll () {
        int count = 0;
        ArrayList<BMNode> groupnodes;
        
        groupnodes = new ArrayList<BMNode>(groupMembers.keySet());
        for (BMNode groupnode : groupnodes)
            count += ungroup(groupnode) - 1;

        return count;
    }

    /**
     * Define a group node in the graph. The node must not already
     * be a group node in the graph, and none of its members can already
     * exist in the graph. There must be at least one member, since empty
     * groups aren't permitted. Null will be returned in case of violations
     * to the above.
     * @param groupnode The group node to put.
     * @param members The list of group members.
     * @return The instance of the group node in the graph.
     */
    public BMNode defineGroup (BMNode groupnode, List<BMNode> members) {
        assert members != null : "Null members";
        if (members.size() == 0)
            return null;

        // Check for conflicts
        if (hasGroupNode(groupnode) || hasGroupMember(groupnode))
            return null;
        for (BMNode member : members) {
            if (hasNode(member) || hasGroupMember(member))
                return null;
        }

        groupnode = ensureHasNode(groupnode);
        groupMembers.put(groupnode, new LinkedList<BMNode>(members));
        members = groupMembers.get(groupnode);
        assert members != null : "Failed to create members list";

        for (BMNode member : members) {
            // DEBUG: Should the createAttributeMaps boolean include members?
            //if (createAttributeMaps && member.getAttributes() == null)
            //    member.setAttributes(new HashMap<String, String>(8, 0.9f));
            groupNodes.put(member, groupnode);
        }

        return groupnode;
    }

    /**
     * Get the set of nodes in this graph.
     * @return The set of nodes (unmodifiable, but node attributes may
     * be modified).
     */
    public Set<BMNode> getNodes () {
        return Collections.<BMNode>unmodifiableSet(nodes.keySet());
    }

    /**
     * Get the set of edges in this graph.
     * @return The set of edges (unmodifiable, but edge attributes may
     * be modified).
     */
    public Set<BMEdge> getEdges () {
        return Collections.<BMEdge>unmodifiableSet(edges.keySet());
    }

    /**
     * Get the number of edges in this graph. Edges of group nodes are
     * counted as single edges.
     * @return The number of edges in this graph.
     */
    public int numEdges () {
        return edges.size();
    }

    /**
     * Get the number of nodes in this graph. Group nodes are counted as
     * single nodes.
     * @return The number of nodes in this graph.
     */
    public int numNodes () {
        return nodes.size();
    }

    /**
     * Get the set of special nodes in this graph.
     * @return The set of special nodes in this graph (unmodifiable, but
     * node attributes may be modified).
     */
    public Set<BMNode> getSpecialNodes () {
        return Collections.<BMNode>unmodifiableSet(specialNodes);
    }

    /**
     * Get the set of group nodes in this graph.
     * @return The set of group nodes in this graph (unmodifiable, but
     * node attributes may be modified).
     */
    public Set<BMNode> getGroupNodes () {
        return Collections.<BMNode>unmodifiableSet(groupMembers.keySet());
    }

    /**
     * Get the set of group member nodes in this graph.
     * @return The set of group member nodes in this graph (unmodifiable,
     * but node attributes may be modified).
     */
    public Set<BMNode> getGroupMembers () {
        return Collections.<BMNode>unmodifiableSet(groupNodes.keySet());
    }

    /**
     * Get the set of group member edges in this graph.
     * Group member edges are used to retain edge attributes for
     * individual edges, for possible future ungrouping. They do not
     * override group edge attributes, except in cases where the
     * attribute is intentionally deleted from the group edge but
     * remains in the member edges.
     * 
     * <p>Note that calling this method goes through the entire list of
     * group member edges and constructs a new set for them, which is
     * then returned. This is therefore not a simple accessor method,
     * and should be used sparingly. However, as a side effect it cleans
     * up the list of member edges, removing obsolete entries which may
     * be left there in some cases after the corresponding group edge
     * has been removed.
     * @return The set of group member edges in this graph (newly
     * allocated, doesn't affect the different structure stored in the
     * graph, but edge attributes may be modified).
     */
    public TreeSet<BMEdge> getGroupMemberEdges () {
        TreeSet<BMEdge> memberEdges = new TreeSet<BMEdge>();
        boolean removedAll;
        BMEdge edge;
        BMNode node, t, f;
        Iterator<BMNode> iter = groupMemberEdges.keySet().iterator();
        Iterator<BMEdge> eiter;
        HashMap<String, String> attributes;
        while (iter.hasNext()) {
            node = iter.next();
            eiter = groupMemberEdges.get(node).iterator();
            removedAll = true;
            while (eiter.hasNext()) {
                edge = eiter.next();
                t = edge.getTo();
                f = edge.getFrom();
                attributes = edge.getAttributes();
                if (attributes != null && !attributes.isEmpty() &&
                    ((groupNodes.containsKey(t) && (nodes.containsKey(f)
                                            || groupNodes.containsKey(f)))
                    || (groupNodes.containsKey(f) && nodes.containsKey(t)))) {
                    removedAll = false;
                    memberEdges.add(edge);
                } else {
                    eiter.remove();
                }
            }
            if (removedAll)
                iter.remove();
        }
        return memberEdges;
    }

    /**
     * Get the list of edges connected to a given node.
     * 
     * Note that the edges may be represented in either direction
     * (there shall only be one BMEdge instance for each logical edge!).
     * 
     * @param node The node to query.
     * @return A list of the node's edges.
     */
    public List<BMEdge> getNodeEdges (BMNode node) {
        return Collections.<BMEdge>unmodifiableList(nodeEdges.get(node));
    }

    /**
     * Get a list of neighbors for a given node.
     * The list is created just for this purpose and can be modified at
     * will, but doing so has no effect on the BMGraph. The nodes in
     * the list are the actual instances stored in the graph, so there
     * is no need to use "getNode" on them.
     * @param node The node whose neighbors to get.
     * @return A new list of the node's neighbors.
     */
    public List<BMNode> getNeighbors (BMNode node) {
        List<BMEdge>edges = nodeEdges.get(node);
        if (edges == null)
            return null;
        ArrayList<BMNode> neighbors = new ArrayList<BMNode>();
        for (BMEdge edge : edges)
            neighbors.add(getNode(edge.otherNode(node)));
        return neighbors;
    }

    /**
     * Get the linked list of comments for this graph.
     * Note that the list does not include "special comments".
     * @return A linked list of comments for this graph (can be modified freely).
     */
    public LinkedList<String> getComments () {
        return comments;
    }

    /**
     * Get the database array for this graph.
     * (NOTE: This should be removed in favour of a more general
     * metadata solution.)
     *
     * The array is always four elements long, but any of the elements
     * may be null (although typically either all or none are). The
     * elements are (in order):
     *
     * <ol><li>database name (e.g. "biomine")<li>
     *     <li>database version (e.g. "v3_2_beta")</li>
     *     <li>database server hostname</li>
     *     <li>database node viewer URL</li>
     *  </ol>
     *
     *  <p>The array may be modified at will, this metadata is not
     *  internally used by this BMGraph implementation (although
     *  other classes may use it).
     * @return A four-element array of database, version, server and URL.
     */
    public String[] getDatabaseArray () {
        return database;
    }

    public URL getNodeExpandURL () {
        return nodeExpandURL;
    }

    public void setNodeExpandURL (URL nodeExpandURL) {
        this.nodeExpandURL = nodeExpandURL;
    }


    public String getNodeExpandProgram () {
        return nodeExpandProgram;
    }

    public void setNodeExpandProgram (String nodeExpandProgram) {
        this.nodeExpandProgram = nodeExpandProgram;
    }

        
    /**
     * Add a new value for a given special comment key (or "type").
     * The (exact) same value is not added multiple times for a single
     * key (type of special comment), but no error is returned if this
     * is attempted (the previously added one just stays in).
     *
     * <p>Note that empty values (length == 0) are not added, but the
     * Set is still returned (possibly empty).
     * 
     * @param key The type of special comment.
     * @param value The data associated with the comment.
     * @return The Set of String values (possibly empty) associated with key.
     */
    public Set<String> addSpecialComment (String key, String value) {
        assert key != null : "Null key";
        assert value != null : "Null value";

        Object val = specialComments.get(key);
        Set<String> set = null;
        
        if (val == null) {
            set = new HashSet<String>(4);
            specialComments.put(key, set);
        } else if (val instanceof Map) {
            // the special comment has been interpreted as a Map;
            // convert back to Set before modifying...
            Map<String,String> map = (Map<String,String>)val; 
            set = specialCommentMapToSet(map);
            specialComments.put(key, set);
        } else if (val instanceof Set) {
            set = (Set<String>)val;
        } else {
            throw new RuntimeException("Invalid representation class for special"+
                    " comment "+key+": "+val.getClass().getName());
        }
                
        if (value.length() != 0) {            
            set.add(value);
        }
        return set;
    
    }
    
    /**
     * Put a key-value pair under a given special comment. If the special 
     * comment already has some value(s), is assumed that the set of values
     * only contains entries that are key-value pairs like key=val; unless this 
     * is the case, a ParseException is thrown. 
     * 
     * Results in a line like the following in the bmgraph file:  
     *    # _commentName key=value
     * Other key-value pairs for the same commentName are stored on separate lines.
     */
    public void putSpecialComment (String commentName, String key, String value) throws ParseException {
        assert commentName != null : "Null comment name";
        assert key != null : "Null key";
        assert value != null : "Null value";
        
        Map<String,String> map = getSpecialCommentAsMap(commentName);
        if (map == null) {            
            map = new HashMap(4);            
            specialComments.put(commentName, map);
        }
        map.put(key, value);
    
    }
    
    /**
     * Transforms a special comment stored as a map back into the set 
     * representation. It is not generally recommended to go back to the
     * set representation once map representation has been requested.
     */
    private static Set<String> specialCommentMapToSet(Map<String,String> map) {
        Set<String> set = new HashSet<String>(map.size());     
        for (String key: map.keySet()) {
            String val = map.get(key);
            set.add(key+"="+val);
        }
        return set;
    }
    
    /**
     * By default, special comments are stored as a set. If requested, the
     * representation is changed into a map on demand, an operation performed 
     * using this method.
     */
    private static Map<String,String> specialCommentSetToMap(Set<String> set,
                                                            String commentName) throws ParseException {
        Map<String,String> map= new HashMap<String,String>(set.size());     
        for (String entry: set) {
            int i = entry.indexOf('=');
            if (i != -1) {
                String key = entry.substring(0, i);
                String val = entry.substring(i+1);
                map.put(key, val);
            }
            else {
                // '=' not found
                System.err.println("Warning: unparsable attribute-value pair: "+
                                   "\""+entry+"\" (in special comment: "+commentName);
            }            
        }
        return map;
    }

    /**
     * Set the given special comment to _only_ contain the given value.
     * This is the same as clearing the set of values for the given
     * special comment and then setting the given value. Same
     * notes apply as to addSpecialComment(key, value).
     * 
     * If the special comment is represented as a map, the map is just disposed
     * of. Currently, this generates no warning, although the situation
     * is not likely in any sensible situation.
     * 
     * @param key The type of special comment.
     * @param value The data associated with the comment.
     * @return The Set of String values associated with key.
     */
    public Set<String> setSpecialComment (String key, String value) {
        assert key != null : "Null key";
        assert value != null : "Null value";
             
        Object commentValue = specialComments.get(key);
        Set<String> values;
        if (commentValue == null || !(commentValue instanceof Set)) {
            // TODO: generate a warning about overriding an existing Map?
            values = new HashSet<String>(4);            
            specialComments.put(key, values);
        } else {
            values = (Set<String>)commentValue;
            values.clear();
        }     
        
        if (value.length() != 0) {
            values.add(value);
        }
        return values;
    }

    /**
     * Ensure that the Set of values exists for a special comment type.
     * This can be used to add a special comment with no values. This is
     * the same as calling the addSpecialComment method with the empty
     * string as value. This can be used to ensure that the Set of values
     * exists, even if one does not desire to set any value.
     * @param key The type of special comment.
     * @return The Set of String values associated with key (possibly empty).
     */
    public Set<String> ensureHasSpecialComment (String key) {
        return addSpecialComment(key, "");
    }

    /**
     * Return the Set of values for a given special comment.
     * Note that the returned set may be null (not set) or empty
     * (special comment is present, but has no values set).
     * @param key The type of special comment.
     * @return The Set of values for key, or null if none.
     */
    public Set<String> getSpecialComment (String key) {                
        
        assert key != null : "Null key";

        Object commentVal = specialComments.get(key);
        
        if (commentVal == null) {
            return null;            
        } else if (commentVal instanceof Set) {
            return (Set<String>)commentVal;
        } else if (commentVal instanceof Map) {
            return specialCommentMapToSet((Map)commentVal);
        } else {
            throw new RuntimeException("Invalid representation class for special"+
                                       " comment "+key+": "+commentVal.getClass().getName());
        }                
    }
    
    /**
     * Get a special comment as a map. Create an empty map if the special comment
     * does not exist yet.
     * 
     * @throws ParseException When the special comment contains values that are
     * not parsable as key-value pairs of form "key=val". 
     */
    public Map<String,String> getSpecialCommentAsMap (String key) throws ParseException {
        assert key != null : "Null key";

        Object commentVal = specialComments.get(key);
        
        if (commentVal == null) {
            Map<String, String> map = new HashMap(4);
            specialComments.put(key, map);
            return map;
        } else if (commentVal instanceof Set) {
            // convert to map (and cache the map)
            Map<String,String> map = specialCommentSetToMap((Set<String>)commentVal, key);
            specialComments.put(key, map);
            return map;
        } else if (commentVal instanceof Map) {
            return (Map<String,String>)commentVal;
        } else {
            throw new RuntimeException("Invalid representation class for special"+
                                       " comment "+key+": "+commentVal.getClass().getName());
        }                
    }
    
    /**
     * Return the special comments as a Collection of Strings.
     * These strings are of the format "_"+key+" "+value,
     * (or "_"+key+" "+key2+" "+value, in the case of map-valued special
     * comments), constructed from the map of special comments.
     * 
     * @return A Collection of String representations of special comments.
     */
    public Collection<String> specialCommentsToStrings () {        
        
        TreeSet<String> c = new TreeSet<String>();
        String key;
        for (Map.Entry<String, Object> entry : specialComments.entrySet()) {            
            key = "_"+entry.getKey()+" ";
            Object setOrMap = entry.getValue();
            if (setOrMap instanceof Set) {
                for (String value : (Set<String>)setOrMap) {
                    c.add(key+value);
                }
            } else if (setOrMap instanceof Map) {                
                for (Map.Entry<String, String> entry2: ((Map<String,String>)setOrMap).entrySet()) {
                    c.add(key+entry2.getKey()+"="+entry2.getValue());
                    if (entry2.getValue().charAt(0)=='=') {
                        System.err.println("Warning: equals sign at beginning of val; key="+key+",entrykey="+entry2.getKey()+",entryval="+entry2.getValue());
                    }
//                    if (entry2.getKey().charAt(entry2.getKey().length()-1)=='=') {
//                        System.err.println("Warning: equals sign at end of key: "+key+entry2.getKey()+"="+entry2.getValue());
//                    }
                }
            }
        }
        return c;
    }

    private void addOrSetEdgeAttributes (BMEdge edge,
                                         HashMap<String, String> attributes) {
        if (edge.getAttributes() == null)
            edge.setAttributes(attributes);
        else
            edge.putAll(attributes);
    }

}
