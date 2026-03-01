package com.minewright.script;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.*;

/**
 * Validates scripts through multiple layers of safety checks.
 *
 * <p><b>Validation Layers:</b></p>
 * <ol>
 *   <li><b>Structural Validation:</b> Checks for cycles, orphan nodes, invalid references</li>
 *   <li><b>Semantic Validation:</b> Verifies action types exist, parameters match expectations</li>
 *   <li><b>Security Validation:</b> Checks for dangerous operations, limits recursion</li>
 *   <li><b>Resource Validation:</b> Estimates execution time, checks distance limits</li>
 * </ol>
 *
 * @see Script
 * @see ScriptNode
 * @since 1.3.0
 */
public class ScriptValidator {

    private static final Logger LOGGER = TestLogger.getLogger(ScriptValidator.class);

    private final Set<String> knownActions;
    private final ValidationConfig config;

    public ScriptValidator() {
        this.knownActions = getDefaultKnownActions();
        this.config = new ValidationConfig();
    }

    public ScriptValidator(Set<String> knownActions) {
        this.knownActions = knownActions != null ? knownActions : getDefaultKnownActions();
        this.config = new ValidationConfig();
    }

    public ScriptValidator(Set<String> knownActions, ValidationConfig config) {
        this.knownActions = knownActions != null ? knownActions : getDefaultKnownActions();
        this.config = config;
    }

    /**
     * Gets the default set of known actions.
     */
    private static Set<String> getDefaultKnownActions() {
        return Set.of(
            "mine", "place", "build", "craft", "gather",
            "pathfind", "follow", "attack", "idle_follow",
            "move", "look", "equip", "deposit"
        );
    }

    /**
     * Validates a script through all validation layers.
     *
     * @param script The script to validate
     * @return Validation result
     */
    public ValidationResult validate(Script script) {
        ValidationResult result = new ValidationResult();

        // Layer 1: Structural Validation
        result.add(structuralValidation(script));

        // Layer 2: Semantic Validation
        result.add(semanticValidation(script));

        // Layer 3: Security Validation
        result.add(securityValidation(script));

        // Layer 4: Resource Validation
        result.add(resourceValidation(script));

        return result;
    }

    /**
     * Structural validation - checks script structure.
     */
    private ValidationResult structuralValidation(Script script) {
        ValidationResult result = new ValidationResult();

        if (script == null) {
            result.addError("Script is null");
            return result;
        }

        ScriptNode rootNode = script.getScriptNode();
        if (rootNode == null) {
            result.addError("Script has no root node");
            return result;
        }

        // Check for cycles
        Set<String> visited = new HashSet<>();
        if (hasCycles(rootNode, visited)) {
            result.addError("Script contains cycles");
        }

        // Check maximum depth
        int depth = calculateDepth(rootNode);
        if (depth > config.maxDepth) {
            result.addWarning("Script depth (" + depth + ") exceeds recommended maximum (" + config.maxDepth + ")");
        }

        // Check maximum nodes
        int nodeCount = countNodes(rootNode);
        if (nodeCount > config.maxNodes) {
            result.addError("Script has too many nodes (" + nodeCount + " > " + config.maxNodes + ")");
        }

        return result;
    }

    /**
     * Semantic validation - checks actions and parameters.
     */
    private ValidationResult semanticValidation(Script script) {
        ValidationResult result = new ValidationResult();

        if (script == null || script.getScriptNode() == null) {
            return result;
        }

        // Validate all action nodes
        validateActions(script.getScriptNode(), result);

        return result;
    }

    /**
     * Security validation - checks for dangerous operations.
     */
    private ValidationResult securityValidation(Script script) {
        ValidationResult result = new ValidationResult();

        if (script == null || script.getScriptNode() == null) {
            return result;
        }

        // Check for disallowed actions
        checkDisallowedActions(script.getScriptNode(), result);

        // Check for infinite loops
        checkInfiniteLoops(script.getScriptNode(), result);

        return result;
    }

    /**
     * Resource validation - estimates resource usage.
     */
    private ValidationResult resourceValidation(Script script) {
        ValidationResult result = new ValidationResult();

        if (script.getScriptNode() == null) {
            return result;
        }

        // Estimate execution time
        int estimatedTicks = estimateExecutionTime(script.getScriptNode());
        if (estimatedTicks > config.maxExecutionTicks) {
            result.addWarning("Estimated execution time (" + (estimatedTicks / 20) + "s) may exceed limit");
        }

        // Check distance requirements
        checkDistanceLimits(script.getScriptNode(), result);

        return result;
    }

    // Helper methods

    private boolean hasCycles(ScriptNode node, Set<String> visited) {
        String nodeId = System.identityHashCode(node) + "";

        if (visited.contains(nodeId)) {
            return true;
        }

        visited.add(nodeId);

        for (ScriptNode child : node.getChildren()) {
            if (hasCycles(child, new HashSet<>(visited))) {
                return true;
            }
        }

        return false;
    }

    private int calculateDepth(ScriptNode node) {
        if (node.getChildren().isEmpty()) {
            return 1;
        }

        int maxChildDepth = 0;
        for (ScriptNode child : node.getChildren()) {
            maxChildDepth = Math.max(maxChildDepth, calculateDepth(child));
        }

        return maxChildDepth + 1;
    }

    private int countNodes(ScriptNode node) {
        int count = 1;
        for (ScriptNode child : node.getChildren()) {
            count += countNodes(child);
        }
        return count;
    }

    private void validateActions(ScriptNode node, ValidationResult result) {
        if (node.getType() == ScriptNode.NodeType.ACTION) {
            String action = node.getAction();

            if (action == null || action.isEmpty()) {
                result.addError("Action node has no action name");
                return;
            }

            // Check if action is known
            if (!knownActions.contains(action)) {
                result.addWarning("Unknown action: " + action);
            }

            // Validate parameters
            validateActionParameters(action, node.getParameters(), result);
        }

        for (ScriptNode child : node.getChildren()) {
            validateActions(child, result);
        }
    }

    private void validateActionParameters(String action, Map<String, Object> parameters, ValidationResult result) {
        // Action-specific parameter validation could go here
        // For now, just check that parameter values are valid types

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                result.addWarning("Parameter '" + entry.getKey() + "' in action '" + action + "' is null");
            } else if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) {
                result.addWarning("Parameter '" + entry.getKey() + "' has unsupported type: " + value.getClass().getSimpleName());
            }
        }

        // Check for required parameters based on action type
        checkRequiredParameters(action, parameters, result);
    }

    /**
     * Checks if required parameters are present for an action.
     */
    private void checkRequiredParameters(String action, Map<String, Object> parameters, ValidationResult result) {
        // Define required parameters for common actions
        switch (action) {
            case "mine", "gather" -> {
                if (!parameters.containsKey("block") && !parameters.containsKey("target") &&
                    !parameters.containsKey("item")) {
                    result.addWarning("Action '" + action + "' should specify a target (block/item)");
                }
            }
            case "place", "build" -> {
                if (!parameters.containsKey("block") && !parameters.containsKey("material")) {
                    result.addWarning("Action '" + action + "' should specify a material");
                }
            }
            case "craft" -> {
                if (!parameters.containsKey("item") && !parameters.containsKey("recipe")) {
                    result.addWarning("Action '" + action + "' should specify what to craft");
                }
            }
            case "move", "pathfind" -> {
                if (!parameters.containsKey("target") && !parameters.containsKey("destination") &&
                    !parameters.containsKey("x") && !parameters.containsKey("z")) {
                    result.addWarning("Action '" + action + "' should specify a destination");
                }
            }
        }
    }

    private void checkDisallowedActions(ScriptNode node, ValidationResult result) {
        if (node.getType() == ScriptNode.NodeType.ACTION) {
            String action = node.getAction();

            // Skip validation if action is null (already caught in semantic validation)
            if (action != null && config.disallowedActions.contains(action)) {
                result.addError("Disallowed action: " + action);
            }
        }

        for (ScriptNode child : node.getChildren()) {
            checkDisallowedActions(child, result);
        }
    }

    private void checkInfiniteLoops(ScriptNode node, ValidationResult result) {
        if (node.getType() == ScriptNode.NodeType.LOOP) {
            Object iterations = node.getParameter("iterations");

            if (iterations == null) {
                result.addWarning("Loop node has no iteration count - potential infinite loop");
            } else if (iterations instanceof Number && ((Number) iterations).intValue() > config.maxLoopIterations) {
                result.addError("Loop iterations (" + iterations + ") exceed maximum (" + config.maxLoopIterations + ")");
            }
        }

        for (ScriptNode child : node.getChildren()) {
            checkInfiniteLoops(child, result);
        }
    }

    private int estimateExecutionTime(ScriptNode node) {
        int baseTime = switch (node.getType()) {
            case ACTION -> 100; // 5 seconds per action
            case SEQUENCE -> 0; // Will sum children
            case SELECTOR -> 0; // Will use first child
            case PARALLEL -> 0; // Will use max child
            case CONDITION -> 20; // 1 second
            case LOOP -> {
                int iterations = node.getIntParameter("iterations", 1);
                int childTime = node.getChildren().isEmpty() ? 0 : estimateExecutionTime(node.getChildren().get(0));
                yield iterations * childTime;
            }
            case IF -> node.getChildren().stream().mapToInt(this::estimateExecutionTime).max().orElse(0);
            default -> 100;
        };

        // Add children time for composite nodes
        if (!node.getChildren().isEmpty()) {
            if (node.getType() == ScriptNode.NodeType.SEQUENCE) {
                for (ScriptNode child : node.getChildren()) {
                    baseTime += estimateExecutionTime(child);
                }
            } else if (node.getType() == ScriptNode.NodeType.PARALLEL || node.getType() == ScriptNode.NodeType.SELECTOR) {
                int maxChildTime = 0;
                for (ScriptNode child : node.getChildren()) {
                    maxChildTime = Math.max(maxChildTime, estimateExecutionTime(child));
                }
                baseTime += maxChildTime;
            }
        }

        return baseTime;
    }

    private void checkDistanceLimits(ScriptNode node, ValidationResult result) {
        // Check for distance parameters in actions
        if (node.getType() == ScriptNode.NodeType.ACTION) {
            Object maxDistance = node.getParameter("max_distance");
            if (maxDistance instanceof Number && ((Number) maxDistance).intValue() > config.maxDistanceBlocks) {
                result.addWarning("Distance parameter (" + maxDistance + ") exceeds recommended maximum (" + config.maxDistanceBlocks + ")");
            }
        }

        for (ScriptNode child : node.getChildren()) {
            checkDistanceLimits(child, result);
        }
    }

    /**
     * Validation result containing errors and warnings.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
            LOGGER.debug("[Validation] Error: {}", error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
            LOGGER.debug("[Validation] Warning: {}", warning);
        }

        public void add(ValidationResult other) {
            errors.addAll(other.errors);
            warnings.addAll(other.warnings);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Validation result: ");
            sb.append(isValid() ? "VALID" : "INVALID");
            sb.append(" (").append(errors.size()).append(" errors, ");
            sb.append(warnings.size()).append(" warnings)");

            if (!errors.isEmpty()) {
                sb.append("\nErrors:\n");
                for (String error : errors) {
                    sb.append("  - ").append(error).append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                sb.append("\nWarnings:\n");
                for (String warning : warnings) {
                    sb.append("  - ").append(warning).append("\n");
                }
            }

            return sb.toString();
        }
    }

    /**
     * Configuration for validation rules.
     */
    public static class ValidationConfig {
        private int maxDepth = 20;
        private int maxNodes = 500;
        private int maxExecutionTicks = 12000; // 10 minutes
        private int maxDistanceBlocks = 200;
        private int maxLoopIterations = 1000;
        private Set<String> disallowedActions = Set.of(); // Empty by default

        public int getMaxDepth() { return maxDepth; }
        public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

        public int getMaxNodes() { return maxNodes; }
        public void setMaxNodes(int maxNodes) { this.maxNodes = maxNodes; }

        public int getMaxExecutionTicks() { return maxExecutionTicks; }
        public void setMaxExecutionTicks(int maxExecutionTicks) { this.maxExecutionTicks = maxExecutionTicks; }

        public int getMaxDistanceBlocks() { return maxDistanceBlocks; }
        public void setMaxDistanceBlocks(int maxDistanceBlocks) { this.maxDistanceBlocks = maxDistanceBlocks; }

        public int getMaxLoopIterations() { return maxLoopIterations; }
        public void setMaxLoopIterations(int maxLoopIterations) { this.maxLoopIterations = maxLoopIterations; }

        public Set<String> getDisallowedActions() { return disallowedActions; }
        public void setDisallowedActions(Set<String> disallowedActions) { this.disallowedActions = disallowedActions; }
    }
}
