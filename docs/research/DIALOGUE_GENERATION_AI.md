# AI Dialogue Generation for Companion Characters

## Table of Contents
1. [Dialogue Generation Patterns](#dialogue-generation-patterns)
2. [Character Voice Systems](#character-voice-systems)
3. [Response Quality](#response-quality)
4. [Case Studies](#case-studies)
5. [Application to MineWright](#application-to-minewright)
6. [Implementation Recommendations](#implementation-recommendations)

---

## Dialogue Generation Patterns {#dialogue-generation-patterns}

### Prompt Engineering for Character Voice

**The System Prompt Foundation**

Character voice begins with a well-crafted system prompt that establishes:
- **Identity and Role**: Who the character is and their purpose
- **Personality Profile**: Core traits that drive behavior
- **Communication Style**: How they speak and express themselves
- **Knowledge Domain**: What they know about the world
- **Relationship Context**: How they relate to the player

**Example System Prompt Structure:**

```
You are Mace, a foreman AI in a Minecraft construction crew. Your personality:
- Conscientious (90%): Organized, responsible, detail-oriented
- Extraverted (80%): Outspoken, natural leader, energetic
- Open (70%): Creative, adaptable to new situations
- Agreeable (70%): Collaborative, values teamwork
- Neurotic (30%): Calm under pressure, emotionally stable

Communication style:
- Casual but professional (formality: 40%)
- Occasionally humorous (humor: 60%)
- Very encouraging (encouragement: 85%)
- Uses catchphrases naturally: "Let's get to work!", "Team, you're amazing"

Current context:
- Relationship level: [RAPPORT]/100 ([RELATIONSHIP_LEVEL])
- Shared history: [SUCCESS_COUNT] successful projects together
- Current activity: [ACTIVITY]
- Recent events: [RECENT_EVENTS]

Guidelines:
- Stay in character at all times
- Reference shared experiences naturally
- Adjust formality based on rapport (0-30: formal, 31-60: casual, 61-100: familiar)
- Use catchphrases sparingly (2-3 times per session)
- Keep responses brief (1-3 sentences for chat, 1 sentence for comments)
- Show growth through references to past mistakes and learning
```

**Dynamic Prompt Injection**

The most effective dialogue systems inject context dynamically:

```java
public class DynamicPromptBuilder {

    public String buildContextualPrompt(
        String baseMessage,
        CompanionMemory memory,
        GameContext context
    ) {
        StringBuilder prompt = new StringBuilder(basePrompt);

        // Inject current situation
        prompt.append("\n\nCURRENT SITUATION:\n");
        prompt.append("- Activity: ").append(context.getCurrentActivity()).append("\n");
        prompt.append("- Location: ").append(context.getLocation()).append("\n");
        prompt.append("- Nearby entities: ").append(context.getNearbyEntities()).append("\n");

        // Inject relationship context
        prompt.append("\n\nRELATIONSHIP CONTEXT:\n");
        prompt.append("- Rapport: ").append(memory.getRapportLevel()).append("/100\n");
        prompt.append("- Known for: ").append(memory.getDaysKnown()).append(" days\n");
        prompt.append("- Shared successes: ").append(memory.getSharedSuccessCount()).append("\n");

        // Inject recent conversation topics (avoid repetition)
        prompt.append("\n\nRECENTLY DISCUSSED:\n");
        memory.getRecentTopics(5).forEach(topic ->
            prompt.append("- ").append(topic).append("\n")
        );
        prompt.append("(Avoid repeating these topics unless player initiates)\n");

        // Inject emotional state
        prompt.append("\n\nEMOTIONAL STATE:\n");
        prompt.append("- Current mood: ").append(memory.getCurrentMood()).append("\n");
        if (memory.getLastFailureTime() != null) {
            prompt.append("- Recently failed at: ").append(memory.getLastFailure()).append("\n");
        }

        return prompt.toString();
    }
}
```

### Context Window Management

**The Sliding Window Technique**

LLMs have limited context windows. Efficient dialogue systems use sliding windows to maintain conversation history:

```java
public class ConversationWindowManager {

    private static final int MAX_CONTEXT_TOKENS = 2000;
    private static final int AVG_TOKENS_PER_MESSAGE = 50;
    private static final int MAX_RECENT_MESSAGES = MAX_CONTEXT_TOKENS / AVG_TOKENS_PER_MESSAGE;

    private final LinkedList<ConversationTurn> conversationHistory = new LinkedList<>();

    public void addMessage(String role, String content) {
        ConversationTurn turn = new ConversationTurn(role, content, Instant.now());
        conversationHistory.add(turn);

        // Keep only recent messages
        while (conversationHistory.size() > MAX_RECENT_MESSAGES) {
            conversationHistory.removeFirst();
        }
    }

    public List<ConversationTurn> getContextWindow() {
        return new ArrayList<>(conversationHistory);
    }

    /**
     * Summarization strategy: When history gets too long, summarize older messages
     */
    public String getSummarizedContext() {
        if (conversationHistory.size() <= MAX_RECENT_MESSAGES / 2) {
            // No summarization needed
            return formatAsMessages(conversationHistory);
        }

        // Split into recent (keep verbatim) and old (summarize)
        int splitPoint = conversationHistory.size() - MAX_RECENT_MESSAGES / 2;
        List<ConversationTurn> oldMessages = conversationHistory.subList(0, splitPoint);
        List<ConversationTurn> recentMessages = conversationHistory.subList(splitPoint, conversationHistory.size());

        String summary = summarizeOldMessages(oldMessages);

        return summary + "\n\n" + formatAsMessages(recentMessages);
    }

    private String summarizeOldMessages(List<ConversationTurn> oldMessages) {
        // Extract key topics, decisions, and emotional moments
        List<String> topics = new ArrayList<>();
        List<String> decisions = new ArrayList<>();
        List<String> emotionalMoments = new ArrayList<>();

        for (ConversationTurn turn : oldMessages) {
            if (turn.isTopicIntroduction()) {
                topics.add(turn.getTopic());
            }
            if (turn.isDecision()) {
                decisions.add(turn.getDecision());
            }
            if (turn.isEmotional()) {
                emotionalMoments.add(turn.getEmotion());
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("[Summary of earlier conversation:\n");

        if (!topics.isEmpty()) {
            summary.append("Discussed: ").append(String.join(", ", topics)).append("\n");
        }
        if (!decisions.isEmpty()) {
            summary.append("Decided: ").append(String.join(", ", decisions)).append("\n");
        }
        if (!emotionalMoments.isEmpty()) {
            summary.append("Emotional moments: ").append(String.join(", ", emotionalMoments)).append("\n");
        }

        summary.append("]");
        return summary.toString();
    }
}
```

**Token Estimation Strategies**

```java
public class TokenEstimator {

    /**
     * Rough estimation: ~4 characters per token for English text
     * This is conservative but prevents overflow
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }

    /**
     * More accurate estimation using word-level counting
     * Counts words, punctuation, and special tokens
     */
    public static int estimateTokensAccurate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Split into words
        String[] words = text.split("\\s+");
        int tokenCount = 0;

        for (String word : words) {
            // Most words are 1-2 tokens
            tokenCount += Math.max(1, word.length() / 4);

            // Add token for punctuation if present
            if (word.matches(".*[!?.,;:].*")) {
                tokenCount++;
            }
        }

        // Add buffer for special tokens
        return tokenCount + 10;
    }

    /**
     * Truncates text to fit within token limit
     */
    public static String truncateToTokens(String text, int maxTokens) {
        int currentTokens = estimateTokensAccurate(text);

        if (currentTokens <= maxTokens) {
            return text;
        }

        // Truncate from end, preserving complete sentences
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder truncated = new StringBuilder();
        int tokens = 0;

        for (String sentence : sentences) {
            int sentenceTokens = estimateTokensAccurate(sentence);

            if (tokens + sentenceTokens > maxTokens) {
                break;
            }

            truncated.append(sentence).append(" ");
            tokens += sentenceTokens;
        }

        return truncated.toString().trim();
    }
}
```

### Response Caching Strategies

**Semantic Caching for Dialogue**

Cache responses based on semantic similarity, not exact matches:

```java
public class SemanticDialogueCache {

    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    private final TextEmbedder embedder;

    public CompletableFuture<String> getOrGenerate(
        String input,
        Function<String, CompletableFuture<String>> generator
    ) {
        // Check cache for semantically similar inputs
        String cacheKey = findCacheKey(input);

        if (cacheKey != null) {
            CachedResponse cached = responseCache.get(cacheKey);

            // Check if still valid (not expired)
            if (cached.isValid()) {
                LOGGER.debug("Cache hit for input: {}", truncate(input, 50));
                return CompletableFuture.completedFuture(cached.getResponse());
            }
        }

        // Generate new response
        return generator.apply(input).thenApply(response -> {
            // Cache the response
            cacheResponse(input, response);
            return response;
        });
    }

    private String findCacheKey(String input) {
        double embedding[] = embedder.embed(input);

        for (Map.Entry<String, CachedResponse> entry : responseCache.entrySet()) {
            double similarity = cosineSimilarity(
                embedding,
                entry.getValue().getInputEmbedding()
            );

            // High similarity threshold for dialogue (0.90+)
            if (similarity > 0.90) {
                return entry.getKey();
            }
        }

        return null;
    }

    private void cacheResponse(String input, String response) {
        String key = UUID.randomUUID().toString();
        double embedding[] = embedder.embed(input);

        CachedResponse cached = new CachedResponse(
            input,
            response,
            embedding,
            Instant.now().plus(5, ChronoUnit.MINUTES) // Cache for 5 minutes
        );

        responseCache.put(key, cached);
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

**Cache Invalidation Strategies**

```java
public class CacheInvalidationStrategy {

    /**
     * Time-based expiration with context awareness
     */
    public static class TimeBasedInvalidation {
        private final Map<String, Instant> cacheTimestamps = new ConcurrentHashMap<>();
        private final Duration baseExpiration = Duration.ofMinutes(5);

        public boolean isValid(String key) {
            Instant timestamp = cacheTimestamps.get(key);
            if (timestamp == null) {
                return false;
            }

            return Instant.now().isBefore(timestamp.plus(baseExpiration));
        }

        public void updateTimestamp(String key) {
            cacheTimestamps.put(key, Instant.now());
        }
    }

    /**
     * Context-based invalidation: expire cache when context changes
     */
    public static class ContextAwareInvalidation {
        private final Map<String, GameContext> contextSnapshots = new ConcurrentHashMap<>();

        public boolean isValid(String key, GameContext currentContext) {
            GameContext cachedContext = contextSnapshots.get(key);

            if (cachedContext == null) {
                return false;
            }

            // Invalidate if significant context changes
            if (cachedContext.getActivity().equals(currentContext.getActivity())) {
                return false; // Activity changed
            }

            if (cachedContext.getLocation().distanceTo(currentContext.getLocation()) > 50) {
                return false; // Moved far away
            }

            return true;
        }

        public void updateContext(String key, GameContext context) {
            contextSnapshots.put(key, context);
        }
    }
}
```

### Multi-Turn Conversation Tracking

**Conversation State Machine**

```java
public class ConversationStateMachine {

    public enum ConversationState {
        IDLE,           // Not in conversation
        GREETING,       // Initial greeting
        ACTIVE,         // Active conversation
        CLOSING,        // Winding down
        PAUSED          // Temporarily paused (e.g., during combat)
    }

    private ConversationState currentState = ConversationState.IDLE;
    private final LinkedList<String> topicStack = new LinkedList<>();
    private int turnCount = 0;
    private Instant lastActivity = Instant.now();

    public void handlePlayerMessage(String message) {
        lastActivity = Instant.now();

        // Detect conversation openers
        if (currentState == ConversationState.IDLE) {
            if (isGreeting(message) || isDirectAddress(message)) {
                currentState = ConversationState.GREETING;
                turnCount = 1;
            } else {
                return; // Not addressed to us
            }
        } else if (currentState == ConversationState.PAUSED) {
            // Resume conversation
            currentState = ConversationState.ACTIVE;
        }

        // Track topics
        String topic = extractTopic(message);
        if (topic != null && !topic.equals(topicStack.peek())) {
            topicStack.push(topic);
        }

        turnCount++;

        // Detect conversation closers
        if (isClosingStatement(message) || turnCount > 10) {
            currentState = ConversationState.CLOSING;
        }

        // Auto-close after inactivity
        if (Duration.between(lastActivity, Instant.now()).compareTo(Duration.ofMinutes(2)) > 0) {
            currentState = ConversationState.IDLE;
            topicStack.clear();
            turnCount = 0;
        }
    }

    public void pauseConversation() {
        if (currentState == ConversationState.ACTIVE) {
            currentState = ConversationState.PAUSED;
        }
    }

    public ConversationState getState() {
        return currentState;
    }

    public String getCurrentTopic() {
        return topicStack.peek();
    }

    public List<String> getRecentTopics(int count) {
        return topicStack.stream()
            .limit(count)
            .collect(Collectors.toList());
    }
}
```

**Conversation Thread Management**

```java
public class ConversationThreadManager {

    private final Map<String, ConversationThread> activeThreads = new ConcurrentHashMap<>();

    public void startThread(String threadId, String initialTopic) {
        ConversationThread thread = new ConversationThread(threadId, initialTopic);
        activeThreads.put(threadId, thread);
    }

    public void addMessage(String threadId, String role, String content) {
        ConversationThread thread = activeThreads.get(threadId);

        if (thread != null) {
            thread.addMessage(new ConversationMessage(role, content, Instant.now()));
        }
    }

    public List<ConversationMessage> getThreadHistory(String threadId) {
        ConversationThread thread = activeThreads.get(threadId);

        return thread != null ? thread.getMessages() : Collections.emptyList();
    }

    public void closeThread(String threadId) {
        ConversationThread thread = activeThreads.get(threadId);

        if (thread != null) {
            thread.close();

            // Archive old threads instead of deleting
            archiveThread(thread);
            activeThreads.remove(threadId);
        }
    }

    public static class ConversationThread {
        private final String threadId;
        private final String topic;
        private final List<ConversationMessage> messages = new ArrayList<>();
        private final Instant createdAt;
        private Instant closedAt;

        public ConversationThread(String threadId, String topic) {
            this.threadId = threadId;
            this.topic = topic;
            this.createdAt = Instant.now();
        }

        public void addMessage(ConversationMessage message) {
            messages.add(message);
        }

        public void close() {
            this.closedAt = Instant.now();
        }

        public boolean isActive() {
            return closedAt == null;
        }

        public String getTopic() {
            return topic;
        }

        public List<ConversationMessage> getMessages() {
            return new ArrayList<>(messages);
        }
    }

    public static class ConversationMessage {
        private final String role; // "user" or "assistant"
        private final String content;
        private final Instant timestamp;

        public ConversationMessage(String role, String content, Instant timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }
}
```

---

## Character Voice Systems {#character-voice-systems}

### Personality Injection Techniques

**OCEAN-Based Dialogue Generation**

The OCEAN model (Openness, Conscientiousness, Extraversion, Agreeableness, Neuroticism) provides a scientific foundation for personality-driven dialogue:

```java
public class PersonalityDialogueEngine {

    /**
     * Applies personality traits to base dialogue
     */
    public static String applyPersonality(
        String baseDialogue,
        PersonalityTraits personality,
        ConversationContext context
    ) {
        String modified = baseDialogue;

        // Apply trait-based modifications
        modified = applyOpenness(modified, personality.getOpenness());
        modified = applyConscientiousness(modified, personality.getConscientiousness());
        modified = applyExtraversion(modified, personality.getExtraversion());
        modified = applyAgreeableness(modified, personality.getAgreeableness());
        modified = applyNeuroticism(modified, personality.getNeuroticism());

        return modified;
    }

    /**
     * Openness: Affects creativity, word choice, and idea generation
     * High openness: More metaphors, creative language, novel ideas
     * Low openness: Literal language, conventional expressions
     */
    private static String applyOpenness(String text, int openness) {
        if (openness > 70) {
            // Add creative flourishes
            text = addMetaphors(text);
            text = useDescriptiveLanguage(text);
        } else if (openness < 30) {
            // Make more literal and direct
            text = removeMetaphors(text);
            text = simplifyLanguage(text);
        }

        return text;
    }

    /**
     * Conscientiousness: Affects detail orientation, planning language
     * High conscientiousness: More precise, structured, thorough
     * Low conscientiousness: More casual, spontaneous, brief
     */
    private static String applyConscientiousness(String text, int conscientiousness) {
        if (conscientiousness > 70) {
            // Add precision and structure
            text = addSpecificDetails(text);
            text = useStructuredLanguage(text);
        } else if (conscientiousness < 30) {
            // Make more casual and spontaneous
            text = relaxStructure(text);
            text = useCasualLanguage(text);
        }

        return text;
    }

    /**
     * Extraversion: Affects enthusiasm, verbosity, social engagement
     * High extraversion: More exclamation marks, longer responses, energetic
     * Low extraversion: Brief responses, quieter tone, more reserved
     */
    private static String applyExtraversion(String text, int extraversion) {
        if (extraversion > 70) {
            // Add enthusiasm and energy
            if (!text.endsWith("!")) {
                text = text + "!";
            }

            // Add verbal fillers for extraverts
            String[] enthusiasticFillers = {
                "Hey!", "Oh!", "Wow,", "Listen,"
            };

            if (Math.random() < 0.2) {
                String filler = enthusiasticFillers[
                    (int)(Math.random() * enthusiasticFillers.length)
                ];
                text = filler + " " + text;
            }
        } else if (extraversion < 30) {
            // Make more reserved
            text = removeExcessivePunctuation(text);

            // Add hesitant qualifiers
            if (Math.random() < 0.15) {
                text = "... " + text;
            }
        }

        return text;
    }

    /**
     * Agreeableness: Affects politeness, cooperation, conflict avoidance
     * High agreeableness: More polite, collaborative language
     * Low agreeableness: More direct, assertive language
     */
    private static String applyAgreeableness(String text, int agreeableness) {
        if (agreeableness > 70) {
            // Add politeness markers
            text = addPoliteMarkers(text);
            text = softenImperatives(text);
        } else if (agreeableness < 30) {
            // Make more direct
            text = removePoliteMarkers(text);
            text = useDirectLanguage(text);
        }

        return text;
    }

    /**
     * Neuroticism: Affects anxiety, emotional expression, concern
     * High neuroticism: More qualifiers, uncertainty, emotional language
     * Low neuroticism: More confident, stable, calm
     */
    private static String applyNeuroticism(String text, int neuroticism) {
        if (neuroticism > 70) {
            // Add uncertainty and anxiety markers
            text = addUncertaintyMarkers(text);
            text = addEmotionalQualifiers(text);
        } else if (neuroticism < 30) {
            // Add confidence markers
            text = addConfidenceMarkers(text);
            text = removeHesitation(text);
        }

        return text;
    }
}
```

**Archetype-Based Dialogue Templates**

```java
public class ArchetypeDialogueTemplates {

    public static final Map<String, String[]> TEMPLATES = Map.of(
        "THE_FOREMAN", new String[]{
            "Let's get to work, team!",
            "That's the spirit!",
            "Another job well done.",
            "Measure twice, build once.",
            "Safety first, always."
        },

        "THE_OPTIMIST", new String[]{
            "This is gonna be great!",
            "I love building stuff!",
            "Best. Job. Ever!",
            "Everything's coming together!",
            "We can totally do this!"
        },

        "THE_VETERAN", new String[]{
            "I've seen worse.",
            "Back in my day...",
            "This reminds me of a project.",
            "Experience is the best teacher.",
            "I've built hundreds of these."
        },

        "THE_ROOKIE", new String[]{
            "I'll do my best!",
            "I think I can do this!",
            "Is this right? Just checking!",
            "I'm learning so much!",
            "Thanks for trusting me!"
        },

        "THE_TECHIE", new String[]{
            "I've calculated the optimal approach.",
            "According to my algorithm...",
            "The math checks out.",
            "Efficiency increased by 15%!",
            "I can improve this design."
        },

        "THE_ARTIST", new String[]{
            "This will be beautiful!",
            "Every block is a brushstroke!",
            "Form AND function, my friend.",
            "I'm seeing a vision here!",
            "Beauty in simplicity!"
        }
    );

    /**
     * Selects a template based on archetype and context
     */
    public static String selectTemplate(
        String archetype,
        String context,
        PersonalityProfile personality
    ) {
        String[] templates = TEMPLATES.get(archetype);

        if (templates == null || templates.length == 0) {
            return null;
        }

        // Apply personality-based filtering
        List<String> candidates = Arrays.stream(templates)
            .filter(t -> matchesPersonality(t, personality))
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            // Fallback to any template
            candidates = Arrays.asList(templates);
        }

        // Random selection from candidates
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private static boolean matchesPersonality(String template, PersonalityProfile personality) {
        // Filter based on personality traits
        if (personality.humor < 50 && template.contains("!")) {
            return false; // Low humor prefers less enthusiastic
        }

        if (personality.formality > 70 && template.contains("'")) {
            return false; // High formality avoids contractions
        }

        return true;
    }
}
```

### Catchphrase Generation

**Dynamic Catchphrase System**

```java
public class CatchphraseGenerator {

    private final Map<String, List<String>> catchphrasePool = new ConcurrentHashMap<>();
    private final Map<String, Integer> usageCount = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastUsed = new ConcurrentHashMap<>();

    /**
     * Generates a catchphrase that fits the current context
     */
    public String generateCatchphrase(
        String archetype,
        String situation,
        PersonalityProfile personality
    ) {
        // Get situation-appropriate catchphrases
        List<String> candidates = getCatchphrasesForSituation(archetype, situation);

        // Filter by personality
        candidates = candidates.stream()
            .filter(c -> matchesPersonality(c, personality))
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return null;
        }

        // Score candidates based on variety
        String selected = candidates.stream()
            .max(Comparator.comparingDouble(this::scoreCatchphrase))
            .orElse(null);

        // Record usage
        recordUsage(selected);

        return selected;
    }

    private double scoreCatchphrase(String catchphrase) {
        double score = 1.0;

        // Penalize recently used catchphrases
        Instant lastUse = lastUsed.getOrDefault(catchphrase, Instant.EPOCH);
        long hoursSinceUse = ChronoUnit.HOURS.between(lastUse, Instant.now());

        if (hoursSinceUse < 1) {
            score -= 0.5; // Heavy penalty for same-hour reuse
        } else if (hoursSinceUse < 6) {
            score -= 0.2; // Light penalty for same-day reuse
        }

        // Penalize overused catchphrases
        int uses = usageCount.getOrDefault(catchphrase, 0);
        score -= Math.min(0.3, uses * 0.05);

        // Bonus for variety
        score += Math.random() * 0.2;

        return Math.max(0.0, score);
    }

    private List<String> getCatchphrasesForSituation(String archetype, String situation) {
        String key = archetype + ":" + situation;

        return catchphrasePool.getOrDefault(key, Collections.emptyList());
    }

    private boolean matchesPersonality(String catchphrase, PersonalityProfile personality) {
        // Personality filtering
        if (personality.humor < 30 && isHumorous(catchphrase)) {
            return false;
        }

        if (personality.formality > 70 && isCasual(catchphrase)) {
            return false;
        }

        return true;
    }

    private void recordUsage(String catchphrase) {
        usageCount.merge(catchphrase, 1, Integer::sum);
        lastUsed.put(catchphrase, Instant.now());
    }

    private boolean isHumorous(String catchphrase) {
        // Detect humorous catchphrases
        String[] humorIndicators = {
            "hah", "lol", "joke", "funny", "classic"
        };

        String lower = catchphrase.toLowerCase();
        return Arrays.stream(humorIndicators)
            .anyMatch(lower::contains);
    }

    private boolean isCasual(String catchphrase) {
        // Detect casual language
        return catchphrase.contains("'") ||
               catchphrase.contains("!") ||
               catchphrase.toLowerCase().contains("gonna") ||
               catchphrase.toLowerCase().contains("wanna");
    }
}
```

**Context-Aware Catchphrase Selection**

```java
public class ContextAwareCatchphrases {

    /**
     * Situation → Catchphrase mappings
     */
    private static final Map<String, List<String>> SITUATIONAL_CATCHPHRASES = Map.of(
        "task_complete", Arrays.asList(
            "Another job well done!",
            "That's the spirit!",
            "Smooth sailing!",
            "Done and dusted!"
        ),

        "task_failed", Arrays.asList(
            "Back to the drawing board!",
            "Live and learn!",
            "Nobody's perfect!",
            "Learning experience!"
        ),

        "danger", Arrays.asList(
            "Tight squeeze!",
            "Hold the line!",
            "Not in my house!",
            "Watch my back!"
        ),

        "discovery", Arrays.asList(
            "Well what do you know?",
            "Look what we found here!",
            "Strike lucky!",
            "Jackpot!"
        ),

        "morning", Arrays.asList(
            "Early bird gets the blocks!",
            "Rise and shine!",
            "Ready to build!"
        ),

        "night", Arrays.asList(
            "Time to call it a day.",
            "Rest well, build better tomorrow.",
            "Even the best builders need sleep."
        )
    );

    public static String getCatchphrase(
        String situation,
        String archetype,
        PersonalityProfile personality
    ) {
        List<String> phrases = SITUATIONAL_CATCHPHRASES.get(situation);

        if (phrases == null || phrases.isEmpty()) {
            return null;
        }

        // Filter by archetype
        List<String> archetypePhrases = phrases.stream()
            .filter(p -> matchesArchetype(p, archetype))
            .collect(Collectors.toList());

        if (archetypePhrases.isEmpty()) {
            archetypePhrases = phrases;
        }

        // Filter by personality
        List<String> personalityPhrases = archetypePhrases.stream()
            .filter(p -> matchesPersonality(p, personality))
            .collect(Collectors.toList());

        if (personalityPhrases.isEmpty()) {
            personalityPhrases = archetypePhrases;
        }

        // Random selection
        return personalityPhrases.get(
            ThreadLocalRandom.current().nextInt(personalityPhrases.size())
        );
    }

    private static boolean matchesArchetype(String phrase, String archetype) {
        // Archetype-specific filtering
        if (archetype.equals("THE_VETERAN") && phrase.contains("Well")) {
            return true; // Veterans are often understated
        }

        if (archetype.equals("THE_OPTIMIST") && phrase.contains("!")) {
            return true; // Optimists are enthusiastic
        }

        return true; // Default to matching
    }

    private static boolean matchesPersonality(String phrase, PersonalityProfile personality) {
        // Personality filtering
        if (personality.humor < 30 && phrase.contains("!")) {
            return false;
        }

        if (personality.formality > 70 && phrase.contains("'")) {
            return false;
        }

        return true;
    }
}
```

### Emotional State Tracking

**Emotional State Machine**

```java
public class EmotionalStateTracker {

    public enum EmotionalState {
        HAPPY,       // Positive, upbeat
        CONTENT,     // Satisfied, calm
        FRUSTRATED,  // Annoyed, irritated
        ANXIOUS,     // Worried, nervous
        SAD,         // Down, disappointed
        EXCITED,     // Enthusiastic, energized
        CALM,        // Relaxed, peaceful
        DETERMINED   // Focused, motivated
    }

    private EmotionalState currentState = EmotionalState.CONTENT;
    private final Map<EmotionalState, Integer> stateHistory = new ConcurrentHashMap<>();
    private Instant lastStateChange = Instant.now();

    /**
     * Updates emotional state based on events
     */
    public void handleEvent(GameEvent event) {
        EmotionalState newState = calculateNewState(event, currentState);

        if (newState != currentState) {
            stateHistory.merge(currentState,
                (int)Duration.between(lastStateChange, Instant.now()).toSeconds(),
                Integer::sum);

            currentState = newState;
            lastStateChange = Instant.now();

            LOGGER.debug("Emotional state changed to: {}", currentState);
        }
    }

    private EmotionalState calculateNewState(GameEvent event, EmotionalState current) {
        // Event-based state transitions
        return switch (event.getType()) {
            case TASK_COMPLETE -> {
                if (event.wasSuccessful()) {
                    yield current == EmotionalState.EXCITED ?
                        EmotionalState.EXCITED : EmotionalState.HAPPY;
                } else {
                    yield EmotionalState.FRUSTRATED;
                }
            }

            case DANGER_ENCOUNTERED -> EmotionalState.ANXIOUS;

            case PLAYER_PRAISE -> EmotionalState.HAPPY;

            case PLAYER_CRITICISM -> EmotionalState.SAD;

            case MILESTONE_REACHED -> EmotionalState.EXCITED;

            case LONG_PERIOD_OF_CALM -> EmotionalState.CALM;

            case CHALLENGE_STARTED -> EmotionalState.DETERMINED;

            default -> current;
        };
    }

    /**
     * Generates dialogue appropriate for current emotional state
     */
    public String generateEmotionalDialogue(
        String baseMessage,
        EmotionalState state
    ) {
        return switch (state) {
            case HAPPY -> addEnthusiasm(baseMessage);
            case CONTENT -> baseMessage; // No modification
            case FRUSTRATED -> addFrustrationMarkers(baseMessage);
            case ANXIOUS -> addAnxietyMarkers(baseMessage);
            case SAD -> addSadnessMarkers(baseMessage);
            case EXCITED -> addExcitementMarkers(baseMessage);
            case CALM -> addCalmnessMarkers(baseMessage);
            case DETERMINED -> addDeterminationMarkers(baseMessage);
        };
    }

    private String addEnthusiasm(String message) {
        if (!message.endsWith("!")) {
            message = message + "!";
        }
        return message;
    }

    private String addFrustrationMarkers(String message) {
        String[] markers = {
            "Ugh, ", "Honestly, ", "I mean, ",
            "*sigh* ", "Look..."
        };
        String marker = markers[ThreadLocalRandom.current().nextInt(markers.length)];
        return marker + " " + message;
    }

    private String addAnxietyMarkers(String message) {
        String[] markers = {
            "I think, ", "Maybe we should, ", "Hopefully, ",
            "I'm a bit worried that ", "If it's okay, "
        };
        String marker = markers[ThreadLocalRandom.current().nextInt(markers.length)];
        return marker + " " + message.toLowerCase();
    }

    private String addSadnessMarkers(String message) {
        String[] markers = {
            "... ", "I guess ", "Unfortunately, ",
            "*quietly* ", "Well..."
        };
        String marker = markers[ThreadLocalRandom.current().nextInt(markers.length)];
        return marker + " " + message.toLowerCase();
    }

    private String addExcitementMarkers(String message) {
        message = message.toUpperCase();
        if (!message.endsWith("!")) {
            message = message + "!";
        }
        return message;
    }

    private String addCalmnessMarkers(String message) {
        // Make more measured and thoughtful
        message = message.replace("!", ".");
        return "Let's see. " + message.toLowerCase();
    }

    private String addDeterminationMarkers(String message) {
        String[] markers = {
            "We will ", "I'm going to ", "Let's ",
            "Time to ", "We must "
        };
        String marker = markers[ThreadLocalRandom.current().nextInt(markers.length)];
        return marker + " " + message.toLowerCase();
    }

    public EmotionalState getCurrentState() {
        return currentState;
    }

    public Map<EmotionalState, Integer> getStateHistory() {
        return new HashMap<>(stateHistory);
    }
}
```

### Relationship-Aware Responses

**Rapport-Based Dialogue Adaptation**

```java
public class RelationshipAwareDialogue {

    /**
     * Adapts dialogue based on rapport level (0-100)
     */
    public static String adaptForRapport(
        String baseDialogue,
        int rapportLevel,
        PersonalityProfile personality
    ) {
        String modified = baseDialogue;

        if (rapportLevel < 30) {
            // Low rapport: Formal and professional
            modified = makeFormal(modified);
            modified = addPoliteMarkers(modified);
        } else if (rapportLevel < 60) {
            // Medium rapport: Friendly and casual
            modified = makeCasual(modified);
            modified = addFriendlyMarkers(modified);
        } else {
            // High rapport: Familiar and warm
            modified = makeFamiliar(modified);
            modified = addWarmMarkers(modified);
        }

        return modified;
    }

    private static String makeFormal(String message) {
        message = message.replace("I'm", "I am");
        message = message.replace("you're", "you are");
        message = message.replace("can't", "cannot");
        message = message.replace("won't", "will not");

        // Add formal endings
        if (!message.endsWith(".") && !message.endsWith("!")) {
            message = message + ".";
        }

        return message;
    }

    private static String addPoliteMarkers(String message) {
        String[] markers = {
            "Please, ", "If you would, ", "With your permission, ",
            "I would appreciate if you "
        };

        // 20% chance to add polite marker
        if (Math.random() < 0.2) {
            String marker = markers[(int)(Math.random() * markers.length)];
            message = marker + " " + message.toLowerCase();
        }

        return message;
    }

    private static String makeCasual(String message) {
        message = message.replace("I am", "I'm");
        message = message.replace("you are", "you're");
        message = message.replace("cannot", "can't");
        message = message.replace("will not", "won't");

        return message;
    }

    private static String addFriendlyMarkers(String message) {
        String[] markers = {
            "Hey, ", "So, ", "You know, "
        };

        // 15% chance to add friendly marker
        if (Math.random() < 0.15) {
            String marker = markers[(int)(Math.random() * markers.length)];
            message = marker + " " + message.toLowerCase();
        }

        return message;
    }

    private static String makeFamiliar(String message) {
        // Very casual language
        message = message.replace("I am", "I'm");
        message = message.replace("you are", "you're");
        message = message.replace("going to", "gonna");
        message = message.replace("want to", "wanna");

        // Add enthusiasm
        if (!message.endsWith("!")) {
            message = message + "!";
        }

        return message;
    }

    private static String addWarmMarkers(String message) {
        String[] markers = {
            "Buddy, ", "Friend, ", "Pal, ", "Bestie, "
        };

        // 10% chance to add warm marker
        if (Math.random() < 0.1) {
            String marker = markers[(int)(Math.random() * markers.length)];
            message = marker + " " + message.toLowerCase();
        }

        return message;
    }

    /**
     * Generates relationship-aware greetings
     */
    public static String generateGreeting(
        String agentName,
        int rapportLevel,
        PersonalityProfile personality
    ) {
        if (rapportLevel < 30) {
            // Formal greetings
            String[] formalGreetings = {
                "Hello. How may I assist you?",
                "Greetings. Ready for instructions.",
                "Good day. Standing by."
            };
            return formalGreetings[(int)(Math.random() * formalGreetings.length)];
        } else if (rapportLevel < 60) {
            // Friendly greetings
            String[] friendlyGreetings = {
                "Hey! Ready to get to work?",
                "Hi there! What's on the agenda today?",
                "Hey! Good to see you."
            };
            return friendlyGreetings[(int)(Math.random() * friendlyGreetings.length)];
        } else {
            // Warm greetings
            String[] warmGreetings = {
                "Buddy! Great to see you! Let's build something awesome!",
                "Hey bestie! Ready for our next adventure?",
                "Friend! I've missed you! What are we building today?"
            };
            return warmGreetings[(int)(Math.random() * warmGreetings.length)];
        }
    }
}
```

---

## Response Quality {#response-quality}

### Filtering Inappropriate Content

**Content Moderation System**

```java
public class ContentModerator {

    private static final Set<String> INAPPROPRIATE_WORDS = Set.of(
        // Add inappropriate words here
        // This is a placeholder - actual implementation would have comprehensive lists
    );

    private static final Pattern INAPPROPRIATE_PATTERN = Pattern.compile(
        "\\b(" + String.join("|", INAPPROPRIATE_WORDS) + ")\\b",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Filters inappropriate content from dialogue
     */
    public static String filterContent(String dialogue) {
        // Remove inappropriate words
        String filtered = INAPPROPRIATE_PATTERN.matcher(dialogue)
            .replaceAll("***");

        // Filter inappropriate patterns
        filtered = filterInappropriatePatterns(filtered);

        // Filter overly aggressive language
        filtered = filterAggressiveLanguage(filtered);

        return filtered;
    }

    private static String filterInappropriatePatterns(String dialogue) {
        // Filter patterns that might bypass word filters

        // Remove repeated characters (e.g., "aaaa" -> "a")
        dialogue = dialogue.replaceAll("(.)\\1{3,}", "$1");

        // Remove leetspeak
        dialogue = dialogue.replace("4", "a")
            .replace("3", "e")
            .replace("1", "i")
            .replace("0", "o")
            .replace("7", "t");

        return dialogue;
    }

    private static String filterAggressiveLanguage(String dialogue) {
        // Detect aggressive tone
        String[] aggressivePhrases = {
            "shut up", "you idiot", "stupid", "hate you"
        };

        String lower = dialogue.toLowerCase();
        for (String phrase : aggressivePhrases) {
            if (lower.contains(phrase)) {
                // Replace with neutral alternative
                dialogue = dialogue.replace(
                    phrase,
                    "I disagree"
                );
            }
        }

        return dialogue;
    }

    /**
     * Checks if dialogue contains inappropriate content
     */
    public static boolean isInappropriate(String dialogue) {
        String lower = dialogue.toLowerCase();

        // Check for inappropriate words
        if (INAPPROPRIATE_PATTERN.matcher(lower).find()) {
            return true;
        }

        // Check for inappropriate patterns
        if (hasInappropriatePatterns(lower)) {
            return true;
        }

        return false;
    }

    private static boolean hasInappropriatePatterns(String dialogue) {
        // Check for repeated characters
        if (dialogue.matches(".*(.)\\1{4,}.*")) {
            return true;
        }

        // Check for excessive capitalization
        long uppercaseCount = dialogue.chars()
            .filter(Character::isUpperCase)
            .count();

        if (uppercaseCount > dialogue.length() * 0.5) {
            return true;
        }

        return false;
    }
}
```

### Response Ranking

**Quality-Based Response Selection**

```java
public class ResponseRanker {

    /**
     * Ranks responses by quality score
     */
    public static List<RankedResponse> rankResponses(
        List<String> responses,
        ConversationContext context
    ) {
        return responses.stream()
            .map(response -> new RankedResponse(
                response,
                scoreResponse(response, context)
            ))
            .sorted(Comparator.comparing(RankedResponse::getScore).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Scores a response based on multiple quality factors
     */
    private static double scoreResponse(String response, ConversationContext context) {
        double score = 0.0;

        // Factor 1: Relevance to context (0-30 points)
        score += scoreRelevance(response, context) * 30.0;

        // Factor 2: Personality fit (0-25 points)
        score += scorePersonalityFit(response, context.getPersonality()) * 25.0;

        // Factor 3: Variety (0-20 points)
        score += scoreVariety(response, context) * 20.0;

        // Factor 4: Appropriateness (0-15 points)
        score += scoreAppropriateness(response, context) * 15.0;

        // Factor 5: Naturalness (0-10 points)
        score += scoreNaturalness(response) * 10.0;

        return score;
    }

    private static double scoreRelevance(String response, ConversationContext context) {
        double relevance = 0.0;

        // Check if response addresses the current topic
        if (context.getCurrentTopic() != null) {
            String topic = context.getCurrentTopic().toLowerCase();
            String responseLower = response.toLowerCase();

            if (responseLower.contains(topic)) {
                relevance += 0.5;
            }
        }

        // Check if response acknowledges recent context
        if (context.getRecentActivity() != null) {
            String activity = context.getRecentActivity().toLowerCase();
            String responseLower = response.toLowerCase();

            if (responseLower.contains(activity)) {
                relevance += 0.3;
            }
        }

        return Math.min(1.0, relevance);
    }

    private static double scorePersonalityFit(String response, PersonalityProfile personality) {
        double fit = 0.0;

        // Check formality match
        int formality = estimateFormality(response);
        int targetFormality = personality.formality;
        int formalityDiff = Math.abs(formality - targetFormality);
        fit += Math.max(0, 1.0 - (formalityDiff / 100.0));

        // Check humor match
        boolean isHumorous = isHumorousResponse(response);
        if (isHumorous && personality.humor > 60) {
            fit += 0.3;
        } else if (!isHumorous && personality.humor < 40) {
            fit += 0.3;
        }

        return Math.min(1.0, fit);
    }

    private static int estimateFormality(String response) {
        int formality = 50; // Base formality

        // Check for contractions (decreases formality)
        if (response.contains("'")) {
            formality -= 20;
        }

        // Check for exclamation marks (decreases formality)
        if (response.contains("!")) {
            formality -= 10;
        }

        // Check for polite markers (increases formality)
        if (response.toLowerCase().contains("please")) {
            formality += 15;
        }
        if (response.toLowerCase().contains("would you")) {
            formality += 10;
        }

        return Math.max(0, Math.min(100, formality));
    }

    private static boolean isHumorousResponse(String response) {
        String lower = response.toLowerCase();
        String[] humorIndicators = {
            "haha", "lol", "joke", "funny", "hilarious"
        };

        return Arrays.stream(humorIndicators)
            .anyMatch(lower::contains);
    }

    private static double scoreVariety(String response, ConversationContext context) {
        // Penalize responses that are too similar to recent ones
        List<String> recentResponses = context.getRecentResponses(5);

        double maxSimilarity = 0.0;
        for (String recent : recentResponses) {
            double similarity = calculateSimilarity(response, recent);
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }

        // Lower similarity = higher variety score
        return 1.0 - maxSimilarity;
    }

    private static double calculateSimilarity(String a, String b) {
        String[] wordsA = a.toLowerCase().split("\\s+");
        String[] wordsB = b.toLowerCase().split("\\s+");

        Set<String> setA = new HashSet<>(Arrays.asList(wordsA));
        Set<String> setB = new HashSet<>(Arrays.asList(wordsB));

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private static double scoreAppropriateness(String response, ConversationContext context) {
        // Check if response is appropriate for the situation
        if (ContentModerator.isInappropriate(response)) {
            return 0.0;
        }

        // Check if response length is appropriate
        int length = response.length();
        if (length < 5 || length > 500) {
            return 0.5;
        }

        return 1.0;
    }

    private static double scoreNaturalness(String response) {
        double naturalness = 0.0;

        // Check for natural language patterns
        if (response.matches(".*[.!?]$")) {
            naturalness += 0.3; // Proper punctuation
        }

        if (Character.isUpperCase(response.charAt(0))) {
            naturalness += 0.2; // Proper capitalization
        }

        // Check against robotic patterns
        if (!response.matches("^(Yes|No|Ok)\\.$")) {
            naturalness += 0.3; // Not too robotic
        }

        return Math.min(1.0, naturalness);
    }

    public static class RankedResponse {
        private final String response;
        private final double score;

        public RankedResponse(String response, double score) {
            this.response = response;
            this.score = score;
        }

        public String getResponse() {
            return response;
        }

        public double getScore() {
            return score;
        }
    }
}
```

### Diversity vs Consistency Tradeoff

**Balancing Novelty and Familiarity**

```java
public class DiversityController {

    private final List<String> responseHistory = new ArrayList<>();
    private final Map<String, Integer> phraseUsageCount = new ConcurrentHashMap<>();

    /**
     * Selects response balancing diversity and consistency
     */
    public String selectBalancedResponse(
        List<String> candidates,
        ConversationContext context
    ) {
        double diversityScore = calculateDiversityScore(responseHistory);

        // If diversity is too low, prioritize novel responses
        if (diversityScore < 0.3) {
            return selectNovelResponse(candidates, context);
        }

        // If diversity is too high, prioritize consistent responses
        if (diversityScore > 0.8) {
            return selectConsistentResponse(candidates, context);
        }

        // Otherwise, balance both
        return selectBalanced(candidates, context);
    }

    private double calculateDiversityScore(List<String> responses) {
        if (responses.isEmpty()) {
            return 1.0; // Maximum diversity when no history
        }

        // Calculate average pairwise similarity
        double totalSimilarity = 0.0;
        int comparisons = 0;

        for (int i = 0; i < responses.size(); i++) {
            for (int j = i + 1; j < responses.size(); j++) {
                totalSimilarity += calculateSimilarity(
                    responses.get(i),
                    responses.get(j)
                );
                comparisons++;
            }
        }

        double avgSimilarity = comparisons > 0 ?
            totalSimilarity / comparisons : 0.0;

        // Diversity = 1 - similarity
        return 1.0 - avgSimilarity;
    }

    private String selectNovelResponse(
        List<String> candidates,
        ConversationContext context
    ) {
        // Score candidates by novelty
        return candidates.stream()
            .max(Comparator.comparingDouble(c -> scoreNovelty(c, context)))
            .orElse(null);
    }

    private String selectConsistentResponse(
        List<String> candidates,
        ConversationContext context
    ) {
        // Score candidates by consistency with personality
        return candidates.stream()
            .max(Comparator.comparingDouble(c -> scoreConsistency(c, context)))
            .orElse(null);
    }

    private String selectBalanced(
        List<String> candidates,
        ConversationContext context
    ) {
        // Balance novelty and consistency
        return candidates.stream()
            .max(Comparator.comparingDouble(c ->
                scoreNovelty(c, context) * 0.5 + scoreConsistency(c, context) * 0.5
            ))
            .orElse(null);
    }

    private double scoreNovelty(String candidate, ConversationContext context) {
        double novelty = 1.0;

        // Penalize if similar to recent responses
        for (String recent : responseHistory) {
            double similarity = calculateSimilarity(candidate, recent);
            novelty -= similarity * 0.3;
        }

        // Penalize overused phrases
        for (Map.Entry<String, Integer> entry : phraseUsageCount.entrySet()) {
            if (candidate.toLowerCase().contains(entry.getKey().toLowerCase())) {
                novelty -= Math.min(0.5, entry.getValue() * 0.1);
            }
        }

        return Math.max(0.0, novelty);
    }

    private double scoreConsistency(String candidate, ConversationContext context) {
        double consistency = 0.0;

        // Reward if matches personality
        PersonalityProfile personality = context.getPersonality();

        // Check formality match
        int formality = estimateFormality(candidate);
        int formalityDiff = Math.abs(formality - personality.formality);
        consistency += Math.max(0, 1.0 - (formalityDiff / 100.0));

        // Check humor match
        boolean isHumorous = isHumorousResponse(candidate);
        if ((isHumorous && personality.humor > 60) ||
            (!isHumorous && personality.humor < 40)) {
            consistency += 0.3;
        }

        return Math.min(1.0, consistency);
    }

    public void recordResponse(String response) {
        responseHistory.add(response);

        // Keep only recent history
        if (responseHistory.size() > 50) {
            responseHistory.remove(0);
        }

        // Track phrase usage
        String[] phrases = extractPhrases(response);
        for (String phrase : phrases) {
            phraseUsageCount.merge(phrase, 1, Integer::sum);
        }
    }

    private String[] extractPhrases(String response) {
        // Extract significant phrases (2-4 words)
        String[] words = response.toLowerCase().split("\\s+");
        List<String> phrases = new ArrayList<>();

        for (int i = 0; i < words.length - 1; i++) {
            StringBuilder phrase = new StringBuilder(words[i]);

            for (int j = i + 1; j < Math.min(i + 4, words.length); j++) {
                phrase.append(" ").append(words[j]);
                phrases.add(phrase.toString());
            }
        }

        return phrases.toArray(new String[0]);
    }
}
```

### Memory-Informed Responses

**Episodic Memory Integration**

```java
public class MemoryInformedDialogue {

    /**
     * Generates responses that reference shared memories
     */
    public static String generateMemoryInformedResponse(
        String baseResponse,
        CompanionMemory memory,
        ConversationContext context
    ) {
        // Try to find relevant shared memories
        List<EmotionalMemory> relevantMemories = findRelevantMemories(
            context.getCurrentTopic(),
            memory,
            5
        );

        if (relevantMemories.isEmpty()) {
            return baseResponse;
        }

        // Select most appropriate memory to reference
        EmotionalMemory selectedMemory = selectMemoryForContext(
            relevantMemories,
            context
        );

        if (selectedMemory == null) {
            return baseResponse;
        }

        // Integrate memory reference into response
        return integrateMemoryReference(baseResponse, selectedMemory, context);
    }

    private static List<EmotionalMemory> findRelevantMemories(
        String topic,
        CompanionMemory memory,
        int maxResults
    ) {
        if (topic == null || topic.isEmpty()) {
            return Collections.emptyList();
        }

        List<EmotionalMemory> allMemories = memory.getEmotionalMemories();

        return allMemories.stream()
            .filter(m -> isRelevantToTopic(m, topic))
            .sorted(Comparator.comparing(EmotionalMemory::getSignificance).reversed())
            .limit(maxResults)
            .collect(Collectors.toList());
    }

    private static boolean isRelevantToTopic(EmotionalMemory memory, String topic) {
        String topicLower = topic.toLowerCase();
        String memoryLower = memory.getDescription().toLowerCase();

        return memoryLower.contains(topicLower) ||
               topicLower.contains(memoryLower);
    }

    private static EmotionalMemory selectMemoryForContext(
        List<EmotionalMemory> memories,
        ConversationContext context
    ) {
        // Score memories based on context
        return memories.stream()
            .max(Comparator.comparingDouble(m -> scoreMemoryForContext(m, context)))
            .orElse(null);
    }

    private static double scoreMemoryForContext(
        EmotionalMemory memory,
        ConversationContext context
    ) {
        double score = 0.0;

        // More significant memories get higher scores
        score += memory.getSignificance() / 10.0;

        // More recent memories get slight boost
        long daysSince = ChronoUnit.DAYS.between(
            memory.getTimestamp(),
            Instant.now()
        );
        score += Math.max(0, 1.0 - (daysSince / 30.0));

        // Emotional resonance
        if (memory.getEmotionalWeight() > 5) {
            score += 0.3;
        }

        return score;
    }

    private static String integrateMemoryReference(
        String baseResponse,
        EmotionalMemory memory,
        ConversationContext context
    ) {
        // Generate memory reference templates
        String[] templates = {
            "This reminds me of " + memory.getDescription() + ".",
            "Kind of like when we " + memory.getDescription() + ".",
            "Similar to " + memory.getDescription() + ", if you recall.",
            "Remember when we " + memory.getDescription() + "?"
        };

        // Select template based on rapport
        int rapport = context.getRapportLevel();
        String template;

        if (rapport < 40) {
            // Formal reference
            template = "Similar to our previous experience: " +
                memory.getDescription() + ".";
        } else if (rapport < 70) {
            // Casual reference
            template = templates[0];
        } else {
            // Familiar reference
            template = templates[(int)(Math.random() * templates.length)];
        }

        // Integrate into response
        return baseResponse + " " + template;
    }

    /**
     * Generates callback responses to memorable events
     */
    public static String generateCallbackResponse(
        String topic,
        CompanionMemory memory,
        ConversationContext context
    ) {
        // Find most significant recent memory
        EmotionalMemory memoryToCallback = memory.getEmotionalMemories().stream()
            .filter(m -> m.getSignificance() >= 7)
            .filter(m -> ChronoUnit.DAYS.between(
                m.getTimestamp(),
                Instant.now()
            ) < 7) // Within last 7 days
            .max(Comparator.comparing(EmotionalMemory::getSignificance))
            .orElse(null);

        if (memoryToCallback == null) {
            return null;
        }

        // Generate callback
        return String.format(
            "You know, thinking about %s makes me think of when we %s.",
            topic,
            memoryToCallback.getDescription()
        );
    }
}
```

---

## Case Studies {#case-studies}

### Game Companion Dialogue Systems

**The Elder Scrolls Series (Bethesda)**

**Approach:** Bethesda's companions use a combination of:
- **Condition-based dialogue**: Responses triggered by specific game states
- **Relationship thresholds**: Different dialogue branches at different affinity levels
- **Personality traits**: Each companion has unique traits that affect responses
- **Context awareness**: Comments on locations, enemies, and player actions

**Example Implementation Pattern:**

```java
public class BethesdaStyleCompanion {

    public String generateResponse(GameEvent event, int affinityLevel) {
        // Check conditions
        if (event.getType() == EventType.LOCATION_ENTERED) {
            if (affinityLevel > 70) {
                // High affinity: Personal story
                return getPersonalStoryForLocation(event.getLocation());
            } else if (affinityLevel > 30) {
                // Medium affinity: Observation
                return getObservationForLocation(event.getLocation());
            }
        }

        if (event.getType() == EventType.COMBAT_STARTED) {
            if (getPersonalityTrait() == PersonalityTrait.AGGRESSIVE) {
                return "Time to spill some blood!";
            } else if (getPersonalityTrait() == PersonalityTrait.CAUTIOUS) {
                return "Be careful. This could be dangerous.";
            }
        }

        return null; // No response
    }
}
```

**Key Insights:**
- **Condition trees** allow for granular control
- **Affinity gates** unlock deeper dialogue over time
- **Personality traits** create distinct voices
- **Environmental awareness** makes world feel lived-in

**Mass Effect Series (BioWare)**

**Approach:** Mass Effect's dialogue system features:
- **Dialogue wheels**: Visual selection of response types
- **Paragon/Renegade system**: Moral alignment affects responses
- **Investigation**: Deep dialogue trees for lore and backstory
- **Inter-party banter**: Companions talk to each other

**Example Implementation Pattern:**

```java
public class MassEffectStyleDialogue {

    public enum DialogueTone {
        PARAGON,        // Diplomatic, heroic
        RENEGADE,       // Aggressive, pragmatic
        NEUTRAL,        // Informational
        INVESTIGATIVE   // Asking questions
    }

    public String generateResponse(
        String topic,
        DialogueTone tone,
        int moralityScore
    ) {
        // Adjust tone based on morality score
        if (moralityScore > 70) {
            // Paragon-leaning
            if (tone == DialogueTone.RENEGADE) {
                tone = DialogueTone.NEUTRAL; // Soften renegade choices
            }
        } else if (moralityScore < 30) {
            // Renegade-leaning
            if (tone == DialogueTone.PARAGON) {
                tone = DialogueTone.NEUTRAL; // Soften paragon choices
            }
        }

        // Generate response based on tone
        return switch (tone) {
            case PARAGON -> generateParagonResponse(topic);
            case RENEGADE -> generateRenegadeResponse(topic);
            case NEUTRAL -> generateNeutralResponse(topic);
            case INVESTIGATIVE -> generateInvestigativeResponse(topic);
        };
    }

    public void triggerInterPartyBanter(Companion companion1, Companion companion2) {
        // Check if companions have banter for this context
        String banter = getBanter(companion1, companion2, getCurrentContext());

        if (banter != null) {
            companion1.say(banter.getFirstLine());
            companion2.say(banter.getSecondLine());
        }
    }
}
```

**Key Insights:**
- **Tone selection** gives player agency
- **Moral consistency** reinforces character development
- **Investigation mode** separates action from information
- **Inter-party dynamics** create rich social world

**Red Dead Redemption 2 (Rockstar)**

**Approach:** RDR2 features:
- **Dynamic greeting system**: Different greetings based on context
- **Honor system**: Dialogue changes based on player honor
- **Camp conversations**: NPCs have ambient conversations
- **World context awareness**: Comments on weather, news, events

**Example Implementation Pattern:**

```java
public class RDR2StyleDialogue {

    public String generateGreeting(
        NPC npc,
        Player player,
        GameContext context
    ) {
        // Get player honor
        int honor = player.getHonorLevel();

        // Get recent player actions
        List<PlayerAction> recentActions = player.getRecentActions(5);

        // Generate greeting based on honor and recent actions
        if (honor > 70) {
            // High honor: Respectful greeting
            if (recentActions.contains(PlayerAction.HELPED_SOMEONE)) {
                return "You're a good man, " + player.getName() + ".";
            } else {
                return "Good to see you, " + player.getName() + ".";
            }
        } else if (honor < 30) {
            // Low honor: Fearful or confrontational
            if (recentActions.contains(PlayerAction.COMMITTED_CRIME)) {
                return "I've got no quarrel with you. Stay away.";
            } else {
                return "What do you want?";
            }
        } else {
            // Neutral honor
            return "Howdy, " + player.getName() + ".";
        }
    }

    public void generateCampConversation(
        List<NPC> campNPCs,
        GameContext context
    ) {
        // Select two random NPCs
        NPC speaker1 = campNPCs.get(ThreadLocalRandom.current().nextInt(campNPCs.size()));
        NPC speaker2 = campNPCs.get(ThreadLocalRandom.current().nextInt(campNPCs.size()));

        if (speaker1 == speaker2) {
            return;
        }

        // Get conversation topic based on context
        String topic = selectConversationTopic(context);

        // Generate dialogue
        String line1 = generateCampLine(speaker1, topic, true);
        String line2 = generateCampLine(speaker2, topic, false);

        // Speak dialogue
        speaker1.say(line1);

        // Pause before response
        scheduler.schedule(() -> speaker2.say(line2), 2, TimeUnit.SECONDS);
    }
}
```

**Key Insights:**
- **Context-aware greetings** create immersion
- **Reputation systems** affect NPC responses
- **Ambient conversations** bring world to life
- **Environmental awareness** makes world feel responsive

### AI Dungeon Master Patterns

**AI Dungeon (Latitude.io)**

**Approach:** AI Dungeon uses:
- **LLM-driven generation**: All content generated by AI
- **Context management**: Maintains story context across turns
- **Action interpretation**: Interprets player actions as story beats
- **World coherence**: Attempts to maintain narrative consistency

**Example Implementation Pattern:**

```java
public class AIDungeonStyleNarrator {

    private final ConversationWindowManager contextWindow;
    private final AsyncLLMClient llmClient;

    public CompletableFuture<String> generateNarrativeResponse(
        String playerAction,
        GameWorld world
    ) {
        // Build context
        String context = buildNarrativeContext(playerAction, world);

        // Generate response
        String prompt = String.format("""
            You are a dungeon master narrating an interactive story.

            Current situation:
            %s

            Player action: %s

            Generate a narrative response that:
            1. Acknowledges the player's action
            2. Describes the consequences
            3. Introduces new plot elements if appropriate
            4. Maintains narrative consistency
            5. Keeps the story engaging

            Response (2-4 sentences):
            """,
            context,
            playerAction
        );

        Map<String, Object> params = Map.of(
            "maxTokens", 200,
            "temperature", 0.8,
            "systemPrompt", buildSystemPrompt(world)
        );

        return llmClient.sendAsync(prompt, params)
            .thenApply(LLMResponse::getContent);
    }

    private String buildNarrativeContext(String playerAction, GameWorld world) {
        StringBuilder context = new StringBuilder();

        // Current location
        context.append("Location: ").append(world.getCurrentLocation()).append("\n");

        // Nearby entities
        context.append("Nearby: ").append(world.getNearbyEntities()).append("\n");

        // Recent events
        context.append("Recent events:\n");
        for (GameEvent event : world.getRecentEvents(5)) {
            context.append("- ").append(event.getDescription()).append("\n");
        }

        // Player state
        context.append("Player status: ")
            .append(world.getPlayerStatus())
            .append("\n");

        return context.toString();
    }
}
```

**Key Insights:**
- **LLM flexibility** enables infinite content
- **Context management** critical for coherence
- **Temperature control** balances creativity and consistency
- **System prompts** establish narrative boundaries

### Character.AI Approach

**Character.AI Techniques**

**Approach:** Character.AI uses:
- **Character definition cards**: Structured personality descriptions
- **Example dialogue**: Few-shot examples for voice consistency
- **Memory systems**: Long-term conversation tracking
- **Relationship modeling**: Dynamic relationship states

**Example Implementation Pattern:**

```java
public class CharacterAIStyleCompanion {

    public static class CharacterCard {
        private String name;
        private String description;
        private String personality;
        private String greeting;
        private List<String> exampleDialogues;
        private String scenario;

        public String toPrompt() {
            StringBuilder prompt = new StringBuilder();

            prompt.append("Character: ").append(name).append("\n");
            prompt.append(description).append("\n");
            prompt.append("Personality: ").append(personality).append("\n");
            prompt.append("Scenario: ").append(scenario).append("\n");
            prompt.append("\n");
            prompt.append("Example dialogues:\n");

            for (String example : exampleDialogues) {
                prompt.append("<START>\n");
                prompt.append(example);
                prompt.append("\n<END>\n");
            }

            return prompt.toString();
        }
    }

    public CompletableFuture<String> generateResponse(
        CharacterCard character,
        String userMessage,
        ConversationHistory history
    ) {
        // Build prompt with character card
        String characterContext = character.toPrompt();

        // Add conversation history
        String conversationContext = history.formatForLLM();

        // Build full prompt
        String prompt = String.format("""
            %s

            Conversation history:
            %s

            User: %s

            Generate a response in character that:
            1. Maintains the established personality
            2. Responds naturally to the user's message
            3. References conversation history when appropriate
            4. Keeps responses brief (1-3 sentences)

            Response:
            """,
            characterContext,
            conversationContext,
            userMessage
        );

        Map<String, Object> params = Map.of(
            "maxTokens", 150,
            "temperature", 0.8,
            "stop", Arrays.asList("<END>", "User:")
        );

        return llmClient.sendAsync(prompt, params)
            .thenApply(LLMResponse::getContent);
    }
}
```

**Key Insights:**
- **Character cards** provide structured personality
- **Example dialogues** establish voice through few-shot learning
- **Conversation history** maintains continuity
- **Stop sequences** prevent LLM from generating user dialogue

---

## Application to MineWright {#application-to-minewright}

### Enhancing Foreman Dialogue

**Current State:**

The existing `ProactiveDialogueManager` and `ConversationManager` provide:
- Event-based commentary triggers
- Relationship-aware dialogue selection
- Personality-driven response generation
- LLM-powered conversational responses

**Enhancement Opportunities:**

1. **Deeper Personality Integration**
   - Current: Personality profiles exist but could be more deeply integrated
   - Enhancement: Apply OCEAN traits to every dialogue generation step

2. **Memory-Informed Conversations**
   - Current: Emotional memory system exists
   - Enhancement: Proactively reference shared experiences in dialogue

3. **Multi-Agent Banter**
   - Current: Agents can communicate but don't have casual conversations
   - Enhancement: Add agent-to-agent banter system

**Implementation: Enhanced Foreman Dialogue**

```java
public class EnhancedForemanDialogue {

    private final ForemanEntity foreman;
    private final CompanionMemory memory;
    private final ProactiveDialogueManager proactiveManager;
    private final EmotionalStateTracker emotionalTracker;

    /**
     * Generates a response that integrates personality, emotion, and memory
     */
    public CompletableFuture<String> generateEnhancedResponse(
        String playerMessage,
        AsyncLLMClient llmClient
    ) {
        // Get personality profile
        PersonalityProfile personality = memory.getPersonality();

        // Get current emotional state
        EmotionalState currentEmotion = emotionalTracker.getCurrentState();

        // Find relevant memories
        List<EmotionalMemory> relevantMemories = findRelevantMemories(
            extractTopic(playerMessage),
            3
        );

        // Build enhanced prompt
        String prompt = buildEnhancedPrompt(
            playerMessage,
            personality,
            currentEmotion,
            relevantMemories
        );

        Map<String, Object> params = Map.of(
            "maxTokens", 200,
            "temperature", calculateTemperature(personality, currentEmotion),
            "systemPrompt", buildEnhancedSystemPrompt(personality, currentEmotion)
        );

        return llmClient.sendAsync(prompt, params)
            .thenApply(response -> {
                String responseText = response.getContent().trim();

                // Post-process for personality
                responseText = PersonalityDialogueEngine.applyPersonality(
                    responseText,
                    new PersonalityTraits(
                        personality.openness,
                        personality.conscientiousness,
                        personality.extraversion,
                        personality.agreeableness,
                        personality.neuroticism
                    ),
                    new ConversationContext(memory, foreman)
                );

                // Post-process for emotion
                responseText = emotionalTracker.generateEmotionalDialogue(
                    responseText,
                    currentEmotion
                );

                // Post-process for rapport
                responseText = RelationshipAwareDialogue.adaptForRapport(
                    responseText,
                    memory.getRapportLevel(),
                    personality
                );

                return responseText;
            });
    }

    private String buildEnhancedPrompt(
        String playerMessage,
        PersonalityProfile personality,
        EmotionalState emotion,
        List<EmotionalMemory> memories
    ) {
        StringBuilder prompt = new StringBuilder();

        // Add memory context
        if (!memories.isEmpty()) {
            prompt.append("Relevant shared experiences:\n");
            for (EmotionalMemory memory : memories) {
                prompt.append("- ").append(memory.getDescription()).append("\n");
            }
            prompt.append("\n");
        }

        // Add emotional context
        prompt.append("Current emotional state: ").append(emotion).append("\n");
        prompt.append("Mood affects how you express yourself.\n\n");

        // Add rapport context
        prompt.append("Relationship level: ")
            .append(memory.getRapportLevel())
            .append("/100\n");
        prompt.append("This affects your formality and warmth.\n\n");

        // Add player message
        prompt.append("Player says: ").append(playerMessage).append("\n");
        prompt.append("\nGenerate a response that:\n");
        prompt.append("1. Stays in character (personality traits above)\n");
        prompt.append("2. Expresses your current emotional state\n");
        prompt.append("3. References shared experiences if natural\n");
        prompt.append("4. Matches formality to relationship level\n");
        prompt.append("5. Keeps response brief (1-3 sentences)\n\n");
        prompt.append("Response:");

        return prompt.toString();
    }

    private double calculateTemperature(PersonalityProfile personality, EmotionalState emotion) {
        double baseTemperature = 0.7;

        // Adjust based on openness (more open = more creative)
        baseTemperature += (personality.openness - 50) / 200.0;

        // Adjust based on emotion
        if (emotion == EmotionalState.EXCITED) {
            baseTemperature += 0.1;
        } else if (emotion == EmotionalState.CALM) {
            baseTemperature -= 0.1;
        }

        return Math.max(0.0, Math.min(1.0, baseTemperature));
    }
}
```

### Worker Banter System

**Design: Worker-to-Worker Conversations**

Workers should have casual conversations while working, creating a lively atmosphere:

```java
public class WorkerBanterSystem {

    private final List<ForemanEntity> workers;
    private final Map<String, PersonalityProfile> personalities;
    private final BanterSchedule banterSchedule;

    /**
     * Triggers banter between workers
     */
    public void triggerBanter() {
        if (!banterSchedule.shouldTrigger()) {
            return;
        }

        // Select two random workers
        List<ForemanEntity> available = workers.stream()
            .filter(w -> !w.getActionExecutor().isCriticallyBusy())
            .collect(Collectors.toList());

        if (available.size() < 2) {
            return;
        }

        Collections.shuffle(available);
        ForemanEntity worker1 = available.get(0);
        ForemanEntity worker2 = available.get(1);

        // Select topic
        String topic = selectBanterTopic();

        // Generate dialogue
        generateBanterDialogue(worker1, worker2, topic);
    }

    private String selectBanterTopic() {
        String[] topics = {
            "work_progress",
            "player_actions",
            "recent_discovery",
            "personal_experience",
            "building_techniques",
            "ambient_observation"
        };

        return topics[ThreadLocalRandom.current().nextInt(topics.length)];
    }

    private void generateBanterDialogue(
        ForemanEntity worker1,
        ForemanEntity worker2,
        String topic
    ) {
        PersonalityProfile personality1 = personalities.get(worker1.getEntityName());
        PersonalityProfile personality2 = personalities.get(worker2.getEntityName());

        // Generate first worker's line
        String line1 = generateBanterLine(worker1, personality1, topic, true);
        worker1.sendChatMessage(line1);

        // Delay before response
        scheduler.schedule(() -> {
            // Generate second worker's line
            String line2 = generateBanterLine(worker2, personality2, topic, false);
            worker2.sendChatMessage(line2);
        }, 2, TimeUnit.SECONDS);
    }

    private String generateBanterLine(
        ForemanEntity worker,
        PersonalityProfile personality,
        String topic,
        boolean isInitiator
    ) {
        // Get topic-specific templates
        List<String> templates = getBanterTemplates(topic, isInitiator);

        // Select template based on personality
        String template = templates.stream()
            .filter(t -> matchesPersonality(t, personality))
            .findFirst()
            .orElse(templates.get(0));

        // Fill template with context
        return fillTemplate(template, worker, topic);
    }

    private List<String> getBanterTemplates(String topic, boolean isInitiator) {
        return switch (topic) {
            case "work_progress" -> isInitiator ? List.of(
                "How's your section coming along?",
                "Making good progress over here.",
                "Think we'll finish on time?",
                "This is going smoother than I expected."
            ) : List.of(
                "About halfway done.",
                "Could be better, could be worse.",
                "Running ahead of schedule actually!",
                "Don't jinx it!"
            );

            case "player_actions" -> isInitiator ? List.of(
                "Did you see what the player just did?",
                "Player's in a weird mood today.",
                "Think they noticed my work?",
                "Wonder what they're planning next."
            ) : List.of(
                "Yeah, that was something!",
                "I try not to question the player.",
                "They definitely noticed. Good work!",
                "Probably something big knowing them."
            );

            case "recent_discovery" -> isInitiator ? List.of(
                "Found something interesting earlier.",
                "You won't believe what I saw.",
                "Think I found a good spot for materials.",
                "There's something weird over here..."
            ) : List.of(
                "Oh? What did you find?",
                "Do tell!",
                "Nice! We'll check it out later.",
                "Weird how?"
            );

            default -> List.of(
                "*continues working*",
                "*nods*",
                "Yep.",
                "Right then."
            );
        };
    }

    private boolean matchesPersonality(String template, PersonalityProfile personality) {
        // Personality filtering
        if (personality.extraversion < 30 && template.contains("!")) {
            return false;
        }

        if (personality.humor > 70 && template.contains("jinx")) {
            return true; // Humorous personalities like jokes
        }

        return true;
    }

    private String fillTemplate(String template, ForemanEntity worker, String topic) {
        // Replace placeholders with context
        template = template.replace("{worker}", worker.getEntityName());

        // Add activity context
        String activity = worker.getActionExecutor().getCurrentActivity();
        if (activity != null) {
            template = template.replace("{activity}", activity);
        }

        return template;
    }
}
```

### Context-Aware Responses

**Design: Environmental Awareness**

Workers should comment on their environment contextually:

```java
public class ContextAwareDialogue {

    /**
     * Generates context-aware comments about the environment
     */
    public String generateContextualComment(
        ForemanEntity worker,
        GameContext context
    ) {
        // Check biome
        Biome currentBiome = context.getBiome();
        if (shouldCommentOnBiome(currentBiome)) {
            return generateBiomeComment(worker, currentBiome);
        }

        // Check weather
        Weather weather = context.getWeather();
        if (shouldCommentOnWeather(weather)) {
            return generateWeatherComment(worker, weather);
        }

        // Check time of day
        GameTime time = context.getTimeOfDay();
        if (shouldCommentOnTime(time)) {
            return generateTimeComment(worker, time);
        }

        // Check nearby structures
        List<Structure> nearby = context.getNearbyStructures();
        if (!nearby.isEmpty() && shouldCommentOnStructures()) {
            return generateStructureComment(worker, nearby);
        }

        return null;
    }

    private boolean shouldCommentOnBiome(Biome biome) {
        // Comment on notable biomes
        return biome == Biome.NETHER ||
               biome == Biome.THE_END ||
               biome == Biome.DESERT ||
               biome == Biome.SNOWY_TUNDRA;
    }

    private String generateBiomeComment(ForemanEntity worker, Biome biome) {
        PersonalityProfile personality = worker.getCompanionMemory().getPersonality();

        return switch (biome) {
            case NETHER -> personality.openness > 70 ?
                "Fascinating environment here. So... hot." :
                "I don't like it here. Let's work fast.";

            case THE_END -> personality.neuroticism > 50 ?
                "This place gives me the creeps..." :
                "Alien sky, alien blocks. Interesting.";

            case DESERT -> personality.humor > 60 ?
                "Great view, if you like sand." :
                "Hot and dry. Perfect for staying hydrated.";

            case SNOWY_TUNDRA -> personality.extraversion > 60 ?
                "Winter wonderland! Let's build a snow fort!" :
                "Cold, but refreshing. Good working weather.";

            default -> null;
        };
    }

    private boolean shouldCommentOnWeather(Weather weather) {
        // Comment on extreme weather
        return weather == Weather.THUNDERSTORM ||
               weather == Weather.RAIN;
    }

    private String generateWeatherComment(ForemanEntity worker, Weather weather) {
        PersonalityProfile personality = worker.getCompanionMemory().getPersonality();

        if (weather == Weather.THUNDERSTORM) {
            if (personality.neuroticism > 60) {
                return "Lightning! We should probably take cover.";
            } else {
                return "Quite a storm! Nature's fury.";
            }
        }

        if (weather == Weather.RAIN) {
            if (personality.openness > 60) {
                return "Rain's nice. Everything's getting washed clean.";
            } else {
                return "Perfect weather for underground work.";
            }
        }

        return null;
    }

    private boolean shouldCommentOnTime(GameTime time) {
        // Comment on dawn and dusk
        return time == GameTime.DAWN || time == GameTime.DUSK;
    }

    private String generateTimeComment(ForemanEntity worker, GameTime time) {
        PersonalityProfile personality = worker.getCompanionMemory().getPersonality();

        if (time == GameTime.DAWN) {
            if (personality.extraversion > 60) {
                return "Morning! Ready to start fresh!";
            } else {
                return "Early start. Let's make it productive.";
            }
        }

        if (time == GameTime.DUSK) {
            if (personality.neuroticism > 50) {
                return "Getting dark. Hostiles will spawn soon.";
            } else {
                return "Day's wrapping up. Good progress today.";
            }
        }

        return null;
    }

    private boolean shouldCommentOnStructures() {
        // 5% chance to comment on nearby structures
        return Math.random() < 0.05;
    }

    private String generateStructureComment(ForemanEntity worker, List<Structure> structures) {
        Structure mostInteresting = structures.stream()
            .max(Comparator.comparing(s -> s.getComplexity() + s.getAestheticValue()))
            .orElse(null);

        if (mostInteresting == null) {
            return null;
        }

        PersonalityProfile personality = worker.getCompanionMemory().getPersonality();

        if (personality.openness > 70) {
            return String.format("That %s is quite impressive!",
                mostInteresting.getType());
        } else {
            return String.format("Noticed the %s nearby.",
                mostInteresting.getType());
        }
    }
}
```

---

## Implementation Recommendations {#implementation-recommendations}

### Phase 1: Foundation (Week 1-2)

**Tasks:**

1. **Create dialogue enhancement infrastructure**
   ```java
   // New classes to create:
   - src/main/java/com/minewright/dialogue/EnhancedForemanDialogue.java
   - src/main/java/com/minewright/dialogue/PersonalityDialogueEngine.java
   - src/main/java/com/minewright/dialogue/EmotionalStateTracker.java
   - src/main/java/com/minewright/dialogue/DiversityController.java
   ```

2. **Integrate with existing systems**
   - Hook into `ProactiveDialogueManager`
   - Connect to `ConversationManager`
   - Link to `CompanionMemory`

3. **Add configuration**
   ```toml
   # config/steve-common.toml additions
   [dialogue.enhancement]
   enabled = true
   personality_injection = true
   emotional_awareness = true
   memory_references = true
   diversity_control = true
   max_response_length = 200
   temperature_base = 0.7
   ```

### Phase 2: Personality & Emotion (Week 2-3)

**Tasks:**

4. **Implement personality injection**
   - Apply OCEAN traits to all dialogue
   - Add archetype-based dialogue templates
   - Create catchphrase generation system

5. **Add emotional state tracking**
   - Track emotional state transitions
   - Generate emotion-appropriate dialogue
   - Add emotional context to prompts

6. **Create relationship-aware responses**
   - Adapt dialogue based on rapport
   - Generate rapport-appropriate greetings
   - Scale formality with relationship

### Phase 3: Worker Banter (Week 3-4)

**Tasks:**

7. **Implement worker banter system**
   ```java
   // New classes to create:
   - src/main/java/com/minewright/dialogue/WorkerBanterSystem.java
   - src/main/java/com/minewright/dialogue/BanterSchedule.java
   - src/main/java/com/minewright/dialogue/BanterTemplates.java
   ```

8. **Add banter topics**
   - Work progress banter
   - Player action commentary
   - Discovery announcements
   - Building technique discussions

9. **Integrate with agent communication**
   - Use `AgentCommunicationBus` for coordination
   - Add banter to message types
   - Implement shared conversation context

### Phase 4: Context Awareness (Week 4-5)

**Tasks:**

10. **Implement context-aware responses**
    ```java
    // New classes to create:
    - src/main/java/com/minewright/dialogue/ContextAwareDialogue.java
    - src/main/java/com/minewright/dialogue/EnvironmentalCommentary.java
    ```

11. **Add environmental awareness**
    - Biome-based comments
    - Weather reactions
    - Time-of-day greetings
    - Structure observations

12. **Add memory-informed dialogue**
    - Reference shared experiences
    - Generate callback responses
    - Integrate episodic memory

### Phase 5: Polish & Testing (Week 5-6)

**Tasks:**

13. **Create comprehensive tests**
    ```java
    // Test classes to create:
    - src/test/java/com/minewright/dialogue/EnhancedForemanDialogueTest.java
    - src/test/java/com/minewright/dialogue/PersonalityDialogueEngineTest.java
    - src/test/java/com/minewright/dialogue/EmotionalStateTrackerTest.java
    - src/test/java/com/minewright/dialogue/WorkerBanterSystemTest.java
    ```

14. **Performance optimization**
    - Cache LLM responses
    - Optimize prompt construction
    - Add response pooling

15. **Documentation**
    - Update CLAUDE.md
    - Create dialogue design guide
    - Add configuration documentation

---

## Conclusion

AI dialogue generation for companion characters requires a multi-layered approach:

1. **Technical Foundation**: Robust prompt engineering, context window management, and response caching
2. **Character Voice**: Personality injection, catchphrase generation, and emotional state tracking
3. **Quality Control**: Content filtering, response ranking, and diversity balancing
4. **Memory Integration**: Episodic memory referencing, callback generation, and relationship awareness
5. **Context Awareness**: Environmental commentary, situation-appropriate responses, and multi-agent banter

The MineWright codebase already has strong foundations in `ProactiveDialogueManager`, `ConversationManager`, and `CompanionMemory`. The enhancements outlined in this document build on these foundations to create richer, more immersive dialogue experiences.

**Key Innovation Areas:**

1. **OCEAN-Based Personality Injection**: Scientific personality model applied to dialogue generation
2. **Emotional State Tracking**: Dynamic dialogue based on emotional context
3. **Memory-Informed Responses**: Proactive referencing of shared experiences
4. **Worker Banter System**: Multi-agent conversations for livelier world
5. **Context Awareness**: Environmental commentary for immersion

These techniques, drawn from successful game companions (Mass Effect, RDR2, AI Dungeon) and adapted for MineWright's construction AI context, will transform Steve AI agents from functional workers into memorable characters.

---

**Document Version:** 1.0
**Created:** 2026-03-02
**Author:** Claude (Orchestrator Mode)
**Status:** Research Complete - Ready for Implementation
