# Skill Learning System - Quick Reference

**Version:** 1.0
**Date:** 2026-03-02
**Purpose:** Quick reference for developers

---

## System Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────────┐
│                    SKILL LEARNING SYSTEM                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐           │
│  │ACQUISITION │───▶│ RETRIEVAL  │◀───│ REFINEMENT  │           │
│  │            │    │            │    │            │           │
│  │ • Extract  │    │ • Semantic │    │ • Track    │           │
│  │ • Generate │    │ • Search   │    │ • Evaluate │           │
│  │ • Validate │    │ • Rank     │    │ • Improve  │           │
│  └────────────┘    └────────────┘    └────────────┘           │
│           │                   │                   │            │
│           └───────────────────┼───────────────────┘            │
│                               ▼                                │
│                    ┌────────────┐                              │
│                    │COMPOSITION │                              │
│                    │            │                              │
│                    │ • Combine  │                              │
│                    │ • Resolve  │                              │
│                    │ • Optimize │                              │
│                    └────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Key Classes Overview

### Acquisition

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `PatternExtractor` | Find patterns in sequences | `extractPatterns()`, `extractHierarchicalPatterns()` |
| `SkillAutoGenerator` | Generate skills from patterns | `generateSkill()`, `generateSkillWithLLM()` |
| `SkillMetadata` | Rich skill metadata | `getName()`, `getParameters()`, `getPreconditions()` |

### Retrieval

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `SemanticSkillSearch` | Vector-based search | `search(query, k)`, `indexSkill(skill)` |
| `ContextAwareRetriever` | Context-aware retrieval | `findApplicable(goal, state, k)` |
| `SkillSearchResult` | Search result with score | `getSkill()`, `getScore()` |

### Refinement

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `SkillVersionControl` | Version management | `createVersion()`, `revertToVersion()` |
| `RefinementAnalyzer` | Performance analysis | `analyzeSkill(skillId)` |
| `AutoRefinementLoop` | Automatic improvement | `start()`, `stop()` |

### Composition

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `SkillComposer` | Compose complex skills | `composeSkill(goal, state)` |
| `CompositeSkill` | Multi-component skill | `getComponents()`, `getConfidence()` |
| `CompositionPlan` | Composition plan | `getSkills()`, `getDependencies()` |

---

## Common Usage Patterns

### Pattern 1: Auto-Learn from Execution

```java
// Agent completes task
tracker.endTracking(agentId, true);

// Learn automatically
int newSkills = learning.acquireFromRecentExecutions();
```

### Pattern 2: Search by Natural Language

```java
// Find relevant skills
List<Skill> skills = learning.search("build spiral staircase", 5);

// Get best match
Skill best = skills.get(0);
```

### Pattern 3: Context-Aware Retrieval

```java
// Find skills for current situation
List<Skill> applicable = learning.findApplicable(
    "mine diamonds",
    currentWorldState,
    10
);
```

### Pattern 4: Compose Complex Skills

```java
// Build complex behavior
CompositeSkill farm = learning.composeSkill(
    "automated tree farm with storage",
    initialState
);
```

### Pattern 5: Refine Underperforming Skills

```java
// Analyze and improve
RefinementReport report = learning.analyzeSkill("spiralMine");
ExecutableSkill refined = learning.applyRefinement(
    "spiralMine",
    report.getTopSuggestion()
);
```

---

## API Quick Reference

### SkillLearningSystem (Main API)

```java
// Acquisition
int acquireFromRecentExecutions();
Skill generateSkillWithLLM(Pattern pattern, LLMClient client);

// Retrieval
List<SkillSearchResult> search(String query, int k);
List<Skill> searchForTask(Task task, int k);
List<Skill> findApplicable(String goal, HTNWorldState state, int k);

// Refinement
RefinementReport analyzeSkill(String skillId);
ExecutableSkill applyRefinement(String skillId, RefinementSuggestion s);
List<SkillVersion> getVersionHistory(String skillId);
boolean revertToVersion(String skillId, int version);

// Composition
CompositeSkill composeSkill(String goal, HTNWorldState state);
ValidationResult validateComposite(CompositeSkill skill);

// Lifecycle
void start();
void stop();
Map<String, Object> getStatus();
```

---

## Integration Points

### With SkillLibrary

```java
// Add skill with semantic indexing
library.addSkillWithIndexing(skill);

// Semantic search
List<Skill> results = library.semanticSearch("mining skill", 10);
```

### With ExecutionTracker

```java
// Auto-learn on completion
tracker.endTracking(agentId, true);
// Automatically triggers learning
```

### With HTNPlanner

```java
// Create method from composite skill
HTNMethod method = HTNMethod.fromCompositeSkill(skill, taskName);
```

### With ActionExecutor

```java
// Execute composite skill
ActionResult result = executor.executeCompositeSkill(skill, context);
```

---

## Configuration

### Embedding Model

```java
EmbeddingModel model = EmbeddingModel.createLocal();
// Or use OpenAI
EmbeddingModel model = new OpenAIEmbeddingModel(apiKey);
```

### Vector Store

```java
InMemoryVectorStore<SkillReference> store =
    new InMemoryVectorStore<>(384); // dimension
```

### LLM Client

```java
LLMClient client = LLMClient.builder()
    .provider("groq")
    .apiKey(key)
    .model("llama3-70b-8192")
    .build();
```

---

## Testing Commands

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests SemanticSkillSearchTest

# Run with coverage
./gradlew test jacocoTestReport
```

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| Acquisition Rate | 5+ skills/hour | Skills per gameplay hour |
| Search Latency | <100ms | Time for search query |
| Search Precision | 80%+ | Top-1 result relevance |
| Refinement Impact | 20%+ | Success rate improvement |
| Composition Success | 70%+ | Complex skill success rate |

---

## Troubleshooting

### Issue: No Skills Found

**Possible Causes:**
- Query too specific
- Skills not indexed
- Embedding model not initialized

**Solutions:**
- Try broader query terms
- Check `learning.getStatus()` for indexing status
- Verify embedding model initialization

### Issue: Low Search Precision

**Possible Causes:**
- Poor quality embeddings
- Inaccurate skill descriptions
- Insufficient ranking factors

**Solutions:**
- Use better embedding model
- Improve skill descriptions
- Adjust ranking weights

### Issue: Composition Fails

**Possible Causes:**
- Unresolvable dependencies
- Circular dependencies
- Conflicting constraints

**Solutions:**
- Check `CompositionPlan.getDependencies()`
- Review `Conflict` list
- Simplify goal decomposition

---

## File Locations

### Source Files

```
src/main/java/com/minewright/skill/
├── acquisition/
│   ├── PatternExtractor.java (enhanced)
│   ├── SkillAutoGenerator.java (enhanced)
│   └── SkillMetadata.java (new)
├── retrieval/
│   ├── SemanticSkillSearch.java (new)
│   ├── ContextAwareRetriever.java (new)
│   └── SkillSearchResult.java (new)
├── refinement/
│   ├── SkillVersionControl.java (new)
│   ├── RefinementAnalyzer.java (new)
│   └── AutoRefinementLoop.java (new)
├── composition/
│   ├── SkillComposer.java (new)
│   ├── CompositeSkill.java (new)
│   └── CompositionPlan.java (new)
└── SkillLearningSystem.java (new interface)
```

### Test Files

```
src/test/java/com/minewright/skill/
├── acquisition/
│   ├── PatternExtractorTest.java
│   └── SkillAutoGeneratorTest.java
├── retrieval/
│   ├── SemanticSkillSearchTest.java
│   └── ContextAwareRetrieverTest.java
├── refinement/
│   ├── SkillVersionControlTest.java
│   └── RefinementAnalyzerTest.java
├── composition/
│   ├── SkillComposerTest.java
│   └── CompositeSkillTest.java
└── SkillLearningSystemTest.java
```

### Documentation

```
docs/research/
├── SKILL_LEARNING_DESIGN.md (47 pages - complete design)
├── SKILL_LEARNING_SUMMARY.md (executive summary)
└── SKILL_LEARNING_QUICKREF.md (this file)
```

---

## Development Workflow

### 1. Feature Development

```bash
# Create feature branch
git checkout -b feature/skill-learning-enhancement

# Make changes
# ...

# Run tests
./gradlew test

# Commit
git add .
git commit -m "Implement semantic skill search"
```

### 2. Code Review Checklist

- [ ] API design follows guidelines
- [ ] Tests have >80% coverage
- [ ] Documentation updated
- [ ] Performance targets met
- [ ] No breaking changes
- [ ] Error handling complete

### 3. Release Process

```bash
# Merge to main
git checkout main
git merge feature/skill-learning-enhancement

# Tag release
git tag -a v1.1.0 -m "Skill learning system"

# Push
git push origin main --tags
```

---

## Key Concepts

### Skill Lifecycle

```
1. DISCOVER: PatternExtractor finds pattern in sequences
2. GENERATE: SkillAutoGenerator creates skill template
3. INDEX: SemanticSkillSearch creates vector embedding
4. EXECUTE: Agent uses skill for task
5. TRACK: EffectivenessTracker records performance
6. REFINE: RefinementAnalyzer suggests improvements
7. VERSION: SkillVersionControl creates new version
8. COMPOSE: SkillComposer combines into complex skills
```

### Confidence Scores

- **0.9-1.0**: High confidence - use directly
- **0.7-0.9**: Good confidence - consider with alternatives
- **0.5-0.7**: Medium confidence - test before use
- **0.3-0.5**: Low confidence - needs refinement
- **0.0-0.3**: Poor confidence - avoid or remove

### Refinement Recommendations

- **KEEP**: Skill performing well (>80% success)
- **REFINE**: Needs improvement (50-80% success, declining)
- **REPLACE**: Better alternative exists
- **REMOVE**: Not useful or harmful (<30% success)
- **DEPRECATE**: Keep but don't use for new tasks

---

## Frequently Asked Questions

**Q: How do I add a new skill manually?**

A: Use `SkillLibrary.getInstance().addSkillWithIndexing(skill)`

**Q: How do I find skills for a specific task?**

A: Use `learning.searchForTask(task, 10)`

**Q: How do I improve a failing skill?**

A: Call `learning.analyzeSkill(skillId)` and apply suggestions

**Q: How do I combine multiple skills?**

A: Use `learning.composeSkill(goal, initialState)`

**Q: How do I track skill performance?**

A: Check `SkillEffectivenessTracker.getInstance().getRecommendation(skillId)`

---

## Resources

### Internal Documentation

- `SKILL_LEARNING_DESIGN.md` - Complete system design
- `SKILL_LEARNING_SUMMARY.md` - Executive summary
- `CLAUDE.md` - Project overview and architecture

### Related Systems

- `BEHAVIOR_TREES_DESIGN.md` - Behavior tree integration
- `SCRIPT_GENERATION_SYSTEM.md` - LLM→Script pipeline
- `MINECRAFT_AI_SOTA_2024_2025.md` - State-of-the-art comparison

### External References

- Voyager Paper: https://arxiv.org/abs/2305.16291
- Minecraft AI Research: Various academic papers
- Game Bot Patterns: WoWGlider, Honorbuddy documentation

---

**Version:** 1.0
**Last Updated:** 2026-03-02
**Maintainer:** Steve AI Development Team
**For Questions:** See SKILL_LEARNING_DESIGN.md or contact team lead
