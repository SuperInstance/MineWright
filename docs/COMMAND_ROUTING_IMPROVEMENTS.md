# Command Routing Improvements for MineWright

**Project:** MineWright Minecraft Mod
**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Status:** Research & Analysis

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current Command Flow Analysis](#current-command-flow-analysis)
3. [Architecture Assessment](#architecture-assessment)
4. [Proposed Routing Improvements](#proposed-routing-improvements)
5. [Multi-Target Command Routing](#multi-target-command-routing)
6. [Command Aliases and Shortcuts](#command-aliases-and-shortcuts)
7. [Command History and Undo](#command-history-and-undo)
8. [Context-Aware Completion](#context-aware-completion)
9. [Batch Command Processing](#batch-command-processing)
10. [GUI Improvements](#gui-improvements)
11. [Implementation Roadmap](#implementation-roadmap)
12. [Code Examples](#code-examples)

---

## Executive Summary

MineWright currently implements a basic command routing system through `ForemanOfficeGUI` that processes natural language commands via `ActionExecutor.processNaturalLanguageCommand()`. While functional, the system has opportunities for improvement in multi-target routing, command history, aliases, and context-aware completion.

**Key Findings:**
- Basic multi-target parsing exists but is limited (comma-separated names)
- Command history is implemented but lacks persistence and undo
- No command alias system or shortcuts
- GUI lacks context-aware completion suggestions
- Batch processing is ad-hoc rather than structured
- No command validation or preview before execution

---

## Current Command Flow Analysis

### 1. User Input Flow

```
User presses K
    ↓
ForemanOfficeGUI.toggle()
    ↓
ForemanOverlayScreen created (input capture)
    ↓
User types command → EditBox
    ↓
User presses Enter → sendCommand()
    ↓
parseTargetCrew() extracts targets
    ↓
mc.player.connection.sendCommand("minewright tell <name> <command>")
    ↓
ForemanCommands.tellSteve() executed
    ↓
ActionExecutor.processNaturalLanguageCommand()
    ↓
TaskPlanner.planTasksAsync() (non-blocking LLM call)
    ↓
ResponseParser.parseAIResponse()
    ↓
Tasks queued in ActionExecutor.taskQueue
    ↓
BaseAction.tick() executes tasks
```

### 2. Current Multi-Target Parsing

**Location:** `ForemanOfficeGUI.parseTargetCrew()` (lines 427-462)

**Supported Patterns:**
- `all crew <command>` - Targets all crew members
- `all <command>` - Targets all crew members
- `everyone <command>` - Targets all crew members
- `Alex, Bob <command>` - Targets specific crew members (comma-separated)
- `Alex <command>` - Targets single crew member

**Limitations:**
- No support for role-based targeting (e.g., "miners, build this")
- No support for proximity-based targeting (e.g., "nearest crew, follow me")
- No support for group aliases (e.g., "team alpha, attack")
- Case-sensitive matching issues (code uses toLowerCase but could be more robust)
- No fuzzy matching for crew names
- No exclusion targeting (e.g., "all except Bob")

### 3. Command History System

**Location:** `ForemanOfficeGUI` (lines 31-32, 317-335)

**Current Implementation:**
```java
private static List<String> commandHistory = new ArrayList<>();
private static int historyIndex = -1;

// Arrow up/down navigation
// History limited to 50 commands
```

**Limitations:**
- No persistence across game sessions
- No search/filter functionality
- No timestamps or metadata
- No command categorization
- No undo/redo capability
- No history export/import

### 4. Command Preprocessing

**Current State:** Minimal preprocessing in `sendCommand()` (lines 380-425)

**Special Cases:**
- `spawn <name>` - Direct spawn command
- Target parsing - Multi-target extraction
- Everything else - Passed to LLM as-is

**Missing:**
- No command validation
- No command preview
- No macro expansion
- No variable substitution
- No conditional commands

---

## Architecture Assessment

### Strengths

1. **Async Processing:** `TaskPlanner.planTasksAsync()` prevents game thread blocking
2. **Plugin Architecture:** `ActionRegistry` allows dynamic action registration
3. **State Machine:** `AgentStateMachine` tracks agent states (IDLE, PLANNING, EXECUTING)
4. **Interceptor Chain:** `LoggingInterceptor`, `MetricsInterceptor` for cross-cutting concerns
5. **Event Bus:** `AgentCommunicationBus` for inter-agent messaging

### Weaknesses

1. **Tight Coupling:** GUI directly calls sendCommand() which calls Minecraft commands
2. **No Abstraction:** Command routing logic scattered across GUI and Commands
3. **Limited Validation:** No pre-execution validation or previews
4. **No Cancellation:** Cannot cancel in-flight async LLM requests
5. **No Batching:** Commands sent individually, no structured batch processing
6. **Missing Persistence:** Command history and aliases not persisted

---

## Proposed Routing Improvements

### 1. Command Router Architecture

**New Package Structure:**
```
com.minewright.command
├── CommandRouter.java          (NEW - Central routing coordinator)
├── CommandContext.java         (NEW - Encapsulates command + context)
├── CommandTarget.java          (NEW - Represents command target)
├── CommandValidator.java       (NEW - Pre-execution validation)
├── CommandHistory.java         (NEW - Persistent history manager)
├── CommandAliasRegistry.java   (NEW - Alias system)
├── CommandBatch.java           (NEW - Batch processing)
├── CommandCompletion.java      (NEW - Context-aware completion)
├── CommandPreview.java         (NEW - Execution preview)
└── ForemanCommands.java        (EXISTING - Brigadier commands)
```

### 2. CommandRouter Design

**Purpose:** Central coordinator for all command routing, validation, and execution

**Responsibilities:**
- Parse command targets (single, multi, role-based, proximity)
- Validate commands before execution
- Manage command history and undo stack
- Resolve aliases and macros
- Provide context-aware completion suggestions
- Execute commands with proper error handling
- Support command preview and confirmation

**Interface:**
```java
public class CommandRouter {
    // Route command to specified targets
    public CompletableFuture<CommandResult> routeCommand(
        String rawCommand,
        CommandTarget target,
        CommandContext context
    );

    // Validate command without executing
    public CommandValidation validateCommand(String rawCommand);

    // Get completion suggestions
    public List<String> getCompletions(String partial, CommandContext context);

    // Undo last command
    public boolean undoLastCommand(String crewName);

    // Create command batch
    public CommandBatch createBatch();
}
```

### 3. CommandContext Design

**Purpose:** Encapsulate all contextual information for command execution

**Fields:**
```java
public class CommandContext {
    private final String playerName;
    private final BlockPos playerPosition;
    private final Collection<ForemanEntity> availableCrew;
    private final Map<String, Object> metadata;
    private final Instant timestamp;
    private final CommandPriority priority;

    // Builder pattern for construction
    public static Builder builder();
}
```

### 4. CommandTarget Design

**Purpose:** Flexible target specification for commands

**Target Types:**
```java
public enum TargetType {
    SINGLE,              // Single crew member by name
    MULTIPLE,            // Multiple specific crew members
    ALL,                 // All crew members
    ROLE,                // Crew members with specific role
    PROXIMITY,           // Nearest N crew members
    EXCEPT,              // All except specified
    GROUP                // Predefined group alias
}

public class CommandTarget {
    private final TargetType type;
    private final Set<String> names;      // For SINGLE, MULTIPLE, EXCEPT
    private final AgentRole role;         // For ROLE
    private final int proximityCount;     // For PROXIMITY
    private final String groupAlias;      // For GROUP
    private final boolean includeSelf;

    // Factory methods for common targets
    public static CommandTarget single(String name);
    public static CommandTarget multiple(String... names);
    public static CommandTarget all();
    public static CommandTarget role(AgentRole role);
    public static CommandTarget nearest(int count);
    public static CommandTarget except(String... names);
}
```

---

## Multi-Target Command Routing

### 1. Enhanced Target Parsing

**Current Implementation Analysis:**

The current `parseTargetCrew()` method in `ForemanOfficeGUI` (lines 427-462) supports:
- Comma-separated names: "Alex, Bob, mine iron"
- "all crew" or "everyone" prefix
- Case-insensitive matching

**Proposed Improvements:**

```java
public class CommandTargetParser {
    private final CrewManager crewManager;
    private final CommandAliasRegistry aliasRegistry;

    /**
     * Parses natural language command to extract targets and clean command.
     *
     * Examples:
     * - "Alex and Bob, mine iron" → targets=[Alex, Bob], command="mine iron"
     * - "miners, build a house" → targets=[role:MINER], command="build a house"
     * - "nearest 2, follow me" → targets=[nearest:2], command="follow me"
     * - "all except Alex, attack" → targets=[all except Alex], command="attack"
     *
     * @param rawCommand Raw command string from user
     * @return ParsedTarget containing targets and cleaned command
     */
    public ParsedTarget parse(String rawCommand) {
        // 1. Check for explicit target patterns
        // 2. Check for role-based targets
        // 3. Check for proximity targets
        // 4. Check for group aliases
        // 5. Check for exclusion patterns
        // 6. Check for conjunctive targets ("Alex and Bob")
        // 7. Default to single target or all
    }

    /**
     * Pattern matching for multi-target commands.
     */
    private static final Pattern TARGET_PATTERNS = Pattern.compile(
        "^(?<targets>(?:all\\s+(?:crew|everyone)|" +           // all crew, everyone
        "(?:\\w+\\s*(?:,|and)\\s*)+\\w+|" +                     // Alex, Bob and Charlie
        "(?:\\w+\\s+(?:and|&)\\s+\\w+)|" +                      // Alex and Bob
        "(?:\\w+\\s+(?:with|and)\\s+\\w+)|" +                   // Alex with Bob
        "nearest\\s+\\d+|" +                                    // nearest 3
        "except\\s+\\w+(?:\\s*,\\s*\\w+)*|" +                   // except Alex, Bob
        "\\w+)(?:\\s*,\\s*)?)\\s*,\\s*" +                       // trailing comma
        "(?<command>.*)$",
        Pattern.CASE_INSENSITIVE
    );
}
```

### 2. Target Resolution Strategy

```java
public class TargetResolver {
    /**
     * Resolves abstract target specification to concrete crew members.
     *
     * Resolution order:
     * 1. Direct name matches (exact, case-insensitive)
     * 2. Fuzzy name matches (Levenshtein distance < 3)
     * 3. Role-based resolution
     * 4. Proximity-based resolution
     * 5. Group alias resolution
     *
     * @param target CommandTarget specification
     * @param context CommandContext with available crew
     * @return Set of resolved ForemanEntity targets
     */
    public Set<ForemanEntity> resolve(CommandTarget target, CommandContext context) {
        Set<ForemanEntity> resolved = new HashSet<>();

        switch (target.getType()) {
            case SINGLE -> resolveByName(target, context, resolved);
            case MULTIPLE -> resolveByNames(target, context, resolved);
            case ALL -> resolveAll(context, resolved);
            case ROLE -> resolveByRole(target, context, resolved);
            case PROXIMITY -> resolveByProximity(target, context, resolved);
            case EXCEPT -> resolveExcluding(target, context, resolved);
            case GROUP -> resolveGroupAlias(target, context, resolved);
        }

        return resolved;
    }

    private void resolveByProximity(CommandTarget target, CommandContext context,
                                   Set<ForemanEntity> resolved) {
        BlockPos origin = context.getPlayerPosition();
        int count = target.getProximityCount();

        context.getAvailableCrew().stream()
            .filter(crew -> !target.getNames().contains(crew.getSteveName()))
            .sorted(Comparator.comparing(
                crew -> crew.blockPosition().distSqr(origin)
            ))
            .limit(count)
            .forEach(resolved::add);
    }
}
```

### 3. Multi-Target Execution Strategy

**Current Approach:** Sequential execution via `sendCommand()` loop (lines 414-424)

**Proposed Improvements:**

```java
public class MultiTargetExecutor {
    private final CommandRouter router;
    private final ExecutorService executorService;

    /**
     * Executes command across multiple targets with different strategies.
     */
    public enum ExecutionStrategy {
        SEQUENTIAL,      // Execute one at a time (current behavior)
        PARALLEL,        // Execute all simultaneously
        BATCHED,         // Execute in batches of N
        COORDINATED      // Coordinate via OrchestratorService
    }

    /**
     * Execute command on multiple targets.
     */
    public CompletableFuture<MultiTargetResult> executeMultiTarget(
        String command,
        Set<ForemanEntity> targets,
        ExecutionStrategy strategy,
        CommandContext context
    ) {
        return switch (strategy) {
            case SEQUENTIAL -> executeSequential(command, targets, context);
            case PARALLEL -> executeParallel(command, targets, context);
            case BATCHED -> executeBatched(command, targets, context, 3);
            case COORDINATED -> executeCoordinated(command, targets, context);
        };
    }

    /**
     * Sequential execution with failure handling.
     */
    private CompletableFuture<MultiTargetResult> executeSequential(
        String command,
        Set<ForemanEntity> targets,
        CommandContext context
    ) {
        List<CompletableFuture<SingleTargetResult>> futures = new ArrayList<>();
        Queue<ForemanEntity> targetQueue = new ArrayDeque<>(targets);

        CompletableFuture<MultiTargetResult> result = new CompletableFuture<>();

        executeNext(command, targetQueue, context, futures, result);
        return result;
    }

    private void executeNext(
        String command,
        Queue<ForemanEntity> remaining,
        CommandContext context,
        List<CompletableFuture<SingleTargetResult>> completed,
        CompletableFuture<MultiTargetResult> finalResult
    ) {
        if (remaining.isEmpty()) {
            // All done
            MultiTargetResult result = aggregateResults(completed);
            finalResult.complete(result);
            return;
        }

        ForemanEntity target = remaining.poll();
        router.routeCommand(command, CommandTarget.single(target.getSteveName()), context)
            .thenAccept(singleResult -> {
                completed.add(CompletableFuture.completedFuture(singleResult));
                executeNext(command, remaining, context, completed, finalResult);
            })
            .exceptionally(ex -> {
                // Log error but continue with next target
                MineWrightMod.LOGGER.error("Command failed for {}", target.getSteveName(), ex);
                executeNext(command, remaining, context, completed, finalResult);
                return null;
            });
    }

    /**
     * Coordinated execution via OrchestratorService.
     * This is the most efficient for collaborative tasks.
     */
    private CompletableFuture<MultiTargetResult> executeCoordinated(
        String command,
        Set<ForemanEntity> targets,
        CommandContext context
    ) {
        OrchestratorService orchestrator = MineWrightMod.getOrchestratorService();

        // Single LLM call for planning (cheaper, faster)
        return TaskPlanner.planTasksAsync(context.getForeman(), command)
            .thenApply(parsedResponse -> {
                // Let orchestrator distribute tasks
                String planId = orchestrator.processHumanCommand(
                    parsedResponse,
                    targets
                );

                return new MultiTargetResult(
                    ExecutionStrategy.COORDINATED,
                    targets.stream()
                        .map(t -> new SingleTargetResult(t.getSteveName(), planId))
                        .toList(),
                    planId
                );
            });
    }
}
```

---

## Command Aliases and Shortcuts

### 1. Alias System Design

**Purpose:** Allow users to define shortcuts for frequently used commands

**Data Structure:**
```java
public class CommandAliasRegistry {
    private final Map<String, CommandAlias> aliases;
    private final Path aliasConfigPath;

    public record CommandAlias(
        String name,
        String expansion,
        String description,
        List<String> requiredParameters,
        Instant createdAt,
        int usageCount
    ) {}

    /**
     * Registers a new alias.
     *
     * Example:
     * - name: "mine"
     * - expansion: "mine {resource} from {direction}"
     * - parameters: ["resource", "direction"]
     *
     * Usage: "mine iron north" → "mine iron from north"
     */
    public void registerAlias(String name, String expansion, String description);

    /**
     * Expands an aliased command.
     */
    public String expandAlias(String aliasedCommand);

    /**
     * Adds built-in aliases.
     */
    private void registerBuiltInAliases() {
        registerAlias("mine", "mine {resource}", "Mine specified resource");
        registerAlias("build", "build {structure}", "Build structure");
        registerAlias("follow", "follow {player}", "Follow player");
        registerAlias("atk", "attack {target}", "Attack target");
        registerAlias("come", "follow me", "Come to me");
        registerAlias("stay", "pathfind {x} {y} {z}", "Stay at location");
        registerAlias("gather", "gather {resource}", "Gather resources");
        registerAlias("stop", "stop all actions", "Stop current action");
    }

    /**
     * Loads aliases from config file.
     * Format: name=expansion|description
     */
    public void loadAliases(Path configPath);

    /**
     * Saves aliases to config file.
     */
    public void saveAliases(Path configPath);
}
```

### 2. Parameter Substitution

```java
public class AliasExpander {
    /**
     * Expands alias with parameter substitution.
     *
     * Examples:
     * - "mine iron" with alias "mine {resource}" → "mine iron"
     * - "mine 10 iron" with alias "mine {count} {resource}" → "mine {quantity: 10, resource: iron}"
     * - "build house here" with alias "build {type} here" → "build {structure: house}"
     */
    public String expand(String aliasedCommand, CommandAlias alias) {
        String expansion = alias.expansion();
        List<String> words = Arrays.asList(aliasedCommand.split("\\s+"));

        // Extract parameters from command
        Map<String, String> params = extractParameters(aliasedCommand, alias);

        // Substitute into expansion
        for (Map.Entry<String, String> entry : params.entrySet()) {
            expansion = expansion.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return expansion;
    }

    /**
     * Extracts named parameters from command string.
     */
    private Map<String, String> extractParameters(String command, CommandAlias alias) {
        Map<String, String> params = new HashMap<>();
        String[] words = command.split("\\s+");
        List<String> requiredParams = alias.requiredParameters();

        // Simple positional matching
        for (int i = 0; i < Math.min(requiredParams.size(), words.length); i++) {
            params.put(requiredParams.get(i), words[i + 1]); // Skip alias name
        }

        return params;
    }
}
```

### 3. Context-Aware Aliases

```java
public class ContextAwareAliasResolver {
    /**
     * Resolves aliases based on current context.
     *
     * Examples:
     * - "here" → current player position
     * - "that" → last referenced block/entity
     * - "those" → last referenced blocks/entities
     * - "again" → repeat last command
     * - "also <command>" → same targets as last command
     */
    public String resolveWithContext(String command, CommandContext context) {
        String resolved = command;

        // Resolve "here" to position
        if (command.contains("here")) {
            BlockPos pos = context.getPlayerPosition();
            resolved = resolved.replace("here",
                String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ()));
        }

        // Resolve "again" to last command
        if (command.equals("again")) {
            String lastCommand = commandHistory.getLastCommand(context.getPlayerName());
            if (lastCommand != null) {
                resolved = lastCommand;
            }
        }

        // Resolve "also" to reuse last targets
        if (command.startsWith("also ")) {
            Set<String> lastTargets = commandHistory.getLastTargets(context.getPlayerName());
            String actualCommand = command.substring(5);
            resolved = String.join(", ", lastTargets) + ", " + actualCommand;
        }

        return resolved;
    }
}
```

---

## Command History and Undo

### 1. Persistent History System

```java
public class CommandHistory {
    private final Path historyPath;
    private final Deque<CommandEntry> history;
    private final Map<String, Deque<CommandEntry>> playerHistory;
    private static final int MAX_HISTORY_SIZE = 1000;

    public record CommandEntry(
        String id,
        String playerName,
        String rawCommand,
        String expandedCommand,
        Set<String> targetNames,
        Instant timestamp,
        CommandStatus status,
        String planId,
        int executionTimeMs
    ) {}

    public enum CommandStatus {
        EXECUTING,
        COMPLETED,
        FAILED,
        CANCELLED,
        UNDONE
    }

    /**
     * Adds a command to history.
     */
    public void addCommand(CommandEntry entry);

    /**
     * Gets command history for player.
     */
    public List<CommandEntry> getHistory(String playerName, int limit);

    /**
     * Searches history by pattern.
     */
    public List<CommandEntry> search(String playerName, String pattern);

    /**
     * Gets the last command for player.
     */
    public CommandEntry getLastCommand(String playerName);

    /**
     * Gets the last targets used by player.
     */
    public Set<String> getLastTargets(String playerName);

    /**
     * Loads history from disk.
     */
    public void loadHistory() throws IOException;

    /**
     * Saves history to disk.
     */
    public void saveHistory() throws IOException;
}
```

### 2. Undo System Design

```java
public class CommandUndoManager {
    private final CommandHistory history;
    private final Map<String, Stack<UndoableAction>> undoStacks;

    /**
     * Undo last command for crew member.
     */
    public boolean undoCommand(String playerName, String crewName) {
        CommandEntry lastCommand = history.getLastCommand(playerName);
        if (lastCommand == null || !lastCommand.targetNames().contains(crewName)) {
            return false;
        }

        UndoableAction action = undoStacks.get(crewName).peek();
        if (action == null) {
            return false;
        }

        // Cancel current action
        ForemanEntity crew = getCrewMember(crewName);
        crew.getActionExecutor().stopCurrentAction();

        // Execute undo
        boolean success = action.undo();
        if (success) {
            undoStacks.get(crewName).pop();
            history.updateStatus(lastCommand.id(), CommandStatus.UNDONE);
        }

        return success;
    }

    /**
     * Registers an action as undoable.
     */
    public void registerUndoable(String crewName, UndoableAction action) {
        undoStacks.computeIfAbsent(crewName, k -> new Stack<>()).push(action);
    }
}

/**
 * Interface for actions that can be undone.
 */
public interface UndoableAction {
    /**
     * Undoes this action.
     *
     * Examples:
     * - Placed block → break block
     * - Mined block → place block back
     * - Follow → return to previous position
     * - Build → remove structure
     */
    boolean undo();

    /**
     * Returns a description of what this action does.
     */
    String getDescription();
}
```

### 3. Action-Level Undo Examples

```java
public class PlaceBlockAction extends BaseAction implements UndoableAction {
    private BlockPos placedPosition;
    private BlockState placedBlock;
    private BlockState previousBlock;

    @Override
    public boolean undo() {
        // Restore previous block state
        foreman.level().setBlock(placedPosition, previousBlock, 3);
        return true;
    }

    @Override
    public void start() {
        // Save previous state before placing
        placedPosition = getTargetPosition();
        previousBlock = foreman.level().getBlockState(placedPosition);

        // Place block
        // ... existing placement logic ...

        // Register as undoable
        MineWrightMod.getUndoManager().registerUndoable(
            foreman.getSteveName(), this);
    }
}

public class BuildStructureAction extends BaseAction implements UndoableAction {
    private List<BlockPos> placedBlocks;
    private Map<BlockPos, BlockState> previousStates;

    @Override
    public boolean undo() {
        // Remove all placed blocks
        for (BlockPos pos : placedBlocks) {
            BlockState previous = previousStates.get(pos);
            foreman.level().setBlock(pos, previous, 3);
        }
        return true;
    }
}
```

---

## Context-Aware Completion

### 1. Completion System Design

```java
public class CommandCompletion {
    private final CommandAliasRegistry aliasRegistry;
    private final CrewManager crewManager;
    private final WorldKnowledge worldKnowledge;

    /**
     * Gets completion suggestions for partial command.
     *
     * Context factors:
     * - Available crew members
     * - Nearby blocks/entities
     * - Command history frequency
     * - Current biome/terrain
     * - Player's inventory
     * - Time of day
     */
    public List<CompletionSuggestion> getCompletions(
        String partial,
        CommandContext context
    ) {
        List<CompletionSuggestion> suggestions = new ArrayList<>();

        // 1. Crew name completions
        if (looksLikeCrewTarget(partial)) {
            suggestions.addAll(getCrewCompletions(partial, context));
        }

        // 2. Command/alias completions
        if (looksLikeCommand(partial)) {
            suggestions.addAll(getCommandCompletions(partial, context));
        }

        // 3. Parameter completions
        if (hasCommandPrefix(partial)) {
            suggestions.addAll(getParameterCompletions(partial, context));
        }

        // 4. Contextual completions
        suggestions.addAll(getContextualCompletions(partial, context));

        // Sort by relevance
        suggestions.sort(Comparator.comparing(CompletionSuggestion::relevance).reversed());

        return suggestions;
    }

    /**
     * Gets crew name completions with relevance scoring.
     */
    private List<CompletionSuggestion> getCrewCompletions(
        String partial,
        CommandContext context
    ) {
        return context.getAvailableCrew().stream()
            .filter(crew -> crew.getSteveName().toLowerCase().startsWith(partial.toLowerCase()))
            .map(crew -> new CompletionSuggestion(
                crew.getSteveName(),
                "Crew member",
                calculateCrewRelevance(crew, context),
                CompletionType.CREW
            ))
            .toList();
    }

    /**
     * Gets command/alias completions.
     */
    private List<CompletionSuggestion> getCommandCompletions(
        String partial,
        CommandContext context
    ) {
        List<CompletionSuggestion> suggestions = new ArrayList<>();

        // Built-in actions
        suggestions.addAll(getActionCompletions(partial));

        // User-defined aliases
        suggestions.addAll(getAliasCompletions(partial));

        // History-based suggestions (frequently used)
        suggestions.addAll(getHistoryCompletions(partial, context));

        return suggestions;
    }

    /**
     * Gets contextual parameter completions.
     */
    private List<CompletionSuggestion> getParameterCompletions(
        String partial,
        CommandContext context
    ) {
        // Block type completions
        if (partial.matches(".*(mine|place|build).*")) {
            return getBlockTypeCompletions(partial, context);
        }

        // Entity target completions
        if (partial.matches(".*attack.*")) {
            return getEntityCompletions(partial, context);
        }

        // Direction completions
        if (partial.matches(".*(from|to|towards).*")) {
            return getDirectionCompletions(partial);
        }

        return List.of();
    }

    /**
     * Gets contextually relevant suggestions.
     */
    private List<CompletionSuggestion> getContextualCompletions(
        String partial,
        CommandContext context
    ) {
        List<CompletionSuggestion> suggestions = new ArrayList<>();

        // Nearby blocks
        if (isNearbyBlocksRelevant(partial)) {
            suggestions.addAll(getNearbyBlockCompletions(context));
        }

        // Nearby entities
        if (isNearbyEntitiesRelevant(partial)) {
            suggestions.addAll(getNearbyEntityCompletions(context));
        }

        // Biome-specific
        suggestions.addAll(getBiomeSpecificCompletions(context));

        // Time-based
        suggestions.addAll(getTimeBasedCompletions(context));

        return suggestions;
    }
}

public record CompletionSuggestion(
    String text,
    String description,
    double relevance,
    CompletionType type,
    String preview
) {
    public enum CompletionType {
        CREW,
        COMMAND,
        ALIAS,
        PARAMETER,
        BLOCK,
        ENTITY,
        HISTORY
    }
}
```

### 2. GUI Integration

```java
public class ForemanOfficeGUI {
    private static CommandCompletion completion;
    private static List<CompletionSuggestion> currentSuggestions;
    private static int selectedSuggestionIndex;

    /**
     * Renders completion suggestions in GUI.
     */
    private static void renderCompletions(GuiGraphics graphics, int panelX, int inputY) {
        if (currentSuggestions.isEmpty()) return;

        int suggestionY = inputY - 30;
        int maxSuggestions = 5;

        for (int i = 0; i < Math.min(currentSuggestions.size(), maxSuggestions); i++) {
            CompletionSuggestion suggestion = currentSuggestions.get(i);
            boolean isSelected = i == selectedSuggestionIndex;

            // Draw suggestion box
            int color = isSelected ? 0xFF4CAF50 : 0xFF333333;
            graphics.fill(panelX + 10, suggestionY, panelX + PANEL_WIDTH - 10, suggestionY + 20, color);

            // Draw suggestion text
            String text = suggestion.text();
            String desc = suggestion.description();

            graphics.drawString(mc.font, text, panelX + 15, suggestionY + 5,
                isSelected ? 0xFFFFFFFF : 0xFFAAAAAA);

            if (desc != null) {
                int descWidth = mc.font.width(desc);
                graphics.drawString(mc.font, desc,
                    panelX + PANEL_WIDTH - 15 - descWidth, suggestionY + 5, 0xFF666666);
            }

            suggestionY += 22;
        }
    }

    /**
     * Handles Tab key for completion cycling.
     */
    public static boolean handleTab() {
        if (!isOpen || currentSuggestions.isEmpty()) return false;

        selectedSuggestionIndex = (selectedSuggestionIndex + 1) % currentSuggestions.size();
        CompletionSuggestion selected = currentSuggestions.get(selectedSuggestionIndex);

        // Apply completion
        String currentText = inputBox.getValue();
        String completed = applyCompletion(currentText, selected);

        inputBox.setValue(completed);
        return true;
    }

    /**
     * Updates completions on text change.
     */
    private static void updateCompletions(String text) {
        CommandContext context = buildCurrentContext();
        currentSuggestions = completion.getCompletions(text, context);
        selectedSuggestionIndex = 0;
    }
}
```

---

## Batch Command Processing

### 1. Command Batch Design

```java
public class CommandBatch {
    private final List<BatchEntry> entries;
    private final BatchExecutionStrategy strategy;
    private final String batchId;
    private boolean isExecuting = false;

    public enum BatchExecutionStrategy {
        SEQUENTIAL,      // Execute one by one, stop on failure
        SEQUENTIAL_IGNORE,  // Execute all, ignore failures
        PARALLEL,        // Execute all simultaneously
        CONDITIONAL      // Execute based on conditions
    }

    public record BatchEntry(
        String id,
        String command,
        CommandTarget target,
        boolean continueOnFailure,
        Predicate<CommandResult> condition
    ) {}

    /**
     * Adds a command to the batch.
     */
    public CommandBatch addCommand(String command, CommandTarget target);

    /**
     * Adds a conditional command.
     *
     * Example:
     * - "if previous succeeded, then build house"
     */
    public CommandBatch addConditionalCommand(
        String command,
        CommandTarget target,
        Predicate<CommandResult> condition
    );

    /**
     * Executes the batch.
     */
    public CompletableFuture<BatchResult> execute(CommandContext context);

    /**
     * Creates a batch from script file.
     *
     * Script format:
     * ```
     * # Comment
     * Alex, build house
     * Bob, mine iron
     * if $SUCCESS, then Charlie, follow Alex
     * ```
     */
    public static CommandBatch fromScript(Path scriptPath) throws IOException;
}
```

### 2. Script Execution

```java
public class CommandScriptExecutor {
    private final CommandRouter router;

    /**
     * Executes a command script.
     */
    public CompletableFuture<ScriptResult> executeScript(
        Path scriptPath,
        CommandContext context
    ) {
        List<String> lines = Files.readAllLines(scriptPath);
        CommandBatch batch = CommandBatch.create();

        for (String line : lines) {
            line = line.trim();

            // Skip comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) continue;

            // Parse conditional
            if (line.startsWith("if ")) {
                batch.addConditionalCommand(
                    extractCommand(line),
                    extractTarget(line),
                    extractCondition(line)
                );
            } else {
                // Regular command
                ParsedTarget parsed = parseTargetAndCommand(line);
                batch.addCommand(parsed.command(), parsed.target());
            }
        }

        return batch.execute(context);
    }

    /**
     * Built-in scripts library.
     */
    public static final class BuiltInScripts {
        public static final String BASE_SETUP = """
            # Base setup script
            spawn Builder
            spawn Miner
            spawn Guard

            Builder, build a small house
            Miner, mine 64 cobblestone
            Guard, follow me
            """;

        public static final String RESOURCE_GATHER = """
            # Resource gathering operation
            all, form a line
            Miner1, mine iron
            Miner2, mine coal
            Builder, prepare storage
            all, return to base when inventory full
            """;

        public static final String DEFENSE_SETUP = """
            # Defense perimeter setup
            Guard1, patrol north
            Guard2, patrol east
            Guard3, patrol south
            Guard4, patrol west
            Builder, build wall around perimeter
            """;
    }
}
```

### 3. Batch Execution with Coordination

```java
public class BatchCoordinator {
    private final OrchestratorService orchestrator;

    /**
     * Coordinates batch execution across multiple crew members.
     *
     * This integrates with the orchestration system to ensure
     * efficient task distribution and progress tracking.
     */
    public CompletableFuture<BatchResult> coordinateBatch(
        CommandBatch batch,
        CommandContext context
    ) {
        // Group batch entries by target
        Map<CommandTarget, List<BatchEntry>> grouped = batch.getEntries().stream()
            .collect(Collectors.groupingBy(BatchEntry::target));

        BatchResult result = new BatchResult(batch.getBatchId());

        // Execute each target group
        for (Map.Entry<CommandTarget, List<BatchEntry>> group : grouped.entrySet()) {
            CommandTarget target = group.getKey();
            List<BatchEntry> entries = group.getValue();

            // Resolve targets
            Set<ForemanEntity> crew = targetResolver.resolve(target, context);

            // Execute entries for this target group
            for (BatchEntry entry : entries) {
                if (entry.condition() != null) {
                    // Check condition
                    if (!entry.condition().test(result.getLastResult())) {
                        continue; // Skip this entry
                    }
                }

                // Route command
                router.routeCommand(entry.command(), target, context)
                    .thenAccept(entryResult -> {
                        result.addResult(entry.id(), entryResult);
                        if (!entryResult.isSuccess() && !entry.continueOnFailure()) {
                            // Stop batch execution
                            result.markStoppedEarly();
                        }
                    });
            }
        }

        return result.asCompleted();
    }
}
```

---

## GUI Improvements

### 1. Enhanced Command Input

**Current State:** Basic `EditBox` with history navigation

**Proposed Improvements:**

```java
public class EnhancedCommandInput {
    private final EditBox inputBox;
    private final List<CompletionSuggestion> suggestions;
    private final CommandHistory history;

    /**
     * Enhanced input features:
     * - Multi-line input for scripts
     * - Syntax highlighting
     * - Auto-completion popup
     * - Command preview panel
     * - Quick action buttons
     */
    public void renderEnhancedInput(GuiGraphics graphics, int x, int y, int width) {
        // 1. Main input field
        inputBox.render(graphics, mouseX, mouseY, partialTick);

        // 2. Completions popup
        if (!suggestions.isEmpty()) {
            renderCompletionsPopup(graphics, x, y - 100, width);
        }

        // 3. Command preview
        renderCommandPreview(graphics, x, y + 30, width);

        // 4. Quick action buttons
        renderQuickActions(graphics, x + width - 100, y);
    }

    /**
     * Renders command preview showing what will be executed.
     */
    private void renderCommandPreview(GuiGraphics graphics, int x, int y, int width) {
        String currentText = inputBox.getValue();

        // Parse and preview
        ParsedTarget parsed = targetParser.parse(currentText);

        // Show targets
        graphics.drawString(mc.font, "Targets:", x, y, 0xFF888888);
        int targetY = y + 15;
        for (String target : parsed.targets()) {
            graphics.drawString(mc.font, "  • " + target, x + 10, targetY, 0xFFCCCCCC);
            targetY += 12;
        }

        // Show resolved crew members
        Set<ForemanEntity> resolved = targetResolver.resolve(
            parsed.targetSpecifier(),
            buildCurrentContext()
        );

        graphics.drawString(mc.font,
            String.format("Will affect: %d crew members", resolved.size()),
            x, targetY + 5, 0xFF4CAF50);

        // Show estimated execution time
        long estimatedTime = estimateExecutionTime(parsed.command(), resolved.size());
        graphics.drawString(mc.font,
            String.format("Est. time: ~%ds", estimatedTime),
            x + width - 100, targetY + 5, 0xFF888888);
    }

    /**
     * Renders quick action buttons.
     */
    private void renderQuickActions(GuiGraphics graphics, int x, int y) {
        // Undo button
        renderButton(graphics, x, y, "↶ Undo", 0xFFFFFF00,
            () -> undoManager.undoLastCommand());

        // Redo button
        renderButton(graphics, x + 30, y, "↷ Redo", 0xFF00FFFF,
            () -> undoManager.redoLastCommand());

        // Repeat button
        renderButton(graphics, x + 60, y, "↻ Repeat", 0xFF00FF00,
            () -> repeatLastCommand());

        // Clear button
        renderButton(graphics, x + 90, y, "✖ Clear", 0xFFFF0000,
            () -> inputBox.setValue(""));
    }
}
```

### 2. Command Palette

```java
public class CommandPalette {
    private boolean isOpen = false;
    private String filter = "";
    private List<PaletteEntry> entries;
    private int selectedIndex = 0;

    public record PaletteEntry(
        String name,
        String description,
        String command,
        PaletteCategory category,
        int hotkey
    ) {}

    public enum PaletteCategory {
        COMMANDS,    // Built-in commands
        ALIASES,     // User-defined aliases
        CREW,        // Crew member targets
        SCRIPTS,     // Saved scripts
        HISTORY      // Recent commands
    }

    /**
     * Renders command palette overlay (Ctrl+P style).
     */
    public void renderPalette(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!isOpen) return;

        int paletteWidth = 400;
        int paletteHeight = 300;
        int paletteX = (screenWidth - paletteWidth) / 2;
        int paletteY = (screenHeight - paletteHeight) / 2;

        // Draw background
        graphics.fillGradient(paletteX, paletteY,
            paletteX + paletteWidth, paletteY + paletteHeight,
            0xDD000000, 0xDD000000);

        // Draw filter input
        renderFilterInput(graphics, paletteX, paletteY, paletteWidth);

        // Draw filtered entries
        renderFilteredEntries(graphics, paletteX, paletteY + 40, paletteWidth, paletteHeight);
    }

    /**
     * Filters palette entries by search text.
     */
    private void filterEntries() {
        entries = getAllEntries().stream()
            .filter(entry -> matchesFilter(entry, filter))
            .sorted(Comparator
                .comparing((PaletteEntry e) -> e.category().ordinal())
                .thenComparing(e -> -frequencyScore(e)))
            .toList();
    }

    /**
     * Handles keyboard input for palette navigation.
     */
    public boolean handleKeyPress(int keyCode) {
        if (!isOpen) return false;

        return switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                isOpen = false;
                yield true;
            }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                navigate(keyCode == GLFW.GLFW_KEY_UP ? -1 : 1);
                yield true;
            }
            case GLFW.GLFW_KEY_ENTER -> {
                executeSelected();
                isOpen = false;
                yield true;
            }
            default -> false;
        };
    }
}
```

### 3. Status Dashboard

```java
public class CommandStatusDashboard {
    /**
     * Renders status dashboard for active commands.
     */
    public void renderDashboard(GuiGraphics graphics, int x, int y, int width) {
        // Get all active plans from orchestrator
        OrchestratorService orchestrator = MineWrightMod.getOrchestratorService();
        Set<String> activePlans = orchestrator.getActivePlanIds();

        // Draw header
        graphics.drawString(mc.font, "Active Commands", x, y, 0xFFFFFFFF);

        int yPos = y + 20;

        // Draw each active plan
        for (String planId : activePlans) {
            int progress = orchestrator.getPlanProgress(planId);

            // Progress bar
            int barWidth = width - 40;
            int filledWidth = (barWidth * progress) / 100;

            // Background bar
            graphics.fill(x + 20, yPos, x + 20 + barWidth, yPos + 10, 0xFF333333);

            // Filled portion
            int color = progress == 100 ? 0xFF4CAF50 : 0xFF2196F3;
            graphics.fill(x + 20, yPos, x + 20 + filledWidth, yPos + 10, color);

            // Plan ID and percentage
            graphics.drawString(mc.font,
                String.format("%s: %d%%", planId, progress),
                x + 20, yPos - 12, 0xFFCCCCCC);

            yPos += 30;
        }

        // Queue status
        renderQueueStatus(graphics, x, yPos, width);
    }

    /**
     * Renders command queue status.
     */
    private void renderQueueStatus(GuiGraphics graphics, int x, int y, int width) {
        graphics.drawString(mc.font, "Command Queue", x, y, 0xFF888888);

        Collection<ForemanEntity> allCrew = MineWrightMod.getCrewManager()
            .getAllCrewMembers();

        int yPos = y + 20;

        for (ForemanEntity crew : allCrew) {
            ActionExecutor executor = crew.getActionExecutor();
            String currentGoal = executor.getCurrentGoal();

            String status = currentGoal != null
                ? "Working: " + currentGoal
                : "Idle";

            // Status indicator
            int statusColor = currentGoal != null ? 0xFF4CAF50 : 0xFF888888;
            graphics.fill(x + 20, yPos + 4, x + 24, yPos + 8, statusColor);

            // Status text
            graphics.drawString(mc.font,
                String.format("%s: %s", crew.getSteveName(), status),
                x + 30, yPos, 0xFFCCCCCC);

            yPos += 15;
        }
    }
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Tasks:**
1. Create `CommandRouter` class with basic routing logic
2. Implement `CommandContext` and `CommandTarget` data structures
3. Add `CommandTargetParser` with enhanced pattern matching
4. Implement `TargetResolver` for flexible target resolution
5. Add unit tests for target parsing and resolution

**Deliverables:**
- `CommandRouter.java`
- `CommandContext.java`
- `CommandTarget.java`
- `CommandTargetParser.java`
- `TargetResolver.java`
- Test suite with >80% coverage

### Phase 2: Multi-Target Execution (Week 3-4)

**Tasks:**
1. Implement `MultiTargetExecutor` with different strategies
2. Integrate with existing `OrchestratorService`
3. Add parallel execution support
4. Implement failure handling and recovery
5. Add progress tracking for multi-target commands

**Deliverables:**
- `MultiTargetExecutor.java`
- Integration tests with multiple crew members
- Performance benchmarks
- Documentation on execution strategies

### Phase 3: Aliases and Shortcuts (Week 5-6)

**Tasks:**
1. Implement `CommandAliasRegistry` with persistence
2. Add built-in aliases for common commands
3. Create `AliasExpander` for parameter substitution
4. Implement context-aware alias resolution
5. Add alias management GUI

**Deliverables:**
- `CommandAliasRegistry.java`
- `AliasExpander.java`
- `ContextAwareAliasResolver.java`
- GUI components for alias management
- Configuration file format documentation

### Phase 4: History and Undo (Week 7-8)

**Tasks:**
1. Implement `CommandHistory` with disk persistence
2. Create `CommandUndoManager` with undo stack
3. Add `UndoableAction` interface
4. Implement undo for existing actions (PlaceBlock, BuildStructure, etc.)
5. Add undo/redo to GUI

**Deliverables:**
- `CommandHistory.java`
- `CommandUndoManager.java`
- `UndoableAction.java`
- Updated action classes with undo support
- GUI integration with keyboard shortcuts

### Phase 5: Completion System (Week 9-10)

**Tasks:**
1. Implement `CommandCompletion` engine
2. Add relevance scoring for suggestions
3. Implement context-aware completions
4. Create GUI components for completion display
5. Add Tab key cycling and Enter to accept

**Deliverables:**
- `CommandCompletion.java`
- GUI completion popup
- Integration with existing `ForemanOfficeGUI`
- Performance testing for large suggestion lists

### Phase 6: Batch Processing (Week 11-12)

**Tasks:**
1. Implement `CommandBatch` with different execution strategies
2. Create script file format and parser
3. Implement `BatchCoordinator` for orchestrated execution
4. Add built-in script library
5. Create script editor GUI

**Deliverables:**
- `CommandBatch.java`
- `CommandScriptExecutor.java`
- `BatchCoordinator.java`
- Script format documentation
- Built-in scripts library
- Script editor GUI

### Phase 7: GUI Enhancements (Week 13-14)

**Tasks:**
1. Implement enhanced command input with preview
2. Create command palette (Ctrl+P style)
3. Add status dashboard for active commands
4. Implement keyboard shortcuts throughout
5. Add visual feedback for command states

**Deliverables:**
- `EnhancedCommandInput.java`
- `CommandPalette.java`
- `CommandStatusDashboard.java`
- Updated `ForemanOfficeGUI` with all enhancements
- User documentation

### Phase 8: Testing and Polish (Week 15-16)

**Tasks:**
1. Comprehensive integration testing
2. Performance profiling and optimization
3. User documentation and tutorials
4. Bug fixes and refinement
5. Feature demonstration videos

**Deliverables:**
- Complete test suite with >90% coverage
- Performance benchmarks
- User guide documentation
- Tutorial videos
- Release notes

---

## Code Examples

### Example 1: Basic Multi-Target Command

```java
// Before: Current implementation
"Alex, Bob, mine iron"
→ Sends to Alex: "mine iron"
→ Sends to Bob: "mine iron"

// After: Enhanced routing
"Alex and Bob, mine 32 iron ore from the cave north of here"
→ Parses target: [Alex, Bob]
→ Parses command: "mine 32 iron ore from the cave north of here"
→ Resolves targets: Alex at [100, 64, 200], Bob at [105, 64, 205]
→ Coordinates via orchestrator for efficient mining
→ Reports progress: "Alex: 50%, Bob: 75%"
```

### Example 2: Role-Based Targeting

```java
// New feature: Role-based targeting
"all miners, mine diamond"
→ Resolves to all crew with role MINER
→ Coordinates diamond mining operation
→ Distributes tasks to avoid conflicts

"nearest guard, protect me"
→ Finds guard closest to player
→ Issues protect command

"all except Builder, follow me"
→ Targets all crew except Builder
→ Issues follow command
```

### Example 3: Command Aliases

```java
// User-defined alias in config
aliases:
  quickhouse: "build a 5x5x5 cobblestone house here"
  mineiron: "mine 64 iron ore"
  gatherall: "all, gather nearby resources"

// Usage
"quickhouse"
→ Expands to: "build a 5x5x5 cobblestone house here"

"Alex, quickhouse"
→ Expands to: "Alex, build a 5x5x5 cobblestone house here"

// Context-aware aliases
"also quickhouse"
→ Reuses last targets: "Bob and Charlie, build a 5x5x5 cobblestone house here"
```

### Example 4: Batch Script Execution

```java
// Script file: setup_base.mw
# Spawn initial crew
spawn Builder
spawn Miner
spawn Guard

# Build base
Builder, build a house here
Miner, mine 128 cobblestone
Guard, follow me

# Wait for completion
waitFor Builder

# Build defenses
Builder, build a wall around the base

// Execute script
/run setup_base.mw
→ Executes all commands sequentially
→ Shows progress dashboard
→ Handles failures gracefully
```

### Example 5: Undo/Redo

```java
// User executes commands
"Builder, build a tower"
→ Builder builds tower

"Guard, follow me"
→ Guard starts following

// User wants to undo
Ctrl+Z (or /undo last)
→ Guard stops following (undo last)
→ Tower still exists

Ctrl+Z again
→ Tower is removed (undo first)
→ Back to initial state

Ctrl+Y (or /redo)
→ Tower is rebuilt (redo)
```

### Example 6: Context-Aware Completion

```java
// User types: "Ali"
→ Shows: [Alex, Alice, All]

// User types: "Alex, "
→ Shows: [mine, build, follow, attack, gather, ...]

// User types: "Alex, mine"
→ Shows: [iron, coal, diamond, gold, ...]
   (sorted by proximity and rarity)

// User types: "Alex, mine iro"
→ Auto-completes to: "Alex, mine iron"

// User presses Tab
→ Cycles through: "Alex, mine iron ore"
→ Then: "Alex, mine iron ore from north"
→ Then: "Alex, mine iron ore from north cave"
```

### Example 7: Command Preview

```java
// User types: "Alex and Bob, build a castle"
→ GUI shows preview:
┌─────────────────────────────────────┐
│ Targets:                             │
│   • Alex (at [100, 64, 200])         │
│   • Bob (at [105, 64, 205])          │
│ Will affect: 2 crew members          │
│ Est. time: ~5min                     │
│                                      │
│ Press Enter to execute, Esc to cancel│
└─────────────────────────────────────┘
```

---

## Conclusion

The proposed command routing improvements will significantly enhance MineWright's usability and functionality. The modular design allows for incremental implementation while maintaining backward compatibility with existing systems.

**Key Benefits:**
- More intuitive multi-target commands
- Faster command entry with aliases and completion
- Safety net with undo/redo functionality
- Powerful batch processing for complex operations
- Enhanced GUI with preview and status feedback

**Integration Points:**
- Extends existing `ActionExecutor` and `TaskPlanner`
- Leverages `OrchestratorService` for coordination
- Builds on `AgentStateMachine` for state tracking
- Compatible with current plugin architecture

**Next Steps:**
1. Review and approve architecture
2. Prioritize features based on user feedback
3. Begin Phase 1 implementation
4. Establish testing strategy
5. Create development branch

---

**Document Metadata:**
- Author: Research & Analysis Team
- Review Status: Pending Review
- Related Documents:
  - `/docs/ACTION_SYSTEM_IMPROVEMENTS.md`
  - `/docs/LLM_OPTIMIZATION_STRATEGIES.md`
  - `/CLAUDE.md` (Project Overview)
