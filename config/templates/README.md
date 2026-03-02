# Script Template Library

This directory contains reusable script templates for common Minecraft tasks.

## Overview

Script templates are JSON files that define parameterized behavior trees. They can be loaded and instantiated with custom parameters using the `ScriptTemplateLoader` class.

## Available Templates

### 1. mining_basic.json
**Purpose:** Basic mining automation with tool durability management

**Parameters:**
- `block_type` (string, required): Type of block to mine
- `target_quantity` (integer, default: 64): Number of blocks to mine
- `search_radius` (integer, default: 64): Search radius for finding blocks
- `storage_location` (string, default: ${chest_nearby}): Location to store items

**Features:**
- Automatic tool durability checking
- Return to storage when inventory full
- Tool replacement when broken
- Error handling for common failures

### 2. building_house.json
**Purpose:** Build complete houses with area clearing, foundation, walls, and roof

**Parameters:**
- `width` (integer, default: 7): House width in blocks
- `depth` (integer, default: 7): House depth in blocks
- `height` (integer, default: 4): House height in blocks
- `wall_material` (string, required): Material for walls
- `foundation_material` (string, default: cobblestone): Material for foundation
- `roof_material` (string, default: oak_stairs): Material for roof
- `build_location` (string, default: ${player_position} + [5, 0, 5]): Build location
- `clear_area` (boolean, default: true): Clear vegetation before building

**Features:**
- Area clearing (vegetation, obstacles)
- Foundation laying
- Wall construction
- Roof building
- Door and window placement
- Resource gathering if insufficient materials

### 3. farming_auto.json
**Purpose:** Automated crop farming with planting, growth monitoring, and harvesting

**Parameters:**
- `crop_type` (string, required): Type of crop (wheat, carrots, potatoes, beetroots, pumpkin, melon)
- `farm_size` (integer, default: 8): Number of plots per row
- `farm_location` (string, required): Location of the farm
- `auto_bone_meal` (boolean, default: true): Use bone meal to accelerate growth
- `storage_chest` (string, default: ${chest_nearby}): Chest for harvested crops
- `continuous_mode` (boolean, default: true): Keep farming indefinitely

**Features:**
- Automatic seed replanting
- Bone meal application
- Mature crop detection
- Inventory management with storage chest
- Continuous farming mode

### 4. combat_defense.json
**Purpose:** Defensive combat AI that attacks threats and retreats when low on health

**Parameters:**
- `engage_distance` (integer, default: 16): Maximum distance to engage enemies
- `flee_health_threshold` (integer, default: 30): Health percentage to trigger retreat
- `safe_location` (string, default: ${player_position}): Location to retreat to
- `priority_targets` (array, default: [zombie, skeleton, spider, creeper]): Enemy types to prioritize
- `use_shield` (boolean, default: true): Use shield for blocking
- `max_combat_time` (integer, default: 60): Maximum seconds in combat

**Features:**
- Enemy detection and prioritization
- Health-based retreat logic
- Shield blocking
- Creeper avoidance
- Combat timeout

### 5. exploration_safe.json
**Purpose:** Safe exploration with POI marking and night return

**Parameters:**
- `exploration_radius` (integer, default: 200): Maximum distance from home
- `home_location` (string, default: ${spawn_point}): Location to return to
- `return_at_night` (boolean, default: true): Return home when night falls
- `mark_points_of_interest` (boolean, default: true): Mark villages, temples, etc.
- `avoid_hostiles` (boolean, default: true): Avoid areas with hostile mobs
- `max_exploration_time` (integer, default: 600): Maximum exploration time in seconds

**Features:**
- Boundary enforcement
- Night return logic
- Point of interest detection
- Hostile avoidance
- Automatic torch placement
- Lost recovery with compass

### 6. storage_organize.json
**Purpose:** Intelligent inventory organization with categorization

**Parameters:**
- `storage_chest_location` (string, required): Location of storage chests
- `sort_method` (string, default: category): How to sort (category, value, name, rarity)
- `keep_valuables` (boolean, default: true): Keep valuable items in inventory
- `discard_junk` (boolean, default: true): Discard low-value items
- `junk_threshold` (integer, default: 5): Max stack size to consider as junk
- `organize_hotbar` (boolean, default: true): Organize hotbar for quick access
- `consolidate_stacks` (boolean, default: true): Consolidate partial stacks

**Features:**
- Category-based sorting
- Junk identification and discard
- Valuable item retention
- Hotbar organization
- Stack consolidation
- Storage by category

## Template Format

```json
{
  "metadata": {
    "id": "template_id",
    "name": "Template Name",
    "description": "Description of what this template does",
    "author": "Author Name",
    "version": "1.0.0",
    "tags": ["tag1", "tag2"],
    "category": "category_name"
  },
  "parameters": [
    {
      "name": "parameter_name",
      "type": "string|integer|boolean|array",
      "default": "default_value",
      "required": false,
      "description": "Parameter description",
      "allowed_values": ["value1", "value2"]
    }
  ],
  "requirements": {
    "inventory": [{"item": "item_name", "quantity": 10}],
    "tools": ["tool_name"],
    "maxExecutionTime": 600,
    "maxDistance": 100
  },
  "triggers": [
    {
      "type": "manual|time|entity_detected|inventory_full|interval",
      "condition": "condition_expression",
      "description": "Trigger description"
    }
  ],
  "root_node": {
    "type": "sequence|selector|parallel|action|condition|loop|if",
    "children": [...],
    "action": "action_name",
    "condition": "condition_expression",
    "parameters": {"key": "value"}
  },
  "error_handlers": {
    "error_type": [...handler nodes...]
  },
  "telemetry": {
    "logLevel": "info|debug|warn|error",
    "metrics": ["metric1", "metric2"]
  }
}
```

## Usage Examples

### Loading a Template

```java
import com.minewright.script.ScriptTemplateLoader;
import com.minewright.script.ScriptTemplate;

// Load a template
ScriptTemplate template = ScriptTemplateLoader.load("mining_basic");

// Load all templates
List<ScriptTemplate> allTemplates = ScriptTemplateLoader.loadAll();

// Load by category
List<ScriptTemplate> miningTemplates =
    ScriptTemplateLoader.loadByCategory("resource_gathering");
```

### Instantiating a Template

```java
import com.minewright.script.Script;
import java.util.Map;
import java.util.HashMap;

// Set parameters
Map<String, Object> params = new HashMap<>();
params.put("block_type", "diamond_ore");
params.put("target_quantity", 32);
params.put("search_radius", 100);

// Instantiate template
Script script = template.instantiate(params);

// Or instantiate with defaults
Script script = template.instantiate();
```

### Using the Script

```java
import com.minewright.script.ScriptExecution;

// Create execution context
ScriptExecution execution = new ScriptExecution(script, agent);

// Execute the script
execution.start();
execution.awaitCompletion();
```

## Node Types

### SEQUENCE
Execute children in order. All must succeed.

```json
{
  "type": "sequence",
  "children": [
    {"type": "action", "action": "step1"},
    {"type": "action", "action": "step2"}
  ]
}
```

### SELECTOR
Try children in order until one succeeds.

```json
{
  "type": "selector",
  "children": [
    {"type": "condition", "condition": "success_condition"},
    {"type": "action", "action": "fallback_action"}
  ]
}
```

### PARALLEL
Execute all children simultaneously.

```json
{
  "type": "parallel",
  "children": [
    {"type": "action", "action": "task1"},
    {"type": "action", "action": "task2"}
  ]
}
```

### ACTION
Execute a single atomic action.

```json
{
  "type": "action",
  "action": "mine",
  "parameters": {
    "block": "iron_ore",
    "quantity": 64
  }
}
```

### CONDITION
Check if a condition is true.

```json
{
  "type": "condition",
  "condition": "inventory_has('pickaxe', 1)"
}
```

### LOOP
Repeat child nodes N times.

```json
{
  "type": "loop",
  "parameters": {"iterations": 5},
  "children": [
    {"type": "action", "action": "repeat_task"}
  ]
}
```

### IF
Execute if branch based on condition.

```json
{
  "type": "if",
  "condition": "health_percent() > 50",
  "then": {
    "type": "action",
    "action": "attack"
  },
  "else": {
    "type": "action",
    "action": "retreat"
  }
}
```

## Condition Expressions

- `inventory_has('item', count)`: Check inventory for items
- `inventory_count('item') >= count`: Count items in inventory
- `inventory_free_slots() > n`: Check free inventory slots
- `distance_to(target) < distance`: Check distance to target
- `health_percent() < threshold`: Check health percentage
- `time_of_day() == 'day|night|dusk|dawn'`: Check time of day
- `blocks_in_range(block_type, radius) >= count`: Count nearby blocks
- `is_crop_mature('crop_type', position)`: Check crop maturity
- `nearest_hostile_distance() < distance`: Check distance to hostiles

## Available Actions

- `mine`: Mine a block
- `place`: Place a block
- `pathfind`: Navigate to location
- `craft`: Craft an item
- `gather`: Collect resources
- `build`: Build a structure
- `equip`: Equip an item
- `deposit`: Store items in chest
- `withdraw`: Withdraw items from chest
- `attack`: Attack a target
- `follow`: Follow an entity
- `say`: Send chat message
- `announce`: Send announcement
- `log`: Write log message

## Creating Custom Templates

1. Create a new JSON file in this directory
2. Follow the template format above
3. Define metadata, parameters, and behavior tree
4. Test your template using `ScriptTemplateLoader`
5. Add documentation to this README

## Best Practices

1. **Parameterization**: Use parameters for values that users might want to customize
2. **Error Handling**: Include error handlers for common failure cases
3. **Validation**: Add conditions to check prerequisites before actions
4. **Fallbacks**: Use selectors to provide fallback behaviors
5. **Resource Management**: Check inventory and tool durability before actions
6. **Telemetry**: Include metrics for monitoring script performance

## Hot Reloading

For development, templates can be reloaded without restarting:

```java
// Reload all templates
ScriptTemplateLoader.reload();

// Reload specific template
ScriptTemplate template = ScriptTemplateLoader.reload("template_id");

// Clear cache
ScriptTemplateLoader.clearCache();
```

## Categories

Templates are organized by category:

- `resource_gathering`: Mining, farming, logging
- `construction`: Building, structure creation
- `combat`: Combat, defense, hunting
- `exploration`: Mapping, scouting, exploration
- `utility`: Storage, organization, maintenance
- `transport`: Movement, pathfinding, logistics

## Contributing

When adding new templates:

1. Use descriptive names and IDs
2. Include comprehensive documentation
3. Provide sensible default values
4. Handle edge cases and errors
5. Add telemetry for monitoring
6. Test with various parameter combinations

## Version History

- **1.0.0** (2026-03-01): Initial template library with 6 core templates
