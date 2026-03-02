# Steve AI Troubleshooting Guide

**Version:** 1.0
**Last Updated:** 2026-03-02
**Project:** Steve AI - "Cursor for Minecraft"

---

## Table of Contents

1. [Quick Diagnosis](#quick-diagnosis)
2. [LLM Connection Issues](#1-llm-connection-issues)
3. [Agent Stuck / Not Responding](#2-agent-stuck--not-responding)
4. [Memory / Performance Issues](#3-memory--performance-issues)
5. [Configuration Errors](#4-configuration-errors)
6. [Multi-Agent Coordination Failures](#5-multi-agent-coordination-failures)
7. [Voice System Problems](#6-voice-system-problems)
8. [Debug Logging](#debug-logging)
9. [Common Error Messages](#common-error-messages)
10. [Recovery Commands](#recovery-commands)

---

## Quick Diagnosis

### Symptom Checklist

Use this checklist to quickly identify your issue category:

- [ ] **LLM Issues**: Agent says "Planning..." forever, no task generation
- [ ] **Stuck Agent**: Agent stands still, not moving, walking in circles
- [ ] **Performance**: Lag, low FPS, game crashes with OutOfMemory
- [ ] **Configuration**: "API key not found", "Invalid provider" errors
- [ ] **Coordination**: Multiple agents fighting over same task, not working together
- [ ] **Voice**: Voice commands not recognized, no audio output

### Health Check Commands

```bash
# Check if mod is loaded
/foreman list

# Check agent state
Look at agent in game - press F3 to see debug info

# Check configuration
View: config/minewright-common.toml

# Check logs
View: logs/latest.log (search for "MineWright" or "minewright")
```

---

## 1. LLM Connection Issues

### Symptom: Agent stuck in "PLANNING" state forever

**Cause:** LLM API not responding, timeout, or circuit breaker open.

**Diagnosis:**

```
[Search logs for]:
"Circuit breaker state: CLOSED -> OPEN"
"Request failed after all retries"
"LLM call failed: timeout"
"API key not found"
```

**Solutions (in order):**

#### 1.1 Check API Key Configuration

**Symptom:** `API key not found` or `401 Unauthorized`

**Solution:**

1. Open `config/minewright-common.toml`
2. Verify API key is set:
   ```toml
   [openai]
   apiKey = "sk-..."  # Or ${OPENAI_API_KEY} for env var
   ```
3. If using environment variables:
   ```bash
   # Linux/Mac
   export OPENAI_API_KEY="sk-..."

   # Windows PowerShell
   $env:OPENAI_API_KEY="sk-..."

   # Windows CMD
   set OPENAI_API_KEY=sk-...
   ```
4. Restart Minecraft

**Verification:**
```log
# Should see in logs:
[ResilientLLMClient] Initializing resilient client for provider: openai
[ResilientLLMClient] Resilient client initialized for provider: openai
```

#### 1.2 Check Circuit Breaker State

**Symptom:** `Circuit breaker state: CLOSED -> OPEN`

**Cause:** Too many failed API calls (5+ failures in a row)

**Solution:**

**Option A - Wait for automatic recovery (60 seconds):**
- Circuit breaker will automatically transition to HALF_OPEN after 60 seconds
- Next successful call will transition to CLOSED

**Option B - Manual reset (requires game restart):**
- Circuit breaker resets on game restart
- Check logs for reset confirmation

**Option C - Fix underlying issue:**
- Check internet connection
- Verify API key is valid
- Check API quota/limits
- Try different provider (groq, gemini)

**Verification:**
```log
# Should see:
[openai] Circuit breaker state: OPEN -> HALF_OPEN
[openai] Circuit breaker state: HALF_OPEN -> CLOSED
```

#### 1.3 API Timeout Issues

**Symptom:** `LLM call failed: timeout` or `Read timed out`

**Cause:** Network latency, API overloaded, slow model

**Solutions:**

**A. Switch to faster provider:**
```toml
# config/minewright-common.toml
[ai]
provider = "groq"  # Groq is fastest (free tier)
# provider = "openai"  # Slower but more capable
# provider = "gemini"  # Medium speed
```

**B. Enable batching (reduces API calls):**
```toml
[llm]
enableBatching = true
batchSize = 5
batchTimeout = 1000  # milliseconds
```

**C. Increase timeout:**
```toml
[openai]
requestTimeout = 30000  # 30 seconds (default)
# Increase to 60000 for slow connections
```

**D. Use simpler model:**
```toml
[zai]
foremanModel = "glm-4.7-air"  # Faster than glm-5
workerSimpleModel = "glm-4.7-air"
```

#### 1.4 Rate Limiting

**Symptom:** `Rate limiter rejected request` or `429 Too Many Requests`

**Cause:** Exceeded API rate limits

**Solution:**

**A. Check rate limits:**
- OpenAI: ~3,000 requests/minute (varies by tier)
- Groq: ~30 requests/minute (free tier)
- Gemini: ~60 requests/minute

**B. Reduce agent count:**
```toml
[behavior]
maxActiveCrewMembers = 3  # Reduce from 10
```

**C. Increase action delay:**
```toml
[behavior]
actionTickDelay = 40  # 2 seconds instead of 1 second
```

**D. Enable caching:**
```toml
[llm]
enableCache = true
cacheSize = 1000
```

#### 1.5 Invalid Response Format

**Symptom:** `Failed to parse LLM response` or `JSON parsing error`

**Cause:** LLM returned malformed JSON or unexpected format

**Solution:**

**A. Check model compatibility:**
- Use models tested with system: `glm-5`, `gpt-4`, `llama3-70b-8192`
- Avoid experimental models

**B. Reduce temperature:**
```toml
[openai]
temperature = 0.3  # Lower = more deterministic
# Avoid temperature > 1.0 for task planning
```

**C. Enable fallback:**
```toml
[llm]
enableFallback = true
fallbackMode = "pattern"  # Use pattern-based responses
```

---

## 2. Agent Stuck / Not Responding

### Symptom: Agent stands still, not moving

**Cause:** Pathfinding failure, stuck detector triggered, action execution blocked

**Diagnosis:**

```
[Search logs for]:
"position stuck for X ticks"
"pathfinding stuck"
"State transition failed"
"No path found to target"
```

**Solutions (in order):**

#### 2.1 Pathfinding Stuck

**Symptom:** Agent doesn't move, pathfinding errors in logs

**Cause:** No valid path to target, obstacles, unloaded chunks

**Diagnosis:**
```log
[StuckDetector] pathfinding stuck at BlockPos{x=100, y=64, z=200}
[PathExecutor] No path found to target
```

**Solutions:**

**A. Check target accessibility:**
- Is target in loaded chunks? (Move closer)
- Is target obstructed? (Clear path)
- Is target too far? (> 1000 blocks may fail)

**B. Enable stuck recovery:**
```toml
[recovery]
enableStuckDetection = true
stuckTimeout = 60  # ticks (3 seconds)
recoveryStrategy = "repath"  # or "teleport" (debug only)
```

**C. Increase pathfinding range:**
```toml
[pathfinding]
maxPathDistance = 500  # blocks
pathTimeout = 10000  # milliseconds
```

**D. Verify movement validator:**
```log
# Check for:
[MovementValidator] Movement blocked at BlockPos{x=...}
# Common causes: water, lava, fire, cactus
```

#### 2.2 Position Stuck

**Symptom:** Agent moves but returns to same position

**Cause:** Walking in circles, navigation loop, conflicting goals

**Diagnosis:**
```log
[StuckDetector] SteveAgent position stuck for 60 ticks at BlockPos{x=100, y=64, z=200}
```

**Solutions:**

**A. Check for conflicting commands:**
- Are multiple agents trying to reach same spot?
- Is agent following player AND executing task?

**B. Enable humanization (adds randomness):**
```toml
[humanization]
enableJitter = true
movementJitter = 0.1  # 10% random variation
```

**C. Reduce path smoothing:**
```toml
[pathfinding]
enablePathSmoothing = false  # May cause loops in complex terrain
```

**D. Check action timeout:**
```toml
[actions]
defaultTimeout = 300  # seconds (5 minutes)
# Agent will give up and report failure
```

#### 2.3 Progress Stuck

**Symptom:** Agent is moving but task progress not increasing

**Cause:** Wrong target, inefficient path, action implementation bug

**Diagnosis:**
```log
[StuckDetector] SteveAgent progress stuck for 100 ticks at 50%
```

**Solutions:**

**A. Verify action implementation:**
```log
# Check for:
[MineAction] No valid blocks found in range
[BuildAction] Missing required materials
[GatherAction] Target resource not found
```

**B. Check action progress tracking:**
- Is `getCurrentActionProgress()` being called correctly?
- Is progress counter incrementing?

**C. Manual intervention:**
```
/foreman order <agent_name> stop
/foreman order <agent_name> idle
```

#### 2.4 State Machine Stuck

**Symptom:** Agent state doesn't transition (e.g., stuck in PLANNING)

**Cause:** Logic deadlock, event bus failure, state machine error

**Diagnosis:**
```log
[StuckDetector] SteveAgent state stuck in PLANNING for 200 ticks
[AgentStateMachine] Invalid state transition: PLANNING -> EXECUTING
```

**Solutions:**

**A. Check state machine logs:**
```log
# Valid transitions:
IDLE -> PLANNING (new command)
PLANNING -> EXECUTING (planning complete)
PLANNING -> FAILED (planning error)
EXECUTING -> COMPLETED (all tasks done)
EXECUTING -> FAILED (execution error)
```

**B. Force state reset (last resort):**
- Restart game
- Use `/foreman remove <agent_name>` then `/foreman spawn <agent_name>`

**C. Check event bus:**
```log
# Should see:
[EventBus] Published StateTransitionEvent: IDLE -> PLANNING
[EventBus] Published StateTransitionEvent: PLANNING -> EXECUTING
```

---

## 3. Memory / Performance Issues

### Symptom: Game lag, low FPS, OutOfMemory crash

**Cause:** Too many agents, large structure generation, memory leak

**Diagnosis:**

```
[Search logs for]:
"OutOfMemoryError"
"java.lang.OutOfMemoryError: Java heap space"
"Memory usage: 90%+"
"GC overhead limit exceeded"
```

**Solutions (in order):**

#### 3.1 Reduce Agent Count

**Symptom:** Lag proportional to agent count

**Solution:**

```toml
[behavior]
maxActiveCrewMembers = 3  # Reduce from 10
```

**Performance estimates:**
- 1 agent: ~5% CPU overhead
- 5 agents: ~15% CPU overhead
- 10 agents: ~30% CPU overhead
- 20+ agents: Significant lag

#### 3.2 Increase JVM Heap

**Symptom:** `OutOfMemoryError: Java heap space`

**Solution:**

**Edit Minecraft launch profile:**
- In Minecraft Launcher → Installations → Edit
- Add JVM argument:
  ```
  -Xmx4G  # Allocate 4GB (adjust based on your RAM)
  ```
- Recommended:
  - Minimum: 2GB (`-Xmx2G`)
  - Recommended: 4GB (`-Xmx4G`)
  - Large modpacks: 6GB (`-Xmx6G`)

#### 3.3 Optimize Structure Generation

**Symptom:** Lag when building large structures

**Cause:** Structure generation loads many chunks at once

**Solution:**

```toml
[structure]
maxStructureSize = 1000  # blocks per batch
chunkLoadingTimeout = 5000  # milliseconds
enableAsyncGeneration = true
```

**Best practices:**
- Build structures in sections (< 1000 blocks each)
- Use `/foreman order <agent> stop` if lag occurs
- Reduce view distance if needed

#### 3.4 Reduce Memory Usage

**Symptom:** Gradual memory increase over time

**Cause:** Memory leak, excessive caching

**Solution:**

```toml
[llm]
enableCache = true
cacheSize = 100  # Reduce from 1000
cacheTTL = 300  # seconds (5 minutes)

[memory]
maxConversationHistory = 50  # Reduce from 100
maxWorldKnowledge = 1000  # Reduce from 10000
```

**Enable memory consolidation:**
```toml
[memory]
enableConsolidation = true
consolidationInterval = 300  # seconds (5 minutes)
```

#### 3.5 Profile Tick Usage

**Symptom:** Random lag spikes

**Diagnosis:**

```log
# Enable tick profiling:
[TickProfiler] Average tick time: 50ms (target: 50ms for 20 TPS)
[TickProfiler] ActionExecutor: 30ms (60%)
[TickProfiler] Pathfinding: 15ms (30%)
[TickProfiler] StateMachine: 2ms (4%)
```

**Solution:**

Identify bottleneck and optimize:
- **ActionExecutor**: Reduce action complexity, add action timeout
- **Pathfinding**: Reduce path distance, disable path smoothing
- **StateMachine**: Check for state loops, add transition timeout

---

## 4. Configuration Errors

### Symptom: Configuration errors on startup

**Cause:** Invalid config values, missing required fields, malformed TOML

**Diagnosis:**

```
[Search logs for]:
"Configuration error"
"Invalid value for"
"Missing required configuration"
"Failed to load config"
```

**Solutions (in order):**

#### 4.1 Invalid Provider

**Symptom:** `Invalid provider: xxx` or `Unknown provider`

**Solution:**

Valid providers: `groq`, `openai`, `gemini`

```toml
[ai]
provider = "groq"  # Must be lowercase
# NOT: "Groq", "GROQ", " openai ", etc.
```

**Verification:**
```log
# Should see:
[ConfigManager] Loaded provider: groq
```

#### 4.2 Missing API Key

**Symptom:** `API key not found` or `API key is required`

**Solution:**

```toml
[openai]
apiKey = "sk-..."  # Must be valid API key
# OR use environment variable:
apiKey = "${OPENAI_API_KEY}"
```

**Common mistakes:**
- Empty string: `apiKey = ""` ❌
- Missing quotes: `apiKey = sk-...` ❌
- Wrong section: `[groq]` when provider is `openai` ❌

**Verification:**
```log
# Should see:
[MineWrightConfig] API key configured: sk-...9abc (preview)
# NOT:
[MineWrightConfig] API key not configured
```

#### 4.3 Invalid Config Values

**Symptom:** `Invalid value for temperature` or `out of range`

**Common config ranges:**

```toml
# Valid ranges:
[openai]
maxTokens = 8000  # 100 to 65536
temperature = 0.7  # 0.0 to 2.0

[behavior]
actionTickDelay = 20  # 1 to 100 ticks
maxActiveCrewMembers = 10  # 1 to 50

[voice]
sttLanguage = "en-US"  # Valid BCP 47 language code
```

**Solution:** Fix values to be within valid ranges

#### 4.4 Malformed TOML

**Symptom:** `Failed to parse config file` or TOML syntax error

**Common TOML mistakes:**

```toml
# WRONG:
[openai
apiKey = "sk-..."  # Missing closing bracket
apiKey = sk-...     # Missing quotes
apiKey = 'sk-...'  # Single quotes (use double)
temperature = 0.7  # Missing newline after

# CORRECT:
[openai]
apiKey = "sk-..."
temperature = 0.7
```

**Solution:** Validate TOML syntax
- Use online TOML validator
- Check for matching brackets
- Use double quotes for strings
- Ensure newlines between sections

#### 4.5 Config Not Reloading

**Symptom:** Changed config but no effect

**Cause:** Config not reloaded after edit

**Solution:**

```bash
# In-game command:
/reload

# Or restart Minecraft
```

**Verification:**
```log
# Should see:
[ConfigManager] Configuration reloaded
[ConfigManager] Provider: groq -> openai
```

---

## 5. Multi-Agent Coordination Failures

### Symptom: Multiple agents fighting, not cooperating, idle when work exists

**Cause:** Contract Net Protocol failure, bid evaluation errors, communication bus issues

**Diagnosis:**

```
[Search logs for]:
"No bids received for task"
"Contract award failed"
"Communication bus error"
"Task announcement timeout"
```

**Solutions (in order):**

#### 5.1 No Bids Received

**Symptom:** Manager announces task but no agents bid

**Cause:** Agents not listening, capability mismatch, bid calculation error

**Diagnosis:**
```log
[ContractNetManager] No bids received for task announcement abc123
[ContractNetManager] Task award failed: no eligible bidders
```

**Solutions:**

**A. Check agent capabilities:**
```log
# Should see:
[AgentCapability] Agent SteveAgent[123] registered capabilities: {mining: 0.9, building: 0.7}
```

**B. Verify task requirements:**
- Is task within agent capability range?
- Is agent too far from task location?
- Is agent already at maximum load?

**C. Check bid timeout:**
```toml
[coordination]
bidTimeout = 5000  # milliseconds (increase if network slow)
maxBidsPerTask = 10  # maximum bids to accept
```

#### 5.2 Bid Evaluation Errors

**Symptom:** Bids received but award fails

**Cause:** Invalid bid values, scoring error, award selector bug

**Diagnosis:**
```log
[AwardSelector] Failed to evaluate bid: invalid score value
[ContractNetManager] Task award failed: scoring error
```

**Solutions:**

**A. Check bid validity:**
- Score must be 0.0 to 1.0
- Confidence must be 0.0 to 1.0
- Estimated time must be positive

```log
# Valid bid:
TaskBid[announcement=abc123, bidder=SteveAgent, score=0.85, time=5000ms, conf=0.90, value=0.1530]

# Invalid bid:
TaskBid[announcement=abc123, bidder=SteveAgent, score=1.5, ...]  # Score > 1.0
```

**B. Check award selector:**
```toml
[coordination]
awardStrategy = "highest_value"  # or "lowest_time", "highest_score"
```

#### 5.3 Communication Bus Failures

**Symptom:** Agents not receiving messages

**Cause:** Event bus failure, message serialization error, bus not started

**Diagnosis:**
```log
[CommunicationBus] Failed to publish message: bus not initialized
[AgentCommunicationBus] Message delivery failed: timeout
```

**Solutions:**

**A. Verify event bus is initialized:**
```log
# Should see on startup:
[EventBus] Initialized with 5 subscribers
[CommunicationBus] Agent communication bus started
```

**B. Check message serialization:**
```log
# Should see:
[AgentCommunicationBus] Published message: TaskAnnouncement{type='mining', ...}
[AgentCommunicationBus] Delivered message to: SteveAgent[123]
```

**C. Increase message timeout:**
```toml
[coordination]
messageTimeout = 30000  # milliseconds (30 seconds)
```

#### 5.4 Task Conflicts

**Symptom:** Multiple agents trying to do same task

**Cause:** No conflict resolution, race condition, duplicate awards

**Diagnosis:**
```log
# Multiple agents working on same block:
[SteveAgent-1] Mining block at BlockPos{x=100, y=64, z=200}
[SteveAgent-2] Mining block at BlockPos{x=100, y=64, z=200}
```

**Solutions:**

**A. Enable conflict resolution:**
```toml
[coordination]
enableConflictResolution = true
conflictStrategy = "first_come"  # or "random", "closest"
```

**B. Use blackboard for coordination:**
```toml
[blackboard]
enabled = true
shareTaskState = true
shareWorldKnowledge = true
```

**C. Check task assignment:**
```log
# Should see unique assignments:
[ContractNetManager] Awarded task abc123 to SteveAgent-1
[ContractNetManager] Awarded task def456 to SteveAgent-2
```

---

## 6. Voice System Problems

### Symptom: Voice commands not recognized, no audio output

**Cause:** Microphone not configured, TTS service down, voice system disabled

**Diagnosis:**

```
[Search logs for]:
"Voice system not enabled"
"Microphone not found"
"TTS service error"
"Voice recognition failed"
```

**Solutions (in order):**

#### 6.1 Voice System Disabled

**Symptom:** Voice commands don't work

**Solution:**

```toml
[voice]
enabled = true
mode = "real"  # or "logging" (for testing), NOT "disabled"
```

**Verification:**
```log
# Should see:
[VoiceSystem] Voice system initialized: RealVoiceSystem
[VoiceSystem] Voice enabled: true
```

#### 6.2 Microphone Not Found

**Symptom:** `Microphone not found` or `No audio input device`

**Solution:**

**A. Check system microphone:**
- Verify microphone works in OS
- Check Minecraft microphone permissions
- Test with other voice applications

**B. Configure microphone:**
```toml
[voice]
microphoneName = "default"  # Or specific device name
sampleRate = 16000  # Hz (16kHz for speech recognition)
```

**C. Test voice input:**
```bash
# In-game, check if voice key is bound:
Settings → Controls → Voice → Push-to-Talk
# Press key and speak
```

#### 6.3 TTS Service Errors

**Symptom:** Agent doesn't speak responses

**Cause:** TTS service down, API key missing, voice not configured

**Diagnosis:**
```log
[TextToSpeech] Failed to synthesize speech: API error
[VoiceSystem] TTS service unavailable
```

**Solutions:**

**A. Check TTS configuration:**
```toml
[voice]
ttsProvider = "elevenlabs"  # or "docker_mcp", "system"
ttsVoice = "default"
```

**B. Verify API key (for ElevenLabs):**
```toml
[voice.elevenlabs]
apiKey = "xi-..."  # ElevenLabs API key
```

**C. Use system TTS (free, offline):**
```toml
[voice]
ttsProvider = "system"
```

#### 6.4 Speech Recognition Errors

**Symptom:** Voice commands not recognized correctly

**Cause:** Background noise, language mismatch, poor microphone quality

**Diagnosis:**
```log
[SpeechToText] Recognition failed: audio too short
[SpeechToText] Recognition result: "" (empty)
```

**Solutions:**

**A. Check language settings:**
```toml
[voice]
sttLanguage = "en-US"  # Must match your language
sttProvider = "whisper"  # or "system"
```

**B. Improve audio quality:**
- Use push-to-talk (hold key while speaking)
- Reduce background noise
- Speak clearly and close to microphone

**C. Enable logging mode for debugging:**
```toml
[voice]
mode = "logging"  # Prints recognized text to console instead of executing
```

**Verification:**
```log
# Should see recognized text:
[LoggingVoiceSystem] Recognized: "build a house"
```

---

## Debug Logging

### Enable Debug Logging

Edit `config/log4j.xml` or use in-game commands:

```xml
<!-- Add to log4j.xml -->
<Logger name="com.minewright" level="DEBUG" />
```

Or via command:
```
/log set com.minewright DEBUG
```

### Key Log Categories

```bash
# LLM issues
grep -i "llm\|circuit\|retry\|timeout" logs/latest.log

# Agent stuck
grep -i "stuck\|pathfind\|state.*transition" logs/latest.log

# Configuration
grep -i "config\|provider\|api.*key" logs/latest.log

# Coordination
grep -i "bid\|award\|contract\|announcement" logs/latest.log

# Voice
grep -i "voice\|stt\|tts\|microphone" logs/latest.log
```

### Log Levels

```
ERROR  - Critical failures (circuit breaker open, crashes)
WARN   - Recoverable issues (retry, timeout, stuck)
INFO   - State transitions, task completion
DEBUG  - Detailed diagnostics (pathfinding, bidding)
TRACE  - Extremely verbose (every tick, every message)
```

---

## Common Error Messages

### LLM Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Circuit breaker state: CLOSED -> OPEN` | 5+ consecutive API failures | Wait 60s or check API key |
| `Request failed after all retries` | API not responding | Check internet, switch provider |
| `Rate limiter rejected request` | Hit API rate limit | Reduce agent count, increase delay |
| `Failed to parse LLM response` | Malformed JSON from LLM | Lower temperature, check model |
| `API key not found` | Missing or invalid API key | Configure API key in config |

### Agent Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `position stuck for X ticks` | Agent not moving | Check pathfinding, clear obstacles |
| `pathfinding stuck` | No valid path to target | Move closer, clear path, reduce distance |
| `Invalid state transition: X -> Y` | State machine logic error | Check valid transitions in code |
| `No path found to target` | Target unreachable | Verify target exists and is accessible |
| `progress stuck for X ticks` | Task not progressing | Check action implementation, verify target |

### Configuration Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Invalid provider: xxx` | Invalid provider name | Use: groq, openai, or gemini |
| `API key is required` | Missing API key | Add API key to config or env var |
| `Invalid value for temperature` | Value out of range | Use 0.0 to 2.0 |
| `Failed to parse config file` | TOML syntax error | Validate TOML syntax |
| `Configuration reload failed` | Invalid config values | Fix config values and reload |

### Coordination Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `No bids received for task` | Agents not bidding | Check capabilities, increase timeout |
| `Task award failed: no eligible bidders` | No valid bids | Check bid scores, verify requirements |
| `Communication bus error` | Event bus failure | Check bus initialization |
| `Message delivery failed: timeout` | Network/bus timeout | Increase message timeout |
| `Multiple agents assigned to same task` | No conflict resolution | Enable conflict resolution |

### Voice Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Voice system not enabled` | Voice disabled in config | Set `voice.enabled = true` |
| `Microphone not found` | No audio input device | Check system microphone |
| `TTS service error` | TTS API down or invalid | Check API key, switch provider |
| `Recognition failed: audio too short` | Voice input too short | Speak longer, use push-to-talk |
| `STT service unavailable` | Speech recognition service down | Check STT provider config |

---

## Recovery Commands

### In-Game Commands

```bash
# List all agents
/foreman list

# Remove stuck agent
/foreman remove <agent_name>

# Spawn new agent
/foreman spawn <agent_name>

# Issue command
/foreman order <agent_name> <command>

# Stop current task
/foreman order <agent_name> stop

# Set agent idle
/foreman order <agent_name> idle

# Reload configuration
/reload
```

### Configuration Reload

```bash
# Edit config file
# config/minewright-common.toml

# Reload in-game
/reload

# Check reload success
# Look for: "Configuration reloaded" in logs
```

### Game Restart

When all else fails, restart Minecraft:

1. Save and quit game
2. Check logs for errors
3. Fix configuration issues
4. Start Minecraft
5. Test with single agent first

---

## Getting Help

### Information to Collect

When reporting issues, include:

1. **Log file excerpts** (search for ERROR, WARN)
2. **Configuration file** (sanitize API keys)
3. **Minecraft version** and Forge version
4. **Steve AI version** (check mod list)
5. **Steps to reproduce** the issue
6. **Expected vs actual** behavior

### Useful Debug Info

```bash
# Minecraft version: 1.20.1
# Forge version: 47.2.0
# Steve AI version: 1.0.0
# Java version: 17

# Log location:
# - Windows: %appdata%/.minecraft/logs/
# - Linux/Mac: ~/.minecraft/logs/

# Config location:
# - Windows: %appdata%/.minecraft/config/
# - Linux/Mac: ~/.minecraft/config/
```

---

## Quick Reference

### Symptom → Quick Fix

| Symptom | Quick Fix |
|---------|-----------|
| Agent stuck in PLANNING | Check API key, switch to Groq |
| Agent not moving | Check pathfinding, clear obstacles |
| Game lag | Reduce agent count, increase heap |
| Config errors | Validate TOML syntax, reload config |
| Agents not cooperating | Check capabilities, increase bid timeout |
| Voice not working | Enable voice system, check microphone |
| Out of memory | Increase `-Xmx` to 4GB |
| Circuit breaker open | Wait 60s or fix API key |
| High API costs | Enable caching, use Groq |

### Emergency Actions

```bash
# 1. Stop all agents
/foreman remove *  (if supported)

# 2. Reduce agent count
Edit config: maxActiveCrewMembers = 1

# 3. Switch to free provider
Edit config: provider = "groq"

# 4. Disable expensive features
Edit config: enableCache = true, enableBatching = true

# 5. Restart game
```

---

**Document End**

For the latest troubleshooting information, check:
- GitHub Issues: https://github.com/your-repo/minewright/issues
- Discord Community: https://discord.gg/your-server
- Documentation: https://your-docs-site.com
