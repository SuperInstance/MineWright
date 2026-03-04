package com.minewright.memory;

import com.minewright.personality.ForemanArchetypeConfig;

import java.util.*;

/**
 * Personality system for companion AI.
 *
 * <p>This class manages the foreman's personality traits, speech patterns,
 * and behavioral characteristics that make each companion unique.</p>
 *
 * <p><b>Personality Dimensions:</b></p>
 * <ul>
 *   <li>Big Five traits (openness, conscientiousness, extraversion, agreeableness, neuroticism)</li>
 *   <li>Custom traits (humor, encouragement, formality)</li>
 *   <li>Speech patterns (catchphrases, verbal tics)</li>
 *   <li>Preferences (favorite block, work style, mood)</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class PersonalitySystem {

    /**
     * Personality profile for the foreman.
     * Thread-safe: uses synchronized collections for concurrent access.
     */
    public static class PersonalityProfile {
        // Big Five traits (0-100) - private with validation
        private volatile int openness = 70;          // Curious, creative
        private volatile int conscientiousness = 80; // Organized, responsible
        private volatile int extraversion = 60;      // Sociable, energetic
        private volatile int agreeableness = 75;     // Cooperative, trusting
        private volatile int neuroticism = 30;       // Calm, stable

        // Custom traits - private with validation
        private volatile int humor = 65;             // How often uses humor
        private volatile int encouragement = 80;     // How encouraging
        private volatile int formality = 40;         // 0 = casual, 100 = formal

        // Verbal tics and catchphrases - thread-safe lists (private with defensive copies)
        private final List<String> catchphrases = new ArrayList<>(List.of(
            "Right then,",
            "Let's get to work!",
            "We've got this.",
            "Another day, another block."
        ));

        // Enhanced speech patterns
        private final List<String> verbalTics = new ArrayList<>(List.of(
            "Well,",
            "You see,",
            "Here's the thing"
        ));

        // Speech pattern usage tracking
        private final Map<String, Integer> ticUsageCount = new HashMap<>();
        private final List<String> recentTics = new ArrayList<>();

        /**
         * Shared Random instance for random behavior generation.
         * Using a single instance is more efficient than creating new Random objects.
         */
        private static final Random RANDOM = new Random();

        // Preferences (for consistent behavior)
        private String favoriteBlock = "cobblestone";
        private String workStyle = "methodical";
        private String mood = "cheerful";

        // Archetype name if using a predefined archetype
        private String archetypeName = "THE_FOREMAN";

        // === Getters and Setters with Validation ===

        /** Gets the openness trait (curiosity, creativity). */
        public int getOpenness() { return openness; }

        /** Sets the openness trait with validation (clamped to 0-100). */
        public void setOpenness(int value) { this.openness = clampToRange(value, 0, 100); }

        /** Gets the conscientiousness trait (organization, responsibility). */
        public int getConscientiousness() { return conscientiousness; }

        /** Sets the conscientiousness trait with validation (clamped to 0-100). */
        public void setConscientiousness(int value) { this.conscientiousness = clampToRange(value, 0, 100); }

        /** Gets the extraversion trait (sociability, energy). */
        public int getExtraversion() { return extraversion; }

        /** Sets the extraversion trait with validation (clamped to 0-100). */
        public void setExtraversion(int value) { this.extraversion = clampToRange(value, 0, 100); }

        /** Gets the agreeableness trait (cooperation, trust). */
        public int getAgreeableness() { return agreeableness; }

        /** Sets the agreeableness trait with validation (clamped to 0-100). */
        public void setAgreeableness(int value) { this.agreeableness = clampToRange(value, 0, 100); }

        /** Gets the neuroticism trait (emotional stability). */
        public int getNeuroticism() { return neuroticism; }

        /** Sets the neuroticism trait with validation (clamped to 0-100). */
        public void setNeuroticism(int value) { this.neuroticism = clampToRange(value, 0, 100); }

        /** Gets the humor level (how often humor is used). */
        public int getHumor() { return humor; }

        /** Sets the humor level with validation (clamped to 0-100). */
        public void setHumor(int value) { this.humor = clampToRange(value, 0, 100); }

        /** Gets the encouragement level. */
        public int getEncouragement() { return encouragement; }

        /** Sets the encouragement level with validation (clamped to 0-100). */
        public void setEncouragement(int value) { this.encouragement = clampToRange(value, 0, 100); }

        /** Gets the formality level (0=casual, 100=formal). */
        public int getFormality() { return formality; }

        /** Sets the formality level with validation (clamped to 0-100). */
        public void setFormality(int value) { this.formality = clampToRange(value, 0, 100); }

        /** Gets an unmodifiable view of the catchphrases list. */
        public List<String> getCatchphrases() { return List.copyOf(catchphrases); }

        /** Adds a catchphrase to the list (validates for null/empty). */
        public void addCatchphrase(String phrase) {
            if (phrase != null && !phrase.isBlank()) { this.catchphrases.add(phrase); }
        }

        /** Removes a catchphrase from the list. */
        public void removeCatchphrase(String phrase) { this.catchphrases.remove(phrase); }

        /** Clears all catchphrases and sets new ones. */
        public void setCatchphrases(List<String> phrases) {
            if (phrases != null) {
                this.catchphrases.clear();
                this.catchphrases.addAll(phrases);
            }
        }

        /** Gets an unmodifiable view of the verbal tics list. */
        public List<String> getVerbalTics() { return List.copyOf(verbalTics); }

        /** Adds a verbal tic to the list (validates for null/empty). */
        public void addVerbalTic(String tic) {
            if (tic != null && !tic.isBlank()) { this.verbalTics.add(tic); }
        }

        /** Removes a verbal tic from the list. */
        public void removeVerbalTic(String tic) { this.verbalTics.remove(tic); }

        /** Clears all verbal tics and sets new ones. */
        public void setVerbalTics(List<String> tics) {
            if (tics != null) {
                this.verbalTics.clear();
                this.verbalTics.addAll(tics);
            }
        }

        /** Gets the tic usage count map (defensive copy). */
        public Map<String, Integer> getTicUsageCount() { return new HashMap<>(ticUsageCount); }

        /** Gets the recent tics list (defensive copy). */
        public List<String> getRecentTics() { return new ArrayList<>(recentTics); }

        /** Gets the favorite block type. */
        public String getFavoriteBlock() { return favoriteBlock; }

        /** Sets the favorite block type with validation (null-safe). */
        public void setFavoriteBlock(String block) { this.favoriteBlock = block != null ? block : "cobblestone"; }

        /** Gets the work style. */
        public String getWorkStyle() { return workStyle; }

        /** Sets the work style with validation (null-safe). */
        public void setWorkStyle(String style) { this.workStyle = style != null ? style : "methodical"; }

        /** Gets the current mood. */
        public String getMood() { return mood; }

        /** Sets the mood with validation (null-safe). */
        public void setMood(String mood) { this.mood = mood != null ? mood : "cheerful"; }

        /** Gets the archetype name. */
        public String getArchetypeName() { return archetypeName; }

        /** Sets the archetype name with validation (null-safe). */
        public void setArchetypeName(String name) { this.archetypeName = name != null ? name : "THE_FOREMAN"; }

        /** Helper method to clamp values to a valid range. */
        private int clampToRange(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        /**
         * Generates a personality summary for prompting.
         */
        public String toPromptContext() {
            StringBuilder sb = new StringBuilder();
            sb.append("Personality Traits:\n");
            sb.append("- Openness: ").append(openness).append("% (curious and creative)\n");
            sb.append("- Conscientiousness: ").append(conscientiousness).append("% (organized and reliable)\n");
            sb.append("- Extraversion: ").append(extraversion).append("% (").append(extraversion > 50 ? "outgoing" : "reserved").append(")\n");
            sb.append("- Agreeableness: ").append(agreeableness).append("% (cooperative)\n");
            sb.append("- Humor Level: ").append(humor).append("%\n");
            sb.append("- Formality: ").append(formality > 50 ? "formal" : "casual and friendly").append("\n");
            sb.append("- Current mood: ").append(mood).append("\n");
            sb.append("- Archetype: ").append(archetypeName).append("\n");

            if (!catchphrases.isEmpty()) {
                sb.append("- Catchphrases: ");
                int count = Math.min(3, catchphrases.size());
                sb.append(String.join(", ", catchphrases.subList(0, count)));
                if (catchphrases.size() > 3) {
                    sb.append(" (and ").append(catchphrases.size() - 3).append(" more)");
                }
                sb.append("\n");
            }

            if (!verbalTics.isEmpty()) {
                sb.append("- Verbal Tics (use occasionally): ");
                sb.append(String.join(", ", verbalTics)).append("\n");
            }

            sb.append("- Favorite block: ").append(favoriteBlock).append("\n");
            return sb.toString();
        }

        /**
         * Applies a foreman archetype configuration to this profile.
         */
        public void applyArchetype(ForemanArchetypeConfig.ForemanArchetype archetype) {
            com.minewright.personality.PersonalityTraits traits = archetype.getTraits();
            setOpenness(traits.getOpenness());
            setConscientiousness(traits.getConscientiousness());
            setExtraversion(traits.getExtraversion());
            setAgreeableness(traits.getAgreeableness());
            setNeuroticism(traits.getNeuroticism());
            setFormality(archetype.getFormality());
            setHumor(archetype.getHumor());
            setEncouragement(archetype.getEncouragement());
            setCatchphrases(new ArrayList<>(archetype.getCatchphrases()));
            setVerbalTics(new ArrayList<>(archetype.getVerbalTics()));
            setArchetypeName(archetype.getName());
        }

        /**
         * Gets a random verbal tic, tracking usage for variety.
         */
        public String getRandomVerbalTic() {
            if (verbalTics.isEmpty()) {
                return "";
            }

            // Track recent tics to avoid repetition
            String selectedTic;
            int attempts = 0;
            Random random = new Random();
            do {
                selectedTic = verbalTics.get(random.nextInt(verbalTics.size()));
                attempts++;
            } while (recentTics.contains(selectedTic) && attempts < 5);

            // Update tracking
            ticUsageCount.merge(selectedTic, 1, Integer::sum);
            recentTics.add(selectedTic);
            if (recentTics.size() > 5) {
                recentTics.remove(0);
            }

            return selectedTic;
        }

        /**
         * Checks if a verbal tic should be used based on personality and recent usage.
         */
        public boolean shouldUseVerbalTic() {
            if (verbalTics.isEmpty()) {
                return false;
            }

            // Base chance based on neuroticism (nervous characters tic more)
            double baseChance = 0.15 + (neuroticism / 500.0); // 15% to 35%

            // Adjust based on recent tic usage (don't overuse)
            if (!recentTics.isEmpty()) {
                double recentPenalty = recentTics.size() * 0.05;
                baseChance -= recentPenalty;
            }

            return new Random().nextDouble() < Math.max(0.05, baseChance);
        }

        /**
         * Gets the speech pattern description for this personality.
         */
        public String getSpeechPatternDescription() {
            List<String> patterns = new ArrayList<>();

            if (extraversion > 70) {
                patterns.add("enthusiastic and expressive");
            } else if (extraversion < 40) {
                patterns.add("quiet and thoughtful");
            }

            if (formality > 60) {
                patterns.add("formal and polite");
            } else if (formality < 40) {
                patterns.add("casual and relaxed");
            }

            if (humor > 60) {
                patterns.add("frequently humorous");
            }

            if (conscientiousness > 70) {
                patterns.add("methodical and precise");
            }

            if (patterns.isEmpty()) {
                return "balanced and friendly";
            }

            return String.join(", ", patterns);
        }
    }

    private final PersonalityProfile personality;

    public PersonalitySystem() {
        this.personality = new PersonalityProfile();
    }

    /**
     * Gets the personality profile.
     */
    public PersonalityProfile getPersonality() {
        return personality;
    }

    /**
     * Parses a mood string into a Mood enum value.
     */
    public static CompanionMemory.Mood parseMood(String moodString) {
        if (moodString == null) {
            return CompanionMemory.Mood.CHEERFUL;
        }
        try {
            return CompanionMemory.Mood.valueOf(moodString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CompanionMemory.Mood.CHEERFUL;
        }
    }
}
