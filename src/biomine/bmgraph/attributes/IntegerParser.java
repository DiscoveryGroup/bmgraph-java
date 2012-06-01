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
