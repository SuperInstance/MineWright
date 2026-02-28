# MineWright R&D Roadmap

**Version:** 1.0
**Last Updated:** 2026-02-28
**Status:** Active Development Roadmap

---

## Executive Summary

This roadmap outlines the research and development path for MineWright's "One Abstraction Away" system - a revolutionary architecture where AI agents (brains) create and refine automation scripts that drive character behaviors, similar to how RTS games separate macro (high-level commands) from micro (unit-level behaviors).

**Key Innovation:** Instead of LLMs controlling every action, they create/refine behavioral scripts that run autonomously, reducing token usage by 10-20x while enabling richer, more responsive agent behaviors.

---

## Model Strategy

### Tiered Model Usage

| Model | Role | Best For | Cost |
|-------|------|----------|------|
| **SmolVLM (Local)** | Vision preprocessing | Screenshots, visual context extraction | FREE |
| **GLM-4.6v (Cloud)** | Complex vision | Confusing screenshots, detailed image analysis | Low |
| **GLM-5 (Cloud)** | Orchestration | Agent coordination, planning, strategy | Medium |
| **GLM-flash (Cloud)** | Reasoning | Back-and-forth dialogue, clarification | Very Low |

### Vision Pipeline

```
Screenshot captured
       ↓
┌──────────────────┐
│ SmolVLM (Local)  │ → Extract visual context, add to prompt
└──────────────────┘
       ↓
Is it confusing?
       ↓
    Yes → GLM-4.6v → Detailed analysis
       ↓
    No  → Continue to planning
       ↓
┌──────────────────┐
│ GLM-5 Planning   │ → Orchestrate agents, create plan
└──────────────────┘
       ↓
Need clarification?
       ↓
    Yes → GLM-flash → Ask user, refine understanding
       ↓
    No  → Execute plan
```

---

## Phase 1: Foundation (Current Sprint)

### 1.1 UX Improvements
**Status:** Ready to implement
**Priority:** High

- [ ] **Mace as Default Agent**
  - Spawn Mace automatically on first world load
  - Mace introduces himself with personality
  - No "Foreman" placeholder names
  - Location: `ForemanEntity.java`, `CrewManager.java`

- [ ] **K Key Enhancement**
  - Opens worker list dialog
  - Simultaneously starts TTS listener for voice commands
  - Visual indicator that voice is active
  - Location: `KeyBindings.java`, `SteveCommandGUI.java`

- [ ] **Conversational Mace**
  - Mace talks during building (not just at start/end)
  - Progress updates with personality
  - Asks clarifying questions naturally
  - Location: `ProactiveDialogueManager.java`

### 1.2 Automation Framework
**Status:** Design complete, ready for implementation
**Priority:** Critical (Killer Feature)

- [ ] **Behavior Tree Infrastructure**
  - `BehaviorTreeManager.java` - Manages behavior trees
  - `Node.java`, `Sequence.java`, `Selector.java`, `Parallel.java`
  - Integration with existing `ActionExecutor`
  - Location: `com.minewright.automation/`

- [ ] **Script DSL**
  - Domain-specific language for automation scripts
  - Scripts stored as text (editable, versionable)
  - Scripts compiled to behavior trees at runtime
  - Location: `com.minewright.automation/ScriptDSL.java`

- [ ] **Script Runner**
  - Tick-based execution (non-blocking)
  - State persistence across game sessions
  - Error recovery and resumption
  - Location: `com.minewright.automation/AutomationRunner.java`

---

## Phase 2: Brain Layer

### 2.1 LLM Script Generation
**Status:** Research complete
**Priority:** High

- [ ] **Script Generator**
  - LLM generates scripts from natural language descriptions
  - Scripts validated before execution
  - Failed scripts trigger refinement loop
  - Location: `com.minewright.automation/ScriptGenerator.java`

- [ ] **Script Refiner**
  - LLM improves scripts based on execution feedback
  - Success rate tracking per script
  - Automatic optimization over time
  - Location: `com.minewright.automation/ScriptRefiner.java`

- [ ] **Pattern Library**
  - Pre-built scripts for common tasks
  - Mining patterns (strip, branch, quarry)
  - Building patterns (house, tower, wall)
  - Farming patterns (wheat, carrot, tree farm)
  - Location: `com.minewright.automation/patterns/`

### 2.2 HTN Integration
**Status:** Full design in HIERARCHICAL_PLANNING_DESIGN.md
**Priority:** Medium

- [ ] **HTN Domain**
  - Define compound tasks (build_house, mine_diamond)
  - Define decomposition methods with preconditions
  - Load default Minecraft domain
  - Location: `com.minewright.htn/HTNDomain.java`

- [ ] **HTN Planner**
  - Recursive task decomposition
  - Method selection by precondition matching
  - Plan caching for common tasks
  - Location: `com.minewright.htn/HTNPlanner.java`

- [ ] **LLM-HTN Bridge**
  - LLM generates new HTN methods
  - Methods learned from successful executions
  - Fallback to LLM for novel tasks
  - Location: `com.minewright.htn/LLMHTNBridge.java`

---

## Phase 3: Autonomous Agents

### 3.1 Needs System
**Status:** Design in AUTONOMOUS_NPC_DESIGN.md
**Priority:** High

- [ ] **Need Tracker**
  - Hunger, Energy, Social, Purpose meters
  - Needs decay over time
  - Low needs trigger autonomous behaviors
  - Location: `com.minewright.autonomy/NeedTracker.java`

- [ ] **Idle Behaviors**
  - State machine: IDLE → WANDER → SOCIALIZE → WORK
  - Personality-driven idle actions
  - Proactive task finding
  - Location: `com.minewright.autonomy/IdleBehaviorManager.java`

- [ ] **Self-Improvement**
  - Practice skills during downtime
  - Optimize inventory organization
  - Maintain tools and equipment
  - Location: `com.minewright.autonomy/SelfImprovement.java`

### 3.2 Conversation System
**Status:** Design in CONVERSATION_COORDINATION_DESIGN.md
**Priority:** High

- [ ] **Conversation Manager**
  - Multi-agent conversation support
  - Turn-taking with personality
  - Context-aware responses
  - Location: `com.minewright.conversation/ConversationManager.java`

- [ ] **Dialogue Triggers**
  - Time-based (periodic check-ins)
  - Event-based (task complete, found something)
  - Context-based (stuck, need help)
  - Memory-based (remember past interactions)
  - Location: `com.minewright.conversation/DialogueTriggers.java`

- [ ] **Mace Personality**
  - Construction foreman persona
  - Dry wit, professional but warm
  - Uses construction terminology
  - Location: `com.minewright.personality/MacePersonality.java`

---

## Phase 4: Multi-Agent Coordination

### 4.1 Worker Specialization
**Status:** Research complete
**Priority:** Medium

- [ ] **Role System**
  - Roles: Miner, Builder, Farmer, Combat, Logistics
  - Role-specific scripts and behaviors
  - Role assignment by Mace based on task needs
  - Location: `com.minewright.coordination/RoleManager.java`

- [ ] **Skill Progression**
  - Workers improve at their roles over time
  - Faster execution, better decisions
  - Unlock advanced techniques
  - Location: `com.minewright.coordination/SkillProgression.java`

- [ ] **Worker Nicknames**
  - Mace assigns nicknames based on behaviors
  - "Dusty" for miners, "Sparks" for fast workers
  - Nicknames evolve with experience
  - Location: `com.minewright.coordination/NicknameGenerator.java`

### 4.2 Coordination Protocols
**Status:** Implemented in ContractNet
**Priority:** Low (Already done)

- [x] Contract Net Protocol
- [x] Agent Capability Registry
- [x] Collaborative Building Coordinator
- [x] Inter-Agent Communication Bus

---

## Phase 5: Token Optimization

### 5.1 Caching Enhancements
**Status:** Implemented, needs tuning
**Priority:** Medium

- [x] Semantic Cache (embedding-based)
- [x] Exact Match Cache
- [ ] Adaptive threshold tuning
- [ ] Context-aware invalidation

### 5.2 Batching Optimization
**Status:** Implemented
**Priority:** Low (Already done)

- [x] Prompt Batcher with priorities
- [x] Rate limit management
- [x] Request merging

### 5.3 Cascade Router Tuning
**Status:** Implemented
**Priority:** Medium

- [x] Complexity analysis
- [x] Multi-tier fallback
- [ ] Local model accuracy testing
- [ ] Vision model integration

---

## Emergent Research Branches

*This section tracks research ideas discovered during implementation that warrant further exploration.*

### Branch 1: Mental Simulation
**Discovered:** During HTN research
**Potential:** Agents could "imagine" outcomes before acting

**Research Questions:**
- Can agents simulate task execution to predict failures?
- How to represent world state for simulation?
- Trade-off between simulation accuracy and computation time?

### Branch 2: Player Modeling
**Discovered:** During conversation research
**Potential:** Learn player preferences and adapt behavior

**Research Questions:**
- How to track player preference signals?
- When to ask vs infer preferences?
- Privacy considerations for behavior tracking?

### Branch 3: Emergent Narratives
**Discovered:** During personality research
**Potential:** Agents develop ongoing storylines and inside jokes

**Research Questions:**
- How to maintain narrative continuity across sessions?
- When is referencing past events appropriate?
- Balance between callbacks and new content?

### Branch 4: Skill Transfer Learning
**Discovered:** During script generation research
**Potential:** Skills learned in one context transfer to another

**Research Questions:**
- How to generalize scripts across contexts?
- Can mining patterns inform building patterns?
- Cross-domain skill application?

---

## Success Metrics

### Token Efficiency
| Metric | Current | Target | Stretch |
|--------|---------|--------|---------|
| Tokens per command | ~2000 | ~500 | ~200 |
| Cache hit rate | 40% | 60% | 80% |
| Local model usage | 30% | 60% | 80% |
| Cost per 1000 commands | $1.26 | $0.50 | $0.10 |

### Agent Behavior
| Metric | Current | Target | Stretch |
|--------|---------|--------|---------|
| Idle time | 80% | 20% | 5% |
| Proactive actions/hour | 2 | 10 | 30 |
| Conversation naturalness | 60% | 80% | 95% |
| Task success rate | 85% | 95% | 99% |

### User Experience
| Metric | Current | Target | Stretch |
|--------|---------|--------|---------|
| Response latency | 5-30s | <1s | <100ms |
| Voice recognition accuracy | 70% | 90% | 98% |
| User satisfaction | 7/10 | 9/10 | 9.5/10 |
| Daily active usage | 10min | 30min | 60min |

---

## Weekly Sprint Goals

### Week 1 (Current)
- [ ] Implement Mace as default agent
- [ ] Add K key TTS integration
- [ ] Create basic behavior tree infrastructure

### Week 2
- [ ] Implement Script DSL
- [ ] Create first automation patterns (mining, building)
- [ ] Add needs system foundation

### Week 3
- [ ] Implement script generation via LLM
- [ ] Add conversation triggers
- [ ] Test token efficiency improvements

### Week 4
- [ ] HTN planner integration
- [ ] Worker specialization system
- [ ] User testing and feedback collection

---

## Agent Instructions for Roadmap Updates

When researching new topics, agents should:

1. **Add Emergent Branches**: If you discover a new research direction that could improve the system, add it to the "Emergent Research Branches" section with:
   - Where it was discovered
   - Research questions
   - Potential impact

2. **Update Metrics**: If you find better benchmarks or targets, update the success metrics tables.

3. **Add New Patterns**: If you discover new automation patterns, add them to Phase 2.2 Pattern Library.

4. **Document Decisions**: If you make architectural decisions during research, add them to the relevant phase with rationale.

5. **Cross-Reference**: Link to detailed research documents in `docs/research/`.

---

**Last Updated By:** Claude Orchestrator
**Next Review:** Weekly sprint planning
