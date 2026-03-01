# MineWright Documentation Index

Complete documentation for MineWright - "Cursor for Minecraft". This is your starting point for all documentation.

---

## Quick Links

### For Players

- [Installation Guide](INSTALLATION.md) - Get MineWright installed and running
- [Configuration Guide](CONFIGURATION.md) - Set up your API key and configure options
- [Performance Guide](PERFORMANCE_GUIDE.md) - Optimize for your hardware
- [Troubleshooting Guide](TROUBLESHOOTING.md) - Fix common issues
- [Main README](../README.md) - Project overview and features

### For Developers

- [Onboarding Guide](ONBOARDING.md) - Quick start for developers
- [Architecture Overview](ARCHITECTURE_OVERVIEW.md) - System design and patterns
- [Development Guide](DEVELOPMENT_GUIDE.md) - Build, test, and contribute
- [API Documentation](#api-documentation) - Complete API reference
- [Research Guide](RESEARCH_GUIDE.md) - Dissertations and research

---

## Getting Started

### What is MineWright?

MineWright is an AI-powered mod for Minecraft that adds autonomous AI companions (called "Foremen") who can:

- Understand natural language commands
- Plan and execute complex tasks
- Coordinate with multiple agents
- Build relationships with players
- Learn and improve over time

**Example Commands:**
```
build a stone house with a roof
mine 64 iron ore and smelt it
follow me and kill any zombies nearby
clear a 10x10 area and build a wheat farm
```

### Key Concepts

**Actions** - Units of work agents can perform (mine, build, craft, etc.)

**Events** - Notifications when things happen (action started, state changed, etc.)

**Plugins** - Extensions that add new actions or modify behavior

**Configuration** - Settings that control AI provider, behavior, and features

### Quick Reference

#### In-Game Commands

| Command | Description |
|---------|-------------|
| `/minewright spawn <name>` | Spawn a new AI agent |
| `/minewright list` | List all active agents |
| `/minewright remove <name>` | Remove an agent |
| `/minewright order <name> <cmd>` | Give a command |
| Press **K** | Open command GUI |

#### Key Configuration

```toml
[ai]
provider = "openai"  # AI provider: openai, groq, gemini

[openai]
apiKey = "${MINEWRIGHT_API_KEY}"  # Your API key
model = "glm-5"  # Model to use

[behavior]
maxActiveCrewMembers = 10  # Max concurrent agents
actionTickDelay = 20  # Ticks between checks
```

---

## User Documentation

### Installation & Setup

| Document | Description |
|----------|-------------|
| [Installation Guide](INSTALLATION.md) | Step-by-step installation for players and developers |
| [Configuration Guide](CONFIGURATION.md) | Complete configuration reference |
| [First-Time Setup](INSTALLATION.md#first-time-setup) | Setting your API key and choosing a provider |

### Optimization & Troubleshooting

| Document | Description |
|----------|-------------|
| [Performance Guide](PERFORMANCE_GUIDE.md) | Optimize for your hardware |
| [Performance Analysis](PERFORMANCE.md) | Technical performance analysis (developers) |
| [Troubleshooting Guide](TROUBLESHOOTING.md) | Common issues and solutions |

---

## Developer Documentation

### Getting Started

| Document | Description |
|----------|-------------|
| [Onboarding Guide](ONBOARDING.md) | Quick start for new developers |
| [Architecture Overview](ARCHITECTURE_OVERVIEW.md) | System design and diagrams |
| [Development Guide](DEVELOPMENT_GUIDE.md) | Build, test, and debug |
| [Project Structure](#project-structure) | Code organization |
| [Contributing](../CONTRIBUTING.md) | How to contribute |

### API Documentation

| Document | Description |
|----------|-------------|
| [Action API](ACTION_API.md) | All available actions and how to use them |
| [Event API](EVENT_API.md) | Event system and event handling |
| [API Template](API_DOCUMENTATION_TEMPLATE.md) | Format for API docs |
| [Integration Guide](INTEGRATION_GUIDE.md) | How to extend MineWright |

### Research & Architecture

| Document | Description |
|----------|-------------|
| [Research Guide](RESEARCH_GUIDE.md) | Dissertations and research docs |
| [Roadmap](FUTURE_ROADMAP.md) | Prioritized future work |
| [Script Layer Learning](SCRIPT_LAYER_LEARNING_SYSTEM.md) | Learning system architecture |

---

## Project Structure

### Package Overview

| Package | Purpose |
|---------|---------|
| `action` | Action execution and task management |
| `action.actions` | Built-in action implementations |
| `behavior` | Behavior tree runtime |
| `blackboard` | Shared knowledge system |
| `client` | GUI, key bindings, client-side features |
| `communication` | Inter-agent messaging |
| `config` | Configuration management |
| `coordination` | Multi-agent coordination |
| `decision` | Utility AI |
| `di` | Dependency injection |
| `entity` | Foreman entity definition |
| `evaluation` | Benchmarking |
| `event` | Event bus and events |
| `execution` | State machine, interceptors |
| `htn` | Hierarchical task network |
| `llm` | LLM clients, prompts |
| `memory` | Persistence, retrieval |
| `orchestration` | Multi-agent orchestration |
| `pathfinding` | Navigation |
| `personality` | AI personality system |
| `plugin` | Plugin system |
| `script` | Script layer |
| `skill` | Skill library |
| `structure` | Procedural generation |
| `util` | Utilities |
| `voice` | Voice integration |

### Architecture Patterns

| Pattern | Usage |
|---------|-------|
| **Plugin Pattern** | Extensible action system |
| **Observer Pattern** | Event bus for communication |
| **State Pattern** | Agent state machine |
| **Interceptor Pattern** | Action execution pipeline |
| **Factory Pattern** | Action creation |
| **Strategy Pattern** | Pluggable LLM providers |

---

## Common Tasks

### Create a Custom Action

1. Extend `BaseAction`
2. Implement `onStart()`, `onTick()`, `onCancel()`
3. Register in plugin's `onLoad()`
4. Update prompt builder

See [Integration Guide > Action Creation](INTEGRATION_GUIDE.md#action-creation)

### Subscribe to Events

```java
eventBus.subscribe(ActionCompletedEvent.class, event -> {
    if (event.isSuccess()) {
        // Handle success
    }
});
```

See [Event API > EventBus API](EVENT_API.md#eventbus-api)

### Configure AI Provider

```toml
[ai]
provider = "openai"

[openai]
apiKey = "${MINEWRIGHT_API_KEY}"
model = "glm-5"
```

See [Configuration > AI Configuration](CONFIGURATION.md#ai-configuration)

---

## Version Information

- **MineWright Version**: 1.0.0
- **Documentation Version**: 1.0
- **Last Updated**: 2026-03-01

---

## Support

### Getting Help

- [Troubleshooting Guide](TROUBLESHOOTING.md) - Common issues and fixes
- [GitHub Issues](https://github.com/SuperInstance/MineWright/issues) - Report bugs
- [GitHub Discussions](https://github.com/SuperInstance/MineWright/discussions) - Ask questions

### Contributing

See [Contributing Guide](../CONTRIBUTING.md) for:
- Code standards
- Pull request guidelines
- Development workflow

---

## License

This documentation is part of MineWright. See main project [LICENSE](../LICENSE) file.

---

**Generated for MineWright 1.0.0**
