package com.minewright.personality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PersonalityTraits}.
 *
 * <p>Tests cover OCEAN trait validation, trait level descriptions,
 * personality blending, builder pattern, and equals/hashCode contracts.</p>
 *
 * @since 1.3.0
 */
@DisplayName("PersonalityTraits Tests")
class PersonalityTraitsTest {

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Constructor should accept valid trait values")
    void testConstructorAcceptsValidValues() {
        PersonalityTraits traits = new PersonalityTraits(50, 60, 70, 80, 90);

        assertEquals(50, traits.getOpenness());
        assertEquals(60, traits.getConscientiousness());
        assertEquals(70, traits.getExtraversion());
        assertEquals(80, traits.getAgreeableness());
        assertEquals(90, traits.getNeuroticism());
    }

    @Test
    @DisplayName("Constructor should accept boundary values (0 and 100)")
    void testConstructorAcceptsBoundaryValues() {
        PersonalityTraits minTraits = new PersonalityTraits(0, 0, 0, 0, 0);
        assertEquals(0, minTraits.getOpenness());
        assertEquals(0, minTraits.getConscientiousness());

        PersonalityTraits maxTraits = new PersonalityTraits(100, 100, 100, 100, 100);
        assertEquals(100, maxTraits.getNeuroticism());
        assertEquals(100, maxTraits.getAgreeableness());
    }

    @Test
    @DisplayName("Constructor should reject negative values")
    void testConstructorRejectsNegativeValues() {
        Exception exception = assertThrows(IllegalArgumentException.class,
            () -> new PersonalityTraits(-1, 50, 50, 50, 50));

        assertTrue(exception.getMessage().contains("openness"));
        assertTrue(exception.getMessage().contains("0 and 100"));
    }

    @Test
    @DisplayName("Constructor should reject values > 100")
    void testConstructorRejectsHighValues() {
        Exception exception = assertThrows(IllegalArgumentException.class,
            () -> new PersonalityTraits(101, 50, 50, 50, 50));

        assertTrue(exception.getMessage().contains("openness"));
        assertTrue(exception.getMessage().contains("0 and 100"));
    }

    @Test
    @DisplayName("Constructor should validate all traits")
    void testConstructorValidatesAllTraits() {
        // Test each trait position
        assertThrows(IllegalArgumentException.class,
            () -> new PersonalityTraits(50, -1, 50, 50, 50),
            "Should reject negative conscientiousness");

        assertThrows(IllegalArgumentException.class,
            () -> new PersonalityTraits(50, 50, 150, 50, 50),
            "Should reject high extraversion");

        assertThrows(IllegalArgumentException.class,
            () -> new PersonalityTraits(50, 50, 50, -10, 50),
            "Should reject negative agreeableness");

        assertThrows(IllegalArgumentException.class,
            () -> new PersonalityTraits(50, 50, 50, 50, 101),
            "Should reject high neuroticism");
    }

    // ========== Getter Tests ==========

    @Test
    @DisplayName("Getters should return correct values")
    void testGettersReturnCorrectValues() {
        PersonalityTraits traits = new PersonalityTraits(25, 45, 65, 85, 15);

        assertEquals(25, traits.getOpenness());
        assertEquals(45, traits.getConscientiousness());
        assertEquals(65, traits.getExtraversion());
        assertEquals(85, traits.getAgreeableness());
        assertEquals(15, traits.getNeuroticism());
    }

    // ========== getTraitLevelDescription Tests ==========

    @Test
    @DisplayName("getTraitLevelDescription should return Very Low for 0-20")
    void testTraitLevelDescriptionVeryLow() {
        assertEquals("Very Low", PersonalityTraits.getTraitLevelDescription(0));
        assertEquals("Very Low", PersonalityTraits.getTraitLevelDescription(10));
        assertEquals("Very Low", PersonalityTraits.getTraitLevelDescription(20));
    }

    @Test
    @DisplayName("getTraitLevelDescription should return Low for 21-40")
    void testTraitLevelDescriptionLow() {
        assertEquals("Low", PersonalityTraits.getTraitLevelDescription(21));
        assertEquals("Low", PersonalityTraits.getTraitLevelDescription(30));
        assertEquals("Low", PersonalityTraits.getTraitLevelDescription(40));
    }

    @Test
    @DisplayName("getTraitLevelDescription should return Average for 41-60")
    void testTraitLevelDescriptionAverage() {
        assertEquals("Average", PersonalityTraits.getTraitLevelDescription(41));
        assertEquals("Average", PersonalityTraits.getTraitLevelDescription(50));
        assertEquals("Average", PersonalityTraits.getTraitLevelDescription(60));
    }

    @Test
    @DisplayName("getTraitLevelDescription should return High for 61-80")
    void testTraitLevelDescriptionHigh() {
        assertEquals("High", PersonalityTraits.getTraitLevelDescription(61));
        assertEquals("High", PersonalityTraits.getTraitLevelDescription(70));
        assertEquals("High", PersonalityTraits.getTraitLevelDescription(80));
    }

    @Test
    @DisplayName("getTraitLevelDescription should return Very High for 81-100")
    void testTraitLevelDescriptionVeryHigh() {
        assertEquals("Very High", PersonalityTraits.getTraitLevelDescription(81));
        assertEquals("Very High", PersonalityTraits.getTraitLevelDescription(90));
        assertEquals("Very High", PersonalityTraits.getTraitLevelDescription(100));
    }

    // ========== blend Tests ==========

    @Test
    @DisplayName("blend with weight 0.0 should return original traits")
    void testBlendWeightZero() {
        PersonalityTraits original = new PersonalityTraits(80, 70, 60, 50, 40);
        PersonalityTraits other = new PersonalityTraits(20, 30, 40, 50, 60);

        PersonalityTraits result = original.blend(other, 0.0);

        assertEquals(original.getOpenness(), result.getOpenness());
        assertEquals(original.getConscientiousness(), result.getConscientiousness());
        assertEquals(original, result,
            "Zero weight should return original unchanged");
    }

    @Test
    @DisplayName("blend with weight 1.0 should return other traits")
    void testBlendWeightOne() {
        PersonalityTraits original = new PersonalityTraits(80, 70, 60, 50, 40);
        PersonalityTraits other = new PersonalityTraits(20, 30, 40, 50, 60);

        PersonalityTraits result = original.blend(other, 1.0);

        assertEquals(other.getOpenness(), result.getOpenness());
        assertEquals(other.getConscientiousness(), result.getConscientiousness());
        assertEquals(other, result,
            "Weight 1.0 should return other unchanged");
    }

    @Test
    @DisplayName("blend with weight 0.5 should average traits")
    void testBlendWeightHalf() {
        PersonalityTraits original = new PersonalityTraits(80, 20, 60, 40, 80);
        PersonalityTraits other = new PersonalityTraits(20, 80, 40, 60, 20);

        PersonalityTraits result = original.blend(other, 0.5);

        assertEquals(50, result.getOpenness(), "Openness should average");
        assertEquals(50, result.getConscientiousness(), "Conscientiousness should average");
        assertEquals(50, result.getExtraversion(), "Extraversion should average");
        assertEquals(50, result.getAgreeableness(), "Agreeableness should average");
        assertEquals(50, result.getNeuroticism(), "Neuroticism should average");
    }

    @Test
    @DisplayName("blend should round to nearest integer")
    void testBlendRounding() {
        PersonalityTraits original = new PersonalityTraits(80, 70, 60, 50, 40);
        PersonalityTraits other = new PersonalityTraits(21, 31, 41, 51, 61);

        PersonalityTraits result = original.blend(other, 0.5);

        // (80 + 21) / 2 = 50.5 -> 51
        assertEquals(51, result.getOpenness());
        // (70 + 31) / 2 = 50.5 -> 51
        assertEquals(51, result.getConscientiousness());
    }

    @Test
    @DisplayName("blend should reject negative weight")
    void testBlendRejectsNegativeWeight() {
        PersonalityTraits traits = new PersonalityTraits(50, 50, 50, 50, 50);
        PersonalityTraits other = new PersonalityTraits(50, 50, 50, 50, 50);

        assertThrows(IllegalArgumentException.class,
            () -> traits.blend(other, -0.1));
    }

    @Test
    @DisplayName("blend should reject weight > 1.0")
    void testBlendRejectsHighWeight() {
        PersonalityTraits traits = new PersonalityTraits(50, 50, 50, 50, 50);
        PersonalityTraits other = new PersonalityTraits(50, 50, 50, 50, 50);

        assertThrows(IllegalArgumentException.class,
            () -> traits.blend(other, 1.1));
    }

    @Test
    @DisplayName("blend with weight 0.25 should favor original")
    void testBlendFavorsOriginal() {
        PersonalityTraits original = new PersonalityTraits(100, 0, 50, 50, 50);
        PersonalityTraits other = new PersonalityTraits(0, 100, 50, 50, 50);

        PersonalityTraits result = original.blend(other, 0.25);

        // 75% original + 25% other
        assertEquals(75, result.getOpenness());
        assertEquals(25, result.getConscientiousness());
    }

    // ========== toDetailedString Tests ==========

    @Test
    @DisplayName("toDetailedString should format all traits")
    void testToDetailedString() {
        PersonalityTraits traits = new PersonalityTraits(15, 35, 55, 75, 95);

        String detailed = traits.toDetailedString();

        assertTrue(detailed.contains("PersonalityTraits:"));
        assertTrue(detailed.contains("Openness: 15 (Very Low)"));
        assertTrue(detailed.contains("Conscientiousness: 35 (Low)"));
        assertTrue(detailed.contains("Extraversion: 55 (Average)"));
        assertTrue(detailed.contains("Agreeableness: 75 (High)"));
        assertTrue(detailed.contains("Neuroticism: 95 (Very High)"));
    }

    // ========== toString Tests ==========

    @Test
    @DisplayName("toString should format compactly")
    void testToString() {
        PersonalityTraits traits = new PersonalityTraits(50, 60, 70, 80, 90);

        String result = traits.toString();

        assertEquals("O:50 C:60 E:70 A:80 N:90", result);
    }

    // ========== equals Tests ==========

    @Test
    @DisplayName("equals should return true for same traits")
    void testEqualsSameTraits() {
        PersonalityTraits traits1 = new PersonalityTraits(50, 60, 70, 80, 90);
        PersonalityTraits traits2 = new PersonalityTraits(50, 60, 70, 80, 90);

        assertEquals(traits1, traits2);
        assertEquals(traits2, traits1);
    }

    @Test
    @DisplayName("equals should return false for different traits")
    void testEqualsDifferentTraits() {
        PersonalityTraits traits1 = new PersonalityTraits(50, 60, 70, 80, 90);
        PersonalityTraits traits2 = new PersonalityTraits(51, 60, 70, 80, 90);

        assertNotEquals(traits1, traits2);
    }

    @Test
    @DisplayName("equals should return false for null")
    void testEqualsNull() {
        PersonalityTraits traits = new PersonalityTraits(50, 60, 70, 80, 90);

        assertNotEquals(null, traits);
    }

    @Test
    @DisplayName("equals should return false for different class")
    void testEqualsDifferentClass() {
        PersonalityTraits traits = new PersonalityTraits(50, 60, 70, 80, 90);

        assertNotEquals("not traits", traits);
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void testEqualsSameInstance() {
        PersonalityTraits traits = new PersonalityTraits(50, 60, 70, 80, 90);

        assertEquals(traits, traits);
    }

    // ========== hashCode Tests ==========

    @Test
    @DisplayName("hashCode should be consistent")
    void testHashCodeConsistent() {
        PersonalityTraits traits = new PersonalityTraits(50, 60, 70, 80, 90);

        int hash1 = traits.hashCode();
        int hash2 = traits.hashCode();

        assertEquals(hash1, hash2,
            "hashCode should be consistent");
    }

    @Test
    @DisplayName("hashCode should be equal for equal objects")
    void testHashCodeEqualObjects() {
        PersonalityTraits traits1 = new PersonalityTraits(50, 60, 70, 80, 90);
        PersonalityTraits traits2 = new PersonalityTraits(50, 60, 70, 80, 90);

        assertEquals(traits1.hashCode(), traits2.hashCode(),
            "Equal objects should have equal hashCode");
    }

    @Test
    @DisplayName("hashCode should differ for different traits")
    void testHashCodeDifferentTraits() {
        PersonalityTraits traits1 = new PersonalityTraits(50, 60, 70, 80, 90);
        PersonalityTraits traits2 = new PersonalityTraits(50, 60, 70, 80, 91);

        assertNotEquals(traits1.hashCode(), traits2.hashCode(),
            "Different traits should likely have different hashCode");
    }

    // ========== Builder Tests ==========

    @Test
    @DisplayName("Builder should construct valid traits")
    void testBuilderConstructsValidTraits() {
        PersonalityTraits traits = PersonalityTraits.builder()
            .openness(30)
            .conscientiousness(40)
            .extraversion(50)
            .agreeableness(60)
            .neuroticism(70)
            .build();

        assertEquals(30, traits.getOpenness());
        assertEquals(40, traits.getConscientiousness());
        assertEquals(50, traits.getExtraversion());
        assertEquals(60, traits.getAgreeableness());
        assertEquals(70, traits.getNeuroticism());
    }

    @Test
    @DisplayName("Builder should validate traits on build")
    void testBuilderValidatesOnBuild() {
        PersonalityTraits.Builder builder = PersonalityTraits.builder()
            .openness(30)
            .conscientiousness(40)
            .extraversion(50)
            .agreeableness(60)
            // neuroticism not set
            ;

        assertThrows(IllegalStateException.class,
            builder::build,
            "Builder should throw when trait is missing");
    }

    @Test
    @DisplayName("Builder should be reusable")
    void testBuilderReusable() {
        PersonalityTraits.Builder builder = PersonalityTraits.builder();

        PersonalityTraits traits1 = builder
            .openness(30)
            .conscientiousness(40)
            .extraversion(50)
            .agreeableness(60)
            .neuroticism(70)
            .build();

        // Builder can be reused with new values
        PersonalityTraits traits2 = builder
            .openness(80)
            .conscientiousness(70)
            .extraversion(60)
            .agreeableness(50)
            .neuroticism(40)
            .build();

        assertEquals(30, traits1.getOpenness());
        assertEquals(80, traits2.getOpenness());
    }

    @Test
    @DisplayName("Builder should validate invalid values")
    void testBuilderValidatesInvalidValues() {
        assertThrows(IllegalArgumentException.class,
            () -> PersonalityTraits.builder()
                .openness(101)
                .conscientiousness(40)
                .extraversion(50)
                .agreeableness(60)
                .neuroticism(70)
                .build());
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Full workflow: create, blend, serialize")
    void testFullWorkflow() {
        // Create original traits
        PersonalityTraits original = PersonalityTraits.builder()
            .openness(80)
            .conscientiousness(70)
            .extraversion(60)
            .agreeableness(50)
            .neuroticism(40)
            .build();

        // Blend with another
        PersonalityTraits other = new PersonalityTraits(20, 30, 40, 50, 60);
        PersonalityTraits blended = original.blend(other, 0.3);

        // Verify blending
        assertEquals(62, blended.getOpenness()); // 80*0.7 + 20*0.3 = 62

        // Verify string representations
        assertNotNull(blended.toString());
        assertNotNull(blended.toDetailedString());

        // Verify equality works
        PersonalityTraits copy = new PersonalityTraits(
            blended.getOpenness(),
            blended.getConscientiousness(),
            blended.getExtraversion(),
            blended.getAgreeableness(),
            blended.getNeuroticism()
        );
        assertEquals(blended, copy);
        assertEquals(blended.hashCode(), copy.hashCode());
    }

    @Test
    @DisplayName("Edge case: all minimum traits")
    void testAllMinimumTraits() {
        PersonalityTraits traits = new PersonalityTraits(0, 0, 0, 0, 0);

        for (int i = 0; i < 5; i++) {
            assertEquals("Very Low",
                PersonalityTraits.getTraitLevelDescription(
                    new int[]{traits.getOpenness(), traits.getConscientiousness(),
                        traits.getExtraversion(), traits.getAgreeableness(),
                        traits.getNeuroticism()}[i]
                ));
        }
    }

    @Test
    @DisplayName("Edge case: all maximum traits")
    void testAllMaximumTraits() {
        PersonalityTraits traits = new PersonalityTraits(100, 100, 100, 100, 100);

        for (int i = 0; i < 5; i++) {
            assertEquals("Very High",
                PersonalityTraits.getTraitLevelDescription(
                    new int[]{traits.getOpenness(), traits.getConscientiousness(),
                        traits.getExtraversion(), traits.getAgreeableness(),
                        traits.getNeuroticism()}[i]
                ));
        }
    }
}
