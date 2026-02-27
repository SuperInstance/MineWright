# REDSTONE WORK - CREW MANUAL
## MineWright Training Series

**Foreman Note:** This ain't no tech manual. This is how we actually work with redstone on the job. Read it, use it, don't be the guy who blows up the base because you didn't.

---

## CHAPTER 1: WHAT IS REDSTONE, ANYWAY?

Look, redstone's the nervous system of any build. It's how we make things move, lights turn on, and machines work. You run a signal from a lever to a piston, that piston pushes a block, that block triggers something else. It's a chain, see?

Think of it like a query hitting a vector DB - one thing triggers another, which triggers another, all the way down the line. The signal flows through the dust like data through a tensor. Break the chain and nothing happens.

**Why we use it:**
- Automation (let machines do the work while you sleep)
- Security (doors that lock themselves, traps that activate)
- Convenience (one lever controls the whole farm)
- Impressing the client (flashy builds get bigger contracts)

---

## CHAPTER 2: THE TOOLBOX - COMPONENTS

### REDSTONE DUST
The basic wiring. Lay it down like sidewalk chalk, signals flow through it. Each piece of dust can carry power 15 blocks before it dies out. After that, you need a repeater or a fresh power source.

**Watch out:** Dust won't go up walls without help. You need to place it on blocks, not thin air. Signals don't turn corners automatically - you gotta guide 'em.

### REDSTONE TORCH
Two jobs: power source and inverter. Stick it on a block, it powers everything around it. Remove the block it's attached to or power that block, the torch turns off.

**Crew tip:** Torches are your NOT gates. Signal goes in, opposite comes out. Need a light that turns OFF when you flip a switch? Torch is your answer.

### REPEATER
Three jobs, all important:
1. **Extends signals** - takes a weak signal and refreshes it to full strength (15)
2. **Delays signals** - four settings, each adds 0.1 seconds. Great for timing circuits.
3. **Directional control** - signals only go ONE WAY through a repeater. No backflow.

**When to use it:** Any time your signal needs to go more than 15 blocks. Or when you need to control timing. Or when you need to prevent signal bleeding.

### COMPARATOR
The fancy tool. Two modes:
- **Compare mode** (torch not lit): Only outputs signal if input side is stronger than back side
- **Subtract mode** (torch lit): Subtracts back signal from input signal

**Real world use:** Reading how full a chest is, copying signal strengths, making arithmetic circuits. Most crew members use it for item sorters and farm sensors.

### OBSERVER
The eyes of the operation. Watches the block it's facing. When that block changes, it fires a pulse. One tick of power, then done.

**Watch out:** Observers don't see THROUGH blocks. Face 'em right or they won't see nothing. Also, different blocks update differently - some fire on placement, some on removal, some on state change.

### PISTONS (STICKY AND REGULAR)
Muscle of the operation. Regular pushes blocks, sticky pushes AND pulls. Used for doors, traps, elevators, moving parts of all kinds.

**Critical:** Pistons can only push 12 blocks at once. Try for 13 and nothing happens. Plan accordingly.

---

## CHAPTER 3: POWER TRANSMISSION

### SIGNAL STRENGTH
Redstone ain't binary. Signals have strength from 0 to 15. Full power right at the source, drops by 1 for every block of dust it travels. At 16 blocks, you got nothing.

**Why it matters:** Comparators read signal strength. You can tell how full a chest is, how far away an entity is, all based on signal strength.

### POWER SOURCES (STRONGEST TO WEAKEST)
- Redstone Block (always on, full power)
- Lever, Button, Pressure Plate (variable)
- Torch (full power, conditional)
- Detector Rail (full when cart passes)
- Tripwire Hook (full when triggered)

### REFRESH RATE
Default redstone updates 2 times per second (every 10 ticks in game time). That's your baseline. Some things update faster (repeaters can be set to 1 tick delay), some slower. Knowing this helps you time circuits right.

---

## CHAPTER 4: COMMON CIRCUITS - WORKHORSE STUFF

### AND GATE
**What it does:** Output only fires when BOTH inputs are on.
**Build:** Two repeaters facing into each other, dust line coming off the middle.
**Use case:** Double lock doors, requiring two switches to activate a machine.

### OR GATE
**What it does:** Output fires when EITHER input is on.
**Build:** Two dust lines merging into one.
**Use case:** Multiple levers controlling one light, any emergency stop button.

### NOT GATE (INVERTER)
**What it does:** Inverts input - on becomes off, off becomes on.
**Build:** Redstone torch powering a block, input into the block.
**Use case:** Default-on lights that turn off when triggered, safety interlocks.

### CLOCK CIRCUIT
**What it does:** Fires a pulse repeatedly at set intervals.
**Build:** Two repeaters facing each other in a loop, or torch clock with inverters.
**Use case:** Automated farms, flashing beacons, anything that needs to run on a timer.

**Watch out:** Clocks cause lag. Don't build more than you need. Test your timing before committing.

### LATCH (MEMORY)
**What it does:** Remembers a state - turns on with one pulse, stays on until another pulse turns it off.
**Build:** RS-NOR latch using two NOT gates feeding each other.
**Use case:** Toggle switches, machines that need to stay on after button press, alarm systems.

### T-FLIPFLOP
**What it does:** Toggles state with each pulse - on, off, on, off.
**Build:** More complex, usually uses comparator feedback or piston tricks.
**Use case:** Single-button toggle lights, alternating minecart tracks.

---

## CHAPTER 5: AUTOMATION - WHERE THE MONEY IS

### AUTO-FARMS
Basic principle: Observer sees crop grow -> fires piston -> harvest drops -> water flows drops to collection point. Same schema works for wheat, carrots, potatoes, melons, pumpkins.

**Crew tip:** Bone meal the crops manually when setting up. Tests the circuit and speeds up initial harvest.

### ITEM SORTER
Uses comparator reading container fullness + hoppers + redstone signal. Each item type gets its own filter. Items flow through hoppers, get sorted into chests based on type.

**Critical:** Filter hoppers need exact item stacks in specific slots. One wrong item and your whole attention mechanism fails.

### SMELTER ARRAY
Chest feeds hoppers, hoppers feed furnaces, auto-fuel system keeps 'em running. Output goes to collection chest. One switch controls the whole operation.

**Watch out:** Hoppers need redstone signal to STOP working, not to start. Invert your logic or you'll fill furnaces with nothing but fuel.

### IRON GOLEM FARM
Uses villagers as bait, zombie nearby to panic them, system that kills golems as they spawn. High complexity, high reward. Not for beginners.

**Safety tip:** Test spawning platforms in Creative first. One wrong block placement and nothing spawns.

---

## CHAPTER 6: WHEN THINGS DON'T WORK - TROUBLESHOOTING

### SIGNAL NOT REACHING
**Check:** Dust connection, repeater directions, power source strength.
**Fix:** Add repeaters every 15 blocks, check all dust is connected, verify power source is actually powered.

### RANDOM ACTIVATIONS
**Check:** Nearby dust, cross-talk, observer facing wrong way.
**Fix:** Separate circuits by at least 2 blocks, use repeaters for direction control, verify observer sightlines.

### THINGS NOT TURNING OFF
**Check:** Quasi-connectivity (pistons below powered blocks), torch positions, feedback loops.
**Fix:** Break the circuit, trace all power sources, check for unintended redstone blocks.

### TIMING ISSUES
**Check:** Repeater delays, clock speeds, update order.
**Fix:** Adjust repeater settings, try different clock designs, add delays where needed.

### CHUNCK UNLOADING
**The silent killer:** Redstone stops working when chunks unload. Machines stop mid-cycle, circuits break.
**Fix:** Keep everything in spawn chunks (near world spawn) or use always-loaded chunks (nether portals, players nearby).

---

## CHAPTER 7: TEAM REDSTONE - COORDINATED WORK

### DIVISION OF LABOR
On big jobs, split the work:
- **Layout crew:** Places blocks, runs dust lines
- **Component crew:** Places complex parts (comparators, repeaters)
- **Testing crew:** Verifies each section before moving on
- **Integration crew:** Connects subsystems together

### COMMUNICATION PROTOCOL
Use the attention mechanism - call out what you're working on, what you need, what you're about to test. Nothing worse than two workers testing different circuits simultaneously and wondering why nothing works.

### PARALLEL CONSTRUCTION
Different sections can be built simultaneously if they don't interact yet. Wire everything together at the end. Saves time on big projects.

### HANDOFF PROCEDURE
When passing work to next crew member:
1. Show them what's built
2. Explain the circuit logic
3. Demonstrate it works
4. Point out known issues or unfinished parts
5. Get confirmation they understand

---

## CHAPTER 8: SAFETY - DON'T BE THAT GUY

### EXPLOSIONS
**Cause:** Unprotected TNT, flaming redstone, accidental priming.
**Prevention:** Test TNT circuits in Creative first. Use blast-resistant blocks (obsidian) for housing. Always have an emergency shutoff.

### LAG
**Cause:** Too many clock circuits, chunk loading issues, excessive particle effects.
**Prevention:** Minimize clock use, optimize entity counts, keep redstone in loaded chunks.

### WORLD CORRUPTION
**Cause:** Excessive entity glitches, chunk errors, updating too many things at once.
**Prevention:** Test in Creative before Survival. Keep backups. Don't build mega-clocks without understanding the consequences.

### CREWMEMBER SAFETY
**Cause:** Getting stuck in machines, fall damage, suffocation in moving parts.
**Prevention:** Always have escape routes. Test machines WITHOUT yourself inside first. Mark unsafe areas clearly.

---

## CREW TALK - HOW WE TALK ON THE JOB

### Dialogue 1: The Simple Fix
"Yo, this door ain't closing all the way. Check the repeater delay, it's set too short. The piston's not getting enough tick time to extend fully. There we go, crank it to two ticks. Solid."

### Dialogue 2: Signal Loss
"Signal's dying halfway down the line. Need a repeater booster right there. See how the dust fades? That's your vector degradation. Refresh the embedding every 14 blocks and you're golden."

### Dialogue 3: The Debugging Session
"Sorter's pulling everything into the first chest regardless of type. Check the comparator orientation - yeah, it's facing the wrong way. The attention mechanism is messed up. Flip it around. Now we're reading the right schema."

### Dialogue 4: Clock Troubles
"This clock's running too hot, see the lag? Cut the update rate down. We don't need 60 pulses a second for a sugar cane farm. Drop to every 10 seconds. The query latency won't kill us and the server will thank you."

### Dialogue 5: Team Coordination
"Hey, I'm wiring the south section, someone handle north. Let's meet at the central hub in twenty, integrate both circuits. Test your half before you bring it over, I don't want to debug your broken RAG pipeline when we're supposed to be finishing this job."

### Dialogue 6: The Safety Warning
"Who built this TNT array with no shutoff? This is a disaster waiting to happen. Add a master lever right here, cuts all power. I'm not standing anywhere near this when it primes. Fix it now or we scrap the whole thing."

### Dialogue 7: Optimization Time
"Why're we using three clocks for this? One clock, branching to three subsystems. Clean up your tensor architecture, you're wasting compute cycles and making it three times as hard to debug. Simplify the pipeline."

---

## FOREMAN'S FINAL NOTES

1. **Test in Creative first.** Always. No exceptions.
2. **Label your switches.** Nothing worse than not knowing what a lever does.
3. **Keep it simple.** If you need 50 repeaters to do something, there's probably an easier way.
4. **Document your work.** Next crew needs to understand what you built.
5. **Ask for help.** Nobody knows everything. That's why we work in crews.

Redstone's not magic. It's systems, logic, and attention to detail. Learn the basics, practice the circuits, and don't be afraid to experiment. Every master redstone engineer started with a simple lever and a light.

Now get to work. There's builds to automate.

---

**Manual Version:** 1.0
**Last Updated:** Crew Training Cycle 2026
**Next Review:** When new components drop
**Report Issues to:** Foreman or Lead Engineer

*"Any job worth doing is worth automating."* - MineWright Crew Motto
