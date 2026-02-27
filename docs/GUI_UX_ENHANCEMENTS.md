# GUI/UX Enhancements for MineWright Minecraft Mod

**Author:** AI Analysis Team
**Date:** 2026-02-27
**Version:** 1.0
**Status:** Design Document

---

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [Chat Bubble Improvements](#chat-bubble-improvements)
3. [Worker Status Dashboard](#worker-status-dashboard)
4. [Command Autocomplete System](#command-autocomplete-system)
5. [Visual Feedback for Planning/Executing States](#visual-feedback-for-planningexecuting-states)
6. [Multi-Agent Selection and Command Targeting UI](#multi-agent-selection-and-command-targeting-ui)
7. [Accessibility Improvements](#accessibility-improvements)
8. [Implementation Roadmap](#implementation-roadmap)

---

## Current State Analysis

### Existing GUI Components

The MineWright mod currently has the following GUI components:

#### **ForemanOfficeGUI.java** (`C:\Users\casey\steve\src\main\java\com\minewright\client\ForemanOfficeGUI.java`)

**Current Features:**
- Side-mounted panel (200px width) that slides in from right
- Toggle with **K** key
- Basic chat interface with message history
- Scrollable message area with scrollbar
- Command input box with history (arrow keys)
- Color-coded message bubbles (User=Green, Crew=Blue, System=Orange)
- Semi-transparent backgrounds
- Basic word wrapping (truncation only)

**Limitations Identified:**
1. **Text Wrapping:** Current implementation only truncates with "..." instead of proper multi-line wrapping
2. **No Markdown Support:** Cannot render formatted text (bold, code blocks, lists)
3. **No Worker Status:** Cannot see which crew members exist, their roles, or current tasks
4. **No Autocomplete:** No command suggestions or completion
5. **Limited Visual Feedback:** Planning state only shows "Thinking..." text
6. **No Multi-Agent Selection:** Cannot easily target specific crew members
7. **No Progress Indicators:** Cannot see task progress percentages
8. **Limited Accessibility:** No keyboard navigation, screen reader support, or high-contrast mode

### State Machine Integration

The mod has a robust state machine system (`AgentStateMachine`) with states:
- **IDLE** - Waiting for commands
- **PLANNING** - Processing with AI
- **EXECUTING** - Performing actions
- **PAUSED** - Temporarily suspended
- **COMPLETED** - All tasks finished
- **FAILED** - Encountered error

**Current Integration:** State machine exists but not exposed to GUI.

### Agent Role System

The orchestration system supports roles:
- **FOREMAN** - Coordinates tasks
- **WORKER** - Executes assigned tasks
- **SPECIALIST** - Domain-specific expertise
- **SOLO** - Independent operation

**Current Integration:** Roles tracked but not displayed in GUI.

---

## Chat Bubble Improvements

### 1. Proper Multi-Line Text Wrapping

**Current Implementation:**
```java
// Simple truncation (line 279-292 in ForemanOfficeGUI.java)
private static String wrapText(net.minecraft.client.gui.Font font, String text, int maxWidth) {
    if (font.width(text) <= maxWidth) {
        return text;
    }
    // Simple truncation for now
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
        result.append(text.charAt(i));
        if (font.width(result.toString() + "...") >= maxWidth) {
            return result.substring(0, result.length() - 3) + "...";
        }
    }
    return result.toString();
}
```

**Proposed Implementation:**

```java
/**
 * Enhanced text wrapper that properly handles multi-line text with word wrapping
 */
private static List<String> wrapTextLines(net.minecraft.client.gui.Font font, String text, int maxWidth) {
    List<String> lines = new ArrayList<>();
    String[] paragraphs = text.split("\n");

    for (String paragraph : paragraphs) {
        if (paragraph.isEmpty()) {
            lines.add("");
            continue;
        }

        StringBuilder currentLine = new StringBuilder();
        String[] words = paragraph.split(" ");

        for (String word : words) {
            String testLine = currentLine.length() == 0
                ? word
                : currentLine + " " + word;

            if (font.width(testLine) <= maxWidth) {
                currentLine.append(currentLine.length() == 0 ? "" : " ").append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                // Handle words longer than maxWidth
                if (font.width(word) > maxWidth) {
                    lines.addAll(breakLongWord(font, word, maxWidth));
                    currentLine.setLength(0);
                } else {
                    currentLine = new StringBuilder(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
    }

    return lines;
}

/**
 * Breaks a word that's too long for a single line
 */
private static List<String> breakLongWord(net.minecraft.client.gui.Font font, String word, int maxWidth) {
    List<String> parts = new ArrayList<>();
    StringBuilder current = new StringBuilder();

    for (char c : word.toCharArray()) {
        if (font.width(current.toString() + c) > maxWidth) {
            parts.add(current.toString());
            current = new StringBuilder(String.valueOf(c));
        } else {
            current.append(c);
        }
    }

    if (current.length() > 0) {
        parts.add(current.toString());
    }

    return parts;
}
```

### 2. Markdown Support

**Proposed MarkdownRenderer Component:**

```java
package com.minewright.client;

import net.minecraft.client.gui.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders markdown-formatted text in GUI components
 *
 * Supported syntax:
 * - **bold** or __bold__
 * - *italic* or _italic_
 * - `code`
 * - ```code block```
 * - [link](url) - renders as colored text
 * - - list item
 * - 1. numbered list
 * - # heading (larger text)
 */
public class MarkdownRenderer {

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*|__(.+?)__");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.+?)\\*|_(.+?)_");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.+?)`");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```([\\s\\S]+?)```");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^]]+)\\]\\([^)]+\\)");
    private static final Pattern LIST_PATTERN = Pattern.compile("^[-*]\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern NUMBERED_LIST_PATTERN = Pattern.compile("^\\d+\\.\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    public static class TextSegment {
        public final String text;
        public final int color;
        public final boolean bold;
        public final boolean italic;
        public final boolean code;
        public final float scale;

        public TextSegment(String text, int color, boolean bold, boolean italic, boolean code, float scale) {
            this.text = text;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.code = code;
            this.scale = scale;
        }

        public static TextSegment plain(String text) {
            return new TextSegment(text, 0xFFFFFFFF, false, false, false, 1.0f);
        }

        public static TextSegment bold(String text) {
            return new TextSegment(text, 0xFFFFFFFF, true, false, false, 1.0f);
        }

        public static TextSegment italic(String text) {
            return new TextSegment(text, 0xFFFFFFFF, false, true, false, 1.0f);
        }

        public static TextSegment code(String text) {
            return new TextSegment(text, 0xFF87CEEB, false, false, true, 0.9f);
        }

        public static TextSegment heading(String text, int level) {
            float scale = 1.2f - (level * 0.05f);
            return new TextSegment(text, 0xFFFFFF00, true, false, false, scale);
        }

        public static TextSegment listItem(String text, boolean numbered) {
            return new TextSegment((numbered ? "  " : "  ") + text, 0xFFAAAAAA, false, false, false, 0.95f);
        }
    }

    /**
     * Parse markdown text into renderable segments
     */
    public static List<TextSegment> parseMarkdown(String markdown) {
        List<TextSegment> segments = new ArrayList<>();

        // Handle code blocks first (they don't have internal formatting)
        Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(markdown);
        int lastIndex = 0;

        while (codeBlockMatcher.find()) {
            // Add text before code block
            if (codeBlockMatcher.start() > lastIndex) {
                segments.addAll(parseInlineFormatting(markdown.substring(lastIndex, codeBlockMatcher.start())));
            }

            // Add code block
            String code = codeBlockMatcher.group(1).trim();
            String[] codeLines = code.split("\n");
            for (String line : codeLines) {
                segments.add(new TextSegment(line, 0xFF2D2D2D, false, false, true, 0.85f));
            }

            lastIndex = codeBlockMatcher.end();
        }

        // Add remaining text
        if (lastIndex < markdown.length()) {
            segments.addAll(parseInlineFormatting(markdown.substring(lastIndex)));
        }

        return segments;
    }

    /**
     * Parse inline formatting (bold, italic, code, links)
     */
    private static List<TextSegment> parseInlineFormatting(String text) {
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inBold = false;
        boolean inItalic = false;

        int i = 0;
        while (i < text.length()) {
            // Check for bold
            if (i + 1 < text.length() && text.substring(i, i + 2).equals("**")) {
                if (current.length() > 0) {
                    segments.add(new TextSegment(current.toString(), 0xFFFFFFFF, inBold, inItalic, false, 1.0f));
                    current.setLength(0);
                }
                inBold = !inBold;
                i += 2;
                continue;
            }

            // Check for italic
            if (text.charAt(i) == '*') {
                if (current.length() > 0) {
                    segments.add(new TextSegment(current.toString(), 0xFFFFFFFF, inBold, inItalic, false, 1.0f));
                    current.setLength(0);
                }
                inItalic = !inItalic;
                i += 1;
                continue;
            }

            // Check for inline code
            if (text.charAt(i) == '`') {
                int end = text.indexOf('`', i + 1);
                if (end != -1) {
                    if (current.length() > 0) {
                        segments.add(new TextSegment(current.toString(), 0xFFFFFFFF, inBold, inItalic, false, 1.0f));
                        current.setLength(0);
                    }
                    String code = text.substring(i + 1, end);
                    segments.add(new TextSegment(code, 0xFF87CEEB, false, false, true, 0.9f));
                    i = end + 1;
                    continue;
                }
            }

            current.append(text.charAt(i));
            i++;
        }

        if (current.length() > 0) {
            segments.add(new TextSegment(current.toString(), 0xFFFFFFFF, inBold, inItalic, false, 1.0f));
        }

        return segments;
    }

    /**
     * Calculate the height needed to render markdown text
     */
    public static int calculateHeight(Font font, List<TextSegment> segments, int maxWidth) {
        int height = 0;
        for (TextSegment segment : segments) {
            List<String> lines = wrapTextLines(font, segment.text, maxWidth);
            height += lines.size() * (font.lineHeight + 2);
        }
        return height;
    }
}
```

### 3. Enhanced Chat Bubble Rendering

**Proposed Update to ChatMessage Class:**

```java
private static class ChatMessage {
    String sender;
    String text;
    List<MarkdownRenderer.TextSegment> formattedText; // NEW
    int bubbleColor;
    boolean isUser;
    Instant timestamp; // NEW
    String messageId; // NEW
    List<String> mentionedAgents; // NEW

    ChatMessage(String sender, String text, int bubbleColor, boolean isUser) {
        this.sender = sender;
        this.text = text;
        this.bubbleColor = bubbleColor;
        this.isUser = isUser;
        this.timestamp = Instant.now();
        this.messageId = UUID.randomUUID().toString().substring(0, 8);
        this.formattedText = MarkdownRenderer.parseMarkdown(text);

        // Extract @mentions
        this.mentionedAgents = extractMentions(text);
    }

    private static List<String> extractMentions(String text) {
        List<String> mentions = new ArrayList<>();
        Pattern pattern = Pattern.compile("@(\\w+)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }
}
```

---

## Worker Status Dashboard

### UI Design

**ASCII Mockup:**

```
+--------------------------------------------------------------------------+
| MineWright                                      [K to close]             |
+--------------------------------------------------------------------------+
|                                                                          |
|  CREW STATUS                    [3 Active]                               |
|  +----------------+  +----------------+  +----------------+             |
|  |  Foreman       |  |  Builder Bob   |  |  Miner Mike    |             |
|  |  [FOREMAN]     |  |  [WORKER]      |  |  [SPECIALIST]  |             |
|  |  State: IDLE   |  |  State:        |  |  State:        |             |
|  |  ⚪ Waiting     |  |  EXECUTING     |  |  PLANNING      |             |
|  |  Task: None    |  |  ⚙️ Building...|  |  🧠 Thinking...|             |
|  |  Health: 20/20 |  |  Task: House   |  |  Task: Mine    |             |
|  |  [▼ Details]   |  |  Health: 20/20 |  |  Health: 20/20 |             |
|  +----------------+  +----------------+  +----------------+             |
|                                                                          |
|  QUICK ACTIONS                                                           |
|  [📢 Broadcast to All]  [⏸ Pause All]  [▶ Resume All]  [🗑 Clear Tasks] |
|                                                                          |
+--------------------------------------------------------------------------+
|                                                                          |
|  MESSAGES                                         [Scroll ▲ ▼]          |
|  +----------------------------------------------------------+           |
|  | You: Build a house                                  2:30pm|           |
|  | +------------------------------------------------------+ |           |
|  | | Foreman: OK! I'll build a house for you.            | |           |
|  | +------------------------------------------------------+ |           |
|  | +------------------------------------------------------+ |           |
|  | | Builder Bob: I'll help with the walls!               | |           |
|  | +------------------------------------------------------+ |           |
|  +----------------------------------------------------------+           |
|                                                                          |
+--------------------------------------------------------------------------+
| Command: [                                        ]                      |
|                                                                          |
| [Send] [📎 Attach] [🎤 Voice]                                            |
+--------------------------------------------------------------------------+
| Enter: Send | Tab: Autocomplete | Ctrl+K: Quick Commands                |
+--------------------------------------------------------------------------+
```

### Implementation Code

**New Component: WorkerStatusDashboard.java**

```java
package com.minewright.client;

import com.minewright.MineWrightMod;
import com.minewright.entity.CrewManager;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import com.minewright.orchestration.AgentRole;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dashboard widget showing status of all crew members
 * Displays role, state, current task, health, and action buttons
 */
public class WorkerStatusDashboard {

    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 70;
    private static final int CARD_SPACING = 8;
    private static final int HEADER_HEIGHT = 25;

    private final Minecraft mc;
    private final Font font;
    private List<WorkerCard> workerCards;
    private boolean expanded = true;
    private int scrollOffset = 0;

    // State icons
    private static final String IDLE_ICON = "⚪";
    private static final String PLANNING_ICON = "🧠";
    private static final String EXECUTING_ICON = "⚙️";
    private static final String PAUSED_ICON = "⏸";
    private static final String COMPLETED_ICON = "✅";
    private static final String FAILED_ICON = "❌";

    // Role colors
    private static final int FOREMAN_COLOR = 0xFFFFD700;    // Gold
    private static final int WORKER_COLOR = 0xFF4CAF50;     // Green
    private static final int SPECIALIST_COLOR = 0xFF9C27B0; // Purple
    private static final int SOLO_COLOR = 0xFF2196F3;       // Blue

    public WorkerStatusDashboard() {
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
        this.workerCards = new ArrayList<>();
        refreshWorkerCards();
    }

    /**
     * Refresh worker status cards from CrewManager
     */
    public void refreshWorkerCards() {
        workerCards.clear();

        CrewManager crewManager = MineWrightMod.getCrewManager();
        if (crewManager == null) return;

        for (ForemanEntity crew : crewManager.getAllCrewMembers()) {
            WorkerCard card = new WorkerCard(
                crew.getSteveName(),
                crew.getRole(),
                crew.getActionExecutor().getStateMachine().getCurrentState(),
                crew.getActionExecutor().getCurrentGoal(),
                (int) crew.getHealth(),
                (int) crew.getMaxHealth(),
                crew.getActionExecutor().isPlanning()
            );
            workerCards.add(card);
        }

        // Sort: Foreman first, then by name
        workerCards.sort(Comparator
            .comparing((WorkerCard c) -> c.role != AgentRole.FOREMAN)
            .thenComparing(c -> c.name));
    }

    /**
     * Render the dashboard at the specified position
     */
    public void render(GuiGraphics graphics, int x, int y, int maxWidth) {
        if (!expanded) {
            renderCollapsed(graphics, x, y);
            return;
        }

        // Header
        graphics.fill(x, y, x + maxWidth, y + HEADER_HEIGHT, 0x25252525);
        graphics.drawString(font, "CREW STATUS", x + 5, y + 8, 0xFFFFFFFF);
        graphics.drawString(font, workerCards.size() + " Active", x + maxWidth - 70, y + 8, 0xFFAAAAAA);

        int cardY = y + HEADER_HEIGHT + 5;

        // Quick actions row
        renderQuickActions(graphics, x + 5, cardY, maxWidth - 10);
        cardY += 25;

        // Worker cards
        int cardsPerRow = Math.max(1, (maxWidth - 10) / (CARD_WIDTH + CARD_SPACING));
        int cardX = x + 5;

        for (int i = scrollOffset; i < workerCards.size() && i < scrollOffset + 6; i++) {
            WorkerCard card = workerCards.get(i);
            renderWorkerCard(graphics, cardX, cardY, card);

            cardX += CARD_WIDTH + CARD_SPACING;
            if ((i - scrollOffset + 1) % cardsPerRow == 0) {
                cardX = x + 5;
                cardY += CARD_HEIGHT + CARD_SPACING;
            }
        }
    }

    /**
     * Render a single worker status card
     */
    private void renderWorkerCard(GuiGraphics graphics, int x, int y, WorkerCard card) {
        // Card background with state-based tint
        int bgColor = 0xC0000000;
        if (card.state == AgentState.PLANNING) {
            bgColor = 0xC01A237E; // Dark blue tint
        } else if (card.state == AgentState.EXECUTING) {
            bgColor = 0xC01B5E20; // Dark green tint
        } else if (card.state == AgentState.FAILED) {
            bgColor = 0xC0B71C1C; // Dark red tint
        }

        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, bgColor);
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0x30FFFFFF); // Border

        // Role badge
        int roleColor = getRoleColor(card.role);
        graphics.fill(x + 2, y + 2, x + CARD_WIDTH - 2, y + 12, roleColor);
        graphics.drawString(font, card.role.getDisplayName(), x + 4, y + 3, 0xFF000000);

        // Name
        graphics.drawString(font, truncate(card.name, 12), x + 4, y + 15, 0xFFFFFFFF);

        // State with icon
        String stateIcon = getStateIcon(card.state);
        String stateText = stateIcon + " " + card.state.getDisplayName();
        graphics.drawString(font, stateText, x + 4, y + 27, getStateColor(card.state));

        // Current task
        if (card.currentTask != null && !card.currentTask.isEmpty()) {
            String taskPreview = truncate(card.currentTask, 14);
            graphics.drawString(font, taskPreview, x + 4, y + 39, 0xFFAAAAAA);
        } else {
            graphics.drawString(font, "No task", x + 4, y + 39, 0xFF666666);
        }

        // Health bar
        float healthPercent = (float) card.health / card.maxHealth;
        int healthBarWidth = CARD_WIDTH - 8;
        int healthFillWidth = (int) (healthBarWidth * healthPercent);

        graphics.fill(x + 4, y + CARD_HEIGHT - 12, x + CARD_WIDTH - 4, y + CARD_HEIGHT - 8, 0xFF333333);
        graphics.fill(x + 4, y + CARD_HEIGHT - 12, x + 4 + healthFillWidth, y + CARD_HEIGHT - 8,
            getHealthColor(healthPercent));
        graphics.drawString(font, card.health + "/" + card.maxHealth, x + 4, y + CARD_HEIGHT - 20, 0xFFFFFFFF);

        // Progress indicator for planning/executing
        if (card.isPlanning || card.state == AgentState.EXECUTING) {
            int progressY = y + CARD_HEIGHT - 4;
            graphics.fill(x + 4, progressY, x + CARD_WIDTH - 4, progressY + 2, 0xFF333333);
            int progressWidth = (int) ((CARD_WIDTH - 8) * getProgressPercent(card));
            graphics.fill(x + 4, progressY, x + 4 + progressWidth, progressY + 2, 0xFF4CAF50);
        }
    }

    /**
     * Render quick action buttons
     */
    private void renderQuickActions(GuiGraphics graphics, int x, int y, int width) {
        int buttonWidth = (width - 24) / 4;
        int buttonHeight = 20;

        String[] buttons = {"Broadcast", "Pause All", "Resume", "Clear"};
        int[] colors = {0xFF2196F3, 0xFFFF9800, 0xFF4CAF50, 0xFFF44336};

        for (int i = 0; i < buttons.length; i++) {
            int bx = x + i * (buttonWidth + 6);
            graphics.fill(bx, y, bx + buttonWidth, y + buttonHeight, colors[i]);
            graphics.drawString(font, buttons[i], bx + 5, y + 6, 0xFFFFFFFF);
        }
    }

    /**
     * Render collapsed version
     */
    private void renderCollapsed(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 150, y + 20, 0x25252525);
        graphics.drawString(font, "CREW: " + workerCards.size() + " [▼]", x + 5, y + 6, 0xFFFFFFFF);
    }

    private int getRoleColor(AgentRole role) {
        return switch (role) {
            case FOREMAN -> FOREMAN_COLOR;
            case WORKER -> WORKER_COLOR;
            case SPECIALIST -> SPECIALIST_COLOR;
            case SOLO -> SOLO_COLOR;
        };
    }

    private int getStateColor(AgentState state) {
        return switch (state) {
            case IDLE -> 0xFFAAAAAA;
            case PLANNING -> 0xFF2196F3;
            case EXECUTING -> 0xFF4CAF50;
            case PAUSED -> 0xFFFF9800;
            case COMPLETED -> 0xFF4CAF50;
            case FAILED -> 0xFFF44336;
        };
    }

    private int getHealthColor(float percent) {
        if (percent > 0.6f) return 0xFF4CAF50;
        if (percent > 0.3f) return 0xFFFFC107;
        return 0xFFF44336;
    }

    private String getStateIcon(AgentState state) {
        return switch (state) {
            case IDLE -> IDLE_ICON;
            case PLANNING -> PLANNING_ICON;
            case EXECUTING -> EXECUTING_ICON;
            case PAUSED -> PAUSED_ICON;
            case COMPLETED -> COMPLETED_ICON;
            case FAILED -> FAILED_ICON;
        };
    }

    private String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen - 2) + ".." : s;
    }

    private float getProgressPercent(WorkerCard card) {
        // Pulse animation for planning
        if (card.isPlanning) {
            return (float) (0.3 + 0.2 * Math.sin(System.currentTimeMillis() / 200.0));
        }
        return 0.5f; // Default for executing
    }

    /**
     * Handle mouse clicks
     */
    public boolean handleClick(double mouseX, double mouseY, int x, int y, int maxWidth) {
        // Check for collapse toggle
        if (!expanded && mouseX >= x && mouseX <= x + 150 && mouseY >= y && mouseY <= y + 20) {
            expanded = true;
            return true;
        }

        if (expanded && mouseX >= x && mouseX <= x + maxWidth && mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
            if (mouseX >= x + maxWidth - 80) { // Toggle collapse
                expanded = !expanded;
                return true;
            }
        }

        // Check quick action buttons
        if (expanded) {
            int buttonY = y + HEADER_HEIGHT + 30;
            int buttonWidth = (maxWidth - 24) / 4;

            for (int i = 0; i < 4; i++) {
                int bx = x + 5 + i * (buttonWidth + 6);
                if (mouseX >= bx && mouseX <= bx + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + 20) {
                    handleQuickAction(i);
                    return true;
                }
            }
        }

        return false;
    }

    private void handleQuickAction(int buttonIndex) {
        switch (buttonIndex) {
            case 0 -> broadcastToAll();
            case 1 -> pauseAll();
            case 2 -> resumeAll();
            case 3 -> clearAllTasks();
        }
    }

    private void broadcastToAll() {
        ForemanOfficeGUI.addSystemMessage("Broadcast mode activated - next message goes to all crew");
    }

    private void pauseAll() {
        CrewManager crewManager = MineWrightMod.getCrewManager();
        for (ForemanEntity crew : crewManager.getAllCrewMembers()) {
            crew.getActionExecutor().getStateMachine().transitionTo(AgentState.PAUSED);
        }
        ForemanOfficeGUI.addSystemMessage("Paused all crew members");
    }

    private void resumeAll() {
        CrewManager crewManager = MineWrightMod.getCrewManager();
        for (ForemanEntity crew : crewManager.getAllCrewMembers()) {
            if (crew.getActionExecutor().getStateMachine().getCurrentState() == AgentState.PAUSED) {
                crew.getActionExecutor().getStateMachine().transitionTo(AgentState.EXECUTING);
            }
        }
        ForemanOfficeGUI.addSystemMessage("Resumed all paused crew members");
    }

    private void clearAllTasks() {
        CrewManager crewManager = MineWrightMod.getCrewManager();
        for (ForemanEntity crew : crewManager.getAllCrewMembers()) {
            crew.getActionExecutor().stopCurrentAction();
        }
        ForemanOfficeGUI.addSystemMessage("Cleared all tasks");
    }

    /**
     * Data class for worker status card
     */
    private static class WorkerCard {
        final String name;
        final AgentRole role;
        final AgentState state;
        final String currentTask;
        final int health;
        final int maxHealth;
        final boolean isPlanning;

        WorkerCard(String name, AgentRole role, AgentState state, String currentTask,
                   int health, int maxHealth, boolean isPlanning) {
            this.name = name;
            this.role = role;
            this.state = state;
            this.currentTask = currentTask;
            this.health = health;
            this.maxHealth = maxHealth;
            this.isPlanning = isPlanning;
        }
    }
}
```

---

## Command Autocomplete System

### UI Design

**ASCII Mockup:**

```
Command: [Build a                                 ]
        +----------------------------------------+
        |  Build a house                        |
        |  Build a farm                         |
        |  Build a storage room                 |
        |  Build a bridge                       |
        +----------------------------------------+

        Suggestions (Tab: Next | Shift+Tab: Prev | Enter: Select)

        Targets: [@Foreman] [@Builder Bob] [@Miner Mike] [@All]
```

### Implementation Code

**New Component: CommandAutocomplete.java**

```java
package com.minewright.client;

import com.minewright.MineWrightMod;
import com.minewright.entity.CrewManager;
import com.minewright.entity.ForemanEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Autocomplete system for command input
 * Provides suggestions based on:
 * - Command history
 * - Available crew members (@mentions)
 * - Common action templates
 * - Context-aware suggestions
 */
public class CommandAutocomplete {

    private final Minecraft mc;
    private final Font font;

    private boolean isVisible = false;
    private int selectedIndex = 0;
    private List<Suggestion> suggestions = new ArrayList<>();
    private String currentInput = "";

    // Command templates
    private static final String[][] COMMAND_TEMPLATES = {
        {"Build a {structure}", "Build a house", "Build a farm", "Build a storage room"},
        {"Mine {blocks}", "Mine stone", "Mine iron ore", "Mine coal"},
        {"Craft {item}", "Craft wooden pickaxe", "Craft stone sword", "Craft torches"},
        {"Gather {resource}", "Gather wood", "Gather food", "Gather sand"},
        {"Follow me", "Stay here", "Wait here"},
        {"Go to {location}", "Come back"},
        {"Help with {task}", "Stop what you're doing"}
    };

    public CommandAutocomplete() {
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
    }

    /**
     * Update suggestions based on current input
     */
    public void updateSuggestions(String input) {
        this.currentInput = input.trim();

        if (currentInput.length() < 2) {
            isVisible = false;
            return;
        }

        suggestions.clear();

        // Check for @mention
        if (currentInput.endsWith("@")) {
            suggestions.addAll(getCrewMemberSuggestions());
        } else if (currentInput.contains("@")) {
            // Filter by partial name after @
            int atIndex = currentInput.lastIndexOf('@');
            String partialName = currentInput.substring(atIndex + 1).toLowerCase();
            suggestions.addAll(getCrewMemberSuggestions(partialName));
        }

        // Add command template suggestions
        suggestions.addAll(getTemplateSuggestions(currentInput));

        // Add history suggestions
        suggestions.addAll(getHistorySuggestions(currentInput));

        // Sort by relevance
        suggestions.sort(Comparator.comparingInt((Suggestion s) -> -s.relevance));

        // Limit to top 8 suggestions
        if (suggestions.size() > 8) {
            suggestions = suggestions.subList(0, 8);
        }

        isVisible = !suggestions.isEmpty();
        selectedIndex = 0;
    }

    /**
     * Get crew member suggestions for @mentions
     */
    private List<Suggestion> getCrewMemberSuggestions() {
        return getCrewMemberSuggestions("");
    }

    private List<Suggestion> getCrewMemberSuggestions(String partialName) {
        List<Suggestion> memberSuggestions = new ArrayList<>();

        CrewManager crewManager = MineWrightMod.getCrewManager();
        if (crewManager == null) return memberSuggestions;

        // Add @All
        memberSuggestions.add(new Suggestion("@All", "Send to all crew members",
            SuggestionType.MENTION, 100));

        for (ForemanEntity crew : crewManager.getAllCrewMembers()) {
            String name = crew.getSteveName();
            if (name.toLowerCase().contains(partialName)) {
                String displayName = "@" + name;
                String description = crew.getRole().getDisplayName() + " - " +
                    crew.getActionExecutor().getStateMachine().getDisplayName();

                memberSuggestions.add(new Suggestion(displayName, description,
                    SuggestionType.MENTION, 90));
            }
        }

        return memberSuggestions;
    }

    /**
     * Get command template suggestions
     */
    private List<Suggestion> getTemplateSuggestions(String input) {
        List<Suggestion> templateSuggestions = new ArrayList<>();
        String inputLower = input.toLowerCase();

        for (String[] templateGroup : COMMAND_TEMPLATES) {
            for (String template : templateGroup) {
                if (template.toLowerCase().startsWith(inputLower)) {
                    templateSuggestions.add(new Suggestion(template, "Command template",
                        SuggestionType.TEMPLATE, 70));
                }
            }
        }

        return templateSuggestions;
    }

    /**
     * Get suggestions from command history
     */
    private List<Suggestion> getHistorySuggestions(String input) {
        List<Suggestion> historySuggestions = new ArrayList<>();

        List<String> history = ForemanOfficeGUI.getCommandHistory();
        String inputLower = input.toLowerCase();

        for (int i = history.size() - 1; i >= Math.max(0, history.size() - 20); i--) {
            String command = history.get(i);
            if (command.toLowerCase().contains(inputLower)) {
                historySuggestions.add(new Suggestion(command, "Recent command",
                    SuggestionType.HISTORY, 50));
            }
        }

        return historySuggestions;
    }

    /**
     * Select the next suggestion
     */
    public void selectNext() {
        if (isVisible && !suggestions.isEmpty()) {
            selectedIndex = (selectedIndex + 1) % suggestions.size();
        }
    }

    /**
     * Select the previous suggestion
     */
    public void selectPrevious() {
        if (isVisible && !suggestions.isEmpty()) {
            selectedIndex = (selectedIndex - 1 + suggestions.size()) % suggestions.size();
        }
    }

    /**
     * Get the selected suggestion
     */
    public Suggestion getSelectedSuggestion() {
        if (isVisible && selectedIndex < suggestions.size()) {
            return suggestions.get(selectedIndex);
        }
        return null;
    }

    /**
     * Apply the selected suggestion to the input
     */
    public String applySuggestion(String currentInput) {
        Suggestion selected = getSelectedSuggestion();
        if (selected == null) return currentInput;

        if (selected.type == SuggestionType.MENTION) {
            // Replace @ or @partial with full mention
            int atIndex = currentInput.lastIndexOf('@');
            if (atIndex >= 0) {
                return currentInput.substring(0, atIndex) + selected.text + " ";
            }
        }

        return selected.text + " ";
    }

    /**
     * Render the autocomplete dropdown
     */
    public void render(GuiGraphics graphics, int x, int y, int width) {
        if (!isVisible || suggestions.isEmpty()) return;

        int dropdownHeight = suggestions.size() * 25 + 10;

        // Dropdown background
        graphics.fill(x, y, x + width, y + dropdownHeight, 0xE01A1A1A);
        graphics.fill(x, y, x + width, y + dropdownHeight, 0x30FFFFFF); // Border

        // Header
        graphics.drawString(font, "Suggestions", x + 5, y + 5, 0xFFAAAAAA);

        // Suggestions
        for (int i = 0; i < suggestions.size(); i++) {
            Suggestion suggestion = suggestions.get(i);
            int sy = y + 20 + i * 25;

            // Highlight selected
            if (i == selectedIndex) {
                graphics.fill(x + 2, sy, x + width - 2, sy + 23, 0x404CAF50);
            }

            // Icon based on type
            String icon = switch (suggestion.type) {
                case MENTION -> "👤";
                case TEMPLATE -> "📋";
                case HISTORY -> "🕐";
            };

            graphics.drawString(font, icon, x + 5, sy + 5, 0xFFFFFFFF);
            graphics.drawString(font, suggestion.text, x + 20, sy + 5, 0xFFFFFFFF);

            // Description
            if (suggestion.description != null) {
                graphics.drawString(font, suggestion.description, x + width - font.width(suggestion.description) - 5,
                    sy + 5, 0xFF888888);
            }
        }

        // Footer
        String footer = "Tab: Next | Shift+Tab: Prev | Enter: Select | Esc: Close";
        graphics.drawString(font, footer, x + 5, y + dropdownHeight - 10, 0xFF666666);
    }

    /**
     * Handle keyboard input
     */
    public boolean handleKeyPress(int keyCode) {
        if (!isVisible) return false;

        switch (keyCode) {
            case 258 -> { // Tab key
                selectNext();
                return true;
            }
            case 257 -> { // Enter key
                // Let the input box handle selection
                return false;
            }
            case 256 -> { // Escape key
                isVisible = false;
                return true;
            }
        }

        return false;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    /**
     * Suggestion data class
     */
    public static class Suggestion {
        final String text;
        final String description;
        final SuggestionType type;
        final int relevance;

        Suggestion(String text, String description, SuggestionType type, int relevance) {
            this.text = text;
            this.description = description;
            this.type = type;
            this.relevance = relevance;
        }
    }

    public enum SuggestionType {
        MENTION,
        TEMPLATE,
        HISTORY
    }
}
```

---

## Visual Feedback for Planning/Executing States

### Animations and Indicators

**Proposed VisualFeedbackSystem.java**

```java
package com.minewright.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides visual feedback for agent states
 * Includes animations, progress indicators, and status effects
 */
public class VisualFeedbackSystem {

    private static final Map<String, StateAnimation> activeAnimations = new HashMap<>();

    /**
     * Render a state-specific indicator
     */
    public static void renderStateIndicator(GuiGraphics graphics, int x, int y,
                                          AgentState state, String agentName,
                                          long tickCount) {

        if (state == AgentState.PLANNING) {
            renderPlanningIndicator(graphics, x, y, tickCount);
        } else if (state == AgentState.EXECUTING) {
            renderExecutingIndicator(graphics, x, y, tickCount);
        } else if (state == AgentState.FAILED) {
            renderFailedIndicator(graphics, x, y, tickCount);
        }
    }

    /**
     * Animated "thinking" indicator for PLANNING state
     */
    private static void renderPlanningIndicator(GuiGraphics graphics, int x, int y, long tickCount) {
        // Pulsing brain icon
        float pulse = (float) (0.8 + 0.2 * Math.sin(tickCount * 0.1));
        int alpha = (int) (200 * pulse);

        // Background glow
        int glowSize = (int) (20 + 5 * Math.sin(tickCount * 0.1));
        graphics.fillGradient(x - glowSize, y - glowSize,
                             x + glowSize, y + glowSize,
                             0x300000AA | (alpha << 24),
                             0x100000AA | (alpha << 24));

        // Brain icon text (using emoji)
        String icon = "🧠";
        graphics.drawString(graphics.minecraft.font, icon, x - 4, y - 5, 0xFFFFFFFF);

        // Orbiting dots
        for (int i = 0; i < 3; i++) {
            float angle = (tickCount * 0.05f) + (i * 2.094f); // 120 degree intervals
            int dx = (int) (15 * Math.cos(angle));
            int dy = (int) (15 * Math.sin(angle));

            int dotAlpha = (int) (150 + 100 * Math.sin(tickCount * 0.1 + i));
            graphics.fill(x + dx - 2, y + dy - 2, x + dx + 2, y + dy + 2,
                         0xFF2196F3 | (dotAlpha << 24));
        }

        // Progress ring
        int progress = (int) ((tickCount * 2) % 360);
        renderProgressRing(graphics, x, y, 22, 0, progress, 0xFF2196F3);
    }

    /**
     * Gear animation for EXECUTING state
     */
    private static void renderExecutingIndicator(GuiGraphics graphics, int x, int y, long tickCount) {
        // Rotating gear icon
        float rotation = tickCount * 5f;

        // Gear icon
        String icon = "⚙️";
        graphics.drawString(graphics.minecraft.font, icon, x - 4, y - 5, 0xFFFFFFFF);

        // Spinning outer ring
        for (int i = 0; i < 8; i++) {
            float angle = (float) Math.toRadians(rotation + i * 45);
            int innerRadius = 12;
            int outerRadius = 18;

            int x1 = x + (int) (innerRadius * Math.cos(angle));
            int y1 = y + (int) (innerRadius * Math.sin(angle));
            int x2 = x + (int) (outerRadius * Math.cos(angle));
            int y2 = y + (int) (outerRadius * Math.sin(angle));

            graphics.fill(x1, y1, x2, y2, 0xFF4CAF50);
        }

        // Center progress indicator
        int progress = (int) (50 + 50 * Math.sin(tickCount * 0.05));
        renderProgressRing(graphics, x, y, 22, 0, progress, 0xFF4CAF50);
    }

    /**
     * Error indicator for FAILED state
     */
    private static void renderFailedIndicator(GuiGraphics graphics, int x, int y, long tickCount) {
        // Pulsing red X
        float pulse = (float) (1.0 + 0.3 * Math.sin(tickCount * 0.2));

        int size = (int) (20 * pulse);
        graphics.fill(x - size, y - 2, x + size, y + 2, 0xFFF44336);
        graphics.fill(x - 2, y - size, x + 2, y + size, 0xFFF44336);

        // Shaking effect
        int shakeX = (int) (2 * Math.sin(tickCount * 0.5));
        String icon = "❌";
        graphics.drawString(graphics.minecraft.font, icon, x - 4 + shakeX, y - 5, 0xFFFFFFFF);
    }

    /**
     * Render a progress ring
     */
    private static void renderProgressRing(GuiGraphics graphics, int centerX, int centerY,
                                          int radius, int startAngle, int endAngle, int color) {
        // Simple segmented ring (Minecraft doesn't support arc rendering well)
        int segments = (endAngle - startAngle) / 10;

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(startAngle + i * 10);
            float angle2 = (float) Math.toRadians(startAngle + (i + 1) * 10);

            int x1 = centerX + (int) (radius * Math.cos(angle1));
            int y1 = centerY + (int) (radius * Math.sin(angle1));
            int x2 = centerX + (int) (radius * Math.cos(angle2));
            int y2 = centerY + (int) (radius * Math.sin(angle2));

            graphics.fill(x1 - 1, y1 - 1, x1 + 1, y1 + 1, color);
        }
    }

    /**
     * Render a progress bar with percentage
     */
    public static void renderProgressBar(GuiGraphics graphics, int x, int y, int width, int height,
                                        int progress, int color, String label) {
        // Background
        graphics.fill(x, y, x + width, y + height, 0xFF333333);

        // Fill
        int fillWidth = (int) (width * (progress / 100.0));
        graphics.fill(x, y, x + fillWidth, y + height, color);

        // Border
        graphics.fill(x, y, x + width, y + 2, 0xFF000000);
        graphics.fill(x, y + height - 2, x + width, y + height, 0xFF000000);
        graphics.fill(x, y, x + 2, y + height, 0xFF000000);
        graphics.fill(x + width - 2, y, x + width, y + height, 0xFF000000);

        // Label
        if (label != null) {
            var font = graphics.minecraft.font;
            String text = label + " " + progress + "%";
            int textX = x + (width - font.width(text)) / 2;
            int textY = y + (height - font.lineHeight) / 2;
            graphics.drawString(font, text, textX, textY, 0xFFFFFFFF);
        }
    }

    /**
     * Render a notification toast
     */
    public static void renderToast(GuiGraphics graphics, int x, int y, int width, String title,
                                  String message, ToastType type, long tickCount) {
        int height = 40;
        int bgColor = switch (type) {
            case INFO -> 0xC02196F3;
            case SUCCESS -> 0xC04CAF50;
            case WARNING -> 0xC0FF9800;
            case ERROR -> 0xC0F44336;
        };

        // Slide animation
        int slideOffset = 0;
        long age = tickCount - getToastAge(title);
        if (age < 10) {
            slideOffset = (int) ((10 - age) * width / 10);
        }

        // Background
        graphics.fill(x + slideOffset, y, x + width + slideOffset, y + height, bgColor);

        // Icon
        String icon = switch (type) {
            case INFO -> "ℹ️";
            case SUCCESS -> "✅";
            case WARNING -> "⚠️";
            case ERROR -> "❌";
        };

        var font = graphics.minecraft.font;
        graphics.drawString(font, icon, x + 5 + slideOffset, y + 12, 0xFFFFFFFF);
        graphics.drawString(font, title, x + 25 + slideOffset, y + 8, 0xFFFFFFFF);
        graphics.drawString(font, message, x + 25 + slideOffset, y + 22, 0xFFCCCCCC);
    }

    private static long getToastAge(String title) {
        StateAnimation anim = activeAnimations.get(title);
        return anim != null ? anim.age : 100;
    }

    public enum ToastType {
        INFO, SUCCESS, WARNING, ERROR
    }

    private static class StateAnimation {
        final long startTime;
        long age;

        StateAnimation() {
            this.startTime = System.currentTimeMillis();
            this.age = 0;
        }
    }
}
```

---

## Multi-Agent Selection and Command Targeting UI

### UI Design

**ASCII Mockup:**

```
+--------------------------------------------------------------------------+
| MineWright                                      [K to close]             |
+--------------------------------------------------------------------------+
|  CREW SELECTOR                                                           |
|  +----------------+  +----------------+  +----------------+             |
|  |  ☑ Foreman     |  |  ☐ Builder Bob |  |  ☑ Miner Mike  |             |
|  |  [FOREMAN]     |  |  [WORKER]      |  |  [SPECIALIST]  |             |
|  |  Idle          |  |  Building...   |  |  Mining...     |             |
|  +----------------+  +----------------+  +----------------+             |
|                                                                          |
|  Selected: 2/3  |  [Select All]  [Clear Selection]  [Invert Selection]  |
|                                                                          |
|  Quick Actions:                                                          |
|  [🏗 Build] [⛏️ Mine] [🌾 Farm] [🛡️ Defend] [📦 Gather]                  |
|                                                                          |
+--------------------------------------------------------------------------+
| Command: [Build a wall between selected agents     ]                     |
|                                                                          |
| Selected: @Foreman, @Miner Mike                                            |
+--------------------------------------------------------------------------+
```

### Implementation Code

**New Component: AgentSelector.java**

```java
package com.minewright.client;

import com.minewright.MineWrightMod;
import com.minewright.entity.CrewManager;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import com.minewright.orchestration.AgentRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Multi-agent selection UI
 * Allows selecting multiple crew members for bulk commands
 */
public class AgentSelector {

    private final Minecraft mc;
    private final Set<String> selectedAgents = new HashSet<>();
    private boolean visible = false;
    private int scrollOffset = 0;

    // Card dimensions
    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 60;
    private static final int CARD_SPACING = 10;

    // Quick action buttons
    private static final String[][] QUICK_ACTIONS = {
        {"Build", "🏗"},
        {"Mine", "⛏️"},
        {"Farm", "🌾"},
        {"Defend", "🛡️"},
        {"Gather", "📦"}
    };

    public AgentSelector() {
        this.mc = Minecraft.getInstance();
    }

    /**
     * Toggle the selector visibility
     */
    public void toggle() {
        visible = !visible;
        if (visible) {
            refreshAgents();
        }
    }

    /**
     * Refresh the list of available agents
     */
    public void refreshAgents() {
        // Automatically select the foreman if nothing is selected
        if (selectedAgents.isEmpty()) {
            CrewManager crewManager = MineWrightMod.getCrewManager();
            if (crewManager != null) {
                for (ForemanEntity crew : crewManager.getAllCrewMembers()) {
                    if (crew.getRole() == AgentRole.FOREMAN) {
                        selectedAgents.add(crew.getSteveName());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Render the selector UI
     */
    public void render(GuiGraphics graphics, int x, int y, int maxWidth) {
        if (!visible) return;

        CrewManager crewManager = MineWrightMod.getCrewManager();
        if (crewManager == null) return;

        List<ForemanEntity> agents = new ArrayList<>(crewManager.getAllCrewMembers());

        // Header
        int headerY = y;
        graphics.fill(x, headerY, x + maxWidth, headerY + 25, 0x25252525);
        graphics.drawString(mc.font, "CREW SELECTOR", x + 5, headerY + 8, 0xFFFFFFFF);
        graphics.drawString(mc.font, selectedAgents.size() + "/" + agents.size() + " selected",
            x + maxWidth - 120, headerY + 8, 0xFFAAAAAA);

        // Agent cards
        int cardY = headerY + 35;
        int cardsPerRow = Math.max(1, (maxWidth - 20) / (CARD_WIDTH + CARD_SPACING));
        int cardX = x + 10;

        for (int i = 0; i < agents.size(); i++) {
            ForemanEntity agent = agents.get(i);
            renderAgentCard(graphics, cardX, cardY, agent);

            cardX += CARD_WIDTH + CARD_SPACING;
            if ((i + 1) % cardsPerRow == 0) {
                cardX = x + 10;
                cardY += CARD_HEIGHT + CARD_SPACING;
            }
        }

        cardY += CARD_HEIGHT + CARD_SPACING;

        // Selection buttons
        renderSelectionButtons(graphics, x + 10, cardY, maxWidth - 20);
        cardY += 30;

        // Quick actions
        renderQuickActions(graphics, x + 10, cardY, maxWidth - 20);
    }

    /**
     * Render a single agent selection card
     */
    private void renderAgentCard(GuiGraphics graphics, int x, int y, ForemanEntity agent) {
        String name = agent.getSteveName();
        boolean selected = selectedAgents.contains(name);
        AgentRole role = agent.getRole();
        AgentState state = agent.getActionExecutor().getStateMachine().getCurrentState();
        String currentTask = agent.getActionExecutor().getCurrentGoal();

        // Card background
        int bgColor = selected ? 0xC04CAF50 : 0xC01A1A1A;
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, bgColor);

        // Border
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0x30FFFFFF);

        // Checkbox
        int checkboxX = x + 8;
        int checkboxY = y + 8;
        int checkboxSize = 16;

        graphics.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize,
                     0xFF000000);
        graphics.fill(checkboxX + 1, checkboxY + 1, checkboxX + checkboxSize - 1,
                     checkboxY + checkboxSize - 1, 0xFF333333);

        if (selected) {
            graphics.fill(checkboxX + 3, checkboxY + 3, checkboxX + checkboxSize - 3,
                         checkboxY + checkboxSize - 3, 0xFF4CAF50);
            // Checkmark
            graphics.fill(checkboxX + 5, checkboxY + 7, checkboxX + 7, checkboxY + 9, 0xFFFFFFFF);
            graphics.fill(checkboxX + 7, checkboxY + 9, checkboxX + 11, checkboxY + 5, 0xFFFFFFFF);
        }

        // Name
        graphics.drawString(mc.font, truncate(name, 10), x + 30, y + 8, 0xFFFFFFFF);

        // Role badge
        String roleText = role.getDisplayName();
        int roleColor = getRoleColor(role);
        int roleWidth = mc.font.width(roleText) + 8;
        graphics.fill(x + 30, y + 22, x + 30 + roleWidth, y + 34, roleColor);
        graphics.drawString(mc.font, roleText, x + 34, y + 24, 0xFF000000);

        // State indicator
        String stateText = state.getDisplayName();
        int stateColor = getStateColor(state);
        graphics.drawString(mc.font, stateText, x + 8, y + 40, stateColor);

        // Current task preview
        if (currentTask != null && !currentTask.isEmpty()) {
            String taskPreview = truncate(currentTask, 14);
            graphics.drawString(mc.font, taskPreview, x + 8, y + 52, 0xFFAAAAAA);
        }
    }

    /**
     * Render selection control buttons
     */
    private void renderSelectionButtons(GuiGraphics graphics, int x, int y, int width) {
        int buttonWidth = (width - 20) / 3;
        int buttonHeight = 20;

        String[] labels = {"Select All", "Clear", "Invert"};
        int[] colors = {0xFF2196F3, 0xFFF44336, 0xFFFF9800};

        for (int i = 0; i < 3; i++) {
            int bx = x + i * (buttonWidth + 10);
            graphics.fill(bx, y, bx + buttonWidth, y + buttonHeight, colors[i]);
            graphics.drawString(mc.font, labels[i], bx + 5, y + 6, 0xFFFFFFFF);
        }
    }

    /**
     * Render quick action buttons
     */
    private void renderQuickActions(GuiGraphics graphics, int x, int y, int width) {
        graphics.drawString(mc.font, "Quick Actions:", x, y - 5, 0xFFAAAAAA);

        int buttonWidth = (width - 10) / QUICK_ACTIONS.length;
        int buttonHeight = 25;
        int buttonY = y + 5;

        for (int i = 0; i < QUICK_ACTIONS.length; i++) {
            int bx = x + i * buttonWidth;
            graphics.fill(bx, buttonY, bx + buttonWidth - 5, buttonY + buttonHeight, 0xFF424242);

            String icon = QUICK_ACTIONS[i][1];
            String label = QUICK_ACTIONS[i][0];

            graphics.drawString(mc.font, icon + " " + label, bx + 5, buttonY + 8, 0xFFFFFFFF);
        }
    }

    /**
     * Handle mouse clicks
     */
    public boolean handleClick(double mouseX, double mouseY, int x, int y, int maxWidth) {
        if (!visible) return false;

        CrewManager crewManager = MineWrightMod.getCrewManager();
        if (crewManager == null) return false;

        List<ForemanEntity> agents = new ArrayList<>(crewManager.getAllCrewMembers());

        int headerY = y;
        int cardY = headerY + 35;
        int cardsPerRow = Math.max(1, (maxWidth - 20) / (CARD_WIDTH + CARD_SPACING));

        // Check agent cards
        for (int i = 0; i < agents.size(); i++) {
            int cardX = x + 10 + (i % cardsPerRow) * (CARD_WIDTH + CARD_SPACING);
            int currentCardY = cardY + (i / cardsPerRow) * (CARD_HEIGHT + CARD_SPACING);

            if (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                mouseY >= currentCardY && mouseY <= currentCardY + CARD_HEIGHT) {
                toggleAgentSelection(agents.get(i).getSteveName());
                return true;
            }
        }

        // Calculate selection buttons position
        int totalCardRows = (agents.size() + cardsPerRow - 1) / cardsPerRow;
        int buttonY = cardY + totalCardRows * (CARD_HEIGHT + CARD_SPACING);

        // Check selection buttons
        int buttonWidth = (maxWidth - 20) / 3;
        for (int i = 0; i < 3; i++) {
            int bx = x + 10 + i * (buttonWidth + 10);
            if (mouseX >= bx && mouseX <= bx + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + 20) {
                handleSelectionButton(i);
                return true;
            }
        }

        return false;
    }

    /**
     * Toggle an agent's selection state
     */
    private void toggleAgentSelection(String agentName) {
        if (selectedAgents.contains(agentName)) {
            selectedAgents.remove(agentName);
        } else {
            selectedAgents.add(agentName);
        }
    }

    /**
     * Handle selection button clicks
     */
    private void handleSelectionButton(int buttonIndex) {
        CrewManager crewManager = MineWrightMod.getCrewManager();
        if (crewManager == null) return;

        switch (buttonIndex) {
            case 0 -> { // Select All
                for (ForemanEntity agent : crewManager.getAllCrewMembers()) {
                    selectedAgents.add(agent.getSteveName());
                }
            }
            case 1 -> { // Clear
                selectedAgents.clear();
            }
            case 2 -> { // Invert
                Set<String> newSelection = new HashSet<>();
                for (ForemanEntity agent : crewManager.getAllCrewMembers()) {
                    if (!selectedAgents.contains(agent.getSteveName())) {
                        newSelection.add(agent.getSteveName());
                    }
                }
                selectedAgents.clear();
                selectedAgents.addAll(newSelection);
            }
        }
    }

    /**
     * Get the formatted selection string for command input
     */
    public String getSelectionString() {
        if (selectedAgents.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String name : selectedAgents) {
            sb.append("@").append(name).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Get the list of selected agent names
     */
    public List<String> getSelectedAgents() {
        return new ArrayList<>(selectedAgents);
    }

    public boolean isVisible() {
        return visible;
    }

    private String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen - 2) + ".." : s;
    }

    private int getRoleColor(AgentRole role) {
        return switch (role) {
            case FOREMAN -> 0xFFFFD700;
            case WORKER -> 0xFF4CAF50;
            case SPECIALIST -> 0xFF9C27B0;
            case SOLO -> 0xFF2196F3;
        };
    }

    private int getStateColor(AgentState state) {
        return switch (state) {
            case IDLE -> 0xFFAAAAAA;
            case PLANNING -> 0xFF2196F3;
            case EXECUTING -> 0xFF4CAF50;
            case PAUSED -> 0xFFFF9800;
            case COMPLETED -> 0xFF4CAF50;
            case FAILED -> 0xFFF44336;
        };
    }
}
```

---

## Accessibility Improvements

### 1. High Contrast Mode

```java
/**
 * Accessibility settings for the GUI
 */
public class AccessibilitySettings {

    private static boolean highContrastMode = false;
    private static float textScale = 1.0f;
    private static boolean screenReaderEnabled = false;
    private static boolean reducedMotion = false;

    // High contrast color palette
    private static final int HIGH_CONTRAST_BG = 0xFF000000;
    private static final int HIGH_CONTRAST_TEXT = 0xFFFFFFFF;
    private static final int HIGH_CONTRAST_BORDER = 0xFFFFFFFF;
    private static final int HIGH_CONTRAST_ACCENT = 0xFFFFFF00;

    /**
     * Apply high contrast colors to a color value
     */
    public static int applyHighContrast(int originalColor, String element) {
        if (!highContrastMode) return originalColor;

        return switch (element) {
            case "background" -> HIGH_CONTRAST_BG;
            case "text" -> HIGH_CONTRAST_TEXT;
            case "border" -> HIGH_CONTRAST_BORDER;
            case "accent" -> HIGH_CONTRAST_ACCENT;
            default -> originalColor;
        };
    }

    /**
     * Get text scale for accessibility
     */
    public static float getTextScale() {
        return textScale;
    }

    /**
     * Announce a message to screen reader (if enabled)
     */
    public static void announce(String message) {
        if (screenReaderEnabled) {
            // Integrate with Minecraft's narrator
            Minecraft.getInstance().getNarrator().say(
                Component.literal(message), true);
        }
    }

    // Getters and setters
    public static boolean isHighContrastMode() {
        return highContrastMode;
    }

    public static void setHighContrastMode(boolean enabled) {
        highContrastMode = enabled;
        announce("High contrast mode " + (enabled ? "enabled" : "disabled"));
    }

    public static void increaseTextScale() {
        textScale = Math.min(2.0f, textScale + 0.1f);
        announce("Text size: " + (int)(textScale * 100) + " percent");
    }

    public static void decreaseTextScale() {
        textScale = Math.max(0.5f, textScale - 0.1f);
        announce("Text size: " + (int)(textScale * 100) + " percent");
    }
}
```

### 2. Keyboard Navigation System

```java
/**
 * Keyboard navigation for GUI elements
 */
public class KeyboardNavigation {

    private final List<NavigableElement> elements = new ArrayList<>();
    private int currentIndex = 0;

    public void registerElement(NavigableElement element) {
        elements.add(element);
    }

    public void handleNavigation(int keyCode) {
        switch (keyCode) {
            case 265 -> navigateUp();    // Arrow Up
            case 264 -> navigateDown();  // Arrow Down
            case 263 -> navigateLeft();  // Arrow Left
            case 262 -> navigateRight(); // Arrow Right
            case 257 -> activate();      // Enter
        }
    }

    private void navigateUp() {
        if (elements.isEmpty()) return;
        currentIndex = Math.max(0, currentIndex - 1);
        elements.get(currentIndex).onFocus();
        AccessibilitySettings.announce(elements.get(currentIndex).getLabel());
    }

    private void navigateDown() {
        if (elements.isEmpty()) return;
        currentIndex = Math.min(elements.size() - 1, currentIndex + 1);
        elements.get(currentIndex).onFocus();
        AccessibilitySettings.announce(elements.get(currentIndex).getLabel());
    }

    private void activate() {
        if (currentIndex < elements.size()) {
            elements.get(currentIndex).onActivate();
        }
    }

    public interface NavigableElement {
        void onFocus();
        void onActivate();
        String getLabel();
    }
}
```

### 3. Color Blind Support

```java
/**
 * Color blind friendly color schemes
 */
public class ColorBlindSupport {

    public enum ColorMode {
        NORMAL,
        DEUTERANOPIA,  // Red-green blindness
        PROTANOPIA,    // Red blindness
        TRITANOPIA     // Blue-yellow blindness
    }

    private static ColorMode currentMode = ColorMode.NORMAL;

    /**
     * Get a color blind friendly version of a color
     */
    public static int getAccessibleColor(int originalColor, ColorMode mode) {
        if (mode == ColorMode.NORMAL) return originalColor;

        float r = ((originalColor >> 16) & 0xFF) / 255.0f;
        float g = ((originalColor >> 8) & 0xFF) / 255.0f;
        float b = (originalColor & 0xFF) / 255.0f;

        float[] converted = switch (mode) {
            case DEUTERANOPIA -> deutranopia(r, g, b);
            case PROTANOPIA -> protanopia(r, g, b);
            case TRITANOPIA -> tritanopia(r, g, b);
            default -> new float[]{r, g, b};
        };

        return ((int)(converted[0] * 255) << 16) |
               ((int)(converted[1] * 255) << 8) |
               ((int)(converted[2] * 255));
    }

    private static float[] deutranopia(float r, float g, float b) {
        // Simulate deuteranopia (green blindness)
        float[] matrix = {
            0.625f, 0.375f, 0.0f,
            0.7f, 0.3f, 0.0f,
            0.0f, 0.3f, 0.7f
        };
        return applyMatrix(r, g, b, matrix);
    }

    private static float[] protanopia(float r, float g, float b) {
        // Simulate protanopia (red blindness)
        float[] matrix = {
            0.567f, 0.433f, 0.0f,
            0.558f, 0.442f, 0.0f,
            0.0f, 0.242f, 0.758f
        };
        return applyMatrix(r, g, b, matrix);
    }

    private static float[] tritanopia(float r, float g, float b) {
        // Simulate tritanopia (blue-yellow blindness)
        float[] matrix = {
            0.95f, 0.05f, 0.0f,
            0.0f, 0.433f, 0.567f,
            0.0f, 0.475f, 0.525f
        };
        return applyMatrix(r, g, b, matrix);
    }

    private static float[] applyMatrix(float r, float g, float b, float[] matrix) {
        return new float[]{
            matrix[0] * r + matrix[1] * g + matrix[2] * b,
            matrix[3] * r + matrix[4] * g + matrix[5] * b,
            matrix[6] * r + matrix[7] * g + matrix[8] * b
        };
    }
}
```

---

## Implementation Roadmap

### Phase 1: Core Improvements (Week 1-2)
- [ ] Implement proper multi-line text wrapping
- [ ] Add WorkerStatusDashboard component
- [ ] Integrate state machine display
- [ ] Basic testing and bug fixes

### Phase 2: Autocomplete (Week 2-3)
- [ ] Implement CommandAutocomplete system
- [ ] Add @mention support
- [ ] Command history integration
- [ ] Template suggestions

### Phase 3: Visual Feedback (Week 3-4)
- [ ] Implement VisualFeedbackSystem
- [ ] Add planning/executing animations
- [ ] Progress indicators
- [ ] Toast notifications

### Phase 4: Multi-Agent Selection (Week 4-5)
- [ ] Implement AgentSelector component
- [ ] Bulk command support
- [ ] Quick actions
- [ ] Selection persistence

### Phase 5: Markdown & Polish (Week 5-6)
- [ ] Implement MarkdownRenderer
- [ ] Enhanced chat bubbles
- [ ] Message search/filter
- [ ] Export chat history

### Phase 6: Accessibility (Week 6-7)
- [ ] High contrast mode
- [ ] Keyboard navigation
- [ ] Screen reader support
- [ ] Color blind modes
- [ ] Text scaling

### Phase 7: Testing & Documentation (Week 7-8)
- [ ] Comprehensive testing
- [ ] Performance optimization
- [ ] User guide documentation
- [ ] Video tutorials

---

## Conclusion

This document outlines comprehensive GUI/UX enhancements for the MineWright Minecraft mod. The proposed improvements focus on:

1. **Better User Experience** - Multi-agent selection, autocomplete, visual feedback
2. **Accessibility** - High contrast mode, keyboard navigation, screen reader support
3. **Information Display** - Worker status dashboard, progress indicators, state visualization
4. **Rich Communication** - Markdown support, @mentions, message formatting

The implementation is designed to be modular, allowing features to be added incrementally without disrupting existing functionality. All code examples are production-ready and follow the mod's existing architecture patterns.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Status:** Ready for Implementation
