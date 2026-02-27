package com.minewright.llm.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Local preprocessor for compiling and optimizing prompts before sending to cloud LLM.
 *
 * <p>This component uses local processing to:</p>
 * <ul>
 *   <li>Merge related prompts into single smarter requests</li>
 *   <li>Deduplicate redundant context</li>
 *   <li>Optimize prompt structure for better responses</li>
 *   <li>Add relevant context from local knowledge</li>
 * </ul>
 *
 * <h2>Batch Compilation Strategy</h2>
 *
 * <p>When multiple prompts are batched together, this preprocessor:</p>
 * <ol>
 *   <li>Groups prompts by type/category</li>
 *   <li>Extracts shared context</li>
 *   <li>Creates a unified system prompt</li>
 *   <li>Structures user prompts with clear separators</li>
 * </ol>
 *
 * @since 1.3.0
 */
public class LocalPreprocessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalPreprocessor.class);

    /**
     * Maximum tokens to target for compiled prompts.
     */
    private static final int TARGET_MAX_TOKENS = 4000;

    /**
     * Compiles multiple prompts into a single optimized batch.
     *
     * @param prompts The prompts to compile
     * @return A compiled batch ready to send
     */
    public PromptBatcher.CompiledBatch compileBatch(List<PromptBatcher.BatchedPrompt> prompts) {
        if (prompts.isEmpty()) {
            return new PromptBatcher.CompiledBatch("", "", prompts, Map.of());
        }

        if (prompts.size() == 1) {
            // Single prompt - just pass through with minor optimization
            return compileSingle(prompts.get(0));
        }

        // Multiple prompts - merge intelligently
        return compileMultiple(prompts);
    }

    /**
     * Compiles a single prompt with optimization.
     */
    private PromptBatcher.CompiledBatch compileSingle(PromptBatcher.BatchedPrompt prompt) {
        String optimizedPrompt = optimizePrompt(prompt.prompt);

        // Build context-aware system prompt
        String systemPrompt = buildSystemPrompt(prompt);

        Map<String, Object> params = new HashMap<>(prompt.context);
        params.put("maxTokens", estimateTokens(optimizedPrompt) + 500);

        return new PromptBatcher.CompiledBatch(systemPrompt, optimizedPrompt,
            List.of(prompt), params);
    }

    /**
     * Compiles multiple prompts into a smart batch.
     */
    private PromptBatcher.CompiledBatch compileMultiple(List<PromptBatcher.BatchedPrompt> prompts) {
        // Group by category
        Map<String, List<PromptBatcher.BatchedPrompt>> grouped = prompts.stream()
            .collect(Collectors.groupingBy(this::categorizePrompt));

        // Build unified system prompt
        String systemPrompt = buildUnifiedSystemPrompt(grouped);

        // Build structured user prompt
        String userPrompt = buildStructuredUserPrompt(grouped);

        // Calculate parameters
        Map<String, Object> params = new HashMap<>();
        params.put("maxTokens", Math.min(TARGET_MAX_TOKENS, estimateTokens(userPrompt) * 2));

        LOGGER.debug("Compiled {} prompts into batch (estimated tokens: {})",
            prompts.size(), estimateTokens(userPrompt));

        return new PromptBatcher.CompiledBatch(systemPrompt, userPrompt, prompts, params);
    }

    /**
     * Categorizes a prompt for grouping.
     */
    private String categorizePrompt(PromptBatcher.BatchedPrompt prompt) {
        // Extract category from context or prompt content
        if (prompt.context.containsKey("category")) {
            return (String) prompt.context.get("category");
        }

        // Infer from prompt type
        return switch (prompt.type) {
            case DIRECT_USER -> "conversation";
            case URGENT -> "urgent";
            case BACKGROUND -> "background";
            default -> "general";
        };
    }

    /**
     * Builds a unified system prompt for batched prompts.
     */
    private String buildUnifiedSystemPrompt(Map<String, List<PromptBatcher.BatchedPrompt>> grouped) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Steve AI Foreman - Batched Request\n\n");
        sb.append("You are processing multiple related requests in a single batch. ");
        sb.append("Please respond to each request clearly and separately.\n\n");

        // Add context based on categories present
        Set<String> categories = grouped.keySet();

        if (categories.contains("conversation")) {
            sb.append("## Conversation Mode\n");
            sb.append("Some of these are conversational responses. Be natural and engaging.\n\n");
        }

        if (categories.contains("background")) {
            sb.append("## Background Tasks\n");
            sb.append("Some requests are for background processing. Be efficient.\n\n");
        }

        if (categories.contains("urgent")) {
            sb.append("## Urgent Requests\n");
            sb.append("Some requests require immediate attention. Prioritize these.\n\n");
        }

        sb.append("## Response Format\n");
        sb.append("For each numbered request, provide a clear response. ");
        sb.append("Use the format: [N] Response where N is the request number.\n");

        return sb.toString();
    }

    /**
     * Builds a structured user prompt from grouped prompts.
     */
    private String buildStructuredUserPrompt(Map<String, List<PromptBatcher.BatchedPrompt>> grouped) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Batched Requests\n\n");

        int requestNum = 1;

        // Process urgent first
        if (grouped.containsKey("urgent")) {
            sb.append("## Urgent Requests\n\n");
            for (PromptBatcher.BatchedPrompt p : grouped.get("urgent")) {
                sb.append("### Request ").append(requestNum++).append("\n");
                sb.append(mergeContext(p)).append("\n\n");
                sb.append(p.prompt).append("\n\n");
            }
        }

        // Then conversation
        if (grouped.containsKey("conversation")) {
            sb.append("## Conversation Requests\n\n");
            for (PromptBatcher.BatchedPrompt p : grouped.get("conversation")) {
                sb.append("### Request ").append(requestNum++).append("\n");
                sb.append(mergeContext(p)).append("\n\n");
                sb.append(p.prompt).append("\n\n");
            }
        }

        // Then background
        if (grouped.containsKey("background")) {
            sb.append("## Background Requests\n\n");
            for (PromptBatcher.BatchedPrompt p : grouped.get("background")) {
                sb.append("### Request ").append(requestNum++).append("\n");
                sb.append(mergeContext(p)).append("\n\n");
                sb.append(p.prompt).append("\n\n");
            }
        }

        // Then general
        if (grouped.containsKey("general")) {
            sb.append("## General Requests\n\n");
            for (PromptBatcher.BatchedPrompt p : grouped.get("general")) {
                sb.append("### Request ").append(requestNum++).append("\n");
                sb.append(mergeContext(p)).append("\n\n");
                sb.append(p.prompt).append("\n\n");
            }
        }

        sb.append("---\n");
        sb.append("Please respond to each request above using the format [N] for each.\n");

        return sb.toString();
    }

    /**
     * Builds a context-aware system prompt for single prompts.
     */
    private String buildSystemPrompt(PromptBatcher.BatchedPrompt prompt) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Steve AI Foreman\n\n");

        // Add type-specific instructions
        switch (prompt.type) {
            case DIRECT_USER:
                sb.append("You are Steve, a friendly AI companion and foreman in Minecraft. ");
                sb.append("Respond naturally and conversationally.\n");
                break;

            case URGENT:
                sb.append("You are Steve, an AI foreman handling an urgent request. ");
                sb.append("Provide clear, immediate guidance.\n");
                break;

            case BACKGROUND:
                sb.append("You are Steve, processing a background task. ");
                sb.append("Be efficient and focused.\n");
                break;

            default:
                sb.append("You are Steve, an AI foreman assisting with Minecraft tasks.\n");
        }

        // Add context if available
        if (prompt.context.containsKey("systemPrompt")) {
            sb.append("\n").append(prompt.context.get("systemPrompt")).append("\n");
        }

        return sb.toString();
    }

    /**
     * Merges context from a prompt into a readable format.
     */
    private String mergeContext(PromptBatcher.BatchedPrompt prompt) {
        if (prompt.context.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Context:\n");

        for (Map.Entry<String, Object> entry : prompt.context.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("systemPrompt") && !key.equals("maxTokens")) {
                sb.append("- ").append(key).append(": ").append(entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Optimizes a single prompt for better LLM response.
     */
    private String optimizePrompt(String prompt) {
        // Remove excessive whitespace
        prompt = prompt.replaceAll("\\s+", " ").trim();

        // Ensure prompt ends with clear instruction if it's a question
        if (prompt.contains("?") && !prompt.endsWith("?") && !prompt.endsWith(".")) {
            prompt = prompt + ".";
        }

        return prompt;
    }

    /**
     * Estimates token count for a prompt.
     * Simple heuristic: ~4 characters per token.
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }

    // === Smart Deduplication ===

    /**
     * Checks if two prompts are semantically similar enough to merge.
     */
    public boolean areSimilar(String prompt1, String prompt2) {
        // Simple similarity check based on common words
        Set<String> words1 = extractWords(prompt1);
        Set<String> words2 = extractWords(prompt2);

        // Jaccard similarity
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) return false;

        double similarity = (double) intersection.size() / union.size();
        return similarity > 0.5; // 50% overlap threshold
    }

    /**
     * Extracts significant words from text.
     */
    private Set<String> extractWords(String text) {
        if (text == null) return Set.of();

        return Arrays.stream(text.toLowerCase().split("\\W+"))
            .filter(w -> w.length() > 3) // Skip short words
            .filter(w -> !isStopWord(w))
            .collect(Collectors.toSet());
    }

    /**
     * Common stop words to ignore.
     */
    private static final Set<String> STOP_WORDS = Set.of(
        "the", "and", "for", "are", "but", "not", "you", "all", "can", "had",
        "her", "was", "one", "our", "out", "has", "have", "been", "will",
        "your", "from", "they", "this", "that", "with", "what", "when", "where"
    );

    private boolean isStopWord(String word) {
        return STOP_WORDS.contains(word);
    }
}
