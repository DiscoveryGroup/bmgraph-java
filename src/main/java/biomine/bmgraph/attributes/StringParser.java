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
 * The parser for the values of String attributes. This involves the
 * escaping and unescaping of values stored in BMGraph (and possibly
 * other) files.
 * @author Kimmo Kulovesi
 */
public class StringParser implements Parser<String> {
    public AttributeType getType () { return AttributeType.STRING; }
    public Class getJavaClass() { return AttributeType.STRING.getJavaClass(); }
    public String parse (String s) { return this.parseString(s); }
    
    /**
     * Parse a String. This un-escapes forbidden character sequences
     * in the String.
     * @param s The String to parse.
     * @return The un-escaped String value.
     */
    public static String parseString (String s) {
        if (s == null)
            return "";
        s = s.replace('+', ' ');
        s = s.replace("%2B", "+");
        s = s.replace("\\n", "\n");
        s = s.replace("\\t", " ");
        s = s.replace("\\\\", "\\");
        return s.intern();
    }

    /**
     * Escape a String for saving to a BMGraph file.
     * @param s The String to escape.
     * @return The escaped String value.
     */
    public static String escape (String s) {
        if (s == null)
            return "";
        s = s.replace("+", "%2B");
        s = s.replace("\n", "\\n");
        s = s.replace("\t", "+");
        s = s.replace("\\", "\\\\");
        s = s.replace(' ', '+');
        return s.intern();
    }

    /**
     * An instance of this parser (they are all alike).
     */
    public static final StringParser instance = new StringParser();
}
