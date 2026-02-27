# Failure Recovery Dialogue - Quick Reference

**Version:** 1.0.0
**Last Updated:** 2025-02-27

## Quick Start

```java
// Create a personality
PersonalityTraits personality = PersonalityTraits.builder()
    .openness(70)
    .conscientiousness(50)
    .extraversion(80)
    .agreeableness(60)
    .neuroticism(40)
    .build();

// Create failure context
FailureContext context = FailureContext.builder()
    .failureType(FailureType.ITEM_LOSS)
    .severity(65)
    .personality(personality)
    .previousFailureCount(2)
    .build();

// Generate response
FailureResponse response = FailureResponseGenerator.generateResponse(context);
System.out.println(response.getDialogue());
```

## Severity Levels

| Level | Range | Name | Examples |
|-------|-------|------|----------|
| 1 | 0-20 | Minor Hiccup | Dropped item, small detour |
| 2 | 21-40 | Moderate Setback | Broke tool, inefficient path |
| 3 | 41-60 | Significant Failure | Lost valuable items, failed quest |
| 4 | 61-100 | Critical Disaster | Destroyed build, lost irreplaceable items |

## Failure Types

- `RESOURCE_WASTE` - Wasted materials
- `TOOL_BREAKAGE` - Broke from improper use
- `NAVIGATION_ERROR` - Got lost
- `STRUCTURAL_FAILURE` - Build collapsed
- `ITEM_LOSS` - Lost important items
- `TASK_FAILURE` - Failed objective
- `COMMUNICATION_ERROR` - Misunderstood instructions
- `SAFETY_VIOLATION` - Danger to self/others
- `REPETITIVE_MISTAKE` - Repeated previous failure
- `EMBARRASSING_MOMENT` - Public failure

## Personality Quick Guide

### High Conscientiousness (80-100)
- Takes full responsibility immediately
- Creates detailed prevention plans
- May be overly self-critical
- Response: Procedural, methodical

### High Neuroticism (80-100)
- Strong emotional reaction
- Catastrophizes situations
- Seeks reassurance
- Response: Anxious, apologetic

### High Extraversion (80-100)
- Vocal about failures
- Uses humor to defuse
- Energetic about recovery
- Response: Expressive, enthusiastic

### High Agreeableness (80-100)
- Deeply concerned about player feelings
- Over-accommodating
- Relationship-focused
- Response: Empathetic, remorseful

### High Openness (80-100)
- Frames failure as learning
- Experimental attitude
- Intellectualizes
- Response: Curious, analytical

## Key Methods

| Method | Purpose |
|--------|---------|
| `generateResponse(context)` | Main dialogue + learning + recovery |
| `generateHelpRequest(context)` | Ask for assistance gracefully |
| `generateEmbarrassmentResponse(context)` | Handle embarrassing moments with dignity |
| `generatePlayerReassurance(context)` | Comfort player after setback |

## Response Structure

Each `FailureResponse` contains:

1. **dialogue** - Main spoken response
2. **severityLevel** - Classified severity
3. **learningStatement** - What was learned
4. **recoveryPlan** - How to fix it
5. **needsPlayerReassurance** - Whether to comfort player

## Examples

### Perfectionist + Item Loss (65)
```
"I failed to maintain proper inventory security. This error is entirely
my fault. I've updated my protocols to require automatic deposit at
50% durability. I'll retrieve replacement materials. It will take
approximately 15 minutes. I apologize for the inefficiency."
```

### Worrier + Item Loss (65)
```
"Oh no, oh no, I lost it! I'm so sorry! I know how valuable that was!
You must be so angry with me! I feel terrible! I promise I didn't mean
to! Please don't hate me! I'll be so careful from now on, I swear!"
```

### Enthusiastic + Item Loss (65)
```
"WOW, that was a big mistake! I really failed at losing important items!
I feel terrible about letting you down! But I'm NOT giving up! I've got
a plan, I've got motivation, and I've got the best player ever! I'm
going to fix this and then some! Just you wait!"
```

### Stoic + Item Loss (65)
```
"Significant failure at losing important items. This is concerning.
I'm implementing preventative measures. I'll be more careful going
forward. I'll regain your trust through improved performance."
```

## Best Practices

1. **Match severity to response** - Don't overreact to minor hiccups
2. **Maintain character consistency** - Personality should drive responses
3. **Show growth over time** - Track learning from failures
4. **Preserve dignity** - Even big failures can be handled gracefully
5. **Reassure the player** - Their frustration matters too
6. **Request help appropriately** - Different personalities ask differently

## Testing

Run the demo to see all personalities in action:

```bash
./gradlew run
# Then select FailureResponseDemo
```

## Configuration

```toml
[character.failure_response]
enabled = true
learning_enabled = true
dignity_preservation = true

[character.failure_response.severity_thresholds]
minor = 20
moderate = 40
significant = 60
critical = 100
```

## Full Documentation

See `FAILURE_RECOVERY_DIALOGUE.md` for complete research, methodology, and examples.
