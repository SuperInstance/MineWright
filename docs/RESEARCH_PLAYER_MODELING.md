# Player Modeling AI for Game Agents - Research Report

**Date:** 2026-02-27
**Project:** MineWright AI - Minecraft Agent System
**Focus:** Player modeling techniques for adaptive AI agents in sandbox games

---

## Executive Summary

This report researches player modeling AI techniques for game agents, with specific focus on applications to the MineWright AI project (Minecraft autonomous agents). Player modeling enables AI systems to understand, predict, and adapt to individual player behaviors, preferences, and play styles. The research covers five core areas:

1. **Behavior Prediction** - Anticipating player actions using machine learning
2. **Preference Learning** - Understanding player goals and motivations
3. **Play Style Adaptation** - Adapting agent behavior to player style
4. **Difficulty Adjustment** - Dynamic difficulty based on player modeling
5. **Personalization** - Creating customized gameplay experiences

---

## Table of Contents

1. [Player Modeling Techniques](#1-player-modeling-techniques)
2. [Behavior Prediction](#2-behavior-prediction)
3. [Preference Learning](#3-preference-learning)
4. [Play Style Adaptation](#4-play-style-adaptation)
5. [Dynamic Difficulty Adjustment](#5-dynamic-difficulty-adjustment)
6. [Personalization Systems](#6-personalization-systems)
7. [Implementation Approaches](#7-implementation-approaches)
8. [MineWright Applications](#8-minewright-applications)
9. [Research Sources](#9-research-sources)

---

## 1. Player Modeling Techniques

### 1.1 Markov Decision Processes (MDPs)

MDPs provide a formal framework for modeling player decisions in games:

**Core Elements:**
- **State Space (S)**: Player position, health, inventory, game progress
- **Action Space (A)**: Movement, combat, crafting, building actions
- **Transition Probability P(s'|s,a)**: Likelihood of state transitions
- **Reward Function R(s,a)**: Player goals and motivations

**Advanced Variants:**
- **Multi-Agent MDPs (MMDPs)**: Centralized strategy for multi-player scenarios
- **Decentralized MDPs (Dec-MDPs)**: Independent agent observations
- **Interactive POMDP (I-POMDP)**: Recursive reasoning for opponent modeling
- **Partially Observable Stochastic Games (POSG)**: General multi-agent framework

### 1.2 Inverse Reinforcement Learning (IRL)

IRL learns player reward functions by observing behavior:

**Key Concepts:**
- Infers player motivations from actions (the "why" behind decisions)
- Handles heterogeneous players with different objectives
- Enables imitation learning for human-like AI behavior
- Supports deceptive behavior recognition in competitive scenarios

**Applications:**
- Understanding player goals without explicit specification
- Cloning expert player behavior
- Modeling deceptive strategies in competitive games
- Multi-agent reward function learning

### 1.3 Transformer-Based Models

Transformers excel at modeling sequential player behavior:

**Architecture Approaches:**
- **Transformer Encoders**: Sequence modeling for action prediction
- **LSTM + Transformer Hybrids**: Combining temporal dependencies with attention
- **Transformer + Diffusion**: Synthetic player generation and behavior cloning
- **VQGAN + Transformer**: Multi-modal video and action prediction

**Microsoft WHAM Example:**
- 1.6 billion parameter Transformer model
- Processes 1-second game footage (~10 frames) + controller inputs
- Uses VQGAN for image compression (300x180 → 540 tokens)
- Achieves consistency, diversity, and persistency in predictions

### 1.4 Clustering and Behavioral Typing

Grouping players into behavioral categories:

**Techniques:**
- **K-means Clustering**: Grouping players by action patterns
- **Density Clustering**: Identifying behavioral outliers (anti-cheat)
- **Hidden Markov Models**: Modeling individual player differences
- **N-gram Models**: Statistical behavior prediction

**Player Typing:**
- Casual vs. Hardcore gamers
- Aggressive vs. Conservative players
- Explorers vs. Achievers vs. Socializers
- Role-based preferences (builder, fighter, crafter)

---

## 2. Behavior Prediction

### 2.1 Real-Time Prediction Models

**Current State (2024-2025):**
- AI models predict player actions instantly during gameplay
- Multi-modal approaches combine vision, controller inputs, and game state
- Self-supervised learning on behavior sequences produces useful embeddings

**Prediction Targets:**
- Next action prediction (short-term)
- Win-loss outcome prediction (long-term)
- Churn prediction (player retention)
- Movement path prediction

**Case Study: MOBA Win-Loss Prediction**
- CNNs for feature extraction + RNNs for sequence modeling
- Achieved 87.85% prediction accuracy
- Models player behavior variation in combat sequences over time

**Case Study: VALORANT Round Prediction**
- LSTM and Transformer Encoder models
- Uses player health bars as input
- Predicts round outcomes in real-time

### 2.2 Sequence Modeling Approaches

**Techniques:**
- **Temporal Analysis**: Analyzing action sequences over time
- **Contextual Modeling**: Incorporating game state and environment
- **Multi-modal Fusion**: Combining different data sources
- **Attention Mechanisms**: Focusing on relevant historical actions

**Data Representation:**
- "Where-what-how-when" representation: (map_id, action_id, detail_id, timestamp)
- Embedding-based behavioral vectors
- Time-series feature extraction

### 2.3 Minecraft-Specific Prediction

**Relevant Behaviors:**
- Mining target prediction (ore type, location)
- Building pattern recognition
- Crafting sequence anticipation
- Exploration path modeling

**Multi-Agent Coordination:**
- Collaborative building prediction
- Resource sharing anticipation
- Division of labor inference
- Communication intent modeling

---

## 3. Preference Learning

### 3.1 Preference Elicitation Methods

**Explicit Methods:**
- Questionnaire surveys (game type interests)
- Interview methods (one-on-one discussions)
- Focus groups (group discussions about preferences)
- Direct preference specification

**Implicit Methods:**
- Observation methods (behavior while playing)
- Behavioral clustering (inferring from actions)
- Choice feedback (pairwise/set comparisons)
- Gameplay pattern analysis

### 3.2 User Modeling Approaches

**Techniques:**
- **Soft Constraints**: Flexible preference modeling
- **Case-Based Reasoning**: Learning from similar players
- **Utility Functions**: Quantifying preference strength
- **Constructive Elicitation**: Synthesizing new configurations
- **Robust Ordinal Regression**: Preference ranking

**Two-Stage Incremental Elicitation:**
1. Initial profile creation from early gameplay
2. Incremental refinement through ongoing interaction

### 3.3 Preference Representation

**Player Typing:**
- **Bartle's Player Types**: Achiever, Explorer, Socializer, Killer
- **MBTI-Based**: Personality-informed preferences
- **Motivation-Based**: Intrinsic vs. extrinsic motivation
- **Skill-Based**: Novice to expert progression

**Preference Dimensions:**
- Risk tolerance (aggressive vs. conservative)
- Exploration tendency (known vs. unknown areas)
- Social preference (solo vs. collaborative)
- Building style (functional vs. aesthetic)
- Combat preference (direct vs. tactical)

### 3.4 Dynamic Preference Modeling

**Challenges:**
- Player preferences change over time
- Context-dependent preferences
- Evolution of play style
- Fatigue and engagement shifts

**Adaptive Approaches:**
- Real-time preference updates
- Sliding window analysis
- Trend detection in behavior
- Session-by-session recognition

---

## 4. Play Style Adaptation

### 4.1 Style Recognition

**Real-Time Style Detection:**
- Automatic recognition of changing play styles
- Session-by-session customization
- Dynamic style clustering
- Adaptive AI responses

**Style Categories:**
- **Speed**: Rusher vs. methodical player
- **Exploration**: Thorough vs. goal-oriented
- **Combat**: Aggressive vs. defensive vs. tactical
- **Building**: Simple vs. complex, functional vs. decorative
- **Social**: Leader vs. follower vs. solo

**Recognition Techniques:**
- Behavioral feature extraction
- Time-series analysis
- Pattern recognition in action sequences
- Clustering on behavioral embeddings

### 4.2 Adaptive AI Responses

**Adaptation Strategies:**
- **Mirroring**: Match player's play style
- **Complementing**: Provide what player lacks
- **Teaching**: Guide toward new styles
- **Challenging**: Counter player's approach

**Implementation:**
- Real-time parameter adjustment
- Behavior tree modifications
- Goal reprioritization
- Communication style adaptation

### 4.3 Case Studies

**Educational Games:**
- Dynamic play style extraction framework
- Customizes content session-by-session
- Recognizes style changes in real-time

**FPS Games:**
- Adaptive agents using deep reinforcement learning
- Substantially outperforms built-in AI
- Adapts to player skill and tactics

**Minecraft Multi-Agent:**
- Work partitioning based on player style
- Complementary role assignment
- Dynamic rebalancing when agents finish early

---

## 5. Dynamic Difficulty Adjustment

### 5.1 Player Modeling for Difficulty

**Data Sources:**
- **Interaction Data**: Actions taken during gameplay
- **Subjective Data**: Survey-based preferences
- **Biometric Data**: Stress, engagement levels (optional)
- **Performance Metrics**: Success rates, completion times

**Modeling Components:**
- **Behavioral Analysis**: Tracking actions and tendencies
- **Skill Assessment**: Evaluating ability and learning curve
- **Preference Prediction**: Understanding motivations

### 5.2 Difficulty Adjustment Techniques

**Approaches:**
- **Real-time Scaling**: Adjust AI behavior instantly
- **Level-based**: Modify game parameters between sessions
- **Hybrid**: Combine real-time and level-based

**Adjustable Parameters:**
- Enemy AI intelligence
- Resource availability
- Time limits/pressure
- Task complexity
- Success thresholds

**Algorithms:**
- Reinforcement learning for adaptive difficulty
- Evolutionary algorithms for AI tuning
- Predictive modeling for anticipation
- Clustering-based player matching

### 5.3 Applications in Games

**Examples:**
- **Left 4 Dead**: AI Director adjusts zombie count based on player stress
- **Resident Evil 4**: Dynamic difficulty based on deaths and retries
- **Mobile Games**: A/B testing with AI for balance optimization
- **MOBA Games**: Matchmaking based on skill modeling

**Benefits:**
- Avoid frustration (too hard)
- Avoid boredom (too easy)
- Maintain flow state
- Increase engagement
- Improve retention

### 5.4 Challenges

**Technical Challenges:**
- Real-time computation requirements
- Balancing smoothness vs. responsiveness
- Preventing exploitation
- Handling edge cases

**Design Challenges:**
- Maintaining game integrity
- Avoiding obvious manipulation
- Preserving intended challenges
- Respecting player autonomy

---

## 6. Personalization Systems

### 6.1 Content Personalization

**Types of Personalization:**
- **Game Rules**: Modify mechanics for player style
- **Content Generation**: Create custom content
- **UI/UX**: Adaptive interfaces
- **Narrative**: Branching storylines
- **Recommendations**: Netflix-style content suggestions

**Techniques:**
- Collaborative filtering (similar players)
- Content-based filtering (similar items)
- Hybrid approaches
- Reinforcement learning for optimization

### 6.2 Player Profiling

**Profile Components:**
- Demographic information
- Gameplay statistics
- Behavioral patterns
- Preference indicators
- Social connections

**Profile Uses:**
- Matchmaking
- Content recommendation
- Difficulty adjustment
- Social features
- Monetization optimization

### 6.3 Adaptive Content Generation

**Procedural Generation:**
- **Level Design**: Adapt difficulty and layout
- **Quest Generation**: Match player interests
- **Item Distribution**: Align with play style
- **NPC Behavior**: Reflect player preferences

**AI-Driven Generation:**
- GANs for content creation
- LLMs for narrative generation
- Reinforcement learning for optimization
- Player modeling for constraints

### 6.4 Multi-Agent Personalization

**Collaborative Personalization:**
- Agents coordinate around player style
- Division of labor based on preferences
- Complementary role assignment
- Adaptive communication style

**Case Study: MineLand**
- Forces agent communication via limited senses
- Agents have physical needs requiring collaboration
- Supports up to 48 agents with multitasking coordination
- Evaluates cooking, building, and crafting tasks

**Case Study: Project Sid**
- 1,000 AI agents in Minecraft
- Emergent social policies and professions
- Organized religion emergence
- Runs autonomously for days

---

## 7. Implementation Approaches

### 7.1 Data Collection

**Required Data:**
- **Action Sequences**: Timestamped player actions
- **Game State**: Context for each action
- **Outcomes**: Results of actions
- **Session Metadata**: Duration, frequency, patterns
- **Social Data**: Interactions with other players

**Collection Methods:**
- Event logging system
- Telemetry framework
- Screen recording (optional)
- Input capture
- Biometric sensors (optional)

### 7.2 Model Architecture

**Pipeline Components:**

```mermaid
graph LR
    A[Raw Game Data] --> B[Feature Extraction]
    B --> C[Behavior Encoder]
    C --> D[State Representation]
    D --> E[Prediction Model]
    E --> F[Adaptation Engine]
    F --> G[Game System]
```

**Model Choices:**
- **Feature Extractor**: CNN for visual, RNN for sequential
- **Behavior Encoder**: Transformer or LSTM
- **State Model**: MDP or POMDP
- **Prediction**: Neural network with attention
- **Adaptation**: Reinforcement learning agent

### 7.3 Real-Time Considerations

**Performance Requirements:**
- Sub-millisecond prediction for action games
- Batch updates for model refinement
- Efficient feature extraction
- Cached embeddings for common states

**Architecture Patterns:**
- Asynchronous model updates
- Lightweight prediction models
- Offline training, online inference
- Gradient accumulation for learning

### 7.4 Privacy and Ethics

**Considerations:**
- GDPR compliance for user data
- Informed consent for data collection
- Anonymization techniques
- Data retention policies
- Transparent use of models

**Ethical Guidelines:**
- Avoid manipulative design
- Preserve player autonomy
- Provide opt-out mechanisms
- Ensure fair treatment
- Prevent addiction exploitation

---

## 8. MineWright Applications

### 8.1 MineWright AI Player Modeling

**Current MineWright AI Architecture:**
- LLM-powered task planning
- Action executor with plugin system
- Multi-agent coordination
- Memory system for context

**Player Modeling Integration Points:**

**1. Enhanced Prompt Building**
```java
// Current: Task context only
// Enhanced: Task + Player Model context
public class EnhancedPromptBuilder {
    private PlayerModel playerModel;

    public String buildPrompt(Task task, GameContext context) {
        return basePrompt(task, context)
            + "\nPlayer Preferences:\n"
            + playerModel.getPreferences()
            + "\nPlayer Style:\n"
            + playerModel.getPlayStyle()
            + "\nSuggested Approach:\n"
            + playerModel.getPreferredApproach();
    }
}
```

**2. Adaptive Action Execution**
```java
public class AdaptiveActionExecutor {
    private PlayerModel playerModel;

    public void executeAction(Action action) {
        // Adapt execution based on player style
        if (playerModel.isMethodical()) {
            action.setSpeed(ActionSpeed.METHODICAL);
            action.setPlanningHorizon(PlanningHorizon.LONG);
        } else if (playerModel.isAggressive()) {
            action.setSpeed(ActionSpeed.FAST);
            action.setPlanningHorizon(PlanningHorizon.SHORT);
        }
        // ... execute adapted action
    }
}
```

**3. Multi-Agent Style Coordination**
```java
public class StyleAwareBuildManager {
    private PlayerModel playerModel;
    private List<Steve> agents;

    public void assignWork(Structure structure) {
        // Adapt division of labor based on player style
        if (playerModel.prefersParallelWork()) {
            assignMaxParallelSections(structure);
        } else if (playerModel.prefersSequentialWork()) {
            assignSequentialWork(structure);
        } else if (playerModel.prefersCollaborativeWork()) {
            assignPairWork(structure);
        }
    }
}
```

### 8.2 Specific Applications

**1. Mining Assistant**
- **Prediction**: Anticipate which ores player wants next
- **Preference**: Mining style (branch, strip, spelunking)
- **Adaptation**: Match mining pace and pattern
- **Coordination**: Clear separate areas or work together

**2. Building Partner**
- **Style Recognition**: Functional vs. aesthetic, simple vs. complex
- **Prediction**: Next block type and placement
- **Adaptation**: Match building style and speed
- **Coordination**: Divide sections, complementary roles

**3. Crafting Assistant**
- **Prediction**: Items needed for current goals
- **Preference**: Manual vs. automated crafting
- **Adaptation**: Suggest recipes vs. auto-craft
- **Coordination**: Resource sharing between agents

**4. Combat Support**
- **Style Recognition**: Aggressive, defensive, tactical
- **Prediction**: Enemy targeting strategy
- **Adaptation**: Complementary combat role
- **Coordination**: Tank, DPS, healer roles

**5. Exploration Guide**
- **Preference**: Cave vs. surface, thorough vs. fast
- **Prediction**: Areas of interest to player
- **Adaptation**: Lead vs. follow, scouting vs. mapping
- **Coordination**: Cover different directions, share discoveries

### 8.3 Player Model Schema

```java
public class PlayerModel {
    // Core attributes
    private String playerId;
    private Instant lastUpdate;

    // Play style (0-1 scales)
    private double aggressionLevel;      // 0 = passive, 1 = aggressive
    private double explorationTendency;  // 0 = stays close, 1 = explores far
    private double methodicalness;       // 0 = impulsive, 1 = planned
    private double socialPreference;     // 0 = solo, 1 = collaborative

    // Activity preferences
    private Map<ActivityType, Double> activityPreferences;

    // Temporal patterns
    private Map<TimeOfDay, Double> activityByTime;
    private Map<SessionPhase, Double> styleByPhase;

    // Skill indicators
    private double combatSkill;
    private double buildingSkill;
    private double craftingKnowledge;
    private double explorationEfficiency;

    // Recent behavior (last N actions)
    private Deque<PlayerAction> recentActions;

    // Predictions
    private Map<ActionType, Double> nextActionProbabilities;
    private Optional<BlockType> predictedNextBlock;
    private Optional<Location> predictedNextLocation;
}

public enum ActivityType {
    MINING, BUILDING, CRAFTING, COMBAT, EXPLORING, FARMING, SOCIALIZING
}

public enum SessionPhase {
    EARLY, MID, LATE
}
```

### 8.4 Implementation Roadmap

**Phase 1: Data Collection**
- Add telemetry to existing action system
- Log player actions with timestamps
- Track outcomes and contexts
- Implement privacy safeguards

**Phase 2: Basic Modeling**
- Implement feature extraction
- Create simple statistical models
- Build initial player profiles
- Add preference tracking

**Phase 3: Prediction**
- Implement action prediction models
- Add sequence modeling (LSTM/Transformer)
- Create embedding-based similarity
- Evaluate prediction accuracy

**Phase 4: Adaptation**
- Modify prompt builder with player context
- Implement adaptive action execution
- Add style-aware coordination
- Test adaptation effectiveness

**Phase 5: Advanced Features**
- Implement IRL for reward inference
- Add multi-agent style coordination
- Create dynamic difficulty adjustment
- Implement content generation

### 8.5 Research Questions

**For MineWright AI:**

1. **How can LLM-based agents effectively incorporate player models?**
   - Prompt engineering for model injection
   - Few-shot examples with player style
   - Context window management

2. **What player behaviors are most predictive in Minecraft?**
   - Movement patterns
   - Block placement sequences
   - Crafting choices
   - Combat tactics

3. **How should multi-agent systems coordinate around player style?**
   - Division of labor strategies
   - Communication protocols
   - Conflict resolution
   - Dynamic rebalancing

4. **Can player modeling improve collaborative building?**
   - Style matching
   - Complementary roles
   - Conflict prediction
   - Efficiency optimization

5. **How to handle preference evolution in long-running agents?**
   - Forgetting old data
   - Detecting style changes
   - Smooth adaptation
   - Explicit preference updates

---

## 9. Research Sources

### Academic Papers

1. **Opponent Modelling for Case-Based Adaptive Game AI**
   - https://www.researchgate.net/publication/222421745_Opponent_modelling_for_case-based_adaptive_game_AI

2. **A Player Behavior Model for Predicting Win-Loss Outcome in MOBA Games**
   - https://m.zhangqiaokeyan.com/academic-conference-foreign_meeting-206551_thesis/020514187810.html

3. **An Economy of AI Agents** (arXiv, Aug 2025)
   - https://arxiv.org/html/2509.01063v1

4. **Game Theory Meets Large Language Models: A Systematic Survey** (arXiv, Aug 2025)
   - https://arxiv.org/html/2502.09053v2

5. **PORTAL: Agents Play Thousands of 3D Video Games** (arXiv, Mar 2025)
   - https://arxiv.org/html/2503.13356v1

6. **Round Outcome Prediction in VALORANT Using Tactical Analysis** (arXiv, Oct 2025)
   - https://arxiv.org/html/2510.17199v1

7. **Large Language Models and Games: A Survey and Roadmap** (arXiv, Dec 2024)
   - https://arxiv.org/html/2402.18659v5

8. **A Survey on Large Language Model-Based Game Agents** (arXiv, Apr 2024)
   - https://arxiv.org/html/2404.02039v1

9. **MineLand: Simulating Large-Scale Multi-Agent Interactions in Minecraft** (arXiv, Mar 2024)
   - https://arxiv.org/html/2403.19267v1

10. **Dynamic Difficulty Adjustment Approaches in Video Games** (Springer, 2024)
    - https://link.springer.com/article/10.1007/s11042-024-18768-x

11. **AI-Driven Dynamic Difficulty Adjustment and Fairness Metrics**
    - https://www.researchgate.net/publication/396953788_Autonomous_Game_Balancing_AI-Driven_Dynamic_Difficulty_Adjustment_and_Fairness_Metrics

12. **Revolutionising Game Design: The Role of AI in Enhancing Player Experience**
    - https://www.researchgate.net/publication/391121511_Revolutionising_game_design_The_role_of_AI_in_enhancing_player_experience_interaction_and_engagement

13. **A Data Imputation Strategy to Enhance Online Game Churn Prediction** (MDPI, Jun 2025)
    - https://www.mdpi.com/2306-5729/10/7/96

### Industry Resources

14. **AI for Game Player Behavior Prediction Report (2025)**
    - https://max.book118.com/html/2025/0929/5132212333012334.shtm

15. **Player Experience & Behavior Analysis in Games**
    - https://sg-info.cn/article/show/157065

16. **The Future of Game AI: Adaptive and Personalized** (CSDN, Dec 2023)
    - https://blog.csdn.net/universsky2015/article/details/135810283

17. **10 Best AI Game Generators** (Unite.ai, Aug 2025)
    - https://www.unite.ai/best-ai-game-generators/

### Technical Articles

18. **Video Game Personalisation Techniques: A Comprehensive Survey**
    - https://m.doc88.com/p-7704492789189.html

19. **Adaptive Agent for FPS Games using Reinforcement Learning**
    - https://www.researchgate.net/publication/344280825_Adaptive_Agent_for_FPS_Games_using_Reinforcement_Learning

20. **Synthetic User Generation in Games: Cloning Player Behavior with Transformer Models**
    - https://www.researchgate.net/publication/390996177_Synthetic_User_Generation_in_Games_Cloning_Player_Behavior_with_Transformer_Models

21. **Towards Deployment of Robust Cooperative AI Agents**
    - https://readpaper.com/paper/3037793033

### Open Source Projects

22. **Steve - Autonomous AI Agent for Minecraft**
    - https://github.com/YuvDwi/Steve

23. **Awesome LLM Agent Optimization Papers**
    - https://github.com/YoungDubbyDu/LLM-Agent-Optimization

### Chinese Resources

24. **游戏用户画像与偏好分析**
    - https://jz.docin.com/touch_new/preview_new.do?id=4868125489

25. **多智能体博弈对抗的对手建模框架**
    - https://m.blog.csdn.net/renhongxia1/article/details/135236792

26. **Mindcraft学术研究应用：多代理协作的实证研究平台**
    - https://blog.csdn.net/gitblog_00558/article/details/154805801

---

## Conclusion

Player modeling AI offers significant opportunities for enhancing the MineWright AI project. The current architecture with LLM-based planning, plugin actions, and multi-agent coordination provides an excellent foundation for incorporating player modeling capabilities.

**Key Takeaways:**

1. **Maturity**: Player modeling techniques are well-established in game AI research
2. **Real-World Use**: Successful deployments exist across multiple game genres
3. **Minecraft Focus**: Active research community uses Minecraft as a testbed
4. **Integration Feasible**: MineWright AI architecture supports player modeling integration
5. **Multi-Agent Rich Area**: Collaborative player modeling is particularly relevant

**Recommended Next Steps:**

1. Implement data collection infrastructure
2. Develop basic feature extraction and profiling
3. Prototype player-context-aware prompt building
4. Test adaptive action execution
5. Evaluate effectiveness with user studies

**Expected Benefits:**

- More intuitive agent behavior
- Reduced user frustration
- Increased engagement
- Better collaboration
- Personalized experiences

---

*Report prepared by Orchestrator Agent*
*Research conducted: 2026-02-27*
*Project: MineWright AI - Autonomous Minecraft Agents*
