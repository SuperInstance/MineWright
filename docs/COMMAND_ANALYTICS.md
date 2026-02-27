# Command Analytics System for MineWright

**Version:** 1.0.0
**Author:** MineWright Development Team
**Date:** 2026-02-27

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Command Frequency Analysis](#command-frequency-analysis)
4. [Pattern Recognition](#pattern-recognition)
5. [Command Suggestion Engine](#command-suggestion-engine)
6. [Preference Learning](#preference-learning)
7. [Macro/Automation Suggestions](#macroautomation-suggestions)
8. [Implementation Guide](#implementation-guide)
9. [Code Examples](#code-examples)
10. [Configuration](#configuration)
11. [Testing Strategy](#testing-strategy)
12. [Performance Considerations](#performance-considerations)

---

## Overview

The Command Analytics System for MineWright provides intelligent analysis of user commands, learning from player behavior to offer suggestions, automate repetitive tasks, and improve the overall user experience. This system builds on the existing `ForemanOfficeGUI` command history and extends it with machine learning capabilities.

### Key Features

- **Real-time Command Analysis**: Track and analyze commands as they are issued
- **Frequency Analysis**: Identify most-used commands and patterns
- **Smart Suggestions**: Auto-complete and suggest commands based on context
- **Preference Learning**: Adapt to individual player styles
- **Macro Detection**: Identify repetitive patterns and suggest automation
- **Temporal Analysis**: Track command patterns over time
- **Success/Failure Tracking**: Learn from command outcomes

### Integration Points

```
ForemanOfficeGUI (existing)
    ↓
CommandAnalyticsManager (new)
    ↓
┌─────────────────────────────────────┐
│  CommandFrequencyAnalyzer           │
│  PatternRecognizer                  │
│  SuggestionEngine                   │
│  PreferenceLearner                  │
│  MacroDetector                      │
└─────────────────────────────────────┘
    ↓
Enhanced GUI with suggestions
```

---

## System Architecture

### Core Components

#### 1. CommandAnalyticsManager

The central orchestrator that coordinates all analytics components.

```java
public class CommandAnalyticsManager {
    private final CommandHistoryStore historyStore;
    private final CommandFrequencyAnalyzer frequencyAnalyzer;
    private final PatternRecognizer patternRecognizer;
    private final SuggestionEngine suggestionEngine;
    private final PreferenceLearner preferenceLearner;
    private final MacroDetector macroDetector;

    // Singleton instance
    private static CommandAnalyticsManager instance;
}
```

#### 2. CommandHistoryStore

Persistent storage for command history with metadata.

```java
public class CommandHistoryStore {
    private final Deque<CommandEntry> commandHistory;
    private final Map<String, CommandStatistics> commandStats;
    private final Map<LocalDate, DailyStats> dailyStats;

    static final int MAX_HISTORY_SIZE = 1000;
}

public class CommandEntry {
    private final String command;
    private final Instant timestamp;
    private final String playerName;
    private final String targetCrew;
    private final boolean success;
    private final long executionTimeMs;
    private final String context; // biome, dimension, etc.
}
```

#### 3. Data Flow

```
User enters command
    ↓
ForemanOfficeGUI.sendCommand()
    ↓
CommandAnalyticsManager.recordCommand()
    ├─→ CommandHistoryStore.persist()
    ├─→ CommandFrequencyAnalyzer.update()
    ├─→ PatternRecognizer.analyze()
    └─→ SuggestionEngine.updateContext()
```

---

## Command Frequency Analysis

### Frequency Metrics

Track multiple frequency dimensions:

```java
public class CommandFrequencyAnalyzer {

    public class FrequencyMetrics {
        // Raw counts
        private int totalCommands;
        private int uniqueCommands;

        // Time-based frequencies
        private double commandsPerMinute;
        private double commandsPerHour;
        private double commandsPerSession;

        // Command ranking
        private List<CommandFrequency> topCommands;

        // Temporal patterns
        private Map<DayOfWeek, Double> dayOfWeekDistribution;
        private Map<HourOfDay, Double> hourlyDistribution;
    }

    public class CommandFrequency {
        private final String command;
        private final int count;
        private final double percentage;
        private final Instant lastUsed;
        private final long averageExecutionTime;
    }
}
```

### Analysis Algorithms

#### 1. Exponential Decay Weighting

Recent commands should have higher weight than old ones:

```java
public double calculateDecayedWeight(CommandEntry entry, Instant now) {
    long hoursSince = ChronoUnit.HOURS.between(entry.timestamp(), now);
    double decayFactor = Math.exp(-hoursSince / DECAY_CONSTANT); // 24 hours
    return decayFactor;
}
```

#### 2. Moving Average

Smooth out short-term fluctuations:

```java
public class MovingAverageCalculator {
    private final Queue<Double> window;
    private final int windowSize;
    private double sum;

    public void addValue(double value) {
        if (window.size() >= windowSize) {
            sum -= window.remove();
        }
        window.add(value);
        sum += value;
    }

    public double getAverage() {
        return window.isEmpty() ? 0 : sum / window.size();
    }
}
```

#### 3. Frequency Percentile Calculation

Identify command usage distribution:

```java
public List<CommandFrequency> getPercentileCommands(double percentile) {
    List<CommandFrequency> allCommands = getAllCommandsByFrequency();
    int cutoffIndex = (int) Math.ceil(allCommands.size() * percentile);
    return allCommands.subList(0, cutoffIndex);
}

// Example: Get top 20% most used commands
List<CommandFrequency> topCommands = getPercentileCommands(0.20);
```

### Visualization Data

Generate data for GUI visualization:

```java
public class FrequencyVisualizationData {
    public BarChartData getTopCommandsBarChart(int limit) {
        // Returns data for: "build", "mine", "craft" vs usage count
    }

    public LineChartData getFrequencyTimeline(String command, Duration period) {
        // Returns data for command frequency over time
    }

    public HeatMapData getDayHourHeatmap() {
        // Returns data for heat map: day of week vs hour
    }
}
```

---

## Pattern Recognition

### Pattern Types

#### 1. Sequential Patterns

Commands that frequently follow each other:

```java
public class SequentialPattern {
    private final List<String> commandSequence;
    private final int occurrenceCount;
    private final double confidence;
    private final double averageTimeBetween;
}

// Example: "build house" → "place torches" → "add furniture"
```

#### 2. Contextual Patterns

Commands based on game context:

```java
public class ContextualPattern {
    private final String command;
    private final GameContext context;
    private final double probability;
}

public class GameContext {
    private final String biome; // "plains", "desert", "cave"
    private final String dimension; // "overworld", "nether"
    private final boolean isUnderground;
    private final boolean nearWater;
    private final TimeOfDay timeOfDay;
    private final ItemStack heldItem;
}
```

#### 3. Temporal Patterns

Time-based command patterns:

```java
public class TemporalPattern {
    private final String command;
    private final LocalTime preferredTime;
    private final DayOfWeek preferredDay;
    private final double strength;
}
```

#### 4. Parameter Patterns

Common parameter values for commands:

```java
public class ParameterPattern {
    private final String command;
    private final String parameter;
    private final Object mostCommonValue;
    private final Map<Object, Integer> valueDistribution;
}

// Example: "build" command often uses "cobblestone", "oak_planks"
```

### Pattern Mining Algorithms

#### 1. N-Gram Analysis

Find common command sequences:

```java
public class NGramAnalyzer {
    private final int n; // n-gram size (typically 2-4)

    public Map<List<String>, Integer> extractNGrams(List<CommandEntry> history) {
        Map<List<String>, Integer> ngrams = new HashMap<>();

        List<String> commands = history.stream()
            .map(CommandEntry::command)
            .toList();

        for (int i = 0; i <= commands.size() - n; i++) {
            List<String> ngram = commands.subList(i, i + n);
            ngrams.merge(ngram, 1, Integer::sum);
        }

        return ngrams;
    }
}
```

#### 2. Association Rule Mining (Apriori Algorithm)

Find commands that frequently occur together:

```java
public class AssociationRuleMiner {
    private final double minSupport;
    private final double minConfidence;

    public class AssociationRule {
        private final Set<String> antecedent; // Commands that trigger
        private final Set<String> consequent; // Commands predicted
        private final double support;
        private final double confidence;
        private final double lift;
    }

    public List<AssociationRule> mineRules(List<CommandEntry> transactions) {
        // Implement Apriori algorithm
        // 1. Find frequent itemsets
        // 2. Generate rules from itemsets
        // 3. Calculate confidence and lift
    }
}
```

#### 3. Markov Chain Prediction

Predict next command based on current state:

```java
public class MarkovChainPredictor {
    private final Map<String, Map<String, Double>> transitionMatrix;

    public void train(List<CommandEntry> history) {
        // Build transition probability matrix
        for (int i = 0; i < history.size() - 1; i++) {
            String current = history.get(i).command();
            String next = history.get(i + 1).command();

            transitionMatrix.computeIfAbsent(current, k -> new HashMap<>())
                .merge(next, 1.0, Double::sum);
        }

        // Normalize to probabilities
        normalizeProbabilities();
    }

    public List<Prediction> predictNextCommand(String currentCommand, int topN) {
        return transitionMatrix.getOrDefault(current, Map.of()).entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(topN)
            .map(e -> new Prediction(e.getKey(), e.getValue()))
            .toList();
    }
}
```

---

## Command Suggestion Engine

### Suggestion Types

#### 1. Auto-Complete Suggestions

Complete partial command input:

```java
public class AutoCompleteSuggester {
    private final Trie<String, CommandSuggestion> commandTrie;

    public List<CommandSuggestion> getSuggestions(String partialInput) {
        // Find matching commands in trie
        // Sort by:
        // 1. Frequency score
        // 2. Recency score
        // 3. Context match score

        return commandTrie.search(partialInput.toLowerCase()).stream()
            .sorted(Comparator.comparingDouble(CommandSuggestion::getScore).reversed())
            .limit(5)
            .toList();
    }
}
```

#### 2. Contextual Suggestions

Suggest commands based on current game state:

```java
public class ContextualSuggester {
    public List<CommandSuggestion> getSuggestions(GameContext context) {
        List<CommandSuggestion> suggestions = new ArrayList<>();

        // Biome-based suggestions
        if (context.biome().equals("desert")) {
            suggestions.add(new CommandSuggestion("build shelter",
                "High priority in desert biome", 0.9));
        }

        // Time-based suggestions
        if (context.timeOfDay() == TimeOfDay.NIGHT) {
            suggestions.add(new CommandSuggestion("place torches",
                "Night time lighting", 0.85));
        }

        // Inventory-based suggestions
        if (has plentifulWood(context.inventory())) {
            suggestions.add(new CommandSuggestion("craft crafting_table",
                "You have plenty of wood", 0.8));
        }

        return suggestions;
    }
}
```

#### 3. Pattern-Based Suggestions

Suggest next command in a sequence:

```java
public class PatternBasedSuggester {
    public List<CommandSuggestion> suggestNextCommands(
        List<String> recentCommands,
        GameContext context
    ) {
        // Use Markov chain for short-term prediction
        List<MarkovChainPredictor.Prediction> markovPredictions =
            markovChain.predictNextCommand(
                recentCommands.get(recentCommands.size() - 1),
                3
            );

        // Use association rules for longer patterns
        List<AssociationRule> matchingRules =
            findMatchingRules(recentCommands);

        // Combine and rank suggestions
        return combineAndRankSuggestions(markovPredictions, matchingRules, context);
    }
}
```

#### 4. Corrective Suggestions

Suggest fixes for failed commands:

```java
public class CorrectiveSuggester {
    public List<CommandSuggestion> suggestFixes(
        String failedCommand,
        String failureReason
    ) {
        return switch (failureReason) {
            case "insufficient_resources" -> List.of(
                new CommandSuggestion("gather " + getMissingResource(failedCommand),
                    "Gather missing resources first", 0.95)
            );
            case "block_not_found" -> List.of(
                new CommandSuggestion("pathfind to " + getTargetBlock(failedCommand),
                    "Move closer to target location", 0.9)
            );
            case "inventory_full" -> List.of(
                new CommandSuggestion("place chest",
                    "Create storage for items", 0.85),
                new CommandSuggestion("craft item",
                    "Use items to free inventory space", 0.7)
            );
            default -> List.of();
        };
    }
}
```

### Suggestion Ranking

Multi-factor scoring system:

```java
public class SuggestionRanker {
    public double calculateScore(CommandSuggestion suggestion, SuggestionContext context) {
        double score = 0.0;

        // Frequency weight: 40%
        score += suggestion.getFrequencyScore() * 0.4;

        // Recency weight: 25%
        score += suggestion.getRecencyScore() * 0.25;

        // Context match weight: 20%
        score += suggestion.getContextMatchScore() * 0.2;

        // User preference weight: 15%
        score += suggestion.getPreferenceScore() * 0.15;

        return score;
    }
}
```

---

## Preference Learning

### Learning Dimensions

#### 1. Command Style Preferences

```java
public class CommandStyleProfile {
    // Verbosity preference
    private double verbosityLevel; // 0.0 = terse, 1.0 = verbose

    // Specificity preference
    private double specificityLevel; // 0.0 = general, 1.0 = specific

    // Automation preference
    private double automationAcceptance; // 0.0 = manual, 1.0 = automated

    // Multi-command preference
    private double batchCommandPreference; // Tendency to use sequences

    // Exploration preference
    private double explorationFactor; // Tendency to try new commands
}
```

#### 2. Time-Based Preferences

```java
public class TemporalProfile {
    private final Map<DayOfWeek, List<String>> preferredCommandsByDay;
    private final Map<LocalTime, List<String>> preferredCommandsByTime;
    private Duration averageSessionLength;
    private Instant preferredPlayTimeStart;
    private Instant preferredPlayTimeEnd;
}
```

#### 3. Contextual Preferences

```java
public class ContextualProfile {
    private final Map<String, List<String>> biomePreferences;
    private final Map<String, List<String>> dimensionPreferences;
    private final Map<String, List<String>> activityPreferences; // building, mining, etc.
}
```

### Learning Algorithms

#### 1. Collaborative Filtering

Learn from similar players:

```java
public class CollaborativeFilteringLearner {
    private final Map<String, PlayerProfile> allProfiles;

    public List<String> suggestCommandsForPlayer(String playerId) {
        PlayerProfile targetProfile = allProfiles.get(playerId);

        // Find similar players
        List<PlayerProfile> similarPlayers = findSimilarPlayers(targetProfile, 10);

        // Aggregate commands from similar players
        Map<String, Double> commandScores = new HashMap<>();
        for (PlayerProfile similar : similarPlayers) {
            double similarity = calculateSimilarity(targetProfile, similar);
            for (String command : similar.getPreferredCommands()) {
                commandScores.merge(command, similarity, Double::sum);
            }
        }

        // Return top suggestions
        return commandScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .toList();
    }

    private double calculateSimilarity(PlayerProfile p1, PlayerProfile p2) {
        // Cosine similarity of command frequency vectors
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        Set<String> allCommands = new HashSet<>();
        allCommands.addAll(p1.getCommandFrequencies().keySet());
        allCommands.addAll(p2.getCommandFrequencies().keySet());

        for (String command : allCommands) {
            double f1 = p1.getCommandFrequency(command);
            double f2 = p2.getCommandFrequency(command);
            dotProduct += f1 * f2;
            norm1 += f1 * f1;
            norm2 += f2 * f2;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
```

#### 2. Reinforcement Learning

Learn from command success/failure:

```java
public class ReinforcementLearner {
    private final Map<String, Double> qTable; // Q-learning table
    private final double learningRate = 0.1;
    private final double discountFactor = 0.9;

    public void updateQValue(String command, double reward) {
        double currentQ = qTable.getOrDefault(command, 0.0);
        double newQ = currentQ + learningRate * (reward - currentQ);
        qTable.put(command, newQ);
    }

    public List<String> suggestActions(String state) {
        // Return actions with highest Q-values for given state
        return qTable.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .toList();
    }
}
```

#### 3. Bayesian Preference Learning

Probabilistic preference modeling:

```java
public class BayesianPreferenceLearner {
    private final Map<String, DirichletDistribution> commandPreferences;

    public void observeCommand(String command, boolean userSatisfied) {
        DirichletDistribution dist = commandPreferences.computeIfAbsent(
            command, k -> new DirichletDistribution(1.0, 1.0)
        );

        if (userSatisfied) {
            dist.update(1, 0); // Increment satisfied count
        } else {
            dist.update(0, 1); // Increment dissatisfied count
        }
    }

    public double getPreferenceProbability(String command) {
        DirichletDistribution dist = commandPreferences.get(command);
        if (dist == null) return 0.5; // Neutral prior
        return dist.getMean();
    }
}
```

---

## Macro/Automation Suggestions

### Macro Detection

Identify repetitive command patterns:

```java
public class MacroDetector {
    private final int minOccurrenceThreshold = 3;
    private final double maxTimeVariance = 0.3; // 30% variance allowed

    public class DetectedMacro {
        private final String name;
        private final List<String> commands;
        private final int occurrenceCount;
        private final double timeConsistency;
        private final double automationPotential;
        private final String suggestedMacroName;
    }

    public List<DetectedMacro> detectMacros(List<CommandEntry> history) {
        // 1. Find repeated sequences using sequence mining
        List<CommandSequence> sequences = findRepeatedSequences(history);

        // 2. Filter by occurrence threshold
        sequences = sequences.stream()
            .filter(s -> s.count >= minOccurrenceThreshold)
            .toList();

        // 3. Analyze time consistency
        sequences = analyzeTimeConsistency(sequences);

        // 4. Calculate automation potential
        return sequences.stream()
            .map(this::calculateAutomationPotential)
            .filter(m -> m.automationPotential > 0.7)
            .toList();
    }

    private List<CommandSequence> findRepeatedSequences(List<CommandEntry> history) {
        List<CommandSequence> sequences = new ArrayList<>();

        // Try sequence lengths from 2 to 10
        for (int length = 2; length <= 10; length++) {
            Map<List<String>, Integer> sequenceCounts = new HashMap<>();

            for (int i = 0; i <= history.size() - length; i++) {
                List<String> sequence = history.subList(i, i + length).stream()
                    .map(CommandEntry::command)
                    .toList();

                sequenceCounts.merge(sequence, 1, Integer::sum);
            }

            // Convert to CommandSequence objects
            for (Map.Entry<List<String>, Integer> entry : sequenceCounts.entrySet()) {
                if (entry.getValue() >= minOccurrenceThreshold) {
                    sequences.add(new CommandSequence(entry.getKey(), entry.getValue()));
                }
            }
        }

        return sequences;
    }
}
```

### Macro Suggestions

Generate actionable macro suggestions:

```java
public class MacroSuggester {
    public class MacroSuggestion {
        private final String macroName;
        private final List<String> commands;
        private final String description;
        private final double estimatedTimeSavings;
        private final String suggestedAlias;
        private final List<String> parameterSuggestions;
    }

    public List<MacroSuggestion> generateSuggestions(List<DetectedMacro> detectedMacros) {
        return detectedMacros.stream()
            .map(this::createSuggestion)
            .sorted(Comparator.comparingDouble(MacroSuggestion::estimatedTimeSavings).reversed())
            .toList();
    }

    private MacroSuggestion createSuggestion(DetectedMacro macro) {
        // Generate meaningful name
        String macroName = generateMacroName(macro.commands);

        // Create description
        String description = createDescription(macro.commands);

        // Calculate time savings
        double timeSavings = calculateTimeSavings(macro);

        // Suggest alias
        String alias = suggestAlias(macroName, macro.commands);

        // Extract parameters
        List<String> parameters = extractParameters(macro.commands);

        return new MacroSuggestion(
            macroName,
            macro.commands,
            description,
            timeSavings,
            alias,
            parameters
        );
    }

    private String generateMacroName(List<String> commands) {
        // Use natural language processing to generate name
        // Example: ["build house", "place torches", "add furniture"]
        //          → "setup_base"

        if (commands.size() == 1) {
            return commands.get(0).replaceAll(" ", "_");
        }

        // Extract key verbs and nouns
        List<String> keyTerms = commands.stream()
            .flatMap(c -> Arrays.stream(c.split(" ")))
            .filter(w -> w.length() > 3)
            .distinct()
            .limit(3)
            .toList();

        return String.join("_", keyTerms);
    }
}
```

### Automation Templates

Pre-built automation patterns:

```java
public class AutomationTemplateLibrary {
    private final Map<String, AutomationTemplate> templates;

    public AutomationTemplateLibrary() {
        templates = Map.of(
            "resource_gathering_cycle", new AutomationTemplate(
                "Resource Gathering Cycle",
                List.of(
                    "mine {resource}",
                    "craft {storage}",
                    "place {storage}",
                    "mine {resource}",
                    "place {storage}"
                ),
                List.of("resource", "storage"),
                "Automated mining and storage cycle"
            ),
            "base_setup", new AutomationTemplate(
                "Base Setup",
                List.of(
                    "build shelter",
                    "place torches",
                    "craft crafting_table",
                    "place crafting_table",
                    "craft furnace",
                    "place furnace"
                ),
                List.of(),
                "Complete base initialization"
            ),
            "farm_setup", new AutomationTemplate(
                "Farm Setup",
                List.of(
                    "hoe ground",
                    "plant seeds",
                    "place water",
                    "build fence"
                ),
                List.of("seeds"),
                "Automated farm creation"
            )
        );
    }

    public AutomationTemplate getMatchingTemplate(List<String> commands) {
        // Find best matching template based on command similarity
        return templates.values().stream()
            .max(Comparator.comparingDouble(t -> calculateSimilarity(t.getCommands(), commands)))
            .orElse(null);
    }
}
```

---

## Implementation Guide

### Phase 1: Core Infrastructure (Week 1-2)

**Tasks:**
1. Create `CommandAnalyticsManager` class
2. Implement `CommandHistoryStore` with NBT persistence
3. Add configuration options to `MineWrightConfig`
4. Set up basic metrics collection

**Files to Create:**
```
src/main/java/com/minewright/analytics/
├── CommandAnalyticsManager.java
├── CommandHistoryStore.java
├── CommandEntry.java
├── CommandStatistics.java
└── analytics-config.toml
```

**Integration Points:**
- Modify `ForemanOfficeGUI.sendCommand()` to record commands
- Add analytics module initialization to `MineWrightMod`

### Phase 2: Frequency Analysis (Week 2-3)

**Tasks:**
1. Implement `CommandFrequencyAnalyzer`
2. Add frequency calculation algorithms
3. Create visualization data generators
4. Add frequency statistics to GUI

**Files to Create:**
```
src/main/java/com/minewright/analytics/frequency/
├── CommandFrequencyAnalyzer.java
├── FrequencyMetrics.java
├── MovingAverageCalculator.java
└── FrequencyVisualizationData.java
```

### Phase 3: Pattern Recognition (Week 3-4)

**Tasks:**
1. Implement pattern mining algorithms
2. Add N-gram analysis
3. Implement Markov chain predictor
4. Create association rule miner

**Files to Create:**
```
src/main/java/com/minewright/analytics/pattern/
├── PatternRecognizer.java
├── SequentialPattern.java
├── NGramAnalyzer.java
├── MarkovChainPredictor.java
├── AssociationRuleMiner.java
└── ContextualPattern.java
```

### Phase 4: Suggestion Engine (Week 4-5)

**Tasks:**
1. Implement auto-complete system
2. Add contextual suggestions
3. Create suggestion ranking system
4. Integrate suggestions into GUI

**Files to Create:**
```
src/main/java/com/minewright/analytics/suggestion/
├── SuggestionEngine.java
├── AutoCompleteSuggester.java
├── ContextualSuggester.java
├── PatternBasedSuggester.java
├── CorrectiveSuggester.java
├── SuggestionRanker.java
└── CommandSuggestion.java
```

### Phase 5: Preference Learning (Week 5-6)

**Tasks:**
1. Implement preference tracking
2. Add collaborative filtering
3. Create reinforcement learning module
4. Implement Bayesian preference learning

**Files to Create:**
```
src/main/java/com/minewright/analytics/preference/
├── PreferenceLearner.java
├── CommandStyleProfile.java
├── TemporalProfile.java
├── ContextualProfile.java
├── CollaborativeFilteringLearner.java
├── ReinforcementLearner.java
└── BayesianPreferenceLearner.java
```

### Phase 6: Macro Detection (Week 6-7)

**Tasks:**
1. Implement macro detection algorithms
2. Create macro suggestion system
3. Build automation template library
4. Add macro creation UI

**Files to Create:**
```
src/main/java/com/minewright/analytics/macro/
├── MacroDetector.java
├── MacroSuggester.java
├── AutomationTemplateLibrary.java
├── DetectedMacro.java
├── MacroSuggestion.java
└── AutomationTemplate.java
```

### Phase 7: GUI Integration (Week 7-8)

**Tasks:**
1. Add analytics panel to ForemanOfficeGUI
2. Implement suggestion display
3. Create macro management UI
4. Add settings for analytics features

**Files to Modify:**
```
src/main/java/com/minewright/client/
├── ForemanOfficeGUI.java (enhance with analytics)
└── AnalyticsOverlayScreen.java (new)
```

### Phase 8: Testing & Optimization (Week 8-9)

**Tasks:**
1. Write unit tests for all components
2. Performance optimization
3. Memory usage profiling
4. Create test data sets

---

## Code Examples

### Example 1: Recording Commands

```java
// In ForemanOfficeGUI.sendCommand()
private static void sendCommand(String command) {
    Minecraft mc = Minecraft.getInstance();

    // Get command analytics manager
    CommandAnalyticsManager analytics = CommandAnalyticsManager.getInstance();

    // Record command with context
    GameContext context = extractGameContext(mc);
    CommandEntry entry = new CommandEntry(
        command,
        Instant.now(),
        mc.player.getName().getString(),
        parseTargetCrew(command),
        context
    );

    analytics.recordCommand(entry);

    // Existing command sending logic...
    commandHistory.add(command);
    // ...
}

private static GameContext extractGameContext(Minecraft mc) {
    if (mc.player == null || mc.level == null) {
        return GameContext.UNKNOWN;
    }

    return new GameContext(
        mc.player.getLevel().getBiome(mc.player.blockPosition())
            .unwrapKey().orElse(null).toString(),
        mc.player.getLevel().dimension().location().toString(),
        mc.player.getY() < mc.level.getSeaLevel(),
        isNearWater(mc.player),
        getTimeOfDay(mc.level),
        mc.player.getMainHandItem()
    );
}
```

### Example 2: Auto-Complete Integration

```java
// In ForemanOfficeGUI
public static boolean handleCharTyped(char codePoint, int modifiers) {
    if (isOpen && inputBox != null) {
        inputBox.charTyped(codePoint, modifiers);

        // Trigger auto-complete
        String currentInput = inputBox.getValue().trim();
        if (currentInput.length() >= 2) {
            List<CommandSuggestion> suggestions = CommandAnalyticsManager
                .getInstance()
                .getSuggestionEngine()
                .getAutoCompleteSuggestions(currentInput);

            displaySuggestions(suggestions);
        }

        return true;
    }
    return false;
}

private static void displaySuggestions(List<CommandSuggestion> suggestions) {
    // Show suggestions in a dropdown below input box
    // User can use Tab to accept suggestion
    currentSuggestions = suggestions;
    selectedSuggestionIndex = 0;
}
```

### Example 3: Pattern-Based Notification

```java
// In ActionExecutor, after command completion
@Override
public void tick() {
    // ... existing tick logic ...

    if (currentAction != null && currentAction.isComplete()) {
        ActionResult result = currentAction.getResult();

        // Notify analytics of success/failure
        CommandAnalyticsManager.getInstance().recordCommandResult(
            pendingCommand,
            result.isSuccess(),
            result.getExecutionTimeMs()
        );

        // Check for pattern-based suggestions
        List<CommandSuggestion> suggestions = CommandAnalyticsManager
            .getInstance()
            .getSuggestionEngine()
            .getPatternBasedSuggestions(recentCommands, currentContext);

        if (!suggestions.isEmpty()) {
            // Show notification: "Based on your pattern, you might want to..."
            showSuggestionNotification(suggestions.get(0));
        }

        currentAction = null;
    }
}
```

### Example 4: Macro Suggestion Display

```java
// In ForemanOfficeGUI, add macro suggestion panel
private static void renderMacroSuggestions(GuiGraphics graphics, int x, int y) {
    CommandAnalyticsManager analytics = CommandAnalyticsManager.getInstance();
    List<MacroSuggestion> suggestions = analytics.getMacroDetector()
        .getPendingSuggestions();

    if (suggestions.isEmpty()) {
        return;
    }

    // Render suggestion panel
    graphics.fillGradient(x, y, x + 200, y + 100, 0xD0202020, 0xD0202020);
    graphics.drawString(font, "§lMacro Suggestion", x + 5, y + 5, 0xFFFFFF);

    MacroSuggestion suggestion = suggestions.get(0);
    graphics.drawString(font, suggestion.getDescription(),
        x + 5, y + 20, 0xAAAAAA);
    graphics.drawString(font, "Time saved: " +
        String.format("%.1f", suggestion.getEstimatedTimeSavings()) + "s",
        x + 5, y + 35, 0x55FF55);

    // Render button: "Create Macro"
    renderButton(graphics, x + 5, y + 55, 190, 20, "Create Macro");
}
```

### Example 5: Persistence with NBT

```java
// In CommandHistoryStore
public void saveToNBT(CompoundTag tag) {
    // Save command history
    ListTag historyList = new ListTag();
    for (CommandEntry entry : commandHistory) {
        CompoundTag entryTag = new CompoundTag();
        entryTag.putString("command", entry.command());
        entryTag.putLong("timestamp", entry.timestamp().toEpochMilli());
        entryTag.putString("playerName", entry.playerName());
        entryTag.putString("targetCrew", entry.targetCrew());
        entryTag.putBoolean("success", entry.success());
        entryTag.putLong("executionTimeMs", entry.executionTimeMs());
        entryTag.putString("context", serializeContext(entry.context()));
        historyList.add(entryTag);
    }
    tag.put("CommandHistory", historyList);

    // Save statistics
    CompoundTag statsTag = new CompoundTag();
    for (Map.Entry<String, CommandStatistics> entry : commandStats.entrySet()) {
        CompoundTag statTag = new CompoundTag();
        statTag.putInt("count", entry.getValue().count());
        statTag.putDouble("averageExecutionTime",
            entry.getValue().averageExecutionTime());
        statTag.putInt("successCount", entry.getValue().successCount());
        statsTag.put(entry.getKey(), statTag);
    }
    tag.put("CommandStatistics", statsTag);

    // Save daily stats
    ListTag dailyList = new ListTag();
    for (Map.Entry<LocalDate, DailyStats> entry : dailyStats.entrySet()) {
        CompoundTag dailyTag = new CompoundTag();
        dailyTag.putString("date", entry.getKey().toString());
        dailyTag.putInt("commandCount", entry.getValue().commandCount());
        dailyTag.putLong("totalSessionTime", entry.getValue().totalSessionTime());
        dailyList.add(dailyTag);
    }
    tag.put("DailyStats", dailyList);
}

public void loadFromNBT(CompoundTag tag) {
    // Load command history
    if (tag.contains("CommandHistory")) {
        commandHistory.clear();
        ListTag historyList = tag.getList("CommandHistory", 10); // COMPOUND
        for (int i = 0; i < historyList.size(); i++) {
            CompoundTag entryTag = historyList.getCompound(i);
            CommandEntry entry = new CommandEntry(
                entryTag.getString("command"),
                Instant.ofEpochMilli(entryTag.getLong("timestamp")),
                entryTag.getString("playerName"),
                entryTag.getString("targetCrew"),
                entryTag.getBoolean("success"),
                entryTag.getLong("executionTimeMs"),
                deserializeContext(entryTag.getString("context"))
            );
            commandHistory.add(entry);
        }
    }

    // Load statistics and daily stats...
}
```

---

## Configuration

### Config Options

Add to `MineWrightConfig.java`:

```java
// Command Analytics Configuration
public static final ForgeConfigSpec.BooleanValue ANALYTICS_ENABLED;
public static final ForgeConfigSpec.IntValue COMMAND_HISTORY_SIZE;
public static final ForgeConfigSpec.BooleanValue SUGGESTIONS_ENABLED;
public static final ForgeConfigSpec.IntValue SUGGESTION_COUNT;
public static final ForgeConfigSpec.BooleanValue MACRO_DETECTION_ENABLED;
public static final ForgeConfigSpec.IntValue MACRO_MIN_OCCURRENCES;
public static final ForgeConfigSpec.BooleanValue PREFERENCE_LEARNING_ENABLED;
public static final ForgeConfigSpec.BooleanValue AUTO_COMPLETE_ENABLED;
public static final ForgeConfigSpec.IntValue AUTO_COMPLETE_MIN_CHARS;

static {
    // ... existing config ...

    builder.comment("Command Analytics Configuration").push("analytics");

    ANALYTICS_ENABLED = builder
        .comment("Enable command analytics and tracking")
        .define("enabled", true);

    COMMAND_HISTORY_SIZE = builder
        .comment("Maximum number of commands to store in history")
        .defineInRange("historySize", 1000, 100, 10000);

    SUGGESTIONS_ENABLED = builder
        .comment("Enable command suggestions")
        .define("suggestionsEnabled", true);

    SUGGESTION_COUNT = builder
        .comment("Maximum number of suggestions to display")
        .defineInRange("suggestionCount", 5, 1, 10);

    MACRO_DETECTION_ENABLED = builder
        .comment("Enable automatic macro detection")
        .define("macroDetectionEnabled", true);

    MACRO_MIN_OCCURRENCES = builder
        .comment("Minimum occurrences before suggesting a macro")
        .defineInRange("macroMinOccurrences", 3, 2, 10);

    PREFERENCE_LEARNING_ENABLED = builder
        .comment("Enable preference learning from user behavior")
        .define("preferenceLearningEnabled", true);

    AUTO_COMPLETE_ENABLED = builder
        .comment("Enable command auto-complete")
        .define("autoCompleteEnabled", true);

    AUTO_COMPLETE_MIN_CHARS = builder
        .comment("Minimum characters before triggering auto-complete")
        .defineInRange("autoCompleteMinChars", 2, 1, 5);

    builder.pop();
}
```

### Configuration File Format

`config/minewright-analytics.toml`:

```toml
[analytics]
enabled = true
history_size = 1000

[suggestions]
enabled = true
count = 5
show_corrections = true
show_pattern_based = true

[auto_complete]
enabled = true
min_chars = 2
delay_ms = 200

[macro_detection]
enabled = true
min_occurrences = 3
time_variance_threshold = 0.3
notify_on_detect = true

[preference_learning]
enabled = true
learning_rate = 0.1
forget_factor = 0.05

[pattern_recognition]
n_gram_size = 3
markov_chain_order = 1
min_support = 0.1
min_confidence = 0.7

[privacy]
share_anonymous_data = false
store_player_names = false
retain_data_days = 90
```

---

## Testing Strategy

### Unit Tests

#### CommandFrequencyAnalyzerTest

```java
@Test
public void testFrequencyCalculation() {
    CommandFrequencyAnalyzer analyzer = new CommandFrequencyAnalyzer();

    // Add test commands
    analyzer.recordCommand(new CommandEntry("build house", now));
    analyzer.recordCommand(new CommandEntry("mine cobblestone", now));
    analyzer.recordCommand(new CommandEntry("build house", now.plusSeconds(10)));

    FrequencyMetrics metrics = analyzer.getMetrics();

    assertEquals(3, metrics.getTotalCommands());
    assertEquals(2, metrics.getUniqueCommands());
    assertEquals(2, metrics.getTopCommands().get(0).count()); // "build house"
}

@Test
public void testDecayedWeight() {
    CommandFrequencyAnalyzer analyzer = new CommandFrequencyAnalyzer();

    Instant oldCommand = Instant.now().minus(48, ChronoUnit.HOURS);
    Instant newCommand = Instant.now().minus(1, ChronoUnit.HOURS);

    analyzer.recordCommand(new CommandEntry("build house", oldCommand));
    analyzer.recordCommand(new CommandEntry("mine cobblestone", newCommand));

    // New command should have higher weighted frequency
    List<CommandFrequency> top = analyzer.getTopCommands(10);
    assertEquals("mine cobblestone", top.get(0).command());
}
```

#### PatternRecognizerTest

```java
@Test
public void testSequentialPatternDetection() {
    PatternRecognizer recognizer = new PatternRecognizer();

    // Create command sequence
    List<CommandEntry> history = List.of(
        new CommandEntry("build house", now),
        new CommandEntry("place torches", now.plusSeconds(5)),
        new CommandEntry("add furniture", now.plusSeconds(10)),
        new CommandEntry("build house", now.plusMinutes(1)),
        new CommandEntry("place torches", now.plusMinutes(1).plusSeconds(5)),
        new CommandEntry("add furniture", now.plusMinutes(1).plusSeconds(10))
    );

    List<SequentialPattern> patterns = recognizer.findSequentialPatterns(history);

    assertFalse(patterns.isEmpty());
    assertEquals(3, patterns.get(0).getCommands().size());
    assertEquals(2, patterns.get(0).getOccurrenceCount());
}
```

#### SuggestionEngineTest

```java
@Test
public void testAutoComplete() {
    SuggestionEngine engine = new SuggestionEngine();

    // Train with commands
    engine.recordCommand(new CommandEntry("build house", now));
    engine.recordCommand(new CommandEntry("build shelter", now));
    engine.recordCommand(new CommandEntry("mine cobblestone", now));

    List<CommandSuggestion> suggestions = engine.getAutoCompleteSuggestions("bu");

    assertEquals(2, suggestions.size());
    assertTrue(suggestions.get(0).getCommand().startsWith("build"));
}

@Test
public void testContextualSuggestions() {
    SuggestionEngine engine = new SuggestionEngine();

    GameContext caveContext = new GameContext(
        "cave", "overworld", true, false,
        TimeOfDay.NIGHT, ItemStack.EMPTY
    );

    List<CommandSuggestion> suggestions = engine.getContextualSuggestions(caveContext);

    assertTrue(suggestions.stream()
        .anyMatch(s -> s.getCommand().contains("torch")));
}
```

### Integration Tests

#### EndToEndAnalyticsTest

```java
@Test
public void testFullAnalyticsPipeline() {
    // Setup
    CommandAnalyticsManager analytics = CommandAnalyticsManager.getInstance();
    ForemanOfficeGUI gui = new ForemanOfficeGUI();

    // Simulate user commands
    String[] commands = {
        "build house",
        "place torches",
        "add furniture",
        "mine cobblestone",
        "build house", // Repeat
        "place torches", // Repeat
    };

    for (String command : commands) {
        gui.sendCommand(command);
    }

    // Verify analytics recorded
    CommandHistoryStore store = analytics.getHistoryStore();
    assertEquals(commands.length, store.getHistorySize());

    // Verify frequency analysis
    FrequencyMetrics metrics = analytics.getFrequencyAnalyzer().getMetrics();
    assertEquals(2, metrics.getTopCommands().get(0).count()); // "build house"

    // Verify pattern detection
    List<SequentialPattern> patterns = analytics.getPatternRecognizer()
        .findSequentialPatterns(store.getHistory());
    assertFalse(patterns.isEmpty());

    // Verify suggestions
    List<CommandSuggestion> suggestions = analytics.getSuggestionEngine()
        .getAutoCompleteSuggestions("b");
    assertTrue(suggestions.size() > 0);
}
```

### Performance Tests

#### AnalyticsPerformanceTest

```java
@Test
public void testLargeDatasetPerformance() {
    CommandAnalyticsManager analytics = CommandAnalyticsManager.getInstance();

    // Generate 10,000 commands
    List<CommandEntry> commands = generateTestCommands(10000);

    long startTime = System.nanoTime();

    for (CommandEntry command : commands) {
        analytics.recordCommand(command);
    }

    long recordTime = System.nanoTime() - startTime;

    // Should process 10,000 commands in less than 1 second
    assertTrue(recordTime < 1_000_000_000); // 1 second in nanoseconds

    // Test query performance
    startTime = System.nanoTime();
    List<CommandSuggestion> suggestions = analytics.getSuggestionEngine()
        .getAutoCompleteSuggestions("build");
    long queryTime = System.nanoTime() - startTime;

    // Queries should be fast (< 100ms)
    assertTrue(queryTime < 100_000_000);
}
```

---

## Performance Considerations

### Memory Management

1. **Limit History Size**: Use bounded collections with fixed maximum size
2. **Lazy Loading**: Load analytics data on-demand rather than all at startup
3. **Data Pruning**: Regularly remove old or low-value data
4. **Compression**: Compress stored command history

```java
public class CommandHistoryStore {
    private static final int MAX_HISTORY_SIZE = 1000;
    private static final long MAX_AGE_MS = Duration.ofDays(90).toMillis();

    public void addCommand(CommandEntry entry) {
        commandHistory.add(entry);

        // Prune old entries
        if (commandHistory.size() > MAX_HISTORY_SIZE) {
            commandHistory.remove();
        }

        // Remove entries older than 90 days
        Instant cutoff = Instant.now().minusMillis(MAX_AGE_MS);
        commandHistory.removeIf(e -> e.timestamp().isBefore(cutoff));
    }
}
```

### CPU Optimization

1. **Async Processing**: Run analytics calculations in background threads
2. **Incremental Updates**: Update statistics incrementally rather than recalculating
3. **Caching**: Cache frequently accessed calculations
4. **Batching**: Process multiple commands in batches

```java
public class AsyncAnalyticsProcessor {
    private final ExecutorService executor;
    private final BlockingQueue<CommandEntry> commandQueue;

    public AsyncAnalyticsProcessor() {
        this.executor = Executors.newSingleThreadExecutor();
        this.commandQueue = new LinkedBlockingQueue<>();

        // Start processing thread
        executor.submit(this::processCommands);
    }

    private void processCommands() {
        List<CommandEntry> batch = new ArrayList<>();

        while (true) {
            try {
                // Wait for first command
                CommandEntry first = commandQueue.take();
                batch.add(first);

                // Collect more commands (up to 100ms worth)
                CommandEntry next;
                while ((next = commandQueue.poll(100, TimeUnit.MILLISECONDS)) != null) {
                    batch.add(next);
                    if (batch.size() >= 100) break; // Max batch size
                }

                // Process batch
                processBatch(batch);
                batch.clear();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processBatch(List<CommandEntry> batch) {
        // Update frequency analyzer
        frequencyAnalyzer.recordCommands(batch);

        // Update pattern recognizer
        patternRecognizer.analyzeBatch(batch);

        // Update suggestions
        suggestionEngine.updateWithBatch(batch);
    }
}
```

### I/O Optimization

1. **Debounced Persistence**: Save to disk at intervals rather than on every command
2. **Binary Format**: Use efficient binary serialization instead of text
3. **Indexing**: Create indexes for fast queries
4. **Async I/O**: Use non-blocking I/O operations

```java
public class DebouncedPersistence {
    private final ScheduledExecutorService scheduler;
    private final Duration debounceDelay;
    private volatile boolean needsSave = false;

    public DebouncedPersistence(Duration debounceDelay) {
        this.debounceDelay = debounceDelay;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void markDirty() {
        needsSave = true;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            if (needsSave) {
                saveToDisk();
                needsSave = false;
            }
        }, 0, debounceDelay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void saveToDisk() {
        // Perform actual save
    }
}
```

### Query Optimization

1. **Pre-computed Aggregations**: Maintain pre-computed statistics
2. **Trie Index**: Use trie structures for fast prefix searches
3. **LRU Cache**: Cache recent query results
4. **Query Optimization**: Use efficient data structures and algorithms

```java
public class OptimizedQueryEngine {
    private final Trie<String, CommandStatistics> commandTrie;
    private final Cache<QueryKey, List<CommandSuggestion>> queryCache;

    public OptimizedQueryEngine() {
        this.commandTrie = new Trie<>();
        this.queryCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
    }

    public List<CommandSuggestion> query(String prefix) {
        return queryCache.get(new QueryKey(prefix), key -> {
            // Compute suggestions
            List<CommandSuggestion> results = commandTrie.search(prefix).stream()
                .map(this::convertToSuggestion)
                .toList();
            return results;
        });
    }
}
```

---

## Summary

The Command Analytics System provides a comprehensive solution for analyzing user commands, learning preferences, and providing intelligent suggestions. Key benefits include:

1. **Improved User Experience**: Faster command input with auto-complete and smart suggestions
2. **Personalization**: Adapts to individual player styles and preferences
3. **Automation**: Identifies repetitive tasks and suggests macros
4. **Insights**: Provides visibility into command patterns and usage
5. **Performance**: Designed for efficiency with async processing and caching

The modular design allows for incremental implementation, starting with basic frequency tracking and progressively adding more advanced features.

---

## Next Steps

1. Review and approve this design document
2. Set up development timeline and milestones
3. Begin Phase 1 implementation (Core Infrastructure)
4. Create detailed test plan
5. Set up CI/CD for automated testing
6. Monitor performance and iterate based on user feedback

---

**Document Version:** 1.0.0
**Last Updated:** 2026-02-27
**Status:** Ready for Implementation
