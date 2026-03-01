package com.minewright.memory;

import com.minewright.memory.embedding.EmbeddingModel;
import com.minewright.memory.embedding.PlaceholderEmbeddingModel;
import com.minewright.memory.vector.InMemoryVectorStore;
import com.minewright.memory.vector.VectorSearchResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Advanced memory system for companion AI that supports relationship building,
 * personality development, and shared experiences with the player.
 *
 * <p>This system tracks multiple types of memories to create a rich,
 * evolving relationship between the foreman MineWright and the player.</p>
 *
 * <p><b>Memory Types:</b></p>
 * <ul>
 *   <li><b>Episodic:</b> Specific events and experiences shared together</li>
 *   <li><b>Semantic:</b> Facts learned about the player (preferences, playstyle)</li>
 *   <li><b>Emotional:</b> Memorable moments with emotional weight</li>
 *   <li><b>Conversational:</b> Topics discussed, inside jokes, catchphrases</li>
 *   <li><b>Working:</b> Current context and recent interactions</li>
 * </ul>
 *
 * <p><b>Relationship Development:</b></p>
 * <ul>
 *   <li>Rapport level (0-100) based on interactions</li>
 *   <li>Trust level based on shared successes/failures</li>
 *   <li>Inside joke collection</li>
 *   <li>Shared milestone memories</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class CompanionMemory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanionMemory.class);

    /**
     * Maximum episodic memories to retain.
     */
    private static final int MAX_EPISODIC_MEMORIES = 200;

    /**
     * Maximum working memory entries.
     */
    private static final int MAX_WORKING_MEMORY = 20;

    /**
     * Maximum inside jokes to track.
     */
    private static final int MAX_INSIDE_JOKES = 30;

    // === Core Memory Stores ===

    /**
     * Episodic memories - specific events and experiences.
     */
    private final Deque<EpisodicMemory> episodicMemories;

    /**
     * Semantic memories - facts about the player.
     */
    private final Map<String, SemanticMemory> semanticMemories;

    /**
     * Emotional memories - high-impact moments.
     * Uses CopyOnWriteArrayList for thread-safe iteration and modification.
     */
    private final List<EmotionalMemory> emotionalMemories;

    /**
     * Conversational memories - topics, jokes, references.
     */
    private final ConversationalMemory conversationalMemory;

    /**
     * Working memory - recent context.
     */
    private final Deque<WorkingMemoryEntry> workingMemory;

    // === Relationship Tracking ===

    /**
     * Overall rapport level (0-100).
     */
    private final AtomicInteger rapportLevel;

    /**
     * Trust level based on shared experiences.
     */
    private final AtomicInteger trustLevel;

    /**
     * Total interactions count.
     */
    private final AtomicInteger interactionCount;

    /**
     * First meeting timestamp.
     */
    private Instant firstMeeting;

    /**
     * Player name (learned on first interaction).
     */
    private String playerName;

    /**
     * Player preferences discovered.
     */
    private final Map<String, Object> playerPreferences;

    /**
     * Player playstyle observations.
     */
    private final Map<String, Integer> playstyleMetrics;

    // === Personality ===

    /**
     * Foreman's personality traits.
     */
    private final PersonalityProfile personality;

    // === Session Context ===

    /**
     * Current session start time.
     */
    private Instant sessionStart;

    /**
     * Topics discussed in current session.
     */
    private final Set<String> sessionTopics;

    // === Vector Search Support ===

    /**
     * Embedding model for generating text embeddings.
     */
    private final EmbeddingModel embeddingModel;

    /**
     * Vector store for semantic memory search.
     */
    private final InMemoryVectorStore<EpisodicMemory> memoryVectorStore;

    /**
     * Maps episodic memory to vector store ID.
     */
    private final Map<EpisodicMemory, Integer> memoryToVectorId;

    /**
     * Milestone tracker for relationship milestones.
     */
    private final MilestoneTracker milestoneTracker;

    /**
     * Creates a new CompanionMemory instance.
     */
    public CompanionMemory() {
        this.episodicMemories = new ArrayDeque<>();
        this.semanticMemories = new ConcurrentHashMap<>();
        this.emotionalMemories = new CopyOnWriteArrayList<>(); // Thread-safe
        this.conversationalMemory = new ConversationalMemory();
        this.workingMemory = new ArrayDeque<>();

        this.rapportLevel = new AtomicInteger(10); // Start with low rapport
        this.trustLevel = new AtomicInteger(5);
        this.interactionCount = new AtomicInteger(0);

        this.playerPreferences = new ConcurrentHashMap<>();
        this.playstyleMetrics = new ConcurrentHashMap<>();

        this.personality = new PersonalityProfile();
        this.sessionTopics = ConcurrentHashMap.newKeySet();

        // Initialize vector search infrastructure
        this.embeddingModel = new PlaceholderEmbeddingModel();
        this.memoryVectorStore = new InMemoryVectorStore<>(embeddingModel.getDimension());
        this.memoryToVectorId = new ConcurrentHashMap<>();

        // Initialize milestone tracker
        this.milestoneTracker = new MilestoneTracker();

        LOGGER.info("CompanionMemory initialized with vector search (model: {})",
                embeddingModel.getModelName());
    }

    // === Memory Recording ===

    /**
     * Records a shared experience with the player.
     *
     * @param eventType Type of event (build, explore, combat, etc.)
     * @param description What happened
     * @param emotionalWeight How memorable (-10 to +10)
     */
    public void recordExperience(String eventType, String description, int emotionalWeight) {
        EpisodicMemory memory = new EpisodicMemory(
            eventType, description, emotionalWeight, Instant.now()
        );

        episodicMemories.addFirst(memory);

        // Add to vector store for semantic search
        addMemoryToVectorStore(memory);

        // Trim if over limit using smart eviction
        while (episodicMemories.size() > MAX_EPISODIC_MEMORIES) {
            evictLowestScoringMemory();
        }

        // High emotional weight events also go to emotional memory
        if (Math.abs(emotionalWeight) >= 5) {
            recordEmotionalMemory(eventType, description, emotionalWeight);
        }

        // Update rapport based on positive experiences
        if (emotionalWeight > 3) {
            adjustRapport(1);
        }

        interactionCount.incrementAndGet();
        LOGGER.debug("Recorded experience: {} (weight={})", eventType, emotionalWeight);
    }

    /**
     * Records a fact learned about the player.
     *
     * @param category Category (preference, skill, habit, etc.)
     * @param key Fact key
     * @param value Fact value
     */
    public void learnPlayerFact(String category, String key, Object value) {
        String compositeKey = category + ":" + key;
        semanticMemories.put(compositeKey, new SemanticMemory(
            category, key, value, Instant.now()
        ));

        if ("preference".equals(category)) {
            playerPreferences.put(key, value);
        }

        LOGGER.debug("Learned player fact: {} = {}", compositeKey, value);
    }

    /**
     * Records an emotionally significant moment.
     * Thread-safe: uses synchronized block for sort and trim operations.
     */
    private void recordEmotionalMemory(String eventType, String description, int emotionalWeight) {
        EmotionalMemory memory = new EmotionalMemory(
            eventType, description, emotionalWeight, Instant.now()
        );

        // CopyOnWriteArrayList handles add() safely
        emotionalMemories.add(memory);

        // Sort and trim requires synchronization
        synchronized (this) {
            // Keep emotional memories sorted by significance
            emotionalMemories.sort((a, b) -> Integer.compare(
                Math.abs(b.emotionalWeight), Math.abs(a.emotionalWeight)
            ));

            // Cap at 50 emotional memories
            if (emotionalMemories.size() > 50) {
                // CopyOnWriteArrayList doesn't support remove by index efficiently
                // Create new sorted list and clear/re-add
                List<EmotionalMemory> sorted = new ArrayList<>(emotionalMemories);
                sorted.sort((a, b) -> Integer.compare(
                    Math.abs(b.emotionalWeight), Math.abs(a.emotionalWeight)
                ));
                emotionalMemories.clear();
                emotionalMemories.addAll(sorted.subList(0, Math.min(50, sorted.size())));
            }
        }

        LOGGER.info("Recorded emotional memory: {} (weight={})", eventType, emotionalWeight);
    }

    /**
     * Records an inside joke or memorable quote.
     *
     * @param context The situation that created the joke
     * @param punchline The memorable phrase
     */
    public void recordInsideJoke(String context, String punchline) {
        conversationalMemory.addInsideJoke(new InsideJoke(
            context, punchline, Instant.now()
        ));

        // Inside jokes significantly increase rapport
        adjustRapport(3);

        LOGGER.info("New inside joke recorded: {}", punchline);
    }

    /**
     * Adds to working memory for current context.
     *
     * @param type Entry type
     * @param content Entry content
     */
    public void addToWorkingMemory(String type, String content) {
        workingMemory.addFirst(new WorkingMemoryEntry(type, content, Instant.now()));

        while (workingMemory.size() > MAX_WORKING_MEMORY) {
            workingMemory.removeLast();
        }
    }

    /**
     * Records player's playstyle observation.
     *
     * @param metricName Metric to track
     * @param delta Change amount
     */
    public void recordPlaystyleMetric(String metricName, int delta) {
        playstyleMetrics.merge(metricName, delta, Integer::sum);
    }

    // === Memory Retrieval ===

    /**
     * Retrieves recent episodic memories.
     *
     * @param count Maximum number to retrieve
     * @return List of recent memories
     */
    public List<EpisodicMemory> getRecentMemories(int count) {
        return episodicMemories.stream()
            .limit(count)
            .collect(Collectors.toList());
    }

    /**
     * Computes a dynamic memory score based on time decay, importance, and access frequency.
     * This combines multiple signals to determine which memories are most valuable.
     *
     * @param memory The memory to score
     * @return A score from 0.0 (least important) to 1.0 (most important)
     */
    public float computeMemoryScore(EpisodicMemory memory) {
        Instant now = Instant.now();

        // Time decay with 7-day half-life
        long daysSinceCreation = ChronoUnit.DAYS.between(memory.timestamp, now);
        float ageScore = (float) Math.exp(-daysSinceCreation / 7.0);

        // Emotional importance (normalize -10 to +10 range to 0-1)
        float importanceScore = Math.min(1.0f, Math.abs(memory.emotionalWeight) / 10.0f);

        // Access frequency (cap at 10 accesses for max score)
        float accessScore = Math.min(1.0f, memory.getAccessCount() / 10.0f);

        // Recent access bonus (if accessed in last 24 hours)
        long hoursSinceAccess = ChronoUnit.HOURS.between(memory.getLastAccessed(), now);
        float recentAccessBonus = hoursSinceAccess < 24 ? 0.2f : 0.0f;

        // Combined weighted score
        return ageScore * 0.4f + importanceScore * 0.4f + accessScore * 0.2f + recentAccessBonus;
    }

    /**
     * Evicts the lowest-scoring memory that is not protected.
     * Implements smart eviction based on memory importance rather than FIFO.
     */
    private void evictLowestScoringMemory() {
        EpisodicMemory lowestScoring = null;
        float lowestScore = Float.MAX_VALUE;

        for (EpisodicMemory memory : episodicMemories) {
            // Skip protected memories
            if (memory.isProtected()) {
                continue;
            }

            float score = computeMemoryScore(memory);
            if (score < lowestScore) {
                lowestScore = score;
                lowestScoring = memory;
            }
        }

        // If we found a candidate to evict, remove it
        if (lowestScoring != null) {
            episodicMemories.remove(lowestScoring);
            Integer vectorId = memoryToVectorId.remove(lowestScoring);
            if (vectorId != null) {
                memoryVectorStore.remove(vectorId);
            }
            LOGGER.debug("Evicted low-scoring memory: {} (score={})",
                lowestScoring.eventType, lowestScore);
        } else {
            // All memories are protected, force remove oldest non-milestone
            EpisodicMemory oldest = null;
            Instant oldestTime = Instant.now();
            for (EpisodicMemory memory : episodicMemories) {
                if (!memory.isMilestone && memory.timestamp.isBefore(oldestTime)) {
                    oldestTime = memory.timestamp;
                    oldest = memory;
                }
            }
            if (oldest != null) {
                episodicMemories.remove(oldest);
                Integer vectorId = memoryToVectorId.remove(oldest);
                if (vectorId != null) {
                    memoryVectorStore.remove(vectorId);
                }
                LOGGER.debug("Force evicted oldest non-milestone memory: {}", oldest.eventType);
            }
        }
    }

    /**
     * Retrieves memories similar to the given context using semantic search.
     * Uses vector embeddings to find conceptually similar memories.
     *
     * @param query Query to find relevant memories for
     * @param k Maximum number of results to return
     * @return List of relevant memories, sorted by similarity
     */
    public List<EpisodicMemory> findRelevantMemories(String query, int k) {
        if (memoryVectorStore.size() == 0) {
            LOGGER.debug("No memories in vector store for semantic search");
            return Collections.emptyList();
        }

        try {
            // Generate embedding for query
            float[] queryEmbedding = embeddingModel.embed(query);

            // Search vector store
            List<VectorSearchResult<EpisodicMemory>> results =
                    memoryVectorStore.search(queryEmbedding, k);

            // Extract memories from results and record access
            List<EpisodicMemory> memories = results.stream()
                    .map(VectorSearchResult::getData)
                    .peek(EpisodicMemory::recordAccess)
                    .collect(Collectors.toList());

            LOGGER.debug("Semantic search for '{}' returned {} memories", query, memories.size());
            return memories;

        } catch (Exception e) {
            LOGGER.error("Error in semantic search, falling back to keyword matching", e);
            return getRelevantMemoriesByKeywords(query, k);
        }
    }

    /**
     * Retrieves memories similar to the given context using keyword matching.
     * Fallback method when vector search is unavailable.
     *
     * @param context Context to match against
     * @param count Maximum results
     * @return Relevant memories
     */
    public List<EpisodicMemory> getRelevantMemories(String context, int count) {
        return findRelevantMemories(context, count);
    }

    /**
     * Keyword-based memory retrieval (fallback method).
     */
    private List<EpisodicMemory> getRelevantMemoriesByKeywords(String context, int count) {
        String lowerContext = context.toLowerCase();

        return episodicMemories.stream()
            .filter(m -> m.description.toLowerCase().contains(lowerContext) ||
                        m.eventType.toLowerCase().contains(lowerContext))
            .limit(count)
            .collect(Collectors.toList());
    }

    /**
     * Adds a memory to the vector store for semantic search.
     *
     * @param memory The memory to add
     */
    private void addMemoryToVectorStore(EpisodicMemory memory) {
        try {
            // Create text representation for embedding
            String textForEmbedding = memory.eventType + ": " + memory.description;

            // Generate embedding
            float[] embedding = embeddingModel.embed(textForEmbedding);

            // Add to vector store
            int vectorId = memoryVectorStore.add(embedding, memory);

            // Store mapping
            memoryToVectorId.put(memory, vectorId);

            LOGGER.debug("Added memory to vector store with ID {}", vectorId);

        } catch (Exception e) {
            LOGGER.error("Failed to add memory to vector store", e);
        }
    }

    /**
     * Gets a player preference.
     *
     * @param key Preference key
     * @return Preference value, or null if unknown
     */
    @SuppressWarnings("unchecked")
    public <T> T getPlayerPreference(String key, T defaultValue) {
        Object value = playerPreferences.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Gets a random inside joke for reference.
     *
     * @return Random inside joke, or null if none exist
     */
    public InsideJoke getRandomInsideJoke() {
        return conversationalMemory.getRandomJoke();
    }

    /**
     * Gets the most emotionally significant shared memory.
     *
     * @return Most significant memory, or null if none
     */
    public EmotionalMemory getMostSignificantMemory() {
        return emotionalMemories.isEmpty() ? null : emotionalMemories.get(0);
    }

    /**
     * Gets working memory as context string.
     *
     * @return Formatted working memory
     */
    public String getWorkingMemoryContext() {
        if (workingMemory.isEmpty()) {
            return "No recent context.";
        }

        StringBuilder sb = new StringBuilder("Recent context:\n");
        for (WorkingMemoryEntry entry : workingMemory) {
            sb.append("- ").append(entry.type).append(": ").append(entry.content).append("\n");
        }
        return sb.toString();
    }

    /**
     * Builds a relationship summary for prompting.
     *
     * @return Relationship context string
     */
    public String getRelationshipContext() {
        StringBuilder sb = new StringBuilder();

        sb.append("Relationship Status:\n");
        sb.append("- Rapport Level: ").append(getRapportLevel()).append("/100\n");
        sb.append("- Trust Level: ").append(getTrustLevel()).append("/100\n");
        sb.append("- Interactions: ").append(interactionCount.get()).append("\n");

        if (firstMeeting != null) {
            long days = ChronoUnit.DAYS.between(firstMeeting, Instant.now());
            sb.append("- Known for: ").append(days).append(" days\n");
        }

        if (!playerPreferences.isEmpty()) {
            sb.append("- Known preferences: ").append(playerPreferences.keySet()).append("\n");
        }

        if (!playstyleMetrics.isEmpty()) {
            sb.append("- Playstyle observations: ").append(playstyleMetrics).append("\n");
        }

        int jokeCount = conversationalMemory.getJokeCount();
        if (jokeCount > 0) {
            sb.append("- Inside jokes shared: ").append(jokeCount).append("\n");
        }

        return sb.toString();
    }

    /**
     * Builds an optimized context string for LLM prompting with memory prioritization.
     * Uses dynamic scoring to include the most relevant memories within token limits.
     *
     * @param query The current query/task to provide context for
     * @param maxTokens Maximum tokens to use for context (approximately)
     * @return Optimized context string
     */
    public String buildOptimizedContext(String query, int maxTokens) {
        List<ScoredMemory> scored = new ArrayList<>();

        // 1. Always include first meeting memory if exists (high priority)
        episodicMemories.stream()
            .filter(m -> "first_meeting".equals(m.eventType))
            .findFirst()
            .ifPresent(m -> {
                m.recordAccess();
                scored.add(new ScoredMemory(m, 1000.0f));
            });

        // 2. Recent working memory (high priority)
        workingMemory.stream()
            .limit(5)
            .forEach(entry -> {
                // Convert working memory to a synthetic episodic memory for scoring
                EpisodicMemory synthetic = new EpisodicMemory(
                    entry.type,
                    entry.content,
                    3, // Moderate weight
                    entry.timestamp
                );
                scored.add(new ScoredMemory(synthetic, 500.0f));
            });

        // 3. High-emotional and protected memories
        episodicMemories.stream()
            .filter(m -> Math.abs(m.emotionalWeight) >= 7 || m.isProtected())
            .forEach(m -> {
                m.recordAccess();
                float score = computeMemoryScore(m) * 2.0f; // Boost high-emotion memories
                scored.add(new ScoredMemory(m, score));
            });

        // 4. Semantically relevant memories (from vector search)
        List<EpisodicMemory> relevant = findRelevantMemories(query, 10);
        relevant.forEach(m -> {
            m.recordAccess();
            float score = computeMemoryScore(m);
            scored.add(new ScoredMemory(m, score));
        });

        // Sort by combined score
        scored.sort((a, b) -> Float.compare(b.score, a.score));

        // Build context, respecting token limit
        StringBuilder context = new StringBuilder();
        int tokens = 0;

        // Add relationship context first
        String relationshipCtx = getRelationshipContext();
        int relationshipTokens = relationshipCtx.length() / 4;
        context.append(relationshipCtx).append("\n\n");
        tokens += relationshipTokens;

        // Add scored memories
        for (ScoredMemory sm : scored) {
            String text = sm.memory.toContextString();
            int estimatedTokens = text.length() / 4;

            if (tokens + estimatedTokens > maxTokens) {
                break;
            }

            context.append(text).append("\n");
            tokens += estimatedTokens;
        }

        LOGGER.debug("Built optimized context: {} memories, ~{} tokens",
            scored.size(), tokens);

        return context.toString();
    }

    /**
     * Helper class for tracking scored memories.
     */
    private static class ScoredMemory {
        final EpisodicMemory memory;
        final float score;

        ScoredMemory(EpisodicMemory memory, float score) {
            this.memory = memory;
            this.score = score;
        }
    }

    // === Memory Consolidation Support ===

    /**
     * Gets memories that are eligible for consolidation.
     * Excludes protected memories and recent memories.
     *
     * @param minAgeDays Minimum age in days for a memory to be consolidatable
     * @return List of consolidatable memories
     */
    public List<EpisodicMemory> getConsolidatableMemories(int minAgeDays) {
        Instant cutoff = Instant.now().minus(minAgeDays, ChronoUnit.DAYS);

        return episodicMemories.stream()
            .filter(m -> !m.isProtected()) // Exclude protected memories
            .filter(m -> m.timestamp.isBefore(cutoff)) // Only old memories
            .collect(Collectors.toList());
    }

    /**
     * Removes a list of memories from storage.
     * Used by consolidation service after summarization.
     *
     * @param memoriesToRemove Memories to remove
     * @return Number of memories removed
     */
    public int removeMemories(List<EpisodicMemory> memoriesToRemove) {
        int removed = 0;

        for (EpisodicMemory memory : memoriesToRemove) {
            if (episodicMemories.remove(memory)) {
                // Remove from vector store
                Integer vectorId = memoryToVectorId.remove(memory);
                if (vectorId != null) {
                    memoryVectorStore.remove(vectorId);
                }
                removed++;
            }
        }

        LOGGER.info("Removed {} consolidated memories", removed);
        return removed;
    }

    /**
     * Validates memory state and logs any inconsistencies.
     * Useful for debugging and ensuring data integrity.
     *
     * @return true if memory state is valid
     */
    public boolean validateMemoryState() {
        boolean valid = true;

        // Check vector store mapping consistency
        int vectorStoreSize = memoryVectorStore.size();
        int mappingSize = memoryToVectorId.size();

        if (vectorStoreSize != mappingSize) {
            LOGGER.warn("Memory state inconsistency: vector store has {} entries, " +
                "but mapping has {} entries", vectorStoreSize, mappingSize);
            valid = false;
        }

        // Check for protected memories in working memory
        long protectedInDeque = episodicMemories.stream()
            .filter(EpisodicMemory::isProtected)
            .count();

        if (protectedInDeque > 0) {
            LOGGER.debug("Found {} protected memories in episodic store", protectedInDeque);
        }

        return valid;
    }

    /**
     * Gets the personality profile for prompting.
     */
    public PersonalityProfile getPersonality() {
        return personality;
    }

    /**
     * Gets the relationship state for external access.
     *
     * @return Relationship object containing rapport, trust, and mood
     */
    public Relationship getRelationship() {
        Mood mood = parseMood(personality.mood);
        return new Relationship(rapportLevel.get(), trustLevel.get(), mood);
    }

    /**
     * Parses a mood string into a Mood enum value.
     */
    private Mood parseMood(String moodString) {
        if (moodString == null) {
            return Mood.CHEERFUL;
        }
        try {
            return Mood.valueOf(moodString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mood.CHEERFUL;
        }
    }

    // === Relationship Management ===

    /**
     * Initializes the relationship on first meeting.
     *
     * @param playerName The player's name
     */
    public void initializeRelationship(String playerName) {
        if (this.playerName == null) {
            this.playerName = playerName;
            this.firstMeeting = Instant.now();
            this.sessionStart = Instant.now();

            learnPlayerFact("identity", "name", playerName);

            // Create and mark as milestone
            EpisodicMemory firstMeetingMemory = new EpisodicMemory(
                "first_meeting",
                "First time meeting " + playerName,
                7,
                Instant.now()
            );
            firstMeetingMemory.setMilestone(true);
            episodicMemories.addFirst(firstMeetingMemory);
            addMemoryToVectorStore(firstMeetingMemory);

            LOGGER.info("Relationship initialized with {}", playerName);
        }
    }

    /**
     * Adjusts rapport level.
     *
     * @param delta Amount to change (can be negative)
     */
    public void adjustRapport(int delta) {
        int newValue = Math.max(0, Math.min(100, rapportLevel.get() + delta));
        rapportLevel.set(newValue);
    }

    /**
     * Adjusts trust level.
     *
     * @param delta Amount to change (can be negative)
     */
    public void adjustTrust(int delta) {
        int newValue = Math.max(0, Math.min(100, trustLevel.get() + delta));
        trustLevel.set(newValue);
    }

    /**
     * Called when a shared task succeeds.
     */
    public void recordSharedSuccess(String taskDescription) {
        recordExperience("success", taskDescription, 5);
        adjustRapport(2);
        adjustTrust(3);
        checkAutoMilestones();
    }

    /**
     * Called when a shared task fails.
     */
    public void recordSharedFailure(String taskDescription, String reason) {
        recordExperience("failure", taskDescription + " - " + reason, -3);
        // Don't reduce rapport for failures - we're in this together
    }

    /**
     * Automatically detects and records relationship milestones based on current state.
     * Called after significant events to check for milestone conditions.
     */
    public void checkAutoMilestones() {
        int interactions = interactionCount.get();
        int rapport = rapportLevel.get();

        // Check for interaction-based milestones
        if (interactions == 10 && !hasMilestone("auto_getting_to_know")) {
            milestoneTracker.recordMilestone(
                new MilestoneTracker.Milestone(
                    "auto_getting_to_know",
                    MilestoneTracker.MilestoneType.COUNT,
                    "Getting to Know You",
                    "We've had 10 interactions now! I feel like we're starting to understand each other.",
                    5,
                    Instant.now()
                )
            );
            adjustRapport(2);
        }

        if (interactions == 50 && !hasMilestone("auto_frequent_companion")) {
            milestoneTracker.recordMilestone(
                new MilestoneTracker.Milestone(
                    "auto_frequent_companion",
                    MilestoneTracker.MilestoneType.COUNT,
                    "Frequent Companions",
                    "50 interactions together! You've become a regular part of my routine.",
                    7,
                    Instant.now()
                )
            );
            adjustRapport(3);
        }

        // Check for rapport-based milestones
        if (rapport >= 50 && !hasMilestone("auto_friends")) {
            milestoneTracker.recordMilestone(
                new MilestoneTracker.Milestone(
                    "auto_friends",
                    MilestoneTracker.MilestoneType.ACHIEVEMENT,
                    "Friends",
                    "I feel like we've really become friends. I trust you and enjoy our time together.",
                    8,
                    Instant.now()
                )
            );
            adjustRapport(5);
        }

        if (rapport >= 80 && !hasMilestone("auto_best_friends")) {
            milestoneTracker.recordMilestone(
                new MilestoneTracker.Milestone(
                    "auto_best_friends",
                    MilestoneTracker.MilestoneType.ACHIEVEMENT,
                    "Best Friends",
                    "You're not just a companion anymore - you're my best friend! We've been through so much together.",
                    10,
                    Instant.now()
                )
            );
            adjustRapport(5);
        }

        // Check for time-based milestones
        if (firstMeeting != null) {
            long days = ChronoUnit.DAYS.between(firstMeeting, Instant.now());
            if (days >= 7 && !hasMilestone("auto_week_together")) {
                milestoneTracker.recordMilestone(
                    new MilestoneTracker.Milestone(
                        "auto_week_together",
                        MilestoneTracker.MilestoneType.ANNIVERSARY,
                        "One Week Together",
                        "It's been a whole week since we met! Here's to many more adventures.",
                        6,
                        Instant.now()
                    )
                );
                adjustRapport(3);
            }
        }
    }

    /**
     * Internal method to record a milestone directly.
     * Used by auto-detection system.
     */
    private void recordMilestone(MilestoneTracker.Milestone milestone) {
        milestoneTracker.recordMilestone(milestone);
        // Also record as episodic memory for the milestone
        recordExperience("milestone", milestone.title + ": " + milestone.description, milestone.importance);
    }

    // === Getters ===

    public int getRapportLevel() {
        return rapportLevel.get();
    }

    public int getTrustLevel() {
        return trustLevel.get();
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getInteractionCount() {
        return interactionCount.get();
    }

    public Instant getFirstMeeting() {
        return firstMeeting;
    }

    public Set<String> getSessionTopics() {
        return Collections.unmodifiableSet(sessionTopics);
    }

    /**
     * Gets the conversational memory for direct access.
     * Package-private for internal use by ConversationManager.
     */
    ConversationalMemory getConversationalMemory() {
        return conversationalMemory;
    }

    /**
     * Increments the interaction count.
     * Package-private for internal use by ConversationManager.
     */
    void incrementInteractionCount() {
        interactionCount.incrementAndGet();
    }

    /**
     * Gets the number of inside jokes shared with the player.
     *
     * @return Number of inside jokes
     */
    public int getInsideJokeCount() {
        return conversationalMemory.getJokeCount();
    }

    // === Milestone Tracking ===

    /**
     * Gets the milestone tracker for this companion.
     *
     * @return The MilestoneTracker instance
     */
    public MilestoneTracker getMilestoneTracker() {
        return milestoneTracker;
    }

    /**
     * Gets all milestones achieved with this companion.
     *
     * @return List of all achieved milestones
     */
    public List<MilestoneTracker.Milestone> getMilestones() {
        return milestoneTracker.getMilestones();
    }

    /**
     * Checks if a specific milestone has been achieved.
     *
     * @param milestoneId The milestone ID to check
     * @return true if the milestone has been achieved
     */
    public boolean hasMilestone(String milestoneId) {
        return milestoneTracker.hasMilestone(milestoneId);
    }

    // === NBT Persistence ===

    /**
     * Saves companion memory data to NBT format for world save.
     *
     * @param tag The CompoundTag to save data to
     */
    public void saveToNBT(CompoundTag tag) {
        // Save relationship data
        tag.putInt("RapportLevel", rapportLevel.get());
        tag.putInt("TrustLevel", trustLevel.get());
        tag.putInt("InteractionCount", interactionCount.get());

        if (firstMeeting != null) {
            tag.putLong("FirstMeeting", firstMeeting.toEpochMilli());
        }

        if (playerName != null) {
            tag.putString("PlayerName", playerName);
        }

        // Save episodic memories
        ListTag episodicList = new ListTag();
        for (EpisodicMemory memory : episodicMemories) {
            CompoundTag memoryTag = new CompoundTag();
            memoryTag.putString("EventType", memory.eventType);
            memoryTag.putString("Description", memory.description);
            memoryTag.putInt("EmotionalWeight", memory.emotionalWeight);
            memoryTag.putLong("Timestamp", memory.timestamp.toEpochMilli());
            memoryTag.putInt("AccessCount", memory.getAccessCount());
            memoryTag.putLong("LastAccessed", memory.getLastAccessed().toEpochMilli());
            memoryTag.putBoolean("IsMilestone", memory.isMilestone);
            episodicList.add(memoryTag);
        }
        tag.put("EpisodicMemories", episodicList);

        // Save semantic memories
        ListTag semanticList = new ListTag();
        for (Map.Entry<String, SemanticMemory> entry : semanticMemories.entrySet()) {
            CompoundTag semanticTag = new CompoundTag();
            semanticTag.putString("Key", entry.getKey());
            semanticTag.putString("Category", entry.getValue().category);
            semanticTag.putString("FactKey", entry.getValue().key);

            Object value = entry.getValue().value;
            if (value instanceof String) {
                semanticTag.putString("Value", (String) value);
                semanticTag.putString("ValueType", "string");
            } else if (value instanceof Integer) {
                semanticTag.putInt("Value", (Integer) value);
                semanticTag.putString("ValueType", "int");
            } else if (value instanceof Boolean) {
                semanticTag.putBoolean("Value", (Boolean) value);
                semanticTag.putString("ValueType", "boolean");
            } else {
                semanticTag.putString("Value", value.toString());
                semanticTag.putString("ValueType", "string");
            }

            semanticTag.putLong("LearnedAt", entry.getValue().learnedAt.toEpochMilli());
            semanticTag.putInt("Confidence", entry.getValue().confidence);
            semanticList.add(semanticTag);
        }
        tag.put("SemanticMemories", semanticList);

        // Save emotional memories
        ListTag emotionalList = new ListTag();
        for (EmotionalMemory memory : emotionalMemories) {
            CompoundTag emotionTag = new CompoundTag();
            emotionTag.putString("EventType", memory.eventType);
            emotionTag.putString("Description", memory.description);
            emotionTag.putInt("EmotionalWeight", memory.emotionalWeight);
            emotionTag.putLong("Timestamp", memory.timestamp.toEpochMilli());
            emotionalList.add(emotionTag);
        }
        tag.put("EmotionalMemories", emotionalList);

        // Save inside jokes
        ListTag jokesList = new ListTag();
        for (InsideJoke joke : conversationalMemory.insideJokes) {
            CompoundTag jokeTag = new CompoundTag();
            jokeTag.putString("Context", joke.context);
            jokeTag.putString("Punchline", joke.punchline);
            jokeTag.putLong("CreatedAt", joke.createdAt.toEpochMilli());
            jokeTag.putInt("ReferenceCount", joke.referenceCount);
            jokesList.add(jokeTag);
        }
        tag.put("InsideJokes", jokesList);

        // Save discussed topics
        ListTag topicsList = new ListTag();
        for (String topic : conversationalMemory.discussedTopics) {
            topicsList.add(StringTag.valueOf(topic));
        }
        tag.put("DiscussedTopics", topicsList);

        // Save phrase usage
        CompoundTag phraseUsageTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : conversationalMemory.phraseUsage.entrySet()) {
            phraseUsageTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("PhraseUsage", phraseUsageTag);

        // Save player preferences
        CompoundTag preferencesTag = new CompoundTag();
        for (Map.Entry<String, Object> entry : playerPreferences.entrySet()) {
            saveValueToNBT(preferencesTag, entry.getKey(), entry.getValue());
        }
        tag.put("PlayerPreferences", preferencesTag);

        // Save playstyle metrics
        CompoundTag playstyleTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : playstyleMetrics.entrySet()) {
            playstyleTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("PlaystyleMetrics", playstyleTag);

        // Save personality
        CompoundTag personalityTag = new CompoundTag();
        personalityTag.putInt("Openness", personality.openness);
        personalityTag.putInt("Conscientiousness", personality.conscientiousness);
        personalityTag.putInt("Extraversion", personality.extraversion);
        personalityTag.putInt("Agreeableness", personality.agreeableness);
        personalityTag.putInt("Neuroticism", personality.neuroticism);
        personalityTag.putInt("Humor", personality.humor);
        personalityTag.putInt("Encouragement", personality.encouragement);
        personalityTag.putInt("Formality", personality.formality);
        personalityTag.putString("FavoriteBlock", personality.favoriteBlock);
        personalityTag.putString("WorkStyle", personality.workStyle);
        personalityTag.putString("Mood", personality.mood);
        personalityTag.putString("ArchetypeName", personality.archetypeName);

        // Save catchphrases
        ListTag catchphrasesList = new ListTag();
        for (String catchphrase : personality.catchphrases) {
            catchphrasesList.add(StringTag.valueOf(catchphrase));
        }
        personalityTag.put("Catchphrases", catchphrasesList);

        // Save verbal tics
        ListTag verbalTicsList = new ListTag();
        for (String tic : personality.verbalTics) {
            verbalTicsList.add(StringTag.valueOf(tic));
        }
        personalityTag.put("VerbalTics", verbalTicsList);

        // Save tic usage counts
        CompoundTag ticUsageTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : personality.ticUsageCount.entrySet()) {
            ticUsageTag.putInt(entry.getKey(), entry.getValue());
        }
        personalityTag.put("TicUsageCount", ticUsageTag);

        tag.put("Personality", personalityTag);

        // Save milestone tracker
        CompoundTag milestoneTag = new CompoundTag();
        milestoneTracker.saveToNBT(milestoneTag);
        tag.put("MilestoneTracker", milestoneTag);

        LOGGER.debug("CompanionMemory saved to NBT ({} episodic, {} semantic memories, {} milestones)",
            episodicMemories.size(), semanticMemories.size(), milestoneTracker.getMilestones().size());
    }

    /**
     * Loads companion memory data from NBT format.
     *
     * @param tag The CompoundTag to load data from
     */
    public void loadFromNBT(CompoundTag tag) {
        // Load relationship data
        rapportLevel.set(tag.getInt("RapportLevel"));
        trustLevel.set(tag.getInt("TrustLevel"));
        interactionCount.set(tag.getInt("InteractionCount"));

        long firstMeetingEpoch = tag.getLong("FirstMeeting");
        if (firstMeetingEpoch != 0) {
            firstMeeting = Instant.ofEpochMilli(firstMeetingEpoch);
        }

        playerName = tag.contains("PlayerName") ? tag.getString("PlayerName") : null;

        // Load episodic memories
        ListTag episodicList = tag.getList("EpisodicMemories", 10);
        if (!episodicList.isEmpty()) {
            episodicMemories.clear();
            for (int i = 0; i < episodicList.size(); i++) {
                CompoundTag memoryTag = episodicList.getCompound(i);
                EpisodicMemory memory = new EpisodicMemory(
                    memoryTag.getString("EventType"),
                    memoryTag.getString("Description"),
                    memoryTag.getInt("EmotionalWeight"),
                    Instant.ofEpochMilli(memoryTag.getLong("Timestamp"))
                );

                // Load access tracking fields
                if (memoryTag.contains("AccessCount")) {
                    for (int j = 0; j < memoryTag.getInt("AccessCount"); j++) {
                        memory.recordAccess();
                    }
                }
                if (memoryTag.contains("LastAccessed")) {
                    // This is internal, we'll set it through recordAccess above
                }
                if (memoryTag.contains("IsMilestone")) {
                    memory.setMilestone(memoryTag.getBoolean("IsMilestone"));
                }

                episodicMemories.add(memory);

                // Rebuild vector store mapping for semantic search
                addMemoryToVectorStore(memory);
            }
        }

        // Load semantic memories
        ListTag semanticList = tag.getList("SemanticMemories", 10);
        if (!semanticList.isEmpty()) {
            semanticMemories.clear();
            for (int i = 0; i < semanticList.size(); i++) {
                CompoundTag semanticTag = semanticList.getCompound(i);
                String key = semanticTag.getString("Key");
                String category = semanticTag.getString("Category");
                String factKey = semanticTag.getString("FactKey");
                String valueType = semanticTag.getString("ValueType");

                Object value = switch (valueType) {
                    case "int" -> semanticTag.getInt("Value");
                    case "boolean" -> semanticTag.getBoolean("Value");
                    default -> semanticTag.getString("Value");
                };

                SemanticMemory memory = new SemanticMemory(
                    category, factKey, value,
                    Instant.ofEpochMilli(semanticTag.getLong("LearnedAt"))
                );
                memory.confidence = semanticTag.getInt("Confidence");
                semanticMemories.put(key, memory);
            }
        }

        // Load emotional memories
        ListTag emotionalList = tag.getList("EmotionalMemories", 10);
        if (!emotionalList.isEmpty()) {
            emotionalMemories.clear();
            for (int i = 0; i < emotionalList.size(); i++) {
                CompoundTag emotionTag = emotionalList.getCompound(i);
                EmotionalMemory memory = new EmotionalMemory(
                    emotionTag.getString("EventType"),
                    emotionTag.getString("Description"),
                    emotionTag.getInt("EmotionalWeight"),
                    Instant.ofEpochMilli(emotionTag.getLong("Timestamp"))
                );
                emotionalMemories.add(memory);
            }
        }

        // Load inside jokes
        ListTag jokesList = tag.getList("InsideJokes", 10);
        if (!jokesList.isEmpty()) {
            conversationalMemory.clearInsideJokes();
            for (int i = 0; i < jokesList.size(); i++) {
                CompoundTag jokeTag = jokesList.getCompound(i);
                InsideJoke joke = new InsideJoke(
                    jokeTag.getString("Context"),
                    jokeTag.getString("Punchline"),
                    Instant.ofEpochMilli(jokeTag.getLong("CreatedAt"))
                );
                joke.referenceCount = jokeTag.getInt("ReferenceCount");
                conversationalMemory.addInsideJokeDirect(joke);
            }
        }

        // Load discussed topics
        ListTag topicsList = tag.getList("DiscussedTopics", 8);
        if (!topicsList.isEmpty()) {
            conversationalMemory.clearDiscussedTopics();
            for (int i = 0; i < topicsList.size(); i++) {
                conversationalMemory.addDiscussedTopicDirect(topicsList.getString(i));
            }
        }

        // Load phrase usage
        CompoundTag phraseUsageTag = tag.getCompound("PhraseUsage");
        if (!phraseUsageTag.isEmpty()) {
            conversationalMemory.clearPhraseUsage();
            for (String key : phraseUsageTag.getAllKeys()) {
                conversationalMemory.addPhraseUsage(key, phraseUsageTag.getInt(key));
            }
        }

        // Load player preferences
        CompoundTag preferencesTag = tag.getCompound("PlayerPreferences");
        if (!preferencesTag.isEmpty()) {
            playerPreferences.clear();
            for (String key : preferencesTag.getAllKeys()) {
                Object value = preferencesTag.contains(key, 99)
                    ? preferencesTag.getInt(key)
                    : preferencesTag.getString(key);
                playerPreferences.put(key, value);
            }
        }

        // Load playstyle metrics
        CompoundTag playstyleTag = tag.getCompound("PlaystyleMetrics");
        if (!playstyleTag.isEmpty()) {
            playstyleMetrics.clear();
            for (String key : playstyleTag.getAllKeys()) {
                playstyleMetrics.put(key, playstyleTag.getInt(key));
            }
        }

        // Load personality
        CompoundTag personalityTag = tag.getCompound("Personality");
        if (!personalityTag.isEmpty()) {
            personality.openness = personalityTag.getInt("Openness");
            personality.conscientiousness = personalityTag.getInt("Conscientiousness");
            personality.extraversion = personalityTag.getInt("Extraversion");
            personality.agreeableness = personalityTag.getInt("Agreeableness");
            personality.neuroticism = personalityTag.getInt("Neuroticism");
            personality.humor = personalityTag.getInt("Humor");
            personality.encouragement = personalityTag.getInt("Encouragement");
            personality.formality = personalityTag.getInt("Formality");

            personality.favoriteBlock = personalityTag.getString("FavoriteBlock");
            personality.workStyle = personalityTag.getString("WorkStyle");
            personality.mood = personalityTag.getString("Mood");
            personality.archetypeName = personalityTag.contains("ArchetypeName")
                ? personalityTag.getString("ArchetypeName")
                : "THE_FOREMAN";

            // Load catchphrases
            ListTag catchphrasesList = personalityTag.getList("Catchphrases", 8);
            if (!catchphrasesList.isEmpty()) {
                personality.catchphrases.clear();
                for (int i = 0; i < catchphrasesList.size(); i++) {
                    personality.catchphrases.add(catchphrasesList.getString(i));
                }
            }

            // Load verbal tics
            ListTag verbalTicsList = personalityTag.getList("VerbalTics", 8);
            if (!verbalTicsList.isEmpty()) {
                personality.verbalTics.clear();
                for (int i = 0; i < verbalTicsList.size(); i++) {
                    personality.verbalTics.add(verbalTicsList.getString(i));
                }
            }

            // Load tic usage counts
            CompoundTag ticUsageTag = personalityTag.getCompound("TicUsageCount");
            if (!ticUsageTag.isEmpty()) {
                personality.ticUsageCount.clear();
                for (String key : ticUsageTag.getAllKeys()) {
                    personality.ticUsageCount.put(key, ticUsageTag.getInt(key));
                }
            }
        }

        LOGGER.info("CompanionMemory loaded from NBT ({} episodic, {} semantic memories)",
            episodicMemories.size(), semanticMemories.size());
    }

    // === NBT Helper Methods ===

    /**
     * Saves an Object value to NBT with type tag.
     */
    private void saveValueToNBT(CompoundTag tag, String key, Object value) {
        if (value instanceof String) {
            tag.putString(key, (String) value);
            tag.putString(key + "Type", "string");
        } else if (value instanceof Integer) {
            tag.putInt(key, (Integer) value);
            tag.putString(key + "Type", "int");
        } else if (value instanceof Boolean) {
            tag.putBoolean(key, (Boolean) value);
            tag.putString(key + "Type", "boolean");
        } else {
            tag.putString(key, value.toString());
            tag.putString(key + "Type", "string");
        }
    }

    /**
     * Loads an Object value from NBT based on type tag.
     */
    private Object loadValueFromNBT(CompoundTag tag, String key) {
        String valueType = tag.getString(key + "Type");
        return switch (valueType) {
            case "int" -> tag.getInt(key);
            case "boolean" -> tag.getBoolean(key);
            default -> tag.getString(key);
        };
    }

    // === Inner Classes ===

    /**
     * An episodic memory of a specific event.
     */
    public static class EpisodicMemory {
        public final String eventType;
        public final String description;
        public final int emotionalWeight;
        public final Instant timestamp;

        // Memory access tracking for importance evolution
        private int accessCount = 0;
        private Instant lastAccessed;
        private boolean isMilestone = false;

        public EpisodicMemory(String eventType, String description, int emotionalWeight, Instant timestamp) {
            this.eventType = eventType;
            this.description = description;
            this.emotionalWeight = emotionalWeight;
            this.timestamp = timestamp;
            this.lastAccessed = timestamp;
        }

        /**
         * Records an access to this memory, increasing its importance.
         */
        public void recordAccess() {
            this.accessCount++;
            this.lastAccessed = Instant.now();
        }

        /**
         * Marks this memory as a milestone that should never be evicted.
         */
        public void setMilestone(boolean milestone) {
            this.isMilestone = milestone;
        }

        /**
         * Checks if this memory is protected from eviction.
         */
        public boolean isProtected() {
            return isMilestone || Math.abs(emotionalWeight) >= 8;
        }

        public int getAccessCount() {
            return accessCount;
        }

        public Instant getLastAccessed() {
            return lastAccessed;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s", eventType, timestamp, description);
        }

        /**
         * Converts this memory to a context string for LLM prompting.
         */
        public String toContextString() {
            return String.format("[%s] %s", eventType, description);
        }
    }

    /**
     * A semantic memory (fact about the player).
     */
    public static class SemanticMemory {
        public final String category;
        public final String key;
        public final Object value;
        public final Instant learnedAt;
        public int confidence;

        public SemanticMemory(String category, String key, Object value, Instant learnedAt) {
            this.category = category;
            this.key = key;
            this.value = value;
            this.learnedAt = learnedAt;
            this.confidence = 1;
        }
    }

    /**
     * An emotionally significant memory.
     */
    public static class EmotionalMemory {
        public final String eventType;
        public final String description;
        public final int emotionalWeight;
        public final Instant timestamp;

        public EmotionalMemory(String eventType, String description, int emotionalWeight, Instant timestamp) {
            this.eventType = eventType;
            this.description = description;
            this.emotionalWeight = emotionalWeight;
            this.timestamp = timestamp;
        }
    }

    /**
     * An inside joke or memorable phrase.
     */
    public static class InsideJoke {
        public final String context;
        public final String punchline;
        public final Instant createdAt;
        public int referenceCount;

        public InsideJoke(String context, String punchline, Instant createdAt) {
            this.context = context;
            this.punchline = punchline;
            this.createdAt = createdAt;
            this.referenceCount = 0;
        }

        public void incrementReference() {
            referenceCount++;
        }
    }

    /**
     * Working memory entry.
     */
    public static class WorkingMemoryEntry {
        public final String type;
        public final String content;
        public final Instant timestamp;

        public WorkingMemoryEntry(String type, String content, Instant timestamp) {
            this.type = type;
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    /**
     * Conversational memory including jokes and references.
     * Thread-safe: uses CopyOnWriteArrayList for safe concurrent access.
     */
    public static class ConversationalMemory {
        private final List<InsideJoke> insideJokes = new CopyOnWriteArrayList<>();
        private final Set<String> discussedTopics = ConcurrentHashMap.newKeySet();
        private final Map<String, Integer> phraseUsage = new ConcurrentHashMap<>();

        public void addInsideJoke(InsideJoke joke) {
            insideJokes.add(joke);
            if (insideJokes.size() > MAX_INSIDE_JOKES) {
                // Remove least referenced joke - needs synchronization for sort
                synchronized (this) {
                    List<InsideJoke> sorted = new ArrayList<>(insideJokes);
                    sorted.sort(Comparator.comparingInt(j -> j.referenceCount));
                    insideJokes.clear();
                    insideJokes.addAll(sorted.subList(1, sorted.size())); // Remove lowest
                }
            }
        }

        public InsideJoke getRandomJoke() {
            if (insideJokes.isEmpty()) return null;
            InsideJoke joke = insideJokes.get(new Random().nextInt(insideJokes.size()));
            joke.incrementReference();
            return joke;
        }

        public int getJokeCount() {
            return insideJokes.size();
        }

        public void addDiscussedTopic(String topic) {
            discussedTopics.add(topic.toLowerCase());
        }

        public boolean hasDiscussed(String topic) {
            return discussedTopics.contains(topic.toLowerCase());
        }

        public void recordPhraseUsage(String phrase) {
            phraseUsage.merge(phrase, 1, Integer::sum);
        }

        public int getPhraseUsageCount(String phrase) {
            return phraseUsage.getOrDefault(phrase, 0);
        }

        // Package-private methods for NBT loading
        void clearInsideJokes() {
            insideJokes.clear();
        }

        void addInsideJokeDirect(InsideJoke joke) {
            insideJokes.add(joke);
        }

        void clearDiscussedTopics() {
            discussedTopics.clear();
        }

        void addDiscussedTopicDirect(String topic) {
            discussedTopics.add(topic);
        }

        void clearPhraseUsage() {
            phraseUsage.clear();
        }

        void addPhraseUsage(String phrase, int count) {
            phraseUsage.put(phrase, count);
        }
    }

    /**
     * Represents the current relationship state between MineWright and the player.
     */
    public static class Relationship {
        private final int rapport;
        private final int trust;
        private final Mood mood;

        public Relationship(int rapport, int trust, Mood mood) {
            this.rapport = rapport;
            this.trust = trust;
            this.mood = mood;
        }

        public int getAffection() {
            return rapport;
        }

        public int getTrust() {
            return trust;
        }

        public Mood getCurrentMood() {
            return mood;
        }
    }

    /**
     * Current mood states for the companion.
     */
    public enum Mood {
        CHEERFUL("Cheerful", "yellow"),
        FOCUSED("Focused", "blue"),
        PLAYFUL("Playful", "green"),
        SERIOUS("Serious", "gray"),
        EXCITED("Excited", "gold"),
        CALM("Calm", "aqua"),
        TIRED("Tired", "dark_gray"),
        HAPPY("Happy", "green");

        private final String displayName;
        private final String color;

        Mood(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * Personality profile for the foreman.
     * Now includes enhanced speech patterns and verbal tics support.
     * Thread-safe: uses synchronized collections for concurrent access.
     */
    public static class PersonalityProfile {
        // Big Five traits (0-100)
        public volatile int openness = 70;          // Curious, creative
        public volatile int conscientiousness = 80; // Organized, responsible
        public volatile int extraversion = 60;      // Sociable, energetic
        public volatile int agreeableness = 75;     // Cooperative, trusting
        public volatile int neuroticism = 30;       // Calm, stable

        // Custom traits
        public volatile int humor = 65;             // How often uses humor
        public volatile int encouragement = 80;     // How encouraging
        public volatile int formality = 40;         // 0 = casual, 100 = formal

        // Verbal tics and catchphrases - thread-safe lists
        public List<String> catchphrases = new CopyOnWriteArrayList<>(List.of(
            "Right then,",
            "Let's get to work!",
            "We've got this.",
            "Another day, another block."
        ));

        // Enhanced speech patterns
        public List<String> verbalTics = new CopyOnWriteArrayList<>(List.of(
            "Well,",
            "You see,",
            "Here's the thing"
        ));

        // Speech pattern usage tracking
        public Map<String, Integer> ticUsageCount = new ConcurrentHashMap<>();
        public List<String> recentTics = Collections.synchronizedList(new ArrayList<>());

        // Preferences (for consistent behavior)
        public String favoriteBlock = "cobblestone";
        public String workStyle = "methodical";
        public String mood = "cheerful";

        // Archetype name if using a predefined archetype
        public String archetypeName = "THE_FOREMAN";

        /**
         * Generates a personality summary for prompting.
         */
        public String toPromptContext() {
            StringBuilder sb = new StringBuilder();
            sb.append("Personality Traits:\n");
            sb.append("- Openness: ").append(openness).append("% (curious and creative)\n");
            sb.append("- Conscientiousness: ").append(conscientiousness).append("% (organized and reliable)\n");
            sb.append("- Extraversion: ").append(extraversion).append("% (").append(extraversion > 50 ? "outgoing" : "reserved").append(")\n");
            sb.append("- Agreeableness: ").append(agreeableness).append("% (cooperative)\n");
            sb.append("- Humor Level: ").append(humor).append("%\n");
            sb.append("- Formality: ").append(formality > 50 ? "formal" : "casual and friendly").append("\n");
            sb.append("- Current mood: ").append(mood).append("\n");
            sb.append("- Archetype: ").append(archetypeName).append("\n");

            if (!catchphrases.isEmpty()) {
                sb.append("- Catchphrases: ");
                int count = Math.min(3, catchphrases.size());
                sb.append(String.join(", ", catchphrases.subList(0, count)));
                if (catchphrases.size() > 3) {
                    sb.append(" (and ").append(catchphrases.size() - 3).append(" more)");
                }
                sb.append("\n");
            }

            if (!verbalTics.isEmpty()) {
                sb.append("- Verbal Tics (use occasionally): ");
                sb.append(String.join(", ", verbalTics)).append("\n");
            }

            sb.append("- Favorite block: ").append(favoriteBlock).append("\n");
            return sb.toString();
        }

        /**
         * Applies a foreman archetype configuration to this profile.
         */
        public void applyArchetype(com.minewright.personality.ForemanArchetypeConfig.ForemanArchetype archetype) {
            com.minewright.personality.PersonalityTraits traits = archetype.getTraits();
            this.openness = traits.getOpenness();
            this.conscientiousness = traits.getConscientiousness();
            this.extraversion = traits.getExtraversion();
            this.agreeableness = traits.getAgreeableness();
            this.neuroticism = traits.getNeuroticism();
            this.formality = archetype.getFormality();
            this.humor = archetype.getHumor();
            this.encouragement = archetype.getEncouragement();
            this.catchphrases = new ArrayList<>(archetype.getCatchphrases());
            this.verbalTics = new ArrayList<>(archetype.getVerbalTics());
            this.archetypeName = archetype.getName();
        }

        /**
         * Gets a random verbal tic, tracking usage for variety.
         */
        public String getRandomVerbalTic() {
            if (verbalTics.isEmpty()) {
                return "";
            }

            // Track recent tics to avoid repetition
            String selectedTic;
            int attempts = 0;
            do {
                selectedTic = verbalTics.get(new Random().nextInt(verbalTics.size()));
                attempts++;
            } while (recentTics.contains(selectedTic) && attempts < 5);

            // Update tracking
            ticUsageCount.merge(selectedTic, 1, Integer::sum);
            recentTics.add(selectedTic);
            if (recentTics.size() > 5) {
                recentTics.remove(0);
            }

            return selectedTic;
        }

        /**
         * Checks if a verbal tic should be used based on personality and recent usage.
         */
        public boolean shouldUseVerbalTic() {
            if (verbalTics.isEmpty()) {
                return false;
            }

            // Base chance based on neuroticism (nervous characters tic more)
            double baseChance = 0.15 + (neuroticism / 500.0); // 15% to 35%

            // Adjust based on recent tic usage (don't overuse)
            if (!recentTics.isEmpty()) {
                double recentPenalty = recentTics.size() * 0.05;
                baseChance -= recentPenalty;
            }

            return new Random().nextDouble() < Math.max(0.05, baseChance);
        }

        /**
         * Gets the speech pattern description for this personality.
         */
        public String getSpeechPatternDescription() {
            List<String> patterns = new ArrayList<>();

            if (extraversion > 70) {
                patterns.add("enthusiastic and expressive");
            } else if (extraversion < 40) {
                patterns.add("quiet and thoughtful");
            }

            if (formality > 60) {
                patterns.add("formal and polite");
            } else if (formality < 40) {
                patterns.add("casual and relaxed");
            }

            if (humor > 60) {
                patterns.add("frequently humorous");
            }

            if (conscientiousness > 70) {
                patterns.add("methodical and precise");
            }

            if (patterns.isEmpty()) {
                return "balanced and friendly";
            }

            return String.join(", ", patterns);
        }
    }
}
