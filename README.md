# MineWright

<div align="center">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.x-orange.svg)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

**AI Companions for Minecraft**

*Type what you want. They figure out how.*

[Quick Start](#quick-start) • [Commands](#commands) • [Configuration](#configuration) • [Architecture](#architecture)

</div>

---

## What is MineWright?

MineWright adds intelligent AI companions to your Minecraft world. Unlike scripted bots, these companions:

- **Understand natural language** — "Build a house" just works
- **Remember conversations** — They learn your preferences
- **Have personalities** — Each one is unique
- **Work together** — Multiple agents coordinate on complex tasks

```
You: "Mine 20 stone and build a small shelter"
Foreman: "Got it. I'll mine the stone first, then construct a 5x5 basic shelter.
         Should have it done in about 3 minutes."
```

---

## Quick Start

### Requirements
- Minecraft 1.20.1
- Forge 47.x
- Java 17+
- An API key (Groq is free, z.ai/GLM recommended)

### Installation

1. **Download** the latest release from [Releases](https://github.com/SuperInstance/MineWright/releases)

2. **Install** into your mods folder:
   - Windows: `%appdata%\.minecraft\mods\`
   - Mac: `~/Library/Application Support/minecraft/mods/`
   - Linux: `~/.minecraft/mods/`

3. **Configure** your API key in `config/minewright-common.toml`:
   ```toml
   [ai]
   provider = "groq"

   [groq]
   apiKey = "${GROQ_API_KEY}"  # Or paste your key directly
   model = "llama3-70b-8192"
   ```

4. **Launch** Minecraft with the Forge profile

5. **Spawn** your first companion:
   ```
   /minewright spawn Alex
   ```

6. **Give commands**:
   ```
   /minewright order Alex "Mine stone"
   ```

### Environment Variables (Recommended)

**Windows PowerShell:**
```powershell
$env:GROQ_API_KEY="gsk_your_key_here"
```

**Linux/Mac:**
```bash
export GROQ_API_KEY="gsk_your_key_here"
```

---

## Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/minewright spawn <name>` | Spawn a companion | `/minewright spawn Steve` |
| `/minewright list` | List all companions | `/minewright list` |
| `/minewright order <name> <cmd>` | Give a task | `/minewright order Steve "Build a tower"` |
| `/minewright remove <name>` | Remove a companion | `/minewright remove Steve` |
| **K** (key) | Open command GUI | Press K in-game |

### Natural Language Tasks

Just describe what you want:

```
mine 20 iron ore
build a small house with a door
follow me and kill zombies
gather wood from the forest
craft 10 torches
clear a 10x10 area
farm wheat and replant
```

---

## Capabilities

| Category | Tasks |
|----------|-------|
| **Mining** | Dig ores, tunnel, strip mine, quarry |
| **Building** | Houses, towers, walls, bridges |
| **Gathering** | Wood, crops, mob drops, items |
| **Combat** | Kill mobs, defend area, protect player |
| **Farming** | Plant, harvest, breed animals |
| **Crafting** | Tools, weapons, armor, items |
| **Navigation** | Follow, lead, explore, patrol |

---

## Personalities

Each companion has a unique personality archetype:

| Type | Style |
|------|-------|
| **Architect** | Creative, precise, loves building |
| **Miner** | Efficient, focused, digs fast |
| **Farmer** | Patient, nurturing, grows crops |
| **Warrior** | Aggressive, loyal, combat ready |
| **Explorer** | Curious, adventurous, maps terrain |
| **Crafter** | Detail-oriented, makes items |
| **Gatherer** | Thorough, organized, collects resources |
| **Scholar** | Analytical, helpful, researches |

---

## Configuration

Edit `config/minewright-common.toml`:

```toml
[ai]
provider = "groq"  # groq, openai, gemini, zai

[groq]
apiKey = "${GROQ_API_KEY}"
model = "llama3-70b-8192"

[behavior]
maxActiveCrewMembers = 10
actionTickDelay = 20
enableChatResponses = true

[performance]
aiTickBudgetMs = 5
```

### API Providers

| Provider | Cost | Speed | Best For |
|----------|------|-------|----------|
| **Groq** | Free | Very Fast | Testing, quick tasks |
| **z.ai GLM** | Free tier | Fast | Production use |
| **OpenAI** | Paid | Moderate | Complex reasoning |
| **Gemini** | Free tier | Moderate | Alternative option |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      BRAIN LAYER (LLM)                       │
│                                                              │
│  • Natural language understanding                           │
│  • Task planning and strategy                               │
│  • Conversation and personality                             │
│                                                              │
│  Update: Every 30-60s or on events                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Generates
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    SCRIPT LAYER (Local)                      │
│                                                              │
│  • Behavior trees, state machines                           │
│  • Pathfinding, mining, building patterns                   │
│  • Reactive behaviors                                       │
│                                                              │
│  Update: Every tick (20 TPS)                                │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Executes
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (API)                       │
│                                                              │
│  • Block interactions, movement, inventory                  │
│  • Entity tracking, world sensing                           │
└─────────────────────────────────────────────────────────────┘
```

This "One Abstraction Away" design means:
- **No blocking** — Game runs at 60 FPS even during LLM calls
- **10-20x fewer tokens** — LLM plans, scripts execute
- **Characterful AI** — Rich personalities, ongoing dialogue

### Key Systems

| System | Purpose |
|--------|---------|
| **Skill Library** | Learns from successful tasks, reuses patterns |
| **Cascade Router** | Picks the right model for task complexity |
| **Semantic Cache** | Remembers similar requests, saves API calls |
| **Blackboard** | Shared knowledge between agents |
| **Contract Net** | Multi-agent task coordination |

---

## Building from Source

```bash
# Clone
git clone https://github.com/SuperInstance/MineWright.git
cd MineWright

# Build
./gradlew build

# Output: build/libs/minewright-1.0.0.jar
```

### Development Commands

```bash
./gradlew build      # Build the mod
./gradlew test       # Run tests
./gradlew runClient  # Launch test client
./gradlew runServer  # Launch test server
```

---

## Project Stats

| Metric | Value |
|--------|-------|
| Source Files | 237 |
| Source Lines | 86,500+ |
| Test Files | 105+ |
| Test Lines | 35,000+ |
| Documentation | 425+ files |
| Packages | 49 |

---

## Troubleshooting

### "API Key Not Configured"
- Check `config/minewright-common.toml` exists
- Verify API key format
- Ensure environment variable is set before launch

### Agent Not Moving
- Check logs for pathfinding errors
- Try spawning in open terrain
- Increase `actionTickDelay` if server is lagging

### Out of Memory
- Reduce `maxActiveCrewMembers`
- Add `-Xmx4G` to JVM arguments
- Close other applications

### Commands Hanging
- Switch to faster provider (Groq)
- Check internet connection
- Verify API quota

---

## Documentation

| Document | Description |
|----------|-------------|
| [CLAUDE.md](CLAUDE.md) | Project guide and architecture |
| [docs/FUTURE_ROADMAP.md](docs/FUTURE_ROADMAP.md) | Development roadmap |
| [docs/ARCHITECTURE_OVERVIEW.md](docs/ARCHITECTURE_OVERVIEW.md) | System design |

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make changes following existing patterns
4. Test: `./gradlew test`
5. Submit a pull request

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

---

## License

MIT License — See [LICENSE](LICENSE)

---

## Credits

- Built with [Minecraft Forge](https://files.minecraftforge.net/)
- LLM providers: Groq, OpenAI, Gemini, z.ai
- Inspired by Baritone, Voyager, and the dream of AI companions

---

<div align="center">

**"Type what you want. They figure out how."**

[GitHub](https://github.com/SuperInstance/MineWright) • [Issues](https://github.com/SuperInstance/MineWright/issues) • [Releases](https://github.com/SuperInstance/MineWright/releases)

</div>
