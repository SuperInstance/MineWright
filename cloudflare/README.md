# Minecraft Agent Reflex - Cloudflare Worker Deployment

This directory contains the Cloudflare Worker deployment for the Minecraft MineWright Reflex Agent. This worker provides fast tactical decisions and state management for autonomous Minecraft agents using Durable Objects.

## Overview

The Reflex Agent is a cloud-based service that:

- **Provides fast tactical decisions** (<50ms response time)
- **Manages per-agent state** using Cloudflare Durable Objects
- **Syncs with Foreman orchestrator** for mission coordination
- **Assesses combat threats** and environmental hazards
- **Prioritizes resources** based on mission needs

## Architecture

```
Minecraft Client (MineWright)
       |
       v HTTP
Cloudflare Worker (Reflex Agent)
       |
       +-- Durable Object (AgentState) - Per-agent state
       +-- AI Binding - Strategic reasoning
       +-- Vectorize - Knowledge search
       +-- KV Cache - Mission caching
       |
       v HTTP
Foreman Orchestrator (Mission coordination)
```

## Prerequisites

1. **Cloudflare Account** with Workers enabled
2. **Node.js 18+** and **npm** (for Wrangler CLI)
3. **Python 3.11+** (for Python workers)
4. **Wrangler CLI** - Install globally:
   ```bash
   npm install -g wrangler
   ```

## Setup

### 1. Install Dependencies

```bash
cd cloudflare
npm install
```

### 2. Configure Wrangler

Login to Cloudflare:

```bash
wrangler login
```

Set your account ID in `wrangler.toml` or via environment variable:

```bash
export CLOUDFLARE_ACCOUNT_ID="your-account-id"
```

### 3. Create Required Resources

#### KV Namespaces

Create two KV namespaces for caching:

```bash
# Production
wrangler kv:namespace create "MISSION_CACHE"
wrangler kv:namespace create "KNOWLEDGE_CACHE"

# Preview (for development)
wrangler kv:namespace create "MISSION_CACHE" --preview
wrangler kv:namespace create "KNOWLEDGE_CACHE" --preview
```

Update the KV namespace IDs in `wrangler.toml` with the returned IDs.

#### Vectorize Index

Create a Vectorize index for semantic search:

```bash
wrangler vectorize create minecraft-knowledge \
  --dimensions=1536 \
  --metric=cosine \
  --preview
```

Update the index name in `wrangler.toml` if different.

### 4. Set Secrets

Set required secrets for production:

```bash
# Foreman connection
wrangler secret put FOREMAN_URL
wrangler secret put FOREMAN_API_KEY

# Optional: OpenAI API key for enhanced reasoning
wrangler secret put OPENAI_API_KEY
```

## Development

### Local Development

Start the local development server:

```bash
wrangler dev
```

This runs the worker locally at `http://localhost:8787`.

### Testing Endpoints

Using curl:

```bash
# Health check
curl http://localhost:8787/agents/test-agent/health

# Sync state (GET)
curl http://localhost:8787/agents/test-agent/sync

# Sync state (POST)
curl -X POST http://localhost:8787/agents/test-agent/sync \
  -H "Content-Type: application/json" \
  -d '{
    "position": {"x": 100, "y": 64, "z": 200},
    "status": "executing",
    "health": 18,
    "hunger": 15
  }'

# Get tactical decision
curl -X POST http://localhost:8787/agents/test-agent/tactical \
  -H "Content-Type: application/json" \
  -d '{
    "position": {"x": 100, "y": 64, "z": 200},
    "nearbyEntities": [
      {
        "type": "zombie",
        "x": 105,
        "y": 64,
        "z": 200,
        "health": 20
      }
    ],
    "nearbyBlocks": [
      {"type": "lava", "x": 110, "y": 63, "z": 200, "solid": false}
    ],
    "health": 18,
    "combatScore": 0.6
  }'

# Get missions
curl http://localhost:8787/agents/test-agent/mission

# Add mission
curl -X POST http://localhost:8787/agents/test-agent/mission \
  -H "Content-Type: application/json" \
  -d '{
    "mission": {
      "id": "mission-123",
      "type": "build",
      "target": {"x": 150, "y": 64, "z": 250},
      "priority": 0.8
    }
  }'

# Complete mission
curl -X DELETE http://localhost:8787/agents/test-agent/mission

# Log telemetry
curl -X POST http://localhost:8787/agents/test-agent/telemetry \
  -H "Content-Type: application/json" \
  -d '{
    "type": "combat",
    "data": {
      "action": "attack",
      "target": "zombie",
      "result": "success"
    }
  }'
```

### Running Tests

Run the test suite:

```bash
pytest
```

Run with coverage:

```bash
pytest --cov=src --cov-report=html
```

## Deployment

### Deploy to Production

Deploy the worker to production:

```bash
wrangler deploy
```

This deploys to the production environment with the name `minecraft-agent-reflex-prod`.

### Deploy to Staging

Deploy to staging first for testing:

```bash
wrangler deploy --env staging
```

### Deployment Environments

- **Development**: `wrangler dev` - Local development
- **Staging**: `wrangler deploy --env staging` - Pre-production testing
- **Production**: `wrangler deploy` - Live deployment

## API Reference

### Endpoints

All endpoints are prefixed with `/agents/{agent_id}/`

#### GET /health

Health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "agentId": "steve-1",
  "timestamp": "2024-01-10T12:00:00Z"
}
```

#### GET /sync

Get current agent state.

**Response:**
```json
{
  "agentId": "steve-1",
  "status": "executing",
  "position": {"x": 100, "y": 64, "z": 200},
  "health": 18,
  "hunger": 15,
  "currentTask": "build-shelter",
  "missionQueue": [...],
  "activeThreats": [...],
  "knownHazards": [...]
}
```

#### POST /sync

Update agent state from Foreman.

**Request:**
```json
{
  "position": {"x": 100, "y": 64, "z": 200},
  "status": "executing",
  "health": 18,
  "hunger": 15,
  "currentTask": "build-shelter",
  "mission": {
    "id": "mission-123",
    "type": "build",
    "priority": 0.8
  }
}
```

**Response:**
```json
{
  "status": "synced",
  "agentId": "steve-1",
  "timestamp": "2024-01-10T12:00:00Z"
}
```

#### POST /tactical

Get quick tactical decision.

**Request:**
```json
{
  "position": {"x": 100, "y": 64, "z": 200},
  "nearbyEntities": [...],
  "nearbyBlocks": [...],
  "health": 18,
  "combatScore": 0.6
}
```

**Response:**
```json
{
  "decision": {
    "action": "attack",
    "priority": 0.7,
    "reasoning": "ENGAGE: zombie at 5.0 blocks",
    "targetPosition": {"x": 103, "y": 64, "z": 200},
    "confidence": 0.8
  },
  "threats": [...],
  "hazards": [...],
  "timestamp": "2024-01-10T12:00:00Z"
}
```

#### GET /mission

Get mission queue.

**Response:**
```json
{
  "agentId": "steve-1",
  "missions": [...],
  "current": "build-shelter"
}
```

#### POST /mission

Add new mission to queue.

**Request:**
```json
{
  "mission": {
    "id": "mission-123",
    "type": "build",
    "target": {"x": 150, "y": 64, "z": 250},
    "priority": 0.8
  }
}
```

**Response:**
```json
{
  "status": "queued",
  "queueLength": 3
}
```

#### DELETE /mission

Complete current mission.

**Response:**
```json
{
  "completed": {
    "id": "mission-123",
    "type": "build"
  }
}
```

#### POST /telemetry

Log telemetry event.

**Request:**
```json
{
  "type": "combat",
  "data": {
    "action": "attack",
    "target": "zombie",
    "result": "success"
  }
}
```

**Response:**
```json
{
  "status": "logged"
}
```

## Monitoring

### View Logs

View real-time logs:

```bash
wrangler tail
```

Filter by environment:

```bash
wrangler tail --env staging
```

### Metrics

Cloudflare Workers automatically provides metrics for:
- Request count
- Error rate
- Response time
- CPU usage

View these in the Cloudflare Dashboard under **Workers & Pages**.

### Analytics

Enable Workers Analytics for detailed insights:

```bash
wrangler analytics
```

## Troubleshooting

### Common Issues

**Worker fails to deploy:**
- Check account ID is set correctly
- Verify all secrets are configured
- Ensure KV namespaces exist

**Durable Object errors:**
- Verify migrations have been applied
- Check class name matches in `wrangler.toml`

**High latency:**
- Check CPU limits in `wrangler.toml`
- Consider enabling Workers for Platforms
- Review geographic location

### Debug Mode

Enable debug logging by setting environment variable:

```bash
wrangler dev --log-level debug
```

## Cost Estimation

Cloudflare Workers pricing (as of 2024):

- **Free Tier**: 100,000 requests/day
- **Paid**: $5/million requests after free tier
- **Durable Objects**: $0.15 per million reads, $0.20 per million writes
- **KV Storage**: $0.50 per million reads, $5.00 per GB-month

For typical usage (100 agents, 1 request/second each):
- ~8.6 million requests/day
- Estimated cost: ~$40-60/month

## Performance

Target SLAs:
- **Tactical decisions**: <50ms p95
- **State sync**: <100ms p95
- **Mission updates**: <200ms p95

Optimization tips:
- Enable Workers KV cache for missions
- Use Durable Object storage for state
- Batch telemetry events
- Geographic distribution for global agents

## Security

- All requests use HTTPS
- Secrets stored via `wrangler secret`
- API keys never in code
- Rate limiting via Cloudflare Rules

## Contributing

When contributing:

1. Follow Python code style (Black formatter)
2. Add tests for new features
3. Update API documentation
4. Test locally with `wrangler dev`

## License

MIT License - See LICENSE file for details.
