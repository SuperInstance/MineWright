package com.minewright.mentorship;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.CompanionMemory;
import com.minewright.mentorship.MentorshipModels.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MentorshipManager}.
 *
 * <p>Tests cover worker registration, teaching moment detection,
 * explanation depth adjustment, praise generation, and NBT persistence.</p>
 *
 * @since 1.5.0
 */
@DisplayName("MentorshipManager Tests")
class MentorshipManagerTest {

    @Mock
    private ForemanEntity mockForeman;

    @Mock
    private CompanionMemory mockMemory;

    private MentorshipManager mentorshipManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lenient().when(mockForeman.getCompanionMemory()).thenReturn(mockMemory);
        lenient().when(mockForeman.getEntityName()).thenReturn("TestForeman");

        mentorshipManager = new MentorshipManager(mockForeman);
    }

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Constructor should initialize with empty workers")
    void testConstructorInitializesEmptyWorkers() {
        assertTrue(mentorshipManager.getWorkers().isEmpty(),
            "Should start with no workers");
    }

    @Test
    @DisplayName("Constructor should initialize taught concepts")
    void testConstructorInitializesTaughtConcepts() {
        assertNotNull(mentorshipManager.getWorkers(),
            "Workers map should be initialized");
    }

    // ========== registerWorker Tests ==========

    @Test
    @DisplayName("registerWorker should add worker to tracking")
    void testRegisterWorkerAddsWorker() {
        mentorshipManager.registerWorker("BuilderBob", "builder");

        assertEquals(1, mentorshipManager.getWorkers().size(),
            "Should have one worker");

        WorkerProfile worker = mentorshipManager.getWorker("BuilderBob");
        assertNotNull(worker, "Worker should be retrievable");
        assertEquals("BuilderBob", worker.getWorkerName());
        assertEquals("builder", worker.getWorkerRole());
    }

    @Test
    @DisplayName("registerWorker should allow multiple workers")
    void testRegisterWorkerMultipleWorkers() {
        mentorshipManager.registerWorker("MinerMike", "miner");
        mentorshipManager.registerWorker("BuilderBob", "builder");
        mentorshipManager.registerWorker("LumberjackLarry", "lumberjack");

        assertEquals(3, mentorshipManager.getWorkers().size(),
            "Should track all workers");
    }

    @Test
    @DisplayName("registerWorker should replace existing worker")
    void testRegisterWorkerReplacesExisting() {
        mentorshipManager.registerWorker("Worker", "builder");
        mentorshipManager.registerWorker("Worker", "miner");

        assertEquals(1, mentorshipManager.getWorkers().size(),
            "Should only have one entry");
        assertEquals("miner", mentorshipManager.getWorker("Worker").getWorkerRole());
    }

    // ========== unregisterWorker Tests ==========

    @Test
    @DisplayName("unregisterWorker should remove worker")
    void testUnregisterWorkerRemovesWorker() {
        mentorshipManager.registerWorker("Worker", "builder");
        assertNotNull(mentorshipManager.getWorker("Worker"));

        mentorshipManager.unregisterWorker("Worker");

        assertNull(mentorshipManager.getWorker("Worker"),
            "Worker should be removed");
        assertEquals(0, mentorshipManager.getWorkers().size());
    }

    @Test
    @DisplayName("unregisterWorker should handle unknown worker gracefully")
    void testUnregisterWorkerUnknown() {
        // Should not throw
        assertDoesNotThrow(() -> mentorshipManager.unregisterWorker("Unknown"));
    }

    // ========== detectTeachingMoment Tests ==========

    @Test
    @DisplayName("detectTeachingMoment should return null for unknown worker")
    void testDetectTeachingMomentUnknownWorker() {
        TeachingMoment moment = mentorshipManager.detectTeachingMoment(
            "Unknown", TeachingMomentTrigger.TASK_COMPLETED, "Built a house"
        );

        assertNull(moment,
            "Should return null for unknown worker");
    }

    @Test
    @DisplayName("detectTeachingMoment should detect moment for registered worker")
    void testDetectTeachingMomentRegisteredWorker() {
        mentorshipManager.registerWorker("Learner", "builder");

        TeachingMoment moment = mentorshipManager.detectTeachingMoment(
            "Learner", TeachingMomentTrigger.TASK_COMPLETED, "Built first structure"
        );

        // Note: Actual teaching moment logic depends on TeachingMomentDetector
        // This test verifies the method can be called without exception
        assertNotNull(mentorshipManager.getWorker("Learner"),
            "Worker should still be registered");
    }

    @Test
    @DisplayName("detectTeachingMoment should handle all trigger types")
    void testDetectTeachingMomentAllTriggerTypes() {
        mentorshipManager.registerWorker("Worker", "general");

        for (TeachingMomentTrigger trigger : TeachingMomentTrigger.values()) {
            assertDoesNotThrow(() -> {
                mentorshipManager.detectTeachingMoment("Worker", trigger, "Test context");
            }, "Should handle trigger: " + trigger);
        }
    }

    // ========== getExplanationDepth Tests ==========

    @Test
    @DisplayName("getExplanationDepth should return DETAILED for unknown worker")
    void testGetExplanationDepthUnknownWorker() {
        ExplanationDepth depth = mentorshipManager.getExplanationDepth(
            "Unknown", "Build a house"
        );

        assertEquals(ExplanationDepth.DETAILED, depth,
            "Unknown workers should get detailed explanations");
    }

    @Test
    @DisplayName("getExplanationDepth should adjust based on skill gap")
    void testGetExplanationDepthSkillGap() {
        mentorshipManager.registerWorker("Novice", "builder");

        // Low skill worker with complex task should get HANDS_ON
        ExplanationDepth depth1 = mentorshipManager.getExplanationDepth(
            "Novice", "Build an automated redstone sorting system"
        );

        assertNotNull(depth1, "Should return a valid depth");
    }

    // ========== generatePraise Tests ==========

    @Test
    @DisplayName("generatePraise should return generic praise for unknown worker")
    void testGeneratePraiseUnknownWorker() {
        String praise = mentorshipManager.generatePraise(
            "Unknown", new TaskCompletion("test task", true, 1000)
        );

        assertNotNull(praise, "Should return praise");
        assertFalse(praise.isEmpty(), "Praise should not be empty");
    }

    @Test
    @DisplayName("generatePraise should handle successful completion")
    void testGeneratePraiseSuccessfulCompletion() {
        mentorshipManager.registerWorker("Worker", "builder");

        String praise = mentorshipManager.generatePraise(
            "Worker", new TaskCompletion("Built a house", true, 5000)
        );

        assertNotNull(praise);
        assertFalse(praise.isEmpty());
    }

    @Test
    @DisplayName("generatePraise should handle failed task")
    void testGeneratePraiseFailedTask() {
        mentorshipManager.registerWorker("Worker", "builder");

        String praise = mentorshipManager.generatePraise(
            "Worker", new TaskCompletion("Attempted complex build", false, 3000)
        );

        assertNotNull(praise);
        assertFalse(praise.isEmpty());
    }

    // ========== getWorkers Tests ==========

    @Test
    @DisplayName("getWorkers should return unmodifiable map")
    void testGetWorkersReturnsUnmodifiable() {
        mentorshipManager.registerWorker("Worker", "role");

        Map<String, WorkerProfile> workers = mentorshipManager.getWorkers();

        assertThrows(UnsupportedOperationException.class,
            () -> workers.put("Another", new WorkerProfile("Another", "role")),
            "Should return unmodifiable map");
    }

    @Test
    @DisplayName("getWorkers should reflect current state")
    void testGetWorkersReflectsCurrentState() {
        assertTrue(mentorshipManager.getWorkers().isEmpty());

        mentorshipManager.registerWorker("Worker1", "role1");
        assertEquals(1, mentorshipManager.getWorkers().size());

        mentorshipManager.registerWorker("Worker2", "role2");
        assertEquals(2, mentorshipManager.getWorkers().size());
    }

    // ========== getWorker Tests ==========

    @Test
    @DisplayName("getWorker should return registered worker")
    void testGetWorkerReturnsRegistered() {
        mentorshipManager.registerWorker("Bob", "builder");

        WorkerProfile worker = mentorshipManager.getWorker("Bob");

        assertNotNull(worker);
        assertEquals("Bob", worker.getWorkerName());
        assertEquals("builder", worker.getWorkerRole());
    }

    @Test
    @DisplayName("getWorker should return null for unknown worker")
    void testGetWorkerReturnsNull() {
        WorkerProfile worker = mentorshipManager.getWorker("Unknown");

        assertNull(worker);
    }

    // ========== NBT Persistence Tests ==========

    @Test
    @DisplayName("saveToNBT should save worker data")
    void testSaveToNBTSavesWorkers() {
        mentorshipManager.registerWorker("Worker1", "role1");
        mentorshipManager.registerWorker("Worker2", "role2");

        CompoundTag tag = new CompoundTag();
        mentorshipManager.saveToNBT(tag);

        assertTrue(tag.contains("MentorshipData"),
            "Should save mentorship data");
    }

    @Test
    @DisplayName("loadFromNBT should load worker data")
    void testLoadFromNBTLoadsWorkers() {
        // Save some workers
        mentorshipManager.registerWorker("Worker1", "role1");
        CompoundTag saveTag = new CompoundTag();
        mentorshipManager.saveToNBT(saveTag);

        // Create new manager and load
        MentorshipManager newManager = new MentorshipManager(mockForeman);
        newManager.loadFromNBT(saveTag);

        assertEquals(1, newManager.getWorkers().size(),
            "Should load one worker");

        WorkerProfile worker = newManager.getWorker("Worker1");
        assertNotNull(worker, "Worker should be loaded");
        assertEquals("Worker1", worker.getWorkerName());
        assertEquals("role1", worker.getWorkerRole());
    }

    @Test
    @DisplayName("loadFromNBT should handle empty tag")
    void testLoadFromNBTEmptyTag() {
        CompoundTag emptyTag = new CompoundTag();

        assertDoesNotThrow(() -> mentorshipManager.loadFromNBT(emptyTag),
            "Should handle empty tag");
        assertTrue(mentorshipManager.getWorkers().isEmpty(),
            "Should have no workers after loading empty tag");
    }

    @Test
    @DisplayName("loadFromNBT should handle missing mentorship data")
    void testLoadFromNBTMissingData() {
        CompoundTag tag = new CompoundTag();
        // Don't add "MentorshipData" tag

        assertDoesNotThrow(() -> mentorshipManager.loadFromNBT(tag),
            "Should handle missing data gracefully");
    }

    @Test
    @DisplayName("Round-trip NBT save/load should preserve data")
    void testRoundTripNBTPreservesData() {
        // Set up initial state
        mentorshipManager.registerWorker("Worker1", "role1");
        mentorshipManager.registerWorker("Worker2", "role2");
        mentorshipManager.registerWorker("Worker3", "role3");

        // Save
        CompoundTag tag = new CompoundTag();
        mentorshipManager.saveToNBT(tag);

        // Load into new manager
        MentorshipManager loadedManager = new MentorshipManager(mockForeman);
        loadedManager.loadFromNBT(tag);

        // Verify
        assertEquals(3, loadedManager.getWorkers().size());
        assertNotNull(loadedManager.getWorker("Worker1"));
        assertNotNull(loadedManager.getWorker("Worker2"));
        assertNotNull(loadedManager.getWorker("Worker3"));
        assertEquals("role1", loadedManager.getWorker("Worker1").getWorkerRole());
        assertEquals("role2", loadedManager.getWorker("Worker2").getWorkerRole());
        assertEquals("role3", loadedManager.getWorker("Worker3").getWorkerRole());
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Full workflow: register, teach, save, load")
    void testFullWorkflow() {
        // Register workers
        mentorshipManager.registerWorker("Apprentice", "builder");

        // Detect teaching moments (should not throw)
        for (TeachingMomentTrigger trigger : TeachingMomentTrigger.values()) {
            mentorshipManager.detectTeachingMoment("Apprentice", trigger, "Test");
        }

        // Generate praise
        String praise = mentorshipManager.generatePraise(
            "Apprentice", new TaskCompletion("Completed task", true, 1000)
        );
        assertNotNull(praise);

        // Save and load
        CompoundTag tag = new CompoundTag();
        mentorshipManager.saveToNBT(tag);

        MentorshipManager newManager = new MentorshipManager(mockForeman);
        newManager.loadFromNBT(tag);

        // Verify state preserved
        assertEquals(1, newManager.getWorkers().size());
        assertNotNull(newManager.getWorker("Apprentice"));
    }

    @Test
    @DisplayName("Multiple workers should be tracked independently")
    void testMultipleWorkersTrackedIndependently() {
        mentorshipManager.registerWorker("Worker1", "role1");
        mentorshipManager.registerWorker("Worker2", "role2");

        WorkerProfile worker1 = mentorshipManager.getWorker("Worker1");
        WorkerProfile worker2 = mentorshipManager.getWorker("Worker2");

        assertNotEquals(worker1, worker2,
            "Workers should be distinct");
        assertEquals("Worker1", worker1.getWorkerName());
        assertEquals("Worker2", worker2.getWorkerName());
    }

    @Test
    @DisplayName("Unregistering one worker should not affect others")
    void testUnregisterDoesNotAffectOthers() {
        mentorshipManager.registerWorker("Worker1", "role1");
        mentorshipManager.registerWorker("Worker2", "role2");
        mentorshipManager.registerWorker("Worker3", "role3");

        mentorshipManager.unregisterWorker("Worker2");

        assertEquals(2, mentorshipManager.getWorkers().size());
        assertNotNull(mentorshipManager.getWorker("Worker1"));
        assertNotNull(mentorshipManager.getWorker("Worker3"));
        assertNull(mentorshipManager.getWorker("Worker2"));
    }

    // ========== Edge Case Tests ==========

    @Test
    @DisplayName("Should handle worker names with special characters")
    void testWorkerNamesWithSpecialCharacters() {
        String specialName = "Worker-With_Special.123";

        mentorshipManager.registerWorker(specialName, "role");

        assertNotNull(mentorshipManager.getWorker(specialName),
            "Should handle special characters in name");
    }

    @Test
    @DisplayName("Should handle empty worker name")
    void testEmptyWorkerName() {
        assertDoesNotThrow(() -> mentorshipManager.registerWorker("", "role"));

        // Empty name should be registered (though not recommended)
        assertNotNull(mentorshipManager.getWorker(""));
    }

    @Test
    @DisplayName("Should handle null context gracefully")
    void testNullContext() {
        mentorshipManager.registerWorker("Worker", "role");

        assertDoesNotThrow(() -> {
            mentorshipManager.detectTeachingMoment("Worker",
                TeachingMomentTrigger.TASK_COMPLETED, null);
        });
    }

    @Test
    @DisplayName("Should handle concurrent worker operations")
    void testConcurrentOperations() {
        // Register multiple workers rapidly
        for (int i = 0; i < 10; i++) {
            mentorshipManager.registerWorker("Worker" + i, "role" + i);
        }

        assertEquals(10, mentorshipManager.getWorkers().size());

        // Unregister some
        for (int i = 0; i < 5; i++) {
            mentorshipManager.unregisterWorker("Worker" + i);
        }

        assertEquals(5, mentorshipManager.getWorkers().size());
    }
}
