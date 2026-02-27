# Cloudflare AI Gateway Research Document

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Research Focus:** Centralized AI Observability for Minecraft Agent Systems

---

## Table of Contents

1. [AI Gateway Overview](#1-ai-gateway-overview)
2. [Configuration](#2-configuration)
3. [Observability Features](#3-observability-features)
4. [Caching and Rate Limiting](#4-caching-and-rate-limiting)
5. [Integration with Workers](#5-integration-with-workers)
6. [Dashboard and Alerts](#6-dashboard-and-alerts)
7. [Minecraft Agent Use Cases](#7-minecraft-agent-use-cases)
8. [Cost Analysis](#8-cost-analysis)
9. [Integration Guide](#9-integration-guide)
10. [References](#10-references)

---

## 1. AI Gateway Overview

### What is AI Gateway?

Cloudflare AI Gateway is a centralized management and control layer for AI applications that sits between your applications and AI service providers. It provides comprehensive observability, security, and optimization features for AI workloads.

**Core Capabilities:**
- Centralized logging and monitoring across all AI providers
- Request/response caching for cost optimization
- Rate limiting and access control
- Multi-provider model fallback and retry
- Unified analytics dashboard
- Cost tracking and budget management

### Supported Providers

| Provider | Endpoint Suffix | Status |
|----------|----------------|--------|
| Workers AI | `workersai` | Native Support |
| OpenAI | `openai` | Native Support |
| Anthropic (Claude) | `anthropic` | Native Support |
| Google AI Studio/Gemini | `google` | Native Support |
| Azure OpenAI | `azure` | Native Support |
| Hugging Face | `huggingface` | Native Support |
| Amazon Bedrock | `bedrock` | Native Support |
| Replicate | `replicate` | Native Support |
| DeepSeek | `deepseek` | Native Support |
| Custom/OpenAI-Compatible | `compat` | Universal Endpoint |

### Centralized Logging and Monitoring

AI Gateway provides a unified view of all AI interactions:

**Real-Time Metrics:**
- Request volume and patterns
- Response times and latency percentiles
- Error rates and failure analysis
- Token consumption trends
- Cost accumulation per provider/model

**Comprehensive Logging:**
- Complete request/response payloads
- Timestamps and duration metrics
- Provider and model information
- User/application metadata
- Status codes and error details

### Cost Tracking

**Unified Billing (Closed Beta):**
- Single account management across all providers
- Add credits to Cloudflare account instead of individual provider accounts
- Real-time usage statistics and credit management
- Monthly invoicing with transparent pricing
- Transaction fees only when loading credits

**Traditional BYO Key (Bring Your Own Key):**
- Continue using existing provider API keys
- Enhanced security through Cloudflare Secrets Store
- Keys encrypted with AES-256
- Full analytics visibility without changing billing

---

## 2. Configuration

### Gateway Setup in Cloudflare Dashboard

**Step-by-Step Creation:**

1. Navigate to **Cloudflare Dashboard** → **AI Gateway**
2. Click **Create** or **Add Gateway**
3. Name your gateway (e.g., `minecraft-steve-ai`)
4. Select providers to configure
5. Add API keys or enable Unified Billing
6. Configure optional settings (caching, rate limiting)

**Configuration Options:**

| Setting | Description | Default |
|---------|-------------|---------|
| Gateway Name | Unique identifier for your gateway | Required |
| Provider | AI service provider(s) to connect | At least one |
| API Key | Provider authentication key | Required for BYO |
| Cache TTL | Duration to cache responses (seconds) | 3360 (56 min) |
| Retry Count | Number of retry attempts on failure | 5 |
| Fallback Strategy | Load balancing or fallback mode | fallback |

### API Endpoint Structure

**Base URL Format:**
```
https://gateway.ai.cloudflare.com/v1/{account_id}/{gateway_name}
```

**Provider-Specific Endpoints:**
```
https://gateway.ai.cloudflare.com/v1/{account_id}/{gateway_name}/openai
https://gateway.ai.cloudflare.com/v1/{account_id}/{gateway_name}/anthropic
https://gateway.ai.cloudflare.com/v1/{account_id}/{gateway_name}/google
```

**Universal/Compat Endpoint (for OpenAI-compatible APIs):**
```
https://gateway.ai.cloudflare.com/v1/{account_id}/{gateway_name}/compat
```

**Example for Minecraft MineWright AI:**
```
Account ID: abc123def456
Gateway Name: minecraft-steve-ai
OpenAI Endpoint: https://gateway.ai.cloudflare.com/v1/abc123def456/minecraft-steve-ai/openai
```

### Authentication Methods

**Method 1: Provider API Key (BYO)**
```bash
# Set environment variable
export OPENAI_API_KEY="sk-..."
export OPENAI_BASE_URL="https://gateway.ai.cloudflare.com/v1/{account_id}/{gateway_name}/openai"
```

**Method 2: Cloudflare Secrets Store**
```javascript
// In wrangler.toml
[vars]
OPENAI_API_KEY = "sk-..."

// In Worker code
const apiKey = env.OPENAI_API_KEY;
```

**Method 3: Unified Billing**
```javascript
// No provider API key needed
// Charges go to Cloudflare account credits
const response = await fetch("https://gateway.ai.cloudflare.com/v1/{account_id}/{gateway_name}/openai/v1/chat/completions", {
  method: "POST",
  headers: {
    "Authorization": `Bearer ${env.AI_GATEWAY_API_TOKEN}`,
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    model: "gpt-4",
    messages: [...]
  })
});
```

### Routing Rules

**Dynamic Routing Configuration:**

AI Gateway supports intelligent request routing based on:

- **Model Type:** Route specific models to designated providers
- **Cost Threshold:** Switch to cheaper models when appropriate
- **Performance:** Route based on latency requirements
- **Availability:** Automatic failover to backup providers

**JSON Configuration Example:**
```json
{
  "strategy": { "mode": "fallback" },
  "retry": { "count": 3 },
  "targets": [
    {
      "provider": "openai",
      "api_key": "sk-xxx",
      "override_params": { "model": "gpt-4" }
    },
    {
      "provider": "anthropic",
      "api_key": "sk-ant-xxx",
      "override_params": { "model": "claude-3-5-sonnet-20241022" }
    },
    {
      "provider": "google",
      "api_key": "gt5xxx",
      "override_params": { "model": "gemini-pro" }
    }
  ]
}
```

**Routing Priority Hierarchy:**
1. Request-level configuration (highest)
2. Gateway-level configuration
3. Provider defaults (lowest)

---

## 3. Observability Features

### Request/Response Logging

**Comprehensive Logging Capabilities:**

| Field | Description | Example |
|-------|-------------|---------|
| Request ID | Unique identifier for tracing | `req_abc123` |
| Timestamp | Request start time | `2026-02-27T10:30:00Z` |
| Duration | Request processing time | `1234ms` |
| Provider | AI provider used | `openai` |
| Model | Specific model invoked | `gpt-4` |
| Prompt | Input text/tokens | `"Build a house..."` |
| Response | Output text/tokens | `"I will build..."` |
| Tokens Used | Input + output token count | `1500` |
| Cost | Request cost in USD | `$0.03` |
| Status | Success/failure status | `200 OK` |
| Metadata | Custom tracking fields | `{"agent": "steve-1", "task": "mining"}` |

**Log Access Methods:**

1. **Dashboard UI:** View logs in Cloudflare dashboard
2. **Logpush:** Export logs to external storage (Workers Paid)
3. **API:** Query logs programmatically
4. **Worker Binding:** Access logs from Workers code

### Latency Metrics

**Measured Metrics:**

| Metric | Description | Use Case |
|--------|-------------|----------|
| Total Duration | End-to-end request time | Performance monitoring |
| Provider Latency | Time spent at AI provider | SLA tracking |
| Gateway Overhead | Processing time at Gateway | Optimization |
| Cache Lookup | Time to check cache | Caching efficiency |
| P50 Latency | Median response time | Baseline performance |
| P95 Latency | 95th percentile | SLA compliance |
| P99 Latency | 99th percentile | Worst-case analysis |

**Visualization:**
- Real-time latency charts
- Historical trends (hourly, daily, weekly, monthly)
- Per-model latency comparison
- Geographic breakdown (by PoP)

### Token Usage Tracking

**Token Metrics:**

| Metric | Description |
|--------|-------------|
| Input Tokens | Tokens sent to model (prompt) |
| Output Tokens | Tokens received from model |
| Total Tokens | Input + Output |
| Tokens per Second | Throughput measurement |
| Cost per 1K Tokens | Pricing analysis |
| Token Budget | Remaining allowance |

**Token Analysis Features:**
- Real-time token consumption counters
- Token usage by model/provider
- Cost projection based on current rate
- Token efficiency metrics (tokens per task)
- Historical token usage trends

### Error Rate Monitoring

**Error Tracking:**

| Error Type | Monitoring |
|------------|------------|
| Provider Errors | 5xx errors from AI providers |
| Rate Limiting | Requests blocked by rate limits |
| Timeout Errors | Requests exceeding time limits |
| Validation Errors | Malformed requests/responses |
| Authentication Failures | Invalid API keys or tokens |

**Error Analysis:**
- Error rate by provider
- Error frequency over time
- Error categorization and root cause
- Automated alerting on error thresholds
- Error log retention (100K-200K logs)

### Cost Analytics

**Cost Tracking Features:**

**Per-Request Cost:**
- Exact cost for each API call
- Cost breakdown by provider/model
- Token-based cost calculation
- Cache hit cost savings

**Aggregated Analytics:**
- Daily/weekly/monthly cost summaries
- Cost by application/agent
- Cost trends and forecasting
- Budget alerts and thresholds

**Cost Optimization Insights:**
- Most expensive models
- Cost per task type
- Cache effectiveness (savings)
- Opportunity analysis (cheaper alternatives)

**Example Cost Report:**
```
Period: Last 7 Days
Total Cost: $12.45

By Provider:
  OpenAI (GPT-4):    $8.50 (68.3%)
  Anthropic (Claude): $3.20 (25.7%)
  Workers AI:         $0.75 (6.0%)

By Agent:
  steve-1:  $5.20 (mining, building)
  steve-2:  $4.30 (farming, trading)
  steve-3:  $2.95 (exploration)

Cache Savings: $3.40 (21.5% reduction)
```

---

## 4. Caching and Rate Limiting

### Response Caching

**Caching Strategy:**

AI Gateway caches identical requests to reduce costs and latency. Cache keys are based on:
- Provider
- Model
- Request parameters (prompt, temperature, max_tokens, etc.)

**Configuration Options:**

| Parameter | Description | Default | Range |
|-----------|-------------|---------|-------|
| `cacheTtl` | Cache duration in seconds | 3360 (56 min) | 60-86400 |
| `skipCache` | Bypass cache for this request | false | boolean |
| `cacheEnabled` | Enable/disable caching globally | true | boolean |

**Usage Example:**
```javascript
const response = await env.AI.run(
  "@cf/meta/llama-3.1-8b-instruct",
  { prompt: "Build a cobblestone house" },
  {
    gateway: {
      id: "minecraft-steve-ai",
      skipCache: false,
      cacheTtl: 7200,  // 2 hours
    },
  }
);
```

**Cache Analytics:**
- Cache hit rate percentage
- Average response time with cache
- Cost savings from caching
- Cache hit/miss trends over time

**Best Practices for Minecraft Agents:**
- Cache navigation plans (reusable paths)
- Cache building templates
- Don't cache: real-time combat responses
- Don't cache: player-specific context

### Rate Limiting Configuration

**Default Limits:**

| Plan | Default QPS | Customizable |
|------|-------------|--------------|
| Workers Free | 100 QPS | No |
| Workers Paid | 100 QPS | Yes |
| Enterprise | Custom | Yes |

**Rate Limiting Strategies:**

**1. Global Rate Limit:**
```json
{
  "rate_limit": {
    "qps": 50,
    "burst": 100
  }
}
```

**2. Per-Agent Rate Limit:**
```javascript
// Track usage per Steve agent
{
  "rate_limit": {
    "key": "{{agent_id}}",
    "qps": 10,
    "burst": 20
  }
}
```

**3. Per-Task Rate Limit:**
```javascript
// Different limits for different task types
{
  "rate_limits": {
    "mining": { "qps": 20 },
    "building": { "qps": 5 },
    "combat": { "qps": 30 }
  }
}
```

**Rate Limiting Metrics:**
- Requests allowed vs. blocked
- Rate limit by agent/task
- Peak usage periods
- Rate limit efficiency

### Fallback Model Configuration

**Automatic Fallback:**

AI Gateway can automatically switch to alternative providers/models when:
- Primary provider returns errors
- Request times out
- Rate limit exceeded
- Model is unavailable

**Fallback Configuration:**

```json
{
  "strategy": { "mode": "fallback" },
  "retry": { "count": 3 },
  "targets": [
    {
      "provider": "openai",
      "priority": 1,
      "override_params": { "model": "gpt-4" }
    },
    {
      "provider": "anthropic",
      "priority": 2,
      "override_params": { "model": "claude-3-5-sonnet-20241022" }
    },
    {
      "provider": "workersai",
      "priority": 3,
      "override_params": { "model": "@cf/meta/llama-3.1-8b-instruct" }
    }
  ]
}
```

**Fallback Metrics:**
- Primary vs. fallback usage ratio
- Average failover time
- Success rate per provider
- Cost impact of fallbacks

**Benefits for Minecraft Agents:**
- **High Availability:** 99.9%+ uptime (vs 95% single provider)
- **Cost Optimization:** Fallback to cheaper models when appropriate
- **Geographic Performance:** Route to nearest provider
- **Load Balancing:** Distribute load across providers

---

## 5. Integration with Workers

### Gateway Binding in wrangler.toml

**Basic Configuration:**

```toml
name = "minecraft-steve-ai"
main = "src/index.js"
compatibility_date = "2024-01-01"

[ai]
binding = "AI"
gateway = "minecraft-steve-ai"

# Optional: Explicit gateway configuration
[ai.gateway]
id = "minecraft-steve-ai"
cache_ttl = 3600
skip_cache = false
```

**Advanced Configuration with Multiple Bindings:**

```toml
[[ai]]
binding = "AI"
gateway = "minecraft-steve-ai"

[[ai]]
binding = "AI_FALLBACK"
gateway = "minecraft-steve-ai-fallback"

# Environment-specific settings
[env.production.ai]
binding = "AI"
gateway = "minecraft-steve-ai-prod"

[env.development.ai]
binding = "AI"
gateway = "minecraft-steve-ai-dev"
```

**Secrets Configuration:**

```toml
# wrangler.toml (for local development)
[vars]
AI_GATEWAY_ACCOUNT_ID = "abc123def456"
AI_GATEWAY_NAME = "minecraft-steve-ai"

# Use `wrangler secret` for production
# wrangler secret put OPENAI_API_KEY
```

### Using gateway.ai.run() Pattern

**Basic Usage:**

```javascript
export default {
  async fetch(request, env) {
    const response = await env.AI.run(
      "@cf/meta/llama-3.1-8b-instruct",
      {
        prompt: "Build a 5x5 cobblestone house at coordinates (100, 64, 200)"
      },
      {
        gateway: {
          id: "minecraft-steve-ai",
          skipCache: false,
          cacheTtl: 1800,
        },
      }
    );

    return new Response(JSON.stringify(response));
  },
};
```

**Advanced Usage with Multiple Models:**

```javascript
// Task planning with GPT-4
const plan = await env.AI.run(
  "openai/gpt-4",
  {
    messages: [
      { role: "system", content: "You are a Minecraft task planner." },
      { role: "user", content: taskDescription }
    ],
    temperature: 0.7,
    max_tokens: 500
  },
  {
    gateway: {
      id: "minecraft-steve-ai",
      metadata: {
        agent_id: "steve-1",
        task_type: "planning",
        urgency: "normal"
      }
    }
  }
);

// Fast execution with Workers AI
const action = await env.AI.run(
  "@cf/meta/llama-3.1-8b-instruct",
  { prompt: immediateAction },
  {
    gateway: {
      id: "minecraft-steve-ai",
      skipCache: true  // Real-time response needed
    }
  }
);
```

### Custom Metadata for Tracking

**Metadata Types:**

```javascript
// Agent tracking
{
  gateway: {
    id: "minecraft-steve-ai",
    metadata: {
      agent_id: "steve-1",
      agent_level: 5,
      agent_xp: 12500
    }
  }
}

// Task tracking
{
  gateway: {
    id: "minecraft-steve-ai",
    metadata: {
      task_type: "mining",
      target_block: "diamond_ore",
      location: "overworld"
    }
  }
}

// Performance tracking
{
  gateway: {
    id: "minecraft-steve-ai",
    metadata: {
      game_tick: 12345,
      server_tps: 19.8,
      player_count: 5
    }
  }
}

// Cost allocation
{
  gateway: {
    id: "minecraft-steve-ai",
    metadata: {
      cost_center: "mining_ops",
      billing_code: "minecraft-server-1"
    }
  }
}
```

**Worker Binding Methods:**

```javascript
// Access gateway binding
const gateway = env.AI.gateway("minecraft-steve-ai");

// Patch log with feedback
await gateway.patchLog(logId, {
  feedback: "positive",
  score: 0.95,
  metadata: {
    task_success: true,
    time_to_complete: 45,
    blocks_placed: 25
  }
});

// Retrieve specific log
const log = await gateway.getLog(logId);
console.log(log);
```

---

## 6. Dashboard and Alerts

### Real-Time Monitoring UI

**Dashboard Features:**

**Overview Tab:**
- Total requests (last 24h, 7d, 30d)
- Average response time
- Success rate
- Total cost
- Active providers

**Analytics Tab:**
- Request volume over time
- Token usage trends
- Cost breakdown by model
- Latency percentiles
- Error rate chart

**Logs Tab:**
- Individual request logs
- Filter by provider, model, status
- Search prompts and responses
- Export logs (CSV, JSON)

**Providers Tab:**
- Provider status
- Request distribution
- Cost comparison
- Performance metrics

**Dashboard Customization:**
- Custom time ranges
- Multiple gateways view
- Filter by metadata tags
- Favorite metrics
- Export reports

### Alert Configuration

**Alert Types:**

1. **Cost Alerts:**
   - Daily/weekly/monthly spend threshold
   - Per-model cost limit
   - Budget exhaustion warning

2. **Performance Alerts:**
   - P95 latency threshold
   - Error rate spike
   - Provider downtime

3. **Usage Alerts:**
   - Token consumption rate
   - Request volume anomaly
   - Cache hit rate drop

**Alert Configuration Example:**

```javascript
// Via Cloudflare Dashboard
{
  "name": "High Cost Alert",
  "type": "cost",
  "threshold": {
    "amount": 10.00,
    "period": "daily"
  },
  "notification": {
    "email": "admin@minecraft-server.com",
    "webhook": "https://hooks.slack.com/..."
  }
}
```

**Notification Channels:**
- Email
- Webhook (Slack, Discord, etc.)
- PagerDuty
- Cloudflare Logging

### Usage Reports

**Report Types:**

**Daily Summary:**
- Total requests and cost
- Top models by usage
- Error summary
- Cache effectiveness

**Weekly Analysis:**
- Cost trends
- Provider comparison
- Agent performance
- Optimization recommendations

**Monthly Billing:**
- Detailed invoice
- Per-provider breakdown
- Log overage charges
- Unified billing credits

**Export Formats:**
- CSV (spreadsheet compatible)
- JSON (API integration)
- PDF (executive summary)

---

## 7. Minecraft Agent Use Cases

### Tracking Agent "Thoughts" in Real-Time

**Scenario:** Steve-1 is deciding how to mine diamonds at coordinates (100, 45, 200)

**Implementation:**

```javascript
// Send planning request with rich metadata
const plan = await openai.chat.completions.create({
  model: "gpt-4",
  messages: [
    {
      role: "system",
      content: "You are Steve, a Minecraft agent planning mining operations."
    },
    {
      role: "user",
      content: "Plan to mine diamonds at (100, 45, 200). Current inventory: iron pickaxe, 3 torches."
    }
  ]
}, {
  gateway: {
    id: "minecraft-steve-ai",
    metadata: {
      agent_id: "steve-1",
      agent_state: "planning",
      task_type: "mining",
      target_block: "diamond_ore",
      urgency: "normal",
      location: { x: 100, y: 45, z: 200 },
      inventory_slots_free: 15,
      nearby_hazards: ["lava lake"],
      game_tick: 12345
    }
  }
});
```

**Dashboard View:**
```
Real-Time Agent Thoughts - steve-1
Time: 2026-02-27 10:30:15
Task: Mining diamonds at (100, 45, 200)
Status: PLANNING

Thought Process:
1. Assess safety: Lava lake detected, need water bucket
2. Check inventory: Iron pickaxe OK, need more torches
3. Plan route: Dig staircase from (100, 60, 200) down to level
4. Estimate time: ~45 seconds
5. Confidence: 85%

Cost: $0.02 | Tokens: 150 | Duration: 1.2s
```

### Cost per Decision Analysis

**Tracking Decision Costs:**

```javascript
// Decision type tracking
const decisionTypes = {
  MINING: { budget: 0.05, priority: "normal" },
  BUILDING: { budget: 0.03, priority: "low" },
  COMBAT: { budget: 0.10, priority: "critical" },
  TRADING: { budget: 0.02, priority: "low" },
  EXPLORATION: { budget: 0.04, priority: "normal" }
};

// Budget-aware decision making
const plan = await planTask(task, decisionTypes[taskType].budget);
```

**Cost Analysis Dashboard:**

| Agent | Task Type | Decisions | Avg Cost/Decision | Total Cost |
|-------|-----------|-----------|-------------------|------------|
| steve-1 | Mining | 234 | $0.023 | $5.38 |
| steve-1 | Building | 89 | $0.018 | $1.60 |
| steve-2 | Farming | 156 | $0.015 | $2.34 |
| steve-3 | Combat | 45 | $0.089 | $4.00 |

**Optimization Insights:**
- Combat decisions most expensive (complex, real-time)
- Farming decisions cheapest (repetitive, cacheable)
- Recommendation: Cache common building templates

### Latency Optimization Insights

**Latency Analysis:**

```javascript
// Track latency by task type
{
  gateway: {
    id: "minecraft-steve-ai",
    metadata: {
      task_type: "combat",
      latency_requirement: "< 500ms",
      skip_cache: true
    }
  }
}
```

**Latency Report:**

| Task Type | P50 Latency | P95 Latency | P99 Latency | Cache Hit Rate |
|-----------|-------------|-------------|-------------|----------------|
| Combat | 250ms | 450ms | 800ms | 5% |
| Mining | 800ms | 1.5s | 3.2s | 45% |
| Building | 1.2s | 2.8s | 5.1s | 68% |
| Trading | 500ms | 1.1s | 2.0s | 22% |

**Optimization Strategies:**
- Combat: Use fastest model (Workers AI), skip cache
- Mining: Cache navigation patterns, use cheaper model
- Building: High cache hit rate, optimize templates
- Trading: Moderate caching, balance speed/quality

### Training Data Collection

**Data Collection for Agent Improvement:**

```javascript
// Rich metadata for training data
{
  gateway: {
    id: "minecraft-steve-ai",
    metadata: {
      // Task context
      task_type: "mining",
      task_success: true,
      task_duration: 45,

      // Environment
      biome: "extreme_hills",
      light_level: 7,
      nearby_entities: ["zombie", "skeleton"],

      // Agent state
      agent_level: 5,
      agent_health: 18,
      agent_hunger: 14,

      // Outcome
      blocks_mined: 8,
      diamonds_found: 2,
      damage_taken: 2,
      xp_gained: 150,

      // Feedback
      human_rating: null,  // For human-in-the-loop
      autonomous_score: 0.85,

      // Training flags
      training_sample: true,
      sample_quality: "high"
    }
  }
}
```

**Training Data Export:**
```javascript
// Export successful decisions for fine-tuning
const exportLogs = async () => {
  const logs = await gateway.getLogs({
    filter: {
      "metadata.task_success": true,
      "metadata.training_sample": true,
      "metadata.sample_quality": "high"
    },
    limit: 10000
  });

  // Export to JSONL for fine-tuning
  return logs.map(log => ({
    messages: log.request.messages,
    completion: log.response.choices[0].message,
    metadata: log.metadata
  }));
};
```

---

## 8. Cost Analysis

### Pricing Structure

**AI Gateway Core Features (Free):**
- Dashboard analytics
- Caching
- Rate limiting
- Basic monitoring
- Request/response logging

**Log Storage Pricing:**

| Plan | Free Logs | Overage |
|------|-----------|---------|
| Workers Free | 100,000 total | N/A (upgrade required) |
| Workers Paid | 200,000 total | $8 per 100,000/month |

**Workers Paid Plan ($5/month):**
- 200,000 free logs stored
- 10 million requests/month
- Additional requests: $0.50 per million
- Logpush support (export logs to R2, S3, etc.)

**Unified Billing (Closed Beta):**
- Add credits to Cloudflare account
- Transparent provider pricing
- No transaction fees when loading credits
- Single monthly invoice

### Cost Optimization Strategies

**Strategy 1: Intelligent Caching**

| Scenario | Without Cache | With Cache | Savings |
|----------|---------------|------------|---------|
| Building template requests | $0.05 each | $0.00 (after first) | 90%+ |
| Navigation planning | $0.03 each | $0.00 | 85% |
| Common chat responses | $0.02 each | $0.00 | 75% |

**Strategy 2: Model Selection**

| Task | Expensive Model | Cheap Model | Savings |
|------|-----------------|-------------|---------|
| Simple mining | GPT-4 ($0.03) | Llama 3 8B ($0.0002) | 99% |
| Complex combat | GPT-4 ($0.08) | Claude Haiku ($0.01) | 87% |
| Trading | Claude Opus ($0.05) | GPT-3.5 ($0.002) | 96% |

**Strategy 3: Fallback Configuration**

Use cheaper models as fallbacks for less critical tasks:
- Primary: GPT-4 for complex planning
- Fallback: Claude 3 Sonnet for routine tasks
- Emergency: Workers AI for simple queries

### ROI Calculator

**Example Scenario:**

| Metric | Before AI Gateway | After AI Gateway | Improvement |
|--------|-------------------|------------------|-------------|
| Monthly API Cost | $50.00 | $38.00 | 24% reduction |
| Avg Response Time | 1.8s | 0.9s | 50% faster |
| Success Rate | 95% | 99.7% | +4.7% |
| Development Time | 20 hrs | 5 hrs | 75% reduction |
| Monitoring Setup | Custom scripts | Built-in dashboard | N/A |

**Total Savings:**
- Direct cost savings: $12/month
- Engineering time: 15 hours/month
- Improved reliability: Priceless

---

## 9. Integration Guide

### Quick Start for Minecraft MineWright AI

**Step 1: Create AI Gateway**

1. Log in to Cloudflare Dashboard
2. Navigate to **AI Gateway** → **Create**
3. Name: `minecraft-steve-ai`
4. Add providers (OpenAI, Workers AI, etc.)
5. Copy **Account ID** and **Gateway Name**

**Step 2: Update OpenAI Client**

```java
// In OpenAIClient.java
public class OpenAIClient {
    private static final String GATEWAY_BASE_URL =
        "https://gateway.ai.cloudflare.com/v1/{account_id}/minecraft-steve-ai/openai";

    private final OpenAI openAI;

    public OpenAIClient(String apiKey) {
        this.openAI = new OpenAI(
            BuildConfig.OPENAI_API_KEY,
            GATEWAY_BASE_URL  // Override base URL
        );
    }
}
```

**Step 3: Update Configuration**

```toml
# In config/minewright-common.toml
[llm]
provider = "openai"
useGateway = true

[cloudflare]
accountId = "abc123def456"
gatewayName = "minecraft-steve-ai"
```

**Step 4: Add Metadata Tracking**

```java
// In TaskPlanner.java
public CompletableFuture<TaskPlan> planTasksAsync(
    SteveEntity steve,
    String command
) {
    // Add metadata for tracking
    Map<String, Object> metadata = Map.of(
        "agent_id", steve.getUUID().toString(),
        "agent_name", steve.getName(),
        "task_type", classifyTask(command),
        "game_tick", steve.level().getGameTime(),
        "location", String.format("(%.0f, %.0f, %.0f)",
            steve.getX(), steve.getY(), steve.getZ())
    );

    return sendLLMRequest(prompt, metadata);
}
```

**Step 5: Verify in Dashboard**

1. Send a test command in-game
2. Check AI Gateway dashboard
3. Verify request appears in logs
4. Check metadata is populated
5. Monitor cost and latency

### Advanced Integration

**Multi-Provider Configuration:**

```java
// In LLMProvider.java
public class LLMProvider {
    private final String gatewayUrl;
    private final String fallbackUrl;

    public LLMProvider(Config config) {
        this.gatewayUrl = config.getGatewayUrl();
        this.fallbackUrl = config.getFallbackUrl();
    }

    public String complete(String prompt, TaskType type) {
        switch (type) {
            case COMBAT:
                return completeWithProvider(prompt, "openai/gpt-4");  // Fastest
            case PLANNING:
                return completeWithProvider(prompt, "anthropic/claude-3-5-sonnet");  // Smartest
            case MINING:
                return completeWithProvider(prompt, "workersai/llama-3-8b");  // Cheapest
            default:
                return completeWithProvider(prompt, "openai/gpt-3.5-turbo");  // Balanced
        }
    }
}
```

**Cache Strategy:**

```java
// In CacheStrategy.java
public class CacheStrategy {
    public boolean shouldCache(TaskType type, String prompt) {
        return switch (type) {
            case COMBAT -> false;  // Real-time, don't cache
            case MINING -> isNavigationPrompt(prompt);  // Cache routes
            case BUILDING -> true;  // Cache templates
            case TRADING -> false;  // Dynamic prices
        };
    }

    public int getCacheTTL(TaskType type) {
        return switch (type) {
            case MINING -> 3600;  // 1 hour
            case BUILDING -> 86400;  // 24 hours
            case TRADING -> 300;  // 5 minutes
            default -> 1800;  // 30 minutes
        };
    }
}
```

### Monitoring and Alerting

**Set Up Alerts:**

1. Navigate to AI Gateway dashboard
2. Click **Alerts** → **Create Alert**
3. Configure:
   - Daily cost limit: $5.00
   - P95 latency threshold: 2000ms
   - Error rate threshold: 5%
4. Add notification channels (email, Slack)

**Create Custom Dashboard:**

```javascript
// Via Cloudflare Dashboard API
const dashboard = await fetch("https://api.cloudflare.com/client/v4/accounts/{account_id}/dashboards", {
  method: "POST",
  headers: {
    "Authorization": `Bearer ${API_TOKEN}`,
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    name: "Minecraft MineWright AI",
    widgets: [
      {
        type: "timeseries",
        metrics: ["requests", "cost", "latency"],
        filter: { "metadata.agent_id": "steve-1" }
      },
      {
        type: "table",
        columns: ["task_type", "avg_cost", "avg_latency", "success_rate"],
        groupBy: "metadata.task_type"
      }
    ]
  })
});
```

---

## 10. References

### Official Documentation

- [Cloudflare AI Gateway Overview](https://developers.cloudflare.com/ai-gateway/)
- [AI Gateway Product Page](https://www.cloudflare.com/developer-platform/products/ai-gateway/)
- [Pricing Information](https://developers.cloudflare.com/ai-gateway/reference/pricing/)
- [Workers AI Integration](https://developers.cloudflare.com/ai-gateway/providers/workersai/)
- [Universal Endpoint](https://developers.cloudflare.com/ai-gateway/usage/universal/)
- [Worker Binding Methods](https://developers.cloudflare.com/ai-gateway/integrations/worker-binding-methods/)
- [JSON Configuration](https://developers.cloudflare.com/ai-gateway/features/dynamic-routing/json-configuration/)
- [Deploy OpenAI via AI Gateway](https://developers.cloudflare.com/ai-gateway/tutorials/deploy-aig-worker/)

### Tutorials and Guides

- [Moltworker Complete Guide 2026](https://dev.to/sienna/moltworker-complete-guide-2026-running-personal-ai-agents-on-cloudflare-without-hardware-4a99)
- [Cloudflare AI Gateway + Google Gemini](https://blog.csdn.net/2301_77187902/article/details/149547714)
- [AI Gateway Proxy for Third-Party LLM](https://m.blog.csdn.net/f2424004764/article/details/157519223)
- [AI Fallback Configuration](https://juejin.cn/post/7584712109171359770)

### Community Resources

- [AI Gateway (OpenAI) Wrapper](https://www.j301.cn/blog/github_ai_tool_gateway_wrapper.html)
- [Cloudflare Workers React Boilerplate](https://github.com/henkisdabro/cloudflare-workers-react-boilerplate)
- [R2 Bucket Manager Worker](https://github.com/neverinfamous/R2-Manager-Worker)

### Related Research

- [Workers AI Complete Guide](https://juejin.cn/post/7580180556983337010)
- [Cloudflare Observability Vision](https://blog.cloudflare.com/zh-cn/vision-for-observability/)
- [AI Gateway General Availability Announcement](https://new.qq.com/rain/a/20240606A04M6V00)

---

## Appendix

### Key Metrics Summary

| Metric | Description | Target |
|--------|-------------|--------|
| Cache Hit Rate | % of requests served from cache | > 50% |
| P95 Latency | 95th percentile response time | < 2s |
| Error Rate | % of failed requests | < 1% |
| Cost per Task | Average cost per decision | < $0.05 |
| Tokens per Task | Average token usage | < 1000 |

### Troubleshooting

**Common Issues:**

1. **Logs not appearing:**
   - Verify gateway ID is correct
   - Check AI Gateway is enabled
   - Ensure requests are being sent to gateway URL

2. **High latency:**
   - Check cache hit rate
   - Consider using faster models
   - Review rate limiting settings

3. **Unexpected costs:**
   - Review token usage by model
   - Check cache effectiveness
   - Set up cost alerts

4. **Authentication errors:**
   - Verify API keys are correct
   - Check Secrets Store configuration
   - Ensure gateway permissions are set

### Glossary

| Term | Definition |
|------|------------|
| Neurons | Cloudflare's pricing unit for Workers AI (1M neurons ≈ 1M tokens) |
| QPS | Queries Per Second - rate limiting metric |
| TTL | Time To Live - cache duration |
| P50/P95/P99 | Latency percentiles |
| BYO Key | Bring Your Own Key - using existing provider API keys |
| Logpush | Export logs to external storage |

---

**Document End**

For questions or updates to this research document, please refer to the official Cloudflare AI Gateway documentation or contact the Cloudflare support team.
