# MineWright Troubleshooting Guide

**Last Updated:** 2026-03-01
**Version:** 1.0

This guide helps you diagnose and resolve common issues with MineWright.

---

## Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [Installation Issues](#installation-issues)
3. [Configuration Issues](#configuration-issues)
4. [In-Game Issues](#in-game-issues)
5. [Performance Issues](#performance-issues)
6. [LLM API Issues](#llm-api-issues)
7. [Crash Recovery](#crash-recovery)
8. [Debug Logging](#debug-logging)
9. [Getting Help](#getting-help)

---

## Quick Diagnostics

### Health Check

Run through these checks to identify your issue:

**Check 1: Mod Loading**
```
✓ MineWright appears in Minecraft mods list
✗ MineWright missing from mods list
```
**Solution:** See [Installation Issues](#installation-issues)

**Check 2: Configuration**
```
✓ config/minewright-common.toml exists
✗ Config file missing or corrupted
```
**Solution:** See [Configuration Issues](#configuration-issues)

**Check 3: API Key**
```
✓ Logs show "API key configured: sk-***..."
✗ Logs show "API key not configured"
```
**Solution:** See [Configuration Issues](#configuration-issues)

**Check 4: Agent Spawning**
```
✓ /minewright spawn works
✗ Command unknown or agent doesn't appear
```
**Solution:** See [In-Game Issues](#in-game-issues)

**Check 5: Agent Response**
```
✓ Agent responds to commands
✗ Agent silent or shows errors
```
**Solution:** See [LLM API Issues](#llm-api-issues)

---

## Installation Issues

### Issue: "Failed to load mod"

**Symptoms:**
- Minecraft crashes on startup
- Crash report mentions MineWright
- Mod doesn't appear in mods list

**Diagnosis:**

1. **Check Minecraft Version:**
   - You must be using **Minecraft 1.20.1**
   - Other versions will not work

2. **Check Forge Version:**
   - You must have **Forge 47.x** installed
   - Run `/forge version` in game to check

3. **Check Java Version:**
   - Run `java -version` in terminal/command prompt
   - Must be **Java 17 or higher**

**Solutions:**

**Solution 1: Install Correct Forge**
1. Download Forge 1.20.1 from [files.minecraftforge.net](https://files.minecraftforge.net/)
2. Run the installer
3. Select "Install Client"
4. Launch Minecraft with Forge profile

**Solution 2: Update Java**
1. Download Java 17+ from [Adoptium](https://adoptium.net/)
2. Install Java
3. Update Minecraft Launcher JVM settings to use new Java
4. Relaunch Minecraft

**Solution 3: Check for Mod Conflicts**
1. Move other mods out of the `mods` folder temporarily
2. Try launching with only MineWright
3. If it works, add mods back one at a time to find the conflict

**Solution 4: Redownload Mod**
1. Delete the existing MineWright JAR
2. Download fresh copy from [Releases](https://github.com/SuperInstance/MineWright/releases)
3. Ensure file is not corrupted (file size should match release)

### Issue: "Mods loaded but nothing works"

**Symptoms:**
- Mod appears in mods list
- Commands don't work
- No error messages

**Solutions:**

**Solution 1: Verify Forge Profile**
1. Open Minecraft Launcher
2. Click arrow next to "Play"
3. Ensure "Forge 1.20.1" is selected
4. Click Play

**Solution 2: Check Server vs Client**
- Some commands only work on certain game modes
- Single-player: All commands available
- Multi-player server: Server must have mod installed

---

## Configuration Issues

### Issue: "API key not configured"

**Symptoms:**
- Logs show: "API key not configured"
- Agent spawns but doesn't respond to commands
- Commands fail silently

**Solutions:**

**Solution 1: Check Config File Exists**
1. Navigate to `config/` folder in Minecraft directory
2. Verify `minewright-common.toml` exists
3. If missing, copy `minewright-common.toml.example` and rename it

**Solution 2: Verify API Key Format**
Edit `config/minewright-common.toml`:
```toml
[openai]
apiKey = "${MINEWRIGHT_API_KEY}"  # Environment variable
# OR
apiKey = "sk-your-actual-key-here"  # Direct entry
```

**Solution 3: Set Environment Variable**
```bash
# Linux/macOS
export MINEWRIGHT_API_KEY="your-key-here"

# Windows PowerShell
$env:MINEWRIGHT_API_KEY="your-key-here"

# Windows CMD
set MINEWRIGHT_API_KEY=your-key-here
```

Then launch Minecraft from the same terminal.

**Solution 4: Restart Minecraft**
- Environment variables are read at launch
- Must set variable before opening Minecraft
- Relaunch after setting variable

### Issue: "Invalid API key"

**Symptoms:**
- Logs show: "Authentication failed" or "Invalid API key"
- LLM returns 401 error

**Solutions:**

**Solution 1: Verify API Key**
1. Log in to your API provider's dashboard
2. Copy the API key again
3. Ensure no extra spaces or characters
4. Update config file

**Solution 2: Check Provider Match**
```toml
[ai]
provider = "openai"  # Ensure this matches your key provider

[openai]
apiKey = "${MINEWRIGHT_API_KEY}"  # OpenAI/z.ai key
```

```toml
[ai]
provider = "groq"  # For Groq

[groq]
apiKey = "${GROQ_API_KEY}"
```

**Solution 3: Check API Key Status**
1. Log in to your provider dashboard
2. Check if key is active
3. Check if quota/credits are available
4. Verify key permissions

### Issue: "Connection timeout"

**Symptoms:**
- Commands hang for 30+ seconds
- Timeout error in logs
- Agent stuck in "PLANNING" state

**Solutions:**

**Solution 1: Check Internet Connection**
1. Verify your internet is working
2. Try opening API provider website in browser
3. Check firewall settings

**Solution 2: Switch Provider**
Some providers are faster:
- **Groq:** Very fast, free
- **z.ai GLM:** Fast, reliable
- **OpenAI:** Moderate speed
- **Gemini:** Moderate speed

```toml
[ai]
provider = "groq"  # Switch to faster provider
```

**Solution 3: Increase Timeout**
Edit `config/minewright-common.toml`:
```toml
[openai]
timeoutMs = 60000  # Increase to 60 seconds
```

**Solution 4: Check Proxy/VPN**
- If using VPN, try disabling it
- If behind proxy, configure Java proxy settings
- Check if API provider is blocked in your region

---

## In-Game Issues

### Issue: Agent won't spawn

**Symptoms:**
- `/minewright spawn` command does nothing
- "Unknown command" error
- Command registered but agent doesn't appear

**Solutions:**

**Solution 1: Check Command Syntax**
```
/minewright spawn AgentName
```
- Must provide a name
- Name cannot have spaces (use underscores)

**Solution 2: Check Available Space**
- Agent needs 2x2x2 space to spawn
- Try spawning in open area
- Ensure not spawning inside blocks

**Solution 3: Check Max Agents**
Edit config:
```toml
[behavior]
maxActiveCrewMembers = 10
```
- Cannot exceed this limit
- Remove existing agents with `/minewright remove`

**Solution 4: Check Game Mode**
- Some commands disabled in spectator mode
- Ensure you're in survival or creative mode

### Issue: Agent stuck or not moving

**Symptoms:**
- Agent spawns but stands still
- Agent doesn't respond to commands
- Agent walks in circles

**Diagnosis:**

1. **Check Agent State:**
   ```
   /minewright list
   ```
   - Shows current state: IDLE, PLANNING, EXECUTING, ERROR

2. **Check Logs:**
   - Look for pathfinding errors
   - Look for action failures
   - Look for LLM errors

**Solutions:**

**Solution 1: Ensure Valid Spawn Location**
- Agent must spawn on solid ground
- Try respawning in open, flat area
- Use `/minewright remove` then `/minewright spawn` again

**Solution 2: Check Pathfinding**
- Agent needs clear path to target
- Remove obstacles between agent and destination
- Try commands in open terrain first

**Solution 3: Increase Tick Delay**
```toml
[behavior]
actionTickDelay = 40  # Increase if server is lagging
```

**Solution 4: Give Simple Commands**
Start with simple commands:
```
/minewright order AgentName "say hello"
/minewright order AgentName "follow me"
```

Then progress to complex tasks.

### Issue: Agent falls through world

**Symptoms:**
- Agent spawns and immediately falls
- Agent falls through blocks
- Agent teleports randomly

**Solutions:**

**Solution 1: Spawn at Ground Level**
- Spawn at y=64 or higher
- Ensure ground is solid (not leaves, snow, etc.)

**Solution 2: Ensure Valid Block Underneath**
- Agent needs solid block to stand on
- Try spawning on stone, dirt, grass
- Avoid spawning on slabs, stairs, fluids

**Solution 3: Check World Corruption**
- Try spawning in new world
- If works in new world, old world may have issues
- Backup and restore world from earlier save

---

## Performance Issues

### Issue: Low FPS / Lag

**Symptoms:**
- Minecraft runs slowly with mod
- Frame rate drops when agents are active
- Delayed agent responses

**Solutions:**

**Solution 1: Reduce Agent Count**
```toml
[behavior]
maxActiveCrewMembers = 3  # Reduce from 10
```

**Solution 2: Increase Tick Delay**
```toml
[behavior]
actionTickDelay = 40  # Increase from 20 (1 second → 2 seconds)
```

**Solution 3: Enable Strict Budget**
```toml
[performance]
aiTickBudgetMs = 3
strictBudgetEnforcement = true
```

**Solution 4: Increase JVM Memory**
Add to Minecraft Launcher JVM Arguments:
```
-Xmx4G -Xms2G
```

**Solution 5: Optimize Java**
Use JVM arguments:
```
-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200
```

### Issue: Out of Memory Crash

**Symptoms:**
- Minecraft crashes with OutOfMemoryError
- Game becomes slow then crashes
- "Java heap space" error

**Solutions:**

**Solution 1: Increase Heap Size**
Add to Minecraft Launcher:
```
-Xmx6G
```
Adjust based on your system RAM:
- 8GB RAM: `-Xmx4G`
- 16GB RAM: `-Xmx6G`
- 32GB RAM: `-Xmx8G`

**Solution 2: Reduce Concurrent Operations**
```toml
[behavior]
maxActiveCrewMembers = 3

[performance]
aiTickBudgetMs = 3
```

**Solution 3: Close Other Applications**
- Close web browsers
- Close other games
- Free up system RAM

**Solution 4: Disable Features**
```toml
[semantic_cache]
enabled = false

[voice]
enabled = false
```

---

## LLM API Issues

### Issue: Rate Limit / Quota Exceeded

**Symptoms:**
- "Rate limit exceeded" error in logs
- Agent stops responding after many commands
- 429 Too Many Requests error

**Solutions:**

**Solution 1: Enable Batching**
```toml
[llm]
enableBatching = true
batchSize = 5
```

**Solution 2: Enable Caching**
```toml
[semantic_cache]
enabled = true
max_size = 500
```

**Solution 3: Use Cascade Router**
```toml
[cascade_router]
enabled = true
```
Routes simple commands to smaller/faster models.

**Solution 4: Increase Quota**
- Upgrade your API plan
- Check provider dashboard for quota limits
- Wait for quota to reset (daily/hourly)

### Issue: High API Costs

**Symptoms:**
- Unexpectedly high API bills
- Rapid token consumption

**Solutions:**

**Solution 1: Use Free Providers**
```toml
[ai]
provider = "groq"  # Free
```

**Solution 2: Enable All Optimizations**
```toml
[semantic_cache]
enabled = true

[cascade_router]
enabled = true

[llm]
enableBatching = true
```

**Solution 3: Use Smaller Models**
```toml
[openai]
model = "glm-4-flash"  # Faster, cheaper
```

**Solution 4: Monitor Usage**
Check logs for:
- Token count per request
- Number of API calls
- Cache hit rate

### Issue: Poor Response Quality

**Symptoms:**
- Agent doesn't understand commands
- Agent gives wrong actions
- Confusing responses

**Solutions:**

**Solution 1: Adjust Temperature**
```toml
[openai]
temperature = 0.5  # Lower = more deterministic
```

**Solution 2: Use Better Model**
```toml
[openai]
model = "glm-5"  # More capable
```

**Solution 3: Clear Cache**
Sometimes stale cached responses cause issues:
1. Delete `config/minewright-common.toml`
2. Restart Minecraft to regenerate
3. Try command again

**Solution 4: Rephrase Commands**
- Be specific: "build a 5x5x4 stone house" vs "build a house"
- Use simple language
- Break complex tasks into steps

---

## Crash Recovery

### Recovering from Crashes

**Immediate Actions:**

1. **Backup Your World:**
   - Copy `.minecraft/saves/YourWorldName`
   - Store in safe location

2. **Check Crash Report:**
   - Location: `crash-reports/`
   - File: `crash-YYYY-MM-DD_HH.MM.SS-server.txt`
   - Look for "MineWright" in the report

3. **Check Logs:**
   - Location: `logs/latest.log`
   - Search for "ERROR" or "FATAL"

**Common Crash Causes:**

| Error | Cause | Solution |
|-------|-------|----------|
| OutOfMemoryError | Insufficient RAM | Increase `-Xmx` JVM argument |
| NullPointerException | Bug in mod | Report issue on GitHub |
| ClassNotFoundException | Incomplete install | Reinstall mod, verify JAR integrity |
| NoSuchMethodError | Mod conflict | Remove conflicting mods |

**Recovery Steps:**

1. **Identify the Crash Type** from crash report
2. **Apply Solution** from table above
3. **Restart Minecraft**
4. **Load World** (may need to restore from backup)

### World Corruption

**Symptoms:**
- World fails to load
- Chunks missing
- Blocks changing randomly

**Solutions:**

**Solution 1: Restore Backup**
```bash
# Backup your world regularly!
cp -r .minecraft/saves/YourWorldName ~/.minecraft/backups/
```

**Solution 2: Remove Agent Data**
1. Close Minecraft
2. Navigate to world save: `.minecraft/saves/YourWorldName/`
3. Delete `minewright/` folder (if exists)
4. Restart Minecraft
5. Spawn new agents

**Solution 3: Use Minecraft Region Fixer**
- External tool for fixing corrupted worlds
- Download from GitHub
- Run on your world save

---

## Debug Logging

### Enable Debug Logging

**Step 1: Enable in Config**
Edit `config/minewright-common.toml`:
```toml
[general]
debugLogging = true
```

**Step 2: Enable Minecraft Debug**
1. Open Minecraft Launcher
2. Go to Launch Options
3. Select your Forge profile
4. Enable "Open Game Log"
5. Or add JVM argument: `-Dforge.logging.markers=REGISTRIES,SCAN,FORGE,EVENT`

**Step 3: Check Logs**
- Location: `logs/latest.log`
- Real-time: `tail -f logs/latest.log` (Linux/Mac) or use text editor

### Log Locations

| Log File | Purpose | Location |
|----------|---------|----------|
| `latest.log` | Current session | `.minecraft/logs/` |
| `debug.log` | Debug output | `.minecraft/logs/` |
| `crash-reports/*.txt` | Crash details | `.minecraft/crash-reports/` |

### Useful Log Searches

**Find Errors:**
```
grep ERROR logs/latest.log
```

**Find MineWright Messages:**
```
grep MineWright logs/latest.log
```

**Find API Issues:**
```
grep "API" logs/latest.log
```

**Find Pathfinding Issues:**
```
grep path logs/latest.log
```

### Creating Bug Reports

When reporting issues, include:

1. **System Info:**
   ```
   OS: Windows 11 / macOS 14 / Ubuntu 22.04
   Java: openjdk 17.0.2
   Minecraft: 1.20.1
   Forge: 47.2.0
   MineWright: 1.0.0
   ```

2. **Configuration:**
   - Paste `config/minewright-common.toml` (remove API key)

3. **Relevant Logs:**
   - Error messages from `logs/latest.log`
   - Crash report if applicable

4. **Steps to Reproduce:**
   ```
   1. Spawn agent with /minewright spawn Steve
   2. Issue command: /minewright order Steve "build a house"
   3. Agent freezes
   4. Error in logs: [...]
   ```

---

## Getting Help

### Self-Service Resources

- [Installation Guide](INSTALLATION.md)
- [Configuration Guide](CONFIGURATION.md)
- [Performance Guide](PERFORMANCE.md)
- [Main README](../README.md)

### Community Support

- [GitHub Issues](https://github.com/SuperInstance/MineWright/issues)
  - Search existing issues first
  - Create new issue with details

- [GitHub Discussions](https://github.com/SuperInstance/MineWright/discussions)
  - Ask questions
  - Share setups
  - Get help from community

### When Creating Issues

Include the following information:

**Required:**
- Minecraft version
- Forge version
- MineWright version
- Steps to reproduce
- Expected behavior
- Actual behavior

**Helpful:**
- Configuration file (with API key removed)
- Relevant log excerpts
- Screenshots/videos
- System specs (OS, Java version, RAM)

### Emergency Recovery

If something goes seriously wrong:

1. **Stop Minecraft** immediately
2. **Backup your world** to safe location
3. **Remove MineWright JAR** from mods folder
4. **Restart Minecraft** to ensure stability
5. **Report issue** with crash logs
6. **Reinstall** when fix is available

---

**Document Version:** 1.0
**Last Updated:** 2026-03-01
**Maintained By:** MineWright Development Team
