# MineWright Configuration Template

**File Location:** `config/minewright-common.toml`
**Config Reload:** Edit the config file and use `/reload` to apply changes without restarting.

---

## Table of Contents

- [AI Configuration](#ai-configuration)
- [Behavior Configuration](#behavior-configuration)
- [Voice Integration Configuration](#voice-integration-configuration)
- [Hive Mind Configuration](#hive-mind-configuration)
- [Skill Library Configuration](#skill-library-configuration)
- [Cascade Router Configuration](#cascade-router-configuration)
- [Utility AI Configuration](#utility-ai-configuration)
- [Multi-Agent Configuration](#multi-agent-configuration)
- [Pathfinding Configuration](#pathfinding-configuration)

---

## AI Configuration

### Section: `[ai]`

| Key | Type | Default | Valid Values | Description |
|-----|------|---------|--------------|-------------|
| `provider` | string | `"groq"` | `groq`, `openai`, `gemini` | AI provider to use for LLM requests |

#### Provider Details

- **groq**: FASTEST, FREE tier available, uses Llama models
- **openai**: GPT-4 and GPT-3.5, requires API key
- **gemini**: Google's Gemini models, requires API key

---

## OpenAI/Gemini API Configuration

### Section: `[openai]`

| Key | Type | Default | Range | Description |
|-----|------|---------|-------|-------------|
| `apiKey` | string | `""` | - | Your API key for the chosen provider |
| `model` | string | `"glm-5"` | - | LLM model to use |
| `maxTokens` | int | `8000` | `100-65536` | Maximum tokens per API request |
| `temperature` | double | `0.7` | `0.0-2.0` | Temperature for AI responses |

#### API Key Sources

- **OpenAI**: Get from https://platform.openai.com/api-keys
- **Groq**: Get from https://console.groq.com/keys
- **Gemini**: Get from https://makersuite.google.com/app/apikey

#### Model Options

- **For Groq**: `llama3-70b-8192`, `mixtral-8x7b-32768`
- **For OpenAI**: `gpt-4`, `gpt-3.5-turbo`
- **For Gemini**: `gemini-pro`, `gemini-ultra`

#### Temperature Guide

- **0.0**: More deterministic, focused responses
- **0.7**: Balanced creativity and focus (recommended)
- **1.0+**: More creative, varied responses
- **2.0**: Maximum creativity, may be unfocused

---

## Behavior Configuration

### Section: `[behavior]`

| Key | Type | Default | Range | Description |
|-----|------|---------|-------|-------------|
| `actionTickDelay` | int | `20` | `1-100` | Ticks between action checks (20 ticks = 1 second) |
| `enableChatResponses` | boolean | `true` | - | Allow crew members to respond in chat |
| `maxActiveCrewMembers` | int | `10` | `1-50` | Maximum number of active crew members |

#### Behavior Details

- **Lower actionTickDelay** = faster response but more CPU usage
- **Recommended actionTickDelay**: 20 (1 second) for balanced performance
- **Higher maxActiveCrewMembers** may impact performance depending on your CPU

---

## Voice Integration Configuration

### Section: `[voice]`

| Key | Type | Default | Range | Description |
|-----|------|---------|-------|-------------|
| `enabled` | boolean | `false` | - | Enable voice input/output features |
| `mode` | string | `"logging"` | `disabled`, `logging`, `real` | Voice system mode |
| `sttLanguage` | string | `"en-US"` | - | Speech-to-text language code |
| `ttsVoice` | string | `"default"` | - | Text-to-speech voice name |
| `ttsVolume` | double | `0.8` | `0.0-1.0` | TTS volume level |
| `ttsRate` | double | `1.0` | `0.5-2.0` | TTS speech rate |
| `ttsPitch` | double | `1.0` | `0.5-2.0` | TTS pitch adjustment |
| `sttSensitivity` | double | `0.5` | `0.0-1.0` | STT sensitivity for speech detection |
| `pushToTalk` | boolean | `true` | - | Require push-to-talk key for voice input |
| `listeningTimeout` | int | `10` | `0-60` | Auto-stop listening after N seconds of silence |
| `debugLogging` | boolean | `true` | - | Enable verbose logging for voice system |

#### Voice Mode Details

- **disabled**: Voice features completely off
- **logging**: Logs what would be heard/said (for testing)
- **real**: Actual TTS/STT functionality

#### Language Examples

- `en-US`, `en-GB`, `es-ES`, `fr-FR`, `de-DE`, `ja-JP`

#### Volume/Rate/Pitch

- **ttsVolume**: 0.0 = silent, 1.0 = maximum
- **ttsRate**: 1.0 = normal speed, 2.0 = 2x speed
- **ttsPitch**: 1.0 = normal pitch
- **listeningTimeout**: 0 = no timeout (manual stop)

---

## Hive Mind Configuration

### Section: `[hivemind]`

| Key | Type | Default | Range | Description |
|-----|------|---------|-------|-------------|
| `enabled` | boolean | `false` | - | Enable Hive Mind distributed AI |
| `workerUrl` | string | `"https://minecraft-agent-reflex.workers.dev"` | - | Cloudflare Worker URL |
| `connectTimeoutMs` | int | `2000` | `500-10000` | Connection timeout in milliseconds |
| `tacticalTimeoutMs` | int | `50` | `10-500` | Tactical decision timeout in milliseconds |
| `syncTimeoutMs` | int | `1000` | `100-5000` | State sync timeout in milliseconds |
| `tacticalCheckInterval` | int | `20` | `5-100` | How often to check for tactical situations (ticks) |
| `syncInterval` | int | `100` | `20-200` | How often to sync state with edge (ticks) |
| `fallbackToLocal` | boolean | `true` | - | Fall back to local decision-making when edge unavailable |

#### Hive Mind Details

- When enabled, agents get sub-20ms combat/hazard responses from edge workers
- When disabled, all decisions are made locally
- **Target tactical timeout**: sub-20ms, Max: 100ms for combat reactions
- **20 ticks = 1 second**

---

## Skill Library Configuration

### Section: `[skill_library]`

| Key | Type | Default | Range | Description |
|-----|------|---------|-------|-------------|
| `enabled` | boolean | `true` | - | Enable skill library for learning patterns |
| `max_skills` | int | `100` | `10-1000` | Maximum number of skills to store |
| `success_threshold` | double | `0.7` | `0.0-1.0` | Success threshold for considering a skill learned |

#### Skill Library Details

- When enabled, agents learn from successful executions and reuse patterns
- When disabled, each action is planned from scratch
- **Higher max_skills** = more learned patterns but more memory usage
- **Recommended**: 100 for balanced performance
- **Higher success_threshold** = fewer but more reliable skills

---

## Cascade Router Configuration

### Section: `[cascade_router]`

| Key | Type | Default | Range | Description |
|-----|------|---------|-------|-------------|
| `enabled` | boolean | `true` | - | Enable cascade router for intelligent LLM selection |
| `similarity_threshold` | double | `0.85` | `0.0-1.0` | Semantic similarity threshold for routing |
| `use_local_llm` | boolean | `false` | - | Use local LLM for cascade router fallback |

#### Cascade Router Details

- When enabled, routes tasks to appropriate LLM based on complexity
- When disabled, always uses primary LLM
- Tasks with similarity above threshold use cached/local LLM
- **Higher similarity_threshold** = more local processing, less API usage

---

## Utility AI Configuration

### Section: `[utility_ai]`

| Key | Type | Default | Range | Description |
|-----|------|---------|-------|-------------|
| `enabled` | boolean | `true` | - | Enable utility AI for decision-making |
| `urgency_weight` | double | `1.0` | `0.0-2.0` | Weight for urgency in utility calculations |
| `proximity_weight` | double | `0.8` | `0.0-2.0` | Weight for proximity in utility calculations |
| `safety_weight` | double | `1.2` | `0.0-2.0` | Weight for safety in utility calculations |

#### Utility AI Details

- When enabled, uses weighted scoring for action selection
- When disabled, uses simple priority-based selection
- **Higher urgency_weight** = prioritizes time-sensitive actions more
- **Higher proximity_weight** = prioritizes nearby tasks more
- **Higher safety_weight** = prioritizes safe actions over risky ones
- **Recommended**: 1.0, 0.8, 1.2 for balanced behavior

---

## Multi-Agent Configuration

### Section: `[multi_agent]`

| Key | Type | Default | Range | Description |
|-----|------|---------|-------|-------------|
| `enabled` | boolean | `true` | - | Enable multi-agent coordination features |
| `max_bid_wait_ms` | int | `1000` | `100-5000` | Maximum time to wait for agent bids (ms) |
| `blackboard_ttl_seconds` | int | `300` | `60-3600` | Time-to-live for blackboard entries (seconds) |

#### Multi-Agent Details

- When enabled, agents can collaborate and coordinate tasks
- When disabled, each agent operates independently
- **Lower max_bid_wait_ms** = faster coordination but may miss capable agents
- **Recommended**: 1000 for balanced performance
- **Lower blackboard_ttl_seconds** = less stale data but more frequent updates
- **Recommended**: 300 (5 minutes)

---

## Pathfinding Configuration

### Section: `[pathfinding]`

| Key | Type | Default | Range | Description |
|-----|------|---------|-------|-------------|
| `enhanced` | boolean | `true` | - | Enable enhanced pathfinding algorithms |
| `max_search_nodes` | int | `10000` | `1000-50000` | Maximum nodes to search in pathfinding |

#### Pathfinding Details

- When enabled, uses advanced pathfinding with obstacle avoidance
- When disabled, uses basic pathfinding
- **Higher max_search_nodes** = can find longer paths but uses more CPU
- **Recommended**: 10000 for balanced performance

---

## Complete Config Template

```toml
# ========================================================================
# MineWright Configuration File
# ========================================================================
# Config Reload: Edit this file and use /reload in-game to apply changes.
# ========================================================================

[ai]
# AI provider: groq (FASTEST, FREE), openai, or gemini
provider = "groq"

[openai]
# Your API key (required for all providers)
apiKey = ""

# LLM model to use
model = "glm-5"

# Maximum tokens per API request (100 to 65536)
maxTokens = 8000

# Temperature for AI responses (0.0 to 2.0)
temperature = 0.7

[behavior]
# Ticks between action checks (20 ticks = 1 second)
actionTickDelay = 20

# Allow crew members to respond in chat
enableChatResponses = true

# Maximum number of crew members that can be active simultaneously
maxActiveCrewMembers = 10

[voice]
# Enable voice input/output features
enabled = false

# Voice system mode: disabled, logging, or real
mode = "logging"

# Speech-to-text language
sttLanguage = "en-US"

# Text-to-speech voice name
ttsVoice = "default"

# TTS settings
ttsVolume = 0.8
ttsRate = 1.0
ttsPitch = 1.0
sttSensitivity = 0.5

# Push-to-talk
pushToTalk = true
listeningTimeout = 10

# Debug logging
debugLogging = true

[hivemind]
# Enable Hive Mind - distributed AI for tactical reflexes
enabled = false

# Cloudflare Worker URL
workerUrl = "https://minecraft-agent-reflex.workers.dev"

# Timeout settings (milliseconds)
connectTimeoutMs = 2000
tacticalTimeoutMs = 50
syncTimeoutMs = 1000

# Check intervals (ticks, 20 = 1 second)
tacticalCheckInterval = 20
syncInterval = 100

# Fallback
fallbackToLocal = true

[skill_library]
# Enable skill library for learning patterns
enabled = true

# Maximum skills and success threshold
max_skills = 100
success_threshold = 0.7

[cascade_router]
# Enable cascade router for intelligent LLM selection
enabled = true

# Semantic similarity threshold (0.0 to 1.0)
similarity_threshold = 0.85

# Use local LLM for fallback
use_local_llm = false

[utility_ai]
# Enable utility AI for decision-making
enabled = true

# Utility weights (0.0 to 2.0)
urgency_weight = 1.0
proximity_weight = 0.8
safety_weight = 1.2

[multi_agent]
# Enable multi-agent coordination
enabled = true

# Coordination settings
max_bid_wait_ms = 1000
blackboard_ttl_seconds = 300

[pathfinding]
# Enable enhanced pathfinding
enhanced = true

# Maximum search nodes
max_search_nodes = 10000
```
