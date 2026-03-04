package com.minewright.script;

import java.util.*;

// ScriptParseException is defined in ScriptParser

/**
 * Parser for YAML-like script format.
 *
 * <p>Parses scripts with YAML-style indentation-based syntax,
 * including metadata, parameters, requirements, and script sections.</p>
 *
 * <p><b>Format Structure:</b></p>
 * <pre>
 * metadata:
 *   id: script_id
 *   name: "Script Name"
 * parameters:
 *   - name: param1
 *     type: string
 * requirements:
 *   inventory:
 *     - item: oak_log
 *       quantity: 10
 * script:
 *   - type: ACTION
 *     action: mine
 * error_handling:
 *   error_type:
 *     - type: ACTION
 *       action: recover
 * </pre>
 *
 * @since 1.3.0
 */
public class YAMLFormatParser {

    private final ScriptLexer lexer;

    /**
     * Creates a new YAML format parser.
     *
     * @param lexer The lexer to use for parsing
     */
    public YAMLFormatParser(ScriptLexer lexer) {
        this.lexer = lexer;
    }

    /**
     * Parses a complete YAML-format script.
     *
     * @return The parsed Script
     * @throws ScriptParseException if parsing fails
     */
    public Script parse() throws ScriptParseException {
        Script.ScriptMetadata.Builder metadataBuilder = Script.ScriptMetadata.builder();
        Script.ScriptRequirements.Builder requirementsBuilder = Script.ScriptRequirements.builder();
        Map<String, List<ScriptNode>> errorHandlers = new HashMap<>();
        ScriptNode scriptNode = null;
        Map<String, Script.Parameter> parameters = new HashMap<>();

        lexer.skipWhitespace();

        // Parse metadata section
        if (lexer.peekKeyword("metadata:")) {
            lexer.consumeKeyword("metadata:");
            metadataBuilder = parseMetadata();
        }

        lexer.skipWhitespace();

        // Parse parameters section
        if (lexer.peekKeyword("parameters:")) {
            lexer.consumeKeyword("parameters:");
            parameters = parseParameters();
        }

        lexer.skipWhitespace();

        // Parse requirements section
        if (lexer.peekKeyword("requirements:")) {
            lexer.consumeKeyword("requirements:");
            requirementsBuilder = parseRequirements();
        }

        lexer.skipWhitespace();

        // Parse script section
        if (lexer.peekKeyword("script:")) {
            lexer.consumeKeyword("script:");
            lexer.skipWhitespace();
            scriptNode = parseScriptBlock();
        }

        lexer.skipWhitespace();

        // Parse error handling section
        if (lexer.peekKeyword("error_handling:")) {
            lexer.consumeKeyword("error_handling:");
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
     * Parses the metadata section.
     *
     * @return Metadata builder
     * @throws ScriptParseException if parsing fails
     */
    private Script.ScriptMetadata.Builder parseMetadata() throws ScriptParseException {
        Script.ScriptMetadata.Builder builder = Script.ScriptMetadata.builder();

        lexer.skipWhitespace();
        int indent = lexer.getCurrentIndent();

        while (!lexer.isAtEnd() && lexer.getCurrentIndent() >= indent) {
            lexer.skipWhitespace();

            if (lexer.peekKeyword("id:")) {
                lexer.consumeKeyword("id:");
                builder.id(lexer.parseStringValue());
            } else if (lexer.peekKeyword("name:")) {
                lexer.consumeKeyword("name:");
                String name = lexer.parseStringValue();
                builder.name(name);
            } else if (lexer.peekKeyword("description:")) {
                lexer.consumeKeyword("description:");
                builder.description(lexer.parseStringValue());
            } else if (lexer.peekKeyword("author:")) {
                lexer.consumeKeyword("author:");
                builder.author(lexer.parseStringValue());
            } else if (lexer.getCurrentIndent() < indent) {
                break;
            } else {
                // Skip unknown keys
                lexer.skipLine();
            }

            lexer.skipWhitespace();
        }

        return builder;
    }

    /**
     * Parses the parameters section.
     *
     * @return Map of parameter names to Parameter objects
     * @throws ScriptParseException if parsing fails
     */
    private Map<String, Script.Parameter> parseParameters() throws ScriptParseException {
        Map<String, Script.Parameter> parameters = new HashMap<>();

        lexer.skipWhitespace();
        int indent = lexer.getCurrentIndent();

        while (!lexer.isAtEnd() && lexer.getCurrentIndent() >= indent) {
            lexer.skipWhitespace();

            if (lexer.peekKeyword("-")) {
                lexer.consumeKeyword("-");
                lexer.skipWhitespace();

                // Parse parameter name (key "name" or just identifier)
                String paramName;
                String paramType = "string";
                Object defaultValue = null;

                // Check for "name:" syntax
                if (lexer.peekKeyword("name:")) {
                    lexer.consumeKeyword("name:");
                    paramName = lexer.parseStringValue();
                    lexer.skipWhitespace();

                    // Parse type if present
                    if (lexer.peekKeyword("type:")) {
                        lexer.consumeKeyword("type:");
                        paramType = lexer.parseStringValue();
                        lexer.skipWhitespace();
                    }

                    // Parse default if present
                    if (lexer.peekKeyword("default:")) {
                        lexer.consumeKeyword("default:");
                        defaultValue = lexer.parseValue();
                        lexer.skipWhitespace();
                    }
                } else {
                    // Parse as simple identifier
                    paramName = lexer.parseIdentifier();
                    lexer.skipWhitespace();
                    if (lexer.peekChar() == ':') {
                        lexer.consume(':');
                        lexer.skipWhitespace();
                        paramType = lexer.parseStringValue();
                    }
                }

                parameters.put(paramName, new Script.Parameter(paramName, paramType, defaultValue, true, ""));

            } else if (lexer.getCurrentIndent() < indent) {
                break;
            } else {
                lexer.skipLine();
            }

            lexer.skipWhitespace();
        }

        return parameters;
    }

    /**
     * Parses the requirements section.
     *
     * @return Requirements builder
     * @throws ScriptParseException if parsing fails
     */
    private Script.ScriptRequirements.Builder parseRequirements() throws ScriptParseException {
        Script.ScriptRequirements.Builder builder = Script.ScriptRequirements.builder();

        lexer.skipWhitespace();
        int indent = lexer.getCurrentIndent();

        while (!lexer.isAtEnd() && lexer.getCurrentIndent() >= indent) {
            lexer.skipWhitespace();

            if (lexer.peekKeyword("inventory:")) {
                lexer.consumeKeyword("inventory:");
                lexer.skipWhitespace();
                // Parse inventory items list
                while (!lexer.isAtEnd() && lexer.getCurrentIndent() > indent) {
                    lexer.skipWhitespace();
                    if (lexer.peekKeyword("-")) {
                        lexer.consumeKeyword("-");
                        String item = parseMapKey("item");
                        lexer.skipWhitespace();
                        if (lexer.peekKeyword("quantity:")) {
                            lexer.consumeKeyword("quantity:");
                            int quantity = lexer.parseIntValue();
                            builder.addInventoryItem(item, quantity);
                        }
                    }
                    lexer.skipWhitespace();
                }
            } else if (lexer.peekKeyword("tools:")) {
                lexer.consumeKeyword("tools:");
                lexer.skipWhitespace();
                // Parse tools list
                while (!lexer.isAtEnd() && lexer.getCurrentIndent() > indent) {
                    lexer.skipWhitespace();
                    if (lexer.peekKeyword("-")) {
                        lexer.consumeKeyword("-");
                        String tool = lexer.parseStringValue();
                        builder.addTool(tool);
                    }
                    lexer.skipWhitespace();
                }
            } else if (lexer.getCurrentIndent() < indent) {
                break;
            } else {
                lexer.skipLine();
            }

            lexer.skipWhitespace();
        }

        return builder;
    }

    /**
     * Parses the error handlers section.
     *
     * @return Map of error types to handler nodes
     * @throws ScriptParseException if parsing fails
     */
    private Map<String, List<ScriptNode>> parseErrorHandlers() throws ScriptParseException {
        Map<String, List<ScriptNode>> handlers = new HashMap<>();

        lexer.skipWhitespace();
        int indent = lexer.getCurrentIndent();

        while (!lexer.isAtEnd() && lexer.getCurrentIndent() >= indent) {
            lexer.skipWhitespace();

            String errorType = lexer.parseIdentifier();
            if (lexer.peekKeyword(":")) {
                lexer.consume(':');
                List<ScriptNode> nodes = parseNodeList();
                handlers.put(errorType, nodes);
            } else if (lexer.getCurrentIndent() < indent) {
                break;
            } else {
                lexer.skipLine();
            }

            lexer.skipWhitespace();
        }

        return handlers;
    }

    /**
     * Parses a script block (indented nodes).
     *
     * @return The root node of the block
     * @throws ScriptParseException if parsing fails
     */
    private ScriptNode parseScriptBlock() throws ScriptParseException {
        lexer.skipWhitespace();
        int baseIndent = lexer.getCurrentIndent();

        List<ScriptNode> nodes = new ArrayList<>();

        while (!lexer.isAtEnd()) {
            lexer.skipWhitespace();
            int currentIndent = lexer.getCurrentIndent();

            if (currentIndent < baseIndent) {
                break;
            }

            nodes.add(parseNode());
            lexer.skipWhitespace();
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
     *
     * @return List of parsed nodes
     * @throws ScriptParseException if parsing fails
     */
    private List<ScriptNode> parseNodeList() throws ScriptParseException {
        List<ScriptNode> nodes = new ArrayList<>();

        lexer.skipWhitespace();
        int baseIndent = lexer.getCurrentIndent();

        while (!lexer.isAtEnd() && lexer.getCurrentIndent() >= baseIndent) {
            nodes.add(parseNode());
            lexer.skipWhitespace();
        }

        return nodes;
    }

    /**
     * Parses a single script node.
     *
     * @return The parsed node
     * @throws ScriptParseException if parsing fails
     */
    private ScriptNode parseNode() throws ScriptParseException {
        lexer.skipWhitespace();

        if (lexer.peekKeyword("type:")) {
            return parseStructuredNode();
        }

        String keyword = lexer.parseIdentifier();

        return switch (keyword) {
            case "sequence" -> parseCompositeNode(ScriptNode.NodeType.SEQUENCE);
            case "selector" -> parseCompositeNode(ScriptNode.NodeType.SELECTOR);
            case "parallel" -> parseCompositeNode(ScriptNode.NodeType.PARALLEL);
            case "action" -> parseActionNode();
            case "condition" -> parseConditionNode();
            case "repeat" -> parseLoopNode();
            case "if" -> parseIfNode();
            default -> parseSimpleAction(keyword);
        };
    }

    /**
     * Parses a structured node (type: ... format).
     *
     * @return The parsed node
     * @throws ScriptParseException if parsing fails
     */
    private ScriptNode parseStructuredNode() throws ScriptParseException {
        ScriptNode.Builder builder = ScriptNode.builder();

        lexer.skipWhitespace();
        int baseIndent = lexer.getCurrentIndent();

        while (!lexer.isAtEnd() && lexer.getCurrentIndent() >= baseIndent) {
            lexer.skipWhitespace();

            String key = parseMapKey();

            if ("type".equals(key)) {
                String typeStr = lexer.parseStringValue();
                builder.type(ScriptNode.NodeType.valueOf(typeStr.toUpperCase()));
            } else if ("action".equals(key)) {
                builder.withAction(lexer.parseStringValue());
            } else if ("condition".equals(key)) {
                builder.withCondition(lexer.parseStringValue());
            } else if ("params".equals(key) || "parameters".equals(key)) {
                lexer.skipWhitespace();
                int paramIndent = lexer.getCurrentIndent();
                while (!lexer.isAtEnd() && lexer.getCurrentIndent() >= paramIndent) {
                    String paramKey = parseMapKey();
                    Object paramValue = lexer.parseValue();
                    builder.addParameter(paramKey, paramValue);
                    lexer.skipWhitespace();
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
                lexer.skipLine();
            }

            lexer.skipWhitespace();
        }

        return builder.build();
    }

    /**
     * Parses a composite node (sequence, selector, parallel).
     *
     * @param type The node type
     * @return The parsed node
     * @throws ScriptParseException if parsing fails
     */
    private ScriptNode parseCompositeNode(ScriptNode.NodeType type) throws ScriptParseException {
        lexer.skipWhitespace();

        // Check for brace style: sequence { ... }
        if (lexer.peekChar() == '{') {
            lexer.consume('{');
            List<ScriptNode> children = parseBraceContent();
            lexer.consume('}');
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
     *
     * @return The parsed action node
     * @throws ScriptParseException if parsing fails
     */
    private ScriptNode parseActionNode() throws ScriptParseException {
        lexer.skipWhitespace();

        // action "actionName" or action(actionName)
        String actionName;

        if (lexer.peekChar() == '(') {
            lexer.consume('(');
            actionName = lexer.parseStringValue();
            lexer.consume(')');

            Map<String, Object> params = new HashMap<>();

            if (lexer.peekChar() == ',' || lexer.peekChar() == '(') {
                if (lexer.peekChar() == ',') lexer.consume(',');
                params = parseParameterList();
            }

            return ScriptNode.builder()
                .type(ScriptNode.NodeType.ACTION)
                .withAction(actionName)
                .parameters(params)
                .build();

        } else {
            // YAML style: action: "name"
            lexer.skipWhitespace();
            if (lexer.peekKeyword("action:")) {
                lexer.consumeKeyword("action:");
                actionName = lexer.parseStringValue();
            } else {
                actionName = lexer.parseStringValue();
            }

            Map<String, Object> params = new HashMap<>();

            lexer.skipWhitespace();
            if (lexer.peekKeyword("params:") || lexer.peekKeyword("parameters:")) {
                String key = lexer.peekKeyword("params:") ? "params:" : "parameters:";
                lexer.consumeKeyword(key);
                int paramIndent = lexer.getCurrentIndent();
                while (!lexer.isAtEnd() && lexer.getCurrentIndent() >= paramIndent) {
                    String paramKey = parseMapKey();
                    Object paramValue = lexer.parseValue();
                    params.put(paramKey, paramValue);
                    lexer.skipWhitespace();
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
     *
     * @param actionName The action name
     * @return The parsed action node
     * @throws ScriptParseException if parsing fails
     */
    private ScriptNode parseSimpleAction(String actionName) throws ScriptParseException {
        lexer.skipWhitespace();

        if (lexer.peekChar() == '(') {
            lexer.consume('(');
            Map<String, Object> params = parseParameterList();
            lexer.consume(')');

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
     *
     * @return Map of parameter names to values
     * @throws ScriptParseException if parsing fails
     */
    private Map<String, Object> parseParameterList() throws ScriptParseException {
        Map<String, Object> params = new HashMap<>();

        lexer.skipWhitespace();
        while (!lexer.isAtEnd() && lexer.peekChar() != ')' && lexer.peekChar() != '}') {
            String key = lexer.parseIdentifier();
            lexer.skipWhitespace();

            if (lexer.peekChar() == '=') {
                lexer.consume('=');
                Object value = lexer.parseValue();
                params.put(key, value);
            }

            lexer.skipWhitespace();
            if (lexer.peekChar() == ',') {
                lexer.consume(',');
                lexer.skipWhitespace();
            } else {
                break;
            }
        }

        return params;
    }

    /**
     * Parses a condition node.
     *
     * @return The parsed condition node
     * @throws ScriptParseException if parsing fails
     */
    private ScriptNode parseConditionNode() throws ScriptParseException {
        lexer.skipWhitespace();

        String condition;
        if (lexer.peekChar() == '(') {
            lexer.consume('(');
            condition = lexer.parseStringValue();
            lexer.consume(')');
        } else {
            condition = lexer.parseStringValue();
        }

        return ScriptNode.builder()
            .type(ScriptNode.NodeType.CONDITION)
            .withCondition(condition)
            .build();
    }

    /**
     * Parses a loop node.
     *
     * @return The parsed loop node
     * @throws ScriptParseException if parsing fails
     */
    private ScriptNode parseLoopNode() throws ScriptParseException {
        lexer.skipWhitespace();

        int iterations = 1;

        // Parse iterations
        if (lexer.peekChar() == '(') {
            lexer.consume('(');
            iterations = lexer.parseIntValue();
            lexer.consume(')');
        } else if (lexer.peekChar() >= '0' && lexer.peekChar() <= '9') {
            iterations = lexer.parseIntValue();
        }

        lexer.skipWhitespace();

        // Parse body
        ScriptNode body;
        if (lexer.peekChar() == '{') {
            lexer.consume('{');
            List<ScriptNode> children = parseBraceContent();
            body = children.size() == 1 ? children.get(0) :
                ScriptNode.builder().type(ScriptNode.NodeType.SEQUENCE).children(children).build();
            lexer.consume('}');
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
     *
     * @return The parsed if-else node
     * @throws ScriptParseException if parsing fails
     */
    private ScriptNode parseIfNode() throws ScriptParseException {
        lexer.skipWhitespace();

        String condition;

        // Parse condition
        if (lexer.peekChar() == '(') {
            lexer.consume('(');
            condition = lexer.parseStringValue();
            lexer.consume(')');
        } else {
            condition = lexer.parseStringValue();
        }

        lexer.skipWhitespace();

        // Parse then branch
        ScriptNode thenBranch;
        if (lexer.peekChar() == '{') {
            lexer.consume('{');
            List<ScriptNode> thenChildren = parseBraceContent();
            thenBranch = thenChildren.size() == 1 ? thenChildren.get(0) :
                ScriptNode.builder().type(ScriptNode.NodeType.SEQUENCE).children(thenChildren).build();
            lexer.consume('}');
        } else {
            thenBranch = parseNode();
        }

        // Parse else branch if present
        ScriptNode elseBranch = null;
        lexer.skipWhitespace();
        if (lexer.peekKeyword("else")) {
            lexer.consumeKeyword("else");
            lexer.skipWhitespace();
            if (lexer.peekChar() == '{') {
                lexer.consume('{');
                List<ScriptNode> elseChildren = parseBraceContent();
                elseBranch = elseChildren.size() == 1 ? elseChildren.get(0) :
                    ScriptNode.builder().type(ScriptNode.NodeType.SEQUENCE).children(elseChildren).build();
                lexer.consume('}');
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
     *
     * @return List of child nodes
     * @throws ScriptParseException if parsing fails
     */
    private List<ScriptNode> parseBraceContent() throws ScriptParseException {
        List<ScriptNode> children = new ArrayList<>();

        lexer.skipWhitespace();
        while (!lexer.isAtEnd() && lexer.peekChar() != '}') {
            children.add(parseNode());
            lexer.skipWhitespace();

            // Allow comma separator between nodes
            if (lexer.peekChar() == ',') {
                lexer.consume(',');
                lexer.skipWhitespace();
            }
        }

        return children;
    }

    /**
     * Parses a map key (identifier followed by colon).
     *
     * @return The parsed key
     * @throws ScriptParseException if parsing fails
     */
    private String parseMapKey() throws ScriptParseException {
        lexer.skipWhitespace();
        String key = lexer.parseIdentifier();
        lexer.skipWhitespace();
        if (lexer.peekChar() == ':') {
            lexer.consume(':');
        }
        return key;
    }

    /**
     * Parses a map key and validates it matches the expected key.
     *
     * @param expectedKey The expected key value
     * @return The parsed key
     * @throws ScriptParseException if key doesn't match
     */
    private String parseMapKey(String expectedKey) throws ScriptParseException {
        lexer.skipWhitespace();
        String key = lexer.parseIdentifier();
        lexer.skipWhitespace();
        if (lexer.peekChar() == ':') {
            lexer.consume(':');
        }
        if (!expectedKey.equals(key)) {
            throw lexer.error("Expected key '" + expectedKey + "', got '" + key + "'");
        }
        return key;
    }
}
