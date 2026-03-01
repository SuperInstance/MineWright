# Multi-Layer AI Agent Architecture: AI as Living Creature

**Dissertation Chapter 2 - Cognitive Architecture Design**
**Date:** 2026-02-28
**Project:** Steve AI - "Cursor for Minecraft"
**Thesis:** AI agents achieve believable autonomy through multiple layers of abstraction operating at different time scales, modeled after biological cognition

---

## Abstract

This document presents a comprehensive multi-layer architecture for AI agents that moves beyond "one abstraction away" to "multiple abstractions across multiple time scales." Drawing inspiration from biological cognition, neuroscience, and evolutionary psychology, this architecture posits that living creatures exhibit intelligence not through monolithic reasoning systems but through layered cognitive processes operating at vastly different speeds, persistence levels, and learning capabilities. The proposed seven-layer architecture—from 0ms reflexes to continuous meta-learning—provides a framework for building AI agents that feel truly alive, capable of both immediate survival responses and long-term strategic adaptation. This work builds upon traditional game AI architectures (FSMs, behavior trees, utility systems) while integrating modern LLM capabilities at appropriate abstraction levels, creating hybrid systems that are both responsive and reflective.

---

## Table of Contents

1. [Introduction: Beyond Single-Layer Intelligence](#1-introduction-beyond-single-layer-intelligence)
2. [Biological Foundations: The Layered Brain](#2-biological-foundations-the-layered-brain)
3. [The Seven-Layer Architecture](#3-the-seven-layer-architecture)
4. [Layer 1: Reflex Layer (0ms)](#4-layer-1-reflex-layer-0ms)
5. [Layer 2: Pattern Layer (1-10ms)](#5-layer-2-pattern-layer-1-10ms)
6. [Layer 3: State Layer (10-100ms)](#6-layer-3-state-layer-10-100ms)
7. [Layer 4: Perception Layer (50-200ms)](#7-layer-4-perception-layer-50-200ms)
8. [Layer 5: Language Layer (100-500ms)](#8-layer-5-language-layer-100-500ms)
9. [Layer 6: Planning Layer (500ms-10s)](#9-layer-6-planning-layer-500ms-10s)
10. [Layer 7: Meta Layer (Continuous)](#10-layer-7-meta-layer-continuous)
11. [Data Flow and Integration](#11-data-flow-and-integration)
12. [Implementation Guidance](#12-implementation-guidance)
13. [Minecraft-Specific Applications](#13-minecraft-specific-applications)
14. [Comparison to MUD Automation Patterns](#14-comparison-to-mud-automation-patterns)
15. [Evaluation and Metrics](#15-evaluation-and-metrics)
16. [Future Research Directions](#16-future-research-directions)
17. [Conclusion](#17-conclusion)

---

## 1. Introduction: Beyond Single-Layer Intelligence

### 1.1 The Monolithic AI Problem

Contemporary AI agent design suffers from monolithic thinking: the belief that a single decision-making architecture, operating at a single time scale, can produce intelligent behavior. This manifests in several anti-patterns:

**LLM-Only Agents:**
```
User Command → LLM Inference → Direct Action
                    ↓
              500-2000ms latency
              Cannot react in real-time
              No muscle memory formation
              Expensive per decision
```

**Behavior Tree-Only Agents:**
```
Sensors → Behavior Tree Evaluation → Actuators
               ↓
         Fast but rigid
         No learning capability
         Cannot handle novelty
         Designer-authored behaviors only
```

**Hybrid But Sequential:**
```
User Command → LLM Plan → Behavior Tree Execution → Feedback
                    ↓              ↓
              Slow planning    Fast execution
              But: No parallel operation
              Planning blocks execution
              No real-time adaptation
```

### 1.2 The Multi-Layer Thesis

Living creatures do not think with a single cognitive process operating at a single speed. Instead, they exhibit **parallel layered cognition**:

```
Conscious Thought (Language)     → 100-500ms
   ↑
Strategic Planning (Reasoning)   → 500ms-10s
   ↑
Emotional State (Feeling)        → 10-100ms
   ↑
Pattern Recognition (Skill)      → 1-10ms
   ↑
Reflex Response (Instinct)       → 0ms
```

Each layer operates in parallel, with higher layers providing slower, more deliberative inputs while lower layers ensure survival and operational continuity. When higher layers are occupied or unavailable, lower layers maintain competence. When higher layers complete processing, they can override or refine lower layer decisions.

### 1.3 Research Contributions

This work contributes:

1. **Formalization of Multi-Layer AI Architecture:** Systematic framework for designing AI agents with multiple cognitive layers operating at different time scales

2. **Biologically-Inspired Layer Definitions:** Each layer grounded in neuroscience and cognitive psychology research

3. **Integration Patterns:** How layers communicate, override, and influence each other without central coordination

4. **Implementation Guidance:** Concrete Java implementation patterns for each layer in Minecraft context

5. **Evaluation Framework:** Metrics for measuring multi-layer architecture effectiveness

6. **Historical Context:** Connection to pre-LLM automation patterns and their evolution

### 1.4 Relationship to "One Abstraction Away"

This work extends the "One Abstraction Away" thesis (see Chapter 1) by recognizing that:

- **One abstraction is insufficient:** Real cognition requires multiple abstractions at multiple levels
- **Speed matters:** Different abstractions operate at vastly different speeds
- **Parallel operation:** Layers must run concurrently, not sequentially
- **Learning varies:** Not all layers learn, and those that do learn differently
- **Persistence varies:** Some layers maintain state, others are stateless

The multi-layer architecture treats "One Abstraction Away" as a pattern **within** the Planning Layer, not the entire cognitive architecture.

---

## 2. Biological Foundations: The Layered Brain

### 2.1 Evolutionary Neuroscience Perspective

The human brain did not evolve as a unified reasoning engine. Instead, it evolved through **accumulative layering**, with newer structures built atop older ones. This concept, formalized as the **triune brain model** (MacLean, 1990), though simplified, provides a useful architectural metaphor:

```
┌─────────────────────────────────────────────────────┐
│  Neocortex (Rational Brain)                          │
│  - Language, abstract reasoning, planning            │
│  - 300 million years old (mammalian evolution)      │
│  - Slow, deliberative, energy-intensive             │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│  Limbic System (Emotional Brain)                    │
│  - Emotion, memory, motivation                      │
│  - 150 million years old (early mammals)            │
│  - Medium speed, persistent state                   │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│  Reptilian Complex (Instinctive Brain)              │
│  - Reflexes, basic survival, routine patterns       │
│  - 500 million years old (reptilian evolution)      │
│  - Fast, automatic, energy-efficient                │
└─────────────────────────────────────────────────────┘
```

### 2.2 Neural Processing Speed Hierarchy

Neural science reveals dramatic speed differences in neural pathways:

| Neural Pathway | Response Time | Function | Conscious Access |
|----------------|---------------|----------|------------------|
| **Monosynaptic Reflex** | 1-5ms | Immediate withdrawal from pain | No |
| **Spinal Cord Patterns** | 10-50ms | Coordinated movements (walking) | No |
| **Brainstem Reflexes** | 20-100ms | Startle, orientation, basic survival | Limited |
| **Emotional Processing** | 100-200ms | Amygdala threat detection | Limited |
| **Perceptual Recognition** | 150-300ms | Visual object recognition | Yes (delayed) |
| **Language Production** | 300-600ms | Word selection, sentence formation | Yes |
| **Deliberate Reasoning** | 500ms-10s | Complex problem solving | Yes |

Crucially, **all pathways operate simultaneously**. A human touching a hot stove:
1. **Reflex** (5ms): Hand pulls away before pain registers
2. **Emotion** (100ms): Fear/alertness activates
3. **Perception** (200ms): "That was the stove"
4. **Language** (500ms): "Ouch! That was hot!"
5. **Planning** (2000ms): "I should turn down the heat"

### 2.3 Memory Systems Hierarchy

Biological memory is not monolithic but organized by persistence and access speed:

| Memory Type | Duration | Capacity | Access Speed | Forgetting |
|-------------|----------|----------|--------------|------------|
| **Sensory Memory** | <1s | Very Large | Instant | Complete |
| **Working Memory** | 10-30s | 7±2 items | Fast (70ms) | Rapid |
| **Short-Term Memory** | Minutes-Hours | Limited | Medium | Gradual |
| **Long-Term Memory** | Years-Lifetime | Very Large | Slow (100ms+) | Minimal |

This hierarchy informs the **State Layer's** emotional weights (fast, decaying) vs. the **Meta Layer's** learning (slow, persistent).

### 2.4 Skill Acquisition: The Four Stages

Fitts and Posner's (1967) three-stage model of skill acquisition (expanded to four by later researchers) provides the foundation for the **Pattern Layer**:

**Stage 1: Cognitive (Novice)**
- Every action requires conscious attention
- Slow, deliberate, error-prone
- Uses verbal instructions and working memory
- Maps to: **LLM Planning Layer**

**Stage 2: Associative (Intermediate)**
- Actions begin to flow together
- Still requires attention but less error-prone
- Begins to form motor programs
- Maps to: **Pattern Layer (learning phase)**

**Stage 3: Autonomous (Expert)**
- Automatic, fluid execution
- Minimal conscious attention required
- Muscle memory takes over
- Maps to: **Pattern Layer (cached patterns)**

**Stage 4: Mindful Mastery (Expert+)**
- Can refine and teach the skill
- Can vary execution contextually
- Meta-awareness of performance
- Maps to: **Meta Layer (pattern refinement)**

### 2.5 Parallel Processing in the Brain

The brain processes information through **massively parallel** pathways:

**Visual Processing Example:**
```
Retinal Input (simultaneous)
    ├─→ Magnocellular Pathway → Motion detection (fast, low detail)
    ├─→ Parvocellular Pathway → Color/detail (slower, high detail)
    └─→ Koniocellular Pathway → Color/blue-light (specialized)
          ↓
    Both pathways merge in visual cortex but remain partially independent
```

**Threat Processing Example:**
```
Sensory Input
    ├─→ Low Road (Fast): Thalamus → Amygdala → Fear Response (50ms)
    └─→ High Road (Slow): Thalamus → Cortex → Amygdala → Contextual Fear (200ms)
```

This dual-pathway architecture directly inspires our **Reflex → State → Perception** cascade.

---

## 3. The Seven-Layer Architecture

### 3.1 Architecture Overview

The proposed seven-layer architecture organizes AI cognition by **processing speed, abstraction level, update frequency, memory persistence, and learning capability**:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    LAYER 7: META LAYER                             │
│                    (Continuous, Never Done)                         │
│  - Self-monitoring, pattern refinement, learning                   │
│  - Persistent: Saves to disk                                       │
│  - Learning: Observes outcomes, updates all layers                 │
│  - Example: "That mining pattern failed, update the cache"         │
└─────────────────────────────────────────────────────────────────────┘
                              ↑ Influences All Layers
┌─────────────────────────────────────────────────────────────────────┐
│                  LAYER 6: PLANNING LAYER                            │
│                  (500ms - 10s, Novelty Handling)                   │
│  - Strategic thinking, LLM reasoning, novel problem solving        │
│  - Stateful: Conversation context                                  │
│  - Learning: One-shot, but slow                                     │
│  - Example: "How should I approach building this castle?"          │
└─────────────────────────────────────────────────────────────────────┘
                              ↑ Provides Plans
┌─────────────────────────────────────────────────────────────────────┐
│                   LAYER 5: LANGUAGE LAYER                           │
│                   (100-500ms, Communication)                       │
│  - Intention-to-language, dialogue generation                      │
│  - Stateful: Dialogue history                                       │
│  - Learning: Conversational patterns                                │
│  - Example: "I'll start building the walls now"                    │
└─────────────────────────────────────────────────────────────────────┘
                              ↑ Describes Actions
┌─────────────────────────────────────────────────────────────────────┐
│                  LAYER 4: PERCEPTION LAYER                          │
│                  (50-200ms, Understanding)                          │
│  - Vision processing, pattern recognition, audio cues              │
│  - Stateless: Always recomputed                                     │
│  - Learning: Pattern recognition improves                          │
│  - Example: "That's a zombie, it's 8 blocks away"                  │
└─────────────────────────────────────────────────────────────────────┘
                              ↑ Provides Context
┌─────────────────────────────────────────────────────────────────────┐
│                   LAYER 3: STATE LAYER                              │
│                   (10-100ms, Emotional Context)                    │
│  - Emotional weights, attention focus, urgency                     │
│  - Temporal: Decays over time                                      │
│  - Learning: Emotional associations                                │
│  - Example: "I'm afraid of that creeper (0.7 fear intensity)"      │
└─────────────────────────────────────────────────────────────────────┘
                              ↑ Modulates All Layers
┌─────────────────────────────────────────────────────────────────────┐
│                   LAYER 2: PATTERN LAYER                            │
│                   (1-10ms, Muscle Memory)                          │
│  - Cached scripts, common sequences, motor programs                │
│  - Persistent: Script cache                                        │
│  - Learning: Repetition strengthens patterns                      │
│  - Example: "Execute 'bridge_gap' pattern"                         │
└─────────────────────────────────────────────────────────────────────┘
                              ↑ Fast Response
┌─────────────────────────────────────────────────────────────────────┐
│                   LAYER 1: REFLEX LAYER                             │
│                   (0ms, Immediate Survival)                        │
│  - Direct API calls, collision avoidance, safety                  │
│  - Stateless: No memory                                             │
│  - Learning: None (hard-coded)                                     │
│  - Example: "Stop immediately if about to fall"                    │
└─────────────────────────────────────────────────────────────────────┘
                              ↑ Guaranteed Response
```

### 3.2 Layer Characteristics Matrix

| Layer | Response Time | State | Learning | Parallel? | Override? |
|-------|---------------|-------|----------|-----------|-----------|
| **Reflex** | 0ms | None | None | Always | Can be overridden |
| **Pattern** | 1-10ms | Script Cache | Repetition | Always | Can be overridden |
| **State** | 10-100ms | Temporal | Associations | Always | Modulates others |
| **Perception** | 50-200ms | None | Pattern Recog | Always | Informs higher |
| **Language** | 100-500ms | Conversation | Patterns | Yes | Optional |
| **Planning** | 500ms-10s | Context | One-shot | Async | Can wait |
| **Meta** | Continuous | Persistent | All outcomes | Background | Influences all |

### 3.3 Design Principles

**1. Lower Layers Run Faster:**
Each layer increases in processing time as abstraction increases. Reflex must beat frame time (16.67ms @ 60fps). Planning can take seconds.

**2. Higher Layers Handle Novelty:**
Patterns handle routine. Planning handles novel situations that lack cached patterns.

**3. Layers Run in Parallel:**
Planning doesn't block execution. The agent can continue executing patterns while planning next actions.

**4. State Layer Provides Context:**
Emotional weights modulate all other layers. Fear makes reflexes hair-trigger. Joy increases exploration.

**5. Meta Layer Always Learning:**
Even when not actively planning, the meta layer observes outcomes and updates pattern cache, emotional associations, and perception models.

**6. Graceful Degradation:**
If higher layers fail (LLM timeout, vision processing error), lower layers maintain basic competence.

**7. Progressive Enhancement:**
Each layer adds capability but doesn't remove lower-layer competence.

---

## 4. Layer 1: Reflex Layer (0ms)

### 4.1 Purpose

The Reflex Layer provides **immediate, guaranteed survival responses** without any cognitive processing. These are hard-coded instincts that execute deterministically based on direct sensor input.

### 4.2 Biological Inspiration

**Monosynaptic Reflex Arc:**
```
Sensory Neuron → Spinal Cord → Motor Neuron → Muscle Contraction
      (1ms)          (0ms)         (1ms)          (instant)
```

This pathway bypasses the brain entirely, enabling responses in 1-5ms. Examples:
- **Withdrawal Reflex:** Pulling hand from hot object
- **Patellar Reflex:** Knee-jerk response to tendon tap
- **Startle Reflex:** Eyeblink to sudden bright light

### 4.3 Minecraft Reflex Examples

**Survival Reflexes:**
```java
// Immediate danger responses
if (fallVelocity > lethalThreshold) {
    activateParachute();  // Elytra or water bucket
    return;
}

if (blockAhead == lava && distance < 1.0) {
    reverseDirection();
    return;
}

if (incomingProjectile != null && distance < 2.0) {
    activateShield();
    return;
}
```

**Physics Safety:**
```java
// Collision avoidance
if (wouldCollideWithSolid(nextPosition)) {
    stopMovement();
    slideAlongSurface();
    return;
}

// Suffocation prevention
if (isInsideSolidBlock()) {
    digWayOut();
    return;
}
```

### 4.4 Implementation

```java
package com.steve.ai.layers.reflex;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Reflex Layer: Immediate survival responses without cognitive processing.
 * Executes every tick with guaranteed response time < 1ms.
 *
 * Characteristics:
 * - Response Time: 0ms (same tick)
 * - State: None (stateless)
 * - Learning: None (hard-coded)
 * - Parallel: Always runs
 * - Override: Can be overridden by higher layers
 */
public class ReflexLayer {

    private final AgentSensors sensors;
    private final AgentActuators actuators;

    public ReflexLayer(AgentSensors sensors, AgentActuators actuators) {
        this.sensors = sensors;
        this.actuators = actuators;
    }

    /**
     * Executes reflex checks. Called first every tick before any other layer.
     * Returns true if a reflex was triggered (higher layers should defer).
     */
    public ReflexResult checkReflexes() {
        // Check in order of priority (most critical first)

        // 1. Lethal fall prevention
        ReflexResult fallReflex = checkFallReflex();
        if (fallReflex.shouldTrigger) {
            return fallReflex;
        }

        // 2. Immediate physical danger
        ReflexResult dangerReflex = checkDangerReflex();
        if (dangerReflex.shouldTrigger) {
            return dangerReflex;
        }

        // 3. Collision prevention
        ReflexResult collisionReflex = checkCollisionReflex();
        if (collisionReflex.shouldTrigger) {
            return collisionReflex;
        }

        // 4. Suffocation prevention
        ReflexResult suffocationReflex = checkSuffocationReflex();
        if (suffocationReflex.shouldTrigger) {
            return suffocationReflex;
        }

        return ReflexResult.noTrigger();
    }

    private ReflexResult checkFallReflex() {
        Vec3 velocity = sensors.getVelocity();
        double fallDistance = sensors.getFallDistance();

        // Lethal fall threshold
        if (velocity.y < -1.5 && fallDistance > 3.0) {
            // Try to place water bucket
            if (actuators.hasItem(Items.WATER_BUCKET)) {
                return ReflexResult.trigger("place_water_bucket", 1000);
            }

            // Try to activate elytra
            if (actuators.hasElytra()) {
                return ReflexResult.trigger("activate_elytra", 1000);
            }

            // Try to grab ledge
            if (sensors.canGrabLedge()) {
                return ReflexResult.trigger("grab_ledge", 1000);
            }
        }

        return ReflexResult.noTrigger();
    }

    private ReflexResult checkDangerReflex() {
        // Check for immediate threats
        if (sensors.isInLava() && !sensors.isFireResistant()) {
            return ReflexResult.trigger("exit_lava", 1000);
        }

        if (sensors.isInFire() && sensors.getFireTicks() > 50) {
            return ReflexResult.trigger("extinguish", 1000);
        }

        if (sensors.isUnderwater() && sensors.getOxygen() < 30) {
            return ReflexResult.trigger("surface_for_air", 1000);
        }

        return ReflexResult.noTrigger();
    }

    private ReflexResult checkCollisionReflex() {
        Vec3 nextPos = sensors.getNextPosition();
        Vec3 currentPos = sensors.getPosition();

        // If next position would cause collision
        if (sensors.wouldCollide(nextPos) && !sensors.wouldCollide(currentPos)) {
            return ReflexResult.trigger("stop_movement", 500);
        }

        return ReflexResult.noTrigger();
    }

    private ReflexResult checkSuffocationReflex() {
        // If inside solid block
        if (sensors.isInsideSolidBlock()) {
            return ReflexResult.trigger("dig_out", 1000);
        }

        return ReflexResult.noTrigger();
    }
}

/**
 * Result of reflex check.
 */
public class ReflexResult {
    public final boolean shouldTrigger;
    public final String action;
    public final int priority;  // Higher = more important

    private ReflexResult(boolean shouldTrigger, String action, int priority) {
        this.shouldTrigger = shouldTrigger;
        this.action = action;
        this.priority = priority;
    }

    public static ReflexResult trigger(String action, int priority) {
        return new ReflexResult(true, action, priority);
    }

    public static ReflexResult noTrigger() {
        return new ReflexResult(false, null, 0);
    }
}
```

### 4.5 Integration with Higher Layers

**Reflex Override Pattern:**
```java
public void tick() {
    // 1. Check reflexes first
    ReflexResult reflex = reflexLayer.checkReflexes();
    if (reflex.shouldTrigger) {
        // Execute reflex immediately, skip other layers
        executeReflex(reflex);
        return;
    }

    // 2. If no reflex, proceed to pattern layer
    patternLayer.tick();

    // ... other layers
}
```

**Reflex Integration Pattern:**
```java
// Higher layers can "acknowledge" reflexes and integrate them
if (reflexLayer.checkFallReflex().triggered) {
    stateLayer.updateEmotion(Emotion.FEAR, 0.3);  // Fear of heights
    planningLayer.interrupt("falling", "need_recovery_plan");
}
```

### 4.6 Reflex Layer Characteristics

**Advantages:**
- **Guaranteed response time:** Always executes within same tick
- **Predictable:** Deterministic, testable, debuggable
- **Low cost:** Minimal CPU usage
- **Survival assurance:** Agent won't die to trivial threats

**Limitations:**
- **No learning:** Cannot adapt to new situations
- **Rigid:** Can't handle context-dependent dangers
- **Limited scope:** Only handles immediate threats
- **False positives:** May over-react to safe situations

**When to Use:**
- Immediate survival threats (falling, lava, suffocation)
- Physical safety (collisions, stuck conditions)
- Involuntary protection mechanisms
- Situations where "better safe than sorry"

---

## 5. Layer 2: Pattern Layer (1-10ms)

### 5.1 Purpose

The Pattern Layer provides **fast, cached execution of learned sequences**. These are "muscle memory" patterns—skills that have been practiced enough to execute automatically without conscious thought.

### 5.2 Biological Inspiration

**Motor Programs:**
Neuroscience research shows that learned skills are stored as **motor programs** in the basal ganglia and cerebellum:

```
Skill Learning Timeline:
Novice (1000ms):    "I need to position my hands like this..."
                   (Conscious, verbal, slow)
Intermediate (100ms): "This feels familiar, I'll try..."
                   (Less conscious, faster)
Expert (10ms):       [Hands execute automatically]
                   (Unconscious, muscle memory)
```

**Procedural Memory:**
Distinct from declarative memory (facts), procedural memory stores **how to perform actions**:
- Riding a bike
- Typing on keyboard
- Playing musical instrument
- Athletic movements

Access time: **1-10ms** for well-practiced sequences.

### 5.3 Minecraft Pattern Examples

**Movement Patterns:**
```javascript
// Parkour sequence: Jump, sprint, place block, continue
pattern "bridge_gap" {
    detect: gap_ahead > 3 blocks
    execute:
        1. sprint_forward()
        2. jump()
        3. place_block(cobblestone, down)
        4. continue()
    cache_key: "bridge_3block_gap"
}
```

**Combat Patterns:**
```javascript
// Shield blocking rhythm
pattern "shield_block" {
    detect: incoming_attack && shield_equipped
    execute:
        1. raise_shield()
        2. wait(attack_duration)
        3. lower_shield()
        4. counter_attack()
    cache_key: "shield_timing_" + enemy_type
}
```

**Resource Gathering Patterns:**
```javascript
// Tree felling sequence
pattern "chop_tree" {
    detect: looking_at(log) && axe_equipped
    execute:
        while (logs_above()) {
            mine_block(ahead)
            move_up()
        }
        harvest_all_logs()
    cache_key: "chop_oak_tree"
}
```

### 5.4 Implementation

```java
package com.steve.ai.layers.pattern;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

/**
 * Pattern Layer: Cached "muscle memory" for fast execution of learned sequences.
 *
 * Characteristics:
 * - Response Time: 1-10ms (cached execution)
 * - State: Script cache (persistent)
 * - Learning: Repetition strengthens patterns
 * - Parallel: Always available
 * - Override: Can be overridden by higher layers
 */
public class PatternLayer {

    private final Cache<String, ExecutablePattern> patternCache;
    private final PatternLearner learner;
    private final PatternExecutor executor;

    public PatternLayer() {
        // Configure cache with high hit-rate targeting
        this.patternCache = Caffeine.newBuilder()
            .maximumSize(1000)  // Store up to 1000 patterns
            .expireAfterAccess(1, TimeUnit.HOURS)  // Decay unused patterns
            .recordStats()  // Track hit rate
            .build();

        this.learner = new PatternLearner(patternCache);
        this.executor = new PatternExecutor();
    }

    /**
     * Attempts to execute a cached pattern for the current situation.
     * Returns true if a pattern was found and executed.
     */
    public PatternResult tryExecutePattern(SituationContext context) {
        String cacheKey = generateCacheKey(context);

        // Try to get cached pattern
        ExecutablePattern pattern = patternCache.getIfPresent(cacheKey);
        if (pattern != null) {
            // Execute pattern
            long startTime = System.nanoTime();
            PatternExecutionResult result = executor.execute(pattern, context);
            long duration = System.nanoTime() - startTime;

            return PatternResult.success(result, duration / 1_000_000.0);
        }

        return PatternResult.notFound();
    }

    /**
     * Records a successful action sequence for potential pattern learning.
     * The PatternLearner analyzes sequences and decides what to cache.
     */
    public void recordSequence(ActionSequence sequence) {
        learner.analyzeAndCache(sequence);
    }

    /**
     * Generates cache key from situation context.
     * Key encodes: action type, environment, tools, constraints
     */
    private String generateCacheKey(SituationContext context) {
        return String.format("%s_%s_%s_%s",
            context.getActionType(),
            context.getEnvironmentType(),
            context.getToolType(),
            context.getConstraints().hashCode()
        );
    }

    /**
     * Gets cache statistics for monitoring pattern effectiveness.
     */
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats =
            patternCache.stats();

        return new CacheStats(
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate(),
            patternCache.asMap().size()
        );
    }
}

/**
 * A cached executable pattern.
 */
public class ExecutablePattern {
    private final String name;
    private final SituationMatcher matcher;
    private final ActionSequence actions;
    private final int executionCount;  // How many times successfully executed
    private final double averageDuration;  // Average execution time in ms

    public ExecutablePattern(String name, SituationMatcher matcher,
                            ActionSequence actions, int executionCount,
                            double averageDuration) {
        this.name = name;
        this.matcher = matcher;
        this.actions = actions;
        this.executionCount = executionCount;
        this.averageDuration = averageDuration;
    }

    public boolean matches(SituationContext context) {
        return matcher.matches(context);
    }

    public ActionSequence getActions() {
        return actions;
    }
}

/**
 * Analyzes action sequences and learns patterns worth caching.
 */
public class PatternLearner {
    private final Cache<String, ExecutablePattern> patternCache;
    private final SequenceHistory history;

    public PatternLearner(Cache<String, ExecutablePattern> patternCache) {
        this.patternCache = patternCache;
        this.history = new SequenceHistory();
    }

    /**
     * Analyzes a completed action sequence and decides whether to cache it.
     */
    public void analyzeAndCache(ActionSequence sequence) {
        // Record sequence
        history.record(sequence);

        // Check if this is a repeated pattern (3+ times with similar contexts)
        if (history.getRepeatCount(sequence) >= 3) {
            // Analyze pattern quality
            PatternQuality quality = analyzeQuality(sequence);

            if (quality.isCacheable()) {
                // Create executable pattern
                ExecutablePattern pattern = createPattern(sequence, quality);

                // Cache it
                String cacheKey = generateCacheKey(sequence);
                patternCache.put(cacheKey, pattern);
            }
        }
    }

    private PatternQuality analyzeQuality(ActionSequence sequence) {
        double successRate = sequence.getSuccessRate();
        double averageDuration = sequence.getAverageDuration();
        double complexity = sequence.getComplexity();

        // Cache if:
        // - High success rate (>80%)
        // - Fast execution (<100ms per action)
        // - Moderate complexity (not too simple, not too complex)
        boolean isCacheable = successRate > 0.8
            && averageDuration < 100.0
            && complexity > 2 && complexity < 20;

        return new PatternQuality(isCacheable, successRate, averageDuration, complexity);
    }

    private ExecutablePattern createPattern(ActionSequence sequence,
                                            PatternQuality quality) {
        SituationMatcher matcher = createMatcher(sequence.getContexts());
        return new ExecutablePattern(
            sequence.getName(),
            matcher,
            sequence,
            sequence.getExecutionCount(),
            quality.averageDuration
        );
    }
}

/**
 * Executes cached patterns efficiently.
 */
public class PatternExecutor {
    /**
     * Executes a pattern in the given context.
     */
    public PatternExecutionResult execute(ExecutablePattern pattern,
                                         SituationContext context) {
        try {
            // Execute each action in sequence
            for (Action action : pattern.getActions()) {
                ActionResult result = action.execute(context);

                if (!result.isSuccess()) {
                    // Action failed, abort pattern
                    return PatternExecutionResult.failed(action, result);
                }

                // Check if context changed significantly
                if (context.hasChangedSignificantly()) {
                    // Context changed, abort pattern
                    return PatternExecutionResult.contextChanged(context);
                }
            }

            // All actions succeeded
            return PatternExecutionResult.success();
        } catch (Exception e) {
            return PatternExecutionResult.error(e);
        }
    }
}
```

### 5.5 Pattern Learning Strategy

**When to Learn Patterns:**

1. **Repetition:** Sequences executed 3+ times in similar contexts
2. **Success Rate:** >80% success rate
3. **Speed:** Faster than LLM planning would be
4. **Complexity:** Moderate (2-20 actions)

**Pattern Discovery:**
```java
// Example: Agent mines iron ore repeatedly
Sequence 1: approach_ore() -> equip_pickaxe() -> mine() -> collect()
Sequence 2: approach_ore() -> equip_pickaxe() -> mine() -> collect()
Sequence 3: approach_ore() -> equip_pickaxe() -> mine() -> collect()

PatternLearner detects repetition:
  - Context: "mine_iron_ore"
  - Actions: [approach, equip, mine, collect]
  - Success: 100%
  - Duration: ~50ms

→ Cache pattern: "mine_iron_ore_fast"
```

**Pattern Invalidatation:**
```java
// Pattern fails 3+ times → Invalidate
if (pattern.getRecentFailureRate() > 0.5) {
    patternCache.invalidate(pattern.getCacheKey());
    // Re-learn with new context
}
```

### 5.6 Pattern Layer Characteristics

**Advantages:**
- **Very fast:** 1-10ms execution (vs 500ms+ for LLM)
- **High reliability:** Patterns only cached after proven success
- **Low cost:** Once cached, zero API costs
- **Predictable:** Deterministic execution

**Limitations:**
- **Inflexible:** Can't adapt to novel situations
- **Requires learning:** Needs 3+ repetitions to cache
- **Context-bound:** Patterns tied to specific contexts
- **Maintenance overhead:** Cache must be managed

**When to Use:**
- Frequently repeated tasks (mining, building, combat)
- Time-sensitive sequences (combat, parkour)
- Well-understood problems with proven solutions
- Situations where speed matters more than novelty

---

## 6. Layer 3: State Layer (10-100ms)

### 6.1 Purpose

The State Layer provides **emotional context, urgency modulation, and attention focus** to all other layers. Unlike reflex (immediate) and pattern (fast execution), the State Layer operates on **temporal emotional weights** that decay over time, creating urgency, fear, joy, and other modulating factors.

### 6.2 Biological Inspiration

**Emotional Processing in the Limbic System:**
```
Sensory Input
    ↓
Thalamus
    ├─→ Cortex (Slow, 200ms) → Conscious perception
    └─→ Amygdala (Fast, 50ms) → Emotional response
           ↓
         Hypothalamus → Physiological changes (heart rate, hormones)
           ↓
         "I feel afraid before I know why"
```

**Temporal Dynamics of Emotion:**
- **Startle Response:** 50ms (peak) → 200ms (decay)
- **Fear Conditioning:** 100ms (acquisition) → hours (persistence)
- **Joy/Excitement:** 500ms (onset) → seconds (decay)
- **Mood:** Minutes to hours (background affect)

**Emotional Modulation:**
Emotions don't directly cause actions but **modulate** other systems:
- **Fear** → Reflexes become hair-trigger (lower threshold)
- **Anger** → Aggressive actions prioritized
- **Joy** → Exploration encouraged, risk tolerance increased
- **Sadness** → Motivation decreased, withdrawal behavior

### 6.3 Minecraft Emotional State Examples

**Fear States:**
```java
// Recent creeper encounter → Fear of dark
emotion fear_of_dark {
    trigger: creeper_encounter (intensity: 0.8)
    decay_rate: 0.05 per second
    effects:
        - Increase light_level_threshold for movement
        - Prioritize torch_placement actions
        - Decrease exploration_distance
        - Modulate reflex: trigger_distance from 2 → 4 blocks
}
```

**Excitement States:**
```java
// Finding diamonds → Excitement
emotion excitement_discovery {
    trigger: find_rare_ore (intensity: 0.9)
    decay_rate: 0.1 per second
    effects:
        - Increase chat_frequency
        - Prioritize sharing_findings with player
        - Increase movement_speed (celebratory jumping)
        - Modulate planning: more ambitious goals
}
```

**Trust States:**
```java
// Player helps in combat → Trust
emotion trust_player {
    trigger: player_saves_life (intensity: 0.7)
    decay_rate: 0.01 per second (very slow)
    effects:
        - Follow player more closely
        - Accept player commands without verification
        - Share resources with player
        - Decrease self_preservation_priority
}
```

### 6.4 Implementation

```java
package com.steve.ai.layers.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State Layer: Emotional weights and temporal state that modulate all other layers.
 *
 * Implements the OCC (Ortony-Clore-Collins) model of emotions for
 * psychologically-grounded emotional responses.
 *
 * Characteristics:
 * - Response Time: 10-100ms (emotional modulation)
 * - State: Temporal (decays over time)
 * - Learning: Emotional associations
 * - Parallel: Always modulates
 * - Override: Cannot be overridden (provides context)
 */
public class StateLayer {

    /** Current emotional intensities */
    private final ConcurrentHashMap<Emotion, Double> emotions;

    /** Personality traits affecting emotional responses */
    private final PersonalityTraits traits;

    /** Emotional memory for associations */
    private final EmotionalMemory memory;

    /** Decay rates for each emotion (per second) */
    private static final Map<Emotion, Double> DECAY_RATES = Map.of(
        // Fast-decaying emotions (momentary reactions)
        Emotion.STARTLE, 0.3,
        Emotion.SURPRISE, 0.2,

        // Medium-decaying emotions (short-term reactions)
        Emotion.FEAR, 0.08,
        Emotion.ANGER, 0.06,
        Emotion.JOY, 0.10,
        Emotion.EXCITEMENT, 0.12,

        // Slow-decaying emotions (longer-term attitudes)
        Emotion.TRUST, 0.02,
        Emotion.VIGILANCE, 0.03,
        Emotion.CAUTIOUSNESS, 0.025
    );

    public StateLayer(PersonalityTraits traits) {
        this.emotions = new ConcurrentHashMap<>();
        this.traits = traits;
        this.memory = new EmotionalMemory();

        // Initialize baseline emotions
        for (Emotion e : Emotion.values()) {
            emotions.put(e, 0.0);
        }
    }

    /**
     * Updates emotional state for one game tick.
     * Applies decay to all emotions.
     */
    public void tick(double deltaTime) {
        for (Emotion emotion : Emotion.values()) {
            double current = emotions.get(emotion);
            double decay = DECAY_RATES.getOrDefault(emotion, 0.1);
            double decayed = current * Math.pow(1.0 - decay, deltaTime);
            emotions.put(emotion, decayed);
        }
    }

    /**
     * Appraises an event and generates emotional responses.
     */
    public void appraiseEvent(EmotionalEvent event) {
        // Calculate desirability
        double desirability = calculateDesirability(event);

        // Event-based emotions
        if (event.isThreat()) {
            updateEmotion(Emotion.FEAR, desirability * traits.fearReactivity);
            if (event.isSurprise()) {
                updateEmotion(Emotion.STARTLE, 0.5);
            }
        }

        if (event.isAchievement()) {
            updateEmotion(Emotion.JOY, desirability * traits.joyReactivity);
            updateEmotion(Emotion.EXCITEMENT, desirability * 0.8);
        }

        if (event.isSocialPositive()) {
            updateEmotion(Emotion.TRUST, desirability * traits.trustPropensity);
        }

        // Store emotional associations for learning
        memory.recordAssociation(event, getCurrentEmotions());
    }

    /**
     * Gets modulation factor for a specific system.
     * Returns 1.0 if no modulation, >1.0 if amplified, <1.0 if inhibited.
     */
    public double getModulation(ModulationTarget target) {
        switch (target) {
            case REFLEX_THRESHOLD:
                // Fear lowers reflex threshold (hair-trigger)
                return 1.0 - (getEmotion(Emotion.FEAR) * 0.5);

            case MOVEMENT_SPEED:
                // Fear slows movement, excitement speeds up
                return 1.0 + getEmotion(Emotion.EXCITEMENT) * 0.2
                           - getEmotion(Emotion.FEAR) * 0.3;

            case RISK_TOLERANCE:
                // Trust increases risk tolerance, fear decreases
                return 1.0 + getEmotion(Emotion.TRUST) * 0.3
                           - getEmotion(Emotion.FEAR) * 0.5;

            case EXPLORATION_RADIUS:
                // Vigilance increases exploration, cautiousness decreases
                return 1.0 + getEmotion(Emotion.VIGILANCE) * 0.5
                           - getEmotion(Emotion.CAUTIOUSNESS) * 0.3;

            case AGGRESSION_LEVEL:
                // Anger increases aggression
                return 1.0 + getEmotion(Emotion.ANGER) * 0.7;

            default:
                return 1.0;
        }
    }

    /**
     * Gets current intensity of an emotion.
     */
    public double getEmotion(Emotion emotion) {
        return emotions.getOrDefault(emotion, 0.0);
    }

    /**
     * Updates an emotion's intensity by delta (clamped to 0.0-1.0).
     */
    private void updateEmotion(Emotion emotion, double delta) {
        double current = emotions.get(emotion);
        double updated = Math.max(0.0, Math.min(1.0, current + delta));
        emotions.put(emotion, updated);
    }

    /**
     * Gets snapshot of all current emotions.
     */
    public Map<Emotion, Double> getCurrentEmotions() {
        return Map.copyOf(emotions);
    }

    /**
     * Calculates desirability of an event outcome.
     */
    private double calculateDesirability(EmotionalEvent event) {
        double desirability = 0.0;

        if (event.isLifeThreatening()) {
            desirability -= 0.9;
        }

        if (event.givesResources()) {
            desirability += event.getResourceScarcity() * traits.materialism;
        }

        if (event.isAchievement()) {
            desirability += traits.ambition * event.getAchievementSignificance();
        }

        return Math.max(-1.0, Math.min(1.0, desirability));
    }
}

/**
 * Emotion types in the system.
 */
public enum Emotion {
    // Immediate reactions
    STARTLE,
    SURPRISE,

    // Survival emotions
    FEAR,
    VIGILANCE,
    CAUTIOUSNESS,

    // Positive emotions
    JOY,
    EXCITEMENT,

    // Social emotions
    TRUST,
    GRATITUDE,

    // Aggressive emotions
    ANGER,
    FRUSTRATION
}

/**
 * Targets that can be modulated by emotional state.
 */
public enum ModulationTarget {
    REFLEX_THRESHOLD,      // How easily reflexes trigger
    MOVEMENT_SPEED,        // How fast to move
    RISK_TOLERANCE,        // How much risk to accept
    EXPLORATION_RADIUS,    // How far to explore
    AGGRESSION_LEVEL,      // How aggressive to be
    COOPERATION_LEVEL,     // How much to cooperate
    RESOURCE_SHARING,      // How much to share resources
    COMMUNICATION_FREQUENCY // How much to chat
}

/**
 * Personality traits affecting emotional responses.
 */
public class PersonalityTraits {
    double fearReactivity = 0.7;      // How strongly fear is felt
    double joyReactivity = 0.6;       // How strongly joy is felt
    double trustPropensity = 0.5;     // How easily trust is given
    double materialism = 0.4;         // Importance of resources
    double ambition = 0.5;            // Drive for achievement

    public PersonalityTraits() {}
}

/**
 * Emotional memory for learning associations.
 */
public class EmotionalMemory {
    private final Map<String, EmotionalAssociation> associations;

    public EmotionalMemory() {
        this.associations = new ConcurrentHashMap<>();
    }

    public void recordAssociation(EmotionalEvent event,
                                  Map<Emotion, Double> emotions) {
        String key = event.getType();
        associations.put(key, new EmotionalAssociation(event, emotions));
    }

    public EmotionalAssociation getAssociation(String eventType) {
        return associations.get(eventType);
    }
}
```

### 6.5 Integration with Other Layers

**Modulating Reflex Layer:**
```java
// Fear makes reflexes trigger more easily
double reflexModulation = stateLayer.getModulation(ModulationTarget.REFLEX_THRESHOLD);
if (distanceToDanger < 4.0 * reflexModulation) {
    triggerReflex();
}
```

**Modulating Pattern Layer:**
```java
// Excitement makes movement faster
double speedModulation = stateLayer.getModulation(ModulationTarget.MOVEMENT_SPEED);
pattern.executeWithSpeedModifier(speedModulation);
```

**Modulating Planning Layer:**
```java
// Trust affects planning risk tolerance
double riskModulation = stateLayer.getModulation(ModulationTarget.RISK_TOLERANCE);
if (riskModulation > 1.2) {
    // High trust: accept player's ambitious plan
    return playerPlan;
} else if (riskModulation < 0.8) {
    // Low trust: generate conservative plan
    return generateConservativePlan();
}
```

### 6.6 State Layer Characteristics

**Advantages:**
- **Contextual modulation:** Provides rich behavioral variation
- **Psychologically grounded:** Based on OCC model
- **Temporal dynamics:** Emotions decay realistically
- **Integration point:** Connects all layers

**Limitations:**
- **Not action-oriented:** Doesn't directly cause actions
- **Complex tuning:** Many parameters to balance
- **Computationally moderate:** 10-100ms per update
- **Requires personality:** Needs trait configuration

**When to Use:**
- Always running (provides background context)
- Situations requiring emotional responses
- Behavioral variation between agents
- Social interactions and relationship modeling

---

## 7. Layer 4: Perception Layer (50-200ms)

### 7.1 Purpose

The Perception Layer processes **sensory input into meaningful understanding**. It transforms raw game state (blocks, entities, positions) into semantic concepts ("zombie," "tree," "danger") that higher layers can reason about.

### 7.2 Biological Inspiration

**Visual Processing Hierarchy:**
```
Retina (Light)
    ↓
LGN (Relay)
    ↓
V1 (Edge Detection)
    ↓
V2/V3 (Shape, Motion, Color)
    ↓
V4/IT (Object Recognition) → "That's a face"
    ↓
PPC (Spatial Relations) → "It's 2 meters away"
    ↓
PFC (Meaning) → "That's my friend, she looks happy"
```

**Two-Stream Hypothesis:**
- **Ventral Stream (What):** Object identification → "Apple"
- **Dorsal Stream (Where):** Spatial location → "At coordinates (x,y,z)"

**Processing Time:**
- **Edge Detection:** 40ms
- **Object Recognition:** 100-150ms
- **Scene Understanding:** 200-300ms

### 7.3 Minecraft Perception Examples

**Visual Object Recognition:**
```java
// Raw: Entity at (x,y,z) with specific texture
// Perceived: "Zombie, 8 blocks away, hostile"

PerceivedEntity zombie = perceptionLayer.identify(entity);
assert zombie.getType() == EntityType.ZOMBIE;
assert zombie.getDistance() == 8.0;
assert zombie.getDisposition() == Disposition.HOSTILE;
assert zombie.getThreatLevel() == ThreatLevel.MEDIUM;
```

**Spatial Reasoning:**
```java
// Raw: Block coordinates
// Perceived: "Tree structure, 12 blocks tall, oak variety"

PerceivedStructure tree = perceptionLayer.analyze(blocks);
assert tree.getType() == StructureType.TREE;
assert tree.getHeight() == 12;
assert tree.getVariety() == TreeVariety.OAK;
assert tree.isReachable();
```

**Pattern Recognition:**
```java
// Raw: Series of block arrangements
// Perceived: "This is a player-built shelter"

PerceivedConstruction shelter = perceptionLayer.recognizePattern(blocks);
assert shelter.getPatternType() == PatternType.SHELTER;
assert shelter.getConfidence() > 0.8;
assert shelter.hasFeatures(Feature.ROOF, Feature.DOOR, Feature.LIGHT);
```

### 7.4 Implementation

```java
package com.steve.ai.layers.perception;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import java.util.List;

/**
 * Perception Layer: Processes sensory input into semantic understanding.
 *
 * Characteristics:
 * - Response Time: 50-200ms (perceptual processing)
 * - State: None (always recomputed)
 * - Learning: Pattern recognition improves
 * - Parallel: Runs continuously
 * - Override: Cannot be overridden (provides ground truth)
 */
public class PerceptionLayer {

    private final EntityClassifier entityClassifier;
    private final StructureAnalyzer structureAnalyzer;
    private final PatternRecognizer patternRecognizer;
    private final SpatialReasoner spatialReasoner;

    public PerceptionLayer() {
        this.entityClassifier = new EntityClassifier();
        this.structureAnalyzer = new StructureAnalyzer();
        this.patternRecognizer = new PatternRecognizer();
        this.spatialReasoner = new SpatialReasoner();
    }

    /**
     * Processes current sensory input and produces perceptual snapshot.
     */
    public PerceptualSnapshot processPerception(SensoryInput input) {
        long startTime = System.nanoTime();

        // Classify entities
        List<PerceivedEntity> entities = entityClassifier.classifyAll(input.getEntities());

        // Analyze structures
        List<PerceivedStructure> structures = structureAnalyzer.analyzeAll(input.getBlocks());

        // Recognize patterns
        List<RecognizedPattern> patterns = patternRecognizer.recognize(input.getBlocks());

        // Spatial reasoning
        SpatialMap spatialMap = spatialReasoner.buildSpatialMap(input.getBlocks(), entities);

        long duration = (System.nanoTime() - startTime) / 1_000_000;

        return new PerceptualSnapshot(entities, structures, patterns, spatialMap, duration);
    }

    /**
     * Classifies a single entity.
     */
    public PerceivedEntity classifyEntity(Entity entity) {
        return entityClassifier.classify(entity);
    }

    /**
     * Analyzes blocks around a position.
     */
    public LocalEnvironment analyzeEnvironment(BlockPos center, int radius) {
        List<BlockState> blocks = getBlocksInRadius(center, radius);
        return structureAnalyzer.analyzeLocal(blocks);
    }
}

/**
 * Classifies entities into semantic categories.
 */
public class EntityClassifier {

    private static final Map<String, EntityTemplate> ENTITY_TEMPLATES = loadEntityTemplates();

    /**
     * Classifies an entity into a semantic category with attributes.
     */
    public PerceivedEntity classify(Entity entity) {
        // Get entity type
        EntityType type = EntityType.fromEntity(entity);

        // Get template for this type
        EntityTemplate template = ENTITY_TEMPLATES.get(type.getId());

        // Extract attributes
        EntityAttributes attributes = extractAttributes(entity, template);

        // Assess threat level
        ThreatLevel threat = assessThreat(entity, attributes);

        // Assess disposition
        Disposition disposition = assessDisposition(entity, template);

        return new PerceivedEntity(
            entity.getUUID(),
            type,
            attributes,
            threat,
            disposition,
            entity.position()
        );
    }

    private ThreatLevel assessThreat(Entity entity, EntityAttributes attributes) {
        // Consider: damage potential, speed, range, hostility
        double threatScore = 0.0;

        if (attributes.isHostile()) {
            threatScore += 0.5;
        }

        threatScore += attributes.getAttackDamage() * 0.3;
        threatScore += attributes.getSpeed() * 0.1;
        threatScore += attributes.getAttackRange() * 0.05;

        if (entity.getHealth() < 10) {
            threatScore -= 0.2;  // Weakened
        }

        if (threatScore > 0.7) return ThreatLevel.CRITICAL;
        if (threatScore > 0.4) return ThreatLevel.HIGH;
        if (threatScore > 0.2) return ThreatLevel.MEDIUM;
        return ThreatLevel.LOW;
    }

    private Disposition assessDisposition(Entity entity, EntityTemplate template) {
        if (template.isNaturallyHostile()) {
            return Disposition.HOSTILE;
        }

        if (template.isNaturallyNeutral()) {
            // Check if provoked
            if (entity.isAngry()) {
                return Disposition.HOSTILE;
            }
            return Disposition.NEUTRAL;
        }

        return Disposition.FRIENDLY;
    }
}

/**
 * Analyzes block structures into semantic concepts.
 */
public class StructureAnalyzer {

    /**
     * Analyzes blocks to identify structures.
     */
    public List<PerceivedStructure> analyzeAll(List<BlockState> blocks) {
        List<PerceivedStructure> structures = new ArrayList<>();

        // Group blocks by proximity
        List<BlockGroup> groups = groupByProximity(blocks, 3.0);

        // Analyze each group
        for (BlockGroup group : groups) {
            PerceivedStructure structure = analyzeGroup(group);
            if (structure != null) {
                structures.add(structure);
            }
        }

        return structures;
    }

    private PerceivedStructure analyzeGroup(BlockGroup group) {
        // Check for known structure types
        if (isTree(group)) {
            return analyzeTree(group);
        }

        if (isBuilding(group)) {
            return analyzeBuilding(group);
        }

        if (isFarm(group)) {
            return analyzeFarm(group);
        }

        // Unknown structure
        return new PerceivedStructure(StructureType.UNKNOWN, group.getBlocks());
    }

    private boolean isTree(BlockGroup group) {
        // Check for: log blocks vertically arranged, leaf blocks on top
        return group.hasBlocksOf(Material.WOOD)
            && group.hasBlocksOf(Material.LEAVES)
            && group.isVerticallyArranged();
    }

    private PerceivedStructure analyzeTree(BlockGroup group) {
        // Determine tree variety
        TreeVariety variety = determineTreeVariety(group);

        // Measure height
        int height = measureHeight(group);

        // Check if fully grown
        boolean isFullyGrown = height >= variety.getMatureHeight();

        return new PerceivedStructure(
            StructureType.TREE,
            variety,
            height,
            isFullyGrown,
            group.getBlocks()
        );
    }

    private boolean isBuilding(BlockGroup group) {
        // Check for: walls, roof, floor, door
        return group.hasPattern(Pattern.WALLS)
            && group.hasPattern(Pattern.ROOF)
            && group.hasPattern(Pattern.FLOOR);
    }

    private PerceivedStructure analyzeBuilding(BlockGroup group) {
        // Determine building type
        BuildingType type = classifyBuildingType(group);

        // Measure dimensions
        Dimensions dimensions = measureDimensions(group);

        // Check features
        List<Feature> features = detectFeatures(group);

        return new PerceivedStructure(
            StructureType.BUILDING,
            type,
            dimensions,
            features,
            group.getBlocks()
        );
    }
}

/**
 * Recognizes patterns in block arrangements.
 */
public class PatternRecognizer {

    private static final List<PatternTemplate> PATTERNS = loadPatternTemplates();

    /**
     * Recognizes patterns in block arrangements.
     */
    public List<RecognizedPattern> recognize(List<BlockState> blocks) {
        List<RecognizedPattern> recognized = new ArrayList<>();

        for (PatternTemplate template : PATTERNS) {
            double confidence = template.match(blocks);

            if (confidence > 0.7) {
                recognized.add(new RecognizedPattern(
                    template.getType(),
                    confidence,
                    template.getAttributes()
                ));
            }
        }

        return recognized;
    }
}

/**
 * Performs spatial reasoning about relationships.
 */
public class SpatialReasoner {

    /**
     * Builds a spatial map of entities and structures.
     */
    public SpatialMap buildSpatialMap(List<BlockState> blocks,
                                     List<PerceivedEntity> entities) {
        SpatialMap map = new SpatialMap();

        // Add entities
        for (PerceivedEntity entity : entities) {
            map.addEntity(entity);
        }

        // Add navigation graph
        NavigationGraph navGraph = buildNavigationGraph(blocks);
        map.setNavigationGraph(navGraph);

        // Add spatial relationships
        List<SpatialRelationship> relationships = deriveRelationships(entities, blocks);
        map.setRelationships(relationships);

        return map;
    }

    private NavigationGraph buildNavigationGraph(List<BlockState> blocks) {
        // Build graph for pathfinding
        // Nodes: walkable positions
        // Edges: valid movements
        // Weights: distance, danger, difficulty
        return new NavigationGraphBuilder()
            .addBlocks(blocks)
            .removeCollisions()
            .addDangerZones()
            .build();
    }

    private List<SpatialRelationship> deriveRelationships(List<PerceivedEntity> entities,
                                                         List<BlockState> blocks) {
        List<SpatialRelationship> relationships = new ArrayList<>();

        // Entity-entity relationships
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                PerceivedEntity a = entities.get(i);
                PerceivedEntity b = entities.get(j);

                double distance = a.getPosition().distanceTo(b.getPosition());

                if (distance < 5.0) {
                    relationships.add(new SpatialRelationship(
                        a, b, RelationType.NEARBY, distance
                    ));
                }

                if (distance < 2.0 && a.isHostile() && b.isHostile()) {
                    relationships.add(new SpatialRelationship(
                        a, b, RelationType.GROUPED, distance
                    ));
                }
            }
        }

        // Entity-structure relationships
        for (PerceivedEntity entity : entities) {
            for (PerceivedStructure structure : analyzeStructures(blocks)) {
                if (structure.contains(entity.getPosition())) {
                    relationships.add(new SpatialRelationship(
                        entity, structure, RelationType.INSIDE, 0.0
                    ));
                }
            }
        }

        return relationships;
    }
}

/**
 * Result of perceptual processing.
 */
public class PerceptualSnapshot {
    private final List<PerceivedEntity> entities;
    private final List<PerceivedStructure> structures;
    private final List<RecognizedPattern> patterns;
    private final SpatialMap spatialMap;
    private final long processingTimeMs;

    public PerceptualSnapshot(List<PerceivedEntity> entities,
                             List<PerceivedStructure> structures,
                             List<RecognizedPattern> patterns,
                             SpatialMap spatialMap,
                             long processingTimeMs) {
        this.entities = entities;
        this.structures = structures;
        this.patterns = patterns;
        this.spatialMap = spatialMap;
        this.processingTimeMs = processingTimeMs;
    }

    public List<PerceivedEntity> getEntities() { return entities; }
    public List<PerceivedStructure> getStructures() { return structures; }
    public List<RecognizedPattern> getPatterns() { return patterns; }
    public SpatialMap getSpatialMap() { return spatialMap; }
    public long getProcessingTimeMs() { return processingTimeMs; }
}
```

### 7.5 Perception Layer Characteristics

**Advantages:**
- **Semantic understanding:** Transforms raw data into concepts
- **Reusable output:** Higher layers use semantic concepts
- **Improves with learning:** Pattern recognition gets better
- **Parallelizable:** Can run on separate thread

**Limitations:**
- **Computationally expensive:** 50-200ms processing time
- **May be wrong:** Pattern recognition can misclassify
- **Requires training:** Needs templates and patterns
- **Stateless:** Must reprocess every tick

**When to Use:**
- Every tick (provides ground truth for higher layers)
- Complex environments requiring understanding
- Situations with many entities/structures
- Novel environments requiring learning

---

## 8. Layer 5: Language Layer (100-500ms)

### 8.1 Purpose

The Language Layer handles **intention-to-language translation and dialogue generation**. It converts the agent's internal state and intentions into natural language that the player can understand.

### 8.2 Biological Inspiration

**Language Production:**
```
Intention (Pre-frontal Cortex)
    ↓
Conceptual Preparation (Wernicke's Area)
    ↓
Grammatical Encoding (Broca's Area)
    ↓
Phonological Encoding
    ↓
Articulation (Motor Cortex)
    ↓
Speech
```

**Processing Time:**
- **Word Retrieval:** 200ms
- **Sentence Formation:** 300-500ms
- **Complex Utterance:** 500ms-2s

**Dual Stream:**
- **Dorsal Stream:** Sensorimotor integration (speech production)
- **Ventral Stream:** Comprehension (speech understanding)

### 8.3 Minecraft Language Examples

**Task Commentary:**
```java
// Agent state: Mining iron ore
// Generates: "I'm mining some iron ore over here."
```

**Question Response:**
```java
// Player: "What are you doing?"
// Agent state: Building structure
// Generates: "I'm working on that shelter you asked for."
```

**Emotional Expression:**
```java
// Agent state: High fear, low health
// Generates: "I'm really scared, can you help me?"
```

### 8.4 Implementation

```java
package com.steve.ai.layers.language;

import java.util.List;
import java.util.ArrayList;

/**
 * Language Layer: Intention-to-language translation and dialogue generation.
 *
 * Characteristics:
 * - Response Time: 100-500ms (language generation)
 * - State: Conversation history (persistent)
 * - Learning: Conversational patterns
 * - Parallel: Runs asynchronously
 * - Override: Optional (can skip dialogue)
 */
public class LanguageLayer {

    private final DialogueGenerator generator;
    private final ConversationHistory history;
    private final IntentionTranslator translator;

    public LanguageLayer() {
        this.generator = new DialogueGenerator();
        this.history = new ConversationHistory();
        this.translator = new IntentionTranslator();
    }

    /**
     * Generates a response to player input.
     */
    public DialogueResponse respondToPlayer(String playerMessage,
                                           AgentState currentState) {
        // 1. Understand player message
        PlayerIntent intent = understandPlayerMessage(playerMessage);

        // 2. Update conversation history
        history.addPlayerMessage(playerMessage, intent);

        // 3. Generate response based on intent and state
        DialogueResponse response = generateResponse(intent, currentState);

        // 4. Record response
        history.addAgentMessage(response);

        return response;
    }

    /**
     * Generates unprompted commentary based on current action.
     */
    public DialogueCommentary generateCommentary(CurrentAction action,
                                                 AgentState state) {
        // Only comment sometimes (not every action)
        if (shouldComment(action, state)) {
            String commentary = translator.translateAction(action, state);
            return new DialogueCommentary(commentary, action);
        }

        return null;
    }

    private boolean shouldComment(CurrentAction action, AgentState state) {
        // Comment if:
        // - Starting new important task
        // - Just completed significant achievement
        // - Experiencing strong emotion
        // - Random chance for variety

        if (action.isSignificantTaskStart()) {
            return true;
        }

        if (action.justCompletedAchievement()) {
            return true;
        }

        if (state.getEmotionalState().getDominantEmotion().getIntensity() > 0.6) {
            return true;
        }

        // 10% chance to comment on routine actions
        return Math.random() < 0.1;
    }

    private DialogueResponse generateResponse(PlayerIntent intent,
                                             AgentState state) {
        // Generate response based on intent type
        switch (intent.getType()) {
            case QUESTION:
                return generator.generateAnswer(intent, state, history);

            case COMMAND:
                return generator.generateAcknowledgment(intent, state);

            case THREAT:
                return generator.generateThreatResponse(intent, state);

            case GREETING:
                return generator.generateGreeting(state, history);

            default:
                return generator.generateGenericResponse(intent, state);
        }
    }

    private PlayerIntent understandPlayerMessage(String message) {
        // Use simple pattern matching or LLM for understanding
        return IntentClassifier.classify(message);
    }
}

/**
 * Translates internal intentions into natural language.
 */
public class IntentionTranslator {

    /**
     * Translates current action into natural language description.
     */
    public String translateAction(CurrentAction action, AgentState state) {
        ActionTemplate template = action.getTemplate();
        Map<String, Object> parameters = action.getParameters();

        // Generate sentence from template
        String sentence = template.generateSentence(parameters);

        // Add emotional flavor if significant emotion present
        Emotion dominant = state.getEmotionalState().getDominantEmotion();
        if (dominant.getIntensity() > 0.5) {
            sentence = addEmotionalFlavor(sentence, dominant, state.getPersonality());
        }

        return sentence;
    }

    private String addEmotionalFlavor(String sentence, Emotion emotion,
                                      Personality personality) {
        switch (emotion.getType()) {
            case EXCITEMENT:
                return addExcitementFlavor(sentence, personality);
            case FEAR:
                return addFearFlavor(sentence, personality);
            case FRUSTRATION:
                return addFrustrationFlavor(sentence, personality);
            default:
                return sentence;
        }
    }

    private String addExcitementFlavor(String sentence, Personality personality) {
        // Add exclamation marks, enthusiastic words
        if (personality.isEnthusiastic()) {
            return sentence + " This is great!";
        }
        return sentence + "!";
    }

    private String addFearFlavor(String sentence, Personality personality) {
        // Add uncertainty, worry words
        if (personality.isVocalAboutFear()) {
            return "I'm a bit worried, but " + sentence.toLowerCase();
        }
        return sentence;
    }
}

/**
 * Generates dialogue responses.
 */
public class DialogueGenerator {

    private final ResponseTemplates templates;

    public DialogueGenerator() {
        this.templates = new ResponseTemplates();
    }

    public DialogueResponse generateAnswer(PlayerIntent intent,
                                          AgentState state,
                                          ConversationHistory history) {
        String question = intent.getQuestion();

        // Check if we know the answer
        if (state.knowsAnswer(question)) {
            String answer = state.getAnswer(question);
            return new DialogueResponse(answer, DialogueType.ANSWER);
        }

        // Don't know, generate appropriate response
        String response = templates.generateDontKnow(question, state);
        return new DialogueResponse(response, DialogueType.DONT_KNOW);
    }

    public DialogueResponse generateAcknowledgment(PlayerIntent intent,
                                                   AgentState state) {
        Command command = intent.getCommand();

        // Generate acknowledgment based on command type
        String ack = templates.generateAcknowledgment(command, state);

        return new DialogueResponse(ack, DialogueType.ACKNOWLEDGMENT);
    }
}

/**
 * Manages conversation history for context.
 */
public class ConversationHistory {

    private final List<ConversationTurn> turns;
    private static final int MAX_HISTORY = 20;

    public ConversationHistory() {
        this.turns = new ArrayList<>();
    }

    public void addPlayerMessage(String message, PlayerIntent intent) {
        turns.add(new ConversationTurn(
            Speaker.PLAYER,
            message,
            intent,
            System.currentTimeMillis()
        ));

        trimHistory();
    }

    public void addAgentMessage(DialogueResponse response) {
        turns.add(new ConversationTurn(
            Speaker.AGENT,
            response.getMessage(),
            response.getIntent(),
            System.currentTimeMillis()
        ));

        trimHistory();
    }

    private void trimHistory() {
        while (turns.size() > MAX_HISTORY) {
            turns.remove(0);
        }
    }

    public List<ConversationTurn> getRecentTurns(int count) {
        int fromIndex = Math.max(0, turns.size() - count);
        return turns.subList(fromIndex, turns.size());
    }

    public ConversationTurn getLastTurn() {
        if (turns.isEmpty()) {
            return null;
        }
        return turns.get(turns.size() - 1);
    }
}

/**
 * Represents a dialogue response.
 */
public class DialogueResponse {
    private final String message;
    private final DialogueType type;
    private final PlayerIntent intent;

    public DialogueResponse(String message, DialogueType type) {
        this(message, type, null);
    }

    public DialogueResponse(String message, DialogueType type, PlayerIntent intent) {
        this.message = message;
        this.type = type;
        this.intent = intent;
    }

    public String getMessage() { return message; }
    public DialogueType getType() { return type; }
    public PlayerIntent getIntent() { return intent; }
}
```

### 8.5 Language Layer Characteristics

**Advantages:**
- **Player communication:** Enables dialogue
- **Transparency:** Player can understand agent reasoning
- **Personality expression:** Dialogue shows character
- **Social connection:** Creates emotional bond

**Limitations:**
- **Optional for functionality:** Agent can work without dialogue
- **Can be repetitive:** Needs variety system
- **May be misunderstood:** Language interpretation issues
- **Latency:** 100-500ms generation time

**When to Use:**
- Player interaction (questions, commands)
- Significant events (achievements, dangers)
- Emotional expression (strong emotions)
- Task updates (progress reports)

---

## 9. Layer 6: Planning Layer (500ms-10s)

### 9.1 Purpose

The Planning Layer handles **strategic thinking, novel problem solving, and LLM reasoning**. This is the slowest layer but handles situations where no cached pattern exists—novel problems requiring creative solutions.

### 9.2 Biological Inspiration

**Deliberate Reasoning:**
```
Problem Representation (Prefrontal Cortex)
    ↓
Goal Decomposition (PFC)
    ↓
Solution Generation (Working Memory + Long-Term Memory)
    ↓
Solution Evaluation (PFC)
    ↓
Plan Selection and Execution
```

**Processing Time:**
- **Simple reasoning:** 500ms-1s
- **Complex problem solving:** 2-10s
- **Novel creative thinking:** 10s-60s

**Working Memory Limits:**
- 7±2 items (Miller, 1956)
- 10-30 second duration without rehearsal

### 9.3 Minecraft Planning Examples

**Novel Building Task:**
```
Player: "Build me a castle near the mountain"
→ Planning Layer activated (no cached pattern)
→ LLM generates plan:
  1. Survey mountain location
  2. Design castle layout
  3. Gather materials (stone, wood, glass)
  4. Build foundation
  5. Construct walls
  6. Add towers
  7. Install defenses
→ Delegates to Pattern Layer for execution
```

**Novel Combat Situation:**
```
Situation: New mob type encountered
→ Planning Layer activated
→ LLM analyzes mob behavior
→ Generates combat strategy
→ Caches successful pattern for future
```

### 9.4 Implementation

```java
package com.steve.ai.layers.planning;

import java.util.concurrent.CompletableFuture;

/**
 * Planning Layer: Strategic thinking and LLM-based novel problem solving.
 *
 * This is the "One Abstraction Away" layer—it sits one level above
 * traditional game AI (patterns, FSM, behavior trees) and uses LLMs
 * to generate, refine, and improve those systems.
 *
 * Characteristics:
 * - Response Time: 500ms-10s (LLM reasoning)
 * - State: Conversation context (persistent)
 * - Learning: One-shot learning from outcomes
 * - Parallel: Runs asynchronously (doesn't block execution)
 * - Override: Can wait (not time-critical)
 */
public class PlanningLayer {

    private final LLMClient llmClient;
    private final PlanCache planCache;
    private final PlanExecutor executor;
    private final PlanRefiner refiner;

    public PlanningLayer(LLMClient llmClient) {
        this.llmClient = llmClient;
        this.planCache = new PlanCache();
        this.executor = new PlanExecutor();
        this.refiner = new PlanRefiner();
    }

    /**
     * Plans a strategy for achieving the given goal.
     * Returns a CompletableFuture that completes when planning is done.
     * Runs asynchronously—doesn't block the agent from continuing execution.
     */
    public CompletableFuture<Plan> planAsync(Goal goal, PlanningContext context) {
        // First, check cache
        Plan cachedPlan = planCache.get(goal, context);
        if (cachedPlan != null) {
            return CompletableFuture.completedFuture(cachedPlan);
        }

        // Not in cache, need to plan
        return CompletableFuture.supplyAsync(() -> {
            // Build planning prompt
            String prompt = buildPlanningPrompt(goal, context);

            // Call LLM
            LLMResponse response = llmClient.complete(prompt);

            // Parse plan from response
            Plan plan = parsePlan(response, goal);

            // Cache for future use
            planCache.put(goal, context, plan);

            return plan;
        });
    }

    /**
     * Interrupts current planning if situation changes.
     */
    public void interrupt(String reason, String newPriority) {
        // Signal to any ongoing planning to stop
        // Start new planning for new priority
    }

    /**
     * Refines an existing plan based on execution feedback.
     */
    public Plan refinePlan(Plan originalPlan, ExecutionFeedback feedback) {
        // Use LLM to refine plan based on what worked and what didn't
        String refinementPrompt = buildRefinementPrompt(originalPlan, feedback);
        LLMResponse response = llmClient.complete(refinementPrompt);
        Plan refinedPlan = parsePlan(response, originalPlan.getGoal());

        // Update cache with refined plan
        planCache.put(originalPlan.getGoal(), originalPlan.getContext(), refinedPlan);

        return refinedPlan;
    }

    private String buildPlanningPrompt(Goal goal, PlanningContext context) {
        StringBuilder prompt = new StringBuilder();

        // System prompt
        prompt.append("You are a Minecraft AI agent planning how to achieve a goal.\n");
        prompt.append("Generate a step-by-step plan using available actions.\n\n");

        // Goal description
        prompt.append("Goal: ").append(goal.getDescription()).append("\n");

        // Current context
        prompt.append("Current Situation:\n");
        prompt.append(context.describeSituation()).append("\n");

        // Available resources
        prompt.append("Available Resources:\n");
        prompt.append(context.describeResources()).append("\n");

        // Available actions
        prompt.append("Available Actions:\n");
        prompt.append(context.describeAvailableActions()).append("\n");

        // Output format
        prompt.append("\nGenerate a plan as a JSON array of action steps:\n");
        prompt.append("[\n");
        prompt.append("  {\"action\": \"action_name\", \"parameters\": {...}, \"reasoning\": \"...\"},\n");
        prompt.append("  ...\n");
        prompt.append("]\n");

        return prompt.toString();
    }

    private String buildRefinementPrompt(Plan originalPlan, ExecutionFeedback feedback) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Refine the following plan based on execution feedback:\n\n");
        prompt.append("Original Plan:\n");
        prompt.append(originalPlan.toJson()).append("\n\n");

        prompt.append("Execution Feedback:\n");
        prompt.append(feedback.describe()).append("\n\n");

        prompt.append("Generate a refined plan that addresses the failures:\n");

        return prompt.toString();
    }

    private Plan parsePlan(LLMResponse response, Goal goal) {
        // Parse LLM response into structured plan
        return PlanParser.parse(response.getText(), goal);
    }
}

/**
 * Caches plans for reuse.
 */
public class PlanCache {

    private final Map<String, Plan> cache;

    public PlanCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public Plan get(Goal goal, PlanningContext context) {
        String key = generateCacheKey(goal, context);
        return cache.get(key);
    }

    public void put(Goal goal, PlanningContext context, Plan plan) {
        String key = generateCacheKey(goal, context);
        cache.put(key, plan);
    }

    private String generateCacheKey(Goal goal, PlanningContext context) {
        return goal.getType() + "_" + context.getRelevantFactorsHash();
    }
}

/**
 * Executes plans by delegating to Pattern Layer.
 */
public class PlanExecutor {

    private final PatternLayer patternLayer;

    public PlanExecutor(PatternLayer patternLayer) {
        this.patternLayer = patternLayer;
    }

    /**
     * Executes a plan step by step.
     * Returns future that completes when plan is done.
     */
    public CompletableFuture<PlanResult> execute(Plan plan) {
        return CompletableFuture.supplyAsync(() -> {
            List<StepResult> stepResults = new ArrayList<>();

            for (PlanStep step : plan.getSteps()) {
                // Try to execute using pattern layer
                PatternResult patternResult = patternLayer.tryExecutePattern(
                    step.getActionContext()
                );

                StepResult stepResult;
                if (patternResult.isSuccess()) {
                    stepResult = StepResult.success(patternResult);
                } else {
                    // Pattern failed, need to execute directly
                    stepResult = executeStepDirectly(step);
                }

                stepResults.add(stepResult);

                // Stop if critical failure
                if (stepResult.isCriticalFailure()) {
                    return PlanResult.partialFailure(stepResults);
                }
            }

            return PlanResult.success(stepResults);
        });
    }

    private StepResult executeStepDirectly(PlanStep step) {
        // Execute action directly without pattern cache
        try {
            ActionResult result = step.getAction().execute();
            if (result.isSuccess()) {
                return StepResult.success(result);
            } else {
                return StepResult.failure(result.getError());
            }
        } catch (Exception e) {
            return StepResult.error(e);
        }
    }
}

/**
 * Refines plans based on execution outcomes.
 */
public class PlanRefiner {

    private final PlanningLayer planningLayer;

    public PlanRefiner(PlanningLayer planningLayer) {
        this.planningLayer = planningLayer;
    }

    /**
     * Learns from plan execution and updates pattern cache.
     */
    public void learnFromExecution(Plan plan, PlanResult result) {
        if (result.isSuccess()) {
            // Successful plan—cache as pattern
            ActionSequence sequence = extractSequence(plan);
            planningLayer.getPatternLayer().recordSequence(sequence);
        } else {
            // Failed plan—refine
            Plan refined = planningLayer.refinePlan(plan, result.getFeedback());
        }
    }

    private ActionSequence extractSequence(Plan plan) {
        List<Action> actions = new ArrayList<>();
        for (PlanStep step : plan.getSteps()) {
            actions.add(step.getAction());
        }
        return new ActionSequence(actions);
    }
}

/**
 * Represents a planning goal.
 */
public class Goal {
    private final String type;
    private final String description;
    private final Map<String, Object> parameters;

    public Goal(String type, String description, Map<String, Object> parameters) {
        this.type = type;
        this.description = description;
        this.parameters = parameters;
    }

    public String getType() { return type; }
    public String getDescription() { return description; }
    public Map<String, Object> getParameters() { return parameters; }
}

/**
 * Represents a plan with steps.
 */
public class Plan {
    private final Goal goal;
    private final List<PlanStep> steps;
    private final PlanningContext context;

    public Plan(Goal goal, List<PlanStep> steps, PlanningContext context) {
        this.goal = goal;
        this.steps = steps;
        this.context = context;
    }

    public Goal getGoal() { return goal; }
    public List<PlanStep> getSteps() { return steps; }
    public PlanningContext getContext() { return context; }

    public String toJson() {
        // Serialize plan to JSON
        return toJsonString();
    }
}

/**
 * Represents a single step in a plan.
 */
public class PlanStep {
    private final String actionName;
    private final Map<String, Object> parameters;
    private final String reasoning;

    public PlanStep(String actionName, Map<String, Object> parameters, String reasoning) {
        this.actionName = actionName;
        this.parameters = parameters;
        this.reasoning = reasoning;
    }

    public String getActionName() { return actionName; }
    public Map<String, Object> getParameters() { return parameters; }
    public String getReasoning() { return reasoning; }

    public Action getAction() {
        return ActionFactory.create(actionName, parameters);
    }

    public SituationContext getActionContext() {
        return new SituationContext(actionName, parameters);
    }
}

/**
 * Context for planning.
 */
public class PlanningContext {
    private final SituationSnapshot situation;
    private final ResourceInventory resources;
    private final List<String> availableActions;

    public PlanningContext(SituationSnapshot situation,
                          ResourceInventory resources,
                          List<String> availableActions) {
        this.situation = situation;
        this.resources = resources;
        this.availableActions = availableActions;
    }

    public String describeSituation() {
        return situation.toDescription();
    }

    public String describeResources() {
        return resources.toDescription();
    }

    public String describeAvailableActions() {
        return String.join(", ", availableActions);
    }

    public int getRelevantFactorsHash() {
        return Objects.hash(situation.getHash(), resources.getHash());
    }
}
```

### 9.5 Planning Layer Characteristics

**Advantages:**
- **Novelty handling:** Can solve new problems
- **Creativity:** Generates innovative solutions
- **One-shot learning:** Learns from single examples
- **Explanation:** Can reason about choices

**Limitations:**
- **Slow:** 500ms-10s planning time
- **Expensive:** LLM API costs
- **Unreliable:** May generate invalid plans
- **Requires async:** Can't block execution

**When to Use:**
- Novel situations without cached patterns
- Complex multi-step problems
- Creative tasks (building, exploration)
- Strategic decisions

---

## 10. Layer 7: Meta Layer (Continuous)

### 10.1 Purpose

The Meta Layer provides **continuous self-monitoring, pattern refinement, and learning from outcomes**. It never "finishes"—it's always observing, learning, and improving all other layers.

### 10.2 Biological Inspiration

**Metacognition:**
```
Executive Function (Prefrontal Cortex)
    ↓
Self-Monitoring (Anterior Cingulate Cortex)
    ↓
Error Detection ("That felt wrong")
    ↓
Strategy Adjustment ("Try a different approach")
    ↓
Learning (Synaptic plasticity across all brain areas)
```

**Neuroplasticity:**
- **Synaptic strengthening:** Repeated patterns become stronger (LTP)
- **Synaptic pruning:** Unused patterns weaken (LTD)
- **Cortical remapping:** Brain areas reorganize based on experience

### 10.3 Meta-Learning Examples

**Pattern Optimization:**
```
Observation: "bridge_gap" pattern fails 30% of the time
Analysis: Fails when gap > 4 blocks
Action: Update pattern to check gap size first
Result: Failure rate drops to 5%
```

**Emotional Learning:**
```
Observation: Fear of dark causes unnecessary torch placement
Analysis: Player never encounters monsters in lit areas
Action: Decrease fear_of_dark decay rate
Result: More efficient behavior
```

**Strategy Refinement:**
```
Observation: Direct combat strategies have high failure rate
Analysis: Agent has low combat skill
Action: Prioritize avoidance and defensive tactics
Result: Survival rate increases 40%
```

### 10.4 Implementation

```java
package com.steve.ai.layers.meta;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Meta Layer: Continuous self-monitoring and learning from outcomes.
 *
 * This layer is never "done"—it always runs in the background,
 * observing outcomes and refining all other layers.
 *
 * Characteristics:
 * - Response Time: Continuous (background processing)
 * - State: Persistent learning (saved to disk)
 * - Learning: All outcomes contribute to refinement
 * - Parallel: Always running
 * - Override: Influences all layers
 */
public class MetaLayer {

    private final OutcomeTracker outcomeTracker;
    private final PatternRefiner patternRefiner;
    private final EmotionalLearner emotionalLearner;
    private final StrategyOptimizer strategyOptimizer;
    private final PerceptionTrainer perceptionTrainer;

    private final ScheduledExecutorService scheduler;

    public MetaLayer(PatternLayer patternLayer,
                     StateLayer stateLayer,
                     PlanningLayer planningLayer,
                     PerceptionLayer perceptionLayer) {
        this.outcomeTracker = new OutcomeTracker();
        this.patternRefiner = new PatternRefiner(patternLayer, outcomeTracker);
        this.emotionalLearner = new EmotionalLearner(stateLayer, outcomeTracker);
        this.strategyOptimizer = new StrategyOptimizer(planningLayer, outcomeTracker);
        this.perceptionTrainer = new PerceptionTrainer(perceptionLayer, outcomeTracker);

        // Start continuous monitoring
        this.scheduler = createScheduler();
    }

    /**
     * Records an action outcome for learning.
     */
    public void recordOutcome(ActionOutcome outcome) {
        outcomeTracker.record(outcome);

        // Trigger immediate analysis if outcome is significant
        if (outcome.isSignificant()) {
            analyzeOutcome(outcome);
        }
    }

    /**
     * Analyzes a single outcome immediately.
     */
    private void analyzeOutcome(ActionOutcome outcome) {
        if (outcome.isSuccess()) {
            // Learn from success
            if (outcome.usedPattern()) {
                patternRefiner.reinforcePattern(outcome.getPatternId());
            }
        } else {
            // Learn from failure
            if (outcome.usedPattern()) {
                patternRefiner.analyzePatternFailure(outcome);
            }

            if (outcome.usedPlan()) {
                strategyOptimizer.analyzePlanFailure(outcome);
            }
        }
    }

    /**
     * Performs periodic batch learning and refinement.
     */
    private void performPeriodicLearning() {
        // Analyze recent outcomes
        List<ActionOutcome> recentOutcomes = outcomeTracker.getRecentOutcomes(1000);

        // Refine patterns
        patternRefiner.refinePatternsFromOutcomes(recentOutcomes);

        // Learn emotional associations
        emotionalLearner.learnFromOutcomes(recentOutcomes);

        // Optimize strategies
        strategyOptimizer.optimizeStrategies(recentOutcomes);

        // Train perception
        perceptionTrainer.trainFromOutcomes(recentOutcomes);

        // Save learning to disk
        saveLearningState();
    }

    private ScheduledExecutorService createScheduler() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        // Analyze outcomes every 10 seconds
        scheduler.scheduleAtFixedRate(
            this::performPeriodicLearning,
            10, 10, TimeUnit.SECONDS
        );

        // Save learning state every minute
        scheduler.scheduleAtFixedRate(
            this::saveLearningState,
            60, 60, TimeUnit.SECONDS
        );

        return scheduler;
    }

    private void saveLearningState() {
        // Save all learned improvements to disk
        LearningState state = new LearningState();
        state.setPatternStatistics(patternRefiner.getStatistics());
        state.setEmotionalAssociations(emotionalLearner.getAssociations());
        state.setStrategyPreferences(strategyOptimizer.getPreferences());
        state.setPerceptionModels(perceptionTrainer.getModels());

        state.saveToFile("learning_state.json");
    }

    public void shutdown() {
        scheduler.shutdown();
        saveLearningState();
    }
}

/**
 * Tracks action outcomes for learning.
 */
public class OutcomeTracker {

    private final Queue<ActionOutcome> recentOutcomes;
    private final Map<String, OutcomeStatistics> statistics;

    public OutcomeTracker() {
        this.recentOutcomes = new ConcurrentLinkedQueue<>();
        this.statistics = new ConcurrentHashMap<>();
    }

    public void record(ActionOutcome outcome) {
        recentOutcomes.add(outcome);

        // Update statistics
        String key = outcome.getKey();
        OutcomeStatistics stats = statistics.computeIfAbsent(key, k -> new OutcomeStatistics());
        stats.record(outcome);

        // Trim recent outcomes
        while (recentOutcomes.size() > 10000) {
            recentOutcomes.poll();
        }
    }

    public List<ActionOutcome> getRecentOutcomes(int limit) {
        return recentOutcomes.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    public OutcomeStatistics getStatistics(String key) {
        return statistics.get(key);
    }
}

/**
 * Refines patterns based on outcomes.
 */
public class PatternRefiner {

    private final PatternLayer patternLayer;
    private final OutcomeTracker outcomeTracker;

    public PatternRefiner(PatternLayer patternLayer, OutcomeTracker outcomeTracker) {
        this.patternLayer = patternLayer;
        this.outcomeTracker = outcomeTracker;
    }

    /**
     * Reinforces a pattern that succeeded.
     */
    public void reinforcePattern(String patternId) {
        // Increase pattern's success confidence
        patternLayer.updatePatternConfidence(patternId, +0.1);

        // Decrease likelihood of pattern being evicted from cache
        patternLayer.markPatternUsed(patternId);
    }

    /**
     * Analyzes pattern failure and potentially invalidates pattern.
     */
    public void analyzePatternFailure(ActionOutcome outcome) {
        String patternId = outcome.getPatternId();
        OutcomeStatistics stats = outcomeTracker.getStatistics(patternId);

        // If pattern is failing more than succeeding
        if (stats.getFailureRate() > 0.5 && stats.getTotalCount() > 5) {
            // Invalidate pattern
            patternLayer.invalidatePattern(patternId);

            // Log reason
            System.out.println("Invalidated pattern " + patternId +
                " due to high failure rate: " + stats.getFailureRate());
        }
    }

    /**
     * Batch refines patterns from multiple outcomes.
     */
    public void refinePatternsFromOutcomes(List<ActionOutcome> outcomes) {
        // Group outcomes by pattern
        Map<String, List<ActionOutcome>> byPattern = outcomes.stream()
            .filter(o -> o.usedPattern())
            .collect(Collectors.groupingBy(ActionOutcome::getPatternId));

        // Analyze each pattern
        for (Map.Entry<String, List<ActionOutcome>> entry : byPattern.entrySet()) {
            String patternId = entry.getKey();
            List<ActionOutcome> patternOutcomes = entry.getValue();

            long successes = patternOutcomes.stream().filter(ActionOutcome::isSuccess).count();
            long failures = patternOutcomes.size() - successes;
            double failureRate = (double) failures / patternOutcomes.size();

            if (failureRate > 0.5) {
                patternLayer.invalidatePattern(patternId);
            } else if (failureRate > 0.2) {
                // Reduce confidence but keep pattern
                patternLayer.updatePatternConfidence(patternId, -0.1);
            }
        }
    }

    public PatternStatistics getStatistics() {
        return patternLayer.getStatistics();
    }
}

/**
 * Learns emotional associations from outcomes.
 */
public class EmotionalLearner {

    private final StateLayer stateLayer;
    private final OutcomeTracker outcomeTracker;
    private final Map<String, EmotionalAssociation> associations;

    public EmotionalLearner(StateLayer stateLayer, OutcomeTracker outcomeTracker) {
        this.stateLayer = stateLayer;
        this.outcomeTracker = outcomeTracker;
        this.associations = new ConcurrentHashMap<>();
    }

    /**
     * Learns emotional associations from outcomes.
     */
    public void learnFromOutcomes(List<ActionOutcome> outcomes) {
        for (ActionOutcome outcome : outcomes) {
            // Associate emotional state with outcome
            Map<Emotion, Double> emotionsAtTime = outcome.getEmotionalState();

            for (Map.Entry<Emotion, Double> entry : emotionsAtTime.entrySet()) {
                Emotion emotion = entry.getKey();
                double intensity = entry.getValue();

                if (intensity > 0.5) {
                    // Strong emotion was present
                    String contextKey = outcome.getContextKey();

                    EmotionalAssociation association = associations.computeIfAbsent(
                        contextKey,
                        k -> new EmotionalAssociation(contextKey)
                    );

                    // Update association based on outcome
                    if (outcome.isSuccess()) {
                        association.associateEmotionWithSuccess(emotion, intensity);
                    } else {
                        association.associateEmotionWithFailure(emotion, intensity);
                    }
                }
            }
        }

        // Apply learned associations to state layer
        applyAssociations();
    }

    private void applyAssociations() {
        for (EmotionalAssociation association : associations.values()) {
            if (association.isConfident()) {
                stateLayer.updateEmotionalAssociation(
                    association.getContext(),
                    association.getLearnedEmotion()
                );
            }
        }
    }

    public Map<String, EmotionalAssociation> getAssociations() {
        return Map.copyOf(associations);
    }
}

/**
 * Optimizes strategies based on outcomes.
 */
public class StrategyOptimizer {

    private final PlanningLayer planningLayer;
    private final OutcomeTracker outcomeTracker;
    private final Map<String, StrategyPreference> preferences;

    public StrategyOptimizer(PlanningLayer planningLayer, OutcomeTracker outcomeTracker) {
        this.planningLayer = planningLayer;
        this.outcomeTracker = outcomeTracker;
        this.preferences = new ConcurrentHashMap<>();
    }

    /**
     * Learns which strategies work best in which contexts.
     */
    public void optimizeStrategies(List<ActionOutcome> outcomes) {
        for (ActionOutcome outcome : outcomes) {
            if (outcome.usedPlan()) {
                String strategyType = outcome.getStrategyType();
                String contextType = outcome.getContextType();

                String key = strategyType + "_" + contextType;
                StrategyPreference preference = preferences.computeIfAbsent(
                    key,
                    k -> new StrategyPreference(strategyType, contextType)
                );

                if (outcome.isSuccess()) {
                    preference.recordSuccess();
                } else {
                    preference.recordFailure();
                }
            }
        }

        // Apply learned preferences
        applyPreferences();
    }

    private void applyPreferences() {
        for (StrategyPreference preference : preferences.values()) {
            if (preference.isConfident()) {
                planningLayer.updateStrategyPreference(
                    preference.getStrategyType(),
                    preference.getContextType(),
                    preference.getPreferenceScore()
                );
            }
        }
    }

    public Map<String, StrategyPreference> getPreferences() {
        return Map.copyOf(preferences);
    }
}

/**
 * Trains perception models from outcomes.
 */
public class PerceptionTrainer {

    private final PerceptionLayer perceptionLayer;
    private final OutcomeTracker outcomeTracker;
    private final Map<String, PerceptionModel> models;

    public PerceptionTrainer(PerceptionLayer perceptionLayer, OutcomeTracker outcomeTracker) {
        this.perceptionLayer = perceptionLayer;
        this.outcomeTracker = outcomeTracker;
        this.models = new ConcurrentHashMap<>();
    }

    /**
     * Trains perception models from outcome feedback.
     */
    public void trainFromOutcomes(List<ActionOutcome> outcomes) {
        for (ActionOutcome outcome : outcomes) {
            // Get perception at time of action
            PerceptualSnapshot perception = outcome.getPerception();

            // If outcome reveals perception was wrong
            if (outcome.revealsPerceptionError()) {
                // Correct the perception model
                String errorType = outcome.getPerceptionErrorType();
                PerceptionModel model = models.computeIfAbsent(
                    errorType,
                    k -> new PerceptionModel(errorType)
                );

                model.correct(perception, outcome.getReality());
            }
        }

        // Apply trained models
        applyModels();
    }

    private void applyModels() {
        for (PerceptionModel model : models.values()) {
            if (model.isTrained()) {
                perceptionLayer.updateModel(model);
            }
        }
    }

    public Map<String, PerceptionModel> getModels() {
        return Map.copyOf(models);
    }
}

/**
 * Represents an action outcome for learning.
 */
public class ActionOutcome {

    private final String actionType;
    private final boolean success;
    private final String patternId;
    private final String planId;
    private final Map<Emotion, Double> emotionalState;
    private final PerceptualSnapshot perception;
    private final long timestamp;

    public ActionOutcome(String actionType, boolean success,
                        String patternId, String planId,
                        Map<Emotion, Double> emotionalState,
                        PerceptualSnapshot perception) {
        this.actionType = actionType;
        this.success = success;
        this.patternId = patternId;
        this.planId = planId;
        this.emotionalState = emotionalState;
        this.perception = perception;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isSuccess() { return success; }
    public boolean isSignificant() { return /* ... */ false; }
    public boolean usedPattern() { return patternId != null; }
    public boolean usedPlan() { return planId != null; }
    public String getPatternId() { return patternId; }
    public String getPlanId() { return planId; }
    public Map<Emotion, Double> getEmotionalState() { return emotionalState; }
    public PerceptualSnapshot getPerception() { return perception; }

    public String getKey() {
        return actionType + (patternId != null ? "_" + patternId : "");
    }
}
```

### 10.5 Meta Layer Characteristics

**Advantages:**
- **Continuous improvement:** Agent gets better over time
- **Multi-layer learning:** Improves all aspects of behavior
- **Persistent:** Learning saved to disk
- **Self-optimizing:** No manual tuning needed

**Limitations:**
- **Computationally expensive:** Continuous background processing
- **Complex:** Many interacting systems
- **Slow to show effects:** Takes time to accumulate data
- **Requires storage:** Disk space for learning state

**When to Use:**
- Always running (continuous background process)
- Long-running agents (hours/days/weeks)
- Situations requiring adaptation
- Multi-agent systems (share learning)

---

## 11. Data Flow and Integration

### 11.1 Tick-by-Tick Execution Flow

```
Each Game Tick (16.67ms @ 60fps):
┌─────────────────────────────────────────────────────────────────┐
│ 1. REFLEX LAYER (0ms)                                            │
│    - Check immediate dangers                                      │
│    - If reflex triggered: Execute, skip to 7                     │
│    - Else: Continue to 2                                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓ (if no reflex)
┌─────────────────────────────────────────────────────────────────┐
│ 2. PATTERN LAYER (1-10ms)                                        │
│    - Check for cached patterns                                   │
│    - If pattern found: Execute, continue to 3                   │
│    - Else: Skip to 4                                             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. STATE LAYER (10-100ms)                                        │
│    - Update emotional weights (decay)                            │
│    - Apply modulations to executed action                        │
│    - Continue to 4                                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. PERCEPTION LAYER (50-200ms) - Runs on separate thread       │
│    - Processing current sensory input                           │
│    - Building perceptual snapshot                               │
│    - Will be available next tick                                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. LANGUAGE LAYER (100-500ms) - Optional, asynchronous         │
│    - If player message waiting: Generate response              │
│    - If strong emotion: Generate commentary                    │
│    - Else: Skip                                                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. PLANNING LAYER (500ms-10s) - Asynchronous                    │
│    - If planning in progress: Check if done                    │
│    - If no plan and need one: Start planning                   │
│    - If plan ready: Begin execution                             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. META LAYER (Continuous) - Background                         │
│    - Record action outcome                                      │
│    - Periodic learning: Every 10 seconds                        │
│    - Save state: Every minute                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 11.2 Inter-Layer Communication

**Reflex → State:**
```java
if (reflexLayer.checkFallReflex().triggered) {
    stateLayer.updateEmotion(Emotion.FEAR, 0.3);  // Fear of heights
}
```

**State → Pattern:**
```java
double speedModulation = stateLayer.getModulation(ModulationTarget.MOVEMENT_SPEED);
pattern.executeWithSpeedModifier(speedModulation);
```

**Perception → Planning:**
```java
PerceptualSnapshot perception = perceptionLayer.processPerception(sensoryInput);
if (planningLayer.isPlanning()) {
    planningLayer.updateContext(perception);
}
```

**Planning → Pattern:**
```java
Plan plan = planningLayer.getCurrentPlan();
PlanStep currentStep = plan.getCurrentStep();
patternLayer.tryExecutePattern(currentStep.getActionContext());
```

**Pattern → Meta:**
```java
ActionOutcome outcome = patternLayer.getLastOutcome();
metaLayer.recordOutcome(outcome);
```

**Meta → All Layers:**
```java
// Periodic refinement
metaLayer.refinePatterns();
metaLayer.updateEmotionalAssociations();
metaLayer.optimizeStrategies();
metaLayer.trainPerception();
```

### 11.3 Async Execution Patterns

**Planning While Executing:**
```java
// Start planning for next goal while executing current plan
CompletableFuture<Plan> futurePlan = planningLayer.planAsync(nextGoal, context);

// Continue executing current plan
currentPlanExecutor.tick();

// Check if future plan is ready
if (futurePlan.isDone()) {
    Plan nextPlan = futurePlan.get();
    planQueue.add(nextPlan);
}
```

**Perception on Separate Thread:**
```java
// Submit perception job to separate thread
CompletableFuture<PerceptualSnapshot> futurePerception =
    CompletableFuture.supplyAsync(() -> perceptionLayer.processPerception(input));

// Continue with other layers
reflexLayer.tick();
patternLayer.tick();

// Get perception when ready (next tick)
PerceptualSnapshot perception = futurePerception.get();
perceptualCache.set(perception);
```

### 11.4 Priority and Override System

```java
public class LayerPriority {
    public static final int REFLEX = 1000;        // Highest
    public static final int PATTERN = 800;
    public static final int STATE = 600;
    public static final int PERCEPTION = 400;
    public static final int LANGUAGE = 200;
    public static final int PLANNING = 100;        // Lowest
}

public class ActionDecision {
    private final String action;
    private final int priority;
    private final String sourceLayer;

    public boolean overrides(ActionDecision other) {
        return this.priority > other.priority;
    }
}
```

---

## 12. Implementation Guidance

### 12.1 Core Architecture

```java
package com.steve.ai.core;

/**
 * Main AI agent orchestrating all cognitive layers.
 */
public class MultiLayerAgent {

    // All layers
    private final ReflexLayer reflexLayer;
    private final PatternLayer patternLayer;
    private final StateLayer stateLayer;
    private final PerceptionLayer perceptionLayer;
    private final LanguageLayer languageLayer;
    private final PlanningLayer planningLayer;
    private final MetaLayer metaLayer;

    // Execution state
    private Plan currentPlan;
    private int currentPlanStep;

    public MultiLayerAgent(AgentConfig config) {
        this.reflexLayer = new ReflexLayer();
        this.patternLayer = new PatternLayer();
        this.stateLayer = new StateLayer(config.getPersonality());
        this.perceptionLayer = new PerceptionLayer();
        this.languageLayer = new LanguageLayer();
        this.planningLayer = new PlanningLayer(config.getLLMClient());
        this.metaLayer = new MetaLayer(patternLayer, stateLayer,
                                       planningLayer, perceptionLayer);
    }

    /**
     * Main tick function called every game tick.
     */
    public void tick() {
        // 1. Check reflexes
        ReflexResult reflex = reflexLayer.checkReflexes();
        if (reflex.shouldTrigger) {
            executeReflex(reflex);
            recordOutcome(reflex.toOutcome());
            return;  // Skip other layers
        }

        // 2. Try pattern execution
        PatternResult pattern = patternLayer.tryExecutePattern(getCurrentContext());
        if (pattern.isSuccess()) {
            recordOutcome(pattern.toOutcome());
        } else {
            // No pattern, need plan
            ensurePlanExists();
        }

        // 3. Update state layer
        stateLayer.tick(0.01667);  // One tick at 60fps

        // 4. Perception (separate thread)
        if (shouldUpdatePerception()) {
            updatePerceptionAsync();
        }

        // 5. Language (optional)
        if (hasPlayerMessage()) {
            respondToPlayerAsync();
        }

        // 6. Planning (async)
        advanceCurrentPlan();

        // 7. Meta layer (continuous, background)
        // Runs independently on scheduler
    }

    private void ensurePlanExists() {
        if (currentPlan == null || currentPlan.isComplete()) {
            // Start planning for current goal
            Goal goal = getCurrentGoal();
            planningLayer.planAsync(goal, getCurrentContext())
                .thenAccept(this::setCurrentPlan);
        }
    }

    private void advanceCurrentPlan() {
        if (currentPlan != null && !currentPlan.isComplete()) {
            PlanStep step = currentPlan.getStep(currentPlanStep);

            // Try to execute step using pattern layer
            PatternResult result = patternLayer.tryExecutePattern(
                step.getActionContext()
            );

            if (result.isSuccess()) {
                currentPlanStep++;
            } else {
                // Step failed, refine plan
                planningLayer.refinePlan(currentPlan, result.toFeedback());
            }
        }
    }

    private void recordOutcome(ActionOutcome outcome) {
        metaLayer.recordOutcome(outcome);
    }

    public void shutdown() {
        metaLayer.shutdown();
    }
}
```

### 12.2 Configuration

```java
package com.steve.ai.config;

/**
 * Configuration for multi-layer agent.
 */
public class AgentConfig {

    private PersonalityTraits personality;
    private LLMClient llmClient;
    private LayerConfig layerConfig;

    public static AgentConfig createDefault() {
        AgentConfig config = new AgentConfig();

        // Default personality
        config.personality = new PersonalityTraits(
            0.7,  // fearReactivity
            0.6,  // joyReactivity
            0.5,  // trustPropensity
            0.4,  // materialism
            0.5   // ambition
        );

        // LLM client
        config.llmClient = new OpenAIClient(System.getenv("OPENAI_API_KEY"));

        // Layer configuration
        config.layerConfig = new LayerConfig();
        config.layerConfig.setReflexEnabled(true);
        config.layerConfig.setPatternCacheSize(1000);
        config.layerConfig.setPerceptionUpdateRate(5);  // Every 5 ticks
        config.layerConfig.setPlanningTimeout(30000);  // 30 seconds

        return config;
    }

    public PersonalityTraits getPersonality() { return personality; }
    public LLMClient getLLMClient() { return llmClient; }
    public LayerConfig getLayerConfig() { return layerConfig; }
}
```

### 12.3 Testing Strategy

```java
package com.steve.ai.test;

/**
 * Testing strategy for multi-layer agent.
 */
public class MultiLayerTest {

    @Test
    public void testReflexLayer() {
        // Test that reflexes trigger immediately
        ReflexLayer layer = new ReflexLayer(testSensors, testActuators);

        // Simulate fall
        testSensors.setFallVelocity(-2.0);
        ReflexResult result = layer.checkReflexes();

        assertTrue(result.shouldTrigger);
        assertEquals("place_water_bucket", result.action);
    }

    @Test
    public void testPatternLayer() {
        // Test pattern caching and execution
        PatternLayer layer = new PatternLayer();

        // Execute sequence 3 times
        for (int i = 0; i < 3; i++) {
            ActionSequence sequence = createTestSequence();
            layer.recordSequence(sequence);
        }

        // Should now be cached
        SituationContext context = createTestContext();
        PatternResult result = layer.tryExecutePattern(context);

        assertTrue(result.isSuccess());
        assertTrue(result.wasCached());
        assertTrue(result.getExecutionTime() < 10.0);  // < 10ms
    }

    @Test
    public void testStateLayer() {
        // Test emotional modulation
        StateLayer layer = new StateLayer(testPersonality);

        // Trigger fear
        EmotionalEvent event = new EmotionalEvent("creeper_encounter", 1.0, true)
            .lifeThreatening();
        layer.appraiseEvent(event);

        // Fear should modulate reflex threshold
        double modulation = layer.getModulation(ModulationTarget.REFLEX_THRESHOLD);
        assertTrue(modulation < 1.0);  // Lower threshold
    }

    @Test
    public void testLayerInteraction() {
        // Test that layers work together
        MultiLayerAgent agent = createTestAgent();

        // Simulate dangerous situation
        testSensors.setIncomingProjectile(true);

        // Tick agent
        agent.tick();

        // Reflex should have triggered
        assertTrue(testActuators.shieldActivated());

        // State should have been updated
        assertTrue(agent.getStateLayer().getEmotion(Emotion.FEAR) > 0.0);

        // Outcome should have been recorded
        assertNotNull(agent.getMetaLayer().getLastOutcome());
    }
}
```

### 12.4 Performance Optimization

```java
package com.steve.ai.optimization;

/**
 * Performance optimizations for multi-layer agent.
 */
public class PerformanceOptimizations {

    /**
     * 1. Pattern Cache Tuning
     */
    public Cache<String, ExecutablePattern> createOptimizedPatternCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)  // Adjust based on memory constraints
            .expireAfterAccess(1, TimeUnit.HOURS)
            .recordStats()
            .executor(Runnable::run)  // Same thread for cache operations
            .build();
    }

    /**
     * 2. Perception Update Rate Control
     */
    public void setPerceptionUpdateRate(int ticksPerUpdate) {
        // Only update perception every N ticks
        this.perceptionUpdateInterval = ticksPerUpdate;
    }

    /**
     * 3. Async Planning with Timeout
     */
    public CompletableFuture<Plan> planWithTimeout(Goal goal, PlanningContext context) {
        return planningLayer.planAsync(goal, context)
            .orTimeout(30, TimeUnit.SECONDS)  // Don't wait forever
            .exceptionally(ex -> {
                if (ex instanceof TimeoutException) {
                    return generateFallbackPlan(goal);
                }
                throw new RuntimeException(ex);
            });
    }

    /**
     * 4. Batch Meta Layer Updates
     */
    public void scheduleMetaLayerUpdates() {
        // Instead of updating every tick, batch updates
        scheduler.scheduleAtFixedRate(
            () -> metaLayer.performPeriodicLearning(),
            10, 10, TimeUnit.SECONDS
        );
    }

    /**
     * 5. Priority-Based Execution
     */
    public void tick() {
        // Skip lower priority layers if time budget exceeded
        long startTime = System.nanoTime();

        reflexLayer.tick();
        if (timeBudgetExceeded(startTime, 5)) return;  // 5ms budget

        patternLayer.tick();
        if (timeBudgetExceeded(startTime, 10)) return;  // 10ms budget

        stateLayer.tick();
        // ... etc
    }

    private boolean timeBudgetExceeded(long startTimeNanos, int budgetMs) {
        long elapsed = (System.nanoTime() - startTimeNanos) / 1_000_000;
        return elapsed > budgetMs;
    }
}
```

---

## 13. Minecraft-Specific Applications

### 13.1 Survival Scenario

**Situation:** Agent encounters hostile mob

```
Tick 1: Perception identifies zombie at 8 blocks
        → State updates: Fear = 0.6

Tick 2: Planning starts (async)
        → "How should I handle this zombie?"

Tick 3: Pattern found: "combat_zombie_melee"
        → Execute: Equip sword, approach, attack

Tick 4: Combat continues
        → State: Fear decaying as combat succeeds

Tick 5: Zombie defeated
        → Meta: Records successful combat pattern
        → State: Joy +0.4 (achievement)

Tick 6+: Meta reinforces pattern
        → Future zombie encounters will use cached pattern
```

### 13.2 Building Scenario

**Situation:** Player commands "Build me a house"

```
Tick 1: Language layer parses command
        → Intent: BUILD_HOUSE
        → State: Excitement +0.5

Tick 2: Planning starts (async)
        → LLM generates house building plan
        → Takes 3 seconds...

Tick 3-180: While planning, agent performs useful tasks
        → Pattern: "gather_wood" (cached)
        → Pattern: "collect_cobblestone" (cached)

Tick 181: Planning completes
        → Plan: 47 steps (foundation, walls, roof, door, windows)

Tick 182+: Execute plan steps
        → Most steps use cached patterns
        → Novel steps use direct execution

Tick 500+: Meta layer learns
        → House building sequence cached as pattern
        → Next house will be 80% faster
```

### 13.3 Exploration Scenario

**Situation:** Agent exploring new biome

```
Tick 1: Perception identifies new biome (ice plains)
        → Pattern: No cached exploration pattern

Tick 2: Planning starts (async)
        → "How should I explore this biome safely?"

Tick 3-10: Reflex layer ensures safety
        → Avoid ice spikes (fall damage)
        → Stay away from polar bears (hostile)

Tick 11: Planning completes
        → Plan: Cautious exploration pattern

Tick 12+: Execute exploration
        → State layer: Cautiousness +0.7
        → Perception: Looking for resources
        → Language: "This biome is dangerous!"

Tick 100+: Meta layer learns
        → Ice plains exploration pattern cached
        → Emotional association: ice_plains + fear
```

### 13.4 Multi-Agent Coordination

**Situation:** Two agents building together

```
Agent A:
  - Planning: "I'll build the west wall"
  - State: Cooperation +0.8
  - Language: "I'll start on the west wall"

Agent B:
  - Perception: Sees Agent A building west wall
  - Planning: "I'll build the east wall then"
  - State: Cooperation +0.8
  - Language: "Good idea, I'll take the east wall"

Meta Layer (both):
  - Records successful collaboration
  - Strengthens cooperative patterns
  - Increases trust between agents
```

---

## 14. Comparison to MUD Automation Patterns

### 14.1 Historical MUD Automation

**Text-Based MUDs (Multi-User Dungeons): 1980s-2000s**

Early automated agents in text-based MUDs used layered approaches that prefigured modern multi-layer AI:

**Trigger Layer (Reflex):**
```
on "You see * approaching*" -> flee_fast
on "You are hungry" -> eat food
on "You are carrying *" -> loot_item
```

**Script Layer (Pattern):**
```
script "kill_goblin" {
    wait "goblin enters"
    cast "fireball"
    wait "goblin is dead"
    loot "all from corpse"
    goto "hunt_goblins"
}
```

**State Layer (Emotional):**
```
if (health < 30%) {
    set mood "fleeing"
    flee_threshold = 10  # Very sensitive
}
```

### 14.2 Modern Graphical Games

**Graphical MMOs (2000s-Present):**

```
┌─────────────────────────────────────────────────────────────┐
│  Botting Framework (e.g., HonorBuddy, Glider)              │
│                                                              │
│  Combat Routine (Reflex + Pattern)                          │
│  - Rotation: Ability 1 → 2 → 3 → 1 (cached sequence)       │
│  - Interrupts: Kick, counterspell (reflex)                  │
│  - Cooldowns: Track and optimize (pattern)                  │
│                                                              │
│  Navigation (Reflex + Perception)                           │
│  - Pathfinding: A* with waypoints (reflex avoidance)        │
│  - Terrain: Recognize obstacles (perception)                │
│                                                              │
│  Grinding (Pattern + Planning)                              │
│  - Kill sequence: Target → pull → kill → loot (pattern)    │
│  - Questing: LLM interprets quest log (planning)            │
│                                                              │
│  Anti-Detection (Meta Layer)                                │
│  - Human-like delays: Randomize execution times             │
│  - Behavior variation: Don't be perfectly consistent        │
│  - Social mimicking: Respond to chat occasionally           │
└─────────────────────────────────────────────────────────────┘
```

### 14.3 Key Parallels

| MUD/MMO Pattern | Multi-Layer Equivalent | Function |
|-----------------|------------------------|----------|
| **Triggers** | Reflex Layer | Immediate response to events |
| **Scripts** | Pattern Layer | Cached action sequences |
| **State Machines** | State Layer | Context-dependent behavior |
| **Pathfinding** | Perception Layer | Spatial understanding |
| **Quest Botting** | Planning Layer | Goal-directed behavior |
| **Anti-Detection** | Meta Layer | Behavioral optimization |

### 14.4 Key Differences

| Aspect | MUD/MMO Bots | Multi-Layer AI |
|--------|--------------|----------------|
| **Goal** | Efficient grinding | Believable behavior |
| **Learning** | Manual scripting | Automatic learning |
| **Perception** | Text parsing | Visual understanding |
| **Social** | Minimal detection avoidance | Rich personality expression |
| **Architecture** | Ad hoc, informal | Systematic, biologically inspired |

### 14.5 Lessons from MUD Automation

**1. Triggers Are Essential:**
MUD bots proved that immediate responses (triggers) are necessary for survival. Modern AI needs reflex layer for same reason.

**2. Scripts Beat Randomness:**
Scripted sequences (patterns) are far more efficient than random or reactive behavior. Caching successful sequences is crucial.

**3. State Matters:**
Health, mana, location all matter. State layer extends this to emotional weights for richer behavior.

**4. Perception Is Key:**
Text MUDs had easy perception (parsing text). 3D games need visual perception layer for semantic understanding.

**5. Planning Is Optional:**
Most MUD bots didn't need complex planning—good scripts were enough. Modern AI uses planning for novelty, not routine.

**6. Meta-Layer Prevents Detection:**
Anti-detection systems (meta layer) taught us that behavioral variation is essential for believability.

---

## 15. Evaluation and Metrics

### 15.1 Performance Metrics

| Layer | Target Latency | Measurement Method |
|-------|----------------|-------------------|
| **Reflex** | <1ms | Tick time profiler |
| **Pattern** | 1-10ms | Execution time tracker |
| **State** | 10-100ms | Emotional update timer |
| **Perception** | 50-200ms | Processing time tracker |
| **Language** | 100-500ms | Response latency tracker |
| **Planning** | 500ms-10s | Async completion tracker |
| **Meta** | Continuous | Background CPU monitor |

### 15.2 Effectiveness Metrics

**Pattern Layer Effectiveness:**
```
Cache Hit Rate = cached_executions / total_executions
Target: >80% for routine tasks

Pattern Success Rate = successful_pattern_executions / total_pattern_executions
Target: >90% for cached patterns
```

**Planning Layer Effectiveness:**
```
Plan Success Rate = successful_plans / total_plans
Target: >70% for novel situations

Plan Execution Time = time_from_plan_request_to_first_action
Target: <5 seconds for routine goals
```

**Meta Layer Effectiveness:**
```
Learning Rate = improvement_over_time / time_spent
Target: Measurable improvement within 1 hour

Pattern Refinement Success = refined_patterns_that_work_better / total_refined_patterns
Target: >60%
```

### 15.3 Believability Metrics

**Emotional Consistency:**
```
Emotion-Action Alignment = appropriate_emotional_responses / total_responses
Target: >85% (fear → caution, joy → celebration)
```

**Behavioral Variety:**
```
Unique Action Ratio = unique_actions / total_actions
Target: >30% (not just repeating same actions)
```

**Social Believability:**
```
Dialogue Appropriateness = appropriate_dialogue / total_dialogue
Target: >80%
```

### 15.4 Comparison Metrics

| Metric | Single-Layer LLM | Multi-Layer |
|--------|------------------|-------------|
| **Avg Response Time** | 1500ms | 5ms (reflex) to 5000ms (planning) |
| **Cost per Hour** | $2-5 | $0.10-0.50 (less LLM usage) |
| **Novelty Handling** | Excellent | Excellent (same LLM) |
| **Routine Efficiency** | Poor | Excellent (cached patterns) |
| **Believability** | Medium | High (emotional modulation) |
| **Learning Rate** | Slow (per-session) | Fast (meta layer) |

---

## 16. Future Research Directions

### 16.1 Hierarchical Planning

**Research Question:** Can planning layers be organized hierarchically?

```
Meta Layer (Strategy)
    ↓
High-Level Planning (Goals)
    ↓
Mid-Level Planning (Tactics)
    ↓
Low-Level Planning (Actions)
    ↓
Pattern Layer (Execution)
```

### 16.2 Multi-Agent Meta-Learning

**Research Question:** Can multiple agents share learned patterns?

```
Agent A learns pattern → Meta layer saves to shared storage
Agent B loads pattern → No learning needed
Swarm intelligence emerges
```

### 16.3 Emotional Memory Systems

**Research Question:** How should emotional memories be organized?

```
Episodic Emotional Memory (what happened)
    ↓
Semantic Emotional Memory (what it means)
    ↓
Procedural Emotional Memory (how to respond)
```

### 16.4 Perception-Action Integration

**Research Question:** Can perception and action layers be more tightly integrated?

```
Affordances: Direct perception of action possibilities
Instead of: Perception → Object → Action
Use: Perception → Affordance → Action
```

### 16.5 Dream Simulation

**Research Question:** Can agents "dream" to reinforce learning?

```
During sleep/inactivity:
    - Replay recent experiences
    - Simulate variations
    - Strengthen successful patterns
    - Weaken unsuccessful ones
```

---

## 17. Conclusion

### 17.1 Summary of Contributions

This work presents a comprehensive **multi-layer architecture for AI agents** that draws inspiration from biological cognition to create systems that feel truly "alive." The seven layers—Reflex, Pattern, State, Perception, Language, Planning, and Meta—operate at different time scales, with different learning capabilities, and different levels of abstraction, yet work together to produce coherent, intelligent behavior.

**Key Insights:**

1. **Multiple Abstractions:** AI requires multiple layers of abstraction, not one
2. **Time Scale Matters:** Different cognitive processes operate at vastly different speeds
3. **Parallel Operation:** Layers must run concurrently, not sequentially
4. **Graceful Degradation:** Lower layers maintain competence when higher layers fail
5. **Continuous Learning:** The meta layer ensures constant improvement

### 17.2 Relationship to Existing Work

This architecture extends the "One Abstraction Away" thesis by recognizing that:
- LLMs are not a replacement for traditional AI
- LLMs are best used as **one layer among many** (the Planning Layer)
- Traditional AI patterns (FSM, BT, utility) remain essential for real-time performance
- Emotional modulation adds richness missing from purely rational systems

### 17.3 Practical Applications

The multi-layer architecture is particularly valuable for:
- **Game Companions:** NPCs that feel alive and responsive
- **Autonomous Agents:** Systems that must operate independently
- **Virtual Assistants:** AI that can both react and plan
- **Educational Agents:** Tutors that adapt to learner states
- **Therapeutic Agents:** Systems that model emotional responses

### 17.4 Future Work

Future research should explore:
- Hierarchical planning layers
- Multi-agent meta-learning
- Emotional memory systems
- Perception-action integration
- Dream simulation for reinforcement learning

### 17.5 Final Thoughts

The vision of "AI as Living Creature" is not about building artificial consciousness. It's about building systems that **exhibit the richness, adaptability, and believability** of living creatures through layered cognition. By organizing AI systems into multiple layers operating at multiple time scales, we can create agents that are both responsive and reflective, fast and thoughtful, emotional and rational.

The multi-layer architecture is not just a technical framework—it's a conceptual bridge between the artificial and the biological, the mechanical and the living. It suggests that the path to believable AI is not through monolithic systems, but through **integrated layering** of multiple cognitive processes, each contributing its unique capabilities to the whole.

---

## References

### Biological and Cognitive Science

- MacLean, P. D. (1990). *The Triune Brain in Evolution*. Plenum Press.
- Fitts, P. M., & Posner, M. I. (1967). *Human Performance*. Brooks/Cole.
- Miller, G. A. (1956). "The magical number seven, plus or minus two." *Psychological Review*, 63(2), 81-97.
- Ortony, A., Clore, G. L., & Collins, A. (1988). *The Cognitive Structure of Emotions*. Cambridge University Press.
- Picard, R. W. (1997). *Affective Computing*. MIT Press.

### Game AI and Architecture

- Isla, D. (2005). "Handling complexity in the Halo 2 AI." *Game Developers Conference*.
- Orkin, J. (2004). "Applying goal-oriented action planning to games." *AI Game Programming Wisdom*.
- Champandard, A. J. (2003). "AI game development: Synthetic creatures with learning and reactive behaviors." *Proceedings of the Game Developers Conference*.
- Rabin, S. (2022). *Game AI Pro*. CRC Press.

### Modern AI Agents

- Wang, Y. et al. (2023). "Voyager: An open-ended embodied agent with large language models." *arXiv preprint arXiv:2305.16291*.
- Fan, L. et al. (2022). "MineDojo: Building open-ended embodied agents with internet-scale knowledge." *NeurIPS*.

### Software Architecture

- Bass, L., Clements, P., & Kazman, R. (2012). *Software Architecture in Practice* (3rd ed.). Addison-Wesley.
- Shaw, M., & Clements, P. (2006). "The golden age of software architecture." *IEEE Software*, 23(1), 31-39.
- Van Vliet, H. (2008). *Software Engineering: Principles and Practice* (3rd ed.). Wiley.

---

**Document Version:** 1.0
**Created:** 2026-02-28
**Author:** Research Team
**Status:** Dissertation Chapter 2 - Cognitive Architecture Design
**Word Count:** ~15,000
