# ForemanOfficeGUI UX Improvements

**Date:** 2026-02-27
**Status:** Design Document
**Component:** `ForemanOfficeGUI.java`
**Scope:** User interface enhancements for MineWright command and control system

---

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [UX Improvement Proposals](#ux-improvement-proposals)
3. [Visual Design Mockups](#visual-design-mockups)
4. [Implementation Priority](#implementation-priority)
5. [Technical Considerations](#technical-considerations)

---

## Current State Analysis

### Existing Features

**File:** `C:\Users\casey\steve\src\main\java\com\minewright\client\ForemanOfficeGUI.java`

| Feature | Status | Notes |
|---------|--------|-------|
| Side-sliding panel (200px width) | ✅ Implemented | Smooth slide animation |
| Toggle with K key | ✅ Implemented | Binds to `KeyBindings.TOGGLE_GUI` |
| Color-coded message bubbles | ✅ Implemented | User (green), Crew (blue), System (orange) |
| Scrollable message history | ✅ Implemented | 500 message max, scrollbar |
| Command input box | ✅ Implemented | EditBox with hint text |
| Command history (arrow keys) | ✅ Implemented | Up/down for prev/next, 50 command storage |
| Mouse wheel scrolling | ✅ Implemented | 3x MESSAGE_HEIGHT scroll speed |
| Semi-transparent backgrounds | ✅ Implemented | Alpha-based rendering |
| Message timestamps | ❌ Missing | Not stored in ChatMessage |
| Multi-line text wrapping | ⚠️ Partial | Truncates with "...", no true wrapping |
| Markdown support | ❌ Missing | Plain text only |
| Crew status visualization | ❌ Missing | No dashboard component |
| Progress indicators | ❌ Missing | No visual progress for long actions |
| Autocomplete/suggestions | ❌ Missing | No Tab completion or suggestions |
| Keyboard navigation | ❌ Missing | No tab index system |
| Multi-agent selection | ❌ Missing | Cannot target multiple crew |
| LLM processing visualization | ❌ Missing | Only "Thinking..." text |
| Error recovery UI | ❌ Missing | Generic error messages |
| Accessibility features | ❌ Missing | No high contrast, screen reader support |

### Message Bubble System

**Current Implementation:**
```java
private static class ChatMessage {
    String sender;           // "You", "Foreman", "Crew", "System"
    String text;             // Plain text message
    int bubbleColor;         // ARGB color with alpha
    boolean isUser;          // true = right-aligned, false = left-aligned
}
```

**Limitations:**
- No timestamp tracking
- No message ID for references
- No mention extraction (@CrewName)
- No formatting (bold, code, links)
- Truncation instead of wrapping

### State Integration

**Available States** (`AgentState` enum):
- `IDLE` - Waiting for commands
- `PLANNING` - Processing with LLM
- `EXECUTING` - Performing actions
- `PAUSED` - Temporarily suspended
- `COMPLETED` - Tasks finished
- `FAILED` - Error occurred

**Current Integration:** None exposed to GUI. State machine exists in `ActionExecutor` but not displayed.

### Agent Roles

**Available Roles** (`AgentRole` enum):
- `FOREMAN` - Coordinates tasks (gold color)
- `WORKER` - Executes tasks (green color)
- `SPECIALIST` - Domain expert (purple color)
- `SOLO` - Independent (blue color)

**Current Integration:** Tracked in `ForemanEntity` but not displayed in GUI.

---

## UX Improvement Proposals

### 1. Enhanced Command Input Experience

#### 1.1 Smart Autocomplete System

**Priority:** HIGH
**Effort:** MEDIUM

**Features:**
- Real-time suggestions as user types
- @mention completion for crew members
- Command template matching
- Fuzzy matching for typos
- Tab/Shift+Tab navigation
- Enter to accept suggestion
- Esc to close dropdown

**Visual Mockup:**
```
┌─────────────────────────────────────────────────────────────────┐
│ Command: [Build a ho]                                           │
│         ┌───────────────────────────────────────────────────┐   │
│         │ 📋 Build a house                                  │   │
│         │ 📋 Build a horse stable                          │   │
│         │ 🕐 Build a hot tub (2 days ago)                   │   │
│         │ 🕐 Build a hotel (5 days ago)                     │   │
│         └───────────────────────────────────────────────────┘   │
│                                                                 │
│         Tab: Next | Shift+Tab: Prev | Enter: Select | Esc: Close│
└─────────────────────────────────────────────────────────────────┘
```

**Data Sources:**
```java
// Suggestion prioritization
1. @mentions (highest priority)
   - @All - Broadcast to all crew
   - @Foreman - The coordinating agent
   - @Builder, @Miner, etc. - Named crew members

2. Command templates (70% relevance)
   - "Build a {structure}"
   - "Mine {quantity} {resource}"
   - "Craft {item}"
   - "Follow me"
   - "Stop what you're doing"

3. Command history (50% relevance)
   - Last 50 commands
   - Boost score for frequently used
   - Decay score over time

4. Contextual suggestions (90% relevance)
   - Nearby blocks: "Mine nearby iron ore"
   - Nearby hostiles: "Attack mobs"
   - Inventory: "Build with {blocks}"
```

#### 1.2 Command Quick Actions Bar

**Priority:** MEDIUM
**Effort:** LOW

**Features:**
- One-click common commands
- Icon-based buttons with tooltips
- Adapts to context (biome, inventory, nearby entities)

**Visual Mockup:**
```
┌─────────────────────────────────────────────────────────────────┐
│ QUICK ACTIONS                                                   │
│ [🏗 Build] [⛏ Mine] [🌾 Farm] [🛡 Defend] [📦 Gather] [✋ Stop]│
└─────────────────────────────────────────────────────────────────┘
```

**Implementation:**
```java
private static final QuickAction[] QUICK_ACTIONS = {
    new QuickAction("Build", "🏗", "build a structure", 0xFF4CAF50),
    new QuickAction("Mine", "⛏", "mine nearby resources", 0xFF9E9E9E),
    new QuickAction("Farm", "🌾", "create automatic farm", 0xFFFFEB3B),
    new QuickAction("Defend", "🛡", "attack nearby enemies", 0xFFF44336),
    new QuickAction("Gather", "📦", "collect dropped items", 0xFF2196F3),
    new QuickAction("Stop", "✋", "stop current action", 0xFFFF9800)
};
```

#### 1.3 Input Validation and Feedback

**Priority:** HIGH
**Effort:** LOW-MEDIUM

**Features:**
- Real-time syntax highlighting
- Error indicators before send
- Helpful correction suggestions

**Visual States:**
```
Normal:  [Build a house]
Error:   [Bulid a house]  ← "Did you mean: Build?"
Warning: [Mine diamonds]  ← ⚠ No diamonds nearby
Success: [Follow me]      ← ✓ Valid command
```

---

### 2. Crew Status Dashboard

#### 2.1 Crew Member Cards

**Priority:** HIGH
**Effort:** MEDIUM

**Features:**
- Grid layout of crew member cards
- Role badge with color coding
- Current state with icon indicator
- Health bar
- Current task preview
- Click to select/deselect

**Visual Mockup:**
```
┌─────────────────────────────────────────────────────────────────┐
│ CREW STATUS                                    [3 Active]        │
├─────────────────────────────────────────────────────────────────┤
│ ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│ │ Foreman     │  │ Builder Bob │  │ Miner Mike  │             │
│ │ [FOREMAN]   │  │ [WORKER]    │  │ [SPECIALIST]│             │
│ │ ⚪ Idle     │  │ ⚙️ Work...  │  │ 🧠 Think... │             │
│ │ Task: None  │  │ Task: House │  │ Task: Mine  │             │
│ │ [██████████]│  │ [██████████]│  │ [██████████]│             │
│ │ 20/20 HP    │  │ 20/20 HP    │  │ 20/20 HP    │             │
│ └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                 │
│ Quick Actions:                                                  │
│ [📢 Broadcast] [⏸ Pause All] [▶ Resume] [🗑 Clear Tasks]      │
└─────────────────────────────────────────────────────────────────┘
```

**Card Specifications:**
```
Card Size: 120px × 80px
Spacing: 8px between cards
Background: 0xC01A1A1A (dark gray with alpha)
Border: 0x30FFFFFF (semi-transparent white)

Role Badge Colors:
- FOREMAN: 0xFFFFD700 (gold)
- WORKER: 0xFF4CAF50 (green)
- SPECIALIST: 0xFF9C27B0 (purple)
- SOLO: 0xFF2196F3 (blue)

State Icons & Colors:
- IDLE: ⚪ 0xFFAAAAAA (gray)
- PLANNING: 🧠 0xFF2196F3 (blue) - pulsing animation
- EXECUTING: ⚙️ 0xFF4CAF50 (green) - rotating gear
- PAUSED: ⏸ 0xFFFF9800 (orange)
- COMPLETED: ✅ 0xFF4CAF50 (green)
- FAILED: ❌ 0xFFF44336 (red) - shaking animation
```

#### 2.2 Multi-Agent Selection

**Priority:** MEDIUM
**Effort:** MEDIUM

**Features:**
- Checkbox on each crew card
- "Select All" / "Clear" / "Invert" buttons
- Selection count display
- Selected agents highlighted

**Selection Mode UI:**
```
┌─────────────────────────────────────────────────────────────────┐
│ CREW SELECTOR                                   [2/3 selected]   │
├─────────────────────────────────────────────────────────────────┤
│ ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│ │ ☑ Foreman   │  │ ☐ Builder   │  │ ☑ Miner     │             │
│ │ [FOREMAN]   │  │ [WORKER]    │  │ [SPECIALIST]│             │
│ └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                 │
│ [Select All] [Clear] [Invert]                                  │
│                                                                 │
│ Command: [Build a wall between @Foreman @Miner Mike      ]     │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3. Visual Feedback Enhancements

#### 3.1 LLM Processing Indicator

**Priority:** HIGH
**Effort:** LOW

**Current:** Simple "Thinking..." text
**Proposed:** Animated indicator with progress ring

**Visual Mockup:**
```
┌─────────────────────────────────────────────────────────────────┐
│ ┌─ Thinking... ────────────────────────────────────── [0:03] ─┐ │
│ │                                                              │ │
│ │                    🧠                                       │ │
│ │                ◌  ◌  ◌                                     │ │
│ │             ◌    ●    ◌                                    │ │
│ │                ◌  ◌  ◌                                     │ │
│ │                                                              │ │
│ │           ████████████░░░░░░░░░                             │ │
│ │           Generating task plan...                          │ │
│ └──────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

**Animation Details:**
- Brain icon with pulsing glow
- Three orbiting dots (120° apart)
- Progress ring filling clockwise
- Ellapsed time counter
- Status message updates

#### 3.2 Action Progress Bars

**Priority:** HIGH
**Effort:** MEDIUM

**Features:**
- Progress bar for long-running actions
- Estimated time remaining
- Current step display

**Visual Mockup:**
```
┌─────────────────────────────────────────────────────────────────┐
│ 🏗 Builder Bob: Building House                                 │
│ ████████████████████████████████████████████████░░░░ 85%       │
│ Step 17/20: Placing roof blocks                                │
│ ETA: ~45 seconds                                               │
└─────────────────────────────────────────────────────────────────┘
```

**Progress Sources:**
```java
// BaseAction progress tracking
public abstract class BaseAction {
    protected int totalSteps = 0;
    protected int completedSteps = 0;

    public float getProgress() {
        return totalSteps == 0 ? 0 : (float)completedSteps / totalSteps;
    }

    public String getStepDescription() {
        return String.format("Step %d/%d", completedSteps, totalSteps);
    }
}

// Specific action implementations
BuildStructureAction: based on blocks placed / total blocks
MineBlockAction: based on blocks mined / target amount
PathfindAction: based on distance traveled / total distance
```

#### 3.3 Toast Notifications

**Priority:** MEDIUM
**Effort:** LOW

**Types:**
- **Success** (green): Task completed
- **Error** (red): Task failed
- **Warning** (orange): Needs attention
- **Info** (blue): Status update

**Visual Mockup:**
```
┌────────────────────────────────────────┐
│ ✅  House completed successfully       │
│     Builder Bob finished in 2:34       │
└────────────────────────────────────────┘
        Slide in from right
        Auto-dismiss after 5 seconds
```

---

### 4. Message History Improvements

#### 4.1 Enhanced Chat Bubbles

**Priority:** HIGH
**Effort:** MEDIUM

**Features:**
- Timestamps on hover
- Message actions (copy, delete, reference)
- @mention highlighting
- Message threading
- Search/filter

**Visual Mockup:**
```
┌─────────────────────────────────────────────────────────────────┐
│ MESSAGES                                    [🔍 Search] [📋]   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  2:35 PM You                                                   │
│ ┌─────────────────────────────────────────────────────────────┐│
│ │ Build a house near the spawn point                          ││
│ └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  2:35 PM @Foreman                                              │
│ ┌─────────────────────────────────────────────────────────────┐│
│ │ OK! I'll build a house for you.                             ││
│ │                                                             ││
│ │ Plan:                                                       ││
│ │ 1. Gather materials (wood, stone)                           ││
│ │ 2. Clear building area                                      ││
│ │ 3. Build foundation (10x10)                                 ││
│ │ 4. Build walls (4 blocks high)                              ││
│ │ 5. Add roof                                                 ││
│ │ 6. Add door and windows                                     ││
│ └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  2:36 PM @Builder Bob                                          │
│ ┌─────────────────────────────────────────────────────────────┐│
│ │ I'll help with the walls! @Foreman, assign me a section.   ││
│ └─────────────────────────────────────────────────────────────┘│
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 4.2 Message Search

**Priority:** LOW
**Effort:** MEDIUM

**Features:**
- Full-text search
- Filter by sender
- Filter by time range
- Highlight matches

**Search UI:**
```
┌─────────────────────────────────────────────────────────────────┐
│ 🔍 Search messages: [house                ]                     │
│                                                                 │
│ Filters: [All] [You] [Foreman] [Builder] [Miner] [System]      │
│         [Today] [This Week] [This Month] [All Time]            │
│                                                                 │
│ Found 12 results                                                │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 2:35 PM You: Build a house near spawn                       │ │
│ │ 2:35 PM Foreman: OK! I'll build a house for you...          │ │
│ │ 2:30 PM Builder Bob: Should I use oak or spruce for...      │ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

### 5. Keyboard Shortcuts

#### 5.1 Current Shortcuts

| Key | Action |
|-----|--------|
| K | Toggle GUI |
| Enter | Send command |
| Escape | Close GUI |
| Arrow Up | Previous command |
| Arrow Down | Next command |
| Mouse Wheel | Scroll messages |

#### 5.2 Proposed Shortcuts

| Key | Action | Priority |
|-----|--------|----------|
| Tab | Autocomplete next suggestion | HIGH |
| Shift+Tab | Autocomplete previous suggestion | HIGH |
| Ctrl+K | Quick command menu | MEDIUM |
| Ctrl+L | Clear chat | LOW |
| Ctrl+F | Search messages | MEDIUM |
| Ctrl+A | Select all crew | MEDIUM |
| Ctrl+D | Deselect all crew | MEDIUM |
| Ctrl+1-9 | Select crew by number | LOW |
| F1 | Help / command list | MEDIUM |
| F2 | Toggle crew dashboard | MEDIUM |
| F3 | Toggle verbose mode | LOW |

---

### 6. Error Handling and Recovery

#### 6.1 Enhanced Error Messages

**Priority:** HIGH
**Effort:** LOW-MEDIUM

**Current Error:** Generic messages
**Proposed:** Contextual errors with recovery steps

**Error Message Format:**
```
┌─────────────────────────────────────────────────────────────────┐
│ ⚠️  Command Failed                                              │
│                                                                 │
│ I couldn't find a crew member named "Builder".                 │
│                                                                 │
│ Available crew members:                                         │
│   • Foreman [FOREMAN] - Idle                                   │
│   • Miner Mike [SPECIALIST] - Mining                           │
│                                                                 │
│ Did you mean:                                                   │
│   • "spawn Builder" - Create a new crew member                 │
│   • "@Foreman build..." - Send to existing foreman             │
│   • "all build..." - Send to all crew                          │
│                                                                 │
│ [Retry] [Cancel] [Help]                                        │
└─────────────────────────────────────────────────────────────────┘
```

**Error Categories:**
1. **Crew Not Found** - List available, suggest spawn
2. **Invalid Command** - Show similar commands, examples
3. **Resource Unavailable** - Suggest locations
4. **Permission Denied** - Explain restriction
5. **Rate Limit** - Show wait time
6. **Network Error** - Offer retry with backoff

#### 6.2 Command Correction

**Priority:** MEDIUM
**Effort:** MEDIUM

**Features:**
- Fuzzy matching for typos
- "Did you mean?" suggestions
- Auto-correct on confirmation

**Example:**
```
You:    "Bulid a house"
System: "Did you mean: Build a house? [Y/N]"
You:    Y
System: [Build a house] → Executing...
```

---

### 7. Accessibility Features

#### 7.1 High Contrast Mode

**Priority:** MEDIUM
**Effort:** LOW

**Features:**
- Toggle high contrast colors
- Solid backgrounds (no transparency)
- White borders on all elements
- Maximum text readability

**Color Mapping:**
```
Normal Mode → High Contrast
Background: 0x15202020 → 0xFF000000
Text: 0xFFFFFFFF → 0xFFFFFFFF
Borders: 0x40404040 → 0xFFFFFFFF
User Bubble: 0xC04CAF50 → 0xFF00FF00 (solid green)
Crew Bubble: 0xC02196F3 → 0xFF0000FF (solid blue)
System Bubble: 0xC0FF9800 → 0xFFFFFF00 (solid yellow)
```

#### 7.2 Text Scaling

**Priority:** LOW
**Effort:** LOW

**Features:**
- Increase/decrease text size
- Scale all GUI elements proportionally
- Persistent setting

**Shortcuts:**
- Ctrl + Plus: Increase size
- Ctrl + Minus: Decrease size
- Ctrl + 0: Reset to default

#### 7.3 Screen Reader Support

**Priority:** LOW
**Effort:** MEDIUM

**Features:**
- Announce new messages
- Announce state changes
- Announce errors
- Navigation announcements

**Integration:**
```java
// Use Minecraft's narrator
Minecraft.getInstance().getNarrator().say(
    Component.literal("Foreman is now planning"), true);

// Announce errors
Minecraft.getInstance().getNarrator().say(
    Component.literal("Command failed: Crew member not found"), true);
```

---

## Visual Design Mockups

### Complete GUI Layout

```
┌──────────────────────────────────────────────────────────────────────┐
│ ▸ MineWright                                    [K to close] [⚙]    │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ ┌─ CREW STATUS ────────────────────────────────────── [3 Active] ─┐ │
│ │ ┌────────────┐  ┌────────────┐  ┌────────────┐                  │ │
│ │ │ Foreman    │  │ Builder Bob│  │ Miner Mike │                  │ │
│ │ │ [FOREMAN]  │  │ [WORKER]   │  │ [SPECIALIST]│                  │ │
│ │ │ ⚪ Idle    │  │ ⚙️ Work... │  │ 🧠 Think...│                  │ │
│ │ │ No task    │  │ House      │  │ Mining     │                  │ │
│ │ │ ████20/20  │  │ ████20/20  │  │ ████20/20  │                  │ │
│ │ └────────────┘  └────────────┘  └────────────┘                  │ │
│ │                                                                  │ │
│ │ [📢 Broadcast] [⏸ Pause] [▶ Resume] [🗑 Clear]                 │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│ ┌─ QUICK ACTIONS ─────────────────────────────────────────────────┐ │
│ │ [🏗 Build] [⛏ Mine] [🌾 Farm] [🛡 Defend] [📦 Gather] [✋ Stop]│ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│ ┌─ MESSAGES ────────────────────────────── [🔍 Search] [📋 Export]─┐ │
│ │                                                                    │ │
│ │  2:35 PM You                                                       │ │
│ │ ┌────────────────────────────────────────────────────────────────┐│ │
│ │ │ Build a house near spawn                                       ││ │
│ │ └────────────────────────────────────────────────────────────────┘│ │
│ │                                                                    │ │
│ │  2:35 PM @Foreman                                                  │ │
│ │ ┌────────────────────────────────────────────────────────────────┐│ │
│ │ │ ✅ OK! I'll build a house for you.                             ││ │
│ │ │                                                                 ││ │
│ │ │ 📋 Plan:                                                        ││ │
│ │ │ 1. Gather materials (wood, stone)                              ││ │
│ │ │ 2. Clear building area 10x10                                   ││ │
│ │ │ 3. Build stone foundation                                      ││ │
│ │ │ 4. Build oak walls (4 blocks high)                             ││ │
│ │ │ 5. Add wooden roof                                             ││ │
│ │ │ 6. Add door and windows                                        ││ │
│ │ │                                                                 ││ │
│ │ │ ⏱️  Estimated time: ~3 minutes                                 ││ │
│ │ └────────────────────────────────────────────────────────────────┘│ │
│ │                                                                    │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│ ┌─ COMMAND ─────────────────────────────────────────────────────────┐ │
│ │                                                                    │ │
│ │  To: [@All] [@Foreman] [@Builder Bob] [@Miner Mike]               │ │
│ │                                                                    │ │
│ │  ┌──────────────────────────────────────────────────────────────┐ │ │
│ │  │ Build a wooden cabin with 2 rooms                           │ │ │
│ │  └──────────────────────────────────────────────────────────────┘ │ │
│ │                                                                    │ │
│ │  ┌──────────────────────────────────────────────────────────────┐ │ │
│ │  │ 📋 Build a castle                                            │ │ │
│ │  │ 📋 Build a chicken coop                                      │ │ │
│ │  │ 🕐 Build a cabin (3 days ago)                                │ │ │
│ │  └──────────────────────────────────────────────────────────────┘ │ │
│ │                                                                    │ │
│ │  [Send] [📎 Attach] [🎤 Voice] [❓ Help]                          │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Enter: Send | Tab: Autocomplete | ↑↓: History | Ctrl+K: Commands   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Collapsed State

```
┌──────────────────────────────────────────────────────────────────────┐
│ ▸ MineWright                                [3 Active] [K to expand]│
└──────────────────────────────────────────────────────────────────────┘
```

### Notification Toast

```
┌────────────────────────────────────────────┐
│ ✅  Task Completed                          │
│     House built successfully in 3:24        │
│     +125 XP earned                          │
└────────────────────────────────────────────┘
     (slides in from right, auto-dismiss)
```

### Progress Overlay

```
┌──────────────────────────────────────────────────────────────────────┐
│ 🏗 Builder Bob: Building House                                      │
│ ████████████████████████████████████████████████░░░░░ 85%           │
│ Step 17/20: Placing oak roof blocks                                  │
│                                                                      │
│ Progress: 85/100 blocks placed                                       │
│ Time elapsed: 2:45                                                  │
│ ETA: ~30 seconds                                                     │
│                                                                      │
│ [⏸ Pause] [⏹ Stop] [📷 Screenshot]                                 │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Implementation Priority

### Phase 1: Quick Wins (Week 1)
**Effort:** 3-5 days
**Impact:** HIGH

- [ ] Enhanced error messages with recovery steps
- [ ] LLM processing indicator (animated)
- [ ] Quick actions bar
- [ ] Message timestamps
- [ ] Basic search/filter

**Files to Modify:**
- `ForemanOfficeGUI.java` - Main UI updates
- `ChatMessage.java` - Add timestamp field
- Create `ErrorHandler.java` - Error message formatting
- Create `QuickActions.java` - Action bar component

### Phase 2: Crew Dashboard (Week 2)
**Effort:** 5-7 days
**Impact:** HIGH

- [ ] Crew status dashboard component
- [ ] Crew member cards
- [ ] State visualization
- [ ] Health bars
- [ ] Quick action buttons (pause, resume, clear)

**Files to Create:**
- `WorkerStatusDashboard.java` - Dashboard component
- `WorkerCard.java` - Individual crew card

**Files to Modify:**
- `ForemanOfficeGUI.java` - Integrate dashboard
- `CrewManager.java` - Add status query methods

### Phase 3: Autocomplete (Week 3)
**Effort:** 5-7 days
**Impact:** HIGH

- [ ] Autocomplete dropdown
- [ ] @mention completion
- [ ] Command template matching
- [ ] History-based suggestions
- [ ] Tab/Shift+Tab navigation

**Files to Create:**
- `CommandAutocomplete.java` - Autocomplete system
- `CommandTemplates.java` - Template definitions
- `FuzzyMatcher.java` - Fuzzy matching algorithm

**Files to Modify:**
- `ForemanOfficeGUI.java` - Integrate autocomplete
- `ForemanOverlayScreen.java` - Handle Tab key

### Phase 4: Progress Indicators (Week 4)
**Effort:** 3-5 days
**Impact:** MEDIUM

- [ ] Progress bars for actions
- [ ] ETA calculation
- [ ] Step-by-step display
- [ ] Toast notifications

**Files to Create:**
- `ProgressTracker.java` - Progress tracking
- `ToastManager.java` - Toast notifications

**Files to Modify:**
- `BaseAction.java` - Add progress reporting
- `ForemanOfficeGUI.java` - Render progress bars

### Phase 5: Multi-Agent Selection (Week 5)
**Effort:** 5-7 days
**Impact:** MEDIUM

- [ ] Agent selector component
- [ ] Checkbox selection
- [ ] Bulk commands
- [ ] Selection persistence

**Files to Create:**
- `AgentSelector.java` - Selection UI
- `SelectionManager.java` - Track selection state

**Files to Modify:**
- `ForemanOfficeGUI.java` - Integrate selector
- `CrewManager.java` - Query selected agents

### Phase 6: Polish & Accessibility (Week 6-7)
**Effort:** 7-10 days
**Impact:** MEDIUM

- [ ] High contrast mode
- [ ] Text scaling
- [ ] Keyboard navigation
- [ ] Screen reader support
- [ ] Color blind modes
- [ ] Animations polish

**Files to Create:**
- `AccessibilitySettings.java` - Settings manager
- `KeyboardNavigation.java` - Tab index system
- `ColorBlindSupport.java` - Color conversion

**Files to Modify:**
- `ForemanOfficeGUI.java` - Apply accessibility settings
- `Config.java` - Add accessibility config options

### Phase 7: Testing & Documentation (Week 8)
**Effort:** 5-7 days
**Impact:** HIGH

- [ ] Unit tests for new components
- [ ] Integration tests
- [ ] User guide documentation
- [ ] Video tutorials
- [ ] Performance optimization
- [ ] Bug fixes

---

## Technical Considerations

### Rendering Performance

**Current Performance:**
- 200px width panel
- ~500 messages max
- Simple fillGradient calls
- No complex animations

**Optimization Strategies:**
1. **Lazy Rendering** - Only render visible messages
2. **Message Culling** - Skip messages outside viewport
3. **Cache Text Metrics** - Pre-calculate text widths
4. **Batch Render Calls** - Group similar draw operations
5. **Limit Animations** - Use tick-based throttling

```java
// Example: Viewport culling
private static void renderMessages(GuiGraphics graphics, int viewportTop, int viewportBottom) {
    for (ChatMessage msg : messages) {
        int msgY = getMessageY(msg);

        // Skip if outside viewport
        if (msgY + msg.height < viewportTop || msgY > viewportBottom) {
            continue;
        }

        renderMessage(graphics, msg, msgY);
    }
}
```

### Data Synchronization

**Client-Server Split:**
- **Client:** GUI rendering, user input, animations
- **Server:** Agent state, action execution, progress tracking

**Synchronization Points:**
1. Agent status updates (every 20 ticks)
2. Action progress (every 5 ticks)
3. State transitions (immediate)
4. New messages (immediate)

```java
// Example: Sync packet
public class ClientboundAgentStatusPacket {
    private String agentName;
    private AgentState state;
    private String currentTask;
    private int progress;
    private int health;

    public static void handle(ClientboundAgentStatusPacket packet, PlayPayloadContext ctx) {
        CrewManager crewManager = MineWrightMod.getCrewManager();
        ForemanEntity agent = crewManager.getCrewMember(packet.agentName);

        if (agent != null) {
            // Update local state
            agent.updateClientState(
                packet.state,
                packet.currentTask,
                packet.progress
            );

            // Refresh GUI
            ForemanOfficeGUI.refreshDashboard();
        }
    }
}
```

### Memory Management

**Concerns:**
- Message history growth
- Cached text metrics
- Animation state

**Mitigation:**
1. **Limit Message History** - 500 message max (already implemented)
2. **Weak References** - Use for cached metrics
3. **Periodic Cleanup** - Clear old animation state
4. **String Interning** - Reduce duplicate strings

```java
// Example: Weak cache for text metrics
private static final Map<String, TextMetrics> textMetricsCache =
    Collections.synchronizedMap(new WeakHashMap<>());

public static TextMetrics getTextMetrics(Font font, String text) {
    return textMetricsCache.computeIfAbsent(text, t -> {
        int width = font.width(t);
        int height = font.lineHeight;
        return new TextMetrics(width, height);
    });
}
```

### Configuration Persistence

**Settings to Persist:**
1. GUI panel position/size
2. Quick action customization
3. Accessibility settings
4. Theme preferences
5. Notification settings

**Storage:** Use Forge's config system

```java
// Example: Config class
@Mod.EventBusSubscriber(modid = MineWrightMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GUIConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue highContrast;
    public static final ForgeConfigSpec.DoubleValue textScale;
    public static final ForgeConfigSpec.BooleanValue showDashboard;
    public static final ForgeConfigSpec.BooleanValue enableAnimations;

    static {
        BUILDER.push("gui");

        highContrast = BUILDER
            .comment("Enable high contrast mode for accessibility")
            .define("highContrast", false);

        textScale = BUILDER
            .comment("Text scaling factor (0.5 to 2.0)")
            .defineInRange("textScale", 1.0, 0.5, 2.0);

        showDashboard = BUILDER
            .comment("Show crew status dashboard")
            .define("showDashboard", true);

        enableAnimations = BUILDER
            .comment("Enable GUI animations")
            .define("enableAnimations", true);

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
```

### Compatibility

**Minecraft Version:** 1.20.1
**Forge Version:** Compatible with 1.20.1
**Java Version:** 17

**Dependencies:**
- Minecraft Forge (already included)
- No additional libraries needed

**Breaking Changes:** None planned
- All changes additive
- Existing UI will remain functional
- New features opt-in

---

## Testing Strategy

### Unit Tests

**Components to Test:**
1. `CommandAutocomplete` - Suggestion relevance scoring
2. `FuzzyMatcher` - Levenshtein distance accuracy
3. `MarkdownRenderer` - Text segmentation
4. `AccessibilitySettings` - Color conversion

**Example Test:**
```java
@Test
public void testAutocompletePrioritization() {
    CommandAutocomplete autocomplete = new CommandAutocomplete();

    // Add history
    addCommandToHistory("build a house");
    addCommandToHistory("build a farm");

    // Test suggestion order
    autocomplete.updateSuggestions("build");

    List<Suggestion> suggestions = autocomplete.getSuggestions();
    assertEquals(3, suggestions.size());
    assertEquals("build a house", suggestions.get(0).text);
    assertEquals("@All", suggestions.get(1).text); // Mentions first
}
```

### Integration Tests

**Scenarios to Test:**
1. Complete command flow (input → plan → execute → complete)
2. Multi-agent coordination
3. Error recovery
4. State persistence
5. Network disconnection handling

### User Testing

**Metrics to Track:**
- Command success rate
- Time to complete tasks
- Error recovery success rate
- Feature usage frequency
- User satisfaction (survey)

**Test Group:**
- 10-15 beta testers
- 2 week testing period
- Feedback survey at end

---

## References

**Related Files:**
- `C:\Users\casey\steve\src\main\java\com\minewright\client\ForemanOfficeGUI.java` - Main GUI
- `C:\Users\casey\steve\src\main\java\com\minewright\client\ForemanOverlayScreen.java` - Input handling
- `C:\Users\casey\steve\src\main\java\com\minewright\client\KeyBindings.java` - Key bindings
- `C:\Users\casey\steve\src\main\java\com\minewright\entity\ForemanEntity.java` - Agent entity
- `C:\Users\casey\steve\src\main\java\com\minewright\entity\CrewManager.java` - Crew management
- `C:\Users\casey\steve\src\main\java\com\minewright\execution\AgentState.java` - State enum
- `C:\Users\casey\steve\src\main\java\com\minewright\action\ActionExecutor.java` - Action execution
- `C:\Users\casey\steve\src\main\java\com\minewright\orchestration\AgentRole.java` - Role enum

**Related Documentation:**
- `C:\Users\casey\steve\docs\GUI_UX_ENHANCEMENTS.md` - Comprehensive UX proposals
- `C:\Users\casey\steve\docs\IMPROVE_COMMAND_UX.md` - Command system improvements

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Status:** Ready for Implementation
