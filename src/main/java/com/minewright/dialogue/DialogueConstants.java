package com.minewright.dialogue;

import java.util.Map;

/**
 * Constants and fallback comments for proactive dialogue.
 *
 * <p>This class contains static fallback comments used when LLM is unavailable,
 * organized by trigger type.</p>
 *
 * @since 1.3.0
 */
public class DialogueConstants {

    // Static fallback comments (used when LLM is unavailable)
    // Expanded with more variety and personality-driven options
    public static final Map<String, String[]> FALLBACK_COMMENTS = Map.of(
        "morning", new String[]{
            "Good morning! Ready to get to work?",
            "Morning! Let's make today productive.",
            "Early bird gets the blocks!",
            "Rise and shine! Another day to build!",
            "Nice morning weather for mining.",
            "Coffee? No? Just blocks then.",
            "The sun's up, time to build up!"
        },
        "night", new String[]{
            "Getting dark. Maybe we should wrap up soon?",
            "Night's falling. Hope you have a shelter ready!",
            "Watch out for the creepers tonight!",
            "Darkness incoming. Stay safe out there.",
            "Time to find cover. The mobs will be waking up.",
            "Night falls. Good time for indoor work.",
            "Keep your sword close tonight."
        },
        "raining", new String[]{
            "Rain's coming down. Perfect weather for mining underground!",
            "Nice day for a break, isn't it?",
            "The crops will love this rain!",
            "Wet weather. At least the fire won't spread.",
            "Rain again? Time to go below ground.",
            "Perfect mining weather. Nobody underground minds rain.",
            "Water from above, blocks below."
        },
        "storm", new String[]{
            "Thunder and lightning! Stay safe out there!",
            "Nasty weather. Let's take cover!",
            "Lightning! Definitely stay under cover now.",
            "The sky's angry. We should be indoors.",
            "Storm's brewing. Not the time to be climbing trees.",
            "Thunder! Hope nothing gets struck.",
            "When lightning strikes, we take shelter."
        },
        "idle_long", new String[]{
            "Everything quiet today?",
            "Let me know if you need anything done!",
            "Standing by, ready to help!",
            "Slow day? Could use a rest.",
            "Been a while. Got any projects in mind?",
            "Quiet... too quiet. Just kidding, or am I?",
            "Ready when you are. Always ready.",
            "Taking a break? I can do breaks."
        },
        "near_danger", new String[]{
            "Be careful around here!",
            "This area looks dangerous.",
            "Stay close, I've got your back!",
            "Something feels off about this place...",
            "Watch your step. Danger nearby.",
            "I don't like the look of this.",
            "Better stay alert. Trouble's close.",
            "Keep your eyes open. This isn't safe."
        },
        "task_complete", new String[]{
            "Another job well done!",
            "That went smoothly!",
            "Nice work!",
            "And that's how it's done!",
            "Task complete. What's next?",
            "Look at that. Perfection.",
            "One more thing crossed off the list.",
            "Smooth sailing on that one.",
            "Done and dusted!",
            "Results are in: we succeeded."
        },
        "task_failed", new String[]{
            "That didn't go as planned.",
            "Well, that's unfortunate.",
            "Let me try a different approach.",
            "Failed. But we learn from failures.",
            "Not ideal. Shall we try again?",
            "Hmm, that method didn't work.",
            "Back to the drawing board on that one.",
            "Every failure is a step to success."
        },
        "milestone", new String[]{
            "We did it! Together!",
            "Now THIS is worth celebrating!",
            "I'll remember this moment.",
            "What a journey this has been!",
            "We're really making progress!",
            "Moments like these make it all worth it.",
            "Here's to us and what we've built!"
        }
    );

    // Context-aware dialogue patterns based on relationship level
    public static final Map<String, String[]> RELATIONSHIP_DIALOGUES = Map.of(
        "low_rapport", new String[]{
            "Let me know if you need help.",
            "I'm here to assist.",
            "Just tell me what to build.",
            "Standing by for instructions."
        },
        "medium_rapport", new String[]{
            "Glad to help you out!",
            "We make a good team.",
            "Looking forward to our next project!",
            "Nice working with you."
        },
        "high_rapport", new String[]{
            "We're unstoppable together!",
            "I've got your back, friend!",
            "Nothing we can't handle as a team!",
            "Best partner I could ask for!"
        }
    );

    private DialogueConstants() {
        // Utility class - prevent instantiation
    }
}
