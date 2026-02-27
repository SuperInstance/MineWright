# Jupyter Kernel Gateway Integration for AI Agent Mental Simulation

**Research Document**
**Author:** Orchestrator Agent
**Date:** 2025-02-27
**Version:** 1.0

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Jupyter Kernel Gateway Architecture](#jupyter-kernel-gateway-architecture)
3. [MCP Integration Possibilities](#mcp-integration-possibilities)
4. [Java Client Implementation](#java-client-implementation)
5. [Mental Simulation Concepts](#mental-simulation-concepts)
6. [Integration with MineWright](#integration-with-minewright)
7. [Pros/Cons Analysis](#proscons-analysis)
8. [Implementation Roadmap](#implementation-roadmap)
9. [References](#references)

---

## Executive Summary

This research document explores integrating **Jupyter Kernel Gateway (JKG)** with the MineWright Minecraft Forge mod to enable **AI agent mental simulation** capabilities. The integration would allow AI agents (ForemanEntity) to use Python/Jupyter kernels as "cognitive sandboxes" for:

- **Predictive world modeling** - Simulating action consequences before execution
- **Spatial reasoning** - Complex 3D geometry calculations for building
- **Pathfinding optimization** - Graph algorithms for navigation
- **Resource estimation** - Calculating material requirements
- **Multi-agent coordination** - Game theory and optimization algorithms

**Key Findings:**
- Jupyter Kernel Gateway provides REST API + WebSocket communication
- Java can communicate via WebSocket using Tyrus, Netty, or Java-WebSocket
- Model Context Protocol (MCP) offers standardized AI tool integration
- Spring AI provides official Java MCP SDK (v0.15.0)
- Mental simulation is a cutting-edge AI research topic (DeepSeek R1, DreamerV3)

---

## Jupyter Kernel Gateway Architecture

### Overview

Jupyter Kernel Gateway is a **headless web server** that exposes Jupyter kernels via HTTP/REST and WebSocket protocols, eliminating the need for direct ZeroMQ messaging from clients.

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Minecraft     │     │  Jupyter Kernel  │     │  Python Kernel  │
│  (Java Client)  │◄────┤     Gateway      │◄────┤   (IPython)     │
│   ForemanEntity │     │   :8888/rest     │     │  Mental Model   │
└─────────────────┘     └──────────────────┘     └─────────────────┘
      WebSocket            ZeroMQ (internal)          Execution
```

### Core Architecture Components

#### 1. Communication Layers

| Layer | Protocol | Purpose |
|-------|----------|---------|
| **Client ↔ Gateway** | HTTP/REST | Kernel lifecycle management |
| **Client ↔ Gateway** | WebSocket | Code execution and result streaming |
| **Gateway ↔ Kernel** | ZeroMQ | High-performance messaging |

#### 2. Jupyter Kernel ZeroMQ Sockets

The kernel implements **five ZeroMQ sockets** for different communication channels:

| Socket Type | Channel | Purpose |
|-------------|---------|---------|
| `DEALER-ROUTER` | **Shell** | Execute requests, code completion, inspection |
| `PUB-SUB` | **IOPub** | Publish outputs (stdout, stderr, results) |
| `DEALER-ROUTER` | **Stdin** | Input requests (e.g., `input()` function) |
| `DEALER-ROUTER` | **Control** | Shutdown, restart, debug requests |
| `REQ-REP` | **Heartbeat** | Monitor kernel health/liveness |

---

## API Endpoints and WebSocket Protocol

### REST API Endpoints

#### Create Kernel

```http
POST /api/kernels HTTP/1.1
Host: localhost:8888
Content-Type: application/json

{
  "name": "python3"
}
```

**Response:**
```json
{
  "id": "f7e3c8a9-2b1d-4e5f-8a9b-2c3d4e5f6a7b",
  "name": "python3",
  "last_activity": "2025-02-27T10:30:00.000Z",
  "execution_state": "idle",
  "connections": 1
}
```

#### List Kernels

```http
GET /api/kernels HTTP/1.1
Host: localhost:8888
```

#### Delete Kernel

```http
DELETE /api/kernels/{kernel_id} HTTP/1.1
Host: localhost:8888
```

### WebSocket Protocol

#### Connection URL

```
ws://localhost:8888/api/kernels/{kernel_id}/channels
```

#### Message Format (Jupyter Messaging Protocol v5.0)

All messages follow this JSON structure:

```json
{
  "header": {
    "msg_id": "unique-uuid-v4",
    "username": "minewright",
    "session": "session-uuid",
    "msg_type": "execute_request",
    "version": "5.0",
    "date": "2025-02-27T10:30:00.000Z"
  },
  "parent_header": {},
  "metadata": {},
  "content": {
    "code": "print('Hello from Minecraft!')",
    "silent": false,
    "store_history": true,
    "user_expressions": {},
    "allow_stdin": false,
    "stop_on_error": true
  },
  "buffers": [],
  "channel": "shell"
}
```

#### Key Message Types

| Message Type | Direction | Purpose |
|--------------|-----------|---------|
| `execute_request` | Client → Kernel | Execute code |
| `execute_reply` | Kernel → Client | Execution result |
| `stream` | Kernel → Client | stdout/stderr output |
| `display_data` | Kernel → Client | Rich media output |
| `error` | Kernel → Client | Error information |
| `status` | Kernel → Client | Kernel busy/idle state |
| `complete_request` | Client → Kernel | Code completion |
| `inspect_request` | Client → Kernel | Object introspection |

---

## MCP Integration Possibilities

### Model Context Protocol Overview

**Model Context Protocol (MCP)** is an open protocol (2025) for connecting AI assistants to external data and tools. It provides:

- **Standardized tool discovery and execution**
- **Resource management with URI-based access**
- **Prompt template support**
- **Multiple transport options**: STDIO, SSE (HTTP), WebSocket

### Java MCP SDK

Official SDK maintained by VMware Tanzu's Spring AI team:

```xml
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>0.15.0</version>
</dependency>
```

### Spring AI Starters

```xml
<!-- Client Starters -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client-webflux</artifactId>
</dependency>

<!-- Server Starters -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
</dependency>
```

### MCP Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   LLM       │◄────┤  MCP Client │◄────┤  MCP Server │
│  (GPT-4)    │     │  (Java)     │     │ (Jupyter)   │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │                   │
    Prompts           Tools/Resources     Tool Execution
```

### MCP Tool Definition Example

```java
public class JupyterSimulationTool {

    public static McpServerFeatures.SyncToolSpecification simulateActionTool() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
            "object",
            Map.of(
                "action", String.class,
                "position", Map.of("x", Integer.class, "y", Integer.class, "z", Integer.class),
                "worldState", Map.class
            ),
            List.of("action", "position"),
            false, null, null
        );

        return new McpServerFeatures.SyncToolSpecification(
            new McpSchema.Tool(
                "simulate_minecraft_action",
                "Simulate Minecraft Action",
                "Simulates the outcome of a Minecraft action using Python world model",
                inputSchema,
                null, null, null
            ),
            (exchange, args) -> {
                String action = (String) args.get("action");
                Map<String, Object> position = (Map<String, Object>) args.get("position");
                Map<String, Object> worldState = (Map<String, Object>) args.get("worldState");

                // Call Jupyter kernel for simulation
                String result = callJupyterKernel(action, position, worldState);

                return McpSchema.CallToolResult.builder()
                    .content(List.of(new McpSchema.TextContent(result)))
                    .isError(false)
                    .build();
            }
        );
    }
}
```

### MCP + Jupyter Integration Strategy

1. **MCP Server wraps Jupyter Gateway** - Expose Jupyter as MCP tools
2. **MCP Client in MineWright** - Query Jupyter via standardized protocol
3. **Tool definitions**:
   - `simulate_action` - Predict action outcome
   - `calculate_path` - Compute optimal path
   - `estimate_resources` - Calculate material needs
   - `optimize_build` - Spatial arrangement optimization

---

## Java Client Implementation

### WebSocket Client Libraries Comparison

| Library | Pros | Cons | Use Case |
|---------|------|------|----------|
| **Tyrus** | JSR 356 standard, Spring integration | Higher memory | Enterprise apps |
| **Netty** | High performance, low latency | Complex API | High-concurrency |
| **Java-WebSocket** | Simple API, lightweight | Less features | Quick development |

### Recommended: Tyrus (JSR 356)

#### Maven Dependencies

```xml
<!-- For Minecraft Forge mod (shadowed) -->
<dependency>
    <groupId>org.glassfish.tyrus</groupId>
    <artifactId>tyrus-client</artifactId>
    <version>2.1.1</version>
</dependency>
<dependency>
    <groupId>org.glassfish.tyrus</groupId>
    <artifactId>tyrus-container-grizzly</artifactId>
    <version>2.1.1</version>
</dependency>

<!-- JSON processing -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

### Complete Jupyter Client Implementation

#### 1. Message Models

```java
package com.minewright.jupyter;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.*;

/**
 * Jupyter messaging protocol models (v5.0)
 */
public class JupyterMessages {

    public static class JupyterMessage {
        @SerializedName("header")
        private MessageHeader header;

        @SerializedName("parent_header")
        private MessageHeader parentHeader;

        @SerializedName("metadata")
        private Map<String, Object> metadata;

        @SerializedName("content")
        private Map<String, Object> content;

        @SerializedName("buffers")
        private List<byte[]> buffers;

        @SerializedName("channel")
        private String channel;

        // Getters, setters, builders
        public static Builder builder() {
            return new Builder();
        }

        public String toJson() {
            return new Gson().toJson(this);
        }

        public static JupyterMessage fromJson(String json) {
            return new Gson().fromJson(json, JupyterMessage.class);
        }

        public static class Builder {
            private final JupyterMessage message = new JupyterMessage();

            public Builder header(MessageHeader header) {
                message.header = header;
                return this;
            }

            public Builder parentHeader(MessageHeader parentHeader) {
                message.parentHeader = parentHeader;
                return this;
            }

            public Builder content(Map<String, Object> content) {
                message.content = content;
                return this;
            }

            public Builder channel(String channel) {
                message.channel = channel;
                return this;
            }

            public JupyterMessage build() {
                if (message.metadata == null) {
                    message.metadata = new HashMap<>();
                }
                if (message.buffers == null) {
                    message.buffers = new ArrayList<>();
                }
                return message;
            }
        }
    }

    public static class MessageHeader {
        @SerializedName("msg_id")
        private String msgId;

        @SerializedName("username")
        private String username;

        @SerializedName("session")
        private String session;

        @SerializedName("msg_type")
        private String msgType;

        @SerializedName("version")
        private String version;

        @SerializedName("date")
        private String date;

        // Constructor, getters, setters
        public MessageHeader(String msgType, String username, String session) {
            this.msgId = UUID.randomUUID().toString();
            this.msgType = msgType;
            this.username = username;
            this.session = session;
            this.version = "5.0";
            this.date = Instant.now().toString();
        }
    }

    public static class ExecuteRequest {
        public static Map<String, Object> create(String code) {
            Map<String, Object> content = new HashMap<>();
            content.put("code", code);
            content.put("silent", false);
            content.put("store_history", true);
            content.put("user_expressions", new HashMap<>());
            content.put("allow_stdin", false);
            content.put("stop_on_error", true);
            return content;
        }
    }
}
```

#### 2. Jupyter Kernel Gateway Client

```java
package com.minewright.jupyter;

import com.minewright.MineWrightMod;
import org.glassfish.tyrus.client.ClientManager;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Client for Jupyter Kernel Gateway REST API and WebSocket communication.
 * Handles kernel lifecycle and code execution with async/non-blocking design.
 */
public class JupyterKernelClient {

    private static final Logger LOGGER = MineWrightMod.LOGGER;

    private final String gatewayUrl;
    private final String kernelName;
    private String kernelId;
    private Session wsSession;
    private final ClientManager clientManager;
    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests;

    public JupyterKernelClient(String gatewayUrl, String kernelName) {
        this.gatewayUrl = gatewayUrl;
        this.kernelName = kernelName;
        this.clientManager = ClientManager.createClient();
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new kernel via REST API.
     * @return CompletableFuture that completes with kernel ID
     */
    public CompletableFuture<String> createKernel() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(gatewayUrl + "/api/kernels"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"name\": \"" + kernelName + "\"}"
                    ))
                    .build();

                HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 201) {
                    Gson gson = new Gson();
                    Map<String, Object> result = gson.fromJson(
                        response.body(),
                        new TypeToken<Map<String, Object>>(){}.getType()
                    );
                    this.kernelId = (String) result.get("id");
                    LOGGER.info("Created Jupyter kernel: {}", kernelId);
                    return kernelId;
                } else {
                    throw new IOException("Failed to create kernel: " + response.statusCode());
                }
            } catch (Exception e) {
                LOGGER.error("Error creating kernel", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Connects to kernel via WebSocket for code execution.
     * @return CompletableFuture that completes when connected
     */
    public CompletableFuture<Void> connectWebSocket() {
        if (kernelId == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Kernel not created")
            );
        }

        return CompletableFuture.runAsync(() -> {
            try {
                String wsUrl = gatewayUrl.replace("http", "ws")
                    + "/api/kernels/" + kernelId + "/channels";

                wsSession = clientManager.connectToServer(
                    new JupyterEndpoint(),
                    URI.create(wsUrl)
                );

                LOGGER.info("Connected to Jupyter kernel WebSocket: {}", wsUrl);
            } catch (Exception e) {
                LOGGER.error("Error connecting WebSocket", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Executes code in the kernel asynchronously.
     * @param code Python code to execute
     * @return CompletableFuture with execution result
     */
    public CompletableFuture<String> executeCode(String code) {
        if (wsSession == null || !wsSession.isOpen()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("WebSocket not connected")
            );
        }

        CompletableFuture<String> resultFuture = new CompletableFuture<>();
        String msgId = UUID.randomUUID().toString();
        pendingRequests.put(msgId, resultFuture);

        try {
            JupyterMessages.MessageHeader header = new JupyterMessages.MessageHeader(
                "execute_request",
                "minewright",
                "session-" + UUID.randomUUID()
            );

            JupyterMessage message = JupyterMessage.builder()
                .header(header)
                .content(JupyterMessages.ExecuteRequest.create(code))
                .channel("shell")
                .build();

            wsSession.getBasicRemote().sendText(message.toJson());
            LOGGER.debug("Sent execute_request: {}", code);

            // Timeout after 30 seconds
            CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS)
                .execute(() -> {
                    if (pendingRequests.containsKey(msgId)) {
                        pendingRequests.remove(msgId);
                        resultFuture.completeExceptionally(
                            new TimeoutException("Execution timeout")
                        );
                    }
                });

        } catch (IOException e) {
            pendingRequests.remove(msgId);
            resultFuture.completeExceptionally(e);
        }

        return resultFuture;
    }

    /**
     * WebSocket endpoint for receiving messages from kernel.
     */
    @ClientEndpoint
    public class JupyterEndpoint {

        @OnOpen
        public void onOpen(Session session) {
            LOGGER.info("WebSocket session opened");
        }

        @OnMessage
        public void onMessage(String message) {
            try {
                JupyterMessage msg = JupyterMessage.fromJson(message);
                String msgType = msg.header.msgType;
                String parentMsgId = msg.parentHeader.msgId;

                LOGGER.debug("Received message: {}", msgType);

                switch (msgType) {
                    case "stream":
                    case "execute_result":
                        // Successful execution
                        CompletableFuture<String> future = pendingRequests.get(parentMsgId);
                        if (future != null) {
                            String output = extractOutput(msg);
                            future.complete(output);
                            pendingRequests.remove(parentMsgId);
                        }
                        break;

                    case "error":
                        // Execution error
                        CompletableFuture<String> errorFuture = pendingRequests.get(parentMsgId);
                        if (errorFuture != null) {
                            String errorMsg = extractError(msg);
                            errorFuture.completeExceptionally(new Exception(errorMsg));
                            pendingRequests.remove(parentMsgId);
                        }
                        break;

                    case "status":
                        // Kernel status change (busy/idle)
                        break;
                }

            } catch (Exception e) {
                LOGGER.error("Error handling WebSocket message", e);
            }
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            LOGGER.info("WebSocket session closed: {}", closeReason);
            // Fail all pending requests
            pendingRequests.forEach((id, future) ->
                future.completeExceptionally(new IOException("WebSocket closed"))
            );
            pendingRequests.clear();
        }

        @OnError
        public void onError(Session session, Throwable thr) {
            LOGGER.error("WebSocket error", thr);
        }

        private String extractOutput(JupyterMessage msg) {
            Map<String, Object> content = msg.content;
            if (content.containsKey("text")) {
                return (String) content.get("text");
            }
            if (content.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) content.get("data");
                if (data.containsKey("text/plain")) {
                    return (String) data.get("text/plain");
                }
            }
            return "";
        }

        private String extractError(JupyterMessage msg) {
            Map<String, Object> content = msg.content;
            List<String> traceback = (List<String>) content.get("traceback");
            return String.join("\n", traceback);
        }
    }

    /**
     * Closes the WebSocket connection.
     */
    public void close() {
        if (wsSession != null && wsSession.isOpen()) {
            try {
                wsSession.close();
            } catch (IOException e) {
                LOGGER.error("Error closing WebSocket", e);
            }
        }
    }

    /**
     * Deletes the kernel via REST API.
     */
    public CompletableFuture<Void> deleteKernel() {
        return CompletableFuture.runAsync(() -> {
            if (kernelId == null) return;

            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(gatewayUrl + "/api/kernels/" + kernelId))
                    .DELETE()
                    .build();

                HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 204) {
                    LOGGER.info("Deleted Jupyter kernel: {}", kernelId);
                }
            } catch (Exception e) {
                LOGGER.error("Error deleting kernel", e);
            }
        });
    }
}
```

#### 3. Mental Simulation Service

```java
package com.minewright.jupyter;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.AgentState;
import net.minecraft.core.BlockPos;
import java.util.concurrent.CompletableFuture;

/**
 * Service for AI agent mental simulation using Jupyter kernels.
 * Provides "cognitive sandbox" for planning before execution.
 */
public class MentalSimulationService {

    private final JupyterKernelClient jupyterClient;
    private final ForemanEntity agent;

    // World model context
    private String worldModelCode = """
        import numpy as np
        from typing import List, Tuple, Dict, Optional
        from dataclasses import dataclass
        import json

        @dataclass
        class Block:
            type: str
            position: Tuple[int, int, int]

        @dataclass
        class Entity:
            type: str
            position: Tuple[int, int, int]
            health: int

        class MinecraftWorldModel:
            def __init__(self):
                self.blocks: List[Block] = []
                self.entities: List[Entity] = []
                self.agent_position = (0, 0, 0)

            def load_from_state(self, state: dict):
                '''Load world state from JSON'''
                self.agent_position = tuple(state['agent_position'])
                self.blocks = [Block(**b) for b in state.get('blocks', [])]
                self.entities = [Entity(**e) for e in state.get('entities', [])]

            def simulate_move(self, dx: int, dy: int, dz: int) -> dict:
                '''Simulate movement and return predicted state'''
                new_pos = (
                    self.agent_position[0] + dx,
                    self.agent_position[1] + dy,
                    self.agent_position[2] + dz
                )

                # Check for collisions
                collisions = [
                    b for b in self.blocks
                    if b.position == new_pos and b.type not in ['air', 'water']
                ]

                return {
                    'new_position': new_pos,
                    'is_blocked': len(collisions) > 0,
                    'collisions': [c.type for c in collisions]
                }

            def simulate_break_block(self, target_pos: Tuple[int, int, int]) -> dict:
                '''Simulate breaking a block'''
                block = next((b for b in self.blocks if b.position == target_pos), None)

                if block is None:
                    return {'success': False, 'reason': 'Block not found'}

                # Calculate drop
                drops = self._calculate_drops(block.type)

                return {
                    'success': True,
                    'block_type': block.type,
                    'drops': drops,
                    'duration': self._calculate_break_time(block.type)
                }

            def simulate_place_block(self, block_type: str, pos: Tuple[int, int, int]) -> dict:
                '''Simulate placing a block'''
                existing = next((b for b in self.blocks if b.position == pos), None)

                if existing is not None:
                    return {'success': False, 'reason': 'Position occupied'}

                return {
                    'success': True,
                    'block_type': block_type,
                    'position': pos
                }

            def optimize_build_sequence(self, target: dict) -> List[dict]:
                '''Optimize building sequence for efficiency'''
                # Greedy algorithm: build from bottom to top
                blocks = sorted(target['blocks'], key=lambda b: b[2])

                sequence = []
                agent_pos = self.agent_position

                for block in blocks:
                    # Calculate path to block position
                    path = self._find_path(agent_pos, (block[0], block[1]-1, block[2]))
                    sequence.append({
                        'action': 'move',
                        'path': path
                    })
                    sequence.append({
                        'action': 'place',
                        'block_type': block[3],
                        'position': (block[0], block[1], block[2])
                    })
                    agent_pos = (block[0], block[1]-1, block[2])

                return sequence

            def _calculate_drops(self, block_type: str) -> List[str]:
                '''Calculate drops from block'''
                drop_table = {
                    'stone': ['cobblestone'],
                    'coal_ore': ['coal'],
                    'iron_ore': ['raw_iron'],
                    'diamond_ore': ['diamond'],
                    'oak_log': ['oak_log', 'oak_log', 'oak_log', 'apple']
                }
                return drop_table.get(block_type, [])

            def _calculate_break_time(self, block_type: str) -> float:
                '''Calculate block break time in seconds'''
                hardness = {
                    'stone': 1.5,
                    'coal_ore': 3.0,
                    'iron_ore': 3.0,
                    'diamond_ore': 3.0,
                    'oak_log': 2.0
                }
                return hardness.get(block_type, 1.0)

            def _find_path(self, start, end):
                '''Simple pathfinding (placeholder for A*)'''
                # Manhattan distance path
                path = []
                dx, dy, dz = end[0]-start[0], end[1]-start[1], end[2]-start[2]

                for i in range(abs(dx)):
                    path.append((1 if dx > 0 else -1, 0, 0))
                for i in range(abs(dy)):
                    path.append((0, 1 if dy > 0 else -1, 0))
                for i in range(abs(dz)):
                    path.append((0, 0, 1 if dz > 0 else -1))

                return path

        # Initialize world model
        world = MinecraftWorldModel()
        """;

    public MentalSimulationService(ForemanEntity agent) {
        this.agent = agent;
        this.jupyterClient = new JupyterKernelClient(
            "http://localhost:8888",
            "python3"
        );

        // Initialize kernel with world model
        initializeWorldModel();
    }

    private void initializeWorldModel() {
        jupyterClient.createKernel()
            .thenCompose(id -> jupyterClient.connectWebSocket())
            .thenCompose(v -> jupyterClient.executeCode(worldModelCode))
            .thenAccept(result -> MineWrightMod.LOGGER.info("World model initialized"))
            .exceptionally(e -> {
                MineWrightMod.LOGGER.error("Failed to initialize world model", e);
                return null;
            });
    }

    /**
     * Simulates movement before executing.
     */
    public CompletableFuture<MovementPrediction> simulateMovement(
        BlockPos from, BlockPos to
    ) {
        String code = String.format("""
            world.load_from_state(%s)
            result = world.simulate_move(%d, %d, %d)
            print(json.dumps(result))
            """,
            getWorldStateJson(),
            to.getX() - from.getX(),
            to.getY() - from.getY(),
            to.getZ() - from.getZ()
        );

        return jupyterClient.executeCode(code)
            .thenApply(output -> {
                Gson gson = new Gson();
                return gson.fromJson(output, MovementPrediction.class);
            });
    }

    /**
     * Simulates block breaking.
     */
    public CompletableFuture<BlockBreakPrediction> simulateBreakBlock(BlockPos target) {
        String code = String.format("""
            world.load_from_state(%s)
            result = world.simulate_break_block((%d, %d, %d))
            print(json.dumps(result))
            """,
            getWorldStateJson(),
            target.getX(),
            target.getY(),
            target.getZ()
        );

        return jupyterClient.executeCode(code)
            .thenApply(output -> {
                Gson gson = new Gson();
                return gson.fromJson(output, BlockBreakPrediction.class);
            });
    }

    /**
     * Optimizes build sequence for efficiency.
     */
    public CompletableFuture<List<BuildAction>> optimizeBuildSequence(
        List<BlockPlacement> blocks
    ) {
        String code = String.format("""
            world.load_from_state(%s)
            target = {'blocks': %s}
            sequence = world.optimize_build_sequence(target)
            print(json.dumps(sequence))
            """,
            getWorldStateJson(),
            blocksToJson(blocks)
        );

        return jupyterClient.executeCode(code)
            .thenApply(output -> {
                Type listType = new TypeToken<List<BuildAction>>(){}.getType();
                return new Gson().fromJson(output, listType);
            });
    }

    /**
     * Gets current world state as JSON for kernel.
     */
    private String getWorldStateJson() {
        WorldKnowledge worldKnowledge = new WorldKnowledge(agent);
        Map<String, Object> state = new HashMap<>();

        state.put("agent_position", List.of(
            agent.blockPosition().getX(),
            agent.blockPosition().getY(),
            agent.blockPosition().getZ()
        ));

        state.put("blocks", serializeBlocks(worldKnowledge));
        state.put("entities", serializeEntities(worldKnowledge));
        state.put("biome", worldKnowledge.getBiomeName());

        return new Gson().toJson(state);
    }

    private List<Map<String, Object>> serializeBlocks(WorldKnowledge wk) {
        // Convert nearby blocks to JSON structure
        // Implementation details...
        return List.of();
    }

    private List<Map<String, Object>> serializeEntities(WorldKnowledge wk) {
        // Convert nearby entities to JSON structure
        // Implementation details...
        return List.of();
    }

    public void shutdown() {
        jupyterClient.close();
        jupyterClient.deleteKernel();
    }
}

// Prediction result models
class MovementPrediction {
    int[] newPosition;
    boolean isBlocked;
    List<String> collisions;
}

class BlockBreakPrediction {
    boolean success;
    String reason;
    String blockType;
    List<String> drops;
    double duration;
}

class BuildAction {
    String action; // "move" or "place"
    Object data;
}
```

---

## Mental Simulation Concepts

### What is Mental Simulation?

**Mental simulation** is the cognitive process of imagining scenarios and predicting outcomes without physically executing them. In AI agents, this enables:

1. **Predictive Planning** - "What will happen if I do X?"
2. **Counterfactual Reasoning** - "What would have happened if I did Y instead?"
3. **Risk Assessment** - "Is this action safe?"
4. **Efficiency Optimization** - "What's the best sequence of actions?"

### World Models

A **world model** is an internal representation of environment dynamics that allows prediction:

```
Current State + Action → World Model → Predicted Next State
```

### Types of World Models

| Type | Description | Example |
|------|-------------|---------|
| **Implicit** | Neural network weights capture dynamics | DreamerV3, MuZero |
| **Explicit** | Dedicated simulator component | Physics engines |
| **LLM-based** | Language model as environment simulator | DeepSeek R1 |

### Mental Simulation in AI (2025 State of the Art)

#### 1. DeepSeek R1 (January 2025)

- **"Mental Theater"** - Internal chain-of-thought before responding
- Models can role-play both agent and environment
- Warning: Can "overthink" - excessive planning without action

#### 2. DreamerV3

- Learn world model from environment interaction
- Imagination-based planning in latent space
- Achieves SOTA in Atari, Minecraft, etc.

#### 3. Genie 2

- Video generation as world model
- Predicts future frames from current state + action

### Mental Simulation for Minecraft AI

#### Why Minecraft?

- **Discrete state space** - Easier to model than continuous physics
- **Clear causal relationships** - Breaking block → drops
- **Multi-step planning** - Building requires sequence optimization
- **Spatial reasoning** - 3D geometry for structures

#### Use Cases

1. **Pathfinding Validation**
   ```python
   # Simulate before walking
   if simulate_move(target).is_blocked:
       find_alternative_path()
   ```

2. **Resource Estimation**
   ```python
   # Calculate materials before building
   required = estimate_blocks(structure)
   available = count_blocks_in_inventory()
   if required > available:
       gather_resources(required - available)
   ```

3. **Build Optimization**
   ```python
   # Find most efficient build order
   sequence = optimize_build_sequence(target)
   for action in sequence:
       execute(action)
   ```

4. **Combat Prediction**
   ```python
   # Simulate enemy behavior
   enemy_actions = predict_enemy_moves()
   counter_strategy = find_counter(enemy_actions)
   ```

---

## Integration with MineWright

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     MineWright Mod                          │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────┐    ┌──────────────┐    ┌─────────────┐   │
│  │ ForemanEntity│───▶│ TaskPlanner  │───▶│  LLM Client │   │
│  │   (Agent)   │    │              │    │  (OpenAI)   │   │
│  └─────────────┘    └──────────────┘    └─────────────┘   │
│         │                                                   │
│         │                                                  │
│         ▼                                                   │
│  ┌──────────────────────────────────────────────────────┐ │
│  │         Mental Simulation Layer (NEW)                │ │
│  │  ┌─────────────────────────────────────────────┐    │ │
│  │  │  JupyterKernelClient                        │    │ │
│  │  │  - REST API (kernel lifecycle)              │    │ │
│  │  │  - WebSocket (code execution)               │    │ │
│  │  └─────────────────────────────────────────────┘    │ │
│  │                        │                             │ │
│  │                        ▼                             │ │
│  │  ┌─────────────────────────────────────────────┐    │ │
│  │  │  MentalSimulationService                    │    │ │
│  │  │  - simulateMovement()                       │    │ │
│  │  │  - simulateBreakBlock()                     │    │ │
│  │  │  - optimizeBuildSequence()                  │    │ │
│  │  │  - estimateResources()                      │    │ │
│  │  └─────────────────────────────────────────────┘    │ │
│  └──────────────────────────────────────────────────────┘ │
│                              │                              │
│                              ▼                              │
│  ┌──────────────────────────────────────────────────────┐ │
│  │         Jupyter Kernel Gateway (localhost:8888)      │ │
│  │                                                        │ │
│  │  ┌─────────────────────────────────────────────┐    │ │
│  │  │         IPython Kernel                      │    │ │
│  │  │  ┌─────────────────────────────────────┐   │    │ │
│  │  │  │  MinecraftWorldModel                │   │    │ │
│  │  │  │  - simulate_move()                  │   │    │ │
│  │  │  │  - simulate_break_block()           │   │    │ │
│  │  │  │  - optimize_build_sequence()        │   │    │ │
│  │  │  │  - calculate_path()                 │   │    │ │
│  │  │  └─────────────────────────────────────┘   │    │ │
│  │  └─────────────────────────────────────────────┘    │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Integration Points

#### 1. Task Planning Enhancement

```java
// Modified TaskPlanner with mental simulation
public class TaskPlanner {

    private MentalSimulationService mentalSim;

    public CompletableFuture<ParsedResponse> planTasksAsync(
        ForemanEntity foreman,
        String command
    ) {
        // Step 1: Get initial plan from LLM
        return llmClient.sendAsync(buildPrompt(foreman, command))
            .thenCompose(parsed -> {
                // Step 2: Use mental simulation to validate/optimize
                return mentalSim.validateAndOptimizePlan(
                    foreman,
                    parsed.getTasks()
                );
            });
    }
}
```

#### 2. Action Execution with Simulation

```java
// Enhanced BaseAction with pre-execution simulation
public abstract class BaseAction {

    protected MentalSimulationService mentalSim;

    @Override
    public void tick() {
        // Before executing, simulate outcome
        if (!hasSimulated) {
            mentalSim.simulate(this)
                .thenAccept(prediction -> {
                    if (prediction.isSafe && prediction.isEfficient) {
                        executeAction();
                    } else if (prediction.hasBetterAlternative) {
                        switchToAlternative(prediction.alternative);
                    }
                });
            hasSimulated = true;
        }

        // Continue execution...
    }
}
```

#### 3. State Machine Integration

```java
// Add MENTAL_SIMULATION state to AgentStateMachine
public enum AgentState {
    IDLE,
    PLANNING,
    MENTAL_SIMULATION,  // NEW
    EXECUTING,
    PAUSED,
    COMPLETED,
    FAILED
}
```

#### 4. Configuration

```toml
# config/steve-common.toml

[jupyter]
enabled = true
gateway_url = "http://localhost:8888"
kernel_name = "python3"
timeout_ms = 30000

[mental_simulation]
enabled = true
simulation_depth = 3  # How many steps to simulate
cache_predictions = true
```

### Benefits for MineWright

| Feature | Current | With Jupyter Integration |
|---------|---------|--------------------------|
| **Planning** | LLM only | LLM + simulation validation |
| **Pathfinding** | Vanilla Minecraft AI | A* with world model |
| **Building** | Sequential | Optimized sequence |
| **Resource Management** | Reactive | Predictive estimation |
| **Multi-agent** | Independent | Coordinated via game theory |

---

## Pros/Cons Analysis

### Advantages

#### 1. **Enhanced Decision Making**
- ✅ Predictive planning reduces failed actions
- ✅ Risk assessment before dangerous actions
- ✅ Counterfactual analysis for learning

#### 2. **Optimization**
- ✅ Compute-intensive algorithms in Python (NumPy, SciPy)
- ✅ Optimal build sequences reduce time
- ✅ Resource estimation prevents shortages

#### 3. **Cognitive Realism**
- ✅ Mirrors human mental simulation
- ✅ Enables "what-if" reasoning
- ✅ Supports explanation of decisions

#### 4. **Extensibility**
- ✅ Easy to add new simulation functions
- ✅ Leverage Python ecosystem (pandas, networkx, etc.)
- ✅ Notebook-based debugging/visualization

#### 5. **Separation of Concerns**
- ✅ Minecraft logic in Java
- ✅ Complex algorithms in Python
- ✅ Clean interface via WebSocket

### Disadvantages

#### 1. **Complexity**
- ❌ Additional service to deploy (JKG)
- ❌ Network communication overhead
- ❌ Error handling for network failures

#### 2. **Performance**
- ❌ WebSocket latency (~10-50ms)
- ❌ Python execution slower than native Java
- ❌ Serialization overhead (JSON)

#### 3. **Dependencies**
- ❌ Requires Python + Jupyter installation
- ❌ Requires kernel gateway running
- ❌ Additional WebSocket library dependencies

#### 4. **Debugging Difficulty**
- ❌ Errors span Java → WebSocket → Python
- ❌ Asynchronous flow harder to trace
- ❌ State synchronization challenges

#### 5. **Resource Usage**
- ❌ Additional memory for Jupyter process
- ❌ Background Python processes
- ❌ Potentially multiple kernels per agent

### Risk Assessment

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Network failure | High | Medium | Fallback to no simulation |
| JKG not running | High | Medium | Startup check + user error |
| Performance degradation | Medium | High | Cache results, limit depth |
| Memory leak | Medium | Low | Kernel lifecycle management |
| Python errors | Low | High | Try/catch, fallback behavior |

### Comparison with Alternatives

| Approach | Pros | Cons | Recommendation |
|----------|------|------|----------------|
| **Jupyter Integration** | Rich algorithms, debuggable | Network overhead | **For complex planning** |
| **GraalVM JS** | In-process, fast | Limited libraries | Current approach |
| **Native Java** | No dependencies | More code to write | For simple algorithms |
| **External Service** | Scalable | Network latency | For multi-server |

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Goal:** Basic Jupyter integration working

1. **Dependencies**
   - [ ] Add Tyrus WebSocket client to `build.gradle`
   - [ ] Add Gson for JSON serialization
   - [ ] Configure shadow plugin for relocation

2. **Core Classes**
   - [ ] `JupyterMessages` - Protocol models
   - [ ] `JupyterKernelClient` - REST + WebSocket client
   - [ ] `MentalSimulationService` - High-level API

3. **Testing**
   - [ ] Unit tests for message serialization
   - [ ] Integration test with local JKG
   - [ ] Error handling tests

### Phase 2: World Model (Week 3-4)

**Goal:** Python world model for Minecraft

1. **Python Code**
   - [ ] `MinecraftWorldModel` class
   - [ ] Block, Entity dataclasses
   - [ ] Core simulation functions

2. **Java Integration**
   - [ ] World state serialization
   - [ ] Result deserialization
   - [ ] Error handling

3. **Validation**
   - [ ] Compare simulation vs reality
   - [ ] Calibrate predictions
   - [ ] Performance benchmarks

### Phase 3: Task Integration (Week 5-6)

**Goal:** Use simulation in planning

1. **Task Planner**
   - [ ] Add simulation step
   - [ ] Validate plans
   - [ ] Optimize sequences

2. **Action System**
   - [ ] Pre-execution simulation
   - [ ] Dynamic adjustment
   - [ ] Learning from outcomes

3. **State Machine**
   - [ ] Add MENTAL_SIMULATION state
   - [ ] State transitions
   - [ ] Timeout handling

### Phase 4: Advanced Features (Week 7-8)

**Goal:** Advanced mental simulation

1. **Multi-Agent**
   - [ ] Shared world model
   - [ ] Conflict resolution
   - [ ] Cooperative planning

2. **Learning**
   - [ ] Record prediction vs actual
   - [ ] Update world model
   - [ ] Improve accuracy

3. **UI/UX**
   - [ ] Debug visualization
   - [ ] Simulation inspector
   - [ ] Performance metrics

### Phase 5: Production Hardening (Week 9-10)

**Goal:** Production-ready

1. **Error Handling**
   - [ ] Fallback strategies
   - [ ] Retry logic
   - [ ] Graceful degradation

2. **Performance**
   - [ ] Result caching
   - [ ] Kernel pooling
   - [ ] Async optimization

3. **Documentation**
   - [ ] Architecture docs
   - [ ] API reference
   - [ ] User guide

---

## References

### Jupyter Kernel Gateway

1. [Jupyter Kernel Gateway API Introduction](https://m.blog.csdn.net/weixin_35045970/article/details/156441553) - CSDN Blog, December 2025
2. [Jupyter Kernel Architecture Deep Dive](https://blog.csdn.net/cuda7parallel/article/details/149550602) - CSDN Blog, July 2025
3. [Jupyter Messaging Protocol](https://m.blog.csdn.net/u011091936/article/details/151350875) - Source code analysis, September 2025

### Model Context Protocol

4. [MCP Ecosystem Overview](https://www.cnblogs.com/xtkyxnx/p/19314065) - January 2025
5. [MCP Java SDK Guide](https://m.blog.csdn.net/gitblog_00391/article/details/152063481) - CSDN Blog, 2025
6. [MCP Java Integration Tutorial](https://m.blog.csdn.net/2302_79380280/article/details/153124301) - CSDN Blog, 2025
7. [Spring AI MCP Reference](https://docs.springframework.org.cn/spring-ai/reference/api/mcp/mcp-overview.html) - Official Documentation

### WebSocket Libraries

8. [Java WebSocket Libraries 2025](https://blog.csdn.net/pea55/article/details/152353337) - Blurr library comparison
9. [Tyrus Documentation](https://eclipse-ee4j.github.io/tyrus/) - Official JSR 356 implementation

### Mental Simulation & AI

10. [Cognitive Architecture for AI Agents](https://m.blog.csdn.net/2501_91483145/article/details/155544360) - CSDN Blog, December 2025
11. [Li Hongyi: AI Agent Principles](https://m.bilibili.com/opus/1070960334475362309) - Bilibili, March 2025
12. [World Models & Predictive Simulation](https://blog.51cto.com/u_13272819/13862906) - 51CTO Blog, April 2025
13. [World Models in AI Agents](https://m.blog.csdn.net/2401_85375151/article/details/148265191) - CSDN Blog, May 2025

### Additional Resources

14. [Jupyter Client Library](https://jupyter-client.readthedocs.io/) - Official Python client
15. [ZeroMQ Documentation](https://zeromq.org/) - Messaging library
16. [IJava - Java Kernel for Jupyter](https://gitcode.com/gh_mirrors/ij/IJava) - Java kernel implementation

---

## Appendix A: Complete Example

```java
// Example: Simulated building action
package com.minewright.action.actions;

import com.minewright.action.BaseAction;
import com.minewright.entity.ForemanEntity;
import com.minewright.jupyter.MentalSimulationService;
import com.minewright.jupyter.BuildAction;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SimulatedBuildAction extends BaseAction {

    private final MentalSimulationService mentalSim;
    private final List<BlockPlacement> blocks;
    private List<BuildAction> optimizedSequence;
    private int currentStep = 0;
    private boolean hasSimulated = false;

    public SimulatedBuildAction(
        ForemanEntity foreman,
        List<BlockPlacement> blocks
    ) {
        super(foreman);
        this.blocks = blocks;
        this.mentalSim = new MentalSimulationService(foreman);
    }

    @Override
    public void tick() {
        // Phase 1: Mental simulation
        if (!hasSimulated) {
            simulateAndOptimize();
            hasSimulated = true;
            return;
        }

        // Phase 2: Execute optimized sequence
        if (currentStep < optimizedSequence.size()) {
            BuildAction action = optimizedSequence.get(currentStep);
            executeStep(action);
            currentStep++;
        } else {
            complete();
        }
    }

    private void simulateAndOptimize() {
        mentalSim.optimizeBuildSequence(blocks)
            .thenAccept(sequence -> {
                this.optimizedSequence = sequence;
                foreman.getLogger().info(
                    "Optimized build sequence: {} steps (was {})",
                    sequence.size(),
                    blocks.size()
                );
            })
            .exceptionally(e -> {
                foreman.getLogger().error("Simulation failed, using original sequence", e);
                this.optimizedSequence = createFallbackSequence();
                return null;
            });
    }

    private void executeStep(BuildAction action) {
        switch (action.action) {
            case "move" -> moveTo((int[]) action.data);
            case "place" -> placeBlock((BlockPlacement) action.data);
        }
    }

    private List<BuildAction> createFallbackSequence() {
        // Direct placement without optimization
        return blocks.stream()
            .map(block -> new BuildAction("place", block))
            .toList();
    }

    @Override
    public void onCancel() {
        mentalSim.shutdown();
    }

    @Override
    public boolean isComplete() {
        return hasSimulated && currentStep >= optimizedSequence.size();
    }
}
```

---

## Appendix B: Configuration Example

```toml
# config/steve-common.toml

# Jupyter Kernel Gateway Configuration
[jupyter]
# Enable or disable Jupyter integration
enabled = true

# URL of the Jupyter Kernel Gateway
gateway_url = "http://localhost:8888"

# Kernel name (python3, ir, etc.)
kernel_name = "python3"

# Request timeout in milliseconds
timeout_ms = 30000

# Maximum number of retries for failed requests
max_retries = 3

# Connection pool size
max_connections = 5

# Mental Simulation Configuration
[mental_simulation]
# Enable or disable mental simulation
enabled = true

# How many steps ahead to simulate
simulation_depth = 3

# Cache simulation results
cache_predictions = true

# Cache size
cache_size = 1000

# Time-to-live for cache entries (seconds)
cache_ttl_seconds = 300

# Use simulation for validation
validate_actions = true

# Use simulation for optimization
optimize_sequences = true

# Minimum complexity threshold for simulation
min_actions_for_simulation = 3

# Logging Configuration
[mental_simulation.logging]
# Log simulation requests
log_requests = true

# Log simulation results
log_results = true

# Log performance metrics
log_performance = true
```

---

**End of Document**

This research document provides a comprehensive foundation for integrating Jupyter Kernel Gateway with MineWright for AI agent mental simulation. The proposed architecture leverages Python's rich ecosystem for complex algorithms while maintaining Minecraft mod logic in Java. The incremental implementation roadmap allows for phased development and validation.
