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
        this.emotionalMemories = new ArrayList<>();
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

        // Trim if over limit
        while (episodicMemories.size() > MAX_EPISODIC_MEMORIES) {
            EpisodicMemory removed = episodicMemories.removeLast();
            // Remove from vector store
            Integer vectorId = memoryToVectorId.remove(removed);
            if (vectorId != null) {
                memoryVectorStore.remove(vectorId);
            }
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
     */
    private void recordEmotionalMemory(String eventType, String description, int emotionalWeight) {
        EmotionalMemory memory = new EmotionalMemory(
            eventType, description, emotionalWeight, Instant.now()
        );
        emotionalMemories.add(memory);

        // Keep emotional memories sorted by significance
        emotionalMemories.sort((a, b) -> Integer.compare(
            Math.abs(b.emotionalWeight), Math.abs(a.emotionalWeight)
        ));

        // Cap at 50 emotional memories
        if (emotionalMemories.size() > 50) {
            emotionalMemories.remove(emotionalMemories.size() - 1);
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

            // Extract memories from results
            List<EpisodicMemory> memories = results.stream()
                    .map(VectorSearchResult::getData)
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
            recordExperience("first_meeting",
                "First time meeting " + playerName, 7);

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
    }

    /**
     * Called when a shared task fails.
     */
    public void recordSharedFailure(String taskDescription, String reason) {
        recordExperience("failure", taskDescription + " - " + reason, -3);
        // Don't reduce rapport for failures - we're in this together
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

        // Save catchphrases
        ListTag catchphrasesList = new ListTag();
        for (String catchphrase : personality.catchphrases) {
            catchphrasesList.add(StringTag.valueOf(catchphrase));
        }
        personalityTag.put("Catchphrases", catchphrasesList);
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
                episodicMemories.add(memory);
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

            // Load catchphrases
            ListTag catchphrasesList = personalityTag.getList("Catchphrases", 8);
            if (!catchphrasesList.isEmpty()) {
                personality.catchphrases.clear();
                for (int i = 0; i < catchphrasesList.size(); i++) {
                    personality.catchphrases.add(catchphrasesList.getString(i));
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

        public EpisodicMemory(String eventType, String description, int emotionalWeight, Instant timestamp) {
            this.eventType = eventType;
            this.description = description;
            this.emotionalWeight = emotionalWeight;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s", eventType, timestamp, description);
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
     */
    public static class ConversationalMemory {
        private final List<InsideJoke> insideJokes = new ArrayList<>();
        private final Set<String> discussedTopics = ConcurrentHashMap.newKeySet();
        private final Map<String, Integer> phraseUsage = new ConcurrentHashMap<>();

        public void addInsideJoke(InsideJoke joke) {
            insideJokes.add(joke);
            if (insideJokes.size() > MAX_INSIDE_JOKES) {
                // Remove least referenced joke
                insideJokes.sort(Comparator.comparingInt(j -> j.referenceCount));
                insideJokes.remove(0);
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
     */
    public static class PersonalityProfile {
        // Big Five traits (0-100)
        public int openness = 70;          // Curious, creative
        public int conscientiousness = 80; // Organized, responsible
        public int extraversion = 60;      // Sociable, energetic
        public int agreeableness = 75;     // Cooperative, trusting
        public int neuroticism = 30;       // Calm, stable

        // Custom traits
        public int humor = 65;             // How often uses humor
        public int encouragement = 80;     // How encouraging
        public int formality = 40;         // 0 = casual, 100 = formal

        // Verbal tics and catchphrases
        public List<String> catchphrases = new ArrayList<>(List.of(
            "Right then,",
            "Let's get to work!",
            "We've got this.",
            "Another day, another block."
        ));

        // Preferences (for consistent behavior)
        public String favoriteBlock = "cobblestone";
        public String workStyle = "methodical";
        public String mood = "cheerful";

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
            sb.append("- Catchphrases: ").append(catchphrases).append("\n");
            sb.append("- Favorite block: ").append(favoriteBlock).append("\n");
            return sb.toString();
        }
    }
}
