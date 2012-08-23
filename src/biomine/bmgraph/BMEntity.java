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

import biomine.bmgraph.attributes.StringParser;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * BMGraph abstract entity with attributes (used for nodes and edges).
 * This defines an entity with an optional set of attributes.
 * It is used by BMNode and BMEdge for storage of node and edge attributes,
 * respectively. Note that this class doesn't define an interface for
 * assigning an attributes map (e.g. "setAttributes"), that must be done
 * in subclasses (either by specifying the setter method or setting it
 * in a constructor or other method).
 *
 * @author Kimmo Kulovesi
 */

public abstract class BMEntity {

    /**
     * Attributes map.
     */
    protected HashMap<String, String> attributes;

    /**
     * Convert attributes to a string, except for specified ones.
     * The attributes are converted in ascending order sorted by the key name.
     * A non-empty result will be prefixed by a space, so that the
     * string may be used in contexts such as "(entityString + attributesString)",
     * regardless of whether or not attributes exist for the entity.
     * @param ignored Attributes not converted.
     * @param before The string to insert before each attribute (even the first).
     * @param between The string to insert between the key and value (e.g. "=").
     * @param after The string to insert after each attribute (even the last).
     * @return Space-separated string, prefixed with a space unless empty. 
     */
    public String attributesToStringExcept (Set<String> ignored, String before,
                                            String between, String after) {
        assert ignored != null : "Null ignores";

        String result = "";

        if (attributes != null) {
            for (String key : new TreeSet<String>(attributes.keySet())) {
                if (key.length() > 0 && !ignored.contains(key)) {
                    result += before;
                    result += StringParser.escape(key.replace(between, ":"));
                    result += between;
                    result += StringParser.escape(attributes.get(key));
                    result += after;
                }
            }
        }
        return result;
    }

    /**
     * Convert attributes to a string, except for specified ones.
     * The attributes are converted in ascending order sorted by the key name.
     * A non-empty result will be prefixed by a space, so that the
     * string may be used in contexts such as "(entityString + attributesString)",
     * regardless of whether or not attributes exist for the entity.
     * @param ignored Attributes not converted.
     * @return Space-separated string, prefixed with a space unless empty. 
     */
    public String attributesToStringExcept (Set<String> ignored) {
        return attributesToStringExcept(ignored, " ", "=", "");
    }

    /**
     * Convert attributes to a string.
     * The attributes are converted in ascending order sorted by the key name.
     * A non-empty result will be prefixed by a space, so that the
     * string may be used in contexts such as "(entityString + attributesString)",
     * regardless of whether or not attributes exist for the entity.
     * @return Space-separated string, prefixed with a space unless empty. 
     */
    public String attributesToString () {
        return attributesToStringExcept(Collections.<String>emptySet());
    }

    /**
     * Get the attributes map of this entity. The map is modifiable.
     * @return Attributes map or null if none.
     */
    public HashMap<String, String> getAttributes () {
        return attributes;
    }

    /**
     * Put all attributes from the given map to this entity. New
     * attributes from the given map will be added, and existing ones
     * will be replaced. Note that this entity MUST have an attributes
     * map already present (even if empty).
     * @param attributes Map containing attributes to set.
     */
   public void putAll (HashMap<String, String> attributes) {
        assert this.attributes != null : "No attributes map";
        assert attributes != null : "Null attributes";

        this.attributes.putAll(attributes);
    }

    /**
     * Get the value of the given attribute.
     * @param key Key to get.
     * @return Value of the key or null if it isn't set (or no attributes map).
     */
    public String get (String key) {
        assert key != null : "Null key";
        return (attributes == null ? null : attributes.get(key));
    }

    /**
     * Set the value of the given attribute.
     * Note that an attributes map must already be present (even if empty).
     * @param key Key to set.
     * @param value Value to set the key to.
     * @return Previous value of the key, or null if none.
     */
    public String put (String key, String value) {
        assert attributes != null : "No attributes map";
        assert key != null : "Null key";

        if (value == null || value.equals(""))
            return attributes.remove(key);
        else
            return attributes.put(key, value);
    }

    /**
     * Remove an attribute.
     * Note that an attributes map must already be present (even if empty).
     * @param key Key to remove.
     * @return Previous value of the key that was just removed.
     */
    public String remove (String key) {
        assert attributes != null : "No attributes map";
        return attributes.remove(key);
    }

    /**
     * Clear all attributes (but retain any attribute map).
     */
    public void clearAttributes () {
        if (attributes == null)
            return;
        attributes.clear();
    }

}
