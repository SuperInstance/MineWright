# Model Context Protocol (MCP) Integration Guide for MineWright

**Research Document Version:** 1.0
**Date:** 2026-02-27
**Author:** Claude Code Research Agent

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [MCP Protocol Overview](#mcp-protocol-overview)
3. [MCP Architecture](#mcp-architecture)
4. [Available MCP Servers](#available-mcp-servers)
5. [Java Integration Guide](#java-integration-guide)
6. [Integration Architecture for MineWright](#integration-architecture-for-minewright)
7. [Code Examples](#code-examples)
8. [Benefits for AI Agent Tool Use](#benefits-for-ai-agent-tool-use)
9. [Implementation Roadmap](#implementation-roadmap)
10. [References and Resources](#references-and-resources)

---

## Executive Summary

The **Model Context Protocol (MCP)** is an open standard introduced by Anthropic in November 2024 that standardizes how AI applications connect to external data sources, tools, and services. Think of MCP as "USB-C for AI" - a universal connector that allows Large Language Models (LLMs) to seamlessly access and interact with external capabilities.

### Key Benefits for MineWright

- **Standardized Tool Integration**: No more custom API wrappers for each tool
- **Discoverable Capabilities**: LLMs can discover available tools at runtime
- **Language Agnostic**: Connect to tools written in any language (Python, Node.js, Go, etc.)
- **Production Ready**: Battle-tested integration with major platforms (GitHub Copilot, JetBrains, Apple)
- **Rich Ecosystem**: 4,700+ MCP servers already available

### Why MCP Matters for Minecraft AI

MineWright uses LLMs to plan and execute actions in Minecraft. MCP enables:
- **Jupyter Integration**: Run Python code for data analysis, world state calculations, pathfinding
- **File System Access**: Read/write configuration files, logs, world backups
- **Database Queries**: Store and retrieve agent memories, building plans
- **Web Search**: Look up Minecraft wiki information, building tutorials
- **Custom Tools**: Any Java or Python tool can be exposed as an MCP server

---

## MCP Protocol Overview

### What is MCP?

**Model Context Protocol (MCP)** is an open protocol that enables seamless integration between LLM applications and external data sources, tools, and services. It solves the "private protocol glue problem" by providing minimal common conventions that different tools and agents can follow.

### Core Characteristics

| Characteristic | Description |
|----------------|-------------|
| **Based on** | JSON-RPC 2.0 |
| **Developed by** | Anthropic (released November 2024) |
| **License** | MIT (Open Source) |
| **Protocol Versions** | 2024-11-05 (current stable) |
| **Transport Layers** | STDIO, SSE (Server-Sent Events), HTTP |

### Three Key Abstractions

MCP defines three core concepts for exposing capabilities:

#### 1. Tools (Executable Functions)
Tools are functions that the LLM can invoke with parameters. Examples:
- `execute_python_code(code: string)`: Run Python in Jupyter
- `read_file(path: string)`: Read a file's contents
- `search_web(query: string)`: Search the web
- `calculate_path(start: Position, end: Position)`: Pathfinding algorithm

#### 2. Resources (Data Sources)
Resources are read-only data that the LLM can access. Examples:
- File contents (logs, configs)
- Database records
- API responses
- World state snapshots

#### 3. Prompts (Reusable Templates)
Prompts are pre-built prompt templates that can be customized. Examples:
- "Analyze this Minecraft world state: {world_state}"
- "Generate a building plan for: {structure_description}"
- "Debug this agent behavior: {error_log}"

---

## MCP Architecture

### Client-Host-Server Model

MCP follows a **client-server architecture** with three key components:

```
┌─────────────────────────────────────────────────────────────────┐
│                         MCP Host                                │
│                    (MineWright Mod)                             │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                     MCP Client                             │  │
│  │  - Protocol negotiation                                   │  │
│  │  - Message passing                                        │  │
│  │  - Session management                                     │  │
│  └───────────────┬───────────────────────────────────────────┘  │
│                  │ JSON-RPC 2.0                                 │
└──────────────────┼──────────────────────────────────────────────┘
                   │
    ┌──────────────┼──────────────┬──────────────────┐
    │              │              │                  │
┌───▼────┐   ┌────▼───┐   ┌────▼──────┐   ┌──────▼──────┐
│ Jupyter │   │  File   │   │ Database  │   │  Custom     │
│ Server  │   │ System  │   │ Server    │   │  Tool       │
└─────────┘   └─────────┘   └───────────┘   └─────────────┘
```

### Component Responsibilities

| Component | Role | Examples |
|-----------|------|----------|
| **Host** | AI application that coordinates clients | Claude Desktop, VS Code, **MineWright** |
| **Client** | Maintains connection with MCP server | MCP Java SDK `McpSyncClient` |
| **Server** | Exposes tools/resources/prompts | Jupyter MCP Server, Filesystem MCP Server |

### Two-Layer Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│              (MineWright Action Execution)                  │
└────────────────────────────────────────────────────────────┘
                              │
┌────────────────────────────────────────────────────────────┐
│                    Data Layer (MCP)                         │
│  ┌──────────────────────────────────────────────────────┐ │
│  │             MCP Protocol (JSON-RPC 2.0)              │ │
│  │  - Lifecycle management (initialize, initialized)    │ │
│  │  - Core primitives (tools, resources, prompts)       │ │
│  │  - Notifications (list_changed, etc.)                │ │
│  └──────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
                              │
┌────────────────────────────────────────────────────────────┐
│                   Transport Layer                           │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐ │
│  │   STDIO    │  │    SSE     │  │      HTTP            │ │
│  │ (Local)    │  │ (Push)     │  │   (Bidirectional)    │ │
│  └────────────┘  └────────────┘  └──────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

---

## Available MCP Servers

### Official MCP Servers

The official repository at `github.com/modelcontextprotocol/servers` provides reference implementations:

| Server | Description | Install Command |
|--------|-------------|-----------------|
| **Filesystem** | File operations (read, write, list) | `npx -y @modelcontextprotocol/server-filesystem ~/path` |
| **GitHub** | GitHub API (issues, PRs, code) | `npx -y @modelcontextprotocol/server-github` |
| **SQLite** | SQL database queries | `uvx mcp-server-sqlite --db-path ./data.db` |
| **Brave Search** | Web search integration | `npx -y @modelcontextprotocol/server-brave-search` |
| **PostgreSQL** | PostgreSQL database access | `npx -y @modelcontextprotocol/server-postgres` |
| **Puppeteer** | Browser automation | `npx -y @modelcontextprotocol/server-puppeteer` |
| **Fetch** | HTTP requests to any URL | `npx -y @modelcontextprotocol/server-fetch` |

### Jupyter MCP Server

**Repository:** `github.com/jupyter-server/jupyter-mcp-server`
**Purpose:** Integrates AI models with Jupyter Notebook for intelligent programming assistance

#### Installation

```bash
pip install jupyter-mcp-server
python -m jupyter_mcp_server
```

#### Capabilities

| Module | Features |
|--------|----------|
| **Kernel Interaction** | Execute code in active Jupyter kernels, get variable states, manage kernel lifecycle |
| **File System Access** | Read/write files and directories through Jupyter's content manager |
| **Terminal Access** | Execute shell commands, install packages, automate system tasks |
| **Notebook Management** | Save/retrieve notebook files, support real-time collaboration |

#### Use Cases for MineWright

- **Data Analysis**: Analyze world state data, agent performance metrics
- **Pathfinding**: Run Python pathfinding algorithms (A*, Dijkstra)
- **Visualization**: Generate plots of agent behavior, building progress
- **Machine Learning**: Train models on player behavior patterns

### Ecosystem Statistics

| Platform | Server Count |
|----------|--------------|
| **Smithery** | 2,200+ servers |
| **PulseMCP** | 3,228+ servers |
| **mcp.so** | 4,700+ servers |
| **Awesome MCP** | 200+ curated servers |

---

## Java Integration Guide

### MCP Java SDK

**Official Repository:** `github.com/modelcontextprotocol/java-sdk`
**Stars:** ~2,869+ (as of late 2025)
**Maintained by:** Model Context Protocol team with Spring AI collaboration

### Dependencies

#### Maven (pom.xml)

```xml
<dependencies>
    <!-- Core MCP SDK -->
    <dependency>
        <groupId>io.modelcontextprotocol.sdk</groupId>
        <artifactId>mcp</artifactId>
        <version>0.9.0</version>
    </dependency>

    <!-- Optional: Spring WebFlux for reactive HTTP -->
    <dependency>
        <groupId>io.modelcontextprotocol.sdk</groupId>
        <artifactId>mcp-spring-webflux</artifactId>
        <version>0.9.0</version>
    </dependency>

    <!-- For STDIO transport -->
    <dependency>
        <groupId>io.modelcontextprotocol.sdk</groupId>
        <artifactId>mcp-transport-stdio</artifactId>
        <version>0.9.0</version>
    </dependency>
</dependencies>
```

#### Gradle (build.gradle)

```gradle
dependencies {
    // Core MCP SDK
    implementation 'io.modelcontextprotocol.sdk:mcp:0.9.0'

    // Optional: Spring WebFlux for reactive HTTP
    implementation 'io.modelcontextprotocol.sdk:mcp-spring-webflux:0.9.0'

    // For STDIO transport
    implementation 'io.modelcontextprotocol.sdk:mcp-transport-stdio:0.9.0'
}
```

### Requirements

- **Java:** 17+ (Java 11+ for earlier versions)
- **Build Tool:** Maven or Gradle
- **Optional:** Spring Boot / Spring WebFlux for reactive support

---

## Integration Architecture for MineWright

### Proposed Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                         MineWright Mod                                 │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │                        Steve Agent                                │ │
│  │  ┌────────────────────────────────────────────────────────────┐  │ │
│  │  │                    TaskPlanner                              │ │ │
│  │  │  (LLM Integration - OpenAI/Groq/Gemini)                     │ │ │
│  │  └────────────────────────────────────────────────────────────┘  │ │
│  │                              │                                     │ │
│  │                              ▼                                     │ │
│  │  ┌────────────────────────────────────────────────────────────┐  │ │
│  │  │                  ActionExecutor                             │ │ │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐    │  │ │
│  │  │  │   Local     │  │    MCP      │  │   Plugin        │    │  │ │
│  │  │  │  Actions    │  │  Actions    │  │   Actions       │    │  │ │
│  │  │  │             │  │             │  │                 │    │  │ │
│  │  │  │ - Mine      │  │ - Python    │  │ - Custom        │    │  │ │
│  │  │  │ - Build     │  │ - File      │  │   Tools         │    │  │ │
│  │  │  │ - Move      │  │ - Database  │  │                 │    │  │ │
│  │  │  └─────────────┘  └─────────────┘  └─────────────────┘    │  │ │
│  │  └────────────────────────────────────────────────────────────┘  │ │
│  └──────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    MCP Client Manager                                  │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │              McpClientFactory (Singleton)                         │ │
│  │  - Manages client connections                                     │ │
│  │  - Connection pooling                                             │ │
│  │  - Error handling & retry logic                                   │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│                              │                                         │
│       ┌──────────────────────┼──────────────────────┐                 │
│       │                      │                      │                 │
│  ┌────▼──────┐         ┌────▼──────┐         ┌─────▼─────┐           │
│  │  Jupyter  │         │  File     │         │  Database  │           │
│  │  Client   │         │  System   │         │   Client   │           │
│  └───────────┘         └───────────┘         └───────────┘           │
└────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       MCP Servers (External)                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐ │
│  │  Jupyter    │  │ Filesystem  │  │   SQLite    │  │  Custom      │ │
│  │  MCP Server │  │ MCP Server  │  │ MCP Server  │  │  Tools       │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └──────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
src/main/java/com/minewright/
├── llm/
│   ├── mcp/                          # NEW: MCP Integration Package
│   │   ├── client/
│   │   │   ├── McpClientFactory.java # Creates and manages MCP clients
│   │   │   ├── McpClientManager.java # Lifecycle management
│   │   │   └── transport/
│   │   │       ├── StdioTransport.java
│   │   │       ├── SseTransport.java
│   │   │       └── HttpTransport.java
│   │   ├── action/
│   │   │   ├── McpAction.java        # Base class for MCP-backed actions
│   │   │   ├── JupyterAction.java    # Execute Python code
│   │   │   ├── FileReadAction.java   # Read files via MCP
│   │   │   ├── FileWriteAction.java  # Write files via MCP
│   │   │   └── DatabaseAction.java   # Query databases via MCP
│   │   ├── config/
│   │   │   ├── McpConfig.java        # Configuration POJO
│   │   │   └── ServerConfig.java     # Per-server config
│   │   └── protocol/
│   │       ├── ToolDefinition.java   # Tool metadata
│   │       ├── ToolCall.java         # Tool invocation
│   │       └── ToolResult.java       # Tool response
│   ├── OpenAIClient.java             # Existing LLM client
│   ├── TaskPlanner.java              # Existing task planner
│   └── ResponseParser.java           # Existing response parser
```

---

## Code Examples

### Example 1: Basic MCP Client Setup

```java
package com.minewright.llm.mcp.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating MCP clients with various transport options.
 */
public class McpClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpClientFactory.class);

    /**
     * Create a sync client with STDIO transport (local processes).
     *
     * @param command Command to start the MCP server
     * @param args Command arguments
     * @return Configured MCP client
     */
    public static McpSyncClient createStdioClient(String command, List<String> args) {
        LOGGER.info("Creating STDIO MCP client: {} {}", command, String.join(" ", args));

        StdioClientTransport transport = StdioClientTransport.builder()
            .command(command)
            .args(args)
            .build();

        return McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(30))
            .capabilities(McpSchema.ClientCapabilities.builder()
                .roots(true)
                .sampling()
                .build())
            .build();
    }

    /**
     * Initialize a client and discover available tools.
     */
    public static McpSchema.InitializeResult initializeAndDiscover(McpSyncClient client) {
        // Initialize connection
        McpSchema.InitializeResult initResult = client.initialize();
        LOGGER.info("Connected to MCP server: {} v{}",
            initResult.serverInfo().name(),
            initResult.serverInfo().version());

        // List available tools
        McpSchema.ListToolsResult tools = client.listTools();
        LOGGER.info("Discovered {} tools:", tools.tools().size());

        for (McpSchema.Tool tool : tools.tools()) {
            LOGGER.info("  - {}: {}", tool.name(), tool.description());
        }

        return initResult;
    }

    /**
     * Call a tool with parameters.
     */
    public static McpSchema.CallToolResult callTool(
        McpSyncClient client,
        String toolName,
        Map<String, Object> arguments
    ) {
        LOGGER.info("Calling tool: {} with arguments: {}", toolName, arguments);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
            toolName,
            arguments
        );

        McpSchema.CallToolResult result = client.callTool(request);

        LOGGER.info("Tool result: {}", result.content());

        return result;
    }
}
```

### Example 2: Jupyter MCP Integration

```java
package com.minewright.llm.mcp.action;

import com.minewright.llm.mcp.client.McpClientFactory;
import com.minewright.llm.mcp.protocol.ToolDefinition;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Action that executes Python code via Jupyter MCP Server.
 *
 * Use cases for MineWright:
 * - Data analysis (world state, agent metrics)
 * - Pathfinding algorithms (A*, Dijkstra)
 * - Visualization (behavior plots, building progress)
 * - Machine learning (player behavior patterns)
 */
public class JupyterAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(JupyterAction.class);

    private final McpSyncClient jupyterClient;

    public JupyterAction() {
        // Start Jupyter MCP server as subprocess
        this.jupyterClient = McpClientFactory.createStdioClient(
            "python",
            List.of("-m", "jupyter_mcp_server")
        );

        // Initialize and discover tools
        McpClientFactory.initializeAndDiscover(jupyterClient);
    }

    /**
     * Execute Python code and return the result.
     *
     * @param code Python code to execute
     * @return Execution output
     */
    public String executeCode(String code) {
        LOGGER.debug("Executing Python code: {}", code);

        Map<String, Object> arguments = Map.of(
            "code", code,
            "kernel", "python3"  // Use Python 3 kernel
        );

        McpSchema.CallToolResult result = McpClientFactory.callTool(
            jupyterClient,
            "execute_code",
            arguments
        );

        // Extract text output from result
        return result.content().stream()
            .filter(content -> content instanceof McpSchema.TextContent)
            .map(content -> ((McpSchema.TextContent) content).text())
            .findFirst()
            .orElse("");
    }

    /**
     * Get the value of a variable from the Jupyter kernel.
     *
     * @param variableName Variable name to retrieve
     * @return Variable value as string
     */
    public String getVariable(String variableName) {
        String code = String.format("print(%s)", variableName);
        return executeCode(code);
    }

    /**
     * Perform pathfinding using A* algorithm.
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param startZ Starting Z coordinate
     * @param endX Ending X coordinate
     * @param endY Ending Y coordinate
     * @param endZ Ending Z coordinate
     * @return List of coordinates forming the path
     */
    public List<String> findPathAStar(
        int startX, int startY, int startZ,
        int endX, int endY, int endZ
    ) {
        String code = String.format("""
            import heapq
            from typing import List, Tuple

            def heuristic(a: Tuple[int, int, int], b: Tuple[int, int, int]) -> float:
                return ((a[0]-b[0])**2 + (a[1]-b[1])**2 + (a[2]-b[2])**2)**0.5

            def a_star(start: Tuple[int, int, int], goal: Tuple[int, int, int]) -> List[Tuple[int, int, int]]:
                frontier = []
                heapq.heappush(frontier, (0, start))
                came_from = {start: None}
                cost_so_far = {start: 0}

                while frontier:
                    current = heapq.heappop(frontier)[1]

                    if current == goal:
                        break

                    # Check neighbors (6 directions in 3D)
                    for dx, dy, dz in [(1,0,0), (-1,0,0), (0,1,0), (0,-1,0), (0,0,1), (0,0,-1)]:
                        next_node = (current[0]+dx, current[1]+dy, current[2]+dz)
                        new_cost = cost_so_far[current] + 1  # Uniform cost

                        if next_node not in cost_so_far or new_cost < cost_so_far[next_node]:
                            cost_so_far[next_node] = new_cost
                            priority = new_cost + heuristic(next_node, goal)
                            heapq.heappush(frontier, (priority, next_node))
                            came_from[next_node] = current

                # Reconstruct path
                if goal not in came_from:
                    return []

                path = []
                current = goal
                while current != start:
                    path.append(current)
                    current = came_from[current]
                path.append(start)
                path.reverse()

                return path

            start = (%d, %d, %d)
            goal = (%d, %d, %d)
            path = a_star(start, goal)
            print(path)
            """,
            startX, startY, startZ,
            endX, endY, endZ
        );

        String result = executeCode(code);
        // Parse result to extract path coordinates
        return parsePathFromOutput(result);
    }

    /**
     * Generate a visualization of agent behavior.
     *
     * @param positions List of agent positions over time
     * @return Base64-encoded plot image
     */
    public String visualizeBehavior(List<String> positions) {
        String code = String.format("""
            import matplotlib.pyplot as plt
            import io
            import base64

            # Parse positions (x, z coordinates)
            positions = %s
            x_coords = [pos[0] for pos in positions]
            z_coords = [pos[1] for pos in positions]

            plt.figure(figsize=(10, 10))
            plt.plot(x_coords, z_coords, 'b-', linewidth=2)
            plt.plot(x_coords, z_coords, 'ro', markersize=3)
            plt.xlabel('X Coordinate')
            plt.ylabel('Z Coordinate')
            plt.title('Agent Movement Over Time')
            plt.grid(True)

            # Save to base64
            buf = io.BytesIO()
            plt.savefig(buf, format='png', dpi=100, bbox_inches='tight')
            buf.seek(0)
            img_base64 = base64.b64encode(buf.read()).decode('utf-8')
            print(img_base64)
            """, positions);

        return executeCode(code);
    }

    private List<String> parsePathFromOutput(String output) {
        // Simple parser - in production, use proper JSON parsing
        return List.of(output.split(", "));
    }

    public void close() {
        if (jupyterClient != null) {
            jupyterClient.close();
        }
    }
}
```

### Example 3: File System MCP Integration

```java
package com.minewright.llm.mcp.action;

import com.minewright.llm.mcp.client.McpClientFactory;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Action for file system operations via Filesystem MCP Server.
 *
 * Use cases for MineWright:
 * - Read/write configuration files
 * - Access world backups and logs
 * - Save/load building plans
 * - Import/export agent state
 */
public class FileSystemAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemAction.class);

    private final McpSyncClient fsClient;
    private final Path allowedDirectory;

    public FileSystemAction(Path allowedDirectory) {
        this.allowedDirectory = allowedDirectory;

        // Start Filesystem MCP server with restricted directory access
        this.fsClient = McpClientFactory.createStdioClient(
            "npx",
            List.of(
                "-y",
                "@modelcontextprotocol/server-filesystem",
                allowedDirectory.toString()
            )
        );

        McpClientFactory.initializeAndDiscover(fsClient);
    }

    /**
     * Read a file's contents.
     *
     * @param filePath Path relative to allowed directory
     * @return File contents
     */
    public String readFile(String filePath) {
        LOGGER.debug("Reading file: {}", filePath);

        Map<String, Object> arguments = Map.of(
            "path", filePath
        );

        McpSchema.CallToolResult result = McpClientFactory.callTool(
            fsClient,
            "read_file",
            arguments
        );

        return extractTextContent(result);
    }

    /**
     * Write content to a file.
     *
     * @param filePath Path relative to allowed directory
     * @param content Content to write
     */
    public void writeFile(String filePath, String content) {
        LOGGER.debug("Writing file: {} ({} bytes)", filePath, content.length());

        Map<String, Object> arguments = Map.of(
            "path", filePath,
            "content", content
        );

        McpClientFactory.callTool(fsClient, "write_file", arguments);
    }

    /**
     * List files in a directory.
     *
     * @param directoryPath Path relative to allowed directory
     * @return List of file/directory names
     */
    public List<String> listDirectory(String directoryPath) {
        LOGGER.debug("Listing directory: {}", directoryPath);

        Map<String, Object> arguments = Map.of(
            "path", directoryPath
        );

        McpSchema.CallToolResult result = McpClientFactory.callTool(
            fsClient,
            "list_directory",
            arguments
        );

        return extractTextContent(result).lines().toList();
    }

    /**
     * Search for files by pattern.
     *
     * @param pattern Glob pattern (e.g., "*.json")
     * @param directoryPath Directory to search
     * @return Matching file paths
     */
    public List<String> searchFiles(String pattern, String directoryPath) {
        LOGGER.debug("Searching files: {} in {}", pattern, directoryPath);

        Map<String, Object> arguments = Map.of(
            "pattern", pattern,
            "path", directoryPath
        );

        McpSchema.CallToolResult result = McpClientFactory.callTool(
            fsClient,
            "search_files",
            arguments
        );

        return extractTextContent(result).lines().toList();
    }

    /**
     * Load agent memory from JSON file.
     */
    public String loadAgentMemory(String agentName) {
        String path = String.format("agents/%s/memory.json", agentName);
        return readFile(path);
    }

    /**
     * Save agent memory to JSON file.
     */
    public void saveAgentMemory(String agentName, String memoryJson) {
        String path = String.format("agents/%s/memory.json", agentName);
        writeFile(path, memoryJson);
    }

    /**
     * Export world state for backup.
     */
    public void exportWorldState(String worldName, String stateJson) {
        String timestamp = java.time.Instant.now()
            .toString()
            .replace(":", "-")
            .replace(".", "-");

        String path = String.format("backups/%s/%s.json", worldName, timestamp);
        writeFile(path, stateJson);
    }

    private String extractTextContent(McpSchema.CallToolResult result) {
        return result.content().stream()
            .filter(content -> content instanceof McpSchema.TextContent)
            .map(content -> ((McpSchema.TextContent) content).text())
            .findFirst()
            .orElse("");
    }

    public void close() {
        if (fsClient != null) {
            fsClient.close();
        }
    }
}
```

### Example 4: Database MCP Integration

```java
package com.minewright.llm.mcp.action;

import com.minewright.llm.mcp.client.McpClientFactory;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Action for database operations via SQLite MCP Server.
 *
 * Use cases for MineWright:
 * - Persistent agent memory storage
 * - World state snapshots
 * - Building plan templates
 * - Analytics and metrics
 */
public class DatabaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAction.class);

    private final McpSyncClient dbClient;

    public DatabaseAction(Path databasePath) {
        // Start SQLite MCP server
        this.dbClient = McpClientFactory.createStdioClient(
            "uvx",
            List.of(
                "mcp-server-sqlite",
                "--db-path",
                databasePath.toString()
            )
        );

        McpClientFactory.initializeAndDiscover(dbClient);

        // Initialize schema
        initializeSchema();
    }

    /**
     * Initialize database schema for MineWright data.
     */
    private void initializeSchema() {
        String schemaSql = """
            CREATE TABLE IF NOT EXISTS agents (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                last_active INTEGER DEFAULT (strftime('%s', 'now'))
            );

            CREATE TABLE IF NOT EXISTS agent_memory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                agent_id TEXT NOT NULL,
                key TEXT NOT NULL,
                value TEXT,
                timestamp INTEGER DEFAULT (strftime('%s', 'now')),
                FOREIGN KEY (agent_id) REFERENCES agents(id)
            );

            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                agent_id TEXT NOT NULL,
                description TEXT NOT NULL,
                status TEXT DEFAULT 'pending',
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                completed_at INTEGER,
                FOREIGN KEY (agent_id) REFERENCES agents(id)
            );

            CREATE TABLE IF NOT EXISTS world_state (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                z INTEGER NOT NULL,
                block_type TEXT NOT NULL,
                timestamp INTEGER DEFAULT (strftime('%s', 'now'))
            );

            CREATE INDEX IF NOT EXISTS idx_agent_memory_agent ON agent_memory(agent_id);
            CREATE INDEX IF NOT EXISTS idx_agent_memory_key ON agent_memory(key);
            CREATE INDEX IF NOT EXISTS idx_tasks_agent ON tasks(agent_id);
            CREATE INDEX IF NOT EXISTS idx_world_position ON world_state(x, y, z);
            """;

        executeSql(schemaSql);
        LOGGER.info("Database schema initialized");
    }

    /**
     * Execute SQL query and return results.
     *
     * @param sql SQL query
     * @return Query results as formatted string
     */
    public String executeSql(String sql) {
        LOGGER.debug("Executing SQL: {}", sql);

        Map<String, Object> arguments = Map.of("sql", sql);

        McpSchema.CallToolResult result = McpClientFactory.callTool(
            dbClient,
            "query",
            arguments
        );

        return extractTextContent(result);
    }

    /**
     * Store agent memory in database.
     *
     * @param agentId Agent identifier
     * @param key Memory key
     * @param value Memory value
     */
    public void storeMemory(String agentId, String key, String value) {
        String sql = String.format(
            "INSERT INTO agent_memory (agent_id, key, value) VALUES ('%s', '%s', '%s')",
            escapeSql(agentId),
            escapeSql(key),
            escapeSql(value)
        );
        executeSql(sql);
    }

    /**
     * Retrieve agent memory from database.
     *
     * @param agentId Agent identifier
     * @param key Memory key
     * @return Memory value or null if not found
     */
    public String retrieveMemory(String agentId, String key) {
        String sql = String.format(
            "SELECT value FROM agent_memory WHERE agent_id = '%s' AND key = '%s' ORDER BY timestamp DESC LIMIT 1",
            escapeSql(agentId),
            escapeSql(key)
        );

        String result = executeSql(sql);
        // Parse result to extract value
        if (result.contains("|")) {
            String[] parts = result.split("\\|");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        return null;
    }

    /**
     * Get world blocks in a radius.
     *
     * @param centerX Center X coordinate
     * @param centerY Center Y coordinate
     * @param centerZ Center Z coordinate
     * @param radius Search radius
     * @return List of blocks
     */
    public List<String> getBlocksInRadius(int centerX, int centerY, int centerZ, int radius) {
        String sql = String.format("""
            SELECT x, y, z, block_type FROM world_state
            WHERE x BETWEEN %d AND %d
              AND y BETWEEN %d AND %d
              AND z BETWEEN %d AND %d
            ORDER BY x, y, z
            """,
            centerX - radius, centerX + radius,
            centerY - radius, centerY + radius,
            centerZ - radius, centerZ + radius
        );

        String result = executeSql(sql);
        return result.lines().toList();
    }

    /**
     * Log task completion for analytics.
     */
    public void logTaskCompletion(String agentId, String taskDescription, boolean success) {
        String status = success ? "completed" : "failed";
        String sql = String.format(
            "UPDATE tasks SET status = '%s', completed_at = strftime('%%s', 'now') WHERE agent_id = '%s' AND description = '%s'",
            status,
            escapeSql(agentId),
            escapeSql(taskDescription)
        );
        executeSql(sql);
    }

    /**
     * Get agent performance metrics.
     */
    public String getAgentMetrics(String agentId) {
        String sql = String.format("""
            SELECT
                COUNT(*) as total_tasks,
                SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completed_tasks,
                AVG(CASE WHEN completed_at IS NOT NULL THEN completed_at - created_at END) as avg_completion_time
            FROM tasks
            WHERE agent_id = '%s'
            """,
            escapeSql(agentId)
        );

        return executeSql(sql);
    }

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }

    private String extractTextContent(McpSchema.CallToolResult result) {
        return result.content().stream()
            .filter(content -> content instanceof McpSchema.TextContent)
            .map(content -> ((McpSchema.TextContent) content).text())
            .findFirst()
            .orElse("");
    }

    public void close() {
        if (dbClient != null) {
            dbClient.close();
        }
    }
}
```

### Example 5: MCP-Backed Base Action

```java
package com.minewright.llm.mcp.action;

import com.minewright.action.actions.BaseAction;
import com.minewright.entity.SteveEntity;
import com.minewright.llm.mcp.client.McpClientFactory;
import com.minewright.llm.mcp.protocol.ToolDefinition;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Base action for MCP-backed capabilities.
 * Integrates MCP tools with MineWright's action system.
 */
public abstract class McpAction extends BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpAction.class);

    protected final McpSyncClient mcpClient;
    protected final String toolName;

    public McpAction(
        SteveEntity steve,
        String task,
        McpSyncClient mcpClient,
        String toolName
    ) {
        super(steve, task);
        this.mcpClient = mcpClient;
        this.toolName = toolName;
    }

    /**
     * Call the MCP tool with arguments.
     *
     * @param arguments Tool arguments
     * @return Tool result
     */
    protected McpSchema.CallToolResult callTool(Map<String, Object> arguments) {
        LOGGER.info("[{}] Calling tool: {}", steve.getName().getString(), toolName);

        try {
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                toolName,
                arguments
            );

            McpSchema.CallToolResult result = mcpClient.callTool(request);

            // Log result for debugging
            String output = result.content().stream()
                .filter(content -> content instanceof McpSchema.TextContent)
                .map(content -> ((McpSchema.TextContent) content).text())
                .findFirst()
                .orElse("");

            LOGGER.debug("[{}] Tool result: {}", steve.getName().getString(), output);

            return result;

        } catch (Exception e) {
            LOGGER.error("[{}] Tool call failed: {}", steve.getName().getString(), e.getMessage(), e);
            throw new RuntimeException("MCP tool call failed", e);
        }
    }

    /**
     * Extract text content from tool result.
     */
    protected String extractText(McpSchema.CallToolResult result) {
        return result.content().stream()
            .filter(content -> content instanceof McpSchema.TextContent)
            .map(content -> ((McpSchema.TextContent) content).text())
            .findFirst()
            .orElse("");
    }

    @Override
    public void onCancel() {
        LOGGER.info("[{}] MCP action cancelled: {}", steve.getName().getString(), toolName);
    }
}
```

### Example 6: Async MCP Client with WebFlux

```java
package com.minewright.llm.mcp.client;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Async MCP client using Spring WebFlux for non-blocking operations.
 *
 * Benefits for MineWright:
 * - Non-blocking tool calls during game ticks
 * - Multiple concurrent MCP operations
 * - Better resource utilization
 */
public class AsyncMcpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncMcpClient.class);

    private final McpAsyncClient client;

    public AsyncMcpClient(String serverUrl) {
        WebClient.Builder webClientBuilder = WebClient.builder()
            .baseUrl(serverUrl);

        WebFluxSseClientTransport transport = WebFluxSseClientTransport
            .builder(webClientBuilder)
            .sseEndpoint("/sse")
            .build();

        this.client = McpAsyncClient.builder()
            .transport(transport)
            .requestTimeout(Duration.ofSeconds(30))
            .capabilities(McpSchema.ClientCapabilities.builder()
                .roots(true)
                .sampling()
                .build())
            .build();
    }

    /**
     * Initialize connection asynchronously.
     */
    public Mono<McpSchema.InitializeResult> initialize() {
        return client.initialize()
            .doOnSubscribe(s -> LOGGER.info("Connecting to MCP server..."))
            .doOnSuccess(result -> LOGGER.info("Connected to: {}", result.serverInfo().name()))
            .doOnError(e -> LOGGER.error("Failed to connect: {}", e.getMessage()));
    }

    /**
     * List tools asynchronously.
     */
    public Mono<List<McpSchema.Tool>> listTools() {
        return client.listTools()
            .map(McpSchema.ListToolsResult::tools);
    }

    /**
     * Call a tool asynchronously.
     */
    public Mono<McpSchema.CallToolResult> callTool(
        String toolName,
        Map<String, Object> arguments
    ) {
        return client.callTool(new McpSchema.CallToolRequest(toolName, arguments))
            .doOnSubscribe(s -> LOGGER.debug("Calling tool: {}", toolName))
            .doOnError(e -> LOGGER.error("Tool call failed: {}", e.getMessage()));
    }

    /**
     * Call multiple tools concurrently and collect results.
     */
    public Flux<McpSchema.CallToolResult> callToolsConcurrently(
        List<ToolCall> toolCalls
    ) {
        return Flux.fromIterable(toolCalls)
            .flatMap(call -> callTool(call.name(), call.arguments()));
    }

    /**
     * Stream tool results as they arrive.
     */
    public Flux<String> streamToolOutput(String toolName, Map<String, Object> arguments) {
        return callTool(toolName, arguments)
            .flatMapMany(result -> Flux.fromIterable(result.content()))
            .filter(content -> content instanceof McpSchema.TextContent)
            .map(content -> ((McpSchema.TextContent) content).text());
    }

    public record ToolCall(String name, Map<String, Object> arguments) {}
}
```

### Example 7: Configuration Integration

```groovy
// build.gradle - Add MCP dependencies
dependencies {
    // Core MCP SDK
    implementation 'io.modelcontextprotocol.sdk:mcp:0.9.0'

    // Spring WebFlux for async support (optional but recommended)
    implementation 'io.modelcontextprotocol.sdk:mcp-spring-webflux:0.9.0'

    // STDIO transport for local servers
    implementation 'io.modelcontextprotocol.sdk:mcp-transport-stdio:0.9.0'

    // Existing dependencies...
    implementation 'org.graalvm.polyglot:polyglot:23.1.0'
    implementation 'org.graalvm.polyglot:js:23.1.0'
    implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.1.0'
    implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'
}
```

```toml
# config/minewright-common.toml - MCP configuration

[mcp]
# Enable MCP integration
enabled = true

# Default transport type (stdio, sse, http)
defaultTransport = "stdio"

# Connection timeout in seconds
connectionTimeout = 30

# Request timeout in seconds
requestTimeout = 60

[mcp.servers.jupyter]
# Jupyter MCP Server for Python execution
enabled = true
command = "python"
args = ["-m", "jupyter_mcp_server"]
transport = "stdio"

[mcp.servers.filesystem]
# Filesystem MCP Server for file operations
enabled = true
command = "npx"
args = ["-y", "@modelcontextprotocol/server-filesystem", "${user.dir}/config"]
transport = "stdio"

[mcp.servers.database]
# SQLite MCP Server for persistent storage
enabled = true
command = "uvx"
args = ["mcp-server-sqlite", "--db-path", "${user.dir}/run/minewright.db"]
transport = "stdio"

[mcp.tools]
# Tool discovery and execution settings
autoDiscovery = true
cacheDefinitions = true
maxConcurrentCalls = 10

[mcp.tools.python]
# Python-specific tool configurations
kernel = "python3"
timeout = 30
maxOutputLength = 10000
```

```java
// src/main/java/com/minewright/llm/mcp/config/McpConfig.java
package com.minewright.llm.mcp.config;

import java.util.List;
import java.util.Map;

/**
 * MCP configuration loaded from TOML config file.
 */
public class McpConfig {

    private boolean enabled = true;
    private String defaultTransport = "stdio";
    private int connectionTimeout = 30;
    private int requestTimeout = 60;
    private Map<String, ServerConfig> servers;
    private ToolConfig tools;

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public String getDefaultTransport() { return defaultTransport; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getRequestTimeout() { return requestTimeout; }
    public Map<String, ServerConfig> getServers() { return servers; }
    public ToolConfig getTools() { return tools; }

    public static class ServerConfig {
        private boolean enabled = true;
        private String command;
        private List<String> args;
        private String transport = "stdio";
        private Map<String, String> env;

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public String getCommand() { return command; }
        public List<String> getArgs() { return args; }
        public String getTransport() { return transport; }
        public Map<String, String> getEnv() { return env; }
    }

    public static class ToolConfig {
        private boolean autoDiscovery = true;
        private boolean cacheDefinitions = true;
        private int maxConcurrentCalls = 10;
        private Map<String, Object> python;

        // Getters and setters
        public boolean isAutoDiscovery() { return autoDiscovery; }
        public boolean isCacheDefinitions() { return cacheDefinitions; }
        public int getMaxConcurrentCalls() { return maxConcurrentCalls; }
        public Map<String, Object> getPython() { return python; }
    }
}
```

---

## Benefits for AI Agent Tool Use

### 1. Standardized Tool Discovery

**Before MCP:**
```java
// Custom API for each tool
WeatherApi weather = new WeatherApi("api-key");
String forecast = weather.getForecast("Paris");

GithubApi github = new GithubApi("token");
List<Issue> issues = github.getIssues("owner/repo");
```

**After MCP:**
```java
// Universal interface
McpSyncClient client = McpClient.sync(transport).build();

// Discover tools automatically
List<Tool> tools = client.listTools().tools();

// Call any tool with same interface
CallToolResult weather = client.callTool(
    new CallToolRequest("get_weather", Map.of("location", "Paris"))
);

CallToolResult issues = client.callTool(
    new CallToolRequest("list_issues", Map.of("repo", "owner/repo"))
);
```

### 2. LLM-Native Tool Calling

MCP is designed for LLM integration. Tools are described in a schema that LLMs can understand:

```json
{
  "name": "calculate_path",
  "description": "Calculate optimal path between two Minecraft positions using A* algorithm",
  "inputSchema": {
    "type": "object",
    "properties": {
      "start": {
        "type": "object",
        "description": "Starting position",
        "properties": {
          "x": {"type": "integer"},
          "y": {"type": "integer"},
          "z": {"type": "integer"}
        }
      },
      "end": {
        "type": "object",
        "description": "Ending position",
        "properties": {
          "x": {"type": "integer"},
          "y": {"type": "integer"},
          "z": {"type": "integer"}
        }
      }
    }
  }
}
```

The LLM receives this schema and knows:
- What the tool does
- What parameters it needs
- What types those parameters are
- How to invoke it

### 3. Language Interoperability

Write tools in any language, use them from Java:

```python
# Python MCP server
@mcp.tool(name="advanced_pathfinding")
def advanced_pathfinding(start: tuple, end: tuple, algorithm: str = "a*") -> list:
    """Advanced pathfinding with multiple algorithms."""
    if algorithm == "a*":
        return a_star(start, end)
    elif algorithm == "dijkstra":
        return dijkstra(start, end)
    elif algorithm == "jps":
        return jump_point_search(start, end)
```

```java
// Call from Java
McpSyncClient client = createStdioClient("python", List.of("pathfinding_server.py"));
CallToolResult result = client.callTool(new CallToolRequest(
    "advanced_pathfinding",
    Map.of(
        "start", List.of(0, 64, 0),
        "end", List.of(100, 64, 100),
        "algorithm", "jps"
    )
));
```

### 4. Composable Tool Ecosystem

Chain multiple MCP servers together:

```java
// Combine Jupyter + Filesystem + Database
McpSyncClient jupyter = createJupyterClient();
McpSyncClient filesystem = createFilesystemClient();
McpSyncClient database = createDatabaseClient();

// Complex workflow: Read config -> Calculate -> Save results
String config = filesystem.callTool("read_file", Map.of("path", "config.json"));
String calculation = jupyter.callTool("execute_code", Map.of("code",
    "import json; config = " + config + "; result = analyze(config)"
));
database.callTool("insert_result", Map.of("data", calculation));
```

### 5. Runtime Flexibility

Add/remove tools without recompiling:

```java
// Discover available tools at runtime
List<Tool> availableTools = client.listTools().tools();

// Filter by capability
List<Tool> pathfindingTools = availableTools.stream()
    .filter(t -> t.description().toLowerCase().contains("path"))
    .toList();

// Let LLM choose best tool
String prompt = "Available pathfinding tools: " +
    pathfindingTools.stream()
        .map(Tool::name)
        .collect(Collectors.joining(", "));
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Goal:** Basic MCP integration with test infrastructure

- [ ] Add MCP Java SDK dependencies to `build.gradle`
- [ ] Create `McpClientFactory` for client creation
- [ ] Implement `McpAction` base class
- [ ] Create unit tests for MCP client lifecycle
- [ ] Add MCP configuration to `minewright-common.toml`

**Deliverables:**
- `McpClientFactory.java` - Client creation and initialization
- `McpAction.java` - Base action for MCP tools
- `McpConfig.java` - Configuration POJO
- Unit tests for MCP integration

### Phase 2: Jupyter Integration (Week 3-4)

**Goal:** Python code execution via Jupyter MCP Server

- [ ] Install and configure Jupyter MCP Server
- [ ] Implement `JupyterAction` for code execution
- [ ] Add pathfinding example (A* algorithm in Python)
- [ ] Add visualization example (agent behavior plots)
- [ ] Test integration with game tick loop

**Deliverables:**
- `JupyterAction.java` - Python code execution
- Pathfinding action using A*
- Visualization action for agent metrics
- Integration tests

### Phase 3: File System Integration (Week 5-6)

**Goal:** File operations via Filesystem MCP Server

- [ ] Implement `FileSystemAction` for file operations
- [ ] Add agent memory persistence
- [ ] Add world state export/import
- [ ] Add configuration file management
- [ ] Security: Restrict file access to config directory

**Deliverables:**
- `FileSystemAction.java` - File operations
- Agent memory save/load actions
- World state backup actions
- Security sandboxing

### Phase 4: Database Integration (Week 7-8)

**Goal:** Persistent storage via SQLite MCP Server

- [ ] Implement `DatabaseAction` for SQL operations
- [ ] Define schema for agents, tasks, world state
- [ ] Add analytics queries
- [ ] Add caching layer with Caffeine
- [ ] Performance testing

**Deliverables:**
- `DatabaseAction.java` - Database operations
- Database schema migrations
- Analytics dashboard queries
- Performance benchmarks

### Phase 5: LLM Integration (Week 9-10)

**Goal:** Enable LLM to discover and use MCP tools

- [ ] Update `PromptBuilder` to include MCP tool definitions
- [ ] Add tool discovery to `ResponseParser`
- [ ] Implement tool selection logic in `TaskPlanner`
- [ ] Add tool result handling
- [ ] Error handling and retry logic

**Deliverables:**
- Updated prompt templates with MCP tools
- Tool discovery mechanism
- Tool execution flow in `ActionExecutor`
- Error handling with Resilience4j

### Phase 6: Advanced Features (Week 11-12)

**Goal:** Advanced MCP capabilities

- [ ] Implement async MCP client with WebFlux
- [ ] Add streaming tool output support
- [ ] Implement custom MCP server for Minecraft-specific tools
- [ ] Add tool composition (chaining multiple tools)
- [ ] Performance optimization

**Deliverables:**
- `AsyncMcpClient.java` - Non-blocking operations
- Streaming output support
- Custom Minecraft MCP server
- Tool composition framework

---

## References and Resources

### Official Documentation

| Resource | URL |
|----------|-----|
| **MCP Specification** | https://spec.modelcontextprotocol.io/ |
| **MCP Official Site** | https://modelcontextprotocol.io/ |
| **GitHub Repository** | https://github.com/modelcontextprotocol/ |
| **Java SDK** | https://github.com/modelcontextprotocol/java-sdk |
| **Python SDK** | https://github.com/modelcontextprotocol/python-sdk |
| **TypeScript SDK** | https://github.com/modelcontextprotocol/typescript-sdk |
| **Official Servers** | https://github.com/modelcontextprotocol/servers |

### Community Resources

| Resource | URL |
|----------|-----|
| **Smithery Registry** | https://smithery.ai/ (2,200+ servers) |
| **PulseMCP** | https://pulsemcp.com/ (3,228+ servers) |
| **mcp.so Portal** | https://mcp.so/ (4,700+ servers) |
| **Awesome MCP** | https://github.com/awesome-mcp/awesome-mcp-servers |
| **LobeHub MCP Directory** | https://lobehub.com/zh/mcp |

### Tutorials and Guides

| Resource | URL |
|----------|-----|
| **freeCodeCamp FastMCP Tutorial** | https://www.freecodecamp.org/news/how-to-build-your-first-mcp-server-using-fastmcp/ |
| **CSDN MCP Java SDK Guide** | https://blog.csdn.net/gitblog_01138/article/details/156412816 |
| **SegmentFault MCP Guide** | https://segmentfault.com/a/1190000047480708 |
| **掘金 MCP 实战** | https://juejin.cn/post/7610160716066308106 |

### Research Sources Consulted

This document was compiled using research from the following sources:

1. **MCP Protocol Architecture** - [CSDN - Model Context Protocol架构解析](https://m.blog.csdn.net/gitblog_00331/article/details/152200199)
2. **MCP Java SDK** - [CSDN - MCP Java SDK 由入门到精通](https://blog.csdn.net/soosky/article/details/149132586)
3. **Jupyter MCP Server** - [Tencent Cloud - Jupyter MCP服务器部署实战](https://cloud.tencent.com/developer/article/2557121)
4. **Filesystem MCP Server** - [CSDN - Filesystem MCP Server](https://m.blog.csdn.net/gitblog_00826/article/details/152347000)
5. **MCP Architecture** - [CSDN - MCP 架构全解析](https://blog.csdn.net/2509_93932770/article/details/153979094)
6. **MCP Tools and Resources** - [CSDN - MCP：标准化的工具暴露与发现](https://m.blog.csdn.net/mboy2008/article/details/157904758)
7. **Spring AI MCP** - [SpringDoc - MCP Overview](https://springdoc.cn/spring-ai/api/mcp/mcp-overview.html)
8. **MCP Transport Layers** - [CSDN - Model Context Protocol传输机制](https://m.blog.csdn.net/gitblog_00335/article/details/152201136)
9. **MCP JSON-RPC Messages** - [CSDN - MCP协议核心概念](https://blog.csdn.net/ttyy1112/article/details/156096714)
10. **Available MCP Servers** - [AI-Bot.cn - MCP Servers](https://ai-bot.cn/mcp-servers/)

### Key Technical Papers

- **Anthropic MCP Introduction** (November 2024) - Original protocol specification
- **ThoughtWorks Technology Radar** - MCP as adoptable technology
- **Spring AI 1.0.0 MCP Client** - Enterprise Java integration patterns

---

## Conclusion

The Model Context Protocol (MCP) represents a paradigm shift in how AI applications integrate with external tools and data sources. For MineWright, MCP offers:

1. **Standardized Integration**: No more custom API wrappers for each tool
2. **LLM-Native Design**: Built specifically for AI agent tool use
3. **Rich Ecosystem**: 4,700+ pre-built servers ready to use
4. **Production Ready**: Battle-tested by major platforms
5. **Future Proof**: Open standard with growing adoption

The integration roadmap provides a clear path from basic MCP setup to advanced multi-agent orchestration, with tangible benefits at each phase.

### Next Steps

1. Review this research document with the development team
2. Prioritize which MCP servers are most valuable (Jupyter, Filesystem, Database)
3. Begin Phase 1 implementation (Foundation)
4. Establish success metrics for each phase

---

**Document Status:** Complete
**Last Updated:** 2026-02-27
**Version:** 1.0
