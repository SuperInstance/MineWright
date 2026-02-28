# MineWright

<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.x-orange.svg)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

**The first AI that doesn't just play Minecraft — it *understands* Minecraft.**

[Features](#features) • [Quick Start](#quick-start) • [Documentation](#documentation) • [Architecture](#technical-architecture)

</div>

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

| What They Say | What It Means |
|---------------|---------------|
| "Vector DB's running low" | Need more training examples |
| "Gonna need more tensors on this" | Complex task ahead |
| "Let me RAG through my memory" | Looking up past experiences |
| "The embedding's not taking cleanly" | Pattern not recognized |
| "Transformer's having a rough day" | LLM is confused |

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

### Multi-Agent Coordination

- **Foreman** — Coordinates, plans, assigns work
- **Workers** — Execute tasks, report progress
- **Specialists** — Combat, mining, building, crafting

### Memory & Relationships

The crew remembers conversations, projects, preferences, and inside jokes. Relationships evolve from "New Hire" → "Trusted Worker" → "Senior Crew" through interactions.

### Vision Understanding

The crew can analyze screenshots to understand the world:
- "What do you see ahead?" — General scene analysis
- "Is this area safe?" — Threat detection
- "Where should I build?" — Terrain assessment

### Voice Integration

Speech-to-text and text-to-speech support:
- **Whisper STT** — Accurate voice recognition
- **ElevenLabs TTS** — High-quality voice synthesis via Docker MCP

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
/minewright spawn Mace
```

Press **K** to open the command panel, then type:

```
build me a small house nearby
```

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
```

---

## In-Game Commands

| Command | Description |
|---------|-------------|
| `/minewright spawn <name>` | Spawn crew member |
| `/minewright list` | List active crew |
| `/minewright tell <name> <cmd>` | Give command |
| `/minewright relationship <name>` | View relationship |
| `/minewright promote <name>` | Promote to Foreman |
| Press **K** | Open command GUI |

---

## Documentation

| Resource | Description |
|----------|-------------|
| [Architecture](docs/architecture/) | System design and patterns |
| [Guides](docs/guides/) | How-to guides and tutorials |
| [Research](docs/research/) | AI research and analysis |
| [Reports](docs/reports/) | Audit and review reports |

---

## Technical Architecture

```
User Command
    │
    ├─► Skill Library (semantic search for learned patterns)
    │   └─► Skill found? → Execute directly (skip LLM)
    │
    ├─► Smart Cascade Router (complexity analysis)
    │   ├─► Simple task → Local LLM (SmolVLM) - FREE
    │   ├─► Moderate task → glm-4.7-air - FAST
    │   └─► Complex task → glm-5 - CAPABLE
    │
    ▼
TaskPlanner ──(async)──▶ LLM
    │                      │
    │                      ▼
    │              ResponseParser
    │                      │
    ├─► Utility AI (task prioritization)
    │
    ├─► Contract Net Protocol (multi-agent task allocation)
    │
    ▼
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
- **Resilience4j** — Circuit breaker, retry patterns
- **Caffeine Cache** — High-performance caching

### Advanced AI Systems

| System | Purpose | Benefit |
|--------|---------|---------|
| **Skill Library** | Self-improving code patterns | 40-60% fewer LLM calls |
| **Smart Cascade Router** | Complexity-based model selection | 40-60% cost reduction |
| **Utility AI** | Multi-factor task prioritization | Smarter decisions |
| **Contract Net Protocol** | Competitive task bidding | Efficient allocation |
| **Blackboard System** | Shared knowledge space | Emergent coordination |
| **Semantic Cache** | Embedding-based response reuse | 30-50% fewer API calls |
| **Vision Pipeline** | Screenshot analysis | Visual understanding |

### Project Structure

```
com.minewright/
├── entity/          # ForemanEntity, CrewManager
├── llm/             # OpenAI, Groq, Gemini, z.ai clients
│   ├── async/       # Non-blocking infrastructure
│   ├── batch/       # Request batching
│   ├── cascade/     # Complexity routing, model selection
│   ├── cache/       # Semantic caching with embeddings
│   └── resilience/  # Circuit breaker, retry
├── action/          # Task execution
├── execution/       # State machine, interceptors, event bus
├── coordination/    # Contract Net Protocol, multi-agent
├── decision/        # Utility AI, task prioritization
├── blackboard/      # Shared knowledge system
├── skill/           # Skill library, skill generation
├── pathfinding/     # Enhanced A*, hierarchical planning
├── communication/   # Inter-agent messaging, protocols
├── memory/          # Persistence, relationships
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

Output: `build/libs/minewright-1.0.0.jar`

---

## Roadmap

### Completed
- [x] Natural language processing
- [x] Async non-blocking architecture
- [x] Multi-agent coordination
- [x] Relationship evolution
- [x] GUI command panel
- [x] Plugin system
- [x] Skill Library System
- [x] Cascade Router
- [x] Utility AI
- [x] Contract Net Protocol
- [x] Blackboard System
- [x] Semantic Cache
- [x] Enhanced Pathfinding
- [x] Agent Communication Protocol
- [x] Vision understanding (screenshots)
- [x] Smart Cascade Router with local LLM
- [x] Voice I/O (Whisper STT + ElevenLabs TTS)

### In Progress
- [ ] Vector memory for long-term learning
- [ ] Mental simulation (what-if planning)
- [ ] Test coverage for new systems

### Planned
- [ ] Local LLM support (vLLM, Ollama)
- [ ] Multiplayer synchronization
- [ ] Multiple foreman archetypes
- [ ] HTN planner for complex task decomposition

---

## Philosophy

**MineWright isn't trying to be "Cursor for Minecraft."**

Cursor helps you code faster. MineWright gives you *friends* in a lonely blocky world.

The goal isn't automation — it's **companionship**. The crew should feel like characters you want to spend time with, not tools you use. They should make you laugh, remember your inside jokes, and genuinely care about the projects you build together.

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
- LLM integration via Groq, OpenAI, Gemini, z.ai
- Voice via Whisper and ElevenLabs
- Inspired by Baritone, Cursor, and dreams of AI companions

---

## License

MIT License

---

<div align="center">

**"We don't give you agents. We give you a Foreman."**

[GitHub](https://github.com/SuperInstance/MineWright) • [Issues](https://github.com/SuperInstance/MineWright/issues) • [Releases](https://github.com/SuperInstance/MineWright/releases)

</div>
