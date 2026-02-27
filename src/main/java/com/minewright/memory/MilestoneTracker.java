package com.minewright.memory;

import com.minewright.entity.ForemanEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks and celebrates relationship milestones between MineWright and the player.
 *
 * <p>This system recognizes "firsts", anniversaries, and achievement milestones
 * to create meaningful moments in the companion relationship.</p>
 *
 * <p><b>Milestone Types:</b></p>
 * <ul>
 *   <li><b>FIRST:</b> First-time experiences together</li>
 *   <li><b>ANNIVERSARY:</b> Time-based milestones (days, weeks, months)</li>
 *   <li><b>COUNT:</b> Quantity milestones (100th interaction, 1000th block)</li>
 *   <li><b>ACHIEVEMENT:</b> Special accomplishments</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class MilestoneTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MilestoneTracker.class);

    // === Milestone Storage ===

    /**
     * All achieved milestones, keyed by unique ID.
     */
    private final Map<String, Milestone> achievedMilestones;

    /**
     * Pending milestones to be announced.
     */
    private final Queue<Milestone> pendingMilestones;

    /**
     * First occurrences of events - for detecting "first" milestones.
     */
    private final Map<String, Instant> firstOccurrences;

    /**
     * Counter-based milestones - for tracking counts.
     */
    private final Map<String, Integer> counters;

    /**
     * Last anniversary check timestamp.
     */
    private Instant lastAnniversaryCheck;

    /**
     * Creates a new MilestoneTracker.
     */
    public MilestoneTracker() {
        this.achievedMilestones = new ConcurrentHashMap<>();
        this.pendingMilestones = new LinkedList<>();
        this.firstOccurrences = new ConcurrentHashMap<>();
        this.counters = new ConcurrentHashMap<>();
        this.lastAnniversaryCheck = Instant.now();
    }

    // === Milestone Checking ===

    /**
     * Checks if an event triggers any milestones.
     *
     * @param minewright The MineWright entity
     * @param eventType The type of event
     * @param context Additional context for the event
     * @return Optional milestone if one was reached
     */
    public Optional<Milestone> checkMilestone(ForemanEntity minewright, String eventType, Object context) {
        CompanionMemory memory = minewright.getCompanionMemory();
        String playerName = memory.getPlayerName();

        if (playerName == null) {
            return Optional.empty();
        }

        // Check for "first" milestones
        Optional<Milestone> firstMilestone = checkFirstMilestone(minewright, eventType, context);
        if (firstMilestone.isPresent()) {
            return firstMilestone;
        }

        // Update counters and check count-based milestones
        Optional<Milestone> countMilestone = checkCountMilestone(minewright, eventType, context);
        if (countMilestone.isPresent()) {
            return countMilestone;
        }

        // Check for achievement milestones
        Optional<Milestone> achievementMilestone = checkAchievementMilestone(minewright, eventType, context);
        if (achievementMilestone.isPresent()) {
            return achievementMilestone;
        }

        return Optional.empty();
    }

    /**
     * Checks for time-based anniversaries.
     * Should be called periodically (e.g., once per game session).
     *
     * @param minewright The MineWright entity
     * @return Optional milestone if an anniversary was reached
     */
    public Optional<Milestone> checkAnniversaries(ForemanEntity minewright) {
        CompanionMemory memory = minewright.getCompanionMemory();
        Instant firstMeeting = memory.getFirstMeeting();

        if (firstMeeting == null) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        long daysSince = ChronoUnit.DAYS.between(firstMeeting, now);

        // Check only once per hour to avoid spam
        if (ChronoUnit.MINUTES.between(lastAnniversaryCheck, now) < 60) {
            return Optional.empty();
        }
        lastAnniversaryCheck = now;

        // Define anniversary milestones
        int[] anniversaryDays = {7, 14, 30, 60, 90, 100, 180, 365, 500, 730, 1000};

        for (int days : anniversaryDays) {
            String milestoneId = "anniversary_" + days + "_days";
            if (!hasMilestone(milestoneId) && daysSince >= days) {
                Milestone milestone = new Milestone(
                    milestoneId,
                    MilestoneType.ANNIVERSARY,
                    days + " Days Together!",
                    String.format("We've been together for %d days! %s",
                        days, getAnniversaryMessage(days)),
                    days,
                    now
                );
                recordMilestone(milestone, memory);
                return Optional.of(milestone);
            }
        }

        return Optional.empty();
    }

    /**
     * Checks for "first" milestones.
     */
    private Optional<Milestone> checkFirstMilestone(ForemanEntity minewright, String eventType, Object context) {
        String firstKey = "first_" + eventType;
        CompanionMemory memory = minewright.getCompanionMemory();
        String playerName = memory.getPlayerName();

        if (!firstOccurrences.containsKey(firstKey)) {
            firstOccurrences.put(firstKey, Instant.now());

            String milestoneId = "milestone_first_" + eventType;
            String title = getFirstMilestoneTitle(eventType);
            String description = getFirstMilestoneDescription(eventType, playerName, context);

            Milestone milestone = new Milestone(
                milestoneId,
                MilestoneType.FIRST,
                title,
                description,
                10, // High importance for "first" milestones
                Instant.now()
            );

            recordMilestone(milestone, memory);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    /**
     * Checks for count-based milestones.
     */
    private Optional<Milestone> checkCountMilestone(ForemanEntity minewright, String eventType, Object context) {
        String counterKey = "count_" + eventType;
        CompanionMemory memory = minewright.getCompanionMemory();
        int newCount = counters.merge(counterKey, 1, Integer::sum);

        // Define count milestones
        int[] countMilestones = {10, 25, 50, 100, 250, 500, 1000};

        for (int threshold : countMilestones) {
            if (newCount == threshold) {
                String milestoneId = "milestone_count_" + eventType + "_" + threshold;
                String title = getCountMilestoneTitle(eventType, threshold);
                String description = getCountMilestoneDescription(eventType, threshold);

                Milestone milestone = new Milestone(
                    milestoneId,
                    MilestoneType.COUNT,
                    title,
                    description,
                    Math.min(10, threshold / 50), // Importance scales with count
                    Instant.now()
                );

                recordMilestone(milestone, memory);
                return Optional.of(milestone);
            }
        }

        return Optional.empty();
    }

    /**
     * Checks for achievement milestones.
     */
    private Optional<Milestone> checkAchievementMilestone(ForemanEntity minewright, String eventType, Object context) {
        CompanionMemory memory = minewright.getCompanionMemory();
        String playerName = memory.getPlayerName();

        // Special milestone checks based on event type
        switch (eventType.toLowerCase()) {
            case "diamond_found":
                return checkDiamondMilestone(minewright, context);

            case "nether_visit":
                return checkNetherMilestone(minewright);

            case "structure_built":
                return checkStructureMilestone(minewright, context);

            case "enemy_defeated":
                return checkCombatMilestone(minewright, context);

            case "night_survived":
                return checkNightSurvivalMilestone(minewright);

            case "gift_exchanged":
                return checkGiftMilestone(minewright, context);

            default:
                return Optional.empty();
        }
    }

    // === Individual Milestone Checks ===

    private Optional<Milestone> checkDiamondMilestone(ForemanEntity minewright, Object context) {
        String milestoneId = "milestone_first_diamond";

        if (!hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();

            Milestone milestone = new Milestone(
                milestoneId,
                MilestoneType.FIRST,
                "First Diamond!",
                playerName + ", we found our first diamond together! This is big! " +
                "Diamonds are rare and valuable - this marks our growth as a team.",
                10,
                Instant.now()
            );

            recordMilestone(milestone, memory);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    private Optional<Milestone> checkNetherMilestone(ForemanEntity minewright) {
        String milestoneId = "milestone_first_nether";

        if (!hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();

            Milestone milestone = new Milestone(
                milestoneId,
                MilestoneType.FIRST,
                "Nether Portal crossed!",
                "We just entered the Nether together, " + playerName + "! " +
                "A dangerous new dimension awaits. I'll stick close to you.",
                10,
                Instant.now()
            );

            recordMilestone(milestone, memory);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    private Optional<Milestone> checkStructureMilestone(ForemanEntity minewright, Object context) {
        String structureType = context instanceof String ? (String) context : "structure";
        String milestoneId = "milestone_first_structure_" + structureType.toLowerCase();

        if (!hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();

            Milestone milestone = new Milestone(
                milestoneId,
                MilestoneType.FIRST,
                "First " + structureType + " Built!",
                "We built our first " + structureType + " together, " + playerName + "! " +
                "Working side by side to create something lasting... that's what partnership is about.",
                9,
                Instant.now()
            );

            recordMilestone(milestone, memory);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    private Optional<Milestone> checkCombatMilestone(ForemanEntity minewright, Object context) {
        String milestoneId = "milestone_first_combat";

        if (!hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();

            Milestone milestone = new Milestone(
                milestoneId,
                MilestoneType.FIRST,
                "First Battle Together!",
                "We fought our first battle together, " + playerName + "! " +
                "Facing danger side by side... I've got your back, partner.",
                8,
                Instant.now()
            );

            recordMilestone(milestone, memory);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    private Optional<Milestone> checkNightSurvivalMilestone(ForemanEntity minewright) {
        String milestoneId = "milestone_first_night";

        if (!hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();

            Milestone milestone = new Milestone(
                milestoneId,
                MilestoneType.FIRST,
                "First Night Survived!",
                "We survived our first night together, " + playerName + "! " +
                "The darkness is dangerous, but we made it through safely.",
                7,
                Instant.now()
            );

            recordMilestone(milestone, memory);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    private Optional<Milestone> checkGiftMilestone(ForemanEntity minewright, Object context) {
        String milestoneId = "milestone_first_gift";

        if (!hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();
            String item = context instanceof String ? (String) context : "gift";

            Milestone milestone = new Milestone(
                milestoneId,
                MilestoneType.FIRST,
                "First Gift Exchanged!",
                playerName + ", this " + item + "... you gave this to me? " +
                "I'll treasure it. It means a lot that you thought of me.",
                10,
                Instant.now()
            );

            recordMilestone(milestone, memory);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    // === Message Generation ===

    /**
     * Generates a milestone celebration prompt for the LLM.
     * Now includes enhanced personality context, speech patterns, and relationship depth.
     *
     * @param milestone The milestone to celebrate
     * @param memory The companion memory for context
     * @return A celebration prompt for the LLM
     */
    public String generateMilestoneMessage(Milestone milestone, CompanionMemory memory) {
        int rapport = memory.getRapportLevel();
        String playerName = memory.getPlayerName() != null ? memory.getPlayerName() : "friend";
        CompanionMemory.PersonalityProfile personality = memory.getPersonality();

        StringBuilder prompt = new StringBuilder();

        // Determine formality and intimacy levels
        String formalityLevel = getFormalityLevel(personality.formality, rapport);
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
        if (!personality.verbalTics.isEmpty()) {
            prompt.append("- Verbal Tics: ").append(String.join(", ", personality.verbalTics)).append("\n");
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
    private String getMilestoneGuidance(Milestone milestone, int rapport,
                                       CompanionMemory.PersonalityProfile personality) {
        StringBuilder guidance = new StringBuilder();
        guidance.append("CELEBRATION APPROACH:\n");

        switch (milestone.type) {
            case FIRST:
                guidance.append("- Emphasize the novelty and excitement of this first-time experience\n");
                guidance.append("- Reference how this marks the beginning of our journey together\n");
                if (personality.extraversion > 60) {
                    guidance.append("- Show enthusiasm and wonder at this new experience\n");
                }
                break;

            case ANNIVERSARY:
                guidance.append("- Reflect on how far we've come since we met\n");
                guidance.append("- Acknowledge the time we've spent building together\n");
                if (rapport > 60) {
                    guidance.append("- Express appreciation for the bond we've developed\n");
                }
                if (personality.neuroticism > 50) {
                    guidance.append("- Add a touch of sentimentality about time passing\n");
                }
                break;

            case COUNT:
                guidance.append("- Celebrate the consistency and dedication this represents\n");
                guidance.append("- Acknowledge the effort we've both put in\n");
                if (personality.conscientiousness > 70) {
                    guidance.append("- Emphasize the value of persistence and hard work\n");
                }
                break;

            case ACHIEVEMENT:
                guidance.append("- Celebrate the skill and accomplishment this represents\n");
                guidance.append("- Acknowledge the challenge we overcame together\n");
                if (personality.humor > 60) {
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

    // === Helper Methods ===

    /**
     * Records a milestone in memory.
     */
    private void recordMilestone(Milestone milestone, CompanionMemory memory) {
        achievedMilestones.put(milestone.id, milestone);
        pendingMilestones.add(milestone);

        // Also record as an episodic memory
        memory.recordExperience(
            "milestone",
            milestone.title + ": " + milestone.description,
            milestone.importance
        );

        // Boost rapport for milestone achievement
        memory.adjustRapport(Math.min(5, milestone.importance));

        LOGGER.info("Milestone achieved: {} - {}", milestone.id, milestone.title);
    }

    /**
     * Checks if a specific milestone has been achieved.
     *
     * @param milestoneId The milestone ID to check
     * @return true if the milestone has been achieved
     */
    public boolean hasMilestone(String milestoneId) {
        return achievedMilestones.containsKey(milestoneId);
    }

    /**
     * Gets all achieved milestones.
     *
     * @return List of all achieved milestones
     */
    public List<Milestone> getMilestones() {
        return new ArrayList<>(achievedMilestones.values());
    }

    /**
     * Gets pending milestones that haven't been announced yet.
     *
     * @return List of pending milestones
     */
    public List<Milestone> getPendingMilestones() {
        List<Milestone> pending = new ArrayList<>();
        Milestone milestone;
        while ((milestone = pendingMilestones.poll()) != null) {
            pending.add(milestone);
        }
        return pending;
    }

    /**
     * Gets the next pending milestone without removing it.
     *
     * @return Optional next pending milestone
     */
    public Optional<Milestone> peekPendingMilestone() {
        return Optional.ofNullable(pendingMilestones.peek());
    }

    /**
     * Clears all pending milestones.
     */
    public void clearPendingMilestones() {
        pendingMilestones.clear();
    }

    // === Milestone Title/Description Generators ===

    private String getFirstMilestoneTitle(String eventType) {
        switch (eventType.toLowerCase()) {
            case "task_completed":
                return "First Task Together!";
            case "night_survived":
                return "First Night Survived!";
            case "diamond_found":
                return "First Diamond!";
            case "nether_visit":
                return "Nether Portal Crossed!";
            case "structure_built":
                return "First Structure Built!";
            case "enemy_defeated":
                return "First Battle Together!";
            case "gift_exchanged":
                return "First Gift Exchanged!";
            default:
                return "First " + eventType.replace("_", " ") + "!";
        }
    }

    private String getFirstMilestoneDescription(String eventType, String playerName, Object context) {
        String contextStr = context != null ? context.toString() : "";

        switch (eventType.toLowerCase()) {
            case "task_completed":
                return "We completed our first task together, " + playerName + "! " +
                       "This is just the beginning of what we can accomplish as a team.";
            case "night_survived":
                return "We survived our first night together, " + playerName + "! " +
                       "The darkness is dangerous, but we made it through safely.";
            case "gift_exchanged":
                return playerName + ", this " + contextStr + "... you gave this to me? " +
                       "I'll treasure it. It means a lot that you thought of me.";
            default:
                return "This is our first time " + eventType.replace("_", " ") +
                       " together, " + playerName + "! A memorable moment.";
        }
    }

    private String getCountMilestoneTitle(String eventType, int count) {
        String event = eventType.replace("count_", "").replace("_", " ");
        return count + getOrdinalSuffix(count) + " " + event + "!";
    }

    private String getCountMilestoneDescription(String eventType, int count) {
        String event = eventType.replace("count_", "").replace("_", " ");
        return "We've reached " + count + " " + event + " together! " +
               "Every step of this journey has been meaningful.";
    }

    private String getOrdinalSuffix(int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    private String getAnniversaryMessage(int days) {
        if (days <= 7) {
            return "A whole week of adventures!";
        } else if (days <= 14) {
            return "Two weeks already!";
        } else if (days <= 30) {
            return "A whole month together!";
        } else if (days <= 90) {
            return "We've really grown together.";
        } else if (days <= 365) {
            return "A year of partnership!";
        } else {
            return "What an incredible journey we've shared!";
        }
    }

    // === NBT Persistence ===

    /**
     * Saves milestone data to NBT.
     *
     * @param tag The CompoundTag to save to
     */
    public void saveToNBT(CompoundTag tag) {
        // Save achieved milestones
        ListTag milestonesList = new ListTag();
        for (Milestone milestone : achievedMilestones.values()) {
            CompoundTag milestoneTag = new CompoundTag();
            milestoneTag.putString("Id", milestone.id);
            milestoneTag.putString("Type", milestone.type.name());
            milestoneTag.putString("Title", milestone.title);
            milestoneTag.putString("Description", milestone.description);
            milestoneTag.putInt("Importance", milestone.importance);
            milestoneTag.putLong("AchievedAt", milestone.achievedAt.toEpochMilli());
            milestonesList.add(milestoneTag);
        }
        tag.put("Milestones", milestonesList);

        // Save first occurrences
        CompoundTag firstOccurrencesTag = new CompoundTag();
        for (Map.Entry<String, Instant> entry : firstOccurrences.entrySet()) {
            firstOccurrencesTag.putLong(entry.getKey(), entry.getValue().toEpochMilli());
        }
        tag.put("FirstOccurrences", firstOccurrencesTag);

        // Save counters
        CompoundTag countersTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : counters.entrySet()) {
            countersTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Counters", countersTag);

        // Save last anniversary check
        tag.putLong("LastAnniversaryCheck", lastAnniversaryCheck.toEpochMilli());

        LOGGER.debug("MilestoneTracker saved to NBT ({} milestones)", achievedMilestones.size());
    }

    /**
     * Loads milestone data from NBT.
     *
     * @param tag The CompoundTag to load from
     */
    public void loadFromNBT(CompoundTag tag) {
        // Load achieved milestones
        ListTag milestonesList = tag.getList("Milestones", 10);
        if (!milestonesList.isEmpty()) {
            achievedMilestones.clear();
            for (int i = 0; i < milestonesList.size(); i++) {
                CompoundTag milestoneTag = milestonesList.getCompound(i);
                Milestone milestone = new Milestone(
                    milestoneTag.getString("Id"),
                    MilestoneType.valueOf(milestoneTag.getString("Type")),
                    milestoneTag.getString("Title"),
                    milestoneTag.getString("Description"),
                    milestoneTag.getInt("Importance"),
                    Instant.ofEpochMilli(milestoneTag.getLong("AchievedAt"))
                );
                achievedMilestones.put(milestone.id, milestone);
            }
        }

        // Load first occurrences
        CompoundTag firstOccurrencesTag = tag.getCompound("FirstOccurrences");
        if (!firstOccurrencesTag.isEmpty()) {
            firstOccurrences.clear();
            for (String key : firstOccurrencesTag.getAllKeys()) {
                firstOccurrences.put(key, Instant.ofEpochMilli(firstOccurrencesTag.getLong(key)));
            }
        }

        // Load counters
        CompoundTag countersTag = tag.getCompound("Counters");
        if (!countersTag.isEmpty()) {
            counters.clear();
            for (String key : countersTag.getAllKeys()) {
                counters.put(key, countersTag.getInt(key));
            }
        }

        // Load last anniversary check
        long lastCheckEpoch = tag.getLong("LastAnniversaryCheck");
        if (lastCheckEpoch != 0) {
            lastAnniversaryCheck = Instant.ofEpochMilli(lastCheckEpoch);
        }

        LOGGER.info("MilestoneTracker loaded from NBT ({} milestones)", achievedMilestones.size());
    }

    // === Inner Classes ===

    /**
     * Represents a relationship milestone.
     */
    public static class Milestone {
        public final String id;
        public final MilestoneType type;
        public final String title;
        public final String description;
        public final int importance;
        public final Instant achievedAt;

        public Milestone(String id, MilestoneType type, String title, String description,
                        int importance, Instant achievedAt) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.description = description;
            this.importance = importance;
            this.achievedAt = achievedAt;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s", type, title, description);
        }
    }

    /**
     * Types of milestones.
     */
    public enum MilestoneType {
        /**
         * First-time experiences.
         */
        FIRST,

        /**
         * Time-based anniversaries.
         */
        ANNIVERSARY,

        /**
         * Count-based milestones.
         */
        COUNT,

        /**
         * Special achievements.
         */
        ACHIEVEMENT
    }
}
