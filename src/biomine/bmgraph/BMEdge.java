package biomine.bmgraph;

import java.util.HashMap;

/**
 * BMGraph edge. Each edge connects two distinct nodes and has
 * a specific linktype. Each linktype is either symmetric (same in both
 * directions) or has a single canonical direction. Each edge will be
 * created so that the "from" and "to" nodes connected by the edge are
 * in the canonical order for the linktype, and the linktype of that edge
 * is set in that canonical direction. An additional reverse linktype
 * may be specified for edges if directional information is to be
 * stored; however, the reverse linktype is only used for output
 * purposes (visualization, etc).
 *
 * <p>For symmetric linktypes, the reverse linktype must be specified
 * as equal to the forward linktype.
 *
 * <p>An edge may also have an attached set of attributes, but this set (or
 * its presence/absence) will not affect the equality or comparison of
 * nodes, but is merely an instance-specific set of metadata. Edge
 * identity is only affected by the connected nodes and the linktype
 * (which is case-sensitive), even the direction of the edge is assumed
 * to be relevant only for visualization purposes and not the concept
 * represented by the edge.
 *
 * <p>Empty linktypes are also supported.
 *
 * @author Kimmo Kulovesi
 */

public class BMEdge extends BMEntity implements Comparable<BMEdge> {
    /**
     * The string which is used to represent "no linktype" in the
     * toString methods.
     */
    public static final String NO_LINKTYPE = "+";

    private int hashCode;
    private BMNode from;
    private BMNode to;
    private String linktype;
    private String reversed;

    /**
     * Create an edge between two nodes, with linktype and attributes.
     * NOTE: Edges must be created in the canonical direction for the
     * linktype, i.e. if the edge is "x referred_by y", then the edge
     * should be created as "y refers_to x" and "referred_by" as the
     * reversed linktype name. The reversed linktype name should be
     * given if and only if the nodes were reversed in order to obtain
     * the canonical direction (like in the above example). For
     * symmetric linktypes the "reversed" name (same as linktype) should
     * always be given regardless of node order.
     *
     * @param from The node from which the edge starts.
     * @param to The node to which the edge goes.
     * @param linktype Type of the link represented by the edge.
     * @param attributes Edge attributes (optional).
     * @param reversed Reversed linkname IFF the edge is reversed.
     */
    public BMEdge (BMNode from, BMNode to, String linktype,
                   HashMap<String, String> attributes, String reversed) {
        super();

        assert from != null : "Null from-node";
        assert to != null : "Null to-node";
        //assert !from.equals(to) : "From and to nodes are the same";

        if (linktype == null || linktype.length() < 1)
            linktype = "";
        if (reversed != null && reversed.length() < 1)
            reversed = "";
        
        this.from = from;
        this.to = to;
        this.linktype = linktype;
        this.reversed = reversed;
        this.attributes = attributes;

        // Put nodes in sort order for symmetric links
        if (reversed != null && reversed.equals(linktype)) {
            this.reversed = linktype; // Ensure reversed is the same object
            if (from.compareTo(to) > 0) {
                // Sort node order
                this.from = to;
                this.to = from;
            }
        }
        hashCode = (from.hashCode() ^ to.hashCode());
    }

    /**
     * Shorthand for creating an edge already in canonical direction.
     * Note that this must not be used for symmetric linktypes, since
     * they do not have a canonical direction as such. (The underlying
     * implementation uses node sort order for symmetric linktypes.)
     */
    public BMEdge (BMNode from, BMNode to, String linktype,
                   HashMap<String, String> attributes) {
        this(from, to, linktype, attributes, null);
    }

    /**
     * Shorthand for creating a canonical edge without attributes.
     * Note that this must not be used for symmetric linktypes, since
     * they do not have a canonical direction as such.
     */
    public BMEdge (BMNode from, BMNode to, String linktype) {
        this(from, to, linktype, null, null);
    }

    /**
     * Set the referred from and to nodes to other EQUAL objects.
     * This is used to ensure the edge refers to specific objects
     * describing the same node. The nodes MUST be equals-same with
     * the original from and to nodes of this edge. The intended
     * function is to substitute some canonical object that describes
     * the attributes for the current edge, but a better way to
     * obtain said reference is to call "getNode" on the BMGraph.
     * @param from The new from-node, must equal the existing one.
     * @param to The new to-node, must equal the existing one.
     */
    public void setNodeReferences (BMNode from, BMNode to) {
        if (from.equals(this.from)) {
            assert to.equals(this.to) : "Substituting non-equal to-node";
            this.from = from;
            this.to = to;
        } else {
            // Maintain internal node order if reversed in parameters
            assert from.equals(this.to) : "Substituting non-equal from-node";
            assert to.equals(this.from) : "Substituting non-equal to-node";
            this.to = from;
            this.from = to;
        }
    }

    /**
     * Clone this edge, but replace one node with another. Any
     * attributes map will also be cloned (using clone()). 
     * @param original Node to replace.
     * @param replacement Replacement node.
     * @return A clone of this edge, with "original" node replaced.
     */
    public BMEdge cloneReplace (BMNode original, BMNode replacement) {
        BMNode clone_from = (this.from == original ? replacement : this.from);
        BMNode clone_to = (this.to == original ? replacement : this.to);

        return new BMEdge(clone_from, clone_to, linktype, (attributes == null ?
                          null : (HashMap<String, String>)attributes.clone()),
                          reversed);
    }

    /**
     * Clone this edge but replace both from- and to-nodes with others.
     * @param from The from-node for the clone (substituting the getFrom() node).
     * @param to The to-node for the clone (substituting the getTo() node).
     * @return A clone of this edge with from- and to-nodes replaced.
     */
    public BMEdge cloneWith (BMNode from, BMNode to) {
        return new BMEdge(from, to, linktype, (attributes == null ?
                          null : (HashMap<String, String>)attributes.clone()),
                          reversed);
    }

    /**
     * Return this edge with no reverse-direction information. If this
     * edge is not reversed to begin with (isReversed() == false), then
     * this edge is returned. If the edge is reversed, then a clone of
     * this edge is returned, but without the reversal information.
     * For symmetric linktypes, the "canonical direction" is here
     * intended as the direction where from and to nodes are sorted.
     * @return This edge in the canonical direction.
     */
    public BMEdge canonicalDirection () {
        if (!isReversed())
            return this;
        return new BMEdge(from, to, linktype, (attributes == null ?
                          null : (HashMap<String, String>)attributes.clone()));
    }

    /**
     * Return this edge in the non-canonical direction. If this edge is
     * already in the non-canonical direction (isReversed() == true) or
     * symmetric, then this edge is returned. Otherwise a reversed
     * clone of this edge is returned with the argument as the reverse
     * linktype.
     * @param reversetype Reverse name for this edge's linktype.
     * @return This edge in the non-canonical direction.
     */
    public BMEdge reverseDirection (String reversetype) {
        if (isSymmetric() || isReversed())
            return this;
        if (reversetype != null && reversetype.length() < 1)
            reversetype = "";
        return new BMEdge(from, to, linktype, (attributes == null ?
                          null : (HashMap<String, String>)attributes.clone()),
                          reversetype);
                          
    }

    /**
     * Return this edge in inverted direction. Note that in contrast to
     * reverseDirection(), this always switches the direction for
     * directed edges. The edge is returned unchanged if it is symmetric.
     * @param reversetype Reverse name for this edge's linktype.
     * @return This edge with the direction inverted (if a directed edge).
     */
    public BMEdge inverted (String reversetype) {
        if (isSymmetric())
            return this;
        return isReversed() ? canonicalDirection() :
                              reverseDirection(reversetype);
    }

    /**
     * Clone this edge. Any attributes map will also be cloned.
     * @return A clone of this edge.
     */
    public BMEdge clone () {
        return cloneWith(this.from, this.to);
    }

    /**
     * Clone this edge, but don't clone the attributes. The attributes
     * map will be left empty.
     * @return A clone of this edge, but with an empty attributes map.
     */
    public BMEdge cloneWithoutAttributes () {
        return new BMEdge(from, to, linktype, null, reversed);
    }

    /**
     * Compare two edges for equality, using nodes and linktype only.
     * Note that attributes and edge direction do NOT affect edge
     * equality. (As long as all edges are created in the canonical
     * direction for each linktype.)
     * @param other The other edge.
     * @return True iff the edges have equal nodes and linktype, false otherwise.
     */
    public boolean equals (BMEdge other) {
        if (other == this)
            return true;
        if (other == null)
            return false;
        return from.equals(other.getFrom()) && to.equals(other.getTo())
               && linktype.equals(other.getLinktype());
    }

    public boolean equals (Object other) {
        if (other instanceof BMEdge)
            return this.equals((BMEdge) other);
        return false;
    }

    /**
     * Compare the order of two edges, using nodes and linktype only.
     * Note that attributes and edge direction do NOT affect edge
     * order. (As long as all edges are created in the canonical
     * direction for each linktype.)
     */
    public int compareTo (BMEdge other) {
        if (other == this)
            return 0;
        int result = other.getFrom().compareTo(from);
        if (result != 0)
            return -result;
        result = other.getTo().compareTo(to);
        if (result != 0)
            return -result;
        result = other.getLinktype().compareTo(linktype);
        return -result;
    }

    /**
     * Get the hashcode for this edge, based on its nodes and linktype.
     * @return The hashcode.
     */
    public int hashCode () {
        return hashCode;
    }

    /**
     * Get the originating ("from") node of this edge.
     * Note that this treats the edge in the canonical direction.
     * @return The "from" node.
     */
    public BMNode getFrom () {
        return from;
    }

    /**
     * Get the destination ("to") node of this edge.
     * Note that this treats the edge in the canonical direction.
     * @return The "to" node.
     */
    public BMNode getTo () {
        return to;
    }

    /**
     * Get the source node of this edge, taking direction into account.
     * Unlike getFrom(), this considers the fact that the edge may be
     * reversed.
     * @return The source node.
     */
    public BMNode getSource () {
        return isReversed() ? to : from;
    }

    /* Get the target node of this edge, taking direction into account.
     * Unlike getTo(), this considers the fact that the edge may be
     * reversed.
     * @return The target node.
     */
    public BMNode getTarget () {
        return isReversed() ? from : to;
    }

    /**
     * Get the linktype for this edge in the canonical direction.
     * @return The canonical linktype.
     */
    public String getLinktype () {
        return linktype;
    }

    /**
     * Get the reverse linktype for this edge (if any).
     * Note that this should very seldom be used, it is by far
     * preferable to call getReverseType on the graph that the edge is
     * part of, since the reverse type may have been redefined in that
     * graph.
     * @return Null iff isReversed() returns false, reverse linktype otherwise.
     */
    public String getReverseType () {
        if (reversed == linktype)
            return null;
        return reversed;
    }

    /**
     * Check if the edge has a given node (either as to- or from-node).
     * @param node The node to check for.
     * @return True iff the given node is either the to- or from-node.
     */
    public boolean hasNode (BMNode node) {
        return (from.equals(node) || to.equals(node));
    }

    /**
     * Check if the edge was originally in a non-canonical direction.
     * The internal representation is always in the canonical direction,
     * so this will return true for edges that weren't originally such,
     * as they will have been reversed for this representation.
     * Symmetric edges are never considered "reversed".
     * @return True iff this edge is not originally in canonical direction.
     */
    public boolean isReversed () {
        return (reversed != null && reversed != linktype);
    }

    /**
     * Check if the edge was created with a symmetric linktype.
     * @return True iff this edge was created with a symmetric linktype.
     */
    public boolean isSymmetric () {
        return (reversed == linktype);
    }

    /**
     * Given one node that is linked by this edge, get the other node.
     * @param node Either one of this edge's nodes.
     * @return The other node.
     */
    public BMNode otherNode (BMNode node) {
        return (from.equals(node) ? to : from);
    }

    /**
     * Convert the edge to a string as "from_node to_node link_name".
     * Note that the edge is converted to a string in the original
     * direction, i.e. not necessarily the internal, canonical
     * representation. Knowledge of the original direction requires
     * that the reverse linktype be specified when constructing the
     * edge.
     * @return A string representation of this edge.
     */
    public String toString () {
        if (reversed != null && reversed != linktype) {
            return to+" "+from+" "+(reversed != "" ? reversed : NO_LINKTYPE);
        } else {
            return from+" "+to+" "+(linktype != "" ? linktype : NO_LINKTYPE);
        }
    }

    /**
     * Convert the edge to a string in the canonical direction.
     * Unlike the common "toString", this version shows the internal,
     * canonical edge direction and ignores any information of the
     * original (possibly reversed) edge direction.
     * @return A string representation of the canonical edge.
     */
    public String toCanonicalString () {
        return from+" "+to+" "+(linktype != "" ? linktype : NO_LINKTYPE);
    }

    /**
     * Set the attributes map of this edge. Edges never create their own
     * attributes maps, and edge attributes may be set only on edges for
     * which the map has been set in the constructor or by calling this
     * method. The map may be removed by calling this with a null argument.
     * @param attributes The attributes map for this edge.
     */
    public void setAttributes (HashMap<String, String> attributes) {
        this.attributes = attributes;
    }

}
