package com.minewright.memory.milestone;

import com.minewright.memory.CompanionMemory;
import com.minewright.memory.MilestoneTracker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Generates milestone celebration messages for LLM prompts.
 *
 * <p>This class handles the creation of personalized milestone messages
 * based on relationship depth, personality, and milestone type.</p>
 */
public class MilestoneMessageGenerator {

    /**
     * Generates a milestone celebration prompt for the LLM.
     * Includes enhanced personality context, speech patterns, and relationship depth.
     */
    public String generateMilestoneMessage(MilestoneTracker.Milestone milestone, CompanionMemory memory) {
        int rapport = memory.getRapportLevel();
        String playerName = memory.getPlayerName() != null ? memory.getPlayerName() : "friend";
        CompanionMemory.PersonalityProfile personality = memory.getPersonality();

        StringBuilder prompt = new StringBuilder();

        // Determine formality and intimacy levels
        String formalityLevel = getFormalityLevel(personality.getFormality(), rapport);
        String intimacyLevel = getIntimacyLevel(rapport);

        prompt.append("[MILESTONE CELEBRATION - ").append(milestone.type).append("]\n");
        prompt.append("The following milestone has been reached. Express genuine celebration ");
        prompt.append("in a ").append(formalityLevel).append(" tone with ").append(intimacyLevel).append(" warmth.\n\n");

        prompt.append("MILESTONE DETAILS:\n");
        prompt.append("- Title: ").append(milestone.title).append("\n");
        prompt.append("- Description: ").append(milestone.description).append("\n");
        prompt.append("- Importance: ").append(milestone.importance).append("/10\n");
        prompt.append("- Type: ").append(milestone.type).append("\n\n");

        prompt.append("RELATIONSHIP CONTEXT:\n");
        prompt.append("- Player Name: ").append(playerName).append("\n");
        prompt.append("- Rapport Level: ").append(rapport).append("/100 (").append(getRelationshipLabel(rapport)).append(")\n");
        prompt.append("- Interaction Count: ").append(memory.getInteractionCount()).append("\n");
        prompt.append("- Shared Milestones: ").append(memory.getMilestones().size()).append("\n");

        if (memory.getFirstMeeting() != null) {
            long days = ChronoUnit.DAYS.between(memory.getFirstMeeting(), Instant.now());
            prompt.append("- Time Together: ").append(days).append(" days\n");
        }
        prompt.append("\n");

        prompt.append("PERSONALITY CONTEXT:\n");
        prompt.append(personality.toPromptContext()).append("\n");

        prompt.append("SPEECH PATTERNS:\n");
        prompt.append("- Speech Style: ").append(personality.getSpeechPatternDescription()).append("\n");
        var verbalTics = personality.getVerbalTics();
        if (!verbalTics.isEmpty()) {
            prompt.append("- Verbal Tics: ").append(String.join(", ", verbalTics)).append("\n");
        }
        prompt.append("\n");

        // Add milestone-specific guidance
        prompt.append(getMilestoneGuidance(milestone, rapport, personality)).append("\n");

        // Add rapport-specific emotional guidance
        prompt.append(getRapportGuidance(rapport, personality)).append("\n");

        prompt.append("\nINSTRUCTIONS:\n");
        prompt.append("Generate a natural, in-character celebration message. ");
        prompt.append("Make it personal and specific to this milestone. ");
        prompt.append("Use catchphrases or verbal tics occasionally if appropriate. ");
        prompt.append("Speak directly to ").append(playerName);
        prompt.append(" as someone who has shared this journey with them.");

        return prompt.toString();
    }

    /**
     * Gets the formality level description.
     */
    private String getFormalityLevel(int formality, int rapport) {
        if (rapport > 70) {
            return "very casual and intimate";
        } else if (formality > 60) {
            return "respectful and structured";
        } else if (formality > 40) {
            return "balanced and friendly";
        } else {
            return "casual and relaxed";
        }
    }

    /**
     * Gets the intimacy level description based on rapport.
     */
    private String getIntimacyLevel(int rapport) {
        if (rapport < 30) return "polite";
        if (rapport < 50) return "friendly";
        if (rapport < 70) return "warm";
        if (rapport < 85) return "affectionate";
        return "deeply emotional";
    }

    /**
     * Gets the relationship label for rapport level.
     */
    private String getRelationshipLabel(int rapport) {
        if (rapport < 30) return "new acquaintance";
        if (rapport < 50) return "casual friend";
        if (rapport < 70) return "trusted friend";
        if (rapport < 85) return "close companion";
        return "family";
    }

    /**
     * Gets milestone-specific celebration guidance.
     */
    private String getMilestoneGuidance(MilestoneTracker.Milestone milestone, int rapport,
                                       CompanionMemory.PersonalityProfile personality) {
        StringBuilder guidance = new StringBuilder();
        guidance.append("CELEBRATION APPROACH:\n");

        switch (milestone.type) {
            case FIRST:
                guidance.append("- Emphasize the novelty and excitement of this first-time experience\n");
                guidance.append("- Reference how this marks the beginning of our journey together\n");
                if (personality.getExtraversion() > 60) {
                    guidance.append("- Show enthusiasm and wonder at this new experience\n");
                }
                break;

            case ANNIVERSARY:
                guidance.append("- Reflect on how far we've come since we met\n");
                guidance.append("- Acknowledge the time we've spent building together\n");
                if (rapport > 60) {
                    guidance.append("- Express appreciation for the bond we've developed\n");
                }
                if (personality.getNeuroticism() > 50) {
                    guidance.append("- Add a touch of sentimentality about time passing\n");
                }
                break;

            case COUNT:
                guidance.append("- Celebrate the consistency and dedication this represents\n");
                guidance.append("- Acknowledge the effort we've both put in\n");
                if (personality.getConscientiousness() > 70) {
                    guidance.append("- Emphasize the value of persistence and hard work\n");
                }
                break;

            case ACHIEVEMENT:
                guidance.append("- Celebrate the skill and accomplishment this represents\n");
                guidance.append("- Acknowledge the challenge we overcame together\n");
                if (personality.getHumor() > 60) {
                    guidance.append("- Add a touch of humor about the journey to get here\n");
                }
                break;
        }

        return guidance.toString();
    }

    /**
     * Gets rapport-specific emotional guidance.
     */
    private String getRapportGuidance(int rapport, CompanionMemory.PersonalityProfile personality) {
        StringBuilder guidance = new StringBuilder();
        guidance.append("EMOTIONAL TONE:\n");

        if (rapport < 30) {
            guidance.append("- Keep it positive but professional\n");
            guidance.append("- Show genuine happiness for the achievement\n");
            guidance.append("- Maintain appropriate boundaries while celebrating\n");
        } else if (rapport < 50) {
            guidance.append("- Show genuine warmth and friendship\n");
            guidance.append("- Express appreciation for working together\n");
            guidance.append("- Include lighthearted references to our shared experiences\n");
        } else if (rapport < 70) {
            guidance.append("- Express deep appreciation for our friendship\n");
            guidance.append("- Reference specific memories we've made together\n");
            guidance.append("- Show emotional investment in our partnership\n");
        } else if (rapport < 85) {
            guidance.append("- Express strong attachment and affection\n");
            guidance.append("- Celebrate not just the milestone, but our relationship\n");
            guidance.append("- Use more emotional and heartfelt language\n");
        } else {
            guidance.append("- Express profound emotional connection\n");
            guidance.append("- Celebrate the depth of our bond\n");
            guidance.append("- Use intimate language appropriate for close family\n");
            guidance.append("- This milestone represents our life together\n");
        }

        return guidance.toString();
    }
}
