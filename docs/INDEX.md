# MineWright API Documentation

Complete API documentation for MineWright - "Cursor for Minecraft". This documentation covers actions, events, configuration, and integration.

## Quick Links

| Document | Description |
|----------|-------------|
| [Action API](ACTION_API.md) | All available actions and how to use them |
| [Event API](EVENT_API.md) | Event system and event handling |
| [Configuration](CONFIGURATION.md) | All configuration options |
| [Integration Guide](INTEGRATION_GUIDE.md) | How to extend MineWright |
| [Documentation Template](API_DOCUMENTATION_TEMPLATE.md) | Format for API docs |

## Getting Started

### What is MineWright?

MineWright is an AI-powered mod for Minecraft that allows autonomous agents (called "Foremen") to execute natural language commands. Users type commands like "Build a house" or "Mine 64 iron ore" and AI-controlled agents plan and execute the tasks.

### Key Concepts

**Actions** - Units of work that agents can perform (mine, build, craft, etc.)

**Events** - Notifications published when things happen (action started, state changed, etc.)

**Plugins** - Extensions that add new actions or modify behavior

**Configuration** - Settings that control AI provider, behavior, and features

### Basic Usage

```json
// Command format
{
  "action": "mine",
  "parameters": {
    "block": "iron_ore",
    "quantity": 32
  }
}
```

### Quick Reference

#### Available Actions

| Action | Description | Parameters |
|--------|-------------|------------|
| `pathfind` | Navigate to coordinates | x, y, z |
| `follow` | Follow a player | playerName, distance |
| `mine` | Mine blocks | block, quantity |
| `gather` | Gather resources | resource, quantity |
| `place` | Place a block | block, x, y, z |
| `build` | Build a structure | structure, blocks, dimensions |
| `craft` | Craft an item | item, quantity |
| `attack` | Attack an entity | target, distance |

#### Main Events

| Event | When Published |
|-------|----------------|
| `ActionStartedEvent` | Action begins execution |
| `ActionCompletedEvent` | Action finishes (success/fail) |
| `StateTransitionEvent` | Agent state changes |

#### Key Configuration

```toml
[ai]
provider = "groq"  # AI provider: groq, openai, gemini

[behavior]
actionTickDelay = 20  # Ticks between checks
enableChatResponses = true  # Agents can chat

[voice]
enabled = false  # Voice features
mode = "logging"  # disabled, logging, real
```

## Documentation Structure

```
docs/
├── INDEX.md                      # This file
├── API_DOCUMENTATION_TEMPLATE.md # Template for API docs
├── ACTION_API.md                 # All actions
├── EVENT_API.md                  # All events
├── CONFIGURATION.md              # All config options
└── INTEGRATION_GUIDE.md          # Extension guide
```

## Version Information

- **MineWright Version**: 1.3.0
- **Documentation Version**: 1.3.0
- **Last Updated**: 2026-02-27

## Project Structure

### Package Overview

| Package | Purpose |
|---------|---------|
| `action` | Action execution and task management |
| `action.actions` | Built-in action implementations |
| `event` | Event bus and event definitions |
| `plugin` | Plugin system and action registry |
| `execution` | State machine, interceptors, code execution |
| `entity` | Foreman entity definition and lifecycle |
| `llm` | LLM clients, prompt building, async support |
| `memory` | Conversation history and world knowledge |
| `structure` | Procedural generation and NBT templates |
| `client` | GUI, key bindings, client-side features |
| `config` | Configuration management |
| `di` | Dependency injection container |

### Architecture Patterns

| Pattern | Usage |
|---------|-------|
| **Plugin Pattern** | Extensible action system via SPI |
| **Observer Pattern** | Event bus for decoupled communication |
| **State Pattern** | Agent state machine |
| **Interceptor Pattern** | Action execution pipeline |
| **Factory Pattern** | Action creation via ActionFactory |
| **Registry Pattern** | Dynamic action lookup |
| **Strategy Pattern** | Pluggable LLM providers |

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
provider = "openai"  # or "groq" or "gemini"

[openai]
apiKey = "sk-..."
model = "gpt-4"
```

See [Configuration > AI Configuration](CONFIGURATION.md#ai-configuration)

## Support

### Documentation

- Use the navigation in each document to find specific topics
- Check the template for documentation format
- See examples in the Integration Guide

### Code Examples

- Integration Guide contains complete examples
- API docs include usage examples for each component
- Template shows expected documentation format

### Troubleshooting

Each document has a troubleshooting section:
- [Action API Troubleshooting](ACTION_API.md#troubleshooting)
- [Event API Debugging](EVENT_API.md#debugging-events)
- [Configuration Troubleshooting](CONFIGURATION.md#troubleshooting)
- [Integration Troubleshooting](INTEGRATION_GUIDE.md#troubleshooting)

## Contributing

When adding new features:

1. Document following the [template](API_DOCUMENTATION_TEMPLATE.md)
2. Add examples to relevant docs
3. Update this index if adding new documents
4. Include version information (since X.Y.Z)

## License

This documentation is part of MineWright. See main project LICENSE file.

---

**Generated for MineWright 1.3.0**
