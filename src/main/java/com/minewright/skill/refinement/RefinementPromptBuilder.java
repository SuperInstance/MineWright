package com.minewright.skill.refinement;

import com.minewright.security.InputSanitizer;
import com.minewright.skill.ExecutableSkill;
import com.minewright.skill.Skill;
import com.minewright.skill.ValidationContext;
import com.minewright.skill.ValidationResult;

import java.util.Map;

public class RefinementPromptBuilder {
    private static final int MAX_PROMPT_LENGTH = 8000;
    private static final int MAX_CODE_LENGTH = 4000;

    private RefinementPromptBuilder() {}

    public static String buildFeedbackPrompt(Skill skill, ValidationResult validationResult, ValidationContext context) {
        if (skill == null || validationResult == null || context == null) {
            throw new IllegalArgumentException("Skill, ValidationResult, and ValidationContext cannot be null");
        }

        String skillName = InputSanitizer.forLLM(skill.getName(), 100);
        String skillDescription = InputSanitizer.forLLM(skill.getDescription(), 500);
        String skillCode = sanitizeCode(extractCode(skill));
        String failureReason = InputSanitizer.forLLM(
            validationResult.getReason() != null ? validationResult.getReason() : "Unknown", 500);
        String currentState = InputSanitizer.forLLM(
            context.getCurrentState() != null ? context.getCurrentState() : "{}", 1000);

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert AI skill critic analyzing Minecraft agent skills.\n\n");
        prompt.append("## Skill Information\n");
        prompt.append("Name: ").append(skillName).append("\n");
        prompt.append("Description: ").append(skillDescription).append("\n");
        prompt.append("Category: ").append(InputSanitizer.forLLM(skill.getCategory(), 50)).append("\n");

        if (!skillCode.isEmpty()) {
            prompt.append("\n## Current Code\n```\n").append(skillCode).append("\n```\n");
        }

        prompt.append("\n## Execution Context\n");
        prompt.append("Current World State: ").append(currentState).append("\n");
        if (context.getExecutionTimeMs() > 0) {
            prompt.append("Execution Time: ").append(context.getExecutionTimeMs()).append("ms\n");
        }

        prompt.append("\n## Validation Failure\n");
        prompt.append("Reason: ").append(failureReason).append("\n");
        prompt.append("\n## Your Task\n");
        prompt.append("Analyze why this skill failed and provide structured feedback for refinement.\n\n");

        prompt.append("Return your response as a JSON object with this exact structure:\n");
        prompt.append("{\n");
        prompt.append("  \"analysis\": \"Brief explanation of what went wrong\",\n");
        prompt.append("  \"issues\": [{\"description\": \"What is wrong\", \"severity\": \"high|medium|low\", \"location\": \"specific part\"}],\n");
        prompt.append("  \"suggestions\": [\"Specific suggestion for improvement\"],\n");
        prompt.append("  \"validationRequirements\": [\"What must be true for success\"]\n");
        prompt.append("}\n\n");

        String result = prompt.toString();
        return result.length() > MAX_PROMPT_LENGTH ? result.substring(0, MAX_PROMPT_LENGTH) : result;
    }

    private static String extractCode(Skill skill) {
        if (skill instanceof ExecutableSkill) {
            return ((ExecutableSkill) skill).generateCode(Map.of());
        }
        return "";
    }

    private static String sanitizeCode(String code) {
        if (code == null || code.isEmpty()) return "";
        return InputSanitizer.forLLM(code, MAX_CODE_LENGTH).replaceAll("\n{3,}", "\n\n");
    }
}
