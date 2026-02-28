# Integration Guide

## System Overview

The MineWright AI agent system is composed of multiple interconnected components that work together to enable autonomous, intelligent behavior. This guide explains how all systems integrate and work together.

```
┌─────────────────────────────────────────────────────────────────┐
│                           User Interface                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │ Chat K   │  │ Commands │  │ GUI      │  │ Voice Input  │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘   │
│       └───────────────┴────────────┴────────────────┘          │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           v
┌─────────────────────────────────────────────────────────────────┐
│                      Command Processing                          │
│  ┌──────────────┐    ┌─────────────┐    ┌──────────────┐      │
│  │  Command     │ -> │  Command    │ -> │  Command     │      │
│  │  Parser      │    │  Router     │    │  Validator   │      │
│  └──────────────┘    └─────────────┘    └──────────────┘      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        v                  v                  v
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Skill Library│  │  Cascade     │  │   Blackboard │
│   Check      │  │   Router     │  │   Context    │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┴─────────────────┘
                         │
                         v
┌─────────────────────────────────────────────────────────────────┐
│                      Decision Making                             │
│  ┌──────────────┐    ┌─────────────┐    ┌──────────────┐      │
│  │  Task        │ -> │  Utility    │ -> │  Task        │      │
│  │  Generator   │    │  Scorer     │    │  Prioritizer │      │
│  └──────────────┘    └─────────────┘    └──────┬───────┘      │
└──────────────────────────────┬─────────────────────────────────┘
                               │
                               v
┌─────────────────────────────────────────────────────────────────┐
│                   Multi-Agent Coordination                       │
│  ┌──────────────┐    ┌─────────────┐    ┌──────────────┐      │
│  │  Contract    │ -> │  Blackboard │ -> │  Agent       │      │
│  │  Net Manager │    │  Updates    │    │  Comm Bus    │      │
│  └──────────────┘    └─────────────┘    └──────────────┘      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           v
┌─────────────────────────────────────────────────────────────────┐
│                      Execution Layer                             │
│  ┌──────────────┐    ┌─────────────┐    ┌──────────────┐      │
│  │  Action      │ -> │  State      │ -> │  Event       │      │
│  │  Executor    │    │  Machine    │    │  Publisher   │      │
│  └──────────────┘    └─────────────┘    └──────────────┘      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           v
┌─────────────────────────────────────────────────────────────────┐
│                      Result Processing                           │
│  ┌──────────────┐    ┌─────────────┐    ┌──────────────┐      │
│  │  Skill       │ -> │  Memory     │ -> │  Response    │      │
│  │  Learning    │    │  Update     │    │  Generator   │      │
│  └──────────────┘    └─────────────┘    └──────────────┘      │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow

### Complete Request Flow

```
1. USER INPUT
   User presses K or types command
   |
   v
2. COMMAND PARSING
   CommandParser.parse() -> ParsedCommand
   |
   v
3. SKILL CHECK
   SkillLibrary.semanticSearch(command)
   |
   +---> SKILL FOUND?
   |     |
   |     +---> YES: ExecutableSkill.execute() -> GraalVM -> Results
   |     |     (Skip to step 9)
   |     |
   |     +---> NO: Continue to step 4
   |
   v
4. COMPLEXITY ANALYSIS
   ComplexityAnalyzer.analyze(command, foreman, worldKnowledge)
   |
   v
5. ROUTER SELECTION
   TaskComplexity -> LLMTier
   |
   +---> TRIVIAL: Cache (if available)
   +---> SIMPLE: FAST tier (Groq llama-3.1-8b)
   +---> MODERATE: BALANCED tier (Groq llama-3.3-70b)
   +---> COMPLEX: SMART tier (gpt-4/claude)
   +---> NOVEL: SMART tier + max context
   |
   v
6. TASK GENERATION
   TaskPlanner.planTasks(command, context) -> List<Task>
   (via selected LLM tier)
   |
   v
7. UTILITY SCORING
   DecisionContext.from(foreman, tasks)
   TaskPrioritizer.prioritize(tasks, context) -> Sorted Tasks
   |
   v
8. MULTI-AGENT COORDINATION (if applicable)
   ContractNetManager.announceTask(task) -> Bids
   ContractNetManager.awardToBestBidder() -> Winner
   |
   v
9. EXECUTION
   ActionExecutor.execute(task) -> ActionResult
   StateMachine.transition(EXECUTING)
   |
   v
10. EVENT PUBLISHING
    EventBus.publish(new ActionCompletedEvent(task, result))
    |
    v
11. BLACKBOARD UPDATE
    Blackboard.post(KnowledgeArea.TASKS, task.id, result)
    |
    v
12. MEMORY UPDATE
    ForemanMemory.recordAction(task, result)
    |
    v
13. SKILL LEARNING (if applicable)
    TaskPattern.detectPatterns(completedTasks)
    SkillGenerator.fromPattern(pattern) -> New Skill
    |
    v
14. RESPONSE GENERATION
    ResponseGenerator.generate(result) -> User Response
    |
    v
15. USER OUTPUT
    GUI overlay, chat message, or voice response
```

## Component Interconnections

### Skill Library Integration Points

**Connected To**:
- `TaskPlanner`: Checks for applicable skills before LLM call
- `ActionExecutor`: Records skill execution outcomes
- `TaskPattern`: Provides data for pattern detection
- `CodeExecutionEngine`: Executes generated JavaScript

**Data Flow**:
```
TaskPlanner -> SkillLibrary.semanticSearch()
                |
                v
            Skill found?
                |
                +---> YES: ExecutableSkill -> CodeExecutionEngine
                |                                        |
                |                                        v
                |                                   Results
                |
                +---> NO: LLM Planning -> Task Execution
                                       |
                                       v
                                   TaskPattern.detectPatterns()
                                       |
                                       v
                                   New Skill Added
```

### Cascade Router Integration Points

**Connected To**:
- `TaskPlanner`: Selects LLM tier based on complexity
- `ComplexityAnalyzer`: Determines task complexity
- `LLMCache`: Checks for cached responses
- `AsyncLLMClient`: Routes to appropriate model

**Data Flow**:
```
TaskPlanner.receiveCommand(command)
        |
        v
ComplexityAnalyzer.analyze(command) -> TaskComplexity
        |
        v
selectTier(complexity) -> LLMTier
        |
        +---> CACHE: LLMCache.get(command)
        +---> FAST: AsyncGroqClient
        +---> BALANCED: AsyncGroqClient (70b) or AsyncOpenAIClient (gpt-3.5)
        +---> SMART: AsyncOpenAIClient (gpt-4) or AsyncGeminiClient
        |
        v
LLMResponse -> TaskPlanner.parseTasks(response)
```

### Utility AI Integration Points

**Connected To**:
- `TaskPlanner`: Prioritizes generated tasks
- `DecisionContext`: Provides scoring context
- `UtilityFactors`: Implements scoring factors
- `ActionExecutor`: Selects next task to execute

**Data Flow**:
```
TaskPlanner.generateTasks(command) -> List<Task>
        |
        v
DecisionContext.of(foreman, tasks)
        |
        v
TaskPrioritizer.prioritize(tasks, context)
        |
        +---> Apply factors (SAFETY, URGENCY, etc.)
        |
        v
UtilityScore.calculate(task, context)
        |
        v
Sorted Tasks (highest score first)
        |
        v
ActionExecutor.execute(prioritizedTasks.get(0))
```

### Contract Net Integration Points

**Connected To**:
- `TaskPlanner`: Announces tasks for bidding
- `AgentCommunicationBus`: Sends announcements/bids
- `Blackboard`: Shares task status
- `ActionExecutor`: Executes awarded tasks

**Data Flow**:
```
TaskPlanner.decomposeTask(complexTask) -> List<SubTask>
        |
        v
ContractNetManager.announceTask(subTask) -> announcementId
        |
        +---> AgentCommunicationBus.broadcast(announcement)
        |
        +---> Agents receive announcement
        |     |
        |     v
        |  Agent.calculateBid() -> TaskBid
        |     |
        |     v
        |  ContractNetManager.submitBid(bid)
        |
        v
ContractNetManager.awardToBestBidder(announcementId) -> TaskBid
        |
        v
Winner ActionExecutor.execute(subTask)
        |
        v
Blackboard.post(KnowledgeArea.TASKS, subTask.id, result)
```

### Blackboard Integration Points

**Connected To**:
- `All Agents`: Subscribe to relevant areas
- `ContractNetManager`: Posts task announcements
- `ForemanEntity`: Posts status updates
- `EventBus`: Notifies on changes

**Data Flow**:
```
Any Agent.observe(event)
        |
        v
Blackboard.post(area, key, value, sourceAgent, confidence, type)
        |
        +---> Notify subscribers
        |
        +---> Store entry
        |
        v
Other Agents.queryArea(area) -> List<BlackboardEntry>
        |
        v
Agents update their behavior based on shared knowledge
```

## Event Flow

### Event Bus Architecture

```
EventPublisher --> EventBus --> EventHandlers
                      |
                      +---> StateMachine.transition()
                      +---> Blackboard.post()
                      +---> MetricsCollector.record()
                      +---> LoggingInterceptor.log()
```

### Event Types

```java
// Command events
CommandReceivedEvent
CommandParsedEvent
CommandCompletedEvent

// Task events
TaskGeneratedEvent
TaskStartedEvent
TaskCompletedEvent
TaskFailedEvent

// Agent events
AgentStateChangedEvent
AgentMovedEvent
AgentInventoryChangedEvent

// Coordination events
TaskAnnouncedEvent
BidSubmittedEvent
ContractAwardedEvent

// World events
BlockChangedEvent
EntityDetectedEvent
ThreatDetectedEvent
```

### Event Handler Example

```java
public class TaskCompletionHandler implements EventHandler<TaskCompletedEvent> {
    @Override
    public void handle(TaskCompletedEvent event) {
        Task task = event.getTask();
        ActionResult result = event.getResult();

        // Update memory
        foreman.getMemory().recordAction(task, result);

        // Post to blackboard
        Blackboard.getInstance().post(
            KnowledgeArea.TASKS,
            task.getId(),
            result,
            foreman.getUUID(),
            1.0,
            BlackboardEntry.EntryType.FACT
        );

        // Check for skill patterns
        List<Task> recentTasks = foreman.getMemory().getRecentTasks(10);
        List<TaskPattern> patterns = TaskPattern.detectPatterns(recentTasks);

        for (TaskPattern pattern : patterns) {
            if (pattern.getFrequency() >= 3) {
                Skill skill = SkillGenerator.fromPattern(pattern);
                SkillLibrary.getInstance().addSkill(skill);
            }
        }

        // Trigger state transition
        if (result.isSuccess()) {
            stateMachine.transition(AgentState.IDLE);
        } else {
            stateMachine.transition(AgentState.ERROR);
        }
    }
}
```

## Configuration

### Complete Configuration File

`config/steve-common.toml`

```toml
# ============================================
# MineWright AI Agent Configuration
# ============================================

[llm]
# Default LLM provider
provider = "groq"

[llm.openai]
apiKey = "sk-..."
model = "gpt-4"

[llm.groq]
apiKey = "gsk_..."
model = "llama-3.1-8b-instant"

[llm.gemini]
apiKey = "..."
model = "gemini-pro"

# ============================================
# Cascade Router Configuration
# ============================================
[llm.cascade]
enabled = true
defaultTier = "BALANCED"
enableCache = true
cacheTTL = "1h"
maxCacheEntries = 1000

[llm.cascade.thresholds]
trivialMaxWords = 3
simpleMaxWords = 10
moderateMaxWords = 25
novelThreshold = 5

[llm.cascade.tiers]
fastModel = "llama-3.1-8b-instant"
fastProvider = "groq"
balancedModel = "llama-3.3-70b"
balancedProvider = "groq"
smartModel = "gpt-4"
smartProvider = "openai"

# ============================================
# Skill Library Configuration
# ============================================
[skills]
enableLearning = true
minPatternFrequency = 3
minSuccessRate = 0.3
maxCacheAge = "24h"
enableSemanticSearch = true

# ============================================
# Utility AI Configuration
# ============================================
[utility]
enabled = true

[utility.weights]
urgency = 1.8
safety = 2.0
resource_proximity = 1.5
efficiency = 1.2
skill_match = 1.0
player_preference = 1.0
tool_readiness = 0.8
health_status = 0.8
hunger_status = 0.7
time_of_day = 0.5
weather_conditions = 0.3

[utility.thresholds]
high_priority = 0.7
low_priority = 0.3

# ============================================
# Multi-Agent Coordination
# ============================================
[coordination.contract_net]
enabled = true
defaultDeadline = 30000
minBids = 1
maxWaitTime = 60000

[coordination.blackboard]
enabled = true
defaultMaxAge = 300000
cleanupInterval = 60000
maxEntriesPerArea = 1000

[coordination.communication]
enabled = true
queueSize = 100
messageTimeout = 30000

# ============================================
# Agent Settings
# ============================================
[agent]
# Number of agents to spawn
defaultCount = 1

# Agent behavior settings
[agent.behavior]
autoAttack = true
autoDefend = true
followDistance = 5
maxTaskQueueSize = 10

# Agent personality
[agent.personality]
# Personality archetype: "default", "british_wit", "artificer"
archetype = "default"

# Voice settings
[agent.voice]
enabled = false
provider = "disabled"
```

## System Initialization

### Startup Sequence

```
1. Minecraft Forge initializes
   |
   v
2. MineWrightMod constructor
   |
   v
3. Initialize ServiceContainer (DI)
   |
   v
4. Initialize EventBus
   |
   v
5. Initialize Blackboard
   |
   v
6. Initialize SkillLibrary (loads built-in skills)
   |
   v
7. Initialize ContractNetManager
   |
   v
8. Initialize TaskPrioritizer (loads default factors)
   |
   v
9. Initialize ComplexityAnalyzer
   |
   v
10. Initialize LLM Clients (OpenAI, Groq, Gemini)
   |
   v
11. Register Event Handlers
   |
   v
12. Register Commands (/steve spawn, /steve list, etc.)
   |
   v
13. System Ready
```

### Initialization Code

```java
@Mod("minewright")
public class MineWrightMod {
    public MineWrightMod() {
        // 1. Initialize DI container
        ServiceContainer services = new SimpleServiceContainer();

        // 2. Initialize event bus
        EventBus eventBus = new SimpleEventBus();
        services.register(EventBus.class, eventBus);

        // 3. Initialize blackboard
        Blackboard blackboard = new Blackboard();
        services.register(Blackboard.class, blackboard);

        // 4. Initialize skill library
        SkillLibrary skills = SkillLibrary.getInstance();
        services.register(SkillLibrary.class, skills);

        // 5. Initialize contract net manager
        ContractNetManager contractNet = new ContractNetManager();
        services.register(ContractNetManager.class, contractNet);

        // 6. Initialize task prioritizer
        TaskPrioritizer prioritizer = TaskPrioritizer.withDefaults();
        services.register(TaskPrioritizer.class, prioritizer);

        // 7. Initialize complexity analyzer
        ComplexityAnalyzer analyzer = new ComplexityAnalyzer();
        services.register(ComplexityAnalyzer.class, analyzer);

        // 8. Initialize LLM clients
        AsyncLLMClient llmClient = createLLMClient(config);
        services.register(AsyncLLMClient.class, llmClient);

        // 9. Register event handlers
        registerEventHandlers(eventBus, services);

        // 10. Register forge events
        FMLJavaModLoadingContext.get().getModEventBus()
            .addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Register commands, capabilities, etc.
    }
}
```

## Extension Points

### Adding New Skills

```java
// 1. Create skill
Skill mySkill = ExecutableSkill.builder("mySkill")
    .description("Description")
    .category("mining")
    .codeTemplate("...")
    .requiredActions("mine")
    .build();

// 2. Register with skill library
SkillLibrary.getInstance().addSkill(mySkill);

// 3. Update PromptBuilder to inform LLM
PromptBuilder.addSkillDescription("mySkill", "Description of what it does");
```

### Adding New Utility Factors

```java
// 1. Create factor
public static final UtilityFactor MY_FACTOR = new UtilityFactor() {
    @Override
    public double calculate(Task task, DecisionContext context) {
        // Your logic here
        return 0.5;
    }

    @Override
    public String getName() {
        return "my_factor";
    }
};

// 2. Register with prioritizer
TaskPrioritizer.getInstance().addFactor(MY_FACTOR, 1.0);
```

### Adding New Event Handlers

```java
// 1. Create handler
public class MyEventHandler implements EventHandler<MyEvent> {
    @Override
    public void handle(MyEvent event) {
        // Handle event
    }
}

// 2. Register with event bus
eventBus.subscribe(MyEvent.class, new MyEventHandler());
```

## Best Practices

1. **Use Dependency Injection**: Register all services with ServiceContainer
2. **Events for Decoupling**: Use EventBus for loose coupling between components
3. **Fail Gracefully**: Always have fallbacks (e.g., LLM fallback for failed skills)
4. **Log Everything**: Use structured logging for debugging
5. **Measure Performance**: Track metrics for all major operations
6. **Clean Up Resources**: Remove stale entries from blackboard, caches
7. **Thread Safety**: Use concurrent collections for shared state
8. **Configuration Driven**: Make behavior configurable via TOML

## Troubleshooting

### System Not Starting

**Symptoms**: Mod fails to load, errors in log

**Solutions**:
1. Check config file syntax
2. Verify API keys are set
3. Check Java version (requires 17+)
4. Review dependencies in build.gradle
5. Check for conflicting mods

### Agents Not Responding

**Symptoms**: Commands have no effect

**Solutions**:
1. Check agent state in `/steve list`
2. Verify LLM client is working
3. Check network connectivity
4. Review task queue for stuck tasks
5. Examine event bus for errors

### High Memory Usage

**Symptoms**: Memory constantly increasing

**Solutions**:
1. Reduce blackboard max entry age
2. Increase cleanup frequency
3. Limit skill library size
4. Check for memory leaks in event handlers
5. Reduce LLM cache size

## References

- **Main Documentation**: `CLAUDE.md`
- **Skill Library**: `SKILL_LIBRARY.md`
- **Cascade Router**: `CASCADE_ROUTER.md`
- **Utility AI**: `UTILITY_AI.md`
- **Multi-Agent**: `MULTI_AGENT_COORDINATION.md`
