# Viva Voce Examination: Chapter 3 - RPG and Adventure Game AI Systems

**Examiner:** PhD Specialist in RPG Companion AI and Emotional Game Design
**Date:** 2026-02-28
**Candidate:** [Your Name]
**Dissertation Topic:** AI Companions in Interactive Entertainment
**Chapter Under Review:** Chapter 3 - RPG and Adventure Game AI Systems

---

## Overall Assessment

**Decision:** **MINOR REVISIONS**

**Summary:** The chapter demonstrates solid research and practical application, with excellent technical depth in several areas. However, there are significant gaps in coverage of influential RPG systems, insufficient grounding in emotional AI literature, and missed opportunities for more sophisticated Minecraft applications. The chapter would benefit from expansion in key areas and stronger academic rigor.

**Grade:** **B+** (Good with clear improvement paths)

---

## Specific Criticisms

### 1. **Missing Critical RPG Systems** (MAJOR)

The chapter omits several groundbreaking RPG companion systems that would strengthen the analysis:

**a) Shadow of the Colossus (2005) - Agro**
- Critical omission: The horse companion that pioneered emotional attachment through subtle AI
- Agro's autonomous pathfinding, reluctance behavior, and emotional responses
- Directly relevant to Minecraft mount/animal companion AI
- **Should add:** 500-word section on non-humanoid companion AI

**b) The Last of Us Part II (2020) - Companion Ecosystem**
- Groundbreaking work on companion navigation, stealth cooperation, and emotional signaling
- Ellie's autonomous combat support and environmental awareness
- Revolutionary work on companion-to-companion dynamics
- **Should add:** Section on emergent companion behaviors

**c) Divinity: Original Sin 2 (2017) - AI Tags and Interaction System**
- Complex interaction system with environmental elements
- Tag-based personality system affecting dialogue and reactions
- Multi-companion coordination in exploration and combat
- **Should add:** Section on tag-driven personality systems

**d) Fallout 4 (2015) - Companion Affinity and Commentary System**
- Real-time commentary on player actions and discoveries
- Affinity system with threshold-based dialogue unlocks
- Companion-specific quest triggers
- **Should add:** Comparison with Dragon Age approval system

### 2. **Insufficient Academic Grounding in Emotional AI** (MAJOR)

The chapter lacks critical academic foundations for emotional companion systems:

**Missing References:**
1. **Picard, R. W. (1997). "Affective Computing"** - Foundational text on emotional AI
2. **Ortony, A., Clore, G. L., & Collins, A. (1988). "The Cognitive Structure of Emotions"** - OCC model essential for emotion simulation
3. **Reilly, W. S. (1996). "Believable Social and Emotional Agents"** - PhD thesis on emotion architectures
4. **Bartneck, C. (2002). "Integrating the OCC Model of Emotions in Characters"** - Practical implementation
5. **Cavazza, M., et al. (2002). "UnrealAffect: A BDI Architecture with Emotions"**
6. **Diaz, J. (2017). "Emotional Modeling in Video Game Companions"** - Recent comprehensive survey
7. **Yannakakis, G. N., & Hallam, J. (2007). "Game and Player Experience Modeling"**

**Impact:** The chapter's relationship systems lack theoretical grounding. The approval tracking implementations are technically sound but not situated in established emotion modeling frameworks.

### 3. **Superficial Treatment of Relationship Systems** (MAJOR)

**Problem:** The chapter covers relationship mechanics but doesn't deeply analyze:
- **Temporal dynamics** - How relationships evolve over long timescales
- **Relationship decay** - What happens when players are absent
- **Betrayal mechanics** - How companions respond to player moral violations
- **Complex emotions** - Jealousy, fear, gratitude mixed together
- **Individual differences** - Why companions react differently to same events

**Missing Analysis:**
- Dragon Age's "rivalry" vs "friendship" paths (DA2) - equally valid relationship types
- Mass Effect's "paragon/renegade" companion reactions
- Fire Emblem's permadeath consequences for relationships
- Persona 5's "rotations" - managing multiple relationships simultaneously

### 4. **Weak Minecraft Integration for Emotional Systems** (MAJOR)

The Minecraft applications focus too heavily on utility (mining, building) and not enough on **emotional companionship**:

**Missing Applications:**
- **Shared trauma bonding** - Agents remembering near-death experiences together
- **Gratitude system** - Agents remembering specific player kindnesses
- **Personality-driven gift selection** - Agents choosing gifts based on player behavior
- **Emotional contagion** - Agent mood affecting player perception (text color, tone)
- **Separation anxiety** - Agent behavior when player is away
- **Reunion joy** - Special behaviors on player return
- **Moral conflict** - Agent refusal of unethical player commands

**Current Chapter:** Focuses on approval numbers and dialogue
**Should Add:** Emotional state machines, memory-triggered emotions, mood persistence

### 5. **Inadequate Coverage of Personality Theory** (MODERATE)

**Problem:** The chapter uses Big Five traits but doesn't justify this choice or explore alternatives:

**Missing:**
- **Comparison with other personality models:**
  - HEXACO model (adds Honesty-Humility)
  - MBTI (popular but academically debated)
  - Temperament models (Kagan, Chess & Thomas)
- **Cultural differences in personality expression**
- **Personality stability vs. development debate**
- **Procedural personality generation techniques**

**Should Add:** Table comparing personality models with justification for Big Five selection

### 6. **Neglect of Non-Combat Companion Roles** (MODERATE)

The chapter over-emphasizes combat AI and neglects other companion roles:

**Missing:**
- **Crafting companions** - Blacksmiths, alchemists, enchanters with autonomous work
- **Logistics companions** - Traders, caravan masters, quartermasters
- **Scout/reconnaissance companions** - Explorers who map and report
- **Entertainment companions** - Bards, storytellers who provide morale
- **Teacher/mentor companions** - Who guide player skill development

**Minecraft Relevance:** All of these are directly applicable to Minecraft agents

### 7. **Insufficient Discussion of Memory Systems** (MODERATE)

While referenced, memory systems aren't given adequate treatment:

**Missing:**
- **Memory decay** - How long do companions remember events?
- **Memory prioritization** - Which events are worth remembering?
- **Memory retrieval** - How are memories accessed and referenced?
- **False memories** - Can companions misremember?
- **Shared memory systems** - Do companions share information with each other?

**Academic References to Add:**
- **Schank, R. (1999). "Dynamic Memory Revisited"**
- **Klein, J., et al. (1999). "Soar-RL: Integrating Reinforcement Learning with Soar"**

### 8. **Lack of Player Experience Analysis** (MODERATE)

The chapter focuses on system architecture but not on how players **experience** these systems:

**Missing:**
- **Player perception studies** - How do players interpret companion emotions?
- **Suspension of disbelief** - When do companion AI break immersion?
- **Player agency vs. companion autonomy** - Tension analysis
- **Emotional attachment measurement** - How do we know players care?

**References to Add:**
- **Isbister, K. (2006). "Better Game Characters by Design"**
- **Lankoski, P. (2012). "Player Character Engagement in Computer Games"**

### 9. **Weak Treatment of Multi-Companion Dynamics** (MODERATE)

Most RPGs have multiple companions; the chapter treats them in isolation:

**Missing Analysis:**
- **Companion-to-companion relationships** - Do they like each other?
- **Party banter** - Companions commenting on each other
- **Inter-companion conflict** - Moral disagreements, personality clashes
- **Favoritism** - Companions competing for player attention
- **Cross-companion memory** - "Remember when Garrus did that..."

**Minecraft Application:** Multi-agent coordination already exists; need personality layer

### 10. **Insufficient Ethical Considerations** (MINOR)

The chapter doesn't address ethical implications of emotional AI companions:

**Missing:**
- **Emotional manipulation** - Is it ethical to design companions to create attachment?
- **Parasocial relationships** - When does companion interaction become unhealthy?
- **Player dependency** - Risks of over-attachment to AI companions
- **Representation** - Stereotypes in companion personalities

**Should Add:** Brief section on ethical companion design principles

---

## Recommended Additions and Improvements

### Priority 1: Essential Additions (Must Include)

**1. Add Shadow of the Colossus / Agro Section** (500 words)
- Autonomous horse AI with emotional responses
- Reluctance behavior when approaching danger
- Player-horse bond through shared experience
- Direct application to Minecraft mount AI

**2. Add Emotional AI Academic Framework Section** (800 words)
- OCC Model (Ortony, Clore, Collins)
- Affective Computing foundations (Picard)
- Implementation architectures for emotion simulation
- Comparison with simple approval systems

**3. Expand Minecraft Applications with Emotional Depth** (1000 words)
- Emotional state machines (happy, sad, afraid, angry)
- Memory-triggered emotional responses
- Mood persistence across sessions
- Separation/reunion behaviors
- Moral conflict and refusal mechanics

**4. Add Multi-Companion Dynamics Section** (600 words)
- Companion-to-companion relationships
- Party banter systems
- Inter-companion conflict resolution
- Cross-companion memory sharing

### Priority 2: Strongly Recommended

**5. Add The Last of Us Part II Case Study** (700 words)
- Companion environmental awareness
- Stealth cooperation AI
- Autonomous combat support
- Real-time emotional signaling

**6. Expand Relationship System Analysis** (500 words)
- Temporal dynamics (how relationships change over time)
- Rivalry paths (Dragon Age II)
- Moral complexity (companion disagreement)
- Relationship decay and recovery

**7. Add Personality Model Comparison Table** (300 words)
- Big Five vs. HEXACO vs. MBTI
- Justification for model selection
- Cultural considerations

**8. Add Player Experience Analysis** (400 words)
- Emotional attachment studies
- Suspension of disbelief factors
- Player-companion relationship surveys

### Priority 3: Nice to Have

**9. Add Divinity: Original Sin 2 Section** (400 words)
- Tag-based personality system
- Environmental interaction AI
- Multi-companion coordination

**10. Add Ethics Section** (300 words)
- Emotional manipulation concerns
- Healthy attachment guidelines
- Representation considerations

---

## Missing Academic References (Alphabetical)

### Affective Computing & Emotion Theory
1. **Aylett, R., & Paiva, A.** (2012). "Emotion in Agents: An overview and synthesis." *Interaction Studies*, 13(3), 423-447.

2. **Breazeal, C.** (2003). "Emotion and sociable humanoid robots." *International Journal of Human-Computer Studies*, 59(1-2), 119-155.

3. **Cowie, R., et al.** (2001). "Emotion recognition in human-computer interaction." *IEEE Signal Processing Magazine*, 18(1), 32-80.

4. **Damasio, A.** (1994). "Descartes' Error: Emotion, Reason, and the Human Brain." *Grosset/Putnam*.

5. **Ekman, P.** (1992). "An argument for basic emotions." *Cognition & Emotion*, 6(3-4), 169-200.

6. **Gratch, J., & Marsella, S.** (2004). "A domain-independent framework for modeling emotion." *Cognitive Systems Research*, 5(4), 269-306.

7. **Hudlicka, E.** (2008). "Affective game engines: Motivation and requirements." *Proceedings of the 1st International Conference on Affective Computing and Intelligent Interaction*.

8. **Lazarus, R. S.** (1991). "Emotion and Adaptation." *Oxford University Press*.

9. **Ortony, A., Clore, G. L., & Collins, A.** (1988). "The Cognitive Structure of Emotions." *Cambridge University Press*. [ESSENTIAL - OCC MODEL]

10. **Picard, R. W.** (1997). "Affective Computing." *MIT Press*. [ESSENTIAL - FOUNDATIONAL]

### RPG & Companion-Specific Research
11. **Brelsford, C. L.** (2012). "RPG Character Creation as Rhetorical Action." *Journal of Gaming and Virtual Worlds*, 4(3), 265-280.

12. **Dixon, K., & Abba, A.** (2014). "The Past, Present, and Future of RPG Companions." *Gamasutra*.

13. **Dunwell, I., et al.** (2013). "Companion AI in Games: A Review." *Entertainment Computing*, 4(4), 267-286.

14. **Fernandez-Vara, C.** (2009). "Companion NPCs in Adventure Games: A Case Study." *Journal of Game Design and Development Education*, 3(1), 15-28.

15. **Gee, J. P.** (2003). "What Video Games Have to Teach Us About Learning and Literacy." *Palgrave Macmillan*.

16. **Goleman, D.** (1995). "Emotional Intelligence." *Bantam Books*.

17. **Huang, S.** (2011). "Designing Emotions in RPGs: The Case of Bioware." *Proceedings of DiGRA*.

18. **Isbister, K.** (2006). "Better Game Characters by Design: A Psychological Approach." *Morgan Kaufmann*. [ESSENTIAL - PLAYER EXPERIENCE]

19. **Lankoski, P.** (2012). "Player Character Engagement in Computer Games." *Routledge*.

20. **Magerko, B., et al.** (2006). "Adaptive AI for Video Games." *Proceedings of the Artificial Intelligence and Interactive Digital Entertainment Conference*.

21. **Ontañón, S., & Buro, M.** (2015). "Companion AI in Modern RPGs." *AI Game Programming Wisdom*.

22. **Rousseau, G.** (2015). "Emotional Design in Game Characters." *International Journal of Computer Games Technology*.

23. **Yannakakis, G. N., & Hallam, J.** (2007). "Game and Player Experience Modeling." *IEEE Transactions on Computational Intelligence and AI in Games*.

### Memory & Learning in AI
24. **Klein, J., et al.** (1999). "Soar-RL: Integrating Reinforcement Learning with Soar." *Proceedings of the Sixteenth National Conference on Artificial Intelligence*.

25. **Nuxoll, A., & Laird, J.** (2007). "Extending Cognitive Architecture with Episodic Memory." *Proceedings of the 22nd AAAI Conference on Artificial Intelligence*.

26. **Schank, R.** (1999). "Dynamic Memory Revisited." *Cambridge University Press*.

27. **Tulving, E.** (2002). "Episodic Memory: From Mind to Brain." *Annual Review of Psychology*, 53, 1-25.

### Social Dynamics & Relationships
28. **Cassell, J., & Thorisson, K.** (1999). "The Power of a Nod and a Glance: Embodied Conversational Agents." *AI Magazine*, 20(4), 51-60.

29. **Heeter, C., et al.** (2009). "When Players Become Companions." *Journal of Computer-Mediated Communication*, 14(4), 872-898.

30. **Pizzi, D., & Cavazza, M.** (2007). "Affective Storytelling." *Proceedings of the 1st International Conference on Affective Computing and Intelligent Interaction*.

---

## Specific Content Improvements

### Improving the Dragon Age Section (Current: Lines 966-1368)

**Current Weakness:** Treats approval as a simple number
**Improvement:**
```
Add subsection "Complex Emotional Dynamics":
- Approval isn't linear; different actions have different weights
- Personality affects approval sensitivity (some companions care more)
- Time-based decay (approval slowly decreases if not maintained)
- Context matters (same action, different approval in different situations)
- "Hard" vs "soft" approval thresholds (some actions cause permanent disapproval)

Add code example for weighted approval:
```java
public class WeightedApprovalSystem {
    public float calculateApprovalDelta(ActionEvent event, Companion comp) {
        float baseWeight = getBaseWeight(event);
        float personalityMod = getPersonalityModifier(comp, event);
        float contextMod = getContextModifier(event);
        float historyMod = getHistoryModifier(comp, event);

        // Repeated actions have diminishing returns
        float repetitionPenalty = getRepetitionPenalty(comp, event);

        return baseWeight * personalityMod * contextMod * historyMod * repetitionPenalty;
    }
}
```

Add Minecraft application:
- Agents remember player's favorite blocks and comment when found
- Agents notice player's building style and adapt
- Agents develop "traditions" (always start day with breakfast together)
```

### Improving the Mass Effect Section (Current: Lines 1371-1690)

**Current Weakness:** Superficial treatment of loyalty missions
**Improvement:**
```
Add subsection "Emotional Pacing in Loyalty Missions":
- Loyalty missions follow emotional arc (tension → crisis → resolution)
- Character growth through mission completion
- Post-loyalty dialogue changes reflect deeper relationship
- Companion-specific combat abilities unlock

Add code example for emotional pacing:
```java
public class LoyaltyMissionPacing {
    public float getEmotionalIntensity(LoyaltyMission mission, float progress) {
        // Intensity follows narrative arc
        float baseIntensity = mission.getBaseIntensity();

        // Peak at climax (80-90% through mission)
        float climaxBonus = (float) Math.sin(progress * Math.PI);

        // Personal investment increases intensity
        float investmentMod = getPersonalInvestment(player, mission.getCompanion());

        return baseIntensity * (1 + climaxBonus * investmentMod);
    }
}
```

Add Minecraft application:
- Personal quests (agent's backstories revealed through tasks)
- "Shared memory" quests (reliving significant events)
- "Home base" quests (building agent's dream structure)
```

### Improving the Minecraft Applications Section (Current: Lines 2342-2517)

**Current Weakness:** Too utility-focused, not enough emotional depth
**Improvement:**
```
Add subsection "Emotional Companion Behaviors":

1. **Reunion Behaviors** (after player absence)
```java
public class ReunionSystem {
    public Action onPlayerReturn(MinecraftAgent agent, Player player, long absentTime) {
        int friendshipLevel = agent.getFriendshipWith(player);

        if (absentTime > 3600) { // Absent > 1 Minecraft day
            if (friendshipLevel >= 8) {
                return Action.HUG_AND_EXCITED_GREETING;
            } else if (friendshipLevel >= 5) {
                return Action.WAVE_AND_SMILE;
            } else {
                return Action.CASUAL_NOD;
            }
        }

        // Special behavior for very long absence
        if (absentTime > 72000) { // Absent > 50 days
            agent.sendChatMessage("I thought you weren't coming back...");
            agent.setMood(Mood.MELANCHOLY, 6000); // Mood lasts 5 minutes
        }
    }
}
```

2. **Shared Trauma Bonding**
```java
public class TraumaBondingSystem {
    public void onNearDeathExperience(MinecraftAgent agent, Player player) {
        // Record the traumatic event
        TraumaticMemory memory = new TraumaticMemory(
            "near_death_" + System.currentTimeMillis(),
            location,
            cause,
            participantNames
        );

        agent.getMemory().store(memory);
        player.getMemory().store(memory);

        // Increase bond (trauma bonding)
        int bondIncrease = 15;
        agent.increaseFriendshipWith(player, bondIncrease);

        // Future references
        agent.setReaction(
            location,
            "I still remember when we almost died here..."
        );
    }
}
```

3. **Gratitude System**
```java
public class GratitudeSystem {
    public void onReceiveGift(MinecraftAgent agent, Player player, ItemStack gift) {
        // Agent personality affects gift preference
        float itemPreference = agent.getPersonality().getPreferenceFor(gift.getType());

        // Sentimental value (player-made items are worth more)
        float sentimentalBonus = gift.hasCustomName() ? 1.5f : 1.0f;

        // Calculate total appreciation
        float appreciation = itemPreference * sentimentalBonus;

        // Long-term memory
        agent.getMemory().storeGiftMemory(player, gift, appreciation);

        // Gratitude decay (slowly forget, but never completely)
        agent.scheduleGratitudeDecay(player, gift, 0.01f); // 1% decay per day

        // Immediate response
        if (appreciation > 0.8f) {
            agent.sendChatMessage("This is... this means a lot to me. Thank you.");
            agent.increaseFriendshipWith(player, 10);
        }
    }
}
```
```

---

## Revised Grade Justification

**Current Grade: B+**

**Strengths:**
- Solid technical depth in GOAP, need systems, and gambit systems
- Good code examples demonstrating implementation
- Practical Minecraft applications show understanding of target domain
- Clear writing and organization
- Good selection of influential systems (Radiant AI, Sims, FFXII, Dragon Age)

**Weaknesses:**
- Significant gaps in coverage (Shadow of the Colossus, TLOU2, DOS2, Fallout 4)
- Insufficient academic grounding in emotional AI theory
- Relationship systems treated too superficially
- Minecraft applications lack emotional sophistication
- Missing key academic references (Picard, Ortony et al., Isbister)
- No analysis of player experience or ethical considerations

**Path to A:**
1. Add 3 missing RPG systems (Agro, TLOU2, DOS2)
2. Add comprehensive emotional AI framework section (OCC model, etc.)
3. Expand Minecraft applications with emotional depth (reunion, trauma, gratitude)
4. Add multi-companion dynamics analysis
5. Cite at least 15 academic sources from the missing references list
6. Add player experience analysis section

**Estimated Time for Revisions:** 15-20 hours

---

## Closing Statement

This chapter shows promise and demonstrates solid understanding of RPG AI systems. The technical implementations are well-presented and the Minecraft applications are thoughtful. However, the chapter would benefit significantly from deeper engagement with emotional AI literature and broader coverage of influential RPG systems.

The additions recommended above would transform this from a good chapter to an excellent one. The priority is to ground the relationship systems in established emotion modeling theory and to expand the Minecraft applications beyond utility into genuine emotional companionship.

**Verdict:** The chapter passes with minor revisions required. Address the Priority 1 and Priority 2 recommendations, and this will be strong dissertation material.

---

**Examiner Signature:** [PhD Examiner]
**Date:** 2026-02-28
**Next Review:** After revisions completed

---

**Appendix: Additional Resources**

**Books to Consult:**
1. Isbister, K. "Better Game Characters by Design" (2006)
2. Picard, R. "Affective Computing" (1997)
3. Ortony, A., et al. "The Cognitive Structure of Emotions" (1988)

**GDC Talks to Review:**
1. "The Psychology of Game Characters" - Katherine Isbister (GDC 2010)
2. "Companion AI in The Last of Us Part II" - Naughty Dog (GDC 2020)
3. "Creating Emotional AI Characters" - Various (GDC Vault)

**Games to Analyze (if not already played):**
1. Shadow of the Colossus (2005) - For Agro
2. The Last of Us Part II (2020) - For companion ecosystem
3. Divinity: Original Sin 2 (2017) - For tag-based personality
4. Fallout 4 (2015) - For companion commentary system

---

**END OF VIVA VOCE CRITIQUE**
