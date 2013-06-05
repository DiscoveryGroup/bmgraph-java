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
 * The parser for the values of integer attributes.
 * @author Kimmo Kulovesi
 */
public class IntegerParser implements Parser<Integer> {
    public AttributeType getType () { return AttributeType.INTEGER; }
    public Class getJavaClass() { return AttributeType.INTEGER.getJavaClass(); }
    public Integer parse (String s) { return this.parseInt(s); }

    /**
     * Parse a String. Note that the parser fails silently on
     * errors, returning 0.
     * @param s The String to parse.
     * @return The parsed int value or 0 in case of failure.
     */
    public static int parseInt (String s) {
        if (s == null)
            return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            if ("true".equals(s))
                return 1;
            return 0;
        }
    }

    /**
     * An instance of this parser (they are all alike).
     */
    public static final IntegerParser instance = new IntegerParser();
}
