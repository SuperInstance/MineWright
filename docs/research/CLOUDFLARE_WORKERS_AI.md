# Cloudflare Workers AI for Minecraft Agent Tactical Decisions

**Research Date:** February 27, 2025
**Document Version:** 1.0
**Target Application:** MineWright AI - Autonomous Minecraft Agents
**Focus:** Sub-100ms tactical decision-making for real-time gameplay

---

## Executive Summary

Cloudflare Workers AI is a serverless AI inference platform running on Cloudflare's global edge network (300+ locations). For Minecraft agent tactical decisions, Workers AI offers:

- **Latency:** 80-100ms average for warm requests, sub-5ms cold starts for inference
- **Cost:** 10,000 free Neurons/day, then $0.011/1,000 Neurons (60-70% cheaper than GPT-3.5)
- **Models:** Llama 3.1 8B Instruct (131K context), Mistral 7B, Phi-3 Mini, and specialized classification models
- **Key Advantage:** Global edge deployment with automatic routing to nearest GPU node

**Verdict for Minecraft Tactical AI:** Suitable for strategic decisions (combat planning, path selection) but **too slow for tick-by-tick actions**. Best used for high-level tactical guidance with local caching.

---

## Table of Contents

1. [Workers AI Models Available (2025)](#1-workers-ai-models-available-2025)
2. [Latency Characteristics](#2-latency-characteristics)
3. [Python Workers Support](#3-python-workers-support)
4. [AI Binding Configuration](#4-ai-binding-configuration)
5. [Use Cases for Minecraft Tactical AI](#5-use-cases-for-minecraft-tactical-ai)
6. [Code Examples](#6-code-examples)
7. [Integration Strategy for MineWright AI](#7-integration-strategy-for-steve-ai)
8. [References and Sources](#8-references-and-sources)

---

## 1. Workers AI Models Available (2025)

### 1.1 Text Generation Models (LLMs)

| Model | Model ID | Context Window | Parameters | Best For |
|-------|----------|----------------|------------|----------|
| **Llama 3.1 8B Instruct** | `@cf/meta/llama-3.1-8b-instruct` | 131,000 tokens | 8B | Tactical planning, reasoning |
| **Llama 3 8B Instruct** | `@cf/meta/llama-3-8b-instruct` | Standard | 8B | General tactical decisions |
| **Mistral 7B Instruct** | `@cf/mistral/mistral-7b-instruct-v0.1` | 32,000 tokens | 7B | Combat strategies |
| **Phi-3 Mini** | `@cf/microsoft/phi-3-mini` | Compact | 3.8B | Quick decisions, low latency |

**Most Popular Model (2025):** `llama-3-8b-instruct` accounts for **36.3%** of all text generation tasks on Workers AI.

### 1.2 Text Classification Models

| Model | Model ID | Use Case | Price |
|-------|----------|----------|-------|
| **DistilBERT SST-2** | `@cf/huggingface/distilbert-sst-2-int8` | Sentiment analysis, binary classification | $2.51/M classifications |
| **Text Embeddings** | `@cf/baai/bge-m3` | Semantic similarity, threat assessment | Pricing varies |

### 1.3 Image Classification Models

| Model | Model ID | Use Case | Price |
|-------|----------|----------|-------|
| **ResNet-50** | `@cf/microsoft/resnet-50` | Screenshot analysis, hazard detection | $2.51/M images |

### 1.4 Token Limits and Pricing

| Tier | Free | Paid |
|------|------|------|
| **Daily Quota** | 10,000 Neurons/day | $0.011 per 1,000 Neurons |
| **Reset** | Daily at UTC 00:00 | Pay-as-you-go |
| **Rate Limit** | ~300 requests/minute for LLMs | Scales with usage |

**Cost Comparison:**
- 60-70% cheaper than OpenAI GPT-3.5
- 90%+ cheaper than GPT-4
- Approximately $0.00001 per simple tactical decision

**Neuron Pricing Examples:**
- Simple text generation: ~100-500 neurons
- Complex reasoning with context: ~500-2000 neurons
- Image classification: ~1000-5000 neurons

---

## 2. Latency Characteristics

### 2.1 Cold Start vs Warm Request Performance

| Metric | Cold Start | Warm Request |
|--------|------------|--------------|
| **Standard Workers AI** | ~10 seconds | 80-100ms |
| **Container-based Workers** | 60-120 seconds | 100-300ms |
| **Edge Network Advantage** | Routes to nearest GPU node | Automatic geographic optimization |

### 2.2 Global Edge Network Distribution

- **Total Locations:** 300+ edge locations globally
- **AI Inference Nodes:** 200+ locations with GPU acceleration
- **Automatic Routing:** Requests route to nearest GPU-equipped location
- **Average Latency:** Reduced from ~300ms (traditional cloud) to ~45ms with Workers AI

### 2.3 Comparison to Local Inference

| Solution | Setup Cost | Latency | Scalability |
|----------|------------|---------|-------------|
| **Workers AI** | $0 (free tier) | 80-100ms | Global auto-scale |
| **Local Llama 8B** | GPU hardware (~$1000+) | 20-50ms | Limited to local machine |
| **Cloud GPU (AWS/GCP)** | High hourly rates | 50-150ms + network | Manual scaling |
| **Local CPU Inference** | $0 | 500-2000ms | Limited to local machine |

### 2.4 Latency Optimization Strategies

1. **Keep-Alive Pings:** Send requests every 5 minutes to prevent cold starts
2. **Warm-up Scripts:** Pre-trigger initialization before gameplay sessions
3. **Caching:** Store common tactical decisions locally
4. **Batch Requests:** Combine multiple decisions into single API call
5. **Model Selection:** Use smaller models (Phi-3 Mini) for faster responses

**Critical Finding:** Workers AI cannot achieve true **sub-10ms latency** required for tick-by-tick game loop decisions. Minecraft runs at 20 TPS (ticks per second), leaving only 50ms per tick. After accounting for game logic overhead (< 5ms budget), network latency alone exceeds the available time.

---

## 3. Python Workers Support

### 3.1 Python Workflows (Beta - 2025)

Cloudflare introduced **Python Workflows** in 2025, built on Workers and Durable Objects:

- **Entry Point:** `WorkflowEntrypoint` class
- **Key Features:** Step retries, waiting, event-driven execution, concurrency, state persistence
- **Best For:** AI/ML pipelines, data engineering, agent workflows
- **Limitations:** Not native Python - runs on Pyodide (Python in WebAssembly)

### 3.2 Basic Python Worker Syntax

```python
# Basic Python Worker
from workers import Response

def on_fetch(request):
    return Response("Hello World!")
```

### 3.3 Async Python Worker Example

```python
from workers import Response
import json

async def on_fetch(request):
    data = await request.json()
    name = data.get("name", "Steve")

    return Response.json({
        "message": f"Hello, {name}!"
    })
```

### 3.4 Workers AI in Python

```python
from workers import Response

async def on_fetch(request, env):
    # Access AI binding from environment
    input = {
        "prompt": "What should a Minecraft agent do when encountering a zombie?"
    }

    response = await env.AI.run(
        "@cf/meta/llama-3.1-8b-instruct",
        input
    )

    return Response.json(response)
```

### 3.5 Python Worker Limitations

| Limitation | Details |
|------------|---------|
| **V8 Architecture** | Workers run on V8, not native Python runtime |
| **No PyTorch/TensorFlow** | ML libraries must use ONNX or WASM exports |
| **Memory Limit** | 128MB per instance |
| **CPU Time** | 10ms (free tier), 30ms (paid) per request |
| **Script Size** | Max 10MB uncompressed |

**Workaround for ML:**
- Use `ms-swift` framework to export models to ONNX/WebAssembly
- Upload models to Cloudflare R2 storage
- Use Workers for orchestration, heavy inference elsewhere

---

## 4. AI Binding Configuration

### 4.1 Basic wrangler.toml Configuration

```toml
name = "minecraft-tactical-ai"
main = "src/index.js"
compatibility_date = "2025-02-27"

[ai]
binding = "AI"
```

### 4.2 Full wrangler.toml Example

```toml
name = "steve-tactical-worker"
main = "src/index.ts"
compatibility_date = "2025-02-27"

# AI Binding
[ai]
binding = "AI"

# Environment Variables (secrets)
[vars]
ENVIRONMENT = "production"
MODEL_ID = "@cf/meta/llama-3.1-8b-instruct"

# KV Namespace for caching decision cache
[[kv_namespaces]]
binding = "DECISION_CACHE"
id = "your_kv_namespace_id"

# R2 Storage for model artifacts
[[r2_buckets]]
binding = "MODEL_STORAGE"
bucket_name = "steve-models"
```

### 4.3 TypeScript Environment Types

```typescript
interface Env {
  // AI binding
  AI: Ai;

  // KV for caching decisions
  DECISION_CACHE: KVNamespace;

  // R2 for model storage
  MODEL_STORAGE: R2Bucket;

  // Environment variables
  ENVIRONMENT: string;
  MODEL_ID: string;
}
```

### 4.4 Model Invocation Patterns

### Pattern 1: Simple Text Generation

```typescript
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const input = {
      prompt: "Analyze this combat situation and recommend action..."
    };

    const response = await env.AI.run(
      "@cf/meta/llama-3.1-8b-instruct",
      input
    );

    return Response.json(response);
  }
};
```

### Pattern 2: Chat Completions (OpenAI-Compatible)

```typescript
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const messages = [
      { role: "system", content: "You are a Minecraft tactical advisor." },
      { role: "user", content: "Should I fight or flee from this zombie?" }
    ];

    const response = await env.AI.run(
      "@cf/meta/llama-3.1-8b-instruct",
      { messages }
    );

    return Response.json(response);
  }
};
```

### Pattern 3: With AI Gateway

```typescript
const response = await env.AI.run(
  "@cf/meta/llama-3.1-8b-instruct",
  { prompt: "tactical advice..." },
  {
    gateway: {
      id: "my-gateway"
    }
  }
);
```

### Pattern 4: Streaming Responses

```typescript
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const stream = await env.AI.run(
      "@cf/meta/llama-3.1-8b-instruct",
      { prompt: "analyze situation..." },
      { stream: true }
    );

    return new Response(
      new ReadableStream({
        async start(controller) {
          for await (const chunk of stream) {
            controller.enqueue(chunk);
          }
          controller.close();
        }
      })
    );
  }
};
```

---

## 5. Use Cases for Minecraft Tactical AI

### 5.1 Combat Decision Trees

**Scenario:** Agent encounters hostile mob

**Decision Flow:**
```
1. Assess threat level (health, equipment, mob type)
2. Query Workers AI for tactical recommendation
3. Receive structured response: { action: "attack", confidence: 0.85 }
4. Cache decision for 10 seconds
5. Execute action locally
```

**Prompt Template:**
```
You are a Minecraft combat advisor. Given the following situation:

Agent State:
- Health: 15/20 hearts
- Equipment: Iron sword, leather armor
- Hunger: 8/20

Threat:
- Mob type: Zombie
- Distance: 8 blocks
- Environment: Daytime, open plains

Recommend one action: ATTACK, FLEE, or STRAFE.
Respond with JSON: {"action": "ATTACK", "confidence": 0.85, "reason": "..."}
```

**Latency Budget:**
- Network RTT: ~40ms
- Inference: ~40ms
- Total: ~80ms (acceptable for tactical decisions, not per-tick)

### 5.2 Hazard Detection

**Scenario:** Agent needs to detect environmental dangers

**Decision Types:**
- Lava proximity
- Cliff edges
- Hostile mob territories
- Suffocation hazards

**Prompt Template:**
```
Analyze this Minecraft location for hazards:

Position: (100, 64, -200)
Nearby blocks:
- (100, 63, -200): Lava
- (100, 64, -199): Air
- (100, 64, -200): Agent
- (101, 64, -200): Stone

Hazards detected: [{"type": "lava", "distance": 1, "severity": "high"}]
```

**Use Local Image Classification:**
- Capture screenshot of agent's view
- Send to ResNet-50 model via Workers AI
- Classify hazards (lava, fire, cliffs)

### 5.3 Quick Pathfinding Adjustments

**Scenario:** Agent encounters obstacle during navigation

**Decision Flow:**
```
1. Original path blocked by water/lava
2. Query Workers AI for alternative
3. Receive waypoints: [(100, 64, -200), (105, 64, -200)]
4. Update local A* pathfinder
5. Execute locally
```

**Prompt Template:**
```
Current position: (100, 64, -200)
Target: (150, 64, -150)
Obstacle: Water at (110, 64, -180)
Available blocks: Stone, Cobblestone, Dirt

Suggest 3-5 alternative waypoints to reach target safely.
Format: [{"x": 105, "y": 64, "z": -200}, ...]
```

### 5.4 Resource Prioritization

**Scenario:** Agent needs to decide what to mine/craft

**Decision Flow:**
```
1. Current inventory: 10 Iron Ore, 5 Coal
2. Goal: Craft Iron Pickaxe
3. Query Workers AI for priority
4. Receive ranking: [Mine Coal, Smelt Iron, Craft Pickaxe]
5. Execute in order
```

**Prompt Template:**
```
Agent State:
- Inventory: 10 Iron Ore, 5 Coal, 20 Stone
- Equipment: Stone Pickaxe
- Goal: Upgrade to Iron tools

Current Task: Mining
Nearby Resources:
- Coal: 15 blocks away
- Iron: 25 blocks away
- Diamonds: 50 blocks away (requires Iron pickaxe)

Prioritize next 3 actions:
1. Mine Coal
2. Smelt Iron Ore
3. Craft Iron Pickaxe

Explain reasoning briefly.
```

---

## 6. Code Examples

### 6.1 Basic Tactical Decision Worker

```typescript
// src/tactical-worker.ts

interface TacticalDecision {
  action: "ATTACK" | "FLEE" | "STRAFE" | "HOLD";
  confidence: number;
  reason: string;
}

interface CombatSituation {
  agentHealth: number;
  agentMaxHealth: number;
  weapon: string;
  armor: string;
  mobType: string;
  mobDistance: number;
  mobHealth: number;
  environment: string;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method !== "POST") {
      return new Response("Method not allowed", { status: 405 });
    }

    const situation: CombatSituation = await request.json();

    // Check cache first
    const cacheKey = `combat:${JSON.stringify(situation)}`;
    const cached = await env.DECISION_CACHE.get(cacheKey, "json");
    if (cached) {
      return Response.json(cached);
    }

    // Build prompt
    const prompt = buildCombatPrompt(situation);

    // Query Workers AI
    const aiResponse = await env.AI.run(
      "@cf/meta/llama-3.1-8b-instruct",
      {
        messages: [
          {
            role: "system",
            content: "You are a Minecraft combat expert. Analyze situations and recommend actions. Respond ONLY with valid JSON: {\"action\": \"ACTION\", \"confidence\": 0.85, \"reason\": \"brief explanation\"}"
          },
          {
            role: "user",
            content: prompt
          }
        ],
        temperature: 0.3, // Lower temperature for consistent decisions
        max_tokens: 100
      }
    );

    // Parse response
    const decision: TacticalDecision = parseDecision(aiResponse);

    // Cache for 10 seconds
    await env.DECISION_CACHE.put(
      cacheKey,
      JSON.stringify(decision),
      { expirationTtl: 10 }
    );

    return Response.json(decision);
  }
};

function buildCombatPrompt(situation: CombatSituation): string {
  return `
Analyze this combat situation:

AGENT:
- Health: ${situation.agentHealth}/${situation.agentMaxHealth}
- Weapon: ${situation.weapon}
- Armor: ${situation.armor}

THREAT:
- Mob: ${situation.mobType}
- Distance: ${situation.mobDistance} blocks
- Health: ${situation.mobHealth}

ENVIRONMENT: ${situation.environment}

Recommend action: ATTACK, FLEE, STRAFE, or HOLD.
Format: {"action": "ACTION", "confidence": 0.0-1.0, "reason": "why"}
`;
}

function parseDecision(response: any): TacticalDecision {
  try {
    const text = response.response || response;
    const jsonMatch = text.match(/\{[^}]+\}/);
    if (jsonMatch) {
      return JSON.parse(jsonMatch[0]);
    }
  } catch (e) {
    console.error("Failed to parse AI response:", e);
  }

  // Fallback
  return {
    action: "FLEE",
    confidence: 0.5,
    reason: "Failed to parse AI response"
  };
}
```

### 6.2 Combat Threat Assessment Worker

```typescript
// src/threat-assessment.ts

interface ThreatAssessment {
  threatLevel: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
  threats: Array<{
    type: string;
    severity: number;
    distance: number;
    recommendation: string;
  }>;
  overallAction: string;
}

interface EnvironmentScan {
  position: { x: number; y: number; z: number };
  visibleMobs: Array<{
    type: string;
    distance: number;
    health: number;
  }>;
  nearbyBlocks: Array<{
    type: string;
    position: { x: number; y: number; z: number };
  }>;
  timeOfDay: "day" | "night" | "dawn" | "dusk";
  biome: string;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const scan: EnvironmentScan = await request.json();

    const prompt = `
Assess threats in this Minecraft environment:

POSITION: (${scan.position.x}, ${scan.position.y}, ${scan.position.z})
BIOME: ${scan.biome}
TIME: ${scan.timeOfDay}

VISIBLE MOBS:
${scan.visibleMobs.map(m => `- ${m.type} at ${m.distance} blocks`).join("\n")}

HAZARDOUS BLOCKS NEARBY:
${scan.nearbyBlocks.filter(b =>
  ["lava", "fire", "magma_block", "campfire"].includes(b.type)
).map(b => `- ${b.type} at ${b.position.x}, ${b.position.y}, ${b.position.z}`).join("\n")}

Rate each threat 0-10 and provide overall assessment.
Format as JSON: {
  "threatLevel": "CRITICAL|HIGH|MEDIUM|LOW",
  "threats": [
    {"type": "...", "severity": 0-10, "distance": 0, "recommendation": "..."}
  ],
  "overallAction": "one sentence tactical recommendation"
}
`;

    const response = await env.AI.run(
      "@cf/meta/llama-3.1-8b-instruct",
      { messages: [{ role: "user", content: prompt }] }
    );

    const assessment: ThreatAssessment = parseThreatAssessment(response);

    return Response.json(assessment);
  }
};
```

### 6.3 Environment Safety Check Worker

```typescript
// src/safety-check.ts

interface SafetyCheck {
  isSafe: boolean;
  hazards: Array<{
    type: string;
    severity: "fatal" | "danger" | "warning";
    distance: number;
  }>;
  safePath: Array<{ x: number; y: number; z: number }>;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const { position, viewDistance } = await request.json();

    // Use classification model for hazard detection
    // This would normally include screenshot data
    const prompt = `
Check safety for agent at (${position.x}, ${position.y}, ${position.z}).
View distance: ${viewDistance} blocks.

Check for:
1. Lava within 5 blocks
2. Cliffs (drops > 4 blocks)
3. Hostile mobs
4. Suffocation hazards

Return JSON: {
  "isSafe": true/false,
  "hazards": [{"type": "...", "severity": "fatal|danger|warning", "distance": 0}],
  "safePath": [{x, y, z}, ...]
}
`;

    const response = await env.AI.run(
      "@cf/meta/llama-3.1-8b-instruct",
      { prompt, max_tokens: 150 }
    );

    const safety: SafetyCheck = parseSafetyCheck(response);

    return Response.json(safety);
  }
};
```

### 6.4 Batch Decision Worker (Optimized for Low Latency)

```typescript
// src/batch-decisions.ts

interface BatchDecisionRequest {
  decisions: Array<{
    id: string;
    type: "combat" | "navigation" | "resource";
    data: any;
  }>;
}

interface BatchDecisionResponse {
  decisions: Array<{
    id: string;
    result: any;
  }>;
  processingTimeMs: number;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const startTime = Date.now();
    const batch: BatchDecisionRequest = await request.json();

    // Process all decisions in parallel
    const results = await Promise.all(
      batch.decisions.map(async (decision) => {
        const prompt = buildPromptForType(decision.type, decision.data);

        // Try cache first
        const cacheKey = `${decision.type}:${JSON.stringify(decision.data)}`;
        const cached = await env.DECISION_CACHE.get(cacheKey, "json");

        if (cached) {
          return { id: decision.id, result: cached };
        }

        // Query AI
        const response = await env.AI.run(
          "@cf/microsoft/phi-3-mini", // Use smaller model for speed
          { prompt, max_tokens: 50 }
        );

        const result = parseResult(decision.type, response);

        // Cache for 30 seconds
        await env.DECISION_CACHE.put(
          cacheKey,
          JSON.stringify(result),
          { expirationTtl: 30 }
        );

        return { id: decision.id, result };
      })
    );

    const response: BatchDecisionResponse = {
      decisions: results,
      processingTimeMs: Date.now() - startTime
    };

    return Response.json(response);
  }
};

function buildPromptForType(type: string, data: any): string {
  // Minimal prompts for faster inference
  switch (type) {
    case "combat":
      return `Health ${data.health}, ${data.mob} at ${data.distance} blocks. Action? (attack/flee)`;
    case "navigation":
      return `From ${data.from} to ${data.to}, obstacle: ${data.obstacle}. Waypoint?`;
    case "resource":
      return `Need ${data.resource}, have ${data.inventory}. Priority? (mine/craft/wait)`;
    default:
      return "Decision needed.";
  }
}
```

### 6.5 Python Worker Example (Python Workflows)

```python
# src/tactical_worker.py

from workers import Response, Request
import json

class TacticalWorker:
    async def on_fetch(self, request: Request, env):
        if request.method != "POST":
            return Response("Method not allowed", status=405)

        situation = await request.json()

        # Check cache
        cache_key = f"tactical:{json.dumps(situation)}"
        cached = await env.DECISION_CACHE.get(cache_key)

        if cached:
            return Response.json(json.loads(cached))

        # Build prompt
        prompt = self.build_prompt(situation)

        # Query AI
        response = await env.AI.run(
            "@cf/meta/llama-3.1-8b-instruct",
            {"prompt": prompt, "max_tokens": 100}
        )

        # Parse decision
        decision = self.parse_decision(response)

        # Cache for 10 seconds
        await env.DECISION_CACHE.put(
            cache_key,
            json.dumps(decision),
            expiration_ttl=10
        )

        return Response.json(decision)

    def build_prompt(self, situation):
        return f"""
Analyze situation: {situation}
Recommend action (attack/flee/strafe).
Format: {{"action": "...", "confidence": 0.0}}
"""

    def parse_decision(self, response):
        text = response.get("response", "")
        try:
            import re
            match = re.search(r'\{[^}]+\}', text)
            if match:
                return json.loads(match.group(0))
        except:
            pass

        return {"action": "flee", "confidence": 0.5}

# Entry point
worker = TacticalWorker()
on_fetch = worker.on_fetch
```

---

## 7. Integration Strategy for MineWright AI

### 7.1 Architecture Overview

```
MineWright AI (Minecraft Forge)
    |
    v
TaskPlanner (Async LLM Client)
    |
    +-- High-Level Planning (OpenAI/Groq - GPT-4/Llama3-70b)
    |   - Strategic goals
    |   - Complex multi-step tasks
    |   - Latency: 2-5 seconds (acceptable)
    |
    +-- Tactical Decisions (Cloudflare Workers AI - Llama3-8b)
    |   - Combat choices
    |   - Hazard detection
    |   - Path adjustments
    |   - Latency: 80-100ms (cached)
    |
    +-- Real-Time Actions (Local Inference)
        - Tick-by-tick movement
        - Block placement
        - Inventory management
        - Latency: <5ms (local)
```

### 7.2 Recommended Integration Points

| Decision Type | Recommended Solution | Reasoning |
|---------------|---------------------|-----------|
| **Strategic Planning** | OpenAI/Groq (GPT-4/Llama3-70b) | Complex reasoning, 2-5s acceptable |
| **Combat Tactics** | Workers AI (Llama3-8b) | Quick decisions, 80-100ms OK |
| **Hazard Detection** | Workers AI (ResNet-50) | Image classification, ~100ms |
| **Path Adjustments** | Workers AI (Phi-3 Mini) | Fast inference, cacheable |
| **Tick Actions** | Local code | Must be <5ms per tick |
| **Emergency Flee** | Local rules | No time for network call |

### 7.3 Caching Strategy

```java
// src/main/java/com/minewright/llm/WorkersAI tactical Client.java

package com.steve.llm;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client for Cloudflare Workers AI tactical decisions.
 * Caches decisions for 10-30 seconds to avoid redundant API calls.
 */
public class WorkersAI tactical Client {

    private final HttpClient httpClient;
    private final String workerUrl;
    private final CaffeineCache<String, TacticalDecision> cache;

    public WorkersAI tactical Client(String workerUrl) {
        this.workerUrl = workerUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(500))
            .build();

        // Cache decisions for 10 seconds
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();
    }

    /**
     * Get tactical decision with caching.
     * Returns cached decision if available, otherwise queries Workers AI.
     */
    public CompletableFuture<TacticalDecision> getTacticalDecision(
        CombatSituation situation
    ) {
        // Generate cache key from situation
        String cacheKey = generateCacheKey(situation);

        // Check cache
        TacticalDecision cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Query Workers AI
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(workerUrl))
                    .timeout(Duration.ofMillis(500))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(situation)
                    ))
                    .build();

                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );

                TacticalDecision decision = objectMapper.readValue(
                    response.body(),
                    TacticalDecision.class
                );

                // Cache the decision
                cache.put(cacheKey, decision);

                return decision;
            } catch (Exception e) {
                // Fallback to conservative decision
                return TacticalDecision.flee();
            }
        });
    }

    /**
     * Batch multiple decisions into single API call for efficiency.
     */
    public CompletableFuture<List<TacticalDecision>> getBatchDecisions(
        List<CombatSituation> situations
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BatchRequest batch = new BatchRequest(situations);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(workerUrl + "/batch"))
                    .timeout(Duration.ofMillis(1000))
                    .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(batch)
                    ))
                    .build();

                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );

                BatchResponse batchResponse = objectMapper.readValue(
                    response.body(),
                    BatchResponse.class
                );

                // Cache all decisions
                for (int i = 0; i < situations.size(); i++) {
                    String cacheKey = generateCacheKey(situations.get(i));
                    cache.put(cacheKey, batchResponse.decisions.get(i));
                }

                return batchResponse.decisions;
            } catch (Exception e) {
                // Fallback to conservative decisions
                return situations.stream()
                    .map(s -> TacticalDecision.flee())
                    .collect(Collectors.toList());
            }
        });
    }

    private String generateCacheKey(CombatSituation situation) {
        // Normalize situation for caching
        // Round distances to nearest block, quantize health, etc.
        return String.format(
            "combat:h=%d:w=%s:m=%s:d=%d",
            situation.getAgentHealth() / 2, // Quantize health
            situation.getWeapon(),
            situation.getMobType(),
            situation.getMobDistance()
        );
    }
}
```

### 7.4 Decision Latency Budget

```
Total Tick Time: 50ms (20 TPS)

Allocation:
- Game Logic: 20ms
- Entity Processing: 15ms
- AI Decision: 10ms
- Rendering: 5ms

AI Decision Breakdown:
- Local Rules Check: 2ms
- Cache Lookup: 1ms
- Network Call (if cache miss): 80ms ❌ TOO SLOW
- Decision Execution: 2ms

Conclusion: Workers AI CANNOT be used for per-tick decisions.
Must be used for higher-frequency tactical decisions (every 2-5 seconds).
```

### 7.5 Recommended Decision Frequency

| Decision Type | Frequency | Latency Budget | Solution |
|---------------|-----------|----------------|----------|
| **Attack/Flee** | Every 2 seconds | 100ms | Workers AI |
| **Path Selection** | Every 5 seconds | 200ms | Workers AI |
| **Hazard Check** | Every 1 second | 100ms | Workers AI |
| **Block Breaking** | Per tick | <5ms | Local |
| **Movement** | Per tick | <5ms | Local |
| **Inventory Sort** | Every 10 seconds | 500ms | Workers AI |

### 7.6 Fallback Strategy

```java
// src/main/java/com/minewright/action/TacticalActionExecutor.java

public class TacticalActionExecutor extends BaseAction {

    private final WorkersAI tactical Client tacticalClient;
    private TacticalDecision cachedDecision;
    private long decisionTime = 0;

    @Override
    public void tick() {
        // Only make new tactical decision every 2 seconds
        long currentTime = System.currentTimeMillis();
        if (currentTime - decisionTime > 2000) {

            // Async get tactical decision
            tacticalClient.getTacticalDecision(getCurrentSituation())
                .thenAccept(decision -> {
                    this.cachedDecision = decision;
                    this.decisionTime = currentTime;
                })
                .exceptionally(e -> {
                    // Fallback to conservative local rules
                    this.cachedDecision = getConservativeDecision();
                    this.decisionTime = currentTime;
                    return null;
                });
        }

        // Execute cached decision locally (tick-by-tick)
        if (cachedDecision != null) {
            executeDecision(cachedDecision);
        } else {
            // No decision yet, use safe default
            executeSafeBehavior();
        }
    }

    private TacticalDecision getConservativeDecision() {
        // Local rule-based fallback
        if (steve.getHealth() < 10) {
            return TacticalDecision.flee();
        }
        if (nearestMobDistance < 5) {
            return TacticalDecision.attack();
        }
        return TacticalDecision.hold();
    }
}
```

---

## 8. References and Sources

### Official Documentation
- [Cloudflare Workers AI Documentation](https://developers.cloudflare.com/workers-ai/)
- [Cloudflare Workers AI Pricing](https://developers.cloudflare.com/workers-ai/platform/pricing/)
- [Cloudflare Workers Python Support](https://developers.cloudflare.com/workers/languages/python/)
- [Cloudflare AI Gateway Binding Methods](https://developers.cloudflare.com/ai-gateway/integrations/worker-binding-methods/)
- [Cloudflare Pages Functions Bindings](https://developers.cloudflare.com/pages/functions/bindings/)

### Articles and Tutorials
- [Workers AI Complete Tutorial (Chinese)](https://juejin.cn/post/7580180556983337010) - Comprehensive guide with 10,000 free calls
- [Moltworker Complete Guide 2026](https://dev.to/sienna/moltworker-complete-guide-2026-running-personal-ai-agents-on-cloudflare-without-hardware-4a99) - Running AI agents on Cloudflare
- [Serverless AI Inference at Edge](https://www.linkedin.com/pulse/serverless-ai-inference-edge-bringing-sub-100ms-responses-sajeer-babu-lnmuc) - Sub-100ms edge AI inference
- [Cloudflare Workers Cold Start Optimization](https://segmentfault.com/a/1190000047274104) - 10 techniques for REST API optimization

### Technical Resources
- [Cloudflare Python Workers Examples](https://github.com/cloudflare/python-workers-examples) - Official GitHub repository
- [Cloudflare Workers AI Models](https://developers.cloudflare.com/workers-ai/models/) - Full model catalog
- [Explore Workers AI Models with Jupyter](https://developers.cloudflare.com/workers-ai/guides/tutorials/explore-workers-ai-models-using-a-jupyter-notebook/) - Interactive model exploration

### Research Papers
- [HydraServe: Minimizing Cold Start Latency for Serverless](https://arxiv.org/html/2502.15524v2) - Research on cold start optimization

### Industry Analysis
- [2026 LLM API Guide](https://www.cnblogs.com/llm-api/p/19429141/llm-api-2131311) - Comprehensive LLM API comparison
- [Cloudflare for Gaming](https://www.cloudflare.com/gaming/) - Gaming-specific solutions

---

## Appendix A: Model Performance Benchmarks

### Llama 3.1 8B Instruct Performance

| Metric | Value |
|--------|-------|
| **Parameters** | 8 billion |
| **Context Window** | 131,072 tokens |
| **Inference Time** | 40-80ms (warm) |
| **Throughput** | ~300 requests/minute |
| **Quality vs GPT-4** | ~75% on reasoning tasks |
| **Quality vs GPT-3.5** | ~90% on tactical tasks |

### Phi-3 Mini Performance

| Metric | Value |
|--------|-------|
| **Parameters** | 3.8 billion |
| **Context Window** | 4,000 tokens |
| **Inference Time** | 20-40ms (warm) |
| **Throughput** | ~500 requests/minute |
| **Best For** | Quick decisions, low latency |

### ResNet-50 Performance

| Metric | Value |
|--------|-------|
| **Inference Time** | 30-60ms (warm) |
| **Accuracy** | 76% top-1 on ImageNet |
| **Best For** | Screenshot classification, hazard detection |

---

## Appendix B: Cost Calculator

### Example: Combat Decisions

**Scenario:** 100 players, each making 10 combat decisions per minute

| Metric | Calculation | Cost |
|--------|-------------|------|
| Decisions per minute | 100 players × 10 = 1,000 | - |
| Decisions per hour | 1,000 × 60 = 60,000 | - |
| Decisions per day | 60,000 × 24 = 1,440,000 | - |
| Neurons per decision | ~100 (simple prompt) | - |
| Total Neurons per day | 1,440,000 × 100 = 144,000,000 | - |
| Free tier coverage | 10,000 neurons/day | -$0.11 (10k free) |
| Paid neurons | 144,000,000 - 10,000 = 143,990,000 | $1.58 |
| **Daily cost** | - | **$1.58** |
| **Monthly cost** | $1.58 × 30 | **$47.40** |

**Optimization with Caching:**
- Cache hit rate: 70% (similar situations)
- Actual API calls: 432,000/day (30% of total)
- Optimized daily cost: **$0.47**
- Optimized monthly cost: **$14.10**

---

## Appendix C: Troubleshooting

### Common Issues

**Issue 1: Cold Start Latency (>10 seconds)**
- **Cause:** Worker not invoked recently
- **Solution:** Implement keep-alive pings every 5 minutes
- **Code:** Use cron trigger or external monitoring service

**Issue 2: Rate Limiting (429 errors)**
- **Cause:** Exceeding 300 requests/minute
- **Solution:** Implement batching and caching
- **Code:** Use `getBatchDecisions()` method

**Issue 3: High Neuron Usage**
- **Cause:** Large prompts or long responses
- **Solution:** Minimize prompt size, use smaller models
- **Code:** Set `max_tokens: 50` for tactical decisions

**Issue 4: JSON Parsing Failures**
- **Cause:** LLM returns malformed JSON
- **Solution:** Use regex extraction and fallback
- **Code:** See `parseDecision()` method

---

## Appendix D: Security Considerations

### API Key Management
```toml
# wrangler.toml - Never commit actual keys
[vars]
WORKER_URL = "https://your-worker.workers.dev"

# Use Cloudflare Secrets for sensitive data
# wrangler secret put API_KEY
```

### Rate Limiting
```typescript
// Implement client-side rate limiting
const rateLimiter = new Map<string, number[]>();

function checkRateLimit(clientId: string): boolean {
  const now = Date.now();
  const requests = rateLimiter.get(clientId) || [];

  // Remove requests older than 1 minute
  const recent = requests.filter(t => now - t < 60000);

  if (recent.length >= 60) { // 60 requests per minute
    return false;
  }

  recent.push(now);
  rateLimiter.set(clientId, recent);
  return true;
}
```

### Input Validation
```typescript
function validateSituation(situation: any): boolean {
  // Validate all inputs to prevent prompt injection
  if (typeof situation.mobType !== 'string') return false;
  if (situation.mobDistance < 0 || situation.mobDistance > 64) return false;
  if (situation.agentHealth < 0 || situation.agentHealth > 20) return false;

  // Sanitize strings to prevent prompt injection
  situation.mobType = situation.mobType.replace(/[^\w\s]/g, '');

  return true;
}
```

---

**Document End**

For questions or updates to this research document, please refer to the official Cloudflare Workers AI documentation or create an issue in the MineWright AI repository.
