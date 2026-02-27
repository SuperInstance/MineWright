# Command UX Improvements - Implementation Guide

**Date:** 2025-02-27
**Related:** `IMPROVE_COMMAND_UX.md`
**Status:** Implementation Specifications

---

## Quick Start Implementation

### 1. Enhanced Error Handler (Priority 1)

**Create:** `C:\Users\casey\steve\src\main\java\com\minewright\command\CommandErrorHandler.java`

```java
package com.minewright.command;

import com.minewright.MineWrightMod;
import com.minewright.config.MineWrightConfig;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides helpful error messages with recovery suggestions.
 */
public class CommandErrorHandler {

    private static final Set<String> VALID_BLOCKS = Set.of(
        // Logs
        "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log",
        "dark_oak_log", "mangrove_log", "cherry_log",
        // Planks
        "oak_planks", "spruce_planks", "birch_planks", "jungle_planks",
        "acacia_planks", "dark_oak_planks", "mangrove_planks", "cherry_planks",
        // Stone
        "stone", "cobblestone", "stone_bricks", "mossy_cobblestone",
        // Ores
        "coal_ore", "iron_ore", "copper_ore", "gold_ore",
        "diamond_ore", "emerald_ore", "redstone_ore", "lapis_ore",
        // Building
        "glass", "glass_pane", "bricks", "brick_stairs", "brick_slab"
    );

    public static Component getErrorMessage(CommandError error) {
        String message = switch (error.getType()) {
            case CREW_NOT_FOUND -> buildCrewNotFoundError(error.getTarget());
            case INVALID_BLOCK_TYPE -> buildBlockTypeError(error.getTarget());
            case PLANNING_FAILED -> buildPlanningFailedError();
            case NO_RESOURCE_NEARBY -> buildResourceNotFoundError(error.getTarget());
            case API_RATE_LIMIT -> buildRateLimitError();
            case NETWORK_ERROR -> buildNetworkError();
            case UNKNOWN_ACTION -> buildUnknownActionError(error.getTarget());
            default -> buildGenericError();
        };

        return Component.literal(message);
    }

    private static String buildCrewNotFoundError(String targetName) {
        var crewManager = MineWrightMod.getCrewManager();
        List<String> availableNames = crewManager.getCrewMemberNames();

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(colorize("&cCrew not found: &e" + targetName)).append("\n");

        if (availableNames.isEmpty()) {
            sb.append("\n");
            sb.append(colorize("&7No crew members exist yet."));
            sb.append(colorize("&aCreate one with: &fspawn <name>"));
            sb.append(colorize("&7Examples:"));
            sb.append(colorize("  &f- spawn Builder"));
            sb.append(colorize("  &f- spawn Miner"));
        } else {
            sb.append("\n");
            sb.append(colorize("&7Available crew members:"));
            for (String name : availableNames) {
                sb.append(colorize("  &e- " + name));
            }
            sb.append("\n");
            sb.append(colorize("&aUse: &f" + availableNames.get(0) + " <command>"));
        }

        return sb.toString();
    }

    private static String buildBlockTypeError(String blockName) {
        String correction = suggestCorrection(blockName, VALID_BLOCKS);

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(colorize("&cInvalid block: &e" + blockName)).append("\n");
        sb.append("\n");
        sb.append(colorize("&7Did you mean: &a" + correction)).append("\n");
        sb.append("\n");
        sb.append(colorize("&7Common block types:"));
        sb.append(colorize("  &f- oak_planks, cobblestone, stone_bricks"));
        sb.append(colorize("  &f- oak_log, spruce_log, birch_log"));
        sb.append(colorize("  &f- glass, glass_pane, bricks"));
        sb.append(colorize("  &f- iron_ore, coal_ore, diamond_ore"));

        return sb.toString();
    }

    private static String buildPlanningFailedError() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(colorize("&cI couldn't understand that command.")).append("\n");
        sb.append("\n");
        sb.append(colorize("&7Try rephrasing:"));
        sb.append(colorize("  &f- &7\"&fbuild a house&7\" &7instead of \"construct\""));
        sb.append(colorize("  &f- &7\"&fget me iron&7\" &7instead of \"obtain\""));
        sb.append(colorize("  &f- &7\"&fkill mobs&7\" &7instead of \"eliminate\""));
        sb.append("\n");
        sb.append(colorize("&7Example commands:"));
        sb.append(colorize("  &f- build a wooden cabin"));
        sb.append(colorize("  &f- mine 32 iron ore"));
        sb.append(colorize("  &f- follow me"));
        sb.append(colorize("  &f- kill mobs"));

        return sb.toString();
    }

    private static String buildResourceNotFoundError(String resource) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(colorize("&cCan't find any &e" + resource + " &cnearby.")).append("\n");
        sb.append("\n");
        sb.append(colorize("&7Suggestions:"));
        sb.append(colorize("  &f- Move to a different area"));
        sb.append(colorize("  &f- Explore caves (Y < 60)"));
        sb.append(colorize("  &f- Check different biomes"));
        sb.append(colorize("  &f- Use \"pathfind\" to explore"));

        return sb.toString();
    }

    private static String buildRateLimitError() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(colorize("&eI'm thinking too fast!")).append("\n");
        sb.append(colorize("&7Please wait a moment before trying again.")).append("\n");
        sb.append("\n");
        sb.append(colorize("&7Tip: You can change the AI provider in config"));
        sb.append(colorize("  &fto use a faster service."));

        return sb.toString();
    }

    private static String buildNetworkError() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(colorize("&cConnection error")).append("\n");
        sb.append("\n");
        sb.append(colorize("&7I couldn't reach the AI service.")).append("\n");
        sb.append(colorize("&7The command will retry automatically.")).append("\n");
        sb.append("\n");
        sb.append(colorize("&7If this persists:"));
        sb.append(colorize("  &f- Check your internet connection"));
        sb.append(colorize("  &f- Verify your API key in config"));
        sb.append(colorize("  &f- Try a different AI provider"));

        return sb.toString();
    }

    private static String buildUnknownActionError(String action) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(colorize("&cUnknown action: &e" + action)).append("\n");
        sb.append("\n");
        sb.append(colorize("&7Available actions:"));
        sb.append(colorize("  &f- build: Create structures"));
        sb.append(colorize("  &f- mine: Gather resources"));
        sb.append(colorize("  &f- attack: Fight mobs"));
        sb.append(colorize("  &f- follow: Follow a player"));
        sb.append(colorize("  &f- craft: Create items"));
        sb.append(colorize("  &f- gather: Collect resources"));

        return sb.toString();
    }

    private static String buildGenericError() {
        return colorize("&cSomething went wrong. Please try again.");
    }

    /**
     * Find the closest matching valid option using Levenshtein distance.
     */
    private static String suggestCorrection(String input, Set<String> validOptions) {
        if (validOptions.contains(input)) {
            return input;
        }

        String lowerInput = input.toLowerCase().replace("_", " ");

        return validOptions.stream()
            .min(Comparator.comparingInt(opt -> {
                String lowerOpt = opt.toLowerCase().replace("_", " ");
                return levenshteinDistance(lowerInput, lowerOpt);
            }))
            .orElse("unknown");
    }

    /**
     * Calculate Levenshtein distance between two strings.
     */
    private static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1),     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
            }
        }

        return dp[a.length()][b.length()];
    }

    /**
     * Convert Minecraft color codes to Component.
     */
    private static String colorize(String message) {
        // This would need proper Component handling
        // For now, return as-is and let the chat system handle it
        return message.replaceAll("&[0-9a-fk-or]", "");
    }

    public enum ErrorType {
        CREW_NOT_FOUND,
        INVALID_BLOCK_TYPE,
        PLANNING_FAILED,
        NO_RESOURCE_NEARBY,
        API_RATE_LIMIT,
        NETWORK_ERROR,
        UNKNOWN_ACTION,
        UNKNOWN
    }

    public static class CommandError {
        private final ErrorType type;
        private final String target;

        public CommandError(ErrorType type, String target) {
            this.type = type;
            this.target = target;
        }

        public ErrorType getType() {
            return type;
        }

        public String getTarget() {
            return target;
        }
    }
}
```

### 2. Command Suggestion Engine (Priority 1)

**Create:** `C:\Users\casey\steve\src\main\java\com\minewright\command\CommandSuggestionEngine.java`

```java
package com.minewright.command;

import com.minewright.MineWrightMod;
import com.minewright.entity.ForemanEntity;
import com.minewright.memory.WorldKnowledge;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides intelligent command suggestions based on context.
 */
public class CommandSuggestionEngine {

    private static final List<CommandTemplate> TEMPLATES = List.of(
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
                "fight zombies"
            )
        ),
        new CommandTemplate(
            "follow",
            List.of("follow", "come with"),
            "follow {player}",
            List.of(
                "follow me",
                "come with me",
                "stay close"
            )
        )
    );

    /**
     * Get suggestions based on partial input and context.
     */
    public List<Suggestion> getSuggestions(ForemanEntity crew, String partialInput) {
        List<Suggestion> suggestions = new ArrayList<>();

        // 1. Fuzzy matches against templates
        suggestions.addAll(getTemplateMatches(partialInput));

        // 2. Context-aware suggestions
        suggestions.addAll(getContextualSuggestions(crew));

        // 3. Crew name completions
        suggestions.addAll(getCrewNameCompletions(partialInput));

        // Sort by relevance and return top 5
        return suggestions.stream()
            .sorted(Comparator.comparingDouble(Suggestion::getRelevance).reversed())
            .limit(5)
            .collect(Collectors.toList());
    }

    private List<Suggestion> getTemplateMatches(String input) {
        List<Suggestion> matches = new ArrayList<>();
        String lowerInput = input.toLowerCase();

        for (CommandTemplate template : TEMPLATES) {
            // Check if input matches any synonym
            for (String synonym : template.synonyms()) {
                if (lowerInput.startsWith(synonym) ||
                    levenshteinDistance(lowerInput, synonym) <= 2) {
                    matches.add(new Suggestion(
                        template.examples().get(0),
                        template.description(),
                        0.8
                    ));
                    break;
                }
            }

            // Add examples as suggestions
            for (String example : template.examples()) {
                if (example.toLowerCase().startsWith(lowerInput)) {
                    matches.add(new Suggestion(
                        example,
                        "Example: " + template.description(),
                        0.9
                    ));
                }
            }
        }

        return matches;
    }

    private List<Suggestion> getContextualSuggestions(ForemanEntity crew) {
        List<Suggestion> suggestions = new ArrayList<>();
        WorldKnowledge world = new WorldKnowledge(crew);

        // Mining suggestions based on nearby ores
        if (world.hasNearbyBlocks("iron_ore", "diamond_ore", "coal_ore")) {
            suggestions.add(new Suggestion(
                "mine nearby ores",
                "Mine resources I can see nearby",
                0.95
            ));
        }

        // Building suggestions based on inventory
        if (crew.hasBlocksInInventory()) {
            suggestions.add(new Suggestion(
                "build a house",
                "Use my inventory to build a structure",
                0.8
            ));
        }

        // Combat suggestions based on nearby hostiles
        if (world.hasNearbyHostiles()) {
            suggestions.add(new Suggestion(
                "kill mobs",
                "Defend against nearby monsters",
                0.95
            ));
        }

        // Follow suggestion if player nearby
        if (world.hasNearbyPlayers()) {
            suggestions.add(new Suggestion(
                "follow me",
                "Follow the nearest player",
                0.7
            ));
        }

        return suggestions;
    }

    private List<Suggestion> getCrewNameCompletions(String input) {
        List<Suggestion> completions = new ArrayList<>();
        String lowerInput = input.toLowerCase();

        // Extract prefix before potential crew name
        String prefix = "";
        String namePartial = input;

        if (input.matches("^(.*?\\s+)(\\w*)")) {
            prefix = input.replaceAll("^(.*?\\s+)\\w*$", "$1");
            namePartial = input.replaceAll("^.*?\\s+(\\w*)$", "$1");
        }

        List<String> crewNames = MineWrightMod.getCrewManager().getCrewMemberNames();
        for (String name : crewNames) {
            if (name.toLowerCase().startsWith(namePartial.toLowerCase())) {
                completions.add(new Suggestion(
                    prefix + name + " ",
                    "Crew member: " + name,
                    0.85
                ));
            }
        }

        return completions;
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }

    public record CommandTemplate(
        String name,
        List<String> synonyms,
        String pattern,
        List<String> examples
    ) {
        public String description() {
            return pattern;
        }
    }

    public record Suggestion(
        String text,
        String description,
        double relevance
    ) {
        public String getRelevance() {
            return String.format("%.0f%%", relevance * 100);
        }
    }
}
```

### 3. Enhanced GUI with Progress (Priority 1)

**Modify:** `C:\Users\casey\steve\src\main\java\com\minewright\client\ForemanOfficeGUI.java`

Add these methods to the ForemanOfficeGUI class:

```java
// Add these fields to the class
private static final Map<String, ActionProgress> actionProgress = new ConcurrentHashMap<>();

/**
 * Update progress for an action.
 */
public static void updateActionProgress(String crewName, String actionName,
                                       int current, int total) {
    actionProgress.put(crewName, new ActionProgress(actionName, current, total));
}

/**
 * Render progress bar for crew actions.
 */
private static void renderProgressBar(GuiGraphics graphics, int x, int y,
                                     int width, int height, ActionProgress progress) {
    if (progress == null || progress.total() == 0) return;

    float progressRatio = (float) progress.current() / progress.total();
    int progressWidth = (int) (width * progressRatio);

    // Background
    graphics.fill(x, y, x + width, y + height, 0xFF333333);

    // Progress bar with color based on completion
    int barColor = getProgressColor(progressRatio);
    graphics.fill(x, y, x + progressWidth, y + height, barColor);

    // Border
    graphics.fill(x, y, x + width, y + 1, 0xFF888888);
    graphics.fill(x, y, x + 1, y + height, 0xFF888888);
    graphics.fill(x + width - 1, y, x + width, y + height, 0xFF888888);
    graphics.fill(x, y + height - 1, x + width, y + height, 0xFF888888);

    // Label
    Minecraft mc = Minecraft.getInstance();
    String label = String.format("%s %d/%d", progress.actionName(),
                                progress.current(), progress.total());
    int textWidth = mc.font.width(label);
    graphics.drawString(mc.font, label, x + (width - textWidth) / 2, y + 2, 0xFFFFFFFF);

    // Percentage
    String percent = String.format("%.0f%%", progressRatio * 100);
    graphics.drawString(mc.font, percent, x + width - mc.font.width(percent) - 5,
                       y - 12, 0xFF55FF55);
}

private static int getProgressColor(float progress) {
    if (progress < 0.3f) return 0xFF5555FF; // Blue (starting)
    if (progress < 0.7f) return 0xFFFFAA00; // Orange (in progress)
    return 0xFF55FF55; // Green (almost done)
}

private static void renderQueueStatus(GuiGraphics graphics, int x, int y,
                                     Queue<Task> queue, String currentAction) {
    Minecraft mc = Minecraft.getInstance();
    graphics.drawString(mc.font, "Task Queue:", x, y, 0xFF00AA00);

    int yOffset = y + 15;

    // Current action
    if (currentAction != null) {
        graphics.drawString(mc.font, "Current: " + currentAction,
                           x, yOffset, 0xFF55FFFF);
        yOffset += 12;
    }

    // Queued tasks
    int index = 1;
    for (Task task : queue) {
        if (index > 3) break;

        String taskDesc = String.format("%d. %s", index,
                                       formatTaskForDisplay(task));
        graphics.drawString(mc.font, taskDesc, x, yOffset, 0xFF888888);
        yOffset += 12;
        index++;
    }

    if (queue.size() > 3) {
        graphics.drawString(mc.font,
                           String.format("... and %d more", queue.size() - 3),
                           x, yOffset, 0xFF666666);
    }
}

private static String formatTaskForDisplay(Task task) {
    return String.format("%s %s", task.getAction(),
                        task.getParameters().keySet().stream()
                            .findFirst()
                            .map(k -> task.getParameters().get(k).toString())
                            .orElse(""));
}

/**
 * Progress tracking data.
 */
public record ActionProgress(String actionName, int current, int total) {}
```

### 4. Command History Improvements (Priority 2)

**Create:** `C:\Users\casey\steve\src\main\java\com\minewright\client\CommandHistoryManager.java`

```java
package com.minewright.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.minewright.MineWrightMod;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages persistent command history with search and statistics.
 */
public class CommandHistoryManager {

    private static final String HISTORY_FILE = "config/minewright_command_history.json";
    private static final int MAX_HISTORY = 100;
    private static final Gson GSON = new Gson();

    private final Map<String, CommandEntry> history = new ConcurrentHashMap<>();
    private final List<String> sessionHistory = new ArrayList<>();

    public void loadHistory() {
        try {
            Path path = Path.of(HISTORY_FILE);
            if (Files.exists(path)) {
                String json = Files.readString(path);
                Type type = new TypeToken<Map<String, CommandEntry>>(){}.getType();
                Map<String, CommandEntry> loaded = GSON.fromJson(json, type);
                if (loaded != null) {
                    history.putAll(loaded);
                }
                MineWrightMod.LOGGER.info("Loaded {} commands from history", history.size());
            }
        } catch (IOException e) {
            MineWrightMod.LOGGER.error("Failed to load command history", e);
        }
    }

    public void saveHistory() {
        try {
            Files.createDirectories(Path.of("config"));

            // Keep only recent entries
            Map<String, CommandEntry> toSave = history.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(
                    Comparator.comparing(CommandEntry::lastUsed).reversed()))
                .limit(MAX_HISTORY)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));

            String json = GSON.toJson(toSave);
            Files.writeString(Path.of(HISTORY_FILE), json);
        } catch (IOException e) {
            MineWrightMod.LOGGER.error("Failed to save command history", e);
        }
    }

    public void addCommand(String command, boolean success, long executionTimeMs) {
        String key = command.toLowerCase().trim();

        CommandEntry existing = history.get(key);
        CommandEntry entry = existing != null
            ? existing.incrementUse(success, executionTimeMs)
            : new CommandEntry(command, success, executionTimeMs);

        history.put(key, entry);
        sessionHistory.add(command);

        // Trim history if needed
        if (history.size() > MAX_HISTORY) {
            history.entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().lastUsed()))
                .ifPresent(toRemove -> history.remove(toRemove.getKey()));
        }
    }

    public List<String> search(String query) {
        String lowerQuery = query.toLowerCase();

        return history.values().stream()
            .filter(entry -> entry.command().toLowerCase().contains(lowerQuery))
            .sorted(Comparator.comparing(CommandEntry::useCount).reversed())
            .map(CommandEntry::command)
            .limit(10)
            .collect(Collectors.toList());
    }

    public List<String> getMostUsed(int count) {
        return history.values().stream()
            .sorted(Comparator.comparing(CommandEntry::useCount).reversed())
            .limit(count)
            .map(CommandEntry::command)
            .collect(Collectors.toList());
    }

    public List<String> getRecentCommands(int count) {
        return history.values().stream()
            .sorted(Comparator.comparing(CommandEntry::lastUsed).reversed())
            .limit(count)
            .map(CommandEntry::command)
            .collect(Collectors.toList());
    }

    public List<String> getSessionHistory() {
        return new ArrayList<>(sessionHistory);
    }

    public record CommandEntry(
        String command,
        int useCount,
        int successCount,
        long totalExecutionTime,
        Instant lastUsed
    ) {
        public CommandEntry(String command, boolean success, long executionTime) {
            this(command, 1, success ? 1 : 0, executionTime, Instant.now());
        }

        public CommandEntry incrementUse(boolean success, long executionTime) {
            return new CommandEntry(
                command,
                useCount + 1,
                successCount + (success ? 1 : 0),
                totalExecutionTime + executionTime,
                Instant.now()
            );
        }

        public double getSuccessRate() {
            return useCount == 0 ? 0 : (double) successCount / useCount;
        }

        public double getAverageExecutionTime() {
            return useCount == 0 ? 0 : (double) totalExecutionTime / useCount;
        }
    }
}
```

### 5. Intent Recognition (Priority 2)

**Create:** `C:\Users\casey\steve\src\main\java\com\minewright\command\IntentRecognizer.java`

```java
package com.minewright.command;

import java.util.*;

/**
 * Recognizes user intent from natural language commands.
 */
public class IntentRecognizer {

    public enum Intent {
        BUILD, MINE, ATTACK, FOLLOW, CRAFT, GATHER, EXPLORE, UNKNOWN
    }

    public enum Target {
        SELF, PLAYER, CREW, ALL_CREW, LOCATION, ENTITY, UNKNOWN
    }

    public static class RecognizedIntent {
        private final Intent intent;
        private final Target target;
        private final String targetName;
        private final Map<String, Object> parameters;
        private final double confidence;

        public RecognizedIntent(Intent intent, Target target, String targetName,
                               Map<String, Object> parameters, double confidence) {
            this.intent = intent;
            this.target = target;
            this.targetName = targetName;
            this.parameters = parameters;
            this.confidence = confidence;
        }

        public Intent getIntent() { return intent; }
        public Target getTarget() { return target; }
        public String getTargetName() { return targetName; }
        public Map<String, Object> getParameters() { return parameters; }
        public double getConfidence() { return confidence; }

        public boolean isHighConfidence() {
            return confidence >= 0.7;
        }
    }

    /**
     * Recognize intent from a natural language command.
     */
    public static RecognizedIntent recognize(String command) {
        String lower = command.toLowerCase();

        // Score each intent
        Map<Intent, Double> scores = new EnumMap<>(Intent.class);

        scores.put(Intent.BUILD, scoreBuildIntent(lower));
        scores.put(Intent.MINE, scoreMineIntent(lower));
        scores.put(Intent.ATTACK, scoreAttackIntent(lower));
        scores.put(Intent.FOLLOW, scoreFollowIntent(lower));
        scores.put(Intent.CRAFT, scoreCraftIntent(lower));
        scores.put(Intent.GATHER, scoreGatherIntent(lower));
        scores.put(Intent.EXPLORE, scoreExploreIntent(lower));

        // Find highest scoring intent
        Intent bestIntent = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(e -> e.getValue() >= 0.3)
            .map(Map.Entry::getKey)
            .orElse(Intent.UNKNOWN);

        double confidence = scores.getOrDefault(bestIntent, 0.0);

        // Extract target
        Target target = recognizeTarget(lower);
        String targetName = extractTargetName(lower, target);

        // Extract parameters
        Map<String, Object> params = extractParameters(lower, bestIntent);

        return new RecognizedIntent(bestIntent, target, targetName, params, confidence);
    }

    private static double scoreBuildIntent(String command) {
        double score = 0;

        // Build verbs
        if (matchesAny(command, "build", "construct", "create", "make", "place", "put"))
            score += 0.4;

        // Structure nouns
        if (matchesAny(command, "house", "cabin", "castle", "tower", "structure",
                       "farm", "barn", "shelter", "home"))
            score += 0.3;

        // Block types
        if (matchesAny(command, "wood", "stone", "brick", "block", "plank"))
            score += 0.2;

        // Size indicators
        if (matchesAny(command, "big", "small", "large", "huge"))
            score += 0.1;

        return Math.min(score, 1.0);
    }

    private static double scoreMineIntent(String command) {
        double score = 0;

        // Mining verbs
        if (matchesAny(command, "mine", "dig", "excavate", "gather", "collect",
                       "get", "harvest", "extract"))
            score += 0.4;

        // Ore types
        if (matchesAny(command, "ore", "diamond", "iron", "gold", "coal",
                       "stone", "copper", "emerald", "redstone"))
            score += 0.4;

        // Direction
        if (matchesAny(command, "down", "under", "below", "deep"))
            score += 0.1;

        // Quantity
        if (command.matches(".*\\d+.*"))
            score += 0.1;

        return Math.min(score, 1.0);
    }

    private static double scoreAttackIntent(String command) {
        double score = 0;

        // Combat verbs
        if (matchesAny(command, "attack", "kill", "fight", "hunt", "defend",
                       "destroy", "eliminate", "stop"))
            score += 0.4;

        // Target types
        if (matchesAny(command, "mob", "monster", "hostile", "enemy",
                       "zombie", "skeleton", "creeper", "spider"))
            score += 0.4;

        // Aggressive language
        if (matchesAny(command, "murder", "destroy", "annihilate"))
            score += 0.2;

        return Math.min(score, 1.0);
    }

    private static double scoreFollowIntent(String command) {
        double score = 0;

        // Follow verbs
        if (matchesAny(command, "follow", "come", "stay", "accompany", "join"))
            score += 0.5;

        // Self-reference
        if (matchesAny(command, "me", "myself"))
            score += 0.3;

        // Player reference
        if (matchesAny(command, "player", "you"))
            score += 0.2;

        return Math.min(score, 1.0);
    }

    private static double scoreCraftIntent(String command) {
        double score = 0;

        if (matchesAny(command, "craft", "make", "create", "forge", "smith"))
            score += 0.5;

        if (matchesAny(command, "sword", "pickaxe", "axe", "tool", "armor",
                       "helmet", "chestplate", "leggings", "boots"))
            score += 0.4;

        return Math.min(score, 1.0);
    }

    private static double scoreGatherIntent(String command) {
        double score = 0;

        if (matchesAny(command, "gather", "collect", "harvest", "pick", "chop"))
            score += 0.4;

        if (matchesAny(command, "wood", "log", "tree", "food", "crop", "wheat"))
            score += 0.4;

        return Math.min(score, 1.0);
    }

    private static double scoreExploreIntent(String command) {
        double score = 0;

        if (matchesAny(command, "explore", "find", "search", "look", "scout"))
            score += 0.5;

        if (matchesAny(command, "village", "biome", "structure", "ruins"))
            score += 0.3;

        return Math.min(score, 1.0);
    }

    private static Target recognizeTarget(String command) {
        if (matchesAny(command, "all crew", "everyone", "everybody"))
            return Target.ALL_CREW;

        if (matchesAny(command, "crew", "foreman", "worker"))
            return Target.CREW;

        if (matchesAny(command, "me", "myself"))
            return Target.PLAYER;

        if (matchesAny(command, "here", "there", "this", "that"))
            return Target.LOCATION;

        return Target.UNKNOWN;
    }

    private static String extractTargetName(String command, Target target) {
        if (target == Target.CREW || target == Target.ALL_CREW) {
            // Extract crew names from command
            String[] words = command.split("\\s+");
            for (String word : words) {
                if (word.length() > 2 && !word.matches("(the|a|an|to|for|with)")) {
                    return word;
                }
            }
        }
        return null;
    }

    private static Map<String, Object> extractParameters(String command, Intent intent) {
        Map<String, Object> params = new HashMap<>();

        // Extract quantity
        if (command.matches(".*\\d+.*")) {
            String[] numbers = command.replaceAll("\\D+", " ").trim().split("\\s+");
            if (numbers.length > 0 && !numbers[0].isEmpty()) {
                try {
                    params.put("quantity", Integer.parseInt(numbers[0]));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Extract target type based on intent
        switch (intent) {
            case BUILD -> {
                if (matchesAny(command, "wood", "oak", "birch"))
                    params.put("material", "wood");
                else if (matchesAny(command, "stone", "cobble"))
                    params.put("material", "stone");
            }
            case MINE -> {
                if (matchesAny(command, "diamond"))
                    params.put("resource", "diamond");
                else if (matchesAny(command, "iron"))
                    params.put("resource", "iron");
                else if (matchesAny(command, "gold"))
                    params.put("resource", "gold");
                else if (matchesAny(command, "coal"))
                    params.put("resource", "coal");
            }
        }

        return params;
    }

    private static boolean matchesAny(String text, String... patterns) {
        return Arrays.stream(patterns).anyMatch(text::contains);
    }
}
```

---

## Integration Steps

### Step 1: Update ActionExecutor

Modify `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java`:

```java
// Add to processNaturalLanguageCommand method
public void processNaturalLanguageCommand(String command) {
    MineWrightMod.LOGGER.info("Foreman '{}' processing command (async): {}",
                             foreman.getSteveName(), command);

    if (isPlanning) {
        MineWrightMod.LOGGER.warn("Foreman '{}' is already planning",
                                 foreman.getSteveName());
        sendToGUI(foreman.getSteveName(),
                 "Hold on, I'm still thinking about the previous command...");
        return;
    }

    // Pre-process command
    String processed = CommandPreProcessor.preProcess(command);

    // Recognize intent for better error messages
    IntentRecognizer.RecognizedIntent intent =
        IntentRecognizer.recognize(processed);

    if (!intent.isHighConfidence()) {
        // Provide suggestions based on partial intent
        List<Suggestion> suggestions =
            CommandSuggestionEngine.getSuggestions(foreman, processed);
        if (!suggestions.isEmpty()) {
            sendToGUI(foreman.getSteveName(),
                     "I'm not sure. Did you mean: " +
                     suggestions.get(0).text() + "?");
        }
    }

    // ... rest of existing code
}

// Add to executeTask method
private void executeTask(Task task) {
    MineWrightMod.LOGGER.info("Foreman '{}' executing task: {}",
                             foreman.getSteveName(), task);

    currentAction = createAction(task);

    if (currentAction == null) {
        CommandErrorHandler.CommandError error =
            new CommandErrorHandler.CommandError(
                CommandErrorHandler.ErrorType.UNKNOWN_ACTION,
                task.getAction()
            );
        Component message = CommandErrorHandler.getErrorMessage(error);
        foreman.sendChatMessage(message);
        return;
    }

    currentAction.start();
}
```

### Step 2: Update ForemanOfficeGUI

Add to the render method in ForemanOfficeGUI:

```java
// After rendering messages, add progress bars
var crewManager = MineWrightMod.getCrewManager();
int progressY = inputAreaY - 50;

for (String crewName : crewManager.getCrewMemberNames()) {
    ForemanEntity crew = crewManager.getCrewMember(crewName);
    if (crew != null) {
        ActionExecutor executor = crew.getActionExecutor();

        // Render progress bar if executing
        if (executor.isExecuting()) {
            ActionProgress progress =
                actionProgress.get(crewName);
            if (progress != null) {
                renderProgressBar(graphics, panelX + PANEL_PADDING,
                                progressY, PANEL_WIDTH - 30, 15, progress);
                progressY -= 25;
            }
        }
    }
}
```

### Step 3: Hook up history manager

Add to ClientSetup or main mod initialization:

```java
private static CommandHistoryManager historyManager;

public static void init() {
    historyManager = new CommandHistoryManager();
    historyManager.loadHistory();
}

public static void shutdown() {
    if (historyManager != null) {
        historyManager.saveHistory();
    }
}
```

---

## Testing Checklist

- [ ] Error messages display correctly with color coding
- [ ] Fuzzy matching suggests correct alternatives
- [ ] Progress bars update during long actions
- [ ] Command history persists across sessions
- [ ] Intent recognition accurately identifies commands
- [ ] Suggestions are contextually relevant
- [ ] Autocomplete completes crew names correctly
- [ ] Queue status shows accurate information
- [ ] Recovery steps lead to successful command execution

---

## Performance Considerations

1. **Fuzzy Matching**: Cache common corrections to avoid repeated calculations
2. **History**: Limit search to recent entries for performance
3. **Suggestions**: Only calculate when user pauses typing (debounce 300ms)
4. **Progress**: Update progress bars every 10 ticks, not every tick

---

## Future Enhancements

1. **Machine Learning**: Train on successful commands to improve suggestions
2. **Voice Integration**: Use voice recognition for hands-free commands
3. **Multiplayer**: Suggest commands based on what other players are doing
4. **Analytics**: Track command patterns to suggest optimizations
5. **Macro System**: Allow users to create command macros
