# MineWright Configuration Guide

**Version:** 2.5.0
**Last Updated:** 2026-03-02
**Config File:** `config/minewright-common.toml`

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Config File Location](#config-file-location)
3. [Environment Variables](#environment-variables)
4. [Configuration Sections](#configuration-sections)
   - [AI Configuration](#ai-configuration)
   - [LLM Provider Configuration](#llm-provider-configuration)
   - [Behavior Configuration](#behavior-configuration)
   - [Voice Configuration](#voice-configuration)
   - [Hive Mind Configuration](#hive-mind-configuration)
   - [Skill Library Configuration](#skill-library-configuration)
   - [Cascade Router Configuration](#cascade-router-configuration)
   - [Utility AI Configuration](#utility-ai-configuration)
   - [Multi-Agent Configuration](#multi-agent-configuration)
   - [Pathfinding Configuration](#pathfinding-configuration)
   - [Performance Configuration](#performance-configuration)
   - [Semantic Cache Configuration](#semantic-cache-configuration)
   - [Humanization Configuration](#humanization-configuration)
5. [Recommended Settings](#recommended-settings)
6. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Minimum Required Configuration

```toml
[ai]
provider = "openai"

[openai]
apiKey = "${OPENAI_API_KEY}"
model = "glm-5"
```

### Setting Environment Variables

**Linux/Mac:**
```bash
export OPENAI_API_KEY="your-api-key-here"
```

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY="your-api-key-here"
```

**Windows (Command Prompt):**
```cmd
set OPENAI_API_KEY=your-api-key-here
```

### Config Reload

Edit the config file and use `/reload` in-game to apply changes without restarting.

---

## Config File Location

The config file is automatically generated on first run at:

**Platform-specific paths:**
- **Windows:** `.minecraft/config/minewright-common.toml`
- **Linux/Mac:** `~/.minecraft/config/minewright-common.toml`

---

## Environment Variables

Environment variables provide a secure way to configure API keys without committing them to version control.

### Syntax

```toml
[openai]
apiKey = "${ENV_VAR_NAME}"
```

### Available Environment Variables

| Config Key | Environment Variable | Description |
|------------|---------------------|-------------|
| `openai.apiKey` | `OPENAI_API_KEY` | OpenAI/z.ai API key |
| `groq.apiKey` | `GROQ_API_KEY` | Groq API key |
| `gemini.apiKey` | `GEMINI_API_KEY` | Google Gemini API key |

### Examples

**Using environment variable:**
```toml
[openai]
apiKey = "${OPENAI_API_KEY}"
```

**Direct value (not recommended for commits):**
```toml
[openai]
apiKey = "sk-abc123..."
```

### Verification

The mod will log API key preview on startup:
```
[INFO] API key configured: sk-a...b123
```

---

## Configuration Sections

---

## AI Configuration

### Section: `[ai]`

Controls the AI provider selection for LLM requests.

#### Options

| Option | Type | Default | Range/Values | Description |
|--------|------|---------|--------------|-------------|
| `provider` | string | `"openai"` | `groq`, `openai`, `gemini` | AI provider to use for LLM requests |

#### Descriptions

**`provider`**
- **`groq`**: FASTEST, FREE tier available, uses Llama models
- **`openai`**: Uses z.ai API with GLM-5 model (RECOMMENDED)
- **`gemini`**: Google's Gemini models

#### Example

```toml
[ai]
provider = "openai"
```

---

## LLM Provider Configuration

### Section: `[openai]`

Controls the LLM API settings. This section is used for all providers (OpenAI, Groq, Gemini).

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `apiKey` | string | `""` | - | Your API key (required) |
| `model` | string | `"glm-5"` | - | LLM model to use |
| `maxTokens` | int | `8000` | 100-65536 | Maximum tokens per API request |
| `temperature` | double | `0.7` | 0.0-2.0 | Temperature for AI responses |

#### Model Options

**For z.ai/OpenAI:**
- `glm-5` - Recommended, best performance
- `glm-4-flash` - Faster, good for simple tasks
- `gpt-4` - OpenAI's GPT-4

**For Groq:**
- `llama3-70b-8192` - High quality
- `mixtral-8x7b-32768` - Faster, good context

**For Gemini:**
- `gemini-pro` - Standard model
- `gemini-ultra` - Advanced model

#### Temperature Guide

| Value | Behavior | Use Case |
|-------|----------|----------|
| 0.0 - 0.3 | Deterministic, focused | Technical tasks, mining, building |
| 0.4 - 0.7 | Balanced (recommended) | General tasks, conversation |
| 0.8 - 1.2 | Creative | Storytelling, creative building |
| 1.3 - 2.0 | Very creative, unfocused | Experimental, may be inconsistent |

#### Example

```toml
[openai]
apiKey = "${OPENAI_API_KEY}"
model = "glm-5"
maxTokens = 8000
temperature = 0.7
```

---

## Behavior Configuration

### Section: `[behavior]`

Controls basic crew behavior settings.

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `actionTickDelay` | int | `20` | 1-100 | Ticks between action checks (20 = 1 second) |
| `enableChatResponses` | boolean | `true` | - | Allow crew members to respond in chat |
| `maxActiveCrewMembers` | int | `10` | 1-50 | Maximum active crew members |

#### Performance Impact

- **`actionTickDelay`**: Lower values = faster response but more CPU usage
- **`maxActiveCrewMembers`**: Higher values may impact performance

#### Example

```toml
[behavior]
actionTickDelay = 20
enableChatResponses = true
maxActiveCrewMembers = 10
```

---

## Voice Configuration

### Section: `[voice]`

Controls voice input/output features.

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `enabled` | boolean | `false` | - | Enable voice input/output features |
| `mode` | string | `"logging"` | `disabled`, `logging`, `real` | Voice system mode |
| `sttLanguage` | string | `"en-US"` | - | Speech-to-text language code |
| `ttsVoice` | string | `"default"` | - | Text-to-speech voice name |
| `ttsVolume` | double | `0.8` | 0.0-1.0 | TTS volume level |
| `ttsRate` | double | `1.0` | 0.5-2.0 | TTS speech rate (1.0 = normal) |
| `ttsPitch` | double | `1.0` | 0.5-2.0 | TTS pitch adjustment (1.0 = normal) |
| `sttSensitivity` | double | `0.5` | 0.0-1.0 | STT sensitivity for speech detection |
| `pushToTalk` | boolean | `true` | - | Require push-to-talk key for voice input |
| `listeningTimeout` | int | `10` | 0-60 | Auto-stop listening after N seconds of silence |
| `debugLogging` | boolean | `true` | - | Enable verbose logging for voice system |

#### Mode Descriptions

- **`disabled`**: Voice features completely off
- **`logging`**: Logs what would be heard/said (for testing)
- **`real`**: Actual TTS/STT functionality

#### Language Codes

Common language codes:
- `en-US` - English (United States)
- `en-GB` - English (United Kingdom)
- `es-ES` - Spanish (Spain)
- `fr-FR` - French (France)
- `de-DE` - German (Germany)
- `ja-JP` - Japanese (Japan)

#### Example

```toml
[voice]
enabled = true
mode = "real"
sttLanguage = "en-US"
ttsVolume = 0.8
ttsRate = 1.0
pushToTalk = true
listeningTimeout = 10
```

---

## Hive Mind Configuration

### Section: `[hivemind]`

Controls distributed AI with Cloudflare Edge for tactical reflexes (sub-20ms combat/hazard responses).

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `enabled` | boolean | `false` | - | Enable Hive Mind |
| `workerUrl` | string | `"https://minecraft-agent-reflex.workers.dev"` | - | Cloudflare Worker URL |
| `connectTimeoutMs` | int | `2000` | 500-10000 | Connection timeout (ms) |
| `tacticalTimeoutMs` | int | `50` | 10-500 | Tactical decision timeout (ms) |
| `syncTimeoutMs` | int | `1000` | 100-5000 | State sync timeout (ms) |
| `tacticalCheckInterval` | int | `20` | 5-100 | Tactical check interval (ticks) |
| `syncInterval` | int | `100` | 20-200 | State sync interval (ticks) |
| `fallbackToLocal` | boolean | `true` | - | Fall back to local when edge unavailable |

#### Tick Conversions

- 20 ticks = 1 second
- 100 ticks = 5 seconds

#### Example

```toml
[hivemind]
enabled = false
workerUrl = "https://minecraft-agent-reflex.workers.dev"
connectTimeoutMs = 2000
tacticalTimeoutMs = 50
syncTimeoutMs = 1000
tacticalCheckInterval = 20
syncInterval = 100
fallbackToLocal = true
```

---

## Skill Library Configuration

### Section: `[skill_library]`

Controls learning and storage of successful action patterns (Voyager-style skill learning).

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `enabled` | boolean | `true` | - | Enable skill library |
| `max_skills` | int | `100` | 10-1000 | Maximum skills to store |
| `success_threshold` | double | `0.7` | 0.0-1.0 | Success threshold for learning |

#### Description

When enabled, agents learn from successful executions and reuse patterns. Skills with success rate above the threshold are stored.

#### Example

```toml
[skill_library]
enabled = true
max_skills = 100
success_threshold = 0.7
```

---

## Cascade Router Configuration

### Section: `[cascade_router]`

Controls intelligent LLM selection based on task complexity.

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `enabled` | boolean | `true` | - | Enable cascade router |
| `similarity_threshold` | double | `0.85` | 0.0-1.0 | Semantic similarity threshold |
| `use_local_llm` | boolean | `false` | - | Use local LLM for fallback |

#### Description

Routes tasks to appropriate LLM based on complexity. Tasks with similarity above the threshold use cached/local LLM, reducing API usage.

#### Example

```toml
[cascade_router]
enabled = true
similarity_threshold = 0.85
use_local_llm = false
```

---

## Utility AI Configuration

### Section: `[utility_ai]`

Controls decision-making weights for action selection.

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `enabled` | boolean | `true` | - | Enable utility AI |
| `urgency_weight` | double | `1.0` | 0.0-2.0 | Weight for urgency in calculations |
| `proximity_weight` | double | `0.8` | 0.0-2.0 | Weight for proximity in calculations |
| `safety_weight` | double | `1.2` | 0.0-2.0 | Weight for safety in calculations |

#### Weight Descriptions

- **`urgency_weight`**: Higher = prioritizes time-sensitive actions more
- **`proximity_weight`**: Higher = prioritizes nearby tasks more
- **`safety_weight`**: Higher = prioritizes safe actions over risky ones

#### Example

```toml
[utility_ai]
enabled = true
urgency_weight = 1.0
proximity_weight = 0.8
safety_weight = 1.2
```

---

## Multi-Agent Configuration

### Section: `[multi_agent]`

Controls coordination features for multiple agents.

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `enabled` | boolean | `true` | - | Enable multi-agent coordination |
| `max_bid_wait_ms` | int | `1000` | 100-5000 | Max time to wait for agent bids (ms) |
| `blackboard_ttl_seconds` | int | `300` | 60-3600 | Blackboard entry TTL (seconds) |

#### Description

When enabled, agents can collaborate and coordinate tasks using the Contract Net Protocol and shared blackboard.

#### Example

```toml
[multi_agent]
enabled = true
max_bid_wait_ms = 1000
blackboard_ttl_seconds = 300
```

---

## Pathfinding Configuration

### Section: `[pathfinding]`

Controls navigation algorithms.

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `enhanced` | boolean | `true` | - | Enable enhanced pathfinding |
| `max_search_nodes` | int | `10000` | 1000-50000 | Maximum nodes to search |
| `max_nodes` | int | `10000` | 1000-50000 | A* pathfinder max node limit |
| `cache_enabled` | boolean | `true` | - | Enable path caching |
| `cache_max_size` | int | `100` | 10-1000 | Maximum cached paths |
| `cache_ttl_minutes` | int | `10` | 1-60 | Path cache TTL (minutes) |

#### Performance Impact

- Higher `max_nodes` = can find longer paths but uses more CPU
- Path caching significantly improves performance for repeated routes

#### Example

```toml
[pathfinding]
enhanced = true
max_search_nodes = 10000
max_nodes = 10000
cache_enabled = true
cache_max_size = 100
cache_ttl_minutes = 10
```

---

## Performance Configuration

### Section: `[performance]`

Controls tick budget and enforcement settings.

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `aiTickBudgetMs` | int | `5` | 1-20 | AI tick budget in milliseconds |
| `budgetWarningThreshold` | int | `80` | 50-95 | Warning threshold (% of budget) |
| `strictBudgetEnforcement` | boolean | `true` | - | Enable strict budget enforcement |
| `pathfindingTimeoutMs` | int | `2000` | 100-10000 | Pathfinding timeout (ms) |

#### Description

AI operations must complete within the tick budget to prevent server lag. Minecraft ticks are 50ms total, AI should use significantly less.

#### Example

```toml
[performance]
aiTickBudgetMs = 5
budgetWarningThreshold = 80
strictBudgetEnforcement = true
pathfindingTimeoutMs = 2000
```

---

## Semantic Cache Configuration

### Section: `[semantic_cache]`

Controls intelligent LLM response caching.

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `enabled` | boolean | `true` | - | Enable semantic caching |
| `similarity_threshold` | double | `0.85` | 0.5-1.0 | Minimum similarity for cache hits |
| `max_size` | int | `500` | 100-2000 | Maximum cache entries |
| `ttl_minutes` | int | `5` | 1-60 | Cache entry TTL (minutes) |
| `embedding_method` | string | `"tfidf"` | `tfidf`, `ngram` | Embedding method |

#### Description

Caches similar prompts using text similarity matching. Significantly reduces API costs for repetitive command patterns.

#### Embedding Methods

- **`tfidf`**: Term frequency-inverse document frequency (recommended)
- **`ngram`**: N-gram based similarity (faster, less accurate)

#### Example

```toml
[semantic_cache]
enabled = true
similarity_threshold = 0.85
max_size = 500
ttl_minutes = 5
embedding_method = "tfidf"
```

---

## Humanization Configuration

### Section: `[humanization]`

Controls natural agent behavior features.

#### Options

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `enabled` | boolean | `true` | - | Enable humanization |
| `timing_variance` | double | `0.3` | 0.0-1.0 | Timing variance (fraction) |
| `min_action_delay_ticks` | int | `2` | 1-100 | Minimum action delay (ticks) |
| `max_action_delay_ticks` | int | `20` | 1-200 | Maximum action delay (ticks) |
| `speed_variance` | double | `0.1` | 0.0-0.5 | Movement speed variance |
| `micro_movement_chance` | double | `0.05` | 0.0-0.2 | Chance of micro-movement |
| `smooth_look` | boolean | `true` | - | Enable smooth look transitions |
| `mistake_rate` | double | `0.03` | 0.0-0.2 | Base mistake rate |
| `reaction_time_min_ms` | int | `150` | 50-1000 | Minimum reaction time (ms) |
| `reaction_time_max_ms` | int | `500` | 100-5000 | Maximum reaction time (ms) |
| `idle_action_chance` | double | `0.02` | 0.0-0.1 | Idle action chance |
| `personality_affects_idle` | boolean | `true` | - | Personality-driven idle behaviors |
| `session_modeling_enabled` | boolean | `true` | - | Session modeling (warm-up, fatigue) |
| `warmup_duration_minutes` | int | `10` | 1-60 | Warm-up duration (minutes) |
| `fatigue_start_minutes` | int | `60` | 15-180 | Fatigue onset time (minutes) |
| `break_interval_minutes` | int | `30` | 5-120 | Minimum break interval |
| `break_duration_minutes` | int | `2` | 1-10 | Break duration |

#### Description

When enabled, agents exhibit human-like timing, mistakes, and behaviors including:
- Reaction time delays
- Occasional mistakes (3% default)
- Session fatigue modeling
- Personality-driven idle behaviors

#### Example

```toml
[humanization]
enabled = true
timing_variance = 0.3
mistake_rate = 0.03
reaction_time_min_ms = 150
reaction_time_max_ms = 500
session_modeling_enabled = true
warmup_duration_minutes = 10
fatigue_start_minutes = 60
```

---

## Recommended Settings

### Development/Test Server

Fast iteration, minimal cost:

```toml
[ai]
provider = "openai"

[openai]
apiKey = "${OPENAI_API_KEY}"
model = "glm-4-flash"  # Faster
maxTokens = 4000
temperature = 0.5

[behavior]
actionTickDelay = 10  # Faster response
maxActiveCrewMembers = 3

[skill_library]
enabled = false  # Disable for testing

[semantic_cache]
enabled = false  # Disable for testing
```

### Production Server (Small)

1-5 players, balanced performance:

```toml
[ai]
provider = "openai"

[openai]
apiKey = "${OPENAI_API_KEY}"
model = "glm-5"
maxTokens = 8000
temperature = 0.7

[behavior]
actionTickDelay = 20
maxActiveCrewMembers = 5

[skill_library]
enabled = true
max_skills = 100

[semantic_cache]
enabled = true
max_size = 500

[performance]
aiTickBudgetMs = 5
```

### Production Server (Large)

10+ players, optimized for performance:

```toml
[ai]
provider = "openai"

[openai]
apiKey = "${OPENAI_API_KEY}"
model = "glm-5"
maxTokens = 8000
temperature = 0.7

[behavior]
actionTickDelay = 20
maxActiveCrewMembers = 20

[skill_library]
enabled = true
max_skills = 200

[semantic_cache]
enabled = true
max_size = 1000

[cascade_router]
enabled = true
similarity_threshold = 0.85

[performance]
aiTickBudgetMs = 3  # Stricter budget
strictBudgetEnforcement = true

[pathfinding]
cache_enabled = true
cache_max_size = 200
```

### Voice-Enabled Setup

For immersive voice interaction:

```toml
[voice]
enabled = true
mode = "real"
sttLanguage = "en-US"
ttsVolume = 0.8
ttsRate = 1.0
pushToTalk = true
listeningTimeout = 10
```

### Human-Like Behavior

For immersive, realistic agents:

```toml
[humanization]
enabled = true
timing_variance = 0.3
mistake_rate = 0.03
reaction_time_min_ms = 150
reaction_time_max_ms = 500
session_modeling_enabled = true
warmup_duration_minutes = 10
fatigue_start_minutes = 60
personality_affects_idle = true
```

---

## Troubleshooting

### API Key Not Working

**Symptom:** Mod logs "API key is not configured!"

**Solution:**
1. Verify API key is set in config
2. Check environment variable is set correctly
3. Use `/reload` after changing config
4. Check logs for API key preview

### Agents Not Responding

**Symptom:** Agents spawn but don't respond to commands

**Solutions:**
1. Check `provider` is set to valid value
2. Verify API key has credits
3. Check internet connection
4. Review logs for API errors

### Performance Issues

**Symptom:** Server lag with multiple agents

**Solutions:**
1. Reduce `maxActiveCrewMembers`
2. Increase `actionTickDelay` to 30-40
3. Reduce `aiTickBudgetMs` to 3
4. Enable `strictBudgetEnforcement`
5. Reduce `max_search_nodes` in pathfinding

### Voice Not Working

**Symptom:** Voice features don't respond

**Solutions:**
1. Set `voice.mode` to `"logging"` to test
2. Check microphone permissions
3. Verify `voice.enabled = true`
4. Review logs with `voice.debugLogging = true`

### Pathfinding Timeout

**Symptom:** Agents get stuck finding paths

**Solutions:**
1. Increase `pathfindingTimeoutMs` to 3000-5000
2. Reduce `max_search_nodes` to prevent long searches
3. Enable path caching: `pathfinding.cache_enabled = true`

### Configuration Not Applying

**Symptom:** Changes to config don't take effect

**Solutions:**
1. Use `/reload` command in-game
2. Check for syntax errors in TOML file
3. Verify config file path is correct
4. Restart server if `/reload` doesn't work

---

## Validation

The mod automatically validates configuration on startup and logs warnings for:

- Missing or empty API keys
- Invalid AI provider
- Invalid voice mode
- Out-of-range values
- Malformed URLs (Hive Mind)

Example validation output:
```
[INFO] Validating MineWright configuration...
[INFO] AI provider: openai
[INFO] API key configured: sk-a...b123
[INFO] Max active crew members: 10
[INFO] Action tick delay: 20 ticks (1.0 seconds)
[INFO] MineWright configuration validated successfully.
```

---

## Further Reading

- **CLAUDE.md** - Project overview and architecture
- **TEST_COVERAGE.md** - Test configuration
- **docs/research/** - Research documentation on AI patterns

---

**Document Version:** 2.5.0
**Last Updated:** 2026-03-02
**Maintained By:** Steve AI Development Team
