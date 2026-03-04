package com.minewright.memory.milestone;

import com.minewright.memory.MilestoneTracker;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage and query interface for milestones.
 *
 * <p>This class handles the storage of achieved milestones, pending milestones,
 * first occurrences, and counters.</p>
 */
public class MilestoneStore {

    /**
     * All achieved milestones, keyed by unique ID.
     */
    private final Map<String, MilestoneTracker.Milestone> achievedMilestones;

    /**
     * Pending milestones to be announced.
     */
    private final Queue<MilestoneTracker.Milestone> pendingMilestones;

    /**
     * First occurrences of events - for detecting "first" milestones.
     */
    private final Map<String, Instant> firstOccurrences;

    /**
     * Counter-based milestones - for tracking counts.
     */
    private final Map<String, Integer> counters;

    public MilestoneStore() {
        this.achievedMilestones = new ConcurrentHashMap<>();
        this.pendingMilestones = new LinkedList<>();
        this.firstOccurrences = new ConcurrentHashMap<>();
        this.counters = new ConcurrentHashMap<>();
    }

    /**
     * Checks if a specific milestone has been achieved.
     */
    public boolean hasMilestone(String milestoneId) {
        return achievedMilestones.containsKey(milestoneId);
    }

    /**
     * Records a milestone in storage.
     */
    public void recordMilestone(MilestoneTracker.Milestone milestone) {
        achievedMilestones.put(milestone.id, milestone);
        pendingMilestones.add(milestone);
    }

    /**
     * Records a milestone without adding to pending list.
     * Used for auto-detected milestones to avoid spam.
     */
    public void recordMilestoneSilent(MilestoneTracker.Milestone milestone) {
        achievedMilestones.put(milestone.id, milestone);
    }

    /**
     * Checks if a first occurrence has been recorded.
     */
    public boolean hasFirstOccurrence(String key) {
        return firstOccurrences.containsKey(key);
    }

    /**
     * Records a first occurrence.
     */
    public void recordFirstOccurrence(String key, Instant timestamp) {
        firstOccurrences.put(key, timestamp);
    }

    /**
     * Gets the timestamp of a first occurrence.
     */
    public Optional<Instant> getFirstOccurrence(String key) {
        return Optional.ofNullable(firstOccurrences.get(key));
    }

    /**
     * Increments a counter and returns the new value.
     */
    public int incrementCounter(String key) {
        return counters.merge(key, 1, Integer::sum);
    }

    /**
     * Gets the current value of a counter.
     */
    public int getCounter(String key) {
        return counters.getOrDefault(key, 0);
    }

    /**
     * Gets all achieved milestones.
     */
    public List<MilestoneTracker.Milestone> getMilestones() {
        return new ArrayList<>(achievedMilestones.values());
    }

    /**
     * Gets pending milestones that haven't been announced yet.
     */
    public List<MilestoneTracker.Milestone> getPendingMilestones() {
        List<MilestoneTracker.Milestone> pending = new ArrayList<>();
        MilestoneTracker.Milestone milestone;
        while ((milestone = pendingMilestones.poll()) != null) {
            pending.add(milestone);
        }
        return pending;
    }

    /**
     * Peeks at the next pending milestone without removing it.
     */
    public Optional<MilestoneTracker.Milestone> peekPendingMilestone() {
        return Optional.ofNullable(pendingMilestones.peek());
    }

    /**
     * Clears all pending milestones.
     */
    public void clearPendingMilestones() {
        pendingMilestones.clear();
    }

    /**
     * Gets the number of achieved milestones.
     */
    public int getMilestoneCount() {
        return achievedMilestones.size();
    }

    // Package-private access for persistence
    Map<String, MilestoneTracker.Milestone> getAchievedMilestonesMap() {
        return achievedMilestones;
    }

    Map<String, Instant> getFirstOccurrencesMap() {
        return firstOccurrences;
    }

    Map<String, Integer> getCountersMap() {
        return counters;
    }
}
