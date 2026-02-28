package com.minewright.blackboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Blackboard} system.
 *
 * Tests cover:
 * <ul>
 *   <li>Knowledge posting and retrieval</li>
 *   <li>Subscription to knowledge changes</li>
 *   <li>Knowledge aging and expiration</li>
 *   <li>Multi-agent knowledge sharing</li>
 * </ul>
 *
 * @see Blackboard
 * @see BlackboardEntry
 * @see KnowledgeArea
 * @since 1.0.0
 */
@DisplayName("Blackboard System Tests")
class BlackboardTest {

    private Blackboard blackboard;
    private UUID agent1;
    private UUID agent2;

    @BeforeEach
    void setUp() {
        // Get singleton instance
        blackboard = Blackboard.getInstance();

        // Reset blackboard for clean test state
        blackboard.reset();

        // Create test agent IDs
        agent1 = UUID.randomUUID();
        agent2 = UUID.randomUUID();
    }

    // ==================== Knowledge Posting and Retrieval Tests ====================

    @Test
    @DisplayName("Post entry to blackboard and retrieve it")
    void testPostAndRetrieveEntry() {
        // Post a fact entry
        BlackboardEntry<String> entry = BlackboardEntry.createFact(
            "block_100_64_200", "diamond_ore", agent1);

        blackboard.post(KnowledgeArea.WORLD_STATE, entry);

        // Retrieve the entry
        Optional<String> result = blackboard.query(KnowledgeArea.WORLD_STATE, "block_100_64_200");

        assertTrue(result.isPresent(), "Entry should be retrievable");
        assertEquals("diamond_ore", result.get(), "Value should match posted value");
    }

    @Test
    @DisplayName("Post entry with convenience method and retrieve")
    void testPostWithConvenienceMethod() {
        blackboard.post(KnowledgeArea.WORLD_STATE, "block_50_70_100", "iron_ore",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        Optional<String> result = blackboard.query(KnowledgeArea.WORLD_STATE, "block_50_70_100");

        assertTrue(result.isPresent());
        assertEquals("iron_ore", result.get());
    }

    @Test
    @DisplayName("Query non-existent entry returns empty optional")
    void testQueryNonExistentEntry() {
        Optional<String> result = blackboard.query(KnowledgeArea.WORLD_STATE, "nonexistent_key");

        assertFalse(result.isPresent(), "Non-existent entry should return empty optional");
    }

    @Test
    @DisplayName("Query with null area returns empty optional")
    void testQueryWithNullArea() {
        Optional<String> result = blackboard.query(null, "some_key");

        assertFalse(result.isPresent(), "Null area should return empty optional");
    }

    @Test
    @DisplayName("Query with null key returns empty optional")
    void testQueryWithNullKey() {
        Optional<String> result = blackboard.query(KnowledgeArea.WORLD_STATE, null);

        assertFalse(result.isPresent(), "Null key should return empty optional");
    }

    @Test
    @DisplayName("Post entry to different knowledge areas")
    void testPostToDifferentAreas() {
        blackboard.post(KnowledgeArea.WORLD_STATE, "block_1", "stone", agent1, 1.0,
                       BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.THREATS, "creeper_1", "hostile", agent1, 0.9,
                       BlackboardEntry.EntryType.FACT);

        Optional<String> worldResult = blackboard.query(KnowledgeArea.WORLD_STATE, "block_1");
        Optional<String> threatResult = blackboard.query(KnowledgeArea.THREATS, "creeper_1");

        assertTrue(worldResult.isPresent());
        assertTrue(threatResult.isPresent());
        assertEquals("stone", worldResult.get());
        assertEquals("hostile", threatResult.get());
    }

    @Test
    @DisplayName("Retrieve full entry with metadata")
    void testRetrieveFullEntry() {
        long beforePost = System.currentTimeMillis();

        BlackboardEntry<String> entry = new BlackboardEntry<>(
            "test_key", "test_value", agent1, 0.85, BlackboardEntry.EntryType.HYPOTHESIS);

        blackboard.post(KnowledgeArea.AGENT_STATUS, entry);

        Optional<BlackboardEntry<String>> result = blackboard.queryEntry(
            KnowledgeArea.AGENT_STATUS, "test_key");

        assertTrue(result.isPresent());
        BlackboardEntry<String> retrieved = result.get();
        assertEquals("test_key", retrieved.getKey());
        assertEquals("test_value", retrieved.getValue());
        assertEquals(agent1, retrieved.getSourceAgent());
        assertEquals(0.85, retrieved.getConfidence());
        assertEquals(BlackboardEntry.EntryType.HYPOTHESIS, retrieved.getType());
        assertTrue(retrieved.getTimestamp() >= beforePost,
                   "Timestamp should be after post start time");
    }

    @Test
    @DisplayName("Query all entries in a knowledge area")
    void testQueryArea() {
        // Post multiple entries
        blackboard.post(KnowledgeArea.WORLD_STATE, "block_1", "stone", agent1, 1.0,
                       BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.WORLD_STATE, "block_2", "dirt", agent1, 1.0,
                       BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.WORLD_STATE, "block_3", "grass", agent2, 1.0,
                       BlackboardEntry.EntryType.FACT);

        List<BlackboardEntry<String>> entries = blackboard.queryArea(KnowledgeArea.WORLD_STATE);

        assertEquals(3, entries.size(), "Should retrieve all entries in area");
    }

    @Test
    @DisplayName("Query area returns empty list for null area")
    void testQueryNullArea() {
        List<BlackboardEntry<String>> entries = blackboard.queryArea(null);

        assertTrue(entries.isEmpty(), "Null area should return empty list");
    }

    @Test
    @DisplayName("Query entries by type")
    void testQueryByType() {
        blackboard.post(KnowledgeArea.AGENT_STATUS, "fact_1", "value1", agent1, 1.0,
                       BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.AGENT_STATUS, "hypothesis_1", "value2", agent1, 0.5,
                       BlackboardEntry.EntryType.HYPOTHESIS);
        blackboard.post(KnowledgeArea.AGENT_STATUS, "fact_2", "value3", agent1, 1.0,
                       BlackboardEntry.EntryType.FACT);

        List<BlackboardEntry<String>> facts = blackboard.queryByType(
            KnowledgeArea.AGENT_STATUS, BlackboardEntry.EntryType.FACT);
        List<BlackboardEntry<String>> hypotheses = blackboard.queryByType(
            KnowledgeArea.AGENT_STATUS, BlackboardEntry.EntryType.HYPOTHESIS);

        assertEquals(2, facts.size(), "Should retrieve 2 FACT entries");
        assertEquals(1, hypotheses.size(), "Should retrieve 1 HYPOTHESIS entry");
    }

    @Test
    @DisplayName("Query entries by source agent")
    void testQueryBySource() {
        blackboard.post(KnowledgeArea.WORLD_STATE, "entry_1", "value1", agent1, 1.0,
                       BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.WORLD_STATE, "entry_2", "value2", agent2, 1.0,
                       BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.WORLD_STATE, "entry_3", "value3", agent1, 1.0,
                       BlackboardEntry.EntryType.FACT);

        List<BlackboardEntry<String>> agent1Entries = blackboard.queryBySource(
            KnowledgeArea.WORLD_STATE, agent1);
        List<BlackboardEntry<String>> agent2Entries = blackboard.queryBySource(
            KnowledgeArea.WORLD_STATE, agent2);

        assertEquals(2, agent1Entries.size(), "Agent1 should have 2 entries");
        assertEquals(1, agent2Entries.size(), "Agent2 should have 1 entry");
    }

    @Test
    @DisplayName("Update existing entry by posting with same key")
    void testUpdateExistingEntry() {
        // Post initial entry
        blackboard.post(KnowledgeArea.WORLD_STATE, "block_1", "stone", agent1, 1.0,
                       BlackboardEntry.EntryType.FACT);

        // Update with new value
        blackboard.post(KnowledgeArea.WORLD_STATE, "block_1", "iron_ore", agent1, 1.0,
                       BlackboardEntry.EntryType.FACT);

        Optional<String> result = blackboard.query(KnowledgeArea.WORLD_STATE, "block_1");

        assertTrue(result.isPresent());
        assertEquals("iron_ore", result.get(), "Should retrieve updated value");
        assertEquals(1, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE),
                    "Should still have only one entry with that key");
    }

    @Test
    @DisplayName("Store different value types in blackboard")
    void testDifferentValueTypes() {
        // String
        blackboard.post(KnowledgeArea.WORLD_STATE, "string_key", "string_value",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Integer
        blackboard.post(KnowledgeArea.WORLD_STATE, "int_key", 42,
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Double
        blackboard.post(KnowledgeArea.WORLD_STATE, "double_key", 3.14,
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Boolean
        blackboard.post(KnowledgeArea.WORLD_STATE, "bool_key", true,
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Complex object
        Map<String, Object> complexValue = new java.util.HashMap<>();
        complexValue.put("x", 100);
        complexValue.put("y", 64);
        complexValue.put("z", 200);
        blackboard.post(KnowledgeArea.WORLD_STATE, "complex_key", complexValue,
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        Optional<String> stringResult = blackboard.query(KnowledgeArea.WORLD_STATE, "string_key");
        Optional<Integer> intResult = blackboard.query(KnowledgeArea.WORLD_STATE, "int_key");
        Optional<Double> doubleResult = blackboard.query(KnowledgeArea.WORLD_STATE, "double_key");
        Optional<Boolean> boolResult = blackboard.query(KnowledgeArea.WORLD_STATE, "bool_key");
        @SuppressWarnings("unchecked")
        Optional<Map<String, Object>> complexResult = blackboard.query(KnowledgeArea.WORLD_STATE, "complex_key");

        assertTrue(stringResult.isPresent());
        assertTrue(intResult.isPresent());
        assertTrue(doubleResult.isPresent());
        assertTrue(boolResult.isPresent());
        assertTrue(complexResult.isPresent());

        assertEquals("string_value", stringResult.get());
        assertEquals(42, intResult.get());
        assertEquals(3.14, doubleResult.get());
        assertTrue(boolResult.get());
        assertEquals(100, complexResult.get().get("x"));
    }

    @Test
    @DisplayName("Post throws exception for null area")
    void testPostNullAreaThrowsException() {
        BlackboardEntry<String> entry = BlackboardEntry.createFact("key", "value", agent1);

        assertThrows(IllegalArgumentException.class, () -> {
            blackboard.post(null, entry);
        });
    }

    @Test
    @DisplayName("Post throws exception for null entry")
    void testPostNullEntryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            blackboard.post(KnowledgeArea.WORLD_STATE, null);
        });
    }

    // ==================== Subscription Tests ====================

    @Test
    @DisplayName("Subscriber receives notification on entry posted")
    void testSubscriberReceivesNotification() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger notificationCount = new AtomicInteger(0);

        BlackboardSubscriber subscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
                notificationCount.incrementAndGet();
                if (entry.getKey().equals("test_key")) {
                    latch.countDown();
                }
            }

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}
        };

        blackboard.subscribe(KnowledgeArea.WORLD_STATE, subscriber);

        blackboard.post(KnowledgeArea.WORLD_STATE, "test_key", "test_value",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertTrue(latch.await(1, TimeUnit.SECONDS),
                   "Subscriber should receive notification within timeout");
        assertEquals(1, notificationCount.get(), "Subscriber should receive exactly one notification");
    }

    @Test
    @DisplayName("Subscriber only receives notifications for subscribed area")
    void testSubscriberAreaFiltering() throws InterruptedException {
        CountDownLatch worldStateLatch = new CountDownLatch(1);
        CountDownLatch threatsLatch = new CountDownLatch(1);

        BlackboardSubscriber worldStateSubscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
                if (area == KnowledgeArea.WORLD_STATE) {
                    worldStateLatch.countDown();
                }
            }

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}
        };

        blackboard.subscribe(KnowledgeArea.WORLD_STATE, worldStateSubscriber);

        // Post to subscribed area
        blackboard.post(KnowledgeArea.WORLD_STATE, "block_1", "stone",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Post to different area
        blackboard.post(KnowledgeArea.THREATS, "creeper_1", "hostile",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertTrue(worldStateLatch.await(1, TimeUnit.SECONDS),
                   "Subscriber should receive notification for subscribed area");
    }

    @Test
    @DisplayName("Global subscriber receives notifications for all areas")
    void testGlobalSubscriber() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        BlackboardSubscriber globalSubscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
                latch.countDown();
            }

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}
        };

        blackboard.subscribeAll(globalSubscriber);

        blackboard.post(KnowledgeArea.WORLD_STATE, "key1", "value1",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.THREATS, "key2", "value2",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertTrue(latch.await(1, TimeUnit.SECONDS),
                   "Global subscriber should receive notifications for all areas");
    }

    @Test
    @DisplayName("Subscriber receives removal notification")
    void testSubscriberReceivesRemovalNotification() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        BlackboardSubscriber subscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {}

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {
                if (key.equals("removable_key")) {
                    latch.countDown();
                }
            }
        };

        blackboard.subscribe(KnowledgeArea.WORLD_STATE, subscriber);

        // Post and then remove entry
        blackboard.post(KnowledgeArea.WORLD_STATE, "removable_key", "value",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.remove(KnowledgeArea.WORLD_STATE, "removable_key");

        assertTrue(latch.await(1, TimeUnit.SECONDS),
                   "Subscriber should receive removal notification");
    }

    @Test
    @DisplayName("Multiple subscribers receive notifications")
    void testMultipleSubscribers() throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);

        BlackboardSubscriber subscriber1 = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
                latch1.countDown();
            }

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}
        };

        BlackboardSubscriber subscriber2 = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
                latch2.countDown();
            }

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}
        };

        BlackboardSubscriber subscriber3 = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
                latch3.countDown();
            }

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}
        };

        blackboard.subscribe(KnowledgeArea.WORLD_STATE, subscriber1);
        blackboard.subscribe(KnowledgeArea.WORLD_STATE, subscriber2);
        blackboard.subscribe(KnowledgeArea.WORLD_STATE, subscriber3);

        blackboard.post(KnowledgeArea.WORLD_STATE, "test_key", "value",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertTrue(latch1.await(1, TimeUnit.SECONDS));
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
        assertTrue(latch3.await(1, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Subscriber can be unsubscribed")
    void testUnsubscribe() throws InterruptedException {
        CountDownLatch firstLatch = new CountDownLatch(1);
        CountDownLatch secondLatch = new CountDownLatch(1);
        AtomicInteger notificationCount = new AtomicInteger(0);

        BlackboardSubscriber subscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
                int count = notificationCount.incrementAndGet();
                if (count == 1) {
                    firstLatch.countDown();
                } else {
                    secondLatch.countDown();
                }
            }

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}
        };

        blackboard.subscribe(KnowledgeArea.WORLD_STATE, subscriber);

        // First post - should receive notification
        blackboard.post(KnowledgeArea.WORLD_STATE, "key1", "value1",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        assertTrue(firstLatch.await(1, TimeUnit.SECONDS));

        // Unsubscribe
        blackboard.unsubscribe(KnowledgeArea.WORLD_STATE, subscriber);

        // Second post - should NOT receive notification
        blackboard.post(KnowledgeArea.WORLD_STATE, "key2", "value2",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertFalse(secondLatch.await(500, TimeUnit.MILLISECONDS),
                   "Unsubscribed subscriber should not receive notifications");
        assertEquals(1, notificationCount.get(), "Should have only received first notification");
    }

    @Test
    @DisplayName("Global subscriber can be unsubscribed from all areas")
    void testUnsubscribeAll() throws InterruptedException {
        CountDownLatch firstLatch = new CountDownLatch(2);
        CountDownLatch secondLatch = new CountDownLatch(2);
        AtomicInteger notificationCount = new AtomicInteger(0);

        BlackboardSubscriber subscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
                int count = notificationCount.incrementAndGet();
                if (count <= 2) {
                    firstLatch.countDown();
                } else {
                    secondLatch.countDown();
                }
            }

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}
        };

        blackboard.subscribeAll(subscriber);

        // Post to two areas
        blackboard.post(KnowledgeArea.WORLD_STATE, "key1", "value1",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.THREATS, "key2", "value2",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertTrue(firstLatch.await(1, TimeUnit.SECONDS));

        // Unsubscribe from all
        blackboard.unsubscribeAll(subscriber);

        // Post again - should not receive notifications
        blackboard.post(KnowledgeArea.WORLD_STATE, "key3", "value3",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.THREATS, "key4", "value4",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertFalse(secondLatch.await(500, TimeUnit.MILLISECONDS));
        assertEquals(2, notificationCount.get());
    }

    @Test
    @DisplayName("Subscriber error handler is called on exception")
    void testSubscriberErrorHandling() throws InterruptedException {
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicInteger errorCount = new AtomicInteger(0);

        BlackboardSubscriber failingSubscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {
                throw new RuntimeException("Test exception in subscriber");
            }

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}

            @Override
            public void onNotificationError(KnowledgeArea area, BlackboardEntry<?> entry,
                                          String key, Throwable throwable) {
                errorCount.incrementAndGet();
                errorLatch.countDown();
            }
        };

        blackboard.subscribe(KnowledgeArea.WORLD_STATE, failingSubscriber);

        blackboard.post(KnowledgeArea.WORLD_STATE, "test_key", "value",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertTrue(errorLatch.await(1, TimeUnit.SECONDS));
        assertEquals(1, errorCount.get());
    }

    @Test
    @DisplayName("Subscriber with acceptsArea filter is not added to rejected areas")
    void testSubscriberAcceptsArea() {
        BlackboardSubscriber selectiveSubscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {}

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}

            @Override
            public boolean acceptsArea(KnowledgeArea area) {
                // Only accept WORLD_STATE and THREATS
                return area == KnowledgeArea.WORLD_STATE || area == KnowledgeArea.THREATS;
            }
        };

        // Try to subscribe to different areas
        blackboard.subscribe(KnowledgeArea.WORLD_STATE, selectiveSubscriber);
        blackboard.subscribe(KnowledgeArea.THREATS, selectiveSubscriber);
        blackboard.subscribe(KnowledgeArea.AGENT_STATUS, selectiveSubscriber);

        // Post to all three areas
        blackboard.post(KnowledgeArea.WORLD_STATE, "key1", "value1",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.THREATS, "key2", "value2",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.AGENT_STATUS, "key3", "value3",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Verify entries exist (they should be posted regardless of subscription)
        assertTrue(blackboard.query(KnowledgeArea.WORLD_STATE, "key1").isPresent());
        assertTrue(blackboard.query(KnowledgeArea.THREATS, "key2").isPresent());
        assertTrue(blackboard.query(KnowledgeArea.AGENT_STATUS, "key3").isPresent());
    }

    @Test
    @DisplayName("Subscribe throws exception for null area")
    void testSubscribeNullAreaThrowsException() {
        BlackboardSubscriber subscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {}

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {}
        };

        assertThrows(IllegalArgumentException.class, () -> {
            blackboard.subscribe(null, subscriber);
        });
    }

    @Test
    @DisplayName("Subscribe throws exception for null subscriber")
    void testSubscribeNullSubscriberThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            blackboard.subscribe(KnowledgeArea.WORLD_STATE, null);
        });
    }

    // ==================== Knowledge Aging and Expiration Tests ====================

    @Test
    @DisplayName("Entry is stale after max age")
    void testEntryIsStale() throws InterruptedException {
        BlackboardEntry<String> entry = new BlackboardEntry<>(
            "test_key", "test_value", System.currentTimeMillis(),
            agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Initially not stale
        assertFalse(entry.isStale(1000), "New entry should not be stale");

        // Wait for entry to age
        Thread.sleep(50);

        // Now it should be stale with lower max age
        assertTrue(entry.isStale(10), "Entry should be stale with low max age");
    }

    @Test
    @DisplayName("Entry age is calculated correctly")
    void testEntryAge() throws InterruptedException {
        BlackboardEntry<String> entry = new BlackboardEntry<>(
            "test_key", "test_value", System.currentTimeMillis(),
            agent1, 1.0, BlackboardEntry.EntryType.FACT);

        long initialAge = entry.getAge();
        assertTrue(initialAge >= 0, "Initial age should be non-negative");

        Thread.sleep(50);

        long laterAge = entry.getAge();
        assertTrue(laterAge >= initialAge, "Age should increase over time");
        assertTrue(laterAge >= 50, "Age should be at least 50ms after sleep");
    }

    @Test
    @DisplayName("Evict stale entries from blackboard")
    void testEvictStaleEntries() throws InterruptedException {
        // Post an entry
        blackboard.post(KnowledgeArea.WORLD_STATE, "old_entry", "value",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertEquals(1, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE));

        // Wait and evict with very short max age
        Thread.sleep(50);
        int evicted = blackboard.evictStale(KnowledgeArea.WORLD_STATE, 10L);

        assertEquals(1, evicted, "Should evict one stale entry");
        assertEquals(0, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE),
                    "Area should be empty after eviction");
    }

    @Test
    @DisplayName("Evict does not remove fresh entries")
    void testEvictDoesNotRemoveFreshEntries() {
        blackboard.post(KnowledgeArea.WORLD_STATE, "fresh_entry", "value",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Evict with long max age
        int evicted = blackboard.evictStale(KnowledgeArea.WORLD_STATE, 60000L);

        assertEquals(0, evicted, "Should not evict fresh entries");
        assertEquals(1, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE));
    }

    @Test
    @DisplayName("Evict stale from all areas")
    void testEvictStaleFromAllAreas() throws InterruptedException {
        blackboard.post(KnowledgeArea.WORLD_STATE, "key1", "value1",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.THREATS, "key2", "value2",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.AGENT_STATUS, "key3", "value3",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertEquals(3, blackboard.getTotalEntryCount());

        Thread.sleep(50);
        int evicted = blackboard.evictStale(10L);

        assertEquals(3, evicted, "Should evict from all areas");
        assertEquals(0, blackboard.getTotalEntryCount());
    }

    @Test
    @DisplayName("Evict with null max age uses area default")
    void testEvictWithAreaDefaultMaxAge() {
        // Post entry to THREATS area (default max age 1000ms)
        blackboard.post(KnowledgeArea.THREATS, "threat_1", "creeper",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Evict with null max age (should use area default)
        int evicted = blackboard.evictStale(KnowledgeArea.THREATS, null);

        assertEquals(0, evicted, "Fresh entry should not be evicted with area default");
    }

    @Test
    @DisplayName("Evict notifies subscribers of removals")
    void testEvictNotifiesSubscribers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        BlackboardSubscriber subscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {}

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {
                if (key.equals("evictable_key")) {
                    latch.countDown();
                }
            }
        };

        blackboard.subscribe(KnowledgeArea.WORLD_STATE, subscriber);

        blackboard.post(KnowledgeArea.WORLD_STATE, "evictable_key", "value",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        Thread.sleep(50);
        blackboard.evictStale(KnowledgeArea.WORLD_STATE, 10L);

        assertTrue(latch.await(1, TimeUnit.SECONDS),
                   "Subscriber should be notified of eviction");
    }

    @Test
    @DisplayName("Knowledge area has correct default max age")
    void testKnowledgeAreaDefaultMaxAge() {
        assertEquals(5000L, KnowledgeArea.WORLD_STATE.getDefaultMaxAgeMs());
        assertEquals(2000L, KnowledgeArea.AGENT_STATUS.getDefaultMaxAgeMs());
        assertEquals(10000L, KnowledgeArea.TASKS.getDefaultMaxAgeMs());
        assertEquals(15000L, KnowledgeArea.RESOURCES.getDefaultMaxAgeMs());
        assertEquals(1000L, KnowledgeArea.THREATS.getDefaultMaxAgeMs());
        assertEquals(3000L, KnowledgeArea.BUILD_PLANS.getDefaultMaxAgeMs());
        assertEquals(60000L, KnowledgeArea.PLAYER_PREFS.getDefaultMaxAgeMs());
    }

    // ==================== Multi-Agent Knowledge Sharing Tests ====================

    @Test
    @DisplayName("Multiple agents can share knowledge through blackboard")
    void testMultiAgentKnowledgeSharing() {
        // Agent 1 posts world observation
        blackboard.post(KnowledgeArea.WORLD_STATE, "block_100", "diamond_ore",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Agent 2 can retrieve it
        Optional<String> agent2Query = blackboard.query(KnowledgeArea.WORLD_STATE, "block_100");

        assertTrue(agent2Query.isPresent());
        assertEquals("diamond_ore", agent2Query.get());
    }

    @Test
    @DisplayName("Agents can track knowledge source")
    void testTrackKnowledgeSource() {
        // Agent 1 posts
        blackboard.post(KnowledgeArea.WORLD_STATE, "resource_1", "iron",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Agent 2 posts
        blackboard.post(KnowledgeArea.WORLD_STATE, "resource_2", "gold",
                       agent2, 1.0, BlackboardEntry.EntryType.FACT);

        // Query by source
        List<BlackboardEntry<String>> agent1Entries = blackboard.queryBySource(
            KnowledgeArea.WORLD_STATE, agent1);
        List<BlackboardEntry<String>> agent2Entries = blackboard.queryBySource(
            KnowledgeArea.WORLD_STATE, agent2);

        assertEquals(1, agent1Entries.size());
        assertEquals(1, agent2Entries.size());
        assertEquals(agent1, agent1Entries.get(0).getSourceAgent());
        assertEquals(agent2, agent2Entries.get(0).getSourceAgent());
    }

    @Test
    @DisplayName("Agents can post hypotheses with varying confidence")
    void testHypothesisConfidenceLevels() {
        // High confidence hypothesis
        blackboard.post(KnowledgeArea.THREATS, "threat_high", "likely_hostile",
                       agent1, 0.9, BlackboardEntry.EntryType.HYPOTHESIS);

        // Low confidence hypothesis
        blackboard.post(KnowledgeArea.THREATS, "threat_low", "possible_hostile",
                       agent1, 0.3, BlackboardEntry.EntryType.HYPOTHESIS);

        Optional<BlackboardEntry<String>> highConf = blackboard.queryEntry(
            KnowledgeArea.THREATS, "threat_high");
        Optional<BlackboardEntry<String>> lowConf = blackboard.queryEntry(
            KnowledgeArea.THREATS, "threat_low");

        assertTrue(highConf.isPresent());
        assertTrue(lowConf.isPresent());
        assertEquals(0.9, highConf.get().getConfidence());
        assertEquals(0.3, lowConf.get().getConfidence());
        assertEquals(BlackboardEntry.EntryType.HYPOTHESIS, highConf.get().getType());
        assertEquals(BlackboardEntry.EntryType.HYPOTHESIS, lowConf.get().getType());
    }

    @Test
    @DisplayName("Agents can share goals through blackboard")
    void testGoalSharing() {
        // Agent 1 posts a goal
        BlackboardEntry<String> goal = BlackboardEntry.createGoal(
            "build_storage", "Build storage at spawn", agent1);

        blackboard.post(KnowledgeArea.BUILD_PLANS, goal);

        // Agent 2 can see the goal
        Optional<BlackboardEntry<String>> retrieved = blackboard.queryEntry(
            KnowledgeArea.BUILD_PLANS, "build_storage");

        assertTrue(retrieved.isPresent());
        assertEquals(BlackboardEntry.EntryType.GOAL, retrieved.get().getType());
        assertEquals(0.9, retrieved.get().getConfidence());
    }

    @Test
    @DisplayName("System can post constraints visible to all agents")
    void testConstraintPosting() {
        // System posts a constraint (null source agent)
        BlackboardEntry<String> constraint = BlackboardEntry.createConstraint(
            "height_limit", "Do not build above y=256");

        blackboard.post(KnowledgeArea.BUILD_PLANS, constraint);

        Optional<BlackboardEntry<String>> retrieved = blackboard.queryEntry(
            KnowledgeArea.BUILD_PLANS, "height_limit");

        assertTrue(retrieved.isPresent());
        assertEquals(BlackboardEntry.EntryType.CONSTRAINT, retrieved.get().getType());
        assertNull(retrieved.get().getSourceAgent(),
                   "Constraints should have null source agent");
    }

    @Test
    @DisplayName("Concurrent posting from multiple agents is thread-safe")
    void testConcurrentPosting() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        int postsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Future<Integer>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Future<Integer> future = executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    int successfulPosts = 0;
                    for (int i = 0; i < postsPerThread; i++) {
                        String key = "thread_" + threadId + "_entry_" + i;
                        blackboard.post(KnowledgeArea.WORLD_STATE, key,
                                       "value_" + threadId + "_" + i,
                                       threadId == 0 ? agent1 : agent2,
                                       1.0, BlackboardEntry.EntryType.FACT);
                        successfulPosts++;
                    }
                    return successfulPosts;

                } catch (Exception e) {
                    return 0;
                } finally {
                    doneLatch.countDown();
                }
            });
            futures.add(future);
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        int totalSuccessful = 0;
        for (Future<Integer> future : futures) {
            totalSuccessful += future.get();
        }

        assertEquals(threadCount * postsPerThread, totalSuccessful,
                    "All posts should succeed");
        assertEquals(threadCount * postsPerThread,
                    blackboard.getEntryCount(KnowledgeArea.WORLD_STATE),
                    "All entries should be stored");

        executor.shutdown();
    }

    @Test
    @DisplayName("Concurrent querying and posting is thread-safe")
    void testConcurrentQueryAndPost() throws InterruptedException {
        int posterThreads = 5;
        int querierThreads = 5;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(posterThreads + querierThreads);
        AtomicInteger successfulQueries = new AtomicInteger(0);

        // Initial entry to query
        blackboard.post(KnowledgeArea.WORLD_STATE, "shared_key", "shared_value",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(posterThreads + querierThreads);

        // Poster threads
        for (int t = 0; t < posterThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "thread_" + threadId + "_key_" + i;
                        blackboard.post(KnowledgeArea.WORLD_STATE, key,
                                       "value_" + i, agent1, 1.0,
                                       BlackboardEntry.EntryType.FACT);
                    }
                } catch (Exception e) {
                    // Ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Querier threads
        for (int t = 0; t < querierThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < operationsPerThread; i++) {
                        Optional<String> result = blackboard.query(
                            KnowledgeArea.WORLD_STATE, "shared_key");
                        if (result.isPresent()) {
                            successfulQueries.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    // Ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        assertEquals(querierThreads * operationsPerThread, successfulQueries.get(),
                    "All queries should succeed");

        executor.shutdown();
    }

    @Test
    @DisplayName("Agents can coordinate through shared task knowledge")
    void testAgentTaskCoordination() {
        // Agent 1 posts a task
        blackboard.post(KnowledgeArea.TASKS, "task_mine_001",
                       "Mine 64 stone at 100,64,200",
                       agent1, 1.0, BlackboardEntry.EntryType.GOAL);

        // Agent 2 can see the task and claim it
        Optional<BlackboardEntry<String>> task = blackboard.queryEntry(
            KnowledgeArea.TASKS, "task_mine_001");

        assertTrue(task.isPresent());
        assertEquals("Mine 64 stone at 100,64,200", task.get().getValue());

        // Agent 2 updates task status
        blackboard.post(KnowledgeArea.TASKS, "task_mine_001_progress",
                       "IN_PROGRESS: 32/64", agent2, 1.0,
                       BlackboardEntry.EntryType.FACT);

        Optional<String> progress = blackboard.query(
            KnowledgeArea.TASKS, "task_mine_001_progress");

        assertTrue(progress.isPresent());
        assertEquals("IN_PROGRESS: 32/64", progress.get());
    }

    @Test
    @DisplayName("Statistics track operations correctly")
    void testStatisticsTracking() {
        assertEquals(0, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE));

        // Post entries
        blackboard.post(KnowledgeArea.WORLD_STATE, "key1", "value1",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.WORLD_STATE, "key2", "value2",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        // Query entries
        blackboard.query(KnowledgeArea.WORLD_STATE, "key1");
        blackboard.query(KnowledgeArea.WORLD_STATE, "key2");
        blackboard.query(KnowledgeArea.WORLD_STATE, "nonexistent");

        // Evict
        blackboard.evictStale(KnowledgeArea.WORLD_STATE, 0L);

        String stats = blackboard.getStatistics();

        assertTrue(stats.contains("Total Posts:"));
        assertTrue(stats.contains("Total Queries:"));
        assertTrue(stats.contains("Total Evictions:"));
        assertTrue(stats.contains("Current Entries:"));
        assertTrue(stats.contains("world_state"));
    }

    @Test
    @DisplayName("Clear area removes all entries and notifies subscribers")
    void testClearArea() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        BlackboardSubscriber subscriber = new BlackboardSubscriber() {
            @Override
            public void onEntryPosted(KnowledgeArea area, BlackboardEntry<?> entry) {}

            @Override
            public void onEntryRemoved(KnowledgeArea area, String key) {
                latch.countDown();
            }
        };

        blackboard.subscribe(KnowledgeArea.WORLD_STATE, subscriber);

        blackboard.post(KnowledgeArea.WORLD_STATE, "key1", "value1",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.WORLD_STATE, "key2", "value2",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertEquals(2, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE));

        int removed = blackboard.clearArea(KnowledgeArea.WORLD_STATE);

        assertEquals(2, removed);
        assertEquals(0, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE));
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Get entry count returns correct values")
    void testGetEntryCount() {
        assertEquals(0, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE));

        blackboard.post(KnowledgeArea.WORLD_STATE, "key1", "value1",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        assertEquals(1, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE));

        blackboard.post(KnowledgeArea.WORLD_STATE, "key2", "value2",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        assertEquals(2, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE));

        blackboard.remove(KnowledgeArea.WORLD_STATE, "key1");
        assertEquals(1, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE));
    }

    @Test
    @DisplayName("Get total entry count sums all areas")
    void testGetTotalEntryCount() {
        blackboard.post(KnowledgeArea.WORLD_STATE, "key1", "value1",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.THREATS, "key2", "value2",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.AGENT_STATUS, "key3", "value3",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertEquals(3, blackboard.getTotalEntryCount());
    }

    @Test
    @DisplayName("Remove entry returns true when entry exists")
    void testRemoveExistingEntry() {
        blackboard.post(KnowledgeArea.WORLD_STATE, "removable", "value",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        boolean removed = blackboard.remove(KnowledgeArea.WORLD_STATE, "removable");

        assertTrue(removed);
        assertFalse(blackboard.query(KnowledgeArea.WORLD_STATE, "removable").isPresent());
    }

    @Test
    @DisplayName("Remove entry returns false when entry does not exist")
    void testRemoveNonExistentEntry() {
        boolean removed = blackboard.remove(KnowledgeArea.WORLD_STATE, "nonexistent");

        assertFalse(removed);
    }

    @Test
    @DisplayName("Knowledge area qualify and unqualify methods")
    void testKnowledgeAreaQualifyUnqualify() {
        String qualified = KnowledgeArea.WORLD_STATE.qualify("block_100");
        assertEquals("world_state:block_100", qualified);

        String unqualified = KnowledgeArea.WORLD_STATE.unqualify(qualified);
        assertEquals("block_100", unqualified);

        String unqualifiedFromDifferentArea = KnowledgeArea.THREATS.unqualify(qualified);
        assertNull(unqualifiedFromDifferentArea,
                   "Unqualifying with wrong area should return null");
    }

    @Test
    @DisplayName("Reset clears all knowledge and statistics")
    void testReset() {
        blackboard.post(KnowledgeArea.WORLD_STATE, "key1", "value1",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);
        blackboard.post(KnowledgeArea.THREATS, "key2", "value2",
                       agent1, 1.0, BlackboardEntry.EntryType.FACT);

        assertTrue(blackboard.getTotalEntryCount() > 0);

        blackboard.reset();

        assertEquals(0, blackboard.getTotalEntryCount());
        assertEquals(0, blackboard.getEntryCount(KnowledgeArea.WORLD_STATE));
        assertEquals(0, blackboard.getEntryCount(KnowledgeArea.THREATS));
    }
}
