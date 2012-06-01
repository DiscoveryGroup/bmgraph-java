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
