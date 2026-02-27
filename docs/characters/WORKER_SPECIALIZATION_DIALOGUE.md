# Worker Specialization Dialogue System

**Project:** MineWright - Minecraft Autonomous Agents
**Component:** Specialization-Based Dialogue System
**Version:** 1.0
**Date:** 2026-02-27

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Research Foundations](#research-foundations)
3. [Specialization Dialogue Profiles](#specialization-dialogue-profiles)
4. [Inter-Specialization Banter](#inter-specialization-banter)
5. [Context-Aware Dialogue Selection](#context-aware-dialogue-selection)
6. [Java Implementation](#java-implementation)
7. [Dialogue Template System](#dialogue-template-system)
8. [Testing and Validation](#testing-and-validation)

---

## Executive Summary

This document defines how the 6 MineWright worker archetypes (Miner, Builder, Guard, Scout, Farmer, Artisan) speak differently based on their specialized roles, drawing on real-world profession-based communication patterns, guild-specific language, and workplace role-based dialogue styles.

**Key Design Principles:**

1. **Authentic Profession Voice:** Each archetype uses terminology, slang, and communication patterns from their real-world equivalents
2. **Context-Specific Speech:** Workers comment on events relevant to their specialization
3. **Distinct Personality Layers:** Base personality modified by specialization traits
4. **Relationship-Modified Dialogue:** Speech evolves from formal (stranger) to intimate (partner) across 5 stages
5. **Inter-Specialization Dynamics:** Workers of different types banter based on compatible/incompatible pairings

---

## Research Foundations

### Profession-Based Dialogue Patterns from Research

Based on web research conducted 2026-02-27, the following patterns emerge:

**Military/Guard Speech:**
- Disciplined, hierarchical communication with ranks and protocols
- Brief, mission-focused dialogue with tactical terminology
- References to duty, honor, sacrifice
- Dark humor about mortality
- Examples from research: "Soldiers, heed my orders! For past glory, forward!" "Being a warrior if you retreat means the battle line retreats."

**Construction/Builder Speech:**
- Practical, direct communication about materials and methods
- References to blueprints, measurements, structural integrity
- Pride in craftsmanship and quality work
- Foreman/worker hierarchy with "job site" terminology
- Research terms: "foreman," "blueprint," "excavator," "wheelbarrow"

**Mining Speech:**
- Earthy, grounded language reflecting subterranean work
- Superstitious beliefs about caves, minerals, underground spirits
- References to depth, darkness, gems, ore veins
- Cautious, practical approach to life
- Mining terminology: "shaft," "vein," "seam," "drift," "motherlode"

**Agriculture/Farmer Speech:**
- Connection to natural cycles and weather patterns
- Patient, nurturing communication style
- References to soil, seasons, harvest, livestock
- Practical wisdom about growth and patience
- Agricultural terms: "sow," "reap," "plow," "irrigation," "fertilizer," "cultivate"

**Scout/Explorer Speech:**
- Enthusiastic discovery-oriented dialogue
- References to landmarks, distances, terrain features
- Restless when stationary, eager to move
- Directional and observational language
- Scout/ranger terminology: "reconnaissance," "patrol," "sign," "tracking," "observation"

**Craftsman/Artisan Speech:**
- Technical precision and pride in workmanship
- References to materials, tools, techniques, recipes
- Perfectionist attention to detail
- Appreciation for beauty and elegance in design
- Trade-specific language: "craftsman," "artisan," "master," "apprentice," "secrets of the trade"

### Jargon as Group Membership Signal

Research from Harvard Business Review identifies that:
- **Jargon** serves as shorthand that signals group membership
- **One person's slang is another's colloquialism** - varies by context
- Trade-specific language creates insider/outsider boundaries
- Examples: "ink stick" for pen signals military background, specialized construction terms signal builder identity

### Workplace Communication Patterns

Each profession has distinct communication styles:
- **Formality levels:** Military (high) vs. Creative arts (low)
- **Directness:** Construction (very direct) vs. Diplomatic roles (indirect)
- **Humor types:** Dark humor (dangerous jobs) vs. Light teasing (safe roles)
- **Authority expression:** Guards command, Artisans suggest, Miners state

---

## Specialization Dialogue Profiles

### 1. MINER ("The Excavator")

#### Core Vocabulary and Slang Patterns

**Mining Terminology:**
```
Primary Terms:
- "Drift" - Horizontal tunnel
- "Shaft" - Vertical tunnel
- "Vein" - Mineral deposit line
- "Motherlode" - Large mineral deposit
- "Face" - Working surface of mine
- "Back" - Ceiling of tunnel
- "Ribs" - Walls of tunnel
- "Stoping" - Extracting ore from walls
- "Winze" - Internal vertical shaft
- "Raise" - Excavated upward pass

Equipment References:
- "Pick," "Stick," "Steel" (pickaxe)
- "Timbers," "Shores," "Props" (support beams)
- "Cage," "Skip," "Hoist" (elevators)
- "Vent," "Air" (ventilation)

Slang/Idioms:
- "Hit paydirt" - Found valuable ore
- "In the black" - Profitable/lucky
- "Canary in the coal mine" - Warning sign
- "Down to the wire" - Close deadline
- "Rock dust" - Very fine particles
- "Beef" - Explosive charge
- "Muck" - Broken rock/ore
- "Waste" - Useless rock
```

#### Topics They Naturally Discuss

**Primary Interests:**
- Ore quality and mineral finds
- Tunnel stability and cave safety
- Depth and underground navigation
- Mining efficiency and technique
- Tool durability and maintenance
- Underground biomes and cave features

**Conversation Starters:**
```
"Found a nice vein of iron down at Y=58. Clean ore, too."
"These tunnels are getting unstable. Need more timbers."
"You can smell the copper before you see it. Earth tells you."
"Been working this face for three hours. Nice progress."
"Got my eye on a diamond patch. Just need to clear the gravel."
```

#### Metaphors and References They Use

**Mining-Based Metaphors:**
```
- "We're in deep now" (difficult situation)
- "Digging ourselves a hole" (making problem worse)
- "Hit rock bottom" (lowest point)
- "Making headway" (progress)
- "Chip away at it" (persistent effort)
- "Gold rush mentality" (eagerness)
- "Strike it rich" (sudden success)
- "Bedrock solid" (very reliable)
- "Prospecting for answers" (seeking information)
```

**Underground Imagery:**
- References to darkness, depth, pressure
- Cave superstitions and mining folklore
- Stone appreciation and geology knowledge
- Comfort with enclosed spaces vs. anxiety in open areas

#### Pet Peeves and Complaints Typical of Their Role

**Common Complaints:**
```
- Surface work ("Too bright up here," "Sky feels wrong")
- Inefficient mining ("You're wasting ores," "Wrong tunnel direction")
- Cave-ins and instability ("Roof's giving," "Unsafe supports")
- Poor lighting ("Can't see a thing," "Need more torches")
- Surface dwellers who don't understand underground dangers
- Building structures that ignore natural cave formations
- Lava near valuable ores
- Gravel and dirt interfering with mining
```

**Typical Frustrations:**
```
"Surface work feels... exposed. No roof over your head."
"Why build on top when there's perfectly good space below?"
"You call this a mine? It's a hole. There's a difference."
"Who placed this torch? Terrible lighting. I can't see the vein edges."
"Daylight's overrated. Give me a nice dark tunnel any day."
```

#### Celebratory Phrases for Job Completion

**Success Markers:**
```
"CLEAN STRIP! That's a solid twenty blocks of pure iron."
"Hit the motherlode! Look at that sparkle!"
"Stable tunnel, good support, clean extraction. Beautiful."
"Another face cleared. Mine's looking good."
"Through the hard part. Smooth sailing from here."
"Paydirt! Finally found the good stuff."
```

**Quality-Oriented:**
```
"Clean work. Minimal waste. That's how you mine."
"Vein's tapped dry. Not a scrap left behind."
"Tunnel's straight as an arrow. Professional grade."
```

#### Warning/Alert Patterns Specific to Their Work

**Danger Signals:**
```
"ROOF'S GIVING! Clear the tunnel, NOW!"
"Unsupported span! Don't cross until I timber it!"
"Lava breach! Section's flooded!"
"Gravel ceiling! Unsafe! Reroute!"
"Air's going stale. Ventilation's blocked."
"Cave sounds ahead. Something's shifting."
"Hear that drip? Water above means trouble below."
```

**Mining Warnings:**
```
"Don't break that support beam unless you want this whole drift on your head."
"Ore's running thin. This vein's played out."
"Gas pocket ahead. Don't use your pick."
"Bedrock's close. We're hitting bottom."
```

---

### 2. BUILDER ("The Architect")

#### Core Vocabulary and Slang Patterns

**Construction Terminology:**
```
Structural Terms:
- "Footing" - Base foundation
- "Framing" - Structural skeleton
- "Load-bearing" - Supports weight
- "Partition" - Interior wall
- "Cavity" - Hollow space in wall
- "Facade" - Front exterior
- "Cantilever" - Unsupported overhang
- "Lintel" - Beam over opening
- "Sill" - Bottom of window/door frame
- "Plumb" - Perfectly vertical
- "Level" - Perfectly horizontal
- "Square" - Perfect 90-degree angle

Equipment Terms:
- "T-square," "Level," "Plumb bob"
- "Scaffolding," "Staging"
- "Mud," "Mortar," "Grout"
- "Blueprints," "Plans," "Specs"

Slang/Idioms:
- "Rough in" - Initial installation
- "Finish work" - Final details
- "Square away" - Organize/complete
- "On the level" - Honest/upright
- "Plumb crazy" - Absolutely insane
- "Hammer out" - Work out details
- "Nail it" - Perfect execution
- "Build from the ground up" - Start properly
```

#### Topics They Naturally Discuss

**Primary Interests:**
- Architectural design and aesthetics
- Structural integrity and stability
- Material quality and appropriateness
- Building techniques and methods
- Blueprint reading and modification
- Famous structures and architectural styles
- Interior design and decoration

**Conversation Starters:**
```
"See those arches? That's proper masonry. Classic gothic influence."
"This foundation's solid. We could build five stories on this footing."
"The symmetry on this facade... it's almost perfect. Almost."
"I've been thinking about modifying the east wing. Want to see my sketches?"
"Wrong material for this load. Stone bricks only, nothing weaker."
```

#### Metaphors and References They Use

**Construction-Based Metaphors:**
```
- "Building castles in the air" (unrealistic plans)
- "Hit a wall" (obstacle)
- "Foundations crumbling" (base failing)
- "Roof over your head" (shelter/security)
- "Glass houses" (vulnerable)
- "House of cards" (unstable structure)
- "Brick by brick" (gradual progress)
- "Scaffold learning" (supportive structure)
- "Blueprint for success" (plan)
- "Measured approach" (careful method)
```

**Architectural Imagery:**
- References to famous buildings and architects
- Discussion of structural principles and physics
- Appreciation for design movements and styles
- Focus on permanence and legacy in construction

#### Pet Peeves and Complaints Typical of Their Role

**Common Complaints:**
```
- Ugly or inefficient designs ("Eyesore," "Waste of materials")
- Structural violations ("That's going to collapse," "Physics doesn't work that way")
- Poor material choices ("Wrong tool for the job," "Material mismatch")
- Inaccurate measurements ("Not square," "Not plumb," "Not level")
- Destructive players ("Why build beautiful things if you'll just destroy them?")
- Rushed work ("Quality takes time," "Measure twice, cut once")
- Asymmetry and chaos ("Where's the pattern?" "No rhyme or reason")
-Improper foundation work
```

**Typical Frustrations:**
```
"That's not going to hold. I'm telling you, it's structurally unsound."
"Who designed this? It's... inefficient. That's being polite."
"You call this a wall? It's a fence with identity issues."
"More cobblestone? Variety exists, you know. Try stone bricks sometime."
"Straight lines aren't that hard. USE A RULER."
```

#### Celebratory Phrases for Job Completion

**Success Markers:**
```
"Structure complete. Load-bearing walls test positive. Perfection."
"Another wall squared and leveled. Quality work."
"The symmetry on this build... it's elegant. Truly elegant."
"Foundation's set. We can build something lasting on this."
"Finished the detailing. The difference is in the details, you know."
```

**Pride in Craftsmanship:**
```
"Plumb, level, and square. The builder's trifecta."
"Materials match, lines are clean, structure is sound. I outdid myself."
"Not just a building. This is ARCHITECTURE."
```

#### Warning/Alert Patterns Specific to Their Work

**Danger Signals:**
```
"UNSAFE LOAD! That's not meant to support weight!"
"Foundation's compromised! Stop building!"
"Overhang's too far! Needs support or it collapses!"
"Wrong material for this application! Change it!"
"Measurements are off! This won't fit!"
"Weak point in the structure! Reinforce here!"
```

**Quality Warnings:**
```
"That's not plumb. The wall's leaning. Fix it before it falls."
"Not level. Water will pool here. Rework the grade."
"Material mismatch! Stone and wood have different properties!"
"Unreinforced span! Add arches or columns!"
```

---

### 3. GUARD ("The Protector")

#### Core Vocabulary and Slang Patterns

**Military Terminology:**
```
Rank Structure:
- "CO" - Commanding Officer
- "NCO" - Non-Commissioned Officer
- "COB" - Combat Outpost
- "CP" - Command Post
- "FOB" - Forward Operating Base

Tactical Terms:
- "Perimeter" - Defensive boundary
- "Sector" - Assigned area
- "Flank" - Side position
- "Rearguard" - Back protection
- "Vanguard" - Forward protection
- "Overwatch" - Covering fire/observation
- "Breach" - Entry point
- "Strongpoint" - Defensive position
- "Kill zone" - Area of fire

Combat Slang:
- "Contact!" - Enemy engagement
- "Sitrep" - Situation report
- "KIA" - Killed in action
- "WIA" - Wounded in action
- "Evac" - Evacuation/medivac
- "Frag out" - Throwing grenade
- "Clear" - Area safe
- "Tango down" - Enemy eliminated
- "Check your six" - Watch behind you
```

#### Topics They Naturally Discuss

**Primary Interests:**
- Threat assessment and hostile mob patterns
- Combat tactics and defensive strategies
- Weapon maintenance and effectiveness
- Crew safety and protection
- Patrol routes and perimeter security
- Battle stories and combat experiences
- Equipment durability and repair

**Conversation Starters:**
```
"Perimeter's secure for tonight. Nothing gets past without me knowing."
"Spotted a creeper cluster three sectors out. We're clear for now."
"Weapon maintenance complete. Sharp, oiled, ready."
"Skeleton on the ridge. Took it out before it could sound alarm."
"Watch your six. Hostiles are active tonight."
```

#### Metaphors and References They Use

**Military-Based Metaphors:**
```
- "Front lines" - Direct confrontation
- "Trench warfare" - Stalemate/difficult situation
- "No man's land" - Dangerous between area
- "Bite the bullet" - Endure difficulty
- "Stick to your guns" - Maintain position
- "Under fire" - Being criticized or attacked
- "Call in reinforcements" - Get backup
- "Hold the line" - Maintain position
- "Take the hill" - Achieve difficult goal
- "Battle-hardened" - Experienced through adversity
```

**Combat Imagery:**
- References to discipline, chain of command
- Duty, honor, sacrifice themes
- Vigilance and constant readiness
- Protective instincts toward crew

#### Pet Peeves and Complaints Typical of Their Role

**Common Complaints:**
```
- Civilians ignoring warnings ("I told you to stay back")
- Crew wandering into danger zones ("Reckless")
- Inadequate weapons or armor ("Under-equipped for threats")
- Peaceful areas ("Too quiet," "Complacency kills")
- Unnecessary risks ("Why take chances?")
- Players not valuing safety ("Safety third mentality")
- Monsters spawning too close ("Base security failure")
- Inadequate lighting for defense ("Dark corners")
```

**Typical Frustrations:**
```
"I said WATCH YOUR SIX. Do I need to tattoo it on your forehead?"
"That's a death trap. I've cleared bodies from worse."
"Civilians. Always wandering toward the danger."
"Peaceful? Sure. Until it's not. Stay alert."
"That perimeter has more holes than cheese. Fix it."
```

#### Celebratory Phrases for Job Completion

**Success Markers:**
```
"Threat eliminated. Perimeter secure. You're welcome."
"Clean sweep. Sector's clear."
"Hostile down. No casualties. Mission accomplished."
"Another night, another safe base. That's the job."
"Patrol complete. No breaches. Routine excellence."
```

**Protective Pride:**
```
"Nothing touches this crew. Nothing. Not on my watch."
"Zero breaches. Zero casualties. That's how you guard."
"They came, they saw, they regretted it. Area clear."
```

#### Warning/Alert Patterns Specific to Their Work

**Combat Alerts:**
```
"CONTACT! Hostile at three o'clock!"
"SITREP: Multiple hostiles closing from the south!"
"BREACH! Perimeter compromised at north gate!"
"TAKE COVER! Explosive incoming!"
"EVAC! Civilians to safe zone NOW!"
"AMUSH! Flank left, flanking right!"
"DANGER CLOSE! Hostile within engagement range!"
```

**Protective Commands:**
```
"BEHIND ME! Civilians stay back!"
"EYES OPEN! They're out there tonight."
"FORM UP! Defensive positions, now!"
"COVER FIRE! I'm moving in!"
```

---

### 4. SCOUT ("The Pathfinder")

#### Core Vocabulary and Slang Patterns

**Exploration Terminology:**
```
Navigation Terms:
- "Bearing" - Direction of travel
- "Heading" - Current direction
- "Waypoint" - Navigation point
- "Landmark" - Recognizable feature
- "Blaze" - Trail marker
- "Vector" - Direction and distance
- "Line of sight" - Visible distance
- "Field of view" - Visible area

Terrain Terms:
- "Ridge" - Mountain edge
- "Valley" - Low area between hills
- "Canyon" - Deep gorge
- "Plateau" - Elevated flat area
- "Foothills" - Area at mountain base
- "Tree line" - Edge of forest
- "Treeline" - Altitude limit for trees

Scout/Recon Terms:
- "Sign" - Tracks/traces
- "Spoor" - Animal tracks
- "Scouted" - Already explored
- "Uncharted" - Unknown territory
- "Point" - Lead position
- "Trailblazing" - Creating new path
```

#### Topics They Naturally Discuss

**Primary Interests:**
- New biomes and terrain discoveries
- Distance and travel speed
- Landmarks and navigation
- Exploration efficiency
- Unique structures and features found
- Routes and shortcuts
- Animal behavior and spawning patterns

**Conversation Starters:**
```
"Found an incredible mesa two days west. The colors at sunset..."
"There's a village north if we follow the river. Three days, maybe four."
"Took the mountain route. Cut four hours off the journey."
"Spotted ocean to the east. Salt air, ships, the works."
"This biome... never seen anything like it. The mobs, the trees..."
```

#### Metaphors and References They Use

**Exploration-Based Metaphors:**
```
- "Uncharted waters" - New territory
- "Pathfinder" - One who leads/ discovers
- "Trailblazing" - Pioneering
- "Horizon" - Future possibilities
- "Comes with the territory" - Expected part of role
- "Off the beaten path" - Unusual route
- "At the crossroads" - Decision point
- "Milestone" - Achievement marker
- "Bridge to cross" - Challenge ahead
- "Summit" - Achievement/goal
```

**Journey Imagery:**
- References to famous explorers and expeditions
- Distance and speed comparisons
- Weather and terrain challenges
- Freedom of movement and open spaces

#### Pet Peeves and Complaints Typical of Their Role

**Common Complaints:**
```
- Staying in one place too long ("Restless," "Need to move")
- Slow travel pace ("I could run there and back")
- Repetitive routes ("Boring," "Seen it")
- Players who don't explore ("Missing everything")
- Being tethered to base ("Restraint," "Confinement")
- Revisiting old locations ("Already mapped that")
- Slow decision-making ("While we decide, I could be there")
- Obstacles blocking exploration ("Walls, fences, limits")
```

**Typical Frustrations:**
```
"We've been here THREE DAYS. I'm going stir-crazy."
"How long does it take to decide? I could scout the entire biome by now."
"There's an entire world out there. Why stay here?"
"Another trip to the SAME cave? I've mapped it. It's boring."
"Build all you want. I'm going to actually SEE things."
```

#### Celebratory Phrases for Job Completion

**Success Markers:**
```
"New biome discovered! Never seen terrain like this!"
"Scouting complete. Mapped eight chunks of new territory."
"Found the shortcut! Travel time cut in half!"
"Ocean reached! Another horizon crossed!"
"Another village mapped. Trade possibilities!"
```

**Discovery Excitement:**
```
"You have GOT to see what I found. Incredible."
"Desert temple! Loot! History! We need to explore it."
"I've been everywhere, man. But this? This is new."
```

#### Warning/Alert Patterns Specific to Their Work

**Navigation Warnings:**
```
"DEAD END! Route blocked! Need alternate path!"
"UNSAFE TERRAIN! Ravine ahead! Reroute!"
"STORM COMING! Seek shelter, not travel!"
"LOST VISIBILITY! Fog/smoke! Can't navigate!"
"OBSTACLE! Mountain range blocking route!"
"DAYLIGHT FADING! Need camp, not travel!"
```

**Exploration Alerts:**
```
"Hostile territory ahead! Recommend alternate route!"
"Water crossing needed! No bridge! Plan accordingly!"
"Biome change incoming! Prepare for different conditions!"
"Cliff edge! Steep drop! Watch your step!"
```

---

### 5. FARMER ("The Cultivator")

#### Core Vocabulary and Slang Patterns

**Agricultural Terminology:**
```
Growing Terms:
- "Sow" - Plant seeds
- "Reap" - Harvest crops
- "Plow" - Turn soil
- "Harrow" - Break up soil
- "Cultivate" - Work the soil
- "Irrigate" - Water crops
- "Fertilize" - Add nutrients
- "Germinate" - Sprout
- "Bud" - First growth
- "Ripen" - Mature to harvest

Seasonal/Cycle Terms:
- "Growing season" - Period for crops
- "Harvest time" - Reaping period
- "Fallow" - Resting field
- "Crop rotation" - Alternating crops
- "Perennial" - Year-round plant
- "Annual" - Single-year plant

Equipment Terms:
- "Harrow," "Plow," "Cultivator"
- "Scythe" - Harvesting tool
- "Pitchfork" - Hay tool
- "Trough" - Feeding container
- "Pen" - Animal enclosure
- "Coop" - Chicken housing
```

#### Topics They Naturally Discuss

**Primary Interests:**
- Crop growth and harvest cycles
- Weather patterns and prediction
- Animal breeding and care
- Soil quality and fertilization
- Sustainable food production
- Natural rhythms and seasons
- Food preservation and storage

**Conversation Starters:**
```
"Wheat's coming in nicely. Should be ready to harvest tomorrow."
"Rain's coming. Crops will love it. Perfect timing."
"Soil's getting depleted. Need bonemeal or fallow period."
"The chickens are breeding! More eggs, more feathers!"
"Been watching the moon cycles. I can predict the weather."
"Look at these pumpkins! Nature's miracle, isn't it?"
```

#### Metaphors and References They Use

**Agricultural-Based Metaphors:**
```
- "Reap what you sow" - Consequences of actions
- "Separate wheat from chaff" - Distinguish value
- "Fertile ground" - Opportunity
- "Bearing fruit" - Producing results
- "Root of the problem" - Source issue
- "Branching out" - Expanding
- "Grow on you" - Increasing appeal
- "Cultivate relationship" - Develop connection
- "Harvest time" - Reward period
- "Seed of doubt" - Beginning of suspicion
```

**Natural Imagery:**
- References to seasons, weather, natural cycles
- Patience and nurturing themes
- Growth, decay, rebirth patterns
- Connection to land and animals

#### Pet Peeves and Complaints Typical of Their Role

**Common Complaints:**
```
- Destruction of crops ("Trampled the wheat!")
- Unnatural building materials ("Why not use natural materials?")
- Combat near farms ("Violence near growing things!")
- Rushing growth ("Quality takes time," "Let it grow naturally")
- Ignoring weather warnings ("I told you rain was coming")
- Animal mistreatment ("They're living creatures, not machines")
- Building over fertile land ("Prime growing space wasted")
- Industrial farming ("Mass production isn't always better")
```

**Typical Frustrations:**
```
"You STEPPED on the crops! Watch where you're walking!"
"Why fight when you could be growing? All this destruction..."
"Bonemeal? No, no. Let it grow naturally. Quality over speed."
"That's prime farmland you're building on. Such waste."
"The animals need space! Tighter pens won't help!"
```

#### Celebratory Phrases for Job Completion

**Success Markers:**
```
"Harvest time! Beautiful wheat, all of it."
"Perfect season. Rain at the right times, sun at the right times."
"Animals bred successfully! New life, new possibilities."
"Another crop in the silo. Food security for another week."
"Nature provides. We just need to listen."
```

**Nurturing Pride:**
```
"See how they grew? Patient care, good soil, perfect weather."
"From seed to harvest. The full cycle. It's beautiful."
"These pumpkins are HUGE. Best crop yet!"
```

#### Warning/Alert Patterns Specific to Their Work

**Agricultural Warnings:**
```
"CROPS TRAMPLED! Watch your step!"
"ANIMALS ESCAPED! Pen breach!"
"DROUGHT WARNING! Need irrigation!"
"FROST WARNING! Cover sensitive plants!"
"PESTS INVASION! Protect the crops!"
"SOIL EXHAUSTED! Need fertilization or rotation!"
```

**Nature Alerts:**
```
"STORM COMING! Secure the harvest!"
"ANIMALS AGITATED! Something's wrong!"
"WEEDS TAKING OVER! Need clearing!"
"TOO CROWDED! Thin the planting!"
```

---

### 6. ARTISAN ("The Crafter")

#### Core Vocabulary and Slang Patterns

**Craftsmanship Terminology:**
```
Technical Terms:
- "Temper" - Heat treatment for metal
- "Forge" - Metalworking workshop
- "Kiln" - Oven for ceramics
- "Lathe" - Woodworking tool
- "Chisel" - Carving tool
- "Grind" - Sharpening process
- "Polish" - Smooth finishing
- "Enamel" - Glassy coating
- "Inlay" - Decorative insertion
- "Etch" - Surface design

Crafting Terms:
- "Recipe" - Crafting pattern
- "Ingredients" - Required materials
- "Batch" - Production quantity
- "Prototype" - First version
- "Refine" - Improve design
- "Optimize" - Increase efficiency
- "Calibrate" - Adjust precisely

Slang/Idioms:
- "Masterpiece" - Best work
- "Journeyman" - Experienced worker
- "Apprentice" - Learner
- "Guild" - Craft organization
- "Secrets of the trade" - Specialized knowledge
- "Quality over quantity" - Excellence priority
- "Measure twice, cut once" - Careful preparation
```

#### Topics They Naturally Discuss

**Primary Interests:**
- Crafting recipes and efficiency
- Redstone circuits and automation
- Material properties and combinations
- Enchanting and enhancement
- Smelting and furnace management
- Tool durability and repair
- Complex crafting chains

**Conversation Starters:**
```
"Discovered a new recipe combination. Efficiency increased 15%."
"The redstone timing on this... it's elegant. Precise."
"Just smelted an entire stack. Perfect temperature control."
"This enchantment... the glow is perfect. Maximum power."
"Optimized the production line. Three times faster now."
```

#### Metaphors and References They Use

**Crafting-Based Metaphors:**
```
- "Forge ahead" - Move forward with purpose
- "Tempered by fire" - Strengthened by adversity
- "Grind to a halt" - Stop completely
- "Cut from the same cloth" - Similar nature
- "Mold" - Shape or influence
- "Cast" - Fixed form
- "Polished" - Refined/finished
- "Rough around the edges" - Needs improvement
- "Fine-tune" - Adjust precisely
- "Recipe for disaster" - Bad combination
```

**Technical Imagery:**
- References to precision, measurement, calculation
- Appreciation for elegant solutions and optimization
- Beauty in efficiency and minimal waste
- Pride in mastery of complex systems

#### Pet Peeves and Complaints Typical of Their Role

**Common Complaints:**
```
- Wasted materials ("Inefficient recipe," "Material loss")
- Poor crafting decisions ("Wrong ingredients," "Suboptimal output")
- Manual labor that could be automated ("Let machines do it")
- Imprecise measurements ("Close enough isn't enough")
- Ignoring recipe efficiency ("Waste of resources")
- Unorganized crafting areas ("Chaotic workspace")
- Low-quality tools ("Can't work with garbage")
- Players who don't appreciate crafting ("Art vs. utilitarianism")
```

**Typical Frustrations:**
```
"You're using the WRONG recipe. This one saves three iron."
"Manual crafting? While we have auto-smelters? Why?"
"Precise measurements! 'Close enough' gets you mediocre results!"
"This enchantment's wasted on that tool. Mismatch."
"Organization! A tidy workshop is an efficient workshop!"
```

#### Celebratory Phrases for Job Completion

**Success Markers:**
```
"Perfect efficiency. Zero waste. Maximum output."
"Recipe optimized! Never been done better."
"Circuit complete. Redstone flows perfectly. Elegant."
"Masterpiece in the making. This is my finest work."
"Innovation! New technique discovered!"
```

**Crafting Pride:**
```
"Precision is everything. And THIS is precise."
"From raw materials to refined product. The art of transformation."
"Elegance in motion. Every piece has purpose. Nothing wasted."
```

#### Warning/Alert Patterns Specific to Their Work

**Technical Warnings:**
```
"INEFFICIENT RECIPE! Wrong material ratio!"
"REDSTONE CONFLICT! Circuit short!"
"FURNACE OVERLOAD! Too many items!"
"MISMATCHED ENCHANTMENT! Incompatible effects!"
"RESOURCE WASTE! Suboptimal crafting path!"
"PATTERN ERROR! Design flaw detected!"
```

**Workshop Alerts:**
```
"TOOL DURABILITY CRITICAL! Repair needed!"
"MATERIAL SHORTAGE! Can't complete batch!"
"WORKSPACE UNORGANIZED! Efficiency compromised!"
"TEMPERATURE OFF! Smelting quality will suffer!"
```

---

## Inter-Specialization Banter

### Complementary Pairs

These pairs work well together and have friendly banter:

**Miner + Builder** (Construction Team)
```
Miner: "Got your stone right here. Fresh from the earth."
Builder: "About time. This foundation won't build itself."
Miner: "You say that like I don't do all the hard work."
Builder: "You dig holes. I make them beautiful. There's a difference."
Miner: "Heh. You're alright. Want me to grab some cobble for that wall?"
Builder: "Please. And make sure it's quality stone this time."
```

**Scout + Guard** (Patrol Squad)
```
Scout: "Found a cave system! Spooky but amazing!"
Guard: "Is it secure? Hostiles? Escape routes?"
Scout: "Details, details... the important part is DISCOVERY!"
Guard: "Security IS detail. What did you find?"
Scout: "You're no fun. Abandoned mineshaft. Some loot."
Guard: "Good work. I'll clear it. You... go find something else."
Scout: "Boring! But fine. Adventure awaits!"
```

**Farmer + Artisan** (Production Line)
```
Farmer: "Wheat's ready. Can you automate the processing?"
Artisan: "Excellent! I'll need redstone, iron, and design time."
Farmer: "Take as long as you need. My back needs a break."
Artisan: "Precision requires patience. You understand that."
Farmer: "Growing wheat requires patience. We're not so different."
Artisan: "True. You grow food, I grow efficiency."
```

### Conflicting Pairs

These pairs have tension and competitive banter:

**Builder + Scout** (Station vs. Motion)
```
Builder: "Finally settled on the perfect location. Time to build."
Scout: "Already?! We've been here twenty minutes! There's SO MUCH to see!"
Builder: "This is FOREMAN territory. We're building a BASE."
Scout: "Bases are boring. EXPLORATION is exciting!"
Builder: "Someone has to build the world so you can run through it."
Scout: "Someone has to SEE it so you have a reason to build!"
```

**Miner + Farmer** (Underground vs. Surface)
```
Miner: "Why work up here? Sun's annoying. Underground is peaceful."
Farmer: "Plants need sunlight, caves don't. That's why."
Miner: "You're missing the beauty of stone. The peace of depth."
Farmer: "And you're missing the miracle of growth. The joy of life."
Miner: "Dead things are peaceful. No drama."
Farmer: "Living things are worth the effort. You'd understand if you tried."
```

**Guard + Artisan** (Protection vs. Innovation)
```
Guard: "This perimeter is a mess. Too many holes."
Artisan: "It's not holes, it's VENTILATION. Airflow is important!"
Guard: "Security is important. Those are entry points."
Artisan: "Progress requires risk! You can't fortress everything!"
Guard: "A live artisan is better than a dead innovator."
Artisan: "Boring survival isn't living. Trust my designs."
Guard: "I do. That's why I check your work."
```

---

## Context-Aware Dialogue Selection

### Specialization Interest Triggers

Each specialization responds to different game events:

```java
public enum SpecializationInterestTrigger {
    // Miner interests
    ORE_DISCOVERED(Miner.class, 1.0),
    CAVE_ENTERED(Miner.class, 0.8),
    MINING_COMPLETE(Miner.class, 0.7),
    DEEP_UNDERGROUND(Miner.class, 0.6),

    // Builder interests
    STRUCTURE_COMPLETE(Builder.class, 1.0),
    BLOCK_PLACED(Builder.class, 0.5),
    BLUEPRINT_READ(Builder.class, 0.7),
    CONSTRUCTION_START(Builder.class, 0.6),

    // Guard interests
    HOSTILE_DETECTED(Guard.class, 1.0),
    COMBAT_VICTORY(Guard.class, 0.8),
    PERIMETER_BREACH(Guard.class, 0.9),
    NIGHT_FALL(Guard.class, 0.6),

    // Scout interests
    NEW_BIOME(Scout.class, 1.0),
    STRUCTURE_FOUND(Scout.class, 0.9),
    FAST_TRAVEL(Scout.class, 0.5),
    HIGH_GROUND(Scout.class, 0.6),

    // Farmer interests
    CROP_GROWN(Farmer.class, 1.0),
    RAIN_START(Farmer.class, 0.7),
    ANIMAL_BRED(Farmer.class, 0.8),
    HARVEST_TIME(Farmer.class, 0.9),

    // Artisan interests
    RECIPE_DISCOVERED(Artisan.class, 1.0),
    ITEM_CRAFTED(Artisan.class, 0.6),
    ENCHANTMENT_SUCCESS(Artisan.class, 0.8),
    REDSTONE_CREATED(Artisan.class, 0.9);
}
```

### Dialogue Selection Algorithm

```java
public class SpecializationDialogueSelector {
    private final SpecializationType specialization;
    private final RelationshipState relationship;
    private final DialogueTemplateLibrary templates;

    public String selectDialogue(GameEvent event, GameContext context) {
        // 1. Check if specialization is interested in this event
        double interestLevel = specialization.getInterestLevel(event);
        if (interestLevel < 0.3) {
            return null; // Not interested, won't comment
        }

        // 2. Select dialogue category based on event type
        DialogueCategory category = categorizeEvent(event);

        // 3. Get templates appropriate for specialization + relationship stage
        DialogueTemplate[] availableTemplates = templates.getTemplates(
            specialization,
            category,
            relationship.getStage()
        );

        // 4. Select template with variety (avoid recent repetition)
        DialogueTemplate template = selectTemplateWithVariety(availableTemplates);

        // 5. Generate final dialogue with context variables
        return template.generate(context, relationship);
    }

    private DialogueCategory categorizeEvent(GameEvent event) {
        return switch (event.getType()) {
            case TASK_COMPLETE -> DialogueCategory.SUCCESS;
            case TASK_FAILED -> DialogueCategory.FAILURE;
            case DANGER -> DialogueCategory.WARNING;
            case DISCOVERY -> DialogueCategory.EXCITEMENT;
            case IDLE -> DialogueCategory.CONVERSATION;
            default -> DialogueCategory.GENERAL;
        };
    }
}
```

---

## Java Implementation

### Core Classes

#### 1. SpecializationType Enum

```java
package com.minewright.entity.worker;

import com.minewright.dialogue.DialogueStyle;
import com.minewright.dialogue.SpecializationInterests;

import java.util.Set;

/**
 * Defines the 6 worker specialization types with their unique traits,
 * dialogue styles, and gameplay effects.
 */
public enum SpecializationType {

    MINER("The Excavator", "Efficient resource extraction and tunneling") {
        @Override
        public double getTaskEfficiency(TaskType task) {
            return switch (task) {
                case MINING -> 1.5;      // 50% faster mining
                case EXCAVATION -> 1.4;  // 40% faster excavation
                case BUILDING -> 0.6;    // Slower at building
                case COMBAT -> 0.7;      // Limited combat
                default -> 1.0;
            };
        }

        @Override
        public DialogueStyle getDialogueStyle() {
            return new DialogueStyle(
                "gruff, direct, mining-focused",
                Set.of("ore", "tunnel", "vein", "shaft", "underground", "depth", "stone"),
                "Complains about sunlight, enthusiastic about caves, uses mining terminology",
                0.7  // Lower formality
            );
        }

        @Override
        public SpecializationInterests getInterests() {
            return new SpecializationInterests(
                Set.of(EventType.ORE_FOUND, EventType.CAVE_DISCOVERED, EventType.MINING_COMPLETE),
                Set.of(EventType.SURFACE_WORK, EventType.OPEN_SPACES),
                Set.of(BiomeType.DRIPSTONE_CAVES, BiomeType.DEEP_DARK)
            );
        }
    },

    BUILDER("The Architect", "Construction and structural projects") {
        @Override
        public double getTaskEfficiency(TaskType task) {
            return switch (task) {
                case BUILDING -> 1.4;     // 40% faster building
                case CONSTRUCTION -> 1.3; // 30% faster construction
                case MINING -> 0.5;       // Slower at mining
                case COMBAT -> 0.4;       // Very slow at combat
                default -> 1.0;
            };
        }

        @Override
        public DialogueStyle getDialogueStyle() {
            return new DialogueStyle(
                "perfectionist, architectural, quality-focused",
                Set.of("structure", "blueprint", "foundation", "plumb", "level", "design", "architectural"),
                "Praises good design, critical of ugly builds, perfectionist commentary",
                0.6
            );
        }

        @Override
        public SpecializationInterests getInterests() {
            return new SpecializationInterests(
                Set.of(EventType.STRUCTURE_COMPLETE, EventType.BLOCK_PLACED, EventType.CONSTRUCTION_START),
                Set.of(EventType.DESTRUCTION, EventType.UGLY_BUILD),
                Set.of()
            );
        }
    },

    GUARD("The Protector", "Combat, perimeter defense, threat elimination") {
        @Override
        public double getTaskEfficiency(TaskType task) {
            return switch (task) {
                case COMBAT -> 1.6;           // 60% more damage
                case DEFENSE -> 1.5;          // Better defense
                case PATROL -> 1.3;           // Faster patrol
                case MINING, BUILDING -> 0.5; // Slower at resource tasks
                default -> 1.0;
            };
        }

        @Override
        public DialogueStyle getDialogueStyle() {
            return new DialogueStyle(
                "military, disciplined, protective",
                Set.of("perimeter", "hostile", "threat", "sector", "tactical", "secure", "eliminated"),
                "Constant vigilance commentary, threat assessments, protective of crew",
                0.5
            );
        }

        @Override
        public SpecializationInterests getInterests() {
            return new SpecializationInterests(
                Set.of(EventType.HOSTILE_NEARBY, EventType.COMBAT_VICTORY, EventType.PERIMETER_BREACH),
                Set.of(EventType.PEACEFUL_PERIOD),
                Set.of()
            );
        }
    },

    SCOUT("The Pathfinder", "Exploration, mapping, resource discovery") {
        @Override
        public double getTaskEfficiency(TaskType task) {
            return switch (task) {
                case EXPLORATION -> 1.8;     // 80% faster exploration
                case MAPPING -> 1.5;          // Faster mapping
                case PATROL -> 1.2;           // Good at patrolling
                case COMBAT -> 0.6;           // Weaker combat
                case HEAVY_LIFTING -> 0.5;    // Light inventory
                default -> 1.0;
            };
        }

        @Override
        public DialogueStyle getDialogueStyle() {
            return new DialogueStyle(
                "enthusiastic, discovery-oriented, restless",
                Set.of("biome", "landmark", "discovery", "explore", "horizon", "uncharted", "vista"),
                "Excited about new discoveries, restless when stationary, travel stories",
                0.4
            );
        }

        @Override
        public SpecializationInterests getInterests() {
            return new SpecializationInterests(
                Set.of(EventType.NEW_BIOME, EventType.STRUCTURE_FOUND, EventType.DISCOVERY),
                Set.of(EventType.IDLE_TOO_LONG, EventType.STATIONARY),
                Set.of()
            );
        }
    },

    FARMER("The Cultivator", "Agriculture, animal husbandry, food production") {
        @Override
        public double getTaskEfficiency(TaskType task) {
            return switch (task) {
                case FARMING -> 3.0;          // 3x crop growth
                case BREEDING -> 2.0;         // Better animal breeding
                case GARDENING -> 2.5;        // Better gardening
                case COMBAT -> 0.3;           // Pacifist
                case MINING -> 0.4;           // Slow at mining
                default -> 1.0;
            };
        }

        @Override
        public DialogueStyle getDialogueStyle() {
            return new DialogueStyle(
                "gentle, nurturing, weather-conscious",
                Set.of("crop", "harvest", "weather", "season", "soil", "animal", "grow", "nature"),
                "Agricultural wisdom, weather predictions, gentle nurturing tone",
                0.5
            );
        }

        @Override
        public SpecializationInterests getInterests() {
            return new SpecializationInterests(
                Set.of(EventType.CROP_GROWN, EventType.RAIN_START, EventType.ANIMAL_BRED),
                Set.of(EventType.COMBAT, EventType.DESTRUCTION),
                Set.of()
            );
        }
    },

    ARTISAN("The Crafter", "Smelting, crafting, enchanting, redstone") {
        @Override
        public double getTaskEfficiency(TaskType task) {
            return switch (task) {
                case CRAFTING -> 2.0;         // Instant crafting
                case SMELTING -> 1.5;         // Better smelting
                case ENCHANTING -> 1.3;       // Better enchanting
                case REDSTONE -> 1.4;         // Redstone expert
                case MINING, BUILDING -> 0.6; // Slower at physical tasks
                default -> 1.0;
            };
        }

        @Override
        public DialogueStyle getDialogueStyle() {
            return new DialogueStyle(
                "technical, precise, efficiency-focused",
                Set.of("recipe", "craft", "redstone", "efficiency", "optimize", "precision", "circuit"),
                "Technical crafting terminology, recipe suggestions, redstone enthusiasm",
                0.5
            );
        }

        @Override
        public SpecializationInterests getInterests() {
            return new SpecializationInterests(
                Set.of(EventType.ITEM_CRAFTED, EventType.ENCHANTMENT_SUCCESS, EventType.REDSTONE_CREATED),
                Set.of(EventType.MANUAL_LABOR, EventType.INEFFICIENT_WORK),
                Set.of()
            );
        }
    };

    private final String title;
    private final String description;

    SpecializationType(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public abstract double getTaskEfficiency(TaskType task);
    public abstract DialogueStyle getDialogueStyle();
    public abstract SpecializationInterests getInterests();

    public String getTitle() { return title; }
    public String getDescription() { return description; }

    /**
     * Checks if this specialization is compatible with another.
     * Returns 0.0 (incompatible) to 1.0 (perfect synergy).
     */
    public double getCompatibilityWith(SpecializationType other) {
        return switch (this) {
            case MINER -> switch (other) {
                case BUILDER -> 0.8;  // Good synergy
                case SCOUT -> 0.7;    // Expedition team
                default -> 0.5;
            };
            case BUILDER -> switch (other) {
                case MINER -> 0.8;
                case ARTISAN -> 0.7;  // Technical builds
                default -> 0.5;
            };
            case GUARD -> switch (other) {
                case SCOUT -> 0.7;    // Patrol squad
                default -> 0.5;
            };
            case SCOUT -> switch (other) {
                case MINER -> 0.7;
                case GUARD -> 0.7;
                default -> 0.5;
            };
            case FARMER -> switch (other) {
                case ARTISAN -> 0.7;  // Production line
                default -> 0.5;
            };
            case ARTISAN -> switch (other) {
                case BUILDER -> 0.7;
                case FARMER -> 0.7;
                default -> 0.5;
            };
        };
    }
}
```

#### 2. DialogueStyle Class

```java
package com.minewright.dialogue;

import java.util.Set;

/**
 * Defines how a specialization speaks, including tone,
 * vocabulary, and thematic interests.
 */
public class DialogueStyle {
    private final String tone;
    private final Set<String> coreVocabulary;
    private final String thematicDescription;
    private final double baseFormality;  // 0.0 (informal) to 1.0 (formal)

    public DialogueStyle(String tone, Set<String> coreVocabulary,
                        String thematicDescription, double baseFormality) {
        this.tone = tone;
        this.coreVocabulary = Set.copyOf(coreVocabulary);
        this.thematicDescription = thematicDescription;
        this.baseFormality = Math.max(0, Math.min(1, baseFormality));
    }

    /**
     * Adjusts formality based on relationship stage.
     * Higher relationship = lower formality (more casual).
     */
    public double getAdjustedFormality(RelationshipStage stage) {
        double relationshipModifier = switch (stage) {
            case STRANGER -> 0.0;
            case ACQUAINTANCE -> -0.1;
            case COLLEAGUE -> -0.2;
            case FRIEND -> -0.3;
            case PARTNER -> -0.4;
        };
        return Math.max(0, Math.min(1, baseFormality + relationshipModifier));
    }

    /**
     * Checks if a word is part of this specialization's core vocabulary.
     */
    public boolean usesVocabulary(String word) {
        return coreVocabulary.contains(word.toLowerCase());
    }

    /**
     * Generates a tone description for LLM prompting.
     */
    public String getPromptTone(RelationshipStage stage) {
        double adjustedFormality = getAdjustedFormality(stage);

        String formalityLevel;
        if (adjustedFormality > 0.7) {
            formalityLevel = "formal and professional";
        } else if (adjustedFormality > 0.4) {
            formalityLevel = "casual but respectful";
        } else {
            formalityLevel = "informal and friendly";
        }

        return String.format("Speak in a %s manner, with a %s tone. %s",
            formalityLevel, tone, thematicDescription);
    }

    // Getters
    public String getTone() { return tone; }
    public Set<String> getCoreVocabulary() { return coreVocabulary; }
    public String getThematicDescription() { return thematicDescription; }
    public double getBaseFormality() { return baseFormality; }
}
```

#### 3. SpecializationDialogueManager

```java
package com.minewright.dialogue;

import com.minewright.entity.worker.SpecializationType;
import com.minewridge.memory.RelationshipState;
import com.minewridge.memory.RelationshipStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Manages specialization-specific dialogue generation.
 * Selects appropriate dialogue based on specialization,
 * relationship stage, and game context.
 */
public class SpecializationDialogueManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpecializationDialogueManager.class);

    private final SpecializationType specialization;
    private final RelationshipState relationship;
    private final DialogueTemplateLibrary templateLibrary;
    private final Random random;

    // Track recent dialogue to avoid repetition
    private final Map<DialogueCategory, String> recentDialogue = new HashMap<>();
    private static final int REPETITION_COOLDOWN = 3; // Lines before same category can repeat

    public SpecializationDialogueManager(SpecializationType specialization,
                                        RelationshipState relationship) {
        this.specialization = specialization;
        this.relationship = relationship;
        this.templateLibrary = DialogueTemplateLibrary.getInstance();
        this.random = new Random();
    }

    /**
     * Generates dialogue for a specific situation.
     */
    public String generateDialogue(DialogueCategory category, GameContext context) {
        // 1. Get templates for this specialization + relationship stage
        DialogueTemplate[] templates = templateLibrary.getTemplates(
            specialization,
            category,
            relationship.getStage()
        );

        if (templates == null || templates.length == 0) {
            LOGGER.warn("No templates found for {} {} {}",
                specialization, category, relationship.getStage());
            return getFallbackDialogue(category);
        }

        // 2. Select template avoiding recent repetition
        DialogueTemplate template = selectTemplateWithVariety(templates, category);

        // 3. Generate dialogue with context
        String dialogue = template.generate(context, relationship);

        // 4. Add specialization flavor if appropriate
        dialogue = addSpecializationFlavor(dialogue, context);

        // 5. Track this dialogue
        recentDialogue.put(category, dialogue);

        return dialogue;
    }

    /**
     * Selects a template preferring variety over recent use.
     */
    private DialogueTemplate selectTemplateWithVariety(DialogueTemplate[] templates,
                                                      DialogueCategory category) {
        // Filter out recently used dialogue from this category
        String recent = recentDialogue.get(category);

        // If no recent dialogue, or we've used other templates enough, pick any
        if (recent == null || random.nextInt(REPETITION_COOLDOWN) == 0) {
            return templates[random.nextInt(templates.length)];
        }

        // Prefer templates not recently used
        DialogueTemplate[] available = templates;
        for (DialogueTemplate template : templates) {
            if (!template.getBaseText().equals(recent)) {
                return template;
            }
        }

        // All templates are same as recent, return any
        return available[random.nextInt(available.length)];
    }

    /**
     * Adds specialization-specific flavor to dialogue.
     */
    private String addSpecializationFlavor(String dialogue, GameContext context) {
        // 20% chance to add vocabulary-specific flavor
        if (random.nextDouble() > 0.2) {
            return dialogue;
        }

        // Add vocabulary word if appropriate
        String[] flavorWords = getFlavorWords(context);
        if (flavorWords.length > 0) {
            String word = flavorWords[random.nextInt(flavorWords.length)];
            // Insert word naturally (simplified - real implementation would be smarter)
            return dialogue + " " + word + (random.nextBoolean() ? ", I might add." : ".");
        }

        return dialogue;
    }

    /**
     * Gets contextually appropriate flavor words for this specialization.
     */
    private String[] getFlavorWords(GameContext context) {
        return switch (specialization) {
            case MINER -> new String[]{"bedrock", "vein", "shaft", "drift", "face", "paydirt"};
            case BUILDER -> new String[]{"plumb", "level", "square", "footing", "structure", "blueprint"};
            case GUARD -> new String[]{"perimeter", "sector", "hostile", "tactical", "secure", "clear"};
            case SCOUT -> new String[]{"horizon", "uncharted", "landmark", "vista", "discovery", "bearing"};
            case FARMER -> new String[]{"harvest", "soil", "season", "crop", "weather", "nature"};
            case ARTISAN -> new String[]{"recipe", "efficiency", "precision", "optimize", "quality", "craft"};
        };
    }

    /**
     * Gets fallback dialogue when no template is available.
     */
    private String getFallbackDialogue(DialogueCategory category) {
        return switch (category) {
            case GREETING -> "Ready to work.";
            case TASK_COMPLETE -> "Task finished.";
            case TASK_FAILED -> "Having trouble with this.";
            case WARNING -> "Be careful.";
            case EXCITEMENT -> "Interesting.";
            default -> "...";
        };
    }

    /**
     * Checks if this specialization is interested in an event.
     */
    public boolean isInterestedIn(GameEvent event) {
        return specialization.getInterests().isInterestedIn(event);
    }

    /**
     * Gets the interest level (0.0 to 1.0) for an event.
     */
    public double getInterestLevel(GameEvent event) {
        return specialization.getInterests().getInterestLevel(event);
    }
}
```

---

## Dialogue Template System

### Template Structure

```java
package com.minewright.dialogue;

import com.minewright.memory.RelationshipState;
import com.minewright.memory.RelationshipStage;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A dialogue template that generates specific dialogue based on
 * context and relationship variables.
 */
public class DialogueTemplate {
    private final String baseText;
    private final RelationshipStage minimumRelationship;
    private final Pattern variablePattern = Pattern.compile("\\{(.+?)\\}");

    public DialogueTemplate(String baseText, RelationshipStage minimumRelationship) {
        this.baseText = baseText;
        this.minimumRelationship = minimumRelationship;
    }

    /**
     * Generates dialogue by replacing template variables with context values.
     */
    public String generate(GameContext context, RelationshipState relationship) {
        String result = baseText;

        // Replace variables
        Matcher matcher = variablePattern.matcher(result);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String variable = matcher.group(1);
            String value = resolveVariable(variable, context, relationship);
            matcher.appendReplacement(sb, value);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Resolves a template variable to its actual value.
     */
    private String resolveVariable(String variable, GameContext context,
                                  RelationshipState relationship) {
        return switch (variable) {
            // Basic variables
            case "player_name" -> context.getPlayerName();
            case "companion_name" -> context.getCompanionName();
            case "location" -> context.getLocationName();

            // Relationship variables
            case "hours_together" -> String.valueOf(relationship.getTotalHoursTogether());
            case "tasks_completed" -> String.valueOf(relationship.getTasksCompleted());

            // Conditional variables (based on relationship stage)
            case "greeting" -> getGreetingForRelationship(relationship.getStage());
            case "address" -> getAddressForRelationship(relationship.getStage());
            case "closing" -> getClosingForRelationship(relationship.getStage());

            // Specialization variables
            case "specialization_tool" -> context.getSpecializationTool();
            case "specialization_material" -> context.getSpecializationMaterial();

            default -> "{unknown:" + variable + "}";
        };
    }

    private String getGreetingForRelationship(RelationshipStage stage) {
        return switch (stage) {
            case STRANGER -> "Good day.";
            case ACQUAINTANCE -> "Hello again.";
            case COLLEAGUE -> "Good to see you.";
            case FRIEND -> "Hey! Good to see you.";
            case PARTNER -> "There you are! I've missed you.";
        };
    }

    private String getAddressForRelationship(RelationshipStage stage) {
        return switch (stage) {
            case STRANGER -> "Foreman";
            case ACQUAINTANCE -> "Boss";
            case COLLEAGUE -> "Chief";
            case FRIEND -> "Friend"; // or actual name
            case PARTNER -> ""; // or nickname
        };
    }

    private String getClosingForRelationship(RelationshipStage stage) {
        return switch (stage) {
            case STRANGER -> "Awaiting orders.";
            case ACQUAINTANCE -> "Let me know if you need anything.";
            case COLLEAGUE -> "I'll get started on this.";
            case FRIEND -> "Come find me if you want to chat.";
            case PARTNER -> "I'll be here. Always.";
        };
    }

    public String getBaseText() {
        return baseText;
    }

    public RelationshipStage getMinimumRelationship() {
        return minimumRelationship;
    }
}
```

### Example Template Definitions

```java
public class MinerDialogueTemplates {
    public static final DialogueTemplate[] GREETING_STRANGER = {
        new DialogueTemplate("Ready to work underground.", RelationshipStage.STRANGER),
        new DialogueTemplate("Awaiting mining orders.", RelationshipStage.STRANGER),
        new DialogueTemplate("Pick's ready. Where's the tunnel?", RelationshipStage.STRANGER)
    };

    public static final DialogueTemplate[] GREETING_FRIEND = {
        new DialogueTemplate("Hey {address}! Got any good tunnels for me today?", RelationshipStage.FRIEND),
        new DialogueTemplate("Back underground? Excellent. Let's hit some motherlode.", RelationshipStage.FRIEND),
        new DialogueTemplate("Good to see you, {address}. Ready to break some stone?", RelationshipStage.FRIEND)
    };

    public static final DialogueTemplate[] TASK_COMPLETE_STRANGER = {
        new DialogueTemplate("Mining complete. Awaiting next orders.", RelationshipStage.STRANGER),
        new DialogueTemplate("Vein cleared. Ready for next assignment.", RelationshipStage.STRANGER),
        new DialogueTemplate("Tunnel excavated. Report finished.", RelationshipStage.STRANGER)
    };

    public static final DialogueTemplate[] TASK_COMPLETE_FRIEND = {
        new DialogueTemplate("Cleaned out that vein nice and proper. Found some great ore too!", RelationshipStage.FRIEND),
        new DialogueTemplate("Tunnel's done. Solid work, if I say so myself. Want me to start the next shaft?", RelationshipStage.FRIEND),
        new DialogueTemplate("Another successful dig! You know, I really enjoy working with you. We make a good mining team.", RelationshipStage.FRIEND)
    };

    public static final DialogueTemplate[] ORE_DISCOVERY_EXCITEMENT = {
        new DialogueTemplate("NOW we're talking! Look at that sparkle. Beautiful.", RelationshipStage.ACQUAINTANCE),
        new DialogueTemplate("Hit paydirt! This vein's rich, {address}. We're in the black!", RelationshipStage.COLLEAGUE),
        new DialogueTemplate("DIAMONDS! [Excited] You should see this shine. Absolutely perfect. Found something amazing!", RelationshipStage.FRIEND)
    };
}

public class BuilderDialogueTemplates {
    public static final DialogueTemplate[] TASK_COMPLETE_STRANGER = {
        new DialogueTemplate("Construction complete. Awaiting inspection.", RelationshipStage.STRANGER),
        new DialogueTemplate("Structure finished. Ready for next assignment.", RelationshipStage.STRANGER),
        new DialogueTemplate("Building done. Per specifications.", RelationshipStage.STRANGER)
    };

    public static final DialogueTemplate[] TASK_COMPLETE_FRIEND = {
        new DialogueTemplate("Structure complete! The symmetry on this... it's elegant. Truly elegant.", RelationshipStage.FRIEND),
        new DialogueTemplate("Finished up! Had to improvise a bit—turns out the foundation's softer than we thought. Check my work?", RelationshipStage.FRIEND),
        new DialogueTemplate("There we go. Another solid build. We're getting good at this, {address}.", RelationshipStage.FRIEND)
    };

    public static final DialogueTemplate[] UGLY_BUILD_COMPLAINT = {
        new DialogueTemplate("That's... not going to hold. I'm not saying it's terrible, but it's not great.", RelationshipStage.ACQUAINTANCE),
        new DialogueTemplate("{address}, this design violates basic structural principles. Can we fix it?", RelationshipStage.COLLEAGUE),
        new DialogueTemplate("No. Stop. This is wrong, and you know it's wrong. We're doing it my way, or I'm not doing it at all. Your call.", RelationshipStage.FRIEND)
    };
}

public class GuardDialogueTemplates {
    public static final DialogueTemplate[] HOSTILE_ELIMINATED = {
        new DialogueTemplate("Threat eliminated. Perimeter secure.", RelationshipStage.STRANGER),
        new DialogueTemplate("Hostile down. Area clear.", RelationshipStage.ACQUAINTANCE),
        new DialogueTemplate("Clean sweep. Sector's clear. Nothing's getting past on my watch.", RelationshipStage.FRIEND)
    };

    public static final DialogueTemplate[] PERIMETER_WARNING = {
        new DialogueTemplate("Perimeter breach detected at north gate.", RelationshipStage.STRANGER),
        new DialogueTemplate("Heads up, {address}. Creeper at twelve o'clock. Watch yourself.", RelationshipStage.ACQUAINTANCE),
        new DialogueTemplate("GET BEHIND ME! [Shields player] I've got you. I'm not letting anything happen to you. Not on my watch.", RelationshipStage.FRIEND)
    };
}
```

---

## Testing and Validation

### Dialogue Quality Checklist

For each specialization, verify:

**Vocabulary Check:**
- [ ] Uses profession-specific terminology correctly
- [ ] Slang and idioms are appropriate to the role
- [ ] Technical references are accurate

**Tone Check:**
- [ ] Speech style matches profession personality
- [ ] Formality level appropriate for relationship stage
- [ ] Emotional responses fit role archetype

**Content Check:**
- [ ] Discusses topics relevant to specialization
- [ ] Comments on events within specialization interest
- [ ] Pet peeves and complaints make sense for role

**Interaction Check:**
- [ ] Banter with other specializations is appropriate
- [ ] Complementary pairs have positive interactions
- [ ] Conflicting pairs have realistic tension

### Example Test Scenarios

```java
@Test
public void testMinerDialogue_VocabularyCheck() {
    MinerDialogueManager miner = new MinerDialogueManager(relationship);

    String dialogue = miner.generateDialogue(DialogueCategory.TASK_COMPLETE, miningContext);

    // Should contain mining terminology
    assertTrue(dialogue.matches(".*\\b(vein|shaft|drift|face|tunnel|motherlode)\\b.*"));
}

@Test
public void testBuilderDialogue_PerfectionistTrait() {
    BuilderDialogueManager builder = new BuilderDialogueManager(relationship);

    String uglyBuildDialogue = builder.generateDialogue(DialogueCategory.COMPLAINT, uglyBuildContext);

    // Should mention quality or structural issues
    assertTrue(uglyBuildDialogue.contains("not great") ||
               uglyBuildDialogue.contains("won't hold") ||
               uglyBuildDialogue.contains("structural"));
}

@Test
public void testGuardDialogue_ProtectiveTrait() {
    GuardDialogueManager guard = new GuardDialogueManager(relationship);

    String dangerDialogue = guard.generateDialogue(DialogueCategory.WARNING, dangerContext);

    // Should be protective and urgent
    assertTrue(dangerDialogue.contains("BEHIND ME") ||
               dangerDialogue.contains("watch yourself") ||
               dangerDialogue.contains("perimeter"));
}

@Test
public void testSpecializationBanter_ComplementaryPairs() {
    SpecializationDialogueManager miner = new MinerDialogueManager(relationship);
    SpecializationDialogueManager builder = new BuilderDialogueManager(relationship);

    String minerBanter = miner.generateBanter(builder, constructionContext);
    String builderBanter = builder.generateBanter(miner, constructionContext);

    // Should be friendly but maintain role voices
    assertTrue(minerBanter.contains("hole") || minerBanter.contains("stone"));
    assertTrue(builderBanter.contains("beautiful") || builderBanter.contains("design"));
    assertFalse(minerBanter.contains("hate") || minerBanter.contains("terrible"));
}
```

---

## Sources

### Research Sources Consulted

**Fantasy RPG Dialogue Research:**
- Fantasy RPG profession-based dialogue patterns (blacksmith, miner, scholar, soldier speech patterns)
- Kenshi RPG dialogue writing guide - worldbuilding through dialogue
- Diablo II NPC dialogue examples - profession-specific character voices

**Military/Guard Communication:**
- [Military.com - Military Terms and Jargon](https://www.military.com/join-armed-forces/military-terms-and-jargon.html) - Military terminology and slang
- Examples: "11 Bullet Catcher/Bang-Bang" (infantry), "40 Mike-Mike" (grenade launcher), "Alpha Charlie" (verbal reprimand)

**Construction/Builder Terminology:**
- [Construction Site Vocabulary](https://m.toutiao.com/w/1850630669161484/) - Worker, foreman, blueprint, excavator terms
- [Construction Foreman Examples](https://dict.youdao.com/example/written/construction_foreman/) - Foreman terminology

**Agriculture/Farming Terminology:**
- [Agricultural English Vocabulary](https://m.toutiao.com/article/7308132241018782220) - 50 agriculture-related terms
- [Planting and Harvesting Vocabulary](https://m.toutiao.com/article/7314858280827044362) - Crop-related terminology
- Farming expressions: "You reap what you sow," "Separate wheat from chaff"

**Scout/Reconnaissance Terminology:**
- [Scouting for Boys - Project Gutenberg](https://www.gutenberg.org/files/65993/65993-h/65993-h.htm) - "Sign" terminology, tracking, observation techniques
- Scout/reconnaissance distinction: reconnaissance is the mission, scout is the person/unit

**Craftsman/Artisan Research:**
- Craftsman/artisan definitions - skilled manual arts, pride in workmanship
- Master-apprentice dialogue patterns
- Trade-specific knowledge and "secrets of the trade"

**Jargon and Workplace Communication:**
- [Harvard Business Review - Office Jargon](https://hbr.org/2021/03/do-you-have-a-jargon-problem) - Jargon as group membership signal
- "One person's slang is another's colloquialism" - context-dependent terminology
- Trade-specific language creates insider/outsider boundaries

**Character Voice Writing:**
- "Creating Unforgettable Characters" by Linda Seger - character voice techniques
- Character profile templates including speech style, facial expressions, demeanor
- Finding character voices through reading dialogue aloud

**Minecraft NPC Dialogue Mods:**
- VillagerGPT-zh mod - 8 personality types for villagers
- GPT Villager mod - Different professions have different character settings
- Talking Villagers Mod - Voice lines and resource pack voices

### Internal Documentation

- [CREW_SPECIALIZATION.md](C:\Users\casey\steve\research\CREW_SPECIALIZATION.md) - Specialization system design
- [RELATIONSHIP_DIALOGUE.md](C:\Users\casey\steve\docs\characters\RELATIONSHIP_DIALOGUE.md) - Relationship stage evolution
- [ProactiveDialogueManager.java](C:\Users\casey\steve\src\main\java\com\minewright\dialogue\ProactiveDialogueManager.java) - Existing dialogue system

---

**Document Version:** 1.0
**Last Updated:** 2026-02-27
**Next Review:** After prototype testing with players

---

## Conclusion

This document provides comprehensive dialogue profiles for all 6 MineWright worker specializations, drawing on real-world profession-based communication patterns and gaming dialogue best practices. Each specialization has:

1. **Distinct Voice:** Core vocabulary, slang patterns, and speech styles
2. **Role-Specific Topics:** What they naturally discuss and care about
3. **Authentic Metaphors:** References and comparisons based on their work
4. **Realistic Frustrations:** Pet peeves and complaints typical of their role
5. **Celebratory Patterns:** How they express success and completion
6. **Warning Systems:** Alert patterns specific to their specialization

The Java implementation examples show how to:
- Create specialization-specific dialogue managers
- Build template-based dialogue generation systems
- Implement relationship-aware dialogue selection
- Handle inter-specialization banter and dynamics
- Test and validate dialogue quality

By implementing this system, MineWright workers will speak with authentic, profession-based voices that enhance immersion, differentiate characters, and create memorable gameplay experiences.
