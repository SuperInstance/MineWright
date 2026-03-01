package com.minewright.skill;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Automatically generates skills from discovered execution patterns.
 *
 * <p><b>Purpose:</b></p>
 * <p>SkillAutoGenerator converts patterns discovered by PatternExtractor
 * into executable Skill objects that can be added to the SkillLibrary.
 * This is the core of the Voyager pattern's automatic skill learning.</p>
 *
 * <p><b>Generation Process:</b></p>
 * <ol>
 *   <li><b>Pattern Analysis:</b> Extract action sequence and parameters</li>
 *   <li><b>Code Generation:</b> Create JavaScript template with placeholders</li>
 *   <li><b>Metadata:</b> Generate name, description, category</li>
 *   <li><b>Validation:</b> Check for applicability patterns</li>
 *   <li><b>Registration:</b> Add to SkillLibrary for reuse</li>
 * </ol>
 *
 * <p><b>Example Generated Skill:</b></p>
 * <pre>
 * Name: digStaircase
 * Description: Dig a staircase downwards for safe mining
 * Actions: [pathfind, mine, place]
 * Code: var depth = {{depth}};
 *       for (var i = 0; i < depth; i++) {
 *         steve.mineBlock(x, y - i, z);
 *       }
 * </pre>
 *
 * <p><b>Template System:</b></p>
 * <p>Generated code uses {{variable}} placeholders for substitution:
 * <ul>
 *   <li>{{depth}} - Numeric parameter</li>
 *   <li>{{direction:quote}} - String parameter with quotes</li>
 *   <li>{{blockType}} - Block identifier</li>
 * </ul></p>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe. Generation is a read-only operation
 * on the provided pattern.</p>
 *
 * @see PatternExtractor
 * @see ExecutableSkill
 * @see SkillLibrary
 * @since 1.0.0
 */
public class SkillAutoGenerator {
    private static final Logger LOGGER = TestLogger.getLogger(SkillAutoGenerator.class);

    /**
     * Singleton instance for global access.
     */
    private static volatile SkillAutoGenerator instance;

    private SkillAutoGenerator() {
        LOGGER.info("SkillAutoGenerator initialized");
    }

    /**
     * Gets the singleton SkillAutoGenerator instance.
     *
     * @return Global instance
     */
    public static SkillAutoGenerator getInstance() {
        if (instance == null) {
            synchronized (SkillAutoGenerator.class) {
                if (instance == null) {
                    instance = new SkillAutoGenerator();
                }
            }
        }
        return instance;
    }

    /**
     * Generates a skill from a discovered pattern.
     *
     * @param pattern The pattern to convert to a skill
     * @return Generated ExecutableSkill, or null if generation fails
     */
    public ExecutableSkill generateSkill(PatternExtractor.Pattern pattern) {
        if (pattern == null) {
            LOGGER.warn("Cannot generate skill from null pattern");
            return null;
        }

        LOGGER.info("Generating skill from pattern: {} (frequency: {}, success: {:.2f}%)",
            pattern.getName(), pattern.getFrequency(), pattern.getSuccessRate() * 100);

        try {
            String skillId = generateSkillId(pattern);
            String description = generateDescription(pattern);
            String category = determineCategory(pattern);
            String codeTemplate = generateCodeTemplate(pattern);
            List<String> requiredActions = pattern.getActionSequence();

            // Build the skill
            ExecutableSkill.Builder builder = ExecutableSkill.builder(skillId)
                .description(description)
                .category(category)
                .codeTemplate(codeTemplate)
                .requiredActions(requiredActions.toArray(new String[0]));

            // Add applicability pattern
            String applicabilityPattern = generateApplicabilityPattern(pattern);
            if (applicabilityPattern != null) {
                builder.applicabilityPattern(applicabilityPattern);
            }

            // Estimate execution time
            int estimatedTicks = (int) (pattern.getAverageExecutionTime() / 50); // 20 TPS = 50ms per tick
            builder.estimatedTicks(Math.max(estimatedTicks, 100)); // Minimum 5 seconds

            ExecutableSkill skill = builder.build();

            LOGGER.info("Successfully generated skill: {}", skillId);
            return skill;

        } catch (Exception e) {
            LOGGER.error("Failed to generate skill from pattern: {}", pattern.getName(), e);
            return null;
        }
    }

    /**
     * Generates multiple skills from a list of patterns.
     *
     * @param patterns List of patterns to convert
     * @return List of generated skills (nulls excluded)
     */
    public List<ExecutableSkill> generateSkills(List<PatternExtractor.Pattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            LOGGER.debug("No patterns provided for skill generation");
            return List.of();
        }

        LOGGER.info("Generating skills from {} patterns", patterns.size());

        List<ExecutableSkill> skills = patterns.stream()
            .map(this::generateSkill)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        LOGGER.info("Generated {} skills from {} patterns", skills.size(), patterns.size());
        return skills;
    }

    /**
     * Generates and registers skills directly from execution sequences.
     * This is a convenience method that combines extraction and generation.
     *
     * @param sequences List of execution sequences
     * @return Number of skills generated and registered
     */
    public int generateAndRegisterSkills(List<ExecutionSequence> sequences) {
        LOGGER.info("Auto-generating skills from {} execution sequences", sequences.size());

        // Extract patterns
        PatternExtractor extractor = new PatternExtractor();
        List<PatternExtractor.Pattern> patterns = extractor.extractPatterns(sequences);

        LOGGER.info("Found {} patterns meeting thresholds", patterns.size());

        // Generate skills
        List<ExecutableSkill> skills = generateSkills(patterns);

        // Register skills
        SkillLibrary library = SkillLibrary.getInstance();
        int registered = 0;

        for (ExecutableSkill skill : skills) {
            if (library.addSkill(skill)) {
                registered++;
            } else {
                LOGGER.warn("Failed to register skill: {}", skill.getName());
            }
        }

        LOGGER.info("Registered {} new skills from {} sequences", registered, sequences.size());
        return registered;
    }

    /**
     * Generates a unique skill ID from the pattern.
     *
     * @param pattern The pattern
     * @return Skill ID (e.g., "digStaircase", "buildShelter")
     */
    private String generateSkillId(PatternExtractor.Pattern pattern) {
        String name = pattern.getName().toLowerCase().replaceAll("\\s+", "");

        // Remove any special characters
        name = name.replaceAll("[^a-zA-Z0-9]", "");

        // Ensure first letter is lowercase
        if (!name.isEmpty()) {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }

        return name;
    }

    /**
     * Generates a human-readable description for the skill.
     *
     * @param pattern The pattern
     * @return Description string
     */
    private String generateDescription(PatternExtractor.Pattern pattern) {
        StringBuilder desc = new StringBuilder();

        // Add main action
        desc.append("Automated ").append(pattern.getName());

        // Add action sequence summary
        List<String> actions = pattern.getActionSequence();
        if (!actions.isEmpty()) {
            desc.append(" (");
            if (actions.size() <= 3) {
                desc.append(String.join(", ", actions));
            } else {
                desc.append(actions.get(0))
                    .append(", ")
                    .append(actions.get(1))
                    .append(", ... ")
                    .append(actions.get(actions.size() - 1));
            }
            desc.append(")");
        }

        // Add success rate info
        desc.append(String.format(" - %.1f%% success rate", pattern.getSuccessRate() * 100));

        return desc.toString();
    }

    /**
     * Determines the skill category based on action types.
     *
     * @param pattern The pattern
     * @return Category string
     */
    private String determineCategory(PatternExtractor.Pattern pattern) {
        List<String> actions = pattern.getActionSequence();

        // Check for category-specific actions
        for (String action : actions) {
            String lower = action.toLowerCase();
            if (lower.contains("mine") || lower.contains("dig") || lower.contains("excavate")) {
                return "mining";
            }
            if (lower.contains("build") || lower.contains("place") || lower.contains("construct")) {
                return "building";
            }
            if (lower.contains("farm") || lower.contains("plant") || lower.contains("harvest")) {
                return "farming";
            }
            if (lower.contains("attack") || lower.contains("combat") || lower.contains("defend")) {
                return "combat";
            }
        }

        return "utility";
    }

    /**
     * Generates JavaScript code template from the pattern.
     *
     * @param pattern The pattern
     * @return JavaScript template string
     */
    private String generateCodeTemplate(PatternExtractor.Pattern pattern) {
        StringBuilder code = new StringBuilder();

        code.append("// Auto-generated skill from pattern\n");
        code.append("// Pattern: ").append(pattern.getSignature()).append("\n");
        code.append("// Success rate: ").append(String.format("%.1f%%", pattern.getSuccessRate() * 100)).append("\n\n");

        // Add common context extraction
        if (pattern.getParameters().contains("depth") || pattern.getParameters().contains("count")) {
            code.append("// Extract parameters\n");
            for (String param : pattern.getParameters()) {
                code.append("var ").append(param).append(" = {{").append(param).append("}};\n");
            }
            code.append("\n");
        }

        // Generate action sequence
        List<String> actions = pattern.getActionSequence();
        if (!actions.isEmpty()) {
            code.append("// Execute action sequence\n");

            // Simple linear execution for now
            // More sophisticated generation would include loops and conditionals
            for (int i = 0; i < actions.size(); i++) {
                String action = actions.get(i);
                code.append(generateActionCall(action, i));
            }
        }

        code.append("\n// Auto-generated skill completed successfully");
        return code.toString();
    }

    /**
     * Generates a JavaScript call for a single action.
     *
     * @param actionType The type of action
     * @param index      Action index in sequence
     * @return JavaScript code line
     */
    private String generateActionCall(String actionType, int index) {
        String lower = actionType.toLowerCase();

        // Generate appropriate call based on action type
        if (lower.contains("mine")) {
            return String.format("steve.mineBlock(startX + i, startY - i, startZ); // Action %d\n", index);
        } else if (lower.contains("place")) {
            return String.format("steve.placeBlock('{{block:quote}}', startX + i, startY, startZ); // Action %d\n", index);
        } else if (lower.contains("pathfind")) {
            return String.format("steve.pathfindTo(startX, startY, startZ); // Action %d\n", index);
        } else if (lower.contains("craft")) {
            return String.format("steve.craftItem('{{item:quote}}', {{quantity}}); // Action %d\n", index);
        } else {
            return String.format("// TODO: Implement %s (Action %d)\n", actionType, index);
        }
    }

    /**
     * Generates a regex pattern for matching applicable tasks.
     *
     * @param pattern The pattern
     * @return Regex pattern string, or null if unable to generate
     */
    private String generateApplicabilityPattern(PatternExtractor.Pattern pattern) {
        String name = pattern.getName().toLowerCase();

        // Build pattern from skill name and actions
        StringBuilder patternBuilder = new StringBuilder();

        // Add name variations
        patternBuilder.append("(").append(name).append(")");

        // Add action-based patterns
        for (String action : pattern.getActionSequence()) {
            patternBuilder.append("|").append(action).append(".*");
        }

        return patternBuilder.toString();
    }

    /**
     * Validates a generated skill before registration.
     *
     * @param skill The skill to validate
     * @return true if skill is valid
     */
    public boolean validateSkill(ExecutableSkill skill) {
        if (skill == null) {
            return false;
        }

        // Check required fields
        if (skill.getName() == null || skill.getName().isEmpty()) {
            LOGGER.warn("Skill validation failed: missing name");
            return false;
        }

        if (skill.getDescription() == null || skill.getDescription().isEmpty()) {
            LOGGER.warn("Skill validation failed: missing description");
            return false;
        }

        if (skill.getRequiredActions().isEmpty()) {
            LOGGER.warn("Skill validation failed: no required actions");
            return false;
        }

        return true;
    }
}
