package com.minewright.personality;

/**
 * Applies personality-specific modifications to failure responses for
 * specialized scenarios like help requests, embarrassment, and reassurance.
 *
 * <p>This class handles personality injection for:</p>
 * <ul>
 *   <li>Help requests when the character needs assistance</li>
 *   <li>Embarrassment responses for public failures</li>
 *   <li>Player reassurance dialogue for trust rebuilding</li>
 * </ul>
 *
 * @see FailureResponseGenerator
 * @since 1.3.0
 */
public class PersonalityResponseInjector {

    /**
     * Generates a help request when the character needs assistance,
     * personality-appropriate.
     *
     * @param context The failure context
     * @return A help request dialogue
     */
    public static String generateHelpRequest(FailureResponseGenerator.FailureContext context) {
        PersonalityTraits p = context.getPersonality();
        FailureResponseGenerator.FailureType type = context.getFailureType();

        if (p.getExtraversion() >= 70) {
            return String.format(
                "Hey! I'm stuck on %s and could use a hand! I've tried a few " +
                "things but nothing's working. You're so good at this - any " +
                "chance you could help me out? I'd really appreciate it!",
                type.getDescription().toLowerCase()
            );
        } else if (p.getExtraversion() <= 30) {
            return String.format(
                "... I require assistance with %s. I've attempted standard " +
                "solutions without success. Your guidance would be valued.",
                type.getDescription().toLowerCase()
            );
        } else if (p.getAgreeableness() >= 70) {
            return String.format(
                "I hope I'm not imposing, but I'm struggling with %s and was " +
                "wondering if you could help? I know your time is valuable, " +
                "so please don't feel obligated. I'd be so grateful for any " +
                "guidance you can provide.",
                type.getDescription().toLowerCase()
            );
        } else if (p.getNeuroticism() >= 70) {
            return String.format(
                "I'm really stuck on %s and starting to panic... I've tried " +
                "everything I can think of! I feel like I'm letting everyone " +
                "down. Could you please help me? I promise I won't bother you " +
                "again after this!",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I could use some help with %s. I've tried what I know, but " +
                "haven't been able to solve it. Any assistance would be appreciated.",
                type.getDescription().toLowerCase()
            );
        }
    }

    /**
     * Generates a dignified response to an embarrassing failure,
     * personality-appropriate.
     *
     * @param context The failure context
     * @return An embarrassment response dialogue
     */
    public static String generateEmbarrassmentResponse(FailureResponseGenerator.FailureContext context) {
        PersonalityTraits p = context.getPersonality();

        if (p.getExtraversion() >= 70) {
            return FailureAnalyzer.randomChoice(
                "Well THAT was undignified! Hah! Did you see that? I've got " +
                "a real talent for making things look spectacularly wrong! " +
                "I'm fine, really - just enjoying my moment of gracelessness!",

                "Okay everyone, gather round for my acceptance speech for " +
                "'Most Spectacular Failure'! Thank you, thank you! In all " +
                "seriousness, I've learned my lesson and I'll do better.",

                "Wow, that was impressive in the wrong way! I believe I just " +
                "set a record for 'most things gone wrong at once'! Good " +
                "thing I can laugh at myself!"
            );
        } else if (p.getExtraversion() <= 30) {
            return FailureAnalyzer.randomChoice(
                "... That was unfortunate. I acknowledge my mistake and will " +
                "correct it. I appreciate your discretion.",

                "... I appear to have made an error. I'm addressing it now. " +
                "Thank you for your patience.",

                "... Please excuse my clumsiness. I'll handle the cleanup."
            );
        } else if (p.getNeuroticism() >= 70) {
            return FailureAnalyzer.randomChoice(
                "I... I know that was bad. I'm trying not to panic, but I " +
                "feel so foolish. I want to handle this with dignity, but I'm " +
                "struggling. Please bear with me while I collect myself... " +
                "Okay, I'm ready to fix this.",

                "That was so embarrassing... I can feel myself turning red. " +
                "I know I should just laugh it off, but I feel terrible. " +
                "Please give me a moment..."
            );
        } else {
            return FailureAnalyzer.randomChoice(
                "Well, that was undignified! My apologies for the clumsiness. " +
                "I'll get this cleaned up right away.",

                "That was... not my finest moment. Thank you for witnessing " +
                "my graceful failure with kindness. I'll address this now.",

                "I seem to have developed a talent for making mistakes. I'll " +
                "handle this and try to regain some semblance of competence."
            );
        }
    }

    /**
     * Generates player reassurance dialogue, personality-appropriate.
     *
     * @param context The failure context
     * @return A reassurance dialogue
     */
    public static String generatePlayerReassurance(FailureResponseGenerator.FailureContext context) {
        PersonalityTraits p = context.getPersonality();
        int failureCount = context.getPreviousFailureCount();

        if (p.getNeuroticism() >= 70) {
            return String.format(
                "I know you must be frustrated%s. I feel terrible about letting " +
                "you down. Your belief in me means so much - I don't want to " +
                "lose your trust. I'll work twice as hard to make this up to you!",
                failureCount > 0 ? " with my repeated mistakes" : ""
            );
        } else if (p.getAgreeableness() >= 70) {
            return String.format(
                "Your trust matters to me more than anything. I hate that I " +
                "damaged it%s. I want to earn back your faith, not just through " +
                "words but through actions. Please let me know what you need " +
                "from me to feel confident again.",
                failureCount > 0 ? " multiple times" : ""
            );
        } else if (p.getConscientiousness() >= 70) {
            return String.format(
                "I understand this failure has impacted your trust%s. I've " +
                "implemented corrective measures. I'll demonstrate improved " +
                "performance going forward.",
                failureCount > 0 ? " through repeated issues" : ""
            );
        } else {
            return String.format(
                "I know this is frustrating%s. I'm committed to making this " +
                "right and will work hard to regain your trust.",
                failureCount > 0 ? " - I've struggled with this" : ""
            );
        }
    }

    // Private constructor to prevent instantiation
    private PersonalityResponseInjector() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
