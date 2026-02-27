# MineWright Configuration Documentation

This document describes all configuration options available in MineWright.

## Table of Contents

1. [AI Configuration](#ai-configuration)
2. [Behavior Configuration](#behavior-configuration)
3. [Voice Configuration](#voice-configuration)
4. [Configuration File Locations](#configuration-file-locations)
5. [Environment Variables](#environment-variables)
6. [Validation Rules](#validation-rules)
7. [Migration Guide](#migration-guide)

---

## AI Configuration

### Section: `ai`

Controls which AI provider and model to use for task planning.

#### `provider`

- **Type**: String
- **Default**: `"groq"`
- **Valid Values**: `"groq"` | `"openai"` | `"gemini"`
- **Description**: The AI provider to use for LLM requests. Groq is fastest and free, OpenAI uses GPT models, Gemini uses Google's models.
- **Impact**: Affects response speed, cost, and quality
- **Restart Required**: No (changes on next request)

```toml
[ai]
provider = "groq"  # FASTEST, FREE option
```

---

### Section: `openai`

Controls OpenAI/Gemini API settings.

#### `apiKey`

- **Type**: String
- **Default**: `""`
- **Valid Values**: Non-empty API key string
- **Description**: Your OpenAI or Gemini API key. Required for non-Groq providers.
- **Impact**: Without valid key, API calls will fail
- **Restart Required**: No

```toml
[openai]
apiKey = "sk-your-api-key-here"
```

#### `model`

- **Type**: String
- **Default**: `"glm-5"`
- **Valid Values**: `"gpt-4"` | `"gpt-3.5-turbo"` | `"glm-5"` | `"gemini-pro"`
- **Description**: The LLM model to use. glm-5 is for z.ai, gpt-4 for OpenAI.
- **Impact**: Affects response quality and cost
- **Restart Required**: No

```toml
[openai]
model = "gpt-4"  # Use GPT-4 for best quality
```

#### `maxTokens`

- **Type**: Integer
- **Default**: `8000`
- **Valid Range**: `100` - `65536`
- **Description**: Maximum tokens per API request. Larger values allow longer responses but cost more.
- **Impact**: Affects how complex tasks can be handled
- **Restart Required**: No

```toml
[openai]
maxTokens = 16000  # Allow for more complex planning
```

#### `temperature`

- **Type**: Double
- **Default**: `0.7`
- **Valid Range**: `0.0` - `2.0`
- **Description**: Controls randomness in responses. Lower = more deterministic, Higher = more creative.
- **Impact**: Affects consistency vs creativity
- **Restart Required**: No

```toml
[openai]
temperature = 0.3  # More deterministic actions
```

---

## Behavior Configuration

### Section: `behavior`

Controls agent behavior and execution settings.

#### `actionTickDelay`

- **Type**: Integer
- **Default**: `20`
- **Valid Range**: `1` - `100`
- **Description**: Ticks between action checks. 20 ticks = 1 second. Lower values = more responsive but more server load.
- **Impact**: Affects how often agents check their actions
- **Restart Required**: No

```toml
[behavior]
actionTickDelay = 10  # Check twice per second
```

#### `enableChatResponses`

- **Type**: Boolean
- **Default**: `true`
- **Valid Values**: `true` | `false`
- **Description**: Allow crew members to respond in chat. When false, agents won't send chat messages.
- **Impact**: Controls chat spam from agents
- **Restart Required**: No

```toml
[behavior]
enableChatResponses = false  # Silence agents in chat
```

#### `maxActiveCrewMembers`

- **Type**: Integer
- **Default**: `10`
- **Valid Range**: `1` - `50`
- **Description**: Maximum number of crew members active simultaneously. Higher values may impact performance.
- **Impact**: Limits concurrent agents
- **Restart Required**: Yes

```toml
[behavior]
maxActiveCrewMembers = 20  # Allow more agents
```

---

## Voice Configuration

### Section: `voice`

Controls voice input/output features.

#### `enabled`

- **Type**: Boolean
- **Default**: `false`
- **Valid Values**: `true` | `false`
- **Description**: Enable voice input/output features.
- **Impact**: When disabled, voice features are completely off
- **Restart Required**: Yes

```toml
[voice]
enabled = true  # Enable voice features
```

#### `mode`

- **Type**: String
- **Default**: `"logging"`
- **Valid Values**: `"disabled"` | `"logging"` | `"real"`
- **Description**: Voice system mode. `disabled` = off, `logging` = logs what would be heard/said, `real` = actual TTS/STT.
- **Impact**: Controls voice behavior
- **Restart Required**: Yes

```toml
[voice]
mode = "real"  # Enable actual voice I/O
```

#### `sttLanguage`

- **Type**: String
- **Default**: `"en-US"`
- **Valid Values**: Language code (e.g., `"en-US"`, `"en-GB"`, `"es-ES"`)
- **Description**: Speech-to-text language.
- **Impact**: Affects speech recognition accuracy
- **Restart Required**: No

```toml
[voice]
sttLanguage = "en-GB"  # Use British English
```

#### `ttsVoice`

- **Type**: String
- **Default**: `"default"`
- **Valid Values**: System TTS voice name
- **Description**: Text-to-speech voice name.
- **Impact**: Affects agent voice output
- **Restart Required**: No

```toml
[voice]
ttsVoice = "Microsoft Zira Desktop"  # Use specific voice
```

#### `ttsVolume`

- **Type**: Double
- **Default**: `0.8`
- **Valid Range**: `0.0` - `1.0`
- **Description**: TTS volume level.
- **Impact**: Affects how loud agent speech is
- **Restart Required**: No

```toml
[voice]
ttsVolume = 0.5  # Lower volume
```

#### `ttsRate`

- **Type**: Double
- **Default**: `1.0`
- **Valid Range**: `0.5` - `2.0`
- **Description**: TTS speech rate. 1.0 = normal speed.
- **Impact**: Affects how fast agent speaks
- **Restart Required**: No

```toml
[voice]
ttsRate = 1.5  # Faster speech
```

#### `ttsPitch`

- **Type**: Double
- **Default**: `1.0`
- **Valid Range**: `0.5` - `2.0`
- **Description**: TTS pitch adjustment. 1.0 = normal pitch.
- **Impact**: Affects agent voice pitch
- **Restart Required**: No

```toml
[voice]
ttsPitch = 0.8  # Lower pitch
```

#### `sttSensitivity`

- **Type**: Double
- **Default**: `0.5`
- **Valid Range**: `0.0` - `1.0`
- **Description**: STT sensitivity for speech detection. Higher = more sensitive to quiet sounds.
- **Impact**: Affects when speech detection triggers
- **Restart Required**: No

```toml
[voice]
sttSensitivity = 0.7  # More sensitive detection
```

#### `pushToTalk`

- **Type**: Boolean
- **Default**: `true`
- **Valid Values**: `true` | `false`
- **Description**: Require push-to-talk key for voice input vs continuous listening.
- **Impact**: Controls how voice input is triggered
- **Restart Required**: No

```toml
[voice]
pushToTalk = false  # Use continuous listening
```

#### `listeningTimeout`

- **Type**: Integer
- **Default**: `10`
- **Valid Range**: `0` - `60`
- **Description**: Auto-stop listening after N seconds of silence. 0 = no timeout.
- **Impact**: Affects when listening stops automatically
- **Restart Required**: No

```toml
[voice]
listeningTimeout = 15  # Wait 15 seconds before timeout
```

#### `debugLogging`

- **Type**: Boolean
- **Default**: `true`
- **Valid Values**: `true` | `false`
- **Description**: Enable verbose logging for voice system operations.
- **Impact**: Controls voice log spam
- **Restart Required**: No

```toml
[voice]
debugLogging = false  # Reduce log verbosity
```

---

## Configuration File Locations

Configuration files are stored in the Minecraft config directory.

### File Locations

| Platform | Path |
|----------|------|
| Windows | `%APPDATA%\.minecraft\config\minewright-common.toml` |
| macOS | `~/Library/Application Support/minecraft/config/minewright-common.toml` |
| Linux | `~/.minecraft/config/minewright-common.toml` |

### File Types

- **minewright-common.toml** - Common config (shared client/server)
- **minewright-client.toml** - Client-only config
- **minewright-server.toml** - Server-only config

### Example Full Config

```toml
# AI API Configuration
[ai]
provider = "groq"

# OpenAI/Gemini API Configuration
[openai]
apiKey = "sk-your-key-here"
model = "glm-5"
maxTokens = 8000
temperature = 0.7

# MineWright Behavior Configuration
[behavior]
actionTickDelay = 20
enableChatResponses = true
maxActiveCrewMembers = 10

# Voice Integration Configuration
[voice]
enabled = false
mode = "logging"
sttLanguage = "en-US"
ttsVoice = "default"
ttsVolume = 0.8
ttsRate = 1.0
ttsPitch = 1.0
sttSensitivity = 0.5
pushToTalk = true
listeningTimeout = 10
debugLogging = true
```

---

## Environment Variables

Environment variables can override config file values.

### Format

```
MINWRIGHT_<SECTION>_<OPTION>
```

### Examples

```bash
# Override AI provider
export MINWRIGHT_AI_PROVIDER=groq

# Override API key
export MINWRIGHT_OPENAI_APIKEY=sk-your-key-here

# Override model
export MINWRIGHT_OPENAI_MODEL=gpt-4

# Override max crew members
export MINWRIGHT_BEHAVIOR_MAXACTIVECREWMEMBERS=20
```

### Priority

1. Environment variables (highest)
2. Config file values
3. Default values (lowest)

---

## Validation Rules

### API Key Validation

- Must be non-empty when provider is not "groq"
- Must match format for selected provider
- Checked before making API calls

### Range Validation

| Option | Min | Max | Validation |
|--------|-----|-----|------------|
| `maxTokens` | 100 | 65536 | Integer |
| `temperature` | 0.0 | 2.0 | Double |
| `actionTickDelay` | 1 | 100 | Integer |
| `maxActiveCrewMembers` | 1 | 50 | Integer |
| `ttsVolume` | 0.0 | 1.0 | Double |
| `ttsRate` | 0.5 | 2.0 | Double |
| `ttsPitch` | 0.5 | 2.0 | Double |
| `sttSensitivity` | 0.0 | 1.0 | Double |
| `listeningTimeout` | 0 | 60 | Integer |

### Provider-Specific Validation

| Provider | Required Options |
|----------|------------------|
| `groq` | None (free access) |
| `openai` | `apiKey`, `model` |
| `gemini` | `apiKey`, `model` |

---

## Migration Guide

### Version 1.2.x to 1.3.0

#### New Options

```toml
[voice]
enabled = false      # NEW
mode = "logging"     # NEW
pushToTalk = true    # NEW
```

#### Changed Options

```toml
# OLD (1.2.x)
[actionDelay]
ticks = 20

# NEW (1.3.0)
[behavior]
actionTickDelay = 20
```

#### Removed Options

```toml
# REMOVED: Use actionTickDelay instead
# [actions]
# speed = "normal"
```

### Version 1.1.x to 1.2.0

#### New Options

```toml
[behavior]
maxActiveCrewMembers = 10  # NEW

[openai]
temperature = 0.7  # NEW
```

---

## Configuration Best Practices

### Development Environment

```toml
[ai]
provider = "groq"  # Free for testing

[behavior]
actionTickDelay = 5  # Faster for testing
enableChatResponses = true

[voice]
enabled = false  # Disable voice in dev
```

### Production Environment

```toml
[ai]
provider = "openai"  # More reliable

[openai]
apiKey = "${MINWRIGHT_OPENAI_APIKEY}"  # Use env var
temperature = 0.3  # More deterministic

[behavior]
actionTickDelay = 20  # Standard speed
enableChatResponses = false  # Reduce chat spam

[voice]
enabled = true
mode = "real"  # Full voice support
```

### High Performance

```toml
[behavior]
actionTickDelay = 40  # Less frequent checks
maxActiveCrewMembers = 5  # Fewer agents
enableChatResponses = false  # Less overhead

[openai]
maxTokens = 4000  # Smaller responses
temperature = 0.0  # Most deterministic
```

### Creative Mode

```toml
[ai]
provider = "openai"  # Best quality

[openai]
temperature = 1.2  # More creative
maxTokens = 16000  # Allow complex plans
```

---

## Troubleshooting

### API Calls Failing

**Problem**: "API key not found or invalid"

**Solution**:
```toml
[ai]
provider = "openai"  # Make sure provider matches API key

[openai]
apiKey = "sk-..."  # Verify key is correct
```

### Agents Too Slow

**Problem**: Agents take too long to respond

**Solution**:
```toml
[behavior]
actionTickDelay = 10  # Reduce delay

[ai]
provider = "groq"  # Use fastest provider
```

### Too Much Chat Spam

**Problem**: Agents flooding chat

**Solution**:
```toml
[behavior]
enableChatResponses = false
```

### Voice Not Working

**Problem**: Voice features not responding

**Solution**:
```toml
[voice]
enabled = true
mode = "real"  # Make sure not "logging" or "disabled"
debugLogging = true  # Enable logs to debug
```

### Performance Issues

**Problem**: Server lag with many agents

**Solution**:
```toml
[behavior]
actionTickDelay = 40  # Increase delay
maxActiveCrewMembers = 5  # Reduce agents

[voice]
enabled = false  # Disable voice features
```
