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

import biomine.bmgraph.BMEdge;
import biomine.bmgraph.BMGraph;
import biomine.bmgraph.BMNode;
import biomine.bmgraph.write.BMGraphWriter;

import java.net.URL;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Reads a BMGraph-formatted file or stream into a BMGraph instance.
 * Semantics of the graph are verified while reading, and errors are
 * reported via a callback interface (ErrorCallback).
 *
 * This class is the preferred way reading into a BMGraph data structure
 * from a file in BMGraph format.
 *
 * @author Kimmo Kulovesi
 */

public class BMGraphReader {

    /**
     * Interface for reporting errors in reading graphs.
     */

    public interface ErrorCallback {
        /**
         * Report an error at a given position.
         * @param message Error message.
         * @param file File (or stream) of occurrence.
         * @param line Line of occurrence.
         * @param column Column of occurrence.
         */
        public void readerError (String message, String file, int line, int column);

        /**
         * Report a warning at a given position.
         * @param message Warning message.
         * @param file File (or stream) of occurrence.
         * @param line Line of occurrence.
         * @param column Column of occurrence.
         */
        public void readerWarning (String message, String file,
                                   int line, int column);
    }

    /**
     * An error callback that reports errors to a PrintStream.
     */
    public class PrintStreamCallback implements ErrorCallback {
        private PrintStream stream;

        public PrintStreamCallback (PrintStream stream) {
            assert stream != null : "Null stream";
            this.stream = stream;
        }

        @Override
        public void readerError (String message, String file,
                                 int line, int column) {
            stream.print("Error:");
            stream.println(file+":"+line+":"+column+":"+message);
        }

        @Override
        public void readerWarning (String message, String file,
                                 int line, int column) {
            stream.print("Warning:");
            stream.println(file+":"+line+":"+column+":"+message);
        }
    }

    private BMGraph graph;
    private BMGraphParser parser;
    private ParserCallback callback;

    /**
     * Create a BMGraphReader that adds to the specified graph.
     * @param graph BMGraph to add to.
     * @param callback For reporting errors (null to print to stderr).
     */
    public BMGraphReader (BMGraph graph, ErrorCallback callback) {
        assert graph != null : "Null graph";
        this.graph = graph;
        this.callback = new ParserCallback(this.graph, callback);
        parser = new BMGraphParser(this.callback);
    }

    /**
     * Create a BMGraphReader that starts with an empty graph.
     * @param callback For reporting errors (null to print to stderr).
     */
    public BMGraphReader (ErrorCallback callback) {
        graph = new BMGraph();
        this.callback = new ParserCallback(this.graph, callback);
        parser = new BMGraphParser(this.callback);
    }

    /**
     * Create a BMGraphReader with an empty graph, reporting errors to stderr.
     */
    public BMGraphReader () {
        this((ErrorCallback)null);
    }

    /**
     * Parse a BMGraph file.
     * @param filename File to parse
     * @return True iff the parsing was successful.
     */
    public boolean parseFile (String filename) {
        return parser.parseFile(filename);
    }

    /**
     * Parse a BMGraph File.
     * @param file File to parse
     * @return True iff the parsing was successful.
     */
    public boolean parseFile (File file) {
        return parser.parseFile(file);
    }

    /**
     * Parse from stdin.
     * @return True iff the parsing was successful.
     */
    public boolean parseSystemIn () {
        return parser.parse(System.in, "stdin");
    }

    /**
     * Parse an InputStream.
     * @param input The InputStream to read from.
     * @param filename The "filename" for the stream.
     * @return True iff the parsing was completely successful.
     */
    public boolean parseStream (InputStream input, String filename) {
        return parser.parse(input, filename);
    }
    
    /**
     * Return the graph that this reader reads to.
     * @return The graph associated with this reader.
     */
    public BMGraph getGraph () {
        return graph;
    }

    /**
     * The actual reader class, made private to hide the required
     * ParserCallback interface.
     */
    private class ParserCallback implements BMGraphParser.ParserCallback {
        private BMGraph graph;
        private ErrorCallback callback;

        public ParserCallback (BMGraph graph, ErrorCallback callback) {
            assert graph != null : "Null graph";

            this.graph = graph;
            if (callback == null)
                this.callback = new PrintStreamCallback(System.err);
            else
                this.callback = callback;
        }

        @Override
        public boolean parserError (String message, String file,
                                    int line, int column) {
            callback.readerError(message, file, line, column);
            return false;
        }

        public boolean parserError (String message, BMGraphToken token) {
            return parserError(message, token.getFile(),
                               token.getLine(), token.getColumn());
        }

        public void parserWarning (String message, String file,
                                      int line, int column) {
            callback.readerWarning(message, file, line, column);
        }

        public void parserWarning (String message, BMGraphToken token) {
            parserWarning(message, token.getFile(),
                          token.getLine(), token.getColumn());
        }

        @Override
        public boolean parsedSpecialNode (BMGraphToken node) {
            BMNode n = new BMNode(node);

            if (graph.hasGroupMember(n)) {
                parserWarning("Group member as a special node caused ungrouping",
                              node);
            }
            graph.ensureHasNode(n);
            graph.markSpecial(n);
            return true;
        }

        @Override
        public boolean parsedEdge (BMGraphToken from, BMGraphToken to,
                                   BMGraphToken linktype,
                                   HashMap<String, String> attributes,
                                   boolean addAttributes) {
            String l;
            BMEdge edge;
            BMNode a, b;
            a = new BMNode(from);
            b = new BMNode(to);

            /*if (a.equals(b)) {
                parserWarning("Edge from node to itself ignored", to);
                return true;
            }*/

            if (linktype == null)
                l = "";
            else
                l = graph.getCanonicalType(linktype.getImage());
            if (graph.isCanonicalDirection(l)) {
                // Please note that symmetric link types are defined to 
                // always be in non-canonical direction, so we never get 
                // here for symmetric types
                edge = new BMEdge(a, b, l, attributes);
            } else {
                // Please note that symmetric link types are always created
                // with this constructor:
                edge = new BMEdge(b, a, graph.getReverseType(l), attributes, l);
            }

            if (addAttributes) {
                if (graph.addEdgeAttributes(edge, attributes) == null)
                    parserWarning("Edge attributes for an unknown edge",
                                  linktype);
                return true;
            }

            if (graph.hasGroupMember(a))
                parserWarning("Member appearing in edge caused ungrouping", from);
            if (graph.hasGroupMember(b))
                parserWarning("Member appearing in edge caused ungrouping", to);

            if (graph.ensureHasEdge(edge) != edge) {
                edge = graph.getEdge(edge);
                if (edge == null)
                    parserWarning("Nonsensical edge ignored", from);
                else
                    edge.putAll(attributes);
            }
            return true;
        }

        @Override
        public boolean parsedNodeAttributes (BMGraphToken node,
                                             HashMap<String, String> attributes) {
            BMNode n = new BMNode(node);

            if (graph.hasGroupMember(n)) {
                n = graph.getGroupMember(n);
                if (n == null) {
                    parserWarning("Attributes for group member node ignored",
                                  node);
                    return true;
                }
            } else {
                n = graph.ensureHasNode(n);
            }
            if (n.getAttributes() == null)
                n.setAttributes(attributes);
            else
                n.putAll(attributes);
            return true;
        }

        @Override
        public boolean parsedNodeGroup (BMGraphToken group,
                                        LinkedList<BMGraphToken> members) {
            BMNode groupnode;
            LinkedList<BMNode> membernodes;

            groupnode = new BMNode(group);
            if (graph.hasGroupMember(groupnode))
                return parserError("Definition of group member as a group", group);
            if (graph.hasGroupNode(groupnode)) {
                parserWarning("Redefinition of node group ignored", group);
                return true;
            }
            if (members.size() == 0) {
                parserWarning("Group definition with no members ignored", group);
                return true;
            }

            membernodes = new LinkedList<BMNode>();
            for (BMGraphToken membertoken : members) {
                BMNode node = new BMNode(membertoken);
                if (graph.hasNode(node)) {
                    parserWarning("Invalid group: member node already in the graph",
                                  membertoken);
                    return true;
                }
                if (graph.hasGroupMember(node)) {
                    parserWarning("Invalid group: member already in another group",
                                  membertoken);
                    return true;
                }
                membernodes.addLast(node);
            }
            if (graph.defineGroup(groupnode, membernodes) == null)
                parserWarning("Conflicting group definition ignored", group);

            return true;
        }

        @Override
        public boolean parsedReverseLinktype (BMGraphToken forward,
                                              BMGraphToken reverse) {
            String f, r;
            f = forward.getImage();
            r = reverse.getImage();
            if (f.equals(r)) {
                parserWarning("Symmetric linktype in reverse definition", reverse);
                return parsedSymmetricLinktype(forward);
            }
            if (graph.numEdges() > 0) {
                if (!r.equals(graph.getReverseType(f)))
                    parserWarning("Linktype defined after edges encountered",
                                  forward);
            }
            graph.defineReverseLinktype(f, r);
            return true;
        }

        @Override
        public boolean parsedSymmetricLinktype (BMGraphToken symmetric) {
            String s = symmetric.getImage();
            if (graph.numEdges() > 0) {
                if (!graph.isSymmetricType(s))
                    parserWarning("Linktype defined after edges encountered",
                                  symmetric);
            }
            graph.defineSymmetricLinktype(s);
            return true;
        }

        @Override
        public boolean parsedDatabase (BMGraphToken database,
                                       BMGraphToken version,
                                       BMGraphToken server,
                                       BMGraphToken url) {
            String[] db = graph.getDatabaseArray();
            if (db[0] != null) {
                if (!db[0].equals(database.getImage())) {
                    db[0] = db[1] = db[2] = null;
                    return true;
                } else {
                    if (version != null && db[1] != null &&
                        !db[1].equals(version.getImage())) {
                        version = null;
                    }
                    if (server != null && db[2] != null &&
                        !db[2].equals(server.getImage())) {
                        server = null;
                    }
                }
            }
            db[0] = (database == null ? null : database.getImage());
            db[1] = (version == null ? null : version.getImage());
            db[2] = (server == null ? null : server.getImage());
            // DEBUG: Should URL be overridden?
            if (url != null)
                db[3] = url.getImage();
            return true;
        }

        @Override
        public boolean parsedNodeExpandURL (BMGraphToken urlPart) {
            try {
                URL temp = new URL(urlPart.toString());
                graph.setNodeExpandURL(temp);
            } catch (Exception e) {
                return parserError("Couldn't parse URL: " + e.getMessage(), urlPart);
            }
            return true;
        }

        @Override
        public boolean parsedNodeExpandProgram (BMGraphToken program) {
            if (program.toString().length() < 2)
                parserWarning("Curiously short node expand program name", program);

            graph.setNodeExpandProgram(program.toString());
            return true;
        }

        @Override
        public boolean parsedSpecialComment (BMGraphToken comment, String content) {            

            //graph.addComment("_"+comment+content);
            graph.addSpecialComment(comment.getImage(), content.trim().intern());

            return true;
        }

        @Override
        public boolean parsedComment (BMGraphToken comment) {
            graph.addComment(comment.getImage());
            return true;
        }
    }

    /**
     * Main function for testing with files given on command line. All
     * files specified on the command line (or stdin if none) are
     * considered to be part of a single graph. This graph will be read
     * and then output in a "canonical" format (everything sorted).
     * @param filenames Files to parse, none for System.in.
     */
    public static void main (String[] filenames) throws Exception {
        BMGraphWriter.main(filenames);
    }
}
