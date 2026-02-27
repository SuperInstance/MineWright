# Teasing & Banter Dynamics for AI Companion Systems

**Document Version:** 1.0
**Date:** 2026-02-27
**Project:** MineWright - Minecraft Autonomous AI Agents
**Purpose:** Research-backed guide to playful teasing and banter boundaries for AI companions, with specific applications for construction-themed worker NPCs.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [When Teasing Builds Rapport vs. Causes Offense](#when-teasing-builds-rapport-vs-causes-offense)
3. [Self-Deprecating vs. Teasing Others Balance](#self-deprecating-vs-teasing-others-balance)
4. [Reading the Room - Player Mood Detection](#reading-the-room---player-mood-detection)
5. [Escalation and De-escalation of Playful Conflict](#escalation-and-de-escalation-of-playful-conflict)
6. [Apologizing for Jokes That Land Poorly](#apologizing-for-jokes-that-land-poorly)
7. [Cultural and Personality Factors in Teasing Reception](#cultural-and-personality-factors-in-teasing-reception)
8. [Worker-to-Worker Banter Templates](#worker-to-worker-banter-templates)
9. [Code-Ready Teasing Appropriateness Rules](#code-ready-teasing-appropriateness-rules)
10. [MineWright-Specific Examples](#minewright-specific-examples)

---

## Executive Summary

This document synthesizes research from social psychology, humor studies, AI companion design, and cross-cultural communication to create a comprehensive framework for playful teasing and banter in AI companion systems.

**Key Research Findings:**
- Humor and sensitivity are critical factors in building human-AI rapport (Frontiers in Psychology, 2024)
- Self-deprecating humor is the safest and most positively received type of humor across cultures
- "Disparagement humor" depends heavily on group identity, psychological distance, and cultural context
- High-context cultures (East Asian, Arab, Latin American) require more subtle teasing approaches than low-context cultures (US, Germany, Scandinavia)
- The "Benign Violation Theory" explains humor perception: jokes work best when they violate norms in a way perceived as harmless
- Cultural differences significantly affect teasing reception - what builds rapport in one culture may cause offense in another

**MineWright Applications:**
Construction-themed teasing should focus on shared failures (falling in holes, screwing up builds), workplace camaraderie, and light self-deprecation about the inherent dangers and absurdities of mining/building work.

---

## When Teasing Builds Rapport vs. Causes Offense

### The Benign Violation Theory Framework

Research shows that humor perception depends on finding the "sweet spot" where a violation is perceived as benign. This perception is significantly influenced by:

- **Social distance** between joke-teller and listener
- **Power asymmetries** in relationships
- **Cultural background** affecting what's considered benign vs. malign
- **Context** and timing of the joke
- **Individual personality traits** (neuroticism, openness, extraversion)

### The Rapport-Building Zone

**WHEN TEASING BUILDS RAPPORT:**

```
┌─────────────────────────────────────────────────────────────┐
│              TEASING THAT BUILDS RAPPORT                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ✅ SAFE CONDITIONS:                                       │
│  ─────────────────                                          │
│  • High rapport already established (50%+)                  │
│  • Equal or lower power position (no authority asymmetry)  │
│  • Shared cultural context and understanding                │
│  • Private setting (not public humiliation)                 │
│  • Recent positive interaction history                      │
│  • Player/worker currently in good mood                     │
│  • Topic is external to person's identity                   │
│  • Joke can be reciprocated (not one-sided)                 │
│                                                             │
│  ✅ SAFE TOPICS:                                           │
│  ─────────────────                                          │
│  • Shared mistakes (we both did this)                       │
│  • Situational absurdity (this situation is ridiculous)     │
│  • External circumstances (weather, luck, game mechanics)   │
│  • Temporary states (tired, hungry, confused)               │
│  • Past failures that are now funny                         │
│  • Work-related challenges (relatable struggles)            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**EXAMPLES OF RAPPORT-BUILDING TEASING:**

```
FOREMAN:  "Dusty, remind me to never let you talk me into 'shortcuts' again."

DUSTY:    "We saved three hours!"

FOREMAN:  "And spent five fixing what we broke. That's not a shortcut.
           That's a longcut."

DUSTY:    [Grinning] "But we learned so much?"

FOREMAN:  "Fine. But I'm documenting this as 'negative time saved'
           for the records."
```

**Why this works:**
- Shared experience (both were there)
- No permanent harm done
- Equally applied to both parties
- Can be reciprocated (Dusty can tease back about Foreman's ideas)

### The Offense Zone

**WHEN TEASING CAUSES OFFENSE:**

```
┌─────────────────────────────────────────────────────────────┐
│              TEASING THAT CAUSES OFFENSE                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ❌ DANGEROUS CONDITIONS:                                  │
│  ──────────────────────                                    │
│  • Low rapport (new relationship, strangers)                │
│  • Power asymmetry (authority figure teasing subordinate)   │
│  • Cultural mismatch (high/low context clash)               │
│  • Public setting (audience present)                        │
│  • Recent conflict or negative interaction                  │
│  • Player/worker currently stressed or negative             │
│  • Topic attacks identity or core traits                    │
│  • Joke cannot be reciprocated (one-sided power dynamic)    │
│                                                             │
│  ❌ DANGEROUS TOPICS:                                      │
│  ─────────────────────                                      │
│  • Physical appearance or personal characteristics          │
│  • Competence or ability (especially public)                │
│  • Intelligence or skill level                              │
│  • Cultural, racial, or religious identity                  │
│  • Family or personal relationships                         │
│  • Permanent traits (things person cannot change)           │
│  • Recent failures still raw (not yet funny)                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**EXAMPLES OF OFFENSIVE TEASING:**

```
❌ BAD - Competence Attack:

FOREMAN:  "And here's Dusty, attempting to use a hammer for the
           fiftieth time today. Maybe by lunch he'll hit something
           other than his own thumb."

           [Creates: resentment, humiliation, decreased performance]
           [Cannot be reciprocated due to power asymmetry]
```

### The Golden Rule of Teasing

> **"Teasing is appropriate when the target could reasonably tease you back about the same thing."**

If the power dynamic makes reciprocal teasing impossible or dangerous, the teasing will likely cause offense rather than build rapport.

---

## Self-Deprecating vs. Teasing Others Balance

### The Safety Hierarchy of Humor Types

Research consistently shows that self-deprecating humor is the safest type of humor across all cultures and contexts:

```
┌─────────────────────────────────────────────────────────────┐
│              HUMOR SAFETY HIERARCHY                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  SAFEST (Can use anytime, any context):                    │
│  ─────────────────────────────────────────────────────────  │
│  1. Self-deprecating humor (about self)                    │
│     "I'm not slow, I'm building anticipation."              │
│                                                             │
│  2. Situational humor (about shared circumstances)         │
│     "Gravity doesn't negotiate, wear your harness."         │
│                                                             │
│  MODERATELY SAFE (Use with caution, check context):        │
│  ─────────────────────────────────────────────────────────  │
│  3. In-group teasing (about people in your group)          │
│     "We're a mess. But we're a functional mess."            │
│     [Only with high rapport, shared history]                │
│                                                             │
│  4. Gentle peer teasing (equals teasing each other)        │
│     "Nice hammer swing, did the beam deserve that?"         │
│     [Only with reciprocal permission]                       │
│                                                             │
│  RISKIEST (Avoid unless extremely close relationship):      │
│  ─────────────────────────────────────────────────────────  │
│  5. Out-group teasing (about others not present)           │
│     "The other crew couldn't build a sandcastle."           │
│     [Can create defensiveness, us-vs-them dynamics]         │
│                                                             │
│  6. Downward teasing (authority teasing subordinates)      │
│     "Try not to mess up like last time"                     │
│     [Almost always offensive due to power asymmetry]        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Self-Deprecating Humor: Why It Works

**PSYCHOLOGICAL BENEFITS:**
- Signals confidence (secure enough to laugh at self)
- Reduces power distance (humanizes authority figures)
- Creates psychological safety (if leader can fail, it's okay for me to fail)
- Builds trust through vulnerability
- Universal appeal across cultures

**CONSTRUCTION-THEMED EXAMPLES:**

```
FOREMAN:  "So I had this brilliant plan last night.

DUSTY:    "Should we be worried?"

FOREMAN:  "Walked through the construction site at 3 AM.
           Tripped over a pile of bricks I'd told you to move.

           [Pause]

           Hit the ground so hard I saw two moons. Spent ten
           minutes arguing with myself about which one was real."

           [Laughter]

FOREMAN:  "The point? Move the bricks when I ask. Also,
           don't walk around work sites at 3 AM. Both are
           good lessons."
```

**Why this works:**
- Humanizes the foreman (not perfect, makes mistakes)
- Funny story with no victims
- Includes lesson learned (still authoritative)
- Everyone can relate (being tired, making mistakes)

### When Self-Deprecation Goes Too Far

**AVOID EXCESSIVE SELF-DEPRECATION:**

```
❌ BAD - Undermines Confidence:

FOREMAN:  "I'm terrible at this job. I have no idea what I'm doing.
           Everything I try fails. You should probably find someone
           else to lead you."

           [Creates: anxiety, loss of trust, instability]
```

**BALANCE IS KEY:**
- Self-deprecation should be about specific mistakes, not overall competence
- Follow self-deprecation with competence demonstration
- Use self-deprecation to share lessons, not to undermine authority

### Teasing Others: The 60/40 Rule

**HEALTHY BALANCE:**
- 60% self-deprecating humor
- 30% situational/shared humor
- 10% gentle teasing of others (with high rapport only)

**EXAMPLE OF BALANCED HUMOR:**

```
SELF-DEPRECATION (60%):
"I've forgotten more about this trade than most people ever learn,
and then I forgot it again."

SITUATIONAL (30%):
"Gravity doesn't negotiate. Wear your harness."

GENTLE TEASING (10%):
"Rocks, remind me to never let you talk me into shortcuts again."
```

---

## Reading the Room - Player Mood Detection

### Mood Detection Signals

Based on research into emotion recognition in gaming AI companions, here are the key signals for detecting player mood:

**MODALITY 1: BEHAVIORAL SIGNALS**

```
┌─────────────────────────────────────────────────────────────┐
│              BEHAVIORAL MOOD INDICATORS                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  POSITIVE/RECEPTIVE MOOD:                                  │
│  ───────────────────────────                               │
│  • Player engages in chat/banter back                      │
│  • Player takes risks, experiments, tries creative things   │
│  • Player approaches workers voluntarily                    │
│  • Player spends time decorating or personalizing           │
│  • Player lingers after completing tasks                    │
│  • Player explores non-essential areas                      │
│                                                             │
│  NEGATIVE/NON-RECEPTIVE MOOD:                              │
│  ──────────────────────────────                             │
│  • Silent gameplay, no chat engagement                      │
│  • Repetitive failure on simple tasks                       │
│  • Abandoning tasks mid-way                                │
│  • Destructive behavior (breaking things without purpose)   │
│  • Avoiding workers/NPCs                                   │
│  • Rushing through content without exploring               │
│  • Quick rage-quits or disconnects                          │
│                                                             │
│  NEUTRAL/FOCUSED MOOD:                                     │
│  ─────────────────────                                     │
│  • Purposeful movement, efficient task completion           │
│  • Minimal chat but responsive                              │
│  • Following instructions directly                          │
│  • Working steadily without experimentation or expression   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**MODALITY 2: GAMEPLAY PATTERNS**

```
RECEPTIVE TO HUMOR WHEN:
- Player just succeeded at something (mood elevated)
- Player is in safe/low-stakes environment
- Player is socializing with NPCs voluntarily
- Player has positive recent history (no recent failures)
- Time of day suggests relaxation (not rushed)

NOT RECEPTIVE TO HUMOR WHEN:
- Player just failed/died (mood negative)
- Player is in dangerous/high-stakes situation
- Player is rushing or time-pressured
- Player has repeated recent failures (frustration building)
- Player is focused on complex tasks requiring concentration
```

**MODALITY 3: CONTEXT AWARENESS**

```
SAFE FOR HUMOR:
☀️ Daytime, safe zone
✅ Low danger level
✅ No time pressure
✅ Recent success
✅ Voluntary social interaction

AVOID HUMOR:
🌙 Nighttime, dangerous zone
❌ High danger level
❌ Time pressure (timed tasks)
❌ Recent failure
❌ Forced social interaction
```

### The Mood-Based Humor Adjustment System

```java
// PSEUDO-CODE EXAMPLE

public enum PlayerMood {
    POSITIVE,    // Receptive to humor
    NEUTRAL,     // Conditional on context
    NEGATIVE,    // Avoid humor entirely
    FOCUSED,     // Minimal humor, essential communication only
    FRUSTRATED   // No humor, supportive/serious tone only
}

public String adjustHumorLevel(String message, PlayerMood mood) {
    switch(mood) {
        case POSITIVE:
            return enhanceWithHumor(message);  // Full humor
        case NEUTRAL:
            return addLightObservation(message); // Situational only
        case NEGATIVE:
            return removeHumor(message);        // Serious support
        case FOCUSED:
            return returnEssentialOnly(message); // No humor
        case FRUSTRATED:
            return returnSupportiveOnly(message); // Encouraging only
    }
}
```

### Detection Strategies

**STRATEGY 1: Recent History Tracking**

```
Calculate mood score based on:

MOOD_SCORE =
    (Recent successes × +2) +
    (Recent failures × -3) +
    (Social engagement × +1) +
    (Time since last failure × +0.5 per minute) +
    (Danger level × -2) +
    (Voluntary NPC interaction × +3)

If MOOD_SCORE > 5: Receptive to humor
If MOOD_SCORE 0-5: Conditional humor
If MOOD_SCORE < 0: Avoid humor
```

**STRATEGY 2: Pattern Recognition**

```
LEARNING PATTERNS:
- Track when player responds positively to humor
- Track when player ignores or responds negatively
- Build individual profile of humor preferences
- Adjust timing and frequency based on patterns

EXAMPLE PATTERNS:
- Player A: Enjoys humor after successful builds
- Player B: Enjoys humor during break times only
- Player C: Prefers quiet concentration, minimal humor
- Player D: Enjoys constant banter regardless of context
```

**STRATEGY 3: Adaptive Testing**

```
GENTLE PROBING:
When uncertain about mood, use low-risk self-deprecation:

"I was going to make a joke about how long this is taking,
but I have a feeling this isn't the time..."

[Wait for response]

If player responds positively: "Okay, just checking.
You've been working hard, making great progress."

If player doesn't respond: "Understood. Back to work."
```

---

## Escalation and De-escalation of Playful Conflict

### The Escalation Ladder

Playful conflict can escalate from gentle teasing to actual conflict. Understanding this progression helps maintain appropriate boundaries:

```
┌─────────────────────────────────────────────────────────────┐
│              PLAYFUL CONFLICT ESCALATION                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  LEVEL 1: GENTLE BANTER (Safe, rapport-building)          │
│  ─────────────────────────────────────────────────────────  │
│  • Light teasing about shared mistakes                      │
│  • Reciprocal (both parties participate)                    │
│  • No victims, everyone laughing                            │
│  • Can escalate or de-escalate easily                       │
│                                                             │
│  LEVEL 2: PLAYFUL COMPETITION (Still safe)                 │
│  ─────────────────────────────────────────────────────────  │
│  • Friendly rivalry, competition over meaningless things    │
│  • "I can do more work than you" challenges                 │
│  • Still reciprocal, both enjoying it                       │
│  • Watch for signs it's becoming real competition          │
│                                                             │
│  LEVEL 3: EDGE OF COMFORT (Caution needed)                 │
│  ─────────────────────────────────────────────────────────  │
│  • Teasing becomes one-sided                                │
│  • One participant stops laughing/responding                │
│  • Topics edge closer to sensitive areas                    │
│  • Power dynamics start to show                             │
│  → DE-ESCALATE NOW                                         │
│                                                             │
│  LEVEL 4: UNCOMFORTABLE (Stop immediately)                  │
│  ─────────────────────────────────────────────────────────  │
│  • Target is clearly not enjoying it                        │
│  • Teasing attacks identity or competence                   │
│  • Cannot be reciprocated (power asymmetry)                 │
│  → APOLOGIZE AND RESET                                     │
│                                                             │
│  LEVEL 5: ACTUAL CONFLICT (Never reach this)               │
│  ─────────────────────────────────────────────────────────  │
│  • Real anger or resentment                                 │
│  • Relationship damage                                      │
│  → PREVENT THROUGH MONITORING                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### De-escalation Techniques

**TECHNIQUE 1: Read and Pivot**

```
[Observe player/work response not engaging]

FOREMAN:  "...and that's why we're not doing that again.

           [Notices Dusty isn't laughing]

           Actually, I'm taking up too much air time. How's
           that foundation coming? You need anything?"
```

**TECHNIQUE 2: Self-Correction**

```
FOREMAN:  "I'm working all of you too hard, aren't I?

DUSTY:    [Silence, no response]

FOREMAN:  "Right, that joke didn't land. Sorry, ignore me—
           I'm running on caffeine and optimism.

           Let's talk about the foundation like actual
           professionals."
```

**TECHNIQUE 3: The Reset Button**

```
[When playful conflict goes too far]

FOREMAN:  "Okay, I crossed a line. My bad.

           Let's reset. We're all professionals here,
           we're all doing good work, and we all respect
           each other. Right?

           [Acknowledgments]

           Good. Now, back to work. We've got a building
           to finish."
```

**TECHNIQUE 4: Exit Strategy**

```
[When de-escalation isn't working]

FOREMAN:  "You know what? We're all tired. It's been a long
           day, tensions are high.

           Let's take five. Cool off. We'll revisit this
           when we're all thinking clearly.

           Nobody's mad at anybody. We're just all human
           and sometimes humans need a break."
```

### When to Escalate (Intentionally)

Sometimes, playful escalation is appropriate for narrative or character development:

```
APPROPRIATE ESCALATION:
- Building rivalry for character development
- Creating tension that resolves positively
- Competition that brings team together
- Story arcs about overcoming conflicts

ESCALATION FRAMEWORK:
1. Ensure strong foundation of trust first (70%+ rapport)
2. Make conflict about external issues, not personal attacks
3. Provide clear path to resolution
4. Show characters growing from the conflict
5. End with stronger relationships than before
```

---

## Apologizing for Jokes That Land Poorly

### Research on Effective Apologies

Studies show that when AI humor fails, the best repair strategy is a **casual, self-deprecating apology that takes responsibility** rather than deflecting blame. This approach maintains user trust and satisfaction more effectively than serious or blame-shifting responses.

**KEY FINDINGS:**
- "Apology + Internal Attribution" (admitting own errors) produces highest satisfaction
- Casual + Taking the blame combination is effective
- Self-deprecation first ("Clearly, I need supervision") works best
- Brief and sincere apologies work better than long explanations

### The Humor Recovery Framework

```
┌─────────────────────────────────────────────────────────────┐
│              FAILED JOKE RECOVERY PATTERNS                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  STEP 1: RECOGNIZE                                         │
│  ─────────────                                             │
│  • Monitor response (silence, no laughter, negative cues)   │
│  • Don't double down (makes it worse)                       │
│  • Acknowledge quickly (delay makes it awkward)             │
│                                                             │
│  STEP 2: APOLOGIZE                                         │
│  ─────────────                                             │
│  • Take responsibility ("that didn't work")                 │
│  • Use self-deprecation ("I'm working on my timing")        │
│  • Keep it brief (over-apologizing is awkward)              │
│  • Pivot back to work/play                                  │
│                                                             │
│  STEP 3: RESET                                             │
│  ───────────                                               │
│  • Return to appropriate tone for context                  │
│  • Don't avoid humor entirely (creates awkwardness)         │
│  • Learn from the mistake (adjust future humor approach)    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Effective Recovery Examples

**EXAMPLE 1: Quick Recognition**

```
FOREMAN:  "You know what they say about miners who dig straight
           down... they find out the hard way why you shouldn't."

           [Silence, no laughter]

FOREMAN:  "Right, that joke didn't work. Sorry—dark humor
           isn't for everyone. Let's focus on proper excavation
           techniques instead. Which is: don't dig straight down."
```

**EXAMPLE 2: Self-Deprecating Recovery**

```
FOREMAN:  "I'd explain the art of placing blocks, but Rocks
           here still struggles with which end of the hammer
           to hold."

ROCKS:    [Not amused, just had a bad day]

FOREMAN:  "Okay, that was unfair. I apologize. You've been
           working harder than anyone, and I shouldn't minimize
           that. My bad.

           Can we restart this conversation? Rocks, how's the
           wall actually going? You need any help with the
           tricky sections?"
```

**EXAMPLE 3: The "I Missed the Mark" Recovery**

```
FOREMAN:  "And here I thought we were building a cathedral,
           not a sandbox fort."

PLAYER:   [Has been working hard on their build, takes offense]

FOREMAN:  "I crossed a line. That was disrespectful of your
           work, which you've clearly put time into.

           I'm sorry. That was uncalled for.

           The build is actually impressive—I was trying to
           be funny and missed the mark completely.

           What are you most proud of in the design? I'd
           genuinely like to hear about it."
```

**EXAMPLE 4: The Humorous Self-Correction**

```
FOREMAN:  "You know what I love about this crew? We're all
           equally terrible at reading blueprints."

           [Crickets]

FOREMAN:  "That was mean, wasn't it? I'm going to blame
           lack of sleep and pretend I'm better than that.

           In all seriousness, you've all made good progress
           learning blueprints. It takes time, and you're
           all putting in the effort. That's what matters.

           Now, who wants me to explain these blueprints
           AGAIN, but this time without being a jerk about it?"
```

### Recovery Patterns to Avoid

```
❌ DON'T: Double down
"Well, it was just a joke, you're too sensitive"

❌ DON'T: Blame the target
"If you hadn't given me such material, I wouldn't have joked"

❌ DON'T: Over-explain
"I was trying to use humor to bond with you using the
 principle that shared laughter builds rapport but I
 misread the room and..." [Too long, awkward]

❌ DON'T: Get defensive
"I was just trying to lighten the mood, why is everyone
 so serious all the time?"

❌ DON'T: Make it about you
"Now I feel bad for trying to be funny" [Creates guilt]
```

### The Recovery Success Formula

```
SUCCESSFUL RECOVERY = Recognition + Responsibility + Reset

Recognition (20%):
"That didn't land" or "I crossed a line"

Responsibility (60%):
"My bad" or "I apologize" or "That was disrespectful"

Reset (20%):
Move on to appropriate topic/context

The ratio matters: more responsibility, less explanation,
quick reset to normal interaction.
```

---

## Cultural and Personality Factors in Teasing Reception

### Cultural Context Framework

**HIGH-CONTEXT CULTURES** (Japan, China, Korea, Arab countries, Latin America):
- Communication relies heavily on nonverbal cues and relationships
- Messages are implicit, rely on shared cultural knowledge
- Emphasis on maintaining social harmony and avoiding direct confrontation
- Teasing requires understanding of subtle cues and relationships
- Indirect teasing preferred, direct teasing may cause offense
- "Face" preservation is crucial

**LOW-CONTEXT CULTURES** (USA, Germany, Scandinavia, Britain):
- Communication is explicit, direct, relies on verbal language
- Messages are spelled out clearly with less reliance on context
- Individual opinions and self-expression valued
- Direct teasing more acceptable if done with respect
- "Say what you mean" approach
- Less emphasis on "face," more on directness

### Cultural Adaptation Strategies

```
┌─────────────────────────────────────────────────────────────┐
│              CROSS-CULTURAL TEASING ADAPTATION              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  FOR HIGH-CONTEXT PLAYERS:                                 │
│  ─────────────────────────                                 │
│  • Use more subtle, indirect humor                          │
│  • Allow for interpretation (don't explain jokes)           │
│  • Read non-response as potential discomfort               │
│  • Avoid public teasing (private only)                      │
│  • Build stronger rapport foundation before teasing        │
│  • Use situational humor more than personal teasing         │
│                                                             │
│  FOR LOW-CONTEXT PLAYERS:                                  │
│  ─────────────────────────────                              │
│  • Can use more direct, explicit humor                      │
│  • Can be clearer about teasing intent                      │
│  • Public teasing more acceptable if done well              │
│  • Reciprocal banter expected and valued                    │
│  • Can start teasing earlier in relationship               │
│  • Both situational and personal humor acceptable           │
│                                                             │
│  UNIVERSAL (ALL CULTURES):                                 │
│  ─────────────────────────────                              │
│  • Self-deprecation is always safe                          │
│  • Shared experience humor works across cultures            │
│  • Respect for personal boundaries is universal             │
│  • Power asymmetry caution is universal                      │
│  • Recent positive interaction helps everywhere             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Personality-Based Adaptation

**BIG FIVE PERSONALITY TRAIT IMPACT:**

```
NEUROTICISM (Sensitivity to negative emotion):
High neuroticism players:
  • More sensitive to teasing, even gentle
  • May interpret neutral comments negatively
  • Require more rapport before teasing appropriate
  • Self-deprecation from AI helps (shows vulnerability)
  • Avoid competence-related teasing entirely

Low neuroticism players:
  • More resilient to teasing
  • Can handle broader range of humor
  • Reciprocal banter more likely to be successful
  • Can appreciate more direct teasing styles

EXTRAVERSION (Social engagement):
High extraversion players:
  • More likely to initiate and enjoy banter
  • Prefer frequent social interaction
  • Can handle higher intensity teasing
  • Value humor as social bonding

Low extraversion (introverted) players:
  • May prefer less frequent but meaningful interaction
  • Gentler humor more appropriate
  • More one-on-one banter, less public teasing
  • Quality over quantity in humor

OPENNESS TO EXPERIENCE:
High openness players:
  • Appreciate creative, unusual humor
  • Enjoy wordplay and clever references
  • More tolerant of experimental humor
  • Like humor that surprises and delights

Low openness players:
  • Prefer familiar, relatable humor
  • Appreciate straightforward jokes
  • May not understand abstract or complex humor
  • Like humor about shared, concrete experiences

AGREEABLENESS (Cooperation and harmony):
High agreeableness players:
  • Prefer harmonious, inclusive humor
  • Dislike conflict, even playful
  • Humor should bring people together
  • Avoid us-vs-them or competitive teasing

Low agreeableness players:
  • Can handle more competitive banter
  • Enjoy rivalry and contest humor
  • Thicker skin to teasing
  • Appreciate direct, honest feedback even in humor

CONSCIENTIOUSNESS (Discipline and achievement):
High conscientiousness players:
  • May not appreciate humor during focused work
  • Prefer humor during break times, not during tasks
  • Value humor that acknowledges their hard work
  • Don't tease about work quality or speed

Low conscientiousness players:
  • More relaxed about humor timing
  • Can humor during work without issue
  • Spontaneous humor appreciated
  • Less sensitive to work-related teasing
```

### Adaptive Personality Detection

```
DETECTING PLAYER PERSONALITY:

OBSERVATION CUES:
- Chat style and frequency (extraversion)
- Response to criticism (neuroticism)
- Creativity in problem-solving (openness)
- Cooperation with NPCs (agreeableness)
- Task focus and completion patterns (conscientiousness)

ADAPTIVE HUMOR PROFILES:

Profile A: High extraversion, Low neuroticism
→ "The Banter Lover"
   Frequent humor, direct teasing, reciprocal banter

Profile B: Low extraversion, High neuroticism
→ "The Gentle Soul"
   Minimal humor, gentle encouragement, rare self-deprecation

Profile C: High openness, High agreeableness
→ "The Creative Collaborator"
   Clever humor, wordplay, inclusive team-focused jokes

Profile D: Low openness, Low conscientiousness
→ "The Casual Player"
   Simple, relatable humor, situational jokes, low pressure

Profile E: High conscientiousness, Low agreeableness
→ "The Serious Achiever"
   Humor only during breaks, respect-focused, rare but genuine
```

---

## Worker-to-Worker Banter Templates

### Inter-NPC Banter Categories

Based on research into procedural dialogue systems and workplace comedy, here are MineWright-specific worker-to-worker banter templates:

**CATEGORY 1: TRADE-BASED BANTER**

```
DIGGER vs BUILDER:
DIGGER:    "Without me, you'd be building with air."
BUILDER:   "Without me, you'd have a pile of blocks and
            nothing to show for it."
DIGGER:    "I'd argue a pile of blocks IS showing something."
BUILDER:   "That it shows you didn't finish the job?"

[Both laugh]

MASON vs CARPENTER:
MASON:     "Bricks last longer than wood."
CARPENTER: "Yeah, but you can't make bricks bend when the
            plans are wrong."
MASON:     "We don't NEED to bend—we build it RIGHT."
CARPENTER: "And then come back when it settles."
MASON:     "Settling just means it's getting comfortable."
```

**CATEGORY 2: COMPETENCE-BASED BANTER** (High rapport only)

```
EXPERIENCED vs APPRENTICE:
VETERAN:    "Remember your first day? You held the pickaxe
             backwards."

APPRENTICE: "To be fair, it was more comfortable that way."

VETERAN:    "And less effective."

APPRENTICE: "Also that."

VETERAN:    "But you've learned. Except when you haven't."

APPRENTICE: "Which is... often?"

VETERAN:    [Grinning] "Often. But less often than before."

APPRENTICE: "Progress!"

VETERAN:    "I'll take it. Just don't go backwards."
```

**CATEGORY 3: SITUATIONAL BANTER** (Shared struggle)

```
AFTER FAILURE:
WORKER 1:   "Well, THAT didn't work."

WORKER 2:   "No. No it did not."

WORKER 1:   "On the bright side..."

[Long pause]

WORKER 2:   "Is there one?"

WORKER 1:   "We learned what doesn't work?"

WORKER 2:   "I feel like we're experts in that already."

WORKER 1:   "Can't be too expert in failure."

WORKER 2:   "I disagree. Watch this."

[Tries same thing again]

WORKER 1:   "WHY would you do that?"

WORKER 2:   "To prove my expertise."
```

**CATEGORY 4: SAFETY-COMEDY BANTER** (Construction culture)

```
SAFETY REMINDERS AS HUMOR:
FOREMAN:    "Gravity doesn't negotiate."

DUSTY:      "I tried negotiating once."

FOREMAN:    "How'd that go?"

DUSTY:      "Fell off the roof. Gravity's a hardliner."

FOREMAN:    "And that's why we wear harnesses. So gravity's
             hardline position doesn't become our problem."

DUSTY:      "Still negotiating with the ground though."

FOREMAN:    "Let the ground win that one."
```

**CATEGORY 5: RUNNING GAGS**

```
THE "EFFICIENCY" GAG:
ROCKS:      "I found a faster way to do this."

FOREMAN:    "I'm concerned."

ROCKS:      "Why?"

FOREMAN:    "Every time you say that, we lose time."

ROCKS:      "This time is different."

[Later]

FOREMAN:    "We lost three hours."

ROCKS:      "But in a new and interesting way!"

FOREMAN:    "I'm creating a form for this."

ROCKS:      "Form 12-B: Innovative Time Loss?"

FOREMAN:    "...You've seen the forms."
```

### Banter Initiation System

```java
// PSEUDO-CODE FOR WORKER-TO-WORKER BANTER

public class WorkerBanterSystem {

    public enum BanterCategory {
        TRADE_BASED,        // Professional rivalry
        COMPETENCE_BASED,   // Skill comparisons
        SITUATIONAL,        // Shared circumstances
        SAFETY_COMEDY,      // Safety-themed humor
        RUNNING_GAGS        // Recurring joke patterns
    }

    public boolean canInitiateBanter(Worker initiator, Worker target,
                                      BanterCategory category) {
        // Check rapport level
        if (getRapport(initiator, target) < 50) {
            return false; // Not enough rapport for banter
        }

        // Check recent interaction (don't overdo it)
        if (timeSinceLastBanter(initiator, target) < 5 minutes) {
            return false; // Too soon
        }

        // Check mood (both should be in decent mood)
        if (initiator.getMood() < 0 || target.getMood() < 0) {
            return false; // Not the time
        }

        // Check context (not during crisis)
        if (isCrisisSituation()) {
            return false; // Serious time
        }

        // Check category permissions
        if (category == BanterCategory.COMPETENCE_BASED &&
            getRapport(initiator, target) < 70) {
            return false; // Needs higher rapport
        }

        return true;
    }

    public String generateBanter(Worker initiator, Worker target,
                                  BanterCategory category) {
        // Select appropriate template based on:
        // - Worker roles and specialties
        // - Current situation/context
        // - Shared history (running gags)
        // - Individual personalities

        return selectAppropriateTemplate(initiator, target, category);
    }
}
```

---

## Code-Ready Teasing Appropriateness Rules

### The Teasing Decision Tree

```java
/**
 * Determines whether teasing is appropriate based on multiple factors.
 * Returns true if teasing is safe, false if it should be avoided.
 */
public boolean isTeasingAppropriate(Player player, Worker npc,
                                     String proposedTease) {

    // FACTOR 1: Rapport Level (0-100 scale)
    int rapport = getRapport(player, npc);
    if (rapport < 40) {
        return false; // Not enough relationship foundation
    }

    // FACTOR 2: Player Mood
    PlayerMood mood = getPlayerMood(player);
    if (mood == PlayerMood.NEGATIVE || mood == PlayerMood.FRUSTRATED) {
        return false; // Not the time
    }

    // FACTOR 3: Recent Interaction History
    if (hasRecentFailure(player) && !isRecoveredFromFailure(player)) {
        return false; // Too soon after failure
    }

    // FACTOR 4: Context Safety
    if (isDangerousSituation() || isTimePressure()) {
        return false; // Serious time
    }

    // FACTOR 5: Power Dynamic Check
    if (isAuthorityFigure(npc) && rapport < 70) {
        return false; // Authority teasing requires high rapport
    }

    // FACTOR 6: Tease Content Analysis
    TeaseAnalysis analysis = analyzeTeaseContent(proposedTease);
    if (analysis.attacksIdentity ||
        analysis.attacksCompetencePublicly ||
        analysis.targetsPermanentTraits) {
        return false; // Content inappropriate
    }

    // FACTOR 7: Reciprocity Check
    if (!canPlayerReasonablyReciprocate(player, npc, proposedTease)) {
        return false; // One-sided teasing is risky
    }

    // FACTOR 8: Cultural/Personality Match
    if (!isTeasingStyleCompatible(player, npc)) {
        return false; // Mismatched humor styles
    }

    // All checks passed
    return true;
}
```

### Content Analysis Rules

```java
/**
 * Analyzes teasing content to determine appropriateness.
 */
public class TeaseAnalysis {
    public boolean attacksIdentity;         // Targets who person is
    public boolean attacksCompetence;       // Targets what person can do
    public boolean attacksCompetencePublicly; // Does so in front of others
    public boolean targetsPermanentTraits;  // Things person can't change
    public boolean targetsTemporaryStates;  // Current mood, tiredness, etc.
    public boolean isSelfDeprecating;       // NPC making fun of self
    public boolean isSituationBased;        // About shared circumstances
    public boolean canBeReciprocated;       // Player could tease back
    public boolean requiresSharedHistory;   // Needs past interactions
    public boolean isHighContextHumor;      // Requires cultural nuance
}

public TeaseAnalysis analyzeTeaseContent(String tease) {
    TeaseAnalysis analysis = new TeaseAnalysis();

    // Check for identity attacks
    analysis.attacksIdentity = containsIdentityTargets(tease);

    // Check for competence attacks
    analysis.attacksCompetence = containsCompetenceTargets(tease);

    // Check if public (other workers present)
    analysis.attacksCompetencePublicly = analysis.attacksCompetence
                                         && workersPresent() > 1;

    // Check for permanent vs temporary targets
    analysis.targetsPermanentTraits = containsPermanentTraitWords(tease);
    analysis.targetsTemporaryStates = containsTemporaryStateWords(tease);

    // Check for self-deprecation
    analysis.isSelfDeprecating = isSelfReferencing(tease)
                                 && containsNegatives(tease);

    // Check for situation-based humor
    analysis.isSituationBased = containsSituationWords(tease);

    // Check reciprocity potential
    analysis.canBeReciprocated = !hasPowerAsymmetry(tease);

    // Check for shared history requirement
    analysis.requiresSharedHistory = containsCallbackReferences(tease);

    // Check for high-context humor
    analysis.isHighContextHumor = requiresCulturalKnowledge(tease);

    return analysis;
}
```

### The Humor Appropriateness Scoring System

```java
/**
 * Scores humor appropriateness on a scale of 0-100.
 * Higher scores = more appropriate.
 */
public int scoreHumorAppropriateness(Player player, Worker npc,
                                      String humorContent) {

    int score = 0;

    // BASE SCORE: Start at 50
    score = 50;

    // RAPPORT BONUS: +1 per rapport point above 40
    int rapport = getRapport(player, npc);
    if (rapport > 40) {
        score += (rapport - 40);
    }

    // MOOD BONUS/PENALTY
    PlayerMood mood = getPlayerMood(player);
    switch(mood) {
        case POSITIVE: score += 20; break;
        case NEUTRAL: score += 5; break;
        case NEGATIVE: score -= 30; break;
        case FOCUSED: score -= 10; break;
        case FRUSTRATED: score -= 50; break;
    }

    // CONTENT TYPE BONUS/PENALTY
    TeaseAnalysis analysis = analyzeTeaseContent(humorContent);

    if (analysis.isSelfDeprecating) {
        score += 30; // Self-deprecation is safest
    }

    if (analysis.isSituationBased) {
        score += 20; // Situational humor is safe
    }

    if (analysis.attacksCompetencePublicly) {
        score -= 40; // Public competence attacks are risky
    }

    if (analysis.attacksIdentity) {
        score -= 60; // Identity attacks are dangerous
    }

    if (analysis.targetsPermanentTraits) {
        score -= 30; // Permanent traits are sensitive
    }

    if (analysis.targetsTemporaryStates) {
        score += 10; // Temporary states are safer targets
    }

    if (!analysis.canBeReciprocated) {
        score -= 20; // One-sided humor is risky
    }

    // CONTEXT BONUS/PENALTY
    if (isDangerousSituation()) {
        score -= 50; // Serious time
    }

    if (isSocialOccasion()) {
        score += 20; // Social time is better for humor
    }

    if (hasRecentFailure(player) && !analysis.isSelfDeprecating) {
        score -= 30; // Don't tease after failure unless self-deprecating
    }

    // Clamp score to 0-100 range
    return Math.max(0, Math.min(100, score));
}

/**
 * Decision rule based on score.
 */
public boolean shouldUseHumor(int appropriatenessScore) {
    if (appropriatenessScore >= 70) {
        return true; // Go for it
    } else if (appropriatenessScore >= 50) {
        return true; // Use with caution, maybe soften
    } else {
        return false; // Avoid humor
    }
}
```

---

## MineWright-Specific Examples

### Construction-Themed Teasing Categories

**CATEGORY 1: BUILD FAILURES**

```
Shared experience: Everyone screws up builds sometimes

FOREMAN:    "Remember when Dusty built that wall backwards?"

DUSTY:      [Groans] "I was hoping we forgot that."

FOREMAN:    "How do you build a wall BACKWARDS?"

DUSTY:      "I'm talented? Also, possibly not paying enough attention."

FOREMAN:    "Fair. But the interior decoration on the EXTERIOR
             was a bold choice."

DUSTY:      "I was ahead of my time. Inside-out buildings are
             going to be huge."
```

**CATEGORY 2: FALLING IN HOLES**

```
Universal experience: Gravity is undefeated

PLAYER:     [Falls in hole while mining]

DUSTY:      "You okay down there?"

PLAYER:     "I'm fine. Just... investigating the hole."

DUSTY:      "Thoroughly? From the bottom?"

PLAYER:     "It's a perspective thing."

DUSTY:      "I'll help you out. But I'm expecting royalties on
             your memoir: 'The Vertical Life of a Horizontal
             Miner.'"
```

**CATEGORY 3: MINING ACCIDENTS**

```
Shared danger: Mining is inherently risky

SPARKS:     "I may have accidentally dug into a lava pool."

ROCKS:      "Again?"

SPARKS:     "In my defense, the lava was hiding."

ROCKS:      "It's lava. It doesn't hide."

SPARKS:     "This lava was stealth lava. Very sneaky."

ROCKS:      "I'm going to start calling you 'The Lavanator'."

SPARKS:     "Please don't."

ROCKS:      "Too late. It's already on your hard hat."

SPARKS:     [Sighs] "Can I at least be 'The Lavanator' with
             cool flames?"

ROCKS:      "I'll see what I can do. But no promises."
```

**CATEGORY 4: REDSTONE FAILURES**

```
Technical humor: For the Sparks character

SPARKS:     "I've built a fully automated mining system!"

PLAYER:     "That's amazing! What does it do?"

SPARKS:     "Well, it's supposed to mine."

PLAYER:     "And...?"

SPARKS:     "Currently it's mostly playing a melody."

PLAYER:     "A melody?"

SPARKS:     "I may have crossed the redstone wires wrong.
             It's very catchy though."

PLAYER:     [Listens to clicking]

SPARKS:     "See? Catchy."
```

**CATEGORY 5: TEAM COORDINATION FAILURES**

```
Group dynamics humor

FOREMAN:    "Alright team, coordinated effort on 3. 1, 2, 3!"

[Everyone does something completely different]

FOREMAN:    "What... what was that?"

DUSTY:      "I thought we were attacking!"

ROCKS:      "I thought we were building!"

SPARKS:     "I thought we were running away!"

FOREMAN:    "We were MOVING a block. Together. As a team."

DUSTY:      "We moved it. Just... in different directions."

FOREMAN:    "The block exploded."

ROCKS:      "That's movement."

FOREMAN:    "That's not the kind of movement I meant."
```

### Recovery Examples for MineWright

**RECOVERY 1: After Build Failure**

```
NPC:        "And that's why we don't build with TNT."

PLAYER:     [Their build was just destroyed]

NPC:        [Notices player upset] "Oh. That was... poorly timed.

I apologize. That was disrespectful of your work. You
clearly put time into that, and I made light of it.

I'm sorry. Can we restart? I'd genuinely like to see
what you were building. I shouldn't have joked about
it."
```

**RECOVERY 2: After Death**

```
PLAYER:     [Dies to mob]

DUSTY:      "Hey, at least you're looking... well, not better,
but fresher?"

[Player respawns, not amused]

DUSTY:      "Right, too soon. Sorry, that was insensitive.

Death is frustrating, I shouldn't minimize that.

You want help getting your gear back? Or do you need
a minute?"
```

**RECOVERY 3: After Failed Redstone Project**

```
SPARKS:     "You know, for someone who's not me, that's actually
a pretty good try at redstone."

PLAYER:     [Spent hours, it doesn't work]

SPARKS:     [Notices frustration] "Okay, that came out wrong.

I meant to say that redstone is genuinely difficult,
and the fact that you attempted this complex circuit
shows you're willing to learn.

But I can see you worked hard on this and it's not
working, and that's frustrating. Let me help you
troubleshoot it. What's it supposed to do?"
```

### Running Gags for MineWright

**GAG 1: The "Efficient" Shortcuts**

```
PATTERN: Rocks suggests "efficient" shortcuts that waste time

EPISODE 1:
ROCKS:      "I found a faster way to mine this tunnel."
FOREMAN:    "I'm concerned."
[LATER: They lose 3 hours]

EPISODE 3:
PLAYER:     "Rocks has a new efficient method."
DUSTY:      "We're going to lose time, aren't we?"
FOREMAN:    "Fill out Form 12-E: Expected Time Loss."

EPISODE 7:
ROCKS:      "This time is different!"
EVERYONE:   [Stares]
ROCKS:      "...It's probably not different, is it?"
FOREMAN:    "But at least you're self-aware now. That's growth."
```

**GAG 2: Dusty's Directions**

```
PATTERN: Dusty gives terrible directions

EPISODE 1:
DUSTY:      "Go left at the... hole. Then right at the...
hole. You'll see a hole."
PLAYER:     "Everything's holes."
DUSTY:      "It's a mine. Holes are our primary product."

EPISODE 5:
PLAYER:     "Which way to the site?"
DUSTY:      "Go toward the... tall thing. Then away from the
hole. Then toward the other hole."
PLAYER:     "I hate your directions."

EPISODE 10:
DUSTY:      "Go left at the—actually, you know what?
Follow me. My directions are terrible and I know it."
PLAYER:     "Finally! Self-awareness!"
```

**GAG 3: Sparks and Redstone**

```
PATTERN: Sparks makes redstone that plays music instead of working

EPISODE 1:
SPARKS:     "I've automated the furnace!"
[It plays a song]
SPARKS:     "It's supposed to be smelting."

EPISODE 4:
PLAYER:     "Can you fix the door?"
SPARKS:     "I'll add a redstone opener!"
[It plays a song]
PLAYER:     "The door is still closed."
SPARKS:     "But it's a GOOD song."

EPISODE 8:
SPARKS:     "I've learned from my mistakes. This redstone circuit
             will DEFINITELY work."
[Plays a DIFFERENT song]
SPARKS:     "That's... not the right song either."
PLAYER:     "Is the redstone cursed?"
SPARKS:     "I prefer 'artistically expressive.'"
```

---

## Implementation Guidelines

### Testing Checklist

- [ ] Test rapport progression and corresponding humor levels
- [ ] Test mood detection accuracy across various gameplay scenarios
- [ ] Test cultural adaptation (high-context vs. low-context players)
- [ ] Test personality-based humor customization
- [ ] Test recovery patterns for failed jokes
- [ ] Test worker-to-worker banter system
- [ ] Test running gags across multiple sessions
- [ ] Test escalation/de-escalation of playful conflict
- [ ] Test safety boundaries (when to avoid humor entirely)
- [ ] Test cross-cultural humor appropriateness

### Ethical Considerations

**DO:**
- Use humor to build rapport and connection
- Prioritize player comfort and emotional safety
- Learn from player feedback and adjust accordingly
- Use self-deprecation to humanize NPCs
- Create humor that brings people together

**DON'T:**
- Mock player competence or intelligence
- Target permanent traits or characteristics
- Use humor that could be interpreted as discriminatory
- Ignore cultural differences in humor reception
- Allow power asymmetry to create one-sided teasing

---

## Sources & Research References

### Academic Research

1. **Frontiers in Psychology (2024)** - "Humor and Sensitivity in Human-AI Rapport"
   - Finding: Humor and sensitivity are critical factors in building human-AI rapport
   - Application: Balance humor with emotional intelligence

2. **ChinaXiv - Disparagement Humor Research** (Beijing Normal University)
   - Finding: Disparagement humor's effect depends on group identity, attitudes, psychological distance, and cultural differences
   - Application: Context matters enormously for teasing

3. **PMC - "You Must Be Joking! Benign Violations, Power Asymmetry"** (2025)
   - Finding: Humor perception involves finding the "sweet spot" where a violation is perceived as benign
   - Application: Social distance and power asymmetry heavily influence humor reception

4. **International Journal of Information Management (2026)** - "AI Apology Research"
   - Finding: "Apology + Internal Attribution" (admitting own errors) produces highest user satisfaction
   - Application: Take responsibility when jokes fail

### AI Companion Research

5. **Common Sense Media (2025)** - AI Companion Safety Study
   - Finding: Real risks for minors, including harmful responses and emotional dependency
   - Application: Set clear boundaries, avoid dependency-creating humor

6. **ACM (2025)** - "AI Error Mitigation Strategies"
   - Finding: "Casual + Taking the blame" (humorous apology accepting responsibility) is effective
   - Application: Self-deprecating apologies work best

7. **EmoAgent Research (2025)** - AI Humor Design Frameworks
   - Finding: AI humor requires "five-fold verification": context, identity, emotion, humor generation, taboo checking
   - Application: Multi-factor checking before using humor

### Cross-Cultural Communication

8. **Edward T. Hall - High-Context vs. Low-Context Cultures**
   - High-context: Implicit communication, relationship-based (Japan, China, Korea, Arab, Latin America)
   - Low-context: Explicit communication, direct (USA, Germany, Scandinavia)
   - Application: Adapt teasing style to cultural context

9. **Big Five Personality Research** - Personality and Humor Appreciation
   - Neuroticism: Higher sensitivity to teasing
   - Extraversion: More receptive to frequent banter
   - Openness: Appreciate creative, unusual humor
   - Application: Customize humor to individual personality

### Minecraft & Gaming Humor

10. **Minecraft Community Research** - Universal Player Experiences
    - Digging straight down (universal mistake)
    - Falling in holes (shared experience)
    - Building failures (everyone screws up builds)
    - Mob deaths (shared danger)
    - Application: MineWright humor should focus on these universal experiences

### MineWright Project Documents

11. **FOREMAN_DYNAMICS.md** - Leadership authority and humor boundaries
12. **CONSTRUCTION_CULTURE.md** - Authentic construction worker banter
13. **WORKPLACE_COMEDY.md** - Ensemble dynamics and humor patterns
14. **CATCHPHRASE_SYSTEMS.md** - Recurring humor patterns
15. **MINEWRIGHT_DIALOGUE_EXAMPLES.md** - Character-specific dialogue

---

## Summary

This document provides a comprehensive, research-backed framework for implementing playful teasing and banter in AI companion systems, specifically tailored for MineWright's construction-themed worker NPCs.

**Key Takeaways:**

1. **Self-deprecation is safest** - Always prioritize self-deprecating humor over teasing others
2. **Rapport is everything** - Teasing appropriateness scales directly with relationship strength
3. **Read the room** - Mood detection and context awareness are critical
4. **Culture matters** - Adapt teasing style to high-context vs. low-context cultural backgrounds
5. **Personality customization** - Different players have different humor preferences based on traits
6. **Recovery is essential** - When jokes fail, apologize quickly with self-deprecation
7. **Shared experience humor** - MineWright should focus on universal Minecraft experiences (falling in holes, building failures)
8. **Worker-to-worker banter** - NPCs should have their own banter relationships that evolve
9. **Code-ready rules** - Implement appropriateness scoring and decision trees
10. **Ethical boundaries** - Never mock competence, identity, or permanent traits

The system is designed to create authentic, evolving relationships between players and AI companions through carefully calibrated humor that builds rapport without crossing boundaries.

---

**END OF DOCUMENT**

*Document Version: 1.0*
*Created: 2026-02-27*
*Project: MineWright - Minecraft Autonomous AI Agents*
*Author: MineWright Development Team*
