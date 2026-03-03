# Skill Learning System Design

**Version:** 1.0
**Date:** 2026-03-02
**Status:** Design Document
**Author:** Claude Orchestrator

---

## Executive Summary

This document designs a comprehensive skill learning system for Steve AI that enables agents to automatically acquire, refine, and compose skills from experience. The system builds on the existing Voyager-inspired skill framework while adding advanced capabilities for semantic retrieval, continuous improvement, and skill composition.

### Key Objectives

1. **Automatic Skill Acquisition** - Learn reusable skills from successful task sequences
2. **Semantic Skill Retrieval** - Find relevant skills using natural language and vector similarity
3. **Continuous Skill Refinement** - Track performance and iteratively improve skills
4. **Skill Composition** - Combine simple skills into complex multi-step behaviors

### Design Philosophy

- **Learn by Doing** - Skills emerge from successful execution patterns
- **Semantic Indexing** - Skills are searchable by meaning, not just keywords
- **Version Control** - Skills evolve through tracked refinements
- **Composition Over Creation** - Complex skills build from proven simple skills

---

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Skill Acquisition](#skill-acquisition)
3. [Skill Retrieval](#skill-retrieval)
4. [Skill Refinement](#skill-refinement)
5. [Skill Composition](#skill-composition)
6. [Class Diagrams](#class-diagrams)
7. [API Design](#api-design)
8. [Integration Points](#integration-points)
9. [Implementation Roadmap](#implementation-roadmap)

---

## System Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        SKILL LEARNING SYSTEM                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐         │
│  │  ACQUISITION  │───▶│   RETRIEVAL   │◀───│  REFINEMENT   │         │
│  │              │    │              │    │              │         │
│  │ • Extract    │    │ • Semantic   │    │ • Track      │         │
│  │ • Generate   │    │ • Search     │    │ • Evaluate   │         │
│  │ • Validate   │    │ • Rank       │    │ • Improve    │         │
│  └──────────────┘    └──────────────┘    └──────────────┘         │
│           │                   │                   │                │
│           └───────────────────┼───────────────────┘                │
│                               ▼                                    │
│                    ┌──────────────┐                                │
│                    │ COMPOSITION  │                                │
│                    │              │                                │
│                    │ • Combine    │                                │
│                    │ • Validate   │                                │
│                    │ • Optimize   │                                │
│                    └──────────────┘                                │
└─────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       EXISTING SYSTEMS                              │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │ SkillLibrary │  │ExecutionTrack│  │  HTNPlanner  │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │  VectorStore │  │   Memory     │  │ ActionExec   │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
1. EXECUTION → ExecutionTracker records action sequences
2. EXTRACTION → PatternExtractor identifies recurring patterns
3. GENERATION → SkillGenerator creates skill templates
4. INDEXING → SemanticSearch creates vector embeddings
5. RETRIEVAL → Skills found by similarity matching
6. REFINEMENT → EffectivenessTracker monitors performance
7. COMPOSITION → SkillComposer builds complex skills
```

---

## Skill Acquisition

### Overview

Skill acquisition transforms successful execution sequences into reusable, parameterized skills through pattern recognition and template generation.

### Process Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ACQUISITION PIPELINE                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ExecutionSequence      PatternExtractor      Pattern               │
│  ┌──────────────┐       ┌──────────────┐     ┌──────────────┐     │
│  │ pathfind     │──────▶│              │────▶│ Loop Pattern │     │
│  │ mine         │       │ • Normalize  │     │ frequency: 5 │     │
│  │ place        │       │ • Cluster    │     │ success: 90% │     │
│  │ pathfind     │       │ • Parameterize│     │              │     │
│  │ mine         │       │ • Score      │     └──────────────┘     │
│  │ place        │       └──────────────┘            │             │
│  └──────────────┘                                │             │
│                                                   ▼             │
│                                         SkillGenerator            │
│                                         ┌──────────────┐          │
│                                         │ ExecutableSkil│          │
│                                         │ name: spiral  │          │
│                                         │ params: depth │          │
│                                         │ code: {{depth}│          │
│                                         │ }} for...     │          │
│                                         └──────────────┘          │
│                                                   │             │
│                                                   ▼             │
│                                         SkillLibrary              │
│                                         ┌──────────────┐          │
│                                         │ addSkill()   │          │
│                                         └──────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
```

### Components

#### 1. Enhanced PatternExtractor

**Location:** `src/main/java/com/minewright/skill/PatternExtractor.java` (existing)

**Enhancements Required:**

```java
/**
 * Enhanced pattern extraction with multi-level pattern detection.
 */
public class PatternExtractor {

    // Existing functionality...

    /**
     * NEW: Extract hierarchical patterns (patterns within patterns).
     * Detects sub-patterns that can be composed into complex skills.
     */
    public List<Pattern> extractHierarchicalPatterns(
        List<ExecutionSequence> sequences,
        int maxDepth
    ) {
        // Detect nested patterns
        // E.g., "spiralMine" contains "pathfind", "mine", "place"
        // Return multi-level pattern hierarchy
    }

    /**
     * NEW: Extract conditional patterns.
     * Detects patterns that branch based on world state.
     */
    public List<ConditionalPattern> extractConditionalPatterns(
        List<ExecutionSequence> sequences
    ) {
        // Find sequences that diverge based on conditions
        // E.g., "if hasWood, buildPlanks; else, chopTree"
    }

    /**
     * NEW: Generate semantic description for pattern.
     * Uses LLM to create human-readable skill descriptions.
     */
    public String generateDescription(Pattern pattern) {
        // Call LLM with pattern signature and action sequence
        // Return natural language description
    }
}
```

**New Pattern Types:**

```java
/**
 * Conditional pattern with branches based on world state.
 */
public class ConditionalPattern extends Pattern {
    private final Map<String, List<String>> branches;
    // Conditions → Action sequences

    public List<String> getActionsForState(Map<String, Object> state) {
        // Return appropriate branch based on current state
    }
}

/**
 * Hierarchical pattern with sub-patterns.
 */
public class HierarchicalPattern extends Pattern {
    private final List<Pattern> subPatterns;
    private final CompositionType composition;

    public enum CompositionType {
        SEQUENTIAL,  // Execute in order
        PARALLEL,    // Execute simultaneously
        SELECTIVE,   // Choose one based on conditions
        REPEATING    // Repeat N times
    }
}
```

#### 2. SkillGenerator Enhancement

**Location:** `src/main/java/com/minewright/skill/SkillAutoGenerator.java` (existing)

**Enhancements Required:**

```java
/**
 * Enhanced skill generation with LLM assistance.
 */
public class SkillAutoGenerator {

    // Existing functionality...

    /**
     * NEW: Generate skill with LLM assistance.
     * Uses LLM to generate more sophisticated code templates.
     */
    public ExecutableSkill generateSkillWithLLM(
        PatternExtractor.Pattern pattern,
        LLMClient llmClient
    ) {
        // Prompt LLM with:
        // - Pattern signature
        // - Action sequence
        // - Example executions
        // - Success metrics

        // Request:
        // - JavaScript code template
        // - Parameter descriptions
        // - Preconditions
        // - Postconditions

        // Validate and return skill
    }

    /**
     * NEW: Generate skill name and description semantically.
     */
    public SkillMetadata generateMetadata(Pattern pattern) {
        // Use LLM to generate:
        // - Descriptive name (camelCase)
        // - Natural language description
        // - Category classification
        // - Tags for search
    }

    /**
     * NEW: Extract parameters from pattern.
     */
    public Map<String, ParameterMetadata> extractParameters(
        Pattern pattern
    ) {
        // Analyze pattern for variable parameters
        // Return parameter metadata:
        // - Name, type, default value
        // - Validation rules
        // - Constraints (min, max, enum values)
    }
}
```

**New Metadata Classes:**

```java
/**
 * Metadata for generated skills.
 */
public class SkillMetadata {
    private final String name;
    private final String description;
    private final String category;
    private final List<String> tags;
    private final Map<String, ParameterMetadata> parameters;
    private final List<String> preconditions;
    private final List<String> postconditions;
    private final double confidence;
}

/**
 * Metadata for skill parameters.
 */
public class ParameterMetadata {
    private final String name;
    private final Class<?> type;
    private final Object defaultValue;
    private final Validator validator;
    private final String description;

    public interface Validator {
        boolean validate(Object value);
        String getErrorMessage();
    }
}
```

### Acquisition API

```java
/**
 * Main API for skill acquisition.
 */
public interface SkillAcquisition {

    /**
     * Acquire skills from recent executions.
     * Called periodically by SkillLearningLoop.
     */
    int acquireFromRecentExecutions();

    /**
     * Acquire skills from specific sequences.
     * Useful for manual skill creation.
     */
    List<Skill> acquireFromSequences(
        List<ExecutionSequence> sequences
    );

    /**
     * Generate skill from pattern with LLM assistance.
     */
    Skill generateSkillWithLLM(
        PatternExtractor.Pattern pattern,
        LLMClient llmClient
    );

    /**
     * Validate generated skill before adding to library.
     */
    ValidationResult validateSkill(Skill skill);

    /**
     * Add skill to library with semantic indexing.
     */
    boolean addSkillWithIndexing(Skill skill);
}
```

---

## Skill Retrieval

### Overview

Skill retrieval enables agents to find relevant skills using natural language queries, semantic similarity, and context-aware matching.

### Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          RETRIEVAL SYSTEM                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Query: "build a spiral staircase"                                  │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │              SemanticSkillSearch                     │          │
│  │  ┌──────────────┐    ┌──────────────┐               │          │
│  │  │   Query      │    │  Embedding   │               │          │
│  │  │  Processor   │───▶│   Model      │               │          │
│  │  └──────────────┘    └──────────────┘               │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │              Vector Search                           │          │
│  │                                                       │          │
│  │  Query Vector ──▶ [Cosine Similarity] ──▶ Results    │          │
│  │                  ┌─────────────┐                     │          │
│  │                  │ SkillEmbed  │                     │          │
│  │                  │  [0.2, ...] │ 0.87 ─▶ spiralMine │          │
│  │                  │ SkillEmbed  │                     │          │
│  │                  │  [0.1, ...] │ 0.72 ─▶ digStair   │          │
│  │                  │ SkillEmbed  │                     │          │
│  │                  │  [0.3, ...] │ 0.65 ─▶ buildShel  │          │
│  │                  └─────────────┘                     │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │              Result Ranking                           │          │
│  │                                                       │          │
│  │  • Semantic similarity (70%)                          │          │
│  │  • Success rate (20%)                                 │          │
│  │  • Execution count (5%)                               │          │
│  │  • Recent usage (5%)                                  │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  Ranked Skills: [spiralMine, digStair, buildShel]                  │
└─────────────────────────────────────────────────────────────────────┘
```

### Components

#### 1. SemanticSkillSearch

**New Class:** `src/main/java/com/minewright/skill/SemanticSkillSearch.java`

```java
/**
 * Semantic search for skills using vector embeddings.
 */
public class SemanticSkillSearch {

    private final EmbeddingModel embeddingModel;
    private final InMemoryVectorStore<SkillReference> vectorStore;
    private final SkillLibrary skillLibrary;

    /**
     * Search for skills by natural language query.
     */
    public List<SkillSearchResult> search(String query, int topK) {
        // 1. Generate embedding for query
        float[] queryEmbedding = embeddingModel.embed(query);

        // 2. Search vector store
        List<VectorSearchResult<SkillReference>> vectorResults =
            vectorStore.search(queryEmbedding, topK * 2); // Get more for reranking

        // 3. Rerank by multiple factors
        return rerank(vectorResults, query);
    }

    /**
     * Search for skills applicable to a specific task.
     */
    public List<Skill> searchForTask(Task task, int topK) {
        // Generate query from task
        String query = generateQueryFromTask(task);
        return search(query, topK).stream()
            .map(SkillSearchResult::getSkill)
            .collect(Collectors.toList());
    }

    /**
     * Find similar skills to a given skill.
     */
    public List<Skill> findSimilar(Skill skill, int topK) {
        // Use skill's embedding to find similar
        float[] skillEmbedding = getSkillEmbedding(skill);
        return vectorStore.search(skillEmbedding, topK).stream()
            .map(r -> skillLibrary.getSkill(r.getData().getSkillId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Index a skill for semantic search.
     */
    public void indexSkill(Skill skill) {
        // Generate embedding from description + name
        String text = skill.getName() + " " + skill.getDescription();
        float[] embedding = embeddingModel.embed(text);

        // Store in vector database
        SkillReference ref = new SkillReference(
            skill.getName(),
            skill.getCategory(),
            skill.getRequiredActions()
        );
        vectorStore.add(embedding, ref);
    }

    /**
     * Rerank results by multiple factors.
     */
    private List<SkillSearchResult> rerank(
        List<VectorSearchResult<SkillReference>> vectorResults,
        String query
    ) {
        return vectorResults.stream()
            .map(vr -> {
                Skill skill = skillLibrary.getSkill(vr.getData().getSkillId());
                double score = calculateCompositeScore(vr, skill, query);
                return new SkillSearchResult(skill, score);
            })
            .sorted(Comparator.comparingDouble(SkillSearchResult::getScore).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Calculate composite score from multiple factors.
     */
    private double calculateCompositeScore(
        VectorSearchResult<SkillReference> vectorResult,
        Skill skill,
        String query
    ) {
        double semanticSimilarity = vectorResult.getSimilarity();
        double successRate = skill.getSuccessRate();
        int executionCount = skill.getExecutionCount();
        long recentUsage = System.currentTimeMillis() - skill.getLastExecutionTime();

        // Weighted combination
        double score = semanticSimilarity * 0.70;
        score += successRate * 0.20;
        score += Math.min(executionCount / 100.0, 1.0) * 0.05;
        score += Math.max(0, 1.0 - recentUsage / (7 * 24 * 3600 * 1000.0)) * 0.05;

        return score;
    }
}

/**
 * Result from semantic skill search.
 */
public class SkillSearchResult {
    private final Skill skill;
    private final double score;
    private final String matchReason;

    public Skill getSkill() { return skill; }
    public double getScore() { return score; }
    public String getMatchReason() { return matchReason; }
}

/**
 * Reference to a skill stored in vector database.
 */
public class SkillReference {
    private final String skillId;
    private final String category;
    private final List<String> requiredActions;
}
```

#### 2. ContextAwareRetriever

**New Class:** `src/main/java/com/minewright/skill/ContextAwareRetriever.java`

```java
/**
 * Context-aware skill retrieval considering world state.
 */
public class ContextAwareRetriever {

    private final SemanticSkillSearch semanticSearch;
    private final HTNWorldState worldState;

    /**
     * Find skills applicable to current context.
     */
    public List<Skill> findApplicableSkills(
        String goal,
        HTNWorldState currentState,
        int topK
    ) {
        // 1. Semantic search
        List<Skill> candidates = semanticSearch.search(goal, topK * 2);

        // 2. Filter by context
        return candidates.stream()
            .filter(skill -> isApplicableInContext(skill, currentState))
            .filter(skill -> hasRequiredItems(skill, currentState))
            .filter(skill -> meetsPreconditions(skill, currentState))
            .limit(topK)
            .collect(Collectors.toList());
    }

    /**
     * Check if skill is applicable in current world state.
     */
    private boolean isApplicableInContext(
        Skill skill,
        HTNWorldState state
    ) {
        // Check if skill's required actions match available actions
        // Check if world state satisfies skill's preconditions
        // Consider environmental factors (biome, time, etc.)
        return true; // Simplified
    }

    /**
     * Check if agent has required items.
     */
    private boolean hasRequiredItems(Skill skill, HTNWorldState state) {
        List<String> requiredItems = skill.getRequiredItems();
        // Check inventory
        return true; // Simplified
    }

    /**
     * Check if preconditions are met.
     */
    private boolean meetsPreconditions(Skill skill, HTNWorldState state) {
        // Evaluate skill's preconditions against world state
        return true; // Simplified
    }
}
```

### Retrieval API

```java
/**
 * Main API for skill retrieval.
 */
public interface SkillRetrieval {

    /**
     * Search skills by natural language query.
     */
    List<SkillSearchResult> search(String query, int topK);

    /**
     * Find skills for a specific task.
     */
    List<Skill> searchForTask(Task task, int topK);

    /**
     * Find skills similar to a given skill.
     */
    List<Skill> findSimilar(Skill skill, int topK);

    /**
     * Find skills applicable to current context.
     */
    List<Skill> findApplicable(
        String goal,
        HTNWorldState currentState,
        int topK
    );

    /**
     * Index a skill for retrieval.
     */
    void indexSkill(Skill skill);

    /**
     * Rebuild index from all skills.
     */
    void rebuildIndex();
}
```

---

## Skill Refinement

### Overview

Skill refinement continuously improves skills based on execution feedback, identifying failure patterns and suggesting or applying improvements.

### Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          REFINEMENT SYSTEM                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         Execution Feedback Collection                 │          │
│  │  • Success/Failure tracking                          │          │
│  │  • Execution time monitoring                         │          │
│  │  • Error message capture                            │          │
│  │  • World state snapshot                             │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         Performance Analysis                         │          │
│  │  • Success rate calculation                          │          │
│  │  • Trend detection (improving/declining)             │          │
│  │  • Failure pattern identification                    │          │
│  │  • Comparative analysis (vs similar skills)          │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         Refinement Decision                          │          │
│  │  • KEEP: High success, stable trend                  │          │
│  │  • REFINE: Declining trend, identifiable issues      │          │
│  │  • REMOVE: Persistent failures, better alternatives  │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         Refinement Generation                        │          │
│  │  • Analyze failure patterns                          │          │
│  │  • Generate improvement suggestions                  │          │
│  │  • Create refined skill versions                     │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         Version Control                              │          │
│  │  v1.0: Initial skill                                 │          │
│  │  v1.1: Added timeout handling                        │          │
│  │  v1.2: Improved error recovery                       │          │
│  │  v2.0: Complete rewrite based on feedback            │          │
│  └──────────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
```

### Components

#### 1. SkillVersionControl

**New Class:** `src/main/java/com/minewright/skill/SkillVersionControl.java`

```java
/**
 * Version control for skills with change tracking.
 */
public class SkillVersionControl {

    private final Map<String, List<SkillVersion>> versionHistory;
    private final SkillLibrary skillLibrary;

    /**
     * Create a new version of a skill.
     */
    public SkillVersion createVersion(
        String skillId,
        ExecutableSkill newSkill,
        String changeDescription,
        String author
    ) {
        // Get current version
        List<SkillVersion> history = versionHistory.get(skillId);
        int nextVersion = history.size() + 1;

        // Create new version
        SkillVersion version = new SkillVersion(
            skillId,
            nextVersion,
            newSkill,
            changeDescription,
            author,
            System.currentTimeMillis()
        );

        // Store version
        history.add(version);

        // Update library
        skillLibrary.removeSkill(skillId);
        skillLibrary.addSkill(newSkill);

        return version;
    }

    /**
     * Get version history for a skill.
     */
    public List<SkillVersion> getVersionHistory(String skillId) {
        return List.copyOf(versionHistory.getOrDefault(skillId, List.of()));
    }

    /**
     * Revert to a previous version.
     */
    public boolean revertToVersion(String skillId, int versionNumber) {
        List<SkillVersion> history = versionHistory.get(skillId);
        if (versionNumber < 1 || versionNumber > history.size()) {
            return false;
        }

        SkillVersion targetVersion = history.get(versionNumber - 1);
        skillLibrary.removeSkill(skillId);
        skillLibrary.addSkill(targetVersion.getSkill());

        return true;
    }

    /**
     * Compare two versions.
     */
    public VersionDiff compareVersions(
        String skillId,
        int version1,
        int version2
    ) {
        List<SkillVersion> history = versionHistory.get(skillId);
        SkillVersion v1 = history.get(version1 - 1);
        SkillVersion v2 = history.get(version2 - 1);

        return new VersionDiff(v1, v2);
    }
}

/**
 * Represents a version of a skill.
 */
public class SkillVersion {
    private final String skillId;
    private final int versionNumber;
    private final ExecutableSkill skill;
    private final String changeDescription;
    private final String author;
    private final long timestamp;
    private final SkillStatistics stats;

    // Getters...
}

/**
 * Differences between two skill versions.
 */
public class VersionDiff {
    private final List<String> codeChanges;
    private final List<String> parameterChanges;
    private final List<String> metadataChanges;
    private final SkillStatistics performanceDelta;
}
```

#### 2. RefinementAnalyzer

**New Class:** `src/main/java/com/minewright/skill/RefinementAnalyzer.java`

```java
/**
 * Analyzes skill performance and generates refinements.
 */
public class RefinementAnalyzer {

    private final SkillEffectivenessTracker effectivenessTracker;
    private final ExecutionTracker executionTracker;
    private final LLMClient llmClient;

    /**
     * Analyze a skill and generate refinement suggestions.
     */
    public RefinementReport analyzeSkill(String skillId) {
        // Gather data
        SkillEffectivenessTracker.SkillStats stats =
            effectivenessTracker.getAllStats().get(skillId);
        List<ExecutionSequence> executions =
            executionTracker.getSequencesByGoal(skillId);

        // Analyze failure patterns
        List<FailurePattern> failurePatterns =
            identifyFailurePatterns(executions);

        // Generate suggestions
        List<RefinementSuggestion> suggestions =
            generateSuggestions(skillId, stats, failurePatterns);

        return new RefinementReport(skillId, stats, failurePatterns, suggestions);
    }

    /**
     * Identify patterns in skill failures.
     */
    private List<FailurePattern> identifyFailurePatterns(
        List<ExecutionSequence> executions
    ) {
        List<ExecutionSequence> failures = executions.stream()
            .filter(seq -> !seq.isSuccessful())
            .collect(Collectors.toList());

        List<FailurePattern> patterns = new ArrayList<>();

        // Group by error type
        Map<String, List<ExecutionSequence>> byError = failures.stream()
            .collect(Collectors.groupingBy(
                seq -> seq.getActions().stream()
                    .filter(a -> !a.isSuccess())
                    .map(ActionRecord::getErrorMessage)
                    .findFirst()
                    .orElse("unknown")
            ));

        // Analyze each error group
        for (Map.Entry<String, List<ExecutionSequence>> entry : byError.entrySet()) {
            String error = entry.getKey();
            List<ExecutionSequence> errorExecutions = entry.getValue();

            if (errorExecutions.size() >= 3) {
                // Significant failure pattern
                patterns.add(new FailurePattern(
                    error,
                    errorExecutions.size(),
                    extractCommonContext(errorExecutions)
                ));
            }
        }

        return patterns;
    }

    /**
     * Generate refinement suggestions using LLM.
     */
    private List<RefinementSuggestion> generateSuggestions(
        String skillId,
        SkillEffectivenessTracker.SkillStats stats,
        List<FailurePattern> failurePatterns
    ) {
        // Build prompt for LLM
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this skill and suggest improvements:\n");
        prompt.append("Skill: ").append(skillId).append("\n");
        prompt.append("Success rate: ").append(stats.getSuccessRate()).append("\n");
        prompt.append("Total executions: ").append(stats.getTotalExecutions()).append("\n");
        prompt.append("Average time: ").append(stats.getAverageExecutionTime()).append("ms\n");
        prompt.append("\nFailure patterns:\n");
        for (FailurePattern pattern : failurePatterns) {
            prompt.append("- ").append(pattern.getError())
                  .append(" (").append(pattern.getCount()).append(" occurrences)\n");
        }
        prompt.append("\nSuggest 3-5 specific improvements with code examples.");

        // Call LLM
        String response = llmClient.complete(prompt.toString());

        // Parse suggestions
        return parseSuggestions(response);
    }

    /**
     * Apply a refinement suggestion to create a new skill version.
     */
    public ExecutableSkill applyRefinement(
        String skillId,
        RefinementSuggestion suggestion
    ) {
        // Get current skill
        Skill currentSkill = SkillLibrary.getInstance().getSkill(skillId);
        if (!(currentSkill instanceof ExecutableSkill)) {
            throw new IllegalArgumentException("Skill must be ExecutableSkill");
        }

        ExecutableSkill current = (ExecutableSkill) currentSkill;

        // Apply suggestion
        String refinedCode = refineCode(
            current.getCodeTemplate(),
            suggestion.getCodeChanges()
        );

        // Create new version
        return ExecutableSkill.builder(current.getName() + "_v2")
            .description(current.getDescription() + " (refined)")
            .category(current.getCategory())
            .codeTemplate(refinedCode)
            .requiredActions(current.getRequiredActions().toArray(new String[0]))
            .build();
    }
}

/**
 * Report from skill refinement analysis.
 */
public class RefinementReport {
    private final String skillId;
    private final SkillEffectivenessTracker.SkillStats statistics;
    private final List<FailurePattern> failurePatterns;
    private final List<RefinementSuggestion> suggestions;
    private final RefinementRecommendation recommendation;
}

/**
 * Pattern identified in skill failures.
 */
public class FailurePattern {
    private final String error;
    private final int count;
    private final Map<String, Object> commonContext;
}

/**
 * Suggested refinement for a skill.
 */
public class RefinementSuggestion {
    private final String description;
    private final String rationale;
    private final List<String> codeChanges;
    private final RefinementType type;
    private final int estimatedImpact;

    public enum RefinementType {
        ERROR_HANDLING,
        PERFORMANCE,
        PRECONDITION_CHECK,
        PARAMETER_VALIDATION,
        ALGORITHM_IMPROVEMENT
    }
}

/**
 * Recommendation for skill disposition.
 */
public enum RefinementRecommendation {
    KEEP,           // Skill is performing well
    REFINE,         // Skill needs improvement
    REPLACE,        // Better alternative exists
    REMOVE,         // Skill is not useful
    DEPRECATE       // Keep but don't use for new tasks
}
```

#### 3. AutoRefinementLoop

**New Class:** `src/main/java/com/minewright/skill/AutoRefinementLoop.java`

```java
/**
 * Automatic refinement loop for continuous skill improvement.
 */
public class AutoRefinementLoop {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoRefinementLoop.class);

    private final RefinementAnalyzer analyzer;
    private final SkillVersionControl versionControl;
    private final ExecutorService executor;
    private volatile boolean running;

    /**
     * Start the automatic refinement loop.
     */
    public void start() {
        running = true;
        executor.submit(this::refinementLoop);
        LOGGER.info("AutoRefinementLoop started");
    }

    /**
     * Stop the automatic refinement loop.
     */
    public void stop() {
        running = false;
        executor.shutdown();
        LOGGER.info("AutoRefinementLoop stopped");
    }

    /**
     * Main refinement loop.
     */
    private void refinementLoop() {
        while (running) {
            try {
                // Wait for interval
                Thread.sleep(3600_000); // 1 hour

                // Analyze all skills
                for (String skillId : SkillLibrary.getInstance().getSkillsBySuccessRate()
                        .stream()
                        .map(Skill::getName)
                        .collect(Collectors.toList())) {

                    RefinementReport report = analyzer.analyzeSkill(skillId);

                    // Apply automatic refinements
                    if (report.getRecommendation() == RefinementRecommendation.REFINE) {
                        for (RefinementSuggestion suggestion : report.getSuggestions()) {
                            if (suggestion.getType() == RefinementSuggestion.RefinementType.ERROR_HANDLING
                                    && suggestion.getEstimatedImpact() > 20) {
                                // Automatically apply high-impact error handling fixes
                                applyAutomaticRefinement(skillId, suggestion);
                            }
                        }
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.error("Error in refinement loop", e);
            }
        }
    }

    /**
     * Apply an automatic refinement.
     */
    private void applyAutomaticRefinement(
        String skillId,
        RefinementSuggestion suggestion
    ) {
        LOGGER.info("Applying automatic refinement to {}: {}",
            skillId, suggestion.getDescription());

        ExecutableSkill refinedSkill = analyzer.applyRefinement(skillId, suggestion);
        versionControl.createVersion(
            skillId,
            refinedSkill,
            suggestion.getDescription(),
            "AutoRefinementLoop"
        );
    }
}
```

### Refinement API

```java
/**
 * Main API for skill refinement.
 */
public interface SkillRefinement {

    /**
     * Analyze a skill and generate refinement report.
     */
    RefinementReport analyzeSkill(String skillId);

    /**
     * Apply a refinement suggestion.
     */
    ExecutableSkill applyRefinement(
        String skillId,
        RefinementSuggestion suggestion
    );

    /**
     * Get version history for a skill.
     */
    List<SkillVersion> getVersionHistory(String skillId);

    /**
     * Revert to a previous version.
     */
    boolean revertToVersion(String skillId, int versionNumber);

    /**
     * Start automatic refinement loop.
     */
    void startAutoRefinement();

    /**
     * Stop automatic refinement loop.
     */
    void stopAutoRefinement();
}
```

---

## Skill Composition

### Overview

Skill composition enables agents to combine simple skills into complex behaviors, handling dependencies, conflicts, and optimization automatically.

### Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         COMPOSITION SYSTEM                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Goal: "Build an automated tree farm with storage"                  │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         Skill Decomposer                             │          │
│  │  Break down complex goal into sub-goals              │          │
│  │  ┌─────────────────────────────────────────────┐    │          │
│  │  │ 1. Clear land area                           │    │          │
│  │  │ 2. Build storage building                    │    │          │
│  │  │ 3. Plant saplings in grid pattern            │    │          │
│  │  │ 4. Add lighting system                       │    │          │
│  │  │ 5. Create collection path                    │    │          │
│  │  └─────────────────────────────────────────────┘    │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         Skill Finder                                 │          │
│  │  Find skills for each sub-goal                       │          │
│  │  ┌─────────────────────────────────────────────┐    │          │
│  │  │ clearLand → [clearArea, removeDebris]       │    │          │
│  │  │ buildStorage → [buildShelter, addChests]    │    │          │
│  │  │ plantSaplings → [farmTree]                  │    │          │
│  │  │ addLighting → [placeTorches]                │    │          │
│  │  │ createPath → [buildPlatform]                 │    │          │
│  │  └─────────────────────────────────────────────┘    │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         Dependency Resolver                          │          │
│  │  Handle dependencies and ordering                    │          │
│  │  ┌─────────────────────────────────────────────┐    │          │
│  │  │ clearArea (independent)                     │    │          │
│  │  │   buildShelter (depends: cleared land)      │    │          │
│  │  │     addChests (depends: shelter built)      │    │          │
│  │  │   placeTorches (depends: shelter built)     │    │          │
│  │  │ farmTree (depends: cleared land)            │    │          │
│  │  │   buildPlatform (depends: trees grown)      │    │          │
│  │  └─────────────────────────────────────────────┘    │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         Conflict Detector                            │          │
│  │  Detect and resolve conflicts                        │          │
│  │  ┌─────────────────────────────────────────────┐    │          │
│  │  │ CONFLICT: buildShelter and farmTree both    │    │          │
│  │  │ need cleared land in same location          │    │          │
│  │  │ RESOLUTION: Execute sequentially with       │    │          │
│  │  │ spatial partitioning                        │    │          │
│  │  └─────────────────────────────────────────────┘    │          │
│  └──────────────────────────────────────────────────────┘          │
│       │                                                             │
│       ▼                                                             │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         Composite Skill Generator                     │          │
│  │  Create new skill from composed components            │          │
│  │  ┌─────────────────────────────────────────────┐    │          │
│  │  │ CompositeSkill: treeFarmComplex             │    │          │
│  │  │ Components: [clearArea, buildShelter, ...]  │    │          │
│  │  │ Execution: SEQUENTIAL with dependencies     │    │          │
│  │  │ Estimated time: 2500 ticks                  │    │          │
│  │  │ Confidence: 0.85                            │    │          │
│  │  └─────────────────────────────────────────────┘    │          │
│  └──────────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
```

### Components

#### 1. SkillComposer

**New Class:** `src/main/java/com/minewright/skill/SkillComposer.java`

```java
/**
 * Composes complex skills from simple skills.
 */
public class SkillComposer {

    private final SemanticSkillSearch skillSearch;
    private final HTNPlanner htnPlanner;

    /**
     * Compose a skill for a complex goal.
     */
    public CompositeSkill composeSkill(
        String goal,
        HTNWorldState initialState
    ) {
        // 1. Decompose goal into sub-goals
        List<SubGoal> subGoals = decomposeGoal(goal);

        // 2. Find skills for each sub-goal
        Map<SubGoal, List<Skill>> skillCandidates = new HashMap<>();
        for (SubGoal subGoal : subGoals) {
            List<Skill> skills = skillSearch.search(
                subGoal.getDescription(),
                5
            ).stream()
                .map(SkillSearchResult::getSkill)
                .collect(Collectors.toList());
            skillCandidates.put(subGoal, skills);
        }

        // 3. Resolve dependencies
        CompositionPlan plan = resolveDependencies(skillCandidates, initialState);

        // 4. Detect and resolve conflicts
        plan = resolveConflicts(plan);

        // 5. Generate composite skill
        return generateCompositeSkill(goal, plan);
    }

    /**
     * Decompose a complex goal into sub-goals.
     */
    private List<SubGoal> decomposeGoal(String goal) {
        // Use HTN planner or LLM to decompose
        // For now, use simple keyword-based decomposition

        List<SubGoal> subGoals = new ArrayList<>();

        if (goal.contains("farm") && goal.contains("storage")) {
            subGoals.add(new SubGoal("Clear land area", 1));
            subGoals.add(new SubGoal("Build storage building", 2));
            subGoals.add(new SubGoal("Plant crops/trees", 3));
            subGoals.add(new SubGoal("Add lighting", 4));
        }

        return subGoals;
    }

    /**
     * Resolve dependencies between skills.
     */
    private CompositionPlan resolveDependencies(
        Map<SubGoal, List<Skill>> skillCandidates,
        HTNWorldState initialState
    ) {
        CompositionPlan plan = new CompositionPlan();

        // For each sub-goal, select best skill considering dependencies
        for (Map.Entry<SubGoal, List<Skill>> entry : skillCandidates.entrySet()) {
            SubGoal subGoal = entry.getKey();
            List<Skill> candidates = entry.getValue();

            // Select best candidate
            Skill selected = selectBestSkill(candidates, plan, initialState);

            // Add to plan
            plan.addSkill(subGoal, selected);
        }

        // Order by dependencies
        plan.orderByDependencies();

        return plan;
    }

    /**
     * Select the best skill from candidates.
     */
    private Skill selectBestSkill(
        List<Skill> candidates,
        CompositionPlan currentPlan,
        HTNWorldState state
    ) {
        // Score each candidate
        return candidates.stream()
            .max(Comparator.comparingDouble(skill ->
                scoreSkillForComposition(skill, currentPlan, state)
            ))
            .orElse(null);
    }

    /**
     * Score a skill for composition.
     */
    private double scoreSkillForComposition(
        Skill skill,
        CompositionPlan plan,
        HTNWorldState state
    ) {
        double score = 0.0;

        // Success rate (40%)
        score += skill.getSuccessRate() * 0.40;

        // Execution time (20% - faster is better)
        int estimatedTime = skill.getEstimatedTicks();
        score += Math.max(0, 1.0 - estimatedTime / 1000.0) * 0.20;

        // Compatibility with plan (30%)
        score += checkCompatibility(skill, plan) * 0.30;

        // Preconditions met (10%)
        score += checkPreconditions(skill, state) * 0.10;

        return score;
    }

    /**
     * Check compatibility with current composition plan.
     */
    private double checkCompatibility(Skill skill, CompositionPlan plan) {
        // Check for conflicts with already selected skills
        // Check for shared resources
        // Check for spatial conflicts
        return 1.0; // Simplified
    }

    /**
     * Check if preconditions are met.
     */
    private double checkPreconditions(Skill skill, HTNWorldState state) {
        // Check if skill's preconditions are satisfied
        return 1.0; // Simplified
    }

    /**
     * Detect and resolve conflicts in composition.
     */
    private CompositionPlan resolveConflicts(CompositionPlan plan) {
        // Detect conflicts
        List<Conflict> conflicts = detectConflicts(plan);

        // Resolve conflicts
        for (Conflict conflict : conflicts) {
            switch (conflict.getType()) {
                case RESOURCE:
                    plan = resolveResourceConflict(plan, conflict);
                    break;
                case SPATIAL:
                    plan = resolveSpatialConflict(plan, conflict);
                    break;
                case TEMPORAL:
                    plan = resolveTemporalConflict(plan, conflict);
                    break;
            }
        }

        return plan;
    }

    /**
     * Detect conflicts in composition plan.
     */
    private List<Conflict> detectConflicts(CompositionPlan plan) {
        List<Conflict> conflicts = new ArrayList<>();

        // Check for resource conflicts
        // Check for spatial conflicts
        // Check for temporal conflicts

        return conflicts;
    }

    /**
     * Resolve resource conflict.
     */
    private CompositionPlan resolveResourceConflict(
        CompositionPlan plan,
        Conflict conflict
    ) {
        // Add sequencing or resource allocation
        plan.addConstraint(new Constraint(
            Constraint.ConstraintType.SEQUENCING,
            conflict.getSkills()
        ));
        return plan;
    }

    /**
     * Resolve spatial conflict.
     */
    private CompositionPlan resolveSpatialConflict(
        CompositionPlan plan,
        Conflict conflict
    ) {
        // Add spatial partitioning
        plan.addConstraint(new Constraint(
            Constraint.ConstraintType.SPATIAL_PARTITION,
            conflict.getSkills()
        ));
        return plan;
    }

    /**
     * Resolve temporal conflict.
     */
    private CompositionPlan resolveTemporalConflict(
        CompositionPlan plan,
        Conflict conflict
    ) {
        // Add timing constraints
        plan.addConstraint(new Constraint(
            Constraint.ConstraintType.TIMING,
            conflict.getSkills()
        ));
        return plan;
    }

    /**
     * Generate composite skill from plan.
     */
    private CompositeSkill generateCompositeSkill(
        String goal,
        CompositionPlan plan
    ) {
        return new CompositeSkill(
            generateSkillName(goal),
            goal,
            plan.getSkills(),
            plan.getConstraints(),
            plan.getEstimatedTotalTime(),
            plan.getConfidence()
        );
    }

    /**
     * Generate skill name from goal.
     */
    private String generateSkillName(String goal) {
        // Convert goal to camelCase skill name
        return goal.toLowerCase()
            .replaceAll("[^a-z\\s]", "")
            .trim()
            .replaceAll("\\s+", "_");
    }
}

/**
 * Represents a sub-goal in composition.
 */
public class SubGoal {
    private final String description;
    private final int priority;
    private final List<String> preconditions;
}

/**
 * Plan for composing skills.
 */
public class CompositionPlan {
    private final List<PlannedSkill> skills;
    private final List<Dependency> dependencies;
    private final List<Constraint> constraints;

    public void orderByDependencies() {
        // Topological sort by dependencies
    }

    public int getEstimatedTotalTime() {
        return skills.stream()
            .mapToInt(ps -> ps.getSkill().getEstimatedTicks())
            .sum();
    }

    public double getConfidence() {
        return skills.stream()
            .mapToDouble(ps -> ps.getSkill().getSuccessRate())
            .average()
            .orElse(0.0);
    }
}

/**
 * A skill in the composition plan.
 */
public class PlannedSkill {
    private final Skill skill;
    private final Map<String, Object> parameters;
    private final int order;
}

/**
 * Dependency between skills.
 */
public class Dependency {
    private final Skill dependsOn;
    private final Skill dependency;
    private final DependencyType type;

    public enum DependencyType {
        SEQUENTIAL,      // Must execute after
        PARALLEL,        // Can execute simultaneously
        RESOURCE_SHARE   // Share a resource
    }
}

/**
 * Constraint on skill composition.
 */
public class Constraint {
    private final ConstraintType type;
    private final List<Skill> skills;
    private final Map<String, Object> parameters;

    public enum ConstraintType {
        SEQUENCING,
        SPATIAL_PARTITION,
        TIMING,
        RESOURCE_ALLOCATION
    }
}

/**
 * Conflict between skills.
 */
public class Conflict {
    private final ConflictType type;
    private final List<Skill> skills;
    private final String description;

    public enum ConflictType {
        RESOURCE,    // Both need same resource
        SPATIAL,     // Both need same space
        TEMPORAL     // Timing conflicts
    }
}
```

#### 2. CompositeSkill

**New Class:** `src/main/java/com/minewright/skill/CompositeSkill.java`

```java
/**
 * A skill composed of multiple component skills.
 */
public class CompositeSkill implements Skill {

    private final String name;
    private final String description;
    private final List<ComponentSkill> components;
    private final List<Constraint> constraints;
    private final int estimatedTicks;
    private final double confidence;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<String> getRequiredActions() {
        return components.stream()
            .flatMap(c -> c.getSkill().getRequiredActions().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public String generateCode(Map<String, Object> context) {
        // Generate code that executes components in order
        StringBuilder code = new StringBuilder();

        // Add context extraction
        code.append("// Composite skill: ").append(name).append("\n");
        code.append("// Components: ").append(components.size()).append("\n\n");

        // Generate code for each component
        for (ComponentSkill component : components) {
            code.append("// Component: ").append(component.getSkill().getName()).append("\n");
            code.append(component.getSkill().generateCode(
                mergeContext(context, component.getParameters())
            ));
            code.append("\n");
        }

        return code.toString();
    }

    @Override
    public boolean isApplicable(Task task) {
        // Check if all components are applicable
        return components.stream()
            .allMatch(c -> c.getSkill().isApplicable(task));
    }

    @Override
    public double getSuccessRate() {
        // Composite success rate is product of component rates
        return components.stream()
            .mapToDouble(c -> c.getSkill().getSuccessRate())
            .reduce(1.0, (a, b) -> a * b);
    }

    @Override
    public void recordSuccess(boolean success) {
        // Record success for all components
        for (ComponentSkill component : components) {
            component.getSkill().recordSuccess(success);
        }
    }

    @Override
    public int getExecutionCount() {
        return components.stream()
            .mapToInt(Skill::getExecutionCount)
            .sum();
    }

    @Override
    public int getEstimatedTicks() {
        return estimatedTicks;
    }

    /**
     * Get confidence score for this composite.
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * Merge context with component parameters.
     */
    private Map<String, Object> mergeContext(
        Map<String, Object> baseContext,
        Map<String, Object> componentParams
    ) {
        Map<String, Object> merged = new HashMap<>(baseContext);
        merged.putAll(componentParams);
        return merged;
    }
}

/**
 * A component skill in a composite.
 */
public class ComponentSkill {
    private final Skill skill;
    private final Map<String, Object> parameters;
    private final int order;
    private final ExecutionMode executionMode;

    public enum ExecutionMode {
        SEQUENTIAL,      // Execute in order
        PARALLEL,        // Execute simultaneously
        CONDITIONAL      // Execute based on condition
    }
}
```

### Composition API

```java
/**
 * Main API for skill composition.
 */
public interface SkillComposition {

    /**
     * Compose a skill for a complex goal.
     */
    CompositeSkill composeSkill(
        String goal,
        HTNWorldState initialState
    );

    /**
     * Compose skill from explicit components.
     */
    CompositeSkill composeFromComponents(
        String name,
        List<ComponentSkill> components,
        List<Constraint> constraints
    );

    /**
     * Validate a composite skill.
     */
    ValidationResult validateComposite(CompositeSkill skill);

    /**
     * Optimize a composite skill.
     */
    CompositeSkill optimize(CompositeSkill skill);

    /**
     * Break down a composite into components.
     */
    List<Skill> decompose(CompositeSkill skill);
}
```

---

## Class Diagrams

### Core Classes

```
┌─────────────────────────┐
│       Skill             │
│  (interface)            │
├─────────────────────────┤
│ + getName(): String     │
│ + getDescription(): Str │
│ + getRequiredActions()  │
│ + generateCode(ctx)     │
│ + isApplicable(task)    │
│ + getSuccessRate()      │
│ + recordSuccess(bool)   │
└─────────┬───────────────┘
          │
          ├── extends ───┐
          │              │
┌─────────┴──────────┐  ┌──────────────────┐
│  ExecutableSkill   │  │  CompositeSkill  │
├────────────────────┤  ├──────────────────┤
│ - codeTemplate     │  │ - components      │
│ - requiredActions  │  │ - constraints     │
│ - executionCount   │  │ - confidence      │
│ - successCount     │  ├──────────────────┤
├────────────────────┤  │ + getConfidence() │
│ + generateCode()   │  │ + execute()       │
│ + execute()        │  └──────────────────┘
└────────────────────┘
```

### Acquisition System

```
┌─────────────────────────────────┐
│   PatternExtractor              │
├─────────────────────────────────┤
│ - MIN_FREQUENCY                 │
│ - MIN_SUCCESS_RATE              │
├─────────────────────────────────┤
│ + extractPatterns(seqs)         │
│ + extractHierarchicalPatterns() │
│ + extractConditionalPatterns()  │
│ + generateDescription()         │
└─────────┬───────────────────────┘
          │ creates
          ▼
┌─────────────────────────────────┐
│   SkillAutoGenerator            │
├─────────────────────────────────┤
│ + generateSkill(pattern)        │
│ + generateSkillWithLLM()        │
│ + generateMetadata()            │
│ + extractParameters()           │
└─────────────────────────────────┘
```

### Retrieval System

```
┌─────────────────────────────────┐
│   SemanticSkillSearch           │
├─────────────────────────────────┤
│ - embeddingModel                │
│ - vectorStore                   │
│ - skillLibrary                  │
├─────────────────────────────────┤
│ + search(query, k)              │
│ + searchForTask(task, k)        │
│ + findSimilar(skill, k)         │
│ + indexSkill(skill)             │
└─────────────────────────────────┘
          │
          │ uses
          ▼
┌─────────────────────────────────┐
│   ContextAwareRetriever         │
├─────────────────────────────────┤
│ - semanticSearch                │
│ - worldState                    │
├─────────────────────────────────┤
│ + findApplicable(goal, state,k) │
│ + isApplicableInContext()       │
│ + hasRequiredItems()            │
└─────────────────────────────────┘
```

### Refinement System

```
┌─────────────────────────────────┐
│   RefinementAnalyzer            │
├─────────────────────────────────┤
│ - effectivenessTracker          │
│ - executionTracker              │
│ - llmClient                     │
├─────────────────────────────────┤
│ + analyzeSkill(skillId)         │
│ + identifyFailurePatterns()     │
│ + generateSuggestions()         │
│ + applyRefinement()             │
└─────────┬───────────────────────┘
          │
          │ creates
          ▼
┌─────────────────────────────────┐
│   SkillVersionControl           │
├─────────────────────────────────┤
│ - versionHistory                │
│ - skillLibrary                  │
├─────────────────────────────────┤
│ + createVersion()               │
│ + getVersionHistory()           │
│ + revertToVersion()             │
│ + compareVersions()             │
└─────────────────────────────────┘
```

### Composition System

```
┌─────────────────────────────────┐
│   SkillComposer                 │
├─────────────────────────────────┤
│ - skillSearch                   │
│ - htnPlanner                    │
├─────────────────────────────────┤
│ + composeSkill(goal, state)     │
│ + decomposeGoal(goal)           │
│ + resolveDependencies()         │
│ + resolveConflicts()            │
│ + generateCompositeSkill()      │
└─────────┬───────────────────────┘
          │ creates
          ▼
┌─────────────────────────────────┐
│   CompositeSkill                │
├─────────────────────────────────┤
│ - components: List<Component>   │
│ - constraints: List<Constraint> │
│ - confidence: double            │
├─────────────────────────────────┤
│ + getConfidence()               │
│ + executeComponents()           │
└─────────────────────────────────┘
```

---

## API Design

### Unified Skill Learning API

```java
/**
 * Unified API for skill learning system.
 */
public interface SkillLearningSystem {

    // ========== Acquisition ==========

    /**
     * Acquire skills from recent executions.
     */
    int acquireFromRecentExecutions();

    /**
     * Generate skill from pattern with LLM.
     */
    Skill generateSkillWithLLM(
        PatternExtractor.Pattern pattern,
        LLMClient llmClient
    );

    // ========== Retrieval ==========

    /**
     * Search skills by natural language.
     */
    List<SkillSearchResult> search(String query, int topK);

    /**
     * Find skills for a task.
     */
    List<Skill> searchForTask(Task task, int topK);

    /**
     * Find applicable skills in context.
     */
    List<Skill> findApplicable(
        String goal,
        HTNWorldState state,
        int topK
    );

    // ========== Refinement ==========

    /**
     * Analyze skill for refinement.
     */
    RefinementReport analyzeSkill(String skillId);

    /**
     * Apply refinement suggestion.
     */
    ExecutableSkill applyRefinement(
        String skillId,
        RefinementSuggestion suggestion
    );

    /**
     * Get version history.
     */
    List<SkillVersion> getVersionHistory(String skillId);

    /**
     * Revert to version.
     */
    boolean revertToVersion(String skillId, int version);

    // ========== Composition ==========

    /**
     * Compose complex skill.
     */
    CompositeSkill composeSkill(
        String goal,
        HTNWorldState initialState
    );

    /**
     * Validate composite.
     */
    ValidationResult validateComposite(CompositeSkill skill);

    // ========== Lifecycle ==========

    /**
     * Start learning loop.
     */
    void start();

    /**
     * Stop learning loop.
     */
    void stop();

    /**
     * Get system status.
     */
    Map<String, Object> getStatus();
}
```

### Implementation

```java
/**
 * Main implementation of skill learning system.
 */
public class SkillLearningSystemImpl implements SkillLearningSystem {

    private final SemanticSkillSearch search;
    private final ContextAwareRetriever retriever;
    private final RefinementAnalyzer analyzer;
    private final SkillVersionControl versionControl;
    private final SkillComposer composer;
    private final SkillLearningLoop learningLoop;
    private final AutoRefinementLoop refinementLoop;

    public SkillLearningSystemImpl() {
        this.search = new SemanticSkillSearch(
            EmbeddingModel.createLocal(),
            new InMemoryVectorStore<>(384),
            SkillLibrary.getInstance()
        );
        this.retriever = new ContextAwareRetriever(search);
        this.analyzer = new RefinementAnalyzer(
            SkillEffectivenessTracker.getInstance(),
            ExecutionTracker.getInstance(),
            LLMClient.createDefault()
        );
        this.versionControl = new SkillVersionControl(
            SkillLibrary.getInstance()
        );
        this.composer = new SkillComposer(search, new HTNPlanner(
            HTNDomain.createDefault()
        ));
        this.learningLoop = SkillLearningLoop.getInstance();
        this.refinementLoop = new AutoRefinementLoop(
            analyzer,
            versionControl
        );
    }

    @Override
    public void start() {
        learningLoop.start();
        refinementLoop.start();
    }

    @Override
    public void stop() {
        learningLoop.stop();
        refinementLoop.stop();
    }

    // Implement all interface methods...
}
```

---

## Integration Points

### 1. Integration with Existing SkillLibrary

```java
// Enhanced SkillLibrary with semantic search
public class SkillLibrary {
    // Existing code...

    private SemanticSkillSearch semanticSearch;

    /**
     * Add skill with semantic indexing.
     */
    public boolean addSkillWithIndexing(Skill skill) {
        boolean added = addSkill(skill);
        if (added && semanticSearch != null) {
            semanticSearch.indexSkill(skill);
        }
        return added;
    }

    /**
     * Semantic search integration.
     */
    public List<Skill> semanticSearch(String query, int topK) {
        if (semanticSearch == null) {
            return List.of();
        }
        return semanticSearch.search(query, topK).stream()
            .map(SkillSearchResult::getSkill)
            .collect(Collectors.toList());
    }
}
```

### 2. Integration with ExecutionTracker

```java
// Enhanced ExecutionTracker with learning
public class ExecutionTracker {
    // Existing code...

    /**
     * Auto-learn from completed sequence.
     */
    public void autoLearnFromSequence(ExecutionSequence sequence) {
        if (!sequence.isSuccessful()) {
            return;
        }

        // Add to learning queue
        SkillLearningSystem learningSystem = SkillLearningSystemImpl.getInstance();
        learningSystem.acquireFromRecentExecutions();
    }
}
```

### 3. Integration with HTNPlanner

```java
// HTN methods can use composite skills
public class HTNMethod {
    // Existing code...

    /**
     * Create method from composite skill.
     */
    public static HTNMethod fromCompositeSkill(
        CompositeSkill skill,
        String taskName
    ) {
        HTNMethod.Builder builder = HTNMethod.builder()
            .methodName(taskName + "_method")
            .taskName(taskName);

        // Add subtasks from components
        for (ComponentSkill component : skill.getComponents()) {
            builder.addSubtask(HTNTask.primitive(
                component.getSkill().getName()
            ).build());
        }

        return builder.build();
    }
}
```

### 4. Integration with ActionExecutor

```java
// ActionExecutor can use composite skills
public class ActionExecutor {
    // Existing code...

    /**
     * Execute composite skill.
     */
    public ActionResult executeCompositeSkill(
        CompositeSkill skill,
        Map<String, Object> context
    ) {
        // Execute components in order
        for (ComponentSkill component : skill.getComponents()) {
            ActionResult result = executeSkill(
                component.getSkill(),
                mergeContext(context, component.getParameters())
            );

            if (!result.isSuccess() && component.getExecutionMode()
                    != ComponentSkill.ExecutionMode.PARALLEL) {
                return result; // Fail on first error for sequential
            }
        }

        return ActionResult.success();
    }
}
```

### 5. Integration with LLM Clients

```java
// LLM-assisted skill generation
public class SkillGenerator {
    /**
     * Generate skill code with LLM.
     */
    public String generateSkillCode(
        PatternExtractor.Pattern pattern,
        LLMClient llmClient
    ) {
        String prompt = buildGenerationPrompt(pattern);

        LLMRequest request = LLMRequest.builder()
            .prompt(prompt)
            .maxTokens(1000)
            .temperature(0.3)
            .build();

        LLMResponse response = llmClient.complete(request);
        return extractCodeFromResponse(response);
    }

    private String buildGenerationPrompt(PatternExtractor.Pattern pattern) {
        return String.format("""
            Generate a JavaScript skill for Minecraft with these requirements:

            Pattern: %s
            Actions: %s
            Success Rate: %.1f%%

            Requirements:
            - Use parameterized template with {{variable}} syntax
            - Include error handling
            - Add comments explaining the code
            - Return true on success, false on failure

            Generate only the code, no explanation.
            """,
            pattern.getName(),
            pattern.getActionSequence(),
            pattern.getSuccessRate() * 100
        );
    }
}
```

---

## Implementation Roadmap

### Phase 1: Enhanced Acquisition (2 weeks)

**Week 1:**
- [ ] Enhance PatternExtractor with hierarchical patterns
- [ ] Add conditional pattern detection
- [ ] Implement LLM-assisted description generation

**Week 2:**
- [ ] Enhance SkillAutoGenerator with LLM code generation
- [ ] Add metadata extraction
- [ ] Implement validation framework

**Deliverables:**
- Enhanced pattern extraction
- LLM-assisted skill generation
- Comprehensive skill metadata

### Phase 2: Semantic Retrieval (2 weeks)

**Week 1:**
- [ ] Implement SemanticSkillSearch
- [ ] Integrate vector embeddings
- [ ] Build skill indexing

**Week 2:**
- [ ] Implement ContextAwareRetriever
- [ ] Add result ranking
- [ ] Integrate with SkillLibrary

**Deliverables:**
- Semantic skill search
- Context-aware retrieval
- Vector-based indexing

### Phase 3: Refinement System (3 weeks)

**Week 1:**
- [ ] Implement SkillVersionControl
- [ ] Add version tracking
- [ ] Build comparison tools

**Week 2:**
- [ ] Implement RefinementAnalyzer
- [ ] Add failure pattern detection
- [ ] Build LLM-assisted suggestions

**Week 3:**
- [ ] Implement AutoRefinementLoop
- [ ] Add automatic refinement
- [ ] Integrate with SkillLibrary

**Deliverables:**
- Version control for skills
- Automated refinement
- Performance tracking

### Phase 4: Skill Composition (3 weeks)

**Week 1:**
- [ ] Implement SkillComposer
- [ ] Add goal decomposition
- [ ] Build dependency resolution

**Week 2:**
- [ ] Implement conflict detection
- [ ] Add conflict resolution
- [ ] Build CompositeSkill

**Week 3:**
- [ ] Integrate with HTNPlanner
- [ ] Add optimization
- [ ] Implement validation

**Deliverables:**
- Complex skill composition
- Dependency management
- Conflict resolution

### Phase 5: Integration & Testing (2 weeks)

**Week 1:**
- [ ] Integrate all components
- [ ] Add unified API
- [ ] Implement lifecycle management

**Week 2:**
- [ ] Write comprehensive tests
- [ ] Add performance benchmarks
- [ ] Document APIs

**Deliverables:**
- Unified skill learning system
- Complete test coverage
- API documentation

### Total Timeline: 12 weeks

---

## Usage Examples

### Example 1: Acquire Skills from Execution

```java
// Agent executes a task successfully
ExecutionSequence sequence = ExecutionTracker.getInstance()
    .endTracking(agentId, true);

// Automatically learn from it
SkillLearningSystem learning = SkillLearningSystemImpl.getInstance();
int newSkills = learning.acquireFromRecentExecutions();

System.out.println("Learned " + newSkills + " new skills");
```

### Example 2: Search for Skills

```java
// Find skills for building a tree farm
SkillLearningSystem learning = SkillLearningSystemImpl.getInstance();

List<Skill> skills = learning.findApplicable(
    "build an automated tree farm with storage",
    currentState,
    5
);

for (Skill skill : skills) {
    System.out.println(skill.getName() + ": " + skill.getDescription());
}
```

### Example 3: Compose Complex Skill

```java
// Compose a complex farming skill
SkillLearningSystem learning = SkillLearningSystemImpl.getInstance();

CompositeSkill treeFarm = learning.composeSkill(
    "automated tree farm with storage and lighting",
    initialState
);

System.out.println("Composed skill with " +
    treeFarm.getComponents().size() + " components");
System.out.println("Confidence: " + treeFarm.getConfidence());
```

### Example 4: Refine Underperforming Skill

```java
// Analyze a skill that's performing poorly
SkillLearningSystem learning = SkillLearningSystemImpl.getInstance();

RefinementReport report = learning.analyzeSkill("spiralMine");

System.out.println("Recommendation: " + report.getRecommendation());
for (RefinementSuggestion suggestion : report.getSuggestions()) {
    System.out.println("- " + suggestion.getDescription());
    System.out.println("  Impact: " + suggestion.getEstimatedImpact() + "%");
}

// Apply top suggestion
if (!report.getSuggestions().isEmpty()) {
    RefinementSuggestion top = report.getSuggestions().get(0);
    ExecutableSkill refined = learning.applyRefinement(
        "spiralMine",
        top
    );
    System.out.println("Created refined skill: " + refined.getName());
}
```

### Example 5: Version Control

```java
// Get version history
SkillLearningSystem learning = SkillLearningSystemImpl.getInstance();

List<SkillVersion> history = learning.getVersionHistory("spiralMine");

System.out.println("Version history for spiralMine:");
for (SkillVersion version : history) {
    System.out.println("v" + version.getVersionNumber() + ": " +
        version.getChangeDescription());
    System.out.println("  Success rate: " +
        version.getStats().getSuccessRate());
}

// Revert if needed
boolean reverted = learning.revertToVersion("spiralMine", 2);
if (reverted) {
    System.out.println("Reverted to v2");
}
```

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testPatternExtraction() {
    // Create test sequences
    List<ExecutionSequence> sequences = createTestSequences();

    // Extract patterns
    PatternExtractor extractor = new PatternExtractor();
    List<PatternExtractor.Pattern> patterns =
        extractor.extractPatterns(sequences);

    // Verify patterns found
    assertFalse(patterns.isEmpty());
    assertTrue(patterns.get(0).getFrequency() >= 3);
    assertTrue(patterns.get(0).getSuccessRate() >= 0.7);
}

@Test
public void testSemanticSearch() {
    // Index skills
    SemanticSkillSearch search = new SemanticSkillSearch(...);
    search.indexSkill(createTestSkill("digStaircase",
        "Dig a staircase downwards"));
    search.indexSkill(createTestSkill("buildPlatform",
        "Build a flat platform"));

    // Search
    List<SkillSearchResult> results = search.search(
        "create stairs going down",
        5
    );

    // Verify results
    assertFalse(results.isEmpty());
    assertEquals("digStaircase", results.get(0).getSkill().getName());
    assertTrue(results.get(0).getScore() > 0.7);
}

@Test
public void testSkillComposition() {
    // Compose skill
    SkillComposer composer = new SkillComposer(...);
    CompositeSkill skill = composer.composeSkill(
        "build tree farm",
        initialState
    );

    // Verify composition
    assertNotNull(skill);
    assertFalse(skill.getComponents().isEmpty());
    assertTrue(skill.getConfidence() > 0.5);
}
```

### Integration Tests

```java
@Test
public void testEndToEndSkillLearning() {
    // 1. Execute tasks
    ExecutionTracker tracker = ExecutionTracker.getInstance();
    tracker.startTracking(agentId, "mine diamonds");
    // ... execute mining actions ...
    tracker.endTracking(agentId, true);

    // 2. Acquire skills
    SkillLearningSystem learning = SkillLearningSystemImpl.getInstance();
    int newSkills = learning.acquireFromRecentExecutions();
    assertTrue(newSkills > 0);

    // 3. Search for skills
    List<Skill> skills = learning.search("mine diamonds", 5);
    assertFalse(skills.isEmpty());

    // 4. Use skill
    Skill skill = skills.get(0);
    ActionResult result = executor.executeSkill(skill, context);
    assertTrue(result.isSuccess());
}
```

### Performance Tests

```java
@Test
public void testLargeScaleSearch() {
    // Index 1000 skills
    for (int i = 0; i < 1000; i++) {
        search.indexSkill(createRandomSkill());
    }

    // Measure search time
    long start = System.currentTimeMillis();
    List<SkillSearchResult> results = search.search("mining skill", 10);
    long duration = System.currentTimeMillis() - start;

    // Verify performance
    assertTrue(duration < 100); // Should complete in <100ms
    assertEquals(10, results.size());
}
```

---

## Future Enhancements

### Short-term (6 months)

1. **Multi-Objective Optimization**
   - Optimize skills for multiple criteria (speed, success, resource usage)
   - Pareto frontier analysis for skill selection

2. **Transfer Learning**
   - Transfer skills between agents
   - Learn from other agents' executions

3. **Explainable AI**
   - Explain why a skill was selected
   - Visualize skill composition

### Long-term (12+ months)

1. **Meta-Learning**
   - Learn how to learn skills faster
   - Adapt acquisition strategies

2. **Federated Learning**
   - Share skills across instances
   - Privacy-preserving aggregation

3. **Neural Symbolic Integration**
   - Combine neural networks with symbolic reasoning
   - Hybrid skill representations

---

## Conclusion

This skill learning system design provides a comprehensive framework for automatic skill acquisition, semantic retrieval, continuous refinement, and intelligent composition. By building on the existing Voyager-inspired foundation and adding advanced capabilities, Steve AI agents will be able to:

1. **Learn from experience** - Automatically acquire skills from successful executions
2. **Find relevant skills** - Use semantic search to locate applicable skills
3. **Improve continuously** - Track performance and refine skills over time
4. **Compose complex behaviors** - Build sophisticated skills from simple components

The system is designed to be modular, extensible, and well-integrated with existing Steve AI components, providing a solid foundation for advanced AI behavior in Minecraft.

---

**Document Version:** 1.0
**Last Updated:** 2026-03-02
**Author:** Claude Orchestrator
**Status:** Design Complete - Ready for Implementation
