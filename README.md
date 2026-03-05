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

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.x-orange.svg?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSJjdXJyZW50Q29sb3IiIHN0cm9rZS13aWR0aD0iMiIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIj48cGF0aCBkPSJNMTIgMmwxMC41IDZ2OEwxMiAyMmwtMTAuNS02VjhMMTIgMnoiLz48L3N2Zz4=)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg?style=for-the-badge)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Passing-success.svg?style=for-the-badge)](https://github.com/SuperInstance/MineWright/actions)

### **Type what you want. They figure out how.**

**"Cursor for Minecraft" — Autonomous AI companions that understand natural language and execute complex tasks in-game.**

[Features](#-features) • [Quick Start](#-quick-start) • [Documentation](#-documentation) • [Contributing](#-contributing)

</div>

---

## ✨ Features

### 🧠 Natural Language Understanding
Just describe what you want in plain English. No command blocks, no redstone, no scripting required.

```
You: "Build a medieval castle with a tower"
Foreman: "On it. I'll construct a castle with keep, curtain wall,
          and corner tower. Materials needed: ~2000 stone, 100 wood.
          ETA: 8 minutes."
```

### 🤖 Multi-Agent Coordination
Deploy multiple AI companions that work together seamlessly on complex projects.

- **Automatic Task Division** — Agents split work without conflicts
- **Spatial Awareness** — Built-in collision avoidance and coordination
- **Contract Net Protocol** — Industry-standard multi-agent negotiation
- **Shared Knowledge** — Blackboard system for team intelligence

### 🎭 Unique Personalities
Each companion has distinct character traits, dialogue styles, and specialties.

| Archetype | Style | Best For |
|-----------|-------|----------|
| 🔧 **Foreman** | Professional, organized, "Client"称呼 | Managing complex projects |
| ⚔️ **Warrior** | Aggressive, loyal, combat-focused | Monster hunting, defense |
| 🏗️ **Architect** | Creative, precise, detail-oriented | Building, design projects |
| ⛏️ **Miner** | Efficient, focused, tunnel-vision | Mining expeditions, quarries |
| 🌾 **Farmer** | Patient, nurturing, methodical | Crop management, animal breeding |
| 🗺️ **Explorer** | Curious, adventurous, bold | Mapping, scouting, expeditions |

### 🚀 Production-Grade Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (Strategic)                     │
│                         LLM Agents                              │
│   Planning, strategy, logistics, conversations                  │
│   Token Usage: LOW | Update Frequency: 30-60s or events        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates & Refines
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Operational)                    │
│                    Behavior Automations                         │
│   Behavior trees, FSMs, macro scripts, pathfinding             │
│   Token Usage: ZERO | Update Frequency: Per tick (20 TPS)      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Executes via
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Actions)                      │
│                     Minecraft API                               │
│   Block interactions, movement, inventory, world sensing        │
└─────────────────────────────────────────────────────────────────┘
```

**"One Abstraction Away" Design Philosophy**
- **No Blocking** — Game runs at 60 FPS even during LLM calls
- **10-20x Fewer Tokens** — LLM plans, scripts execute
- **Elegant Architecture** — Clean separation of concerns

### 🎯 Advanced Capabilities

| Capability | Description |
|------------|-------------|
| **🧱 Procedural Building** | Generate structures from natural language descriptions |
| **🛣️ Intelligent Pathfinding** | A* algorithm with hierarchical pathfinding |
| **⚔️ Combat System** | Mob fighting, boss battles, raid participation |
| **🌾 Farming Automation** | Plant, harvest, breed animals automatically |
| **⛏️ Smart Mining** | Ore detection, strip mining, quarry operations |
| **📦 Inventory Management** | Item sorting, storage organization, chest management |
| **🧠 Skill Learning** | Learn from successful tasks, reuse patterns |
| **💬 Natural Dialogue** | Context-aware conversations, memory system |
| **🔄 Task Persistence** | Save/load tasks across server restarts |
| **🎪 Voice Support** | Optional text-to-speech and speech-to-text |

---

## 🚀 Quick Start

### Prerequisites

- **Minecraft 1.20.1** with Forge 47.x
- **Java 17+** ([Download](https://adoptium.net/))
- **API Key** — Free options available:
  - [Groq](https://groq.com/) (Free, very fast, recommended)
  - [z.ai GLM](https://open.bigmodel.cn/) (Free tier)
  - OpenAI (Paid, GPT-4)
  - Gemini (Free tier)

### Installation

<div align="center">

**Option A: Download Release**

</div>

1. Download the latest [release](https://github.com/SuperInstance/MineWright/releases)
2. Place JAR in your mods folder:
   - **Windows**: `%appdata%\.minecraft\mods\`
   - **Mac**: `~/Library/Application Support/minecraft/mods/`
   - **Linux**: `~/.minecraft/mods/`
3. Launch Minecraft with Forge profile

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
# AI Provider Selection (groq, openai, gemini, zai)
[ai]
provider = "groq"

# Groq Configuration (Free & Fast)
[groq]
apiKey = "${GROQ_API_KEY}"  # Or paste directly
model = "llama3-70b-8192"

# Optional: OpenAI Configuration
[openai]
apiKey = "${OPENAI_API_KEY}"
model = "gpt-4"

# Behavior Settings
[behavior]
maxActiveCrew = 10
actionTickDelay = 20
enableChatResponses = true

# Performance Tuning
[performance]
aiTickBudgetMs = 5
enableSemanticCache = true
```

<div align="center">

**Get Your Free API Key:**

```bash
# Groq (Recommended)
# 1. Visit https://console.groq.com/
# 2. Sign up and get your API key
# 3. Set environment variable:
export GROQ_API_KEY="gsk_your_key_here"
```

</div>

### First Steps

1. **Launch Minecraft** with Forge
2. **Enter a world** (create or load existing)
3. **Spawn your first companion:**
   ```
   /minewright spawn Alex
   ```
4. **Open command GUI** (Press **K**)
5. **Give your first command:**
   ```
   "Mine 20 stone and build a small shelter"
   ```

---

## 📖 Usage Examples

### Basic Commands

| Command | Description |
|---------|-------------|
| `/minewright spawn <name>` | Spawn a new companion |
| `/minewright list` | List all active companions |
| `/minewright order <name> <command>` | Give a task |
| `/minewright remove <name>` | Remove a companion |
| **K** (key) | Open command GUI |

### Natural Language Tasks

```
# Mining & Gathering
"Mine 20 iron ore"
"Gather 10 oak logs"
"Strip mine at Y=-48"

# Building
"Build a 5x5 cobblestone house"
"Create a stone bridge across the river"
"Construct a medieval tower"

# Farming
"Plant and harvest wheat"
"Breed cows in the pen"
"Create a sugarcane farm"

# Combat
"Kill all zombies nearby"
"Protect me from mobs"
"Fight the wither boss"

# Multi-Agent
"Team up to build a castle"
"Divide and clear the forest"
"Work together to mine diamonds"
```

### Advanced Features

```bash
# Task Profiles (Predefined Task Templates)
/minewright order Alex profile:mining_iron
/minewright order Steve profile:farming_wheat

# Skill Learning
# Agents automatically learn from successful tasks
# and reuse patterns for similar future tasks

# Persistent Memory
# Companions remember:
# - Past conversations
# - Your preferences
# - World locations
# - Task outcomes
```

---

## 🏗️ Architecture

### Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Platform** | Minecraft Forge 1.20.1 | Mod framework |
| **Language** | Java 17 | Primary implementation |
| **AI Providers** | Groq, OpenAI, Gemini, GLM | LLM inference |
| **Concurrency** | ConcurrentHashMap, AtomicInteger | Lock-free coordination |
| **Caching** | Caffeine 3.1.8 | High-performance caching |
| **Resilience** | Resilience4j 2.3.0 | Retry, circuit breaker, rate limiting |
| **Scripting** | GraalVM JS 24.1.2 | Dynamic code execution |
| **Networking** | Java 11+ HttpClient | API communication |

### Key Systems

| System | Purpose | Innovation |
|--------|---------|------------|
| **Skill Library** | Learn and reuse task patterns | Adaptive AI |
| **Cascade Router** | Tier-based model selection | Cost optimization |
| **Semantic Cache** | Remember similar requests | 10x API reduction |
| **Blackboard** | Shared agent knowledge | Team intelligence |
| **Contract Net** | Multi-agent coordination | Conflict-free work |
| **Event Bus** | Async communication | Decoupled architecture |
| **Action Interceptors** | Pre/post processing | Extensible actions |

### Code Statistics

| Metric | Value |
|--------|-------|
| **Source Files** | 237 Java files |
| **Source Lines** | 115,937 LOC |
| **Test Files** | 155 test files |
| **Test Lines** | 99,357 LOC |
| **Test Coverage** | 40%+ |
| **Documentation** | 425+ files |
| **Packages** | 40 |

---

## 🧪 Development

### Build & Test

```bash
# Standard build
./gradlew build

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests ActionExecutorTest

# Generate coverage report
./gradlew test jacocoTestReport

# Run SpotBugs analysis
./gradlew spotbugsMain

# Run Checkstyle
./gradlew checkstyleMain

# Launch test client
./gradlew runClient

# Launch test server
./gradlew runServer
```

### Project Structure

```
minewright/
├── src/
│   ├── main/java/com/minewright/
│   │   ├── action/          # Task execution system
│   │   ├── llm/             # LLM integration & caching
│   │   ├── entity/          # Foreman entities
│   │   ├── pathfinding/     # A* pathfinding
│   │   ├── skill/           # Skill learning system
│   │   ├── script/          # Script parsing & execution
│   │   ├── memory/          # Persistent memory
│   │   ├── personality/     # AI personality system
│   │   └── ...
│   ├── test/java/com/minewright/
│   └── main/resources/
├── docs/
│   ├── architecture/        # Architecture docs
│   ├── agent-guides/        # Capability guides
│   ├── characters/          # Personality docs
│   └── audits/              # Code audits
├── config/
│   ├── profiles/            # Task profiles
│   └── templates/           # Config templates
├── build.gradle
└── README.md
```

### Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Submit a pull request

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [CLAUDE.md](CLAUDE.md) | Complete project guide |
| [Architecture Overview](docs/architecture/TECHNICAL_DEEP_DIVE.md) | Deep technical dive |
| [Agent Guides](docs/agent-guides/GUIDE_INDEX.md) | Capability documentation |
| [Personality System](docs/characters/MASTER_CHARACTER_GUIDE.md) | Character & dialogue |
| [Future Roadmap](docs/FUTURE_ROADMAP.md) | Development roadmap |

---

## 🐛 Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| **"API Key Not Configured"** | Check `config/minewright-common.toml` exists and contains valid API key |
| **Agent Not Moving** | Verify pathfinding in logs; spawn in open terrain |
| **Out of Memory** | Reduce `maxActiveCrew`; add `-Xmx4G` to JVM args |
| **Commands Hanging** | Switch to Groq (faster); check internet connection |
| **Build Fails** | Run `./gradlew clean`; ensure Java 17+ installed |

### Debug Mode

Enable debug logging in `config/minewright-common.toml`:

```toml
[logging]
level = "debug"
```

---

## 🙏 Credits

- **Built with** [Minecraft Forge](https://files.minecraftforge.net/)
- **LLM Providers**: Groq, OpenAI, Gemini, z.ai
- **Inspired by**: Baritone, Voyager, and the dream of AI companions
- **Special Thanks**: The Minecraft modding community

---

## 📄 License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=SuperInstance/MineWright&type=Date)](https://star-history.com/#SuperInstance/MineWright&Date)

---

<div align="center">

### **"Type what you want. They figure out how."**

[GitHub](https://github.com/SuperInstance/MineWright)
• [Issues](https://github.com/SuperInstance/MineWright/issues)
• [Releases](https://github.com/SuperInstance/MineWright/releases)
• [Discord](https://discord.gg/minewright)

**Made with ❤️ by the MineWright team**

</div>
