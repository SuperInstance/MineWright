# Mentorship Dialogue System - Quick Reference

## Overview

The `MentorshipManager` handles teaching, learning, and mentorship dynamics between Foremen and Workers in MineWright.

## Quick Start

```java
// In ForemanEntity.java
private MentorshipManager mentorshipManager;

public ForemanEntity(...) {
    // ...
    this.mentorshipManager = new MentorshipManager(this);
}

// Register workers when they join
mentorshipManager.registerWorker("BuilderBob", "builder");
mentorshipManager.registerWorker("MinerMike", "miner");

// Detect and handle teaching moments
TeachingMoment moment = mentorshipManager.detectTeachingMoment(
    "BuilderBob",
    TeachingMomentTrigger.WORKER_STUCK,
    "building a redstone circuit"
);

if (moment != null) {
    sendChatMessage(moment.getDialogue());
}
```

## Teaching Moment Triggers

| Trigger | When to Use | Cooldown |
|---------|-------------|----------|
| `WORKER_STUCK` | Worker can't proceed with task | 2 minutes |
| `WORKER_MISTAKE` | Worker made an error | 5 minutes |
| `WORKER_SUCCESS_SUBOPTIMAL` | Succeeded but inefficiently | 3 minutes |
| `WORKER_QUESTION` | Worker asks for help | None (always respond) |
| `NEW_CHALLENGE` | Novel situation encountered | 10 minutes |
| `SKILL_MILESTONE` | Worker demonstrates growth | None (always celebrate) |
| `PATTERN_RECOGNITION` | Foreman notices pattern | 5 minutes |

## Teaching Moment Types

| Type | Description | Example |
|------|-------------|---------|
| `HANDS_ON` | Full demonstration | "This is tricky, let me show you..." |
| `HINT` | Guided hint without solution | "Think about what makes structures stable..." |
| `CORRECTION` | Direct correction (repeated mistakes) | "I noticed you used wood. Let's try stone..." |
| `GENTLE_GUIDANCE` | Gentle correction (first-time) | "Hmm, let me show you something..." |
| `SUGGESTION` | Improvement suggestion | "You completed it, but there might be a faster way..." |
| `SOCRATIC` | Question-based guidance | "What's your goal here? What have you tried?" |
| `COLLABORATIVE` | Working together | "Let's figure this out together..." |
| `CELEBRATION` | Milestone celebration | "That's the fifth time in a row!" |
| `INSIGHT` | Pattern recognition | "You've been improving consistently..." |

## Explanation Depth Levels

The system automatically adjusts explanation depth based on skill gap:

| Gap (Task - Worker) | Depth | Approach |
|---------------------|-------|----------|
| -3 or less | MINIMAL | "You've got this." |
| -2 | CONFIRMATION | "You've done this before." |
| -1 | HINTS | Gentle guidance |
| 0 | HINTS | "Think about..." |
| +1 | SCAFFOLDED | "We'll do this in steps..." |
| +2 | DETAILED | Full explanation |
| +3 or more | HANDS_ON | Demonstration |

## Skill Levels

| Level | Name | Description |
|-------|------|-------------|
| 0 | NOVICE | No experience, needs rules |
| 1 | BEGINNER | Basic awareness |
| 2 | APPRENTICE | Can do with help |
| 3 | COMPETENT | Independent |
| 4 | PROFICIENT | Handles complexity |
| 5 | EXPERT | Masterful, innovative |

## Hint System

Three-tier hint system that escalates:

1. **Conceptual** - "Think about what makes structures stable."
2. **Process** - "Start with a solid foundation, then build up."
3. **Specific** - "Try starting with the corners."

## Praise Templates

Use specific, not generic praise:

```java
// Create specific praise
TaskCompletion completion = new TaskCompletion(
    "aligned the blocks perfectly",  // specific action
    "the wall is straight",            // outcome
    "attention to detail"              // trait demonstrated
);

String praise = mentorshipManager.generatePraise("BuilderBob", completion);
// "The aligned the blocks perfectly is perfect. That attention to detail really stands out."
```

## Correction Framework

Use CSS (Clear, Specific, Supportive):

```java
// Automatic correction generation
String correction = mentorshipManager.generateCorrection(worker, context);
// "I noticed you used wood here. It might not hold up as well as stone would. Let's try using stone instead."
```

## Milestone Celebrations

The system tracks and celebrates:

| Milestone | Trigger | Example Dialogue |
|-----------|---------|------------------|
| FIRST_SUCCESS | First completion | "First time building a farm!" |
| CONSISTENT_PERFORMANCE | 5th success | "That's 5 in a row!" |
| SPEED_IMPROVEMENT | Faster than before | "Much faster than last time!" |
| QUALITY_LEAP | Better quality | "Noticeably better than before!" |
| INDEPENDENCE | No help needed | "Completed entirely on your own!" |
| INNOVATION | Creative approach | "Never seen that approach before!" |
| TEACHING_OTHER | Helping others | "You've come full circle!" |

## Foreman Vulnerability

Foremen can admit uncertainty:

```java
String vulnerability = mentorshipManager.generateForemanVulnerability("nether portal farm");
// "You know, I haven't actually done nether portal farm before. What's your approach?"
```

## Persistence

Save/load mentorship state:

```java
// In ForemanEntity.saveToNBT()
mentorshipManager.saveToNBT(tag);

// In ForemanEntity.loadFromNBT()
mentorshipManager.loadFromNBT(tag);
```

## Integration with ActionExecutor

```java
// In ActionExecutor when worker completes task
public void onWorkerTaskComplete(String workerName, String task, boolean success) {
    if (success) {
        // Track success
        WorkerProfile worker = mentorshipManager.getWorker(workerName);
        if (worker != null) {
            worker.recordSuccess(task);
        }

        // Check for teaching moment
        TeachingMoment moment = mentorshipManager.detectTeachingMoment(
            workerName,
            TeachingMomentTrigger.SKILL_MILESTONE,
            task
        );

        if (moment != null) {
            foreman.sendChatMessage(moment.getDialogue());
        }
    }
}

// When worker gets stuck
public void onWorkerStuck(String workerName, String task) {
    TeachingMoment moment = mentorshipManager.detectTeachingMoment(
        workerName,
        TeachingMomentTrigger.WORKER_STUCK,
        task
    );

    if (moment != null) {
        foreman.sendChatMessage(moment.getDialogue());
    }
}
```

## Worker State Management

```java
// Update worker state
WorkerProfile worker = mentorshipManager.getWorker("BuilderBob");
worker.setFocusLevel(0.8);      // 0.0 to 1.0
worker.setStressLevel(0.3);     // 0.0 to 1.0
worker.setRapportLevel(45);     // 0 to 100
```

## Socratic Question Examples

The system generates contextual questions:

- "What's your goal here?"
- "What have you tried so far?"
- "What would happen if we tried a different arrangement?"
- "Where is the signal flowing?"
- "What do crops need to thrive?"
- "What have you learned from this?"

## Best Practices

1. **Check teachable state** before using teaching moments
2. **Use specific praise**, not generic "good job"
3. **Escalate hints gradually** (conceptual → process → specific)
4. **Celebrate milestones** to reinforce progress
5. **Admit foreman uncertainty** for authenticity
6. **Track repeated mistakes** for deeper intervention
7. **Adjust for stress** - stressed workers need more support
8. **Fade support** as workers improve

## Configuration

Adjust foreman teaching personality:

```java
MentorshipPersonality personality = new MentorshipPersonality();
personality.setAdmitsUncertainty(true);
personality.setWillingnessToLearnFromWorkers(0.8);
```

## Key Classes

- `MentorshipManager` - Main mentorship system
- `WorkerProfile` - Individual worker tracking
- `TeachingMoment` - Detected teaching opportunity
- `TaskCompletion` - For praise generation
- `TaskError` - For correction generation

## Research Sources

This system is based on research from:

- Star Wars mentorship patterns (character testing, philosophy over technique)
- Socratic teaching method (questioning techniques)
- Instructional scaffolding (Vygotsky's Zone of Proximal Development)
- Workplace coaching (CSS Framework)
- Master craftsman traditions (observation, demonstration, practice)
- Positive reinforcement (specific vs. generic praise)
- Constructive feedback (non-condescending correction)

See `MENTORSHIP_DIALOGUE.md` for full research details.
