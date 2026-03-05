package com.minewright.personality;

import com.minewright.memory.CompanionMemory;
import com.minewright.memory.PersonalitySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the personality system.
 *
 * <p>These tests verify that:</p>
 * <ul>
 *   <li>Personality traits are validated and clamped correctly</li>
 *   <li>Personality blending produces expected results</li>
 *   <li>Archetype configurations are applied correctly</li>
 *   <li>Mood tracking and speech patterns work as expected</li>
 *   <li>Verbal tics and catchphrases are managed properly</li>
 *   <li>All edge cases and error conditions are handled</li>
 * </ul>
 *
 * @since 1.4.0
 */
@DisplayName("Personality System Tests")
class PersonalitySystemTest {

    private PersonalitySystem.PersonalityProfile profile;
    private PersonalitySystem personalitySystem;

    @BeforeEach
    void setUp() {
        personalitySystem = new PersonalitySystem();
        profile = personalitySystem.getPersonality();
    }

    // ========================================================================
    // PersonalityTraits Tests
    // ========================================================================

    @Test
    @DisplayName("PersonalityTraits should accept valid trait values")
    void testPersonalityTraitsValidValues() {
        PersonalityTraits traits = new PersonalityTraits(50, 60, 70, 80, 90);

        assertEquals(50, traits.getOpenness());
        assertEquals(60, traits.getConscientiousness());
        assertEquals(70, traits.getExtraversion());
        assertEquals(80, traits.getAgreeableness());
        assertEquals(90, traits.getNeuroticism());
    }

    @Test
    @DisplayName("PersonalityTraits should reject negative values")
    void testPersonalityTraitsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PersonalityTraits(-1, 50, 50, 50, 50);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PersonalityTraits(50, -1, 50, 50, 50);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PersonalityTraits(50, 50, -1, 50, 50);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PersonalityTraits(50, 50, 50, -1, 50);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PersonalityTraits(50, 50, 50, 50, -1);
        });
    }

    @Test
    @DisplayName("PersonalityTraits should reject values greater than 100")
    void testPersonalityTraitsValuesGreaterThan100() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PersonalityTraits(101, 50, 50, 50, 50);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PersonalityTraits(50, 101, 50, 50, 50);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PersonalityTraits(50, 50, 101, 50, 50);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PersonalityTraits(50, 50, 50, 101, 50);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PersonalityTraits(50, 50, 50, 50, 101);
        });
    }

    @Test
    @DisplayName("PersonalityTraits should accept boundary values (0 and 100)")
    void testPersonalityTraitsBoundaryValues() {
        PersonalityTraits traits = new PersonalityTraits(0, 100, 50, 0, 100);

        assertEquals(0, traits.getOpenness());
        assertEquals(100, traits.getConscientiousness());
        assertEquals(50, traits.getExtraversion());
        assertEquals(0, traits.getAgreeableness());
        assertEquals(100, traits.getNeuroticism());
    }

    @Test
    @DisplayName("getTraitLevelDescription should return correct descriptions")
    void testGetTraitLevelDescription() {
        assertEquals("Very Low", PersonalityTraits.getTraitLevelDescription(0));
        assertEquals("Very Low", PersonalityTraits.getTraitLevelDescription(20));
        assertEquals("Low", PersonalityTraits.getTraitLevelDescription(21));
        assertEquals("Low", PersonalityTraits.getTraitLevelDescription(40));
        assertEquals("Average", PersonalityTraits.getTraitLevelDescription(41));
        assertEquals("Average", PersonalityTraits.getTraitLevelDescription(60));
        assertEquals("High", PersonalityTraits.getTraitLevelDescription(61));
        assertEquals("High", PersonalityTraits.getTraitLevelDescription(80));
        assertEquals("Very High", PersonalityTraits.getTraitLevelDescription(81));
        assertEquals("Very High", PersonalityTraits.getTraitLevelDescription(100));
    }

    @Test
    @DisplayName("PersonalityTraits blend with weight 0.0 should return original traits")
    void testPersonalityTraitsBlendWeightZero() {
        PersonalityTraits original = new PersonalityTraits(50, 60, 70, 80, 90);
        PersonalityTraits other = new PersonalityTraits(10, 20, 30, 40, 50);

        PersonalityTraits blended = original.blend(other, 0.0);

        assertEquals(original.getOpenness(), blended.getOpenness());
        assertEquals(original.getConscientiousness(), blended.getConscientiousness());
        assertEquals(original.getExtraversion(), blended.getExtraversion());
        assertEquals(original.getAgreeableness(), blended.getAgreeableness());
        assertEquals(original.getNeuroticism(), blended.getNeuroticism());
    }

    @Test
    @DisplayName("PersonalityTraits blend with weight 1.0 should return other traits")
    void testPersonalityTraitsBlendWeightOne() {
        PersonalityTraits original = new PersonalityTraits(50, 60, 70, 80, 90);
        PersonalityTraits other = new PersonalityTraits(10, 20, 30, 40, 50);

        PersonalityTraits blended = original.blend(other, 1.0);

        assertEquals(other.getOpenness(), blended.getOpenness());
        assertEquals(other.getConscientiousness(), blended.getConscientiousness());
        assertEquals(other.getExtraversion(), blended.getExtraversion());
        assertEquals(other.getAgreeableness(), blended.getAgreeableness());
        assertEquals(other.getNeuroticism(), blended.getNeuroticism());
    }

    @Test
    @DisplayName("PersonalityTraits blend with weight 0.5 should return average")
    void testPersonalityTraitsBlendWeightHalf() {
        PersonalityTraits original = new PersonalityTraits(100, 100, 100, 100, 100);
        PersonalityTraits other = new PersonalityTraits(0, 0, 0, 0, 0);

        PersonalityTraits blended = original.blend(other, 0.5);

        assertEquals(50, blended.getOpenness());
        assertEquals(50, blended.getConscientiousness());
        assertEquals(50, blended.getExtraversion());
        assertEquals(50, blended.getAgreeableness());
        assertEquals(50, blended.getNeuroticism());
    }

    @Test
    @DisplayName("PersonalityTraits blend should reject invalid weight")
    void testPersonalityTraitsBlendInvalidWeight() {
        PersonalityTraits traits = new PersonalityTraits(50, 50, 50, 50, 50);
        PersonalityTraits other = new PersonalityTraits(50, 50, 50, 50, 50);

        assertThrows(IllegalArgumentException.class, () -> {
            traits.blend(other, -0.1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            traits.blend(other, 1.1);
        });
    }

    @Test
    @DisplayName("PersonalityTraits blend should round correctly")
    void testPersonalityTraitsBlendRounding() {
        PersonalityTraits original = new PersonalityTraits(50, 50, 50, 50, 50);
        PersonalityTraits other = new PersonalityTraits(33, 33, 33, 33, 33);

        // 50 * 0.7 + 33 * 0.3 = 35 + 9.9 = 44.9 -> rounds to 45
        PersonalityTraits blended = original.blend(other, 0.3);

        assertEquals(45, blended.getOpenness());
        assertEquals(45, blended.getConscientiousness());
        assertEquals(45, blended.getExtraversion());
        assertEquals(45, blended.getAgreeableness());
        assertEquals(45, blended.getNeuroticism());
    }

    @Test
    @DisplayName("PersonalityTraits toDetailedString should contain all traits")
    void testPersonalityTraitsToDetailedString() {
        PersonalityTraits traits = new PersonalityTraits(50, 60, 70, 80, 90);
        String detailed = traits.toDetailedString();

        assertTrue(detailed.contains("Openness: 50"));
        assertTrue(detailed.contains("Conscientiousness: 60"));
        assertTrue(detailed.contains("Extraversion: 70"));
        assertTrue(detailed.contains("Agreeableness: 80"));
        assertTrue(detailed.contains("Neuroticism: 90"));
        assertTrue(detailed.contains("Average"));
        assertTrue(detailed.contains("High"));
        assertTrue(detailed.contains("Very High"));
    }

    @Test
    @DisplayName("PersonalityTraits toString should use compact format")
    void testPersonalityTraitsToString() {
        PersonalityTraits traits = new PersonalityTraits(50, 60, 70, 80, 90);
        String str = traits.toString();

        assertEquals("O:50 C:60 E:70 A:80 N:90", str);
    }

    @Test
    @DisplayName("PersonalityTraits equals should work correctly")
    void testPersonalityTraitsEquals() {
        PersonalityTraits traits1 = new PersonalityTraits(50, 60, 70, 80, 90);
        PersonalityTraits traits2 = new PersonalityTraits(50, 60, 70, 80, 90);
        PersonalityTraits traits3 = new PersonalityTraits(50, 60, 70, 80, 89);

        assertEquals(traits1, traits2);
        assertNotEquals(traits1, traits3);
        assertNotEquals(traits1, null);
        assertNotEquals(traits1, "not a trait");
    }

    @Test
    @DisplayName("PersonalityTraits hashCode should be consistent")
    void testPersonalityTraitsHashCode() {
        PersonalityTraits traits1 = new PersonalityTraits(50, 60, 70, 80, 90);
        PersonalityTraits traits2 = new PersonalityTraits(50, 60, 70, 80, 90);

        assertEquals(traits1.hashCode(), traits2.hashCode());
    }

    // ========================================================================
    // Builder Tests
    // ========================================================================

    @Test
    @DisplayName("Builder should construct valid PersonalityTraits")
    void testBuilderConstructValidTraits() {
        PersonalityTraits traits = PersonalityTraits.builder()
                .openness(50)
                .conscientiousness(60)
                .extraversion(70)
                .agreeableness(80)
                .neuroticism(90)
                .build();

        assertEquals(50, traits.getOpenness());
        assertEquals(60, traits.getConscientiousness());
        assertEquals(70, traits.getExtraversion());
        assertEquals(80, traits.getAgreeableness());
        assertEquals(90, traits.getNeuroticism());
    }

    @Test
    @DisplayName("Builder should throw when building with missing values")
    void testBuilderMissingValues() {
        assertThrows(IllegalStateException.class, () -> {
            PersonalityTraits.builder()
                    .openness(50)
                    .conscientiousness(60)
                    .extraversion(70)
                    .agreeableness(80)
                    // neuroticism missing
                    .build();
        });

        assertThrows(IllegalStateException.class, () -> {
            PersonalityTraits.builder()
                    .openness(50)
                    .conscientiousness(60)
                    // extraversion, agreeableness, neuroticism missing
                    .build();
        });
    }

    // ========================================================================
    // PersonalityProfile Tests
    // ========================================================================

    @Test
    @DisplayName("Profile should have default trait values")
    void testProfileDefaultValues() {
        assertEquals(70, profile.getOpenness());
        assertEquals(80, profile.getConscientiousness());
        assertEquals(60, profile.getExtraversion());
        assertEquals(75, profile.getAgreeableness());
        assertEquals(30, profile.getNeuroticism());
    }

    @Test
    @DisplayName("Profile should clamp trait values to valid range")
    void testProfileClampValues() {
        profile.setOpenness(150);
        assertEquals(100, profile.getOpenness());

        profile.setOpenness(-50);
        assertEquals(0, profile.getOpenness());

        profile.setConscientiousness(200);
        assertEquals(100, profile.getConscientiousness());

        profile.setExtraversion(-100);
        assertEquals(0, profile.getExtraversion());
    }

    @Test
    @DisplayName("Profile should set custom traits correctly")
    void testProfileSetCustomTraits() {
        profile.setHumor(90);
        profile.setEncouragement(95);
        profile.setFormality(10);

        assertEquals(90, profile.getHumor());
        assertEquals(95, profile.getEncouragement());
        assertEquals(10, profile.getFormality());
    }

    @Test
    @DisplayName("Profile should clamp custom traits to valid range")
    void testProfileClampCustomTraits() {
        profile.setHumor(150);
        assertEquals(100, profile.getHumor());

        profile.setEncouragement(-50);
        assertEquals(0, profile.getEncouragement());

        profile.setFormality(200);
        assertEquals(100, profile.getFormality());
    }

    @Test
    @DisplayName("Profile should manage catchphrases correctly")
    void testProfileCatchphrases() {
        List<String> initialCatchphrases = profile.getCatchphrases();
        assertFalse(initialCatchphrases.isEmpty());
        assertTrue(initialCatchphrases.contains("Right then,"));
        assertTrue(initialCatchphrases.contains("Let's get to work!"));

        // Add catchphrase
        profile.addCatchphrase("New catchphrase!");
        assertTrue(profile.getCatchphrases().contains("New catchphrase!"));

        // Remove catchphrase
        profile.removeCatchphrase("New catchphrase!");
        assertFalse(profile.getCatchphrases().contains("New catchphrase!"));

        // Set catchphrases
        List<String> newCatchphrases = List.of("Phrase 1", "Phrase 2", "Phrase 3");
        profile.setCatchphrases(new ArrayList<>(newCatchphrases));
        assertEquals(3, profile.getCatchphrases().size());
        assertTrue(profile.getCatchphrases().contains("Phrase 1"));
    }

    @Test
    @DisplayName("Profile should reject null and empty catchphrases")
    void testProfileInvalidCatchphrases() {
        int originalSize = profile.getCatchphrases().size();

        profile.addCatchphrase(null);
        profile.addCatchphrase("");
        profile.addCatchphrase("   ");

        assertEquals(originalSize, profile.getCatchphrases().size());
    }

    @Test
    @DisplayName("Profile should manage verbal tics correctly")
    void testProfileVerbalTics() {
        List<String> initialTics = profile.getVerbalTics();
        assertFalse(initialTics.isEmpty());
        assertTrue(initialTics.contains("Well,"));

        // Add verbal tic
        profile.addVerbalTic("Um,");
        assertTrue(profile.getVerbalTics().contains("Um,"));

        // Remove verbal tic
        profile.removeVerbalTic("Um,");
        assertFalse(profile.getVerbalTics().contains("Um,"));

        // Set verbal tics
        List<String> newTics = List.of("Tic 1", "Tic 2");
        profile.setVerbalTics(new ArrayList<>(newTics));
        assertEquals(2, profile.getVerbalTics().size());
    }

    @Test
    @DisplayName("Profile should reject null and empty verbal tics")
    void testProfileInvalidVerbalTics() {
        int originalSize = profile.getVerbalTics().size();

        profile.addVerbalTic(null);
        profile.addVerbalTic("");
        profile.addVerbalTic("   ");

        assertEquals(originalSize, profile.getVerbalTics().size());
    }

    @Test
    @DisplayName("Profile should track tic usage")
    void testProfileTicUsageTracking() {
        profile.setVerbalTics(new ArrayList<>(List.of("Tic1", "Tic2", "Tic3")));

        // Use tics multiple times
        profile.getRandomVerbalTic();
        profile.getRandomVerbalTic();
        profile.getRandomVerbalTic();

        Map<String, Integer> usageCount = profile.getTicUsageCount();
        assertTrue(usageCount.size() > 0);
    }

    @Test
    @DisplayName("Profile should return empty tic when list is empty")
    void testProfileEmptyTicList() {
        profile.setVerbalTics(new ArrayList<>());

        String tic = profile.getRandomVerbalTic();
        assertEquals("", tic);

        boolean shouldUse = profile.shouldUseVerbalTic();
        assertFalse(shouldUse);
    }

    @Test
    @DisplayName("Profile should manage mood correctly")
    void testProfileMood() {
        profile.setMood("excited");
        assertEquals("excited", profile.getMood());

        profile.setMood(null);
        assertEquals("cheerful", profile.getMood()); // default

        profile.setMood("");
        assertEquals("cheerful", profile.getMood()); // default
    }

    @Test
    @DisplayName("Profile should manage work style correctly")
    void testProfileWorkStyle() {
        profile.setWorkStyle("efficient");
        assertEquals("efficient", profile.getWorkStyle());

        profile.setWorkStyle(null);
        assertEquals("methodical", profile.getWorkStyle()); // default
    }

    @Test
    @DisplayName("Profile should manage favorite block correctly")
    void testProfileFavoriteBlock() {
        profile.setFavoriteBlock("diamond_block");
        assertEquals("diamond_block", profile.getFavoriteBlock());

        profile.setFavoriteBlock(null);
        assertEquals("cobblestone", profile.getFavoriteBlock()); // default
    }

    @Test
    @DisplayName("Profile should manage archetype name correctly")
    void testProfileArchetypeName() {
        profile.setArchetypeName("THE_OPTIMIST");
        assertEquals("THE_OPTIMIST", profile.getArchetypeName());

        profile.setArchetypeName(null);
        assertEquals("THE_FOREMAN", profile.getArchetypeName()); // default
    }

    // ========================================================================
    // ForemanArchetypeConfig Tests
    // ========================================================================

    @Test
    @DisplayName("All foreman archetypes should have valid configurations")
    void testAllForemanArchetypesValid() {
        for (ForemanArchetypeConfig.ForemanArchetype archetype : ForemanArchetypeConfig.ALL_ARCHETYPES) {
            assertNotNull(archetype.getName());
            assertNotNull(archetype.getTraits());
            assertTrue(archetype.getFormality() >= 0 && archetype.getFormality() <= 100);
            assertTrue(archetype.getHumor() >= 0 && archetype.getHumor() <= 100);
            assertTrue(archetype.getEncouragement() >= 0 && archetype.getEncouragement() <= 100);
            assertFalse(archetype.getCatchphrases().isEmpty());
        }
    }

    @Test
    @DisplayName("Foreman archetype byName should find correct archetypes")
    void testForemanArchetypeByName() {
        ForemanArchetypeConfig.ForemanArchetype foreman = ForemanArchetypeConfig.byName("THE_FOREMAN");
        assertNotNull(foreman);
        assertEquals("The Foreman", foreman.getName());

        ForemanArchetypeConfig.ForemanArchetype optimist = ForemanArchetypeConfig.byName("The Optimist");
        assertNotNull(optimist);
        assertEquals("The Optimist", optimist.getName());

        ForemanArchetypeConfig.ForemanArchetype notFound = ForemanArchetypeConfig.byName("Nonexistent");
        assertNull(notFound);
    }

    @Test
    @DisplayName("Foreman archetype byName should handle null input")
    void testForemanArchetypeByNameNull() {
        ForemanArchetypeConfig.ForemanArchetype result = ForemanArchetypeConfig.byName(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Foreman archetype random should return valid archetype")
    void testForemanArchetypeRandom() {
        ForemanArchetypeConfig.ForemanArchetype archetype = ForemanArchetypeConfig.random();
        assertNotNull(archetype);
        assertTrue(ForemanArchetypeConfig.ALL_ARCHETYPES.contains(archetype));
    }

    @Test
    @DisplayName("Foreman archetype should apply to profile correctly")
    void testForemanArchetypeApplyToProfile() {
        PersonalitySystem.PersonalityProfile testProfile = personalitySystem.getPersonality();

        ForemanArchetypeConfig.ForemanArchetype optimist = ForemanArchetypeConfig.THE_OPTIMIST;
        optimist.applyTo(testProfile);

        assertEquals(optimist.getTraits().getOpenness(), testProfile.getOpenness());
        assertEquals(optimist.getFormality(), testProfile.getFormality());
        assertEquals(optimist.getHumor(), testProfile.getHumor());
        assertEquals(optimist.getEncouragement(), testProfile.getEncouragement());
        assertEquals(optimist.getName(), testProfile.getArchetypeName());
    }

    @Test
    @DisplayName("Foreman archetype toPromptContext should contain all information")
    void testForemanArchetypeToPromptContext() {
        ForemanArchetypeConfig.ForemanArchetype foreman = ForemanArchetypeConfig.THE_FOREMAN;
        String context = foreman.toPromptContext();

        assertTrue(context.contains(foreman.getName()));
        assertTrue(context.contains("PERSONALITY TRAITS"));
        assertTrue(context.contains("COMMUNICATION STYLE"));
        assertTrue(context.contains("SIGNATURE CATCHPHRASES"));
        assertTrue(context.contains("Openness:"));
        assertTrue(context.contains("Formality:"));
    }

    // ========================================================================
    // ArtificerArchetype Tests
    // ========================================================================

    @Test
    @DisplayName("All artificer archetypes should have valid configurations")
    void testAllArtificerArchetypesValid() {
        for (ArtificerArchetype archetype : ArtificerArchetype.values()) {
            assertNotNull(archetype.getName());
            assertNotNull(archetype.getTitle());
            assertNotNull(archetype.getTraits());
            assertTrue(archetype.getFormality() >= 0 && archetype.getFormality() <= 100);
            assertTrue(archetype.getHumor() >= 0 && archetype.getHumor() <= 100);
            assertTrue(archetype.getEncouragement() >= 0 && archetype.getEncouragement() <= 100);
            assertFalse(archetype.getCatchphrases().isEmpty());
        }
    }

    @Test
    @DisplayName("Artificer archetype byName should find correct archetypes")
    void testArtificerArchetypeByName() {
        ArtificerArchetype lucius = ArtificerArchetype.byName("LUCIUS_FOX");
        assertEquals(ArtificerArchetype.LUCIUS_FOX, lucius);

        ArtificerArchetype phineas = ArtificerArchetype.byName("Phineas");
        assertEquals(ArtificerArchetype.PHINEAS, phineas);

        ArtificerArchetype notFound = ArtificerArchetype.byName("Nonexistent");
        assertNull(notFound);
    }

    @Test
    @DisplayName("Artificer archetype byName should handle null input")
    void testArtificerArchetypeByNameNull() {
        ArtificerArchetype result = ArtificerArchetype.byName(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Artificer archetype random should return valid archetype")
    void testArtificerArchetypeRandom() {
        ArtificerArchetype archetype = ArtificerArchetype.random();
        assertNotNull(archetype);
    }

    @Test
    @DisplayName("Artificer archetype getRandomCatchphrase should return valid phrase")
    void testArtificerArchetypeGetRandomCatchphrase() {
        ArtificerArchetype phineas = ArtificerArchetype.PHINEAS;
        String phrase = phineas.getRandomCatchphrase();

        assertNotNull(phrase);
        assertFalse(phrase.isEmpty());
        assertTrue(phineas.getCatchphrases().contains(phrase));
    }

    @Test
    @DisplayName("Artificer archetype toPromptContext should be complete")
    void testArtificerArchetypeToPromptContext() {
        ArtificerArchetype lucius = ArtificerArchetype.LUCIUS_FOX;
        String context = lucius.toPromptContext();

        assertTrue(context.contains("LUCIUS_FOX"));
        assertTrue(context.contains("High-Tech Patron"));
        assertTrue(context.contains("PERSONALITY TRAITS"));
        assertTrue(context.contains("COMMUNICATION STYLE"));
        assertTrue(context.contains("SIGNATURE CATCHPHRASES"));
        assertTrue(context.contains("GREETING STYLE"));
        assertTrue(context.contains("CELEBRATION STYLE"));
    }

    @Test
    @DisplayName("Artificer archetype getGreetingStyle should return unique styles")
    void testArtificerArchetypeGetGreetingStyle() {
        String luciusGreeting = ArtificerArchetype.LUCIUS_FOX.getGreetingStyle();
        String phineasGreeting = ArtificerArchetype.PHINEAS.getGreetingStyle();

        assertNotNull(luciusGreeting);
        assertNotNull(phineasGreeting);
        assertNotEquals(luciusGreeting, phineasGreeting);
    }

    @Test
    @DisplayName("Artificer archetype getCelebrationStyle should return unique styles")
    void testArtificerArchetypeGetCelebrationStyle() {
        String luciusCelebration = ArtificerArchetype.LUCIUS_FOX.getCelebrationStyle();
        String phineasCelebration = ArtificerArchetype.PHINEAS.getCelebrationStyle();

        assertNotNull(luciusCelebration);
        assertNotNull(phineasCelebration);
        assertNotEquals(luciusCelebration, phineasCelebration);
    }

    // ========================================================================
    // BlendResult Tests
    // ========================================================================

    @Test
    @DisplayName("ArtificerArchetype blend should work correctly")
    void testArtificerArchetypeBlend() {
        ArtificerArchetype lucius = ArtificerArchetype.LUCIUS_FOX;
        ArtificerArchetype phineas = ArtificerArchetype.PHINEAS;

        BlendResult result = lucius.blend(phineas, 0.5);

        assertNotNull(result);
        assertEquals(lucius, result.getPrimary());
        assertEquals(phineas, result.getSecondary());
        assertEquals(0.5, result.getBlendWeight());
        assertNotNull(result.getBlendedTraits());
    }

    @Test
    @DisplayName("ArtificerArchetype blend should reject invalid weight")
    void testArtificerArchetypeBlendInvalidWeight() {
        ArtificerArchetype lucius = ArtificerArchetype.LUCIUS_FOX;
        ArtificerArchetype phineas = ArtificerArchetype.PHINEAS;

        assertThrows(IllegalArgumentException.class, () -> {
            lucius.blend(phineas, -0.1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            lucius.blend(phineas, 1.1);
        });
    }

    @Test
    @DisplayName("BlendResult getBlendDescription should describe blends correctly")
    void testBlendResultGetBlendDescription() {
        ArtificerArchetype lucius = ArtificerArchetype.LUCIUS_FOX;
        ArtificerArchetype phineas = ArtificerArchetype.PHINEAS;

        BlendResult purePrimary = lucius.blend(phineas, 0.0);
        assertTrue(purePrimary.getBlendDescription().contains("Pure"));

        BlendResult pureSecondary = lucius.blend(phineas, 1.0);
        assertTrue(pureSecondary.getBlendDescription().contains("Pure"));

        BlendResult hintOfSecondary = lucius.blend(phineas, 0.25);
        assertTrue(hintOfSecondary.getBlendDescription().contains("hints of"));

        BlendResult balanced = lucius.blend(phineas, 0.5);
        assertTrue(balanced.getBlendDescription().contains("Balanced"));

        BlendResult mostlySecondary = lucius.blend(phineas, 0.75);
        assertTrue(mostlySecondary.getBlendDescription().contains("foundation"));
    }

    @Test
    @DisplayName("BlendResult builder should construct valid result")
    void testBlendResultBuilder() {
        PersonalityTraits traits = new PersonalityTraits(50, 50, 50, 50, 50);

        BlendResult result = BlendResult.builder()
                .primary(ArtificerArchetype.LUCIUS_FOX)
                .secondary(ArtificerArchetype.PHINEAS)
                .blendWeight(0.5)
                .blendedTraits(traits)
                .blendedFormality(50)
                .blendedHumor(50)
                .blendedEncouragement(50)
                .build();

        assertNotNull(result);
        assertEquals(ArtificerArchetype.LUCIUS_FOX, result.getPrimary());
        assertEquals(ArtificerArchetype.PHINEAS, result.getSecondary());
        assertEquals(0.5, result.getBlendWeight());
    }

    @Test
    @DisplayName("BlendResult builder should throw when missing values")
    void testBlendResultBuilderMissingValues() {
        assertThrows(IllegalStateException.class, () -> {
            BlendResult.builder()
                    .primary(ArtificerArchetype.LUCIUS_FOX)
                    .secondary(ArtificerArchetype.PHINEAS)
                    .blendWeight(0.5)
                    // Missing blended traits and communication values
                    .build();
        });
    }

    // ========================================================================
    // Mood and Speech Pattern Tests
    // ========================================================================

    @Test
    @DisplayName("ParseMood should handle valid mood strings")
    void testParseMoodValid() {
        assertEquals(CompanionMemory.Mood.CHEERFUL, PersonalitySystem.parseMood("cheerful"));
        assertEquals(CompanionMemory.Mood.CHEERFUL, PersonalitySystem.parseMood("CHEERFUL"));
        assertEquals(CompanionMemory.Mood.SERIOUS, PersonalitySystem.parseMood("serious"));
        assertEquals(CompanionMemory.Mood.FOCUSED, PersonalitySystem.parseMood("focused"));
        assertEquals(CompanionMemory.Mood.EXCITED, PersonalitySystem.parseMood("excited"));
        assertEquals(CompanionMemory.Mood.CALM, PersonalitySystem.parseMood("calm"));
        assertEquals(CompanionMemory.Mood.PLAYFUL, PersonalitySystem.parseMood("playful"));
        assertEquals(CompanionMemory.Mood.TIRED, PersonalitySystem.parseMood("tired"));
        assertEquals(CompanionMemory.Mood.HAPPY, PersonalitySystem.parseMood("happy"));
    }

    @Test
    @DisplayName("ParseMood should return default for invalid input")
    void testParseMoodInvalid() {
        assertEquals(CompanionMemory.Mood.CHEERFUL, PersonalitySystem.parseMood(null));
        assertEquals(CompanionMemory.Mood.CHEERFUL, PersonalitySystem.parseMood("invalid"));
        assertEquals(CompanionMemory.Mood.CHEERFUL, PersonalitySystem.parseMood(""));
    }

    @Test
    @DisplayName("Profile toPromptContext should contain all personality information")
    void testProfileToPromptContext() {
        String context = profile.toPromptContext();

        assertTrue(context.contains("Personality Traits"));
        assertTrue(context.contains("Openness:"));
        assertTrue(context.contains("Conscientiousness:"));
        assertTrue(context.contains("Extraversion:"));
        assertTrue(context.contains("Agreeableness:"));
        assertTrue(context.contains("Humor Level:"));
        assertTrue(context.contains("Formality:"));
        assertTrue(context.contains("Current mood:"));
        assertTrue(context.contains("Archetype:"));
    }

    @Test
    @DisplayName("Profile getSpeechPatternDescription should describe speech correctly")
    void testProfileGetSpeechPatternDescription() {
        profile.setExtraversion(80);
        profile.setFormality(20);
        profile.setHumor(70);
        profile.setConscientiousness(80);

        String description = profile.getSpeechPatternDescription();

        assertTrue(description.contains("enthusiastic and expressive"));
        assertTrue(description.contains("casual and relaxed"));
        assertTrue(description.contains("frequently humorous"));
        assertTrue(description.contains("methodical and precise"));
    }

    @Test
    @DisplayName("Profile getSpeechPatternDescription should return balanced for average traits")
    void testProfileGetSpeechPatternDescriptionBalanced() {
        profile.setExtraversion(50);
        profile.setFormality(50);
        profile.setHumor(50);
        profile.setConscientiousness(50);

        String description = profile.getSpeechPatternDescription();

        assertEquals("balanced and friendly", description);
    }

    // ========================================================================
    // ForemanArchetype Traits Distance Tests
    // ========================================================================

    @Test
    @DisplayName("ForemanArchetype byTraits should find closest match")
    void testForemanArchetypeByTraits() {
        // Traits very close to THE_OPTIMIST
        PersonalityTraits optimisticTraits = new PersonalityTraits(80, 70, 85, 85, 25);
        ForemanArchetypeConfig.ForemanArchetype match = ForemanArchetypeConfig.byTraits(optimisticTraits);

        assertNotNull(match);
        assertEquals("The Optimist", match.getName());
    }

    @Test
    @DisplayName("ForemanArchetype byTraits should handle exact match")
    void testForemanArchetypeByTraitsExactMatch() {
        ForemanArchetypeConfig.ForemanArchetype foreman = ForemanArchetypeConfig.THE_FOREMAN;
        ForemanArchetypeConfig.ForemanArchetype match = ForemanArchetypeConfig.byTraits(foreman.getTraits());

        assertEquals(foreman, match);
    }

    // ========================================================================
    // Edge Cases and Error Handling
    // ========================================================================

    @Test
    @DisplayName("Profile should handle setting catchphrases to null")
    void testProfileSetCatchphrasesNull() {
        int originalSize = profile.getCatchphrases().size();
        profile.setCatchphrases(null);

        // Should keep original catchphrases
        assertEquals(originalSize, profile.getCatchphrases().size());
    }

    @Test
    @DisplayName("Profile should handle setting verbal tics to null")
    void testProfileSetVerbalTicsNull() {
        int originalSize = profile.getVerbalTics().size();
        profile.setVerbalTics(null);

        // Should keep original tics
        assertEquals(originalSize, profile.getVerbalTics().size());
    }

    @Test
    @DisplayName("Profile shouldUseVerbalTic should handle high neuroticism")
    void testProfileShouldUseVerbalTicHighNeuroticism() {
        profile.setNeuroticism(90);
        profile.setVerbalTics(new ArrayList<>(List.of("Tic1", "Tic2")));

        // With high neuroticism, should be more likely to use tics
        // Run multiple times to increase chance of at least one true
        boolean usedTic = false;
        for (int i = 0; i < 100; i++) {
            if (profile.shouldUseVerbalTic()) {
                usedTic = true;
                break;
            }
        }
        assertTrue(usedTic, "Should use verbal tics with high neuroticism");
    }

    @Test
    @DisplayName("Profile shouldUseVerbalTic should handle low neuroticism")
    void testProfileShouldUseVerbalTicLowNeuroticism() {
        profile.setNeuroticism(10);
        profile.setVerbalTics(new ArrayList<>(List.of("Tic1", "Tic2")));

        // With low neuroticism, should be less likely to use tics
        // But still possible due to base chance
        int ticCount = 0;
        for (int i = 0; i < 100; i++) {
            if (profile.shouldUseVerbalTic()) {
                ticCount++;
            }
        }
        assertTrue(ticCount < 30, "Should use fewer verbal tics with low neuroticism");
    }

    @Test
    @DisplayName("Profile getRandomVerbalTic should avoid recent tics")
    void testProfileGetRandomVerbalTicAvoidRecent() {
        profile.setVerbalTics(new ArrayList<>(List.of("Tic1", "Tic2", "Tic3", "Tic4", "Tic5")));

        List<String> tics = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tics.add(profile.getRandomVerbalTic());
        }

        // With 5 tics and avoiding recent ones, should see variety
        long uniqueTics = tics.stream().distinct().count();
        assertTrue(uniqueTics >= 3, "Should use variety of tics, not just repeat same ones");
    }

    @Test
    @DisplayName("Profile should track recent tics correctly")
    void testProfileRecentTicsTracking() {
        profile.setVerbalTics(new ArrayList<>(List.of("Tic1", "Tic2", "Tic3")));

        // Generate some tics
        profile.getRandomVerbalTic();
        profile.getRandomVerbalTic();
        profile.getRandomVerbalTic();

        List<String> recentTics = profile.getRecentTics();
        assertTrue(recentTics.size() >= 2, "Should track recent tics");
        assertTrue(recentTics.size() <= 5, "Should limit recent tics to 5");
    }
}
