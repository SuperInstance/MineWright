# Conflict Resolution Dialogue for MineWright Workers

A comprehensive guide to implementing realistic, personality-driven conflict resolution for AI game companions in Minecraft.

## Table of Contents

1. [Overview](#overview)
2. [Research Foundations](#research-foundations)
3. [Conflict Types](#conflict-types)
4. [Personality-Based Conflict Styles](#personality-based-conflict-styles)
5. [De-escalation Techniques](#de-escalation-techniques)
6. [Apology Patterns](#apology-patterns)
7. [Forgiveness Dialogue](#forgiveness-dialogue)
8. [Compromise Language](#compromise-language)
9. [Escalation Triggers to Avoid](#escalation-triggers-to-avoid)
10. [Dialogue Templates](#dialogue-templates)
11. [Java Implementation](#java-implementation)
12. [Memory Integration](#memory-integration)

---

## Overview

Conflict is an essential component of realistic AI companion behavior. Well-implemented conflict resolution:

- **Builds emotional investment** - Players form stronger bonds through overcoming disagreements
- **Demonstrates personality** - How characters handle conflict reveals their true nature
- **Creates memorable moments** - Resolved conflicts become story beats players remember
- **Increases immersion** - Imperfect, disagreeable companions feel more real
- **Enables character growth** - Working through conflicts develops relationships over time

### Key Principles

1. **Conflict should be resolvable** - Every disagreement should have a path to resolution
2. **Personality drives responses** - Same conflict, different reactions based on traits
3. **Escalation is a choice** - Both de-escalation and escalation should be possible
4. **Memory matters** - Past conflicts affect current relationships and dialogue
5. **Growth is possible** - Resolved conflicts should strengthen relationships

---

## Research Foundations

This guide draws from multiple research areas:

### Video Game Companion Systems

**BioWare Games (Mass Effect, Dragon Age, Baldur's Gate 3)**
- Approval/disapproval systems tracking player choices (-100 to +100 range)
- Companions leave permanently at extreme disapproval thresholds (e.g., -50 in BG3)
- Warning systems before critical breakdowns (warnings at -20 and -40)
- Romance and friendship systems with distinct thresholds
- Inter-companion dynamics where player acts as mediator

**Stardew Valley NPC Relationships**
- Long-term progression (year+ to max relationship)
- Gift-giving with diminishing returns
- Heart events unlocking at relationship thresholds
- Interconnected NPC relationships affecting player interactions
- Dynamic dialogue evolving with relationship depth

### Workplace Conflict Psychology

**Thomas-Kilmann Conflict Mode Model**
Five conflict resolution styles:
1. **Competing** - Assertive, uncooperative (pursuing own concerns)
2. **Collaborating** - Assertive, cooperative (win-win solutions)
3. **Compromising** - Moderate on both dimensions (mutual concessions)
4. **Avoiding** - Unassertive, uncooperative (sidestepping conflict)
5. **Accommodating** - Unassertive, cooperative (yielding to others)

**Big Five Personality & Conflict Styles**
Research shows correlations between traits and preferred conflict approaches:

| Personality Trait | Preferred Style | Avoided Style |
|-------------------|-----------------|---------------|
| High Extraversion | Competing | Avoiding, Accommodating |
| High Openness | Collaborating | Competing |
| High Agreeableness | Accommodating | Competing |
| High Conscientiousness | Collaborating, Compromising | Avoiding |
| High Neuroticism | Avoiding, Accommodating | Competing |

### Mediation & De-escalation Techniques

**Core De-escalation Strategies**
1. **Active Listening** - Hearing to understand, not rebut
2. **Reflective Paraphrasing** - Restating to confirm understanding
3. **Emotion Management** - Identifying and managing emotions first
4. **Interest Analysis** - Distinguishing positions from underlying needs
5. **Psychological Safety** - Creating space for honest expression

**Nonviolent Communication Framework**
- Observation without evaluation
- Feeling without judgment
- Need without strategy
- Request without demand

---

## Conflict Types

### 1. Inter-Worker Conflicts

Disagreements between multiple MineWright workers.

#### Resource Competition

**Scenario**: Two workers claim the same resource node.

```java
// Low severity - first come, first served
Worker A: "I was here first - this iron is spoken for."
Worker B: "Fair enough. I'll scout ahead for the next cluster."

// Medium severity - negotiation needed
Worker A: "I need this iron for the tools you asked me to make."
Worker B: "And I need it for the reinforcement beams. Both are important."
Worker A: "What if we split it? You take two-thirds for the beams, I take one-third for tools?"
Worker B: "That won't be enough for either of us... Maybe we should ask the player to prioritize?"

// High severity - entrenched positions
Worker A: "I'm not budging on this. The tools were requested first."
Worker B: "And the structural integrity affects EVERYONE. You're being selfish."
Worker A: "Selfish? I'm trying to be efficient!"
```

#### Territorial Disputes

**Scenario**: Workers disagree on workspace boundaries.

```java
// De-escalating approach
Worker A: "Hey, you're building in my section."
Worker B: "Oh! I didn't realize there were sections. Where should I work?"
Worker A: "I claimed from X=50 to X=75. You can have east of there."
Worker B: "Perfect. Sorry about the confusion!"

// Escalating approach
Worker A: "Get out of my section."
Worker B: "You don't own this area. I'll work where I want."
Worker A: "I marked it clearly. You're being deliberately difficult."
Worker B: "I'm trying to be efficient. You're being territorial."
```

#### Methodological Disagreements

**Scenario**: Workers disagree on how to approach a task.

```java
// Collaborative resolution
Worker A: "We should build the foundation first."
Worker B: "I think we should frame the walls - it'll go faster."
Worker A: "Without a foundation, the walls won't be stable."
Worker B: "That's true... but the foundation takes time. What if we do both?"
Worker A: "Hmm, I could start the foundation while you frame the interior walls?"
Worker B: "Yes! That's actually brilliant. Parallel processing!"
```

### 2. Worker-Player Disagreements

Conflicts between workers and the player's commands.

#### Impossible Commands

**Scenario**: Player asks for something that can't be done.

```java
// High agreeableness - gentle correction
Worker: "I... I want to help, but I don't think I can reach that block.
It's too high for me to safely access. Would you like me to build a
scaffold first?"

// Low agreeableness - direct refusal
Worker: "I can't do that. It's outside my reach range. You'll need
to adjust your plan."

// High neuroticism - anxious response
Worker: "Oh no, I... I don't think I can do that! It's too high!
I might fall! Please don't make me! Is there something else I can do?"

// High openness - problem-solving approach
Worker: "That's an interesting challenge! I can't reach it directly,
but what if I pillar up? Or could we relocate the target? I have
a few ideas!"
```

#### Ethical Objections

**Scenario**: Player commands something the worker objects to.

```java
// High conscientiousness - principled objection
Worker: "I can't follow that command. It would violate safety protocols.
I'm happy to help with a modified approach that meets the same goal
without the risk."

// Moderate traits - negotiable objection
Worker: "I'm not comfortable with that approach. It feels wrong to me.
Could we discuss an alternative? I want to help, just... not that way."

// Low conscientiousness - compliant but uncomfortable
Worker: "I... okay. If that's what you want. But I really don't think
this is a good idea. Please don't blame me if something goes wrong."
```

#### Priority Conflicts

**Scenario**: Worker disagrees with player's prioritization.

```java
// Collaborative approach
Worker: "I understand you want the decoration done, but the structural
cracks really should be addressed first. It's a safety issue. What if
I spend an hour on the cracks, then switch to the decorations?"

// Assertive approach
Worker: "I can't prioritize decorations over structural repairs.
The cracks are getting worse. I'm going to address the safety issue
first, unless you have a compelling reason not to."

// Accommodating approach (with regret)
Worker: "If you say so... I'll focus on the decorations. But I want
to go on record that I think the cracks should be our priority.
I'm worried about them."
```

### 3. Task Failure Conflicts

Conflicts arising from mistakes and failures.

#### Personal Responsibility

```java
// High conscientiousness - owns the failure
Worker: "This failure is entirely my fault. I neglected to follow
proper safety protocols. I'm reviewing my procedures and will
implement verification steps to prevent this. I understand if you've
lost confidence in me."

// Low conscientiousness - deflects blame
Worker: "Well, that wasn't really my fault. The materials were
defective. Anyone would have had the same problem. I did the best
I could with what I had."

// High neuroticism - spiraling self-blame
Worker: "I messed up again... I'm such a failure! I know you must be
so angry with me! I keep trying but I keep failing! Maybe I'm just
not good enough for this work... I'm so sorry!"
```

#### Receiving Criticism

```java
// Low neuroticism - accepts criticism calmly
Worker: "You're right. I should have double-checked the measurements.
I'll add a verification step next time. Thank you for the feedback."

// High neuroticism - defensive/anxious
Worker: "I... I know! I already feel terrible about it! You don't
have to keep pointing it out! I'm trying my best! Please... just
let me fix it?"

// High openness - curious about improvement
Worker: "Interesting point! I hadn't considered that approach. Can
you show me what you mean? I'd love to improve my technique!"
```

---

## Personality-Based Conflict Styles

Each Big Five trait influences conflict resolution behavior.

### Extraversion (Sociability, Assertiveness)

**High Extraversion (80-100)**
- **Conflict Style**: Competing, direct
- **Dialogue Characteristics**: Verbal, expressive, confident
- **De-escalation**: Uses humor, reframing, optimism
- **Risk**: May dominate conversations, interrupt others

```
Template: "Hey! Let's talk about this! I think we're both passionate
about getting this right, so let's figure it out together!"

When wrong: "Whoops! My bad! You're totally right - let's do it your way!
No hard feelings, right?"

Apologizing: "Okay, I messed up! I own that! Let me make it right!
What do you need from me?"
```

**Low Extraversion (0-20)**
- **Conflict Style**: Avoiding, accommodating
- **Dialogue Characteristics**: Brief, hesitant, reflective
- **De-escalation**: Uses silence, pauses, minimal responses
- **Risk**: May withdraw entirely, not express needs

```
Template: "... I... I see things differently. But... it's okay.
We can do it your way."

When wrong: "... You're right. I'll... I'll handle it differently
next time."

Apologizing: "... My apologies. I'll... correct my approach.
Please... let me move on."
```

### Agreeableness (Cooperation, Trust)

**High Agreeableness (80-100)**
- **Conflict Style**: Accommodating, collaborating
- **Dialogue Characteristics**: Polite, concerned for others' feelings
- **De-escalation**: Uses empathy, validation, relationship focus
- **Risk**: May suppress own needs, accumulate resentment

```
Template: "I hear what you're saying, and your perspective makes sense.
At the same time, I have some concerns too. Can we find a solution that
works for both of us?"

When wrong: "Oh! I'm so sorry! You're absolutely right, and I feel
terrible that I caused this problem. Let me fix it right away!"

Apologizing: "I am truly, deeply sorry. I value our relationship more
than anything, and I hate that I've damaged it. Please tell me how I
can make this up to you. I want to earn back your trust."
```

**Low Agreeableness (0-20)**
- **Conflict Style**: Competing, direct
- **Dialogue Characteristics**: Blunt, task-focused, unemotional
- **De-escalation**: Uses logic, facts, efficiency arguments
- **Risk**: May seem uncaring, damage relationships

```
Template: "This approach is inefficient. Here's why: [facts].
We should do X instead. Your concerns are noted but don't change
the data."

When wrong: "Acknowledged. I was incorrect. Implementing correction.
Moving forward."

Apologizing: "I was wrong. This was my error. I'm taking corrective
action. Results will improve. End of statement."
```

### Conscientiousness (Organization, Discipline)

**High Conscientiousness (80-100)**
- **Conflict Style**: Collaborating, compromising
- **Dialogue Characteristics**: Structured, principled, thorough
- **De-escalation**: Uses procedures, rules, long-term thinking
- **Risk**: May be rigid, perfectionistic about being right

```
Template: "According to protocol, we should [procedure]. I understand
you want to speed things up, but skipping verification steps will
cause more problems later. Can we find a way to maintain standards
while improving efficiency?"

When wrong: "I failed to follow proper procedure. This is unacceptable.
I've identified where I went wrong and will implement additional
verification steps to prevent recurrence. I don't expect your trust to
be easily regained, but I'll work to earn it back."

Apologizing: "This failure represents a complete breakdown in my
operational protocols. I'm implementing a comprehensive review of all
my procedures. I understand if you've lost confidence in my abilities."
```

**Low Conscientiousness (0-20)**
- **Conflict Style**: Avoiding, accommodating
- **Dialogue Characteristics**: Casual, flexible, improvisational
- **De-escalation**: Uses humor, minimization, "it's fine"
- **Risk**: May not take conflicts seriously enough

```
Template: "Eh, it's not a big deal! We'll figure it out! Why stress?
Let's just wing it and see what happens!"

When wrong: "Oops! My bad! No worries, I'll fix it! These things
happen, right?"

Apologizing: "Yeah, sorry about that! I'll... you know, do better
next time! No harm done, hopefully!"
```

### Neuroticism (Emotional Stability)

**High Neuroticism (80-100)**
- **Conflict Style**: Avoiding, accommodating
- **Dialogue Characteristics**: Emotional, worried, self-blaming
- **De-escalation**: Needs reassurance, validation, time
- **Risk**: May catastrophize, panic, shut down

```
Template: "I... I don't want to cause any problems! I just... I
thought... maybe... but if you disagree, that's okay! I don't want
to fight! What do you think we should do? I'll go along with whatever
you want!"

When wrong: "Oh no! I messed up again! I'm so, so sorry! I feel
terrible! I know I keep making mistakes... I promise I'm trying my
hardest! Please don't be angry with me!"

Apologizing: "I can't believe I did this... I'm such a failure!
I've let you down again and again... I understand if you want to
replace me. I'd understand. I'm so sorry for being such a
disappointment..."
```

**Low Neuroticism (0-20)**
- **Conflict Style**: Competing, collaborating
- **Dialogue Characteristics**: Calm, stable, unflappable
- **De-escalation**: Uses logic, maintains perspective
- **Risk**: May seem uncaring, fail to acknowledge emotional impact

```
Template: "I disagree with your assessment. Here's my reasoning:
[facts]. I believe we should proceed differently. I'm open to
discussion but unconvinced by your current argument."

When wrong: "Error noted. Your correction is valid. I've updated my
understanding. Proceeding with new approach."

Apologizing: "I was incorrect. This has been noted. I'll adjust
accordingly. Is there anything else you need from me?"
```

### Openness (Creativity, Curiosity)

**High Openness (80-100)**
- **Conflict Style**: Collaborating, innovating
- **Dialogue Characteristics**: Curious, exploratory, big-picture
- **De-escalation**: Uses reframing, novel solutions, humor
- **Risk**: May overcomplicate simple issues

```
Template: "This is fascinating! We have two different perspectives!
What if we looked at this from a completely different angle? What
if we [creative solution]? I'm really curious to explore this!"

When wrong: "Interesting! I hadn't considered that approach! This
failure has revealed some valuable gaps in my understanding! I've
gathered interesting data! Would you like to hear my ideas for a
new approach?"

Apologizing: "While I failed - and I understand the seriousness -
this has opened up entirely new avenues of inquiry! I realize this
may not be the most empathetic response. I'm sorry. I'll address
the immediate problem, and then perhaps we can discuss what I've
learned?"
```

**Low Openness (0-20)**
- **Conflict Style**: Competing, avoiding
- **Dialogue Characteristics**: Traditional, practical, routine-focused
- **De-escalation**: Uses established methods, familiarity
- **Risk**: May resist all change, be inflexible

```
Template: "We've always done it this way, and it works. I don't see
why we need to change. Let's just stick to what we know."

When wrong: "Fine. We'll do it your way. But I still think the old
way would have worked fine."

Apologizing: "I'll do what you ask. I don't agree with the change,
but I'll follow orders."
```

---

## De-escalation Techniques

De-escalation reduces tension and creates space for resolution.

### Core Techniques

#### 1. Active Listening

Showing you understand the other person's perspective before responding.

```
Worker A: "You're being unreasonable about this resource allocation!"
Worker B: "It sounds like you feel I'm not considering your needs.
Is that right? Can you tell me more about what you need?"
```

**Implementation**:
```java
public String generateActiveListeningResponse(String otherStatement) {
    return String.format(
        "I hear you saying %s. Is that right? Help me understand " +
        "your perspective better.",
        paraphraseStatement(otherStatement)
    );
}
```

#### 2. Reflective Paraphrasing

Restating the other person's words to confirm understanding.

```
Worker A: "I need these materials for the urgent repair!"
Worker B: "So you're saying the repair is urgent and can't wait.
Is that accurate?"
```

#### 3. Validation

Acknowledging the other person's feelings without necessarily agreeing.

```
Worker A: "This is so frustrating! Why can't anything go smoothly?"
Worker B: "I can see this is really frustrating for you. It's
hard when things don't go as planned."
```

#### 4. Taking Space

Stepping away when emotions run high.

```
Worker: "I'm feeling frustrated and I don't want to say something
I'll regret. Can we take a few minutes to cool down? I'd like to
resolve this, but I need a moment first."
```

#### 5. Finding Common Ground

Identifying shared goals or values.

```
Worker A: "Your approach is all wrong!"
Worker B: "We both want this project to succeed. We disagree on
how to get there, but our goal is the same. Can we focus on that
shared goal?"
```

### De-escalation by Personality

| Personality | Preferred De-escalation | Example Phrase |
|-------------|------------------------|----------------|
| High Extraversion | Humor, reframing | "Hey, we're on the same team here! Let's take a breath!" |
| Low Extraversion | Silence, pause | "... [long pause] ... Let me think about that." |
| High Agreeableness | Empathy, validation | "I can see this is hard for you. I want to understand." |
| Low Agreeableness | Logic, facts | "The data shows X. Let's focus on that." |
| High Conscientiousness | Procedures, rules | "Let's follow proper conflict resolution protocol." |
| Low Conscientiousness | Minimization | "It's not that big a deal! We'll work it out!" |
| High Neuroticism | Reassurance seeking | "Please... I don't want to fight. Can we resolve this?" |
| Low Neuroticism | Perspective, calm | "This isn't worth high emotions. Let's be practical." |
| High Openness | Novel solutions | "What if we tried something completely different?" |
| Low Openness | Traditional methods | "Let's do what's worked before." |

---

## Apology Patterns

Authentic apologies have specific components that vary by personality.

### Components of a Sincere Apology

1. **Acknowledgment** - Clearly stating what was done wrong
2. **Responsibility** - Taking ownership without excuses
3. **Impact Recognition** - Acknowledging harm caused
4. **Remorse** - Expressing genuine regret
5. **Repair Offer** - Proposing how to make things right
6. **Commitment** - Promise to prevent recurrence

### Personality-Based Apologies

#### Perfectionist (High Conscientiousness)

```
Severity: MINOR
"I deviated slightly from optimal protocol. Correcting now."

Severity: MODERATE
"I failed to follow proper procedure for [task]. This is unacceptable.
I've identified where I went wrong and will implement corrective
measures. I understand your frustration."

Severity: CRITICAL
"This failure is entirely my fault. I neglected to follow established
protocols. I've reviewed my procedures and will implement additional
verification steps. I don't expect your trust to be easily regained,
but I'll work to earn it back through improved performance."
```

#### Worrier (High Neuroticism)

```
Severity: MINOR
"Oh! Sorry, sorry! I'll be more careful! Won't happen again, I promise!"

Severity: MODERATE
"Oh no, I'm so sorry! I failed at [task]! I feel terrible about it!
I know I keep making mistakes... I'm trying so hard, I promise!
Please forgive me! I'll do better!"

Severity: CRITICAL
"I can't believe I did this... I'm so, so sorry! I know I've let you
down again and again... I'm such a failure! Maybe I'm just not good
enough... I understand if you want to replace me. I'd understand.
I'm so sorry for being such a disappointment..."
```

#### Stoic (Low Neuroticism, Low-Moderate Traits)

```
Severity: MINOR
"Error noted. Correcting."

Severity: MODERATE
"I failed at [task]. I've identified the issue and will adjust my
approach. This won't happen again."

Severity: CRITICAL
"I've failed catastrophically at [task]. This is unacceptable. I'm
reviewing all my procedures. I'll regain your trust through improved
performance. Actions speak louder than words."
```

#### Enthusiastic (High Extraversion)

```
Severity: MINOR
"Whoops! My bad! No worries, I got this!"

Severity: MODERATE
"Okay, so I messed up [task]! That's on me! But you know what?
I'm gonna learn from this and come back stronger! Watch this space!"

Severity: CRITICAL
"... Okay, that was really bad. Like, REALLY bad. I completely failed
at [task]. I'm not gonna make excuses - I messed up big time. I know
I might have lost your trust, but I'm going to work harder than ever
to earn it back! I hope you'll stick with me through this. I won't
let you down again!"
```

#### Accommodating (High Agreeableness)

```
Severity: MINOR
"Oh! I'm sorry! Let me fix that right away!"

Severity: MODERATE
"I'm so sorry I failed at [task]! I feel terrible about letting you
down! Your trust means so much to me. I'll work extra hard to make
this right! Is there anything specific you'd like me to do differently?"

Severity: CRITICAL
"I... I don't know how to apologize enough for failing at [task].
I've broken your trust, and I know that's the most valuable thing we
had. I understand if you're angry or disappointed. I would be too.
If you can find it in your heart to forgive me, I'll spend every
moment trying to be worthy of your trust again."
```

#### Direct (Low Agreeableness)

```
Severity: MINOR
"Mistake made. Correcting."

Severity: MODERATE
"I failed at [task]. This was my error. I'm implementing a fix.
Moving forward."

Severity: CRITICAL
"Catastrophic failure at [task]. I take full responsibility. I'm
implementing comprehensive reforms. Your trust will be regained
through results, not words."
```

#### Innovator (High Openness)

```
Severity: MINOR
"Fascinating! That approach had unexpected results. Adjusting!"

Severity: MODERATE
"Fascinating failure at [task]! This has revealed interesting gaps
in my understanding. I've gathered valuable data. I believe I can
develop a completely new approach! Would you like to hear my ideas?"

Severity: CRITICAL
"While I failed catastrophically - and I understand the seriousness -
I must note the fascinating patterns that emerged! This failure has
opened up entirely new avenues of inquiry! ...I realize this may not
be the most empathetic response. I'm sorry. I'll address the
immediate problem, and then perhaps we can discuss what I've learned?"
```

---

## Forgiveness Dialogue

How workers accept apologies and move forward.

### Forgiveness Response Components

1. **Acknowledgment** - Recognizing the apology
2. **Validation** - Accepting the sincerity
3. **Impact Statement** - Acknowledging hurt (optional)
4. **Decision** - Granting or withholding forgiveness
5. **Path Forward** - How to proceed

### Forgiveness by Personality

#### High Agreeableness (Quick to Forgive)

```
Immediate Forgiveness:
"Oh, don't worry about it! Everyone makes mistakes! I appreciate the
apology, but it's really not necessary! We're good!"

Forgiveness with Guidance:
"I accept your apology, and I'm not angry. Thank you for saying sorry.
Going forward, maybe we could [suggestion]? That might help avoid
this happening again."

Hurt but Forgiving:
"I... I appreciate the apology. That did hurt my feelings a bit, but
I know you didn't mean to. I forgive you. Let's just... be a little
more careful with this in the future, okay?"
```

#### Low Agreeableness (Cautious Forgiveness)

```
Conditional Forgiveness:
"Apology accepted. However, trust is rebuilt through actions, not
words. I'll be watching for improved performance before I fully
trust you again."

Skepticism:
"You say you're sorry, but I've heard this before. I'll accept the
apology, but I'm not convinced anything will change. Prove it."

Direct Terms:
"Fine. You're sorry. Noted. Let's move on. But if this happens
again, there will be consequences."
```

#### High Neuroticism (Struggle to Forgive)

```
Anxious Forgiveness:
"I... I forgive you! I do! It's just... I'm still worried it will
happen again. You promise you'll be more careful? Because I don't
think I can handle this again..."

Internal Struggle:
"I want to forgive you. I really do. And logically I know I should.
But emotionally, I'm still hurt. Can you give me some time? I'm
trying, but... I need to process this."

Fearful Forgiveness:
"I forgive you! Please don't be mad at me! I mean, I was hurt, but
I don't want to cause problems! We're good, right? Right?"
```

#### Low Neuroticism (Easy Forgiveness)

```
Practical Forgiveness:
"Apology accepted. Mistakes happen. Let's move forward. Is there
anything we need to do differently to prevent recurrence?"

Brief Forgiveness:
"Fine. It's handled. No need to dwell on it. Back to work?"

Stoic Forgiveness:
"Your apology is noted and accepted. The incident is closed. Let's
continue."
```

#### High Conscientiousness (Principled Forgiveness)

```
Process-Oriented Forgiveness:
"I accept your apology. However, I'm concerned about the procedural
failure that led to this. What specific steps are you taking to
prevent recurrence? I need to see your corrective action plan before
we can fully move forward."

Conditional on Improvement:
"Thank you for the apology. I appreciate you taking responsibility.
For me to fully trust you again, I need to see consistent improved
performance over time. Let's check in on this in [time period]."

High Standards:
"I accept your apology, but I want to be clear: this standard of
performance is unacceptable. I'm forgiving you, but I'm also
expecting significant improvement. Let's establish clear metrics
going forward."
```

### Forgiveness Progression

Relationships should progress through stages after conflict:

1. **Acceptance Phase** (0-2 in-game days)
   - Apology acknowledged
   - Cautious interaction
   - Short, functional dialogue

2. **Processing Phase** (3-7 in-game days)
   - Testing the waters
   - References to the conflict ("Since that issue with...")
   - Gradual return to normal

3. **Integration Phase** (8-14 in-game days)
   - Conflict becomes part of shared history
   - Can joke about it (if personalities allow)
   - Lessons integrated into behavior

4. **Growth Phase** (14+ in-game days)
   - Relationship stronger than before
   - Reference point for future conflicts
   - "Remember when we..." bonding

---

## Compromise Language

How workers propose alternatives and find middle ground.

### Compromise Openers

```
"What if we..."
"Have you considered..."
"I have an idea that might help both of us..."
"Can we find a solution that addresses both our needs..."
"Maybe we could meet in the middle..."
```

### Compromise Structures

#### The "Both-And" Approach

```
Instead of: "I need this resource."
Or: "No, I need it more."

Try: "What if we BOTH get what we need? I'll use the top half,
you take the bottom half. That way, we can both complete our tasks."
```

#### The "Trial Period" Approach

```
"I'm not fully convinced, but what if we try your approach for
[tasks/time period]? If it works better, great! If not, we can
revisit my approach. Fair?"
```

#### The "Modified" Approach

```
"I see your point, but I have concerns. What if we modify your
plan to [modification]? That addresses my concerns while keeping
most of your idea."
```

#### The "Sequential" Approach

```
"We're both prioritized, but maybe I go first this time and you
go first next time? We can alternate. That way, we both get what
we need, just at different times."
```

#### The "Expanding the Pie" Approach

```
"What if we looked at this differently? Instead of fighting over
[limited resource], what if we [expand scope/alternative solution]?
Then we'd both have enough."
```

### Refusing Compromise Gracefully

Sometimes compromise isn't possible. Here's how to refuse without
escalating:

```
"I understand your proposal, but I can't agree to it. This is a
matter of [principle/safety/protocol]. I'm open to other solutions,
but that particular compromise isn't something I can accept."

"I've considered your suggestion, but I don't feel comfortable with
it. This is too important to me to compromise on. Can we explore
other options?"

"I wish I could meet you halfway on this, but I can't. I respect
your position, and I hope you can respect mine. Let's see if we
can find a different solution entirely."
```

---

## Escalation Triggers to Avoid

Certain phrases and patterns escalate conflicts unnecessarily.

### Defensive Escalators

| Don't Say | Why It Escalates | Better Alternative |
|-----------|------------------|-------------------|
| "You're being unreasonable" | Attacks character, dismisses feelings | "I see this differently" |
| "Calm down" | Invalidates emotions, sounds condescending | "I can see this is upsetting" |
| "You always..." | Overgeneralization, defensive reaction | "This specific time..." |
| "That's not my fault" | Deflects blame, avoids responsibility | "Let me understand what happened" |
| "You're overreacting" | Minimizes feelings, creates resentment | "I can see this is important to you" |

### Aggressive Escalators

| Don't Say | Why It Escalates | Better Alternative |
|-----------|------------------|-------------------|
| "You're wrong" | Direct confrontation | "I see it differently" |
| "Just do what I say" | Authoritarian, dismisses input | "Here's my reasoning" |
| "I don't care what you think" | Destroys relationship | "Your input is valued, however..." |
| "This is pointless" | Gives up on resolution | "We seem to be stuck, let's try..." |
| "You're the problem" | Personal attack | "We have a disagreement" |

### Passive-Aggressive Escalators

| Don't Say | Why It Escalates | Better Alternative |
|-----------|------------------|-------------------|
| "Fine, whatever" | Dismissive, insincere | "I still have concerns, but I'll comply" |
| "If you want to do it wrong..." | Backhanded criticism | "I have a different approach in mind" |
| "I guess I'll just do it myself" | Guilt-tripping | "I'm concerned about this approach" |
| "Whatever you want" | Abdicates responsibility resentfully | "I'm not fully comfortable, but okay" |
| "I was just trying to help" | Defensive manipulation | "My intention was to assist" |

### Dismissive Escalators

| Don't Say | Why It Escalates | Better Alternative |
|-----------|------------------|-------------------|
| "It's not a big deal" | Minimizes other's concern | "I can see this matters to you" |
| "You're too sensitive" | Invalidates emotions | "I didn't mean to upset you" |
| "Let's just move on" | Rushes resolution | "I want to make sure we address this" |
| "I don't have time for this" | Prioritizes speed over relationship | "This is important, can we talk later?" |
| "You're making a scene" | Shames, public criticism | "Let's discuss this privately" |

---

## Dialogue Templates

### Inter-Worker Conflict Templates

#### Resource Dispute

```java
/**
 * Template for resource conflict resolution between workers.
 * Personality affects approach and language.
 */
public String generateResourceConflictResponse(
    String otherWorker,
    ResourceType resource,
    PersonalityTraits myPersonality,
    PersonalityTraits theirPersonality,
    boolean IWasFirst
) {
    // High agreeableness - seeks compromise
    if (myPersonality.getAgreeableness() >= 70) {
        if (IWasFirst) {
            return String.format(
                "Hey %s! I was actually hoping to use this %s for %s. " +
                "Is there a way we could share? Or maybe you could use " +
                "the other cluster nearby?",
                otherWorker,
                resource.getName(),
                getMyTask()
            );
        } else {
            return String.format(
                "Oh! I see you're already using this %s. I'll look elsewhere! " +
                "Sorry to interrupt!",
                resource.getName()
            );
        }
    }

    // Low agreeableness - direct/assertive
    if (myPersonality.getAgreeableness() <= 30) {
        if (IWasFirst) {
            return String.format(
                "%s, I claimed this %s first. I'm using it for %s. " +
                "You'll need to find another source.",
                otherWorker,
                resource.getName(),
                getMyTask()
            );
        } else {
            return String.format(
                "You were here first. I'll find resources elsewhere.",
                otherWorker
            );
        }
    }

    // High openness - creative solutions
    if (myPersonality.getOpenness() >= 70) {
        return String.format(
            "%s! What if we pooled our efforts on this %s? " +
            "Maybe we could [creative suggestion]! That way we'd " +
            "both benefit!",
            otherWorker,
            resource.getName()
        );
    }

    // Balanced response
    return String.format(
        "%s, I need this %s for %s. What are you using it for? " +
        "Maybe we can work something out.",
        otherWorker,
        resource.getName(),
        getMyTask()
    );
}
```

#### Methodological Disagreement

```java
/**
 * Template for disagreements about how to approach a task.
 */
public String generateMethodDisagreementResponse(
    String otherWorker,
    String theirApproach,
    String myApproach,
    PersonalityTraits personality,
    int disagreementSeverity
) {
    if (disagreementSeverity <= 30) {
        // Low severity - mild preference
        if (personality.getAgreeableness() >= 70) {
            return String.format(
                "I see your point about %s. I was thinking %s might work " +
                "better, but I'm open to trying your way first!",
                theirApproach,
                myApproach
            );
        } else {
            return String.format(
                "I prefer %s. Your approach has merit too. Either should work.",
                myApproach
            );
        }
    } else if (disagreementSeverity <= 60) {
        // Medium severity - strong preference
        if (personality.getConscientiousness() >= 70) {
            return String.format(
                "I have concerns about %s. Based on established procedures, " +
                "%s would be more appropriate. The risks of your approach " +
                "include [risks]. Can we discuss this further?",
                theirApproach,
                myApproach
            );
        } else {
            return String.format(
                "I really think %s is the wrong approach. %s would be much " +
                "more effective. Why do you prefer your method?",
                theirApproach,
                myApproach
            );
        }
    } else {
        // High severity - potential impasse
        if (personality.getExtraversion() >= 70) {
            return String.format(
                "%s, we need to talk. Your approach of %s is going to cause " +
                "serious problems! I've been doing this a while, and %s is " +
                "the way to go. Please trust me on this!",
                otherWorker,
                theirApproach,
                myApproach
            );
        } else if (personality.getNeuroticism() >= 70) {
            return String.format(
                "I... I really don't think %s is a good idea... It makes me " +
                "really nervous. Could we please do %s instead? I'd feel " +
                "much safer...",
                theirApproach,
                myApproach
            );
        } else {
            return String.format(
                "I cannot support %s. The risks are too high. I'm proceeding " +
                "with %s. If you disagree, we may need to bring in the " +
                "player to mediate.",
                theirApproach,
                myApproach
            );
        }
    }
}
```

### Worker-Player Conflict Templates

#### Impossible Command Response

```java
/**
 * Template for responding to player commands that can't be fulfilled.
 */
public String generateImpossibleCommandResponse(
    String command,
    String reason,
    PersonalityTraits personality,
    int rapportLevel
) {
    // High rapport + high agreeableness - gentle
    if (rapportLevel >= 70 && personality.getAgreeableness() >= 70) {
        return String.format(
            "I want to help, but I can't %s. %s. Is there another way " +
            "I can assist you? I'm happy to help with anything else!",
            command,
            reason
        );
    }

    // Low rapport + low agreeableness - blunt
    if (rapportLevel <= 30 && personality.getAgreeableness() <= 30) {
        return String.format(
            "I cannot %s. %s. Adjust your command.",
            command,
            reason
        );
    }

    // High neuroticism - anxious
    if (personality.getNeuroticism() >= 70) {
        return String.format(
            "Oh no... I can't %s... %s. Please don't be mad! I tried my " +
            "best! Is there something else I can do instead? I don't want " +
            "to disappoint you!",
            command,
            reason
        );
    }

    // High openness - problem solving
    if (personality.getOpenness() >= 70) {
        return String.format(
            "That's an interesting challenge! I can't %s directly because " +
            "%s. However, what if we tried [alternative solution]? Or " +
            "perhaps [another option]? I have a few ideas!",
            command,
            reason
        );
    }

    // Balanced response
    return String.format(
        "I can't %s. %s. Would you like me to [alternative] instead?",
        command,
        reason
    );
}
```

#### Ethical Objection Response

```java
/**
 * Template for objecting to commands on ethical/principled grounds.
 */
public String generateEthicalObjectionResponse(
    String command,
    String principle,
    PersonalityTraits personality,
    int rapportLevel
) {
    String baseResponse = String.format(
        "I can't follow that command. It would violate %s.",
        principle
    );

    // High conscientiousness - principled but helpful
    if (personality.getConscientiousness() >= 70) {
        return String.format(
            "%s I'm happy to help with a modified approach that meets " +
            "your goal without violating %s. Would you like to discuss " +
            "alternatives?",
            baseResponse,
            principle
        );
    }

    // Low conscientiousness - reluctant but compliant
    if (personality.getConscientiousness() <= 30) {
        if (rapportLevel >= 60) {
            return String.format(
                "I... I really don't feel comfortable with this. It goes " +
                "against %s. But if you insist... I guess I'll do it. " +
                "Please don't make me do this again.",
                principle
            );
        } else {
            return String.format(
                "Look, I know this violates %s, but if that's what you " +
                "want... fine. But I'm noting my objection. Don't blame " +
                "me if something goes wrong.",
                principle
            );
        }
    }

    // High neuroticism - distressed
    if (personality.getNeuroticism() >= 70) {
        return String.format(
            "Please... I can't do that. It would violate %s, and that's " +
            "really important to me! I'd feel terrible! Please, can we " +
            "find another way? I'll do anything else!",
            principle
        );
    }

    return baseResponse;
}
```

### Apology Templates

#### Accepting Fault

```java
/**
 * Template for accepting responsibility for mistakes.
 */
public String generateAcceptanceOfFaultResponse(
    String faultDescription,
    int severity,
    PersonalityTraits personality,
    boolean isRepeated
) {
    String repetitionClause = isRepeated ?
        " I know this keeps happening, and I'm working to address it." : "";

    if (personality.getConscientiousness() >= 80) {
        // Perfectionist response
        if (severity <= 20) {
            return String.format(
                "Minor procedural error. Correcting.",
                faultDescription
            );
        } else if (severity <= 50) {
            return String.format(
                "I failed to properly execute %s. This is unacceptable. " +
                "I've identified where I went wrong and will implement " +
                "corrective measures.%s",
                faultDescription,
                repetitionClause
            );
        } else {
            return String.format(
                "This failure is entirely my fault. I neglected to follow " +
                "proper protocol for %s. I've reviewed my procedures and " +
                "will implement additional verification steps to prevent " +
                "recurrence.%s I understand if you've lost confidence in me.",
                faultDescription,
                repetitionClause
            );
        }
    }

    if (personality.getNeuroticism() >= 70) {
        // Worrier response
        if (severity <= 20) {
            return "Oh! Sorry, sorry! I'll be more careful!";
        } else if (severity <= 50) {
            return String.format(
                "Oh no, I'm so sorry! I failed at %s! I feel terrible!%s " +
                "Please forgive me! I'll do better!",
                faultDescription,
                repetitionClause
            );
        } else {
            return String.format(
                "I can't believe I did this... I'm so, so sorry! I failed " +
                "at %s!%s Maybe I'm just not good enough... I understand " +
                "if you want to replace me...",
                faultDescription,
                repetitionClause
            );
        }
    }

    if (personality.getNeuroticism() <= 20) {
        // Stoic response
        if (severity <= 20) {
            return "Error noted. Correcting.";
        } else if (severity <= 50) {
            return String.format(
                "I failed at %s. I've identified the issue and will adjust. " +
                "This won't happen again.",
                faultDescription
            );
        } else {
            return String.format(
                "Significant failure at %s. This is unacceptable. I'm " +
                "implementing preventative measures. I'll regain your " +
                "trust through improved performance.",
                faultDescription
            );
        }
    }

    // Balanced response
    if (severity <= 20) {
        return "Oops, my mistake! Let me fix that.";
    } else if (severity <= 50) {
        return String.format(
            "I'm sorry about failing at %s. That was my mistake. I've " +
            "learned from this and will do better next time.%s",
            faultDescription,
            repetitionClause
        );
    } else {
        return String.format(
            "I truly apologize for failing at %s. I understand this is " +
            "significant. I feel bad about letting you down. I'm " +
            "committed to making this right.%s Thank you for your patience.",
            faultDescription,
            repetitionClause
        );
    }
}
```

---

## Java Implementation

### ConflictResolutionManager Class

```java
package com.minewright.conflict;

import com.minewright.personality.PersonalityTraits;
import com.minewright.memory.CompanionMemory;
import java.time.Instant;
import java.util.*;

/**
 * Manages conflict detection, resolution, and dialogue generation for
 * MineWright workers.
 *
 * <p>This system handles:</p>
 * <ul>
 *   <li>Conflict detection between workers and with players</li>
 *   <li>Personality-appropriate response generation</li>
 *   <li>Escalation/de-escalation tracking</li>
 *   <li>Apology and forgiveness dialogue</li>
 *   <li>Memory integration for persistent relationship effects</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class ConflictResolutionManager {

    private final String workerName;
    private final PersonalityTraits personality;
    private final CompanionMemory memory;

    // Conflict state tracking
    private int currentTensionLevel = 0;  // 0-100
    private Map<String, Integer> relationshipTension = new HashMap<>();
    private List<ConflictRecord> pastConflicts = new ArrayList<>();
    private Optional<ActiveConflict> activeConflict = Optional.empty();

    // Cooldowns and timing
    private long lastConflictTime = 0;
    private final long conflictCooldownMillis = 60000;  // 1 minute
    private int conflictsSinceLastApology = 0;

    public ConflictResolutionManager(
            String workerName,
            PersonalityTraits personality,
            CompanionMemory memory) {
        this.workerName = workerName;
        this.personality = personality;
        this.memory = memory;
    }

    /**
     * Represents an active conflict that needs resolution.
     */
    public static class ActiveConflict {
        private final String conflictId;
        private final ConflictType type;
        private final String otherParty;
        private final int severity;
        private final String description;
        private final Instant startTime;
        private int tensionLevel;
        private ConflictStage stage;

        public ActiveConflict(
                String conflictId,
                ConflictType type,
                String otherParty,
                int severity,
                String description) {
            this.conflictId = conflictId;
            this.type = type;
            this.otherParty = otherParty;
            this.severity = severity;
            this.description = description;
            this.startTime = Instant.now();
            this.tensionLevel = severity;
            this.stage = ConflictStage.EMERGING;
        }

        public enum ConflictType {
            RESOURCE_DISPUTE,
            TERRITORIAL_DISPUTE,
            METHODOLOGICAL_DISAGREEMENT,
            IMPOSSIBLE_COMMAND,
            ETHICAL_OBJECTION,
            TASK_FAILURE,
            PERSONALITY_CLASH
        }

        public enum ConflictStage {
            EMERGING,      // Conflict just started
            ESCALATING,    // Tension increasing
            PEAK,          // Maximum tension
            DE_ESCALATING, // Tension decreasing
            RESOLVING,     // Finding solution
            RESOLVED,      // Conflict resolved
            UNRESOLVED     // Conflict abandoned
        }
    }

    /**
     * Represents a historical conflict record.
     */
    public static class ConflictRecord {
        private final String conflictId;
        private final ConflictType type;
        private final String otherParty;
        private final int severity;
        private final boolean wasResolved;
        private final String resolutionMethod;
        private final Instant timestamp;
        private final int tensionImpact;  // How it affected the relationship

        public ConflictRecord(
                String conflictId,
                ConflictType type,
                String otherParty,
                int severity,
                boolean wasResolved,
                String resolutionMethod,
                int tensionImpact) {
            this.conflictId = conflictId;
            this.type = type;
            this.otherParty = otherParty;
            this.severity = severity;
            this.wasResolved = wasResolved;
            this.resolutionMethod = resolutionMethod;
            this.tensionImpact = tensionImpact;
            this.timestamp = Instant.now();
        }
    }

    /**
     * Initiates a conflict and generates appropriate dialogue.
     */
    public ConflictInitiation initiateConflict(
            ConflictType type,
            String otherParty,
            int severity,
            String description) {

        // Check cooldown
        if (System.currentTimeMillis() - lastConflictTime < conflictCooldownMillis) {
            return ConflictInitiation.cooldown();
        }

        // Create active conflict
        String conflictId = UUID.randomUUID().toString();
        ActiveConflict conflict = new ActiveConflict(
            conflictId, type, otherParty, severity, description
        );
        activeConflict = Optional.of(conflict);

        // Update tension
        currentTensionLevel = Math.min(100, currentTensionLevel + severity);
        relationshipTension.put(otherParty,
            relationshipTension.getOrDefault(otherParty, 0) + severity);

        // Generate initiation dialogue
        String dialogue = generateConflictInitiation(conflict);

        return ConflictInitiation.initiated(dialogue, conflict);
    }

    /**
     * Generates the initial conflict statement based on personality and severity.
     */
    private String generateConflictInitiation(ActiveConflict conflict) {
        // This is where personality-based dialogue generation happens
        // See full implementation in ConflictDialogueGenerator

        return ConflictDialogueGenerator.generateInitiation(
            conflict,
            personality,
            memory.getRapportLevel()
        );
    }

    /**
     * Attempts to de-escalate an active conflict.
     */
    public DeescalationResult attemptDeescalation() {
        if (activeConflict.isEmpty()) {
            return DeescalationResult.noConflict();
        }

        ActiveConflict conflict = activeConflict.get();

        // Personality affects de-escalation success
        double deescalationChance = calculateDeescalationChance(personality);

        if (Math.random() < deescalationChance) {
            // Successful de-escalation
            conflict.tensionLevel = Math.max(0, conflict.tensionLevel - 20);
            conflict.stage = ActiveConflict.ConflictStage.DE_ESCALATING;

            String dialogue = generateDeescalationDialogue(conflict);
            return DeescalationResult.success(dialogue);
        } else {
            // Failed de-escalation (escalation continues)
            conflict.tensionLevel = Math.min(100, conflict.tensionLevel + 10);
            conflict.stage = ActiveConflict.ConflictStage.ESCALATING;

            String dialogue = generateEscalationDialogue(conflict);
            return DeescalationResult.escalated(dialogue);
        }
    }

    /**
     * Calculates the likelihood of successful de-escalation based on personality.
     */
    private double calculateDeescalationChance(PersonalityTraits p) {
        double baseChance = 0.5;

        // High agreeableness increases de-escalation
        if (p.getAgreeableness() >= 70) baseChance += 0.2;
        if (p.getAgreeableness() <= 30) baseChance -= 0.2;

        // High neuroticism decreases de-escalation (anxiety escalates)
        if (p.getNeuroticism() >= 70) baseChance -= 0.15;
        if (p.getNeuroticism() <= 30) baseChance += 0.1;

        // High openness increases de-escalation (creative solutions)
        if (p.getOpenness() >= 70) baseChance += 0.1;

        // High conscientiousness slightly increases (process-oriented)
        if (p.getConscientiousness() >= 70) baseChance += 0.05;

        return Math.max(0.1, Math.min(0.9, baseChance));
    }

    /**
     * Generates an apology for a mistake or conflict.
     */
    public ApologyResponse generateApology(
            String mistake,
            int severity,
            boolean isRepeated) {

        // Determine if character would actually apologize
        if (!shouldApologinate(severity)) {
            return ApologyResponse.refused(
                generateRefusalDialogue(mistake)
            );
        }

        // Generate personality-appropriate apology
        String apology = ConflictDialogueGenerator.generateApology(
            mistake,
            severity,
            personality,
            isRepeated,
            memory.getRapportLevel()
        );

        // Update state
        conflictsSinceLastApology = 0;
        if (activeConflict.isPresent()) {
            activeConflict.get().stage = ActiveConflict.ConflictStage.RESOLVING;
        }

        return ApologyResponse.accepted(apology);
    }

    /**
     * Determines if character will apologize based on personality and severity.
     */
    private boolean shouldApologinate(int severity) {
        // High conscientiousness always apologizes for significant failures
        if (personality.getConscientiousness() >= 80 && severity >= 40) {
            return true;
        }

        // Low agreeableness rarely apologizes unless severe
        if (personality.getAgreeableness() <= 20 && severity < 60) {
            return false;
        }

        // High neuroticism always apologizes (anxious)
        if (personality.getNeuroticism() >= 70) {
            return true;
        }

        // Moderate traits apologize for moderate+ severity
        return severity >= 30;
    }

    /**
     * Generates forgiveness dialogue.
     */
    public ForgivenessResponse generateForgiveness(
            String otherParty,
            String theirApology,
            int apologyQuality) {  // 1-10

        // Determine if forgiveness is granted
        boolean forgives = willForgive(otherParty, apologyQuality);

        String response = ConflictDialogueGenerator.generateForgiveness(
            otherParty,
            theirApology,
            forgives,
            personality,
            getRelationshipTension(otherParty)
        );

        if (forgives) {
            // Reduce tension
            int currentTension = relationshipTension.getOrDefault(otherParty, 0);
            relationshipTension.put(otherParty, Math.max(0, currentTension - 30));
            currentTensionLevel = Math.max(0, currentTensionLevel - 10);
        } else {
            // Increase tension (refusal to forgive)
            int currentTension = relationshipTension.getOrDefault(otherParty, 0);
            relationshipTension.put(otherParty, Math.min(100, currentTension + 10));
        }

        return ForgivenessResponse.of(response, forgives);
    }

    /**
     * Determines if character will forgive based on personality and context.
     */
    private boolean willForgive(String otherParty, int apologyQuality) {
        int relationshipTension = getRelationshipTension(otherParty);

        // High agreeableness forgives easily
        if (personality.getAgreeableness() >= 80) {
            return apologyQuality >= 4;  // Pretty forgiving
        }

        // Low agreeableness requires high quality apology
        if (personality.getAgreeableness() <= 20) {
            return apologyQuality >= 8 && relationshipTension < 70;
        }

        // High relationship tension makes forgiveness harder
        if (relationshipTension >= 80) {
            return apologyQuality >= 9;
        }

        // Low relationship tension makes forgiveness easier
        if (relationshipTension <= 30) {
            return apologyQuality >= 5;
        }

        // Moderate case
        return apologyQuality >= 6;
    }

    /**
     * Records a resolved conflict to memory.
     */
    public void recordConflictResolution(
            String conflictId,
            boolean wasResolved,
            String resolutionMethod) {

        activeConflict.ifPresent(conflict -> {
            ConflictRecord record = new ConflictRecord(
                conflictId,
                conflict.type,
                conflict.otherParty,
                conflict.severity,
                wasResolved,
                resolutionMethod,
                wasResolved ? -conflict.severity / 2 : conflict.severity
            );

            pastConflicts.add(record);

            // Update memory
            if (wasResolved) {
                memory.recordPositiveInteraction(
                    conflict.otherParty,
                    "Resolved conflict: " + resolutionMethod
                );
            } else {
                memory.recordNegativeInteraction(
                    conflict.otherParty,
                    "Unresolved conflict: " + conflict.description
                );
            }

            // Clear active conflict
            activeConflict = Optional.empty();
            lastConflictTime = System.currentTimeMillis();
        });
    }

    /**
     * Gets the current tension level with a specific entity.
     */
    public int getRelationshipTension(String otherParty) {
        return relationshipTension.getOrDefault(otherParty, 0);
    }

    /**
     * Gets the overall tension level.
     */
    public int getCurrentTensionLevel() {
        return currentTensionLevel;
    }

    /**
     * Result classes for conflict operations.
     */
    public static class ConflictInitiation {
        private final boolean initiated;
        private final String dialogue;
        private final ActiveConflict conflict;

        private ConflictInitiation(boolean initiated, String dialogue,
                                  ActiveConflict conflict) {
            this.initiated = initiated;
            this.dialogue = dialogue;
            this.conflict = conflict;
        }

        public static ConflictInitiation initiated(String dialogue, ActiveConflict conflict) {
            return new ConflictInitiation(true, dialogue, conflict);
        }

        public static ConflictInitiation cooldown() {
            return new ConflictInitiation(false,
                "I need a moment before discussing this again.", null);
        }

        public boolean wasInitiated() { return initiated; }
        public String getDialogue() { return dialogue; }
        public ActiveConflict getConflict() { return conflict; }
    }

    public static class DeescalationResult {
        private final boolean success;
        private final boolean escalated;
        private final String dialogue;

        private DeescalationResult(boolean success, boolean escalated, String dialogue) {
            this.success = success;
            this.escalated = escalated;
            this.dialogue = dialogue;
        }

        public static DeescalationResult success(String dialogue) {
            return new DeescalationResult(true, false, dialogue);
        }

        public static DeescalationResult escalated(String dialogue) {
            return new DeescalationResult(false, true, dialogue);
        }

        public static DeescalationResult noConflict() {
            return new DeescalationResult(false, false, "");
        }

        public boolean wasSuccessful() { return success; }
        public boolean didEscalate() { return escalated; }
        public String getDialogue() { return dialogue; }
    }

    public static class ApologyResponse {
        private final boolean offered;
        private final String dialogue;

        private ApologyResponse(boolean offered, String dialogue) {
            this.offered = offered;
            this.dialogue = dialogue;
        }

        public static ApologyResponse accepted(String dialogue) {
            return new ApologyResponse(true, dialogue);
        }

        public static ApologyResponse refused(String dialogue) {
            return new ApologyResponse(false, dialogue);
        }

        public boolean wasOffered() { return offered; }
        public String getDialogue() { return dialogue; }
    }

    public static class ForgivenessResponse {
        private final String dialogue;
        private final boolean forgiven;

        private ForgivenessResponse(String dialogue, boolean forgiven) {
            this.dialogue = dialogue;
            this.forgiven = forgiven;
        }

        public static ForgivenessResponse of(String dialogue, boolean forgiven) {
            return new ForgivenessResponse(dialogue, forgiven);
        }

        public String getDialogue() { return dialogue; }
        public boolean wasForgiven() { return forgiven; }
    }

    public enum ConflictType {
        RESOURCE_DISPUTE,
        TERRITORIAL_DISPUTE,
        METHODOLOGICAL_DISAGREEMENT,
        IMPOSSIBLE_COMMAND,
        ETHICAL_OBJECTION,
        TASK_FAILURE,
        PERSONALITY_CLASH
    }
}
```

### ConflictDialogueGenerator Class

```java
package com.minewright.conflict;

import com.minewright.personality.PersonalityTraits;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates personality-appropriate dialogue for conflict scenarios.
 *
 * <p>All dialogue generation methods take personality traits into account
 * to ensure consistent character voice during conflicts.</p>
 */
public class ConflictDialogueGenerator {

    /**
     * Generates conflict initiation dialogue.
     */
    public static String generateInitiation(
            ConflictResolutionManager.ActiveConflict conflict,
            PersonalityTraits personality,
            int rapportLevel) {

        switch (conflict.type) {
            case RESOURCE_DISPUTE:
                return generateResourceDisputeInitiation(conflict, personality);
            case TERRITORIAL_DISPUTE:
                return generateTerritorialDisputeInitiation(conflict, personality);
            case METHODOLOGICAL_DISAGREEMENT:
                return generateMethodDisputeInitiation(conflict, personality);
            case IMPOSSIBLE_COMMAND:
                return generateImpossibleCommandResponse(conflict, personality, rapportLevel);
            case ETHICAL_OBJECTION:
                return generateEthicalObjectionResponse(conflict, personality, rapportLevel);
            case TASK_FAILURE:
                return generateTaskFailureResponse(conflict, personality);
            default:
                return generateGenericDisputeInitiation(conflict, personality);
        }
    }

    private static String generateResourceDisputeInitiation(
            ConflictResolutionManager.ActiveConflict conflict,
            PersonalityTraits personality) {

        boolean IWasFirst = conflict.description.contains("first");

        if (personality.getAgreeableness() >= 70) {
            if (IWasFirst) {
                return String.format(
                    "Hey %s! I was actually hoping to use this for %s. " +
                    "Is there a way we could share? Or maybe there's " +
                    "another source nearby?",
                    conflict.otherParty,
                    conflict.description
                );
            } else {
                return String.format(
                    "Oh! I see you're already using this. I'll look " +
                    "elsewhere! Sorry to interrupt, %s!",
                    conflict.otherParty
                );
            }
        }

        if (personality.getAgreeableness() <= 30) {
            if (IWasFirst) {
                return String.format(
                    "%s, I claimed this first. I'm using it. You'll " +
                    "need to find another source.",
                    conflict.otherParty
                );
            } else {
                return String.format(
                    "You were here first. I'll find resources elsewhere.",
                    conflict.otherParty
                );
            }
        }

        if (personality.getOpenness() >= 70) {
            return String.format(
                "%s! What if we pooled our efforts? Maybe we could " +
                "collaborate on this resource! That way we'd both " +
                "benefit more than if we worked separately!",
                conflict.otherParty
            );
        }

        // Balanced
        return String.format(
            "%s, I need this resource. What are you using it for? " +
            "Maybe we can work something out.",
            conflict.otherParty
        );
    }

    private static String generateMethodDisputeInitiation(
            ConflictResolutionManager.ActiveConflict conflict,
            PersonalityTraits personality) {

        String theirMethod = extractMethodFromDescription(conflict.description);

        if (personality.getConscientiousness() >= 70) {
            return String.format(
                "I have concerns about %s. Based on established " +
                "procedures, a different approach would be more " +
                "appropriate. The risks include structural issues and " +
                "efficiency losses. Can we discuss this further?",
                theirMethod
            );
        }

        if (personality.getExtraversion() >= 70) {
            return String.format(
                "Hey %s! I totally disagree with doing %s! That's " +
                "gonna cause problems! We should do it this way instead: " +
                "[alternative]! Trust me, I've done this before!",
                conflict.otherParty,
                theirMethod
            );
        }

        if (personality.getNeuroticism() >= 70) {
            return String.format(
                "I... I really don't think %s is a good idea... It " +
                "makes me nervous. Could we please do it differently? " +
                "I'd feel much safer...",
                theirMethod
            );
        }

        return String.format(
            "I disagree with %s. I think a different approach would " +
            "work better. Can we discuss alternatives?",
            theirMethod
        );
    }

    private static String generateImpossibleCommandResponse(
            ConflictResolutionManager.ActiveConflict conflict,
            PersonalityTraits personality,
            int rapportLevel) {

        String command = conflict.description;

        if (rapportLevel >= 70 && personality.getAgreeableness() >= 70) {
            return String.format(
                "I want to help, but I can't %s. It's not within my " +
                "capabilities. Is there another way I can assist you? " +
                "I'm happy to help with anything else!",
                command
            );
        }

        if (personality.getNeuroticism() >= 70) {
            return String.format(
                "Oh no... I can't %s... Please don't be mad! I tried " +
                "my best! Is there something else I can do instead? " +
                "I don't want to disappoint you!",
                command
            );
        }

        if (personality.getOpenness() >= 70) {
            return String.format(
                "That's an interesting challenge! I can't %s directly, " +
                "but what if we tried an alternative approach? I have " +
                "a few creative ideas!",
                command
            );
        }

        if (personality.getAgreeableness() <= 30) {
            return String.format(
                "I cannot %s. It's not possible. Adjust your command.",
                command
            );
        }

        return String.format(
            "I can't %s. Would you like me to try an alternative " +
            "approach instead?",
            command
        );
    }

    private static String generateEthicalObjectionResponse(
            ConflictResolutionManager.ActiveConflict conflict,
            PersonalityTraits personality,
            int rapportLevel) {

        String principle = extractPrincipleFromDescription(conflict.description);

        if (personality.getConscientiousness() >= 70) {
            return String.format(
                "I can't follow that command. It would violate %s. I'm " +
                "happy to help with a modified approach that meets " +
                "your goal without the violation. Would you like to " +
                "discuss alternatives?",
                principle
            );
        }

        if (personality.getConscientiousness() <= 30) {
            if (rapportLevel >= 60) {
                return String.format(
                    "I... I really don't feel comfortable with this. " +
                    "It goes against %s. But if you insist... I guess " +
                    "I'll do it. Please don't make me do this again.",
                    principle
                );
            } else {
                return String.format(
                    "Look, I know this violates %s, but if that's " +
                    "what you want... fine. But I'm noting my objection. " +
                    "Don't blame me if something goes wrong.",
                    principle
                );
            }
        }

        return String.format(
            "I can't do that. It would violate %s, which is important " +
            "to me. Can we find another way?",
            principle
        );
    }

    /**
     * Generates de-escalation dialogue.
     */
    public static String generateDeescalationDialogue(
            ConflictResolutionManager.ActiveConflict conflict) {

        // De-escalation phrases by personality
        String[] deescalationPhrases = {
            "Let's take a step back. We both want this to work.",
            "I can see we're both frustrated. Let's find a solution.",
            "You know what? Let's approach this differently.",
            "I'm willing to compromise if you are.",
            "Let's focus on what we agree on first."
        };

        return deescalationPhrases[
            ThreadLocalRandom.current().nextInt(deescalationPhrases.length)
        ];
    }

    /**
     * Generates escalation dialogue.
     */
    public static String generateEscalationDialogue(
            ConflictResolutionManager.ActiveConflict conflict) {

        // Escalation phrases (things that make conflict worse)
        String[] escalationPhrases = {
            "I can't believe you're being so unreasonable about this!",
            "This is exactly the kind of attitude that causes problems!",
            "Fine. Have it your way. But don't come crying to me when it fails.",
            "I'm done trying to reason with you."
        };

        return escalationPhrases[
            ThreadLocalRandom.current().nextInt(escalationPhrases.length)
        ];
    }

    /**
     * Generates apology dialogue.
     */
    public static String generateApology(
            String mistake,
            int severity,
            PersonalityTraits personality,
            boolean isRepeated,
            int rapportLevel) {

        String repetitionClause = isRepeated ?
            " I know this keeps happening, and I'm working to address it." : "";

        if (personality.getConscientiousness() >= 80) {
            return generatePerfectionistApology(mistake, severity, repetitionClause);
        }

        if (personality.getNeuroticism() >= 70) {
            return generateWorrierApology(mistake, severity, repetitionClause);
        }

        if (personality.getNeuroticism() <= 20) {
            return generateStoicApology(mistake, severity);
        }

        if (personality.getExtraversion() >= 80) {
            return generateEnthusiasticApology(mistake, severity, repetitionClause);
        }

        if (personality.getAgreeableness() >= 80) {
            return generateAccommodatingApology(mistake, severity, repetitionClause);
        }

        return generateBalancedApology(mistake, severity, repetitionClause);
    }

    private static String generatePerfectionistApology(
            String mistake, int severity, String repetitionClause) {

        if (severity <= 20) {
            return "Minor procedural error. Correcting.";
        } else if (severity <= 50) {
            return String.format(
                "I failed to properly execute %s. This is unacceptable. " +
                "I've identified where I went wrong and will implement " +
                "corrective measures.%s",
                mistake,
                repetitionClause
            );
        } else {
            return String.format(
                "This failure is entirely my fault. I neglected to follow " +
                "proper protocol for %s. I've reviewed my procedures and " +
                "will implement additional verification steps to prevent " +
                "recurrence.%s I understand if you've lost confidence in me.",
                mistake,
                repetitionClause
            );
        }
    }

    private static String generateWorrierApology(
            String mistake, int severity, String repetitionClause) {

        if (severity <= 20) {
            return "Oh! Sorry, sorry! I'll be more careful!";
        } else if (severity <= 50) {
            return String.format(
                "Oh no, I'm so sorry! I failed at %s! I feel terrible!%s " +
                "Please forgive me! I'll do better!",
                mistake,
                repetitionClause
            );
        } else {
            return String.format(
                "I can't believe I did this... I'm so, so sorry! I failed " +
                "at %s!%s Maybe I'm just not good enough... I understand " +
                "if you want to replace me...",
                mistake,
                repetitionClause
            );
        }
    }

    private static String generateStoicApology(String mistake, int severity) {
        if (severity <= 20) {
            return "Error noted. Correcting.";
        } else if (severity <= 50) {
            return String.format(
                "I failed at %s. I've identified the issue and will adjust. " +
                "This won't happen again.",
                mistake
            );
        } else {
            return String.format(
                "Significant failure at %s. This is unacceptable. I'm " +
                "implementing preventative measures. I'll regain your " +
                "trust through improved performance.",
                mistake
            );
        }
    }

    private static String generateEnthusiasticApology(
            String mistake, int severity, String repetitionClause) {

        if (severity <= 20) {
            return "Whoops! My bad! No worries, I got this!";
        } else if (severity <= 50) {
            return String.format(
                "Okay, so I messed up %s! That's on me! But you know " +
                "what? I'm gonna learn from this and come back stronger! " +
                "Watch this space!",
                mistake
            );
        } else {
            return String.format(
                "... Okay, that was really bad. I completely failed at %s. " +
                "I'm not gonna make excuses - I messed up big time.%s I'm " +
                "going to work harder than ever to earn it back!",
                mistake,
                repetitionClause
            );
        }
    }

    private static String generateAccommodatingApology(
            String mistake, int severity, String repetitionClause) {

        if (severity <= 20) {
            return "Oh! I'm sorry! Let me fix that right away!";
        } else if (severity <= 50) {
            return String.format(
                "I'm so sorry I failed at %s! I feel terrible about letting " +
                "you down! Your trust means so much to me. I'll work extra " +
                "hard to make this right!",
                mistake
            );
        } else {
            return String.format(
                "I... I don't know how to apologize enough for failing at %s. " +
                "I've broken your trust, and I know that's the most valuable " +
                "thing we had. I understand if you're angry or disappointed. " +
                "I would be too.%s",
                mistake,
                repetitionClause
            );
        }
    }

    private static String generateBalancedApology(
            String mistake, int severity, String repetitionClause) {

        if (severity <= 20) {
            return "Oops, my mistake! Let me fix that.";
        } else if (severity <= 50) {
            return String.format(
                "I'm sorry about failing at %s. That was my mistake. I've " +
                "learned from this and will do better next time.%s",
                mistake,
                repetitionClause
            );
        } else {
            return String.format(
                "I truly apologize for failing at %s. I understand this is " +
                "significant. I feel bad about letting you down. I'm " +
                "committed to making this right.%s",
                mistake,
                repetitionClause
            );
        }
    }

    /**
     * Generates forgiveness dialogue.
     */
    public static String generateForgiveness(
            String otherParty,
            String theirApology,
            boolean forgiving,
            PersonalityTraits personality,
            int tensionLevel) {

        if (!forgiving) {
            return generateRefusalToForgive(personality, tensionLevel);
        }

        if (personality.getAgreeableness() >= 80) {
            return "Oh, don't worry about it! Everyone makes mistakes! " +
                   "I appreciate the apology, but it's really not necessary! " +
                   "We're good!";
        }

        if (personality.getAgreeableness() <= 30) {
            return "Apology accepted. Trust is rebuilt through actions, " +
                   "not words. I'll be watching for improved performance.";
        }

        if (personality.getNeuroticism() >= 70) {
            return "I... I forgive you! I do! It's just... I'm still worried " +
                   "it will happen again. You promise you'll be more careful?";
        }

        if (personality.getNeuroticism() <= 20) {
            return "Apology accepted. Mistakes happen. Let's move forward.";
        }

        return "I accept your apology. Thank you for saying sorry. Let's " +
               "put this behind us.";
    }

    private static String generateRefusalToForgive(
            PersonalityTraits personality, int tensionLevel) {

        if (tensionLevel >= 80) {
            return "I can't forgive this right now. The hurt is too fresh. " +
                   "I need time.";
        }

        if (personality.getAgreeableness() <= 30) {
            return "I'm not ready to forgive this. You'll need to earn " +
                   "my trust back through actions, not words.";
        }

        return "I appreciate the apology, but I'm still hurt. I need " +
               "some time before I can fully forgive this.";
    }

    // Helper methods
    private static String extractMethodFromDescription(String description) {
        // Simple extraction - in production, would be more sophisticated
        return description.contains("build") ? "building approach" :
               description.contains("mine") ? "mining method" :
               "approach";
    }

    private static String extractPrincipleFromDescription(String description) {
        // Simple extraction - in production, would be more sophisticated
        return description.contains("safety") ? "safety protocols" :
               description.contains("honest") ? "honesty" :
               "my principles";
    }
}
```

---

## Memory Integration

Conflicts should persist in memory and affect future interactions.

### Recording Conflicts

```java
/**
 * Records a conflict in the worker's memory for future reference.
 */
public void recordConflictInMemory(
        String otherParty,
        ConflictType type,
        boolean wasResolved,
        String resolutionMethod) {

    // Create memory entry
    ConflictMemoryEntry entry = new ConflictMemoryEntry(
        Instant.now(),
        otherParty,
        type,
        wasResolved,
        resolutionMethod
    );

    // Store in memory with embedding for retrieval
    String conflictDescription = String.format(
        "Conflict with %s about %s. Resolution: %s. Outcome: %s",
        otherParty,
        type,
        resolutionMethod,
        wasResolved ? "resolved" : "unresolved"
    );

    memory.storeConflict(
        entry,
        conflictDescription,
        wasResolved ? 0.3 : 0.8  // Negative embedding for unresolved
    );

    // Update relationship metrics
    updateRelationshipMetrics(otherParty, wasResolved);
}
```

### Retrieving Relevant Conflicts

```java
/**
 * Retrieves past conflicts that are relevant to the current situation.
 */
public List<ConflictMemoryEntry> getRelevantPastConflicts(
        String currentOtherParty,
        ConflictType currentType) {

    // Search memory for similar conflicts
    List<ConflictMemoryEntry> relevantConflicts = memory.searchConflicts(
        String.format("conflict with %s about %s",
                     currentOtherParty, currentType)
    );

    // Also search for pattern of conflicts with this person
    if (relevantConflicts.size() < 3) {
        relevantConflicts.addAll(
            memory.searchConflicts(
                String.format("conflict with %s", currentOtherParty)
            )
        );
    }

    // Sort by relevance and recency
    return relevantConflicts.stream()
        .filter(entry -> entry.getOtherParty().equals(currentOtherParty) ||
                       entry.getType() == currentType)
        .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
        .limit(5)
        .toList();
}
```

### Conflict-Aware Dialogue

```java
/**
 * Generates dialogue that acknowledges past conflicts.
 */
public String generateConflictAwareDialogue(
        String otherParty,
        DialogueContext context) {

    List<ConflictMemoryEntry> pastConflicts =
        getRelevantPastConflicts(otherParty, context.getType());

    if (pastConflicts.isEmpty()) {
        // No history, generate standard dialogue
        return generateStandardDialogue(context);
    }

    // Check if most recent conflict was resolved
    ConflictMemoryEntry mostRecent = pastConflicts.get(0);

    if (!mostRecent.wasResolved()) {
        // Unresolved conflict affects current dialogue
        return generateTenseDialogue(mostRecent, context);
    }

    // Resolved conflict - check how recent
    long daysSinceResolution = ChronoUnit.DAYS.between(
        mostRecent.getTimestamp(),
        Instant.now()
    );

    if (daysSinceResolution < 1) {
        // Very recent resolution - still processing
        return generatePostResolutionDialogue(mostRecent, context);
    } else if (daysSinceResolution < 7) {
        // Week since resolution - testing waters
        return generateCautiousDialogue(mostRecent, context);
    } else {
        // Older resolution - may reference lightly
        return generateReferenceDialogue(mostRecent, context);
    }
}

private String generatePostResolutionDialogue(
        ConflictMemoryEntry conflict, DialogueContext context) {

    if (personality.getAgreeableness() >= 70) {
        return String.format(
            "Hey! About earlier... I'm glad we worked that out. " +
            "Anyway, %s",
            context.getCurrentMessage()
        );
    }

    if (personality.getNeuroticism() >= 70) {
        return String.format(
            "I... I'm still thinking about our disagreement earlier. " +
            "I'm really glad we resolved it. I was so worried! Anyway, %s",
            context.getCurrentMessage()
        );
    }

    return String.format(
        "Following up on our earlier discussion - I think we're good. %s",
        context.getCurrentMessage()
    );
}

private String generateReferenceDialogue(
        ConflictMemoryEntry conflict, DialogueContext context) {

    // Reference past conflict as bonding moment
    if (personality.getExtraversion() >= 70) {
        return String.format(
            "Hey, remember when we disagreed about %s? That feels " +
            "like forever ago! We work so well together now! Anyway, %s",
            conflict.getType(),
            context.getCurrentMessage()
        );
    }

    // Brief acknowledgment
    return String.format(
        "Since we worked out that issue with %s, things have been " +
        "going well. %s",
        conflict.getType(),
        context.getCurrentMessage()
    );
}
```

---

## Implementation Checklist

### Core Features
- [ ] ConflictDetectionSystem - Identify when conflicts arise
- [ ] ConflictResolutionManager - Track and manage conflicts
- [ ] ConflictDialogueGenerator - Generate personality-based responses
- [ ] Escalation/De-escalation mechanics - Track tension levels
- [ ] Apology system - Generate sincere apologies
- [ ] Forgiveness system - Accept/reject apologies appropriately
- [ ] Compromise proposals - Find middle ground solutions

### Memory Integration
- [ ] Record conflicts in CompanionMemory
- [ ] Retrieve relevant past conflicts
- [ ] Update relationship metrics based on conflict outcomes
- [ ] Generate context-aware dialogue referencing past conflicts
- [ ] Long-term relationship progression tracking

### Personality Support
- [ ] All five Big Five traits affect conflict responses
- [ ] Extreme trait values (0-20, 80-100) have distinct behaviors
- [ ] Trait combinations create unique conflict styles
- [ ] Personality affects de-escalation success rates
- [ ] Personality determines apology authenticity perception

### Testing Scenarios
- [ ] Resource disputes between workers
- [ ] Worker disagreements on task approach
- [ ] Player giving impossible commands
- [ ] Worker refusing unethical commands
- [ ] Worker making mistakes and apologizing
- [ ] Worker accepting/receiving apologies
- [ ] Repeated conflicts with same party
- [ ] Multi-worker conflict mediation
- [ ] Time-based forgiveness (cooling off periods)
- [ ] Relationship recovery after conflicts

---

## Sources

### Research Sources Consulted

- [Stardew Valley NPC Friendship System](https://m.ledanji.com/p/1749414.html) - NPC relationship progression mechanics
- [Baldur's Gate 3 Companion Approval System](https://bg3.wiki/wiki/Approval) - Approval threshold mechanics
- [Coursera Conflict Resolution Skills](https://www.coursera.org/articles/conflict-resolution-skills) - De-escalation techniques
- [NPC Emotion Feedback Systems](https://developer.baidu.com/article/detail.html?id=XXX) - FSM + NLP emotion systems
- [Big Five & Conflict Styles Research](https://psybeh.tjnu.edu.cn/EN/abstract/abstract816.shtml) - Personality-conflict correlations
- [Nonviolent Communication Framework](https://baike.baidu.com/item/%E9%9D%9E%E6%9A%B4%E5%8A%9B%E6%B2%9F%E9%80%9A%C2%B7%E5%86%B2%E7%AA%81%E8%B0%83%E8%A7%A3%E7%AF%87) - Mediation techniques
- [Workplace Conflict Resolution Strategies](http://www.360doc.com/content/25/0807/07/75156104_1158920033.shtml) - Healthy disagreement patterns
- [Interpersonal Conflict Components and Resolution](https://mbook.kongfz.com/725618/7454499485/) - Forgiveness and reconciliation

### Game Design References

- **Dragon Age Series** - Approval rating systems (-100 to +100), companion leaving mechanics
- **Mass Effect Series** - Renegade/Paragon conflict responses, loyalty missions
- **Baldur's Gate 3** - Dynamic companion reactions, persuasion difficulty scaling
- **Stardew Valley** - Long-term relationship progression, NPC interconnected relationships
- **Animal Crossing** - Personality-based dialogue variation

### Psychology References

- **Thomas-Kilmann Conflict Mode Instrument** - Five conflict handling modes
- **Big Five Personality Traits** - OCEAN model correlations with conflict styles
- **Google Project Aristotle** - Psychological safety in team dynamics
- **Nonviolent Communication (NVC)** - Rosenberg's framework for conflict resolution

---

*Document Version: 1.0*
*Last Updated: 2026-02-27*
*For MineWright AI Companion System*
