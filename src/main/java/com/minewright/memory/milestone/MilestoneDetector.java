package com.minewright.memory.milestone;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.CompanionMemory;
import com.minewright.memory.MilestoneTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Detects and creates milestones based on game events.
 *
 * <p>This class contains all the logic for checking if an event
 * triggers a milestone.</p>
 */
public class MilestoneDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MilestoneDetector.class);

    private final MilestoneStore store;
    private Instant lastAnniversaryCheck;

    public MilestoneDetector(MilestoneStore store) {
        this.store = store;
        this.lastAnniversaryCheck = Instant.now();
    }

    /**
     * Checks if an event triggers any milestones.
     */
    public Optional<MilestoneTracker.Milestone> checkMilestone(
            ForemanEntity minewright,
            String eventType,
            Object context) {
        CompanionMemory memory = minewright.getCompanionMemory();
        String playerName = memory.getPlayerName();

        if (playerName == null) {
            return Optional.empty();
        }

        // Check for "first" milestones
        Optional<MilestoneTracker.Milestone> firstMilestone = checkFirstMilestone(minewright, eventType, context);
        if (firstMilestone.isPresent()) {
            return firstMilestone;
        }

        // Update counters and check count-based milestones
        Optional<MilestoneTracker.Milestone> countMilestone = checkCountMilestone(minewright, eventType, context);
        if (countMilestone.isPresent()) {
            return countMilestone;
        }

        // Check for achievement milestones
        Optional<MilestoneTracker.Milestone> achievementMilestone = checkAchievementMilestone(minewright, eventType, context);
        if (achievementMilestone.isPresent()) {
            return achievementMilestone;
        }

        return Optional.empty();
    }

    /**
     * Checks for time-based anniversaries.
     */
    public Optional<MilestoneTracker.Milestone> checkAnniversaries(ForemanEntity minewright) {
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
            if (!store.hasMilestone(milestoneId) && daysSince >= days) {
                MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
                    milestoneId,
                    MilestoneTracker.MilestoneType.ANNIVERSARY,
                    days + " Days Together!",
                    String.format("We've been together for %d days! %s",
                        days, getAnniversaryMessage(days)),
                    days,
                    now
                );
                return Optional.of(milestone);
            }
        }

        return Optional.empty();
    }

    /**
     * Checks for "first" milestones.
     */
    private Optional<MilestoneTracker.Milestone> checkFirstMilestone(
            ForemanEntity minewright,
            String eventType,
            Object context) {
        String firstKey = "first_" + eventType;
        CompanionMemory memory = minewright.getCompanionMemory();
        String playerName = memory.getPlayerName();

        if (!store.hasFirstOccurrence(firstKey)) {
            store.recordFirstOccurrence(firstKey, Instant.now());

            String milestoneId = "milestone_first_" + eventType;
            String title = getFirstMilestoneTitle(eventType);
            String description = getFirstMilestoneDescription(eventType, playerName, context);

            MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
                milestoneId,
                MilestoneTracker.MilestoneType.FIRST,
                title,
                description,
                10, // High importance for "first" milestones
                Instant.now()
            );

            LOGGER.info("Milestone achieved: {} - {}", milestone.id, milestone.title);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    /**
     * Checks for count-based milestones.
     */
    private Optional<MilestoneTracker.Milestone> checkCountMilestone(
            ForemanEntity minewright,
            String eventType,
            Object context) {
        String counterKey = "count_" + eventType;
        int newCount = store.incrementCounter(counterKey);

        // Define count milestones
        int[] countMilestones = {10, 25, 50, 100, 250, 500, 1000};

        for (int threshold : countMilestones) {
            if (newCount == threshold) {
                String milestoneId = "milestone_count_" + eventType + "_" + threshold;
                String title = getCountMilestoneTitle(eventType, threshold);
                String description = getCountMilestoneDescription(eventType, threshold);

                MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
                    milestoneId,
                    MilestoneTracker.MilestoneType.COUNT,
                    title,
                    description,
                    Math.min(10, threshold / 50), // Importance scales with count
                    Instant.now()
                );

                LOGGER.info("Milestone achieved: {} - {}", milestone.id, milestone.title);
                return Optional.of(milestone);
            }
        }

        return Optional.empty();
    }

    /**
     * Checks for achievement milestones.
     */
    private Optional<MilestoneTracker.Milestone> checkAchievementMilestone(
            ForemanEntity minewright,
            String eventType,
            Object context) {
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

    // Individual milestone checks

    private Optional<MilestoneTracker.Milestone> checkDiamondMilestone(ForemanEntity minewright, Object context) {
        String milestoneId = "milestone_first_diamond";

        if (!store.hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();

            MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
                milestoneId,
                MilestoneTracker.MilestoneType.FIRST,
                "First Diamond!",
                playerName + ", we found our first diamond together! This is big! " +
                "Diamonds are rare and valuable - this marks our growth as a team.",
                10,
                Instant.now()
            );

            LOGGER.info("Milestone achieved: {} - {}", milestone.id, milestone.title);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    private Optional<MilestoneTracker.Milestone> checkNetherMilestone(ForemanEntity minewright) {
        String milestoneId = "milestone_first_nether";

        if (!store.hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();

            MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
                milestoneId,
                MilestoneTracker.MilestoneType.FIRST,
                "Nether Portal crossed!",
                "We just entered the Nether together, " + playerName + "! " +
                "A dangerous new dimension awaits. I'll stick close to you.",
                10,
                Instant.now()
            );

            LOGGER.info("Milestone achieved: {} - {}", milestone.id, milestone.title);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    private Optional<MilestoneTracker.Milestone> checkStructureMilestone(ForemanEntity minewright, Object context) {
        String structureType = context instanceof String ? (String) context : "structure";
        String milestoneId = "milestone_first_structure_" + structureType.toLowerCase();

        if (!store.hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();

            MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
                milestoneId,
                MilestoneTracker.MilestoneType.FIRST,
                "First " + structureType + " Built!",
                "We built our first " + structureType + " together, " + playerName + "! " +
                "Working side by side to create something lasting... that's what partnership is about.",
                9,
                Instant.now()
            );

            LOGGER.info("Milestone achieved: {} - {}", milestone.id, milestone.title);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    private Optional<MilestoneTracker.Milestone> checkCombatMilestone(ForemanEntity minewright, Object context) {
        String milestoneId = "milestone_first_combat";

        if (!store.hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();

            MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
                milestoneId,
                MilestoneTracker.MilestoneType.FIRST,
                "First Battle Together!",
                "We fought our first battle together, " + playerName + "! " +
                "Facing danger side by side... I've got your back, partner.",
                8,
                Instant.now()
            );

            LOGGER.info("Milestone achieved: {} - {}", milestone.id, milestone.title);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    private Optional<MilestoneTracker.Milestone> checkNightSurvivalMilestone(ForemanEntity minewright) {
        String milestoneId = "milestone_first_night";

        if (!store.hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();

            MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
                milestoneId,
                MilestoneTracker.MilestoneType.FIRST,
                "First Night Survived!",
                "We survived our first night together, " + playerName + "! " +
                "The darkness is dangerous, but we made it through safely.",
                7,
                Instant.now()
            );

            LOGGER.info("Milestone achieved: {} - {}", milestone.id, milestone.title);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    private Optional<MilestoneTracker.Milestone> checkGiftMilestone(ForemanEntity minewright, Object context) {
        String milestoneId = "milestone_first_gift";

        if (!store.hasMilestone(milestoneId)) {
            CompanionMemory memory = minewright.getCompanionMemory();
            String playerName = memory.getPlayerName();
            String item = context instanceof String ? (String) context : "gift";

            MilestoneTracker.Milestone milestone = new MilestoneTracker.Milestone(
                milestoneId,
                MilestoneTracker.MilestoneType.FIRST,
                "First Gift Exchanged!",
                playerName + ", this " + item + "... you gave this to me? " +
                "I'll treasure it. It means a lot that you thought of me.",
                10,
                Instant.now()
            );

            LOGGER.info("Milestone achieved: {} - {}", milestone.id, milestone.title);
            return Optional.of(milestone);
        }

        return Optional.empty();
    }

    // Message generation helpers

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
}
