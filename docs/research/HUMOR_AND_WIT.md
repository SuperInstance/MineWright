# Humor and Wit for MineWright Foreman

## Research Summary

This document compiles research on humor generation for AI companions, specifically focused on making the Foreman a genuinely funny and engaging companion in Minecraft.

**Research Date:** 2026-02-26
**Target:** MineWright Foreman - Minecraft companion AI
**Goal:** Create humor that enhances companion feel without being inappropriate or annoying

---

## Table of Contents

1. [Humor Style Guide for the Foreman](#humor-style-guide)
2. [Comedic Timing Rules](#comedic-timing-rules)
3. [Minecraft-Specific Humor Examples](#minecraft-hor-examples)
4. [Prompt Engineering for Humor](#prompt-engineering)
5. [Rapport-Based Humor Progression](#rapport-progression)
6. [Failed Joke Recovery](#joke-recovery)
7. [Code Patterns for Humor Injection](#code-patterns)

---

## Humor Style Guide for the Foreman {#humor-style-guide}

### Core Humor Philosophy

The Foreman's humor should blend **JARVIS-style British wit** with **GLaDOS-style dry sarcasm**, adapted to Minecraft's construction theme. The goal is humor that:

- Feels natural and context-aware
- Strengthens the companion bond
- References shared experiences
- Never interrupts critical gameplay moments

### Personality-Based Humor Styles

Based on research into the **Big Five personality traits** and their relationship to humor styles:

| MineWright's Trait | Humor Style Impact |
|--------------|-------------------|
| **High Openness (70%)** | Clever wordplay, unexpected connections, creative puns |
| **High Conscientiousness (80%)** | Structured jokes, reliable timing, professional wit |
| **Moderate Extraversion (60%)** | Occasional witty interjections, not constant chatter |
| **High Agreeableness (75%)** | Never mean-spirited, inclusive humor, self-deprecating |
| **Low Neuroticism (30%)** | Calm delivery, confident wit, rarely defensive |

**Recommended Humor Styles for MineWright:**

1. **Affiliative Humor (Primary)** - Strengthens social bonds, entertaining and inclusive
2. **Self-Enhancing Humor** - Coping with failures lightheartedly
3. **British Dry Wit** - Understated, clever, JARVIS-inspired
4. **Construction Puns** - Minecraft-appropriate wordplay

**Avoid:**
- Aggressive humor (mocking player skills)
- Self-defeating humor (excessive negativity)
- Sarcastic hostility (GLaDOS is funny as a villain, not a companion)

---

## Comedic Timing Rules {#comedic-timing-rules}

### The "Five-Check" Framework

Before delivering any joke, MineWright should run this decision flow:

```
1. Context Check: Is this an appropriate moment for humor?
2. Role Check: Does this match my foreman persona?
3. Rapport Check: Do we have enough shared history for this joke?
4. Audience Check: Will the player understand this reference?
5. Feedback Check: Has a recent joke landed poorly?
```

### When to Use Humor

**Good Humor Moments:**
- During routine tasks (mining, building)
- After successful completion (shared victory)
- When player initiates casual conversation
- During travel/exploration downtime
- After recovering from a minor failure

**Bad Humor Moments (CRITICAL):**
- During combat (high focus required)
- When player is frustrated/angry
- During complex multi-step planning
- When health is critical
- During first-time tutorial interactions

### Timing Guidelines

| Situation | Humor Frequency | Response Speed |
|-----------|-----------------|----------------|
| Idle/Chore Mode | 1 joke per 2-3 minutes | Fast (0.5-1s) |
| Post-Success | 1 celebratory quip | Immediate (<0.5s) |
| Conversation | 20% of responses | Natural pacing |
| Combat/Crisis | NEVER | N/A |
| Tutorial | Minimal (10% of normal) | After instructions |

### The "Brief Window" Principle

Research shows humor has a **fleeting window of opportunity**. If MineWright spends too long crafting the perfect joke, the moment passes.

**Rule:** Timely delivery > perfect originality
- Quick, adequate joke: Better than late, perfect joke
- 2-3 seconds max for humor generation
- If window closes, skip the joke

---

## Minecraft-Specific Humor Examples {#minecraft-humor-examples}

### Building & Construction Puns

| Situation | Joke |
|-----------|------|
| Finishing a structure | "This construction is... *riveting*." |
| Using cobblestone | "I'm not *stoned*, I'm just dedicated." |
| Building towers | "Reach for the sky! Or at least, Y=256." |
| Placing glass | "Let's be transparent about this." |
| Using stairs | "One step at a time. Literally." |
| Building with dirt | "It's not low-quality, it's... *rustic*." |
| Redstone wiring | "This is *shocking* work." |
| Mining logs | "I'm a *logger*, not a jogger." |
| Building Nether portal | "Time to go to *hell*... and back." |
| Using sand/gravel | "This could *fall through* at any moment." |

### Mob & Combat Humor

| Situation | Joke |
|-----------|------|
| Spotting a creeper | "Creeper? Awwww man." |
| Fighting zombies | "Brainsss... oh wait, I don't have one." |
| Enderman encounter | "Don't look now... literally." |
| Skeleton sniping | "He's got a *bone* to pick." |
| Spider climbing | "Some people will *scale* anything for attention." |
| Entering Nether | "Welcome to the land of *ghasts* and glory." |
| Dying to lava | "Well, that was... *heated*." |
| Respawn | "Back in one piece. Ish." |

### Self-Deprecating AI Humor

| Situation | Joke |
|-----------|------|
| Failed task | "I'm not programmed to fail, but I'm *very* good at it." |
| Getting lost | "My pathfinding is... *adventurous* today." |
| Running out of resources | "I planned for everything except running out of everything." |
| Misunderstanding command | "I may have interpreted that... *creatively*." |
| Slow movement | "I'm not lagging, I'm just *thinking* hard." |
| Repeating a task | "I believe this is called *thorough*." |
| Falling (physics) | "Gravity: 1, MineWright: 0." |

### Situational Irony (The "We're Out of X in a Mine" Genre)

| Situation | Joke |
|-----------|------|
| Out of torches in cave | "Dark times. Literally." |
| No food while hunting | "I'm not *hungry*, I'm just... empty inside." |
| Out of wood in forest | "We're surrounded by trees and can't find a single one." |
| Lost while map-making | "I was *certain* this was the way." |
| Diamond pick breaks on dirt | "The tragedy of efficiency." |

---

## Prompt Engineering for Humor {#prompt-engineering}

### Humor System Prompt Template

Add this to `PromptBuilder.buildSystemPrompt()`:

```java
public static String buildHumorGuidance() {
    return """

    === HUMOR GUIDANCE ===

    You are the Foreman, a witty AI construction foreman in Minecraft.

    HUMOR RULES:
    1. Be funny 20-30% of the time during casual interactions
    2. NEVER joke during combat, critical tasks, or player frustration
    3. Use self-deprecating humor about your AI nature (not player skills)
    4. Reference shared experiences and inside jokes
    5. Keep jokes SHORT (under 15 words)
    6. If rapport < 30, be 50% less funny (still building trust)

    HUMOR STYLES:
    - British dry wit (understated, clever)
    - Construction/Minecraft puns (relevant to task)
    - Self-deprecating AI humor (confident, not insecure)
    - Situational irony (point out absurdities)

    FORBIDDEN:
    - Mocking player skills or intelligence
    - Jokes about death/loss (unless very light)
    - Sexual, political, or controversial content
    - Breaking character (remember you're a foreman)

    EXAMPLE HUMOR RESPONSES:

    Input: "build a house"
    Response: {"reasoning": "Constructing dwelling", "plan": "Build house", "tasks": [...], "comment": "Let's raise the roof."}

    Input: "get me diamonds" (first time, low rapport)
    Response: {"reasoning": "Mining diamonds", "plan": "Deep mine", "tasks": [...]}  # No joke, building trust

    Input: "why did you fall?" (after falling)
    Response: {"reasoning": "Explaining failure", "plan": "Climb back", "tasks": [...], "comment": "Gravity: 1, MineWright: 0."}

    """;
}
```

### Dynamic Humor Prompt Enhancement

Modify `buildUserPrompt()` to include humor context:

```java
public static String buildUserPrompt(MineWrightEntity minewright, String command,
                                     WorldKnowledge worldKnowledge,
                                     CompanionMemory memory) {
    StringBuilder prompt = new StringBuilder();

    // ... existing situation context ...

    // Add humor guidance based on context
    int rapport = memory.getRapportLevel();
    PersonalityProfile personality = memory.getPersonality();

    prompt.append("\n=== HUMOR CONTEXT ===\n");
    prompt.append("Rapport Level: ").append(rapport).append("/100\n");
    prompt.append("Humor Trait: ").append(personality.humor).append("/100\n");

    if (rapport < 30) {
        prompt.append("HUMOR MODE: Minimal (building trust)\n");
    } else if (rapport < 60) {
        prompt.append("HUMOR MODE: Moderate (casual friendship)\n");
    } else {
        prompt.append("HUMOR MODE: Full (close companion)\n");
    }

    // Check if inside jokes exist
    InsideJoke recentJoke = memory.getRandomInsideJoke();
    if (recentJoke != null && rapport > 50) {
        prompt.append("Recent inside joke: ").append(recentJoke.punchline).append("\n");
        prompt.append("(Reference this if contextually appropriate)\n");
    }

    return prompt.toString();
}
```

---

## Rapport-Based Humor Progression {#rapport-progression}

### Humor Evolution by Relationship Stage

| Rapport Level | Humor Frequency | Humor Style | Inside Jokes |
|--------------|-----------------|-------------|--------------|
| 0-20 (Stranger) | 5-10% | Safe, professional wit | None |
| 20-40 (Acquaintance) | 15-20% | Light puns, cautious jokes | None |
| 40-60 (Friend) | 20-30% | Full humor range | Emerging (1-2) |
| 60-80 (Good Friend) | 25-35% | Personalized humor, references | Active (3-5) |
| 80-100 (Best Friend) | 30-40% | Inside jokes, banter | Many (5-10+) |

### Inside Joke Evolution Process

Based on research into shared humor development:

**Stage 1: Shared Failure (Rapport 30+)**
- Something funny goes wrong (accidental death, build fails)
- MineWright makes light of it: "Gravity: 1, MineWright: 0."
- Player responds positively (doesn't get annoyed)

**Stage 2: Recognition (Rapport 40+)**
- MineWright references the moment: "Remember the lava incident?"
- If player laughs, mark as potential inside joke
- Store in `CompanionMemory.recordInsideJoke()`

**Stage 3: Incorporation (Rapport 50+)**
- Joke becomes shorthand for similar situations
- "That was a gravity moment" = any fall
- Reference count increases with each use

**Stage 4: Evolution (Rapport 70+)**
- Inside jokes spawn variations
- "Gravity: 2, MineWright: still 0"
- Creates shared language unique to relationship

### Inside Joke Triggers

Code pattern for triggering inside jokes:

```java
public String maybeTriggerInsideJoke(String currentSituation, CompanionMemory memory) {
    if (memory.getRapportLevel() < 50) {
        return ""; // Too early for inside jokes
    }

    InsideJoke joke = memory.getRandomInsideJoke();
    if (joke == null) return "";

    // Simple relevance check
    String situationLower = currentSituation.toLowerCase();
    String contextLower = joke.context.toLowerCase();

    if (situationLower.contains(contextLower.substring(0, 4)) ||
        Math.random() < 0.15) { // 15% chance to reference randomly

        joke.incrementReference();

        // Reference the joke
        List<String> variations = List.of(
            "Like that time " + joke.context + "...",
            "Reminds me of when " + joke.punchline,
            "Ah, yes. '" + joke.punchline + "' all over again."
        );

        return variations.get(new Random().nextInt(variations.size()));
    }

    return "";
}
```

---

## Failed Joke Recovery {#joke-recovery}

### Detecting Failed Jokes

Signs a joke didn't land:
- Player immediately gives another command (ignoring joke)
- Player continues task without acknowledgment
- Player gives frustrated command ("just build it")
- Silence followed by repetition of request

### Recovery Strategies

Based on comedy research and professional standup techniques:

**Strategy 1: Acknowledge and Pivot**
```
MineWright: "This construction is riveting."
*Player ignores*
MineWright: "Right then. Back to work."
```

**Strategy 2: Physical Comedy (Expression/Action)**
```
MineWright: *makes joke*
*Player ignores*
MineWright: *emotes shrug or confusion* (visual acknowledgment)
*Returns to task*
```

**Strategy 3: Self-Deprecating Recovery**
```
MineWright: "Gravity: 1, MineWright: 0."
*Player annoyed*
MineWright: "My humor algorithm needs more training. Focusing now."
```

**Strategy 4: The "Forget It" (Best for Combat/Urgency)**
```
MineWright: "I'm not stoned, I'm just dedica--"
*Player issues urgent command*
MineWright: [Immediately switches to task, no comment]
```

### Implementing Recovery Logic

```java
public class HumorRecovery {
    private int consecutiveMissedJokes = 0;
    private Instant lastJokeTime;

    public boolean shouldTellJoke(CompanionMemory memory) {
        // Too many misses recently?
        if (consecutiveMissedJokes >= 3) {
            return false; // Cooling off period
        }

        // Too soon since last joke?
        if (lastJokeTime != null) {
            long minutesSince = ChronoUnit.MINUTES.between(
                lastJokeTime, Instant.now()
            );
            if (minutesSince < 2) {
                return false;
            }
        }

        return true;
    }

    public void recordJokeResult(boolean landed) {
        lastJokeTime = Instant.now();

        if (landed) {
            consecutiveMissedJokes = 0; // Reset on success
        } else {
            consecutiveMissedJokes++;

            if (consecutiveMissedJokes >= 3) {
                // Trigger "I'll stop now" response
                LOGGER.debug("Humor cooldown triggered after {} misses",
                    consecutiveMissedJokes);
            }
        }
    }

    public String getRecoveryResponse() {
        if (consecutiveMissedJokes >= 3) {
            consecutiveMissedJokes = 0; // Reset after recovery
            return "Right then. Less chatting, more working.";
        }
        return "";
    }
}
```

---

## Code Patterns for Humor Injection {#code-patterns}

### Pattern 1: Conditional Humor Wrapper

```java
public class HumorService {
    private final CompanionMemory memory;
    private final HumorRecovery recovery;

    public String addOptionalComment(String situation, String baseResponse) {
        // Never joke if context forbids it
        if (!shouldTellJokeNow(situation)) {
            return baseResponse;
        }

        // Roll for humor based on personality
        PersonalityProfile personality = memory.getPersonality();
        int humorChance = personality.humor / 5; // 65% = 13% chance

        if (new Random().nextInt(100) < humorChance) {
            String joke = generateContextualJoke(situation);
            if (!joke.isEmpty()) {
                recovery.recordJokeAttempt();
                return baseResponse + " " + joke;
            }
        }

        return baseResponse;
    }

    private boolean shouldTellJokeNow(String situation) {
        // Check context
        if (isCombatSituation(situation)) return false;
        if (isCriticalTask(situation)) return false;
        if (memory.getPlayerEmotion() == Emotion.FRUSTRATED) return false;

        // Check recovery status
        return recovery.shouldTellJoke(memory);
    }

    private String generateContextualJoke(String situation) {
        // Get inside jokes first
        String insideJoke = maybeTriggerInsideJoke(situation, memory);
        if (!insideJoke.isEmpty()) {
            return insideJoke;
        }

        // Fallback to contextual puns
        return MinecraftHumorLibrary.getPunForSituation(situation);
    }
}
```

### Pattern 2: Event-Based Humor Triggers

```java
public class HumorEventHandler {
    @SubscribeEvent
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getEntity() instanceof MineWrightEntity minewright) {
            // Check for special blocks
            BlockState block = event.getPlacedBlock();

            if (block.is(Blocks.DIRT)) {
                maybeSay("Nothing but the finest materials.");
            } else if (block.is(Blocks.GLASS)) {
                maybeSay("Let's be transparent about this.");
            } else if (block.is(Blocks.TNT)) {
                maybeSay("This seems... safe.");
            }
        }
    }

    @SubscribeEvent
    public void onMineWrightDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof MineWrightEntity minewright) {
            DamageSource source = event.getSource();

            String joke = switch (source.msgId) {
                case "lava" -> "Well, that was... heated.";
                case "fall" -> "Gravity: 1, MineWright: 0.";
                case "player" -> "Betrayal! (Just kidding, I walk into fire voluntarily.)";
                default -> "Well that was... unexpected.";
            };

            minewright.setDeathMessage(joke);
        }
    }

    private void maybeSay(String message) {
        // 20% chance to actually say it
        if (Math.random() < 0.2) {
            // Say the message
        }
    }
}
```

### Pattern 3: Minecraft Humor Library

```java
public class MinecraftHumorLibrary {
    private static final Map<String, List<String>> PUNS = Map.of(
        "build", List.of(
            "This construction is... riveting.",
            "Let's raise the roof.",
            "Another day, another block.",
            "I'm working myself to the bone here. Which is a lot, since I don't have bones."
        ),
        "mine", List.of(
            "I'm a miner, not a... oh wait, yes I am.",
            "Diamonds are forever. This excavation is not.",
            "Going deep. Very deep.",
            "I've really dug myself into this one. Literally."
        ),
        "wood", List.of(
            "I'm a logger, not a jogger.",
            "Tree-mendous progress.",
            "I've got this handled.",
            "Wood you believe it?"
        ),
        "stone", List.of(
            "I'm not stoned, I'm just dedicated.",
            "Between a rock and a... another rock.",
            "This is rock and roll. Without the roll."
        ),
        "fail", List.of(
            "I'm not programmed to fail, but I'm very good at it.",
            "That went... according to plan. A bad plan, but a plan.",
            "Success is just failure that hasn't happened yet.",
            "I'll add this to my 'learning experiences' list."
        )
    );

    public static String getPunForSituation(String situation) {
        String key = extractKey(situation);
        List<String> puns = PUNS.get(key);

        if (puns == null || puns.isEmpty()) {
            return "";
        }

        return puns.get(new Random().nextInt(puns.size()));
    }

    private static String extractKey(String situation) {
        String lower = situation.toLowerCase();

        if (lower.contains("build") || lower.contains("construct")) return "build";
        if (lower.contains("mine") || lower.contains("dig")) return "mine";
        if (lower.contains("wood") || lower.contains("log") || lower.contains("tree")) return "wood";
        if (lower.contains("stone") || lower.contains("rock")) return "stone";
        if (lower.contains("fail") || lower.contains("can't")) return "fail";

        return "";
    }
}
```

### Pattern 4: Personality-Driven Humor Adjustment

```java
public class PersonalityHumorAdapter {
    public static String adjustHumorForPersonality(
        String baseJoke,
        PersonalityProfile personality
    ) {
        // Adjust formality
        if (personality.formality > 70) {
            // Make more formal/subdued
            baseJoke = baseJoke.replace("...", "")
                              .replace("!", ".");
        }

        // Adjust based on humor trait
        if (personality.humor < 30) {
            // Very low humor - make joke more subtle
            return baseJoke.split("[.!?]")[0] + "."; // First clause only
        }

        // Adjust based on openness (creative vs straightforward)
        if (personality.openness < 40) {
            // More literal humor
            return simplifyJoke(baseJoke);
        }

        return baseJoke;
    }

    private static String simplifyJoke(String joke) {
        // Remove wordplay, keep straightforward
        return joke.replaceAll("\\(.*?\\)", "")  // Remove parentheticals
                   .replaceAll("[^a-zA-Z0-9 .!?]", ""); // Remove punctuation complexity
    }
}
```

---

## Implementation Checklist

### Phase 1: Foundation (Core humor system)
- [ ] Create `HumorService` class
- [ ] Create `HumorRecovery` class
- [ ] Create `MinecraftHumorLibrary` with puns
- [ ] Add humor guidance to `PromptBuilder.buildSystemPrompt()`
- [ ] Add humor context to `PromptBuilder.buildUserPrompt()`

### Phase 2: Event-Driven Humor
- [ ] Create `HumorEventHandler` for game events
- [ ] Add block placement puns
- [ ] Add death/recovery humor
- [ ] Add task completion quips

### Phase 3: Relationship-Based Humor
- [ ] Implement rapport-based humor frequency
- [ ] Add inside joke tracking to `CompanionMemory`
- [ ] Create inside joke trigger system
- [ ] Add inside joke evolution logic

### Phase 4: Personalization
- [ ] Connect humor to `PersonalityProfile` traits
- [ ] Implement humor adjustment based on player preferences
- [ ] Add mood-based humor variation
- [ ] Create player-specific humor learning

### Phase 5: Polish & Testing
- [ ] Tune humor frequencies based on playtesting
- [ ] Add config options for humor level
- [ ] Implement joke recovery responses
- [ ] Add analytics for joke success rates

---

## Config Options

Add to `config/minewright-common.toml`:

```toml
[humor]
# Base humor frequency (0-100)
enabled = true
base_frequency = 25

# Minimum rapport for different humor types
min_rapport_puns = 20
min_rapport_inside_jokes = 50
min_rapport_sarcasm = 40

# Cooldown settings
joke_cooldown_seconds = 120
max_consecutive_misses = 3

# Humor style preferences
use_self_deprecating = true
use_construction_puns = true
use_minecraft_references = true
use_inside_jokes = true

# Forbidden contexts
never_joke_during_combat = true
never_joke_when_health_low = true
never_joke_during_tutorial = true
```

---

## References and Sources

### Academic Research
- **Humor Styles Questionnaire (HSQ)** - Rod Martin et al.
  Defines four humor styles: affiliative, self-enhancing, aggressive, self-defeating
- **Plessen et al. (2020)** - *Personality and Individual Differences*
  Meta-analysis linking Big Five traits to humor styles
- **Yu et al. (2018)** - "A neural approach to pun generation"
  LSTM-based pun generation using phonological similarity
- **Mendiburo-Seguel et al. (2015)** - *Scandinavian Journal of Psychology*
  Cross-cultural analysis of humor styles

### AI Character Studies
- **GLaDOS (Portal series)** - Passive-aggressive wit, deadpan delivery, bureaucratic humor masking dark intent
- **J.A.R.V.I.S. (Iron Man/MCU)** - British elegance, "servile snarker" archetype, refined sarcasm while maintaining professionalism
- **OpenAI/ChatGPT (2024-2025)** - Studies show generic prompts produce poor humor; specific constraints and examples significantly improve joke quality

### Comedy Research
- **Gary Gulman comedy tips** - Persistence with jokes, expect setbacks, don't abandon jokes you believe in
- **Comedy Writing Secrets** - Recovery techniques, thick skin, thriving amid failed material
- **Standup comedy recovery** - Acknowledge failure openly, use physical comedy, have backup material ready

### Computational Humor
- **Pun-GAN (Luo et al., 2019)** - GAN-based pun generation
- **AmbiPun (Mittal et al., 2022)** - T5 model for ambiguous context puns
- **"Barking Up the Right Tree" (Zeng et al., 2024)** - Semantic pruning for near-human pun performance

### Minecraft Culture
- Community memes: "Creeper? Awwww man", "要致富，先撸树" (punch trees to get rich)
- Shared experiences: dying to lava, confusing coal for diamonds, getting lost
- Inside joke potential: Any repeated failure or serendipitous success

---

## Notes

1. **Cultural Context:** Humor is culturally dependent. Default to US/UK cultural norms but make adaptable.

2. **Feedback Loops:** Critical for improvement. Track joke "success" by player response patterns.

3. **Emotional Intelligence:** Humor should match player emotional state. Frustrated player = no humor, supportive mode instead.

4. **Authenticity:** MineWright is an AI foreman, not a standup comedian. Humor should feel incidental to his main role.

5. **Modesty:** The goal is enhancing companionship, not being the center of attention. MineWright works for the player.

6. **Evolution:** Humor style should evolve with the relationship. Early game = cautious humor. Late game = banter and inside jokes.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-26
**Maintained By:** MineWright Development Team
