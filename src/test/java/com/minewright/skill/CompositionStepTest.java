package com.minewright.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CompositionStep}.
 *
 * Tests cover:
 * <ul>
 *   <li>Composition step creation</li>
 *   <li>Skill name and instance management</li>
 *   <li>Context variable handling</li>
 *   <li>Skill resolution from library</li>
 *   <li>Builder pattern usage</li>
 *   <li>Null and empty context handling</li>
 *   <li>Equality and hashCode</li>
 *   <li>toString output</li>
 * </ul>
 *
 * @see CompositionStep
 * @see ComposedSkill
 * @see SkillComposer
 * @since 1.1.0
 */
@DisplayName("CompositionStep Tests")
class CompositionStepTest {

    private Skill mockSkill;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        mockSkill = mock(Skill.class);
        when(mockSkill.getName()).thenReturn("testSkill");
        when(mockSkill.getCategory()).thenReturn("test");

        context = Map.of(
            "depth", 50,
            "direction", "north",
            "target", "iron_ore"
        );
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("CompositionStep creates with valid parameters")
    void testCompositionStepCreation() {
        CompositionStep step = new CompositionStep(
            "stripMine",
            mockSkill,
            context
        );

        assertEquals("stripMine", step.getSkillName());
        assertEquals(mockSkill, step.getSkill());
        assertEquals(3, step.getContext().size());
    }

    @Test
    @DisplayName("CompositionStep throws on null skill name")
    void testCompositionStepThrowsOnNullSkillName() {
        assertThrows(NullPointerException.class, () -> {
            new CompositionStep(null, mockSkill, context);
        });
    }

    @Test
    @DisplayName("CompositionStep allows null skill")
    void testCompositionStepAllowsNullSkill() {
        assertDoesNotThrow(() -> {
            CompositionStep step = new CompositionStep(
                "testSkill",
                null,
                context
            );

            assertNull(step.getSkill());
        });
    }

    @Test
    @DisplayName("CompositionStep handles null context")
    void testCompositionStepHandlesNullContext() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            null
        );

        assertTrue(step.getContext().isEmpty());
    }

    @Test
    @DisplayName("CompositionStep creates defensive copy of context")
    void testCompositionStepCreatesDefensiveCopyOfContext() {
        Map<String, Object> mutableContext = new java.util.HashMap<>();
        mutableContext.put("key1", "value1");

        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            mutableContext
        );

        // Modify original map
        mutableContext.put("key2", "value2");

        // Step should not be affected
        assertFalse(step.getContext().containsKey("key2"));
        assertEquals(1, step.getContext().size());
    }

    // ==================== Getter Tests ====================

    @Test
    @DisplayName("Get skill name returns correct value")
    void testGetSkillName() {
        CompositionStep step = new CompositionStep(
            "mineOre",
            mockSkill,
            context
        );

        assertEquals("mineOre", step.getSkillName());
    }

    @Test
    @DisplayName("Get skill returns skill instance")
    void testGetSkill() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        assertEquals(mockSkill, step.getSkill());
    }

    @Test
    @DisplayName("Get skill returns null when not set")
    void testGetSkillReturnsNull() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            null,
            context
        );

        assertNull(step.getSkill());
    }

    @Test
    @DisplayName("Get context returns unmodifiable map")
    void testGetContextReturnsUnmodifiableMap() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        Map<String, Object> returnedContext = step.getContext();

        assertThrows(UnsupportedOperationException.class, () -> {
            returnedContext.put("newKey", "newValue");
        });
    }

    @Test
    @DisplayName("Get context contains all values")
    void testGetContextContainsAllValues() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        Map<String, Object> returnedContext = step.getContext();

        assertEquals(3, returnedContext.size());
        assertEquals(50, returnedContext.get("depth"));
        assertEquals("north", returnedContext.get("direction"));
        assertEquals("iron_ore", returnedContext.get("target"));
    }

    // ==================== Has Skill Tests ====================

    @Test
    @DisplayName("Has skill returns true when skill is set")
    void testHasSkillReturnsTrue() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        assertTrue(step.hasSkill());
    }

    @Test
    @DisplayName("Has skill returns false when skill is null")
    void testHasSkillReturnsFalse() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            null,
            context
        );

        assertFalse(step.hasSkill());
    }

    // ==================== Resolve Tests ====================

    @Test
    @DisplayName("Resolve returns same step when skill already set")
    void testResolveReturnsSameStepWhenSkillSet() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        CompositionStep resolved = step.resolve();

        assertSame(step, resolved, "Should return same step when already resolved");
    }

    @Test
    @DisplayName("Resolve resolves skill from library when null")
    void testResolveResolvesSkillFromLibrary() {
        // Add skill to library
        SkillLibrary.getInstance().registerSkill(mockSkill);

        CompositionStep step = new CompositionStep(
            "testSkill",
            null,
            context
        );

        CompositionStep resolved = step.resolve();

        assertNotSame(step, resolved, "Should return new step with resolved skill");
        assertNotNull(resolved.getSkill(), "Skill should be resolved");
        assertEquals("testSkill", resolved.getSkill().getName());

        // Clean up
        SkillLibrary.getInstance().unregisterSkill("testSkill");
    }

    @Test
    @DisplayName("Resolve preserves context")
    void testResolvePreservesContext() {
        SkillLibrary.getInstance().registerSkill(mockSkill);

        CompositionStep step = new CompositionStep(
            "testSkill",
            null,
            context
        );

        CompositionStep resolved = step.resolve();

        assertEquals(step.getContext(), resolved.getContext(),
                     "Context should be preserved during resolution");

        // Clean up
        SkillLibrary.getInstance().unregisterSkill("testSkill");
    }

    // ==================== Builder Tests ====================

    @Test
    @DisplayName("Builder creates step with skill")
    void testBuilderCreatesStepWithSkill() {
        CompositionStep step = CompositionStep.builder("testSkill")
            .skill(mockSkill)
            .build();

        assertEquals("testSkill", step.getSkillName());
        assertEquals(mockSkill, step.getSkill());
    }

    @Test
    @DisplayName("Builder creates step with context variables")
    void testBuilderCreatesStepWithContext() {
        CompositionStep step = CompositionStep.builder("testSkill")
            .context("depth", 50)
            .context("direction", "north")
            .build();

        assertEquals(2, step.getContext().size());
        assertEquals(50, step.getContext().get("depth"));
        assertEquals("north", step.getContext().get("direction"));
    }

    @Test
    @DisplayName("Builder creates step with context map")
    void testBuilderCreatesStepWithContextMap() {
        Map<String, Object> contextMap = Map.of(
            "key1", "value1",
            "key2", "value2"
        );

        CompositionStep step = CompositionStep.builder("testSkill")
            .context(contextMap)
            .build();

        assertEquals(2, step.getContext().size());
        assertEquals("value1", step.getContext().get("key1"));
        assertEquals("value2", step.getContext().get("key2"));
    }

    @Test
    @DisplayName("Builder combines individual and map context")
    void testBuilderCombinesContext() {
        Map<String, Object> contextMap = Map.of("key1", "value1");

        CompositionStep step = CompositionStep.builder("testSkill")
            .context(contextMap)
            .context("key2", "value2")
            .build();

        assertEquals(2, step.getContext().size());
        assertTrue(step.getContext().containsKey("key1"));
        assertTrue(step.getContext().containsKey("key2"));
    }

    @Test
    @DisplayName("Builder resolves skill from library if not set")
    void testBuilderResolvesSkillFromLibrary() {
        SkillLibrary.getInstance().registerSkill(mockSkill);

        CompositionStep step = CompositionStep.builder("testSkill")
            .context("key", "value")
            .build();

        assertNotNull(step.getSkill(), "Builder should resolve skill from library");
        assertEquals("testSkill", step.getSkill().getName());

        // Clean up
        SkillLibrary.getInstance().unregisterSkill("testSkill");
    }

    @Test
    @DisplayName("Builder allows chaining")
    void testBuilderAllowsChaining() {
        CompositionStep step = CompositionStep.builder("testSkill")
            .skill(mockSkill)
            .context("key1", "value1")
            .context("key2", "value2")
            .context(Map.of("key3", "value3"))
            .build();

        assertEquals(mockSkill, step.getSkill());
        assertEquals(3, step.getContext().size());
    }

    // ==================== Equality Tests ====================

    @Test
    @DisplayName("Equals returns true for identical steps")
    void testEqualsForIdenticalSteps() {
        CompositionStep step1 = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        CompositionStep step2 = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        assertEquals(step1, step2);
    }

    @Test
    @DisplayName("Equals returns false for different skill names")
    void testEqualsForDifferentSkillNames() {
        CompositionStep step1 = new CompositionStep(
            "skill1",
            mockSkill,
            context
        );

        CompositionStep step2 = new CompositionStep(
            "skill2",
            mockSkill,
            context
        );

        assertNotEquals(step1, step2);
    }

    @Test
    @DisplayName("Equals returns false for different contexts")
    void testEqualsForDifferentContexts() {
        CompositionStep step1 = new CompositionStep(
            "testSkill",
            mockSkill,
            Map.of("key1", "value1")
        );

        CompositionStep step2 = new CompositionStep(
            "testSkill",
            mockSkill,
            Map.of("key2", "value2")
        );

        assertNotEquals(step1, step2);
    }

    @Test
    @DisplayName("Equals ignores skill instance")
    void testEqualsIgnoresSkillInstance() {
        Skill mockSkill2 = mock(Skill.class);
        when(mockSkill2.getName()).thenReturn("testSkill");

        CompositionStep step1 = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        CompositionStep step2 = new CompositionStep(
            "testSkill",
            mockSkill2,
            context
        );

        assertEquals(step1, step2, "Equals should ignore skill instance");
    }

    @Test
    @DisplayName("Equals returns false for null")
    void testEqualsForNull() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        assertNotEquals(null, step);
    }

    @Test
    @DisplayName("Equals returns false for different class")
    void testEqualsForDifferentClass() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        assertNotEquals("not a step", step);
    }

    @Test
    @DisplayName("Equals returns true for same instance")
    void testEqualsForSameInstance() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        assertEquals(step, step);
    }

    // ==================== HashCode Tests ====================

    @Test
    @DisplayName("HashCode is consistent for equal steps")
    void testHashCodeConsistent() {
        CompositionStep step1 = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        CompositionStep step2 = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        assertEquals(step1.hashCode(), step2.hashCode());
    }

    @Test
    @DisplayName("HashCode differs for different steps")
    void testHashCodeDiffers() {
        CompositionStep step1 = new CompositionStep(
            "skill1",
            mockSkill,
            context
        );

        CompositionStep step2 = new CompositionStep(
            "skill2",
            mockSkill,
            context
        );

        assertNotEquals(step1.hashCode(), step2.hashCode());
    }

    @Test
    @DisplayName("HashCode is consistent across multiple calls")
    void testHashCodeConsistentAcrossCalls() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        int hash1 = step.hashCode();
        int hash2 = step.hashCode();

        assertEquals(hash1, hash2);
    }

    // ==================== ToString Tests ====================

    @Test
    @DisplayName("ToString contains skill name and context count")
    void testToStringFormat() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            context
        );

        String str = step.toString();

        assertTrue(str.contains("CompositionStep"));
        assertTrue(str.contains("testSkill"));
        assertTrue(str.contains("3")); // context size
    }

    @Test
    @DisplayName("ToString handles empty context")
    void testToStringWithEmptyContext() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            Map.of()
        );

        String str = step.toString();

        assertTrue(str.contains("0 vars"));
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("CompositionStep with empty context works")
    void testCompositionStepWithEmptyContext() {
        assertDoesNotThrow(() -> {
            CompositionStep step = new CompositionStep(
                "testSkill",
                mockSkill,
                Map.of()
            );

            assertTrue(step.getContext().isEmpty());
        });
    }

    @Test
    @DisplayName("CompositionStep with complex context values works")
    void testCompositionStepWithComplexContextValues() {
        Map<String, Object> complexContext = Map.of(
            "string", "value",
            "integer", 42,
            "double", 3.14,
            "boolean", true,
            "null", null
        );

        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            complexContext
        );

        assertEquals(5, step.getContext().size());
        assertEquals("value", step.getContext().get("string"));
        assertEquals(42, step.getContext().get("integer"));
        assertEquals(3.14, step.getContext().get("double"));
        assertEquals(true, step.getContext().get("boolean"));
        assertEquals(null, step.getContext().get("null"));
    }

    @Test
    @DisplayName("Builder with empty context creates step")
    void testBuilderWithEmptyContext() {
        CompositionStep step = CompositionStep.builder("testSkill")
            .build();

        assertTrue(step.getContext().isEmpty());
    }

    @Test
    @DisplayName("Resolve handles missing skill gracefully")
    void testResolveHandlesMissingSkill() {
        // Don't add skill to library
        CompositionStep step = new CompositionStep(
            "nonExistentSkill",
            null,
            context
        );

        CompositionStep resolved = step.resolve();

        assertNotNull(resolved);
        assertNull(resolved.getSkill(), "Should return null when skill not in library");
    }

    @Test
    @DisplayName("Builder handles null skill name")
    void testBuilderHandlesNullSkillName() {
        assertThrows(NullPointerException.class, () -> {
            CompositionStep.builder(null);
        });
    }

    @Test
    @DisplayName("Get context on empty step returns empty map")
    void testGetContextOnEmptyStep() {
        CompositionStep step = new CompositionStep(
            "testSkill",
            null,
            null
        );

        assertTrue(step.getContext().isEmpty());
    }

    @Test
    @DisplayName("CompositionStep context is independent of original")
    void testContextIndependence() {
        Map<String, Object> originalContext = new java.util.HashMap<>();
        originalContext.put("key", "value");

        CompositionStep step = new CompositionStep(
            "testSkill",
            mockSkill,
            originalContext
        );

        // Clear original map
        originalContext.clear();

        // Step context should be unaffected
        assertFalse(step.getContext().isEmpty());
        assertEquals("value", step.getContext().get("key"));
    }

    @Test
    @DisplayName("Builder context modifications don't affect step")
    void testBuilderContextIndependence() {
        Map<String, Object> mutableContext = new java.util.HashMap<>();
        mutableContext.put("key1", "value1");

        CompositionStep.Builder builder = CompositionStep.builder("testSkill")
            .context(mutableContext);

        // Modify map after passing to builder
        mutableContext.put("key2", "value2");

        CompositionStep step = builder.build();

        // Step should only have key1
        assertEquals(1, step.getContext().size());
        assertTrue(step.getContext().containsKey("key1"));
        assertFalse(step.getContext().containsKey("key2"));
    }
}
