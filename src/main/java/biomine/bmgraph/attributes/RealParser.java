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
 * The parser for the values of real number attributes.
 * @author Kimmo Kulovesi
 */
public class RealParser implements Parser<Double> {
    public AttributeType getType () { return AttributeType.REAL; }
    public Class getJavaClass() { return AttributeType.REAL.getJavaClass(); }
    public Double parse (String s) { return this.parseDouble(s); }

    /**
     * Parse a String. Note that the parser fails silently on
     * errors, returning 0.
     * @param s The String to parse.
     * @return The parsed double value or 0 in case of failure.
     */
    public static double parseDouble (String s) {
        if (s == null)
            return 0.0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return (double)(IntegerParser.parseInt(s));
        }
    }

    /**
     * An instance of this parser (they are all alike).
     */
    public static final RealParser instance = new RealParser();
}
