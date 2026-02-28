package com.minewright.coordination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link TaskBid}.
 *
 * <p>Tests cover task bid functionality including:</p>
 * <ul>
 *   <li>Bid construction and validation</li>
 *   <li>Bid value calculation</li>
 *   <li>Bid comparison and ordering</li>
 *   <li>Capability extraction</li>
 *   <li>Builder pattern</li>
 *   <li>Equality and hashCode</li>
 *   <li>String representation</li>
 *   <li>Edge cases and validation</li>
 * </ul>
 *
 * @see TaskBid
 * @see TaskAnnouncement
 */
@DisplayName("Task Bid Tests")
class TaskBidTest {

    private String announcementId;
    private UUID bidderId;

    @BeforeEach
    void setUp() {
        announcementId = "announce_" + UUID.randomUUID().toString().substring(0, 8);
        bidderId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Constructor and Validation Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor creates valid bid")
        void constructorCreatesValidBid() {
            TaskBid bid = new TaskBid(
                    announcementId,
                    bidderId,
                    0.8,
                    30000,
                    0.9,
                    Map.of("key", "value")
            );

            assertEquals(announcementId, bid.announcementId());
            assertEquals(bidderId, bid.bidderId());
            assertEquals(0.8, bid.score());
            assertEquals(30000, bid.estimatedTime());
            assertEquals(0.9, bid.confidence());
            assertEquals(1, bid.capabilities().size());
        }

        @Test
        @DisplayName("Constructor throws on null announcement ID")
        void constructorThrowsOnNullAnnouncementId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TaskBid(null, bidderId, 0.8, 30000, 0.9, Map.of()),
                    "Should throw for null announcement ID");
        }

        @Test
        @DisplayName("Constructor throws on blank announcement ID")
        void constructorThrowsOnBlankAnnouncementId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TaskBid("", bidderId, 0.8, 30000, 0.9, Map.of()),
                    "Should throw for blank announcement ID");

            assertThrows(IllegalArgumentException.class,
                    () -> new TaskBid("   ", bidderId, 0.8, 30000, 0.9, Map.of()),
                    "Should throw for whitespace announcement ID");
        }

        @Test
        @DisplayName("Constructor throws on null bidder ID")
        void constructorThrowsOnNullBidderId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TaskBid(announcementId, null, 0.8, 30000, 0.9, Map.of()),
                    "Should throw for null bidder ID");
        }

        @Test
        @DisplayName("Constructor throws on invalid score")
        void constructorThrowsOnInvalidScore() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TaskBid(announcementId, bidderId, -0.1, 30000, 0.9, Map.of()),
                    "Should throw for negative score");

            assertThrows(IllegalArgumentException.class,
                    () -> new TaskBid(announcementId, bidderId, 1.1, 30000, 0.9, Map.of()),
                    "Should throw for score > 1.0");
        }

        @Test
        @DisplayName("Constructor allows boundary score values")
        void constructorAllowsBoundaryScoreValues() {
            assertDoesNotThrow(() -> new TaskBid(announcementId, bidderId, 0.0, 30000, 0.9, Map.of()));
            assertDoesNotThrow(() -> new TaskBid(announcementId, bidderId, 1.0, 30000, 0.9, Map.of()));
        }

        @Test
        @DisplayName("Constructor throws on non-positive estimated time")
        void constructorThrowsOnNonPositiveEstimatedTime() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TaskBid(announcementId, bidderId, 0.8, 0, 0.9, Map.of()),
                    "Should throw for zero estimated time");

            assertThrows(IllegalArgumentException.class,
                    () -> new TaskBid(announcementId, bidderId, 0.8, -1000, 0.9, Map.of()),
                    "Should throw for negative estimated time");
        }

        @Test
        @DisplayName("Constructor throws on invalid confidence")
        void constructorThrowsOnInvalidConfidence() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TaskBid(announcementId, bidderId, 0.8, 30000, -0.1, Map.of()),
                    "Should throw for negative confidence");

            assertThrows(IllegalArgumentException.class,
                    () -> new TaskBid(announcementId, bidderId, 0.8, 30000, 1.1, Map.of()),
                    "Should throw for confidence > 1.0");
        }

        @Test
        @DisplayName("Constructor allows boundary confidence values")
        void constructorAllowsBoundaryConfidenceValues() {
            assertDoesNotThrow(() -> new TaskBid(announcementId, bidderId, 0.8, 30000, 0.0, Map.of()));
            assertDoesNotThrow(() -> new TaskBid(announcementId, bidderId, 0.8, 30000, 1.0, Map.of()));
        }

        @Test
        @DisplayName("Constructor uses empty map for null capabilities")
        void constructorUsesEmptyMapForNullCapabilities() {
            TaskBid bid = new TaskBid(
                    announcementId,
                    bidderId,
                    0.8,
                    30000,
                    0.9,
                    null
            );

            assertNotNull(bid.capabilities());
            assertTrue(bid.capabilities().isEmpty());
        }
    }

    @Nested
    @DisplayName("Bid Value Calculation Tests")
    class BidValueTests {

        @Test
        @DisplayName("Calculate bid value uses score, confidence, and time")
        void calculateBidValueUsesScoreConfidenceAndTime() {
            // score=0.8, confidence=0.9, time=30000ms (30s)
            // bid value = (0.8 * 0.9) / 30 = 0.024
            TaskBid bid = new TaskBid(
                    announcementId,
                    bidderId,
                    0.8,
                    30000,
                    0.9,
                    Map.of()
            );

            double expectedValue = (0.8 * 0.9) / 30.0;
            assertEquals(expectedValue, bid.getBidValue(), 0.0001);
        }

        @Test
        @DisplayName("Bid value prevents division by very small times")
        void bidValuePreventsDivisionByVerySmallTimes() {
            // time=500ms (0.5s) - should use 1.0 as minimum time factor
            TaskBid bid = new TaskBid(
                    announcementId,
                    bidderId,
                    0.5,
                    500,
                    1.0,
                    Map.of()
            );

            double expectedValue = (0.5 * 1.0) / 1.0; // Uses 1.0 instead of 0.5
            assertEquals(expectedValue, bid.getBidValue(), 0.0001);
        }

        @Test
        @DisplayName("Higher score produces higher bid value")
        void higherScoreProducesHigherBidValue() {
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.7, 30000, 0.9, Map.of());
            TaskBid bid2 = new TaskBid(announcementId, bidderId, 0.9, 30000, 0.9, Map.of());

            assertTrue(bid2.getBidValue() > bid1.getBidValue(),
                    "Higher score should produce higher bid value");
        }

        @Test
        @DisplayName("Higher confidence produces higher bid value")
        void higherConfidenceProducesHigherBidValue() {
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.7, Map.of());
            TaskBid bid2 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            assertTrue(bid2.getBidValue() > bid1.getBidValue(),
                    "Higher confidence should produce higher bid value");
        }

        @Test
        @DisplayName("Longer estimated time produces lower bid value")
        void longerTimeProducesLowerBidValue() {
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.8, 15000, 0.9, Map.of());
            TaskBid bid2 = new TaskBid(announcementId, bidderId, 0.8, 60000, 0.9, Map.of());

            assertTrue(bid1.getBidValue() > bid2.getBidValue(),
                    "Longer time should produce lower bid value");
        }
    }

    @Nested
    @DisplayName("Capability Extraction Tests")
    class CapabilityExtractionTests {

        private Map<String, Object> capabilities;

        @BeforeEach
        void setUp() {
            capabilities = new HashMap<>();
            capabilities.put("proficiencies", Map.of("mining", 0.9, "building", 0.7));
            capabilities.put("tools", Set.of("pickaxe", "shovel"));
            capabilities.put("distance", 25.5);
            capabilities.put("currentLoad", 0.3);
        }

        @Test
        @DisplayName("Get distance returns correct value")
        void getDistanceReturnsCorrectValue() {
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, capabilities);

            assertEquals(25.5, bid.getDistance(), 0.001);
        }

        @Test
        @DisplayName("Get distance returns -1 when not specified")
        void getDistanceReturnsMinusOneWhenNotSpecified() {
            Map<String, Object> caps = Map.of("tools", Set.of("pickaxe"));
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, caps);

            assertEquals(-1.0, bid.getDistance(), 0.001);
        }

        @Test
        @DisplayName("Get distance handles integer values")
        void getDistanceHandlesIntegerValues() {
            Map<String, Object> caps = Map.of("distance", 50);
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, caps);

            assertEquals(50.0, bid.getDistance(), 0.001);
        }

        @Test
        @DisplayName("Get current load returns correct value")
        void getCurrentLoadReturnsCorrectValue() {
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, capabilities);

            assertEquals(0.3, bid.getCurrentLoad(), 0.001);
        }

        @Test
        @DisplayName("Get current load returns 0 when not specified")
        void getCurrentLoadReturnsZeroWhenNotSpecified() {
            Map<String, Object> caps = Map.of("tools", Set.of("pickaxe"));
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, caps);

            assertEquals(0.0, bid.getCurrentLoad(), 0.001);
        }

        @Test
        @DisplayName("Capabilities map is accessible")
        void capabilitiesMapIsAccessible() {
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, capabilities);

            assertEquals(capabilities, bid.capabilities());
            assertTrue(bid.capabilities().containsKey("proficiencies"));
            assertTrue(bid.capabilities().containsKey("tools"));
        }
    }

    @Nested
    @DisplayName("Bid Comparison Tests")
    class ComparisonTests {

        @Test
        @DisplayName("Bid comparison uses score, time, and confidence ratio")
        void bidComparisonUsesScoreTimeRatio() {
            // Bid 1: score=0.8, time=30s, confidence=0.9 -> value=0.024
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            // Bid 2: score=0.9, time=30s, confidence=0.9 -> value=0.027 (better)
            TaskBid bid2 = new TaskBid(announcementId, bidderId, 0.9, 30000, 0.9, Map.of());

            assertTrue(bid2.compareTo(bid1) < 0,
                    "Bid2 should be ranked higher (lower comparison value = better)");
        }

        @Test
        @DisplayName("Higher score wins when times are equal")
        void higherScoreWinsWhenTimesAreEqual() {
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.7, 30000, 0.9, Map.of());
            TaskBid bid2 = new TaskBid(announcementId, bidderId, 0.9, 30000, 0.9, Map.of());

            assertTrue(bid2.compareTo(bid1) < 0,
                    "Higher score should win");
            assertTrue(bid1.compareTo(bid2) > 0,
                    "Lower score should lose");
        }

        @Test
        @DisplayName("Lower time wins on equal score")
        void lowerTimeWinsOnEqualScore() {
            // Same score and confidence, but bid1 is faster
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.8, 15000, 0.9, Map.of());
            TaskBid bid2 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            assertTrue(bid1.compareTo(bid2) < 0,
                    "Faster bid should win when scores are equal");
        }

        @Test
        @DisplayName("Confidence affects ranking")
        void confidenceAffectsRanking() {
            // Same score and time, but bid2 has higher confidence
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.7, Map.of());
            TaskBid bid2 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            assertTrue(bid2.compareTo(bid1) < 0,
                    "Higher confidence should win");
        }

        @Test
        @DisplayName("Bidder ID is final tie-breaker")
        void bidderIdIsFinalTieBreaker() {
            UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

            // Identical bids except for bidder ID
            TaskBid bid1 = new TaskBid(announcementId, id1, 0.8, 30000, 0.9, Map.of());
            TaskBid bid2 = new TaskBid(announcementId, id2, 0.8, 30000, 0.9, Map.of());

            assertTrue(bid1.compareTo(bid2) < 0,
                    "Lower bidder ID should win in final tie-breaker");
        }

        @Test
        @DisplayName("Comparison with null returns positive")
        void comparisonWithNullReturnsPositive() {
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            assertTrue(bid.compareTo(null) > 0,
                    "Comparison with null should return positive");
        }

        @Test
        @DisplayName("Bids are equal when all values match")
        void bidsAreEqualWhenAllValuesMatch() {
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());
            TaskBid bid2 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            assertEquals(0, bid1.compareTo(bid2),
                    "Equal bids should have comparison of 0");
            assertEquals(0, bid2.compareTo(bid1),
                    "Equal bids should have comparison of 0");
        }

        @Test
        @DisplayName("Complex comparison scenario")
        void complexComparisonScenario() {
            UUID agent1 = UUID.randomUUID();
            UUID agent2 = UUID.randomUUID();
            UUID agent3 = UUID.randomUUID();

            // Agent 1: High score but slow
            TaskBid bid1 = new TaskBid(announcementId, agent1, 0.95, 60000, 0.9, Map.of());
            // Value = (0.95 * 0.9) / 60 = 0.01425

            // Agent 2: Medium score, medium time
            TaskBid bid2 = new TaskBid(announcementId, agent2, 0.8, 30000, 0.95, Map.of());
            // Value = (0.8 * 0.95) / 30 = 0.0253

            // Agent 3: Lower score but fast
            TaskBid bid3 = new TaskBid(announcementId, agent3, 0.7, 10000, 0.9, Map.of());
            // Value = (0.7 * 0.9) / 10 = 0.063

            // bid3 should win (highest value)
            assertTrue(bid3.compareTo(bid1) < 0);
            assertTrue(bid3.compareTo(bid2) < 0);
            assertTrue(bid2.compareTo(bid1) < 0);
        }
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderTests {

        @Test
        @DisplayName("Builder creates valid bid")
        void builderCreatesValidBid() {
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .score(0.8)
                    .estimatedTime(30000)
                    .confidence(0.9)
                    .build();

            assertEquals(announcementId, bid.announcementId());
            assertEquals(bidderId, bid.bidderId());
            assertEquals(0.8, bid.score());
            assertEquals(30000, bid.estimatedTime());
            assertEquals(0.9, bid.confidence());
        }

        @Test
        @DisplayName("Builder uses default values")
        void builderUsesDefaultValues() {
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .build();

            assertEquals(0.5, bid.score(), "Default score should be 0.5");
            assertEquals(60000, bid.estimatedTime(), "Default time should be 60000ms");
            assertEquals(0.5, bid.confidence(), "Default confidence should be 0.5");
        }

        @Test
        @DisplayName("Builder with estimated seconds")
        void builderWithEstimatedSeconds() {
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .estimatedSeconds(30.0)
                    .build();

            assertEquals(30000, bid.estimatedTime());
        }

        @Test
        @DisplayName("Builder throws on invalid score")
        void builderThrowsOnInvalidScore() {
            TaskBid.Builder builder = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId);

            assertThrows(IllegalArgumentException.class,
                    () -> builder.score(-0.1),
                    "Should throw for negative score");

            assertThrows(IllegalArgumentException.class,
                    () -> builder.score(1.1),
                    "Should throw for score > 1.0");
        }

        @Test
        @DisplayName("Builder throws on invalid confidence")
        void builderThrowsOnInvalidConfidence() {
            TaskBid.Builder builder = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId);

            assertThrows(IllegalArgumentException.class,
                    () -> builder.confidence(-0.1),
                    "Should throw for negative confidence");

            assertThrows(IllegalArgumentException.class,
                    () -> builder.confidence(1.1),
                    "Should throw for confidence > 1.0");
        }

        @Test
        @DisplayName("Builder throws on invalid estimated time")
        void builderThrowsOnInvalidEstimatedTime() {
            TaskBid.Builder builder = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId);

            assertThrows(IllegalArgumentException.class,
                    () -> builder.estimatedTime(0),
                    "Should throw for zero estimated time");

            assertThrows(IllegalArgumentException.class,
                    () -> builder.estimatedTime(-1000),
                    "Should throw for negative estimated time");
        }

        @Test
        @DisplayName("Builder throws on missing announcement ID")
        void builderThrowsOnMissingAnnouncementId() {
            assertThrows(IllegalStateException.class,
                    () -> TaskBid.builder()
                            .bidderId(bidderId)
                            .build(),
                    "Should throw when announcement ID is missing");
        }

        @Test
        @DisplayName("Builder throws on missing bidder ID")
        void builderThrowsOnMissingBidderId() {
            assertThrows(IllegalStateException.class,
                    () -> TaskBid.builder()
                            .announcementId(announcementId)
                            .build(),
                    "Should throw when bidder ID is missing");
        }

        @Test
        @DisplayName("Builder with capabilities")
        void builderWithCapabilities() {
            Map<String, Double> proficiencies = Map.of("mining", 0.9, "building", 0.7);
            Set<String> tools = Set.of("pickaxe", "shovel");

            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .proficiencies(proficiencies)
                    .tools(tools)
                    .distance(25.5)
                    .currentLoad(0.3)
                    .build();

            assertTrue(bid.capabilities().containsKey("proficiencies"));
            assertTrue(bid.capabilities().containsKey("tools"));
            assertEquals(25.5, bid.getDistance());
            assertEquals(0.3, bid.getCurrentLoad());
        }

        @Test
        @DisplayName("Builder with individual capabilities")
        void builderWithIndividualCapabilities() {
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .capability("customKey", "customValue")
                    .build();

            assertEquals("customValue", bid.capabilities().get("customKey"));
        }

        @Test
        @DisplayName("Builder with capabilities map")
        void builderWithCapabilitiesMap() {
            Map<String, Object> caps = new HashMap<>();
            caps.put("key1", "value1");
            caps.put("key2", 42);

            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .capabilities(caps)
                    .capability("key3", "value3")
                    .build();

            assertEquals("value1", bid.capabilities().get("key1"));
            assertEquals(42, bid.capabilities().get("key2"));
            assertEquals("value3", bid.capabilities().get("key3"));
        }

        @Test
        @DisplayName("Builder methods can be chained")
        void builderMethodsCanBeChained() {
            TaskBid bid = TaskBid.builder()
                    .announcementId(announcementId)
                    .bidderId(bidderId)
                    .score(0.9)
                    .confidence(0.95)
                    .estimatedSeconds(45.0)
                    .distance(15.0)
                    .currentLoad(0.2)
                    .build();

            assertEquals(0.9, bid.score());
            assertEquals(0.95, bid.confidence());
            assertEquals(45000, bid.estimatedTime());
            assertEquals(15.0, bid.getDistance());
            assertEquals(0.2, bid.getCurrentLoad());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualityTests {

        @Test
        @DisplayName("Bids with same announcement and bidder are equal")
        void bidsWithSameAnnouncementAndBidderAreEqual() {
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());
            TaskBid bid2 = new TaskBid(announcementId, bidderId, 0.5, 10000, 0.7, Map.of());

            assertEquals(bid1, bid2,
                    "Bids with same announcement ID and bidder ID should be equal");
            assertEquals(bid1.hashCode(), bid2.hashCode(),
                    "Equal bids should have same hashCode");
        }

        @Test
        @DisplayName("Bids with different announcement IDs are not equal")
        void bidsWithDifferentAnnouncementIdsAreNotEqual() {
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());
            TaskBid bid2 = new TaskBid("different_announcement", bidderId, 0.8, 30000, 0.9, Map.of());

            assertNotEquals(bid1, bid2);
        }

        @Test
        @DisplayName("Bids with different bidder IDs are not equal")
        void bidsWithDifferentBidderIdsAreNotEqual() {
            UUID otherId = UUID.randomUUID();
            TaskBid bid1 = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());
            TaskBid bid2 = new TaskBid(announcementId, otherId, 0.8, 30000, 0.9, Map.of());

            assertNotEquals(bid1, bid2);
        }

        @Test
        @DisplayName("Bid equals same instance")
        void bidEqualsSameInstance() {
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            assertEquals(bid, bid);
        }

        @Test
        @DisplayName("Bid not equal to null")
        void bidNotEqualToNull() {
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            assertNotEquals(bid, null);
        }

        @Test
        @DisplayName("Bid not equal to different type")
        void bidNotEqualToDifferentType() {
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            assertNotEquals(bid, "not a bid");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("ToString contains bid information")
        void toStringContainsBidInformation() {
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            String str = bid.toString();

            assertTrue(str.contains("TaskBid"));
            assertTrue(str.contains(announcementId.substring(0, 8)));
            assertTrue(str.contains(bidderId.toString().substring(0, 8)));
            assertTrue(str.contains("0.80"));
            assertTrue(str.contains("30000"));
            assertTrue(str.contains("0.90"));
        }

        @Test
        @DisplayName("ToString contains calculated bid value")
        void toStringContainsCalculatedBidValue() {
            TaskBid bid = new TaskBid(announcementId, bidderId, 0.8, 30000, 0.9, Map.of());

            String str = bid.toString();

            assertTrue(str.contains("value="),
                    "toString should contain calculated value");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Special Scenarios")
    class EdgeCaseTests {

        @Test
        @DisplayName("Bid with minimum values")
        void bidWithMinimumValues() {
            TaskBid bid = new TaskBid(
                    announcementId,
                    bidderId,
                    0.0,
                    1,
                    0.0,
                    Map.of()
            );

            assertEquals(0.0, bid.score());
            assertEquals(1, bid.estimatedTime());
            assertEquals(0.0, bid.confidence());
            assertEquals(0.0, bid.getBidValue());
        }

        @Test
        @DisplayName("Bid with maximum values")
        void bidWithMaximumValues() {
            TaskBid bid = new TaskBid(
                    announcementId,
                    bidderId,
                    1.0,
                    Long.MAX_VALUE,
                    1.0,
                    Map.of()
            );

            assertEquals(1.0, bid.score());
            assertEquals(Long.MAX_VALUE, bid.estimatedTime());
            assertEquals(1.0, bid.confidence());
            assertTrue(bid.getBidValue() > 0.0);
            assertTrue(bid.getBidValue() < 1.0);
        }

        @Test
        @DisplayName("Bid with very large time")
        void bidWithVeryLargeTime() {
            TaskBid bid = new TaskBid(
                    announcementId,
                    bidderId,
                    0.8,
                    3600000, // 1 hour
                    0.9,
                    Map.of()
            );

            double expectedValue = (0.8 * 0.9) / 3600.0;
            assertEquals(expectedValue, bid.getBidValue(), 0.0001);
        }

        @Test
        @DisplayName("Bid with very small time (under 1 second)")
        void bidWithVerySmallTime() {
            TaskBid bid = new TaskBid(
                    announcementId,
                    bidderId,
                    0.8,
                    100, // 0.1 seconds
                    0.9,
                    Map.of()
            );

            // Should use 1.0 as minimum time factor
            double expectedValue = (0.8 * 0.9) / 1.0;
            assertEquals(expectedValue, bid.getBidValue(), 0.0001);
        }

        @Test
        @DisplayName("Bid with empty capabilities")
        void bidWithEmptyCapabilities() {
            TaskBid bid = new TaskBid(
                    announcementId,
                    bidderId,
                    0.8,
                    30000,
                    0.9,
                    Map.of()
            );

            assertTrue(bid.capabilities().isEmpty());
            assertEquals(-1.0, bid.getDistance());
            assertEquals(0.0, bid.getCurrentLoad());
        }

        @Test
        @DisplayName("Bid with complex capabilities")
        void bidWithComplexCapabilities() {
            Map<String, Object> caps = new HashMap<>();
            caps.put("proficiencies", Map.of("mining", 0.9, "building", 0.7, "combat", 0.5));
            caps.put("tools", Set.of("pickaxe", "shovel", "axe", "sword"));
            caps.put("distance", 123.45);
            caps.put("currentLoad", 0.75);
            caps.put("customField", "customValue");
            caps.put("numericField", 42);

            TaskBid bid = new TaskBid(
                    announcementId,
                    bidderId,
                    0.8,
                    30000,
                    0.9,
                    caps
            );

            assertEquals(6, bid.capabilities().size());
            assertEquals(123.45, bid.getDistance());
            assertEquals(0.75, bid.getCurrentLoad());
        }

        @Test
        @DisplayName("Multiple bids can be sorted")
        void multipleBidsCanBeSorted() {
            List<TaskBid> bids = List.of(
                    new TaskBid(announcementId, UUID.randomUUID(), 0.7, 40000, 0.8, Map.of()),
                    new TaskBid(announcementId, UUID.randomUUID(), 0.9, 25000, 0.95, Map.of()),
                    new TaskBid(announcementId, UUID.randomUUID(), 0.6, 50000, 0.7, Map.of()),
                    new TaskBid(announcementId, UUID.randomUUID(), 0.8, 30000, 0.9, Map.of())
            );

            List<TaskBid> sorted = bids.stream()
                    .sorted()
                    .toList();

            // Verify they are sorted by bid value (highest first)
            for (int i = 0; i < sorted.size() - 1; i++) {
                assertTrue(sorted.get(i).getBidValue() >= sorted.get(i + 1).getBidValue(),
                        "List should be sorted by bid value");
            }
        }
    }
}
