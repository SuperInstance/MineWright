# MineWright Installation Guide

**Last Updated:** 2026-03-01
**Version:** 1.0

This guide will walk you through installing MineWright, an AI-powered mod for Minecraft that adds autonomous AI companions to your game.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Installation Methods](#installation-methods)
3. [First-Time Setup](#first-time-setup)
4. [Configuration](#configuration)
5. [Testing Your Installation](#testing-your-installation)
6. [Uninstallation](#uninstallation)
7. [Upgrading](#upgrading)

---

## Prerequisites

### Required Software

| Software | Minimum Version | Recommended Version | Notes |
|----------|----------------|---------------------|-------|
| **Minecraft** | 1.20.1 | 1.20.1 | Exact version required |
| **Forge** | 47.2.0 | 47.2.x | Must match Minecraft version |
| **Java** | 17 | 21 | Java 21 recommended for better performance |
| **RAM** | 4 GB | 8 GB+ | More RAM allows more AI agents |
| **API Key** | Any provider | z.ai GLM or Groq | Free options available |

### Operating System Support

- **Windows:** 10/11 (64-bit)
- **macOS:** 10.15 (Catalina) or later
- **Linux:** Most modern distributions

### Account Requirements

- **Minecraft Java Edition** account
- **API key** from one of:
  - [z.ai](https://console.z.ai/) (GLM-5, free tier available)
  - [Groq](https://groq.com/) (Llama 3, free)
  - [OpenAI](https://openai.com/) (GPT-4, paid)
  - [Google AI](https://ai.google.dev/) (Gemini Pro, free tier)

---

## Installation Methods

### Method 1: Pre-built JAR (Recommended for Players)

This is the easiest method for most users.

#### Step 1: Install Minecraft Forge

1. **Download Forge Installer:**
   - Visit [files.minecraftforge.net](https://files.minecraftforge.net/)
   - Select Minecraft version: **1.20.1**
   - Download the **Installer** version (not Universal)

2. **Run the Forge Installer:**
   - Windows: Double-click the downloaded `.jar` file
   - Mac/Linux: Run `java -jar forge-1.20.1-47.x.x-installer.jar` in terminal
   - Select **Install Client** and click OK

3. **Verify Installation:**
   - Open Minecraft Launcher
   - Click the arrow next to the "Play" button
   - You should see "Forge 1.20.1" in the versions list

#### Step 2: Download MineWright

1. **Download from GitHub:**
   - Go to [Releases](https://github.com/SuperInstance/MineWright/releases)
   - Download the latest `minewright-1.0.0.jar` file

2. **Locate Your Mods Folder:**

   **Windows:**
   ```
   %appdata%\.minecraft\mods\
   ```

   **Mac:**
   ```
   ~/Library/Application Support/minecraft/mods/
   ```

   **Linux:**
   ```
   ~/.minecraft/mods/
   ```

   **Pro Tip:** Press Win+R, type `%appdata%\.minecraft\mods`, and press Enter (Windows)

3. **Install the Mod:**
   - Copy the `minewright-1.0.0.jar` file to your `mods` folder
   - If the `mods` folder doesn't exist, create it

#### Step 3: Launch Minecraft

1. **Select Forge Profile:**
   - Open Minecraft Launcher
   - Click the arrow next to "Play"
   - Select "Forge 1.20.1"

2. **Launch the Game:**
   - Click "Play"
   - Wait for the main menu to load
   - MineWright will generate configuration files on first launch

### Method 2: Build from Source (For Developers)

This method is for developers who want to modify the code or contribute to the project.

#### Step 1: Install Development Tools

1. **Install Java 17+:**
   - Download from [Adoptium](https://adoptium.net/) (recommended)
   - Or use your system package manager

2. **Verify Java Installation:**
   ```bash
   java -version
   # Should show: openjdk version "17.x.x" or higher
   ```

3. **Install Git:**
   - Windows: [git-scm.com](https://git-scm.com/download/win)
   - Mac: Included with Xcode Command Line Tools
   - Linux: `sudo apt-get install git` (Ubuntu/Debian)

#### Step 2: Clone and Build

```bash
# Clone the repository
git clone https://github.com/SuperInstance/MineWright.git
cd MineWright

# Build the mod
./gradlew build

# The JAR will be created at:
# build/libs/minewright-1.0.0.jar
```

**Windows users:** Use `gradlew.bat` instead of `./gradlew`

#### Step 3: Install Your Build

1. Copy `build/libs/minewright-1.0.0.jar` to your Minecraft `mods` folder
2. Launch Minecraft with the Forge profile

---

## First-Time Setup

### Initial Configuration

When you first launch Minecraft with MineWright, it will create configuration files:

```
config/minewright-common.toml
```

### Setting Your API Key

**SECURITY BEST PRACTICE:** Use environment variables to avoid committing API keys to version control.

#### Option 1: Environment Variable (Recommended)

1. **Edit Configuration:**
   ```toml
   # In config/minewright-common.toml
   [openai]
   apiKey = "${MINEWRIGHT_API_KEY}"
   ```

2. **Set Environment Variable:**

   **Linux/macOS (bash/zsh):**
   ```bash
   export MINEWRIGHT_API_KEY="your-api-key-here"
   ```

   Add to `~/.bashrc` or `~/.zshrc` to make it permanent:
   ```bash
   echo 'export MINEWRIGHT_API_KEY="your-api-key-here"' >> ~/.bashrc
   source ~/.bashrc
   ```

   **Windows PowerShell:**
   ```powershell
   $env:MINEWRIGHT_API_KEY="your-api-key-here"
   ```

   To make it permanent, set in System Environment Variables:
   - Press Win+R, type `sysdm.cpl`
   - Go to Advanced → Environment Variables
   - Add new system variable: `MINEWRIGHT_API_KEY`

   **Windows Command Prompt:**
   ```cmd
   set MINEWRIGHT_API_KEY=your-api-key-here
   ```

3. **Launch Minecraft** from the same terminal/session where you set the variable

#### Option 2: Direct Configuration (Less Secure)

Edit `config/minewright-common.toml`:
```toml
[openai]
apiKey = "your-api-key-here"
```

**WARNING:** Never commit this file to version control with a real API key!

### Choosing an AI Provider

Edit `config/minewright-common.toml`:

```toml
[ai]
provider = "openai"  # Options: openai, groq, gemini
```

**Recommendations:**
- **Best Overall:** z.ai GLM (provider: `openai`, model: `glm-5`)
- **Fastest Free:** Groq (provider: `groq`)
- **Most Capable:** OpenAI GPT-4 (provider: `openai`, model: `gpt-4`)
- **Alternative:** Google Gemini (provider: `gemini`)

---

## Configuration

### Basic Configuration

After first launch, edit `config/minewright-common.toml`:

```toml
[ai]
provider = "openai"  # AI provider selection

[openai]
apiKey = "${MINEWRIGHT_API_KEY}"  # Your API key
model = "glm-5"                    # Model to use
maxTokens = 8000                   # Max tokens per request
temperature = 0.7                  # Response randomness (0.0-2.0)

[behavior]
actionTickDelay = 20               # Ticks between actions (20 = 1 second)
enableChatResponses = true         # Allow agents to chat
maxActiveCrewMembers = 10          # Maximum concurrent agents
```

### Advanced Configuration

See [docs/CONFIGURATION.md](CONFIGURATION.md) for complete configuration options including:
- Performance tuning
- Voice system setup
- Multi-agent coordination
- Pathfinding options
- Debug logging
- Skill library settings
- Cascade router configuration

---

## Testing Your Installation

### Step 1: Verify Mod Loading

1. **Launch Minecraft** with Forge profile
2. **Check the Main Menu** - MineWright should be loaded
3. **Check Logs:**
   - Open `logs/latest.log` in your Minecraft directory
   - Search for "MineWright"
   - You should see: `MineWright: Loaded successfully`

### Step 2: Verify Configuration

1. **Check logs for API key:**
   - Search for "API key configured"
   - Should show: `API key configured: sk-***...` (preview, not full key)
2. **If you see errors:**
   - "API key not configured" - Check your config file
   - "Invalid API key" - Verify your key is correct
   - "Connection timeout" - Check your internet connection

### Step 3: Spawn Your First Agent

1. **Create a new world** or load an existing one
2. **Open chat** (press T or /)
3. **Spawn an agent:**
   ```
   /minewright spawn Steve
   ```
4. **Verify spawn:**
   - You should see an AI agent appear nearby
   - The agent should send a greeting message

### Step 4: Test Basic Command

1. **Open the command panel** (press K)
2. **Type a simple command:**
   ```
   say hello
   ```
3. **Verify response:**
   - The agent should respond in chat
   - If no response, check logs for errors

### Step 5: Test Complex Command

1. **Issue a building command:**
   ```
   build a small stone house nearby
   ```
2. **Watch the agent:**
   - Agent should acknowledge the command
   - Agent should begin planning (may take 10-30 seconds)
   - Agent should start executing tasks

### Common Setup Issues

| Issue | Solution |
|-------|----------|
| Mod doesn't load | Verify Forge 1.20.1 is installed |
| "API key not configured" | Check config file and environment variables |
| Agent doesn't respond | Check logs for LLM errors, verify API key |
| Out of memory | Increase JVM heap: `-Xmx4G` |
| Agent stuck | Ensure agent is in valid location (not in blocks) |

---

## Uninstallation

### Remove MineWright

1. **Close Minecraft** completely
2. **Navigate to your mods folder:**
   - Windows: `%appdata%\.minecraft\mods\`
   - Mac: `~/Library/Application Support/minecraft/mods/`
   - Linux: `~/.minecraft/mods/`
3. **Delete the mod file:**
   - Remove `minewright-1.0.0.jar`
4. **Optional: Remove configuration:**
   - Delete `config/minewright-common.toml`
   - Delete `config/minewright-common.toml.example` (if present)

### Note on Saved Worlds

- MineWright data is stored in each world's save file
- Removing the mod will remove MineWright entities from your worlds
- Existing worlds will load normally, but AI agents will be gone
- To restore agents, reinstall the mod and load the world again

---

## Upgrading

### Upgrade from Previous Version

1. **Backup your world saves** (optional but recommended)
2. **Download the new version** from [Releases](https://github.com/SuperInstance/MineWright/releases)
3. **Delete the old JAR** from your mods folder
4. **Install the new JAR** in your mods folder
5. **Launch Minecraft**

### Configuration Migration

- Configuration files are preserved between versions
- New configuration options will use default values
- Review `config/minewright-common.toml.example` for new options

### Breaking Changes

Major version updates may include breaking changes. Check the release notes for:
- Configuration changes
- Command syntax changes
- Save file compatibility

---

## Next Steps

Now that you have MineWright installed:

1. **Read the Configuration Guide:** [docs/CONFIGURATION.md](CONFIGURATION.md)
2. **Learn In-Game Commands:** See [README.md](../README.md#in-game-commands)
3. **Optimize Performance:** See [docs/PERFORMANCE.md](PERFORMANCE.md)
4. **Get Help:** See [docs/TROUBLESHOOTING.md](TROUBLESHOOTING.md)

---

## Getting Help

### Documentation

- [Main README](../README.md)
- [Configuration Guide](CONFIGURATION.md)
- [Troubleshooting](TROUBLESHOOTING.md)
- [Performance Guide](PERFORMANCE.md)

### Community

- [GitHub Issues](https://github.com/SuperInstance/MineWright/issues)
- [GitHub Discussions](https://github.com/SuperInstance/MineWright/discussions)

### Reporting Issues

When reporting installation issues, include:
- Minecraft version
- Forge version
- MineWright version
- Operating system
- Java version (`java -version`)
- Relevant logs from `logs/latest.log`
- Screenshots if applicable

---

**Document Version:** 1.0
**Last Updated:** 2026-03-01
**Maintained By:** MineWright Development Team
