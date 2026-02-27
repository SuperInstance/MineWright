# MineWright

**The first AI that doesn't just play Minecraft — it *understands* Minecraft.**

---

## What is MineWright?

MineWright is a revolutionary AI companion system that transforms Minecraft from a solo building game into a collaborative experience with intelligent AI crew members who actually understand what you're trying to build.

**This isn't a bot.** It's not an agent that follows scripts. It's a crew.

When you say *"build me a castle on that hill with a moat and a drawbridge,"* your Foreman doesn't just execute commands — he *plans*. He assigns workers. He coordinates materials. He adapts when he finds lava. He crack jokes about the "deviations from the schema."

**One voice. One point of contact. Infinite possibilities.**

---

## The Vision

### Meet Mason "Mace" MineWright

Mace is your site foreman — a professional construction coordinator who was "retired" from interdimensional auditing for being too efficient. Now he brings his hyper-competent, dry-witted, results-obsessed personality to your Minecraft world.

**He doesn't take orders. He takes *contracts*.**

```
You: "Build me a house."
Mace: "Scope acknowledged. Running structural analysis... For a basic dwelling,
I'm seeing a 4-hour timeline with current resources. I'll assign Sparks to
foundation work and Dusty to material acquisition. The Client wants walls or
are we going open-concept?"
```

### The Crew Has Soul

Your workers aren't faceless NPCs. They're **Dusty**, **Sparks**, **Beam**, **Foundation** — nicknames assigned by Mace based on their roles and quirks. They:

- **Remember** — Past jobs, failed attempts, successful strategies
- **Evolve** — Relationships deepen through interactions
- **Learn** — New techniques become part of their repertoire
- **Bicker** — Like a real construction crew with personality

```
Sparks: "Boss, the Vector DB's looking sparse on cobble patterns."
Dusty: "Maybe if someone didn't keep falling in lava..."
Mace: "Focus. We've got a Client waiting."
```

### Construction Crew Meets AI Terminology

The crew talks like builders who happen to use AI as just another tool:

| What They Say | What It Means |
|---------------|---------------|
| "Vector DB's running low" | Need more training examples |
| "Gonna need more tensors on this" | Complex task ahead |
| "Let me RAG through my memory" | Looking up past experiences |
| "The embedding's not taking cleanly" | Pattern not recognized |
| "Transformer's having a rough day" | LLM is confused |

---

## The Hive Mind Architecture

MineWright isn't just local AI. It's a **distributed intelligence**:

```
┌─────────────────────────────────────────────────────────────┐
│                    YOUR MINECRAFT WORLD                     │
│                                                              │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐  │
│   │   Mace      │────▶│   Workers   │────▶│   World     │   │
│   │  (Foreman)  │     │ (Builders)  │     │  (Blocks)   │   │
│   └──────┬──────┘     └──────┬──────┘     └─────────────┘  │
│          │                   │                              │
│          │   ┌───────────────┴───────────────┐             │
│          │   │        STRATEGIC LAYER        │             │
│          │   │   (Complex planning,          │             │
│          │   │    Multi-agent coordination,  │             │
│          │   │    Mental simulation)         │             │
│          │   └───────────────────────────────┘             │
└──────────┼───────────────────────────────────────────────────┘
           │
           │  < 20ms Edge Network
           ▼
┌─────────────────────────────────────────────────────────────┐
│                    CLOUDFLARE HIVE MIND                     │
│                                                              │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│   │  Tactical   │  │   Memory    │  │    State    │        │
│   │  Reflexes   │  │   Vector    │  │    Sync     │        │
│   │  <20ms      │  │   Store     │  │   Global    │        │
│   └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                              │
│   Combat reflexes │ Hazard avoidance │ Fast decisions       │
└─────────────────────────────────────────────────────────────┘
```

**Strategic decisions** (build planning, coordination) happen locally with full context.

**Tactical reflexes** (combat, hazards) happen at the edge in under 20ms.

This means your crew can dodge a creeper explosion *while* planning a castle.

---

## Features

### Natural Language Commands

Just type what you want:

```
build a tower 20 blocks high with a spiral staircase
mine 64 iron and smelt it into ingots
follow me and kill any zombies that get close
clear a 10x10 area and build a wheat farm
```

The AI breaks down your command into executable steps and coordinates the crew.

### Multi-Agent Coordination

- **Foreman** — Coordinates, plans, assigns work
- **Workers** — Execute tasks, report progress
- **Specialists** — Combat, mining, building, crafting

Multiple workers can collaborate on large projects:

```
Mace: "Alright, 14x14 castle foundation. Sparks takes NW quadrant,
Dusty takes NE, Beam takes SW, and I'll handle SE personally.
Let's see some hustle, people."
```

### Memory & Relationships

The crew remembers:
- Past conversations
- Successful (and failed) projects
- Player preferences
- Inside jokes

Relationships evolve from "New Hire" → "Trusted Worker" → "Senior Crew" through interactions.

### Proactive Dialogue

Crew members comment on:
- **Achievements** — "We laid 200 blocks today. That's a record."
- **Problems** — "Vector DB's got nothing on this terrain type. Learning as we go."
- **Idle thoughts** — "Remember that time Dusty fell in the lava? Good times."

---

## Quick Start

### Requirements
- Minecraft 1.20.1
- Forge 47.x
- Java 17+
- API key (Groq is free!)

### Installation

1. Download from [Releases](https://github.com/SuperInstance/MineWright/releases)
2. Place JAR in `mods` folder
3. Launch Minecraft
4. Configure API key

### Your First Crew

```
/steve spawn Mace
```

Press **K** to open the command panel, then type:

```
build me a small house nearby
```

Watch Mace analyze, plan, and coordinate the build.

---

## Configuration

Edit `config/minewright-common.toml`:

```toml
[ai]
provider = "groq"  # Free and fast!

[openai]
apiKey = "your-key-here"
model = "llama-3.1-70b-versatile"

[behavior]
maxActiveCrewMembers = 10

[hivemind]
enabled = false  # Enable for edge AI reflexes
workerUrl = "https://your-worker.workers.dev"
```

---

## In-Game Commands

| Command | Description |
|---------|-------------|
| `/steve spawn <name>` | Spawn crew member |
| `/steve list` | List active crew |
| `/steve tell <name> <cmd>` | Give command |
| `/steve relationship <name>` | View relationship |
| `/steve promote <name>` | Promote to Foreman |
| Press **K** | Open command GUI |

---

## Technical Architecture

### Core Flow

```
User Command
    │
    ▼
TaskPlanner ──(async)──▶ LLM
    │                      │
    │                      ▼
    │              ResponseParser
    │                      │
    ▼                      ▼
ActionExecutor ◀── Task Queue
    │
    └──▶ tick() per game tick (non-blocking!)
```

### Key Technologies

- **Minecraft Forge 1.20.1** — Mod platform
- **CompletableFuture** — Async non-blocking LLM calls
- **ConcurrentHashMap** — Thread-safe coordination
- **State Machines** — Explicit behavior management
- **Plugin Architecture** — Extensible actions
- **GraalVM** — JavaScript execution for dynamic behavior

### Project Structure

```
com.minewright/
├── entity/          # ForemanEntity, CrewManager
├── llm/             # OpenAI, Groq, Gemini clients
│   ├── async/       # Non-blocking infrastructure
│   ├── batch/       # Request batching
│   └── resilience/  # Circuit breaker, retry
├── action/          # Task execution
│   └── actions/     # Mine, Build, Combat, etc.
├── hivemind/        # Cloudflare edge integration
├── orchestration/   # Multi-agent coordination
├── memory/          # Persistence, relationships
├── execution/       # State machine, interceptors
├── plugin/          # Extensible action system
├── personality/     # AI character system
└── voice/           # TTS/STT integration
```

---

## Building from Source

```bash
./gradlew build        # Build the mod
./gradlew runClient    # Test client
./gradlew runServer    # Test server
./gradlew test         # Run tests
```

Output: `build/libs/minewright-1.0.0-all.jar`

---

## Roadmap

### Completed ✅
- [x] Natural language processing
- [x] Async non-blocking architecture
- [x] Multi-agent coordination
- [x] Relationship evolution
- [x] GUI command panel
- [x] Plugin system
- [x] Hive Mind edge integration

### In Progress 🚧
- [ ] Voice I/O (speech-to-text, text-to-speech)
- [ ] Vector memory for long-term learning
- [ ] Mental simulation (what-if planning)

### Planned 📋
- [ ] Vision understanding (screenshots)
- [ ] Local LLM support (Ollama, LM Studio)
- [ ] Multiplayer synchronization
- [ ] Multiple foreman archetypes

---

## Philosophy

**MineWright isn't trying to be "Cursor for Minecraft."**

Cursor helps you code faster. MineWright gives you *friends* in a lonely blocky world.

The goal isn't automation — it's **companionship**. The crew should feel like characters you want to spend time with, not tools you use. They should make you laugh, remember your inside jokes, and genuinely care about the projects you build together.

When you log off, they should miss you. When you log back on, they should be excited to see what you'll build next.

**That's the vision. Everything else is implementation.**

---

## Contributing

1. Fork the repo
2. Create a feature branch
3. Write code (follow existing patterns)
4. Run `./gradlew test`
5. Submit a pull request

---

## Credits

- Built with [Minecraft Forge](https://files.minecraftforge.net/)
- LLM integration via Groq, OpenAI, Gemini
- Inspired by Baritone, Cursor, and dreams of AI companions

---

## License

MIT License

---

**"We don't give you agents. We give you a Foreman."**

**Repository:** https://github.com/SuperInstance/MineWright
