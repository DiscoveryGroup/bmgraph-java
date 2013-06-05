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

package biomine.bmgraph.read;

import biomine.bmgraph.BMEntity;

import java.util.regex.Pattern;
import java.util.HashMap;

/**
 * BMGraph token with type, image (string), optional attributes, and position.
 * Tokens as returned by the BMGraphLexer may be modified and
 * concatenated by the BMGraphParser, so a token does not necessarily
 * represent things as the lexer saw them in the input. Furthermore,
 * arbitrary attributes may be attached to each token separately of the
 * token's image, and this data may have come from an entirely different
 * place than the token's image.
 *
 * The content of a token must match its type. Any string of non-space
 * characters may be stored in a token of type IDENTIFIER, and any
 * string at all may be stored in a token of type TEXT.
 *
 * @author Kimmo Kulovesi
 */

public class BMGraphToken extends BMEntity {

    /**
     * Token types.
     */
    public enum Type {
        /**
         * Hash (#).
         */
        HASH ("#", false),
         /**
          * Colon (:).
          */
        COLON (":", false),
        /**
         * Comma (,).
         */
        COMMA (",", false),
        /**
         * Whitespace.
         */
        SPACE (" ", true),
        /**
         * Equals-sign (=).
         */
        EQUALS ("=", false),
        /**
         * Newline.
         */
        NEWLINE ("\n", true),
        /**
         * Percent sign (%).
         */
        PERCENT ("%", false),
        /**
         * Underscore (_).
         */
        UNDERSCORE ("_", false),
        /**
         * An identifier (any string not containing whitespace).
         */
        IDENTIFIER ("[^ \r\n\f\t]*", false),
        /**
         * Free text (any string).
         */
        TEXT (".*", false),
        /**
         * End of file/stream.
         */
        EOF ("\000", true);

        private final Pattern pattern;
        private final String patternString;
        private final boolean isSpace;

        Type (String pattern, boolean isSpace) {
            assert pattern != null : "Null pattern";
            this.isSpace = isSpace;
            this.patternString = pattern;
            this.pattern = Pattern.compile("^("+pattern+")$",
                                           Pattern.CASE_INSENSITIVE);
        }

        /**
         * Get the pattern for token type.
         * @return Pattern object for token type.
         */
        public Pattern getPattern () {
            return pattern;
        }

        /**
         * Matches a given string against the pattern of this token type.
         * The match is case-insensitive.
         * @param img The string to match against.
         * @return True if the pattern matches, false otherwise.
         */
        public boolean patternMatches (String img) {
            assert img != null : "Null string";
            return pattern.matcher(img).matches();
        }

        /**
         * Get the string representation of the token type's pattern.
         * @return Pattern string.
         */
        public String patternString () {
            return patternString;
        }

        /**
         * Is this token type some kind of space (newline, whitespace, EOF)?
         * @return True if this is a space type, false otherwise.
         */
        public boolean isSpace () {
            return isSpace;
        }
    }

    /**
     * Create a new token with type, image and position.
     * @param type Token type.
     * @param image The token "image", i.e. the token as it appeared in source.
     * @param line Line on which the token appeared.
     * @param column Column in which the token _ends_.
     */
    public BMGraphToken (Type type, String image, int line, int column) {
        super();

        assert image != null : "Null image";
        assert type.patternMatches(image) : "Invalid content ("+image+") for type "+type;

        this.type = type;
        this.image = image;
        this.line = line;
        this.column = (column - image.length());
        this.file = null;
    }

    /**
     * Create a new token with type and image, but no position.
     * @param type Token type.
     * @param image The token "image", i.e. the token as it appeared in source.
     */
    public BMGraphToken (Type type, String image) {
        this(type, image, -1, -1);
    }

    /**
     * Get a string representation of the token.
     * Note that this is NOT necessarily the token's image, e.g. for
     * space tokens it is the token type.
     * @return A string describing the token.
     */
    @Override
    public String toString () {
        if (type.isSpace())
            return type.toString();
        return getImage();
    }

    /**
     * Get the token's type.
     * @return Token's type.
     */
    public Type getType () {
        return type;
    }

    /**
     * Get the token's image.
     * @return Token's image.
     */
    public String getImage () {
        return image;
    }

    /**
     * Set the token's image. The new image must not violate the pattern
     * for the token's type, i.e. the token should be made into an
     * IDENTIFIER or a TEXT token first if necessary. IDENTIFIERS can
     * not contain whitespace, while TEXT tokens can contain anything.
     * @param image The token's new image.
     */
    public void setImage (String image) {
        assert image != null : "Null image";
        assert type.patternMatches(image) : "Invalid content ("+image+") for type "
                                             + type;
        this.image = image;
    }

    /**
     * Set the token's type to IDENTIFIER. This is used to discard the
     * special meaning of certain characters when they are part of an
     * identifier. The token's current type must not be any of the 
     * space types, since that would violate the pattern for identifiers,
     * nor can it be a TEXT token. Identifiers can not contain any
     * spaces, unlike TEXT tokens.
     */
    public void makeIdentifier () {
        if (Type.IDENTIFIER.patternMatches(image)) {
            type = Type.IDENTIFIER;
            return;
        }
        assert !(type.isSpace) : "Tried to make a space token into identifier";
        assert !(type == Type.TEXT) : "Tried to make a text token into identifier";
        assert false : "Pattern mismatch in token image for makeIdentifier()";
    }

    /**
     * Set the token's type to TEXT. A TEXT identifier can contain any
     * arbitrary text, without any constraints.
     */
    public void makeText () {
        type = Type.TEXT;
    }

    /**
     * Get the token's line position.
     * @return Token's line.
     */
    public int getLine () {
        return line;
    }
    
    /**
     * Get the token's column position.
     * @return Token's column.
     */
    public int getColumn () {
        return column;
    }

    /**
     * Get the token's file of occurrence.
     * @return Token's file of occurrence (or null).
     */
    public String getFile() {
        return file;
    }

    /**
     * Set the token's file of occurrence. May only be set once.
     * @param file Token's file of occurrence.
     */
    public void setFile(String file) {
        assert file != null : "Null filename";
        assert this.file == null : "File already set";
        this.file = file;
    }

    /**
     * Get a string representation of the token's position.
     * @return Token's file, line and column represented as a string.
     */
    public String positionToString () {
        return (file == null ? "" : file)+":"+
               (line < 0 ? "" : line)+":"+
               (column < 0 ? "" : column);
    }

    /**
     * Set the value of the given attributes, creating map if necessary.
     * @param key Key to set.
     * @param value Value to set the key to.
     * @return Previous value of the key, or null if none.
     */
    @Override
    public String put (String key, String value) {
        if (attributes == null)
            attributes = new HashMap<String, String>(8, 0.9f);
        return super.put(key, value);
    }

    /**
     * Remove an attribute.
     * @param key Key to remove.
     * @return Previous value of the key that was just removed.
     */
    @Override
    public String remove (String key) {
        if (attributes == null)
            return null;
        return super.remove(key);
    }

    private Type type;
    private String image;
    private String file;
    private int line;
    private int column;
}
