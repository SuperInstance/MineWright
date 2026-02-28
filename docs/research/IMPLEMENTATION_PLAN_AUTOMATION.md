# Implementation Plan: "One Abstraction Away" Automation System

**Document Version:** 1.0
**Date:** 2026-02-28
**Status:** Implementation Roadmap
**Project:** MineWright AI (Steve AI) - Script-Based Automation System

---

## Executive Summary

### What is "One Abstraction Away"?

The **"One Abstraction Away"** system is a paradigm shift from direct LLM-controlled actions to **script-based automation**. Instead of LLMs micromanaging every action, they generate reusable **macro scripts** (DSL programs) that agents execute repeatedly. This approach:

- **Reduces token consumption by 60-80%** through script reuse
- **Enables intelligent behavior without constant LLM calls**
- **Provides predictable, debuggable agent behaviors**
- **Supports continuous improvement through refinement loops**
- **Maintains full functionality during LLM outages**

### Why This Is Our Killer Feature

1. **Unmatched Token Efficiency**: Scripts are generated once, executed thousands of times
2. **Reliability**: Deterministic execution without network dependency
3. **Evolutionary**: Agents learn and improve through conversation and A/B testing
4. **Scalable**: Same script can run on multiple agents simultaneously
5. **Debuggable**: Every action is traceable through script execution logs

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     User Command Layer                         │
│  "Build me a house" → LLM generates script → Script executes   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   Script Generation Layer                      │
│  • LLM generates DSL scripts from natural language             │
│  • Script templates for common patterns                        │
│  • Script cache with semantic similarity matching              │
│  • Version control and A/B testing framework                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Execution Engine Layer                      │
│  • Behavior Tree runtime (fast, deterministic)                 │
│  • Utility AI for decision scoring                             │
│  • Trigger system for reactive behaviors                      │
│  • State machine for lifecycle management                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Action Layer (Existing)                    │
│  • ActionRegistry with plugin architecture                    │
│  • BaseAction implementations (Mine, Build, etc.)             │
│  • Interceptor chain for logging/metrics/events                │
└─────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Foundation (Week 1-2)

### Objective
Build the core infrastructure for script-based automation without disrupting existing systems.

### 1.1 Behavior Tree Runtime

**Deliverables:**
- Core behavior tree node interfaces and implementations
- Blackboard for world state management
- Integration with existing ActionExecutor

**Implementation:**

```java
package com.minewright.behavior;

// Core node interface
public interface Node {
    enum Status { SUCCESS, FAILURE, RUNNING }

    Status tick(Blackboard blackboard);
    void reset();
    String getDescription();
}

// Composite nodes
public class SequenceNode implements Node {
    private final List<Node> children;
    private int currentChild = 0;

    @Override
    public Status tick(Blackboard blackboard) {
        if (children.isEmpty()) return Status.SUCCESS;

        Status childStatus = children.get(currentChild).tick(blackboard);

        switch (childStatus) {
            case RUNNING -> { return Status.RUNNING; }
            case FAILURE -> {
                currentChild = 0;
                return Status.FAILURE;
            }
            case SUCCESS -> {
                currentChild++;
                if (currentChild >= children.size()) {
                    currentChild = 0;
                    return Status.SUCCESS;
                }
                return Status.RUNNING;
            }
        }
    }
}

public class SelectorNode implements Node {
    // Similar to Sequence but returns SUCCESS on first child success
    // Returns FAILURE only if all children fail
}
```

**Testing:**
```java
@Test
void testSequenceNode() {
    Node child1 = mockSuccessNode();
    Node child2 = mockSuccessNode();
    SequenceNode sequence = new SequenceNode(List.of(child1, child2));
    Blackboard blackboard = mock(Blackboard.class);

    assertEquals(Status.RUNNING, sequence.tick(blackboard));
    assertEquals(Status.SUCCESS, sequence.tick(blackboard));
}
```

### 1.2 Script DSL Specification

**Deliverables:**
- YAML-based script format specification
- Script parser implementation
- Script validator with multi-layer checks

**Script Format:**
```yaml
metadata:
  id: "gather_iron_ore"
  name: "Gather Iron Ore"
  version: "1.0.0"

parameters:
  target_amount: 64
  search_radius: 64

script:
  type: "sequence"
  steps:
    - type: "action"
      action: "check_requirements"
      params:
        items: ["pickaxe"]

    - type: "selector"
      steps:
        - type: "sequence"
          steps:
            - type: "action"
              action: "locate_nearest"
              params:
                block: "iron_ore"
                radius: "{{search_radius}}"
                save_as: "target"
            - type: "action"
              action: "pathfind_to"
              params:
                target: "@target"
```

**Parser Implementation:**
```java
public class ScriptParser {
    public Script parse(String yamlContent) throws ScriptParseException {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);

        Script script = new Script();
        script.setMetadata(parseMetadata(data.get("metadata")));
        script.setParameters(parseParameters(data.get("parameters")));
        script.setRootNode(parseNode(data.get("script")));

        return script;
    }

    private Node parseNode(Object nodeData) {
        Map<String, Object> map = (Map<String, Object>) nodeData;
        String type = (String) map.get("type");

        return switch (type) {
            case "sequence" -> parseSequence(map);
            case "selector" -> parseSelector(map);
            case "action" -> parseAction(map);
            case "condition" -> parseCondition(map);
            default -> throw new ScriptParseException("Unknown node type: " + type);
        };
    }
}
```

### 1.3 Script Validator

```java
public class ScriptValidator {
    public ValidationResult validate(Script script) {
        ValidationResult result = new ValidationResult();

        // Layer 1: Structural validation
        result.add(validateStructure(script));

        // Layer 2: Semantic validation
        result.add(validateSemantics(script));

        // Layer 3: Security validation
        result.add(validateSecurity(script));

        // Layer 4: Resource validation
        result.add(validateResources(script));

        return result;
    }

    private ValidationResult validateStructure(Script script) {
        // Check for cycles, orphan nodes, invalid references
        // Ensure tree is well-formed
        List<String> errors = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        validateNodeStructure(script.getRootNode(), visited, errors);

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private ValidationResult validateSemantics(Script script) {
        // Check action types exist in ActionRegistry
        // Verify parameter types match expectations
        List<String> errors = new ArrayList<>();
        ActionRegistry registry = ActionRegistry.getInstance();

        validateActionsExist(script.getRootNode(), registry, errors);
        validateParameterTypes(script.getRootNode(), errors);

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
```

### 1.4 Integration with Existing Systems

```java
public class ActionExecutor {
    private final BehaviorTreeManager behaviorTreeManager;
    private final ScriptManager scriptManager;

    public ActionExecutor(ForemanEntity foreman) {
        this.behaviorTreeManager = new BehaviorTreeManager(foreman);
        this.scriptManager = new ScriptManager(llmClient, parser, cache);

        // Set default behavior tree based on role
        setDefaultBehaviorTree();
    }

    public void tick() {
        ticksSinceLastAction++;

        // Check for async planning completion
        checkAsyncPlanning();

        // NEW: Tick behavior tree if enabled
        if (behaviorTreeManager.isEnabled()) {
            behaviorTreeManager.tick();
        }

        // Existing: Execute current task
        if (currentTask != null) {
            executeCurrentTask();
        }
    }

    private void setDefaultBehaviorTree() {
        AgentRole role = foreman.getRole();
        BehaviorTree tree = switch (role) {
            case WORKER -> WorkerBehaviorTrees.createMiningTree();
            case FOREMAN -> WorkerBehaviorTrees.createForemanTree();
            case SOLO -> WorkerBehaviorTrees.createSoloTree();
        };

        behaviorTreeManager.setTree(tree);
    }
}
```

**Success Criteria:**
- Behavior tree ticks in < 1ms
- Scripts parse and validate in < 100ms
- No regression in existing action execution
- Unit tests cover all node types

---

## Phase 2: Core Features (Week 3-4)

### Objective
Implement script generation from LLM and refinement loops.

### 2.1 Script Generation from LLM

**Deliverables:**
- Script generation prompts
- LLM-to-Script parser
- Template-based script generation

**Prompt Template:**
```java
public class ScriptPromptBuilder {
    public String buildScriptGenerationPrompt(String command, ScriptGenerationContext context) {
        return String.format("""
            You are an expert Minecraft automation architect.

            Generate a DSL script for this task:
            %s

            ## Context
            Agent: %s
            Role: %s
            Location: %s
            Inventory: %s

            ## Available Actions
            %s

            ## Script DSL Grammar
            sequence { <script>+ }     # Execute in order, all must succeed
            selector { <script>+ }     # Try in order until one succeeds
            parallel { <script>+ }     # Execute simultaneously
            action(<name>, <params>)   # Execute atomic action
            condition(<expr>)          # Check condition

            ## Output Format
            Return ONLY valid YAML. No explanations.

            ```yaml
            metadata:
              id: "script_id"
              name: "Script Name"
              version: "1.0.0"

            parameters:
              param_name: param_value

            script:
              type: "sequence"
              steps:
                - type: "action"
                  action: "action_name"
                  params:
                    key: value
            ```
            """,
            command,
            context.getAgentName(),
            context.getRole(),
            context.getLocation(),
            context.getInventory(),
            ActionRegistry.getInstance().getActionsAsList()
        );
    }
}
```

**Script Manager:**
```java
public class ScriptManager {
    private final LLMClient llmClient;
    private final ScriptParser parser;
    private final ScriptCache cache;
    private final ScriptTemplateManager templateManager;

    public CompletableFuture<Script> generateScriptAsync(
            String command,
            ScriptGenerationContext context) {

        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            CachedScript cached = cache.findSimilar(command, 0.85);
            if (cached != null) {
                LOGGER.info("Using cached script: {}", cached.getScriptId());
                return cached.getScript().copy();
            }

            // Try template matching
            Script template = templateManager.findTemplate(command);
            if (template != null) {
                LOGGER.info("Using template: {}", template.getId());
                return template.fillParameters(context);
            }

            // Generate new script
            return generateNewScript(command, context);
        });
    }

    private Script generateNewScript(String command, ScriptGenerationContext context) {
        String prompt = new ScriptPromptBuilder()
            .buildScriptGenerationPrompt(command, context);

        LLMResponse response = llmClient.complete(prompt);
        Script script = parser.parse(response.getContent());

        ValidationResult validation = new ScriptValidator().validate(script);
        if (!validation.isValid()) {
            throw new ScriptValidationException(validation.getErrors());
        }

        cache.cache(script.getId(), script, command, null);
        return script;
    }
}
```

### 2.2 Script Refinement Loop

**Deliverables:**
- Failure detection system
- Hierarchical refinement (line → block → global)
- Version control for scripts

**Failure Detection:**
```java
public class ScriptFailureDetector {
    public FailureType detectFailure(ScriptExecutionState state) {
        // 1. Action Failure
        if (state.getLastActionFailed()) {
            return FailureType.ACTION_FAILED;
        }

        // 2. Timeout
        if (state.getExecutionTime() > state.getScript().getTimeLimit()) {
            return FailureType.TIMEOUT;
        }

        // 3. Stuck Detection
        if (isAgentStuck(state)) {
            return FailureType.AGENT_STUCK;
        }

        // 4. Resource Exhaustion
        if (hasResourceShortage(state)) {
            return FailureType.RESOURCE_EXHAUSTION;
        }

        return null; // No failure
    }

    private boolean isAgentStuck(ScriptExecutionState state) {
        // Agent hasn't moved in N ticks
        // Agent is repeating same actions without progress
        // Pathfinding returns same result repeatedly

        BlockPos currentPos = state.getAgentPosition();
        List<BlockPos> recentPositions = state.getRecentPositions();

        // Check if stuck in same position for 100 ticks
        long stuckTicks = recentPositions.stream()
            .filter(p -> p.equals(currentPos))
            .count();

        return stuckTicks > 100;
    }
}
```

**Hierarchical Refinement:**
```java
public class HierarchicalScriptRefiner {
    public RefinementResult refineWithScope(Script script, FailureContext context) {
        // Level 1: Line Scope (most token-efficient)
        RefinementResult lineResult = refineLineScope(script, context);
        if (lineResult.isSuccess()) {
            return lineResult;
        }

        // Level 2: Block Scope (moderate token usage)
        RefinementResult blockResult = refineBlockScope(script, context);
        if (blockResult.isSuccess()) {
            return blockResult;
        }

        // Level 3: Global Scope (highest token usage, last resort)
        return refineGlobalScope(script, context);
    }

    private RefinementResult refineLineScope(Script script, FailureContext context) {
        // Isolate the failed step + immediate context
        // Attempt local fix with minimal changes
        // Best for: Individual action failures

        int failedStep = context.getFailedStep();
        Node failedNode = script.findNodeAtStep(failedStep);

        String prompt = String.format("""
            Fix this specific step that failed:

            Step %d: %s
            Error: %s

            Context:
            - Agent position: %s
            - Inventory: %s
            - Nearby blocks: %s

            Provide only the corrected step in DSL format.
            """,
            failedStep,
            failedNode.getDescription(),
            context.getErrorMessage(),
            context.getAgentPosition(),
            context.getInventory(),
            context.getNearbyBlocks()
        );

        LLMResponse response = llmClient.complete(prompt);
        Node fixedNode = parser.parseNode(response.getContent());

        script.replaceNode(failedStep, fixedNode);
        return new RefinementResult(true, "Fixed at line scope", script);
    }
}
```

**Version Control:**
```java
public class ScriptVersionControl {
    public ScriptVersion commit(Script script, String commitMessage, String author) {
        ScriptVersion version = new ScriptVersion();
        version.setId(generateVersionId());
        version.setParentId(script.getCurrentVersionId());
        version.setScript(script.copy());
        version.setCommitMessage(commitMessage);
        version.setAuthor(author);
        version.setTimestamp(Instant.now());
        version.setDiff(generateDiff(script.getParentVersion(), script));

        repository.save(version);
        return version;
    }

    public ScriptDiff diff(String versionId1, String versionId2) {
        ScriptVersion v1 = repository.getVersion(versionId1);
        ScriptVersion v2 = repository.getVersion(versionId2);
        return ScriptDiff.compute(v1.getScript(), v2.getScript());
    }

    public Script revert(String scriptId, String targetVersionId) {
        ScriptVersion target = repository.getVersion(targetVersionId);
        Script reverted = target.getScript().copy();
        reverted.setCurrentVersionId(targetVersionId);
        return reverted;
    }
}
```

### 2.3 Pattern Library

**Deliverables:**
- Pre-built script templates for common tasks
- Template parameter system
- Template discovery and matching

**Template Definitions:**
```yaml
# gather_resource.yml
name: "Gather Resource Template"
description: "Generic resource gathering with customizable parameters"

template_source: |
  sequence {
    condition("inventory_has_space", target="{{resource}}", quantity="{{target_amount}}")
    action("locate_nearest", block="{{resource}}", radius="{{search_radius}}", save_as="target")
    action("pathfind_to", target="@target", max_distance="{{max_distance}}")
    action("face", target="@target")

    if "{{requires_tool}}" {
      action("equip", tool="{{required_tool}}")
    }

    action("gather", target="@target", tool="{{required_tool}}")

    condition("inventory_percent", threshold="{{storage_threshold}}")
    action("deposit_nearby", radius="{{deposit_radius}}")
  }

parameters:
  - name: "resource"
    type: "BlockType"
    required: true
    description: "Block type to gather"

  - name: "target_amount"
    type: "integer"
    required: true
    description: "Amount to collect"

  - name: "search_radius"
    type: "integer"
    default: 64
    description: "How far to search"

examples:
  - task: "Gather 64 oak logs"
    parameters:
      resource: "oak_log"
      target_amount: 64
      requires_tool: true
      required_tool: "diamond_axe"
```

**Template Manager:**
```java
public class ScriptTemplateManager {
    private final Map<String, ScriptTemplate> templates;
    private final EmbeddingModel embeddingModel;

    public Script generateFromTemplate(String templateName, Map<String, Object> parameters) {
        ScriptTemplate template = templates.get(templateName);

        // Fill template parameters
        String scriptSource = template.getSource();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            scriptSource = scriptSource.replace(placeholder, entry.getValue().toString());
        }

        return scriptParser.parse(scriptSource);
    }

    public ScriptTemplate findTemplate(String command) {
        // Generate embedding for command
        float[] commandEmbedding = embeddingModel.embed(command);

        ScriptTemplate bestMatch = null;
        double bestSimilarity = 0.0;

        for (ScriptTemplate template : templates.values()) {
            double similarity = cosineSimilarity(
                commandEmbedding,
                template.getEmbedding()
            );

            if (similarity > bestSimilarity && similarity >= 0.85) {
                bestSimilarity = similarity;
                bestMatch = template;
            }
        }

        return bestMatch;
    }
}
```

**Success Criteria:**
- Scripts generate from natural language in < 5 seconds
- Refinement loops converge in < 3 iterations
- Templates cover 80% of common tasks
- Version control tracks all script changes

---

## Phase 3: Advanced Features (Week 5-6)

### Objective
Enable multi-agent script coordination and autonomous behaviors.

### 3.1 Multi-Agent Script Coordination

**Deliverables:**
- Script partitioning for parallel execution
- Agent assignment based on utility scoring
- Coordination protocol for collaborative scripts

**Script Partitioning:**
```java
public class ScriptPartitioner {
    public List<ScriptPartition> partitionForMultiAgent(Script script, int agentCount) {
        // Analyze script structure
        List<Node> parallelizableNodes = findParallelizableNodes(script.getRootNode());

        // Create partitions
        List<ScriptPartition> partitions = new ArrayList<>();

        for (Node node : parallelizableNodes) {
            if (node instanceof SequenceNode sequence) {
                // Partition sequence into chunks
                List<List<Node>> chunks = partitionSequence(sequence.getChildren(), agentCount);

                for (int i = 0; i < chunks.size(); i++) {
                    ScriptPartition partition = new ScriptPartition();
                    partition.setPartitionId(i);
                    partition.setNodes(chunks.get(i));
                    partition.setDependencies(findDependencies(chunks.get(i), sequence));
                    partitions.add(partition);
                }
            }
        }

        return partitions;
    }

    private List<List<Node>> partitionSequence(List<Node> nodes, int agentCount) {
        // Group nodes into agentCount partitions
        // Balance workload based on estimated duration
        List<List<Node>> partitions = new ArrayList<>();
        for (int i = 0; i < agentCount; i++) {
            partitions.add(new ArrayList<>());
        }

        // Distribute nodes using round-robin
        for (int i = 0; i < nodes.size(); i++) {
            int partitionIndex = i % agentCount;
            partitions.get(partitionIndex).add(nodes.get(i));
        }

        return partitions;
    }
}
```

**Agent Assignment:**
```java
public class AgentAssignmentSystem {
    private final AgentCommunicationBus communicationBus;
    private final TaskPrioritizer taskPrioritizer;

    public Map<String, ScriptPartition> assignPartitionsToAgents(
            List<ScriptPartition> partitions,
            List<ForemanEntity> availableAgents) {

        Map<String, ScriptPartition> assignments = new HashMap<>();

        for (ScriptPartition partition : partitions) {
            // Score each agent for this partition
            Map<ForemanEntity, Double> scores = new HashMap<>();

            for (ForemanEntity agent : availableAgents) {
                if (assignments.containsKey(agent.getUUID().toString())) {
                    continue; // Already assigned
                }

                DecisionContext context = new DecisionContext(agent, partition);
                UtilityScore score = taskPrioritizer.scorePartition(partition, context);
                scores.put(agent, score.finalScore());
            }

            // Assign to highest-scoring agent
            ForemanEntity bestAgent = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

            if (bestAgent != null) {
                assignments.put(bestAgent.getUUID().toString(), partition);

                // Send assignment via communication bus
                AgentMessage message = new AgentMessage(
                    "orchestrator",
                    bestAgent.getUUID().toString(),
                    AgentMessage.Type.SCRIPT_ASSIGNMENT,
                    Map.of("partition", partition)
                );
                communicationBus.send(message);
            }
        }

        return assignments;
    }
}
```

### 3.2 Autonomous Idle Behaviors

**Deliverables:**
- Idle behavior scripts
- Proactive task detection
- Context-aware activity selection

**Idle Behavior Tree:**
```java
public class IdleBehaviorTree {
    public static BehaviorTree create() {
        return new BehaviorTree.Builder()
            .name("idle_behaviors")
            .root(createRoot())
            .build();
    }

    private static Node createRoot() {
        return new SelectorNode(List.of(
            // Priority 1: Help other agents
            helpOthersSequence(),

            // Priority 2: Organize inventory
            organizeInventorySequence(),

            // Priority 3: Maintain base
            maintainBaseSequence(),

            // Priority 4: Gather resources
            gatherResourcesSequence(),

            // Priority 5: Patrol
            patrolSequence(),

            // Fallback: Follow player
            followPlayerSequence()
        ));
    }

    private static Node helpOthersSequence() {
        return new SequenceNode(List.of(
            new ConditionNode() {
                @Override
                protected boolean evaluate(Blackboard blackboard) {
                    // Check if other agents need help
                    return blackboard.getAgent().getLevel().getEntitiesOfClass(
                        ForemanEntity.class,
                        blackboard.getAgent().getBoundingBox().inflate(32.0)
                    ).stream().anyMatch(agent -> agent.needsHelp());
                }
            },

            new ActionNode() {
                @Override
                protected Status onTick(Blackboard blackboard) {
                    // Offer help to nearest agent in need
                    ForemanEntity agent = blackboard.getAgent();
                    ForemanEntity needingHelp = findNearestNeedingHelp(agent);

                    if (needingHelp != null) {
                        // Send help message
                        AgentMessage message = new AgentMessage(
                            agent.getUUID().toString(),
                            needingHelp.getUUID().toString(),
                            AgentMessage.Type.HELP_OFFER,
                            Map.of("task", needingHelp.getCurrentTask())
                        );
                        agent.getCommunicationBus().send(message);

                        return Status.SUCCESS;
                    }

                    return Status.FAILURE;
                }
            }
        ));
    }

    private static Node organizeInventorySequence() {
        return new SequenceNode(List.of(
            new ConditionNode() {
                @Override
                protected boolean evaluate(Blackboard blackboard) {
                    // Check if inventory is messy
                    return blackboard.getAgent().getInventory().getOccupiedSlotCount() > 20;
                }
            },

            new ActionNode() {
                @Override
                protected Status onTick(Blackboard blackboard) {
                    // Find nearest chest
                    // Sort and stack items
                    // Deposit non-essentials
                    return Status.RUNNING;
                }
            }
        ));
    }
}
```

**Proactive Task Detection:**
```java
public class ProactiveTaskDetector {
    private final List<ProactiveTrigger> triggers;

    public ProactiveTaskDetector() {
        this.triggers = List.of(
            new LowResourceTrigger(),
            new DamagedEquipmentTrigger(),
            new NearbyThreatTrigger(),
            new UnfinishedStructureTrigger()
        );
    }

    public Optional<Task> detectProactiveTask(ForemanEntity agent) {
        for (ProactiveTrigger trigger : triggers) {
            if (trigger.isTriggered(agent)) {
                Task task = trigger.generateTask(agent);
                if (task != null) {
                    return Optional.of(task);
                }
            }
        }
        return Optional.empty();
    }
}

class LowResourceTrigger implements ProactiveTrigger {
    @Override
    public boolean isTriggered(ForemanEntity agent) {
        // Check if critical resources are low
        return agent.getInventory().countItem("oak_log") < 16 ||
               agent.getInventory().countItem("iron_ingot") < 32;
    }

    @Override
    public Task generateTask(ForemanEntity agent) {
        String resourceNeeded = agent.getInventory().countItem("oak_log") < 16
            ? "oak_log"
            : "iron_ingot";

        return Task.builder()
            .action("gather")
            .parameter("resource", resourceNeeded)
            .parameter("amount", 64)
            .parameter("proactive", true)
            .build();
    }
}
```

### 3.3 Player Preference Learning

**Deliverables:**
- Preference tracking system
- Adaptive script behavior based on history
- Player feedback integration

**Preference Tracker:**
```java
public class PlayerPreferenceTracker {
    private final Map<String, PlayerPreferences> preferences;

    public void recordActionOutcome(String playerId, Action action, boolean success, Duration duration) {
        PlayerPreferences prefs = preferences.computeIfAbsent(
            playerId, k -> new PlayerPreferences()
        );

        // Record completion time preference
        if (duration.toSeconds() < 30) {
            prefs.incrementPreference("fast_completion");
        } else if (duration.toSeconds() > 120) {
            prefs.incrementPreference("thorough_completion");
        }

        // Record style preference
        if (action instanceof PlaceBlockAction) {
            prefs.incrementPreference("building_style");
        } else if (action instanceof MineBlockAction) {
            prefs.incrementPreference("mining_style");
        }

        // Record resource usage
        if (action.getResourceEfficiency() > 0.9) {
            prefs.incrementPreference("resource_efficient");
        }
    }

    public void adjustScriptBehavior(Script script, String playerId) {
        PlayerPreferences prefs = preferences.get(playerId);
        if (prefs == null) return;

        // Adjust script based on preferences
        if (prefs.hasPreference("fast_completion")) {
            script.setSpeedMultiplier(1.5);
            script.setQualityThreshold(0.7);
        } else if (prefs.hasPreference("thorough_completion")) {
            script.setSpeedMultiplier(0.8);
            script.setQualityThreshold(0.95);
        }

        if (prefs.hasPreference("resource_efficient")) {
            script.setResourcePriority(1.0);
        }
    }
}
```

**Success Criteria:**
- Multi-agent scripts achieve 2x speedup
- Idle behaviors activate in < 5 seconds of idleness
- Player preferences adapt after 10 interactions
- Proactive tasks detect 80% of maintenance needs

---

## Phase 4: Polish and Optimization (Week 7-8)

### Objective
Optimize performance and ensure production readiness.

### 4.1 Performance Optimization

**Deliverables:**
- Behavior tree optimization
- Script caching improvements
- Memory usage profiling

**Behavior Tree Optimization:**
```java
public class OptimizedBehaviorTree {
    private final Node root;
    private final Map<Class<?>, Node[]> nodeCache;

    public Status tick(Blackboard blackboard) {
        long start = System.nanoTime();

        // Update blackboard (cached, don't update every tick)
        if (blackboard.needsUpdate()) {
            blackboard.update();
        }

        // Tick root
        Status result = root.tick(blackboard);

        // Track performance
        long duration = System.nanoTime() - start;
        if (duration > 1_000_000) {  // > 1ms
            LOGGER.warn("Behavior tree tick took {} ms", duration / 1_000_000.0);
        }

        return result;
    }
}
```

**Script Cache Optimization:**
```java
public class OptimizedScriptCache {
    private final CaffeineCache<String, CachedScript> cache;

    public OptimizedScriptCache() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }

    public void cache(String key, Script script, float[] embedding) {
        CachedScript cached = new CachedScript(script, embedding);
        cache.put(key, cached);
    }

    public Optional<Script> findSimilar(String query, float[] queryEmbedding, double threshold) {
        // Brute-force search is O(n), optimize with HNSW later
        return cache.asMap().values().stream()
            .filter(cached -> {
                double similarity = cosineSimilarity(queryEmbedding, cached.getEmbedding());
                return similarity >= threshold;
            })
            .map(CachedScript::getScript)
            .findFirst();
    }

    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.stats();

        return new CacheStats(
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate(),
            stats.evictionCount(),
            cache.estimatedSize()
        );
    }
}
```

### 4.2 Error Recovery

**Deliverables:**
- Comprehensive error handling
- Graceful degradation
- Automatic retry logic

**Error Recovery System:**
```java
public class ScriptErrorRecovery {
    public RecoveryResult handleFailure(ScriptExecution execution, Throwable error) {
        LOGGER.error("Script execution failed: {}", execution.getScriptId(), error);

        // Classify error
        ErrorType errorType = classifyError(error);

        // Attempt recovery based on error type
        return switch (errorType) {
            case NETWORK_ERROR -> recoverFromNetworkError(execution, error);
            case PARSING_ERROR -> recoverFromParsingError(execution, error);
            case VALIDATION_ERROR -> recoverFromValidationError(execution, error);
            case RUNTIME_ERROR -> recoverFromRuntimeError(execution, error);
            case TIMEOUT -> recoverFromTimeout(execution, error);
            default -> RecoveryResult.failure("Unknown error type: " + errorType);
        };
    }

    private RecoveryResult recoverFromRuntimeError(ScriptExecution execution, Throwable error) {
        // Try to identify failed step
        Optional<Integer> failedStep = execution.findFailedStep(error);

        if (failedStep.isPresent()) {
            // Attempt to refine script at failed step
            Script refined = refinementEngine.refineAtStep(
                execution.getScript(),
                failedStep.get(),
                error
            );

            // Retry with refined script
            if (refined != null) {
                LOGGER.info("Retrying with refined script");
                return RecoveryResult.retry(refined);
            }
        }

        // Fall back to safe state
        execution.transitionTo(AgentState.IDLE, "Runtime error recovery");
        return RecoveryResult.fallback("Transitioned to IDLE");
    }
}
```

### 4.3 Documentation

**Deliverables:**
- API documentation
- User guide for script creation
- Troubleshooting guide

**API Documentation:**
```java
/**
 * Manages the lifecycle of automation scripts for MineWright agents.
 *
 * <p>Scripts are reusable DSL programs that define agent behaviors.
 * This class handles generation, validation, execution, versioning,
 * and refinement of scripts.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * ScriptManager manager = new ScriptManager(llmClient, parser, cache);
 *
 * // Generate script from natural language
 * Script script = manager.generateScriptAsync(
 *     "Build a 5x5 house",
 *     new ScriptGenerationContext(agent, world)
 * ).join();
 *
 * // Execute script
 * ScriptExecution execution = manager.execute(script, agent, context);
 *
 * // Wait for completion
 * execution.awaitCompletion(5, TimeUnit.MINUTES);
 * }</pre>
 *
 * @since 1.3.0
 * @see Script
 * @see ScriptExecution
 * @see ScriptValidator
 */
public class ScriptManager {
    // ...
}
```

**Success Criteria:**
- Behavior tree ticks in < 0.5ms (p99)
- Scripts generate in < 3 seconds (p95)
- Cache hit rate > 40%
- Memory usage < 100MB per agent
- Zero critical bugs in production

---

## Technical Specifications

### Class Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        ScriptManager                           │
├─────────────────────────────────────────────────────────────────┤
│ - llmClient: LLMClient                                         │
│ - parser: ScriptParser                                         │
│ - cache: ScriptCache                                           │
│ - versionControl: ScriptVersionControl                         │
├─────────────────────────────────────────────────────────────────┤
│ + generateScriptAsync(): CompletableFuture<Script>             │
│ + execute(): ScriptExecution                                    │
│ + refineScriptAsync(): CompletableFuture<Script>                │
│ + getActiveExecutions(): List<ScriptExecution>                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                         BehaviorTree                           │
├─────────────────────────────────────────────────────────────────┤
│ - root: Node                                                   │
│ - name: String                                                 │
├─────────────────────────────────────────────────────────────────┤
│ + tick(blackboard): Status                                     │
│ + reset(): void                                                │
│ + visualize(): String                                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                            Node                               │
├─────────────────────────────────────────────────────────────────┤
│ + tick(blackboard): Status                                     │
│ + reset(): void                                                │
│ + getDescription(): String                                     │
└─────────────────────────────────────────────────────────────────┘
           ↑                                            ↑
           │                                            │
    ┌──────────────┐                            ┌──────────────┐
    │ CompositeNode│                            │  LeafNode    │
    ├──────────────┤                            ├──────────────┤
    │ - children   │                            │              │
    └──────────────┘                            └──────────────┘
           ↑                                            ↑
    ┌──────────┬────────────┐                ┌──────────┬─────────┐
    │Sequence  │  Selector  │                │Condition │ Action  │
    └──────────┴────────────┘                └──────────┴─────────┘
```

### Interface Definitions

```java
package com.minewright.behavior;

/**
 * Core behavior tree node interface.
 * All nodes must implement tick() which is called each game tick.
 */
public interface Node {
    enum Status { SUCCESS, FAILURE, RUNNING }

    /**
     * Called each game tick to evaluate the node.
     * @param blackboard The blackboard containing world state
     * @return The node's status
     */
    Status tick(Blackboard blackboard);

    /**
     * Resets the node's internal state.
     * Called when the behavior tree is restarted.
     */
    void reset();

    /**
     * Returns a human-readable description.
     */
    String getDescription();
}
```

```java
package com.minewright.script;

/**
 * Represents a reusable automation script.
 * Scripts are DSL programs that define agent behaviors.
 */
public class Script {
    private String id;
    private String name;
    private String version;
    private ScriptMetadata metadata;
    private Map<String, Object> parameters;
    private Node rootNode;

    /**
     * Executes this script on an agent.
     * @param agent The agent to execute on
     * @param context The action context
     * @return ScriptExecution handle for monitoring
     */
    public ScriptExecution execute(ForemanEntity agent, ActionContext context) {
        // Implementation
    }

    /**
     * Creates a copy of this script with new ID.
     */
    public Script copy() {
        // Deep copy implementation
    }
}
```

### Code Skeletons

**ScriptManager.java**
```java
package com.minewright.script;

public class ScriptManager {
    private final LLMClient llmClient;
    private final ScriptParser parser;
    private final ScriptCache cache;
    private final Map<String, ScriptExecution> activeExecutions;

    public CompletableFuture<Script> generateScriptAsync(
            String command,
            ScriptGenerationContext context) {
        // Check cache → Check templates → Generate from LLM
    }

    public ScriptExecution execute(Script script, ForemanEntity agent, ActionContext context) {
        // Validate → Create execution → Start → Return handle
    }

    public CompletableFuture<Script> refineScriptAsync(
            Script failedScript,
            ScriptFailureContext failureContext) {
        // Hierarchical refinement → Validate → Commit new version
    }
}
```

**BehaviorTreeManager.java**
```java
package com.minewright.behavior;

public class BehaviorTreeManager {
    private final ForemanEntity foreman;
    private final Blackboard blackboard;
    private BehaviorTree activeTree;

    public void tick() {
        // Update blackboard → Tick tree → Track statistics
    }

    public void setTree(BehaviorTree tree) {
        // Reset old tree → Set new tree → Log
    }

    public Statistics getStatistics() {
        // Return execution statistics
    }
}
```

---

## Success Metrics

### Token Reduction

| Metric | Target | Measurement |
|--------|--------|-------------|
| Tokens per action | < 500 | Average tokens used per completed action |
| Script reuse rate | > 60% | Percentage of actions executed via cached scripts |
| Template usage | > 40% | Percentage of actions using templates |
| LLM call reduction | > 70% | Reduction in LLM API calls vs baseline |

**Calculation:**
```
Token Reduction = (Baseline Tokens - Current Tokens) / Baseline Tokens
Baseline: Direct LLM planning every action
Current: Script generation + script execution
```

### User Experience

| Metric | Target | Measurement |
|--------|--------|-------------|
| Response time | < 3 seconds | Time from command to script generation |
| Execution speed | > 90% baseline | Action execution speed vs direct control |
| Error recovery | < 10 seconds | Time to recover from script failure |
| Reliability | > 99% uptime | Percentage of time system is functional |

### Agent Behavior Quality

| Metric | Target | Measurement |
|--------|--------|-------------|
| Task completion rate | > 85% | Percentage of tasks completed successfully |
| Script refinement convergence | < 3 iterations | Average iterations to fix failed script |
| Idle behavior activation | < 5 seconds | Time to activate proactive behaviors |
| Multi-agent coordination efficiency | > 1.5x speedup | Speed improvement with multiple agents |

### Development Benchmarks

| Phase | Duration | Key Deliverables |
|-------|----------|------------------|
| Phase 1 | 2 weeks | Behavior tree runtime, Script DSL, Validator |
| Phase 2 | 2 weeks | Script generation, Refinement loops, Templates |
| Phase 3 | 2 weeks | Multi-agent coordination, Idle behaviors |
| Phase 4 | 2 weeks | Performance optimization, Documentation |

**Total: 8 weeks to full implementation**

---

## Risk Mitigation

### Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Behavior tree performance degradation | High | Profile continuously, set performance budgets |
| Script parsing errors | Medium | Comprehensive validation, error recovery |
| LLM hallucination in scripts | High | Validation layer, fallback to direct control |
| Memory leaks in long-running scripts | Medium | Regular profiling, resource limits |

### Operational Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| LLM API downtime | High | Cache pre-warming, fallback responses |
| Script version conflicts | Medium | Version control system, conflict resolution |
| Multi-agent deadlocks | High | Timeout system, deadlock detection |

---

## Conclusion

The "One Abstraction Away" system represents a fundamental shift in how MineWright agents operate. By introducing script-based automation between natural language commands and action execution, we achieve:

1. **Massive token efficiency** - Scripts generate once, execute thousands of times
2. **Improved reliability** - Deterministic execution without constant LLM dependency
3. **Enhanced capabilities** - Multi-agent coordination, proactive behaviors, continuous learning
4. **Better user experience** - Faster responses, more predictable behavior, easier debugging

The implementation plan outlined above provides a clear, phased approach to building this system while maintaining backward compatibility and ensuring production readiness.

---

**Next Steps:**

1. Review and approve this implementation plan
2. Set up development branch: `feature/one-abstraction-away`
3. Begin Phase 1: Foundation implementation
4. Weekly progress reviews with stakeholder demos

---

**Document Version:** 1.0
**Last Updated:** 2026-02-28
**Author:** Technical Lead (Synthesized from Developer Analyses)
**Status:** Ready for Implementation
