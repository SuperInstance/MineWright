package com.minewright.script;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Lexer for script DSL parsing.
 *
 * <p>Performs lexical analysis by scanning character-by-character,
 * creating tokens, and tracking position within the source.</p>
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Character-by-character scanning</li>
 *   <li>Token creation and classification</li>
 *   <li>Position tracking (line, column)</li>
 *   <li>Whitespace and comment skipping</li>
 *   <li>String literal parsing with escape sequences</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class ScriptLexer {

    private static final Logger LOGGER = TestLogger.getLogger(ScriptLexer.class);

    private final String source;
    private int position;
    private final int length;

    /**
     * Creates a new lexer for the given source.
     *
     * @param source The source text to lex
     */
    public ScriptLexer(String source) {
        this.source = source != null ? source : "";
        this.position = 0;
        this.length = this.source.length();
    }

    /**
     * Gets the current position in the source.
     *
     * @return Current position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the current position in the source.
     *
     * @param position The new position
     */
    public void setPosition(int position) {
        this.position = Math.max(0, Math.min(position, length));
    }

    /**
     * Gets the total length of the source.
     *
     * @return Source length
     */
    public int getLength() {
        return length;
    }

    /**
     * Checks if at end of source.
     *
     * @return true if at end, false otherwise
     */
    public boolean isAtEnd() {
        return position >= length;
    }

    /**
     * Peeks at the current character without consuming it.
     *
     * @return Current character or '\0' if at end
     */
    public char peekChar() {
        return position < length ? source.charAt(position) : '\0';
    }

    /**
     * Peeks at the next character without consuming it,
     * after skipping whitespace.
     *
     * @return Next non-whitespace character or '\0' if at end
     */
    public char peek() {
        skipWhitespace();
        return peekChar();
    }

    /**
     * Peeks ahead by a specific offset.
     *
     * @param offset Number of characters to look ahead
     * @return Character at offset or '\0' if out of bounds
     */
    public char peekAhead(int offset) {
        int targetPos = position + offset;
        return targetPos < length ? source.charAt(targetPos) : '\0';
    }

    /**
     * Consumes and returns the current character.
     *
     * @return The consumed character
     */
    public char consumeChar() {
        return source.charAt(position++);
    }

    /**
     * Consumes the current character if it matches the expected value.
     *
     * @param expected The expected character
     * @throws ScriptParseException if character doesn't match
     */
    public void consume(char expected) throws ScriptParseException {
        skipWhitespace();
        if (peekChar() != expected) {
            throw error("Expected '" + expected + "', found '" + peekChar() + "'");
        }
        position++;
    }

    /**
     * Consumes a string if it matches at the current position.
     *
     * @param str The expected string
     * @throws ScriptParseException if string doesn't match
     */
    public void consume(String str) throws ScriptParseException {
        for (char c : str.toCharArray()) {
            if (peekChar() != c) {
                throw error("Expected '" + str + "'");
            }
            position++;
        }
    }

    /**
     * Checks if the current position matches a keyword.
     *
     * @param keyword The keyword to check for
     * @return true if keyword is found, false otherwise
     */
    public boolean peekKeyword(String keyword) {
        int savedPos = position;
        skipWhitespace();

        for (char c : keyword.toCharArray()) {
            if (peekChar() != c) {
                position = savedPos;
                return false;
            }
            position++;
        }

        // Check if keyword is followed by non-alphanumeric
        boolean result = isAtEnd() || !Character.isLetterOrDigit(peekChar());
        position = savedPos;
        return result;
    }

    /**
     * Consumes a keyword if it exists at the current position.
     *
     * @param keyword The keyword to consume
     * @throws ScriptParseException if keyword not found
     */
    public void consumeKeyword(String keyword) throws ScriptParseException {
        skipWhitespace();

        for (char c : keyword.toCharArray()) {
            if (peekChar() != c) {
                throw error("Expected keyword '" + keyword + "'");
            }
            position++;
        }
    }

    /**
     * Skips whitespace characters.
     */
    public void skipWhitespace() {
        while (!isAtEnd() && Character.isWhitespace(peekChar())) {
            position++;
        }
    }

    /**
     * Skips to the end of the current line.
     */
    public void skipLine() {
        while (!isAtEnd() && peekChar() != '\n') {
            position++;
        }
        if (!isAtEnd()) {
            position++; // Skip newline
        }
    }

    /**
     * Parses an identifier (alphanumeric + underscore + hyphen).
     *
     * @return The parsed identifier
     * @throws ScriptParseException if no identifier found
     */
    public String parseIdentifier() throws ScriptParseException {
        skipWhitespace();

        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peekChar()) || peekChar() == '_' || peekChar() == '-')) {
            sb.append(consumeChar());
        }

        if (sb.isEmpty()) {
            throw error("Expected identifier");
        }

        return sb.toString();
    }

    /**
     * Parses a string value (quoted or unquoted).
     *
     * @return The parsed string
     * @throws ScriptParseException if parsing fails
     */
    public String parseStringValue() throws ScriptParseException {
        skipWhitespace();

        if (peekChar() == '"') {
            return parseQuotedString();
        }

        // Parse unquoted string (may include spaces for metadata values)
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peekChar() != '\n' && peekChar() != '\r') {
            char c = peekChar();
            // Stop at special YAML characters (colon followed by space or end)
            if (c == ':' && !sb.isEmpty()) {
                char lastChar = sb.charAt(sb.length() - 1);
                if (Character.isWhitespace(lastChar)) {
                    break;
                }
            }
            sb.append(consumeChar());
        }

        String result = sb.toString().trim();
        return result.isEmpty() ? "" : result;
    }

    /**
     * Parses a quoted string with escape sequence support.
     *
     * @return The parsed string
     * @throws ScriptParseException if string is unterminated
     */
    public String parseQuotedString() throws ScriptParseException {
        consume('"');
        StringBuilder sb = new StringBuilder();

        while (!isAtEnd() && peekChar() != '"') {
            char c = consumeChar();
            if (c == '\\' && !isAtEnd()) {
                c = consumeChar();
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }

        if (isAtEnd()) {
            throw error("Unterminated string");
        }

        consume('"');
        return sb.toString();
    }

    /**
     * Parses an integer value.
     *
     * @return The parsed integer
     * @throws ScriptParseException if no integer found
     */
    public int parseIntValue() throws ScriptParseException {
        skipWhitespace();
        StringBuilder sb = new StringBuilder();

        while (!isAtEnd() && (Character.isDigit(peekChar()) || peekChar() == '-')) {
            sb.append(consumeChar());
        }

        if (sb.isEmpty()) {
            throw error("Expected number");
        }

        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            throw error("Invalid number: " + sb);
        }
    }

    /**
     * Parses a value (string, boolean, or number).
     *
     * @return The parsed value
     * @throws ScriptParseException if parsing fails
     */
    public Object parseValue() throws ScriptParseException {
        skipWhitespace();

        if (peekChar() == '"') {
            return parseQuotedString();
        }

        if (peekChar() == 't' || peekChar() == 'f') {
            String boolStr = parseIdentifier();
            if ("true".equalsIgnoreCase(boolStr)) return true;
            if ("false".equalsIgnoreCase(boolStr)) return false;
        }

        if (peekChar() >= '0' && peekChar() <= '9' || peekChar() == '-') {
            return parseIntValue();
        }

        return parseStringValue();
    }

    /**
     * Gets the current indentation level.
     *
     * @return Number of leading spaces/tabs on current line
     */
    public int getCurrentIndent() {
        int savedPos = position;
        int indent = 0;

        // Find start of current line
        while (savedPos > 0 && source.charAt(savedPos - 1) != '\n') {
            savedPos--;
        }

        // Count leading spaces/tabs
        while (savedPos < length && (source.charAt(savedPos) == ' ' || source.charAt(savedPos) == '\t')) {
            indent++;
            savedPos++;
        }

        return indent;
    }

    /**
     * Creates a parse exception with current position information.
     *
     * @param message The error message
     * @return A ScriptParseException with line and column information
     */
    public ScriptParseException error(String message) {
        int line = 1;
        int column = 1;

        // Calculate line and column
        for (int i = 0; i < position && i < length; i++) {
            if (source.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }

        return new ScriptParseException(message + " at line " + line + ", column " + column);
    }

    /**
     * Saves the current position for later restoration.
     *
     * @return A checkpoint that can be passed to restore()
     */
    public int checkpoint() {
        return position;
    }

    /**
     * Restores a previously saved position.
     *
     * @param checkpoint The checkpoint to restore
     */
    public void restore(int checkpoint) {
        this.position = checkpoint;
    }
}
