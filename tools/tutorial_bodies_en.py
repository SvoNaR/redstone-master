# -*- coding: utf-8 -*-
"""Expanded lesson bodies (EN). Run: python tools/gen_tutorial_json.py"""

SEARCH = {
    "carry_signal": "carry signal lamp strength 0-15 dust lever weak strong power",
    "power_sources": "lever button plate block source pulse steady",
    "horizontal_wire": "horizontal dust corner routing wire",
    "vertical_wire": "vertical torch water observer tower floor",
    "repeater_extend": "repeater extend delay lock 15 dust boost",
    "torch_invert": "invert not torch negation",
    "torch_chain": "torch chain relay delay",
    "comparator_chest": "chest strength fill comparator container",
    "comparator_subtract": "subtract closed loop memory counter",
    "comparator_lectern": "lectern book page signal address",
    "piston_push": "push piston 12 blocks quasi connectivity",
    "piston_sticky": "sticky pull retract door bridge",
    "observer_detect": "observer detect pulse block update",
    "minecart_start": "launch rails minecart push",
    "minecart_hopper": "hopper minecart farm collect items",
    "minecart_activator": "activator rail boost pulse",
    "minecart_furnace": "furnace minecart coal ride",
    "minecart_stack": "train stack collision carts",
    "hopper_insert": "hopper insert lock chest redstone",
    "hopper_timer": "hopper timer clock comparator",
    "daylight_wait": "daylight sensor sky invert",
    "target_hit": "target shoot bow crossbow strength",
    "sculk_sound": "sculk vibration sound wool",
    "sculk_armor": "calibrated sculk armor filter vibration",
    "dropper_drop": "dropper eject container",
    "dispenser_activate": "dispenser bucket tnt arrow use",
    "crafter_gold": "gold ingot crafter slot lock",
    "crafter_cake": "cake craft ingredients hopper",
}

BODIES = {}

BODIES["carry_signal"] = """LESSON GOAL
Wire a power source, redstone dust, and an output (lamp) so the signal reaches the lamp reliably while you understand the 0–15 power scale.

SIGNAL BASICS
Redstone power is an integer from 0 (off) to 15 (maximum). Lamps, pistons, hoppers, and repeaters usually turn on at power ≥1, but comparators, analog circuits, and counters depend on the exact number.

Dust on the ground or on a block carries power along a line: each segment away from the source decreases power by 1. Levers and redstone blocks typically start at 15; after 15 dust segments only 1 remains, then the line dies. Without a repeater, horizontal dust runs are limited to 15 blocks.

STRONG VS WEAK POWER
Strong power (from a redstone block, repeater output, comparator output, side torch, etc.) can power adjacent blocks so dust and mechanisms on other faces of that block activate. Weak power (lever, button, pressure plate, dust line) feeds adjacent dust and devices but does not always power through a block the same way strong power does. Example: a side torch strongly powers the block and a lamp on top; a lever on a block weakly powers dust on top but not always below without extra wiring.

PRACTICE: LEVER → DUST → LAMP
1. Place a solid block. Put a lever on top; it outputs 15 when on.
2. Run dust from the lever. The first segment is 14, then 13, and so on. Connect a redstone lamp so dust touches it or powers the block under the lamp.
3. Toggle the lever—the lamp should follow (about 1 tick / 0.05 s propagation delay).

TROUBLESHOOTING
• Line longer than 15 dust—add a repeater (see Repeater section).
• Dust in mid-air or on glass without support—it will not link.
• Step up 1 block—dust does not climb alone; use a block with dust on top, a torch, or a repeater.
• Lamp not tied to the line—extend dust or strongly power the support block.

GOING FURTHER
Dust on top of blocks continues lines; 90° turns use raised blocks, repeaters, or torches. A redstone block outputs 15 on all faces—great for testing.

RELATED LESSONS
Power sources, horizontal routing, vertical routing, repeaters."""

BODIES["power_sources"] = """LESSON GOAL
Tell steady sources apart from pulses, know each source’s strength, and wire several to lamps to compare behavior.

STEADY SOURCES
• Redstone block—15 on all faces, strong power; always on, bulky and expensive.
• Lever—15 while on; weak power to the attached face; best manual switch.
• Pressure plate—15 while an entity stands on it; wood also reacts to dropped items and arrows.
• Weighted pressure plate—strength scales with entity count (up to 15).
• Tripwire hook—15 while the line is triggered.

PULSES AND SHORT SOURCES
• Button (stone ~1.5 s, wood ~1 s)—brief pulse to 15 then off; ideal for droppers and one-shot crafts.
• Target block—pulse on hit; strength depends on accuracy (Target section).
• Daylight detector, sculk sensor, observer—conditional sources (their own lessons).

PRACTICE
Build a “showcase”: parallel lamps, each fed by a different source through short dust. Lever stays on; button blinks; plate only works while you stand on it. Note which sources strongly power blocks versus only feeding dust.

CIRCUIT IMPLICATIONS
Use pulses when a device must fire once (dropper, crafter). Use steady power to hold a door open, keep a piston extended, or lock a hopper (redstone signal from below stops hopper transfer). A long button pulse through repeaters can “stretch” timing—for precise clocks use 1–2 tick monostables.

COMMON MISTAKES
Using a button where a lever is needed for memory; plates under your own door; ignoring strong/weak power when routing upstairs.

GOING FURTHER
Analog sources (containers, lectern, daylight) output 0–15, not just on/off—see Comparator and other sections.

REFERENCE TABLE (TYPICAL)
Redstone block and lever/button at trigger: 15. Wood button is shorter than stone—mind timings. Minecart on a detector plate over rails outputs 15 while seated—station automation. Tripwire with hooks: 15 when taut, 0 when broken—traps and perimeter lines. In Creative, run equal-length dust from each source to one lamp and confirm all start at 15 (except analog sensors)."""

BODIES["horizontal_wire"] = """LESSON GOAL
Route signals on a flat plane, turn corners, and pick dust, observer, or piston transport as appropriate.

DUST ROUTING
Dust links to neighboring dust on the same level (and limited vertical links). Floor lines: up to 15 segments from a source, then a repeater. Dust on block tops is common along walls.

90° turns: dust cannot step up 1 block alone. Fix: raise the line (block + dust on top), a repeater facing the turn, a side torch (mind inversion), or a comparator/repeater buffer.

Crossing lines on one level connects them—isolate with a block, repeater, or torch on a block.

OBSERVER TRANSPORT
An observer watches a block that will change (crops, piston-moved blocks). On update it outputs a 2-tick pulse from the back. Chains move “ticks” along farms without long dust. Downside: pulses, not levels—you may need memory or a repeater to hold state.

PISTON TRANSPORT
Pistons slide carrier blocks (redstone blocks, powered blocks with dust)—compact but heavy, 12-block push limit, delays, and cost.

PRACTICE
1. Straight 10–15 dust lever → lamp.
2. An “L” turn via block + top dust.
3. Two isolated lines—only one lamp should light.

MISTAKES
Floating dust; missing the 1-block step; repeater facing the wrong way (input is the back of the arrow).

GOING FURTHER
Long survival buses: repeater every 15 dust; creative tests can use intermediate redstone blocks as 15-power anchors.

OVERPASS WIRING
Lower dust powers a block; upper dust runs perpendicular—two lines cross without connecting. For 30 blocks: 15 dust—repeater—15 dust—end lamp; last segment must stay ≥1."""

BODIES["vertical_wire"] = """LESSON GOAL
Move redstone up or down several blocks—a core skill for multi-floor bases and farms.

TORCH TOWER
Block → side torch → block above → torch on another side. Each torch inverts and adds ~1 tick delay. Even torch count preserves phase; odd count inverts. Cheap and educational; slower and bulkier than repeaters.

REPEATER / DUST STAIRCASE
“Stair” dust and repeaters up a wall—no inversion, strength reset to 15 each repeater.

OBSERVER + PISTON
Moving a watched block fires observers—wave of pulses up a piston stack. Used in farms and hidden lifts.

WATER TRICKS
Flowing water can trigger block updates with observers—niche farm tech; know it exists after you master torches.

PRACTICE
Lever on the ground, lamp 3 blocks higher. Build a torch tower or repeater ladder; confirm on/off matches expectation (torches invert). Toggle the lever—no stuck states.

MISTAKES
Forgotten inversion; torch towers without understanding delay; trying to run dust straight up through air.

RELATED
Combine with horizontal routing and repeaters for full builds.

EXTRA: PARALLEL COLUMNS
For 10+ blocks up, mix block+side torch steps with repeater steps to limit torch delay stack. Closed indoor bases still need torch/repeater towers—daylight sensors do not replace vertical wiring underground."""

BODIES["repeater_extend"] = """LESSON GOAL
Extend dust beyond 15 blocks, restore power to 15, and add controlled delay when needed.

HOW REPEATERS WORK
A repeater reads the back (opposite the arrow) and outputs 15 if input ≥1. Place one where strength would otherwise fall off. Delay: 1–4 ticks (right-click), ~0.05 s per tick (20 t/s).

THE 15-BLOCK RULE
At most 15 dust between repeaters. Pattern: source 15 → 15 dust → repeater → 15 dust → … → lamp.

ORIENTATION
Arrow points forward. Input from behind or powered block behind. Sides are not normal inputs (except lock mechanics).

DELAY USES
2–4 ticks between stages stretch pulses for shift registers, sorters, hopper sync. Too much delay breaks fast clocks.

LOCKING
Strong side power locks the repeater so output stops following input—memory building block in advanced designs.

PRACTICE
Lever → exactly 15 dust → repeater → 15 dust → lamp. Break segments to see where power hits 0. Add a second repeater for 30+ blocks.

MISTAKES
Repeater facing the source; 16+ dust between repeaters; accidental side lock wiring.

GOING FURTHER
Repeaters act as one-way diodes—isolating feedback in counters.

DELAY TABLE
1 tick = 0.05 s; 4-tick repeater = 0.2 s. Four repeaters each at 4 ticks can stretch ~0.8 s between line pulses—slow farms. On Java Edition repeaters update neighbors when toggling—can bud adjacent pistons; isolate with blocks if needed. Two repeaters back-to-back without dust between sum their delays while keeping strength 15."""

BODIES["torch_invert"] = """LESSON GOAL
Build a NOT gate with a redstone torch and see the lamp on when the lever is off.

TORCH LOGIC
A torch on a solid block is lit (output 15) when that block is NOT strongly powered. Power the block → torch off → output 0. That is inversion.

PRACTICE WIRING
Lever powers block A. Torch on side of block B. Dust from torch to lamp. Classic: lever → block → torch → dust → lamp. Lever OFF → block unpowered → torch ON → lamp ON.

TIMING
Torch toggle ~1 tick—mind burn-out risks in very fast loops in older guides; still avoid pointless tight oscillation.

USES
Inverted buttons, alarm “off when powered”, teaching NAND/RS paths later.

MISTAKES
Torch on air; weak power where strong is needed; wrong block powered.

PRACTICE
Truth table: lever on/off vs lamp. A second lever preview for RS memory later.

EXTENDED NOT VARIANTS
A: lever powers the torch block directly. B: lever dust into block, torch on side. C: lever on block, torch on block above. Sketch all three and mark strong vs weak power. For doors, NOT from a button pairs with memory for “open after release” behavior.

LAMP ON BLOCK
A lamp on a powered block glows from block power without dust—a NOT torch on a neighbor can unpower that block and turn the lamp off—another inversion style without dust to the lamp."""

BODIES["torch_chain"] = """LESSON GOAL
Send signals through torch relays knowing inversion and stacked delay.

RELAY CHAIN
Each torch powers the next block and inverts. Even count = same polarity; odd = inverted. ~1 tick per torch. Five torches ≈ 5 ticks (0.25 s). Repeaters often beat long chains for distance without flipping logic.

WHEN TO USE TORCHES
Tight shafts, hidden wiring, learning logic. Large buses use repeaters/comparators.

PRACTICE
3–5 steps up a wall; lever in, lamp out; compare delay to an equal repeater ladder.

MISTAKES
Torches fighting on one block; runaway flicker without a break repeater.

GOING FURTHER
Torch towers pair with the vertical routing lesson.

DELAY NOTE
Five torches ≈5 ticks; shorter input pulses may not reach the end—stretch input with a 2–4 tick repeater. Hopper transfers every 8 ticks—align torch count or add comparator delay for sync."""

BODIES["comparator_chest"] = """LESSON GOAL
Read container fill as analog 0–15 and use default compare mode.

MEASURE MODE
Comparator rear faces the container (chest, barrel, hopper). Output strength depends on slot fill (formula uses item types/stacks). Empty ≈ 0; full chest ≈ 15. Mid values enable “almost full” sorters and refuel logic.

COMPARE MODE (default)
Output 15 if rear ≥ side; else 0. Example: furnace only when coal count exceeds ore threshold via side reference.

SUBTRACT MODE
Right-click toggles (front torch off)—see subtract lesson: max(0, rear − side).

PRACTICE
Chest + comparator + dust → lamp. Add stacks—strength changes. Compare rear to side power 8—adjust threshold.

MISTAKES
Comparator facing wrong way; expecting double-chest read without proper attachment; mixing modes.

GOING FURTHER
Hopper fill scales differ—experiment for precise sorter thresholds.

FILL FORMULA (SIMPLIFIED)
The game weighs item types and stacks, not slot count alone. One diamond stack already raises output noticeably. Test 1, 8, 16, 32, 64 items and log comparator strength. Lamp through compare≥8 = “half full” indicator. Barrels and shulker boxes follow the same rear-face read rule. Double chests: a comparator on one half only reads that half’s 27 slots—use two comparators or hopper merge for total fill."""

BODIES["comparator_subtract"] = """LESSON GOAL
Use subtract mode for feedback loops, memory, and counters.

FORMULA
Subtract (front small torch off): output = max(0, rear − side). No negative power—floor at 0.

CLOSED LOOP
Output feeds back to the side through delayed repeaters—basis of memory cells when combined with torches/repeaters.

PRACTICE (SIMPLE)
Subtract comparator: rear from lever, side from its own output through 2-tick repeaters. Add a reset button. Watch a short input “write” state.

DELAY REQUIRED
Without 1–2 ticks between output and side, the circuit may oscillate.

MISTAKES
Wrong mode; zero delay; subtract without a side signal.

GOING FURTHER
Item counters with hopper + subtract + hopper clock.

NUMBER EXAMPLE
Rear 12, side 5 → output 7. Rear 5, side 9 → 0. Memory write: side pulled to 15 → output dies; side 0 with rear 15 → cell holds. Tie to hopper clock lesson. Practice subtract with two levers (rear/side) before building RAM from tutorials."""

BODIES["comparator_lectern"] = """LESSON GOAL
Read lectern book page as 0–15 power and use pages as addresses.

MECHANICS
Rear faces the lectern. Strength follows current page (up to 15 on a full book). Players flipping pages change output live—codes, channel select, puzzle doors.

PRACTICE
Lectern + book + comparator → lamps through compare thresholds (3, 7, 11) or one lamp only on the “correct” page.

USES
Teaching analog redstone, adventure maps, rare survival locks.

MISTAKES
No book → 0; wrong facing; expecting level 16 (max 15).

GOING FURTHER
Combine chest analog with subtract for threshold math.

BOOK PAGES LAB
Write 16 pages, log strength on pages 1/5/10/15. Four lamps with compare thresholds 4/8/12/15—a simple address panel demo."""

BODIES["piston_push"] = """LESSON GOAL
Push blocks with a normal piston, respect the 12-block limit, and know immovable blocks.

EXTENSION
On power, the piston head extends 1 block and pushes up to 12 connected movable blocks. Timing: roughly 2 ticks per extend/retract cycle in typical setups.

CANNOT PUSH (EXAMPLES)
Bedrock, command blocks (survival), respawn anchor, open furnace GUI, some special states, obsidian, many technical blocks. Slime/honey carry groups—advanced.

PRACTICE
Piston + button + stone block in front. Watch motion. Stack 13 blocks—13th should not move. Lamp on button dust for sync.

QUASI-CONNECTIVITY (BRIEF)
Pistons can activate from powering blocks below/side—hidden doors; know it exists beyond basic push.

MISTAKES
Pushing into air with nothing to move; expecting bedrock motion; confusing sticky vs normal.

GOING FURTHER
0-tick and double-piston farms are advanced; not required here.

PUSH LIST
Sand/gravel push; 12-block row test—13th stays. Closed chest pushes; open GUI does not. Verify in your version for modded blocks."""

BODIES["piston_sticky"] = """LESSON GOAL
Pull the first moved block back with a sticky piston when power turns off.

VS NORMAL
Sticky retracts and pulls the last pushed movable block if nothing blocks it. Normal pistons leave blocks behind.

USES
Hidden doors, sliding floors, bridges, block sorters, observer farms (Observer section).

PRACTICE
Sticky piston + button + block. On → extend; off → retract with block. Two-block chain—only the adjacent movable rules apply.

LIMITS
Air gap after push—nothing to pull. Huge 12-block trains may not fully retract. Stable off signal required.

MISTAKES
Sticky where only push is needed; immovable blocks; two pistons fighting one block.

GOING FURTHER
Slime/honey block clusters move platforms as one."""

BODIES["observer_detect"] = """LESSON GOAL
Detect block updates on the observer face and output a 2-tick rear pulse.

LAYOUT
Face watches the target block. Rear outputs power 15 for 2 ticks on block update (growth, piston move, state change, some inventory updates).

DETECTS
Crop growth, piston slides, many block state changes—not a full analog level sensor, just “something changed.”

PRACTICE
Observer watching sand/crop; dust → lamp. Break block—pulse. Chain three observers—pulse travels.

FARMS
Bamboo/sugar cane/cactus → piston/dispenser; often need a repeater or T-flip-flop to hold.

MISTAKES
Face to air; expecting steady power; spam updates without delay.

GOING FURTHER
Observers on containers react to item changes—sorters."""

BODIES["minecart_start"] = """LESSON GOAL
Start a minecart on rails manually and with redstone.

RAILS
Normal rails—gravity on slopes, slowdown on flat. Powered rails boost while powered. Activator rails give a kick and special activation (hopper carts wake up). Detector rails pulse when passed.

START
Place rails, place cart, push by hand or power activator/powered rail under the cart.

PRACTICE
10-block track + hand push; then button → activator rail at start; add powered sections for hills.

MISTAKES
Cart off-rail; no boost on long flat spans; wrong rail type.

GOING FURTHER
Stations and switches—wiki Rails article."""

BODIES["minecart_hopper"] = """LESSON GOAL
Build a simple item collection loop with a hopper minecart under tracks.

MECHANICS
Hopper carts pick items from below and from containers under track when active. Activator rails wake locked hopper carts to transfer into stationary hoppers.

FARM SKETCH
Drop zone under rail; cart loop collects and unloads at a station (activator + hopper → chest).

PRACTICE
Rail over hopper → chest. Drop items; run cart; check chest fill.

MISTAKES
No station stop; hopper locked by redstone below; missing activator at unload.

GOING FURTHER
Pair with activator rail and Hopper lessons."""

BODIES["minecart_activator"] = """LESSON GOAL
Power activator rails to boost carts and activate special cart types.

BEHAVIOR
Powered activator rail under a cart gives a speed pulse and briefly activates hopper/TNT carts per version rules.

PRACTICE
Button → dust → activator under stopped cart—cart moves. Hopper cart locked until activator pulse transfers items.

VS POWERED RAILS
Powered rails sustain speed while on; activators give kicks. Combine on long routes.

MISTAKES
No power to rail; cart not centered; wrong rail block.

GOING FURTHER
Detector rails for automatic station pulses."""

BODIES["minecart_furnace"] = """LESSON GOAL
Ride a furnace minecart with fuel on rails.

USE
Put coal/wood as fuel, sit, drive forward on rails. Consumes fuel over time; not a boat replacement off-rail.

PRACTICE
Track + coal in cart + ride. Add powered rails on climbs.

NOTES
Check your Minecraft version—cart physics differ from old guides. Automation often prefers powered rails over furnace carts.

MISTAKES
No fuel; steep hill without powered rails; expecting boat controls off-rail.

GOING FURTHER
Multi-cart trains—experiment for projects."""

BODIES["minecart_stack"] = """LESSON GOAL
Understand many carts on one rail—trains, collisions, pushing.

MECHANICS
Carts push each other into trains. Hooks (if available) or tight packing. Rear collisions transfer momentum.

PRACTICE
Stack many carts on one line (as the lesson title suggests). Push from behind or use activator rail—watch speed loss and bounce at stops.

USES
Fun tests, lag checks, basis for detector-rail stations.

MISTAKES
Expecting infinite speed without powered rails; derailing on bad turns.

GOING FURTHER
Per-cart unloading stations—advanced logistics."""

BODIES["hopper_insert"] = """LESSON GOAL
Move items through hoppers into containers and lock hoppers with redstone.

FLOW
Hoppers pull from above inventories/drops and push down/sideways. Typical rate ~1 stack per 8 ticks (0.4 s)—slow but reliable.

LOCK
Power 15 below (or powered block below) stops pull and push—farm stops, single-item timing, comparator sync.

PRACTICE
Hopper on chest, drop item—lands in chest. Power below with lever—flow stops while on.

MISTAKES
Wrong facing; double chest half mismatch; expecting instant transfer.

GOING FURTHER
Side chains for item sorters."""

BODIES["hopper_timer"] = """LESSON GOAL
Build a hopper clock—timer using hoppers and a comparator.

IDEA
Two hoppers (or hopper+chest) cycle an item; comparator reads partial fill and pulses. Period depends on item count and repeater delays.

PRACTICE
Minimal two-hopper loop + comparator → lamp. Tune item count for different periods.

USES
Slow furnaces, farms, cheap clocks without many repeaters.

MISTAKES
Wrong comparator mode; hopper always locked; no item to cycle.

GOING FURTHER
Compare to repeater clocks and daylight sensors."""

BODIES["daylight_wait"] = """LESSON GOAL
Read sky light with a daylight detector and toggle day/night mode.

REQUIREMENTS
Clear view of the sky above the detector. Output 0–15 by time/weather—noon highest, night lower.

INVERT
Right-click toggles inverted mode—handy for “lamps on at night” without a torch NOT.

PRACTICE
Roof detector → dust → lamp. Wait for sunset—output changes. Toggle invert—behavior swaps.

USES
Street lights, night doors, mob farm timing.

MISTAKES
Under solid roof; expecting exact clock without comparator thresholds.

GOING FURTHER
Compare mode threshold for “only noon.”"""

BODIES["target_hit"] = """LESSON GOAL
Trigger a target block with projectiles and see strength vs accuracy.

MECHANICS
Hits from arrows/snowballs/tridents (per version) output power; center hits stronger (up to 15). Good for ranges, minigames, comparator scoring.

PRACTICE
Target → dust → lamp. Shoot at varying distance and draw strength—use comparators for thresholds.

MISTAKES
Melee does not work; expecting hold without repeat hits; blocked faces.

GOING FURTHER
Multiple targets for “hit all” door puzzles."""

BODIES["sculk_sound"] = """LESSON GOAL
Trigger a sculk sensor with vibrations—steps, blocks, shots—and get a redstone pulse.

MECHANICS (1.19+)
Sensors listen in radius; different actions have vibration frequencies. Pulse output; sculk may spread unless wool/carpet blocks spread. Wool also isolates sensors.

PRACTICE
Sensor → dust → lamp. Jump nearby—pulse. Place block—pulse. Wool perimeter to limit sculk growth.

SAFETY
Avoid summoning the Warden in Deep Dark; practice in safer biomes.

MISTAKES
Noise chaos; no wool—sculk spreads everywhere.

GOING FURTHER
Calibrated sensor—next lesson."""

BODIES["sculk_armor"] = """LESSON GOAL
Configure a calibrated sculk sensor to filter vibration types—example: armor unequip.

CALIBRATION
Calibrated sensors take a tuning signal and listen for one frequency class (steps, blocks, projectiles, equip changes—see wiki table).

PRACTICE
Set filter per wiki/in-game tuning. Remove armor near sensor—lamp on; jump may not trigger if filtered out.

USES
Hidden bases, traps, event-only entrances without mob false triggers.

MISTAKES
Wrong frequency; unfiltered sensor hears everything; no wool isolation.

GOING FURTHER
Sensor chains as event buses—advanced."""

BODIES["dropper_drop"] = """LESSON GOAL
Eject items with a dropper on a pulse into containers or the world.

VS DISPENSER
Droppers only drop items. Dispensers “use” many items (buckets, TNT, spawn eggs). Do not swap them in farms.

POWER
1-tick pulse = one eject. Hold power—repeats every 8 ticks while items remain.

PRACTICE
Dropper facing hopper/chest; item inside; button—item transfers. Face down—drops to ground.

MISTAKES
Full container overflows; level power without repeated ejects; using dispenser for pure transport.

GOING FURTHER
Dropper elevators—item vertical transport."""

BODIES["dispenser_activate"] = """LESSON GOAL
Dispense items that have special use: water/lava buckets, arrows, eggs, TNT, armor on stands, etc.

MECHANICS
Dispenser tries to use the item toward its facing: place fluids, ignite TNT, shoot arrows, equip armor per rules.

PRACTICE
Dispenser + button. Water bucket at a source block. TNT only in safe areas. Compare dropper on same item.

DANGER
Fire, explosions, flooding—test far from spawn.

MISTAKES
Expecting crafting (use Crafter); wrong item; hopper blocking slot.

GOING FURTHER
Water dispensers for harvest farms—classic."""

BODIES["crafter_gold"] = """LESSON GOAL
Set up a Crafter for gold ingots with slot locks and hopper feed.

RECIPE GRID
8 gold nuggets around empty center (verify recipe in your version). Right-click lock slots that must stay empty so hoppers cannot fill wrong cells.

FEEDING
Hoppers insert only into allowed slots. Redstone pulse (button/clock) = one craft per pulse.

PRACTICE
1) Configure grid locks. 2) Hopper feed nuggets. 3) Pulse—ingot to output. 4) Output hopper → chest.

MISTAKES
All slots unlocked—messy fills; no pulse; locks mismatch recipe.

GOING FURTHER
Filtered hoppers for gold-only supply."""

BODIES["crafter_cake"] = """LESSON GOAL
Automate cake in a Crafter—multiple ingredient types and slot discipline.

RECIPE
Cake uses milk, sugar, egg, wheat in specific 3×3 cells—memorize positions. Lock every cell so only the correct ingredient can enter.

LOGISTICS
Several filtered hoppers or timed phases so ingredients do not mix. Often load, then one pulse.

PRACTICE
Manual correct layout + locks + single pulse = cake. Replace one ingredient at a time with hopper feed.

MISTAKES
Egg in milk slot; milk bucket supply confusion; infinite loop without reset.

GOING FURTHER
Multi-crafter food storage—endgame base project; see wiki Crafter."""
