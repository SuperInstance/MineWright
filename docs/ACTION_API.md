# MineWright Action API Documentation

This document describes all actions available in MineWright. Actions are the basic units of work that agents can execute.

## Table of Contents

1. [Navigation Actions](#navigation-actions)
   - [pathfind](#action-pathfind)
   - [follow](#action-follow)
2. [Resource Actions](#resource-actions)
   - [mine](#action-mine)
   - [gather](#action-gather)
3. [Building Actions](#building-actions)
   - [place](#action-place)
   - [build](#action-build)
4. [Crafting Actions](#crafting-actions)
   - [craft](#action-craft)
5. [Combat Actions](#combat-actions)
   - [attack](#action-attack)
6. [Action Registry](#action-registry)
7. [Creating Custom Actions](#creating-custom-actions)

---

## Navigation Actions

### Action: `pathfind`

Navigate to a specific coordinate in the world.

#### Parameters

**Required Parameters**
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `x` | int | Target X coordinate | `100` |
| `y` | int | Target Y coordinate | `64` |
| `z` | int | Target Z coordinate | `-200` |

**Optional Parameters**
| Parameter | Type | Default | Description | Example |
|-----------|------|---------|-------------|---------|
| `tolerance` | int | `2` | Distance tolerance in blocks | `5` |

#### Usage Examples

```json
{
  "action": "pathfind",
  "parameters": {
    "x": 100,
    "y": 64,
    "z": -200
  }
}
```

#### Behavior

**Pre-conditions**
- Agent must be spawned
- Target coordinates must be loaded in world

**Execution Process**
1. Agent calculates path to target using Minecraft pathfinding
2. Agent moves along path
3. Action completes when within tolerance distance

**Post-conditions**
- Agent at target location

#### Error Cases

| Error Scenario | Error Message | Resolution |
|----------------|---------------|------------|
| No path found | `"Cannot find path to target"` | Clear obstacles or choose different target |
| Timeout | `"Pathfinding timeout"` | Reduce distance or clear path |

#### State Transitions
- **Starts From**: `IDLE`, `COMPLETED`, `FAILED`
- **Transitions To**: `EXECUTING` -> `COMPLETED`

#### Tick Behavior
- **Tick Interval**: Every tick
- **Blocking**: Yes
- **Interruptible**: Yes

#### Version
- **Since**: 1.0.0

---

### Action: `follow`

Follow a nearby player.

#### Parameters

**Required Parameters**
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `playerName` | string | Name of player to follow | `"Steve"` |

**Optional Parameters**
| Parameter | Type | Default | Description | Example |
|-----------|------|---------|-------------|---------|
| `distance` | int | `3` | Follow distance in blocks | `5` |

#### Usage Examples

```json
{
  "action": "follow",
  "parameters": {
    "playerName": "Steve",
    "distance": 5
  }
}
```

#### Behavior

**Pre-conditions**
- Agent must be spawned
- Player must be online

**Execution Process**
1. Agent locates target player
2. Agent maintains follow distance
3. Continues until cancelled

**Post-conditions**
- Agent following player (continuous action)

#### Error Cases

| Error Scenario | Error Message | Resolution |
|----------------|---------------|------------|
| Player not found | `"Player not found: name"` | Check player is online |
| Player too far | `"Player out of range"` | Move closer |

#### State Transitions
- **Starts From**: `IDLE`
- **Transitions To**: `EXECUTING` (continuous until cancelled)

#### Tick Behavior
- **Tick Interval**: Every tick
- **Blocking**: Yes
- **Interruptible**: Yes

#### Version
- **Since**: 1.0.0

---

## Resource Actions

### Action: `mine`

Mine a specified type of block in the world.

#### Parameters

**Required Parameters**
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `block` | string | Block type to mine (supports common names) | `"iron_ore"` |

**Optional Parameters**
| Parameter | Type | Default | Description | Example |
|-----------|------|---------|-------------|---------|
| `quantity` | int | `8` | Number of blocks to mine | `64` |

#### Usage Examples

```json
{
  "action": "mine",
  "parameters": {
    "block": "diamond_ore",
    "quantity": 10
  }
}
```

#### Behavior

**Pre-conditions**
- Agent must be spawned
- Block type must exist in reachable area

**Execution Process**
1. Agent determines mining direction from player's view
2. Creates tunnel in that direction
3. Searches for target blocks ahead
4. Mines found blocks
5. Places torches for lighting

**Post-conditions**
- Target quantity mined (or fewer if timeout)
- Agent returns to idle

#### Error Cases

| Error Scenario | Error Message | Resolution |
|----------------|---------------|------------|
| Invalid block | `"Invalid block type: xyz"` | Use valid block ID |
| Timeout | `"Mining timeout - only found N"` | Reduce quantity |

#### State Transitions
- **Starts From**: `IDLE`, `COMPLETED`, `FAILED`
- **Transitions To**: `EXECUTING` -> `COMPLETED` on success, `FAILED` on timeout

#### Tick Behavior
- **Tick Interval**: Every tick
- **Blocking**: Yes
- **Interruptible**: Yes
- **Max Duration**: 24000 ticks (20 minutes)

#### Configuration Dependencies
- `config.behavior.actionTickDelay` affects check frequency

#### Implementation Details

**Ore Depth Knowledge**
Agent knows optimal Y levels:
- Diamond: -59 (deepslate)
- Iron: -16 (deepslate), 64 (normal)
- Gold: -16 (deepslate), 32 (normal)
- Coal: 96
- Redstone: -32 (deepslate), 16 (normal)

**Torch Placement**
- Places torch every 100 ticks (5 seconds)
- Places when light level < 8
- Searches for wall/floor position

#### Version
- **Since**: 1.0.0
- **Modified**: 1.2.0 - Added intelligent tunnel mining

---

### Action: `gather`

Gather resources from the environment (higher-level than mine).

#### Parameters

**Required Parameters**
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `resource` | string | Resource to gather | `"wood"` |

**Optional Parameters**
| Parameter | Type | Default | Description | Example |
|-----------|------|---------|-------------|---------|
| `quantity` | int | `16` | Amount to gather | `64` |

#### Usage Examples

```json
{
  "action": "gather",
  "parameters": {
    "resource": "wood",
    "quantity": 32
  }
}
```

#### Behavior

**Pre-conditions**
- Agent must be spawned
- Resource must be available

**Execution Process**
1. Agent locates resource type
2. Agent harvests resource
3. Agent collects drops

**Post-conditions**
- Resource gathered
- Items in agent inventory

#### Error Cases

| Error Scenario | Error Message | Resolution |
|----------------|---------------|------------|
| Resource not found | `"Cannot find resource: type"` | Move to different area |

#### State Transitions
- **Starts From**: `IDLE`, `COMPLETED`, `FAILED`
- **Transitions To**: `EXECUTING` -> `COMPLETED`

#### Version
- **Since**: 1.0.0

---

## Building Actions

### Action: `place`

Place a single block at a specific location.

#### Parameters

**Required Parameters**
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `block` | string | Block type to place | `"stone"` |

**Optional Parameters**
| Parameter | Type | Default | Description | Example |
|-----------|------|---------|-------------|---------|
| `x` | int | `null` | X coordinate (defaults to agent location) | `100` |
| `y` | int | `null` | Y coordinate (defaults to agent location) | `64` |
| `z` | int | `null` | Z coordinate (defaults to agent location) | `-200` |

#### Usage Examples

```json
{
  "action": "place",
  "parameters": {
    "block": "cobblestone",
    "x": 100,
    "y": 64,
    "z": -200
  }
}
```

#### Behavior

**Pre-conditions**
- Agent must be at or can reach target location
- Block must be placeable at target

**Execution Process**
1. Agent moves to target if needed
2. Agent places block

**Post-conditions**
- Block placed at location

#### Error Cases

| Error Scenario | Error Message | Resolution |
|----------------|---------------|------------|
| Invalid block | `"Invalid block type: xyz"` | Use valid block ID |
| Cannot place | `"Cannot place block at location"` | Check location is valid |

#### Version
- **Since**: 1.0.0

---

### Action: `build`

Build a structure from a template or procedurally generate one. Supports multi-agent collaborative building.

#### Parameters

**Required Parameters**
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `structure` | string | Structure type name | `"house"` |

**Optional Parameters**
| Parameter | Type | Default | Description | Example |
|-----------|------|---------|-------------|---------|
| `blocks` | list | `["oak_planks"]` | Blocks to use for building | `["oak_planks", "cobblestone", "glass"]` |
| `material` | string | `"oak_planks"` | Single material (if blocks not specified) | `"stone"` |
| `dimensions` | list | `[9, 6, 9]` | Structure dimensions [width, height, depth] | `[15, 10, 15]` |
| `width` | int | `9` | Structure width | `15` |
| `height` | int | `6` | Structure height | `10` |
| `depth` | int | `9` | Structure depth | `15` |

#### Usage Examples

**Basic House**
```json
{
  "action": "build",
  "parameters": {
    "structure": "house",
    "blocks": ["oak_planks", "cobblestone", "glass_pane"]
  }
}
```

**Large Castle**
```json
{
  "action": "build",
  "parameters": {
    "structure": "castle",
    "material": "stone_bricks",
    "dimensions": [30, 20, 30]
  }
}
```

**Custom Size Shelter**
```json
{
  "action": "build",
  "parameters": {
    "structure": "shelter",
    "width": 7,
    "height": 5,
    "depth": 7
  }
}
```

#### Behavior

**Pre-conditions**
- Agent must be spawned
- Suitable ground must exist in player's view

**Execution Process**
1. Agent finds suitable building site in player's view
2. Tries to load structure from NBT template
3. Falls back to procedural generation if no template
4. Registers build with CollaborativeBuildManager
5. Other agents can join the build
6. Places blocks one at a time with particle effects

**Post-conditions**
- Structure built
- Agent returns to idle

#### Collaborative Building

When multiple agents execute the same build action:
- First agent creates and registers the build
- Subsequent agents join the existing build
- Work is divided spatially among agents
- Progress is tracked centrally
- Agents claim sections atomically

#### Error Cases

| Error Scenario | Error Message | Resolution |
|----------------|---------------|------------|
| Invalid structure | `"Cannot generate build plan: type"` | Use valid structure type |
| No suitable ground | `"Cannot find suitable ground"` | Look at different area |

#### State Transitions
- **Starts From**: `IDLE`, `COMPLETED`, `FAILED`
- **Transitions To**: `EXECUTING` -> `COMPLETED`

#### Tick Behavior
- **Tick Interval**: Every tick
- **Blocking**: Yes
- **Interruptible**: Yes
- **Max Duration**: 120000 ticks (100 minutes)
- **Blocks per Tick**: 1

#### Configuration Dependencies
- `config.behavior.actionTickDelay` affects placement speed

#### Implementation Details

**Structure Loading Priority**
1. Tries NBT template from `data/structures/`
2. Falls back to `StructureGenerators.generate()`

**Supported Structure Types**
- `house` - Basic house with door and windows
- `shelter` - Simple shelter
- `tower` - Watch tower
- `wall` - Straight wall
- `castle` - Castle structure
- `farm` - Farm plot
- Custom - Any name for procedural generation

**Building Site Requirements**
- Ground must be relatively flat (max 2 block height variation)
- Area must be clear of obstructions
- Searches in expanding circles from look target

#### Version
- **Since**: 1.0.0
- **Modified**: 1.3.0 - Added collaborative building

---

## Crafting Actions

### Action: `craft`

Craft an item using available materials.

#### Parameters

**Required Parameters**
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `item` | string | Item to craft | `"iron_pickaxe"` |

**Optional Parameters**
| Parameter | Type | Default | Description | Example |
|-----------|------|---------|-------------|---------|
| `quantity` | int | `1` | Number to craft | `5` |

#### Usage Examples

```json
{
  "action": "craft",
  "parameters": {
    "item": "iron_pickaxe",
    "quantity": 2
  }
}
```

#### Behavior

**Pre-conditions**
- Agent must have required materials
- Crafting table must be accessible (if needed)

**Execution Process**
1. Agent verifies materials available
2. Agent crafts item

**Post-conditions**
- Item crafted
- Materials consumed

#### Error Cases

| Error Scenario | Error Message | Resolution |
|----------------|---------------|------------|
| Unknown item | `"Unknown recipe: item"` | Use valid item name |
| Missing materials | `"Missing materials: list"` | Provide materials |

#### Version
- **Since**: 1.0.0

---

## Combat Actions

### Action: `attack`

Attack a target entity.

#### Parameters

**Required Parameters**
| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `target` | string | Target entity type or name | `"zombie"` |

**Optional Parameters**
| Parameter | Type | Default | Description | Example |
|-----------|------|---------|-------------|---------|
| `distance` | int | `10` | Max search distance | `20` |

#### Usage Examples

```json
{
  "action": "attack",
  "parameters": {
    "target": "zombie"
  }
}
```

#### Behavior

**Pre-conditions**
- Agent must be spawned
- Target must be within range

**Execution Process**
1. Agent locates target
2. Agent moves to attack range
3. Agent attacks until target dead or timeout

**Post-conditions**
- Target eliminated
- Agent returns to idle

#### Error Cases

| Error Scenario | Error Message | Resolution |
|----------------|---------------|------------|
| Target not found | `"No target found: type"` | Target must be in range |

#### Version
- **Since**: 1.0.0

---

## Action Registry

The `ActionRegistry` manages all available actions using the Registry Pattern.

### Registering Actions

```java
ActionRegistry registry = ActionRegistry.getInstance();

// Simple registration
registry.register("myaction", (foreman, task, ctx) ->
    new MyAction(foreman, task));

// With priority and plugin ID
registry.register("myaction", factory, 100, "my-plugin");
```

### Creating Actions

```java
BaseAction action = registry.createAction(
    "mine",           // action name
    foremanEntity,    // agent
    task,             // task with parameters
    actionContext     // context with dependencies
);
```

### Checking Actions

```java
boolean exists = registry.hasAction("mine");
Set<String> allActions = registry.getRegisteredActions();
```

---

## Creating Custom Actions

### Step 1: Extend BaseAction

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;

public class MyCustomAction extends BaseAction {

    public MyCustomAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        // Initialize action
        // Validate parameters
        String value = task.getStringParameter("myparam", "default");
        // Setup resources
    }

    @Override
    protected void onTick() {
        // Called every game tick
        // Do work here
        // Check completion
        if (isWorkComplete()) {
            result = ActionResult.success("Work completed!");
        }
    }

    @Override
    protected void onCancel() {
        // Cleanup when cancelled
    }

    @Override
    public String getDescription() {
        return "Description shown to users";
    }
}
```

### Step 2: Create Plugin

```java
package com.myplugin;

import com.minewright.plugin.*;
import com.minewright.di.ServiceContainer;
import com.minewright.action.actions.MyCustomAction;

public class MyPlugin implements ActionPlugin {

    @Override
    public String getPluginId() {
        return "my-plugin";
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        registry.register("myaction",
            (foreman, task, ctx) -> new MyCustomAction(foreman, task),
            100,  // priority
            getPluginId()
        );
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String[] getDependencies() {
        return new String[0];  // No dependencies
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "My custom action plugin";
    }

    @Override
    public void onUnload() {
        // Cleanup if needed
    }
}
```

### Step 3: Register Plugin

Create file: `META-INF/services/com.minewright.plugin.ActionPlugin`

```
com.myplugin.MyPlugin
```

### Step 4: Update Prompt Builder

Add action description to `PromptBuilder` so LLM knows about it:

```java
private void buildActionDescriptions() {
    actions.put("myaction", "Description for LLM");
}
```

---

## Action Best Practices

### Do's

1. **Keep actions focused** - Single responsibility
2. **Handle errors gracefully** - Return descriptive ActionResult
3. **Use tick-based execution** - Don't block the thread
4. **Validate parameters** - In `onStart()` method
5. **Clean up resources** - In `onCancel()` method
6. **Provide descriptions** - For UI and debugging

### Don'ts

1. **Don't block** - Avoid long-running operations in tick
2. **Don't assume success** - Always check conditions
3. **Don't forget timeouts** - Prevent infinite actions
4. **Don't ignore cancellations** - Always handle onCancel
5. **Don't hardcode values** - Use parameters when possible

---

## Common Patterns

### Polling Pattern

For waiting on external conditions:

```java
@Override
protected void onTick() {
    if (ticksRunning++ > maxTicks) {
        result = ActionResult.failure("Timeout");
        return;
    }

    if (checkCondition()) {
        result = ActionResult.success("Done!");
    }
}
```

### Progress Tracking

For long-running actions:

```java
@Override
public String getDescription() {
    return String.format("Processing (%d/%d)", current, total);
}
```

### State Machine

For complex actions with multiple phases:

```java
private enum Phase { INITIALIZING, WORKING, FINALIZING }
private Phase currentPhase = Phase.INITIALIZING;

@Override
protected void onTick() {
    switch (currentPhase) {
        case INITIALIZING -> { /* init */ currentPhase = Phase.WORKING; }
        case WORKING -> { /* work */ if (done) currentPhase = Phase.FINALIZING; }
        case FINALIZING -> { /* finish */ result = ActionResult.success("Done"); }
    }
}
```
