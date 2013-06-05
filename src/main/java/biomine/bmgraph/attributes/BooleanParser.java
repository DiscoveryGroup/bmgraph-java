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

package biomine.bmgraph.attributes;

/**
 * The parser for the values of boolean attributes.
 * @author Kimmo Kulovesi
 */
public class BooleanParser implements Parser<Boolean> {
    public AttributeType getType () { return AttributeType.BOOLEAN; }
    public Class getJavaClass () { return AttributeType.INTEGER.getJavaClass(); }
    public Boolean parse (String s) { return this.parseBoolean(s); }

    /**
     * Parse a String. Note that the parser fails silently on
     * errors, returning false.
     * @param s The String to parse.
     * @return True iff the string is "true" or "1", false otherwise.
     */
    public static boolean parseBoolean (String s) {
        if ("1".equals(s) || "true".equals(s))
            return true;
        return false;
    }

    /**
     * An instance of this parser (they are all alike).
     */
    public static final BooleanParser instance = new BooleanParser();
}
