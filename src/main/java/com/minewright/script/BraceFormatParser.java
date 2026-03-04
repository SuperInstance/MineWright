package com.minewright.script;

import java.util.*;

/**
 * Parser for brace-based script format.
 *
 * <p>Parses scripts with C-style brace syntax for composite nodes.
 * This format is more compact and suitable for simple scripts.</p>
 *
 * <p><b>Format Structure:</b></p>
 * <pre>
 * sequence {
 *   mine(block=oak_log, count=10),
 *   craft(item=planks),
 *   if $has_planks {
 *     build(item=oak_planks)
 *   }
 * }
 * </pre>
 *
 * @since 1.3.0
 */
public class BraceFormatParser {

    private final ScriptLexer lexer;

    /**
     * Creates a new brace format parser.
     *
     * @param lexer The lexer to use for parsing
     */
    public BraceFormatParser(ScriptLexer lexer) {
        this.lexer = lexer;
    }

    /**
     * Parses a script node in brace format.
     *
     * @return The parsed script node
     * @throws ScriptParseException if parsing fails
     */
    public ScriptNode parseNode() throws ScriptParseException {
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
     * Parses a composite node (sequence, selector, parallel) with brace syntax.
     *
     * @param type The node type
     * @return The parsed node
     * @throws ScriptParseException if parsing fails
     */
    private ScriptNode parseCompositeNode(ScriptNode.NodeType type) throws ScriptParseException {
        lexer.skipWhitespace();

        // Brace style: sequence { ... }
        if (lexer.peekChar() == '{') {
            lexer.consume('{');
            List<ScriptNode> children = parseBraceContent();
            lexer.consume('}');
            return ScriptNode.builder()
                .type(type)
                .children(children)
                .build();
        }

        // Fallback to indented block
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
            // Identifier style: action: "name"
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
}
