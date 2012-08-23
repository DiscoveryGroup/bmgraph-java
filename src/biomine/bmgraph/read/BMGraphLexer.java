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

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.IOException;
import java.io.FileInputStream;

/**
 * BMGraph lexer. Lexes a stream of input into BMGraphTokens, using one
 * byte lookahead. This is used by BMGraphParser.
 * @author Kimmo Kulovesi
 */

public class BMGraphLexer {
    private byte[] buffer;

    /**
     * Interface for reporting errors in lexing.
     */
    public interface ErrorCallback {
        /**
         * Report an error at a given position.
         * @param message Error message.
         * @param line Line of occurrence.
         * @param column Column of occurrence.
         */
        public void lexerError (String message, int line, int column);
    }

    /**
     * Create a new lexer using given input and an error handler.
     * @param input InputStream for lexer.
     * @param errorHandler Handler for lexer errors or null to print to stderr.
     */
    public BMGraphLexer (InputStream input, ErrorCallback errorHandler) {
        assert input != null : "Null input";
        this.input = new PushbackInputStream(input, 6);
        this.token = null;
        this.line = 1;
        this.column = 1;
        this.errorHandler = errorHandler;
        this.buffer = new byte[1024];
    }

    /**
     * Create a new lexer using given input.
     * @param input InputStream for lexer.
     */
    public BMGraphLexer (InputStream input) {
        this(input, null);
    }
    
    /**
     * Peek at the next token.
     * @return The next token that will be returned by getToken().
     */
    public BMGraphToken nextToken () {
        if (token == null)
            readToken();
        return token;
    }

    /**
     * Peek at the next token's type.
     * @return Type of the next token.
     */
    public BMGraphToken.Type nextTokenType () {
        if (token == null)
            readToken();
        return token.getType();
    }

    /**
     * Is the next token a line terminator?
     * @return True iff the next token is NEWLINE or EOF.
     */
    public boolean atEOL () {
        if (token == null)
            readToken();
        return (token.getType() == BMGraphToken.Type.NEWLINE ||
                token.getType() == BMGraphToken.Type.EOF);
    }

    /**
     * Get a token.
     * @return A token.
     */
    public BMGraphToken getToken () {
        BMGraphToken t = nextToken();
        token = null;
        return t;
    }

    /**
     * Get the current input line number (on which the next token is).
     * @return Current input line.
     */
    public int getLine () {
        return nextToken().getLine();
    }

    /**
     * Get the current input column number (at which the next token starts).
     * @return Current input column.
     */
    public int getColumn () {
        return nextToken().getColumn();
    }

    /**
     * Report an error at current position.
     * @param message The error message.
     */
    protected void error (String message) {
        if (errorHandler != null) {
            errorHandler.lexerError(message, line, column);
        } else {
            System.err.println("Lexer error:"+line+":"+column+":"+message);
        }
    }

    /**
     * Return a single token from the current position up to the end of line.
     * The next token after this one will be a NEWLINE or an EOF token.
     * This will return null if the current "next token" is already
     * NEWLINE or EOF.
     * @return A TEXT token up to the end of line, or null if none.
     */
    public BMGraphToken getToEOL () {
        if (token == null)
            readToken();

        if (token.getType() == BMGraphToken.Type.NEWLINE
            || token.getType() == BMGraphToken.Type.EOF)
            return null;

        int c;
        int i = 0;
        do {
            c = readByte();
            if (c == '\n' || c == -1) {
                unreadByte(c);
                break;
            }
            buffer[i++] = (byte)c;
            if (i >= buffer.length) {
                error("Line too long"); break;
            }
        } while (true);

        String img;
        if (i > 0) {
            try {
                img = new String(buffer, 0, i, "utf-8");
            } catch (Exception e) {
                img = new String(buffer, 0, i);
            }
        } else {
            img = "";
        }
        BMGraphToken t = new BMGraphToken(BMGraphToken.Type.TEXT,
                                        token.getImage() + img, line, column);
        token = null;
        return t;
    }

    /**
     * Skip text until the end of line. The next token will be a NEWLINE
     * or an EOF token. This does nothing if already at NEWLINE or EOF
     * (i.e. to skip multiple lines, the NEWLINE tokens must be
     * processed with getToken()).
     */
    public void skipToEOL () {
        if (token == null)
            readToken();

        if (token.getType() == BMGraphToken.Type.NEWLINE
            || token.getType() == BMGraphToken.Type.EOF)
            return;

        int c;
        do { c = readByte(); } while (c != '\n' && c != -1);
        unreadByte(c);

        token = null;
    }

    /*
     * Read a new token from the input.
     */
    private void readToken () {
        int c;
        do {
            c = readByte();
        } while (c == '\r');

        switch (c) {
            case ' ':
            case '\t':
            case '\f': {
                do {
                    c = readByte();
                } while (c == ' ' || c == '\t' || c == '\r' || c == '\f');
                unreadByte(c);
                token = new BMGraphToken(BMGraphToken.Type.SPACE, " ",
                                         line, column);
                return;
            }
            case '_': {
                token = new BMGraphToken(BMGraphToken.Type.UNDERSCORE, "_",
                                         line, column);
                return;
            }
            case ':': {
                token = new BMGraphToken(BMGraphToken.Type.COLON, ":",
                                         line, column);
                return;
            }
            case ',': {
                token = new BMGraphToken(BMGraphToken.Type.COMMA, ",",
                                         line, column);
                return;
            }
            case '=': {
                token = new BMGraphToken(BMGraphToken.Type.EQUALS, "=",
                                         line, column);
                return;
            }
            case '%': {
                token = new BMGraphToken(BMGraphToken.Type.PERCENT, "%",
                                         line, column);
                return;
            }
            case '#': {
                token = new BMGraphToken(BMGraphToken.Type.HASH, "#", line, column);
                return;
            }
            case -1: {      /* End of file */
                token = new BMGraphToken(BMGraphToken.Type.EOF, "\000", line, column);
                return;
            }
            case '\n': {
                token = new BMGraphToken(BMGraphToken.Type.NEWLINE, "\n", line, column);
                ++line;
                column = 1;
                return;
            }
            default: {      /* Identifier */
                int i = 0;
                do {
                    buffer[i++] = (byte)c;
                    if (i >= buffer.length) {
                        error("Identifier too long");
                        break;
                    }
                    c = readByte();
                } while (c != ' ' && c != '\n' && c != '_' && c != '=' && c != ':'
                         && c != ',' && c != '\t' && c != '\r' && c != '\f'
                         && c != -1);
                unreadByte(c);
                String img;
                try {
                    img = new String(buffer, 0, i, "utf-8");
                } catch (Exception e) {
                    img = new String(buffer, 0, i);
                }
                token = new BMGraphToken(BMGraphToken.Type.IDENTIFIER,
                                         img, line, column);
            }
        }
    }

    /*
     * Read a single character from the input.
     * @return The character read or -1 on EOF or error.
     */
    private int readByte () {
        int c;
        try {
            c = input.read();
            if (c != -1)
                ++column;
        } catch (IOException e) {
            c = -1;
            error(e.getMessage());
        }
        return c;
    }

    /*
     * Unread a single character. It will be returned by the next call
     * to readByte().
     */
    private void unreadByte (int c) {
        if (c == -1)
            return;

        try {
            input.unread(c);
            --column;
        } catch (IOException e) {
            error(e.getMessage());
        }
    }
    
    /*
     * The next token or null if one hasn't been read yet.
     */
    private BMGraphToken token;

    /*
     * The input from which we read.
     */
    private PushbackInputStream input;

    /*
     * Line number in the input, starting from 1.
     */
    private int line;

    /*
     * Column number in the input, starting from 1. The column is
     * counted simply by incrementing the count by one for each
     * character other than newline, which resets the column to 1.
     */
    private int column;

    /*
     * Handler for the lexer's error messages.
     */
    private ErrorCallback errorHandler;

    /**
     * Main function for testing with files given on the command line.
     * @param filenames Files to parse.
     */
    public static void main (String[] filenames) throws Exception {
        for (String filename : filenames) {
            FileInputStream input = new FileInputStream(filename);
            BMGraphLexer lexer = new BMGraphLexer(input);
            BMGraphToken t;
            do {
                t = lexer.getToken();
                if (t.getType() == BMGraphToken.Type.EOF)
                    break;
                t.setFile(filename);
                System.out.println(t.toString() + ":" + t.positionToString());
            } while(true);
            input.close();
        }
    }
}
