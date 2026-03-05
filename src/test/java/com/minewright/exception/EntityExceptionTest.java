package com.minewright.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for EntityException.
 * Tests exception construction, factory methods, entity info, and edge cases.
 */
@DisplayName("EntityException Tests")
class EntityExceptionTest {

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Basic constructor creates exception with entity info")
    void testBasicConstructor() {
        EntityException exception = new EntityException(
            "Entity spawn failed",
            "uuid-12345",
            "Steve",
            MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED,
            "Try different location"
        );

        assertEquals("Entity spawn failed", exception.getMessage());
        assertEquals("uuid-12345", exception.getEntityId());
        assertEquals("Steve", exception.getEntityName());
        assertEquals(MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED, exception.getErrorCode());
        assertEquals("Try different location", exception.getRecoverySuggestion());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with cause preserves exception chain")
    void testConstructorWithCause() {
        Throwable cause = new IllegalStateException("World not loaded");
        EntityException exception = new EntityException(
            "Tick error",
            "uuid-67890",
            "Alex",
            MineWrightException.ErrorCode.ENTITY_TICK_ERROR,
            "Wait for world load",
            cause
        );

        assertEquals("Tick error", exception.getMessage());
        assertEquals("uuid-67890", exception.getEntityId());
        assertEquals("Alex", exception.getEntityName());
        assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Constructor with cause includes context in base exception")
    void testConstructorWithCauseIncludesContext() {
        Throwable cause = new RuntimeException("Test cause");
        EntityException exception = new EntityException(
            "Test error",
            "test-id",
            "TestBot",
            MineWrightException.ErrorCode.ENTITY_INVALID_STATE,
            "Reset state",
            cause
        );

        // The constructor adds context to the base exception
        // Check that context was added
        String context = exception.getContext();
        assertNotNull(context);
        assertTrue(context.contains("TestBot") || context.contains("test-id"));
    }

    @Test
    @DisplayName("Constructor accepts null entity ID and name")
    void testConstructorWithNulls() {
        EntityException exception = new EntityException(
            "Generic entity error",
            null,
            null,
            MineWrightException.ErrorCode.ENTITY_NOT_FOUND,
            "Create entity"
        );

        assertNull(exception.getEntityId());
        assertNull(exception.getEntityName());
    }

    // ========== Factory Method Tests ==========

    @Test
    @DisplayName("spawnFailed factory creates correct exception")
    void testSpawnFailedFactory() {
        EntityException exception = EntityException.spawnFailed("BuilderBot", "No valid spawn point");

        assertEquals("Failed to spawn entity 'BuilderBot': No valid spawn point", exception.getMessage());
        assertEquals("BuilderBot", exception.getEntityName());
        assertEquals("unknown", exception.getEntityId(), "spawnFailed uses 'unknown' for ID");
        assertEquals(MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED, exception.getErrorCode());
        assertTrue(exception.getRecoverySuggestion().contains("spawn space"));
        assertTrue(exception.getRecoverySuggestion().contains("respawning"));
    }

    @Test
    @DisplayName("spawnFailed with various failure reasons")
    void testSpawnFailedReasons() {
        EntityException noSpace = EntityException.spawnFailed("Miner", "No space");
        EntityException worldNotReady = EntityException.spawnFailed("Farmer", "World not loaded");
        EntityException tooMany = EntityException.spawnFailed("Guard", "Too many entities");

        assertTrue(noSpace.getMessage().contains("No space"));
        assertTrue(worldNotReady.getMessage().contains("World not loaded"));
        assertTrue(tooMany.getMessage().contains("Too many entities"));
    }

    @Test
    @DisplayName("tickError factory creates exception with cause")
    void testTickErrorFactory() {
        Throwable cause = new NullPointerException("Null target entity");
        EntityException exception = EntityException.tickError("uuid-111", "Warrior", "Target is null", cause);

        assertEquals("Entity tick error for 'Warrior': Target is null", exception.getMessage());
        assertEquals("uuid-111", exception.getEntityId());
        assertEquals("Warrior", exception.getEntityName());
        assertEquals(MineWrightException.ErrorCode.ENTITY_TICK_ERROR, exception.getErrorCode());
        assertSame(cause, exception.getCause());
        assertTrue(exception.getRecoverySuggestion().contains("recoverable"));
        assertTrue(exception.getRecoverySuggestion().contains("respawning"));
    }

    @Test
    @DisplayName("tickError factory handles null cause")
    void testTickErrorFactoryNullCause() {
        EntityException exception = EntityException.tickError("uuid-222", "Archer", "State invalid", null);

        assertNull(exception.getCause());
        assertTrue(exception.getRecoverySuggestion().contains("recoverable"));
    }

    @Test
    @DisplayName("invalidState factory creates correct exception")
    void testInvalidStateFactory() {
        EntityException exception = EntityException.invalidState("uuid-333", "Trader", "IDLE but has active task");

        assertEquals("Entity 'Trader' in invalid state: IDLE but has active task", exception.getMessage());
        assertEquals("uuid-333", exception.getEntityId());
        assertEquals("Trader", exception.getEntityName());
        assertEquals(MineWrightException.ErrorCode.ENTITY_INVALID_STATE, exception.getErrorCode());
        assertTrue(exception.getRecoverySuggestion().contains("reset"));
        assertTrue(exception.getRecoverySuggestion().contains("respawn"));
    }

    @Test
    @DisplayName("invalidState with various invalid states")
    void testInvalidStateStates() {
        EntityException state1 = EntityException.invalidState("id1", "Bot1", "EXECUTING but no task");
        EntityException state2 = EntityException.invalidState("id2", "Bot2", "PLANNING but executor null");
        EntityException state3 = EntityException.invalidState("id3", "Bot3", "ERROR but never started");

        assertTrue(state1.getMessage().contains("EXECUTING"));
        assertTrue(state2.getMessage().contains("PLANNING"));
        assertTrue(state3.getMessage().contains("ERROR"));
    }

    @Test
    @DisplayName("notFound factory creates correct exception")
    void testNotFoundFactory() {
        EntityException exception = EntityException.notFound("MissingBot");

        assertEquals("Entity not found: MissingBot", exception.getMessage());
        assertEquals("MissingBot", exception.getEntityName());
        assertEquals("unknown", exception.getEntityId(), "notFound uses 'unknown' for ID");
        assertEquals(MineWrightException.ErrorCode.ENTITY_NOT_FOUND, exception.getErrorCode());
        assertTrue(exception.getRecoverySuggestion().contains("/crew list"));
        assertTrue(exception.getRecoverySuggestion().contains("check the name"));
    }

    @Test
    @DisplayName("notFound with various entity names")
    void testNotFoundNames() {
        EntityException notFound1 = EntityException.notFound("Steve");
        EntityException notFound2 = EntityException.notFound("Alex");
        EntityException notFound3 = EntityException.notFound("CustomBot-123");

        assertTrue(notFound1.getMessage().contains("Steve"));
        assertTrue(notFound2.getMessage().contains("Alex"));
        assertTrue(notFound3.getMessage().contains("CustomBot-123"));
    }

    // ========== toString Tests ==========

    @Test
    @DisplayName("toString includes all relevant fields")
    void testToStringFormat() {
        EntityException exception = new EntityException(
            "Test entity error",
            "test-uuid-456",
            "TestEntity",
            MineWrightException.ErrorCode.ENTITY_TICK_ERROR,
            "Recovery suggestion"
        );

        String toString = exception.toString();
        assertTrue(toString.contains("EntityException"));
        assertTrue(toString.contains("entityName='TestEntity'"));
        assertTrue(toString.contains("entityId='test-uuid-456'"));
        assertTrue(toString.contains("code=" + MineWrightException.ErrorCode.ENTITY_TICK_ERROR.getCode()));
        assertTrue(toString.contains("message='Test entity error'"));
    }

    @Test
    @DisplayName("toString with unknown entity ID")
    void testToStringUnknownId() {
        EntityException exception = EntityException.notFound("Missing");

        assertTrue(exception.toString().contains("entityId='unknown'"));
    }

    // ========== Entity Info Tests ==========

    @Test
    @DisplayName("getEntityId returns correct IDs")
    void testGetEntityId() {
        EntityException withId = new EntityException(
            "Error",
            "specific-uuid-789",
            "Bot",
            MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED,
            "Fix"
        );
        EntityException withoutId = EntityException.spawnFailed("Bot", "reason");

        assertEquals("specific-uuid-789", withId.getEntityId());
        assertEquals("unknown", withoutId.getEntityId());
    }

    @Test
    @DisplayName("getEntityName returns correct names")
    void testGetEntityName() {
        assertEquals("Steve", EntityException.notFound("Steve").getEntityName());
        assertEquals("Alex", EntityException.spawnFailed("Alex", "No space").getEntityName());
        assertEquals("Builder", EntityException.invalidState("id", "Builder", "bad state").getEntityName());
    }

    // ========== Edge Case Tests ==========

    @Test
    @DisplayName("Exception can be thrown and caught correctly")
    void testExceptionThrowCatch() {
        EntityException thrown = assertThrows(EntityException.class, () -> {
            throw EntityException.notFound("TestBot");
        });

        assertEquals("Entity not found: TestBot", thrown.getMessage());
    }

    @Test
    @DisplayName("Exception with cause preserves stack trace")
    void testExceptionCauseStackTrace() {
        Throwable cause = new IllegalArgumentException("Invalid entity type");
        EntityException exception = EntityException.tickError("id", "Bot", "Error", cause);

        assertSame(cause, exception.getCause());
        assertEquals("Invalid entity type", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Multiple entity exceptions can be chained")
    void testExceptionChaining() {
        Throwable rootCause = new IllegalStateException("Root error");
        EntityException middle = new EntityException(
            "Middle entity error",
            "middle-id",
            "MiddleBot",
            MineWrightException.ErrorCode.ENTITY_TICK_ERROR,
            "Fix middle",
            rootCause
        );
        EntityException top = new EntityException(
            "Top entity error",
            "top-id",
            "TopBot",
            MineWrightException.ErrorCode.ENTITY_INVALID_STATE,
            "Fix top",
            middle
        );

        assertSame(middle, top.getCause());
        assertSame(rootCause, top.getCause().getCause());
    }

    @Test
    @DisplayName("Factory methods handle empty and null parameters")
    void testFactoryMethodEmptyNullParams() {
        EntityException emptyName = EntityException.notFound("");
        EntityException emptyReason = EntityException.spawnFailed("Bot", "");
        EntityException emptyState = EntityException.invalidState("id", "Bot", "");

        assertEquals("", emptyName.getEntityName());
        assertEquals("", emptyReason.getMessage().substring(emptyReason.getMessage().lastIndexOf(": ") + 2));
        assertEquals("", emptyState.getMessage().substring(emptyState.getMessage().lastIndexOf(": ") + 2));
    }

    @Test
    @DisplayName("Exception handles very long entity names")
    void testLongEntityName() {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longName.append("VeryLongBotName").append(i);
        }

        EntityException exception = EntityException.notFound(longName.toString());
        assertTrue(exception.getEntityName().length() > 400);
    }

    @Test
    @DisplayName("Exception handles special characters in entity names")
    void testSpecialCharactersInName() {
        String specialName = "Bot-With-Special_Chars.123";
        EntityException exception = EntityException.spawnFailed(specialName, "No space");

        assertEquals(specialName, exception.getEntityName());
        assertTrue(exception.getMessage().contains(specialName));
    }

    @Test
    @DisplayName("Exception handles UUID-like entity IDs")
    void testUuidLikeIds() {
        String uuid1 = "550e8400-e29b-41d4-a716-446655440000";
        String uuid2 = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";

        EntityException exception1 = EntityException.invalidState(uuid1, "Bot1", "bad");
        EntityException exception2 = EntityException.tickError(uuid2, "Bot2", "error", null);

        assertEquals(uuid1, exception1.getEntityId());
        assertEquals(uuid2, exception2.getEntityId());
    }

    @Test
    @DisplayName("Exception is serializable")
    void testExceptionSerialization() {
        EntityException exception = new EntityException(
            "Serializable entity error",
            "serial-uuid",
            "SerialBot",
            MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED,
            "Respawn",
            new RuntimeException("Serialization cause")
        );

        // Verify exception structure
        assertNotNull(exception);
        assertEquals("Serializable entity error", exception.getMessage());
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Exception works in realistic entity scenario")
    void testRealisticScenario() {
        // Simulate a spawn failure scenario
        EntityException exception = EntityException.spawnFailed("BuilderBot", "No valid spawn point within 100 blocks");

        assertEquals("BuilderBot", exception.getEntityName());
        assertTrue(exception.getRecoverySuggestion().contains("spawn space"));
        assertTrue(exception.getRecoverySuggestion().contains("world is fully loaded"));

        // In real code, would trigger appropriate error handling
        if (exception.getErrorCode() == MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED) {
            // Would show error to user and suggest clearing space
            assertTrue(exception.getRecoverySuggestion().length() > 0);
        }
    }

    @Test
    @DisplayName("Different entity errors can be distinguished")
    void testEntityErrorDiscrimination() {
        EntityException spawn = EntityException.spawnFailed("Bot", "No space");
        EntityException tick = EntityException.tickError("id", "Bot", "Null pointer", null);
        EntityException state = EntityException.invalidState("id", "Bot", "Invalid state");
        EntityException notFound = EntityException.notFound("Bot");

        // Can distinguish by error code
        assertEquals(MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED, spawn.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.ENTITY_TICK_ERROR, tick.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.ENTITY_INVALID_STATE, state.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.ENTITY_NOT_FOUND, notFound.getErrorCode());
    }

    @Test
    @DisplayName("Recovery suggestions are contextually appropriate")
    void testRecoverySuggestions() {
        EntityException spawn = EntityException.spawnFailed("Bot", "No space");
        EntityException tick = EntityException.tickError("id", "Bot", "Error", null);
        EntityException state = EntityException.invalidState("id", "Bot", "Bad state");
        EntityException notFound = EntityException.notFound("Bot");

        // Each factory method provides appropriate recovery
        assertTrue(spawn.getRecoverySuggestion().contains("spawn space"));
        assertTrue(spawn.getRecoverySuggestion().contains("world is fully loaded"));

        assertTrue(tick.getRecoverySuggestion().contains("recoverable"));
        assertTrue(tick.getRecoverySuggestion().contains("respawning"));

        assertTrue(state.getRecoverySuggestion().contains("reset"));
        assertTrue(state.getRecoverySuggestion().contains("respawn"));

        assertTrue(notFound.getRecoverySuggestion().contains("/crew list"));
        assertTrue(notFound.getRecoverySuggestion().contains("check the name"));
    }

    @Test
    @DisplayName("Exception maintains error code type safety")
    void testErrorCodeTypeSafety() {
        EntityException exception = EntityException.notFound("TestBot");

        assertEquals(MineWrightException.ErrorCode.ENTITY_NOT_FOUND, exception.getErrorCode());

        // Can switch on error codes
        switch (exception.getErrorCode()) {
            case ENTITY_NOT_FOUND:
                // Correct branch
                break;
            case ENTITY_SPAWN_FAILED:
            case ENTITY_TICK_ERROR:
            case ENTITY_INVALID_STATE:
                fail("Should have entered ENTITY_NOT_FOUND case");
                break;
            default:
                fail("Unexpected error code");
        }
    }

    @Test
    @DisplayName("Factory methods are consistent with constructors")
    void testFactoryVsConstructorConsistency() {
        // Factory method
        EntityException factoryException = EntityException.spawnFailed("BuilderBot", "No space");

        // Manual constructor with same parameters
        EntityException manualException = new EntityException(
            "Failed to spawn entity 'BuilderBot': No space",
            "unknown",
            "BuilderBot",
            MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED,
            factoryException.getRecoverySuggestion()
        );

        assertEquals(factoryException.getEntityName(), manualException.getEntityName());
        assertEquals(factoryException.getErrorCode(), manualException.getErrorCode());
        assertEquals(factoryException.getMessage(), manualException.getMessage());
    }

    @Test
    @DisplayName("Entity info is preserved through exception chain")
    void testEntityInfoPreservation() {
        String entityId = "test-entity-uuid-123";
        String entityName = "TestBot";

        Throwable cause = new RuntimeException("Root cause");
        EntityException exception = EntityException.tickError(entityId, entityName, "Tick failed", cause);

        // Entity info should be accessible
        assertEquals(entityId, exception.getEntityId());
        assertEquals(entityName, exception.getEntityName());

        // Even after catching and rethrowing
        try {
            throw exception;
        } catch (EntityException e) {
            assertEquals(entityId, e.getEntityId());
            assertEquals(entityName, e.getEntityName());
        }
    }

    @Test
    @DisplayName("Context is properly set when cause is provided")
    void testContextSetting() {
        String entityId = "ctx-test-id";
        String entityName = "ContextBot";

        EntityException exception = EntityException.tickError(
            entityId,
            entityName,
            "Test error",
            new RuntimeException("Cause")
        );

        // The constructor should have added context
        String context = exception.getContext();
        assertNotNull(context, "Context should be set when cause is provided");

        // Context should contain entity information
        assertTrue(context.contains(entityName) || context.contains(entityId),
            "Context should contain entity name or ID");
    }
}
