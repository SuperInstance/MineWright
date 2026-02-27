# Cloudflare Durable Objects for Agent State Management

**Research Date:** 2026-02-27
**Status:** Production Ready (GA)
**Purpose:** Maintaining AI agent state at the edge with strong consistency and global distribution

---

## Executive Summary

Cloudflare Durable Objects provide a unique combination of **compute + storage** in a globally distributed serverless platform. They are ideal for AI agent state management, offering:

- **Stateful serverless** with persistent memory across invocations
- **Global uniqueness** via `idFromName()` for consistent agent identity
- **SQLite storage** (10GB per object) with zero-latency access
- **WebSocket hibernation** for real-time agent communication
- **Strong consistency** with transactional storage guarantees
- **Free tier** available for AI agent development

**Key Insight for Steve AI:** Durable Objects can serve as the "brain" for each Steve agent, maintaining state (position, inventory, tasks) at the edge while coordinating with a central Foreman system.

---

## 1. Durable Objects Architecture

### What Are Durable Objects?

Durable Objects are **stateful serverless compute units** that combine:
- **Compute logic** (like a Worker, but stateful)
- **Durable storage** (SQLite database, up to 10GB per object)
- **Global routing** via unique IDs
- **Strong consistency** (single-threaded per object)

### Core Concepts

| Concept | Description |
|---------|-------------|
| **Globally-Unique Naming** | Each object has a unique ID that routes requests from anywhere in the world to the same instance |
| **Single-Threaded Concurrency** | Only one request processes at a time per object (cooperative multitasking like browsers) |
| **Durable Storage** | Up to 10GB SQLite per object with strong consistency and transactional semantics |
| **Elastic Horizontal Scaling** | Scale across Cloudflare's global network automatically |
| **WebSocket Hibernation** | Objects can evict from memory while keeping WebSocket connections open |

### Storage Guarantees

- **Strong consistency:** All reads see the latest writes
- **Transactional:** ACID properties via SQLite
- **Durable:** Survives memory evictions and restarts
- **Point-in-time recovery:** Available for SQLite-backed objects

### Global Uniqueness and Routing

```javascript
// Create a deterministic ID from a business identifier
const agentId = env.AGENTS.idFromName("steve-agent-42");

// Get a stub to communicate with the object
const stub = env.AGENTS.get(agentId);

// All requests to "steve-agent-42" route to the same instance worldwide
const response = await stub.fetch(request);
```

**Key Point:** The same `idFromName("steve-agent-42")` will **always** route to the same Durable Object instance, regardless of where the request originates globally.

---

## 2. Agent State Management

### Per-Agent Unique Instances

Each AI agent (Steve) should have its own Durable Object instance. Use `idFromName()` with a stable identifier:

```javascript
// Steve entity spawns with unique name
const steveName = `steve-${uuid}`; // e.g., "steve-a1b2c3d4"
const agentId = env.AGENTS.idFromName(steveName);
const agentStub = env.AGENTS.get(agentId);

// Initialize agent state on first spawn
await agentStub.fetch(new Request('http://agent/init', {
  method: 'POST',
  body: JSON.stringify({
    position: { x: 100, y: 64, z: -200 },
    inventory: [],
    health: 20
  })
}));
```

### State Persistence Patterns

#### Pattern 1: Direct SQL Storage (Recommended)

```javascript
export class SteveAgent extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.storage = ctx.storage;

    // Initialize SQLite schema on first run
    this.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS agent_state (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL,
        updated_at INTEGER NOT NULL
      )
    `);
  }

  // Update agent position
  updatePosition(x, y, z) {
    const position = JSON.stringify({ x, y, z, timestamp: Date.now() });
    this.storage.sql.exec(
      "INSERT OR REPLACE INTO agent_state (key, value, updated_at) VALUES (?, ?, ?)",
      "position", position, Date.now()
    );
  }

  // Get current position
  getPosition() {
    const result = this.storage.sql.exec(
      "SELECT value FROM agent_state WHERE key = ?",
      "position"
    ).one();
    return result ? JSON.parse(result.value) : null;
  }
}
```

#### Pattern 2: KV-Style Storage

```javascript
export class SteveAgent extends DurableObject {
  async setState(key, value) {
    await this.ctx.storage.put(key, value);
  }

  async getState(key) {
    return await this.ctx.storage.get(key);
  }

  async getAllState() {
    const state = {};
    for (const [key, value] of await this.ctx.storage.list()) {
      state[key] = value;
    }
    return state;
  }
}
```

### Storage API Reference

#### SQLite Storage API (new_sqlite_classes)

| Method | Description |
|--------|-------------|
| `sql.exec(sql, ...params)` | Execute SQL query with parameters |
| `sql.exec(query).one()` | Get single row |
| `sql.exec(query).toArray()` | Get all rows as array |

#### KV-Style Storage API

| Method | Description |
|--------|-------------|
| `await storage.put(key, value)` | Store a key-value pair |
| `await storage.get(key)` | Retrieve value by key |
| `await storage.delete(key)` | Delete a key |
| `await storage.list()` | List all key-value pairs |
| `await storage.transaction(callback)` | Execute transaction |

### Transaction Semantics

```javascript
// SQLite transactions (implicit per exec)
this.storage.sql.exec(`
  BEGIN TRANSACTION;
  UPDATE agent_state SET value = ? WHERE key = 'health';
  UPDATE agent_state SET value = ? WHERE key = 'inventory';
  COMMIT;
`);

// KV-style transactions
await this.ctx.storage.transaction(async (txn) => {
  const health = await txn.get('health');
  if (health > 0) {
    await txn.put('health', health - 1);
    await txn.put('last_damage', Date.now());
  }
});
```

---

## 3. Python Worker Syntax

### Important Note

As of 2026, **Cloudflare Durable Objects are officially supported only for JavaScript/TypeScript Workers**. Python support is available via:

1. **Cloudflare Workflows** (built on DO) - now supports Python
2. **Python-to-WebAssembly compilation** (Pyodide, etc.)
3. **Edge-side Python execution** via Containers (beta)

### TypeScript/JavaScript Worker Syntax (Official)

#### Class-Based Durable Object Definition

```typescript
import { DurableObject } from "cloudflare:workers";

interface Env {
  AGENTS: DurableObjectNamespace<AgentFactory>;
}

export class AgentFactory extends DurableObject {
  private state: AgentState;

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);

    // Initialize state from storage or defaults
    this.state = {
      health: 20,
      position: { x: 0, y: 64, z: 0 },
      inventory: [],
      tasks: []
    };

    // Load persisted state
    this.loadState();
  }

  private async loadState() {
    const stored = await this.ctx.storage.get<AgentState>("state");
    if (stored) {
      this.state = stored;
    }
  }

  private async saveState() {
    await this.ctx.storage.put("state", this.state);
  }

  // Handle incoming fetch requests
  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;

    switch (path) {
      case "/update":
        return this.handleUpdate(request);
      case "/state":
        return Response.json(this.state);
      case "/sync":
        return this.handleSync(request);
      default:
        return new Response("Not found", { status: 404 });
    }
  }

  private async handleUpdate(request: Request): Promise<Response> {
    const update = await request.json() as Partial<AgentState>;
    Object.assign(this.state, update);
    await this.saveState();
    return Response.json({ success: true });
  }

  private async handleSync(request: Request): Promise<Response> {
    // Called by Foreman to query state
    return Response.json({
      health: this.state.health,
      position: this.state.position,
      activeTask: this.state.tasks[0]
    });
  }
}
```

#### Worker Entry Point with Routing

```typescript
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const agentName = url.pathname.split("/")[1];

    if (!agentName) {
      return new Response("Agent name required", { status: 400 });
    }

    // Get or create agent Durable Object
    const agentId = env.AGENTS.idFromName(agentName);
    const stub = env.AGENTS.get(agentId);

    // Forward request to agent
    return stub.fetch(request);
  }
};
```

#### State Initialization

```typescript
export class AgentFactory extends DurableObject {
  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);

    // Check if this is first initialization
    const initialized = await this.ctx.storage.get<boolean>("initialized");

    if (!initialized) {
      await this.initializeState();
      await this.ctx.storage.put("initialized", true);
    }
  }

  private async initializeState() {
    await this.ctx.storage.put("state", {
      health: 20,
      maxHealth: 20,
      position: { x: 0, y: 64, z: 0 },
      inventory: [],
      tasks: [],
      createdAt: Date.now(),
      lastSeen: Date.now()
    });

    // Initialize SQLite schema
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS tasks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        type TEXT NOT NULL,
        status TEXT NOT NULL,
        data TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      )
    `);
  }
}
```

---

## 4. Memory Limits and Pricing

### Storage Limits (SQLite-Backed Durable Objects)

| Resource | Free Plan | Paid Plan |
|----------|-----------|-----------|
| Storage per Durable Object | 10 GB | 10 GB |
| Total storage per account | 5 GB | Unlimited |
| Maximum Durable Object classes | 100 | 500 |
| Key + Value size combined | 2 MB | 2 MB |
| WebSocket message size | 1 MiB | 1 MiB |

### Compute Limits

| Resource | Limit |
|----------|-------|
| CPU time per request | 30 seconds (default) |
| CPU time (configurable) | Up to 5 minutes |
| Concurrent requests per object | Single-threaded (cooperative) |
| Alarms | 1 per object at a time |

### Pricing Model (Paid Plan)

Durable Objects billing is based on:

1. **Storage** ($0.20/GB-month): Measured in gigabytes (not gibibytes)
2. **Requests:** Per-request pricing for all operations
3. **CPU Time:** Billed per CPU-second
4. **Data Transfer:** Standard Workers egress pricing

**Note:** As of 2026, detailed per-unit pricing should be verified on the official [Cloudflare Durable Objects pricing page](https://developers.cloudflare.com/durable-objects/platform/pricing/).

### Free Tier Benefits

- **5 GB total storage** across all Durable Objects
- **SQLite storage** available (key-value backend is paid-only)
- **No commitment** required
- Ideal for AI agent development and testing

---

## 5. Integration Patterns

### Pattern 1: Deterministic Agent Identity with `idFromName()`

Use `idFromName()` for agents with stable identifiers (usernames, entity IDs):

```javascript
// Minecraft entity spawns with unique name
const steveName = `steve-${entityUuid}`; // e.g., "steve-a1b2c3d4"

// All requests for this Steve route to the same DO instance
const agentId = env.AGENTS.idFromName(steveName);
const stub = env.AGENTS.get(agentId);

// Update agent state
await stub.fetch(new Request('http://agent/update', {
  method: 'POST',
  body: JSON.stringify({ position: { x: 100, y: 64, z: -200 } })
}));

// Later, retrieve state from anywhere in the world
const agentId2 = env.AGENTS.idFromName("steve-a1b2c3d4"); // Same ID!
const stub2 = env.AGENTS.get(agentId2);
const state = await stub2.fetch(new Request('http://agent/state'));
```

**Key Benefits:**
- Same agent always maps to same DO instance
- State persists across invocations
- No need to store IDs externally

### Pattern 2: Stored ID Routing with `idFromString()`

Use when IDs are stored externally (database, cookies, tokens):

```javascript
// First time: Create unique ID and store it
const agentId = env.AGENTS.newUniqueId();
const agentIdString = agentId.toString();

// Store in external system (e.g., player session database)
await db.saveSession(playerId, { agentId: agentIdString });

// Later: Retrieve ID and reconstruct
const session = await db.getSession(playerId);
const agentId = env.AGENTS.idFromString(session.agentId);
const stub = env.AGENTS.get(agentId);
```

### Pattern 3: Multi-Agent Coordination

Agents can coordinate by calling each other:

```javascript
export class SteveAgent extends DurableObject {
  async collaborateWith(otherAgentName, action) {
    // Get reference to another agent
    const otherId = env.AGENTS.idFromName(otherAgentName);
    const otherStub = env.AGENTS.get(otherId);

    // Send coordination request
    const response = await otherStub.fetch(new Request('http://agent/coordinate', {
      method: 'POST',
      body: JSON.stringify({
        from: this.name,
        action: action
      })
    }));

    return await response.json();
  }

  async fetch(request) {
    if (request.url.endsWith('/coordinate')) {
      const { from, action } = await request.json();

      // Handle coordination request
      if (action.type === 'trade') {
        return this.handleTrade(from, action);
      }
    }
  }
}
```

### Pattern 4: State Synchronization with Foreman

```javascript
// In SteveAgent Durable Object
async syncWithForeman() {
  // Get Foreman DO instance
  const foremanId = env.FOREMAN.idFromName("global-foreman");
  const foremanStub = env.FOREMAN.get(foremanId);

  // Send heartbeat with current state
  await foremanStub.fetch(new Request('http://foreman/heartbeat', {
    method: 'POST',
    body: JSON.stringify({
      agentName: this.name,
      health: this.state.health,
      position: this.state.position,
      currentTask: this.state.tasks[0],
      timestamp: Date.now()
    })
  }));
}

// In Foreman Durable Object
async handleHeartbeat(data) {
  // Update agent registry
  this.ctx.storage.sql.exec(
    "INSERT OR REPLACE INTO agents (name, health, position, task, last_seen) VALUES (?, ?, ?, ?, ?)",
    data.agentName, data.health, JSON.stringify(data.position), data.currentTask, Date.now()
  );
}
```

### Pattern 5: Geolocation Hints

Route requests to nearest DO instance:

```javascript
const stub = env.AGENTS.get(agentId, {
  locationHint: 'ewn' // Western Europe
});
```

---

## 6. Code Examples

### Example 1: Complete AgentState Class with HP, Inventory, Position

```typescript
interface AgentState {
  name: string;
  health: number;
  maxHealth: number;
  position: { x: number; y: number; z: number };
  inventory: InventoryItem[];
  tasks: Task[];
  stats: {
    blocksMined: number;
    blocksPlaced: number;
    distanceTraveled: number;
  };
}

interface InventoryItem {
  type: string;
  count: number;
  slot?: number;
}

interface Task {
  id: string;
  type: 'mine' | 'build' | 'move' | 'attack';
  target: any;
  status: 'pending' | 'in_progress' | 'completed' | 'failed';
  createdAt: number;
}

export class SteveAgent extends DurableObject {
  private state: AgentState;

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    this.initializeState();
  }

  private async initializeState() {
    // Try to load existing state
    const stored = await this.ctx.storage.get<AgentState>("state");

    if (stored) {
      this.state = stored;
    } else {
      // Initialize default state
      this.state = {
        name: "Steve",
        health: 20,
        maxHealth: 20,
        position: { x: 0, y: 64, z: 0 },
        inventory: [],
        tasks: [],
        stats: {
          blocksMined: 0,
          blocksPlaced: 0,
          distanceTraveled: 0
        }
      };
      await this.saveState();
    }

    // Initialize SQLite schema
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS task_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        type TEXT NOT NULL,
        status TEXT NOT NULL,
        result TEXT,
        started_at INTEGER NOT NULL,
        completed_at INTEGER
      )
    `);
  }

  private async saveState() {
    await this.ctx.storage.put("state", this.state);
  }

  // Take damage
  async takeDamage(amount: number, source: string) {
    this.state.health = Math.max(0, this.state.health - amount);

    // Log damage event
    this.ctx.storage.sql.exec(
      "INSERT INTO events (type, data, timestamp) VALUES (?, ?, ?)",
      "damage", JSON.stringify({ amount, source }), Date.now()
    );

    await this.saveState();

    if (this.state.health === 0) {
      await this.handleDeath();
    }

    return this.state.health;
  }

  // Heal
  async heal(amount: number) {
    this.state.health = Math.min(this.state.maxHealth, this.state.health + amount);
    await this.saveState();
    return this.state.health;
  }

  // Update position
  async moveTo(x: number, y: number, z: number) {
    const oldPos = this.state.position;
    this.state.position = { x, y, z };

    // Calculate distance
    const distance = Math.sqrt(
      Math.pow(x - oldPos.x, 2) +
      Math.pow(y - oldPos.y, 2) +
      Math.pow(z - oldPos.z, 2)
    );

    this.state.stats.distanceTraveled += distance;
    await this.saveState();

    return this.state.position;
  }

  // Add item to inventory
  async addToInventory(item: InventoryItem) {
    const existing = this.state.inventory.find(i => i.type === item.type);

    if (existing) {
      existing.count += item.count;
    } else {
      this.state.inventory.push({
        type: item.type,
        count: item.count,
        slot: this.state.inventory.length
      });
    }

    await this.saveState();
    return this.state.inventory;
  }

  // Handle death
  private async handleDeath() {
    // Drop inventory items in world
    // Reset position to spawn
    // Log death event
    this.ctx.storage.sql.exec(
      "INSERT INTO deaths (position, inventory, timestamp) VALUES (?, ?, ?)",
      JSON.stringify(this.state.position),
      JSON.stringify(this.state.inventory),
      Date.now()
    );

    // Respawn
    this.state.health = this.state.maxHealth;
    this.state.position = { x: 0, y: 64, z: 0 };
    this.state.inventory = [];
    await this.saveState();
  }

  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;

    switch (path) {
      case "/state":
        return Response.json(this.state);

      case "/damage":
        const { amount, source } = await request.json();
        const newHealth = await this.takeDamage(amount, source);
        return Response.json({ health: newHealth });

      case "/move":
        const { x, y, z } = await request.json();
        const newPos = await this.moveTo(x, y, z);
        return Response.json({ position: newPos });

      case "/inventory/add":
        const item = await request.json();
        const inv = await this.addToInventory(item);
        return Response.json({ inventory: inv });

      default:
        return new Response("Not found", { status: 404 });
    }
  }
}
```

### Example 2: Sync Endpoint for Foreman Queries

```typescript
// Foreman Durable Object - coordinates all agents
export class Foreman extends DurableObject {
  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);

    // Initialize agent registry schema
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS agents (
        name TEXT PRIMARY KEY,
        health INTEGER NOT NULL,
        position TEXT NOT NULL,
        current_task TEXT,
        last_seen INTEGER NOT NULL,
        status TEXT DEFAULT 'active'
      )

      CREATE TABLE IF NOT EXISTS assignments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        agent_name TEXT NOT NULL,
        task_type TEXT NOT NULL,
        target TEXT NOT NULL,
        assigned_at INTEGER NOT NULL,
        status TEXT DEFAULT 'pending',
        FOREIGN KEY (agent_name) REFERENCES agents(name)
      )
    `);
  }

  // Receive heartbeat from agent
  async handleHeartbeat(data: AgentHeartbeat) {
    this.ctx.storage.sql.exec(
      `INSERT OR REPLACE INTO agents
       (name, health, position, current_task, last_seen, status)
       VALUES (?, ?, ?, ?, ?, ?)`,
      data.agentName,
      data.health,
      JSON.stringify(data.position),
      data.currentTask || null,
      Date.now(),
      data.status || 'active'
    );
  }

  // Get all active agents
  async getActiveAgents() {
    const agents = this.ctx.storage.sql.exec(`
      SELECT * FROM agents
      WHERE last_seen > ? AND status = 'active'
      ORDER BY last_seen DESC
    `, Date.now() - 60000).toArray(); // Active within last 60 seconds

    return agents.map(row => ({
      name: row.name,
      health: row.health,
      position: JSON.parse(row.position),
      currentTask: row.current_task,
      lastSeen: row.last_seen
    }));
  }

  // Find nearest agent to position
  async findNearestAgent(x: number, y: number, z: number) {
    const agents = await this.getActiveAgents();

    let nearest = null;
    let nearestDistance = Infinity;

    for (const agent of agents) {
      const pos = agent.position;
      const distance = Math.sqrt(
        Math.pow(x - pos.x, 2) +
        Math.pow(y - pos.y, 2) +
        Math.pow(z - pos.z, 2)
      );

      if (distance < nearestDistance) {
        nearest = agent;
        nearestDistance = distance;
      }
    }

    return nearest;
  }

  // Assign task to agent
  async assignTask(agentName: string, task: Task) {
    // Update agent's current task
    this.ctx.storage.sql.exec(
      "UPDATE agents SET current_task = ? WHERE name = ?",
      JSON.stringify(task), agentName
    );

    // Record assignment
    this.ctx.storage.sql.exec(
      "INSERT INTO assignments (agent_name, task_type, target, assigned_at) VALUES (?, ?, ?, ?)",
      agentName, task.type, JSON.stringify(task.target), Date.now()
    );

    // Notify agent
    const agentId = env.AGENTS.idFromName(agentName);
    const agentStub = env.AGENTS.get(agentId);

    await agentStub.fetch(new Request('http://agent/assign', {
      method: 'POST',
      body: JSON.stringify(task)
    }));
  }

  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;

    switch (path) {
      case "/heartbeat":
        const data = await request.json();
        await this.handleHeartbeat(data);
        return Response.json({ received: true });

      case "/agents":
        const agents = await this.getActiveAgents();
        return Response.json({ agents });

      case "/nearest":
        const { x, y, z } = Object.fromEntries(url.searchParams);
        const nearest = await this.findNearestAgent(
          parseFloat(x), parseFloat(y), parseFloat(z)
        );
        return Response.json({ nearest });

      case "/assign":
        const { agentName, task } = await request.json();
        await this.assignTask(agentName, task);
        return Response.json({ assigned: true });

      default:
        return new Response("Not found", { status: 404 });
    }
  }
}
```

### Example 3: Mission Storage and Retrieval

```typescript
interface Mission {
  id: string;
  name: string;
  description: string;
  objectives: Objective[];
  status: 'planned' | 'active' | 'completed' | 'failed';
  assignedAgents: string[];
  createdAt: number;
  updatedAt: number;
}

interface Objective {
  id: string;
  type: 'mine' | 'build' | 'explore' | 'defend';
  target: any;
  status: 'pending' | 'in_progress' | 'completed';
  assignedTo?: string;
}

export class MissionControl extends DurableObject {
  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);

    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS missions (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        description TEXT,
        status TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      )

      CREATE TABLE IF NOT EXISTS objectives (
        id TEXT PRIMARY KEY,
        mission_id TEXT NOT NULL,
        type TEXT NOT NULL,
        target TEXT NOT NULL,
        status TEXT NOT NULL,
        assigned_to TEXT,
        FOREIGN KEY (mission_id) REFERENCES missions(id)
      )

      CREATE TABLE IF NOT EXISTS mission_agents (
        mission_id TEXT NOT NULL,
        agent_name TEXT NOT NULL,
        assigned_at INTEGER NOT NULL,
        PRIMARY KEY (mission_id, agent_name),
        FOREIGN KEY (mission_id) REFERENCES missions(id)
      )
    `);
  }

  // Create new mission
  async createMission(mission: Omit<Mission, 'id' | 'createdAt' | 'updatedAt'>) {
    const id = crypto.randomUUID();
    const now = Date.now();

    const newMission: Mission = {
      id,
      ...mission,
      status: 'planned',
      createdAt: now,
      updatedAt: now
    };

    this.ctx.storage.sql.exec(
      "INSERT INTO missions (id, name, description, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
      id, newMission.name, newMission.description, newMission.status, now, now
    );

    // Insert objectives
    for (const obj of newMission.objectives) {
      this.ctx.storage.sql.exec(
        "INSERT INTO objectives (id, mission_id, type, target, status) VALUES (?, ?, ?, ?, ?)",
        obj.id, id, obj.type, JSON.stringify(obj.target), obj.status
      );
    }

    return newMission;
  }

  // Get mission by ID
  async getMission(id: string): Promise<Mission | null> {
    const mission = this.ctx.storage.sql.exec(
      "SELECT * FROM missions WHERE id = ?",
      id
    ).one();

    if (!mission) return null;

    const objectives = this.ctx.storage.sql.exec(
      "SELECT * FROM objectives WHERE mission_id = ?",
      id
    ).toArray();

    const agents = this.ctx.storage.sql.exec(
      "SELECT agent_name FROM mission_agents WHERE mission_id = ?",
      id
    ).toArray();

    return {
      id: mission.id,
      name: mission.name,
      description: mission.description,
      status: mission.status,
      objectives: objectives.map(obj => ({
        id: obj.id,
        type: obj.type,
        target: JSON.parse(obj.target),
        status: obj.status,
        assignedTo: obj.assigned_to
      })),
      assignedAgents: agents.map(a => a.agent_name),
      createdAt: mission.created_at,
      updatedAt: mission.updated_at
    };
  }

  // Assign agent to mission
  async assignAgentToMission(missionId: string, agentName: string) {
    this.ctx.storage.sql.exec(
      "INSERT OR IGNORE INTO mission_agents (mission_id, agent_name, assigned_at) VALUES (?, ?, ?)",
      missionId, agentName, Date.now()
    );

    // Notify agent
    const mission = await this.getMission(missionId);
    // Send mission details to agent...
  }

  // Update objective status
  async updateObjective(objectiveId: string, status: string) {
    this.ctx.storage.sql.exec(
      "UPDATE objectives SET status = ? WHERE id = ?",
      status, objectiveId
    );

    // Check if all objectives are complete
    const objective = this.ctx.storage.sql.exec(
      "SELECT mission_id FROM objectives WHERE id = ?",
      objectiveId
    ).one();

    const pendingCount = this.ctx.storage.sql.exec(
      "SELECT COUNT(*) as count FROM objectives WHERE mission_id = ? AND status != 'completed'",
      objective.mission_id
    ).one().count;

    if (pendingCount === 0) {
      // Mark mission as complete
      this.ctx.storage.sql.exec(
        "UPDATE missions SET status = 'completed', updated_at = ? WHERE id = ?",
        Date.now(), objective.mission_id
      );
    }
  }
}
```

### Example 4: Real-Time Telemetry Logging

```typescript
export class TelemetryLogger extends DurableObject {
  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);

    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS telemetry_events (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        agent_name TEXT NOT NULL,
        event_type TEXT NOT NULL,
        data TEXT NOT NULL,
        timestamp INTEGER NOT NULL
      )

      CREATE INDEX IF NOT EXISTS idx_agent_time ON telemetry_events(agent_name, timestamp)
      CREATE INDEX IF NOT EXISTS idx_event_type ON telemetry_events(event_type, timestamp)
    `);
  }

  // Log event
  async log(agentName: string, eventType: string, data: any) {
    this.ctx.storage.sql.exec(
      "INSERT INTO telemetry_events (agent_name, event_type, data, timestamp) VALUES (?, ?, ?, ?)",
      agentName, eventType, JSON.stringify(data), Date.now()
    );
  }

  // Query agent events
  async getAgentEvents(agentName: string, limit = 100) {
    return this.ctx.storage.sql.exec(`
      SELECT * FROM telemetry_events
      WHERE agent_name = ?
      ORDER BY timestamp DESC
      LIMIT ?
    `, agentName, limit).toArray();
  }

  // Query by event type
  async getEventsByType(eventType: string, startTime?: number, endTime?: number) {
    let query = "SELECT * FROM telemetry_events WHERE event_type = ?";
    const params: any[] = [eventType];

    if (startTime) {
      query += " AND timestamp >= ?";
      params.push(startTime);
    }

    if (endTime) {
      query += " AND timestamp <= ?";
      params.push(endTime);
    }

    query += " ORDER BY timestamp DESC LIMIT 1000";

    return this.ctx.storage.sql.exec(query, ...params).toArray();
  }

  // Get agent statistics
  async getAgentStats(agentName: string) {
    return this.ctx.storage.sql.exec(`
      SELECT
        event_type,
        COUNT(*) as count,
        MIN(timestamp) as first_seen,
        MAX(timestamp) as last_seen
      FROM telemetry_events
      WHERE agent_name = ?
      GROUP BY event_type
    `, agentName).toArray();
  }

  // Cleanup old events
  async cleanup(olderThanMs: number = 7 * 24 * 60 * 60 * 1000) { // 7 days
    const cutoff = Date.now() - olderThanMs;

    this.ctx.storage.sql.exec(
      "DELETE FROM telemetry_events WHERE timestamp < ?",
      cutoff
    );
  }
}

// Usage in agent:
export class SteveAgent extends DurableObject {
  async logEvent(type: string, data: any) {
    const telemetryId = env.TELEMETRY.idFromName("global-telemetry");
    const telemetryStub = env.TELEMETRY.get(telemetryId);

    await telemetryStub.fetch(new Request('http://telemetry/log', {
      method: 'POST',
      body: JSON.stringify({
        agentName: this.name,
        eventType: type,
        data: data
      })
    }));
  }

  async moveTo(x: number, y: number, z: number) {
    const oldPos = this.state.position;
    this.state.position = { x, y, z };

    await this.logEvent('move', {
      from: oldPos,
      to: { x, y, z },
      distance: Math.sqrt(
        Math.pow(x - oldPos.x, 2) +
        Math.pow(y - oldPos.y, 2) +
        Math.pow(z - oldPos.z, 2)
      )
    });

    await this.saveState();
  }
}
```

---

## 7. Migrations and Schema

### wrangler.toml Migration Syntax

```toml
name = "steve-agents"
main = "src/index.ts"
compatibility_date = "2024-12-01"
compatibility_flags = ["nodejs_compat"]

# Durable Objects bindings
[[durable_objects.bindings]]
name = "AGENTS"
class_name = "SteveAgent"

[[durable_objects.bindings]]
name = "FOREMAN"
class_name = "Foreman"

# Migrations
[[migrations]]
tag = "v1"          # Version identifier
new_sqlite_classes = ["SteveAgent", "Foreman"]  # New classes with SQLite

[[migrations]]
tag = "v2"
new_sqlite_classes = ["MissionControl"]

[[migrations]]
tag = "v3"
renamed_classes = [
  { old = "SteveAgent", new = "Agent" }
]

[[migrations]]
tag = "v4"
deleted_classes = ["LegacyTaskQueue"]
```

### Migration Rules

| Option | Purpose | Example |
|--------|---------|---------|
| `new_sqlite_classes` | Create new SQLite-backed DO classes | `new_sqlite_classes = ["Agent"]` |
| `new_classes` | Create new KV-style DO classes (paid only) | `new_classes = ["LegacyDO"]` |
| `renamed_classes` | Rename existing classes | `renamed_classes = [{ old: "Agent", new: "Steve" }]` |
| `deleted_classes` | Delete classes and cleanup data | `deleted_classes = ["OldDO"]` |

### Migration Best Practices

1. **Always use `new_sqlite_classes`** for new Durable Objects (includes free tier support)
2. **Tag versions sequentially** (v1, v2, v3) for easy rollback
3. **Don't skip tags** when deploying
4. **Test migrations locally** with `wrangler dev`

### Version Management

```bash
# Deploy with migration
wrangler deploy

# Rollback to previous version
wrangler rollback --version v2

# View migration status
wrangler migrations list
```

### Schema Evolution

```typescript
// Version 1: Basic state
interface StateV1 {
  health: number;
  position: { x: number; y: number; z: number };
}

// Version 2: Added inventory
interface StateV2 {
  health: number;
  position: { x: number; y: number; z: number };
  inventory: InventoryItem[];
}

// Migration handler
export class Agent extends DurableObject {
  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);

    const version = await this.ctx.storage.get<number>("schema_version") || 1;

    if (version === 1) {
      await this.migrateV1ToV2();
      await this.ctx.storage.put("schema_version", 2);
    }
  }

  private async migrateV1ToV2() {
    const v1State = await this.ctx.storage.get<StateV1>("state");

    const v2State: StateV2 = {
      ...v1State,
      inventory: []
    };

    await this.ctx.storage.put("state", v2State);
  }
}
```

---

## 8. Alarm API for Scheduled Tasks

### Setting Alarms

```typescript
export class Agent extends DurableObject {
  // Schedule alarm for future execution
  async scheduleTask(delayMs: number, task: Task) {
    // Store task for alarm to process
    await this.ctx.storage.put(`task_${task.id}`, task);

    // Schedule alarm
    await this.ctx.storage.setAlarm(Date.now() + delayMs);
  }

  // Alarm handler (called when alarm triggers)
  async alarm() {
    // Get pending tasks
    const tasks = await this.ctx.storage.list<Task>({
      prefix: "task_"
    });

    for (const [key, task] of tasks) {
      await this.executeTask(task);

      // Clean up processed task
      await this.ctx.storage.delete(key);
    }

    // Schedule next alarm if needed
    await this.scheduleHeartbeat();
  }

  private async scheduleHeartbeat() {
    // Send heartbeat every 30 seconds
    await this.ctx.storage.setAlarm(Date.now() + 30000);
  }
}
```

### Alarm Best Practices

1. **Only one alarm at a time** per Durable Object
2. **Must re-schedule** if you want recurring execution
3. **Alarms persist** across object evictions
4. **Use for**: Heartbeats, cleanup, polling, delayed execution

---

## 9. WebSocket Hibernation

### WebSocket Pattern for Agent Communication

```typescript
export class Agent extends DurableObject {
  private sessions: Set<WebSocket> = new Set();
  private storage: DurableObjectStorage;

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    this.storage = ctx.storage;

    // Implement WebSocket hibernation
    ctx.getWebSockets().accept().setWebSocketHandler({
      message: (ws, data) => this.handleMessage(ws, data),
      close: (ws, code, reason) => this handleClose(ws, code, reason)
    });
  }

  private async handleMessage(ws: WebSocket, data: ArrayBuffer | string) {
    const message = JSON.parse(data.toString());

    switch (message.type) {
      case 'subscribe':
        // Add client to sessions
        this.sessions.add(ws);
        ws.send(JSON.stringify({ type: 'subscribed', agentId: this.id.toString() }));
        break;

      case 'command':
        // Execute command
        await this.executeCommand(message.command);
        this.broadcast({ type: 'state_update', state: this.state });
        break;
    }
  }

  private broadcast(data: any) {
    const message = JSON.stringify(data);
    for (const ws of this.sessions) {
      try {
        ws.send(message);
      } catch (e) {
        // Session closed
        this.sessions.delete(ws);
      }
    }
  }

  private handleClose(ws: WebSocket, code: number, reason: string) {
    this.sessions.delete(ws);
  }

  async fetch(request: Request): Promise<Response> {
    // Upgrade to WebSocket
    if (request.headers.get("Upgrade") === "websocket") {
      return this.ctx.getWebSockets().fetch(request);
    }

    return new Response("Expected WebSocket", { status: 400 });
  }
}
```

---

## 10. Comparison with Alternatives

### Durable Objects vs Redis

| Feature | Durable Objects | Redis |
|---------|----------------|-------|
| Global distribution | Built-in | Requires setup |
| Strong consistency | Yes (per object) | Depends on config |
| Persistence | Automatic (SQLite) | Requires RDB/AOF |
| WebSocket support | Native | Requires Pub/Sub |
| Compute included | Yes | No (needs separate app) |
| Free tier | Yes (5GB) | Limited (usually 30MB) |

### Durable Objects vs Database Connection Pooling

| Feature | Durable Objects | Traditional DB |
|---------|----------------|----------------|
| Cold starts | Minimal (objects stay warm) | Connection overhead |
| State locality | Co-located with compute | Network round-trip |
| Horizontal scaling | Automatic | Requires connection pooling |
| Transaction scope | Single object | Global transactions |

---

## 11. Use Cases for Steve AI

### Recommended Architecture

```
┌─────────────────┐
│   Minecraft     │
│    Server       │
└────────┬────────┘
         │ WebSocket
         ▼
┌─────────────────────────────────────┐
│     Cloudflare Workers (Edge)       │
│                                     │
│  ┌──────────┐  ┌──────────────┐    │
│  │ Routing  │─▶│ Agent (DO)   │    │
│  │ Worker   │  │ - Steve-42   │    │
│  └──────────┘  │ - Steve-43   │    │
│       │        └──────────────┘    │
│       ▼                 ▲          │
│  ┌──────────────┐       │          │
│  │  Foreman     │───────┘          │
│  │  (DO)        │                  │
│  └──────────────┘                  │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────┐
│   OpenAI API    │
│   (for LLM)     │
└─────────────────┘
```

### State Distribution

| Component | Storage Strategy |
|-----------|------------------|
| Agent State (HP, pos, inventory) | Agent DO (SQLite) |
| Task Queue | Agent DO (SQLite) |
| World Knowledge | Shared KV or R2 |
| Agent Registry | Foreman DO (SQLite) |
| Mission History | MissionControl DO (SQLite) |

---

## 12. Key Takeaways

1. **Use `idFromName()` for agent identity** - Ensures same name = same DO instance
2. **SQLite storage is recommended** - 10GB per object, free tier available
3. **Single alarm per object** - Must reschedule for recurring tasks
4. **Strong consistency within object** - No race conditions within one agent
5. **Global distribution is automatic** - Agents routed to nearest instance
6. **WebSocket hibernation** - Keep connections open while object sleeps
7. **Migrations are required** - Add classes via wrangler.toml migrations

---

## Sources

- [What are Durable Objects? - Cloudflare Docs](https://developers.cloudflare.com/durable-objects/concepts/what-are-durable-objects/)
- [Getting Started - Cloudflare Durable Objects](https://developers.cloudflare.com/durable-objects/get-started/)
- [SQLite in Durable Objects - Cloudflare Blog](https://blog.cloudflare.com/sqlite-in-durable-objects/)
- [Building Agents with OpenAI + Cloudflare Agents SDK](https://blog.cloudflare.com/building-agents-with-openai-and-cloudflares-agents-sdk/)
- [Durable Object Namespace API](https://developers.cloudflare.com/durable-objects/api/namespace/)
- [Durable Object ID API](https://developers.cloudflare.com/durable-objects/api/id/)
- [Rules of Durable Objects](https://developers.cloudflare.com/durable-objects/best-practices/rules-of-durable-objects/)
- [Agent Class Internals - Cloudflare Docs](https://developers.cloudflare.com/agents/concepts/agent-class/)
- [Build a Rate Limiter Example](https://developers.cloudflare.com/durable-objects/examples/build-a-rate-limiter/)
- [Durable Objects Pricing](https://developers.cloudflare.com/durable-objects/platform/pricing/)

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Next Review:** 2026-03-27
