package com.minewright.hivemind;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CloudflareClient data classes and basic functionality.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Data class construction and validation</li>
 *   <li>TacticalDecision properties and methods</li>
 *   <li>TacticalRequest factory methods</li>
 *   <li>AgentSyncState construction</li>
 *   <li>SyncResult and MissionData</li>
 *   <li>Edge cases and boundary conditions</li>
 * </ul>
 *
 * @since 1.2.0
 */
@DisplayName("CloudflareClient Data Classes Tests")
class CloudflareClientDataClassTest {

    private static final String TEST_AGENT_ID = "test-agent-uuid-12345";

    @BeforeEach
    void setUp() {
        // Setup before each test if needed
    }

    // ==================== TACTICAL DECISION TESTS ====================

    @Test
    @DisplayName("TacticalDecision - Basic construction")
    void testTacticalDecision_Construction() {
        CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
            "attack", 0.85f, "Hostile detected", 12
        );

        assertEquals("attack", decision.action);
        assertEquals(0.85f, decision.priority);
        assertEquals("Hostile detected", decision.reasoning);
        assertEquals(12, decision.latencyMs);
        assertFalse(decision.isFallback);
    }

    @Test
    @DisplayName("TacticalDecision - Fallback creation")
    void testTacticalDecision_Fallback() {
        CloudflareClient.TacticalDecision fallback = CloudflareClient.TacticalDecision.fallback("Test error");

        assertTrue(fallback.isFallback);
        assertEquals("hold", fallback.action);
        assertEquals(0f, fallback.priority);
        assertTrue(fallback.reasoning.contains("Fallback: Test error"));
        assertEquals(0, fallback.latencyMs);
    }

    @Test
    @DisplayName("TacticalDecision - Requires action for offensive moves")
    void testTacticalDecision_RequiresAction_Offensive() {
        CloudflareClient.TacticalDecision attack = new CloudflareClient.TacticalDecision(
            "attack", 0.8f, "Enemy nearby", 10
        );
        assertTrue(attack.requiresAction());

        CloudflareClient.TacticalDecision retreat = new CloudflareClient.TacticalDecision(
            "retreat", 0.9f, "Danger", 10
        );
        assertTrue(retreat.requiresAction());

        CloudflareClient.TacticalDecision dodge = new CloudflareClient.TacticalDecision(
            "dodge", 0.95f, "Arrow incoming", 5
        );
        assertTrue(dodge.requiresAction());
    }

    @Test
    @DisplayName("TacticalDecision - Does not require action for passive moves")
    void testTacticalDecision_RequiresAction_Passive() {
        CloudflareClient.TacticalDecision hold = new CloudflareClient.TacticalDecision(
            "hold", 0.5f, "Waiting", 10
        );
        assertFalse(hold.requiresAction());

        CloudflareClient.TacticalDecision proceed = new CloudflareClient.TacticalDecision(
            "proceed", 0.5f, "Clear", 10
        );
        assertFalse(proceed.requiresAction());
    }

    @Test
    @DisplayName("TacticalDecision - ToString format")
    void testTacticalDecision_ToString() {
        CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
            "attack", 0.85f, "Hostile detected", 12
        );

        String str = decision.toString();
        assertTrue(str.contains("attack"));
        assertTrue(str.contains("0.85"));
        assertTrue(str.contains("Hostile detected"));
    }

    @Test
    @DisplayName("TacticalDecision - Priority boundary values")
    void testTacticalDecision_PriorityBoundaries() {
        CloudflareClient.TacticalDecision minPriority = new CloudflareClient.TacticalDecision(
            "hold", 0.0f, "Minimum priority", 10
        );
        assertEquals(0.0f, minPriority.priority);

        CloudflareClient.TacticalDecision maxPriority = new CloudflareClient.TacticalDecision(
            "attack", 1.0f, "Maximum priority", 10
        );
        assertEquals(1.0f, maxPriority.priority);
    }

    @Test
    @DisplayName("TacticalDecision - Latency boundary values")
    void testTacticalDecision_LatencyBoundaries() {
        CloudflareClient.TacticalDecision zeroLatency = new CloudflareClient.TacticalDecision(
            "proceed", 0.5f, "Instant", 0
        );
        assertEquals(0, zeroLatency.latencyMs);

        CloudflareClient.TacticalDecision highLatency = new CloudflareClient.TacticalDecision(
            "hold", 0.5f, "Slow", 1000
        );
        assertEquals(1000, highLatency.latencyMs);
    }

    // ==================== TACTICAL REQUEST TESTS ====================

    @Test
    @DisplayName("TacticalRequest - Basic construction")
    void testTacticalRequest_Construction() {
        JsonArray entities = new JsonArray();
        JsonArray blocks = new JsonArray();

        CloudflareClient.TacticalRequest request = new CloudflareClient.TacticalRequest(
            "test_action", 10, 20, 30, 15.5f, entities, blocks, 0.7f
        );

        assertEquals("test_action", request.action);
        assertArrayEquals(new int[]{10, 20, 30}, request.position);
        assertEquals(15.5f, request.health);
        assertEquals(0.7f, request.combatScore);
        assertNotNull(request.nearbyEntities);
        assertNotNull(request.nearbyBlocks);
    }

    @Test
    @DisplayName("TacticalRequest - Null-safe arrays")
    void testTacticalRequest_NullSafeArrays() {
        CloudflareClient.TacticalRequest request = new CloudflareClient.TacticalRequest(
            "test_action", 0, 0, 0, 20.0f, null, null, 0.5f
        );

        assertNotNull(request.nearbyEntities);
        assertNotNull(request.nearbyBlocks);
        assertEquals(0, request.nearbyEntities.size());
        assertEquals(0, request.nearbyBlocks.size());
    }

    @Test
    @DisplayName("TacticalRequest - Emergency check factory")
    void testTacticalRequest_EmergencyCheck() {
        JsonArray entities = new JsonArray();
        JsonArray blocks = new JsonArray();

        CloudflareClient.TacticalRequest request = CloudflareClient.TacticalRequest.emergencyCheck(
            5, 10, 15, 18.0f, entities, blocks
        );

        assertEquals("check_emergency", request.action);
        assertArrayEquals(new int[]{5, 10, 15}, request.position);
        assertEquals(18.0f, request.health);
        assertEquals(0.5f, request.combatScore);
    }

    @Test
    @DisplayName("TacticalRequest - Combat reflex factory")
    void testTacticalRequest_CombatReflex() {
        JsonArray mobs = new JsonArray();
        JsonObject mob = new JsonObject();
        mob.addProperty("type", "zombie");
        mobs.add(mob);

        CloudflareClient.TacticalRequest request = CloudflareClient.TacticalRequest.combatReflex(
            100, 64, 200, 10.0f, mobs, 0.3f
        );

        assertEquals("combat_reflex", request.action);
        assertArrayEquals(new int[]{100, 64, 200}, request.position);
        assertEquals(10.0f, request.health);
        assertEquals(0.3f, request.combatScore);
        assertEquals(1, request.nearbyEntities.size());
        assertEquals(0, request.nearbyBlocks.size());
    }

    @Test
    @DisplayName("TacticalRequest - Extreme positions")
    void testTacticalRequest_ExtremePositions() {
        JsonArray entities = new JsonArray();
        JsonArray blocks = new JsonArray();

        CloudflareClient.TacticalRequest request = new CloudflareClient.TacticalRequest(
            "test", Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 20.0f, entities, blocks, 0.5f
        );

        assertEquals(Integer.MAX_VALUE, request.position[0]);
        assertEquals(Integer.MIN_VALUE, request.position[1]);
    }

    @Test
    @DisplayName("TacticalRequest - Extreme health values")
    void testTacticalRequest_ExtremeHealth() {
        JsonArray entities = new JsonArray();
        JsonArray blocks = new JsonArray();

        CloudflareClient.TacticalRequest request = new CloudflareClient.TacticalRequest(
            "test", 0, 0, 0, Float.MAX_VALUE, entities, blocks, 0.5f
        );

        assertEquals(Float.MAX_VALUE, request.health);
    }

    @Test
    @DisplayName("TacticalRequest - Combat score boundaries")
    void testTacticalRequest_CombatScoreBoundaries() {
        JsonArray entities = new JsonArray();
        JsonArray blocks = new JsonArray();

        CloudflareClient.TacticalRequest minScore = new CloudflareClient.TacticalRequest(
            "test", 0, 0, 0, 20.0f, entities, blocks, 0.0f
        );
        assertEquals(0.0f, minScore.combatScore);

        CloudflareClient.TacticalRequest maxScore = new CloudflareClient.TacticalRequest(
            "test", 0, 0, 0, 20.0f, entities, blocks, 1.0f
        );
        assertEquals(1.0f, maxScore.combatScore);
    }

    // ==================== AGENT SYNC STATE TESTS ====================

    @Test
    @DisplayName("AgentSyncState - Basic construction")
    void testAgentSyncState_Construction() {
        CloudflareClient.AgentSyncState state = new CloudflareClient.AgentSyncState(
            100, 64, -50, "idle", 15.5f, 12, "following"
        );

        assertArrayEquals(new int[]{100, 64, -50}, state.position);
        assertEquals("idle", state.status);
        assertEquals(15.5f, state.health);
        assertEquals(12, state.hunger);
        assertEquals("following", state.currentTask);
    }

    @Test
    @DisplayName("AgentSyncState - Working status")
    void testAgentSyncState_WorkingStatus() {
        CloudflareClient.AgentSyncState state = new CloudflareClient.AgentSyncState(
            0, 64, 0, "working", 20.0f, 18, "mining"
        );

        assertEquals("working", state.status);
        assertEquals("mining", state.currentTask);
    }

    @Test
    @DisplayName("AgentSyncState - Extreme positions")
    void testAgentSyncState_ExtremePositions() {
        CloudflareClient.AgentSyncState state = new CloudflareClient.AgentSyncState(
            Integer.MAX_VALUE, 64, Integer.MIN_VALUE, "idle", 20.0f, 20, "none"
        );

        assertEquals(Integer.MAX_VALUE, state.position[0]);
        assertEquals(Integer.MIN_VALUE, state.position[2]);
    }

    @Test
    @DisplayName("AgentSyncState - Long task name")
    void testAgentSyncState_LongTaskName() {
        String longTask = "a".repeat(10000);
        CloudflareClient.AgentSyncState state = new CloudflareClient.AgentSyncState(
            0, 64, 0, "working", 20.0f, 18, longTask
        );

        assertEquals(longTask, state.currentTask);
    }

    @Test
    @DisplayName("AgentSyncState - Special characters in task")
    void testAgentSyncState_SpecialCharacters() {
        String specialTask = "Task with special chars: \n\t\r\"'\\<>{}[]";
        CloudflareClient.AgentSyncState state = new CloudflareClient.AgentSyncState(
            0, 64, 0, "working", 20.0f, 18, specialTask
        );

        assertEquals(specialTask, state.currentTask);
    }

    @Test
    @DisplayName("AgentSyncState - Unicode in task")
    void testAgentSyncState_Unicode() {
        String unicodeTask = "Task with unicode: 你好 🎮 𝕌𝕟𝕚𝕔𝕠𝕕𝕖";
        CloudflareClient.AgentSyncState state = new CloudflareClient.AgentSyncState(
            0, 64, 0, "working", 20.0f, 18, unicodeTask
        );

        assertEquals(unicodeTask, state.currentTask);
    }

    // ==================== SYNC RESULT TESTS ====================

    @Test
    @DisplayName("SyncResult - Successful sync with mission")
    void testSyncResult_SuccessWithMission() {
        JsonObject mission = new JsonObject();
        mission.addProperty("id", "mission-123");
        mission.addProperty("type", "mining");

        CloudflareClient.SyncResult result = new CloudflareClient.SyncResult(
            true, "Sync successful", mission
        );

        assertTrue(result.synced);
        assertEquals("Sync successful", result.message);
        assertEquals(mission, result.mission);
    }

    @Test
    @DisplayName("SyncResult - Failed sync")
    void testSyncResult_Failed() {
        CloudflareClient.SyncResult result = new CloudflareClient.SyncResult(
            false, "Connection error", null
        );

        assertFalse(result.synced);
        assertEquals("Connection error", result.message);
        assertNull(result.mission);
    }

    @Test
    @DisplayName("SyncResult - No mission available")
    void testSyncResult_NoMission() {
        CloudflareClient.SyncResult result = new CloudflareClient.SyncResult(
            true, "No mission", null
        );

        assertTrue(result.synced);
        assertEquals("No mission", result.message);
        assertNull(result.mission);
    }

    // ==================== MISSION DATA TESTS ====================

    @Test
    @DisplayName("MissionData - Complete construction")
    void testMissionData_Complete() {
        JsonObject payload = new JsonObject();
        payload.addProperty("target", "iron_ore");
        payload.addProperty("count", 32);

        CloudflareClient.MissionData mission = new CloudflareClient.MissionData(
            "mission-456", "gathering", "Gather iron ore", payload
        );

        assertEquals("mission-456", mission.id);
        assertEquals("gathering", mission.type);
        assertEquals("Gather iron ore", mission.description);
        assertEquals(payload, mission.payload);
    }

    @Test
    @DisplayName("MissionData - Minimal construction")
    void testMissionData_Minimal() {
        CloudflareClient.MissionData mission = new CloudflareClient.MissionData(
            "mission-789", "patrol", "Patrol area", null
        );

        assertEquals("mission-789", mission.id);
        assertEquals("patrol", mission.type);
        assertEquals("Patrol area", mission.description);
        assertNull(mission.payload);
    }

    @Test
    @DisplayName("MissionData - Null description")
    void testMissionData_NullDescription() {
        CloudflareClient.MissionData mission = new CloudflareClient.MissionData(
            "mission-001", "test", null, null
        );

        assertEquals("mission-001", mission.id);
        assertEquals("test", mission.type);
        assertNull(mission.description);
        assertNull(mission.payload);
    }

    @Test
    @DisplayName("MissionData - Complex payload")
    void testMissionData_ComplexPayload() {
        JsonObject payload = new JsonObject();
        payload.addProperty("target", "diamond_ore");
        payload.addProperty("count", 64);
        payload.addProperty("priority", "high");

        JsonObject location = new JsonObject();
        location.addProperty("x", 100);
        location.addProperty("y", 12);
        location.addProperty("z", 200);
        payload.add("location", location);

        CloudflareClient.MissionData mission = new CloudflareClient.MissionData(
            "mission-diamond", "mining", "Mine diamonds at depth", payload
        );

        assertEquals(payload, mission.payload);
        assertEquals("diamond_ore", mission.payload.get("target").getAsString());
        assertEquals(64, mission.payload.get("count").getAsInt());
    }

    // ==================== COMPLEX SCENARIOS ====================

    @Test
    @DisplayName("Multiple agents - Different positions")
    void testMultipleAgents_DifferentPositions() {
        CloudflareClient.AgentSyncState agent1 = new CloudflareClient.AgentSyncState(
            0, 64, 0, "working", 20.0f, 18, "mining"
        );
        CloudflareClient.AgentSyncState agent2 = new CloudflareClient.AgentSyncState(
            100, 64, 100, "idle", 15.0f, 10, "none"
        );
        CloudflareClient.AgentSyncState agent3 = new CloudflareClient.AgentSyncState(
            -50, 64, -50, "working", 18.0f, 16, "building"
        );

        assertEquals(0, agent1.position[0]);
        assertEquals(100, agent2.position[0]);
        assertEquals(-50, agent3.position[0]);
    }

    @Test
    @DisplayName("Multiple agents - Different health states")
    void testMultipleAgents_DifferentHealthStates() {
        CloudflareClient.AgentSyncState healthy = new CloudflareClient.AgentSyncState(
            0, 64, 0, "working", 20.0f, 20, "mining"
        );
        CloudflareClient.AgentSyncState wounded = new CloudflareClient.AgentSyncState(
            100, 64, 100, "idle", 8.0f, 15, "healing"
        );
        CloudflareClient.AgentSyncState critical = new CloudflareClient.AgentSyncState(
            -50, 64, -50, "working", 2.0f, 5, "fleeing"
        );

        assertEquals(20.0f, healthy.health);
        assertEquals(8.0f, wounded.health);
        assertEquals(2.0f, critical.health);
    }

    @Test
    @DisplayName("Mission types - Different mission types")
    void testMissionTypes_DifferentTypes() {
        JsonObject miningPayload = new JsonObject();
        miningPayload.addProperty("resource", "iron");

        JsonObject combatPayload = new JsonObject();
        combatPayload.addProperty("target", "zombie");
        combatPayload.addProperty("count", 5);

        CloudflareClient.MissionData mining = new CloudflareClient.MissionData(
            "mission-1", "mining", "Gather iron", miningPayload
        );
        CloudflareClient.MissionData combat = new CloudflareClient.MissionData(
            "mission-2", "combat", "Defeat zombies", combatPayload
        );
        CloudflareClient.MissionData patrol = new CloudflareClient.MissionData(
            "mission-3", "patrol", "Patrol perimeter", null
        );

        assertEquals("mining", mining.type);
        assertEquals("combat", combat.type);
        assertEquals("patrol", patrol.type);
    }

    @Test
    @DisplayName("Tactical decisions - Priority ordering")
    void testTacticalDecisions_PriorityOrdering() {
        CloudflareClient.TacticalDecision lowPriority = new CloudflareClient.TacticalDecision(
            "proceed", 0.1f, "Clear path", 10
        );
        CloudflareClient.TacticalDecision mediumPriority = new CloudflareClient.TacticalDecision(
            "attack", 0.5f, "Enemy nearby", 10
        );
        CloudflareClient.TacticalDecision highPriority = new CloudflareClient.TacticalDecision(
            "retreat", 0.95f, "Overwhelmed", 10
        );

        assertTrue(lowPriority.priority < mediumPriority.priority);
        assertTrue(mediumPriority.priority < highPriority.priority);
    }
}
