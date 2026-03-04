package com.minewright.script;

import java.util.*;

/**
 * Builder for constructing Script AST nodes.
 *
 * <p>Provides factory methods for creating ScriptNode instances
 * with proper validation and default values.</p>
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Creating script nodes with proper structure</li>
 *   <li>Creating action nodes with validation</li>
 *   <li>Creating selector nodes with children</li>
 *   <li>Ensuring AST consistency</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class ScriptASTBuilder {

    /**
     * Creates a script node with the specified type.
     *
     * @param type The node type
     * @return A new ScriptNode builder
     */
    public static ScriptNode.Builder createNode(ScriptNode.NodeType type) {
        return ScriptNode.builder().type(type);
    }

    /**
     * Creates an action node with the given name.
     *
     * @param actionName The action name
     * @return A new action node
     */
    public static ScriptNode createAction(String actionName) {
        return ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction(actionName)
            .build();
    }

    /**
     * Creates an action node with parameters.
     *
     * @param actionName The action name
     * @param parameters The action parameters
     * @return A new action node
     */
    public static ScriptNode createAction(String actionName, Map<String, Object> parameters) {
        return ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction(actionName)
            .parameters(parameters)
            .build();
    }

    /**
     * Creates a sequence node with children.
     *
     * @param children The child nodes
     * @return A new sequence node
     */
    public static ScriptNode createSequence(ScriptNode... children) {
        return ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .children(Arrays.asList(children))
            .build();
    }

    /**
     * Creates a selector node with children.
     *
     * @param children The child nodes
     * @return A new selector node
     */
    public static ScriptNode createSelector(ScriptNode... children) {
        return ScriptNode.builder()
            .type(ScriptNode.NodeType.SELECTOR)
            .children(Arrays.asList(children))
            .build();
    }

    /**
     * Creates a parallel node with children.
     *
     * @param children The child nodes
     * @return A new parallel node
     */
    public static ScriptNode createParallel(ScriptNode... children) {
        return ScriptNode.builder()
            .type(ScriptNode.NodeType.PARALLEL)
            .children(Arrays.asList(children))
            .build();
    }

    /**
     * Creates a condition node.
     *
     * @param condition The condition expression
     * @return A new condition node
     */
    public static ScriptNode createCondition(String condition) {
        return ScriptNode.builder()
            .type(ScriptNode.NodeType.CONDITION)
            .withCondition(condition)
            .build();
    }

    /**
     * Creates a loop node.
     *
     * @param iterations The number of iterations
     * @param body The loop body
     * @return A new loop node
     */
    public static ScriptNode createLoop(int iterations, ScriptNode body) {
        return ScriptNode.builder()
            .type(ScriptNode.NodeType.LOOP)
            .addParameter("iterations", iterations)
            .addChild(body)
            .build();
    }

    /**
     * Creates an if-then-else node.
     *
     * @param condition The condition expression
     * @param thenBranch The then branch
     * @param elseBranch The else branch (optional)
     * @return A new if-else node
     */
    public static ScriptNode createIfElse(String condition, ScriptNode thenBranch, ScriptNode elseBranch) {
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
     * Wraps a single node in a sequence if needed.
     *
     * @param nodes The nodes to wrap
     * @return A sequence node if multiple nodes, otherwise the single node
     */
    public static ScriptNode wrapInSequence(List<ScriptNode> nodes) {
        if (nodes.isEmpty()) {
            return ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build();
        } else if (nodes.size() == 1) {
            return nodes.get(0);
        } else {
            return ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .children(nodes)
                .build();
        }
    }

    /**
     * Wraps a single node in a sequence if needed.
     *
     * @param nodes The nodes to wrap
     * @return A sequence node if multiple nodes, otherwise the single node
     */
    public static ScriptNode wrapInSequence(ScriptNode... nodes) {
        return wrapInSequence(Arrays.asList(nodes));
    }

    /**
     * Creates a complete Script object.
     *
     * @param metadata The script metadata
     * @param scriptNode The root script node
     * @return A new Script
     */
    public static Script createScript(Script.ScriptMetadata metadata, ScriptNode scriptNode) {
        return Script.builder()
            .metadata(metadata)
            .scriptNode(scriptNode)
            .version("1.0.0")
            .build();
    }

    /**
     * Creates a Script with default metadata.
     *
     * @param scriptNode The root script node
     * @return A new Script with auto-generated metadata
     */
    public static Script createScriptWithDefaults(ScriptNode scriptNode) {
        Script.ScriptMetadata metadata = Script.ScriptMetadata.builder()
            .id("script-" + UUID.randomUUID().toString().substring(0, 8))
            .name("Generated Script")
            .description("Auto-generated from DSL")
            .createdAt(java.time.Instant.now())
            .build();

        Script.ScriptRequirements requirements = Script.ScriptRequirements.builder()
            .build();

        return Script.builder()
            .metadata(metadata)
            .requirements(requirements)
            .scriptNode(scriptNode)
            .version("1.0.0")
            .build();
    }

    /**
     * Validates a node structure.
     *
     * @param node The node to validate
     * @throws ScriptParseException if the node is invalid
     */
    public static void validateNode(ScriptNode node) throws ScriptParseException {
        if (node.getType() == null) {
            throw new ScriptParseException("Node type cannot be null");
        }

        // Validate action nodes have action name
        if (node.getType() == ScriptNode.NodeType.ACTION && node.getAction() == null) {
            throw new ScriptParseException("Action node must have an action name");
        }

        // Validate condition nodes have condition
        if (node.getType() == ScriptNode.NodeType.CONDITION && node.getCondition() == null) {
            throw new ScriptParseException("Condition node must have a condition expression");
        }

        // Validate if nodes have condition
        if (node.getType() == ScriptNode.NodeType.IF && node.getCondition() == null) {
            throw new ScriptParseException("If node must have a condition expression");
        }

        // Validate if nodes have at least a then branch
        if (node.getType() == ScriptNode.NodeType.IF && node.getChildren().isEmpty()) {
            throw new ScriptParseException("If node must have at least a then branch");
        }

        // Recursively validate children
        for (ScriptNode child : node.getChildren()) {
            validateNode(child);
        }
    }

    /**
     * Creates a deep copy of a node.
     *
     * @param node The node to copy
     * @return A deep copy of the node
     */
    public static ScriptNode copyNode(ScriptNode node) {
        return node.copy();
    }

    /**
     * Merges multiple nodes into a sequence.
     *
     * @param nodes The nodes to merge
     * @return A sequence containing all nodes
     */
    public static ScriptNode mergeIntoSequence(List<ScriptNode> nodes) {
        List<ScriptNode> flattened = new ArrayList<>();

        for (ScriptNode node : nodes) {
            if (node.getType() == ScriptNode.NodeType.SEQUENCE) {
                flattened.addAll(node.getChildren());
            } else {
                flattened.add(node);
            }
        }

        return createSequence(flattened.toArray(new ScriptNode[0]));
    }

    /**
     * Creates a repeat-until node.
     *
     * @param condition The exit condition
     * @param body The loop body
     * @return A new repeat-until node
     */
    public static ScriptNode createRepeatUntil(String condition, ScriptNode body) {
        return ScriptNode.builder()
            .type(ScriptNode.NodeType.REPEAT_UNTIL)
            .withCondition(condition)
            .addChild(body)
            .build();
    }
}
