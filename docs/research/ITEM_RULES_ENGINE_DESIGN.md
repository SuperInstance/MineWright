# Item Rules Engine Design

**Design Date:** 2026-03-01
**Status:** Design Document
**Inspired By:** Diablo 2/3 Pickit System, Koolo NIP Files
**Context:** Steve AI - "Cursor for Minecraft" autonomous agent system

---

## 1. Overview

### 1.1 Purpose

The Item Rules Engine is a declarative filtering system that allows AI agents to make intelligent decisions about items in Minecraft without LLM involvement. Inspired by Diablo's pickit system, it uses rule-based evaluation to determine whether to pick up, keep, discard, store, or sell items.

### 1.2 Key Benefits

- **Zero LLM Token Usage:** Item decisions made locally via rules
- **Declarative Configuration:** Rules defined in config files, not code
- **Player Control:** Users customize what agents consider valuable
- **Fast Execution:** Rule evaluation in microseconds per item
- **Extensible:** Easy to add new conditions and actions

### 1.3 Use Cases

1. **GatherAction Filtering:** Decide which dropped items to pick up
2. **Inventory Management:** Decide which items to keep vs discard
3. **Storage Organization:** Automatically sort items into chests
4. **Trading Decisions:** Identify items to sell or trade
5. **Equipment Selection:** Choose best gear to equip

---

## 2. Rule Syntax

### 2.1 Basic Rule Format

Rules follow a simple IF-THEN structure:

```toml
[[item_rules]]
# Conditions (all must match)
type = "diamond_ore"
action = "keep"
priority = "high"

[[item_rules]]
type = "dirt"
action = "discard"

[[item_rules]]
type = "iron_pickaxe"
durability_percent = 50
action = "keep"
priority = "normal"
```

### 2.2 Advanced Rule Format

Rules support compound conditions:

```toml
[[item_rules]]
# Keep damaged tools for repair
type = "diamond_pickaxe"
durability_percent = 60
enchantment_has = ["mending", "unbreaking"]
action = "keep"
priority = "high"

[[item_rules]]
# Discard worn tools without enchantments
type = "diamond_pickaxe"
durability_percent = 30
enchantment_count = 0
action = "discard"
```

### 2.3 Wildcard and Range Support

```toml
[[item_rules]]
# All valuable ores
type_match = "*_ore"
exclude_types = ["dirt", "grass"]
action = "keep"
priority = "high"

[[item_rules]]
# Keep stacks below threshold
type = "oak_log"
stack_size = "< 64"
action = "keep"

[[item_rules]]
# Discard when at max stack
type = "oak_log"
stack_size = ">= 64"
action = "discard"
```

---

## 3. Rule Components

### 3.1 Conditions

| Condition | Type | Description | Examples |
|-----------|------|-------------|----------|
| `type` | string | Exact item/block type match | `"diamond_ore"`, `"oak_log"` |
| `type_match` | string | Wildcard pattern match | `"*_ore"`, `"diamond_*"` |
| `exclude_types` | array | Items to exclude | `["dirt", "cobblestone"]` |
| `stack_size` | string | Stack count comparison | `"< 10"`, `">= 64"` |
| `quantity` | string | Total owned count | `"< 100"`, `"> 500"` |
| `durability_percent` | number | Minimum durability % | `50`, `75` |
| `enchantment_has` | array | Must have these enchantments | `["mending", "efficiency"]` |
| `enchantment_count` | number | Minimum enchantment count | `1`, `2` |
| `enchantment_level` | string | Specific enchantment level | `"efficiency >= 3"` |
| `tool_tier` | string | Tool tier required | `"diamond"`, `"netherite"` |
| `rarity` | string | Item rarity (if applicable) | `"rare"`, `"epic"` |
| `name_match` | string | Custom item name pattern | `"*of*Speed*"` |
| `nbt_tag` | string | NBT data tag presence/absence | `"display.Name"` |
| `custom_id` | string | Modded item ID | `"mymod:special_item"` |
| `biome` | string | Only in specific biome | `"minecraft:plains"` |
| `dimension` | string | Only in specific dimension | `"minecraft:overworld"` |
| `time_of_day` | string | Day/night restrictions | `"day"`, `"night"` |
| `game_stage` | string | Game progress stage | `"early"`, `"mid"`, `"late"` |

### 3.2 Actions

| Action | Description | Implementation Notes |
|--------|-------------|---------------------|
| `keep` | Pick up and keep | Default priority: normal |
| `keep_high` | Keep with high priority | For valuable items |
| `keep_low` | Keep with low priority | May be dropped first |
| `discard` | Don't pick up / drop | For garbage items |
| `store` | Move to storage chest | Requires chest access |
| `equip` | Equip as gear | For armor/tools |
| `sell` | Prepare for trading | For villager trading |
| `smelt` | Add to furnace queue | Auto-smelting |
| `craft` | Queue for crafting | Auto-crafting |

### 3.3 Priorities

| Priority | Value | Use Case |
|----------|-------|----------|
| `critical` | 100 | Never drop (diamonds, nether stars) |
| `high` | 75 | Valuable items (enchanted gear) |
| `normal` | 50 | Standard items (common resources) |
| `low` | 25 | May drop if inventory full |
| `garbage` | 0 | Drop first when needed |

---

## 4. Rule Parser Design

### 4.1 Configuration File Format

Rules are stored in `config/item-rules.toml`:

```toml
# Item Rules Configuration
# Rules are evaluated in order, first match wins

#####################################################################
# PREDEFINED RULE SETS
#####################################################################

[rule_sets]
    # Predefined rule sets for quick switching
    default = ["mining", "building", "survival"]
    early_game = ["essentials_only", "basic_tools"]
    late_game = ["everything_valuable", "enchanted_gear"]

#####################################################################
# ORES AND MINERALS
#####################################################################

[[rule_sets.mining]]
    name = "Mining Operations"
    description = "Rules for automated mining operations"

    [[rule_sets.mining.rules]]
        type = "diamond_ore"
        action = "keep"
        priority = "critical"

    [[rule_sets.mining.rules]]
        type = "iron_ore"
        action = "keep"
        priority = "high"

    [[rule_sets.mining.rules]]
        type = "coal_ore"
        action = "keep"
        priority = "normal"

    [[rule_sets.mining.rules]]
        type = "dirt"
        action = "discard"

#####################################################################
# TOOLS AND EQUIPMENT
#####################################################################

[[rule_sets.tools]]
    name = "Tool Management"
    description = "Rules for tool pickup and equipment"

    [[rule_sets.tools.rules]]
        type = "diamond_pickaxe"
        durability_percent = 50
        enchantment_has = ["mending", "unbreaking"]
        action = "equip"
        priority = "high"

    [[rule_sets.tools.rules]]
        type = "diamond_pickaxe"
        durability_percent = 30
        enchantment_count = 0
        action = "discard"

    [[rule_sets.tools.rules]]
        type = "iron_pickaxe"
        action = "keep"
        priority = "normal"

#####################################################################
# BUILDING MATERIALS
#####################################################################

[[rule_sets.building]]
    name = "Building Materials"
    description = "Rules for construction projects"

    [[rule_sets.building.rules]]
        type_match = "*_planks"
        stack_size = "< 64"
        action = "keep"
        priority = "normal"

    [[rule_sets.building.rules]]
        type_match = "*_log"
        action = "keep"
        priority = "normal"

    [[rule_sets.building.rules]]
        type = "cobblestone"
        quantity = "< 500"
        action = "keep"
        priority = "low"

    [[rule_sets.building.rules]]
        type = "cobblestone"
        quantity = ">= 500"
        action = "discard"

#####################################################################
# SURVIVAL ESSENTIALS
#####################################################################

[[rule_sets.survival]]
    name = "Survival Essentials"
    description = "Critical survival items"

    [[rule_sets.survival.rules]]
        type = "oak_log"
        action = "keep"
        priority = "normal"

    [[rule_sets.survival.rules]]
        type = "wheat"
        action = "keep"
        priority = "normal"

    [[rule_sets.survival.rules]]
        type = "apple"
        action = "keep"
        priority = "high"

    [[rule_sets.survival.rules]]
        type_match = "*_sword"
        action = "keep"
        priority = "high"
```

### 4.2 Parser Implementation

```java
package com.minewright.rules;

import com.moandjiezana.toml.Toml;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Parses and evaluates item rules from configuration.
 */
public class ItemRuleParser {
    private final List<ItemRule> rules;
    private final Map<String, List<ItemRule>> ruleSets;

    public ItemRuleParser(File configFile) {
        Toml config = new Toml().read(configFile);
        this.rules = loadRules(config);
        this.ruleSets = loadRuleSets(config);
    }

    /**
     * Evaluates an item stack against all rules.
     * Returns the first matching rule's action.
     */
    public ItemRuleResult evaluate(ItemStack stack, ItemContext context) {
        for (ItemRule rule : rules) {
            if (rule.matches(stack, context)) {
                return new ItemRuleResult(rule.getAction(), rule.getPriority());
            }
        }
        // Default: discard unknown items
        return new ItemRuleResult(ItemAction.DISCARD, ItemPriority.NORMAL);
    }

    private List<ItemRule> loadRules(Toml config) {
        List<ItemRule> loadedRules = new ArrayList<>();

        List<Toml> ruleEntries = config.getTables("item_rules");
        for (Toml entry : ruleEntries) {
            ItemRule rule = parseRule(entry);
            if (rule != null) {
                loadedRules.add(rule);
            }
        }

        return loadedRules;
    }

    private ItemRule parseRule(Toml entry) {
        ItemRule.Builder builder = ItemRule.builder();

        // Parse conditions
        if (entry.contains("type")) {
            builder.type(entry.getString("type"));
        }
        if (entry.contains("type_match")) {
            builder.typePattern(entry.getString("type_match"));
        }
        if (entry.contains("exclude_types")) {
            builder.excludeTypes(entry.getList(String.class, "exclude_types"));
        }
        if (entry.contains("stack_size")) {
            builder.stackCondition(entry.getString("stack_size"));
        }
        if (entry.contains("durability_percent")) {
            builder.minDurability(entry.getLong("durability_percent").intValue());
        }
        if (entry.contains("enchantment_has")) {
            builder.enchantments(entry.getList(String.class, "enchantment_has"));
        }
        if (entry.contains("enchantment_count")) {
            builder.minEnchantments(entry.getLong("enchantment_count").intValue());
        }

        // Parse action
        String actionStr = entry.getString("action", "keep");
        ItemAction action = ItemAction.valueOf(actionStr.toUpperCase());
        builder.action(action);

        // Parse priority
        String priorityStr = entry.getString("priority", "normal");
        ItemPriority priority = ItemPriority.valueOf(priorityStr.toUpperCase());
        builder.priority(priority);

        return builder.build();
    }
}
```

---

## 5. Rule Engine Architecture

### 5.1 Class Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        ItemRuleEngine                           │
│                                                                 │
│   + evaluate(ItemStack, ItemContext): ItemRuleResult           │
│   + loadRules(File): void                                      │
│   + reloadRules(): void                                        │
│   - rules: List<ItemRule>                                      │
│   - ruleSets: Map<String, List<ItemRule>>                      │
│   - parser: ItemRuleParser                                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ uses
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          ItemRule                               │
│                                                                 │
│   + matches(ItemStack, ItemContext): boolean                   │
│   + getAction(): ItemAction                                    │
│   + getPriority(): ItemPriority                                │
│   - conditions: List<RuleCondition>                            │
│   - action: ItemAction                                         │
│   - priority: ItemPriority                                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ contains
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       RuleCondition                            │
│                                                                 │
│   + evaluate(ItemStack, ItemContext): boolean                  │
│   + getType(): ConditionType                                   │
│                                                                 │
│   ┌───────────────────────────────────────────────────────────┐ │
│   │                   ConditionType                          │ │
│   │                                                           │ │
│   │   TYPE_MATCH, STACK_CONDITION, DURABILITY,                │ │
│   │   ENCHANTMENT, TOOL_TIER, RARITY, NBT_TAG,                │ │
│   │   BIOME, DIMENSION, TIME_OF_DAY                           │ │
│   └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 Core Classes

```java
/**
 * Result of rule evaluation.
 */
public class ItemRuleResult {
    private final ItemAction action;
    private final ItemPriority priority;
    private final String reason;

    public ItemRuleResult(ItemAction action, ItemPriority priority, String reason) {
        this.action = action;
        this.priority = priority;
        this.reason = reason;
    }

    public boolean shouldPickUp() {
        return action == ItemAction.KEEP ||
               action == ItemAction.KEEP_HIGH ||
               action == ItemAction.KEEP_LOW;
    }

    // Getters...
}

/**
 * Action to take for an item.
 */
public enum ItemAction {
    KEEP,           // Pick up and keep (normal priority)
    KEEP_HIGH,      // Pick up and keep (high priority)
    KEEP_LOW,       // Pick up and keep (low priority)
    DISCARD,        // Don't pick up / drop
    STORE,          // Move to storage chest
    EQUIP,          // Equip as gear
    SELL,           // Prepare for trading
    SMELT,          // Add to furnace queue
    CRAFT           // Queue for crafting
}

/**
 * Priority level for items.
 */
public enum ItemPriority {
    CRITICAL(100),  // Never drop
    HIGH(75),       // Valuable items
    NORMAL(50),     // Standard items
    LOW(25),        // May drop first
    GARBAGE(0);     // Drop immediately

    private final int value;

    ItemPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

/**
 * Context information for rule evaluation.
 */
public class ItemContext {
    private final String biome;
    private final String dimension;
    private final long gameTime;
    private final int totalOwned;
    private final Map<String, Integer> inventoryCounts;

    public ItemContext(String biome, String dimension, long gameTime,
                      int totalOwned, Map<String, Integer> inventoryCounts) {
        this.biome = biome;
        this.dimension = dimension;
        this.gameTime = gameTime;
        this.totalOwned = totalOwned;
        this.inventoryCounts = inventoryCounts;
    }

    public boolean isDay() {
        return (gameTime % 24000) < 12000;
    }

    public boolean isNight() {
        return !isDay();
    }

    // Getters...
}
```

---

## 6. Example Rules for Minecraft

### 6.1 Mining Bot Rules

```toml
[[rule_sets.mining_bot]]
    name = "Automated Mining Bot"

    # Keep all valuable ores
    [[rule_sets.mining_bot.rules]]
        type_match = "*_ore"
        exclude_types = ["coal_ore", "copper_ore"]
        action = "keep"
        priority = "critical"

    # Keep coal but with lower priority
    [[rule_sets.mining_bot.rules]]
        type = "coal_ore"
        action = "keep"
        priority = "low"

    # Always discard stone/cobble when mining
    [[rule_sets.mining_bot.rules]]
        type_match = ["stone", "cobblestone"]
        action = "discard"

    # Keep torches for lighting
    [[rule_sets.mining_bot.rules]]
        type = "torch"
        stack_size = "< 64"
        action = "keep"
        priority = "normal"
```

### 6.2 Building Bot Rules

```toml
[[rule_sets.building_bot]]
    name = "Construction Assistant"

    # Keep all building materials
    [[rule_sets.building_bot.rules]]
        type_match = ["*_planks", "*_log", "cobblestone", "stone"]
        action = "keep"
        priority = "normal"

    # Store excess materials in chests
    [[rule_sets.building_bot.rules]]
        type_match = ["*_planks", "cobblestone"]
        stack_size = ">= 32"
        action = "store"
        priority = "normal"

    # Keep tools for construction
    [[rule_sets.building_bot.rules]]
        type = "diamond_pickaxe"
        action = "keep"
        priority = "high"

    [[rule_sets.building_bot.rules]]
        type = "diamond_axe"
        action = "keep"
        priority = "high"
```

### 6.3 Survival Scavenger Rules

```toml
[[rule_sets.survival_scavenger]]
    name = "Survival Item Collector"

    # Food is critical
    [[rule_sets.survival_scavenger.rules]]
        type_match = ["*_bread", "*_berry", "apple", "cooked_*"]
        action = "keep"
        priority = "high"

    # Armor is valuable
    [[rule_sets.survival_scavenger.rules]]
        type_match = ["diamond_*", "iron_*"]
        enchantment_has = ["protection", "unbreaking"]
        action = "equip"
        priority = "high"

    # Weapons for defense
    [[rule_sets.survival_scavenger.rules]]
        type_match = "*_sword"
        enchantment_has = ["sharpness", "fire_aspect"]
        action = "equip"
        priority = "critical"

    # Discard common garbage
    [[rule_sets.survival_scavenger.rules]]
        type = ["dirt", "grass", "sand"]
        action = "discard"
```

### 6.4 Enchantment Hunter Rules

```toml
[[rule_sets.enchantment_hunter]]
    name = "Enchanted Gear Collector"

    # Keep any enchanted item
    [[rule_sets.enchantment_hunter.rules]]
        enchantment_count = 1
        enchantment_has = ["mending", "unbreaking", "efficiency",
                         "sharpness", "protection", "fortune"]
        action = "keep"
        priority = "high"

    # High-level enchantments
    [[rule_sets.enchantment_hunter.rules]]
        enchantment_level = "efficiency >= 4"
        action = "keep"
        priority = "critical"

    [[rule_sets.enchantment_hunter.rules]]
        enchantment_level = "fortune >= 3"
        action = "keep"
        priority = "critical"

    # Discard unenchanted diamond gear (already have better)
    [[rule_sets.enchantment_hunter.rules]]
        type_match = "diamond_*"
        enchantment_count = 0
        action = "discard"
```

---

## 7. Integration with Actions

### 7.1 GatherAction Integration

```java
public class GatherResourceAction extends BaseAction {
    private ItemRuleEngine ruleEngine;

    @Override
    protected void onStart() {
        // Initialize rule engine
        ruleEngine = new ItemRuleEngine(new File("config/item-rules.toml"));

        // ... existing code ...
    }

    /**
     * Checks if an item should be picked up based on rules.
     */
    private boolean shouldPickUp(ItemStack droppedItem, BlockPos pos) {
        // Build evaluation context
        ItemContext context = new ItemContext(
            foreman.level().getBiome(pos).toString(),
            foreman.level().dimension().toString(),
            foreman.level().getDayTime(),
            getTotalItemCount(droppedItem.getItem()),
            getInventoryCounts()
        );

        // Evaluate against rules
        ItemRuleResult result = ruleEngine.evaluate(droppedItem, context);

        LOGGER.debug("[{}] Item rule evaluation for {}: {} (priority: {})",
            foreman.getEntityName(),
            droppedItem.getDescriptionId(),
            result.getAction(),
            result.getPriority()
        );

        return result.shouldPickUp();
    }

    /**
     * Called when a block is destroyed to check if drops should be collected.
     */
    private void onBlockDropped(ItemStack drop, BlockPos pos) {
        if (shouldPickUp(drop, pos)) {
            // Navigate to item and pick it up
            currentTarget = pos;
        } else {
            LOGGER.debug("[{}] Skipping item based on rules: {}",
                foreman.getEntityName(), drop.getDescriptionId());
        }
    }
}
```

### 7.2 Inventory Management Integration

```java
/**
 * Action for organizing inventory based on item rules.
 */
public class OrganizeInventoryAction extends BaseAction {
    private ItemRuleEngine ruleEngine;

    @Override
    protected void onStart() {
        ruleEngine = new ItemRuleEngine(new File("config/item-rules.toml"));

        // Sort inventory by priority
        sortByPriority();

        // Move items to storage based on rules
        processStorageRules();

        // Drop garbage items
        processDiscardRules();

        succeed("Inventory organized according to rules");
    }

    private void sortByPriority() {
        List<ItemStack> items = getInventoryItems();
        ItemContext context = buildContext();

        // Sort by rule priority (highest first)
        items.sort((a, b) -> {
            ItemRuleResult resultA = ruleEngine.evaluate(a, context);
            ItemRuleResult resultB = ruleEngine.evaluate(b, context);
            return Integer.compare(
                resultB.getPriority().getValue(),
                resultA.getPriority().getValue()
            );
        });

        // Reorganize inventory based on sorted order
        reorganizeInventory(items);
    }

    private void processStorageRules() {
        ItemContext context = buildContext();

        for (ItemStack stack : getInventoryItems()) {
            ItemRuleResult result = ruleEngine.evaluate(stack, context);

            if (result.getAction() == ItemAction.STORE) {
                moveItemToChest(stack, getNearestStorageChest());
            }
        }
    }

    private void processDiscardRules() {
        ItemContext context = buildContext();

        for (ItemStack stack : getInventoryItems()) {
            ItemRuleResult result = ruleEngine.evaluate(stack, context);

            if (result.getAction() == ItemAction.DISCARD ||
                result.getAction() == ItemAction.GARBAGE) {
                dropItem(stack);
            }
        }
    }
}
```

### 7.3 Task Planner Integration

```java
public class TaskPlanner {
    /**
     * Generates gather tasks based on item rules.
     * When LLM says "gather resources", check rules for what to gather.
     */
    public List<Task> planGatherTasks(ForemanEntity foreman, String target) {
        List<Task> tasks = new ArrayList<>();
        ItemRuleEngine ruleEngine = foreman.getItemRuleEngine();
        ItemContext context = buildContext(foreman);

        // Scan nearby area for items
        for (BlockPos pos : getNearbyPositions(foreman, 32)) {
            ItemStack potentialItem = getItemAtPosition(pos);

            if (potentialItem != null) {
                ItemRuleResult result = ruleEngine.evaluate(potentialItem, context);

                if (result.shouldPickUp() &&
                    result.getPriority().getValue() >= ItemPriority.NORMAL.getValue()) {

                    Task task = new Task("gather", Map.of(
                        "resource", potentialItem.getDescriptionId(),
                        "position", pos.toString(),
                        "priority", result.getPriority().toString()
                    ));
                    tasks.add(task);
                }
            }
        }

        // Sort by priority
        tasks.sort((a, b) -> {
            ItemPriority priorityA = ItemPriority.valueOf(
                a.getStringParameter("priority", "NORMAL"));
            ItemPriority priorityB = ItemPriority.valueOf(
                b.getStringParameter("priority", "NORMAL"));
            return priorityB.getValue() - priorityA.getValue();
        });

        return tasks;
    }
}
```

---

## 8. Configuration File Examples

### 8.1 Complete Example Configuration

```toml
# config/item-rules.toml
# Complete item rules configuration for Steve AI

version = "1.0"
description = "Default item filtering rules"

#####################################################################
# GLOBAL SETTINGS
#####################################################################
[settings]
    # Default action when no rules match
    default_action = "discard"

    # Enable rule logging
    enable_logging = true

    # Reload rules when file changes
    auto_reload = true

#####################################################################
# RULE SETS
#####################################################################
[rule_sets]
    # Active rule sets (in order of evaluation)
    active = ["essentials", "mining", "building", "combat", "garbage"]

#####################################################################
# ESSENTIALS (Always evaluate first)
#####################################################################
[[rule_sets.essentials]]
    name = "Essential Items"
    priority = 1

    # Never lose these items
    [[rule_sets.essentials.rules]]
        type = ["diamond", "nether_star", "elytra",
                "totem_of_undying", "enchanted_golden_apple"]
        action = "keep"
        priority = "critical"

    # Keep keys and special items
    [[rule_sets.essentials.rules]]
        type_match = "*_key"
        action = "keep"
        priority = "critical"

#####################################################################
# MINING RULES
#####################################################################
[[rule_sets.mining]]
    name = "Mining Operations"
    priority = 2

    # Valuable ores
    [[rule_sets.mining.rules]]
        type = ["diamond_ore", "ancient_debris", "deepslate_diamond_ore"]
        action = "keep"
        priority = "critical"

    [[rule_sets.mining.rules]]
        type = ["iron_ore", "gold_ore", "copper_ore", "redstone_ore"]
        action = "keep"
        priority = "high"

    # Coal is low priority
    [[rule_sets.mining.rules]]
        type = ["coal_ore", "deepslate_coal_ore"]
        action = "keep"
        priority = "low"

    # Discard common stone when full
    [[rule_sets.mining.rules]]
        type = ["cobblestone", "stone", "dirt", "grass"]
        quantity = ">= 1000"
        action = "discard"

#####################################################################
# BUILDING RULES
#####################################################################
[[rule_sets.building]]
    name = "Building Materials"
    priority = 3

    # Always keep wood
    [[rule_sets.building.rules]]
        type_match = "*_log"
        action = "keep"
        priority = "normal"

    # Keep planks but store when full
    [[rule_sets.building.rules]]
        type_match = "*_planks"
        stack_size = ">= 32"
        action = "store"
        priority = "normal"

    # Building blocks
    [[rule_sets.building.rules]]
        type = ["cobblestone", "stone", "dirt", "sand", "gravel"]
        quantity = "< 500"
        action = "keep"
        priority = "low"

    # Redstone components
    [[rule_sets.building.rules]]
        type_match = ["redstone", "repeater", "comparator", "piston"]
        action = "keep"
        priority = "normal"

#####################################################################
# COMBAT RULES
#####################################################################
[[rule_sets.combat]]
    name = "Combat Equipment"
    priority = 4

    # Enchanted weapons
    [[rule_sets.combat.rules]]
        type_match = "*_sword"
        enchantment_has = ["sharpness", "smite", "fire_aspect"]
        action = "equip"
        priority = "high"

    # Armor with protection
    [[rule_sets.combat.rules]]
        type_match = ["*_helmet", "*_chestplate", "*_leggings", "*_boots"]
        enchantment_has = ["protection", "unbreaking"]
        action = "equip"
        priority = "high"

    # Bows and arrows
    [[rule_sets.combat.rules]]
        type = ["bow", "crossbow", "arrow", "spectral_arrow", "tipped_arrow"]
        action = "keep"
        priority = "normal"

    # Shields
    [[rule_sets.combat.rules]]
        type = "shield"
        action = "keep"
        priority = "normal"

#####################################################################
# GARBAGE (Always evaluate last)
#####################################################################
[[rule_sets.garbage]]
    name = "Garbage Items"
    priority = 100

    # Definitely discard
    [[rule_sets.garbage.rules]]
        type = ["dirt", "grass", "sand", "gravel"]
        quantity = ">= 500"
        action = "discard"

    # Unenchanted basic tools
    [[rule_sets.garbage.rules]]
        type_match = ["wooden_*", "stone_*"]
        enchantment_count = 0
        action = "discard"

    # Rotten food
    [[rule_sets.garbage.rules]]
        type = ["rotten_flesh", "spider_eye", "poisonous_potato"]
        action = "discard"
```

### 8.2 Early Game Configuration

```toml
# config/item-rules-early-game.toml
# Conservative rules for early survival

[[rule_sets.early_game]]
    # Only keep essentials
    [[rule_sets.early_game.rules]]
        type = ["oak_log", "cobblestone", "coal", "iron_ore"]
        action = "keep"
        priority = "normal"

    [[rule_sets.early_game.rules]]
        type = ["wheat", "seeds", "apple", "carrot", "potato"]
        action = "keep"
        priority = "high"

    # Discard everything else
    [[rule_sets.early_game.rules]]
        action = "discard"
```

### 8.3 Late Game Configuration

```toml
# config/item-rules-late-game.toml
# Aggressive collection for established players

[[rule_sets.late_game]]
    # Keep almost everything valuable
    [[rule_sets.late_game.rules]]
        exclude_types = ["dirt", "grass", "stone", "cobblestone"]
        action = "keep"
        priority = "normal"

    # Keep enchanted items regardless of type
    [[rule_sets.late_game.rules]]
        enchantment_count = 1
        action = "keep"
        priority = "high"

    # Keep rare items
    [[rule_sets.late_game.rules]]
        type = ["diamond", "netherite", "gold", "emerald"]
        action = "keep"
        priority = "critical"
```

---

## 9. Implementation Roadmap

### Phase 1: Core Engine (Week 1-2)
- [ ] `ItemRule` class with conditions
- [ ] `ItemRuleParser` for TOML parsing
- [ ] `ItemRuleEngine` for evaluation
- [ ] `ItemContext` for context data
- [ ] Basic condition types (type, stack_size, durability)

### Phase 2: Advanced Conditions (Week 3)
- [ ] Enchantment conditions
- [ ] Tool tier conditions
- [ ] NBT tag conditions
- [ ] Wildcard pattern matching
- [ ] Range comparisons (<, >, <=, >=)

### Phase 3: Action Integration (Week 4)
- [ ] GatherAction integration
- [ ] Inventory sorting action
- [ ] Storage management action
- [ ] Equipment selection action

### Phase 4: Configuration (Week 5)
- [ ] Default rule sets
- [ ] Early/mid/late game profiles
- [ ] Per-agent rule profiles
- [ ] Dynamic rule reloading

### Phase 5: Testing (Week 6)
- [ ] Unit tests for rule matching
- [ ] Integration tests with actions
- [ ] Performance benchmarks
- [ ] Configuration validation

---

## 10. Performance Considerations

### 10.1 Optimization Strategies

1. **Rule Caching:** Compile rules to bytecode on load
2. **Short-Circuit Evaluation:** Stop checking after first match
3. **Index by Type:** Hash rules by item type for fast lookup
4. **Lazy Evaluation:** Only evaluate expensive conditions when needed
5. **Batch Processing:** Evaluate multiple items in single pass

### 10.2 Expected Performance

- **Rule evaluation:** < 10 microseconds per item
- **Startup time:** < 100ms to load 1000 rules
- **Memory usage:** ~1KB per rule (compiled)

---

## 11. Comparison with Diablo Pickit

| Feature | Diablo Pickit | Steve AI Item Rules |
|---------|---------------|---------------------|
| **Rule Format** | NIP files | TOML config |
| **Syntax** | `[Name] == ColossusBlade # [KEEP]` | `type = "diamond_ore" action = "keep"` |
| **Conditions** | Item stats | Item stats + context |
| **Actions** | Keep/Sell | Keep/Store/Discard/Equip/More |
| **Priorities** | Implicit order | Explicit priority levels |
| **Context** | None | Biome, dimension, inventory |
| **Mod Support** | No | Yes (modded items) |

---

## 12. References

- **Diablo 2 Pickit:** Koolo bot NIP file format
- **TinTin++:** MUD client trigger system
- **Minecraft Forge:** Item and block APIs
- **Toml:** Configuration file format

---

**Document Version:** 1.0
**Last Updated:** 2026-03-01
**Author:** Design Document for Steve AI Project
**Status:** Ready for Implementation
