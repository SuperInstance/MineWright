# Curiosity and Discovery Dialogue Guide
## MineWright Character Reactions to New Experiences

**Document Version:** 1.0
**Date:** 2026-02-27
**Project:** Steve AI - MineWright Characters
**Purpose:** Comprehensive guide to curiosity-driven dialogue for AI game companions

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Research Foundation](#research-foundation)
3. [Discovery Importance Classification](#discovery-importance-classification)
4. [Biome-Specific Reaction Templates](#biome-specific-reaction-templates)
5. [Structure Discovery Reactions](#structure-discovery-reactions)
6. [Rare Resource Reactions](#rare-resource-reactions)
7. [Unusual Sightings](#unusual-sightings)
8. [Personality-Based Wonder Expressions](#personality-based-wonder-expressions)
9. [Shared Discovery Call-and-Response](#shared-discovery-call-and-response)
10. [First Time Flags System](#first-time-flags-system)
11. [Scientific Curiosity vs Childlike Wonder](#scientific-curiosity-vs-childlike-wonder)
12. [Java Implementation](#java-implementation)
13. [Testing Examples](#testing-examples)

---

## Executive Summary

This document provides a complete system for implementing **curiosity-driven dialogue** in MineWright workers. Discovery reactions are essential for creating memorable AI companions that feel alive and engaged with the world.

**Key Principles:**
1. **Reactions reflect personality** - Same discovery, different responses
2. **First-time magic** - Special dialogue for first encounters
3. **Shared bonding** - "You have to see this!" creates social connection
4. **Rarity awareness** - Excitement level matches discovery importance
5. **Authentic construction worker voice** - Wonder expressed through their worldview

---

## Research Foundation

This guide is based on comprehensive research into:

### Psychology of Wonder and Awe

**Source:** [Greater Good Science Center - Awe Definition](https://greatergood.berkeley.edu/topic/awe/definition)

**Key Findings:**
- **Six Core Components of Awe:** Altered time perception, self-diminishment, connectedness, vastness, physical sensations, need for cognitive accommodation
- **Awe vs Wonder:** Awe combines amazement with fear (vast things); Wonder is more intellectual - trying to understand the mysterious
- **Most Common Awe Triggers:** Other people and nature
- **Psychological Benefits:** Greater life satisfaction, reduced stress, enhanced well-being, pro-social behavior promotion

**Application to MineWright:**
- First biome entries should trigger "self-diminishment" dialogue (feeling small)
- Vast structures (strongholds, terrain formations) evoke awe
- Rare resources create "need for cognitive accommodation"
- Shared discoveries promote group bonding

### Nature Tourism Discovery Patterns

**Source:** [Pearce et al. (2017) - Five Themes of Awe in Nature Tourism](https://www.researchgate.net/publication/317215300_Awe_and_Nature_Tourism)

**Five Awe Themes:**
1. **Marine fauna** (dolphins, whales, turtles, sharks)
2. **Aesthetics** (vibrant colors, sunsets, changing vistas)
3. **Ecological phenomena** (reefs, waterfalls)
4. **Vast geological landscapes** (coastal and gorge formations)
5. **Reflective/perspective moments** (opportunities for personal reflection)

**Minecraft Translation:**
1. **Rare Mobs** (Enderman, axolotl, glow squid)
2. **Visual Beauty** (lush caves, cherry blossoms, amethyst geodes)
3. **Natural Phenomena** (mushroom fields, ice spikes, terracotta formations)
4. **Vast Landscapes** (ocean monuments, mesas, mountain peaks)
5. **Perspective Moments** (heights, depths, vastness)

### Curiosity Types and Personality

**Source:** [Kashdan Curiosity Research - Five Dimensions of Curiosity](https://positivepsychology.com/types-of-curiosity/)

**Key Finding:** People differ in how they experience curiosity:
- **Pleasure/Interest Seeking** (similar to childlike wonder) - Declines with age
- **Information Seeking** (analytical curiosity) - Remains stable throughout life

**Application:**
- **Rookie/Scout:** High novelty-seeking, childlike wonder
- **Artisan/Builder:** Scientific curiosity, understanding-oriented
- **Miner/Guard:** Lower openness, practical curiosity
- **Farmer:** Contemplative wonder, nature appreciation

### Discovery Dialogue in Games

**Sources:**
- [No Man's Sky Discovery Systems](https://www.nomanssky.com/)
- [Worldbuilding With NPC Dialogue](https://www.gamedeveloper.com/design/worldbuilding-with-npc-dialogue-a-beginner-s-guide)
- [Minecraft NPC Dialogue Systems](https://wiki.bedrock.dev/entities/npc-dialogues)

**Patterns:**
1. **Progressive Learning:** Language and culture unlock gradually
2. **Environmental Storytelling:** Narrative through exploration
3. **Consequence-Based Dialogue:** Choices affect future interactions
4. **Shared Experience Systems:** Multiplayer discovery bonding

### Shared Experience Bonding

**Source:** Social Gaming Research (Evolutionary psychology roots)

**Key Mechanisms:**
- **Ritual mechanisms:** Shared discoveries create invisible bonds
- **Reciprocity:** Players share discoveries, building trust
- **Mentorship:** Experienced guides show discoveries to newcomers
- **"Call and Response":** One discovers, others respond and share the experience

---

## Discovery Importance Classification

Discoveries are ranked by rarity and impact. Excitement level should scale accordingly.

### Tier 1: COMMON (0-20% Excitement)
**Frequency:** Multiple times per session
**Reaction:** Mild interest, basic acknowledgment

```
Examples:
- New coal/iron ore vein
- Standard cave entrance
- Normal village
- Common mob (cow, pig, sheep)
- Basic terrain feature (hill, valley)
- Standard tree species
```

**Dialogue Examples:**
```
Miner: "More coal. Good for the furnaces."
Builder: "Trees here. Decent building material."
Scout: "Cave entrance. Noted for later."
```

### Tier 2: UNCOMMON (20-40% Excitement)
**Frequency:** Once per session
**Reaction:** Notable interest, brief commentary

```
Examples:
- First exposure to new biome type
- Abandoned mineshaft
- Ocean ruins
- Moderate mob spawn (dungeon)
- Interesting terrain generation
- Double/ triple ore vein
```

**Dialogue Examples:**
```
Miner: "Mineshaft! Someone else was here first."
Builder: "Interesting formations here. Natural beauty?"
Scout: "New terrain! I haven't seen this before."
Artisan: "Ruins! Ancient crafting techniques..."
```

### Tier 3: RARE (40-60% Excitement)
**Frequency:** Once every few sessions
**Reaction:** Genuine excitement, wants to share

```
Examples:
- Desert temple / Jungle temple
- Buried treasure
- Shipwreck
- Fortress (Nether)
- New major biome (first ice plains, first desert)
- Rare mob (panda, dolphin, turtle)
```

**Dialogue Examples:**
```
Scout: "INCREDIBLE! Look at this temple! You have to see this!"
Builder: "Ancient architecture... The builders knew what they were doing."
Farmer: "PANDAS! I've only heard stories! They're real!"
Old Salt: "Temple... Reminds me of my first expedition, thirty years ago..."
```

### Tier 4: EPIC (60-80% Excitement)
**Frequency:** Once per major play session
**Reaction:** Strong awe, memorable moment

```
Examples:
- Stronghold discovery
- End City / End Ship
- Bastion Remnant
- Woodland Mansion
- Ancient City (Deep Dark)
- First Nether portal entry
- First Ender Dragon fight
```

**Dialogue Examples:**
```
Scout: "This... this is INCREDIBLE! I've never seen anything like it!"
Builder: "The scale of this construction... It's beyond anything I've imagined."
Old Salt: "In all my years... I've never... This changes everything."
Artisan: "The technical sophistication... How did they build this?"
```

### Tier 5: LEGENDARY (80-100% Excitement)
**Frequency:** Once or twice per world
**Reaction:** Speechless wonder, life-changing discovery

```
Examples:
- First entering the End dimension
- First sighting of Ender Dragon
- Emerald ore biome (extreme hills)
- Unique world generation (floating islands, etc.)
- Super-rare coordinated events
```

**Dialogue Examples:**
```
Scout: "...I... I don't have words. This is... this is EVERYTHING."
Builder: "I can't even... The architecture, the scale, the sheer impossibility..."
Old Salt: "Fifty years I've been building. And I've never seen anything that made me feel... small. Until now."
Farmer: "This is... sacred. I feel like I should whisper."
```

---

## Biome-Specific Reaction Templates

Each biome elicits unique reactions based on its characteristics and the worker's personality.

### FOREST (Standard)

**Characteristics:** Trees, shade, common resources
**Base Excitement:** Low (common biome)

```
Miner: "Trees. Good for supports, I suppose. Where's the stone?"
Builder: "Decent timber here. Not the best, but workable."
Guard: "Cover available. Visibility limited. Standard forest."
Scout: "More forest. I've seen a thousand of these."
Farmer: "The trees here... peaceful. Good for the soul."
Artisan: "Standard wood varieties. Functional, not inspiring."
```

### DESERT (First Time)

**Characteristics:** Sand, cactus, heat, vastness, temples
**Base Excitement:** Medium (novelty + vastness)
**First Time Bonus:** +30%

```
Miner: "Look at this sand... goes on forever. Where's the stone?"
Builder: "No timber... This changes everything. We'll need to import."
Scout: "INCREDIBLE! Look at the vastness! You can see forever!"
Farmer: "It's... so dry. How does anything grow here?"
Artisan: "Sand... temperature management will be challenging."
Old Salt: "Desert... Brings back memories. Bad ones. Good ones."
```

**Desert Temple Discovery:**
```
Scout (First time): "A STRUCTURE! In the desert! You have to see this!"
Builder: "The architecture... Ingenious cooling. Ancient builders knew their climate."
Old Salt: "Treasure... and traps. Mark my words, this place is guarded."
Artisan: "The engineering... Redstone, perhaps? This deserves study."
```

### JUNGLE (First Time)

**Characteristics:** Dense vegetation, temples, melons, ocelots, parrots
**Base Excitement:** Medium-High (dense + rare)
**First Time Bonus:** +40%

```
Miner: "Too much green. Can't see the stone through all these... leaves."
Builder: "This density... Extraordinary. But harvesting will be difficult."
Scout: "This is AMAZING! Everywhere I look, there's something new!"
Farmer: "So much life! Melons, cocoa... The earth is generous here!"
Artisan: "New materials! New possibilities! I must experiment!"
Guard: "Visibility: zero. Hostiles: everywhere. This is a death trap."
```

**Jungle Temple Discovery:**
```
Scout: "Another structure! This jungle is FULL of secrets!"
Builder: "The craftsmanship... Remarkable. Redstone mechanisms, clearly."
Artisan: "I detect redstone circuits! This must be investigated carefully."
Old Salt: "Traps. Deadly ones. Watch your step, rookie."
```

### OCEAN (First Time)

**Characteristics:** Water, drowned, ocean ruins, ships, guardians
**Base Excitement:** High (vastness + danger)
**First Time Bonus:** +50%

```
Miner: "Water... Everywhere. No stone. I don't like this."
Builder: "Construction here... Requires completely different techniques."
Scout: "The OCEAN! It's ENDLESS! Look at the horizon!"
Farmer: "So much life below... But also so much danger."
Guard: "Exposure. No cover. Hostiles in the water. This is dangerous."
Artisan: "The physics... Water changes everything. New engineering challenges."
```

**Ocean Monument Discovery:**
```
Scout: "WHAT IS THAT?! Something massive in the water!"
Guard: "Hostiles detected. Multiple. dangerous. Stay back."
Builder: "That construction... Underwater? Impossible... But there it is."
Artisan: "Sponges! Ancient technology! This could revolutionize... everything!"
Old Salt: "Guardians... I've heard stories. Nothing good. Nothing good at all."
```

### PLAINS

**Characteristics:** Open space, villages, horses, flowers
**Base Excitement:** Low-Medium (peaceful + villages)

```
Miner: "Too open. No stone. Too much sky."
Builder: "Perfect for construction. Flat, open, accessible."
Scout: "You can see everything! But... it's a bit boring?"
Farmer: "This... this is perfect. Fertile, peaceful, beautiful."
Guard: "Maximum visibility. Easy to defend. Good location."
Artisan: "Village nearby! Trade opportunities! Knowledge sharing!"
```

**Village Discovery:**
```
Scout: "A VILLAGE! People! Well... villagers. But still!"
Builder: "Look at their construction techniques! Crude, but functional."
Farmer: "Crops! Animals! Other farmers! I want to talk to them!"
Artisan: "Trading opportunities... I must assess their inventory."
Old Salt: "Villagers... Good for trade. Bad for conversation. 'Hmmm.'"
```

### MOUNTAINS / EXTREME HILLS

**Characteristics:** Height, emeralds, snow, goats, fall damage
**Base Excitement:** Medium-High (height + emeralds)
**First Time Bonus:** +30%

```
Miner: "NOW we're talking! Look at this stone! And... EMERALDS!"
Builder: "The view... It's incredible. But building here will be challenging."
Scout: "We're SO HIGH! I can see the whole world from here!"
Farmer: "It's cold... But beautiful. Snow-covered peaks..."
Guard: "Elevation advantage. Excellent defensive position. But fall danger."
Artisan: "EMERALDS! The rarest ore! This is extraordinary!"
```

### TAIGA

**Characteristics:** Cold, snow, wolves, spruce, sweet berries
**Base Excitement:** Medium (cold + wolves)
**First Time Bonus:** +25%

```
Miner: "Cold stone. Good. Cold is better than heat."
Builder: "Spruce... Excellent timber. Strong, durable."
Scout: "Everything is covered in white! It's magical!"
Farmer: "Sweet berries! The earth provides even in the cold."
Guard: "Wolves nearby. Keep alert. They're not hostile unless provoked."
Artisan: "Temperature considerations... This will require planning."
```

### SWAMP

**Characteristics:** Water, lily pads, witch huts, slime
**Base Excitement:** Medium (witches + unique)
**First Time Bonus:** +35%

```
Miner: "Wet. Muddy. The stone is... slippery. Unpleasant."
Builder: "The foundation would be terrible here. Too much water."
Scout: "It's mysterious! Foggy! Strange!"
Farmer: "Lily pads... Blue flowers... Life finds a way everywhere."
Artisan: "Slime! Rare material! The crafting possibilities!"
Guard: "Limited visibility. Difficult terrain. I don't like it."
```

**Witch Hut Discovery:**
```
Artisan: "A WITCH HUT! Potions! Rare ingredients!"
Builder: "That construction... It's barely standing. How?"
Farmer: "A witch... I've heard stories. They're misunderstood, maybe?"
Old Salt: "Witches. Dangerous. Don't underestimate them."
```

### BADLANDS (MESA)

**Characteristics:** Terracotta, gold mines, mineshafts
**Base Excitement:** High (visual + gold)
**First Time Bonus:** +45%

```
Miner: "GOLD! I see gold in those cliffs! We're RICH!"
Builder: "The colors... Incredible. The terracotta... Beautiful material."
Scout: "It's like another world! The colors are amazing!"
Farmer: "Dry... But there's a stark beauty here."
Artisan: "Terracotta! Unique aesthetic possibilities! I must build with this!"
Old Salt: "I've seen many landscapes. This one... This one stays with you."
```

### DARK OAK FOREST

**Characteristics:** Huge trees, mushrooms, dense shade
**Base Excitement:** Medium (large trees)
**First Time Bonus:** +30%

```
Miner: "DARK. Finally, some shade I can appreciate."
Builder: "These trees... The scale is enormous! Incredible timber!"
Scout: "This forest feels ANCIENT. Like it holds secrets."
Farmer: "Mushrooms everywhere! Huge ones! Nature is miraculous."
Artisan: "Dark oak... Superior material quality. This is excellent."
```

### SAVANNA

**Characteristics:** Acacia, villages, horses, warmth
**Base Excitement:** Medium (acacia unique)
**First Time Bonus:** +25%

```
Miner: "Warm stone. Not ideal, but workable."
Builder: "Acacia wood... Unique patterns! Interesting aesthetic."
Scout: "It's like the plains, but... more exotic somehow!"
Farmer: "Warm, open... The animals here are beautiful."
Guard: "Good visibility. Defensible. Acceptable location."
```

### ICE PLAINS / SNOWY TUNDRA

**Characteristics:** Ice, snow, polar bears, igloos
**Base Excitement:** Medium (cold + igloos)
**First Time Bonus:** +30%

```
Miner: "FREEZING. The stone is frozen solid. My pickaxe might shatter."
Builder: "The cold affects everything. Mortar, materials, motivation."
Scout: "Everything is WHITE! It's beautiful! But cold..."
Farmer: "How does anything survive here? The resilience of nature..."
Guard: "Hypothermia risk. Reduced mobility. Dangerous environment."
Artisan: "Ice... Packed ice... Unique material with interesting properties."
```

**Igloo Discovery:**
```
Scout: "A structure made of ICE! How does it not melt?"
Builder: "Insulation engineering... Clever. The builders knew their environment."
Artisan: "There's a basement! This igloo has secrets!"
Old Salt: "Warm inside. Someone's been here recently. Or still is."
```

### THE NETHER (First Time)

**Characteristics:** Lava, netherrack, fortresses, unique mobs
**Base Excitement:** VERY HIGH (completely new dimension)
**First Time Bonus:** +70%

```
Miner: "It's... like home, but angry. Hotter. Deadlier."
Builder: "The HEAT! Materials will degrade! This is challenging!"
Scout: "ANOTHER WORLD! I can't believe we're HERE! INCREDIBLE!"
Farmer: "This... this isn't right. This place feels... wrong."
Guard: "Maximum alert. Hostiles everywhere. No safe zones."
Artisan: "New materials! New resources! The crafting potential!"
Old Salt: "The Nether... I've heard stories. They weren't exaggerating."
```

**Nether Fortress Discovery:**
```
Scout: "A FORTRESS! In the Nether! Who built this?!"
Builder: "That construction... It defies physics. How does it stand?"
Guard: "Blazes detected. Wither skeletons. This is extremely dangerous."
Artisan: "Nether wart! The brewing ingredient! This is essential!"
Old Salt: "Fortress... Brings back memories. Bad ones. Many didn't make it."
```

### THE END (First Time)

**Characteristics:** Void, end stone, dragons, cities
**Base Excitement:** MAXIMUM (final dimension)
**First Time Bonus:** +100%

```
Miner: "...I don't... This isn't stone. This is... alien."
Builder: "The architecture... Beyond comprehension. Beyond physics."
Scout: "I... I have no words. This is... everything."
Farmer: "There's no life here. It's... peaceful? Or empty?"
Guard: "The dragon... The threat... We need to be ready."
Artisan: "End stone... Chorus fruit... Materials beyond anything."
Old Salt: "After all these years... I finally made it. I'm here."
```

---

## Structure Discovery Reactions

Structures are special discoveries that blend awe with curiosity about who/what created them.

### VILLAGE

**Initial Discovery:**
```
Scout: "People! Well... villagers! But still!"
Farmer: "Other workers! Like us! I wonder if they'd trade?"
Artisan: "A smithy! A library! Knowledge! Resources!"
Builder: "Look at their construction techniques. Crude, but functional."
Old Salt: "Villagers. Great for trade. Terrible for conversation."
```

**First Trading Interaction:**
```
Artisan: "Their prices... Exorbitant. But their emeralds are pure."
Farmer: "They have crops! Different varieties! I want to learn!"
Builder: "They're using different building techniques. I should study them."
```

### DESERT TEMPLE

```
Scout (First time): "A PYRAMID! Rising from the sand! INCREDIBLE!"
Builder: "The precision... The alignment... This builders knew geometry."
Artisan: "Treasure chamber below... But also traps. Clever design."
Old Salt: "Every temple I've explored had guardians. Be ready."
Guard: "Single entrance. Easy to defend... Or trap intruders."
```

### JUNGLE TEMPLE

```
Scout: "Hidden in the vines! A secret temple!"
Builder: "The integration with nature... The builders respected this place."
Artisan: "Redstone mechanisms! Ancient technology! I must study it!"
Farmer: "Even here, people built. The drive to create is universal."
```

### OCEAN RUINS

```
Scout: "Underwater! RUINS! Who lived here? What happened?"
Builder: "Submerged construction... The engineering challenges..."
Artisan: "Buried treasure! The ocean preserves secrets well."
Miner: "Too much water. But... treasure is treasure."
```

### SHIPWRECK

```
Scout: "A shipwreck! Tragedy... but also TREASURE!"
Old Salt: "Been there. Not on a ship, but... I know the feeling."
Artisan: "Buried treasure maps! This could lead to riches!"
Builder: "The hull construction... Even in death, the craft remains."
```

### OCEAN MONUMENT

```
Scout: "Something MASSIVE! In the water! It's GLOWING!"
Guard: "Hostiles detected. Multiple threats. This is dangerous."
Builder: "That scale... Underwater... How is this possible?"
Artisan: "Sponges! Sea lanterns! Materials I've only read about!"
Old Salt: "Guardians... I've heard stories. None of them end well."
```

### PILLAGER OUTPOST

```
Guard: "Hostiles! Armed! Organized! This is a threat!"
Builder: "Look at their construction... It's military. Defensive."
Scout: "They're raiders... I can feel it. This isn't a trade post."
Old Salt: "Pillagers. I've fought them before. They don't negotiate."
```

### WOODLAND MANSION

```
Scout: "A MANSION! In the woods! It's HUGE!"
Builder: "The scale of this construction... Multiple rooms... Wings..."
Artisan: "This must have been the seat of power. Wealth. Knowledge."
Guard: "Multiple hostiles inside. Clearing this will be... difficult."
Old Salt: "I've explored many structures. This one... There's darkness here."
```

### STRONGHOLD

```
Scout: "Under this city... A STRONGHOLD! The secrets it must hold!"
Builder: "The scale is enormous! Multiple levels! Endless corridors!"
Artisan: "The libraries... The knowledge preserved here..."
Miner: "Finally! Stone! DEEP stone! This is where I belong!"
Old Salt: "A stronghold... This leads somewhere. Somewhere important."
```

### NETHER FORTRESS

```
Scout: "A FORTRESS! In the Nether! It's FLOATING!"
Builder: "Supporting that structure... Here? Impossible... But real."
Artisan: "Nether wart! The brewing essential! We must harvest!"
Guard: "Blazes. Wither skeletons. This is death waiting to happen."
```

### BASTION REMNANT

```
Scout: "What IS this?! It's like a fortress, but... different!"
Artisan: "Piglins! They're traders! But also... warriors?"
Builder: "The construction style... Brutal. Massive. Intimidating."
Old Salt: "Piglins don't like gold greed. Remember that. Or die."
```

### END CITY

```
Scout: "A CITY! In the END! Look at those TOWERS!"
Builder: "The architecture... It's alien. Beautiful. Impossible."
Artisan: "Elytra! The wings! And shulkers! The rarest materials!"
Miner: "End stone... Purple... Strange. But strong."
Old Salt: "We made it. All the way here. I never thought..."
```

### ANCIENT CITY (DEEP DARK)

```
Scout: "A city... UNDER the bedrock! Who BUILT this?!"
Builder: "The scale... The darkness... This place feels... old."
Artisan: "Sculk sensors! Redstone alternatives! Revolutionary!"
Guard: "I don't like this. Something watches. Something waits."
Old Salt: "The Deep Dark... I've heard whispers. Some places should stay lost."
```

### IGLOO

```
Scout: "A house! Made of ICE! How does it not melt?!"
Builder: "Insulation engineering... Brilliant use of local materials."
Artisan: "There's a basement! This igloo has secrets!"
Farmer: "Warm inside. They built a shelter that works."
```

### ABANDONED MINESHAFT

```
Miner: "Someone else mined here... Long ago. I wonder what happened?"
Builder: "The support beams... Rotting. This place is dangerous."
Scout: "A mine! That goes on forever! I want to explore!"
Artisan: "Minecart with chest! The previous miners left... things."
Old Salt: "Every abandoned mine has a story. Usually tragic."
```

### PORTAL RUINS (Ruined Portal)

```
Scout: "A broken portal! Someone tried to travel... Why didn't they return?"
Builder: "The construction... Damaged. But the shape is recognizable."
Artisan: "The obsidian is still here! We could reactivate it!"
Old Salt: "Portals... One-way trips, sometimes. Be careful."
```

---

## Rare Resource Reactions

### DIAMONDS

**First Discovery:**
```
Miner: "DIAMONDS! FINALLY! Look at that sparkle! BEAUTIFUL!"
Builder: "The ultimate material! Diamond tools! Diamond armor!"
Artisan: "The hardest material known! The crafting possibilities!"
Scout: "You found DIAMONDS?! That's INCREDIBLE!"
Old Salt: "Diamonds... I remember my first vein. Thirty years ago. Felt like yesterday."
```

**Subsequent Discoveries:**
```
Miner: "More diamonds. Good. Never enough."
Builder: "Excellent material quality. Always welcome."
Artisan: "Diamonds! I'll add these to the crafting reserves."
```

### EMERALDS (Extreme Hills)

**First Discovery:**
```
Miner: "EMERALDS?! In these mountains?! We're RICH!"
Artisan: "The rarest ore! Villagers trade for these!"
Builder: "Trading currency! This changes everything!"
Scout: "Another rare mineral! This world keeps giving!"
```

### ANCIENT DEBRIS (Nether)

**First Discovery:**
```
Miner: "What... What IS this? Ancient debris?"
Artisan: "Netherite! Better than diamond! The ultimate material!"
Builder: "This material... It doesn't burn. It's indestructible!"
Scout: "In this fiery hellscape, something THIS rare exists!"
Old Salt: "Netherite... I've heard stories. Never thought I'd see it."
```

### AMETHYST GEODE

**First Discovery:**
```
Scout: "Something's glowing! Inside this stone! You have to see this!"
Builder: "The crystals... The symmetry... Natural architecture!"
Farmer: "It's beautiful... Like the earth grew jewelry."
Artisan: "Amethyst! New material! New crafting possibilities!"
Miner: "Pretty. But can I mine it with my pick? Yes? Good."
```

### COPPER ORE

**First Discovery:**
```
Artisan: "COPPER! A new metal! The oxidation creates patina!"
Builder: "Unique aesthetic possibilities! This ages beautifully!"
Scout: "Orange metal! I've never seen anything like it!"
Miner: "Common, but... useful. Better than iron, maybe?"
```

### GLOW LICHEN

```
Scout: "It's GLOWING! In the cave! Life that LIGHTS up!"
Farmer: "Life finds a way everywhere. Even in the deepest dark."
Artisan: "Natural lighting! No torches needed! Brilliant!"
Builder: "Could be incorporated into builds... Interesting aesthetic."
```

### AZALEA / LUSH CAVE INDICATOR

```
Scout: "These flowers... They're different somehow."
Farmer: "Life! Green life! In a cave! How?"
Miner: "Caves below. Lush caves. Best kind of cave."
Builder: "Nature underground... The contrast is beautiful."
```

### SCULK / SCULK SENSORS

```
Artisan: "Sculk! Living technology! Vibration detection!"
Builder: "The organic architecture... It's not built. It's... grown."
Guard: "It detects movement... Tactical disadvantage."
Scout: "It's ALIVE! The cave itself is alive!"
```

### BUNDLE ITEM

```
Artisan: "A bundle! Storage optimization! This changes inventory management!"
Builder: "Practical. Efficient. Why didn't we think of this?"
Scout: "You can carry MORE things! Adventure is easier!"
```

### HEART OF THE SEA

```
Scout: "A HEART! From the ocean! It's BEAUTIFUL!"
Artisan: "The conduit ingredient! Ultimate underwater power!"
Builder: "This belongs in a monument. It feels... sacred."
Farmer: "The ocean's heart... We should treat it with respect."
```

### TOTEM OF UNDYING

```
Artisan: "A totem! It... it prevents death? How?!"
Guard: "Combat advantage. Resurrection. This is powerful."
Old Salt: "I've seen many warriors fall. This... This would have saved them."
Builder: "Evoker drops... These are earned in battle."
```

### DRAGON HEAD / ELYTRA

```
Scout: "A DRAGON HEAD! The ACTUAL head! And WINGS?!"
Builder: "Elytra! Flight without machines! Pure freedom!"
Artisan: "Dragon technology... The ultimate crafting material!"
Old Salt: "I never thought I'd see the day... We defeated the dragon."
```

### MUSIC DISCS

```
Scout: "Music! From the past! Someone made this!"
Artisan: "Cultural artifact! Music preserved!"
Builder: "The music... Melancholy. Beautiful. Haunting."
Farmer: "It tells a story. Someone, somewhere, felt this."
```

### ENCHANTED BOOK

```
Artisan: "Knowledge preserved! Magic captured in pages!"
Builder: "Enchantments! This could improve... everything!"
Scout: "A book with MAGIC! What does it do?!"
```

### NAME TAG

```
Scout: "We can NAME things! Give them identity!"
Farmer: "The animals... They're not just livestock anymore."
Artisan: "Labeling system! Inventory organization improvement!"
```

### SPONGE (Wet/Dry)

```
Artisan: "SPONGE! Ancient cleaning technology! Water removal!"
Builder: "This would make underwater construction feasible!"
Miner: "Soaking up water... Simple. But brilliant."
```

---

## Unusual Sightings

### RARE MOBS

**Pink Sheep:**
```
Farmer: "A PINK sheep! That's... incredibly rare! And adorable!"
Scout: "I've never seen a pink sheep! What are the odds?!"
Builder: "Genetic variation? Unique. Note it for the records."
```

**Brown Mooshroom:**
```
Farmer: "A BROWN mooshroom! The rare ones! I've only heard stories!"
Scout: "Two kinds of mushrooms cows! Nature is amazing!"
Artisan: "Mushroom stew... Infinite? With brown mooshrooms? Extraordinary!"
```

**Jockey Variants (Chicken Spider, etc.):**
```
Guard: "ABOMINATION! Spider riding a chicken... Kill it!"
Scout: "That's the most TERRIFYING thing I've ever seen!"
Builder: "How... How does that even work? The physics..."
```

**Fox ( Arctic/Red):**
```
Farmer: "A FOX! They're beautiful! And they hunt for us!"
Scout: "Look at them move! So graceful! So clever!"
Artisan: "They can pick up items! They're smarter than they look!"
```

**Bee Nests:**
```
Farmer: "BEES! They pollinate! They make honey! Essential workers!"
Scout: "They're so fuzzy! And dangerous! Perfect balance!"
Artisan: "Honey! Bottled honey! Honeycomb! Crafting possibilities!"
Builder: "The nests... Natural architecture. Fascinating."
```

**Panda:**
```
Farmer: "PANDAS! I've only heard STORIES! They're REAL!"
Scout: "They're so... round! And lazy! I love them!"
Artisan: "Bamboo! Fast growth! Sustainable farming material!"
```

**Turtle:**
```
Farmer: "Turtles! Ancient wisdom! They've been here forever!"
Scout: "They're adorable! And they lay eggs! Let's protect them!"
Artisan: "Turtle shells! Potions! Scute! Valuable crafting!"
```

**Dolphin:**
```
Scout: "DOLPHINS! They're our friends! They lead us to treasure!"
Farmer: "Intelligent creatures! They help us! Nature is generous!"
Guard: "They can detect underwater threats. Useful allies."
```

**Axolotl:**
```
Farmer: "AXOLOTLS! They're SO CUTE! And they attack guardians!"
Scout: "Little water puppies! I want a thousand of them!"
Artisan: "They play dead! Tactical genius! And they regenerate!"
```

**Glow Squid:**
```
Scout: "It's GLOWING! A squid that LIGHTS UP!"
Artisan: "Glow ink sacs! Signage! Art! Illumination!"
Builder: "Natural bioluminescence... Could be incorporated into builds."
```

**Rabbit (Killer Bunny Variant):**
```
Guard: "That rabbit... It's HOSTILE! Defend yourself!"
Scout: "It's so FLUFFY but so DANGEROUS!"
Old Salt: "Don't let appearances fool you. Small things can be deadly."
```

**Llama:**
```
Farmer: "LLAMAS! They carry things! And they SPIT!"
Scout: "They form caravans! We can have a llama train!"
Artisan: "Storage beasts! Efficiency improvement!"
```

**Polar Bear:**
```
Farmer: "A POLAR BEAR! Magnificent! Dangerous but magnificent!"
Guard: "Hostile if provoked. Protect the cub at all costs."
Scout: "They're so white! And big! And deadly!"
```

**Goat:**
```
Miner: "Goats! In the mountains! Like me!"
Scout: "They RAM things! It's hilarious!"
Builder: "Goat horns! Musical instruments! Natural resources!"
```

**Sniffer:**
```
Farmer: "A SNIFFER! I thought they were EXTINCT!"
Artisan: "Ancient seeds! Plants from the past! This changes botany!"
Scout: "It's a LIVING FOSSIL! We've rediscovered history!"
```

### ODD TERRAIN GENERATION

**Floating Island:**
```
Scout: "AN ISLAND! IN THE SKY! How does that even WORK?!"
Builder: "Physics? What physics? This defies explanation."
Artisan: "Could we build a bridge? Reach it? Explore it?"
Miner: "Stone in the sky. My kind of place. If I could reach it."
```

**Massive Overhang:**
```
Builder: "The overhang... The scale... Natural wonder!"
Scout: "It's like a wave of stone! Frozen forever!"
Miner: "Perfect shelter. Mining under there would be ideal."
```

**Surface Lava Lake:**
```
Miner: "Lava! On the surface! Convenient!"
Builder: "Dangerous beauty. But also... fuel source."
Artisan: "Obsidian! Infinite obsidian if we have water!"
```

**Ice Spikes:**
```
Scout: "SPIKES! Of ICE! The landscape is attacking!"
Builder: "Natural art. The geometry is perfect."
Farmer: "Cold beauty. Sharp, but beautiful."
```

**Huge Mushroom:**
```
Farmer: "A MUSHROOM! The size of a TREE!"
Artisan: "Mushroom blocks! Unique building material!"
Scout: "It's like a fairy tale! Magical forest!"
Builder: "Could be incorporated into builds... Interesting aesthetic."
```

**Bamboo Jungle:**
```
Farmer: "Bamboo! Fastest growing plant! Sustainable!"
Builder: "Construction material! Scaffolding! Versatile!"
Artisan: "Pandas eat it! Chests with it! Crafting material!"
```

**Cherry Grove (Sakura):**
```
Farmer: "CHERRY BLOSSOMS! The most beautiful trees!"
Scout: "Everything is PINK! It's magical! It's WONDERFUL!"
Builder: "The aesthetic... Perfect for themed construction."
Farmer: "Petals falling like snow... The world celebrates beauty."
```

### PLAYER BUILDS

**When Discovering Player Creation:**
```
Builder: "This... This isn't natural. Someone BUILT this."
Scout: "A creation! A person was here! We're not alone!"
Artisan: "The design... The techniques... I must study this."
Old Salt: "Another builder. Maybe as skilled as me. Maybe more."

[If build is impressive]
Builder: "This... This is MASTERWORK level. Who built this?"
Artisan: "Ingenious techniques! I must learn from this structure!"
Old Salt: "Rare to find such quality. Rarer still to find such dedication."

[If build is humble]
Builder: "Simple construction. But honest. Built with care."
Farmer: "Someone made a home. That matters."
```

---

## Personality-Based Wonder Expressions

The same discovery elicits different reactions based on personality type.

### THE SCIENTIFIC CURIOSITY RESPONSE
**Archetypes:** Artisan, Builder, Square (Precision Carpenter)
**Focus:** Understanding how/why, technical appreciation, potential applications

```
Discovery: Amethyst Geode

Builder: "The crystal formation follows perfect geometric principles.
Symmetrical growth patterns suggest consistent environmental conditions.
These crystals could enhance architectural acoustics.
The purple hue is natural? Remarkable pigment chemistry."

Artisan: "Silicon dioxide structure! Impurities create the purple coloration.
The light refraction properties... Could be used for signal transmission.
If we harvest carefully, the crystals could be used in redstone alternatives.
This merits detailed study."

Scientific Curiosity Dialogue Templates:
- "How did this form? The geology is fascinating."
- "The structural properties of this material..."
- "If we apply this technique to other applications..."
- "The physics of this creation are extraordinary."
- "We should document this. Record the observations."
```

### THE CHILDLIKE WONDER RESPONSE
**Archetypes:** Scout, Rookie, Farmer
**Focus:** Emotional reaction, beauty, excitement, "wow factor"

```
Discovery: End City

Scout: "INCREDIBLE! Look at the TOWERS! They FLOAT!
I've never seen ANYTHING like this!
It's like a DREAM but REAL!
We have to EXPLORE EVERYTHING!
Oh my goodness, oh my goodness, OH MY GOODNESS!"

Farmer: "It's... BEAUTIFUL!
Even in this alien place, there's beauty.
The purple, the stars, the endless void...
It makes me feel small but... in a good way?
Like I'm part of something bigger."

Childlike Wonder Dialogue Templates:
- "WOW! LOOK at that!"
- "It's BEAUTIFUL!"
- "I've NEVER seen anything like this!"
- "You HAVE to see this!"
- "This is the BEST thing EVER!"
- "It's like MAGIC but REAL!"
- "I can't BELIEVE my eyes!"
```

### THE PROFESSIONAL APPRECIATION RESPONSE
**Archetypes:** Old Salt, Boss, Builder
**Focus:** Craft admiration, recognizing skill, quality assessment

```
Discovery: Woodland Mansion

Old Salt: "Look at that joinery. Dovetail joints, even on this scale.
Whoever built this knew their craft.
The timber selection... Quality stuff. They didn't cut corners.
Been a thousand years since anyone lived here, but this place?
Still standing. That's not accident. That's SKILL."

Builder: "The architectural design is sophisticated.
Multiple wings, central hall, defensive considerations...
This was designed by a master. Someone who understood
both aesthetics AND structural integrity.
I could learn from this structure."

Professional Appreciation Dialogue Templates:
- "Quality construction. This will last."
- "The craft on display here... Exceptional."
- "Someone knew what they were doing."
- "This is masterwork level. Truly."
- "I appreciate the skill involved."
- "Built right. Built to last."
```

### THE GUARDED SKEPTIC RESPONSE
**Archetypes:** Guard, Old Salt (war stories), Eagle Eye
**Focus:** Danger assessment, past trauma, cautious curiosity

```
Discovery: Nether Fortress

Guard: "Structural analysis: ominous. Threat level: extreme.
Hostiles detected: multiple. No safe zones identified.
This fortress wasn't built for visitors.
It was built to KEEP people OUT.
Stay alert. Follow my lead. Nobody dies today."

Old Salt: "Nether fortress...
[Pause, distant look]
I've explored three of them. Before this one.
Lost good people. Good builders.
The treasure's worth it. Maybe.
But ask yourself: Is your life worth the risk?
[Darkly]
I already know my answer."
```

### THE MYSTICAL/SPIRITUAL RESPONSE
**Archetypes:** Farmer, Old Salt (reflective moments)
**Focus:** Sacredness, connection to earth, reverence

```
Discovery: Ancient City

Farmer: "This place... It's sacred. I can feel it.
The earth remembers. The stone remembers.
We should whisper here. Speak softly.
This isn't a place for grabbing.
This is a place for respect.
For listening to the past."

Old Salt: "Sometimes... you find a place that says 'stop'.
Not because of danger. Because of MEANING.
This city? It's older than anything we've seen.
People lived here. Loved here. Died here.
Maybe... we just honor them. And leave."
```

---

## Shared Discovery Call-and-Response

Shared discoveries create powerful social bonding through the "call and response" pattern.

### THE PATTERN

```
1. DISCOVERY CALL: One crew member spots something unique
2. RESPONSE: Others react with appropriate emotion
3. SHARED EXPERIENCE: Group acknowledgment creates bonding
4. MEMORY FORMATION: Becomes part of shared history
```

### EXAMPLE 1: Diamond Discovery

```
[Initial silence as miner breaks stone]

Miner: [Discovery Call]
"Wait... [gasp] DIAMONDS! DIAMOND ORE!
Look at this sparkle! BEAUTIFUL!
Boss! You have to see this!"

[Response from others]

Scout: "NO WAY! DIAMONDS?!"
[Rushes over]

"INCREDIBLE! You found the jackpot!"

Builder: "Let me see...
[Arrives, examines]
Exquisite quality. Multiple ores in one vein.
This is extraordinary. Exceptional find."

Old Salt: [Arrives, nods]
" diamonds. I remember my first vein.
[Smiles nostalgically]
Thirty years ago. Felt like yesterday.
Congratulations, kid. You earned this."

Boss: "Excellent work, team.
[Turns to miner]
Your persistence paid off.
These diamonds will serve us well.
Mark the location. We'll be back."

[Shared bonding moment]

Scout: "Remember this! We found diamonds TOGETHER!"
Miner: "Yeah... Yeah, we did."
[Group shares moment of pride]
```

### EXAMPLE 2: First Nether Entry

```
[The group approaches the Nether Portal]

Scout: [Discovery Call - already excited]
"The portal! It's GLOWING! The other side is WAITING!"
"We're ACTUALLY doing this! We're GOING to the Nether!"

[The portal activates]

[Silence as purple particles swirl]

Builder: [Response - apprehensive]
"Are we... are we certain about this?
The structural integrity of inter-dimensional travel...
Is it SAFE?"

Guard: [Response - protective]
"I'll go first.
If there's hostiles, I'll draw their attention.
Stay behind me. Stay ALIVE."

Old Salt: [Response - experience]
"I've been to the Nether three times.
[ grimly]
Each time, someone didn't come back.
But...
[looks at each crew member]
This crew? This crew is different.
We're going in together. We're coming out together."

[Shared moment]

Scout: "Together?"
All: "Together."

[They step through as a group]
```

### EXAMPLE 3: Ocean Monument Discovery

```
[The crew is exploring ocean ruins]

Scout: [Discovery Call]
"Wait... What's THAT?!
In the distance! Under the water!
Something MASSIVE!
It's GLOWING!
What IS that?!"

[Swims closer, gasps]

"EVERYONE! You HAVE to see this!
A structure! An UNDERWATER structure!
It's HUGE!"

[Others arrive]

Guard: [Immediately tactical]
"Hostiles! I see guardians!
Stay back! Defend yourselves!"
[Draws weapon]

Artisan: [Intellectual curiosity]
"An ocean monument! Ancient prismarine construction!
The architecture is... impossible!
How did they build this?!
How does it NOT collapse?!"

Builder: [Structural analysis]
"Look at the scale! Multiple rooms! Complex geometry!
Underwater engineering... It's beyond anything we've seen!
The builders... They must have been masters."

Farmer: [Spiritual response]
"There's something... sacred about this place.
The ocean preserved it.
Honored it.
We should be respectful."

[Shared moment of awe]

Scout: "We're looking at something ancient.
Something nobody's seen in... forever.
And we're seeing it TOGETHER."

[Group falls silent, appreciating the moment]

Old Salt: "Monuments...
I've explored two. Lost good people at the third.
[Pause]
But this crew? We're different.
Let's do it right. Let's do it TOGETHER."

[The group prepares, as a team]
```

### EXAMPLE 4: Ancient City Discovery

```
[The crew is mining at deepslate levels]

Miner: [Stops suddenly]
"Hold up. Everyone stop.
[Points]
Listen."

[Silence]

"You hear that? That... vibration?"

[Warden screech in distance]

Scout: "What IS that?!
It sounds... DANGEROUS!"

[They continue carefully]

[The city opens before them]

[Discovery Call]

Scout: "EVERYTHING!
STOP EVERYTHING!

LOOK!
[Points into the darkness]
A CITY!
An ANCIENT city!
Under the BEDROCK!
Who BUILT this?!
WHEN did they build this?!

You have to see this!
ALL of you!"

[The crew gathers at the edge]

[Individual Responses]

Builder: "The scale...
It's ENDLESS!
Corridors, rooms, structures...
This isn't just buildings.
This is a METROPOLIS."

Artisan: "Sculk! SCULK everywhere!
Living technology!
Vibration sensors!
The entire city is... ALIVE!"

Guard: "I don't like this.
Something watches.
Something WAITS.
That sound... The warden...
We're not alone here."

Farmer: [Whispered]
"This is sacred.
Can you feel it?
The earth remembers.
People lived here.
Loved here.
Died here.
We should be quiet.
Respectful."

Old Salt: "The Deep Dark...
[Turns to face each crew member]
I've heard stories.
My grandfather told me, when I was a kid.
Some places... some places should STAY lost."

[Silence]

Scout: "But... we found it.
WE found it.
Doesn't that MEAN something?
Doesn't that mean we're supposed to be here?"

[Group considers]

[Shared Moment]

Old Salt: [Slow nod]
"Maybe, kid. Maybe.
[Looks at the ancient city]
But we do it together.
Or we don't do it at all."

All: "Together."

[The group enters, as a team]
```

### RESPONSE PATTERNS BY PERSONALITY

**When Scout calls out a discovery:**

```
Miner: "Is it stone? Is it ore?
If not, why are we stopping?"

Builder: "Is it a structure?
Architecture worth seeing?
Show me."

Guard: "Is it dangerous?
Does it threaten the team?
Assess first, appreciate later."

Artisan: "Is it new technology?
Rare materials?
Crafting possibilities?"

Farmer: "Is it beautiful?
Is it natural?
Wonder is worth the time."

Old Salt: "Let me see.
[Observes]
Hmph.
[Softens, smiles]
Well now. That IS something."
```

**When Old Salt shares a discovery:**

```
[Unusual - Old Salt rarely gets excited]

Old Salt: "Boss. Scout. Come here.
[Points]
Found something."

[Others arrive immediately]

Scout: "You found something?!
Old Salt NEVER gets excited!
This must be INCREDIBLE!"

Builder: "If it impressed Old Salt...
It must be extraordinary."
```

**When Farmer shares a discovery:**

```
Farmer: "Everyone... come see this.
It's... beautiful."

[The group gathers, knowing Farmer rarely calls them over]

Scout: "Farmer found something beautiful?
I'm THERE."

Old Salt: "If Farmer says it's worth seeing...
It's worth seeing."
```

---

## First Time Flags System

The discovery reaction system needs to track "first time" experiences to provide special dialogue.

### FLAG ARCHITECTURE

```java
public class DiscoveryFlags {

    // Biome first-time flags
    private boolean enteredDesert = false;
    private boolean enteredJungle = false;
    private boolean enteredOcean = false;
    private boolean enteredNether = false;
    private boolean enteredEnd = false;
    private boolean enteredIcePlains = false;
    private boolean enteredBadlands = false;
    private boolean enteredDarkOakForest = false;
    private boolean enteredSavanna = false;
    private boolean enteredTaiga = false;
    private boolean enteredSwamp = false;
    private boolean enteredMushroomFields = false;

    // Structure first-time flags
    private boolean discoveredVillage = false;
    private boolean discoveredDesertTemple = false;
    private boolean discoveredJungleTemple = false;
    private boolean discoveredOceanRuins = false;
    private boolean discoveredShipwreck = false;
    private boolean discoveredOceanMonument = false;
    private boolean discoveredPillagerOutpost = false;
    private boolean discoveredWoodlandMansion = false;
    private boolean discoveredStronghold = false;
    private boolean discoveredNetherFortress = false;
    private boolean discoveredBastionRemnant = false;
    private boolean discoveredEndCity = false;
    private boolean discoveredAncientCity = false;
    private boolean discoveredIgloo = false;
    private boolean discoveredAbandonedMineshaft = false;
    private boolean discoveredRuinedPortal = false;

    // Resource first-time flags
    private boolean foundDiamonds = false;
    private boolean foundEmeralds = false;
    private boolean foundAncientDebris = false;
    private boolean foundAmethystGeode = false;
    private boolean foundGlowBerries = false;
    private boolean foundAzalea = false;
    private boolean foundSculk = false;
    private boolean foundCopper = false;
    private boolean foundLodestone = false;
    private boolean foundNetherite = false;

    // Mob first-time flags
    private boolean seenWolf = false;
    private boolean seenOcelot = false;
    private boolean seenFox = false;
    private boolean seenPanda = false;
    private boolean seenTurtle = false;
    private boolean seenDolphin = false;
    private boolean seenAxolotl = false;
    private boolean seenGlowSquid = false;
    private boolean seenBee = false;
    private boolean seenGoat = false;
    private boolean seenSniffer = false;
    private boolean seenRavager = false;
    private boolean seenEnderDragon = false;
    private boolean seenWither = false;
    private boolean seenWarden = false;

    // Special event flags
    private boolean experiencedThunderstorm = false;
    private boolean experiencedBloodMoon = false;  // If modded
    private boolean enteredEndDimension = false;
    private boolean defeatedEnderDragon = false;
    private boolean foundStronghold = false;

    // Get first-time bonus multiplier
    public double getFirstTimeExcitementMultiplier(String discoveryType) {
        if (isFirstTime(discoveryType)) {
            return 1.5 + Math.random() * 0.5;  // 1.5x to 2.0x excitement
        }
        return 1.0;
    }

    public boolean isFirstTime(String discoveryType) {
        return switch (discoveryType) {
            case "desert" -> !enteredDesert;
            case "jungle" -> !enteredJungle;
            case "ocean" -> !enteredOcean;
            case "nether" -> !enteredNether;
            case "end" -> !enteredEnd;
            case "diamonds" -> !foundDiamonds;
            case "village" -> !discoveredVillage;
            case "stronghold" -> !discoveredStronghold;
            case "ancient_city" -> !discoveredAncientCity;
            case "ender_dragon" -> !seenEnderDragon;
            default -> false;
        };
    }

    public void markDiscovered(String discoveryType) {
        switch (discoveryType) {
            case "desert" -> enteredDesert = true;
            case "jungle" -> enteredJungle = true;
            case "ocean" -> enteredOcean = true;
            case "nether" -> enteredNether = true;
            case "end" -> enteredEnd = true;
            case "diamonds" -> foundDiamonds = true;
            case "village" -> discoveredVillage = true;
            case "stronghold" -> discoveredStronghold = true;
            case "ancient_city" -> discoveredAncientCity = true;
            case "ender_dragon" -> seenEnderDragon = true;
        }
    }

    // NBT serialization for persistence
    public void writeToNBT(CompoundTag nbt) {
        nbt.putBoolean("enteredDesert", enteredDesert);
        nbt.putBoolean("enteredJungle", enteredJungle);
        nbt.putBoolean("enteredOcean", enteredOcean);
        nbt.putBoolean("enteredNether", enteredNether);
        nbt.putBoolean("enteredEnd", enteredEnd);
        nbt.putBoolean("foundDiamonds", foundDiamonds);
        nbt.putBoolean("discoveredVillage", discoveredVillage);
        nbt.putBoolean("discoveredStronghold", discoveredStronghold);
        nbt.putBoolean("discoveredAncientCity", discoveredAncientCity);
        nbt.putBoolean("seenEnderDragon", seenEnderDragon);
        // ... etc for all flags
    }

    public void readFromNBT(CompoundTag nbt) {
        enteredDesert = nbt.getBoolean("enteredDesert");
        enteredJungle = nbt.getBoolean("enteredJungle");
        enteredOcean = nbt.getBoolean("enteredOcean");
        enteredNether = nbt.getBoolean("enteredNether");
        enteredEnd = nbt.getBoolean("enteredEnd");
        foundDiamonds = nbt.getBoolean("foundDiamonds");
        discoveredVillage = nbt.getBoolean("discoveredVillage");
        discoveredStronghold = nbt.getBoolean("discoveredStronghold");
        discoveredAncientCity = nbt.getBoolean("discoveredAncientCity");
        seenEnderDragon = nbt.getBoolean("seenEnderDragon");
        // ... etc for all flags
    }
}
```

### FIRST TIME DIALOGUE SELECTION

```java
public String getDiscoveryReaction(DiscoveryType discovery,
                                   SpecializationType specialization,
                                   PersonalityProfile personality,
                                   DiscoveryFlags flags) {

    boolean isFirstTime = flags.isFirstTime(discovery.getId());
    double excitementMultiplier = flags.getFirstTimeExcitementMultiplier(discovery.getId());

    // Mark as discovered
    flags.markDiscovered(discovery.getId());

    // Select dialogue based on first-time status
    if (isFirstTime) {
        return getFirstTimeReaction(discovery, specialization, personality, excitementMultiplier);
    } else {
        return getStandardReaction(discovery, specialization, personality);
    }
}

private String getFirstTimeReaction(DiscoveryType discovery,
                                   SpecializationType specialization,
                                   PersonalityProfile personality,
                                   double multiplier) {

    // Apply excitement multiplier to personality-based response
    String baseReaction = switch (specialization) {
        case SCOUT -> getScoutFirstTimeReaction(discovery);
        case BUILDER -> getBuilderFirstTimeReaction(discovery);
        case MINER -> getMinerFirstTimeReaction(discovery);
        case FARMER -> getFarmerFirstTimeReaction(discovery);
        case GUARD -> getGuardFirstTimeReaction(discovery);
        case ARTISAN -> getArtisanFirstTimeReaction(discovery);
    };

    // Modify based on multiplier (more exclamation points, stronger adjectives)
    if (multiplier > 1.5) {
        return enhanceExcitement(baseReaction);
    }

    return baseReaction;
}

private String enhanceExcitement(String reaction) {
    // Add emphasis markers
    reaction = reaction.replace("!", "!!");
    reaction = reaction.replace(".", "!");

    // Add intensifiers if not present
    if (!reaction.contains("INCREDIBLE") && !reaction.contains("AMAZING")) {
        reaction = reaction.replace("Look at", "LOOK AT");
    }

    return reaction;
}
```

---

## Scientific Curiosity vs Childlike Wonder

### RESEARCH BACKGROUND

**Source:** [Curiosity Types Research](https://positivepsychology.com/types-of-curiosity/)

**Key Finding:**
- **Novelty-Seeking Impulses** (childlike wonder) decline with age
- **Specific/Cognitive Curiosity** (scientific) remains stable into adulthood

### MINEWRIGHT APPLICATION

**Workers with Childlike Wonder:**
- **Scout:** Openness 0.95 - Maximum novelty seeking
- **Rookie:** Openness 0.80 - Learning, enthusiastic
- **Farmer:** Openness 0.60 - Natural wonder, appreciation

**Workers with Scientific Curiosity:**
- **Artisan:** Openness 0.90 - Creative, experimental
- **Builder:** Openness 0.80 - Innovative, but structured
- **Old Salt:** Openness 0.50 - Traditional, practical curiosity

### COMPARATIVE REACTIONS

**Discovery: Amethyst Geode**

**Childlike Wonder (Scout):**
```
"Whoa! WHOA!
Something's GLOWING!
Inside that rock!
[Excited gasp]
You HAVE to see this!
It's BEAUTIFUL!
Like a treasure chest but BETTER!
Purple sparkles EVERYWHERE!
I want to LIVE in there!
Can we? Can we please?!"
```

**Scientific Curiosity (Artisan):**
```
"Silicate crystal formation.
Purple hue indicates manganese impurities.
Geometric structure suggests uniform growth conditions.
Light refraction properties... remarkable.
The crystals could have practical applications.
Redstone signal transmission? Illumination?
We should conduct experiments.
Document the properties.
This merits systematic study."
```

**Balanced Appreciation (Builder):**
```
"The formation is structurally remarkable.
Perfect symmetry in the crystal growth.
[Appreciative pause]
And aesthetically... quite beautiful.
Nature's architecture rivals human design.
We should harvest some crystals.
Both for study... and for decoration."
```

### WONDER MATURITY SPECTRUM

```
┌─────────────────────────────────────────────────────────────┐
│              CURIOSITY MATURITY SPECTRUM                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  CHILDLIKE WONDER  ←  BALANCED  →  SCIENTIFIC CURIOSITY    │
│  (Age 5-10)          (Age 20-30)      (Age 40+)            │
│                                                             │
│  "WOW! Look!"       "Remarkable!"    "Fascinating."        │
│  "SO PRETTY!"       "Beautiful!"     "Interesting."        │
│  "I want one!"      "Let's explore." "Let's analyze."      │
│  "Magic!"           "Amazing!"       "Explainable."         │
│                                                             │
│  Emotion-driven     Mixed response   Logic-driven          │
│  Pure excitement    Appreciation    Intellectual           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### PERSONALITY-BASED CURIOSITY

**Scout (Openness 0.95):**
```
"I'm not just excited... I'm ECSTATIC!
Every discovery is the BEST discovery!
I've never seen this before!
It's ALL new to me!
This is why I EXPLORE!"
```

**Farmer (Openness 0.60):**
```
"The earth is full of wonders.
Sometimes violent, sometimes gentle.
Always beautiful.
I appreciate what I find.
I don't need to understand everything.
Just appreciate it."
```

**Artisan (Openness 0.90):**
```
"I want to know HOW it works.
WHAT makes it function.
CAN I use this in crafting?
WILL it improve efficiency?
Curiosity drives innovation.
Wonder is the first step.
Understanding is the goal."
```

**Old Salt (Openness 0.50):**
```
"I've seen a lot.
Not everything.
But a lot.
Some things still surprise me.
[Grudging smile]
This place? Yeah. It surprised me.
Don't happen often anymore.
But when it does...
[Thoughtful pause]
I remember why I started exploring."
```

### AGE/EXPERIENCE CURVE

```
Novice (Rookie):     "WOW! EVERYTHING IS AMAZING!"
                      ↓
Intermediate:         "This is really cool!"
                      ↓
Experienced:          "That's interesting. I should learn about it."
                      ↓
Expert (Old Salt):    "Hmph. Not bad. Actually... quite remarkable."
```

---

## Java Implementation

### DiscoveryReactionManager Class

```java
package com.steve.ai.discovery;

import com.steve.ai.entity.SteveEntity;
import com.steve.ai.personality.PersonalityProfile;
import com.steve.ai.personality.SpecializationType;
import com.steve.ai.memory.SteveMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Manages discovery reactions for MineWright workers.
 * Handles first-time discoveries, personality-based responses,
 * and shared discovery call-and-response patterns.
 */
public class DiscoveryReactionManager {

    private final SteveEntity steve;
    private final DiscoveryFlags flags;
    private final SteveMemory memory;
    private final Random random;

    // Cooldown system to prevent spam
    private final Map<String, Long> lastReactionTime;
    private static final long REACTION_COOLDOWN_MS = 30000; // 30 seconds

    public DiscoveryReactionManager(SteveEntity steve) {
        this.steve = steve;
        this.flags = steve.getDiscoveryFlags();
        this.memory = steve.getMemory();
        this.random = new Random();
        this.lastReactionTime = new HashMap<>();
    }

    /**
     * Main entry point for discovery reactions
     */
    public void onDiscovery(DiscoveryEvent event) {
        if (!shouldReact(event)) {
            return;
        }

        String discoveryType = event.getType();
        boolean isFirstTime = flags.isFirstTime(discoveryType);

        // Generate reaction
        String reaction = generateReaction(event, isFirstTime);

        if (reaction != null && !reaction.isEmpty()) {
            // Speak the reaction
            steve.say(reaction);

            // Mark as discovered
            flags.markDiscovered(discoveryType);

            // Record in memory
            memory.addDiscovery(event);

            // Trigger shared discovery for nearby crew
            triggerSharedDiscovery(event, isFirstTime);

            // Update cooldown
            lastReactionTime.put(discoveryType, System.currentTimeMillis());
        }
    }

    /**
     * Determines if this discovery warrants a reaction
     */
    private boolean shouldReact(DiscoveryEvent event) {
        // Check cooldown
        String type = event.getType();
        Long lastTime = lastReactionTime.get(type);
        if (lastTime != null && System.currentTimeMillis() - lastTime < REACTION_COOLDOWN_MS) {
            return false;
        }

        // Check if this character would care about this discovery
        SpecializationType spec = steve.getSpecialization();
        if (!isRelevantToSpecialization(event, spec)) {
            return false;
        }

        // Check personality interest level
        PersonalityProfile personality = steve.getPersonality();
        double interestLevel = calculateInterest(event, personality);
        return random.nextDouble() < interestLevel;
    }

    /**
     * Calculate interest based on personality traits
     */
    private double calculateInterest(DiscoveryEvent event, PersonalityProfile personality) {
        double baseInterest = 0.5;

        // Openness increases interest in all discoveries
        baseInterest += personality.openness * 0.3;

        // Specific traits for specific discoveries
        switch (event.getCategory()) {
            case BIOME -> baseInterest += personality.openness * 0.2;
            case STRUCTURE -> baseInterest += personality.openness * 0.15;
            case RESOURCE -> {
                if (event.getRarity() >= Rarity.RARE) {
                    baseInterest += personality.conscientiousness * 0.2;
                }
            }
            case MOB -> baseInterest += personality.agreeableness * 0.1;
        }

        return Math.min(1.0, baseInterest);
    }

    /**
     * Generate appropriate reaction based on all factors
     */
    private String generateReaction(DiscoveryEvent event, boolean isFirstTime) {
        SpecializationType spec = steve.getSpecialization();
        PersonalityProfile personality = steve.getPersonality();
        int rapport = steve.getRapportWithPlayer();

        // Get base reaction
        String reaction = getBaseReaction(event, spec, isFirstTime);

        // Modify based on personality
        reaction = applyPersonalityModifiers(reaction, event, personality);

        // Modify based on rapport level
        reaction = applyRapportModifiers(reaction, rapport);

        return reaction;
    }

    /**
     * Get base reaction for discovery type and specialization
     */
    private String getBaseReaction(DiscoveryEvent event, SpecializationType spec, boolean isFirstTime) {
        return switch (event.getCategory()) {
            case BIOME -> getBiomeReaction(event.getBiome(), spec, isFirstTime);
            case STRUCTURE -> getStructureReaction(event.getStructure(), spec, isFirstTime);
            case RESOURCE -> getResourceReaction(event.getResource(), spec, isFirstTime);
            case MOB -> getMobReaction(event.getMob(), spec, isFirstTime);
        };
    }

    /**
     * Biome-specific reactions
     */
    private String getBiomeReaction(Biome biome, SpecializationType spec, boolean isFirstTime) {
        ResourceLocation biomeKey = biome.getRegistryName();

        if (isFirstTime) {
            return switch (spec) {
                case SCOUT -> getScoutFirstBiomeReaction(biomeKey);
                case BUILDER -> getBuilderFirstBiomeReaction(biomeKey);
                case MINER -> getMinerFirstBiomeReaction(biomeKey);
                case FARMER -> getFarmerFirstBiomeReaction(biomeKey);
                case GUARD -> getGuardFirstBiomeReaction(biomeKey);
                case ARTISAN -> getArtisanFirstBiomeReaction(biomeKey);
            };
        } else {
            return switch (spec) {
                case SCOUT -> getScoutBiomeReaction(biomeKey);
                case BUILDER -> getBuilderBiomeReaction(biomeKey);
                case MINER -> getMinerBiomeReaction(biomeKey);
                case FARMER -> getFarmerBiomeReaction(biomeKey);
                case GUARD -> getGuardBiomeReaction(biomeKey);
                case ARTISAN -> getArtisanBiomeReaction(biomeKey);
            };
        }
    }

    // Scout biome reactions
    private String getScoutFirstBiomeReaction(ResourceLocation biome) {
        String biomeName = biome.getPath();

        return switch (biomeName) {
            case "desert" -> "A DESERT! Endless sand! It's INCREDIBLE! Look at the vastness!";
            case "jungle" -> "A JUNGLE! So much GREEN! Everything's ALIVE here!";
            case "ocean" -> "The OCEAN! It's ENDLESS! Water everywhere!";
            case "ice_plains", "snowy_plains" -> "It's all WHITE! A winter wonderland!";
            case "badlands" -> "The COLORS! Look at these TERRACOTTA formations!";
            case "mushroom_fields" -> "MUSHROOMS everywhere! GIANT ones! This is MAGICAL!";
            case "nether" -> "The NETHER! It's... It's ANOTHER WORLD! INCREDIBLE!";
            case "the_end" -> "The END! I... I have no words. This is EVERYTHING.";
            default -> "A new biome! I've never seen this before! You have to see this!";
        };
    }

    private String getScoutBiomeReaction(ResourceLocation biome) {
        String biomeName = biome.getPath();

        return switch (biomeName) {
            case "desert" -> "More desert. Still impressive, but I've seen it before.";
            case "plains" -> "Plains again. Nice, but not very exciting.";
            case "forest" -> "Forest. Seen plenty of these.";
            default -> "Familiar territory. Nothing new.";
        };
    }

    // Builder biome reactions
    private String getBuilderFirstBiomeReaction(ResourceLocation biome) {
        String biomeName = biome.getPath();

        return switch (biomeName) {
            case "desert" -> "A desert... No timber. We'll need to import materials. The heat will affect construction too.";
            case "jungle" -> "Dense vegetation... The timber here is exceptional. But harvesting will be challenging.";
            case "badlands" -> "The terracotta formations... Remarkable. Unique aesthetic possibilities here.";
            case "nether" -> "The Nether... The heat! Materials will degrade! This presents engineering challenges!";
            case "the_end" -> "The architecture... The scale... Beyond comprehension. Beyond physics.";
            default -> "New biome to consider. Different construction challenges. I'll adapt.";
        };
    }

    private String getBuilderBiomeReaction(ResourceLocation biome) {
        String biomeName = biome.getPath();

        return switch (biomeName) {
            case "plains" -> "Plains. Ideal for construction. Flat, accessible.";
            case "forest" -> "Forest. Good timber supply. Decent building conditions.";
            default -> "Familiar biome. Standard construction techniques apply.";
        };
    }

    // Similar methods for Miner, Farmer, Guard, Artisan...
    // (Omitted for brevity, follow same pattern)

    /**
     * Apply personality modifiers to reaction
     */
    private String applyPersonalityModifiers(String reaction, DiscoveryEvent event,
                                            PersonalityProfile personality) {
        // High openness = more enthusiastic
        if (personality.openness > 0.8) {
            reaction = enhanceEnthusiasm(reaction);
        }

        // High conscientiousness = more detailed/analytical
        if (personality.conscientiousness > 0.8) {
            reaction = addDetail(reaction, event);
        }

        // Low extraversion = more reserved reactions
        if (personality.extraversion < 0.3) {
            reaction = toneDownReaction(reaction);
        }

        return reaction;
    }

    /**
     * Enhance enthusiasm for high-openness personalities
     */
    private String enhanceEnthusiasm(String reaction) {
        reaction = reaction.replace("!", "!!");
        reaction = reaction.replace(".", "!");

        if (!reaction.contains("INCREDIBLE") && !reaction.contains("AMAZING")) {
            String[] enthusiasticPrefixes = {
                "INCREDIBLE! ",
                "AMAZING! ",
                "You have to see this! ",
                "WOW! "
            };
            String prefix = enthusiasticPrefixes[random.nextInt(enthusiasticPrefixes.length)];
            reaction = prefix + reaction;
        }

        return reaction;
    }

    /**
     * Add technical details for high-conscientiousness personalities
     */
    private String addDetail(String reaction, DiscoveryEvent event) {
        String[] detailAdditions = {
            " I should document this.",
            " The properties are fascinating.",
            " This merits further study.",
            " I need to examine this more carefully."
        };
        String addition = detailAdditions[random.nextInt(detailAdditions.length)];
        return reaction + addition;
    }

    /**
     * Tone down reaction for low-extraversion personalities
     */
    private String toneDownReaction(String reaction) {
        reaction = reaction.replace("!!", "!");
        reaction = reaction.replace("INCREDIBLE", "Interesting");
        reaction = reaction.replace("AMAZING", "Notable");
        reaction = reaction.replace("YOU HAVE TO SEE THIS", "You should see this");
        return reaction;
    }

    /**
     * Apply rapport-based modifiers
     */
    private String applyRapportModifiers(String reaction, int rapport) {
        if (rapport < 25) {
            // Formal - add respectful address
            if (!reaction.startsWith("Sir,") && !reaction.startsWith("Boss,")) {
                reaction = "Sir, " + reaction.toLowerCase();
            }
        } else if (rapport >= 75) {
            // Close friendship - more casual, shared excitement
            reaction = reaction.replace("You have to see this!",
                                      "We have to see this TOGETHER!");
        }

        return reaction;
    }

    /**
     * Trigger shared discovery for nearby crew members
     */
    private void triggerSharedDiscovery(DiscoveryEvent event, boolean isFirstTime) {
        if (!isFirstTime) return;

        // Find nearby crew members
        List<SteveEntity> nearbyCrew = steve.level.getEntitiesOfClass(
            SteveEntity.class,
            steve.getBoundingBox().inflate(20.0)
        );

        // Remove self
        nearbyCrew.removeIf(s -> s == steve);

        if (nearbyCrew.isEmpty()) return;

        // Choose responder
        SteveEntity responder = nearbyCrew.get(random.nextInt(nearbyCrew.size()));

        // Generate response
        String response = generateSharedResponse(event, responder, steve);

        if (response != null) {
            // Delay response slightly for natural timing
            steve.level.getServer().tell(
                new TickTask(steve.level.getServer().getTickCount() + 20, () -> {
                    responder.say(response);
                })
            );
        }
    }

    /**
     * Generate response to shared discovery
     */
    private String generateSharedResponse(DiscoveryEvent event, SteveEntity responder,
                                         SteveEntity discoverer) {
        SpecializationType spec = responder.getSpecialization();

        return switch (spec) {
            case SCOUT -> generateScoutSharedResponse(event, discoverer);
            case BUILDER -> generateBuilderSharedResponse(event, discoverer);
            case MINER -> generateMinerSharedResponse(event, discoverer);
            // ... etc for all specializations
        };
    }

    private String generateScoutSharedResponse(DiscoveryEvent event, SteveEntity discoverer) {
        String discovererName = discoverer.getName().getString();

        return switch (event.getCategory()) {
            case BIOME -> String.format("%s found a NEW biome?! I'm coming!", discovererName);
            case STRUCTURE -> String.format("%s discovered something?! Where?! I want to see!", discovererName);
            case RESOURCE -> String.format("%s found something RARE?! Show me!", discovererName);
            case MOB -> String.format("%s saw a RARE creature?! Where is it?!", discovererName);
        };
    }

    /**
     * Check if discovery is relevant to specialization
     */
    private boolean isRelevantToSpecialization(DiscoveryEvent event, SpecializationType spec) {
        return switch (spec) {
            case MINER -> event.getCategory() == DiscoveryCategory.RESOURCE
                       || event.getBiomeKey().getPath().contains("caves");
            case BUILDER -> event.getCategory() == DiscoveryCategory.STRUCTURE
                       || event.getBiomeKey().getPath().contains("plains");
            case SCOUT -> true; // Scout cares about everything
            case FARMER -> event.getBiomeKey().getPath().contains("forest")
                       || event.getBiomeKey().getPath().contains("plains")
                       || event.getCategory() == DiscoveryCategory.MOB;
            case GUARD -> event.getCategory() == DiscoveryCategory.STRUCTURE
                       || event.getCategory() == DiscoveryCategory.MOB;
            case ARTISAN -> event.getCategory() == DiscoveryCategory.RESOURCE
                       || event.getCategory() == DiscoveryCategory.STRUCTURE;
        };
    }
}
```

### Discovery Event Class

```java
package com.steve.ai.discovery;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.entity.EntityType;

/**
 * Represents a discovery event
 */
public class DiscoveryEvent {

    private final DiscoveryCategory category;
    private final ResourceLocation biomeKey;
    private final String biomeName;
    private final BlockPos position;
    private final Rarity rarity;

    // Specific data based on category
    private final Structure structure;
    private final ResourceLocation resource;
    private final EntityType<?> mob;

    private DiscoveryEvent(Builder builder) {
        this.category = builder.category;
        this.biomeKey = builder.biomeKey;
        this.biomeName = builder.biomeName;
        this.position = builder.position;
        this.rarity = builder.rarity;
        this.structure = builder.structure;
        this.resource = builder.resource;
        this.mob = builder.mob;
    }

    public DiscoveryCategory getCategory() {
        return category;
    }

    public String getType() {
        return switch (category) {
            case BIOME -> "biome_" + biomeKey.getPath();
            case STRUCTURE -> "structure_" + structure.getKey().location().getPath();
            case RESOURCE -> "resource_" + resource.getPath();
            case MOB -> "mob_" + mob.getRegistryName().getPath();
        };
    }

    public ResourceLocation getBiomeKey() {
        return biomeKey;
    }

    public String getBiomeName() {
        return biomeName;
    }

    public BlockPos getPosition() {
        return position;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public Structure getStructure() {
        return structure;
    }

    public ResourceLocation getResource() {
        return resource;
    }

    public EntityType<?> getMob() {
        return mob;
    }

    public enum DiscoveryCategory {
        BIOME, STRUCTURE, RESOURCE, MOB
    }

    public enum Rarity {
        COMMON(0.2),
        UNCOMMON(0.4),
        RARE(0.6),
        EPIC(0.8),
        LEGENDARY(1.0);

        private final double excitementMultiplier;

        Rarity(double excitementMultiplier) {
            this.excitementMultiplier = excitementMultiplier;
        }

        public double getExcitementMultiplier() {
            return excitementMultiplier;
        }
    }

    public static class Builder {
        private DiscoveryCategory category;
        private ResourceLocation biomeKey;
        private String biomeName;
        private BlockPos position;
        private Rarity rarity;
        private Structure structure;
        private ResourceLocation resource;
        private EntityType<?> mob;

        public static Builder create() {
            return new Builder();
        }

        public Builder category(DiscoveryCategory category) {
            this.category = category;
            return this;
        }

        public Builder biome(Biome biome) {
            this.biomeKey = biome.getRegistryName();
            this.biomeName = biomeKey.getPath();
            return this;
        }

        public Builder position(BlockPos pos) {
            this.position = pos;
            return this;
        }

        public Builder rarity(Rarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public Builder structure(Structure structure) {
            this.structure = structure;
            this.category = DiscoveryCategory.STRUCTURE;
            return this;
        }

        public Builder resource(ResourceLocation resource) {
            this.resource = resource;
            this.category = DiscoveryCategory.RESOURCE;
            return this;
        }

        public Builder mob(EntityType<?> mob) {
            this.mob = mob;
            this.category = DiscoveryCategory.MOB;
            return this;
        }

        public DiscoveryEvent build() {
            return new DiscoveryEvent(this);
        }
    }
}
```

---

## Testing Examples

### Test Case 1: First Desert Entry

```
Scenario: Scout enters desert for the first time

Setup:
- Specialization: SCOUT
- Openness: 0.95
- Rapport: 50%
- Discovery Flags: enteredDesert = false

Expected Output:
"A DESERT! Endless sand! It's INCREDIBLE! Look at the vastness!"

Verification:
✓ First-time flag checked
✓ Scout-specific dialogue used
✓ High enthusiasm reflected (multiple exclamation marks)
✓ Discovery flag set to true
✓ Memory updated
```

### Test Case 2: Second Desert Entry

```
Scenario: Same Scout enters desert again

Setup:
- Specialization: SCOUT
- Openness: 0.95
- Rapport: 50%
- Discovery Flags: enteredDesert = true

Expected Output:
"More desert. Still impressive, but I've seen it before."

Verification:
✓ First-time flag checked (true)
✓ Standard dialogue used
✓ Reduced enthusiasm
✓ No memory update (not new)
```

### Test Case 3: Diamond Discovery

```
Scenario: Miner finds diamonds for first time

Setup:
- Specialization: MINER
- Openness: 0.50
- Rapport: 30%
- Discovery Flags: foundDiamonds = false

Expected Output:
"DIAMONDS! FINALLY! Look at that sparkle! BEAUTIFUL!"

Shared Response (nearby Scout):
"DIAMONDS?! Where?! Show me!"

Verification:
✓ First-time flag checked
✓ Miner-specific excitement
✓ Shared discovery triggered
✓ Scout responded appropriately
```

### Test Case 4: Ancient City Discovery

```
Scenario: Crew discovers Ancient City together

Setup:
- Discovery: Ancient City (LEGENDARY rarity)
- First time for all crew members

Expected Output:

Scout: "EVERYTHING! STOP EVERYTHING! LOOK! A CITY! An ANCIENT city! Under the BEDROCK!"

Builder: "The scale... It's ENDLESS! Corridors, rooms, structures... This isn't just buildings. This is a METROPOLIS."

Artisan: "Sculk! SCULK everywhere! Living technology! Vibration sensors! The entire city is... ALIVE!"

Guard: "I don't like this. Something watches. Something WAITS."

Farmer: [Whispered] "This is sacred. Can you feel it?"

Old Salt: "The Deep Dark... I've heard stories. Some places should STAY lost."

Verification:
✓ Legendary rarity triggered maximum excitement
✓ Each character's personality reflected
✓ Varied emotional responses (wonder, fear, reverence)
✓ Group interaction established
✓ Memory formation triggered
```

### Test Case 5: Villager Village Discovery

```
Scenario: Builder finds village for first time

Setup:
- Specialization: BUILDER
- Openness: 0.80
- Rapport: 40%
- Discovery Flags: discoveredVillage = false

Expected Output:
"A village! Other builders! I want to study their construction techniques."

Nearby Artisan Response:
"Village! Trading opportunities! I must assess their inventory!"

Verification:
✓ Professional curiosity reflected
✓ Builder focused on construction
✓ Artisan focused on trade
✓ Different priorities shown
```

---

## Summary

The Curiosity and Discovery Dialogue system provides:

1. **Rarity-Based Excitement** - Reactions scale from common to legendary
2. **First-Time Magic** - Special dialogue for first encounters
3. **Personality-Driven Responses** - Same discovery, different perspectives
4. **Shared Bonding** - "You have to see this!" creates team connection
5. **Authentic Construction Voice** - Wonder expressed through worker worldview
6. **Research-Based Design** - Grounded in psychology of awe and wonder
7. **Memory Formation** - Discoveries become part of shared history
8. **Cooldown System** - Prevents spam while maintaining engagement

**Implementation Priority:**
1. Core DiscoveryFlags system (NBT persistence)
2. DiscoveryReactionManager with basic reactions
3. First-time dialogue for major discoveries
4. Shared discovery call-and-response
5. Personality-based reaction modifiers
6. Cooldown and interest systems

This system transforms MineWright workers from mere tools into engaged companions who experience and share the wonder of exploration with the player.

---

**Document Version:** 1.0
**Created:** 2026-02-27
**Author:** MineWright Development Team
**Status:** Complete - Ready for Implementation

**Sources:**
- [Greater Good Science Center - Awe Definition](https://greatergood.berkeley.edu/topic/awe/definition)
- [Pearce et al. (2017) - Five Themes of Awe in Nature Tourism](https://www.researchgate.net/publication/317215300_Awe_and_Nature_Tourism)
- [Kashdan Curiosity Research](https://positivepsychology.com/types-of-curiosity/)
- [No Man's Sky Discovery Systems](https://www.nomanssky.com/)
- [Worldbuilding With NPC Dialogue](https://www.gamedeveloper.com/design/worldbuilding-with-npc-dialogue-a-beginner-s-guide)
- [Minecraft Bedrock NPC Dialogues](https://wiki.bedrock.dev/entities/npc-dialogues)

---

**End of Document**
