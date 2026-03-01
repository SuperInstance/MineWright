# Script DSL Guide

## Overview

The MineWright Script DSL (Domain-Specific Language) provides a JSON-based format for defining automation patterns for AI agents. Scripts enable agents to execute complex behaviors in real-time without requiring LLM intervention for every action.

## Architecture

### Three-Layer Model

```
┌─────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (Strategic)                     │
│                         LLM Agents                              │
│                                                                 │
│   • Planning, strategy, logistics                              │
│   • Creating and refining automation scripts                   │
│   • High-level goal setting                                    │
│                                                                 │
│   Token Usage: LOW (batched, infrequent calls)                 │
│   Update Frequency: Every 30-60 seconds or on events           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates & Refines
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Operational)                    │
│                    Behavior Automations                         │
│                                                                 │
│   • Behavior trees, FSMs, macro scripts                        │
│   • Pathfinding, mining, building patterns                     │
│   • Reactive behaviors (danger response, opportunities)        │
│   • Idle behaviors (wandering, chatting, self-improvement)     │
│                                                                 │
│   Token Usage: ZERO (runs locally)                             │
│   Update Frequency: Every tick (20 TPS)                        │
└─────────────────────────────────────────────────────────────────┘
```

### Core Components

1. **ScriptDSL** - Grammar and schema definition
2. **Script** - Complete automation definition
3. **ScriptNode** - Behavior tree nodes
4. **Action** - Action model (separate from ScriptNode)
5. **Trigger** - Event/condition triggers
6. **ScriptParser** - DSL parser
7. **ScriptValidator** - Safety validation
8. **ScriptRegistry** - Storage and retrieval

## Script Structure

### JSON Schema

```json
{
  "id": "string (unique identifier)",
  "name": "string (human-readable name)",
  "description": "string (what this script does)",
  "version": "string (semantic version)",
  "triggers": [
    {
      "type": "EVENT|CONDITION|TIME|PLAYER_ACTION",
      "condition": "expression string",
      "delay": "number (ticks, optional)",
      "cooldown": "number (ticks, optional)"
    }
  ],
  "variables": {
    "varName": "value (string|number|boolean)"
  },
  "actions": [
    {
      "type": "SEQUENCE|PARALLEL|CONDITIONAL|LOOP|ATOMIC",
      "command": "string (for ATOMIC actions)",
      "parameters": {
        "key": "value"
      },
      "condition": "expression (for CONDITIONAL)",
      "iterations": "number (for LOOP)",
      "children": [ ...nested actions... ]
    }
  ]
}
```

## Trigger Types

### EVENT

Fired when a specific game event occurs.

```json
{
  "type": "EVENT",
  "condition": "block_broken == \"oak_log\"",
  "cooldown": 100
}
```

### CONDITION

Continuously evaluated, fires when condition becomes true.

```json
{
  "type": "CONDITION",
  "condition": "inventory_count(\"oak_log\") < 10",
  "delay": 20,
  "cooldown": 200
}
```

### TIME

Fired at regular time intervals.

```json
{
  "type": "TIME",
  "condition": "time_of_day() == \"day\"",
  "delay": 1200  // 1 minute
}
```

### PLAYER_ACTION

Fired when player performs specific action.

```json
{
  "type": "PLAYER_ACTION",
  "condition": "player_action == \"broke_block\"",
  "cooldown": 50
}
```

## Action Types

### SEQUENCE

Execute children in order, all must succeed.

```yaml
actions:
  - type: SEQUENCE
    children:
      - type: ATOMIC
        command: move
        parameters:
          x: 100
          z: 200
      - type: ATOMIC
        command: mine
        parameters:
          block: oak_log
```

### PARALLEL

Execute all children simultaneously.

```yaml
actions:
  - type: PARALLEL
    children:
      - type: ATOMIC
        command: look
        parameters:
          direction: north
      - type: ATOMIC
        command: attack
        parameters:
          target: hostile
```

### CONDITIONAL

Execute children if condition is true.

```yaml
actions:
  - type: CONDITIONAL
    condition: "inventory_count(\"oak_log\") < 10"
    children:
      - type: ATOMIC
        command: pathfind
        parameters:
          target: nearest
          block_type: oak_log
```

### LOOP

Repeat children N times.

```yaml
actions:
  - type: LOOP
    iterations: 10
    children:
      - type: ATOMIC
        command: mine
        parameters:
          block: stone
```

### ATOMIC

Execute a single game command.

```yaml
actions:
  - type: ATOMIC
    command: mine
    parameters:
      block: oak_log
      quantity: 5
```

## Condition Expressions

### Built-in Functions

#### Inventory Functions
- `inventory_count("item")` - Returns count of item in inventory
- `inventory_has("item")` - Returns true if item is in inventory
- `inventory_space()` - Returns available inventory slots

#### Distance/Position Functions
- `distance_to(entity)` - Returns distance to entity
- `distance_to_nearest("block_type")` - Returns distance to nearest block type
- `distance_to_player()` - Returns distance to player

#### Entity State Functions
- `health_percent()` - Returns entity health as percentage
- `hunger_level()` - Returns entity hunger level
- `is_on_ground()` - Returns true if entity is on ground

#### World State Functions
- `time_of_day()` - Returns current time of day (day/night/dusk/dawn)
- `is_raining()` - Returns true if currently raining
- `block_at(x, y, z)` - Returns block type at position
- `blocks_in_range("block_type", radius)` - Returns count of blocks in range

#### Task State Functions
- `is_executing()` - Returns true if currently executing task
- `last_task_succeeded()` - Returns true if last task succeeded

### Operators

- Comparison: `==`, `!=`, `<`, `<=`, `>`, `>=`
- Logical: `and`, `or`, `not`, `&&`, `||`, `!`

### Examples

```
inventory_count("oak_log") > 10
distance_to_player() < 20
health_percent() < 50 and is_on_ground()
time_of_day() == "day" and not is_raining()
```

## Variables

Scripts can maintain state through variables. Variables are prefixed with `$` in conditions.

```yaml
variables:
  target_block: oak_log
  min_quantity: 10
  return_to_base: false

actions:
  - type: LOOP
    iterations: $min_quantity
    children:
      - type: ATOMIC
        command: mine
        parameters:
          block: $target_block
```

## Validation Rules

1. **Unique ID**: Script must have unique ID
2. **Triggers or Actions**: At least one must be defined
3. **Tree Structure**: Actions must form valid tree (no cycles)
4. **Atomic Commands**: Atomic actions must have valid command
5. **Loop Iterations**: Must be positive integer (max 1000)
6. **Condition Syntax**: Must be syntactically valid
7. **Variable References**: Must be defined in variables section
8. **Maximum Depth**: 20 levels
9. **Maximum Nodes**: 500 total nodes

## Example Scripts

### Auto Mining Script

```yaml
id: auto_mine_oak
name: Auto Mine Oak Logs
description: Automatically mines oak logs when inventory is empty
version: 1.0.0

triggers:
  - type: CONDITION
    condition: inventory_count("oak_log") < 5
    cooldown: 100

variables:
  target_block: oak_log
  min_quantity: 10

actions:
  - type: CONDITIONAL
    condition: distance_to_nearest("oak_log") < 50
    children:
      - type: SEQUENCE
        children:
          - type: ATOMIC
            command: pathfind
            parameters:
              target: nearest
              block_type: $target_block
          - type: LOOP
            iterations: $min_quantity
            children:
              - type: ATOMIC
                command: mine
                parameters:
                  block: $target_block
```

### Building Script

```yaml
id: auto_build_shelter
name: Auto Build Shelter
description: Builds a simple shelter when night approaches
version: 1.0.0

triggers:
  - type: CONDITION
    condition: time_of_day() == "night" and distance_to_player() < 10
    cooldown: 6000

actions:
  - type: SEQUENCE
    children:
      - type: ATOMIC
        command: place
        parameters:
          block: cobblestone
          x: 0
          y: 0
          z: 0
      - type: ATOMIC
        command: place
        parameters:
          block: oak_planks
          x: 0
          y: 1
          z: 0
```

## API Usage

### Creating a Script

```java
Script script = Script.builder()
    .metadata(Script.ScriptMetadata.builder()
        .id("auto_mine")
        .name("Auto Mine")
        .description("Automatically mines resources")
        .build())
    .scriptNode(ScriptNode.builder()
        .type(ScriptNode.NodeType.SEQUENCE)
        .addChild(ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .addParameter("block", "oak_log")
            .build())
        .build())
    .build();
```

### Registering a Script

```java
ScriptRegistry registry = ScriptRegistry.getInstance();
registry.register(script);
```

### Retrieving a Script

```java
Script script = registry.get("auto_mine");
List<Script> miningScripts = registry.searchByName("mine");
```

### Validating a Script

```java
ScriptValidator validator = new ScriptValidator();
ScriptValidator.ValidationResult result = validator.validate(script);

if (!result.isValid()) {
    for (String error : result.getErrors()) {
        LOGGER.error("Validation error: {}", error);
    }
}
```

## File Locations

- **Source Code**: `src/main/java/com/minewright/script/`
- **Tests**: `src/test/java/com/minewright/script/`
- **Documentation**: `docs/SCRIPT_DSL_GUIDE.md`

## Key Classes

| Class | Purpose |
|-------|---------|
| `ScriptDSL` | Grammar and schema definition |
| `Script` | Complete automation definition |
| `ScriptNode` | Behavior tree node |
| `Action` | Action model |
| `Trigger` | Trigger model |
| `ScriptParser` | DSL parser |
| `ScriptValidator` | Safety validation |
| `ScriptRegistry` | Storage and retrieval |

## Best Practices

1. **Start Simple**: Begin with basic scripts and add complexity gradually
2. **Test Thoroughly**: Use ScriptValidator before registering scripts
3. **Document Well**: Include clear descriptions in metadata
4. **Version Control**: Use semantic versioning for script updates
5. **Error Handling**: Always include error handlers for failure cases
6. **Resource Limits**: Respect maximum depth and node counts
7. **Cooldown Management**: Use appropriate cooldowns to prevent spam
8. **Condition Optimization**: Keep conditions simple and efficient

## Future Enhancements

- [ ] Script DSL editor with syntax highlighting
- [ ] Visual script editor (node-based)
- [ ] Script marketplace for sharing templates
- [ ] Auto-generation from natural language
- [ ] Script performance profiling
- [ ] Hot-reloading of script changes
- [ ] Script debugging tools
- [ ] Integration with behavior tree editors

## References

- **CLAUDE.md**: Project overview and architecture
- **SCRIPT_CACHE_IMPLEMENTATION.md**: Script caching system
- **SCRIPT_LAYER_LEARNING_SYSTEM.md**: Learning system integration
- **MINECRAFT_FORGE_DOCS**: Minecraft modding documentation
