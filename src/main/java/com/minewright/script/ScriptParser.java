package com.minewright.script;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses DSL scripts into Script objects.
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
 * @see Script
 * @see ScriptNode
 * @since 1.3.0
 */
public class ScriptParser {

    private static final Logger LOGGER = TestLogger.getLogger(ScriptParser.class);

    private final String scriptSource;
    private int position;
    private final int length;

    public ScriptParser(String scriptSource) {
        this.scriptSource = scriptSource != null ? scriptSource : "";
        this.position = 0;
        this.length = this.scriptSource.length();
    }

    /**
     * Parses a script from DSL format.
     *
     * @param scriptSource The DSL script source
     * @return Parsed Script object
     * @throws ScriptParseException if parsing fails
     */
    public static Script parse(String scriptSource) throws ScriptParseException {
        return new ScriptParser(scriptSource).parse();
    }

    /**
     * Parses a script node from DSL format.
     *
     * @param nodeSource The DSL node source
     * @return Parsed ScriptNode object
     * @throws ScriptParseException if parsing fails
     */
    public static ScriptNode parseNode(String nodeSource) throws ScriptParseException {
        ScriptParser parser = new ScriptParser(nodeSource);
        ScriptNode node = parser.parseNode();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw parser.error("Unexpected content after node: " + parser.peek());
        }
        return node;
    }

    /**
     * Parses the script.
     */
    public Script parse() throws ScriptParseException {
        skipWhitespace();

        // Try parsing YAML-like format first
        if (peekKeyword("metadata:") || peekKeyword("parameters:") || peekKeyword("script:")) {
            return parseYAMLFormat();
        }

        // Fall back to simple DSL format
        ScriptNode rootNode = parseNode();

        Script.ScriptMetadata.Builder metadataBuilder = Script.ScriptMetadata.builder()
            .id("script-" + UUID.randomUUID().toString().substring(0, 8))
            .name("Generated Script")
            .description("Auto-generated from DSL")
            .createdAt(java.time.Instant.now());

        Script.ScriptRequirements.Builder requirementsBuilder = Script.ScriptRequirements.builder();

        return Script.builder()
            .metadata(metadataBuilder.build())
            .requirements(requirementsBuilder.build())
            .scriptNode(rootNode)
            .version("1.0.0")
            .build();
    }

    /**
     * Parses a YAML-like format script.
     */
    private Script parseYAMLFormat() throws ScriptParseException {
        Script.ScriptMetadata.Builder metadataBuilder = Script.ScriptMetadata.builder();
        Script.ScriptRequirements.Builder requirementsBuilder = Script.ScriptRequirements.builder();
        Map<String, List<ScriptNode>> errorHandlers = new HashMap<>();
        ScriptNode scriptNode = null;

        skipWhitespace();

        // Parse metadata section
        if (peekKeyword("metadata:")) {
            consumeKeyword("metadata:");
            metadataBuilder = parseMetadata();
        }

        skipWhitespace();

        // Parse parameters section
        Map<String, Script.Parameter> parameters = new HashMap<>();
        if (peekKeyword("parameters:")) {
            consumeKeyword("parameters:");
            parameters = parseParameters();
        }

        skipWhitespace();

        // Parse requirements section
        if (peekKeyword("requirements:")) {
            consumeKeyword("requirements:");
            requirementsBuilder = parseRequirements();
        }

        skipWhitespace();

        // Parse script section
        if (peekKeyword("script:")) {
            consumeKeyword("script:");
            skipWhitespace();
            scriptNode = parseScriptBlock();
        }

        skipWhitespace();

        // Parse error handling section
        if (peekKeyword("error_handling:")) {
            consumeKeyword("error_handling:");
            errorHandlers = parseErrorHandlers();
        }

        // Set default metadata if not provided
        if (metadataBuilder.build().getId() == null) {
            metadataBuilder.id("script-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (metadataBuilder.build().getName() == null) {
            metadataBuilder.name("Generated Script");
        }

        return Script.builder()
            .metadata(metadataBuilder.build())
            .parameters(parameters)
            .requirements(requirementsBuilder.build())
            .scriptNode(scriptNode)
            .errorHandlers(errorHandlers)
            .version("1.0.0")
            .build();
    }

    /**
     * Parses metadata section.
     */
    private Script.ScriptMetadata.Builder parseMetadata() throws ScriptParseException {
        Script.ScriptMetadata.Builder builder = Script.ScriptMetadata.builder();

        skipWhitespace();
        int indent = getCurrentIndent();

        while (!isAtEnd() && getCurrentIndent() >= indent) {
            skipWhitespace();

            if (peekKeyword("id:")) {
                consumeKeyword("id:");
                builder.id(parseStringValue());
            } else if (peekKeyword("name:")) {
                consumeKeyword("name:");
                // Allow multi-word names by reading quoted or unquoted string to end of line
                String name = parseStringValue();
                builder.name(name);
            } else if (peekKeyword("description:")) {
                consumeKeyword("description:");
                builder.description(parseStringValue());
            } else if (peekKeyword("author:")) {
                consumeKeyword("author:");
                builder.author(parseStringValue());
            } else if (getCurrentIndent() < indent) {
                break;
            } else {
                // Skip unknown keys
                skipLine();
            }

            skipWhitespace();
        }

        return builder;
    }

    /**
     * Parses parameters section.
     */
    private Map<String, Script.Parameter> parseParameters() throws ScriptParseException {
        Map<String, Script.Parameter> parameters = new HashMap<>();

        skipWhitespace();
        int indent = getCurrentIndent();

        while (!isAtEnd() && getCurrentIndent() >= indent) {
            skipWhitespace();

            if (peekKeyword("-")) {
                consumeKeyword("-");
                skipWhitespace();

                // Parse parameter name (key "name" or just identifier)
                String paramName;
                String paramType = "string";
                Object defaultValue = null;

                // Check for "name:" syntax
                if (peekKeyword("name:")) {
                    consumeKeyword("name:");
                    paramName = parseStringValue();
                    skipWhitespace();

                    // Parse type if present
                    if (peekKeyword("type:")) {
                        consumeKeyword("type:");
                        paramType = parseStringValue();
                        skipWhitespace();
                    }

                    // Parse default if present
                    if (peekKeyword("default:")) {
                        consumeKeyword("default:");
                        defaultValue = parseValue();
                        skipWhitespace();
                    }
                } else {
                    // Parse as simple identifier
                    paramName = parseIdentifier();
                    skipWhitespace();
                    if (peekChar() == ':') {
                        consume(':');
                        skipWhitespace();
                        paramType = parseStringValue();
                    }
                }

                parameters.put(paramName, new Script.Parameter(paramName, paramType, defaultValue, true, ""));

            } else if (getCurrentIndent() < indent) {
                break;
            } else {
                skipLine();
            }

            skipWhitespace();
        }

        return parameters;
    }

    /**
     * Parses requirements section.
     */
    private Script.ScriptRequirements.Builder parseRequirements() throws ScriptParseException {
        Script.ScriptRequirements.Builder builder = Script.ScriptRequirements.builder();

        skipWhitespace();
        int indent = getCurrentIndent();

        while (!isAtEnd() && getCurrentIndent() >= indent) {
            skipWhitespace();

            if (peekKeyword("inventory:")) {
                consumeKeyword("inventory:");
                skipWhitespace();
                // Parse inventory items list
                while (!isAtEnd() && getCurrentIndent() > indent) {
                    skipWhitespace();
                    if (peekKeyword("-")) {
                        consumeKeyword("-");
                        String item = parseMapKey("item");
                        skipWhitespace();
                        if (peekKeyword("quantity:")) {
                            consumeKeyword("quantity:");
                            int quantity = parseIntValue();
                            builder.addInventoryItem(item, quantity);
                        }
                    }
                    skipWhitespace();
                }
            } else if (peekKeyword("tools:")) {
                consumeKeyword("tools:");
                skipWhitespace();
                // Parse tools list
                while (!isAtEnd() && getCurrentIndent() > indent) {
                    skipWhitespace();
                    if (peekKeyword("-")) {
                        consumeKeyword("-");
                        String tool = parseStringValue();
                        builder.addTool(tool);
                    }
                    skipWhitespace();
                }
            } else if (getCurrentIndent() < indent) {
                break;
            } else {
                skipLine();
            }

            skipWhitespace();
        }

        return builder;
    }

    /**
     * Parses error handlers section.
     */
    private Map<String, List<ScriptNode>> parseErrorHandlers() throws ScriptParseException {
        Map<String, List<ScriptNode>> handlers = new HashMap<>();

        skipWhitespace();
        int indent = getCurrentIndent();

        while (!isAtEnd() && getCurrentIndent() >= indent) {
            skipWhitespace();

            String errorType = parseIdentifier();
            if (peekKeyword(":")) {
                consume(":");
                List<ScriptNode> nodes = parseNodeList();
                handlers.put(errorType, nodes);
            } else if (getCurrentIndent() < indent) {
                break;
            } else {
                skipLine();
            }

            skipWhitespace();
        }

        return handlers;
    }

    /**
     * Parses a script block (indented nodes).
     */
    private ScriptNode parseScriptBlock() throws ScriptParseException {
        skipWhitespace();
        int baseIndent = getCurrentIndent();

        List<ScriptNode> nodes = new ArrayList<>();

        while (!isAtEnd()) {
            skipWhitespace();
            int currentIndent = getCurrentIndent();

            if (currentIndent < baseIndent) {
                break;
            }

            nodes.add(parseNode());
            skipWhitespace();
        }

        // If we have multiple nodes at root level, wrap in sequence
        if (nodes.size() == 1) {
            return nodes.get(0);
        } else if (nodes.isEmpty()) {
            return ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build();
        } else {
            return ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .children(nodes)
                .build();
        }
    }

    /**
     * Parses a list of nodes.
     */
    private List<ScriptNode> parseNodeList() throws ScriptParseException {
        List<ScriptNode> nodes = new ArrayList<>();

        skipWhitespace();
        int baseIndent = getCurrentIndent();

        while (!isAtEnd() && getCurrentIndent() >= baseIndent) {
            nodes.add(parseNode());
            skipWhitespace();
        }

        return nodes;
    }

    /**
     * Parses a single script node.
     */
    private ScriptNode parseNode() throws ScriptParseException {
        skipWhitespace();

        if (peekKeyword("type:")) {
            return parseStructuredNode();
        }

        String keyword = parseIdentifier();

        return switch (keyword) {
            case "sequence" -> parseCompositeNode(ScriptNode.NodeType.SEQUENCE);
            case "selector" -> parseCompositeNode(ScriptNode.NodeType.SELECTOR);
            case "parallel" -> parseCompositeNode(ScriptNode.NodeType.PARALLEL);
            case "action" -> parseActionNode();
            case "condition" -> parseConditionNode();
            case "repeat" -> parseLoopNode();
            case "if" -> parseIfNode();
            default -> {
                // Might be a simple action call
                yield parseSimpleAction(keyword);
            }
        };
    }

    /**
     * Parses a structured node (type: ... format).
     */
    private ScriptNode parseStructuredNode() throws ScriptParseException {
        ScriptNode.Builder builder = ScriptNode.builder();

        skipWhitespace();
        int baseIndent = getCurrentIndent();

        while (!isAtEnd() && getCurrentIndent() >= baseIndent) {
            skipWhitespace();

            String key = parseMapKey();

            if ("type".equals(key)) {
                String typeStr = parseStringValue();
                builder.type(ScriptNode.NodeType.valueOf(typeStr.toUpperCase()));
            } else if ("action".equals(key)) {
                builder.withAction(parseStringValue());
            } else if ("condition".equals(key)) {
                builder.withCondition(parseStringValue());
            } else if ("params".equals(key) || "parameters".equals(key)) {
                skipWhitespace();
                int paramIndent = getCurrentIndent();
                while (!isAtEnd() && getCurrentIndent() >= paramIndent) {
                    String paramKey = parseMapKey();
                    Object paramValue = parseValue();
                    builder.addParameter(paramKey, paramValue);
                    skipWhitespace();
                }
            } else if ("children".equals(key)) {
                List<ScriptNode> children = parseNodeList();
                for (ScriptNode child : children) {
                    builder.addChild(child);
                }
            } else if ("steps".equals(key)) {
                List<ScriptNode> children = parseNodeList();
                for (ScriptNode child : children) {
                    builder.addChild(child);
                }
            } else {
                // Skip unknown key
                skipLine();
            }

            skipWhitespace();
        }

        return builder.build();
    }

    /**
     * Parses a composite node (sequence, selector, parallel).
     */
    private ScriptNode parseCompositeNode(ScriptNode.NodeType type) throws ScriptParseException {
        skipWhitespace();

        // Check for brace style: sequence { ... }
        if (peekChar() == '{') {
            consume('{');
            List<ScriptNode> children = parseBraceContent();
            consume('}');
            return ScriptNode.builder()
                .type(type)
                .children(children)
                .build();
        }

        // Otherwise, expect indented block
        List<ScriptNode> children = parseNodeList();

        return ScriptNode.builder()
            .type(type)
            .children(children)
            .build();
    }

    /**
     * Parses an action node.
     */
    private ScriptNode parseActionNode() throws ScriptParseException {
        skipWhitespace();

        // action "actionName" or action(actionName)
        String actionName;

        if (peekChar() == '(') {
            consume('(');
            actionName = parseStringValue();
            consume(')');

            Map<String, Object> params = new HashMap<>();

            if (peekChar() == ',' || peekChar() == '(') {
                if (peekChar() == ',') consume(',');
                params = parseParameterList();
            }

            return ScriptNode.builder()
                .type(ScriptNode.NodeType.ACTION)
                .withAction(actionName)
                .parameters(params)
                .build();

        } else {
            // YAML style: action: "name"
            skipWhitespace();
            if (peekKeyword("action:")) {
                consumeKeyword("action:");
                actionName = parseStringValue();
            } else {
                actionName = parseStringValue();
            }

            Map<String, Object> params = new HashMap<>();

            skipWhitespace();
            if (peekKeyword("params:") || peekKeyword("parameters:")) {
                String key = peekKeyword("params:") ? "params:" : "parameters:";
                consumeKeyword(key);
                int paramIndent = getCurrentIndent();
                while (!isAtEnd() && getCurrentIndent() >= paramIndent) {
                    String paramKey = parseMapKey();
                    Object paramValue = parseValue();
                    params.put(paramKey, paramValue);
                    skipWhitespace();
                }
            }

            return ScriptNode.builder()
                .type(ScriptNode.NodeType.ACTION)
                .withAction(actionName)
                .parameters(params)
                .build();
        }
    }

    /**
     * Parses a simple action node: actionName(...)
     */
    private ScriptNode parseSimpleAction(String actionName) throws ScriptParseException {
        skipWhitespace();

        if (peekChar() == '(') {
            consume('(');
            Map<String, Object> params = parseParameterList();
            consume(')');

            return ScriptNode.builder()
                .type(ScriptNode.NodeType.ACTION)
                .withAction(actionName)
                .parameters(params)
                .build();
        }

        return ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction(actionName)
            .build();
    }

    /**
     * Parses a parameter list.
     */
    private Map<String, Object> parseParameterList() throws ScriptParseException {
        Map<String, Object> params = new HashMap<>();

        skipWhitespace();
        while (!isAtEnd() && peekChar() != ')' && peekChar() != '}') {
            String key = parseIdentifier();
            skipWhitespace();

            if (peekChar() == '=') {
                consume('=');
                Object value = parseValue();
                params.put(key, value);
            }

            skipWhitespace();
            if (peekChar() == ',') {
                consume(',');
                skipWhitespace();
            } else {
                break;
            }
        }

        return params;
    }

    /**
     * Parses a condition node.
     */
    private ScriptNode parseConditionNode() throws ScriptParseException {
        skipWhitespace();

        String condition;
        if (peekChar() == '(') {
            consume('(');
            condition = parseStringValue();
            consume(')');
        } else {
            condition = parseStringValue();
        }

        return ScriptNode.builder()
            .type(ScriptNode.NodeType.CONDITION)
            .withCondition(condition)
            .build();
    }

    /**
     * Parses a loop node.
     */
    private ScriptNode parseLoopNode() throws ScriptParseException {
        skipWhitespace();

        int iterations = 1;

        // Parse iterations
        if (peekChar() == '(') {
            consume('(');
            iterations = parseIntValue();
            consume(')');
        } else if (peekChar() >= '0' && peekChar() <= '9') {
            iterations = parseIntValue();
        }

        skipWhitespace();

        // Parse body
        ScriptNode body;
        if (peekChar() == '{') {
            consume('{');
            List<ScriptNode> children = parseBraceContent();
            body = children.size() == 1 ? children.get(0) :
                ScriptNode.builder().type(ScriptNode.NodeType.SEQUENCE).children(children).build();
            consume('}');
        } else {
            body = parseNode();
        }

        return ScriptNode.builder()
            .type(ScriptNode.NodeType.LOOP)
            .addParameter("iterations", iterations)
            .addChild(body)
            .build();
    }

    /**
     * Parses an if-else node.
     */
    private ScriptNode parseIfNode() throws ScriptParseException {
        skipWhitespace();

        String condition;

        // Parse condition
        if (peekChar() == '(') {
            consume('(');
            condition = parseStringValue();
            consume(')');
        } else {
            condition = parseStringValue();
        }

        skipWhitespace();

        // Parse then branch
        ScriptNode thenBranch;
        if (peekChar() == '{') {
            consume('{');
            List<ScriptNode> thenChildren = parseBraceContent();
            thenBranch = thenChildren.size() == 1 ? thenChildren.get(0) :
                ScriptNode.builder().type(ScriptNode.NodeType.SEQUENCE).children(thenChildren).build();
            consume('}');
        } else {
            thenBranch = parseNode();
        }

        // Parse else branch if present
        ScriptNode elseBranch = null;
        skipWhitespace();
        if (peekKeyword("else")) {
            consumeKeyword("else");
            skipWhitespace();
            if (peekChar() == '{') {
                consume('{');
                List<ScriptNode> elseChildren = parseBraceContent();
                elseBranch = elseChildren.size() == 1 ? elseChildren.get(0) :
                    ScriptNode.builder().type(ScriptNode.NodeType.SEQUENCE).children(elseChildren).build();
                consume('}');
            } else {
                elseBranch = parseNode();
            }
        }

        ScriptNode.Builder builder = ScriptNode.builder()
            .type(ScriptNode.NodeType.IF)
            .withCondition(condition)
            .addChild(thenBranch);

        if (elseBranch != null) {
            builder.addChild(elseBranch);
        }

        return builder.build();
    }

    /**
     * Parses content within braces.
     */
    private List<ScriptNode> parseBraceContent() throws ScriptParseException {
        List<ScriptNode> children = new ArrayList<>();

        skipWhitespace();
        while (!isAtEnd() && peekChar() != '}') {
            children.add(parseNode());
            skipWhitespace();

            // Allow comma separator between nodes
            if (peekChar() == ',') {
                consume(',');
                skipWhitespace();
            }
        }

        return children;
    }

    // Utility methods

    private String parseMapKey() throws ScriptParseException {
        skipWhitespace();
        String key = parseIdentifier();
        skipWhitespace();
        if (peekChar() == ':') {
            consume(':');
        }
        return key;
    }

    private String parseMapKey(String expectedKey) throws ScriptParseException {
        skipWhitespace();
        String key = parseIdentifier();
        skipWhitespace();
        if (peekChar() == ':') {
            consume(':');
        }
        if (!expectedKey.equals(key)) {
            throw error("Expected key '" + expectedKey + "', got '" + key + "'");
        }
        return key;
    }

    private String parseIdentifier() throws ScriptParseException {
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

    private String parseStringValue() throws ScriptParseException {
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

    private String parseQuotedString() throws ScriptParseException {
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

    private int parseIntValue() throws ScriptParseException {
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

    private Object parseValue() throws ScriptParseException {
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

    private char peekChar() {
        return position < length ? scriptSource.charAt(position) : '\0';
    }

    /**
     * Peeks at the next character without consuming it.
     * This method is used by parseNode to check for unexpected content.
     */
    private char peek() {
        skipWhitespace();
        return peekChar();
    }

    private char consumeChar() {
        return scriptSource.charAt(position++);
    }

    private void consume(char expected) throws ScriptParseException {
        skipWhitespace();
        if (peekChar() != expected) {
            throw error("Expected '" + expected + "', found '" + peekChar() + "'");
        }
        position++;
    }

    private void consume(String str) throws ScriptParseException {
        for (char c : str.toCharArray()) {
            if (peekChar() != c) {
                throw error("Expected '" + str + "'");
            }
            position++;
        }
    }

    private boolean peekKeyword(String keyword) {
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

    private void consumeKeyword(String keyword) throws ScriptParseException {
        skipWhitespace();

        for (char c : keyword.toCharArray()) {
            if (peekChar() != c) {
                throw error("Expected keyword '" + keyword + "'");
            }
            position++;
        }
    }

    private void skipWhitespace() {
        while (!isAtEnd() && Character.isWhitespace(peekChar())) {
            position++;
        }
    }

    private void skipLine() {
        while (!isAtEnd() && peekChar() != '\n') {
            position++;
        }
        if (!isAtEnd()) {
            position++; // Skip newline
        }
    }

    private int getCurrentIndent() {
        int savedPos = position;
        int indent = 0;

        // Find start of current line
        while (savedPos > 0 && scriptSource.charAt(savedPos - 1) != '\n') {
            savedPos--;
        }

        // Count leading spaces/tabs
        while (savedPos < length && (scriptSource.charAt(savedPos) == ' ' || scriptSource.charAt(savedPos) == '\t')) {
            indent++;
            savedPos++;
        }

        return indent;
    }

    private boolean isAtEnd() {
        return position >= length;
    }

    private ScriptParseException error(String message) {
        int line = 1;
        int column = 1;

        // Calculate line and column
        for (int i = 0; i < position && i < length; i++) {
            if (scriptSource.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }

        return new ScriptParseException(message + " at line " + line + ", column " + column);
    }

    /**
     * Exception thrown when script parsing fails.
     */
    public static class ScriptParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public ScriptParseException(String message) {
            super(message);
        }
    }
}
