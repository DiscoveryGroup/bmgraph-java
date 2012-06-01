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
