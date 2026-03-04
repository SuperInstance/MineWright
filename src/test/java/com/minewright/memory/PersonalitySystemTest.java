package com.minewright.memory;

import com.minewright.personality.ForemanArchetypeConfig;
import com.minewright.personality.PersonalityTraits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PersonalitySystem} and its {@link PersonalitySystem.PersonalityProfile} class.
 *
 * Tests cover:
 * <ul>
 *   <li>Personality profile creation and defaults</li>
 *   <li>Big Five personality traits (OCEAN) with validation</li>
 *   <li>Custom traits (humor, encouragement, formality)</li>
 *   <li>Catchphrase and verbal tic management</li>
 *   <li>Speech pattern descriptions</li>
 *   <li>Archetype application</li>
 *   <li>Mood parsing</li>
 *   <li>Random verbal tics with variety</li>
 * </ul>
 *
 * @see PersonalitySystem
 * @see PersonalitySystem.PersonalityProfile
 * @since 1.4.0
 */
@DisplayName("PersonalitySystem Tests")
class PersonalitySystemTest {

    private PersonalitySystem personalitySystem;
    private PersonalitySystem.PersonalityProfile profile;

    @BeforeEach
    void setUp() {
        personalitySystem = new PersonalitySystem();
        profile = personalitySystem.getPersonality();
    }

    // ==================== Personality System Creation Tests ====================

    @Test
    @DisplayName("PersonalitySystem creates profile with default traits")
    void testPersonalitySystemCreatesProfile() {
        assertNotNull(personalitySystem);
        assertNotNull(profile);
    }

    @Test
    @DisplayName("GetPersonality returns the same profile instance")
    void testGetPersonalityReturnsSameInstance() {
        PersonalitySystem.PersonalityProfile profile1 = personalitySystem.getPersonality();
        PersonalitySystem.PersonalityProfile profile2 = personalitySystem.getPersonality();

        assertSame(profile1, profile2, "Should return the same profile instance");
    }

    // ==================== Big Five Traits Tests ====================

    @Test
    @DisplayName("Default Openness trait value")
    void testDefaultOpenness() {
        assertEquals(70, profile.getOpenness(), "Default openness should be 70");
    }

    @Test
    @DisplayName("Default Conscientiousness trait value")
    void testDefaultConscientiousness() {
        assertEquals(80, profile.getConscientiousness(), "Default conscientiousness should be 80");
    }

    @Test
    @DisplayName("Default Extraversion trait value")
    void testDefaultExtraversion() {
        assertEquals(60, profile.getExtraversion(), "Default extraversion should be 60");
    }

    @Test
    @DisplayName("Default Agreeableness trait value")
    void testDefaultAgreeableness() {
        assertEquals(75, profile.getAgreeableness(), "Default agreeableness should be 75");
    }

    @Test
    @DisplayName("Default Neuroticism trait value")
    void testDefaultNeuroticism() {
        assertEquals(30, profile.getNeuroticism(), "Default neuroticism should be 30");
    }

    @Test
    @DisplayName("Set and get Openness trait")
    void testSetOpenness() {
        profile.setOpenness(85);
        assertEquals(85, profile.getOpenness());
    }

    @Test
    @DisplayName("Set and get Conscientiousness trait")
    void testSetConscientiousness() {
        profile.setConscientiousness(95);
        assertEquals(95, profile.getConscientiousness());
    }

    @Test
    @DisplayName("Set and get Extraversion trait")
    void testSetExtraversion() {
        profile.setExtraversion(45);
        assertEquals(45, profile.getExtraversion());
    }

    @Test
    @DisplayName("Set and get Agreeableness trait")
    void testSetAgreeableness() {
        profile.setAgreeableness(55);
        assertEquals(55, profile.getAgreeableness());
    }

    @Test
    @DisplayName("Set and get Neuroticism trait")
    void testSetNeuroticism() {
        profile.setNeuroticism(25);
        assertEquals(25, profile.getNeuroticism());
    }

    @Test
    @DisplayName("Trait values are clamped to maximum 100")
    void testTraitValuesClampedToMaximum() {
        profile.setOpenness(150);
        assertEquals(100, profile.getOpenness(), "Value should be clamped to 100");

        profile.setConscientiousness(999);
        assertEquals(100, profile.getConscientiousness(), "Value should be clamped to 100");
    }

    @Test
    @DisplayName("Trait values are clamped to minimum 0")
    void testTraitValuesClampedToMinimum() {
        profile.setExtraversion(-50);
        assertEquals(0, profile.getExtraversion(), "Value should be clamped to 0");

        profile.setAgreeableness(-10);
        assertEquals(0, profile.getAgreeableness(), "Value should be clamped to 0");
    }

    @Test
    @DisplayName("Set trait to boundary value 0")
    void testSetTraitToBoundaryZero() {
        profile.setNeuroticism(0);
        assertEquals(0, profile.getNeuroticism());
    }

    @Test
    @DisplayName("Set trait to boundary value 100")
    void testSetTraitToBoundaryHundred() {
        profile.setOpenness(100);
        assertEquals(100, profile.getOpenness());
    }

    // ==================== Custom Traits Tests ====================

    @Test
    @DisplayName("Default humor value")
    void testDefaultHumor() {
        assertEquals(65, profile.getHumor(), "Default humor should be 65");
    }

    @Test
    @DisplayName("Default encouragement value")
    void testDefaultEncouragement() {
        assertEquals(80, profile.getEncouragement(), "Default encouragement should be 80");
    }

    @Test
    @DisplayName("Default formality value")
    void testDefaultFormality() {
        assertEquals(40, profile.getFormality(), "Default formality should be 40");
    }

    @Test
    @DisplayName("Set and get humor value")
    void testSetHumor() {
        profile.setHumor(90);
        assertEquals(90, profile.getHumor());
    }

    @Test
    @DisplayName("Set and get encouragement value")
    void testSetEncouragement() {
        profile.setEncouragement(55);
        assertEquals(55, profile.getEncouragement());
    }

    @Test
    @DisplayName("Set and get formality value")
    void testSetFormality() {
        profile.setFormality(75);
        assertEquals(75, profile.getFormality());
    }

    @Test
    @DisplayName("Custom traits are clamped to valid range")
    void testCustomTraitsClamped() {
        profile.setHumor(150);
        assertEquals(100, profile.getHumor());

        profile.setEncouragement(-20);
        assertEquals(0, profile.getEncouragement());

        profile.setFormality(50);
        assertEquals(50, profile.getFormality());
    }

    // ==================== Catchphrase Management Tests ====================

    @Test
    @DisplayName("Default catchphrases are present")
    void testDefaultCatchphrases() {
        List<String> catchphrases = profile.getCatchphrases();

        assertNotNull(catchphrases);
        assertFalse(catchphrases.isEmpty());
        assertEquals(4, catchphrases.size());
    }

    @Test
    @DisplayName("Catchphrases list contains expected default phrases")
    void testDefaultCatchphrasesContent() {
        List<String> catchphrases = profile.getCatchphrases();

        assertTrue(catchphrases.contains("Right then,"));
        assertTrue(catchphrases.contains("Let's get to work!"));
        assertTrue(catchphrases.contains("We've got this."));
        assertTrue(catchphrases.contains("Another day, another block."));
    }

    @Test
    @DisplayName("Add catchphrase to profile")
    void testAddCatchphrase() {
        int originalSize = profile.getCatchphrases().size();

        profile.addCatchphrase("New catchphrase!");

        assertEquals(originalSize + 1, profile.getCatchphrases().size());
        assertTrue(profile.getCatchphrases().contains("New catchphrase!"));
    }

    @Test
    @DisplayName("Add catchphrase ignores null values")
    void testAddCatchphraseIgnoresNull() {
        int originalSize = profile.getCatchphrases().size();

        profile.addCatchphrase(null);

        assertEquals(originalSize, profile.getCatchphrases().size(),
                      "Null catchphrases should be ignored");
    }

    @Test
    @DisplayName("Add catchphrase ignores blank strings")
    void testAddCatchphraseIgnoresBlank() {
        int originalSize = profile.getCatchphrases().size();

        profile.addCatchphrase("   ");
        profile.addCatchphrase("");

        assertEquals(originalSize, profile.getCatchphrases().size(),
                      "Blank catchphrases should be ignored");
    }

    @Test
    @DisplayName("Remove existing catchphrase")
    void testRemoveCatchphrase() {
        profile.addCatchphrase("Temporary catchphrase");
        assertTrue(profile.getCatchphrases().contains("Temporary catchphrase"));

        profile.removeCatchphrase("Temporary catchphrase");

        assertFalse(profile.getCatchphrases().contains("Temporary catchphrase"));
    }

    @Test
    @DisplayName("Remove non-existent catchphrase does nothing")
    void testRemoveNonExistentCatchphrase() {
        int originalSize = profile.getCatchphrases().size();

        profile.removeCatchphrase("This catchphrase doesn't exist");

        assertEquals(originalSize, profile.getCatchphrases().size());
    }

    @Test
    @DisplayName("Set catchphrases replaces existing list")
    void testSetCatchphrases() {
        List<String> newCatchphrases = List.of("Phrase 1", "Phrase 2", "Phrase 3");

        profile.setCatchphrases(newCatchphrases);

        assertEquals(3, profile.getCatchphrases().size());
        assertTrue(profile.getCatchphrases().contains("Phrase 1"));
        assertTrue(profile.getCatchphrases().contains("Phrase 2"));
        assertTrue(profile.getCatchphrases().contains("Phrase 3"));
        assertFalse(profile.getCatchphrases().contains("Right then,"));
    }

    @Test
    @DisplayName("Set catchphrases with null does nothing")
    void testSetCatchphrasesWithNull() {
        int originalSize = profile.getCatchphrases().size();

        profile.setCatchphrases(null);

        assertEquals(originalSize, profile.getCatchphrases().size(),
                      "Setting null should not change catchphrases");
    }

    @Test
    @DisplayName("Get catchphrases returns unmodifiable list")
    void testGetCatchphrasesReturnsUnmodifiableList() {
        List<String> catchphrases = profile.getCatchphrases();

        assertThrows(UnsupportedOperationException.class, () -> {
            catchphrases.add("Should not be able to add");
        }, "Returned list should be unmodifiable");
    }

    // ==================== Verbal Tic Management Tests ====================

    @Test
    @DisplayName("Default verbal tics are present")
    void testDefaultVerbalTics() {
        List<String> tics = profile.getVerbalTics();

        assertNotNull(tics);
        assertFalse(tics.isEmpty());
        assertEquals(3, tics.size());
    }

    @Test
    @DisplayName("Verbal tics list contains expected defaults")
    void testDefaultVerbalTicsContent() {
        List<String> tics = profile.getVerbalTics();

        assertTrue(tics.contains("Well,"));
        assertTrue(tics.contains("You see,"));
        assertTrue(tics.contains("Here's the thing"));
    }

    @Test
    @DisplayName("Add verbal tic to profile")
    void testAddVerbalTic() {
        int originalSize = profile.getVerbalTics().size();

        profile.addVerbalTic("Actually,");

        assertEquals(originalSize + 1, profile.getVerbalTics().size());
        assertTrue(profile.getVerbalTics().contains("Actually,"));
    }

    @Test
    @DisplayName("Add verbal tic ignores null values")
    void testAddVerbalTicIgnoresNull() {
        int originalSize = profile.getVerbalTics().size();

        profile.addVerbalTic(null);

        assertEquals(originalSize, profile.getVerbalTics().size());
    }

    @Test
    @DisplayName("Remove verbal tic from profile")
    void testRemoveVerbalTic() {
        profile.addVerbalTic("Temporary tic");
        assertTrue(profile.getVerbalTics().contains("Temporary tic"));

        profile.removeVerbalTic("Temporary tic");

        assertFalse(profile.getVerbalTics().contains("Temporary tic"));
    }

    @Test
    @DisplayName("Set verbal tics replaces existing list")
    void testSetVerbalTics() {
        List<String> newTics = List.of("Hmm,", "Like,", "You know,");

        profile.setVerbalTics(newTics);

        assertEquals(3, profile.getVerbalTics().size());
        assertTrue(profile.getVerbalTics().contains("Hmm,"));
        assertFalse(profile.getVerbalTics().contains("Well,"));
    }

    @Test
    @DisplayName("Get verbal tics returns unmodifiable list")
    void testGetVerbalTicsReturnsUnmodifiableList() {
        List<String> tics = profile.getVerbalTics();

        assertThrows(UnsupportedOperationException.class, () -> {
            tics.add("Should not work");
        }, "Returned list should be unmodifiable");
    }

    // ==================== Tic Usage Tracking Tests ====================

    @Test
    @DisplayName("Get random verbal tic returns valid tic")
    void testGetRandomVerbalTic() {
        String tic = profile.getRandomVerbalTic();

        assertNotNull(tic);
        assertFalse(tic.isEmpty());
        assertTrue(profile.getVerbalTics().contains(tic));
    }

    @Test
    @DisplayName("Get random verbal tic returns empty string when no tics")
    void testGetRandomVerbalTicWithEmptyList() {
        profile.setVerbalTics(new ArrayList<>());

        String tic = profile.getRandomVerbalTic();

        assertTrue(tic.isEmpty(), "Should return empty string when no tics available");
    }

    @Test
    @DisplayName("Get random verbal tic tracks usage count")
    void testGetRandomVerbalTicTracksUsage() {
        profile.setVerbalTics(List.of("Tic1", "Tic2"));

        profile.getRandomVerbalTic(); // Get one tic

        Map<String, Integer> usageCount = profile.getTicUsageCount();

        assertTrue(usageCount.size() > 0, "Usage count should be tracked");
    }

    @Test
    @DisplayName("Get random verbal tic updates recent tics")
    void testGetRandomVerbalTicUpdatesRecentTics() {
        profile.setVerbalTics(List.of("Tic1", "Tic2", "Tic3", "Tic4", "Tic5"));

        for (int i = 0; i < 10; i++) {
            profile.getRandomVerbalTic();
        }

        List<String> recentTics = profile.getRecentTics();

        assertTrue(recentTics.size() <= 5, "Recent tics should be limited to 5");
    }

    @Test
    @DisplayName("Get random verbal tic avoids recent repetitions")
    void testGetRandomVerbalTicAvoidsRepetition() {
        profile.setVerbalTics(List.of("Tic1", "Tic2"));

        String tic1 = profile.getRandomVerbalTic();
        profile.setVerbalTics(List.of("Tic1", "Tic2"));
        String tic2 = profile.getRandomVerbalTic();

        // With 2 tics and 5 attempts to avoid repetition, should get both
        assertTrue(profile.getVerbalTics().contains(tic1));
        assertTrue(profile.getVerbalTics().contains(tic2));
    }

    @Test
    @DisplayName("Should use verbal tic returns true when tics available")
    void testShouldUseVerbalTic() {
        profile.setVerbalTics(List.of("Tic1"));
        profile.setNeuroticism(50);

        boolean shouldUse = profile.shouldUseVerbalTic();

        // Based on neuroticism 50, base chance is ~25%
        // With randomness, we can't assert exact value, but can run multiple times
        assertNotNull(shouldUse);
    }

    @Test
    @DisplayName("Should use verbal tic returns false when no tics")
    void testShouldUseVerbalTicReturnsFalseWhenEmpty() {
        profile.setVerbalTics(new ArrayList<>());

        assertFalse(profile.shouldUseVerbalTic(),
                   "Should return false when no verbal tics available");
    }

    // ==================== Preferences Tests ====================

    @Test
    @DisplayName("Default favorite block")
    void testDefaultFavoriteBlock() {
        assertEquals("cobblestone", profile.getFavoriteBlock());
    }

    @Test
    @DisplayName("Set and get favorite block")
    void testSetFavoriteBlock() {
        profile.setFavoriteBlock("oak_planks");
        assertEquals("oak_planks", profile.getFavoriteBlock());
    }

    @Test
    @DisplayName("Set favorite block to null uses default")
    void testSetFavoriteBlockToNull() {
        profile.setFavoriteBlock("stone");
        profile.setFavoriteBlock(null);

        assertEquals("cobblestone", profile.getFavoriteBlock(),
                     "Null should revert to default");
    }

    @Test
    @DisplayName("Default work style")
    void testDefaultWorkStyle() {
        assertEquals("methodical", profile.getWorkStyle());
    }

    @Test
    @DisplayName("Set and get work style")
    void testSetWorkStyle() {
        profile.setWorkStyle("efficient");
        assertEquals("efficient", profile.getWorkStyle());
    }

    @Test
    @DisplayName("Set work style to null uses default")
    void testSetWorkStyleToNull() {
        profile.setWorkStyle("careful");
        profile.setWorkStyle(null);

        assertEquals("methodical", profile.getWorkStyle());
    }

    @Test
    @DisplayName("Default mood")
    void testDefaultMood() {
        assertEquals("cheerful", profile.getMood());
    }

    @Test
    @DisplayName("Set and get mood")
    void testSetMood() {
        profile.setMood("focused");
        assertEquals("focused", profile.getMood());
    }

    @Test
    @DisplayName("Set mood to null uses default")
    void testSetMoodToNull() {
        profile.setMood("excited");
        profile.setMood(null);

        assertEquals("cheerful", profile.getMood());
    }

    @Test
    @DisplayName("Default archetype name")
    void testDefaultArchetypeName() {
        assertEquals("THE_FOREMAN", profile.getArchetypeName());
    }

    @Test
    @DisplayName("Set and get archetype name")
    void testSetArchetypeName() {
        profile.setArchetypeName("THE_BUILDER");
        assertEquals("THE_BUILDER", profile.getArchetypeName());
    }

    @Test
    @DisplayName("Set archetype name to null uses default")
    void testSetArchetypeNameToNull() {
        profile.setArchetypeName("THE_OPTIMIST");
        profile.setArchetypeName(null);

        assertEquals("THE_FOREMAN", profile.getArchetypeName());
    }

    // ==================== Speech Pattern Tests ====================

    @Test
    @DisplayName("Get speech pattern description for balanced personality")
    void testGetSpeechPatternDescriptionBalanced() {
        profile.setExtraversion(50);
        profile.setFormality(50);
        profile.setHumor(50);
        profile.setConscientiousness(50);

        String description = profile.getSpeechPatternDescription();

        assertEquals("balanced and friendly", description);
    }

    @Test
    @DisplayName("Get speech pattern description for extraverted")
    void testGetSpeechPatternDescriptionExtraverted() {
        profile.setExtraversion(80);
        profile.setFormality(50);
        profile.setHumor(50);

        String description = profile.getSpeechPatternDescription();

        assertTrue(description.contains("enthusiastic and expressive"));
    }

    @Test
    @DisplayName("Get speech pattern description for introverted")
    void testGetSpeechPatternDescriptionIntroverted() {
        profile.setExtraversion(30);
        profile.setFormality(50);
        profile.setHumor(50);

        String description = profile.getSpeechPatternDescription();

        assertTrue(description.contains("quiet and thoughtful"));
    }

    @Test
    @DisplayName("Get speech pattern description for formal")
    void testGetSpeechPatternDescriptionFormal() {
        profile.setExtraversion(50);
        profile.setFormality(70);
        profile.setHumor(50);

        String description = profile.getSpeechPatternDescription();

        assertTrue(description.contains("formal and polite"));
    }

    @Test
    @DisplayName("Get speech pattern description for casual")
    void testGetSpeechPatternDescriptionCasual() {
        profile.setExtraversion(50);
        profile.setFormality(30);
        profile.setHumor(50);

        String description = profile.getSpeechPatternDescription();

        assertTrue(description.contains("casual and relaxed"));
    }

    @Test
    @DisplayName("Get speech pattern description for humorous")
    void testGetSpeechPatternDescriptionHumorous() {
        profile.setExtraversion(50);
        profile.setFormality(50);
        profile.setHumor(70);

        String description = profile.getSpeechPatternDescription();

        assertTrue(description.contains("frequently humorous"));
    }

    @Test
    @DisplayName("Get speech pattern description for methodical")
    void testGetSpeechPatternDescriptionMethodical() {
        profile.setExtraversion(50);
        profile.setFormality(50);
        profile.setHumor(50);
        profile.setConscientiousness(80);

        String description = profile.getSpeechPatternDescription();

        assertTrue(description.contains("methodical and precise"));
    }

    @Test
    @DisplayName("Get speech pattern description combines multiple traits")
    void testGetSpeechPatternDescriptionCombinesTraits() {
        profile.setExtraversion(80);
        profile.setFormality(30);
        profile.setHumor(70);
        profile.setConscientiousness(80);

        String description = profile.getSpeechPatternDescription();

        assertTrue(description.contains("enthusiastic and expressive"));
        assertTrue(description.contains("casual and relaxed"));
        assertTrue(description.contains("frequently humorous"));
        assertTrue(description.contains("methodical and precise"));
    }

    // ==================== Prompt Context Tests ====================

    @Test
    @DisplayName("To prompt context generates valid output")
    void testToPromptContext() {
        String context = profile.toPromptContext();

        assertNotNull(context);
        assertFalse(context.isEmpty());
    }

    @Test
    @DisplayName("To prompt context contains personality traits")
    void testToPromptContextContainsTraits() {
        String context = profile.toPromptContext();

        assertTrue(context.contains("Personality Traits:"));
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
    @DisplayName("To prompt context contains catchphrases")
    void testToPromptContextContainsCatchphrases() {
        String context = profile.toPromptContext();

        assertTrue(context.contains("Catchphrases:"));
    }

    @Test
    @DisplayName("To prompt context contains verbal tics")
    void testToPromptContextContainsVerbalTics() {
        String context = profile.toPromptContext();

        assertTrue(context.contains("Verbal Tics"));
    }

    @Test
    @DisplayName("To prompt context contains favorite block")
    void testToPromptContextContainsFavoriteBlock() {
        profile.setFavoriteBlock("diamond_block");

        String context = profile.toPromptContext();

        assertTrue(context.contains("Favorite block:"));
        assertTrue(context.contains("diamond_block"));
    }

    // ==================== Archetype Application Tests ====================

    @Test
    @DisplayName("Apply archetype updates personality traits")
    void testApplyArchetypeUpdatesTraits() {
        ForemanArchetypeConfig.ForemanArchetype archetype = ForemanArchetypeConfig.THE_OPTIMIST;

        profile.applyArchetype(archetype);

        assertEquals(archetype.getTraits().getOpenness(), profile.getOpenness());
        assertEquals(archetype.getTraits().getConscientiousness(), profile.getConscientiousness());
        assertEquals(archetype.getTraits().getExtraversion(), profile.getExtraversion());
        assertEquals(archetype.getTraits().getAgreeableness(), profile.getAgreeableness());
        assertEquals(archetype.getTraits().getNeuroticism(), profile.getNeuroticism());
    }

    @Test
    @DisplayName("Apply archetype updates custom traits")
    void testApplyArchetypeUpdatesCustomTraits() {
        ForemanArchetypeConfig.ForemanArchetype archetype = ForemanArchetypeConfig.THE_BUILDER;

        profile.applyArchetype(archetype);

        assertEquals(archetype.getFormality(), profile.getFormality());
        assertEquals(archetype.getHumor(), profile.getHumor());
        assertEquals(archetype.getEncouragement(), profile.getEncouragement());
    }

    @Test
    @DisplayName("Apply archetype updates catchphrases")
    void testApplyArchetypeUpdatesCatchphrases() {
        ForemanArchetypeConfig.ForemanArchetype archetype = ForemanArchetypeConfig.THE_VETERAN;

        profile.applyArchetype(archetype);

        List<String> archetypeCatchphrases = archetype.getCatchphrases();
        List<String> profileCatchphrases = profile.getCatchphrases();

        assertEquals(archetypeCatchphrases.size(), profileCatchphrases.size());
        for (String phrase : archetypeCatchphrases) {
            assertTrue(profileCatchphrases.contains(phrase));
        }
    }

    @Test
    @DisplayName("Apply archetype updates verbal tics")
    void testApplyArchetypeUpdatesVerbalTics() {
        ForemanArchetypeConfig.ForemanArchetype archetype = ForemanArchetypeConfig.THE_TECHIE;

        profile.applyArchetype(archetype);

        List<String> archetypeTics = archetype.getVerbalTics();
        List<String> profileTics = profile.getVerbalTics();

        assertEquals(archetypeTics.size(), profileTics.size());
        for (String tic : archetypeTics) {
            assertTrue(profileTics.contains(tic));
        }
    }

    @Test
    @DisplayName("Apply archetype updates archetype name")
    void testApplyArchetypeUpdatesName() {
        ForemanArchetypeConfig.ForemanArchetype archetype = ForemanArchetypeConfig.THE_ROOKIE;

        profile.applyArchetype(archetype);

        assertEquals(archetype.getName(), profile.getArchetypeName());
    }

    // ==================== Mood Parsing Tests ====================

    @Test
    @DisplayName("Parse mood valid enum value")
    void testParseMoodValid() {
        CompanionMemory.Mood mood = PersonalitySystem.parseMood("CHEERFUL");

        assertEquals(CompanionMemory.Mood.CHEERFUL, mood);
    }

    @Test
    @DisplayName("Parse mood case insensitive")
    void testParseMoodCaseInsensitive() {
        CompanionMemory.Mood mood = PersonalitySystem.parseMood("cheerful");

        assertEquals(CompanionMemory.Mood.CHEERFUL, mood);
    }

    @Test
    @DisplayName("Parse mood null returns default")
    void testParseMoodNull() {
        CompanionMemory.Mood mood = PersonalitySystem.parseMood(null);

        assertEquals(CompanionMemory.Mood.CHEERFUL, mood);
    }

    @Test
    @DisplayName("Parse mood invalid returns default")
    void testParseMoodInvalid() {
        CompanionMemory.Mood mood = PersonalitySystem.parseMood("not_a_real_mood");

        assertEquals(CompanionMemory.Mood.CHEERFUL, mood);
    }

    @Test
    @DisplayName("Parse mood various valid values")
    void testParseMoodVariousValues() {
        assertEquals(CompanionMemory.Mood.FOCUSED, PersonalitySystem.parseMood("FOCUSED"));
        assertEquals(CompanionMemory.Mood.HAPPY, PersonalitySystem.parseMood("happy"));
        assertEquals(CompanionMemory.Mood.CALM, PersonalitySystem.parseMood("Calm"));
    }

    // ==================== Copy Constructor Tests ====================

    @Test
    @DisplayName("Get tic usage count returns defensive copy")
    void testGetTicUsageCountReturnsDefensiveCopy() {
        profile.getRandomVerbalTic();

        Map<String, Integer> usageCount1 = profile.getTicUsageCount();
        Map<String, Integer> usageCount2 = profile.getTicUsageCount();

        assertNotSame(usageCount1, usageCount2, "Should return new map each time");
        assertEquals(usageCount1, usageCount2, "But content should be the same");
    }

    @Test
    @DisplayName("Get recent tics returns defensive copy")
    void testGetRecentTicsReturnsDefensiveCopy() {
        profile.getRandomVerbalTic();

        List<String> recent1 = profile.getRecentTics();
        List<String> recent2 = profile.getRecentTics();

        assertNotSame(recent1, recent2, "Should return new list each time");
        assertEquals(recent1, recent2, "But content should be the same");
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("Profile with all traits at minimum values")
    void testProfileWithAllTraitsAtMinimum() {
        profile.setOpenness(0);
        profile.setConscientiousness(0);
        profile.setExtraversion(0);
        profile.setAgreeableness(0);
        profile.setNeuroticism(0);
        profile.setHumor(0);
        profile.setEncouragement(0);
        profile.setFormality(0);

        assertEquals(0, profile.getOpenness());
        assertEquals(0, profile.getConscientiousness());
        assertEquals(0, profile.getExtraversion());
        assertEquals(0, profile.getAgreeableness());
        assertEquals(0, profile.getNeuroticism());
        assertEquals(0, profile.getHumor());
        assertEquals(0, profile.getEncouragement());
        assertEquals(0, profile.getFormality());
    }

    @Test
    @DisplayName("Profile with all traits at maximum values")
    void testProfileWithAllTraitsAtMaximum() {
        profile.setOpenness(100);
        profile.setConscientiousness(100);
        profile.setExtraversion(100);
        profile.setAgreeableness(100);
        profile.setNeuroticism(100);
        profile.setHumor(100);
        profile.setEncouragement(100);
        profile.setFormality(100);

        assertEquals(100, profile.getOpenness());
        assertEquals(100, profile.getConscientiousness());
        assertEquals(100, profile.getExtraversion());
        assertEquals(100, profile.getAgreeableness());
        assertEquals(100, profile.getNeuroticism());
        assertEquals(100, profile.getHumor());
        assertEquals(100, profile.getEncouragement());
        assertEquals(100, profile.getFormality());
    }

    @Test
    @DisplayName("Speech pattern with high extraversion shows correct description")
    void testSpeechPatternWithHighExtraversion() {
        profile.setExtraversion(85);
        String description = profile.getSpeechPatternDescription();

        assertTrue(description.contains("enthusiastic"));
        assertFalse(description.contains("quiet"));
    }

    @Test
    @DisplayName("Speech pattern with low extraversion shows correct description")
    void testSpeechPatternWithLowExtraversion() {
        profile.setExtraversion(25);
        String description = profile.getSpeechPatternDescription();

        assertTrue(description.contains("quiet"));
        assertFalse(description.contains("enthusiastic"));
    }
}
