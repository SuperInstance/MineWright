package com.minewright.skill;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import com.minewright.action.Task;
import com.minewright.execution.CodeExecutionEngine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Concrete implementation of a skill that can generate and execute JavaScript code.
 *
 * <p><b>Purpose:</b></p>
 * <p>ExecutableSkill stores a skill's metadata and JavaScript template code.
 * When executed, it generates task-specific code by substituting context
 * variables into the template, then executes it via GraalVM.</p>
 *
 * <p><b>Template Variables:</b></p>
 * <p>Templates use {{variable}} syntax for substitution:</p>
 * <pre>
 * // Template example for digStaircase skill
 * var depth = {{depth}};
 * var direction = "{{direction}}";
 * for (var i = 0; i < depth; i++) {
 *     steve.mineBlockAt(x, y - i, z);
 *     if (i % 3 == 0) steve.placeBlock("torch", x, y - i, z);
 * }
 * </pre>
 *
 * <p><b>Success Tracking:</b></p>
 * <p>Each skill tracks:</p>
 * <ul>
 *   <li>Total executions</li>
 *   <li>Successful executions</li>
 *   <li>Success rate (0.0 to 1.0)</li>
 *   <li>Last execution timestamp</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>Uses atomic counters for thread-safe success tracking.</p>
 *
 * @see Skill
 * @see SkillLibrary
 * @since 1.0.0
 */
public class ExecutableSkill implements Skill {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(ExecutableSkill.class);

    private final String name;
    private final String description;
    private final String category;
    private final String codeTemplate;
    private final List<String> requiredActions;
    private final List<String> requiredItems;
    private final int estimatedTicks;
    private final Pattern applicabilityPattern;

    // Thread-safe success tracking
    private final AtomicInteger executionCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastExecutionTime = new AtomicLong(0);

    /**
     * Creates a new executable skill.
     *
     * @param builder Builder containing all skill parameters
     */
    private ExecutableSkill(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.category = builder.category;
        this.codeTemplate = builder.codeTemplate;
        this.requiredActions = List.copyOf(builder.requiredActions);
        this.requiredItems = List.copyOf(builder.requiredItems);
        this.estimatedTicks = builder.estimatedTicks;
        this.applicabilityPattern = builder.applicabilityPattern != null
            ? Pattern.compile(builder.applicabilityPattern, Pattern.CASE_INSENSITIVE)
            : null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public List<String> getRequiredActions() {
        return requiredActions;
    }

    @Override
    public List<String> getRequiredItems() {
        return requiredItems;
    }

    @Override
    public int getEstimatedTicks() {
        return estimatedTicks;
    }

    /**
     * Generates executable code by substituting context variables into the template.
     *
     * <p><b>Substitution Rules:</b></p>
     * <ul>
     *   <li>{{variable}} - Replaced with toString() of context value</li>
 *     *   <li>{{variable:quote}} - Wraps string value in quotes with proper escaping</li>
     *   <li>Missing variables - Left as-is (causes runtime error)</li>
     * </ul>
     *
     * @param context Map of variable names to values
     * @return Generated JavaScript code ready for execution
     */
    @Override
    public String generateCode(Map<String, Object> context) {
        String code = codeTemplate;

        // Substitute context variables
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Regular substitution
            String placeholder = "{{" + key + "}}";
            String replacement = value != null ? value.toString() : "null";
            code = code.replace(placeholder, replacement);

            // Quoted substitution (for strings) - escapes special characters
            String quotedPlaceholder = "{{" + key + ":quote}}";
            String quotedReplacement;
            if (value != null) {
                // Escape backslashes and quotes for JavaScript string literals
                String escapedValue = value.toString()
                    .replace("\\", "\\\\")  // Escape backslashes first
                    .replace("\"", "\\\"")  // Escape double quotes
                    .replace("\n", "\\n")   // Escape newlines
                    .replace("\r", "\\r")   // Escape carriage returns
                    .replace("\t", "\\t");  // Escape tabs
                quotedReplacement = "\"" + escapedValue + "\"";
            } else {
                quotedReplacement = "null";
            }
            code = code.replace(quotedPlaceholder, quotedReplacement);
        }

        return code;
    }

    /**
     * Checks if this skill is applicable to the given task.
     *
     * <p>Uses regex pattern matching against task description and parameters.
     * If no pattern is defined, checks if required actions match.</p>
     *
     * @param task The task to check
     * @return true if this skill can handle the task
     */
    @Override
    public boolean isApplicable(Task task) {
        // Use regex pattern if defined - pattern takes precedence
        if (applicabilityPattern != null) {
            String taskString = task.toString().toLowerCase();
            Matcher matcher = applicabilityPattern.matcher(taskString);
            return matcher.find();
        }

        // Fallback: check if action type matches (only when no pattern is defined)
        return requiredActions.contains(task.getAction());
    }

    /**
     * Executes this skill with the given context.
     *
     * @param context Execution context variables
     * @param engine Code execution engine
     * @return Execution result
     */
    public CodeExecutionEngine.ExecutionResult execute(
            Map<String, Object> context,
            CodeExecutionEngine engine) {
        executionCount.incrementAndGet();
        lastExecutionTime.set(System.currentTimeMillis());

        String code = generateCode(context);
        LOGGER.debug("[Skill:{}] Executing code:\n{}", name, code);

        CodeExecutionEngine.ExecutionResult result = engine.execute(code);

        if (result.isSuccess()) {
            successCount.incrementAndGet();
        }

        return result;
    }

    @Override
    public double getSuccessRate() {
        int total = executionCount.get();
        if (total == 0) {
            return 1.0; // Assume success for untested skills
        }
        return (double) successCount.get() / total;
    }

    @Override
    public void recordSuccess(boolean success) {
        executionCount.incrementAndGet();
        if (success) {
            successCount.incrementAndGet();
        }
        lastExecutionTime.set(System.currentTimeMillis());
    }

    @Override
    public int getExecutionCount() {
        return executionCount.get();
    }

    /**
     * Gets the timestamp of the last execution.
     *
     * @return Unix timestamp in milliseconds, or 0 if never executed
     */
    public long getLastExecutionTime() {
        return lastExecutionTime.get();
    }

    /**
     * Creates a new builder for constructing an ExecutableSkill.
     *
     * @param name Unique skill name
     * @return Builder instance
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Builder pattern for constructing ExecutableSkill instances.
     */
    public static class Builder {
        private final String name;
        private String description = "";
        private String category = "utility";
        private String codeTemplate = "";
        private final List<String> requiredActions = new java.util.ArrayList<>();
        private final List<String> requiredItems = new java.util.ArrayList<>();
        private int estimatedTicks = 100;
        private String applicabilityPattern;

        private Builder(String name) {
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder codeTemplate(String template) {
            this.codeTemplate = template;
            return this;
        }

        public Builder requiredAction(String action) {
            this.requiredActions.add(action);
            return this;
        }

        public Builder requiredActions(String... actions) {
            this.requiredActions.addAll(List.of(actions));
            return this;
        }

        public Builder requiredItem(String item) {
            this.requiredItems.add(item);
            return this;
        }

        public Builder requiredItems(String... items) {
            this.requiredItems.addAll(List.of(items));
            return this;
        }

        public Builder estimatedTicks(int ticks) {
            this.estimatedTicks = ticks;
            return this;
        }

        public Builder applicabilityPattern(String pattern) {
            this.applicabilityPattern = pattern;
            return this;
        }

        /**
         * Builds the ExecutableSkill instance.
         *
         * @return New ExecutableSkill
         * @throws IllegalArgumentException if required fields are missing
         */
        public ExecutableSkill build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Skill name cannot be empty");
            }
            if (description == null || description.isEmpty()) {
                throw new IllegalArgumentException("Skill description cannot be empty");
            }
            if (codeTemplate == null || codeTemplate.isEmpty()) {
                throw new IllegalArgumentException("Code template cannot be empty");
            }
            return new ExecutableSkill(this);
        }
    }

    @Override
    public String toString() {
        return String.format("Skill[name=%s, category=%s, successRate=%.2f%%, executions=%d]",
            name, category, getSuccessRate() * 100, executionCount.get());
    }
}
