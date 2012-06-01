package biomine.bmgraph.read;

import biomine.bmgraph.attributes.StringParser;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * BMGraph parser. Parses a BMGraph from a stream, using callbacks to
 * return the parsed tokens. The semantics of the graph are not verified
 * by this parser; instead, that is left to the reader. This class is
 * used as the parser by BMGraphReader, through the ParserCallback
 * interface.
 * @author Kimmo Kulovesi
 */

public class BMGraphParser implements BMGraphLexer.ErrorCallback {
    /**
     * The linktype string which is parsed as "no linktype".
     */
    public static final String NO_LINKTYPE = "+";

    /**
     * Interface for parser callback.
     */
    public interface ParserCallback {
        /**
         * Report an error in a given file and position.
         * @param message Error message.
         * @param file Input file (or stream) name.
         * @param line Line of occurrence.
         * @param column Column of occurrence.
         * @return True if parsing should be continued, false to abort.
         */
        public boolean parserError (String message, String file,
                                    int line, int column);

        /**
         * Called whenever a special node is parsed. The node token will
         * have at least the "type" and "dbid" attributes set, and
         * possibly "db" and "id" as well, if it was possible to split
         * "dbid" into these components.
         * @param node A special node.
         * @return True if parsing should be continued, false to abort.
         */
        public boolean parsedSpecialNode (BMGraphToken node);

        /**
         * Called whenever an edge is parsed. The from and to nodes will
         * have at least the "type" and "dbid" attributes set, and
         * possibly "db" and "id" as well, if it was possible to split
         * "dbid" into these components. The image of each node token
         * will be "type_dbid".
         * @param from The node from which the edge starts.
         * @param to The node to which the edge ends.
         * @param linktype The type of the link between the two nodes
         * (may be null).
         * @param attributes A map of edge attributes (may be null).
         * @param addAttributes True iff this line is intended to add
         * edge attributes only (instead of defining a new edge).
         * @return True if parsing should be continued, false to abort.
         */
        public boolean parsedEdge (BMGraphToken from, BMGraphToken to,
                                   BMGraphToken linktype,
                                   HashMap<String, String> attributes,
                                   boolean addAttributes);

        /**
         * Called whenever node attributes are parsed. Note that this
         * may be called multiple times for a single node. The node will
         * have at least the "type" and "dbid" attributes set, and
         * possibly "db" and "id" as well, if it was possible to split
         * "dbid" into these components. Note that the map of attributes
         * may be empty (but not null).
         * @param node The node whose attributes were parsed.
         * @param attributes A map of new attributes for the node.
         * @return True if parsing should be continued, false to abort.
         */
        public boolean parsedNodeAttributes (BMGraphToken node,
                                             HashMap<String, String> attributes);

        /**
         * Called whenever a node group is parsed. The node tokens will
         * have at least the "type" and "dbid" attributes set, and
         * possibly "db" and "id" as well, if it was possible to split
         * "dbid" into these components.
         * @param groupnode The group node.
         * @param members Group member nodes.
         * @return True if parsing should be continued, false to abort.
         */
        public boolean parsedNodeGroup (BMGraphToken groupnode,
                                        LinkedList<BMGraphToken> members);

        /**
         * Called whenever a reverse linktype specification is parsed.
         * @param forward The forward linktype name.
         * @param reverse The reverse linktype name.
         * @return True if parsing should be continued, false to abort.
         */
        public boolean parsedReverseLinktype (BMGraphToken forward,
                                              BMGraphToken reverse);

        /**
         * Called whenever a symmetric linktype specification is parsed.
         * @param symmetric The symmetric linktype name.
         * @return True if parsing should be continued, false to abort.
         */
        public boolean parsedSymmetricLinktype (BMGraphToken symmetric);

        /**
         * Called whenever a database specification is parsed.
         * @param database The database name (always non-null).
         * @param version Database version (may be null).
         * @param server Database server (may be null).
         * @param url Node viewer URL (may be null).
         * @return True if parsing should be continued, false to abort.
         */
        public boolean parsedDatabase (BMGraphToken database,
                                       BMGraphToken version,
                                       BMGraphToken server,
                                       BMGraphToken url);

        public boolean parsedNodeExpandURL (BMGraphToken urlPart);
        public boolean parsedNodeExpandProgram (BMGraphToken program);

        /**
         * Called whenever an unrecognized special comment is parsed.
         * @param comment The comment identifier in "# _identifier".
         * @param content Everything on the line after the identifier.
         * @return True if parsing should be continued, false to abort.
         */
        public boolean parsedSpecialComment (BMGraphToken comment, String content);

        /**
         * Called whenever a non-empty regular comment is parsed.
         * @param comment The comment text, without the leading "# " part.
         * @return True if parsing should be continued, false to abort.
         */
        public boolean parsedComment (BMGraphToken comment);

    }

    /**
     * Initialize a new parser.
     * @param callback Callback for parser progress and errors.
     */
    public BMGraphParser (ParserCallback callback) {
        assert callback != null : "Null callback";

        this.callback = callback;
        success = true;
        identifiers = new TreeMap<String, String>();
    }

    /**
     * Parse an input stream.
     * @param input InputStream to parse.
     * @param filename Name of the inputstream (e.g. filename, "stdin").
     * @return True on success, false on failure.
     */
    public boolean parse (InputStream input, String filename) {
        assert currentFile == null : "Already parsing";

        if (filename == null)
            currentFile = "input";
        else
            currentFile = filename;

        lex = new BMGraphLexer(input, this);

        try {
            parse_GRAPH();
            matchToken(BMGraphToken.Type.EOF);
        } catch (StopParse s) {
            // Parsing aborted in callback
        }
        
        currentFile = null;
        lex = null;

        return success;
    }

    /**
     * Parse a named file.
     * @param filename Source file to parse.
     * @return True on success, false on failure.
     */
    public boolean parseFile (String filename) {
        assert filename != null : "Null filename.";
        assert currentFile == null : "Already parsing";

        boolean result;
        InputStream input;

        try {
            input = new FileInputStream(filename);
        } catch (Exception e) {
            callback.parserError(e.getMessage(), filename, 0, 0);
            return false;
        }
        result = parse(input, filename);
        try { input.close(); } catch (IOException e) { }
        return result;
    }

    /**
     * Parse a File.
     * @param file File to parse.
     * @return True on success, false on failure.
     */
    public boolean parseFile (File file) {
        assert file != null : "Null file";
        assert currentFile == null : "Already parsing";

        boolean result;
        InputStream input;

        try {
            input = new FileInputStream(file);
        } catch (Exception e) {
            callback.parserError(e.getMessage(), file.getName(), 0, 0);
            return false;
        }
        result = parse(input, file.getName());
        try { input.close(); } catch (IOException e) { }
        return result;
    }

    /**
     * Report a lexer error. Should only be called by the lexer.
     * @param message Error message.
     * @param line Line in current file where the error was encountered.
     * @param column Column in current file where the error was encountered.
     */
    public void lexerError (String message, int line, int column) {
        assert currentFile != null : "Lexer error when no current file?";
        success = false;
        callback.parserError(message, currentFile, line, column);
    }


    /**
     * Report an error at the current position.
     * @param message Error message.
     */
    private void error (String message) throws StopParse {
        int line = (lex == null ? -1 : lex.getLine());
        int column = (lex == null ? -1 : lex.getColumn());
        String filename = (currentFile == null ? "" : currentFile);

        success = false;
        if (!callback.parserError(message, filename, line, column)) {
            throw new StopParse(message);
        }
    }

    /**
     * Report error at current position, stating what token was expected
     * and what token was found.
     * @param what Description of expected token(s).
     */
    private void expected (String what) throws StopParse {
        error(what+" expected, got "+lex.nextToken());
    }

    /**
     * Consume token of given type, or report an error.
     * @param type Expected type of token.
     * @return Token of given type or null if one wasn't available.
     */
    private BMGraphToken matchToken (BMGraphToken.Type type) throws StopParse {
        BMGraphToken t;

        if (lex.nextTokenType() == type) {
            t = lex.getToken();
            t.setFile(currentFile);
            return t;
        }
        expected(type.toString());
        return null;
    }

    /**
     * Consume an end of the line token, or report an error.
     */
    private void matchEOL () throws StopParse {
        if (!lex.atEOL()) {
            expected(BMGraphToken.Type.NEWLINE.toString());
        }
    }

    /**
     * Consume tokens until EOF or a token of given type is encountered.
     * DEBUG: Currently unused after implementation of "lex.skipToEOL()".
     * @param type Type of token to consume up to.
     */    
    private void forwardUntil (BMGraphToken.Type type) {
        BMGraphToken t = lex.nextToken();
        while(t.getType() != type && t.getType() != BMGraphToken.Type.EOF) {
            lex.getToken();
            t = lex.nextToken();
        }
    }

    /**
     * Create an IDENTIFIER token by joining multiple tokens together.
     * First a token is consumed. Then the following tokens are concatenated
     * to the end of that token's image until either some kind of space
     * token (SPACE, NEWLINE, EOF) or a token of either "endmark" type is
     * encountered. That token will not be a part of the token.
     * @param endmark Token type which ends the identifier.
     * @param endmark2 Another token type which ends the identifier.
     * @return A token of IDENTIFIER type or null if none encountered.
     */
    private BMGraphToken parseIdentifierUntil (BMGraphToken.Type endmark,
                                               BMGraphToken.Type endmark2)
            throws StopParse
    {
        BMGraphToken.Type nexttype;
        BMGraphToken t, identifier;

        if (lex.nextTokenType().isSpace()) {
            expected("Start of an identifier");
            return null;
        }

        identifier = lex.getToken();
        identifier.setFile(currentFile);
        identifier.makeIdentifier();

        nexttype = lex.nextTokenType();
        while (nexttype != endmark && nexttype != endmark2 && !nexttype.isSpace()) {
            t = lex.getToken();
            identifier.setImage(identifier.getImage() + t.getImage());
            nexttype = lex.nextTokenType();
        }
        return identifier;
    }

    /**
     * Create an IDENTIFIER token by joining multiple tokens together.
     * First a token is consumed. Then the following tokens are concatenated
     * to the end of that token's image until either some kind of space
     * token (SPACE, NEWLINE, EOF) or a token of the "endmark" type is
     * encountered. That token will not be a part of the token.
     * @param endmark Token type which ends the identifier.
     * @return A token of IDENTIFIER type or null if none encountered.
     */
    private BMGraphToken parseIdentifierUntil (BMGraphToken.Type endmark)
            throws StopParse
    {
        return parseIdentifierUntil(endmark, BMGraphToken.Type.SPACE);
    }

    /**
     * Recycle the same String instance for an identifier's image. The
     * parser maintains a Map of previously encountered identifiers.
     * Calling this method for a token will ensure that the String
     * instance used as the image of the identifier will be the same as
     * that stored in the identifier map. For example, edges tend to
     * have the same attributes but possibly in different order, so this
     * ensures that the same String for the attributes is recycled for
     * each edge.
     * @param identifier The token whose image string to recycle.
     */
    private void recycleString (BMGraphToken identifier) {
        String image = identifier.getImage();
        String s = identifiers.get(image);
        if (s == null) {
            s = image.intern();
            identifiers.put(s, s);
        }
        identifier.setImage(s);
    }

    /**
     * True if no errors have been encountered.
     */
    private boolean success;

    /**
     * The name of current input file.
     */
    private String currentFile;

    /**
     * Lexer for current input file.
     */
    private BMGraphLexer lex;

    /**
     * Callback for parser progress and error messages.
     */
    private ParserCallback callback;


    /**
     * Map for identifier strings (to recycle the same instances).
     */
    private Map<String, String> identifiers;

    /* Here be dragons: */

    private void parse_GRAPH () throws StopParse {
        do {
            switch (lex.nextTokenType()) {
                case IDENTIFIER: {
                    parse_NODE_OR_EDGE();
                    break;
                }
                case HASH: {
                    parse_COMMENT();
                    break;
                }
                case PERCENT: {
                    // Ignore GraphViz directives (deprecated)
                    lex.skipToEOL();
                    break;
                 }
                case NEWLINE: {
                    lex.getToken();
                    break;
                }
                case EOF: {
                    return;
                }
                default: {
                    expected("Node identifier or a comment");
                    lex.skipToEOL();
                }
            }
        } while(true);
    }

    private void parse_NODE_OR_EDGE () throws StopParse {
        BMGraphToken node = parse_NODE(BMGraphToken.Type.SPACE);
        if (lex.atEOL()) {
            if (!callback.parsedSpecialNode(node))
                throw new StopParse("Special node");
            return;
        }
        parse_EDGE(node);
    }

    private void parse_COMMENT () throws StopParse {
        matchToken(BMGraphToken.Type.HASH);
        if (lex.nextTokenType() == BMGraphToken.Type.SPACE)
            lex.getToken();
        switch (lex.nextTokenType()) {
            case UNDERSCORE: {
                matchToken(BMGraphToken.Type.UNDERSCORE);
                if (lex.nextTokenType() == BMGraphToken.Type.IDENTIFIER)
                    parse_SPECIAL_COMMENT();
                break;
            }
            default: {
                BMGraphToken comment = lex.getToEOL();
                if (comment != null)
                    callback.parsedComment(comment);
            }
        }
    }

    private BMGraphToken parse_NODE_DBID (BMGraphToken.Type endmark)
            throws StopParse
    {
        BMGraphToken db;

        db = parseIdentifierUntil(BMGraphToken.Type.COLON, endmark);
        if (db == null)
            return null;

        recycleString(db);
        if (lex.nextTokenType() == endmark) // In case the endmark is a colon
            return db;

        if (lex.nextTokenType() == BMGraphToken.Type.COLON) {
            lex.getToken();
            if (!(lex.nextTokenType() == endmark || lex.nextTokenType().isSpace())) 
            {
                BMGraphToken dbid = parseIdentifierUntil(endmark);
                if (dbid != null) {
                    recycleString(db);
                    recycleString(dbid);
                    db.put("db", db.getImage());
                    db.put("id", dbid.getImage());
                    db.setImage(db.getImage()+":"+dbid.getImage());
                }
            }
        }
        return db;
    }

    private BMGraphToken parse_NODE (BMGraphToken.Type endmark) throws StopParse {
        BMGraphToken dbid;
        BMGraphToken node = parseIdentifierUntil(BMGraphToken.Type.UNDERSCORE);

        if (node == null || matchToken(BMGraphToken.Type.UNDERSCORE) == null)
            return null;

        dbid = parse_NODE_DBID(endmark);
        if (dbid == null)
            return null;

        recycleString(node);
        node.put("type", node.getImage());
        if (dbid.getAttributes() != null)
            node.putAll(dbid.getAttributes());
        node.put("dbid", dbid.getImage());
        node.setImage(node.getImage()+"_"+dbid.getImage());

        return node;
    }

    private void parse_EDGE (BMGraphToken node_a) throws StopParse {
        parse_EDGE(node_a, false);
    }

    private void parse_EDGE (BMGraphToken node_a, boolean addAttr)
        throws StopParse
    {
        BMGraphToken node_b = null, linktype = null;
        HashMap<String, String> attributes = null;

        matchToken(BMGraphToken.Type.SPACE);
        node_b = parse_NODE(BMGraphToken.Type.SPACE);
        if (!lex.atEOL()) {
            matchToken(BMGraphToken.Type.SPACE);
            linktype = parseIdentifierUntil(BMGraphToken.Type.SPACE);
            if (NO_LINKTYPE.equals(linktype.getImage()))
                linktype = null;
            else
                recycleString(linktype);
            attributes = new HashMap<String, String>(16, 0.9f);
            parse_ATTRIBUTES(attributes);
            if (attributes.size() == 0)
                attributes = null;
        }
        if (!callback.parsedEdge(node_a, node_b, linktype,
                                 attributes, addAttr))
            throw new StopParse("Edge");
    }

    private void parse_ATTRIBUTES (Map<String, String> attributes) throws StopParse
    {
        while (!lex.atEOL()) {
            BMGraphToken key, value;

            matchToken(BMGraphToken.Type.SPACE);
            if (lex.atEOL())
                return; // Allow trailing space on the line

            key = parseIdentifierUntil(BMGraphToken.Type.EQUALS);
            matchToken(BMGraphToken.Type.EQUALS);
            value = parseIdentifierUntil(BMGraphToken.Type.SPACE);

            if (key != null && value != null) {
                // Unescape
                String s;
                s = StringParser.parseString(value.getImage());
                value.makeText();
                value.setImage(s);
                s = StringParser.parseString(key.getImage());
                key.makeText();
                key.setImage(s);
                recycleString(key);
                recycleString(value);
                attributes.put(key.getImage(), value.getImage());
            }
        }
    }

    private void parse_NODE_ATTRIBUTES () throws StopParse {
        BMGraphToken node;
        HashMap<String, String> attributes = new HashMap<String, String>(8, 0.9f);

        matchToken(BMGraphToken.Type.SPACE);
        node = parse_NODE(BMGraphToken.Type.SPACE);
        if (node == null)
            return;

        parse_ATTRIBUTES(attributes);

        if (!callback.parsedNodeAttributes(node, attributes))
            throw new StopParse("Node attributes");
    }

    private void parse_SPECIAL_COMMENT () throws StopParse {
        BMGraphToken t = parseIdentifierUntil(BMGraphToken.Type.SPACE);

        // Note: Before this method is called, it is verified that
        // there is an upcoming identifier token - t should never be/ null
        assert t != null : "No identifier for special comment";

        String img = t.getImage();

        if (img.equals("attributes")) {
            parse_NODE_ATTRIBUTES();
        } else if (img.equals("edge")) {
            matchToken(BMGraphToken.Type.SPACE);
            t = parse_NODE(BMGraphToken.Type.SPACE);
            parse_EDGE(t, true);
        } else if (img.equals("group")) {
            parse_NODE_GROUP();
        } else if (img.equals("reverse")) {
            parse_LINKTYPE_REVERSE();
        } else if (img.equals("symmetric")) {
            parse_LINKTYPE_SYMMETRIC();
        } else if (img.equals("canvas")) {
            parse_CANVAS(t);
        } else if (img.equals("database")) {
            parse_DATABASE();
        } else if (img.equals("node_expand_url")) {
            parse_NODE_EXPAND_URL();
        } else if (img.equals("node_expand_program")) {
            parse_NODE_EXPAND_PROGRAM();
        } else {
            // Unrecognized special comment
            if (lex.nextTokenType() == BMGraphToken.Type.SPACE)
                lex.getToken();
            BMGraphToken content = lex.getToEOL();
            if (!callback.parsedSpecialComment(t, (content == null ? "" : content.getImage())))
                throw new StopParse("Special comment");
        }
        matchEOL();
    }

    private void parse_NODE_GROUP () throws StopParse {
        int num_members;
        LinkedList<BMGraphToken> members;
        BMGraphToken groupnode, type;

        matchToken(BMGraphToken.Type.SPACE);
        groupnode = parse_NODE(BMGraphToken.Type.COLON);
        if (groupnode == null) {
            lex.skipToEOL();
            return;
        }
        if (lex.nextTokenType().isSpace())
            lex.getToken();
        else
            matchToken(BMGraphToken.Type.COLON);
        num_members = parse_INT();
        if (lex.nextTokenType().isSpace())
            lex.getToken();
        else
            matchToken(BMGraphToken.Type.COLON);
        type = parseIdentifierUntil(BMGraphToken.Type.COLON);
        if (lex.nextTokenType().isSpace())
            lex.getToken();
        else
            matchToken(BMGraphToken.Type.COLON);
        if (type != null) {
            members = new LinkedList<BMGraphToken>();
            parse_NODE_ID_LIST(type, members);
            if (members.size() != num_members)
                error(num_members+" nodes expected, got "+members.size());
            else if (!callback.parsedNodeGroup(groupnode, members))
                    throw new StopParse("Node group");
        }
        lex.skipToEOL();
    }

    private void parse_NODE_ID_LIST (BMGraphToken nodetype,
                                     List<BMGraphToken> list) throws StopParse {
        assert nodetype != null : "Null nodetype token";
        assert list != null : "Null list";

        BMGraphToken dbid;

        recycleString(nodetype);
        do {
            dbid = parse_NODE_DBID(BMGraphToken.Type.COMMA);
            if (dbid != null) {
                dbid.put("type", nodetype.getImage());
                dbid.put("dbid", dbid.getImage());
                dbid.setImage(nodetype.getImage()+"_"+dbid.getImage());
                recycleString(dbid);
                list.add(dbid);
            }
            if (lex.nextTokenType() == BMGraphToken.Type.COMMA)
                lex.getToken();
        } while (!lex.nextTokenType().isSpace());
    }

    private void parse_LINKTYPE_REVERSE () throws StopParse {
        BMGraphToken forward, reverse;

        matchToken(BMGraphToken.Type.SPACE);
        forward = parseIdentifierUntil(BMGraphToken.Type.SPACE);
        matchToken(BMGraphToken.Type.SPACE);
        reverse = parseIdentifierUntil(BMGraphToken.Type.NEWLINE);

        if (forward == null || reverse == null)
            return;
        if (!callback.parsedReverseLinktype(forward, reverse))
            throw new StopParse("Reverse linktype");
    }

    private void parse_LINKTYPE_SYMMETRIC () throws StopParse {
        BMGraphToken linktype;

        matchToken(BMGraphToken.Type.SPACE);
        linktype = parseIdentifierUntil(BMGraphToken.Type.SPACE);
        if (linktype == null)
            return;
        if (!callback.parsedSymmetricLinktype(linktype))
            throw new StopParse("Symmetric linktype");
    }

    private void parse_CANVAS (BMGraphToken canvas) throws StopParse {
        double left, right, top, bottom;

        matchToken(BMGraphToken.Type.SPACE);
        if ((left = parse_DOUBLE()) == Double.NaN)
            return;
        matchToken(BMGraphToken.Type.COMMA);
        if ((top = parse_DOUBLE()) == Double.NaN)
            return;
        matchToken(BMGraphToken.Type.COMMA);
        if ((right = parse_DOUBLE()) == Double.NaN)
            return;
        matchToken(BMGraphToken.Type.COMMA);
        if ((bottom = parse_DOUBLE()) == Double.NaN)
            return;

        if (!callback.parsedSpecialComment(canvas,
                                           left+","+top+","+right+","+bottom)) {
            throw new StopParse("Canvas");
        }
        /*
        if (!callback.parsedCanvas(left, right, top, bottom))
            throw new StopParse("Canvas");
            */
    }

    private void parse_DATABASE () throws StopParse {
        BMGraphToken database;
        BMGraphToken version = null, server = null, url = null;

        matchToken(BMGraphToken.Type.SPACE);
        database = parseIdentifierUntil(BMGraphToken.Type.SPACE);
        if (database == null) {
            lex.skipToEOL();
            return;
        }

        if (!lex.atEOL()) {
            matchToken(BMGraphToken.Type.SPACE);
            version = parseIdentifierUntil(BMGraphToken.Type.SPACE);
            if (!lex.atEOL()) {
                matchToken(BMGraphToken.Type.SPACE);
                server = parseIdentifierUntil(BMGraphToken.Type.SPACE);
                if (!lex.atEOL()) {
                    matchToken(BMGraphToken.Type.SPACE);
                    url = parseIdentifierUntil(BMGraphToken.Type.SPACE);
                    if (!lex.atEOL()) {
                        lex.skipToEOL();
                    }
                }
            }
        }

        if (!callback.parsedDatabase(database, version, server, url))
            throw new StopParse("Database");
    }

    private void parse_NODE_EXPAND_URL () throws StopParse {
        matchToken(BMGraphToken.Type.SPACE);
        BMGraphToken urlPart = parseIdentifierUntil(BMGraphToken.Type.SPACE, BMGraphToken.Type.NEWLINE);

        if (!lex.atEOL())
            lex.skipToEOL();

        if (urlPart == null)
            return;

        if (!callback.parsedNodeExpandURL(urlPart))
            throw new StopParse("Node expand url");
    }

    private void parse_NODE_EXPAND_PROGRAM () throws StopParse {
        matchToken(BMGraphToken.Type.SPACE);
        BMGraphToken program = parseIdentifierUntil(BMGraphToken.Type.SPACE, BMGraphToken.Type.NEWLINE);

        if (!lex.atEOL())
            lex.skipToEOL();

        if (program == null)
            return;

        if (!callback.parsedNodeExpandProgram(program))
            throw new StopParse("Node expand url");
    }


    private double parse_DOUBLE () throws StopParse {
        double d;
        BMGraphToken t = matchToken(BMGraphToken.Type.IDENTIFIER);

        if (t == null)
            return -1;
        try {
            d = Double.parseDouble(t.getImage());
        } catch (NumberFormatException e) {
            error("A number expected, got "+t);
            return Double.NaN;
        }
        return d;
    }

    private int parse_INT () throws StopParse {
        int i;
        BMGraphToken t = matchToken(BMGraphToken.Type.IDENTIFIER);

        if (t == null)
            return Integer.MIN_VALUE;
        try {
            i = Integer.parseInt(t.getImage());
        } catch (NumberFormatException e) {
            error("An integer expected, got "+t);
            return Integer.MIN_VALUE;
        }
        return i;
    }


    /*
     * An exception used to abort parsing if requested during callback.
     */
    private class StopParse extends Exception {
        public StopParse (String message) {
            super("Parsing aborted in callback ("+message+")");
        }        
        public StopParse () {
            super("Parsing aborted in callback");
        }
    }

    /*
     * A callback implementation for testing. Just prints whatever is
     * parsed to stdout and errors to stderr.
     */
    private static class TestCallback implements ParserCallback {
        public boolean parserError (String message, String file,
                                    int line, int column)
        {
            if (file == null)
                System.err.println("Error:"+message);
            else
                System.err.println("Error:"+file+":"+line+":"+column+":"+message);
            return true;
        }

        public boolean parsedSpecialNode (BMGraphToken node) {
            System.out.println(node.toString());
            return true;
        }
                                                                                   

        public boolean parsedEdge (BMGraphToken from, BMGraphToken to,
                                   BMGraphToken linktype,
                                   HashMap<String, String> attributes,
                                   boolean addAttr) {
            if (addAttr)
                System.out.print("# _edge ");
            System.out.println(from+" "+to+" "+linktype+" "+attributes);
            return true;
        }

        public boolean parsedNodeAttributes (BMGraphToken node,
                                             HashMap<String, String> attributes) {
            System.out.println("# "+node+" attributes: "+attributes);
            return true;
        }

        public boolean parsedNodeGroup (BMGraphToken groupnode,
                                        LinkedList<BMGraphToken> members) {
            System.out.println("# Group "+groupnode+" members: "+members);
            return true;
        }

        public boolean parsedReverseLinktype (BMGraphToken forward,
                                              BMGraphToken reverse) {
            System.out.println("# Linktype: "+forward+" <-> "+reverse);
            return true;
        }

        public boolean parsedSymmetricLinktype (BMGraphToken symmetric) {
            return parsedReverseLinktype(symmetric, symmetric);
        }

        public boolean parsedDatabase (BMGraphToken database,
                                       BMGraphToken version,
                                       BMGraphToken server,
                                       BMGraphToken url) {
            System.out.print("# Database: "+database);
            if (version != null)
                System.out.print(", version "+version);
            if (server != null)
                System.out.print(", server "+server);
            if (url != null)
                System.out.println(", url "+url);
            else
                System.out.print("\n");
            return true;
        }

        public boolean parsedNodeExpandURL (BMGraphToken urlPart) {
            System.out.println("# Node expand url: " + urlPart);
            return true;
        }

        public boolean parsedNodeExpandProgram (BMGraphToken program) {
            System.out.println("# Node expand program: " + program);
            return true;
        }

        public boolean parsedSpecialComment (BMGraphToken comment, String content) {
            System.out.println("# _"+comment+content);
            return true;
        }

        public boolean parsedComment (BMGraphToken comment) {
            System.out.println("# _"+comment);
            return true;
        }
    }

    /**
     * Main function for testing with files given on command line.
     * @param filenames Files to parse.
     */
    public static void main (String[] filenames) throws Exception {
        TestCallback cb = new TestCallback();
        BMGraphParser parser = new BMGraphParser(cb);
        if (filenames.length == 0) {
            parser.parse(System.in, "stdin");
        } else {
            for (String filename : filenames) {
                System.out.println("Parsing " + filename);
                parser.parseFile(filename);
            }
        }
    }
}
