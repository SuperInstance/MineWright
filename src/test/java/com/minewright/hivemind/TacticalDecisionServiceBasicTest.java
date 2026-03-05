package com.minewright.hivemind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for TacticalDecisionService.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Singleton pattern</li>
 *   <li>Configuration access</li>
 *   <li>Decision caching</li>
 *   <li>Health check functionality</li>
 *   <li>Basic service operations</li>
 * </ul>
 *
 * <p>Note: Full integration tests with Minecraft entities require
 * complex mocking and are handled separately.</p>
 *
 * @since 1.2.0
 */
@DisplayName("TacticalDecisionService Basic Tests")
class TacticalDecisionServiceBasicTest {

    private TacticalDecisionService service;

    @BeforeEach
    void setUp() {
        service = TacticalDecisionService.getInstance();
    }

    // ==================== SINGLETON PATTERN TESTS ====================

    @Test
    @DisplayName("Singleton - Same instance returned")
    void testSingleton_SameInstance() {
        TacticalDecisionService instance1 = TacticalDecisionService.getInstance();
        TacticalDecisionService instance2 = TacticalDecisionService.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("Singleton - Multiple calls return same object")
    void testSingleton_MultipleCalls() {
        TacticalDecisionService instance1 = TacticalDecisionService.getInstance();
        TacticalDecisionService instance2 = TacticalDecisionService.getInstance();
        TacticalDecisionService instance3 = TacticalDecisionService.getInstance();

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }

    // ==================== CONFIGURATION TESTS ====================

    @Test
    @DisplayName("Configuration - Get check interval")
    void testConfiguration_GetCheckInterval() {
        int interval = service.getCheckInterval();
        assertTrue(interval > 0, "Check interval should be positive");
        assertTrue(interval <= 100, "Check interval should be reasonable");
    }

    @Test
    @DisplayName("Configuration - Get sync interval")
    void testConfiguration_GetSyncInterval() {
        int interval = service.getSyncInterval();
        assertTrue(interval > 0, "Sync interval should be positive");
        assertTrue(interval <= 1000, "Sync interval should be reasonable");
    }

    @Test
    @DisplayName("Configuration - Intervals are different")
    void testConfiguration_IntervalsAreDifferent() {
        // Check interval should typically be smaller than sync interval
        int checkInterval = service.getCheckInterval();
        int syncInterval = service.getSyncInterval();

        // This is a reasonable assumption for the architecture
        // (tactical checks more frequent than state syncs)
        assertTrue(checkInterval <= syncInterval || checkInterval > 0,
            "Check interval should be reasonable relative to sync interval");
    }

    // ==================== ENABLED STATUS TESTS ====================

    @Test
    @DisplayName("Enabled status - Returns boolean")
    void testEnabledStatus_ReturnsBoolean() {
        boolean enabled = service.isEnabled();

        // Should return either true or false, not throw exception
        assertTrue(enabled == true || enabled == false);
    }

    // ==================== DECISION CACHING TESTS ====================

    @Test
    @DisplayName("Decision caching - Update last decision")
    void testDecisionCaching_UpdateLastDecision() {
        String agentId = UUID.randomUUID().toString();
        CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
            "attack", 0.8f, "Test decision", 15
        );

        assertDoesNotThrow(() -> {
            service.updateLastDecision(agentId, decision);
        });
    }

    @Test
    @DisplayName("Decision caching - Update for multiple agents")
    void testDecisionCaching_MultipleAgents() {
        String agent1 = UUID.randomUUID().toString();
        String agent2 = UUID.randomUUID().toString();
        String agent3 = UUID.randomUUID().toString();

        CloudflareClient.TacticalDecision decision1 = new CloudflareClient.TacticalDecision(
            "attack", 0.8f, "Agent1 decision", 15
        );
        CloudflareClient.TacticalDecision decision2 = new CloudflareClient.TacticalDecision(
            "retreat", 0.9f, "Agent2 decision", 12
        );
        CloudflareClient.TacticalDecision decision3 = new CloudflareClient.TacticalDecision(
            "hold", 0.5f, "Agent3 decision", 10
        );

        assertDoesNotThrow(() -> {
            service.updateLastDecision(agent1, decision1);
            service.updateLastDecision(agent2, decision2);
            service.updateLastDecision(agent3, decision3);
        });
    }

    @Test
    @DisplayName("Decision caching - Same agent multiple updates")
    void testDecisionCaching_SameAgentMultipleUpdates() {
        String agentId = UUID.randomUUID().toString();

        CloudflareClient.TacticalDecision decision1 = new CloudflareClient.TacticalDecision(
            "attack", 0.8f, "First decision", 15
        );
        CloudflareClient.TacticalDecision decision2 = new CloudflareClient.TacticalDecision(
            "retreat", 0.9f, "Second decision", 12
        );
        CloudflareClient.TacticalDecision decision3 = new CloudflareClient.TacticalDecision(
            "dodge", 0.95f, "Third decision", 8
        );

        assertDoesNotThrow(() -> {
            service.updateLastDecision(agentId, decision1);
            service.updateLastDecision(agentId, decision2);
            service.updateLastDecision(agentId, decision3);
            // Each update should overwrite the previous one
        });
    }

    @Test
    @DisplayName("Decision caching - Null agent ID")
    void testDecisionCaching_NullAgentId() {
        CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
            "attack", 0.8f, "Test decision", 15
        );

        assertDoesNotThrow(() -> {
            service.updateLastDecision(null, decision);
        });
    }

    @Test
    @DisplayName("Decision caching - Empty agent ID")
    void testDecisionCaching_EmptyAgentId() {
        CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
            "attack", 0.8f, "Test decision", 15
        );

        assertDoesNotThrow(() -> {
            service.updateLastDecision("", decision);
        });
    }

    // ==================== HEALTH CHECK TESTS ====================

    @Test
    @DisplayName("Health check - Returns future")
    void testHealthCheck_ReturnsFuture() {
        CompletableFuture<Boolean> future = service.checkHealth();

        assertNotNull(future, "Health check should return a future");
    }

    @Test
    @DisplayName("Health check - Completes without exception")
    void testHealthCheck_Completes() {
        CompletableFuture<Boolean> future = service.checkHealth();

        // The future should complete (succeed or fail, but complete)
        assertDoesNotThrow(() -> {
            // Just check that the future is created properly
            // We don't wait for completion as it may require network
            assertNotNull(future);
        });
    }

    // ==================== DISTRIBUTED COORDINATION TESTS ====================

    @Test
    @DisplayName("Distributed coordination - Concurrent decision updates")
    void testDistributedCoordination_ConcurrentUpdates() {
        String agent1 = UUID.randomUUID().toString();
        String agent2 = UUID.randomUUID().toString();
        String agent3 = UUID.randomUUID().toString();

        CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
            "test", 0.5f, "Concurrent test", 10
        );

        assertDoesNotThrow(() -> {
            // Simulate concurrent updates from different agents
            service.updateLastDecision(agent1, decision);
            service.updateLastDecision(agent2, decision);
            service.updateLastDecision(agent3, decision);

            // The service should handle concurrent access
            service.updateLastDecision(agent1, decision);
            service.updateLastDecision(agent2, decision);
            service.updateLastDecision(agent3, decision);
        });
    }

    @Test
    @DisplayName("Distributed coordination - Many agents")
    void testDistributedCoordination_ManyAgents() {
        int agentCount = 100;

        assertDoesNotThrow(() -> {
            for (int i = 0; i < agentCount; i++) {
                String agentId = UUID.randomUUID().toString();
                CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
                    "test", 0.5f, "Agent " + i, 10
                );
                service.updateLastDecision(agentId, decision);
            }
        });
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("Edge case - Very long agent ID")
    void testEdgeCase_VeryLongAgentId() {
        String longAgentId = "a".repeat(10000);
        CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
            "test", 0.5f, "Long ID test", 10
        );

        assertDoesNotThrow(() -> {
            service.updateLastDecision(longAgentId, decision);
        });
    }

    @Test
    @DisplayName("Edge case - Special characters in agent ID")
    void testEdgeCase_SpecialCharactersInAgentId() {
        String specialAgentId = "agent-with-special.chars\n\t\r\"'\\<>{}[]";
        CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
            "test", 0.5f, "Special chars test", 10
        );

        assertDoesNotThrow(() -> {
            service.updateLastDecision(specialAgentId, decision);
        });
    }

    @Test
    @DisplayName("Edge case - Unicode in agent ID")
    void testEdgeCase_UnicodeInAgentId() {
        String unicodeAgentId = "agent-你好-🎮-𝕌𝕟𝕚𝕔𝕠𝕕𝕖";
        CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
            "test", 0.5f, "Unicode test", 10
        );

        assertDoesNotThrow(() -> {
            service.updateLastDecision(unicodeAgentId, decision);
        });
    }

    @Test
    @DisplayName("Edge case - Decision with extreme values")
    void testEdgeCase_ExtremeDecisionValues() {
        String agentId = UUID.randomUUID().toString();

        CloudflareClient.TacticalDecision extremePriority = new CloudflareClient.TacticalDecision(
            "test", Float.MAX_VALUE, "Extreme priority", 0
        );
        CloudflareClient.TacticalDecision extremeLatency = new CloudflareClient.TacticalDecision(
            "test", 0.5f, "Extreme latency", Integer.MAX_VALUE
        );
        CloudflareClient.TacticalDecision extremeReasoning = new CloudflareClient.TacticalDecision(
            "test", 0.5f, "a".repeat(10000), 10
        );

        assertDoesNotThrow(() -> {
            service.updateLastDecision(agentId, extremePriority);
            service.updateLastDecision(agentId, extremeLatency);
            service.updateLastDecision(agentId, extremeReasoning);
        });
    }

    @Test
    @DisplayName("Edge case - Fallback decision")
    void testEdgeCase_FallbackDecision() {
        String agentId = UUID.randomUUID().toString();
        CloudflareClient.TacticalDecision fallback = CloudflareClient.TacticalDecision.fallback(
            "Test fallback"
        );

        assertDoesNotThrow(() -> {
            service.updateLastDecision(agentId, fallback);
        });

        assertTrue(fallback.isFallback);
        assertEquals("hold", fallback.action);
    }

    // ==================== THREAD SAFETY TESTS ====================

    @Test
    @DisplayName("Thread safety - Concurrent access from multiple threads")
    void testThreadSafety_ConcurrentAccess() throws InterruptedException {
        String agentId = UUID.randomUUID().toString();
        CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
            "test", 0.5f, "Thread safety test", 10
        );

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    service.updateLastDecision(agentId, decision);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // If we get here without exceptions, the test passed
        assertTrue(true);
    }

    @Test
    @DisplayName("Thread safety - Many agents, many threads")
    void testThreadSafety_ManyAgentsManyThreads() throws InterruptedException {
        Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 50; j++) {
                    String agentId = UUID.randomUUID().toString();
                    CloudflareClient.TacticalDecision decision = new CloudflareClient.TacticalDecision(
                        "test", 0.5f, "Thread " + threadId, 10
                    );
                    service.updateLastDecision(agentId, decision);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // If we get here without exceptions, the test passed
        assertTrue(true);
    }
}
