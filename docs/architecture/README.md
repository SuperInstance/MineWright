# Architecture Documentation Index

This directory contains comprehensive documentation for MineWright's AI architecture systems.

## Documentation Files

### [SKILL_LIBRARY.md](SKILL_LIBRARY.md)
Documents the self-improving skill library system inspired by the Voyager pattern. Covers:
- Skill interface and ExecutableSkill implementation
- SkillLibrary central registry with semantic search
- TaskPattern detection for automatic skill generation
- Built-in skills (mining, building, farming, utility)
- Integration with TaskPlanner and ActionExecutor
- GraalVM JavaScript execution

### [CASCADE_ROUTER.md](CASCADE_ROUTER.md)
Documents intelligent LLM selection based on task complexity. Covers:
- Task complexity levels (TRIVIAL, SIMPLE, MODERATE, COMPLEX, NOVEL)
- LLM tiers (CACHE, FAST, BALANCED, SMART)
- ComplexityAnalyzer with pattern matching
- Cost optimization (70-90% savings expected)
- Configuration and tuning

### [UTILITY_AI.md](UTILITY_AI.md)
Documents task prioritization using utility scoring. Covers:
- UtilityFactor interface and implementation
- DecisionContext for world/agent state
- TaskPrioritizer for scoring and sorting
- Built-in factors (URGENCY, SAFETY, RESOURCE_PROXIMITY, etc.)
- Custom factor creation
- Scoring formula and examples

### [MULTI_AGENT_COORDINATION.md](MULTI_AGENT_COORDINATION.md)
Documents multi-agent coordination systems. Covers:
- Contract Net Protocol for task allocation
- TaskAnnouncement and TaskBid records
- ContractNetManager for bidding process
- Blackboard pattern for shared knowledge
- KnowledgeArea partitioning (WORLD_STATE, AGENT_STATUS, TASKS, etc.)
- Agent communication via message bus
- Example workflows (collaborative building, resource distribution, threat response)

### [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)
Comprehensive guide on how all systems work together. Covers:
- System overview with visual diagrams
- Complete request flow (15 steps from input to output)
- Component interconnections and data flow
- Event flow and event types
- Complete configuration file
- System initialization sequence
- Extension points and best practices

## Quick Reference

### System Architecture Diagram

```
User Input
    |
    v
Skill Check --> Cascade Router --> Task Generation
    |               |                  |
    |               v                  v
    |          LLM Tier Selection   Utility Scoring
    |               |                  |
    v               v                  v
GraalVM      Contract Net        Task Queue
              & Blackboard              |
                  |                     v
                  v                 Action Executor
              Coordination             |
                  |                     v
                  v                 Event Publishing
              Shared Knowledge          |
                                      v
                                 Learning & Memory
```

### Key Components by Package

| Package | Component | Purpose |
|---------|-----------|---------|
| `skill` | SkillLibrary, ExecutableSkill, TaskPattern | Self-improving skill system |
| `llm.cascade` | TaskComplexity, LLMTier, ComplexityAnalyzer | Intelligent LLM routing |
| `decision` | UtilityFactor, TaskPrioritizer, DecisionContext | Utility-based task prioritization |
| `coordination` | ContractNetManager, TaskAnnouncement, TaskBid | Multi-agent task allocation |
| `blackboard` | Blackboard, BlackboardEntry, KnowledgeArea | Shared knowledge system |
| `communication` | AgentCommunicationBus, AgentMessage | Inter-agent messaging |
| `event` | EventBus, EventHandler | Event-driven architecture |
| `execution` | ActionExecutor, StateMachine | Task execution and state management |

### Configuration Reference

All systems are configured via `config/steve-common.toml`:

```toml
[llm.cascade]           # Cascade router settings
[skills]                # Skill library settings
[utility]               # Utility AI settings
[coordination.contract_net]  # Contract net protocol
[coordination.blackboard]    # Blackboard system
[coordination.communication] # Agent communication
[agent.behavior]        # Agent behavior settings
```

## Usage Examples

### Using the Skill Library

```java
// Find applicable skills
List<Skill> skills = SkillLibrary.getInstance().findApplicableSkills(task);

// Execute skill
ExecutableSkill skill = (ExecutableSkill) skills.get(0);
Map<String, Object> context = Map.of("depth", 10, "direction", "north");
CodeExecutionEngine.ExecutionResult result = skill.execute(context, engine);
```

### Using the Cascade Router

```java
// Analyze complexity
ComplexityAnalyzer analyzer = new ComplexityAnalyzer();
TaskComplexity complexity = analyzer.analyze(command, foreman, worldKnowledge);

// Select tier
LLMTier tier = selectTierForComplexity(complexity);

// Route to LLM
LLMResponse response = llmRouter.route(tier, prompt);
```

### Using Utility AI

```java
// Create prioritizer
TaskPrioritizer prioritizer = TaskPrioritizer.withDefaults();

// Prioritize tasks
DecisionContext context = DecisionContext.of(foreman, tasks);
List<Task> prioritized = prioritizer.prioritize(tasks, context);

// Execute highest priority task
Task bestTask = prioritized.get(0);
actionExecutor.execute(bestTask);
```

### Using Contract Net

```java
// Announce task
String announcementId = contractNet.announceTask(task, requesterId, 30000);

// Submit bid
TaskBid bid = TaskBid.builder()
    .announcementId(announcementId)
    .bidderId(myAgentId)
    .score(0.8)
    .estimatedTime(5000)
    .confidence(0.9)
    .build();
contractNet.submitBid(bid);

// Award contract
contractNet.awardToBestBidder(announcementId);
```

### Using Blackboard

```java
// Post information
Blackboard.getInstance().post(
    KnowledgeArea.THREATS,
    "zombie_001",
    threatData,
    agentId,
    1.0,
    BlackboardEntry.EntryType.FACT
);

// Query area
List<BlackboardEntry<?>> threats = Blackboard.getInstance()
    .queryArea(KnowledgeArea.THREATS);

// Subscribe to changes
Blackboard.getInstance().subscribe(KnowledgeArea.THREATS, entry -> {
    logger.info("New threat detected: {}", entry);
});
```

## Further Reading

- **Main Project Documentation**: `CLAUDE.md` (project root)
- **Research Documents**: `docs/research/` (AI architectures, multi-agent systems)
- **Example Code**: `docs/examples/` (demonstrations and tutorials)

## Contributing

When adding new systems or modifying existing ones:

1. Update this index with new documentation files
2. Keep documentation in sync with code changes
3. Add code examples to relevant sections
4. Update configuration reference
5. Include diagrams where helpful

## Support

For questions or issues:
1. Check the relevant documentation file above
2. Review the code in `src/main/java/com/minewright/`
3. Check example code in `docs/examples/`
4. Review research documents in `docs/research/`

---

**Last Updated**: 2025-02-27
**Documentation Version**: 1.0.0
**Project Version**: 1.20.1
