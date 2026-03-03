package com.minewright.skill.refinement;

import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMException;
import com.minewright.llm.async.LLMResponse;
import com.minewright.skill.Skill;
import com.minewright.skill.ValidationContext;
import com.minewright.skill.ValidationResult;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LLMFeedbackGenerator {
    private static final Logger LOGGER = TestLogger.getLogger(LLMFeedbackGenerator.class);
    private static final int MAX_RETRIES = 2;
    private static final long FEEDBACK_TIMEOUT_MS = 60000;
    private static final double DEFAULT_TEMPERATURE = 0.3;
    private static final int DEFAULT_MAX_TOKENS = 2000;

    private final AsyncLLMClient llmClient;
    private final String model;
    private final AtomicInteger feedbackRequests = new AtomicInteger(0);
    private final AtomicInteger feedbackSuccesses = new AtomicInteger(0);
    private final AtomicInteger feedbackFailures = new AtomicInteger(0);
    private final AtomicLong totalFeedbackTime = new AtomicLong(0);
    private final AtomicLong totalTokensUsed = new AtomicLong(0);

    public LLMFeedbackGenerator(AsyncLLMClient llmClient, String model) {
        if (llmClient == null) throw new IllegalArgumentException("LLM client cannot be null");
        if (model == null || model.isEmpty()) throw new IllegalArgumentException("Model cannot be null or empty");
        this.llmClient = llmClient;
        this.model = model;
    }

    public CompletableFuture<SkillFeedback> generateFeedback(Skill skill, ValidationResult validationResult, ValidationContext context) {
        if (skill == null || validationResult == null || context == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Null parameters"));
        }
        feedbackRequests.incrementAndGet();
        return generateFeedbackWithRetry(skill, validationResult, context, 0);
    }

    private CompletableFuture<SkillFeedback> generateFeedbackWithRetry(Skill skill, ValidationResult validationResult, ValidationContext context, int retryCount) {
        long startTime = System.currentTimeMillis();
        String prompt = RefinementPromptBuilder.buildFeedbackPrompt(skill, validationResult, context);
        Map<String, Object> params = new HashMap<>();
        params.put("model", model);
        params.put("maxTokens", DEFAULT_MAX_TOKENS);
        params.put("temperature", DEFAULT_TEMPERATURE);

        return llmClient.sendAsync(prompt, params)
            .orTimeout(FEEDBACK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .thenApply(response -> parseFeedbackResponse(response, skill.getName()))
            .thenApply(feedback -> {
                totalFeedbackTime.addAndGet(System.currentTimeMillis() - startTime);
                feedbackSuccesses.incrementAndGet();
                return feedback;
            })
            .exceptionally(throwable -> {
                if (retryCount < MAX_RETRIES && isRetryable(throwable)) {
                    return generateFeedbackWithRetry(skill, validationResult, context, retryCount + 1).join();
                }
                totalFeedbackTime.addAndGet(System.currentTimeMillis() - startTime);
                feedbackFailures.incrementAndGet();
                return createFallbackFeedback(validationResult);
            });
    }

    private SkillFeedback parseFeedbackResponse(LLMResponse response, String skillName) {
        try {
            totalTokensUsed.addAndGet(response.getTokensUsed());
            String content = response.getContent();
            String jsonContent = extractJSON(content);
            if (jsonContent == null) jsonContent = content;

            String analysis = extractStringField(jsonContent, "analysis");
            List<SkillFeedback.Issue> issues = extractIssues(jsonContent);
            List<String> suggestions = extractStringArray(jsonContent, "suggestions");
            List<String> requirements = extractStringArray(jsonContent, "validationRequirements");

            SkillFeedback.Builder builder = SkillFeedback.builder();
            if (analysis != null) builder.analysis(analysis);
            for (SkillFeedback.Issue issue : issues) {
                builder.issue(issue.getDescription(), issue.getSeverity(), issue.getLocation());
            }
            for (String suggestion : suggestions) builder.suggestion(suggestion);
            for (String requirement : requirements) builder.validationRequirement(requirement);
            return builder.build();
        } catch (Exception e) {
            return createFallbackFeedback(null);
        }
    }

    private String extractJSON(String content) {
        int start = content.indexOf('{');
        if (start == -1) return null;
        int braceCount = 0;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') {
                braceCount--;
                if (braceCount == 0) return content.substring(start, i + 1);
            }
        }
        return null;
    }

    private String extractStringField(String json, String fieldName) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\"");
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) return m.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        return null;
    }

    private List<SkillFeedback.Issue> extractIssues(String json) {
        List<SkillFeedback.Issue> issues = new ArrayList<>();
        int arrayStart = json.indexOf("\"issues\"");
        if (arrayStart == -1) return issues;
        int bracketStart = json.indexOf('[', arrayStart);
        if (bracketStart == -1) return issues;
        int bracketEnd = findMatchingBracket(json, bracketStart);
        if (bracketEnd == -1) return issues;
        String arrayContent = json.substring(bracketStart + 1, bracketEnd);

        int objStart = arrayContent.indexOf('{');
        while (objStart != -1) {
            int objEnd = findMatchingBrace(arrayContent, objStart);
            if (objEnd == -1) break;
            String objContent = arrayContent.substring(objStart + 1, objEnd);
            String description = extractStringField(objContent, "description");
            String severity = extractStringField(objContent, "severity");
            String location = extractStringField(objContent, "location");
            if (description != null) {
                issues.add(new SkillFeedback.Issue(description, severity != null ? severity : "medium", location));
            }
            objStart = arrayContent.indexOf('{', objEnd);
        }
        return issues;
    }

    private List<String> extractStringArray(String json, String arrayName) {
        List<String> result = new ArrayList<>();
        int arrayStart = json.indexOf("\"" + arrayName + "\"");
        if (arrayStart == -1) return result;
        int bracketStart = json.indexOf('[', arrayStart);
        if (bracketStart == -1) return result;
        int bracketEnd = findMatchingBracket(json, bracketStart);
        if (bracketEnd == -1) return result;
        String arrayContent = json.substring(bracketStart + 1, bracketEnd);

        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"([^\"]*)\"");
        java.util.regex.Matcher m = p.matcher(arrayContent);
        while (m.find()) result.add(m.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\"));
        return result;
    }

    private int findMatchingBracket(String str, int start) {
        int count = 1;
        for (int i = start + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '[') count++;
            else if (c == ']') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    private int findMatchingBrace(String str, int start) {
        int count = 1;
        for (int i = start + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '{') count++;
            else if (c == '}') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    private SkillFeedback createFallbackFeedback(ValidationResult result) {
        SkillFeedback.Builder builder = SkillFeedback.builder()
            .analysis("Unable to generate detailed feedback. Using basic validation result.");
        if (result != null && result.getReason() != null) {
            builder.issue(result.getReason(), "high").suggestion("Address: " + result.getReason());
        }
        return builder.validationRequirement("Skill must complete without errors").build();
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof TimeoutException) return true;
        if (throwable instanceof LLMException) {
            LLMException llmEx = (LLMException) throwable;
            return llmEx.isRetryable();
        }
        return false;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("feedbackRequests", feedbackRequests.get());
        stats.put("feedbackSuccesses", feedbackSuccesses.get());
        stats.put("feedbackFailures", feedbackFailures.get());
        stats.put("totalFeedbackTimeMs", totalFeedbackTime.get());
        stats.put("totalTokensUsed", totalTokensUsed.get());
        return stats;
    }
}
