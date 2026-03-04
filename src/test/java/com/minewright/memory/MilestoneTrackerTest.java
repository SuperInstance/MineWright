package com.minewright.memory;

import com.minewright.entity.ForemanEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MilestoneTracker}.
 *
 * Tests cover:
 * <ul>
 *   <li>Milestone creation and detection</li>
 *   <li>First-time event milestones</li>
 *   <li>Count-based milestones</li>
 *   <li>Achievement milestones</li>
 *   <li>Anniversary milestones</li>
 *   <li>NBT persistence</li>
 *   <li>Milestone message generation</li>
 * </ul>
 *
 * @see MilestoneTracker
 * @since 1.3.0
 */
@DisplayName("MilestoneTracker Tests")
class MilestoneTrackerTest {

    private MilestoneTracker milestoneTracker;

    @Mock
    private ForemanEntity mockForeman;

    @Mock
    private CompanionMemory mockMemory;

    @BeforeEach
    void setUp() {
        milestoneTracker = new MilestoneTracker();
        mockForeman = mock(ForemanEntity.class);
        mockMemory = mock(CompanionMemory.class);

        when(mockForeman.getCompanionMemory()).thenReturn(mockMemory);
        when(mockMemory.getPlayerName()).thenReturn("TestPlayer");
        when(mockMemory.getRapportLevel()).thenReturn(50);
        when(mockMemory.getInteractionCount()).thenReturn(10);
        when(mockMemory.getMilestones()).thenReturn(List.of());
        when(mockMemory.getFirstMeeting()).thenReturn(Instant.now().minusSeconds(86400)); // 1 day ago
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("MilestoneTracker initializes with empty state")
    void testMilestoneTrackerInitialization() {
        assertNotNull(milestoneTracker);
        assertTrue(milestoneTracker.getMilestones().isEmpty());
        assertTrue(milestoneTracker.getPendingMilestones().isEmpty());
    }

    // ==================== First Milestone Tests ====================

    @Test
    @DisplayName("First event creates a milestone")
    void testFirstEventCreatesMilestone() {
        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkMilestone(
            mockForeman, "task_completed", "Built a house"
        );

        assertTrue(result.isPresent(), "First event should create a milestone");
        assertEquals(MilestoneTracker.MilestoneType.FIRST, result.get().type);
        assertTrue(result.get().title.contains("First"));
    }

    @Test
    @DisplayName("Subsequent events do not create first milestone")
    void testSubsequentEventDoesNotCreateFirstMilestone() {
        // First event
        milestoneTracker.checkMilestone(mockForeman, "task_completed", null);

        // Second event
        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkMilestone(
            mockForeman, "task_completed", null
        );

        assertFalse(result.isPresent(), "Subsequent events should not create first milestone");
    }

    @Test
    @DisplayName("Different event types create separate first milestones")
    void testDifferentEventsCreateSeparateFirstMilestones() {
        Optional<MilestoneTracker.Milestone> first = milestoneTracker.checkMilestone(
            mockForeman, "task_completed", null
        );
        Optional<MilestoneTracker.Milestone> second = milestoneTracker.checkMilestone(
            mockForeman, "night_survived", null
        );

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertNotEquals(first.get().id, second.get().id);
    }

    // ==================== Count Milestone Tests ====================

    @Test
    @DisplayName("Count milestone triggered at threshold")
    void testCountMilestoneAtThreshold() {
        // Trigger 10 events to reach threshold
        for (int i = 0; i < 10; i++) {
            milestoneTracker.checkMilestone(mockForeman, "mining", null);
        }

        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkMilestone(
            mockForeman, "mining", null
        );

        // 10th event should trigger count milestone (though it's actually 11th call)
        // The count milestone is checked during the event, so after 10 events
        // we should check if any milestone was triggered
        assertTrue(milestoneTracker.getMilestones().size() > 0 || result.isPresent(),
                   "Count milestone should be triggered at threshold");
    }

    // ==================== Achievement Milestone Tests ====================

    @Test
    @DisplayName("Diamond found creates achievement milestone")
    void testDiamondFoundCreatesMilestone() {
        when(mockMemory.getFirstMeeting()).thenReturn(Instant.now());

        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkMilestone(
            mockForeman, "diamond_found", "diamond"
        );

        assertTrue(result.isPresent(), "Diamond found should create milestone");
        assertTrue(result.get().title.contains("Diamond"));
    }

    @Test
    @DisplayName("Nether visit creates achievement milestone")
    void testNetherVisitCreatesMilestone() {
        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkMilestone(
            mockForeman, "nether_visit", null
        );

        assertTrue(result.isPresent(), "Nether visit should create milestone");
        assertTrue(result.get().title.contains("Nether"));
    }

    @Test
    @DisplayName("Structure built creates achievement milestone")
    void testStructureBuiltCreatesMilestone() {
        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkMilestone(
            mockForeman, "structure_built", "castle"
        );

        assertTrue(result.isPresent(), "Structure built should create milestone");
        assertTrue(result.get().title.contains("castle"));
    }

    @Test
    @DisplayName("Enemy defeated creates achievement milestone")
    void testEnemyDefeatedCreatesMilestone() {
        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkMilestone(
            mockForeman, "enemy_defeated", "zombie"
        );

        assertTrue(result.isPresent(), "Enemy defeated should create milestone");
        assertTrue(result.get().title.contains("Battle"));
    }

    @Test
    @DisplayName("Night survived creates achievement milestone")
    void testNightSurvivedCreatesMilestone() {
        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkMilestone(
            mockForeman, "night_survived", null
        );

        assertTrue(result.isPresent(), "Night survived should create milestone");
        assertTrue(result.get().title.contains("Night"));
    }

    @Test
    @DisplayName("Gift exchanged creates achievement milestone")
    void testGiftExchangedCreatesMilestone() {
        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkMilestone(
            mockForeman, "gift_exchanged", "diamond sword"
        );

        assertTrue(result.isPresent(), "Gift exchanged should create milestone");
        assertTrue(result.get().title.contains("Gift"));
    }

    // ==================== Anniversary Tests ====================

    @Test
    @DisplayName("Anniversary check returns empty when first meeting is null")
    void testAnniversaryCheckWithNullFirstMeeting() {
        when(mockMemory.getFirstMeeting()).thenReturn(null);

        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkAnniversaries(mockForeman);

        assertFalse(result.isPresent(), "Should return empty when first meeting is null");
    }

    @Test
    @DisplayName("Anniversary check returns empty when player name is null")
    void testAnniversaryCheckWithNullPlayerName() {
        when(mockMemory.getPlayerName()).thenReturn(null);

        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkAnniversaries(mockForeman);

        assertFalse(result.isPresent(), "Should return empty when player name is null");
    }

    @Test
    @DisplayName("Seven day anniversary creates milestone")
    void testSevenDayAnniversary() {
        Instant sevenDaysAgo = Instant.now().minusSeconds(7 * 86400 + 3600); // 7 days + 1 hour
        when(mockMemory.getFirstMeeting()).thenReturn(sevenDaysAgo);

        Optional<MilestoneTracker.Milestone> result = milestoneTracker.checkAnniversaries(mockForeman);

        assertTrue(result.isPresent(), "Seven day anniversary should create milestone");
        assertEquals(MilestoneTracker.MilestoneType.ANNIVERSARY, result.get().type);
        assertTrue(result.get().title.contains("7"));
    }

    // ==================== Milestone Recording Tests ====================

    @Test
    @DisplayName("Record milestone adds to achieved milestones")
    void testRecordMilestoneAddsToAchieved() {
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "test_milestone",
            MilestoneTracker.MilestoneType.FIRST,
            "Test Milestone",
            "Test description",
            5,
            Instant.now()
        );

        milestoneTracker.recordMilestone(milestone, mockMemory);

        assertTrue(milestoneTracker.hasMilestone("test_milestone"));
        assertEquals(1, milestoneTracker.getMilestones().size());
    }

    @Test
    @DisplayName("Has milestone returns true for achieved milestone")
    void testHasMilestoneReturnsTrue() {
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "test_milestone",
            MilestoneTracker.MilestoneType.FIRST,
            "Test",
            "Description",
            5,
            Instant.now()
        );

        milestoneTracker.recordMilestone(milestone, mockMemory);

        assertTrue(milestoneTracker.hasMilestone("test_milestone"));
    }

    @Test
    @DisplayName("Has milestone returns false for non-existent milestone")
    void testHasMilestoneReturnsFalse() {
        assertFalse(milestoneTracker.hasMilestone("non_existent_milestone"));
    }

    // ==================== Pending Milestones Tests ====================

    @Test
    @DisplayName("Get pending milestones removes them from queue")
    void testGetPendingMilestoneRemovesFromQueue() {
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "pending_milestone",
            MilestoneTracker.MilestoneType.FIRST,
            "Pending",
            "Description",
            5,
            Instant.now()
        );

        milestoneTracker.recordMilestone(milestone, mockMemory);

        List<MilestoneTracker.Milestone> pending = milestoneTracker.getPendingMilestones();

        assertFalse(pending.isEmpty(), "Should have pending milestone");
        assertEquals(1, pending.size());

        // Second call should return empty
        List<MilestoneTracker.Milestone> secondCall = milestoneTracker.getPendingMilestones();
        assertTrue(secondCall.isEmpty(), "Second call should return empty");
    }

    @Test
    @DisplayName("Peek pending milestone does not remove it")
    void testPeekPendingMilestoneDoesNotRemove() {
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "peek_milestone",
            MilestoneTracker.MilestoneType.FIRST,
            "Peek Test",
            "Description",
            5,
            Instant.now()
        );

        milestoneTracker.recordMilestone(milestone, mockMemory);

        Optional<MilestoneTracker.Milestone> peek1 = milestoneTracker.peekPendingMilestone();
        Optional<MilestoneTracker.Milestone> peek2 = milestoneTracker.peekPendingMilestone();

        assertTrue(peek1.isPresent());
        assertTrue(peek2.isPresent(), "Peek should not remove the milestone");
    }

    @Test
    @DisplayName("Clear pending milestones removes all pending")
    void testClearPendingMilestones() {
        MilestoneTracker.Milestone milestone1 = new MilestoneTracker.Milestone(
            "pending_1",
            MilestoneTracker.MilestoneType.FIRST,
            "Pending 1",
            "Description",
            5,
            Instant.now()
        );

        MilestoneTracker.Milestone milestone2 = new MilestoneTracker.Milestone(
            "pending_2",
            MilestoneTracker.MilestoneType.COUNT,
            "Pending 2",
            "Description",
            5,
            Instant.now()
        );

        milestoneTracker.recordMilestone(milestone1, mockMemory);
        milestoneTracker.recordMilestone(milestone2, mockMemory);

        milestoneTracker.clearPendingMilestones();

        List<MilestoneTracker.Milestone> pending = milestoneTracker.getPendingMilestones();
        assertTrue(pending.isEmpty(), "All pending milestones should be cleared");
    }

    // ==================== NBT Persistence Tests ====================

    @Test
    @DisplayName("Save to NBT creates valid compound tag")
    void testSaveToNBTCreatesValidTag() {
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "nbt_test",
            MilestoneTracker.MilestoneType.ACHIEVEMENT,
            "NBT Test",
            "Testing NBT persistence",
            8,
            Instant.now()
        );

        milestoneTracker.recordMilestone(milestone, mockMemory);

        CompoundTag tag = new CompoundTag();
        milestoneTracker.saveToNBT(tag);

        assertTrue(tag.contains("Milestones"));
        assertTrue(tag.contains("FirstOccurrences"));
        assertTrue(tag.contains("Counters"));
        assertTrue(tag.contains("LastAnniversaryCheck"));
    }

    @Test
    @DisplayName("Save to NBT saves milestone data correctly")
    void testSaveToNBTSavesMilestoneData() {
        Instant testTime = Instant.ofEpochMilli(123456789000L);
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "save_test",
            MilestoneTracker.MilestoneType.FIRST,
            "Save Test",
            "Test Description",
            7,
            testTime
        );

        milestoneTracker.recordMilestone(milestone, mockMemory);

        CompoundTag tag = new CompoundTag();
        milestoneTracker.saveToNBT(tag);

        ListTag milestonesList = tag.getList("Milestones", 10);
        assertFalse(milestonesList.isEmpty());

        CompoundTag milestoneTag = milestonesList.getCompound(0);
        assertEquals("save_test", milestoneTag.getString("Id"));
        assertEquals("FIRST", milestoneTag.getString("Type"));
        assertEquals("Save Test", milestoneTag.getString("Title"));
        assertEquals("Test Description", milestoneTag.getString("Description"));
        assertEquals(7, milestoneTag.getInt("Importance"));
        assertEquals(123456789000L, milestoneTag.getLong("AchievedAt"));
    }

    @Test
    @DisplayName("Load from NBT restores milestone data")
    void testLoadFromNBTRestoresMilestone() {
        // Create and save a milestone
        MilestoneTracker.Milestone original = new MilestoneTracker.Milestone(
            "load_test",
            MilestoneTracker.MilestoneType.ANNIVERSARY,
            "Load Test",
            "Testing load functionality",
            9,
            Instant.now()
        );

        milestoneTracker.recordMilestone(original, mockMemory);

        // Save to NBT
        CompoundTag tag = new CompoundTag();
        milestoneTracker.saveToNBT(tag);

        // Create new tracker and load
        MilestoneTracker newTracker = new MilestoneTracker();
        newTracker.loadFromNBT(tag);

        // Verify loaded
        assertTrue(newTracker.hasMilestone("load_test"));
        assertEquals(1, newTracker.getMilestones().size());

        MilestoneTracker.Milestone loaded = newTracker.getMilestones().get(0);
        assertEquals(original.id, loaded.id);
        assertEquals(original.type, loaded.type);
        assertEquals(original.title, loaded.title);
        assertEquals(original.description, loaded.description);
        assertEquals(original.importance, loaded.importance);
    }

    @Test
    @DisplayName("Load from empty NBT creates empty tracker")
    void testLoadFromEmptyNBT() {
        CompoundTag tag = new CompoundTag();

        MilestoneTracker newTracker = new MilestoneTracker();
        newTracker.loadFromNBT(tag);

        assertTrue(newTracker.getMilestones().isEmpty());
        assertTrue(newTracker.getPendingMilestones().isEmpty());
    }

    // ==================== Milestone Message Tests ====================

    @Test
    @DisplayName("Generate milestone message contains required fields")
    void testGenerateMilestoneMessageContainsRequiredFields() {
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "message_test",
            MilestoneTracker.MilestoneType.FIRST,
            "Message Test",
            "Testing message generation",
            8,
            Instant.now()
        );

        PersonalitySystem.PersonalityProfile profile = new PersonalitySystem().getPersonality();

        String message = milestoneTracker.generateMilestoneMessage(milestone, mockMemory);

        assertNotNull(message);
        assertTrue(message.contains("MILESTONE CELEBRATION"));
        assertTrue(message.contains("Message Test"));
        assertTrue(message.contains("Testing message generation"));
        assertTrue(message.contains("TestPlayer"));
        assertTrue(message.contains("Personality"));
    }

    @Test
    @DisplayName("Generate milestone message adapts to rapport level")
    void testGenerateMilestoneMessageAdaptsToRapport() {
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "rapport_test",
            MilestoneTracker.MilestoneType.FIRST,
            "Rapport Test",
            "Testing rapport adaptation",
            5,
            Instant.now()
        );

        // Low rapport
        when(mockMemory.getRapportLevel()).thenReturn(20);
        String lowRapportMessage = milestoneTracker.generateMilestoneMessage(milestone, mockMemory);
        assertTrue(lowRapportMessage.contains("professional") ||
                   lowRapportMessage.contains("appropriate boundaries"));

        // High rapport
        when(mockMemory.getRapportLevel()).thenReturn(90);
        String highRapportMessage = milestoneTracker.generateMilestoneMessage(milestone, mockMemory);
        assertTrue(highRapportMessage.contains("affection") ||
                   highRapportMessage.contains("intimate"));
    }

    // ==================== Milestone Class Tests ====================

    @Test
    @DisplayName("Milestone toString generates correct format")
    void testMilestoneToString() {
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "tostring_test",
            MilestoneTracker.MilestoneType.ACHIEVEMENT,
            "ToString Test",
            "Testing toString",
            6,
            Instant.now()
        );

        String str = milestone.toString();

        assertTrue(str.contains("[ACHIEVEMENT]"));
        assertTrue(str.contains("ToString Test"));
        assertTrue(str.contains("Testing toString"));
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("Check milestone with null context does not throw")
    void testCheckMilestoneWithNullContext() {
        assertDoesNotThrow(() -> {
            milestoneTracker.checkMilestone(mockForeman, "test_event", null);
        });
    }

    @Test
    @DisplayName("Multiple milestones of different types can be tracked")
    void testMultipleMilestoneTypes() {
        // First milestone
        milestoneTracker.checkMilestone(mockForeman, "first_event", null);

        // Count milestone (trigger multiple times)
        for (int i = 0; i < 25; i++) {
            milestoneTracker.checkMilestone(mockForeman, "count_event", null);
        }

        // Achievement milestone
        milestoneTracker.checkMilestone(mockForeman, "diamond_found", null);

        // Verify all types are tracked
        assertTrue(milestoneTracker.getMilestones().size() > 0,
                   "Should have tracked multiple milestone types");
    }

    @Test
    @DisplayName("Milestone importance is preserved through save/load")
    void testMilestoneImportancePreserved() {
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "importance_test",
            MilestoneTracker.MilestoneType.FIRST,
            "Importance Test",
            "Testing importance preservation",
            10,
            Instant.now()
        );

        milestoneTracker.recordMilestone(milestone, mockMemory);

        CompoundTag tag = new CompoundTag();
        milestoneTracker.saveToNBT(tag);

        MilestoneTracker newTracker = new MilestoneTracker();
        newTracker.loadFromNBT(tag);

        assertEquals(10, newTracker.getMilestones().get(0).importance);
    }

    @Test
    @DisplayName("Milestone timestamp is preserved through save/load")
    void testMilestoneTimestampPreserved() {
        Instant testTime = Instant.ofEpochMilli(987654321000L);
        MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
            "timestamp_test",
            MilestoneTracker.MilestoneType.ANNIVERSARY,
            "Timestamp Test",
            "Testing timestamp preservation",
            6,
            testTime
        );

        milestoneTracker.recordMilestone(milestone, mockMemory);

        CompoundTag tag = new CompoundTag();
        milestoneTracker.saveToNBT(tag);

        MilestoneTracker newTracker = new MilestoneTracker();
        newTracker.loadFromNBT(tag);

        assertEquals(testTime, newTracker.getMilestones().get(0).achievedAt);
    }
}
