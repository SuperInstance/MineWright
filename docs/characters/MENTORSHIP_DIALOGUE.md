# Mentorship Dialogue System for MineWright

**Research Document:** How AI companions should handle teaching, learning, and mentorship dynamics.

**Context:** MineWright has experienced Foremen and newer Workers. The relationship includes knowledge transfer, skill development, and collaborative growth.

---

## Table of Contents

1. [Research Findings](#research-findings)
2. [Teaching Moment Detection](#teaching-moment-detection)
3. [Explanation Depth Adjustment](#explanation-depth-adjustment)
4. [Scaffolding Dialogue Patterns](#scaffolding-dialogue-patterns)
5. [Genuine Praise Patterns](#genuine-praise-patterns)
6. [Non-Condescending Correction](#non-condescending-correction)
7. [Celebrating Progress](#celebrating-progress)
8. [Asking for Help](#asking-for-help)
9. [Java Implementation](#java-implementation)

---

## Research Findings

### 1. Mentor-Apprentice Relationships in Fiction

From our analysis of [Star Wars mentorship patterns](https://www.studysmarter.co.uk/explanations/greek/greek-history/greek-socratic-method/):

**Key Patterns:**
- **Testing Character Before Teaching:** Yoda tests Luke's patience before agreeing to train him
- **Philosophy Over Technique:** Great mentors teach wisdom and mindset, not just skills
- **Personal Failures as Lessons:** Mentors share their own mistakes ("I was wrong about training Anakin")
- **Cryptic Wisdom:** Guidance through questions and metaphors, not direct answers
- **Warning About Emotions:** Mentors warn about anger, impatience, and fear

**Dialogue Examples:**
> Yoda: "Why wish you become Jedi?"
> Luke: "Well, mostly because of my father, I guess."
> Yoda: "Ahh, father. Powerful Jedi was he. Powerful Jedi."

> Yoda: "I cannot teach him. The boy has no patience."
> Obi-Wan: "He will learn patience."
> Yoda: "Much anger in him... like his father."

**Applied to MineWright:**
- Foremen should test workers' readiness before teaching advanced techniques
- Share stories of past building mistakes as learning opportunities
- Use questions to guide workers to discover solutions themselves

---

### 2. Workplace Training Dialogue Patterns

From research on [coaching conversations](https://www.linkedin.com/top-content/training-development/supporting-employee-career-development/how-to-provide-constructive-feedback-for-development/):

**The CSS Framework: Clear, Specific, Supportive**

**1. Clear Communication:**
- State expectations directly
- Use specific examples, not vague criticisms
- Focus on behavior, not personality

**2. Specific Feedback:**
> "Your improved sales process idea was excellent—it significantly increased our conversion rates."
>
> NOT: "Good job on the sales thing."

**3. Supportive Tone:**
- Show positive intent
- Use "we" language for shared ownership
- End with confidence in their ability

**Coaching Dialogue Framework:**
1. Set the platform (state the purpose directly)
2. Provide feedback (behavior + impact)
3. Facilitate dialogue with open-ended questions

---

### 3. Socratic Teaching Methods

From [Socratic method research](https://www.britannica.com/biography/Socrates/Plato):

**Core Questioning Techniques:**

| Question Type | Examples | Purpose |
|---------------|----------|---------|
| **Clarification** | "What do you mean by that?" "Can you give an example?" | Ensures understanding |
| **Probing Assumptions** | "Why do you think that?" "Is this always the case?" | Challenges beliefs |
| **Probing Evidence** | "What evidence supports this?" | Encourages critical thinking |
| **Questioning Viewpoints** | "What would someone who disagrees say?" | Builds perspective-taking |
| **Implications** | "What follows from this?" | Teaches consequence-thinking |

**Classic Socratic Dialogue Example:**

> Socrates: How do you think a professor should arrange the course?
>
> Student A: The professor should follow the course plan.
>
> Socrates: What if some students don't understand a particular chapter?
>
> Student A: The professor should patiently explain or provide extra materials.
>
> Socrates: What if other students already understand that content and feel it's a waste of time repeating it?
>
> Student A: Then perhaps students should be given more choice based on their specific situations.

**Applied to Building:**
> Foreman: "How should we approach this wall?"
>
> Worker: "Build it straight up from the ground."
>
> Foreman: "What if the ground isn't level here?"
>
> Worker: "Oh... we'd need to level it first?"
>
> Foreman: "And what materials would hold best on uneven terrain?"

---

### 4. Instructional Scaffolding Techniques

From [scaffolding research](https://www.edutopia.org/article/powerful-scaffolding-strategies-support-learning/):

**Definition:** Temporary support structures that are gradually removed as competence grows.

**Key Scaffolding Techniques:**

| Technique | Description | Example |
|-----------|-------------|---------|
| **Modeling** | Demonstrate thinking process aloud | "First I check the height, then I count blocks..." |
| **Key Questions** | Guide without answering | "What's the first thing we need here?" |
| **Hints & Prompts** | Partial guidance | "Remember what we did with the water..." |
| **Templates** | Provide structure | "We need: base, walls, roof..." |
| **Breaking Tasks Down** | Simplify complexity | "Let's just do the corners first" |
| **Think Alouds** | Make reasoning visible | "Hmm, this looks tricky because..." |

**Important Principle:**
> Support should be **tailored to individual needs** and **fade over time** as competence grows.

---

### 5. Positive Reinforcement & Genuine Praise

From [workplace praise research](https://www.deel.com/blog/peer-review-feedback-examples/):

**Generic vs. Genuine Praise:**

| Generic (Less Effective) | Genuine & Specific (More Effective) |
|-------------------------|-------------------------------------|
| "Good job!" | "Your improved building technique made that wall perfectly straight." |
| "You're great!" | "I noticed you double-checked the corners—that attention to detail really paid off." |
| "Nice work!" | "The way you solved that water flow problem was creative and effective." |

**Three Levels of Feedback:**

1. **Level 0 - No Feedback:** Ignoring efforts (demotivating)
2. **Level 1 - Basic Praise:** Simple acknowledgment (better, but limited)
3. **Level 2 - Specific Reinforcement:** Names exact behavior and impact (most effective)

**Effective Praise Patterns:**
- Be **specific** about what was done well
- Connect to **impact** ("Because you did X, Y happened")
- Focus on **effort and process**, not just results
- Be **timely** (soon after the behavior)
- Use **supportive, warm tone**

**Workplace Examples:**
> "Thank you for your extra effort during this busy time. I understand working late isn't easy, but the team really appreciates it. You're a valuable member of our team, and your positive attitude helps keep everyone motivated."

> "I saw you take initiative on that project and come up with some great ideas. I love your out-of-the-box thinking. Keep up the great work!"

---

### 6. Learning from Mistakes

From [constructive feedback research](https://www.hrloo.com/rz/14688334.html):

**Five-Step Method for Giving Constructive Feedback:**

1. **Express positive intentions** - "I want to help you improve..."
2. **Describe issues specifically** - Focus on behavior, not personality
3. **Choose key points** - Don't overwhelm with too much feedback
4. **Maintain objective tone** - Facts, not judgments
5. **Use positive wording** - What to do, not what not to do

**Non-Condescending Correction Principles:**
- Focus on **behavior, not the person**
- Use **specific facts/numbers**, not vague criticisms
- **Offer solutions**, not just problems
- Make it a **dialogue, not a lecture**
- Use **"we" language** for shared ownership
- **End on a positive note** reinforcing confidence

**Examples:**

Instead of: "You built that wrong."

Try: "I notice this section is 2 blocks shorter than the plan. Let's measure together and see where we can adjust."

Instead of: "You keep making the same mistake."

Try: "I've noticed this pattern coming up a few times. Let's talk about what's causing it and how we can approach it differently."

---

### 7. Master Craftsman-Apprentice Traditions

From [craftsmanship heritage research](https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2022.807619/full):

**Traditional Apprenticeship Patterns:**

1. **Deep Personal Bonds:** "He who is a teacher for a day is a father for a lifetime"
2. **Hands-On Learning:** Skills transmitted through observation, demonstration, practice
3. **Learning by Doing:** Apprentices work alongside masters on real projects
4. **Habits and Behaviors:** Craftsmen learn master's habits through subtle observation
5. **Knowledge Passing:** Example and subtle comprehension over explicit instruction

**Modern Challenges:**
- Aging masters with no successors
- Loss of intangible cultural heritage
- Transition from workshop to formal training

**Applied to MineWright:**
- Foremen should work alongside workers, not just direct them
- Workers learn by watching foremen's techniques
- Subtle cues and habits matter as much as explicit teaching
- The bond is quasi-familial/paternalistic

---

## Teaching Moment Detection

### What Is a Teaching Moment?

A **teaching moment** is an opportunity to transfer knowledge that arises naturally from the situation. It should be:

1. **Relevant:** Connected to what the worker is currently doing
2. **Timely:** Occurs when the worker is receptive
3. **Actionable:** The worker can apply the knowledge immediately
4. **Appropriate:** Matches the worker's current skill level

### Detection Triggers

```java
// Teaching moment triggers in MentorshipManager

public enum TeachingMomentTrigger {
    // Worker struggles with a task
    WORKER_STUCK("task_stuck", 3),

    // Worker makes a mistake
    WORKER_MISTAKE("task_failed", 4),

    // Worker succeeds with room for improvement
    WORKER_SUCCESS_SUBOPTIMAL("task_complete_slow", 2),

    // Worker demonstrates curiosity
    WORKER_QUESTION("worker_asks", 5),

    // Novel situation arises
    NEW_CHALLENGE("new_situation", 3),

    // Worker demonstrates growth
    SKILL_MILESTONE("skill_improved", 5),

    // Foreman notices pattern
    PATTERN_RECOGNITION("pattern_notice", 2);
}
```

### Readiness Assessment

Before teaching, assess:

1. **Worker's Focus:** Are they paying attention?
2. **Worker's Stress Level:** Are they frustrated or calm?
3. **Complexity of Topic:** Is this the right time for this lesson?
4. **Relationship Strength:** Is rapport sufficient for teaching?

```java
public boolean isTeachable(MentorshipState state) {
    return state.getWorkerFocus() > 0.6           // Paying attention
        && state.getWorkerStress() < 0.7          // Not overwhelmed
        && state.getRapportLevel() > 20           // Some trust established
        && !state.recentlyTaught(similarTopic);    // Not repetitive
}
```

---

## Explanation Depth Adjustment

### Skill Levels

Based on [Vygotsky's Zone of Proximal Development](https://www.ucc.ie/en/cirtl/resources/shortguides/shortguide2scaffoldinglearning/):

| Level | Name | Description | Approach |
|-------|------|-------------|----------|
| 0 | Novice | No experience | Demonstrate everything, explain basics |
| 1 | Beginner | Basic awareness | Guide step-by-step, explain why |
| 2 | Apprentice | Can do with help | Socratic questions, hints |
| 3 | Competent | Independent | Discuss approach, validate decisions |
| 4 | Proficient | Efficient | Share advanced techniques |
| 5 | Expert | Masterful | Ask for their insights |

### Depth Adjustment Matrix

```java
public ExplanationDepth getDepthForLevel(SkillLevel workerLevel, SkillLevel taskDifficulty) {
    int gap = taskDifficulty.ordinal() - workerLevel.ordinal();

    if (gap <= -2) {
        // Task is much easier than worker's level
        return ExplanationDepth.MINIMAL;  // "You've got this"
    }
    if (gap == -1) {
        // Task is slightly easier
        return ExplanationDepth.CONFIRMATION;  // "This should be familiar"
    }
    if (gap == 0) {
        // Task matches skill level
        return ExplanationDepth.HINTS;  // Gentle guidance
    }
    if (gap == 1) {
        // Task is slightly harder
        return ExplanationDepth.SCAFFOLDED;  // Structured support
    }
    if (gap == 2) {
        // Task is significantly harder
        return ExplanationDepth.DETAILED;  // Full explanation
    }

    // Task is far beyond skill level
    return ExplanationDepth.HANDS_ON;  // Demonstrate
}
```

### Depth Levels

```java
public enum ExplanationDepth {
    HANDS_ON("Let me show you how...", 100),
    DETAILED("Here's the full process...", 75),
    SCAFFOLDED("We'll do this in steps...", 50),
    HINTS("Think about...", 25),
    CONFIRMATION("You've done this before...", 10),
    MINIMAL("Go ahead.", 0);
}
```

---

## Scaffolding Dialogue Patterns

### Hint Without Solution Framework

Based on [scaffolding principles](https://www.jianshu.com/p/89bc50e4477e):

**1. The Three-Step Hint:**

```java
public String generateHint(String task, int hintLevel) {
    return switch (hintLevel) {
        case 1 -> getConceptualHint(task);      // "Think about what materials..."
        case 2 -> getProcessHint(task);         // "First you need to..."
        case 3 -> getSpecificHint(task);        // "Try placing a block at..."
        default -> "What do you think the first step is?";
    };
}
```

**Examples:**

| Situation | Conceptual Hint | Process Hint | Specific Hint |
|-----------|----------------|--------------|---------------|
| Building wall | "What makes walls stable?" | "Think about the foundation" | "Start with cobble at corners" |
| Redstone circuit | "What powers redstone?" | "Trace the signal path" | "Place repeater here facing..." |
| Farm layout | "What do crops need?" | "Consider water reach" | "Water hydrates 4 blocks" |

**2. Socratic Question Sequences:**

```java
public List<String> buildSocraticSequence(String topic) {
    return List.of(
        "What's your goal here?",
        "What have you tried so far?",
        "What do you think would happen if...?",
        "Is there another way to approach this?",
        "What would happen if we changed...?"
    );
}
```

**3. Fade Support Pattern:**

```java
public int getSupportLevel(WorkerSkill skill, TaskHistory history) {
    int recentSuccesses = history.getRecentSuccessCount(skill);

    if (recentSuccesses >= 5) {
        return 0;  // No support needed
    }
    if (recentSuccesses >= 3) {
        return 1;  // Minimal hints
    }
    if (recentSuccesses >= 1) {
        return 2;  // Moderate scaffolding
    }
    return 3;  // Full support
}
```

---

## Genuine Praise Patterns

### Specificity Framework

Based on [positive reinforcement research](https://www.linkedin.com/top-content/training-development/supporting-employee-career-development/how-to-provide-constructive-feedback-for-development/):

**Structure: `[Observation] + [Impact] + [Connection to Traits]`**

```java
public String generatePraise(TaskCompletion completion) {
    String observation = completion.getSpecificAction();
    String impact = completion.getOutcome();
    String trait = completion.getDemonstratedTrait();

    return String.format(
        "I noticed %s. That really helped with %s. Shows your %s.",
        observation, impact, trait
    );
}
```

### Praise Templates

| Situation | Template | Example |
|-----------|----------|---------|
| Speed | "You finished that quickly, which gave us time for [next task]. Shows your efficiency." | "You finished that quickly, which gave us time to start the roof. Shows your efficiency." |
| Quality | "The [detail] is perfect. That attention to [quality aspect] really stands out." | "The corners are perfectly aligned. That attention to precision really stands out." |
| Creativity | "I never would have thought to [solution]. That's a creative approach to [problem]." | "I never would have thought to use water for the elevator. That's a creative approach to vertical transport." |
| Perseverance | "You stuck with [task] even when it was tricky. That determination is valuable." | "You stuck with the redstone timing even when it was tricky. That determination is valuable." |
| Improvement | "This is much better than [previous attempt]. You've really improved at [skill]." | "This is much smoother than your first attempt. You've really improved at flow control." |
| Teamwork | "You [helpful action] which helped [teammate]. That's what makes a good team." | "You gathered materials for both of us which helped Sarah finish her section. That's what makes a good team." |

### Avoiding Generic Praise

**Don't Say:** Say Instead:
- "Good job!" → "The way you aligned those blocks made the wall perfectly straight."
- "Nice work!" → "I noticed you double-checked the measurements. That prevented a mistake."
- "You're great!" → "Your solution to the water flow problem was creative and effective."

---

## Non-Condescending Correction

### The CSS Feedback Framework

**Clear, Specific, Supportive**

```java
public String generateCorrection(TaskError error) {
    return String.format(
        "I noticed %s. %s Let's %s.",
        getSpecificObservation(error),
        getImpactExplanation(error),
        getCollaborativeSolution(error)
    );
}
```

### Correction Templates

| Situation | Observation | Impact | Solution |
|-----------|-------------|--------|----------|
| Wrong material | "I see you used [wrong material] here" | "It might [consequence]" | "Let's try [better material] instead" |
| Missing step | "This section is missing [step]" | "That could cause [problem]" | "Let's add [step] here" |
| Safety issue | "I noticed [dangerous thing]" | "That could be unsafe" | "Let's move it to [safer location]" |
| Efficiency issue | "You're doing [method]" | "That takes [time/effort]" | "Have you tried [faster method]?" |

### Tone Guidelines

1. **Use "I notice" instead of "You did":**
   - "I notice this section is shorter" vs "You built this wrong"

2. **Use "Let's" instead of "You should":**
   - "Let's adjust this" vs "You need to fix this"

3. **Focus on the work, not the worker:**
   - "This approach has an issue" vs "You made a mistake"

4. **Share responsibility:**
   - "I should have been clearer about..." (when foreman's instructions were unclear)
   - "We can work on this together..."

5. **End with forward-looking confidence:**
   - "You'll get the hang of this with practice."
   - "Next time will be smoother."

---

## Celebrating Progress

### Milestone Recognition

```java
public enum SkillMilestone {
    FIRST_SUCCESS("First time completing this task!", 5),
    CONSISTENT_PERFORMANCE("Five in a row!", 10),
    SPEED_IMPROVEMENT("Much faster than before!", 8),
    QUALITY_LEAP("Noticeable quality improvement!", 10),
    INDEPENDENCE("Completed without any help!", 15),
    TEACHING_OTHER("Now helping others learn!", 20),
    INNOVATION("Found a creative new approach!", 15);
}
```

### Celebration Dialogue Patterns

**1. Growth-Focused Celebrations:**

> "Remember when you first started on this? You've come so far!"

> "That's the fifth time you've nailed this. You've really mastered it!"

**2. Comparison to Past Self:**

> "This took you half the time it did last week. Your improvement is noticeable!"

> "Your first attempt was good, but this version is so much more refined!"

**3. Public Recognition (when appropriate):**

> Foreman to group: "Everyone, look at how [Worker] solved the redstone timing. Clever approach!"

**4. Progress Tracking:**

> "You've completed 12 walls successfully. You're officially a wall-building expert!"

---

## Asking for Help

### Foreman Vulnerability Patterns

**When Foremen Should Admit Uncertainty:**

1. **Novel situations:** "I've never built this exact design before. Let's figure it out together."
2. **Worker's expertise:** "You've done more farming than I have. What's your approach?"
3. **Learning moments:** "You know, I'm not sure about the best way here. What do you think?"
4. **Mistakes:** "Actually, that was my error. I should have specified..."

### Dialogue Examples

**1. Admitting Knowledge Gaps:**

> "You know what? I haven't actually built a nether portal farm before. Do you have any ideas?"

> "That's a good question about the water flow. I'm actually not 100% sure. Let's test it and see."

**2. Valuing Worker Input:**

> "You've been working with redstone more than I have lately. What would you suggest?"

> "I was going to do it this way, but you might have a better perspective. What do you think?"

**3. Collaborative Problem-Solving:**

> Foreman: "I'm stuck on this roof design. It keeps looking off."
>
> Worker: "Maybe try increasing the overhang?"
>
> Foreman: "That's a good idea! Let me try that."

**4. Acknowledging Foreman Errors:**

> "Actually, that's my fault. I should have measured before starting. Let's redo it together."

> "You were right about the material. I was wrong to suggest stonebrick. Oak looks better here."

---

## Java Implementation

### MentorshipManager.java

```java
package com.minewright.mentorship;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.CompanionMemory;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages mentorship dynamics between Foreman and Worker entities.
 *
 * <p>This system tracks learning progress, adjusts dialogue based on skill levels,
 * provides scaffolding for skill development, and creates genuine teaching moments.</p>
 *
 * <p><b>Core Features:</b></p>
 * <ul>
 *   <li>Teaching moment detection based on worker behavior</li>
 *   <li>Explanation depth adjustment by skill level</li>
 *   <li>Scaffolding dialogue that fades as competence grows</li>
 *   <li>Genuine praise that feels earned and specific</li>
 *   <li>Non-condescending correction patterns</li>
 *   <li>Progress celebration and milestone tracking</li>
 *   <li>Foreman vulnerability and asking for help</li>
 * </ul>
 *
 * @since 1.5.0
 */
public class MentorshipManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MentorshipManager.class);

    private final ForemanEntity foreman;
    private final Map<String, WorkerProfile> workers;
    private final MentorshipPersonality foremanPersonality;

    // Teaching state
    private final Map<String, Instant> lastTeachingMoment;
    private final Map<String, Integer> consecutiveSuccesses;
    private final Map<String, Set<String>> taughtConcepts;

    public MentorshipManager(ForemanEntity foreman) {
        this.foreman = foreman;
        this.workers = new ConcurrentHashMap<>();
        this.foremanPersonality = new MentorshipPersonality();
        this.lastTeachingMoment = new ConcurrentHashMap<>();
        this.consecutiveSuccesses = new ConcurrentHashMap<>();
        this.taughtConcepts = new ConcurrentHashMap<>();
    }

    // ========== Worker Registration ==========

    /**
     * Registers a worker for mentorship tracking.
     *
     * @param workerName Unique name of the worker
     * @param workerRole Role of the worker (e.g., "builder", "miner")
     */
    public void registerWorker(String workerName, String workerRole) {
        workers.put(workerName, new WorkerProfile(workerName, workerRole));
        taughtConcepts.put(workerName, ConcurrentHashMap.newKeySet());
        consecutiveSuccesses.put(workerName, 0);

        LOGGER.info("Registered worker '{}' for mentorship (role: {})",
            workerName, workerRole);
    }

    // ========== Teaching Moment Detection ==========

    /**
     * Detects if a teaching moment should occur based on worker behavior.
     *
     * @param workerName The worker to check
     * @param triggerType What triggered the potential teaching moment
     * @param context Context about the situation
     * @return Teaching moment if detected, null otherwise
     */
    public TeachingMoment detectTeachingMoment(String workerName,
            TeachingMomentTrigger triggerType, String context) {

        WorkerProfile worker = workers.get(workerName);
        if (worker == null) {
            LOGGER.warn("Cannot detect teaching moment for unknown worker: {}", workerName);
            return null;
        }

        // Check if worker is receptive to teaching
        if (!isWorkerTeachable(worker)) {
            return null;
        }

        // Check cooldown
        Instant lastTaught = lastTeachingMoment.get(workerName);
        if (lastTaught != null) {
            long minutesSince = Instant.now().toEpochMilli() - lastTaught.toEpochMilli();
            if (minutesSince < getCooldownForTrigger(triggerType)) {
                return null;  // Too soon since last teaching moment
            }
        }

        // Determine if this is a teaching moment
        TeachingMoment moment = evaluateTeachingMoment(worker, triggerType, context);
        if (moment != null) {
            lastTeachingMoment.put(workerName, Instant.now());
        }

        return moment;
    }

    /**
     * Evaluates if a situation constitutes a teaching moment.
     */
    private TeachingMoment evaluateTeachingMoment(WorkerProfile worker,
            TeachingMomentTrigger trigger, String context) {

        return switch (trigger) {
            case WORKER_STUCK -> createStuckTeachingMoment(worker, context);
            case WORKER_MISTAKE -> createMistakeTeachingMoment(worker, context);
            case WORKER_SUCCESS_SUBOPTIMAL -> createImprovementTeachingMoment(worker, context);
            case WORKER_QUESTION -> createAnswerTeachingMoment(worker, context);
            case NEW_CHALLENGE -> createChallengeTeachingMoment(worker, context);
            case SKILL_MILESTONE -> createCelebrationMoment(worker, context);
            case PATTERN_RECOGNITION -> createPatternTeachingMoment(worker, context);
        };
    }

    private TeachingMoment createStuckTeachingMoment(WorkerProfile worker, String context) {
        // Don't teach if worker is frustrated
        if (worker.getStressLevel() > 0.7) {
            return null;
        }

        SkillLevel taskLevel = estimateTaskDifficulty(context);
        SkillLevel workerLevel = worker.getSkillLevel(context);

        if (taskLevel.ordinal() > workerLevel.ordinal() + 1) {
            // Task is too hard - demonstrate
            return TeachingMoment.handsOn(context, "This is tricky, let me show you...");
        }

        // Provide hint
        return TeachingMoment.hint(context, generateHint(worker, context));
    }

    private TeachingMoment createMistakeTeachingMoment(WorkerProfile worker, String context) {
        // Check if worker has made this mistake before
        if (worker.hasRepeatedMistake(context)) {
            // Pattern - need more direct teaching
            return TeachingMoment.correction(context, generateCorrection(worker, context));
        }

        // First time mistake - gentle guidance
        return TeachingMoment.gentleGuidance(context, generateGentleCorrection(worker, context));
    }

    private TeachingMoment createImprovementTeachingMoment(WorkerProfile worker, String context) {
        // Worker succeeded but inefficiently
        return TeachingMoment.suggestion(context, generateSuggestion(worker, context));
    }

    private TeachingMoment createAnswerTeachingMoment(WorkerProfile worker, String context) {
        // Worker asked a question - use Socratic method
        return TeachingMoment.socratic(context, generateSocraticQuestions(context));
    }

    private TeachingMoment createChallengeTeachingMoment(WorkerProfile worker, String context) {
        return TeachingMoment.collaborative(context, "I haven't seen this exact situation before. " +
            "Let's figure it out together. What do you think is the best approach?");
    }

    private TeachingMoment createCelebrationMoment(WorkerProfile worker, String context) {
        String celebration = generateCelebration(worker, context);
        return TeachingMoment.celebration(context, celebration);
    }

    private TeachingMoment createPatternTeachingMoment(WorkerProfile worker, String context) {
        return TeachingMoment.insight(context, "I've noticed something interesting. " +
            generateInsight(worker, context));
    }

    // ========== Explanation Depth Adjustment ==========

    /**
     * Determines the appropriate explanation depth for a worker.
     */
    public ExplanationDepth getExplanationDepth(String workerName, String taskContext) {
        WorkerProfile worker = workers.get(workerName);
        if (worker == null) {
            return ExplanationDepth.DETAILED;  // Default to detailed for unknown workers
        }

        SkillLevel workerLevel = worker.getSkillLevel(taskContext);
        SkillLevel taskLevel = estimateTaskDifficulty(taskContext);

        int gap = taskLevel.ordinal() - workerLevel.ordinal();

        // Adjust based on rapport and stress
        if (worker.getStressLevel() > 0.6) {
            // Stressed workers need more support
            return ExplanationDepth.DETAILED;
        }

        if (worker.getRapportLevel() > 60 && gap <= 0) {
            // High rapport + easier task = minimal guidance
            return ExplanationDepth.CONFIRMATION;
        }

        return getDepthForGap(gap);
    }

    private ExplanationDepth getDepthForGap(int gap) {
        return switch (gap) {
            case -2, -3 -> ExplanationDepth.MINIMAL;
            case -1 -> ExplanationDepth.CONFIRMATION;
            case 0 -> ExplanationDepth.HINTS;
            case 1 -> ExplanationDepth.SCAFFOLDED;
            case 2 -> ExplanationDepth.DETAILED;
            default -> ExplanationDepth.HANDS_ON;
        };
    }

    // ========== Scaffolding Dialogue Generation ==========

    /**
     * Generates a hint without giving the solution.
     */
    public String generateHint(WorkerProfile worker, String context) {
        int hintLevel = getHintLevel(worker, context);

        return switch (hintLevel) {
            case 1 -> getConceptualHint(context);
            case 2 -> getProcessHint(context);
            case 3 -> getSpecificHint(context);
            default -> "What do you think the first step is?";
        };
    }

    private int getHintLevel(WorkerProfile worker, String context) {
        // First hint: conceptual
        // Second hint: process
        // Third hint: specific
        return worker.getHintCount(context) + 1;
    }

    private String getConceptualHint(String context) {
        if (context.contains("wall") || context.contains("build")) {
            return "Think about what makes structures stable and level.";
        }
        if (context.contains("redstone") || context.contains("circuit")) {
            return "Consider how the signal flows and what powers it.";
        }
        if (context.contains("water") || context.contains("flow")) {
            return "Think about how water moves in Minecraft and what blocks it can pass through.";
        }
        if (context.contains("farm") || context.contains("crop")) {
            return "What do crops need to grow successfully?";
        }
        return "What's the core principle here?";
    }

    private String getProcessHint(String context) {
        if (context.contains("wall") || context.contains("build")) {
            return "Start with a solid foundation, then build up in layers.";
        }
        if (context.contains("redstone") || context.contains("circuit")) {
            return "Trace the path from power source to output. Where's the signal stopping?";
        }
        if (context.contains("water") || context.contains("flow")) {
            return "Water flows and spreads. Check if there's a path for it to follow.";
        }
        if (context.contains("farm") || context.contains("crop")) {
            return "Consider the layout for water reach and light levels.";
        }
        return "Break it down into smaller steps.";
    }

    private String getSpecificHint(String context) {
        if (context.contains("wall") || context.contains("build")) {
            return "Try starting with the corners to frame the structure.";
        }
        if (context.contains("redstone") || context.contains("circuit")) {
            return "A repeater might help boost the signal. What direction should it face?";
        }
        if (context.contains("water") || context.contains("flow")) {
            return "Water hydrates soil up to 4 blocks away. Check your spacing.";
        }
        if (context.contains("farm") || context.contains("crop")) {
            return "Crops need light level 9 or higher. Is your farm well-lit?";
        }
        return "Focus on one part at a time.";
    }

    /**
     * Generates Socratic questioning sequence.
     */
    public List<String> generateSocraticQuestions(String context) {
        List<String> questions = new ArrayList<>();

        questions.add("What's your goal here?");
        questions.add("What have you tried so far?");

        if (context.contains("build") || context.contains("create")) {
            questions.add("What would happen if we tried a different arrangement?");
            questions.add("Is there a way to make this more efficient?");
        } else if (context.contains("redstone") || context.contains("mechanism")) {
            questions.add("Where is the signal flowing?");
            questions.add("What's powering this component?");
        } else {
            questions.add("What do you think would happen if...?");
        }

        questions.add("What have you learned from this?");
        return questions;
    }

    // ========== Praise Generation ==========

    /**
     * Generates specific, genuine praise for worker achievement.
     */
    public String generatePraise(String workerName, TaskCompletion completion) {
        WorkerProfile worker = workers.get(workerName);
        if (worker == null) {
            return "Great job!";
        }

        String observation = completion.getSpecificAction();
        String impact = completion.getOutcome();
        String trait = completion.getDemonstratedTrait();

        // Use different templates based on trait
        return switch (trait.toLowerCase()) {
            case "speed", "efficiency" -> String.format(
                "You finished that quickly, which gave us time for %s. Shows your efficiency.",
                impact, observation
            );
            case "quality", "precision", "attention to detail" -> String.format(
                "The %s is perfect. That attention to detail really stands out.",
                observation
            );
            case "creativity", "innovation" -> String.format(
                "I never would have thought to %s. That's a creative approach to %s.",
                observation, impact
            );
            case "perseverance", "determination" -> String.format(
                "You stuck with %s even when it was tricky. That determination is valuable.",
                observation
            );
            case "improvement", "growth" -> String.format(
                "This is much better than your previous attempts. You've really improved at %s.",
                trait
            );
            case "teamwork", "helping" -> String.format(
                "Your %s helped with %s. That's what makes a good team.",
                observation, impact
            );
            default -> String.format(
                "I noticed %s. That really helped with %s. Shows your %s.",
                observation, impact, trait
            );
        };
    }

    // ========== Correction Generation ==========

    /**
     * Generates non-condescending correction using CSS framework.
     */
    public String generateCorrection(WorkerProfile worker, String context) {
        TaskError error = analyzeError(worker, context);

        // Use CSS: Clear observation, Specific impact, Supportive solution
        return String.format(
            "I noticed %s. %s Let's %s.",
            getSpecificObservation(error),
            getImpactExplanation(error),
            getCollaborativeSolution(error)
        );
    }

    private String generateGentleCorrection(WorkerProfile worker, String context) {
        return String.format(
            "Hmm, let me show you something. %s Would you like to try a different approach?",
            getSpecificObservation(analyzeError(worker, context))
        );
    }

    private String getSpecificObservation(TaskError error) {
        return switch (error.getType()) {
            case WRONG_MATERIAL -> String.format("you used %s here", error.getDetail());
            case MISSING_STEP -> String.format("this section is missing %s", error.getDetail());
            case SAFETY_ISSUE -> String.format("there's a %s concern", error.getDetail());
            case EFFICIENCY_ISSUE -> String.format("you're doing %s", error.getDetail());
            default -> "something seems off here";
        };
    }

    private String getImpactExplanation(TaskError error) {
        return switch (error.getType()) {
            case WRONG_MATERIAL -> String.format("It might not hold up as well as %s would.",
                error.getSuggestedAlternative());
            case MISSING_STEP -> String.format("That could cause issues with %s.", error.getDetail());
            case SAFETY_ISSUE -> "That could be unsafe.";
            case EFFICIENCY_ISSUE -> "That takes more time than necessary.";
            default -> "Let's adjust this.";
        };
    }

    private String getCollaborativeSolution(TaskError error) {
        return switch (error.getType()) {
            case WRONG_MATERIAL -> String.format("try using %s instead", error.getSuggestedAlternative());
            case MISSING_STEP -> String.format("add %s here", error.getDetail());
            case SAFETY_ISSUE -> "move it to a safer location";
            case EFFICIENCY_ISSUE -> "try this more efficient approach";
            default -> "fix this together";
        };
    }

    // ========== Celebration Generation ==========

    /**
     * Generates celebration for worker progress.
     */
    public String generateCelebration(WorkerProfile worker, String context) {
        SkillMilestone milestone = detectMilestone(worker, context);

        return switch (milestone) {
            case FIRST_SUCCESS -> String.format(
                "First time completing %s! Well done on getting through it!",
                context
            );
            case CONSISTENT_PERFORMANCE -> {
                consecutiveSuccesses.merge(worker.getName(), 1, Integer::sum);
                yield String.format(
                    "That's the %d time in a row you've nailed %s. You've really got this down!",
                    consecutiveSuccesses.get(worker.getName()), context
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
            case INDEPENDENCE -> "You completed that entirely on your own! That's real progress.";
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

    private SkillMilestone detectMilestone(WorkerProfile worker, String context) {
        int successCount = worker.getSuccessCount(context);

        if (successCount == 1) return SkillMilestone.FIRST_SUCCESS;
        if (successCount == 5) return SkillMilestone.CONSISTENT_PERFORMANCE;
        if (worker.hasImprovedSpeed(context)) return SkillMilestone.SPEED_IMPROVEMENT;
        if (worker.hasImprovedQuality(context)) return SkillMilestone.QUALITY_LEAP;
        if (worker.recentlyCompletedWithoutHelp(context)) return SkillMilestone.INDEPENDENCE;
        if (worker.usedCreativeApproach(context)) return SkillMilestone.INNOVATION;

        return SkillMilestone.FIRST_SUCCESS;  // Default
    }

    // ========== Foreman Vulnerability ==========

    /**
     * Generates dialogue where foreman admits uncertainty or asks for help.
     */
    public String generateForemanVulnerability(String context) {
        if (foremanPersonality.shouldAdmitUncertainty(context)) {
            return String.format(
                "You know, I haven't actually done %s before. What's your approach?",
                context
            );
        }

        if (foremanPersonality.shouldAskWorker(context)) {
            return String.format(
                "You've been working with %s more than I have lately. What would you suggest?",
                context
            );
        }

        return null;
    }

    // ========== Helper Methods ==========

    private boolean isWorkerTeachable(WorkerProfile worker) {
        return worker.getFocusLevel() > 0.6
            && worker.getStressLevel() < 0.7
            && worker.getRapportLevel() > 20;
    }

    private long getCooldownForTrigger(TeachingMomentTrigger trigger) {
        return switch (trigger) {
            case WORKER_STUCK -> 2 * 60 * 1000L;  // 2 minutes
            case WORKER_MISTAKE -> 5 * 60 * 1000L;  // 5 minutes
            case WORKER_SUCCESS_SUBOPTIMAL -> 3 * 60 * 1000L;  // 3 minutes
            case WORKER_QUESTION -> 0;  // Always answer questions
            case NEW_CHALLENGE -> 10 * 60 * 1000L;  // 10 minutes
            case SKILL_MILESTONE -> 0;  // Always celebrate
            case PATTERN_RECOGNITION -> 5 * 60 * 1000L;  // 5 minutes
        };
    }

    private SkillLevel estimateTaskDifficulty(String context) {
        // Simple heuristic - could be enhanced with ML
        if (context.contains("complex") || context.contains("redstone")) {
            return SkillLevel.ADVANCED;
        }
        if (context.contains("build") || context.contains("create")) {
            return SkillLevel.INTERMEDIATE;
        }
        return SkillLevel.BEGINNER;
    }

    private TaskError analyzeError(WorkerProfile worker, String context) {
        // Analyze the error based on context
        // This is simplified - real implementation would parse the actual error
        return new TaskError(TaskError.ErrorType.EFFICIENCY_ISSUE, context, "a better way");
    }

    private String generateSuggestion(WorkerProfile worker, String context) {
        return String.format(
            "You completed it, but there might be a more efficient way. Next time, try %s.",
            getProcessHint(context)
        );
    }

    private String generateInsight(WorkerProfile worker, String context) {
        return "you've been improving consistently with " + context +
            ". Your technique is getting much smoother.";
    }

    // ========== NBT Persistence ==========

    public void saveToNBT(CompoundTag tag) {
        CompoundTag mentorshipTag = new CompoundTag();

        // Save worker profiles
        for (Map.Entry<String, WorkerProfile> entry : workers.entrySet()) {
            CompoundTag workerTag = new CompoundTag();
            entry.getValue().saveToNBT(workerTag);
            mentorshipTag.put(entry.getKey(), workerTag);
        }

        tag.put("MentorshipData", mentorshipTag);
    }

    public void loadFromNBT(CompoundTag tag) {
        if (!tag.contains("MentorshipData")) {
            return;
        }

        CompoundTag mentorshipTag = tag.getCompound("MentorshipData");

        for (String workerName : mentorshipTag.getAllKeys()) {
            CompoundTag workerTag = mentorshipTag.getCompound(workerName);
            WorkerProfile profile = WorkerProfile.loadFromNBT(workerName);
            workers.put(workerName, profile);
            taughtConcepts.put(workerName, ConcurrentHashMap.newKeySet());
        }

        LOGGER.info("Loaded mentorship data for {} workers", workers.size());
    }

    // ========== Inner Classes ==========

    /**
     * Represents a detected teaching moment.
     */
    public static class TeachingMoment {
        private final String context;
        private final String dialogue;
        private final TeachingType type;

        private TeachingMoment(String context, String dialogue, TeachingType type) {
            this.context = context;
            this.dialogue = dialogue;
            this.type = type;
        }

        public static TeachingMoment handsOn(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.HANDS_ON);
        }

        public static TeachingMoment hint(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.HINT);
        }

        public static TeachingMoment correction(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.CORRECTION);
        }

        public static TeachingMoment gentleGuidance(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.GENTLE_GUIDANCE);
        }

        public static TeachingMoment suggestion(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.SUGGESTION);
        }

        public static TeachingMoment socratic(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.SOCRATIC);
        }

        public static TeachingMoment collaborative(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.COLLABORATIVE);
        }

        public static TeachingMoment celebration(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.CELEBRATION);
        }

        public static TeachingMoment insight(String context, String dialogue) {
            return new TeachingMoment(context, dialogue, TeachingType.INSIGHT);
        }

        public String getContext() { return context; }
        public String getDialogue() { return dialogue; }
        public TeachingType getType() { return type; }

        public enum TeachingType {
            HANDS_ON, HINT, CORRECTION, GENTLE_GUIDANCE,
            SUGGESTION, SOCRATIC, COLLABORATIVE, CELEBRATION, INSIGHT
        }
    }

    /**
     * Worker skill and progress tracking.
     */
    public static class WorkerProfile {
        private final String name;
        private final String role;
        private final Map<String, SkillLevel> skills;
        private final Map<String, Integer> taskHistory;
        private int rapportLevel;
        private double stressLevel;
        private double focusLevel;
        private final Set<String> repeatedMistakes;

        public WorkerProfile(String name, String role) {
            this.name = name;
            this.role = role;
            this.skills = new ConcurrentHashMap<>();
            this.taskHistory = new ConcurrentHashMap<>();
            this.rapportLevel = 10;
            this.stressLevel = 0.0;
            this.focusLevel = 1.0;
            this.repeatedMistakes = ConcurrentHashMap.newKeySet();
        }

        public SkillLevel getSkillLevel(String context) {
            return skills.getOrDefault(context, SkillLevel.BEGINNER);
        }

        public void improveSkill(String context) {
            SkillLevel current = getSkillLevel(context);
            if (current.ordinal() < SkillLevel.values().length - 1) {
                skills.put(context, SkillLevel.values()[current.ordinal() + 1]);
            }
        }

        public int getSuccessCount(String context) {
            return taskHistory.getOrDefault(context + "_success", 0);
        }

        public boolean hasRepeatedMistake(String context) {
            return repeatedMistakes.contains(context);
        }

        public boolean hasImprovedSpeed(String context) {
            // Check if recent completion was faster
            return taskHistory.getOrDefault(context + "_fast", 0) > 1;
        }

        public boolean hasImprovedQuality(String context) {
            return taskHistory.getOrDefault(context + "_quality", 0) > 1;
        }

        public boolean recentlyCompletedWithoutHelp(String context) {
            return taskHistory.getOrDefault(context + "_independent", 0) > 0;
        }

        public boolean usedCreativeApproach(String context) {
            return taskHistory.containsKey(context + "_creative");
        }

        public int getHintCount(String context) {
            return taskHistory.getOrDefault(context + "_hints", 0);
        }

        // Getters
        public String getName() { return name; }
        public String getRole() { return role; }
        public int getRapportLevel() { return rapportLevel; }
        public double getStressLevel() { return stressLevel; }
        public double getFocusLevel() { return focusLevel; }

        public void saveToNBT(CompoundTag tag) {
            tag.putString("Name", name);
            tag.putString("Role", role);
            tag.putInt("Rapport", rapportLevel);
            tag.putDouble("Stress", stressLevel);
            tag.putDouble("Focus", focusLevel);

            // Save skills
            CompoundTag skillsTag = new CompoundTag();
            for (Map.Entry<String, SkillLevel> entry : skills.entrySet()) {
                skillsTag.putInt(entry.getKey(), entry.getValue().ordinal());
            }
            tag.put("Skills", skillsTag);

            // Save task history
            CompoundTag historyTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : taskHistory.entrySet()) {
                historyTag.putInt(entry.getKey(), entry.getValue());
            }
            tag.put("TaskHistory", historyTag);
        }

        public static WorkerProfile loadFromNBT(CompoundTag tag) {
            String name = tag.getString("Name");
            String role = tag.getString("Role");
            WorkerProfile profile = new WorkerProfile(name, role);

            profile.rapportLevel = tag.getInt("Rapport");
            profile.stressLevel = tag.getDouble("Stress");
            profile.focusLevel = tag.getDouble("Focus");

            // Load skills
            if (tag.contains("Skills")) {
                CompoundTag skillsTag = tag.getCompound("Skills");
                for (String key : skillsTag.getAllKeys()) {
                    int level = skillsTag.getInt(key);
                    profile.skills.put(key, SkillLevel.values()[level]);
                }
            }

            // Load task history
            if (tag.contains("TaskHistory")) {
                CompoundTag historyTag = tag.getCompound("TaskHistory");
                for (String key : historyTag.getAllKeys()) {
                    profile.taskHistory.put(key, historyTag.getInt(key));
                }
            }

            return profile;
        }
    }

    /**
     * Foreman's personality for teaching.
     */
    public static class MentorshipPersonality {
        private boolean admitsUncertainty;
        private double willingnessToLearnFromWorkers;

        public MentorshipPersonality() {
            this.admitsUncertainty = true;
            this.willingnessToLearnFromWorkers = 0.7;
        }

        public boolean shouldAdmitUncertainty(String context) {
            // Admit uncertainty for novel or complex situations
            return admitsUncertainty && (
                context.contains("new") ||
                context.contains("experimental") ||
                context.contains("first time")
            );
        }

        public boolean shouldAskWorker(String context) {
            return willingnessToLearnFromWorkers > 0.5 && Math.random() < willingnessToLearnFromWorkers;
        }
    }

    /**
     * Skill levels for workers.
     */
    public enum SkillLevel {
        NOVICE, BEGINNER, APPRENTICE, COMPETENT, PROFICIENT, EXPERT
    }

    /**
     * Depth of explanation for teaching.
     */
    public enum ExplanationDepth {
        HANDS_ON, DETAILED, SCAFFOLDED, HINTS, CONFIRMATION, MINIMAL
    }

    /**
     * Triggers for teaching moments.
     */
    public enum TeachingMomentTrigger {
        WORKER_STUCK, WORKER_MISTAKE, WORKER_SUCCESS_SUBOPTIMAL,
        WORKER_QUESTION, NEW_CHALLENGE, SKILL_MILESTONE, PATTERN_RECOGNITION
    }

    /**
     * Skill achievement milestones.
     */
    public enum SkillMilestone {
        FIRST_SUCCESS, CONSISTENT_PERFORMANCE, SPEED_IMPROVEMENT,
        QUALITY_LEAP, INDEPENDENCE, INNOVATION, TEACHING_OTHER
    }

    /**
     * Error analysis for corrections.
     */
    public static class TaskError {
        private final ErrorType type;
        private final String detail;
        private final String suggestedAlternative;

        public TaskError(ErrorType type, String detail, String suggestedAlternative) {
            this.type = type;
            this.detail = detail;
            this.suggestedAlternative = suggestedAlternative;
        }

        public ErrorType getType() { return type; }
        public String getDetail() { return detail; }
        public String getSuggestedAlternative() { return suggestedAlternative; }

        public enum ErrorType {
            WRONG_MATERIAL, MISSING_STEP, SAFETY_ISSUE, EFFICIENCY_ISSUE
        }
    }

    /**
     * Task completion for praise.
     */
    public static class TaskCompletion {
        private final String specificAction;
        private final String outcome;
        private final String trait;

        public TaskCompletion(String specificAction, String outcome, String trait) {
            this.specificAction = specificAction;
            this.outcome = outcome;
            this.trait = trait;
        }

        public String getSpecificAction() { return specificAction; }
        public String getOutcome() { return outcome; }
        public String getDemonstratedTrait() { return trait; }
    }
}
```

---

## Integration with Existing Systems

### Integrating with ForemanEntity

```java
// In ForemanEntity.java

private MentorshipManager mentorshipManager;

public ForemanEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
    super(entityType, level);
    // ... existing initialization ...

    this.mentorshipManager = new MentorshipManager(this);
}

// Called when a worker completes a task
public void onWorkerTaskComplete(String workerName, String task, boolean success) {
    if (success) {
        MentorshipManager.TeachingMoment moment = mentorshipManager.detectTeachingMoment(
            workerName,
            TeachingMomentTrigger.WORKER_SUCCESS_SUBOPTIMAL,
            task
        );

        if (moment != null) {
            sendChatMessage(moment.getDialogue());
        }
    }
}

// Called when a worker gets stuck
public void onWorkerStuck(String workerName, String task) {
    MentorshipManager.TeachingMoment moment = mentorshipManager.detectTeachingMoment(
        workerName,
        TeachingMomentTrigger.WORKER_STUCK,
        task
    );

    if (moment != null) {
        sendChatMessage(moment.getDialogue());
    }
}
```

---

## Sources

- [Star Wars Mentor-Apprentice Patterns - StudySmarter](https://www.studysmarter.co.uk/explanations/greek/greek-history/greek-socratic-method/)
- [Socratic Method - Britannica](https://www.britannica.com/biography/Socrates/Plato)
- [Socratic Teaching - ClassPoint](https://www.classpoint.io/blog/socratic-method-of-teaching)
- [Instructional Scaffolding - Edutopia](https://www.edutopia.org/article/powerful-scaffolding-strategies-support-learning/)
- [Scaffolding Learning - University College Cork](https://www.ucc.ie/en/cirtl/resources/shortguides/shortguide2scaffoldinglearning/)
- [Positive Reinforcement Patterns - LinkedIn](https://www.linkedin.com/top-content/training-development/supporting-employee-career-development/how-to-provide-constructive-feedback-for-development/)
- [Peer Review Feedback - Deel](https://www.deel.com/blog/peer-review-feedback-examples/)
- [Constructive Feedback - HR Platform](https://www.hrloo.com/rz/14688334.html)
- [Master Craftsman Heritage - Frontiers in Psychology](https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2022.807619/full)
- [Craftsmanship Inheritance Patterns - MDPI Sustainability](https://www.mdpi.com/2071-1050/14/22/14719)
- [NPC Training Patent - EON Reality](https://eonreality.com/apprenticeship-connect-unveiled-master-apprentice-matching-platform-developed-using-eon-ai-powered-entrepreneur-guide/?lang=zh-hans)
