# Player Preference Learning and Adaptation for Steve AI

**Research Document**
**Date:** 2026-02-28
**Status:** Implementation Planning
**Version:** 1.0

---

## Executive Summary

This document provides a comprehensive research framework for implementing player preference learning and adaptive behavior in the Steve AI mod (MineWright). We explore practical approaches for learning from both explicit and implicit feedback signals, with implementation strategies tailored to the existing architecture.

### Key Findings

1. **Multi-Modal Preference Signals**: Combine explicit feedback (corrections, commands) with implicit signals (building patterns, timing, communication style)
2. **Hybrid Learning Approach**: Bayesian preference modeling for initial learning + collaborative filtering for cross-player insights
3. **Incremental Adaptation**: Start with simple frequency-based features, evolve to sophisticated embedding-based models
4. **Privacy-First Design**: Store only aggregated preferences, allow full player control and transparency

---

## 1. Preference Signals: What Can We Learn?

### 1.1 Explicit Feedback Signals

**Direct Commands and Corrections**
```java
// Player explicitly tells Steve what to do
"Build a modern house" → Preference for modern architecture
"Use oak planks instead" → Material preference
"Make it bigger" → Scale preference
"I don't like that style" → Negative preference signal
```

**Positive/Negative Feedback**
- Commands like "Good job!" or "That's perfect"
- Corrections: "No, build it this way"
- Emotional markers in chat: enthusiasm, frustration

**Implementation Approach:**
```java
public class ExplicitFeedbackCollector {

    public void recordCorrection(String originalAction, String correctedAction) {
        // Store as negative preference for original
        updatePreference("architecture", originalAction, -0.3f);

        // Store as positive preference for correction
        updatePreference("architecture", correctedAction, 0.5f);

        // Record in episodic memory
        memory.recordExperience("correction",
            String.format("Preferred %s over %s", correctedAction, originalAction),
            3);
    }

    public void recordPraise(String taskType) {
        // Strengthen preference for this type of task
        updatePreference("task_preference", taskType, 0.2f);
    }
}
```

### 1.2 Implicit Feedback Signals

**Building Style Analysis**
```java
// Extract features from what player builds themselves
public class BuildingStyleAnalyzer {

    public void analyzePlayerBuild(BlockPos center, int radius) {
        Map<String, Integer> materials = countMaterials(center, radius);
        String dominantStyle = classifyBuildStyle(materials);

        // Learn material preferences
        materials.forEach((block, count) -> {
            float preference = count / (float) totalBlocks;
            updatePreference("material", block, preference * 0.1f);
        });

        // Learn style preference
        updatePreference("architecture", dominantStyle, 0.2f);
    }
}
```

**Temporal Patterns**
```java
// When does the player play? How long?
public class TemporalPreferenceTracker {

    public void recordSession(long startTime, long duration) {
        // Time of day preference
        int hour = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).getHour();
        String timeSlot = getTimeSlot(hour); // "morning", "afternoon", "evening", "night"
        updatePreference("play_time", timeSlot, 0.05f);

        // Session length preference
        if (duration > TWO_HOURS) {
            updatePreference("play_style", "extended_sessions", 0.1f);
        } else if (duration < THIRTY_MINUTES) {
            updatePreference("play_style", "quick_sessions", 0.1f);
        }
    }
}
```

**Communication Style Analysis**
```java
// How does the player communicate?
public class CommunicationStyleAnalyzer {

    public void analyzeMessage(String message) {
        // Formality
        boolean usesContractions = message.contains("'");
        boolean usesGreetings = message.matches("^(hi|hello|hey).*");
        boolean usesEmojis = message.matches(".*[😀-🙏].*");

        if (usesContractions) {
            updatePreference("communication", "casual", 0.1f);
        }

        // Brevity vs verbosity
        int wordCount = message.split("\\s+").length;
        if (wordCount < 5) {
            updatePreference("communication", "concise", 0.1f);
        } else if (wordCount > 15) {
            updatePreference("communication", "detailed", 0.1f);
        }
    }
}
```

**Action Selection Patterns**
```java
// What tasks does the player request most?
public class TaskPreferenceTracker {

    public void recordTaskRequest(String taskType, Map<String, Object> params) {
        // Track frequency
        taskFrequency.merge(taskType, 1, Integer::sum);

        // If player keeps requesting similar structures
        if (taskType.equals("build")) {
            String structure = (String) params.get("structure");
            updatePreference("build_structure", structure, 0.15f);
        }

        // Mining preferences
        if (taskType.equals("mine")) {
            String material = (String) params.get("block");
            updatePreference("mine_target", material, 0.1f);
        }
    }
}
```

### 1.3 Preference Categories

| Category | Signals | Storage | Update Frequency |
|----------|---------|---------|------------------|
| **Architecture Style** | Build commands, corrections, player builds | Semantic memory | Per build |
| **Material Preferences** | Block choices, material corrections | Semantic memory | Per build |
| **Task Preferences** | Command frequency, task success/failure | Semantic memory | Per command |
| **Communication Style** | Message length, formality, emoji use | Semantic memory | Per message |
| **Play Schedule** | Session times, duration | Semantic memory | Per session |
| **Personality Matching** | Response to agent personalities | Semantic memory | Per interaction |
| **Pacing** | Task complexity choices, interruption frequency | Semantic memory | Per task |

---

## 2. Learning Approaches

### 2.1 Frequency-Based Learning (Immediate Implementation)

**Pros:** Simple, transparent, explainable, works with sparse data
**Cons:** Limited nuance, cold start problem

**Implementation:**
```java
public class FrequencyBasedPreferenceModel {

    private final Map<String, Map<String, Float>> preferences;
    private final Random random = new Random();

    /**
     * Gets a preference value with Bayesian smoothing
     */
    public float getPreference(String category, String key, float defaultValue) {
        Map<String, Float> categoryPrefs = preferences.get(category);
        if (categoryPrefs == null) return defaultValue;

        // Bayesian smoothing: combine observed data with prior
        float observed = categoryPrefs.getOrDefault(key, 0f);
        int count = observationCount.getOrDefault(category + ":" + key, 0);

        // Simple Bayesian estimate
        float prior = defaultValue;
        float priorWeight = 2f; // Pseudo-count for prior
        float posteriorMean = (observed * count + prior * priorWeight) / (count + priorWeight);

        return posteriorMean;
    }

    /**
     * Updates a preference with exponential decay for recency
     */
    public void updatePreference(String category, String key, float delta) {
        preferences.computeIfAbsent(category, k -> new ConcurrentHashMap<>())
                  .merge(key, delta, Float::sum);

        // Cap preferences at [-1, 1]
        float value = preferences.get(category).get(key);
        if (value > 1f) preferences.get(category).put(key, 1f);
        if (value < -1f) preferences.get(category).put(key, -1f);
    }

    /**
     * Samples from preference distribution (for exploration)
     */
    public String samplePreference(String category) {
        Map<String, Float> categoryPrefs = preferences.get(category);
        if (categoryPrefs == null || categoryPrefs.isEmpty()) {
            return null;
        }

        // Softmax sampling for exploration
        List<String> keys = new ArrayList<>(categoryPrefs.keySet());
        float[] probabilities = new float[keys.size()];
        float sum = 0f;

        for (int i = 0; i < keys.size(); i++) {
            // Temperature = 2.0 for exploration
            probabilities[i] = (float) Math.exp(categoryPrefs.get(keys.get(i)) / 2.0);
            sum += probabilities[i];
        }

        float r = random.nextFloat() * sum;
        float cumulative = 0f;

        for (int i = 0; i < keys.size(); i++) {
            cumulative += probabilities[i];
            if (r <= cumulative) {
                return keys.get(i);
            }
        }

        return keys.get(keys.size() - 1);
    }
}
```

**Integration with Existing System:**
```java
// Add to CompanionMemory
public class CompanionMemory {

    private final FrequencyBasedPreferenceModel preferenceModel;

    public void learnPlayerFact(String category, String key, Object value) {
        // Store in semantic memory
        semanticMemories.put(category + ":" + key, new SemanticMemory(...));

        // Update preference model
        if (value instanceof Float) {
            preferenceModel.updatePreference(category, key, (Float) value);
        }
    }

    public float getPlayerPreference(String category, String key, float defaultValue) {
        return preferenceModel.getPreference(category, key, defaultValue);
    }
}
```

### 2.2 Bayesian Preference Modeling (Medium-Term)

**Based on research from Bayesian cognitive models and user intention prediction.**

**Key Insight:** Treat preferences as hidden variables, observe actions as evidence.

```java
public class BayesianPreferenceModel {

    /**
     * Represents a probability distribution over preferences
     */
    public static class PreferenceDistribution {
        private final float[] probabilities; // e.g., [0.1, 0.3, 0.4, 0.2] for 4 options
        private final String[] options;
        private final float priorWeight;

        public PreferenceDistribution(String[] options, float priorWeight) {
            this.options = options;
            this.probabilities = new float[options.length];
            this.priorWeight = priorWeight;

            // Initialize with uniform prior
            Arrays.fill(this.probabilities, 1f / options.length);
        }

        /**
         * Bayesian update: P(H|E) = P(E|H) * P(H) / P(E)
         */
        public void update(String evidence, float likelihood) {
            int index = indexOf(evidence);
            if (index == -1) return;

            // Update posterior
            float prior = probabilities[index];
            float posterior = likelihood * prior;

            // Renormalize (P(E) calculation)
            float sum = 0f;
            for (int i = 0; i < probabilities.length; i++) {
                if (i == index) {
                    probabilities[i] = posterior;
                } else {
                    // Decay others slightly
                    probabilities[i] *= (1f - 0.1f);
                }
                sum += probabilities[i];
            }

            // Normalize
            for (int i = 0; i < probabilities.length; i++) {
                probabilities[i] /= sum;
            }
        }

        /**
         * Sample from distribution (Thompson sampling)
         */
        public String sample() {
            float r = random.nextFloat();
            float cumulative = 0f;

            for (int i = 0; i < probabilities.length; i++) {
                cumulative += probabilities[i];
                if (r <= cumulative) {
                    return options[i];
                }
            }

            return options[options.length - 1];
        }

        /**
         * Get MAP estimate (most likely preference)
         */
        public String getMostLikely() {
            int maxIndex = 0;
            for (int i = 1; i < probabilities.length; i++) {
                if (probabilities[i] > probabilities[maxIndex]) {
                    maxIndex = i;
                }
            }
            return options[maxIndex];
        }
    }

    // Example: Architecture style preference
    private final Map<String, PreferenceDistribution> preferenceDistributions;

    public void recordBuildChoice(String chosenStyle) {
        PreferenceDistribution dist = preferenceDistributions.get("architecture");
        if (dist == null) {
            dist = new PreferenceDistribution(
                new String[]{"modern", "medieval", "rustic", "futuristic"},
                2.0f // Prior weight
            );
            preferenceDistributions.put("architecture", dist);
        }

        // Likelihood: how much evidence does this choice provide?
        // Positive choice = strong evidence
        dist.update(chosenStyle, 2.5f);
    }

    public void recordBuildRejection(String rejectedStyle) {
        PreferenceDistribution dist = preferenceDistributions.get("architecture");
        if (dist == null) return;

        // Negative choice = weak evidence
        dist.update(rejectedStyle, 0.3f);
    }
}
```

### 2.3 Collaborative Filtering (Long-Term)

**For discovering patterns across multiple players.**

**Research-Backed Approach:** Use matrix factorization or neural collaborative filtering as seen in modern recommendation systems.

```java
public class CollaborativePreferenceLearner {

    /**
     * Player-Preference Matrix
     * Rows: Players
     * Cols: Preferences (e.g., "architecture:modern", "material:oak")
     * Values: Preference scores
     */
    private final Map<String, float[]> playerEmbeddings;
    private final Map<String, float[]> preferenceEmbeddings;

    /**
     * Find similar players based on preferences
     */
    public List<String> findSimilarPlayers(String playerId, int k) {
        float[] targetEmbedding = playerEmbeddings.get(playerId);
        if (targetEmbedding == null) return Collections.emptyList();

        // Calculate cosine similarity
        Map<String, Float> similarities = new HashMap<>();

        for (Map.Entry<String, float[]> entry : playerEmbeddings.entrySet()) {
            if (entry.getKey().equals(playerId)) continue;

            float similarity = cosineSimilarity(targetEmbedding, entry.getValue());
            similarities.put(entry.getKey(), similarity);
        }

        // Return top-k
        return similarities.entrySet().stream()
            .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
            .limit(k)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Recommend preferences based on similar players
     */
    public Map<String, Float> recommendFromSimilarPlayers(String playerId) {
        List<String> similarPlayers = findSimilarPlayers(playerId, 5);

        // Aggregate preferences from similar players
        Map<String, Float> recommendations = new HashMap<>();

        for (String similarPlayer : similarPlayers) {
            float similarity = cosineSimilarity(
                playerEmbeddings.get(playerId),
                playerEmbeddings.get(similarPlayer)
            );

            // Weight by similarity
            // Add preferences weighted by similarity
        }

        return recommendations;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0f;
        float normA = 0f;
        float normB = 0f;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

### 2.4 Reinforcement Learning from Feedback (Advanced)

**For learning optimal behavior policies through reward signals.**

```java
public class PreferenceBasedRLAgent {

    /**
     * Q-Learning for preference-based decisions
     */
    public static class QTable {
        private final Map<String, Map<String, Float>> qTable;
        private final float learningRate = 0.1f;
        private final float discountFactor = 0.9f;
        private final float explorationRate = 0.1f;

        /**
         * Choose action using epsilon-greedy policy
         */
        public String chooseAction(String state, String[] actions) {
            if (random.nextFloat() < explorationRate) {
                // Explore: random action
                return actions[random.nextInt(actions.length)];
            }

            // Exploit: best action
            return actions[argmax(state, actions)];
        }

        /**
         * Update Q-value: Q(s,a) = Q(s,a) + α[r + γmax Q(s',a') - Q(s,a)]
         */
        public void update(String state, String action, float reward,
                          String nextState, String[] possibleActions) {
            float currentQ = getQValue(state, action);
            float maxNextQ = getMaxQValue(nextState, possibleActions);

            float newQ = currentQ + learningRate *
                (reward + discountFactor * maxNextQ - currentQ);

            setQValue(state, action, newQ);
        }

        private int argmax(String state, String[] actions) {
            int bestIndex = 0;
            float bestValue = Float.NEGATIVE_INFINITY;

            for (int i = 0; i < actions.length; i++) {
                float qValue = getQValue(state, actions[i]);
                if (qValue > bestValue) {
                    bestValue = qValue;
                    bestIndex = i;
                }
            }

            return bestIndex;
        }
    }

    /**
     * Reward function based on player feedback
     */
    public float calculateReward(String taskType, ActionResult result,
                                PlayerFeedback feedback) {
        float reward = 0f;

        // Task completion reward
        if (result.isSuccess()) {
            reward += 1f;
        } else {
            reward -= 0.5f;
        }

        // Player feedback reward
        if (feedback.isPositive()) {
            reward += 2f;
        } else if (feedback.isNegative()) {
            reward -= 1f;
        }

        // Efficiency reward (completion time vs expected)
        float efficiency = result.getExpectedTime() / result.getActualTime();
        reward += efficiency * 0.5f;

        return reward;
    }
}
```

---

## 3. Personalization Dimensions

### 3.1 Building Style Preferences

**Detectable Patterns:**
- Material choices (oak vs spruce, stone vs brick)
- Structure types (house vs tower vs castle)
- Architectural era (modern vs medieval vs rustic)
- Scale preferences (compact vs grand)
- Detail level (minimalist vs ornate)

**Implementation:**
```java
public class BuildStyleProfile {

    public static class MaterialPreference {
        String blockType;
        float preference; // -1 to 1
        int usageCount;
        float confidence; // 0 to 1
    }

    public static class StylePreference {
        String style; // "modern", "medieval", etc.
        float preference;
        int buildCount;
        float lastSeen; // Recency score
    }

    private final Map<String, MaterialPreference> materialPrefs;
    private final Map<String, StylePreference> stylePrefs;

    /**
     * Predict best materials for a build based on history
     */
    public String[] recommendMaterials(String structureType) {
        // Get top materials for this structure type
        return materialPrefs.entrySet().stream()
            .filter(e -> e.getValue().confidence > 0.5f)
            .sorted((a, b) -> Float.compare(b.getValue().preference, a.getValue().preference))
            .limit(3)
            .map(Map.Entry::getKey)
            .toArray(String[]::new);
    }

    /**
     * Predict style based on player's previous builds
     */
    public String predictStyle() {
        return stylePrefs.entrySet().stream()
            .max(Comparator.comparing(e ->
                e.getValue().preference * e.getValue().confidence * e.getValue().lastSeen))
            .map(Map.Entry::getKey)
            .orElse("modern"); // Default
    }
}
```

### 3.2 Communication Frequency

**Dimensions:**
- Verbose vs concise responses
- Formal vs casual tone
- Frequency of proactive comments
- Humor usage
- Enthusiasm level

**Learning:**
```java
public class CommunicationStyleProfile {

    private float verbosity; // 0-1
    private float formality; // 0-1
    private float humorLevel; // 0-1
    private float enthusiasm; // 0-1
    private float proactiveFrequency; // Comments per hour

    public void analyzePlayerMessage(String message) {
        int wordCount = message.split("\\s+").length;

        // Update verbosity (running average)
        verbosity = 0.9f * verbosity + 0.1f * (wordCount / 20f);

        // Detect formality
        boolean formal = !message.contains("'") && !message.matches(".*\\bhey\\b.*");
        formality = 0.95f * formality + 0.05f * (formal ? 1f : 0f);

        // Detect humor preference
        boolean wantsHumor = message.matches(".*(funny|joke|hilarious|lol).*");
        if (wantsHumor) {
            humorLevel = Math.min(1f, humorLevel + 0.1f);
        }
    }

    public void applyToPersonality(PersonalityProfile profile) {
        profile.formality = (int) (formality * 100);
        profile.humor = (int) (humorLevel * 100);
        profile.extraversion = (int) (enthusiasm * 100);
    }
}
```

### 3.3 Task Prioritization

**What does the player value?**
- Speed vs quality
- Efficiency vs creativity
- Resource gathering vs building
- Combat vs peaceful activities

**Learning:**
```java
public class TaskPriorityProfile {

    // Importance weights for different objectives
    private float speedImportance;
    private float qualityImportance;
    private float resourceEfficiency;
    private float creativityImportance;

    public void recordTaskResult(String taskType, ActionResult result) {
        // If player repeats task quickly, they value speed
        if (result.getCompletionTime() < result.getExpectedTime() * 0.8f) {
            speedImportance += 0.1f;
        }

        // If player asks for redo or modifications, they value quality
        if (result.getModificationCount() > 0) {
            qualityImportance += 0.2f;
        }

        // Normalize
        normalize();
    }

    public void applyToTaskPlanning(TaskPlanner planner) {
        // Adjust planning strategy based on priorities
        if (speedImportance > 0.7f) {
            planner.setOptimizationMode("speed");
            planner.setParallelTasks(true);
        } else if (qualityImportance > 0.7f) {
            planner.setOptimizationMode("quality");
            planner.setVerificationSteps(true);
        }
    }
}
```

### 3.4 Personality Matching

**Adapting the agent's personality to complement the player.**

```java
public class PersonalityMatchingSystem {

    /**
     * Determine if player prefers complementary or similar personalities
     */
    public void learnPersonalityPreference(PlayerResponse response,
                                          PersonalityProfile agentPersonality) {
        if (response.isPositive()) {
            // Player likes this personality type
            recordPositivePersonality(agentPersonality);
        } else {
            recordNegativePersonality(agentPersonality);
        }
    }

    /**
     * Recommend personality adjustments
     */
    public PersonalityProfile recommendPersonality(
        PersonalityProfile currentPersonality,
        CommunicationStyleProfile playerStyle
    ) {
        PersonalityProfile recommended = currentPersonality.clone();

        // Match formality to player (within reasonable bounds)
        int targetFormality = (int) (playerStyle.getFormality() * 100);
        recommended.formality = clamp(targetFormality, 20, 80);

        // Adjust extraversion: extroverted players prefer introverted agents
        // and vice versa (complementary)
        if (playerStyle.getEnthusiasm() > 0.6f) {
            recommended.extraversion = clamp(100 - (int)(playerStyle.getEnthusiasm() * 100), 30, 70);
        }

        // Match humor level
        recommended.humor = (int) (playerStyle.getHumorLevel() * 100);

        return recommended;
    }
}
```

---

## 4. Implementation in Our System

### 4.1 Storage Architecture

**Leverage existing CompanionMemory:**

```java
// Extend CompanionMemory
public class CompanionMemory {

    // Add preference tracking
    private final FrequencyBasedPreferenceModel preferenceModel;
    private final BayesianPreferenceModel bayesianModel;
    private final BuildStyleProfile buildStyleProfile;
    private final CommunicationStyleProfile communicationStyle;
    private final TaskPriorityProfile taskPriorities;

    /**
     * Initialize preference learning systems
     */
    public CompanionMemory() {
        // ... existing initialization ...

        this.preferenceModel = new FrequencyBasedPreferenceModel();
        this.bayesianModel = new BayesianPreferenceModel();
        this.buildStyleProfile = new BuildStyleProfile();
        this.communicationStyle = new CommunicationStyleProfile();
        this.taskPriorities = new TaskPriorityProfile();
    }

    /**
     * Record player feedback (explicit)
     */
    public void recordPlayerFeedback(String category, String item, float score) {
        // Update all models
        preferenceModel.updatePreference(category, item, score);
        learnPlayerFact(category, item, score);

        // Trigger adaptation
        if (shouldAdapt()) {
            adaptAgentBehavior();
        }
    }

    /**
     * Analyze and learn from player actions (implicit)
     */
    public void analyzePlayerBehavior(PlayerAction action) {
        switch (action.getType()) {
            case BUILD:
                buildStyleProfile.analyzeBuild(action.getBuildData());
                break;
            case COMMUNICATE:
                communicationStyle.analyzeMessage(action.getMessage());
                break;
            case TASK_REQUEST:
                taskPriorities.recordTaskRequest(action.getTaskType());
                break;
        }
    }
}
```

### 4.2 Preference Update Pipeline

**Event-driven preference updates:**

```java
public class PreferenceUpdatePipeline {

    private final List<PreferenceObserver> observers;

    public interface PreferenceObserver {
        void onPreferenceUpdate(String category, String key, float newValue);
        void onFeedbackRecorded(PlayerFeedback feedback);
        void onBehaviorAnalyzed(PlayerBehavior behavior);
    }

    /**
     * Register observers for preference changes
     */
    public void registerObserver(PreferenceObserver observer) {
        observers.add(observer);
    }

    /**
     * Process feedback and notify observers
     */
    public void processFeedback(PlayerFeedback feedback) {
        // Extract preference signals
        Map<String, PreferenceSignal> signals = extractSignals(feedback);

        // Update models
        for (PreferenceSignal signal : signals.values()) {
            updateModels(signal);

            // Notify observers
            observers.forEach(o -> o.onPreferenceUpdate(
                signal.getCategory(),
                signal.getKey(),
                signal.getValue()
            ));
        }
    }

    private void updateModels(PreferenceSignal signal) {
        // Update frequency model
        frequencyModel.update(signal.getCategory(), signal.getKey(), signal.getValue());

        // Update Bayesian model
        bayesianModel.recordObservation(signal.getCategory(), signal.getKey(), signal.getValue());

        // Update embeddings (if using collaborative filtering)
        embeddingModel.update(signal.getPlayerId(), signal.getCategory(), signal.getKey());
    }
}
```

### 4.3 Applying Preferences to Scripts

**Modify PromptBuilder to use preferences:**

```java
public class PromptBuilder {

    public static String buildUserPrompt(ForemanEntity foreman, String command,
                                        WorldKnowledge worldKnowledge,
                                        CompanionMemory memory) {
        StringBuilder prompt = new StringBuilder(384);

        // Existing context
        prompt.append("POS:[");
        BlockPos pos = foreman.blockPosition();
        prompt.append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ()).append(']');
        // ... rest of existing context ...

        // NEW: Add player preferences
        prompt.append("\nPLAYER_PREFS:");

        // Material preferences
        String[] preferredMaterials = memory.getBuildStyleProfile()
            .recommendMaterials(extractStructureType(command));
        if (preferredMaterials.length > 0) {
            prompt.append(" mats:").append(String.join(",", preferredMaterials));
        }

        // Style preference
        String style = memory.getBuildStyleProfile().predictStyle();
        prompt.append(" style:").append(style);

        // Communication style
        float verbosity = memory.getCommunicationStyle().getVerbosity();
        prompt.append(" verbosity:").append(verbosity > 0.5f ? "high" : "low");

        // Task priorities
        if (memory.getTaskPriorities().getSpeedImportance() > 0.7f) {
            prompt.append(" optimize:speed");
        }

        prompt.append("\nCMD:\"").append(command).append("\"");

        return prompt.toString();
    }
}
```

**Adapt agent personality dynamically:**

```java
public class DynamicPersonalityAdapter {

    public void adaptPersonality(CompanionMemory memory) {
        PersonalityProfile profile = memory.getPersonality();
        CommunicationStyleProfile playerStyle = memory.getCommunicationStyle();

        // Gradual adaptation (not abrupt changes)
        float adaptationRate = 0.05f;

        // Adapt formality
        int targetFormality = (int) (playerStyle.getFormality() * 100);
        profile.formality += (int) ((targetFormality - profile.formality) * adaptationRate);

        // Adapt humor
        int targetHumor = (int) (playerStyle.getHumorLevel() * 100);
        profile.humor += (int) ((targetHumor - profile.humor) * adaptationRate);

        // Log adaptation
        LOGGER.debug("Adapted personality: formality={}, humor={}",
            profile.formality, profile.humor);
    }
}
```

### 4.4 Privacy Considerations

**Privacy-First Design Principles:**

1. **Data Minimization**: Store only aggregated preferences, not raw logs
2. **Player Control**: Allow viewing, editing, and deleting preferences
3. **Transparency**: Show what has been learned and why
4. **Local Storage**: Keep preferences on the client, no cloud upload
5. **Opt-In**: Require explicit consent for learning systems

**Implementation:**

```java
public class PrivacyManager {

    /**
     * Privacy settings for each player
     */
    public static class PrivacySettings {
        boolean allowLearning = true;
        boolean allowImplicitTracking = true;
        boolean allowBehaviorAnalysis = true;
        boolean allowPersonalityAdaptation = true;
        int retentionDays = 90;
    }

    /**
     * Export preferences for player viewing
     */
    public String exportPreferences(CompanionMemory memory) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== LEARNED PREFERENCES ===\n\n");

        // Architecture preferences
        sb.append("Architecture Style:\n");
        for (Map.Entry<String, Float> entry : memory.getPreferences("architecture").entrySet()) {
            sb.append("  - ").append(entry.getKey())
              .append(": ").append(formatPreference(entry.getValue())).append("\n");
        }

        // Material preferences
        sb.append("\nMaterials:\n");
        for (Map.Entry<String, Float> entry : memory.getPreferences("material").entrySet()) {
            sb.append("  - ").append(entry.getKey())
              .append(": ").append(formatPreference(entry.getValue())).append("\n");
        }

        // Communication style
        sb.append("\nCommunication Style:\n");
        sb.append("  - Verbosity: ").append(memory.getCommunicationStyle().getVerbosity()).append("\n");
        sb.append("  - Formality: ").append(memory.getCommunicationStyle().getFormality()).append("\n");

        return sb.toString();
    }

    /**
     * Clear all learned preferences
     */
    public void clearAllPreferences(CompanionMemory memory) {
        memory.clearPreferences();
        LOGGER.info("Cleared all preferences for player {}", memory.getPlayerName());
    }

    /**
     * Auto-expire old preferences based on retention policy
     */
    public void applyRetentionPolicy(CompanionMemory memory, PrivacySettings settings) {
        Instant cutoff = Instant.now().minus(settings.retentionDays, ChronoUnit.DAYS);

        // Remove old episodic memories
        memory.getEpisodicMemories().removeIf(m -> m.timestamp.isBefore(cutoff));

        // Decay old semantic memories
        memory.getSemanticMemories().entrySet().removeIf(entry -> {
            if (entry.getValue().learnedAt.isBefore(cutoff)) {
                // Decay confidence instead of removing
                entry.getValue().confidence *= 0.5f;
                return entry.getValue().confidence < 0.1f;
            }
            return false;
        });
    }

    private String formatPreference(float value) {
        if (value > 0.3f) return "Strongly Prefers";
        if (value > 0.1f) return "Prefers";
        if (value < -0.3f) return "Strongly Dislikes";
        if (value < -0.1f) return "Dislikes";
        return "Neutral";
    }
}
```

**Player Commands for Privacy:**

```java
public class PreferenceCommands {

    @Command("steve preferences show")
    public void showPreferences(Player player) {
        CompanionMemory memory = getMemory(player);
        String report = privacyManager.exportPreferences(memory);
        player.displayClientMessage(Component.literal(report));
    }

    @Command("steve preferences clear")
    public void clearPreferences(Player player) {
        CompanionMemory memory = getMemory(player);
        privacyManager.clearAllPreferences(memory);
        player.displayClientMessage(Component.literal("All preferences cleared."));
    }

    @Command("steve preferences disable")
    public void disableLearning(Player player) {
        PrivacySettings settings = getPrivacySettings(player);
        settings.allowLearning = false;
        player.displayClientMessage(Component.literal("Preference learning disabled."));
    }

    @Command("steve preferences enable")
    public void enableLearning(Player player) {
        PrivacySettings settings = getPrivacySettings(player);
        settings.allowLearning = true;
        player.displayClientMessage(Component.literal("Preference learning enabled."));
    }
}
```

---

## 5. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Deliverables:**
1. `FrequencyBasedPreferenceModel` class
2. Integration with `CompanionMemory`
3. Basic preference collection from commands
4. Preference viewing command

**Tasks:**
```java
[ ] Create FrequencyBasedPreferenceModel
[ ] Add preference collection to TaskPlanner
[ ] Store preferences in CompanionMemory NBT
[ ] Add /steve preferences show command
[ ] Add /steve preferences clear command
```

### Phase 2: Implicit Signals (Week 3-4)

**Deliverables:**
1. `BuildStyleAnalyzer` for learning from player builds
2. `CommunicationStyleAnalyzer` for message analysis
3. `TemporalPreferenceTracker` for session patterns
4. Command feedback integration

**Tasks:**
```java
[ ] Implement BuildStyleAnalyzer
[ ] Add block type detection from player builds
[ ] Implement CommunicationStyleAnalyzer
[ ] Track command frequency patterns
[ ] Add feedback commands (good job, no do X)
```

### Phase 3: Bayesian Modeling (Week 5-6)

**Deliverables:**
1. `BayesianPreferenceModel` with Thompson sampling
2. Probability distributions for key preferences
3. Confidence scoring for predictions
4. Exploration/exploitation balance

**Tasks:**
```java
[ ] Create BayesianPreferenceModel
[ ] Implement PreferenceDistribution
[ ] Add Thompson sampling for exploration
[ ] Calculate confidence intervals
[ ] Test preference prediction accuracy
```

### Phase 4: Personalization (Week 7-8)

**Deliverables:**
1. Integration with `PromptBuilder`
2. Dynamic personality adaptation
3. Task prioritization based on preferences
4. Material and style recommendations

**Tasks:**
```java
[ ] Modify PromptBuilder to use preferences
[ ] Create DynamicPersonalityAdapter
[ ] Implement TaskPriorityProfile
[ ] Add material recommendations to builds
[ ] Adapt agent responses to player style
```

### Phase 5: Advanced Features (Week 9-10)

**Deliverables:**
1. `CollaborativePreferenceLearner` for cross-player patterns
2. `PreferenceBasedRLAgent` for optimal policies
3. Privacy controls and export
4. Preference explanation UI

**Tasks:**
```java
[ ] Implement collaborative filtering (optional)
[ ] Add reinforcement learning agent (optional)
[ ] Create privacy management system
[ ] Build preference visualization UI
[ ] Add preference editing commands
```

---

## 6. Evaluation Metrics

### 6.1 Learning Accuracy

**Metrics:**
- **Prediction Accuracy**: How often does the system correctly predict player choices?
- **Convergence Speed**: How many interactions before accurate predictions?
- **Cold Start Performance**: How well does it work with new players?

**Measurement:**
```java
public class PreferenceEvaluator {

    /**
     * Evaluate prediction accuracy
     */
    public float evaluatePredictionAccuracy(CompanionMemory memory,
                                           List<PlayerAction> testActions) {
        int correct = 0;
        int total = testActions.size();

        for (PlayerAction action : testActions) {
            String predicted = predictPlayerChoice(memory, action.getContext());
            if (predicted.equals(action.getChoice())) {
                correct++;
            }
        }

        return (float) correct / total;
    }

    /**
     * Measure convergence speed
     */
    public int measureConvergenceSpeed(CompanionMemory memory) {
        // Track how many interactions until prediction accuracy > 80%
        return 0;
    }
}
```

### 6.2 Player Satisfaction

**Metrics:**
- **Feedback Positivity Ratio**: Positive vs negative feedback
- **Task Success Rate**: Improved success after personalization
- **Engagement**: Time spent with personalized vs non-personalized

**Measurement:**
```java
public class SatisfactionMetrics {

    private final AtomicInteger positiveFeedbackCount;
    private final AtomicInteger negativeFeedbackCount;
    private final AtomicInteger taskSuccessCount;
    private final AtomicInteger taskAttemptCount;

    public float getFeedbackPositivityRatio() {
        int total = positiveFeedbackCount.get() + negativeFeedbackCount.get();
        return total > 0 ? (float) positiveFeedbackCount.get() / total : 0f;
    }

    public float getTaskSuccessRate() {
        int total = taskAttemptCount.get();
        return total > 0 ? (float) taskSuccessCount.get() / total : 0f;
    }
}
```

### 6.3 Performance Impact

**Metrics:**
- **Memory Usage**: Additional memory per player
- **CPU Overhead**: Preference update processing time
- **Storage Size**: NBT data size for preferences

**Targets:**
- Memory: < 1MB per player
- CPU: < 5ms per preference update
- Storage: < 100KB per player in NBT

---

## 7. Research References

### Academic Sources

1. **Modeling and Optimizing User Preferences in AI Copilots** (arXiv, May 2025)
   - https://arxiv.org/html/2505.21907v1
   - Covers preference signal acquisition and feedback loops in real-time AI systems

2. **Bayesian Personalized Ranking (BPR) for Implicit Feedback**
   - Framework for learning from positive-only feedback
   - Addresses data sparsity in preference learning

3. **User Preference and Embedding Learning with Implicit Feedback**
   - Neural collaborative filtering approaches
   - Sequential behavior modeling

4. **Learning and Predicting User Pairwise Preferences From Emotions and Gaze Behavior**
   - http://www.aminer.cn/pub/5daaed073a55ac9a1ab2194d
   - Implicit feedback from behavioral patterns

5. **Multi-modal Recommendation Algorithm Fusing Visual and Textual Features**
   - https://pmc.ncbi.nlm.nih.gov/articles/PMC10310001/
   - Visual Bayesian Personalized Ranking (VBPR)

### Industry Applications

1. **Netflix Recommendation System**
   - Hybrid collaborative + content-based filtering
   - Deep learning for behavior analysis

2. **Spotify Personalization**
   - Collaborative filtering for music preferences
   - Real-time adaptation based on listening behavior

### Key Insights Applied

| Research Insight | Our Application |
|------------------|-----------------|
| Implicit feedback is non-intrusive but noisy | Use confidence scores and Bayesian smoothing |
| Collaborative filtering requires similar users | Build player similarity graphs from play patterns |
| Exploration/exploitation tradeoff | Use Thompson sampling for preference exploration |
| Temporal dynamics matter | Decay old preferences, weight recent interactions |
| Multi-modal signals improve accuracy | Combine explicit commands + implicit behavior |

---

## 8. Conclusion

Player preference learning for Steve AI is feasible and highly valuable for creating engaging, personalized experiences. The key is to:

1. **Start Simple**: Begin with frequency-based preferences, evolve to Bayesian models
2. **Multiple Signals**: Combine explicit feedback with implicit behavior analysis
3. **Continuous Learning**: Update preferences incrementally with every interaction
4. **Privacy-First**: Give players control and transparency over learned preferences
5. **Measure Impact**: Track prediction accuracy and player satisfaction

### Recommended Next Steps

1. **Implement Phase 1** (Foundation) - 2 weeks
2. **A/B Test** with beta testers to validate approach
3. **Iterate** based on player feedback
4. **Expand** to Phases 2-4 based on success metrics

### Success Criteria

- 80%+ prediction accuracy after 20 interactions
- Positive feedback ratio increases by 30%
- Player retention improves by 20%
- Minimal performance impact (<5% CPU overhead)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-28
**Author:** Claude (Research Mode)
**Status:** Ready for Implementation Planning
