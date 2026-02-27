# Raid Farm Automation for MineWright

## Overview

This document describes the design and implementation of an automated raid farming system for the MineWright Minecraft mod (Forge 1.20.1). The system enables Foreman entities to automatically manage Bad Omen effects, trigger raids, collect loot (especially Totems of Undying), farm emeralds, and coordinate multi-agent raid operations.

## Table of Contents

1. [Raid Mechanics Overview](#raid-mechanics-overview)
2. [Bad Omen Management](#bad-omen-management)
3. [Raid Spawn Mechanics](#raid-spawn-mechanics)
4. [Totem of Undying Farming](#totem-of-undying-farming)
5. [Emerald Collection](#emerald-collection)
6. [Multi-Agent Raid Coordination](#multi-agent-raid-coordination)
7. [Farm Design Patterns](#farm-design-patterns)
8. [Code Examples](#code-examples)
9. [Implementation Roadmap](#implementation-roadmap)

---

## Raid Mechanics Overview

### What is a Raid?

A raid is an in-game event where waves of illagers attack a village. Players defend the village and receive valuable rewards upon completion.

### Raid Requirements (Minecraft 1.20.1 Java Edition)

| Requirement | Value |
|-------------|-------|
| Player with Bad Omen effect | Required |
| Village detection | 3×3×3 subchunk region around claimed POI |
| Minimum village size | 1 villager + 1 claimed bed |
| Difficulty | Normal or Hard (Easy won't trigger raids) |
| Game rule | `disableRaids` must be `false` |
| Dimension | Overworld only (no Nether/End raids) |

### Raid Waves by Difficulty

| Difficulty | Base Waves | With Bad Omen II-V |
|------------|-----------|-------------------|
| Easy | 3 waves | 4 waves |
| Normal | 5 waves | 6 waves |
| Hard | 7 waves | 8 waves |

### Raid Mob Composition

| Wave | Mobs That Can Spawn |
|------|---------------------|
| Wave 1-2 | Pillagers only |
| Wave 3+ | Pillagers, Vindicators, Ravagers |
| Wave 4+ | Adds Witches |
| Wave 5+ | Adds Evokers (Totem of Undying drop!) |
| Wave 7+ | Bonus waves with enchanted gear |

### Rewards

1. **Totem of Undying** - Dropped by Evokers (100% drop rate)
2. **Hero of the Village** - 40-minute effect with trade discounts
3. **Emeralds** - Dropped by raid mobs
4. **Enchanted Books** - Random drops
5. **Saddles** - Dropped by Ravagers
6. **Villager Gifts** - Free items from villagers after raid

---

## Bad Omen Management

### Bad Omen Effect Details

```java
// Bad Omen effect in Minecraft 1.20.1
MobEffects.BAD_OMEN

// Duration: 100 minutes (1:40:00)
// Can stack to Level V by killing multiple captains
// Each level increases raid difficulty and adds bonus waves
```

### Sources of Bad Omen

1. **Pillager Outpost Captains** - 1-3 captains per outpost
2. **Illager Patrols** - Spawn 24-48 blocks from player at light level ≤8
3. **Woodland Mansions** - Vindicator captains

### Detection Strategy

```java
package com.minewright.raid;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;

public class BadOmenManager {

    /**
     * Checks if a raid captain is nearby.
     * Captains are identified by the ominous banner on their head.
     */
    public static Optional<Raider> findNearbyCaptain(ForemanEntity foreman) {
        AABB searchBox = foreman.getBoundingBox().inflate(48.0);

        return foreman.level().getEntitiesOfClass(Raider.class, searchBox)
            .stream()
            .filter(raider -> raider.hasBanner()) // Captains have banners
            .filter(raider -> foreman.distanceToSqr(raider) < 48 * 48)
            .findFirst();
    }

    /**
     * Checks if the foreman currently has Bad Omen effect.
     */
    public static boolean hasBadOmen(ForemanEntity foreman) {
        return foreman.hasEffect(MobEffects.BAD_OMEN);
    }

    /**
     * Gets the current Bad Omen level (1-5).
     */
    public static int getBadOmenLevel(ForemanEntity foreman) {
        if (!hasBadOmen(foreman)) {
            return 0;
        }

        var effect = foreman.getEffect(MobEffects.BAD_OMEN);
        return effect.getAmplifier() + 1; // Amplifier is 0-indexed
    }

    /**
     * Removes Bad Omen effect (e.g., by drinking milk).
     */
    public static void clearBadOmen(ForemanEntity foreman) {
        foreman.removeEffect(MobEffects.BAD_OMEN);
        MineWrightMod.LOGGER.info("Foreman '{}' Bad Omen cleared",
            foreman.getSteveName());
    }

    /**
     * Stacks Bad Omen to higher levels for bonus raid waves.
     */
    public static void stackBadOmen(ForemanEntity foreman, int targetLevel) {
        int currentLevel = getBadOmenLevel(foreman);

        if (currentLevel >= targetLevel) {
            return; // Already at or above target level
        }

        // Apply Bad Omen with desired amplifier
        int amplifier = targetLevel - 1;
        int duration = 100 * 60 * 20; // 100 minutes in ticks

        foreman.addEffect(new MobEffectInstance(
            MobEffects.BAD_OMEN,
            duration,
            amplifier,
            false,
            false
        ));

        MineWrightMod.LOGGER.info("Foreman '{}' Bad Omen stacked to level {}",
            foreman.getSteveName(), targetLevel);
    }
}
```

### Captain Hunting Strategy

```java
package com.minewright.action.actions;

import com.minewright.raid.BadOmenManager;

public class HuntCaptainAction extends BaseAction {

    private enum Phase {
        SEARCHING_OUTPOST,
        HUNTING_CAPTAIN,
        KILLING_CAPTAIN,
        STACKING_BAD_OMEN,
        COMPLETE
    }

    private Phase phase = Phase.SEARCHING_OUTPOST;
    private Raider targetCaptain;
    private int captainsKilled = 0;
    private int targetBadOmenLevel;

    @Override
    protected void onStart() {
        targetBadOmenLevel = task.getIntParameter("bad_omen_level", 1);

        if (BadOmenManager.getBadOmenLevel(foreman) >= targetBadOmenLevel) {
            result = ActionResult.success("Already have Bad Omen level " +
                BadOmenManager.getBadOmenLevel(foreman));
            return;
        }

        sendChatMessage("Hunting for raid captains...");
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case SEARCHING_OUTPOST -> searchForCaptain();
            case HUNTING_CAPTAIN -> huntCaptain();
            case KILLING_CAPTAIN -> killCaptain();
            case STACKING_BAD_OMEN -> stackBadOmen();
            case COMPLETE -> complete();
        }
    }

    private void searchForCaptain() {
        Optional<Raider> captain = BadOmenManager.findNearbyCaptain(foreman);

        if (captain.isPresent()) {
            targetCaptain = captain.get();
            phase = Phase.HUNTING_CAPTAIN;
            sendChatMessage("Found raid captain!");
        } else {
            // Search for pillager outpost or patrol
            foreman.getNavigation().moveTo(
                findNearestOutpost(), 1.5
            );

            ticksRunning++;
            if (ticksRunning > 600) { // 30 second timeout
                result = ActionResult.failure(
                    "No raid captains found nearby");
            }
        }
    }

    private void huntCaptain() {
        if (targetCaptain == null || !targetCaptain.isAlive()) {
            phase = Phase.KILLING_CAPTAIN;
            return;
        }

        double distance = foreman.distanceTo(targetCaptain);

        if (distance <= 3.5) {
            phase = Phase.KILLING_CAPTAIN;
            return;
        }

        foreman.getNavigation().moveTo(targetCaptain, 2.0);
    }

    private void killCaptain() {
        captainsKilled++;

        // Check if we've reached target Bad Omen level
        int currentLevel = BadOmenManager.getBadOmenLevel(foreman);

        if (currentLevel >= targetBadOmenLevel) {
            phase = Phase.STACKING_BAD_OMEN;
        } else {
            // Kill more captains
            phase = Phase.SEARCHING_OUTPOST;
            sendChatMessage("Bad Omen level " + currentLevel +
                ", need level " + targetBadOmenLevel);
        }
    }

    private void stackBadOmen() {
        int currentLevel = BadOmenManager.getBadOmenLevel(foreman);

        result = ActionResult.success(
            "Killed " + captainsKilled + " captains, " +
            "Bad Omen level " + currentLevel);
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Hunt raid captains for Bad Omen";
    }
}
```

---

## Raid Spawn Mechanics

### Village Detection Algorithm

Minecraft detects villages using a **3×3×3 subchunk region** (subchunk = 16×16×16 blocks):

```java
package com.minewright.raid;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.core.BlockPos;

public class VillageDetector {

    /**
     * Finds the nearest village center.
     * A village is defined by claimed POIs (beds, workstations, bells).
     */
    public static Optional<BlockPos> findNearestVillage(ForemanEntity foreman) {
        ServerLevel level = (ServerLevel) foreman.level();

        // Search in expanding radius
        for (int radius = 32; radius <= 128; radius += 32) {
            AABB searchBox = foreman.getBoundingBox().inflate(radius);

            // Find all claimed beds
            List<BlockPos> beds = findClaimedBeds(level, searchBox);

            if (!beds.isEmpty()) {
                // Calculate village center (average position)
                BlockPos center = calculateVillageCenter(beds);
                return Optional.of(center);
            }
        }

        return Optional.empty();
    }

    /**
     * Finds all claimed beds within the search area.
     */
    private static List<BlockPos> findClaimedBeds(ServerLevel level,
            AABB searchBox) {

        List<BlockPos> beds = new ArrayList<>();

        // Scan all block entities in the area
        level.getBlockEntities(searchBox).forEach((pos, entity) -> {

            // Check if this is a claimed bed
            if (isClaimedBed(level, pos)) {
                beds.add(pos);
            }
        });

        return beds;
    }

    /**
     * Checks if a bed is claimed by a villager.
     */
    private static boolean isClaimedBed(ServerLevel level, BlockPos pos) {
        // Use Minecraft's internal POI system
        var poiManager = level.getPoiManager();

        // Check if this position is a bed POI
        return poiManager.getType(pos)
            .filter(type -> type == PointOfInterestType.HOME)
            .isPresent();
    }

    /**
     * Calculates the center point of a village from bed positions.
     */
    private static BlockPos calculateVillageCenter(List<BlockPos> beds) {
        if (beds.isEmpty()) {
            throw new IllegalArgumentException("No beds provided");
        }

        long sumX = 0, sumY = 0, sumZ = 0;

        for (BlockPos bed : beds) {
            sumX += bed.getX();
            sumY += bed.getY();
            sumZ += bed.getZ();
        }

        return new BlockPos(
            (int)(sumX / beds.size()),
            (int)(sumY / beds.size()),
            (int)(sumZ / beds.size())
        );
    }

    /**
     * Checks if the foreman is within raid trigger range of a village.
     */
    public static boolean isInVillageRange(ForemanEntity foreman) {
        Optional<BlockPos> village = findNearestVillage(foreman);

        if (village.isEmpty()) {
            return false;
        }

        double distance = foreman.blockPosition().distSqr(village.get());
        return distance < 64 * 64; // Within 64 blocks
    }

    /**
     * Gets the active raid at the foreman's location, if any.
     */
    public static Optional<Raid> getActiveRaid(ForemanEntity foreman) {
        ServerLevel level = (ServerLevel) foreman.level();

        return level.getRaids().getRaids().stream()
            .filter(raid -> {
                BlockPos center = raid.getCenter();
                double distance = foreman.blockPosition().distSqr(center);
                return distance < 96 * 96; // Within bossbar range
            })
            .findFirst();
    }
}
```

### Raid Triggering

```java
package com.minewright.action.actions;

import com.minewright.raid.VillageDetector;

public class TriggerRaidAction extends BaseAction {

    private enum Phase {
        CHECKING_BAD_OMEN,
        FINDING_VILLAGE,
        APPROACHING_VILLAGE,
        WAITING_FOR_RAID,
        RAID_ACTIVE,
        COMPLETE
    }

    private Phase phase = Phase.CHECKING_BAD_OMEN;
    private BlockPos villageCenter;
    private Raid activeRaid;

    @Override
    protected void onStart() {
        sendChatMessage("Preparing to trigger raid...");
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case CHECKING_BAD_OMEN -> checkBadOmen();
            case FINDING_VILLAGE -> findVillage();
            case APPROACHING_VILLAGE -> approachVillage();
            case WAITING_FOR_RAID -> waitForRaid();
            case RAID_ACTIVE -> monitorRaid();
            case COMPLETE -> complete();
        }
    }

    private void checkBadOmen() {
        if (!BadOmenManager.hasBadOmen(foreman)) {
            result = ActionResult.failure(
                "No Bad Omen effect. Hunt raid captains first.");
            return;
        }

        int level = BadOmenManager.getBadOmenLevel(foreman);
        sendChatMessage("Bad Omen level " + level + " active");

        phase = Phase.FINDING_VILLAGE;
    }

    private void findVillage() {
        Optional<BlockPos> village = VillageDetector.findNearestVillage(foreman);

        if (village.isEmpty()) {
            result = ActionResult.failure(
                "No village found nearby");
            return;
        }

        villageCenter = village.get();
        sendChatMessage("Found village at " +
            villageCenter.getX() + ", " + villageCenter.getZ());

        phase = Phase.APPROACHING_VILLAGE;
    }

    private void approachVillage() {
        if (villageCenter == null) {
            result = ActionResult.failure("Village despawned");
            return;
        }

        double distance = foreman.blockPosition().distSqr(villageCenter);

        // Raid triggers when entering village radius
        if (distance < 48 * 48) {
            phase = Phase.WAITING_FOR_RAID;
            sendChatMessage("Entering village range...");
            return;
        }

        foreman.getNavigation().moveTo(villageCenter, 1.5);

        ticksRunning++;
        if (ticksRunning > 400) { // 20 second timeout
            result = ActionResult.failure("Cannot reach village");
        }
    }

    private void waitForRaid() {
        Optional<Raid> raid = VillageDetector.getActiveRaid(foreman);

        if (raid.isPresent()) {
            activeRaid = raid.get();
            phase = Phase.RAID_ACTIVE;
            sendChatMessage("Raid triggered! " +
                activeRaid.getGroupsCount() + " waves");
        } else {
            ticksRunning++;
            if (ticksRunning > 100) { // 5 second timeout
                result = ActionResult.failure(
                    "Raid failed to trigger");
            }
        }
    }

    private void monitorRaid() {
        if (activeRaid == null || activeRaid.isStopped()) {
            boolean won = activeRaid != null && activeRaid.isVictory();

            if (won) {
                result = ActionResult.success(
                    "Raid won! Hero of the Village!");
            } else {
                result = ActionResult.failure(
                    "Raid failed or stopped");
            }
            return;
        }

        // Wait for raid completion
        ticksRunning++;
        if (ticksRunning > 3600) { // 3 minute timeout
            result = ActionResult.failure(
                "Raid taking too long");
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Trigger village raid";
    }
}
```

---

## Totem of Undying Farming

### Evoker Spawn Mechanics

Evokers spawn starting from **Wave 5** in Java Edition raids. Each wave can spawn 1-2 evokers.

### Totem Collection Strategy

```java
package com.minewright.action.actions;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Items;

public class CollectTotemAction extends BaseAction {

    private enum Phase {
        FINDING_EVOKER,
        APPROACHING_EVOKER,
        KILLING_EVOKER,
        COLLECTING_TOTEM,
        COMPLETE
    }

    private Phase phase = Phase.FINDING_EVOKER;
    private Raider targetEvoker;
    private int totemsCollected = 0;
    private int targetTotems;

    @Override
    protected void onStart() {
        targetTotems = task.getIntParameter("quantity", 1);
        sendChatMessage("Hunting for " + targetTotems + " Totems of Undying...");
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case FINDING_EVOKER -> findEvoker();
            case APPROACHING_EVOKER -> approachEvoker();
            case KILLING_EVOKER -> killEvoker();
            case COLLECTING_TOTEM -> collectTotem();
            case COMPLETE -> complete();
        }
    }

    private void findEvoker() {
        Optional<Raid> raid = VillageDetector.getActiveRaid(foreman);

        if (raid.isEmpty()) {
            result = ActionResult.failure(
                "No active raid found");
            return;
        }

        Raid activeRaid = raid.get();

        // Find living evokers in the raid
        targetEvoker = activeRaid.getRaiders().stream()
            .filter(raider -> raider.getType() == EntityType.EVOKER)
            .filter(raider -> raider.isAlive())
            .filter(raider -> foreman.distanceToSqr(raider) < 64 * 64)
            .findFirst()
            .orElse(null);

        if (targetEvoker == null) {
            // Check if raid is still active
            if (activeRaid.isStopped()) {
                result = ActionResult.failure(
                    "Raid ended, collected " + totemsCollected + " totems");
            }
            // Wait for next wave
            return;
        }

        phase = Phase.APPROACHING_EVOKER;
        sendChatMessage("Found Evoker!");
    }

    private void approachEvoker() {
        if (targetEvoker == null || !targetEvoker.isAlive()) {
            phase = Phase.KILLING_EVOKER;
            return;
        }

        double distance = foreman.distanceTo(targetEvoker);

        if (distance <= 3.5) {
            phase = Phase.KILLING_EVOKER;
            return;
        }

        foreman.getNavigation().moveTo(targetEvoker, 2.5);
        foreman.setSprinting(true);

        ticksRunning++;
        if (ticksRunning > 300) { // 15 second timeout
            // Evoker moved or died
            phase = Phase.FINDING_EVOKER;
        }
    }

    private void killEvoker() {
        // Kill the evoker (totem drops automatically)
        phase = Phase.COLLECTING_TOTEM;
        sendChatMessage("Evoker defeated!");
    }

    private void collectTotem() {
        totemsCollected++;

        if (totemsCollected >= targetTotems) {
            result = ActionResult.success(
                "Collected " + totemsCollected + " Totems of Undying!");
        } else {
            phase = Phase.FINDING_EVOKER;
            sendChatMessage("Collected " + totemsCollected + "/" +
                targetTotems + " totems");
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
        foreman.setSprinting(false);
    }

    @Override
    public String getDescription() {
        return "Collect Totems of Undying";
    }
}
```

### Automatic Totem Pickup

```java
package com.minewright.raid;

import net.minecraft.world.item.Items;

public class TotemCollector {

    /**
     * Scans the ground for dropped totems and picks them up.
     */
    public static int collectNearbyTotems(ForemanEntity foreman) {
        AABB searchBox = foreman.getBoundingBox().inflate(8.0);

        List<ItemEntity> items = foreman.level().getEntitiesOfClass(
            ItemEntity.class, searchBox
        );

        int collected = 0;

        for (ItemEntity item : items) {
            if (item.getItem().getItem() == Items.TOTEM_OF_UNDYING) {
                // Pick up the totem
                foreman.getInventory().addItem(item.getItem());
                item.discard();
                collected++;
            }
        }

        if (collected > 0) {
            MineWrightMod.LOGGER.info("Foreman '{}' collected {} totems",
                foreman.getSteveName(), collected);
        }

        return collected;
    }

    /**
     * Counts totems in the foreman's inventory.
     */
    public static int countTotems(ForemanEntity foreman) {
        return foreman.getInventory().countItem(Items.TOTEM_OF_UNDYING);
    }

    /**
     * Checks if the foreman should hold a totem for auto-resurrection.
     */
    public static void equipTotemIfAvailable(ForemanEntity foreman) {
        if (countTotems(foreman) == 0) {
            return;
        }

        // Check if offhand is empty
        ItemStack offhand = foreman.getOffhandItem();

        if (offhand.isEmpty()) {
            // Move a totem to offhand
            int totemSlot = findTotemSlot(foreman);

            if (totemSlot >= 0) {
                ItemStack totem = foreman.getInventory().removeItem(totemSlot, 1);
                foreman.setItemSlot(EquipmentSlot.OFFHAND, totem);

                MineWrightMod.LOGGER.info("Foreman '{}' equipped Totem of Undying",
                    foreman.getSteveName());
            }
        }
    }

    private static int findTotemSlot(ForemanEntity foreman) {
        for (int i = 0; i < foreman.getInventory().getContainerSize(); i++) {
            ItemStack stack = foreman.getInventory().getItem(i);

            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }
}
```

---

## Emerald Collection

### Emerald Sources in Raids

1. **Direct Drops** - Pillagers, Vindicators, Evokers drop emeralds
2. **Looting Bonus** - Looting enchantment increases drop rate
3. **Bonus Waves** - Bad Omen level V adds extra waves with more drops

### Collection Strategy

```java
package com.minewright.action.actions;

import net.minecraft.world.item.Items;

public class CollectRaidLootAction extends BaseAction {

    private enum Phase {
        SEARCHING_ITEMS,
        COLLECTING_ITEMS,
        ORGANIZING_LOOT,
        COMPLETE
    }

    private Phase phase = Phase.SEARCHING_ITEMS;
    private int emeraldsCollected = 0;
    private int itemsCollected = 0;

    @Override
    protected void onStart() {
        sendChatMessage("Collecting raid loot...");
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case SEARCHING_ITEMS -> searchForItems();
            case COLLECTING_ITEMS -> collectItems();
            case ORGANIZING_LOOT -> organizeLoot();
            case COMPLETE -> complete();
        }
    }

    private void searchForItems() {
        AABB searchBox = foreman.getBoundingBox().inflate(16.0);

        List<ItemEntity> items = foreman.level().getEntitiesOfClass(
            ItemEntity.class, searchBox
        );

        if (items.isEmpty()) {
            // No more items to collect
            phase = Phase.ORGANIZING_LOOT;
            return;
        }

        phase = Phase.COLLECTING_ITEMS;
    }

    private void collectItems() {
        AABB searchBox = foreman.getBoundingBox().inflate(8.0);

        List<ItemEntity> items = foreman.level().getEntitiesOfClass(
            ItemEntity.class, searchBox
        );

        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();

            // Priority: Emeralds > Totems > Enchanted Books > Other
            if (stack.getItem() == Items.EMERALD) {
                int collected = collectItem(item);
                emeraldsCollected += collected;
                itemsCollected++;
            } else if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                collectItem(item);
                itemsCollected++;
                sendChatMessage("Found Totem of Undying!");
            } else if (stack.getItem() == Items.ENCHANTED_BOOK) {
                collectItem(item);
                itemsCollected++;
            } else if (!stack.isEmpty()) {
                // Collect other valuable items
                collectItem(item);
                itemsCollected++;
            }
        }

        // Check if there are more items nearby
        List<ItemEntity> remaining = foreman.level().getEntitiesOfClass(
            ItemEntity.class, foreman.getBoundingBox().inflate(16.0)
        );

        if (remaining.isEmpty()) {
            phase = Phase.ORGANIZING_LOOT;
        }
    }

    private int collectItem(ItemEntity item) {
        ItemStack stack = item.getItem();
        int collected = stack.getCount();

        // Add to inventory
        foreman.getInventory().addItem(stack);
        item.discard();

        return collected;
    }

    private void organizeLoot() {
        // Update memory with emerald count
        int totalEmeralds = foreman.getInventory().countItem(Items.EMERALD);
        foreman.getMemory().setEmeraldCount(totalEmeralds);

        // Update totem count
        int totems = TotemCollector.countTotems(foreman);
        foreman.getMemory().setTotemCount(totems);

        result = ActionResult.success(
            "Collected " + itemsCollected + " items, " +
            emeraldsCollected + " emeralds. " +
            "Total emeralds: " + totalEmeralds);
    }

    @Override
    protected void onCancel() {
        // Save progress
        int totalEmeralds = foreman.getInventory().countItem(Items.EMERALD);
        foreman.getMemory().setEmeraldCount(totalEmeralds);
    }

    @Override
    public String getDescription() {
        return "Collect raid loot";
    }
}
```

### Looting Optimization

```java
package com.minewright.raid;

import net.minecraft.world.item.enchantment.Enchantments;

public class LootingOptimizer {

    /**
     * Equips the best looting weapon for maximum drops.
     */
    public static void equipLootingWeapon(ForemanEntity foreman) {
        ItemStack bestWeapon = findBestLootingWeapon(foreman);

        if (bestWeapon.isEmpty()) {
            return;
        }

        // Equip in main hand
        foreman.setItemInHand(InteractionHand.MAIN_HAND, bestWeapon);

        MineWrightMod.LOGGER.info("Foreman '{}' equipped looting weapon",
            foreman.getSteveName());
    }

    /**
     * Finds the weapon with highest looting enchantment.
     */
    private static ItemStack findBestLootingWeapon(ForemanEntity foreman) {
        ItemStack best = ItemStack.EMPTY;
        int bestLooting = -1;

        for (int i = 0; i < foreman.getInventory().getContainerSize(); i++) {
            ItemStack stack = foreman.getInventory().getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            // Check if it's a weapon
            if (!isWeapon(stack)) {
                continue;
            }

            // Get looting level
            int looting = getEnchantmentLevel(stack, Enchantments.MOB_LOOTING);

            if (looting > bestLooting) {
                best = stack;
                bestLooting = looting;
            }
        }

        return best;
    }

    private static boolean isWeapon(ItemStack stack) {
        // Sword, axe, or trident
        return stack.getItem() instanceof SwordItem ||
               stack.getItem() instanceof AxeItem ||
               stack.getItem() == Items.TRIDENT;
    }

    private static int getEnchantmentLevel(ItemStack stack,
            Enchantment enchantment) {

        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }
}
```

---

## Multi-Agent Raid Coordination

### Coordination Architecture

Multiple Foreman entities can coordinate raid operations for maximum efficiency:

```
Foreman 1 (Raid Leader)
    ├─ Triggers raid
    ├─ Coordinates other foremen
    └─ Collects loot

Foreman 2-4 (Combat Squad)
    ├─ Eliminates raid mobs
    ├─ Protects villagers
    └─ Targets evokers

Foreman 5+ (Support)
    ├─ Collects drops
    ├─ Restocks supplies
    └─ Builds farm infrastructure
```

### Coordination Protocol

```java
package com.minewright.raid;

import com.minewright.orchestration.AgentMessage;
import com.minewright.orchestration.OrchestratorService;

public class RaidCoordinator {

    private final ForemanEntity leader;
    private final OrchestratorService orchestrator;
    private final List<UUID> squadMembers;
    private Raid activeRaid;
    private RaidPhase currentPhase = RaidPhase.IDLE;

    public RaidCoordinator(ForemanEntity leader) {
        this.leader = leader;
        this.orchestrator = MineWrightMod.getOrchestratorService();
        this.squadMembers = new ArrayList<>();
    }

    public enum RaidPhase {
        IDLE,
        PREPARING,
        TRIGGERING,
        WAVE_IN_PROGRESS,
        COLLECTING_LOOT,
        COMPLETE
    }

    /**
     * Assembles a raid squad from available foremen.
     */
    public void assembleSquad(int squadSize) {
        CrewManager crewManager = MineWrightMod.getCrewManager();

        List<ForemanEntity> available = crewManager.getActiveCrew().stream()
            .filter(f -> f != leader)
            .filter(f -> !f.getActionExecutor().isExecuting())
            .limit(squadSize)
            .toList();

        for (ForemanEntity member : available) {
            squadMembers.add(member.getUUID());

            // Send assignment message
            AgentMessage assignment = AgentMessage.taskAssignment(
                leader.getSteveName(), leader.getSteveName(),
                member.getSteveName(),
                UUID.randomUUID().toString(),
                Map.of(
                    "taskDescription", "Join raid squad",
                    "role", "combat",
                    "followTarget", leader.getSteveName()
                )
            );

            orchestrator.getCommunicationBus().publish(assignment);
        }

        sendChatMessage("Assembled raid squad: " +
            (squadMembers.size() + 1) + " members");
    }

    /**
     * Coordinates squad positioning before triggering raid.
     */
    public void coordinatePositions() {
        currentPhase = RaidPhase.PREPARING;

        // Assign positions around village center
        BlockPos villageCenter = VillageDetector.findNearestVillage(leader)
            .orElseThrow();

        int anglePerMember = 360 / (squadMembers.size() + 1);

        int index = 0;
        for (UUID memberId : squadMembers) {
            ForemanEntity member = findForeman(memberId);
            if (member == null) continue;

            int angle = index * anglePerMember;
            int radius = 20;

            BlockPos targetPos = new BlockPos(
                villageCenter.getX() + (int)(radius * Math.cos(Math.toRadians(angle))),
                villageCenter.getY(),
                villageCenter.getZ() + (int)(radius * Math.sin(Math.toRadians(angle)))
            );

            // Send position assignment
            AgentMessage positionMsg = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_ASSIGNMENT)
                .sender(leader.getSteveName(), leader.getSteveName())
                .recipient(member.getSteveName())
                .content("Move to position")
                .payload("taskDescription", "Move to raid position")
                .payload("x", targetPos.getX())
                .payload("y", targetPos.getY())
                .payload("z", targetPos.getZ())
                .priority(AgentMessage.Priority.HIGH)
                .build();

            orchestrator.getCommunicationBus().publish(positionMsg);
            index++;
        }

        sendChatMessage("Squad positions assigned");
    }

    /**
     * Triggers the raid and signals squad to engage.
     */
    public void triggerRaid() {
        currentPhase = RaidPhase.TRIGGERING;

        // Signal squad to prepare for combat
        broadcastSquadMessage("Prepare for raid!", AgentMessage.Priority.HIGH);

        // Trigger the raid (leader enters village)
        // This is handled by TriggerRaidAction
    }

    /**
     * Coordinates combat during raid waves.
     */
    public void onRaidWave(int waveNumber, List<Raider> raiders) {
        currentPhase = RaidPhase.WAVE_IN_PROGRESS;

        sendChatMessage("Wave " + waveNumber + ": " +
            raiders.size() + " enemies");

        // Assign targets to squad members
        assignTargets(raiders);
    }

    /**
     * Assigns raid mob targets to squad members.
     */
    private void assignTargets(List<Raider> raiders) {
        // Priority: Evokers > Vindicators > Witches > Pillagers > Ravagers

        List<Raider> priorityTargets = raiders.stream()
            .sorted((a, b) -> {
                int priorityA = getTargetPriority(a.getType());
                int priorityB = getTargetPriority(b.getType());
                return Integer.compare(priorityB, priorityA); // Descending
            })
            .toList();

        int targetsPerMember = (int) Math.ceil(
            (double) priorityTargets.size() / (squadMembers.size() + 1)
        );

        // Assign leader's targets
        List<Raider> leaderTargets = priorityTargets.stream()
            .limit(targetsPerMember)
            .toList();

        leader.getMemory().setTargets(leaderTargets);

        // Assign squad members' targets
        int memberIndex = 0;
        UUID currentMemberId = null;

        for (Raider target : priorityTargets) {
            if (leaderTargets.contains(target)) {
                continue; // Already assigned to leader
            }

            // Get next squad member
            if (memberIndex < squadMembers.size()) {
                currentMemberId = squadMembers.get(memberIndex);
                memberIndex++;
            } else {
                memberIndex = 0;
                currentMemberId = squadMembers.get(memberIndex);
                memberIndex++;
            }

            // Send target assignment
            ForemanEntity member = findForeman(currentMemberId);
            if (member != null) {
                AgentMessage targetMsg = new AgentMessage.Builder()
                    .type(AgentMessage.Type.TASK_ASSIGNMENT)
                    .sender(leader.getSteveName(), leader.getSteveName())
                    .recipient(member.getSteveName())
                    .content("Attack target")
                    .payload("taskDescription", "Attack " +
                        target.getType().toString())
                    .payload("targetUuid", target.getUUID().toString())
                    .payload("targetX", target.getX())
                    .payload("targetY", target.getY())
                    .payload("targetZ", target.getZ())
                    .priority(AgentMessage.Priority.URGENT)
                    .build();

                orchestrator.getCommunicationBus().publish(targetMsg);
            }
        }
    }

    private int getTargetPriority(EntityType<?> type) {
        if (type == EntityType.EVOKER) return 100;
        if (type == EntityType.VINDICATOR) return 80;
        if (type == EntityType.WITCH) return 60;
        if (type == EntityType.PILLAGER) return 40;
        if (type == EntityType.RAVAGER) return 20;
        return 0;
    }

    /**
     * Coordinates loot collection after raid completion.
     */
    public void onRaidComplete(boolean victory) {
        currentPhase = RaidPhase.COLLECTING_LOOT;

        if (victory) {
            sendChatMessage("Raid victory! Collecting loot...");

            // Assign collection zones to squad members
            assignLootZones();
        } else {
            sendChatMessage("Raid failed. Regrouping...");
        }
    }

    private void assignLootZones() {
        BlockPos villageCenter = VillageDetector.findNearestVillage(leader)
            .orElseThrow();

        int zoneSize = 16;
        int zonesPerRow = (int) Math.ceil(Math.sqrt(squadMembers.size() + 1));

        int index = 0;
        for (UUID memberId : squadMembers) {
            int row = index / zonesPerRow;
            int col = index % zonesPerRow;

            BlockPos zoneCenter = new BlockPos(
                villageCenter.getX() + (col - zonesPerRow / 2) * zoneSize,
                villageCenter.getY(),
                villageCenter.getZ() + (row - zonesPerRow / 2) * zoneSize
            );

            // Send collection zone assignment
            ForemanEntity member = findForeman(memberId);
            if (member != null) {
                AgentMessage collectMsg = new AgentMessage.Builder()
                    .type(AgentMessage.Type.TASK_ASSIGNMENT)
                    .sender(leader.getSteveName(), leader.getSteveName())
                    .recipient(member.getSteVEName())
                    .content("Collect loot in zone")
                    .payload("taskDescription", "Collect loot")
                    .payload("zoneX", zoneCenter.getX())
                    .payload("zoneZ", zoneCenter.getZ())
                    .payload("zoneRadius", zoneSize / 2)
                    .priority(AgentMessage.Priority.NORMAL)
                    .build();

                orchestrator.getCommunicationBus().publish(collectMsg);
            }

            index++;
        }
    }

    private void broadcastSquadMessage(String message,
            AgentMessage.Priority priority) {

        for (UUID memberId : squadMembers) {
            ForemanEntity member = findForeman(memberId);
            if (member == null) continue;

            AgentMessage broadcast = new AgentMessage.Builder()
                .type(AgentMessage.Type.BROADCAST)
                .sender(leader.getSteveName(), leader.getSteveName())
                .recipient(member.getSteveName())
                .content(message)
                .priority(priority)
                .build();

            orchestrator.getCommunicationBus().publish(broadcast);
        }
    }

    private ForemanEntity findForeman(UUID uuid) {
        return MineWrightMod.getCrewManager().getCrewMember(uuid);
    }

    private void sendChatMessage(String message) {
        leader.sendChatMessage("[Raid Coordinator] " + message);
    }
}
```

### Squad Combat Action

```java
package com.minewright.action.actions;

public class RaidSquadCombatAction extends BaseAction {

    private enum Phase {
        MOVING_TO_POSITION,
        WAITING_FOR_RAID,
        ENGAGING_TARGETS,
        COLLECTING_LOOT,
        COMPLETE
    }

    private Phase phase = Phase.MOVING_TO_POSITION;
    private BlockPos assignedPosition;
    private LivingEntity currentTarget;
    private int targetsEliminated = 0;

    @Override
    protected void onStart() {
        // Parse position from task
        int x = task.getIntParameter("x", 0);
        int y = task.getIntParameter("y", 0);
        int z = task.getIntParameter("z", 0);

        assignedPosition = new BlockPos(x, y, z);

        foreman.setInvulnerableBuilding(false); // Enable combat
        sendChatMessage("Moving to raid position...");
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case MOVING_TO_POSITION -> moveToPosition();
            case WAITING_FOR_RAID -> waitForRaid();
            case ENGAGING_TARGETS -> engageTargets();
            case COLLECTING_LOOT -> collectLoot();
            case COMPLETE -> complete();
        }
    }

    private void moveToPosition() {
        if (assignedPosition == null) {
            phase = Phase.WAITING_FOR_RAID;
            return;
        }

        double distance = foreman.blockPosition().distSqr(assignedPosition);

        if (distance < 4) {
            phase = Phase.WAITING_FOR_RAID;
            return;
        }

        foreman.getNavigation().moveTo(assignedPosition, 2.0);

        ticksRunning++;
        if (ticksRunning > 400) { // 20 second timeout
            phase = Phase.WAITING_FOR_RAID; // Give up and wait
        }
    }

    private void waitForRaid() {
        // Check if raid has started
        Optional<Raid> raid = VillageDetector.getActiveRaid(foreman);

        if (raid.isPresent()) {
            phase = Phase.ENGAGING_TARGETS;
            sendChatMessage("Raid started! Engaging enemies!");
        }
    }

    private void engageTargets() {
        // Check for assigned target from coordinator
        String targetUuid = task.getStringParameter("targetUuid");

        if (targetUuid != null) {
            // Find and attack specific target
            Entity target = foreman.level().getEntity(UUID.fromString(targetUuid));

            if (target instanceof LivingEntity living && living.isAlive()) {
                currentTarget = living;
            }
        }

        // Find nearby targets if none assigned
        if (currentTarget == null || !currentTarget.isAlive()) {
            findNearestTarget();
        }

        if (currentTarget == null) {
            // Check if raid is complete
            Optional<Raid> raid = VillageDetector.getActiveRaid(foreman);

            if (raid.isEmpty() || raid.get().isStopped()) {
                phase = Phase.COLLECTING_LOOT;
                return;
            }

            return; // Wait for targets
        }

        // Attack target
        double distance = foreman.distanceTo(currentTarget);

        if (distance <= 3.5) {
            foreman.doHurtTarget(currentTarget);
            foreman.swing(InteractionHand.MAIN_HAND, true);

            if (ticksRunning % 7 == 0) {
                foreman.doHurtTarget(currentTarget);
            }

            if (!currentTarget.isAlive()) {
                targetsEliminated++;
                currentTarget = null;
            }
        } else {
            foreman.setSprinting(true);
            foreman.getNavigation().moveTo(currentTarget, 2.5);
        }
    }

    private void findNearestTarget() {
        AABB searchBox = foreman.getBoundingBox().inflate(32.0);

        List<Entity> raiders = foreman.level().getEntities(foreman, searchBox)
            .stream()
            .filter(e -> e instanceof Raider)
            .filter(LivingEntity::isAlive)
            .toList();

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : raiders) {
            if (entity instanceof LivingEntity living) {
                double distance = foreman.distanceTo(living);

                if (distance < nearestDistance) {
                    nearest = living;
                    nearestDistance = distance;
                }
            }
        }

        currentTarget = nearest;
    }

    private void collectLoot() {
        // Collect items in assigned zone
        int zoneRadius = task.getIntParameter("zoneRadius", 16);
        int zoneX = task.getIntParameter("zoneX", 0);
        int zoneZ = task.getIntParameter("zoneZ", 0);

        AABB zone = new AABB(
            zoneX - zoneRadius, foreman.getY() - 4, zoneZ - zoneRadius,
            zoneX + zoneRadius, foreman.getY() + 4, zoneZ + zoneRadius
        );

        List<ItemEntity> items = foreman.level().getEntitiesOfClass(
            ItemEntity.class, zone
        );

        int collected = 0;
        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();

            // Prioritize emeralds and totems
            if (stack.getItem() == Items.EMERALD ||
                stack.getItem() == Items.TOTEM_OF_UNDYING) {

                foreman.getInventory().addItem(stack);
                item.discard();
                collected++;
            }
        }

        phase = Phase.COMPLETE;
        result = ActionResult.success(
            "Eliminated " + targetsEliminated + " enemies, " +
            "collected " + collected + " items");
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
        foreman.setSprinting(false);
        foreman.setInvulnerableBuilding(true); // Re-enable invulnerability
    }

    @Override
    public String getDescription() {
        return "Raid squad combat";
    }
}
```

---

## Farm Design Patterns

### Design Pattern 1: Simple Raid Tower

A compact, single-player raid farm design:

```
Top-down view:
+---------------------+
|         W           |  W = Water stream
|       W W W         |
|     W   V   W       |  V = Villager (village center)
|       W W W         |
|         W           |
|         |           |
|     [Foreman]       |  Foreman position
|                     |
+---------------------+

Side view:
    +-------+
    |  Bed  |  Villager bed
    +-------+
        |
    +-------+
    | Villager |  Village POI
    +-------+
        |
    +-------+
    | Water |  Flowing water
    +-------+
        |
    +-------+
    | Drop  |  Raid mobs drop here
    | chute |
    +-------+
        |
    +-------+
    |Kill   |  Foreman kills mobs
    |zone   |
    +-------+
        |
    +-------+
    |Hopper |  Loot collection
    |system |
    +-------+
```

### Design Pattern 2: Multi-Agent Raid Arena

A larger design for coordinated squad operations:

```
Top-down view (64x64 arena):
+--------------------------------------------------+
|                                                  |
|   F1   F2   F3   F4                             |
|    \   |   /   |                                |
|     \  |  /   /                                 |
|      \ | /   /                                  |
|       \|/   /                    Village Zone   |
|        V  /                    +-------------+  |
|       /|\ /                    | V V V V V   |  |
|      / | X                    |  B   B   B  |  |
|     /  |/ \                   | V V V V V   |  |
|    /   |   \                  +-------------+  |
|   F5   F6   F7                                 |
|                                                  |
+--------------------------------------------------+

Legend:
F1-F7 = Foreman positions
V = Villager
B = Bed
X = Raid spawn center (village center)
```

### Building Construction AI

```java
package com.minewright.action.actions;

public class BuildRaidFarmAction extends BaseAction {

    private enum Phase {
        DESIGNING,
        CLEARING_AREA,
        BUILDING_VILLAGE,
        BUILDING_KILL_CHAMBER,
        BUILDING_COLLECTION,
        COMPLETE
    }

    private Phase phase = Phase.DESIGNING;
    private RaidFarmDesign design;
    private int blocksPlaced = 0;

    @Override
    protected void onStart() {
        String designType = task.getStringParameter("design", "simple_tower");

        design = RaidFarmDesign.create(designType, foreman.blockPosition());

        sendChatMessage("Building " + designType + " raid farm...");
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case DESIGNING -> designFarm();
            case CLEARING_AREA -> clearArea();
            case BUILDING_VILLAGE -> buildVillage();
            case BUILDING_KILL_CHAMBER -> buildKillChamber();
            case BUILDING_COLLECTION -> buildCollection();
            case COMPLETE -> complete();
        }
    }

    private void designFarm() {
        // Calculate farm layout
        design.calculateLayout();

        phase = Phase.CLEARING_AREA;
    }

    private void clearArea() {
        // Clear area for farm
        BlockPos min = design.getMinCorner();
        BlockPos max = design.getMaxCorner();

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (ticksRunning % 5 == 0) { // Rate limit
                return; // Continue next tick
            }

            // Remove blocks
            foreman.level().removeBlock(pos, false);
            blocksPlaced++;
        }

        phase = Phase.BUILDING_VILLAGE;
    }

    private void buildVillage() {
        // Place villager beds and workstations
        for (BlockPos bedPos : design.getBedPositions()) {
            if (ticksRunning % 5 == 0) return;

            foreman.level().setBlock(bedPos,
                Blocks.WHITE_BED.defaultBlockState(), 3);
            blocksPlaced++;
        }

        phase = Phase.BUILDING_KILL_CHAMBER;
    }

    private void buildKillChamber() {
        // Build water streams and drop chute
        for (BlockPos pos : design.getWaterPositions()) {
            if (ticksRunning % 5 == 0) return;

            foreman.level().setBlock(pos,
                Blocks.WATER.defaultBlockState(), 3);
            blocksPlaced++;
        }

        phase = Phase.BUILDING_COLLECTION;
    }

    private void buildCollection() {
        // Place hoppers and chests
        for (BlockPos pos : design.getHopperPositions()) {
            if (ticksRunning % 5 == 0) return;

            foreman.level().setBlock(pos,
                Blocks.HOPPER.defaultBlockState(), 3);
            blocksPlaced++;
        }

        for (BlockPos pos : design.getChestPositions()) {
            if (ticksRunning % 5 == 0) return;

            foreman.level().setBlock(pos,
                Blocks.CHEST.defaultBlockState(), 3);
            blocksPlaced++;
        }

        phase = Phase.COMPLETE;
    }

    @Override
    protected void onCancel() {
        sendChatMessage("Farm construction cancelled at " +
            blocksPlaced + " blocks");
    }

    @Override
    public String getDescription() {
        return "Build raid farm";
    }
}

class RaidFarmDesign {
    private BlockPos center;
    private String type;
    private List<BlockPos> bedPositions;
    private List<BlockPos> waterPositions;
    private List<BlockPos> hopperPositions;
    private List<BlockPos> chestPositions;

    public static RaidFarmDesign create(String type, BlockPos center) {
        return switch (type) {
            case "simple_tower" -> new SimpleTowerDesign(center);
            case "multi_agent_arena" -> new MultiAgentArenaDesign(center);
            default -> new SimpleTowerDesign(center);
        };
    }

    public void calculateLayout() {
        // Calculate positions based on design
        bedPositions = calculateBedPositions();
        waterPositions = calculateWaterPositions();
        hopperPositions = calculateHopperPositions();
        chestPositions = calculateChestPositions();
    }

    // Getters
    public List<BlockPos> getBedPositions() { return bedPositions; }
    public List<BlockPos> getWaterPositions() { return waterPositions; }
    public List<BlockPos> getHopperPositions() { return hopperPositions; }
    public List<BlockPos> getChestPositions() { return chestPositions; }
    public BlockPos getMinCorner() { return center.offset(-16, -5, -16); }
    public BlockPos getMaxCorner() { return center.offset(16, 10, 16); }

    // Abstract methods for subclasses
    protected List<BlockPos> calculateBedPositions() { return new ArrayList<>(); }
    protected List<BlockPos> calculateWaterPositions() { return new ArrayList<>(); }
    protected List<BlockPos> calculateHopperPositions() { return new ArrayList<>(); }
    protected List<BlockPos> calculateChestPositions() { return new ArrayList<>(); }
}

class SimpleTowerDesign extends RaidFarmDesign {
    public SimpleTowerDesign(BlockPos center) {
        this.center = center;
        this.type = "simple_tower";
    }

    @Override
    protected List<BlockPos> calculateBedPositions() {
        List<BlockPos> beds = new ArrayList<>();
        // Place 5 beds for village
        for (int i = -2; i <= 2; i++) {
            beds.add(center.offset(i, 0, -5));
        }
        return beds;
    }

    @Override
    protected List<BlockPos> calculateWaterPositions() {
        List<BlockPos> water = new ArrayList<>();
        // Create cross-shaped water pattern
        for (int i = -3; i <= 3; i++) {
            water.add(center.offset(i, 0, 0));
            water.add(center.offset(0, 0, i));
        }
        return water;
    }

    @Override
    protected List<BlockPos> calculateHopperPositions() {
        List<BlockPos> hoppers = new ArrayList<>();
        // Hopper line under kill zone
        for (int i = -2; i <= 2; i++) {
            hoppers.add(center.offset(i, -3, 0));
        }
        return hoppers;
    }

    @Override
    protected List<BlockPos> calculateChestPositions() {
        List<BlockPos> chests = new ArrayList<>();
        // Chests at end of hopper line
        chests.add(center.offset(-3, -3, 0));
        chests.add(center.offset(3, -3, 0));
        return chests;
    }
}
```

---

## Code Examples

### Complete Raid Automation Action

```java
package com.minewright.action.actions;

/**
 * High-level action that orchestrates a complete raid cycle:
 * 1. Get Bad Omen
 * 2. Trigger raid
 * 3. Fight waves
 * 4. Collect loot
 * 5. Repeat
 */
public class AutomatedRaidAction extends BaseAction {

    private enum Phase {
        HUNTING_CAPTAINS,
        TRIGGERING_RAID,
        FIGHTING_RAID,
        COLLECTING_LOOT,
        RESETTING,
        COMPLETE
    }

    private Phase phase = Phase.HUNTING_CAPTAINS;
    private int raidsCompleted = 0;
    private int targetRaids;
    private int totemsCollected = 0;
    private int emeraldsCollected = 0;
    private RaidCoordinator coordinator;

    @Override
    protected void onStart() {
        targetRaids = task.getIntParameter("raids", 1);
        int squadSize = task.getIntParameter("squad_size", 0);

        if (squadSize > 0) {
            coordinator = new RaidCoordinator(foreman);
            coordinator.assembleSquad(squadSize);
        }

        sendChatMessage("Starting automated raid sequence (" +
            targetRaids + " raids)...");
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case HUNTING_CAPTAINS -> huntCaptains();
            case TRIGGERING_RAID -> triggerRaid();
            case FIGHTING_RAID -> fightRaid();
            case COLLECTING_LOOT -> collectLoot();
            case RESETTING -> reset();
            case COMPLETE -> complete();
        }
    }

    private void huntCaptains() {
        if (BadOmenManager.getBadOmenLevel(foreman) >= 1) {
            phase = Phase.TRIGGERING_RAID;
            return;
        }

        // Check if we need to hunt captains
        Task huntTask = new Task("hunt_captain", Map.of(
            "bad_omen_level", 1
        ));

        HuntCaptainAction huntAction = new HuntCaptainAction(foreman, huntTask);
        huntAction.start();

        while (!huntAction.isComplete()) {
            huntAction.tick();
        }

        if (huntAction.getResult().isSuccess()) {
            phase = Phase.TRIGGERING_RAID;
        } else {
            result = ActionResult.failure(
                "Failed to get Bad Omen: " +
                huntAction.getResult().getMessage());
        }
    }

    private void triggerRaid() {
        if (coordinator != null) {
            coordinator.coordinatePositions();
        }

        Task triggerTask = new Task("trigger_raid", Map.of());
        TriggerRaidAction triggerAction = new TriggerRaidAction(foreman, triggerTask);
        triggerAction.start();

        while (!triggerAction.isComplete()) {
            triggerAction.tick();
        }

        if (triggerAction.getResult().isSuccess()) {
            phase = Phase.FIGHTING_RAID;
        } else {
            result = ActionResult.failure(
                "Failed to trigger raid: " +
                triggerAction.getResult().getMessage());
        }
    }

    private void fightRaid() {
        Optional<Raid> raid = VillageDetector.getActiveRaid(foreman);

        if (raid.isEmpty() || raid.get().isStopped()) {
            phase = Phase.COLLECTING_LOOT;
            return;
        }

        Raid activeRaid = raid.get();

        if (coordinator != null) {
            coordinator.onRaidWave(
                activeRaid.getGroupsCount(),
                new ArrayList<>(activeRaid.getAllRaiders())
            );
        }

        // Fight the raid (using existing CombatAction)
        Task combatTask = new Task("attack", Map.of(
            "target", "raider"
        ));

        CombatAction combatAction = new CombatAction(foreman, combatTask);
        combatAction.start();

        while (!combatAction.isComplete() && !activeRaid.isStopped()) {
            combatAction.tick();
        }

        combatAction.cancel();

        if (activeRaid.isStopped()) {
            raidsCompleted++;
            sendChatMessage("Raid " + raidsCompleted + "/" +
                targetRaids + " complete!");
        }
    }

    private void collectLoot() {
        Task collectTask = new Task("collect_loot", Map.of());
        CollectRaidLootAction collectAction =
            new CollectRaidLootAction(foreman, collectTask);
        collectAction.start();

        while (!collectAction.isComplete()) {
            collectAction.tick();
        }

        emeraldsCollected += foreman.getInventory().countItem(Items.EMERALD);
        totemsCollected += TotemCollector.countTotems(foreman);

        if (coordinator != null) {
            coordinator.onRaidComplete(true);
        }

        if (raidsCompleted >= targetRaids) {
            phase = Phase.COMPLETE;
        } else {
            phase = Phase.RESETTING;
        }
    }

    private void reset() {
        // Wait for Bad Omen to expire or clear it
        BadOmenManager.clearBadOmen(foreman);

        // Short delay before next raid
        ticksRunning++;
        if (ticksRunning > 100) {
            phase = Phase.HUNTING_CAPTAINS;
        }
    }

    @Override
    protected void onCancel() {
        foreman.getNavigation().stop();
        BadOmenManager.clearBadOmen(foreman);
    }

    @Override
    public String getDescription() {
        return "Automated raid farming";
    }
}
```

### Plugin Registration

```java
package com.minewright.plugin;

public class RaidActionsPlugin implements ActionPlugin {

    @Override
    public String getPluginId() {
        return "raid-actions";
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        LOGGER.info("Loading RaidActionsPlugin");

        int priority = getPriority();

        // Bad Omen Management
        registry.register("hunt_captain",
            (foreman, task, ctx) -> new HuntCaptainAction(foreman, task),
            priority, getPluginId());

        // Raid Triggers
        registry.register("trigger_raid",
            (foreman, task, ctx) -> new TriggerRaidAction(foreman, task),
            priority, getPluginId());

        // Combat
        registry.register("fight_raid",
            (foreman, task, ctx) -> new RaidSquadCombatAction(foreman, task),
            priority, getPluginId());

        // Loot Collection
        registry.register("collect_loot",
            (foreman, task, ctx) -> new CollectRaidLootAction(foreman, task),
            priority, getPluginId());

        // Totem Farming
        registry.register("collect_totems",
            (foreman, task, ctx) -> new CollectTotemAction(foreman, task),
            priority, getPluginId());

        // Farm Building
        registry.register("build_raid_farm",
            (foreman, task, ctx) -> new BuildRaidFarmAction(foreman, task),
            priority, getPluginId());

        // Complete Automation
        registry.register("automated_raid",
            (foreman, task, ctx) -> new AutomatedRaidAction(foreman, task),
            priority, getPluginId());

        LOGGER.info("RaidActionsPlugin loaded 7 actions");
    }

    @Override
    public int getPriority() {
        return 500; // Medium priority
    }

    @Override
    public String[] getDependencies() {
        return new String[]{ "core-actions" };
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Raid farming automation: Bad Omen management, " +
               "raid triggers, combat, loot collection";
    }
}
```

### PromptBuilder Integration

```java
// In PromptBuilder.java, add to ACTIONS list:

String RAID_ACTIONS = """
    - hunt_captain: {"bad_omen_level": 1} (optional: bad_omen_level 1-5)
    - trigger_raid: {} (no parameters)
    - fight_raid: {"target": "raider"} (optional: target, targetUuid, position)
    - collect_loot: {} (no parameters)
    - collect_totems: {"quantity": 1} (optional: quantity)
    - build_raid_farm: {"design": "simple_tower"} (optional: design)
    - automated_raid: {"raids": 1, "squad_size": 0} (optional: raids, squad_size)
    """;

// Example prompts:

// Input: "farm totems"
/*
{
    "reasoning": "Need to trigger raids and kill evokers for Totems of Undying",
    "plan": "Execute automated raid cycle to collect totems",
    "tasks": [
        {"action": "automated_raid", "parameters": {"raids": 5}}
    ]
}
*/

// Input: "build raid farm"
/*
{
    "reasoning": "Construct automated raid farming infrastructure",
    "plan": "Build raid farm for automated farming",
    "tasks": [
        {"action": "build_raid_farm", "parameters": {"design": "simple_tower"}}
    ]
}
*/

// Input: "coordinate raid with squad"
/*
{
    "reasoning": "Multiple foremen can work together to clear raids faster",
    "plan": "Assemble squad and execute coordinated raid",
    "tasks": [
        {"action": "automated_raid", "parameters": {"raids": 3, "squad_size": 4}}
    ]
}
*/
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)

**Tasks:**
1. Create `BadOmenManager` class
2. Implement captain detection (`hasBanner()`)
3. Add village detection (`VillageDetector`)
4. Create raid event monitoring
5. Basic unit tests

**Deliverables:**
- Can detect Bad Omen status
- Can find raid captains
- Can detect villages
- Can monitor active raids

### Phase 2: Bad Omen Management (Week 1-2)

**Tasks:**
1. Implement `HuntCaptainAction`
2. Add captain pathfinding
3. Implement Bad Omen stacking
4. Add milk drinking for effect removal
5. Test captain hunting

**Deliverables:**
- Can hunt and kill captains
- Can stack Bad Omen to level V
- Can clear Bad Omen effect
- Action completes successfully

### Phase 3: Raid Triggering (Week 2)

**Tasks:**
1. Implement `TriggerRaidAction`
2. Add village navigation
3. Implement raid spawn monitoring
4. Add raid wave tracking
5. Test raid triggering

**Deliverables:**
- Can trigger raids consistently
- Tracks raid waves
- Detects raid completion
- Handles raid failures

### Phase 4: Combat & Loot Collection (Week 2-3)

**Tasks:**
1. Implement raid-specific combat logic
2. Add evoker targeting priority
3. Implement `CollectRaidLootAction`
4. Add looting weapon optimization
5. Implement totem collection

**Deliverables:**
- Prioritizes evokers in combat
- Collects loot efficiently
- Equips looting weapons
- Tracks totem counts

### Phase 5: Multi-Agent Coordination (Week 3-4)

**Tasks:**
1. Implement `RaidCoordinator`
2. Add squad assembly logic
3. Implement position assignment
4. Add target assignment system
5. Implement coordinated combat

**Deliverables:**
- Can assemble raid squads
- Assigns tactical positions
- Coordinates target engagement
- Optimizes loot collection zones

### Phase 6: Farm Construction (Week 4)

**Tasks:**
1. Implement `BuildRaidFarmAction`
2. Create design patterns
3. Add automated building logic
4. Implement village setup
5. Build collection systems

**Deliverables:**
- Builds simple raid towers
- Creates multi-agent arenas
- Sets up villages correctly
- Constructs loot collection

### Phase 7: Complete Automation (Week 5)

**Tasks:**
1. Implement `AutomatedRaidAction`
2. Create full raid cycle automation
3. Add raid repetition logic
4. Implement stat tracking
5. Add error recovery

**Deliverables:**
- Fully automated raid cycles
- Tracks totems/emeralds
- Handles failures gracefully
- Optimizes over time

### Phase 8: Plugin Integration (Week 5-6)

**Tasks:**
1. Create `RaidActionsPlugin`
2. Register all raid actions
3. Update `PromptBuilder`
4. Add raid memory to `ForemanMemory`
5. Create configuration options

**Deliverables:**
- All actions registered
- LLM can request raids
- Raid stats persist
- Configurable behavior

### Phase 9: Polish & Optimization (Week 6)

**Tasks:**
1. Add chat feedback
2. Performance optimization
3. Edge case handling
4. Comprehensive testing
5. Documentation

**Deliverables:**
- Smooth user experience
- Efficient execution
- Robust error handling
- Complete test coverage

---

## Configuration

Add to `config/minewright-common.toml`:

```toml
[raid_farming]
# Enable automated raid farming
enabled = true

# Default Bad Omen level to stack (1-5)
default_bad_omen_level = 1

# Maximum raids to run in automated sequence
max_raids_per_sequence = 10

# Squad size for coordinated raids (0 = solo)
default_squad_size = 0

# Whether to automatically collect loot
auto_collect_loot = true

# Whether to equip looting weapons
use_looting_weapons = true

# Prioritize evokers for totem farming
prioritize_evokers = true

# Minimum emeralds to trigger automated raid
min_emeral_threshold = 0

# Maximum time to spend per raid (in ticks)
max_raid_duration = 3600

[raid_farm_designs]
# Available farm designs
simple_tower_enabled = true
multi_agent_arena_enabled = true

# Default farm dimensions
simple_tower_radius = 16
simple_tower_height = 10

multi_agent_arena_size = 64

[raid_priorities]
# Mob targeting priority (higher = more important)
evoker_priority = 100
vindicator_priority = 80
witch_priority = 60
pillager_priority = 40
ravager_priority = 20

# Loot collection priority
totem_priority = 100
emerald_priority = 90
enchanted_book_priority = 70
saddle_priority = 50
other_priority = 10
```

---

## Performance Considerations

### Tick Rate Optimization

```java
// Only scan for captains every 5 seconds
private static final int CAPTAIN_SCAN_INTERVAL = 100;

// Only check village position every 10 seconds
private static final int VILLAGE_SCAN_INTERVAL = 200;

// Rate limit loot collection
private static final int LOOT_COLLECTION_INTERVAL = 20;
```

### Caching Strategy

```java
public class RaidCache {
    private static final Map<UUID, RaidInfo> raidCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 2 * 60 * 1000; // 2 minutes

    public static Optional<RaidInfo> getCachedRaid(ForemanEntity foreman) {
        RaidInfo cached = raidCache.get(foreman.getUUID());

        if (cached != null && !cached.isStale()) {
            return Optional.of(cached);
        }

        return Optional.empty();
    }

    public static void cacheRaid(ForemanEntity foreman, Raid raid) {
        RaidInfo info = new RaidInfo(raid);
        raidCache.put(foreman.getUUID(), info);
    }
}
```

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testBadOmenDetection() {
    ForemanEntity foreman = createTestForeman();
    foreman.addEffect(new MobEffectInstance(
        MobEffects.BAD_OMEN, 1000, 2
    ));

    assertEquals(3, BadOmenManager.getBadOmenLevel(foreman));
    assertTrue(BadOmenManager.hasBadOmen(foreman));
}

@Test
public void testCaptainDetection() {
    ForemanEntity foreman = spawnTestForeman();
    Raider captain = spawnCaptain(foreman.level(), foreman.blockPosition());

    Optional<Raider> found = BadOmenManager.findNearbyCaptain(foreman);

    assertTrue(found.isPresent());
    assertEquals(captain.getUUID(), found.get().getUUID());
}

@Test
public void testVillageDetection() {
    // Create test village
    ServerLevel level = createTestLevel();
    createVillage(level, new BlockPos(0, 64, 0), 5);

    ForemanEntity foreman = spawnTestForeman(level, new BlockPos(0, 64, 0));

    Optional<BlockPos> village = VillageDetector.findNearestVillage(foreman);

    assertTrue(village.isPresent());
}
```

### Integration Tests

```java
@Test
public void testCompleteRaidCycle() {
    ForemanEntity foreman = spawnTestForeman();

    // Setup: Give Bad Omen
    BadOmenManager.stackBadOmen(foreman, 1);

    // Create village nearby
    createVillage(foreman.level(), foreman.blockPosition().offset(20, 0, 0), 5);

    // Execute automated raid
    Task task = new Task("automated_raid", Map.of("raids", 1));
    AutomatedRaidAction action = new AutomatedRaidAction(foreman, task);

    action.start();

    // Wait for completion (with timeout)
    tickAction(action, 7200); // 6 minute timeout

    assertTrue(action.isComplete());
    assertTrue(action.getResult().isSuccess());
}
```

---

## Future Enhancements

1. **Raid Speedrunning**: Optimize for fastest raid completion times
2. **Hardcore Mode**: Permadeath challenge runs for totems
3. **Statistics Tracking**: Track raid win rate, totems/hour, emeralds/hour
4. **Auto-Enchanting**: Use loot to enchant gear for better raids
5. **Raid Village Builder**: Auto-build optimal raid villages
6. **Multi-Base Coordination**: Run multiple raid farms simultaneously
7. **Raid Prediction**: Predict raid wave composition for strategy
8. **Auto-Sell System**: Integrate with villager trading for profit
9. **Raid Replay**: Record and analyze raid performance
10. **Community Raids**: Coordinate with other players for mega-raids

---

## Conclusion

This raid farm automation system provides MineWright agents with comprehensive raid farming capabilities. The modular design allows for incremental implementation, starting with basic Bad Omen management and progressing to fully automated multi-agent raid operations.

Key benefits:

1. **Totem of Undying Farming**: Automated totem collection for immortality
2. **Emerald Generation**: Consistent emerald income from raid drops
3. **Multi-Agent Coordination**: Squad-based tactics for efficient raids
4. **Flexible Design**: Supports multiple farm designs and playstyles
5. **Complete Automation**: End-to-end raid cycles with minimal supervision

The system integrates seamlessly with MineWright's existing action framework, orchestration system, and LLM-driven task planning, enabling agents to autonomously engage in complex raid operations that provide valuable resources and enhance their capabilities.

By implementing this system, MineWright agents will be able to:

- Automatically acquire Bad Omen and trigger raids
- Coordinate multi-agent combat squads
- Farm Totems of Undying for immortality
- Generate emeralds through raid loot
- Build and optimize raid farms
- Execute complete automated raid cycles

This makes MineWright significantly more capable as an autonomous Minecraft AI companion.

---

**Sources:**
- [Minecraft Wiki - Bad Omen](https://minecraft.fandom.com/wiki/Bad_Omen)
- [Minecraft Wiki - Raid](https://minecraft.fandom.com/wiki/Raid)
- [Beebom - Minecraft Raids Explained](https://beebom.com/minecraft-raids-explained/)
- [GPORTAL - Bad Omen Guide](https://www.g-portail.com/en/blog/bad_omen-in-minecraft)
- [Pocket Gamer - Bad O Removal](https://www.pocketgamer.com/minecraft/bad-omen/)
