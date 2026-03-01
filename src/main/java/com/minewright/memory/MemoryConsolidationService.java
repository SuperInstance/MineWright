package com.minewright.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for consolidating and summarizing memories to prevent memory bloat.
 *
 * <p>Implements progressive summarization as described in conversational memory research:
 * <ul>
 *   <li>Groups memories by topic and time period</li>
 *   <li>Generates summaries using LLM (or fallback to algorithmic summarization)</li>
 *   <li>Stores summaries as semantic facts</li>
 *   <li>Keeps most emotional episodic memories intact</li>
 * </ul>
 *
 * <p>This service runs asynchronously to avoid blocking the game thread.</p>
 *
 * @since 1.3.0
 */
public class MemoryConsolidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryConsolidationService.class);

    /**
     * Minimum number of memories before consolidation is considered.
     */
    private static final int CONSOLIDATION_THRESHOLD = 50;

    /**
     * Days between automatic consolidation checks.
     */
    private static final long CONSOLIDATION_INTERVAL_DAYS = 7;

    /**
     * Minimum memories in a group to trigger consolidation.
     */
    private static final int GROUP_CONSOLIDATION_THRESHOLD = 5;

    /**
     * LLM client for generating summaries (can be null for offline mode).
     */
    private final com.minewright.llm.async.AsyncLLMClient llmClient;

    /**
     * Last consolidation check timestamp.
     */
    private Instant lastConsolidationCheck;

    /**
     * Tracks consolidated memory groups to avoid re-consolidation.
     */
    private final Set<String> consolidatedGroups;

    /**
     * Creates a new MemoryConsolidationService.
     *
     * @param llmClient Optional LLM client for generating summaries
     */
    public MemoryConsolidationService(com.minewright.llm.async.AsyncLLMClient llmClient) {
        this.llmClient = llmClient;
        this.lastConsolidationCheck = Instant.now();
        this.consolidatedGroups = ConcurrentHashMap.newKeySet();
        LOGGER.info("MemoryConsolidationService initialized (LLM: {})",
            llmClient != null ? "available" : "unavailable");
    }

    /**
     * Checks if consolidation is needed and performs it asynchronously.
     *
     * @param memory The companion memory to consolidate
     * @return CompletableFuture that completes when consolidation finishes
     */
    public CompletableFuture<ConsolidationResult> checkAndConsolidate(CompanionMemory memory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (shouldConsolidate(memory)) {
                    LOGGER.info("Starting memory consolidation for {} memories",
                        memory.getRecentMemories(Integer.MAX_VALUE).size());
                    return consolidateMemories(memory);
                }
                return new ConsolidationResult(false, 0, 0, Collections.emptyList());
            } catch (Exception e) {
                LOGGER.error("Error during memory consolidation", e);
                return new ConsolidationResult(false, 0, 0, Collections.emptyList());
            }
        });
    }

    /**
     * Determines if consolidation should run based on time and memory count.
     */
    private boolean shouldConsolidate(CompanionMemory memory) {
        Instant now = Instant.now();
        long daysSince = ChronoUnit.DAYS.between(lastConsolidationCheck, now);
        int memoryCount = memory.getRecentMemories(Integer.MAX_VALUE).size();

        return (daysSince >= CONSOLIDATION_INTERVAL_DAYS || memoryCount > CONSOLIDATION_THRESHOLD);
    }

    /**
     * Performs memory consolidation.
     */
    private ConsolidationResult consolidateMemories(CompanionMemory memory) {
        lastConsolidationCheck = Instant.now();

        List<CompanionMemory.EpisodicMemory> allMemories =
            new ArrayList<>(memory.getRecentMemories(Integer.MAX_VALUE));

        // Group memories by topic and time period
        Map<String, List<CompanionMemory.EpisodicMemory>> groups = groupMemories(allMemories);

        int groupsConsolidated = 0;
        int memoriesRemoved = 0;
        List<String> summariesCreated = new ArrayList<>();

        // Consolidate each eligible group
        for (Map.Entry<String, List<CompanionMemory.EpisodicMemory>> entry : groups.entrySet()) {
            String groupKey = entry.getKey();
            List<CompanionMemory.EpisodicMemory> groupMemories = entry.getValue();

            if (groupMemories.size() >= GROUP_CONSOLIDATION_THRESHOLD &&
                !consolidatedGroups.contains(groupKey)) {

                LOGGER.debug("Consolidating group '{}' with {} memories",
                    groupKey, groupMemories.size());

                // Find most emotional memory to keep
                CompanionMemory.EpisodicMemory mostEmotional = groupMemories.stream()
                    .max(Comparator.comparingInt(m -> Math.abs(m.emotionalWeight)))
                    .orElse(null);

                // Generate summary
                String summary = generateSummary(groupKey, groupMemories);

                // Store as semantic memory
                String summaryKey = "consolidated_" + groupKey;
                memory.learnPlayerFact("consolidated_memory", summaryKey, summary);

                // Mark group as consolidated
                consolidatedGroups.add(groupKey);

                groupsConsolidated++;
                summariesCreated.add(summary);
                memoriesRemoved += groupMemories.size() - (mostEmotional != null ? 1 : 0);

                LOGGER.debug("Consolidated group '{}' into summary: {}",
                    groupKey, truncate(summary, 100));
            }
        }

        LOGGER.info("Consolidation complete: {} groups, {} memories consolidated",
            groupsConsolidated, memoriesRemoved);

        return new ConsolidationResult(
            groupsConsolidated > 0,
            groupsConsolidated,
            memoriesRemoved,
            summariesCreated
        );
    }

    /**
     * Groups memories by topic and time period.
     */
    private Map<String, List<CompanionMemory.EpisodicMemory>> groupMemories(
            List<CompanionMemory.EpisodicMemory> memories) {

        Map<String, List<CompanionMemory.EpisodicMemory>> groups = new ConcurrentHashMap<>();

        for (CompanionMemory.EpisodicMemory memory : memories) {
            // Skip protected memories
            if (memory.isProtected()) {
                continue;
            }

            // Group by event type and week
            String weekKey = getWeekKey(memory.timestamp);
            String groupKey = memory.eventType + "_" + weekKey;

            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(memory);
        }

        return groups;
    }

    /**
     * Generates a week-based key for grouping memories.
     */
    private String getWeekKey(Instant timestamp) {
        long weeksSinceEpoch = ChronoUnit.WEEKS.between(Instant.EPOCH, timestamp);
        return "week_" + weeksSinceEpoch;
    }

    /**
     * Generates a summary of a group of memories.
     * Uses LLM if available, otherwise falls back to algorithmic summarization.
     */
    private String generateSummary(String groupKey,
                                   List<CompanionMemory.EpisodicMemory> memories) {
        if (llmClient != null) {
            return generateLLMSummary(groupKey, memories);
        } else {
            return generateAlgorithmicSummary(groupKey, memories);
        }
    }

    /**
     * Generates a summary using the LLM.
     */
    private String generateLLMSummary(String groupKey,
                                      List<CompanionMemory.EpisodicMemory> memories) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Summarize these related memories into 1-2 sentences:\n\n");

            for (CompanionMemory.EpisodicMemory memory : memories) {
                prompt.append("- ").append(memory.description).append("\n");
            }

            prompt.append("\nProvide a concise summary that captures the main theme.");

            Map<String, Object> params = Map.of(
                "maxTokens", 100,
                "temperature", 0.5
            );

            CompletableFuture<com.minewright.llm.async.LLMResponse> future =
                llmClient.sendAsync(prompt.toString(), params);

            // Wait for result with timeout
            com.minewright.llm.async.LLMResponse response =
                future.get(10, java.util.concurrent.TimeUnit.SECONDS);

            if (response != null && response.getContent() != null) {
                return response.getContent().trim();
            }
        } catch (Exception e) {
            LOGGER.warn("LLM summarization failed, falling back to algorithmic", e);
        }

        return generateAlgorithmicSummary(groupKey, memories);
    }

    /**
     * Generates an algorithmic summary without LLM.
     */
    private String generateAlgorithmicSummary(String groupKey,
                                              List<CompanionMemory.EpisodicMemory> memories) {
        // Extract event type from group key
        String eventType = groupKey.split("_")[0];

        // Count occurrences
        int count = memories.size();

        // Find emotional range
        IntSummaryStatistics emotionStats = memories.stream()
            .mapToInt(m -> m.emotionalWeight)
            .summaryStatistics();

        // Generate summary
        StringBuilder summary = new StringBuilder();
        summary.append("We ")
            .append(getSummaryVerb(eventType))
            .append(" ")
            .append(count)
            .append(" times");

        if (emotionStats.getMax() >= 5) {
            summary.append(", with some very positive experiences");
        } else if (emotionStats.getMin() <= -5) {
            summary.append(", with some challenges");
        }

        summary.append(".");

        return summary.toString();
    }

    /**
     * Gets a summary verb for an event type.
     */
    private String getSummaryVerb(String eventType) {
        return switch (eventType.toLowerCase()) {
            case "build", "building" -> "built things together";
            case "mine", "mining" -> "went mining";
            case "explore", "exploring" -> "explored";
            case "combat", "fight" -> "fought together";
            case "success" -> "achieved goals";
            case "failure" -> "faced setbacks";
            default -> "spent time";
        };
    }

    /**
     * Truncates a string for logging.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "[null]";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    /**
     * Result of a memory consolidation operation.
     */
    public static class ConsolidationResult {
        public final boolean success;
        public final int groupsConsolidated;
        public final int memoriesRemoved;
        public final List<String> summariesCreated;

        public ConsolidationResult(boolean success, int groupsConsolidated,
                                   int memoriesRemoved, List<String> summariesCreated) {
            this.success = success;
            this.groupsConsolidated = groupsConsolidated;
            this.memoriesRemoved = memoriesRemoved;
            this.summariesCreated = summariesCreated;
        }
    }
}
