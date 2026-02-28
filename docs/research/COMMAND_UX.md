# Command & Interaction UX Design

**Version:** 1.0
**Status:** Design Document
**Focus:** Player Experience and Intuitiveness

---

## Executive Summary

This document defines the command and interaction experience for players using the MineWright Minecraft mod. The design prioritizes natural language input, multi-agent coordination, voice interaction, and clear feedback without chat spam.

### Core Principles
1. **Natural Language First** - Players should speak naturally, not memorize commands
2. **Progressive Disclosure** - Simple inputs work, complexity reveals itself as needed
3. **Always Clear Feedback** - Player always knows what's happening
4. **Multi-Agent Intuitive** - Coordinating multiple agents feels like managing a team, not programming
5. **Accessibility First** - Voice, text, and visual feedback for all playstyles

---

## Current State Analysis

### Existing Commands
Based on `C:\Users\casey\minewright\src\main\java\com\minewright\ai\command\MineWrightCommands.java`:

| Command | Syntax | Purpose |
|---------|--------|---------|
| `/minewright spawn <name>` | Required | Create a new MineWright agent |
| `/minewright remove <name>` | Required | Remove a MineWright agent |
| `/minewright list` | Optional | List all active MineWrights |
| `/minewright status` | Optional | Show status of all MineWrights |
| `/minewright stop <name>` | Important | Emergency stop a MineWright |
| `/minewright tell <name> <command>` | Core | Send natural language command |
| `/minewright relationship <name>` | Optional | View relationship stats |
| `/minewright promote <name>` | TODO | Promote to Foreman |
| `/minewright demote <name>` | TODO | Demote to Worker |
| `/minewright voice on/off/status/test` | Voice | Control voice system |

### Input Methods
- **GUI**: Press `K` to open sliding panel (cursor-inspired design)
- **Chat**: `/minewright tell <name> <command>`
- **Voice**: Press `V` for push-to-talk (STT interface exists but not implemented)

### Feedback Mechanisms
- **Chat Messages**: `<MineWrightName> message`
- **GUI Panel**: Scrollable message history with bubbles
- **Voice**: TTS responses (configured but stub implementation)
- **Proactive Dialogue**: MineWright comments on environment/events

---

## 1. Command Taxonomy

### 1.1 Management Commands
Commands for controlling the MineWright system itself.

#### Agent Lifecycle
```
/spawn [name]              # Create new agent (default: "MineWright")
/remove <name>             # Remove agent
/list                      # List all agents
/status                    # Show agent status
```

#### Role Management
```
/promote <name>            # Promote to Foreman
/demote <name>             # Demote to Worker
/assign <name> <role>      # Assign specific role
```

#### System Control
```
/minewright stop <name>         # Emergency stop
/minewright stop all            # Stop all agents
/minewright pause               # Pause all agents
/minewright resume              # Resume paused agents
```

### 1.2 Action Commands
Natural language commands for tasks. These go through the LLM planner.

#### Resource Gathering
```
"Mine 10 iron ore"
"Gather 64 cobblestone"
"Chop down this tree"
"Collect sand from the beach"
```

#### Building & Construction
```
"Build a house"
"Create a 5x5 stone platform"
"Construct a wall around this area"
"Build a tower to height 50"
```

#### Movement & Positioning
```
"Come here"
"Follow me"
"Stay here"
"Go to x=100 z=200"
"Patrol around this area"
```

#### Combat & Defense
```
"Attack nearby monsters"
"Defend this position"
"Clear out this cave"
```

#### Crafting & Smelting
```
"Craft 10 pickaxes"
"Smelt all iron ore"
"Make torches"
```

### 1.3 Coordination Commands
Multi-agent commands for team scenarios.

#### Broadcast Commands
```
"All MineWrights, stop"
"Everyone, gather here"
"Team, status report"
```

#### Targeted Commands
```
"MineWright and Alex, build a wall"
"Miner MineWright: gather iron"
"Builder Bob: construct house"
```

#### Foreman Delegation (Future)
```
"Foreman, coordinate building a castle"
"Task team with gathering resources"
"Divide and conquer: clear this forest"
```

### 1.4 Relationship & Social Commands
Commands for companion features.

```
/relationship <name>       # View relationship stats
/hug <name>                # Increase rapport (future)
/gift <name> <item>        # Give item (future)
```

---

## 2. Natural Language Patterns

### 2.1 Supported Patterns

The LLM planner should understand these natural variations:

#### Mining Resources
```
"Mine 10 iron ore"
"Get me 10 iron"
"I need iron ore"
"Can you mine some iron?"
"Start mining iron"
```

#### Building Structures
```
"Build a house"
"Construct a house"
"Make a house"
"Create a house"
"I want a house here"
```

#### Movement Commands
```
"Come here"
"Follow me"
"Stay put"
"Wait here"
"Go over there"
```

### 2.2 Contextual Understanding

#### Location References
```
"Build a house here"        # At player's location
"Mine over there"           # Where player is looking
"Go to the cave"            # Known location
"Build at x=100"            # Explicit coordinate
```

#### Quantity References
```
"Mine 10 iron"              # Explicit count
"Mine some iron"            # Default quantity
"Mine lots of iron"         # Large quantity
"Mine all visible iron"     # All in sight
```

#### Target References
```
"MineWright, mine iron"          # Named agent
"All MineWrights, stop"          # All agents
"The miner, get to work"    # Role-based
"You, build this"           # Implied target
```

### 2.3 Ambiguity Resolution

When commands are ambiguous, the system should:

1. **Ask for clarification** (via GUI or voice)
2. **Make reasonable assumptions** (based on context)
3. **Provide feedback** on what was understood

```
Player: "Build a house"
System: "Building a 5x5 wood house here. OK?"
         [GUI: Confirm / Cancel / Modify]
```

---

## 3. Voice Command Grammar

### 3.1 Voice Mode Structure

Voice commands follow a natural grammar:

```
[PREFIX] [TARGET] [ACTION] [PARAMETERS]
```

#### Components

**Prefix (Optional):**
- "Hey MineWright" / "OK MineWright" / "MineWright"
- Activates voice listening

**Target (Optional):**
- Agent name: "MineWright", "Alex", "Builder"
- Group: "Everyone", "All MineWrights", "Team"
- Implied: Uses last/primary agent

**Action:**
- Verb phrase: "mine", "build", "come here"
- Natural language

**Parameters:**
- Quantities: "10 iron ore"
- Locations: "here", "over there"
- Specifications: "5x5", "stone"

### 3.2 Example Voice Commands

```
"Hey MineWright, mine 10 iron ore"
"OK MineWright, build a house"
"MineWright, come here"
"Everyone, stop what you're doing"
"Alex, construct a wall"
"All MineWrights, status report"
```

### 3.3 Push-to-Talk Flow

1. Player holds `V` key
2. Visual indicator shows "Listening..."
3. Player speaks command
4. Release `V` key
5. STT processes speech
6. GUI shows: "Heard: 'mine 10 iron'"
7. Command executes

### 3.4 Continuous Listening (Optional)

If PTT is disabled:
- Always listening for wake word
- "Hey MineWright" activates for 10 seconds
- Visual feedback when active
- Privacy indicator in GUI

---

## 4. Feedback Mechanisms

### 4.1 Visual Feedback

#### GUI Panel (Primary)
- **Message Bubbles**: Color-coded by sender
  - Green: User commands
  - Blue: MineWright responses
  - Orange: System messages
- **Status Indicators**:
  - Planning: "Thinking..." (spinner)
  - Executing: Progress bar
  - Complete: Checkmark
  - Error: Red X
- **Agent Status Panel**:
  - Current action
  - Progress percentage
  - Inventory (collapsible)

#### In-World Indicators
- **Particle Effects**:
  - Planning: Thought bubbles
  - Working: Action-specific particles
  - Complete: Celebration particles
- **Name Plates**:
  - Show current goal
  - Progress indicator
  - Role badge (Foreman/Worker)
- **Path Visualization**:
  - Show planned path (toggle)
  - Highlight target blocks

#### Toast Notifications
- Brief popups for important events
- Auto-dismiss after 5 seconds
- Non-intrusive

### 4.2 Audio Feedback

#### Text-to-Speech Responses
- **Confirmation**: "On it!", "Right away!"
- **Status**: "Still working on it..."
- **Completion**: "All done!", "Finished!"
- **Errors**: "I can't do that", "I'm stuck"

#### Sound Effects
- **Command Received**: Chime
- **Planning**: Thinking sound
- **Working**: Activity-specific sounds
- **Complete**: Success sound
- **Error**: Error buzz

#### Audio Cues
- **Attention Needed**: Gentle bell
- **Task Stuck**: Warning tone
- **Low Health**: Alert sound

### 4.3 Chat Feedback

**Minimal Chat Usage** (to avoid spam):
- Only important messages in chat
- Success/failure of long tasks
- Emergency alerts
- Relationship milestones

**Chat Message Types:**
```
<MineWright> Task completed: Gathered 10 iron ore
<MineWright> Task failed: Can't reach target
<MineWright> [ALERT] Low health!
<MineWright> [MILESTONE] We've been friends for 7 days!
```

### 4.4 Haptic Feedback (Optional)

For controllers with vibration:
- Light pulse on command received
- Continuous rumble during long tasks
- Double pulse on completion

---

## 5. Error Handling & Help

### 5.1 Error Categories

#### Understanding Errors
```
"I didn't understand that"
"Could you rephrase that?"
"What do you mean by [word]?"
```

#### Execution Errors
```
"I can't reach that"
"I don't have the right tools"
"There's not enough space here"
```

#### System Errors
```
"Sorry, I'm having trouble connecting"
"My AI systems are offline"
"Please try again in a moment"
```

### 5.2 Help System

#### In-Game Help
```
/minewright help                 # General help
/minewright help commands        # Command reference
/minewright help voice           # Voice commands
/minewright help multi           # Multi-agent coordination
```

#### Contextual Help
- Auto-suggestions in GUI
- Command templates
- Examples based on context
- Tutorial mode for new players

#### Progressive Tutorial
```
First spawn:
  "Welcome! I'm MineWright. Press K to open commands,
   or just type what you want me to do!"

First command:
  "You said: [command]. I'll start working on that.
   Check the panel for progress!"

Multi-agent:
  "You now have multiple MineWrights! Use 'All MineWrights'
   to command everyone, or name specific agents."
```

### 5.3 Recovery Actions

When things go wrong:

#### Automatic Retry
- "I got stuck. Trying another approach..."
- "That didn't work. Let me try again."

#### Ask for Help
- "I'm stuck. Can you help?"
- "I can't reach that. Can you clear a path?"

#### Graceful Failure
- "I couldn't complete that task."
- "Something went wrong. Sorry!"
- Clear status indication

---

## 6. Multi-Agent Interaction Patterns

### 6.1 Single Agent (Current Default)

**Flow:**
1. Player presses `K` or opens chat
2. Types natural language command
3. Single MineWright executes

**Example:**
```
Player: "Build a house"
MineWright: "On it! Building a 5x5 wood house."
```

### 6.2 Multiple Agents - Broadcast

**Flow:**
1. Player uses "all" keyword
2. All agents receive command
3. Each agent independently executes

**Example:**
```
Player: "All MineWrights, gather wood"
MineWright1: "Gathering wood!"
Alex: "On it!"
Bob: "Got it!"
```

### 6.3 Multiple Agents - Targeted

**Flow:**
1. Player names specific agents
2. Only named agents execute
3. Others continue current tasks

**Example:**
```
Player: "MineWright and Alex, build a wall"
MineWright: "Building wall!"
Alex: "Helping with wall!"
Bob: (continues previous task)
```

### 6.4 Multiple Agents - Foreman Coordination (Future)

**Flow:**
1. Player commands Foreman
2. Foreman plans and delegates
3. Workers execute sub-tasks
4. Foreman reports progress

**Example:**
```
Player: "Foreman, build a castle"
Foreman: "Planning castle construction..."
         "MineWright: gather stone"
         "Alex: gather wood"
         "Bob: start foundation"
Foreman: "Construction underway. ETA: 5 minutes"
```

### 6.5 Team Interaction Patterns

#### Collaboration
```
Player: "Work together to build a house"
         "MineWright: gather materials"
         "Alex: place blocks"
         "Bob: manage inventory"
```

#### Specialization
```
Player: "Assign roles"
         "MineWright: permanent miner"
         "Alex: permanent builder"
         "Bob: permanent farmer"
```

#### Dynamic Rebalancing
```
Player: "MineWright is done mining"
         "Alex: Share your workload with MineWright"
         "MineWright: Taking over some building tasks"
```

---

## 7. Accessibility Options

### 7.1 Visual Accessibility

#### Color Blindness
- Alternative color schemes
- Pattern/shape indicators
- High contrast mode

#### Text Size
- Adjustable GUI scale
- Large text mode
- Dyslexia-friendly font option

#### Visual Clarity
- Clear, high-contrast indicators
- Minimal visual clutter
- Toggleable particle effects

### 7.2 Auditory Accessibility

#### Visual Substitutes
- Closed captions for TTS
- Visual indicators for sounds
- Screen flash for alerts

#### Volume Controls
- Separate TTS volume
- SFX volume
- Master mute option

#### Alternative Feedback
- Vibration instead of sound
- Visual cues only mode
- Text-only responses

### 7.3 Motor Accessibility

#### Input Flexibility
- Keyboard-only controls
- Mouse-only controls
- Controller support

#### Timing
- No time limits on input
- Adjustable response times
- Hold-to-confirm alternative

#### Voice Alternatives
- Full keyboard command support
- GUI-only mode
- No voice required

### 7.4 Cognitive Accessibility

#### Simplification
- Basic command mode
- Fewer options at once
- Clear, concise feedback

#### Pacing
- Self-paced tutorials
- No forced progress
- Ability to pause/review

#### Memory Aids
- Command history
- Auto-completion
- Contextual hints

---

## 8. Configuration Options

### 8.1 Command Processing

```toml
[commands]
# Natural language processing provider
llm_provider = "groq"  # groq, openai, gemini

# Response verbosity
verbosity = "normal"  # minimal, normal, detailed

# Auto-confirmation for ambiguous commands
auto_confirm = false

# Maximum agents to command with "all"
max_broadcast = 10
```

### 8.2 Feedback Preferences

```toml
[feedback]
# Enable chat messages
chat_responses = true

# Enable GUI panel
gui_enabled = true

# Enable TTS responses
tts_enabled = false

# Enable sound effects
sfx_enabled = true

# Enable particle effects
particles_enabled = true

# Show progress bars
show_progress = true
```

### 8.3 Voice Configuration

```toml
[voice]
# Enable voice input/output
enabled = false

# Voice mode
mode = "logging"  # disabled, logging, real

# Push-to-talk required
push_to_talk = true

# STT settings
stt_language = "en-US"
stt_sensitivity = 0.5

# TTS settings
tts_voice = "default"
tts_volume = 0.8
tts_rate = 1.0
tts_pitch = 1.0
```

---

## 9. Implementation Priorities

### Phase 1: Core Commands (Current)
- [x] Basic natural language processing
- [x] Single agent commands
- [x] GUI panel with feedback
- [x] Basic error handling

### Phase 2: Enhanced Feedback
- [ ] Better progress indication
- [ ] In-world visual indicators
- [ ] Sound effects for actions
- [ ] Improved error messages

### Phase 3: Voice Integration
- [ ] STT implementation
- [ ] TTS integration
- [ ] Push-to-talk keybind
- [ ] Voice command grammar

### Phase 4: Multi-Agent Coordination
- [ ] Targeted commands
- [ ] Broadcast commands
- [ ] Foreman orchestration
- [ ] Team collaboration

### Phase 5: Accessibility
- [ ] Color blind modes
- [ ] High contrast
- [ ] Visual sound indicators
- [ ] Controller support

---

## 10. Design Decisions

### 10.1 Natural Language over Structured Commands

**Decision:** Prioritize natural language input over slash commands.

**Rationale:**
- More intuitive for players
- Leverages LLM capabilities
- Reduces memorization burden
- More flexible and expressive

**Trade-offs:**
- Slightly slower processing
- Occasional misunderstandings
- Requires API connectivity

### 10.2 GUI Panel over Chat Spam

**Decision:** Use dedicated GUI panel for most feedback.

**Rationale:**
- Keeps chat clean
- Richer feedback options
- Scrollable history
- Better organization

**Trade-offs:**
- Players must open panel
- Less immediately visible
- Requires screen space

### 10.3 Push-to-Talk over Continuous Listening

**Decision:** Default to push-to-talk for voice.

**Rationale:**
- Privacy conscious
- Clear activation
- Fewer accidental triggers
- Resource efficient

**Trade-offs:**
- Requires key hold
- Less natural conversation
- Two-handed operation

### 10.4 Explicit Targeting over Smart Guessing

**Decision:** Require explicit agent targeting in multi-agent scenarios.

**Rationale:**
- Avoids confusion
- Clear intent
- Prevents mistakes
- Teachable pattern

**Trade-offs:**
- More verbose commands
- Requires agent name knowledge
- Less "magical" feel

---

## 11. Future Enhancements

### 11.1 Command Shortcuts
```
/mine 10 iron               # Quick commands
/house                       # Common tasks
/follow                      # Basic actions
```

### 11.2 Command Macros
```
/setup base                 # Predefined sequences
/farm starter
/defensive perimeter
```

### 11.3 Conditional Commands
```
"Build a house if you have materials"
"Mine iron until you have 64"
"Follow me until I say stop"
```

### 11.4 Relative Positioning
```
"Build a house to my left"
"Mine the ore above me"
"Go to the cave north of here"
```

### 11.5 Contextual Commands
```
"This one"                  # Refers to targeted block
"That way"                  # Refers to look direction
"Like the last one"         # Refers to previous task
```

---

## 12. User Research Questions

To validate this design, we should investigate:

1. **Command Preference**: Do players prefer natural language or structured commands?
2. **Feedback Channels**: Which feedback methods are most noticeble?
3. **Multi-Agent Mental Model**: How do players think about coordinating multiple agents?
4. **Voice Acceptance**: Will players use voice commands in-game?
5. **Error Tolerance**: How forgiving should the system be of misunderstood commands?
6. **Learning Curve**: How long does it take to become proficient?
7. **Accessibility Needs**: What accommodations are most important?

---

## Appendix A: Command Reference

### Quick Reference Card

```
SPAWN AGENT
  /minewright spawn [name]

SEND COMMAND
  /minewright tell <name> <command>
  Or press K and type natural language

CHECK STATUS
  /minewright status
  /minewright list

EMERGENCY STOP
  /minewright stop <name>
  /minewright stop all

VOICE CONTROL
  /minewright voice on|off|status
  Press V to talk

RELATIONSHIP
  /minewright relationship <name>
```

---

## Appendix B: Example Workflows

### Scenario 1: First Time Setup

```
1. Player joins world
2. /minewright spawn Bob
3. Bob: "Hi! I'm Bob. Ready to help!"
4. Player presses K, types: "Build a simple house"
5. Bob: "On it! Building a 5x5 wood house."
6. Player watches Bob work
```

### Scenario 2: Resource Gathering Team

```
1. Player: /minewright spawn MineWright
2. Player: /minewright spawn Alex
3. Player: /minewright spawn Miner
4. Player presses K: "MineWright, gather wood"
5. Player presses K: "Alex, mine stone"
6. Player presses K: "Miner, find iron"
7. All three work simultaneously
```

### Scenario 3: Large Construction Project

```
1. Player spawns 5 agents
2. Player: "Everyone, gather materials"
3. Agents spread out gathering
4. Player: "All MineWrights, return here"
5. Player: "Work together to build a castle"
6. Agents collaborate on construction
```

---

**Document Status:** Ready for Review
**Next Steps:**
1. User testing with natural language commands
2. Implement Phase 2 feedback enhancements
3. Prototype voice integration
4. Accessibility audit

---

*This document is a living design. Feedback and iteration welcome.*
