# Improvement Roadmap

**Strategic plan for Steve AI development and enhancements.**

Last Updated: 2026-03-03

---

## Executive Summary

Steve AI is 85% production-ready with comprehensive core systems. This roadmap outlines the path to 100% production readiness and beyond.

**Current State:**
- 234 source files, 85,752 lines of production code
- 91 test files, 33,349 lines of test code
- 425 documentation files
- All P0 tests complete (ActionExecutor: 909 lines, AgentStateMachine: 1048 lines)

---

## Phase 1: Production Readiness (Current)

### 1.1 Skill Composition System ✅ IN PROGRESS

**Status:** Implementation started

**Components:**
- [x] `SkillComposer.java` - Compose skills into complex behaviors
- [x] `ComposedSkill.java` - Represents a multi-step skill
- [x] `CompositionStep.java` - Individual step in composition
- [ ] Tests for SkillComposer
- [ ] Integration with ActionExecutor

**Impact:** Enables Voyager-style skill composition (3.3x performance improvement)

### 1.2 Multi-Agent Coordination

**Status:** 50% Complete

**Remaining Work:**
- [ ] Implement Contract Net Protocol bidding
- [ ] Add capability matching
- [ ] Add workload tracking
- [ ] Test with multiple mock agents

**Impact:** Emergent intelligence from agent cooperation

### 1.3 Quality Tools

**Status:** Configured but disabled

**Tasks:**
- [ ] Run `./gradlew checkstyleMain` and fix warnings
- [ ] Run `./gradlew spotbugsMain` and fix bugs
- [ ] Change `ignoreFailures = false` in build.gradle

---

## Phase 2: Feature Completion

### 2.1 Script DSL System

**Purpose:** Declarative automation scripts to reduce LLM token usage

**Components:**
- [ ] Script DSL grammar definition
- [ ] ScriptDSLParser.java
- [ ] ScriptDSLExecutor.java
- [ ] LLM prompt templates for DSL generation

**Example DSL:**
```javascript
SCRIPT "mine_iron"
  TRIGGER "need_iron"
  PRECONDITION "has_pickaxe"
  SEQUENCE {
    PATHFIND nearest("iron_ore")
    MINE "iron_ore" quantity(10)
    RETURN_TO player
  }
  ON_FAIL { SAY "Couldn't find iron!" }
END
```

### 2.2 LLM→Script Generation Pipeline

**Purpose:** Automate script creation from natural language

**Components:**
- [ ] ScriptGenerator.java enhancement
- [ ] Script validation logic
- [ ] Refinement loop based on execution feedback

### 2.3 Small Model Specialization

**Purpose:** Reduce API costs by 40-60%

**Tasks:**
- [ ] Train/fine-tune small models for specific tasks
- [ ] Integrate with CascadeRouter
- [ ] Add fallback to large models

---

## Phase 3: Research & Development

### 3.1 MUD Automation Learning

**Purpose:** Extract principles from 1990s MUD automation for LLM learning

**Research Questions:**
- How did TinTin++/ZMud solve complex problems without LLMs?
- What patterns can LLMs learn from trigger/alias systems?
- How to translate procedural scripts into learnable principles?

### 3.2 DEPS Planning Integration

**Purpose:** Add Detective-style verification to planning

**Components:**
- [ ] Study DEPS paper patterns
- [ ] Implement verification step
- [ ] Add self-correction loop

### 3.3 DreamerV3 World Model

**Purpose:** Enable imagination-based planning

**Research:**
- Study DreamerV3 architecture
- Evaluate applicability to Minecraft
- Design integration approach

---

## Phase 4: Performance Optimization

### 4.1 Memory Optimization

**Issues Identified:**
- Unbounded collections in CompanionMemory
- CopyOnWriteArrayList overhead in emotional memories

**Solutions:**
- Add size limits with LRU eviction
- Replace with ReentrantReadWriteLock
- Profile and optimize hot paths

### 4.2 Pathfinding Optimization

**Current State:** Good performance (<50ms for 100-block paths)

**Improvements:**
- [ ] Parallel path exploration
- [ ] Better chunk caching
- [ ] Dynamic heuristic adjustment

### 4.3 LLM Caching

**Current:** 40-60% hit rate with semantic caching

**Improvements:**
- [ ] Increase cache hit rate to 70%+
- [ ] Add context-aware caching
- [ ] Implement cache warming

---

## Phase 5: Dissertation Completion

### 5.1 Chapter 3: Emotional AI

**Status:** 60% Complete

**Remaining:**
- Complete emotional AI section
- Add 2024-2025 citations
- Integration with code examples

### 5.2 Chapter 6: Architecture

**Status:** In Progress

**Tasks:**
- Add more implementation details
- Discuss limitations
- Future work section

### 5.3 Publication Preparation

**Target Venues:** ICLR, NeurIPS, AAAI

**Requirements:**
- Benchmark results
- Comparison with Voyager, DreamerV3, DEPS
- Novel contribution statement

---

## Metrics & Success Criteria

### Production Readiness

| Metric | Current | Target |
|--------|---------|--------|
| Test Coverage | 40% | 60% |
| Checkstyle | Disabled | Clean |
| SpotBugs | Disabled | Clean |
| Documentation | 425 files | Complete |
| Build Time | 30s | <30s |

### Performance

| Metric | Current | Target |
|--------|---------|--------|
| LLM Latency | 1-5s | <3s avg |
| Cache Hit Rate | 40-60% | 70%+ |
| Pathfinding | <50ms | <30ms |
| Memory/Agent | ~5MB | <3MB |

### Features

| Feature | Current | Target |
|---------|---------|--------|
| Skill Composition | 80% | 100% |
| Multi-Agent | 50% | 100% |
| Script DSL | 0% | 100% |
| Small Models | 0% | 50% |

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| API rate limits | Medium | High | Local LLM fallback |
| Memory leaks | Low | High | Profiling, limits |
| LLM hallucination | Medium | Medium | Validation layer |
| Test flakiness | Low | Medium | Better mocking |

---

## Timeline

**Q1 2026:**
- Complete skill composition system
- Re-enable quality tools
- Improve test coverage to 50%

**Q2 2026:**
- Complete multi-agent coordination
- Implement Script DSL
- Begin dissertation finalization

**Q3 2026:**
- Performance optimization
- Small model integration
- Publication preparation

**Q4 2026:**
- Release candidate
- Documentation finalization
- Community feedback integration

---

## Resource Requirements

### Development
- Java 17+ JDK
- Minecraft Forge 1.20.1
- LLM API keys (OpenAI, Groq, Gemini)
- Optional: Ollama for local inference

### Testing
- JUnit 5
- Mockito
- Minecraft test framework

### Documentation
- Markdown editor
- Diagram tools (Mermaid, PlantUML)

---

## Next Actions

**Immediate (This Session):**
1. ✅ Create SkillComposer, ComposedSkill, CompositionStep
2. [ ] Add tests for SkillComposer
3. [ ] Push improvements to repo

**Short-term (Next Session):**
1. Complete multi-agent coordination bidding
2. Run and fix Checkstyle warnings
3. Run and fix SpotBugs issues

**Medium-term (This Week):**
1. Implement Script DSL grammar
2. Create ScriptDSLParser
3. Integrate with LLM generation

---

*Last Updated: 2026-03-03*
*Next Review: After Phase 1 completion*
