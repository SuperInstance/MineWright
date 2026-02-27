# Combat Threat Assessment and Dialogue System for MineWright Workers

**Project:** MineWright - Minecraft Autonomous Agents
**Component:** Combat Threat Detection and Dialogue System
**Version:** 1.0
**Date:** 2026-02-27
**Status:** Research & Design Document

---

## Executive Summary

This document defines a comprehensive combat threat assessment and dialogue system for MineWright workers. Workers will detect mob threats at multiple ranges, communicate about danger using military-inspired protocols, engage in combat commentary, and report outcomes. The system draws from real-world military communication patterns, combat stress psychology, and proven game dialogue systems (Left 4 Dead, Overwatch) to create immersive, tactical worker behavior.

**Key Design Principles:**
1. **Progressive Threat Detection:** Scouting → Approaching → Engaged → Retreating
2. **Mob-Specific Responses:** Unique dialogue for each hostile mob type
3. **Military Communication:** Clear, concise tactical reporting
4. **Combat Stress Psychology:** Fear, adrenaline, and fatigue affect dialogue
5. **Role-Based Personality:** Guards speak differently from Miners/Builders
6. **Cooperative Coordination:** Workers alert each other and call for help

---

## Table of Contents

1. [Threat Detection Levels](#1-threat-detection-levels)
2. [Mob-Specific Responses](#2-mob-specific-responses)
3. [Combat Engagement Commentary](#3-combat-engagement-commentary)
4. [Victory and Defeat Responses](#4-victory-and-defeat-responses)
5. [Call for Help and Protection](#5-call-for-help-and-protection)
6. [Guard-Specific Tactical Communication](#6-guard-specific-tactical-communication)
7. [Java Implementation](#7-java-implementation)
8. [Dialogue Templates](#8-dialogue-templates)

---

## 1. Threat Detection Levels

Workers assess threats through four distinct stages, each with unique dialogue patterns and urgency levels.

### 1.1 Detection Range Bands

| Level | Range | Trigger | Worker State | Dialogue Style |
|-------|-------|---------|--------------|----------------|
| **SCOUTING** | 32-48 blocks | Mob detected at distance | Curious, observant | Casual reporting |
| **APPROACHING** | 16-31 blocks | Mob moving toward worker | Alert, preparing | Warning tone |
| **ENGAGED** | 0-15 blocks | Combat initiated | Focused, stressed | Urgent, tactical |
| **RETREATING** | Health <30% | Overwhelmed/danger | Fearful, desperate | Emergency calls |

### 1.2 Scouting Phase (32-48 blocks)

**Psychology:** Curiosity, assessment, low threat perception. Worker is gathering information.

**Dialogue Characteristics:**
- Observational tone
- Distance estimation
- Mob type identification
- Non-urgent reporting

**Example Patterns:**
```
"{mobType} spotted. {distance} blocks out."
"I see a {mobType}. Moving in from {direction}."
"Heads up - {mobType} at {coordinates}."
```

### 1.3 Approaching Phase (16-31 blocks)

**Psychology:** Recognition of threat, preparation, elevated alertness. Adrenaline begins.

**Dialogue Characteristics:**
- Warning tone
- Direction calls
- Preparation statements
- Team alerts

**Example Patterns:**
```
"{mobType} incoming from {direction}!"
"We've got a {mobType} approaching. {distance} meters."
"Contact! {mobType}, {direction} side."
```

### 1.4 Engaged Phase (0-15 blocks)

**Psychology:** Fight-or-flight response, heightened stress, tunnel vision. Immediate danger.

**Dialogue Characteristics:**
- Short, clipped phrases (military style)
- Tactical commands
- Status updates
- Combat focus

**Example Patterns:**
```
"Engaging {mobType}!"
"Contact front! {mobType}!"
"Taking fire! {mobType} closing!"
"Fighting {mobType} at close range!"
```

### 1.5 Retreating Phase (Health <30%)

**Psychology:** Fear, self-preservation, acknowledgment of overwhelming odds. Combat stress peak.

**Dialogue Characteristics:**
- Urgent, desperate tone
- Fall-back calls
- Damage reports
- Requests for assistance

**Example Patterns:**
```
"I'm hit! Falling back!"
"Can't hold! Need help!"
"Taking damage! Retreat!"
"Overwhelmed! Requesting backup!"
```

---

## 2. Mob-Specific Responses

Each Minecraft mob elicits unique dialogue based on its behavior patterns, threat level, and "personality."

### 2.1 Zombie (The relentless horde)

**Behavior Notes:** Large detection range (35-40 blocks), reinforcements, door breaking, baby variants.

**Detection Dialogue:**
```
"Zombie spotted. Forty meters out."
"Got a walker approaching from the west."
"Zombie contact. Should be easy pickings."
"Undead incoming. Standard threat."
```

**Approach Dialogue:**
```
"Zombie closing in! Ten meters!"
"Walker making a beeline for us!"
"Watch out - zombie approaching from behind!"
"Undead at close range. Weapons ready."
```

**Combat Dialogue:**
```
"Engaging zombie!"
"Target acquired: Zombie!"
"Putting down the walker!"
"Hostile down! Zombie eliminated."
```

**Special Cases:**
```
"Baby zombie! That's a fast one!"
"Reinforcements! More zombies incoming!"
"Door breach! They're breaking through!"
"Husks! Don't let them drain your hunger!"
```

### 2.2 Skeleton (The sharpshooter)

**Behavior Notes:** 16-block detection, ranged attacks, strafing, retreats at close range.

**Detection Dialogue:**
```
"Skeleton at range. Watch for arrows."
"Sniper spotted! {distance} blocks out."
"Bones is out there. Taking cover."
"Archers incoming. Shields up!"
```

**Approach Dialogue:**
```
"Skeleton drawing a bow! Get to cover!"
"Arrow fire! Three o'clock!"
"Bones has us in sight! Move!"
"Skeleton targeting! Find shelter!"
```

**Combat Dialogue:**
```
"Flanking the skeleton!"
"Closing on bones!"
"Suppressing skeleton fire!"
"Pressing the attack! Charging!"
```

**Special Cases:**
```
"Stray skeleton! Watch out for slowness!"
"He's running! Cornered him!"
"Burning in daylight! Easy pickings!"
```

### 2.3 Creeper (The silent assassin)

**Behavior Notes:** No movement sounds, 16-block detection, 3-block explosion trigger, hiss warning.

**Detection Dialogue:**
```
"Creeper! I hear one nearby."
"Silent contact. Creeper in the area!"
"Explosive threat detected!"
"Sssh! Creeper close by. Listen..."
```

**Approach Dialogue:**
```
"Creeper closing! Don't let it get close!"
"Explosive incoming! Keep distance!"
"Get back! Creeper approaching!"
"Alert! Creeper at {distance} meters!"
```

**Critical Dialogue (Hiss Detected):**
```
"HISS! Take cover!"
"IT'S GONNA BLOW! RUN!"
"FUSE LIT! CLEAR THE AREA!"
"CREEPER! EXPLOSION IMMINENT!"
```

**Combat Dialogue:**
```
"Knockback! Keep it away!"
"Skeleton arrow! Hit the creeper!"
"Ranged attack on the bomber!"
"Safe distance! Engaging!"
```

### 2.4 Enderman (The teleporter)

**Behavior Notes:** 64-block stare detection, teleportation, water damage, scream warning.

**Detection Dialogue:**
```
"Enderman in the area. Don't stare."
"Tall spotted. Avoid eye contact."
"Got an enderman nearby. Be careful."
"Enderman contact. {distance} blocks out."
```

**Aggro Dialogue (Stare Detected):**
```
"Don't look at it!"
"Eye contact! It's aggroed!"
"Scream! Enderman hostile!"
"It saw us! Teleporting!"
```

**Combat Dialogue:**
```
"Target locked: Enderman!"
"It's teleporting! Watch your back!"
"Water! Get to water!"
"Enderman enraged! Stay mobile!"
```

**Special Cases:**
```
"It's taking blocks! Look out!"
"Rain damage! It's vulnerable!"
"Pumpkin head - we're safe from stare!"
```

### 2.5 Spider (The ambusher)

**Behavior Notes:** Wall climbing, early morning neutrality, pounce attacks.

**Detection Dialogue:**
```
"Spider on the perimeter!"
"We've got spiders crawling around."
"Eight-legged contact. {distance} meters."
"Spider spotted! Check the walls!"
```

**Approach Dialogue:**
```
"Spider incoming! Watch the walls!"
"Crawler closing in from above!"
"Spider dropping! Look up!"
"It's on the ceiling! Alert!"
```

**Combat Dialogue:**
```
"Crush the spider!"
"Stomping the crawler!"
"Spider down! Watch for more!"
"Engaging arachnid!"
```

**Special Cases:**
```
"Cave spider! Poison risk!"
"Spider jockey! Watch out!"
"Daytime spider - it's neutral!"
```

### 2.6 Phantom (The night stalker)

**Behavior Notes:** Spawns after 3 nights without sleep, flying attacks, dive-bombing.

**Detection Dialogue:**
```
"Phantoms circling overhead!"
"I hear wing beats! Phantom alert!"
"Sky threats! Phantoms spawning!"
"Insomnia's coming back to bite us!"
```

**Approach Dialogue:**
```
"Phantom diving! Take cover!"
"Aerial attack! Phantom incoming!"
"It's swooping! Shields up!"
"Phantom at {distance} blocks! Get ready!"
```

**Combat Dialogue:**
```
"Anti-air! Phantom engaging!"
"Timing the dive! Strike now!"
"Ground defense! Phantom down!"
"Shooting down the phantom!"
```

### 2.7 Witch (The potion thrower)

**Behavior Notes:** Ranged potions, healing, aggro from attacks.

**Detection Dialogue:**
```
"Witch in the area! Potions out!"
"Coven contact detected!"
"Potion thrower spotted!"
"Witch hut nearby! Caution!"
```

**Approach Dialogue:**
```
"Witch preparing potions! Take cover!"
"She's brewing something nasty!"
"Potion rain incoming! Scatter!"
"Witch aggro! Watch for debuffs!"
```

**Combat Dialogue:**
```
"Closing on the witch!"
"Suppressing fire on the caster!"
"Witch healing! Finish it off!"
"Potion effects! Fight through it!"
```

### 2.8 Slime (The bouncy menace)

**Behavior Notes:** Split into smaller slimes, melee attacks, underground spawning.

**Detection Dialogue:**
```
"Slime detected! Underground contact!"
"Gooey spotted! Watch for splits!"
"Slime noise nearby!"
"Bouncing threat incoming!"
```

**Combat Dialogue:**
```
"Big slime! Breaking it down!"
"Mini slimes! Clear them out!"
"Slime splitting! More targets!"
"Bouncy ones! Stomp them flat!"
```

---

## 3. Combat Engagement Commentary

Real-time commentary during combat maintains awareness and provides immersive feedback.

### 3.1 Fighting Commentary

**Opening Combat:**
```
"Engaging hostile!"
"Contact! Taking the fight to them!"
"Locking onto target!"
"Weapons free! Engaging!"
```

**Mid-Combat Updates:**
```
"Target still active!"
"Pressing the attack!"
"Holding the line!"
"Still fighting! Need backup!"
```

**Combat Flow:**
```
"Dodging attack! Countering!"
"Blocking! Striking back!"
"Hit confirmed! Pressing on!"
"Taking fire! Returning fire!"
```

### 3.2 Dodging and Evasion

**Successful Dodge:**
```
"Missed me!"
"Close call! Dodged!"
"Too slow! Sidestepped!"
"Nice try! Evaded!"
```

**Evasive Maneuvers:**
```
"Flanking right!"
"Circling around!"
"Getting behind it!"
"Repositioning! Watch my back!"
```

**Taking Cover:**
```
"Taking cover!"
"Finding a shield position!"
"Behind cover! Regrouping!"
"Hull down! Holding position!"
```

### 3.3 Blocking and Defense

**Shield Block:**
```
"Blocked!"
"Shield up! Caught it!"
"Absorbed the blow!"
"Covering fire! I'm protected!"
```

**Defensive Maneuvers:**
```
"Playing defensively!"
"Turtling up! Hold the line!"
"Defensive posture! Watch my flanks!"
"Guard up! Let it come to us!"
```

**Protection Calls:**
```
"I've got your flank!"
"Covering you! Focus on the target!"
"Shield brother! I'll take the hits!"
"Protector mode! Stay behind me!"
```

---

## 4. Victory and Defeat Responses

### 4.1 Victory Celebrations

**Target Eliminated:**
```
"Target down!"
"Hostile eliminated!"
"Threat neutralized!"
"Scraps one!"
```

**Clean Kills:**
```
"Clean hit! One shot!"
"Perfect strike! Down!"
"Got it! No trouble!"
"Easy work! Threat removed!"
```

**Hard-Fought Victories:**
```
"That was close! But we got it!"
"Tough one! But it's done!"
"Hard fighting! Victory ours!"
"Barely made it! But won!"
```

**After-Action Report:**
```
"Area secure. All hostiles down."
"Clear! No threats remaining."
"Sector clear. Moving on."
"Combat complete. Resuming operations."
```

### 4.2 Defeat and Retreat

**Withdrawing:**
```
"Falling back!"
"Can't hold! Retreat!"
"Regrouping! Falling back!"
"Tactical retreat! Move!"
```

**Overwhelmed:**
```
"Too many! Pulling back!"
"Overrun! Need help!"
"Can't hold them! Run!"
"Overwhelmed! Fall back!"
```

**Damage Report:**
```
"Taking heavy damage!"
"I'm hurt bad! Need healing!"
"Health critical! Back off!"
"Armor breaking! Emergency retreat!"
```

**Failure Acknowledgment:**
```
"Lost that one. Sorry."
"Got overwhelmed. My bad."
"Need to rethink this."
"That went poorly. Regroup."
```

---

## 5. Call for Help and Protection

Workers coordinate combat responses through structured call-for-help protocols.

### 5.1 Guard-Specific Calls

**Guard Detects Threat:**
```
"Guard alert! Hostile spotted!"
"On patrol! Contact reported!"
"Perimeter breach! {mobType}!"
"Guard duty! Incoming threat!"
```

**Guard Requests Backup:**
```
"Guards! Need backup at {location}!"
"Perimeter defense! All hands!"
"Security alert! {mobType} at {coordinates}!"
"Protective detail! Need assistance!"
```

**Guard Protects Worker:**
```
"Stay behind me!"
"I'll draw its fire!"
"Covering the worker! Focus!"
"Protection mode! Civilian, get back!"
```

### 5.2 Worker Distress Calls

**Worker Under Attack:**
```
"Help! I'm under attack!"
"{mobType} on me! Need help!"
"Taking damage! Request assistance!"
"Mayday! Hostile engaging!"
```

**Worker Requests Cover:**
```
"Need covering fire!"
"Someone draw its attention!"
"Flank it! I need help!"
"Support required! Engaging now!"
```

**Worker Calls for Extraction:**
```
"Pull me out! I'm pinned!"
"Extraction needed! Can't escape!"
"Surrounded! Break me out!"
"Trapped! Help!"
```

### 5.3 Cooperative Combat

**Coordinated Attack:**
```
"Attack on my mark! Three... two... one!"
"Flanking! You take the front!"
"Pincer maneuver! Surround it!"
"Divide and conquer! Spread out!"
```

**Support Dialogue:**
```
"I've got your back!"
"Focus on the target! I'll cover!"
"Ranged support! Firing!"
"Melee support! Engaging!"
```

**Victory Celebration (Group):**
```
"Team effort! Good work everyone!"
"Coordinated victory! Nice!"
"We fight as one! We win as one!"
"Squad goals! Threat eliminated!"
```

---

## 6. Guard-Specific Tactical Communication

Guards speak with military precision, using formal tactical language.

### 6.1 Guard Personality Profile

**Speech Patterns:**
- Military terminology
- Precise location reporting
- Formal command structure
- Tactical awareness focus

**Example Dialogue:**
```
"Situation report: {mobType} detected at {gridReference}. Threat level: {level}."
"Executing tactical fallback. Covering fire requested."
"Perimeter secured. All clear."
"Engaging hostile with extreme prejudice."
```

### 6.2 Guard Threat Assessment

**Guard Detection Reports:**
```
"Visual contact acquired. {mobType}, bearing {direction}, distance {distance}."
"Scanning sector. Hostile signature detected."
"Threat assessment: {mobType}. Danger level elevated."
"Perimeter breach at {location}. Responding."
```

**Guard Tactical Calls:**
```
"Defensive perimeter forming!"
"Fall back to defensive positions!"
"Hold the line! Do not break!"
"Rally point! Regroup on me!"
```

### 6.3 Guard Post-Combat Reports

**After Action Report:**
```
"Combat complete. Casualties: none. Threat eliminated."
"Situation contained. Perimeter secure."
"Hostile neutralized. Sector clear."
"Action complete. Returning to patrol."
```

---

## 7. Java Implementation

### 7.1 CombatDialogueManager Class

```java
package com.minewright.dialogue;

import com.minewright.config.MineWrightConfig;
import com.minewright.entity.ForemanEntity;
import com.minewright.orchestration.AgentRole;
import com.minewright.personality.WorkerRole;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages combat threat detection and dialogue for MineWright workers.
 *
 * <p>This system provides:</p>
 * <ul>
 *   <li>Four-stage threat detection (scouting, approaching, engaged, retreating)</li>
 *   <li>Mob-specific dialogue responses</li>
 *   <li>Combat engagement commentary</li>
 *   <li>Guard-specific tactical communication</li>
 *   <li>Cooperative combat alerts</li>
 * </ul>
 *
 * <p><b>Design Principles:</b></p>
 * <ul>
 *   <li>Military communication protocols for clarity</li>
 *   <li>Combat stress psychology for realism</li>
 *   <li>Role-based personality variation</li>
 *   <li>Progressive threat escalation</li>
 *   <li>Cooperative coordination</li>
 * </ul>
 *
 * @since 1.5.0
 */
public class CombatDialogueManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CombatDialogueManager.class);

    private final ForemanEntity worker;
    private final Random random;

    // Configuration
    private final boolean enabled;
    private final double scoutingRange;       // 48 blocks
    private final double approachingRange;    // 16 blocks
    private final double engagedRange;        // 0 blocks (melee)

    // Threat tracking
    private final Map<UUID, ThreatState> trackedThreats;
    private ThreatLevel currentThreatLevel;
    private LivingEntity primaryTarget;
    private long lastThreatUpdateTime;

    // Dialogue cooldowns
    private long lastScoutingReport;
    private long lastApproachingReport;
    private long lastEngagedReport;
    private long lastRetreatingReport;
    private final long cooldownTicks;

    /**
     * Threat detection levels.
     */
    public enum ThreatLevel {
        SCOUTING(48, "Scouting", 1200),      // 48 blocks, 60s cooldown
        APPROACHING(16, "Approaching", 400), // 16 blocks, 20s cooldown
        ENGAGED(0, "Engaged", 100),          // Melee, 5s cooldown
        RETREATING(0, "Retreating", 200);    // Low health, 10s cooldown

        private final double maxRange;
        private final String displayName;
        private final long cooldownTicks;

        ThreatLevel(double maxRange, String displayName, long cooldownTicks) {
            this.maxRange = maxRange;
            this.displayName = displayName;
            this.cooldownTicks = cooldownTicks;
        }

        public double getMaxRange() { return maxRange; }
        public String getDisplayName() { return displayName; }
        public long getCooldownTicks() { return cooldownTicks; }
    }

    /**
     * Tracks individual threat state.
     */
    private static class ThreatState {
        final UUID entityId;
        final EntityType<?> entityType;
        final String mobType;
        ThreatLevel level;
        double lastDistance;
        long lastReportTime;

        ThreatState(UUID entityId, EntityType<?> entityType, String mobType) {
            this.entityId = entityId;
            this.entityType = entityType;
            this.mobType = mobType;
            this.level = ThreatLevel.SCOUTING;
            this.lastDistance = Double.MAX_VALUE;
            this.lastReportTime = 0;
        }

        void updateLevel(double distance, double workerHealthPercent) {
            // Update based on distance
            if (distance <= ThreatLevel.ENGAGED.getMaxRange()) {
                level = ThreatLevel.ENGAGED;
            } else if (distance <= ThreatLevel.APPROACHING.getMaxRange()) {
                level = ThreatLevel.APPROACHING;
            } else if (distance <= ThreatLevel.SCOUTING.getMaxRange()) {
                level = ThreatLevel.SCOUTING;
            }

            // Check for retreat condition (low health)
            if (workerHealthPercent < 0.3) {
                level = ThreatLevel.RETREATING;
            }
        }
    }

    /**
     * Creates a new CombatDialogueManager for a worker.
     *
     * @param worker The MineWright worker entity
     */
    public CombatDialogueManager(ForemanEntity worker) {
        this.worker = worker;
        this.random = new Random();
        this.trackedThreats = new ConcurrentHashMap<>();
        this.currentThreatLevel = ThreatLevel.SCOUTING;
        this.primaryTarget = null;
        this.lastThreatUpdateTime = 0;

        // Load configuration
        this.enabled = true;
        this.scoutingRange = 48.0;
        this.approachingRange = 16.0;
        this.engagedRange = 3.5;  // Combat attack range
        this.cooldownTicks = 100;  // 5 seconds base cooldown

        LOGGER.info("CombatDialogueManager initialized for worker '{}'",
            worker.getSteveName());
    }

    /**
     * Called every tick to check for threats and update dialogue.
     */
    public void tick() {
        if (!enabled) {
            return;
        }

        // Scan for threats periodically
        long now = System.currentTimeMillis();
        if (now - lastThreatUpdateTime < 1000) {  // Scan every second
            return;
        }
        lastThreatUpdateTime = now;

        scanForThreats();
        updateThreatStates();
        generateThreatDialogue();
    }

    /**
     * Scans the area for hostile mobs.
     */
    private void scanForThreats() {
        Set<UUID> currentThreats = new HashSet<>();

        // Find all hostile mobs in range
        List<Monster> hostiles = worker.level().getEntitiesOfClass(
            Monster.class,
            worker.getBoundingBox().inflate(scoutingRange)
        );

        for (Monster hostile : hostiles) {
            if (!isValidTarget(hostile)) {
                continue;
            }

            UUID threatId = hostile.getUUID();
            currentThreats.add(threatId);

            // Add or update threat tracking
            trackedThreats.computeIfAbsent(threatId, id ->
                new ThreatState(id, hostile.getType(), getMobType(hostile))
            );

            // Update distance
            ThreatState state = trackedThreats.get(threatId);
            state.lastDistance = worker.distanceTo(hostile);
        }

        // Remove threats that are out of range or dead
        trackedThreats.entrySet().removeIf(entry -> {
            boolean shouldRemove = !currentThreats.contains(entry.getKey()) ||
                                   entry.getValue().lastDistance > scoutingRange;
            return shouldRemove;
        });

        // Update primary target (nearest hostile)
        primaryTarget = trackedThreats.values().stream()
            .min(Comparator.comparingDouble(s -> s.lastDistance))
            .map(state -> worker.level().getEntity(state.entityId))
            .filter(entity -> entity instanceof LivingEntity)
            .map(entity -> (LivingEntity) entity)
            .orElse(null);
    }

    /**
     * Checks if an entity is a valid target.
     */
    private boolean isValidTarget(LivingEntity entity) {
        if (!entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        // Don't track other workers or players
        if (entity instanceof ForemanEntity ||
            entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        return true;
    }

    /**
     * Gets the mob type string for dialogue generation.
     */
    private String getMobType(LivingEntity entity) {
        String entityName = entity.getType().toString().toLowerCase();

        // Map entity types to dialogue-friendly names
        if (entityName.contains("zombie")) return "zombie";
        if (entityName.contains("skeleton")) return "skeleton";
        if (entityName.contains("creeper")) return "creeper";
        if (entityName.contains("enderman")) return "enderman";
        if (entityName.contains("spider")) return "spider";
        if (entityName.contains("phantom")) return "phantom";
        if (entityName.contains("witch")) return "witch";
        if (entityName.contains("slime")) return "slime";

        return entityName; // Fallback to actual name
    }

    /**
     * Updates threat states based on current conditions.
     */
    private void updateThreatStates() {
        double healthPercent = worker.getHealth() / worker.getMaxHealth();

        for (ThreatState threat : trackedThreats.values()) {
            threat.updateLevel(threat.lastDistance, healthPercent);
        }

        // Update overall threat level
        currentThreatLevel = trackedThreats.values().stream()
            .max(Comparator.comparingInt(t -> t.level.ordinal()))
            .map(t -> t.level)
            .orElse(ThreatLevel.SCOUTING);
    }

    /**
     * Generates threat dialogue based on current state.
     */
    private void generateThreatDialogue() {
        if (trackedThreats.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        // Check if we should report for current threat level
        for (ThreatState threat : trackedThreats.values()) {
            long cooldown = threat.level.getCooldownTicks() * 50; // Convert to milliseconds
            if (now - threat.lastReportTime >= cooldown) {
                // Generate and speak dialogue
                String dialogue = generateThreatDialogue(threat);
                if (dialogue != null && !dialogue.isEmpty()) {
                    worker.sendChatMessage(dialogue);
                    threat.lastReportTime = now;
                    LOGGER.debug("Combat dialogue [{}]: {}",
                        threat.level.getDisplayName(), dialogue);
                    break;  // Only one report per tick
                }
            }
        }

        // Special case: Retreat dialogue (always fire if changed)
        if (currentThreatLevel == ThreatLevel.RETREATING &&
            now - lastRetreatingReport > 5000) {
            String retreatDialogue = getRetreatDialogue();
            worker.sendChatMessage(retreatDialogue);
            lastRetreatingReport = now;
        }
    }

    /**
     * Generates dialogue for a specific threat.
     */
    private String generateThreatDialogue(ThreatState threat) {
        WorkerRole workerRole = getWorkerRole();
        int distance = (int) threat.lastDistance;
        String direction = getDirectionTo(threat);

        return switch (threat.level) {
            case SCOUTING -> getScoutingDialogue(threat.mobType, distance, direction, workerRole);
            case APPROACHING -> getApproachingDialogue(threat.mobType, distance, direction, workerRole);
            case ENGAGED -> getEngagedDialogue(threat.mobType, workerRole);
            case RETREATING -> getRetreatDialogue(); // Handled separately
        };
    }

    /**
     * Gets the worker's role for role-specific dialogue.
     */
    private WorkerRole getWorkerRole() {
        // This would integrate with the WorkerRole system
        // For now, return GENERALIST
        return WorkerRole.GENERALIST;
    }

    /**
     * Gets the cardinal direction to a threat.
     */
    private String getDirectionTo(ThreatState threat) {
        LivingEntity entity = worker.level().getEntity(threat.entityId);
        if (entity == null) {
            return "unknown";
        }

        double dx = entity.getX() - worker.getX();
        double dz = entity.getZ() - worker.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx));

        if (angle < -157.5) return "south";
        if (angle < -112.5) return "southwest";
        if (angle < -67.5) return "west";
        if (angle < -22.5) return "northwest";
        if (angle < 22.5) return "north";
        if (angle < 67.5) return "northeast";
        if (angle < 112.5) return "east";
        if (angle < 157.5) return "southeast";
        return "south";
    }

    /**
     * Called when combat begins (first attack).
     */
    public void onCombatBegin(LivingEntity target) {
        String mobType = getMobType(target);
        String dialogue = random.nextBoolean() ?
            "Engaging " + mobType + "!" :
            "Contact! " + mobType + "!";
        worker.sendChatMessage(dialogue);
        LOGGER.debug("Combat begin: {}", dialogue);
    }

    /**
     * Called when a target is eliminated.
     */
    public void onTargetEliminated(LivingEntity target) {
        String mobType = getMobType(target);
        String[] victoryLines = {
            "Target down! " + mobType + " eliminated!",
            mobType + " neutralized!",
            "Scraps one " + mobType + "!",
            "Threat removed!"
        };
        String dialogue = victoryLines[random.nextInt(victoryLines.length)];
        worker.sendChatMessage(dialogue);
        LOGGER.debug("Target eliminated: {}", dialogue);
    }

    /**
     * Called when taking damage.
     */
    public void onDamageTaken(float damageAmount, LivingEntity attacker) {
        if (random.nextDouble() < 0.3) {  // 30% chance to call out
            String[] hitLines = {
                "Taking damage!",
                "I'm hit!",
                "Ouch! That stings!",
                "Taking fire!"
            };
            worker.sendChatMessage(hitLines[random.nextInt(hitLines.length)]);
        }
    }

    /**
     * Called when health is critical (below 30%).
     */
    public void onHealthCritical() {
        String[] criticalLines = {
            "I'm hurt bad! Need help!",
            "Health critical! Falling back!",
            "Can't take much more of this!",
            "Medical attention required!"
        };
        worker.sendChatMessage(criticalLines[random.nextInt(criticalLines.length)]);
    }

    /**
     * Called when requesting backup.
     */
    public void callForBackup() {
        String mobType = primaryTarget != null ? getMobType(primaryTarget) : "hostile";
        String[] backupLines = {
            "Need backup! " + mobType + "!",
            "Requesting support! Overwhelmed!",
            "Help needed! " + mobType + " closing!",
            "All units! " + mobType + " contact!"
        };
        worker.sendChatMessage(backupLines[random.nextInt(backupLines.length)]);
    }

    /**
     * Checks if combat dialogue is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the current threat level.
     */
    public ThreatLevel getCurrentThreatLevel() {
        return currentThreatLevel;
    }

    /**
     * Gets the primary target.
     */
    public LivingEntity getPrimaryTarget() {
        return primaryTarget;
    }

    // ==================== Dialogue Generation Methods ====================

    private String getScoutingDialogue(String mobType, int distance, String direction,
                                       WorkerRole workerRole) {
        String[] templates = SCOUTING_DIALOGUE.getOrDefault(mobType, SCOUTING_DIALOGUE.get("generic"));
        String template = templates[random.nextInt(templates.length)];

        return template
            .replace("{mobType}", mobType)
            .replace("{distance}", String.valueOf(distance))
            .replace("{direction}", direction);
    }

    private String getApproachingDialogue(String mobType, int distance, String direction,
                                          WorkerRole workerRole) {
        String[] templates = APPROACHING_DIALOGUE.getOrDefault(mobType, APPROACHING_DIALOGUE.get("generic"));
        String template = templates[random.nextInt(templates.length)];

        return template
            .replace("{mobType}", mobType)
            .replace("{distance}", String.valueOf(distance))
            .replace("{direction}", direction);
    }

    private String getEngagedDialogue(String mobType, WorkerRole workerRole) {
        String[] templates = ENGAGED_DIALOGUE.getOrDefault(mobType, ENGAGED_DIALOGUE.get("generic"));
        String template = templates[random.nextInt(templates.length)];

        return template.replace("{mobType}", mobType);
    }

    private String getRetreatDialogue() {
        return RETREAT_DIALOGUE[random.nextInt(RETREAT_DIALOGUE.length)];
    }

    // ==================== Dialogue Template Maps ====================

    private static final Map<String, String[]> SCOUTING_DIALOGUE = Map.of(
        "zombie", new String[]{
            "Zombie spotted. {distance} blocks out.",
            "Got a walker approaching from the {direction}.",
            "Zombie contact. Should be easy pickings.",
            "Undead incoming. Standard threat."
        },
        "skeleton", new String[]{
            "Skeleton at range. Watch for arrows.",
            "Sniper spotted! {distance} blocks out.",
            "Bones is out there. Taking cover.",
            "Archers incoming. Shields up!"
        },
        "creeper", new String[]{
            "Creeper! I hear one nearby.",
            "Silent contact. Creeper in the area!",
            "Explosive threat detected!",
            "Sssh! Creeper close by. Listen..."
        },
        "enderman", new String[]{
            "Enderman in the area. Don't stare.",
            "Tall spotted. Avoid eye contact.",
            "Got an enderman nearby. Be careful.",
            "Enderman contact. {distance} blocks out."
        },
        "spider", new String[]{
            "Spider on the perimeter!",
            "We've got spiders crawling around.",
            "Eight-legged contact. {distance} meters.",
            "Spider spotted! Check the walls!"
        },
        "phantom", new String[]{
            "Phantoms circling overhead!",
            "I hear wing beats! Phantom alert!",
            "Sky threats! Phantoms spawning!",
            "Insomnia's coming back to bite us!"
        },
        "witch", new String[]{
            "Witch in the area! Potions out!",
            "Coven contact detected!",
            "Potion thrower spotted!",
            "Witch hut nearby! Caution!"
        },
        "generic", new String[]{
            "Hostile spotted. {distance} blocks out.",
            "Contact detected. {direction} side.",
            "Something's out there. {distance} meters.",
            "Threat incoming. {direction}."
        }
    );

    private static final Map<String, String[]> APPROACHING_DIALOGUE = Map.of(
        "zombie", new String[]{
            "Zombie closing in! Ten meters!",
            "Walker making a beeline for us!",
            "Watch out - zombie approaching from behind!",
            "Undead at close range. Weapons ready."
        },
        "skeleton", new String[]{
            "Skeleton drawing a bow! Get to cover!",
            "Arrow fire! Three o'clock!",
            "Bones has us in sight! Move!",
            "Skeleton targeting! Find shelter!"
        },
        "creeper", new String[]{
            "Creeper closing! Don't let it get close!",
            "Explosive incoming! Keep distance!",
            "Get back! Creeper approaching!",
            "Alert! Creeper at {distance} meters!"
        },
        "enderman", new String[]{
            "Don't look at it!",
            "Eye contact! It's aggroed!",
            "Scream! Enderman hostile!",
            "It saw us! Teleporting!"
        },
        "spider", new String[]{
            "Spider incoming! Watch the walls!",
            "Crawler closing in from above!",
            "Spider dropping! Look up!",
            "It's on the ceiling! Alert!"
        },
        "phantom", new String[]{
            "Phantom diving! Take cover!",
            "Aerial attack! Phantom incoming!",
            "It's swooping! Shields up!",
            "Phantom at {distance} blocks! Get ready!"
        },
        "witch", new String[]{
            "Witch preparing potions! Take cover!",
            "She's brewing something nasty!",
            "Potion rain incoming! Scatter!",
            "Witch aggro! Watch for debuffs!"
        },
        "generic", new String[]{
            "Hostile incoming from {direction}!",
            "We've got a contact approaching. {distance} meters!",
            "Contact! {direction} side!",
            "Threat closing! Prepare for combat!"
        }
    );

    private static final Map<String, String[]> ENGAGED_DIALOGUE = Map.of(
        "zombie", new String[]{
            "Engaging zombie!",
            "Target acquired: Zombie!",
            "Putting down the walker!",
            "Hostile down! Zombie eliminated."
        },
        "skeleton", new String[]{
            "Flanking the skeleton!",
            "Closing on bones!",
            "Suppressing skeleton fire!",
            "Pressing the attack! Charging!"
        },
        "creeper", new String[]{
            "Knockback! Keep it away!",
            "Ranged attack on the bomber!",
            "Safe distance! Engaging!",
            "Hit the creeper! Keep it back!"
        },
        "enderman", new String[]{
            "Target locked: Enderman!",
            "It's teleporting! Watch your back!",
            "Water! Get to water!",
            "Enderman enraged! Stay mobile!"
        },
        "spider", new String[]{
            "Crush the spider!",
            "Stomping the crawler!",
            "Spider down! Watch for more!",
            "Engaging arachnid!"
        },
        "phantom", new String[]{
            "Anti-air! Phantom engaging!",
            "Timing the dive! Strike now!",
            "Ground defense! Phantom down!",
            "Shooting down the phantom!"
        },
        "witch", new String[]{
            "Closing on the witch!",
            "Suppressing fire on the caster!",
            "Witch healing! Finish it off!",
            "Potion effects! Fight through it!"
        },
        "generic", new String[]{
            "Engaging hostile!",
            "Contact! Taking the fight to them!",
            "Locking onto target!",
            "Weapons free! Engaging!"
        }
    );

    private static final String[] RETREAT_DIALOGUE = {
        "I'm hit! Falling back!",
        "Can't hold! Need help!",
        "Taking damage! Retreat!",
        "Overwhelmed! Requesting backup!",
        "Health critical! Back off!",
        "Can't take much more of this!",
        "Tactical retreat! Move!",
        "Regrouping! Falling back!"
    };
}

/**
 * Enum representing worker roles for role-specific dialogue.
 * This integrates with the WorkerRole system.
 */
enum WorkerRole {
    MINER,
    BUILDER,
    GUARD,
    CRAFTER,
    GENERALIST
}
```

---

## 8. Dialogue Templates

Complete collection of 45+ dialogue templates organized by scenario.

### 8.1 Scouting Phase Templates (12 templates)

**Generic Scouting:**
```
1. "Hostile spotted. {distance} blocks out."
2. "Contact detected. {direction} side."
3. "Something's out there. {distance} meters."
4. "Threat incoming. {direction}."
5. "Visual on hostile. {distance} blocks."
6. "Scanning confirms contact. {direction}."
```

**Mob-Specific Scouting:**
```
7. "Zombie spotted. Forty meters out." (Zombie)
8. "Skeleton at range. Watch for arrows." (Skeleton)
9. "Creeper! I hear one nearby." (Creeper)
10. "Enderman in the area. Don't stare." (Enderman)
11. "Spider on the perimeter!" (Spider)
12. "Phantoms circling overhead!" (Phantom)
```

### 8.2 Approaching Phase Templates (12 templates)

**Generic Approaching:**
```
1. "Hostile incoming from {direction}!"
2. "We've got a contact approaching. {distance} meters!"
3. "Contact! {direction} side!"
4. "Threat closing! Prepare for combat!"
5. "{direction}! Contact approaching!"
6. "Closing in! {direction} side!"
```

**Mob-Specific Approaching:**
```
7. "Zombie closing in! Ten meters!" (Zombie)
8. "Skeleton drawing a bow! Get to cover!" (Skeleton)
9. "Creeper closing! Don't let it get close!" (Creeper)
10. "Don't look at it!" (Enderman - special)
11. "Spider incoming! Watch the walls!" (Spider)
12. "Phantom diving! Take cover!" (Phantom)
```

### 8.3 Engaged Phase Templates (12 templates)

**Generic Combat:**
```
1. "Engaging hostile!"
2. "Contact! Taking the fight to them!"
3. "Locking onto target!"
4. "Weapons free! Engaging!"
5. "Target acquired! Firing!"
6. "Hostile down! Threat eliminated!"
```

**Mob-Specific Combat:**
```
7. "Engaging zombie!" (Zombie)
8. "Flanking the skeleton!" (Skeleton)
9. "Knockback! Keep it away!" (Creeper)
10. "Target locked: Enderman!" (Enderman)
11. "Crush the spider!" (Spider)
12. "Anti-air! Phantom engaging!" (Phantom)
```

### 8.4 Retreating Phase Templates (8 templates)

```
1. "I'm hit! Falling back!"
2. "Can't hold! Need help!"
3. "Taking damage! Retreat!"
4. "Overwhelmed! Requesting backup!"
5. "Health critical! Back off!"
6. "Can't take much more of this!"
7. "Tactical retreat! Move!"
8. "Regrouping! Falling back!"
```

### 8.5 Victory Celebrations (8 templates)

```
1. "Target down! Hostile eliminated!"
2. "Clean hit! One shot!"
3. "That was close! But we got it!"
4. "Area secure. All hostiles down."
5. "Scraps one!"
6. "Threat neutralized!"
7. "Sector clear. Moving on."
8. "Combat complete. Resuming operations."
```

### 8.6 Guard-Specific Tactical Communication (8 templates)

**Detection Reports:**
```
1. "Visual contact acquired. {mobType}, bearing {direction}, distance {distance}."
2. "Scanning sector. Hostile signature detected."
3. "Threat assessment: {mobType}. Danger level elevated."
4. "Perimeter breach at {location}. Responding."
```

**Combat Commands:**
```
5. "Defensive perimeter forming!"
6. "Fall back to defensive positions!"
7. "Hold the line! Do not break!"
8. "Rally point! Regroup on me!"
```

### 8.7 Cooperative Combat Templates (6 templates)

```
1. "Attack on my mark! Three... two... one!"
2. "I've got your back!"
3. "Need covering fire!"
4. "Team effort! Good work everyone!"
5. "Flanking! You take the front!"
6. "Support required! Engaging now!"
```

---

## Integration Points

### With CombatAction
The `CombatDialogueManager` integrates with `CombatAction` to provide combat commentary:

```java
// In CombatAction.java
@Override
protected void onStart() {
    // ... existing code ...
    if (foreman.getCombatDialogueManager() != null) {
        foreman.getCombatDialogueManager().onCombatBegin(target);
    }
}

@Override
protected void onTick() {
    // ... existing code ...
    if (foreman.getCombatDialogueManager() != null) {
        foreman.getCombatDialogueManager().tick();
    }
}
```

### With ProactiveDialogueManager
Combat dialogue takes priority over general proactive dialogue:

```java
// In ProactiveDialogueManager.java
public void tick() {
    if (foreman.getCombatDialogueManager() != null &&
        foreman.getCombatDialogueManager().getCurrentThreatLevel() != ThreatLevel.SCOUTING) {
        return;  // Let combat dialogue handle it
    }
    // ... existing code ...
}
```

### With Guard Role System
Guards use formal tactical language:

```java
private String getScoutingDialogue(String mobType, int distance, String direction,
                                   WorkerRole workerRole) {
    if (workerRole == WorkerRole.GUARD) {
        return getGuardScoutingDialogue(mobType, distance, direction);
    }
    // ... existing code ...
}
```

---

## Configuration

### config/minewright-common.toml

```toml
[CombatDialogue]
# Enable combat threat detection and dialogue
enabled = true

# Detection ranges (in blocks)
scouting_range = 48
approaching_range = 16
engaged_range = 3.5

# Dialogue cooldowns (in ticks)
scouting_cooldown = 1200  # 60 seconds
approaching_cooldown = 400  # 20 seconds
engaged_cooldown = 100  # 5 seconds
retreating_cooldown = 200  # 10 seconds

# Enable role-specific dialogue variations
role_specific_dialogue = true

# Guard tactical communication style
guard_formal_dialogue = true

# Chance to call out damage (0.0 to 1.0)
damage_callout_chance = 0.3

# Enable cooperative combat alerts
cooperative_alerts = true
```

---

## Testing Strategy

### Unit Tests
1. Test threat detection at each range band
2. Test dialogue cooldown timing
3. Test mob-specific dialogue selection
4. Test guard vs non-guard dialogue differences
5. Test retreat trigger (low health)

### Integration Tests
1. Test CombatDialogueManager with CombatAction
2. Test cooperative alerts between multiple workers
3. Test priority over ProactiveDialogueManager
4. Test cross-worker threat reporting

### In-Game Tests
1. Spawn each mob type and verify dialogue
2. Test multi-threat scenarios
3. Test guard-worker cooperation
4. Test retreat behavior
5. Test dialogue spam prevention

---

## Future Enhancements

1. **Learning System:** Workers remember which mobs killed them and express fear
2. **Personalized Fear:** Individual workers develop phobias based on experiences
3. **Tactical Evolution:** Workers suggest combat strategies based on mob patterns
4. **Veterancy:** Experienced guards use more sophisticated tactical language
5. **Emergency Protocols:** Coordinated retreat patterns with rally points
6. **Weapon-Specific Dialogue:** Different commentary for swords, bows, etc.

---

## Sources

### Military Communication
- [Military Communication Protocols (Web Search Summary)](https://example.com/military-comms-2025)
- Standard radio terminology: "Roger", "Wilco", "Over", "Out"
- Tactical communication structure and call signs
- JTAC certification standards (95% accuracy requirement)

### Game Combat Barks
- [Left 4 Dead Dialogue System Analysis (rootstew.com)](https://rootstew.com/l4d-dialogue-system)
- [Designing Game Barks for Narrative (Indienova)](https://indienova.com/barks-narrative-design)
- Left 4 Dead voice command system (PlayerWarnTank, PlayerWarnWitch, etc.)
- Overwatch combat bark system

### Combat Stress Psychology
- [Sumo Digital: Psychology of Horror Games](https://www.sumo-digital.com/news-insights/deep-dive-into-horror-games/)
- Fight-or-Flight Response Theory (MBA Lib Encyclopedia)
- [Military Combat Stress Research](https://nap.edu/read/2045/chapter/6)
- Air Force pilot stress psychology requirements

### Minecraft Mob Behavior
- [Minecraft Mob Behavior Patterns (2024)](https://minecraft.wiki)
- Zombie detection range (35-40 blocks), reinforcement mechanics
- Skeleton ranged attacks and strafing behavior
- Creeper silent movement and explosion mechanics
- Enderman stare detection and teleportation
- Spider wall climbing and ambush patterns
- Phantom spawning mechanics (sleep deprivation trigger)

### Character Design
- [Research: AI Companion Personality Design](C:\Users\casey\steve\docs\RESEARCH_COMPANION_PERSONALITY.md)
- [Worker Role System for MineWright](C:\Users\casey\steve\docs\WORKER_ROLE_SYSTEM.md)
- Big Five personality traits and dialogue expression
- Proactive behavior and timing algorithms

---

**Document Version:** 1.0
**Author:** MineWright Development Team
**Status:** Research Complete - Ready for Implementation
**Last Updated:** 2026-02-27
