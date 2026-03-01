# MineWright Quick Start Guide

Get your AI construction crew up and running in 5 minutes.

---

## What You'll Need

### Prerequisites

- **Minecraft 1.20.1** with Forge 47.x installed
- **Java 17+** (Java 21 recommended for best performance)
- **An API key** from one of these providers:
  - [z.ai GLM](https://open.bigmodel.cn/) (recommended, free tier available)
  - [Groq](https://groq.com/) (fastest, free tier available)
  - [OpenAI](https://openai.com/) (paid, most capable)
  - [Gemini](https://ai.google.dev/) (free tier available)

### Recommended System Requirements

- **RAM:** 4GB minimum (8GB recommended)
- **CPU:** Modern dual-core or better
- **Internet:** Stable connection for AI API calls

---

## Installation

### Step 1: Install the Mod

1. Download the latest `minewright-1.0.0-all.jar` from [Releases](https://github.com/SuperInstance/MineWright/releases)
2. Copy the JAR file to your Minecraft `mods` folder:
   - **Windows:** `%appdata%\.minecraft\mods\`
   - **Mac:** `~/Library/Application Support/minecraft/mods/`
   - **Linux:** `~/.minecraft/mods/`

### Step 2: Launch Minecraft

Start Minecraft using the Forge 1.20.1 profile. The mod will generate a default configuration file.

---

## First-Time Setup

### Step 1: Get an API Key

**Recommended: z.ai GLM (Free Tier Available)**

1. Visit [https://open.bigmodel.cn/](https://open.bigmodel.cn/)
2. Create an account
3. Generate an API key
4. Copy the key (starts with a pattern like `xxxxxxxx`)

**Alternative: Groq (Fastest, Free)**

1. Visit [https://groq.com/](https://groq.com/)
2. Create an account
3. Generate an API key from the dashboard
4. Copy the key (starts with `gsk_`)

### Step 2: Configure the Mod

#### Option A: Using Environment Variables (Recommended)

1. Open your config file: `config/minewright-common.toml`
2. Verify it has this line:
   ```toml
   apiKey = "${MINEWRIGHT_API_KEY}"
   ```

3. Set the environment variable before launching Minecraft:

   **Windows PowerShell:**
   ```powershell
   $env:MINEWRIGHT_API_KEY="your-actual-api-key-here"
   ```

   **Windows Command Prompt:**
   ```cmd
   set MINEWRIGHT_API_KEY=your-actual-api-key-here
   ```

   **Linux/macOS:**
   ```bash
   export MINEWRIGHT_API_KEY="your-actual-api-key-here"
   ```

4. Launch Minecraft from the same terminal/session

#### Option B: Direct Entry (Less Secure)

1. Open `config/minewright-common.toml`
2. Find the `[openai]` section
3. Replace the placeholder with your actual key:
   ```toml
   [openai]
   apiKey = "your-actual-api-key-here"
   model = "glm-5"
   ```

**WARNING:** Never share your config file or commit it to version control with a real API key!

### Step 3: Choose Your AI Provider

Edit `config/minewright-common.toml`:

```toml
[ai]
provider = "openai"  # For z.ai GLM (recommended)
# provider = "groq"   # For Groq (fastest)
# provider = "gemini" # For Gemini
```

### Step 4: Verify Installation

1. Launch Minecraft with Forge
2. Check the logs (`logs/latest.log`) for:
   ```
   MineWright: Loaded successfully
   API key configured: sk-***... (or similar)
   ```
3. If you see errors, see the [Troubleshooting](#troubleshooting) section below

---

## Basic Commands

### Spawning Your First Foreman

Once in-game, open the chat window and type:

```
/foreman spawn Mace
```

This spawns your first AI foreman named "Mace" at your location.

**Other Commands:**

| Command | Description |
|---------|-------------|
| `/foreman spawn <name>` | Spawn a new crew member |
| `/foreman list` | List all active crew members |
| `/foreman order <name> <command>` | Give a command to a specific crew member |
| `/foreman remove <name>` | Remove a crew member |
| Press **K** | Open the command GUI (easier than typing) |

---

## Quick Tutorial

### Task 1: Say Hello

1. Spawn your foreman:
   ```
   /foreman spawn Mace
   ```

2. Give a simple command:
   ```
   /foreman order Mace Say hello
   ```

3. Mace should respond in chat!

### Task 2: Gather Resources

1. Make sure you're in an area with trees nearby:
   ```
   /foreman order Mace Gather 10 oak logs
   ```

2. Watch Mace plan and execute the task!

### Task 3: Build Something Simple

1. Clear some space:
   ```
   /foreman order Mace Clear a 5x5 area here
   ```

2. Build a small structure:
   ```
   /foreman order Mace Build a 3x3 cobblestone platform
   ```

### Tips for Better Results

- **Be specific** - "Build a house" is vague. "Build a 5x5 cobblestone house with a door" is clear.
- **Check materials** - Make sure you have the required blocks in your inventory or nearby
- **Start simple** - Begin with gathering and building before trying complex tasks
- **Be patient** - The AI needs time to think and plan (especially on first use)

---

## Troubleshooting

### "API Key Not Configured" Error

**Problem:** The mod can't find your API key.

**Solutions:**
1. Check that `config/minewright-common.toml` exists
2. Verify the API key is set correctly in the config
3. If using environment variables, make sure they're set before launching Minecraft
4. Try the direct entry method (Option B above) to test

### Agent Stuck or Not Moving

**Problem:** Your foreman spawns but doesn't move or execute tasks.

**Solutions:**
1. Make sure the agent spawned in a valid location (not inside blocks)
2. Try spawning in an open, flat area
3. Check `logs/latest.log` for pathfinding errors
4. Increase `actionTickDelay` in config if server is lagging

### Commands Hang Without Response

**Problem:** You give a command but nothing happens.

**Solutions:**
1. Check your internet connection
2. Verify your API key is valid
3. Check if you've hit your API quota
4. Try switching to a faster provider (Groq is fastest)
5. Check `logs/latest.log` for API errors

### Out of Memory Crash

**Problem:** Minecraft crashes with OutOfMemoryError.

**Solutions:**
1. Reduce `maxActiveCrewMembers` in config (try 3-5 instead of 10)
2. Increase Minecraft's memory allocation:
   - Open Minecraft Launcher
   - Go to Installations → Forge 1.20.1 → More Options
   - Change JVM Arguments to include `-Xmx4G` (for 4GB)
3. Close other applications to free memory

### Mod Won't Load

**Problem:** Minecraft crashes on startup with MineWright errors.

**Solutions:**
1. Verify you're using **Minecraft 1.20.1** with **Forge 47.x**
2. Check for mod conflicts (try with only MineWright installed)
3. Ensure Java 17+ is installed
4. Check the crash report for specific error messages
5. Make sure you're using the `-all` JAR file, not the regular one

---

## Getting Help

### Check the Logs

When something goes wrong, the first step is checking the log file:

**Location:** `logs/latest.log` in your Minecraft directory

**What to look for:**
- API errors (authentication, quota, timeout)
- Pathfinding failures
- Configuration issues
- Missing dependencies

### Documentation

- **[Full Configuration Guide](CONFIGURATION.md)** - All config options explained
- **[Troubleshooting Guide](TROUBLESHOOTING.md)** - Detailed problem-solving
- **[Performance Guide](docs/PERFORMANCE_GUIDE.md)** - Optimization tips
- **[Architecture Overview](ARCHITECTURE_OVERVIEW.md)** - How it works

### Community

- **[GitHub Issues](https://github.com/SuperInstance/MineWright/issues)** - Bug reports and feature requests
- **[GitHub Discussions](https://github.com/SuperInstance/MineWright/discussions)** - Ask questions, share ideas

---

## What's Next?

Now that you have your crew running, try these:

### Advanced Commands

```
/foreman order Mace Build a watchtower 10 blocks high
/foreman order Mace Create a wheat farm nearby
/foreman order Mace Follow me and kill hostile mobs
```

### Multiple Crew Members

```
/foreman spawn Dusty
/foreman order Dusty Mine 64 iron ore
/foreman order Mace Build a storage room
```

### Press K for the Command GUI

The GUI provides:
- Command history
- Auto-completion
- Easier command entry
- Visual feedback

---

## Performance Tips

### For Low-End PCs (4GB RAM or less)

Edit `config/minewright-common.toml`:

```toml
[behavior]
maxActiveCrewMembers = 3
actionTickDelay = 40

[performance]
aiTickBudgetMs = 3
strictBudgetEnforcement = true
```

### For High-End PCs (16GB+ RAM)

```toml
[behavior]
maxActiveCrewMembers = 10
actionTickDelay = 10

[performance]
aiTickBudgetMs = 10
strictBudgetEnforcement = false
```

---

## Security Best Practices

1. **Never share your API keys** - Keep them secret
2. **Use environment variables** - Don't hardcode keys in config files
3. **Don't commit config files** - Add `config/minewright-common.toml` to `.gitignore`
4. **Monitor API usage** - Check your provider dashboard for unusual activity
5. **Use read-only keys** - Some providers support keys with limited permissions

---

## FAQ

**Q: Is this mod free?**

A: Yes, the mod is free and open-source. However, you'll need an API key from an AI provider, which may have costs (though several have free tiers).

**Q: Which AI provider should I use?**

A: Start with **z.ai GLM** - it's recommended, fast, and has a free tier. **Groq** is the fastest if you want quick responses.

**Q: Can I play multiplayer with this mod?**

A: The mod is currently designed for single-player. Multiplayer support is planned for the future.

**Q: How much does it cost to run?**

A: With z.ai GLM or Groq free tiers, it can be free for casual use. Heavy usage may incur costs depending on your provider.

**Q: Can the crew members grief my world?**

A: Crew members follow your commands. They won't intentionally grief, but complex commands in crowded areas may have unintended results. Always save before giving complex commands!

**Q: How do I update the mod?**

A: Download the latest JAR from [Releases](https://github.com/SuperInstance/MineWright/releases) and replace the old JAR in your `mods` folder. Your config and crew data will be preserved.

---

**Enjoy your AI crew! Remember: They don't take orders. They take contracts.**

[← Back to Main README](../README.md) | [Configuration Guide →](CONFIGURATION.md) | [Troubleshooting →](TROUBLESHOOTING.md)
