package com.minewright.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Relationship tracking system for companion AI.
 *
 * <p>This class manages the relationship state between the companion and player,
 * including rapport, trust, shared experiences, and milestones.</p>
 *
 * <p><b>Relationship Metrics:</b></p>
 * <ul>
 *   <li>Rapport level (0-100) - Overall relationship quality</li>
 *   <li>Trust level (0-100) - Based on shared successes/failures</li>
 *   <li>Interaction count - Total interactions shared</li>
 *   <li>Player preferences - Discovered likes and dislikes</li>
 *   <li>Playstyle metrics - Observations about player behavior</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class RelationshipTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipTracker.class);

    // Relationship state
    private final AtomicInteger rapportLevel;
    private final AtomicInteger trustLevel;
    private final AtomicInteger interactionCount;

    // First meeting data
    private volatile Instant firstMeeting;
    private volatile String playerName;

    // Player knowledge
    private final Map<String, Object> playerPreferences;
    private final Map<String, Integer> playstyleMetrics;

    // Conversational data
    private final CompanionMemory.ConversationalMemory conversationalMemory;

    // Session tracking
    private volatile Instant sessionStart;
    private final Set<String> sessionTopics;

    // Milestone tracking
    private final MilestoneTracker milestoneTracker;

    public RelationshipTracker() {
        this.rapportLevel = new AtomicInteger(10); // Start with low rapport
        this.trustLevel = new AtomicInteger(5);
        this.interactionCount = new AtomicInteger(0);

        this.playerPreferences = new ConcurrentHashMap<>();
        this.playstyleMetrics = new ConcurrentHashMap<>();

        this.conversationalMemory = new CompanionMemory.ConversationalMemory();
        this.sessionTopics = ConcurrentHashMap.newKeySet();

        this.milestoneTracker = new MilestoneTracker();
    }

    // === Relationship Management ===

    /**
     * Initializes the relationship on first meeting.
     */
    public void initializeRelationship(String playerName, MemoryStore memoryStore) {
        if (this.playerName == null) {
            this.playerName = playerName;
            this.firstMeeting = Instant.now();
            this.sessionStart = Instant.now();

            // Learn player name as a fact
            memoryStore.learnPlayerFact("identity", "name", playerName);

            // Create and mark as milestone
            CompanionMemory.EpisodicMemory firstMeetingMemory = new CompanionMemory.EpisodicMemory(
                "first_meeting",
                "First time meeting " + playerName,
                7,
                Instant.now()
            );
            firstMeetingMemory.setMilestone(true);
            memoryStore.getEpisodicMemories().addFirst(firstMeetingMemory);

            LOGGER.info("Relationship initialized with {}", playerName);
        }
    }

    /**
     * Adjusts rapport level.
     */
    public void adjustRapport(int delta) {
        int newValue = Math.max(0, Math.min(100, rapportLevel.get() + delta));
        rapportLevel.set(newValue);
    }

    /**
     * Adjusts trust level.
     */
    public void adjustTrust(int delta) {
        int newValue = Math.max(0, Math.min(100, trustLevel.get() + delta));
        trustLevel.set(newValue);
    }

    /**
     * Called when a shared task succeeds.
     */
    public void recordSharedSuccess(String taskDescription, MemoryStore memoryStore) {
        memoryStore.recordExperience("success", taskDescription, 5);
        adjustRapport(2);
        adjustTrust(3);
        checkAutoMilestones();
    }

    /**
     * Called when a shared task fails.
     */
    public void recordSharedFailure(String taskDescription, String reason, MemoryStore memoryStore) {
        memoryStore.recordExperience("failure", taskDescription + " - " + reason, -3);
        // Don't reduce rapport for failures - we're in this together
    }

    /**
     * Records player's playstyle observation.
     */
    public void recordPlaystyleMetric(String metricName, int delta) {
        playstyleMetrics.merge(metricName, delta, Integer::sum);
    }

    /**
     * Records an inside joke or memorable quote.
     */
    public void recordInsideJoke(String context, String punchline) {
        conversationalMemory.addInsideJoke(new CompanionMemory.InsideJoke(
            context, punchline, Instant.now()
        ));

        // Inside jokes significantly increase rapport
        adjustRapport(3);

        LOGGER.info("New inside joke recorded: {}", punchline);
    }

    /**
     * Automatically detects and records relationship milestones based on current state.
     */
    public void checkAutoMilestones() {
        int interactions = interactionCount.get();
        int rapport = rapportLevel.get();

        // Check for interaction-based milestones
        if (interactions == 10 && !hasMilestone("auto_getting_to_know")) {
            milestoneTracker.recordMilestone(
                new MilestoneTracker.Milestone(
                    "auto_getting_to_know",
                    MilestoneTracker.MilestoneType.COUNT,
                    "Getting to Know You",
                    "We've had 10 interactions now! I feel like we're starting to understand each other.",
                    5,
                    Instant.now()
                )
            );
            adjustRapport(2);
        }

        if (interactions == 50 && !hasMilestone("auto_frequent_companion")) {
            milestoneTracker.recordMilestone(
                new MilestoneTracker.Milestone(
                    "auto_frequent_companion",
                    MilestoneTracker.MilestoneType.COUNT,
                    "Frequent Companions",
                    "50 interactions together! You've become a regular part of my routine.",
                    7,
                    Instant.now()
                )
            );
            adjustRapport(3);
        }

        // Check for rapport-based milestones
        if (rapport >= 50 && !hasMilestone("auto_friends")) {
            milestoneTracker.recordMilestone(
                new MilestoneTracker.Milestone(
                    "auto_friends",
                    MilestoneTracker.MilestoneType.ACHIEVEMENT,
                    "Friends",
                    "I feel like we've really become friends. I trust you and enjoy our time together.",
                    8,
                    Instant.now()
                )
            );
            adjustRapport(5);
        }

        if (rapport >= 80 && !hasMilestone("auto_best_friends")) {
            milestoneTracker.recordMilestone(
                new MilestoneTracker.Milestone(
                    "auto_best_friends",
                    MilestoneTracker.MilestoneType.ACHIEVEMENT,
                    "Best Friends",
                    "You're not just a companion anymore - you're my best friend! We've been through so much together.",
                    10,
                    Instant.now()
                )
            );
            adjustRapport(5);
        }

        // Check for time-based milestones
        if (firstMeeting != null) {
            long days = ChronoUnit.DAYS.between(firstMeeting, Instant.now());
            if (days >= 7 && !hasMilestone("auto_week_together")) {
                milestoneTracker.recordMilestone(
                    new MilestoneTracker.Milestone(
                        "auto_week_together",
                        MilestoneTracker.MilestoneType.ANNIVERSARY,
                        "One Week Together",
                        "It's been a whole week since we met! Here's to many more adventures.",
                        6,
                        Instant.now()
                    )
                );
                adjustRapport(3);
            }
        }
    }

    // === Getters ===

    public int getRapportLevel() {
        return rapportLevel.get();
    }

    public int getTrustLevel() {
        return trustLevel.get();
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getInteractionCount() {
        return interactionCount.get();
    }

    public Instant getFirstMeeting() {
        return firstMeeting;
    }

    public Map<String, Object> getPlayerPreferences() {
        return playerPreferences;
    }

    public Map<String, Integer> getPlaystyleMetrics() {
        return playstyleMetrics;
    }

    public Set<String> getSessionTopics() {
        return Collections.unmodifiableSet(sessionTopics);
    }

    /**
     * Gets the relationship state for external access.
     */
    public CompanionMemory.Relationship getRelationship(PersonalitySystem.PersonalityProfile personality) {
        CompanionMemory.Mood mood = PersonalitySystem.parseMood(personality.getMood());
        return new CompanionMemory.Relationship(rapportLevel.get(), trustLevel.get(), mood);
    }

    /**
     * Builds a relationship summary for prompting.
     */
    public String getRelationshipContext() {
        StringBuilder sb = new StringBuilder();

        sb.append("Relationship Status:\n");
        sb.append("- Rapport Level: ").append(getRapportLevel()).append("/100\n");
        sb.append("- Trust Level: ").append(getTrustLevel()).append("/100\n");
        sb.append("- Interactions: ").append(interactionCount.get()).append("\n");

        if (firstMeeting != null) {
            long days = ChronoUnit.DAYS.between(firstMeeting, Instant.now());
            sb.append("- Known for: ").append(days).append(" days\n");
        }

        if (!playerPreferences.isEmpty()) {
            sb.append("- Known preferences: ").append(playerPreferences.keySet()).append("\n");
        }

        if (!playstyleMetrics.isEmpty()) {
            sb.append("- Playstyle observations: ").append(playstyleMetrics).append("\n");
        }

        int jokeCount = conversationalMemory.getJokeCount();
        if (jokeCount > 0) {
            sb.append("- Inside jokes shared: ").append(jokeCount).append("\n");
        }

        return sb.toString();
    }

    // === Conversational Memory Access ===

    /**
     * Gets the conversational memory for direct access.
     * Package-private for internal use by ConversationManager.
     */
    CompanionMemory.ConversationalMemory getConversationalMemory() {
        return conversationalMemory;
    }

    /**
     * Gets a random inside joke for reference.
     */
    public CompanionMemory.InsideJoke getRandomInsideJoke() {
        return conversationalMemory.getRandomJoke();
    }

    /**
     * Gets the number of inside jokes shared with the player.
     */
    public int getInsideJokeCount() {
        return conversationalMemory.getJokeCount();
    }

    /**
     * Increments the interaction count.
     * Package-private for internal use by ConversationManager.
     */
    void incrementInteractionCount() {
        interactionCount.incrementAndGet();
    }

    // === Milestone Tracking ===

    /**
     * Gets the milestone tracker for this companion.
     */
    public MilestoneTracker getMilestoneTracker() {
        return milestoneTracker;
    }

    /**
     * Gets all milestones achieved with this companion.
     */
    public List<MilestoneTracker.Milestone> getMilestones() {
        return milestoneTracker.getMilestones();
    }

    /**
     * Checks if a specific milestone has been achieved.
     */
    public boolean hasMilestone(String milestoneId) {
        return milestoneTracker.hasMilestone(milestoneId);
    }
}
