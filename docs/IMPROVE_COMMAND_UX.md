# Command System UX Improvements

**Date:** 2025-02-27
**Status:** Analysis & Recommendations
**Scope:** Natural language command processing, GUI feedback, error handling

---

## Executive Summary

The MineWright command system has a solid foundation with async LLM processing, GUI chat interface, and command history. However, there are significant opportunities to improve the user experience through better error messages, command suggestions, progress indicators, and enhanced natural language understanding.

**Key Findings:**
- Strong async architecture prevents game freezing
- GUI provides good visual feedback with message bubbles
- Command history with arrow keys works well
- Missing: intelligent command suggestions, detailed error recovery, progress visualization
- Limited natural language understanding (no fuzzy matching or intent recognition)

---

## Current State Analysis

### Command Parsing (Natural Language Understanding)

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\llm\TaskPlanner.java`

**Current Approach:**
- Raw natural language sent directly to LLM
- No pre-processing or intent detection
- No fuzzy matching for common command patterns
- No command templates or shortcuts
- LLM handles all interpretation

**Strengths:**
- Flexible: can understand any command the LLM can parse
- Async processing prevents blocking
- World context provided to LLM (position, nearby entities, blocks)

**Weaknesses:**
- No local validation before LLM call (wastes API credits on invalid commands)
- No fuzzy matching for typos ("buld" instead of "build")
- No intent caching (similar commands re-planned every time)
- No learning from past commands
- No command aliases or shortcuts

---

### Error Messages

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`

**Current Error Messages:**

| Scenario | Current Message | Issue |
|----------|----------------|-------|
| Planning failed | "I couldn't understand that command." | Generic, no context |
| Planning in progress | "Hold on, I'm still thinking about the previous command..." | Good, but no ETA |
| AI system error | "Sorry, I'm having trouble with my AI systems!" | Too vague |
| Action failed | "Action failed: {message}" | No recovery suggestions |
| Unknown action | "Unknown action type: {type}" | Lists no available actions |
| Crew not found | "Crew member not found: {name}" | Doesn't suggest available crew |

**Issues:**
- Errors are generic and don't guide users toward solutions
- No suggested corrections or alternatives
- No context about what went wrong
- No recovery steps
- No examples of valid commands

---

### Command Feedback

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\client\ForemanOfficeGUI.java`

**Current Feedback:**
- "Thinking..." message when planning starts
- "Okay! {plan}" when planning succeeds
- Message bubbles color-coded by sender (green=user, blue=crew, orange=system)
- Scrollable message history (500 messages max)
- Progress indicators missing for long-running actions

**Strengths:**
- Visual feedback is immediate and clear
- Color coding helps identify message types
- Scrollable history prevents losing context
- Non-blocking updates

**Weaknesses:**
- No progress bars for long actions (building, mining)
- No estimated time to completion
- No intermediate progress updates during action execution
- No visual indication of action queue status
- No way to see what the agent is currently doing vs. queued

---

### Command History

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\client\ForemanOfficeGUI.java`

**Current Implementation:**
- Arrow up/down for previous/next command
- Stores last 50 commands
- Auto-scrolls to bottom on new message
- History resets when panel closes

**Strengths:**
- Standard keyboard shortcuts (up/down arrows)
- Good history size (50 commands)
- Auto-scroll prevents getting lost

**Weaknesses:**
- No persistent history across sessions
- No command search/filtering
- No command favorites or pins
- No command statistics (most used, success rate)
- No command duplication detection
- No command templates or snippets

---

### Autocomplete/Suggestions

**Current State:** None

**Missing Features:**
- No autocomplete while typing
- No command suggestions based on context
- No fuzzy matching for typos
- No predictive text based on history
- No command templates
- No example commands displayed

---

## Recommended Improvements

### 1. Enhanced Error Messages

**Priority:** High
**Impact:** High
**Effort:** Medium

#### 1.1 Specific Error Messages with Recovery Steps

```java
// New error message system
public class CommandErrorHandler {

    public static String getErrorExplanation(CommandError error) {
        return switch (error.getType()) {
            case CREW_NOT_FOUND -> {
                List<String> available = getCrewNames();
                String suggestion = available.isEmpty()
                    ? "No crew members exist. Use 'spawn <name>' to create one."
                    : "Available crew: " + String.join(", ", available);
                yield String.format("""
                    I couldn't find a crew member named '%s'.

                    %s

                    Examples:
                    - spawn Builder
                    - spawn Miner
                    """, error.getTarget(), suggestion);
            }
            case INVALID_BLOCK_TYPE -> {
                String corrected = suggestCorrection(error.getTarget(), VALID_BLOCKS);
                yield String.format("""
                    '%s' is not a valid block type.

                    Did you mean: %s?

                    Valid blocks include:
                    - oak_planks, cobblestone, stone_bricks
                    - oak_log, spruce_log, birch_log
                    - glass, glass_pane, bricks
                    """, error.getTarget(), corrected);
            }
            case PLANNING_FAILED -> {
                yield String.format("""
                    I had trouble understanding that command.

                    Try rephrasing:
                    - "build a house" instead of "construct house"
                    - "get me iron" instead of "obtain iron ore"
                    - "kill mobs" instead of "eliminate hostiles"

                    Or try these examples:
                    - build a wooden cabin
                    - mine 32 iron ore
                    - follow me
                    """);
            }
            case NO_RESOURCE_NEARBY -> {
                yield String.format("""
                    I can't find any %s nearby.

                    Try:
                    - Move to an area with %s
                    - Use "pathfind" to explore
                    - Check different biomes (caves, mountains)
                    """, error.getTarget(), error.getTarget());
            }
            case API_RATE_LIMIT -> {
                yield String.format("""
                    I'm thinking too fast and need to slow down!

                    Wait a moment, then try again.
                    Or switch to a faster AI provider in config.
                    """);
            }
            default -> "Something went wrong. Please try again.";
        };
    }

    private static String suggestCorrection(String input, Set<String> validOptions) {
        // Levenshtein distance fuzzy matching
        return validOptions.stream()
            .min(Comparator.comparingInt(opt -> levenshteinDistance(input, opt)))
            .orElse("unknown");
    }
}
```

#### 1.2 Error Categories

| Category | Trigger | Recovery Steps |
|----------|---------|----------------|
| Crew Management | Invalid crew name | List available crew, suggest spawn |
| Block/Item Validation | Invalid block/item | Suggest correction, list valid options |
| Planning Failure | LLM couldn't parse | Provide example commands |
| Resource Not Found | Can't find target | Suggest locations/biomes |
| Rate Limit | API throttled | Wait or switch provider |
| Network Error | Connection failed | Retry with exponential backoff |
| Permission Denied | Can't access area | Suggest alternative locations |

---

### 2. Command Suggestion System

**Priority:** High
**Impact:** High
**Effort:** Medium-High

#### 2.1 Context-Aware Suggestions

```java
public class CommandSuggestionEngine {

    /**
     * Get suggestions based on current context
     */
    public List<CommandSuggestion> getSuggestions(ForemanEntity crew, String partialInput) {
        List<CommandSuggestion> suggestions = new ArrayList<>();

        // 1. Fuzzy match against command templates
        suggestions.addAll(getFuzzyMatches(partialInput, COMMAND_TEMPLATES));

        // 2. Context-aware suggestions
        WorldKnowledge world = new WorldKnowledge(crew);
        suggestions.addAll(getContextualSuggestions(crew, world));

        // 3. History-based suggestions
        suggestions.addAll(getHistoryBasedSuggestions(partialInput));

        // 4. Sort by relevance
        return suggestions.stream()
            .sorted(Comparator.comparingDouble(CommandSuggestion::getRelevance).reversed())
            .limit(5)
            .toList();
    }

    private List<CommandSuggestion> getContextualSuggestions(ForemanEntity crew, WorldKnowledge world) {
        List<CommandSuggestion> suggestions = new ArrayList<>();

        // Suggest mining based on nearby ores
        if (world.hasNearbyBlocks("iron_ore", "diamond_ore", "coal_ore")) {
            suggestions.add(new CommandSuggestion(
                "mine nearby ores",
                "Mine resources I can see nearby",
                0.9
            ));
        }

        // Suggest building based on inventory
        if (crew.hasBlocksInInventory()) {
            suggestions.add(new CommandSuggestion(
                "build a house",
                "Use my inventory to build",
                0.8
            ));
        }

        // Suggest combat based on nearby hostiles
        if (world.hasNearbyHostiles()) {
            suggestions.add(new CommandSuggestion(
                "attack mobs",
                "Defend against nearby monsters",
                0.95
            ));
        }

        // Suggest farming based on biome
        if (world.isInPlainsBiome()) {
            suggestions.add(new CommandSuggestion(
                "build a wheat farm",
                "Create automatic wheat farming",
                0.7
            ));
        }

        return suggestions;
    }
}
```

#### 2.2 Command Templates

```java
public class CommandTemplates {

    public static final List<CommandTemplate> TEMPLATES = List.of(
        new CommandTemplate(
            "build",
            List.of("build", "construct", "create", "make"),
            "build {structure} using {blocks}",
            List.of(
                "build a wooden cabin",
                "build a stone castle",
                "build a modern house",
                "construct a tower",
                "make a barn"
            )
        ),
        new CommandTemplate(
            "mine",
            List.of("mine", "gather", "collect", "get", "harvest"),
            "mine {quantity} {resource}",
            List.of(
                "mine 32 iron ore",
                "get me 16 diamonds",
                "collect 64 coal",
                "gather 24 gold ore",
                "harvest 10 emeralds"
            )
        ),
        new CommandTemplate(
            "attack",
            List.of("attack", "kill", "fight", "hunt"),
            "attack {target}",
            List.of(
                "kill mobs",
                "attack hostile creatures",
                "hunt creepers",
                "fight zombies",
                "defend against monsters"
            )
        ),
        new CommandTemplate(
            "follow",
            List.of("follow", "come with", "stay with"),
            "follow {player}",
            List.of(
                "follow me",
                "come with me",
                "stay close to me"
            )
        ),
        new CommandTemplate(
            "craft",
            List.of("craft", "make", "create"),
            "craft {quantity} {item}",
            List.of(
                "craft 10 iron pickaxes",
                "make 5 torches",
                "create 1 diamond sword"
            )
        )
    );
}
```

---

### 3. Progress Indicators

**Priority:** High
**Impact:** Medium
**Effort:** Medium

#### 3.1 Visual Progress Bars

```java
// Add to ForemanOfficeGUI
private static void renderProgressBar(GuiGraphics graphics, int x, int y, int width, int height,
                                     float progress, String label) {
    // Background
    graphics.fill(x, y, x + width, y + height, 0xFF333333);

    // Progress bar
    int progressWidth = (int)(width * progress);
    int color = getProgressColor(progress);
    graphics.fill(x, y, x + progressWidth, y + height, color);

    // Label
    graphics.drawString(mc.font, label, x, y - 12, 0xFFFFFFFF);

    // Percentage text
    String percentText = String.format("%.0f%%", progress * 100);
    int textWidth = mc.font.width(percentText);
    graphics.drawString(mc.font, percentText, x + width - textWidth - 5, y + 2, 0xFFFFFFFF);
}

private static int getProgressColor(float progress) {
    if (progress < 0.3f) return 0xFFFF5555; // Red
    if (progress < 0.7f) return 0xFFFFAA00; // Orange
    return 0xFF55FF55; // Green
}
```

#### 3.2 Action Progress Tracking

```java
// Add to BaseAction
public abstract class BaseAction {
    private int totalSteps;
    private int completedSteps;

    public float getProgress() {
        if (totalSteps == 0) return 0;
        return (float) completedSteps / totalSteps;
    }

    protected void setProgress(int completed, int total) {
        this.completedSteps = completed;
        this.totalSteps = total;
    }
}

// Example: BuildStructureAction
public class BuildStructureAction extends BaseAction {
    @Override
    public float getProgress() {
        if (totalBlocks == 0) return 0;
        return (float) placedBlocks / totalBlocks;
    }
}
```

#### 3.3 Queue Status Display

```java
// Add to ForemanOfficeGUI
private static void renderQueueStatus(GuiGraphics graphics, int x, int y, Queue<Task> queue) {
    graphics.drawString(mc.font, "Task Queue:", x, y, 0xFF00AA00);

    int yOffset = y + 15;
    int index = 1;
    for (Task task : queue) {
        if (index > 3) break; // Show max 3 queued tasks

        String taskDesc = String.format("%d. %s", index, formatTask(task));
        graphics.drawString(mc.font, taskDesc, x, yOffset, 0xFF888888);
        yOffset += 12;
        index++;
    }

    if (queue.size() > 3) {
        graphics.drawString(mc.font, String.format("... and %d more", queue.size() - 3),
            x, yOffset, 0xFF666666);
    }
}
```

---

### 4. Enhanced Command History

**Priority:** Medium
**Impact:** Medium
**Effort:** Low-Medium

#### 4.1 Persistent History

```java
public class CommandHistoryManager {
    private static final String HISTORY_FILE = "config/minewright_command_history.json";
    private static final int MAX_HISTORY = 100;

    private final List<CommandHistoryEntry> history = new ArrayList<>();

    public void saveHistory() {
        try {
            Files.createDirectories(Paths.get("config"));
            String json = GSON.toJson(history);
            Files.writeString(Path.of(HISTORY_FILE), json);
        } catch (IOException e) {
            MineWrightMod.LOGGER.error("Failed to save command history", e);
        }
    }

    public void loadHistory() {
        try {
            if (Files.exists(Path.of(HISTORY_FILE))) {
                String json = Files.readString(Path.of(HISTORY_FILE));
                Type type = new TypeToken<List<CommandHistoryEntry>>(){}.getType();
                history.addAll(GSON.fromJson(json, type));
            }
        } catch (IOException e) {
            MineWrightMod.LOGGER.error("Failed to load command history", e);
        }
    }
}

public class CommandHistoryEntry {
    private final String command;
    private final Instant timestamp;
    private final boolean success;
    private final String result;
    private final int executionTimeMs;
}
```

#### 4.2 Command Search

```java
public class CommandHistoryManager {
    public List<String> search(String query) {
        String lowerQuery = query.toLowerCase();

        return history.stream()
            .filter(entry -> entry.getCommand().toLowerCase().contains(lowerQuery))
            .map(CommandHistoryEntry::getCommand)
            .distinct()
            .limit(10)
            .toList();
    }

    public List<String> getMostUsed(int count) {
        Map<String, Long> frequencies = history.stream()
            .collect(Collectors.groupingBy(
                CommandHistoryEntry::getCommand,
                Collectors.counting()
            ));

        return frequencies.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(count)
            .map(Map.Entry::getKey)
            .toList();
    }
}
```

---

### 5. Autocomplete System

**Priority:** Medium
**Impact:** Medium-High
**Effort:** Medium-High

#### 5.1 Real-Time Autocomplete

```java
// Add to ForemanOfficeGUI
private static List<String> getAutocompleteSuggestions(String partialInput) {
    List<String> suggestions = new ArrayList<>();

    // 1. Command history matches
    suggestions.addAll(history.stream()
        .filter(cmd -> cmd.toLowerCase().startsWith(partialInput.toLowerCase()))
        .distinct()
        .toList());

    // 2. Template matches
    suggestions.addAll(templates.stream()
        .filter(t -> t.matches(partialInput))
        .map(CommandTemplate::getExample)
        .toList());

    // 3. Crew name completion
    if (partialInput.matches("^(.*?\\s+)(\\w*)")) {
        String prefix = partialInput.replaceAll("^(.*?\\s+)\\w*$", "$1");
        String namePartial = partialInput.replaceAll("^.*?\\s+(\\w*)$", "$1");

        List<String> crewNames = MineWrightMod.getCrewManager().getCrewMemberNames();
        suggestions.addAll(crewNames.stream()
            .filter(name -> name.toLowerCase().startsWith(namePartial.toLowerCase()))
            .map(name -> prefix + name + " ")
            .toList());
    }

    // Remove duplicates and limit
    return suggestions.stream()
        .distinct()
        .limit(5)
        .toList();
}

private static void renderAutocompletePopup(GuiGraphics graphics, int x, int y, List<String> suggestions) {
    if (suggestions.isEmpty()) return;

    int popupHeight = suggestions.size() * 15 + 10;
    int popupWidth = 200;

    // Background
    graphics.fill(x, y, x + popupWidth, y + popupHeight, 0xDD000000);

    // Suggestions
    int yOffset = y + 5;
    for (int i = 0; i < suggestions.size(); i++) {
        String suggestion = suggestions.get(i);
        int color = (i == autocompleteIndex) ? 0xFF55FFFF : 0xFFFFFFFF;
        graphics.drawString(mc.font, suggestion, x + 5, yOffset, color);
        yOffset += 15;
    }

    // Border
    graphics.fill(x, y, x + popupWidth, y + 1, 0xFF888888);
    graphics.fill(x, y, x + 1, y + popupHeight, 0xFF888888);
    graphics.fill(x + popupWidth - 1, y, x + popupWidth, y + popupHeight, 0xFF888888);
    graphics.fill(x, y + popupHeight - 1, x + popupWidth, y + popupHeight, 0xFF888888);
}
```

#### 5.2 Tab Completion

```java
public static boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
    if (!isOpen || inputBox == null) return false;

    // Tab key - autocomplete
    if (keyCode == 258) { // TAB
        String current = inputBox.getValue();
        List<String> suggestions = getAutocompleteSuggestions(current);

        if (!suggestions.isEmpty()) {
            String completed = suggestions.get(autocompleteIndex % suggestions.size());
            inputBox.setValue(completed);
            return true;
        }
    }

    // ... existing key handling
}
```

---

### 6. Natural Language Improvements

**Priority:** Medium
**Impact:** High
**Effort:** High

#### 6.1 Intent Recognition

```java
public class IntentRecognizer {

    public enum Intent {
        BUILD, MINE, ATTACK, FOLLOW, CRAFT, GATHER, EXPLORE, UNKNOWN
    }

    public static Intent recognizeIntent(String command) {
        String lower = command.toLowerCase();

        // Pattern matching with weighted scoring
        Map<Intent, Integer> scores = new EnumMap<>(Intent.class);

        // Build intent patterns
        if (matchesAny(lower, "build", "construct", "create", "make", "place", "put"))
            scores.merge(Intent.BUILD, 3, Integer::sum);
        if (matchesAny(lower, "house", "cabin", "castle", "tower", "structure", "farm"))
            scores.merge(Intent.BUILD, 2, Integer::sum);

        // Mine intent patterns
        if (matchesAny(lower, "mine", "dig", "excavate", "gather", "collect", "get", "harvest"))
            scores.merge(Intent.MINE, 3, Integer::sum);
        if (matchesAny(lower, "ore", "diamond", "iron", "gold", "coal", "stone"))
            scores.merge(Intent.MINE, 2, Integer::sum);

        // Attack intent patterns
        if (matchesAny(lower, "attack", "kill", "fight", "hunt", "defend", "destroy"))
            scores.merge(Intent.ATTACK, 3, Integer::sum);
        if (matchesAny(lower, "mob", "monster", "hostile", "enemy", "zombie", "skeleton"))
            scores.merge(Intent.ATTACK, 2, Integer::sum);

        // Follow intent patterns
        if (matchesAny(lower, "follow", "come", "stay", "with me", "accompany"))
            scores.merge(Intent.FOLLOW, 3, Integer::sum);

        // Find highest scoring intent
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(e -> e.getValue() >= 2)
            .map(Map.Entry::getKey)
            .orElse(Intent.UNKNOWN);
    }

    private static boolean matchesAny(String text, String... patterns) {
        return Arrays.stream(patterns).anyMatch(text::contains);
    }
}
```

#### 6.2 Pre-Processing Pipeline

```java
public class CommandPreProcessor {

    public String preProcess(String rawCommand) {
        String processed = rawCommand;

        // 1. Common typo corrections
        processed = applyTypoCorrections(processed);

        // 2. Normalize whitespace
        processed = processed.replaceAll("\\s+", " ").trim();

        // 3. Expand contractions
        processed = expandContractions(processed);

        // 4. Remove filler words
        processed = removeFillerWords(processed);

        return processed;
    }

    private String applyTypoCorrections(String command) {
        Map<String, String> corrections = Map.of(
            "buld", "build",
            "mke", "make",
            "minig", "mining",
            "biuld", "build",
            "attck", "attack",
            "folow", "follow",
            "crafr", "craft",
            "diomond", "diamond",
            "ironore", "iron ore"
        );

        String corrected = command.toLowerCase();
        for (Map.Entry<String, String> entry : corrections.entrySet()) {
            corrected = corrected.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }

        return corrected;
    }

    private String expandContractions(String command) {
        return command
            .replaceAll("'re", " are")
            .replaceAll("'ve", " have")
            .replaceAll("'ll", " will")
            .replaceAll("n't", " not");
    }

    private String removeFillerWords(String command) {
        Set<String> fillers = Set.of("please", "can you", "could you", "would you",
                                     "hey", "um", "uh", "just", "pretty");

        String result = command;
        for (String filler : fillers) {
            result = result.replaceAll("(?i)\\b" + filler + "\\b\\s*", "");
        }
        return result.trim();
    }
}
```

---

### 7. Feedback Improvements

**Priority:** Medium
**Impact:** Medium
**Effort:** Low-Medium

#### 7.1 Rich Status Messages

```java
public class StatusMessageBuilder {

    public static String buildPlanningStatus(String command, boolean isBatched, int queuePosition) {
        if (isBatched) {
            return String.format("Thinking... (Position %d in queue - batching for efficiency)", queuePosition);
        }
        return "Thinking...";
    }

    public static String buildExecutionStatus(String action, int current, int total) {
        float progress = (float) current / total;
        String progressBar = getProgressBar(progress);
        return String.format("%s %s %d/%d", action, progressBar, current, total);
    }

    public static String buildQueueStatus(int queueSize, String currentAction) {
        if (queueSize == 0) {
            return String.format("Working on: %s", currentAction);
        }
        return String.format("Working on: %s (%d tasks queued)", currentAction, queueSize);
    }

    private static String getProgressBar(float progress) {
        int filled = (int) (progress * 10);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "=" : "-");
        }
        bar.append("]");
        return bar.toString();
    }
}
```

#### 7.2 Toast Notifications

```java
public class ToastNotificationManager {

    public static void showToast(CommandSourceStack source, ToastType type, String message) {
        Component component = switch (type) {
            case SUCCESS -> Component.literal(message).withStyle(s -> s.withColor(0x55FF55));
            case ERROR -> Component.literal(message).withStyle(s -> s.withColor(0xFF5555));
            case WARNING -> Component.literal(message).withStyle(s -> s.withColor(0xFFFF55));
            case INFO -> Component.literal(message).withStyle(s -> s.withColor(0x5555FF));
        };

        // Send to hotbar (brief message at top of screen)
        source.sendSuccess(() -> component, true);
    }
}
```

---

## Implementation Priority

### Phase 1: Quick Wins (1-2 weeks)
1. Enhanced error messages with recovery steps
2. Progress bars for long-running actions
3. Queue status display
4. Typo correction in commands

### Phase 2: Core Features (2-4 weeks)
1. Command suggestion engine
2. Intent recognition
3. Persistent command history
4. Command search functionality

### Phase 3: Advanced Features (4-8 weeks)
1. Real-time autocomplete
2. Fuzzy matching for all inputs
3. Command learning from history
4. Advanced natural language processing

---

## Testing Strategy

### Unit Tests
- Error message generation
- Intent recognition accuracy
- Typo correction effectiveness
- Command template matching

### Integration Tests
- End-to-end command flow
- GUI feedback rendering
- History persistence
- Suggestion relevance

### User Testing
- Command success rate
- Time to complete tasks
- User satisfaction scores
- Error recovery success rate

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Command success rate | 85%+ | % of commands that execute successfully |
| Error recovery rate | 70%+ | % of errors that user recovers from without help |
| Average planning time | <5s | Time from command to "Okay!" message |
| Suggestion acceptance | 40%+ | % of suggestions that users accept |
| User satisfaction | 4.0/5 | User survey ratings |

---

## References

**Files Analyzed:**
- `C:\Users\casey\steve\src\main\java\com\minewright\command\ForemanCommands.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\client\ForemanOfficeGUI.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\client\ForemanOverlayScreen.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\llm\TaskPlanner.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\llm\PromptBuilder.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\llm\ResponseParser.java`
- `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`

**Related Documentation:**
- `C:\Users\casey\steve\docs\COMMAND_ANALYTICS.md` - Command usage analysis
- `C:\Users\casey\steve\docs\COMMAND_ROUTING_IMPROVEMENTS.md` - Command flow optimization
- `C:\Users\casey\steve\docs\GUI_UX_ENHANCEMENTS.md` - UI improvements
