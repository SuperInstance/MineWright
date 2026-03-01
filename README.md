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
- **Minecraft 1.20.1** with Forge 47.x
- **Java 17+** (Java 21 recommended)
- **API Key** - Groq (free!), z.ai/GLM, OpenAI, or Gemini

### Installation

#### Option 1: Download Pre-built JAR (Recommended for Players)

1. **Download the latest release** from [Releases](https://github.com/SuperInstance/MineWright/releases)
2. **Install Minecraft Forge 1.20.1** - Download from [files.minecraftforge.net](https://files.minecraftforge.net/)
3. **Place the JAR** in your Minecraft `mods` folder:
   - Windows: `%appdata%\.minecraft\mods\`
   - Mac: `~/Library/Application Support/minecraft/mods/`
   - Linux: `~/.minecraft/mods/`
4. **Launch Minecraft** with the Forge profile
5. **Configure your API key** (see [Configuration](#configuration) below)

#### Option 2: Build from Source (For Developers)

```bash
# Clone the repository
git clone https://github.com/SuperInstance/MineWright.git
cd MineWright

# Build the mod
./gradlew build

# The JAR will be in build/libs/minewright-1.0.0.jar
# Copy this to your mods folder
```

See [Building from Source](#building-from-source) for detailed build instructions.

### Your First Crew

Once in-game:

1. **Spawn your first Foreman:**
   ```
   /minewright spawn Mace
   ```

2. **Open the command panel** by pressing **K**

3. **Issue your first command:**
   ```
   build me a small house nearby
   ```

Mace will acknowledge the command and begin planning and execution!

---

## Configuration

### Quick Setup

After launching Minecraft with the mod, a configuration file will be created at:
```
config/minewright-common.toml
```

### Setting Your API Key

**SECURITY BEST PRACTICE:** Use environment variables instead of hardcoding API keys.

#### Option 1: Environment Variable (Recommended)

Edit `config/minewright-common.toml`:
```toml
[openai]
apiKey = "${MINEWRIGHT_API_KEY}"
```

Then set the environment variable before launching Minecraft:

**Linux/macOS:**
```bash
export MINEWRIGHT_API_KEY="your-api-key-here"
# Then launch Minecraft from the same terminal
```

**Windows PowerShell:**
```powershell
$env:MINEWRIGHT_API_KEY="your-api-key-here"
# Then launch Minecraft from the same PowerShell session
```

**Windows Command Prompt:**
```cmd
set MINEWRIGHT_API_KEY=your-api-key-here
# Then launch Minecraft from the same command prompt
```

**Minecraft Launcher (Java Arguments):**
Add to JVM Arguments:
```
-DMINEWRIGHT_API_KEY=your-api-key-here
```

#### Option 2: Direct Entry (Less Secure)

Edit `config/minewright-common.toml`:
```toml
[openai]
apiKey = "your-api-key-here"
```

**WARNING:** Never commit your config file to version control with a real API key!

### Choosing an AI Provider

```toml
[ai]
provider = "openai"  # Options: openai (z.ai GLM), groq, gemini
```

| Provider | Models | Cost | Speed | Best For |
|----------|--------|------|-------|----------|
| **z.ai GLM** | glm-5, glm-4-flash | Free tier available | Fast | Best overall, recommended |
| **Groq** | llama3-70b-8192 | Free | Very Fast | Quick testing |
| **OpenAI** | gpt-4, gpt-3.5-turbo | Paid | Moderate | Complex tasks |
| **Gemini** | gemini-pro | Free tier | Moderate | Alternative option |

### Basic Configuration Example

```toml
[ai]
provider = "openai"  # z.ai GLM recommended

[openai]
apiKey = "${MINEWRIGHT_API_KEY}"
model = "glm-5"
maxTokens = 8000
temperature = 0.7

[behavior]
actionTickDelay = 20
enableChatResponses = true
maxActiveCrewMembers = 10
```

### Full Configuration Reference

See [docs/CONFIGURATION.md](docs/CONFIGURATION.md) for complete configuration options including:
- Performance tuning
- Voice system setup
- Multi-agent coordination
- Pathfinding options
- Debug logging

For performance optimization tips, see [docs/PERFORMANCE_GUIDE.md](docs/PERFORMANCE_GUIDE.md).

### Testing Your Configuration

1. **Launch Minecraft** with the mod loaded
2. **Check the logs** (`logs/latest.log`) for:
   - `MineWright: Loaded successfully`
   - `API key configured: sk-***...` (shows preview, not full key)
3. **Spawn a test agent:**
   ```
   /minewright spawn TestAgent
   ```
4. **Issue a simple command:**
   ```
   /minewright order TestAgent "Say hello"
   ```

If the agent responds, your configuration is working!

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

### User Documentation

| Resource | Description |
|----------|-------------|
| [Installation Guide](docs/INSTALLATION.md) | Detailed installation instructions |
| [Configuration](docs/CONFIGURATION.md) | Complete configuration reference |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | Common issues and solutions |
| [Performance Guide](docs/PERFORMANCE_GUIDE.md) | Optimization tips and tuning |

### Developer Documentation

| Resource | Description |
|----------|-------------|
| [Onboarding](docs/ONBOARDING.md) | Quick start for new developers |
| [Architecture](docs/ARCHITECTURE_OVERVIEW.md) | System design and diagrams |
| [Development Guide](docs/DEVELOPMENT_GUIDE.md) | Build, test, and debug |
| [API Documentation](docs/INDEX.md) | Complete API reference |
| [Research Guide](docs/RESEARCH_GUIDE.md) | Dissertation and research docs |
| [Roadmap](docs/FUTURE_ROADMAP.md) | Prioritized future work |
| [Contributing](CONTRIBUTING.md) | How to contribute |

---

## Troubleshooting

### Common Issues

#### "API Key Not Configured" Error

**Symptom:** Agent doesn't respond, logs show missing API key

**Solutions:**
1. Check that `config/minewright-common.toml` exists
2. Verify your API key is set correctly
3. If using environment variables, ensure they're set before launching Minecraft
4. Check logs for the full error message

#### Agent Stuck or Not Moving

**Symptom:** Agent spawns but doesn't move or execute tasks

**Solutions:**
1. Check if the agent is in a valid location (not inside blocks)
2. Ensure pathfinding can find a route (try open terrain)
3. Increase tick delay in config if server is lagging
4. Check logs for pathfinding errors

#### Out of Memory Crash

**Symptom:** Minecraft crashes with OutOfMemoryError

**Solutions:**
1. Reduce `maxActiveCrewMembers` in config
2. Increase JVM heap size: `-Xmx4G` in Minecraft launcher
3. Close other applications to free memory

#### LLM API Timeout

**Symptom:** Commands hang without response

**Solutions:**
1. Switch to faster provider (Groq is very fast)
2. Check your internet connection
3. Verify API key is valid
4. Check if API quota is exhausted

#### Mod Won't Load

**Symptom:** Minecraft crashes on startup with MineWright error

**Solutions:**
1. Verify you're using Minecraft 1.20.1 with Forge 47.x
2. Check for mod conflicts (try with only MineWright)
3. Ensure Java 17+ is installed
4. Check the crash report for specific errors

### Getting Help

If you're still having trouble:

1. **Check the logs:** `logs/latest.log` in your Minecraft directory
2. **Search existing issues:** [GitHub Issues](https://github.com/SuperInstance/MineWright/issues)
3. **Create a new issue:** Include:
   - Minecraft version
   - Forge version
   - MineWright version
   - Relevant logs
   - Steps to reproduce

See [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) for more detailed troubleshooting.

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

### Prerequisites for Building

- **Java 17+** (Java 21 recommended)
- **Git** for cloning the repository
- **Gradle** (included via wrapper, no separate installation needed)

### Build Commands

```bash
# Clone the repository
git clone https://github.com/SuperInstance/MineWright.git
cd MineWright

# Build the mod (creates JAR in build/libs/)
./gradlew build

# Run client for testing
./gradlew runClient

# Run server for testing
./gradlew runServer

# Run tests
./gradlew test

# Build distribution JAR with dependencies
./gradlew shadowJar

# Clean build artifacts
./gradlew clean
```

### Build Output

**Development JAR:** `build/libs/minewright-1.0.0.jar`
**Distribution JAR:** `build/libs/minewright-1.0.0-all.jar` (use this for distribution)

### IDE Setup

**IntelliJ IDEA:**
1. Open the project directory
2. IntelliJ will automatically detect the Gradle project
3. Wait for dependency indexing to complete
4. Run/debug configurations are auto-generated from build.gradle

**VS Code:**
1. Install the "Java Extension Pack"
2. Open the project directory
3. Trust the Gradle project when prompted

**Eclipse:**
```bash
./gradlew eclipse
# Then import as "Existing Projects into Workspace"
```

---

## Performance Tips

### Recommended Settings

**For Low-End PCs (4GB RAM or less):**
```toml
[behavior]
maxActiveCrewMembers = 3
actionTickDelay = 40

[performance]
aiTickBudgetMs = 3
strictBudgetEnforcement = true
```

**For Mid-Range PCs (8GB RAM):**
```toml
[behavior]
maxActiveCrewMembers = 5
actionTickDelay = 20

[performance]
aiTickBudgetMs = 5
strictBudgetEnforcement = true
```

**For High-End PCs (16GB+ RAM):**
```toml
[behavior]
maxActiveCrewMembers = 10
actionTickDelay = 10

[performance]
aiTickBudgetMs = 10
strictBudgetEnforcement = false
```

### Optimization Tips

1. **Use Groq for fastest responses** - Reduces command lag
2. **Enable semantic caching** - Reduces API calls for similar commands
3. **Limit concurrent agents** - Each agent uses CPU for pathfinding
4. **Increase action tick delay** - Reduces CPU usage (at cost of responsiveness)
5. **Use Java 21** - Better GC performance than Java 17

### JVM Arguments

Add to Minecraft Launcher JVM Settings:
```
-Xmx4G -Xms2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled
```

See [docs/PERFORMANCE_GUIDE.md](docs/PERFORMANCE_GUIDE.md) for detailed performance tuning.

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

We welcome contributions! MineWright is a community project and we value all contributions.

### Quick Start for Contributors

1. **Fork the repository** on GitHub
2. **Clone your fork:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/MineWright.git
   cd MineWright
   ```
3. **Create a feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make your changes** following existing patterns
5. **Test your changes:**
   ```bash
   ./gradlew test
   ./gradlew runClient
   ```
6. **Commit your changes:**
   ```bash
   git commit -m "Add: Your feature description"
   ```
7. **Push to your fork:**
   ```bash
   git push origin feature/your-feature-name
   ```
8. **Create a Pull Request** on GitHub

### What We're Looking For

**High Priority:**
- Bug fixes
- Performance improvements
- Additional test coverage
- Documentation improvements
- New action implementations

**Medium Priority:**
- New AI provider integrations
- Voice system enhancements
- Multi-agent coordination features
- UI/UX improvements

**Low Priority:**
- New personality types
- Cosmetic changes
- Nice-to-have features

### Coding Standards

- **Java 17+** features are allowed
- **4-space indentation**
- **120 character line limit**
- **JavaDoc** for public APIs
- **Follow existing patterns** (see [ONBOARDING.md](docs/ONBOARDING.md))

### Testing

All contributions should include tests where applicable:
- Unit tests for new functionality
- Integration tests for complex features
- Manual testing for gameplay changes

### Pull Request Guidelines

- **Describe your changes** in the PR description
- **Reference related issues** (e.g., "Fixes #123")
- **Add tests** for new functionality
- **Update documentation** if needed
- **Ensure all tests pass** before submitting

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed contribution guidelines.

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
