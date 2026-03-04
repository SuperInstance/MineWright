package com.minewright.mentorship;

import com.minewright.mentorship.MentorshipModels.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Generates dialogue for mentorship interactions.
 *
 * <p>This class handles all dialogue generation for teaching moments,
 * including hints, praise, corrections, celebrations, and Socratic questioning.</p>
 *
 * @since 1.5.0
 */
public class MentorshipDialogueGenerator implements TeachingMomentDetector.DialogueGenerator,
        TeachingMomentDetector.HintGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MentorshipDialogueGenerator.class);

    /**
     * Generates a hint without giving the solution.
     *
     * <p>Uses a three-tier hint system:</p>
     * <ol>
     *   <li>Conceptual hints (think about principles)</li>
     *   <li>Process hints (step-by-step thinking)</li>
     *   <li>Specific hints (direct guidance)</li>
     * </ol>
     *
     * @param worker The worker to generate hint for
     * @param context The task context
     * @param hintLevel Which level of hint (1-3)
     * @return The hint dialogue
     */
    @Override
    public String generateHint(WorkerProfile worker, String context, int hintLevel) {
        return switch (hintLevel) {
            case 1 -> getConceptualHint(context);
            case 2 -> getProcessHint(context);
            case 3 -> getSpecificHint(context);
            default -> "What do you think the first step is?";
        };
    }

    /**
     * Generates conceptual hints about principles.
     */
    private String getConceptualHint(String context) {
        String lower = context.toLowerCase();

        if (lower.contains("wall") || lower.contains("build") || lower.contains("structure")) {
            return "Think about what makes structures stable and level.";
        }
        if (lower.contains("redstone") || lower.contains("circuit") || lower.contains("wire")) {
            return "Consider how the signal flows and what powers it.";
        }
        if (lower.contains("water") || lower.contains("flow") || lower.contains("liquid")) {
            return "Think about how water moves in Minecraft and what blocks it can pass through.";
        }
        if (lower.contains("farm") || lower.contains("crop") || lower.contains("grow")) {
            return "What do crops need to grow successfully?";
        }
        if (lower.contains("mine") || lower.contains("ore") || lower.contains("dig")) {
            return "Think about the most efficient way to gather resources.";
        }

        return "What's the core principle we need to consider here?";
    }

    /**
     * Generates process hints about approach.
     */
    private String getProcessHint(String context) {
        String lower = context.toLowerCase();

        if (lower.contains("wall") || lower.contains("build")) {
            return "Start with a solid foundation, then build up in layers.";
        }
        if (lower.contains("redstone") || lower.contains("circuit")) {
            return "Trace the path from power source to output. Where's the signal stopping?";
        }
        if (lower.contains("water") || lower.contains("flow")) {
            return "Water flows and spreads. Check if there's a path for it to follow.";
        }
        if (lower.contains("farm") || lower.contains("crop")) {
            return "Consider the layout for water reach and light levels.";
        }
        if (lower.contains("mine") || lower.contains("ore")) {
            return "Think about branch mining patterns and cave exploration efficiency.";
        }

        return "Break it down into smaller, manageable steps.";
    }

    /**
     * Generates specific hints with direct guidance.
     */
    private String getSpecificHint(String context) {
        String lower = context.toLowerCase();

        if (lower.contains("wall") || lower.contains("build")) {
            return "Try starting with the corners to frame the structure.";
        }
        if (lower.contains("redstone") || lower.contains("circuit")) {
            return "A repeater might help boost the signal. What direction should it face?";
        }
        if (lower.contains("water") || lower.contains("flow")) {
            return "Water hydrates soil up to 4 blocks away. Check your spacing.";
        }
        if (lower.contains("farm") || lower.contains("crop")) {
            return "Crops need light level 9 or higher. Is your farm well-lit?";
        }
        if (lower.contains("mine")) {
            return "Try mining at Y=-58 for the best diamond distribution.";
        }

        return "Focus on one part at a time, then move to the next.";
    }

    /**
     * Generates Socratic questioning sequence.
     *
     * <p>Based on the Socratic method, these questions guide workers
     * to discover answers themselves rather than being told directly.</p>
     */
    @Override
    public List<String> generateSocraticQuestions(String context) {
        List<String> questions = new ArrayList<>();

        questions.add("What's your goal here?");
        questions.add("What have you tried so far?");

        String lower = context.toLowerCase();

        if (lower.contains("build") || lower.contains("create")) {
            questions.add("What would happen if we tried a different arrangement?");
            questions.add("Is there a way to make this more efficient?");
        } else if (lower.contains("redstone") || lower.contains("mechanism")) {
            questions.add("Where is the signal flowing?");
            questions.add("What's powering this component?");
            questions.add("What happens if the signal path is blocked?");
        } else if (lower.contains("farm") || lower.contains("crop")) {
            questions.add("What do these crops need to thrive?");
            questions.add("How does the water reach them?");
        } else {
            questions.add("What do you think would happen if...?");
            questions.add("Is there another way to approach this?");
        }

        questions.add("What have you learned from trying this?");
        return questions;
    }

    /**
     * Generates specific, genuine praise for worker achievement.
     *
     * <p>Uses the CSS Framework: Clear observation, Specific impact, Supportive tone.
     * Avoids generic praise like "good job" in favor of specific feedback.</p>
     *
     * @param workerName The worker being praised
     * @param completion Details about what was completed
     * @return Specific praise dialogue
     */
    public String generatePraise(String workerName, TaskCompletion completion) {
        String observation = completion.getSpecificAction();
        String impact = completion.getOutcome();
        String trait = completion.getDemonstratedTrait();

        // Use different templates based on trait
        return switch (trait.toLowerCase()) {
            case "speed", "efficiency", "fast", "quick" -> String.format(
                "You finished that quickly, which gave us time for %s. Shows your efficiency.",
                impact
            );
            case "quality", "precision", "accuracy", "detail", "careful" -> String.format(
                "The %s is perfect. That attention to detail really stands out.",
                observation
            );
            case "creativity", "creative", "innovative", "clever", "smart" -> String.format(
                "I never would have thought to %s. That's a creative approach to %s.",
                observation, impact
            );
            case "perseverance", "determination", "persistent", "stuck with it" -> String.format(
                "You stuck with %s even when it was tricky. That determination is valuable.",
                observation
            );
            case "improvement", "improved", "growth", "progress" -> String.format(
                "This is much better than your previous attempts. You've really improved at %s.",
                trait
            );
            case "teamwork", "helping", "cooperative", "together" -> String.format(
                "Your %s helped with %s. That's what makes a good team.",
                observation, impact
            );
            default -> String.format(
                "I noticed %s. That really helped with %s. Shows your %s.",
                observation, impact, trait
            );
        };
    }

    /**
     * Generates non-condescending correction using CSS framework.
     *
     * <p>Principles:</p>
     * <ul>
     *   <li>Use "I notice" instead of "You did"</li>
     *   <li>Use "Let's" instead of "You should"</li>
     *   <li>Focus on work, not worker</li>
     *   <li>Share responsibility when appropriate</li>
     *   <li>End with confidence</li>
     * </ul>
     */
    @Override
    public String generateCorrection(WorkerProfile worker, String context) {
        TaskError error = ErrorAnalyzer.analyzeError(worker, context);

        // Use CSS: Clear observation, Specific impact, Supportive solution
        return String.format(
            "I noticed %s. %s Let's %s.",
            getSpecificObservation(error),
            getImpactExplanation(error),
            getCollaborativeSolution(error)
        );
    }

    /**
     * Generates gentle correction for first-time mistakes.
     */
    @Override
    public String generateGentleCorrection(WorkerProfile worker, String context) {
        TaskError error = ErrorAnalyzer.analyzeError(worker, context);

        return String.format(
            "Hmm, let me show you something. %s Would you like to try a different approach?",
            getSpecificObservation(error)
        );
    }

    /**
     * Generates specific observation of the issue.
     */
    private String getSpecificObservation(TaskError error) {
        return switch (error.getType()) {
            case WRONG_MATERIAL -> String.format("you used %s here", error.getDetail());
            case MISSING_STEP -> String.format("this section is missing %s", error.getDetail());
            case SAFETY_ISSUE -> String.format("there's a %s concern", error.getDetail());
            case EFFICIENCY_ISSUE -> String.format("you're doing %s", error.getDetail());
            case STRUCTURAL_ISSUE -> String.format("the %s might not hold", error.getDetail());
        };
    }

    /**
     * Explains the impact of the issue.
     */
    private String getImpactExplanation(TaskError error) {
        return switch (error.getType()) {
            case WRONG_MATERIAL -> String.format("It might not work as well as %s would.",
                error.getSuggestedAlternative());
            case MISSING_STEP -> String.format("That could cause issues with %s.", error.getDetail());
            case SAFETY_ISSUE -> "That could be unsafe or cause problems.";
            case EFFICIENCY_ISSUE -> "That takes more time and effort than necessary.";
            case STRUCTURAL_ISSUE -> "That might lead to instability or collapse.";
        };
    }

    /**
     * Provides collaborative solution.
     */
    private String getCollaborativeSolution(TaskError error) {
        return switch (error.getType()) {
            case WRONG_MATERIAL -> String.format("try using %s instead", error.getSuggestedAlternative());
            case MISSING_STEP -> String.format("add %s here", error.getDetail());
            case SAFETY_ISSUE -> "move it to a safer location";
            case EFFICIENCY_ISSUE -> "try this more efficient approach";
            case STRUCTURAL_ISSUE -> "reinforce it with additional support";
        };
    }

    /**
     * Generates celebration for worker progress.
     *
     * <p>Celebrates growth by comparing to past performance, not absolute standards.
     * Focuses on the worker's journey and improvement.</p>
     */
    @Override
    public String generateCelebration(WorkerProfile worker, String context, Map<String, Integer> consecutiveSuccesses) {
        SkillMilestone milestone = MilestoneDetector.detectMilestone(worker, context);

        // Track success
        worker.recordSuccess(context);

        return switch (milestone) {
            case FIRST_SUCCESS -> String.format(
                "First time completing %s! Well done on getting through it!",
                context
            );
            case CONSISTENT_PERFORMANCE -> {
                int count = consecutiveSuccesses.merge(worker.getName(), 1, Integer::sum);
                yield String.format(
                    "That's the %d time in a row you've nailed %s. You've really got this down!",
                    count, context
                );
            }
            case SPEED_IMPROVEMENT -> String.format(
                "That was much faster than before. Your improvement with %s is noticeable!",
                context
            );
            case QUALITY_LEAP -> String.format(
                "This is noticeably better quality than your earlier attempts. Great progress on %s!",
                context
            );
            case INDEPENDENCE -> {
                worker.recordIndependentCompletion(context);
                yield "You completed that entirely on your own! That's real progress.";
            }
            case INNOVATION -> String.format(
                "I've never seen that approach to %s before. Creative thinking!",
                context
            );
            case TEACHING_OTHER -> String.format(
                "You're helping others learn %s now. You've come full circle!",
                context
            );
        };
    }

    /**
     * Generates suggestion for improvement.
     */
    @Override
    public String generateSuggestion(WorkerProfile worker, String context) {
        return String.format(
            "You completed it, but there might be a more efficient way. Next time, %s",
            getProcessHint(context)
        );
    }

    /**
     * Generates insight about worker's progress.
     */
    @Override
    public String generateInsight(WorkerProfile worker, String context) {
        return String.format(
            "you've been improving consistently with %s. Your technique is getting much smoother.",
            context
        );
    }

    /**
     * Generates dialogue where foreman admits uncertainty or asks for help.
     *
     * <p>This makes the foreman more relatable and models that learning is ongoing.
     * Based on research showing mentors who acknowledge their own limitations
     * are more effective and trusted.</p>
     */
    @Override
    public String generateForemanVulnerability(String context) {
        MentorshipPersonality personality = new MentorshipPersonality();

        if (personality.shouldAdmitUncertainty(context)) {
            return String.format(
                "You know, I haven't actually done %s before. What's your approach?",
                context
            );
        }

        if (personality.shouldAskWorker(context)) {
            return String.format(
                "You've been working with %s more than I have lately. What would you suggest?",
                context
            );
        }

        return null;
    }
}
