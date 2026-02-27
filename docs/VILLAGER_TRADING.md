# Villager Trading AI for MineWright

## Overview

This document describes the design and implementation of a villager trading AI system for the MineWright Minecraft mod (Forge 1.20.1). The system enables Foreman entities to automatically detect villagers, analyze trade opportunities, optimize deals, manage reputation, and handle emerald economics.

## Table of Contents

1. [Architecture](#architecture)
2. [Villager Profession Detection](#villager-profession-detection)
3. [Trade Analysis Algorithm](#trade-analysis-algorithm)
4. [Best Deal Finding](#best-deal-finding)
5. [Emerald Economy Management](#emerald-economy-management)
6. [Reputation Management](#reputation-management)
7. [Restock Timing Awareness](#restock-timing-awareness)
8. [Code Examples](#code-examples)
9. [Implementation Roadmap](#implementation-roadmap)

---

## Architecture

### Component Overview

```
com.minewright.villager/
├── VillagerTradeAction.java       # Main action for trading
├── VillagerTradeManager.java      # High-level trade orchestration
├── VillagerAnalyzer.java          # Profession & trade detection
├── TradeOptimizer.java            # Find best deals
├── EmeraldEconomy.java            # Emerald budgeting & accounting
├── ReputationTracker.java         # Reputation state management
├── RestockMonitor.java            # Track restock timing
└── TradeOffer.java                # Data class for trade info
```

### Integration Points

- **Action System**: Extends `BaseAction` like other actions
- **Memory System**: Stores villager locations and trade history in `ForemanMemory`
- **WorldKnowledge**: Extended to detect nearby villagers
- **PromptBuilder**: Updated with "trade" action for LLM awareness

---

## Villager Profession Detection

### Detection Strategy

Minecraft villagers have 15 professions (plus unemployed and nitwit). Detection uses `Villager` entity inspection:

```java
// Key Minecraft 1.20.1 APIs
villager.getVillagerData().getProfession()  // Profession enum
villager.getVillagerData().getLevel()        // 1-5 (Novice to Master)
villager.getOffers()                         // MerchantOffers container
```

### Profession Mapping

| Profession | Trades | Economic Value |
|------------|--------|----------------|
| Farmer     | Crops, food | High (emeralds for crops) |
| Librarian  | Enchanted books | Very High (Mending) |
| Cleric     | Potions, rotten flesh | Medium (redstone, glowstone) |
| Armorer    | Armor, chainmail | High (diamond gear trades) |
| Weaponsmith| Weapons, bells | High (diamond weapons) |
| Toolsmith  | Tools, bells | Medium (diamond tools) |
| Butcher    | Food, rabbit foot | Low-Medium |
| Leatherworker| Leather, horse armor | Low |
| Shepherd   | Wool, banners | Low |
| Fisherman  | Fish, fishing rods | Low-Medium |
| Fletcher   | Arrows, bows, flint | Medium (flint for emeralds) |
| Mason      | Stone, quartz | Medium |
| Cartographer| Maps, banners | Medium (glass panes for emeralds) |
| Smith      | Tools, armor | Medium |
| Nitwit     | None | None |

### Detection Code Pattern

```java
public class VillagerAnalyzer {

    public static Optional<VillagerInfo> analyze(Villager villager) {
        VillagerData data = villager.getVillagerData();
        Profession profession = data.getProfession();

        if (profession == Profession.NONE || profession == Profession.NITWIT) {
            return Optional.empty();
        }

        VillagerInfo info = new VillagerInfo(
            villager.getUUID(),
            profession,
            data.getLevel(),
            extractOffers(villager),
            villager.blockPosition()
        );

        return Optional.of(info);
    }

    private static List<TradeOffer> extractOffers(Villager villager) {
        MerchantOffers offers = villager.getOffers();
        List<TradeOffer> result = new ArrayList<>();

        for (MerchantOffer offer : offers) {
            result.add(TradeOffer.fromMinecraft(offer));
        }

        return result;
    }
}
```

---

## Trade Analysis Algorithm

### Scoring Formula

Each trade is scored based on:

```
TradeScore = (ValueScore * WeightMultiplier) / (Cost + ReputationPenalty)
```

**Components:**

1. **ValueScore**: Intrinsic value of output item (1-100)
   - Enchanted books: 80-100
   - Diamond gear: 70-90
   - Iron/gold ingots: 50-60
   - Redstone/glowstone: 40-50
   - Building materials: 10-30
   - Food: 5-20

2. **WeightMultiplier**: Based on quantity
   - Single rare items: 1.5x
   - Stackable resources: 1.0x
   - Bulk (>16): 1.2x

3. **Cost**: Emeralds or resource cost (normalized)
   - 1 emerald = 10 cost units
   - Resources valued at market rates

4. **ReputationPenalty**: Extra cost due to low reputation
   - Hero of the Village: 0.8x (discount!)
   - Neutral: 1.0x
   - Low reputation: 1.3x (price hiking)

### Algorithm Implementation

```java
public class TradeOptimizer {

    public List<TradeRecommendation> analyzeTrades(VillagerInfo villager,
            Inventory playerInventory, EmeraldBudget budget) {

        List<TradeRecommendation> recommendations = new ArrayList<>();

        for (TradeOffer offer : villager.getOffers()) {
            if (!canAfford(offer, playerInventory, budget)) {
                continue;
            }

            double score = calculateTradeScore(offer, villager);
            int maxTrades = calculateMaxTrades(offer, playerInventory, budget);

            recommendations.add(new TradeRecommendation(
                offer,
                score,
                maxTrades,
                calculateROI(offer)
            ));
        }

        // Sort by score descending
        recommendations.sort(Comparator.comparingDouble(
            TradeRecommendation::getScore).reversed());

        return recommendations;
    }

    private double calculateTradeScore(TradeOffer offer, VillagerInfo villager) {
        double valueScore = ItemValueDatabase.getValue(offer.getOutputItem());
        double weightMultiplier = calculateWeightMultiplier(offer);
        double cost = normalizeCost(offer);
        double reputationPenalty = ReputationTracker.getPenalty(villager);

        return (valueScore * weightMultiplier) / (cost * reputationPenalty);
    }

    private int calculateMaxTrades(TradeOffer offer, Inventory inv, EmeraldBudget budget) {
        int emeraldCost = offer.getEmeraldCost();
        int resourceLimit = Integer.MAX_VALUE;

        // Check resource costs (e.g., 32 wheat for 1 emerald)
        for (Map.Entry<Item, Integer> cost : offer.getResourceCosts().entrySet()) {
            int available = countItem(inv, cost.getKey());
            int possible = available / cost.getValue();
            resourceLimit = Math.min(resourceLimit, possible);
        }

        // Check emerald budget
        int emeraldLimit = budget.canSpend(emeraldCost) ?
            budget.getRemaining() / emeraldCost : 0;

        return Math.min(resourceLimit, emeraldLimit);
    }
}
```

---

## Best Deal Finding

### Strategy Hierarchy

Trades are prioritized by ROI (Return on Investment):

**Tier 1: Essential Enchantments**
- Mending books (1 emerald + 1 book at Master Librarian)
- Unbreaking III
- Efficiency IV/V
- Fortune III

**Tier 2: Resource Arbitrage**
- Crops/rotten flesh for emeralds (farmer/cleric - profit!)
- Iron/gold for emeralds (armorer/weaponsmith - sometimes profitable)
- Flint for emeralds (fletcher - profit!)

**Tier 3: Gear Acquisition**
- Diamond armor/tools at master level
- Saddle, bell, lantern

**Tier 4: Bulk Resources**
- Glass panes, wool, stone (mason/shepherd - convenience)

### Code Example

```java
public class TradeOptimizer {

    public Optional<TradeOffer> findBestDeal(List<VillagerInfo> villagers,
            TradingGoal goal) {

        List<TradeOffer> allOffers = villagers.stream()
            .flatMap(v -> v.getOffers().stream())
            .collect(Collectors.toList());

        switch (goal.getType()) {
            case SPECIFIC_ITEM:
                return findBestForItem(allOffers, goal.getTargetItem());

            case MAXIMIZE_EMERALDS:
                return findBestEmeraldProfit(allOffers);

            case HIGH_VALUE:
                return findHighestValue(allOffers);

            default:
                return Optional.empty();
        }
    }

    private Optional<TradeOffer> findBestForItem(List<TradeOffer> offers, Item target) {
        return offers.stream()
            .filter(o -> o.getOutputItem().getItem() == target)
            .filter(o -> o.isAffordable())
            .min(Comparator.comparingDouble(TradeOffer::getEffectiveCost));
    }

    private Optional<TradeOffer> findBestEmeraldProfit(List<TradeOffer> offers) {
        // Look for trades that give emeralds for cheap resources
        return offers.stream()
            .filter(o -> o.getOutputItem().getItem() == Items.EMERALD)
            .filter(o -> o.getInputCost() < 8) // Less than 8 items per emerald
            .max(Comparator.comparingDouble(this::calculateProfitMargin));
    }
}
```

---

## Emerald Economy Management

### Budget System

The AI manages emeralds like a currency budget:

```java
public class EmeraldBudget {
    private int totalBudget;
    private int spent;
    private int reserved;
    private Map<TradingGoal, Integer> allocations;

    public boolean canSpend(int amount) {
        return (spent + reserved + amount) <= totalBudget;
    }

    public void recordSpent(int amount) {
        spent += amount;
    }

    public void reserveForGoal(TradingGoal goal, int amount) {
        if (canSpend(amount)) {
            reserved += amount;
            allocations.put(goal, allocations.getOrDefault(goal, 0) + amount);
        }
    }

    public int getRemaining() {
        return totalBudget - spent - reserved;
    }

    public double getUtilization() {
        return (double) spent / totalBudget;
    }
}
```

### Income Sources

1. **Selling to Villagers**: Crops, resources, mob drops
2. **Mining**: Direct emerald ore mining (requires silk touch or fortune)
3. **Raid Rewards**: Hero of the Village discounts
4. **Looting**: Desert temples, shipwrecks, ruins

### Spending Priorities

```
Priority 1: Mending/Unbreaking books (long-term investment)
Priority 2: Diamond gear (combat/mining efficiency)
Priority 3: Resource arbitrage (turn crops into emeralds)
Priority 4: Utility items (saddle, bells, Ender pearls)
Priority 5: Convenience (glass, wool, stone)
```

### Economic Cycle

```
[Emerald Budget]
       |
       v
[Buy Seeds/Crops] -> [Farm] -> [Sell Crops]
                           |
                           v
                    [More Emeralds]
                           |
                           v
                    [Buy Enchanted Gear]
                           |
                           v
                    [Better Mining]
                           |
                           v
                    [Direct Emerald Mining]
```

---

## Reputation Management

### Reputation Mechanics

Minecraft's reputation system affects prices:

- **Hero of the Village**: Discounts (multiplier 0.2-0.6x)
- **Neutral**: Normal prices (1.0x)
- **Low Reputation**: Price hiking (1.1-1.3x)
- **Very Low**: Some trades locked

### Reputation Sources

| Action | Reputation Change |
|--------|-------------------|
| Cure zombie villager | +10 to +20 (major boost) |
| Raid win (Hero) | +2 per villager level |
| Raid loss (Bad Omen) | -1 to -2 (price hike) |
| Normal trading | +0.1 per successful trade |
| Hitting villager | -1 to -5 (major penalty) |

### Management Strategy

```java
public class ReputationTracker {

    private static final Map<UUID, Integer> reputationCache = new ConcurrentHashMap<>();

    public static boolean shouldAvoidVillager(Villager villager) {
        // Access internal Minecraft reputation via reflection
        int reputation = getGossipReputation(villager);
        UUID uuid = villager.getUUID();

        reputationCache.put(uuid, reputation);

        // Avoid if reputation is very low (prices will be terrible)
        return reputation < -5;
    }

    public static double getDiscountMultiplier(Villager villager) {
        // Hero of the Village provides massive discounts
        if (villager.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            int amplifier = villager.getEffect(MobEffects.HERO_OF_THE_VILLAGE).getAmplifier();
            return 1.0 - (0.1 * (amplifier + 1)); // 10% discount per level
        }

        return 1.0; // Normal prices
    }

    public static boolean isGoodTimeToBuy(Villager villager) {
        // Best time: Hero of the Village active
        if (villager.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            return true;
        }

        // Good time: High positive reputation
        int reputation = reputationCache.getOrDefault(villager.getUUID(), 0);
        return reputation >= 5;
    }
}
```

### Gossip System Integration

Villagers share "gossip" about players:

```java
// Gossip types affecting reputation
enum GossipType {
    MAJOR_POSITIVE,  // Cured zombie villager
    MINOR_POSITIVE,  // Successful trading
    MAJOR_NEGATIVE,  // Attacked villager
    MINOR_NEGATIVE,  // Killed villager's golem
    TRADING          // Trading volume
}
```

---

## Restock Timing Awareness

### Restock Mechanics

- Villagers restock twice per Minecraft day
- Workstation blocks required for restocking
- Restock happens at noon and evening
- Master level villagers restock faster

### Detection Strategy

```java
public class RestockMonitor {

    private static final Map<UUID, Long> lastRestockTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> previousStock = new ConcurrentHashMap<>();

    public static boolean hasRestocked(Villager villager) {
        UUID uuid = villager.getUUID();
        int currentStock = countAvailableTrades(villager);

        if (!previousStock.containsKey(uuid)) {
            previousStock.put(uuid, currentStock);
            return false;
        }

        int oldStock = previousStock.get(uuid);
        previousStock.put(uuid, currentStock);

        if (currentStock > oldStock) {
            lastRestockTime.put(uuid, System.currentTimeMillis());
            return true;
        }

        return false;
    }

    public static long getTimeUntilNextRestock(Villager villager) {
        UUID uuid = villager.getUUID();
        Long lastRestock = lastRestockTime.get(uuid);

        if (lastRestock == null) {
            return 0; // Unknown, assume ready
        }

        long elapsed = System.currentTimeMillis() - lastRestock;
        long restockInterval = 10 * 60 * 1000; // ~10 minutes (Minecraft day/2)

        return Math.max(0, restockInterval - elapsed);
    }

    public static boolean isLowOnStock(Villager villager) {
        MerchantOffers offers = villager.getOffers();
        int totalStock = offers.stream()
            .mapToInt(MerchantOffer::getMaxUses)
            .sum();

        return totalStock < 4; // Less than 4 trades available
    }
}
```

### Optimal Trading Schedule

```
Best: Right after restock (full inventory available)
Good: Within 5 minutes of restock
Poor: >8 minutes since restock (running low)
Avoid: When stock depleted (wait for restock)
```

### Workstation Proximity

Villagers must access their workstation to restock:

```java
public class WorkstationChecker {

    public static boolean hasWorkstationAccess(Villager villager) {
        BlockPos workstationPos = findWorkstation(villager);
        if (workstationPos == null) {
            return false;
        }

        double distance = villager.blockPosition().distSqr(workstationPos);
        return distance < 16; // Within 4 blocks
    }

    private static BlockPos findWorkstation(Villager villager) {
        // Villagers claim job site blocks within 16 blocks
        // This requires NBT data inspection
        return VillagerUtils.getJobSite(villager);
    }
}
```

---

## Code Examples

### Main Trade Action

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.villager.*;
import net.minecraft.world.entity.npc.Villager;
import optifine.Random;

public class VillagerTradeAction extends BaseAction {

    private enum Phase {
        SEARCHING,
        APPROACHING,
        ANALYZING,
        TRADING,
        COMPLETE
    }

    private Phase phase = Phase.SEARCHING;
    private Villager targetVillager;
    private List<TradeRecommendation> recommendations;
    private int currentTradeIndex = 0;
    private int ticksWaited = 0;

    public VillagerTradeAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        sendChatMessage("Looking for villagers to trade with...");
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case SEARCHING -> findVillager();
            case APPROACHING -> approachVillager();
            case ANALYZING -> analyzeTrades();
            case TRADING -> executeTrades();
            case COMPLETE -> complete();
        }
    }

    private void findVillager() {
        String targetProfession = task.getStringParameter("profession");

        List<Villager> nearbyVillagers = foreman.level().getEntitiesOfClass(
            Villager.class,
            foreman.getBoundingBox().inflate(32)
        );

        // Filter by profession if specified
        if (targetProfession != null) {
            nearbyVillagers = nearbyVillagers.stream()
                .filter(v -> v.getVillagerData().getProfession().getName()
                    .equalsIgnoreCase(targetProfession))
                .toList();
        }

        if (nearbyVillagers.isEmpty()) {
            result = ActionResult.failure("No villagers found nearby");
            return;
        }

        // Pick closest villager with good reputation
        targetVillager = nearbyVillagers.stream()
            .filter(v -> !ReputationTracker.shouldAvoidVillager(v))
            .min(Comparator.comparingDouble(v ->
                v.distanceToSqr(foreman)))
            .orElse(nearbyVillagers.get(0));

        phase = Phase.APPROACHING;
        sendChatMessage("Found " + targetVillager.getVillagerData()
            .getProfession().getName() + " villager");
    }

    private void approachVillager() {
        if (targetVillager == null) {
            result = ActionResult.failure("Villager despawned");
            return;
        }

        double distance = foreman.distanceToSqr(targetVillager);

        if (distance < 4) {
            phase = Phase.ANALYZING;
            return;
        }

        foreman.getNavigation().moveTo(
            targetVillager.blockPosition(), 1.0
        );

        ticksWaited++;
        if (ticksWaited > 200) { // 10 second timeout
            result = ActionResult.failure("Cannot reach villager");
        }
    }

    private void analyzeTrades() {
        VillagerInfo info = VillagerAnalyzer.analyze(targetVillager)
            .orElseThrow();

        EmeraldBudget budget = new EmeraldBudget(
            foreman.getMemory().getEmeraldCount()
        );

        recommendations = TradeOptimizer.getInstance()
            .analyzeTrades(info, foreman.getInventory(), budget);

        if (recommendations.isEmpty()) {
            result = ActionResult.failure(
                "No affordable trades available");
            return;
        }

        String targetItem = task.getStringParameter("item");
        if (targetItem != null) {
            recommendations = recommendations.stream()
                .filter(r -> r.getOffer().getOutputItem()
                    .getItem().toString().contains(targetItem))
                .toList();
        }

        if (recommendations.isEmpty()) {
            result = ActionResult.failure(
                "No trades found for: " + targetItem);
            return;
        }

        sendChatMessage("Found " + recommendations.size() + " good trades");
        phase = Phase.TRADING;
    }

    private void executeTrades() {
        if (currentTradeIndex >= recommendations.size()) {
            phase = Phase.COMPLETE;
            return;
        }

        TradeRecommendation rec = recommendations.get(currentTradeIndex);
        TradeOffer offer = rec.getOffer();

        // Execute the trade
        boolean success = executeSingleTrade(offer);

        if (success) {
            int tradesRemaining = rec.getMaxTrades() - 1;

            if (tradesRemaining <= 0) {
                currentTradeIndex++;
            } else {
                // Continue trading this offer
                rec.setMaxTrades(tradesRemaining);
            }
        } else {
            // Trade failed, try next one
            currentTradeIndex++;
        }

        // Small delay between trades
        ticksWaited++;
        if (ticksWaited < 5) {
            return;
        }
        ticksWaited = 0;
    }

    private boolean executeSingleTrade(TradeOffer offer) {
        // Remove input items from inventory
        for (Map.Entry<Item, Integer> cost : offer.getResourceCosts().entrySet()) {
            int removed = foreman.getInventory()
                .removeItem(cost.getKey(), cost.getValue());
            if (removed < cost.getValue()) {
                return false; // Couldn't afford
            }
        }

        // Add output item to inventory
        foreman.getInventory().addItem(offer.getOutputItem());

        // Update emerald budget
        foreman.getMemory().updateEmeralds(
            offer.getEmeraldChange()
        );

        // Actually perform Minecraft trade
        // This requires packet manipulation or reflection
        performMinecraftTrade(offer);

        return true;
    }

    private void performMinecraftTrade(TradeOffer offer) {
        // This requires accessing Minecraft's trade system
        // Option 1: Open merchant menu and simulate clicks
        // Option 2: Use reflection to directly modify villager offers
        // Option 3: Send custom packets to server

        // Simplified: Just log for now
        LOGGER.info("Trading: {} -> {}",
            offer.getInputItems(),
            offer.getOutputItem());
    }

    private void complete() {
        int tradesCompleted = recommendations.stream()
            .mapToInt(TradeRecommendation::getMaxTrades)
            .sum();

        result = ActionResult.success(
            "Completed " + tradesCompleted + " trades");
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Trade with villager";
    }
}
```

### Data Classes

```java
package com.minewright.villager;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import java.util.*;

public record VillagerInfo(
    UUID uuid,
    Profession profession,
    int level,
    List<TradeOffer> offers,
    BlockPos position
) {
    public String getProfessionName() {
        return profession.getName();
    }

    public boolean isMasterLevel() {
        return level >= 5;
    }
}

public record TradeOffer(
    ItemStack outputItem,
    Map<Item, Integer> resourceCosts,
    int emeraldCost,
    int maxUses,
    int xpReward
) {
    public static TradeOffer fromMinecraft(MerchantOffer offer) {
        Map<Item, Integer> costs = new HashMap<>();

        // Parse first input cost
        ItemStack costA = offer.getCostA();
        if (!costA.isEmpty()) {
            costs.put(costA.getItem(), costA.getCount());
        }

        // Parse second input cost (if present)
        ItemStack costB = offer.getCostB();
        if (costB != null && !costB.isEmpty()) {
            costs.put(costB.getItem(), costB.getCount());
        }

        int emeraldCost = costs.entrySet().stream()
            .filter(e -> e.getKey() == Items.EMERALD)
            .mapToInt(Map.Entry::getValue)
            .sum();

        return new TradeOffer(
            offer.getResult(),
            costs,
            emeraldCost,
            offer.getMaxUses(),
            offer.getXp()
        );
    }

    public int getEmeraldChange() {
        // Positive if we gain emeralds, negative if we spend
        int outputEmeralds = outputItem.getItem() == Items.EMERALD ?
            outputItem.getCount() : 0;
        return outputEmeralds - emeraldCost;
    }
}

public record TradeRecommendation(
    TradeOffer offer,
    double score,
    int maxTrades,
    double roi
) {
    public boolean isAffordable() {
        return maxTrades > 0;
    }
}

public record TradingGoal(
    GoalType type,
    Item targetItem,
    int quantity,
    int maxBudget
) {
    public enum GoalType {
        SPECIFIC_ITEM,
        MAXIMIZE_EMERALDS,
        HIGH_VALUE,
        ANY_PROFITABLE
    }
}
```

### PromptBuilder Integration

Add to `PromptBuilder.java`:

```java
// In buildSystemPrompt(), add to ACTIONS list:
- trade: {"profession": "librarian", "item": "mending_book"} (optional: profession, item)

// In VALID MINECRAFT BLOCK TYPES, add:
VILLAGER PROFESSIONS: farmer, librarian, cleric, armorer, weaponsmith, toolsmith, butcher, leatherworker, shepherd, fisherman, fletcher, mason, cartographer

// Example for user prompt:
Input: "get mending book"
{"reasoning": "Need librarian with mending book trade", "plan": "Trade with librarian", "tasks": [{"action": "trade", "parameters": {"profession": "librarian", "item": "mending_book"}}]}

Input: "buy diamond pickaxe"
{"reasoning": "Trade with toolsmith for diamond pickaxe", "plan": "Trade for tool", "tasks": [{"action": "trade", "parameters": {"profession": "toolsmith", "item": "diamond_pickaxe"}}]}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)

**Tasks:**
1. Create `VillagerInfo` and `TradeOffer` data classes
2. Implement `VillagerAnalyzer` for profession detection
3. Add villager detection to `WorldKnowledge`
4. Basic unit tests for villager scanning

**Deliverables:**
- Data classes implemented
- Can detect and categorize villagers
- Unit tests passing

### Phase 2: Trade Analysis (Week 1-2)

**Tasks:**
1. Implement `TradeOptimizer` with scoring algorithm
2. Create `ItemValueDatabase` with item valuations
3. Add `EmeraldBudget` class
4. Write tests for trade scoring

**Deliverables:**
- Trade analysis working
- Can rank trades by value
- Budget tracking functional

### Phase 3: Core Trading Action (Week 2)

**Tasks:**
1. Implement `VillagerTradeAction` extending `BaseAction`
2. Add villager pathfinding
3. Implement trade execution via reflection/packets
4. Test basic trades

**Deliverables:**
- Can navigate to villager
- Can execute trades
- Action completes successfully

### Phase 4: Reputation & Restock (Week 3)

**Tasks:**
1. Implement `ReputationTracker`
2. Add gossip system integration
3. Implement `RestockMonitor`
4. Add workstation checking

**Deliverables:**
- Reputation-aware trading
- Waits for restocks when needed
- Avoids bad reputation villagers

### Phase 5: Economy Management (Week 3-4)

**Tasks:**
1. Implement emerald income strategies
2. Add arbitrage detection (crops for emeralds)
3. Create trading goal system
4. Implement economic cycle

**Deliverables:**
- AI can generate emeralds from farming
- Optimizes for profit
- Budget-conscious trading

### Phase 6: Integration (Week 4)

**Tasks:**
1. Register `VillagerTradeAction` in `CoreActionsPlugin`
2. Update `PromptBuilder` with trade action
3. Add trade history to `ForemanMemory`
4. Create villager location memory

**Deliverables:**
- LLM can request trades
- Trade history persists
- Villagers remembered across sessions

### Phase 7: Polish & Testing (Week 5)

**Tasks:**
1. Add chat messages for trading status
2. Handle edge cases (villager death, dispawn)
3. Performance optimization (caching, lazy loading)
4. Comprehensive testing

**Deliverables:**
- Smooth user experience
- Robust error handling
- Performance benchmarks

### Phase 8: Advanced Features (Optional, Week 6+)

**Tasks:**
1. Raid participation for Hero discounts
2. Zombie villager curing for reputation
3. Trading hall construction
4. Multi-villager trading routes

**Deliverables:**
- Auto-cures zombie villagers
- Builds trading halls
- Optimizes villager economy

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testVillagerProfessionDetection() {
    Villager mockVillager = createMockVillager(Profession.LIBRARIAN, 3);
    Optional<VillagerInfo> info = VillagerAnalyzer.analyze(mockVillager);

    assertTrue(info.isPresent());
    assertEquals(Profession.LIBRARIAN, info.get().profession());
    assertEquals(3, info.get().level());
}

@Test
public void testTradeScoring() {
    TradeOffer mendingBook = createMendingBookOffer(1, 64); // 64 emeralds
    TradeOffer wheatForEmerald = createWheatOffer(20, 1);   // 20 wheat = 1 emerald

    double mendingScore = TradeOptimizer.calculateScore(mendingBook);
    double wheatScore = TradeOptimizer.calculateScore(wheatForEmerald);

    assertTrue(mendingScore > wheatScore); // Mending should score higher
}

@Test
public void testEmeraldBudget() {
    EmeraldBudget budget = new EmeraldBudget(100);

    assertTrue(budget.canSpend(50));
    budget.recordSpent(50);

    assertTrue(budget.canSpend(40));
    assertFalse(budget.canSpend(60));

    assertEquals(50, budget.getRemaining());
    assertEquals(0.5, budget.getUtilization(), 0.01);
}
```

### Integration Tests

```java
@Test
public void testFullTradingCycle() {
    // Setup: Spawn villager, give foreman emeralds
    Villager villager = spawnVillager(foreman.level(), Profession.FARMER);
    foreman.getMemory().addEmeralds(64);

    // Execute trade action
    Task tradeTask = new Task("trade", Map.of(
        "profession", "farmer",
        "item", "emerald" // Actually wants to SELL crops
    ));

    VillagerTradeAction action = new VillagerTradeAction(foreman, tradeTask);
    action.start();

    // Wait for completion
    tickAction(action, 200);

    assertTrue(action.isComplete());
    assertTrue(action.getResult().isSuccess());
}
```

---

## Performance Considerations

### Caching Strategy

```java
public class VillagerCache {
    private static final Map<UUID, VillagerInfo> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 60 * 1000; // 1 minute

    public static Optional<VillagerInfo> getCached(Villager villager) {
        VillagerInfo info = cache.get(villager.getUUID());
        if (info != null && !isStale(info)) {
            return Optional.of(info);
        }
        return Optional.empty();
    }

    public static void cache(VillagerInfo info) {
        cache.put(info.uuid(), info);
    }

    private static boolean isStale(VillagerInfo info) {
        return info.age() > CACHE_TTL;
    }
}
```

### Lazy Loading

Only analyze villager trades when actually trading, not during initial scan.

### Batch Processing

When analyzing multiple villagers, process in parallel:

```java
List<VillagerInfo> infos = villagers.parallelStream()
    .map(VillagerAnalyzer::analyze)
    .filter(Optional::isPresent)
    .map(Optional::get)
    .toList();
```

---

## Configuration

Add to `config/minewright-common.toml`:

```toml
[villager_trading]
# Maximum emeralds to spend per trading session
max_trading_budget = 64

# Minimum reputation score to trade with a villager
min_reputation_threshold = -5

# Whether to wait for villager restocks
wait_for_restock = true

# Maximum time to wait for restock (in ticks)
max_restock_wait_ticks = 1200

# Priority items to always buy if available
priority_items = [
    "enchanted_book|mending",
    "enchanted_book|unbreaking",
    "diamond_pickaxe",
    "emerald"
]

# Items to sell for emeralds (profitable trades)
profitable_sells = [
    "wheat",
    "potato",
    "carrot",
    "beetroot",
    "pumpkin",
    "melon_slice",
    "rotten_flesh"
]

# Minimum profit margin to execute a trade (as percentage)
min_profit_margin = 20
```

---

## Future Enhancements

1. **Trading Hall Construction**: AI builds and optimizes villager trading halls
2. **Auto-Curing**: Automatically cures zombie villagers for reputation boost
3. **Raid Participation**: Joins raids to get Hero of the Village discounts
4. **Cross-Villager Arbitrage**: Trades between multiple villagers for profit
5. **Market Analysis**: Tracks price changes and optimizes timing
6. **Emerald Farms**: Sets up automated emerald farms with crop trading
7. **Librarian Fishing**: Resets librarians to get specific enchantments
8. **Master Level Rush**: Prioritizes trades to level up villagers faster

---

## Conclusion

This villager trading AI system provides MineWright agents with sophisticated economic capabilities. The modular design allows for incremental implementation, starting with basic trading and progressively adding reputation management, restock awareness, and economic optimization.

The system integrates seamlessly with MineWright's existing action framework, memory system, and LLM-driven task planning, enabling agents to autonomously engage in complex trading activities that support their broader goals.

By implementing this system, MineWright agents will be able to:

- Automatically locate and interact with villagers
- Analyze and optimize trade opportunities
- Manage emerald economics intelligently
- Build reputation strategically
- Time trades for maximum benefit
- Contribute to autonomous self-sufficiency

This makes MineWright significantly more capable as an autonomous Minecraft AI companion.
