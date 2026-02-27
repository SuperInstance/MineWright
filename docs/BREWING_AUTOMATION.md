# Brewing Automation AI - Design Document

## Executive Summary

This document outlines the design and implementation strategy for an intelligent brewing automation system for MineWright (Forge 1.20.1). The system enables Foreman entities to automatically brew potions, manage brewing stands, optimize ingredient gathering, and select appropriate potions based on task requirements.

**Version:** 1.0
**Date:** 2026-02-27
**Status:** Design Phase

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Potion Recipe Knowledge System](#potion-recipe-knowledge-system)
3. [Brewing Stand Automation](#brewing-stand-automation)
4. [Ingredient Gathering Optimization](#ingredient-gathering-optimization)
5. [Potion Selection Logic](#potion-selection-logic)
6. [Splash/Lingering Tactics](#splashlingering-tactics)
7. [Integration Architecture](#integration-architecture)
8. [Implementation Roadmap](#implementation-roadmap)
9. [Code Examples](#code-examples)

---

## System Overview

### Design Goals

1. **Complete Recipe Coverage**: Support all vanilla Minecraft 1.20.1 brewing recipes
2. **Efficient Automation**: Multi-brewing-stand operation with parallel processing
3. **Intelligent Selection**: Choose potions based on task context and requirements
4. **Ingredient Optimization**: Minimize waste through smart ingredient management
5. **Tactical Integration**: Support splash and lingering potions for combat scenarios
6. **Scalability**: Handle large-scale brewing operations (potions farms)

### Core Components

```
BrewingAutomationSystem
├── PotionRecipeRegistry (recipe knowledge)
├── BrewingStandManager (automation control)
├── IngredientOptimizer (resource management)
├── PotionSelector (task-based selection)
├── TacticalBrewingSystem (splash/lingering logic)
└── BrewingActionPlugin (action registration)
```

### Supported Features

- **All vanilla potions**: Awkward, Thick, Mundane, and all effect potions
- **Enhancement variants**: Extended duration, amplified strength
- **Splash conversions**: Gunpowder-based splash potions
- **Lingering conversions**: Dragon's breath-based lingering potions
- **Multi-stand automation**: Up to 16 brewing stands simultaneously
- **Automatic fueling**: Blaze powder restocking
- **Ingredient tracking**: Real-time inventory management

---

## Potion Recipe Knowledge System

### Recipe Data Structure

```java
package com.minewright.brewing;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Represents a complete brewing recipe with all possible variants.
 */
public class PotionRecipe {
    private final String name;
    private final PotionType baseType;
    private final Item ingredient;
    private final MobEffectInstance effect;
    private final boolean canExtend;
    private final boolean canAmplify;
    private final RecipeTier tier;

    public enum PotionType {
        AWKWARD, THICK, MUNDANE,
        STRENGTH, SPEED, REGENERATION, HEALING, HARMING,
        POISON, NIGHT_VISION, INVISIBILITY, FIRE_RESISTANCE,
        WATER_BREATHING, SLOWNESS, LEAPING, TURTLE_MASTER,
        SLOW_FALLING
    }

    public enum RecipeTier {
        BASE,        // Water bottle + ingredient
        EFFECT,      // Base potion + ingredient
        EXTENDED,    // Effect + redstone
        AMPLIFIED,   // Effect + glowstone
        SPLASH,      // Any + gunpowder
        LINGERING    // Splash + dragon's breath
    }
}
```

### Complete Recipe Registry

```java
package com.minewright.brewing;

import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.*;

/**
 * Complete registry of all vanilla Minecraft 1.20.1 brewing recipes.
 * Includes base potions, effect potions, enhancements, and conversions.
 */
public class PotionRecipeRegistry {
    private static final Map<String, PotionRecipe> RECIPES = new HashMap<>();
    private static final Map<PotionRecipe.PotionType, List<PotionRecipe>> BY_TYPE = new HashMap<>();

    static {
        initializeBasePotions();
        initializeEffectPotions();
        initializeEnhancements();
        initializeConversions();
    }

    private static void initializeBasePotions() {
        // Water Bottle + Nether Wart = Awkward Potion (base for most potions)
        register(new PotionRecipe(
            "awkward",
            PotionRecipe.PotionType.AWKWARD,
            Items.NETHER_WART,
            null,
            false, false,
            PotionRecipe.RecipeTier.BASE
        ));

        // Water Bottle + Glowstone Dust = Thick Potion
        register(new PotionRecipe(
            "thick",
            PotionRecipe.PotionType.THICK,
            Items.GLOWSTONE_DUST,
            null,
            false, false,
            PotionRecipe.RecipeTier.BASE
        ));

        // Water Bottle + Redstone Dust = Mundane Potion
        register(new PotionRecipe(
            "mundane",
            PotionRecipe.PotionType.MUNDANE,
            Items.REDSTONE,
            null,
            false, false,
            PotionRecipe.RecipeTier.BASE
        ));
    }

    private static void initializeEffectPotions() {
        // POSITIVE EFFECTS

        // Awkward + Blaze Powder = Strength
        registerEffectRecipe("strength", Items.BLAZE_POWDER,
            new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0),
            true, true);

        // Awkward + Sugar = Speed
        registerEffectRecipe("speed", Items.SUGAR,
            new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0),
            true, true);

        // Awkward + Ghast Tear = Regeneration
        registerEffectRecipe("regeneration", Items.GHAST_TEAR,
            new MobEffectInstance(MobEffects.REGENERATION, 900, 0),
            true, true);

        // Awkward + Glistering Melon Slice = Healing
        registerEffectRecipe("healing", Items.GLISTERING_MELON_SLICE,
            new MobEffectInstance(MobEffects.HEAL, 1, 0),
            false, true);

        // Awkward + Spider Eye = Poison
        registerEffectRecipe("poison", Items.SPIDER_EYE,
            new MobEffectInstance(MobEffects.POISON, 900, 0),
            true, true);

        // Awkward + Golden Carrot = Night Vision
        registerEffectRecipe("night_vision", Items.GOLDEN_CARROT,
            new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0),
            true, false);

        // Awkward + Pufferfish = Water Breathing
        registerEffectRecipe("water_breathing", Items.PUFFERFISH,
            new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0),
            true, false);

        // Awkward + Magma Cream = Fire Resistance
        registerEffectRecipe("fire_resistance", Items.MAGMA_CREAM,
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0),
            true, false);

        // Awkward + Rabbit's Foot = Leaping (Jump Boost)
        registerEffectRecipe("leaping", Items.RABBITS_FOOT,
            new MobEffectInstance(MobEffects.JUMP, 3600, 0),
            true, true);

        // Awkward + Phantom Membrane = Slow Falling
        registerEffectRecipe("slow_falling", Items.PHANTOM_MEMBRANE,
            new MobEffectInstance(MobEffects.SLOW_FALLING, 1800, 0),
            true, false);

        // NEGATIVE EFFECTS

        // Water Bottle + Fermented Spider Eye = Weakness
        registerEffectRecipe("weakness", Items.FERMENTED_SPIDER_EYE,
            new MobEffectInstance(MobEffects.WEAKNESS, 1800, 0),
            true, false);

        // Awkward + Fermented Spider Eye = Harming (damage)
        registerEffectRecipe("harming", Items.FERMENTED_SPIDER_EYE,
            new MobEffectInstance(MobEffects.HARM, 1, 0),
            false, true);

        // CONVERSIONS (via Fermented Spider Eye)

        // Speed + Fermented Spider Eye = Slowness
        registerConversionRecipe("slowness", "speed", Items.FERMENTED_SPIDER_EYE,
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1800, 0),
            true, false);

        // Leaping + Fermented Spider Eye = Slowness
        registerConversionRecipe("slowness_leaping", "leaping", Items.FERMENTED_SPIDER_EYE,
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1800, 0),
            true, false);

        // Night Vision + Fermented Spider Eye = Invisibility
        registerConversionRecipe("invisibility", "night_vision", Items.FERMENTED_SPIDER_EYE,
            new MobEffectInstance(MobEffects.INVISIBILITY, 1800, 0),
            true, false);

        // Poison + Fermented Spider Eye = Harming
        registerConversionRecipe("harming_poison", "poison", Items.FERMENTED_SPIDER_EYE,
            new MobEffectInstance(MobEffects.HARM, 1, 0),
            false, true);

        // Fire Resistance + Fermented Spider Eye = Slowness
        registerConversionRecipe("slowness_fire", "fire_resistance", Items.FERMENTED_SPIDER_EYE,
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1800, 0),
            true, false);

        // Water Breathing + Fermented Spider Eye = Harming
        registerConversionRecipe("harming_water", "water_breathing", Items.FERMENTED_SPIDER_EYE,
            new MobEffectInstance(MobEffects.HARM, 1, 0),
            false, true);

        // Regeneration + Fermented Spider Eye = Wither (modded, not vanilla)
        // Vanilla doesn't have wither potion, but some servers add it
    }

    private static void initializeEnhancements() {
        // EXTENDED DURATION (Redstone)

        registerEnhancementRecipe("strength_extended", "strength", Items.REDSTONE,
            new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9600, 0));

        registerEnhancementRecipe("speed_extended", "speed", Items.REDSTONE,
            new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 9600, 0));

        registerEnhancementRecipe("regeneration_extended", "regeneration", Items.REDSTONE,
            new MobEffectInstance(MobEffects.REGENERATION, 1800, 0));

        registerEnhancementRecipe("poison_extended", "poison", Items.REDSTONE,
            new MobEffectInstance(MobEffects.POISON, 1800, 0));

        registerEnhancementRecipe("night_vision_extended", "night_vision", Items.REDSTONE,
            new MobEffectInstance(MobEffects.NIGHT_VISION, 9600, 0));

        registerEnhancementRecipe("water_breathing_extended", "water_breathing", Items.REDSTONE,
            new MobEffectInstance(MobEffects.WATER_BREATHING, 9600, 0));

        registerEnhancementRecipe("fire_resistance_extended", "fire_resistance", Items.REDSTONE,
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 9600, 0));

        registerEnhancementRecipe("leaping_extended", "leaping", Items.REDSTONE,
            new MobEffectInstance(MobEffects.JUMP, 9600, 0));

        registerEnhancementRecipe("slow_falling_extended", "slow_falling", Items.REDSTONE,
            new MobEffectInstance(MobEffects.SLOW_FALLING, 4800, 0));

        registerEnhancementRecipe("slowness_extended", "slowness", Items.REDSTONE,
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4800, 0));

        registerEnhancementRecipe("weakness_extended", "weakness", Items.REDSTONE,
            new MobEffectInstance(MobEffects.WEAKNESS, 4800, 0));

        registerEnhancementRecipe("invisibility_extended", "invisibility", Items.REDSTONE,
            new MobEffectInstance(MobEffects.INVISIBILITY, 4800, 0));

        // AMPLIFIED STRENGTH (Glowstone Dust)

        registerEnhancementRecipe("strength_strong", "strength", Items.GLOWSTONE_DUST,
            new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800, 1));

        registerEnhancementRecipe("speed_strong", "speed", Items.GLOWSTONE_DUST,
            new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 1));

        registerEnhancementRecipe("regeneration_strong", "regeneration", Items.GLOWSTONE_DUST,
            new MobEffectInstance(MobEffects.REGENERATION, 450, 1));

        registerEnhancementRecipe("healing_strong", "healing", Items.GLOWSTONE_DUST,
            new MobEffectInstance(MobEffects.HEAL, 1, 1));

        registerEnhancementRecipe("poison_strong", "poison", Items.GLOWSTONE_DUST,
            new MobEffectInstance(MobEffects.POISON, 450, 1));

        registerEnhancementRecipe("leaping_strong", "leaping", Items.GLOWSTONE_DUST,
            new MobEffectInstance(MobEffects.JUMP, 1800, 1));

        registerEnhancementRecipe("harming_strong", "harming", Items.GLOWSTONE_DUST,
            new MobEffectInstance(MobEffects.HARM, 1, 1));

        registerEnhancementRecipe("slowness_strong", "slowness", Items.GLOWSTONE_DUST,
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 1));
    }

    private static void initializeConversions() {
        // SPLASH POTIONS (Gunpowder)
        // All potions can be converted to splash versions

        List<String> allPotions = Arrays.asList(
            "strength", "speed", "regeneration", "healing", "harming",
            "poison", "night_vision", "invisibility", "fire_resistance",
            "water_breathing", "slowness", "leaping", "slow_falling",
            "weakness",
            "strength_extended", "speed_extended", "regeneration_extended",
            "poison_extended", "night_vision_extended", "water_breathing_extended",
            "fire_resistance_extended", "leaping_extended", "slow_falling_extended",
            "slowness_extended", "weakness_extended", "invisibility_extended",
            "strength_strong", "speed_strong", "regeneration_strong",
            "healing_strong", "poison_strong", "leaping_strong",
            "harming_strong", "slowness_strong"
        );

        for (String potion : allPotions) {
            registerSplashRecipe("splash_" + potion, potion);
        }

        // LINGERING POTIONS (Dragon's Breath)
        // Only splash potions can be converted to lingering

        for (String potion : allPotions) {
            registerLingeringRecipe("lingering_" + potion, "splash_" + potion);
        }
    }

    // Helper methods for recipe registration

    private static void register(PotionRecipe recipe) {
        RECIPES.put(recipe.getName(), recipe);
        BY_TYPE.computeIfAbsent(recipe.getBaseType(), k -> new ArrayList<>()).add(recipe);
    }

    private static void registerEffectRecipe(String name, Item ingredient,
            MobEffectInstance effect, boolean canExtend, boolean canAmplify) {
        register(new PotionRecipe(name, PotionRecipe.PotionType.valueOf(name.toUpperCase()),
            ingredient, effect, canExtend, canAmplify, PotionRecipe.RecipeTier.EFFECT));
    }

    private static void registerConversionRecipe(String name, String fromPotion,
            Item ingredient, MobEffectInstance effect, boolean canExtend, boolean canAmplify) {
        register(new PotionRecipe(name, PotionRecipe.PotionType.valueOf(name.split("_")[0].toUpperCase()),
            ingredient, effect, canExtend, canAmplify, PotionRecipe.RecipeTier.EFFECT));
    }

    private static void registerEnhancementRecipe(String name, String fromPotion,
            Item ingredient, MobEffectInstance effect) {
        PotionRecipe.PotionType type = PotionRecipe.PotionType.valueOf(
            fromPotion.replace("_extended", "").replace("_strong", "").toUpperCase());
        register(new PotionRecipe(name, type, ingredient, effect,
            false, false, PotionRecipe.RecipeTier.EXTENDED));
    }

    private static void registerSplashRecipe(String name, String fromPotion) {
        PotionRecipe base = RECIPES.get(fromPotion);
        register(new PotionRecipe(name, base.getBaseType(), Items.GUNPOWDER,
            base.getEffect(), false, false, PotionRecipe.RecipeTier.SPLASH));
    }

    private static void registerLingeringRecipe(String name, String fromPotion) {
        PotionRecipe splash = RECIPES.get(fromPotion);
        register(new PotionRecipe(name, splash.getBaseType(), Items.DRAGON_BREATH,
            splash.getEffect(), false, false, PotionRecipe.RecipeTier.LINGERING));
    }

    // Public API

    public static Optional<PotionRecipe> getRecipe(String name) {
        return Optional.ofNullable(RECIPES.get(name));
    }

    public static List<PotionRecipe> getRecipesByType(PotionRecipe.PotionType type) {
        return BY_TYPE.getOrDefault(type, Collections.emptyList());
    }

    public static List<PotionRecipe> getAllRecipes() {
        return new ArrayList<>(RECIPES.values());
    }

    public static List<PotionRecipe> getCombatPotions() {
        return Arrays.asList(
            RECIPES.get("strength"),
            RECIPES.get("strength_strong"),
            RECIPES.get("speed"),
            RECIPES.get("speed_strong"),
            RECIPES.get("healing"),
            RECIPES.get("healing_strong"),
            RECIPES.get("harming"),
            RECIPES.get("harming_strong"),
            RECIPES.get("poison"),
            RECIPES.get("poison_strong")
        );
    }

    public static List<PotionRecipe> getUtilityPotions() {
        return Arrays.asList(
            RECIPES.get("fire_resistance"),
            RECIPES.get("night_vision"),
            RECIPES.get("invisibility"),
            RECIPES.get("water_breathing"),
            RECIPES.get("slow_falling")
        );
    }
}
```

---

## Brewing Stand Automation

### Multi-Stand Management

```java
package com.minewright.brewing;

import com.minewright.entity.ForemanEntity;
import com.minewright.action.ActionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple brewing stands for parallel potion brewing.
 * Handles automatic ingredient loading, fuel management, and product collection.
 */
public class BrewingStandManager {
    private final ForemanEntity foreman;
    private final Map<BlockPos, BrewingStandController> activeStands;
    private final Queue<BrewingJob> jobQueue;
    private int maxConcurrentStands;

    public BrewingStandManager(ForemanEntity foreman, int maxConcurrentStands) {
        this.foreman = foreman;
        this.maxConcurrentStands = maxConcurrentStands;
        this.activeStands = new ConcurrentHashMap<>();
        this.jobQueue = new LinkedList<>();
    }

    /**
     * Add a brewing job to the queue.
     * Jobs will be assigned to available brewing stands automatically.
     */
    public void queueJob(BrewingJob job) {
        jobQueue.add(job);
        assignJobsToStands();
    }

    /**
     * Update all active brewing stands. Call this every tick.
     */
    public void tick() {
        // Update each active stand
        activeStands.entrySet().removeIf(entry -> {
            BrewingStandController controller = entry.getValue();
            controller.tick();

            // Remove completed stands
            if (controller.isComplete()) {
                collectResults(entry.getKey());
                return true;
            }
            return false;
        });

        // Assign new jobs to available stands
        assignJobsToStands();
    }

    /**
     * Scan nearby area for brewing stands and register them.
     */
    public void discoverBrewingStands(int searchRadius) {
        BlockPos center = foreman.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-searchRadius, -searchRadius, -searchRadius),
            center.offset(searchRadius, searchRadius, searchRadius)
        )) {
            BlockState state = foreman.level().getBlockState(pos);
            if (state.is(Blocks.BREWING_STAND)) {
                registerStand(pos);
            }
        }
    }

    /**
     * Register a brewing stand at the given position.
     */
    public void registerStand(BlockPos pos) {
        if (!activeStands.containsKey(pos)) {
            activeStands.put(pos, new BrewingStandController(foreman, pos));
        }
    }

    /**
     * Assign queued jobs to available brewing stands.
     */
    private void assignJobsToStands() {
        // Find idle stands
        List<BrewingStandController> idleStands = activeStands.values().stream()
            .filter(controller -> controller.isIdle())
            .limit(maxConcurrentStands - activeStands.values().stream()
                .filter(controller -> !controller.isIdle()).count())
            .toList();

        // Assign jobs to idle stands
        for (BrewingStandController stand : idleStands) {
            BrewingJob job = jobQueue.poll();
            if (job != null) {
                stand.startJob(job);
            } else {
                break;
            }
        }
    }

    /**
     * Collect brewed potions from a completed brewing stand.
     */
    private void collectResults(BlockPos standPos) {
        BrewingStandController controller = activeStands.get(standPos);
        if (controller != null) {
            List<ItemStack> results = controller.collectResults();
            // Add to inventory or storage
            storePotions(results);
        }
    }

    /**
     * Store potions in inventory or nearby chests.
     */
    private void storePotions(List<ItemStack> potions) {
        // Implementation depends on inventory system
        // For now, just log the collection
        for (ItemStack potion : potions) {
            foreman.getInventory().add(potion);
        }
    }

    public int getActiveStandCount() {
        return (int) activeStands.values().stream().filter(c -> !c.isIdle()).count();
    }

    public int getQueuedJobCount() {
        return jobQueue.size();
    }
}
```

### Brewing Stand Controller

```java
package com.minewright.brewing;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/**
 * Controls a single brewing stand's operation.
 * Handles fuel management, ingredient loading, and brewing progress.
 */
public class BrewingStandController {
    private final ForemanEntity foreman;
    private final BlockPos standPos;
    private BrewingStandBlockEntity brewingStand;
    private BrewingJob currentJob;
    private int brewingProgress;
    private boolean isComplete;

    public BrewingStandController(ForemanEntity foreman, BlockPos standPos) {
        this.foreman = foreman;
        this.standPos = standPos;
        this.brewingProgress = 0;
        this.isComplete = false;

        // Get the brewing stand block entity
        if (foreman.level().getBlockEntity(standPos) instanceof BrewingStandBlockEntity bs) {
            this.brewingStand = bs;
        }
    }

    /**
     * Start a brewing job on this stand.
     */
    public void startJob(BrewingJob job) {
        this.currentJob = job;
        this.isComplete = false;
        this.brewingProgress = 0;

        // Load ingredients into the stand
        loadIngredients();

        // Ensure fuel is available
        ensureFuel();
    }

    /**
     * Update brewing progress. Call every tick.
     */
    public void tick() {
        if (currentJob == null || isComplete) {
            return;
        }

        if (brewingStand != null) {
            brewingProgress = brewingStand.brewingTime;

            if (brewingProgress >= 400) { // Brewing complete (400 ticks = 20 seconds)
                isComplete = true;
            }
        }
    }

    /**
     * Load potion bottles and ingredients into the stand.
     */
    private void loadIngredients() {
        if (brewingStand == null) return;

        // Load water bottles or base potions
        for (int i = 0; i < 3; i++) {
            ItemStack potion = currentJob.getInputPotion();
            brewingStand.setItem(i, potion);
        }

        // Load ingredient
        brewingStand.setItem(3, new ItemStack(currentJob.getIngredient(), 1));
    }

    /**
     * Ensure the brewing stand has fuel (blaze powder).
     */
    private void ensureFuel() {
        if (brewingStand == null) return;

        // Check fuel level
        if (brewingStand.fuel <= 0) {
            // Add blaze powder
            ItemStack fuel = new ItemStack(Items.BLAZE_POWDER);
            brewingStand.setItem(4, fuel);
        }
    }

    /**
     * Collect the brewed potions from the stand.
     */
    public List<ItemStack> collectResults() {
        List<ItemStack> results = new ArrayList<>();

        if (brewingStand != null) {
            // Collect potions from slots 0-2
            for (int i = 0; i < 3; i++) {
                ItemStack potion = brewingStand.getItem(i);
                if (!potion.isEmpty()) {
                    results.add(potion.copy());
                    brewingStand.setItem(i, ItemStack.EMPTY);
                }
            }
        }

        // Reset for next job
        currentJob = null;
        isComplete = false;
        brewingProgress = 0;

        return results;
    }

    public boolean isIdle() {
        return currentJob == null || isComplete;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public BlockPos getStandPos() {
        return standPos;
    }
}
```

### Brewing Job Structure

```java
package com.minewright.brewing;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Represents a single brewing job (batch of 3 potions).
 */
public class BrewingJob {
    private final PotionRecipe recipe;
    private final ItemStack inputPotion;
    private final Item ingredient;
    private final int quantity; // Number of batches (3 potions per batch)

    public BrewingJob(PotionRecipe recipe, ItemStack inputPotion, int quantity) {
        this.recipe = recipe;
        this.inputPotion = inputPotion;
        this.ingredient = recipe.getIngredient();
        this.quantity = quantity;
    }

    public ItemStack getInputPotion() {
        return inputPotion.copy();
    }

    public Item getIngredient() {
        return ingredient;
    }

    public PotionRecipe getRecipe() {
        return recipe;
    }

    public int getQuantity() {
        return quantity;
    }

    /**
     * Create a job from water bottles.
     */
    public static BrewingJob fromWaterBottles(PotionRecipe recipe, int batches) {
        ItemStack waterBottle = new ItemStack(Items.POTION);
        // Set to water bottle variant
        return new BrewingJob(recipe, waterBottle, batches);
    }

    /**
     * Create a job from existing potions (for enhancements).
     */
    public static BrewingJob fromPotions(PotionRecipe recipe, ItemStack basePotion, int batches) {
        return new BrewingJob(recipe, basePotion, batches);
    }
}
```

---

## Ingredient Gathering Optimization

### Ingredient Requirements Calculator

```java
package com.minewright.brewing;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Calculates and optimizes ingredient requirements for brewing operations.
 */
public class IngredientRequirementsCalculator {

    public static class IngredientRequirement {
        private final Item item;
        private final int quantity;
        private final String description;

        public IngredientRequirement(Item item, int quantity, String description) {
            this.item = item;
            this.quantity = quantity;
            this.description = description;
        }

        public Item getItem() { return item; }
        public int getQuantity() { return quantity; }
        public String getDescription() { return description; }
    }

    /**
     * Calculate total ingredients needed for a list of brewing jobs.
     */
    public static List<IngredientRequirement> calculateRequirements(List<BrewingJob> jobs) {
        Map<Item, Integer> totals = new HashMap<>();

        for (BrewingJob job : jobs) {
            // Count main ingredient
            Item ingredient = job.getIngredient();
            totals.merge(ingredient, job.getQuantity(), Integer::sum);

            // Count input potions (water bottles or base potions)
            ItemStack input = job.getInputPotion();
            totals.merge(input.getItem(), job.getQuantity() * 3, Integer::sum);
        }

        // Add fuel requirements (blaze powder)
        // 1 blaze powder = 20 batches
        int totalBatches = jobs.stream().mapToInt(BrewingJob::getQuantity).sum();
        int blazePowderNeeded = (totalBatches + 19) / 20; // Round up
        totals.put(Items.BLAZE_POWDER, blazePowderNeeded);

        // Convert to requirements list with descriptions
        List<IngredientRequirement> requirements = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : totals.entrySet()) {
            String description = getDescriptionForItem(entry.getKey());
            requirements.add(new IngredientRequirement(entry.getKey(), entry.getValue(), description));
        }

        // Sort by quantity (largest first for prioritized gathering)
        requirements.sort((a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()));

        return requirements;
    }

    /**
     * Optimize gathering order based on proximity and scarcity.
     */
    public static List<IngredientRequirement> optimizeGatheringOrder(
            List<IngredientRequirement> requirements,
            Map<Item, Integer> availableIngredients) {

        List<IngredientRequirement> optimized = new ArrayList<>();

        // Separate into available and missing
        List<IngredientRequirement> available = new ArrayList<>();
        List<IngredientRequirement> missing = new ArrayList<>();

        for (IngredientRequirement req : requirements) {
            int availableQty = availableIngredients.getOrDefault(req.getItem(), 0);
            if (availableQty >= req.getQuantity()) {
                // We have enough
                available.add(req);
            } else if (availableQty > 0) {
                // We have some but not enough
                missing.add(req);
            } else {
                // We have none
                missing.add(req);
            }
        }

        // Prioritize missing ingredients by rarity
        missing.sort((a, b) -> {
            int rarityA = getRarity(a.getItem());
            int rarityB = getRarity(b.getItem());
            return Integer.compare(rarityA, rarityB); // Lower rarity = more urgent
        });

        optimized.addAll(missing);
        optimized.addAll(available);

        return optimized;
    }

    private static String getDescriptionForItem(Item item) {
        if (item == Items.NETHER_WART) return "Nether Wart (base for all potions)";
        if (item == Items.BLAZE_POWDER) return "Blaze Powder (brewing fuel + Strength)";
        if (item == Items.SUGAR) return "Sugar (Speed)";
        if (item == Items.RABBITS_FOOT) return "Rabbit's Foot (Leaping)";
        if (item == Items.GLISTERING_MELON_SLICE) return "Glistering Melon (Healing)";
        if (item == Items.SPIDER_EYE) return "Spider Eye (Poison)";
        if (item == Items.GHAST_TEAR) return "Ghast Tear (Regeneration)";
        if (item == Items.MAGMA_CREAM) return "Magma Cream (Fire Resistance)";
        if (item == Items.PUFFERFISH) return "Pufferfish (Water Breathing)";
        if (item == Items.GOLDEN_CARROT) return "Golden Carrot (Night Vision)";
        if (item == Items.PHANTOM_MEMBRANE) return "Phantom Membrane (Slow Falling)";
        if (item == Items.FERMENTED_SPIDER_EYE) return "Fermented Spider Eye (conversion)";
        if (item == Items.REDSTONE) return "Redstone (extend duration)";
        if (item == Items.GLOWSTONE_DUST) return "Glowstone Dust (amplify effect)";
        if (item == Items.GUNPOWDER) return "Gunpowder (splash potions)";
        if (item == Items.DRAGON_BREATH) return "Dragon's Breath (lingering potions)";
        return item.getDescription().getString();
    }

    /**
     * Get rarity score for ingredient (lower = rarer = higher priority).
     */
    private static int getRarity(Item item) {
        // Very rare (requires Nether fortress or End)
        if (item == Items.BLAZE_POWDER || item == Items.GHAST_TEAR ||
            item == Items.DRAGON_BREATH || item == Items.MAGMA_CREAM) {
            return 1;
        }

        // Rare (requires Nether)
        if (item == Items.NETHER_WART || item == Items.MAGMA_CREAM ||
            item == Items.GLOWSTONE_DUST) {
            return 2;
        }

        // Uncommon (specific mob drops or farming)
        if (item == Items.RABBITS_FOOT || item == Items.PUFFERFISH ||
            item == Items.PHANTOM_MEMBRANE || item == Items.GLISTERING_MELON_SLICE ||
            item == Items.GOLDEN_CARROT || item == Items.FERMENTED_SPIDER_EYE) {
            return 3;
        }

        // Common (easily obtainable)
        if (item == Items.SUGAR || item == Items.SPIDER_EYE ||
            item == Items.REDSTONE || item == Items.GUNPOWDER) {
            return 4;
        }

        return 5; // Unknown
    }
}
```

### Automated Ingredient Gathering

```java
package com.minewright.brewing;

import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Coordinates gathering of brewing ingredients from multiple sources.
 */
public class IngredientGatherer {
    private final ForemanEntity foreman;

    public IngredientGatherer(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Create tasks to gather missing ingredients.
     */
    public void gatherIngredients(List<IngredientRequirementsCalculator.IngredientRequirement> requirements) {
        for (IngredientRequirementsCalculator.IngredientRequirement req : requirements) {
            createGatheringTask(req);
        }
    }

    private void createGatheringTask(IngredientRequirementsCalculator.IngredientRequirement req) {
        Item item = req.getItem();
        int quantity = req.getQuantity();

        // Determine best gathering strategy based on item type
        String gatheringAction = determineGatheringAction(item);

        // Create and queue the task
        Task task = new Task(gatheringAction, Map.of(
            "item", item.toString(),
            "quantity", quantity,
            "description", req.getDescription()
        ));

        // Add to foreman's task queue
        foreman.getActionExecutor().queueTask(task);
    }

    private String determineGatheringAction(Item item) {
        // Determine the best action to gather this item
        // This would integrate with the existing gather/mine/craft actions

        if (item.toString().contains("nether_wart")) {
            return "farm"; // Nether wart farming
        } else if (item.toString().contains("blaze_powder")) {
            return "hunt"; // Blazes in Nether fortress
        } else if (item.toString().contains("sugar")) {
            return "craft"; // From sugar cane
        } else if (item.toString().contains("glistering_melon")) {
            return "craft"; // From melon and gold nuggets
        } else {
            return "gather"; // General gathering
        }
    }
}
```

---

## Potion Selection Logic

### Task-Based Potion Selection

```java
package com.minewright.brewing;

import com.minewright.action.Task;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.*;

/**
 * Selects appropriate potions based on task requirements and context.
 */
public class PotionSelector {

    public static class PotionRecommendation {
        private final PotionRecipe recipe;
        private final int quantity;
        private final String reason;
        private final int priority;

        public PotionRecommendation(PotionRecipe recipe, int quantity, String reason, int priority) {
            this.recipe = recipe;
            this.quantity = quantity;
            this.reason = reason;
            this.priority = priority;
        }

        public PotionRecipe getRecipe() { return recipe; }
        public int getQuantity() { return quantity; }
        public String getReason() { return reason; }
        public int getPriority() { return priority; }
    }

    /**
     * Recommend potions based on task type.
     */
    public static List<PotionRecommendation> recommendForTask(Task task) {
        String actionType = task.getActionType();
        List<PotionRecommendation> recommendations = new ArrayList<>();

        switch (actionType) {
            case "attack":
            case "combat":
                recommendations.addAll(getCombatPotions(task));
                break;

            case "mine":
            case "gather":
                recommendations.addAll(getMiningPotions(task));
                break;

            case "build":
            case "construct":
                recommendations.addAll(getConstructionPotions(task));
                break;

            case "explore":
            case "travel":
                recommendations.addAll(getExplorationPotions(task));
                break;

            case "nether":
                recommendations.addAll(getNetherPotions(task));
                break;

            case "underwater":
                recommendations.addAll(getUnderwaterPotions(task));
                break;

            case "raid":
                recommendations.addAll(getRaidPotions(task));
                break;

            default:
                // Generic recommendations
                recommendations.addAll(getGenericPotions(task));
                break;
        }

        // Sort by priority
        recommendations.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        return recommendations;
    }

    private static List<PotionRecommendation> getCombatPotions(Task task) {
        List<PotionRecommendation> potions = new ArrayList<>();

        // Strength II - essential for combat
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("strength_strong").get(),
            2, "Maximum damage output", 100
        ));

        // Speed II - for strafing and positioning
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("speed_strong").get(),
            2, "Improved mobility", 90
        ));

        // Healing II - emergency healing
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("splash_healing_strong").get(),
            4, "Instant healing during combat", 95
        ));

        // Fire Resistance - if enemy uses fire
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("fire_resistance_extended").get(),
            1, "Protection from fire attacks", 70
        ));

        return potions;
    }

    private static List<PotionRecommendation> getMiningPotions(Task task) {
        List<PotionRecommendation> potions = new ArrayList<>();

        // Night Vision - for cave mining
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("night_vision_extended").get(),
            1, "See in dark caves", 80
        ));

        // Fire Resistance - for lava mining
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("fire_resistance_extended").get(),
            1, "Lava protection", 90
        ));

        // Speed - faster travel to mining sites
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("speed_extended").get(),
            1, "Faster mining travel", 70
        ));

        // Harming splash - for clearing mobs
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("splash_harming_strong").get(),
            2, "Clear hostile mobs", 60
        ));

        return potions;
    }

    private static List<PotionRecommendation> getConstructionPotions(Task task) {
        List<PotionRecommendation> potions = new ArrayList<>();

        // Leaping - reach high places
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("leaping_extended").get(),
            1, "Reach high blocks", 80
        ));

        // Speed - faster material transport
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("speed_extended").get(),
            1, "Faster material movement", 70
        ));

        // Night Vision - work in dark conditions
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("night_vision_extended").get(),
            1, "Work in any lighting", 60
        ));

        return potions;
    }

    private static List<PotionRecommendation> getExplorationPotions(Task task) {
        List<PotionRecommendation> potions = new ArrayList<>();

        // Night Vision - see in dark
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("night_vision_extended").get(),
            2, "Extended night vision", 90
        ));

        // Speed - faster travel
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("speed_extended").get(),
            2, "Faster exploration", 85
        ));

        // Slow Falling - safe descent from cliffs
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("slow_falling_extended").get(),
            2, "Safe descent from heights", 80
        ));

        // Fire Resistance - for lava lakes
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("fire_resistance_extended").get(),
            1, "Lava protection", 70
        ));

        return potions;
    }

    private static List<PotionRecommendation> getNetherPotions(Task task) {
        List<PotionRecommendation> potions = new ArrayList<>();

        // Fire Resistance - ESSENTIAL in Nether
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("fire_resistance_extended").get(),
            2, "Essential lava protection", 100
        ));

        // Speed - avoid ghast fireballs
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("speed_extended").get(),
            2, "Dodge fireballs", 90
        ));

        // Strength - fight blazes and wither skeletons
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("strength_extended").get(),
            2, "Combat Nether mobs", 85
        ));

        // Healing - for wither skeleton attacks
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("healing_strong").get(),
            4, "Wither skeleton protection", 80
        ));

        return potions;
    }

    private static List<PotionRecommendation> getUnderwaterPotions(Task task) {
        List<PotionRecommendation> potions = new ArrayList<>();

        // Water Breathing - ESSENTIAL
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("water_breathing_extended").get(),
            2, "Extended underwater time", 100
        ));

        // Night Vision - see underwater
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("night_vision_extended").get(),
            1, "Underwater vision", 90
        ));

        // Speed - faster swimming
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("speed_extended").get(),
            1, "Faster swimming", 70
        ));

        return potions;
    }

    private static List<PotionRecommendation> getRaidPotions(Task task) {
        List<PotionRecommendation> potions = new ArrayList<>();

        // Strength II - maximum damage
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("strength_strong").get(),
            4, "Maximum raid damage", 100
        ));

        // Speed II - raid mobility
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("speed_strong").get(),
            4, "Raid positioning", 95
        ));

        // Regeneration II - sustain during raid
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("regeneration_strong").get(),
            2, "Raid sustain", 90
        ));

        // Splash Healing - emergency healing
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("splash_healing_strong").get(),
            8, "Emergency raid healing", 95
        ));

        // Splash Poison - weaken ravagers
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("splash_poison_strong").get(),
            4, "Weaken tough mobs", 70
        ));

        return potions;
    }

    private static List<PotionRecommendation> getGenericPotions(Task task) {
        List<PotionRecommendation> potions = new ArrayList<>();

        // Basic utility potions
        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("speed").get(),
            1, "General mobility", 50
        ));

        potions.add(new PotionRecommendation(
            PotionRecipeRegistry.getRecipe("night_vision").get(),
            1, "Dark vision", 40
        ));

        return potions;
    }

    /**
     * Recommend potions based on environmental conditions.
     */
    public static List<PotionRecommendation> recommendForEnvironment(
            String biome, boolean isNight, boolean isRaining, boolean isThundering) {

        List<PotionRecommendation> recommendations = new ArrayList<>();

        // Night time
        if (isNight) {
            recommendations.add(new PotionRecommendation(
                PotionRecipeRegistry.getRecipe("night_vision_extended").get(),
                1, "Night vision", 70
            ));
        }

        // Thunder (lightning danger)
        if (isThundering) {
            recommendations.add(new PotionRecommendation(
                PotionRecipeRegistry.getRecipe("fire_resistance_extended").get(),
                1, "Lightning protection", 90
            ));
        }

        // Cold biomes
        if (biome.contains("snow") || biome.contains("ice")) {
            recommendations.add(new PotionRecommendation(
                PotionRecipeRegistry.getRecipe("fire_resistance").get(),
                1, "Cold protection", 60
            ));
        }

        // Hot biomes (desert, nether)
        if (biome.contains("desert") || biome.contains("nether") || biome.contains("basalt")) {
            recommendations.add(new PotionRecommendation(
                PotionRecipeRegistry.getRecipe("fire_resistance_extended").get(),
                1, "Heat protection", 85
            ));
        }

        return recommendations;
    }
}
```

---

## Splash/Lingering Tactics

### Tactical Brewing System

```java
package com.minewright.brewing;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Manages tactical use of splash and lingering potions for combat scenarios.
 */
public class TacticalBrewingSystem {
    private final ForemanEntity foreman;

    // Tactical scenarios
    public enum TacticalScenario {
        AREA_DENIAL,      // Create no-go zones with lingering potions
        CROWD_CONTROL,    // Affect multiple enemies
        BUFF_ALLIES,      // Beneficial effects for team
        EMERGENCY_HEAL,   // Quick healing in combat
        ESCAPE,           // Create escape opportunities
        AMBUSH,           // Surprise attacks
        SIEGE,            // Extended combat scenarios
        BOSS_FIGHT        // High-stakes single combat
    }

    public TacticalBrewingSystem(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    /**
     * Get tactical potion loadout for scenario.
     */
    public List<PotionRecipe> getTacticalLoadout(TacticalScenario scenario) {
        return switch (scenario) {
            case AREA_DENIAL -> getAreaDenialLoadout();
            case CROWD_CONTROL -> getCrowdControlLoadout();
            case BUFF_ALLIES -> getBuffAlliesLoadout();
            case EMERGENCY_HEAL -> getEmergencyHealLoadout();
            case ESCAPE -> getEscapeLoadout();
            case AMBUSH -> getAmbushLoadout();
            case SIEGE -> getSiegeLoadout();
            case BOSS_FIGHT -> getBossFightLoadout();
        };
    }

    private List<PotionRecipe> getAreaDenialLoadout() {
        return Arrays.asList(
            // Lingering Harming - creates damage zone
            PotionRecipeRegistry.getRecipe("lingering_harming_strong").get(),
            // Lingering Poison - sustained damage
            PotionRecipeRegistry.getRecipe("lingering_poison_strong").get(),
            // Lingering Slowness - movement denial
            PotionRecipeRegistry.getRecipe("lingering_slowness_strong").get(),
            // Lingering Weakness - combat debuff
            PotionRecipeRegistry.getRecipe("lingering_weakness_extended").get()
        );
    }

    private List<PotionRecipe> getCrowdControlLoadout() {
        return Arrays.asList(
            // Splash Harming - damage multiple enemies
            PotionRecipeRegistry.getRecipe("splash_harming_strong").get(),
            // Splash Poison - affect group
            PotionRecipeRegistry.getRecipe("splash_poison_strong").get(),
            // Splash Slowness - reduce mobility
            PotionRecipeRegistry.getRecipe("splash_slowness_strong").get(),
            // Splash Weakness - reduce damage
            PotionRecipeRegistry.getRecipe("splash_weakness_extended").get()
        );
    }

    private List<PotionRecipe> getBuffAlliesLoadout() {
        return Arrays.asList(
            // Splash Strength - buff team damage
            PotionRecipeRegistry.getRecipe("splash_strength_extended").get(),
            // Splash Speed - team mobility
            PotionRecipeRegistry.getRecipe("splash_speed_extended").get(),
            // Splash Regeneration - team sustain
            PotionRecipeRegistry.getRecipe("splash_regeneration_strong").get(),
            // Splash Healing - emergency team heal
            PotionRecipeRegistry.getRecipe("splash_healing_strong").get()
        );
    }

    private List<PotionRecipe> getEmergencyHealLoadout() {
        return Arrays.asList(
            // Splash Healing II - maximum healing
            PotionRecipeRegistry.getRecipe("splash_healing_strong").get(),
            // Regeneration - sustained healing
            PotionRecipeRegistry.getRecipe("regeneration_strong").get(),
            // Absorption (if modded) - damage buffer
            // Vanilla doesn't have absorption potion
            PotionRecipeRegistry.getRecipe("healing_strong").get() // Drinking healing
        );
    }

    private List<PotionRecipe> getEscapeLoadout() {
        return Arrays.asList(
            // Splash Speed II - maximum speed
            PotionRecipeRegistry.getRecipe("splash_speed_strong").get(),
            // Splash Slowness - slow pursuers
            PotionRecipeRegistry.getRecipe("splash_slowness_strong").get(),
            // Splash Poison - discourage pursuit
            PotionRecipeRegistry.getRecipe("splash_poison_strong").get(),
            // Invisibility - hide from pursuers
            PotionRecipeRegistry.getRecipe("invisibility_extended").get(),
            // Leaping - jump over obstacles
            PotionRecipeRegistry.getRecipe("leaping_strong").get()
        );
    }

    private List<PotionRecipe> getAmbushLoadout() {
        return Arrays.asList(
            // Invisibility - surprise element
            PotionRecipeRegistry.getRecipe("invisibility_extended").get(),
            // Strength II - burst damage
            PotionRecipeRegistry.getRecipe("strength_strong").get(),
            // Speed II - positioning
            PotionRecipeRegistry.getRecipe("speed_strong").get(),
            // Splash Harming - initial attack
            PotionRecipeRegistry.getRecipe("splash_harming_strong").get()
        );
    }

    private List<PotionRecipe> getSiegeLoadout() {
        return Arrays.asList(
            // Lingering Harming - zone control
            PotionRecipeRegistry.getRecipe("lingering_harming_strong").get(),
            // Splash Strength - offensive bursts
            PotionRecipeRegistry.getRecipe("splash_strength_extended").get(),
            // Regeneration - long sustain
            PotionRecipeRegistry.getRecipe("regeneration_extended").get(),
            // Fire Resistance - defense
            PotionRecipeRegistry.getRecipe("fire_resistance_extended").get(),
            // Night Vision - extended visibility
            PotionRecipeRegistry.getRecipe("night_vision_extended").get()
        );
    }

    private List<PotionRecipe> getBossFightLoadout() {
        return Arrays.asList(
            // Strength II - maximum damage
            PotionRecipeRegistry.getRecipe("strength_strong").get(),
            // Speed II - dodge attacks
            PotionRecipeRegistry.getRecipe("speed_strong").get(),
            // Regeneration II - sustain
            PotionRecipeRegistry.getRecipe("regeneration_strong").get(),
            // Fire Resistance - boss fire attacks
            PotionRecipeRegistry.getRecipe("fire_resistance_extended").get(),
            // Healing II - emergency heal
            PotionRecipeRegistry.getRecipe("healing_strong").get(),
            // Slow Falling - avoid knockback
            PotionRecipeRegistry.getRecipe("slow_falling_extended").get()
        );
    }

    /**
     * Analyze combat situation and recommend tactical approach.
     */
    public TacticalScenario analyzeSituation() {
        BlockPos pos = foreman.blockPosition();
        int nearbyEnemies = countNearbyEnemies(16);
        int nearbyAllies = countNearbyAllies(16);

        if (nearbyAllies >= 3) {
            return TacticalScenario.BUFF_ALLIES;
        } else if (nearbyEnemies >= 5) {
            return TacticalScenario.CROWD_CONTROL;
        } else if (nearbyEnemies >= 1) {
            return TacticalScenario.BOSS_FIGHT;
        } else if (foreman.getHealth() < 10) {
            return TacticalScenario.EMERGENCY_HEAL;
        } else {
            return TacticalScenario.AMBUSH;
        }
    }

    /**
     * Calculate optimal splash potion throwing position.
     */
    public BlockPos calculateSplashPosition(List<? extends Entity> targets) {
        if (targets.isEmpty()) {
            return null;
        }

        // Calculate centroid of targets
        double sumX = 0, sumY = 0, sumZ = 0;
        for (Entity target : targets) {
            sumX += target.getX();
            sumY += target.getY();
            sumZ += target.getZ();
        }

        double centerX = sumX / targets.size();
        double centerY = sumY / targets.size();
        double centerZ = sumZ / targets.size();

        // Adjust for eye level (splash potions affect entities at eye level)
        centerY += 1.6;

        return new BlockPos((int)centerX, (int)centerY, (int)centerZ);
    }

    /**
     * Calculate lingering potion cloud position for area denial.
     */
    public BlockPos calculateLingeringPosition(BlockPos areaCenter, int radius) {
        // Place cloud at ground level in center of area
        BlockPos groundPos = findGroundLevel(areaCenter);

        // Adjust slightly for optimal cloud coverage
        return groundPos.above(1);
    }

    private BlockPos findGroundLevel(BlockPos pos) {
        // Find first solid block below position
        for (int y = pos.getY(); y > pos.getY() - 10; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (foreman.level().getBlockState(checkPos).isSolidRender(foreman.level(), checkPos)) {
                return checkPos;
            }
        }
        return pos;
    }

    private int countNearbyEnemies(int radius) {
        AABB searchBox = new AABB(
            foreman.blockPosition().offset(-radius, -radius, -radius),
            foreman.blockPosition().offset(radius, radius, radius)
        );

        return (int) foreman.level().getEntities(foreman, searchBox)
            .stream()
            .filter(e -> e instanceof LivingEntity)
            .filter(e -> ((LivingEntity)e).getType().getCategory().isFriendly() == false)
            .count();
    }

    private int countNearbyAllies(int radius) {
        AABB searchBox = new AABB(
            foreman.blockPosition().offset(-radius, -radius, -radius),
            foreman.blockPosition().offset(radius, radius, radius)
        );

        return (int) foreman.level().getEntities(foreman, searchBox)
            .stream()
            .filter(e -> e instanceof LivingEntity)
            .filter(e -> ((LivingEntity)e).getType().getCategory().isFriendly() == true)
            .count();
    }

    /**
     * Execute tactical potion throw.
     */
    public ActionResult throwSplashPotion(PotionRecipe potion, BlockPos target) {
        // This would interface with the entity's potion throwing mechanics
        // Implementation depends on Forge entity interaction system

        return ActionResult.success("Threw splash potion at " + target);
    }

    /**
     * Execute lingering potion deployment.
     */
    public ActionResult deployLingeringPotion(PotionRecipe potion, BlockPos target) {
        // Deploy lingering potion cloud at target position
        // Creates area of effect that persists

        return ActionResult.success("Deployed lingering cloud at " + target);
    }
}
```

---

## Integration Architecture

### Action Integration

```java
package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.brewing.*;
import com.minewright.entity.ForemanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Action for brewing potions automatically.
 * Handles the entire brewing process from ingredient gathering to collection.
 */
public class BrewPotionAction extends BaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrewPotionAction.class);

    private String potionName;
    private int quantity;
    private BrewingStandManager brewingManager;
    private IngredientGatherer ingredientGatherer;
    private int brewedCount = 0;
    private BrewingPhase phase = BrewingPhase.GATHERING;

    private enum BrewingPhase {
        GATHERING,
        SETTING_UP,
        BREWING,
        COLLECTING,
        COMPLETE
    }

    public BrewPotionAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        potionName = task.getStringParameter("potion");
        quantity = task.getIntParameter("quantity", 3);

        // Get the recipe
        PotionRecipe recipe = PotionRecipeRegistry.getRecipe(potionName).orElse(null);
        if (recipe == null) {
            result = ActionResult.failure("Unknown potion recipe: " + potionName);
            return;
        }

        LOGGER.info("Foreman '{}' starting to brew {} {} potions",
            foreman.getSteveName(), quantity, potionName);

        // Initialize brewing systems
        brewingManager = new BrewingStandManager(foreman, 4);
        ingredientGatherer = new IngredientGatherer(foreman);

        // Discover brewing stands
        brewingManager.discoverBrewingStands(16);

        // Calculate ingredient requirements
        BrewingJob job = BrewingJob.fromWaterBottles(recipe, (quantity + 2) / 3);
        List<IngredientRequirementsCalculator.IngredientRequirement> requirements =
            IngredientRequirementsCalculator.calculateRequirements(List.of(job));

        // Optimize gathering order
        List<IngredientRequirementsCalculator.IngredientRequirement> optimized =
            IngredientRequirementsCalculator.optimizeGatheringOrder(requirements,
                getAvailableIngredients());

        // Start gathering ingredients
        phase = BrewingPhase.GATHERING;
        ingredientGatherer.gatherIngredients(optimized);
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case GATHERING -> checkGatheringComplete();
            case SETTING_UP -> setupBrewingStands();
            case BREWING -> monitorBrewing();
            case COLLECTING -> collectPotions();
        }
    }

    private void checkGatheringComplete() {
        // Check if all ingredients are available
        // For now, just proceed (would integrate with inventory system)

        LOGGER.info("Foreman '{}' finished gathering ingredients", foreman.getSteveName());
        phase = BrewingPhase.SETTING_UP;
    }

    private void setupBrewingStands() {
        // Create brewing jobs
        PotionRecipe recipe = PotionRecipeRegistry.getRecipe(potionName).orElseThrow();
        int batches = (quantity + 2) / 3;

        for (int i = 0; i < batches; i++) {
            BrewingJob job = BrewingJob.fromWaterBottles(recipe, 1);
            brewingManager.queueJob(job);
        }

        LOGGER.info("Foreman '{}' queued {} brewing batches", foreman.getSteveName(), batches);
        phase = BrewingPhase.BREWING;
    }

    private void monitorBrewing() {
        brewingManager.tick();

        if (brewingManager.getQueuedJobCount() == 0 && brewingManager.getActiveStandCount() == 0) {
            LOGGER.info("Foreman '{}' finished brewing", foreman.getSteveName());
            phase = BrewingPhase.COLLECTING;
        }
    }

    private void collectPotions() {
        brewedCount = quantity; // Simplified
        result = ActionResult.success("Brewed " + brewedCount + " " + potionName + " potions");
        phase = BrewingPhase.COMPLETE;
    }

    @Override
    protected void onCancel() {
        if (brewingManager != null) {
            // Clean up brewing operations
        }
    }

    @Override
    public String getDescription() {
        return "Brew " + quantity + " " + potionName + " potions (" + brewedCount + " done, phase: " + phase + ")";
    }

    private java.util.Map<net.minecraft.world.item.Item, Integer> getAvailableIngredients() {
        // Would interface with inventory system
        return java.util.Collections.emptyMap();
    }
}
```

### Plugin Registration

```java
package com.minewright.brewing;

import com.minewright.action.actions.BrewPotionAction;
import com.minewright.plugin.ActionPlugin;
import com.minewright.plugin.ActionRegistry;
import com.minewright.di.ServiceContainer;

/**
 * Plugin that registers brewing-related actions.
 */
public class BrewingActionPlugin implements ActionPlugin {

    private static final String PLUGIN_ID = "brewing-actions";
    private static final String VERSION = "1.0.0";

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void onLoad(ActionRegistry registry, ServiceContainer container) {
        int priority = getPriority();

        // Register brewing action
        registry.register("brew",
            (foreman, task, ctx) -> new BrewPotionAction(foreman, task),
            priority, PLUGIN_ID);

        // Register tactical brewing action
        registry.register("brew_tactical",
            (foreman, task, ctx) -> new TacticalBrewingAction(foreman, task),
            priority, PLUGIN_ID);
    }

    @Override
    public void onUnload() {
        // Cleanup
    }

    @Override
    public int getPriority() {
        return 500; // Medium priority
    }

    @Override
    public String[] getDependencies() {
        return new String[]{"core-actions"};
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return "Brewing automation: potion brewing, ingredient management, tactical potions";
    }
}
```

---

## Implementation Roadmap

### Phase 1: Core Recipe System (Week 1)

**Tasks:**
1. Implement `PotionRecipe` data structure
2. Create `PotionRecipeRegistry` with all vanilla recipes
3. Add recipe lookup and filtering methods
4. Create unit tests for recipe system
5. Document all supported recipes

**Deliverables:**
- Complete recipe registry
- Recipe lookup API
- Test coverage for all recipes

### Phase 2: Brewing Stand Management (Week 2)

**Tasks:**
1. Implement `BrewingJob` structure
2. Create `BrewingStandController` for single stand
3. Implement `BrewingStandManager` for multi-stand
4. Add automatic fuel management
5. Create brewing progress tracking
6. Add result collection system

**Deliverables:**
- Multi-stand brewing system
- Automatic fueling
- Progress tracking

### Phase 3: Ingredient Optimization (Week 3)

**Tasks:**
1. Implement `IngredientRequirementsCalculator`
2. Create rarity-based prioritization
3. Implement `IngredientGatherer`
4. Add inventory integration
5. Create gathering task generation
6. Optimize gathering routes

**Deliverables:**
- Ingredient calculation system
- Optimized gathering
- Inventory integration

### Phase 4: Potion Selection Logic (Week 4)

**Tasks:**
1. Implement `PotionSelector`
2. Add task-based recommendations
3. Create environment-based selection
4. Implement combat scenario detection
5. Add priority scoring system
6. Create recommendation API

**Deliverables:**
- Intelligent potion selection
- Task/environment awareness
- Combat recommendations

### Phase 5: Tactical Brewing System (Week 5)

**Tasks:**
1. Implement `TacticalBrewingSystem`
2. Add all tactical scenarios
3. Create splash/lingering logic
4. Implement optimal throw positioning
5. Add area denial algorithms
6. Create crowd control tactics

**Deliverables:**
- Complete tactical system
- Splash/lingering support
- Combat tactics

### Phase 6: Action Integration (Week 6)

**Tasks:**
1. Create `BrewPotionAction`
2. Create `TacticalBrewingAction`
3. Implement `BrewingActionPlugin`
4. Add action executor integration
5. Create configuration options
6. Add player commands

**Deliverables:**
- Action integration
- Plugin registration
- User commands

### Phase 7: Testing & Polish (Week 7)

**Tasks:**
1. Integration testing
2. Performance optimization
3. Error handling improvements
4. User interface enhancements
5. Documentation completion
6. Bug fixes

**Deliverables:**
- Tested, optimized system
- Complete documentation
- Production-ready code

---

## Code Examples

### Example 1: Simple Brewing Command

```java
// User command: "Brew 6 strength potions"
Task task = new Task("brew", Map.of(
    "potion", "strength_strong",
    "quantity", 6
));

foreman.getActionExecutor().queueTask(task);
```

### Example 2: Tactical Brewing Loadout

```java
// Get tactical potions for a boss fight
TacticalBrewingSystem tactical = new TacticalBrewingSystem(foreman);
TacticalScenario scenario = TacticalScenario.BOSS_FIGHT;

List<PotionRecipe> loadout = tactical.getTacticalLoadout(scenario);

// Queue brewing jobs for each potion
for (PotionRecipe recipe : loadout) {
    Task task = new Task("brew", Map.of(
        "potion", recipe.getName(),
        "quantity", 2
    ));
    foreman.getActionExecutor().queueTask(task);
}
```

### Example 3: Context-Aware Potion Selection

```java
// Automatically select potions based on task
Task currentTask = foreman.getActionExecutor().getCurrentTask();
List<PotionRecommendation> recommendations = PotionSelector.recommendForTask(currentTask);

// Brew top 3 recommendations
for (int i = 0; i < Math.min(3, recommendations.size()); i++) {
    PotionRecommendation rec = recommendations.get(i);
    Task task = new Task("brew", Map.of(
        "potion", rec.getRecipe().getName(),
        "quantity", rec.getQuantity(),
        "reason", rec.getReason()
    ));
    foreman.getActionExecutor().queueTask(task);
}
```

### Example 4: Multi-Stand Automation

```java
// Set up automated brewing farm
BrewingStandManager manager = new BrewingStandManager(foreman, 8); // 8 stands

// Discover and register stands
manager.discoverBrewingStands(32);

// Queue mass production
PotionRecipe targetRecipe = PotionRecipeRegistry.getRecipe("healing_strong").get();
for (int i = 0; i < 20; i++) { // 20 batches (60 potions)
    BrewingJob job = BrewingJob.fromWaterBottles(targetRecipe, 1);
    manager.queueJob(job);
}

// Manager will automatically handle multi-stand brewing
```

### Example 5: Combat Integration

```java
// During combat, analyze situation and use potions
TacticalBrewingSystem tactical = new TacticalBrewingSystem(foreman);
TacticalScenario scenario = tactical.analyzeSituation();

if (scenario == TacticalScenario.EMERGENCY_HEAL) {
    // Throw splash healing potion
    PotionRecipe healing = PotionRecipeRegistry.getRecipe("splash_healing_strong").get();
    BlockPos throwPos = tactical.calculateSplashPosition(List.of(foreman));
    tactical.throwSplashPotion(healing, throwPos);
} else if (scenario == TacticalScenario.CROWD_CONTROL) {
    // Use lingering slowness potion
    PotionRecipe slowness = PotionRecipeRegistry.getRecipe("lingering_slowness_strong").get();
    List<Entity> enemies = getNearbyEnemies();
    BlockPos cloudPos = tactical.calculateSplashPosition(enemies);
    tactical.deployLingeringPotion(slowness, cloudPos);
}
```

---

## Performance Considerations

### Optimization Strategies

1. **Parallel Brewing**: Use multiple stands simultaneously (up to 16)
2. **Batch Processing**: Group brewing jobs by recipe type
3. **Lazy Evaluation**: Only calculate requirements when needed
4. **Caching**: Cache recipe lookups and recommendations
5. **Async Operations**: Non-blocking brewing progress checks

### Resource Efficiency

- **Ingredient Optimization**: Minimize waste through smart batching
- **Fuel Management**: Automatic blaze powder restocking
- **Inventory Integration**: Direct chest-to-stand transfers
- **Queue Prioritization**: Critical potions brew first

### Scalability

- **Large-Scale Brewing**: Support for 64+ potion batches
- **Multi-Agent Coordination**: Distribute brewing across multiple foremen
- **Farm Integration**: Automatic brewing from ingredient farms

---

## Configuration

### Configuration Options

```toml
# config/minewright-common.toml

[brewing]
# Enable brewing automation system
enabled = true

# Maximum number of brewing stands to operate simultaneously
max_brewing_stands = 8

# Brewing stand discovery radius (blocks)
discovery_radius = 32

# Automatic fuel management
auto_refuel = true
blaze_powder_threshold = 2  # Refuel when below this

# Ingredient gathering
auto_gather = true
gathering_radius = 64

# Tactical brewing
enable_tactical = true
auto_tactical_selection = true

# Potion selection
auto_recommend = true
max_recommendations = 5

# Performance
check_interval = 20  # Ticks between brewing checks
```

---

## Future Enhancements

### Advanced Features

1. **Custom Recipe Support**: Add modded potion recipes
2. **Potion Farm Integration**: Auto-brew from ingredient farms
3. **Multi-Agent Coordination**: Distribute brewing across agents
4. **Potion Storage System**: Automatic sorting and storage
5. **Market Integration**: Sell excess potions
6. **Potion Buff System**: Track active potion effects
7. **Combat Logging**: Record potion usage statistics

### Experimental Features

1. **Potion Brewing AI**: Machine learning for optimal brewing
2. **Potion Combination Effects**: Stack multiple effects
3. **Custom Potion Creation**: Mix and match effects
4. **Potion Delivery Systems**: Redstone-based distribution
5. **Auto-Potion Drink**: Self-buffing in combat

---

## Appendix

### A. Complete Potion Recipe List

All supported vanilla Minecraft 1.20.1 potions:

**Base Potions:**
- Awkward, Thick, Mundane

**Positive Effect Potions:**
- Strength, Speed, Regeneration, Healing
- Night Vision, Invisibility, Fire Resistance
- Water Breathing, Leaping, Slow Falling

**Negative Effect Potions:**
- Poison, Harming, Weakness, Slowness

**Enhancements:**
- Extended duration (Redstone)
- Amplified strength (Glowstone)

**Conversions:**
- Splash potions (Gunpowder)
- Lingering potions (Dragon's Breath)

### B. Ingredient Sources

| Ingredient | Source | Rarity |
|------------|--------|--------|
| Nether Wart | Nether Fortress | Rare |
| Blaze Powder | Blaze (Nether) | Rare |
| Ghast Tear | Ghast (Nether) | Rare |
| Magma Cream | Magma Cube (Nether) | Rare |
| Golden Carrot | Craft (Gold + Carrot) | Uncommon |
| Glistering Melon | Craft (Melon + Gold) | Uncommon |
| Rabbit's Foot | Rabbit | Uncommon |
| Phantom Membrane | Phantom | Uncommon |
| Pufferfish | Fishing | Uncommon |
| Spider Eye | Spider | Common |
| Sugar | Sugar Cane | Common |
| Redstone | Mining | Common |
| Glowstone Dust | Nether | Uncommon |
| Gunpowder | Creeper/Witch | Common |
| Dragon's Breath | End Dragon | Very Rare |
| Fermented Spider Eye | Craft (Spider Eye + Mushroom + Sugar) | Common |

### C. Tactical Scenario Guide

| Scenario | Recommended Potions | Tactics |
|----------|-------------------|---------|
| Arena PvP | Strength II, Speed II, Healing II | Buff before engaging |
| Raid | Strength II, Regeneration II, Splash Healing | Sustain through waves |
| Boss Fight | Strength II, Speed II, Regeneration II | Maximum DPS and sustain |
| Escape | Speed II, Splash Slowness, Invisibility | Break line of sight |
| Ambush | Invisibility, Strength II, Splash Harming | Surprise attack |
| Area Defense | Lingering Poison, Lingering Harming | Zone control |
| Team Buff | Splash Strength, Splash Speed, Splash Regen | Support allies |

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Author:** Claude Code (Brewing Automation Design)

---

## Sources

- [Sportskeeda Wiki - Brewing Recipes](https://wiki.sportskeeda.com/minecraft/brewing-recipes-in-minecraft)
- [Minecraft Fandom Wiki - Brewing](https://minecraft.fandom.com/wiki/Brewing)
- [AS.com Meristation - Brewing Guide](https://www.as.com/meristation/guides/brewing-in-minecraft-how-to-make-potions-all-recipes-and-ingredients-n/)
- [Minecraft Fandom Wiki - Brewing Stand](https://minecraft.fandom.com/wiki/Brewing_Stand)
- [Forge Modding Tutorial - Custom Recipes](https://m.bilibili.com/video/BV1Et421a7i8)
- [Minecraft Wiki - PvP Tutorial](https://zh.minecraft.wiki/w/Tutorial:PvP?variant=zh-cn)
