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
 * The interface for parsers of attribute values.
 * @author Kimmo Kulovesi
 */
public interface Parser<T> {
    /**
     * Get the attribute type parsed by this parser.
     * @return The parsed attribute type.
     */
    public AttributeType getType ();

    /**
     * Get the Java class for the attribute type parsed by this parser.
     * @return The java class parsed by this parser.
     */
    public Class getJavaClass ();

    /**
     * Parse a String. Note that these parsers fail silently on
     * errors, returning the default value for the data type in case
     * the parsing fails.
     * @param s The string to parse.
     * @return The parsed value.
     */
    public T parse (String s);
}
