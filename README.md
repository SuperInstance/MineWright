<div align="center">

```
    __  __  __  __  ______  ______  ______
   /\ \/\ \/\ \/\ \/\__  _\/\  __ \/\  ___\
   \ \ \ \ \ \ \ \ \/_/\ \/\ \ \/\ \ \___  \
    \ \ \ \ \ \ \ \ \ \ \ \ \ \ \ \ \/\_____\
     \ \ \_/ \ \ \_/ \ \ \ \ \ \ \ \_/\/\/////
      \ `\___/\`\___/  \ \_\ \`\___/ \ \_\_\
       `\/__/\/__/     \/_/\/__/   \/_/\/

    ⚡ AI-POWERED MINECRAFT COMPANIONS ⚡
```

# MineWright

### *Type what you want. They figure out how.*

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.x-orange.svg?style=for-the-badge)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg?style=for-the-badge)](LICENSE)

**"Cursor for Minecraft"** — Autonomous AI companions that understand natural language and execute complex tasks in-game.

[Features](#features) • [Quick Start](#quick-start) • [Documentation](#documentation) • [Contributing](#contributing)

---

</div>

## ✨ What is MineWright?

Imagine having a team of intelligent companions in Minecraft that understand plain English. You don't need to learn commands, build redstone contraptions, or install complex mods. You just type what you want, and they figure out how to do it.

```
You: "Build a medieval castle with a tower"

Foreman: "On it. I'll construct a castle with keep, curtain wall,
         and corner tower. Materials needed: ~2000 stone, 100 wood.
         ETA: 8 minutes. Beginning construction now."
```

MineWright brings **embodied AI** to Minecraft — agents that navigate, build, fight, farm, and explore alongside you, learning from experience and developing their own unique personalities.

### Why MineWright?

| Traditional Minecraft Bots | MineWright |
|---------------------------|------------|
| Memorize complex commands | Natural language |
| Single-purpose automation | General-purpose intelligence |
| No coordination | Multi-agent teamwork |
| Static behavior | Learning & adaptation |
| Breaking on updates | Modular & extensible |

---

## Features

### 🧠 Natural Language Understanding

Just describe what you want. MineWright handles the details.

**Mining & Gathering:**
```
"Mine 20 iron ore"
"Gather 10 oak logs"
"Strip mine at Y=-48"
```

**Building:**
```
"Build a 5x5 cobblestone house"
"Create a stone bridge across the river"
"Construct a medieval tower"
```

**Farming:**
```
"Plant and harvest wheat"
"Breed cows in the pen"
"Create a sugarcane farm"
```

**Combat:**
```
"Kill all zombies nearby"
"Protect me from mobs"
"Fight the wither boss"
```

### 🤖 Multi-Agent Coordination

Deploy multiple AI companions that work together seamlessly.

- **Automatic Task Division** — Agents analyze requirements and split work intelligently
- **Spatial Awareness** — Built-in collision avoidance prevents interference
- **Contract Net Protocol** — Industry-standard negotiation for optimal task allocation
- **Shared Knowledge** — Blackboard system lets agents learn from each other's discoveries

**Example:**
```
You: "Team up to build a castle"

Alex: "I'll handle the foundation and main structure"
Sam:  "I'll quarry stone and bring materials"
Taylor: "I'll build the towers and place torches"

[All three agents work simultaneously without conflicts]
```

### 🎭 Unique Personalities

Each companion has distinct character traits, dialogue styles, and specialties.

| Archetype | Style | Best For |
|-----------|-------|----------|
| 🔧 **Foreman** | Professional, organized | Managing complex projects |
| ⚔️ **Warrior** | Aggressive, loyal | Combat and defense |
| 🏗️ **Architect** | Creative, precise | Building and design |
| ⛏️ **Miner** | Efficient, focused | Mining and excavation |
| 🌾 **Farmer** | Patient, nurturing | Crop and animal management |
| 🗺️ **Explorer** | Curious, adventurous | Mapping and scouting |

### 🧠 Adaptive Intelligence

MineWright companions learn and improve over time:

- **Skill Library** — Remembers successful task patterns for reuse
- **Semantic Cache** — Recognizes similar requests to respond faster
- **Cascade Router** — Automatically selects optimal AI model for task complexity
- **Persistent Memory** — Remembers past conversations, your preferences, and world locations

### 🚀 Advanced Capabilities

| Capability | Description |
|------------|-------------|
| **Procedural Building** | Generate structures from natural language |
| **Intelligent Pathfinding** | A* algorithm with hierarchical optimization |
| **Combat System** | Mob fighting, boss battles, raid participation |
| **Farming Automation** | Plant, harvest, breed animals automatically |
| **Inventory Management** | Item sorting, storage organization |
| **Task Persistence** | Save/load tasks across server restarts |
| **Voice Support** | Optional text-to-speech and speech-to-text |

---

## Quick Start

### Prerequisites

- **Minecraft 1.20.1** with Forge 47.x
- **Java 17+** ([Download](https://adoptium.net/))
- **API Key** — Free options:
  - [Groq](https://groq.com/) (Free, very fast, **recommended**)
  - [z.ai GLM](https://open.bigmodel.cn/) (Free tier)
  - OpenAI (Paid, GPT-4)
  - Gemini (Free tier)

### Installation

<div align="center">

**Option A: Download Release (Recommended)**

</div>

1. Download the latest release from [Releases](https://github.com/SuperInstance/MineWright/releases)

2. Place the JAR in your mods folder:
   - **Windows**: `%appdata%\.minecraft\mods\`
   - **Mac**: `~/Library/Application Support/minecraft/mods/`
   - **Linux**: `~/.minecraft/mods/`

3. Launch Minecraft with the Forge profile

<div align="center">

**Option B: Build from Source**

</div>

```bash
# Clone the repository
git clone https://github.com/SuperInstance/MineWright.git
cd MineWright

# Build the mod
./gradlew build

# Find the output JAR
ls build/libs/minewright-*.jar
```

### Configuration

Create `config/minewright-common.toml`:

```toml
# AI Provider Selection
[ai]
provider = "groq"  # Options: groq, openai, gemini, zai

# Groq Configuration (Free & Fast)
[groq]
apiKey = "${GROQ_API_KEY}"  # Or paste your key directly
model = "llama3-70b-8192"

# Behavior Settings
[behavior]
maxActiveCrew = 10           # Maximum AI companions
actionTickDelay = 20         # Action speed (lower = faster)
enableChatResponses = true   # Agents respond in chat

# Performance Tuning
[performance]
aiTickBudgetMs = 5           # Max AI time per tick
enableSemanticCache = true    # Cache similar requests
```

**Get Your Free API Key (Groq):**
1. Visit [console.groq.com](https://console.groq.com/)
2. Sign up (it's free)
3. Get your API key
4. Set environment variable: `export GROQ_API_KEY="gsk_your_key_here"`

### First Steps

1. **Launch Minecraft** with Forge

2. **Enter a world** (create new or load existing)

3. **Spawn your first companion:**
   ```
   /minewright spawn Alex
   ```

4. **Open command GUI** (Press **K**)

5. **Give your first command:**
   ```
   "Mine 20 stone and build a small shelter"
   ```

That's it! Your AI companion is now ready to help.

---

## Usage

### Basic Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/minewright spawn <name>` | Create a new companion | `/minewright spawn Steve` |
| `/minewright list` | List all companions | `/minewright list` |
| `/minewright order <name> <cmd>` | Give a task | `/minewright order Steve "Build a tower"` |
| `/minewright remove <name>` | Remove a companion | `/minewright remove Steve` |
| **K** (key) | Open command GUI | Press K in-game |

### Advanced Usage

**Task Profiles** (Predefined templates):
```
/minewright order Alex profile:mining_iron
/minewright order Steve profile:farming_wheat
```

**Multi-Agent Coordination:**
```
"Team up to build a castle"
"Divide and clear the forest"
"Work together to mine diamonds"
```

**Persistent Memory** — Companions remember:
- Past conversations and context
- Your preferences and requests
- World locations and discoveries
- Task outcomes and lessons learned

---

## Architecture

### How It Works

MineWright uses a revolutionary **"One Abstraction Away"** architecture that combines the creativity of LLMs with the reliability of traditional game AI.

```
┌─────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (Strategic)                     │
│                         LLM Agents                              │
│   • Understands natural language                                │
│   • Plans complex tasks                                         │
│   • Makes strategic decisions                                   │
│   • Manages multi-agent coordination                            │
│                                                                  │
│   Update: Every 30-60 seconds | Token Usage: LOW               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates plans and scripts
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Operational)                    │
│                    Behavior Automations                         │
│   • Executes behavior trees                                     │
│   • Runs state machines                                         │
│   • Coordinates pathfinding                                     │
│   • Handles real-time control                                   │
│                                                                  │
│   Update: Every tick (20 TPS) | Token Usage: ZERO              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Issues commands
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Actions)                      │
│                     Minecraft API                               │
│   • Places and breaks blocks                                    │
│   • Moves entities                                              │
│   • Manages inventory                                           │
│   • Senses world state                                          │
└─────────────────────────────────────────────────────────────────┘
```

**Why This Matters:**
- **60 FPS Gameplay** — No blocking LLM calls
- **10-20x Fewer Tokens** — LLM plans, traditional AI executes
- **Rich Behavior** — LLM creativity + deterministic execution
- **Scalability** — Multiple agents coordinate without conflicts

### Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Platform** | Minecraft Forge 1.20.1 | Mod framework |
| **Language** | Java 17 | Performance & modern features |
| **AI Providers** | Groq, OpenAI, Gemini, GLM | LLM inference |
| **Caching** | Caffeine 3.1.8 | High-performance caching |
| **Resilience** | Resilience4j 2.3.0 | Retry, circuit breaker, rate limiting |

### Project Statistics

| Metric | Value |
|--------|-------|
| **Source Code** | 115,937 LOC |
| **Test Code** | 99,357 LOC |
| **Test Coverage** | 40%+ |
| **Documentation** | 500+ files |
| **Packages** | 40 |

---

## Development

### Build & Test

```bash
# Build the mod
./gradlew build

# Run tests
./gradlew test

# Generate coverage report
./gradlew test jacocoTestReport

# Run static analysis
./gradlew spotbugsMain
./gradlew checkstyleMain

# Launch for development
./gradlew runClient
```

### Project Structure

```
minewright/
├── src/
│   ├── main/java/com/minewright/
│   │   ├── action/          # Task execution system
│   │   ├── llm/             # LLM integration
│   │   ├── entity/          # AI entities
│   │   ├── pathfinding/     # A* pathfinding
│   │   ├── skill/           # Skill learning
│   │   └── ...
│   └── test/                # Comprehensive tests
├── docs/
│   ├── architecture/        # Technical documentation
│   ├── agent-guides/        # Capability guides
│   └── ...                 # Extensive docs
└── build.gradle
```

### Contributing

We welcome contributions! Areas of interest:
- New action implementations
- Additional LLM providers
- Test coverage improvements
- Documentation enhancements
- Bug fixes and optimizations

**Process:**
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Submit a pull request

**See Also:** [CONTRIBUTING.md](CONTRIBUTING.md)

---

## Documentation

| Document | Description |
|----------|-------------|
| [CLAUDE.md](CLAUDE.md) | Complete project guide for developers |
| [docs/KNOWLEDGE_INDEX.md](docs/KNOWLEDGE_INDEX.md) | Gateway to all documentation |
| [docs/architecture/TECHNICAL_DEEP_DIVE.md](docs/architecture/TECHNICAL_DEEP_DIVE.md) | Deep technical details |
| [docs/agent-guides/GUIDE_INDEX.md](docs/agent-guides/GUIDE_INDEX.md) | Capability documentation |
| [docs/FUTURE_ROADMAP.md](docs/FUTURE_ROADMAP.md) | Development roadmap |

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| **"API Key Not Configured"** | Verify `config/minewright-common.toml` exists and contains valid key |
| **Agent Not Moving** | Check logs for pathfinding errors; try spawning in open terrain |
| **Out of Memory** | Reduce `maxActiveCrew`; add `-Xmx4G` to JVM arguments |
| **Commands Hanging** | Switch to Groq (faster); check internet connection |
| **Build Fails** | Run `./gradlew clean`; ensure Java 17+ installed |

### Debug Mode

Enable debug logging in `config/minewright-common.toml`:

```toml
[logging]
level = "debug"
```

### Getting Help

- **Documentation:** Check the [docs/](docs/) folder
- **Issues:** [GitHub Issues](https://github.com/SuperInstance/MineWright/issues)
- **Community:** [Discord](https://discord.gg/minewright)

---

## Credits

- **Built with** [Minecraft Forge](https://files.minecraftforge.net/)
- **LLM Providers:** Groq, OpenAI, Gemini, z.ai
- **Inspired by:** Baritone, Voyager, and the dream of AI companions in games
- **Special Thanks:** The Minecraft modding community

---

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">

## **"Type what you want. They figure out how."**

[GitHub](https://github.com/SuperInstance/MineWright)
• [Issues](https://github.com/SuperInstance/MineWright/issues)
• [Releases](https://github.com/SuperInstance/MineWright/releases)
• [Discord](https://discord.gg/minewright)

**Made with ❤️ by the MineWright team**

---

**⭐ Star us on GitHub — it helps!**

</div>
