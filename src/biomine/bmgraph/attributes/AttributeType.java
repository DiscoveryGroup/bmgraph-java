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

import biomine.bmgraph.BMEntity;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Definitions and utilities for typed attributes in BMEntities and type
 * inference from attribute values.
 *
 * <p>While the attributes stored in BMEntities are all just strings,
 * implementations using, reading or writing them often assign other
 * types to certain attributes. This class contains definitions for the
 * types, facilities to infer types from values and related tools for
 * use by graph reading, writing, and visualization tools.
 * @author Kimmo Kulovesi
 */
public enum AttributeType {
    /**
     * Boolean attribute, either 0/1, or "true"/"false".
     */
    BOOLEAN ("[01]|(true|false)", boolean.class, BooleanParser.instance),
    /**
     * Integer attribute.
     */
    INTEGER ("[0-9]+", int.class, IntegerParser.instance),
    /**
     * Real (floating point) attribute.
     */
    REAL ("[0-9]*[.]?[0-9]+(e[+-][0-9]+)?", double.class, RealParser.instance),
    /**
     * List type attribute (list of strings).
    LIST ("([A-Za-z0-9_:^!?.-]+,)+[A-Za-z0-9_:^!?.-]+"),
     */
    /**
     * String attribute (default).
     */
    STRING (".*", String.class, StringParser.instance);

    private final Pattern pattern;
    private final Class javaClass;
    private final Parser parser;

    AttributeType (String pattern, Class javaClass, Parser parser) {
        this.pattern = Pattern.compile("^("+pattern+")$",
                                       Pattern.CASE_INSENSITIVE);
        this.javaClass = javaClass;
        this.parser = parser;
    }
    
    /**
     * Check a given value against the pattern of this type.
     * @param value Value to test.
     * @return True iff value matches this type.
     */
    public boolean matches (String value) {
        if (value == null)
            return false;
        return pattern.matcher(value).matches();
    }

    /**
     * Get the Java class commonly used for representing this type.
     * @return The Java class for this type.
     */
    public Class getJavaClass () {
        return javaClass;
    }

    /**
     * Get the parser for String values of this type of attribute.
     * @return The Parser for this type of attribute.
     */
    public Parser getParser () {
        return parser;
    }

    /**
     * Parse a given value of this type with this type's parser.
     * @param value The value to parse (must be of this type).
     * @return The parsed value, of the type returned by the parser.
     */
    public Object parse (String value) {
        return parser.parse(value);
    }

    /**
     * Check if this AttributeType can represent the values of
     * another type. For example, the values of integers can be
     * represented by real numbers, and the values of any other type can
     * be represented by a string.
     * @param other The other type.
     * @return True iff the values of the other type can theoretically
     * be represented by this type.
     */
    public boolean canRepresent (AttributeType other) {
        if (other == null)
            return true;
        return (this.ordinal() >= other.ordinal());
    }

    /**
     * Get the most special (least general) type for a given value.
     * Note that this will return boolean type for values "0" and "1";
     * this is intentional.
     * @param value The String value to check.
     * @return The least general attribute type capable of representing
     * the value.
     */
    public static AttributeType getMostSpecialTypeFor (String value) {
        int i = 0;
        AttributeType[] types = values();
        while (i < types.length) {
            if (types[i].matches(value))
                return types[i];
            ++i;
        }
        return STRING;
    }

    /**
     * Get the most special (least general) type covering given values.
     * The intended usage is to read all values for an unknown attribute
     * and infer the type of that attribute from the values.
     * @param values An iterator for the values.
     * @return The least general attribute type capable of representing
     * all of the given values.
     */
    public static AttributeType getMostSpecialTypeCovering (
            Iterator<String> values) {
        assert values != null : "null iterator for values";
        String value;
        AttributeType t, mostSpecial = null;
        while (values.hasNext()) {
            value = values.next();
            t = getMostSpecialTypeFor(value);
            if (t.canRepresent(mostSpecial))
                mostSpecial = t;
        }
        if (mostSpecial == null)
            return STRING;
        return mostSpecial;
    }

    /**
     * Get the best-suited attribute type for a given Java class.
     * @param javaClass The Java class.
     * @return The closest matching attribute type for representing
     * values of the given Java class, or null if none.
     */
    public static AttributeType getAttributeTypeFor (Class javaClass) {
        assert javaClass != null : "Null class";
        return TYPE_FOR_JAVA.get(javaClass);
    }

    /**
     * Get the parser best-suited for parsing attribute values of the
     * type of a given Java class.
     * @param javaClass The Java class.
     * @return The parser best-suited for parsing attribute values of
     * the given Java class, or null if no suitable parser available.
     */
    public static Parser getParserFor (Class javaClass) {
        AttributeType t = getAttributeTypeFor(javaClass);
        if (t == null)
            return null;
        return t.getParser();
    }

    /**
     * Attribute names mapped to their standard types. Graph writers and
     * such can define more attributes as necessary, but these are the
     * "standard" ones generally shared by everything. Note that this
     * HashMap is modifiable for use with applications other than the
     * Biomine project.
     */
    public static final HashMap<String, AttributeType> STANDARD_TYPES =
                                        new HashMap<String, AttributeType>();

    private static final HashMap<Class, AttributeType> TYPE_FOR_JAVA =
                                        new HashMap<Class, AttributeType>();

    /**
     * Infer the least general types covering all values for each attribute
     * in use in a collection of BMEntities. The resulting HashMap will
     * have all attribute names in the given BMEntities collection as
     * keys and their inferred types as values. The types will be the
     * least general types that can be used to represent all values
     * present for each attribute in the collection.
     *
     * <p>The typical usage is to supply bmgraph.getNodes() or
     * bmgraph.getEdges() as the argument.
     *
     * @param items A Collection which must contain only BMEntities.
     * @return A new HashMap from String attribute names to inferred types.
     */
    public static HashMap<String, AttributeType> inferFromValues (
                                        Collection<? extends BMEntity> items)
    {
        HashMap<String, HashSet<String>> allValues;
        HashMap<String, AttributeType> attributesInUse;
        allValues = new HashMap<String, HashSet<String>>();

        AttributeType t;
        BMEntity entity;

        for (Object e : items) {
            assert (e instanceof BMEntity) : "Non-BMEntity item";
            entity = (BMEntity)e;
            HashMap<String, String> attributes = entity.getAttributes();
            if (attributes == null)
                continue;

            for (String key : attributes.keySet()) {
                if (!allValues.containsKey(key))
                    allValues.put(key, new HashSet<String>());
                allValues.get(key).add(attributes.get(key));
            }
        }
        attributesInUse = new HashMap<String, AttributeType>();
        for (String key : allValues.keySet()) {
            t = AttributeType.getMostSpecialTypeCovering(
                                    allValues.get(key).iterator());
            if (t == null)
                t = AttributeType.STRING;
            attributesInUse.put(key, t);
        }
        return attributesInUse;
    }

    static {
        STANDARD_TYPES.put("_groupnode", AttributeType.BOOLEAN);
        STANDARD_TYPES.put("_membernode", AttributeType.BOOLEAN);
        STANDARD_TYPES.put("_edgenode", AttributeType.BOOLEAN);
        STANDARD_TYPES.put("_arrowedge", AttributeType.BOOLEAN);
        STANDARD_TYPES.put("_reversed", AttributeType.BOOLEAN);
        STANDARD_TYPES.put("_nodesize", AttributeType.INTEGER);

        STANDARD_TYPES.put("pos_x", AttributeType.REAL);
        STANDARD_TYPES.put("pos_y", AttributeType.REAL);
        STANDARD_TYPES.put("pinned", AttributeType.BOOLEAN);
        STANDARD_TYPES.put("special", AttributeType.BOOLEAN);

        STANDARD_TYPES.put("shortest", AttributeType.BOOLEAN);
        STANDARD_TYPES.put("best", AttributeType.BOOLEAN);
        STANDARD_TYPES.put("minimal", AttributeType.BOOLEAN);

        STANDARD_TYPES.put("ttnr", AttributeType.REAL);
        STANDARD_TYPES.put("goodness", AttributeType.REAL);
        STANDARD_TYPES.put("goodness_of_best_path", AttributeType.REAL);
        STANDARD_TYPES.put("relevance", AttributeType.REAL);
        STANDARD_TYPES.put("reliability", AttributeType.REAL);
        STANDARD_TYPES.put("rarity", AttributeType.REAL);

        STANDARD_TYPES.put("distance", AttributeType.INTEGER);
        STANDARD_TYPES.put("length_of_best_path", AttributeType.INTEGER);

        int i = 0;
        AttributeType[] types = values();
        while (i < types.length) {
            TYPE_FOR_JAVA.put(types[i].getJavaClass(), types[i]);
            ++i;
        }
        TYPE_FOR_JAVA.put(Boolean.class, AttributeType.BOOLEAN);
        TYPE_FOR_JAVA.put(Integer.class, AttributeType.INTEGER);
        TYPE_FOR_JAVA.put(Double.class, AttributeType.REAL);
        TYPE_FOR_JAVA.put(Float.class, AttributeType.REAL);
        TYPE_FOR_JAVA.put(float.class, AttributeType.REAL);
        //TYPE_FOR_JAVA.put(LinkedList.class, AttributeType.LIST);
    }
}
