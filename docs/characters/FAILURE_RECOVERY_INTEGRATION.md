# Failure Response Integration Guide

## Integrating with Steve AI's Action System

The FailureResponseGenerator integrates seamlessly with Steve AI's existing action execution system.

### Integration Points

#### 1. Action Execution Failure Handling

```java
// In your BaseAction or ActionExecutor
@Override
public ActionResult execute(ActionContext context) {
    try {
        // Attempt the action
        return doAction(context);
    } catch (ActionFailureException failure) {
        // Classify the failure
        int severity = classifyFailureSeverity(failure);

        // Get the worker's personality
        PersonalityTraits personality = getWorkerPersonality(context);

        // Check if this is a repeated failure
        int failureCount = getPreviousFailureCount(context, failure.getType());

        // Generate the response
        FailureContext fc = FailureContext.builder()
            .failureType(mapToFailureType(failure.getType()))
            .severity(severity)
            .personality(personality)
            .previousFailureCount(failureCount)
            .emotionalState(getCurrentEmotionalState(context))
            .build();

        FailureResponse response = FailureResponseGenerator.generateResponse(fc);

        // Send to player
        context.getPlayer().sendSystemMessage(response.getDialogue());

        // Log learning
        if (!response.getLearningStatement().isEmpty()) {
            context.getMemory().recordLearning(response.getLearningStatement());
        }

        // Execute recovery plan if available
        if (!response.getRecoveryPlan().isEmpty()) {
            scheduleRecovery(context, response.getRecoveryPlan());
        }

        // Check if player needs reassurance
        if (response.needsPlayerReassurance()) {
            scheduleReassurance(context, response);
        }

        return ActionResult.failure(response.getDialogue());
    }
}
```

#### 2. Personality System Integration

```java
// In your Worker/Steve entity class
private PersonalityTraits personality;

public void setPersonality(PersonalityTraits personality) {
    this.personality = personality;
}

public PersonalityTraits getPersonality() {
    return personality;
}

// Load from NBT or config
public void loadPersonalityFromNBT(CompoundTag tag) {
    this.personality = PersonalityTraits.builder()
        .openness(tag.getInt("Openness"))
        .conscientiousness(tag.getInt("Conscientiousness"))
        .extraversion(tag.getInt("Extraversion"))
        .agreeableness(tag.getInt("Agreeableness"))
        .neuroticism(tag.getInt("Neuroticism"))
        .build();
}
```

#### 3. Memory System Integration

```java
// Track failures in SteveMemory for learning
public class SteveMemory {
    private Map<FailureType, Integer> failureCounts = new HashMap<>();
    private List<String> learnings = new ArrayList<>();

    public void recordFailure(FailureType type) {
        failureCounts.put(type, failureCounts.getOrDefault(type, 0) + 1);
    }

    public int getFailureCount(FailureType type) {
        return failureCounts.getOrDefault(type, 0);
    }

    public void recordLearning(String learning) {
        learnings.add(learning);
        // Could use this in future LLM prompts
    }
}
```

### Configuration

Add to your `config/steve-common.toml`:

```toml
[personality]
# Default personality for new workers
default_openness = 50
default_conscientiousness = 50
default_extraversion = 50
default_agreeableness = 50
default_neuroticism = 50

[personality.failure_response]
# Enable personality-driven failure responses
enabled = true

# Should workers learn from failures?
learning_enabled = true

# Should workers maintain dignity after embarrassing failures?
dignity_preservation = true

# Should workers reassure player after failures?
reassurance_enabled = true

# Severity thresholds
[personality.failure_response.severity]
minor = 20      # 0-20: Small hiccups
moderate = 40   # 21-40: Noticeable setbacks
significant = 60 # 41-60: Major failures
critical = 100  # 61-100: Disasters

# Personality variety presets
[personality.presets.perfectionist]
openness = 40
conscientiousness = 95
extraversion = 30
agreeableness = 50
neuroticism = 30

[personality.presets.anxious_worker]
openness = 40
conscientiousness = 60
extraversion = 50
agreeableness = 80
neuroticism = 95

[personality.presets.stoic_builder]
openness = 40
conscientiousness = 60
extraversion = 30
agreeableness = 40
neuroticism = 5

[personality.presets.enthusiastic_helper]
openness = 70
conscientiousness = 50
extraversion = 95
agreeableness = 70
neuroticism = 40

[personality.presets.balanced_worker]
openness = 50
conscientiousness = 50
extraversion = 50
agreeableness = 50
neuroticism = 50
```

### Command Integration

Add commands to set worker personality:

```java
// /steve personality <name> <trait> <value>
public class SetPersonalityCommand {
    public static void execute(ServerPlayer player, String workerName,
                               String trait, int value) {
        SteveWorker worker = getWorker(workerName);
        PersonalityTraits current = worker.getPersonality();
        PersonalityTraits updated = adjustTrait(current, trait, value);
        worker.setPersonality(updated);

        player.sendSystemMessage(Component.literal(
            "Set " + workerName + "'s " + trait + " to " + value
        ));
    }
}

// /steve personality preset <name> <preset>
public class SetPersonalityPresetCommand {
    public static void execute(ServerPlayer player, String workerName,
                               String presetName) {
        SteveWorker worker = getWorker(workerName);
        PersonalityTraits preset = loadPreset(presetName);
        worker.setPersonality(preset);

        player.sendSystemMessage(Component.literal(
            "Applied " + presetName + " personality to " + workerName
        ));
    }
}
```

### Example Usage Flow

1. **Spawn a worker with a personality**
```java
/steve spawn Builder
/steve personality preset Builder perfectionist
```

2. **Worker encounters a failure**
```java
// Worker breaks tool from improper use
// ActionExecutionException thrown
```

3. **System generates response**
```java
// Perfectionist personality responds:
"I failed to follow proper tool usage procedures. This is unacceptable.
I've identified where I went wrong and will implement corrective measures.
I'll craft a replacement tool and be more careful with durability monitoring."
```

4. **Worker learns and improves**
```java
// Learning recorded in memory
// Future LLM prompts include this learning
// Next time, worker checks durability before use
```

### Testing

Run the demo to see all personalities:

```bash
./gradlew run
# In game, use the command system or run the demo class
```

### Benefits

1. **Character Depth**: Workers feel like individuals, not robots
2. **Emotional Engagement**: Players form connections through failures
3. **Trust Building**: Apologies and responsibility-taking build trust
4. **Memorable Moments**: Failure responses create memorable stories
5. **Player Investment**: Players care more about workers who struggle and grow

## Sources

- [Attribution Theory Research](http://changingminds.org/explanations/theories/attribution_theory.htm)
- [Growth Mindset Research](https://www.jianshu.com/p/9a94d59ba93c)
- [Professional Apology Patterns](https://www.meipian.cn/56rcpuji)
- [Game Design NPC Feedback](https://www.gameres.com/820735.html)
- [Handling Embarrassing Moments](https://k.sina.cn/article_7879776328_1d5abd848068019ulk.html)
