package com.minewright.personality;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BlendResult}.
 *
 * <p>Tests cover result construction, getter methods, blend descriptions,
 * string representations, equals/hashCode contracts, and builder pattern.</p>
 *
 * @since 1.3.0
 */
@DisplayName("BlendResult Tests")
class BlendResultTest {

    private ArtificerArchetype primary;
    private ArtificerArchetype secondary;
    private PersonalityTraits blendedTraits;
    private BlendResult blendResult;

    @BeforeEach
    void setUp() {
        // Create test archetypes - use actual enum values
        primary = ArtificerArchetype.LUCIUS_FOX;
        secondary = ArtificerArchetype.PHINEAS;

        // Create blended traits (averaged values)
        blendedTraits = new PersonalityTraits(
            (primary.getTraits().getOpenness() + secondary.getTraits().getOpenness()) / 2,
            (primary.getTraits().getConscientiousness() + secondary.getTraits().getConscientiousness()) / 2,
            (primary.getTraits().getExtraversion() + secondary.getTraits().getExtraversion()) / 2,
            (primary.getTraits().getAgreeableness() + secondary.getTraits().getAgreeableness()) / 2,
            (primary.getTraits().getNeuroticism() + secondary.getTraits().getNeuroticism()) / 2
        );

        // Create blend result
        int blendedFormality = (primary.getFormality() + secondary.getFormality()) / 2;
        int blendedHumor = (primary.getHumor() + secondary.getHumor()) / 2;
        int blendedEncouragement = (primary.getEncouragement() + secondary.getEncouragement()) / 2;

        blendResult = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, blendedFormality, blendedHumor, blendedEncouragement
        );
    }

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Constructor should store all fields")
    void testConstructorStoresAllFields() {
        assertEquals(primary, blendResult.getPrimary());
        assertEquals(secondary, blendResult.getSecondary());
        assertEquals(0.5, blendResult.getBlendWeight(), 0.001);
        assertEquals(blendedTraits, blendResult.getBlendedTraits());
    }

    @Test
    @DisplayName("Constructor should store blended communication values")
    void testConstructorStoresCommunicationValues() {
        int expectedFormality = (primary.getFormality() + secondary.getFormality()) / 2;
        int expectedHumor = (primary.getHumor() + secondary.getHumor()) / 2;
        int expectedEncouragement = (primary.getEncouragement() + secondary.getEncouragement()) / 2;

        assertEquals(expectedFormality, blendResult.getBlendedFormality());
        assertEquals(expectedHumor, blendResult.getBlendedHumor());
        assertEquals(expectedEncouragement, blendResult.getBlendedEncouragement());
    }

    // ========== Getter Tests ==========

    @Test
    @DisplayName("getPrimary should return primary archetype")
    void testGetPrimary() {
        assertEquals(primary, blendResult.getPrimary());
    }

    @Test
    @DisplayName("getSecondary should return secondary archetype")
    void testGetSecondary() {
        assertEquals(secondary, blendResult.getSecondary());
    }

    @Test
    @DisplayName("getBlendWeight should return weight")
    void testGetBlendWeight() {
        assertEquals(0.5, blendResult.getBlendWeight(), 0.001);
    }

    @Test
    @DisplayName("getBlendedTraits should return blended traits")
    void testGetBlendedTraits() {
        assertEquals(blendedTraits, blendResult.getBlendedTraits());
    }

    @Test
    @DisplayName("getBlendedFormality should return blended formality")
    void testGetBlendedFormality() {
        int expected = (primary.getFormality() + secondary.getFormality()) / 2;
        assertEquals(expected, blendResult.getBlendedFormality());
    }

    @Test
    @DisplayName("getBlendedHumor should return blended humor")
    void testGetBlendedHumor() {
        int expected = (primary.getHumor() + secondary.getHumor()) / 2;
        assertEquals(expected, blendResult.getBlendedHumor());
    }

    @Test
    @DisplayName("getBlendedEncouragement should return blended encouragement")
    void testGetBlendedEncouragement() {
        int expected = (primary.getEncouragement() + secondary.getEncouragement()) / 2;
        assertEquals(expected, blendResult.getBlendedEncouragement());
    }

    // ========== getBlendDescription Tests ==========

    @Test
    @DisplayName("getBlendDescription should return 'Pure primary' for weight 0.0")
    void testBlendDescriptionWeightZero() {
        BlendResult result = new BlendResult(
            primary, secondary, 0.0,
            blendedTraits, 50, 50, 50
        );

        String description = result.getBlendDescription();
        assertTrue(description.contains("Pure"));
        assertTrue(description.contains(primary.getName()));
        assertFalse(description.contains(secondary.getName()));
    }

    @Test
    @DisplayName("getBlendDescription should return 'Pure secondary' for weight 1.0")
    void testBlendDescriptionWeightOne() {
        BlendResult result = new BlendResult(
            primary, secondary, 1.0,
            blendedTraits, 50, 50, 50
        );

        String description = result.getBlendDescription();
        assertTrue(description.contains("Pure"));
        assertTrue(description.contains(secondary.getName()));
        assertFalse(description.contains(primary.getName()));
    }

    @Test
    @DisplayName("getBlendDescription should return 'hints of' for weight <= 0.33")
    void testBlendDescriptionHintsOf() {
        BlendResult result = new BlendResult(
            primary, secondary, 0.25,
            blendedTraits, 50, 50, 50
        );

        String description = result.getBlendDescription();
        assertTrue(description.contains("hints of"));
        assertTrue(description.contains(primary.getName()));
        assertTrue(description.contains(secondary.getName()));
    }

    @Test
    @DisplayName("getBlendDescription should return 'Balanced blend' for weight <= 0.66")
    void testBlendDescriptionBalanced() {
        BlendResult result = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );

        String description = result.getBlendDescription();
        assertTrue(description.contains("Balanced"));
        assertTrue(description.contains("blend"));
        assertTrue(description.contains(primary.getName()));
        assertTrue(description.contains(secondary.getName()));
    }

    @Test
    @DisplayName("getBlendDescription should return 'with foundation' for weight > 0.66")
    void testBlendDescriptionWithFoundation() {
        BlendResult result = new BlendResult(
            primary, secondary, 0.8,
            blendedTraits, 50, 50, 50
        );

        String description = result.getBlendDescription();
        assertTrue(description.contains("foundation"));
        assertTrue(description.contains(secondary.getName()));
        assertTrue(description.contains(primary.getName()));
    }

    // ========== toDetailedString Tests ==========

    @Test
    @DisplayName("toDetailedString should format all information")
    void testToDetailedString() {
        String detailed = blendResult.toDetailedString();

        assertTrue(detailed.contains("BlendResult:"));
        assertTrue(detailed.contains("Primary:"));
        assertTrue(detailed.contains(primary.getName()));
        assertTrue(detailed.contains(primary.getTitle()));
        assertTrue(detailed.contains("Secondary:"));
        assertTrue(detailed.contains(secondary.getName()));
        assertTrue(detailed.contains("Weight:"));
        assertTrue(detailed.contains("0.50"));
        assertTrue(detailed.contains("Traits:"));
        assertTrue(detailed.contains("Communication:"));
        assertTrue(detailed.contains("Formality="));
        assertTrue(detailed.contains("Humor="));
        assertTrue(detailed.contains("Encouragement="));
    }

    // ========== toString Tests ==========

    @Test
    @DisplayName("toString should format compactly")
    void testToString() {
        String result = blendResult.toString();

        assertTrue(result.contains("BlendResult"));
        assertTrue(result.contains(primary.getName()));
        assertTrue(result.contains(secondary.getName()));
        assertTrue(result.contains("50%")); // 0.5 = 50%
    }

    @Test
    @DisplayName("toString format should match expected pattern")
    void testToStringFormat() {
        BlendResult result = new BlendResult(
            primary, secondary, 0.75,
            blendedTraits, 60, 40, 80
        );

        String str = result.toString();
        assertTrue(str.matches("BlendResult\\[.*\\+.*\\(75%\\)]"));
    }

    // ========== equals Tests ==========

    @Test
    @DisplayName("equals should return true for identical results")
    void testEqualsIdentical() {
        BlendResult result1 = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );
        BlendResult result2 = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );

        assertEquals(result1, result2);
        assertEquals(result2, result1);
    }

    @Test
    @DisplayName("equals should return false for different weight")
    void testEqualsDifferentWeight() {
        BlendResult result1 = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );
        BlendResult result2 = new BlendResult(
            primary, secondary, 0.6,
            blendedTraits, 50, 50, 50
        );

        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("equals should return false for different primary")
    void testEqualsDifferentPrimary() {
        BlendResult result1 = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );
        BlendResult result2 = new BlendResult(
            ArtificerArchetype.HEPHAEUSTUS, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );

        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("equals should return false for different secondary")
    void testEqualsDifferentSecondary() {
        BlendResult result1 = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );
        BlendResult result2 = new BlendResult(
            primary, ArtificerArchetype.GETAFIX, 0.5,
            blendedTraits, 50, 50, 50
        );

        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("equals should return false for different formality")
    void testEqualsDifferentFormality() {
        BlendResult result1 = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );
        BlendResult result2 = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 60, 50, 50
        );

        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("equals should return false for null")
    void testEqualsNull() {
        assertNotEquals(null, blendResult);
    }

    @Test
    @DisplayName("equals should return false for different class")
    void testEqualsDifferentClass() {
        assertNotEquals("not a BlendResult", blendResult);
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void testEqualsSameInstance() {
        assertEquals(blendResult, blendResult);
    }

    // ========== hashCode Tests ==========

    @Test
    @DisplayName("hashCode should be consistent")
    void testHashCodeConsistent() {
        int hash1 = blendResult.hashCode();
        int hash2 = blendResult.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hashCode should be equal for equal objects")
    void testHashCodeEqualObjects() {
        BlendResult result1 = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );
        BlendResult result2 = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );

        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    @DisplayName("hashCode should differ for different objects")
    void testHashCodeDifferentObjects() {
        BlendResult result1 = new BlendResult(
            primary, secondary, 0.5,
            blendedTraits, 50, 50, 50
        );
        BlendResult result2 = new BlendResult(
            primary, secondary, 0.6,
            blendedTraits, 50, 50, 50
        );

        assertNotEquals(result1.hashCode(), result2.hashCode());
    }

    // ========== Builder Tests ==========

    @Test
    @DisplayName("Builder should construct valid result")
    void testBuilderConstructsValid() {
        BlendResult result = BlendResult.builder()
            .primary(primary)
            .secondary(secondary)
            .blendWeight(0.5)
            .blendedTraits(blendedTraits)
            .blendedFormality(50)
            .blendedHumor(60)
            .blendedEncouragement(70)
            .build();

        assertEquals(primary, result.getPrimary());
        assertEquals(secondary, result.getSecondary());
        assertEquals(0.5, result.getBlendWeight(), 0.001);
        assertEquals(50, result.getBlendedFormality());
        assertEquals(60, result.getBlendedHumor());
        assertEquals(70, result.getBlendedEncouragement());
    }

    @Test
    @DisplayName("Builder should throw when field is null")
    void testBuilderThrowsOnNullField() {
        BlendResult.Builder builder = BlendResult.builder()
            .primary(primary)
            .secondary(secondary)
            .blendWeight(0.5)
            // blendedTraits not set
            .blendedFormality(50)
            .blendedHumor(60)
            .blendedEncouragement(70);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("Builder should be reusable")
    void testBuilderReusable() {
        BlendResult.Builder builder = BlendResult.builder()
            .primary(primary)
            .secondary(secondary);

        BlendResult result1 = builder
            .blendWeight(0.3)
            .blendedTraits(blendedTraits)
            .blendedFormality(50)
            .blendedHumor(50)
            .blendedEncouragement(50)
            .build();

        BlendResult result2 = builder
            .blendWeight(0.7)
            .blendedTraits(blendedTraits)
            .blendedFormality(60)
            .blendedHumor(60)
            .blendedEncouragement(60)
            .build();

        assertEquals(0.3, result1.getBlendWeight(), 0.001);
        assertEquals(0.7, result2.getBlendWeight(), 0.001);
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Full workflow: create, describe, serialize")
    void testFullWorkflow() {
        // Create blend result
        BlendResult result = BlendResult.builder()
            .primary(ArtificerArchetype.LUCIUS_FOX)
            .secondary(ArtificerArchetype.PHINEAS)
            .blendWeight(0.4)
            .blendedTraits(new PersonalityTraits(60, 70, 50, 65, 45))
            .blendedFormality(55)
            .blendedHumor(65)
            .blendedEncouragement(70)
            .build();

        // Verify description
        String description = result.getBlendDescription();
        assertTrue(description.contains("Lucius Fox"));
        assertTrue(description.contains("Phineas"));

        // Verify string representations
        assertNotNull(result.toString());
        assertNotNull(result.toDetailedString());

        // Verify equality
        BlendResult copy = BlendResult.builder()
            .primary(result.getPrimary())
            .secondary(result.getSecondary())
            .blendWeight(result.getBlendWeight())
            .blendedTraits(result.getBlendedTraits())
            .blendedFormality(result.getBlendedFormality())
            .blendedHumor(result.getBlendedHumor())
            .blendedEncouragement(result.getBlendedEncouragement())
            .build();

        assertEquals(result, copy);
        assertEquals(result.hashCode(), copy.hashCode());
    }

    @Test
    @DisplayName("Blend weights at boundaries should produce correct descriptions")
    void testBoundaryBlendWeights() {
        // Test weight = 0.0
        BlendResult zeroWeight = new BlendResult(
            primary, secondary, 0.0,
            blendedTraits, 50, 50, 50
        );
        assertTrue(zeroWeight.getBlendDescription().contains("Pure"));

        // Test weight = 0.33 (boundary)
        BlendResult boundary1 = new BlendResult(
            primary, secondary, 0.33,
            blendedTraits, 50, 50, 50
        );
        assertTrue(boundary1.getBlendDescription().contains("hints of"));

        // Test weight = 0.66 (boundary)
        BlendResult boundary2 = new BlendResult(
            primary, secondary, 0.66,
            blendedTraits, 50, 50, 50
        );
        assertTrue(boundary2.getBlendDescription().contains("Balanced"));

        // Test weight = 1.0
        BlendResult oneWeight = new BlendResult(
            primary, secondary, 1.0,
            blendedTraits, 50, 50, 50
        );
        assertTrue(oneWeight.getBlendDescription().contains("Pure"));
    }

    @Test
    @DisplayName("Communication values should be blended correctly")
    void testCommunicationValuesBlending() {
        ArtificerArchetype arch1 = ArtificerArchetype.LUCIUS_FOX;
        ArtificerArchetype arch2 = ArtificerArchetype.PHINEAS;

        double weight = 0.6;
        int expectedFormality = (int) Math.round(arch1.getFormality() * 0.4 + arch2.getFormality() * 0.6);
        int expectedHumor = (int) Math.round(arch1.getHumor() * 0.4 + arch2.getHumor() * 0.6);
        int expectedEncouragement = (int) Math.round(arch1.getEncouragement() * 0.4 + arch2.getEncouragement() * 0.6);

        BlendResult result = new BlendResult(
            arch1, arch2, weight,
            blendedTraits, expectedFormality, expectedHumor, expectedEncouragement
        );

        assertEquals(expectedFormality, result.getBlendedFormality());
        assertEquals(expectedHumor, result.getBlendedHumor());
        assertEquals(expectedEncouragement, result.getBlendedEncouragement());
    }
}
