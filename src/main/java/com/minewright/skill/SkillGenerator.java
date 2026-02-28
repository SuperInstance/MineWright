package com.minewright.skill;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import com.minewright.action.Task;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Generates new skills from successful task sequences.
 *
 * <p><b>Voyager Pattern - Learning Component:</b></p>
 * <p>The SkillGenerator analyzes completed task sequences, identifies
 * successful patterns, and automatically creates new skills. This enables
 * the system to learn from experience and improve over time.</p>
 *
 * <p><b>Learning Pipeline:</b></p>
 * <ol>
 *   <li><b>Pattern Detection:</b> Analyze task sequence for reusable patterns</li>
 *   <li><b>Validation:</b> Verify pattern is consistent and successful</li>
 *   <li><b>Code Generation:</b> Create JavaScript template from pattern</li>
 *   <li><b>Skill Creation:</b> Build ExecutableSkill with metadata</li>
 *   <li><b>Registration:</b> Add to SkillLibrary if unique</li>
 * </ol>
 *
 * <p><b>Pattern Requirements:</b></p>
 * <ul>
 *   <li>Minimum 3 tasks in sequence</li>
 *   <li>At least 70% success rate</li>
 *   <li>Repeating or predictable structure</li>
 *   <li>No existing skill with same signature</li>
 * </ul>
 *
 * <p><b>Generated Skills:</b></p>
 * <p>Each generated skill includes:</p>
 * <ul>
 *   <li>Auto-generated name (e.g., "auto_skill_mine_loop_001")</li>
 *   <li>Description of the pattern</li>
 *   <li>JavaScript code template</li>
 *   <li>Required actions and items</li>
 *   <li>Applicability regex pattern</li>
 * </ul>
 *
 * @see Skill
 * @see SkillLibrary
 * @see TaskPattern
 * @since 1.0.0
 */
public class SkillGenerator {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(SkillGenerator.class);

    private final SkillLibrary skillLibrary;
    private final Map<String, Integer> patternFrequency;
    private final Set<String> recentSignatures;
    private int generatedSkillCount = 0;

    /**
     * Minimum success rate for a pattern to become a skill.
     */
    private static final double MIN_SUCCESS_RATE = 0.7;

    /**
     * Minimum frequency for a pattern to become a skill.
     */
    private static final int MIN_FREQUENCY = 2;

    /**
     * Maximum number of auto-generated skills to prevent bloat.
     */
    private static final int MAX_AUTO_SKILLS = 50;

    /**
     * Creates a new SkillGenerator.
     *
     * @param skillLibrary The skill library to register generated skills with
     */
    public SkillGenerator(SkillLibrary skillLibrary) {
        this.skillLibrary = skillLibrary;
        this.patternFrequency = new ConcurrentHashMap<>();
        this.recentSignatures = ConcurrentHashMap.newKeySet();

        LOGGER.info("SkillGenerator initialized");
    }

    /**
     * Analyzes a completed task sequence and generates skills if patterns are found.
     *
     * <p><b>Analysis Steps:</b></p>
     * <ol>
     *   <li>Detect patterns in task sequence</li>
     *   <li>Validate patterns meet requirements</li>
     *   <li>Generate skill code from pattern</li>
     *   <li>Register skill if unique</li>
     * </ol>
     *
     * @param tasks Sequence of tasks that were executed
     * @param wasSuccessful Whether the overall sequence was successful
     * @return List of generated skills (may be empty)
     */
    public List<Skill> analyzeTaskSequence(List<Task> tasks, boolean wasSuccessful) {
        if (tasks.isEmpty()) {
            return List.of();
        }

        LOGGER.debug("Analyzing task sequence of {} tasks (success: {})",
            tasks.size(), wasSuccessful);

        List<Skill> generatedSkills = new ArrayList<>();

        // Only learn from successful sequences
        if (!wasSuccessful) {
            LOGGER.debug("Skipping skill generation from failed sequence");
            return generatedSkills;
        }

        // Detect patterns
        List<TaskPattern> patterns = TaskPattern.detectPatterns(tasks);
        LOGGER.debug("Detected {} patterns in task sequence", patterns.size());

        // Process each pattern
        for (TaskPattern pattern : patterns) {
            // Update frequency tracking
            String signature = pattern.getSignature();
            patternFrequency.merge(signature, 1, Integer::sum);

            // Check if pattern meets requirements
            if (shouldGenerateSkill(pattern)) {
                Skill skill = generateSkillFromPattern(pattern);
                if (skill != null) {
                    generatedSkills.add(skill);
                }
            }
        }

        if (!generatedSkills.isEmpty()) {
            LOGGER.info("Generated {} new skills from task sequence",
                generatedSkills.size());
        }

        return generatedSkills;
    }

    /**
     * Generates a skill from a detected pattern.
     *
     * @param pattern The pattern to convert to a skill
     * @return Generated skill, or null if generation failed
     */
    public Skill generateSkillFromPattern(TaskPattern pattern) {
        try {
            // Generate skill name
            String skillName = generateSkillName(pattern);

            // Generate description
            String description = generateDescription(pattern);

            // Generate code template
            String codeTemplate = generateCodeTemplate(pattern);

            // Determine required actions
            List<String> requiredActions = extractRequiredActions(pattern);

            // Generate applicability pattern
            String applicabilityPattern = generateApplicabilityPattern(pattern);

            // Determine category
            String category = determineCategory(pattern);

            // Build the skill
            ExecutableSkill skill = ExecutableSkill.builder(skillName)
                .description(description)
                .category(category)
                .codeTemplate(codeTemplate)
                .requiredActions(requiredActions.toArray(new String[0]))
                .estimatedTicks(calculateEstimatedTicks(pattern))
                .applicabilityPattern(applicabilityPattern)
                .build();

            // Validate and register
            if (validateSkill(skill)) {
                if (skillLibrary.addSkill(skill)) {
                    generatedSkillCount++;
                    recentSignatures.add(pattern.getSignature());
                    LOGGER.info("Successfully generated and registered skill: {}",
                        skillName);
                    return skill;
                } else {
                    LOGGER.debug("Skill '{}' already exists in library", skillName);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to generate skill from pattern: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Validates that a skill meets all requirements.
     *
     * @param skill The skill to validate
     * @return true if valid
     */
    public boolean validateSkill(Skill skill) {
        // Check if we've hit the auto-skill limit
        if (generatedSkillCount >= MAX_AUTO_SKILLS) {
            LOGGER.debug("Reached maximum auto-generated skills limit ({})",
                MAX_AUTO_SKILLS);
            return false;
        }

        // Check for duplicate
        if (skillLibrary.hasSkill(skill.getName())) {
            LOGGER.debug("Skill with name '{}' already exists", skill.getName());
            return false;
        }

        // Validate required actions are known
        for (String action : skill.getRequiredActions()) {
            if (!isValidAction(action)) {
                LOGGER.warn("Skill '{}' requires unknown action '{}'",
                    skill.getName(), action);
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a pattern should be converted to a skill.
     */
    private boolean shouldGenerateSkill(TaskPattern pattern) {
        // Check minimum frequency
        int frequency = patternFrequency.getOrDefault(pattern.getSignature(), 0);
        if (frequency < MIN_FREQUENCY) {
            return false;
        }

        // Check success rate
        if (pattern.getSuccessRate() < MIN_SUCCESS_RATE) {
            return false;
        }

        // Check if recently generated (prevent duplicates in short time)
        if (recentSignatures.contains(pattern.getSignature())) {
            return false;
        }

        return true;
    }

    /**
     * Generates a unique name for a skill.
     */
    private String generateSkillName(TaskPattern pattern) {
        String baseName = pattern.getName().replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        String uniqueId = String.format("%03d", generatedSkillCount);
        return "auto_" + baseName + "_" + uniqueId;
    }

    /**
     * Generates a human-readable description.
     */
    private String generateDescription(TaskPattern pattern) {
        StringBuilder desc = new StringBuilder();

        desc.append("Auto-generated skill from ");
        desc.append(pattern.getType().toString().toLowerCase());
        desc.append(" pattern. ");

        if (!pattern.getSteps().isEmpty()) {
            List<String> actions = pattern.getSteps().stream()
                .map(TaskPattern.TaskStep::getAction)
                .distinct()
                .collect(Collectors.toList());
            desc.append("Actions: ").append(String.join(", ", actions));
        }

        if (pattern.getFrequency() > 1) {
            desc.append(" (seen ").append(pattern.getFrequency()).append(" times)");
        }

        return desc.toString();
    }

    /**
     * Generates JavaScript code template from a pattern.
     */
    private String generateCodeTemplate(TaskPattern pattern) {
        StringBuilder code = new StringBuilder();

        switch (pattern.getType()) {
            case LOOP:
                code.append(generateLoopCode(pattern));
                break;

            case SEQUENCE:
                code.append(generateSequenceCode(pattern));
                break;

            case PARAMETERIZED:
                code.append(generateParameterizedCode(pattern));
                break;

            default:
                code.append(generateGenericCode(pattern));
                break;
        }

        return code.toString();
    }

    /**
     * Generates code for loop patterns.
     */
    private String generateLoopCode(TaskPattern pattern) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated loop pattern\n");

        // Extract loop variables
        int iterations = pattern.getFrequency();
        code.append(String.format("var iterations = %d;\n\n", iterations));

        code.append("for (var i = 0; i < iterations; i++) {\n");

        // Add loop body from first step
        if (!pattern.getSteps().isEmpty()) {
            TaskPattern.TaskStep firstStep = pattern.getSteps().get(0);
            code.append(generateStepCode(firstStep, "i"));
        }

        code.append("}\n");

        return code.toString();
    }

    /**
     * Generates code for sequence patterns.
     */
    private String generateSequenceCode(TaskPattern pattern) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated sequence pattern\n\n");

        for (TaskPattern.TaskStep step : pattern.getSteps()) {
            code.append(generateStepCode(step, null));
        }

        return code.toString();
    }

    /**
     * Generates code for parameterized patterns.
     */
    private String generateParameterizedCode(TaskPattern pattern) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated parameterized pattern\n\n");

        if (!pattern.getSteps().isEmpty()) {
            TaskPattern.TaskStep step = pattern.getSteps().get(0);
            code.append(generateStepCode(step, "variable"));
        }

        return code.toString();
    }

    /**
     * Generates generic code for unknown pattern types.
     */
    private String generateGenericCode(TaskPattern pattern) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated skill\n");
        code.append("// Pattern type: ").append(pattern.getType()).append("\n\n");

        for (TaskPattern.TaskStep step : pattern.getSteps()) {
            code.append("// Step: ").append(step.getAction()).append("\n");
            code.append(generateStepCode(step, null));
        }

        return code.toString();
    }

    /**
     * Generates code for a single task step.
     */
    private String generateStepCode(TaskPattern.TaskStep step, String indexVar) {
        StringBuilder code = new StringBuilder();

        String action = step.getAction();
        code.append("steve.").append(action).append("(");

        // Add parameters
        List<String> params = new ArrayList<>();
        for (Map.Entry<String, TaskPattern.ParameterPattern> entry :
                step.getParameters().entrySet()) {

            TaskPattern.ParameterPattern paramPattern = entry.getValue();
            String valueStr = generateParameterCode(paramPattern, indexVar);
            params.add(valueStr);
        }

        code.append(String.join(", ", params));
        code.append(");\n");

        return code.toString();
    }

    /**
     * Generates code for a parameter.
     */
    private String generateParameterCode(TaskPattern.ParameterPattern param, String indexVar) {
        if (param.getType() == TaskPattern.ParameterPattern.ParameterType.CONSTANT) {
            if (param.getBaseValue() instanceof String) {
                return "\"" + param.getBaseValue() + "\"";
            }
            return String.valueOf(param.getBaseValue());
        }

        if (param.getType() == TaskPattern.ParameterPattern.ParameterType.INCREMENTING) {
            if (indexVar != null) {
                return "start" + param.getName().substring(0, 1).toUpperCase() +
                       param.getName().substring(1) + " + (" + indexVar + " * " +
                       param.getIncrement() + ")";
            }
            return "{{" + param.getName() + "}}";
        }

        return "{{" + param.getName() + "}}";
    }

    /**
     * Extracts required actions from a pattern.
     */
    private List<String> extractRequiredActions(TaskPattern pattern) {
        return pattern.getSteps().stream()
            .map(TaskPattern.TaskStep::getAction)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Generates an applicability regex pattern.
     */
    private String generateApplicabilityPattern(TaskPattern pattern) {
        List<String> actions = extractRequiredActions(pattern);

        if (actions.size() == 1) {
            return actions.get(0) + ".*";
        }

        return String.join(".*", actions);
    }

    /**
     * Determines the category for a skill.
     */
    private String determineCategory(TaskPattern pattern) {
        Set<String> actions = pattern.getSteps().stream()
            .map(TaskPattern.TaskStep::getAction)
            .collect(Collectors.toSet());

        // Mining actions
        if (actions.contains("mine") || actions.contains("gather")) {
            return "mining";
        }

        // Building actions
        if (actions.contains("place") || actions.contains("build")) {
            return "building";
        }

        // Farming actions
        if (actions.contains("plant") || actions.contains("farm") || actions.contains("till")) {
            return "farming";
        }

        // Combat actions
        if (actions.contains("attack") || actions.contains("defend")) {
            return "combat";
        }

        return "utility";
    }

    /**
     * Calculates estimated execution ticks for a pattern.
     */
    private int calculateEstimatedTicks(TaskPattern pattern) {
        // Base estimate: 100 ticks per step
        int baseTicks = pattern.getSteps().size() * 100;

        // Adjust for frequency (loops take longer)
        if (pattern.getType() == TaskPattern.PatternType.LOOP) {
            baseTicks *= pattern.getFrequency();
        }

        return Math.min(baseTicks, 3600); // Cap at 3 minutes
    }

    /**
     * Checks if an action type is valid.
     */
    private boolean isValidAction(String action) {
        return Set.of("pathfind", "mine", "place", "craft", "attack",
                      "follow", "gather", "build", "organize", "collect")
                   .contains(action);
    }

    /**
     * Gets statistics about generated skills.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("generatedSkills", generatedSkillCount);
        stats.put("trackedPatterns", patternFrequency.size());
        stats.put("recentSignatures", recentSignatures.size());
        return stats;
    }

    /**
     * Clears the recent signatures cache to allow regeneration.
     */
    public void clearRecentSignatures() {
        recentSignatures.clear();
    }

    /**
     * Resets the generator (for testing).
     */
    public void reset() {
        generatedSkillCount = 0;
        patternFrequency.clear();
        recentSignatures.clear();
    }
}
