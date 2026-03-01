# MineWright Performance Optimization Guide

**Last Updated:** 2026-03-01
**Version:** 1.0

This guide helps you optimize MineWright for smooth gameplay and responsive AI agents.

---

## Table of Contents

1. [Performance Fundamentals](#performance-fundamentals)
2. [Hardware Requirements](#hardware-requirements)
3. [Recommended Settings by Hardware](#recommended-settings-by-hardware)
4. [Configuration Tuning](#configuration-tuning)
5. [Java/Minecraft Optimization](#javaminecraft-optimization)
6. [Network & API Optimization](#network--api-optimization)
7. [Monitoring Performance](#monitoring-performance)
8. [Troubleshooting Performance Issues](#troubleshooting-performance-issues)

---

## Performance Fundamentals

### What Affects Performance

MineWright uses your computer's resources in several ways:

| Resource | How It's Used | Impact |
|----------|---------------|--------|
| **CPU** | Pathfinding, AI decisions, action execution | More agents = more CPU needed |
| **RAM** | Agent memory, caching, world data | Each agent uses ~100-200MB |
| **Network** | Communication with AI API | Faster internet = quicker responses |
| **Disk** | Logs, world saves | Minimal impact |

### The Tick Budget System

MineWright uses a "tick budget" to prevent lag:

- Each game tick = 50ms (20 ticks per second)
- AI gets a small budget (default: 5ms per tick)
- If AI exceeds budget, operations are delayed to next tick
- This keeps your game smooth even with complex AI

**Trade-off:** Lower budget = smoother gameplay but slower agent responses

---

## Hardware Requirements

### Minimum (Playable)

**Hardware:**
- CPU: Dual-core 2.5 GHz
- RAM: 4 GB total (6 GB recommended)
- Internet: Stable connection

**Expected Experience:**
- 1-2 agents
- Simple commands
- Occasional lag during complex planning

### Recommended (Smooth)

**Hardware:**
- CPU: Quad-core 3.0 GHz
- RAM: 8 GB total (12 GB recommended)
- Internet: Good connection

**Expected Experience:**
- 3-5 agents
- Moderate complexity commands
- Smooth gameplay

### Optimal (Maximum)

**Hardware:**
- CPU: 6+ cores at 3.5+ GHz
- RAM: 16 GB total (24 GB recommended)
- Internet: Excellent connection

**Expected Experience:**
- 10+ agents
- Complex multi-agent projects
- No lag

---

## Recommended Settings by Hardware

### Low-End PC (4GB RAM, Dual-Core CPU)

**Configuration:**
```toml
# In config/minewright-common.toml
[behavior]
maxActiveCrewMembers = 2
actionTickDelay = 40  # Check every 2 seconds
enableChatResponses = false  # Disable chat for performance

[performance]
aiTickBudgetMs = 3  # Strict budget
strictBudgetEnforcement = true

[semantic_cache]
enabled = false  # Disable caching to save RAM

[pathfinding]
max_search_nodes = 5000  # Limit pathfinding
```

**Minecraft Launcher JVM Arguments:**
```
-Xmx3G -Xms2G -XX:+UseG1GC
```

**In-Game Settings:**
- Render Distance: 6 chunks
- Graphics: Fast
- Smooth Lighting: Off
- Particles: Minimal

**What to Expect:**
- 2 agents working together
- Simple tasks like mining, basic building
- Playable but may lag during complex commands

---

### Mid-Range PC (8GB RAM, Quad-Core CPU)

**Configuration:**
```toml
[behavior]
maxActiveCrewMembers = 5
actionTickDelay = 20  # Check every 1 second (default)
enableChatResponses = true

[performance]
aiTickBudgetMs = 5  # Balanced budget
strictBudgetEnforcement = true

[semantic_cache]
enabled = true
max_size = 300

[pathfinding]
max_search_nodes = 10000  # Default
```

**Minecraft Launcher JVM Arguments:**
```
-Xmx6G -Xms4G -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**In-Game Settings:**
- Render Distance: 8-10 chunks
- Graphics: Fancy or Fast
- Smooth Lighting: Minimum
- Particles: Normal

**What to Expect:**
- 5 agents working together
- Complex building projects
- Smooth gameplay

---

### High-End PC (16GB+ RAM, 6+ Cores)

**Configuration:**
```toml
[behavior]
maxActiveCrewMembers = 10
actionTickDelay = 10  # Check every 0.5 seconds
enableChatResponses = true

[performance]
aiTickBudgetMs = 8  # More generous budget
strictBudgetEnforcement = false  # Track but don't limit

[semantic_cache]
enabled = true
max_size = 500

[pathfinding]
max_search_nodes = 20000  # Allow longer paths

[voice]
enabled = true  # Enable voice features
```

**Minecraft Launcher JVM Arguments:**
```
-Xmx12G -Xms8G -XX:+UseG1GC -XX:MaxGCPauseMillis=150
```

**In-Game Settings:**
- Render Distance: 12+ chunks
- Graphics: Fancy
- Smooth Lighting: Maximum
- Particles: All

**What to Expect:**
- 10+ agents working together
- Massive construction projects
- No lag whatsoever

---

## Configuration Tuning

### Agent Count

**maxActiveCrewMembers** - Maximum number of AI agents

```toml
[behavior]
maxActiveCrewMembers = 5
```

**Guidelines:**
- 2-3 agents: Dual-core CPU
- 5-7 agents: Quad-core CPU
- 10+ agents: 6+ core CPU

**Impact:** Each additional agent adds ~5-10% CPU usage

### Action Tick Delay

**actionTickDelay** - How often agents check for new actions

```toml
[behavior]
actionTickDelay = 20  # Ticks (20 = 1 second)
```

**Options:**
- 10 ticks (0.5s): Very responsive, more CPU
- 20 ticks (1s): Balanced (default)
- 40 ticks (2s): Less responsive, less CPU
- 60 ticks (3s): Minimal CPU impact

**Trade-off:** Lower = snappier agents, Higher = less CPU usage

### AI Tick Budget

**aiTickBudgetMs** - Maximum time AI can use per tick

```toml
[performance]
aiTickBudgetMs = 5  # Milliseconds
```

**Warning:** Setting this too high WILL cause FPS drops!

**Guidelines:**
- 3ms: Very safe, slower agents
- 5ms: Balanced (default)
- 7ms: Aggressive, may cause minor lag
- 10ms+: Dangerous, likely to lag

### Caching

**semantic_cache** - Remembers similar commands to avoid repeated API calls

```toml
[semantic_cache]
enabled = true
max_size = 500
similarity_threshold = 0.85
```

**Benefits:**
- 30-50% fewer API calls
- Faster responses for repeated commands
- Lower API costs

**Trade-off:** Uses ~50-100MB of RAM

---

## Java/Minecraft Optimization

### Memory Settings

**Set in Minecraft Launcher → JVM Arguments:**

```
-Xmx6G -Xms4G
```

**-Xmx** (Maximum Heap Size):
- 4GB: Minimum for smooth gameplay
- 6GB: Recommended for 5+ agents
- 8GB: For 10+ agents

**-Xms** (Initial Heap Size):
- Set to 50-75% of -Xmx
- Prevents runtime resizing
- Reduces lag spikes

### Recommended JVM Arguments

**For 8GB RAM System:**
```
-Xmx6G -Xms4G -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**For 16GB RAM System:**
```
-Xmx12G -Xms8G -XX:+UseG1GC -XX:MaxGCPauseMillis=150
```

**What These Do:**
- `-Xmx`: Maximum memory Minecraft can use
- `-Xms`: Memory allocated at startup
- `-XX:+UseG1GC`: Better garbage collector
- `-XX:MaxGCPauseMillis`: Target GC pause time

### In-Game Settings

**For Best Performance:**
- Render Distance: 6-8 chunks
- Simulation Distance: 6 chunks
- Graphics: Fast
- Smooth Lighting: Off or Minimum
- Particles: Minimal
- VSync: Off (if you have high FPS monitor)
- View Bobbing: Off
- Clouds: Off or Fast
- Entity Shadows: Off

---

## Network & API Optimization

### Choosing an API Provider

**Speed Comparison:**

| Provider | Speed | Cost | Best For |
|----------|-------|------|----------|
| **Groq** | Very Fast (~200ms) | Free | Testing, simple commands |
| **z.ai GLM-4** | Fast (~300ms) | Free tier | Balanced performance |
| **z.ai GLM-5** | Medium (~500ms) | Paid | Complex planning |
| **Gemini Pro** | Medium (~800ms) | Free tier | Backup option |

**Recommendation:** Start with Groq (fastest free), upgrade to GLM-5 for complex projects.

**Configuration:**
```toml
[ai]
provider = "groq"  # Change to preferred provider

[groq]
apiKey = "${GROQ_API_KEY}"
```

### Reducing API Latency

**Enable Caching:**
```toml
[semantic_cache]
enabled = true
max_size = 500
```

**Enable Batching** (for multiple agents):
```toml
[llm]
enableBatching = true
batchSize = 5
```

**Use Cascade Router** (routes simple commands to faster models):
```toml
[cascade_router]
enabled = true
```

---

## Monitoring Performance

### In-Game Monitoring

**Press F3** to see:
- **FPS:** Should be 60+ for smooth gameplay
- **Chunk updates:** Should be minimal when agents are active
- **Entity count:** Shows all active entities

### Check Agent Status

```
/minewright list
```

Shows:
- Number of active agents
- Current state (IDLE, PLANNING, EXECUTING)
- Current action

### Check Logs

**Location:** `.minecraft/logs/latest.log`

**Search for Warnings:**
```
WARN budget exceeded
```

**Check API Response Times:**
```
API response time: 250ms
```

### Performance Metrics

**Enable in Config:**
```toml
[performance]
enableMetrics = true
```

This logs:
- Average tick time
- API call duration
- Cache hit rate
- Agent count

---

## Troubleshooting Performance Issues

### Issue: Low FPS with Agents Active

**Symptoms:**
- FPS drops when agents are working
- Lag during agent actions

**Solutions:**
1. **Reduce agent count:**
   ```toml
   [behavior]
   maxActiveCrewMembers = 3  # Reduce from current
   ```

2. **Increase tick delay:**
   ```toml
   [behavior]
   actionTickDelay = 40  # Check less frequently
   ```

3. **Lower tick budget:**
   ```toml
   [performance]
   aiTickBudgetMs = 3  # Be more strict
   ```

4. **Lower Minecraft settings:**
   - Reduce render distance to 6-8 chunks
   - Set graphics to Fast
   - Disable smooth lighting

### Issue: Slow Agent Responses

**Symptoms:**
- Agents take long time to respond to commands
- Long planning phase

**Solutions:**
1. **Switch to faster API provider:**
   ```toml
   [ai]
   provider = "groq"  # Fastest free option
   ```

2. **Enable caching:**
   ```toml
   [semantic_cache]
   enabled = true
   ```

3. **Increase tick budget slightly:**
   ```toml
   [performance]
   aiTickBudgetMs = 7  # Allow more processing
   ```

### Issue: Out of Memory Crashes

**Symptoms:**
- Minecraft crashes with "OutOfMemoryError"
- Game becomes slow then crashes

**Solutions:**
1. **Increase JVM memory:**
   ```
   -Xmx6G  # Increase this value
   ```

2. **Reduce agent count:**
   ```toml
   [behavior]
   maxActiveCrewMembers = 3
   ```

3. **Disable caching to save RAM:**
   ```toml
   [semantic_cache]
   enabled = false
   ```

4. **Close other applications**
   - Close web browsers
   - Close other games
   - Free up system RAM

### Issue: Stuttering Gameplay

**Symptoms:**
- FPS fluctuates wildly
- Occasional freeze-frames

**Solutions:**
1. **Enable G1GC** (already in recommended JVM args)

2. **Increase GC pause target:**
   ```
   -XX:MaxGCPauseMillis=300  # Increase from 200
   ```

3. **Reduce tick budget:**
   ```toml
   [performance]
   aiTickBudgetMs = 3  # Be more strict
   ```

4. **Lower render distance:**
   - In Video Settings, set to 6-8 chunks

---

## Performance Quick Reference

### Settings Quick Adjustments

**For immediate performance boost:**
1. Reduce `maxActiveCrewMembers` to 3
2. Set `actionTickDelay` to 40
3. Set `aiTickBudgetMs` to 3
4. Lower Minecraft render distance to 6 chunks

**For maximum agent capability:**
1. Increase `maxActiveCrewMembers` to 10
2. Set `actionTickDelay` to 10
3. Set `aiTickBudgetMs` to 8
4. Ensure 8GB+ RAM allocated to Minecraft

### Performance Checklist

**Before Playing:**
- [ ] Java 17+ installed
- [ ] Minecraft allocated sufficient RAM (-Xmx6G or more)
- [ ] Config file created and API key set
- [ ] Appropriate settings for your hardware

**When Experiencing Lag:**
- [ ] Reduce agent count
- [ ] Increase action tick delay
- [ ] Lower Minecraft render distance
- [ ] Check for other applications using CPU/RAM

**For Long-Playing Sessions:**
- [ ] Restart Minecraft every 2-3 hours
- [ ] Monitor RAM usage
- [ ] Check logs for warnings

---

## Additional Resources

- [Installation Guide](INSTALLATION.md) - Setup instructions
- [Configuration Guide](CONFIGURATION.md) - All config options
- [Troubleshooting Guide](TROUBLESHOOTING.md) - Common issues and fixes
- [Main README](../README.md) - Project overview

---

**Document Version:** 1.0
**Last Updated:** 2026-03-01
**Maintained By:** MineWright Development Team
