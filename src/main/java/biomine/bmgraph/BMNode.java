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

import biomine.bmgraph.read.BMGraphToken;
import java.text.ParseException;
import java.util.HashMap;

/**
 * BMGraph node. Each node has a type and an id, which are used to
 * uniquely identify the node. Both type and id are case-sensitive.
 * A node may also have a set of attributes attached to it, but the set
 * of attributes for each instance does not affect equality or comparison
 * of nodes, it is merely an instance-specific set of metadata.
 *
 * <p>Biomine nodes generally have "database:id" as the node id. One should
 * take care to note that this class treats such database-id -combinations
 * as the node id, and so "node.getId()" refers to the combination and not
 * just the "id" part. "splitId()" can be used to split the id into the
 * components, but otherwise this implementation is oblivious to this
 * special id syntax.
 *
 * @author Kimmo Kulovesi
 */

public class BMNode extends BMEntity implements Comparable<BMNode> {
    private int hashCode;
    private String type;
    private String dbid;

    /**
     * Create a node with type and id given as strings.
     * @param nodeType Node's type.
     * @param nodeId Node's id.
     */
    public BMNode (String nodeType, String nodeId) {
        super();

        assert nodeType != null : "Null type";
        assert nodeId != null : "Null ID";

        type = nodeType;
        dbid = nodeId;
        attributes = null;
        hashCode = type.hashCode() ^ dbid.hashCode();
    }

    /**
     * Create a node from a single string representation.
     * @param nodeString Node as a String of the form "type_nodeid".
     */ 
    public BMNode(String nodeString) throws java.text.ParseException {
        int i = nodeString.indexOf("_");
        if (i == -1) {
            throw new ParseException("Unparseable id for a BMNode: "+nodeString,
                                     0);
        }
        type = nodeString.substring(0, i);
        dbid = nodeString.substring(i + 1);
        attributes = null;
        hashCode = type.hashCode() ^ dbid.hashCode();        
    }
    
    /**
     * Create a node from a parser token.
     * @param nodeToken A token containing attributes "type" and "dbid".
     */
    public BMNode (BMGraphToken nodeToken) {
        super();

        assert nodeToken != null : "Null token";

        type = nodeToken.get("type");
        assert type != null : "No type attribute in node token";
        dbid = nodeToken.get("dbid");
        assert dbid != null : "No dbid attribute in node token";
        attributes = null;
        hashCode = type.hashCode() ^ dbid.hashCode();
    }

    /**
     * Clone this node and any attributes map it may have.
     * @return A clone of this node.
     */
    @Override
    public BMNode clone () {
        BMNode clone = new BMNode(type, dbid);
        if (attributes != null)
            clone.setAttributes((HashMap<String,String>)(attributes.clone()));
        return clone;
    }

    /**
     * Get the type of this node.     * 
     */
    public String getType () {
        return type;
    }

    /**
     * Get the id of this node.
     * @return The node's id
     */
    public String getId () {
        return dbid;
    }
    
    /**
     * Get the db and id parts of a "db:id"-formatted node id separately.
     * @return Null if parsing failed, or "db" and "id" at indices 0 and 1.
     */
    public String[] splitId () {
        int comma = dbid.indexOf(':');
        if (comma <= 0)
            return null;
        String dbpart = dbid.substring(0, comma);
        if (dbpart == null || dbpart.length() < 1)
            return null;
        String idpart = dbid.substring(comma + 1);
        if (idpart == null || idpart.length() < 1)
            return null;
        String[] result = new String[2];
        result[0] = dbpart;
        result[1] = idpart;
        return result;
    }

    /**
     * Convert the node to a string of format "Type_id".
     * @return Node type and id as a string.
     */
    @Override
    public String toString () {
        return type+"_"+dbid;
    }

    /**
     * Compare node for equality using type and id. Note that attributes
     * do NOT affect node equality.
     * @param other Other node.
     * @return True if nodes have the same type and id, false otherwise.
     */
    public boolean equals (BMNode other) {
        if (other == this)
            return true;
        if (other == null)
            return false;
        return (type.equals(other.getType()) && dbid.equals(other.getId()));
    }

    @Override
    public boolean equals (Object other) {
        if (other instanceof BMNode)
            return this.equals((BMNode)other);
        return false;
    }

    /**
     * Compare node using type and id. Note that attributes do NOT
     * affect the result.
     * @param other The other node to compare to.
     * @return A negative integer, zero, or a positive integer if this
     * node is less than, equal to, or greater than the other node,
     * respectively, according to their lexicographical order based on
     * type and id.
     */
    @Override
    public int compareTo (BMNode other) {
        if (other == this)
            return 0;

        int result = type.compareTo(other.getType());
        if (result != 0)
            return result;

        return dbid.compareTo(other.getId());
    }

    /**
     * Obtain the hashcode for this node, based on type and id only.
     * @return The hashcode.
     */
    @Override
    public int hashCode () {
        return hashCode;
    }

    /**
     * Set the attributes map of this node. Nodes never create their own
     * attributes maps, and node attributes may be set only on nodes for
     * which the map has been set by calling this method. The map may be
     * removed by calling this with a null parameter.
     * @param attributes The attributes map for this node.
     */
    public void setAttributes (HashMap<String, String> attributes) {
        this.attributes = attributes;
    }

}
