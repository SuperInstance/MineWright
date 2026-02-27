# Cloudflare Vectorize for Agent Long-Term Memory Storage

**Research Date:** 2026-02-27
**For:** MineWright AI - Long-term vector-based memory storage
**Focus:** Distributed vector database at the edge, Minecraft world memory, multi-agent shared memory

---

## Executive Summary

Cloudflare Vectorize is a globally distributed vector database designed for AI applications. It integrates seamlessly with Workers AI embeddings, providing low-latency semantic search at the edge. For MineWright AI, Vectorize offers:

1. **Global edge distribution** - Memory accessible worldwide with ~31ms median query latency
2. **Generous free tier** - 30M vector dimension queries/month for development
3. **Seamless Workers AI integration** - Direct embedding generation workflow
4. **Metadata filtering** - Rich queries for Minecraft-specific data (biomes, dimensions, resources)
5. **Multi-agent shared memory** - All Steve agents can access learned world knowledge

**Key Decision Point:** Vectorize V2 GA (Sept 2024) supports up to 5M vectors per index with 50,000 indexes per account, making it production-ready for Minecraft world-scale memory.

---

## 1. Vectorize Overview

### What is Vectorize?

Vectorize is Cloudflare's fully managed vector database service that runs on the global edge network. Unlike traditional databases that require centralized infrastructure, Vectorize stores and queries vectors closer to users worldwide.

**Core Characteristics:**

| Feature | Description |
|---------|-------------|
| **Deployment** | Globally distributed across 300+ edge locations |
| **Latency** | ~31ms median query time (18x faster than V1) |
| **Capacity** | Up to 5 million vectors per index (V2) |
| **Scaling** | Auto-scaling, no capacity planning needed |
| **Integration** | Native bindings for Workers, Pages, and D1 |

### Vector Database at the Edge

Traditional vector databases (Pinecone, Weaviate, Qdrant) require regional deployment:
```
Client -> Regional VDB Endpoint -> Vector Search -> Results
         (high latency for global users)
```

Vectorize edge architecture:
```
Client -> Nearest Edge Location -> Local Vector Search -> Results
         (~10ms latency anywhere)
```

**For Minecraft Multiplayer:** Players worldwide get fast memory queries regardless of server location.

### Index Types and Dimensions

Vectorize indexes are created with specific dimensionality matching the embedding model:

| Embedding Model | Dimensions | Vectorize Model |
|-----------------|------------|-----------------|
| BGE Small | 384 | `@cf/baai/bge-small-en-v1.5` |
| BGE Base | 768 | `@cf/baai/bge-base-en-v1.5` |
| OpenAI Ada-002 | 1536 | Use external API |
| OpenAI text-embedding-3-small | 1536 | Use external API |
| Cohere embed-english-v3.0 | 1024 | Use external API |

**Recommended for MineWright AI:**
- **Primary:** `@cf/baai/bge-base-en-v1.5` (768 dimensions) - Best balance of quality/speed
- **Alternative:** `@cf/baai/bge-small-en-v1.5` (384 dimensions) - For cost optimization

### Metadata Filtering

Vectorize supports rich metadata attached to each vector:
```javascript
metadata: {
    // Minecraft-specific fields
    dimension: "overworld",
    biome: "minecraft:plains",
    resourceType: "diamond_ore",
    // Temporal data
    discoveredAt: "2026-02-27T10:30:00Z",
    // Agent identifiers
    discoveredBy: "steve-001",
    // Quantitative data
    count: 8,
    confidence: 0.95
}
```

**Metadata Indexes (V2):** Create up to 10 indexed metadata fields for efficient filtering:
```bash
wrangler vectorize create world-memory \
  --dimensions=768 \
  --metric=cosine \
  --metadata-index-fields=dimension,biome,resourceType,discoveredBy
```

---

## 2. Index Management

### Creating Indexes via Wrangler

**Basic Index Creation:**
```bash
# Create index for command memory
wrangler vectorize create command-memory \
  --dimensions=768 \
  --metric=cosine

# Create index with metadata filtering
wrangler vectorize create world-memory \
  --dimensions=768 \
  --metric=euclidean \
  --metadata-index-fields=dimension,biome,resourceType
```

**Metric Types:**

| Metric | Formula | Best For | Range |
|--------|---------|----------|-------|
| **cosine** (default) | `1 - cos(A, B)` | Text embeddings, semantic similarity | [-1, 1] |
| **euclidean** | `sqrt(sum((A-B)^2))` | Spatial coordinates, physical distances | [0, ∞] |
| **dot-product** | `-A · B` | Normalized vectors, recommendation systems | (-∞, ∞] |

**For MineWright AI:**
- **Command/Conversational Memory:** cosine (semantic similarity)
- **Location/Coordinate Memory:** euclidean (physical distance)
- **Resource/Item Memory:** dot-product (if vectors normalized)

### Dimension Configuration

**Choosing Dimensions:**

| Use Case | Recommended | Reasoning |
|----------|-------------|-----------|
| Commands/Actions | 384 (BGE Small) | Fast queries, lower storage |
| World Knowledge | 768 (BGE Base) | Better quality for complex queries |
| Structure Blueprints | 1536 (OpenAI) | High fidelity for complex patterns |

**Cost Impact:**
```
Storage Cost = (vectors × dimensions) / 1,000,000 × $0.01
Query Cost   = (queries × dimensions) / 1,000,000 × rate

Example: 100K vectors, 768 dimensions
Storage: (100,000 × 768) / 1M × $0.01 = $0.74/month
Queries (10K/day): 10K × 30 × 768 / 1M × $0.0X ≈ $1-2/month
```

### Index Limits and Pricing

**Free Tier (2025):**

| Resource | Limit |
|----------|-------|
| Vector dimension queries | 30 million/month |
| Stored dimensions | Unlimited (pay per usage) |
| Indexes per account | 50,000 |
| Namespaces per index | 50,000 |
| Vectors per index | 5 million (V2) |

**Paid Tier Pricing:**

| Resource | Cost |
|----------|------|
| Stored dimensions | ~$0.01 per 1M dimensions |
| Query dimensions | Varies by usage volume |
| Data transfer | FREE (no egress fees) |

**Comparison to Competitors:**

| Service | Monthly Cost for 1M 768-dim vectors |
|---------|--------------------------------------|
| **Vectorize** | ~$0.31 |
| Pinecone | ~$41 |
| Weaviate | ~$25-153 |

**Cost Example for MineWright AI:**
```
Scenario: Multiplayer server with 50 active players
- 10K resource discoveries per month
- 5K base blueprints
- 20K conversational memories
- 50K queries per day

Total storage: ~35K vectors × 768 = 27M dimensions = $0.27/month
Queries: 50K × 30 × 768 = 1.15B dimensions = ~$30-50/month (paid tier)
```

---

## 3. Vector Operations

### Inserting Vectors with Metadata

**Basic Insert:**
```javascript
// In a Cloudflare Worker
export default {
  async fetch(request, env, ctx) {
    // Generate embedding
    const embedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
      text: "Diamond vein discovered at coordinates x:123, y:-58, z:456 in plains biome"
    });

    // Insert with metadata
    const inserted = await env.VECTORIZE.insert([{
      id: "diamond-vein-20260227-123-456",
      values: embedding.data[0],
      metadata: {
        resourceType: "diamond_ore",
        dimension: "overworld",
        biome: "minecraft:plains",
        coordinates: { x: 123, y: -58, z: 456 },
        discoveredAt: new Date().toISOString(),
        discoveredBy: "steve-001",
        count: 8,
        verified: true
      }
    }]);

    return Response.json({ success: true, id: inserted });
  }
}
```

**Batch Insert (Recommended):**
```javascript
// Insert multiple discoveries efficiently
const discoveries = [
  {
    id: "diamond-1",
    text: "Diamond vein at 123, -58, 456",
    metadata: { resourceType: "diamond_ore", x: 123, y: -58, z: 456 }
  },
  {
    id: "ancient-debris-1",
    text: "Ancient debris in nether at 456, 15, 789",
    metadata: { resourceType: "ancient_debris", dimension: "the_nether", x: 456, y: 15, z: 789 }
  }
];

// Generate embeddings in batch
const embeddings = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: discoveries.map(d => d.text)
});

// Prepare vectors
const vectors = discoveries.map((d, i) => ({
  id: d.id,
  values: embeddings.data[i],
  metadata: d.metadata
}));

// Batch insert
const result = await env.VECTORIZE.insert(vectors);
```

### Querying for Similar Vectors

**Basic Similarity Search:**
```javascript
// Find similar resource discoveries
const queryVector = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: "Where can I find diamonds near spawn?"
});

const matches = await env.VECTORIZE.query(queryVector.data[0], {
  topK: 10,
  returnValues: false,
  returnMetadata: true,
  filter: {
    dimension: "overworld",
    resourceType: "diamond_ore"
  }
});

// Results ordered by similarity score
matches.matches.forEach(match => {
  console.log(`Score: ${match.score}`);
  console.log(`Location: ${match.metadata.coordinates}`);
  console.log(`Discovered: ${match.metadata.discoveredAt}`);
});
```

**Metadata-Filtered Query:**
```javascript
// Find structures in specific biome
const queryVector = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: "village buildings"
});

const matches = await env.VECTORIZE.query(queryVector.data[0], {
  topK: 5,
  filter: {
    biome: "minecraft:plains",
    structureType: "village"
  },
  namespace: "shared-memory" // Multi-agent namespace
});
```

**Spatial Query (Euclidean):**
```javascript
// Find nearest base from current position
// Note: For true spatial queries, encode coordinates into vector or use separate spatial index

const currentPosition = { x: 100, y: 64, z: 200 };
const queryVector = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: `base location near ${currentPosition.x}, ${currentPosition.y}, ${currentPosition.z}`
});

const matches = await env.VECTORIZE.query(queryVector.data[0], {
  topK: 3,
  filter: {
    type: "base",
    dimension: "overworld"
  }
});

// Post-process for actual distance
const nearbyBases = matches.matches
  .map(m => ({
    ...m.metadata,
    distance: Math.sqrt(
      Math.pow(m.metadata.coordinates.x - currentPosition.x, 2) +
      Math.pow(m.metadata.coordinates.y - currentPosition.y, 2) +
      Math.pow(m.metadata.coordinates.z - currentPosition.z, 2)
    )
  }))
  .sort((a, b) => a.distance - b.distance);
```

### Updating and Deleting Vectors

**Upsert (Insert or Update):**
```javascript
// Update existing discovery with new information
await env.VECTORIZE.upsert([{
  id: "diamond-vein-20260227-123-456",
  values: embedding.data[0],
  metadata: {
    resourceType: "diamond_ore",
    coordinates: { x: 123, y: -58, z: 456 },
    // Updated fields
    lastMined: new Date().toISOString(),
    totalMined: 64,
    depleted: true
  }
}]);
```

**Delete by IDs:**
```javascript
// Remove depleted resource from memory
await env.VECTORIZE.deleteByIds([
  "diamond-vein-20260227-123-456",
  "ancient-debris-1"
]);
```

**Delete by Filter (via query + delete):**
```javascript
// Find all depleted resources
const depleted = await env.VECTORIZE.query(embedding, {
  topK: 1000,
  filter: { depleted: true }
});

// Delete in batches
const depletedIds = depleted.matches.map(m => m.id);
await env.VECTORIZE.deleteByIds(depletedIds);
```

### Batch Operations

**Batch Size Guidelines:**
- **Insert/Upsert:** Up to 1000 vectors per request
- **Delete:** Up to 10,000 IDs per request
- **Query:** Single vector, multiple filters

**Efficient Batch Insert Pattern:**
```javascript
async function batchInsertVectorize(items, batchSize = 100) {
  for (let i = 0; i < items.length; i += batchSize) {
    const batch = items.slice(i, i + batchSize);

    // Generate embeddings for batch
    const embeddings = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
      text: batch.map(item => item.text)
    });

    // Prepare vectors
    const vectors = batch.map((item, idx) => ({
      id: item.id,
      values: embeddings.data[idx],
      metadata: item.metadata
    }));

    // Insert batch
    await env.VECTORIZE.insert(vectors);

    // Rate limiting delay if needed
    await new Promise(resolve => setTimeout(resolve, 100));
  }
}
```

---

## 4. Minecraft World Memory Use Cases

### 4.1 Store Coordinates of Discovered Resources

**Data Schema:**
```javascript
{
  id: "resource-{type}-{x}-{y}-{z}",
  values: [/* 768-dim embedding */],
  metadata: {
    // Resource identification
    resourceType: "diamond_ore" | "ancient_debris" | "copper_ore",
    // Location
    dimension: "overworld" | "the_nether" | "the_end",
    biome: "minecraft:plains" | "minecraft:desert" | ...,
    coordinates: { x: 123, y: -58, z: 456 },
    // Discovery context
    discoveredAt: "2026-02-27T10:30:00Z",
    discoveredBy: "steve-001",
    // Resource details
    count: 8,
    exposed: true,
    nearbyFeatures: ["lava_pool", "dungeon"],
    // Mining metadata
    mined: false,
    depleted: false,
    totalYield: 0
  }
}
```

**Use Case: Query for Resource Finding**
```javascript
// Natural language: "Where's the nearest diamond?"
const queryEmbedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: "diamond ore location near spawn"
});

const results = await env.VECTORIZE.query(queryEmbedding.data[0], {
  topK: 5,
  filter: {
    resourceType: "diamond_ore",
    depleted: false,
    dimension: "overworld"
  },
  returnMetadata: true
});

// Returns: Unmined diamond locations ranked by relevance and proximity
```

### 4.2 Base Blueprints and Structure Embeddings

**Data Schema:**
```javascript
{
  id: "blueprint-{name}-{version}",
  values: [/* 768-dim embedding of description + schematic summary */],
  metadata: {
    // Structure identification
    type: "base" | "farm" | "storage" | "decoration",
    name: "Iron Golem Farm",
    version: 2,
    // Location (if built)
    built: true,
    dimension: "overworld",
    coordinates: { x: 500, y: 70, z: -300 },
    // Build requirements
    materials: {
      "minecraft:iron_block": 64,
      "minecraft:hoppper": 24,
      "minecraft:chest": 8
    },
    dimensions: { length: 15, width: 10, height: 8 },
    // Performance metrics
    efficiency: 95, // iron per hour
    status: "active",
    // Creator
    createdBy: "player123",
    createdAt: "2026-02-27T10:30:00Z"
  }
}
```

**Use Case: Find Similar Structures**
```javascript
// Agent asks: "Show me efficient farms I can build"
const queryEmbedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: "efficient automated farm design iron gool"
});

const results = await env.VECTORIZE.query(queryEmbedding.data[0], {
  topK: 10,
  filter: {
    type: "farm",
    efficiency: { min: 50 } // Require minimum efficiency
  },
  returnMetadata: true
});

// Returns: Similar farm designs with materials and performance data
```

### 4.3 Mob Spawn Point Patterns

**Data Schema:**
```javascript
{
  id: "spawn-point-{mobType}-{x}-{z}",
  values: [/* 768-dim embedding of spawn context */],
  metadata: {
    // Mob identification
    mobType: "zombie" | "skeleton" | "slime" | "witch",
    category: "hostile" | "passive" | "neutral",
    // Location
    dimension: "overworld",
    biome: "minecraft:swamp",
    coordinates: { x: 123, y: 64, z: 456 },
    // Spawn conditions
    lightLevel: 0,
    spawnBlock: "grass_block",
    timeOfDay: "night",
    moonPhase: 3,
    // Spawn statistics
    observations: 42,
    lastSeen: "2026-02-27T22:00:00Z",
    spawnRate: "high",
    // Context
    nearbyStructures: ["village", "pillager_outpost"],
    dangerLevel: 8
  }
}
```

**Use Case: Avoid Dangerous Areas**
```javascript
// Agent asks: "What areas should I avoid at night?"
const queryEmbedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: "dangerous hostile mob spawn areas night"
});

const results = await env.VECTORIZE.query(queryEmbedding.data[0], {
  topK: 20,
  filter: {
    category: "hostile",
    dangerLevel: { min: 5 },
    spawnRate: "high"
  },
  returnMetadata: true
});

// Agent can plan routes avoiding these coordinates
```

### 4.4 Biome Feature Memory

**Data Schema:**
```javascript
{
  id: "biome-feature-{biome}-{x}-{z}",
  values: [/* 768-dim embedding of biome description */],
  metadata: {
    // Biome identification
    biome: "minecraft:jungle",
    variant: "bamboo_jungle",
    // Location
    coordinates: { xMin: 1000, zMin: -500, xMax: 1500, zMax: 0 },
    center: { x: 1250, y: 64, z: -250 },
    // Features
    features: {
      "jungle_pyramid": 1,
      "jungle_tree": 342,
      "cocoa_pod": 89,
      "melon_stem": 15,
      "oak_tree": 56
    },
    // Resources
    resources: {
      "diamond_ore": "present",
      "gold_ore": "abundant",
      "iron_ore": "common"
    },
    // Exploration data
    exploredPercent: 75,
    lastVisited: "2026-02-27T10:30:00Z",
    visitedBy: ["steve-001", "player123"],
    // Notes
    notes: "Good for bamboo, has pyramid with traps",
    rating: 8
  }
}
```

**Use Case: Find Specific Resources**
```javascript
// Agent asks: "Where can I find gold?"
const queryEmbedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: "gold ore abundant biome mesa badlands"
});

const results = await env.VECTORIZE.query(queryEmbedding.data[0], {
  topK: 10,
  filter: {
    "resources.gold_ore": { $in: ["abundant", "common"] }
  },
  returnMetadata: true
});

// Returns: Biomes rich in gold with locations
```

---

## 5. Integration with Workers

### 5.1 Vectorize Binding in wrangler.toml

**Basic Configuration:**
```toml
name = "steve-ai-memory"
main = "src/index.js"
compatibility_date = "2024-09-01"

# Vectorize bindings
[[vectorize]]
binding = "WORLD_MEMORY"
index_name = "minecraft-world-memory"

[[vectorize]]
binding = "COMMAND_MEMORY"
index_name = "steve-command-history"

[[vectorize]]
binding = "BLUEPRINT_MEMORY"
index_name = "structure-blueprints"

# Workers AI binding
[ai]
binding = "AI"

# KV binding for caching
[[kv_namespaces]]
binding = "CACHE"
id = "kv_cache_id"

# D1 binding for structured data
[[d1_databases]]
binding = "DB"
database_name = "steve-structured-data"
```

**Environment Interface:**
```typescript
interface Env {
  // Vectorize indexes
  WORLD_MEMORY: VectorizeIndex;
  COMMAND_MEMORY: VectorizeIndex;
  BLUEPRINT_MEMORY: VectorizeIndex;

  // Workers AI
  AI: Ai;

  // Storage
  KV: KVNamespace;
  DB: D1Database;
}
```

### 5.2 Query Patterns from Workers

**RESTful API Endpoints:**
```javascript
// Worker: src/index.js
export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;

    // CORS headers for Minecraft server integration
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE",
      "Access-Control-Allow-Headers": "Content-Type"
    };

    if (path === "/api/memory/resource") {
      if (request.method === "POST") {
        return handleStoreResource(request, env);
      } else if (request.method === "GET") {
        return handleQueryResource(request, env);
      }
    }

    if (path === "/api/memory/blueprint") {
      if (request.method === "POST") {
        return handleStoreBlueprint(request, env);
      } else if (request.method === "GET") {
        return handleQueryBlueprint(request, env);
      }
    }

    return new Response("Not found", { status: 404, headers: corsHeaders });
  }
};
```

**Store Resource Handler:**
```javascript
async function handleStoreResource(request, env) {
  const { resourceType, dimension, biome, coordinates, discoveredBy } = await request.json();

  // Generate embedding
  const embedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
    text: `${resourceType} at ${coordinates.x}, ${coordinates.y}, ${coordinates.z} in ${biome}`
  });

  // Store in Vectorize
  const id = `resource-${resourceType}-${coordinates.x}-${coordinates.y}-${coordinates.z}`;
  await env.WORLD_MEMORY.insert([{
    id,
    values: embedding.data[0],
    metadata: {
      resourceType,
      dimension,
      biome,
      coordinates,
      discoveredAt: new Date().toISOString(),
      discoveredBy,
      depleted: false
    }
  }]);

  return Response.json({ success: true, id });
}
```

**Query Resource Handler:**
```javascript
async function handleQueryResource(request, env) {
  const url = new URL(request.url);
  const query = url.searchParams.get("q");
  const resourceType = url.searchParams.get("type");
  const dimension = url.searchParams.get("dimension");
  const limit = parseInt(url.searchParams.get("limit") || "10");

  // Generate query embedding
  const embedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
    text: query
  });

  // Build filter
  const filter = { depleted: false };
  if (resourceType) filter.resourceType = resourceType;
  if (dimension) filter.dimension = dimension;

  // Query Vectorize
  const results = await env.WORLD_MEMORY.query(embedding.data[0], {
    topK: limit,
    filter,
    returnMetadata: true
  });

  return Response.json({
    success: true,
    results: results.matches.map(m => ({
      id: m.id,
      score: m.score,
      ...m.metadata
    }))
  });
}
```

### 5.3 Metadata Schema for Minecraft Data

**Comprehensive Schema:**
```typescript
interface MinecraftMetadata {
  // Common fields
  dimension: "overworld" | "the_nether" | "the_end";
  biome: string; // Minecraft biome ID
  coordinates: { x: number; y: number; z: number };
  timestamp: string; // ISO 8601

  // Resource-specific
  resourceType?: string; // diamond_ore, ancient_debris, etc.
  depleted?: boolean;
  count?: number;

  // Structure-specific
  structureType?: string; // village, fortress, portal, etc.
  built?: boolean;
  createdBy?: string;

  // Mob-specific
  mobType?: string;
  category?: "hostile" | "passive" | "neutral";
  dangerLevel?: number;

  // Agent-specific
  agentId?: string;
  shared?: boolean; // Multi-agent shared memory

  // Custom fields
  [key: string]: any;
}
```

**Namespaces for Multi-Agent Memory:**
```javascript
// Private agent memory
const privateNamespace = `agent-${agentId}`;

// Shared team memory
const sharedNamespace = "shared";

// Server-wide public memory
const publicNamespace = "server";

// Query with namespace
await env.WORLD_MEMORY.query(vector, {
  topK: 10,
  namespace: sharedNamespace
});
```

---

## 6. Embedding Generation

### 6.1 Where to Generate Embeddings (Edge vs Local)

| Approach | Pros | Cons | Best For |
|----------|------|------|----------|
| **Edge (Workers AI)** | No local setup, auto-scaling, low latency | API costs, network dependency | Production, low traffic |
| **Local (ONNX)** | No API costs, offline capable | Initial model loading, GPU needed | High traffic, offline first |
| **Hybrid** | Cost + speed optimization | Cache management complexity | Variable traffic |

**Recommendation for MineWright AI:**

1. **Development:** Edge (Workers AI) - Fastest iteration
2. **Low Traffic Servers (<10 queries/min):** Edge - Simpler infrastructure
3. **High Traffic Servers (>100 queries/min):** Local + cache - Lower costs
4. **Offline/Single Player:** Local only - No network dependency

### 6.2 Text Embeddings for Commands

**Command Embedding Strategy:**
```javascript
// Embed the command with context
const commandContext = {
  command: "Build a cobblestone generator",
  playerIntent: "automated resource generation",
  currentLocation: { x: 100, y: 64, z: 100 },
  availableMaterials: ["cobblestone", "bucket", "water_bucket"],
  nearbyStructures: ["base", "storage"]
};

const textToEmbed = JSON.stringify(commandContext);

const embedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: textToEmbed
});

// Store for similar command retrieval
await env.COMMAND_MEMORY.insert([{
  id: `command-${Date.now()}`,
  values: embedding.data[0],
  metadata: {
    command: commandContext.command,
    intent: commandContext.playerIntent,
    executedAt: new Date().toISOString(),
    success: true,
    executionTime: 45, // seconds
    agentId: "steve-001"
  }
}]);
```

**Use Case: Command Similarity**
```javascript
// User types: "Make a stone generator"
// Agent checks if similar to previous commands

const queryEmbedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: "make a stone generator"
});

const similar = await env.COMMAND_MEMORY.query(queryEmbedding.data[0], {
  topK: 3,
  returnMetadata: true
});

// If high similarity found:
// - Reuse successful execution plan
// - Avoid previous failures
// - Adapt from similar commands
```

### 6.3 Spatial Embeddings for Coordinates

**Challenge:** Vector similarity doesn't capture spatial relationships directly.

**Solution 1: Encode coordinates in text**
```javascript
const locationText = `
  Location at coordinates x:${x}, y:${y}, z:${z}
  Biome: ${biome}
  Nearby: ${nearbyFeatures.join(", ")}
  Elevation: ${elevation}
  Distance from spawn: ${distanceFromSpawn}
`;

const embedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
  text: locationText
});
```

**Solution 2: Hybrid spatial + semantic**
```javascript
// Use separate spatial index for exact coordinates
// Use vector search for semantic similarity

// 1. Vector search for similar locations (by biome, features)
const semanticResults = await env.WORLD_MEMORY.query(embedding, {
  topK: 20,
  filter: { biome: "minecraft:plains" }
});

// 2. Post-process for spatial distance
const currentPos = { x: 100, y: 64, z: 100 };
const spatialResults = semanticResults.matches
  .map(m => ({
    ...m,
    distance: calculateDistance(currentPos, m.metadata.coordinates)
  }))
  .sort((a, b) => a.distance - b.distance)
  .slice(0, 5);

function calculateDistance(pos1, pos2) {
  return Math.sqrt(
    Math.pow(pos2.x - pos1.x, 2) +
    Math.pow(pos2.y - pos1.y, 2) +
    Math.pow(pos2.z - pos1.z, 2)
  );
}
```

### 6.4 Caching Strategies

**Embedding Cache (KV):**
```javascript
async function getCachedOrGenerateEmbedding(env, text) {
  // Generate cache key from text hash
  const cacheKey = `embedding:${hashString(text)}`;

  // Check KV cache
  const cached = await env.KV.get(cacheKey, "json");
  if (cached) {
    return cached;
  }

  // Generate new embedding
  const embedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
    text
  });

  // Cache for 7 days
  await env.KV.put(cacheKey, JSON.stringify(embedding.data[0]), {
    expirationTtl: 604800 // 7 days
  });

  return embedding.data[0];
}

function hashString(str) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash;
  }
  return hash.toString(36);
}
```

**Precompute Common Embeddings:**
```javascript
// Precompute embeddings for common queries
const commonQueries = [
  "where are diamonds",
  "find iron ore",
  "nearest village",
  "safe place to sleep",
  "build base location",
  "food sources nearby"
];

async function precomputeEmbeddings(env) {
  for (const query of commonQueries) {
    await getCachedOrGenerateEmbedding(env, query);
  }
}

// Run on schedule or deploy
```

---

## 7. Code Examples

### 7.1 Store Discovered Diamond Vein Location

```javascript
async function storeDiamondDiscovery(env, location, context) {
  const { x, y, z } = location;
  const { biome, agentId, veinSize } = context;

  // Generate embedding
  const embedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
    text: `Diamond vein discovered at coordinates ${x}, ${y}, ${z} in ${biome} biome. Size: ${veinSize} ores.`
  });

  // Store in Vectorize
  const vectorId = `diamond-${x}-${y}-${z}-${Date.now()}`;
  await env.WORLD_MEMORY.insert([{
    id: vectorId,
    values: embedding.data[0],
    metadata: {
      resourceType: "diamond_ore",
      dimension: "overworld",
      biome: biome,
      coordinates: { x, y, z },
      discoveredAt: new Date().toISOString(),
      discoveredBy: agentId,
      veinSize: veinSize,
      estimatedYield: Math.floor(veinSize * 1.3), // Average with fortune
      mined: false,
      depleted: false,
      // Accessibility metadata
      accessibility: {
        hasLava: context.nearLava || false,
        nearWater: context.nearWater || false,
        darknessLevel: context.lightLevel || 0,
        dangerLevel: context.mobsNearby ? 7 : 2
      },
      // Share with team
      shared: true,
      namespace: "shared"
    }
  }]);

  // Also store in structured DB for exact queries
  await env.DB.prepare(`
    INSERT INTO resource_discoveries
    (id, resource_type, x, y, z, biome, discovered_by, vein_size)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `).bind(
    vectorId,
    "diamond_ore",
    x, y, z,
    biome,
    agentId,
    veinSize
  ).run();

  return { success: true, id: vectorId };
}

// Usage
await storeDiamondDiscovery(env, { x: 123, y: -58, z: 456 }, {
  biome: "minecraft:plains",
  agentId: "steve-001",
  veinSize: 8,
  nearLava: true,
  lightLevel: 0
});
```

### 7.2 Query Nearest Base from Position

```javascript
async function findNearestBase(env, currentPosition) {
  const { x, y, z } = currentPosition;

  // Generate query embedding
  const queryEmbedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
    text: `base or home location near coordinates ${x}, ${y}, ${z}`
  });

  // Query for similar structures
  const results = await env.BLUEPRINT_MEMORY.query(queryEmbedding.data[0], {
    topK: 20, // Get more candidates for spatial filtering
    filter: {
      type: "base",
      built: true
    },
    returnMetadata: true
  });

  // Calculate actual distances
  const basesWithDistance = results.matches
    .filter(m => m.metadata.coordinates) // Filter out blueprints not yet built
    .map(match => {
      const coords = match.metadata.coordinates;
      const distance = Math.sqrt(
        Math.pow(coords.x - x, 2) +
        Math.pow(coords.y - y, 2) +
        Math.pow(coords.z - z, 2)
      );

      return {
        id: match.id,
        score: match.score,
        name: match.metadata.name,
        distance: Math.round(distance),
        coordinates: coords,
        facilities: match.metadata.facilities || [],
        owner: match.metadata.createdBy,
        lastVisited: match.metadata.lastVisited
      };
    })
    .sort((a, b) => a.distance - b.distance) // Sort by actual distance
    .slice(0, 5); // Return top 5 nearest

  return basesWithDistance;
}

// Usage
const currentPosition = { x: 100, y: 64, z: 200 };
const nearestBases = await findNearestBase(env, currentPosition);

console.log("Nearest bases:");
nearestBases.forEach((base, idx) => {
  console.log(`${idx + 1}. ${base.name} - ${base.distance} blocks away`);
  console.log(`   Facilities: ${base.facilities.join(", ")}`);
});
```

### 7.3 Store and Retrieve Build Blueprints

```javascript
async function storeBlueprint(env, blueprint) {
  const {
    name,
    type,
    description,
    materials,
    dimensions,
    creator,
    schematic // NBT or schematic format data
  } = blueprint;

  // Generate rich text for embedding
  const embeddingText = `
    ${name}: ${description}
    Type: ${type}
    Dimensions: ${dimensions.length}x${dimensions.width}x${dimensions.height}
    Materials: ${Object.entries(materials).map(([m, c]) => `${m}x${c}`).join(", ")}
    Features: ${blueprint.features?.join(", ") || "none"}
  `;

  const embedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
    text: embeddingText
  });

  // Store blueprint metadata
  const blueprintId = `blueprint-${type}-${Date.now()}`;
  await env.BLUEPRINT_MEMORY.insert([{
    id: blueprintId,
    values: embedding.data[0],
    metadata: {
      type, // farm, storage, base, decoration
      name,
      description,
      dimensions,
      materials,
      features: blueprint.features || [],
      difficulty: blueprint.difficulty || "medium",
      estimatedTime: blueprint.estimatedTime || 0, // minutes
      creator,
      createdAt: new Date().toISOString(),
      // Performance metrics (if built before)
      performance: blueprint.performance || null,
      rating: blueprint.rating || 0,
      timesBuilt: 0,
      // Share status
      shared: blueprint.shared !== false, // Default shared
      namespace: blueprint.shared === false ? `private-${creator}` : "shared"
    }
  }]);

  // Store full schematic in R2 or D1
  await env.DB.prepare(`
    INSERT INTO blueprints (id, name, schematic_data, created_by)
    VALUES (?, ?, ?, ?)
  `).bind(
    blueprintId,
    name,
    JSON.stringify(schematic),
    creator
  ).run();

  return { success: true, id: blueprintId };
}

async function findSimilarBlueprints(env, query, filters = {}) {
  // Generate query embedding
  const queryEmbedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
    text: query
  });

  // Build metadata filter
  const filter = {};
  if (filters.type) filter.type = filters.type;
  if (filters.difficulty) filter.difficulty = filters.difficulty;
  if (filters.maxDifficulty) filter.difficulty = { $lte: filters.maxDifficulty };
  if (filters.minRating) filter.rating = { $gte: filters.minRating };

  // Query Vectorize
  const results = await env.BLUEPRINT_MEMORY.query(queryEmbedding.data[0], {
    topK: filters.limit || 10,
    filter: Object.keys(filter).length > 0 ? filter : undefined,
    returnMetadata: true
  });

  // Return blueprint summaries
  return results.matches.map(match => ({
    id: match.id,
    similarity: match.score,
    name: match.metadata.name,
    type: match.metadata.type,
    description: match.metadata.description,
    dimensions: match.metadata.dimensions,
    materials: match.metadata.materials,
    difficulty: match.metadata.difficulty,
    estimatedTime: match.metadata.estimatedTime,
    rating: match.metadata.rating,
    timesBuilt: match.metadata.timesBuilt,
    creator: match.metadata.creator
  }));
}

// Usage
// Store a blueprint
await storeBlueprint(env, {
  name: "Efficient Iron Golem Farm",
  type: "farm",
  description: "Compact iron golem farm producing ~60 iron/hour with minimal resources",
  materials: {
    "minecraft:iron_block": 64,
    "minecraft:hopper": 24,
    "minecraft:chest": 8,
    "minecraft:trapdoor": 32,
    "minecraft:door": 16
  },
  dimensions: { length: 15, width: 10, height: 8 },
  features: ["automatic", "compact", "reset-proof"],
  difficulty: "medium",
  estimatedTime: 120, // 2 hours
  creator: "player123",
  shared: true,
  schematic: { /* NBT data */ }
});

// Find similar blueprints
const similarFarms = await findSimilarBlueprints(env, "automated farm iron efficient", {
  type: "farm",
  maxDifficulty: "medium",
  minRating: 4,
  limit: 5
});
```

### 7.4 Multi-Agent Shared Memory

```javascript
// Namespace strategy for multi-agent memory
const NAMESPACES = {
  PRIVATE: (agentId) => `private-${agentId}`,
  SHARED: "shared",
  TEAM: (teamId) => `team-${teamId}`,
  SERVER: "server"
};

async function storeSharedMemory(env, memory, agentId) {
  const { type, content, shareLevel } = memory;

  // Determine namespace based on share level
  let namespace;
  switch (shareLevel) {
    case "private":
      namespace = NAMESPACES.PRIVATE(agentId);
      break;
    case "team":
      namespace = NAMESPACES.TEAM(memory.teamId || "default");
      break;
    case "server":
      namespace = NAMESPACES.SERVER;
      break;
    default:
      namespace = NAMESPACES.SHARED;
  }

  // Generate embedding
  const embedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
    text: content
  });

  // Store with namespace
  const memoryId = `${type}-${namespace}-${Date.now()}`;
  await env.WORLD_MEMORY.insert([{
    id: memoryId,
    values: embedding.data[0],
    metadata: {
      type,
      content,
      namespace,
      createdBy: agentId,
      createdAt: new Date().toISOString(),
      accessCount: 0,
      lastAccessed: null,
      // Access control
      shareLevel,
      teamId: memory.teamId || null
    }
  }]);

  return { success: true, id: memoryId, namespace };
}

async function queryAgentMemory(env, query, agentId, options = {}) {
  const { includePrivate = true, includeShared = true, includeTeam = true } = options;

  // Generate query embedding
  const queryEmbedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', {
    text: query
  });

  // Query across multiple namespaces
  const results = [];

  if (includePrivate) {
    const privateResults = await env.WORLD_MEMORY.query(queryEmbedding.data[0], {
      topK: 10,
      namespace: NAMESPACES.PRIVATE(agentId),
      returnMetadata: true
    });
    results.push(...privateResults.matches.map(m => ({ ...m, source: "private" })));
  }

  if (includeShared) {
    const sharedResults = await env.WORLD_MEMORY.query(queryEmbedding.data[0], {
      topK: 10,
      namespace: NAMESPACES.SHARED,
      returnMetadata: true
    });
    results.push(...sharedResults.matches.map(m => ({ ...m, source: "shared" })));
  }

  if (includeTeam && options.teamId) {
    const teamResults = await env.WORLD_MEMORY.query(queryEmbedding.data[0], {
      topK: 10,
      namespace: NAMESPACES.TEAM(options.teamId),
      returnMetadata: true
    });
    results.push(...teamResults.matches.map(m => ({ ...m, source: "team" })));
  }

  // Combine, deduplicate, and sort by relevance
  const combined = results
    .sort((a, b) => b.score - a.score)
    .slice(0, 20)
    .map(m => ({
      id: m.id,
      content: m.metadata.content,
      source: m.source,
      similarity: m.score,
      createdBy: m.metadata.createdBy,
      createdAt: m.metadata.createdAt
    }));

  // Update access tracking
  for (const result of combined) {
    await env.WORLD_MEMORY.upsert([{
      id: result.id,
      values: queryEmbedding.data[0], // Keep original embedding
      metadata: {
        ...result,
        accessCount: (result.accessCount || 0) + 1,
        lastAccessed: new Date().toISOString()
      }
    }]);
  }

  return combined;
}

// Usage
// Agent stores private discovery
await storeSharedMemory(env, {
  type: "resource_discovery",
  content: "Found secret diamond stash at my secret base location",
  shareLevel: "private"
}, "steve-001");

// Agent stores shared discovery
await storeSharedMemory(env, {
  type: "resource_discovery",
  content: "Diamond vein at 123, -58, 456 in plains biome",
  shareLevel: "shared"
}, "steve-001");

// Agent queries memory (sees private + shared)
const memories = await queryAgentMemory(env, "diamond locations", "steve-001", {
  includePrivate: true,
  includeShared: true
});

// Another agent queries (sees only shared)
const publicMemories = await queryAgentMemory(env, "diamond locations", "steve-002", {
  includePrivate: false,
  includeShared: true
});
```

---

## 8. Implementation Roadmap

### Phase 1: Foundation (Week 1-2)

**Tasks:**
1. Create Cloudflare account and Workers project
2. Set up Wrangler CLI and authentication
3. Create initial Vectorize indexes
4. Deploy basic Worker with CRUD endpoints
5. Test embedding generation with Workers AI

**Deliverables:**
- Working Vectorize indexes (world-memory, command-memory, blueprint-memory)
- Deployed Worker with REST API
- Basic Python/Java client for Minecraft mod integration

### Phase 2: Minecraft Integration (Week 3-4)

**Tasks:**
1. Create Java client for Vectorize API
2. Integrate with existing `SteveMemory` class
3. Add resource discovery tracking
4. Implement base location storage
5. Add mob spawn point tracking

**Deliverables:**
- `VectorizeMemoryClient.java` for HTTP requests
- Resource discovery auto-tracking in `MineAction`
- Base location commands (`/minewright remember base`)
- Mob encounter logging in `CombatAction`

### Phase 3: Advanced Features (Week 5-6)

**Tasks:**
1. Implement multi-agent shared memory
2. Add namespace isolation
3. Implement caching layer
4. Add blueprint storage and retrieval
5. Create natural language query interface

**Deliverables:**
- Shared memory across all Steve agents
- Blueprint import/export with Litematic support
- Chat interface: "Where did we find diamonds?"
- Memory summarization for long-term storage

### Phase 4: Optimization (Week 7-8)

**Tasks:**
1. Implement embedding caching
2. Add batch insert optimizations
3. Implement local embedding fallback
4. Add monitoring and metrics
5. Cost optimization and rate limiting

**Deliverables:**
- KV cache for common embeddings
- Batch operations for bulk data
- Local ONNX embedding model for offline mode
- Cost dashboard and alerts
- Rate limiting for free tier compliance

---

## 9. Best Practices

### Performance

1. **Batch Operations**
   ```javascript
   // Bad: Individual inserts
   for (const item of items) {
     await env.VECTORIZE.insert([item]);
   }

   // Good: Batch insert
   await env.VECTORIZE.insert(items);
   ```

2. **Cache Embeddings**
   ```javascript
   // Cache common queries in KV
   const cacheKey = `embed:${hash(query)}`;
   let embedding = await env.KV.get(cacheKey, "json");
   if (!embedding) {
     embedding = await env.AI.run('@cf/baai/bge-base-en-v1.5', { text: query });
     await env.KV.put(cacheKey, JSON.stringify(embedding), { expirationTtl: 604800 });
   }
   ```

3. **Use Metadata Filters**
   ```javascript
   // Bad: Fetch all, filter in code
   const results = await env.VECTORIZE.query(vector, { topK: 1000 });
   const filtered = results.matches.filter(m => m.metadata.dimension === "overworld");

   // Good: Filter at query time
   const results = await env.VECTORIZE.query(vector, {
     topK: 10,
     filter: { dimension: "overworld" }
   });
   ```

### Reliability

1. **Handle Edge Cases**
   ```javascript
   try {
     const results = await env.VECTORIZE.query(vector);
     if (!results.matches || results.matches.length === 0) {
       return []; // No results is valid
     }
     return results.matches;
   } catch (error) {
     console.error("Vectorize query failed:", error);
     // Fallback to local memory or alternative strategy
     return await fallbackSearch(query);
   }
   ```

2. **Retry Logic**
   ```javascript
   async function vectorizeWithRetry(operation, maxRetries = 3) {
     for (let i = 0; i < maxRetries; i++) {
       try {
         return await operation();
       } catch (error) {
         if (i === maxRetries - 1) throw error;
         await new Promise(resolve => setTimeout(resolve, Math.pow(2, i) * 1000));
       }
     }
   }
   ```

3. **Validate Input**
   ```javascript
   function validateCoordinates(coords) {
     if (!coords || typeof coords !== "object") return false;
     if (typeof coords.x !== "number" || typeof coords.y !== "number" || typeof coords.z !== "number") return false;
     if (coords.y < -64 || coords.y > 320) return false; // Minecraft Y limits
     return true;
   }
   ```

### Security

1. **API Key Management**
   ```javascript
   // Use environment variables, never hardcode
   const apiToken = env.VECTORIZE_API_TOKEN;

   // Add rate limiting
   const rateLimit = new Map();
   function checkRateLimit(agentId) {
     const key = `rl:${agentId}`;
     const count = rateLimit.get(key) || 0;
     if (count > 100) throw new Error("Rate limit exceeded");
     rateLimit.set(key, count + 1);
     setTimeout(() => rateLimit.delete(key), 60000);
   }
   ```

2. **Input Sanitization**
   ```javascript
   function sanitizeInput(input) {
     return input
       .replace(/</g, "&lt;")
       .replace(/>/g, "&gt;")
       .substring(0, 1000); // Limit length
   }
   ```

3. **Namespace Isolation**
   ```javascript
   // Ensure agents can only access their namespace
   const agentNamespace = `private-${agentId}`;
   if (requestedNamespace !== agentNamespace && !hasSharedAccess) {
     throw new Error("Access denied");
   }
   ```

### Cost Optimization

1. **Monitor Usage**
   ```javascript
   // Track dimension usage
   let dimensionsStored = 0;
   let dimensionsQueried = 0;

   function trackStorage(vectorCount, dimensions) {
     dimensionsStored += vectorCount * dimensions;
     if (dimensionsStored > 1_000_000) {
       console.warn(`Approaching storage limit: ${dimensionsStored} dimensions`);
     }
   }

   function trackQuery(topK, dimensions) {
     dimensionsQueried += topK * dimensions;
   }
   ```

2. **Use Free Tier Wisely**
   ```javascript
   // 30M free queries/month = ~1M/day
   const DAILY_LIMIT = 1_000_000;
   let dailyUsage = 0;

   function checkQuota() {
     if (dailyUsage >= DAILY_LIMIT) {
       // Switch to local mode or queue requests
       return false;
     }
     return true;
   }
   ```

3. **Optimize Vector Dimension**
   ```javascript
   // For simple text, use smaller model
   const model = complexity < 0.5
     ? "@cf/baai/bge-small-en-v1.5"  // 384 dims
     : "@cf/baai/bge-base-en-v1.5";  // 768 dims

   // Saves 50% on storage and queries
   ```

---

## 10. Troubleshooting

### Common Issues

**Issue 1: Vectors not appearing in queries**
```
Cause: Async mutations take a few seconds to complete
Solution: Check mutation.status or wait before querying
```

**Issue 2: Metadata filter not working**
```
Cause: Metadata indexes not created before inserting vectors
Solution: Create indexes with --metadata-index-fields before inserting data
```

**Issue 3: Dimension mismatch**
```
Cause: Embedding model output doesn't match index dimensions
Solution: Verify model (BGE-small = 384, BGE-base = 768) matches index creation
```

**Issue 4: Rate limiting**
```
Cause: Exceeded free tier or paid tier limits
Solution: Implement backoff, use caching, consider paid tier
```

### Debug Commands

```bash
# Check index details
wrangler vectorize describe world-memory

# List all indexes
wrangler vectorize list

# Test query manually
wrangler vectorize query world-memory --vector=[0.1,0.2,...] --topK=5

# Check usage
wrangler analytics vectorize
```

---

## 11. Sources

### Official Cloudflare Documentation

- [Cloudflare Vectorize Product Page](https://www.cloudflare.com/zh-cn/developer-platform/products/vectorize/)
- [What is a Vector Database?](https://www.cloudflare.com/zh-cn/learning/ai/what-is-vector-database/)
- [RAG Tutorial Guide](https://developers.cloudflare.com/workers-ai/guides/tutorials/build-a-retrieval-augmented-generation-ai/)
- [Cloudflare Workers Bindings](https://developers.cloudflare.com/pages/functions/bindings/)
- [Vectorize Pricing](https://developers.cloudflare.com/vectorize/platform/pricing/)

### Technical Articles and Tutorials

- [Using Cloudflare Vectorize for Vector Database](https://jimmysong.io/zh/book/rag-handbook/vectorization/vectorize/) (Jimmy Song)
- [Personal Knowledge Base, Free Online](https://m.163.com/dy/article/JSQ6GSOJ0519EA27.html)
- [Save $50/month with Vector Database](https://juejin.cn/post/7600629210155909126)
- [Cloudflare Workers Billing and Limits](https://developers.cloudflare.com/workers/static-assets/billing-and-limitations/)
- [AI Gateway Pricing](https://developers.cloudflare.com/ai-gateway/reference/pricing/)

### Workers AI and Embeddings

- [RAG and Embeddings Models](https://m.blog.csdn.net/jonas80029735/article/details/158322596) (CSDN)
- [Using Cloudflare Workers AI for Text Embeddings](https://m.blog.csdn.net/bBADAS/article/details/146449056) (CSDN)
- [Text Embedding Implementation](https://m.blog.csdn.net/dgay_hua/article/details/145918188) (CSDN)
- [Efficient ML Model Inference with Workers AI](https://juejin.cn/post/7448190939762868276) (Juejin)
- [Machine Learning Embeddings Guide](https://juejin.cn/post/7444127300600119331) (Juejin)

### Community Resources

- [2025 RAG Practice Handbook](https://www.eet-china.com/mp/a458589.html)
- [Cloudflare Vectorize Blog](https://blog.cloudflare.com/vectorize-vector-database-open-beta/)
- [Building AI-powered Apps](https://blog.cloudflare.com/vectorize-vector-database-open-beta/)

---

## Appendix A: Quick Reference

### Wrangler Commands

```bash
# Install
npm install -g wrangler
wrangler login

# Create index
wrangler vectorize create <name> --dimensions=768 --metric=cosine

# Configure binding (in wrangler.toml)
[[vectorize]]
binding = "VECTORIZE"
index_name = "your-index-name"

# Deploy
wrangler deploy

# Test
wrangler dev
```

### API Endpoints

```javascript
// POST /api/memory/store
{
  "text": "Diamond at 123, -58, 456",
  "metadata": {
    "resourceType": "diamond_ore",
    "coordinates": { "x": 123, "y": -58, "z": 456 }
  }
}

// GET /api/memory/query?q=where+are+diamonds&limit=5
{
  "results": [
    {
      "id": "...",
      "score": 0.95,
      "metadata": { ... }
    }
  ]
}
```

### Model Reference

| Model | Dimensions | Use Case |
|-------|------------|----------|
| @cf/baai/bge-small-en-v1.5 | 384 | Fast queries, low storage |
| @cf/baai/bge-base-en-v1.5 | 768 | Balanced quality/speed |
| @cf/baai/bge-large-en-v1.5 | 1024 | High quality (if available) |

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Status:** Ready for Implementation
