package com.minewright.personality;

/**
 * Demonstration and usage examples for the Artificer Archetype personality system.
 *
 * <p>This class provides example code showing how to use the personality system
 * for creating, blending, and applying AI agent personalities.</p>
 *
 * <p><b>Note:</b> This is a demonstration class. In production, personality selection
 * would typically be configured per agent through the agent creation API.</p>
 *
 * @since 1.3.0
 */
public class PersonalityDemo {

    /**
     * Demonstrates basic archetype usage.
     */
    public static void demonstrateBasicUsage() {
        // Select an archetype
        ArtificerArchetype archetype = ArtificerArchetype.PHINEAS;

        // Get personality information
        System.out.println("Archetype: " + archetype.getName());
        System.out.println("Title: " + archetype.getTitle());
        System.out.println("Traits: " + archetype.getTraits());

        // Get a catchphrase
        String phrase = archetype.getRandomCatchphrase();
        System.out.println("Catchphrase: \"" + phrase + "\"");

        // Generate LLM prompt context
        String promptContext = archetype.toPromptContext();
        System.out.println("\nPrompt Context:\n" + promptContext);
    }

    /**
     * Demonstrates archetype blending.
     */
    public static void demonstrateBlending() {
        // Blend Lucius Fox with Phineas (30% Phineas influence)
        ArtificerArchetype base = ArtificerArchetype.LUCIUS_FOX;
        ArtificerArchetype modifier = ArtificerArchetype.PHINEAS;

        BlendResult result = base.blend(modifier, 0.3);

        System.out.println("Blend Result:");
        System.out.println("  Description: " + result.getBlendDescription());
        System.out.println("  Traits: " + result.getBlendedTraits());
        System.out.println("  Formality: " + result.getBlendedFormality());
        System.out.println("  Humor: " + result.getBlendedHumor());
        System.out.println("  Encouragement: " + result.getBlendedEncouragement());
    }

    /**
     * Demonstrates personality traits usage.
     */
    public static void demonstrateTraits() {
        // Create custom traits using builder
        PersonalityTraits customTraits = PersonalityTraits.builder()
            .openness(85)
            .conscientiousness(70)
            .extraversion(60)
            .agreeableness(75)
            .neuroticism(25)
            .build();

        System.out.println("Custom Traits:");
        System.out.println(customTraits.toDetailedString());

        // Blend traits
        PersonalityTraits blended = customTraits.blend(
            ArtificerArchetype.HEPHAESTUS.getTraits(),
            0.5
        );
        System.out.println("\nBlended Traits: " + blended);
    }

    /**
     * Demonstrates archetype lookup and iteration.
     */
    public static void demonstrateArchetypeLookup() {
        // Find archetype by name
        ArtificerArchetype found = ArtificerArchetype.byName("getafix");
        if (found != null) {
            System.out.println("Found: " + found);
        }

        // List all archetypes
        System.out.println("\nAll Available Archetypes:");
        for (ArtificerArchetype archetype : ArtificerArchetype.values()) {
            System.out.printf("  - %s: %s%n",
                archetype.getName(),
                archetype.getTitle()
            );
        }

        // Get random archetype
        ArtificerArchetype random = ArtificerArchetype.random();
        System.out.println("\nRandom Selection: " + random);
    }

    /**
     * Demonstrates communication style access.
     */
    public static void demonstrateCommunicationStyles() {
        for (ArtificerArchetype archetype : ArtificerArchetype.values()) {
            System.out.println("\n=== " + archetype.getName() + " ===");
            System.out.println("Greeting Style: " + archetype.getGreetingStyle());
            System.out.println("Celebration Style: " + archetype.getCelebrationStyle());
            System.out.println("Communication Metrics:");
            System.out.println("  - Formality: " + archetype.getFormality());
            System.out.println("  - Humor: " + archetype.getHumor());
            System.out.println("  - Encouragement: " + archetype.getEncouragement());
        }
    }

    /**
     * Main method for standalone demonstration.
     */
    public static void main(String[] args) {
        System.out.println("=== Artificer Archetype Personality System Demo ===\n");

        demonstrateBasicUsage();
        System.out.println("\n" + "=".repeat(60) + "\n");

        demonstrateBlending();
        System.out.println("\n" + "=".repeat(60) + "\n");

        demonstrateTraits();
        System.out.println("\n" + "=".repeat(60) + "\n");

        demonstrateArchetypeLookup();
        System.out.println("\n" + "=".repeat(60) + "\n");

        demonstrateCommunicationStyles();
    }
}
