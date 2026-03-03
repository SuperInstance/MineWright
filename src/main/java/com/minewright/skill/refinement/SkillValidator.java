package com.minewright.skill.refinement;

import com.minewright.skill.ExecutableSkill;
import com.minewright.skill.Skill;
import com.minewright.skill.ValidationResult;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SkillValidator {
    private static final Logger LOGGER = TestLogger.getLogger(SkillValidator.class);

    private static final Pattern[] DANGEROUS_PATTERNS = {
        Pattern.compile("\\beval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bFunction\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsetTimeout\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bwhile\\s*\\(\\s*true\\s*\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bfor\\s*\\(\\s*;\\s*;\\s*\\)", Pattern.CASE_INSENSITIVE)
    };

    public ValidationResult validate(ExecutableSkill refinedSkill) {
        return validateForRefinement(refinedSkill, null);
    }

    public ValidationResult validateForRefinement(ExecutableSkill refinedSkill, Skill originalSkill) {
        if (refinedSkill == null) {
            return new ValidationResult(false, "Refined skill is null", 0);
        }

        String code = extractCode(refinedSkill);
        ValidationResult syntaxResult = validateSyntax(code);
        if (!syntaxResult.isValid()) return syntaxResult;

        ValidationResult safetyResult = validateSafety(code);
        if (!safetyResult.isValid()) return safetyResult;

        if (originalSkill != null) {
            ValidationResult improvementResult = validateImprovement(refinedSkill, originalSkill);
            if (!improvementResult.isValid()) return improvementResult;
        }

        return new ValidationResult(true, null, 0);
    }

    private ValidationResult validateSyntax(String code) {
        if (code == null || code.trim().isEmpty()) {
            return new ValidationResult(false, "Code is empty", 0);
        }

        int braceCount = 0;
        int parenCount = 0;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            if (c == '\'' || c == '"' || c == '`') {
                char quote = c;
                i++;
                while (i < code.length() && code.charAt(i) != quote) {
                    if (code.charAt(i) == '\\') i++;
                    i++;
                }
                continue;
            }

            if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
                while (i < code.length() && code.charAt(i) != '\n') i++;
                continue;
            }

            switch (c) {
                case '{': braceCount++; break;
                case '}':
                    braceCount--;
                    if (braceCount < 0) return new ValidationResult(false, "Unmatched closing brace", 0);
                    break;
                case '(': parenCount++; break;
                case ')':
                    parenCount--;
                    if (parenCount < 0) return new ValidationResult(false, "Unmatched closing parenthesis", 0);
                    break;
            }
        }

        if (braceCount != 0) return new ValidationResult(false, "Unmatched braces", 0);
        if (parenCount != 0) return new ValidationResult(false, "Unmatched parentheses", 0);

        return new ValidationResult(true, null, 0);
    }

    private ValidationResult validateSafety(String code) {
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(code).find()) {
                return new ValidationResult(false, "Dangerous pattern detected", 0);
            }
        }
        return new ValidationResult(true, null, 0);
    }

    private ValidationResult validateImprovement(ExecutableSkill refinedSkill, Skill originalSkill) {
        String originalCode = extractCode(originalSkill);
        String refinedCode = extractCode(refinedSkill);

        if (originalCode.equals(refinedCode)) {
            return new ValidationResult(false, "Refined code is identical to original", 0);
        }

        return new ValidationResult(true, null, 0);
    }

    private String extractCode(Skill skill) {
        if (skill instanceof ExecutableSkill) {
            return ((ExecutableSkill) skill).generateCode(Map.of());
        }
        return "";
    }

    public Map<String, ValidationResult> batchValidate(List<ExecutableSkill> skills) {
        Map<String, ValidationResult> results = new java.util.HashMap<>();
        for (ExecutableSkill skill : skills) {
            results.put(skill.getName(), validate(skill));
        }
        return results;
    }
}
