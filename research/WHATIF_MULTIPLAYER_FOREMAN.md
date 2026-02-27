# What-If Analysis: Multiple Players, One Foreman

## Executive Summary

This document explores the architectural challenges and proposed solutions for handling multiple human players interacting with a single foreman AI in the MineWright system. The current architecture assumes a single-player relationship model, which breaks down in multiplayer scenarios.

**Current State:** The `CompanionMemory` system maintains a one-to-one relationship between the foreman and a single player, tracking rapport, trust, preferences, and shared experiences.

**Problem:** When multiple players exist, the foreman cannot distinguish between players, leading to ambiguous context ("we"), conflicting commands, and compromised relationship dynamics.

---

## Problem Analysis

### 1. Relationship Management

**Current Implementation:**
```java
// CompanionMemory.java (lines 119-126)
private String playerName;
private final AtomicInteger rapportLevel;
private final AtomicInteger trustLevel;
private final Map<String, Object> playerPreferences;
private final Map<String, Integer> playstyleMetrics;
```

**Issue:** Single `playerName` field stores only one player's identity. The Foreman has no concept of multiple simultaneous relationships.

**Real-World Scenario:**
- Player A (Alice) and Player B (Bob) join the server
- Foreman initializes with Alice's name (first player logged in)
- Bob gives commands, but foreman addresses responses to "Alice" or says "we" ambiguously
- Bob feels ignored; Alice gets credit for Bob's work

### 2. Conflicting Commands

**Current Implementation:**
```java
// ActionExecutor.java (lines 113-158)
public void processNaturalLanguageCommand(String command) {
    // Commands processed FIFO, no prioritization
    if (isPlanning) {
        MineWrightMod.LOGGER.warn("MineWright '{}' is already planning, ignoring command: {}", ...);
        sendToGUI(minewright.getMineWrightName(), "Hold on, I'm still thinking...");
        return;
    }
    // ... processes command immediately
}
```

**Issue:** No command source tracking or conflict resolution.

**Real-World Scenario:**
- Alice: "Build a house"
- Foreman: "Okay! Building a house"
- Bob: "Mine diamonds" (1 second later)
- Foreman: Abandons house building, starts mining
- Alice's goal is discarded without acknowledgment

### 3. Ambiguous Conversation Context

**Current Implementation:**
```java
// CompanionMemory.java (lines 476-504)
public String getRelationshipContext() {
    StringBuilder sb = new StringBuilder();
    sb.append("Relationship Status:\n");
    sb.append("- Rapport Level: ").append(getRapportLevel()).append("/100\n");
    sb.append("- Trust Level: ").append(getTrustLevel()).append("/100\n");
    // ... no player disambiguation
}
```

**Issue:** Foreman uses pronouns "we", "you", "us" without clear referent.

**Real-World Scenario:**
```
Foreman: "We've built 50 houses together!"
Alice: (confused) "I've never built with you..."
Bob: "Oh, he means me. We built those last week."
```

### 4. Memory Isolation

**Current Implementation:**
```java
// CompanionMemory.java (lines 74-94)
private final Deque<EpisodicMemory> episodicMemories;
private final Map<String, SemanticMemory> semanticMemories;
private final List<EmotionalMemory> emotionalMemories;
// All memories stored in single collections, no player tagging
```

**Issue:** Memories lack player attribution. Shared experiences cannot be traced to specific players.

**Real-World Scenario:**
- Foreman: "Remember that time we died in lava?"
- Alice: "I've never died in lava..."
- Bob: "Oh yeah, that was me! You weren't there, Alice."
- Result: Inside jokes and shared moments lose meaning

### 5. Preference Conflicts

**Current Implementation:**
```java
// CompanionMemory.java (lines 124-126)
private final Map<String, Object> playerPreferences;
// Simple key-value, no per-player tracking
```

**Issue:** Cannot reconcile opposing preferences between players.

**Real-World Scenario:**
- Alice prefers: aggressive expansion, quick builds, cobblestone
- Bob prefers: careful planning, detailed builds, stone bricks
- Foreman: Uses cobblestone (Alice's pref), Bob is annoyed
- Foreman: Builds slowly (Bob's style), Alice gets frustrated

### 6. Rapport Dynamics

**Current Implementation:**
```java
// CompanionMemory.java (lines 548-551)
public void adjustRapport(int delta) {
    int newValue = Math.max(0, Math.min(100, rapportLevel.get() + delta));
    rapportLevel.set(newValue);
}
```

**Issue:** Single rapport score cannot reflect multi-player dynamics.

**Real-World Scenarios:**
- Alice plays daily, high rapport (90/100)
- Bob plays weekly, low rapport (30/100)
- Both give conflicting commands
- Question: Whose rapport should weigh more?

---

## Proposed Solutions

### Solution 1: Per-Player Memory (Multi-Relationship Model)

**Concept:** Maintain separate `CompanionMemory` instances for each player, with a shared "world knowledge" base.

#### Architecture

```java
public class MultiPlayerCompanionMemory {
    // Shared across all players
    private final WorldKnowledge worldKnowledge;
    private final PersonalityProfile foremanPersonality;

    // Per-player isolated memories
    private final Map<UUID, PlayerRelationship> relationships;

    public class PlayerRelationship {
        private UUID playerId;
        private String playerName;
        private CompanionMemory personalMemory;  // Isolated

        // Relationship metrics
        private int rapportLevel;
        private int trustLevel;
        private int interactionCount;
        private Instant firstMeeting;

        // Command priority (based on rapport/time)
        private double getCommandWeight() {
            double rapportFactor = rapportLevel / 100.0;
            double recencyFactor = calculateRecencyFactor();
            return rapportFactor * 0.7 + recencyFactor * 0.3;
        }
    }
}
```

#### Command Processing Flow

```java
public void processCommand(UUID playerId, String command) {
    PlayerRelationship relationship = relationships.get(playerId);

    // Check if higher-rapport player has priority
    if (hasConflictingCommandFromHigherRapport(relationship)) {
        sendResponse(playerId, "Hold on, I'm helping " +
            getCurrentFocusPlayer().getName() + " first.");
        queueCommand(playerId, command);  // Queue for later
        return;
    }

    // Process with player-specific context
    CompanionMemory memory = relationship.getPersonalMemory();
    String enrichedPrompt = buildPromptWithPlayerContext(command, memory);
    // ... execute
}
```

#### Pros
- **Clear attribution:** Every memory, preference, and joke is tied to a specific player
- **Fair conflict resolution:** Higher rapport players get priority (earned privilege)
- **Rich personalization:** Each player gets unique inside jokes, shared history
- **Privacy:** Players cannot see each other's relationship data

#### Cons
- **Memory overhead:** N players × full memory footprint
- **Complexity:** Significant refactoring of `CompanionMemory`
- **Identity confusion:** Foreman may reference experiences the current player doesn't share
- **Fragmentation:** World knowledge (what exists where) duplicated or needs shared layer

#### Implementation Sketch

```java
// ForemanEntity.java
private MultiPlayerCompanionMemory multiPlayerMemory;

public void processNaturalLanguageCommand(UUID playerId, String command) {
    PlayerRelationship relationship = multiPlayerMemory.getRelationship(playerId);

    // Switch foreman's "focus" to this player
    multiPlayerMemory.setCurrentFocus(playerId);

    // Get player-specific prompt context
    String context = relationship.buildContextString();

    // Include relationship context in LLM prompt
    String enrichedPrompt = String.format("""
        %s

        Current conversation is with: %s (rapport: %d/100, trust: %d/100)
        Known preferences: %s
        Shared inside jokes: %s

        Player command: %s
        """,
        PromptBuilder.buildSystemPrompt(),
        relationship.getPlayerName(),
        relationship.getRapportLevel(),
        relationship.getTrustLevel(),
        relationship.getPreferences(),
        relationship.getInsideJokes(),
        command
    );

    taskPlanner.planTasksAsync(this, enrichedPrompt);
}

// Response generation
public void sendChatMessage(String message, UUID targetPlayerId) {
    PlayerRelationship relationship = multiPlayerMemory.getRelationship(targetPlayerId);

    // Disambiguate pronouns based on context
    String personalizedMessage = relationship.disambiguatePronouns(message);

    // Send only to target player (or all if broadcast)
    if (isBroadcast) {
        level().players().forEach(p -> p.sendSystemMessage(personalizedMessage));
    } else {
        Player targetPlayer = level().getPlayerByUUID(targetPlayerId);
        targetPlayer.sendSystemMessage(personalizedMessage);
    }
}
```

---

### Solution 2: Democratic Foreman (Team Identity Model)

**Concept:** Foreman maintains relationship with "the team" as a collective entity, using voting/conflict resolution for commands.

#### Architecture

```java
public class DemocraticForemanMemory {
    // Team-level relationship
    private TeamIdentity teamIdentity;

    // Individual player stats (for weighted voting)
    private final Map<UUID, PlayerStats> playerStats;

    public class TeamIdentity {
        private String teamName;
        private int teamRapport;
        private int teamTrust;

        // Collective preferences (democratic average)
        private Map<String, Object> consensusPreferences;

        // Shared experiences (attributed to team)
        private List<TeamMemory> sharedMemories;
    }

    public class PlayerStats {
        private UUID playerId;
        private String playerName;

        // Voting weight based on contribution
        private double votingWeight;

        // Individual rapport (for messaging personalization)
        private int personalRapport;
    }

    public class TeamMemory {
        private String description;
        private Set<UUID> participatingPlayers;  // Who was there
        private int emotionalWeight;
    }
}
```

#### Command Resolution

```java
public void processCommand(UUID playerId, String command) {
    if (isConflictingCommand(command)) {
        // Trigger voting
        resolveConflictDemocratically(command, playerId);
    } else {
        // Build consensus
        executeWithTeamConsensus(command);
    }
}

private void resolveConflictDemocratically(String newCommand, UUID proposerId) {
    // Get all active players
    List<ServerPlayer> activePlayers = getActivePlayers();

    // Present options
    String conflictMessage = String.format("""
        Conflicting commands!
        Current task: %s (from %s)
        New request: %s (from %s)

        Vote now: /foreman vote [1|2]
        """,
        currentTask, currentTaskProposer,
        newCommand, getPlayerName(proposerId)
    );

    broadcastMessage(conflictMessage);

    // Weight votes by rapport + recent contribution
    startVotingPeriod(20 * 5);  // 5 seconds
}
```

#### Pros
- **Social dynamics:** Encourages collaboration, negotiation among players
- **Fairness:** Democratic process prevents command spamming
- **Team identity:** Creates shared "we" narrative (foreman + team)
- **Simpler memory:** Single shared memory pool (less overhead)

#### Cons
- **Voting overhead:** Stops gameplay for conflict resolution
- **Tyranny of majority:** Minority players constantly outvoted
- **Slower response:** Consensus building takes time
- **Complex UX:** Players must learn voting system

#### Implementation Sketch

```java
// TeamMemory.java
public class TeamMemory {
    private final List<SharedExperience> sharedExperiences;
    private final TeamConsensus consensus;

    public String buildTeamContext() {
        return String.format("""
            Team: %s
            Members: %s
            Team Rapport: %d/100
            Collective Preferences: %s

            Recent Team Accomplishments:
            %s

            Current Task: %s (agreed by %s)
            """,
            teamIdentity.getName(),
            getActivePlayerNames(),
            teamIdentity.getTeamRapport(),
            teamIdentity.getConsensusPreferences(),
            getRecentAccomplishments(),
            currentTask,
            currentTaskProposer
        );
    }
}

// Prompt generation with team context
String enrichedPrompt = String.format("""
    %s

    You are coordinating with a TEAM of players.
    %s

    When referring to players:
    - Use "the team" or "everyone" for group actions
    - Use specific names for individual player contributions
    - Acknowledge team consensus in your responses

    Team command: %s
    """,
    PromptBuilder.buildSystemPrompt(),
    teamMemory.buildTeamContext(),
    command
);
```

---

### Solution 3: Context Switching (Active Player Model)

**Concept:** Foreman has one "active conversation" at a time, with explicit session management and context switching.

#### Architecture

```java
public class ContextSwitchingMemory {
    // All player relationships
    private final Map<UUID, PlayerSession> playerSessions;

    // Current active conversation
    private UUID activePlayerId;
    private Instant lastContextSwitch;

    // Session timeout (30 seconds of inactivity)
    private static final long SESSION_TIMEOUT_MS = 30_000;

    public class PlayerSession {
        private UUID playerId;
        private CompanionMemory memory;

        private Instant lastInteraction;
        private boolean isActive;

        // Conversation state
        private List<String> pendingResponses;
        private String currentTopic;
    }

    public UUID switchContext(UUID requestingPlayerId) {
        PlayerSession currentSession = playerSessions.get(activePlayerId);
        PlayerSession requestingSession = playerSessions.get(requestingPlayerId);

        // Check if current session is still active
        if (currentSession != null &&
            Duration.between(currentSession.getLastInteraction(), Instant.now()).toMillis() < SESSION_TIMEOUT_MS) {

            // Ask for permission to interrupt
            sendInterruptRequest(activePlayerId, requestingPlayerId);
            return null;  // Waiting for permission
        }

        // Switch context
        UUID previousPlayer = activePlayerId;
        activePlayerId = requestingPlayerId;
        requestingSession.setActive(true);

        if (currentSession != null) {
            currentSession.setActive(false);
        }

        broadcastContextSwitch(previousPlayer, requestingPlayerId);
        return requestingPlayerId;
    }
}
```

#### Context Switching Flow

```java
public void processCommand(UUID playerId, String command) {
    if (!playerId.equals(activePlayerId)) {
        // Attempt to switch context
        UUID newActive = contextMemory.switchContext(playerId);

        if (newActive == null) {
            // Waiting for current player to yield
            sendQueuedMessage(playerId, "One moment, finishing up with " +
                getPlayerName(activePlayerId));
            queueCommand(playerId, command);
            return;
        }
    }

    // Process with full attention
    PlayerSession session = contextMemory.getSession(playerId);
    session.updateLastInteraction();

    String contextPrompt = String.format("""
        %s

        ACTIVE CONVERSATION WITH: %s
        Shared history: %s
        Inside jokes: %s
        Current rapport: %d/100

        FOCUSED MODE: You are giving full attention to %s.
        Refer to "we" as you and %s specifically.
        """,
        PromptBuilder.buildSystemPrompt(),
        session.getPlayerName(),
        session.getSharedHistory(),
        session.getInsideJokes(),
        session.getRapport(),
        session.getPlayerName(),
        session.getPlayerName()
    );

    taskPlanner.planTasksAsync(this, contextPrompt + "\n\n" + command);
}
```

#### Pros
- **Clear focus:** Foreman never "we"s ambiguously
- **Personal connection:** Each player gets full foreman attention
- **Simple model:** Existing `CompanionMemory` mostly unchanged
- **Polite interruption:** Explicit handoff prevents confusion

#### Cons
- **Sequential access:** Only one player can command at a time
- **Queue frustration:** Players wait for their turn
- **Context loss:** Switching costs (forgetting previous context)
- **Solo experience:** Doesn't leverage multiplayer potential

#### Implementation Sketch

```java
// ForemanEntity.java with context switching
private ContextSwitchingMemory contextMemory;

public void tick() {
    super.tick();

    // Check for session timeout
    UUID timedOutPlayer = contextMemory.checkTimeout();
    if (timedOutPlayer != null) {
        sendChatMessage("Session with " + getPlayerName(timedOutPlayer) + " timed out.");
        contextMemory.releaseSession(timedOutPlayer);
    }

    // Process queued commands for next player
    if (!isExecuting() && !isPlanning()) {
        QueuedCommand next = contextMemory.getNextQueuedCommand();
        if (next != null) {
            processCommand(next.getPlayerId(), next.getCommand());
        }
    }
}

// Player-facing command: /foreman attention
public void requestAttention(UUID playerId) {
    UUID currentActive = contextMemory.getActivePlayer();

    if (currentActive == null) {
        // No active player, grant immediately
        contextMemory.switchContext(playerId);
        sendChatMessage(getPlayerName(playerId) + " has my attention now!");
    } else if (currentActive.equals(playerId)) {
        sendChatMessage("You already have my attention!");
    } else {
        // Request interrupt
        PlayerSession currentSession = contextMemory.getSession(currentActive);
        if (currentSession != null) {
            sendPrivateMessage(currentActive,
                getPlayerName(playerId) + " is requesting my attention. Yield? /foreman yield");
            sendPrivateMessage(playerId,
                "Requesting attention from " + getPlayerName(currentActive) + "...");
        }
    }
}
```

---

## Recommended Approach: Hybrid Multi-Relationship

**Recommendation:** Combine **Solution 1 (Per-Player Memory)** with elements of **Solution 3 (Context Switching)**.

### Rationale

1. **Per-Player Memory Foundation** (from Solution 1)
   - Each player gets isolated `CompanionMemory` instance
   - Clear attribution of experiences, preferences, jokes
   - Fair rapport-based prioritization

2. **Soft Context Switching** (modified from Solution 3)
   - Instead of exclusive locking, use "focus weighting"
   - Foreman can process multiple commands but acknowledges primary focus
   - Dynamic focus based on:
     - Most recent interaction
     - Highest rapport
     - Current task relevance

3. **Team Awareness** (lightweight from Solution 2)
   - Foreman knows about all players on server
   - Can reference "the team" when appropriate
   - Shared accomplishments credited to all participants

### Implementation Architecture

```java
public class HybridMultiPlayerMemory {
    // Core data structures
    private final Map<UUID, PlayerRelationship> relationships;
    private final WorldKnowledge sharedWorldKnowledge;

    // Dynamic focus management
    private FocusManager focusManager;

    public class FocusManager {
        // Weighted priority for each player
        private final Map<UUID, Double> focusWeights;

        public UUID getPrimaryFocus() {
            return focusWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }

        public void updateWeight(UUID playerId, double delta) {
            double currentWeight = focusWeights.getOrDefault(playerId, 0.0);
            double newWeight = clamp(0.0, currentWeight + delta, 100.0);

            focusWeights.put(playerId, newWeight);

            // Decay all weights slightly (recent interactions matter more)
            decayAllWeights(0.99);
        }

        public double getFocusWeight(UUID playerId) {
            return focusWeights.getOrDefault(playerId, 0.0);
        }
    }

    public class PlayerRelationship {
        private UUID playerId;
        private String playerName;

        // Isolated memory
        private final CompanionMemory personalMemory;

        // Public profile (visible to other players)
        private final PublicProfile publicProfile;

        public class PublicProfile {
            private int rapportLevel;  // Visible: "Alice is trusted (90/100)"
            private int totalContributions;  // Visible: "Alice has built 500 blocks"
            private String specializations;  // Visible: "Alice prefers building"
        }
    }
}
```

### Command Processing Flow

```java
public void processNaturalLanguageCommand(UUID playerId, String command) {
    PlayerRelationship relationship = relationships.get(playerId);

    // Update focus weight (interaction boost)
    focusManager.updateWeight(playerId, 20.0);

    UUID primaryFocus = focusManager.getPrimaryFocus();

    // Build enriched prompt
    String enrichedPrompt = buildMultiPlayerPrompt(command, playerId, primaryFocus);

    // Process command
    taskPlanner.planTasksAsync(this, enrichedPrompt);
}

private String buildMultiPlayerPrompt(String command, UUID requesterId, UUID focusId) {
    PlayerRelationship requester = relationships.get(requesterId);
    boolean isPrimaryFocus = requesterId.equals(focusId);

    StringBuilder prompt = new StringBuilder();
    prompt.append(PromptBuilder.buildSystemPrompt()).append("\n\n");

    // Multi-player context
    prompt.append("MULTI-PLAYER CONTEXT:\n");
    prompt.append("All players on server: ").append(getAllPlayerNames()).append("\n");
    prompt.append("Primary focus: ").append(getPlayerName(focusId)).append("\n");
    prompt.append("Command requester: ").append(requester.getPlayerName()).append("\n");

    if (isPrimaryFocus) {
        prompt.append("\nSTATUS: You are giving PRIMARY ATTENTION to ")
              .append(requester.getPlayerName()).append(".\n");
        prompt.append("Use 'we' to mean you and ").append(requester.getPlayerName()).append(".\n");
    } else {
        prompt.append("\nSTATUS: You are multitasking. ")
              .append(getPlayerName(focusId)).append(" has primary focus.\n");
        prompt.append("Acknowledge ").append(requester.getPlayerName())
              .append("'s request but maintain current priority.\n");
    }

    // Relationship-specific context
    prompt.append("\nRELATIONSHIP WITH ").append(requester.getPlayerName().toUpperCase()).append(":\n");
    prompt.append(relationship.getPersonalMemory().getRelationshipContext());
    prompt.append("Shared inside jokes: ").append(relationship.getInsideJokes()).append("\n");
    prompt.append("Known preferences: ").append(relationship.getPreferences()).append("\n");

    // Command
    prompt.append("\nCOMMAND: ").append(command);

    return prompt.toString();
}
```

### Response Generation

```java
public void sendChatMessage(String message, UUID targetPlayerId) {
    PlayerRelationship relationship = relationships.get(targetPlayerId);

    // Disambiguate pronouns
    String personalizedMessage = disambiguateMessage(message, targetPlayerId);

    // Add relationship-based flavor
    if (relationship.getRapport() > 80) {
        personalizedMessage = addCasualFlavor(personalizedMessage);
    } else if (relationship.getRapport() < 30) {
        personalizedMessage = addFormalFlavor(personalizedMessage);
    }

    // Send to specific player (not broadcast)
    ServerPlayer targetPlayer = level().getPlayerByUUID(targetPlayerId);
    if (targetPlayer != null) {
        targetPlayer.sendSystemMessage(Component.literal(personalizedMessage));
    }
}

private String disambiguateMessage(String message, UUID targetPlayerId) {
    UUID primaryFocus = focusManager.getPrimaryFocus();
    String playerName = relationships.get(targetPlayerId).getPlayerName();

    // Replace ambiguous pronouns
    String disambiguated = message;

    if (targetPlayerId.equals(primaryFocus)) {
        disambiguated = disambiguated.replace(" we ", " you and I, " + playerName + ", ");
        disambiguated = disambiguated.replace(" us ", " you and me ");
    } else {
        disambiguated = disambiguated.replace(" we ", " the team and ");
        disambiguated = disambiguated.replace(" us ", " everyone ");
    }

    return disambiguated;
}
```

### Conflict Resolution

```java
public void handleConflictingCommand(UUID newPlayerId, String newCommand) {
    UUID currentPlayerId = currentTaskProposer;
    PlayerRelationship currentRel = relationships.get(currentPlayerId);
    PlayerRelationship newRel = relationships.get(newPlayerId);

    double currentWeight = focusManager.getFocusWeight(currentPlayerId);
    double newWeight = focusManager.getFocusWeight(newPlayerId);

    // Significant weight difference? Priority decision
    if (Math.abs(currentWeight - newWeight) > 30.0) {
        UUID priorityPlayer = (newWeight > currentWeight) ? newPlayerId : currentPlayerId;

        if (priorityPlayer.equals(newPlayerId)) {
            // New player preempts
            preemptCurrentTask(newPlayerId, newCommand, currentPlayerId);
        } else {
            // Current player maintains priority
            queueTask(newPlayerId, newCommand);
            sendPrivateMessage(newPlayerId,
                "I'm helping " + getPlayerName(currentPlayerId) + " right now. I'll get to you next!");
        }
    } else {
        // Similar weight: ask for team decision
        initiateTeamVote(newPlayerId, newCommand, currentPlayerId);
    }
}

private void preemptCurrentTask(UUID priorityPlayerId, String newCommand, UUID interruptedPlayerId) {
    sendPrivateMessage(interruptedPlayerId,
        "Sorry, " + getPlayerName(priorityPlayerId) + " needs my attention urgently. Queuing your task.");
    queueTask(interruptedPlayerId, currentTask);

    // Switch to priority player
    currentTaskProposer = priorityPlayerId;
    focusManager.updateWeight(priorityPlayerId, 50.0);  // Boost for interruption
    processCommand(priorityPlayerId, newCommand);
}
```

---

## Migration Path

### Phase 1: Foundation (Week 1-2)
- [ ] Create `MultiPlayerCompanionMemory` class
- [ ] Add UUID tracking to `CompanionMemory`
- [ ] Implement per-player memory isolation
- [ ] Update `ForemanEntity` to use new memory system

### Phase 2: Focus Management (Week 3)
- [ ] Implement `FocusManager`
- [ ] Add weight-based prioritization
- [ ] Update prompt building for multi-player context
- [ ] Add player-specific message routing

### Phase 3: Conflict Resolution (Week 4)
- [ ] Implement command queue per player
- [ ] Add conflict detection
- [ ] Implement priority-based preemption
- [ ] Add team voting UI

### Phase 4: Polish (Week 5)
- [ ] Add `/foreman` commands for players (attention, status, yield)
- [ ] Implement player profile visibility
- [ ] Add team statistics display
- [ ] Performance optimization (caching, memory limits)

---

## Code Examples

### Example 1: Player Joins Server

```java
@SubscribeEvent
public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
        UUID playerId = player.getUUID();
        String playerName = player.getName().getString();

        // Get foreman
        ForemanEntity foreman = MineWrightMod.getMineWrightManager().getForeman();
        if (foreman != null) {
            MultiPlayerCompanionMemory memory = foreman.getMultiPlayerMemory();

            // Create new relationship
            if (!memory.hasRelationship(playerId)) {
                memory.initializeRelationship(playerId, playerName);

                // Greeting based on existing players
                if (memory.getPlayerCount() == 1) {
                    foreman.sendChatMessage("Hello " + playerName + "! I'm the foreman. Let's build great things!");
                } else {
                    foreman.sendChatMessage("Welcome " + playerName + "! " +
                        memory.getPlayerNames() + " are already here. Ready to work together?");
                }
            } else {
                // Returning player
                PlayerRelationship rel = memory.getRelationship(playerId);
                foreman.sendChatMessage("Good to see you again, " + playerName +
                    "! Rapport: " + rel.getRapport() + "/100");
            }
        }
    }
}
```

### Example 2: Two Players Give Conflicting Commands

```
[Alice]: /minewright build a house
[Foreman]: <to Alice> On it, Alice! We've built 47 structures together, let's make it 48.
           Starting to plan...
[Foreman]: <to Alice> Okay! Building a 10x10 cobblestone house with oak door.

[Bob]: /minewright mine diamonds
[Foreman]: <to Bob> Hey Bob! I'm currently helping Alice build a house.
           Your rapport is 45/100, Alice's is 82/100.
           I'll finish this task first, then start mining. Queued for later.

[Foreman]: <to Bob> I've added "mine diamonds" to your queue. Position: #1

[... house building completes ...]

[Foreman]: <to Alice> House complete! That's 48 structures for us, Alice.
[Foreman]: <to Bob> Starting your diamond mining task now, Bob!
```

### Example 3: Inside Joke Attribution

```java
// During conversation
[Alice]: /minewright remember that time with the lava?
[Foreman]: <to Alice> Haha! Yes, Alice! That was 3 days ago.
           You: "Lava is just hot orange water!" *splash* *sizzle*
           We lost 32 diamonds but laughed for 5 minutes. Classic! :)

[Bob]: /minewright remember that time with the lava?
[Foreman]: <to Bob> Um, Bob... I don't remember any lava experiences with you specifically.
           Want to go cause some chaos together? ;)
```

---

## Testing Scenarios

### Scenario 1: Sequential Player Entry
1. Alice joins → foreman initializes with Alice
2. Alice builds rapport to 50/100
3. Bob joins → foreman welcomes Bob, creates separate relationship
4. Bob gives command → foreman acknowledges Bob individually
5. Alice gives command → foreman maintains separate context

**Expected:** Separate memories, no cross-contamination

### Scenario 2: Conflicting Commands
1. Alice (rapport 90) commands: "build house"
2. Foreman starts building
3. Bob (rapport 30) commands: "mine diamonds" after 2 seconds
4. Foreman: politely queues Bob's task, continues Alice's

**Expected:** Alice prioritized due to higher rapport

### Scenario 3: Team Vote
1. Alice (rapport 50) commands: "build castle"
2. Bob (rapport 55) commands: "build farm" (similar weight)
3. Foreman detects conflict, initiates vote
4. Charlie joins, votes for castle
5. Foreman executes castle, credits Alice + Charlie

**Expected:** Democratic resolution with credit attribution

### Scenario 4: Memory Isolation
1. Alice & Foreman: die in lava, share traumatic experience
2. Foreman records emotional memory with Alice
3. Bob joins, asks "remember our close calls?"
4. Foreman: "I don't have any shared memories with you yet, Bob"
5. Alice joins, asks same → Foreman shares lava story

**Expected:** Clear memory attribution per player

---

## Performance Considerations

### Memory Overhead
- **Per-player memory:** ~50KB per player (200 episodic + 100 semantic memories)
- **10 players:** ~500KB overhead (acceptable)
- **100 players:** ~5MB (consider memory limits)

**Mitigation:**
```java
// Lazy loading: only load active players' memories
public class LazyPlayerRelationship {
    private UUID playerId;
    private transient CompanionMemory memory;  // Loaded on demand

    public CompanionMemory getMemory() {
        if (memory == null) {
            memory = loadFromDisk(playerId);
        }
        return memory;
    }
}
```

### Focus Weight Calculation
- **O(N)** where N = active players
- **Optimization:** Only recalculate when interaction occurs
- **Caching:** Cache sorted player list, invalidate on interaction

### Prompt Building
- **Current overhead:** +200 tokens per command for multi-player context
- **Mitigation:** Compress player list (e.g., "Alice(90), Bob(45), Charlie(30)")
- **Caching:** Cache relationship context string, refresh every 60 seconds

---

## Conclusion

The **Hybrid Multi-Relationship** approach provides the best balance of:

1. **Rich personalization** (per-player memory)
2. **Fair conflict resolution** (rapport-based priority)
3. **Team awareness** (lightweight collective identity)
4. **Performance** (lazy loading, caching)

This architecture enables the foreman to maintain meaningful relationships with multiple players simultaneously while preserving the companion AI's core value: building unique, evolving partnerships with each human player.

**Next Steps:**
1. Prototype `MultiPlayerCompanionMemory` with 2-3 players
2. Test conflict resolution scenarios
3. Gather user feedback on prioritization fairness
4. Iterate on focus weight algorithm
5. Scale testing with 10+ players
