# Companion UI/UX Research Report
## AI Companion Interaction Design for Games

**Research Date:** February 26, 2026
**Project:** MineWright - "Cursor for Minecraft"
**Focus:** Designing intuitive, non-intrusive companion interaction UI

---

## Executive Summary

This document synthesizes research on AI companion UI/UX patterns from game design, conversational interfaces, and accessibility best practices. The goal is to create an optimal user experience for interacting with MineWright agents in Minecraft while maintaining gameplay immersion.

**Key Findings:**
1. **Command input** should be accessible but non-intrusive (hotkey + overlay)
2. **Visual feedback** is critical for companion state and emotional expression
3. **Multi-agent coordination** requires clear selection and status visualization
4. **Progressive disclosure** prevents overwhelming users with features
5. **Voice integration** needs clear activation indicators and fallback options

---

## Table of Contents

1. [Game Companion UI Patterns](#1-game-companion-ui-patterns)
2. [Chat Interface Design](#2-chat-interface-design)
3. [Command Input Methods](#3-command-input-methods)
4. [Feedback Mechanisms](#4-feedback-mechanisms)
5. [HUD Design for Minecraft](#5-hud-design-for-minecraft)
6. [Emotional Expression](#6-emotional-expression)
7. [Multi-Agent Visualization](#7-multi-agent-visualization)
8. [Accessibility](#8-accessibility)
9. [UI Mockup Concepts](#9-ui-mockup-concepts)
10. [Interaction Flow Diagrams](#10-interaction-flow-diagrams)
11. [Implementation Priority](#11-implementation-priority)

---

## 1. Game Companion UI Patterns

### 1.1 RPG Companion Systems

**Mass Effect / Dragon Age Style:**
- **Companion Wheel:** Radial menu for quick commands
- **Status Indicators:** Health bars over companions
- **Dialogue History:** Log of conversations
- **Relationship Meters:** Visible rapport tracking

**Key Elements:**
```
┌─────────────────────────────────┐
│  [Party Portrait] [Name]        │
│  ████████████░░  80/100 HP      │
│  ❤️🧡💛💚  4 Hearts Relation     │
│  💬 Chat  📋 Tasks  ⚙️ Settings │
└─────────────────────────────────┘
```

### 1.2 Relationship Visualization

**Affection/Rapport Systems:**
- **Heart-shaped meters** for romantic relationships
- **Shield icons** for loyalty/trust
- **Color gradients** (red→green) for attitude
- **Numerical displays** (0-100) alongside visuals

**Implementation Patterns:**
```java
// Two-layer progress bar system
Background: Empty state (outline)
Foreground: Fill state (colored gradient)
Update: Event-driven on relationship changes
Save: Persist in game saves
```

**Best Practices:**
- Show changes immediately when relationship shifts
- Match icon style to game theme
- Provide tooltips explaining relationship levels
- Unlock content at thresholds with visual feedback

---

## 2. Chat Interface Design

### 2.1 2025 AI Interface Trends

**Major Shift:** Moving away from generic chat-alike interfaces

> "The ubiquitous chatbox (bottom input field, speech bubbles, sidebar history) is considered a 'lazy solution'." - 2025 UX Research

**Proactive UX Design Principles:**
1. **Anticipate user needs** - Suggest next steps
2. **Contextual coherence** - Remember conversation history
3. **Multimodal support** - Text, voice, gesture inputs
4. **Progressive disclosure** - Reveal advanced features gradually

### 2.2 Conversational UI Best Practices

**Visual Design:**
- **Minimalist, immersive** - Focus on content, not chrome
- **Dark themes** with high contrast for reduced eye strain
- **Human-friendly animations** to reduce mechanical feel
- **Modular layouts** - Sidebar, bottom, or floating window

**Message Bubbles:**
```
┌──────────────────────────────────────┐
│ You: Build a house here               │  ← Green (user)
│                                      │
│   MineWright: I'll start with the          │  ← Blue (MineWright)
│   foundation. Placing stones...       │
│                                      │
│   Alex: I'll help with the roof!      │  ← Purple (Alex)
└──────────────────────────────────────┘
```

**Color Coding:**
- **User messages:** Green (0xC04CAF50)
- **Companion responses:** Blue (0xC02196F3)
- **System messages:** Orange (0xC0FF9800)
- **Error messages:** Red (0xC0F44336)

### 2.3 Current MineWright Implementation

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\client\MineWrightGUI.java`

**Strengths:**
- ✓ Slide-in panel from right side
- ✓ Scrollable message history (500 messages)
- ✓ Message bubbles with color coding
- ✓ Command history navigation (↑↓ arrows)
- ✓ Transparent overlay (doesn't block gameplay)

**Current Implementation:**
```java
// Panel dimensions
private static final int PANEL_WIDTH = 200;
private static final int PANEL_PADDING = 6;

// Transparency
private static final int BACKGROUND_COLOR = 0x15202020; // 8% opacity
private static final int BORDER_COLOR = 0x40404040;     // 25% opacity

// Message types
- USER_BUBBLE_COLOR = Green
- MINEWRIGHT_BUBBLE_COLOR = Blue
- SYSTEM_BUBBLE_COLOR = Orange
```

**Improvement Opportunities:**
- Add companion status indicators
- Show task progress visualization
- Implement relationship/trust meters
- Add agent selection for multi-agent coordination

---

## 3. Command Input Methods

### 3.1 Radial Menu Design

**Best For:** Quick actions, gamepad-friendly

**Pattern:**
```
        ┌───┐
     ┌──┤ V ├──┐
     │  └───┘  │
  ┌──┤   ◉   ├──┐
  │  └───┬───┘  │
  │ ◀ ◉ │ ◉ ▶  │
  │     │       │
  │  ◉  ↓  ◉   │
  │     │       │
  └─────┴───────┘

Center: Current action
N: Move/Build
S: Cancel/Wait
E: Attack/Mine
W: Follow/Defend
```

**Implementation:**
- **4-8 options** work best with directional inputs
- **Sub-menus** spawn from center re-alignment
- **Click outside** center to dismiss
- **Controller support:** D-pad, joystick, mouse middle-click

### 3.2 Command Input Patterns

**1. Hotkey Activation (Current Implementation)**
```java
// Press K to open command panel
KeyBindings.java: KEY_FOREMAN_GUI = KeyMapping.Builder
    .get("key.minewright.gui", GLFW_KEY_K, "key.categories.minewright")
```

**2. Chat Prefix Commands**
```
.minewright build house
.minewright follow me
.minewright status
```

**3. Voice Commands**
```
"Hey MineWright, build a house"
"MineWright, follow me"
"Everyone, stop what you're doing"
```

**4. Context Menu (Right-click on MineWright)**
```
┌─────────────────┐
│ 👤 Command      │
│ 📋 Status       │
│ 🎯 Give Task    │
│ ⚙️ Settings     │
│ ❌ Dismiss      │
└─────────────────┘
```

### 3.3 Recommended Input Hierarchy

**Priority Order:**
1. **Hotkey (K)** - Primary method, fastest access
2. **Text commands** - For complex multi-step instructions
3. **Voice** - Accessibility and hands-free gameplay
4. **Radial menu** - Quick actions without typing
5. **Context menu** - Agent-specific actions

---

## 4. Feedback Mechanisms

### 4.1 Visual Feedback States

**Companion States:**
```
State          Icon    Color    Description
─────────────────────────────────────────────
IDLE           💤      Gray     Waiting for commands
PLANNING       🧠      Blue     LLM processing
EXECUTING      ▶️      Green    Performing action
WAITING        ⏸️      Yellow   Blocked/waiting
ERROR          ❌      Red     Failed action
SUCCESS        ✅       Green   Task completed
```

**Implementation:**
```java
public enum AgentState {
    IDLE, PLANNING, EXECUTING, WAITING, ERROR
}

// Visual feedback loop
1. User sends command
2. MineWright state → PLANNING (🧠 blue)
3. LLM processes (show spinner)
4. MineWright state → EXECUTING (▶️ green)
5. Task completes (✅ green)
6. MineWright state → IDLE (💤 gray)
```

### 4.2 Voice Command Feedback

**Visual Indicators (Microsoft Game Chat 2 API):**

**States:**
- **Silent:** No mic icon
- **Listening:** Mic icon with pulse animation
- **Processing:** Spinner with waveform
- **Speaking:** Sound wave animation
- **Muted:** Mic icon with red line

**Implementation:**
```java
// Poll every UI frame for voice state
chat_user::chat_indicator() returns:
- SILENT
- TALKING
- LOCAL_MICROPHONE_MUTED
- LISTENING

// Visual feedback
LISTENING:  🔵 Pulsing blue ring
PROCESSING: 🔄 Spinning wheel
SPEAKING:   🟢 Green waveform
ERROR:      🔴 Red "Try again"
```

**Best Practices:**
- Display **mute/speaking icons** in player UI
- Poll **every UI frame** for real-time updates
- Provide **directional indicators** for voice input
- Show **volume level** when detected
- Offer both **push-to-talk** and **always-on** modes

### 4.3 Task Progress Visualization

**Progress Bar Psychology:**
- **Zeigarnik Effect:** Unfilled bars create urge to complete
- **LinkedIn Success:** 20% increase in profile completion

**Progress Indicators:**
```
┌─────────────────────────────────┐
│ Building: ████████░░ 80%        │  ← Linear bar
│                                 │
│ Mining:   ⛏️░░░░░░░ 1/10         │  ← Step tracker
│                                 │
│ Crafting: 🔄 Processing...      │  ← Spinner
└─────────────────────────────────┘
```

**Types:**
- **Horizontal bars** - Traditional linear progress
- **Circular/ring** - Compact, space-efficient
- **Step trackers** - Multi-step processes (3-5 steps)
- **Map-based** - Visual journey representation

**Design Principles:**
- Clear visual hierarchy
- Frequent, visible updates
- Achievable step increments
- Immediate response to actions

---

## 5. HUD Design for Minecraft

### 5.1 Minecraft Forge 1.20.1 GUI Best Practices

**Key APIs:**
```java
// HUD rendering
@SubscribeEvent
public static void onRenderOverlay(RenderGuiOverlayEvent.Post event)

// Screen dimensions
int width = event.getResolution().getScaledWidth();
int height = event.getResolution().getScaledHeight();

// Proper texture binding
mc.renderEngine.bindTexture(texture);

// Forging 1.20.1
- IGuiOverlay still available
- Use RegisterGuiOverlaysEvent
- Extend GuiScreen for custom screens
```

### 5.2 HUD Layout Strategies

**Option 1: Side Panel (Current)**
```
┌─────────────────┬─────────┐
│                 │ [MineWright] │
│                 │ Panel   │
│   Game View     │ 200px   │
│                 │ Width   │
│                 │         │
└─────────────────┴─────────┘
```

**Option 2: Top Bar Overlay**
```
┌─────────────────────────────────┐
│ MineWright: 🧠 Planning... 💚 20/20   │
└─────────────────────────────────┘
│                                 │
│         Game View               │
│                                 │
└─────────────────────────────────┘
```

**Option 3: Corner Status**
```
┌─────────────────────┬─────────┐
│                     │ [S] 💚  │
│                     │ 🧠 Pln  │
│      Game View      │ 🔨 Bld  │
│                     │ [A] 💚  │
│                     │ ⚔️ Idl  │
└─────────────────────┴─────────┘
```

### 5.3 Transmitted Design Principles

**From HUDAPI Mod:**
- **Simple API:** `Panels.add(panel)`
- **Player customization:** Reposition via Alt+drag
- **14 customizable properties:** Position, colors, background, outline

**Best Practices:**
- **8-15% opacity** for overlays (don't obstruct gameplay)
- **Scissoring** for scrollable areas
- **Gradient fills** for alpha support
- **Non-blocking:** Use overlay screens, not pause screens

### 5.4 Current Implementation Analysis

**File:** `C:\Users\casey\minewright\src\main\java\com\minewright\client\MineWrightGUI.java`

**Transparency Levels:**
```java
BACKGROUND_COLOR = 0x15202020  // Alpha: 0x15 (21/255 ≈ 8%)
BORDER_COLOR = 0x40404040      // Alpha: 0x40 (64/255 ≈ 25%)
HEADER_COLOR = 0x25252525      // Alpha: 0x25 (37/255 ≈ 15%)
```

**Strengths:**
- ✓ Ultra-transparent overlay
- ✓ Slide-in animation
- ✓ Scrollable message history
- ✓ Command history navigation
- ✓ Scissoring for message area

**Recommended Enhancements:**
1. Add Foreman status display in header
2. Show relationship/trust meter
3. Implement task progress bars
4. Add agent selection tabs
5. Include voice activation indicator

---

## 6. Emotional Expression

### 6.1 Expression Without Facial Animation

**Challenge:** Minecraft entities have limited facial animation

**Solutions:**

**1. Body Language & Posture**
```
Happy:   Jumping, head bobbing
Sad:     Slowed movement, head down
Angry:   Rapid movements, shaking
Confused: Rotating in place, pauses
```

**2. Particle Effects**
```
Happy:   ❤️ Heart particles
Thinking: 💭 Thought bubbles
Success: ✨ Sparkle particles
Error:   💨 Smoke particles
Working: ⬆️ Progress particles
```

**3. Color & Lighting**
```
Glow color changes:
- Blue: Processing/Thinking
- Green: Executing/Success
- Yellow: Waiting/Blocked
- Red: Error/Failed
- Purple: Special ability
```

**4. Sound Design**
```
Acknowledge: Soft chime
Working:      Tool sounds
Success:      Achievement sound
Error:        Low buzz
Question:     Rising tone
```

### 6.2 Relationship-Based Expression

**Trust Level Behaviors:**
```
Trust 0-25%:  Hesitant actions, frequent checks
Trust 25-50%: Normal execution, occasional confirmations
Trust 50-75%: Faster execution, proactive suggestions
Trust 75-100%: Autonomous decisions, anticipatory actions
```

**Mood System:**
```java
public enum Mood {
    HAPPY,      // ❤️ Hearts, bouncy
    CURIOUS,    // 💭 Thoughts, head tilts
    FOCUSED,    // 🔧 Determined, steady
    CONFUSED,   // ❓ Question marks, pauses
    FRUSTRATED, // 😤 Shaking, smoke
    EXCITED     // ✨ Sparkles, jumps
}

// Mood affects:
- Action speed
- Particle effects
- Sound responses
- Message tone
```

### 6.3 Feedback Without Animation

**Visual Feedback Techniques:**
1. **Floating text** above entity ("Ok!", "Working...")
2. **Progress bar** above MineWright during tasks
3. **Equipment changes** (hold different items)
4. **Name tag color** changes with state
5. **Status icon** in HUD

**Example:**
```
     ┌─────────────────┐
     │  MineWright         │ ← Green border = Healthy
     │  [🧠 Thinking]  │ ← Status icon
     │  ████████░░ 80%│ ← Task progress
     │  ❤️🧡💛💚      │ ← Relationship
     └─────────────────┘
```

---

## 7. Multi-Agent Visualization

### 7.1 Squad Control UI Patterns

**Strategy Games Approach:**
- **Multi-panel layouts** for monitoring multiple agents
- **Visual feedback** for agent actions
- **Relationship graphs** showing communication
- **Hierarchical command** with agent autonomy

**Implementation:**
```java
// Agent state tracking
enum AgentState { IDLE, BUSY, WAKING }

// Force-directed relationship graph
Map<Agent, Set<Agent>> communicationGraph
```

### 7.2 Multi-Agent Selection

**Tab Interface:**
```
┌─────────────────────────────────┐
│ [MineWright] [Alex] [Bob] [All]      │  ← Agent tabs
│─────────────────────────────────│
│ MineWright: 🧠 Planning...           │
│ Task: Build house               │
│ Progress: ████████░░ 80%        │
└─────────────────────────────────┘
```

**Command Targeting:**
```
"MineWright, build a house"     → One agent
"MineWright, Alex, follow me"   → Multiple agents
"Everyone, stop"           → All agents
"All miners, go to cave"   → Role-based
```

**Current Implementation:**
```java
// MineWrightGUI.java - parseTargetMineWrights()
- "all minewrights" / "all" / "everyone"
- Comma-separated names
- First word matching

// Example:
"MineWright, Alex, build a house"
→ Targets MineWright and Alex
```

### 7.3 Coordination HUD

**Minimap Integration:**
```
┌─────────────────┐
│                 │
│    [S] [A] [B]  │ ← Agent positions
│       ⛏️        │ ← Current task icon
│                 │
└─────────────────┘
```

**Status Dashboard:**
```
┌─────────────────────────────────┐
│ Active Agents: 3                │
├─────────────────────────────────┤
│ MineWright: 🔨 Building house (80%)  │
│ Alex:  ⛏️ Mining iron (45%)     │
│ Bob:   🏃 Following player      │
└─────────────────────────────────┘
```

**Best Practices:**
- **Compact display** for 5+ agents
- **Color coding** per agent
- **Quick select** via hotkeys (1-5)
- **Group commands** for efficiency
- **Individual override** for specific tasks

---

## 8. Accessibility

### 8.1 Screen Reader Support

**Challenges:**
- Minecraft has limited native screen reader support
- HUD overlays may not be accessible
- Dynamic content needs proper labeling

**Solutions:**
```java
// Add ARIA labels to UI elements
Component.text("MineWright status: Planning")
    .withStyle(style -> style.withFont(ChatFormatting.DARK_GRAY));

// Audio cues for state changes
if (stateChanged) {
    playSound(SoundEvents.UI_BUTTON_CLICK);
    narrator.say("MineWright is now " + newState);
}
```

**Best Practices:**
- **Audio cues** for all state changes
- **Keyboard navigation** for all UI elements
- **High contrast** mode support
- **Text-to-speech** feedback
- **Customizable** text sizes

### 8.2 Voice Command Accessibility

**Benefits:**
- Hands-free gameplay
- Motor accessibility
- Visual impairment support

**Implementation:**
```
Activation:
- Push-to-talk (V key)
- "Hey MineWright" wake word
- Always-on mode

Feedback:
- Visual: Mic icon, waveform
- Audio: Chime when listening
- Haptic: Controller vibration

Commands:
- "MineWright, build a house"
- "Everyone, follow me"
- "Status report"
- "Cancel all tasks"
```

### 8.3 Accessibility Checklist

**Visual:**
- ✓ High contrast mode (toggleable)
- ✓ Colorblind-friendly palettes
- ✓ Scalable text sizes
- ✓ Icon + text labels
- ✓ No color-only information

**Motor:**
- ✓ Full keyboard navigation
- ✓ Remappable keybindings
- ✓ Voice command support
- ✓ Adjustable timing/delays
- ✓ Single-switch mode support

**Cognitive:**
- ✓ Progressive disclosure
- ✓ Clear error messages
- ✓ Undo functionality
- ✓ Contextual help
- ✓ Adjustable complexity

**Hearing:**
- ✓ Visual audio indicators
- ✓ Subtitle support
- ✓ Visual state feedback
- ✓ Haptic feedback option
- ✓ Volume-independent cues

---

## 9. UI Mockup Concepts

### 9.1 Primary Command Interface (Enhanced Current)

```
┌──────────────────────────────────────┐
│  ◀ MineWright AI                    [X]   │  ← Header with close
│  ═══════════════════════════════════ │  ← Separator
│                                       │
│  [MineWright] [Alex] [All] [+ New]        │  ← Agent tabs
│  ───────────────────────────────────  │
│                                       │
│  MineWright: 🧠 Planning... 💚 20/20       │  ← Status line
│  Trust: ❤️🧡💛💚 (75%)                │  ← Relationship meter
│  Task: Building foundation           │  ← Current task
│  Progress: ████████░░ 80%            │  ← Task progress
│                                       │
│  ════ Message History ════           │  ← Messages
│  ┌─────────────────────────────────┐ │
│  │ You: Build a house here         │ │  ← User (green)
│  │                                 │ │
│  │   MineWright: Starting               │ │  ← MineWright (blue)
│  │   foundation now.               │ │
│  │                                 │ │
│  │   System: MineWright entered         │ │  ← System (orange)
│  │   PLANNING state                │ │
│  └─────────────────────────────────┘ │
│           [Scroll ▼]                 │  ← Scroll indicator
│                                       │
│  ════ Command Input ════            │  ← Input area
│  > Tell MineWright what to do...         │  ← Input field
│  [🎤] [Enter: Send | ↑↓: History]   │  ← Voice + hints
└──────────────────────────────────────┘
```

**Key Features:**
- Slide-in from right (animated)
- Agent selection tabs
- Status display with health/trust
- Scrollable message history
- Voice input button
- Command history navigation

### 9.2 Compact HUD Overlay

```
┌──────────────────────────────────────┐
│ K: Command | V: Voice | 1-5: Agents │  ← Hotkey hints
└──────────────────────────────────────┘

┌─────────────────┬─────────────────┐
│                 │ ┌─────────────┐ │
│                 │ │ MineWright       │ │
│   Game View     │ │ 💚 20/20    │ │
│                 │ │ 🧠 Planning │ │
│                 │ │ ❤️❤️❤️♡♡    │ │
│                 │ └─────────────┘ │
│                 │ ┌─────────────┐ │
│                 │ │ Alex        │ │
│                 │ │ 💚 18/20    │ │
│                 │ │ ⛏️ Mining   │ │
│                 │ │ ❤️❤️♡♡♡     │ │
│                 │ └─────────────┘ │
└─────────────────┴─────────────────┘
```

**Purpose:** Persistent status display without blocking gameplay

### 9.3 Radial Menu (Quick Actions)

```
               ┌───┐
            ┌──┤ V ├──┐
            │  └───┘  │
         ┌──┤   ◉   ├──┐
         │  └───┬───┘  │
      ◀  ◉    │    ◉  ▶  (Move, Build, Mine, Follow)
         │     ↓      │
         │   Cancel   │
         └────────────┘

Center: Current state
Outer ring: Quick actions
Hold longer: Sub-menu
```

**Activation:** Hold V (voice) or Right-click MineWright

### 9.4 Voice Command Interface

```
┌──────────────────────────────────────┐
│           🎤 MineWright is listening      │  ← Active indicator
│                                      │
│         ╭──────────────────╮         │
│        ╱                    ╲        │  ← Waveform anim
│       │  ▂▄▆█▆▄▂           │       │
│        ╲                    ╲        │
│         ╰──────────────────╯         │
│                                      │
│     "Hey MineWright, build a house"       │  ← Transcribed
│                                      │
│     [Cancel] [Retry] [Help]          │  ← Actions
└──────────────────────────────────────┘

States:
🎤 Gray:   Microphone available
🔵 Blue:   Listening (pulsing)
🟢 Green:  Processing command
✅ Check:  Command recognized
❌ X:      Recognition failed
```

### 9.5 Relationship/Trust Screen

```
┌──────────────────────────────────────┐
│  MineWright - Relationship Summary        │
│  ═══════════════════════════════════  │
│                                      │
│  Overall Trust: 75%                  │
│  ████████████████░░░░░░░░            │
│                                      │
│  Relationship Level: Trusted Friend  │
│  ❤️🧡💛💚 4/5 Hearts                  │
│                                      │
│  Recent Interactions:                │
│  ✓ "Build house"     +5 trust        │
│  ✓ "Mine iron"       +3 trust        │
│  ✗ Failed task       -2 trust        │
│                                      │
│  Unlocked Perks:                     │
│  • Proactive suggestions             │
│  • Faster execution speed            │
│  • Autonomous decisions              │
│                                      │
│  [View Full History] [Gifts] [Chat]  │
└──────────────────────────────────────┘
```

---

## 10. Interaction Flow Diagrams

### 10.1 Command Flow

```
USER                    FOREMAN                   UI
│                       │                       │
├─ Press K ────────────┼─────────────────────→ │
│                       │                  Open GUI
│                       │                  Show panel
│                       │                       │
├─ Type "build house" ─┼─────────────────────→ │
│                       │              Update input
│                       │                       │
├─ Press Enter ────────┼─────────────────────→ │
│                       │           Add user msg
│                       │                       │
│                  Send command                │
│                       │                  [PLANNING]
│                       │              Show 🧠 icon
│                       │              Show spinner
│                       │                       │
│              Process LLM                    │
│                       │                  [EXECUTING]
│                       │              Show ▶️ icon
│                       │              Show progress
│                       │                       │
│              Execute task                   │
│              (tick by tick)                 │
│                       │                       │
│                 Complete                    │
│                       │            Add success msg
│                       │            Show ✅ icon
│                       │            Update trust
│                       │                       │
│                  [IDLE]                     │
│                  Show 💤 icon               │
```

### 10.2 Multi-Agent Coordination Flow

```
USER                    AGENTS                  UI
│                       │                       │
├─ "Everyone, mine" ────┼─────────────────────→ │
│                       │         Add command
│                       │         Show "All" tab
│                       │                       │
│     ┌─────────────────┼─────────────────┐    │
│     │                 │                 │    │
│     ├─→ MineWright         ├─→ Alex          │    │
│     │  [PLANNING]     │  [PLANNING]      │    │
│     │  🧠             │  🧠              │    │
│     │                 │                 │    │
│     │  [EXECUTING]    │  [EXECUTING]     │    │
│     │  ⛏️ Mining      │  ⛏️ Mining       │    │
│     │  45%            │  62%             │    │
│     │                 │                 │    │
│     └─────────────────┴─────────────────┘    │
│                       │         Update all
│                       │         statuses
│                       │                       │
│  (Alex finishes)      │                 │    │
│                       │  Alex: ✅ Done   │    │
│                       │  [IDLE]         │    │
│                       │                 │    │
│                       │  MineWright: Still   │    │
│                       │  working...     │    │
│                       │                 │    │
│  "Alex, help MineWright" ──┼─────────────────→ │    │
│                       │  Alex: Ok!      │    │
│                       │  [EXECUTING]    │    │
```

### 10.3 Voice Command Flow

```
USER                    SYSTEM                  UI
│                       │                       │
├─ Press V (hold) ──────┼─────────────────────→ │
│                       │           Show 🎤 icon
│                       │           Start pulse
│                       │                       │
├─ "Hey MineWright..." ──────┼─────────────────────→ │
│                       │         Show waveform
│                       │         Audio feedback
│                       │                       │
│                 Transcribe                   │
│                       │         Show text
│                       │                       │
│                 Parse intent                 │
│                       │                       │
│               Validate command               │
│                       │         Show ✅ check
│                       │         Or ❌ error
│                       │                       │
│               Send to MineWright                  │
│                       │         Hide overlay
│                       │         Show status
```

### 10.4 Relationship Building Flow

```
INTERACTION             FOREMAN                   UI
│                       │                       │
├─ Successful task ─────┼─────────────────────→ │
│                       │          +5 trust
│                       │          Show ❤️
│                       │          Update meter
│                       │                       │
├─ Gift item ───────────┼─────────────────────→ │
│                       │          +10 trust
│                       │          Show 💝
│                       │          Special anim
│                       │                       │
├─ Chat conversation ───┼─────────────────────→ │
│                       │          +2 trust
│                       │          Show 💬
│                       │          Memory save
│                       │                       │
│                 Check threshold             │
│                       │                       │
│              If > 50%: Unlock               │
│                       │          New perk
│                       │          Show toast
│                       │          "Level Up!"
```

---

## 11. Implementation Priority

### Phase 1: Core Enhancement (Week 1-2)

**Priority: CRITICAL**

1. **Status Display Enhancement**
   - Add MineWright status line to header
   - Show current task and progress
   - Display health and state
   - File: `MineWrightGUI.java` line 169-172

2. **Relationship Meter**
   - Add trust/rapport meter to UI
   - Implement relationship tracking
   - Show hearts or shield icons
   - Update based on interactions

3. **Task Progress Visualization**
   - Add progress bar for current task
   - Show completion percentage
   - Display sub-tasks if applicable
   - Animate progress updates

### Phase 2: Multi-Agent Support (Week 3-4)

**Priority: HIGH**

4. **Agent Selection Tabs**
   - Add tabs for each MineWright agent
   - "All" tab for group commands
   - Color code per agent
   - Quick select hotkeys (1-5)

5. **Multi-Agent Status Dashboard**
   - Compact view of all agents
   - Show states at a glance
   - Quick command targeting
   - Individual agent override

6. **Command Targeting Enhancement**
   - Improve `parseTargetMineWrights()` method
   - Support role-based targeting
   - Add "near" and "far" filters
   - Visual selection feedback

### Phase 3: Voice Integration (Week 5-6)

**Priority: MEDIUM**

7. **Voice Activation Button**
   - Add microphone button to UI
   - Push-to-talk (V key)
   - Visual feedback (waveform)
   - Fallback to text if unavailable

8. **Voice Command Recognition**
   - Integrate speech-to-text
   - Command parsing
   - Error handling
   - Visual transcription

9. **Voice Feedback System**
   - Audio cues for states
   - Text-to-speech responses
   - Visual indicators
   - Adjustable settings

### Phase 4: Advanced Features (Week 7-8)

**Priority: LOW**

10. **Radial Menu**
    - Quick action wheel
    - Controller support
    - Customizable slots
    - Sub-menu support

11. **Emotional Expression**
    - Particle effects per mood
    - Body language animations
    - Sound design
    - Context-aware responses

12. **Relationship Screen**
    - Full relationship history
    - Perk unlocking system
    - Gift mechanics
    - Special interactions

### Phase 5: Polish & Accessibility (Week 9-10)

**Priority: MEDIUM**

13. **Accessibility Features**
    - High contrast mode
    - Screen reader support
    - Keyboard navigation
    - Scalable text

14. **Animation Polish**
    - Smooth transitions
    - State change animations
    - Loading spinners
    - Success celebrations

15. **Settings & Customization**
    - UI position customization
    - Color themes
    - Opacity controls
    - Keybinding remapping

---

## Implementation Quick Start

### File: `MineWrightGUI.java` Enhancement

**Location:** `C:\Users\casey\minewright\src\main\java\com\minewright\ai\client\MineWrightGUI.java`

**Immediate Enhancements:**

```java
// Add to line 169-172 (header section)
// After: graphics.drawString(mc.font, "§lMineWright AI", panelX + PANEL_PADDING, panelY + 8, TEXT_COLOR);

// NEW: MineWright status display
MineWrightEntity selectedMineWright = getSelectedMineWright();
if (selectedMineWright != null) {
    String statusText = getStatusText(selectedMineWright);
    int statusColor = getStatusColor(selectedMineWright);
    graphics.drawString(mc.font, statusText, panelX + PANEL_PADDING, panelY + 20, statusColor);

    // Trust meter
    int trustLevel = selectedMineWright.getTrustLevel();
    String hearts = "❤️".repeat(trustLevel / 25) + "♡".repeat(4 - trustLevel / 25);
    graphics.drawString(mc.font, hearts, panelX + PANEL_PADDING, panelY + 32, 0xFFFF6B6B);
}

// NEW: Task progress
if (selectedMineWright != null && selectedMineWright.getCurrentTask() != null) {
    String taskName = selectedMineWright.getCurrentTask().getName();
    float progress = selectedMineWright.getCurrentTask().getProgress();

    int progressY = inputAreaY - 30;
    graphics.drawString(mc.font, "§7Task: " + taskName, panelX + PANEL_PADDING, progressY, 0xFF888888);

    int barWidth = PANEL_WIDTH - (PANEL_PADDING * 2);
    int barHeight = 8;
    int filledWidth = (int)(barWidth * progress);

    // Background
    graphics.fillGradient(panelX + PANEL_PADDING, progressY + 12,
                          panelX + PANEL_PADDING + barWidth, progressY + 12 + barHeight,
                          0x40000000, 0x40000000);
    // Fill
    graphics.fillGradient(panelX + PANEL_PADDING, progressY + 12,
                          panelX + PANEL_PADDING + filledWidth, progressY + 12 + barHeight,
                          0xC04CAF50, 0xC04CAF50);
}

// NEW: Agent selection tabs
// Add after message history area (before input area)
List<MineWrightEntity> allMineWrights = MineWrightMod.getMineWrightManager().getAllMineWrights();
if (allMineWrights.size() > 1) {
    int tabY = headerHeight + 5;
    int tabWidth = 40;
    int tabHeight = 15;
    int tabIndex = 0;

    for (MineWrightEntity minewright : allMineWrights) {
        int tabX = panelX + PANEL_PADDING + (tabIndex * (tabWidth + 5));

        // Tab background
        int tabColor = (selectedMineWright == minewright) ? 0x604CAF50 : 0x30202020;
        graphics.fillGradient(tabX, tabY, tabX + tabWidth, tabY + tabHeight, tabColor, tabColor);

        // Tab text (first letter of name)
        String initial = minewright.getMineWrightName().substring(0, 1).toUpperCase();
        graphics.drawString(mc.font, initial, tabX + tabWidth/2 - 3, tabY + 3, 0xFFFFFFFF);

        tabIndex++;
    }
}
```

### Supporting Classes to Add

**File:** `MineWrightEntity.java` additions

```java
// Add to MineWrightEntity class
private int trustLevel = 50; // 0-100
private Task currentTask;
private Mood mood = Mood.IDLE;

public int getTrustLevel() {
    return trustLevel;
}

public void addTrust(int amount) {
    this.trustLevel = Math.max(0, Math.min(100, this.trustLevel + amount));
}

public Task getCurrentTask() {
    return currentTask;
}

public void setCurrentTask(Task task) {
    this.currentTask = task;
}

public Mood getMood() {
    return mood;
}

public void setMood(Mood mood) {
    this.mood = mood;
}
```

---

## Sources & References

### AI Interface Design
- [2025 AI Product Design Principles - UE/UX Checklist](https://juejin.cn/post/7583971282892455988)
- [Design Patterns For AI Interfaces - Smashing Magazine](https://www.smashingmagazine.com/2025/07/design-patterns-ai-interfaces/)
- [ChatUI 3.0 - Conversational Experience Framework](https://www.usmartcloud.com/alibaba/ChatUI/introduction)
- [Generic MCP Chat Client](https://github.com/thoughtspot/mcp-chat-client)

### Game Companion Systems
- [LoveHate - Unity Relationship & Emotion Simulator](https://www.interestcreator.com/lovehate-unity-2024/)
- [TV Tropes - Relationship Values](https://tvtropes.org/pmwiki/pmwiki.php/Main/RelationshipValues)
- [Game UI Design: Mechanics of Fun](https://www.justinmind.com/ui-design/game)
- [Affection Systems in Games](https://www.jianshu.com/p/681b8ebe7f80)

### Progress Visualization
- [Progress Tracking Interfaces - LinkedIn](https://www.linkedin.com/top-content/user-experience/gamification-in-ux-design/progress-tracking-interfaces/)
- [Inspiring Progress Bars - Justinmind](https://www.justinmind.com/ui-design/progress-bars)
- [5 Progress Bar Components - CSDN Blog](https://m.blog.csdn.net/gitblog_00251/article/details/153064256)
- [Design Better Progress Trackers - UXPin](https://www.uxpin.com/studio/blog/design-progress-trackers/)

### Voice Command Integration
- [Microsoft Game Chat 2 C++ API](https://learn.microsoft.com/zh-cn/gaming/gdk/docs/services/multiplayer/chat/game-chat2/using-game-chat-2)
- [Azure Voice Assistants Guidelines](https://learn.microsoft.com/zh-cn/azure/cognitive-services/speech-service/how-to-windows-voice-assistants-get-started)
- [Visual Feedback for Voice Recognition - Patent](http://www.xjishu.com/zhuanli/21/CN105074815.html)
- [Steam Voice Documentation](https://partner.steamgames.com/doc/features/voice)

### Minecraft Forge GUI
- [HUDAPI Mod Documentation](https://www.mcmod.cn/class/21002.html)
- [Minecraft Forge UI Design Guide](https://m.blog.csdn.net/gitblog_00366/article/details/152247889)
- [Minecraft Forge Mod Development Notes](https://www.cnblogs.com/dream0-0/p/13175593.html)
- [BetterF3 GitHub Repository](https://github.com/cominixo/BetterF3)

### Radial Menu Design
- [Steam Controller Radial Menus](https://partner.steamgames.com/doc/features/steam_controller/radial_menus)
- [Game UI Controls: Radial/Pie Menus](https://m.blog.csdn.net/guoqx/article/details/125847159)
- [Tasty Pie Menu - Unity Asset](https://blog.csdn.net/2403_88403568/article/details/146249614)

### Accessibility
- [Accessible Games Resource](https://accessible.games/)
- [Microsoft Gaming GDK - Error Codes](https://learn.microsoft.com/zh-cn/gaming/gdk/docs/reference/errorcodes)
- [Game Connection - Accessibility Opportunities](https://www.game-connection.com/)

---

## Conclusion

This research provides a comprehensive foundation for designing an intuitive, accessible, and engaging companion UI/UX for MineWright AI in Minecraft. The key is to balance **feature richness** with **non-intrusive design**, ensuring that the companion interface enhances rather than interrupts gameplay.

**Success Metrics:**
- Command input latency < 100ms
- Visual state updates within 1 tick
- Zero gameplay obstruction
- Full keyboard navigation
- Voice command accuracy > 90%

**Next Steps:**
1. Implement Phase 1 enhancements (status display, relationship meter)
2. User testing with 5+ players
3. Iterate based on feedback
4. Proceed to multi-agent coordination
5. Long-term: Voice integration and accessibility

---

**Document Version:** 1.0
**Last Updated:** February 26, 2026
**Research Team:** Claude Code (Orchestrator Mode)
