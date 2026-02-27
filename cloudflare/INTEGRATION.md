# Cloudflare Worker Integration Guide

This guide explains how to integrate the Cloudflare Worker Reflex Agent with the Minecraft MineWright mod.

## Overview

The Cloudflare Worker provides a backend service for:
1. **Fast tactical decisions** - Combat threat assessment and hazard detection
2. **State synchronization** - Persistent agent state via Durable Objects
3. **Mission queue** - Receive missions from Foreman orchestrator
4. **Telemetry** - Log events for analysis

## Architecture

```
Minecraft Client (Steve Mod)
       |
       v HTTP
Cloudflare Worker (Reflex Agent)
       |
       v HTTP
Foreman Orchestrator (on your server)
```

## Integration Steps

### 1. Add HTTP Client to Minecraft Mod

Add an HTTP client to your mod for communicating with the Cloudflare Worker.

In `src/main/java/com/minewright/client/CloudflareReflexClient.java`:

```java
package com.minewright.client;

import com.google.gson.*;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class CloudflareReflexClient {
    private static final String WORKER_URL = "https://minecraft-agent-reflex.workers.dev";
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private final String agentId;

    public CloudflareReflexClient(String agentId) {
        this.agentId = agentId;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Get tactical decision from Cloudflare Worker
     */
    public TacticalDecision getTacticalDecision(SteveEntity steve, List<Entity> nearbyEntities) throws Exception {
        // Build request payload
        JsonObject payload = new JsonObject();
        payload.add("position", buildPosition(steve.blockPosition()));
        payload.add("nearbyEntities", buildEntities(nearbyEntities));
        payload.addProperty("health", steve.getHealth());
        // Add combat score calculation
        payload.addProperty("combatScore", calculateCombatScore(steve));

        // Send request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(WORKER_URL + "/agents/" + agentId + "/tactical"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
            .timeout(Duration.ofMillis(500))  // 500ms timeout for fast decisions
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Worker returned " + response.statusCode());
        }

        // Parse response
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonObject decisionJson = jsonResponse.getAsJsonObject("decision");

        return new TacticalDecision(
            decisionJson.get("action").getAsString(),
            decisionJson.get("priority").getAsFloat(),
            decisionJson.get("reasoning").getAsString()
        );
    }

    /**
     * Sync agent state with Cloudflare Worker
     */
    public void syncState(SteveEntity steve) throws Exception {
        JsonObject payload = new JsonObject();
        payload.add("position", buildPosition(steve.blockPosition()));
        payload.addProperty("status", steve.getStatus().name().toLowerCase());
        payload.addProperty("health", steve.getHealth());
        payload.addProperty("hunger", steve.getFoodData().getFoodLevel());
        payload.addProperty("currentTask", steve.getCurrentTask());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(WORKER_URL + "/agents/" + agentId + "/sync"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
            .timeout(Duration.ofMillis(1000))
            .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Log telemetry event
     */
    public void logTelemetry(String eventType, Map<String, Object> data) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", eventType);
        payload.add("data", GSON.toJsonTree(data));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(WORKER_URL + "/agents/" + agentId + "/telemetry"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
            .timeout(Duration.ofMillis(500))
            .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject buildPosition(BlockPos pos) {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", pos.getX());
        obj.addProperty("y", pos.getY());
        obj.addProperty("z", pos.getZ());
        return obj;
    }

    private JsonArray buildEntities(List<Entity> entities) {
        JsonArray array = new JsonArray();
        for (Entity entity : entities) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", entity.getType().toString().toLowerCase());
            obj.addProperty("x", entity.getX());
            obj.addProperty("y", entity.getY());
            obj.addProperty("z", entity.getZ());
            obj.addProperty("health", (int) entity.getHealth());
            array.add(obj);
        }
        return array;
    }

    private float calculateCombatScore(SteveEntity steve) {
        // Simple combat score based on health and equipment
        float score = 0.5f; // Base score

        // Health bonus
        score += (steve.getHealth() / 20.0f) * 0.3f;

        // TODO: Add equipment bonus

        return Math.min(1.0f, score);
    }

    public static class TacticalDecision {
        public final String action;
        public final float priority;
        public final String reasoning;

        public TacticalDecision(String action, float priority, String reasoning) {
            this.action = action;
            this.priority = priority;
            this.reasoning = reasoning;
        }
    }
}
```

### 2. Update Steve Entity

Modify `SteveEntity` to use the Cloudflare Reflex Client:

```java
public class SteveEntity extends PathfinderMob {
    private CloudflareReflexClient reflexClient;
    private long lastTacticalCheck = 0;
    private static final long TACTICAL_CHECK_INTERVAL = 20; // Check every second (20 ticks)

    @Override
    public void tick() {
        super.tick();

        // Periodic tactical check
        if (this.level.getGameTime() - lastTacticalCheck >= TACTICAL_CHECK_INTERVAL) {
            lastTacticalCheck = this.level.getGameTime();
            checkTacticalSituation();
        }

        // Sync state every 5 seconds (100 ticks)
        if (this.level.getGameTime() % 100 == 0) {
            syncState();
        }
    }

    private void checkTacticalSituation() {
        if (reflexClient == null) {
            reflexClient = new CloudflareReflexClient(this.getUUID().toString());
        }

        // Get nearby hostile entities
        List<Entity> nearbyEntities = this.level.getEntitiesOfClass(
            Entity.class,
            this.getBoundingBox().inflate(16.0) // 16 block radius
        );

        try {
            CloudflareReflexClient.TacticalDecision decision = reflexClient.getTacticalDecision(this, nearbyEntities);

            // Execute decision
            switch (decision.action) {
                case "flee" -> executeFlee(decision);
                case "attack" -> executeAttack(decision);
                case "shield" -> executeShield(decision);
                case "dodge" -> executeDodge(decision);
                case "hold" -> {} // Continue current action
            }

            // Log telemetry
            reflexClient.logTelemetry("tactical", Map.of(
                "action", decision.action,
                "priority", decision.priority,
                "reasoning", decision.reasoning
            ));

        } catch (Exception e) {
            // Log error but continue - local AI should handle fallback
            LOGGER.error("Failed to get tactical decision from worker", e);
        }
    }

    private void executeFlee(TacticalDecision decision) {
        // Set movement away from threat
        // This integrates with your existing action system
    }

    private void executeAttack(TacticalDecision decision) {
        // Engage combat with target
    }

    private void executeShield(TacticalDecision decision) {
        // Activate shield/block
    }

    private void executeDodge(TacticalDecision decision) {
        // Quick movement to avoid hazard
    }

    private void syncState() {
        if (reflexClient == null) {
            reflexClient = new CloudflareReflexClient(this.getUUID().toString());
        }

        try {
            reflexClient.syncState(this);
        } catch (Exception e) {
            LOGGER.error("Failed to sync state with worker", e);
        }
    }
}
```

### 3. Configuration

Add to `config/minewright-common.toml`:

```toml
[cloudflare]
# Enable Cloudflare Worker integration
enabled = true

# Worker URL (update with your deployed URL)
worker_url = "https://minecraft-agent-reflex.workers.dev"

# Request timeouts (milliseconds)
tactical_timeout = 500
sync_timeout = 1000
telemetry_timeout = 500

# Check intervals (ticks)
tactical_check_interval = 20  # Every second
sync_interval = 100  # Every 5 seconds

# Enable telemetry
telemetry_enabled = true
```

## Benefits

1. **Reduced Local Load** - Offload threat assessment to cloud
2. **Global Intelligence** - Share threat patterns across agents
3. **Persistent State** - Agent state survives server restarts
4. **Centralized Control** - Foreman can coordinate all agents
5. **Analytics** - Telemetry for improving AI behavior

## Fallback Strategy

The mod should work even if the Cloudflare Worker is unavailable:

```java
private void checkTacticalSituation() {
    try {
        // Try cloud reflex first
        CloudflareReflexClient.TacticalDecision decision =
            reflexClient.getTacticalDecision(this, nearbyEntities);
        executeDecision(decision);
    } catch (Exception e) {
        // Fallback to local AI
        LOGGER.debug("Worker unavailable, using local AI");
        LocalTacticalAI.assessThreats(this, nearbyEntities);
    }
}
```

## Security

1. **Use HTTPS** - All communication encrypted
2. **Add API Keys** - Require authentication in production
3. **Rate Limiting** - Prevent abuse
4. **Validate Responses** - Check for malformed data

## Monitoring

View real-time logs from the worker:

```bash
wrangler tail
```

Check agent activity in the Cloudflare Dashboard:
- Request rate and latency
- Error rates
- CPU usage per Durable Object

## Cost Considerations

- **Free Tier**: 100,000 requests/day
- **Typical Usage**: 1 tactical check/second = 86,400 requests/day
- **Cost after free tier**: $5/million requests

For 100 agents @ 1 check/second: ~$40-60/month

## Troubleshooting

### High Latency

If tactical decisions take too long:
1. Check worker geographic location
2. Enable Workers for Platforms
3. Reduce request payload size
4. Increase timeout in mod config

### Connection Failures

If mod can't reach worker:
1. Check WORKER_URL is correct
2. Verify firewall allows outbound HTTPS
3. Check worker logs: `wrangler tail`
4. Test endpoint manually with curl

### State Not Persisting

If agent state is lost:
1. Verify Durable Objects are enabled
2. Check migrations have run
3. Verify agent_id is consistent
4. Check storage limits

## Next Steps

1. **Deploy Worker**: Follow `cloudflare/README.md`
2. **Add to Mod**: Copy the integration code above
3. **Configure**: Update config with your WORKER_URL
4. **Test**: Spawn a Steve and watch the logs
5. **Monitor**: Use Cloudflare Dashboard and `wrangler tail`
