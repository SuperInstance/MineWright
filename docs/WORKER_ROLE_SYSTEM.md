# Worker Role System for MineWright Multi-Agent Coordination

**Project:** MineWright - Minecraft Autonomous Agents
**Component:** Worker Role System with Contract Net Protocol
**Version:** 1.0
**Date:** 2026-02-27
**Status:** Research & Design Document

---

## Executive Summary

This document outlines the implementation of a Worker Role System for MineWright's multi-agent coordination. The system extends the existing `AgentRole` enum (FOREMAN, WORKER, SPECIALIST, SOLO) with specialized worker roles (Miner, Builder, Guard, Crafter) and implements Contract Net Protocol (CNP) for intelligent task bidding and assignment.

**Key Design Principles:**
1. **Capability-Based Routing:** Tasks are assigned based on worker capabilities, not just availability
2. **Contract Net Protocol:** Workers bid on tasks they're qualified for, foreman awards to best bidder
3. **Nickname Identity:** Crew members get personalized nicknames (e.g., "Sparks", "Dusty") not ID numbers
4. **Progressive Specialization:** Workers develop expertise through task completion and skill progression
5. **Brand Consistency:** Maintains Mace MineWright's professional contractor personality throughout

---

## Table of Contents

1. [Current Architecture Analysis](#current-architecture-analysis)
2. [WorkerRole Enum Design](#workerrole-enum-design)
3. [Capability System](#capability-system)
4. [Contract Net Protocol Implementation](#contract-net-protocol-implementation)
5. [Role-Based Action Filtering](#role-based-action-filtering)
6. [Task Assignment with Capability Matching](#task-assignment-with-capability-matching)
7. [Nickname Assignment System](#nickname-assignment-system)
8. [Integration with Existing Systems](#integration-with-existing-systems)
9. [Implementation Roadmap](#implementation-roadmap)

---

## Current Architecture Analysis

### Existing AgentRole System

**Location:** `C:\Users\casey\steve\src\main\java\com\minewright\orchestration\AgentRole.java`

**Current Enum:**
```java
public enum AgentRole {
    FOREMAN("Foreman", true, true),
    WORKER("Worker", false, true),
    SPECIALIST("Specialist", false, true),
    SOLO("Solo", false, false);
}
```

**Current Limitations:**
- All workers are identical - no differentiation between capabilities
- No capability checking before task assignment
- Round-robin task distribution doesn't consider worker suitability
- Nicknames not implemented (crew get generic names like "Foreman-1")
- SPECIALIST role defined but not implemented

**Existing Infrastructure to Leverage:**

| Component | Location | Purpose |
|-----------|----------|---------|
| `OrchestratorService` | `orchestration/OrchestratorService.java` | Task distribution coordination |
| `AgentCommunicationBus` | `orchestration/AgentCommunicationBus.java` | Message passing between agents |
| `AgentMessage` | `orchestration/AgentMessage.java` | Message format with types like TASK_ASSIGNMENT |
| `TaskAssignment` | `orchestration/TaskAssignment.java` | Task lifecycle tracking |
| `ForemanEntity` | `entity/ForemanEntity.java` | Worker entity with message handling |
| `ActionExecutor` | `action/ActionExecutor.java` | Plugin-based action execution |
| `ProactiveDialogueManager` | `dialogue/ProactiveDialogueManager.java` | Personality-driven dialogue |

---

## WorkerRole Enum Design

### Enhanced WorkerRole Hierarchy

The `AgentRole.WORKER` becomes a parent category, with `WorkerRole` defining specific specializations:

```java
/**
 * Specialized worker roles within the WORKER category.
 * Each role has distinct capabilities and preferred task types.
 */
public enum WorkerRole {

    /**
     * MINER - Resource extraction specialist
     *
     * Capabilities: Mining, ore detection, tunneling, underground navigation
     * Preferred Actions: mine, gather, explore_caves
     * Efficiency Bonus: +50% mining speed, +30% ore yield
     * Nickname Style: Industrial, electrical themes ("Sparks", "Fuse", "Static")
     */
    MINER("Miner", CapabilitySet.MINING_CAPABILITIES),

    /**
     * BUILDER - Construction specialist
     *
     * Capabilities: Block placement, structure assembly, scaffolding
     * Preferred Actions: build, place, construct
     * Efficiency Bonus: +40% placement speed, structural integrity awareness
     * Nickname Style: Professional, architectural ("Draft", "Level", "Chalk")
     */
    BUILDER("Builder", CapabilitySet.BUILDING_CAPABILITIES),

    /**
     * GUARD - Combat and perimeter defense
     *
     * Capabilities: Combat, threat detection, patrol
     * Preferred Actions: attack, defend, patrol
     * Efficiency Bonus: +60% combat damage, wider threat detection
     * Nickname Style: Military, protective ("Shield", "Barricade", "Watch")
     */
    GUARD("Guard", CapabilitySet.COMBAT_CAPABILITIES),

    /**
     * CRAFTER - Item creation and processing
     *
     * Capabilities: Crafting, smelting, redstone
     * Preferred Actions: craft, smelt, enchant
     * Efficiency Bonus: Instant crafting, furnace management
     * Nickname Style: Technical, academic ("Professor", "Gears", "Circuit")
     */
    CRAFTER("Crafter", CapabilitySet.CRAFTING_CAPABILITIES),

    /**
     * GENERALIST - Jack of all trades (default role)
     *
     * Capabilities: Basic proficiency in all areas
     * Preferred Actions: Any (no bonus)
     * Efficiency Bonus: None
     * Nickname Style: Generic, dependable ("Helper", "Handy", "Ace")
     */
    GENERALIST("Generalist", CapabilitySet.GENERAL_CAPABILITIES);

    private final String displayName;
    private final CapabilitySet capabilities;

    WorkerRole(String displayName, CapabilitySet capabilities) {
        this.displayName = displayName;
        this.capabilities = capabilities;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CapabilitySet getCapabilities() {
        return capabilities;
    }

    /**
     * Checks if this role can efficiently perform the given action.
     */
    public boolean canPerformAction(String action) {
        return capabilities.supportsAction(action);
    }

    /**
     * Returns efficiency multiplier for the given action (0.0 to 2.0).
     */
    public double getEfficiencyForAction(String action) {
        return capabilities.getEfficiency(action);
    }
}
```

### CapabilitySet Design

```java
/**
 * Defines the set of capabilities for a worker role.
 */
public class CapabilitySet {

    private final Set<String> supportedActions;
    private final Map<String, Double> efficiencyMultipliers;
    private final Set<String> uniqueAbilities;

    public CapabilitySet(Set<String> supportedActions,
                        Map<String, Double> efficiencyMultipliers,
                        Set<String> uniqueAbilities) {
        this.supportedActions = Collections.unmodifiableSet(supportedActions);
        this.efficiencyMultipliers = Collections.unmodifiableMap(efficiencyMultipliers);
        this.uniqueAbilities = Collections.unmodifiableSet(uniqueAbilities);
    }

    // Predefined capability sets
    public static final CapabilitySet MINING_CAPABILITIES = new CapabilitySet(
        Set.of("mine", "gather", "explore_caves", "tunnel"),
        Map.of(
            "mine", 1.5,      // +50% mining speed
            "gather", 1.3,    // +30% resource yield
            "explore_caves", 1.5
        ),
        Set.of("ore_detection", "tunnel_navigation")
    );

    public static final CapabilitySet BUILDING_CAPABILITIES = new CapabilitySet(
        Set.of("build", "place", "construct", "blueprint"),
        Map.of(
            "build", 1.4,     // +40% building speed
            "place", 1.4,
            "construct", 1.3
        ),
        Set.of("structural_integrity", "scaffolding")
    );

    public static final CapabilitySet COMBAT_CAPABILITIES = new CapabilitySet(
        Set.of("attack", "defend", "patrol", "guard"),
        Map.of(
            "attack", 1.6,    // +60% combat damage
            "defend", 1.5,
            "patrol", 1.3
        ),
        Set.of("threat_detection", "perimeter_alert")
    );

    public static final CapabilitySet CRAFTING_CAPABILITIES = new CapabilitySet(
        Set.of("craft", "smelt", "enchant", "repair"),
        Map.of(
            "craft", 2.0,     // Instant crafting
            "smelt", 1.5,
            "enchant", 1.3
        ),
        Set.of("recipe_knowledge", "furnace_management")
    );

    public static final CapabilitySet GENERAL_CAPABILITIES = new CapabilitySet(
        Set.of("mine", "build", "attack", "craft", "gather", "place"),
        Map.of( // All 1.0 (no bonus)
            "mine", 1.0, "build", 1.0, "attack", 1.0,
            "craft", 1.0, "gather", 1.0, "place", 1.0
        ),
        Set.of()
    );

    public boolean supportsAction(String action) {
        return supportedActions.contains(action.toLowerCase());
    }

    public double getEfficiency(String action) {
        return efficiencyMultipliers.getOrDefault(action.toLowerCase(), 1.0);
    }

    public boolean hasUniqueAbility(String ability) {
        return uniqueAbilities.contains(ability.toLowerCase());
    }
}
```

---

## Capability System

### Action Type Classification

```java
/**
 * Classifies actions into capability categories.
 */
public enum ActionType {

    // Primary action types
    MINING("mine", "gather", "quarry"),
    BUILDING("build", "place", "construct", "assemble"),
    COMBAT("attack", "defend", "fight", "guard"),
    CRAFTING("craft", "smelt", "enchant", "repair"),

    // Secondary action types
    LOGISTICS("transport", "deliver", "fetch"),
    EXPLORATION("explore", "scout", "survey"),
    MAINTENANCE("repair", "clean", "refuel"),
    COORDINATION("coordinate", "manage", "supervise");

    private final Set<String> aliases;

    ActionType(String... aliases) {
        this.aliases = Set.of(aliases);
    }

    public static ActionType classifyAction(String action) {
        String actionLower = action.toLowerCase();

        for (ActionType type : values()) {
            if (type.aliases.contains(actionLower)) {
                return type;
            }
        }

        // Default to LOGISTICS for unknown actions
        return LOGISTICS;
    }
}
```

### WorkerCapability Interface

```java
/**
 * Interface for worker capability checking.
 */
public interface WorkerCapability {

    /**
     * Checks if this worker can perform the given task.
     * @param task The task to check
     * @return true if the worker has the required capability
     */
    boolean canPerform(Task task);

    /**
     * Calculates the efficiency score for performing the task.
     * Higher scores indicate better capability match.
     * @param task The task to score
     * @return Efficiency score (0.0 to 2.0, where 1.0 is baseline)
     */
    double getEfficiencyScore(Task task);

    /**
     * Estimates completion time for the task based on capabilities.
     * @param task The task to estimate
     * @return Estimated time in ticks
     */
    int estimateCompletionTime(Task task);

    /**
     * Gets the worker's primary role.
     */
    WorkerRole getRole();

    /**
     * Gets the worker's secondary role (if any, for cross-training).
     */
    Optional<WorkerRole> getSecondaryRole();
}
```

---

## Contract Net Protocol Implementation

### Protocol Overview

Contract Net Protocol (CNP) is a negotiation protocol for task allocation:

1. **Announcement:** Foreman announces task to all workers
2. **Bidding:** Qualified workers submit bids with their capability score
3. **Evaluation:** Foreman evaluates bids based on capability, workload, location
4. **Award:** Foreman awards task to best bidder
5. **Acceptance/Rejection:** Worker accepts or rejects the award
6. **Execution:** Worker performs the task
7. **Completion:** Worker reports results back to foreman

### CNP Message Types

```java
/**
 * Contract Net Protocol message types.
 * Extends AgentMessage.Type with CNP-specific types.
 */
public enum CNPMessageType {

    /**
     * Foreman announces a task is available for bidding.
     * Payload: taskDescription, taskType, requiredCapabilities, deadline
     */
    TASK_ANNOUNCEMENT,

    /**
     * Worker submits a bid for a task.
     * Payload: taskId, capabilityScore, estimatedTime, confidence
     */
    TASK_BID,

    /**
     * Foreman awards the task to a specific worker.
     * Payload: taskId, selectedWorkerId, contractTerms
     */
    TASK_AWARD,

    /**
     * Worker accepts the task award.
     * Payload: taskId, acceptedTerms
     */
    AWARD_ACCEPTED,

    /**
     * Worker rejects the task award.
     * Payload: taskId, rejectionReason
     */
    AWARD_REJECTED,

    /**
     * Worker declares task completion.
     * Payload: taskId, result, actualTime
     */
    TASK_COMPLETE,

    /**
     * Worker reports task failure.
     * Payload: taskId, failureReason, progressMade
     */
    TASK_FAILED;
}
```

### CNP Implementation in OrchestratorService

```java
/**
 * Contract Net Protocol implementation for task allocation.
 * Integrated into OrchestratorService.
 */
public class ContractNetProtocol {

    private final OrchestratorService orchestrator;
    private final AgentCommunicationBus communicationBus;
    private final Map<String, TaskAnnouncement> activeAnnouncements;
    private final Map<String, List<TaskBid>> pendingBids;

    /**
     * Initiates CNP for a task by sending announcement to all workers.
     */
    public void announceTask(Task task, String planId) {
        String announcementId = UUID.randomUUID().toString().substring(0, 8);

        // Classify the task type
        ActionType taskType = ActionType.classifyAction(task.getAction());

        // Determine required capabilities
        Set<String> requiredCapabilities = determineRequiredCapabilities(task);

        // Create announcement
        TaskAnnouncement announcement = new TaskAnnouncement(
            announcementId,
            task,
            planId,
            taskType,
            requiredCapabilities,
            Instant.now().plusSeconds(30) // 30 second bidding deadline
        );

        activeAnnouncements.put(announcementId, announcement);
        pendingBids.put(announcementId, new CopyOnWriteArrayList<>());

        // Send announcement to all workers
        AgentMessage announcementMsg = new AgentMessage.Builder()
            .type(AgentMessage.Type.CNP_ANNOUNCEMENT)
            .sender(orchestrator.getForemanId(), "Foreman")
            .recipient("*") // Broadcast to all workers
            .content("Task available for bid: " + task.getAction())
            .payload("announcementId", announcementId)
            .payload("taskDescription", task.getAction())
            .payload("taskType", taskType.name())
            .payload("requiredCapabilities", requiredCapabilities)
            .payload("deadline", announcement.getDeadline().toString())
            .priority(AgentMessage.Priority.NORMAL)
            .build();

        communicationBus.publish(announcementMsg);

        MineWrightMod.LOGGER.info("[CNP] Task announced: {} ({}) - Bidding closes in 30s",
            task.getAction(), taskType);
    }

    /**
     * Handles incoming bid from a worker.
     */
    public void handleBid(String workerId, AgentMessage bidMessage) {
        String announcementId = bidMessage.getPayloadValue("announcementId", "");

        TaskAnnouncement announcement = activeAnnouncements.get(announcementId);
        if (announcement == null) {
            MineWrightMod.LOGGER.warn("[CNP] Bid for unknown announcement: {}", announcementId);
            return;
        }

        // Extract bid details
        double capabilityScore = bidMessage.getPayloadValue("capabilityScore", 0.0);
        int estimatedTime = bidMessage.getPayloadValue("estimatedTime", 999);
        double confidence = bidMessage.getPayloadValue("confidence", 0.5);

        // Create bid
        TaskBid bid = new TaskBid(
            workerId,
            announcementId,
            capabilityScore,
            estimatedTime,
            confidence,
            Instant.now()
        );

        // Store bid
        List<TaskBid> bids = pendingBids.get(announcementId);
        bids.add(bid);

        MineWrightMod.LOGGER.info("[CNP] Bid received from {}: score={}, time={}, confidence={}",
            workerId, capabilityScore, estimatedTime, confidence);

        // Check if bidding deadline has passed
        if (Instant.now().isAfter(announcement.getDeadline())) {
            evaluateAndAwardTask(announcementId);
        }
    }

    /**
     * Evaluates all bids and awards task to best bidder.
     */
    private void evaluateAndAwardTask(String announcementId) {
        TaskAnnouncement announcement = activeAnnouncements.get(announcementId);
        List<TaskBid> bids = pendingBids.get(announcementId);

        if (bids.isEmpty()) {
            MineWrightMod.LOGGER.warn("[CNP] No bids received for task: {}",
                announcement.getTask().getAction());

            // Revert to round-robin assignment
            orchestrator.assignTaskRoundRobin(announcement.getTask());
            return;
        }

        // Score each bid
        TaskBid bestBid = null;
        double bestScore = -1.0;

        for (TaskBid bid : bids) {
            double score = calculateBidScore(bid, announcement);

            MineWrightMod.LOGGER.info("[CNP] Scoring bid from {}: {}",
                bid.getWorkerId(), String.format("%.2f", score));

            if (score > bestScore) {
                bestScore = score;
                bestBid = bid;
            }
        }

        // Award task to best bidder
        if (bestBid != null) {
            awardTask(announcement, bestBid);
        }

        // Clean up
        activeAnnouncements.remove(announcementId);
        pendingBids.remove(announcementId);
    }

    /**
     * Calculates the overall score for a bid.
     * Considers: capability score, estimated time, worker workload, distance
     */
    private double calculateBidScore(TaskBid bid, TaskAnnouncement announcement) {
        double score = 0.0;

        // Capability score (0-100 points)
        score += bid.getCapabilityScore() * 100;

        // Time penalty (prefer faster completion)
        int estimatedTime = bid.getEstimatedTime();
        score += Math.max(0, 50 - (estimatedTime / 10)); // Max 50 points

        // Confidence bonus (0-20 points)
        score += bid.getConfidence() * 20;

        // Current workload penalty
        String workerId = bid.getWorkerId();
        TaskAssignment currentAssignment = orchestrator.getWorkerAssignment(workerId);
        if (currentAssignment != null && !currentAssignment.isTerminal()) {
            score -= 30; // Penalty for busy workers
        }

        // Distance to task location (if applicable)
        double distance = orchestrator.getDistanceToTask(workerId, announcement.getTask());
        score -= Math.min(20, distance / 10); // Max 20 point penalty

        return score;
    }

    /**
     * Awards the task to the selected worker.
     */
    private void awardTask(TaskAnnouncement announcement, TaskBid selectedBid) {
        String workerId = selectedBid.getWorkerId();

        // Send award message
        AgentMessage awardMsg = new AgentMessage.Builder()
            .type(AgentMessage.Type.TASK_AWARD)
            .sender(orchestrator.getForemanId(), "Foreman")
            .recipient(workerId)
            .content("You've been awarded the task: " + announcement.getTask().getAction())
            .payload("announcementId", announcement.getAnnouncementId())
            .payload("taskId", announcement.getTask().getAction())
            .payload("contractTerms", "Complete within " + selectedBid.getEstimatedTime() + " ticks")
            .priority(AgentMessage.Priority.HIGH)
            .build();

        communicationBus.publish(awardMsg);

        // Create task assignment
        TaskAssignment assignment = new TaskAssignment(
            orchestrator.getForemanId(),
            announcement.getTask(),
            announcement.getPlanId()
        );
        assignment.assignTo(workerId);
        assignment.accept(); // Pre-accept since they bid

        // Track assignment
        orchestrator.trackAssignment(assignment);

        MineWrightMod.LOGGER.info("[CNP] Task awarded to {} for task: {} (score: {})",
            workerId, announcement.getTask().getAction(),
            String.format("%.2f", calculateBidScore(selectedBid, announcement)));

        // Notify other bidders they weren't selected
        notifyUnsuccessfulBidders(announcement.getAnnouncementId(), workerId);
    }

    private void notifyUnsuccessfulBidders(String announcementId, String selectedWorkerId) {
        List<TaskBid> bids = pendingBids.get(announcementId);

        for (TaskBid bid : bids) {
            if (!bid.getWorkerId().equals(selectedWorkerId)) {
                AgentMessage rejectionMsg = new AgentMessage.Builder()
                    .type(AgentMessage.Type.CNP_REJECTION)
                    .sender(orchestrator.getForemanId(), "Foreman")
                    .recipient(bid.getWorkerId())
                    .content("Task awarded to another worker")
                    .payload("announcementId", announcementId)
                    .priority(AgentMessage.Priority.LOW)
                    .build();

                communicationBus.publish(rejectionMsg);
            }
        }
    }

    /**
     * Determines required capabilities for a task.
     */
    private Set<String> determineRequiredCapabilities(Task task) {
        ActionType taskType = ActionType.classifyAction(task.getAction());

        return switch (taskType) {
            case MINING -> Set.of("mining");
            case BUILDING -> Set.of("building");
            case COMBAT -> Set.of("combat");
            case CRAFTING -> Set.of("crafting");
            default -> Set.of(); // No specific requirements
        };
    }
}
```

---

## Role-Based Action Filtering

### Action Filtering in ForemanEntity

When a worker receives a task assignment, it checks if the action is compatible with its role:

```java
/**
 * Enhanced ForemanEntity with role-based action filtering.
 */
public class ForemanEntity extends PathfinderMob {

    private WorkerRole workerRole = WorkerRole.GENERALIST;
    private String nickname = null;

    /**
     * Handles task assignment with role filtering.
     */
    private void handleTaskAssignment(AgentMessage message) {
        if (role == AgentRole.FOREMAN) {
            return; // Foreman doesn't receive task assignments
        }

        String taskDescription = message.getPayloadValue("taskDescription", "Unknown task");
        Task task = new Task(taskDescription, message.getPayload());

        // Check if this worker can perform the task based on role
        if (!canPerformTask(task)) {
            // Reject the task
            AgentMessage rejection = AgentMessage.taskComplete(
                entityName, entityName,
                message.getSenderId(),
                message.getMessageId(),
                false,
                "Task incompatible with my role as " + workerRole.getDisplayName()
            );
            orchestrator.getCommunicationBus().publish(rejection);

            sendChatMessage("Sorry, that's not my specialty. I'm a " + workerRole.getDisplayName() + ".");
            return;
        }

        // Accept the task
        sendChatMessage("Accepting task: " + taskDescription);
        currentTaskId = message.getMessageId();

        // Add to action queue
        actionExecutor.queueTask(task);

        // Send acknowledgment
        AgentMessage ack = AgentMessage.taskProgress(
            entityName, entityName,
            message.getSenderId(),
            message.getMessageId(),
            0,
            "Task accepted by " + workerRole.getDisplayName()
        );
        orchestrator.getCommunicationBus().publish(ack);
    }

    /**
     * Checks if this worker can perform the given task.
     */
    private boolean canPerformTask(Task task) {
        String action = task.getAction();

        // Check primary role
        if (workerRole.canPerformAction(action)) {
            return true;
        }

        // Check secondary role if cross-trained
        if (secondaryRole != null && secondaryRole.canPerformAction(action)) {
            return true;
        }

        // GENERALIST can do anything (with no bonus)
        return workerRole == WorkerRole.GENERALIST;
    }

    /**
     * Gets the efficiency multiplier for the current action.
     */
    public double getActionEfficiency(String action) {
        // Check primary role
        if (workerRole.canPerformAction(action)) {
            return workerRole.getEfficiencyForAction(action);
        }

        // Check secondary role (reduced efficiency)
        if (secondaryRole != null && secondaryRole.canPerformAction(action)) {
            return secondaryRole.getEfficiencyForAction(action) * 0.7;
        }

        // GENERALIST baseline
        return 1.0;
    }
}
```

### ActionExecutor Integration

```java
/**
 * Enhanced ActionExecutor with role-based efficiency.
 */
public class ActionExecutor {

    /**
     * Applies role-based efficiency to action execution.
     */
    private void applyRoleEfficiency(BaseAction action) {
        String actionType = action.getClass().getSimpleName()
            .replace("Action", "").toLowerCase();

        double efficiency = foreman.getActionEfficiency(actionType);

        // Apply efficiency modifier to action
        action.setEfficiencyModifier(efficiency);

        if (efficiency != 1.0) {
            MineWrightMod.LOGGER.info("Role efficiency applied: {}x for {} (role: {})",
                String.format("%.1f", efficiency),
                actionType,
                foreman.getWorkerRole().getDisplayName());
        }
    }
}
```

---

## Task Assignment with Capability Matching

### Specialization-Aware Task Router

```java
/**
 * Routes tasks to workers based on capability matching.
 */
public class CapabilityAwareTaskRouter {

    private final Map<String, WorkerCapability> workerCapabilities;
    private final CrewManager crewManager;

    /**
     * Assigns a task to the best-suited available worker.
     */
    public String assignTask(Task task, Collection<ForemanEntity> availableWorkers) {
        // Classify the task
        ActionType taskType = ActionType.classifyAction(task.getAction());

        // Score each worker
        Map<String, Double> scores = new HashMap<>();

        for (ForemanEntity worker : availableWorkers) {
            if (worker.getRole() == AgentRole.FOREMAN) {
                continue; // Skip foreman
            }

            double score = calculateWorkerScore(worker, task, taskType);
            scores.put(worker.getSteveName(), score);

            MineWrightMod.LOGGER.debug("Worker {} scored {} for task {}",
                worker.getSteveName(),
                String.format("%.2f", score),
                task.getAction());
        }

        // Select best worker
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(e -> e.getValue() > 0.5) // Minimum threshold
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * Calculates a worker's suitability score for a task.
     */
    private double calculateWorkerScore(ForemanEntity worker, Task task, ActionType taskType) {
        double score = 0.0;

        WorkerRole role = worker.getWorkerRole();

        // Base capability score (0-1.0)
        if (role.canPerformAction(task.getAction())) {
            score += role.getEfficiencyForAction(task.getAction()) * 0.5;
        } else {
            return 0.0; // Cannot perform task at all
        }

        // Skill level bonus (0-0.3)
        // score += getSkillLevel(worker, taskType) * 0.003;

        // Current workload penalty
        if (isWorkerBusy(worker)) {
            score -= 0.3;
        }

        // Distance to task location
        // double distance = getDistanceToTask(worker, task);
        // score -= Math.min(0.2, distance / 100.0);

        return Math.max(0.0, score);
    }

    private boolean isWorkerBusy(ForemanEntity worker) {
        return worker.getActionExecutor().isExecuting();
    }
}
```

---

## Nickname Assignment System

### Nickname Assignment Patterns

```java
/**
 * Manages nickname assignment for crew members.
 * Implements Mace MineWright's "Zero-Grievance Policy" - no ID numbers.
 */
public class NicknameManager {

    private final Map<String, String> nicknames = new ConcurrentHashMap<>();
    private final Map<WorkerRole, List<String>> nicknamePools;

    public NicknameManager() {
        // Initialize nickname pools by role
        this.nicknamePools = Map.of(
            WorkerRole.MINER, List.of(
                "Sparks", "Fuse", "Static", "Dusty", "Tunnel",
                "Vein", "Ore", "Strata", "Bedrock", "Gem"
            ),
            WorkerRole.BUILDER, List.of(
                "Draft", "Level", "Chalk", "Square", "Plumb",
                "Frame", "Beam", "Foundation", "Roof", "Blueprint"
            ),
            WorkerRole.GUARD, List.of(
                "Shield", "Barricade", "Watch", "Tower", "Fort",
                "Patrol", "Sentry", "Warden", "Guardian", "Bastion"
            ),
            WorkerRole.CRAFTER, List.of(
                "Professor", "Gears", "Circuit", "Furnace", "Anvil",
                "Recipe", "Craft", "Smelt", "Enchant", "Smith"
            ),
            WorkerRole.GENERALIST, List.of(
                "Helper", "Handy", "Ace", "Pro", "Expert",
                "Reliable", "Steady", "Ready", "Swift", "Sure"
            )
        );
    }

    /**
     * Assigns a nickname to a crew member based on their role.
     * Ensures uniqueness within the crew.
     */
    public String assignNickname(String entityId, WorkerRole role) {
        // Get available nicknames for this role
        List<String> pool = new ArrayList<>(nicknamePools.getOrDefault(
            role,
            nicknamePools.get(WorkerRole.GENERALIST)
        ));

        // Remove already-taken nicknames
        pool.removeAll(nicknames.values());

        if (pool.isEmpty()) {
            // Fallback: Generate unique nickname
            return generateUniqueNickname(role, nicknames.size());
        }

        // Select random nickname
        String nickname = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        nicknames.put(entityId, nickname);

        MineWrightMod.LOGGER.info("Assigned nickname '{}' to {} (role: {})",
            nickname, entityId, role);

        return nickname;
    }

    /**
     * Gets the nickname for an entity.
     */
    public String getNickname(String entityId) {
        return nicknames.getOrDefault(entityId, entityId);
    }

    /**
     * Generates a unique nickname when pool is exhausted.
     */
    private String generateUniqueNickname(WorkerRole role, int suffix) {
        return role.getDisplayName() + "-" + (suffix + 1);
    }

    /**
     * Assigns a manual nickname (player-specified).
     */
    public void setManualNickname(String entityId, String nickname) {
        nicknames.put(entityId, nickname);
        MineWrightMod.LOGGER.info("Manual nickname '{}' assigned to {}", nickname, entityId);
    }
}
```

### Nickname Assignment in ForemanEntity

```java
/**
 * ForemanEntity with nickname assignment.
 */
public class ForemanEntity extends PathfinderMob {

    private String nickname = null;
    private WorkerRole workerRole = WorkerRole.GENERALIST;

    /**
     * Called during registration to assign nickname and role.
     */
    public void assignIdentity(WorkerRole role, NicknameManager nicknameManager) {
        this.workerRole = role;

        // Assign nickname if not already set
        if (this.nickname == null) {
            this.nickname = nicknameManager.assignNickname(
                this.getUUID().toString(),
                role
            );

            // Update display name
            setEntityName(nickname);

            // Announce identity
            sendChatMessage(String.format("Identity assigned: I'm %s the %s, ready to work!",
                nickname, role.getDisplayName()));
        }
    }

    /**
     * Gets the worker's nickname.
     */
    public String getNickname() {
        return nickname != null ? nickname : getEntityName();
    }

    @Override
    public void setEntityName(String name) {
        this.nickname = name;
        this.entityData.set(ENTITY_NAME, name);
        this.setCustomName(Component.literal(name));
    }
}
```

---

## Integration with Existing Systems

### OrchestratorService Enhancements

```java
/**
 * Enhanced OrchestratorService with WorkerRole support.
 */
public class OrchestratorService {

    private final ContractNetProtocol contractNetProtocol;
    private final CapabilityAwareTaskRouter taskRouter;
    private final NicknameManager nicknameManager;

    /**
     * Registers an agent with role and nickname assignment.
     */
    public void registerAgent(ForemanEntity minewright, AgentRole role) {
        String agentId = minewright.getSteveName();

        // Register with communication bus
        communicationBus.registerAgent(agentId, agentId);

        if (role == AgentRole.FOREMAN) {
            // Foreman registration (unchanged)
            if (foremanId != null && !foremanId.equals(agentId)) {
                LOGGER.warn("Replacing foreman: {} -> {}", foremanId, agentId);
            }
            this.foremanId = agentId;
            LOGGER.info("Registered FOREMAN: {}", agentId);

            // Assign foreman nickname
            minewright.setEntityName("Mace");

        } else {
            // Worker registration with role assignment
            WorkerRole workerRole = assignWorkerRole(minewright);
            minewright.assignIdentity(workerRole, nicknameManager);

            workerRegistry.put(agentId, new WorkerInfo(agentId, agentId, role, workerRole));
            LOGGER.info("Registered WORKER: {} (role={}, nickname={})",
                agentId, role, minewright.getNickname());
        }

        // Subscribe to messages
        communicationBus.subscribe(agentId, message ->
            handleMessageFromAgent(agentId, message));
    }

    /**
     * Assigns a role to a new worker based on initial capabilities.
     * In full implementation, this would consider affinity scores.
     */
    private WorkerRole assignWorkerRole(ForemanEntity minewright) {
        // For now, assign random role
        // In production: Use affinity scores, player preference, or crew composition needs
        WorkerRole[] roles = WorkerRole.values();
        return roles[ThreadLocalRandom.current().nextInt(roles.length)];
    }

    /**
     * Process human command using Contract Net Protocol.
     */
    public String processHumanCommand(ResponseParser.ParsedResponse parsedResponse,
                                      Collection<ForemanEntity> availableSteves) {
        if (foremanId == null) {
            return processSoloCommand(parsedResponse, availableSteves);
        }

        String planId = UUID.randomUUID().toString().substring(0, 8);
        PlanExecution plan = new PlanExecution(
            planId,
            parsedResponse.getPlan(),
            parsedResponse.getTasks(),
            foremanId
        );

        activePlans.put(planId, plan);

        // Use CNP for task distribution
        for (Task task : parsedResponse.getTasks()) {
            contractNetProtocol.announceTask(task, planId);
        }

        return planId;
    }
}
```

### Enhanced WorkerInfo

```java
/**
 * Worker information with role and capability data.
 */
private static class WorkerInfo {
    final String id;
    final String name;
    final AgentRole role;
    final WorkerRole workerRole; // NEW
    final String nickname;        // NEW
    volatile Instant lastSeen;

    WorkerInfo(String id, String name, AgentRole role, WorkerRole workerRole) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.workerRole = workerRole;
        this.nickname = name; // Initially same, updated by NicknameManager
        this.lastSeen = Instant.now();
    }

    /**
     * Gets the worker's capability score for a task type.
     */
    public double getCapabilityScore(ActionType taskType) {
        return workerRole.getCapabilities().getEfficiency(taskType.name().toLowerCase());
    }
}
```

---

## Implementation Roadmap

### Phase 1: Core Role System (Week 1)

**Tasks:**
- [ ] Create `WorkerRole` enum with MINER, BUILDER, GUARD, CRAFTER, GENERALIST
- [ ] Create `CapabilitySet` class with role-specific capabilities
- [ ] Add `workerRole` field to `ForemanEntity`
- [ ] Implement `canPerformTask()` role filtering
- [ ] Add role-based efficiency multipliers

**Deliverables:**
- Workers have assigned roles
- Role filtering prevents incompatible task assignments
- Efficiency bonuses apply to role-appropriate tasks

### Phase 2: Nickname System (Week 1)

**Tasks:**
- [ ] Create `NicknameManager` with role-based nickname pools
- [ ] Implement nickname assignment on worker spawn
- [ ] Update `ForemanEntity` to use nicknames
- [ ] Add manual nickname command

**Deliverables:**
- Workers get role-appropriate nicknames
- Nicknames are unique within crew
- No ID numbers visible to players

### Phase 3: Contract Net Protocol (Week 2)

**Tasks:**
- [ ] Create `CNPMessageType` enum extending `AgentMessage.Type`
- [ ] Implement `ContractNetProtocol` class in `OrchestratorService`
- [ ] Add task announcement broadcast
- [ ] Implement bid submission handling
- [ ] Implement bid evaluation and award logic
- [ ] Add acceptance/rejection handling

**Deliverables:**
- Tasks are announced via CNP
- Workers submit capability-based bids
- Tasks awarded to best bidder
- Fallback to round-robin if no bids

### Phase 4: Capability-Aware Routing (Week 2)

**Tasks:**
- [ ] Create `ActionType` enum for task classification
- [ ] Implement `CapabilityAwareTaskRouter`
- [ ] Add worker scoring algorithm
- [ ] Integrate with `OrchestratorService.distributeTasks()`
- [ ] Add capability mismatch logging

**Deliverables:**
- Tasks routed based on capability matching
- Workers scored on multiple factors
- Best-suited worker selected

### Phase 5: Visual & Dialogue Polish (Week 3)

**Tasks:**
- [ ] Add role-based equipment visuals
- [ ] Implement role-specific dialogue
- [ ] Add particle effects for role-specific abilities
- [ ] Create HUD showing worker roles
- [ ] Add role-based proactive comments

**Deliverables:**
- Visual distinction between roles
- Role-appropriate dialogue
- HUD shows worker specializations

### Phase 6: Testing & Balance (Week 3-4)

**Tasks:**
- [ ] Test CNP with various task types
- [ ] Balance efficiency multipliers
- [ ] Adjust bid scoring weights
- [ ] Test nickname uniqueness
- [ ] Performance testing with 10+ workers

**Deliverables:**
- Balanced gameplay experience
- Smooth CNP operation
- No performance degradation

---

## Configuration

### Config File Format (minewright-common.toml)

```toml
[WorkerRoles]
# Enable worker role specialization
enabled = true

# Allow players to manually assign roles
allow_manual_role_assignment = true

# Default role for new workers if not auto-assigned
default_role = "GENERALIST"

# Role distribution preference (auto-assignment)
# Format: role_name = percentage (total should equal 100)
role_distribution = [
    {role = "MINER", weight = 25},
    {role = "BUILDER", weight = 25},
    {role = "GUARD", weight = 20},
    {role = "CRAFTER", weight = 20},
    {role = "GENERALIST", weight = 10}
]

[Nicknames]
# Enable nickname assignment
enabled = true

# Allow players to manually assign nicknames
allow_manual_nicknames = true

# Nickname assignment mode: "AUTO", "MANUAL", or "OFF"
assignment_mode = "AUTO"

# Whether to ensure unique nicknames
require_unique = true

[ContractNetProtocol]
# Enable Contract Net Protocol for task assignment
enabled = true

# Bidding deadline in seconds
bidding_deadline_seconds = 30

# Minimum capability score to submit a bid
min_bid_capability = 0.3

# Fallback to round-robin if no bids received
fallback_to_round_robin = true

# Logging verbosity for CNP: "MINIMAL", "NORMAL", "VERBOSE"
logging_level = "NORMAL"

[Efficiency]
# Efficiency multipliers for role-action combinations
# Format: role.action = multiplier (1.0 = baseline, 2.0 = 2x speed)

# Miner efficiencies
[Efficiency.MINER]
mine = 1.5
gather = 1.3
explore_caves = 1.5

# Builder efficiencies
[Efficiency.BUILDER]
build = 1.4
place = 1.4
construct = 1.3

# Guard efficiencies
[Efficiency.GUARD]
attack = 1.6
defend = 1.5
patrol = 1.3

# Crafter efficiencies
[Efficiency.CRAFTER]
craft = 2.0
smelt = 1.5
enchant = 1.3
```

---

## Commands

### New Commands

```
# Assign a role to a worker
/crew role <name> <role>
  Example: /crew role Sparks MINER

# Assign a manual nickname
/crew nickname <name> <nickname>
  Example: /crew nickname Steve_001 "Sparks"

# View worker role and capabilities
/crew inspect <name>
  Example: /crew inspect Sparks

# List all workers by role
/crew list [role]
  Example: /crew list MINER

# Set worker role distribution
/crew distribution <role> <percentage>
  Example: /crew distribution MINER 30

# Toggle Contract Net Protocol
/foreman cnp <on|off>
  Example: /foreman cnp on
```

---

## Example Scenarios

### Scenario 1: Mining Task with CNP

```
1. Player: "Mine 64 iron ore"

2. Foreman (Mace): "Request logged. Initiating worker coordination."

3. [CNP] Foreman announces task to all workers:
   - Task: mine iron ore
   - Required capabilities: mining
   - Bidding deadline: 30 seconds

4. Workers evaluate and bid:
   - Sparks (MINER): capability_score=1.5, estimated_time=1200, confidence=0.9
   - Dusty (MINER): capability_score=1.5, estimated_time=1400, confidence=0.8
   - Chalk (BUILDER): capability_score=0.4 (below threshold, no bid)

5. Foreman evaluates bids:
   - Sparks: 150 + 38 + 18 = 206 points
   - Dusty: 150 + 36 + 16 = 202 points

6. Foreman awards task to Sparks.

7. Sparks accepts: "On it! Iron ore coming up."

8. Dusty receives rejection: "Task awarded to another worker."

9. Sparks completes task and reports: "Got all 64 iron. Job done."
```

### Scenario 2: Mixed Task Distribution

```
1. Player: "Build a house and clear the area"

2. Foreman decomposes into tasks:
   - Task 1: Mine 64 cobblestone
   - Task 2: Place 200 oak planks
   - Task 3: Craft 10 torches

3. CNP announcements for all three tasks.

4. Workers bid:
   - Sparks (MINER): bids on Task 1 (score: 180)
   - Draft (BUILDER): bids on Task 2 (score: 190)
   - Professor (CRAFTER): bids on Task 3 (score: 175)

5. Tasks awarded to best bidders:
   - Task 1 → Sparks (MINER)
   - Task 2 → Draft (BUILDER)
   - Task 3 → Professor (CRAFTER)

6. Mace reports: "I've got Sparks on materials, Draft on construction, and Professor on torches. ETA: 4 minutes."
```

---

## Brand Consistency

### Mace MineWright's Dialogue Style

All worker coordination maintains Mace's professional contractor personality:

**Task Announcement:**
> "Request logged. Initiating worker coordination."

**Awarding Task:**
> "Sparks, you're on mining detail. Don't let me down."
> "Draft, I need you on the east wing. Precision matters."

**Task Complete:**
> "Sparks came through. Put it in the file."
> "Draft delivered. Structure's up. Not pretty, but it'll hold."

**No Qualified Workers:**
> "We've got a... variance in the crew composition. Handling manually."

**CNP Status:**
> "Bidding open. Thirty seconds. Don't make me wait."

---

## Conclusion

This Worker Role System transforms the generic worker model into a strategic, capability-based coordination system while maintaining the professional, results-oriented brand identity of Mace MineWright. The Contract Net Protocol ensures tasks are assigned to the most qualified workers, nicknames provide emotional attachment, and role specialization adds strategic depth to crew management.

**Key Benefits:**

1. **Capability Matching:** Tasks go to workers who can actually do them well
2. **Strategic Crew Building:** Players consider role composition when hiring
3. **Emotional Attachment:** Nicknames make crew members memorable
4. **Brand Consistency:** Mace's personality shines through coordination
5. **Scalability:** CNP handles large crews efficiently
6. **Flexibility:** Falls back to round-robin if CNP fails

**Next Steps:**

1. Implement Phase 1 (Core Role System)
2. Test nickname assignment
3. Develop CNP message handling
4. Balance efficiency multipliers
5. Write role-specific dialogue
6. Performance testing

---

**Document Version:** 1.0
**Author:** MineWright Development Team
**Status:** Research Complete - Ready for Implementation
**Last Updated:** 2026-02-27
