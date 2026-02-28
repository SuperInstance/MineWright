# DSL Design for Game Automation Scripts

**Document Version:** 1.0
**Date:** 2026-02-28
**Status:** Research & Design
**Project:** MineWright - Minecraft AI Automation

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [DSL Design Principles](#dsl-design-principles)
3. [Existing Game Scripting DSLs Analysis](#existing-game-scripting-dsls-analysis)
4. [Recommended DSL Syntax for Minecraft](#recommended-dsl-syntax-for-minecraft)
5. [Key DSL Features](#key-dsl-features)
6. [LLM-Generated DSL](#llm-generated-dsl)
7. [Java Implementation Approach](#java-implementation-approach)
8. [Example Scripts](#example-scripts)
9. [References](#references)

---

## Executive Summary

This document researches and recommends a Domain-Specific Language (DSL) design for game automation scripts, specifically tailored for Minecraft automation in the MineWright project. The DSL balances **readability**, **expressiveness**, and **LLM-friendliness** while leveraging proven patterns from existing game scripting languages like Lua, Age of Empires scripts, and ComputerCraft/OpenComputers.

**Key Recommendations:**
- **Hybrid YAML/JavaScript syntax** for structure and logic
- **Interpreted execution** via GraalVM JavaScript (already in project)
- **Dynamic typing** with runtime validation for flexibility
- **Behavior tree inspired** control flow (sequence, selector, parallel)
- **Event-driven triggers** for reactive automation

---

## DSL Design Principles

### 1. Readability vs Power Tradeoff

Based on DSL research from 2025, the key tradeoffs are:

| Aspect | Readability Focus | Power Focus | **Recommended Balance** |
|--------|-------------------|-------------|-------------------------|
| **Syntax** | Natural language, verbose | Concise, symbolic | **YAML-like structure with JavaScript expressions** |
| **Type System** | Dynamic, inferred | Static, declared | **Dynamic with runtime validation** |
| **Control Flow** | Simple if/else/loops | Complex patterns | **Behavior tree primitives (sequence/selector)** |
| **Abstraction** | Flat, linear | Nested, recursive | **Hierarchical with max depth limits** |

**Research Insight:** From [DSL Engineering: Design Principles](https://m.blog.csdn.net/universsky2015/article/details/132770063):
> "DSLs compromise on expressiveness to gain efficiency in a specific domain. Limited expressiveness makes it harder to make mistakes and errors easier to discover."

### 2. Compiled vs Interpreted

**Decision: INTERPRETED**

**Rationale:**
- **Hot reload capability** - Scripts can be updated without restarting
- **Faster iteration** - No compilation step during development
- **Already using GraalVM** - Project has `CodeExecutionEngine.java` with JavaScript support
- **Dynamic game environment** - Minecraft world changes require adaptive scripts

**Tradeoffs:**
- ✅ Pros: Flexibility, rapid development, easier debugging
- ❌ Cons: Slower execution (mitigated by tick-based architecture), runtime errors only

**Implementation Pattern:**
```java
// Existing infrastructure in CodeExecutionEngine.java
public class ScriptInterpreter {
    private final Context graalContext;
    private final ScriptValidator validator;

    public ExecutionResult execute(String scriptSource) {
        // 1. Validate syntax
        if (!validator.validateSyntax(scriptSource)) {
            return ExecutionResult.error("Syntax validation failed");
        }

        // 2. Execute with timeout
        return graalContext.eval("js", scriptSource);
    }
}
```

### 3. Type Safety vs Flexibility

**Decision: DYNAMIC TYPING WITH RUNTIME VALIDATION**

**Rationale:**
- From [Dynamic Language Research](https://m.blog.csdn.net/csdnnews/article/details/131799085):
  > "Dynamic typing allows constructing things static type systems cannot. DSLs benefit from dynamic typing - more concise syntax makes domain semantics clearer."

- **LLM-friendly**: LLMs generate better code for dynamic languages like Lua/JavaScript than for statically-typed languages
- **Easier for non-programmers**: Minecraft players don't want to declare types

**Validation Strategy:**
```yaml
# Three-layer validation
validation:
  syntax_check:
    - parse DSL grammar
    - check for malformed structures

  semantic_check:
    - verify action names exist in ActionRegistry
    - validate block types against Minecraft registry
    - check parameter types at runtime

  security_check:
    - limit recursion depth (max 20)
    - prevent infinite loops (max iterations 10,000)
    - sandbox restrictions (no file/network access)
```

---

## Existing Game Scripting DSLs Analysis

### 1. Lua in Games (WoW, Roblox, ComputerCraft)

**Characteristics:**
- **De facto standard** for game scripting
- Lightweight (~200KB core library)
- Excellent C/C++ integration
- Dynamic typing with JIT compilation support

**Success Patterns:**
```lua
-- ComputerCraft Turtle (Minecraft) - Clear, readable API
turtle.forward()
turtle.dig()
turtle.placeUp()

-- Conditionals
if turtle.getItemCount(16) > 0 then
    turtle.drop()
end

-- Loops
for i = 1, 64 do
    turtle.dig()
end
```

**What to Adopt:**
- ✅ Verb-noun API design (`moveTo`, `mineBlock`)
- ✅ Simple control flow (`if`, `for`, `while`)
- ✅ Dynamic typing
- ✅ Table-based data structures

**What to Avoid:**
- ❌ Lua-specific syntax (1-based indexing, `~=`, `nil` vs `null`)
- ❌ Metatables (too complex for LLM generation)

### 2. Age of Empires AI Scripts

**Characteristics:**
- Rule-based `(defrule)` syntax
- Pattern: conditions → actions
- Event-driven execution

**Success Patterns:**
```age-of-empires
; Simple, declarative rules
(defrule
    (can-build-farm)
    (food-amount >= 60)
=>
    (build-farm)
)

; Priority by order
(defrule
    (enemy-soldiers-nearby > 3)
=>
    (retreat)
    (request-reinforcements)
)
```

**What to Adopt:**
- ✅ Declarative rule structure (perfect for triggers)
- ✅ Condition → action pattern
- ✅ Priority-based execution

**What to Avoid:**
- ❌ Lisp-like parentheses (confusing for non-programmers)

### 3. Unreal Blueprints (Visual Scripting)

**Characteristics:**
- Node-based visual programming
- Data flow + execution flow
- Event-driven with triggers

**Success Patterns:**
```
Event (Overlap) → Condition (Is Enemy?) → Action (Attack)
                      ↓ No
                   Action (Ignore)
```

**What to Adopt:**
- ✅ Event-driven triggers
- ✅ Sequence/selector flow control
- ✅ Visual debugging capability

**What to Avoid:**
- ❌ Visual-only (need text representation for LLMs)

### 4. Minecraft-Specific DSLs

**ComputerCraft:**
```lua
-- Lua-based, very successful
turtle.refuel()
while turtle.detect() do
    turtle.dig()
    turtle.forward()
end
```

**OpenComputers:**
```lua
-- More sophisticated hardware abstraction
computer = require("computer")
robot = require("robot")

while computer.energy() > 1000 do
    robot.swing()
    robot.forward()
end
```

**Key Insights:**
- ✅ Minecraft DSLs should be **block/entity** centric
- ✅ Inventory management is critical
- ✅ Position/coordinates are fundamental
- ✅ Energy/fuel considerations

---

## Recommended DSL Syntax for Minecraft

### DSL Design: MBL (Minecraft Behavior Language)

**Hybrid Approach:** YAML structure + JavaScript expressions

```yaml
# MBL Script Format
metadata:
  name: "Gather Oak Logs"
  version: "1.0.0"
  author: "foreman-alpha"

parameters:
  target_amount: 64
  search_radius: 128

# Behavior tree structure
script:
  type: "sequence"
  steps:
    # Step 1: Check requirements
    - type: "condition"
      condition: "inventory_has('axe')"
      on_false:
        type: "sequence"
        steps:
          - action: "announce"
            message: "No axe available!"
          - action: "terminate"

    # Step 2: Locate trees
    - type: "action"
      action: "locate_nearest"
      params:
        block: "oak_log"
        radius: "{{parameters.search_radius}}"
        save_as: "target_tree"

    # Step 3: Gather (repeat until done)
    - type: "repeat_until"
      condition: "inventory_count('oak_log') >= {{parameters.target_amount}}"
      body:
        type: "sequence"
        steps:
          - action: "pathfind_to"
            target: "@target_tree"

          - action: "face"
            target: "@target_tree"

          - action: "mine_block"
            target: "@target_tree"
            tool: "equipped_axe"

          - condition: "inventory_percent() > 0.9"
            on_true:
              action: "deposit_nearby"
              radius: 32

# Event triggers
triggers:
  - event: "inventory_full"
    action:
      type: "sequence"
      steps:
        - action: "pathfind_to"
          target: "nearest_chest"
        - action: "deposit_all"
          except: ["axe"]

# Error handling
on_error:
  stuck:
    - action: "log"
      message: "Agent stuck, trying alternative path"
    - action: "break_blocks_around"
      radius: 1

  no_resources:
    - action: "announce"
      message: "Task incomplete: out of resources"
```

### Syntax Grammar

```
<script> ::= <metadata> <parameters>? <script_def> <triggers>? <on_error>?

<script_def> ::= <node>

<node> ::= <sequence_node> | <selector_node> | <parallel_node> |
           <repeat_node> | <condition_node> | <action_node>

<sequence_node> ::=
    "type:" "sequence"
    "steps:" "[" <node>+ "]"

<selector_node> ::=
    "type:" "selector"
    "steps:" "[" <node>+ "]"

<parallel_node> ::=
    "type:" "parallel"
    "steps:" "[" <node>+ "]"
    ("max_parallel:" <number>)?

<repeat_node> ::=
    ("type:" "repeat" "count:" <number>) |
    ("type:" "repeat_until" "condition:" <expression>)
    "body:" <node>

<condition_node> ::=
    "type:" "condition"
    ("condition:" <expression>)
    "on_true:" <node>?
    "on_false:" <node>?

<action_node> ::=
    "type:" "action"
    "action:" <identifier>
    ("params:" <parameter_map>)?
    ("target:" <target_ref>)?

<parameter_map> ::= "{" <parameter_entry> ("," <parameter_entry>)* "}"
<parameter_entry> := <identifier> ":" <value>

<value> ::= <string> | <number> | <boolean> | <parameter_ref> | <variable_ref>
<parameter_ref> ::= "{{" <identifier> "}}"
<variable_ref> ::= "@" <identifier>

<triggers> ::= "triggers:" "[" <trigger>+ "]"
<trigger> ::= <event_def> <node>

<event_def> ::= "event:" <identifier>
```

### JavaScript Expression Subset

For conditions and dynamic values, use safe JavaScript subset:

```javascript
// Allowed expressions
inventory_count('oak_log')
inventory_percent()
distance_to(@target_tree)
current_position().y
has_item('diamond_pickaxe')

// Operators
==, !=, <, >, <=, >=, &&, ||, !

// Functions
inventory_count(item)
inventory_percent()
distance_to(target)
has_item(item)
current_position()
get_health()
get_hunger()
```

---

## Key DSL Features

### 1. Conditionals

**If/Else Pattern:**
```yaml
- type: "condition"
  condition: "inventory_count('oak_log') >= 64"
  on_true:
    type: "action"
    action: "announce"
    message: "Task complete!"
  on_false:
    type: "action"
    action: "continue"
```

**Switch Pattern (Selector):**
```yaml
- type: "selector"
  steps:
    # Try diamond pickaxe first
    - type: "condition"
      condition: "has_item('diamond_pickaxe')"
      on_true:
        action: "equip"
        item: "diamond_pickaxe"

    # Fall back to iron
    - type: "condition"
      condition: "has_item('iron_pickaxe')"
      on_true:
        action: "equip"
        item: "iron_pickaxe"

    # Otherwise use stone
    - type: "action"
      action: "equip"
      item: "stone_pickaxe"
```

### 2. Loops

**Fixed Count:**
```yaml
- type: "repeat"
  count: 10
  body:
    type: "sequence"
    steps:
      - action: "mine"
        target: "ahead"
      - action: "move"
        direction: "forward"
```

**Until Condition:**
```yaml
- type: "repeat_until"
  condition: "inventory_count('cobblestone') >= 64"
  body:
    type: "sequence"
    steps:
      - action: "mine"
        target: "ahead"
      - action: "collect"
```

**While Pattern (using negation):**
```yaml
- type: "repeat_until"
  condition: "!has_item('water_bucket')"
  body:
    type: "action"
    action: "fill_bucket"
    source: "nearest_water"
```

### 3. Events and Triggers

**Event Types:**
```yaml
triggers:
  # Health-based triggers
  - event: "health_low"
    threshold: 30
    action:
      type: "sequence"
      steps:
        - action: "use_item"
          item: "golden_apple"
        - action: "retreat"

  # Inventory triggers
  - event: "inventory_full"
    action:
      type: "action"
      action: "deposit_all"
      location: "nearest_chest"

  # Combat triggers
  - event: "entity_spotted"
    entity_type: "zombie"
    distance: "< 20"
    action:
      type: "action"
      action: "attack"
      target: "detected_entity"

  # Time-based triggers
  - event: "time_of_day"
    time: "night"
    action:
      type: "action"
      action: "place_torches"
      interval: 20

  # Position triggers
  - event: "enter_zone"
    zone: "base_area"
    action:
      type: "action"
      action: "deposit_loot"
```

**Event Bus Integration:**
```java
// From existing EventBus pattern in codebase
public class ScriptTriggerManager {
    private final EventBus eventBus;
    private final Map<String, List<ScriptTrigger>> triggers;

    public void registerTriggers(Script script) {
        for (ScriptTrigger trigger : script.getTriggers()) {
            eventBus.subscribe(trigger.getEventType(),
                condition -> trigger.matches(condition),
                event -> executeTrigger(trigger, event)
            );
        }
    }
}
```

### 4. State Persistence

**Variables Across Script Execution:**
```yaml
script:
  type: "sequence"
  steps:
    # Save checkpoint
    - action: "set_state"
      key: "checkpoint_position"
      value: "current_position()"

    # Do work
    - action: "mine_branch"
      length: 50

    # Return to checkpoint
    - action: "pathfind_to"
      target: "@state.checkpoint_position"
```

**Persistence Types:**
```yaml
# Position state
state:
  checkpoint_position: { x: 100, y: 64, z: -200 }
  last_mined_position: null

# Counter state
counters:
  blocks_mined: 127
  trees_chopped: 5

# Flag state
flags:
  has_fuel: true
  visited_nether: false
```

### 5. Parallel Execution

**Multiple Agents:**
```yaml
- type: "parallel"
  max_parallel: 3
  steps:
    - type: "action"
      agent: "foreman-alpha"
      action: "gather"
      resource: "oak_log"

    - type: "action"
      agent: "foreman-beta"
      action: "gather"
      resource: "cobblestone"

    - type: "action"
      agent: "foreman-gamma"
      action: "build"
      structure: "storage_room"
```

**Parallel Actions (Single Agent):**
```yaml
- type: "parallel"
  steps:
    - action: "look_at"
      target: "enemy"

    - action: "equip_weapon"
      weapon: "bow"
```

### 6. Error Handling

**Try-Catch Pattern:**
```yaml
- type: "try"
  body:
    type: "sequence"
    steps:
      - action: "mine_dangerous_block"
        target: "obsidian"
  catch:
    - type: "selector"
      steps:
        - type: "condition"
          condition: "error.type == 'no_tool'"
          on_true:
            action: "announce"
            message: "Need diamond pickaxe!"

        - type: "condition"
          condition: "error.type == 'stuck'"
          on_true:
            action: "break_free"
```

**Global Error Handlers:**
```yaml
on_error:
  timeout:
    - action: "log"
      level: "warning"
      message: "Script timed out after {{timeout}} seconds"
    - action: "return_to_base"

  pathfinding_failed:
    - action: "log"
      level: "error"
      message: "Cannot reach target: @failed_target"
    - action: "mark_unreachable"
      target: "@failed_target"
```

---

## LLM-Generated DSL

### 1. Natural Language to DSL Compilation

**Architecture Pattern:**
```
Natural Language Command
    ↓
LLM TaskPlanner (existing)
    ↓
DSL Generator (new)
    ↓
Script Validator (new)
    ↓
Script Cache (new)
    ↓
Interpreter (CodeExecutionEngine - existing)
```

**Research-Based Approach:**
From [Natural Language to DSL Compiler Research](https://time.geekbang.org/column/article/913122):

1. **DSL Pattern Definition** - Explicit BNF/JSON Schema
2. **Knowledge Retrieval (RAG)** - Inject similar examples
3. **Grammar Prompting** - Constrain output format
4. **Validation** - Post-generation syntax checking

### 2. LLM Prompt Templates for DSL Generation

**Template 1: Script Generation**
```prompt
You are an expert Minecraft automation architect. Generate a DSL script for this task.

TASK: {user_command}

CONTEXT:
- Agent Name: {agent_name}
- Current Position: {position}
- Inventory: {inventory}
- Nearby Blocks: {nearby_blocks}
- Time of Day: {time_of_day}

AVAILABLE ACTIONS:
{action_registry}

DSL GRAMMAR:
<dsl_grammar_specification>

EXAMPLES:
{few_shot_examples}

REQUIREMENTS:
1. Return ONLY valid YAML DSL
2. Use sequence/selector/parallel for control flow
3. Include proper error handling
4. Add comments explaining each step
5. Use condition checks before actions
6. Optimize for efficiency (minimize travel, batch similar actions)

Generate the DSL script:
```

**Template 2: Script Refinement (Hierarchical)**
```prompt
Refine this DSL script based on execution feedback.

ORIGINAL SCRIPT:
{original_dsl}

FAILURE CONTEXT:
- Failed at Step: {failed_step}
- Error Type: {error_type}
- Error Message: {error_message}
- Environment: {environment_snapshot}

REFACTORING SCOPE: {scope}  # line | block | global

INSTRUCTIONS:
1. Fix the specific failure at step {failed_step}
2. Maintain the successful parts of the script
3. Add safeguards to prevent similar failures
4. Preserve the original script's intent
5. Optimize for the same goal

Return ONLY the refined DSL script (no explanations).
```

**Template 3: DSL to Natural Language (Validation)**
```prompt
Explain what this DSL script does in plain English.

DSL SCRIPT:
{dsl_script}

Provide:
1. High-level goal summary (1 sentence)
2. Step-by-step breakdown
3. Potential failure points
4. Suggested improvements

This will be shown to the user for verification.
```

### 3. DSL Validation and Testing

**Multi-Layer Validation:**
```java
public class ScriptValidator {

    public ValidationResult validate(Script script) {
        ValidationResult result = new ValidationResult();

        // Layer 1: Structural Validation
        result.add(validateStructure(script));

        // Layer 2: Semantic Validation
        result.add(validateSemantics(script));

        // Layer 3: Security Validation
        result.add(validateSecurity(script));

        // Layer 4: Feasibility Validation
        result.add(validateFeasibility(script));

        return result;
    }

    private ValidationResult validateStructure(Script script) {
        // Check YAML syntax
        // Verify node structure
        // Check for cycles
        // Validate node tree
        // Ensure required fields present
    }

    private ValidationResult validateSemantics(Script script) {
        // Check action names exist in ActionRegistry
        // Verify parameter types match
        // Validate block types
        // Check expression syntax
        // Verify variable references
    }

    private ValidationResult validateSecurity(Script script) {
        // Limit recursion depth (max 20)
        // Prevent infinite loops
        // Sandbox restrictions
        // Resource limits (max distance, time)
        // No dangerous operations
    }

    private ValidationResult validateFeasibility(Script script) {
        // Estimate execution time
        // Check resource requirements
        // Validate reachability
        // Check inventory capacity
        // Verify tool requirements
    }
}
```

**Dry-Run Testing:**
```java
public class ScriptSimulator {

    public SimulationResult simulate(Script script, WorldState initialState) {
        WorldState simulatedState = initialState.copy();
        SimulationResult result = new SimulationResult();

        try {
            for (ScriptStep step : script.getSteps()) {
                // Check if action is possible
                if (!isActionPossible(step, simulatedState)) {
                    result.addFailure(step, "Action not possible in current state");
                    return result;
                }

                // Apply action effects
                applyEffects(step, simulatedState);
                result.recordStep(step, simulatedState);
            }

            result.setSuccess(true);
        } catch (Exception e) {
            result.setError(e.getMessage());
        }

        return result;
    }
}
```

### 4. Temperature and Parameters for LLM DSL Generation

From [IQuest-Coder Research](https://m.blog.csdn.net/weixin_36299472/article/details/157000597):

```java
public class DSLGenerationConfig {
    // Temperature: Lower = more deterministic syntax
    private static final double TEMPERATURE = 0.3;

    // Top-p: Maintain variety while filtering errors
    private static final double TOP_P = 0.9;

    // Repetition penalty: Prevent syntax loops
    private static final double REPETITION_PENALTY = 1.1;

    // Max tokens: Prevent runaway generation
    private static final int MAX_TOKENS = 2000;

    // Stop sequences: End DSL cleanly
    private static final List<String> STOP_SEQUENCES = List.of(
        "# END_SCRIPT",
        "# END"
    );
}
```

---

## Java Implementation Approach

### Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  ScriptManager                       │
│  - generateScript()                                 │
│  - validateScript()                                 │
│  - executeScript()                                  │
│  - cacheScript()                                    │
└─────────────────┬───────────────────────────────────┘
                  │
    ┌─────────────┼─────────────┬──────────────┐
    │             │             │              │
    ▼             ▼             ▼              ▼
┌────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐
│ Parser │  │Validator│  │Generator│  │ Cache    │
└────────┘  └─────────┘  └──────────┘  └──────────┘
    │
    ▼
┌─────────────────────────────────────────────────────┐
│              ScriptInterpreter                       │
│  - interpret()  [extends CodeExecutionEngine]       │
│  - executeNode()                                    │
│  - handleTriggers()                                 │
└─────────────────────────────────────────────────────┘
```

### Core Classes

**1. Script.java (AST Representation)**
```java
package com.minewright.script;

import java.util.*;

/**
 * Abstract Syntax Tree for DSL scripts.
 */
public class Script {
    private ScriptMetadata metadata;
    private Map<String, Object> parameters;
    private ScriptNode rootNode;
    private List<ScriptTrigger> triggers;
    private Map<String, ScriptNode> errorHandlers;

    // Getters, setters, builders
}

public class ScriptMetadata {
    private String name;
    private String version;
    private String author;
    private Instant createdAt;
    private List<String> tags;
}

public interface ScriptNode {
    NodeType getType();
    <T> accept(ScriptNodeVisitor<T> visitor);
}

enum NodeType {
    SEQUENCE, SELECTOR, PARALLEL, REPEAT, CONDITION, ACTION
}
```

**2. DSLParser.java**
```java
package com.minewright.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses DSL scripts from YAML format.
 */
public class DSLParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(DSLParser.class);
    private final ObjectMapper yamlMapper;

    public DSLParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public Script parse(String yamlSource) throws ScriptParseException {
        try {
            // Parse YAML to generic map
            Map<String, Object> raw = yamlMapper.readValue(yamlSource, new TypeReference<>() {});

            // Validate structure
            validateRootStructure(raw);

            // Build AST
            return buildScript(raw);

        } catch (Exception e) {
            throw new ScriptParseException("Failed to parse DSL: " + e.getMessage(), e);
        }
    }

    private Script buildScript(Map<String, Object> raw) {
        Script script = new Script();

        // Parse metadata
        script.setMetadata(parseMetadata(raw.get("metadata")));

        // Parse parameters
        script.setParameters(parseParameters(raw.get("parameters")));

        // Parse script tree
        script.setRootNode(parseNode(raw.get("script")));

        // Parse triggers
        script.setTriggers(parseTriggers(raw.get("triggers")));

        // Parse error handlers
        script.setErrorHandlers(parseErrorHandlers(raw.get("on_error")));

        return script;
    }

    private ScriptNode parseNode(Object nodeObj) {
        if (!(nodeObj instanceof Map)) {
            throw new ScriptParseException("Node must be a map");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> nodeMap = (Map<String, Object>) nodeObj;
        String type = (String) nodeMap.get("type");

        return switch (type) {
            case "sequence" -> parseSequenceNode(nodeMap);
            case "selector" -> parseSelectorNode(nodeMap);
            case "parallel" -> parseParallelNode(nodeMap);
            case "repeat" -> parseRepeatNode(nodeMap);
            case "repeat_until" -> parseRepeatUntilNode(nodeMap);
            case "condition" -> parseConditionNode(nodeMap);
            case "action" -> parseActionNode(nodeMap);
            default -> throw new ScriptParseException("Unknown node type: " + type);
        };
    }

    private SequenceNode parseSequenceNode(Map<String, Object> map) {
        List<Object> stepsObj = (List<Object>) map.get("steps");
        List<ScriptNode> steps = new ArrayList<>();

        for (Object stepObj : stepsObj) {
            steps.add(parseNode(stepObj));
        }

        return new SequenceNode(steps);
    }

    private ActionNode parseActionNode(Map<String, Object> map) {
        String action = (String) map.get("action");
        Map<String, Object> params = parseParameters(map.get("params"));
        Object target = map.get("target");

        return new ActionNode(action, params, target);
    }

    // ... other node parsers
}
```

**3. ScriptValidator.java**
```java
package com.minewright.script;

import com.minewright.plugin.ActionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates DSL scripts for correctness and safety.
 */
public class ScriptValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptValidator.class);
    private final ActionRegistry actionRegistry;

    private static final int MAX_RECURSION_DEPTH = 20;
    private static final int MAX_LOOP_ITERATIONS = 10000;
    private static final int MAX_SCRIPT_STEPS = 1000;

    public ValidationResult validate(Script script) {
        ValidationResult result = new ValidationResult();

        // Structural validation
        validateStructure(script, result);

        // Semantic validation
        validateSemantics(script, result);

        // Security validation
        validateSecurity(script, result);

        // Feasibility validation
        validateFeasibility(script, result);

        return result;
    }

    private void validateStructure(Script script, ValidationResult result) {
        // Check for required fields
        if (script.getMetadata() == null) {
            result.addError("Missing metadata section");
        }
        if (script.getRootNode() == null) {
            result.addError("Missing script section");
        }

        // Check for cycles
        detectCycles(script.getRootNode(), new HashSet<>(), result);

        // Check depth
        checkDepth(script.getRootNode(), 0, result);
    }

    private void detectCycles(ScriptNode node, Set<ScriptNode> visited, ValidationResult result) {
        if (visited.contains(node)) {
            result.addError("Cycle detected in script graph");
            return;
        }

        visited.add(node);

        for (ScriptNode child : node.getChildren()) {
            detectCycles(child, visited, result);
        }

        visited.remove(node);
    }

    private void checkDepth(ScriptNode node, int depth, ValidationResult result) {
        if (depth > MAX_RECURSION_DEPTH) {
            result.addError("Script exceeds maximum depth of " + MAX_RECURSION_DEPTH);
            return;
        }

        for (ScriptNode child : node.getChildren()) {
            checkDepth(child, depth + 1, result);
        }
    }

    private void validateSemantics(Script script, ValidationResult result) {
        validateActionNames(script.getRootNode(), result);
        validateExpressions(script.getRootNode(), result);
        validateVariableReferences(script, result);
    }

    private void validateActionNames(ScriptNode node, ValidationResult result) {
        if (node.getType() == NodeType.ACTION) {
            ActionNode actionNode = (ActionNode) node;
            String actionName = actionNode.getActionName();

            if (!actionRegistry.hasAction(actionName)) {
                result.addWarning("Unknown action: " + actionName);
            }
        }

        for (ScriptNode child : node.getChildren()) {
            validateActionNames(child, result);
        }
    }

    private void validateSecurity(Script script, ValidationResult result) {
        // Check for dangerous operations
        checkDangerousActions(script.getRootNode(), result);

        // Check resource limits
        checkResourceLimits(script, result);
    }

    private void validateFeasibility(Script script, ValidationResult result) {
        // Estimate execution time
        long estimatedTime = estimateExecutionTime(script);
        if (estimatedTime > 300000) { // 5 minutes
            result.addWarning("Script may take longer than 5 minutes");
        }

        // Check tool requirements
        validateToolRequirements(script, result);
    }
}
```

**4. ScriptInterpreter.java**
```java
package com.minewright.script;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.ActionContext;
import com.minewright.execution.CodeExecutionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interprets and executes DSL scripts.
 * Extends CodeExecutionEngine for JavaScript expression evaluation.
 */
public class ScriptInterpreter extends CodeExecutionEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptInterpreter.class);

    private final ScriptExecutionState state;
    private final ScriptTriggerManager triggerManager;

    public ScriptInterpreter(ForemanEntity foreman, ActionContext actionContext) {
        super(foreman);
        this.state = new ScriptExecutionState(foreman);
        this.triggerManager = new ScriptTriggerManager(foreman, this);
    }

    public ScriptExecutionResult execute(Script script) {
        ScriptExecutionResult result = new ScriptExecutionResult();
        state.reset();

        try {
            // Register triggers
            triggerManager.registerTriggers(script.getTriggers());

            // Execute main script
            executeNode(script.getRootNode(), result);

            result.setSuccess(true);

        } catch (ScriptExecutionException e) {
            result.setSuccess(false);
            result.setError(e.getMessage());

            // Try error handler
            handleScriptError(script, e, result);
        } finally {
            triggerManager.unregisterTriggers();
        }

        return result;
    }

    private void executeNode(ScriptNode node, ScriptExecutionResult result) {
        switch (node.getType()) {
            case SEQUENCE -> executeSequence((SequenceNode) node, result);
            case SELECTOR -> executeSelector((SelectorNode) node, result);
            case PARALLEL -> executeParallel((ParallelNode) node, result);
            case REPEAT -> executeRepeat((RepeatNode) node, result);
            case CONDITION -> executeCondition((ConditionNode) node, result);
            case ACTION -> executeAction((ActionNode) node, result);
        }
    }

    private void executeSequence(SequenceNode node, ScriptExecutionResult result) {
        for (ScriptNode child : node.getSteps()) {
            executeNode(child, result);

            if (result.hasFailed()) {
                return; // Stop on failure
            }
        }
    }

    private void executeSelector(SelectorNode node, ScriptExecutionResult result) {
        for (ScriptNode child : node.getSteps()) {
            ScriptExecutionResult childResult = new ScriptExecutionResult();
            executeNode(child, childResult);

            if (childResult.isSuccess()) {
                result.setSuccess(true);
                return; // First success wins
            }
        }

        result.setSuccess(false); // All failed
    }

    private void executeAction(ActionNode node, ScriptExecutionResult result) {
        String actionName = node.getActionName();
        Map<String, Object> params = resolveParameters(node.getParameters());

        // Create task
        Task task = new Task(actionName, params);

        // Execute via ActionExecutor
        ActionResult actionResult = state.getActionExecutor().executeTask(task);

        if (actionResult.isSuccess()) {
            result.setSuccess(true);
        } else {
            result.setSuccess(false);
            result.setError(actionResult.getMessage());
        }
    }

    private void executeCondition(ConditionNode node, ScriptExecutionResult result) {
        // Evaluate condition expression
        boolean conditionMet = evaluateExpression(node.getCondition());

        if (conditionMet && node.getOnTrue() != null) {
            executeNode(node.getOnTrue(), result);
        } else if (!conditionMet && node.getOnFalse() != null) {
            executeNode(node.getOnFalse(), result);
        } else {
            result.setSuccess(true); // Condition evaluated, no action needed
        }
    }

    private boolean evaluateExpression(String expression) {
        // Use GraalVM to evaluate JavaScript expression
        try {
            Value result = getGraalContext().eval("js", expression);
            return result.asBoolean();
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate expression: {}", expression, e);
            return false;
        }
    }

    private Map<String, Object> resolveParameters(Map<String, Object> params) {
        Map<String, Object> resolved = new HashMap<>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof String strValue) {
                // Resolve parameter references {{param}}
                if (strValue.startsWith("{{") && strValue.endsWith("}}")) {
                    String paramName = strValue.substring(2, strValue.length() - 2);
                    value = state.getParameter(paramName);
                }
                // Resolve variable references @variable
                else if (strValue.startsWith("@")) {
                    String varName = strValue.substring(1);
                    value = state.getVariable(varName);
                }
                // Evaluate as expression
                else if (strValue.contains("(") || strValue.contains("==")) {
                    value = evaluateExpression(strValue);
                }
            }

            resolved.put(entry.getKey(), value);
        }

        return resolved;
    }

    private void handleScriptError(Script script, ScriptExecutionException error, ScriptExecutionResult result) {
        String errorType = error.getErrorType();

        // Check for specific error handler
        ScriptNode errorHandler = script.getErrorHandler(errorType);

        if (errorHandler != null) {
            LOGGER.info("Executing error handler for: {}", errorType);
            ScriptExecutionResult handlerResult = new ScriptExecutionResult();
            executeNode(errorHandler, handlerResult);

            if (handlerResult.isSuccess()) {
                result.setSuccess(true); // Error handled successfully
            }
        }
    }
}
```

**5. ScriptGenerator.java (LLM Integration)**
```java
package com.minewright.script;

import com.minewright.llm.async.AsyncLLMClient;
import com.minewright.llm.async.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Generates DSL scripts from natural language using LLM.
 */
public class ScriptGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptGenerator.class);
    private final AsyncLLMClient llmClient;
    private final DSLParser parser;
    private final ScriptValidator validator;

    // Few-shot examples for better generation
    private static final String[] EXAMPLES = loadExamples();

    public CompletableFuture<Script> generateScriptAsync(String command, ScriptGenerationContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build prompt
                String prompt = buildGenerationPrompt(command, context);

                // Call LLM
                LLMResponse response = llmClient.complete(prompt);

                // Parse DSL
                Script script = parser.parse(response.getContent());

                // Validate
                ValidationResult validation = validator.validate(script);
                if (!validation.isValid()) {
                    throw new ScriptGenerationException("Generated script failed validation: " + validation.getErrors());
                }

                return script;

            } catch (Exception e) {
                LOGGER.error("Failed to generate script for command: {}", command, e);
                throw new ScriptGenerationException("Script generation failed", e);
            }
        });
    }

    private String buildGenerationPrompt(String command, ScriptGenerationContext context) {
        StringBuilder prompt = new StringBuilder(2048);

        prompt.append("You are an expert Minecraft automation architect.\n\n");
        prompt.append("Generate a DSL script for this task:\n");
        prompt.append(command).append("\n\n");

        prompt.append("CONTEXT:\n");
        prompt.append(context.toString()).append("\n\n");

        prompt.append("DSL GRAMMAR:\n");
        prompt.append(getDSLGrammar()).append("\n\n");

        prompt.append("EXAMPLES:\n");
        for (String example : EXAMPLES) {
            prompt.append(example).append("\n\n");
        }

        prompt.append("REQUIREMENTS:\n");
        prompt.append("1. Return ONLY valid YAML DSL\n");
        prompt.append("2. Use sequence/selector/parallel for control flow\n");
        prompt.append("3. Include proper error handling\n");
        prompt.append("4. Add comments explaining each step\n");
        prompt.append("5. Check conditions before actions\n\n");

        prompt.append("Generate the DSL script:");

        return prompt.toString();
    }

    private static String[] loadExamples() {
        // Load from resources or hardcode common patterns
        return new String[] {
            loadExample("gather_oak_logs.yml"),
            loadExample("mine_iron_ore.yml"),
            loadExample("build_shelter.yml")
        };
    }
}
```

---

## Example Scripts

### 1. Simple: Gather Resources

```yaml
metadata:
  name: "Gather Oak Logs"
  version: "1.0.0"
  description: "Collect 64 oak logs from nearby trees"

parameters:
  target_amount: 64
  search_radius: 128

script:
  type: "sequence"
  steps:
    # Check for axe
    - type: "condition"
      condition: "inventory_has('axe', any_type=true)"
      on_false:
        type: "action"
        action: "announce"
        message: "No axe available!"

    # Find and chop trees
    - type: "repeat_until"
      condition: "inventory_count('oak_log') >= {{parameters.target_amount}}"
      body:
        type: "sequence"
        steps:
          - type: "action"
            action: "locate_nearest"
            params:
              block: "oak_log"
              radius: "{{parameters.search_radius}}"
              save_as: "target_tree"

          - type: "condition"
            condition: "@target_tree != null"
            on_false:
              type: "action"
              action: "announce"
              message: "No more trees in range!"

          - type: "action"
            action: "pathfind_to"
            target: "@target_tree"

          - type: "action"
            action: "mine_block"
            target: "@target_tree"

          - type: "condition"
            condition: "inventory_percent() > 0.9"
            on_true:
              type: "action"
              action: "deposit_nearby"
              radius: 32
```

### 2. Complex: Underground Mining

```yaml
metadata:
  name: "Underground Iron Mining"
  version: "2.0.0"
  description: "Mine iron ore from underground caverns"

parameters:
  target_amount: 64
  max_depth: 60
  search_radius: 64

requirements:
  inventory:
    - item: "torch"
      quantity: 16
    - item: "pickaxe"
      condition: "type in ['iron', 'diamond']"

script:
  type: "sequence"
  steps:
    # Preparation
    - type: "sequence"
      name: "prepare"
      steps:
        - type: "action"
          action: "check_requirements"
          params:
            items: "{{requirements.inventory}}"

        - type: "action"
          action: "set_checkpoint"
          params:
            name: "surface_entry"

    # Descend to mining depth
    - type: "sequence"
      name: "descend"
      steps:
        - type: "condition"
          condition: "current_position().y > {{parameters.max_depth}}"
          on_true:
            type: "sequence"
            steps:
              - type: "action"
                action: "mine_staircase_down"
                params:
                  target_depth: "{{parameters.max_depth}}"
                  place_torches: true
                  torch_interval: 10

    # Locate and mine ore
    - type: "selector"
      name: "locate_ore"
      steps:
        # Try visible ore first
        - type: "sequence"
          steps:
            - type: "action"
              action: "scan_visible_blocks"
              params:
                block: "iron_ore"
                radius: 32
                save_as: "visible_ore"

            - type: "condition"
              condition: "@visible_ore != null"
              on_true:
                type: "action"
                action: "save_target"
                params:
                  target: "@visible_ore"

        # Explore caverns
        - type: "sequence"
          steps:
            - type: "action"
              action: "find_nearest_cavern"
              params:
                radius: "{{parameters.search_radius}}"
                save_as: "cavern"

            - type: "action"
              action: "explore_structure"
              params:
                structure: "@cavern"
                block: "iron_ore"
                save_as: "found_ore"

            - type: "condition"
              condition: "@found_ore != null"
              on_true:
                type: "action"
                action: "save_target"
                params:
                  target: "@found_ore"

        # Branch mine as fallback
        - type: "action"
          action: "mine_branch_tunnel"
          params:
            length: 20
            branches: 3
            spacing: 3
            target: "iron_ore"

    # Mine the ore
    - type: "repeat_until"
      condition: "inventory_count('iron_ore') >= {{parameters.target_amount}}"
      body:
        type: "sequence"
        steps:
          - type: "action"
            action: "pathfind_to"
            target: "@saved_target"
            max_distance: 100

          - type: "action"
            action: "mine_block"
            target: "@saved_target"

          - type: "condition"
            condition: "inventory_percent() > 0.9"
            on_true:
              type: "action"
              action: "return_to_checkpoint"
              params:
                checkpoint: "surface_entry"

          - type: "action"
            action: "find_next_target"
            params:
              block: "iron_ore"
              radius: 32

triggers:
  - event: "health_low"
    threshold: 30
    action:
      type: "sequence"
      steps:
        - type: "action"
          action: "return_to_checkpoint"
          params:
            checkpoint: "surface_entry"
        - type: "action"
          action: "consume"
          item: "golden_apple"

on_error:
  stuck:
    - type: "action"
      action: "log"
      level: "warning"
      message: "Agent stuck, breaking blocks around"
    - type: "action"
      action: "break_blocks_around"
      radius: 1
      block_types: ["dirt", "stone"]

  no_resources:
    - type: "action"
      action: "announce"
      message: "Mining complete: reached {{inventory_count('iron_ore')}} iron ore"
```

### 3. Advanced: Multi-Agent Build

```yaml
metadata:
  name: "Coordinated House Build"
  version: "1.0.0"
  description: "Multiple agents build a house in parallel"

parameters:
  width: 7
  height: 4
  depth: 7
  material: "oak_planks"

script:
  type: "sequence"
  steps:
    # Divide work among agents
    - type: "action"
      action: "partition_build_space"
      params:
        width: "{{parameters.width}}"
        depth: "{{parameters.depth}}"
        agents: ["foreman-alpha", "foreman-beta", "foreman-gamma"]
        save_as: "sections"

    # Build in parallel
    - type: "parallel"
      max_parallel: 3
      steps:
        # Agent 1: Front wall and door
        - type: "sequence"
          agent: "foreman-alpha"
          steps:
            - type: "action"
              action: "claim_section"
              section: "@sections[0]"

            - type: "action"
              action: "build_wall"
              params:
                section: "@sections[0]"
                material: "{{parameters.material}}"
                height: "{{parameters.height}}"

            - type: "action"
              action: "place_block"
              params:
                block: "oak_door"
                position: ["front_center", "ground"]

        # Agent 2: Back wall
        - type: "sequence"
          agent: "foreman-beta"
          steps:
            - type: "action"
              action: "claim_section"
              section: "@sections[1]"

            - type: "action"
              action: "build_wall"
              params:
                section: "@sections[1]"
                material: "{{parameters.material}}"
                height: "{{parameters.height}}"

        # Agent 3: Side walls and roof
        - type: "sequence"
          agent: "foreman-gamma"
          steps:
            - type: "action"
              action: "claim_section"
              section: "@sections[2]"

            - type: "action"
              action: "build_walls"
              params:
                sections: ["@sections[2]", "@sections[3]"]
                material: "{{parameters.material}}"
                height: "{{parameters.height}}"

            - type: "action"
              action: "build_roof"
              params:
                width: "{{parameters.width}}"
                depth: "{{parameters.depth}}"
                material: "{{parameters.material}}"
```

---

## References

### Academic & Industry Research

1. **DSL Design Principles**
   - [DSL Engineering: Design Principles for Clean, Flexible](https://m.blog.csdn.net/universsky2015/article/details/132770063) - Core DSL design patterns
   - [深度解析领域特定语言（DSL）第一章——DSL设计基本原则](https://m.blog.csdn.net/weixin_46217641/article/details/148449243) - Comprehensive DSL principles

2. **LLM Code Generation (2025-2026)**
   - [自然语言：LLM转DSL](https://time.geekbang.org/column/article/913122) - Text2DSL methodology
   - [IQuest-Coder-V1-40B：领域特定语言(DSL)生成器](https://m.blog.csdn.net/weixin_36299472/article/details/157000597) - DSL generation parameters
   - [LLMs pose an interesting problem for DSL designers](https://news.ycombinator.com/item?id=44302797) - Hacker News discussion

3. **Game Scripting Languages**
   - Lua in WoW, Roblox - Industry standard for game scripting
   - Age of Empires AI Scripts - Rule-based pattern
   - ComputerCraft/OpenComputers - Minecraft-specific DSLs
   - Unreal Blueprints - Visual scripting paradigm

4. **Type Systems**
   - [最小化动态编程语言的缺点](https://m.blog.csdn.net/csdnnews/article/details/131799085) - Dynamic typing pros/cons
   - [Dynamic vs Static Typing in DSLs](https://max.book118.com/html/2024/0308/5214211234011120.shtm) - Type system research

### Project-Specific References

1. **Existing Codebase**
   - `CodeExecutionEngine.java` - GraalVM JavaScript execution
   - `ActionExecutor.java` - Tick-based action execution
   - `ForemanAPI.java` - Safe API bridge
   - `ResponseParser.java` - LLM response parsing
   - `SCRIPT_GENERATION_SYSTEM.md` - Script generation architecture

2. **Related Research Documents**
   - `PRE_LLM_GAME_AUTOMATION.md` - Historical game automation techniques
   - `BEHAVIOR_TREES_DESIGN.md` - Behavior tree patterns
   - `SELF_MODIFYING_SCRIPTS.md` - Dynamic script modification

---

## Appendix A: DSL Feature Checklist

### Core Features (Required)
- [x] Sequence execution (steps in order)
- [x] Selector execution (try until success)
- [x] Parallel execution (simultaneous actions)
- [x] Conditional branching (if/else)
- [x] Loops (fixed count, until condition)
- [x] Action execution (via ActionRegistry)
- [x] Parameter substitution ({{param}})
- [x] Variable references (@variable)
- [x] Expression evaluation (JavaScript subset)

### Advanced Features (Important)
- [x] Event triggers (health_low, inventory_full, etc.)
- [x] Error handling (try/catch, on_error)
- [x] State persistence (checkpoints, counters)
- [x] Multi-agent coordination
- [x] Resource requirements validation
- [x] Script metadata (name, version, tags)

### Future Features (Nice to Have)
- [ ] Visual debugging UI
- [ ] Script version control integration
- [ ] A/B testing framework
- [ ] Script marketplace/sharing
- [ ] Performance profiling
- [ ] Auto-optimization suggestions

---

## Appendix B: Migration Path

### Phase 1: Foundation (Week 1-2)
- Implement `DSLParser` (YAML parsing)
- Implement `ScriptValidator` (structural validation)
- Create basic node types (Sequence, Selector, Action)
- Unit tests for parser and validator

### Phase 2: Interpreter (Week 3-4)
- Implement `ScriptInterpreter`
- Integrate with existing `CodeExecutionEngine`
- Add expression evaluation via GraalVM
- Integration tests for simple scripts

### Phase 3: LLM Generation (Week 5-6)
- Implement `ScriptGenerator`
- Create prompt templates
- Add few-shot examples
- Test generation quality

### Phase 4: Advanced Features (Week 7-8)
- Implement triggers and events
- Add error handling
- Implement state persistence
- Multi-agent coordination

### Phase 5: Polish (Week 9-10)
- Performance optimization
- Documentation
- Example scripts library
- User testing and feedback

---

**End of Document**

This research document provides a comprehensive foundation for designing and implementing a DSL for game automation in Minecraft, specifically tailored for the MineWright project's architecture and goals.
