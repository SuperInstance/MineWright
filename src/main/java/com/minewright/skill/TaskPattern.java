package com.minewright.skill;

import com.minewright.action.Task;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a discovered pattern in a sequence of executed tasks.
 *
 * <p><b>Purpose:</b></p>
 * <p>TaskPattern analyzes successful task sequences to identify reusable patterns.
 * These patterns become the foundation for new skills via the SkillGenerator.</p>
 *
 * <p><b>Pattern Detection:</b></p>
 * <p>Identifies:</p>
 * <ul>
 *   <li><b>Repeating Sequences:</b> Same actions repeated (e.g., mine, place, mine, place)</li>
 *   <li><b>Parameterized Patterns:</b> Actions with incrementing values (e.g., x+1, x+2)</li>
 *   <li><b>Conditional Branching:</b> Different actions based on state</li>
 *   <li><b>Loops:</b> Repeated action with similar parameters</li>
 * </ul>
 *
 * <p><b>Example Patterns:</b></p>
 * <pre>
 * // Staircase pattern (detected loop)
 * Task 1: mine at (0, 60, 0)
 * Task 2: place torch at (0, 60, 0)
 * Task 3: pathfind to (0, 59, 1)
 * Task 4: mine at (0, 59, 1)
 * Task 5: place torch at (0, 59, 1)
 * → Pattern: Loop with y decrement, z increment
 * </pre>
 *
 * @see SkillGenerator
 * @see Skill
 * @since 1.0.0
 */
public class TaskPattern {
    private final String name;
    private final String description;
    private final List<TaskStep> steps;
    private final Map<String, PatternVariable> variables;
    private final PatternType type;
    private final int frequency;
    private final double successRate;

    /**
     * Types of patterns that can be detected.
     */
    public enum PatternType {
        /** Repeating sequence of actions */
        LOOP,
        /** Actions with parameter relationships */
        SEQUENCE,
        /** Conditional branching based on state */
        CONDITIONAL,
        /** Single action with variable parameters */
        PARAMETERIZED,
        /** Complex multi-action pattern */
        COMPLEX
    }

    /**
     * Represents a single step in a task pattern.
     */
    public static class TaskStep {
        private final String action;
        private final Map<String, ParameterPattern> parameters;
        private final int index;

        public TaskStep(String action, Map<String, ParameterPattern> parameters, int index) {
            this.action = action;
            this.parameters = parameters;
            this.index = index;
        }

        public String getAction() {
            return action;
        }

        public Map<String, ParameterPattern> getParameters() {
            return parameters;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return "Step{" + index + ": " + action + " " + parameters + "}";
        }
    }

    /**
     * Describes how a parameter varies in a pattern.
     */
    public static class ParameterPattern {
        private final String name;
        private final ParameterType type;
        private final Object baseValue;
        private final Number increment;

        public enum ParameterType {
            /** Parameter stays constant */
            CONSTANT,
            /** Parameter increments by fixed amount */
            INCREMENTING,
            /** Parameter follows a formula */
            FORMULA,
            /** Parameter is variable/unpredictable */
            VARIABLE
        }

        public ParameterPattern(String name, ParameterType type, Object baseValue, Number increment) {
            this.name = name;
            this.type = type;
            this.baseValue = baseValue;
            this.increment = increment;
        }

        public String getName() {
            return name;
        }

        public ParameterType getType() {
            return type;
        }

        public Object getBaseValue() {
            return baseValue;
        }

        public Number getIncrement() {
            return increment;
        }

        @Override
        public String toString() {
            return name + "=" + type + (increment != null ? "(+" + increment + ")" : "");
        }
    }

    /**
     * Represents a variable discovered in a pattern.
     */
    public static class PatternVariable {
        private final String name;
        private final Class<?> type;
        private final Object defaultValue;

        public PatternVariable(String name, Class<?> type, Object defaultValue) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public Class<?> getType() {
            return type;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    private TaskPattern(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.steps = List.copyOf(builder.steps);
        this.variables = Map.copyOf(builder.variables);
        this.type = builder.type;
        this.frequency = builder.frequency;
        this.successRate = builder.successRate;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<TaskStep> getSteps() {
        return steps;
    }

    public Map<String, PatternVariable> getVariables() {
        return variables;
    }

    public PatternType getType() {
        return type;
    }

    public int getFrequency() {
        return frequency;
    }

    public double getSuccessRate() {
        return successRate;
    }

    /**
     * Generates a unique signature for this pattern.
     * Used for duplicate detection.
     */
    public String getSignature() {
        StringBuilder sig = new StringBuilder();
        sig.append(type).append(":");
        for (TaskStep step : steps) {
            sig.append(step.getAction()).append(";");
        }
        return sig.toString();
    }

    /**
     * Checks if this pattern is similar to another pattern.
     * Used for merging related patterns.
     */
    public boolean isSimilarTo(TaskPattern other) {
        if (this.type != other.type) {
            return false;
        }

        if (this.steps.size() != other.steps.size()) {
            return false;
        }

        // Compare action sequences
        for (int i = 0; i < steps.size(); i++) {
            if (!steps.get(i).getAction().equals(other.steps.get(i).getAction())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a builder for constructing a TaskPattern.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for TaskPattern.
     */
    public static class Builder {
        private String name;
        private String description;
        private final List<TaskStep> steps = new ArrayList<>();
        private final Map<String, PatternVariable> variables = new HashMap<>();
        private PatternType type = PatternType.SEQUENCE;
        private int frequency = 1;
        private double successRate = 1.0;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder addStep(TaskStep step) {
            this.steps.add(step);
            return this;
        }

        public Builder addStep(String action, Map<String, ParameterPattern> parameters, int index) {
            this.steps.add(new TaskStep(action, parameters, index));
            return this;
        }

        public Builder variable(String name, Class<?> type, Object defaultValue) {
            this.variables.put(name, new PatternVariable(name, type, defaultValue));
            return this;
        }

        public Builder type(PatternType type) {
            this.type = type;
            return this;
        }

        public Builder frequency(int frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder successRate(double successRate) {
            this.successRate = successRate;
            return this;
        }

        public TaskPattern build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Pattern name cannot be empty");
            }
            if (steps.isEmpty()) {
                throw new IllegalArgumentException("Pattern must have at least one step");
            }
            return new TaskPattern(this);
        }
    }

    /**
     * Analyzes a list of tasks to detect patterns.
     *
     * @param tasks Task sequence to analyze
     * @return Detected patterns, or empty list if none found
     */
    public static List<TaskPattern> detectPatterns(List<Task> tasks) {
        if (tasks.size() < 3) {
            return List.of(); // Need at least 3 tasks to detect a pattern
        }

        List<TaskPattern> patterns = new ArrayList<>();

        // Detect repeating loops
        patterns.addAll(detectLoopPatterns(tasks));

        // Detect sequences
        patterns.addAll(detectSequencePatterns(tasks));

        // Detect parameterized patterns
        patterns.addAll(detectParameterizedPatterns(tasks));

        return patterns;
    }

    /**
     * Detects loop patterns (repeating action sequences).
     */
    private static List<TaskPattern> detectLoopPatterns(List<Task> tasks) {
        List<TaskPattern> patterns = new ArrayList<>();

        // Look for same action repeated with incrementing coordinates
        Map<String, List<Integer>> actionIndices = new HashMap<>();

        for (int i = 0; i < tasks.size(); i++) {
            String action = tasks.get(i).getAction();
            actionIndices.computeIfAbsent(action, k -> new ArrayList<>()).add(i);
        }

        // Check for repeated actions
        for (Map.Entry<String, List<Integer>> entry : actionIndices.entrySet()) {
            List<Integer> indices = entry.getValue();
            if (indices.size() >= 3) {
                // Check if parameters follow a pattern
                Task firstTask = tasks.get(indices.get(0));
                Task secondTask = tasks.get(indices.get(1));
                Task thirdTask = tasks.get(indices.get(2));

                if (hasIncrementingPattern(firstTask, secondTask, thirdTask)) {
                    TaskPattern pattern = createLoopPattern(entry.getKey(), indices, tasks);
                    if (pattern != null) {
                        patterns.add(pattern);
                    }
                }
            }
        }

        return patterns;
    }

    /**
     * Detects sequence patterns (ordered action sequences).
     */
    private static List<TaskPattern> detectSequencePatterns(List<Task> tasks) {
        List<TaskPattern> patterns = new ArrayList<>();

        // Look for common action sequences
        // e.g., "pathfind" → "mine" → "place"

        for (int i = 0; i < tasks.size() - 2; i++) {
            String action1 = tasks.get(i).getAction();
            String action2 = tasks.get(i + 1).getAction();
            String action3 = tasks.get(i + 2).getAction();

            // Check if this sequence repeats later
            for (int j = i + 3; j < tasks.size() - 2; j++) {
                if (tasks.get(j).getAction().equals(action1) &&
                    tasks.get(j + 1).getAction().equals(action2) &&
                    tasks.get(j + 2).getAction().equals(action3)) {

                    // Found a repeating sequence
                    String patternName = action1 + "_" + action2 + "_" + action3;
                    TaskPattern pattern = TaskPattern.builder()
                        .name(patternName)
                        .description("Sequence: " + action1 + " → " + action2 + " → " + action3)
                        .type(PatternType.SEQUENCE)
                        .frequency(2)
                        .build();

                    patterns.add(pattern);
                    break;
                }
            }
        }

        return patterns;
    }

    /**
     * Detects parameterized patterns (same action, different parameters).
     */
    private static List<TaskPattern> detectParameterizedPatterns(List<Task> tasks) {
        List<TaskPattern> patterns = new ArrayList<>();

        // Group tasks by action type
        Map<String, List<Task>> byAction = tasks.stream()
            .collect(Collectors.groupingBy(Task::getAction));

        for (Map.Entry<String, List<Task>> entry : byAction.entrySet()) {
            List<Task> sameActionTasks = entry.getValue();
            if (sameActionTasks.size() >= 3) {
                // Analyze parameter variation
                Map<String, ParameterPattern> paramPatterns = analyzeParameterVariation(sameActionTasks);
                if (!paramPatterns.isEmpty()) {
                    TaskPattern pattern = TaskPattern.builder()
                        .name(entry.getKey() + "_pattern")
                        .description("Parameterized " + entry.getKey())
                        .type(PatternType.PARAMETERIZED)
                        .frequency(sameActionTasks.size())
                        .build();

                    patterns.add(pattern);
                }
            }
        }

        return patterns;
    }

    /**
     * Checks if three tasks show an incrementing pattern.
     */
    private static boolean hasIncrementingPattern(Task t1, Task t2, Task t3) {
        // Check x coordinate pattern
        Integer x1 = t1.getIntParameter("x", 0);
        Integer x2 = t2.getIntParameter("x", 0);
        Integer x3 = t3.getIntParameter("x", 0);

        if (x2 - x1 == x3 - x2 && x2 != x1) {
            return true; // Incrementing by same amount
        }

        // Check y coordinate pattern
        Integer y1 = t1.getIntParameter("y", 0);
        Integer y2 = t2.getIntParameter("y", 0);
        Integer y3 = t3.getIntParameter("y", 0);

        if (y2 - y1 == y3 - y2 && y2 != y1) {
            return true;
        }

        // Check z coordinate pattern
        Integer z1 = t1.getIntParameter("z", 0);
        Integer z2 = t2.getIntParameter("z", 0);
        Integer z3 = t3.getIntParameter("z", 0);

        if (z2 - z1 == z3 - z2 && z2 != z1) {
            return true;
        }

        return false;
    }

    /**
     * Creates a loop pattern from detected indices.
     */
    private static TaskPattern createLoopPattern(String action, List<Integer> indices, List<Task> tasks) {
        if (indices.isEmpty()) {
            return null;
        }

        Task firstTask = tasks.get(indices.get(0));
        Task secondTask = tasks.get(indices.get(1));

        // Calculate increment
        int xInc = secondTask.getIntParameter("x", 0) - firstTask.getIntParameter("x", 0);
        int yInc = secondTask.getIntParameter("y", 0) - firstTask.getIntParameter("y", 0);
        int zInc = secondTask.getIntParameter("z", 0) - firstTask.getIntParameter("z", 0);

        return TaskPattern.builder()
            .name(action + "_loop")
            .description("Looping " + action + " with increments")
            .type(PatternType.LOOP)
            .frequency(indices.size())
            .variable("iterations", Integer.class, indices.size())
            .variable("xIncrement", Integer.class, xInc)
            .variable("yIncrement", Integer.class, yInc)
            .variable("zIncrement", Integer.class, zInc)
            .build();
    }

    /**
     * Analyzes how parameters vary across a list of tasks.
     */
    private static Map<String, ParameterPattern> analyzeParameterVariation(List<Task> tasks) {
        Map<String, ParameterPattern> patterns = new HashMap<>();

        if (tasks.isEmpty()) {
            return patterns;
        }

        Task first = tasks.get(0);

        for (String paramKey : first.getParameters().keySet()) {
            Object firstValue = first.getParameter(paramKey);

            // Check if parameter is constant
            boolean isConstant = tasks.stream()
                .allMatch(t -> Objects.equals(t.getParameter(paramKey), firstValue));

            if (isConstant) {
                patterns.put(paramKey, new ParameterPattern(
                    paramKey,
                    ParameterPattern.ParameterType.CONSTANT,
                    firstValue,
                    null
                ));
                continue;
            }

            // Check if parameter increments
            if (firstValue instanceof Number) {
                boolean isIncrementing = true;
                Number increment = null;

                for (int i = 1; i < Math.min(3, tasks.size()); i++) {
                    Number prev = (Number) tasks.get(i - 1).getParameter(paramKey);
                    Number curr = (Number) tasks.get(i).getParameter(paramKey);

                    if (curr == null || prev == null) {
                        isIncrementing = false;
                        break;
                    }

                    double currInc = curr.doubleValue() - prev.doubleValue();
                    if (increment == null) {
                        increment = currInc;
                    } else if (Math.abs(currInc - increment.doubleValue()) > 0.001) {
                        isIncrementing = false;
                        break;
                    }
                }

                if (isIncrementing && increment != null) {
                    patterns.put(paramKey, new ParameterPattern(
                        paramKey,
                        ParameterPattern.ParameterType.INCREMENTING,
                        firstValue,
                        increment
                    ));
                }
            }
        }

        return patterns;
    }

    @Override
    public String toString() {
        return "TaskPattern{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", steps=" + steps.size() +
            ", frequency=" + frequency +
            ", successRate=" + successRate +
            '}';
    }
}
