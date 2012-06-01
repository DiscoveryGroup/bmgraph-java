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
