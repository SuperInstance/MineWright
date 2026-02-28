package com.minewright.hivemind;

import com.google.gson.*;
import com.minewright.config.MineWrightConfig;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for Cloudflare Worker communication.
 *
 * <p>Provides async, non-blocking communication with the Cloudflare edge layer
 * for tactical decisions, state sync, and mission management.</p>
 *
 * <p><b>Latency Targets:</b></p>
 * <ul>
 *   <li>Tactical decisions: &lt;20ms</li>
 *   <li>State sync: &lt;100ms</li>
 *   <li>Mission ops: &lt;200ms</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class CloudflareClient {
    private static final Logger LOGGER = TestLogger.getLogger(CloudflareClient.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final HttpClient httpClient;
    private final String workerUrl;
    private final boolean enabled;

    public CloudflareClient() {
        this.enabled = MineWrightConfig.HIVEMIND_ENABLED.get();
        this.workerUrl = MineWrightConfig.HIVEMIND_WORKER_URL.get();

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(MineWrightConfig.HIVEMIND_CONNECT_TIMEOUT.get()))
            .build();

        if (enabled) {
            LOGGER.info("CloudflareClient initialized with worker: {}", workerUrl);
        } else {
            LOGGER.info("CloudflareClient disabled (Hive Mind not enabled in config)");
        }
    }

    /**
     * Checks if the Hive Mind integration is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    // ==================== TACTICAL DECISIONS ====================

    /**
     * Gets a tactical decision from the edge (combat reflexes, hazard detection).
     *
     * <p>This is the fastest path - designed for sub-20ms responses.</p>
     *
     * @param agentId Agent UUID
     * @param request Tactical request with position, nearby entities, hazards
     * @return CompletableFuture with tactical decision
     */
    public CompletableFuture<TacticalDecision> getTacticalDecision(String agentId, TacticalRequest request) {
        if (!enabled) {
            return CompletableFuture.completedFuture(TacticalDecision.fallback("Hive Mind disabled"));
        }

        String url = workerUrl + "/agents/" + agentId + "/tactical";

        return sendAsync(url, request, MineWrightConfig.HIVEMIND_TACTICAL_TIMEOUT.get())
            .thenApply(this::parseTacticalDecision)
            .exceptionally(e -> {
                LOGGER.debug("Tactical decision failed, using fallback: {}", e.getMessage());
                return TacticalDecision.fallback(e.getMessage());
            });
    }

    // ==================== STATE SYNC ====================

    /**
     * Syncs agent state with Durable Objects at the edge.
     *
     * @param agentId Agent UUID
     * @param state Current agent state
     * @return CompletableFuture with sync result
     */
    public CompletableFuture<SyncResult> syncState(String agentId, AgentSyncState state) {
        if (!enabled) {
            return CompletableFuture.completedFuture(new SyncResult(false, "Disabled", null));
        }

        String url = workerUrl + "/agents/" + agentId + "/sync";

        return sendAsync(url, state, MineWrightConfig.HIVEMIND_SYNC_TIMEOUT.get())
            .thenApply(this::parseSyncResult)
            .exceptionally(e -> new SyncResult(false, e.getMessage(), null));
    }

    // ==================== MISSION MANAGEMENT ====================

    /**
     * Gets pending mission for an agent.
     *
     * @param agentId Agent UUID
     * @return CompletableFuture with mission data or null
     */
    public CompletableFuture<MissionData> getMission(String agentId) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }

        String url = workerUrl + "/agents/" + agentId + "/mission";

        return getAsync(url, MineWrightConfig.HIVEMIND_SYNC_TIMEOUT.get())
            .thenApply(this::parseMissionData)
            .exceptionally(e -> {
                LOGGER.debug("Failed to get mission: {}", e.getMessage());
                return null;
            });
    }

    /**
     * Reports mission completion to the edge.
     *
     * @param agentId Agent UUID
     * @param missionId Mission ID
     * @param success Whether mission succeeded
     * @param result Result message
     */
    public void reportMissionComplete(String agentId, String missionId, boolean success, String result) {
        if (!enabled) return;

        String url = workerUrl + "/agents/" + agentId + "/mission/" + missionId + "/complete";

        Map<String, Object> payload = Map.of(
            "success", success,
            "result", result,
            "timestamp", System.currentTimeMillis()
        );

        sendAsync(url, payload, MineWrightConfig.HIVEMIND_SYNC_TIMEOUT.get())
            .exceptionally(e -> {
                LOGGER.debug("Failed to report mission complete: {}", e.getMessage());
                return null;
            });
    }

    // ==================== HEALTH CHECK ====================

    /**
     * Checks worker health.
     *
     * @return CompletableFuture with true if healthy
     */
    public CompletableFuture<Boolean> checkHealth() {
        if (!enabled) {
            return CompletableFuture.completedFuture(false);
        }

        return getAsync(workerUrl + "/health", 2000)
            .thenApply(response -> response != null && response.contains("ok"))
            .exceptionally(e -> false);
    }

    // ==================== HTTP HELPERS ====================

    private CompletableFuture<String> sendAsync(String url, Object payload, int timeoutMs) {
        String body;
        try {
            body = GSON.toJson(payload);
        } catch (Exception e) {
            // Java 17 compatible alternative to CompletableFuture.failedFuture()
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofMillis(timeoutMs))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body);
    }

    private CompletableFuture<String> getAsync(String url, int timeoutMs) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofMillis(timeoutMs))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body);
    }

    // ==================== PARSERS ====================

    private TacticalDecision parseTacticalDecision(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonObject decision = obj.has("decision") ? obj.getAsJsonObject("decision") : obj;

            // Null-safe field access
            String action = decision.has("action") ? decision.get("action").getAsString() : "hold";
            float priority = decision.has("priority") ? decision.get("priority").getAsFloat() : 0.5f;
            String reasoning = decision.has("reasoning") ? decision.get("reasoning").getAsString() : "";
            int latency = decision.has("latency_ms") ? decision.get("latency_ms").getAsInt() : 0;

            return new TacticalDecision(action, priority, reasoning, latency);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse tactical decision: {}", e.getMessage());
            return TacticalDecision.fallback("Parse error: " + e.getMessage());
        }
    }

    private SyncResult parseSyncResult(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            // Null-safe field access
            boolean synced = obj.has("synced") && obj.get("synced").getAsBoolean();
            String message = obj.has("message") ? obj.get("message").getAsString() : "OK";
            JsonObject mission = obj.has("mission") ? obj.getAsJsonObject("mission") : null;

            return new SyncResult(synced, message, mission);
        } catch (Exception e) {
            return new SyncResult(false, "Parse error: " + e.getMessage(), null);
        }
    }

    private MissionData parseMissionData(String json) {
        if (json == null || json.isEmpty()) return null;

        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("mission")) return null;

            JsonObject mission = obj.getAsJsonObject("mission");

            // Null-safe field access
            if (!mission.has("id") || !mission.has("type")) return null;

            String id = mission.get("id").getAsString();
            String type = mission.get("type").getAsString();
            String description = mission.has("description") ? mission.get("description").getAsString() : "";
            JsonObject payload = mission.has("payload") ? mission.getAsJsonObject("payload") : null;

            return new MissionData(id, type, description, payload);
        } catch (Exception e) {
            LOGGER.debug("Failed to parse mission data: {}", e.getMessage());
            return null;
        }
    }

    // ==================== DATA CLASSES ====================

    /**
     * Request for tactical decision.
     */
    public static class TacticalRequest {
        public final String action;
        public final int[] position;
        public final float health;
        public final JsonArray nearbyEntities;
        public final JsonArray nearbyBlocks;
        public final float combatScore;

        public TacticalRequest(String action, int x, int y, int z, float health,
                              JsonArray nearbyEntities, JsonArray nearbyBlocks, float combatScore) {
            this.action = action;
            this.position = new int[]{x, y, z};
            this.health = health;
            this.nearbyEntities = nearbyEntities != null ? nearbyEntities : new JsonArray();
            this.nearbyBlocks = nearbyBlocks != null ? nearbyBlocks : new JsonArray();
            this.combatScore = combatScore;
        }

        public static TacticalRequest emergencyCheck(int x, int y, int z, float health,
                                                     JsonArray entities, JsonArray blocks) {
            return new TacticalRequest("check_emergency", x, y, z, health, entities, blocks, 0.5f);
        }

        public static TacticalRequest combatReflex(int x, int y, int z, float health,
                                                   JsonArray mobs, float combatScore) {
            return new TacticalRequest("combat_reflex", x, y, z, health, mobs, new JsonArray(), combatScore);
        }
    }

    /**
     * Tactical decision response.
     */
    public static class TacticalDecision {
        public final String action;
        public final float priority;
        public final String reasoning;
        public final int latencyMs;
        public final boolean isFallback;

        public TacticalDecision(String action, float priority, String reasoning, int latencyMs) {
            this.action = action;
            this.priority = priority;
            this.reasoning = reasoning;
            this.latencyMs = latencyMs;
            this.isFallback = false;
        }

        private TacticalDecision(String action, float priority, String reasoning, int latencyMs, boolean isFallback) {
            this.action = action;
            this.priority = priority;
            this.reasoning = reasoning;
            this.latencyMs = latencyMs;
            this.isFallback = isFallback;
        }

        public static TacticalDecision fallback(String reason) {
            return new TacticalDecision("hold", 0f, "Fallback: " + reason, 0, true);
        }

        public boolean requiresAction() {
            return !"hold".equals(action) && !"proceed".equals(action);
        }

        @Override
        public String toString() {
            return String.format("TacticalDecision[%s, priority=%.2f, %s]", action, priority, reasoning);
        }
    }

    /**
     * Agent state for synchronization.
     */
    public static class AgentSyncState {
        public final int[] position;
        public final String status;
        public final float health;
        public final int hunger;
        public final String currentTask;

        public AgentSyncState(int x, int y, int z, String status, float health, int hunger, String currentTask) {
            this.position = new int[]{x, y, z};
            this.status = status;
            this.health = health;
            this.hunger = hunger;
            this.currentTask = currentTask;
        }
    }

    /**
     * Result of state sync.
     */
    public static class SyncResult {
        public final boolean synced;
        public final String message;
        public final JsonObject mission;

        public SyncResult(boolean synced, String message, JsonObject mission) {
            this.synced = synced;
            this.message = message;
            this.mission = mission;
        }
    }

    /**
     * Mission data from the edge.
     */
    public static class MissionData {
        public final String id;
        public final String type;
        public final String description;
        public final JsonObject payload;

        public MissionData(String id, String type, String description, JsonObject payload) {
            this.id = id;
            this.type = type;
            this.description = description;
            this.payload = payload;
        }
    }
}
