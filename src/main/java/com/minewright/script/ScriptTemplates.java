package com.minewright.script;

import java.util.*;

/**
 * Prompt templates for LLM-based script generation and refinement.
 *
 * <p>This class provides structured prompt templates that guide LLMs in generating
 * high-quality automation scripts. The templates are designed to:</p>
 *
 * <ul>
 *   <li>Provide clear DSL grammar references</li>
 *   <li>Include relevant examples for few-shot learning</li>
 *   <li>Structure output for reliable parsing</li>
 *   <li>Include safety and validation guidelines</li>
 * </ul>
 *
 * <p><b>Template Categories:</b></p>
 * <ol>
 *   <li><b>Generation Templates:</b> For creating new scripts from commands</li>
 *   <li><b>Refinement Templates:</b> For improving existing scripts</li>
 *   <li><b>Explanation Templates:</b> For documenting script behavior</li>
 *   <li><b>Validation Templates:</b> For checking script correctness</li>
 * </ol>
 *
 * @see ScriptGenerator
 * @see ScriptRefiner
 * @see Script
 * @since 1.3.0
 */
public final class ScriptTemplates {

    private ScriptTemplates() {
        // Utility class - prevent instantiation
    }

    /**
     * Template for generating new scripts from natural language commands.
     */
    public static final String SCRIPT_GENERATION_PROMPT = """
        You are an expert Minecraft automation architect. Your task is to generate
        a DSL (Domain-Specific Language) script that implements the given command.

        ## Script DSL Grammar

        Scripts use a YAML-based DSL format with behavior tree structure.

        **Node Types:**
        - `sequence`: Execute children in order, all must succeed
        - `selector`: Try children in order until one succeeds
        - `parallel`: Execute all children simultaneously
        - `action`: Execute a single atomic action
        - `condition`: Check if a condition is true
        - `loop`: Repeat child nodes N times
        - `if`: Execute if branch based on condition

        **Script Structure:**
        ```yaml
        metadata:
          id: "script-id"
          name: "Script Name"
          description: "Description of what this script does"
          author: "author"

        parameters:
          - name: "param_name"
            type: "string|integer|boolean"
            default: "default_value"

        script:
          type: "sequence"
          steps:
            - type: "action"
              action: "action_name"
              params:
                key: "value"

            - type: "selector"
              steps:
                - type: "condition"
                  condition: "condition_expression"
                - type: "action"
                  action: "fallback_action"

        error_handling:
          on_failure:
            - type: "action"
              action: "handle_error"
        ```

        **Available Actions:**
        - `mine`: Mine a block (params: block, target, radius)
        - `place`: Place a block (params: block, position)
        - `pathfind`: Navigate to location (params: target, max_distance)
        - `craft`: Craft an item (params: item, count)
        - `gather`: Collect resources (params: resource, amount, radius)
        - `build`: Build a structure (params: structure, position, material)
        - `equip`: Equip an item (params: item, slot)
        - `deposit`: Store items (params: item, location)
        - `withdraw`: Withdraw items (params: item, location, amount)
        - `attack`: Attack a target (params: target)
        - `follow`: Follow an entity (params: target, distance)

        **Condition Expressions:**
        - `inventory_has('item', count)`: Check if inventory has items
        - `distance_to(target) < distance`: Check distance to target
        - `health_percent() < threshold`: Check health percentage
        - `time_of_day() == 'day|night|dusk|dawn'`: Check time of day
        - `blocks_in_range(block_type, radius) >= count`: Count nearby blocks

        %s

        ## Task

        Generate a script for the following command:
        "%s"

        ## Requirements

        1. Return ONLY the script in YAML DSL format (no markdown code blocks, no explanation)
        2. Include metadata section with id, name, and description
        3. Use appropriate node types for the task (sequence for steps, selector for alternatives)
        4. Include error handling for common failure cases
        5. Ensure the script is safe and efficient
        6. Use parameterized values where appropriate (e.g., {{parameter_name}})
        7. Add conditions to check prerequisites before actions
        8. Keep the script simple and focused on the specific task
        """;

    /**
     * Template for refining existing scripts based on feedback.
     */
    public static final String SCRIPT_REFINEMENT_PROMPT = """
        You are an expert Minecraft automation architect. Your task is to analyze
        a script execution result and generate an improved version.

        ## Script DSL Grammar

        %s

        ## Original Script

        ```yaml
        %s
        ```

        ## Execution Feedback

        **Status:** %s
        **Execution Time:** %d ms
        **Success:** %s

        %s

        ## Refinement Instructions

        Analyze the feedback and generate an improved version of the script. Focus on:

        %s

        ## Requirements

        1. Return ONLY the refined script in YAML DSL format (no markdown, no explanation)
        2. Keep the same script ID and metadata structure
        3. Address the specific issues identified in the feedback
        4. Add appropriate error handling and fallback behaviors
        5. Optimize for better performance and resource usage
        6. Ensure the script remains safe and valid
        7. Use comments in YAML to explain key improvements
        """;

    /**
     * Template for explaining script behavior to users.
     */
    public static final String SCRIPT_EXPLANATION_PROMPT = """
        You are an expert Minecraft automation architect. Your task is to explain
        how a script works in clear, simple language.

        ## Script

        ```yaml
        %s
        ```

        ## Context

        %s

        ## Instructions

        Provide a clear explanation of:
        1. What this script does (overview)
        2. The steps it takes to accomplish its goal
        3. Any prerequisites or requirements
        4. Potential failure points and how they're handled

        Write in a way that a non-technical player can understand. Use simple language
        and avoid jargon where possible. Format your response with clear sections.

        ## Format

        **Overview:** [Brief description of what the script does]

        **How It Works:** [Step-by-step explanation]

        **Requirements:** [What's needed for this script to work]

        **Potential Issues:** [What could go wrong and how it's handled]
        """;

    /**
     * Example scripts for few-shot learning.
     */
    public static final String EXAMPLE_SCRIPTS = """

        ## Example Scripts

        ### Example 1: Simple Mining with Error Handling

        ```yaml
        metadata:
          id: "simple_iron_mining"
          name: "Simple Iron Mining"
          description: "Mines iron ore nearby, handles common failures"

        script:
          type: "sequence"
          steps:
            - type: "selector"
              steps:
                # Try to find iron ore
                - type: "condition"
                  condition: "blocks_in_range('iron_ore', 64) > 0"

                # If not found, announce and return
                - type: "action"
                  action: "say"
                  params:
                    message: "No iron ore found nearby"

            - type: "action"
              action: "pathfind"
              params:
                target: "nearest"
                block_type: "iron_ore"
                max_distance: 64

            - type: "action"
              action: "mine"
              params:
                block: "iron_ore"
        ```

        ### Example 2: Building with Resource Gathering

        ```yaml
        metadata:
          id: "build_shelter"
          name: "Build Simple Shelter"
          description: "Builds a 5x3x5 shelter, gathering materials if needed"

        parameters:
          - name: "material"
            type: "string"
            default: "oak_planks"

        script:
          type: "sequence"
          steps:
            # Check if we have enough materials
            - type: "selector"
              steps:
                # If we have materials, continue
                - type: "condition"
                  condition: "inventory_has('{{material}}', 20)"

                # Otherwise, try to gather or craft
                - type: "selector"
                  steps:
                    - type: "action"
                      action: "gather"
                      params:
                        resource: "{{material}}"
                        amount: 20

                    - type: "action"
                      action: "craft"
                      params:
                        item: "{{material}}"
                        count: 20

                    - type: "action"
                      action: "say"
                      params:
                        message: "Cannot find or craft enough materials"

            # Build the shelter
            - type: "action"
              action: "build"
              params:
                structure: "shelter"
                width: 5
                depth: 5
                height: 3
                material: "{{material}}"

        error_handling:
          on_no_resources:
            - type: "action"
              action: "announce"
              params:
                message: "Not enough resources to build shelter"
        ```

        ### Example 3: Farming Loop

        ```yaml
        metadata:
          id: "auto_wheat_farm"
          name: "Auto Wheat Farm"
          description: "Harvests and replants wheat crops automatically"

        script:
          type: "sequence"
          steps:
            - type: "loop"
              iterations: 8
              steps:
                - type: "selector"
                  steps:
                    # Find mature wheat
                    - type: "condition"
                      condition: "blocks_in_range('wheat', 16, age=7) > 0"

                    # No mature wheat found
                    - type: "action"
                      action: "say"
                      params:
                        message: "No mature wheat found"

                # Pathfind to wheat
                - type: "action"
                  action: "pathfind"
                  params:
                    target: "nearest"
                    block_type: "wheat"
                    max_distance: 16

                # Harvest wheat
                - type: "action"
                  action: "mine"
                  params:
                    block: "wheat"

                # Replant wheat seeds
                - type: "action"
                  action: "place"
                  params:
                    block: "wheat_seeds"
        ```

        ### Example 4: Combat with Retreat

        ```yaml
        metadata:
          id: "defensive_combat"
          name: "Defensive Combat"
          description: "Attacks hostile enemies, retreats if low health"

        script:
          type: "sequence"
          steps:
            # Check health before engaging
            - type: "selector"
              steps:
                # Good health, proceed to attack
                - type: "condition"
                  condition: "health_percent() > 30"

                # Low health, retreat instead
                - type: "sequence"
                  steps:
                    - type: "action"
                      action: "say"
                      params:
                        message: "Low health, retreating!"

                    - type: "action"
                      action: "pathfind"
                      params:
                        target: "player"
                        max_distance: 50

            # Find and attack hostile mobs
            - type: "loop"
              iterations: 3
              steps:
                - type: "selector"
                  steps:
                    - type: "condition"
                      condition: "distance_to_nearest('hostile_mob') < 10"

                    - type: "action"
                      action: "attack"
                      params:
                        target: "nearest"
                        target_type: "hostile_mob"
        ```
        """;

    /**
     * Builds a generation prompt with context and examples.
     *
     * @param context The generation context (agent state, inventory, etc.)
     * @param command The user's natural language command
     * @return The formatted prompt
     */
    public static String buildGenerationPrompt(ScriptGenerationContext context, String command) {
        String contextSection = context != null && !context.isEmpty()
            ? "## Context\n\n" + context.toPromptSection() + "\n"
            : "";

        String examples = EXAMPLE_SCRIPTS;

        return String.format(
            SCRIPT_GENERATION_PROMPT,
            examples,
            command
        );
    }

    /**
     * Builds a refinement prompt with script and feedback.
     *
     * @param script The script to refine
     * @param feedback The execution feedback
     * @return The formatted prompt
     */
    public static String buildRefinementPrompt(Script script, ExecutionFeedback feedback) {
        String dslGrammar = getDSLGrammar();

        String scriptDSL = script.toDSL();

        String status = feedback.isSuccess() ? "Success" : "Failed";
        long executionTime = feedback.getExecutionTime().toMillis();
        boolean success = feedback.isSuccess();

        String feedbackDetails = buildFeedbackDetails(feedback);

        String instructions = buildRefinementInstructions(feedback);

        return String.format(
            SCRIPT_REFINEMENT_PROMPT,
            dslGrammar,
            scriptDSL,
            status,
            executionTime,
            success,
            feedbackDetails,
            instructions
        );
    }

    /**
     * Builds an explanation prompt for a script.
     *
     * @param script The script to explain
     * @param context Additional context for the explanation
     * @return The formatted prompt
     */
    public static String buildExplanationPrompt(Script script, String context) {
        String scriptDSL = script.toDSL();

        String contextSection = context != null && !context.isEmpty()
            ? "**Additional Context:**\n" + context
            : "No additional context provided.";

        return String.format(
            SCRIPT_EXPLANATION_PROMPT,
            scriptDSL,
            contextSection
        );
    }

    /**
     * Returns the DSL grammar reference.
     */
    private static String getDSLGrammar() {
        return """
            Scripts use a YAML-based DSL format with behavior tree structure.

            **Node Types:**
            - `sequence`: Execute children in order, all must succeed
            - `selector`: Try children in order until one succeeds
            - `parallel`: Execute all children simultaneously
            - `action`: Execute a single atomic action
            - `condition`: Check if a condition is true
            - `loop`: Repeat child nodes N times
            - `if`: Execute if branch based on condition

            **Available Actions:**
            - `mine`, `place`, `build`, `craft`, `gather`, `pathfind`,
              `equip`, `deposit`, `withdraw`, `attack`, `follow`

            **Condition Expressions:**
            - `inventory_has('item', count)`, `distance_to(target) < distance`,
              `health_percent() < threshold`, `blocks_in_range(block_type, radius) >= count`
            """;
    }

    /**
     * Builds detailed feedback information for the prompt.
     */
    private static String buildFeedbackDetails(ExecutionFeedback feedback) {
        StringBuilder sb = new StringBuilder();

        if (feedback.getFailureReason() != null) {
            sb.append("**Failure Reason:** ").append(feedback.getFailureReason()).append("\n");
        }

        if (feedback.getErrorMessages() != null && !feedback.getErrorMessages().isEmpty()) {
            sb.append("**Errors:**\n");
            for (String error : feedback.getErrorMessages()) {
                sb.append("  - ").append(error).append("\n");
            }
        }

        if (feedback.getResourceUsage() != null) {
            sb.append("**Resource Usage:**\n");
            sb.append("  - Efficiency Score: ").append(feedback.getResourceUsage().getScore()).append("\n");
        }

        if (feedback.getUserSatisfaction() != null) {
            sb.append("**User Satisfaction:** ").append(feedback.getUserSatisfaction()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Builds refinement instructions based on feedback.
     */
    private static String buildRefinementInstructions(ExecutionFeedback feedback) {
        StringBuilder instructions = new StringBuilder();

        if (!feedback.isSuccess()) {
            instructions.append("1. **Fix the failure**: Address the error that caused the script to fail\n");
        }

        if (feedback.getExecutionTime().toMillis() > 30000) {
            instructions.append("2. **Improve performance**: The script took too long, optimize it\n");
        }

        if (feedback.getErrorMessages() != null && !feedback.getErrorMessages().isEmpty()) {
            instructions.append("3. **Add error handling**: Add fallback behaviors for the errors encountered\n");
        }

        if (feedback.getResourceUsage() != null && feedback.getResourceUsage().getScore() < 0.6) {
            instructions.append("4. **Optimize resources**: Improve resource usage efficiency\n");
        }

        return instructions.toString();
    }

    /**
     * Template for validation prompts.
     */
    public static final String SCRIPT_VALIDATION_PROMPT = """
        You are a script validation expert. Review the following script for potential issues.

        ## Script

        ```yaml
        %s
        ```

        ## Validation Checklist

        Check for:
        1. **Safety Issues**: Infinite loops, dangerous operations, missing validation
        2. **Performance Issues**: Redundant operations, inefficient patterns
        3. **Error Handling**: Missing error handlers, no fallback behaviors
        4. **Resource Issues**: Potential resource exhaustion, missing prerequisites
        5. **Logic Errors**: Unreachable code, incorrect conditions

        ## Output Format

        Return a JSON object with:
        {
          "valid": true/false,
          "errors": ["list of critical errors"],
          "warnings": ["list of warnings"],
          "suggestions": ["list of improvement suggestions"]
        }
        """;

    /**
     * Builds a validation prompt for a script.
     *
     * @param script The script to validate
     * @return The formatted prompt
     */
    public static String buildValidationPrompt(Script script) {
        return String.format(SCRIPT_VALIDATION_PROMPT, script.toDSL());
    }
}
