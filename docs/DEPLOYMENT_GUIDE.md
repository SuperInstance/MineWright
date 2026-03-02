# MineWright Deployment Guide

**Version:** 1.0.0
**Last Updated:** 2026-03-02
**Minecraft Version:** 1.20.1
**Forge Version:** 47.4.16

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Development Build Process](#development-build-process)
4. [Distribution JAR Creation](#distribution-jar-creation)
5. [Client Installation](#client-installation)
6. [Server Installation](#server-installation)
7. [Configuration](#configuration)
8. [Multiplayer Considerations](#multiplayer-considerations)
9. [Version Compatibility](#version-compatibility)
10. [Troubleshooting](#troubleshooting)
11. [Security Best Practices](#security-best-practices)

---

## Overview

MineWright is a sophisticated AI-powered Minecraft mod that adds autonomous agent companions. This guide covers building, deploying, and configuring the mod for both client and server environments.

### Key Features

- **Natural Language Commands:** Control agents using plain English
- **Multi-Agent Coordination:** Multiple agents work together seamlessly
- **LLM-Powered Planning:** Advanced AI using GLM-5, GPT-4, or other models
- **Real-Time Execution:** 60 FPS performance without blocking
- **Rich Personalities:** 8 unique agent archetypes with distinct behaviors

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     BRAIN LAYER (Strategic)                     │
│                         LLM Agents                              │
│   • Planning, strategy, logistics                              │
│   • Conversations with player and other agents                 │
│   • Creating and refining automation scripts                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Generates & Refines
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SCRIPT LAYER (Operational)                    │
│                    Behavior Automations                         │
│   • Behavior trees, FSMs, macro scripts                        │
│   • Pathfinding, mining, building patterns                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Executes via
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PHYSICAL LAYER (Actions)                      │
│                     Minecraft API                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

### Development Build Requirements

| Requirement | Minimum Version | Recommended Version |
|-------------|-----------------|---------------------|
| **Java JDK** | 17 | Eclipse Temurin 17 LTS |
| **Gradle** | 8.5 | 8.10 (bundled with project) |
| **Git** | 2.30+ | Latest stable |
| **Memory** | 4 GB RAM | 8 GB RAM |
| **Disk Space** | 2 GB free | 5 GB free |

### Runtime Requirements

| Component | Requirement |
|-----------|-------------|
| **Minecraft** | 1.20.1 |
| **Forge** | 47.4.16 (or any 47.x) |
| **Java** | 17+ |
| **RAM** | 4 GB minimum, 6 GB recommended |
| **API Access** | OpenAI/z.ai, Groq, or Gemini API key |

### Installing Java 17

**Windows:**
```powershell
# Using Winget
winget install EclipseAdoptium.Temurin.17.JDK

# Or download from: https://adoptium.net/temurin/releases/?version=17
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# Verify installation
java -version
```

**macOS:**
```bash
# Using Homebrew
brew install openjdk@17

# Set JAVA_HOME
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
```

---

## Development Build Process

### 1. Clone Repository

```bash
# Clone the repository
git clone https://github.com/SuperInstance/MineWright.git
cd MineWright

# Verify Java version
java -version  # Should show 17.x.x
```

### 2. Configure Gradle

The project uses Gradle 8.10 (configured in `gradle/wrapper/gradle-wrapper.properties`).

```bash
# Verify Gradle version
./gradlew --version

# Expected output:
# Gradle 8.10
# Kotlin: 1.9.0
# Groovy: 4.0.15
# Ant: 1.10.14
# JVM: 17.x.x
```

### 3. Development Build Commands

**Standard Development Build:**

```bash
# Clean and build
./gradlew clean build

# Build without running tests
./gradlew build -x test

# Build with specific OS (Windows example)
./gradlew build -Dos=windows
```

**Run Minecraft Client (Development):**

```bash
# Run development client
./gradlew runClient

# Run with specific workspace directory
./gradlew runClient --working-dir=./dev-client
```

**Run Minecraft Server (Development):**

```bash
# Run development server
./gradlew runServer

# Run with specific workspace directory
./gradlew runServer --working-dir=./dev-server
```

### 4. Development Build Outputs

Development builds create the following artifacts:

```
build/
├── libs/
│   ├── minewright-1.0.0.jar           # Development JAR (not obfuscated)
│   └── minewright-1.0.0-sources.jar   # Source JAR
├── reports/
│   ├── tests/                          # Test reports
│   ├── jacoco/                         # Code coverage reports
│   └── spotbugs/                       # Bug analysis reports
└── tmp/
    └── compiled/
        ├── mainClasses/                # Compiled classes
        └── testClasses/                # Test classes
```

**Important:** Development JARs are **not obfuscated** and should only be used for testing, not distribution.

### 5. Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests InputSanitizerTest

# Run tests with coverage report
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html  # macOS
xdg-open build/reports/jacoco/test/html/index.html  # Linux
start build/reports/jacoco/test/html/index.html  # Windows
```

---

## Distribution JAR Creation

### Overview

Distribution JARs include all dependencies and are obfuscated for production use. This is the format you distribute to users.

### Build Commands

**Step 1: Clean Previous Builds**

```bash
./gradlew clean
```

**Step 2: Build Distribution JAR**

```bash
# Build shadow JAR with all dependencies
./gradlew shadowJar

# Obfuscate the shadow JAR (required for distribution)
./gradlew reobfShadowJar

# Or combine both commands
./gradlew shadowJar reobfShadowJar
```

**Step 3: Verify Build Artifacts**

```bash
# List output files
ls -lh build/libs/

# Expected output:
# minewright-1.0.0.jar              # Regular JAR (not for distribution)
# minewright-1.0.0-all.jar          # Shadow JAR (not obfuscated)
# minewright-1.0.0-all-obfuscated.jar  # DISTRIBUTION JAR (use this!)
```

### Distribution JAR Details

**File:** `build/libs/minewright-1.0.0-all-obfuscated.jar`

**Characteristics:**
- Includes all dependencies (GraalVM, Resilience4j, Caffeine, etc.)
- Obfuscated (Minecraft mappings applied)
- Ready for deployment to client or server
- Self-contained (no external dependency JARs needed)

**File Size:** Approximately 15-25 MB (depending on included dependencies)

### Dependency Relocation

The shadow JAR relocates dependencies to avoid conflicts:

| Original Package | Relocated Package |
|-----------------|-------------------|
| `org.graalvm` | `com.minewright.shaded.org.graalvm` |
| `com.oracle.truffle` | `com.minewright.shaded.com.oracle.truffle` |

### Creating Release Packages

**Package Structure:**

```bash
# Create release directory
mkdir -p release/minewright-1.0.0

# Copy distribution JAR
cp build/libs/minewright-1.0.0-all-obfuscated.jar release/minewright-1.0.0/minewright-1.0.0.jar

# Copy configuration template
cp config/minewright-common.toml.example release/minewright-1.0.0/

# Create README
cat > release/minewright-1.0.0/README.txt << 'EOF'
MineWright v1.0.0 - Installation Instructions
=============================================

1. Install Minecraft Forge 1.20.1-47.4.16
2. Copy minewright-1.0.0.jar to the mods folder
3. Launch Minecraft
4. Configure API key in config/minewright-common.toml

For detailed instructions, see: https://github.com/SuperInstance/MineWright
EOF

# Create ZIP archive
cd release
zip -r minewright-1.0.0.zip minewright-1.0.0/
```

---

## Client Installation

### Installation Methods

#### Method 1: Manual Installation (Recommended)

1. **Install Forge**
   - Download Forge Installer 1.20.1-47.4.16 from https://files.minecraftforge.net/
   - Run the installer
   - Select "Install Client"
   - Launch Minecraft once to verify Forge installation

2. **Install MineWright**
   - Navigate to your Minecraft directory:
     - **Windows:** `%APPDATA%\.minecraft`
     - **Linux:** `~/.minecraft`
     - **macOS:** `~/Library/Application Support/minecraft`
   - Create the `mods` folder if it doesn't exist
   - Copy `minewright-1.0.0-all-obfuscated.jar` to `mods/`

3. **Configure API Key**
   - Launch Minecraft with Forge profile
   - Close Minecraft after it reaches the main menu
   - Navigate to `config/` folder in your Minecraft directory
   - Copy `minewright-common.toml.example` to `minewright-common.toml`
   - Edit `minewright-common.toml` and add your API key:
   ```toml
   [openai]
   apiKey = "${MINEWRIGHT_API_KEY}"  # Or paste your key directly
   ```

4. **Launch and Play**
   - Launch Minecraft with Forge profile
   - Join a world (singleplayer or multiplayer)
   - Use `/foreman spawn <name>` to create an agent

#### Method 2: CurseForge/Twitch (Future)

Once published to CurseForge:

1. Install the CurseForge app
2. Search for "MineWright"
3. Click "Install"
4. Launch Minecraft with the CurseForge profile

### Verifying Installation

**In-Game Check:**

1. Launch Minecraft with Forge
2. Open the Mods menu (Options → Mods)
3. Verify "MineWright" appears in the mod list
4. Check version: 1.0.0

**Log File Check:**

```
Location: logs/latest.log
Search for: "MineWright"

Expected output:
[main/INFO]: Loading mod MineWright v1.0.0
[main/INFO]: MineWright: Configuration loaded successfully
```

---

## Server Installation

### Server Prerequisites

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **CPU** | 2 cores | 4+ cores |
| **RAM** | 4 GB | 6-8 GB |
| **OS** | Windows 10+, Ubuntu 20.04+ | Any modern OS |
| **Java** | 17 | Eclipse Temurin 17 LTS |
| **Network** | 10 Mbps upload | 100 Mbps upload |

### Installation Steps

#### Step 1: Install Forge Server

1. **Download Forge Installer**
   ```bash
   wget https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.16/forge-1.20.1-47.4.16-installer.jar
   ```

2. **Install Server**
   ```bash
   java -jar forge-1.20.1-47.4.16-installer.jar --installServer
   ```

3. **Accept EULA**
   ```bash
   # Edit eula.txt
   echo "eula=true" > eula.txt
   ```

#### Step 2: Install MineWright

```bash
# Create mods directory
mkdir -p mods

# Copy distribution JAR
cp minewright-1.0.0-all-obfuscated.jar mods/

# Verify installation
ls -lh mods/
```

#### Step 3: Configure Server Properties

**server.properties:**

```properties
# Server Configuration
server-name=MineWright Server
server-port=25565
max-players=20
motd=Welcome to MineWright AI Server

# Performance Settings
view-distance=10
simulation-distance=6

# AI Agent Settings
max-world-size=29999984
enable-command-block=true
spawn-protection=0

# PvP and Difficulty
pvp=false
difficulty=normal
hardcore=false
```

#### Step 4: Configure MineWright

**config/minewright-common.toml:**

```toml
[ai]
provider = "openai"

[openai]
# Use environment variable for security
apiKey = "${MINEWRIGHT_API_KEY}"
model = "glm-5"
maxTokens = 8000
temperature = 0.7

[behavior]
actionTickDelay = 20
enableChatResponses = true
maxActiveCrewMembers = 10

[multi_agent]
enabled = true
max_bid_wait_ms = 1000
blackboard_ttl_seconds = 300

[performance]
aiTickBudgetMs = 5
budgetWarningThreshold = 80
strictBudgetEnforcement = true
```

#### Step 5: Set Environment Variables (Recommended)

**Linux/macOS:**

```bash
# Set environment variable before starting server
export MINEWRIGHT_API_KEY="your-api-key-here"

# Or add to startup script
cat > start-server.sh << 'EOF'
#!/bin/bash
export MINEWRIGHT_API_KEY="your-api-key-here"
java -Xmx6G -Xms4G -jar forge-1.20.1-47.4.16-universal.jar nogui
EOF

chmod +x start-server.sh
```

**Windows:**

```powershell
# Set environment variable
set MINEWRIGHT_API_KEY=your-api-key-here

# Or add to startup script
cat > start-server.bat << 'EOF'
@echo off
set MINEWRIGHT_API_KEY=your-api-key-here
java -Xmx6G -Xms4G -jar forge-1.20.1-47.4.16-universal.jar nogui
EOF
```

#### Step 6: Launch Server

**Linux/macOS:**

```bash
# Start server with 6GB RAM
java -Xmx6G -Xms4G -jar forge-1.20.1-47.4.16-universal.jar nogui

# Or use the startup script
./start-server.sh
```

**Windows:**

```powershell
# Start server with 6GB RAM
java -Xmx6G -Xms4G -jar forge-1.20.1-47.4.16-universal.jar nogui

# Or use the startup script
start-server.bat
```

### Server Performance Tuning

**Java VM Options:**

```bash
# Recommended JVM flags for server
java -Xmx6G \                    # Maximum heap size (6GB)
     -Xms4G \                    # Initial heap size (4GB)
     -XX:+UseG1GC \              # Use G1 garbage collector
     -XX:+ParallelRefProcEnabled \
     -XX:MaxGCPauseMillis=200 \  # Target GC pause time
     -XX:+UnlockExperimentalVMOptions \
     -XX:+DisableExplicitGC \    # Disable explicit GC calls
     -XX:+AlwaysPreTouch \       # Pre-touch memory pages
     -XX:G1NewSizePercent=30 \   # G1 new generation size
     -XX:G1MaxNewSizePercent=40 \
     -XX:G1HeapRegionSize=8M \
     -XX:G1ReservePercent=20 \
     -XX:G1HeapWastePercent=5 \
     -XX:G1MixedGCCountTarget=4 \
     -XX:InitiatingHeapOccupancyPercent=15 \
     -XX:G1MixedGCLiveThresholdPercent=90 \
     -XX:G1RSetUpdatingPauseTimePercent=5 \
     -XX:SurvivorRatio=32 \
     -XX:+PerfDisableSharedMem \
     -XX:MaxTenuringThreshold=1 \
     -Dusing.aikars.flags=https://mcflags.emc.gs \
     -Daikars.new.flags=true \
     -jar forge-1.20.1-47.4.16-universal.jar nogui
```

---

## Configuration

### Configuration File Location

The configuration file is automatically generated on first launch:

**Location:** `config/minewright-common.toml`

**Platform-Specific Paths:**
- **Windows:** `%APPDATA%\.minecraft\config\minewright-common.toml`
- **Linux:** `~/.minecraft/config/minewright-common.toml`
- **macOS:** `~/Library/Application Support/minecraft/config/minewright-common.toml`

### Configuration Template

A template configuration is provided at `config/minewright-common.toml.example` in the repository.

### Key Configuration Sections

#### 1. AI Provider Configuration

```toml
[ai]
# Options: 'openai' (z.ai GLM-5), 'groq', 'gemini'
provider = "openai"
```

#### 2. API Configuration

```toml
[openai]
# SECURE: Use environment variable
apiKey = "${MINEWRIGHT_API_KEY}"

# Model selection
model = "glm-5"  # Options: 'glm-5', 'glm-4-flash', 'gpt-4'

# Request parameters
maxTokens = 8000
temperature = 0.7
```

#### 3. Behavior Configuration

```toml
[behavior]
# Ticks between action checks (20 ticks = 1 second)
actionTickDelay = 20

# Allow crew to respond in chat
enableChatResponses = true

# Maximum active agents
maxActiveCrewMembers = 10
```

#### 4. Performance Configuration

```toml
[performance]
# AI tick budget in milliseconds
aiTickBudgetMs = 5

# Warning threshold (percentage)
budgetWarningThreshold = 80

# Strict budget enforcement
strictBudgetEnforcement = true
```

#### 5. Multi-Agent Configuration

```toml
[multi_agent]
# Enable coordination features
enabled = true

# Maximum bid wait time (milliseconds)
max_bid_wait_ms = 1000

# Blackboard TTL (seconds)
blackboard_ttl_seconds = 300
```

### Reloading Configuration

Edit the config file and use the in-game command:

```
/reload
```

This reloads the configuration without restarting the game.

---

## Multiplayer Considerations

### Server-Side vs Client-Side

MineWright is designed to run **server-side**, but also supports **client-side singleplayer**.

| Feature | Server-Side | Client-Side |
|---------|-------------|-------------|
| **Agent Spawning** | Yes | Yes |
| **AI Planning** | Yes | Yes |
| **Multi-Agent Coordination** | Yes | Yes |
| **Voice Integration** | Server controls | Client controls |
| **Configuration** | Server config | Client config |

### Permission Requirements

**Required Permissions for Players:**

1. **Command Access:**
   - `/foreman spawn` - Requires OP level 2
   - `/foreman list` - Requires OP level 2
   - `/foreman remove` - Requires OP level 2
   - `/foreman order` - Requires OP level 2

2. **Granting Permissions:**
   ```
   /op <playername>
   ```

### Network Configuration

**Firewall Settings:**

```bash
# Default Minecraft port
PORT=25565

# Allow through firewall (Linux)
sudo ufw allow 25565/tcp
sudo ufw reload

# Allow through firewall (Windows PowerShell)
New-NetFirewallRule -DisplayName "Minecraft Server" -Direction Inbound -LocalPort 25565 -Protocol TCP -Action Allow
```

**Port Forwarding (if hosting from home):**

1. Access router admin panel
2. Forward port 25565 TCP to server IP
3. Test with: https://canyouseeme.org/

### Player Limitations

**Non-OP Players:**

- Cannot spawn or control agents
- Can see and interact with agents spawned by OPs
- Can chat with agents if `enableChatResponses = true`

**Recommended Setup:**

```toml
[behavior]
# Limit agents per OP player
maxActiveCrewMembers = 5

# Enable public chat with agents
enableChatResponses = true
```

### Server Resource Management

**Agent Allocation Strategy:**

```toml
[multi_agent]
# Maximum concurrent agents across all players
max_global_agents = 50

# Maximum agents per player
max_agents_per_player = 5

# Agent cleanup timeout (seconds)
agent_inactive_timeout = 300
```

---

## Version Compatibility

### Minecraft Versions

| MineWright Version | Minecraft Version | Forge Version | Status |
|-------------------|-------------------|---------------|--------|
| 1.0.0 | 1.20.1 | 47.4.16 | Stable |

**Forge Version Compatibility:**

- Minimum: 47.0.0
- Recommended: 47.4.16
- Maximum: 47.x.x (any 47.x should work)

### Java Version Compatibility

| Java Version | Status | Notes |
|--------------|--------|-------|
| 16 | Not Supported | Requires Java 17 features |
| 17 | Supported | Minimum version |
| 18 | Supported | Tested |
| 19 | Supported | Tested |
| 20 | Supported | Tested |
| 21 | Supported | Tested |

### Dependency Versions

| Dependency | Version | Purpose |
|------------|---------|---------|
| GraalVM Polyglot | 24.1.2 | Script execution |
| GraalVM JS | 24.1.2 | JavaScript engine |
| Resilience4j | 2.3.0 | Retry, circuit breaker |
| Caffeine | 3.1.8 | Caching |
| Apache Commons Codec | 1.17.1 | Encoding utilities |
| JUnit Jupiter | 5.11.4 | Testing |
| Mockito | 5.15.2 | Mocking framework |

### Mod Compatibility

**Known Compatible Mods:**

- Optifine (install after Forge)
- JourneyMap
- minimap (with Forge edition)
- Inventory Tweaks Renewed
- Just Enough Items (JEI)
- Better Advancements

**Known Conflicts:**

- Other agent mods (e.g., CustomNPCs) - May cause entity ID conflicts
- Profiler mods - May interfere with tick profiling

---

## Troubleshooting

### Build Issues

#### Issue: Gradle Build Fails

**Symptoms:**
```
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':compileJava'.
```

**Solutions:**

1. **Check Java Version:**
   ```bash
   java -version  # Must be 17.x.x
   ```

2. **Clean Build:**
   ```bash
   ./gradlew clean build --refresh-dependencies
   ```

3. **Increase Gradle Memory:**
   ```bash
   # Edit gradle.properties
   org.gradle.jvmargs=-Xmx4G -XX:MaxMetaspaceSize=512m
   ```

#### Issue: Shadow JAR Build Fails

**Symptoms:**
```
Execution failed for task ':shadowJar'.
> Failed to relocate dependency
```

**Solutions:**

1. **Clean and Rebuild:**
   ```bash
   ./gradlew clean shadowJar --rerun-tasks
   ```

2. **Verify Dependency Versions:**
   ```bash
   ./gradlew dependencies --configuration runtimeClasspath
   ```

### Installation Issues

#### Issue: Mod Not Loading

**Symptoms:**
- Mod doesn't appear in mods list
- No MineWright log entries

**Solutions:**

1. **Verify Forge Version:**
   ```
   Must be Forge 1.20.1-47.x.x
   ```

2. **Check File Extension:**
   ```
   Ensure file is named: minewright-1.0.0.jar
   Not: minewright-1.0.0.jar.zip
   ```

3. **Verify File Integrity:**
   ```bash
   # Check JAR is valid
   jar -tf minewright-1.0.0.jar | head -20
   ```

#### Issue: Configuration Not Generated

**Symptoms:**
- No config file after launch
- Config file empty

**Solutions:**

1. **Launch Minecraft Fully:**
   - Reach main menu
   - Join a world
   - Exit game
   - Check config folder again

2. **Manual Configuration:**
   ```bash
   # Copy template
   cp minewright-common.toml.example minewright-common.toml

   # Edit with your API key
   nano minewright-common.toml
   ```

### Runtime Issues

#### Issue: API Key Not Working

**Symptoms:**
```
[WARN]: API key validation failed
[ERROR]: Failed to connect to API endpoint
```

**Solutions:**

1. **Verify API Key Format:**
   ```toml
   # Correct
   apiKey = "${MINEWRIGHT_API_KEY}"

   # Incorrect (missing quotes)
   apiKey = ${MINEWRIGHT_API_KEY}

   # Incorrect (direct value without quotes)
   apiKey = sk-your-key-here
   ```

2. **Set Environment Variable:**
   ```bash
   # Linux/macOS
   export MINEWRIGHT_API_KEY="sk-your-key-here"

   # Windows PowerShell
   $env:MINEWRIGHT_API_KEY="sk-your-key-here"

   # Windows CMD
   set MINEWRIGHT_API_KEY=sk-your-key-here
   ```

3. **Test API Key:**
   ```bash
   # Test with curl
   curl -X POST https://api.z.ai/api/paas/v4/chat/completions \
     -H "Authorization: Bearer YOUR_API_KEY" \
     -H "Content-Type: application/json" \
     -d '{"model":"glm-5","messages":[{"role":"user","content":"test"}]}'
   ```

#### Issue: Agent Not Responding

**Symptoms:**
- Agent spawned but doesn't respond to commands
- Agent stands still

**Solutions:**

1. **Check API Connectivity:**
   ```
   View logs: logs/latest.log
   Search for: "API request" or "LLM response"
   ```

2. **Increase Timeout:**
   ```toml
   [openai]
   # Add or increase timeout
   timeoutMs = 30000  # 30 seconds
   ```

3. **Verify Model Availability:**
   ```toml
   [openai]
   # Try different model
   model = "glm-4-flash"  # Faster model
   ```

#### Issue: Performance Problems

**Symptoms:**
- Server lag when agents active
- Low FPS on client

**Solutions:**

1. **Reduce Agent Count:**
   ```toml
   [behavior]
   maxActiveCrewMembers = 3  # Reduce from 10
   ```

2. **Increase Tick Budget:**
   ```toml
   [performance]
   aiTickBudgetMs = 10  # Increase from 5ms
   ```

3. **Disable Features:**
   ```toml
   [humanization]
   enabled = false  # Disable for performance

   [voice]
   enabled = false  # Disable voice
   ```

4. **Server Performance:**
   ```bash
   # Increase server RAM
   java -Xmx8G -Xms6G -jar forge-1.20.1-47.4.16-universal.jar nogui
   ```

### Server-Specific Issues

#### Issue: Agents Despawn on Server Restart

**Symptoms:**
- Agents disappear after server restart
- Need to respawn agents manually

**Solutions:**

1. **Verify Persistence:**
   ```toml
   # This feature may not be fully implemented
   # Check for persistence settings in future versions
   ```

2. **Auto-Spawn Script:**
   ```bash
   # Create startup script that spawns agents
   # Add to server startup script
   ```

#### Issue: Multi-Agent Coordination Failing

**Symptoms:**
- Agents don't work together
- Tasks not distributed

**Solutions:**

1. **Enable Multi-Agent:**
   ```toml
   [multi_agent]
   enabled = true
   ```

2. **Increase Bid Wait Time:**
   ```toml
   [multi_agent]
   max_bid_wait_ms = 2000  # Increase from 1000ms
   ```

3. **Check Network Latency:**
   ```bash
   # High latency can cause coordination issues
   ping <server-ip>
   ```

---

## Security Best Practices

### API Key Management

**NEVER:**

- Commit API keys to version control
- Share API keys in chat or Discord
- Use production keys for development
- Hardcode API keys in source code

**ALWAYS:**

- Use environment variables for API keys
- Rotate API keys regularly
- Use separate keys for dev/prod
- Monitor API usage and costs

### Environment Variable Configuration

**Linux/macOS:**

```bash
# Add to .bashrc or .zshrc
export MINEWRIGHT_API_KEY="your-api-key-here"

# Source the file
source ~/.bashrc
```

**Windows:**

```powershell
# Set permanent environment variable
[System.Environment]::SetEnvironmentVariable('MINEWRIGHT_API_KEY', 'your-api-key-here', 'User')
```

### Server Security

1. **Firewall:**
   ```bash
   # Only allow necessary ports
   sudo ufw allow 25565/tcp
   sudo ufw enable
   ```

2. **OP Permissions:**
   ```
   # Only OP trusted players
   /op <trusted-player>
   /deop <untrusted-player>
   ```

3. **Plugin Validation:**
   - Only install mods from trusted sources
   - Verify JAR signatures
   - Review mod code if possible

### Input Sanitization

MineWright includes input sanitization to prevent:

- Prompt injection attacks
- Jailbreak attempts
- Code execution exploits
- System prompt extraction

**Security Features:**

- Pattern-based threat detection
- Command validation before processing
- Suspicious pattern logging
- Automatic rejection of malicious input

---

## Additional Resources

### Documentation

- **Architecture Overview:** `docs/ARCHITECTURE_OVERVIEW.md`
- **Project Guide:** `CLAUDE.md`
- **Research Documents:** `docs/research/`
- **API Documentation:** `docs/ACTION_API.md`

### Community

- **GitHub Issues:** https://github.com/SuperInstance/MineWright/issues
- **Discussions:** https://github.com/SuperInstance/MineWright/discussions

### Support

For deployment issues:
1. Check this guide's troubleshooting section
2. Review log files in `logs/latest.log`
3. Search existing GitHub issues
4. Create a new issue with:
   - MineWright version
   - Minecraft/Forge version
   - Error logs
   - Configuration file (without API keys)

---

## Appendix

### Quick Reference Commands

**Development:**
```bash
./gradlew build                 # Build development JAR
./gradlew test                  # Run tests
./gradlew runClient             # Run development client
./gradlew shadowJar reobfShadowJar  # Build distribution JAR
```

**Installation:**
```bash
# Client
cp minewright-1.0.0.jar ~/.minecraft/mods/

# Server
cp minewright-1.0.0.jar /path/to/server/mods/
```

**Configuration:**
```bash
# Copy template
cp minewright-common.toml.example minewright-common.toml

# Edit config
nano minewright-common.toml

# Reload in-game
/reload
```

### File Locations

**Development:**
- Source: `src/main/java/com/minewright/`
- Config Template: `config/minewright-common.toml.example`
- Build Output: `build/libs/`

**Installation:**
- Mods: `mods/minewright-1.0.0.jar`
- Config: `config/minewright-common.toml`
- Logs: `logs/latest.log`

### Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-03-02 | Initial release |

---

**Document Version:** 1.0.0
**Last Updated:** 2026-03-02
**Maintained By:** MineWright Team
**License:** MIT
