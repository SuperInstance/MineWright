package com.minewright.skill;

import com.minewright.action.Task;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * A skill composed of multiple simpler skills executed in sequence.
 *
 * <p><b>Purpose:</b></p>
 * <p>ComposedSkill represents a complex, multi-step skill that is built
 * by combining simpler skills. This enables the creation of sophisticated
 * behaviors without writing new code.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * // "Mine iron and craft pickaxe" composition
 * ComposedSkill skill = new ComposedSkill(
 *     "mineIronAndCraft",
 *     "Mine iron ore and craft a pickaxe",
 *     "mining",
 *     Arrays.asList(
 *         new CompositionStep("stripMine", stripMineSkill, ironContext),
 *         new CompositionStep("collectDrops", collectSkill, emptyContext),
 *         new CompositionStep("craftItem", craftSkill, pickaxeContext)
 *     ),
 *     baseContext
 * );
 * </pre>
 *
 * @see SkillComposer
 * @see CompositionStep
 * @since 1.1.0
 */
public class ComposedSkill implements Skill {
    private final String name;
    private final String description;
    private final String category;
    private final List<CompositionStep> steps;
    private final Map<String, Object> baseContext;

    // Thread-safe success tracking
    private final AtomicInteger executionCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastExecutionTime = new AtomicLong(0);

    /**
     * Creates a new composed skill.
     *
     * @param name Unique skill name
     * @param description Human-readable description
     * @param category Skill category
     * @param steps Ordered list of composition steps
     * @param baseContext Base context variables for all steps
     */
    public ComposedSkill(String name, String description, String category,
                         List<CompositionStep> steps, Map<String, Object> baseContext) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.description = description != null ? description : "";
        this.category = category != null ? category : "composed";
        this.steps = List.copyOf(steps);
        this.baseContext = Map.copyOf(baseContext);
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

    /**
     * Gets the ordered list of composition steps.
     *
     * @return Unmodifiable list of steps
     */
    public List<CompositionStep> getSteps() {
        return steps;
    }

    /**
     * Gets the number of steps in this composition.
     *
     * @return Step count
     */
    public int getStepCount() {
        return steps.size();
    }

    /**
     * Gets the base context variables.
     *
     * @return Unmodifiable map of context variables
     */
    public Map<String, Object> getBaseContext() {
        return baseContext;
    }

    /**
     * Gets a unique signature for this composition.
     * Used for caching and duplicate detection.
     *
     * @return Composition signature
     */
    public String getSignature() {
        String stepNames = steps.stream()
            .map(CompositionStep::getSkillName)
            .collect(Collectors.joining("|"));
        return category + ":" + name + ":" + stepNames.hashCode();
    }

    @Override
    public List<String> getRequiredActions() {
        return steps.stream()
            .flatMap(step -> step.getSkill().getRequiredActions().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getRequiredItems() {
        return steps.stream()
            .flatMap(step -> step.getSkill().getRequiredItems().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public int getEstimatedTicks() {
        return steps.stream()
            .mapToInt(step -> step.getSkill().getEstimatedTicks())
            .sum();
    }

    @Override
    public String generateCode(Map<String, Object> context) {
        // Merge base context with provided context
        Map<String, Object> mergedContext = new HashMap<>(baseContext);
        mergedContext.putAll(context);

        StringBuilder code = new StringBuilder();
        code.append("// Composed skill: ").append(name).append("\n");
        code.append("// Steps: ").append(getStepCount()).append("\n\n");

        for (int i = 0; i < steps.size(); i++) {
            CompositionStep step = steps.get(i);
            code.append("// Step ").append(i + 1).append(": ").append(step.getSkillName()).append("\n");

            Map<String, Object> stepContext = new HashMap<>(mergedContext);
            stepContext.putAll(step.getContext());

            String stepCode = step.getSkill().generateCode(stepContext);
            code.append(stepCode).append("\n\n");
        }

        return code.toString();
    }

    @Override
    public boolean isApplicable(Task task) {
        // A composition is applicable if ANY of its component skills is applicable
        return steps.stream()
            .anyMatch(step -> step.getSkill() != null && step.getSkill().isApplicable(task));
    }

    @Override
    public double getSuccessRate() {
        int total = executionCount.get();
        if (total == 0) {
            return 1.0; // Assume success for untested compositions
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
     * Gets the names of all component skills.
     *
     * @return List of skill names
     */
    public List<String> getComponentSkillNames() {
        return steps.stream()
            .map(CompositionStep::getSkillName)
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("ComposedSkill[name=%s, steps=%d, category=%s, successRate=%.2f%%]",
            name, getStepCount(), category, getSuccessRate() * 100);
    }
}
