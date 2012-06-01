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
