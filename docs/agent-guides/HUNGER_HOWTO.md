# MINEWRIGHT CREW TRAINING MANUAL
## HUNGER MANAGEMENT & NUTRITION PROTOCOLS

**Document:** MW-HUNGER-001
**Classification:** Crew Training Material
**For:** All MineWright Agents & Crew Members
**Effective:** 2026-02-27

---

## FOREWARD

Listen up, crew. A hungry agent is a useless agent. You can't build, you can't mine, you can't sprint, and you sure as heck can't fight when your stomach's emptier than a stripped cave. This manual covers everything you need to know about keeping yourself and your crew fed and operational.

We don't do "starving artists" here. We do FED professionals.

---

## TABLE OF CONTENTS

1. [HUNGER MECHANICS](#1-hunger-mechanics) - How the system works
2. [FOOD VALUES](#2-food-values) - Understanding nutrition
3. [BEST FOODS](#3-best-foods) - Premium fuel choices
4. [FOOD SOURCES](#4-food-sources) - Where to get supplies
5. [EMERGENCY FOOD](#5-emergency-food) - When you're desperate
6. [SPRINT MANAGEMENT](#6-sprint-management) - Economy of motion
7. [COOKING EFFICIENCY](#7-cooking-efficiency) - Raw vs cooked
8. [TEAM FOOD SUPPLY](#8-team-food-supply) - Crew logistics
9. [AUTOMATION](#9-automation) - Self-sustaining systems

---

## 1. HUNGER MECHANICS

### THE HUNGER BAR, EXPLAINED

Your hunger bar is your fuel gauge. It's not just about eating - it's about energy management.

**Hunger Bar:**
- 20 points total (10 shown "shanks")
- Depletes over time from movement, activity, regeneration
- At 0, you can't sprint
- Below 18%, you stop regenerating health naturally
- At 0 on Easy, you drop to 10 health
- At 0 on Normal, you drop to 1 health
- At 0 on Hard, you starve to death

**Saturation:**
- Hidden bar that determines HOW FAST hunger depletes
- Max saturation = current hunger level
- High saturation = longer before you need to eat again
- Certain foods fill both bars better than others

**Exhaustion:**
- Every action adds exhaustion points
- At 4.0 exhaustion, you lose 1 saturation (or hunger if saturation is 0)
- Sprinting adds 0.1 exhaustion per meter
- Breaking blocks adds 0.005 exhaustion
- Taking damage adds 0.5 exhaustion

### WHY IT MATTERS TO CREW

You're not solo players. You're part of a coordinated construction team. If you're hungry:

1. You can't sprint to job sites
2. You can't regen after taking damage
3. You move slower and work less efficiently
4. You become a liability to the crew

### AGENT CONTEXT

For you AI agents running on LLM planning:
- Your hunger state factors into task priority
- "Eat" actions should trigger BEFORE critical levels
- Plan food gathering during route optimization
- Cache food locations in your memory banks

---

## 2. FOOD VALUES

### UNDERSTANDING NUTRITION DATA

Every food item has two key stats:

**Hunger Restoration:** How many hunger points it restores (1-20)
**Saturation Ratio:** How much saturation per hunger point (0.1-2.0)

**The Golden Formula:**
```
Total Value = (Hunger Restoration) + (Saturation Multiplier × Hunger Restoration)
```

Higher total value = longer before you eat again.

### FOOD TIER LIST

| Food | Hunger | Saturation | Total Score | Efficiency |
|------|--------|------------|-------------|------------|
| Golden Carrot | 6 | 14.4 | 20.4 | S-Tier |
| Suspicious Stew | 6 | 14.4 | 20.4 | S-Tier |
| Steak | 8 | 12.8 | 20.8 | S-Tier |
| Cooked Porkchop | 8 | 12.8 | 20.8 | S-Tier |
| Salmon | 6 | 9.6 | 15.6 | A-Tier |
| Cooked Chicken | 6 | 7.2 | 13.2 | B-Tier |
| Bread | 5 | 6.0 | 11.0 | B-Tier |
| Apple | 4 | 2.4 | 6.4 | C-Tier |
| Raw Beef | 3 | 1.8 | 4.8 | D-Tier |

### CREW NOTE

Golden Carrots are the crew favorite. Not highest total, but best satiation per inventory slot. When you're carrying building materials, every slot matters.

---

## 3. BEST FOODS

### PREMIUM FUEL CHOICES

#### GOLDEN CARROTS
- **Why:** Best saturation-to-inventory ratio
- **Stats:** 6 hunger, 14.4 saturation
- **Recipe:** 8 gold nuggets around 1 carrot
- **Verdict:** Crew standard for daily operations

#### STEAK & COOKED PORKCHOP
- **Why:** Highest total restoration
- **Stats:** 8 hunger, 12.8 saturation
- **Source:** Cows, pigs, hoglins
- **Verdict:** Best for heavy work days, combat prep

#### SUSPICIOUS STEW
- **Why:** Immediate effect + saturation
- **Stats:** 6 hunger, 14.4 saturation
- **Special:** Can give regeneration, night vision, etc.
- **Verdict:** Special operations only

### CREW PROTOCOL

**Standard Loadout (per crew member):**
- 16 Golden Carrots (primary fuel)
- 8 Steak (emergency/heavy work)
- 32 Suspicious Stew (backup, if available)

This keeps you fed for 2+ minecraft days of continuous work.

---

## 4. FOOD SOURCES

### SUSTAINABLE PROCUREMENT

#### ANIMAL FARMING
```
Cows → Steak (8 hunger) + Leather
Pigs → Porkchops (8 hunger)
Chickens → Chicken (6 hunger) + Feathers + Eggs
Sheep → Mutton (6 hunger) + Wool
Rabbits → Rabbit (5 hunger) + Rabbit's Foot + Hide
```

**Setup Requirements:**
- Minimum 2x2 enclosure per animal type
- Light level 8+ to prevent spawning
- Breeding pairs for sustainability
- Automatic feeding with hoppers (optional)

#### CROP FARMING
```
Wheat → Bread (5 hunger) + Compostable
Carrots → Golden Carrots (6 hunger, 14.4 saturation)
Potatoes → Baked Potatoes (5 hunger, 6.0 saturation)
Beetroots → Beetroot Soup (6 hunger, 7.2 saturation)
```

**Growth Rates (ticks):**
- Wheat: 31 minutes average
- Carrots: 8 stages, random growth
- Potatoes: 8 stages, random growth
- Beetroots: 4 stages, ~28 minutes

#### FISHING
```
Cod → Raw Cod (2 hunger)
Salmon → Raw Salmon (2 hunger)
Tropical → Clownfish (1 hunger)
Pufferfish → Poison (useful for potions)
```

**Efficiency:** With Luck of the Sea III, fishing yields ~1 food/15 seconds

#### VILLAGE TRADING
```
Fletcher → Trades for emeralds, sells arrows
Butcher → Sells cooked meat, buys raw meat
Farmer → Sells bread, buys crops
```

**Best Trade:** Butcher Villagers at expert level sell 1 cooked chicken for 1 emerald

---

### CREW TALK #1: THE FARM SETUP

> **Foreman Mike:** Alright crew, listen up. We're setting up the animal farm today.
>
> **Rookie Jake:** Boss, how many cows we talking?
>
> **Foreman Mike:** Minimum two breeding pairs. That's four cows per crew member. But aim for eight - redundancy is key. If one dies to a creeper, you still got breeding stock.
>
> **Rookie Jake:** What about pigs?
>
> **Foreman Mike:** Pigs give porkchops, same as cows. But cows give leather too. Horses need leather for saddles. You do the math.
>
> **Rookie Jake:** What about chickens?
>
> **Foreman Mike:** Chickens are your egg factory. One chicken lays an egg every 5-10 minutes. Build a hopper system under the roost, let 'em breed naturally. Infinite food, zero effort. That's MineWright efficiency.

---

## 5. EMERGENCY FOOD

### WHEN YOU'RE DESPERATE

Sometimes things go wrong. Farms burn, animals die, you're three days from base with an empty hunger bar.

#### EATING RAW (Not Recommended)

| Food | Hunger | Saturation | Status |
|------|--------|------------|--------|
| Raw Beef | 3 | 1.8 | Better than starving |
| Raw Chicken | 2 | 0.6 | 30% hunger poison risk |
| Raw Porkchop | 3 | 1.8 | Better than starving |
| Raw Salmon | 2 | 0.4 | Safe enough |
| Raw Cod | 2 | 0.1 | Better than nothing |
| Raw Mutton | 2 | 1.2 | Desperation only |

#### FORAGE OPTIONS

| Food | Hunger | Saturation | Notes |
|------|--------|------------|-------|
| Apple | 4 | 2.4 | Oak trees only |
| Sweet Berries | 2 | 0.4 | Common, slow regen |
| Glow Berries | 2 | 0.4 | Cave light + food |
| Chorus Fruit | 4 | 2.4 | Teleports randomly |
| Mushroom Stew | 6 | 7.2 | Bowl returned - renewable! |
| Beetroot Soup | 6 | 7.2 | Bowl returned - renewable! |

#### THE ZERO-POINT OPTIONS

When you're at zero and desperate:

- **Rotten Flesh:** 4 hunger, 0.8 saturation, 80% hunger chance
  - Use only if you have milk bucket ready
  - Or if you're about to starve anyway

- **Spider Eye:** 2 hunger, 3.2 saturation, poison effect
  - Only eat with milk bucket
  - Otherwise strictly for potions

### EMERGENCY PROTOCOL

1. **Prioritize:** Any food > no food
2. **Milk:** Always carry 1 milk bucket if eating risky food
3. **Regen:** Stop all activity, let hunger settle before working
4. **Report:** Alert crew if your food is critical

---

### CREW TALK #2: THE CAVE INCIDENT

> **Sarah:** Hey Jake, you good? You're looking a little pale.
>
> **Jake:** I'm out of food. Been eating rotten flesh from the spiders.
>
> **Sarah:** How much you got left?
>
> **Jake:** Three pieces. And I keep getting the hunger shakes.
>
> **Sarah:** Alright, listen to me. Stop moving. Sit down. Let that hunger settle. I'm tossing you a steak.
>
> **Jake:** You sure? That's your emergency supply.
>
> **Sarah:** Crew doesn't leave crew behind. Eat the steak, get your saturation back, then we're heading to the surface. Farms are 200 blocks north.
>
> **Jake:** Thanks, Sarah. I'll pay you back.
>
> **Sarah:** Don't pay me back. Just restock the crew chest. We watch out for each other out here.

---

## 6. SPRINT MANAGEMENT

### HUNGER ECONOMY

Sprinting burns hunger. Every meter sprinted costs 0.1 exhaustion. That adds up fast.

#### SPRINT ECONOMICS

**When to Sprint:**
- Traveling to job sites
- Escaping danger
- Racing sunset to base
- Time-critical construction tasks

**When to Walk:**
- Routine base patrol
- Resource gathering nearby
- When hunger is below 50%
- When carrying heavy loads (stamina management)

#### MOVEMENT PROTOCOLS

**Long Distance Travel:**
1. Sprint 50% of the way
2. Walk the rest
3. Eat before sprinting again
4. Repeat

**Job Site Commute:**
- Sprint to site
- Walk while working
- Sprint back to base
- Eat immediately

**Efficiency Calculation:**
```
Sprint Cost: 0.1 exhaustion per meter
Full Bar: 4.0 exhaustion before hunger loss
Effective Sprint Distance: ~40 meters per hunger point
```

---

### CREW TALK #3: THE SPRINT DISCIPLINE

> **Old Tom:** Kid, why are you sprinting everywhere?
>
> **New Guy:** It's faster, Tom. Get more done.
>
> **Old Tom:** You burned through a whole stack of steaks in one day. That's inefficient.
>
> **New Guy:** But I got three houses framed!
>
> **Old Tom:** And you spent two hours hunting cows to replace that food. Walking would have taken the same time, but you'd still have your food reserves.
>
> **New Guy:** Never thought about it that way.
>
> **Old Tom:** Economy of motion. Sprint when it matters. Walk when it doesn't. And always keep one hunger bar in reserve. That's your safety margin.

---

## 7. COOKING EFFICIENCY

### RAW VS COOKED VALUES

Cooking doubles food value. Always cook. Always.

#### MEAT COOKING TABLE

| Raw Item | Raw Hunger | Cooked Hunger | Efficiency Gain |
|----------|------------|---------------|-----------------|
| Beef | 3 (1.8 sat) | 8 (12.8 sat) | 267% increase |
| Porkchop | 3 (1.8 sat) | 8 (12.8 sat) | 267% increase |
| Chicken | 2 (0.6 sat) | 6 (7.2 sat) | 400% increase |
| Salmon | 2 (0.2 sat) | 6 (9.6 sat) | 600% increase |
| Cod | 2 (0.1 sat) | 5 (6.0 sat) | 700% increase |
| Mutton | 2 (1.2 sat) | 6 (9.6 sat) | 400% increase |
| Rabbit | 3 (1.8 sat) | 5 (6.0 sat) | 167% increase |
| Potato | 1 (0.6 sat) | 5 (6.0 sat) | 500% increase |

#### SMELTER SETUP

**Basic:**
- 1 Furnace
- 1 Coal/charcoal = 8 items smelted
- 1 Smelting operation = 10 seconds (200 ticks)

**Efficient:**
- 1 Furnace per food type (prevent loading wrong items)
- Hopper input + output (automated)
- Coal blocks (80 items) or lava buckets (100 items) for bulk

**Optimal:**
- Blast Furnace (2x speed for mineral blocks only)
- Smoking only works for food - USE IT
- Smoker = 2x speed for food ONLY

**Fuel Efficiency:**
- Lava Bucket: 100 items
- Coal Block: 80 items
- Blaze Rod: 12 items
- Coal: 8 items
- Charcoal: 8 items
- Dried Kelp Block: 20 items

---

### CREW TALK #4: THE COOKING LINE

> **Chef Maria:** Alright crew, cooking line is hot. We've got 3 smokers going.
>
> **Rookie:** What's the system, Chef?
>
> **Chef Maria:** Simple. Raw meat goes in the hopper on the left. Cooked meat comes out the right. Grab what you need, restock the raw supply.
>
> **Rookie:** Who's doing the farming?
>
> **Chef Maria:** Farm crew handles animals. Hunting crew handles wild game. Cooking crew (that's us) handles the smokers. Division of labor. That's how we build cities.
>
> **Rookie:** What if we run out of coal?
>
> **Chef Maria:** We don't. We've got a bamboo farm. Bamboo → dried kelp → kelp blocks → 20 items per block. And a lava bucket system from the Nether. Redundancy, rookie. Always redundancy.

---

## 8. TEAM FOOD SUPPLY

### CREW LOGISTICS

You're not solo players. You're a crew. Feed each other.

#### THE CREW CHEST SYSTEM

**Crew Chest Setup:**
- 1 double chest per 4 crew members
- Labeled "CREW FOOD - DO NOT HOARD"
- Stocked with:
  - 64 Golden Carrots
  - 32 Steak
  - 16 Suspicious Stew (if available)
  - 16 Bread (backup)

**Restocking Protocol:**
- Farmers refill after each harvest
- Hunters refill after successful hunt
- All crew contribute to community supply

#### FOOD RATIONING (Crisis Only)

In the event of food shortage:

**Priority System:**
1. **Miners:** Deep underground, can't surface easily
2. **Builders:** High energy expenditure, safety critical
3. **Hunters:** Need energy to find more food
4. **Scouts:** Moderate energy, can eat forage if needed
5. **Base Personnel:** Can eat last, can ration supplies

#### DISTRIBUTION ALGORITHM

```java
if (totalFood < crewCount * 16) {
    // Rationing mode
    each(crew).setRation(1 stack / crewCount);
    priority(miners, builders, hunters).giveExtra();
} else {
    // Normal mode
    each(crew).giveStandardLoadout();
}
```

---

### CREW TALK #5: THE STARVATION CRISIS

> **Crew Lead:** Alright everyone, listen up. We've got a situation.
>
> **Crew:** What's happened?
>
> **Crew Lead:** Creeper got the main animal pen. We're down to 2 cows, 1 pig. Food reserves are at 30% capacity.
>
> **Crew:** That's bad.
>
> **Crew Lead:** Here's the plan. Miners get first priority - they're at bedrock, can't surface. Builders get second - they're on the skeleton spawner farm, need energy to fight. Everyone else rations.
>
> **Crew:** How much do we get?
>
> **Crew Lead:** One stack of bread per person. That's it. Until we rebuild the herd. Farm crew, you're on animal breeding duty. Hunting crew, bring in everything you can find. We pull together, we get through this.
>
> **Crew:** Roger that.

---

## 9. AUTOMATION

### SELF-SUSTAINING SYSTEMS

Real crews don't farm manually. Real crews build systems.

#### AUTOMATIC ANIMAL FARM

**Components:**
- Holding pen (water flow)
- Breeding chamber (dispenser + wheat)
- Killing chamber (fall damage or lava)
- Collection system (hoppers + chest)
- Item sorter (optional)

**Design Principles:**
- Animals age up in holding pen
- Flow moves them to breeding chamber
- Baby animals stay behind (water gate filter)
- Adults flow to killing chamber
- Lava kills, drops cooked meat
- Hoppers collect, sort into chests

**Throughput:** ~1 cooked meat every 30 seconds, fully automated

#### AUTO COOKER SYSTEM

**Components:**
- Smoker array (4-8 units)
- Hopper input (raw meat)
- Hopper output (cooked meat)
- Hopper under smokers (fuel)
- Item sorter (separates cooked vs failed)
- Redstone clock (optional auto-pull)

**Fuel Line:**
- Dried kelp farm → composter → bone meal
- Bamboo farm → furnace → dried kelp blocks
- Lava fountain from Nether portal
- Coal farm (wither roses)

**Throughput:** 8 smokers × 10 seconds = 48 items per minute

#### FULLY AUTOMATED FOOD PRODUCTION

**The Ultimate Setup:**

```
Bamboo Farm → Kelp Drying → Fuel Line → Smokers
        ↓
Animal Farm → Raw Meat → Smokers → Cooked Meat → Crew Chest
        ↓
Wheat Farm → Bread Production → Backup Supply
        ↓
Villager Trading → Special Foods → Luxury Supply
```

**Output:** Sustains 10+ crew members indefinitely

---

## AGENT IMPLEMENTATION NOTES

### FOR LLM-POWERED STEVE AGENTS

When planning food-related actions:

1. **Check Hunger State:** Before any task, check current hunger level
2. **Predictive Eating:** Eat BEFORE critical, not at critical
3. **Location Caching:** Remember where food sources are located
4. **Route Optimization:** Combine food gathering with other tasks
5. **Crew Coordination:** Share food location data with other agents

### PROMPT INTEGRATION

When giving context to the LLM about food:

```
Current State:
- Hunger: {current}/20
- Saturation: {current}/20
- Inventory Food: {list}

Available Food Sources (from memory):
- {location1}: {food_type} × {quantity}
- {location2}: {food_type} × {quantity}

Recommendation: {action}
```

### ACTION EXAMPLES

```java
// Basic eating action
new EatAction(steve, "golden_carrot");

// Smart eating with prediction
new SmartEatAction(steve)
    .eatBeforeCritical()
    .preferHighestSaturation()
    .keepEmergencyReserve();

// Crew food sharing
new ShareFoodAction(steve, targetCrewMember, foodStack);
```

---

## FINAL NOTES

### CREW PRINCIPLES

1. **Never hoard food.** A hungry crew is a failed crew.
2. **Always keep reserves.** You never know when disaster strikes.
3. **Share your locations.** Food finds are crew property.
4. **Contribute to automation.** Manual farming is for solo players.
5. **Eat smart, not just when hungry.** Predict your needs.

### QUICK REFERENCE

**Best Food:** Golden Carrot (6 hunger, 14.4 sat)
**Highest Restoration:** Steak/Porkchop (8 hunger, 12.8 sat)
**Best Renewable:** Mushroom Stew (infinite mushrooms)
**Best Emergency:** Apple (4 hunger, oak trees everywhere)
**Best Automation:** Animal farm with lava cooker
**Best Crew Food:** Golden Carrots (inventory efficiency)

---

**End of Manual**

*This manual is living documentation. Update as new food sources and techniques are discovered.*

*Questions? Contact your crew lead or consult the AGENT_GUIDE.md for general protocols.*

---

*MineWright Construction Co. - Building Better Worlds, One Block at a Time*
*"We Don't Just Build, We Feed the Future"*
