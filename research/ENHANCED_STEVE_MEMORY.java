package com.minewright.memory;

import com.minewright.MineWrightMod;
import com.minewright.llm.embeddings.LocalEmbeddingModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Enhanced ForemanMemory with semantic search capabilities.
 *
 * <p>This implementation uses local embedding models to enable semantic search,
 * allowing the AI to retrieve relevant context based on meaning rather than
 * exact keyword matching.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Semantic search: Find memories by meaning, not just keywords</li>
 *   <li>Temporal decay: Recent memories are weighted more heavily</li>
 *   <li>Type filtering: Search specific memory types (actions, conversations, observations)</li>
 *   <li>Deduplication: Avoid storing duplicate memories</li>
 *   <li>Offline operation: No API calls required</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * // Initialize with embedding model
 * EnhancedForemanMemory memory = new EnhancedForemanMemory("models/all-MiniLM-L6-v2.onnx");
 *
 * // Add memories
 * memory.addMemory("crew member mined diamond ore at Y=-54", "action");
 * memory.addMemory("player asked foreman to build a house", "conversation");
 * memory.addMemory("found village near spawn", "observation");
 *
 * // Semantic search (finds relevant memories even with different wording)
 * List&lt;MemoryEntry&gt; relevant = memory.searchRelevant(
 *     "underground mining resources",
 *     5,  // top K results
 *     0.6 // minimum similarity threshold
 * );
 *
 * // Result includes: "crew member mined diamond ore at Y=-54"
 * // Because "underground mining" is semantically similar to "mined diamond"
 *
 * memory.close();
 * </pre>
 *
 * @author MineWright Team
 * @version 2.0
 * @since 1.2.0
 */
public class EnhancedForemanMemory implements AutoCloseable {

    private final LocalEmbeddingModel embeddingModel;
    private final List<MemoryEntry> memories;
    private final Map<String, Set<String>> deduplicationIndex;

    // Configuration
    private final int maxMemories;
    private final double temporalDecayFactor;
    private final int maxTokensPerMemory;

    /**
     * Creates a new EnhancedForemanMemory instance.
     *
     * @param embeddingModelPath Path to ONNX embedding model
     * @throws Exception if model fails to load
     */
    public EnhancedForemanMemory(String embeddingModelPath) throws Exception {
        this(embeddingModelPath, 10000, 0.1, 512);
    }

    /**
     * Creates a new EnhancedForemanMemory instance with custom configuration.
     *
     * @param embeddingModelPath Path to ONNX embedding model
     * @param maxMemories Maximum number of memories to store
     * @param temporalDecayFactor Factor for temporal decay (0.0-1.0)
     * @param maxTokensPerMemory Maximum tokens per memory entry
     * @throws Exception if model fails to load
     */
    public EnhancedForemanMemory(
        String embeddingModelPath,
        int maxMemories,
        double temporalDecayFactor,
        int maxTokensPerMemory
    ) throws Exception {
        MineWrightMod.LOGGER.info("Initializing EnhancedForemanMemory...");

        this.embeddingModel = new LocalEmbeddingModel(embeddingModelPath);
        this.memories = new CopyOnWriteArrayList<>();
        this.deduplicationIndex = new ConcurrentHashMap<>();
        this.maxMemories = maxMemories;
        this.temporalDecayFactor = temporalDecayFactor;
        this.maxTokensPerMemory = maxTokensPerMemory;

        MineWrightMod.LOGGER.info("EnhancedForemanMemory initialized successfully");
    }

    /**
     * Adds a memory entry to the store.
     *
     * <p>This method will:</p>
     * <ul>
     *   <li>Generate an embedding for semantic search</li>
     *   <li>Check for duplicates</li>
     *   <li>Enforce memory limits</li>
     * </ul>
     *
     * @param content Memory content
     * @param type Memory type (action, conversation, observation, etc.)
     * @return true if memory was added, false if duplicate
     */
    public boolean addMemory(String content, String type) {
        try {
            // Check for duplicates
            if (isDuplicate(content, type)) {
                MineWrightMod.LOGGER.debug("Duplicate memory detected, skipping: {}", content);
                return false;
            }

            // Truncate if too long
            String truncatedContent = truncateContent(content);

            // Generate embedding
            float[] embedding = embeddingModel.generateEmbedding(truncatedContent);

            // Create memory entry
            MemoryEntry entry = new MemoryEntry(
                truncatedContent,
                type,
                embedding,
                System.currentTimeMillis(),
                1.0 // initial importance
            );

            // Add to memories
            memories.add(entry);

            // Update deduplication index
            deduplicationIndex.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet())
                .add(generateSignature(truncatedContent));

            // Enforce memory limits (remove oldest if necessary)
            enforceMemoryLimit();

            MineWrightMod.LOGGER.debug("Added memory: [{}] {}", type, truncatedContent);

            return true;

        } catch (Exception e) {
            MineWrightMod.LOGGER.error("Failed to add memory: {}", content, e);
            return false;
        }
    }

    /**
     * Performs semantic search for relevant memories.
     *
     * <p>This search finds memories that are semantically similar to the query,
     * even if they don't share exact keywords.</p>
     *
     * @param query Search query
     * @param topK Number of results to return
     * @param minSimilarity Minimum similarity threshold (0.0-1.0)
     * @return List of relevant memories, sorted by relevance
     */
    public List<MemoryEntry> searchRelevant(String query, int topK, double minSimilarity) {
        try {
            long startTime = System.currentTimeMillis();

            // Generate query embedding
            float[] queryEmbedding = embeddingModel.generateEmbedding(query);

            // Calculate similarity scores with temporal decay
            List<ScoredMemory> scoredMemories = new ArrayList<>();

            long currentTime = System.currentTimeMillis();

            for (MemoryEntry memory : memories) {
                // Semantic similarity
                double semanticSimilarity = embeddingModel.cosineSimilarity(
                    queryEmbedding,
                    memory.getEmbedding()
                );

                // Temporal decay: recent memories are more relevant
                long ageMs = currentTime - memory.getTimestamp();
                double temporalWeight = Math.exp(-temporalDecayFactor * ageMs / 3600000.0); // decay per hour

                // Combined score
                double combinedScore = semanticSimilarity * temporalWeight * memory.getImportance();

                if (combinedScore >= minSimilarity) {
                    scoredMemories.add(new ScoredMemory(memory, combinedScore));
                }
            }

            // Sort by score (descending)
            scoredMemories.sort((a, b) -> Double.compare(b.score, a.score));

            // Get top K
            List<MemoryEntry> results = scoredMemories.stream()
                .limit(topK)
                .map(sm -> sm.memory)
                .collect(Collectors.toList());

            long searchTime = System.currentTimeMillis() - startTime;
            MineWrightMod.LOGGER.debug("Semantic search returned {} results in {}ms",
                results.size(), searchTime);

            return results;

        } catch (Exception e) {
            MineWrightMod.LOGGER.error("Semantic search failed", e);
            return Collections.emptyList();
        }
    }

    /**
     * Performs semantic search with default parameters.
     *
     * @param query Search query
     * @return List of relevant memories (top 10, min similarity 0.5)
     */
    public List<MemoryEntry> searchRelevant(String query) {
        return searchRelevant(query, 10, 0.5);
    }

    /**
     * Filters memories by type.
     *
     * @param type Memory type to filter by
     * @return List of memories of the specified type
     */
    public List<MemoryEntry> getMemoriesByType(String type) {
        return memories.stream()
            .filter(m -> m.getType().equals(type))
            .collect(Collectors.toList());
    }

    /**
     * Gets recent memories within a time window.
     *
     * @param minutesAgo Time window in minutes
     * @return List of recent memories
     */
    public List<MemoryEntry> getRecentMemories(int minutesAgo) {
        long cutoff = System.currentTimeMillis() - (minutesAgo * 60000L);

        return memories.stream()
            .filter(m -> m.getTimestamp() >= cutoff)
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .collect(Collectors.toList());
    }

    /**
     * Boosts the importance of specific memories.
     *
     * <p>Useful for highlighting important events that should be
     * prioritized in future searches.</p>
     *
     * @param query Query to find memories
     * @param boostFactor Factor to multiply importance by
     */
    public void boostImportance(String query, double boostFactor) {
        try {
            float[] queryEmbedding = embeddingModel.generateEmbedding(query);

            for (MemoryEntry memory : memories) {
                double similarity = embeddingModel.cosineSimilarity(
                    queryEmbedding,
                    memory.getEmbedding()
                );

                if (similarity > 0.7) {
                    memory.setImportance(memory.getImportance() * boostFactor);
                }
            }

        } catch (Exception e) {
            MineWrightMod.LOGGER.error("Failed to boost importance", e);
        }
    }

    /**
     * Returns formatted context string for LLM prompts.
     *
     * @param query Query to find relevant context
     * @param maxMemories Maximum memories to include
     * @return Formatted context string
     */
    public String getContextForLLM(String query, int maxMemories) {
        List<MemoryEntry> relevant = searchRelevant(query, maxMemories, 0.4);

        if (relevant.isEmpty()) {
            return "No relevant memories found.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Relevant memories:\n");

        for (int i = 0; i < relevant.size(); i++) {
            MemoryEntry memory = relevant.get(i);
            context.append(String.format("%d. [%s] %s\n",
                i + 1,
                memory.getType(),
                memory.getContent()
            ));
        }

        return context.toString();
    }

    /**
     * Returns statistics about memory usage.
     *
     * @return Map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalMemories", memories.size());
        stats.put("maxMemories", maxMemories);

        // Count by type
        Map<String, Long> countsByType = memories.stream()
            .collect(Collectors.groupingBy(
                MemoryEntry::getType,
                Collectors.counting()
            ));
        stats.put("memoriesByType", countsByType);

        // Average importance
        double avgImportance = memories.stream()
            .mapToDouble(MemoryEntry::getImportance)
            .average()
            .orElse(0.0);
        stats.put("averageImportance", avgImportance);

        // Memory age stats
        long now = System.currentTimeMillis();
        OptionalLong oldestMemory = memories.stream()
            .mapToLong(MemoryEntry::getTimestamp)
            .min();

        if (oldestMemory.isPresent()) {
            long oldestAgeHours = (now - oldestMemory.getAsLong()) / 3600000;
            stats.put("oldestMemoryAgeHours", oldestAgeHours);
        }

        return stats;
    }

    /**
     * Clears all memories.
     */
    public void clear() {
        memories.clear();
        deduplicationIndex.clear();
        MineWrightMod.LOGGER.info("Memory cleared");
    }

    @Override
    public void close() {
        try {
            if (embeddingModel != null) {
                embeddingModel.close();
            }
        } catch (Exception e) {
            MineWrightMod.LOGGER.error("Error closing memory", e);
        }
    }

    // ========== Private Methods ==========

    private boolean isDuplicate(String content, String type) {
        String signature = generateSignature(content);

        Set<String> typeSignatures = deduplicationIndex.get(type);
        return typeSignatures != null && typeSignatures.contains(signature);
    }

    private String generateSignature(String content) {
        // Simple signature: first 5 words normalized
        String[] words = content.toLowerCase().split("\\s+");
        return Arrays.stream(words, 0, Math.min(5, words.length))
            .collect(Collectors.joining(" "));
    }

    private String truncateContent(String content) {
        // Simple token-based truncation
        String[] tokens = content.split("(?<=\\s)|(?=\\s)");
        if (tokens.length <= maxTokensPerMemory) {
            return content;
        }

        return String.join("", Arrays.copyOf(tokens, maxTokensPerMemory)) + "...";
    }

    private void enforceMemoryLimit() {
        while (memories.size() > maxMemories) {
            // Remove oldest memory
            MemoryEntry oldest = memories.stream()
                .min(Comparator.comparingLong(MemoryEntry::getTimestamp))
                .orElse(null);

            if (oldest != null) {
                memories.remove(oldest);

                // Update deduplication index
                Set<String> typeSignatures = deduplicationIndex.get(oldest.getType());
                if (typeSignatures != null) {
                    typeSignatures.remove(generateSignature(oldest.getContent()));
                }
            }
        }
    }

    // ========== Inner Classes ==========

    /**
     * Memory entry with embedding and metadata.
     */
    public static class MemoryEntry {
        private final String content;
        private final String type;
        private final float[] embedding;
        private final long timestamp;
        private double importance;

        public MemoryEntry(String content, String type, float[] embedding,
                          long timestamp, double importance) {
            this.content = content;
            this.type = type;
            this.embedding = embedding;
            this.timestamp = timestamp;
            this.importance = importance;
        }

        public String getContent() { return content; }
        public String getType() { return type; }
        public float[] getEmbedding() { return embedding; }
        public long getTimestamp() { return timestamp; }
        public double getImportance() { return importance; }
        public void setImportance(double importance) { this.importance = importance; }
    }

    /**
     * Memory with associated relevance score.
     */
    private static class ScoredMemory {
        final MemoryEntry memory;
        final double score;

        ScoredMemory(MemoryEntry memory, double score) {
            this.memory = memory;
            this.score = score;
        }
    }
}
