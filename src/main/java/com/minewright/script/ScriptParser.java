package com.minewright.script;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

/**
 * Facade for parsing DSL scripts into Script objects.
 *
 * <p><b>DSL Grammar:</b></p>
 * <pre>
 * script          ::= sequence | selector | parallel | action
 * sequence        ::= "sequence" "{" script+ "}"
 * selector        ::= "selector" "{" script+ "}"
 * parallel        ::= "parallel" "{" script+ "}"
 * action          ::= identifier "(" parameters ")"
 * parameters      ::= parameter ("," parameter)*
 * parameter       ::= key "=" value
 * value           ::= string | number | boolean
 * condition       ::= "condition" "(" expression ")" "{" script "}"
 * loop            ::= "repeat" number "{" script "}"
 * if              ::= "if" condition "{" script "}" ("else" "{" script "}")?
 * </pre>
 *
 * <p><b>Architecture:</b></p>
 * <ul>
 *   <li>{@link ScriptLexer} - Lexical analysis and token scanning</li>
 *   <li>{@link YAMLFormatParser} - YAML-like format parsing</li>
 *   <li>{@link BraceFormatParser} - Brace-based format parsing</li>
 *   <li>{@link ScriptASTBuilder} - AST construction utilities</li>
 *   <li>{@link ScriptValidator} - Validation and error checking</li>
 * </ul>
 *
 * @see Script
 * @see ScriptNode
 * @see ScriptLexer
 * @see YAMLFormatParser
 * @see BraceFormatParser
 * @since 1.3.0
 */
public class ScriptParser {

    private static final Logger LOGGER = TestLogger.getLogger(ScriptParser.class);

    /**
     * Parses a script from DSL format.
     *
     * <p>Automatically detects format (YAML-like or brace-based) and delegates
     * to the appropriate parser.</p>
     *
     * @param scriptSource The DSL script source
     * @return Parsed Script object
     * @throws ScriptParseException if parsing fails
     */
    public static Script parse(String scriptSource) throws ScriptParseException {
        ScriptLexer lexer = new ScriptLexer(scriptSource);
        lexer.skipWhitespace();

        // Detect format and delegate to appropriate parser
        if (lexer.peekKeyword("metadata:") || lexer.peekKeyword("parameters:") || lexer.peekKeyword("script:")) {
            // YAML-like format
            YAMLFormatParser parser = new YAMLFormatParser(lexer);
            return parser.parse();
        } else {
            // Brace format - parse as node and wrap in script
            ScriptNode rootNode = parseNode(scriptSource);
            return ScriptASTBuilder.createScriptWithDefaults(rootNode);
        }
    }

    /**
     * Parses a script node from DSL format.
     *
     * <p>Supports both brace-based and YAML-like node syntax.</p>
     *
     * @param nodeSource The DSL node source
     * @return Parsed ScriptNode object
     * @throws ScriptParseException if parsing fails
     */
    public static ScriptNode parseNode(String nodeSource) throws ScriptParseException {
        ScriptLexer lexer = new ScriptLexer(nodeSource);
        BraceFormatParser parser = new BraceFormatParser(lexer);

        ScriptNode node = parser.parseNode();
        lexer.skipWhitespace();

        if (!lexer.isAtEnd()) {
            throw lexer.error("Unexpected content after node: " + lexer.peek());
        }

        return node;
    }
}
