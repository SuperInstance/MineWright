package com.minewright.skill;

import com.minewright.action.Task;

import java.util.List;
import java.util.Map;

/**
 * Represents a reusable skill that agents can learn and execute.
 *
 * <p><b>Voyager Pattern:</b></p>
 * <p>Based on the Voyager architecture where agents learn executable skills
 * from successful task sequences. Skills encapsulate proven patterns that can
 * be reused without LLM planning, reducing both latency and cost.</p>
 *
 * <p><b>Skill Lifecycle:</b></p>
 * <ol>
 *   <li><b>Discovery:</b> SkillGenerator identifies patterns in successful task sequences</li>
 *   <li><b>Validation:</b> Skill is tested and verified before adding to library</li>
 *   <li><b>Execution:</b> Skill generates task-specific code from template</li>
 *   <li><b>Refinement:</b> Success rates track effectiveness for optimization</li>
 * </ol>
 *
 * <p><b>Built-in Skills:</b></p>
 * <ul>
 *   <li>digStaircase - Creates staircases for mining</li>
 *   <li>stripMine - Organized strip mining pattern</li>
 *   <li>buildShelter - Basic shelter construction</li>
 *   <li>farmWheat - Automated wheat farming</li>
 * </ul>
 *
 * @see ExecutableSkill
 * @see SkillLibrary
 * @see SkillGenerator
 * @since 1.0.0
 */
public interface Skill {
    /**
     * Returns the unique name of this skill.
     * Used as the key in SkillLibrary.
     *
     * @return Unique skill identifier (e.g., "digStaircase")
     */
    String getName();

    /**
     * Returns a human-readable description of what this skill does.
     * Used for semantic search and user-facing explanations.
     *
     * @return Skill description
     */
    String getDescription();

    /**
     * Returns the list of action types required by this skill.
     * Used to validate that the agent can execute this skill.
     *
     * @return List of required action types (e.g., ["mine", "place", "pathfind"])
     */
    List<String> getRequiredActions();

    /**
     * Generates executable code for this skill with the given context.
     * The code is JavaScript that will be executed via GraalVM.
     *
     * <p><b>Context Parameters:</b></p>
     * <ul>
     *   <li>foreman - The ForemanEntity executing the skill</li>
     *   <li>targetBlock - Block type to interact with</li>
     *   <li>position - Target position {x, y, z}</li>
     *   <li>quantity - Number of items/blocks</li>
     * </ul>
     *
     * @param context Map of context variables for code generation
     * @return Executable JavaScript code
     */
    String generateCode(Map<String, Object> context);

    /**
     * Checks if this skill is applicable to the given task.
     * Used by SkillLibrary to find relevant skills for a command.
     *
     * @param task The task to check applicability for
     * @return true if this skill can handle the task
     */
    boolean isApplicable(Task task);

    /**
     * Returns the historical success rate of this skill.
     * Range: 0.0 to 1.0, where 1.0 is 100% success.
     *
     * @return Success rate (0.0 to 1.0)
     */
    double getSuccessRate();

    /**
     * Records the outcome of executing this skill.
     * Used to track success rates and improve skill selection.
     *
     * @param success true if execution was successful
     */
    void recordSuccess(boolean success);

    /**
     * Returns the number of times this skill has been executed.
     * Used for statistics and skill prioritization.
     *
     * @return Execution count
     */
    int getExecutionCount();

    /**
     * Returns the skill category for organization.
     * Categories: "mining", "building", "farming", "combat", "utility"
     *
     * @return Skill category
     */
    default String getCategory() {
        return "utility";
    }

    /**
     * Returns the estimated execution time in ticks.
     * Used for planning and timeout calculations.
     *
     * @return Estimated ticks (20 ticks = 1 second)
     */
    default int getEstimatedTicks() {
        return 100; // Default: 5 seconds
    }

    /**
     * Checks if this skill requires specific items in inventory.
     *
     * @return List of required item names, or empty list if none
     */
    default List<String> getRequiredItems() {
        return List.of();
    }
}
