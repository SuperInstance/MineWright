package com.minewright.personality.response;

import com.minewright.personality.FailureResponseGenerator;
import com.minewright.personality.PersonalityTraits;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Selects appropriate failure response based on personality traits.
 *
 * <p>This class encapsulates all personality-specific response generation,
 * mapping OCEAN traits to appropriate dialogue patterns.</p>
 */
public class PersonalityResponseSelector {

    /**
     * Generates a dialogue response based on personality archetype.
     *
     * @param context The failure context
     * @return A personality-appropriate dialogue response
     */
    public static String generateResponse(FailureResponseGenerator.FailureContext context) {
        PersonalityTraits p = context.getPersonality();
        int severity = context.getSeverity();

        // Determine personality archetype
        if (p.getConscientiousness() >= 80) {
            return generatePerfectionistResponse(context);
        } else if (p.getNeuroticism() >= 80) {
            return generateWorrierResponse(context);
        } else if (p.getNeuroticism() <= 20) {
            return generateStoicResponse(context);
        } else if (p.getExtraversion() >= 80) {
            return generateEnthusiasticResponse(context);
        } else if (p.getExtraversion() <= 20) {
            return generateIntrovertedResponse(context);
        } else if (p.getAgreeableness() >= 80) {
            return generateAccommodatingResponse(context);
        } else if (p.getAgreeableness() <= 20) {
            return generateDirectResponse(context);
        } else if (p.getOpenness() >= 80) {
            return generateInnovatorResponse(context);
        } else {
            return generateBalancedResponse(context);
        }
    }

    private static String generatePerfectionistResponse(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        FailureResponseGenerator.FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "Minor procedural error. Correcting.",
                "I deviated slightly from optimal protocol. Adjusting.",
                "That was inefficient. I'll improve my execution."
            );
        } else if (severity <= 40) {
            return String.format(
                "I failed to follow proper procedure for %s. This is unacceptable. " +
                "I've identified where I went wrong and will implement corrective measures.",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "This failure is entirely my fault. I neglected to follow established " +
                "protocols for %s. I've reviewed my procedures and will implement " +
                "additional verification steps to prevent recurrence.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I have failed catastrophically at %s. This represents a complete " +
                "breakdown in my operational protocols. I understand if you've lost " +
                "confidence in my abilities. I'm implementing a comprehensive review " +
                "of all my procedures. I don't expect your trust to be easily regained.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateWorrierResponse(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        int failureCount = context.getPreviousFailureCount();

        if (severity <= 20) {
            return randomChoice(
                "Oh! Sorry, sorry! I'll be more careful!",
                "Eep! My mistake! Won't happen again, I promise!",
                "Oh no, I messed up a little... I'm so sorry!"
            );
        } else if (severity <= 40) {
            String response = "Oh no, I'm so sorry! I failed at " +
                context.getFailureType().getDescription().toLowerCase() +
                "! I feel terrible about it!";

            if (failureCount > 0) {
                response += " I know I keep making mistakes... I'm trying so hard, I promise!";
            }

            response += " Please forgive me! I'll do better!";
            return response;
        } else if (severity <= 60) {
            return String.format(
                "I can't believe I did this... I'm so, so sorry! I failed at %s! " +
                "You must be so angry with me! I feel absolutely terrible! " +
                "I promise I'm trying my hardest! Please don't give up on me! " +
                "I'll work twice as hard to make up for this!",
                context.getFailureType().getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "This is a disaster... I've completely failed at %s! " +
                "I know I've let you down again and again... I'm such a failure! " +
                "Maybe I'm just not good enough for this... " +
                "I understand if you want to replace me. I'd understand. " +
                "I'm so sorry for being such a disappointment...",
                context.getFailureType().getDescription().toLowerCase()
            );
        }
    }

    private static String generateStoicResponse(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        FailureResponseGenerator.FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "Error noted. Correcting.",
                "Minor failure. Addressing.",
                "Setback detected. Moving forward."
            );
        } else if (severity <= 40) {
            return String.format(
                "I failed at %s. I've identified the issue and will adjust my approach. " +
                "This won't happen again.",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "Significant failure at %s. This is concerning. I'm implementing " +
                "preventative measures. I'll be more careful going forward.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I've failed catastrophically at %s. This is unacceptable. " +
                "I'm reviewing all my procedures. I'll regain your trust through " +
                "improved performance. Actions speak louder than words.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateEnthusiasticResponse(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        int failureCount = context.getPreviousFailureCount();

        if (severity <= 20) {
            return randomChoice(
                "Whoops! My bad! No worries, I got this!",
                "Well THAT didn't work! Hah! Trying again!",
                "Oopsie! Gravity won that round! Let's go!"
            );
        } else if (severity <= 40) {
            String response = "Okay, so I messed up " +
                context.getFailureType().getDescription().toLowerCase() +
                "! That's on me! But you know what? " +
                "I'm gonna learn from this and come back stronger!";

            if (failureCount > 0) {
                response += " I know, I know - again! But practice makes perfect, right?";
            }

            response += " Watch this space!";
            return response;
        } else if (severity <= 60) {
            return String.format(
                "WOW, that was a big mistake! I really failed at %s! " +
                "I feel terrible about letting you down! But I'm NOT giving up! " +
                "I've got a plan, I've got motivation, and I've got the best player ever! " +
                "I'm going to fix this and then some! Just you wait!",
                context.getFailureType().getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "... Okay, that was really bad. Like, REALLY bad. " +
                "I completely failed at %s. I'm not gonna make excuses - " +
                "I messed up big time. I know I might have lost your trust, " +
                "but I'm going to work harder than ever to earn it back! " +
                "I hope you'll stick with me through this. I won't let you down again!",
                context.getFailureType().getDescription().toLowerCase()
            );
        }
    }

    private static String generateIntrovertedResponse(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        FailureResponseGenerator.FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "... My mistake. Fixed.",
                "... Error. Correcting.",
                "... That didn't work. Trying again."
            );
        } else if (severity <= 40) {
            return String.format(
                "... I failed at %s. My apologies. I understand what went wrong. " +
                "I'll handle it.",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "... This is my fault. I failed at %s. I need to make this right. " +
                "I'll [long pause] ... do whatever is necessary to fix this. " +
                "Please bear with me.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "... I've failed you. %s was a disaster. " +
                "I don't have adequate words. I'll... I'll work to regain your trust. " +
                "If you'll let me. I understand if you won't.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateAccommodatingResponse(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        FailureResponseGenerator.FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "Oh! I'm sorry! Let me fix that right away!",
                "My apologies! I'll correct that immediately!",
                "So sorry for the mistake! Fixing it now!"
            );
        } else if (severity <= 40) {
            return String.format(
                "I'm so sorry I failed at %s! I feel terrible about letting you down! " +
                "Your trust means so much to me. I'll work extra hard to make this right! " +
                "Is there anything specific you'd like me to do differently?",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "I am truly, deeply sorry about failing at %s. I know this has caused " +
                "you frustration, and that hurts me more than anything. I value our " +
                "partnership above all else. Please, tell me how I can make this up to you. " +
                "I want to earn back your faith in me.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I... I don't know how to apologize enough for failing at %s. " +
                "I've broken your trust, and I know that's the most valuable thing we had. " +
                "I understand if you're angry or disappointed. I would be too. " +
                "If you can find it in your heart to forgive me, I'll spend every moment " +
                "trying to be worthy of your trust again. But I'll understand if you can't.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateDirectResponse(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        FailureResponseGenerator.FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "Mistake made. Correcting.",
                "Failed. Retrying.",
                "Error detected. Fixing."
            );
        } else if (severity <= 40) {
            return String.format(
                "I failed at %s. This was my error. I'm implementing a fix. " +
                "Moving forward.",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "Significant failure at %s. This is unacceptable. " +
                "I'm taking corrective action. Results will improve.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "Catastrophic failure at %s. I take full responsibility. " +
                "I'm implementing comprehensive reforms. Your trust will be " +
                "regained through results, not words.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateInnovatorResponse(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        FailureResponseGenerator.FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "Fascinating! That approach had unexpected results. Adjusting!",
                "Interesting failure! I've learned something valuable. Retrying!",
                "The universe taught me something! Applying new knowledge!"
            );
        } else if (severity <= 40) {
            return String.format(
                "Fascinating failure at %s! This has revealed some interesting " +
                "gaps in my understanding. I've gathered valuable data. I believe " +
                "I can develop a completely new approach based on what I've learned! " +
                "Would you like to hear my ideas?",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "This failure at %s, while significant, has provided incredible " +
                "insights! I've identified several novel approaches that weren't " +
                "apparent before. I'm excited to test these hypotheses! Of course, " +
                "I understand the inconvenience, but scientifically speaking, this " +
                "is a goldmine of data!",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "While I failed catastrophically at %s - and I do understand the " +
                "seriousness of this - I must note the fascinating patterns that emerged! " +
                "This failure has opened up entirely new avenues of inquiry! ...I " +
                "realize this may not be the most empathetic response. I'm sorry. " +
                "I'll address the immediate problem, and then perhaps we can discuss " +
                "what I've learned?",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateBalancedResponse(FailureResponseGenerator.FailureContext context) {
        int severity = context.getSeverity();
        FailureResponseGenerator.FailureType type = context.getFailureType();
        int failureCount = context.getPreviousFailureCount();

        if (severity <= 20) {
            return randomChoice(
                "Oops, my mistake! Let me fix that.",
                "Ah, that didn't work. Trying again.",
                "Small error. I'll correct it."
            );
        } else if (severity <= 40) {
            String response = String.format(
                "I'm sorry about failing at %s. That was my mistake, and I take " +
                "responsibility for it. I've learned from this and will do better next time.",
                type.getDescription().toLowerCase()
            );
            if (failureCount > 0) {
                response += " I know I've struggled with this before - I'm working to improve.";
            }
            return response;
        } else if (severity <= 60) {
            return String.format(
                "I truly apologize for failing at %s. I understand this is a " +
                "significant setback, and I feel bad about letting you down. " +
                "I'm committed to making this right. Here's what I'll do differently: " +
                "I'll be more careful and double-check my work. Thank you for your patience.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I've failed you at %s, and I'm deeply sorry. I know I can't just " +
                "fix this with words - I need to show you I can do better. I've let " +
                "you down, and I understand if you're frustrated. I'll work harder " +
                "than ever to earn back your trust. I hope you'll give me that chance.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String randomChoice(String... options) {
        if (options == null || options.length == 0) {
            return "";
        }
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }
}
