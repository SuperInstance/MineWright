# God Class Refactoring Plan

**Project:** Steve AI - MineWright Mod
**Team:** Team 4 - Code Quality Fixes
**Week:** 3 P2 Code Quality
**Date:** 2026-03-03
**Status:** Analysis Complete - Ready for Week 4 Implementation

---

## Executive Summary

This document provides a comprehensive analysis and refactoring plan for 11 "god classes" (files exceeding 800 lines) in the Steve AI codebase. The analysis identifies distinct responsibilities, proposes class extractions, maps dependencies, estimates effort, and provides migration strategies for each file.

**Total Lines to Refactor:** ~12,800 lines across 11 files

### Priority Classification

| Priority | Files | Rationale |
|----------|-------|-----------|
| **P1 - High Impact** | CompanionMemory, SmartCascadeRouter | Core systems used throughout codebase |
| **P2 - Medium Impact** | ProactiveDialogueManager, FailureResponseGenerator | Important but isolated functionality |
| **P3 - Lower Impact** | MineWrightConfig, ForemanOfficeGUI | Configuration and UI, less critical to core logic |

---

## File-by-File Analysis

### 1. CompanionMemory.java (1,890 lines)

**Location:** `src/main/java/com/minewright/memory/CompanionMemory.java`

**Current Responsibilities:**
- Memory storage (episodic, semantic, emotional, conversational, working)
- Vector search integration
- Relationship tracking (rapport, trust, milestones)
- NBT persistence
- Personality management
- Memory scoring and eviction
- Consolidation support

**Distinct Functions Identified:**

1. **Memory Storage Management** (~400 lines)
   - Adding/retrieving episodic memories
   - Managing semantic memories
   - Working memory operations
   - Memory scoring and eviction

2. **Relationship Tracking** (~300 lines)
   - Rapport/trust levels
   - Interaction counting
   - Milestone detection
   - Player preferences

3. **Personality Management** (~400 lines)
   - OCEAN traits (PersonalityProfile inner class)
   - Catchphrases and verbal tics
   - Mood tracking
   - Speech pattern generation

4. **Vector Search Integration** (~200 lines)
   - Embedding generation
   - Semantic similarity search
   - Vector store management

5. **NBT Persistence** (~400 lines)
   - Save/load operations
   - Serialization logic

6. **Conversational Memory** (~190 lines)
   - Inside jokes
   - Discussed topics
   - Phrase usage tracking

**Proposed Class Extractions:**

```
CompanionMemory (orchestrator, ~200 lines)
├── MemoryStore (episodic, semantic, working memory)
├── RelationshipTracker (rapport, trust, milestones)
├── PersonalityProfile (standalone class, already exists)
├── MemoryVectorIndex (vector search integration)
├── MemoryPersistence (NBT save/load)
└── ConversationalMemory (already separate inner class)
```

**Dependency Mapping:**

| New Class | Dependencies | Dependents |
|-----------|--------------|------------|
| MemoryStore | EpisodicMemory, SemanticMemory, WorkingMemoryEntry | CompanionMemory, RelationshipTracker |
| RelationshipTracker | MilestoneTracker, PersonalityProfile | CompanionMemory |
| MemoryVectorIndex | EmbeddingModel, InMemoryVectorStore | CompanionMemory |
| MemoryPersistence | NBT tags, all memory types | CompanionMemory |

**Estimated Effort:** 8 hours
- Extract MemoryStore: 2 hours
- Extract RelationshipTracker: 2 hours
- Extract MemoryVectorIndex: 1.5 hours
- Extract MemoryPersistence: 2 hours
- Testing and validation: 0.5 hours

**Migration Strategy:**
1. Create new classes in same package
2. Move relevant methods with delegation wrappers in CompanionMemory
3. Update imports across codebase (32 files reference CompanionMemory)
4. Run tests to validate behavior preservation
5. Remove delegation wrappers once confirmed working

---

### 2. MineWrightConfig.java (1,730 lines)

**Location:** `src/main/java/com/minewright/config/MineWrightConfig.java`

**Current Responsibilities:**
- Configuration loading from TOML
- All config sections (AI, OPENAI, BEHAVIOR, VOICE, etc.)
- Environment variable resolution
- Config validation

**Distinct Functions Identified:**

1. **Config Loading Infrastructure** (~300 lines)
   - Forge config registration
   - File loading
   - Environment variable resolution

2. **AI/LLM Configuration** (~200 lines)
   - OpenAI settings
   - Groq settings
   - z.ai/GLM settings

3. **Behavior Configuration** (~200 lines)
   - Personality settings
   - Dialogue settings
   - Learning parameters

4. **Voice Configuration** (~150 lines)
   - TTS/STT settings
   - Voice integration config

5. **Feature Flags** (~200 lines)
   - Hive Mind settings
   - Skill library config
   - Cascade router config

6. **Performance Tuning** (~150 lines)
   - Pathfinding settings
   - Cache settings
   - Multi-agent coordination

**Proposed Class Extractions:**

```
MineWrightConfig (main loader, ~200 lines)
├── AIConfig (OpenAI, Groq, z.ai settings)
├── BehaviorConfig (personality, dialogue)
├── VoiceConfig (TTS/STT)
├── FeatureFlags (hive mind, skills, cascade)
└── PerformanceConfig (pathfinding, caching)
```

**Alternative Approach:** Keep as centralized config but extract to separate config objects loaded by MineWrightConfig. This maintains Forge's config system while improving organization.

**Dependency Mapping:**

| New Class | Dependencies | Dependents |
|-----------|--------------|------------|
| AIConfig | None (data holder) | All LLM clients |
| BehaviorConfig | None | PersonalityProfile, ProactiveDialogueManager |
| VoiceConfig | None | VoiceSystem |
| FeatureFlags | None | Various feature systems |
| PerformanceConfig | None | Pathfinding, caching systems |

**Estimated Effort:** 6 hours
- Create config section classes: 2 hours
- Update MineWrightConfig to use sections: 2 hours
- Update references across codebase: 1.5 hours
- Testing: 0.5 hours

**Migration Strategy:**
1. Create static inner classes for each config section
2. Move relevant config fields into sections
3. Update MineWrightConfig to delegate to sections
4. Update references: `MineWrightConfig.OPENAI_API_KEY` → `MineWrightConfig.ai.openaiApiKey`
5. Maintain backward compatibility with deprecated accessors temporarily

---

### 3. ForemanOfficeGUI.java (1,298 lines)

**Location:** `src/main/java/com/minewright/client/ForemanOfficeGUI.java`

**Current Responsibilities:**
- Screen rendering and layout
- Event handling (mouse, keyboard)
- Message panel management
- Voice integration controls
- Order submission interface

**Distinct Functions Identified:**

1. **Screen Rendering** (~400 lines)
   - Main layout
   - Component rendering
   - Visual effects

2. **Input Handling** (~300 lines)
   - Mouse events
   - Keyboard events
   - Widget interactions

3. **Message Panel** (~250 lines)
   - Message display
   - Scroll management
   - Message history

4. **Voice Controls** (~150 lines)
   - Voice input button
   - Status display
   - Settings access

5. **Order Management** (~200 lines)
   - Order input
   - Order submission
   - Order history

**Proposed Class Extractions:**

```
ForemanOfficeGUI (main screen, ~200 lines)
├── OfficeScreenLayout (rendering)
├── OfficeInputHandler (input events)
├── MessagePanelComponent (messages)
├── VoiceControlComponent (voice)
└── OrderInterfaceComponent (orders)
```

**Dependency Mapping:**

| New Class | Dependencies | Dependents |
|-----------|--------------|------------|
| OfficeScreenLayout | Minecraft GUI classes | ForemanOfficeGUI |
| OfficeInputHandler | GUI event classes | ForemanOfficeGUI |
| MessagePanelComponent | Font renderer | ForemanOfficeGUI |
| VoiceControlComponent | VoiceSystem | ForemanOfficeGUI |
| OrderInterfaceComponent | TaskPlanner | ForemanOfficeGUI |

**Estimated Effort:** 6 hours
- Extract layout component: 1.5 hours
- Extract input handler: 1.5 hours
- Extract message panel: 1.5 hours
- Extract voice/order components: 1 hour
- Testing: 0.5 hours

**Migration Strategy:**
1. Create component classes as package-private in same package
2. Move rendering/input logic to components
3. ForemanOfficeGUI becomes coordinator/delegator
4. Test GUI thoroughly (manual testing required)

---

### 4. ForemanEntity.java (1,242 lines)

**Location:** `src/main/java/com/minewright/entity/ForemanEntity.java`

**Current Responsibilities:**
- Entity lifecycle (spawn, tick, despawn)
- Action coordination
- Communication with other entities
- State management
- Player interaction
- Custom goals

**Distinct Functions Identified:**

1. **Entity Lifecycle** (~300 lines)
   - Spawn initialization
   - Tick updates
   - Death/despawn

2. **Action Coordination** (~250 lines)
   - ActionExecutor management
   - Task execution
   - Goal setting

3. **Communication** (~200 lines)
   - Chat messages
   - Entity coordination
   - Event publishing

4. **State Management** (~200 lines)
   - State machine integration
   - State transitions
   - State queries

5. **Player Interaction** (~150 lines)
   - Command handling
   - Trading
   - Custom interactions

6. **Custom AI Goals** (~142 lines)
   - Foreman-specific goals
   - Look behavior
   - Follow behavior

**Proposed Class Extractions:**

```
ForemanEntity (entity core, ~300 lines)
├── ForemanLifecycle (spawn, tick, death)
├── ForemanActionCoordinator (action management)
├── ForemanCommunication (chat, events)
├── ForemanStateManager (state machine wrapper)
└── ForemanInteractionHandler (player interaction)
```

**Note:** Custom goals should remain in ForemanEntity or move to separate goal classes in `goal/` package.

**Dependency Mapping:**

| New Class | Dependencies | Dependents |
|-----------|--------------|------------|
| ForemanLifecycle | Minecraft entity classes | ForemanEntity |
| ForemanActionCoordinator | ActionExecutor, TaskPlanner | ForemanEntity |
| ForemanCommunication | EventBus, CommunicationBus | ForemanEntity |
| ForemanStateManager | AgentStateMachine | ForemanEntity |
| ForemanInteractionHandler | Player, CommandRegistry | ForemanEntity |

**Estimated Effort:** 7 hours
- Extract lifecycle manager: 2 hours
- Extract action coordinator: 1.5 hours
- Extract communication: 1.5 hours
- Extract state manager: 1 hour
- Extract interaction handler: 0.5 hours
- Testing: 0.5 hours

**Migration Strategy:**
1. Create inner classes (can be private to ForemanEntity)
2. Move methods to appropriate classes
3. ForemanEntity delegates to inner classes
4. Maintain public API for external consumers
5. Test entity behavior thoroughly

---

### 5. MentorshipManager.java (1,219 lines)

**Location:** `src/main/java/com/minewright/mentorship/MentorshipManager.java`

**Current Responsibilities:**
- Lesson planning
- Student progress tracking
- Teaching sessions
- Skill assessment
- Curriculum management

**Distinct Functions Identified:**

1. **Lesson Management** (~350 lines)
   - Lesson creation
   - Lesson scheduling
   - Lesson content

2. **Student Progress** (~300 lines)
   - Progress tracking
   - Assessment
   - Milestone tracking

3. **Teaching Sessions** (~300 lines)
   - Session execution
   - Feedback
   - Adaptation

4. **Curriculum Management** (~269 lines)
   - Skill trees
   - Prerequisites
   - Learning paths

**Proposed Class Extractions:**

```
MentorshipManager (orchestrator, ~200 lines)
├── LessonPlanner (lesson creation and scheduling)
├── StudentProgressTracker (progress and assessment)
├── TeachingSessionExecutor (session management)
└── CurriculumManager (skill trees and learning paths)
```

**Dependency Mapping:**

| New Class | Dependencies | Dependents |
|-----------|--------------|------------|
| LessonPlanner | Skill, Task | MentorshipManager |
| StudentProgressTracker | CompanionMemory | MentorshipManager |
| TeachingSessionExecutor | ActionExecutor | MentorshipManager |
| CurriculumManager | SkillLibrary | MentorshipManager |

**Estimated Effort:** 6 hours
- Extract lesson planner: 1.5 hours
- Extract progress tracker: 1.5 hours
- Extract session executor: 1.5 hours
- Extract curriculum manager: 1 hour
- Testing: 0.5 hours

**Migration Strategy:**
1. Create new classes in mentorship package
2. Move relevant methods
3. MentorshipManager becomes coordinator
4. Update references (limited usage in codebase)

---

### 6. ProactiveDialogueManager.java (1,061 lines)

**Location:** `src/main/java/com/minewright/dialogue/ProactiveDialogueManager.java`

**Current Responsibilities:**
- Trigger detection (time, weather, biome, proximity)
- Dialogue generation
- Cooldown management
- Speech pattern tracking
- Relationship-aware dialogue
- Analytics and logging

**Distinct Functions Identified:**

1. **Trigger Detection** (~250 lines)
   - Time-based triggers
   - Weather triggers
   - Context triggers (biome, danger)
   - Proximity triggers

2. **Dialogue Generation** (~300 lines)
   - LLM-based generation
   - Fallback comments
   - Relationship-aware selection
   - Speech pattern application

3. **Cooldown Management** (~150 lines)
   - Trigger cooldowns
   - Frequency control
   - State tracking

4. **Speech Pattern Tracking** (~200 lines)
   - Phrase usage counting
   - Recent phrase tracking
   - Pattern analysis

5. **Analytics** (~161 lines)
   - Dialogue decision logging
   - Statistics
   - History tracking

**Proposed Class Extractions:**

```
ProactiveDialogueManager (coordinator, ~150 lines)
├── DialogueTriggerDetector (environment and event detection)
├── DialogueGenerator (LLM and fallback generation)
├── DialogueCooldownManager (frequency control)
├── SpeechPatternTracker (pattern analysis)
└── DialogueAnalytics (logging and statistics)
```

**Dependency Mapping:**

| New Class | Dependencies | Dependents |
|-----------|--------------|------------|
| DialogueTriggerDetector | Level, Biome | ProactiveDialogueManager |
| DialogueGenerator | LLMClient, CompanionMemory | ProactiveDialogueManager |
| DialogueCooldownManager | None (state tracking) | ProactiveDialogueManager |
| SpeechPatternTracker | None (counting) | ProactiveDialogueManager |
| DialogueAnalytics | None (logging) | ProactiveDialogueManager |

**Estimated Effort:** 5 hours
- Extract trigger detector: 1.5 hours
- Extract dialogue generator: 1.5 hours
- Extract cooldown manager: 0.5 hours
- Extract speech pattern tracker: 0.5 hours
- Extract analytics: 0.5 hours
- Testing: 0.5 hours

**Migration Strategy:**
1. Create classes in dialogue package
2. Move methods with delegation wrappers
3. Maintain public API (triggerComment, onTaskCompleted, etc.)
4. Test dialogue behavior

---

### 7. ScriptParser.java (1,029 lines)

**Location:** `src/main/java/com/minewright/script/ScriptParser.java`

**Current Responsibilities:**
- Lexical analysis (tokenization)
- Parsing (YAML and brace formats)
- AST building
- Validation
- NBT persistence

**Distinct Functions Identified:**

1. **Lexical Analysis** (~150 lines)
   - Character-by-character scanning
   - Token creation
   - Position tracking

2. **YAML Format Parsing** (~200 lines)
   - Indentation-based parsing
   - Node creation
   - Error handling

3. **Brace Format Parsing** (~200 lines)
   - Brace matching
   - Block parsing
   - Sequence detection

4. **AST Building** (~250 lines)
   - Script node creation
   - Action node creation
   - Selector node creation

5. **Validation** (~150 lines)
   - Syntax checking
   - Semantic validation
   - Error reporting

6. **NBT Persistence** (~79 lines)
   - Save/load scripts

**Proposed Class Extractions:**

```
ScriptParser (main entry, ~100 lines)
├── ScriptLexer (lexical analysis)
├── YAMLFormatParser (YAML parsing)
├── BraceFormatParser (brace parsing)
├── ScriptASTBuilder (AST construction)
├── ScriptValidator (validation)
└── ScriptPersistence (NBT save/load)
```

**Dependency Mapping:**

| New Class | Dependencies | Dependents |
|-----------|--------------|------------|
| ScriptLexer | None | YAMLFormatParser, BraceFormatParser |
| YAMLFormatParser | ScriptLexer | ScriptParser |
| BraceFormatParser | ScriptLexer | ScriptParser |
| ScriptASTBuilder | Script nodes | ScriptParser |
| ScriptValidator | Script nodes | ScriptParser |
| ScriptPersistence | NBT tags | ScriptParser |

**Estimated Effort:** 6 hours
- Extract lexer: 1 hour
- Extract YAML parser: 1.5 hours
- Extract brace parser: 1.5 hours
- Extract AST builder: 1 hour
- Extract validator: 0.5 hours
- Extract persistence: 0.5 hours
- Testing: 0.5 hours

**Migration Strategy:**
1. Create classes in script package
2. ScriptParser becomes facade that delegates to format-specific parsers
3. Parse method: determine format → delegate → return result
4. Test all script formats

---

### 8. FailureResponseGenerator.java (943 lines)

**Location:** `src/main/java/com/minewright/personality/FailureResponseGenerator.java`

**Current Responsibilities:**
- Failure context building
- Response generation by personality
- Learning statement generation
- Recovery plan generation
- Help request generation
- Embarrassment response

**Distinct Functions Identified:**

1. **Response Generation Logic** (~400 lines)
   - Personality-based response selection
   - 9 different personality generators
   - Severity-based modulation

2. **Context Building** (~150 lines)
   - FailureContext builder
   - Severity classification
   - Failure type categorization

3. **Learning and Recovery** (~150 lines)
   - Learning statements
   - Recovery plans
   - Immediate fix generation

4. **Special Responses** (~243 lines)
   - Help requests
   - Embarrassment responses
   - Player reassurance

**Proposed Class Extractions:**

```
FailureResponseGenerator (main facade, ~150 lines)
├── PersonalityResponseSelector (9 personality generators)
├── FailureContextBuilder (context creation)
├── LearningAndRecoveryGenerator (learning/recovery plans)
└── SpecialResponseGenerator (help, embarrassment, reassurance)
```

**Dependency Mapping:**

| New Class | Dependencies | Dependents |
|-----------|--------------|------------|
| PersonalityResponseSelector | PersonalityTraits | FailureResponseGenerator |
| FailureContextBuilder | FailureType, SeverityLevel | FailureResponseGenerator |
| LearningAndRecoveryGenerator | PersonalityTraits | FailureResponseGenerator |
| SpecialResponseGenerator | PersonalityTraits | FailureResponseGenerator |

**Estimated Effort:** 4 hours
- Extract personality selector: 1.5 hours
- Extract context builder: 0.5 hours
- Extract learning/recovery: 1 hour
- Extract special responses: 0.5 hours
- Testing: 0.5 hours

**Migration Strategy:**
1. Create classes in personality package
2. Move personality-specific generators to selector
3. FailureResponseGenerator delegates to specialists
4. Maintain static API for backward compatibility

---

### 9. ConfigDocumentation.java (907 lines)

**Location:** `src/main/java/com/minewright/config/ConfigDocumentation.java`

**Current Responsibilities:**
- Configuration section documentation
- Template generation
- Nested static classes for each section

**Distinct Functions Identified:**

1. **Documentation Structure** (~700 lines)
   - Section definitions (AI, OPENAI, BEHAVIOR, etc.)
   - Field documentation
   - Default values

2. **Template Generation** (~207 lines)
   - TOML template creation
   - Comment formatting
   - File output

**Recommendation:** Move to external markdown documentation file rather than code refactoring.

**Rationale:**
- This is documentation, not logic
- Static strings don't belong in Java code
- Markdown is more maintainable
- Can be processed by documentation tools

**Alternative:** If keeping in code, extract to separate documentation classes:

```
ConfigDocumentation (main class, ~50 lines)
├── ConfigSectionDefinitions (section definitions, ~600 lines)
└── ConfigTemplateGenerator (template generation, ~200 lines)
```

**Estimated Effort:** 2 hours (to move to markdown) or 3 hours (to refactor)

**Migration Strategy (Markdown Approach):**
1. Create `config/TEMPLATE.toml.md` with all documentation
2. Create `config/sections/` directory with per-section markdown
3. Add script to generate TOML from markdown (if needed)
4. Deprecate ConfigDocumentation class

---

### 10. MilestoneTracker.java (898 lines)

**Location:** `src/main/java/com/minewright/memory/MilestoneTracker.java`

**Current Responsibilities:**
- Milestone storage
- Milestone checking and detection
- Milestone message generation
- NBT persistence
- Anniversary tracking

**Distinct Functions Identified:**

1. **Milestone Storage** (~150 lines)
   - Milestone maps
   - Counters
   - First occurrence tracking

2. **Milestone Detection** (~300 lines)
   - Check method with switch statements
   - Anniversary detection
   - Count-based detection

3. **Message Generation** (~250 lines)
   - Generate milestone message
   - Personality context integration
   - Message templates

4. **NBT Persistence** (~100 lines)
   - Save to NBT
   - Load from NBT

5. **Milestone Types** (~98 lines)
   - Enum definitions
   - Type-specific logic

**Proposed Class Extractions:**

```
MilestoneTracker (coordinator, ~150 lines)
├── MilestoneStore (storage and queries)
├── MilestoneDetector (checking logic)
├── MilestoneMessageGenerator (message creation)
└── MilestonePersistence (NBT save/load)
```

**Dependency Mapping:**

| New Class | Dependencies | Dependents |
|-----------|--------------|------------|
| MilestoneStore | Milestone, MilestoneType | MilestoneTracker |
| MilestoneDetector | MilestoneType | MilestoneTracker |
| MilestoneMessageGenerator | CompanionMemory, PersonalityProfile | MilestoneTracker |
| MilestonePersistence | NBT tags | MilestoneTracker |

**Estimated Effort:** 4 hours
- Extract store: 1 hour
- Extract detector: 1 hour
- Extract message generator: 1 hour
- Extract persistence: 0.5 hours
- Testing: 0.5 hours

**Migration Strategy:**
1. Create classes in memory package
2. Move relevant methods
3. MilestoneTracker delegates to components
4. Update CompanionMemory reference

---

### 11. SmartCascadeRouter.java (899 lines)

**Location:** `src/main/java/com/minewright/llm/SmartCascadeRouter.java`

**Current Responsibilities:**
- Complexity assessment
- Model selection and routing
- Fallback chain management
- Local/cloud LLM coordination
- Vision request handling
- Metrics tracking
- Failure tracking

**Distinct Functions Identified:**

1. **Complexity Assessment** (~150 lines)
   - Preprocessing
   - Heuristic analysis
   - Complexity classification

2. **Model Routing** (~200 lines)
   - Route by complexity
   - Handle trivial/simple/moderate/complex
   - Fallback chains

3. **API Communication** (~250 lines)
   - Local vision requests
   - Cloud vision requests
   - Cloud text requests
   - Response parsing

4. **Failure Tracking** (~100 lines)
   - Skip model logic
   - Record success/failure
   - Reset logic

5. **Metrics and Monitoring** (~100 lines)
   - Request counting
   - Cost tracking
   - Statistics logging

**Proposed Class Extractions:**

```
SmartCascadeRouter (coordinator, ~150 lines)
├── ComplexityAnalyzer (assessment)
├── ModelRouter (routing logic)
├── LLMAPIConnector (API communication)
├── FailureTracker (failure management)
└── RouterMetrics (monitoring)
```

**Dependency Mapping:**

| New Class | Dependencies | Dependents |
|-----------|--------------|------------|
| ComplexityAnalyzer | TaskComplexity | SmartCascadeRouter |
| ModelRouter | LocalLLMClient, HttpClient | SmartCascadeRouter |
| LLMAPIConnector | HttpClient, JsonObject | SmartCascadeRouter |
| FailureTracker | ConcurrentHashMap | SmartCascadeRouter |
| RouterMetrics | AtomicLong | SmartCascadeRouter |

**Estimated Effort:** 5 hours
- Extract complexity analyzer: 1 hour
- Extract model router: 1.5 hours
- Extract API connector: 1.5 hours
- Extract failure tracker: 0.5 hours
- Extract metrics: 0.5 hours
- Testing: 0.5 hours

**Migration Strategy:**
1. Create classes in llm package (or llm.cascade subpackage)
2. Move relevant methods
3. SmartCascadeRouter delegates to specialists
4. Maintain public API (processWithCascade, etc.)

---

## Recommended Priority Order

### Phase 1: High Impact, Low Risk (Week 4, Days 1-2)

1. **ConfigDocumentation** → Markdown (2 hours)
   - Lowest risk, immediate benefit
   - Reduces code bloat significantly

2. **FailureResponseGenerator** (4 hours)
   - Isolated functionality
   - Clear separation points
   - Easy to test

3. **MilestoneTracker** (4 hours)
   - Well-defined boundaries
   - Limited external dependencies

### Phase 2: Core Systems (Week 4, Days 3-4)

4. **SmartCascadeRouter** (5 hours)
   - Critical path for LLM operations
   - Needs careful testing

5. **ProactiveDialogueManager** (5 hours)
   - Important but isolated
   - Can be tested independently

### Phase 3: Complex Refactoring (Week 4, Days 5-6)

6. **ScriptParser** (6 hours)
   - Complex parsing logic
   - Multiple format support

7. **MentorshipManager** (6 hours)
   - Moderate complexity
   - Limited usage in codebase

### Phase 4: Entity and Memory (Week 4, Days 7-8)

8. **ForemanEntity** (7 hours)
   - Core entity class
   - Requires extensive testing

9. **CompanionMemory** (8 hours)
   - Most complex refactoring
   - Critical system used throughout

### Phase 5: Remaining (Week 4, Days 9-10)

10. **MineWrightConfig** (6 hours)
    - Config system affects everything
    - Needs careful migration

11. **ForemanOfficeGUI** (6 hours)
    - UI refactoring
    - Requires manual testing

**Total Estimated Effort:** 59 hours (~7.5 days)

---

## General Migration Patterns

### Pattern 1: Delegation Wrapper

```java
// Before: All logic in god class
public class GodClass {
    public void doSomething() {
        // 50 lines of logic
    }
}

// After: Extract class with delegation
public class GodClass {
    private final ExtractedClass extracted = new ExtractedClass();

    public void doSomething() {
        extracted.doSomething();
    }
}
```

### Pattern 2: Inner Class → Separate Class

```java
// Before: Inner class
public class GodClass {
    public static class Inner {
        // logic
    }
}

// After: Separate class in same package
public class ExtractedClass {
    // logic
}
```

### Pattern 3: Facade Pattern

```java
// Before: Monolithic class with multiple responsibilities
public class GodClass {
    public void responsibility1() {}
    public void responsibility2() {}
    public void responsibility3() {}
}

// After: Facade coordinating specialists
public class GodClass {
    private final Specialist1 s1 = new Specialist1();
    private final Specialist2 s2 = new Specialist2();
    private final Specialist3 s3 = new Specialist3();

    public void responsibility1() { s1.execute(); }
    public void responsibility2() { s2.execute(); }
    public void responsibility3() { s3.execute(); }
}
```

---

## Testing Strategy

### Unit Testing

Each extracted class should have:
1. **Constructor tests** - Verify proper initialization
2. **Method tests** - Test each public method
3. **Edge cases** - Null inputs, empty collections, boundary values
4. **Integration tests** - Test interaction with dependencies

### Integration Testing

For god classes that coordinate multiple components:
1. **Workflow tests** - Test complete use cases
2. **State tests** - Verify state transitions
3. **Persistence tests** - Test save/load functionality

### Regression Testing

Before and after refactoring:
1. Run existing test suite
2. Add new tests for extracted classes
3. Compare behavior metrics (if applicable)

---

## Risk Assessment

| File | Risk Level | Concerns | Mitigation |
|------|-----------|----------|------------|
| CompanionMemory | HIGH | Core system, many dependencies | Comprehensive testing, phased rollout |
| SmartCascadeRouter | MEDIUM | LLM routing critical | Extensive API testing |
| ForemanEntity | HIGH | Entity core, Minecraft integration | In-game testing, backup saves |
| MineWrightConfig | MEDIUM | Affects all config loading | Backward compatibility layer |
| ScriptParser | LOW | Isolated system | Unit tests for all formats |
| ProactiveDialogueManager | LOW | Isolated feature | Test trigger detection |
| FailureResponseGenerator | LOW | Pure functions | Easy to test |
| MilestoneTracker | LOW | Well-defined boundaries | State transition tests |
| MentorshipManager | LOW | Limited usage | Basic integration tests |
| ForemanOfficeGUI | MEDIUM | UI requires manual testing | Screenshot testing |
| ConfigDocumentation | NONE | Documentation only | No runtime impact |

---

## Success Criteria

### Code Quality Metrics

- **Maximum Lines per Class:** < 500 lines
- **Maximum Methods per Class:** < 30 methods
- **Maximum Parameters per Method:** < 5 parameters
- **Cyclomatic Complexity:** < 15 per method

### Functional Requirements

- All existing tests pass
- No behavioral changes
- Performance maintained or improved
- No new warnings

### Documentation Requirements

- JavaDoc on all public classes
- JavaDoc on all public methods
- Update CLAUDE.md if architecture changes

---

## Next Steps (Week 4 Implementation)

1. **Day 1:** Start with Phase 1 (ConfigDocumentation, FailureResponseGenerator)
2. **Day 2:** Complete Phase 1 (MilestoneTracker)
3. **Day 3:** Begin Phase 2 (SmartCascadeRouter)
4. **Day 4:** Complete Phase 2 (ProactiveDialogueManager)
5. **Day 5:** Begin Phase 3 (ScriptParser)
6. **Day 6:** Complete Phase 3 (MentorshipManager)
7. **Day 7-8:** Phase 4 (ForemanEntity, CompanionMemory)
8. **Day 9-10:** Phase 5 (MineWrightConfig, ForemanOfficeGUI)

### Daily Workflow

1. Morning: Create new classes, move methods
2. Midday: Update references, add delegation
3. Afternoon: Test, debug, validate
4. End of day: Commit changes, update documentation

---

## Appendix: File Summary Table

| File | Lines | Priority | Est. Hours | Phase | Risk |
|------|-------|----------|-----------|-------|------|
| CompanionMemory.java | 1,890 | P1 | 8 | 4 | HIGH |
| MineWrightConfig.java | 1,730 | P3 | 6 | 5 | MEDIUM |
| ForemanOfficeGUI.java | 1,298 | P3 | 6 | 5 | MEDIUM |
| ForemanEntity.java | 1,242 | P2 | 7 | 4 | HIGH |
| MentorshipManager.java | 1,219 | P2 | 6 | 3 | LOW |
| ProactiveDialogueManager.java | 1,061 | P2 | 5 | 2 | LOW |
| ScriptParser.java | 1,029 | P2 | 6 | 3 | LOW |
| FailureResponseGenerator.java | 943 | P2 | 4 | 1 | LOW |
| ConfigDocumentation.java | 907 | P3 | 2 | 1 | NONE |
| MilestoneTracker.java | 898 | P2 | 4 | 1 | LOW |
| SmartCascadeRouter.java | 899 | P1 | 5 | 2 | MEDIUM |
| **TOTAL** | **12,796** | - | **59** | - | - |

---

**Document Version:** 1.0
**Created By:** Team 4 - Code Quality Fixes
**Last Updated:** 2026-03-03
**Status:** Ready for Implementation Week 4
