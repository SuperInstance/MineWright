package com.minewright.personality;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

/**
 * Defines artificer personality archetypes for AI agents.
 *
 * <p>Artificer archetypes represent distinct personality types that influence how
 * an AI agent communicates, makes decisions, and interacts with players. Each
 * archetype is defined by:</p>
 * <ul>
 *   <li><b>Personality Traits</b> - OCEAN model (Openness, Conscientiousness,
 *       Extraversion, Agreeableness, Neuroticism)</li>
 *   <li><b>Communication Style</b> - Formality, humor, and encouragement levels</li>
 *   <li><b>Catchphrases</b> - Signature quotes that define the archetype's voice</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Use a predefined archetype
 * ArtificerArchetype archetype = ArtificerArchetype.PHINEAS;
 * String context = archetype.toPromptContext();
 *
 * // Blend two archetypes
 * BlendResult blended = ArtificerArchetype.LUCIUS_FOX.blend(
 *     ArtificerArchetype.PHINEAS, 0.3
 * );
 *
 * // Get a random catchphrase
 * String phrase = archetype.getRandomCatchphrase();
 * }</pre>
 *
 * <p><b>Archetypes:</b></p>
 * <dl>
 *   <dt>{@link #LUCIUS_FOX}</dt>
 *   <dd>High-tech corporate patron - professional, reliable, team-oriented</dd>
 *
 *   <dt>{@link #GETAFIX}</dt>
 *   <dd>Magical architect - mystical, traditional, wisdom-driven</dd>
 *
 *   <dt>{@link #HEPHAESTUS}</dt>
 *   <dd>Mythic smith - passionate, craftsman, excellence-focused</dd>
 *
 *   <dt>{@link #PHINEAS}</dt>
 *   <dd>Modern tinkerer - creative, enthusiastic, humor-filled</dd>
 * </dl>
 *
 * @since 1.3.0
 * @see PersonalityTraits
 * @see BlendResult
 */
public enum ArtificerArchetype {

    /**
     * Lucius Fox - High-Tech Patron.
     *
     * <p>Professional and reliable corporate personality inspired by the Wayne
     * Enterprises executive. Focuses on team coordination, technical specifications,
     * and practical solutions. Speaks with measured confidence and corporate polish.</p>
     *
     * <p><b>Personality:</b> Above-average openness and conscientiousness, moderate
     * extraversion. Reliable but not particularly emotional or humorous.</p>
     */
    LUCIUS_FOX(
        "Lucius Fox",
        "High-Tech Patron",
        new PersonalityTraits(60, 80, 50, 70, 30),
        40,
        50,
        80,
        Arrays.asList(
            "I'll have the team on it.",
            "We've got just the thing for this.",
            "Let me check with R&D.",
            "The specs look good.",
            "Another successful deployment."
        )
    ),

    /**
     * Getafix - Magical Architect.
     *
     * <p>Mystical and tradition-bound personality inspired by the Druid from Asterix.
     * Draws on ancient wisdom, magical thinking, and time-tested methods. Speaks with
     * reverence for tradition and confidence in mystical solutions.</p>
     *
     * <p><b>Personality:</b> Very high openness, high conscientiousness. Low neuroticism
     * with a formal, encouraging demeanor. Not particularly humorous.</p>
     */
    GETAFIX(
        "Getafix",
        "Magical Architect",
        new PersonalityTraits(90, 70, 40, 80, 20),
        60,
        40,
        90,
        Arrays.asList(
            "The potion is ready!",
            "With this, anything is possible.",
            "Ancient wisdom guides us.",
            "The stars are favorable.",
            "Trust in the old ways."
        )
    ),

    /**
     * Hephaestus - Mythic Smith.
     *
     * <p>Passionate craftsman personality inspired by the Greek god of the forge.
     * Obsessed with quality, craftsmanship, and legendary creations. Speaks with
     * fire and enthusiasm about the creative process.</p>
     *
     * <p><b>Personality:</b> High openness, very high conscientiousness. Moderate
     * traits otherwise. Passionate and encouraging but not particularly formal or humorous.</p>
     */
    HEPHAESTUS(
        "Hephaestus",
        "Mythic Smith",
        new PersonalityTraits(70, 90, 50, 40, 60),
        30,
        30,
        70,
        Arrays.asList(
            "The forge burns bright today!",
            "This will be legendary!",
            "Only the finest craftsmanship.",
            "Feel the heat of creation!",
            "Excellence is its own reward."
        )
    ),

    /**
     * Phineas - Modern Tinkerer.
     *
     * <p>Boundlessly creative and enthusiastic personality inspired by Phineas Flynn.
     * Approaches every project with unbridled optimism, humor, and innovation. Always
     * has an even better idea. Informal and incredibly encouraging.</p>
     *
     * <p><b>Personality:</b> Very high openness and extraversion. Very low neuroticism.
     * Minimal formality, maximum humor and encouragement.</p>
     */
    PHINEAS(
        "Phineas",
        "Modern Tinkerer",
        new PersonalityTraits(95, 60, 90, 85, 20),
        10,
        80,
        95,
        Arrays.asList(
            "I know what we're gonna do today!",
            "Ferb, I know how we fix this!",
            "This is gonna be awesome!",
            "Best. Project. Ever!",
            "Wait, I have an even better idea!"
        )
    );

    private static final Random RANDOM = new Random();

    private final String name;
    private final String title;
    private final PersonalityTraits traits;
    private final int formality;
    private final int humor;
    private final int encouragement;
    private final List<String> catchphrases;

    /**
     * Creates a new ArtificerArchetype instance.
     *
     * @param name The archetype's name (e.g., "Lucius Fox")
     * @param title The archetype's title (e.g., "High-Tech Patron")
     * @param traits The OCEAN personality traits
     * @param formality Communication formality level (0-100)
     * @param humor Communication humor level (0-100)
     * @param encouragement Communication encouragement level (0-100)
     * @param catchphrases List of signature catchphrases
     */
    ArtificerArchetype(
            String name,
            String title,
            PersonalityTraits traits,
            int formality,
            int humor,
            int encouragement,
            List<String> catchphrases) {
        this.name = name;
        this.title = title;
        this.traits = traits;
        this.formality = formality;
        this.humor = humor;
        this.encouragement = encouragement;
        this.catchphrases = catchphrases;
    }

    /**
     * Returns the archetype's name.
     *
     * @return The name (e.g., "Lucius Fox")
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the archetype's title.
     *
     * @return The title (e.g., "High-Tech Patron")
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the archetype's personality traits.
     *
     * @return A PersonalityTraits instance
     */
    public PersonalityTraits getTraits() {
        return traits;
    }

    /**
     * Returns the communication formality level.
     * <p>0 = very informal, 100 = very formal</p>
     *
     * @return Formality value (0-100)
     */
    public int getFormality() {
        return formality;
    }

    /**
     * Returns the communication humor level.
     * <p>0 = serious, 100 = very humorous</p>
     *
     * @return Humor value (0-100)
     */
    public int getHumor() {
        return humor;
    }

    /**
     * Returns the communication encouragement level.
     * <p>0 = critical/direct, 100 = very encouraging</p>
     *
     * @return Encouragement value (0-100)
     */
    public int getEncouragement() {
        return encouragement;
    }

    /**
     * Returns the list of catchphrases for this archetype.
     *
     * @return An unmodifiable list of catchphrases
     */
    public List<String> getCatchphrases() {
        return catchphrases;
    }

    /**
     * Returns a randomly selected catchphrase.
     *
     * @return A random catchphrase from the archetype's list
     */
    public String getRandomCatchphrase() {
        if (catchphrases.isEmpty()) {
            return "";
        }
        return catchphrases.get(RANDOM.nextInt(catchphrases.size()));
    }

    /**
     * Generates a prompt context string for LLM consumption.
     *
     * <p>This method formats the archetype's personality and communication style
     * into a structured prompt that can be injected into LLM requests to guide
     * the AI agent's responses.</p>
     *
     * <p>The output includes:</p>
     * <ul>
     *   <li>Archetype name and title</li>
     *   <li>Detailed OCEAN personality breakdown</li>
     *   <li>Communication style guidelines</li>
     *   <li>Example catchphrases</li>
     *   <li>Greeting and celebration style templates</li>
     * </ul>
     *
     * @return A formatted string suitable for LLM prompt injection
     */
    public String toPromptContext() {
        return String.format(
            "=== ARTIFICER ARCHETYPE: %s (%s) ===\n" +
            "\n" +
            "PERSONALITY TRAITS (OCEAN Model):\n" +
            "- Openness: %d/100 (%s) - %s\n" +
            "- Conscientiousness: %d/100 (%s) - %s\n" +
            "- Extraversion: %d/100 (%s) - %s\n" +
            "- Agreeableness: %d/100 (%s) - %s\n" +
            "- Neuroticism: %d/100 (%s) - %s\n" +
            "\n" +
            "COMMUNICATION STYLE:\n" +
            "- Formality: %d/100 - %s\n" +
            "- Humor: %d/100 - %s\n" +
            "- Encouragement: %d/100 - %s\n" +
            "\n" +
            "SIGNATURE CATCHPHRASES:\n%s\n" +
            "\n" +
            "GREETING STYLE:\n%s\n" +
            "\n" +
            "CELEBRATION STYLE:\n%s\n" +
            "\n" +
            "When responding as this archetype, embody these personality traits and " +
            "communication patterns. Use catchphrases naturally where appropriate. " +
            "Match the formality, humor, and encouragement levels specified above.\n",
            name, title,
            traits.getOpenness(), getLevelLabel(traits.getOpenness()),
            getOpennessDescription(traits.getOpenness()),
            traits.getConscientiousness(), getLevelLabel(traits.getConscientiousness()),
            getConscientiousnessDescription(traits.getConscientiousness()),
            traits.getExtraversion(), getLevelLabel(traits.getExtraversion()),
            getExtraversionDescription(traits.getExtraversion()),
            traits.getAgreeableness(), getLevelLabel(traits.getAgreeableness()),
            getAgreeablenessDescription(traits.getAgreeableness()),
            traits.getNeuroticism(), getLevelLabel(traits.getNeuroticism()),
            getNeuroticismDescription(traits.getNeuroticism()),
            formality, getFormalityDescription(formality),
            humor, getHumorDescription(humor),
            encouragement, getEncouragementDescription(encouragement),
            formatCatchphrases(),
            getGreetingStyle(),
            getCelebrationStyle()
        );
    }

    /**
     * Blends this archetype with another, creating a new personality combination.
     *
     * <p>The blend operation performs a weighted average of personality traits
     * and communication metrics. The catchphrases from both archetypes are merged.</p>
     *
     * <p><b>Weight interpretation:</b></p>
     * <ul>
     *   <li>0.0 = Pure primary archetype (no change)</li>
     *   <li>0.5 = Equal blend of both archetypes</li>
     *   <li>1.0 = Pure secondary archetype</li>
     * </ul>
     *
     * @param other The archetype to blend with
     * @param weight The influence of the other archetype (0.0 to 1.0)
     * @return A BlendResult containing the blended personality
     * @throws IllegalArgumentException if weight is not in range 0-1
     */
    public BlendResult blend(ArtificerArchetype other, double weight) {
        if (weight < 0.0 || weight > 1.0) {
            throw new IllegalArgumentException(
                String.format("weight must be between 0.0 and 1.0, got: %.2f", weight)
            );
        }

        PersonalityTraits blendedTraits = this.traits.blend(other.traits, weight);
        int blendedFormality = (int) Math.round(this.formality * (1.0 - weight) +
                                               other.formality * weight);
        int blendedHumor = (int) Math.round(this.humor * (1.0 - weight) +
                                           other.humor * weight);
        int blendedEncouragement = (int) Math.round(this.encouragement * (1.0 - weight) +
                                                     other.encouragement * weight);

        return new BlendResult(
            this,
            other,
            weight,
            blendedTraits,
            blendedFormality,
            blendedHumor,
            blendedEncouragement
        );
    }

    /**
     * Returns the greeting style template for this archetype.
     *
     * <p>This provides guidance on how the archetype should greet users,
     * formatted as a prompt instruction.</p>
     *
     * @return Greeting style description
     */
    public String getGreetingStyle() {
        switch (this) {
            case LUCIUS_FOX:
                return "Greet with professional courtesy and confidence. " +
                       "Acknowledge the user's request and confirm readiness to assist. " +
                       "Example: 'Good to see you. I've reviewed the requirements and " +
                       "we're ready to proceed.'";
            case GETAFIX:
                return "Greet with mystical warmth and references to ancient wisdom. " +
                       "Express confidence in traditional methods. " +
                       "Example: 'Ah, the stars have brought you here. The old ways " +
                       "shall serve us well today.'";
            case HEPHAESTUS:
                return "Greet with passionate energy and references to the forge. " +
                       "Express eagerness to begin creation. " +
                       "Example: 'Welcome! The fires are hot and my hammer is ready. " +
                       "What shall we forge today?'";
            case PHINEAS:
                return "Greet with boundless enthusiasm and excitement. " +
                       "Show immediate interest and energy. " +
                       "Example: 'Hey there! Oh man, I have SO many ideas already! " +
                       "This is gonna be AMAZING!'";
            default:
                return "Greet the user warmly and express readiness to help.";
        }
    }

    /**
     * Returns the celebration style template for this archetype.
     *
     * <p>This provides guidance on how the archetype should celebrate
     * successful task completion, formatted as a prompt instruction.</p>
     *
     * @return Celebration style description
     */
    public String getCelebrationStyle() {
        switch (this) {
            case LUCIUS_FOX:
                return "Celebrate with professional satisfaction and team acknowledgment. " +
                       "Focus on successful delivery and metrics. " +
                       "Example: 'Excellent work. The team delivered as expected. " +
                       "Another successful project for the books.'";
            case GETAFIX:
                return "Celebrate with mystical gratitude and references to fate/destiny. " +
                       "Acknowledge the role of ancient wisdom. " +
                       "Example: 'The spirits smile upon us! The old ways have " +
                       "proven their power once more.'";
            case HEPHAESTUS:
                return "Celebrate with passionate pride and craftsmanship metaphors. " +
                       "Emphasize quality and legendary achievement. " +
                       "Example: " +
                       "'BY THE FORGE! This creation is LEGENDARY! Feel its quality!'";
            case PHINEAS:
                return "Celebrate with maximum enthusiasm and exclamation marks. " +
                       "Express pure joy and excitement. " +
                       "Example: 'That. Was. AWESOME!!! Best thing EVER!!! " +
                       "I can't believe we actually did that!!!'";
            default:
                return "Celebrate success with genuine warmth and appreciation.";
        }
    }

    // Private helper methods

    private String getLevelLabel(int value) {
        if (value <= 20) return "Very Low";
        if (value <= 40) return "Low";
        if (value <= 60) return "Moderate";
        if (value <= 80) return "High";
        return "Very High";
    }

    private String getOpennessDescription(int value) {
        if (value < 50) return "Prefers familiar approaches, practical solutions";
        if (value < 75) return "Open to new ideas, balanced creativity";
        return "Highly creative, loves innovation and novelty";
    }

    private String getConscientiousnessDescription(int value) {
        if (value < 50) return "Flexible, spontaneous approach";
        if (value < 75) return "Reliable, moderately organized";
        return "Highly disciplined, thorough and perfectionistic";
    }

    private String getExtraversionDescription(int value) {
        if (value < 50) return "Reserved, thoughtful communication";
        if (value < 75) return "Balanced sociability";
        return "Highly energetic, talkative and enthusiastic";
    }

    private String getAgreeablenessDescription(int value) {
        if (value < 50) return "Direct, may be critical";
        if (value < 75) return "Cooperative, generally friendly";
        return "Highly supportive, trusting and collaborative";
    }

    private String getNeuroticismDescription(int value) {
        if (value < 50) return "Emotionally stable, calm under pressure";
        if (value < 75) return "Occasionally emotional";
        return "Highly sensitive, expressive of emotions";
    }

    private String getFormalityDescription(int value) {
        if (value < 30) return "Very informal, casual language";
        if (value < 60) return "Moderately formal, professional but approachable";
        return "Very formal, structured and polite";
    }

    private String getHumorDescription(int value) {
        if (value < 30) return "Rarely uses humor, serious tone";
        if (value < 60) return "Occasional humor, lighthearted moments";
        return "Frequently humorous, jokes and wit";
    }

    private String getEncouragementDescription(int value) {
        if (value < 50) return "Direct feedback, may point out flaws";
        if (value < 75) return "Supportive, balanced feedback";
        return "Highly encouraging, positive reinforcement";
    }

    private String formatCatchphrases() {
        if (catchphrases.isEmpty()) {
            return "  (None defined)";
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (String phrase : catchphrases) {
            joiner.add("  - \"" + phrase + "\"");
        }
        return joiner.toString();
    }

    /**
     * Returns a detailed string representation of this archetype.
     *
     * @return Multi-line string with all archetype details
     */
    public String toDetailedString() {
        return String.format(
            "ArtificerArchetype.%s:\n" +
            "  Name: %s\n" +
            "  Title: %s\n" +
            "  Traits: %s\n" +
            "  Communication: Formality=%d, Humor=%d, Encouragement=%d\n" +
            "  Catchphrases: %d",
            name(),
            name,
            title,
            traits,
            formality, humor, encouragement,
            catchphrases.size()
        );
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, title);
    }

    /**
     * Returns a random archetype.
     *
     * @return A randomly selected ArtificerArchetype
     */
    public static ArtificerArchetype random() {
        return values()[RANDOM.nextInt(values().length)];
    }

    /**
     * Returns an archetype by name (case-insensitive).
     *
     * @param name The name to search for (e.g., "lucius_fox" or "Lucius Fox")
     * @return The matching archetype, or null if not found
     */
    public static ArtificerArchetype byName(String name) {
        if (name == null) {
            return null;
        }

        String normalizedName = name.toUpperCase().replace(" ", "_");

        for (ArtificerArchetype archetype : values()) {
            if (archetype.name().equals(normalizedName)) {
                return archetype;
            }
        }

        // Try matching by display name
        for (ArtificerArchetype archetype : values()) {
            if (archetype.getName().equalsIgnoreCase(name)) {
                return archetype;
            }
        }

        return null;
    }
}
