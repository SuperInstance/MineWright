package com.minewright.script;

import java.util.*;

/**
 * A node in a script's behavior tree.
 *
 * <p><b>Node Types:</b></p>
 * <ul>
 *   <li><b>SEQUENCE:</b> Execute children in order, all must succeed</li>
 *   <li><b>SELECTOR:</b> Try children in order until one succeeds</li>
 *   <li><b>PARALLEL:</b> Execute all children simultaneously</li>
 *   <li><b>ACTION:</b> Execute a single atomic action</li>
 *   <li><b>CONDITION:</b> Check if a condition is true</li>
 *   <li><b>LOOP:</b> Repeat child nodes N times</li>
 *   <li><b>REPEAT_UNTIL:</b> Repeat until condition is met</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class ScriptNode {

    /**
     * Node types for behavior tree construction.
     */
    public enum NodeType {
        /** Execute children in order, all must succeed */
        SEQUENCE,

        /** Try children in order until one succeeds */
        SELECTOR,

        /** Execute all children simultaneously */
        PARALLEL,

        /** Execute a single atomic action */
        ACTION,

        /** Check if a condition is true */
        CONDITION,

        /** Repeat child nodes N times */
        LOOP,

        /** Repeat until condition is met */
        REPEAT_UNTIL,

        /** Execute if branch based on condition */
        IF
    }

    private final NodeType type;
    private final String action;
    private final String condition;
    private final Map<String, Object> parameters;
    private final List<ScriptNode> children;
    private final Map<String, String> metadata;

    private ScriptNode(Builder builder) {
        this.type = builder.type;
        this.action = builder.action;
        this.condition = builder.condition;
        this.parameters = Collections.unmodifiableMap(new HashMap<>(builder.parameters));
        this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    /**
     * Creates a new builder for constructing script nodes.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder initialized from this node.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    // Getters
    public NodeType getType() { return type; }
    public String getAction() { return action; }
    public String getCondition() { return condition; }
    public Map<String, Object> getParameters() { return parameters; }
    public List<ScriptNode> getChildren() { return children; }
    public Map<String, String> getMetadata() { return metadata; }

    /**
     * Gets a parameter value by key.
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * Gets a string parameter value.
     */
    public String getStringParameter(String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Gets an integer parameter value.
     */
    public int getIntParameter(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Creates a deep copy of this node.
     */
    public ScriptNode copy() {
        return this.toBuilder().build();
    }

    @Override
    public String toString() {
        return "ScriptNode{" +
            "type=" + type +
            ", action='" + action + '\'' +
            ", condition='" + condition + '\'' +
            ", children=" + children.size() +
            '}';
    }

    /**
     * Builder for constructing ScriptNode instances.
     */
    public static class Builder {
        private NodeType type;
        private String action;
        private String condition;
        private Map<String, Object> parameters = new HashMap<>();
        private List<ScriptNode> children = new ArrayList<>();
        private Map<String, String> metadata = new HashMap<>();

        private Builder() {}

        private Builder(ScriptNode existing) {
            this.type = existing.type;
            this.action = existing.action;
            this.condition = existing.condition;
            this.parameters = new HashMap<>(existing.parameters);
            this.children = new ArrayList<>(existing.children);
            this.metadata = new HashMap<>(existing.metadata);
        }

        public Builder type(NodeType type) {
            this.type = type;
            return this;
        }

        public Builder withAction(String action) {
            this.action = action;
            return this;
        }

        public Builder withCondition(String condition) {
            this.condition = condition;
            return this;
        }

        public Builder addParameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = new HashMap<>(parameters);
            return this;
        }

        public Builder addChild(ScriptNode child) {
            if (child != null) {
                this.children.add(child);
            }
            return this;
        }

        public Builder children(List<ScriptNode> children) {
            this.children = new ArrayList<>(children);
            return this;
        }

        public Builder addMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        /**
         * Creates a sequence node from the given children.
         */
        public static Builder sequence(ScriptNode... children) {
            return new Builder()
                .type(NodeType.SEQUENCE)
                .children(Arrays.asList(children));
        }

        /**
         * Creates a selector node from the given children.
         */
        public static Builder selector(ScriptNode... children) {
            return new Builder()
                .type(NodeType.SELECTOR)
                .children(Arrays.asList(children));
        }

        /**
         * Creates a parallel node from the given children.
         */
        public static Builder parallel(ScriptNode... children) {
            return new Builder()
                .type(NodeType.PARALLEL)
                .children(Arrays.asList(children));
        }

        /**
         * Creates an action node.
         */
        public static Builder action(String actionName, Map<String, Object> params) {
            return new Builder()
                .type(NodeType.ACTION)
                .withAction(actionName)
                .parameters(params);
        }

        /**
         * Creates a simple action node.
         */
        public static Builder simpleAction(String actionName) {
            return action(actionName, new HashMap<>());
        }

        /**
         * Creates a condition node.
         */
        public static Builder simpleCondition(String condition) {
            return new Builder()
                .type(NodeType.CONDITION)
                .withCondition(condition);
        }

        /**
         * Creates a loop node.
         */
        public static Builder loop(int iterations, ScriptNode body) {
            return new Builder()
                .type(NodeType.LOOP)
                .addParameter("iterations", iterations)
                .addChild(body);
        }

        /**
         * Creates a repeat-until node.
         */
        public static Builder repeatUntil(String condition, ScriptNode body) {
            return new Builder()
                .type(NodeType.REPEAT_UNTIL)
                .withCondition(condition)
                .addChild(body);
        }

        /**
         * Creates an if-else node.
         */
        public static Builder ifElse(String condition, ScriptNode thenNode, ScriptNode elseNode) {
            Builder builder = new Builder()
                .type(NodeType.IF)
                .withCondition(condition)
                .addChild(thenNode);

            if (elseNode != null) {
                builder.addChild(elseNode);
            }

            return builder;
        }

        public ScriptNode build() {
            if (type == null) {
                throw new IllegalStateException("Node type is required");
            }
            return new ScriptNode(this);
        }
    }

    /**
     * Visitor pattern for processing script nodes.
     */
    public interface Visitor {
        void visit(ScriptNode node);

        default void traverse(ScriptNode node) {
            visit(node);
            for (ScriptNode child : node.getChildren()) {
                traverse(child);
            }
        }
    }

    /**
     * Finds all nodes of a specific type.
     */
    public static List<ScriptNode> findNodesOfType(ScriptNode root, NodeType type) {
        List<ScriptNode> result = new ArrayList<>();
        Visitor visitor = new Visitor() {
            @Override
            public void visit(ScriptNode node) {
                if (node.getType() == type) {
                    result.add(node);
                }
            }
        };
        visitor.traverse(root);
        return result;
    }

    /**
     * Finds all nodes with a specific action.
     */
    public static List<ScriptNode> findNodesWithAction(ScriptNode root, String action) {
        List<ScriptNode> result = new ArrayList<>();
        Visitor visitor = new Visitor() {
            @Override
            public void visit(ScriptNode node) {
                if (action.equals(node.getAction())) {
                    result.add(node);
                }
            }
        };
        visitor.traverse(root);
        return result;
    }
}
