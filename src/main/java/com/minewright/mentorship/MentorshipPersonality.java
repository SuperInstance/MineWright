package com.minewright.mentorship;

/**
 * Foreman's personality for teaching interactions.
 *
 * <p>This class models the mentor's personality traits that affect
 * how they interact with workers, including vulnerability and
 * willingness to learn from workers.</p>
 *
 * @since 1.5.0
 */
public class MentorshipPersonality {
    private boolean admitsUncertainty;
    private double willingnessToLearnFromWorkers;

    public MentorshipPersonality() {
        this.admitsUncertainty = true;
        this.willingnessToLearnFromWorkers = 0.7;
    }

    public boolean shouldAdmitUncertainty(String context) {
        return admitsUncertainty && (
            context.toLowerCase().contains("new") ||
            context.toLowerCase().contains("experimental") ||
            context.toLowerCase().contains("first time") ||
            context.toLowerCase().contains("never")
        );
    }

    public boolean shouldAskWorker(String context) {
        return willingnessToLearnFromWorkers > 0.5 &&
            Math.random() < willingnessToLearnFromWorkers;
    }

    public boolean isAdmitsUncertainty() {
        return admitsUncertainty;
    }

    public void setAdmitsUncertainty(boolean admitsUncertainty) {
        this.admitsUncertainty = admitsUncertainty;
    }

    public double getWillingnessToLearnFromWorkers() {
        return willingnessToLearnFromWorkers;
    }

    public void setWillingnessToLearnFromWorkers(double willingnessToLearnFromWorkers) {
        this.willingnessToLearnFromWorkers = willingnessToLearnFromWorkers;
    }
}
