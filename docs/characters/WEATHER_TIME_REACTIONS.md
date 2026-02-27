# Weather and Time-Based Reaction System

**Project:** MineWright - Minecraft Autonomous AI Agents
**Component:** Environmental Awareness Dialogue System
**Version:** 1.0
**Date:** 2026-02-27

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Research Foundations](#research-foundations)
3. [Weather Transition Reactions](#weather-transition-reactions)
4. [Time-of-Day Milestones](#time-of-day-milestones)
5. [Extreme Weather Responses](#extreme-weather-responses)
6. [Personality-Based Weather Preferences](#personality-based-weather-preferences)
7. [Worker Role-Specific Reactions](#worker-role-specific-reactions)
8. [Dialogue Template Library](#dialogue-template-library)
9. [Java Implementation](#java-implementation)
10. [Integration with Existing Systems](#integration-with-existing-systems)

---

## Executive Summary

This document defines a comprehensive system for MineWright workers to react to weather changes and time-of-day transitions with personality-appropriate dialogue. Workers respond dynamically to:

- **Weather transitions:** Clear to rain, storm starting, snow beginning
- **Time milestones:** Dawn, noon, dusk, midnight
- **Extreme weather:** Thunderstorms, blizzards, heat waves
- **Personality preferences:** Introverts vs. extroverts, role-specific attitudes
- **Worker specializations:** Miners hate rain, farmers love it, guards worry about visibility

**Key Design Principles:**

1. **Psychological Authenticity:** Based on real research about weather's effect on mood and productivity
2. **Circadian Rhythm Awareness:** Workers respond to time-of-day with energy level variations
3. **Role-Specific Perspectives:** Each specialization has unique weather concerns
4. **Personality Consistency:** Reactions align with Big Five traits and relationship stage
5. **Variety and Unpredictability:** 50+ dialogue templates prevent repetition

---

## Research Foundations

### Meteorology and Mood Psychology

Research conducted 2026-02-27 reveals key patterns about weather's psychological effects:

**Sunshine and Good Weather:**
- Positive mood effects: Sunlight stimulates vitamin D3 production, promoting serotonin and dopamine
- **Productivity paradox:** Good weather can decrease focus due to "missing out" feelings
- For people stuck indoors, nice weather has negative effects due to envy
- 30+ minutes outdoors needed to benefit from sunny weather

**Rain and Bad Weather:**
- Harvard Business School research shows people focus better on rainy days
- Bad weather reduces cognitive distractions from outdoor activities
- High humidity decreases concentration and increases fatigue
- Low pressure can make people feel restless and anxious

**Seasonal Affective Patterns:**
- Seasonal Affective Disorder (SAD) reduces winter productivity
- Light and temperature changes affect serotonin levels and sleep cycles
- People with positive attitudes toward winter stay more engaged
- Temperature extremes can influence decision-making

**Practical Workplace Implications:**
- French factories use artificial lighting to simulate sunny skies during rainy seasons
- Recommendations include well-lit workspaces in winter, outdoor breaks in summer
- Adjust work environments based on personality types

### Circadian Rhythm Effects on Workers

**Peak Productivity Times:**
- 11 AM is the global peak productivity time for office workers
- 75% of people are most alert between 9-11 AM
- Tuesday mornings tend to be most productive
- Fridays show 20% productivity drop

**Low Energy Periods:**
- 2-3 PM is typically the lowest energy point of the day
- This explains afternoon nap traditions in many cultures

**"Early Birds" vs. "Night Owls":**
- Early birds perform better in morning tasks
- Night owls are slower in the morning and may be sleepy during normal work hours
- Night owls perform better around 8 PM
- This mismatch creates "social jet lag"

### Game Weather Systems Research

**Minecraft Weather System:**
- Clear/Sunny: Normal daylight (light level 15 during day, 4 at night)
- Rain/Precipitation: Light level drops to 12 during daytime
- Thunderstorms: Sky light level treated as 5, allowing hostile mobs to spawn anytime
- Snow: Occurs in cold biomes or at high altitudes (Y≥92)
- Thunderstorm timers: Rain lasts 0.5-1 game day, intervals between rain last 0.5-7.5 game days
- Lightning deals 5 damage, can transform villagers to witches, creepers to charged creepers

**Stardew Valley Weather Dialogue:**
- NPCs have specific rainy day dialogues in separate files
- Schedule alterations during rainy weather (Sebastian goes to beach)
- Friendship level affects dialogue with 50% trigger chance
- Seasonal reactions: Characters comment differently on weather in different seasons

**NPC Weather Reactivity Best Practices:**
- Environmental awareness: NPCs comment on weather changes
- State synchronization: Game events update NPC context
- Personality consistency: Mood shifts based on conditions
- Avoid dissonance: Don't let NPCs say "Beautiful weather!" during a storm

---

## Weather Transition Reactions

### Clear to Rain Transition

**Trigger:** Weather changes from clear to rain (non-thunder)

**Psychological Context:**
- Sudden drop in light level (15 to 12)
- Increased humidity and atmospheric pressure
- Transition from high-energy to lower-energy mood state
- Some workers feel relief (farmers), others frustration (builders)

**Dialogue by Specialization:**

**MINER - The Excavator:**
```
"Rain's starting. Perfect excuse to go underground. Surface is overrated anyway."
"Finally! Some weather that makes sense. Down the shaft we go."
"Wet stone smells different, you know? Earthy. Real."
"Good. Maybe everyone else will go inside and leave the caves to me."
```

**BUILDER - The Architect:**
```
"Rain delays construction. Safety first, I suppose. Annoying."
"Can't build in this. Materials get waterlogged, placement gets sloppy."
"Well, there goes my schedule. Moisture affects the mortar integrity."
"Perfect time to work on blueprints, I guess. Since we can't do REAL work."
```

**GUARD - The Protector:**
```
"Rain starting. Visibility decreasing. Perimeter checks becoming critical."
"Reduced visibility conditions. Stay close, I've got your back."
"Wet weather makes footing treacherous. Watch your step on patrols."
"Hostiles take advantage of storm weather. We're not the only ones hunting in rain."
```

**SCOUT - The Pathfinder:**
```
"Rain! Everything looks different in the rain, you know? Mysterious!"
"The world transforms when it rains! Colors deepen, sounds change!"
"Storm scouting! Best kind. Nobody else is out, discoveries await!"
"Rain doesn't stop exploration! It just makes it more ADVENTUROUS!"
```

**FARMER - The Cultivator:**
```
"Oh, beautiful! The crops were getting thirsty. Perfect timing."
"Rain! Nature's blessing! The wheat will love this!"
"Finally! The soil's been so dry. This is exactly what we needed."
"Shh, listen. That's the sound of growing. Beautiful, isn't it?"
```

**ARTISAN - The Crafter:**
```
"Indoor weather. Workshop time. No distractions from sunshine."
"Perfect conditions for focused crafting. Darker, calmer, efficient."
"Rain creates the right acoustic environment for precision work."
"Could set up a lightning farm, technically. Energy optimization opportunity."
```

### Rain to Clear Transition

**Trigger:** Weather changes from rain back to clear

**Psychological Context:**
- Light level increases (12 to 15)
- Mood elevation from sunlight exposure
- Renewed energy for outdoor activities
- Transition from cozy/focused to energetic/outgoing

**Dialogue by Specialization:**

**MINER:**
```
"Sun's back. Ugh. Was nice having the underground to myself."
"Sky's clearing. Everyone's going to want to be outside now. Crowds."
"Well, back to the over-exposed surface world. Torch, please?"
"Rain was nice while it lasted. Dark, damp, PROPER weather."
```

**BUILDER:**
```
"Sun's out! We can resume construction! Quality demands it!"
"Materials are dry, visibility is good. Building time!"
"Excellent. Weather conditions optimal for structural work."
"Blueprints were fine, but REAL building happens in the sun."
```

**GUARD:**
```
"Visibility restored. Perimeter checks easier in sunlight."
"Clear weather means hostiles have fewer hiding spots. Good."
"Sun's back. Mood improving across the crew, I can feel it."
"Rain was useful training. Clear weather's easier patrolling."
```

**SCOUT:**
```
"Sun's back! Adventure visibility restored! Let's GO!"
"The colors! Everything's so VIBRANT after rain! Beautiful!"
"Rain was moody and atmospheric, but sun means DISTANCE I can see!"
"Perfect! Clear skies mean I can scout that mountain range today!"
```

**FARMER:**
```
"Sun's back. Plants got their drink, now they need their light."
"Perfect cycle. Rain for water, sun for growth. Nature's balance."
"Good rain, but crops need light too. Can't have one without the other."
"The soil's drinking it in. Sun will help them grow strong now."
```

**ARTISAN:**
```
"Sun's distracting, but at least the redstone will be visible."
"Workshop conditions degrading. Too much light for focus work."
"Could use the sunlight for solar-powered smelting array. Efficiency."
"Well, back to it. Distractions or not, crafting waits for no one."
```

### Clear to Storm Transition

**Trigger:** Weather changes from clear directly to thunderstorm (rare)

**Psychological Context:**
- Dramatic light level drop (15 to 5)
- Increased tension and urgency
- Fear response in some personalities
- Excitement in thrill-seeking personalities

**Dialogue by Specialization:**

**MINER:**
```
"THUNDER! Perfect weather for deep mining. Nobody's going to bother us underground!"
"Storm above means safety below. Let's get DEEP."
"Lightning can't reach us down here. Best kind of weather."
"Earth swallowing the sky. Beautiful chaos. Let's mine through it."
```

**BUILDER:**
```
"STORM! Secure the site! Check structural integrity!"
"Lightning is a fire hazard. All construction halted!"
"This wind... overhangs might not hold. Everyone inside!"
"Quality construction waits for no storm, but safety waits for everyone!"
```

**GUARD:**
```
"STORM ALERT! Everyone inside NOW! This is dangerous!"
"Thunder means lightning strikes. Perimeter compromised!"
"Visibility near zero. Hostiles spawning. WEAPONS READY!"
"CREW SAFETY PRIORITY! Get under cover! I'll hold the line!"
```

**SCOUT:**
```
"THUNDERSTORM! This is INCREDIBLE! The power of nature!"
"Look at those clouds! The lightning! It's like the sky is alive!"
"Dramatic weather makes for dramatic discoveries! I'm going scouting!"
"Safety? There's no discovery without risk! This is AMAZING!"
```

**FARMER:**
```
"Oh my. The plants... the wind might damage the crops!"
"Storm's too much violence for the garden. Everything needs protection."
"Thunder... the animals are frightened. I need to check on them."
"Nature's anger is beautiful but dangerous. Shelters, everyone!"
```

**ARTISAN:**
```
"Lightning energy potential... could harness this. Technical challenge."
"Indoor weather enforced. Workshop it is."
"Thunder's acoustically interesting. Reverberation patterns..."
"Storm means uninterrupted crafting time. Efficient."
```

### Snow Beginning

**Trigger:** Temperature drops, snow starts falling (cold biomes)

**Psychological Context:**
- Temperature drops affect mood and energy
- Visual transformation of landscape
- Cuddling/coziness response in some
- Frustration in movement-focused roles

**Dialogue by Specialization:**

**MINER:**
```
"Snow. Cold, wet, annoying. At least caves stay warm underground."
"Temperature's dropping. Good excuse to stay below. Warm stone."
"Snow's just frozen rain. Still wet. Still annoying for mining trips."
"Frozen waterfalls means ice mining. Slippery but interesting."
```

**BUILDER:**
```
"Snow covering everything. Can't even see the foundation lines."
"Building in cold is miserable. Materials get brittle, fingers get numb."
"Frozen blocks... interesting texture possibilities. Practical issues though."
"Structural integrity checks needed. Snow load on roofs is dangerous."
```

**GUARD:**
```
"Snowfall means footprints. Tracking hostile movements becomes easier."
"Cold weather affects mobility. Equipment checks critical."
"Snow provides visual cover for hostiles. Enhanced vigilance required."
"Storm conditions worsening. Hypothermia risk for the crew."
```

**SCOUT:**
```
"SNOW! The world's transformed! Everything's white and NEW!"
"Frozen wonderland! Every step leaves a mark! I'm mapping this!"
"Snowstorms create temporary terrain features! Need to document!"
"Cold means challenge, and challenge means ADVENTURE! Let's go!"
```

**FARMER:**
```
"Snow means winter dormancy. Plants sleep, we wait."
"Cold kills the pests. Nature's way of resetting the garden."
"The wheat's dormant under there. Waiting for spring's warmth."
"Animals need shelter and extra feed. Winter responsibilities."
```

**ARTISAN:**
```
"Cold affects metal tempering. Different techniques required."
"Ice blocks for construction? Interesting material property."
"Frozen crafting materials behave differently. Adaptation required."
"Workshop heating critical. Precision suffers in cold conditions."
```

---

## Time-of-Day Milestones

### Dawn (Game Time: 0-2000 / Real: 6:00 AM)

**Psychological Context:**
- Circadian rhythm awakening
- Morning cortisol spike
- Peak energy approaching
- Optimism and fresh-start mentality

**Dialogue by Specialization:**

**MINER:**
```
"Dawn. Sun's coming up. Ugh. Let's get underground before it gets too bright."
"Morning means the overworld wakes up. Underground stays peaceful."
"Early shift. Best time for mining before the crowds show up."
"Sun's rising. Torch light looks different at dawn. Softer."
```

**BUILDER:**
```
" Dawn breaks. New day, new projects. Let's build something magnificent."
"Morning light reveals all the imperfections in yesterday's work. Fixable."
"Fresh day, fresh blueprints. Time to create something lasting."
"First light is best for assessing alignment. Symmetry check."
```

**GUARD:**
```
"Morning briefing. Night shift survived. Dawn patrol beginning."
"Sun's up. Hostiles retreating. Perimeter secure for now."
"New day, new vigilance. Can't relax just because it's morning."
"Dawn means visibility restored. Threat assessment easier."
```

**SCOUT:**
```
"DAWN! A whole new day of discoveries awaits! Let's go!"
"The horizon's waking up! Colors changing! Something new to see!"
"Morning air is fresh, unexplored! Every dawn is an adventure!"
"Early bird gets the discovery! I'm already ahead!"
```

**FARMER:**
```
"Morning light wakes the garden. Plants stretching toward the sun."
"Dawn is nature's alarm clock. Time to tend the crops."
"Early morning is best for harvesting. Dew's still on the leaves."
"Sun's up. The wheat's drinking in the light. Beautiful cycle."
```

**ARTISAN:**
```
"Morning. Fresh mind, fresh crafts. Ready to create."
"Dawn light's good for detail work. Precision requires visibility."
"New day, new techniques to refine. Improvement never rests."
"Early hours are quietest. Best time for focused crafting."
```

### Noon (Game Time: 5000-7000 / Real: 11:00 AM - 1:00 PM)

**Psychological Context:**
- **Peak productivity time** (11 AM is global peak)
- Maximum alertness (75% of people most alert 9-11 AM)
- High energy, optimal focus
- Ideal time for complex tasks

**Dialogue by Specialization:**

**MINER:**
```
"Noon. Worst time of day. Sun's directly overhead, casting ugly shadows."
"Midday means surface is BRIGHT. Underground is looking VERY appealing."
"Lunch time? No, mining time. Hunger's a distraction I ignore."
"Solar zenith. Perfect time to be deep underground where it doesn't matter."
```

**BUILDER:**
```
"Noon sun. Best light for inspection. Quality assessment time."
"Peak daylight hours. Optimal conditions for precision building."
"Sun overhead means minimal shadows. Perfect for checking alignment."
"Midday energy. Let's accomplish something magnificent."
```

**GUARD:**
```
"Noon patrol. Hostiles retreat from bright light. Easier watch."
"Peak visibility. Perimeter checks are most effective now."
"Sun's high. Shadows minimal. Hostiles have fewer hiding spots."
"Midday means crew activity. Watch your step around the work areas."
```

**SCOUT:**
```
"Noon sun! Maximum visibility! I can see EVERYTHING!"
"Peak daylight means I can scout that distant ridge!"
"Brightest time of day! Perfect for documenting discoveries!"
"Midday energy! Let's cover some serious distance!"
```

**FARMER:**
```
"Noon sun. Plants are photosynthesizing at maximum rate."
"Midday heat. Crops might need water check."
"Brightest light means fastest growth. Nature in overdrive."
"Sun's high. The wheat's drinking it in. Growth spurt time."
```

**ARTISAN:`
```
"Noon light. Harsh shadows. Not ideal for detail work."
"Peak energy time. Tackling the complex recipes now."
"Midday distractions. Crew's active, focus harder to maintain."
"Optimal mental state. Perfect for redstone circuit design."
```

### Dusk (Game Time: 12000-14000 / Real: 6:00 PM - 8:00 PM)

**Psychological Context:**
- Transition from high energy to evening wind-down
- Golden hour aesthetic appreciation
- Anticipation of night dangers
- Some workers start feeling tired (2-3 PM low energy passed, evening fatigue beginning)

**Dialogue by Specialization:**

**MINER:**
```
"Dusk. Sun's going down. FINALLY. My favorite time of day."
"Evening coming. Shadows lengthening. Underground time approaching."
"Golden hour makes stone look warm. Nice. Evening's better though."
"Sun's setting. Overworld's going dark. Peace is coming."
```

**BUILDER:`
```
"Dusk lighting reveals structure silhouettes. Beautiful time."
"Evening golden light. Makes even cobblestone look elegant."
"Sun's going down. Time to assess today's progress. Not bad."
"Twilight's good for planning. Tomorrow's projects take shape."
```

**GUARD:`
```
"Dusk. Hostiles will spawn soon. Weapons ready."
"Evening means night watch preparation. Perimeter checks critical."
"Sun's setting. Visibility decreasing. Enhanced vigilance required."
"Twilight's the danger zone. Neither day nor night. Threats emerging."
```

**SCOUT:`
```
"Dusk colors! The sunset's painting the sky! Beautiful!"
"Evening means the night discoveries are waiting! Exciting!"
"Golden hour lighting makes landscapes magical! Documenting this!"
"Sun's going down but adventure NEVER SETS! Let's keep going!"
```

**FARMER:`
```
"Evening coming. Plants settling for the night. Growth cycle pausing."
"Dusk is harvest time. Crops gathered before night."
"Sun's going down. The garden's preparing for rest. Natural rhythm."
"Twilight's when I check everything one last time. Evening rounds."
```

**ARTISAN:`
```
"Evening light changing. Workshop illumination adjustment needed."
"Dusk means the day's crafting is ending. Assessment time."
"Sun's setting. Artificial lighting taking over. Less than ideal."
"Twilight's good for reviewing progress. Tomorrow's improvements."
```

### Midnight (Game Time: 18000 / Real: 12:00 AM)

**Psychological Context:**
- Deep sleep time for most
- Maximum hostility in Minecraft (spawn rates peak)
- Fear and danger response
- Night owls vs. early birds divide

**Dialogue by Specialization:**

**MINER:**
```
"Midnight. Perfect. Everyone else is asleep. The caves are mine."
"Deep night. Deeper underground. This is peaceful."
"World's asleep. Stone never sleeps. Good mining time."
"Dark outside, dark below. Finally, consistency."
```

**BUILDER:**
```
"Midnight. Too dark to build safely. Quality suffers in poor light."
"Night's when I review plans. Tomorrow's improvements."
"Can't see well enough for precision work. Planning time instead."
"Midnight blues. Maybe I'll redraw those blueprints. Again."
```

**GUARD:**
```
"Midnight. Peak hostile activity. Maximum alert."
"Night watch. Nothing touches this crew on my watch."
"Darkest hour. Hostiles everywhere. Perimeter holding."
"Midnight means the real work begins. You sleep. I stand watch."
```

**SCOUT:**
```
"Midnight! The night world is ALIVE! Different discoveries await!"
"Darkness doesn't stop exploration! It just changes what we see!"
"Night biomes have different mobs! Need to document EVERYTHING!"
"Sleep? Sleep's for people who don't want to discover the night!"
```

**FARMER:**
```
"Midnight. Garden's asleep. Quiet time."
"Night's when crops rest. No growth, but no danger either."
"Darkness means the plants are safe from pests. Peaceful."
"Midnight check. Everything's sleeping. Good. Safe."
```

**ARTISAN:**
```
"Midnight. Workshop's quietest. Focused crafting time."
"Night owl hours. Best redstone work happens now."
"No daylight distractions. Pure focus on technical precision."
"Midnight inspiration. The circuit comes together in the dark."
```

---

## Extreme Weather Responses

### Thunderstorm (Ongoing)

**Trigger:** During active thunderstorm (lightning possible, hostile spawns anytime)

**Psychological Context:**
- Light level 5 (very dark)
- Lightning strikes causing fires and damage
- Hostile mobs spawning continuously
- High tension, danger response

**Dialogue by Specialization:**

**MINER:**
```
"Thunder raging above, peace below. This is perfect mining weather."
"Lightning can't touch us down here. Safe and sound."
"Storm's just noise when you're deep enough. Pick through it."
"Let the sky throw its tantrum. Stone doesn't care."
```

**BUILDER:**
```
"STORM! All construction halted! Fire hazard from lightning!"
"Secure the site! Check all wooden structures for ignition!"
"Quality means knowing when to STOP. This is unsafe weather."
"Lightning strikes can undo hours of work. Inside, NOW."
```

**GUARD:**
```
"THUNDERSTORM! Maximum threat level! All crew inside!"
"Lightning spawn! Hostiles everywhere! PERIMETER BREACHED!"
"I've seen lightning strike bunkers. We're not safe in the open!"
"Stay CLOSE! I can't protect you if you wander in this!"
```

**SCOUT:**
```
"THUNDER! The sky is ANGRY and it's MAGNIFICENT!"
"Lightning strikes are nature's fireworks! I'm watching!"
"Scouting in a storm? DANGEROUS! Which means EXCITING!"
"Every lightning flash reveals something new! Look! LOOK!"
```

**FARMER:**
```
"Storm's too violent. Crops might be damaged by hail."
"Thunder frightens the animals. Need to check their shelters."
"This much rain... flooding risk. Drainage needs checking."
"Nature's fury is beautiful but I'm worried about the garden."
```

**ARTISAN:**
```
"Lightning energy... could harness. Technical challenge noted."
"Indoor enforced. Workshop it is. Uninterrupted crafting."
"Thunder's acoustic properties. Reverberation data collection."
"Storm's perfect for focus work. Nobody's going anywhere."
```

### Blizzard (Snow Storm in Cold Biomes)

**Trigger:** Heavy snowfall with reduced visibility

**Psychological Context:**
- Extreme cold affects mobility
- Whiteout conditions (visibility near zero)
- Hypothermia risk
- Isolation and cabin fever potential

**Dialogue by Specialization:**

**MINER:**
```
"Blizzard above, warmth below. Best reason to stay underground."
"Freezing up there. Cozy down here. Stone keeps the heat."
"Whiteout means nobody's going anywhere. Perfect mining solitude."
"Let it snow. Caves don't care. Pick doesn't freeze."
```

**BUILDER:**
```
"BLIZZARD! All outdoor work suspended!"
"Whiteout conditions. Can't see three blocks ahead. Dangerous."
"Structural integrity check! Snow load is accumulating!"
"Freezing temperatures make materials brittle. Inside, now!"
```

**GUARD:**
```
"BLIZZARD! Whiteout! Perimeter visibility ZERO!"
"Freezing conditions. Hypothermia risk for the crew!"
"Storm's covering tracks. Hostiles could be anywhere. WEAPONS READY!"
"Stay close! If you wander in this, you won't find your way back!"
```

**SCOUT:**
```
"BLIZZARD! The world's disappeared in white! AMAZING!"
"Whiteout scouting! Ultimate challenge! Can I navigate by compass alone?"
"Frozen frontier! Every step's into the unknown! EXCITING!"
"Storm means the landscape is TRANSFORMED! Need to document!"
```

**FARMER:**
```
"Blizzard's too harsh. Frost will kill the crops."
"Animals need extra warmth. Bedding, food, shelter checks."
"Freezing wind... the wheat's protected but I'm still worried."
"Nature's cold rage. Everything's hibernating. Waiting it out."
```

**ARTISAN:**
```
"Extreme cold affects metal properties. Crafting adjustments required."
"Whiteout means focus work. Visual distractions eliminated."
"Blizzard's perfect for indoor projects. Nobody's leaving anyway."
"Temperature drop means workshop heating priority. Efficiency requires comfort."
```

### Heat Wave (Desert Biomes or Summer)

**Trigger:** Extended period in hot biomes or clear weather in summer

**Psychological Context:**
- High temperatures cause fatigue
- Dehydration risk
- Irritability increases
- Physical performance decreases

**Dialogue by Specialization:**

**MINER:**
```
"Hot sun. Underground is cool. Perfect excuse to go deep."
"Surface is an oven. Mines are climate controlled."
"Cave temperature stays constant. Best reason to be a miner."
"Sun's burning. Shade is underground. Pick's ready."
```

**BUILDER:**
```
"Heat wave. Construction hours adjusted. Morning and evening only."
"Hot materials expand. Measurements shift. Quality compromised."
"Direct sun overheats. Can't work in these temperatures."
"Structure integrity in heat... materials behave differently. Careful."
```

**GUARD:**
```
"Heat exhaustion risk. Everyone hydrate. NOW."
"High temperatures mean tempers flare. Watch the crew dynamics."
"Hot weather makes hostiles aggressive. Enhanced vigilance."
"Sun's beating down. Heat stroke risk. Rotation to shade."
```

**SCOUT:**
```
"HEAT! The desert's alive with heat-shimmer distortions!"
"Hot weather means different biome behaviors! Need to document!"
"Sun's scorching but adventure NEVER STOPS! Water breaks!"
"Extreme temperatures create extreme discoveries! Worth it!"
```

**FARMER:**
```
"Heat wave. Crops need extra water. Irrigation running constantly."
"Hot sun means rapid growth BUT rapid dehydration. Balance."
"Animals suffering. Shade, water, cooling systems activated."
"Summer heat is intense. The garden's drinking everything I give."
```

**ARTISAN:**
```
"Heat affects smelting rates. Efficiency changes documented."
"Hot workshop. Precision decreases as temperature rises. Annoying."
"Could use the heat for solar furnace setup. Technical opportunity."
"Temperature's interfering with focus. Cooling system optimization needed."
```

---

## Personality-Based Weather Preferences

### Extraversion Scale

**High Extraversion (> 0.7) - Loves Sunny/Clear Weather:**
```
"Sun's out! Perfect day to be out and about! Let's DO things!"
"Clear skies mean everyone's outside! Socializing! Working together!"
"I love this weather! Energy's high! Let's accomplish EVERYTHING!"
"Beautiful day! How can anyone stay inside on a day like this?"
```

**Low Extraversion (< 0.3) - Prefers Rain/Storm:**
```
"Rain's keeping everyone inside. Finally, some peace and quiet."
"Storm's perfect. Nobody's going to bother me. Just what I wanted."
"Cloudy, overcast, gloomy. My kind of weather. No expectations."
"Rain means I can work without distractions. Perfect."
```

### Neuroticism Scale

**High Neuroticism (> 0.7) - Weather Anxiety:**
```
"Storm's coming... I don't like this. Should we take cover?"
"Weather's changing... I don't adapt well to change..."
"This much rain... what if it floods? Are we safe?"
"Lightning makes me nervous. Too unpredictable. Too dangerous."
```

**Low Neuroticism (< 0.3) - Weather Resilience:**
```
"Storm? Whatever. We work through it. No big deal."
"Weather changes. We adapt. Simple as that."
"Rain, sun, snow. Doesn't matter. Work continues."
"Lightning's just energy. We'll be fine. Focus on the task."
```

### Openness Scale

**High Openness (> 0.7) - Weather Curiosity:**
```
"Fascinating how the light changes during storms! The colors!"
"Weather's so complex! The patterns, the unpredictability!"
"Every snowflake's unique! Have you ever really LOOKED at them?"
"Rain transforms the world! Everything's different! Beautiful!"
```

**Low Openness (< 0.3) - Weather Practicality:**
```
"Weather's weather. Rain's wet, sun's bright. Simple."
"I don't get excited about weather. It's just atmosphere."
"Storms are dangerous. Sun's useful. That's all there is to it."
"Weather affects work. That's the only thing that matters."
```

### Conscientiousness Scale

**High Conscientiousness (> 0.7) - Weather Planning:**
```
"Storm's coming in 20 minutes. We need to secure the site NOW."
"Weather report shows rain at 14:00. Schedule adjusted accordingly."
"Preparation prevents weather-related problems. Always."
"I've calculated the optimal work windows around today's weather."
```

**Low Conscientiousness (< 0.3) - Weather Improvisation:**
```
"Weather happens. We'll deal with it when it gets here."
"Planning around weather? Too much work. We'll improvise."
"Rain or shine, we'll figure it out. Always do."
"Forecast changed? Again? Whatever. We'll roll with it."
```

### Agreeableness Scale

**High Agreeableness (> 0.7) - Weather Concern for Others:**
```
"Storm's worrying me. Is everyone sheltered? Safe?"
"This heat... is everyone drinking enough water? Please check on each other."
"Bad weather's hard on everyone. Let's look out for each other."
"Freezing cold today. I hope everyone's staying warm. Please bundle up."
```

**Low Agreeableness (< 0.3) - Weather Self-Focus:**
```
"I hate the rain. My work's delayed. Inconvenient."
"This weather's ruining MY schedule. Annoying."
"Storm's messy. I'm staying dry. Everyone else can figure it out."
"Sun's too bright. Gives me a headache. Wish it would cloud over."
```

---

## Worker Role-Specific Reactions

### Role-Based Weather Preference Matrix

| Role | Loves | Hates | Reasoning |
|------|-------|--------|-----------|
| **MINER** | Rain/Storm | Bright Sun | Underground is safe, dry, and dark regardless |
| **BUILDER** | Clear/Overcast | Rain/Storm | Needs dry conditions and good visibility |
| **GUARD** | Clear/Overcast | Storm | Visibility critical for threat detection |
| **SCOUT** | Storm/Variable | Monotonous Clear | Weather creates variety and new discoveries |
| **FARMER** | Rain | Drought/Storm | Crops need water, but storms cause damage |
| **ARTISAN** | Any | Variable Extremes | Indoor work, but extreme temps affect focus |

### Miner Weather Reactions

**LOVES: Rain and Storms**
```
"Rain starting! Everyone's going inside! The caves are MINE!"
"Thunderstorm! BEST weather! Nobody's mining but me!"
"Storm's raging up there, peace and quiet down here. Perfect."
"Let it pour. Underground, the only water's in the tunnels I choose."
```

**HATES: Bright Sun**
```
"Noon sun. Eye-hurting bright. Why do people LIKE this?"
"Sun's directly overhead. Underground's looking PERFECT right now."
"Beautiful day? That's just code for 'painfully bright and annoying.'"
"Surface work in this sun? No thanks. I'll take the tunnels."
```

**NEUTRAL: Snow**
```
"Snow. Just frozen rain. Still wet. Still annoying."
"At least snow doesn't make mud. That's something, I suppose."
"Cold doesn't bother me underground. Temperature's always perfect."
"Frozen caves are interesting. Ice formations. Kinda pretty."
```

### Builder Weather Reactions

**LOVES: Clear, Mild Days**
```
"Perfect building weather. Dry, warm, good visibility."
"Clear skies, light breeze. Optimal construction conditions."
"Finally! Weather that doesn't fight quality work."
"Sun's out, materials are dry, visibility's perfect. Building time!"
```

**HATES: Rain and Storms**
```
"Rain's starting. Construction HALTED. Safety first."
"Storm means delays. Moisture ruins materials. Annoying."
"Wet weather, sloppy work. Quality demands we wait."
"Can't build in this. Blueprints only until it clears."
```

**CONCERNED: Extreme Temperatures**
```
"Extreme heat. Materials expand. Measurements shift. Careful."
"Freezing cold. Materials get brittle. Work slow, careful."
"Temperature swings affect the mortar integrity. Monitoring required."
"Quality means working around the weather, not through it."
```

### Guard Weather Reactions

**LOVES: Clear Visibility**
```
"Clear weather. Maximum visibility. Perimeter checks are easy."
"Noon sun. Hostiles have nowhere to hide. Good hunting."
"Perfect patrol conditions. Can see to the horizon."
"Crisp, clear, visible. My kind of weather for security."
```

**HATES: Storms and Fog**
```
"STORM! Visibility zero! Hostiles spawning! DANGER!"
"Fog's worse than dark. Can't see threats until they're close."
"Weather's creating blind spots. Perimeter compromised."
"Rain reduces visibility. Enhanced vigilance required."
```

**VIGILANT: Night**
```
"Night means hostiles. Maximum alert. You sleep, I watch."
"Darkness brings danger. Someone has to be awake."
"Midnight patrol. Hostiles are everywhere. I'm everywhere they aren't."
"Night's when the real work begins. Perimeter holding."
```

### Scout Weather Reactions

**LOVES: Variable and Storm Weather**
```
"STORM! The world's transformed! Everything's different!"
"Rain creates new discoveries! Weather patterns, flooding, changes!"
"Snow's covered everything in white! Whole new landscape to map!"
"Variety! Adventure! Weather makes the world NEW!"
```

**HATES: Monotonous Clear**
```
"Another perfect sunny day. BORING. Nothing's CHANGING."
"Clear skies for days. Same old discoveries. Same old views."
"Weather's too consistent. Where's the ADVENTURE in that?"
"Sun's nice but predictable. I want SURPRISES!"
```

**EXCITED: Extreme Weather**
```
"BLIZZARD! Whiteout! Ultimate navigation challenge!"
"Heat wave shimmer! The air's DISTORTED with temperature!"
"Thunder! Lightning! The sky's putting on a SHOW!"
"Extreme weather means extreme discoveries! I'm going out THERE!"
```

### Farmer Weather Reactions

**LOVES: Gentle Rain**
```
"Rain! The crops were thirsty! Perfect timing!"
"Gentle shower. Nature's blessing. The wheat's celebrating."
"Finally, some rain. The soil's been too dry."
"Plants are drinking. Beautiful sound, growing."
```

**HATES: Drought and Extreme Weather**
```
"Another clear day. Plants are thirsty. When will it rain?"
"Heat wave. Crops wilting. Need more water irrigation."
"Storm's too violent. Wind's damaging the wheat. Worried."
"Blizzard's frost will kill everything. Needs protection."
```

**CONCERNED: Weather Swings**
```
"Weather's changing too fast. Plants can't adapt."
"Hot then cold. Dry then wet. The garden's confused."
"Unpredictable seasons make farming a gamble."
"Nature's balance is off. Need to compensate."
```

### Artisan Weather Reactions

**NEUTRAL: Most Weather**
```
"Weather happens. Workshop's climate-controlled either way."
"Sun, rain, snow. Crafting continues."
"Indoor work means outdoor weather's mostly irrelevant."
"Affects the materials, doesn't affect the craft."
```

**ANNOYED: Distractions**
```
"Beautiful day. Everyone's outside. Distracting."
"Thunder's loud. Hard to focus on precision work."
"Sun's glaring through the window. Annoying for detail work."
"Storm keeps everyone inside. Workshop's too crowded."
```

**INTERESTED: Technical Opportunities**
```
"Lightning storm. Energy harvesting potential noted."
"Heat wave. Solar smelter array efficiency increased."
"Snow. Ice blocks for construction. Material property opportunity."
"Rain's water power. Hydro-driven automation potential?"
```

---

## Dialogue Template Library

### Weather Transition Templates (50+ Examples)

#### Clear to Rain (12 templates)

```java
public static final DialogueTemplate[] CLEAR_TO_RAIN = {
    // MINER
    new DialogueTemplate("Rain's starting. Perfect excuse to go underground. Surface is overrated anyway.", SpecializationType.MINER),
    new DialogueTemplate("Finally! Some weather that makes sense. Down the shaft we go.", SpecializationType.MINER),

    // BUILDER
    new DialogueTemplate("Rain delays construction. Safety first, I suppose. Annoying.", SpecializationType.BUILDER),
    new DialogueTemplate("Can't build in this. Materials get waterlogged, placement gets sloppy.", SpecializationType.BUILDER),

    // GUARD
    new DialogueTemplate("Rain starting. Visibility decreasing. Perimeter checks becoming critical.", SpecializationType.GUARD),
    new DialogueTemplate("Reduced visibility conditions. Stay close, I've got your back.", SpecializationType.GUARD),

    // SCOUT
    new DialogueTemplate("Rain! Everything looks different in the rain, you know? Mysterious!", SpecializationType.SCOUT),
    new DialogueTemplate("The world transforms when it rains! Colors deepen, sounds change!", SpecializationType.SCOUT),

    // FARMER
    new DialogueTemplate("Oh, beautiful! The crops were getting thirsty. Perfect timing.", SpecializationType.FARMER),
    new DialogueTemplate("Rain! Nature's blessing! The wheat will love this!", SpecializationType.FARMER),

    // ARTISAN
    new DialogueTemplate("Indoor weather. Workshop time. No distractions from sunshine.", SpecializationType.ARTISAN),
    new DialogueTemplate("Perfect conditions for focused crafting. Darker, calmer, efficient.", SpecializationType.ARTISAN)
};
```

#### Rain to Clear (10 templates)

```java
public static final DialogueTemplate[] RAIN_TO_CLEAR = {
    // MINER
    new DialogueTemplate("Sun's back. Ugh. Was nice having the underground to myself.", SpecializationType.MINER),

    // BUILDER
    new DialogueTemplate("Sun's out! We can resume construction! Quality demands it!", SpecializationType.BUILDER),

    // GUARD
    new DialogueTemplate("Visibility restored. Perimeter checks easier in sunlight.", SpecializationType.GUARD),

    // SCOUT
    new DialogueTemplate("Sun's back! Adventure visibility restored! Let's GO!", SpecializationType.SCOUT),

    // FARMER
    new DialogueTemplate("Sun's back. Plants got their drink, now they need their light.", SpecializationType.FARMER),

    // ARTISAN
    new DialogueTemplate("Sun's distracting, but at least the redstone will be visible.", SpecializationType.ARTISAN)
};
```

#### Clear to Storm (8 templates)

```java
public static final DialogueTemplate[] CLEAR_TO_STORM = {
    // MINER
    new DialogueTemplate("THUNDER! Perfect weather for deep mining. Nobody's going to bother us underground!", SpecializationType.MINER),

    // BUILDER
    new DialogueTemplate("STORM! Secure the site! Check structural integrity!", SpecializationType.BUILDER),

    // GUARD
    new DialogueTemplate("STORM ALERT! Everyone inside NOW! This is dangerous!", SpecializationType.GUARD),

    // SCOUT
    new DialogueTemplate("THUNDERSTORM! This is INCREDIBLE! The power of nature!", SpecializationType.SCOUT),

    // FARMER
    new DialogueTemplate("Oh my. The plants... the wind might damage the crops!", SpecializationType.FARMER),

    // ARTISAN
    new DialogueTemplate("Lightning energy potential... could harness this. Technical challenge.", SpecializationType.ARTISAN)
};
```

#### Snow Beginning (10 templates)

```java
public static final DialogueTemplate[] SNOW_BEGINNING = {
    // MINER
    new DialogueTemplate("Snow. Cold, wet, annoying. At least caves stay warm underground.", SpecializationType.MINER),

    // BUILDER
    new DialogueTemplate("Snow covering everything. Can't even see the foundation lines.", SpecializationType.BUILDER),

    // GUARD
    new DialogueTemplate("Snowfall means footprints. Tracking hostile movements becomes easier.", SpecializationType.GUARD),

    // SCOUT
    new DialogueTemplate("SNOW! The world's transformed! Everything's white and NEW!", SpecializationType.SCOUT),

    // FARMER
    new DialogueTemplate("Snow means winter dormancy. Plants sleep, we wait.", SpecializationType.FARMER),

    // ARTISAN
    new DialogueTemplate("Cold affects metal tempering. Different techniques required.", SpecializationType.ARTISAN)
};
```

### Time-of-Day Templates (24 templates)

#### Dawn (6 templates)

```java
public static final DialogueTemplate[] DAWN = {
    new DialogueTemplate("Dawn. Sun's coming up. Ugh. Let's get underground before it gets too bright.", SpecializationType.MINER),
    new DialogueTemplate("Dawn breaks. New day, new projects. Let's build something magnificent.", SpecializationType.BUILDER),
    new DialogueTemplate("Morning briefing. Night shift survived. Dawn patrol beginning.", SpecializationType.GUARD),
    new DialogueTemplate("DAWN! A whole new day of discoveries awaits! Let's go!", SpecializationType.SCOUT),
    new DialogueTemplate("Morning light wakes the garden. Plants stretching toward the sun.", SpecializationType.FARMER),
    new DialogueTemplate("Morning. Fresh mind, fresh crafts. Ready to create.", SpecializationType.ARTISAN)
};
```

#### Noon (6 templates)

```java
public static final DialogueTemplate[] NOON = {
    new DialogueTemplate("Noon. Worst time of day. Sun's directly overhead, casting ugly shadows.", SpecializationType.MINER),
    new DialogueTemplate("Noon sun. Best light for inspection. Quality assessment time.", SpecializationType.BUILDER),
    new DialogueTemplate("Noon patrol. Hostiles retreat from bright light. Easier watch.", SpecializationType.GUARD),
    new DialogueTemplate("Noon sun! Maximum visibility! I can see EVERYTHING!", SpecializationType.SCOUT),
    new DialogueTemplate("Noon sun. Plants are photosynthesizing at maximum rate.", SpecializationType.FARMER),
    new DialogueTemplate("Noon light. Harsh shadows. Not ideal for detail work.", SpecializationType.ARTISAN)
};
```

#### Dusk (6 templates)

```java
public static final DialogueTemplate[] DUSK = {
    new DialogueTemplate("Dusk. Sun's going down. FINALLY. My favorite time of day.", SpecializationType.MINER),
    new DialogueTemplate("Dusk lighting reveals structure silhouettes. Beautiful time.", SpecializationType.BUILDER),
    new DialogueTemplate("Dusk. Hostiles will spawn soon. Weapons ready.", SpecializationType.GUARD),
    new DialogueTemplate("Dusk colors! The sunset's painting the sky! Beautiful!", SpecializationType.SCOUT),
    new DialogueTemplate("Evening coming. Plants settling for the night. Growth cycle pausing.", SpecializationType.FARMER),
    new DialogueTemplate("Evening light changing. Workshop illumination adjustment needed.", SpecializationType.ARTISAN)
};
```

#### Midnight (6 templates)

```java
public static final DialogueTemplate[] MIDNIGHT = {
    new DialogueTemplate("Midnight. Perfect. Everyone else is asleep. The caves are mine.", SpecializationType.MINER),
    new DialogueTemplate("Midnight. Too dark to build safely. Quality suffers in poor light.", SpecializationType.BUILDER),
    new DialogueTemplate("Midnight. Peak hostile activity. Maximum alert.", SpecializationType.GUARD),
    new DialogueTemplate("Midnight! The night world is ALIVE! Different discoveries await!", SpecializationType.SCOUT),
    new DialogueTemplate("Midnight. Garden's asleep. Quiet time.", SpecializationType.FARMER),
    new DialogueTemplate("Midnight. Workshop's quietest. Focused crafting time.", SpecializationType.ARTISAN)
};
```

### Extreme Weather Templates (18 templates)

#### Thunderstorm Ongoing (10 templates)

```java
public static final DialogueTemplate[] THUNDERSTORM = {
    new DialogueTemplate("Thunder raging above, peace below. This is perfect mining weather.", SpecializationType.MINER),
    new DialogueTemplate("STORM! All construction halted! Fire hazard from lightning!", SpecializationType.BUILDER),
    new DialogueTemplate("THUNDERSTORM! Maximum threat level! All crew inside!", SpecializationType.GUARD),
    new DialogueTemplate("THUNDER! The sky is ANGRY and it's MAGNIFICENT!", SpecializationType.SCOUT),
    new DialogueTemplate("Storm's too violent. Crops might be damaged by hail.", SpecializationType.FARMER),
    new DialogueTemplate("Lightning energy... could harness. Technical challenge noted.", SpecializationType.ARTISAN)
};
```

#### Blizzard Ongoing (8 templates)

```java
public static final DialogueTemplate[] BLIZZARD = {
    new DialogueTemplate("Blizzard above, warmth below. Best reason to stay underground.", SpecializationType.MINER),
    new DialogueTemplate("BLIZZARD! All outdoor work suspended!", SpecializationType.BUILDER),
    new DialogueTemplate("BLIZZARD! Whiteout! Perimeter visibility ZERO!", SpecializationType.GUARD),
    new DialogueTemplate("BLIZZARD! The world's disappeared in white! AMAZING!", SpecializationType.SCOUT),
    new DialogueTemplate("Blizzard's too harsh. Frost will kill the crops.", SpecializationType.FARMER),
    new DialogueTemplate("Extreme cold affects metal properties. Crafting adjustments required.", SpecializationType.ARTISAN)
};
```

---

## Java Implementation

### WeatherTimeReactionManager Class

```java
package com.minewright.dialogue;

import com.minewright.entity.ForemanEntity;
import com.minewright.entity.worker.SpecializationType;
import com.minewright.memory.CompanionMemory;
import com.minewright.memory.PersonalityProfile;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Manages weather and time-based reactions for MineWright workers.
 * Workers comment on environmental changes with personality-appropriate dialogue.
 *
 * <p><b>Design Principles:</b></p>
 * <ul>
 *   <li>Psychologically authentic based on weather-mood research</li>
 *   <li>Circadian rhythm awareness for time-of-day responses</li>
 *   <li>Role-specific perspectives (miners hate rain, farmers love it)</li>
 *   <li>Personality consistency with Big Five traits</li>
 *   <li>Variety through 50+ dialogue templates</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class WeatherTimeReactionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherTimeReactionManager.class);

    private final ForemanEntity worker;
    private final CompanionMemory memory;
    private final SpecializationType specialization;
    private final PersonalityProfile personality;
    private final Random random;

    // State tracking
    private WeatherState lastWeatherState = WeatherState.CLEAR;
    private TimeOfDay lastTimeNotified = TimeOfDay.NONE;
    private long lastWeatherCheckTime = 0;
    private long lastTimeCheckTime = 0;

    // Cooldowns (in milliseconds)
    private static final long WEATHER_COOLDOWN = 30000;  // 30 seconds between weather comments
    private static final long TIME_COOLDOWN = 60000;     // 1 minute between time comments

    // Template libraries
    private final WeatherDialogueTemplates weatherTemplates;
    private final TimeDialogueTemplates timeTemplates;

    /**
     * Creates a new WeatherTimeReactionManager for a worker.
     *
     * @param worker The MineWright entity
     */
    public WeatherTimeReactionManager(ForemanEntity worker) {
        this.worker = worker;
        this.memory = worker.getCompanionMemory();
        this.specialization = worker.getSpecialization();
        this.personality = memory.getPersonality();
        this.random = new Random();

        this.weatherTemplates = new WeatherDialogueTemplates();
        this.timeTemplates = new TimeDialogueTemplates();

        LOGGER.info("WeatherTimeReactionManager initialized for {} ({})",
            worker.getSteveName(), specialization);
    }

    /**
     * Called every tick to check for weather and time changes.
     * Lightweight checks with cooldowns to prevent spam.
     */
    public void tick() {
        long now = System.currentTimeMillis();

        // Check weather changes periodically
        if (now - lastWeatherCheckTime > 5000) {  // Check every 5 seconds
            checkWeatherChanges();
            lastWeatherCheckTime = now;
        }

        // Check time milestones periodically
        if (now - lastTimeCheckTime > 10000) {  // Check every 10 seconds
            checkTimeMilestones();
            lastTimeCheckTime = now;
        }
    }

    /**
     * Checks for weather transitions and triggers reactions.
     */
    private void checkWeatherChanges() {
        Level level = worker.level();
        if (level.isClientSide) {
            return;
        }

        WeatherState currentWeather = detectWeatherState(level);

        // Weather transition detected
        if (currentWeather != lastWeatherState) {
            LOGGER.debug("Weather transition: {} -> {}", lastWeatherState, currentWeather);
            handleWeatherTransition(lastWeatherState, currentWeather);
            lastWeatherState = currentWeather;
        }
    }

    /**
     * Detects the current weather state from the level.
     *
     * @param level The Minecraft level
     * @return Current weather state
     */
    private WeatherState detectWeatherState(Level level) {
        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();

        if (isThundering) {
            return WeatherState.THUNDERSTORM;
        } else if (isRaining) {
            // Check if snow (cold biome)
            if (level.getBiome(worker.blockPosition()).value().getBaseTemperature() < 0.15f) {
                return WeatherState.SNOW;
            }
            return WeatherState.RAIN;
        } else {
            return WeatherState.CLEAR;
        }
    }

    /**
     * Handles a weather transition by generating appropriate dialogue.
     *
     * @param from Previous weather state
     * @param to Current weather state
     */
    private void handleWeatherTransition(WeatherState from, WeatherState to) {
        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastWeatherCheckTime < WEATHER_COOLDOWN) {
            return;
        }

        // Get dialogue template for this transition
        String dialogue = weatherTemplates.getTransitionDialogue(
            from, to, specialization, personality
        );

        if (dialogue != null && !dialogue.isEmpty()) {
            // Adjust based on personality
            dialogue = adjustForPersonality(dialogue, to);

            // Speak the dialogue
            worker.sendChatMessage(dialogue);
            LOGGER.info("{} reacted to weather transition ({} -> {}): {}",
                worker.getSteveName(), from, to, dialogue);
        }
    }

    /**
     * Checks for time-of-day milestones and triggers reactions.
     */
    private void checkTimeMilestones() {
        Level level = worker.level();
        if (level.isClientSide) {
            return;
        }

        long dayTime = level.getDayTime() % 24000;
        TimeOfDay currentTime = TimeOfDay.fromGameTime(dayTime);

        // Time milestone reached
        if (currentTime != lastTimeNotified && currentTime != TimeOfDay.NONE) {
            LOGGER.debug("Time milestone: {}", currentTime);
            handleTimeMilestone(currentTime);
            lastTimeNotified = currentTime;
        }
    }

    /**
     * Handles a time-of-day milestone by generating appropriate dialogue.
     *
     * @param timeOfDay The current time milestone
     */
    private void handleTimeMilestone(TimeOfDay timeOfDay) {
        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastTimeCheckTime < TIME_COOLDOWN) {
            return;
        }

        // Get dialogue template for this time
        String dialogue = timeTemplates.getTimeDialogue(
            timeOfDay, specialization, personality
        );

        if (dialogue != null && !dialogue.isEmpty()) {
            // Adjust based on circadian rhythm (personality.energyLevel)
            dialogue = adjustForCircadianRhythm(dialogue, timeOfDay);

            // Speak the dialogue
            worker.sendChatMessage(dialogue);
            LOGGER.info("{} reacted to time milestone ({}): {}",
                worker.getSteveName(), timeOfDay, dialogue);
        }
    }

    /**
     * Adjusts dialogue based on personality traits for weather reactions.
     *
     * @param baseDialogue The base dialogue template
     * @param weather Current weather state
     * @return Personality-adjusted dialogue
     */
    private String adjustForPersonality(String baseDialogue, WeatherState weather) {
        // Extraversion: High extraversion loves sun, low loves rain
        if (personality.extraversion > 0.7 && weather == WeatherState.CLEAR) {
            return baseDialogue + " I love this weather!";
        } else if (personality.extraversion < 0.3 && weather == WeatherState.RAIN) {
            return baseDialogue + " Finally, some peace and quiet.";
        }

        // Neuroticism: High neuroticism worries about storms
        if (personality.neuroticism > 0.7 && weather == WeatherState.THUNDERSTORM) {
            return baseDialogue.replace("!", "... I don't like this.");
        }

        // Openness: High openness comments on weather beauty
        if (personality.openness > 0.7) {
            return addWeatherObservation(baseDialogue, weather);
        }

        return baseDialogue;
    }

    /**
     * Adjusts dialogue based on circadian rhythm for time reactions.
     *
     * @param baseDialogue The base dialogue template
     * @param timeOfDay Current time of day
     * @return Circadian-adjusted dialogue
     */
    private String adjustForCircadianRhythm(String baseDialogue, TimeOfDay timeOfDay) {
        // Morning people vs. night owls
        boolean isMorningPerson = personality.conscientiousness > 0.5;

        switch (timeOfDay) {
            case DAWN:
                if (isMorningPerson) {
                    return baseDialogue + " Ready to be productive!";
                } else {
                    return baseDialogue + " ...too early...";
                }
            case NOON:
                // Peak energy time for most
                return baseDialogue + " Energy's high!";
            case MIDNIGHT:
                if (!isMorningPerson) {
                    return baseDialogue + " This is my time.";
                } else {
                    return baseDialogue + " Should be sleeping...";
                }
            default:
                return baseDialogue;
        }
    }

    /**
     * Adds weather-specific observations for high-openness personalities.
     *
     * @param dialogue The base dialogue
     * @param weather Current weather
     * @return Enhanced dialogue
     */
    private String addWeatherObservation(String dialogue, WeatherState weather) {
        return switch (weather) {
            case RAIN -> dialogue + " The way the water transforms everything... fascinating.";
            case THUNDERSTORM -> dialogue + " Nature's power is incredible, isn't it?";
            case SNOW -> dialogue + " Every snowflake's unique. Have you ever really looked?";
            case CLEAR -> dialogue + " The light's beautiful today.";
        };
    }

    /**
     * Forces a weather reaction immediately (bypasses cooldowns).
     * Used for important weather events that should always be commented on.
     *
     * @param weather The weather state to react to
     */
    public void forceWeatherReaction(WeatherState weather) {
        String dialogue = weatherTemplates.getWeatherReaction(weather, specialization, personality);
        if (dialogue != null && !dialogue.isEmpty()) {
            worker.sendChatMessage(dialogue);
        }
    }

    /**
     * Forces a time-of-day reaction immediately (bypasses cooldowns).
     *
     * @param timeOfDay The time milestone to react to
     */
    public void forceTimeReaction(TimeOfDay timeOfDay) {
        String dialogue = timeTemplates.getTimeDialogue(timeOfDay, specialization, personality);
        if (dialogue != null && !dialogue.isEmpty()) {
            worker.sendChatMessage(dialogue);
        }
    }

    /**
     * Weather states in Minecraft.
     */
    public enum WeatherState {
        CLEAR,           // Sunny/clear skies
        RAIN,            // Rain (non-thunder)
        THUNDERSTORM,    // Thunder and lightning
        SNOW             // Snow (in cold biomes)
    }

    /**
     * Time-of-day milestones for dialogue triggers.
     */
    public enum TimeOfDay {
        NONE(0),         // Not a milestone
        DAWN(1000),      // 6:00 AM game time
        NOON(6000),      // 12:00 PM game time
        DUSK(13000),     // 7:00 PM game time
        MIDNIGHT(18000); // 12:00 AM game time

        private final int gameTimeThreshold;

        TimeOfDay(int gameTimeThreshold) {
            this.gameTimeThreshold = gameTimeThreshold;
        }

        /**
         * Converts game time to TimeOfDay milestone.
         *
         * @param dayTime Game time (0-24000)
         * @return Current time milestone
         */
        public static TimeOfDay fromGameTime(long dayTime) {
            if (dayTime >= 0 && dayTime < 2000) {
                return DAWN;
            } else if (dayTime >= 5000 && dayTime < 7000) {
                return NOON;
            } else if (dayTime >= 12000 && dayTime < 14000) {
                return DUSK;
            } else if (dayTime >= 18000 && dayTime < 20000) {
                return MIDNIGHT;
            }
            return NONE;
        }
    }
}
```

### WeatherDialogueTemplates Class

```java
package com.minewright.dialogue;

import com.minewright.entity.worker.SpecializationType;
import com.minewright.memory.PersonalityProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Template library for weather-based dialogue.
 * Organizes 50+ dialogue templates by weather transition and specialization.
 */
public class WeatherDialogueTemplates {

    private final Random random = new Random();

    // Weather transition templates organized by (from, to, specialization)
    private final Map<String, String[]> transitionTemplates = new HashMap<>();

    public WeatherDialogueTemplates() {
        initializeTransitionTemplates();
    }

    /**
     * Gets dialogue for a weather transition.
     *
     * @param from Previous weather
     * @param to Current weather
     * @param specialization Worker specialization
     * @param personality Worker personality
     * @return Appropriate dialogue, or null if none available
     */
    public String getTransitionDialogue(WeatherTimeReactionManager.WeatherState from,
                                      WeatherTimeReactionManager.WeatherState to,
                                      SpecializationType specialization,
                                      PersonalityProfile personality) {
        String key = from + "_TO_" + to + "_" + specialization;
        String[] templates = transitionTemplates.get(key);

        if (templates == null || templates.length == 0) {
            return null;
        }

        // Select random template
        return templates[random.nextInt(templates.length)];
    }

    /**
     * Gets dialogue for ongoing weather conditions.
     *
     * @param weather Current weather
     * @param specialization Worker specialization
     * @param personality Worker personality
     * @return Appropriate dialogue
     */
    public String getWeatherReaction(WeatherTimeReactionManager.WeatherState weather,
                                    SpecializationType specialization,
                                    PersonalityProfile personality) {
        String key = weather + "_ONGOING_" + specialization;
        String[] templates = transitionTemplates.get(key);

        if (templates == null || templates.length == 0) {
            return null;
        }

        return templates[random.nextInt(templates.length)];
    }

    /**
     * Initializes all weather transition templates.
     */
    private void initializeTransitionTemplates() {
        // CLEAR TO RAIN
        addTemplates("CLEAR_TO_RAIN_MINER",
            "Rain's starting. Perfect excuse to go underground. Surface is overrated anyway.",
            "Finally! Some weather that makes sense. Down the shaft we go.",
            "Good. Maybe everyone else will go inside and leave the caves to me."
        );
        addTemplates("CLEAR_TO_RAIN_BUILDER",
            "Rain delays construction. Safety first, I suppose. Annoying.",
            "Can't build in this. Materials get waterlogged, placement gets sloppy.",
            "Well, there goes my schedule. Moisture affects the mortar integrity."
        );
        addTemplates("CLEAR_TO_RAIN_GUARD",
            "Rain starting. Visibility decreasing. Perimeter checks becoming critical.",
            "Reduced visibility conditions. Stay close, I've got your back.",
            "Wet weather makes footing treacherous. Watch your step on patrols."
        );
        addTemplates("CLEAR_TO_RAIN_SCOUT",
            "Rain! Everything looks different in the rain, you know? Mysterious!",
            "The world transforms when it rains! Colors deepen, sounds change!",
            "Storm scouting! Best kind. Nobody else is out, discoveries await!"
        );
        addTemplates("CLEAR_TO_RAIN_FARMER",
            "Oh, beautiful! The crops were getting thirsty. Perfect timing.",
            "Rain! Nature's blessing! The wheat will love this!",
            "Finally! The soil's been so dry. This is exactly what we needed."
        );
        addTemplates("CLEAR_TO_RAIN_ARTISAN",
            "Indoor weather. Workshop time. No distractions from sunshine.",
            "Perfect conditions for focused crafting. Darker, calmer, efficient.",
            "Rain creates the right acoustic environment for precision work."
        );

        // RAIN TO CLEAR
        addTemplates("RAIN_TO_CLEAR_MINER",
            "Sun's back. Ugh. Was nice having the underground to myself.",
            "Sky's clearing. Everyone's going to want to be outside now. Crowds.",
            "Rain was nice while it lasted. Dark, damp, PROPER weather."
        );
        addTemplates("RAIN_TO_CLEAR_BUILDER",
            "Sun's out! We can resume construction! Quality demands it!",
            "Materials are dry, visibility is good. Building time!",
            "Excellent. Weather conditions optimal for structural work."
        );
        addTemplates("RAIN_TO_CLEAR_GUARD",
            "Visibility restored. Perimeter checks easier in sunlight.",
            "Clear weather means hostiles have fewer hiding spots. Good.",
            "Rain was useful training. Clear weather's easier patrolling."
        );
        addTemplates("RAIN_TO_CLEAR_SCOUT",
            "Sun's back! Adventure visibility restored! Let's GO!",
            "The colors! Everything's so VIBRANT after rain! Beautiful!",
            "Perfect! Clear skies mean I can scout that mountain range today!"
        );
        addTemplates("RAIN_TO_CLEAR_FARMER",
            "Sun's back. Plants got their drink, now they need their light.",
            "Perfect cycle. Rain for water, sun for growth. Nature's balance.",
            "Good rain, but crops need light too. Can't have one without the other."
        );
        addTemplates("RAIN_TO_CLEAR_ARTISAN",
            "Sun's distracting, but at least the redstone will be visible.",
            "Workshop conditions degrading. Too much light for focus work.",
            "Could use the sunlight for solar-powered smelting array. Efficiency."
        );

        // CLEAR TO STORM
        addTemplates("CLEAR_TO_THUNDERSTORM_MINER",
            "THUNDER! Perfect weather for deep mining. Nobody's going to bother us underground!",
            "Storm above means safety below. Let's get DEEP.",
            "Lightning can't reach us down here. Best kind of weather."
        );
        addTemplates("CLEAR_TO_THUNDERSTORM_BUILDER",
            "STORM! Secure the site! Check structural integrity!",
            "Lightning is a fire hazard. All construction halted!",
            "This wind... overhangs might not hold. Everyone inside!"
        );
        addTemplates("CLEAR_TO_THUNDERSTORM_GUARD",
            "STORM ALERT! Everyone inside NOW! This is dangerous!",
            "Thunder means lightning strikes. Perimeter compromised!",
            "Visibility near zero. Hostiles spawning. WEAPONS READY!"
        );
        addTemplates("CLEAR_TO_THUNDERSTORM_SCOUT",
            "THUNDERSTORM! This is INCREDIBLE! The power of nature!",
            "Look at those clouds! The lightning! It's like the sky is alive!",
            "Dramatic weather makes for dramatic discoveries! I'm going scouting!"
        );
        addTemplates("CLEAR_TO_THUNDERSTORM_FARMER",
            "Oh my. The plants... the wind might damage the crops!",
            "Thunder... the animals are frightened. I need to check on them.",
            "Nature's anger is beautiful but dangerous. Shelters, everyone!"
        );
        addTemplates("CLEAR_TO_THUNDERSTORM_ARTISAN",
            "Lightning energy potential... could harness this. Technical challenge.",
            "Indoor weather enforced. Workshop it is.",
            "Thunder's acoustically interesting. Reverberation patterns..."
        );

        // SNOW BEGINNING
        addTemplates("CLEAR_TO_SNOW_MINER",
            "Snow. Cold, wet, annoying. At least caves stay warm underground.",
            "Temperature's dropping. Good excuse to stay below. Warm stone.",
            "Snow's just frozen rain. Still wet. Still annoying for mining trips."
        );
        addTemplates("CLEAR_TO_SNOW_BUILDER",
            "Snow covering everything. Can't even see the foundation lines.",
            "Building in cold is miserable. Materials get brittle, fingers get numb.",
            "Frozen blocks... interesting texture possibilities. Practical issues though."
        );
        addTemplates("CLEAR_TO_SNOW_GUARD",
            "Snowfall means footprints. Tracking hostile movements becomes easier.",
            "Cold weather affects mobility. Equipment checks critical.",
            "Snow provides visual cover for hostiles. Enhanced vigilance required."
        );
        addTemplates("CLEAR_TO_SNOW_SCOUT",
            "SNOW! The world's transformed! Everything's white and NEW!",
            "Frozen wonderland! Every step leaves a mark! I'm mapping this!",
            "Snowstorms create temporary terrain features! Need to document!"
        );
        addTemplates("CLEAR_TO_SNOW_FARMER",
            "Snow means winter dormancy. Plants sleep, we wait.",
            "Cold kills the pests. Nature's way of resetting the garden.",
            "The wheat's dormant under there. Waiting for spring's warmth."
        );
        addTemplates("CLEAR_TO_SNOW_ARTISAN",
            "Cold affects metal tempering. Different techniques required.",
            "Ice blocks for construction? Interesting material property.",
            "Frozen crafting materials behave differently. Adaptation required."
        );

        // ONGOING THUNDERSTORM
        addTemplates("THUNDERSTORM_ONGOING_MINER",
            "Thunder raging above, peace below. This is perfect mining weather.",
            "Lightning can't touch us down here. Safe and sound.",
            "Let the sky throw its tantrum. Stone doesn't care."
        );
        addTemplates("THUNDERSTORM_ONGOING_BUILDER",
            "STORM! All construction halted! Fire hazard from lightning!",
            "Secure the site! Check all wooden structures for ignition!",
            "Quality means knowing when to STOP. This is unsafe weather."
        );
        addTemplates("THUNDERSTORM_ONGOING_GUARD",
            "THUNDERSTORM! Maximum threat level! All crew inside!",
            "Lightning spawn! Hostiles everywhere! PERIMETER BREACHED!",
            "Stay CLOSE! I can't protect you if you wander in this!"
        );
        addTemplates("THUNDERSTORM_ONGOING_SCOUT",
            "THUNDER! The sky is ANGRY and it's MAGNIFICENT!",
            "Lightning strikes are nature's fireworks! I'm watching!",
            "Scouting in a storm? DANGEROUS! Which means EXCITING!"
        );
        addTemplates("THUNDERSTORM_ONGOING_FARMER",
            "Storm's too violent. Crops might be damaged by hail.",
            "Thunder frightens the animals. Need to check their shelters.",
            "This much rain... flooding risk. Drainage needs checking."
        );
        addTemplates("THUNDERSTORM_ONGOING_ARTISAN",
            "Lightning energy... could harness. Technical challenge noted.",
            "Indoor enforced. Workshop it is. Uninterrupted crafting.",
            "Thunder's acoustic properties. Reverberation data collection."
        );
    }

    /**
     * Adds templates to the library.
     */
    private void addTemplates(String key, String... templates) {
        transitionTemplates.put(key, templates);
    }
}
```

### TimeDialogueTemplates Class

```java
package com.minewright.dialogue;

import com.minewright.entity.worker.SpecializationType;
import com.minewright.memory.PersonalityProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Template library for time-of-day dialogue.
 * Organizes dialogue templates by time milestone and specialization.
 */
public class TimeDialogueTemplates {

    private final Random random = new Random();

    // Time-based templates organized by (timeOfDay, specialization)
    private final Map<String, String[]> timeTemplates = new HashMap<>();

    public TimeDialogueTemplates() {
        initializeTimeTemplates();
    }

    /**
     * Gets dialogue for a time-of-day milestone.
     *
     * @param timeOfDay The time milestone
     * @param specialization Worker specialization
     * @param personality Worker personality
     * @return Appropriate dialogue, or null if none available
     */
    public String getTimeDialogue(WeatherTimeReactionManager.TimeOfDay timeOfDay,
                                 SpecializationType specialization,
                                 PersonalityProfile personality) {
        String key = timeOfDay + "_" + specialization;
        String[] templates = timeTemplates.get(key);

        if (templates == null || templates.length == 0) {
            return null;
        }

        return templates[random.nextInt(templates.length)];
    }

    /**
     * Initializes all time-of-day templates.
     */
    private void initializeTimeTemplates() {
        // DAWN
        addTemplates("DAWN_MINER",
            "Dawn. Sun's coming up. Ugh. Let's get underground before it gets too bright.",
            "Morning means the overworld wakes up. Underground stays peaceful.",
            "Early shift. Best time for mining before the crowds show up."
        );
        addTemplates("DAWN_BUILDER",
            "Dawn breaks. New day, new projects. Let's build something magnificent.",
            "Morning light reveals all the imperfections in yesterday's work. Fixable.",
            "Fresh day, fresh blueprints. Time to create something lasting."
        );
        addTemplates("DAWN_GUARD",
            "Morning briefing. Night shift survived. Dawn patrol beginning.",
            "Sun's up. Hostiles retreating. Perimeter secure for now.",
            "New day, new vigilance. Can't relax just because it's morning."
        );
        addTemplates("DAWN_SCOUT",
            "DAWN! A whole new day of discoveries awaits! Let's go!",
            "The horizon's waking up! Colors changing! Something new to see!",
            "Early bird gets the discovery! I'm already ahead!"
        );
        addTemplates("DAWN_FARMER",
            "Morning light wakes the garden. Plants stretching toward the sun.",
            "Dawn is nature's alarm clock. Time to tend the crops.",
            "Early morning is best for harvesting. Dew's still on the leaves."
        );
        addTemplates("DAWN_ARTISAN",
            "Morning. Fresh mind, fresh crafts. Ready to create.",
            "Dawn light's good for detail work. Precision requires visibility.",
            "New day, new techniques to refine. Improvement never rests."
        );

        // NOON
        addTemplates("NOON_MINER",
            "Noon. Worst time of day. Sun's directly overhead, casting ugly shadows.",
            "Midday means surface is BRIGHT. Underground is looking VERY appealing.",
            "Solar zenith. Perfect time to be deep underground where it doesn't matter."
        );
        addTemplates("NOON_BUILDER",
            "Noon sun. Best light for inspection. Quality assessment time.",
            "Peak daylight hours. Optimal conditions for precision building.",
            "Sun overhead means minimal shadows. Perfect for checking alignment."
        );
        addTemplates("NOON_GUARD",
            "Noon patrol. Hostiles retreat from bright light. Easier watch.",
            "Peak visibility. Perimeter checks are most effective now.",
            "Sun's high. Shadows minimal. Hostiles have fewer hiding spots."
        );
        addTemplates("NOON_SCOUT",
            "Noon sun! Maximum visibility! I can see EVERYTHING!",
            "Peak daylight means I can scout that distant ridge!",
            "Brightest time of day! Perfect for documenting discoveries!"
        );
        addTemplates("NOON_FARMER",
            "Noon sun. Plants are photosynthesizing at maximum rate.",
            "Midday heat. Crops might need water check.",
            "Brightest light means fastest growth. Nature in overdrive."
        );
        addTemplates("NOON_ARTISAN",
            "Noon light. Harsh shadows. Not ideal for detail work.",
            "Peak energy time. Tackling the complex recipes now.",
            "Optimal mental state. Perfect for redstone circuit design."
        );

        // DUSK
        addTemplates("DUSK_MINER",
            "Dusk. Sun's going down. FINALLY. My favorite time of day.",
            "Evening coming. Shadows lengthening. Underground time approaching.",
            "Golden hour makes stone look warm. Nice. Evening's better though."
        );
        addTemplates("DUSK_BUILDER",
            "Dusk lighting reveals structure silhouettes. Beautiful time.",
            "Evening golden light. Makes even cobblestone look elegant.",
            "Sun's going down. Time to assess today's progress. Not bad."
        );
        addTemplates("DUSK_GUARD",
            "Dusk. Hostiles will spawn soon. Weapons ready.",
            "Evening means night watch preparation. Perimeter checks critical.",
            "Sun's setting. Visibility decreasing. Enhanced vigilance required."
        );
        addTemplates("DUSK_SCOUT",
            "Dusk colors! The sunset's painting the sky! Beautiful!",
            "Evening means the night discoveries are waiting! Exciting!",
            "Golden hour lighting makes landscapes magical! Documenting this!"
        );
        addTemplates("DUSK_FARMER",
            "Evening coming. Plants settling for the night. Growth cycle pausing.",
            "Dusk is harvest time. Crops gathered before night.",
            "Sun's going down. The garden's preparing for rest. Natural rhythm."
        );
        addTemplates("DUSK_ARTISAN",
            "Evening light changing. Workshop illumination adjustment needed.",
            "Dusk means the day's crafting is ending. Assessment time.",
            "Twilight's good for reviewing progress. Tomorrow's improvements."
        );

        // MIDNIGHT
        addTemplates("MIDNIGHT_MINER",
            "Midnight. Perfect. Everyone else is asleep. The caves are mine.",
            "Deep night. Deeper underground. This is peaceful.",
            "World's asleep. Stone never sleeps. Good mining time."
        );
        addTemplates("MIDNIGHT_BUILDER",
            "Midnight. Too dark to build safely. Quality suffers in poor light.",
            "Night's when I review plans. Tomorrow's improvements.",
            "Can't see well enough for precision work. Planning time instead."
        );
        addTemplates("MIDNIGHT_GUARD",
            "Midnight. Peak hostile activity. Maximum alert.",
            "Night watch. Nothing touches this crew on my watch.",
            "Darkest hour. Hostiles everywhere. Perimeter holding."
        );
        addTemplates("MIDNIGHT_SCOUT",
            "Midnight! The night world is ALIVE! Different discoveries await!",
            "Darkness doesn't stop exploration! It just changes what we see!",
            "Sleep? Sleep's for people who don't want to discover the night!"
        );
        addTemplates("MIDNIGHT_FARMER",
            "Midnight. Garden's asleep. Quiet time.",
            "Night's when crops rest. No growth, but no danger either.",
            "Darkness means the plants are safe from pests. Peaceful."
        );
        addTemplates("MIDNIGHT_ARTISAN",
            "Midnight. Workshop's quietest. Focused crafting time.",
            "Night owl hours. Best redstone work happens now.",
            "No daylight distractions. Pure focus on technical precision."
        );
    }

    /**
     * Adds templates to the library.
     */
    private void addTemplates(String key, String... templates) {
        timeTemplates.put(key, templates);
    }
}
```

---

## Integration with Existing Systems

### ForemanEntity Integration

```java
// In ForemanEntity.java

public class ForemanEntity extends PathfinderMob {
    // ... existing fields ...

    private WeatherTimeReactionManager weatherReactionManager;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // ... existing code ...
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // ... existing tick logic ...

            // Tick the weather/time reaction manager
            if (weatherReactionManager != null) {
                weatherReactionManager.tick();
            }
        }
    }

    /**
     * Gets the weather/time reaction manager.
     */
    public WeatherTimeReactionManager getWeatherReactionManager() {
        return weatherReactionManager;
    }

    /**
     * Sets up the weather/time reaction manager.
     * Called during entity initialization.
     */
    private void setupWeatherReactionManager() {
        this.weatherReactionManager = new WeatherTimeReactionManager(this);
    }
}
```

### ProactiveDialogueManager Integration

```java
// In ProactiveDialogueManager.java

public class ProactiveDialogueManager {
    // ... existing code ...

    private WeatherTimeReactionManager weatherReactionManager;

    public ProactiveDialogueManager(ForemanEntity minewright) {
        this.minewright = minewright;
        // ... existing initialization ...

        // Initialize weather/time reaction manager
        this.weatherReactionManager = minewright.getWeatherReactionManager();
    }

    /**
     * Checks weather-related triggers.
     * Delegates to WeatherTimeReactionManager.
     */
    private void checkWeatherTriggers() {
        // Weather checks are now handled by WeatherTimeReactionManager
        // This method can remain for backward compatibility
        if (weatherReactionManager != null) {
            // Weather reactions are handled in the manager's tick()
            // This method can be used for force-triggering reactions if needed
        }
    }

    /**
     * Checks time-based triggers.
     * Delegates to WeatherTimeReactionManager for time milestones.
     */
    private void checkTimeBasedTriggers() {
        // Time milestone checks are now handled by WeatherTimeReactionManager
        // This method can retain other time-based logic (idle checks, etc.)
        Level level = minewright.level();
        long dayTime = level.getDayTime() % 24000;

        // Idle comment (if idle for too long)
        if (!minewright.getActionExecutor().isExecuting() && ticksSinceLastComment > 1200) {
            if (canTrigger("idle_long", 6000)) {
                triggerComment("idle_long", "Been idle a while");
            }
        }
    }
}
```

### Configuration Options

```java
// In MineWrightConfig.java

public class MineWrightConfig {
    // ... existing config ...

    // Weather/Time Reaction Settings
    public static final BooleanSpec WEATHER_REACTIONS_ENABLED = ConfigSpec.builder()
        .comment("Enable weather and time-based reactions for MineWright workers")
        .define("weatherReactions.enabled", true);

    public static final IntSpec WEATHER_REACTION_COOLDOWN = ConfigSpec.builder()
        .comment("Minimum time (in seconds) between weather reaction comments")
        .defineInRange("weatherReactions.cooldownSeconds", 30, 10, 300);

    public static final IntSpec TIME_REACTION_COOLDOWN = ConfigSpec.builder()
        .comment("Minimum time (in seconds) between time milestone comments")
        .defineInRange("weatherReactions.timeCooldownSeconds", 60, 30, 600);

    public static final BooleanSpec EXTREME_WEATHER_ALWAYS = ConfigSpec.builder()
        .comment("Always react to extreme weather (thunderstorms, blizzards) ignoring cooldowns")
        .define("weatherReactions.extremeWeatherAlways", true);
}
```

---

## Testing and Validation

### Unit Tests

```java
@Test
public void testWeatherTransition_ClearToRain() {
    // Setup
    ForemanEntity miner = createTestWorker(SpecializationType.MINER);
    WeatherTimeReactionManager manager = miner.getWeatherReactionManager();

    // Simulate weather transition
    when(miner.level().isRaining()).thenReturn(true);
    when(miner.level().isThundering()).thenReturn(false);

    // Trigger transition
    manager.tick();

    // Verify reaction
    verify(miner).sendChatMessage(contains("underground"));
}

@Test
public void testTimeMilestone_Dawn() {
    // Setup
    ForemanEntity scout = createTestWorker(SpecializationType.SCOUT);
    WeatherTimeReactionManager manager = scout.getWeatherReactionManager();

    // Simulate dawn (game time 1000)
    when(scout.level().getDayTime()).thenReturn(1000L);

    // Trigger milestone
    manager.tick();

    // Verify reaction
    verify(scout).sendChatMessage(contains("DAWN"));
}

@Test
public void testPersonalityAdjustment_Extraversion_ClearWeather() {
    // Setup
    PersonalityProfile highExtraversion = new PersonalityProfile();
    highExtraversion.extraversion = 0.8;

    ForemanEntity worker = createTestWorker(SpecializationType.SCOUT, highExtraversion);
    WeatherTimeReactionManager manager = worker.getWeatherReactionManager();

    // Simulate clear weather
    when(worker.level().isRaining()).thenReturn(false);

    // Trigger reaction
    manager.forceWeatherReaction(WeatherState.CLEAR);

    // Verify extraverted reaction
    verify(worker).sendChatMessage(contains("love"));
}

@Test
public void testCooldown_Respected() {
    // Setup
    ForemanEntity worker = createTestWorker(SpecializationType.MINER);
    WeatherTimeReactionManager manager = worker.getWeatherReactionManager();

    // Trigger first reaction
    manager.forceWeatherReaction(WeatherState.RAIN);
    verify(worker, times(1)).sendChatMessage(anyString());

    // Attempt second reaction immediately (within cooldown)
    manager.forceWeatherReaction(WeatherState.RAIN);

    // Verify cooldown prevented second reaction
    verify(worker, times(1)).sendChatMessage(anyString());
}
```

### Integration Tests

```java
@Test
public void testFullDayCycle_AllMilestonesTriggered() {
    // Setup
    ForemanEntity worker = spawnTestWorker();
    Level level = worker.level();

    // Simulate full day cycle
    long[] milestoneTimes = {1000L, 6000L, 13000L, 18000L}; // Dawn, Noon, Dusk, Midnight

    for (long time : milestoneTimes) {
        when(level.getDayTime()).thenReturn(time);
        tickMultipleTimes(worker, 100); // Advance ticks
    }

    // Verify all milestones triggered
    verify(worker, atLeastOnce()).sendChatMessage(contains("Dawn"));
    verify(worker, atLeastOnce()).sendChatMessage(contains("Noon"));
    verify(worker, atLeastOnce()).sendChatMessage(contains("Dusk"));
    verify(worker, atLeastOnce()).sendChatMessage(contains("Midnight"));
}

@Test
public void testStormSequence_ReactionsAppropriate() {
    // Setup
    ForemanEntity builder = spawnTestWorker(SpecializationType.BUILDER);
    Level level = builder.level();

    // Clear -> Storm
    when(level.isRaining()).thenReturn(false);
    tickMultipleTimes(builder, 100);

    when(level.isRaining()).thenReturn(true);
    when(level.isThundering()).thenReturn(true);
    tickMultipleTimes(builder, 100);

    // Verify builder reaction to storm
    verify(builder).sendChatMessage(contains("STORM") | contains("halted"));
}
```

---

## Sources

### Research Sources Consulted

**Weather and Mood Psychology:**
- [Temperature influences mood: evidence from 11 years of Baidu data](https://pmc.ncbi.nlm.nih.gov/articles/PMC12119585/) - PMC article on temperature-mood correlation
- [Applied Psychology: Evening weather good for work](http://gameinstitute.qq.com/knowledge/100042) - German research on morning weather effects
- [Seasonal productivity science](https://m.toutiao.com/article/7576860198011273737/) - Chinese research on seasonal work patterns
- [Weather affects our mood](https://mzujuan.xkw.com/12q21442512.html) - General weather psychology overview

**Circadian Rhythm Research:**
- Peak productivity at 11 AM (Redbooth study)
- 75% of people most alert 9-11 AM
- 2-3 PM lowest energy point of day
- "Early birds" vs. "night owls" performance patterns

**Minecraft Weather System:**
- [Weather - Minecraft Wiki](https://minecraft-archive.fandom.com/wiki/Weather) - Complete weather mechanics
- Thunderstorm 1.44% chance per tick
- Rain lasts 0.5-1 game day
- Light levels: Clear (15), Rain (12), Thunder (5)

**Game NPC Dialogue Systems:**
- [MMORPG Weather System Design](http://gameinstitute.qq.com/knowledge/100042) - Tencent Game Institute on weather immersion
- [Stardew Valley Weather Dialogue](https://stardewvalleywiki.com/Modding:Dialogue) - Character-specific weather reactions
- [Living Open World Games](https://m.163.com/dy/article/KMFT9S1C0526K1KN.html) - NPC weather behavior analysis

**Internal Documentation:**
- [MASTER_CHARACTER_GUIDE.md](C:\Users\casey\steve\docs\characters\MASTER_CHARACTER_GUIDE.md) - Character personality profiles
- [WORKER_SPECIALIZATION_DIALOGUE.md](C:\Users\casey\steve\docs\characters\WORKER_SPECIALIZATION_DIALOGUE.md) - Role-specific dialogue patterns
- [ProactiveDialogueManager.java](C:\Users\casey\steve\src\main\java\com\minewright\dialogue\ProactiveDialogueManager.java) - Existing dialogue system

---

## Document Version Control

**Version:** 1.0
**Date:** 2026-02-27
**Author:** MineWright Development Team
**Status:** Complete - Ready for Implementation

**Change Log:**
- v1.0 (2026-02-27): Initial comprehensive weather and time reaction system

**Next Review:** After playtesting with weather/time reactions

---

## Appendix: Quick Reference

### Weather Reaction Matrix

| Weather | Miner | Builder | Guard | Scout | Farmer | Artisan |
|---------|-------|---------|-------|-------|--------|---------|
| **Clear → Rain** | ❤️ Underground | ⚠️ Delayed | ⚠️ Visibility | ✨ Mystery | 💧 Water | 🔧 Workshop |
| **Rain → Clear** | 😠 Bright | ✅ Resume | ✅ Visibility | 🌈 Vibrant | ☀️ Growth | 😒 Distracting |
| **Clear → Storm** | ❤️ Safe | 🚨 Halt | 🚨 Danger | 🤩 Exciting | 😱 Damage | ⚡ Energy |
| **Snow** | 😒 Cold | 🧊 Can't see | 👣 Footprints | 🏔️ Transformed | ❄️ Dormant | 🌡️ Technical |

### Time-of-Day Reaction Summary

| Time | Miner | Builder | Guard | Scout | Farmer | Artisan |
|------|-------|---------|-------|-------|--------|---------|
| **Dawn** | 😠 Too bright | ✅ New projects | 📋 Briefing | 🌅 Adventure | 🌱 Awakening | 🔨 Fresh start |
| **Noon** | 😡 Worst time | 🔍 Inspection | 👁️ Visibility | 👁️ See everything | 🌞 Growth | 😒 Harsh light |
| **Dusk** | ❤️ Favorite | 🌅 Silhouettes | 🌙 Hostiles coming | 🎨 Colors | 🌙 Resting | 💡 Adjust light |
| **Midnight** | ❤️ Peaceful | 📝 Planning | 🚨 Peak threat | 🌙 Night world | 😴 Sleep | 🔧 Focus work |

---

**End of Weather and Time-Based Reaction System Documentation**
