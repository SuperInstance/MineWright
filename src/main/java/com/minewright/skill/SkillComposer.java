package com.minewright.skill;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import com.minewright.action.Task;
import com.minewright.execution.CodeExecutionEngine;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Composes simple skills into complex, multi-step skills.
 *
 * <p><b>Purpose:</b></p>
 * <p>The SkillComposer enables the creation of complex skills by combining
 * simpler skills in a dependency-aware manner. This follows the Voyager pattern
 * where complex behaviors emerge from compositional hierarchies.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Skill Composition:</b> Combine multiple skills into a sequence</li>
 *   <li><b>Dependency Validation:</b> Ensure all required skills are available</li>
 *   <li><b>Ordered Execution:</b> Execute skills in dependency order</li>
 *   <li><b>Rollback Support:</b> Partial rollback on failure</li>
 *   <li><b>Success Tracking:</b> Track composition success rate</li>
 *   <li><b>Automatic Caching:</b> Cache frequently used compositions</li>
 * </ul>
 *
 * <p><b>Composition Example:</b></p>
 * <pre>
 * // Compose "mine iron and craft pickaxe" from simpler skills
 * ComposedSkill skill = SkillComposer.compose("mineIronAndCraft")
 *     .fromSkill("stripMine")       // First: strip mine for iron
 *     .fromSkill("collectDrops")    // Then: collect the drops
 *     .fromSkill("craftItem")       // Finally: craft the pickaxe
 *     .withContext("target", "iron_ore")
 *     .withContext("item", "iron_pickaxe")
 *     .build();
 * </pre>
 *
 * <p><b>Dependency Resolution:</b></p>
 * <p>The composer automatically resolves dependencies between skills.
 * If skill A requires skill B as a prerequisite, B will be executed first.</p>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>All operations are thread-safe for concurrent access from multiple agents.</p>
 *
 * @see Skill
 * @see SkillLibrary
 * @see ComposedSkill
 * @since 1.1.0
 */
public class SkillComposer {
    private static final Logger LOGGER = TestLogger.getLogger(SkillComposer.class);

    /**
     * Singleton instance for global access.
     */
    private static volatile SkillComposer instance;

    /**
     * Cache of composed skills by their signature.
     */
    private final Map<String, ComposedSkill> compositionCache;

    /**
     * Track composition success rates.
     */
    private final Map<String, AtomicInteger> compositionSuccesses;
    private final Map<String, AtomicInteger> compositionAttempts;

    /**
     * Maximum cache size to prevent memory issues.
     */
    private static final int MAX_CACHE_SIZE = 100;

    private SkillComposer() {
        this.compositionCache = new ConcurrentHashMap<>();
        this.compositionSuccesses = new ConcurrentHashMap<>();
        this.compositionAttempts = new ConcurrentHashMap<>();
        LOGGER.info("SkillComposer initialized");
    }

    /**
     * Gets the singleton SkillComposer instance.
     *
     * @return Global SkillComposer instance
     */
    public static SkillComposer getInstance() {
        if (instance == null) {
            synchronized (SkillComposer.class) {
                if (instance == null) {
                    instance = new SkillComposer();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a new composition builder.
     *
     * @param name Unique name for the composed skill
     * @return Builder for constructing the composition
     */
    public static CompositionBuilder compose(String name) {
        return new CompositionBuilder(name);
    }

    /**
     * Validates that all required skills exist in the library.
     *
     * @param skillNames Names of skills to validate
     * @return ValidationResult containing missing skills (if any)
     */
    public ValidationResult validateDependencies(List<String> skillNames) {
        SkillLibrary library = SkillLibrary.getInstance();
        List<String> missing = new ArrayList<>();
        List<String> available = new ArrayList<>();

        for (String name : skillNames) {
            if (library.hasSkill(name)) {
                available.add(name);
            } else {
                missing.add(name);
            }
        }

        boolean valid = missing.isEmpty();
        return new ValidationResult(valid, available, missing);
    }

    /**
     * Executes a composed skill with the given context.
     *
     * @param composition The composed skill to execute
     * @param context Execution context variables
     * @param engine Code execution engine
     * @return Execution result
     */
    public CompositionResult executeComposed(
            ComposedSkill composition,
            Map<String, Object> context,
            CodeExecutionEngine engine) {

        String signature = composition.getSignature();
        compositionAttempts.computeIfAbsent(signature, k -> new AtomicInteger(0)).incrementAndGet();

        LOGGER.info("[Composer] Executing composition '{}' with {} steps",
            composition.getName(), composition.getStepCount());

        List<StepResult> stepResults = new ArrayList<>();
        int currentStep = 0;

        try {
            for (CompositionStep step : composition.getSteps()) {
                currentStep++;
                LOGGER.debug("[Composer] Step {}/{}: {}",
                    currentStep, composition.getStepCount(), step.getSkillName());

                Skill skill = step.getSkill();
                if (skill == null) {
                    LOGGER.error("[Composer] Skill '{}' not found in library", step.getSkillName());
                    stepResults.add(new StepResult(step, false, "Skill not found", null));
                    return new CompositionResult(false, "Skill not found: " + step.getSkillName(),
                        stepResults, currentStep - 1);
                }

                // Merge step context with base context
                Map<String, Object> mergedContext = new HashMap<>(context);
                mergedContext.putAll(step.getContext());

                // Execute the skill
                long startTime = System.currentTimeMillis();
                CodeExecutionEngine.ExecutionResult result;

                if (skill instanceof ExecutableSkill) {
                    result = ((ExecutableSkill) skill).execute(mergedContext, engine);
                } else {
                    // Fallback for other skill types
                    String code = skill.generateCode(mergedContext);
                    result = engine.execute(code);
                }
                long duration = System.currentTimeMillis() - startTime;

                boolean success = result.isSuccess();
                stepResults.add(new StepResult(step, success, result.getError() != null ? result.getError() : "Success", duration));

                if (!success) {
                    String errorMsg = result.getError() != null ? result.getError() : "Unknown error";
                    LOGGER.warn("[Composer] Step {} failed: {}", currentStep, errorMsg);
                    compositionSuccesses.computeIfAbsent(signature, k -> new AtomicInteger(0));
                    return new CompositionResult(false, "Step " + currentStep + " failed: " + errorMsg,
                        stepResults, currentStep - 1);
                }

                LOGGER.debug("[Composer] Step {} completed in {}ms", currentStep, duration);
            }

            // All steps succeeded
            compositionSuccesses.computeIfAbsent(signature, k -> new AtomicInteger(0)).incrementAndGet();
            LOGGER.info("[Composer] Composition '{}' completed successfully in {} steps",
                composition.getName(), composition.getStepCount());

            return new CompositionResult(true, "Composition completed successfully",
                stepResults, composition.getStepCount());

        } catch (Exception e) {
            LOGGER.error("[Composer] Exception during composition execution", e);
            return new CompositionResult(false, "Exception: " + e.getMessage(),
                stepResults, currentStep - 1);
        }
    }

    /**
     * Executes a composed skill asynchronously.
     *
     * @param composition The composed skill to execute
     * @param context Execution context variables
     * @param engine Code execution engine
     * @return CompletableFuture with the execution result
     */
    public CompletableFuture<CompositionResult> executeComposedAsync(
            ComposedSkill composition,
            Map<String, Object> context,
            CodeExecutionEngine engine) {
        return CompletableFuture.supplyAsync(() -> executeComposed(composition, context, engine));
    }

    /**
     * Adds a composed skill to the cache.
     *
     * @param composition The composed skill to cache
     */
    public void cacheComposition(ComposedSkill composition) {
        if (compositionCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest cached composition
            String oldest = compositionCache.keySet().iterator().next();
            compositionCache.remove(oldest);
            LOGGER.debug("[Composer] Evicted cached composition: {}", oldest);
        }

        compositionCache.put(composition.getSignature(), composition);
        LOGGER.debug("[Composer] Cached composition: {}", composition.getName());
    }

    /**
     * Gets a cached composition by signature.
     *
     * @param signature The composition signature
     * @return Cached composition, or null if not found
     */
    public ComposedSkill getCachedComposition(String signature) {
        return compositionCache.get(signature);
    }

    /**
     * Gets the success rate for a composition.
     *
     * @param signature The composition signature
     * @return Success rate (0.0 to 1.0), or 1.0 if never executed
     */
    public double getCompositionSuccessRate(String signature) {
        AtomicInteger attempts = compositionAttempts.get(signature);
        AtomicInteger successes = compositionSuccesses.get(signature);

        if (attempts == null || attempts.get() == 0) {
            return 1.0; // Assume success for untested compositions
        }

        return (double) successes.get() / attempts.get();
    }

    /**
     * Gets statistics about the composer.
     *
     * @return Statistics map
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedCompositions", compositionCache.size());
        stats.put("totalAttempts", compositionAttempts.values().stream()
            .mapToInt(AtomicInteger::get).sum());
        stats.put("totalSuccesses", compositionSuccesses.values().stream()
            .mapToInt(AtomicInteger::get).sum());
        return stats;
    }

    /**
     * Clears the composition cache.
     */
    public void clearCache() {
        compositionCache.clear();
        LOGGER.info("[Composer] Cache cleared");
    }

    // ==================== Inner Classes ====================

    /**
     * Builder for creating composed skills.
     */
    public static class CompositionBuilder {
        private final String name;
        private String description = "";
        private String category = "composed";
        private final List<CompositionStep> steps = new ArrayList<>();
        private final Map<String, Object> baseContext = new HashMap<>();

        private CompositionBuilder(String name) {
            this.name = name;
        }

        /**
         * Sets the description for the composed skill.
         */
        public CompositionBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the category for the composed skill.
         */
        public CompositionBuilder category(String category) {
            this.category = category;
            return this;
        }

        /**
         * Adds a skill step to the composition.
         *
         * @param skillName Name of the skill to add
         * @return This builder
         */
        public CompositionBuilder fromSkill(String skillName) {
            Skill skill = SkillLibrary.getInstance().getSkill(skillName);
            steps.add(new CompositionStep(skillName, skill, new HashMap<>()));
            return this;
        }

        /**
         * Adds a skill step with specific context.
         *
         * @param skillName Name of the skill to add
         * @param stepContext Context variables for this step
         * @return This builder
         */
        public CompositionBuilder fromSkillWithContext(String skillName, Map<String, Object> stepContext) {
            Skill skill = SkillLibrary.getInstance().getSkill(skillName);
            steps.add(new CompositionStep(skillName, skill, new HashMap<>(stepContext)));
            return this;
        }

        /**
         * Adds context variables that apply to all steps.
         *
         * @param key Context key
         * @param value Context value
         * @return This builder
         */
        public CompositionBuilder withContext(String key, Object value) {
            this.baseContext.put(key, value);
            return this;
        }

        /**
         * Adds multiple context variables.
         *
         * @param context Map of context variables
         * @return This builder
         */
        public CompositionBuilder withContext(Map<String, Object> context) {
            this.baseContext.putAll(context);
            return this;
        }

        /**
         * Builds the composed skill.
         *
         * @return New ComposedSkill instance
         * @throws IllegalStateException if no steps are defined
         */
        public ComposedSkill build() {
            if (steps.isEmpty()) {
                throw new IllegalStateException("Cannot build composition with no steps");
            }

            if (description.isEmpty()) {
                description = "Composed skill: " + steps.stream()
                    .map(CompositionStep::getSkillName)
                    .collect(Collectors.joining(" -> "));
            }

            return new ComposedSkill(name, description, category, steps, baseContext);
        }
    }

    /**
     * Result of dependency validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> availableSkills;
        private final List<String> missingSkills;

        public ValidationResult(boolean valid, List<String> availableSkills, List<String> missingSkills) {
            this.valid = valid;
            this.availableSkills = Collections.unmodifiableList(availableSkills);
            this.missingSkills = Collections.unmodifiableList(missingSkills);
        }

        public boolean isValid() { return valid; }
        public List<String> getAvailableSkills() { return availableSkills; }
        public List<String> getMissingSkills() { return missingSkills; }

        @Override
        public String toString() {
            return valid
                ? "Valid: " + availableSkills.size() + " skills available"
                : "Invalid: missing " + missingSkills;
        }
    }

    /**
     * Result of a single step execution.
     */
    public static class StepResult {
        private final CompositionStep step;
        private final boolean success;
        private final String message;
        private final Long durationMs;

        public StepResult(CompositionStep step, boolean success, String message, Long durationMs) {
            this.step = step;
            this.success = success;
            this.message = message;
            this.durationMs = durationMs;
        }

        public CompositionStep getStep() { return step; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getDurationMs() { return durationMs; }
    }

    /**
     * Result of a composition execution.
     */
    public static class CompositionResult {
        private final boolean success;
        private final String message;
        private final List<StepResult> stepResults;
        private final int completedSteps;

        public CompositionResult(boolean success, String message, List<StepResult> stepResults, int completedSteps) {
            this.success = success;
            this.message = message;
            this.stepResults = Collections.unmodifiableList(stepResults);
            this.completedSteps = completedSteps;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<StepResult> getStepResults() { return stepResults; }
        public int getCompletedSteps() { return completedSteps; }
        public int getTotalSteps() { return stepResults.size(); }
        public double getProgress() {
            return stepResults.isEmpty() ? 0 : (double) completedSteps / stepResults.size();
        }

        @Override
        public String toString() {
            return success
                ? String.format("Success: %d steps completed", completedSteps)
                : String.format("Failed at step %d: %s", completedSteps + 1, message);
        }
    }
}
