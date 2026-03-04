package com.minewright.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RelationshipTracker}.
 *
 * Tests cover:
 * <ul>
 *   <li>Relationship initialization and first meeting</li>
 *   <li>Rapport level adjustments with boundary conditions</li>
 *   <li>Trust level management</li>
 *   <li>Inside joke tracking and retrieval</li>
 *   <li>Player preferences management</li>
 *   <li>Playstyle metrics recording</li>
 *   <li>Session topics tracking</li>
 *   <li>Milestone detection and celebration</li>
 *   <li>Relationship context generation</li>
 *   <li>Shared success/failure recording</li>
 *   <li>Interaction count tracking</li>
 *   <li>Relationship object creation with personality</li>
 * </ul>
 *
 * @see RelationshipTracker
 * @since 1.4.0
 */
@DisplayName("RelationshipTracker Tests")
class RelationshipTrackerTest {

    private RelationshipTracker relationshipTracker;
    private MemoryStore memoryStore;
    private PersonalitySystem.PersonalityProfile personalityProfile;

    @BeforeEach
    void setUp() {
        relationshipTracker = new RelationshipTracker();
        memoryStore = new MemoryStore();
        personalityProfile = new PersonalitySystem().getPersonality();
    }

    // ==================== Relationship Initialization Tests ====================

    @Test
    @DisplayName("RelationshipTracker creates with default values")
    void testRelationshipTrackerCreation() {
        assertNotNull(relationshipTracker);
        assertEquals(10, relationshipTracker.getRapportLevel(), "Default rapport should be 10");
        assertEquals(5, relationshipTracker.getTrustLevel(), "Default trust should be 5");
        assertEquals(0, relationshipTracker.getInteractionCount(), "Initial interaction count should be 0");
        assertNull(relationshipTracker.getPlayerName(), "Player name should be null initially");
        assertNull(relationshipTracker.getFirstMeeting(), "First meeting should be null initially");
    }

    @Test
    @DisplayName("Initialize relationship sets player name and first meeting")
    void testInitializeRelationship() {
        relationshipTracker.initializeRelationship("Steve", memoryStore);

        assertEquals("Steve", relationshipTracker.getPlayerName());
        assertNotNull(relationshipTracker.getFirstMeeting());

        // Check that player name was learned as a fact
        CompanionMemory.SemanticMemory nameFact = memoryStore.getSemanticMemories().get("identity:name");
        assertNotNull(nameFact, "Player name should be stored as semantic memory");
        assertEquals("Steve", nameFact.value);

        // Check that first meeting memory was created
        List<CompanionMemory.EpisodicMemory> recentMemories = memoryStore.getRecentMemories(1);
        assertFalse(recentMemories.isEmpty(), "First meeting memory should be created");
        assertEquals("first_meeting", recentMemories.get(0).eventType);
        assertTrue(recentMemories.get(0).isMilestone(), "First meeting should be marked as milestone");
    }

    @Test
    @DisplayName("Initialize relationship only sets values once")
    void testInitializeRelationshipIdempotent() {
        relationshipTracker.initializeRelationship("Steve", memoryStore);
        Instant firstMeetingTime = relationshipTracker.getFirstMeeting();

        // Try to initialize again
        relationshipTracker.initializeRelationship("Alex", memoryStore);

        assertEquals("Steve", relationshipTracker.getPlayerName(), "Player name should not change");
        assertEquals(firstMeetingTime, relationshipTracker.getFirstMeeting(), "First meeting time should not change");
    }

    // ==================== Rapport Level Tests ====================

    @Test
    @DisplayName("Adjust rapport with positive delta increases level")
    void testAdjustRapportPositive() {
        int initialRapport = relationshipTracker.getRapportLevel();
        relationshipTracker.adjustRapport(10);

        assertEquals(initialRapport + 10, relationshipTracker.getRapportLevel());
    }

    @Test
    @DisplayName("Adjust rapport with negative delta decreases level")
    void testAdjustRapportNegative() {
        relationshipTracker.adjustRapport(20); // Start higher
        int initialRapport = relationshipTracker.getRapportLevel();
        relationshipTracker.adjustRapport(-5);

        assertEquals(initialRapport - 5, relationshipTracker.getRapportLevel());
    }

    @Test
    @DisplayName("Rapport level clamps to minimum 0")
    void testRapportClampsToMinimum() {
        relationshipTracker.adjustRapport(-100);

        assertEquals(0, relationshipTracker.getRapportLevel(), "Rapport should be clamped to 0");
    }

    @Test
    @DisplayName("Rapport level clamps to maximum 100")
    void testRapportClampsToMaximum() {
        relationshipTracker.adjustRapport(200);

        assertEquals(100, relationshipTracker.getRapportLevel(), "Rapport should be clamped to 100");
    }

    @Test
    @DisplayName("Rapport level at boundary 0 stays at 0 when decreased")
    void testRapportAtBoundaryZero() {
        relationshipTracker.adjustRapport(-10);
        assertEquals(0, relationshipTracker.getRapportLevel());

        relationshipTracker.adjustRapport(-5);
        assertEquals(0, relationshipTracker.getRapportLevel(), "Should stay at 0");
    }

    @Test
    @DisplayName("Rapport level at boundary 100 stays at 100 when increased")
    void testRapportAtBoundaryHundred() {
        relationshipTracker.adjustRapport(100);
        assertEquals(100, relationshipTracker.getRapportLevel());

        relationshipTracker.adjustRapport(10);
        assertEquals(100, relationshipTracker.getRapportLevel(), "Should stay at 100");
    }

    @Test
    @DisplayName("Rapport level with zero delta remains unchanged")
    void testRapportZeroDelta() {
        int initialRapport = relationshipTracker.getRapportLevel();
        relationshipTracker.adjustRapport(0);

        assertEquals(initialRapport, relationshipTracker.getRapportLevel());
    }

    // ==================== Trust Level Tests ====================

    @Test
    @DisplayName("Adjust trust with positive delta increases level")
    void testAdjustTrustPositive() {
        int initialTrust = relationshipTracker.getTrustLevel();
        relationshipTracker.adjustTrust(15);

        assertEquals(initialTrust + 15, relationshipTracker.getTrustLevel());
    }

    @Test
    @DisplayName("Adjust trust with negative delta decreases level")
    void testAdjustTrustNegative() {
        relationshipTracker.adjustTrust(20); // Start higher
        int initialTrust = relationshipTracker.getTrustLevel();
        relationshipTracker.adjustTrust(-8);

        assertEquals(initialTrust - 8, relationshipTracker.getTrustLevel());
    }

    @Test
    @DisplayName("Trust level clamps to minimum 0")
    void testTrustClampsToMinimum() {
        relationshipTracker.adjustTrust(-200);

        assertEquals(0, relationshipTracker.getTrustLevel(), "Trust should be clamped to 0");
    }

    @Test
    @DisplayName("Trust level clamps to maximum 100")
    void testTrustClampsToMaximum() {
        relationshipTracker.adjustTrust(300);

        assertEquals(100, relationshipTracker.getTrustLevel(), "Trust should be clamped to 100");
    }

    @Test
    @DisplayName("Trust at boundary 0 cannot go negative")
    void testTrustAtBoundaryZero() {
        relationshipTracker.adjustTrust(-10);
        assertEquals(0, relationshipTracker.getTrustLevel());

        relationshipTracker.adjustTrust(-1);
        assertEquals(0, relationshipTracker.getTrustLevel());
    }

    @Test
    @DisplayName("Trust at boundary 100 cannot exceed maximum")
    void testTrustAtBoundaryHundred() {
        relationshipTracker.adjustTrust(100);
        assertEquals(100, relationshipTracker.getTrustLevel());

        relationshipTracker.adjustTrust(1);
        assertEquals(100, relationshipTracker.getTrustLevel());
    }

    // ==================== Shared Success/Failure Tests ====================

    @Test
    @DisplayName("Record shared success increases rapport and trust")
    void testRecordSharedSuccess() {
        int initialRapport = relationshipTracker.getRapportLevel();
        int initialTrust = relationshipTracker.getTrustLevel();

        relationshipTracker.recordSharedSuccess("Built a house together", memoryStore);

        assertEquals(initialRapport + 2, relationshipTracker.getRapportLevel(), "Rapport should increase by 2");
        assertEquals(initialTrust + 3, relationshipTracker.getTrustLevel(), "Trust should increase by 3");

        // Check that experience was recorded
        List<CompanionMemory.EpisodicMemory> recentMemories = memoryStore.getRecentMemories(1);
        assertFalse(recentMemories.isEmpty());
        assertEquals("success", recentMemories.get(0).eventType);
    }

    @Test
    @DisplayName("Record shared failure does not decrease rapport")
    void testRecordSharedFailure() {
        relationshipTracker.adjustRapport(50);
        int initialRapport = relationshipTracker.getRapportLevel();

        relationshipTracker.recordSharedFailure("Tried to build a tower", "It fell over", memoryStore);

        assertEquals(initialRapport, relationshipTracker.getRapportLevel(), "Rapport should not change");

        // Check that failure was recorded
        List<CompanionMemory.EpisodicMemory> recentMemories = memoryStore.getRecentMemories(1);
        assertFalse(recentMemories.isEmpty());
        assertEquals("failure", recentMemories.get(0).eventType);
    }

    @Test
    @DisplayName("Multiple shared successes build rapport progressively")
    void testMultipleSharedSuccesses() {
        int initialRapport = relationshipTracker.getRapportLevel();

        for (int i = 0; i < 5; i++) {
            relationshipTracker.recordSharedSuccess("Task " + i, memoryStore);
        }

        assertEquals(initialRapport + 10, relationshipTracker.getRapportLevel(), "Rapport should increase by 10 (5 * 2)");
    }

    // ==================== Inside Joke Tests ====================

    @Test
    @DisplayName("Record inside joke increases rapport")
    void testRecordInsideJokeIncreasesRapport() {
        int initialRapport = relationshipTracker.getRapportLevel();

        relationshipTracker.recordInsideJoke("Falling off a cliff", "Gravity check failed!");

        assertEquals(initialRapport + 3, relationshipTracker.getRapportLevel(), "Inside joke should increase rapport by 3");
    }

    @Test
    @DisplayName("Record inside joke stores joke in conversational memory")
    void testRecordInsideJokeStored() {
        relationshipTracker.recordInsideJoke("Mining diamonds", "We found the shiny rocks!");

        assertEquals(1, relationshipTracker.getInsideJokeCount(), "Should have 1 inside joke");

        CompanionMemory.InsideJoke joke = relationshipTracker.getRandomInsideJoke();
        assertNotNull(joke);
        assertEquals("Mining diamonds", joke.context);
        assertEquals("We found the shiny rocks!", joke.punchline);
    }

    @Test
    @DisplayName("Record multiple inside jokes")
    void testRecordMultipleInsideJokes() {
        relationshipTracker.recordInsideJoke("Context 1", "Joke 1");
        relationshipTracker.recordInsideJoke("Context 2", "Joke 2");
        relationshipTracker.recordInsideJoke("Context 3", "Joke 3");

        assertEquals(3, relationshipTracker.getInsideJokeCount());
    }

    @Test
    @DisplayName("Get random inside joke returns null when none exist")
    void testGetRandomInsideJokeNoneExist() {
        CompanionMemory.InsideJoke joke = relationshipTracker.getRandomInsideJoke();

        assertNull(joke, "Should return null when no jokes exist");
    }

    @Test
    @DisplayName("Get random inside joke retrieves stored jokes")
    void testGetRandomInsideJokeRetrievesJoke() {
        relationshipTracker.recordInsideJoke("Test context", "Test punchline");

        CompanionMemory.InsideJoke joke = relationshipTracker.getRandomInsideJoke();

        assertNotNull(joke);
        assertEquals("Test context", joke.context);
        assertEquals("Test punchline", joke.punchline);
    }

    // ==================== Player Preferences Tests ====================

    @Test
    @DisplayName("Player preferences map is initially empty")
    void testPlayerPreferencesInitiallyEmpty() {
        Map<String, Object> preferences = relationshipTracker.getPlayerPreferences();

        assertNotNull(preferences);
        assertTrue(preferences.isEmpty(), "Preferences should be empty initially");
    }

    @Test
    @DisplayName("Can add player preferences through the map")
    void testAddPlayerPreferences() {
        Map<String, Object> preferences = relationshipTracker.getPlayerPreferences();

        preferences.put("favorite_block", "oak_planks");
        preferences.put("mining_style", "branch");

        assertEquals(2, preferences.size());
        assertEquals("oak_planks", preferences.get("favorite_block"));
        assertEquals("branch", preferences.get("mining_style"));
    }

    @Test
    @DisplayName("Player preferences map is modifiable")
    void testPlayerPreferencesModifiable() {
        Map<String, Object> preferences = relationshipTracker.getPlayerPreferences();

        preferences.put("test_key", "test_value");
        assertEquals("test_value", preferences.get("test_key"));

        preferences.remove("test_key");
        assertFalse(preferences.containsKey("test_key"));
    }

    // ==================== Playstyle Metrics Tests ====================

    @Test
    @DisplayName("Playstyle metrics map is initially empty")
    void testPlaystyleMetricsInitiallyEmpty() {
        Map<String, Integer> metrics = relationshipTracker.getPlaystyleMetrics();

        assertNotNull(metrics);
        assertTrue(metrics.isEmpty(), "Playstyle metrics should be empty initially");
    }

    @Test
    @DisplayName("Record playstyle metric adds new metric")
    void testRecordPlaystyleMetric() {
        relationshipTracker.recordPlaystyleMetric("cautiousness", 5);

        Map<String, Integer> metrics = relationshipTracker.getPlaystyleMetrics();
        assertEquals(1, metrics.size());
        assertEquals(5, metrics.get("cautiousness"));
    }

    @Test
    @DisplayName("Record playstyle metric merges with existing metric")
    void testRecordPlaystyleMetricMerges() {
        relationshipTracker.recordPlaystyleMetric("aggression", 3);
        relationshipTracker.recordPlaystyleMetric("aggression", 2);

        Map<String, Integer> metrics = relationshipTracker.getPlaystyleMetrics();
        assertEquals(5, metrics.get("aggression"), "Metrics should merge by summing values");
    }

    @Test
    @DisplayName("Record playstyle metric with negative delta")
    void testRecordPlaystyleMetricNegative() {
        relationshipTracker.recordPlaystyleMetric("risk_taking", 10);
        relationshipTracker.recordPlaystyleMetric("risk_taking", -3);

        Map<String, Integer> metrics = relationshipTracker.getPlaystyleMetrics();
        assertEquals(7, metrics.get("risk_taking"));
    }

    @Test
    @DisplayName("Record multiple different playstyle metrics")
    void testRecordMultiplePlaystyleMetrics() {
        relationshipTracker.recordPlaystyleMetric("exploration", 8);
        relationshipTracker.recordPlaystyleMetric("building", 6);
        relationshipTracker.recordPlaystyleMetric("combat", 4);

        Map<String, Integer> metrics = relationshipTracker.getPlaystyleMetrics();
        assertEquals(3, metrics.size());
    }

    // ==================== Session Topics Tests ====================

    @Test
    @DisplayName("Session topics set is initially empty")
    void testSessionTopicsInitiallyEmpty() {
        Set<String> topics = relationshipTracker.getSessionTopics();

        assertNotNull(topics);
        assertTrue(topics.isEmpty(), "Session topics should be empty initially");
    }

    @Test
    @DisplayName("Get session topics returns unmodifiable set")
    void testGetSessionTopicsReturnsUnmodifiable() {
        Set<String> topics = relationshipTracker.getSessionTopics();

        assertThrows(UnsupportedOperationException.class, () -> {
            topics.add("new_topic");
        }, "Returned set should be unmodifiable");
    }

    // ==================== Interaction Count Tests ====================

    @Test
    @DisplayName("Initial interaction count is zero")
    void testInitialInteractionCount() {
        assertEquals(0, relationshipTracker.getInteractionCount());
    }

    @Test
    @DisplayName("Increment interaction count increases count")
    void testIncrementInteractionCount() {
        relationshipTracker.incrementInteractionCount();
        assertEquals(1, relationshipTracker.getInteractionCount());

        relationshipTracker.incrementInteractionCount();
        assertEquals(2, relationshipTracker.getInteractionCount());
    }

    @Test
    @DisplayName("Multiple increments increase interaction count")
    void testMultipleIncrements() {
        for (int i = 0; i < 10; i++) {
            relationshipTracker.incrementInteractionCount();
        }

        assertEquals(10, relationshipTracker.getInteractionCount());
    }

    // ==================== Milestone Detection Tests ====================

    @Test
    @DisplayName("Check auto milestones detects no milestones initially")
    void testCheckAutoMilestonesInitiallyNone() {
        relationshipTracker.checkAutoMilestones();

        List<MilestoneTracker.Milestone> milestones = relationshipTracker.getMilestones();
        assertTrue(milestones.isEmpty(), "No auto milestones should be detected initially");
    }

    @Test
    @DisplayName("Check auto milestones detects getting to know milestone at 10 interactions")
    void testAutoMilestoneGettingToKnow() {
        // Simulate 10 interactions
        for (int i = 0; i < 10; i++) {
            relationshipTracker.incrementInteractionCount();
        }

        relationshipTracker.checkAutoMilestones();

        assertTrue(relationshipTracker.hasMilestone("auto_getting_to_know"), "Should detect getting to know milestone");

        List<MilestoneTracker.Milestone> milestones = relationshipTracker.getMilestones();
        assertEquals(1, milestones.size());
        assertEquals("Getting to Know You", milestones.get(0).title);
    }

    @Test
    @DisplayName("Check auto milestones detects frequent companion at 50 interactions")
    void testAutoMilestoneFrequentCompanion() {
        // Simulate 50 interactions
        for (int i = 0; i < 50; i++) {
            relationshipTracker.incrementInteractionCount();
        }

        relationshipTracker.checkAutoMilestones();

        assertTrue(relationshipTracker.hasMilestone("auto_frequent_companion"), "Should detect frequent companion milestone");
    }

    @Test
    @DisplayName("Check auto milestones detects friends at rapport 50")
    void testAutoMilestoneFriends() {
        relationshipTracker.adjustRapport(50); // Set rapport to 50 (10 initial + 40 adjustment)

        relationshipTracker.checkAutoMilestones();

        assertTrue(relationshipTracker.hasMilestone("auto_friends"), "Should detect friends milestone");

        List<MilestoneTracker.Milestone> milestones = relationshipTracker.getMilestones();
        assertFalse(milestones.isEmpty());
        assertEquals("Friends", milestones.get(0).title);
    }

    @Test
    @DisplayName("Check auto milestones detects best friends at rapport 80")
    void testAutoMilestoneBestFriends() {
        relationshipTracker.adjustRapport(90); // Set rapport above 80

        relationshipTracker.checkAutoMilestones();

        assertTrue(relationshipTracker.hasMilestone("auto_best_friends"), "Should detect best friends milestone");

        List<MilestoneTracker.Milestone> milestones = relationshipTracker.getMilestones();
        assertFalse(milestones.isEmpty());
        assertEquals("Best Friends", milestones.get(0).title);
    }

    @Test
    @DisplayName("Auto milestone only triggers once")
    void testAutoMilestoneOnlyOnce() {
        // Trigger friends milestone
        relationshipTracker.adjustRapport(50);
        relationshipTracker.checkAutoMilestones();

        int rapportAfterFirst = relationshipTracker.getRapportLevel();

        // Check again
        relationshipTracker.checkAutoMilestones();

        assertEquals(rapportAfterFirst, relationshipTracker.getRapportLevel(), "Rapport should not increase again");
    }

    // ==================== Milestone Tracking Tests ====================

    @Test
    @DisplayName("Has milestone returns false for non-existent milestone")
    void testHasMilestoneFalse() {
        assertFalse(relationshipTracker.hasMilestone("non_existent_milestone"));
    }

    @Test
    @DisplayName("Get milestones returns empty list initially")
    void testGetMilestonesInitiallyEmpty() {
        List<MilestoneTracker.Milestone> milestones = relationshipTracker.getMilestones();

        assertNotNull(milestones);
        assertTrue(milestones.isEmpty());
    }

    @Test
    @DisplayName("Get milestone tracker returns non-null tracker")
    void testGetMilestoneTracker() {
        MilestoneTracker tracker = relationshipTracker.getMilestoneTracker();

        assertNotNull(tracker);
    }

    // ==================== Relationship Context Tests ====================

    @Test
    @DisplayName("Get relationship context generates valid context")
    void testGetRelationshipContext() {
        String context = relationshipTracker.getRelationshipContext();

        assertNotNull(context);
        assertFalse(context.isEmpty());
    }

    @Test
    @DisplayName("Get relationship context contains rapport level")
    void testRelationshipContextContainsRapport() {
        relationshipTracker.adjustRapport(45);
        String context = relationshipTracker.getRelationshipContext();

        assertTrue(context.contains("Rapport Level:"));
        assertTrue(context.contains("55"));
    }

    @Test
    @DisplayName("Get relationship context contains trust level")
    void testRelationshipContextContainsTrust() {
        relationshipTracker.adjustTrust(25);
        String context = relationshipTracker.getRelationshipContext();

        assertTrue(context.contains("Trust Level:"));
        assertTrue(context.contains("30"));
    }

    @Test
    @DisplayName("Get relationship context contains interaction count")
    void testRelationshipContextContainsInteractions() {
        relationshipTracker.incrementInteractionCount();
        relationshipTracker.incrementInteractionCount();
        relationshipTracker.incrementInteractionCount();

        String context = relationshipTracker.getRelationshipContext();

        assertTrue(context.contains("Interactions: 3"));
    }

    @Test
    @DisplayName("Get relationship context contains days known when initialized")
    void testRelationshipContextContainsDaysKnown() {
        relationshipTracker.initializeRelationship("TestPlayer", memoryStore);

        String context = relationshipTracker.getRelationshipContext();

        assertTrue(context.contains("Known for:"));
        assertTrue(context.contains("days"));
    }

    @Test
    @DisplayName("Get relationship context contains preferences when set")
    void testRelationshipContextContainsPreferences() {
        relationshipTracker.getPlayerPreferences().put("likes_mining", true);
        relationshipTracker.getPlayerPreferences().put("favorite_biome", "plains");

        String context = relationshipTracker.getRelationshipContext();

        assertTrue(context.contains("Known preferences:"));
        assertTrue(context.contains("likes_mining"));
        assertTrue(context.contains("favorite_biome"));
    }

    @Test
    @DisplayName("Get relationship context contains playstyle when set")
    void testRelationshipContextContainsPlaystyle() {
        relationshipTracker.recordPlaystyleMetric("aggression", 5);
        relationshipTracker.recordPlaystyleMetric("exploration", 8);

        String context = relationshipTracker.getRelationshipContext();

        assertTrue(context.contains("Playstyle observations:"));
    }

    @Test
    @DisplayName("Get relationship context contains inside jokes when recorded")
    void testRelationshipContextContainsInsideJokes() {
        relationshipTracker.recordInsideJoke("Context 1", "Joke 1");
        relationshipTracker.recordInsideJoke("Context 2", "Joke 2");

        String context = relationshipTracker.getRelationshipContext();

        assertTrue(context.contains("Inside jokes shared: 2"));
    }

    // ==================== Relationship Object Tests ====================

    @Test
    @DisplayName("Get relationship creates valid relationship object")
    void testGetRelationship() {
        CompanionMemory.Relationship relationship = relationshipTracker.getRelationship(personalityProfile);

        assertNotNull(relationship);
        assertEquals(10, relationship.getAffection());
        assertEquals(5, relationship.getTrust());
        assertNotNull(relationship.getCurrentMood());
    }

    @Test
    @DisplayName("Get relationship reflects current rapport and trust")
    void testGetRelationshipReflectsCurrentLevels() {
        relationshipTracker.adjustRapport(30);
        relationshipTracker.adjustTrust(15);

        CompanionMemory.Relationship relationship = relationshipTracker.getRelationship(personalityProfile);

        assertEquals(40, relationship.getAffection(), "Rapport should be 40 (10 initial + 30)");
        assertEquals(20, relationship.getTrust(), "Trust should be 20 (5 initial + 15)");
    }

    @Test
    @DisplayName("Get relationship uses personality mood")
    void testGetRelationshipUsesPersonalityMood() {
        personalityProfile.setMood("focused");

        CompanionMemory.Relationship relationship = relationshipTracker.getRelationship(personalityProfile);

        assertEquals(CompanionMemory.Mood.FOCUSED, relationship.getCurrentMood());
    }

    // ==================== Conversational Memory Access Tests ====================

    @Test
    @DisplayName("Get conversational memory returns valid memory")
    void testGetConversationalMemory() {
        CompanionMemory.ConversationalMemory convMemory = relationshipTracker.getConversationalMemory();

        assertNotNull(convMemory);
    }

    @Test
    @DisplayName("Conversational memory tracks discussed topics")
    void testConversationalMemoryTracksTopics() {
        CompanionMemory.ConversationalMemory convMemory = relationshipTracker.getConversationalMemory();

        convMemory.addDiscussedTopic("mining");
        convMemory.addDiscussedTopic("building");

        assertTrue(convMemory.hasDiscussed("mining"));
        assertTrue(convMemory.hasDiscussed("building"));
        assertFalse(convMemory.hasDiscussed("combat"));
    }

    @Test
    @DisplayName("Conversational memory tracks phrase usage")
    void testConversationalMemoryTracksPhraseUsage() {
        CompanionMemory.ConversationalMemory convMemory = relationshipTracker.getConversationalMemory();

        convMemory.recordPhraseUsage("hello there");
        convMemory.recordPhraseUsage("hello there");
        convMemory.recordPhraseUsage("goodbye");

        assertEquals(2, convMemory.getPhraseUsageCount("hello there"));
        assertEquals(1, convMemory.getPhraseUsageCount("goodbye"));
        assertEquals(0, convMemory.getPhraseUsageCount("never said"));
    }

    // ==================== Edge Cases and Boundary Tests ====================

    @Test
    @DisplayName("Record shared success at rapport cap does not exceed 100")
    void testRecordSharedSuccessAtRapportCap() {
        relationshipTracker.adjustRapport(98); // Get close to cap

        relationshipTracker.recordSharedSuccess("Final success", memoryStore);

        assertEquals(100, relationshipTracker.getRapportLevel(), "Should cap at 100");
    }

    @Test
    @DisplayName("Record shared success at trust cap does not exceed 100")
    void testRecordSharedSuccessAtTrustCap() {
        relationshipTracker.adjustTrust(97); // Get close to cap

        relationshipTracker.recordSharedSuccess("Final success", memoryStore);

        assertEquals(100, relationshipTracker.getTrustLevel(), "Should cap at 100");
    }

    @Test
    @DisplayName("Inside joke at rapport cap does not exceed 100")
    void testInsideJokeAtRapportCap() {
        relationshipTracker.adjustRapport(99); // One away from cap

        relationshipTracker.recordInsideJoke("Context", "Joke");

        assertEquals(100, relationshipTracker.getRapportLevel(), "Should cap at 100");
    }

    @Test
    @DisplayName("Multiple milestone detections in sequence")
    void testMultipleMilestoneDetections() {
        // First reach 10 interactions
        for (int i = 0; i < 10; i++) {
            relationshipTracker.incrementInteractionCount();
        }
        relationshipTracker.checkAutoMilestones();

        // Then reach 50 interactions
        for (int i = 0; i < 40; i++) {
            relationshipTracker.incrementInteractionCount();
        }
        relationshipTracker.checkAutoMilestones();

        // Should have both milestones
        assertTrue(relationshipTracker.hasMilestone("auto_getting_to_know"));
        assertTrue(relationshipTracker.hasMilestone("auto_frequent_companion"));

        List<MilestoneTracker.Milestone> milestones = relationshipTracker.getMilestones();
        assertEquals(2, milestones.size());
    }

    @Test
    @DisplayName("Rapport and trust are independent")
    void testRapportAndTrustIndependent() {
        relationshipTracker.adjustRapport(50);
        relationshipTracker.adjustTrust(30);

        assertEquals(60, relationshipTracker.getRapportLevel());
        assertEquals(35, relationshipTracker.getTrustLevel());

        // Decreasing rapport shouldn't affect trust
        relationshipTracker.adjustRapport(-20);
        assertEquals(40, relationshipTracker.getRapportLevel());
        assertEquals(35, relationshipTracker.getTrustLevel(), "Trust should be unchanged");
    }

    @Test
    @DisplayName("Relationship context without initialization")
    void testRelationshipContextWithoutInitialization() {
        String context = relationshipTracker.getRelationshipContext();

        assertTrue(context.contains("Rapport Level:"));
        assertTrue(context.contains("Trust Level:"));
        assertFalse(context.contains("Known for:"), "Should not contain days known without initialization");
    }
}
